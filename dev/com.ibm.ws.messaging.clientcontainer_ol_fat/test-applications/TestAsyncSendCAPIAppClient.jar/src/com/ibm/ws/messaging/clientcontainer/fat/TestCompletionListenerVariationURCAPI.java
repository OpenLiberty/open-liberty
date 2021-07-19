package com.ibm.ws.messaging.clientcontainer.fat;

import java.util.concurrent.locks.ReentrantLock;

import javax.jms.CompletionListener;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.QueueSession;

public class TestCompletionListenerVariationURCAPI implements
		CompletionListener {

	public static QueueSession queueSessionUR = null;
	public static QueueSession queueSessionNAT = null;

	public boolean close = true;
	public boolean commit = false;
	public boolean rollback = false;
	public static int counter = 0;
	public static int exCounter = 0;
	public ReentrantLock lockObj = new ReentrantLock();
	public int exCnt = 0;
	public int excCnt = 0;

	TestCompletionListenerVariationURCAPI(QueueSession QsessionUR,
			QueueSession QSessionNative) {

		queueSessionUR = QsessionUR;
		queueSessionNAT = QSessionNative;

	}

	@Override
	public void onCompletion(Message msg) {
		// TODO Auto-generated method stub

		System.out.println("VARIATION Entered On Completion.");

		try {
			System.out.println("Before closing queue session:");
			queueSessionUR.close();
			close = true;
		} catch (JMSException e) {
			// TODO Auto-generated catch block
			System.out.println("going to print stacktrace for close");
			e.printStackTrace();
			close = false;

		}

		try {
			System.out.println("Before commiting queue session:");
			queueSessionNAT.commit();
		} catch (JMSException e) {
			// TODO Auto-generated catch block
			System.out.println("going to print stacktrace for commit");
			e.printStackTrace();
			commit = true;
		}

		try {
			System.out.println("Before rollback queue session:");
			queueSessionNAT.rollback();

		} catch (JMSException e) {
			System.out.println("going to print stacktrace for rollback");
			// TODO Auto-generated catch block
			e.printStackTrace();
			rollback = true;
		}

		counter++;
		new TestCases_AsyncSendCAPI().setCounter(counter);
		new TestCases_AsyncSendCAPI().setOperation(close, commit, rollback);
		if (counter == exCnt)
			lockObj.notify();
	}

	@Override
	public void onException(Message msg, Exception exp) {
		// TODO Auto-generated method stub
		System.out.println("On Exception. Async Send Failed!!");
		exCounter++;
		new TestCases_AsyncSendCAPI().setexCounter(exCounter);
		if (exCounter == excCnt)
			lockObj.notify();
	}

	public void resetCounter() {
		close = true;
		commit = false;
		rollback = false;
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
