
import java.math.BigInteger;

import com.google.bitcoin.core.AbstractBlockChain;
import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.Wallet;
import com.google.bitcoin.core.AbstractBlockChain.NewBlockType;
import com.google.bitcoin.core.Wallet.SendRequest;
import com.google.bitcoin.params.UnitTestParams;
import com.google.bitcoin.store.BlockStore;
import com.google.bitcoin.store.MemoryBlockStore;
import com.google.bitcoin.utils.TestUtils;


public class Trans {

	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		
		
		
		Wallet myWallet = new ArtificialWallet();
        
        System.out.println("The Wallet:\n"+myWallet);
        // create a key and address to send to
        
        ECKey keyDest = new ECKey();
        Address toDestinationAddress = keyDest.toAddress(myWallet.getNetworkParameters());
        
        // create a send request
		SendRequest request = SendRequest.to(toDestinationAddress, BigInteger.valueOf(100000000L));
		
		System.out.println("BALANCE:" + myWallet.getBalance());
		
		//Can change transaction fee here
				//Wallet.SendRequest.DEFAULT_FEE_PER_KB = BigInteger.ZERO;
				//request.ensureMinRequiredFee = false;
				//request.fee = BigInteger.ZERO;
				//request.feePerKb = BigInteger.ZERO;
				
				//Create the transaction
				Transaction transaction = myWallet.sendCoinsOffline(request);
				
				System.out.println("The transaction:\n"+transaction);
				
				
				 System.out.println("The Wallet:\n"+myWallet);

	}
	
	
	static class ArtificialWallet extends Wallet {
		
		static NetworkParameters params = UnitTestParams.get();
		
		private ECKey myKey;
		
	    private Address myAddress;
	    
		private BlockStore blockStore;
		
		public ArtificialWallet() throws Exception {
			super(params);
			myKey = new ECKey();
	        myAddress = myKey.toAddress(params);
	        addKey(myKey);
	        
	        blockStore = new MemoryBlockStore(params);
	        // recieve the 5BTC 
	        receiveFakeCoins(blockStore, BigInteger.valueOf(500000000), this, myAddress);

		}
		public static void receiveFakeCoins(BlockStore blockStore, BigInteger amount, Wallet wallet, Address toAddress) throws Exception {
			Transaction tx = TestUtils.createFakeTx(wallet.getNetworkParameters(), amount, toAddress);
			TestUtils.BlockPair bp = TestUtils.createFakeBlock(blockStore, tx);
			wallet.receiveFromBlock(tx, bp.storedBlock, AbstractBlockChain.NewBlockType.BEST_CHAIN, 0);
			wallet.notifyNewBestBlock(bp.storedBlock);
		}
	}
	
	public static String show() throws Exception{
		
		String test = "";
		
		Wallet myWallet = new ArtificialWallet();
		Wallet myWallet2 = new ArtificialWallet();
        //System.out.println("The Wallet:\n"+myWallet);
        // create a key and address to send to
        
        ECKey keyDest = new ECKey();
        Address toDestinationAddress = keyDest.toAddress(myWallet.getNetworkParameters());
        
        // create a send request
		SendRequest request = SendRequest.to(toDestinationAddress, BigInteger.valueOf(100000000L));
		
		//Can change transaction fee here
				Wallet.SendRequest.DEFAULT_FEE_PER_KB = BigInteger.ZERO;
				request.ensureMinRequiredFee = false;
				request.fee = BigInteger.ZERO;
				request.feePerKb = BigInteger.ZERO;
				
				//Create the transaction
				Transaction transaction = myWallet.sendCoinsOffline(request);
				
				//System.out.println("The transaction:\n"+transaction);
		
		test = "The Wallet:\n"+myWallet2 + "\n\n\n\n" + "The transaction:\n"+transaction + "\n\n\n\n" + "The Wallet now:\n" + myWallet ;
		
		return test;
		
	}

}
