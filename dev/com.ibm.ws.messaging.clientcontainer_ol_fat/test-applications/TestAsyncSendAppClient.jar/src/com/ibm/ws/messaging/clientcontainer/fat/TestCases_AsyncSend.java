package com.ibm.ws.messaging.clientcontainer.fat;

import java.util.Enumeration;
import java.util.concurrent.locks.ReentrantLock;

import javax.jms.BytesMessage;
import javax.jms.ConnectionFactory;

import javax.jms.InvalidDestinationRuntimeException;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.JMSProducer;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageNotWriteableException;

import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.jms.QueueConnectionFactory;

import javax.jms.Session;
import javax.jms.StreamMessage;
import javax.jms.TextMessage;
import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;

public class TestCases_AsyncSend {

	public JMSProducer jmsProducer = null;
	public JMSConsumer jmsConsumer = null;
	public Queue jmsQueue = null;
	public Message message = null;
	public static QueueConnectionFactory qcf = null;
	public static ConnectionFactory cf = null;
	public static JMSContext jmsContext = null;
	public static JMSContext jmsContextCtx = null;
	public static JMSContext jmsContextTransacted = null;
	public static JMSContext jmsContextTransacted1 = null;
	public static JMSContext jmsContextTransactedN = null;
	public JMSProducer jmsProducerMO = null;
	public JMSProducer jmsProducerMO1 = null;
	public JMSProducer jmsProducerMO2 = null;
	public JMSProducer jmsProducerMO3 = null;
	public JMSProducer jmsProducerMO4 = null;

	public JMSConsumer jmsConsumerMO = null;
	public Queue jmsQueueMO = null;
	public Queue exQueue = null;
	public Message messageMO = null;
	private static int WAIT_TIME = 10000;


	TestCompletionListener tcmpListner = null;
	TestCompletionListenerContext tcmpListnerCtx = null;
	TestCompletionListener_MessageOrdering tcmpListenerMO = null;
	TestCompletionListenerVariation tcmpListVariation = null;
	TestCompletionListenerVariationUR tcmpListVariationUR = null;
	public static int order = 0;

	public static QueueBrowser qb = null;

	public static int count = 0;
	public static int excount = 0;
	public static boolean closeTC = false;

	public static boolean commitTC = false;
	public static boolean rollbackTC = false;

	public static boolean closeUR = false;
	public static boolean commitUR = false;
	public static boolean rollbackUR = false;

	public static boolean closeCtx = false;
	public static boolean commitCtx = false;
	public static boolean rollbackCtx = false;
	public static boolean prodCtx = false;

	public ReentrantLock lock = new ReentrantLock();

	public String initialPrep() {
	//	GetJMSResources.establishConnection(busName, providerEndPoints,
			//	DurableSubscriptionHome);
		System.out.println("Entered the initialPrep method");
		try {
			GetJMSResources.establishConnection();
		} catch (NamingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		

		qcf = GetJMSResources.getQCF();
		cf= GetJMSResources.getCF();
		
		jmsQueue = GetJMSResources.getJMSQueue();
		exQueue = GetJMSResources.getexQueue();
		jmsQueueMO = GetJMSResources.getJMSQueueMO();
		return ("initial Prep done");

	}

	public TestCompletionListener testSetUp()  {

		
		tcmpListner = new TestCompletionListener();
		return tcmpListner;
	}

	public TestCompletionListenerContext testSetUpContext() {

		jmsContextCtx = qcf.createContext(Session.SESSION_TRANSACTED);
		tcmpListnerCtx = new TestCompletionListenerContext(jmsContextCtx);
		return tcmpListnerCtx;
	}

	public TestCompletionListener_MessageOrdering testSetUpOrder() {

		tcmpListenerMO = new TestCompletionListener_MessageOrdering();
		return tcmpListenerMO;
	}

	public TestCompletionListenerVariation testSetUpVariation() {

		jmsContextTransactedN = qcf.createContext(Session.SESSION_TRANSACTED);
		tcmpListVariation = new TestCompletionListenerVariation(
				jmsContextTransactedN);

		return tcmpListVariation;
	}

	public TestCompletionListenerVariationUR testSetUpVariationUR() {

		jmsContextTransacted1 = qcf.createContext(Session.SESSION_TRANSACTED);
		jmsContextTransacted = qcf.createContext(Session.SESSION_TRANSACTED);

		tcmpListVariationUR = new TestCompletionListenerVariationUR(
				jmsContextTransacted1, jmsContextTransacted);

		return tcmpListVariationUR;
	}

	public Message getMessage(int order) {

		messageMO = GetJMSResources.getJMSMessage(order);
		return messageMO;

	}

	public void testSetAsyncOff(TestCompletionListener listner) throws JMSException
			 {

		listner.resetCounter();
		resetCounter();
		clearQueue(jmsQueue);
		System.out
				.println("*****************Starting Test : testSetAsyncOff***********************");
		JMSContext jmsContext = qcf.createContext();
System.out.println("context is:"+ jmsContext);
		JMSProducer jmsProducer = jmsContext.createProducer();
		System.out.println("producer is:"+ jmsProducer);
		
		JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);
		
		System.out.println("consumer is:"+ jmsConsumer);
		jmsProducer.setAsync(null);
		System.out.println("setting listener as null");
		
		jmsProducer.send(jmsQueue, "This is a test with setAsync");
		
		System.out.println("Reached after send:");

		String msgRcvd = jmsConsumer.receiveBody(String.class, WAIT_TIME)
				.toString();

		System.out.println("Message Received is :" + msgRcvd);
		boolean flag = false;
		
		
		if (msgRcvd.equals("This is a test with setAsync") && count == 0
				&& excount == 0) {
			flag = true;
		System.out.println("flag set is:"+ flag);
			//Assert.assertTrue(flag);
				
			System.out.println("after assert");
			System.out.println("Test Case: testSetAsyncOff Passed"); }
		 else {
			
			
			System.out.println("Test Case: testSetAsyncOff failed");
			//Assert.assertTrue(flag);
			try {
				throw new Exception("Condition check failed.");
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

		System.out.println("Exiting assert block");
		clearQueue(jmsQueue);

		System.out.println("clearing queue");
		jmsContext.close();

		
		System.out
				.println("*******************Ending Test : testSetAsyncOff************************");
	}

	public void testSetAsyncOn(TestCompletionListener listner)
			throws JMSException, InterruptedException {
		listner.resetCounter();
		resetCounter();
		clearQueue(jmsQueue);
		int expectedCount = 1;
		int exceptionCount = 0;
		listner.setLock(lock, expectedCount, exceptionCount);
		System.out
				.println("**********************Starting Test : testSetAsyncOn**********************");

		JMSContext jmsContext = qcf.createContext();
		JMSProducer jmsProducer = jmsContext.createProducer();
		JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);
		jmsProducer.setAsync(listner);

		jmsProducer.send(jmsQueue, "This is a test with setAsync");

		// Thread.sleep(100);

		synchronized (lock) {
			while (count != 1) {
				lock.wait(WAIT_TIME);
			}
		}

		System.out
				.println("Now the message has been sent. We will compare the message received");

		String msgRcvd = jmsConsumer.receiveBody(String.class, WAIT_TIME)
				.toString();

		System.out.println("Message Received is :" + msgRcvd);

		System.out.println(count);
		System.out.println(excount);
		boolean flag = false;
		if (msgRcvd.equals("This is a test with setAsync") && count == 1
				&& excount == 0) {
			flag = true;
			//Assert.assertTrue(flag);
			System.out.println("Test Case: testSetAsyncOn Passed");
		} else {
			System.out.println("Test Case: testSetAsyncOn failed");
			//Assert.assertTrue(flag);
			try {
				throw new Exception("Condition check failed.");
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		clearQueue(jmsQueue);

		listner.resetCounter();
		resetCounter();
		jmsContext.close();
		System.out
				.println("******************Ending Test : testSetAsyncOn***************************");

	}

	public void testMessageOrderingSingleJMSProducer(
			TestCompletionListener_MessageOrdering lsnrMO, int numMsgs)
			throws JMSException, InterruptedException {

		lsnrMO.resetCounter();
		resetCounter();

		clearQueue(jmsQueueMO);
		lsnrMO.setMessgCount(numMsgs);
		System.out
				.println("**********************Starting Test : testMessageOrderingSingleJMSProducer**********************");

		int expectedCount = 5;
		int exceptionCount = 0;
		lsnrMO.setLock(lock, expectedCount, exceptionCount);
		int i = 0;
		int redFlag = 0;
		int msgOrder[] = new int[numMsgs];

		JMSContext jmsContext = qcf.createContext();
		JMSProducer messageProducerMO = jmsContext.createProducer();
		JMSConsumer messageConsumerMO = jmsContext.createConsumer(jmsQueueMO);
		messageProducerMO.setAsync(lsnrMO);
		try {
			for (i = 0; i < numMsgs; i++) {
				Message msg = getMessage(i);
				messageProducerMO.send(jmsQueueMO, msg);
				int msgRcvd = messageConsumerMO.receive(WAIT_TIME)
						.getIntProperty("Message_Order");

				msgOrder[i] = msgRcvd;
				System.out.println(msgOrder[i]);

			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		for (i = 0; i < numMsgs; i++) {
			System.out.println("Retrieving Message Order:" + msgOrder[i]);
			if (msgOrder[i] == i)
				System.out.println("msgOrder:" + msgOrder[i]);
			else
				redFlag++;
		}

		boolean flag = false;

		synchronized (lock) {
			while (count != 5) {
				lock.wait(WAIT_TIME);
			}
		}

		System.out.println("redFlag:" + redFlag);
		System.out.println("count:" + count);
		System.out.println("excount:" + excount);

		int total_order = lsnrMO.getMessgOrderCount();
		System.out.println("TotalMessageOrder:" + total_order);

		if (redFlag == 0 && count == 5 && excount == 0
				&& total_order == numMsgs) {

			flag = true;
			//Assert.assertTrue(flag);
			System.out
					.println("Test case :testMessageOrderingSingleJMSProducer passed");

		} else {
			System.out
					.println("Test case : testMessageOrderingSingleJMSProducer failed");
			//Assert.assertTrue(flag);
			try {
				throw new Exception("Condition check failed.");
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		clearQueue(jmsQueueMO);
		jmsContext.close();
		lsnrMO.resetCounter();
		resetCounter();
		System.out
				.println("**********************Ending Test : testMessageOrderingSingleJMSProducer**********************");

	}

	public void testMessageOrderingMultipleJMSProducers(
			TestCompletionListener_MessageOrdering lsnrMO) throws JMSException,
			InterruptedException {

		lsnrMO.setMessgCount(5);
		lsnrMO.resetCounter();
		resetCounter();
		clearQueue(jmsQueueMO);
		int expectedCount = 5;
		int exceptionCount = 0;
		lsnrMO.setLock(lock, expectedCount, exceptionCount);
		System.out
				.println("**********************Starting Test : testMessageOrderingMultipleJMSProducers**********************");
		int i = 0;
		int redFlag = 0;
		int msgOrder[] = new int[5];
		int msgRcvd = 0;
		JMSContext jmsContext = qcf.createContext();

		JMSProducer messageProducerMO = jmsContext.createProducer();
		messageProducerMO.setAsync(lsnrMO);
		JMSProducer messageProducerMO1 = jmsContext.createProducer();
		messageProducerMO1.setAsync(lsnrMO);
		JMSProducer messageProducerMO2 = jmsContext.createProducer();
		messageProducerMO2.setAsync(lsnrMO);
		JMSProducer messageProducerMO3 = jmsContext.createProducer();
		messageProducerMO3.setAsync(lsnrMO);
		JMSProducer messageProducerMO4 = jmsContext.createProducer();
		messageProducerMO4.setAsync(lsnrMO);
		JMSConsumer messageConsumerMO = jmsContext.createConsumer(jmsQueueMO);

		Message msg0 = getMessage(0);
		messageProducerMO.send(jmsQueueMO, msg0);
		// Thread.sleep(100);

		System.out.println("Retrieving the message order");
		msgRcvd = messageConsumerMO.receive(WAIT_TIME).getIntProperty(
				"Message_Order");
		msgOrder[0] = msgRcvd;
		System.out.println(msgOrder[0]);

		Message msg1 = getMessage(1);
		messageProducerMO1.send(jmsQueueMO, msg1);
		// Thread.sleep(100);
		System.out.println("Retrieving the message order");
		msgRcvd = messageConsumerMO.receive(WAIT_TIME).getIntProperty(
				"Message_Order");
		msgOrder[1] = msgRcvd;
		System.out.println(msgOrder[1]);

		Message msg2 = getMessage(2);
		messageProducerMO2.send(jmsQueueMO, msg2);
		System.out.println("Retrieving the message order");
		// Thread.sleep(100);
		msgRcvd = messageConsumerMO.receive(WAIT_TIME).getIntProperty(
				"Message_Order");
		msgOrder[2] = msgRcvd;
		System.out.println(msgOrder[2]);

		Message msg3 = getMessage(3);
		messageProducerMO3.send(jmsQueueMO, msg3);
		System.out.println("Retrieving the message order");
		// Thread.sleep(100);
		msgRcvd = messageConsumerMO.receive(WAIT_TIME).getIntProperty(
				"Message_Order");
		msgOrder[3] = msgRcvd;
		System.out.println(msgOrder[4]);

		Message msg4 = getMessage(4);
		messageProducerMO4.send(jmsQueueMO, msg4);
		System.out.println("Retrieving the message order");
		// Thread.sleep(100);
		msgRcvd = messageConsumerMO.receive(WAIT_TIME).getIntProperty(
				"Message_Order");
		msgOrder[4] = msgRcvd;
		System.out.println(msgOrder[4]);

		for (i = 0; i < 5; i++) {
			System.out.println("Retrieving Message Order:" + msgOrder[i]);
			if (msgOrder[i] == i)
				System.out.println("msgOrder:" + msgOrder[i]);
			else
				redFlag++;
		}

		synchronized (lock) {
			while (count != 5) {
				lock.wait(WAIT_TIME);
			}
		}

		System.out.println("redFlag:" + redFlag);
		System.out.println("count:" + count);
		System.out.println("excount:" + excount);

		int total_order = lsnrMO.getMessgOrderCount();
		System.out.println("TotalMessageOrder:" + total_order);

		boolean flag = false;
		if (redFlag == 0 && count == 5 && excount == 0 && total_order == 5) {
			flag = true;
			//Assert.assertTrue(flag);
			System.out
					.println("Test case :testMessageOrderingMultipleJMSProducers passed");
		} else {
			System.out
					.println("Test case : testMessageOrderingMultipleJMSProducers failed");
			//Assert.assertTrue(flag);
			try {
				throw new Exception("Condition check failed.");
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		clearQueue(jmsQueueMO);
		jmsContext.close();
		lsnrMO.resetCounter();
		resetCounter();

		System.out
				.println("**********************Ending Test : testMessageOrderingMultipleJMSProducers**********************");

	}

	public void testMessageOrderingMultipleJMSContexts(
			TestCompletionListener_MessageOrdering lsnrMO) throws JMSException,
			InterruptedException {

		System.out
				.println("**********************Starting Test : testMessageOrderingMultipleContexts**********************");
		lsnrMO.setMessgCount(5);
		lsnrMO.resetCounter();
		resetCounter();
		clearQueue(jmsQueueMO);

		int expectedCount = 5;
		int exceptionCount = 0;
		lsnrMO.setLock(lock, expectedCount, exceptionCount);
		int i = 0;
		int redFlag = 0;
		int msgOrder[] = new int[5];
		int msgRcvd = 0;

		JMSContext jmsContext = qcf.createContext();

		JMSContext ctx1 = qcf.createContext();
		JMSProducer msgProd1 = ctx1.createProducer();
		msgProd1.setAsync(lsnrMO);

		JMSContext ctx2 = qcf.createContext();
		JMSProducer msgProd2 = ctx2.createProducer();
		msgProd2.setAsync(lsnrMO);

		JMSContext ctx3 = qcf.createContext();
		JMSProducer msgProd3 = ctx3.createProducer();
		msgProd3.setAsync(lsnrMO);

		JMSContext ctx4 = qcf.createContext();
		JMSProducer msgProd4 = ctx4.createProducer();
		msgProd4.setAsync(lsnrMO);

		JMSConsumer messageConsumerMO = jmsContext.createConsumer(jmsQueueMO);

		Message msg0 = getMessage(0);
		msgProd1.send(jmsQueueMO, msg0);
		// Thread.sleep(100);
		msgRcvd = messageConsumerMO.receive(WAIT_TIME).getIntProperty(
				"Message_Order");
		msgOrder[0] = msgRcvd;
		System.out.println(msgOrder[0]);

		Message msg1 = getMessage(1);
		msgProd1.send(jmsQueueMO, msg1);
		// Thread.sleep(100);
		msgRcvd = messageConsumerMO.receive(WAIT_TIME).getIntProperty(
				"Message_Order");
		msgOrder[1] = msgRcvd;
		System.out.println(msgOrder[1]);

		Message msg2 = getMessage(2);
		msgProd2.send(jmsQueueMO, msg2);
		// Thread.sleep(100);
		msgRcvd = messageConsumerMO.receive(WAIT_TIME).getIntProperty(
				"Message_Order");
		msgOrder[2] = msgRcvd;
		System.out.println(msgOrder[2]);

		Message msg3 = getMessage(3);
		msgProd3.send(jmsQueueMO, msg3);
		// Thread.sleep(100);
		msgRcvd = messageConsumerMO.receive(WAIT_TIME).getIntProperty(
				"Message_Order");
		msgOrder[3] = msgRcvd;
		System.out.println(msgOrder[3]);

		Message msg4 = getMessage(4);
		msgProd4.send(jmsQueueMO, msg4);
		// Thread.sleep(100);
		msgRcvd = messageConsumerMO.receive(WAIT_TIME).getIntProperty(
				"Message_Order");
		msgOrder[4] = msgRcvd;
		System.out.println(msgOrder[4]);

		for (i = 0; i < 5; i++) {
			System.out.println("Retrieving Message Order:" + msgOrder[i]);
			if (msgOrder[i] == i)
				System.out.println("msgOrder:" + msgOrder[i]);
			else
				redFlag++;
		}

		synchronized (lock) {
			while (count != 5) {
				lock.wait(WAIT_TIME);
			}
		}

		int total_order = lsnrMO.getMessgOrderCount();
		System.out.println("TotalMessageOrder:" + total_order);
		boolean flag = false;
		if (redFlag == 0 && count == 5 && excount == 0 && total_order == 5) {
			flag = true;
			//Assert.assertTrue(flag);
			System.out
					.println("Test case :testMessageOrderingMultipleContexts passed");
		} else {
			System.out
					.println("Test case : testMessageOrderingMultipleContexts failed");
			//Assert.assertTrue(flag);
			try {
				throw new Exception("Condition check failed.");
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		clearQueue(jmsQueueMO);
		lsnrMO.resetCounter();
		resetCounter();
		jmsContext.close();
		System.out
				.println("**********************Ending Test : testMessageOrderingMultipleContexts**********************");

	}

	public void testMessageOrderingSyncAsyncMix(
			TestCompletionListener_MessageOrdering lsnrMO) throws Exception {

		System.out
				.println("**********************Starting Test : testMessageOrderingSyncAsyncMix**********************");
		clearQueue(jmsQueueMO);
		lsnrMO.setMessgCount(5);
		lsnrMO.resetCounter();
		resetCounter();
		int expectedCount = 5;
		int exceptionCount = 0;
		lsnrMO.setLock(lock, expectedCount, exceptionCount);
		int i = 0;
		int redFlag = 0;
		int msgOrder[] = new int[5];
		int msgRcvd = 0;
		System.out.println("Entering Async Block:");

		JMSContext jmsContext = qcf.createContext();

		JMSProducer messageProducerMO = jmsContext.createProducer();
		messageProducerMO.setAsync(lsnrMO);

		JMSConsumer messageConsumerMO = jmsContext.createConsumer(jmsQueueMO);

		for (i = 0; i < 5; i++) {

			Message msg = getMessage(i);
			messageProducerMO.send(jmsQueueMO, msg);
			// Thread.sleep(100);
			msgRcvd = messageConsumerMO.receive(WAIT_TIME).getIntProperty(
					"Message_Order");
			msgOrder[i] = msgRcvd;
			System.out.println(msgOrder[i]);

		}

		System.out.println("Exited Async Block:");

		int pos = 0;
		System.out.println("Entering Sync Block");
		for (i = 0; i < 5; i++) {

			Message msg = jmsContext.createMessage();
			msg.setStringProperty("Feature", "AsyncSend");
			msg.setIntProperty("Order", i);
			messageProducerMO.setAsync(null);
			messageProducerMO.send(jmsQueueMO, msg);

			Message rcvd = messageConsumerMO.receive(WAIT_TIME);
			int order = rcvd.getIntProperty("Order");
			String str = rcvd.getStringProperty("Feature");

			if (!(str.equals("AsyncSend") && order == i))
				pos++;

		}
		System.out.println("Exiting Sync Block");

		for (i = 0; i < 5; i++) {
			System.out.println("Retrieving Message Order:" + msgOrder[i]);
			if (msgOrder[i] == i)
				System.out.println("msgOrder:" + msgOrder[i]);
			else
				redFlag++;
		}

		synchronized (lock) {
			while (count != 5) {
				lock.wait(WAIT_TIME);
			}
		}

		int total_order = lsnrMO.getMessgOrderCount();
		System.out.println("TotalMessageOrder:" + total_order);
		boolean flag = false;
		if (redFlag == 0 && count == 5 && excount == 0 && total_order == 5
				&& pos == 0) {
			flag = true;
			//Assert.assertTrue(flag);

			System.out
					.println("Test case :testMessageOrderingSyncAsyncMix passed");
		}

		else {

			System.out
					.println("Test case : testMessageOrderingSyncAsyncMix failed");
			//Assert.assertTrue(flag);
			try {
				throw new Exception("Condition check failed.");
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		clearQueue(jmsQueueMO);

		lsnrMO.resetCounter();
		resetCounter();

		jmsContext.close();
		System.out
				.println("**********************Ending Test : testMessageOrderingSyncAsyncMix**********************");

	}

	// Case where the acknowledgement is not received, the JMS provider would
	// notify the application by invoking the CompletionListener onexception

	public void testExceptionMessageThreshhold(
			TestCompletionListener_MessageOrdering listnerMO, int numMsgs)
			throws JMSException {
		System.out
				.println("**********************Starting Test : testExceptionMessageThreshhold**********************");
		clearQueue(exQueue);

		listnerMO.resetCounter();
		resetCounter();
		int expectedCount = 5;
		int exceptionCount = 1;
		listnerMO.setLock(lock, expectedCount, exceptionCount);
		JMSContext jmsContext = qcf.createContext();
		int i = 0;
		try {

			JMSProducer jmsProducerMO = jmsContext.createProducer();
			jmsProducerMO.setAsync(listnerMO);

			for (i = 0; i <= numMsgs; i++) {
				Message msg = getMessage(i);
				jmsProducerMO.send(exQueue, msg);
				// Thread.sleep(1000);

			}

			System.out.println("excount before lock is :" + excount);

			synchronized (lock) {
				while (count != 5 && excount != 1) {
					lock.wait(WAIT_TIME);
				}
			}
			
			Thread.sleep(1000);

			System.out.println("excount after lock is:" + excount);

			boolean flag = false;

			if (count == 5 && excount == 1) {
				flag = true;
				//Assert.assertTrue(flag);
				System.out
						.println("Test case : testExceptionMessageThreshhold passed. ");
			} else {
				System.out
						.println("Test case : testExceptionMessageThreshhold failed. ");
				//Assert.assertTrue(flag);
				try {
					throw new Exception("Condition check failed.");
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

		} catch (Exception ex) {
			ex.printStackTrace();
		}

		clearQueue(exQueue);
		listnerMO.resetCounter();
		resetCounter();

		jmsContext.close();

		System.out
				.println("**********************Ending Test : testExceptionMessageThreshhold**********************");

	}

	public void testExceptionNEQueue(TestCompletionListener tcmpListner)
			throws JMSException, InterruptedException {
		System.out
				.println("**********************Starting Test : testExceptionNEQueue**********************");
		tcmpListner.resetCounter();
		resetCounter();
		clearQueue(jmsQueue);
		int expectedCount = 0;
		int exceptionCount = 1;
		tcmpListner.setLock(lock, expectedCount, exceptionCount);
		JMSContext jmsContext = qcf.createContext();
		Queue queue = jmsContext.createQueue("QUEUE4");
		JMSProducer jmsProducerMO = jmsContext.createProducer();

		jmsProducerMO.setAsync(tcmpListner);

		Message msg = getMessage(1);
		jmsProducerMO.send(queue, msg);

		
		synchronized (lock) {
			while (excount != 1) {
				lock.wait(WAIT_TIME);
			}
		}
		boolean flag = false;
		if (count == 0 && excount == 1) {

			flag = true;
			//Assert.assertTrue(flag);
			System.out.println("Test case : testExceptionNEQueue passed");
		} else {

			System.out.println("Test case : testExceptionNEQueue failed");
			//Assert.assertTrue(flag);
			try {
				throw new Exception("Condition check failed.");
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		tcmpListner.resetCounter();
		resetCounter();

		jmsContext.close();
		System.out
				.println("**********************Ending Test : testExceptionNEQueue**********************");

	}

	public void testJMSContextinCallBackMethodOC(
			TestCompletionListenerContext tcmpListner) throws JMSException,
			InterruptedException {

		System.out
				.println("**********************Starting Test : testJMSContextinCallBackMethodOC**********************");
		tcmpListner.reset();
		resetCounter();
		resetFlag();

		clearQueue(jmsQueue);

		int expectedCount = 1;
		int exceptionCount = 0;
		tcmpListner.setLock(lock, expectedCount, exceptionCount);

		JMSProducer jmsProducer = jmsContextCtx.createProducer();
		JMSConsumer jmsConsumer = jmsContextCtx.createConsumer(jmsQueue);

		jmsProducer.setAsync(tcmpListner);

		jmsProducer.send(jmsQueue, "This is a test with setAsync");
		// Thread.sleep(1000);

		synchronized (lock) {
			while (count != 1) {
				lock.wait(WAIT_TIME);
			}
		}
		jmsContextCtx.commit();

		System.out
				.println("Now the message has been sent. We will compare the message received");

		String msgRcvd = jmsConsumer.receiveBody(String.class, WAIT_TIME)
				.toString();

		System.out.println("Message Received is :" + msgRcvd);

		System.out.println(count);
		System.out.println(closeCtx);
		System.out.println(commitCtx);
		System.out.println(rollbackCtx);
		System.out.println(prodCtx);

		boolean flag = false;
		if (count == 1 && closeCtx == true && commitCtx == true
				&& rollbackCtx == true && prodCtx == true) {
			flag = true;
			//Assert.assertTrue(flag);
			System.out
					.println("Test Case: testJMSContextinCallBackMethodOC Passed");

		} else {
			System.out
					.println("Test Case: testJMSContextinCallBackMethodOC failed");
			//Assert.assertTrue(flag);
			try {
				throw new Exception("Condition check failed.");
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		clearQueue(jmsQueue);
		tcmpListner.reset();
		resetCounter();
		resetFlag();

		System.out
				.println("******************Ending Test : testJMSContextinCallBackMethodOC***************************");

	}

	public void testJMSContextinCallBackMethodOE(
			TestCompletionListenerContext tcmpListnerCtx1) throws JMSException,
			InterruptedException {

		System.out
				.println("**********************Starting Test : testJMSContextinCallBackMethodOE**********************");

		/*
		 * int expectedCount = 5; int exceptionCount = 1;
		 * tcmpListner.setLock(lock, expectedCount, exceptionCount);
		 */

		Queue exQ = jmsContextCtx.createQueue("exQUEUE1");

		JMSProducer jmsProducer = jmsContextCtx.createProducer();

		jmsProducer.setAsync(tcmpListnerCtx1);

		for (int i = 0; i < 6; i++) {
			jmsProducer.send(exQ, "Hello");
			Thread.sleep(100);
		}

		/*
		 * jmsProducer.send(exQueue, "Hello");
		 * 
		 * synchronized (lock) { while (count != 5 && excount != 1) {
		 * lock.wait(WAIT_TIME); } }
		 */
		boolean flag = false;
		if (count == 5 && excount == 1 && prodCtx == true) {
			flag = true;
			//Assert.assertTrue(flag);
			System.out
					.println("Test case: testJMSContextinCallBackMethodOE passed");
		} else {
			System.out
					.println("Test case: testJMSContextinCallBackMethodOE failed");
			//Assert.assertTrue(flag);
			try {
				throw new Exception("Condition check failed.");
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		clearQueue(exQ);

		tcmpListnerCtx1.reset();
		resetCounter();
		resetFlag();

		System.out
				.println("******************Ending Test : testJMSContextinCallBackMethodOE***************************");

	}

	public void testStreamMessageTypesAOC(TestCompletionListener tcmpListner)
			throws JMSException, InterruptedException {

		System.out
				.println("**********************Starting Test : testStreamMessageTypesAOC**********************");
		tcmpListner.resetCounter();
		resetCounter();
		clearQueue(jmsQueue);
		int expectedCount = 1;
		int exceptionCount = 0;
		tcmpListner.setLock(lock, expectedCount, exceptionCount);

		JMSContext jmsContext = qcf.createContext();

		StreamMessage smsg = jmsContext.createStreamMessage();
		smsg.setBooleanProperty("Value", true);
		smsg.setJMSCorrelationID("CORREL");

		JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);
		JMSProducer messageProducer = jmsContext.createProducer();
		messageProducer.setAsync(tcmpListner);

		messageProducer.send(jmsQueue, smsg);
		// Thread.sleep(1000);

		synchronized (lock) {
			while (count != 1) {
				lock.wait(WAIT_TIME);
			}
		}

		smsg.setBooleanProperty("Value", false);

		StreamMessage msgRcvd = (StreamMessage) jmsConsumer.receive(WAIT_TIME);

		String corrID = msgRcvd.getJMSCorrelationID();

		boolean propertyVal = false;

		if (msgRcvd.getBooleanProperty("Value") == true
				&& corrID.equals("CORREL"))
			propertyVal = true;
		System.out.println("prop" + propertyVal);
		messageProducer.send(jmsQueue, smsg);

		// Thread.sleep(1000);

		tcmpListner.setLock(lock, expectedCount, exceptionCount);

		synchronized (lock) {
			while (count != 2) {
				lock.wait(WAIT_TIME);
			}
		}

		boolean flag = false;
		msgRcvd = (StreamMessage) jmsConsumer.receive(WAIT_TIME);
		if (msgRcvd.getBooleanProperty("Value") == false)
			flag = true;
		System.out.println("Flag" + flag);

		boolean flagT = false;
		if (propertyVal == true && flag == true && count == 2) {
			flagT = true;
			//Assert.assertTrue(flagT);

			System.out.println("Test case : testStreamMessageTypesAOC passed");
		} else {
			System.out.println("Test case : testStreamMessageTypesAOC failed");
			//Assert.assertTrue(flag);
			try {
				throw new Exception("Condition check failed.");
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		clearQueue(jmsQueue);
		tcmpListner.resetCounter();
		resetCounter();
		jmsContext.close();
		System.out
				.println("******************Ending Test : testStreamMessageTypesAOC***************************");

	}

	public void testBytesMessageTypesAOC(TestCompletionListener tcmpListner)
			throws JMSException, InterruptedException {

		System.out
				.println("**********************Starting Test : testBytesMessageTypesAOC**********************");
		clearQueue(jmsQueue);
		int expectedCount = 1;
		int exceptionCount = 0;
		tcmpListner.setLock(lock, expectedCount, exceptionCount);

		JMSContext jmsContext = qcf.createContext();
		BytesMessage bmsg = jmsContext.createBytesMessage();
		bmsg.setBooleanProperty("Value", true);
		bmsg.setJMSCorrelationID("CORREL");

		JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);
		JMSProducer messageProducer = jmsContext.createProducer();

		messageProducer.setAsync(tcmpListner);
		messageProducer.send(jmsQueue, bmsg);
		// Thread.sleep(100);

		synchronized (lock) {
			while (count != 1) {
				lock.wait(WAIT_TIME);
			}
		}

		bmsg.setBooleanProperty("Value", false);
		BytesMessage msgRcvd = (BytesMessage) jmsConsumer.receive(WAIT_TIME);

		String corrID = msgRcvd.getJMSCorrelationID();

		boolean propertyVal = false;

		if (msgRcvd.getBooleanProperty("Value") == true
				&& corrID.equals("CORREL"))
			propertyVal = true;
		System.out.println("prop" + propertyVal);
		messageProducer.send(jmsQueue, bmsg);

		// Thread.sleep(1000);

		tcmpListner.setLock(lock, expectedCount, exceptionCount);

		synchronized (lock) {
			while (count != 2) {
				lock.wait(WAIT_TIME);
			}
		}
		boolean flag = false;
		msgRcvd = (BytesMessage) jmsConsumer.receive(WAIT_TIME);
		if (msgRcvd.getBooleanProperty("Value") == false)
			flag = true;
		System.out.println("Flag" + flag);

		boolean flagT = false;
		if (propertyVal == true && flag == true && count == 2) {
			flagT = true;
			//Assert.assertTrue(flagT);

			System.out.println("Test case : testBytesMessageTypesAOC passed");
		} else {
			System.out.println("Test case : testBytesMessageTypesAOC failed");
			//Assert.assertTrue(flagT);
			try {
				throw new Exception("Condition check failed.");
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		clearQueue(jmsQueue);
		tcmpListner.resetCounter();
		resetCounter();
		jmsContext.close();
		System.out
				.println("******************Ending Test : testBytesMessageTypesAOC***************************");

	}

	public void testTextMessageTypesAOC(TestCompletionListener tcmpListner)
			throws JMSException, InterruptedException {

		System.out
				.println("**********************Starting Test : testTextMessageTypesAOC**********************");
		clearQueue(jmsQueue);
		int expectedCount = 1;
		int exceptionCount = 0;
		tcmpListner.setLock(lock, expectedCount, exceptionCount);

		JMSContext jmsContext = qcf.createContext();
		TextMessage tmsg = jmsContext.createTextMessage();
		tmsg.setBooleanProperty("Value", true);
		tmsg.setJMSCorrelationID("CORREL");

		JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);
		JMSProducer messageProducer = jmsContext.createProducer();
		messageProducer.setAsync(tcmpListner);
		messageProducer.send(jmsQueue, tmsg);
		// Thread.sleep(100);

		synchronized (lock) {
			while (count != 1) {
				lock.wait(WAIT_TIME);
			}
		}

		tmsg.setBooleanProperty("Value", false);
		TextMessage msgRcvd = (TextMessage) jmsConsumer.receive(WAIT_TIME);

		String corrID = msgRcvd.getJMSCorrelationID();

		boolean propertyVal = false;

		if (msgRcvd.getBooleanProperty("Value") == true
				&& corrID.equals("CORREL"))
			propertyVal = true;
		System.out.println("prop" + propertyVal);
		messageProducer.send(jmsQueue, tmsg);

		// Thread.sleep(1000);
		tcmpListner.setLock(lock, expectedCount, exceptionCount);

		synchronized (lock) {
			while (count != 2) {
				lock.wait(WAIT_TIME);
			}
		}

		boolean flag = false;
		msgRcvd = (TextMessage) jmsConsumer.receive(WAIT_TIME);
		if (msgRcvd.getBooleanProperty("Value") == false)
			flag = true;
		System.out.println("Flag" + flag);

		boolean flagT = false;
		if (propertyVal == true && flag == true && count == 2) {
			flagT = true;
			//Assert.assertTrue(flagT);

			System.out.println("Test case : testTextMessageTypesAOC passed");
		} else {
			System.out.println("Test case : testTextMessageTypesAOC failed");
			//Assert.assertTrue(flagT);
			try {
				throw new Exception("Condition check failed.");
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		clearQueue(jmsQueue);
		tcmpListner.resetCounter();
		resetCounter();
		jmsContext.close();
		System.out
				.println("******************Ending Test : testTextMessageTypesAOC***************************");

	}

	public void testObjectMessageTypesAOC(TestCompletionListener tcmpListner)
			throws JMSException, InterruptedException {

		System.out
				.println("**********************Starting Test : testObjectMessageTypesAOC**********************");
		clearQueue(jmsQueue);
		int expectedCount = 1;
		int exceptionCount = 0;
		tcmpListner.setLock(lock, expectedCount, exceptionCount);

		JMSContext jmsContext = qcf.createContext();
		ObjectMessage obmsg = jmsContext.createObjectMessage();
		obmsg.setBooleanProperty("Value", true);
		obmsg.setJMSCorrelationID("CORREL");
		JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);
		JMSProducer messageProducer = jmsContext.createProducer();
		messageProducer.setAsync(tcmpListner);
		messageProducer.send(jmsQueue, obmsg);
		// Thread.sleep(100);

		synchronized (lock) {
			while (count != 1) {
				lock.wait(WAIT_TIME);
			}
		}

		obmsg.setBooleanProperty("Value", false);
		ObjectMessage msgRcvd = (ObjectMessage) jmsConsumer.receive(WAIT_TIME);

		String corrID = msgRcvd.getJMSCorrelationID();

		boolean propertyVal = false;

		if (msgRcvd.getBooleanProperty("Value") == true
				&& corrID.equals("CORREL"))
			propertyVal = true;
		System.out.println("prop" + propertyVal);
		messageProducer.send(jmsQueue, obmsg);

		// Thread.sleep(1000);

		tcmpListner.setLock(lock, expectedCount, exceptionCount);

		synchronized (lock) {
			while (count != 2) {
				lock.wait(WAIT_TIME);
			}
		}

		boolean flag = false;
		msgRcvd = (ObjectMessage) jmsConsumer.receive(WAIT_TIME);
		if (msgRcvd.getBooleanProperty("Value") == false)
			flag = true;
		System.out.println("Flag" + flag);
		boolean flagT = false;
		if (propertyVal == true && flag == true && count == 2) {
			flagT = true;
			//Assert.assertTrue(flagT);
			System.out.println("Test case : testObjectMessageTypesAOC passed");
		} else {
			System.out.println("Test case : testObjectMessageTypesAOC failed");
			//Assert.assertTrue(flagT);
			try {
				throw new Exception("Condition check failed.");
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		clearQueue(jmsQueue);
		tcmpListner.resetCounter();
		resetCounter();
		jmsContext.close();

		System.out
				.println("******************Ending Test : testObjectMessageTypesAOC***************************");

	}

	public void testMapMessageTypesAOC(TestCompletionListener tcmpListner)
			throws JMSException, InterruptedException {

		System.out
				.println("**********************Starting Test : testMapMessageTypesAOC**********************");
		clearQueue(jmsQueue);
		int expectedCount = 1;
		int exceptionCount = 0;
		tcmpListner.setLock(lock, expectedCount, exceptionCount);

		JMSContext jmsContext = qcf.createContext();
		MapMessage mapmsg = jmsContext.createMapMessage();
		mapmsg.setBooleanProperty("Value", true);
		mapmsg.setJMSCorrelationID("CORREL");

		JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);
		JMSProducer messageProducer = jmsContext.createProducer();
		messageProducer.setAsync(tcmpListner);
		messageProducer.send(jmsQueue, mapmsg);
		// Thread.sleep(100);

		synchronized (lock) {
			while (count != 1) {
				lock.wait(WAIT_TIME);
			}
		}

		mapmsg.setBooleanProperty("Value", false);
		MapMessage msgRcvd = (MapMessage) jmsConsumer.receive(WAIT_TIME);

		String corrID = msgRcvd.getJMSCorrelationID();

		boolean propertyVal = false;

		if (msgRcvd.getBooleanProperty("Value") == true
				&& corrID.equals("CORREL"))
			propertyVal = true;
		System.out.println("prop" + propertyVal);
		messageProducer.send(jmsQueue, mapmsg);

		// Thread.sleep(1000);
		tcmpListner.setLock(lock, expectedCount, exceptionCount);

		synchronized (lock) {
			while (count != 2) {
				lock.wait(WAIT_TIME);
			}
		}

		boolean flag = false;
		msgRcvd = (MapMessage) jmsConsumer.receive(WAIT_TIME);
		if (msgRcvd.getBooleanProperty("Value") == false)
			flag = true;
		System.out.println("Flag" + flag);
		boolean flagT = false;
		if (propertyVal == true && flag == true && count == 2) {
			flagT = true;
			//Assert.assertTrue(flagT);

			System.out.println("Test case : testMapMessageTypesAOC passed");
		} else {
			System.out.println("Test case : testMapMessageTypesAOC failed");
			//Assert.assertTrue(flagT);
			try {
				throw new Exception("Condition check failed.");
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		clearQueue(jmsQueue);
		tcmpListner.resetCounter();
		resetCounter();
		jmsContext.close();

		System.out
				.println("******************Ending Test : testMapMessageTypesAOC***************************");

	}

	public void testStreamMessageTypesBOC(
			TestCompletionListener_MessageOrdering listnerMO)
			throws JMSException, InterruptedException {

		System.out
				.println("**********************Starting Test : testStreamMessageTypesBOC**********************");
		clearQueue(jmsQueueMO);
		listnerMO.resetCounter();
		resetCounter();

		int expectedCount = 1000;
		int exceptionCount = 0;
		listnerMO.setLock(lock, expectedCount, exceptionCount);
		final StreamMessage[] smArray = new StreamMessage[1000];

		JMSContext jmsContext = qcf.createContext();
		JMSProducer messageProducerMO = jmsContext.createProducer();
		messageProducerMO.setAsync(listnerMO);
		boolean val1 = false;

		boolean val3 = false;
		for (int i = 0; i < 1000; i++) {
			smArray[i] = jmsContext.createStreamMessage();
			smArray[i].setBooleanProperty("Value", true);
			smArray[i].setJMSCorrelationID("CORREL");
			System.out.println("Setting Message order:" + i);
			smArray[i].setIntProperty("Message_Order", i);
			System.out.println("Sending message number:" + i);
			messageProducerMO.send(jmsQueueMO, smArray[i]);

			try {
				if (i == 2) {
					System.out.println("This is the 2nd message");
					System.out.println(smArray[i].getBooleanProperty("Value"));
					System.out.println(smArray[i].getJMSCorrelationID());

				}
			} catch (JMSException ex) {
				ex.printStackTrace();
				val1 = true;

			}

			try {
				if (i == 999) {
					System.out.println("This is the 999th message");
					System.out.println(smArray[i].getBooleanProperty("Value"));
					System.out.println(smArray[i].getJMSCorrelationID());

				}
			} catch (JMSException ex) {
				ex.printStackTrace();
				val3 = true;
			}

			// Thread.sleep(10);

		}

		// Thread.sleep(5);

		synchronized (lock) {
			while (count != 1000) {
				lock.wait(WAIT_TIME);
			}
		}

		System.out.println(val1 + ":" + val3 + ":" + count);

		boolean flagT = false;

		if (val1 == true && val3 == true && count == 1000) {
			flagT = true;
			//Assert.assertTrue(flagT);

			System.out.println("Test case : testStreamMessageTypesBOC passed");
		} else {
			System.out.println("Test case : testStreamMessageTypesBOC failed");
			//Assert.assertTrue(flagT);
			try {
				throw new Exception("Condition check failed.");
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		clearQueue(jmsQueueMO);
		listnerMO.resetCounter();
		resetCounter();
		jmsContext.close();
		System.out
				.println("******************Ending Test : testStreamMessageTypesBOC***************************");

	}

	public void testBytesMessageTypesBOC(
			TestCompletionListener_MessageOrdering listnerMO)
			throws JMSException, InterruptedException {

		System.out
				.println("**********************Starting Test : testBytesMessageTypesBOC**********************");
		clearQueue(jmsQueueMO);
		listnerMO.resetCounter();
		resetCounter();
		final BytesMessage[] smArray = new BytesMessage[1000];

		int expectedCount = 1000;
		int exceptionCount = 0;
		listnerMO.setLock(lock, expectedCount, exceptionCount);

		JMSContext jmsContext = qcf.createContext();
		JMSProducer messageProducerMO = jmsContext.createProducer();
		messageProducerMO.setAsync(listnerMO);
		boolean val1 = false;

		boolean val3 = false;
		for (int i = 0; i < 1000; i++) {
			smArray[i] = jmsContext.createBytesMessage();
			smArray[i].setBooleanProperty("Value", true);
			smArray[i].setJMSCorrelationID("CORREL");
			System.out.println("Setting Message order:" + i);
			smArray[i].setIntProperty("Message_Order", i);
			System.out.println("Sending message number:" + i);
			messageProducerMO.send(jmsQueueMO, smArray[i]);

			try {
				if (i == 2) {
					System.out.println("This is the 2nd message");
					System.out.println(smArray[i].getBooleanProperty("Value"));
					System.out.println(smArray[i].getJMSCorrelationID());

				}
			} catch (JMSException ex) {
				ex.printStackTrace();
				val1 = true;

			}

			try {
				if (i == 999) {
					System.out.println("This is the 999th message");
					System.out.println(smArray[i].getBooleanProperty("Value"));
					System.out.println(smArray[i].getJMSCorrelationID());

				}
			} catch (JMSException ex) {
				ex.printStackTrace();
				val3 = true;
			}

			// Thread.sleep(10);

		}

		// Thread.sleep(5);

		synchronized (lock) {
			while (count != 1000) {
				lock.wait(WAIT_TIME);
			}
		}

		System.out.println(val1 + ":" + val3 + ":" + count);

		boolean flagT = false;
		if (val1 == true && val3 == true && count == 1000) {
			flagT = true;
			//Assert.assertTrue(flagT);

			System.out.println("Test case : testBytesMessageTypesBOC passed");

		} else {
			System.out.println("Test case : testBytesMessageTypesBOC failed");
			//Assert.assertTrue(flagT);
			try {
				throw new Exception("Condition check failed.");
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		clearQueue(jmsQueueMO);
		System.out
				.println("******************Ending Test : testBytesMessageTypesBOC***************************");

		listnerMO.resetCounter();
		resetCounter();
	}

	public void testObjectMessageTypesBOC(
			TestCompletionListener_MessageOrdering listnerMO)
			throws JMSException, InterruptedException {

		System.out
				.println("**********************Starting Test : testObjectMessageTypesBOC**********************");
		clearQueue(jmsQueueMO);
		listnerMO.resetCounter();
		resetCounter();

		int expectedCount = 1000;
		int exceptionCount = 0;
		listnerMO.setLock(lock, expectedCount, exceptionCount);
		final ObjectMessage[] smArray = new ObjectMessage[1000];

		JMSContext jmsContext = qcf.createContext();
		JMSProducer messageProducerMO = jmsContext.createProducer();
		messageProducerMO.setAsync(listnerMO);
		boolean val1 = false;

		boolean val3 = false;
		for (int i = 0; i < 1000; i++) {
			smArray[i] = jmsContext.createObjectMessage();
			smArray[i].setBooleanProperty("Value", true);
			smArray[i].setJMSCorrelationID("CORREL");
			System.out.println("Setting Message order:" + i);
			smArray[i].setIntProperty("Message_Order", i);
			System.out.println("Sending message number:" + i);
			messageProducerMO.send(jmsQueueMO, smArray[i]);

			try {
				if (i == 2) {
					System.out.println("This is the 2nd message");
					System.out.println(smArray[i].getBooleanProperty("Value"));
					System.out.println(smArray[i].getJMSCorrelationID());

				}
			} catch (JMSException ex) {
				ex.printStackTrace();
				val1 = true;

			}

			try {
				if (i == 999) {
					System.out.println("This is the 999th message");
					System.out.println(smArray[i].getBooleanProperty("Value"));
					System.out.println(smArray[i].getJMSCorrelationID());

				}
			} catch (JMSException ex) {
				ex.printStackTrace();
				val3 = true;
			}

			// Thread.sleep(10);

		}

		// Thread.sleep(5);

		synchronized (lock) {
			while (count != 1000) {
				lock.wait(WAIT_TIME);
			}
		}

		System.out.println(val1 + ":" + val3 + ":" + count);

		boolean flagT = false;
		if (val1 == true && val3 == true && count == 1000) {
			flagT = true;
			//Assert.assertTrue(flagT);

			System.out.println("Test case : testObjectMessageTypesBOC passed");

		} else {
			System.out.println("Test case : testObjectMessageTypesBOC failed");
			//Assert.assertTrue(flagT);
			try {
				throw new Exception("Condition check failed.");
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		clearQueue(jmsQueueMO);
		System.out
				.println("******************Ending Test : testObjectMessageTypesBOC***************************");

		listnerMO.resetCounter();
		resetCounter();
	}

	public void testMapMessageTypesBOC(
			TestCompletionListener_MessageOrdering listnerMO)
			throws JMSException, InterruptedException {

		System.out
				.println("**********************Starting Test : testMapMessageTypesBOC**********************");
		clearQueue(jmsQueueMO);
		listnerMO.resetCounter();
		resetCounter();

		final MapMessage[] smArray = new MapMessage[1000];

		int expectedCount = 1000;
		int exceptionCount = 0;
		listnerMO.setLock(lock, expectedCount, exceptionCount);
		JMSContext jmsContext = qcf.createContext();
		JMSProducer messageProducerMO = jmsContext.createProducer();
		messageProducerMO.setAsync(listnerMO);
		boolean val1 = false;

		boolean val3 = false;
		for (int i = 0; i < 1000; i++) {
			smArray[i] = jmsContext.createMapMessage();
			smArray[i].setBooleanProperty("Value", true);
			smArray[i].setJMSCorrelationID("CORREL");
			System.out.println("Setting Message order:" + i);
			smArray[i].setIntProperty("Message_Order", i);
			System.out.println("Sending message number:" + i);
			messageProducerMO.send(jmsQueueMO, smArray[i]);

			try {
				if (i == 2) {
					System.out.println("This is the 2nd message");
					System.out.println(smArray[i].getBooleanProperty("Value"));
					System.out.println(smArray[i].getJMSCorrelationID());

				}
			} catch (JMSException ex) {
				ex.printStackTrace();
				val1 = true;

			}

			try {
				if (i == 999) {
					System.out.println("This is the 999th message");
					System.out.println(smArray[i].getBooleanProperty("Value"));
					System.out.println(smArray[i].getJMSCorrelationID());

				}
			} catch (JMSException ex) {
				ex.printStackTrace();
				val3 = true;
			}

			// Thread.sleep(10);

		}

		// Thread.sleep(5);

		synchronized (lock) {
			while (count != 1000) {
				lock.wait(WAIT_TIME);
			}
		}

		System.out.println(val1 + ":" + val3 + ":" + count);

		boolean flagT = false;
		if (val1 == true && val3 == true && count == 1000) {
			flagT = true;
			//Assert.assertTrue(flagT);
			System.out.println("Test case : testMapMessageTypesBOC passed");

		} else {
			System.out.println("Test case : testMapMessageTypesBOC failed");
			//Assert.assertTrue(flagT);
			try {
				throw new Exception("Condition check failed.");
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		clearQueue(jmsQueueMO);
		listnerMO.resetCounter();
		resetCounter();
		System.out
				.println("******************Ending Test : testMapMessageTypesBOC***************************");

	}

	public void testTextMessageTypesBOC(
			TestCompletionListener_MessageOrdering listnerMO)
			throws JMSException, InterruptedException {

		System.out
				.println("**********************Starting Test : testTextMessageTypesBOC**********************");
		clearQueue(jmsQueueMO);
		listnerMO.resetCounter();
		resetCounter();

		final TextMessage[] smArray = new TextMessage[1000];

		int expectedCount = 1000;
		int exceptionCount = 0;
		listnerMO.setLock(lock, expectedCount, exceptionCount);
		JMSContext jmsContext = qcf.createContext();

		JMSProducer messageProducerMO = jmsContext.createProducer();
		messageProducerMO.setAsync(listnerMO);
		boolean val1 = false;

		boolean val3 = false;
		for (int i = 0; i < 1000; i++) {
			smArray[i] = jmsContext.createTextMessage();
			smArray[i].setBooleanProperty("Value", true);
			smArray[i].setJMSCorrelationID("CORREL");
			System.out.println("Setting Message order:" + i);
			smArray[i].setIntProperty("Message_Order", i);
			System.out.println("Sending message number:" + i);
			messageProducerMO.send(jmsQueueMO, smArray[i]);

			try {
				if (i == 2) {
					System.out.println("This is the 2nd message");
					System.out.println(smArray[i].getBooleanProperty("Value"));
					System.out.println(smArray[i].getJMSCorrelationID());

				}
			} catch (JMSException ex) {
				ex.printStackTrace();
				val1 = true;

			}

			try {
				if (i == 999) {
					System.out.println("This is the 999th message");
					System.out.println(smArray[i].getBooleanProperty("Value"));
					System.out.println(smArray[i].getJMSCorrelationID());

				}
			} catch (JMSException ex) {
				ex.printStackTrace();
				val3 = true;
			}

			// Thread.sleep(10);

		}

		// Thread.sleep(5);

		synchronized (lock) {
			while (count != 1000) {
				lock.wait(WAIT_TIME);
			}
		}

		System.out.println(val1 + ":" + val3 + ":" + count);

		boolean flagT = false;

		if (val1 == true && val3 == true && count == 1000) {
			flagT = true;
			//Assert.assertTrue(flagT);

			System.out.println("Test case : testTextMessageTypesBOC passed");
		} else {

			System.out.println("Test case : testTextMessageTypesBOC failed");
			//Assert.assertTrue(flagT);
			try {
				throw new Exception("Condition check failed.");
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		clearQueue(jmsQueueMO);
		listnerMO.resetCounter();
		resetCounter();
		jmsContext.close();
		System.out
				.println("******************Ending Test : testTextMessageTypesBOC***************************");

	}

	public void testMessageOnException(TestCompletionListener tcmpListner)
			throws Exception, MessageNotWriteableException, JMSException {

		int expectedCount = 5;
		int exceptionCount = 1;
		tcmpListner.setLock(lock, expectedCount, exceptionCount);

		clearQueue(exQueue);
		tcmpListner.resetCounter();
		resetCounter();

		System.out
				.println("******************Starting Test : testMessageOnException***************************");

		int maxMessageDepth = 5;
		JMSContext jmsContext = qcf.createContext();
		TextMessage textmsg = jmsContext.createTextMessage();
		textmsg.setBooleanProperty("Value", true);
		textmsg.setJMSCorrelationID("CORREL");
		JMSProducer messageProducer = jmsContext.createProducer();
		messageProducer.setAsync(tcmpListner);

		for (int i = 0; i < maxMessageDepth; i++) {

			System.out.println("Sending message number:" + i);
			messageProducer.send(exQueue, textmsg);

		}
		System.out.println("-----Count after first send ----------:" + count);
		System.out.println("-----exCount after first send ----------:" + excount);
		boolean flag = false;
		messageProducer.send(exQueue, textmsg);
		
		System.out.println("-----Count after second send ----------:" + count);
		System.out.println("-----exCount after second send ----------:" + excount);
		try {
			textmsg.setIntProperty("order", 1);
		} catch (JMSException ex) {
			ex.printStackTrace();
			flag = true;
		}

		System.out.println("-----Count before Sync lock ----------:" + count);
		System.out.println("-----exCount before Sync lock ----------:" + excount);
		System.out.println("-----flag before Sync lock ----------:" + flag);
		
		synchronized (lock) {
			while (count != 5 && excount != 1) {
				lock.wait(WAIT_TIME);
			}
		}

		System.out.println("-----Count after Sync lock ----------:" + count);
		System.out.println("-----exCount after Sync lock ----------:" + excount);
		System.out.println("-----flag after Sync lock ----------:" + flag);
		
		boolean flagT = false;
		if (count == 5 && excount == 1 && flag == true) {
			flagT = true;
			//Assert.assertTrue(flagT);

			System.out.println("Test case : testMessageOnException passed.");
		} else {
			System.out.println("Test case : testMessageOnException failed.");
			//Assert.assertTrue(flagT);
			try {
				throw new Exception("Condition check failed.");
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		clearQueue(exQueue);
		tcmpListner.resetCounter();
		resetCounter();
		jmsContext.close();
		System.out
				.println("******************Ending Test : testMessageOnException***************************");

	}

	// For a given JMSContext, callbacks (both onCompletion and onException)
	// will be performed in the same order as the corresponding calls to the
	// asynchronous send method.

	public void testOnCompletionOnException(TestCompletionListener tcmpListner)
			throws JMSException, InterruptedException {

		int expectedCount = 5;
		int exceptionCount = 1;
		tcmpListner.setLock(lock, expectedCount, exceptionCount);
		clearQueue(exQueue);
		tcmpListner.resetCounter();
		resetCounter();
		System.out
				.println("**********************Starting Test : testOnCompletionOnException**********************");

		// Queue queue =
		// GetJMSResources.getQueueSession().createQueue("QUEUE4");

		JMSContext jmsContext = qcf.createContext();
		JMSProducer msgProducer = jmsContext.createProducer();
		msgProducer.setAsync(tcmpListner);
		JMSConsumer msgConsumer = jmsContext.createConsumer(exQueue);

		for (int i = 0; i < 5; i++) {
			TextMessage tmessg = jmsContext.createTextMessage("Hello");
			tmessg.setText("Hello");
			msgProducer.send(exQueue, tmessg);

		}

		synchronized (lock) {
			while (count != 5) {
				lock.wait(WAIT_TIME);
			}
		}

		boolean flag = false;
		if (count == 5 && excount == 0)
			flag = true;

		TextMessage tmessg = jmsContext.createTextMessage("Hello");
		msgProducer.send(exQueue, tmessg);

		// Thread.sleep(1000);

		//int num = 0;
		

		/*for (int i = 5; i > 0; i--) {
			TextMessage msgRcvd = (TextMessage) msgConsumer.receive(WAIT_TIME);

			if (msgRcvd.getText().equals("Hello"))
				num++;
		}*/

		boolean flagT = false;

		synchronized (lock) {
			while (excount != 1) {
				lock.wait(WAIT_TIME);
			}
		}
		if (flag == true && excount == 1 ) {
			flagT = true;
			//Assert.assertTrue(flagT);
			System.out
					.println("Test case : testOnCompletionOnException passed ");

		} else {
			System.out
					.println("test case : testOnCompletionOnException failed");
			//Assert.assertTrue(flagT);
			try {
				throw new Exception("Condition check failed.");
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		clearQueue(jmsQueue);
		tcmpListner.resetCounter();
		resetCounter();
		jmsContext.close();

		System.out
				.println("******************Ending Test : testOnCompletionOnException***************************");

	}

	public void testCallingNativeContext_unrelatedContext(
			TestCompletionListenerVariationUR listnrV) throws JMSException,
			InterruptedException {

		System.out
				.println("Starting Test : testCallingNativeContext_unrelatedContext");

		listnrV.resetCounter();
		resetFlag();
		resetCounter();
		clearQueue(jmsQueue);

		int expectedCount = 1;
		int exceptionCount = 0;
		listnrV.setLock(lock, expectedCount, exceptionCount);
		// JMSContext jmsContextTransacted =
		// qcf.createContext(Session.SESSION_TRANSACTED);
		Message message = jmsContextTransacted.createTextMessage("Hello World");

		JMSProducer jmsProducerTrans = jmsContextTransacted.createProducer();
		jmsProducerTrans.setAsync(listnrV);

		jmsProducerTrans.send(jmsQueue, message);
		// Thread.sleep(1000);

		synchronized (lock) {
			while (count != 1) {
				lock.wait(WAIT_TIME);
			}
		}

		jmsContextTransacted.commit();

		QueueBrowser qb = jmsContextTransacted.createBrowser(jmsQueue);
		Enumeration e1 = qb.getEnumeration();

		int numMsgs = 0;
		// count number of messages
		while (e1.hasMoreElements()) {
			e1.nextElement();
			numMsgs++;
		}
		// Thread.sleep(1000);

		System.out.println("Count:" + count);
		System.out.println("exCount:" + excount);
		System.out.println("numMsgs:" + numMsgs);
		System.out.println("closeUR:" + closeUR);
		System.out.println("commitUR:" + commitUR);
		System.out.println("rollbackUR:" + rollbackUR);

		boolean flagT = false;
		if (closeUR == true && commitUR == true && rollbackUR == true
				&& count == 1 && excount == 0 && numMsgs == 1) {
			flagT = true;
			//Assert.assertTrue(flagT);

			System.out
					.println("Test case : testCallingNativeContext_unrelatedContext passed");
		} else {
			System.out
					.println("Test case : testCallingNativeContext_unrelatedContext failed");
			//Assert.assertTrue(flagT);
			try {
				throw new Exception("Condition check failed.");
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		clearQueue(jmsQueue);

		listnrV.resetCounter();
		resetFlag();
		resetCounter();

		System.out
				.println("Ending Test : testCallingNativeContext_unrelatedContext");

	}

	public void testGetAsync(TestCompletionListener tcmpListner) {
		boolean flag1 = false;
		boolean flag2 = false;
		System.out.println("Starting Test : testGetAsync");
		System.out.println(jmsProducer);

		JMSContext jmsContext = qcf.createContext();
		JMSProducer jmsProducer = jmsContext.createProducer();
		jmsProducer.setAsync(tcmpListner);
		if (jmsProducer.getAsync() == tcmpListner)
			flag1 = true;
		jmsProducer.setAsync(null);

		if (jmsProducer.getAsync() == null)
			flag2 = true;

		boolean flagT = false;
		if (flag1 == true && flag2 == true) {

			flagT = true;
			//Assert.assertTrue(flagT);
			System.out.println("Test case :testGetAsync passed ");
		} else {
			System.out.println("Test case :testGetAsync failed ");
			//Assert.assertTrue(flagT);
			try {
				throw new Exception("Condition check failed.");
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		System.out.println("Ending Test : testGetAsync");

		jmsContext.close();

	}

	public void testClose(TestCompletionListener tListner) throws JMSException,
			InterruptedException {

		System.out.println("Starting Test :testClose");

		tListner.resetCounter();
		resetCounter();
		clearQueue(jmsQueue);
		int expectedCount = 100;
		int exceptionCount = 0;
		tListner.setLock(lock, expectedCount, exceptionCount);

		QueueConnectionFactory qcfLocal = GetJMSResources.getQCF();
		JMSContext ctxLocal = qcfLocal.createContext();
		String text = "This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.vThis is a test with setAsync.This is a test with setAsync.This is a test with setAsync.vvvThis is a test with setAsync.This is a test with setAsync.vvThis is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.vThis is a test with setAsync.This is a test with setAsync.This is a test with setAsync.vvvThis is a test with setAsync.This is a test with setAsync.vvThis is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync. This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.vThis is a test with setAsync.This is a test with setAsync.This is a test with setAsync.vvvThis is a test with setAsync.This is a test with setAsync.vvThis is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync. This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.vThis is a test with setAsync.This is a test with setAsync.This is a test with setAsync.vvvThis is a test with setAsync.This is a test with setAsync.vvThis is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync. This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.vThis is a test with setAsync.This is a test with setAsync.This is a test with setAsync.vvvThis is a test with setAsync.This is a test with setAsync.vvThis is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync. This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.vThis is a test with setAsync.This is a test with setAsync.This is a test with setAsync.vvvThis is a test with setAsync.This is a test with setAsync.vvThis is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync. This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.vThis is a test with setAsync.This is a test with setAsync.This is a test with setAsync.vvvThis is a test with setAsync.This is a test with setAsync.vvThis is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync. This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.vThis is a test with setAsync.This is a test with setAsync.This is a test with setAsync.vvvThis is a test with setAsync.This is a test with setAsync.vvThis is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync. This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.vThis is a test with setAsync.This is a test with setAsync.This is a test with setAsync.vvvThis is a test with setAsync.This is a test with setAsync.vvThis is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync. This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.vThis is a test with setAsync.This is a test with setAsync.This is a test with setAsync.vvvThis is a test with setAsync.This is a test with setAsync.vvThis is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync. This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.vThis is a test with setAsync.This is a test with setAsync.This is a test with setAsync.vvvThis is a test with setAsync.This is a test with setAsync.vvThis is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync. This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.vThis is a test with setAsync.This is a test with setAsync.This is a test with setAsync.vvvThis is a test with setAsync.This is a test with setAsync.vvThis is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync. This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.vThis is a test with setAsync.This is a test with setAsync.This is a test with setAsync.vvvThis is a test with setAsync.This is a test with setAsync.vvThis is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync. This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.vThis is a test with setAsync.This is a test with setAsync.This is a test with setAsync.vvvThis is a test with setAsync.This is a test with setAsync.vvThis is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync. This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.vThis is a test with setAsync.This is a test with setAsync.This is a test with setAsync.vvvThis is a test with setAsync.This is a test with setAsync.vvThis is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync. This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.vThis is a test with setAsync.This is a test with setAsync.This is a test with setAsync.vvvThis is a test with setAsync.This is a test with setAsync.vvThis is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.  ";
		TextMessage msg = ctxLocal.createTextMessage(text);
		JMSProducer msgProd = ctxLocal.createProducer();
		msgProd.setAsync(tListner);

		for (int i = 0; i < 100; i++) {
			msgProd.send(jmsQueue, msg);
			System.out.println("Message # sent is :" + i);
		}
		System.out.println("Calling close");

		ctxLocal.close();

		System.out.println("close completed");

		boolean flagT = false;

		synchronized (lock) {
			while (count != 100) {
				lock.wait(WAIT_TIME);
			}
		}
		if (count == 100 && excount == 0) {

			flagT = true;
			//Assert.assertTrue(flagT);
			System.out.println("Test case : testClose passed. ");

		} else {
			System.out.println("Test case : testClose failed.");
			//Assert.assertTrue(flagT);
			try {
				throw new Exception("Condition check failed.");
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		clearQueue(jmsQueue);

		System.out.println("EndingTest :testClose");

		tListner.resetCounter();
		resetCounter();
		ctxLocal.close();
	}

	public void testCommit(TestCompletionListener tlistner)
			throws JMSException, InterruptedException {

		tlistner.resetCounter();
		resetCounter();
		System.out.println("Starting Test :testCommit");
		clearQueue(jmsQueue);
		int expectedCount = 100;
		int exceptionCount = 0;
		tlistner.setLock(lock, expectedCount, exceptionCount);
		QueueConnectionFactory qcfLocal = GetJMSResources.getQCF();
		JMSContext jmsContextTransacted = qcfLocal
				.createContext(Session.SESSION_TRANSACTED);
		JMSProducer messageProducerTrans = jmsContextTransacted
				.createProducer();

		messageProducerTrans.setAsync(tlistner);
		for (int i = 0; i < 100; i++) {
			Message message = getMessage(i);
			messageProducerTrans.send(jmsQueue, message);
		}

		System.out.println("Before commit");
		jmsContextTransacted.commit();
		System.out.println("Commit completed");

		JMSConsumer messageConsumerTrans = jmsContextTransacted
				.createConsumer(jmsQueue);
		for (int i = 99; i > 0; i--) {
			System.out.println("Printing Message" + i + ":");

			System.out.println(messageConsumerTrans.receive(WAIT_TIME)
					.toString());
		}

		messageConsumerTrans.close();
		jmsContextTransacted.close();
		boolean flagT = false;

		synchronized (lock) {
			while (count != 100) {
				lock.wait(WAIT_TIME);
			}
		}
		if (count == 100 && excount == 0) {
			flagT = true;

			//Assert.assertTrue(flagT);
			System.out.println("Test case : testCommit passed. ");
		} else {
			System.out.println("Test case : testCommit failed.");
			//Assert.assertTrue(flagT);
			try {
				throw new Exception("Condition check failed.");
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		clearQueue(jmsQueue);

		System.out.println("Ending Test :testCommit");
		tlistner.resetCounter();
		resetCounter();

	}

	public void testRollBack(TestCompletionListener tListener)
			throws JMSException, InterruptedException {
		System.out.println("Starting Test : testRollBack");
		tListener.resetCounter();
		resetCounter();
		int expectedCount = 100;
		int exceptionCount = 0;
		clearQueue(jmsQueue);
		tListener.setLock(lock, expectedCount, exceptionCount);
		QueueConnectionFactory qcfLocal = GetJMSResources.getQCF();
		JMSContext jmsContextTransacted = qcfLocal
				.createContext(Session.SESSION_TRANSACTED);
		Message message = jmsContextTransacted.createTextMessage("Hello World");

		JMSProducer jmsProducerTrans = jmsContextTransacted.createProducer();
		jmsProducerTrans.setAsync(tListener);

		for (int i = 0; i < 100; i++) {
			System.out.println("Sending Message:" + i + ":");
			jmsProducerTrans.send(jmsQueue, message);

		}

		jmsContextTransacted.rollback();

		synchronized (lock) {
			while (count != 100) {
				lock.wait(WAIT_TIME);
			}
		}
		QueueBrowser qb = jmsContextTransacted.createBrowser(jmsQueue);
		Enumeration e1 = qb.getEnumeration();

		int numMsgs = 0;
		// count number of messages
		while (e1.hasMoreElements()) {
			e1.nextElement();
			numMsgs++;
		}

		jmsContextTransacted.close();

		boolean flagT = false;
		if (count == 100 && numMsgs == 0) {
			flagT = true;
			//Assert.assertTrue(flagT);

			System.out.println("Test case : testRollBack passed. ");
		} else {
			System.out.println("Test case : testRollBack failed.");
			//Assert.assertTrue(flagT);
			try {
				throw new Exception("Condition check failed.");
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		clearQueue(jmsQueue);

		System.out.println("Ending Test : testRollBack");

		tListener.resetCounter();
		resetCounter();
	}

	public void testCallingNativeContext(TestCompletionListenerVariation listnrV)
			throws JMSException, InterruptedException {
		System.out.println("Starting Test :testCallingNativeContext");
		int expectedCount = 1;
		int exceptionCount = 0;
		listnrV.setLock(lock, expectedCount, exceptionCount);
		listnrV.resetFlag();
		listnrV.resetCounter();
		resetCounter();
		resetFlag();
		clearQueue(jmsQueue);
		QueueConnectionFactory qcfLocal = GetJMSResources.getQCF();
		// JMSContext jmsContextTransacted =
		// qcfLocal.createContext(Session.SESSION_TRANSACTED);

		Message message = jmsContextTransactedN
				.createTextMessage("Hello World");

		JMSProducer jmsProducerTrans = jmsContextTransactedN.createProducer();
		jmsProducerTrans.setAsync(listnrV);

		jmsProducerTrans.send(jmsQueue, message);

		// Thread.sleep(1000);

		synchronized (lock) {
			while (count != 1) {
				lock.wait(WAIT_TIME);
			}
		}

		boolean flagT = false;
		if (closeTC == true && commitTC == true && rollbackTC == true) {

			flagT = true;
			//Assert.assertTrue(flagT);
			System.out.println("Test case :testCallingNativeContext passed.");
		} else {
			System.out.println("Test case : testCallingNativeContext failed");
			//Assert.assertTrue(flagT);
			try {
				throw new Exception("Condition check failed.");
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		clearQueue(jmsQueue);
		listnrV.resetFlag();
		resetFlag();
		listnrV.resetCounter();
		resetCounter();

		System.out.println("Ending Test :testCallingNativeContext");

	}
	public void testAsyncSendNDE1(TestCompletionListener listener)
			throws JMSException {

		System.out
				.println("*****************Starting Test : testAsyncSendNDE1***********************");
		listener.resetCounter();
		resetCounter();
		
JMSContext jmsContext = cf.createContext();
	BytesMessage bmsg = jmsContext.createBytesMessage();
		JMSProducer jmsProducer = jmsContext.createProducer();
jmsProducer.setAsync(listener);
		boolean flag = false;
		try {
		 	
			jmsProducer.send(null,bmsg );
		} catch (InvalidDestinationRuntimeException ex) {
			ex.printStackTrace();
			flag = true;
		}
		
		

		if (flag == true && count == 0 && excount == 0) {
			
			System.out.println("Test case :testAsyncSendNDE1 passed ");
		}

		else {
			

			System.out.println("Test case : testAsyncSendNDE1 failed");
			try {
				throw new Exception("Condition check failed.");
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		System.out
				.println("*******************Ending Test : testAsyncSendNDE1************************");

		listener.resetCounter();
		resetCounter();
	}

	public void testAsyncSendNDE2(TestCompletionListener listener)
			throws JMSException {

		System.out
				.println("*****************Starting Test : testAsyncSendNDE2***********************");
		listener.resetCounter();
		resetCounter();
		JMSContext jmsContext = cf.createContext();
		TextMessage tmsg = jmsContext.createTextMessage();
			JMSProducer jmsProducer = jmsContext.createProducer();
			jmsProducer.setAsync(listener);

		boolean flag = false;
		try {

			jmsProducer.send(null,tmsg);
		} catch (InvalidDestinationRuntimeException ex) {
			ex.printStackTrace();
			flag = true;
		}

		if (flag == true && count == 0 && excount == 0) {
			
			System.out.println("Test case :testAsyncSendNDE2 passed ");
		}

		else {
			

			System.out.println("Test case : testAsyncSendNDE2 failed");
			try {
				throw new Exception("Condition check failed.");
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		System.out
				.println("*******************Ending Test : testAsyncSendNDE2************************");

		listener.resetCounter();
		resetCounter();
	}
	
	
	public void testAsyncSendNDE3(TestCompletionListener listener)
			throws JMSException {

		System.out
				.println("*****************Starting Test : testAsyncSendNDE3***********************");
		listener.resetCounter();
		resetCounter();
		JMSContext jmsContext = cf.createContext();
		ObjectMessage obmsg = jmsContext.createObjectMessage();
			JMSProducer jmsProducer = jmsContext.createProducer();
		
			jmsProducer.setAsync(listener);
		boolean flag = false;
		try {

			jmsProducer.send(null,obmsg);
		} catch (InvalidDestinationRuntimeException ex) {
			ex.printStackTrace();
			flag = true;
		}

		if (flag == true && count == 0 && excount == 0) {
			
			System.out.println("Test case :testAsyncSendNDE3 passed ");
		}

		else {
			
			System.out.println("Test case : testAsyncSendNDE3 failed");
			try {
				throw new Exception("Condition check failed.");
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		System.out
				.println("*******************Ending Test : testAsyncSendNDE3************************");

		listener.resetCounter();
		resetCounter();
	}
	
	
	
	public void testAsyncSendNDE4(TestCompletionListener listener)
			throws JMSException {
		
		System.out
				.println("*****************Starting Test : testAsyncSendNDE4***********************");
		listener.resetCounter();
		resetCounter();
		JMSContext jmsContext = cf.createContext();
		MapMessage mapmsg = jmsContext.createMapMessage();
			JMSProducer jmsProducer = jmsContext.createProducer();
		
			jmsProducer.setAsync(listener);
		boolean flag = false;
		try {

			jmsProducer.send(null,mapmsg);
		} catch (InvalidDestinationRuntimeException ex) {
			ex.printStackTrace();
			flag = true;
		}

		if (flag == true && count == 0 && excount == 0) {
			
			System.out.println("Test case :testAsyncSendNDE4 passed ");
		}

		else {
			

			System.out.println("Test case : testAsyncSendNDE4 failed");
			try {
				throw new Exception("Condition check failed.");
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		System.out
				.println("*******************Ending Test : testAsyncSendNDE4************************");

		listener.resetCounter();
		resetCounter();
	}
	
	public void testAsyncSendNDE5(TestCompletionListener listener)
			throws JMSException {
		
		System.out
				.println("*****************Starting Test : testAsyncSendNDE5***********************");
		listener.resetCounter();
		resetCounter();
		JMSContext jmsContext = cf.createContext();
		Message msg = jmsContext.createMessage();
			JMSProducer jmsProducer = jmsContext.createProducer();
		
			jmsProducer.setAsync(listener);
		boolean flag = false;
		try {

			jmsProducer.send(null,msg);
		} catch (InvalidDestinationRuntimeException ex) {
			ex.printStackTrace();
			flag = true;
		}

		if (flag == true && count == 0 && excount == 0) {
			
			System.out.println("Test case :testAsyncSendNDE5 passed ");
		}

		else {
			

			System.out.println("Test case : testAsyncSendNDE5 failed");
			try {
				throw new Exception("Condition check failed.");
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		System.out
				.println("*******************Ending Test : testAsyncSendNDE5************************");

		listener.resetCounter();
		resetCounter();
	}
	
	public void testDCF() throws Exception {
		boolean exceptionFlag = false;
		try{
			
		 ConnectionFactory cf = (ConnectionFactory) new InitialContext()
         .lookup("java:comp/DefaultJMSConnectionFactory");
		 
		}catch(NameNotFoundException e){
			e.printStackTrace();
			exceptionFlag = true;
		}
		
		if(!exceptionFlag)
			throw new Exception("testDCF failed to throw NameNotFoundException");
		else
			System.out.println("Test case :testDCF passed ");
	}
	
	public void testDCFVariation() throws Exception {
		boolean exceptionFlag = false;
		try{
			ConnectionFactory cf = (ConnectionFactory) new InitialContext()
	         .lookup("java:comp/env/myCF");
		}catch(NamingException e){
			e.printStackTrace();
			exceptionFlag = true;
		}
		
		if(!exceptionFlag)
			throw new Exception("testDCFVariation failed to throw NameNotFoundException");
		else
			System.out.println("Test case :testDCFVariation passed ");
	}
	
	public void setCounter(int counter) {
		// TODO Auto-generated method stub
		count = counter;

	}

	public void setexCounter(int exCounter) {
		// TODO Auto-generated method stub
		excount = exCounter;
		System.out.println("In setexCounter:");
		System.out.println("exCounter:" + exCounter);
		System.out.println("excount:" + excount);

	}

	public void resetCounter() {
		count = 0;
		excount = 0;
	}

	public void resetFlag() {
		closeTC = false;
		commitTC = false;
		rollbackTC = false;

		closeUR = false;
		commitUR = false;
		rollbackUR = false;

		closeCtx = false;
		commitCtx = false;
		rollbackCtx = false;
		prodCtx = false;

	}

	public void setFlagClose(boolean close) {
		closeTC = close;

	}

	public void setFlagCommit(boolean commit) {
		commitTC = commit;

	}

	public void setFlagRollback(boolean rollback) {
		rollbackTC = rollback;

	}

	public void setOperation(boolean close, boolean commit, boolean rollback) {
		// TODO Auto-generated method stub
		closeUR = close;
		commitUR = commit;
		rollbackUR = rollback;
	}

	public void setOperationTest(boolean close, boolean commit,
			boolean rollback, boolean prod) {
		// TODO Auto-generated method stub
		closeCtx = close;
		commitCtx = commit;
		rollbackCtx = rollback;
		prodCtx = prod;
	}

	public void clearQueue(Queue queue) throws JMSException {

		JMSContext jmsCxt = qcf.createContext();
		qb = jmsCxt.createBrowser(queue);
		JMSConsumer jmsCons = jmsCxt.createConsumer(queue);

		Enumeration e = qb.getEnumeration();

		int count = 0;
		// count number of messages
		while (e.hasMoreElements()) {
			e.nextElement();
			count++;
		}

		for (int i = count; count > 0; count--) {
			jmsCons.receiveNoWait();

		}
	}

}
