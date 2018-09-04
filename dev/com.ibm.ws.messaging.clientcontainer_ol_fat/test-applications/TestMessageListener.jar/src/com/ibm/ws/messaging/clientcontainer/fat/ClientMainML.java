package com.ibm.ws.messaging.clientcontainer.fat;

public class ClientMainML {
TestCases_MessageListenerML tc = new TestCases_MessageListenerML();
String str = tc.initialPrep();
	
    public static void main(String[] args) {
    	
    	ClientMainML test = new ClientMainML();

    	try {

    		 
    		test.testMessageListener_Contexttest();  		   		
    		test.testMessageListener_Connectionttest(); 		
    		test.testMessageListener_sessiontest();
    		

		} catch (Throwable e) {

			e.printStackTrace();
		}
   
        
        
    }
    
    
      
    
    public void testMessageListener_Contexttest() throws Exception {
  	  tc.testMessageListener_Contexttest();
  	}

  
    public void testMessageListener_Connectionttest() throws Exception {
  	  tc.testMessageListener_Connectionttest();
  	}
   
    public void testMessageListener_sessiontest() throws Exception {
    	  tc.testMessageListener_sessiontest();
    	}

   
 

}
