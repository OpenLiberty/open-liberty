package com.ibm.ws.messaging.clientcontainer.fat;



public class ClientMainClientID {

	ClientIDTestcase tc = new ClientIDTestcase();

	String str = tc.initialPrep1();
	

    public static void main(String[] args) {
    	    	
    	ClientMainClientID test = new ClientMainClientID();
    	
    	try {
    		
			test.testSetClientID();
			test.testSetClientIDID();
			test.testDurableSubscriberClientIDEmpty();			
		
		} catch (Throwable e) {
			
			e.printStackTrace();
		}  
 
    }
 
    
    public void testSetClientID() throws Exception {

	  tc.testSetClientID();
	  
	}
    
    public void testSetClientIDID() throws Exception {

	  tc.testSetClientIDID();
	  
	}
    
    
    public void testDurableSubscriberClientIDEmpty() throws Exception {

	  tc.testDurableSubscriberClientIDEmpty();
	  
	}
    


}
