import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.NetworkParameters;


public class CreateAddress {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		
		   // use the bitcoin test network (can also use "prod" to use the production network)
		   // use the bitcoin test network (can also use "prod" to use the production network)
		   String prodNet = "prod";
		   String testNet = "test";
		
		   // create a new EC Key
	       ECKey key = new ECKey();
	       ECKey key2 = new ECKey();

	             
	        // select the test net or prod net
	        final NetworkParameters prodNetParams;
	        final NetworkParameters testNetParams;
	        
	        prodNetParams = NetworkParameters.prodNet();
	        testNetParams = NetworkParameters.testNet3();
	        /*
	        if (net.equals("prod")) 
	        {
	          netParams = NetworkParameters.prodNet();
	        } 
	        else 
	        {
	           netParams = NetworkParameters.testNet3();
	        }
	        */
	               
	        // generate a public address based on which network selected
	        Address addressFromKeyForProd = key.toAddress(prodNetParams);
	        Address addressFromKeyForTest = key2.toAddress(testNetParams);
	               
	        String tester = "We created key:\n" + key + "\nOn the " + prodNet + " network, we can use this address:\n" + addressFromKeyForProd 
	        		+ "\n\n\n" + "We created key:\n" + key2 + "\nOn the " + testNet + " network, we can use this address:\n" + addressFromKeyForTest;
	        
	        System.out.println(tester);
	  }
	
	public static String show(){

		   // use the bitcoin test network (can also use "prod" to use the production network)
		   String prodNet = "prod";
		   String testNet = "test";
		
		   // create a new EC Key
	       ECKey key = new ECKey();
	       ECKey key2 = new ECKey();

	             
	        // select the test net or prod net
	        final NetworkParameters prodNetParams;
	        final NetworkParameters testNetParams;
	        
	        prodNetParams = NetworkParameters.prodNet();
	        testNetParams = NetworkParameters.testNet3();
	        /*
	        if (net.equals("prod")) 
	        {
	          netParams = NetworkParameters.prodNet();
	        } 
	        else 
	        {
	           netParams = NetworkParameters.testNet3();
	        }
	        */
	               
	        // generate a public address based on which network selected
	        Address addressFromKeyForProd = key.toAddress(prodNetParams);
	        Address addressFromKeyForTest = key2.toAddress(testNetParams);
	               
	        String tester = "We created key:\n" + key + "\nOn the " + prodNet + " network, we can use this address:\n" + addressFromKeyForProd 
	        		+ "\n\n\n" + "We created key:\n" + key2 + "\nOn the " + testNet + " network, we can use this address:\n" + addressFromKeyForTest;
	       
			return tester;
		
	}
}
