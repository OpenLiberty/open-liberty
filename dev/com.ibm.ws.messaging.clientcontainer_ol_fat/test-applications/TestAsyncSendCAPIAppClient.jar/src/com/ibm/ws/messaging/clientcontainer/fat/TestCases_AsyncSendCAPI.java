package com.ibm.ws.messaging.clientcontainer.fat;

import java.util.Enumeration;
import java.util.concurrent.locks.ReentrantLock;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.InvalidDestinationException;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageFormatException;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.NamingException;





public class TestCases_AsyncSendCAPI {

	public MessageProducer messageProducer = null;
	public MessageConsumer messageConsumer = null;
	public Queue jmsQueue = null;
	public Queue exQueue = null;
	public Message message = null;
	public static QueueConnection queueConnection = null;
	public static QueueSession queueSession = null;
	public static Connection connection = null;
	public static Session session = null;
	public static QueueSession queueSessionTest = null;
	public static QueueSession queueSessionTransacted = null;
	public static QueueSession queueSessionTransacted1 = null;
	public static QueueSession queueSessionTransactedN = null;
	public MessageProducer jmsProducerTest = null;
	public MessageProducer messageProducerMO = null;
	public MessageProducer messageProducerMO1 = null;
	public MessageProducer messageProducerMO2 = null;
	public MessageProducer messageProducerMO3 = null;
	public MessageProducer messageProducerMO4 = null;

	public MessageConsumer messageConsumerMO = null;
	public Queue jmsQueueMO = null;
	public Message messageMO = null;

	public String providerEndPoints = "localhost:9007:BootstrapBasicMessaging";
	public String busName = "defaultBus";
	public String DurableSubscriptionHome = "defaultME";

	TestCompletionListenerCAPI tcmpListner = null;
	TestCompletionListenerSessionCAPI tcmpListnerSession = null;
	TestCompletionListener_MessageOrderingCAPI tcmpListenerMO = null;
	TestCompletionListenerVariationCAPI tcmpListVariation = null;
	TestCompletionListenerVariationURCAPI tcmpListVariationUR = null;

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
	public static boolean mprodCtx = false;
	
	private static int WAIT_TIME = 8000;

	public ReentrantLock lock = new ReentrantLock();

	public String initialPrep()  {

		System.out.println("Entered the initialPrep method");
		try {
			GetJMSResourcesCAPI.establishConnection();
		} catch (NamingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JMSException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		queueConnection = GetJMSResourcesCAPI.getQueueConnection();
connection = GetJMSResourcesCAPI.getConnection();
		try {
			queueConnection.start();
			connection.start();

		} catch (JMSException e) {
			// TODO Auto-generated catch block
			// Do you need FFDC here? Remember FFDC instrumentation and
			// @FFDCIgnore
			// http://was.pok.ibm.com/xwiki/bin/view/Liberty/LoggingFFDC
			e.printStackTrace();
		}

		jmsQueue = GetJMSResourcesCAPI.getJMSQueue();
		jmsQueueMO = GetJMSResourcesCAPI.getJMSQueueMO();
		exQueue = GetJMSResourcesCAPI.getexQueue();
		return ("initial Prep done");

	}

	public TestCompletionListenerCAPI testSetUp() {

		tcmpListner = new TestCompletionListenerCAPI();
		return tcmpListner;
	}

	public TestCompletionListener_MessageOrderingCAPI testSetUpOrder() {

		tcmpListenerMO = new TestCompletionListener_MessageOrderingCAPI();
		return tcmpListenerMO;
	}

	public TestCompletionListenerSessionCAPI testSetUpSession() {

		try {
			queueSessionTest = queueConnection.createQueueSession(true,
					Session.AUTO_ACKNOWLEDGE);
			jmsProducerTest = queueSessionTest.createProducer(jmsQueue);
		} catch (JMSException e) {
			// TODO Auto-generated catch block
			// Do you need FFDC here? Remember FFDC instrumentation and
			// @FFDCIgnore
			// http://was.pok.ibm.com/xwiki/bin/view/Liberty/LoggingFFDC
			e.printStackTrace();
		}

		tcmpListnerSession = new TestCompletionListenerSessionCAPI(
				queueSessionTest,jmsProducerTest);
		return tcmpListnerSession;
	}

	public TestCompletionListenerVariationCAPI testSetUpVariation() {

		try {
			queueSessionTransactedN = queueConnection.createQueueSession(true,
					Session.AUTO_ACKNOWLEDGE);
			System.out.println("QueueSessionTransactedN:"
					+ queueSessionTransactedN);

			tcmpListVariation = new TestCompletionListenerVariationCAPI(
					queueSessionTransactedN);

		} catch (JMSException e) {
			// TODO Auto-generated catch block
			// Do you need FFDC here? Remember FFDC instrumentation and
			// @FFDCIgnore
			// http://was.pok.ibm.com/xwiki/bin/view/Liberty/LoggingFFDC
			e.printStackTrace();
		}

		return tcmpListVariation;
	}

	public TestCompletionListenerVariationURCAPI testSetUpVariationUR() {

		try {
			queueSessionTransacted = queueConnection.createQueueSession(true,
					Session.AUTO_ACKNOWLEDGE);
			queueSessionTransacted1 = queueConnection.createQueueSession(true,
					Session.SESSION_TRANSACTED);
		} catch (JMSException e) {
			// TODO Auto-generated catch block
			// Do you need FFDC here? Remember FFDC instrumentation and
			// @FFDCIgnore
			// http://was.pok.ibm.com/xwiki/bin/view/Liberty/LoggingFFDC
			e.printStackTrace();
		}

		tcmpListVariationUR = new TestCompletionListenerVariationURCAPI(
				queueSessionTransacted1, queueSessionTransacted);

		return tcmpListVariationUR;
	}

	public Message getMessage(int order) {

		messageMO = GetJMSResourcesCAPI.getJMSMessage(order);
		return messageMO;

	}

	// send(Message message, CompletionListener completionListener)

	public void testAsyncSendCAPI(TestCompletionListenerCAPI tcmpListener)
			throws JMSException {

		System.out
				.println("*****************Starting Test : testSetAsyncSend***********************");

		clearQueue(jmsQueue);
		tcmpListner.resetCounter();
		resetCounter();
		int expectedCount = 1;
		int exceptionCount = 0;
		tcmpListener.setLock(lock, expectedCount, exceptionCount);

		QueueSession queueSession = queueConnection.createQueueSession(false,
				javax.jms.Session.AUTO_ACKNOWLEDGE);
		try {
			TextMessage tmsg = queueSession
					.createTextMessage("This is a test with setAsyncSend");
			tmsg.setText("Hello Everyone");

			MessageProducer messageProducer = queueSession
					.createProducer(jmsQueue);
			
			MessageConsumer messageConsumer = queueSession.createConsumer(jmsQueue);

			messageProducer.send(tmsg, tcmpListner);

			// Thread.sleep(100);

			synchronized (lock) {
				while (count != 1) {
					lock.wait(WAIT_TIME);
				}
			}
			TextMessage msgRcvd = (TextMessage) messageConsumer
					.receive(WAIT_TIME);
			System.out
					.println("Message Text Received is :" + msgRcvd.getText());

			boolean flag = false;

			if (msgRcvd.getText().equals("Hello Everyone") && count == 1
					&& excount == 0) {
				flag = true;
				////Assert.assertTrue(flag);
				System.out.println("Test Case: testAsyncSendCAPI Passed");

			}

			else {
				//Assert.assertTrue(flag);
				System.out.println("Test Case: testAsyncSend Failed");
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

		clearQueue(jmsQueue);
		tcmpListner.resetCounter();
		resetCounter();
		System.out
				.println("*******************Ending Test : testSetAsyncSend************************");

	}

	// Case where the acknowledgement is not received, the JMS provider would
	// notify the application by invoking the CompletionListener�s onException
	// method.

	public void testExceptionMessageThreshhold1(
			TestCompletionListenerCAPI tcmpListner, int numMsgs)
			throws JMSException {
		System.out
				.println("**********************Starting Test : testExceptionMessageThreshhold1**********************");

		clearQueue(exQueue);
		tcmpListner.resetCounter();
		resetCounter();
		int expectedCount = 1;
		int exceptionCount = 0;
		tcmpListner.setLock(lock, expectedCount, exceptionCount);

		QueueSession queueSession = queueConnection.createQueueSession(false,
				javax.jms.Session.AUTO_ACKNOWLEDGE);
		int i = 0;

		try {
			MessageProducer messageProducerMO = queueSession
					.createProducer(exQueue);
			for (i = 0; i <= numMsgs; i++) {
				Message msg = getMessage(i);
				messageProducerMO.send(msg, DeliveryMode.PERSISTENT, 0, 10000,
						tcmpListner);
				// Thread.sleep(100);
			}

			synchronized (lock) {
				while (count != 5 && excount != 1) {
					lock.wait(WAIT_TIME);
				}
			}
			boolean flag = false;
			if (count == 5 && excount == 1) {
				flag = true;
				//Assert.assertTrue(flag);

				System.out
						.println("Test case : testExceptionMessageThreshhold1 passed. ");
			} else {

				//Assert.assertTrue(flag);
				System.out
						.println("Test case : testExceptionMessageThreshhold1 failed. ");
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
		tcmpListner.resetCounter();
		resetCounter();

		System.out
				.println("**********************Ending Test : testExceptionMessageThreshhold1**********************");

	}

	public void testAsyncSendException2(TestCompletionListenerCAPI lsnr)
			throws JMSException {

		System.out
				.println("*****************Starting Test : testAsyncSendException2***********************");

		boolean flag = false;
		boolean flagT = false;

		QueueSession queueSession = queueConnection.createQueueSession(false,
				javax.jms.Session.AUTO_ACKNOWLEDGE);

		try {

			MessageProducer messageProducer = queueSession.createProducer(null);

			messageProducer.send(jmsQueue, null, tcmpListner);

		} catch (MessageFormatException ex) {
			ex.printStackTrace();
			flag = true;
		}

		if (flag == true && excount == 0 && count == 0) {
			flagT = true;
			//Assert.assertTrue(flagT);
			System.out.println("Test Case : testAsyncSendException2 passed");

		} else {

			//Assert.assertTrue(flagT);

			System.out.println("Test Case : testAsyncSendException2 failed");
			try {
				throw new Exception("Condition check failed.");
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		resetCounter();
		System.out
				.println("*******************Ending Test : testAsyncSendException2************************");

	}

	public void testMessageOrderingSingleJMSProducer3(
			TestCompletionListener_MessageOrderingCAPI lsnrMO, int numMsgs)
			throws JMSException, InterruptedException {
		lsnrMO.resetCounter();
		resetCounter();

		clearQueue(jmsQueueMO);
		lsnrMO.setMessgCount(numMsgs);
		System.out
				.println("**********************Starting Test : testMessageOrderingSingleJMSProducer3**********************");

		int expectedCount = 5;
		int exceptionCount = 0;
		lsnrMO.setLock(lock, expectedCount, exceptionCount);
		int i = 0;
		int redFlag = 0;
		int msgOrder[] = new int[numMsgs];

		boolean flag = false;

		QueueSession queueSession = queueConnection.createQueueSession(false,
				javax.jms.Session.AUTO_ACKNOWLEDGE);

		MessageConsumer messageConsumerMO = queueSession
				.createConsumer(jmsQueueMO);

		MessageProducer messageProducerMO = queueSession.createProducer(null);

		try {
			for (i = 0; i < numMsgs; i++) {
				Message msg = getMessage(i);
				messageProducerMO.send(jmsQueueMO, msg,
						DeliveryMode.PERSISTENT, 0, 10000, lsnrMO);
				// Thread.sleep(100);

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

		synchronized (lock) {
			while (count != 5) {
				lock.wait(WAIT_TIME);
			}
		}

		int total_order = lsnrMO.getMessgOrderCount();
		System.out.println("TotalMessageOrder:" + total_order);

		if (redFlag == 0 && count == 5 && excount == 0
				&& total_order == numMsgs) {
			flag = true;
			//Assert.assertTrue(flag);
			System.out
					.println("Test case :testMessageOrderingSingleJMSProducer3 passed");
		} else {

			//Assert.assertTrue(flag);

			System.out
					.println("Test case : testMessageOrderingSingleJMSProducer3 failed");
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
		System.out
				.println("**********************Ending Test : testMessageOrderingSingleJMSProducer3**********************");

	}

	public void testMessageOrderingMultipleJMSProducers(
			TestCompletionListener_MessageOrderingCAPI lsnrMO)
			throws JMSException, InterruptedException {
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

		QueueSession queueSession = queueConnection.createQueueSession(false,
				javax.jms.Session.AUTO_ACKNOWLEDGE);

		MessageProducer messageProducerMO = queueSession
				.createProducer(jmsQueueMO);

		MessageProducer messageProducerMO1 = queueSession
				.createProducer(jmsQueueMO);

		MessageProducer messageProducerMO2 = queueSession
				.createProducer(jmsQueueMO);

		MessageProducer messageProducerMO3 = queueSession
				.createProducer(jmsQueueMO);

		MessageProducer messageProducerMO4 = queueSession
				.createProducer(jmsQueueMO);

		MessageConsumer messageConsumerMO = queueSession
				.createConsumer(jmsQueueMO);

		Message msg0 = getMessage(0);
		messageProducerMO.send(msg0, lsnrMO);
		// Thread.sleep(1000);
		msgRcvd = messageConsumerMO.receive(WAIT_TIME).getIntProperty(
				"Message_Order");
		msgOrder[0] = msgRcvd;
		System.out.println(msgOrder[0]);

		Message msg1 = getMessage(1);
		messageProducerMO1.send(msg1, lsnrMO);
		// Thread.sleep(1000);
		msgRcvd = messageConsumerMO.receive(WAIT_TIME).getIntProperty(
				"Message_Order");
		msgOrder[1] = msgRcvd;
		System.out.println(msgOrder[1]);

		Message msg2 = getMessage(2);
		messageProducerMO2.send(msg2, lsnrMO);
		// Thread.sleep(1000);
		msgRcvd = messageConsumerMO.receive(WAIT_TIME).getIntProperty(
				"Message_Order");
		msgOrder[2] = msgRcvd;
		System.out.println(msgOrder[2]);

		Message msg3 = getMessage(3);
		messageProducerMO3.send(msg3, lsnrMO);
		// Thread.sleep(100);
		msgRcvd = messageConsumerMO.receive(WAIT_TIME).getIntProperty(
				"Message_Order");
		msgOrder[3] = msgRcvd;
		System.out.println(msgOrder[4]);

		Message msg4 = getMessage(4);
		messageProducerMO4.send(msg4, lsnrMO);
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
					.println("Test case :testMessageOrderingMultipleJMSProducers passed");

		} else

		{

			//Assert.assertTrue(flag);
			System.out
					.println("Test case : testMessageOrderingMultipleJMSProducers failed");
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
		System.out
				.println("**********************Ending Test : testMessageOrderingMultipleJMSProducers**********************");

	}

	public void testMessageOrderingMultipleSession1(
			TestCompletionListener_MessageOrderingCAPI lsnrMO)
			throws JMSException, InterruptedException {

		System.out
				.println("**********************Starting Test : testMessageOrderingMultipleSession1**********************");
		clearQueue(jmsQueueMO);
		lsnrMO.resetCounter();
		resetCounter();
		int expectedCount = 1;
		int exceptionCount = 0;
		lsnrMO.setLock(lock, expectedCount, exceptionCount);

		lsnrMO.setMessgCount(5);
		int i = 0;
		int redFlag = 0;
		int msgOrder[] = new int[5];
		int msgRcvd = 0;

		QueueSession qSess1 = queueConnection.createQueueSession(false,
				javax.jms.Session.AUTO_ACKNOWLEDGE);
		MessageConsumer messageConsumerMO = qSess1.createConsumer(jmsQueueMO);
		MessageProducer msgProd1 = qSess1.createProducer(jmsQueueMO);

		QueueSession qSess2 = queueConnection.createQueueSession(false,
				javax.jms.Session.AUTO_ACKNOWLEDGE);
		MessageProducer msgProd2 = qSess2.createProducer(jmsQueueMO);

		QueueSession qSess3 = queueConnection.createQueueSession(false,
				javax.jms.Session.AUTO_ACKNOWLEDGE);
		MessageProducer msgProd3 = qSess3.createProducer(jmsQueueMO);
		MessageProducer msgProd4 = qSess3.createProducer(jmsQueueMO);
		MessageProducer msgProd5 = qSess3.createProducer(jmsQueueMO);

		Message msg0 = getMessage(0);
		msgProd1.send(msg0, DeliveryMode.PERSISTENT, 0, 10000, lsnrMO);

		// Thread.sleep(100);
		msgRcvd = messageConsumerMO.receive(WAIT_TIME).getIntProperty(
				"Message_Order");
		msgOrder[0] = msgRcvd;
		System.out.println(msgOrder[0]);

		Message msg1 = getMessage(1);
		msgProd2.send(msg1, DeliveryMode.PERSISTENT, 0, 10000, lsnrMO);

		// Thread.sleep(100);
		msgRcvd = messageConsumerMO.receive(WAIT_TIME).getIntProperty(
				"Message_Order");
		msgOrder[1] = msgRcvd;
		System.out.println(msgOrder[1]);

		Message msg2 = getMessage(2);
		msgProd3.send(msg2, DeliveryMode.PERSISTENT, 0, 10000, lsnrMO);

		// Thread.sleep(100);
		msgRcvd = messageConsumerMO.receive(WAIT_TIME).getIntProperty(
				"Message_Order");
		msgOrder[2] = msgRcvd;
		System.out.println(msgOrder[2]);

		Message msg3 = getMessage(3);
		msgProd4.send(msg3, DeliveryMode.PERSISTENT, 0, 10000, lsnrMO);

		// Thread.sleep(100);
		msgRcvd = messageConsumerMO.receive(WAIT_TIME).getIntProperty(
				"Message_Order");
		msgOrder[3] = msgRcvd;
		System.out.println(msgOrder[3]);

		Message msg4 = getMessage(4);
		msgProd5.send(msg4, DeliveryMode.PERSISTENT, 0, 10000, lsnrMO);
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
					.println("Test case :testMessageOrderingMultipleSession1 passed");

		} else {
			//Assert.assertTrue(flag);

			System.out
					.println("Test case : testMessageOrderingMultipleSession1 failed");
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
		System.out
				.println("**********************Ending Test : testMessageOrderingMultipleSession1**********************");

	}

	public void testCloseSession2(TestCompletionListenerCAPI tcmpListner)
			throws JMSException, InterruptedException {

		clearQueue(jmsQueue);
		tcmpListner.resetCounter();
		resetCounter();
		int expectedCount = 100;
		int exceptionCount = 0;
		tcmpListner.setLock(lock, expectedCount, exceptionCount);

		System.out.println("Starting Test :testCloseSession2");
		QueueConnectionFactory qcfLocal = GetJMSResourcesCAPI.getQCF();
		QueueConnection qconLocal = qcfLocal.createQueueConnection();
		QueueSession queueSessionLocal = qconLocal.createQueueSession(false,
				Session.AUTO_ACKNOWLEDGE);
		String text = "This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.vThis is a test with setAsync.This is a test with setAsync.This is a test with setAsync.vvvThis is a test with setAsync.This is a test with setAsync.vvThis is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.vThis is a test with setAsync.This is a test with setAsync.This is a test with setAsync.vvvThis is a test with setAsync.This is a test with setAsync.vvThis is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync. This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.vThis is a test with setAsync.This is a test with setAsync.This is a test with setAsync.vvvThis is a test with setAsync.This is a test with setAsync.vvThis is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync. This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.vThis is a test with setAsync.This is a test with setAsync.This is a test with setAsync.vvvThis is a test with setAsync.This is a test with setAsync.vvThis is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync. This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.vThis is a test with setAsync.This is a test with setAsync.This is a test with setAsync.vvvThis is a test with setAsync.This is a test with setAsync.vvThis is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync. This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.vThis is a test with setAsync.This is a test with setAsync.This is a test with setAsync.vvvThis is a test with setAsync.This is a test with setAsync.vvThis is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync. This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.vThis is a test with setAsync.This is a test with setAsync.This is a test with setAsync.vvvThis is a test with setAsync.This is a test with setAsync.vvThis is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync. This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.vThis is a test with setAsync.This is a test with setAsync.This is a test with setAsync.vvvThis is a test with setAsync.This is a test with setAsync.vvThis is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync. This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.vThis is a test with setAsync.This is a test with setAsync.This is a test with setAsync.vvvThis is a test with setAsync.This is a test with setAsync.vvThis is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync. This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.vThis is a test with setAsync.This is a test with setAsync.This is a test with setAsync.vvvThis is a test with setAsync.This is a test with setAsync.vvThis is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync. This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.vThis is a test with setAsync.This is a test with setAsync.This is a test with setAsync.vvvThis is a test with setAsync.This is a test with setAsync.vvThis is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync. This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.vThis is a test with setAsync.This is a test with setAsync.This is a test with setAsync.vvvThis is a test with setAsync.This is a test with setAsync.vvThis is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync. This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.vThis is a test with setAsync.This is a test with setAsync.This is a test with setAsync.vvvThis is a test with setAsync.This is a test with setAsync.vvThis is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync. This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.vThis is a test with setAsync.This is a test with setAsync.This is a test with setAsync.vvvThis is a test with setAsync.This is a test with setAsync.vvThis is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync. This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.vThis is a test with setAsync.This is a test with setAsync.This is a test with setAsync.vvvThis is a test with setAsync.This is a test with setAsync.vvThis is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync. This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.vThis is a test with setAsync.This is a test with setAsync.This is a test with setAsync.vvvThis is a test with setAsync.This is a test with setAsync.vvThis is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.  ";
		TextMessage msg = queueSessionLocal.createTextMessage(text);
		MessageProducer msgProd = queueSessionLocal.createProducer(null);

		for (int i = 0; i < 100; i++) {
			msgProd.send(jmsQueue, msg, tcmpListner);
			System.out.println("Message # sent is :" + i);
		}
		System.out.println("Calling close");
		queueSessionLocal.close();

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
			System.out.println("Test case : testCloseSession2 passed. ");

		} else {
			System.out.println("Test case : testCloseSession2 failed.");
			try {
				throw new Exception("Condition check failed.");
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			//Assert.assertTrue(flagT);
		}
		clearQueue(jmsQueue);

		System.out.println("EndingTest :testCloseSession2");

		tcmpListner.resetCounter();
		resetCounter();

	}

	public void testCloseConnection3(TestCompletionListenerCAPI tcmpListner)
			throws JMSException, InterruptedException {

		clearQueue(jmsQueue);
		tcmpListner.resetCounter();
		resetCounter();
		int expectedCount = 100;
		int exceptionCount = 0;
		tcmpListner.setLock(lock, expectedCount, exceptionCount);
		System.out.println("Starting Test :testCloseConnection3");
		QueueConnectionFactory qcfLocal = GetJMSResourcesCAPI.getQCF();
		QueueConnection qconLocal = qcfLocal.createQueueConnection();
		QueueSession queueSessionLocal = qconLocal.createQueueSession(false,
				Session.AUTO_ACKNOWLEDGE);
		String text = "This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.vThis is a test with setAsync.This is a test with setAsync.This is a test with setAsync.vvvThis is a test with setAsync.This is a test with setAsync.vvThis is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.vThis is a test with setAsync.This is a test with setAsync.This is a test with setAsync.vvvThis is a test with setAsync.This is a test with setAsync.vvThis is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync. This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.vThis is a test with setAsync.This is a test with setAsync.This is a test with setAsync.vvvThis is a test with setAsync.This is a test with setAsync.vvThis is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync. This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.vThis is a test with setAsync.This is a test with setAsync.This is a test with setAsync.vvvThis is a test with setAsync.This is a test with setAsync.vvThis is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync. This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.vThis is a test with setAsync.This is a test with setAsync.This is a test with setAsync.vvvThis is a test with setAsync.This is a test with setAsync.vvThis is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync. This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.vThis is a test with setAsync.This is a test with setAsync.This is a test with setAsync.vvvThis is a test with setAsync.This is a test with setAsync.vvThis is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync. This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.vThis is a test with setAsync.This is a test with setAsync.This is a test with setAsync.vvvThis is a test with setAsync.This is a test with setAsync.vvThis is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync. This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.vThis is a test with setAsync.This is a test with setAsync.This is a test with setAsync.vvvThis is a test with setAsync.This is a test with setAsync.vvThis is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync. This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.vThis is a test with setAsync.This is a test with setAsync.This is a test with setAsync.vvvThis is a test with setAsync.This is a test with setAsync.vvThis is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync. This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.vThis is a test with setAsync.This is a test with setAsync.This is a test with setAsync.vvvThis is a test with setAsync.This is a test with setAsync.vvThis is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync. This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.vThis is a test with setAsync.This is a test with setAsync.This is a test with setAsync.vvvThis is a test with setAsync.This is a test with setAsync.vvThis is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync. This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.vThis is a test with setAsync.This is a test with setAsync.This is a test with setAsync.vvvThis is a test with setAsync.This is a test with setAsync.vvThis is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync. This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.vThis is a test with setAsync.This is a test with setAsync.This is a test with setAsync.vvvThis is a test with setAsync.This is a test with setAsync.vvThis is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync. This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.vThis is a test with setAsync.This is a test with setAsync.This is a test with setAsync.vvvThis is a test with setAsync.This is a test with setAsync.vvThis is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync. This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.vThis is a test with setAsync.This is a test with setAsync.This is a test with setAsync.vvvThis is a test with setAsync.This is a test with setAsync.vvThis is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync. This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.vThis is a test with setAsync.This is a test with setAsync.This is a test with setAsync.vvvThis is a test with setAsync.This is a test with setAsync.vvThis is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.This is a test with setAsync.  ";
		TextMessage msg = queueSessionLocal.createTextMessage(text);
		MessageProducer msgProd = queueSessionLocal.createProducer(null);

		for (int i = 0; i < 100; i++) {
			msgProd.send(jmsQueue, msg, DeliveryMode.PERSISTENT, 0, 10000,
					tcmpListner);
			System.out.println("Message # sent is :" + i);
		}

		System.out.println("Calling close");
		qconLocal.close();

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
			System.out.println("Test case : testCloseConnection3 passed. ");

		} else {
			System.out.println("Test case : testCloseConnection3 failed.");
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

		System.out.println("EndingTest :testCloseConnection3");

	}

	public void testCallingNativeSession(
			TestCompletionListenerVariationCAPI listnrV)
			throws InterruptedException, JMSException {

		clearQueue(jmsQueue);
		int expectedCount = 1;
		int exceptionCount = 0;

		listnrV.resetFlag();
		listnrV.resetCounter();
		resetCounter();
		resetFlag();

		listnrV.setLock(lock, expectedCount, exceptionCount);
		System.out.println(queueSessionTransactedN);

		System.out.println("Starting Test :testCallingNativeSession");
		Message message;

		message = queueSessionTransactedN.createTextMessage("Hello World");

		MessageProducer jmsProducerTrans = queueSessionTransactedN
				.createProducer(jmsQueue);

		System.out.println(jmsProducerTrans);

		jmsProducerTrans.send(message, listnrV);

		synchronized (lock) {
			while (count != 1) {
				lock.wait(WAIT_TIME);
			}
		}

		boolean flagT = false;
		if (closeTC == true && commitTC == true && rollbackTC == true) {

			flagT = true;
			//Assert.assertTrue(flagT);
			System.out.println("Test case :testCallingNativeSession passed.");
		} else {
			System.out.println("Test case : testCallingNativeSession failed");
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

		System.out.println("Ending Test :testCallingNativeSession");

	}

	public void testAsyncSendIDE2(TestCompletionListenerCAPI listener)
			throws JMSException, InterruptedException {

		System.out
				.println("*****************Starting Test : testAsyncSendIDE2***********************");

		listener.resetCounter();
		resetCounter();

		QueueSession queueSession = queueConnection.createQueueSession(false,
				javax.jms.Session.AUTO_ACKNOWLEDGE);
		;
		TextMessage tmsg = queueSession
				.createTextMessage("This is a test with setAsyncSend");
		// Queue queueNE = queueSession.createQueue("QUEUE4");

		MessageProducer msgProd = queueSession.createProducer(null);
		boolean flag = false;

		try {
			msgProd.send(null, tmsg, listener);
		} catch (InvalidDestinationException ex) {
			ex.printStackTrace();
			flag = true;
		}

		System.out.println(flag + ":" + count + ":" + excount);

		boolean flagT = false;
		if (flag == true && count == 0 && excount == 0) {
			flagT = true;
			System.out.println("Test case :testAsyncSendIDE2 passed.");
			//Assert.assertTrue(flagT);
		} else {
			//Assert.assertTrue(flagT);
			System.out.println("Test case : testAsyncSendIDE2 failed");
			try {
				throw new Exception("Condition check failed.");
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		listener.resetCounter();
		resetCounter();
		System.out
				.println("*******************Ending Test : testAsyncSendIDE2************************");

	}

	public void testAsyncSendCLN() throws JMSException {

		System.out
				.println("*****************Starting Test : testAsyncSendCLN***********************");
		QueueSession queueSession = queueConnection.createQueueSession(false,
				javax.jms.Session.AUTO_ACKNOWLEDGE);
		TextMessage tmsg = queueSession
				.createTextMessage("This is a test with setAsyncSend");
		MessageProducer messageProducer = queueSession.createProducer(jmsQueue);
		boolean flag = false;
		try {
			messageProducer.send(tmsg, null);
		} catch (IllegalArgumentException ex) {
			ex.printStackTrace();
			flag = true;
		}

		if (flag == true) {
			//Assert.assertTrue(flag);
			System.out.println("Test case : testAsyncSendCLN passed");
		} else {

			//Assert.assertTrue(flag);
			System.out.println("Test case : testAsyncSendCLN failed");
			try {
				throw new Exception("Condition check failed.");
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		System.out
				.println("*******************Ending Test : testAsyncSendCLN************************");
	}

	public void testAsyncSendNDE1(TestCompletionListenerCAPI listener)
			throws JMSException {

		System.out
				.println("*****************Starting Test : testAsyncSendNDE1***********************");
		listener.resetCounter();
		resetCounter();
		QueueSession queueSession = queueConnection.createQueueSession(false,
				javax.jms.Session.AUTO_ACKNOWLEDGE);

		TextMessage tmsg = queueSession
				.createTextMessage("This is a test with setAsyncSend");
		MessageProducer messageProducer = queueSession.createProducer(null);

		boolean flag = false;
		try {

			messageProducer.send(tmsg, DeliveryMode.PERSISTENT, 0, 10000,
					tcmpListner);
		} catch (UnsupportedOperationException ex) {
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

	public void testAsyncSendNDE2(TestCompletionListenerCAPI listener)
			throws JMSException {

		System.out
				.println("*****************Starting Test : testAsyncSendNDE2***********************");
		listener.resetCounter();
		resetCounter();
	
		Session session = connection.createSession(false,
				javax.jms.Session.AUTO_ACKNOWLEDGE);
		TextMessage tmsg = session
				.createTextMessage("This is a test with setAsyncSend");
		MessageProducer messageProducer = session.createProducer(null);

		boolean flag = false;
		try {

			messageProducer.send(tmsg,tcmpListner);
		} catch (UnsupportedOperationException ex) {
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
	
	
	public void testAsyncSendNDE3(TestCompletionListenerCAPI listener)
			throws JMSException {

		System.out
				.println("*****************Starting Test : testAsyncSendNDE3***********************");
		listener.resetCounter();
		resetCounter();
		
		
		Session session = connection.createSession(false,
				javax.jms.Session.AUTO_ACKNOWLEDGE);

		TextMessage tmsg = session
				.createTextMessage("This is a test with setAsyncSend");
		MessageProducer messageProducer = session.createProducer(null);

		boolean flag = false;
		try {

			messageProducer.send(null,tmsg,tcmpListner);
		} catch (InvalidDestinationException ex) {
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
	
	public void testAsyncSendNDE4(TestCompletionListenerCAPI listener)
			throws JMSException {

		System.out
				.println("*****************Starting Test : testAsyncSendNDE4***********************");
		listener.resetCounter();
		resetCounter();
		
		
		Session session = connection.createSession(false,
				javax.jms.Session.AUTO_ACKNOWLEDGE);

		TextMessage tmsg = session
				.createTextMessage("This is a test with setAsyncSend");
		MessageProducer messageProducer = session.createProducer(null);

		boolean flag = false;
		try {

			messageProducer.send(null,tmsg, DeliveryMode.PERSISTENT, 0, 10000,
					tcmpListner);
		} catch (InvalidDestinationException ex) {
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
				.println("*******************Ending Test : testAsyncSendNDE3************************");

		listener.resetCounter();
		resetCounter();
	}
	
	public void testCallingNativeSession_unrelatedSession3(
			TestCompletionListenerVariationURCAPI listnrV) throws JMSException,
			InterruptedException {
		System.out
				.println("Starting Test : testCallingNativeSession_unrelatedSession1");

		clearQueue(jmsQueue);

		listnrV.resetCounter();
		resetFlag();
		resetCounter();

		int expectedCount = 1;
		int exceptionCount = 0;
		listnrV.setLock(lock, expectedCount, exceptionCount);

		Message message = queueSessionTransacted
				.createTextMessage("Hello World");

		MessageProducer jmsProducerTrans = queueSessionTransacted
				.createProducer(null);

		jmsProducerTrans.send(jmsQueue, message, DeliveryMode.PERSISTENT, 0,
				10000, listnrV);
		synchronized (lock) {
			while (count != 1) {
				lock.wait(WAIT_TIME);
			}
		}
		queueSessionTransacted.commit();

		QueueBrowser qb = queueSessionTransacted.createBrowser(jmsQueue);
		Enumeration e1 = qb.getEnumeration();

		int numMsgs = 0;
		// count number of messages
		while (e1.hasMoreElements()) {
			e1.nextElement();
			numMsgs++;
		}

		System.out.println(count);
		System.out.println(excount);
		System.out.println(numMsgs);
		System.out.println(closeUR);
		System.out.println(commitUR);
		System.out.println(rollbackUR);
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

		System.out
				.println("Starting Test : testCallingNativeSession_unrelatedSession3");
		clearQueue(jmsQueue);

		listnrV.resetCounter();
		resetFlag();
		resetCounter();

	}

	// For a given JMSContext, callbacks (both onCompletion and onException)
	// will be performed in the same order as the corresponding calls to the
	// asynchronous send method.

	public void testOnCompletionOnException2(TestCompletionListenerCAPI listner)
			throws JMSException, InterruptedException {

		System.out
				.println("**********************Starting Test : testOnCompletionOnException2**********************");

		clearQueue(jmsQueue);
		listner.resetCounter();
		resetCounter();
		int expectedCount = 5;
		int exceptionCount = 1;
		listner.setLock(lock, expectedCount, exceptionCount);

		QueueSession queueSession = queueConnection.createQueueSession(false,
				javax.jms.Session.AUTO_ACKNOWLEDGE);

		// Queue queue =
		// GetJMSResources.getQueueSession().createQueue("QUEUE4");
		MessageProducer msgProducer = queueSession.createProducer(null);
		MessageConsumer msgConsumer = queueSession.createConsumer(exQueue);

		for (int i = 0; i < 5; i++) {
			TextMessage tmessg = queueSession.createTextMessage("Hello");
			tmessg.setText("Hello");
			msgProducer.send(exQueue, tmessg, listner);

		}

		synchronized (lock) {
			while (count != 5) {
				lock.wait(WAIT_TIME);
			}
		}

		boolean flag = false;
		if (count == 5 && excount == 0)
			flag = true;

		TextMessage tmessg = queueSession.createTextMessage("Hello");
		msgProducer.send(exQueue, tmessg, listner);

		synchronized (lock) {
			while (excount != 1) {
				lock.wait(WAIT_TIME);
			}
		}

		int num = 0;
		System.out
				.println("Now the message has been sent. We will compare the message received");

		for (int i = 5; i > 0; i--) {
			TextMessage msgRcvd = (TextMessage) msgConsumer.receiveNoWait();

			if (msgRcvd.getText().equals("Hello"))
				num++;
		}

		boolean flagT = true;
		if (flag == true && excount == 1 && num == 5) {
			flagT = true;
			//Assert.assertTrue(flagT);

			System.out
					.println("Test case : testOnCompletionOnException2 passed ");
		} else {
			//Assert.assertTrue(flagT);
			System.out
					.println("test case : testOnCompletionOnException2 failed");
			try {
				throw new Exception("Condition check failed.");
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		clearQueue(jmsQueue);

		System.out
				.println("******************Ending Test : testOnCompletionOnException2***************************");

		listner.resetCounter();
		resetCounter();

	}

	public void testSessioninCallBackMethodOC1(
			TestCompletionListenerSessionCAPI tcmpListner) throws JMSException,
			InterruptedException {

		System.out
				.println("**********************Starting Test : testSessioninCallBackMethodOC1**********************");
		clearQueue(jmsQueue);

		tcmpListner.reset();
		resetCounter();
		resetFlag();
		int expectedCount = 1;
		int exceptionCount = 0;
		tcmpListner.setLock(lock, expectedCount, exceptionCount);

		//MessageProducer jmsProducer = queueSessionTest.createProducer(jmsQueue);
		MessageConsumer jmsConsumer = queueSessionTest.createConsumer(jmsQueue);

		TextMessage tmessg = queueSessionTest
				.createTextMessage("This is a test with setAsync");

		jmsProducerTest
				.send(tmessg, DeliveryMode.PERSISTENT, 0, 10000, tcmpListner);
		synchronized (lock) {
			while (count != 1) {
				lock.wait(WAIT_TIME);
			}
		}

		queueSessionTest.commit();

		System.out
				.println("Now the message has been sent. We will compare the message received");

		String msgRcvd = jmsConsumer.receive(WAIT_TIME).toString();

		System.out.println("Message Received is :" + msgRcvd);

		System.out.println(count);
		System.out.println(closeCtx);
		System.out.println(commitCtx);
		System.out.println(rollbackCtx);
		System.out.println(prodCtx);
		System.out.println(mprodCtx);

		boolean flag = false;

		if (count == 1 && closeCtx == true && commitCtx == true
				&& rollbackCtx == true && prodCtx == true && mprodCtx == true) {
			flag = true;
			//Assert.assertTrue(flag);
			System.out
					.println("Test Case: testSessioninCallBackMethodOC1 Passed");
		} else {
			//Assert.assertTrue(flag);
			System.out
					.println("Test Case: testSessioninCallBackMethodOC1 failed");
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
				.println("******************Ending Test : testSessioninCallBackMethodOC1***************************");

	}
	public void testSessioninCallBackMethodOE(
			TestCompletionListenerSessionCAPI tcmpListner) throws JMSException,
			InterruptedException {

		System.out
				.println("**********************Starting Test : testSessioninCallBackMethodOE**********************");
		clearQueue(exQueue);
		tcmpListner.reset();
		resetCounter();
		resetFlag();
		int expectedCount = 5;
		int exceptionCount = 1;
		tcmpListner.setLock(lock, expectedCount, exceptionCount);
		MessageProducer jmsProducer = queueSessionTest.createProducer(exQueue);

		for (int i = 0; i < 6; i++) {
			TextMessage tmessg = queueSessionTest
					.createTextMessage("This is a test with setAsync");
			jmsProducer.send(tmessg, tcmpListner);

		}

		synchronized (lock) {
			while (count != 5 && excount != 1) {
				lock.wait(WAIT_TIME);
			}
		}

		boolean flag = false;
		if (count == 5 && excount == 1 && prodCtx == true) {
			flag = true;
			//Assert.assertTrue(flag);
			System.out
					.println("Test case: testSessioninCallBackMethodOE passed");
		}

		else {
			//Assert.assertTrue(flag);
			System.out
					.println("Test case: testSessioninCallBackMethodOE failed");
			try {
				throw new Exception("Condition check failed.");
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		clearQueue(exQueue);
		tcmpListner.reset();
		resetCounter();
		resetFlag();

		System.out
				.println("******************Ending Test : testSessioninCallBackMethodOE***************************");

	}

	public void testTimetoLiveVariation(TestCompletionListenerCAPI lsnr)
			throws JMSException, InterruptedException {

		System.out
				.println("*****************Starting Test : testTimetoLiveVariation***********************");
		boolean flag1 = false;
		boolean flag2 = false;
		boolean flag3 = false;
		boolean flag4 = false;
		boolean exFlag = false;
		clearQueue(jmsQueue);
		lsnr.resetCounter();
		resetCounter();

		QueueSession queueSession = queueConnection.createQueueSession(false,
				javax.jms.Session.AUTO_ACKNOWLEDGE);

		TextMessage tmsg = queueSession
				.createTextMessage("This is a test with setAsyncSend");
		MessageProducer messageProducer = queueSession.createProducer(null);
		MessageConsumer messageConsumer = queueSession.createConsumer(jmsQueue);
		try {

			messageProducer.send(jmsQueue, tmsg, DeliveryMode.NON_PERSISTENT, 0,
					500, lsnr);

		} catch (Exception ex) {
			ex.printStackTrace();
		}
		Thread.sleep(2000);

		QueueBrowser qb = queueSession.createBrowser(jmsQueue);
		Enumeration e1 = qb.getEnumeration();

		int numMsgs = 0;
		// count number of messages
		while (e1.hasMoreElements()) {
			e1.nextElement();
			numMsgs++;
		}
		TextMessage msgRcvd = (TextMessage) messageConsumer.receiveNoWait();

		System.out.println("NumMsgs is:"+numMsgs);
		System.out.println("message received :"+ msgRcvd);
		if (numMsgs == 0 && msgRcvd == null && count == 1 && excount == 0)
			flag1 = true;
		else
			flag1 = false;

		lsnr.resetCounter();
		resetCounter();

		// Try with negative timetolive

		MessageProducer messageProducer2 = queueSession
				.createProducer(jmsQueue);
		try {
			messageProducer2.send(tmsg, DeliveryMode.PERSISTENT, 0, -100, lsnr);
		} catch (JMSException ex) {
			ex.printStackTrace();
			exFlag = true;

		}
		Thread.sleep(100);

		QueueBrowser qb2 = queueSession.createBrowser(jmsQueue);
		Enumeration e3 = qb2.getEnumeration();

		int numMsgs2 = 0;
		// count number of messages
		while (e3.hasMoreElements()) {
			e3.nextElement();
			numMsgs2++;
		}

		TextMessage msgRcvd2 = (TextMessage) messageConsumer.receiveNoWait();

		System.out.println("Printing values for negativetimetolive");
		System.out.println(numMsgs2);
		System.out.println(msgRcvd2);

		System.out.println(exFlag);
		System.out.println(count);
		System.out.println(excount);
		if (exFlag == true && numMsgs2 == 0 && msgRcvd2 == null && count == 0
				&& excount == 0)
			flag3 = true;
		else
			flag3 = false;

		System.out.println("Flag1:" + flag1);

		System.out.println("Flag3:" + flag3);

		boolean flagT = false;
		if (flag3 == true) {
			//flagT = true;
			//Assert.assertTrue(flagT);
			System.out.println("Test case :testTimetoLiveVariation passed");

		} else {
			//Assert.assertTrue(flagT);
			System.out.println("Test case :testTimetoLiveVariation failed");
			try {
				throw new Exception("Condition check failed.");
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		clearQueue(jmsQueue);
		lsnr.resetCounter();
		resetCounter();
		System.out
				.println("*******************Ending Test : testTimetoLiveVariation************************");

	}

	public void testPriorityVariation(TestCompletionListenerCAPI lsnr)
			throws JMSException, InterruptedException {
		System.out
				.println("*****************Starting Test : testPriorityVariation***********************");
		boolean flag = false;

		clearQueue(jmsQueue);
		lsnr.resetCounter();
		resetCounter();
		int expectedCount = 10;
		int exceptionCount = 0;
		lsnr.setLock(lock, expectedCount, exceptionCount);

		QueueSession queueSession = queueConnection.createQueueSession(false,
				javax.jms.Session.AUTO_ACKNOWLEDGE);

		TextMessage tmsg = queueSession
				.createTextMessage("This is a test with setAsyncSend");
		MessageConsumer messageConsumer = queueSession.createConsumer(jmsQueue);
		MessageProducer messageProducer = queueSession.createProducer(null);

		for (int i = 0; i < 10; i++) {

			tmsg = queueSession.createTextMessage();
			messageProducer.send(jmsQueue, tmsg, DeliveryMode.PERSISTENT, i,
					10000, lsnr);

		}

		synchronized (lock) {
			while (count != 10) {
				lock.wait(WAIT_TIME);
			}
		}

		TextMessage msgR = null;

		QueueBrowser qb = queueSession.createBrowser(jmsQueue);
		Enumeration e = qb.getEnumeration();

		int numMsgs = 0;

		// count number of messages
		while (e.hasMoreElements() && numMsgs < 10) {
			msgR = (TextMessage) messageConsumer.receive(WAIT_TIME);
			if (msgR.getJMSPriority() + numMsgs == 9) {

				System.out
						.print("Message has been received in correct order. The priority is :"
								+ msgR.getJMSPriority());
				;
				flag = true;

			} else {
				flag = false;

			}
			numMsgs++;
		}

		boolean flagT = false;
		if (count == 10 && flag == true) {
			flagT = true;

			//Assert.assertTrue(flagT);
			System.out.println("Test case : testPriorityVariation passed");
		} else {
			//Assert.assertTrue(flagT);
			System.out.println("Test case : testPriorityVariation failed");
			try {
				throw new Exception("Condition check failed.");
			} catch (Exception ex) {
				// TODO Auto-generated catch block
				ex.printStackTrace();
			}

		}
		lsnr.resetCounter();
		resetCounter();

		clearQueue(jmsQueue);

		System.out
				.println("*****************Ending Test : testPriorityVariation***********************");
	}

	public void testPriorityVariation_negative(TestCompletionListenerCAPI lsnr)
			throws JMSException, InterruptedException {
		System.out
				.println("*****************Starting Test : testPriorityVariation_negative***********************");
		boolean flag = false;

		clearQueue(jmsQueue);
		lsnr.resetCounter();
		resetCounter();

		QueueSession queueSession = queueConnection.createQueueSession(false,
				javax.jms.Session.AUTO_ACKNOWLEDGE);
		TextMessage tmsg = queueSession
				.createTextMessage("This is a test with setAsyncSend");

		MessageProducer messageProducer = queueSession.createProducer(null);

		tmsg = queueSession.createTextMessage();

		boolean exFlag = false;
		try {
			messageProducer.send(jmsQueue, tmsg, DeliveryMode.PERSISTENT, -2,
					10000, lsnr);
		} catch (JMSException ex) {
			ex.printStackTrace();
			exFlag = true;
		}

		if (exFlag == true) {

			//Assert.assertTrue(exFlag);

			System.out
					.println("Test case : testPriorityVariation_negative passed");
		} else {
			//Assert.assertTrue(flag);

			System.out
					.println("Test case : testPriorityVariation_negative failed");
			try {
				throw new Exception("Condition check failed.");
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		clearQueue(jmsQueue);
		lsnr.resetCounter();
		resetCounter();

		System.out
				.println("*****************Ending Test : testPriorityVariation_negative***********************");
	}

	public void testInvalidDeliveryMode(TestCompletionListenerCAPI lsnr)
			throws JMSException, InterruptedException {
		System.out
				.println("*****************Starting Test : testPriorityVariation_negative***********************");
		boolean flag = false;
		clearQueue(jmsQueue);
		lsnr.resetCounter();
		resetCounter();
		int expectedCount = 1;
		int exceptionCount = 0;
		lsnr.setLock(lock, expectedCount, exceptionCount);

		QueueSession queueSession = queueConnection.createQueueSession(false,
				javax.jms.Session.AUTO_ACKNOWLEDGE);

		TextMessage tmsg = queueSession
				.createTextMessage("This is a test with setAsyncSend");

		MessageProducer messageProducer = queueSession.createProducer(jmsQueue);
		MessageConsumer messageConsumer = queueSession.createConsumer(jmsQueue);

		messageProducer
				.send(tmsg, DeliveryMode.NON_PERSISTENT, 1, 100000, lsnr);
		// Thread.sleep(100);
		messageProducer.send(tmsg, DeliveryMode.PERSISTENT, 0, 100000, lsnr);
		// Thread.sleep(100);

		synchronized (lock) {
			while (count != 2) {
				lock.wait(WAIT_TIME);
			}
		}

		QueueBrowser qb = queueSession.createBrowser(jmsQueue);
		Enumeration e = qb.getEnumeration();

		int numMsgs = 0;

		// count number of messages
		while (e.hasMoreElements() && numMsgs < 2) {
			TextMessage tmsg1 = (TextMessage) messageConsumer
					.receive(WAIT_TIME);
			if (tmsg1.getJMSPriority() + numMsgs == 1) {

				System.out
						.print("Message has been received in correct order. The priority is :"
								+ tmsg.getJMSPriority());

				flag = true;

			} else {
				flag = false;

			}
			numMsgs++;
		}
		int i = 9;

		lsnr.resetCounter();
		resetCounter();
		boolean exFlag = false;
		try {
			messageProducer.send(tmsg, i, 0, 10000, lsnr);

		} catch (JMSException ex) {
			ex.printStackTrace();
			exFlag = true;

		}

		boolean flagT = false;
		if (count == 0 && excount == 0 && flag == true && exFlag == true) {
			flagT = true;
			//Assert.assertTrue(flagT);

			System.out.println("Test case :testInvalidDeliveryMode passed");
		} else {
			//Assert.assertTrue(flagT);

			System.out.println("Test case :testInvalidDeliveryMode failed");
			try {
				throw new Exception("Condition check failed.");
			} catch (Exception ex) {
				// TODO Auto-generated catch block
				ex.printStackTrace();
			}
		}

		clearQueue(jmsQueue);
		lsnr.resetCounter();
		resetCounter();
		System.out
				.println("*****************Ending Test : testPriorityVariation_negative***********************");

	}

	public void testNullEmptyMessage(TestCompletionListenerCAPI tcmpListener)
			throws JMSException, InterruptedException {
		
		boolean flag1 = false;
		boolean flag2 = false;
		System.out
				.println("*****************Starting Test : testNullEmptyMessage***********************");
		clearQueue(jmsQueue);
		tcmpListner.resetCounter();
		resetCounter();
		int expectedCount = 1;
		int exceptionCount = 0;
		tcmpListener.setLock(lock, expectedCount, exceptionCount);

		QueueSession queueSession = queueConnection.createQueueSession(false,
				javax.jms.Session.AUTO_ACKNOWLEDGE);
		MessageConsumer messageConsumer = queueSession.createConsumer(jmsQueue);
		MessageProducer messageProducer = queueSession.createProducer(jmsQueue);
		TextMessage tmsg = queueSession.createTextMessage("");
		messageProducer.send(tmsg, DeliveryMode.PERSISTENT, 0, 10000,
				tcmpListner);

		synchronized (lock) {
			while (count != 1) {
				lock.wait(WAIT_TIME);
			}
		}

		String msgRcvd = messageConsumer.receive(WAIT_TIME).getBody(
				String.class);
		System.out.println("Message Text Received is :" + msgRcvd);

		if (msgRcvd.equals("") && count == 1 && excount == 0)
			flag1 = true;
		else
			flag1 = false;

		try {
			messageProducer.send(null, DeliveryMode.PERSISTENT, 0, 10000,
					tcmpListner);
		} catch (MessageFormatException ex) {
			flag2 = true;
		}

		if (flag1 == true && flag2 == true && count == 1 && excount == 0){
			System.out.println("Test case : testNullEmptyMessage passed");}
		else{
			System.out.println("Test case : testNullEmptyMessage failed");
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
		System.out
				.println("*******************Ending Test : testNullEmptyMessage************************");

	}
	
	
	
    
	

	public void clearQueue(Queue queue) throws JMSException {

		QueueConnection qCon = GetJMSResourcesCAPI.getQueueConnection();
		qCon.start();
		QueueSession qSess = GetJMSResourcesCAPI.getQueueSession();
		qb = qSess.createBrowser(queue);
		MessageConsumer msgCons = qSess.createConsumer(queue);

		Enumeration e = qb.getEnumeration();

		int count = 0;
		// count number of messages
		while (e.hasMoreElements()) {
			e.nextElement();
			count++;
		}

		for (int i = count; count >= 0; count--) {
			msgCons.receiveNoWait();

		}

	}

	public int setCounter(int counter) {
		// TODO Auto-generated method stub
		count = counter;

		return count;
	}

	public int setexCounter(int exCounter) {
		// TODO Auto-generated method stub
		excount = exCounter;

		return excount;
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
	}

	public boolean setFlagClose(boolean close) {
		closeTC = close;
		return closeTC;
	}

	public boolean setFlagCommit(boolean commit) {
		commitTC = commit;
		return commitTC;
	}

	public boolean setFlagRollback(boolean rollback) {
		rollbackTC = rollback;
		return rollbackTC;
	}

	public void setOperation(boolean close, boolean commit, boolean rollback) {
		// TODO Auto-generated method stub
		closeUR = close;
		commitUR = commit;
		rollbackUR = rollback;
	}

	public void setOperationTest(boolean close, boolean commit,
			boolean rollback, boolean prod, boolean mprod) {
		// TODO Auto-generated method stub
		closeCtx = close;
		commitCtx = commit;
		rollbackCtx = rollback;
		prodCtx = prod;
		mprodCtx = mprod;
	}

}