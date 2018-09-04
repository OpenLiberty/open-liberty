package com.ibm.ws.messaging.clientcontainer.fat;

import java.util.concurrent.locks.ReentrantLock;

import javax.jms.CompletionListener;
import javax.jms.IllegalStateRuntimeException;
import javax.jms.JMSContext;
import javax.jms.Message;

public class TestCompletionListenerContext implements CompletionListener {

	public static JMSContext jmsContext = null;
	public boolean close = false;
	public boolean commit = false;
	public boolean rollback = false;
	public boolean prod = false;
	public int counter = 0;
	public int exCounter = 0;
	public ReentrantLock lockObj = new ReentrantLock();
	public int exCnt = 0;
	public int excCnt = 0;

	TestCompletionListenerContext(JMSContext context) {

		jmsContext = context;

	}

	@Override
	public void onCompletion(Message msg) {
		// TODO Auto-generated method stub

		System.out.println("Entered On Completion.");
		try {
			jmsContext.close();
		} catch (IllegalStateRuntimeException ex) {
			ex.printStackTrace();
			close = true;
		}

		try {
			jmsContext.commit();
		} catch (IllegalStateRuntimeException ex) {
			ex.printStackTrace();
			commit = true;
		}

		try {
			jmsContext.rollback();
		} catch (IllegalStateRuntimeException ex) {
			ex.printStackTrace();
			rollback = true;
		}

		if (jmsContext.createProducer() != null)
			prod = true;

		counter++;

		new TestCases_AsyncSend().setCounter(counter);
		new TestCases_AsyncSend().setOperationTest(close, commit, rollback,
				prod);

		if (counter == exCnt)
			lockObj.notify();
		System.out
				.println("On Completion. You have successfully invoked AsyncSend");
	}

	@Override
	public void onException(Message msg, Exception exp) {
		// TODO Auto-generated method stub

		exCounter++;
		if (jmsContext.createProducer() != null)
			prod = true;

		new TestCases_AsyncSend().setexCounter(exCounter);
		new TestCases_AsyncSend().setOperationTest(close, commit, rollback,
				prod);

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
	}

	public void setLock(ReentrantLock lock, int expectedCount,
			int exceptionCounter) {
		// TODO Auto-generated method stub

		lockObj = lock;
		exCnt = expectedCount;
		excCnt = exceptionCounter;

	}

}
