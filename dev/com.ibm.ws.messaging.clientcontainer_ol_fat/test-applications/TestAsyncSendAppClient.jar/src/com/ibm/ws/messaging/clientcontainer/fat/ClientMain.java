package com.ibm.ws.messaging.clientcontainer.fat;






public class ClientMain {

	TestCases_AsyncSend tc = new TestCases_AsyncSend();
	String str = tc.initialPrep();
	TestCompletionListener listener = tc.testSetUp();
	TestCompletionListener_MessageOrdering listnerMO = tc.testSetUpOrder();
	TestCompletionListenerContext listnrCtx = tc.testSetUpContext();
	TestCompletionListenerVariation listnrVariation = tc.testSetUpVariation();
	TestCompletionListenerVariationUR listnrUnrelated = tc
			.testSetUpVariationUR();
	
    public static void main(String[] args) {
    	System.out.println("Entered Main");
    	
    	ClientMain test = new ClientMain();
    	
    	System.out.println("Initialized TestAsyncSendClient");
    	try {
    		System.out.println(" Simplified API test cases:");
    		test.testSetAsyncOff();
    		 
    		test.testSetAsyncOn();
			test.testExceptionMessageThreshhold();
			 test.testExceptionNEQueue();
			 test.testMessageOrderingSingleJMSProducer();
			test.testMessageOrderingMultipleJMSProducers();
			test.testMessageOrderingSyncAsyncMix();
			test.testMessageOrderingMultipleJMSContexts();
			test.testClose();
			test.testCommit();
			test.testRollBack();
			
			test.testCallingNativeContext();
			test.testStreamMessageTypesAOC();
			test.testObjectMessageTypesAOC();
			test.testMapMessageTypesAOC();
			test.testBytesMessageTypesAOC();
			test.testTextMessageTypesAOC();
			
			test.testStreamMessageTypesBOC();
			test.testObjectMessageTypesBOC();
			test.testMapMessageTypesBOC();
			test.testBytesMessageTypesBOC();
			test.testTextMessageTypesBOC();
			test.testMessageOnException();
			test.testOnCompletionOnException();
			test.testCallingNativeContext_unrelatedContext();
			test.testGetAsync();
			test.testJMSContextinCallBackMethodOC();
			
		test.testAsyncSendNDE1();
		
		test.testAsyncSendNDE2();
		test.testAsyncSendNDE3();
		test.testAsyncSendNDE4();
		
		test.testDCF();
		test.testDCFVariation();
			
			System.out.println("came back from method test case");
		} catch (Throwable e) {
			// TODO Auto-generated catch block
			System.out.println("I am in catch block:Some test failed.");
			e.printStackTrace();
		}
   
        
        
    }
    
    
      
 
   
  public void testSetAsyncOn() throws Exception {

	  System.out.println("Entering test case method");
	  tc.testSetAsyncOn(listener);

	}
 
    public void testSetAsyncOff() throws Exception {

	  System.out.println("Entering test case method");
	  tc.testSetAsyncOff(listener);

	}
    
     
  public void testExceptionMessageThreshhold() throws Exception {

		tc.testExceptionMessageThreshhold(listnerMO, 5);

	}
  
  
  
	public void testExceptionNEQueue() throws Exception {

		tc.testExceptionNEQueue(listener);

	}

	
	public void testMessageOrderingSingleJMSProducer() throws Exception {

		tc.testMessageOrderingSingleJMSProducer(listnerMO, 5);

	}


	public void testMessageOrderingMultipleJMSProducers() throws Exception {

		tc.testMessageOrderingMultipleJMSProducers(listnerMO);

	}

	
	public void testMessageOrderingSyncAsyncMix() throws Exception {

		tc.testMessageOrderingSyncAsyncMix(listnerMO);

	}

	
	public void testMessageOrderingMultipleJMSContexts() throws Exception {

		tc.testMessageOrderingMultipleJMSContexts(listnerMO);

	}

	
	public void testClose() throws Exception {

		tc.testClose(listener);

	}

	
	public void testCommit() throws Exception {

		tc.testCommit(listener);

	}

	
	public void testRollBack() throws Exception {

		tc.testRollBack(listener);

	}

	
	public void testCallingNativeContext() throws Exception {

		tc.testCallingNativeContext(listnrVariation);

	}

	
	public void testStreamMessageTypesAOC() throws Exception {

		tc.testStreamMessageTypesAOC(listener);

	}

	
	public void testBytesMessageTypesAOC() throws Exception {

		tc.testBytesMessageTypesAOC(listener);
	}

	
	public void testTextMessageTypesAOC() throws Exception {

		tc.testTextMessageTypesAOC(listener);
	}

	
	public void testObjectMessageTypesAOC() throws Exception {

		tc.testObjectMessageTypesAOC(listener);
	}

	
	public void testMapMessageTypesAOC() throws Exception {

		tc.testMapMessageTypesAOC(listener);

	}

	public void testStreamMessageTypesBOC() throws Exception {

		tc.testStreamMessageTypesBOC(listnerMO);
	}

	
	public void testTextMessageTypesBOC() throws Exception {

		tc.testTextMessageTypesBOC(listnerMO);
	}

	
	public void testBytesMessageTypesBOC() throws Exception {

		tc.testBytesMessageTypesBOC(listnerMO);

	}

	
	public void testObjectMessageTypesBOC() throws Exception {

		tc.testObjectMessageTypesBOC(listnerMO);

	}

	
	public void testMapMessageTypesBOC() throws Exception {

		tc.testMapMessageTypesBOC(listnerMO);
	}

	
	public void testMessageOnException() throws Exception {

		tc.testMessageOnException(listener);
	}

	
	public void testOnCompletionOnException() throws Exception {

		tc.testOnCompletionOnException(listener);
	}

	
	public void testCallingNativeContext_unrelatedContext() throws Exception {

		tc.testCallingNativeContext_unrelatedContext(listnrUnrelated);
	}

	
	public void testGetAsync() throws Exception {

		tc.testGetAsync(listener);
	}

	
	public void testJMSContextinCallBackMethodOC() throws Exception {

		tc.testJMSContextinCallBackMethodOC(listnrCtx);
	}

	
	public void testAsyncSendNDE1() throws Exception {

		tc.testAsyncSendNDE1(listener);
	}

	
	public void testAsyncSendNDE2() throws Exception {

		tc.testAsyncSendNDE2(listener);
	}

	public void testAsyncSendNDE3() throws Exception {

		tc.testAsyncSendNDE3(listener);
	}
	
	public void testAsyncSendNDE4() throws Exception {

		tc.testAsyncSendNDE4(listener);
	}
	
	public void testDCF() throws Exception {

		tc.testDCF();
	}
	
	public void testDCFVariation() throws Exception {

		tc.testDCFVariation();
	}
}
