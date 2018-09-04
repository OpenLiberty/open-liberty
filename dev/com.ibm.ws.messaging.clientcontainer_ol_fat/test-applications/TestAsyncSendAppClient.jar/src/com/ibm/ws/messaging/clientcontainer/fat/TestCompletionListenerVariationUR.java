package com.ibm.ws.messaging.clientcontainer.fat;

import java.util.concurrent.locks.ReentrantLock;

import javax.jms.CompletionListener;
import javax.jms.JMSContext;
import javax.jms.Message;




public class TestCompletionListenerVariationUR implements CompletionListener{
	
	public static JMSContext jmsContextUR = null;
	public static JMSContext jmsContextNAT = null;
	public boolean close = true;
	public boolean commit = false;
	public boolean rollback = false;

	public static int counter = 0;
	public static int exCounter = 0;
	public ReentrantLock lockObj = new ReentrantLock();
	public int exCnt = 0;
	public int excCnt=0;
	TestCompletionListenerVariationUR(JMSContext jContextUR, JMSContext jContextnative){
		
		jmsContextUR = jContextUR;
		jmsContextNAT=jContextnative;
		
	}
	

	public void onCompletion(Message msg) {
		// TODO Auto-generated method stub
							
				System.out.println("VARIATION Entered On Completion.");	
				
				try {
					System.out.println("Before closing queue session:");
					jmsContextUR.close();
					close = true;
				} catch (Exception e) {
					// TODO Auto-generated catch block
					System.out.println("going to print stacktrace for close");
					e.printStackTrace();
					close = false;
					
				} 
				
				
				try {
						System.out.println("Before commiting queue session:");
						jmsContextNAT.commit();
					} catch (Exception e) {
						// TODO Auto-generated catch block
						System.out.println("going to print stacktrace for commit");
						e.printStackTrace();
						commit = true;
					} 

				
				try {
						System.out.println("Before rollback queue session:");
						jmsContextNAT.rollback();
				
					} catch (Exception e) {
						System.out.println("going to print stacktrace for rollback");
						// TODO Auto-generated catch block
						e.printStackTrace();
						rollback=true;
					}
				counter++;
				new TestCases_AsyncSend().setCounter(counter);
				new TestCases_AsyncSend().setOperation(close,commit,rollback);	
				if(counter ==exCnt)
					lockObj.notify();	
	}

	public void onException(Message msg, Exception exp) {
		// TODO Auto-generated method stub
		System.out.println("On Exception. Async Send Failed!!");
		exCounter++;
		new TestCases_AsyncSend().setexCounter(exCounter);
		if(exCounter ==excCnt)
			lockObj.notify();
	}
	
	
	public void resetCounter(){
	close = true;
	commit = false;
	rollback = false;
	counter = 0;
	exCounter = 0;
	}
	
	public void setLock(ReentrantLock lock, int expectedCount,int exceptionCounter) {
		// TODO Auto-generated method stub
		
	 lockObj = lock;
	 exCnt = expectedCount;
	 excCnt = exceptionCounter;
		
	}

}
