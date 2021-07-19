package com.ibm.ws.messaging.clientcontainer.fat;

import java.util.concurrent.locks.ReentrantLock;

import javax.jms.CompletionListener;
import javax.jms.Message;

public class TestCompletionListenerCAPI implements CompletionListener {

	public int counter = 0;
	public int exCounter = 0;
	public ReentrantLock lockObj = new ReentrantLock();
	public int exCnt = 0;
	public int excCnt = 0;

	@Override
	public void onCompletion(Message msg) {
		// TODO Auto-generated method stub

		System.out
				.println("On Completion. You have successfully invoked AsyncSend");
		// System.out.println("Message sent is :"+ msg);
		counter++;
		new TestCases_AsyncSendCAPI().setCounter(counter);

		System.out.println("From OC method :Counter set is:" + counter);

		if (counter == exCnt)
			lockObj.notify();
	}

	@Override
	public void onException(Message msg, Exception exp) {
		// TODO Auto-generated method stub

		System.out.println("On Exception. Async Send Failed!!");
		exp.printStackTrace();
		exCounter++;
		new TestCases_AsyncSendCAPI().setexCounter(exCounter);
		System.out.println("From OE method :exCounter set is:" + exCounter);
		if (exCounter == excCnt)
			lockObj.notify();

	}

	public void resetCounter() {
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
