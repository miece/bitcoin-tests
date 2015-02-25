import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.core.Wallet;


public class CreateWallet {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		// create arraylist to hold keys
		//ArrayList al = new ArrayList();
		
		 // work with the testnet3 (latest testnet version)
        final NetworkParameters netParams = NetworkParameters.prodNet();

        // Create wallet obj and try to read it from file, create a new one if its not available
        Wallet myWallet = null;
        final File walletFile = new File("tester.wallet");
        
        try {
        	 myWallet = new Wallet(netParams);
            
            // 5 keys
            for (int i = 0; i < 5; i++) {
                
                // create a key and add it to the wallet
            	 myWallet.addKey(new ECKey());
            }
            
            // save wallet contents to disk
            myWallet.saveToFile(walletFile);
            
        } catch (IOException e) {
            System.out.println("Unable to create wallet file.");
        }
        
        // get the first key in the wallet
        
        ECKey firstKey = myWallet.getKeys().get(0);

        // output key 
        System.out.println("First key in the wallet:\n" + firstKey);
        
        // output whole wallet
        System.out.println("Complete content of the wallet:\n" + myWallet);
        
        // we can use the hash of the public key
        // to check whether the key pair is in this wallet
        
        //System.out.println(firstKey.getPrivKeyBytes());
        
        if (myWallet.isPubKeyHashMine(firstKey.getPubKeyHash())) {
            System.out.println("Key is in the wallet");
        } else {
            System.out.println("Key is not in the wallet.");
        }


        
	}
	
	
	
	public static String show(){

		String tester;
		 // work with the testnet3 (latest testnet version)
        final NetworkParameters netParams = NetworkParameters.prodNet();

        // Create wallet obj and try to read it from file, create a new one if its not available
        Wallet myWallet = null;
        final File walletFile = new File("tester.wallet");
        
        try {
        	 myWallet = new Wallet(netParams);
            
            // 5 keys
            for (int i = 0; i < 5; i++) {
                
                // create a key and add it to the wallet
            	 myWallet.addKey(new ECKey());
            }
            
            // save wallet contents to disk
            myWallet.saveToFile(walletFile);
            
        } catch (IOException e) {
        	tester = "Unable to create wallet file.";
            //System.out.println("Unable to create wallet file.");
        }
        
        // get the first key in the wallet
        
        ECKey firstKey = myWallet.getKeys().get(0);

        // output key 
        //System.out.println("First key in the wallet:\n" + firstKey);
        
        // output whole wallet
        //System.out.println("Complete content of the wallet:\n" + myWallet);
        
        // we can use the hash of the public key
        // to check whether the key pair is in this wallet
        
        //System.out.println(firstKey.getPrivKeyBytes());
        
        if (myWallet.isPubKeyHashMine(firstKey.getPubKeyHash())) {
            System.out.println("Key is in the wallet");
        } else {
            System.out.println("Key is not in the wallet.");
        }
		tester = "\nFirst key in the wallet:\n" + firstKey + "\n\n\nComplete content of the wallet:\n" + myWallet;
		
		return tester;
	}

}
