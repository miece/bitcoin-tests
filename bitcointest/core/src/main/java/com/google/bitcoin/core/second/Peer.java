/**
 * Copyright 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.bitcoin.core.second;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;

/**
 * A Peer handles the high level communication with a BitCoin node.
 *
 * <p>After making the connection with connect(), call run() to start the message handling loop.
 */
public class Peer {
    private static final Logger log = LoggerFactory.getLogger(Peer.class);

    private NetworkConnection conn;
    private final NetworkParameters params;
    // Whether the peer loop is supposed to be running or not. Set to false during shutdown so the peer loop
    // knows to quit when the socket goes away.
    private boolean running;
    private final BlockChain blockChain;
    // When an API user explicitly requests a block or transaction from a peer, the InventoryItem is put here
    // whilst waiting for the response. Synchronized on itself. Is not used for downloads Peer generates itself.
    private final List<GetDataFuture<Block>> pendingGetBlockFutures;
    // Height of the chain advertised in the peers version message.
    private int bestHeight;
    private PeerAddress address;
    private List<PeerEventListener> eventListeners;
    // Whether to try and download blocks and transactions from this peer. Set to false by PeerGroup if not the
    // primary peer. This is to avoid redundant work and concurrency problems with downloading the same chain
    // in parallel.
    private boolean downloadData = true;

    /**
     * If true, we do some things that may only make sense on constrained devices like Android phones. Currently this
     * only controls message deduplication.
     */
    public static boolean MOBILE_OPTIMIZED = false;

    /**
     * Construct a peer that reads/writes from the given block chain. Note that communication won't occur until
     * you call connect(), which will set up a new NetworkConnection.
     *
     * @param bestHeight our current best chain height, to facilitate downloading
     */
    public Peer(NetworkParameters params, PeerAddress address, int bestHeight, BlockChain blockChain) {
        this.params = params;
        this.address = address;
        this.bestHeight = bestHeight;
        this.blockChain = blockChain;
        this.pendingGetBlockFutures = new ArrayList<GetDataFuture<Block>>();
        this.eventListeners = new ArrayList<PeerEventListener>();
    }

    /**
     * Construct a peer that reads/writes from the given block chain. Note that communication won't occur until
     * you call connect(), which will set up a new NetworkConnection.
     */
    public Peer(NetworkParameters params, PeerAddress address, BlockChain blockChain) {
        this(params, address, 0, blockChain);
    }

    /**
     * Construct a peer that uses the given, already connected network connection object.
     */
    public Peer(NetworkParameters params, BlockChain blockChain, NetworkConnection connection) {
        this(params, null, 0, blockChain);
        this.conn = connection;
    }
    
    public synchronized void addEventListener(PeerEventListener listener) {
        eventListeners.add(listener);
    }

    public synchronized boolean removeEventListener(PeerEventListener listener) {
        return eventListeners.remove(listener);
    }

    @Override
    public String toString() {
        if (address == null) {
            // User-provided NetworkConnection object.
            return "Peer(NetworkConnection:" + conn + ")";
        } else {
            return "Peer(" + address.getAddr() + ":" + address.getPort() + ")";
        }
    }

    /**
     * Connects to the peer.
     *
     * @throws PeerException when there is a temporary problem with the peer and we should retry later
     */
    public synchronized void connect() throws PeerException {
        try {
            conn = new TCPNetworkConnection(address, params, bestHeight, 60000, MOBILE_OPTIMIZED);
        } catch (IOException ex) {
            throw new PeerException(ex);
        } catch (ProtocolException ex) {
            throw new PeerException(ex);
        }
    }

    // For testing
    void setConnection(NetworkConnection conn) {
        this.conn = conn;
    }

    /**
     * Runs in the peers network loop and manages communication with the peer.
     *
     * <p>connect() must be called first
     *
     * @throws PeerException when there is a temporary problem with the peer and we should retry later
     */
    public void run() throws PeerException {
        // This should be called in the network loop thread for this peer
        if (conn == null)
            throw new RuntimeException("please call connect() first");

        running = true;

        try {
            while (true) {
                Message m = conn.readMessage();
                if (m instanceof InventoryMessage) {
                    processInv((InventoryMessage) m);
                } else if (m instanceof Block) {
                    processBlock((Block) m);
                } else if (m instanceof AddressMessage) {
                    // We don't care about addresses of the network right now. But in future,
                    // we should save them in the wallet so we don't put too much load on the seed nodes and can
                    // properly explore the network.
                } else {
                    // TODO: Handle the other messages we can receive.
                    log.warn("Received unhandled message: {}", m);
                }
            }
        } catch (IOException e) {
            if (!running) {
                // This exception was expected because we are tearing down the socket as part of quitting.
                log.info("Shutting down peer loop");
            } else {
                disconnect();
                throw new PeerException(e);
            }
        } catch (ProtocolException e) {
            disconnect();
            throw new PeerException(e);
        } catch (RuntimeException e) {
            disconnect();
            log.error("unexpected exception in peer loop", e);
            throw e;
        }

        disconnect();
    }

    private void processBlock(Block m) throws IOException {
        // This should called in the network loop thread for this peer
        try {
            // Was this block requested by getBlock()?
            synchronized (pendingGetBlockFutures) {
                for (int i = 0; i < pendingGetBlockFutures.size(); i++) {
                    GetDataFuture<Block> f = pendingGetBlockFutures.get(i);
                    if (f.getItem().hash.equals(m.getHash())) {
                        // Yes, it was. So pass it through the future.
                        f.setResult(m);
                        // Blocks explicitly requested don't get sent to the block chain.
                        pendingGetBlockFutures.remove(i);
                        return;
                    }
                }
            }
            // Otherwise it's a block sent to us because the peer thought we needed it, so add it to the block chain.
            // This call will synchronize on blockChain.
            if (blockChain.add(m)) {
                // The block was successfully linked into the chain. Notify the user of our progress.
                for (PeerEventListener listener : eventListeners) {
                    synchronized (listener) {
                        listener.onBlocksDownloaded(this, m, getPeerBlocksToGet());
                    }
                }
            } else {
                // This block is unconnected - we don't know how to get from it back to the genesis block yet. That
                // must mean that there are blocks we are missing, so do another getblocks with a new block locator
                // to ask the peer to send them to us. This can happen during the initial block chain download where
                // the peer will only send us 500 at a time and then sends us the head block expecting us to request
                // the others.

                // TODO: Should actually request root of orphan chain here.
                blockChainDownload(m.getHash());
            }
        } catch (VerificationException e) {
            // We don't want verification failures to kill the thread.
            log.warn("Block verification failed", e);
        } catch (ScriptException e) {
            // We don't want script failures to kill the thread.
            log.warn("Script exception", e);
        }
    }

    private void processInv(InventoryMessage inv) throws IOException {
        // This should be called in the network loop thread for this peer.

        // If this peer isn't responsible for downloading stuff, ignore inv messages.
        // TODO: In future, we should not ignore but count them. This allows a guesstimate of trustworthyness.
        if (!downloadData)
            return;

        // The peer told us about some blocks or transactions they have. For now we only care about blocks.
        Block topBlock = blockChain.getUnconnectedBlock();
        Sha256Hash topHash = (topBlock != null ? topBlock.getHash() : null);
        List<InventoryItem> items = inv.getItems();
        if (isNewBlockTickle(topHash, items)) {
            // An inv with a single hash containing our most recent unconnected block is a special inv,
            // it's kind of like a tickle from the peer telling us that it's time to download more blocks to catch up to
            // the block chain. We could just ignore this and treat it as a regular inv but then we'd download the head
            // block over and over again after each batch of 500 blocks, which is wasteful.
            blockChainDownload(topHash);
            return;
        }
        GetDataMessage getdata = new GetDataMessage(params);
        boolean dirty = false;
        for (InventoryItem item : items) {
            if (item.type != InventoryItem.Type.Block) continue;
            getdata.addItem(item);
            dirty = true;
        }
        // No blocks to download. This probably contained transactions instead, but right now we can't prove they are
        // valid so we don't bother downloading transactions that aren't in blocks yet.
        if (!dirty)
            return;
        // This will cause us to receive a bunch of block messages.
        conn.writeMessage(getdata);
    }

    /** A new block tickle is an inv with a hash containing the topmost block. */
    private boolean isNewBlockTickle(Sha256Hash topHash, List<InventoryItem> items) {
        return items.size() == 1 &&
               items.get(0).type == InventoryItem.Type.Block &&
               topHash != null &&
               items.get(0).hash.equals(topHash);
    }

    /**
     * Asks the connected peer for the block of the given hash, and returns a Future representing the answer.
     * If you want the block right away and don't mind waiting for it, just call .get() on the result. Your thread
     * will block until the peer answers. You can also use the Future object to wait with a timeout, or just check
     * whether it's done later.
     *
     * @param blockHash Hash of the block you wareare requesting.
     * @throws IOException
     */
    public Future<Block> getBlock(Sha256Hash blockHash) throws IOException {
        GetDataMessage getdata = new GetDataMessage(params);
        InventoryItem inventoryItem = new InventoryItem(InventoryItem.Type.Block, blockHash);
        getdata.addItem(inventoryItem);
        GetDataFuture<Block> future = new GetDataFuture<Block>(inventoryItem);
        // Add to the list of things we're waiting for. It's important this come before the network send to avoid
        // race conditions.
        synchronized (pendingGetBlockFutures) {
            pendingGetBlockFutures.add(future);
        }
        conn.writeMessage(getdata);
        return future;
    }

    // A GetDataFuture wraps the result of a getBlock or (in future) getTransaction so the owner of the object can
    // decide whether to wait forever, wait for a short while or check later after doing other work.
    private static class GetDataFuture<T extends Message> implements Future<T> {
        private boolean cancelled;
        private final InventoryItem item;
        private final CountDownLatch latch;
        private T result;

        GetDataFuture(InventoryItem item) {
            this.item = item;
            this.latch = new CountDownLatch(1);
        }

        public boolean cancel(boolean b) {
            // Cannot cancel a getdata - once sent, it's sent.
            cancelled = true;
            return false;
        }

        public boolean isCancelled() {
            return cancelled;
        }

        public boolean isDone() {
            return result != null || cancelled;
        }

        public T get() throws InterruptedException, ExecutionException {
            latch.await();
            assert result != null;
            return result;
        }

        public T get(long l, TimeUnit timeUnit) throws InterruptedException, ExecutionException, TimeoutException {
            if (!latch.await(l, timeUnit))
                throw new TimeoutException();
            assert result != null;
            return result;
        }

        InventoryItem getItem() {
            return item;
        }

        /** Called by the Peer when the result has arrived. Completes the task. */
        void setResult(T result) {
            // This should be called in the network loop thread for this peer
            this.result = result;
            // Now release the thread that is waiting. We don't need to synchronize here as the latch establishes
            // a memory barrier.
            latch.countDown();
        }
    }

    /**
     * Send the given Transaction, ie, make a payment with BitCoins. To create a transaction you can broadcast, use
     * a {@link Wallet}. After the broadcast completes, confirm the send using the wallet confirmSend() method.
     * @throws IOException
     */
    void broadcastTransaction(Transaction tx) throws IOException {
        conn.writeMessage(tx);
    }

    private void blockChainDownload(Sha256Hash toHash) throws IOException {
        // This may run in ANY thread.

        // The block chain download process is a bit complicated. Basically, we start with zero or more blocks in a
        // chain that we have from a previous session. We want to catch up to the head of the chain BUT we don't know
        // where that chain is up to or even if the top block we have is even still in the chain - we
        // might have got ourselves onto a fork that was later resolved by the network.
        //
        // To solve this, we send the peer a block locator which is just a list of block hashes. It contains the
        // blocks we know about, but not all of them, just enough of them so the peer can figure out if we did end up
        // on a fork and if so, what the earliest still valid block we know about is likely to be.
        //
        // Once it has decided which blocks we need, it will send us an inv with up to 500 block messages. We may
        // have some of them already if we already have a block chain and just need to catch up. Once we request the
        // last block, if there are still more to come it sends us an "inv" containing only the hash of the head
        // block.
        //
        // That causes us to download the head block but then we find (in processBlock) that we can't connect
        // it to the chain yet because we don't have the intermediate blocks. So we rerun this function building a
        // new block locator describing where we're up to.
        //
        // The getblocks with the new locator gets us another inv with another bunch of blocks. We download them once
        // again. This time when the peer sends us an inv with the head block, we already have it so we won't download
        // it again - but we recognize this case as special and call back into blockChainDownload to continue the
        // process.
        //
        // So this is a complicated process but it has the advantage that we can download a chain of enormous length
        // in a relatively stateless manner and with constant/bounded memory usage.
        log.info("blockChainDownload({})", toHash.toString());

        // TODO: Block locators should be abstracted out rather than special cased here.
        List<Sha256Hash> blockLocator = new LinkedList<Sha256Hash>();
        // For now we don't do the exponential thinning as suggested here: 
        //  https://en.bitcoin.it/wiki/Protocol_specification#getblocks
        // However, this should be taken seriously going forward. The old implementation only added the hash of the 
        // genesis block and the current chain head, which randomly led us to halt block fetching when ending on a
        // chain that turned out not to be the longest. This happened roughly once a week. 
        // Now we add three hashes to the locator:
        // 1. Hash of genesis block
        // 2. Hash of the block previous to the chain head
        // 3. Hash of the chain head
        // This allows our peer to see that we are on the wrong track if we ended up on the wrong side of a chain fork
        // if the fork is only one block deep.
        blockLocator.add(params.genesisBlock.getHash());
        Block topBlock = blockChain.getChainHead().getHeader();
        if (!topBlock.equals(params.genesisBlock)) {
            if (!topBlock.getPrevBlockHash().equals(params.genesisBlock)){
                blockLocator.add(0, topBlock.getPrevBlockHash());
            }
            blockLocator.add(0, topBlock.getHash());
        }
        GetBlocksMessage message = new GetBlocksMessage(params, blockLocator, toHash);
        conn.writeMessage(message);
    }

    /**
     * Starts an asynchronous download of the block chain. The chain download is deemed to be complete once we've
     * downloaded the same number of blocks that the peer advertised having in its version handshake message.
     */
    public void startBlockChainDownload() throws IOException {
        setDownloadData(true);
        // TODO: peer might still have blocks that we don't have, and even have a heavier
        // chain even if the chain block count is lower.
        if (getPeerBlocksToGet() >= 0) {
            for (PeerEventListener listener : eventListeners) {
                synchronized (listener) {
                    listener.onChainDownloadStarted(this, getPeerBlocksToGet());
                }
            }

            // When we just want as many blocks as possible, we can set the target hash to zero.
            blockChainDownload(Sha256Hash.ZERO_HASH);
        }
    }

    /**
     * @return the number of blocks to get, based on our chain height and the peer reported height
     */
    private int getPeerBlocksToGet() {
        // Chain will overflow signed int blocks in ~41,000 years.
        int chainHeight = (int) conn.getVersionMessage().bestHeight;
        if (chainHeight <= 0) {
            // This should not happen because we shouldn't have given the user a Peer that is to another client-mode
            // node. If that happens it means the user overrode us somewhere.
            return -1;
        }
        int blocksToGet = chainHeight - blockChain.getChainHead().getHeight();
        return blocksToGet;
    }

    /**
     * Terminates the network connection and stops the message handling loop.
     */
    public synchronized void disconnect() {
        running = false;
        try {
            // This is the correct way to stop an IO bound loop
            if (conn != null)
                conn.shutdown();
        } catch (IOException e) {
            // Don't care about this.
        }
    }

    /**
     * Returns true if this peer will try and download things it is sent in "inv" messages. Normally you only need
     * one peer to be downloading data. Defaults to true.
     */
    public boolean getDownloadData() {
        return downloadData;
    }

    /**
     * If set to false, the peer won't try and fetch blocks and transactions it hears about. Normally, only one
     * peer should download missing blocks. Defaults to true.
     */
    public void setDownloadData(boolean downloadData) {
        this.downloadData = downloadData;
    }
}
