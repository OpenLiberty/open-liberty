package com.ibm.ws.messaging.clientcontainer.fat;

import java.util.concurrent.locks.ReentrantLock;

import javax.jms.CompletionListener;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.QueueSession;

public class TestCompletionListenerSessionCAPI implements CompletionListener {

	public static QueueSession queueSession = null;
	public static MessageProducer msgProducer = null;
	public boolean close = false;
	public boolean commit = false;
	public boolean rollback = false;
	public boolean prod = false;
	public boolean mprod = false;

	public static Queue queue = null;
	public int counter = 0;
	public int exCounter = 0;
	public ReentrantLock lockObj = new ReentrantLock();
	public int exCnt = 0;
	public int excCnt = 0;

	TestCompletionListenerSessionCAPI(QueueSession QSession, MessageProducer mProducer) {

		queueSession = QSession;
		msgProducer = mProducer;
		queue = GetJMSResourcesCAPI.getJMSQueue();

	}

	@Override
	public void onCompletion(Message msg) {
		// TODO Auto-generated method stub

		System.out.println("Entered On Completion.");
		
			try {
				queueSession.close();
			
		} catch (javax.jms.IllegalStateException ex) {
			ex.printStackTrace();
			close = true;
		} catch(JMSException ex){
			//close = true;
		}

		try {
			queueSession.commit();
		} catch (javax.jms.IllegalStateException ex) {
			ex.printStackTrace();
			commit = true;
		} catch(JMSException ex){
			//commit = true;
		}

		try {
			queueSession.rollback();
		} catch (javax.jms.IllegalStateException  ex) {
			ex.printStackTrace();
			rollback = true;
		} catch (JMSException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			//rollback=true;
		}

	
			
				try {
					System.out.println("Entering the block where close() is called on messageProducer");
					msgProducer.close();
					
				} catch (javax.jms.IllegalStateException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
					mprod = true;
					System.out.println("Got IllegalStateException on calling close() on messageProducer");
				}catch (JMSException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					
				}
		
		try {
			if (queueSession.createProducer(queue) != null)
				prod = true;
		} catch (JMSException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		counter++;

		new TestCases_AsyncSendCAPI().setCounter(counter);
		new TestCases_AsyncSendCAPI().setOperationTest(close, commit, rollback,
				prod,mprod);

		if (counter == exCnt)
			lockObj.notify();
		System.out
				.println("On Completion. You have successfully invoked AsyncSend");

	}

	@Override
	public void onException(Message msg, Exception exp) {
		// TODO Auto-generated method stub

		exCounter++;
		try {
			if (queueSession.createProducer(queue) != null)
				prod = true;
		} catch (JMSException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		new TestCases_AsyncSendCAPI().setexCounter(exCounter);
		new TestCases_AsyncSendCAPI().setOperationTest(close, commit, rollback,
				prod,mprod);

		if (exCounter == excCnt)
			lockObj.notify();
		System.out.println("On Exception. Async Send Failed!!");
	}

	public void reset() {
		close = false;
		commit = false;
		rollback = false;
		prod = false;
		counter = 0;
		exCounter = 0;
		mprod=false;
	}

	public void setLock(ReentrantLock lock, int expectedCount,
			int exceptionCounter) {
		// TODO Auto-generated method stub

		lockObj = lock;
		exCnt = expectedCount;
		excCnt = exceptionCounter;

	}

}
