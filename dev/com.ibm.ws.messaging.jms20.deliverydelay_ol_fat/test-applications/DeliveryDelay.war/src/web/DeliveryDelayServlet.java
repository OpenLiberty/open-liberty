/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2014
 *
 * The source code for this program is not published or otherwise divested 
 * of its trade secrets, irrespective of what has been deposited with the 
 * U.S. Copyright Office.
 */
package web;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.Enumeration;

import javax.jms.BytesMessage;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.IllegalStateRuntimeException;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.JMSProducer;
import javax.jms.JMSRuntimeException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueReceiver;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TemporaryQueue;
import javax.jms.TemporaryTopic;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicPublisher;
import javax.jms.TopicSession;
import javax.jms.TopicSubscriber;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 *
 */
@SuppressWarnings("serial")
public class DeliveryDelayServlet extends HttpServlet {

	QueueConnectionFactory jmsQCFBindings = null;
	QueueConnectionFactory jmsQCFTCP = null;
	TopicConnectionFactory jmsTCFBindings = null;
	TopicConnectionFactory jmsTCFTCP = null;
	Queue jmsQueue = null;
	Queue jmsQueue1 = null;
	Queue jmsQueue2 = null;
	Topic jmsTopic = null;
	Topic jmsTopic1 = null;
	Topic jmsTopic2 = null;
	long deliveryDelay = 10000;

	boolean exceptionFlag = false;

	@Override
	public void init() throws ServletException {
		// TODO Auto-generated method stub

		super.init();
		try {
			jmsQCFBindings = getQCF("jndi_JMS_BASE_QCF");
			jmsQCFTCP = getQCF("jndi_JMS_BASE_QCF1");
			jmsQueue = getQueue("eis/queue1");
			jmsQueue1 = getQueue("eis/queue2");
			jmsQueue2 = getQueue("eis/queue2");
			jmsTCFBindings = getTCF("eis/tcf");
			jmsTCFTCP = getTCF("eis/tcf1");
			jmsTopic = getTopic("eis/topic1");
			jmsTopic1 = getTopic("eis/topic2");
			jmsTopic2 = getTopic("eis/topic3");

		} catch (NamingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	@Override
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		String test = request.getParameter("test");
		PrintWriter out = response.getWriter();
		out.println("Starting " + test + "<br>");
		final TraceComponent tc = Tr.register(DeliveryDelayServlet.class); // injection
		// engine
		// doesn't
		// like
		// this
		// at
		// the
		// class
		// level
		Tr.entry(this, tc, test);
		try {
			System.out.println("Start: " + test);
			getClass().getMethod(test, HttpServletRequest.class,
					HttpServletResponse.class).invoke(this, request, response);
			out.println(test + " COMPLETED SUCCESSFULLY");
			System.out.println("End: " + test);
			Tr.exit(this, tc, test);
		} catch (Throwable x) {
			if (x instanceof InvocationTargetException)
				x = x.getCause();
			Tr.exit(this, tc, test, x);
			out.println("<pre>ERROR in " + test + ":");
			System.out.println("Error: " + test);
			x.printStackTrace(out);
			out.println("</pre>");
		}
	}

	// DeliveryDelayChanges

	public void testSetDeliveryDelay(HttpServletRequest request,
			HttpServletResponse response) throws Throwable {

		exceptionFlag = false;
		JMSContext jmsContext = jmsQCFBindings.createContext();
		emptyQueue(jmsQCFBindings, jmsQueue);
		JMSProducer producer = jmsContext.createProducer();
		JMSConsumer consumer = jmsContext.createConsumer(jmsQueue);
		producer.setDeliveryDelay(deliveryDelay);
		TextMessage send_msg = jmsContext
				.createTextMessage("testSetDeliveryDelay");

		sendAndCheckDeliveryTime(producer, jmsQueue, send_msg);

		TextMessage msg = (TextMessage) consumer.receiveNoWait();
		if (msg == null) {
			System.out.println("Expected NPE seen when receiving before delay");
			exceptionFlag = true;
		}

		if (!exceptionFlag)
			throw new WrongException(
					"testSetDeliveryDelay failed: Expected exception was not seen");

		exceptionFlag = false;

		TextMessage rec_msg = (TextMessage) consumer.receive(30000);
		if (!(rec_msg != null && (rec_msg.getText()
				.equals("testSetDeliveryDelay")))) {
			System.out.println("Receiving after delay: " + rec_msg.getText());
			System.out
					.println("Unexpected NPE seen when receiving after delay");
			exceptionFlag = true;
		}

		jmsContext.close();
		if (exceptionFlag)
			throw new WrongException(
					"testReceiveAfterDelay failed: Unexpected NPE seen");
	}

	public void testSetDeliveryDelay_Tcp(HttpServletRequest request,
			HttpServletResponse response) throws Throwable {

		exceptionFlag = false;
		JMSContext jmsContext = jmsQCFTCP.createContext();
		emptyQueue(jmsQCFTCP, jmsQueue);
		JMSProducer producer = jmsContext.createProducer();
		JMSConsumer consumer = jmsContext.createConsumer(jmsQueue);
		producer.setDeliveryDelay(deliveryDelay);
		TextMessage send_msg = jmsContext
				.createTextMessage("testSetDeliveryDelay_TCP");

		sendAndCheckDeliveryTime(producer, jmsQueue, send_msg);

		TextMessage msg = (TextMessage) consumer.receiveNoWait();
		if (msg == null) {
			System.out.println("Expected NPE seen when receiving before delay");
			exceptionFlag = true;
		}

		if (!exceptionFlag)
			throw new WrongException(
					"testSetDeliveryDelay_TCP failed: Expected exception was not seen");

		exceptionFlag = false;

		TextMessage rec_msg = (TextMessage) consumer.receive(30000);
		if (!(rec_msg != null && (rec_msg.getText()
				.equals("testSetDeliveryDelay_TCP")))) {
			System.out.println("Receiving after delay: " + rec_msg.getText());
			exceptionFlag = true;
		}

		jmsContext.close();
		if (exceptionFlag)
			throw new WrongException(
					"testReceiveAfterDelay_TCP failed: Unexpected NPE seen");

	}

	public void testSetDeliveryDelayTopic(HttpServletRequest request,
			HttpServletResponse response) throws Throwable {

		exceptionFlag = false;

		JMSContext jmsContext = jmsTCFBindings.createContext();
		JMSConsumer consumer = jmsContext.createConsumer(jmsTopic);
		JMSProducer producer = jmsContext.createProducer();
		producer.setDeliveryDelay(deliveryDelay);
		TextMessage send_msg = jmsContext
				.createTextMessage("testSetDeliveryDelayTopic");

		sendAndCheckDeliveryTime(producer, jmsTopic, send_msg);

		TextMessage msg = (TextMessage) consumer.receiveNoWait();
		if (msg == null) {
			System.out.println("Expected NPE seen when receiving before delay");
			exceptionFlag = true;
		}

		if (!exceptionFlag)
			throw new WrongException(
					"testSetDeliveryDelayTopic failed: Expected exception was not seen");

		exceptionFlag = false;

		TextMessage rec_msg = (TextMessage) consumer.receive(30000);
		if (!(rec_msg != null && (rec_msg.getText()
				.equals("testSetDeliveryDelayTopic")))) {
			System.out.println("Receiving after delay: " + rec_msg.getText());
			exceptionFlag = true;
		}

		consumer.close();
		jmsContext.close();
		if (exceptionFlag)
			throw new WrongException(
					"testReceiveAfterDelayTopic failed: Unexpected NPE seen");
	}

	public void testSetDeliveryDelayTopic_Tcp(HttpServletRequest request,
			HttpServletResponse response) throws Throwable {

		exceptionFlag = false;

		JMSContext jmsContext = jmsTCFTCP.createContext();

		JMSConsumer consumer = jmsContext.createConsumer(jmsTopic);
		JMSProducer producer = jmsContext.createProducer();
		producer.setDeliveryDelay(deliveryDelay);
		TextMessage send_msg = jmsContext
				.createTextMessage("testSetDeliveryDelayTopic_TCP");

		sendAndCheckDeliveryTime(producer, jmsTopic, send_msg);

		TextMessage msg = (TextMessage) consumer.receiveNoWait();
		if (msg == null) {
			System.out.println("Expected NPE seen when receiving before delay");
			exceptionFlag = true;
		}

		if (!exceptionFlag)
			throw new WrongException(
					"testSetDeliveryDelayTopic_TCP failed: Expected exception was not seen");

		exceptionFlag = false;

		TextMessage rec_msg = (TextMessage) consumer.receive(30000);
		if (!(rec_msg != null && (rec_msg.getText()
				.equals("testSetDeliveryDelayTopic_TCP")))) {
			System.out.println("Receiving after delay: " + rec_msg.getText());
			exceptionFlag = true;
		}

		consumer.close();
		jmsContext.close();
		if (exceptionFlag)
			throw new WrongException(
					"testReceiveAfterDelayTopic_TCP failed: Unexpected NPE seen");

	}

	public void testSetDeliveryDelayTopicDurSub(HttpServletRequest request,
			HttpServletResponse response) throws Throwable {

		exceptionFlag = false;
		JMSContext jmsContext = jmsTCFBindings.createContext();
		JMSConsumer durConsumer = jmsContext.createDurableConsumer(jmsTopic,
				"subs");
		JMSProducer producer = jmsContext.createProducer();

		producer.setDeliveryDelay(deliveryDelay);
		TextMessage send_msg = jmsContext
				.createTextMessage("testSetDeliveryDelayTopicDurSub");

		sendAndCheckDeliveryTime(producer, jmsTopic, send_msg);

		TextMessage msg = (TextMessage) durConsumer.receiveNoWait();
		if (msg == null) {
			System.out.println("Expected NPE seen when receiving before delay");
			exceptionFlag = true;
		}

		if (!exceptionFlag)
			throw new WrongException(
					"testSetDeliveryDelayTopicDurSub failed: Expected exception was not seen");

		exceptionFlag = false;

		TextMessage rec_msg = (TextMessage) durConsumer.receive(30000);
		if (!(rec_msg != null && (rec_msg.getText()
				.equals("testSetDeliveryDelayTopicDurSub")))) {
			System.out.println("Receiving after delay: " + rec_msg.getText());
			exceptionFlag = true;
		}

		durConsumer.close();
		jmsContext.unsubscribe("subs");
		jmsContext.close();
		if (exceptionFlag)
			throw new WrongException(
					"testSetDeliveryDelayTopicDurSub failed: Expected exception was not seen");
	}

	public void testSetDeliveryDelayTopicDurSub_Tcp(HttpServletRequest request,
			HttpServletResponse response) throws Throwable {

		exceptionFlag = false;
		JMSContext jmsContext = jmsTCFTCP.createContext();
		JMSConsumer durConsumer = jmsContext.createDurableConsumer(jmsTopic,
				"subs1");
		JMSProducer producer = jmsContext.createProducer();

		producer.setDeliveryDelay(deliveryDelay);
		TextMessage send_msg = jmsContext
				.createTextMessage("testSetDeliveryDelayTopicDurSub_Tcp");

		sendAndCheckDeliveryTime(producer, jmsTopic, send_msg);

		TextMessage msg = (TextMessage) durConsumer.receiveNoWait();
		if (msg == null) {
			System.out.println("Expected NPE seen when receiving before delay");
			exceptionFlag = true;
		}

		if (!exceptionFlag)
			throw new WrongException(
					"testSetDeliveryDelayTopicDurSub_Tcp failed: Expected exception was not seen");

		exceptionFlag = false;

		TextMessage rec_msg = (TextMessage) durConsumer.receive(30000);
		if (!(rec_msg != null && (rec_msg.getText()
				.equals("testSetDeliveryDelayTopicDurSub_Tcp")))) {
			System.out.println("Receiving after delay: " + rec_msg.getText());
			exceptionFlag = true;
		}
		durConsumer.close();
		jmsContext.unsubscribe("subs1");
		jmsContext.close();
		if (exceptionFlag)
			throw new WrongException(
					"testSetDeliveryDelayTopicDurSub_Tcp failed: Expected exception was not seen");

	}

	public void testReceiveAfterDelayTopicDurSub(HttpServletRequest request,
			HttpServletResponse response) throws Throwable {

		exceptionFlag = false;
		JMSContext jmsContext = jmsTCFBindings.createContext();

		JMSConsumer consumer = jmsContext.createConsumer(jmsTopic);

		TextMessage msg = (TextMessage) consumer.receiveNoWait();
		try {
			System.out.println("Receiving after delay: " + msg.getText());
		} catch (NullPointerException e) {
			System.out
					.println("Unexpected NPE seen when receiving after delay");
			exceptionFlag = true;
		}

		consumer.close();
		jmsContext.close();
		if (exceptionFlag)
			throw new WrongException(
					"testReceiveAfterDelayTopic failed: Unexpected NPE seen");
	}

	public void testReceiveAfterDelayTopicDurSub_Tcp(
			HttpServletRequest request, HttpServletResponse response)
			throws Throwable {

		exceptionFlag = false;
		JMSContext jmsContext = jmsTCFTCP.createContext();

		JMSConsumer consumer = jmsContext.createConsumer(jmsTopic);

		TextMessage msg = (TextMessage) consumer.receiveNoWait();
		try {
			System.out.println("Receiving after delay: " + msg.getText());
		} catch (NullPointerException e) {
			System.out
					.println("Unexpected NPE seen when receiving after delay");
			exceptionFlag = true;
		}

		consumer.close();
		jmsContext.close();
		if (exceptionFlag)
			throw new WrongException(
					"testReceiveAfterDelayTopic_TCP failed: Unexpected NPE seen");

	}

	public void testDeliveryDelayForDifferentDelays(HttpServletRequest request,
			HttpServletResponse response) throws Throwable {

		exceptionFlag = false;
		JMSContext jmsContext = jmsQCFBindings.createContext();

		Queue queue = (Queue) new InitialContext()
				.lookup("java:comp/env/jndi_INPUT_Q");
		emptyQueue(jmsQCFBindings, queue);
		JMSProducer producer = jmsContext.createProducer();
		producer.setDeliveryDelay(5000);

		producer.send(queue, "QueueBindingsMessage1");

		producer.setDeliveryDelay(1000);
		producer.send(queue, "QueueBindingsMessage2");

		Thread.sleep(8000);
		jmsContext.close();
	}

	public void testDeliveryDelayForDifferentDelays_Tcp(
			HttpServletRequest request, HttpServletResponse response)
			throws Throwable {

		exceptionFlag = false;
		JMSContext jmsContext = jmsQCFTCP.createContext();

		Queue queue = (Queue) new InitialContext()
				.lookup("java:comp/env/jndi_INPUT_Q");
		emptyQueue(jmsQCFTCP, queue);
		JMSProducer producer = jmsContext.createProducer();
		producer.setDeliveryDelay(5000);

		producer.send(queue, "QueueTCPMessage1");

		producer.setDeliveryDelay(1000);
		producer.send(queue, "QueueTCPMessage2");
		Thread.sleep(8000);
		jmsContext.close();
	}

	public void testDeliveryDelayForDifferentDelaysTopic(
			HttpServletRequest request, HttpServletResponse response)
			throws Throwable {

		exceptionFlag = false;
		JMSContext jmsContext = jmsTCFBindings.createContext();

		Topic topic = (Topic) new InitialContext()
				.lookup("java:comp/env/eis/topic");
		JMSProducer producer = jmsContext.createProducer();
		producer.setDeliveryDelay(5000);
		producer.send(topic, "TopicBindingsMessage1");

		producer.setDeliveryDelay(1000);
		producer.send(topic, "TopicBindingsMessage2");
		Thread.sleep(8000);
		jmsContext.close();
	}

	public void testDeliveryDelayForDifferentDelaysTopic_Tcp(
			HttpServletRequest request, HttpServletResponse response)
			throws Throwable {

		exceptionFlag = false;
		JMSContext jmsContext = jmsTCFTCP.createContext();

		Topic topic = (Topic) new InitialContext()
				.lookup("java:comp/env/eis/topic");
		JMSProducer producer = jmsContext.createProducer();
		producer.setDeliveryDelay(5000);

		producer.send(topic, "TopicTCPMessage1");

		producer.setDeliveryDelay(1000);
		producer.send(topic, "TopicTCPMessage2");
		Thread.sleep(8000);
		jmsContext.close();
	}

	public void testDeliveryMultipleMsgs(HttpServletRequest request,
			HttpServletResponse response) throws Throwable {

		exceptionFlag = false;
		boolean val1 = false;
		boolean val2 = false;

		JMSContext jmsContext = jmsQCFBindings.createContext();
		emptyQueue(jmsQCFBindings, jmsQueue);
		JMSProducer producer = jmsContext.createProducer();
		JMSConsumer consumer = jmsContext.createConsumer(jmsQueue);

		JMSConsumer consumer1 = jmsContext.createConsumer(jmsQueue1);
		producer.setDeliveryDelay(deliveryDelay);
		TextMessage send_msg1 = jmsContext
				.createTextMessage("testDeliveryMultipleMsgs1");

		TextMessage send_msg2 = jmsContext
				.createTextMessage("testDeliveryMultipleMsgs2");

		sendAndCheckDeliveryTime(producer, jmsQueue, send_msg1);

		TextMessage msg1 = (TextMessage) consumer.receiveNoWait();

		sendAndCheckDeliveryTime(producer, jmsQueue1, send_msg2);

		TextMessage msg2 = (TextMessage) consumer1.receiveNoWait();

		if (msg1 == null && msg2 == null) {
			System.out
					.println("No message received before the delay as expected");
			val1 = true;
		}

		msg1 = (TextMessage) consumer.receive(30000);
		msg2 = (TextMessage) consumer1.receive(30000);

		if ((msg1 != null && msg1.getText().equals("testDeliveryMultipleMsgs1"))
				&& (msg2 != null && msg2.getText().equals(
						"testDeliveryMultipleMsgs2"))) {
			System.out
					.println("Both messages have been received after delay as expected");
			val2 = true;
		}

		if (!(val1 && val2))
			exceptionFlag = true;

		consumer.close();
		consumer1.close();
		jmsContext.close();

		if (exceptionFlag)
			throw new WrongException("testDeliveryMultipleMsgs failed");

	}

	public void testDeliveryMultipleMsgs_Tcp(HttpServletRequest request,
			HttpServletResponse response) throws Throwable {

		exceptionFlag = false;
		boolean val1 = false;
		boolean val2 = false;

		JMSContext jmsContext = jmsQCFTCP.createContext();
		emptyQueue(jmsQCFTCP, jmsQueue);
		JMSProducer producer = jmsContext.createProducer();
		JMSConsumer consumer = jmsContext.createConsumer(jmsQueue);

		JMSConsumer consumer1 = jmsContext.createConsumer(jmsQueue1);
		producer.setDeliveryDelay(deliveryDelay);

		TextMessage send_msg1 = jmsContext
				.createTextMessage("testDeliveryMultipleMsgs_Tcp1");

		TextMessage send_msg2 = jmsContext
				.createTextMessage("testDeliveryMultipleMsgs_Tcp2");

		sendAndCheckDeliveryTime(producer, jmsQueue, send_msg1);

		TextMessage msg1 = (TextMessage) consumer.receiveNoWait();

		sendAndCheckDeliveryTime(producer, jmsQueue1, send_msg2);

		TextMessage msg2 = (TextMessage) consumer1.receiveNoWait();

		if (msg1 == null && msg2 == null) {
			System.out
					.println("No message received before the delay as expected");
			val1 = true;
		}

		msg1 = (TextMessage) consumer.receive(30000);
		msg2 = (TextMessage) consumer1.receive(30000);

		if ((msg1 != null && msg1.getText().equals(
				"testDeliveryMultipleMsgs_Tcp1"))
				&& (msg2 != null && msg2.getText().equals(
						"testDeliveryMultipleMsgs_Tcp2"))) {
			System.out
					.println("Both messages have been received after delay as expected");
			val2 = true;
		}

		if (!(val1 && val2))
			exceptionFlag = true;

		consumer.close();
		consumer1.close();
		jmsContext.close();

		if (exceptionFlag)
			throw new WrongException("testDeliveryMultipleMsgs_TCP failed");
	}

	public void testDeliveryMultipleMsgsTopic(HttpServletRequest request,
			HttpServletResponse response) throws Throwable {

		exceptionFlag = false;
		boolean val1 = false;
		boolean val2 = false;

		JMSContext jmsContext = jmsTCFBindings.createContext();

		JMSConsumer consumer = jmsContext.createConsumer(jmsTopic);

		JMSConsumer consumer1 = jmsContext.createConsumer(jmsTopic1);

		JMSProducer producer = jmsContext.createProducer();
		producer.setDeliveryDelay(deliveryDelay);

		TextMessage send_msg1 = jmsContext
				.createTextMessage("testDeliveryMultipleMsgsTopic1");

		TextMessage send_msg2 = jmsContext
				.createTextMessage("testDeliveryMultipleMsgsTopic2");

		sendAndCheckDeliveryTime(producer, jmsTopic, send_msg1);

		TextMessage msg1 = (TextMessage) consumer.receiveNoWait();

		sendAndCheckDeliveryTime(producer, jmsTopic1, send_msg2);

		TextMessage msg2 = (TextMessage) consumer1.receiveNoWait();

		if (msg1 == null && msg2 == null) {
			System.out
					.println("No message received before the delay as expected");
			val1 = true;
		}

		msg1 = (TextMessage) consumer.receive(30000);
		msg2 = (TextMessage) consumer1.receive(30000);

		if ((msg1 != null && msg1.getText().equals(
				"testDeliveryMultipleMsgsTopic1"))
				&& (msg2 != null && msg2.getText().equals(
						"testDeliveryMultipleMsgsTopic2"))) {
			System.out
					.println("Both messages have been received after delay as expected");
			val2 = true;
		}

		if (!(val1 && val2))
			exceptionFlag = true;

		consumer.close();
		consumer1.close();
		jmsContext.close();

		if (exceptionFlag)
			throw new WrongException("testDeliveryMultipleMsgsTopic failed");

	}

	public void testDeliveryMultipleMsgsTopic_Tcp(HttpServletRequest request,
			HttpServletResponse response) throws Throwable {

		exceptionFlag = false;
		boolean val1 = false;
		boolean val2 = false;

		JMSContext jmsContext = jmsTCFTCP.createContext();

		JMSConsumer consumer = jmsContext.createConsumer(jmsTopic);

		JMSConsumer consumer1 = jmsContext.createConsumer(jmsTopic1);

		JMSProducer producer = jmsContext.createProducer();
		producer.setDeliveryDelay(deliveryDelay);

		TextMessage send_msg1 = jmsContext
				.createTextMessage("testDeliveryMultipleMsgsTopic_Tcp1");

		TextMessage send_msg2 = jmsContext
				.createTextMessage("testDeliveryMultipleMsgsTopic_Tcp2");

		sendAndCheckDeliveryTime(producer, jmsTopic, send_msg1);

		TextMessage msg1 = (TextMessage) consumer.receiveNoWait();

		sendAndCheckDeliveryTime(producer, jmsTopic1, send_msg2);

		TextMessage msg2 = (TextMessage) consumer1.receiveNoWait();

		if (msg1 == null && msg2 == null) {
			System.out
					.println("No message received before the delay as expected");
			val1 = true;
		}

		msg1 = (TextMessage) consumer.receive(30000);
		msg2 = (TextMessage) consumer1.receive(30000);

		if ((msg1 != null && msg1.getText().equals(
				"testDeliveryMultipleMsgsTopic_Tcp1"))
				&& (msg2 != null && msg2.getText().equals(
						"testDeliveryMultipleMsgsTopic_Tcp2"))) {
			System.out
					.println("Both messages have been received after delay as expected");
			val2 = true;
		}

		if (!(val1 && val2))
			exceptionFlag = true;

		consumer.close();
		consumer1.close();
		jmsContext.close();

		if (exceptionFlag)
			throw new WrongException("testDeliveryMultipleMsgsTopic_TCP failed");
	}

	public void testDeliveryDelayZeroAndNegativeValues(
			HttpServletRequest request, HttpServletResponse response)
			throws Throwable {

		exceptionFlag = false;
		boolean val1 = false;
		boolean val2 = false;

		JMSContext jmsContext = jmsQCFBindings.createContext();
		emptyQueue(jmsQCFBindings, jmsQueue1);
		JMSProducer producer = jmsContext.createProducer();
		JMSConsumer consumer1 = jmsContext.createConsumer(jmsQueue1);
		producer.setDeliveryDelay(0);
		producer.send(jmsQueue1, "Zero Delivery Delay");

		TextMessage msg2 = (TextMessage) consumer1.receiveNoWait();

		if (msg2 != null) {
			System.out
					.println("Message was received immediately when delay is set to zero "
							+ msg2.getText());
			val1 = true;
		}

		try {
			producer.setDeliveryDelay(-10);
			producer.send(jmsQueue, "Negative Delivery Delay");
		} catch (JMSRuntimeException e) {
			System.out
					.println("Expected JMSRuntimeException seen when delivery delay is set to negative value");
			val2 = true;
		}

		if (!(val1 && val2))
			exceptionFlag = true;

		consumer1.close();
		jmsContext.close();

		if (exceptionFlag)
			throw new WrongException(
					"testDeliveryDelayZeroAndNegativeValues failed");
	}

	public void testDeliveryDelayZeroAndNegativeValues_Tcp(
			HttpServletRequest request, HttpServletResponse response)
			throws Throwable {

		exceptionFlag = false;
		boolean val1 = false;
		boolean val2 = false;

		JMSContext jmsContext = jmsQCFTCP.createContext();
		emptyQueue(jmsQCFTCP, jmsQueue1);
		JMSProducer producer = jmsContext.createProducer();
		JMSConsumer consumer1 = jmsContext.createConsumer(jmsQueue1);
		producer.setDeliveryDelay(0);
		producer.send(jmsQueue1, "Zero Delivery Delay");

		TextMessage msg2 = (TextMessage) consumer1.receiveNoWait();

		if (msg2 != null) {
			System.out
					.println("Message was received immediately when delay is set to zero "
							+ msg2.getText());
			val1 = true;
		}

		try {
			producer.setDeliveryDelay(-10);
		} catch (JMSRuntimeException e) {
			System.out
					.println("Expected JMSRuntimeException seen when delivery delay is set to negative value");
			val2 = true;
		}

		if (!(val1 && val2))
			exceptionFlag = true;

		consumer1.close();
		jmsContext.close();

		if (exceptionFlag)
			throw new WrongException(
					"testDeliveryDelayZeroAndNegativeValues failed");

	}

	public void testDeliveryDelayZeroAndNegativeValuesTopic(
			HttpServletRequest request, HttpServletResponse response)
			throws Throwable {

		exceptionFlag = false;
		boolean val1 = false;
		boolean val2 = false;

		JMSContext jmsContext = jmsTCFBindings.createContext();
		JMSConsumer consumer1 = jmsContext.createConsumer(jmsTopic);
		JMSProducer producer = jmsContext.createProducer();

		producer.setDeliveryDelay(0);
		producer.send(jmsTopic, "Zero Delivery Delay");

		TextMessage msg2 = (TextMessage) consumer1.receiveNoWait();

		if (msg2 != null) {
			System.out
					.println("Message was received immediately when delay is set to zero "
							+ msg2.getText());
			val1 = true;
		}

		try {
			producer.setDeliveryDelay(-10);
		} catch (JMSRuntimeException e) {
			System.out
					.println("Expected JMSRuntimeException seen when delivery delay is set to negative value");
			val2 = true;
		}

		if (!(val1 && val2))
			exceptionFlag = true;

		consumer1.close();
		jmsContext.close();

		if (exceptionFlag)
			throw new WrongException(
					"testDeliveryDelayZeroAndNegativeValuesTopic failed");
	}

	public void testDeliveryDelayZeroAndNegativeValuesTopic_Tcp(
			HttpServletRequest request, HttpServletResponse response)
			throws Throwable {

		exceptionFlag = false;
		boolean val1 = false;
		boolean val2 = false;

		JMSContext jmsContext = jmsTCFTCP.createContext();
		JMSConsumer consumer1 = jmsContext.createConsumer(jmsTopic);
		JMSProducer producer = jmsContext.createProducer();

		producer.setDeliveryDelay(0);
		producer.send(jmsTopic, "Zero Delivery Delay");

		TextMessage msg2 = (TextMessage) consumer1.receiveNoWait();

		if (msg2 != null) {
			System.out
					.println("Message was received immediately when delay is set to zero "
							+ msg2.getText());
			val1 = true;
		}

		try {
			producer.setDeliveryDelay(-10);
		} catch (JMSRuntimeException e) {
			System.out
					.println("Expected JMSRuntimeException seen when delivery delay is set to negative value");
			val2 = true;
		}

		if (!(val1 && val2))
			exceptionFlag = true;

		consumer1.close();
		jmsContext.close();

		if (exceptionFlag)
			throw new WrongException(
					"testDeliveryDelayZeroAndNegativeValuesTopic_Tcp failed");
	}

	public void testSettingMultipleProperties(HttpServletRequest request,
			HttpServletResponse response) throws Throwable {

		exceptionFlag = false;
		JMSContext jmsContext = jmsQCFBindings.createContext();
		emptyQueue(jmsQCFBindings, jmsQueue);
		JMSProducer producer = jmsContext.createProducer();
		JMSConsumer consumer = jmsContext.createConsumer(jmsQueue);
		producer.setDeliveryDelay(1000).setDisableMessageID(true);

		producer.send(jmsQueue, "testSettingMultipleProperties");

		TextMessage msg = (TextMessage) consumer.receive(30000);
		if (!(msg != null
				&& msg.getText().equals("testSettingMultipleProperties") && msg
					.getJMSMessageID() == null)) {
			exceptionFlag = true;
		} else {
			System.out.println(msg.getText());
			System.out.println(msg.getJMSMessageID());
		}
		consumer.close();
		jmsContext.close();
		if (exceptionFlag)
			throw new WrongException(
					"testSettingMultipleProperties failed: Expected exception not seen");
	}

	public void testSettingMultipleProperties_Tcp(HttpServletRequest request,
			HttpServletResponse response) throws Throwable {

		exceptionFlag = false;
		JMSContext jmsContext = jmsQCFTCP.createContext();
		emptyQueue(jmsQCFTCP, jmsQueue);
		JMSProducer producer = jmsContext.createProducer();
		JMSConsumer consumer = jmsContext.createConsumer(jmsQueue);
		producer.setDeliveryDelay(1000).setDisableMessageID(true);

		producer.send(jmsQueue, "testSettingMultipleProperties_Tcp");

		TextMessage msg = (TextMessage) consumer.receive(30000);
		if (!(msg != null
				&& msg.getText().equals("testSettingMultipleProperties_Tcp") && msg
					.getJMSMessageID() == null)) {
			exceptionFlag = true;
		}
		consumer.close();
		jmsContext.close();
		if (exceptionFlag)
			throw new WrongException(
					"testSettingMultipleProperties failed: Expected exception not seen");

	}

	public void testSettingMultiplePropertiesTopic(HttpServletRequest request,
			HttpServletResponse response) throws Throwable {

		exceptionFlag = false;
		JMSContext jmsContext = jmsTCFBindings.createContext();
		JMSConsumer consumer = jmsContext.createConsumer(jmsTopic);
		JMSProducer producer = jmsContext.createProducer();

		producer.setDeliveryDelay(1000).setDisableMessageID(true);

		producer.send(jmsTopic, "testSettingMultiplePropertiesTopic");

		TextMessage msg = (TextMessage) consumer.receive(30000);
		if (!(msg != null
				&& msg.getText().equals("testSettingMultiplePropertiesTopic") && msg
					.getJMSMessageID() == null)) {
			exceptionFlag = true;
		}
		consumer.close();
		jmsContext.close();
		if (exceptionFlag)
			throw new WrongException(
					"testSettingMultiplePropertiesTopic failed: Expected exception not seen");

	}

	public void testSettingMultiplePropertiesTopic_Tcp(
			HttpServletRequest request, HttpServletResponse response)
			throws Throwable {

		exceptionFlag = false;
		JMSContext jmsContext = jmsTCFTCP.createContext();
		JMSConsumer consumer = jmsContext.createConsumer(jmsTopic);
		JMSProducer producer = jmsContext.createProducer();

		producer.setDeliveryDelay(1000).setDisableMessageID(true);

		producer.send(jmsTopic, "testSettingMultiplePropertiesTopic_Tcp");

		TextMessage msg = (TextMessage) consumer.receive(30000);
		if (!(msg != null
				&& msg.getText().equals(
						"testSettingMultiplePropertiesTopic_Tcp") && msg
					.getJMSMessageID() == null)) {
			exceptionFlag = true;
		}
		consumer.close();
		jmsContext.close();
		if (exceptionFlag)
			throw new WrongException(
					"testSettingMultiplePropertiesTopic_Tcp failed: Expected exception not seen");

	}

	// testTransactedSend_B

	public void testTransactedSend_B(HttpServletRequest request,
			HttpServletResponse response) throws Throwable {

		exceptionFlag = false;

		JMSContext jmsContext = jmsQCFBindings
				.createContext(Session.SESSION_TRANSACTED);
		emptyQueue(jmsQCFBindings, jmsQueue);
		JMSProducer producer = jmsContext.createProducer();
		JMSConsumer consumer = jmsContext.createConsumer(jmsQueue);
		Message message = jmsContext.createTextMessage("testTransactedSend_B");
		producer.setDeliveryDelay(2000);
		producer.send(jmsQueue, message);
		long time_after_send = System.currentTimeMillis() + 2000;
		Thread.sleep(1000);
		jmsContext.commit();
		long time_after_commit = System.currentTimeMillis() + 2000;

		TextMessage msg = (TextMessage) consumer.receive(40000);
		jmsContext.commit();
		long rec_time = msg.getLongProperty("JMSDeliveryTime");
		if ((msg != null && msg.getText().equals("testTransactedSend_B"))) {
			if ((rec_time - time_after_send <= Math.abs(100))
					&& (rec_time - time_after_commit >= Math.abs(100))) {
				exceptionFlag = true;
			}
		} else {
			exceptionFlag = true;
		}

		System.out.println("After send: " + time_after_send);
		System.out.println(" After commit: " + time_after_commit);
		System.out.println(" Receive time: " + rec_time);

		consumer.close();
		jmsContext.close();

		if (exceptionFlag)
			throw new WrongException("testTransactedSend_B failed");

	}

	public void testTransactedSend_Tcp(HttpServletRequest request,
			HttpServletResponse response) throws Throwable {

		exceptionFlag = false;

		JMSContext jmsContext = jmsQCFTCP
				.createContext(Session.SESSION_TRANSACTED);
		emptyQueue(jmsQCFTCP, jmsQueue);
		JMSProducer producer = jmsContext.createProducer();
		JMSConsumer consumer = jmsContext.createConsumer(jmsQueue);
		Message message = jmsContext
				.createTextMessage("testTransactedSend_Tcp");
		producer.setDeliveryDelay(2000);
		producer.send(jmsQueue, message);
		long time_after_send = System.currentTimeMillis() + 2000;
		Thread.sleep(1000);
		jmsContext.commit();
		long time_after_commit = System.currentTimeMillis() + 2000;

		TextMessage msg = (TextMessage) consumer.receive(40000);
		jmsContext.commit();
		long rec_time = msg.getLongProperty("JMSDeliveryTime");
		if ((msg != null && msg.getText().equals("testTransactedSend_Tcp"))) {
			if ((rec_time - time_after_send <= Math.abs(100))
					&& (rec_time - time_after_commit >= Math.abs(100))) {
				exceptionFlag = true;
			}
		} else {
			exceptionFlag = true;
		}

		System.out.println("After send: " + time_after_send);
		System.out.println(" After commit: " + time_after_commit);
		System.out.println(" Receive time: " + rec_time);

		consumer.close();
		jmsContext.close();

		if (exceptionFlag)
			throw new WrongException("testTransactedSend_Tcp failed");
	}

	public void testTransactedSendTopic_B(HttpServletRequest request,
			HttpServletResponse response) throws Throwable {

		exceptionFlag = false;
		JMSContext jmsContext = jmsTCFBindings
				.createContext(Session.SESSION_TRANSACTED);
		JMSConsumer consumer = jmsContext.createConsumer(jmsTopic);
		JMSProducer producer = jmsContext.createProducer();

		Message message = jmsContext
				.createTextMessage("testTransactedSendTopic_B");
		producer.setDeliveryDelay(2000);
		producer.send(jmsTopic, message);
		long time_after_send = System.currentTimeMillis() + 2000;
		Thread.sleep(1000);
		jmsContext.commit();
		long time_after_commit = System.currentTimeMillis() + 2000;

		TextMessage msg = (TextMessage) consumer.receive(40000);
		jmsContext.commit();
		long rec_time = msg.getLongProperty("JMSDeliveryTime");
		if ((msg != null && msg.getText().equals("testTransactedSendTopic_B"))) {
			if ((rec_time - time_after_send <= Math.abs(100))
					&& (rec_time - time_after_commit >= Math.abs(100))) {
				exceptionFlag = true;
			}
		} else {
			exceptionFlag = true;
		}

		System.out.println("After send: " + time_after_send);
		System.out.println(" After commit: " + time_after_commit);
		System.out.println(" Receive time: " + rec_time);

		consumer.close();
		jmsContext.close();

		if (exceptionFlag)
			throw new WrongException("testTransactedSendTopic_B failed");
	}

	public void testTransactedSendTopic_Tcp(HttpServletRequest request,
			HttpServletResponse response) throws Throwable {

		exceptionFlag = false;
		JMSContext jmsContext = jmsTCFTCP
				.createContext(Session.SESSION_TRANSACTED);
		JMSConsumer consumer = jmsContext.createConsumer(jmsTopic);
		JMSProducer producer = jmsContext.createProducer();

		Message message = jmsContext
				.createTextMessage("testTransactedSendTopic_Tcp");
		producer.setDeliveryDelay(2000);
		producer.send(jmsTopic, message);
		long time_after_send = System.currentTimeMillis() + 2000;
		Thread.sleep(1000);
		jmsContext.commit();
		long time_after_commit = System.currentTimeMillis() + 2000;

		TextMessage msg = (TextMessage) consumer.receive(40000);
		jmsContext.commit();
		long rec_time = msg.getLongProperty("JMSDeliveryTime");
		if ((msg != null && msg.getText().equals("testTransactedSendTopic_Tcp"))) {
			if ((rec_time - time_after_send <= Math.abs(100))
					&& (rec_time - time_after_commit >= Math.abs(100))) {
				exceptionFlag = true;
			}
		} else {
			exceptionFlag = true;
		}

		System.out.println("After send: " + time_after_send);
		System.out.println(" After commit: " + time_after_commit);
		System.out.println(" Receive time: " + rec_time);

		consumer.close();
		jmsContext.close();

		if (exceptionFlag)
			throw new WrongException("testTransactedSendTopic_Tcp failed");
	}

	public void testTiming_B(HttpServletRequest request,
			HttpServletResponse response) throws Throwable {

		exceptionFlag = false;
		JMSContext jmsContext = jmsQCFBindings.createContext();
		emptyQueue(jmsQCFBindings, jmsQueue);
		JMSConsumer consumer = jmsContext.createConsumer(jmsQueue);
		JMSProducer producer = jmsContext.createProducer();

		Message send_msg = jmsContext.createTextMessage("testTiming_B");
		producer.setDeliveryDelay(1000);
		producer.send(jmsQueue, send_msg);

		long jmsDeliveryTime = send_msg.getLongProperty("JMSDeliveryTime");
		long jmsTimestamp = send_msg.getLongProperty("JMSTimestamp");

		if (!(jmsDeliveryTime == jmsTimestamp + 1000)) {
			System.out
					.println("The JMSDeliveryTime header value is not equal to (JMSTimestamp + delivery delay)");
			exceptionFlag = true;
		}

		if (exceptionFlag)
			throw new WrongException(
					"testTiming_B failed because JMSDeliveryTime header value is not equal to (JMSTimestamp + delivery delay)");

		exceptionFlag = false;
		TextMessage rec_msg = (TextMessage) consumer.receive(31000);
		long receive_time = System.currentTimeMillis();

		System.out.println(" delivery_time : " + jmsDeliveryTime);
		System.out.println(" receive_time : " + receive_time);

		if (rec_msg.getText().equals("testTiming_B")) {
			System.out.println("Correct msg received");
			if (!(jmsDeliveryTime <= receive_time)) {
				System.out
						.println("Check to verify that the time at which message was received was greater than (send time + delivery delay) FAILED");
				exceptionFlag = true;
			}
		} else {
			System.out.println("Incorrect message received");
			exceptionFlag = true;
		}

		consumer.close();
		jmsContext.close();
		if (exceptionFlag)
			throw new WrongException("testTiming_B failed: ");

	}

	public void testTiming_Tcp(HttpServletRequest request,
			HttpServletResponse response) throws Throwable {

		exceptionFlag = false;
		JMSContext jmsContext = jmsQCFTCP.createContext();
		emptyQueue(jmsQCFTCP, jmsQueue);
		JMSConsumer consumer = jmsContext.createConsumer(jmsQueue);
		JMSProducer producer = jmsContext.createProducer();

		Message send_msg = jmsContext.createTextMessage("testTiming_Tcp");
		producer.setDeliveryDelay(1000);
		producer.send(jmsQueue, send_msg);

		long jmsDeliveryTime = send_msg.getLongProperty("JMSDeliveryTime");
		long jmsTimestamp = send_msg.getLongProperty("JMSTimestamp");

		if (!(jmsDeliveryTime == jmsTimestamp + 1000)) {
			System.out
					.println("The JMSDeliveryTime header value is not equal to (JMSTimestamp + delivery delay)");
			exceptionFlag = true;
		}

		if (exceptionFlag)
			throw new WrongException(
					"testTiming_Tcp failed because JMSDeliveryTime header value is not equal to (JMSTimestamp + delivery delay)");

		exceptionFlag = false;
		TextMessage rec_msg = (TextMessage) consumer.receive(31000);
		long receive_time = System.currentTimeMillis();

		System.out.println(" delivery_time : " + jmsDeliveryTime);
		System.out.println(" receive_time : " + receive_time);

		if (rec_msg.getText().equals("testTiming_Tcp")) {
			System.out.println("Correct msg received");
			if (!(jmsDeliveryTime <= receive_time)) {
				System.out
						.println("Check to verify that the time at which message was received was greater than (send time + delivery delay) FAILED");
				exceptionFlag = true;
			}
		} else {
			System.out.println("Incorrect message received");
			exceptionFlag = true;
		}

		consumer.close();
		jmsContext.close();
		if (exceptionFlag)
			throw new WrongException("testTiming_Tcp failed");
	}

	public void testTimingTopic_B(HttpServletRequest request,
			HttpServletResponse response) throws Throwable {

		exceptionFlag = false;
		JMSContext jmsContext = jmsTCFBindings.createContext();
		JMSConsumer consumer = jmsContext.createConsumer(jmsTopic);
		JMSProducer producer = jmsContext.createProducer();

		Message send_msg = jmsContext.createTextMessage("testTimingTopic_B");
		producer.setDeliveryDelay(1000);
		producer.send(jmsTopic, send_msg);

		long jmsDeliveryTime = send_msg.getLongProperty("JMSDeliveryTime");
		long jmsTimestamp = send_msg.getLongProperty("JMSTimestamp");

		if (!(jmsDeliveryTime == jmsTimestamp + 1000)) {
			System.out
					.println("The JMSDeliveryTime header value is not equal to (JMSTimestamp + delivery delay)");
			exceptionFlag = true;
		}

		if (exceptionFlag)
			throw new WrongException(
					"testTimingTopic_B failed because JMSDeliveryTime header value is not equal to (JMSTimestamp + delivery delay)");

		exceptionFlag = false;
		TextMessage rec_msg = (TextMessage) consumer.receive(31000);
		long receive_time = System.currentTimeMillis();

		System.out.println(" delivery_time : " + jmsDeliveryTime);
		System.out.println(" receive_time : " + receive_time);

		if (rec_msg.getText().equals("testTimingTopic_B")) {
			System.out.println("Correct msg received");
			if (!(jmsDeliveryTime <= receive_time)) {
				System.out
						.println("Check to verify that the time at which message was received was greater than (send time + delivery delay) FAILED");
				exceptionFlag = true;
			}
		} else {
			System.out.println("Incorrect message received");
			exceptionFlag = true;
		}

		consumer.close();
		jmsContext.close();
		if (exceptionFlag)
			throw new WrongException("testTimingTopic_B failed");
	}

	public void testTimingTopic_Tcp(HttpServletRequest request,
			HttpServletResponse response) throws Throwable {

		exceptionFlag = false;
		JMSContext jmsContext = jmsTCFTCP.createContext();
		JMSConsumer consumer = jmsContext.createConsumer(jmsTopic);
		JMSProducer producer = jmsContext.createProducer();

		Message send_msg = jmsContext.createTextMessage("testTimingTopic_Tcp");
		producer.setDeliveryDelay(1000);
		producer.send(jmsTopic, send_msg);

		long jmsDeliveryTime = send_msg.getLongProperty("JMSDeliveryTime");
		long jmsTimestamp = send_msg.getLongProperty("JMSTimestamp");

		if (!(jmsDeliveryTime == jmsTimestamp + 1000)) {
			System.out
					.println("The JMSDeliveryTime header value is not equal to (JMSTimestamp + delivery delay)");
			exceptionFlag = true;
		}

		if (exceptionFlag)
			throw new WrongException(
					"testTimingTopic_Tcp failed because JMSDeliveryTime header value is not equal to (JMSTimestamp + delivery delay)");

		exceptionFlag = false;
		TextMessage rec_msg = (TextMessage) consumer.receive(31000);
		long receive_time = System.currentTimeMillis();

		System.out.println(" delivery_time : " + jmsDeliveryTime);
		System.out.println(" receive_time : " + receive_time);

		if (rec_msg.getText().equals("testTimingTopic_Tcp")) {
			System.out.println("Correct msg received");
			if (!(jmsDeliveryTime <= receive_time)) {
				System.out
						.println("Check to verify that the time at which message was received was greater than (send time + delivery delay) FAILED");
				exceptionFlag = true;
			}
		} else {
			System.out.println("Incorrect message received");
			exceptionFlag = true;
		}
		consumer.close();
		jmsContext.close();

		if (exceptionFlag)
			throw new WrongException("testTimingTopic_Tcp failed");
	}

	public void testGetDeliveryDelay(HttpServletRequest request,
			HttpServletResponse response) throws Throwable {

		exceptionFlag = false;
		boolean val1 = false;
		boolean val2 = false;
		JMSContext jmsContext = jmsQCFBindings.createContext();

		JMSProducer producer = jmsContext.createProducer();

		long val = producer.getDeliveryDelay();

		if (val == 0) {
			System.out
					.println("testGetDeliveryDelay: getDeliveryDelay returns zero when not set as expected");
			val1 = true;
		}
		producer.setDeliveryDelay(1000);

		val = producer.getDeliveryDelay();

		if (val == 1000) {
			System.out
					.println("testGetDeliveryDelay: getDeliveryDelay returns same value as the set value as expected");
			val2 = true;
		}

		jmsContext.close();
		if (!(val1 && val2))
			throw new WrongException("testGetDeliveryDelay failed");

	}

	public void testGetDeliveryDelay_Tcp(HttpServletRequest request,
			HttpServletResponse response) throws Throwable {

		exceptionFlag = false;
		boolean val1 = false;
		boolean val2 = false;
		JMSContext jmsContext = jmsQCFTCP.createContext();

		JMSProducer producer = jmsContext.createProducer();

		long val = producer.getDeliveryDelay();

		if (val == 0) {
			System.out
					.println("testGetDeliveryDelay_Tcp: getDeliveryDelay returns zero when not set as expected");
			val1 = true;
		}
		producer.setDeliveryDelay(1000);

		val = producer.getDeliveryDelay();

		if (val == 1000) {
			System.out
					.println("testGetDeliveryDelay_Tcp: getDeliveryDelay returns same value as the set value as expected");
			val2 = true;
		}

		jmsContext.close();
		if (!(val1 && val2))
			throw new WrongException("testGetDeliveryDelay_TCP failed");

	}

	public void testGetDeliveryDelayTopic(HttpServletRequest request,
			HttpServletResponse response) throws Throwable {

		exceptionFlag = false;
		boolean val1 = false;
		boolean val2 = false;
		JMSContext jmsContext = jmsTCFBindings.createContext();

		JMSProducer producer = jmsContext.createProducer();

		long val = producer.getDeliveryDelay();

		if (val == 0) {
			System.out
					.println("testGetDeliveryDelay: getDeliveryDelay returns zero when not set as expected");
			val1 = true;
		}
		producer.setDeliveryDelay(1000);

		val = producer.getDeliveryDelay();

		if (val == 1000) {
			System.out
					.println("testGetDeliveryDelay: getDeliveryDelay returns same value as the set value as expected");
			val2 = true;
		}

		jmsContext.close();
		if (!(val1 && val2))
			throw new WrongException("testGetDeliveryDelayTopic failed");
	}

	public void testGetDeliveryDelayTopic_Tcp(HttpServletRequest request,
			HttpServletResponse response) throws Throwable {

		exceptionFlag = false;
		boolean val1 = false;
		boolean val2 = false;
		JMSContext jmsContext = jmsTCFTCP.createContext();
		JMSConsumer consumer = jmsContext.createConsumer(jmsTopic);
		JMSProducer producer = jmsContext.createProducer();

		long val = producer.getDeliveryDelay();

		if (val == 0) {
			System.out
					.println("testGetDeliveryDelay: getDeliveryDelay returns zero when not set as expected");
			val1 = true;
		}
		producer.setDeliveryDelay(1000);
		val = producer.getDeliveryDelay();

		if (val == 1000) {
			System.out
					.println("testGetDeliveryDelay: getDeliveryDelay returns same value as the set value as expected");
			val2 = true;
		}

		jmsContext.close();
		if (!(val1 && val2))
			throw new WrongException("testGetDeliveryDelayTopic failed");
	}

	// new tests for simplified API

	public void testPersistentMessage(HttpServletRequest request,
			HttpServletResponse response) throws Throwable {

		exceptionFlag = false;
		JMSContext jmsContext = jmsQCFBindings.createContext();
		JMSConsumer consumer = jmsContext.createConsumer(jmsQueue);
		JMSProducer producer = jmsContext.createProducer();
		emptyQueue(jmsQCFBindings, jmsQueue);
		emptyQueue(jmsQCFBindings, jmsQueue1);
		producer.setDeliveryMode(DeliveryMode.PERSISTENT)
				.setDeliveryDelay(1000);
		producer.send(jmsQueue, "testPersistentMessage_PersistentMsg");

		producer.setDeliveryDelay(1000).setDeliveryMode(
				DeliveryMode.NON_PERSISTENT);
		producer.send(jmsQueue1, "testPersistentMessage_NonPersistentMsg");
		consumer.close();
		jmsContext.close();
	}

	public void testPersistentMessageReceive(HttpServletRequest request,
			HttpServletResponse response) throws Throwable {

		exceptionFlag = false;
		JMSContext jmsContext = jmsQCFBindings.createContext();
		JMSConsumer consumer1 = jmsContext.createConsumer(jmsQueue);
		JMSConsumer consumer2 = jmsContext.createConsumer(jmsQueue1);
		JMSProducer producer = jmsContext.createProducer();

		TextMessage msg1 = (TextMessage) consumer1.receive(30000);
		System.out.println("Received message is " + msg1);

		TextMessage msg2 = (TextMessage) consumer2.receive(30000);
		System.out.println("Received message is " + msg2);
		if (!(msg1 != null
				&& msg1.getText().equals("testPersistentMessage_PersistentMsg") && msg2 == null))

			exceptionFlag = true;

		consumer1.close();
		consumer2.close();
		if (exceptionFlag)
			throw new WrongException("testPersistentMessageReceive failed");
	}

	public void testPersistentMessage_Tcp(HttpServletRequest request,
			HttpServletResponse response) throws Throwable {

		exceptionFlag = false;
		JMSContext jmsContext = jmsQCFTCP.createContext();
		JMSConsumer consumer = jmsContext.createConsumer(jmsQueue);
		JMSProducer producer = jmsContext.createProducer();
		emptyQueue(jmsQCFTCP, jmsQueue);
		emptyQueue(jmsQCFTCP, jmsQueue1);
		producer.setDeliveryMode(DeliveryMode.PERSISTENT)
				.setDeliveryDelay(1000);
		producer.send(jmsQueue, "testPersistentMessage_PersistentMsgTcp");

		producer.setDeliveryDelay(1000).setDeliveryMode(
				DeliveryMode.NON_PERSISTENT);
		producer.send(jmsQueue1, "testPersistentMessage_NonPersistentMsgTcp");
		consumer.close();
		jmsContext.close();
	}

	public void testPersistentMessageReceive_Tcp(HttpServletRequest request,
			HttpServletResponse response) throws Throwable {

		exceptionFlag = false;
		JMSContext jmsContext = jmsQCFTCP.createContext();
		JMSConsumer consumer1 = jmsContext.createConsumer(jmsQueue);
		JMSConsumer consumer2 = jmsContext.createConsumer(jmsQueue1);
		JMSProducer producer = jmsContext.createProducer();

		TextMessage msg1 = (TextMessage) consumer1.receive(30000);
		System.out.println("Received message is " + msg1);

		TextMessage msg2 = (TextMessage) consumer2.receive(30000);
		System.out.println("Received message is " + msg2);
		if (!(msg1 != null
				&& msg1.getText().equals(
						"testPersistentMessage_PersistentMsgTcp") && msg2 == null))

			exceptionFlag = true;

		consumer1.close();
		consumer2.close();
		if (exceptionFlag)
			throw new WrongException("testPersistentMessageReceive_Tcp failed");
	}

	public void testPersistentMessageTopic(HttpServletRequest request,
			HttpServletResponse response) throws Throwable {

		exceptionFlag = false;
		JMSContext jmsContext = jmsTCFBindings.createContext();
		JMSConsumer consumer1 = jmsContext.createDurableConsumer(jmsTopic,
				"durPersMsg1");
		JMSConsumer consumer2 = jmsContext.createDurableConsumer(jmsTopic1,
				"durPersMsg2");
		JMSProducer producer = jmsContext.createProducer();

		producer.setDeliveryMode(DeliveryMode.PERSISTENT)
				.setDeliveryDelay(1000);
		producer.send(jmsTopic, "testPersistentMessage_PersistentMsgTopic");

		producer.setDeliveryDelay(1000).setDeliveryMode(
				DeliveryMode.NON_PERSISTENT);
		producer.send(jmsTopic1, "testPersistentMessage_NonPersistentMsgTopic");

	}

	public void testPersistentMessageReceiveTopic(HttpServletRequest request,
			HttpServletResponse response) throws Throwable {

		exceptionFlag = false;
		JMSContext jmsContext = jmsTCFBindings.createContext();
		JMSConsumer consumer1 = jmsContext.createDurableConsumer(jmsTopic,
				"durPersMsg1");
		JMSConsumer consumer2 = jmsContext.createDurableConsumer(jmsTopic1,
				"durPersMsg2");
		JMSProducer producer = jmsContext.createProducer();

		TextMessage msg1 = (TextMessage) consumer1.receive(30000);
		System.out.println("Received message is " + msg1);

		TextMessage msg2 = (TextMessage) consumer2.receive(30000);
		System.out.println("Received message is " + msg2);
		if (!(msg1 != null
				&& msg1.getText().equals(
						"testPersistentMessage_PersistentMsgTopic") && msg2 == null))

			exceptionFlag = true;

		consumer1.close();
		consumer2.close();
		jmsContext.unsubscribe("durPersMsg1");
		jmsContext.unsubscribe("durPersMsg2");
		jmsContext.close();
		if (exceptionFlag)
			throw new WrongException("testPersistentMessageReceiveTopic failed");
	}

	public void testPersistentMessageTopic_Tcp(HttpServletRequest request,
			HttpServletResponse response) throws Throwable {

		exceptionFlag = false;
		JMSContext jmsContext = jmsTCFTCP.createContext();
		JMSConsumer consumer1 = jmsContext.createDurableConsumer(jmsTopic,
				"durPersMsgTcp1");
		JMSConsumer consumer2 = jmsContext.createDurableConsumer(jmsTopic1,
				"durPersMsgTcp2");
		JMSProducer producer = jmsContext.createProducer();

		producer.setDeliveryMode(DeliveryMode.PERSISTENT)
				.setDeliveryDelay(1000);
		producer.send(jmsTopic, "testPersistentMessage_PersistentMsgTopicTcp");

		producer.setDeliveryDelay(1000).setDeliveryMode(
				DeliveryMode.NON_PERSISTENT);
		producer.send(jmsTopic1,
				"testPersistentMessage_NonPersistentMsgTopicTcp");

	}

	public void testPersistentMessageReceiveTopic_Tcp(
			HttpServletRequest request, HttpServletResponse response)
			throws Throwable {

		exceptionFlag = false;
		JMSContext jmsContext = jmsTCFTCP.createContext();
		JMSConsumer consumer1 = jmsContext.createDurableConsumer(jmsTopic,
				"durPersMsgTcp1");
		JMSConsumer consumer2 = jmsContext.createDurableConsumer(jmsTopic1,
				"durPersMsgTcp2");
		JMSProducer producer = jmsContext.createProducer();

		TextMessage msg1 = (TextMessage) consumer1.receive(30000);
		System.out.println("Received message is " + msg1);

		TextMessage msg2 = (TextMessage) consumer2.receive(30000);
		System.out.println("Received message is " + msg2);
		if (!(msg1 != null
				&& msg1.getText().equals(
						"testPersistentMessage_PersistentMsgTopicTcp") && msg2 == null))

			exceptionFlag = true;

		consumer1.close();
		consumer2.close();
		jmsContext.unsubscribe("durPersMsgTcp1");
		jmsContext.unsubscribe("durPersMsgTcp2");
		jmsContext.close();

		if (exceptionFlag)
			throw new WrongException(
					"testPersistentMessageStoreReceiveTopic_Tcp failed");
	}

	public void testTimeToLiveWithDeliveryDelay(HttpServletRequest request,
			HttpServletResponse response) throws Throwable {

		exceptionFlag = false;
		JMSContext jmsContext = jmsQCFBindings.createContext();
		emptyQueue(jmsQCFBindings, jmsQueue);
		JMSProducer producer = jmsContext.createProducer();

		producer.setTimeToLive(1000);
		producer.setDeliveryDelay(2000);

		producer.send(jmsQueue, "testTimeToLiveWithDeliveryDelay");
		JMSConsumer consumer = jmsContext.createConsumer(jmsQueue);

		TextMessage rec_msg = (TextMessage) consumer.receive(30000);
		System.out.println("Message received " + rec_msg);
		/*
		 * if (!(rec_msg != null && (rec_msg.getText()
		 * .equals("testTimeToLiveWithDeliveryDelay")))) {
		 * System.out.println("Receiving after delay: " + rec_msg.getText());
		 * exceptionFlag = true; }
		 * 
		 * jmsContext.close(); if (exceptionFlag) throw new WrongException(
		 * "testTimeToLiveWithDeliveryDelay failed: Unexpected NPE seen");
		 */
	}

	public void testReceiveBodyObjectMsgWithDD(HttpServletRequest request,
			HttpServletResponse response) throws Throwable {

		exceptionFlag = false;
		JMSContext jmsContext = jmsQCFBindings.createContext();
		emptyQueue(jmsQCFBindings, jmsQueue);
		JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);
		Object abc = new String("testReceiveBodyObjectMsg_B_SecOff");
		ObjectMessage message = jmsContext.createObjectMessage();
		message.setObject((Serializable) abc);
		jmsContext.createProducer().setDeliveryDelay(1000)
				.send(jmsQueue, message);

		Object message1 = jmsConsumer.receiveBody(Serializable.class, 30000);

		if (!(message1 != null && message1.equals(abc)))
			exceptionFlag = true;

		jmsConsumer.close();
		jmsContext.close();

		if (exceptionFlag)
			throw new WrongException(
					"testReceiveBodyObjectMsg failed: Expected message was not received");

	}

	public void testReceiveBodyObjectMsgWithDD_Tcp(HttpServletRequest request,
			HttpServletResponse response) throws Throwable {

		exceptionFlag = false;
		JMSContext jmsContext = jmsQCFTCP.createContext();
		emptyQueue(jmsQCFTCP, jmsQueue);
		JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);
		Object abc = new String("testReceiveBodyObjectMsg_TcpIp_SecOff");
		ObjectMessage message = jmsContext.createObjectMessage();
		message.setObject((Serializable) abc);
		jmsContext.createProducer().setDeliveryDelay(1000)
				.send(jmsQueue, message);

		Object message1 = jmsConsumer.receiveBody(Serializable.class, 30000);

		if (!(message1 != null && message1.equals(abc)))
			exceptionFlag = true;

		jmsConsumer.close();
		jmsContext.close();
		if (exceptionFlag)
			throw new WrongException(
					"testReceiveBodyObjectMsg_Tcp failed: Expected message was not received");
	}

	public void testCloseConsumer(HttpServletRequest request,
			HttpServletResponse response) throws Throwable {

		exceptionFlag = false;
		try {
			JMSContext jmsContext = jmsQCFBindings.createContext();
			JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);
			jmsConsumer.close();
			jmsConsumer.receive();

		} catch (JMSRuntimeException ex) {

			ex.printStackTrace();
			exceptionFlag = true;
			System.out
					.println("******THE EXCEPTION IN testCloseConsumer_B_SecOff IS : "
							+ ex.getClass().getName());
		}

		if (exceptionFlag == false)
			throw new WrongException(
					"testCloseConsumer failed: Expected exception did not occur");

	}

	public void testCloseConsumer_Tcp(HttpServletRequest request,
			HttpServletResponse response) throws Throwable {
		exceptionFlag = false;
		try {
			JMSContext jmsContext = jmsQCFTCP.createContext();
			JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);
			jmsConsumer.close();
			jmsConsumer.receive();

		} catch (JMSRuntimeException ex) {
			ex.printStackTrace();
			exceptionFlag = true;
			System.out
					.println("******THE EXCEPTION IN testCloseConsumer_TcpIp_SecOff IS : "
							+ ex.getClass().getName());
		}

		if (exceptionFlag == false)
			throw new WrongException(
					"testCloseConsumer_Tcp failed: Expected exception did not occur");

	}

	public void testQueueNameNullWithDD(HttpServletRequest request,
			HttpServletResponse response) throws Throwable {

		exceptionFlag = false;
		JMSContext jmsContext = jmsQCFBindings.createContext();

		JMSProducer jmsProducer = jmsContext.createProducer();
		try {
			Queue queue = jmsContext.createQueue(null);
			emptyQueue(jmsQCFBindings, queue);
			jmsProducer.setDeliveryDelay(1000).send(queue,
					"testQueueNameNull_B");

			JMSConsumer jmsConsumer = jmsContext.createConsumer(queue);
			TextMessage m = (TextMessage) jmsConsumer.receive(30000);
			System.out.println(m.getText());
		} catch (JMSRuntimeException e) {
			System.out
					.println("Expected JMSRuntimeException occured in testQueueNameNull_B");
			exceptionFlag = true;
			e.printStackTrace();
		}

		if (!exceptionFlag)
			throw new WrongException(
					"testQueueNameNull_B failed: Expected JMSRuntimeException was not seen");

	}

	public void testQueueNameNullWithDD_Tcp(HttpServletRequest request,
			HttpServletResponse response) throws Throwable {

		exceptionFlag = false;
		JMSContext jmsContext = jmsQCFTCP.createContext();

		JMSProducer jmsProducer = jmsContext.createProducer();
		try {
			Queue queue = jmsContext.createQueue(null);
			emptyQueue(jmsQCFTCP, queue);
			jmsProducer.setDeliveryDelay(1000).send(queue,
					"testQueueNameNull_TCP");

			JMSConsumer jmsConsumer = jmsContext.createConsumer(queue);
			TextMessage m = (TextMessage) jmsConsumer.receive(30000);
			System.out.println(m.getText());
		} catch (JMSRuntimeException e) {
			System.out
					.println("Expected JMSRuntimeException occured in testQueueNameNull_TcpIp");
			exceptionFlag = true;
			e.printStackTrace();
		}

		if (!exceptionFlag)
			throw new WrongException(
					"testQueueNameNull_TcpIp failed: Expected JMSRuntimeException was not seen");
	}

	public void testTopicNameNullWithDD(HttpServletRequest request,
			HttpServletResponse response) throws Throwable {

		exceptionFlag = false;
		JMSContext jmsContext = jmsTCFBindings.createContext();
		try {

			Topic topic = jmsContext.createTopic(null);
			JMSConsumer jmsConsumer = jmsContext.createConsumer(topic);

			jmsContext.createProducer().setDeliveryDelay(1000)
					.send(topic, "testTopicNameNull_B");

			TextMessage msg = (TextMessage) jmsConsumer.receive(30000);
			System.out.println(msg.getText());
		} catch (JMSRuntimeException ex) {
			System.out
					.println("Expected JMSRuntimeException seen in testTopicNameNull_B");
			exceptionFlag = true;
			ex.printStackTrace();
		}

		jmsContext.close();
		if (!exceptionFlag)
			throw new WrongException(
					"testTopicNameNull_B failed: Expected JMSRuntimeException was not seen");
	}

	public void testTopicNameNullWithDD_Tcp(HttpServletRequest request,
			HttpServletResponse response) throws Throwable {

		exceptionFlag = false;
		JMSContext jmsContext = jmsTCFTCP.createContext();
		try {

			Topic topic = jmsContext.createTopic(null);
			JMSConsumer jmsConsumer = jmsContext.createConsumer(topic);

			jmsContext.createProducer().setDeliveryDelay(1000)
					.send(topic, "testTopicNameNull_TcpIP");

			TextMessage msg = (TextMessage) jmsConsumer.receive(30000);
			System.out.println(msg.getText());
		} catch (JMSRuntimeException ex) {
			System.out
					.println("Expected JMSRuntimeException seen in testTopicNameNull_TcpIp");
			exceptionFlag = true;
			ex.printStackTrace();
		}

		jmsContext.close();
		if (!exceptionFlag)
			throw new WrongException(
					"testTopicNameNull_TcpIp failed: Expected JMSRuntimeException was not seen");

	}

	public void testAckOnClosedContextWithDD(HttpServletRequest request,
			HttpServletResponse response) throws Throwable {

		exceptionFlag = false;
		JMSContext jmsContext = jmsQCFBindings.createContext();
		emptyQueue(jmsQCFBindings, jmsQueue);
		JMSProducer jmsProducer = jmsContext.createProducer();

		jmsProducer.setDeliveryDelay(1000).send(jmsQueue,
				"testAckOnClosedContext_B_SecOff");
		JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);

		TextMessage msg = (TextMessage) jmsConsumer.receive(30000);
		jmsContext.close();
		try {
			jmsContext.acknowledge();

		} catch (IllegalStateRuntimeException ex) {

			ex.printStackTrace();
			System.out
					.println("******THE EXCEPTION IN testAckOnClosedContextWithDD IS : "
							+ ex.getClass().getName());
			exceptionFlag = true;
		}

		jmsConsumer.close();
		jmsContext.close();
		if (!exceptionFlag)
			throw new WrongException(
					"testAckOnClosedContextWithDD failed: Expected exception was not seen");
	}

	public void testAckOnClosedContextWithDD_Tcp(HttpServletRequest request,
			HttpServletResponse response) throws Throwable {

		exceptionFlag = false;
		JMSContext jmsContext = jmsQCFTCP.createContext();
		emptyQueue(jmsQCFTCP, jmsQueue);
		JMSProducer jmsProducer = jmsContext.createProducer();

		jmsProducer.setDeliveryDelay(1000).send(jmsQueue,
				"testAckOnClosedContext_B_SecOff");
		JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);

		TextMessage msg = (TextMessage) jmsConsumer.receive(30000);
		jmsContext.close();
		try {
			jmsContext.acknowledge();

		} catch (IllegalStateRuntimeException ex) {

			ex.printStackTrace();
			System.out
					.println("******THE EXCEPTION IN testAckOnClosedContextWithDD_Tcp IS : "
							+ ex.getClass().getName());
			exceptionFlag = true;
		}

		jmsConsumer.close();
		jmsContext.close();
		if (!exceptionFlag)
			throw new WrongException(
					"testAckOnClosedContextWithDD_Tcp failed: Expected exception was not seen");

	}

	public void testCreateConsumerWithMsgSelectorWithDD(
			HttpServletRequest request, HttpServletResponse response)
			throws Throwable {

		exceptionFlag = false;
		JMSContext jmsContext = jmsQCFBindings.createContext();
		emptyQueue(jmsQCFBindings, jmsQueue);
		JMSProducer jmsProducer = jmsContext.createProducer();
		JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue,
				"Team = 'SIB'");

		TextMessage message = jmsContext
				.createTextMessage("testCreateConsumerWithMsgSelector_B_SecOff");
		message.setStringProperty("Team", "SIB");
		jmsProducer.setDeliveryDelay(1000).send(jmsQueue, message);

		TextMessage msg = (TextMessage) jmsConsumer.receive(30000);
		System.out.println("Message is : " + msg.getText()
				+ " and property is - Team = " + msg.getStringProperty("Team"));

		jmsConsumer.close();
		jmsContext.close();

		if (!(msg != null && msg.getText().equals(
				"testCreateConsumerWithMsgSelector_B_SecOff"))
				&& msg.getStringProperty("Team").equals("SIB"))
			exceptionFlag = true;

		if (exceptionFlag)
			throw new WrongException(
					"testCreateConsumerWithMsgSelectorWithDD failed: Expected message or property value was not received");

	}

	public void testCreateConsumerWithMsgSelectorWithDD_Tcp(
			HttpServletRequest request, HttpServletResponse response)
			throws Throwable {

		exceptionFlag = false;
		JMSContext jmsContext = jmsQCFTCP.createContext();
		emptyQueue(jmsQCFTCP, jmsQueue);
		JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue,
				"Team = 'SIB'");
		JMSProducer jmsProducer = jmsContext.createProducer();
		TextMessage message = jmsContext
				.createTextMessage("testCreateConsumerWithMsgSelector_TcpIp_SecOff");
		message.setStringProperty("Team", "SIB");
		jmsProducer.setDeliveryDelay(1000).send(jmsQueue, message);

		TextMessage msg = (TextMessage) jmsConsumer.receive(30000);
		System.out.println("Message is : " + msg.getText()
				+ " and property is - Team = " + msg.getStringProperty("Team"));

		jmsConsumer.close();
		jmsContext.close();

		if (!(msg != null && msg.getText().equals(
				"testCreateConsumerWithMsgSelector_TcpIp_SecOff"))
				&& msg.getStringProperty("Team").equals("SIB"))
			exceptionFlag = true;

		if (exceptionFlag)
			throw new WrongException(
					"testCreateConsumerWithMsgSelectorWithDD_Tcp failed: Expected message or property value was not received");
	}

	public void testCreateConsumerWithMsgSelectorWithDDTopic(
			HttpServletRequest request, HttpServletResponse response)
			throws Throwable {

		exceptionFlag = false;
		JMSContext jmsContext = jmsTCFBindings.createContext();
		JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsTopic,
				"Team = 'SIB'");
		JMSProducer jmsProducer = jmsContext.createProducer();
		TextMessage message = jmsContext
				.createTextMessage("testCreateConsumerWithMsgSelectorTopic_B_SecOff");
		message.setStringProperty("Team", "SIB");
		jmsProducer.setDeliveryDelay(1000).send(jmsTopic, message);

		TextMessage msg = (TextMessage) jmsConsumer.receive(30000);
		System.out.println("Message is : " + msg.getText()
				+ " and property is - Team = " + msg.getStringProperty("Team"));

		jmsConsumer.close();
		jmsContext.close();

		if (!(msg != null && msg.getText().equals(
				"testCreateConsumerWithMsgSelectorTopic_B_SecOff"))
				&& msg.getStringProperty("Team").equals("SIB"))
			exceptionFlag = true;

		if (exceptionFlag)
			throw new WrongException(
					"testCreateConsumerWithMsgSelectorWithDDTopic failed: Expected message or property value was not received");

	}

	public void testCreateConsumerWithMsgSelectorWithDDTopic_Tcp(
			HttpServletRequest request, HttpServletResponse response)
			throws Throwable {

		exceptionFlag = false;
		JMSContext jmsContext = jmsTCFTCP.createContext();
		JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsTopic,
				"Team = 'SIB'");
		JMSProducer jmsProducer = jmsContext.createProducer();
		TextMessage message = jmsContext
				.createTextMessage("testCreateConsumerWithMsgSelectorTopic_TcpIp_SecOff");
		message.setStringProperty("Team", "SIB");
		jmsProducer.setDeliveryDelay(1000).send(jmsTopic, message);

		TextMessage msg = (TextMessage) jmsConsumer.receive(30000);
		System.out.println("Message is : " + msg.getText()
				+ " and property is - Team = " + msg.getStringProperty("Team"));

		jmsConsumer.close();
		jmsContext.close();

		if (!(msg != null && msg.getText().equals(
				"testCreateConsumerWithMsgSelectorTopic_TcpIp_SecOff"))
				&& msg.getStringProperty("Team").equals("SIB"))
			exceptionFlag = true;

		if (exceptionFlag)
			throw new WrongException(
					"testCreateConsumerWithMsgSelectorWithDDTopic_Tcp failed: Expected message or property value was not received");

	}

	public void testJMSPriorityWithDD(HttpServletRequest request,
			HttpServletResponse response) throws Throwable

	{

		exceptionFlag = false;
		JMSContext jmsContextQCFBindings = jmsQCFBindings.createContext();
		emptyQueue(jmsQCFBindings, jmsQueue);
		JMSConsumer consumer = jmsContextQCFBindings.createConsumer(jmsQueue);
		Message messageQCFBindings = jmsContextQCFBindings.createMessage();

		messageQCFBindings.setJMSPriority(9);

		jmsContextQCFBindings.createProducer().setPriority(1)
				.setDeliveryDelay(1000).send(jmsQueue, messageQCFBindings);
		int pri = consumer.receive(30000).getJMSPriority();

		if (!(pri == 1))
			exceptionFlag = true;

		consumer.close();
		jmsContextQCFBindings.close();

		if (exceptionFlag)
			throw new WrongException("testJMSPriorityWithDD failed");

	}

	public void testJMSPriorityWithDD_Tcp(HttpServletRequest request,
			HttpServletResponse response) throws Throwable

	{
		exceptionFlag = false;
		JMSContext jmsContextQCFTCP = jmsQCFTCP.createContext();
		emptyQueue(jmsQCFTCP, jmsQueue);
		JMSConsumer consumer = jmsContextQCFTCP.createConsumer(jmsQueue);
		Message messageQCFTCP = jmsContextQCFTCP.createMessage();

		messageQCFTCP.setJMSPriority(9);

		jmsContextQCFTCP.createProducer().setPriority(1).setDeliveryDelay(1000)
				.send(jmsQueue, messageQCFTCP);
		int pri = consumer.receive(30000).getJMSPriority();

		if (!(pri == 1))
			exceptionFlag = true;

		consumer.close();
		jmsContextQCFTCP.close();

		if (exceptionFlag)
			throw new WrongException("testJMSPriorityWithDD_Tcp failed");

	}

	public void testConnStartAuto_createContextUserSessionModeWithDD(
			HttpServletRequest request, HttpServletResponse response)
			throws Throwable {
		exceptionFlag = false;

		String userName = "user1";
		String password = "user1pwd";
		int smode = JMSContext.AUTO_ACKNOWLEDGE;

		JMSContext jmsContextT = jmsTCFBindings.createContext(userName,
				password, smode);
		JMSConsumer jmsConsumer = jmsContextT.createConsumer(jmsTopic);

		TextMessage msg = jmsContextT.createTextMessage("Hello");

		jmsContextT.createProducer().setDeliveryDelay(1000).send(jmsTopic, msg);

		TextMessage message = (TextMessage) jmsConsumer.receive(30000);
		if (!(message.getText().equals("Hello")))
			exceptionFlag = true;

		jmsConsumer.close();
		jmsContextT.close();

		if (exceptionFlag)
			throw new WrongException(
					"testConnStartAuto_createContextUserSessionModeWithDD failed");

	}

	public void testConnStartAuto_createContextUserSessionModeWithDD_Tcp(
			HttpServletRequest request, HttpServletResponse response)
			throws Throwable {
		exceptionFlag = false;

		String userName = "user1";
		String password = "user1pwd";
		int smode = JMSContext.AUTO_ACKNOWLEDGE;

		JMSContext jmsContextT = jmsTCFTCP.createContext(userName, password,
				smode);
		JMSConsumer jmsConsumer = jmsContextT.createConsumer(jmsTopic);

		TextMessage msg = jmsContextT.createTextMessage("Hello");

		jmsContextT.createProducer().setDeliveryDelay(1000).send(jmsTopic, msg);

		TextMessage message = (TextMessage) jmsConsumer.receive(30000);
		if (!(message.getText().equals("Hello")))
			exceptionFlag = true;

		jmsConsumer.close();
		jmsContextT.close();

		if (exceptionFlag)
			throw new WrongException(
					"testConnStartAuto_createContextUserSessionModeWithDD_Tcp failed");

	}

	public void testcreateBrowserWithDD(HttpServletRequest request,
			HttpServletResponse response) throws Throwable {

		exceptionFlag = false;

		JMSContext jmsContextQCFBindings = jmsQCFBindings.createContext();
		emptyQueue(jmsQCFBindings, jmsQueue2);
		jmsContextQCFBindings.createProducer().setDeliveryDelay(1000)
				.send(jmsQueue2, "Tester");

		Thread.sleep(2000);
		QueueBrowser queueBrowserQCFBindings = jmsContextQCFBindings
				.createBrowser(jmsQueue2);

		int numMsgs = getMessageCount(queueBrowserQCFBindings);

		jmsContextQCFBindings.createConsumer(jmsQueue2).receive(30000);

		if (!(numMsgs == 1))
			exceptionFlag = true;
		jmsContextQCFBindings.close();
		if (exceptionFlag)
			throw new WrongException("testcreateBrowserWithDD failed");

	}

	public void testcreateBrowserWithDD_Tcp(HttpServletRequest request,
			HttpServletResponse response) throws Throwable {
		exceptionFlag = false;

		JMSContext jmsContextQCFTCP = jmsQCFTCP.createContext();
		emptyQueue(jmsQCFTCP, jmsQueue2);
		jmsContextQCFTCP.createProducer().setDeliveryDelay(1000)
				.send(jmsQueue2, "Tester");
		Thread.sleep(2000);
		QueueBrowser queueBrowserQCFTCP = jmsContextQCFTCP
				.createBrowser(jmsQueue2);
		int numMsgs = getMessageCount(queueBrowserQCFTCP);
		jmsContextQCFTCP.createConsumer(jmsQueue2).receive(30000);

		if (!(numMsgs == 1))
			exceptionFlag = true;
		jmsContextQCFTCP.close();
		if (exceptionFlag)
			throw new WrongException("testcreateBrowserWithDD_Tcp failed");

	}

	public void testInitialJMSXDeliveryCountWithDD(HttpServletRequest request,
			HttpServletResponse response) throws Throwable {

		exceptionFlag = false;
		JMSContext jmsContext = jmsQCFBindings.createContext();
		emptyQueue(jmsQCFBindings, jmsQueue);
		JMSConsumer consumer = jmsContext.createConsumer(jmsQueue);
		JMSProducer producer = jmsContext.createProducer();
		producer.setDeliveryDelay(1000).send(jmsQueue,
				"testInitialJMSXDeliveryCount_B_SecOff");

		TextMessage msg = (TextMessage) consumer.receive(30000);
		System.out.println(msg.getText());

		if (!(msg.getIntProperty("JMSXDeliveryCount") == 1)) {
			System.out
					.println("Initial JMSXDeliveryCount value expected is 1 but actual value is "
							+ msg.getIntProperty("JMSXDeliveryCount"));
			exceptionFlag = true;
		}

		consumer.close();
		jmsContext.close();

		if (exceptionFlag)
			throw new WrongException(
					"testInitialJMSXDeliveryCountWithDD failed");
	}

	public void testInitialJMSXDeliveryCountWithDD_Tcp(
			HttpServletRequest request, HttpServletResponse response)
			throws Throwable {

		exceptionFlag = false;
		JMSContext jmsContext = jmsQCFTCP.createContext();
		emptyQueue(jmsQCFTCP, jmsQueue);
		JMSProducer producer = jmsContext.createProducer();
		JMSConsumer consumer = jmsContext.createConsumer(jmsQueue);
		producer.setDeliveryDelay(1000).send(jmsQueue,
				"testInitialJMSXDeliveryCount_TcpIp_SecOff");

		TextMessage msg = (TextMessage) consumer.receive(30000);
		System.out.println(msg.getText());

		if (!(msg.getIntProperty("JMSXDeliveryCount") == 1)) {
			System.out
					.println("Initial JMSXDeliveryCount value expected is 1 but actual value is "
							+ msg.getIntProperty("JMSXDeliveryCount"));
			exceptionFlag = true;
		}

		consumer.close();
		jmsContext.close();

		if (exceptionFlag)
			throw new WrongException(
					"testInitialJMSXDeliveryCountWithDD_Tcp failed");
	}

	public void testJMSProducerSendTextMessage_EmptyMessageWithDD_Topic(
			HttpServletRequest request, HttpServletResponse response)
			throws Throwable {

		exceptionFlag = false;
		JMSContext jmsContextTCFBindings = jmsTCFBindings.createContext();
		JMSConsumer jmsConsumer = jmsContextTCFBindings
				.createConsumer(jmsTopic);

		JMSProducer producer = jmsContextTCFBindings.createProducer();

		producer.setDeliveryDelay(1000).send(jmsTopic, "");

		String recvdMessage = jmsConsumer.receive(30000).getBody(String.class);

		if (!(recvdMessage.equals("")))
			exceptionFlag = true;

		jmsConsumer.close();
		jmsContextTCFBindings.close();

		if (exceptionFlag)
			throw new WrongException(
					"testJMSProducerSendTextMessage_EmptyMessageWithDD_Topic failed");
	}

	public void testJMSProducerSendTextMessage_EmptyMessageWithDD_Topic_Tcp(
			HttpServletRequest request, HttpServletResponse response)
			throws Throwable {

		exceptionFlag = false;
		JMSContext jmsContextTCFTCP = jmsTCFTCP.createContext();

		JMSConsumer jmsConsumer = jmsContextTCFTCP.createConsumer(jmsTopic);

		JMSProducer producer = jmsContextTCFTCP.createProducer();

		producer.setDeliveryDelay(1000).send(jmsTopic, "");

		String recvdMessage = jmsConsumer.receive(30000).getBody(String.class);

		if (!(recvdMessage.equals("")))
			exceptionFlag = true;

		jmsConsumer.close();
		jmsContextTCFTCP.close();

		if (exceptionFlag)
			throw new WrongException(
					"testJMSProducerSendTextMessage_EmptyMessage_Topic_Tcp failed");

	}

	public void testClearProperties_NotsetWithDD(HttpServletRequest request,
			HttpServletResponse response) throws Throwable {

		boolean flag = false;
		boolean compFlag = false;
		exceptionFlag = false;

		JMSContext jmsContextQCFBindings = jmsQCFBindings.createContext();

		JMSProducer jmsProducer = jmsContextQCFBindings.createProducer();

		try {
			jmsProducer.clearProperties();
			flag = true;
		} catch (Exception ex) {
			ex.printStackTrace();
			flag = false;
		}

		jmsProducer.setProperty("Name", "Tester").setDeliveryDelay(1000);
		jmsProducer.setProperty("ObjectType", new Integer(1414));

		jmsProducer.clearProperties();

		try {
			jmsProducer.clearProperties();
			compFlag = true;
		} catch (Exception ex) {
			ex.printStackTrace();
			compFlag = false;
		}

		if (!(flag == true && compFlag == true))
			exceptionFlag = true;

		jmsContextQCFBindings.close();

		if (exceptionFlag)
			throw new WrongException("testClearProperties_NotsetWithDD failed");

	}

	public void testClearProperties_NotsetWithDD_Tcp(
			HttpServletRequest request, HttpServletResponse response)
			throws Throwable {

		boolean flag = false;
		boolean compFlag = false;
		exceptionFlag = false;

		JMSContext jmsContextQCFTCP = jmsQCFTCP.createContext();

		JMSProducer jmsProducer = jmsContextQCFTCP.createProducer();

		try {
			jmsProducer.clearProperties();
			flag = true;
		} catch (Exception ex) {
			ex.printStackTrace();
			flag = false;
		}

		jmsProducer.setProperty("Name", "Tester").setDeliveryDelay(1000);
		jmsProducer.setProperty("ObjectType", new Integer(1414));

		jmsProducer.clearProperties();

		try {
			jmsProducer.clearProperties();
			compFlag = true;
		} catch (Exception ex) {
			ex.printStackTrace();
			compFlag = false;
		}

		if (!(flag == true && compFlag == true))
			exceptionFlag = true;

		jmsContextQCFTCP.close();

		if (exceptionFlag)
			throw new WrongException(
					"testClearProperties_NotsetWithDD_Tcp failed");

	}

	public void testStartJMSContextWithDD(HttpServletRequest request,
			HttpServletResponse response) throws Throwable {

		exceptionFlag = false;

		JMSContext jmsContextQCFBindings = jmsQCFBindings.createContext();
		emptyQueue(jmsQCFBindings, jmsQueue);
		jmsContextQCFBindings.setAutoStart(false);

		String outbound = "Hello World";
		JMSConsumer jmsConsumerQCFBindings = jmsContextQCFBindings
				.createConsumer(jmsQueue);
		JMSProducer jmsProducerQCFBindings = jmsContextQCFBindings
				.createProducer();
		jmsProducerQCFBindings.setDeliveryDelay(1000).send(jmsQueue, outbound);
		jmsContextQCFBindings.start();

		TextMessage receiveMsg = (TextMessage) jmsConsumerQCFBindings
				.receive(30000);

		String inbound = "";
		inbound = receiveMsg.getText();

		if (outbound.equals(inbound))
			exceptionFlag = false;
		else
			exceptionFlag = true;

		jmsConsumerQCFBindings.close();
		jmsContextQCFBindings.close();

		if (exceptionFlag == true)
			throw new WrongException("testStartJMSContextWithDD failed");

	}

	public void testStartJMSContextWithDD_Tcp(HttpServletRequest request,
			HttpServletResponse response) throws Throwable {
		exceptionFlag = false;

		JMSContext jmsContextQCFTCP = jmsQCFTCP.createContext();
		emptyQueue(jmsQCFTCP, jmsQueue);
		jmsContextQCFTCP.setAutoStart(false);
		JMSConsumer jmsConsumerQCFTCP = jmsContextQCFTCP
				.createConsumer(jmsQueue);
		String outbound = "Hello World";
		JMSProducer jmsProducerQCFTCP = jmsContextQCFTCP.createProducer();
		jmsProducerQCFTCP.setDeliveryDelay(1000).send(jmsQueue, outbound);
		jmsContextQCFTCP.start();

		TextMessage receiveMsg = (TextMessage) jmsConsumerQCFTCP.receive(30000);

		String inbound = "";
		inbound = receiveMsg.getText();

		if (outbound.equals(inbound))
			exceptionFlag = false;
		else
			exceptionFlag = true;

		jmsConsumerQCFTCP.close();
		jmsContextQCFTCP.close();

		if (exceptionFlag == true)
			throw new WrongException("testStartJMSContextWithDD_Tcp failed");

	}

	public void testPTPTemporaryQueueWithDD(HttpServletRequest request,
			HttpServletResponse response) throws Throwable {
		exceptionFlag = false;

		JMSContext jmsContextQCFBindings = jmsQCFBindings.createContext();
		emptyQueue(jmsQCFBindings, jmsQueue);
		TemporaryQueue tempQ = jmsContextQCFBindings.createTemporaryQueue();

		JMSProducer jmsProducerQCFBindings = jmsContextQCFBindings
				.createProducer();

		JMSConsumer jmsConsumerQCFBindings = jmsContextQCFBindings
				.createConsumer(tempQ);

		jmsProducerQCFBindings.setDeliveryDelay(1000).send(tempQ,
				"testPTPTemporaryQueueWithDD");

		TextMessage recMessage = (TextMessage) jmsConsumerQCFBindings
				.receive(30000);

		if (recMessage.getText() == "testPTPTemporaryQueueWithDD") {
			exceptionFlag = false;
		} else {

			exceptionFlag = true;
		}
		jmsConsumerQCFBindings.close();
		jmsContextQCFBindings.close();

		if (exceptionFlag == true)
			throw new WrongException("testPTPTemporaryQueueWithDD failed");

	}

	public void testPTPTemporaryQueueWithDD_Tcp(HttpServletRequest request,
			HttpServletResponse response) throws Throwable {
		exceptionFlag = false;

		JMSContext jmsContextQCFTCP = jmsQCFTCP.createContext();
		emptyQueue(jmsQCFTCP, jmsQueue);
		TemporaryQueue tempQ = jmsContextQCFTCP.createTemporaryQueue();

		JMSProducer jmsProducerQCFTCP = jmsContextQCFTCP.createProducer();

		JMSConsumer jmsConsumerQCFTCP = jmsContextQCFTCP.createConsumer(tempQ);

		jmsProducerQCFTCP.setDeliveryDelay(1000).send(tempQ,
				"testPTPTemporaryQueueWithDD_Tcp");

		TextMessage recMessage = (TextMessage) jmsConsumerQCFTCP.receive(30000);

		if (recMessage.getText().equalsIgnoreCase(
				"testPTPTemporaryQueueWithDD_Tcp")) {
			exceptionFlag = false;
		} else {

			exceptionFlag = true;
		}
		jmsConsumerQCFTCP.close();
		jmsContextQCFTCP.close();

		if (exceptionFlag == true)
			throw new WrongException("testPTPTemporaryQueueWithDD_Tcp failed");
	}

	public void testTemporaryTopicPubSubWithDD(HttpServletRequest request,
			HttpServletResponse response) throws Throwable {
		exceptionFlag = false;

		JMSContext jmsContextTCFBindings = jmsTCFBindings.createContext();

		TemporaryTopic tempT = jmsContextTCFBindings.createTemporaryTopic();

		JMSProducer jmsProducerTCFBindings = jmsContextTCFBindings
				.createProducer();

		JMSConsumer jmsConsumerTCFBindings = jmsContextTCFBindings
				.createConsumer(tempT);

		jmsProducerTCFBindings.setDeliveryDelay(1000).send(tempT,
				"testTemporaryTopicPubSubWithDD");

		TextMessage recMessage = (TextMessage) jmsConsumerTCFBindings
				.receive(30000);

		if (recMessage.getText() == "testTemporaryTopicPubSubWithDD") {
			exceptionFlag = false;
		} else {

			exceptionFlag = true;
		}
		jmsConsumerTCFBindings.close();
		jmsContextTCFBindings.close();

		if (exceptionFlag == true)
			throw new WrongException("testTemporaryTopicPubSubWithDD failed");

	}

	public void testTemporaryTopicPubSubWithDD_Tcp(HttpServletRequest request,
			HttpServletResponse response) throws Throwable {

		exceptionFlag = false;

		JMSContext jmsContextTCFTCP = jmsTCFTCP.createContext();

		TemporaryTopic tempT = jmsContextTCFTCP.createTemporaryTopic();

		// create resources on the queue
		JMSProducer jmsProducerTCFTCP = jmsContextTCFTCP.createProducer();

		JMSConsumer jmsConsumerTCFTCP = jmsContextTCFTCP.createConsumer(tempT);

		jmsProducerTCFTCP.setDeliveryDelay(1000).send(tempT,
				"testTemporaryTopicPubSubWithDD_Tcp");

		TextMessage recMessage = (TextMessage) jmsConsumerTCFTCP.receive(30000);

		if (recMessage.getText().equals("testTemporaryTopicPubSubWithDD_Tcp")) {
			exceptionFlag = false;
		} else {

			exceptionFlag = true;
		}

		jmsConsumerTCFTCP.close();
		jmsContextTCFTCP.close();

		if (exceptionFlag == true)
			throw new WrongException(
					"testTemporaryTopicPubSubWithDD_Tcp failed");

	}

	public void testCommitLocalTransactionWithDD(HttpServletRequest request,
			HttpServletResponse response) throws Exception {

		exceptionFlag = false;
		try {

			JMSContext jmsContextQCFBindings = jmsQCFBindings
					.createContext(Session.SESSION_TRANSACTED);

			Message message = jmsContextQCFBindings
					.createTextMessage("testCommitLocalTransactionWithDD");
			emptyQueue(jmsQCFBindings, jmsQueue);
			JMSProducer jmsProducerQCFBindings = jmsContextQCFBindings
					.createProducer();
			jmsProducerQCFBindings.setDeliveryDelay(1000).send(jmsQueue,
					message);

			QueueBrowser qb = jmsContextQCFBindings.createBrowser(jmsQueue);
			Enumeration e = qb.getEnumeration();
			int numMsgs = 0;
			while (e.hasMoreElements()) {
				TextMessage message1 = (TextMessage) e.nextElement();
				numMsgs++;
			}

			jmsContextQCFBindings.commit();
			QueueBrowser qb1 = jmsContextQCFBindings.createBrowser(jmsQueue);
			Enumeration e1 = qb1.getEnumeration();
			int numMsgs1 = 0;
			// count number of messages
			while (e1.hasMoreElements()) {
				TextMessage message1 = (TextMessage) e1.nextElement();
				numMsgs1++;
			}
			JMSConsumer jmsConsumerQCFBindings = jmsContextQCFBindings
					.createConsumer(jmsQueue);
			TextMessage rmsg = (TextMessage) jmsConsumerQCFBindings
					.receive(30000);

			jmsContextQCFBindings.commit();

			jmsConsumerQCFBindings.close();
			jmsContextQCFBindings.close();

			System.out.println(" number of msgs before and after commit "
					+ numMsgs + " & " + numMsgs1);

		} catch (Exception e) {

			e.printStackTrace();
			exceptionFlag = true;
		}
		if (exceptionFlag == true)
			throw new WrongException("testCommitLocalTransactionWithDD failed");

	}

	public void testCommitLocalTransactionWithDD_Tcp(
			HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		exceptionFlag = false;
		try {

			JMSContext jmsContextQCFTCP = jmsQCFTCP
					.createContext(Session.SESSION_TRANSACTED);

			emptyQueue(jmsQCFTCP, jmsQueue);
			Message message = jmsContextQCFTCP
					.createTextMessage("testCommitLocalTransactionWithDD_Tcp");
			JMSProducer jmsProducerQCFTCP = jmsContextQCFTCP.createProducer();
			jmsProducerQCFTCP.setDeliveryDelay(1000).send(jmsQueue, message);

			QueueBrowser qb = jmsContextQCFTCP.createBrowser(jmsQueue);
			Enumeration e = qb.getEnumeration();
			int numMsgs = 0;
			while (e.hasMoreElements()) {
				TextMessage message1 = (TextMessage) e.nextElement();
				numMsgs++;
			}

			jmsContextQCFTCP.commit();

			QueueBrowser qb1 = jmsContextQCFTCP.createBrowser(jmsQueue);
			Enumeration e1 = qb1.getEnumeration();
			int numMsgs1 = 0;
			// count * number of messages
			while (e1.hasMoreElements()) {
				TextMessage message1 = (TextMessage) e1.nextElement();
				numMsgs1++;
			}

			JMSConsumer jmsConsumerQCFTCP = jmsContextQCFTCP
					.createConsumer(jmsQueue);
			jmsConsumerQCFTCP.receive(30000);

			jmsContextQCFTCP.commit();

			jmsConsumerQCFTCP.close();
			jmsContextQCFTCP.close();
			System.out.println(" number of msgs before and after commit "
					+ numMsgs + " & " + numMsgs1);

		} catch (Exception e) {

			e.printStackTrace();
			exceptionFlag = true;
		}

		if (exceptionFlag == true)
			throw new WrongException(
					"testCommitLocalTransactionWithDD_Tcp failed");

	}

	public void testCreateSharedDurableConsumer_create_B_SecOff(
			HttpServletRequest request, HttpServletResponse response)
			throws Throwable {

		JMSContext jmsContextSender = jmsTCFBindings.createContext();

		JMSContext jmsContextReceiver = jmsTCFBindings.createContext();

		JMSConsumer jmsConsumer = jmsContextReceiver
				.createSharedDurableConsumer(jmsTopic, "SUBID");

		JMSProducer jmsProducer = jmsContextSender.createProducer();

		TextMessage tmsg = jmsContextSender
				.createTextMessage("This is a test message");

		jmsProducer.setDeliveryDelay(1000).send(jmsTopic, tmsg);

		jmsContextSender.close();

	}

	public void testCreateSharedDurableConsumer_consume_B_SecOff(
			HttpServletRequest request, HttpServletResponse response)
			throws Throwable {

		exceptionFlag = false;

		JMSContext jmsContextReceiver = jmsTCFBindings.createContext();
		JMSConsumer jmsConsumer = jmsContextReceiver
				.createSharedDurableConsumer(jmsTopic, "SUBID");

		TextMessage tmsg = (TextMessage) jmsConsumer.receiveNoWait();

		if (!(tmsg != null))
			exceptionFlag = true;

		if (exceptionFlag)
			throw new WrongException(
					"testCreateSharedDurableExpiry_B_SecOff failed");

		jmsConsumer.close();

		jmsContextReceiver.unsubscribe("SUBID");

		jmsContextReceiver.close();

	}

	public void testCreateSharedNonDurableConsumer_create_B_SecOff(
			HttpServletRequest request, HttpServletResponse response)
			throws Throwable {

		JMSContext jmsContextSender = jmsTCFBindings.createContext();

		JMSContext jmsContextReceiver = jmsTCFBindings.createContext();

		JMSConsumer jmsConsumer = jmsContextReceiver.createSharedConsumer(
				jmsTopic, "SUBID");

		JMSProducer jmsProducer = jmsContextSender.createProducer();

		TextMessage tmsg = jmsContextSender
				.createTextMessage("This is a test message");

		jmsProducer.setDeliveryDelay(1000).send(jmsTopic, tmsg);

		jmsContextSender.close();

	}

	public void testCreateSharedNonDurableConsumer_consume_B_SecOff(
			HttpServletRequest request, HttpServletResponse response)
			throws Throwable {

		exceptionFlag = false;
		JMSContext jmsContextReceiver = jmsTCFBindings.createContext();
		JMSConsumer jmsConsumer = jmsContextReceiver.createSharedConsumer(
				jmsTopic, "SUBID");

		TextMessage tmsg = (TextMessage) jmsConsumer.receiveNoWait();

		if (!(tmsg == null))
			exceptionFlag = true;

		jmsConsumer.close();

		// jmsContextReceiver.unsubscribe("SUBID");

		jmsContextReceiver.close();

	}

	public void testCreateUnSharedDurableConsumer_create(
			HttpServletRequest request, HttpServletResponse response)
			throws Throwable {

		JMSContext jmsContextSender = jmsTCFBindings.createContext();

		JMSContext jmsContextReceiver = jmsTCFBindings.createContext();

		JMSConsumer jmsConsumer = jmsContextReceiver.createDurableConsumer(
				jmsTopic, "SUBID");

		JMSProducer jmsProducer = jmsContextSender.createProducer();

		TextMessage tmsg = jmsContextSender
				.createTextMessage("This is a test message");

		jmsProducer.setDeliveryDelay(1000).send(jmsTopic, tmsg);

		jmsConsumer.close();

		jmsContextReceiver.close();

		jmsContextSender.close();

	}

	public void testCreateUnSharedDurableConsumer_consume(
			HttpServletRequest request, HttpServletResponse response)
			throws Throwable {

		exceptionFlag = false;
		JMSContext jmsContextReceiver = jmsTCFBindings.createContext();
		JMSConsumer jmsConsumer = jmsContextReceiver.createDurableConsumer(
				jmsTopic, "SUBID");

		TextMessage tmsg = (TextMessage) jmsConsumer.receiveNoWait();

		if (!(tmsg != null))
			exceptionFlag = true;

		if (exceptionFlag == true)
			throw new WrongException(
					"testCreateUnSharedDurableConsumer_consume failed");

		jmsConsumer.close();

		jmsContextReceiver.unsubscribe("SUBID");

		jmsContextReceiver.close();

	}

	// CLASSIC
	public void testSetDeliveryDelayClassicApi(HttpServletRequest request,
			HttpServletResponse response) throws Throwable {

		exceptionFlag = false;
		QueueConnection con = jmsQCFBindings.createQueueConnection();
		con.start();
		emptyQueue(jmsQCFBindings, jmsQueue1);
		QueueSession sessionSender = con.createQueueSession(false,
				javax.jms.Session.AUTO_ACKNOWLEDGE);
		QueueReceiver rec = sessionSender.createReceiver(jmsQueue1);
		QueueSender send = sessionSender.createSender(jmsQueue1);
		send.setDeliveryDelay(deliveryDelay);
		TextMessage send_msg = sessionSender
				.createTextMessage("testSetDeliveryDelayClassicApi");

		sendAndCheckDeliveryTime(send, jmsQueue1, send_msg);

		TextMessage msg = (TextMessage) rec.receiveNoWait();

		if (msg == null) {
			System.out.println("Expected NPE seen when receiving before delay");
			exceptionFlag = true;
		}
		if (!exceptionFlag)
			throw new WrongException("testSetDeliveryDelayClassicApi failed");

		exceptionFlag = false;

		TextMessage rec_msg = (TextMessage) rec.receive(30000);

		send.close();
		if (con != null)
			con.close();
		if (!(rec_msg != null && (rec_msg.getText()
				.equals("testSetDeliveryDelayClassicApi")))) {
			System.out.println("Receiving after delay: " + rec_msg.getText());
			exceptionFlag = true;
		}

		if (exceptionFlag)
			throw new WrongException("testReceiveAfterDelayClassicApi failed");
	}

	public void testSetDeliveryDelayClassicApi_Tcp(HttpServletRequest request,
			HttpServletResponse response) throws Throwable {

		exceptionFlag = false;
		QueueConnection con = jmsQCFTCP.createQueueConnection();
		con.start();
		emptyQueue(jmsQCFTCP, jmsQueue1);
		QueueSession sessionSender = con.createQueueSession(false,
				javax.jms.Session.AUTO_ACKNOWLEDGE);
		QueueReceiver rec = sessionSender.createReceiver(jmsQueue1);
		QueueSender send = sessionSender.createSender(jmsQueue1);
		send.setDeliveryDelay(deliveryDelay);
		TextMessage send_msg = sessionSender
				.createTextMessage("testSetDeliveryDelayClassicApi_Tcp");
		sendAndCheckDeliveryTime(send, jmsQueue1, send_msg);
		TextMessage msg = (TextMessage) rec.receiveNoWait();

		if (msg == null) {
			System.out.println("Expected NPE seen when receiving before delay");
			exceptionFlag = true;
		}
		if (!exceptionFlag)
			throw new WrongException(
					"testSetDeliveryDelayClassicApi_Tcp failed");

		exceptionFlag = false;

		TextMessage rec_msg = (TextMessage) rec.receive(30000);

		send.close();
		if (con != null)
			con.close();
		if (!(rec_msg != null && (rec_msg.getText()
				.equals("testSetDeliveryDelayClassicApi_Tcp")))) {
			System.out.println("Receiving after delay: " + rec_msg.getText());
			exceptionFlag = true;
		}

		if (exceptionFlag)
			throw new WrongException(
					"testSetDeliveryDelayClassicApi_Tcp failed");

	}

	public void testSetDeliveryDelayTopicClassicApi(HttpServletRequest request,
			HttpServletResponse response) throws Throwable {

		TopicConnection con = jmsTCFBindings.createTopicConnection();

		con.start();
		TopicSession session = con.createTopicSession(false,
				javax.jms.Session.AUTO_ACKNOWLEDGE);

		TopicSubscriber sub = session.createSubscriber(jmsTopic);

		TopicPublisher publisher = session.createPublisher(jmsTopic);
		publisher.setDeliveryDelay(deliveryDelay);
		TextMessage send_msg = session
				.createTextMessage("testSetDeliveryDelayTopicClassicApi");

		sendAndCheckDeliveryTime(publisher, jmsTopic, send_msg);

		TextMessage msg = (TextMessage) sub.receiveNoWait();

		if (msg == null) {
			System.out.println("Expected NPE seen when receiving before delay");
			exceptionFlag = true;
		}

		if (!exceptionFlag)
			throw new WrongException(
					"testSetDeliveryDelayTopicClassicApi failed");

		exceptionFlag = false;

		TextMessage rec_msg = (TextMessage) sub.receive(30000);

		if (sub != null)
			sub.close();
		if (con != null)
			con.close();

		if (!(rec_msg != null && (rec_msg.getText()
				.equals("testSetDeliveryDelayTopicClassicApi")))) {
			System.out.println("Receiving after delay: " + rec_msg.getText());
			exceptionFlag = true;
		}

		if (exceptionFlag)
			throw new WrongException(
					"testReceiveAfterDelayTopicClassicApi failed");

	}

	public void testSetDeliveryDelayTopicClassicApi_Tcp(
			HttpServletRequest request, HttpServletResponse response)
			throws Throwable {

		TopicConnection con = jmsTCFTCP.createTopicConnection();

		con.start();
		TopicSession session = con.createTopicSession(false,
				javax.jms.Session.AUTO_ACKNOWLEDGE);

		TopicSubscriber sub = session.createSubscriber(jmsTopic);

		TopicPublisher publisher = session.createPublisher(jmsTopic);
		publisher.setDeliveryDelay(deliveryDelay);
		TextMessage send_msg = session
				.createTextMessage("testSetDeliveryDelayTopicClassicApi_Tcp");

		sendAndCheckDeliveryTime(publisher, jmsTopic, send_msg);

		TextMessage msg = (TextMessage) sub.receiveNoWait();

		if (msg == null) {
			System.out.println("Expected NPE seen when receiving before delay");
			exceptionFlag = true;
		}

		if (!exceptionFlag)
			throw new WrongException(
					"testSetDeliveryDelayTopicClassicApi failed");

		exceptionFlag = false;

		TextMessage rec_msg = (TextMessage) sub.receive(30000);

		if (sub != null)
			sub.close();
		if (con != null)
			con.close();

		if (!(rec_msg != null && (rec_msg.getText()
				.equals("testSetDeliveryDelayTopicClassicApi_Tcp")))) {
			System.out.println("Receiving after delay: " + rec_msg.getText());
			exceptionFlag = true;
		}

		if (exceptionFlag)
			throw new WrongException(
					"testReceiveAfterDelayTopicClassicApi failed");
	}

	public void testSetDeliveryDelayTopicDurSubClassicApi(
			HttpServletRequest request, HttpServletResponse response)
			throws Throwable {

		TopicConnection con = jmsTCFBindings.createTopicConnection();

		con.start();
		TopicSession session = con.createTopicSession(false,
				javax.jms.Session.AUTO_ACKNOWLEDGE);

		TopicSubscriber sub = session.createDurableSubscriber(jmsTopic,
				"dursub");

		TopicPublisher publisher = session.createPublisher(jmsTopic);
		publisher.setDeliveryDelay(deliveryDelay);
		TextMessage send_msg = session
				.createTextMessage("testSetDeliveryDelayTopicDurSubClassicApi");

		sendAndCheckDeliveryTime(publisher, jmsTopic, send_msg);

		TextMessage msg = (TextMessage) sub.receiveNoWait();
		if (msg == null) {
			System.out.println("Expected NPE seen when receiving before delay");
			exceptionFlag = true;
		}

		if (!exceptionFlag)
			throw new WrongException(
					"testSetDeliveryDelayTopicDurSubClassicApi failed");

		exceptionFlag = false;

		TextMessage rec_msg = (TextMessage) sub.receive(30000);

		if (!(rec_msg != null && (rec_msg.getText()
				.equals("testSetDeliveryDelayTopicDurSubClassicApi")))) {
			System.out.println("Receiving after delay: " + rec_msg.getText());
			exceptionFlag = true;
		}

		if (sub != null)
			sub.close();
		session.unsubscribe("dursub");
		if (con != null)
			con.close();

		if (exceptionFlag)
			throw new WrongException(
					"testSetDeliveryDelayTopicDurSubClassicApi failed");

	}

	public void testSetDeliveryDelayTopicDurSubClassicApi_Tcp(
			HttpServletRequest request, HttpServletResponse response)
			throws Throwable {

		TopicConnection con = jmsTCFTCP.createTopicConnection();

		con.start();
		TopicSession session = con.createTopicSession(false,
				javax.jms.Session.AUTO_ACKNOWLEDGE);

		TopicSubscriber sub = session.createDurableSubscriber(jmsTopic,
				"dursubtcp");

		TopicPublisher publisher = session.createPublisher(jmsTopic);
		publisher.setDeliveryDelay(deliveryDelay);
		TextMessage send_msg = session
				.createTextMessage("testSetDeliveryDelayTopicDurSubClassicApi_Tcp");

		sendAndCheckDeliveryTime(publisher, jmsTopic, send_msg);

		TextMessage msg = (TextMessage) sub.receiveNoWait();
		if (msg == null) {
			System.out.println("Expected NPE seen when receiving before delay");
			exceptionFlag = true;
		}

		if (!exceptionFlag)
			throw new WrongException(
					"testSetDeliveryDelayTopicDurSubClassicApi_Tcp failed");

		exceptionFlag = false;

		TextMessage rec_msg = (TextMessage) sub.receive(30000);

		if (!(rec_msg != null && (rec_msg.getText()
				.equals("testSetDeliveryDelayTopicDurSubClassicApi_Tcp")))) {
			System.out.println("Receiving after delay: " + rec_msg.getText());
			exceptionFlag = true;
		}

		if (sub != null)
			sub.close();
		session.unsubscribe("dursubtcp");
		if (con != null)
			con.close();

		if (exceptionFlag)
			throw new WrongException(
					"testSetDeliveryDelayTopicDurSubClassicApi_Tcp failed");
	}

	public void testDeliveryDelayForDifferentDelaysClassicApi(
			HttpServletRequest request, HttpServletResponse response)
			throws Throwable {

		QueueConnection con = jmsQCFBindings.createQueueConnection();
		con.start();
		Queue queue = (Queue) new InitialContext()
				.lookup("java:comp/env/jndi_INPUT_Q");
		emptyQueue(jmsQCFBindings, queue);
		QueueSession sessionSender = con.createQueueSession(false,
				javax.jms.Session.AUTO_ACKNOWLEDGE);

		QueueSender send = sessionSender.createSender(queue);
		send.setDeliveryDelay(5000);
		send.send(sessionSender
				.createTextMessage("QueueBindingsMessage1-ClassicApi"));
		send.setDeliveryDelay(1000);
		send.send(sessionSender
				.createTextMessage("QueueBindingsMessage2-ClassicApi"));

		Thread.sleep(8000);
		send.close();
		if (con != null)
			con.close();
	}

	public void testDeliveryDelayForDifferentDelaysClassicApi_Tcp(
			HttpServletRequest request, HttpServletResponse response)
			throws Throwable {

		QueueConnection con = jmsQCFTCP.createQueueConnection();
		con.start();
		Queue queue = (Queue) new InitialContext()
				.lookup("java:comp/env/jndi_INPUT_Q");
		QueueSession sessionSender = con.createQueueSession(false,
				javax.jms.Session.AUTO_ACKNOWLEDGE);
		emptyQueue(jmsQCFTCP, queue);
		QueueSender send = sessionSender.createSender(queue);
		send.setDeliveryDelay(5000);
		send.send(sessionSender
				.createTextMessage("QueueTCPMessage1-ClassicApi"));
		send.setDeliveryDelay(1000);
		send.send(sessionSender
				.createTextMessage("QueueTCPMessage2-ClassicApi"));

		Thread.sleep(8000);
		send.close();
		if (con != null)
			con.close();
	}

	public void testDeliveryDelayForDifferentDelaysTopicClassicApi(
			HttpServletRequest request, HttpServletResponse response)
			throws Throwable {

		TopicConnection con = jmsTCFBindings.createTopicConnection();
		Topic topic = (Topic) new InitialContext()
				.lookup("java:comp/env/eis/topic");
		con.start();
		TopicSession session = con.createTopicSession(false,
				javax.jms.Session.AUTO_ACKNOWLEDGE);

		// TopicSubscriber sub = session.createDurableSubscriber(topic, "sub2");

		TopicPublisher publisher = session.createPublisher(topic);
		publisher.setDeliveryDelay(5000);
		String msgText = "TopicBindingsMessage1-ClassicApi";
		publisher.publish(session.createTextMessage(msgText));

		publisher.setDeliveryDelay(1000);
		msgText = "TopicBindingsMessage2-ClassicApi";
		publisher.publish(session.createTextMessage(msgText));

		Thread.sleep(8000);

		if (con != null)
			con.close();
	}

	public void testDeliveryDelayForDifferentDelaysTopicClassicApi_Tcp(
			HttpServletRequest request, HttpServletResponse response)
			throws Throwable {

		TopicConnection con = jmsTCFTCP.createTopicConnection();
		Topic topic = (Topic) new InitialContext()
				.lookup("java:comp/env/eis/topic");
		con.start();
		TopicSession session = con.createTopicSession(false,
				javax.jms.Session.AUTO_ACKNOWLEDGE);

		// TopicSubscriber sub = session.createDurableSubscriber(topic, "sub2");

		TopicPublisher publisher = session.createPublisher(topic);
		publisher.setDeliveryDelay(5000);
		String msgText = "TopicTCPMessage1-ClassicApi";
		publisher.publish(session.createTextMessage(msgText));

		publisher.setDeliveryDelay(1000);
		msgText = "TopicTCPMessage2-ClassicApi";
		publisher.publish(session.createTextMessage(msgText));

		Thread.sleep(8000);

		if (con != null)
			con.close();
	}

	public void testDeliveryMultipleMsgsClassicApi(HttpServletRequest request,
			HttpServletResponse response) throws Throwable {

		boolean val1 = false;
		boolean val2 = false;
		exceptionFlag = false;
		QueueConnection con = jmsQCFBindings.createQueueConnection();
		con.start();

		QueueSession sessionSender = con.createQueueSession(false,
				javax.jms.Session.AUTO_ACKNOWLEDGE);
		emptyQueue(jmsQCFBindings, jmsQueue1);
		QueueReceiver rec = sessionSender.createReceiver(jmsQueue1);
		QueueSender send = sessionSender.createSender(jmsQueue1);

		send.setDeliveryDelay(deliveryDelay);

		TextMessage send_msg1 = sessionSender
				.createTextMessage("testDeliveryMultipleMsgsClassicApi1");

		TextMessage send_msg2 = sessionSender
				.createTextMessage("testDeliveryMultipleMsgsClassicApi2");

		sendAndCheckDeliveryTime(send, jmsQueue1, send_msg1);

		TextMessage msg1 = (TextMessage) rec.receiveNoWait();

		// QueueReceiver rec1 = sessionSender.createReceiver(queue1); //This
		// sets the delivery delay of all messages sent using that producer. and
		// in classic api we can create sender for a single queue

		sendAndCheckDeliveryTime(send, jmsQueue1, send_msg2);

		TextMessage msg2 = (TextMessage) rec.receiveNoWait();

		if (msg1 == null && msg2 == null) {
			System.out
					.println("No message received before the delay as expected");
			val1 = true;
		}

		msg1 = (TextMessage) rec.receive(30000);
		msg2 = (TextMessage) rec.receive(30000);

		if ((msg1 != null && msg1.getText().equals(
				"testDeliveryMultipleMsgsClassicApi1"))
				&& (msg2 != null && msg2.getText().equals(
						"testDeliveryMultipleMsgsClassicApi2"))) {
			System.out
					.println("Both messages have been received after delay as expected");
			val2 = true;
		}

		send.close();
		if (con != null)
			con.close();
		if (!(val1 && val2))
			exceptionFlag = true;

		if (exceptionFlag)
			throw new WrongException(
					"testDeliveryMultipleMsgsClassicApi failed");
	}

	public void testDeliveryMultipleMsgsClassicApi_Tcp(
			HttpServletRequest request, HttpServletResponse response)
			throws Throwable {

		boolean val1 = false;
		boolean val2 = false;
		exceptionFlag = false;
		QueueConnection con = jmsQCFTCP.createQueueConnection();
		con.start();

		QueueSession sessionSender = con.createQueueSession(false,
				javax.jms.Session.AUTO_ACKNOWLEDGE);
		emptyQueue(jmsQCFTCP, jmsQueue1);
		QueueReceiver rec = sessionSender.createReceiver(jmsQueue1);
		QueueSender send = sessionSender.createSender(jmsQueue1);

		send.setDeliveryDelay(deliveryDelay);

		TextMessage send_msg1 = sessionSender
				.createTextMessage("testDeliveryMultipleMsgsClassicApi_Tcp1");

		TextMessage send_msg2 = sessionSender
				.createTextMessage("testDeliveryMultipleMsgsClassicApi_Tcp2");

		sendAndCheckDeliveryTime(send, jmsQueue1, send_msg1);

		TextMessage msg1 = (TextMessage) rec.receiveNoWait();

		// QueueReceiver rec1 = sessionSender.createReceiver(queue1); //This
		// sets the delivery delay of all messages sent using that producer. and
		// in classic api we can create sender for a single queue

		sendAndCheckDeliveryTime(send, jmsQueue1, send_msg2);

		TextMessage msg2 = (TextMessage) rec.receiveNoWait();

		if (msg1 == null && msg2 == null) {
			System.out
					.println("No message received before the delay as expected");
			val1 = true;
		}

		msg1 = (TextMessage) rec.receive(30000);
		msg2 = (TextMessage) rec.receive(30000);

		if ((msg1 != null && msg1.getText().equals(
				"testDeliveryMultipleMsgsClassicApi_Tcp1"))
				&& (msg2 != null && msg2.getText().equals(
						"testDeliveryMultipleMsgsClassicApi_Tcp2"))) {
			System.out
					.println("Both messages have been received after delay as expected");
			val2 = true;
		}

		send.close();
		if (con != null)
			con.close();
		if (!(val1 && val2))
			exceptionFlag = true;

		if (exceptionFlag)
			throw new WrongException(
					"testDeliveryMultipleMsgsClassicApi_Tcp failed");
	}

	public void testDeliveryMultipleMsgsTopicClassicApi(
			HttpServletRequest request, HttpServletResponse response)
			throws Throwable {

		exceptionFlag = false;
		boolean val1 = false;
		boolean val2 = false;
		TopicConnection con = jmsTCFBindings.createTopicConnection();
		con.start();

		TopicSession sessionSender = con.createTopicSession(false,
				javax.jms.Session.AUTO_ACKNOWLEDGE);

		TopicSubscriber sub = sessionSender.createSubscriber(jmsTopic);
		TopicPublisher send = sessionSender.createPublisher(jmsTopic);
		send.setDeliveryDelay(deliveryDelay);

		TextMessage send_msg1 = sessionSender
				.createTextMessage("testDeliveryMultipleMsgsTopicClassicApi1");

		TextMessage send_msg2 = sessionSender
				.createTextMessage("testDeliveryMultipleMsgsTopicClassicApi2");

		sendAndCheckDeliveryTime(send, jmsTopic, send_msg1);

		TextMessage msg1 = (TextMessage) sub.receiveNoWait();

		// QueueReceiver rec1 = sessionSender.createReceiver(queue1); //This
		// sets the delivery delay of all messages sent using that producer. and
		// in classic api we can create sender for a single queue

		sendAndCheckDeliveryTime(send, jmsTopic, send_msg2);

		TextMessage msg2 = (TextMessage) sub.receiveNoWait();

		if (msg1 == null && msg2 == null) {
			System.out
					.println("No message received before the delay as expected");
			val1 = true;
		}

		msg1 = (TextMessage) sub.receive(30000);
		msg2 = (TextMessage) sub.receive(30000);

		if ((msg1 != null && msg1.getText().equals(
				"testDeliveryMultipleMsgsTopicClassicApi1"))
				&& (msg2 != null && msg2.getText().equals(
						"testDeliveryMultipleMsgsTopicClassicApi2"))) {
			System.out
					.println("Both messages have been received after delay as expected");
			val2 = true;
		}

		if (sub != null)
			sub.close();
		if (con != null)
			con.close();

		if (!(val1 && val2))
			exceptionFlag = true;

		if (exceptionFlag)
			throw new WrongException(
					"testDeliveryMultipleMsgsTopicClassicApi failed");
	}

	public void testDeliveryMultipleMsgsTopicClassicApi_Tcp(
			HttpServletRequest request, HttpServletResponse response)
			throws Throwable {

		exceptionFlag = false;
		boolean val1 = false;
		boolean val2 = false;
		TopicConnection con = jmsTCFTCP.createTopicConnection();
		con.start();

		TopicSession sessionSender = con.createTopicSession(false,
				javax.jms.Session.AUTO_ACKNOWLEDGE);

		TopicSubscriber sub = sessionSender.createSubscriber(jmsTopic);
		TopicPublisher send = sessionSender.createPublisher(jmsTopic);
		send.setDeliveryDelay(deliveryDelay);

		TextMessage send_msg1 = sessionSender
				.createTextMessage("testDeliveryMultipleMsgsTopicClassicApi_Tcp1");

		TextMessage send_msg2 = sessionSender
				.createTextMessage("testDeliveryMultipleMsgsTopicClassicApi_Tcp2");

		sendAndCheckDeliveryTime(send, jmsTopic, send_msg1);

		TextMessage msg1 = (TextMessage) sub.receiveNoWait();

		// QueueReceiver rec1 = sessionSender.createReceiver(queue1); //This
		// sets the delivery delay of all messages sent using that producer. and
		// in classic api we can create sender for a single queue

		sendAndCheckDeliveryTime(send, jmsTopic, send_msg2);

		TextMessage msg2 = (TextMessage) sub.receiveNoWait();
		if (msg1 == null && msg2 == null) {
			System.out
					.println("No message received before the delay as expected");
			val1 = true;
		}

		msg1 = (TextMessage) sub.receive(30000);
		msg2 = (TextMessage) sub.receive(30000);

		if ((msg1 != null && msg1.getText().equals(
				"testDeliveryMultipleMsgsTopicClassicApi_Tcp1"))
				&& (msg2 != null && msg2.getText().equals(
						"testDeliveryMultipleMsgsTopicClassicApi_Tcp2"))) {
			System.out
					.println("Both messages have been received after delay as expected");
			val2 = true;
		}

		if (sub != null)
			sub.close();
		if (con != null)
			con.close();

		if (!(val1 && val2))
			exceptionFlag = true;

		if (exceptionFlag)
			throw new WrongException(
					"testDeliveryMultipleMsgsTopicClassicApi_Tcp failed");
	}

	public void testDeliveryDelayZeroAndNegativeValuesClassicApi(
			HttpServletRequest request, HttpServletResponse response)
			throws Throwable {

		boolean val1 = false;
		boolean val2 = false;
		QueueConnection con = jmsQCFBindings.createQueueConnection();
		con.start();

		QueueSession sessionSender = con.createQueueSession(false,
				javax.jms.Session.AUTO_ACKNOWLEDGE);
		emptyQueue(jmsQCFBindings, jmsQueue1);
		QueueReceiver rec = sessionSender.createReceiver(jmsQueue1);
		QueueSender send = sessionSender.createSender(jmsQueue1);

		send.send(sessionSender.createTextMessage("Zero Delivery Delay"));

		TextMessage msg = (TextMessage) rec.receiveNoWait();
		if (msg != null) {
			System.out
					.println("Message was received immediately when delay is set to zero "
							+ msg.getText());
			val1 = true;
		}
		try {
			send.setDeliveryDelay(-10);
		} catch (JMSException e) {
			System.out
					.println("Expected JMSRuntimeException seen when delivery delay is set to negative value");
			val2 = true;
		}

		send.close();
		if (con != null)
			con.close();

		if (!(val1 && val2))
			throw new WrongException(
					"testDeliveryDelayZeroAndNegativeValuesClassicApi failed");

	}

	public void testDeliveryDelayZeroAndNegativeValuesClassicApi_Tcp(
			HttpServletRequest request, HttpServletResponse response)
			throws Throwable {

		boolean val1 = false;
		boolean val2 = false;
		QueueConnection con = jmsQCFTCP.createQueueConnection();
		con.start();

		QueueSession sessionSender = con.createQueueSession(false,
				javax.jms.Session.AUTO_ACKNOWLEDGE);
		emptyQueue(jmsQCFTCP, jmsQueue1);
		QueueReceiver rec = sessionSender.createReceiver(jmsQueue1);
		QueueSender send = sessionSender.createSender(jmsQueue1);

		send.send(sessionSender.createTextMessage("Zero Delivery Delay"));

		TextMessage msg = (TextMessage) rec.receiveNoWait();
		if (msg != null) {
			System.out
					.println("Message was received immediately when delay is set to zero "
							+ msg.getText());
			val1 = true;
		}
		try {
			send.setDeliveryDelay(-10);
		} catch (JMSException e) {
			System.out
					.println("Expected JMSRuntimeException seen when delivery delay is set to negative value");
			val2 = true;
		}

		send.close();
		if (con != null)
			con.close();

		if (!(val1 && val2))
			throw new WrongException(
					"testDeliveryDelayZeroAndNegativeValuesClassicApi failed");
	}

	public void testDeliveryDelayZeroAndNegativeValuesTopicClassicApi(
			HttpServletRequest request, HttpServletResponse response)
			throws Throwable {

		boolean val1 = false;
		boolean val2 = false;

		TopicConnection con = jmsTCFBindings.createTopicConnection();
		con.start();

		TopicSession sessionSender = con.createTopicSession(false,
				javax.jms.Session.AUTO_ACKNOWLEDGE);

		TopicSubscriber rec = sessionSender.createSubscriber(jmsTopic);
		TopicPublisher send = sessionSender.createPublisher(jmsTopic);

		send.publish(sessionSender.createTextMessage("Zero Delivery Delay"));

		TextMessage msg = (TextMessage) rec.receiveNoWait();
		if (msg != null) {
			System.out
					.println("Message was received immediately when delay is set to zero "
							+ msg.getText());
			val1 = true;
		}
		try {
			send.setDeliveryDelay(-10);
		} catch (JMSException e) {
			System.out
					.println("Expected JMSRuntimeException seen when delivery delay is set to negative value");
			val2 = true;
		}

		if (rec != null)
			rec.close();
		if (con != null)
			con.close();
		if (!(val1 && val2))
			throw new WrongException(
					"testDeliveryDelayZeroAndNegativeValuesClassicApi failed");
	}

	public void testDeliveryDelayZeroAndNegativeValuesTopicClassicApi_Tcp(
			HttpServletRequest request, HttpServletResponse response)
			throws Throwable {

		boolean val1 = false;
		boolean val2 = false;

		TopicConnection con = jmsTCFTCP.createTopicConnection();
		con.start();

		TopicSession sessionSender = con.createTopicSession(false,
				javax.jms.Session.AUTO_ACKNOWLEDGE);

		TopicSubscriber rec = sessionSender.createSubscriber(jmsTopic);
		TopicPublisher send = sessionSender.createPublisher(jmsTopic);

		send.publish(sessionSender.createTextMessage("Zero Delivery Delay"));

		TextMessage msg = (TextMessage) rec.receiveNoWait();
		if (msg != null) {
			System.out
					.println("Message was received immediately when delay is set to zero "
							+ msg.getText());
			val1 = true;
		}
		try {
			send.setDeliveryDelay(-10);
		} catch (JMSException e) {
			System.out
					.println("Expected JMSRuntimeException seen when delivery delay is set to negative value");
			val2 = true;
		}

		if (rec != null)
			rec.close();
		if (con != null)
			con.close();
		if (!(val1 && val2))
			throw new WrongException(
					"testDeliveryDelayZeroAndNegativeValuesClassicApi failed");
	}

	public void testSettingMultiplePropertiesClassicApi(
			HttpServletRequest request, HttpServletResponse response)
			throws Throwable {
		exceptionFlag = false;
		QueueConnection con = jmsQCFBindings.createQueueConnection();
		con.start();

		QueueSession sessionSender = con.createQueueSession(false,
				javax.jms.Session.AUTO_ACKNOWLEDGE);
		emptyQueue(jmsQCFBindings, jmsQueue1);
		QueueReceiver rec = sessionSender.createReceiver(jmsQueue1);
		QueueSender send = sessionSender.createSender(jmsQueue1);
		send.setDeliveryDelay(1000);
		send.setDisableMessageID(true);
		send.send(sessionSender
				.createTextMessage("testSettingMultiplePropertiesClassicApi"));

		TextMessage msg = (TextMessage) rec.receive(30000);
		System.out
				.println("testSettingMultiplePropertiesClassicApi: Message text is: "
						+ msg.getText()
						+ " and message id is: "
						+ msg.getJMSMessageID());
		if (!(msg != null
				&& msg.getText().equals(
						"testSettingMultiplePropertiesClassicApi") && msg
					.getJMSMessageID() == null)) {
			exceptionFlag = true;
		}

		send.close();
		if (con != null)
			con.close();

		if (exceptionFlag)
			throw new WrongException(
					"testSettingMultiplePropertiesClassicApi failed");

	}

	public void testSettingMultiplePropertiesClassicApi_Tcp(
			HttpServletRequest request, HttpServletResponse response)
			throws Throwable {

		exceptionFlag = false;
		QueueConnection con = jmsQCFTCP.createQueueConnection();
		con.start();

		QueueSession sessionSender = con.createQueueSession(false,
				javax.jms.Session.AUTO_ACKNOWLEDGE);
		emptyQueue(jmsQCFTCP, jmsQueue1);
		QueueReceiver rec = sessionSender.createReceiver(jmsQueue1);
		QueueSender send = sessionSender.createSender(jmsQueue1);
		send.setDeliveryDelay(1000);
		send.setDisableMessageID(true);
		send.send(sessionSender
				.createTextMessage("testSettingMultiplePropertiesClassicApi_Tcp"));

		TextMessage msg = (TextMessage) rec.receive(30000);
		if (!(msg != null
				&& msg.getText().equals(
						"testSettingMultiplePropertiesClassicApi_Tcp") && msg
					.getJMSMessageID() == null)) {
			exceptionFlag = true;
		}

		send.close();
		if (con != null)
			con.close();

		if (exceptionFlag)
			throw new WrongException(
					"testSettingMultiplePropertiesClassicApi failed");

	}

	public void testSettingMultiplePropertiesTopicClassicApi(
			HttpServletRequest request, HttpServletResponse response)
			throws Throwable {

		exceptionFlag = false;
		TopicConnection con = jmsTCFBindings.createTopicConnection();
		con.start();

		TopicSession sessionSender = con.createTopicSession(false,
				javax.jms.Session.AUTO_ACKNOWLEDGE);

		TopicSubscriber sub = sessionSender.createSubscriber(jmsTopic);
		TopicPublisher send = sessionSender.createPublisher(jmsTopic);
		send.setDeliveryDelay(1000);
		send.setDisableMessageID(true);

		send.publish(sessionSender
				.createTextMessage("testSettingMultiplePropertiesTopicClassicApi"));

		TextMessage msg = (TextMessage) sub.receive(30000);

		if (!(msg != null
				&& msg.getText().equals(
						"testSettingMultiplePropertiesTopicClassicApi") && msg
					.getJMSMessageID() == null)) {
			exceptionFlag = true;
		}

		if (sub != null)
			sub.close();
		if (con != null)
			con.close();

		if (exceptionFlag)
			throw new WrongException(
					"testSettingMultiplePropertiesTopicClassicApi failed");

	}

	public void testSettingMultiplePropertiesTopicClassicApi_Tcp(
			HttpServletRequest request, HttpServletResponse response)
			throws Throwable {

		exceptionFlag = false;
		TopicConnection con = jmsTCFTCP.createTopicConnection();
		con.start();

		TopicSession sessionSender = con.createTopicSession(false,
				javax.jms.Session.AUTO_ACKNOWLEDGE);

		TopicSubscriber sub = sessionSender.createSubscriber(jmsTopic);
		TopicPublisher send = sessionSender.createPublisher(jmsTopic);
		send.setDeliveryDelay(1000);
		send.setDisableMessageID(true);

		send.publish(sessionSender
				.createTextMessage("testSettingMultiplePropertiesTopicClassicApi_Tcp"));

		TextMessage msg = (TextMessage) sub.receive(30000);

		if (!(msg != null
				&& msg.getText().equals(
						"testSettingMultiplePropertiesTopicClassicApi_Tcp") && msg
					.getJMSMessageID() == null)) {
			exceptionFlag = true;
		}

		if (sub != null)
			sub.close();

		if (con != null)
			con.close();

		if (exceptionFlag)
			throw new WrongException(
					"testSettingMultiplePropertiesTopicClassicApi failed");

	}

	// testTransactedSend_B

	public void testTransactedSendClassicApi_B(HttpServletRequest request,
			HttpServletResponse response) throws Throwable {

		QueueConnection con = jmsQCFBindings.createQueueConnection();
		con.start();

		QueueSession sessionSender = con.createQueueSession(true,
				javax.jms.Session.AUTO_ACKNOWLEDGE);
		emptyQueue(jmsQCFBindings, jmsQueue1);
		QueueReceiver rec = sessionSender.createReceiver(jmsQueue1);
		QueueSender send = sessionSender.createSender(jmsQueue1);
		send.setDeliveryDelay(2000);
		send.send(sessionSender
				.createTextMessage("testTransactedSendClassicApi_B"));
		long time_after_send = System.currentTimeMillis() + 2000;
		Thread.sleep(1000);
		sessionSender.commit();
		long time_after_commit = System.currentTimeMillis() + 2000;

		TextMessage msg = (TextMessage) rec.receive(40000);
		sessionSender.commit();
		long rec_time = msg.getLongProperty("JMSDeliveryTime");
		if ((msg != null && msg.getText().equals(
				"testTransactedSendClassicApi_B"))) {
			if ((rec_time - time_after_send <= Math.abs(100))
					&& (rec_time - time_after_commit >= Math.abs(100))) {
				exceptionFlag = true;
			}
		} else {
			exceptionFlag = true;
		}

		System.out.println("After send: " + time_after_send);
		System.out.println(" After commit: " + time_after_commit);
		System.out.println(" Receive time: " + rec_time);

		send.close();
		if (con != null)
			con.close();

		if (exceptionFlag)
			throw new WrongException("testTransactedSendClassicApi_B failed");
	}

	public void testTransactedSendClassicApi_Tcp(HttpServletRequest request,
			HttpServletResponse response) throws Throwable {

		QueueConnection con = jmsQCFTCP.createQueueConnection();
		con.start();

		QueueSession sessionSender = con.createQueueSession(true,
				javax.jms.Session.AUTO_ACKNOWLEDGE);
		emptyQueue(jmsQCFTCP, jmsQueue1);
		QueueReceiver rec = sessionSender.createReceiver(jmsQueue1);
		QueueSender send = sessionSender.createSender(jmsQueue1);
		send.setDeliveryDelay(2000);
		send.send(sessionSender
				.createTextMessage("testTransactedSendClassicApi_Tcp"));
		long time_after_send = System.currentTimeMillis() + 2000;
		Thread.sleep(1000);
		sessionSender.commit();
		long time_after_commit = System.currentTimeMillis() + 2000;

		TextMessage msg = (TextMessage) rec.receive(40000);
		sessionSender.commit();
		long rec_time = msg.getLongProperty("JMSDeliveryTime");
		if ((msg != null && msg.getText().equals(
				"testTransactedSendClassicApi_Tcp"))) {
			if ((rec_time - time_after_send <= Math.abs(100))
					&& (rec_time - time_after_commit >= Math.abs(100))) {
				exceptionFlag = true;
			}
		} else {
			exceptionFlag = true;
		}

		System.out.println("After send: " + time_after_send);
		System.out.println(" After commit: " + time_after_commit);
		System.out.println(" Receive time: " + rec_time);

		send.close();
		if (con != null)
			con.close();

		if (exceptionFlag)
			throw new WrongException("testTransactedSendClassicApi_Tcp failed");
	}

	public void testTransactedSendTopicClassicApi_B(HttpServletRequest request,
			HttpServletResponse response) throws Throwable {

		exceptionFlag = false;
		TopicConnection con = jmsTCFBindings.createTopicConnection();
		con.start();

		TopicSession sessionSender = con.createTopicSession(true,
				javax.jms.Session.AUTO_ACKNOWLEDGE);

		TopicSubscriber rec = sessionSender.createSubscriber(jmsTopic);
		TopicPublisher send = sessionSender.createPublisher(jmsTopic);
		send.setDeliveryDelay(2000);
		send.publish(sessionSender
				.createTextMessage("testTransactedSendTopicClassicApi_B"));
		long time_after_send = System.currentTimeMillis() + 2000;
		Thread.sleep(1000);
		sessionSender.commit();
		long time_after_commit = System.currentTimeMillis() + 2000;

		TextMessage msg = (TextMessage) rec.receive(40000);
		sessionSender.commit();

		long rec_time = msg.getLongProperty("JMSDeliveryTime");
		if ((msg != null && msg.getText().equals(
				"testTransactedSendTopicClassicApi_B"))) {
			if ((rec_time - time_after_send <= Math.abs(100))
					&& (rec_time - time_after_commit >= Math.abs(100))) {
				exceptionFlag = true;
			}
		} else {
			exceptionFlag = true;
		}

		System.out.println("After send: " + time_after_send);
		System.out.println(" After commit: " + time_after_commit);
		System.out.println(" Receive time: " + rec_time);

		if (rec != null)
			rec.close();
		if (con != null)
			con.close();

		if (exceptionFlag)
			throw new WrongException(
					"testTransactedSendTopicClassicApi_B failed");
	}

	public void testTransactedSendTopicClassicApi_Tcp(
			HttpServletRequest request, HttpServletResponse response)
			throws Throwable {

		exceptionFlag = false;
		TopicConnection con = jmsTCFTCP.createTopicConnection();
		con.start();

		TopicSession sessionSender = con.createTopicSession(true,
				javax.jms.Session.AUTO_ACKNOWLEDGE);

		TopicSubscriber rec = sessionSender.createSubscriber(jmsTopic);
		TopicPublisher send = sessionSender.createPublisher(jmsTopic);
		send.setDeliveryDelay(2000);
		send.publish(sessionSender
				.createTextMessage("testTransactedSendTopicClassicApi_Tcp"));
		long time_after_send = System.currentTimeMillis() + 2000;
		Thread.sleep(1000);
		sessionSender.commit();
		long time_after_commit = System.currentTimeMillis() + 2000;

		TextMessage msg = (TextMessage) rec.receive(40000);
		sessionSender.commit();

		long rec_time = msg.getLongProperty("JMSDeliveryTime");
		if ((msg != null && msg.getText().equals(
				"testTransactedSendTopicClassicApi_Tcp"))) {
			if ((rec_time - time_after_send <= Math.abs(100))
					&& (rec_time - time_after_commit >= Math.abs(100))) {
				exceptionFlag = true;
			}
		} else {
			exceptionFlag = true;
		}

		System.out.println("After send: " + time_after_send);
		System.out.println(" After commit: " + time_after_commit);
		System.out.println(" Receive time: " + rec_time);

		if (rec != null)
			rec.close();
		if (con != null)
			con.close();

		if (exceptionFlag)
			throw new WrongException(
					"testTransactedSendTopicClassicApi_Tcp failed");
	}

	public void testTimingClassicApi_B(HttpServletRequest request,
			HttpServletResponse response) throws Throwable {

		exceptionFlag = false;
		QueueConnection con = jmsQCFBindings.createQueueConnection();
		con.start();
		emptyQueue(jmsQCFBindings, jmsQueue1);
		QueueSession sessionSender = con.createQueueSession(false,
				javax.jms.Session.AUTO_ACKNOWLEDGE);
		QueueReceiver rec = sessionSender.createReceiver(jmsQueue1);
		QueueSender send = sessionSender.createSender(jmsQueue1);
		send.setDeliveryDelay(1000);
		TextMessage send_msg = sessionSender
				.createTextMessage("testTimingClassicApi_B");
		send.send(send_msg);

		long jmsDeliveryTime = send_msg.getLongProperty("JMSDeliveryTime");
		long jmsTimestamp = send_msg.getLongProperty("JMSTimestamp");

		if (!(jmsDeliveryTime == jmsTimestamp + 1000)) {
			System.out
					.println("The JMSDeliveryTime header value is not equal to (JMSTimestamp + delivery delay)");
			exceptionFlag = true;
		}

		if (exceptionFlag)
			throw new WrongException(
					"testTimingClassicApi_B failed because JMSDeliveryTime header value is not equal to (JMSTimestamp + delivery delay)");

		exceptionFlag = false;
		TextMessage rec_msg = (TextMessage) rec.receive(31000);
		long receive_time = System.currentTimeMillis();

		System.out.println(" delivery_time : " + jmsDeliveryTime);
		System.out.println(" receive_time : " + receive_time);

		if (rec_msg.getText().equals("testTimingClassicApi_B")) {
			System.out.println("Correct msg received");
			if (!(jmsDeliveryTime <= receive_time)) {
				System.out
						.println("Check to verify that the time at which message was received was greater than (send time + delivery delay) FAILED");
				exceptionFlag = true;
			}
		} else {
			System.out.println("Incorrect message received");
			exceptionFlag = true;
		}
		send.close();
		if (con != null)
			con.close();

		if (exceptionFlag)
			throw new WrongException("testTimingClassicApi_B failed");

	}

	public void testTimingClassicApi_Tcp(HttpServletRequest request,
			HttpServletResponse response) throws Throwable {

		exceptionFlag = false;
		QueueConnection con = jmsQCFTCP.createQueueConnection();
		con.start();
		emptyQueue(jmsQCFTCP, jmsQueue1);
		QueueSession sessionSender = con.createQueueSession(false,
				javax.jms.Session.AUTO_ACKNOWLEDGE);
		QueueReceiver rec = sessionSender.createReceiver(jmsQueue1);
		QueueSender send = sessionSender.createSender(jmsQueue1);
		send.setDeliveryDelay(1000);
		TextMessage send_msg = sessionSender
				.createTextMessage("testTimingClassicApi_Tcp");
		send.send(send_msg);

		long jmsDeliveryTime = send_msg.getLongProperty("JMSDeliveryTime");
		long jmsTimestamp = send_msg.getLongProperty("JMSTimestamp");

		if (!(jmsDeliveryTime == jmsTimestamp + 1000)) {
			System.out
					.println("The JMSDeliveryTime header value is not equal to (JMSTimestamp + delivery delay)");
			exceptionFlag = true;
		}

		if (exceptionFlag)
			throw new WrongException(
					"testTimingClassicApi_Tcp failed because JMSDeliveryTime header value is not equal to (JMSTimestamp + delivery delay)");

		exceptionFlag = false;
		TextMessage rec_msg = (TextMessage) rec.receive(31000);
		long receive_time = System.currentTimeMillis();

		System.out.println(" delivery_time : " + jmsDeliveryTime);
		System.out.println(" receive_time : " + receive_time);

		if (rec_msg.getText().equals("testTimingClassicApi_Tcp")) {
			System.out.println("Correct msg received");
			if (!(jmsDeliveryTime <= receive_time)) {
				System.out
						.println("Check to verify that the time at which message was received was greater than (send time + delivery delay) FAILED");
				exceptionFlag = true;
			}
		} else {
			System.out.println("Incorrect message received");
			exceptionFlag = true;
		}
		send.close();
		if (con != null)
			con.close();

		if (exceptionFlag)
			throw new WrongException("testTimingClassicApi_Tcp failed");
	}

	public void testTimingTopicClassicApi_B(HttpServletRequest request,
			HttpServletResponse response) throws Throwable {

		TopicConnection con = jmsTCFBindings.createTopicConnection();
		con.start();

		TopicSession sessionSender = con.createTopicSession(false,
				javax.jms.Session.AUTO_ACKNOWLEDGE);

		TopicSubscriber rec = sessionSender.createSubscriber(jmsTopic);
		TopicPublisher send = sessionSender.createPublisher(jmsTopic);
		send.setDeliveryDelay(1000);
		TextMessage send_msg = sessionSender
				.createTextMessage("testTimingTopicClassicApi_B");

		send.send(send_msg);

		long jmsDeliveryTime = send_msg.getLongProperty("JMSDeliveryTime");
		long jmsTimestamp = send_msg.getLongProperty("JMSTimestamp");

		if (!(jmsDeliveryTime == jmsTimestamp + 1000)) {
			System.out
					.println("The JMSDeliveryTime header value is not equal to (JMSTimestamp + delivery delay)");
			exceptionFlag = true;
		}

		if (exceptionFlag)
			throw new WrongException(
					"testTimingTopicClassicApi_B failed because JMSDeliveryTime header value is not equal to (JMSTimestamp + delivery delay)");

		exceptionFlag = false;
		TextMessage rec_msg = (TextMessage) rec.receive(31000);
		long receive_time = System.currentTimeMillis();

		System.out.println(" delivery_time : " + jmsDeliveryTime);
		System.out.println(" receive_time : " + receive_time);

		if (rec_msg.getText().equals("testTimingTopicClassicApi_B")) {
			System.out.println("Correct msg received");
			if (!(jmsDeliveryTime <= receive_time)) {
				System.out
						.println("Check to verify that the time at which message was received was greater than (send time + delivery delay) FAILED");
				exceptionFlag = true;
			}
		} else {
			System.out.println("Incorrect message received");
			exceptionFlag = true;
		}

		if (rec != null)
			rec.close();
		if (con != null)
			con.close();

		if (exceptionFlag)
			throw new WrongException("testTimingTopicClassicApi_B failed");
	}

	public void testTimingTopicClassicApi_Tcp(HttpServletRequest request,
			HttpServletResponse response) throws Throwable {

		TopicConnection con = jmsTCFTCP.createTopicConnection();
		con.start();

		TopicSession sessionSender = con.createTopicSession(false,
				javax.jms.Session.AUTO_ACKNOWLEDGE);

		TopicSubscriber rec = sessionSender.createSubscriber(jmsTopic);
		TopicPublisher send = sessionSender.createPublisher(jmsTopic);
		send.setDeliveryDelay(1000);
		TextMessage send_msg = sessionSender
				.createTextMessage("testTimingTopicClassicApi_Tcp");

		send.send(send_msg);

		long jmsDeliveryTime = send_msg.getLongProperty("JMSDeliveryTime");
		long jmsTimestamp = send_msg.getLongProperty("JMSTimestamp");

		if (!(jmsDeliveryTime == jmsTimestamp + 1000)) {
			System.out
					.println("The JMSDeliveryTime header value is not equal to (JMSTimestamp + delivery delay)");
			exceptionFlag = true;
		}

		if (exceptionFlag)
			throw new WrongException(
					"testTimingTopicClassicApi_Tcp failed because JMSDeliveryTime header value is not equal to (JMSTimestamp + delivery delay)");

		exceptionFlag = false;
		TextMessage rec_msg = (TextMessage) rec.receive(31000);
		long receive_time = System.currentTimeMillis();

		System.out.println(" delivery_time : " + jmsDeliveryTime);
		System.out.println(" receive_time : " + receive_time);

		if (rec_msg.getText().equals("testTimingTopicClassicApi_Tcp")) {
			System.out.println("Correct msg received");
			if (!(jmsDeliveryTime <= receive_time)) {
				System.out
						.println("Check to verify that the time at which message was received was greater than (send time + delivery delay) FAILED");
				exceptionFlag = true;
			}
		} else {
			System.out.println("Incorrect message received");
			exceptionFlag = true;
		}

		if (rec != null)
			rec.close();
		if (con != null)
			con.close();

		if (exceptionFlag)
			throw new WrongException("testTimingTopicClassicApi_Tcp failed");
	}

	public void testGetDeliveryDelayClassicApi(HttpServletRequest request,
			HttpServletResponse response) throws Throwable {

		boolean val1 = false;
		boolean val2 = false;
		QueueConnection con = jmsQCFBindings.createQueueConnection();
		con.start();

		QueueSession sessionSender = con.createQueueSession(false,
				javax.jms.Session.AUTO_ACKNOWLEDGE);
		emptyQueue(jmsQCFBindings, jmsQueue1);
		QueueSender send = sessionSender.createSender(jmsQueue1);

		long val = send.getDeliveryDelay();

		if (val == 0) {
			System.out
					.println("testGetDeliveryDelay: getDeliveryDelay returns zero when not set as expected");
			val1 = true;
		}
		send.setDeliveryDelay(1000);
		val = send.getDeliveryDelay();

		if (val == 1000) {
			System.out
					.println("testGetDeliveryDelay: getDeliveryDelay returns same value as the set value as expected");
			val2 = true;
		}
		if (!(val1 && val2))
			throw new WrongException("testGetDeliveryDelayClassicApi failed");

		sessionSender.close();
		if (con != null)
			con.close();

	}

	public void testGetDeliveryDelayClassicApi_Tcp(HttpServletRequest request,
			HttpServletResponse response) throws Throwable {

		boolean val1 = false;
		boolean val2 = false;
		QueueConnection con = jmsQCFTCP.createQueueConnection();
		con.start();

		QueueSession sessionSender = con.createQueueSession(false,
				javax.jms.Session.AUTO_ACKNOWLEDGE);
		emptyQueue(jmsQCFTCP, jmsQueue1);
		QueueSender send = sessionSender.createSender(jmsQueue1);

		long val = send.getDeliveryDelay();

		if (val == 0) {
			System.out
					.println("testGetDeliveryDelayClassicApi_Tcp: getDeliveryDelay returns zero when not set as expected");
			val1 = true;
		}

		send.setDeliveryDelay(1000);
		val = send.getDeliveryDelay();

		if (val == 1000) {
			System.out
					.println("testGetDeliveryDelayClassicApi_Tcp: getDeliveryDelay returns same value as the set value as expected");
			val2 = true;
		}
		if (!(val1 && val2))
			throw new WrongException(
					"testGetDeliveryDelayClassicApi_Tcp failed");

		sessionSender.close();
		if (con != null)
			con.close();

	}

	public void testGetDeliveryDelayClassicApiTopic(HttpServletRequest request,
			HttpServletResponse response) throws Throwable {

		boolean val1 = false;
		boolean val2 = false;
		TopicConnection con = jmsTCFBindings.createTopicConnection();
		con.start();

		TopicSession sessionSender = con.createTopicSession(false,
				javax.jms.Session.AUTO_ACKNOWLEDGE);

		TopicSubscriber rec = sessionSender.createSubscriber(jmsTopic);
		TopicPublisher send = sessionSender.createPublisher(jmsTopic);

		long val = send.getDeliveryDelay();

		if (val == 0) {
			System.out
					.println("testGetDeliveryDelayClassicApiTopic: getDeliveryDelay returns zero when not set as expected");
			val1 = true;
		}
		send.setDeliveryDelay(1000);

		val = send.getDeliveryDelay();

		if (val == 1000) {
			System.out
					.println("testGetDeliveryDelayClassicApiTopic: getDeliveryDelay returns same value as the set value as expected");
			val2 = true;
		}

		if (!(val1 && val2))
			throw new WrongException(
					"testGetDeliveryDelayClassicApiTopic failed");

		if (rec != null)
			rec.close();
		if (con != null)
			con.close();

	}

	public void testGetDeliveryDelayClassicApiTopic_Tcp(
			HttpServletRequest request, HttpServletResponse response)
			throws Throwable {

		boolean val1 = false;
		boolean val2 = false;
		TopicConnection con = jmsTCFTCP.createTopicConnection();
		con.start();

		TopicSession sessionSender = con.createTopicSession(false,
				javax.jms.Session.AUTO_ACKNOWLEDGE);

		TopicSubscriber rec = sessionSender.createSubscriber(jmsTopic);
		TopicPublisher send = sessionSender.createPublisher(jmsTopic);

		long val = send.getDeliveryDelay();

		if (val == 0) {
			System.out
					.println("testGetDeliveryDelayClassicApiTopic_Tcp: getDeliveryDelay returns zero when not set as expected");
			val1 = true;
		}

		send.setDeliveryDelay(1000);

		val = send.getDeliveryDelay();

		if (val == 1000) {
			System.out
					.println("testGetDeliveryDelayClassicApiTopic_Tcp: getDeliveryDelay returns same value as the set value as expected");
			val2 = true;
		}

		if (!(val1 && val2))
			throw new WrongException(
					"testGetDeliveryDelayClassicApiTopic_Tcp failed");
		if (rec != null)
			rec.close();
		if (con != null)
			con.close();

	}

	public void testPersistentMessageClassicApi(HttpServletRequest request,
			HttpServletResponse response) throws Throwable {

		exceptionFlag = false;
		QueueConnection con = jmsQCFBindings.createQueueConnection();
		con.start();

		QueueSession sessionSender = con.createQueueSession(false,
				javax.jms.Session.AUTO_ACKNOWLEDGE);
		QueueSender producer1 = sessionSender.createSender(jmsQueue);
		QueueSender producer2 = sessionSender.createSender(jmsQueue1);
		emptyQueue(jmsQCFBindings, jmsQueue);
		emptyQueue(jmsQCFBindings, jmsQueue1);
		producer1.setDeliveryMode(DeliveryMode.PERSISTENT);
		producer1.setDeliveryDelay(1000);
		TextMessage msg1 = sessionSender
				.createTextMessage("testPersistentMessage_PersistentMsgClassicApi");
		producer1.send(msg1);

		producer2.setDeliveryDelay(1000);
		producer2.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
		TextMessage msg2 = sessionSender
				.createTextMessage("testPersistentMessage_NonPersistentMsgClassicApi");
		producer2.send(msg2);

		sessionSender.close();
		con.close();
	}

	public void testPersistentMessageReceiveClassicApi(
			HttpServletRequest request, HttpServletResponse response)
			throws Throwable {

		exceptionFlag = false;
		QueueConnection con = jmsQCFBindings.createQueueConnection();
		con.start();

		QueueSession sessionSender = con.createQueueSession(false,
				javax.jms.Session.AUTO_ACKNOWLEDGE);
		QueueReceiver consumer1 = sessionSender.createReceiver(jmsQueue);
		QueueReceiver consumer2 = sessionSender.createReceiver(jmsQueue);
		QueueSender producer = sessionSender.createSender(jmsQueue1);

		TextMessage msg1 = (TextMessage) consumer1.receive(30000);
		System.out.println("Received message is " + msg1);

		TextMessage msg2 = (TextMessage) consumer2.receive(30000);
		System.out.println("Received message is " + msg2);
		if (!(msg1 != null
				&& msg1.getText().equals(
						"testPersistentMessage_PersistentMsgClassicApi") && msg2 == null))

			exceptionFlag = true;

		if (exceptionFlag)
			throw new WrongException(
					"testPersistentMessageReceiveClassicApi failed");

		sessionSender.close();
		if (con != null)
			con.close();
	}

	public void testPersistentMessageClassicApi_Tcp(HttpServletRequest request,
			HttpServletResponse response) throws Throwable {

		exceptionFlag = false;
		QueueConnection con = jmsQCFTCP.createQueueConnection();
		con.start();

		QueueSession sessionSender = con.createQueueSession(false,
				javax.jms.Session.AUTO_ACKNOWLEDGE);
		QueueSender producer1 = sessionSender.createSender(jmsQueue);
		QueueSender producer2 = sessionSender.createSender(jmsQueue1);
		emptyQueue(jmsQCFBindings, jmsQueue);
		emptyQueue(jmsQCFBindings, jmsQueue1);
		producer1.setDeliveryMode(DeliveryMode.PERSISTENT);
		producer1.setDeliveryDelay(1000);
		TextMessage msg1 = sessionSender
				.createTextMessage("testPersistentMessage_PersistentMsgClassicApi");
		producer1.send(msg1);

		producer2.setDeliveryDelay(1000);
		producer2.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
		TextMessage msg2 = sessionSender
				.createTextMessage("testPersistentMessage_NonPersistentMsgClassicApi");
		producer2.send(msg2);
		sessionSender.close();
		con.close();
	}

	public void testPersistentMessageReceiveClassicApi_Tcp(
			HttpServletRequest request, HttpServletResponse response)
			throws Throwable {

		exceptionFlag = false;
		QueueConnection con = jmsQCFTCP.createQueueConnection();
		con.start();

		QueueSession sessionSender = con.createQueueSession(false,
				javax.jms.Session.AUTO_ACKNOWLEDGE);
		MessageConsumer consumer1 = sessionSender.createConsumer(jmsQueue);
		MessageConsumer consumer2 = sessionSender.createConsumer(jmsQueue);
		QueueSender producer = sessionSender.createSender(jmsQueue1);

		TextMessage msg1 = (TextMessage) consumer1.receive(30000);
		System.out.println("Received message is " + msg1);

		TextMessage msg2 = (TextMessage) consumer2.receive(30000);
		System.out.println("Received message is " + msg2);
		if (!(msg1 != null
				&& msg1.getText().equals(
						"testPersistentMessage_PersistentMsgClassicApi") && msg2 == null))

			exceptionFlag = true;

		sessionSender.close();
		if (con != null)
			con.close();

		if (exceptionFlag)
			throw new WrongException(
					"testPersistentMessageReceiveClassicApi failed");
	}

	public void testPersistentMessageTopicClassicApi(
			HttpServletRequest request, HttpServletResponse response)
			throws Throwable {

		exceptionFlag = false;
		TopicConnection con = jmsTCFBindings.createTopicConnection();
		con.start();

		TopicSession sessionSender = con.createTopicSession(false,
				javax.jms.Session.AUTO_ACKNOWLEDGE);

		TopicSubscriber consumer1 = sessionSender.createDurableSubscriber(
				jmsTopic, "durPersMsgCA1");
		TopicSubscriber consumer2 = sessionSender.createDurableSubscriber(
				jmsTopic1, "durPersMsgCA2");
		TopicPublisher producer1 = sessionSender.createPublisher(jmsTopic);
		TopicPublisher producer2 = sessionSender.createPublisher(jmsTopic1);

		producer1.setDeliveryMode(DeliveryMode.PERSISTENT);
		producer1.setDeliveryDelay(1000);
		TextMessage msg1 = sessionSender
				.createTextMessage("testPersistentMessage_PersistentMsgTopicClassicApi");
		producer1.send(msg1);

		producer2.setDeliveryDelay(1000);
		producer2.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
		TextMessage msg2 = sessionSender
				.createTextMessage("testPersistentMessage_NonPersistentMsgTopicClassicApi");
		producer2.send(msg2);

		con.close();

	}

	public void testPersistentMessageReceiveTopicClassicApi(
			HttpServletRequest request, HttpServletResponse response)
			throws Throwable {

		exceptionFlag = false;
		TopicConnection con = jmsTCFBindings.createTopicConnection();
		con.start();

		TopicSession sessionSender = con.createTopicSession(false,
				javax.jms.Session.AUTO_ACKNOWLEDGE);

		TopicSubscriber consumer1 = sessionSender.createDurableSubscriber(
				jmsTopic, "durPersMsgCA1");
		TopicSubscriber consumer2 = sessionSender.createDurableSubscriber(
				jmsTopic1, "durPersMsgCA2");
		TopicPublisher producer = sessionSender.createPublisher(jmsTopic);

		TextMessage msg1 = (TextMessage) consumer1.receive(30000);
		System.out.println("Received message is " + msg1);

		TextMessage msg2 = (TextMessage) consumer2.receive(30000);
		System.out.println("Received message is " + msg2);
		if (!(msg1 != null
				&& msg1.getText().equals(
						"testPersistentMessage_PersistentMsgTopicClassicApi") && msg2 == null))

			exceptionFlag = true;

		consumer1.close();
		consumer2.close();

		sessionSender.unsubscribe("durPersMsgCA1");
		sessionSender.unsubscribe("durPersMsgCA2");
		sessionSender.close();

		con.close();
		if (exceptionFlag)
			throw new WrongException(
					"testPersistentMessageReceiveTopicClassicApi failed");
	}

	public void testPersistentMessageTopicClassicApi_Tcp(
			HttpServletRequest request, HttpServletResponse response)
			throws Throwable {

		exceptionFlag = false;
		TopicConnection con = jmsTCFTCP.createTopicConnection();
		con.start();

		TopicSession sessionSender = con.createTopicSession(false,
				javax.jms.Session.AUTO_ACKNOWLEDGE);

		TopicSubscriber consumer1 = sessionSender.createDurableSubscriber(
				jmsTopic, "durPersMsgCATcp1");
		TopicSubscriber consumer2 = sessionSender.createDurableSubscriber(
				jmsTopic1, "durPersMsgCATcp2");
		TopicPublisher producer1 = sessionSender.createPublisher(jmsTopic);
		TopicPublisher producer2 = sessionSender.createPublisher(jmsTopic1);
		producer1.setDeliveryMode(DeliveryMode.PERSISTENT);
		producer1.setDeliveryDelay(1000);
		TextMessage msg1 = sessionSender
				.createTextMessage("testPersistentMessage_PersistentMsgTopicClassicApiTcp");
		producer1.send(msg1);

		producer2.setDeliveryDelay(1000);
		producer2.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
		TextMessage msg2 = sessionSender
				.createTextMessage("testPersistentMessage_NonPersistentMsgTopicClassicApiTcp");
		producer2.send(msg2);
		con.close();

	}

	public void testPersistentMessageReceiveTopicClassicApi_Tcp(
			HttpServletRequest request, HttpServletResponse response)
			throws Throwable {

		exceptionFlag = false;
		TopicConnection con = jmsTCFTCP.createTopicConnection();
		con.start();

		TopicSession sessionSender = con.createTopicSession(false,
				javax.jms.Session.AUTO_ACKNOWLEDGE);

		TopicSubscriber consumer1 = sessionSender.createDurableSubscriber(
				jmsTopic, "durPersMsgCATcp1");
		TopicSubscriber consumer2 = sessionSender.createDurableSubscriber(
				jmsTopic1, "durPersMsgCATcp2");
		TopicPublisher producer = sessionSender.createPublisher(jmsTopic);

		TextMessage msg1 = (TextMessage) consumer1.receive(30000);
		System.out.println("Received message is " + msg1);

		TextMessage msg2 = (TextMessage) consumer2.receive(30000);
		System.out.println("Received message is " + msg2);

		if (!(msg1 != null
				&& msg1.getText()
						.equals("testPersistentMessage_PersistentMsgTopicClassicApiTcp") && msg2 == null))

			exceptionFlag = true;

		consumer1.close();
		consumer2.close();

		sessionSender.unsubscribe("durPersMsgCATcp1");
		sessionSender.unsubscribe("durPersMsgCATcp2");
		sessionSender.close();
		con.close();
		if (exceptionFlag)
			throw new WrongException(
					"testPersistentMessageStoreReceiveTopicClassicApi_Tcp failed");
	}

	public void testJSAD_Send_Message_P2PTest(HttpServletRequest request,
			HttpServletResponse response) throws Throwable {

		QueueConnectionFactory cf1 = (QueueConnectionFactory) new InitialContext()
				.lookup("java:comp/env/jndi_JMS_BASE_QCF");

		System.out.println("Queue Connection factory" + cf1);
		Queue queue = (Queue) new InitialContext()
				.lookup("java:comp/env/jndi_INPUT_Q");
		emptyQueue(cf1, queue);
		QueueConnection con = cf1.createQueueConnection();
		con.start();

		QueueSession sessionSender = con.createQueueSession(false,
				javax.jms.Session.AUTO_ACKNOWLEDGE);

		QueueSender send = sessionSender.createSender(queue);

		String outbound = "Hello World from testJSAD_Send_Message_P2PTest";
		send.setDeliveryDelay(1000);
		send.send(sessionSender.createTextMessage(outbound));

		Queue queue1 = sessionSender.createQueue("QUEUE1");
		QueueReceiver rec = sessionSender.createReceiver(queue1);
		TextMessage receiveMsg = (TextMessage) rec.receive(30000);

		String inbound = "";

		inbound = receiveMsg.getText();

		if (!(outbound.equals(inbound)))
			throw new WrongException("testJSAD_Send_Message_P2PTest failed");
		else
			System.out.println("Matched Sent and received Msg");

		sessionSender.close();
		if (con != null)
			con.close();

	}

	/**
	 * Basic point-to-point test with a single send to a queue and a receive
	 * from the alias,
	 */

	public void testJSAD_Receive_Message_P2PTest(HttpServletRequest request,
			HttpServletResponse response) throws Throwable {

		QueueConnectionFactory cf1 = (QueueConnectionFactory) new InitialContext()
				.lookup("java:comp/env/jndi_JMS_BASE_QCF");

		QueueConnection con = cf1.createQueueConnection();
		con.start();

		QueueSession sessionSender = con.createQueueSession(false,
				javax.jms.Session.AUTO_ACKNOWLEDGE);
		Queue queue = sessionSender.createQueue("QUEUE1");
		emptyQueue(cf1, queue);
		QueueSender send = sessionSender.createSender(queue);

		String outbound = "Hello World from testJSAD_Receive_Message_P2PTest";
		send.setDeliveryDelay(1000);
		send.send(sessionSender.createTextMessage(outbound));

		Queue queue1 = sessionSender.createQueue("alias2Q1");

		QueueReceiver rec = sessionSender.createReceiver(queue1);

		TextMessage receiveMsg = (TextMessage) rec.receive(30000);

		String inbound = "";

		inbound = receiveMsg.getText();

		if (!(outbound.equals(inbound)))
			throw new WrongException("testJSAD_Receive_Message_P2PTest failed");
		else
			System.out.println("Matched Sent and received Msg");

		sessionSender.close();
		if (con != null)
			con.close();

	}

	public void testBasicTemporaryQueue(HttpServletRequest request,
			HttpServletResponse response) throws Throwable {

		exceptionFlag = false;

		QueueConnectionFactory cf1 = (QueueConnectionFactory) new InitialContext()
				.lookup("java:comp/env/jndi_JMS_BASE_QCF");

		QueueConnection con = cf1.createQueueConnection();
		con.start();

		QueueSession jmsSession = con.createQueueSession(false,
				javax.jms.Session.AUTO_ACKNOWLEDGE);

		TemporaryQueue tempQ = jmsSession.createTemporaryQueue();
		QueueSender queueSender = jmsSession.createSender(tempQ);
		QueueReceiver queueReceiver = jmsSession.createReceiver(tempQ);

		TextMessage msg1 = jmsSession
				.createTextMessage("testBasicTemporaryQueue");

		// assertTrue("created message is null", msg1 != null);
		queueSender.setDeliveryDelay(1000);
		queueSender.send(msg1);

		TextMessage recMessage = (TextMessage) queueReceiver.receive(30000);

		if (!(recMessage.getText().equals("testBasicTemporaryQueue")))
			exceptionFlag = true;
		queueSender.close();
		queueReceiver.close();
		tempQ.delete();

		if (exceptionFlag)
			throw new WrongException("testBasicTemporaryQueue failed");

	}

	public void testSendMessageToQueue(HttpServletRequest request,
			HttpServletResponse response) throws Throwable {

		QueueConnection con = null;

		QueueConnectionFactory cf1 = (QueueConnectionFactory) new InitialContext()
				.lookup("java:comp/env/jndi_JMS_BASE_QCF");
		Queue queue = (Queue) new InitialContext()
				.lookup("java:comp/env/jndi_INPUT_Q");

		con = cf1.createQueueConnection();
		con.start();
		emptyQueue(cf1, queue);
		QueueSession sessionSender = con.createQueueSession(false,
				javax.jms.Session.AUTO_ACKNOWLEDGE);

		QueueSender send = sessionSender.createSender(queue);

		TextMessage msg = sessionSender
				.createTextMessage("ExceptionDestinationMessage");
		send.setDeliveryDelay(1000);
		send.send(msg);
		System.out.println("Sent msg : " + msg.getText());
		Thread.sleep(3000);
		if (con != null)
			con.close();

	}

	public void testReadMsgFromExceptionQueue(HttpServletRequest request,
			HttpServletResponse response) throws Throwable {

		exceptionFlag = false;
		QueueConnectionFactory cf1 = (QueueConnectionFactory) new InitialContext()
				.lookup("java:comp/env/jndi_JMS_BASE_QCF");
		Queue queue = (Queue) new InitialContext()
				.lookup("java:comp/env/jndi_EXCEPTION_Q");

		QueueConnection con = cf1.createQueueConnection();
		con.start();

		QueueSession session = con.createQueueSession(false,
				javax.jms.Session.AUTO_ACKNOWLEDGE);

		QueueReceiver rec = session.createReceiver(queue);
		TextMessage msg = (TextMessage) rec.receive(30000);

		if (!(msg.getText().equals("ExceptionDestinationMessage")))
			exceptionFlag = true;
		if (con != null)
			con.close();

		if (exceptionFlag)
			throw new WrongException("testReadMsgFromExceptionQueue failed");

	}

	public void testBytesMessage(HttpServletRequest request,
			HttpServletResponse response) throws Throwable {

		boolean boolean1 = true;
		byte byte1 = 1;
		byte[] bytes1 = new byte[] { -3, -2, -1, 0, 1, 2, 3, 4, 5 };
		char char1 = '\u0001';
		double double1 = 1.0d;
		float float1 = 1.0f;
		int int1 = 1;
		long long1 = 1L;
		Integer integer1 = new Integer(1);
		short short1 = 1;
		String string1 = "one";

		PrintWriter out = response.getWriter();

		QueueConnectionFactory cf1 = (QueueConnectionFactory) new InitialContext()
				.lookup("java:comp/env/jndi_JMS_BASE_QCF");
		Queue queue = (Queue) new InitialContext()
				.lookup("java:comp/env/jndi_INPUT_Q");
		QueueConnection con = cf1.createQueueConnection();
		con.start();
		emptyQueue(cf1, jmsQueue);
		QueueSession jmsSession = con.createQueueSession(false,
				javax.jms.Session.AUTO_ACKNOWLEDGE);

		BytesMessage BM = jmsSession.createBytesMessage();

		out.println("setting values into Bytes message");

		try {
			BM.writeBoolean(boolean1);
			BM.writeByte(byte1);
			BM.writeChar(char1);
			BM.writeDouble(double1);
			BM.writeFloat(float1);
			BM.writeInt(int1);
			BM.writeLong(long1);
			BM.writeShort(short1);
			BM.writeUTF(string1);
			BM.writeBytes(bytes1);
			BM.writeObject(integer1);

		} catch (JMSException e) {
			throw new Exception("caught unexpected exception, "
					+ e.getMessage());
		}

		QueueSender send = jmsSession.createSender(jmsQueue);
		QueueReceiver queueReceiver = jmsSession.createReceiver(jmsQueue);
		send.setDeliveryDelay(1000);
		send.send(jmsQueue, BM);
		out.println("Sent Message is " + BM);

		BytesMessage recMessage = (BytesMessage) queueReceiver.receive(30000);

		out.println("message is recived" + recMessage);

		if ((recMessage.readBoolean()) != boolean1) {
			throw new WrongException("Boolean value is not same");
		}
		if ((recMessage.readByte()) != byte1) {
			throw new WrongException("Byte value is not same");
		}

		if ((recMessage.readChar()) != char1) {
			throw new WrongException("char value is not same");
		}

		if ((recMessage.readDouble()) != double1) {
			throw new WrongException("double value is not same");
		}

		if ((recMessage.readFloat()) != float1) {
			throw new WrongException("float value is not same");
		}

		if ((recMessage.readInt()) != int1) {
			throw new WrongException("int value is not same");
		}

		if ((recMessage.readLong()) != long1) {
			throw new WrongException("long value is not same");
		}

		if (recMessage.readShort() != short1) {
			throw new WrongException("short value is not same");
		}

		String s1 = recMessage.readUTF();

		if ((s1.compareTo(string1) < 0 || s1.compareTo(string1) > 0)) {
			throw new WrongException("UTF value is not same");
		}

		if ((recMessage.readBytes(bytes1) != 9)) {
			throw new WrongException("Bytes value is not same");
		}

		if (con != null)
			con.close();
	}

	public void testComms_Send_Message_P2PTest_Default(
			HttpServletRequest request, HttpServletResponse response)
			throws Throwable {

		exceptionFlag = false;
		QueueConnectionFactory cf1 = (QueueConnectionFactory) new InitialContext()
				.lookup("java:comp/env/jndi_JMS_BASE_QCF");

		Queue queue = (Queue) new InitialContext()
				.lookup("java:comp/env/jndi_INPUT_Q");

		QueueConnection con = cf1.createQueueConnection();
		con.start();
		emptyQueue(cf1, jmsQueue);
		QueueSession sessionSender = con.createQueueSession(false,
				javax.jms.Session.AUTO_ACKNOWLEDGE);
		QueueSender send = sessionSender.createSender(jmsQueue);
		QueueReceiver rec = sessionSender.createReceiver(jmsQueue);
		String outbound = "testComms_Send_Message_P2PTest_Default";
		send.setDeliveryDelay(1000);
		send.send(jmsQueue, sessionSender.createTextMessage(outbound));

		TextMessage receiveMsg = (TextMessage) rec.receive(30000);

		String inbound = "";

		inbound = receiveMsg.getText();

		if (!(outbound.equals(inbound)))
			exceptionFlag = true;
		else
			System.out.println("Matched Sent and received Msg");

		if (con != null)
			con.close();
		if (exceptionFlag)
			throw new WrongException(
					"testComms_Send_Message_P2PTest_Default failed");

	}

	public void testSendMessage(HttpServletRequest request,
			HttpServletResponse response) throws Throwable {

		exceptionFlag = false;
		JMSContext jmsContext = jmsQCFBindings.createContext();
		emptyQueue(jmsQCFBindings, jmsQueue);
		JMSProducer producer = jmsContext.createProducer();

		producer.setDeliveryDelay((deliveryDelay * 12));
		TextMessage send_msg = jmsContext.createTextMessage("testSendMessage");

		sendAndCheckDeliveryTime(producer, jmsQueue, send_msg);

		jmsContext.close();
	}

	public void testReceiveMessage(HttpServletRequest request,
			HttpServletResponse response) throws Throwable {

		exceptionFlag = false;
		JMSContext jmsContext = jmsQCFBindings.createContext();

		JMSConsumer consumer = jmsContext.createConsumer(jmsQueue);

		String recdMsg = ((TextMessage) consumer.receive(120000)).getText();

		if (!(recdMsg.equals("testSendMessage")))
			exceptionFlag = true;

		if (exceptionFlag)
			throw new WrongException(
					"testReceiveMessage failed: Expected 'testSendMessage' but received '"
							+ recdMsg + "'");
		jmsContext.close();
	}

	public void testSendMessage_TCP(HttpServletRequest request,
			HttpServletResponse response) throws Throwable {

		exceptionFlag = false;
		JMSContext jmsContext = jmsQCFTCP.createContext();
		emptyQueue(jmsQCFTCP, jmsQueue);
		JMSProducer producer = jmsContext.createProducer();

		producer.setDeliveryDelay((deliveryDelay * 12));
		TextMessage send_msg = jmsContext.createTextMessage("testSendMessage");

		sendAndCheckDeliveryTime(producer, jmsQueue, send_msg);

		jmsContext.close();
	}

	public void testReceiveMessage_TCP(HttpServletRequest request,
			HttpServletResponse response) throws Throwable {

		exceptionFlag = false;
		JMSContext jmsContext = jmsQCFTCP.createContext();

		JMSConsumer consumer = jmsContext.createConsumer(jmsQueue);

		String recdMsg = ((TextMessage) consumer.receive(120000)).getText();

		if (!(recdMsg.equals("testSendMessage")))
			exceptionFlag = true;

		if (exceptionFlag)
			throw new WrongException(
					"testReceiveMessage failed: Expected 'testSendMessage' but received '"
							+ recdMsg + "'");
		jmsContext.close();
	}

	private QueueConnectionFactory getQCF(String name) throws NamingException {

		QueueConnectionFactory qcf = (QueueConnectionFactory) new InitialContext()
				.lookup("java:comp/env/" + name);
		return qcf;

	}

	private Queue getQueue(String name) throws NamingException {

		Queue q1 = (Queue) new InitialContext().lookup("java:comp/env/" + name);
		return q1;

	}

	private TopicConnectionFactory getTCF(String name) throws NamingException {

		TopicConnectionFactory tcf = (TopicConnectionFactory) new InitialContext()
				.lookup("java:comp/env/" + name);
		return tcf;

	}

	private Topic getTopic(String name) throws NamingException {

		Topic t1 = (Topic) new InitialContext().lookup("java:comp/env/" + name);
		return t1;

	}

	public class WrongException extends Exception {
		String str;

		public WrongException(String str) {
			this.str = str;
			System.out.println(" <ERROR> " + str + " </ERROR>");
		}
	}

	public void emptyQueue(QueueConnectionFactory qcf, Queue q)
			throws Exception {

		JMSContext context = qcf.createContext();
		QueueBrowser qb = context.createBrowser(q);
		Enumeration e = qb.getEnumeration();
		JMSConsumer consumer = context.createConsumer(q);
		int numMsgs = 0;
		// count number of messages
		while (e.hasMoreElements()) {
			Message message = (Message) e.nextElement();
			numMsgs++;
		}

		for (int i = 0; i < numMsgs; i++) {
			Message msg = consumer.receive();
		}

		context.close();
	}

	public int getMessageCount(QueueBrowser qb) throws JMSException {

		Enumeration e = qb.getEnumeration();

		int numMsgs = 0;
		// count number of messages
		while (e.hasMoreElements()) {
			e.nextElement();
			numMsgs++;
		}

		return numMsgs;
	}

	/**
	 * @param producer
	 * @param jmsQueue3
	 * @param send_msg
	 * @throws JMSException
	 */
	private void sendAndCheckDeliveryTime(Object producer, Destination dest,
			TextMessage send_msg) throws JMSException {

		long before_send_time = System.currentTimeMillis();

		if (producer instanceof JMSProducer)
			((JMSProducer) producer).send(dest, send_msg);

		else if (producer instanceof MessageProducer)
			((MessageProducer) producer).send(send_msg);

		long after_send_time = System.currentTimeMillis();

		long timeTakenToSend = after_send_time - before_send_time;

		if (timeTakenToSend >= deliveryDelay) {
			System.out.println("WARNING : The time taken to send is : "
					+ timeTakenToSend + " which more than deliveryDelay : "
					+ deliveryDelay
					+ ". Please analyse the time taken by send operation");
		}
	}

}
