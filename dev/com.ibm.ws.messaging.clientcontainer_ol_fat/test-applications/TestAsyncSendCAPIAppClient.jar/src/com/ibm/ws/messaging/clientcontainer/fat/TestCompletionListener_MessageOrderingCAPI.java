package com.ibm.ws.messaging.clientcontainer.fat;

import java.util.concurrent.locks.ReentrantLock;

import javax.jms.CompletionListener;
import javax.jms.JMSException;
import javax.jms.Message;

public class TestCompletionListener_MessageOrderingCAPI implements
		CompletionListener {

	public int counter = 0;
	public int exCounter = 0;
	public int numMsgs = 0;
	public int i = 0;

	public ReentrantLock lockObj = new ReentrantLock();
	public int exCnt = 0;
	public int excCnt = 0;

	public void setMessgCount(int num) {
		numMsgs = num;
	}

	public int getMessgOrderCount() {
		return i;
	}

	@Override
	public void onCompletion(Message msg) {
		// TODO Auto-generated method stub

		System.out
				.println("On Completion. You have successfully invoked AsyncSend");

		try {
			System.out.println("Order of the message sent is:"
					+ msg.getIntProperty("Message_Order"));

			if (i < numMsgs) {
				if (msg.getIntProperty("Message_Order") == i)
					i++;

			}

			counter++;
			new TestCases_AsyncSendCAPI().setCounter(counter);

			System.out.println("From OC method :Counter set is:" + counter);
		} catch (JMSException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if (counter == exCnt)
			lockObj.notify();

	}

	@Override
	public void onException(Message msg, Exception exp) {
		// TODO Auto-generated method stub

		System.out.println("On Exception.AsyncSend Failed");
		exCounter++;
		new TestCases_AsyncSendCAPI().setexCounter(exCounter);

		System.out.println("From OE method :Counter set is:" + exCounter);
		System.out.println("On Exception. Async Send Failed!!");

		exp.printStackTrace();
		
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
