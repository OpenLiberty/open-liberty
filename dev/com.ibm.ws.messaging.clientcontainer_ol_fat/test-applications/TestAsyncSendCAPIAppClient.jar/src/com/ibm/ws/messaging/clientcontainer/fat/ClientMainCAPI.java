package com.ibm.ws.messaging.clientcontainer.fat;






public class ClientMainCAPI {

	
	
	TestCases_AsyncSendCAPI tc1 = new TestCases_AsyncSendCAPI();
	String str1 = tc1.initialPrep();
	TestCompletionListenerCAPI listener1 = tc1.testSetUp();
	TestCompletionListener_MessageOrderingCAPI listnerMO1 = tc1.testSetUpOrder();
	TestCompletionListenerSessionCAPI listnrSession = tc1.testSetUpSession();
	TestCompletionListenerVariationCAPI listnrVariation1 = tc1
			.testSetUpVariation();
	TestCompletionListenerVariationURCAPI listnrUnrelated1 = tc1
			.testSetUpVariationUR();

	
    public static void main(String[] args) {
    	System.out.println("Entered Main");
    	
    	ClientMainCAPI test = new ClientMainCAPI();
    	
    	System.out.println("Initialized TestAsyncSendClient");
    	try {
    		System.out.println(" Classic API test cases:");
    		
			
			test.testAsyncSendCAPI();
			test.testExceptionMessageThreshhold11();
			test.testAsyncSendException2();
			test.testMessageOrderingSingleJMSProducer3();
			test.testMessageOrderingMultipleJMSProducers();
			test.testMessageOrderingMultipleSession1();
			test.testcloseSession2();
			test.testcloseConnection3();
			test.testcallingNativeSession();
			test.testAsyncSendIDE2();
			test.testAsyncSendCLN();
			test.testAsyncSendNDE1();
			test.testAsyncSendNDE2();
			test.testAsyncSendNDE3();
			test.testAsyncSendNDE4();
			test.testcallingNativeSession_unrelatedSession3();
			test.testOnCompletionOnException2();
			test.testSessioninCallBackMethodOC();
			test.testTimetoLiveVariation();
			
			test.testPriorityVariation();
			
			test.testPriorityVariation_negative();
			
			test.testInvalidDeliveryMode();
			test.testNullEmptyMessage();
			
			
			System.out.println("came back from method test case");
		} catch (Throwable e) {
			// TODO Auto-generated catch block
			System.out.println("I am in catch block:Some test failed.");
			e.printStackTrace();
		}
   
        
        
    }
    
    
      
 


	 //classic api
	public void testAsyncSendCAPI() throws Exception {

		tc1.testAsyncSendCAPI(listener1);

	}

	 
	public void testExceptionMessageThreshhold11() throws Exception {

		tc1.testExceptionMessageThreshhold1(listener1, 5);

	}

	 
	public void testAsyncSendException2() throws Exception {

		tc1.testAsyncSendException2(listener1);

	}

	 
	public void testMessageOrderingSingleJMSProducer3() throws Exception {

		tc1.testMessageOrderingSingleJMSProducer3(listnerMO1, 5);

	}

	 
	public void testMessageOrderingMultipleJMSProducers() throws Exception {

		tc1.testMessageOrderingMultipleJMSProducers(listnerMO1);

	}

	 
	public void testMessageOrderingMultipleSession1() throws Exception {

		tc1.testMessageOrderingMultipleSession1(listnerMO1);

	}

	 
	public void testcloseSession2() throws Exception {

		tc1.testCloseSession2(listener1);

	}

	 
	public void testcloseConnection3() throws Exception {

		tc1.testCloseConnection3(listener1);

	}

	 
	public void testcallingNativeSession() throws Exception {

		tc1.testCallingNativeSession(listnrVariation1);

	}

	 
	public void testAsyncSendIDE2() throws Exception {

		tc1.testAsyncSendIDE2(listener1);
	}

	 
	public void testAsyncSendCLN() throws Exception {

		tc1.testAsyncSendCLN();
	}

	 
	public void testAsyncSendNDE1() throws Exception {

		tc1.testAsyncSendNDE1(listener1);
	}
	public void testAsyncSendNDE2() throws Exception {

		tc1.testAsyncSendNDE2(listener1);
	}
	public void testAsyncSendNDE3() throws Exception {

		tc1.testAsyncSendNDE3(listener1);
	}
	
	public void testAsyncSendNDE4() throws Exception {

		tc1.testAsyncSendNDE4(listener1);
	}
	 
	public void testcallingNativeSession_unrelatedSession3() throws Exception {

		tc1.testCallingNativeSession_unrelatedSession3(listnrUnrelated1);
	}

	 
	public void testOnCompletionOnException2() throws Exception {

		tc1.testOnCompletionOnException2(listener1);
	}

	 
	public void testSessioninCallBackMethodOC() throws Exception {

		tc1.testSessioninCallBackMethodOC1(listnrSession);
	}

	
	 
	public void testTimetoLiveVariation() throws Exception {

		tc1.testTimetoLiveVariation(listener1);
	}

	 
	public void testPriorityVariation() throws Exception {

		tc1.testPriorityVariation(listener1);
	}

	 
	public void testPriorityVariation_negative() throws Exception {

		tc1.testPriorityVariation_negative(listener1);
	}

	 
	public void testInvalidDeliveryMode() throws Exception {

		tc1.testInvalidDeliveryMode(listener1);
	}

	 
	public void testNullEmptyMessage() throws Exception {

		tc1.testNullEmptyMessage(listener1);
	}

}
