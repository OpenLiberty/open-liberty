package com.ibm.ws.messaging.clientcontainer.fat;

import java.util.concurrent.locks.ReentrantLock;

import javax.jms.CompletionListener;
import javax.jms.JMSContext;
import javax.jms.Message;




public class TestCompletionListenerVariation implements CompletionListener{
	
	public static JMSContext jmsContext = null;
	
	public boolean close = false;
	public boolean commit = false;
	public boolean rollback = false;
	public static int counter = 0;
	public static int exCounter = 0;
	public ReentrantLock lockObj = new ReentrantLock();
	public int exCnt = 0;
	public int excCnt=0;
	TestCompletionListenerVariation(JMSContext jContext){
		
		jmsContext = jContext;
		
	}
	
	public void onCompletion(Message msg) {
		// TODO Auto-generated method stub
							
				System.out.println("VARIATION Entered On Completion.");	
								
				try {
					System.out.println("Before closing queue session:");
					jmsContext.close();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					System.out.println("going to print stacktrace for close");
					e.printStackTrace();
					close = true;
					new TestCases_AsyncSend().setFlagClose(close);
					
					System.out.println("From OC method :Flag set for close is:"+ close);
					
				} 
				
				
				
					try {
						System.out.println("Before commiting queue session:");
						jmsContext.commit();
					} catch (Exception e) {
						// TODO Auto-generated catch block
						System.out.println("going to print stacktrace for commit");
						e.printStackTrace();
						commit = true;
						new TestCases_AsyncSend().setFlagCommit(commit);
						
						System.out.println("From OC method :Flag set for commit is:"+ commit);

					} 

				
					try {
						System.out.println("Before rollback queue session:");
						jmsContext.rollback();
				
					} catch (Exception e) {
						System.out.println("going to print stacktrace for rollback");
						// TODO Auto-generated catch block
						e.printStackTrace();
						rollback = true;
						
						new TestCases_AsyncSend().setFlagRollback(rollback);
						
						System.out.println("From OC method :Flag set for rollback is:"+ rollback);

					}
					counter++;
					new TestCases_AsyncSend().setCounter(counter);
					
					System.out.println("From OC method :Counter set is:"+ counter);

					if(counter ==exCnt)
						lockObj.notify();	
				
	}

	public void onException(Message msg, Exception exp) {
		// TODO Auto-generated method stub
		System.out.println("On Exception. Async Send Failed!!");
		exCounter++;
		new TestCases_AsyncSend().setexCounter(exCounter);
		System.out.println("From OE method :exCounter set is:"+ exCounter);
		if(exCounter ==excCnt)
			lockObj.notify();
	}
	
	public void resetFlag()
		{
			close =false;
			commit = false;
			rollback = false;
		}
		
		public void resetCounter()
		{
			counter =0;
			exCounter =0;
		}
		
		public void setLock(ReentrantLock lock, int expectedCount,int exceptionCounter) {
			// TODO Auto-generated method stub
			
		 lockObj = lock;
		 exCnt = expectedCount;
		 excCnt = exceptionCounter;
			
		}

}
	

