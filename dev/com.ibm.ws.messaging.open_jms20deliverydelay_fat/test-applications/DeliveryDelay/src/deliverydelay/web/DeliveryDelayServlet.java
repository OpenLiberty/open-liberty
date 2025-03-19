/* =============================================================================
 * Copyright (c) 2014, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 * =============================================================================
 */
package deliverydelay.web;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;

import javax.jms.BytesMessage;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
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
import javax.jms.StreamMessage;
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

@SuppressWarnings("serial")
public class DeliveryDelayServlet extends HttpServlet {

    QueueConnectionFactory jmsQCFBindings = null;
    QueueConnectionFactory jmsQCFTCP = null;
    Queue jmsQueue = null;
    Queue jmsQueue1 = null;
    Queue jmsQueue2 = null;

    TopicConnectionFactory jmsTCFBindings = null;
    TopicConnectionFactory jmsTCFTCP = null;
    Topic jmsTopic = null;
    Topic jmsTopic1 = null;
    Topic jmsTopic2 = null;

    /** @return the methodName of the caller. */
    private static final String methodName() { return new Exception().getStackTrace()[1].getMethodName(); }
    
    private final class TestException extends Exception {
        TestException(String message) {
            super(timeStamp() +" "+message);
        }
    }
    
    // The current time, formatted with millisecond resolution.
    private static final SimpleDateFormat timeStampFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS Z");
    private static final String timeStamp() { return timeStampFormat.format(new Date());}
    
    public QueueConnectionFactory getQCF(String name) {
        QueueConnectionFactory qcf;
        try {
            qcf = (QueueConnectionFactory) new InitialContext().lookup(name);
        } catch ( NamingException e ) {
            e.printStackTrace();
            qcf = null;
        }
        System.out.println("Queue connection factory '" + name + "' [ " + qcf + " ]");
        return qcf;
    }

    public static TopicConnectionFactory getTCF(String name) {
        TopicConnectionFactory tcf;
        try {
            tcf = (TopicConnectionFactory) new InitialContext().lookup(name);
        } catch ( NamingException e ) {
            e.printStackTrace();
            tcf = null;
        }
        System.out.println("Topic connection factory '" + name + "' [ " + tcf + " ]");
        return tcf;
    }

    public Queue getQueue(String name) {
        Queue queue;
        try {
            queue = (Queue) new InitialContext().lookup(name);
        } catch ( NamingException e ) {
            e.printStackTrace();
            queue = null;
        }
        System.out.println("Queue '" + name + "' [ " + queue + " ]");
        return queue;
    }

    public Topic getTopic(String name) {
        Topic topic;
        try {
            topic = (Topic) new InitialContext().lookup(name);
        } catch ( NamingException e ) {
            e.printStackTrace();
            topic = null;
        }
        System.out.println("Topic '" + name + "' [ " + topic + " ]");
        return topic;
    }

    public void emptyQueue(ConnectionFactory cf, Queue q) 
        throws TestException {
       
        long messagesReceived = 0; 
        try (JMSContext jmsContext = cf.createContext(JMSContext.SESSION_TRANSACTED)) {
            JMSConsumer jmsConsumer = jmsContext.createConsumer(q);
            while ( jmsConsumer.receiveNoWait() != null) {messagesReceived++;}
            jmsContext.commit();
        }
        if (messagesReceived != 0) 
            throw new TestException("Queue:"+q+ "contained "+messagesReceived+" messages");
    }

    public int getMessageCount(QueueBrowser qb) throws JMSException {
        int numMsgs = 0;

        Enumeration e = qb.getEnumeration();
        while ( e.hasMoreElements() ) {
            e.nextElement();
            numMsgs++;
        }

        return numMsgs;
    }

    private static final long defaultTestDeliveryDelay = 10000;
    
    
    /**
     * Utility method that sends a message to a producer and checks that the time taken to send the message is less than the test default deliveryDelay time.
     * If the message took longer to send than the time it is to be delayed then the method logs a warning message as this might interfere with later test results.
     * 
     * @param producer
     * @param dest
     * @param send_msg
     * @return the value of System.currentTimeMillis() from when the send call is called. This can be used as a basis for later checks for delay times.
     * @throws JMSException
     */
    private long sendAndCheckDeliveryTime(
        Object producer,
        Destination dest,
        TextMessage send_msg) throws JMSException {

        long beforeSend = System.currentTimeMillis();

        if ( producer  instanceof JMSProducer ) {
            ((JMSProducer) producer).send(dest, send_msg);
        } else if ( producer instanceof MessageProducer ) {
            ((MessageProducer) producer).send(send_msg);
        } else {
            throw new IllegalArgumentException("Unknown producer type [ " + producer + " ] [ " + producer.getClass() + " ]");
        }

        long afterSend = System.currentTimeMillis();

        long sendDuration = afterSend - beforeSend;

        if ( sendDuration >= defaultTestDeliveryDelay ) {
            System.out.println(
                "WARNING : The time taken to send the message was : " + sendDuration +
                ", which more than delivery delay " + defaultTestDeliveryDelay + "."+
                " This is too slow to meaningfully test the delivery delay. Please analyse the send time.");
        }
        
        return beforeSend;
    }

    //

    @Override
    public void init() throws ServletException {
        super.init();

        jmsQCFBindings = getQCF("java:comp/env/jndi_JMS_BASE_QCF");
        jmsQCFTCP = getQCF("java:comp/env/jndi_JMS_BASE_QCF1");

        jmsQueue = getQueue("java:comp/env/eis/queue1");
        jmsQueue1 = getQueue("java:comp/env/eis/queue2");
        jmsQueue2 = getQueue("java:comp/env/eis/queue2");

        jmsTCFBindings = getTCF("java:comp/env/eis/tcf");
        jmsTCFTCP = getTCF("java:comp/env/eis/tcf1");

        jmsTopic = getTopic("java:comp/env/eis/topic1");
        jmsTopic1 = getTopic("java:comp/env/eis/topic2");
        jmsTopic2 = getTopic("java:comp/env/eis/topic3");

        if ( jmsQCFBindings == null ) {
            throw new ServletException("Null queue connection factory 'java:comp/env/jndi_JMS_BASE_QCF'");
        }
        if ( jmsQCFTCP == null ) {
            throw new ServletException("Null queue connection factory 'java:comp/env/jndi_JMS_BASE_QCF1'");
        }

        if ( jmsQueue == null ) {
            throw new ServletException("Null queue 'java:comp/env/eis/queue1'");
        }
        if ( jmsQueue1 == null ) {
            throw new ServletException("Null queue 'java:comp/env/eis/queue2'");
        }
        if ( jmsQueue2 == null ) {
            throw new ServletException("Null queue 'java:comp/env/eis/queue2'");
        }

        if ( jmsTCFBindings == null ) {
            throw new ServletException("Null topic connection factory 'java:comp/env/eis/tcf'");
        }
        if ( jmsTCFTCP == null ) {
            throw new ServletException("Null topic connection factory 'java:comp/env/eis/tcf1'");
        }

        if ( jmsTopic == null ) {
            throw new ServletException("Null topic 'java:comp/env/eis/topic1'");
        }
        if ( jmsTopic1 == null ) {
            throw new ServletException("Null topic 'java:comp/env/eis/topic2'");
        }
        if ( jmsTopic2 == null ) {
            throw new ServletException("Null topic 'java:comp/env/eis/topic3'");
        }
    }

    /**
     * Handle a GET request to this servlet: Invoke the test method specified as
     * request paramater "test".
     *
     * The test method throws an exception when it fails.  If no exception
     * is thrown by the test method, indicate success through the response
     * output.  If an exception is thrown, omit the success indication.
     * Instead, display an error indication and display the exception stack
     * to the response output.
     *
     * @param request The HTTP request which is being processed.
     * @param response The HTTP response which is being processed.
     *
     * @throws ServletException Thrown in case of a servlet processing error.
     * @throws IOException Thrown in case of an input/output error.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {

        String test = request.getParameter("test");

        PrintWriter out = response.getWriter();
        out.println("Starting " + test + "<br>");

        // The injection engine doesn't like this at the class level.
        TraceComponent tc = Tr.register(DeliveryDelayServlet.class);

        Tr.entry(this, tc, test);
        try {
            System.out.println(" Starting : " + test);
            getClass().getMethod(test, HttpServletRequest.class, HttpServletResponse.class)
                      .invoke(this, request, response);
       
            out.println(test + " COMPLETED SUCCESSFULLY");
            System.out.println(" Ending : " + test);
            Tr.exit(this, tc, test);

        } catch ( Throwable e ) {
            if ( e instanceof InvocationTargetException ) {
                e = e.getCause();
            }

            out.println("<pre>ERROR in " + test + ":");
            System.out.println(" Ending : " + test);
            System.out.println(" <ERROR> " + e.getMessage() + " </ERROR>");
            e.printStackTrace(out);
            out.println("</pre>");

            Tr.exit(this, tc, test, e);
        }
    }

    
    // Simple deliveryDelay tests using the JMS2.0 Simplified API

    public void testSetDeliveryDelay(HttpServletRequest request, HttpServletResponse response) throws JMSException, TestException {
        emptyQueue(jmsQCFBindings, jmsQueue);
        testSetDeliveryDelay(jmsQCFBindings, jmsQueue, false);
    }

    public void testSetDeliveryDelay_Tcp(HttpServletRequest request, HttpServletResponse response) throws JMSException, TestException {
        emptyQueue(jmsQCFTCP, jmsQueue);
        testSetDeliveryDelay(jmsQCFTCP, jmsQueue, false);
    }

    public void testSetDeliveryDelayTopic(HttpServletRequest request, HttpServletResponse response) throws JMSException, TestException {
        testSetDeliveryDelay(jmsTCFBindings, jmsTopic, false);
    }

    public void testSetDeliveryDelayTopic_Tcp(HttpServletRequest request, HttpServletResponse response) throws JMSException, TestException {
        testSetDeliveryDelay(jmsTCFTCP, jmsTopic, false);
    }
    
    public void testSetDeliveryDelayTopicDurSub(
            HttpServletRequest request, HttpServletResponse response) throws JMSException, TestException {
    	testSetDeliveryDelay(jmsTCFBindings, jmsTopic, true);
    }
    
    public void testSetDeliveryDelayTopicDurSub_Tcp(
            HttpServletRequest request, HttpServletResponse response) throws JMSException, TestException {
    	testSetDeliveryDelay(jmsTCFTCP, jmsTopic, true);
    }
    
    /**
     * 
     * Internal test code used by setDeliveryDelay tests.
     * 
     * This connects to the messaging provider, sends a message to a destination with a (test default) deliveryDelay
     * It then receives the message back using a receive timeout of twice the deliverDelay period.
     * The test then checks that the message was only received after the deliveryDelay period had elapsed.
     * This can be driven for either messaging domain and optionally with a durable subscriber
     * 
     * @param connectionFactory
     * @param destination
     * @param useDurableSubscriber
     * @throws JMSException
     * @throws TestException
     */
    private void testSetDeliveryDelay(ConnectionFactory connectionFactory, Destination destination, boolean useDurableSubscriber) throws JMSException, TestException {

    	// Do an initial check that we haven't been asked for a durableSubscriber but only been given a Queue destination
    	if ( useDurableSubscriber && !(destination instanceof Topic)) {
    		throw new TestException("Incompatible parameters. useDurablSubscriber specified, but destination was not a Topic: " + destination);
    	}
    	
    	String durableSubscriberName = methodName() + "_dursub";
    	
        try (JMSContext jmsContext = connectionFactory.createContext()) {
        	
        	JMSConsumer jmsConsumer = null;
        	
        	if (useDurableSubscriber) {
        		jmsConsumer = jmsContext.createDurableConsumer((Topic)destination, durableSubscriberName);
        	}
        	else {
        		jmsConsumer = jmsContext.createConsumer(destination);
        	}

            JMSProducer jmsProducer = jmsContext.createProducer();
            jmsProducer.setDeliveryDelay(defaultTestDeliveryDelay);

            TextMessage sentMessage = jmsContext.createTextMessage(methodName() + " at " + timeStamp());

            long beforeSend = this.sendAndCheckDeliveryTime(jmsProducer, destination, sentMessage);

            TextMessage receivedMessage = (TextMessage) jmsConsumer.receive(defaultTestDeliveryDelay * 2 );
            long afterReceive = System.currentTimeMillis();

            // If necessary, unsubscribe the durable subscriber before we check the results. Just print a warning if this fails.
            try {
                if (useDurableSubscriber) {
                    jmsConsumer.close();
                    jmsContext.unsubscribe(durableSubscriberName);
                }
            }
            catch (JMSRuntimeException je ){
            	System.out.println("WARNING: failed to unsubscribe durable subscription - " + durableSubscriberName);
            	System.out.println(je.toString());
            	je.printStackTrace(System.out);
            }
            
            
            if (receivedMessage == null)
                throw new TestException("No message received, sentMessage:" + sentMessage);
            if (!receivedMessage.getText().equals(sentMessage.getBody(String.class)))
                throw new TestException("Wrong message received:" + receivedMessage + " sent:" + sentMessage);
            if(afterReceive - beforeSend < defaultTestDeliveryDelay )
                throw new TestException("Message received too soon, beforeSend: " + beforeSend + " afterReceive: " + afterReceive + " deliveryDelay: " + defaultTestDeliveryDelay
                        + "\nreceivedMessage:\n" + receivedMessage);            

            
            
        }
        return;
    }

    
    // Simple setDeliveryDelay tests using standard domain-specific APIs
    
    // Tests for point-to-point domain
    
    public void testSetDeliveryDelayClassicApi(
            HttpServletRequest request, HttpServletResponse response) throws Exception {
    	
    	emptyQueue(jmsQCFBindings, jmsQueue1);
    	testSetDeliveryDelayQueueClassicApi(jmsQCFBindings, jmsQueue1);
  
    }
    
    public void testSetDeliveryDelayClassicApi_Tcp(
            HttpServletRequest request, HttpServletResponse response) throws Exception {
    	
    	emptyQueue(jmsQCFTCP, jmsQueue1);
    	testSetDeliveryDelayQueueClassicApi(jmsQCFTCP, jmsQueue1);
    	
    }
    
    /**
     * Simple setDeliveryDelay test that uses the standard point-to-point API
     * 
     * @param queueConnectionFactory
     * @param queue
     * @throws Exception
     */
    private void testSetDeliveryDelayQueueClassicApi(QueueConnectionFactory connectionFactory, Queue queue) throws Exception {
    	
    	
    	try ( QueueConnection connection = connectionFactory.createQueueConnection();
    		  QueueSession session = connection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);) {
    		
        	connection.start();

            QueueReceiver receiver = session.createReceiver(queue);

            QueueSender sender = session.createSender(queue);
            sender.setDeliveryDelay(defaultTestDeliveryDelay);

            TextMessage sentMessage = session.createTextMessage(methodName() + " at " + timeStamp());
        	
            long beforeSend = sendAndCheckDeliveryTime(sender, queue, sentMessage);
            
            TextMessage receivedMessage = (TextMessage) receiver.receive(defaultTestDeliveryDelay * 2);
            long afterReceive = System.currentTimeMillis();

            if (receivedMessage == null)
                throw new TestException("No message received, sentMessage:" + sentMessage);
            if (!receivedMessage.getText().equals(sentMessage.getBody(String.class)))
                throw new TestException("Wrong message received:" + receivedMessage + " sent:" + sentMessage);
            if(afterReceive - beforeSend < defaultTestDeliveryDelay )
                throw new TestException("Message received to soon, beforeSend: " + beforeSend + " afterReceive: " + afterReceive + " deliveryDelay: " + defaultTestDeliveryDelay
                        + "\nreceivedMessage:\n" + receivedMessage);            
    		
    	}
    	
    	return;
    }
    

    
    // tests for pub/sub domain
    
    public void testSetDeliveryDelayTopicClassicApi(
            HttpServletRequest request, HttpServletResponse response) throws Exception {
    	
    	testSetDeliveryDelayTopicClassicApi(jmsTCFBindings, jmsTopic, false);
    	
    }
    
    public void testSetDeliveryDelayTopicClassicApi_Tcp(
            HttpServletRequest request, HttpServletResponse response) throws Exception {

    	testSetDeliveryDelayTopicClassicApi(jmsTCFTCP, jmsTopic, false);

    }

    public void testSetDeliveryDelayTopicDurSubClassicApi(
            HttpServletRequest request, HttpServletResponse response) throws Exception {

    	testSetDeliveryDelayTopicClassicApi(jmsTCFBindings, jmsTopic, true);

    }

    public void testSetDeliveryDelayTopicDurSubClassicApi_Tcp(
            HttpServletRequest request, HttpServletResponse response) throws Exception {

    	testSetDeliveryDelayTopicClassicApi(jmsTCFTCP, jmsTopic, true);

    }
    
    /**
     * Simple setDeliveryDelay test that uses the standard publish/subscribe API 
     * 
     * @param connnectionFactory
     * @param topic
     * @param useDurableSubscriber whether to create a durable or nondurable subscriber
     * @throws Exception
     */
    private void testSetDeliveryDelayTopicClassicApi( TopicConnectionFactory connectionFactory, Topic topic, boolean useDurableSubscriber) throws Exception {

    	String durableSubscriberName = methodName() + "_dursub";

    	try (TopicConnection connection = connectionFactory.createTopicConnection();
    		 TopicSession session = connection.createTopicSession(false, Session.AUTO_ACKNOWLEDGE)) {
    		
            connection.start();

            
        	TopicSubscriber subscriber = null;
        	if (useDurableSubscriber) {
            	subscriber = session.createDurableSubscriber(topic, durableSubscriberName);
            }
            else {
                subscriber = session.createSubscriber(topic);
            }

            TopicPublisher publisher = session.createPublisher(topic);
            publisher.setDeliveryDelay(defaultTestDeliveryDelay);

            TextMessage sentMessage = session.createTextMessage(methodName() + " at " + timeStamp());
            long beforeSend = sendAndCheckDeliveryTime(publisher, topic, sentMessage);
            
            
            TextMessage receivedMessage = (TextMessage) subscriber.receive(defaultTestDeliveryDelay * 2);
            long afterReceive = System.currentTimeMillis();

            // If necessary, unsubscribe the durable subscriber before we check the results. Just print a warning if this fails.
            if (useDurableSubscriber) {

                try {
                    subscriber.close();
                    if (useDurableSubscriber) {
                        session.unsubscribe(durableSubscriberName);
                    }
                }
                catch (JMSException je) {
                	System.out.println("WARNING: failed to unsubscribe durable subscription - " + durableSubscriberName);
                	System.out.println(je.toString());
                	je.printStackTrace(System.out);
                }
            }

            
            if (receivedMessage == null)
            	throw new TestException("No message received, sentMessage:" + sentMessage);
            if (!receivedMessage.getText().equals(sentMessage.getBody(String.class)))
            	throw new TestException("Wrong message received:" + receivedMessage + " sent:" + sentMessage);
            if(afterReceive - beforeSend < defaultTestDeliveryDelay )
            	throw new TestException("Message received to soon, beforeSend: " + beforeSend + " afterReceive: " + afterReceive + " deliveryDelay: " + defaultTestDeliveryDelay
                        + "\nreceivedMessage:\n" + receivedMessage);            

    	}
    	
    	return;
    	
     }
    
	// ------------------------------------------------------------------------

    // Methods to send messages with different DeliveryDelay values

	/**
	 * Internal method to send 2 message with different deliveryDelay values to a
	 * destination. The second message will have a shorter deliveryDelay than the
	 * first.
	 *
	 * @param connectionFactory
	 * @param destination
	 * @param messageTextStem   the base text to put into the sent messages. The
	 *                          number of each message will be appended to this to
	 *                          differentiate them
	 * @param useSimplifiedAPI
	 */
	private void sendMessagesWithDifferentDeliveryDelays(ConnectionFactory connectionFactory,
			Destination destination, String messageTextStem, boolean useSimplifiedAPI) throws Exception {

		String expectedDeliveryTime_PropertyName = "ExpectedDeliveryTime";
		long delay = Message.DEFAULT_DELIVERY_DELAY;

		if (destination instanceof Queue)
			emptyQueue(connectionFactory, (Queue) destination);
		if (useSimplifiedAPI) {

			try (JMSContext jmsContext = connectionFactory.createContext()) {
				JMSProducer jmsProducer = jmsContext.createProducer();

				delay = defaultTestDeliveryDelay * 2;
				jmsProducer.setDeliveryDelay(delay);
				jmsProducer.setProperty(expectedDeliveryTime_PropertyName,
						(Calendar.getInstance().getTimeInMillis() + delay));
				jmsProducer.send(destination, messageTextStem + "1");

				delay = defaultTestDeliveryDelay;
				jmsProducer.setDeliveryDelay(delay);
				jmsProducer.setProperty(expectedDeliveryTime_PropertyName,
						(Calendar.getInstance().getTimeInMillis() + delay));
				jmsProducer.send(destination, messageTextStem + "2");

			}

		} else {

			// Previously the classic API test used the older, domain-specific APIs.
			// Here, we will use the JMS1.1 unified domain API as it means we can remove
			// duplication.
			// For completeness, we could return later to make this part use either type and
			// test all the combinations.

			try (Connection connection = connectionFactory.createConnection();
					Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)) {

				connection.start();

				MessageProducer messageProducer = session.createProducer(destination);
				TextMessage sendMessage = session.createTextMessage();

				delay = defaultTestDeliveryDelay * 2;
				messageProducer.setDeliveryDelay(delay);
				sendMessage.setText(messageTextStem + "1");
				sendMessage.setLongProperty(expectedDeliveryTime_PropertyName,
						(Calendar.getInstance().getTimeInMillis() + delay));
				messageProducer.send(sendMessage);

				delay = defaultTestDeliveryDelay;
				messageProducer.setDeliveryDelay(delay);
				sendMessage.setText(messageTextStem + "2");
				sendMessage.setLongProperty(expectedDeliveryTime_PropertyName,
						(Calendar.getInstance().getTimeInMillis() + delay));
				messageProducer.send(sendMessage);

			}

		}

		return;
	}
    
	// Externally visible variants oftestDeliveryDelayForDifferentDelays()
	
	// Simplified API variants
	
    public void testDeliveryDelayForDifferentDelays(
        HttpServletRequest request, HttpServletResponse response) throws Exception {
    	
        Queue queue = (Queue) new InitialContext().lookup("java:comp/env/jndi_INPUT_Q");
    	sendMessagesWithDifferentDeliveryDelays(jmsQCFBindings, queue, "QueueBindingsMessage", true);

        Thread.sleep(8000);
        
        return;
    }

    public void testDeliveryDelayForDifferentDelays_Tcp(
        HttpServletRequest request, HttpServletResponse response) throws Exception {
    	
        Queue queue = (Queue) new InitialContext().lookup("java:comp/env/jndi_INPUT_Q");
    	sendMessagesWithDifferentDeliveryDelays(jmsQCFTCP, queue, "QueueTCPMessage", true);

        Thread.sleep(8000);
        
        return;
    }

    public void testDeliveryDelayForDifferentDelaysTopic(
        HttpServletRequest request, HttpServletResponse response) throws Exception {
    	
        Topic topic = (Topic) new InitialContext().lookup("java:comp/env/eis/topic");
    	sendMessagesWithDifferentDeliveryDelays(jmsTCFBindings, topic, "TopicBindingsMessage", true);

        Thread.sleep(20000);

    	return;
    }

    public void testDeliveryDelayForDifferentDelaysTopic_Tcp(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        Topic topic = (Topic) new InitialContext().lookup("java:comp/env/eis/topic");
    	sendMessagesWithDifferentDeliveryDelays(jmsTCFTCP, topic, "TopicTCPMessage", true);
    	
        Thread.sleep(20000);

    	return;
    }
    
    // Classic API variants
    
    public void testDeliveryDelayForDifferentDelaysClassicApi(
            HttpServletRequest request, HttpServletResponse response) throws Exception {

        Queue queue = (Queue) new InitialContext().lookup("java:comp/env/jndi_INPUT_Q");

    	sendMessagesWithDifferentDeliveryDelays(jmsQCFBindings, queue, "QueueBindingsMessage-ClassicApi", false);

        Thread.sleep(8000);
          
        return;
    }

    public void testDeliveryDelayForDifferentDelaysClassicApi_Tcp(
        HttpServletRequest request, HttpServletResponse response) throws Exception {
    	
        Queue queue = (Queue) new InitialContext().lookup("java:comp/env/jndi_INPUT_Q");
    	sendMessagesWithDifferentDeliveryDelays(jmsQCFTCP, queue, "QueueTCPMessage-ClassicApi", false);

        Thread.sleep(8000);
        
        return;
    }

    public void testDeliveryDelayForDifferentDelaysTopicClassicApi(
        HttpServletRequest request, HttpServletResponse response) throws Exception {
    	
        Topic topic = (Topic) new InitialContext().lookup("java:comp/env/eis/topic");
    	sendMessagesWithDifferentDeliveryDelays(jmsTCFBindings, topic, "TopicBindingsMessage-ClassicApi", false);


        Thread.sleep(20000);

        return;
    }

    public void testDeliveryDelayForDifferentDelaysTopicClassicApi_Tcp(
        HttpServletRequest request, HttpServletResponse response) throws Exception {
    	
        Topic topic = (Topic) new InitialContext().lookup("java:comp/env/eis/topic");
    	sendMessagesWithDifferentDeliveryDelays(jmsTCFTCP, topic, "TopicTCPMessage-ClassicApi", false);

        Thread.sleep(20000);
        
        return;
    }
    
	// ------------------------------------------------------------------------

    
    public void testDeliveryMultipleMsgs(
            HttpServletRequest request, HttpServletResponse response) throws JMSException, TestException {
        emptyQueue(jmsQCFBindings, jmsQueue);
        emptyQueue(jmsQCFBindings, jmsQueue1);
        testDeliveryMultipleMsgs(jmsQCFBindings, new Destination[] {jmsQueue, jmsQueue1});
    }
    
    public void testDeliveryMultipleMsgs_Tcp(
            HttpServletRequest request, HttpServletResponse response) throws JMSException, TestException {
        emptyQueue(jmsQCFBindings, jmsQueue);
        emptyQueue(jmsQCFBindings, jmsQueue1);
        testDeliveryMultipleMsgs(jmsQCFTCP, new Destination[] {jmsQueue, jmsQueue1});
    }
   
    public void testDeliveryMultipleMsgsTopic(
            HttpServletRequest request, HttpServletResponse response) throws JMSException, TestException {
        testDeliveryMultipleMsgs(jmsTCFBindings, new Destination[] {jmsTopic, jmsTopic1});
    }
    
    public void testDeliveryMultipleMsgsTopic_Tcp(
            HttpServletRequest request, HttpServletResponse response) throws JMSException, TestException {
        testDeliveryMultipleMsgs(jmsTCFTCP, new Destination[] {jmsTopic, jmsTopic1});
    }
    
    private void testDeliveryMultipleMsgs(ConnectionFactory connectionFactory, Destination[] destinations) throws JMSException, TestException {

        try (JMSContext jmsContext = jmsQCFBindings.createContext()) {

            JMSConsumer[] jmsConsumers = new JMSConsumer[destinations.length];
            for (int i = 0; i < destinations.length; i++)
                jmsConsumers[i] = jmsContext.createConsumer(destinations[i]);

            JMSProducer jmsProducer = jmsContext.createProducer();
            jmsProducer.setDeliveryDelay(defaultTestDeliveryDelay);

            TextMessage[] sentMessages = new TextMessage[destinations.length];
            long beforeSend = System.currentTimeMillis();
            for (int i = 0; i < destinations.length; i++) {
                sentMessages[i] = jmsContext.createTextMessage(methodName() + " to" + destinations[i] + " at " + timeStamp());
                jmsProducer.send(destinations[i], sentMessages[i]);
            }
            long afterSend = System.currentTimeMillis();
            if (afterSend - beforeSend > defaultTestDeliveryDelay)
                throw new TestException("Test Infrastructure running too slowly to meangfully test delivery delay beforeSend:"+beforeSend+" afterSend:"+afterSend+" deliveryDelay:"+defaultTestDeliveryDelay);

            for (int i = 0; i < destinations.length; i++) {
                TextMessage receivedMessage = (TextMessage) jmsConsumers[i].receive(30000);
                long afterReceive = System.currentTimeMillis();
                if (receivedMessage == null)
                    throw new TestException("No message received("+i+"), sentMessage:" + sentMessages[i]); 
                if (!receivedMessage.getText().equals(sentMessages[i].getBody(String.class)))
                    throw new TestException("Wrong message ("+i+") received:" + receivedMessage + " sent:" + sentMessages[i]);
                if(afterReceive - beforeSend < defaultTestDeliveryDelay )
                    throw new TestException("Message received to soon, afterSend:"+afterSend+" afterReceive"+afterReceive+" deliveryDelay:"+defaultTestDeliveryDelay
                            +"\nreceivedMessage:" + receivedMessage);
            } 
        }
    }

    public void testDeliveryDelayZeroAndNegativeValues(HttpServletRequest request, HttpServletResponse response) throws JMSException, TestException {
        emptyQueue(jmsQCFBindings, jmsQueue1);
        testDeliveryDelayZeroAndNegativeValues(jmsQCFBindings, jmsQueue1);
    }
    
    public void testDeliveryDelayZeroAndNegativeValues_Tcp(HttpServletRequest request, HttpServletResponse response) throws JMSException, TestException {
        emptyQueue(jmsQCFTCP, jmsQueue1);
        testDeliveryDelayZeroAndNegativeValues(jmsQCFTCP, jmsQueue1);
    }
    
    public void testDeliveryDelayZeroAndNegativeValuesTopic(HttpServletRequest request, HttpServletResponse response) throws JMSException, TestException {
        testDeliveryDelayZeroAndNegativeValues(jmsTCFBindings, jmsTopic);
    }
    
    public void testDeliveryDelayZeroAndNegativeValuesTopic_Tcp(HttpServletRequest request, HttpServletResponse response) throws JMSException, TestException {
        testDeliveryDelayZeroAndNegativeValues(jmsTCFTCP, jmsTopic);
    }
    
    private void testDeliveryDelayZeroAndNegativeValues(ConnectionFactory connectionFactory, Destination destination) throws JMSException, TestException {

        // Note that, we use a transacted Context. Transacted, container managed and autocommit should all work.
        try (JMSContext jmsContext = connectionFactory.createContext(JMSContext.SESSION_TRANSACTED)) {
           
            JMSProducer jmsProducer = jmsContext.createProducer();
            jmsProducer.setDeliveryDelay(0);

            JMSConsumer jmsConsumer = jmsContext.createConsumer(destination);

            String sentMessageBody = methodName() + " at " + timeStamp();
            jmsProducer.send(destination, sentMessageBody);
            jmsContext.commit();

            // Even though we are testing that there is no delay we allow 100 milliseconds for the
            // message to appear on the queue, due to network transmission delays and server side processing.
            TextMessage receivedMessage = (TextMessage) jmsConsumer.receive(100);
            jmsContext.commit();

            if (receivedMessage == null) {
                // Wait a little longer to see if the message arrives late.
                TextMessage receivedMessageLate = (TextMessage) jmsConsumer.receive(10*1000);
                jmsContext.commit();
                if (receivedMessageLate == null)
                    throw new TestException("No message received, sent:" + sentMessageBody + " time now:"+timeStamp());
                else 
                    throw new TestException("Message received late, sent:" + sentMessageBody + "\nreceived:"+ receivedMessageLate + "\ntime now:"+timeStamp());
            }
            if (!receivedMessage.getText().equals(sentMessageBody))
                throw new TestException("Wrong message received:" + receivedMessage + " sent:" + sentMessageBody);

            try {
                jmsProducer.setDeliveryDelay(-10);
                throw new TestException("Incorrectly set delivery delay to -10");
            } catch (JMSRuntimeException e) {
                // expected
            }
        }
    }
   
    public void testSettingMultipleProperties(HttpServletRequest request, HttpServletResponse response) throws JMSException, TestException {
        emptyQueue(jmsQCFBindings, jmsQueue);
        testSettingMultipleProperties(jmsQCFBindings, jmsQueue);
    }
    
    public void testSettingMultipleProperties_Tcp(HttpServletRequest request, HttpServletResponse response) throws JMSException, TestException {
        emptyQueue(jmsQCFTCP, jmsQueue);
        testSettingMultipleProperties(jmsQCFTCP, jmsQueue);
    }
    
    public void testSettingMultiplePropertiesTopic(HttpServletRequest request, HttpServletResponse response) throws JMSException, TestException {
        emptyQueue(jmsQCFBindings, jmsQueue);
        testSettingMultipleProperties(jmsQCFBindings, jmsTopic);
    }
    
    public void testSettingMultiplePropertiesTopic_Tcp(HttpServletRequest request, HttpServletResponse response) throws JMSException, TestException {
        emptyQueue(jmsQCFTCP, jmsQueue);
        testSettingMultipleProperties(jmsQCFTCP, jmsTopic);
    }

    private void testSettingMultipleProperties(ConnectionFactory connectionFactory, Destination destination) throws JMSException, TestException {

        try (JMSContext jmsContext = connectionFactory.createContext()) {
            JMSProducer jmsProducer = jmsContext.createProducer();
            jmsProducer.setDeliveryDelay(1000).setDisableMessageID(true);

            JMSConsumer jmsConsumer = jmsContext.createConsumer(destination);

            String sentMessageBody = methodName() + " at " + timeStamp();
            jmsProducer.send(destination, sentMessageBody);
            TextMessage receivedMessage = (TextMessage) jmsConsumer.receive(30000);

            if (receivedMessage == null)
                throw new TestException("No message received, sent:" + sentMessageBody);
            if (!receivedMessage.getText().equals(sentMessageBody))
                throw new TestException("Wrong message received:" + receivedMessage + " sent:" + sentMessageBody);
            if (receivedMessage.getJMSMessageID() != null)
                throw new TestException("Wrong JMSMessageID received:" + receivedMessage + " sent:" + sentMessageBody);
        }
    }

    // TODO: Why do we have this when we could be using the deliveryDelay variable instead? Remove this later.
    private static final int DELIVERY_DELAY = 2000;

    public void testTransactedSend_B(HttpServletRequest request, HttpServletResponse response) throws JMSException, TestException, InterruptedException {
        emptyQueue(jmsQCFBindings, jmsQueue);
        testTransactedSend(jmsQCFBindings, jmsQueue1);
    }

    public void testTransactedSend_Tcp(HttpServletRequest request, HttpServletResponse response) throws JMSException, TestException, InterruptedException {
        emptyQueue(jmsQCFTCP, jmsQueue);
        testTransactedSend(jmsQCFTCP, jmsQueue1);
    }

    public void testTransactedSendTopic_B(HttpServletRequest request, HttpServletResponse response) throws JMSException, TestException, InterruptedException {
        testTransactedSend(jmsTCFBindings, jmsTopic);
    }

    public void testTransactedSendTopic_Tcp(HttpServletRequest request, HttpServletResponse response) throws JMSException, TestException, InterruptedException {
        testTransactedSend(jmsTCFTCP, jmsTopic);
    }

    /**
     * For transacted sends, the delay time starts when the client sends the message, not when the transaction is committed.
     * 
     * @see https://docs.oracle.com/javaee/7/api/index.html?javax/jms/JMSProducer.html
     */
    private void testTransactedSend(ConnectionFactory connectionFactory, Destination destination) throws JMSException, TestException, InterruptedException {

        try (JMSContext jmsContext = connectionFactory.createContext(Session.SESSION_TRANSACTED)) {

            JMSConsumer jmsConsumer = jmsContext.createConsumer(destination);

            JMSProducer jmsProducer = jmsContext.createProducer();
            jmsProducer.setDeliveryDelay(defaultTestDeliveryDelay);

            TextMessage sentMessage = jmsContext.createTextMessage(methodName() + " at " + timeStamp());
            long beforeSend = System.currentTimeMillis();
            jmsProducer.send(destination, sentMessage);
            long afterSend = System.currentTimeMillis();
            if (afterSend - beforeSend > defaultTestDeliveryDelay)
                throw new TestException("Test Infrastructure running too slowly to meangfully test delivery delay beforeSend:" + beforeSend + " afterSend:" + afterSend
                        + " deliveryDelay:" + defaultTestDeliveryDelay);
            
            final long commitDelay = 1000;
            Thread.sleep(commitDelay);
            jmsContext.commit();

            TextMessage receivedMessage = (TextMessage) jmsConsumer.receive(30000);
            long afterReceive = System.currentTimeMillis();
            jmsContext.commit();
            if (receivedMessage == null)
                throw new TestException("No message received, sentMessage:" + sentMessage);
            
            // For PubSub messages in Sib, the delivery delay time starts when the message is committed, later than what the JMS spec says. 
            // For point to point messages, the delivery delay starts when the message is sent.
            // JMSDeliveryTime is computed from the time the message is sent, even though a published message is actually received
            // after deliverydelay+commitDelay. The JMS specification says the earliest time a message can be received is after deliveryDelay
            // so this is within the specification. 
            
            long receiveLatency = afterReceive - receivedMessage.getJMSDeliveryTime();
            if (receiveLatency < 0 )
                throw new TestException("JMSDeliveryTime too soon, afterReceive:" + afterReceive + " JMSDeliveryTime:" + receivedMessage.getJMSDeliveryTime()
                        + "\nreceivedMessage:" + receivedMessage);
            
            if (!receivedMessage.getText().equals(sentMessage.getBody(String.class)))
                throw new TestException("Wrong message received:" + receivedMessage + " sent:" + sentMessage);
            if (afterReceive - beforeSend < defaultTestDeliveryDelay)
                throw new TestException("Message received to soon, afterReceive:" + afterReceive + " beforeSend:" + beforeSend + " deliveryDelay:" + defaultTestDeliveryDelay
                        + "\nreceivedMessage:" + receivedMessage);
            
        }
    }

    public void testTiming_B(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContext = jmsQCFBindings.createContext();
        emptyQueue(jmsQCFBindings, jmsQueue);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);

        JMSProducer jmsProducer = jmsContext.createProducer();

        Message sendMsg = jmsContext.createTextMessage("testTiming_B");
        jmsProducer.setDeliveryDelay(1000);
        jmsProducer.send(jmsQueue, sendMsg);

        long jmsDeliveryTime = sendMsg.getLongProperty("JMSDeliveryTime");
        long jmsTimestamp = sendMsg.getLongProperty("JMSTimestamp");

        if ( jmsDeliveryTime != (jmsTimestamp + 1000) ) {
            testFailed = true;
        }

        TextMessage recMsg2 = (TextMessage) jmsConsumer.receive(31000);
        long receive_time = System.currentTimeMillis();

        if ( (recMsg2.getText() != null) && recMsg2.getText().equals("testTiming_B")) {
            if ( jmsDeliveryTime > receive_time ) {
                testFailed = true;
            }
        } else {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testTiming_B failed");
        }
    }

    public void testTiming_Tcp(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContext = jmsQCFTCP.createContext();
        emptyQueue(jmsQCFTCP, jmsQueue);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);

        JMSProducer jmsProducer = jmsContext.createProducer();

        Message sendMsg = jmsContext.createTextMessage("testTiming_Tcp");
        jmsProducer.setDeliveryDelay(1000);
        jmsProducer.send(jmsQueue, sendMsg);

        long jmsDeliveryTime = sendMsg.getLongProperty("JMSDeliveryTime");
        long jmsTimestamp = sendMsg.getLongProperty("JMSTimestamp");

        if ( jmsDeliveryTime != (jmsTimestamp + 1000) ) {
            testFailed = true;
        }

        TextMessage recMsg2 = (TextMessage) jmsConsumer.receive(31000);
        long receive_time = System.currentTimeMillis();

        if ( (recMsg2.getText() != null) && recMsg2.getText().equals("testTiming_Tcp") ) {
            if ( jmsDeliveryTime > receive_time ) {
                testFailed = true;
            }
        } else {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testTiming_Tcp failed");
        }
    }

    public void testTimingTopic_B(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContext = jmsTCFBindings.createContext();
        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsTopic);
        JMSProducer jmsProducer = jmsContext.createProducer();

        Message sendMsg = jmsContext.createTextMessage("testTimingTopic_B");
        jmsProducer.setDeliveryDelay(1000);
        jmsProducer.send(jmsTopic, sendMsg);

        long jmsDeliveryTime = sendMsg.getLongProperty("JMSDeliveryTime");
        long jmsTimestamp = sendMsg.getLongProperty("JMSTimestamp");

        if ( jmsDeliveryTime != (jmsTimestamp + 1000) ) {
            testFailed = true;
        }

        TextMessage recMsg2 = (TextMessage) jmsConsumer.receive(31000);
        long receive_time = System.currentTimeMillis();

        if ( (recMsg2.getText() != null) && recMsg2.getText().equals("testTimingTopic_B") ) {
            if ( jmsDeliveryTime > receive_time ) {
                testFailed = true;
            }
        } else {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testTimingTopic_B failed");
        }
    }

    public void testTimingTopic_Tcp(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContext = jmsTCFTCP.createContext();
        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsTopic);
        JMSProducer jmsProducer = jmsContext.createProducer();

        Message sendMsg = jmsContext.createTextMessage("testTimingTopic_Tcp");
        jmsProducer.setDeliveryDelay(1000);
        jmsProducer.send(jmsTopic, sendMsg);

        long jmsDeliveryTime = sendMsg.getLongProperty("JMSDeliveryTime");
        long jmsTimestamp = sendMsg.getLongProperty("JMSTimestamp");

        if ( jmsDeliveryTime != (jmsTimestamp + 1000) ) {
            testFailed = true;
        }

        TextMessage recMsg2 = (TextMessage) jmsConsumer.receive(31000);
        long receive_time = System.currentTimeMillis();

        if ( (recMsg2.getText() != null) && recMsg2.getText().equals("testTimingTopic_Tcp") ) {
            if ( jmsDeliveryTime > receive_time ) {
                testFailed = true;
            }
        } else {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testTimingTopic_Tcp failed");
        }
    }

    public void testGetDeliveryDelay(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContext = jmsQCFBindings.createContext();
        JMSProducer jmsProducer = jmsContext.createProducer();

        long val = jmsProducer.getDeliveryDelay();
        if ( val != 0 ) {
            testFailed = true;
        }

        jmsProducer.setDeliveryDelay(1000);

        val = jmsProducer.getDeliveryDelay();
        if ( val != 1000 ) {
            testFailed = true;
        }

        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testGetDeliveryDelay failed");
        }
    }

    public void testGetDeliveryDelay_Tcp(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContext = jmsQCFTCP.createContext();
        JMSProducer jmsProducer = jmsContext.createProducer();

        long val = jmsProducer.getDeliveryDelay();
        if ( val != 0 ) {
            testFailed = true;
        }
        jmsProducer.setDeliveryDelay(1000);

        val = jmsProducer.getDeliveryDelay();
        if ( val != 1000) {
            testFailed = true;
        }

        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testGetDeliveryDelay_TCP failed");
        }
    }

    public void testGetDeliveryDelayTopic(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContext = jmsTCFBindings.createContext();
        JMSProducer jmsProducer = jmsContext.createProducer();

        long val = jmsProducer.getDeliveryDelay();
        if ( val != 0 ) {
            testFailed = true;
        }

        jmsProducer.setDeliveryDelay(1000);
        val = jmsProducer.getDeliveryDelay();
        if ( val != 1000 ) {
            testFailed = true;
        }

        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testGetDeliveryDelayTopic failed");
        }
    }

    public void testGetDeliveryDelayTopic_Tcp(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContext = jmsTCFTCP.createContext();
        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsTopic);
        JMSProducer jmsProducer = jmsContext.createProducer();

        long val = jmsProducer.getDeliveryDelay();
        if ( val != 0 ) {
            testFailed = true;
        }

        jmsProducer.setDeliveryDelay(1000);

        val = jmsProducer.getDeliveryDelay();
        if ( val != 1000 ) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testGetDeliveryDelayTopic failed");
        }
    }

    
    // Tests for delayed message persistence over server restart.
    // These tests are each in two parts; a sender part that sends 2 messages, and a receiver part that attempts to receive the messages.
    // The methods are driven from the Test class, which calls the sender method, restarts the server, and then calls the receiver method.
    
    
    // new tests for simplified API
    
    // New consolidated simplified API persistent test sender method
    private void testPersistentMessageSimplifiedAPI_send(ConnectionFactory cf,
    													Destination persistentMessageDestination,
    													Destination nonpersistentMessageDestination,
    													String identifier)
    													throws Exception {
    	
    	boolean pubSub = persistentMessageDestination instanceof Topic;
    	JMSConsumer jmsConsumer1 = null, jmsConsumer2 = null;
    	
    	try (JMSContext jmsContext = cf.createContext()) {

    		  JMSProducer jmsProducer = jmsContext.createProducer();

    		  // If we are sending messages to Topic destinations then there needs to be subscriptions to receive the messages
    		  if (pubSub) {
    		    jmsConsumer1 = jmsContext.createDurableConsumer((Topic) persistentMessageDestination, "durPersMsg1_" + identifier);
    		    jmsConsumer2 = jmsContext.createDurableConsumer((Topic) nonpersistentMessageDestination, "durPersMsg2_" + identifier);
    		  }
    		  else {
    			  // empty the message queue before sending the new messages.
    			  // This is replicating previous behaviour, but will probably need to be removed if we re-work the tests to run concurrently.
    			  emptyQueue(cf, (Queue)persistentMessageDestination);
    			  emptyQueue(cf, (Queue)nonpersistentMessageDestination);
    		  }
    		  
    		  // Send the messages
    		  jmsProducer.setDeliveryDelay(defaultTestDeliveryDelay).setDeliveryMode(DeliveryMode.PERSISTENT)
    	      .send(persistentMessageDestination, "PersistentMessage_" + identifier);

    		  jmsProducer.setDeliveryDelay(defaultTestDeliveryDelay).setDeliveryMode(DeliveryMode.NON_PERSISTENT)
    		  .send(nonpersistentMessageDestination, "NonPersistentMessage_" + identifier);

    	  // If we're running in the pub/sub domain then close the subscribers (the subscriptions will remain open
    	  if (pubSub) {
    	    jmsConsumer1.close();
    	    jmsConsumer2.close();
    	  }

    	}
    	
    	return;
    }
    
    // New consolidated simplified API persistent test receiver method
    private boolean testPersistentMessageSimplifiedAPI_receive(ConnectionFactory cf,
			Destination persistentMessageDestination,
			Destination nonpersistentMessageDestination,
			String identifier)
			throws Exception {
    	
    	boolean pubSub = persistentMessageDestination instanceof Topic;
    	boolean testPassed = true;
    	
        String subscriber1Name = "durPersMsg1_" + identifier;
        String subscriber2Name = "durPersMsg2_" + identifier;
    	
    	JMSConsumer jmsConsumer1 = null, jmsConsumer2 = null;
    	
    	try (JMSContext jmsContext = cf.createContext()) {
    		
    		if (pubSub) {
    		    jmsConsumer1 = jmsContext.createDurableConsumer((Topic) persistentMessageDestination, subscriber1Name);
    		    jmsConsumer2 = jmsContext.createDurableConsumer((Topic) nonpersistentMessageDestination, subscriber2Name);
    		}
    		else {
    	        jmsConsumer1 = jmsContext.createConsumer(persistentMessageDestination);
    	        jmsConsumer2 = jmsContext.createConsumer(nonpersistentMessageDestination);
    		}
    		
    		// Try to receive a message from each destination. Wait for twice the default deliveryDelay time if required
    		// Only the persistentMessageDestination should receive something
    		
            TextMessage recMsg1 = (TextMessage) jmsConsumer1.receive(defaultTestDeliveryDelay * 2);
            TextMessage recMsg2 = (TextMessage) jmsConsumer2.receive(defaultTestDeliveryDelay * 2);

            // and check whether we got the expected results
            if ( ((recMsg1 == null) || // If we failed to get a persistent message
                    (recMsg1.getText() == null) || // or the message had no payload...
                    !recMsg1.getText().equals("PersistentMessage_" + identifier)) || // ...or the payload wasn't what we expected (implying it's a message from some other test or something)
                   (recMsg2 != null) ) { // or we received a message from the nonpersistentMessageDestination (as the message there should not have persisted over the server restart)
                  testPassed = false; // ...then we need to mark the test as failed.
              }
            
            // tidy up
            jmsConsumer1.close();
            jmsConsumer2.close();
            
            if (pubSub) {
                jmsContext.unsubscribe(subscriber1Name);
                jmsContext.unsubscribe(subscriber2Name);
            }

    	}
    	
        return testPassed;
    }
    
    
    
    private String testPersistentQueueMessageIdentifier = "testPersistentQueueMessage";

    public void testPersistentMessage(
        HttpServletRequest request, HttpServletResponse response) throws Exception {
    	// Originally used message text "testPersistentMessage_PersistentMsg" "testPersistentMessage_NonPersistentMsg"
    	// jmsQCFBindings, jmsQueue, jmsQueue1
    	
    	testPersistentMessageSimplifiedAPI_send(jmsQCFBindings, jmsQueue, jmsQueue1, testPersistentQueueMessageIdentifier);
    	
    	return;
    	
    }

    public void testPersistentMessageReceive(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

    	boolean testPassed = testPersistentMessageSimplifiedAPI_receive(jmsQCFBindings, jmsQueue, jmsQueue1, testPersistentQueueMessageIdentifier);

        if ( !testPassed ) {
            throw new Exception("testPersistentMessageReceive failed");
        }
        
        return;
    }

    private String testPersistentQueueMessageTcpIdentifier = "testPersistentQueueMessageTcp";

    public void testPersistentMessage_Tcp(
        HttpServletRequest request, HttpServletResponse response) throws Exception {
    	// Originally used message text "testPersistentMessage_PersistentMsgTcp" "testPersistentMessage_NonPersistentMsgTcp"
    	// jmsQCFTCP, jmsQueue, jmsQueue1
    	
    	testPersistentMessageSimplifiedAPI_send(jmsQCFTCP, jmsQueue, jmsQueue1, testPersistentQueueMessageTcpIdentifier);

    	return;
    }

    public void testPersistentMessageReceive_Tcp(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

    	boolean testPassed = testPersistentMessageSimplifiedAPI_receive(jmsQCFTCP, jmsQueue, jmsQueue1, testPersistentQueueMessageTcpIdentifier);

        if ( !testPassed ) {
            throw new Exception("testPersistentMessageReceive_Tcp failed");
        }
        
        return;
    }

    private String testPersistentTopicMessageIdentifier = "testPersistentTopicMessage";
    
    public void testPersistentMessageTopic(
        HttpServletRequest request, HttpServletResponse response) throws Exception {
    	// Originally used message text "testPersistentMessage_PersistentMsgTopic" "testPersistentMessage_NonPersistentMsgTopic"
    	// jmsTCFBindings, jmsTopic, jmsTopic1

    	testPersistentMessageSimplifiedAPI_send(jmsTCFBindings, jmsTopic, jmsTopic1, testPersistentTopicMessageIdentifier);
    	
    	return;
    }

    public void testPersistentMessageReceiveTopic(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

    	boolean testPassed = testPersistentMessageSimplifiedAPI_receive(jmsTCFBindings, jmsTopic, jmsTopic1, testPersistentTopicMessageIdentifier);

        if ( !testPassed ) {
            throw new Exception("testPersistentMessageReceiveTopic failed");
        }
        
        return;
    }

    private String testPersistentTopicMessageTcpIdentifier = "testPersistentTopicMessageTcp";
    
    public void testPersistentMessageTopic_Tcp(
        HttpServletRequest request, HttpServletResponse response) throws Exception {
    	// Originally used message text "testPersistentMessage_PersistentMsgTopicTcp" "testPersistentMessage_NonPersistentMsgTopicTcp"
    	// jmsTCFTCP, jmsTopic, jmsTopic1
    	
    	testPersistentMessageSimplifiedAPI_send(jmsTCFTCP, jmsTopic, jmsTopic1, testPersistentTopicMessageTcpIdentifier);
    	
    	return;
    }

    public void testPersistentMessageReceiveTopic_Tcp(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

    	boolean testPassed = testPersistentMessageSimplifiedAPI_receive(jmsTCFTCP, jmsTopic, jmsTopic1, testPersistentTopicMessageTcpIdentifier);

        if ( !testPassed ) {
            throw new Exception("testPersistentMessageStoreReceiveTopic_Tcp failed");
        }
        
        return;
    }


    // Old tests for classic API
    // The new consolidated versions use the JMS1.1 unified domain objects instead of the older JMS1.02 domain-specific objects.
    
    
    // New consolidated classic API persistent test sender method
    private boolean testPersistentMessageClassicAPI_send(ConnectionFactory cf,
			Destination persistentMessageDestination,
			Destination nonpersistentMessageDestination,
			String identifier)
			throws Exception {
    	
    	// If we are sending pub/sub messages then we need to ensure there are durable subscribers that can receive the messages.
    	MessageConsumer consumer1 = null, consumer2 = null;

    	boolean pubSub = persistentMessageDestination instanceof Topic;
    	boolean testPassed = true;
    	
        String subscriber1Name = "durPersMsg1_" + identifier;
        String subscriber2Name = "durPersMsg2_" + identifier;

        Connection connection = cf.createConnection();
        connection.start();
        
        Session session = connection.createSession(Session.AUTO_ACKNOWLEDGE);
        
        MessageProducer producer1 = session.createProducer(persistentMessageDestination);
        MessageProducer producer2 = session.createProducer(nonpersistentMessageDestination);

        if (pubSub) {
        	consumer1 = session.createDurableConsumer((Topic)persistentMessageDestination, subscriber1Name);
           	consumer2 = session.createDurableConsumer((Topic)nonpersistentMessageDestination, subscriber2Name);
        }
        else {
        	emptyQueue(cf, (Queue)persistentMessageDestination);
        	emptyQueue(cf, (Queue)nonpersistentMessageDestination);
        }

        producer1.setDeliveryMode(DeliveryMode.PERSISTENT);
        producer1.setDeliveryDelay(defaultTestDeliveryDelay);
        TextMessage msg1 = session.createTextMessage("PersistentMessage_" + identifier);
        producer1.send(msg1);

        producer2.setDeliveryDelay(defaultTestDeliveryDelay);
        producer2.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
        TextMessage msg2 = session.createTextMessage("NonPersistentMessage_" + identifier);
        producer2.send(msg2);


        // If we're running in the pub/sub domain then close the subscribers (the subscriptions will remain open
        if (pubSub) {
        	consumer1.close();
        	consumer2.close();
        }
        
        session.close();
        connection.close();
        
        return testPassed;
    }
    
    // New consolidated classic API persistent test receiver method
    private boolean testPersistentMessageClassicAPI_receive(ConnectionFactory cf,
			Destination persistentMessageDestination,
			Destination nonpersistentMessageDestination,
			String identifier)
			throws Exception {
    	
    	boolean pubSub = persistentMessageDestination instanceof Topic;
    	boolean testPassed = true;
    	
        String subscriber1Name = "durPersMsg1_" + identifier;
        String subscriber2Name = "durPersMsg2_" + identifier;
        
        MessageConsumer consumer1 = null, consumer2 = null;
        
        
        Connection connection = cf.createConnection();
        connection.start();
        
        Session session = connection.createSession(Session.AUTO_ACKNOWLEDGE);
        
        if (pubSub) {
		    consumer1 = session.createDurableConsumer((Topic) persistentMessageDestination, subscriber1Name);
		    consumer2 = session.createDurableConsumer((Topic) nonpersistentMessageDestination, subscriber2Name);
		}
		else {
			consumer1 = session.createConsumer(persistentMessageDestination);
			consumer2 = session.createConsumer(nonpersistentMessageDestination);
		}
        
		// Try to receive a message from each destination. Wait for twice the default deliveryDelay time if required
		// Only the persistentMessageDestination should receive something
		
        TextMessage recMsg1 = (TextMessage) consumer1.receive(defaultTestDeliveryDelay * 2);
        TextMessage recMsg2 = (TextMessage) consumer2.receive(defaultTestDeliveryDelay * 2);
        
        // and check whether we got the expected results
        if ( ((recMsg1 == null) || // If we failed to get a persistent message
                (recMsg1.getText() == null) || // or the message had no payload...
                !recMsg1.getText().equals("PersistentMessage_" + identifier)) || // ...or the payload wasn't what we expected (implying it's a message from some other test or something)
               (recMsg2 != null) ) { // or we received a message from the nonpersistentMessageDestination (as the message there should not have persisted over the server restart)
              testPassed = false; // ...then we need to mark the test as failed.
          }

        // Tidy up
        consumer1.close();
        consumer2.close();
        
        if (pubSub) {
        	session.unsubscribe(subscriber1Name);
        	session.unsubscribe(subscriber2Name);
        }
        
        return testPassed;
    }
    
    
    private String testPersistentQueueMessageClassicIdentifier = "testPersistentQueueMessageClassic";

    public void testPersistentMessageClassicApi(
            HttpServletRequest request, HttpServletResponse response) throws Exception {
    	
    	testPersistentMessageClassicAPI_send(jmsQCFBindings, jmsQueue, jmsQueue1, testPersistentQueueMessageClassicIdentifier);
    	
    	return;
        }

        public void testPersistentMessageReceiveClassicApi(
            HttpServletRequest request, HttpServletResponse response) throws Exception {

        	boolean testPassed = testPersistentMessageClassicAPI_receive(jmsQCFBindings, jmsQueue, jmsQueue1, testPersistentQueueMessageClassicIdentifier);

            if ( !testPassed ) {
                throw new Exception("testPersistentMessageReceiveClassicApi failed");
            }
        }
        

        private String testPersistentQueueMessageClassicTcpIdentifier = "testPersistentQueueMessageClassicTcp";

        public void testPersistentMessageClassicApi_Tcp(
            HttpServletRequest request, HttpServletResponse response) throws Exception {

        	testPersistentMessageClassicAPI_send(jmsQCFTCP, jmsQueue, jmsQueue1, testPersistentQueueMessageClassicTcpIdentifier);
        	
        	return;
        }

        public void testPersistentMessageReceiveClassicApi_Tcp(
            HttpServletRequest request, HttpServletResponse response) throws Exception {

        	boolean testPassed = testPersistentMessageClassicAPI_receive(jmsQCFTCP, jmsQueue, jmsQueue1, testPersistentQueueMessageClassicTcpIdentifier);

            if ( !testPassed ) {
                throw new Exception("testPersistentMessageReceiveClassicApi failed");
            }
        }

        
        private String testPersistentTopicMessageClassicIdentifier = "testPersistentTopicMessageClassic";

        public void testPersistentMessageTopicClassicApi(
            HttpServletRequest request, HttpServletResponse response) throws Exception {

        	testPersistentMessageClassicAPI_send(jmsTCFBindings, jmsTopic, jmsTopic1, testPersistentTopicMessageClassicIdentifier);
        	
        	return;
        }

        public void testPersistentMessageReceiveTopicClassicApi(
            HttpServletRequest request, HttpServletResponse response) throws Exception {

        	boolean testPassed = testPersistentMessageClassicAPI_receive(jmsTCFBindings, jmsTopic, jmsTopic1, testPersistentTopicMessageClassicIdentifier);

            if ( !testPassed ) {
                throw new Exception("testPersistentMessageReceiveTopicClassicApi failed");
            }
        }

        private String testPersistentTopicMessageClassicTcpIdentifier = "testPersistentTopicMessageClassicTcp";

        public void testPersistentMessageTopicClassicApi_Tcp(
            HttpServletRequest request, HttpServletResponse response) throws Exception {

        	testPersistentMessageClassicAPI_send(jmsTCFTCP, jmsTopic, jmsTopic1, testPersistentTopicMessageClassicTcpIdentifier);
        	
        	return;
        }

        public void testPersistentMessageReceiveTopicClassicApi_Tcp(
            HttpServletRequest request, HttpServletResponse response) throws Exception {

        	boolean testPassed = testPersistentMessageClassicAPI_receive(jmsTCFTCP, jmsTopic, jmsTopic1, testPersistentTopicMessageClassicTcpIdentifier);

            if ( !testPassed ) {
                throw new Exception("testPersistentMessageStoreReceiveTopicClassicApi_Tcp failed");
            }
        }
    
    
    public void testTimeToLiveWithDeliveryDelay(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContext = jmsQCFBindings.createContext();
        emptyQueue(jmsQCFBindings, jmsQueue);

        JMSProducer jmsProducer = jmsContext.createProducer();
        jmsProducer.setTimeToLive(1000);
        jmsProducer.setDeliveryDelay(DELIVERY_DELAY);
        jmsProducer.send(jmsQueue, "testTimeToLiveWithDeliveryDelay");

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);
        TextMessage recMsg = (TextMessage) jmsConsumer.receive(30000);

        // TODO: Why is this test verification disabled?

        // if ( (recMsg == null) ||
        //      (recMsg.getText() == null) ||
        //      !recMsg.getText().equals("testTimeToLiveWithDeliveryDelay") ) {
        //     testFailed = true;
        // }

        jmsConsumer.close();
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testTimeToLiveWithDeliveryDelay failed");
        }
    }

    public void testReceiveBodyObjectMsgWithDD(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContext = jmsQCFBindings.createContext();
        emptyQueue(jmsQCFBindings, jmsQueue);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);

        String abc = new String("testReceiveBodyObjectMsg_B_SecOff");
        ObjectMessage message = jmsContext.createObjectMessage();
        message.setObject(abc);
        jmsContext.createProducer().setDeliveryDelay(1000).send(jmsQueue, message);

        Object body = jmsConsumer.receiveBody(Serializable.class, 30000);

        if ( (body == null) || !body.equals(abc) ) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testReceiveBodyObjectMsg failed");
        }
    }

    public void testReceiveBodyObjectMsgWithDD_Tcp(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContext = jmsQCFTCP.createContext();
        emptyQueue(jmsQCFTCP, jmsQueue);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);
        String abc = new String("testReceiveBodyObjectMsg_TcpIp_SecOff");
        ObjectMessage message = jmsContext.createObjectMessage();
        message.setObject(abc);
        jmsContext.createProducer().setDeliveryDelay(1000).send(jmsQueue, message);

        Object body = jmsConsumer.receiveBody(Serializable.class, 30000);

        if ( (body == null) || !body.equals(abc) ) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testReceiveBodyObjectMsg_Tcp failed");
        }
    }

    public void testCloseConsumer(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContext = jmsQCFBindings.createContext();

        try {
            JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);
            jmsConsumer.close();

            jmsConsumer.receive();

            testFailed = true;

        } catch ( JMSRuntimeException ex ) {
            // expected
        }

        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testCloseConsumer failed");
        }
    }

    public void testCloseConsumer_Tcp(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContext = jmsQCFTCP.createContext();

        try {
            JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);
            jmsConsumer.close();

            jmsConsumer.receive();

            testFailed = true;

        } catch ( JMSRuntimeException ex ) {
            // expected
        }

        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testCloseConsumer_Tcp failed");
        }
    }

    public void testQueueNameNullWithDD(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContext = jmsQCFBindings.createContext();

        JMSProducer jmsProducer = jmsContext.createProducer();

        try {
            Queue queue = jmsContext.createQueue(null);
            emptyQueue(jmsQCFBindings, queue);

            jmsProducer.setDeliveryDelay(1000).send(queue,"testQueueNameNull_B");

            JMSConsumer jmsConsumer = jmsContext.createConsumer(queue);

            try {
                TextMessage m = (TextMessage) jmsConsumer.receive(30000);
                m.getText();

                testFailed = true;

            } finally {
                jmsConsumer.close();
            }

        } catch ( JMSRuntimeException e ) {
            // expected
        }

        if ( testFailed ) {
            throw new Exception("testQueueNameNull_B failed");
        }
    }

    public void testQueueNameNullWithDD_Tcp(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContext = jmsQCFTCP.createContext();

        JMSProducer jmsProducer = jmsContext.createProducer();

        try {
            Queue queue = jmsContext.createQueue(null);
            emptyQueue(jmsQCFTCP, queue);

            jmsProducer.setDeliveryDelay(1000).send(queue, "testQueueNameNull_TCP");

            JMSConsumer jmsConsumer = jmsContext.createConsumer(queue);

            try {
                TextMessage m = (TextMessage) jmsConsumer.receive(30000);
                m.getText();

                testFailed = true;

            } finally {
                jmsConsumer.close();
            }

        } catch ( JMSRuntimeException e ) {
            // expected
        }

        if ( testFailed ) {
            throw new Exception("testQueueNameNull_TcpIp failed");
        }
    }

    public void testTopicNameNullWithDD(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContext = jmsTCFBindings.createContext();

        try {
            Topic topic = jmsContext.createTopic(null);
            JMSConsumer jmsConsumer = jmsContext.createConsumer(topic);

            try {
                jmsContext.createProducer().setDeliveryDelay(1000).send(topic, "testTopicNameNull_B");

                TextMessage recMsg = (TextMessage) jmsConsumer.receive(30000);
                recMsg.getText();

                testFailed = true;

            } finally {
                jmsConsumer.close();
            }

        } catch ( JMSRuntimeException ex ) {
            // expected
        }

        jmsContext.close();

        if ( testFailed ) {
            throw new Exception( "testTopicNameNull_B failed");
        }
    }

    public void testTopicNameNullWithDD_Tcp(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContext = jmsTCFTCP.createContext();

        try {
            Topic topic = jmsContext.createTopic(null);

            JMSConsumer jmsConsumer = jmsContext.createConsumer(topic);

            try {
                jmsContext.createProducer()
                    .setDeliveryDelay(1000)
                    .send(topic, "testTopicNameNull_TcpIP");

                TextMessage recMsg = (TextMessage) jmsConsumer.receive(30000);
                recMsg.getText();

                testFailed = true;

            } finally {
                jmsConsumer.close();
            }

        } catch ( JMSRuntimeException ex ) {
            // expected
        }

        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testTopicNameNull_TcpIp failed");
        }
    }

    public void testAckOnClosedContextWithDD(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContext = jmsQCFBindings.createContext();
        emptyQueue(jmsQCFBindings, jmsQueue);

        JMSProducer jmsProducer = jmsContext.createProducer();
        jmsProducer.
            setDeliveryDelay(1000).
            send(jmsQueue, "testAckOnClosedContext_B_SecOff");

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);

        TextMessage recMsg = (TextMessage) jmsConsumer.receive(30000);
        jmsContext.close();

        try {
            jmsContext.acknowledge();
            testFailed = true;

        } catch ( IllegalStateRuntimeException ex ) {
            ex.printStackTrace();
        }

        jmsConsumer.close();
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testAckOnClosedContextWithDD failed");
        }
    }

    public void testAckOnClosedContextWithDD_Tcp(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContext = jmsQCFTCP.createContext();
        emptyQueue(jmsQCFTCP, jmsQueue);

        JMSProducer jmsProducer = jmsContext.createProducer();
        jmsProducer.setDeliveryDelay(1000).send(jmsQueue, "testAckOnClosedContext_B_SecOff");

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);
        TextMessage recMsg = (TextMessage) jmsConsumer.receive(30000);

        jmsContext.close();

        try {
            jmsContext.acknowledge();
            testFailed = true;
        } catch ( IllegalStateRuntimeException ex ) {
            // expected
        }

        jmsConsumer.close();
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testAckOnClosedContextWithDD_Tcp failed");
        }
    }

    public void testCreateConsumerWithMsgSelectorWithDD(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContext = jmsQCFBindings.createContext();
        emptyQueue(jmsQCFBindings, jmsQueue);

        JMSProducer jmsProducer = jmsContext.createProducer();

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue, "Team = 'SIB'");

        TextMessage message = jmsContext .createTextMessage("testCreateConsumerWithMsgSelector_B_SecOff");
        message.setStringProperty("Team", "SIB");
        jmsProducer.setDeliveryDelay(1000).send(jmsQueue, message);

        TextMessage recMsg = (TextMessage) jmsConsumer.receive(30000);
        recMsg.getText();
        recMsg.getStringProperty("Team");

        if ( ((recMsg == null) ||
              (recMsg.getText() == null) ||
              !recMsg.getText().equals("testCreateConsumerWithMsgSelector_B_SecOff")) ||
             ((recMsg.getStringProperty("Team") == null) ||
              !recMsg.getStringProperty("Team").equals("SIB")) ) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testCreateConsumerWithMsgSelectorWithDD failed");
        }
    }

    public void testCreateConsumerWithMsgSelectorWithDD_Tcp(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContext = jmsQCFTCP.createContext();
        emptyQueue(jmsQCFTCP, jmsQueue);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue, "Team = 'SIB'");

        JMSProducer jmsProducer = jmsContext.createProducer();
        TextMessage message = jmsContext.createTextMessage("testCreateConsumerWithMsgSelector_TcpIp_SecOff");
        message.setStringProperty("Team", "SIB");
        jmsProducer.setDeliveryDelay(1000).send(jmsQueue, message);

        TextMessage recMsg = (TextMessage) jmsConsumer.receive(30000);
        recMsg.getText();
        recMsg.getStringProperty("Team");

        if ( ((recMsg == null) ||
              (recMsg.getText() == null) ||
              !recMsg.getText().equals("testCreateConsumerWithMsgSelector_TcpIp_SecOff")) ||
             ((recMsg.getStringProperty("Team") == null) ||
              !recMsg.getStringProperty("Team").equals("SIB")) ) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testCreateConsumerWithMsgSelectorWithDD_Tcp failed");
        }
    }

    public void testCreateConsumerWithMsgSelectorWithDDTopic(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContext = jmsTCFBindings.createContext();

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsTopic, "Team = 'SIB'");

        JMSProducer jmsProducer = jmsContext.createProducer();
        TextMessage message = jmsContext.createTextMessage("testCreateConsumerWithMsgSelectorTopic_B_SecOff");
        message.setStringProperty("Team", "SIB");
        jmsProducer.setDeliveryDelay(1000).send(jmsTopic, message);

        TextMessage recMsg = (TextMessage) jmsConsumer.receive(30000);
        recMsg.getText();
        recMsg.getStringProperty("Team");

        if ( ((recMsg == null) ||
              (recMsg.getText() == null) ||
              !recMsg.getText().equals("testCreateConsumerWithMsgSelectorTopic_B_SecOff")) ||
             ((recMsg.getStringProperty("Team") == null) ||
              !recMsg.getStringProperty("Team").equals("SIB")) ) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testCreateConsumerWithMsgSelectorWithDDTopic failed");
        }
    }

    public void testCreateConsumerWithMsgSelectorWithDDTopic_Tcp(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContext = jmsTCFTCP.createContext();

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsTopic, "Team = 'SIB'");

        JMSProducer jmsProducer = jmsContext.createProducer();
        TextMessage message = jmsContext.createTextMessage("testCreateConsumerWithMsgSelectorTopic_TcpIp_SecOff");
        message.setStringProperty("Team", "SIB");
        jmsProducer.setDeliveryDelay(1000).send(jmsTopic, message);

        TextMessage recMsg = (TextMessage) jmsConsumer.receive(30000);
        recMsg.getText();
        recMsg.getStringProperty("Team");

        if ( ((recMsg == null) ||
              (recMsg.getText() == null) ||
              !recMsg.getText().equals("testCreateConsumerWithMsgSelectorTopic_TcpIp_SecOff")) ||
             ((recMsg.getStringProperty("Team") == null) ||
              !recMsg.getStringProperty("Team").equals("SIB")) ) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testCreateConsumerWithMsgSelectorWithDDTopic_Tcp failed");
        }
    }

    public void testJMSPriorityWithDD(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContext = jmsQCFBindings.createContext();
        emptyQueue(jmsQCFBindings, jmsQueue);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);

        Message message = jmsContext.createMessage();
        message.setJMSPriority(9);
        jmsContext.createProducer()
            .setPriority(1)
            .setDeliveryDelay(1000)
            .send(jmsQueue, message);

        int pri = jmsConsumer.receive(30000).getJMSPriority();
        if ( pri != 1 ) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testJMSPriorityWithDD failed");
        }
    }

    public void testJMSPriorityWithDD_Tcp(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContext = jmsQCFTCP.createContext();
        emptyQueue(jmsQCFTCP, jmsQueue);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);

        Message message = jmsContext.createMessage();
        message.setJMSPriority(9);

        jmsContext.createProducer()
            .setPriority(1)
            .setDeliveryDelay(1000)
            .send(jmsQueue, message);

        int pri = jmsConsumer.receive(30000).getJMSPriority();
        if ( pri != 1 ) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testJMSPriorityWithDD_Tcp failed");
        }
    }

    public void testConnStartAuto_createContextUserSessionModeWithDD(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        String userName = "user1";
        String password = "user1pwd";
        int smode = JMSContext.AUTO_ACKNOWLEDGE;
        JMSContext jmsContext = jmsTCFBindings.createContext(userName, password, smode);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsTopic);

        TextMessage recMsg = jmsContext.createTextMessage("Hello");
        jmsContext.createProducer().setDeliveryDelay(1000).send(jmsTopic, recMsg);

        TextMessage message = (TextMessage) jmsConsumer.receive(30000);
        if ( (message == null) ||
             (message.getText() == null) ||
             !message.getText().equals("Hello") ) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testConnStartAuto_createContextUserSessionModeWithDD failed");
        }
    }

    public void testConnStartAuto_createContextUserSessionModeWithDD_Tcp(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        String userName = "user1";
        String password = "user1pwd";
        int smode = JMSContext.AUTO_ACKNOWLEDGE;
        JMSContext jmsContext = jmsTCFTCP.createContext(userName, password, smode);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsTopic);

        TextMessage recMsg = jmsContext.createTextMessage("Hello");
        jmsContext.createProducer()
            .setDeliveryDelay(1000)
            .send(jmsTopic, recMsg);

        TextMessage message = (TextMessage) jmsConsumer.receive(30000);
        if ( (message == null) ||
             (message.getText() == null) ||
             !message.getText().equals("Hello") ) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testConnStartAuto_createContextUserSessionModeWithDD_Tcp failed");
        }
    }

    public void testcreateBrowserWithDD(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContext = jmsQCFBindings.createContext();
        emptyQueue(jmsQCFBindings, jmsQueue2);

        jmsContext.createProducer()
            .setDeliveryDelay(1000)
            .send(jmsQueue2, "Tester");

        Thread.sleep(2000);

        QueueBrowser queueBrowser = jmsContext.createBrowser(jmsQueue2);
        int numMsgs = getMessageCount(queueBrowser);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue2);
        jmsConsumer.receive(30000);

        if ( numMsgs != 1 ) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testcreateBrowserWithDD failed");
        }
    }

    public void testcreateBrowserWithDD_Tcp(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContext = jmsQCFTCP.createContext();
        emptyQueue(jmsQCFTCP, jmsQueue2);

        jmsContext.createProducer()
            .setDeliveryDelay(1000)
            .send(jmsQueue2, "Tester");

        Thread.sleep(2000);

        QueueBrowser queueBrowser = jmsContext.createBrowser(jmsQueue2);

        int numMsgs = getMessageCount(queueBrowser);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue2);
        jmsConsumer.receive(30000);

        if ( numMsgs != 1 ) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testcreateBrowserWithDD_Tcp failed");
        }
    }

    public void testInitialJMSXDeliveryCountWithDD(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContext = jmsQCFBindings.createContext();
        emptyQueue(jmsQCFBindings, jmsQueue);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);

        JMSProducer jmsProducer = jmsContext.createProducer();
        jmsProducer.setDeliveryDelay(1000).send(jmsQueue, "testInitialJMSXDeliveryCount_B_SecOff");

        TextMessage recMsg = (TextMessage) jmsConsumer.receive(30000);
        recMsg.getText();

        if ( recMsg.getIntProperty("JMSXDeliveryCount") != 1 ) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testInitialJMSXDeliveryCountWithDD failed");
        }
    }

    public void testInitialJMSXDeliveryCountWithDD_Tcp(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContext = jmsQCFTCP.createContext();
        emptyQueue(jmsQCFTCP, jmsQueue);

        JMSProducer jmsProducer = jmsContext.createProducer();

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);

        jmsProducer
            .setDeliveryDelay(1000)
            .send(jmsQueue, "testInitialJMSXDeliveryCount_TcpIp_SecOff");

        TextMessage recMsg = (TextMessage) jmsConsumer.receive(30000);

        if ( recMsg.getIntProperty("JMSXDeliveryCount") != 1 ) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testInitialJMSXDeliveryCountWithDD_Tcp failed");
        }
    }

    public void testJMSProducerSendTextMessage_EmptyMessageWithDD_Topic(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContext = jmsTCFBindings.createContext();

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsTopic);

        JMSProducer jmsProducer = jmsContext.createProducer();
        jmsProducer.setDeliveryDelay(1000).send(jmsTopic, "");

        String recvdMessage = jmsConsumer.receive(30000).getBody(String.class);

        if ( !recvdMessage.equals("") ) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testJMSProducerSendTextMessage_EmptyMessageWithDD_Topic failed");
        }
    }

    public void testJMSProducerSendTextMessage_EmptyMessageWithDD_Topic_Tcp(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContext = jmsTCFTCP.createContext();

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsTopic);

        JMSProducer jmsProducer = jmsContext.createProducer();
        jmsProducer.setDeliveryDelay(1000).send(jmsTopic, "");

        String recvdMessage = jmsConsumer.receive(30000).getBody(String.class);

        if ( !recvdMessage.equals("") ) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testJMSProducerSendTextMessage_EmptyMessage_Topic_Tcp failed");
        }
    }

    public void testClearProperties_NotsetWithDD(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContext = jmsQCFBindings.createContext();
        JMSProducer jmsProducer = jmsContext.createProducer();

        try {
            jmsProducer.clearProperties();
        } catch ( Exception ex ) {
            ex.printStackTrace();
            testFailed = true;
        }

        jmsProducer.setProperty("Name", "Tester").setDeliveryDelay(1000);
        jmsProducer.setProperty("ObjectType", new Integer(1414));

        jmsProducer.clearProperties();

        try {
            jmsProducer.clearProperties();
        } catch ( Exception ex ) {
            ex.printStackTrace();
            testFailed = true;
        }

        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testClearProperties_NotsetWithDD failed");
        }
    }

    public void testClearProperties_NotsetWithDD_Tcp(
            HttpServletRequest request, HttpServletResponse response)
            throws Exception {

        boolean testFailed = false;

        JMSContext jmsContext = jmsQCFTCP.createContext();
        JMSProducer jmsProducer = jmsContext.createProducer();

        try {
            jmsProducer.clearProperties();
        } catch ( Exception ex ) {
            ex.printStackTrace();
            testFailed = true;
        }

        jmsProducer.setProperty("Name", "Tester").setDeliveryDelay(1000);
        jmsProducer.setProperty("ObjectType", new Integer(1414));

        jmsProducer.clearProperties();

        try {
            jmsProducer.clearProperties();
        } catch ( Exception ex ) {
            ex.printStackTrace();
            testFailed = true;
        }

        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testClearProperties_NotsetWithDD_Tcp failed");
        }
    }

    public void testStartJMSContextWithDD(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContext = jmsQCFBindings.createContext();
        emptyQueue(jmsQCFBindings, jmsQueue);
        jmsContext.setAutoStart(false);

        String outbound = "Hello World";

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);

        JMSProducer jmsProducer = jmsContext.createProducer();
        jmsProducer.setDeliveryDelay(1000).send(jmsQueue, outbound);

        jmsContext.start();

        TextMessage receiveMsg = (TextMessage) jmsConsumer.receive(30000);

        String inbound = receiveMsg.getText();

        if ( !outbound.equals(inbound) ) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if ( testFailed == true ) {
            throw new Exception("testStartJMSContextWithDD failed");
        }
    }

    public void testStartJMSContextWithDD_Tcp(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContext = jmsQCFTCP.createContext();
        emptyQueue(jmsQCFTCP, jmsQueue);

        jmsContext.setAutoStart(false);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);

        String outbound = "Hello World";
        JMSProducer jmsProducer = jmsContext.createProducer();
        jmsProducer.setDeliveryDelay(1000).send(jmsQueue, outbound);

        jmsContext.start();

        TextMessage receiveMsg = (TextMessage) jmsConsumer.receive(30000);

        String inbound = receiveMsg.getText();

        if ( !outbound.equals(inbound) ) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testStartJMSContextWithDD_Tcp failed");
        }
    }

    public void testPTPTemporaryQueueWithDD(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContext = jmsQCFBindings.createContext();
        emptyQueue(jmsQCFBindings, jmsQueue);

        TemporaryQueue tempQ = jmsContext.createTemporaryQueue();

        JMSProducer jmsProducer = jmsContext.createProducer();

        JMSConsumer jmsConsumer = jmsContext.createConsumer(tempQ);

        jmsProducer.setDeliveryDelay(1000).send(tempQ, "testPTPTemporaryQueueWithDD");

        TextMessage recMessage = (TextMessage) jmsConsumer.receive(30000);

        if ( (recMessage.getText() == null) ||
             !recMessage.getText().equals("testPTPTemporaryQueueWithDD") ) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testPTPTemporaryQueueWithDD failed");
        }
    }

    public void testPTPTemporaryQueueWithDD_Tcp(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContext = jmsQCFTCP.createContext();
        emptyQueue(jmsQCFTCP, jmsQueue);

        TemporaryQueue tempQ = jmsContext.createTemporaryQueue();

        JMSProducer jmsProducer = jmsContext.createProducer();

        JMSConsumer jmsConsumer = jmsContext.createConsumer(tempQ);

        jmsProducer.setDeliveryDelay(1000).send(tempQ, "testPTPTemporaryQueueWithDD_Tcp");

        TextMessage recMessage = (TextMessage) jmsConsumer.receive(30000);

        if ( (recMessage.getText() == null) ||
             !recMessage.getText().equalsIgnoreCase("testPTPTemporaryQueueWithDD_Tcp") ) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testPTPTemporaryQueueWithDD_Tcp failed");
        }
    }

    public void testTemporaryTopicPubSubWithDD(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContext = jmsTCFBindings.createContext();

        TemporaryTopic tempT = jmsContext.createTemporaryTopic();

        JMSProducer jmsProducer = jmsContext.createProducer();

        JMSConsumer jmsConsumer = jmsContext.createConsumer(tempT);

        jmsProducer.setDeliveryDelay(1000).send(tempT, "testTemporaryTopicPubSubWithDD");

        TextMessage recMessage = (TextMessage) jmsConsumer.receive(30000);

        if ( (recMessage.getText() == null) ||
             !recMessage.getText().equals("testTemporaryTopicPubSubWithDD") ) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testTemporaryTopicPubSubWithDD failed");
        }
    }

    public void testTemporaryTopicPubSubWithDD_Tcp(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContext = jmsTCFTCP.createContext();

        TemporaryTopic tempT = jmsContext.createTemporaryTopic();

        JMSProducer jmsProducer = jmsContext.createProducer();

        JMSConsumer jmsConsumer = jmsContext.createConsumer(tempT);

        jmsProducer.setDeliveryDelay(1000).send(tempT, "testTemporaryTopicPubSubWithDD_Tcp");

        TextMessage recMessage = (TextMessage) jmsConsumer.receive(30000);

        if ( (recMessage.getText() == null) ||
             !recMessage.getText().equals("testTemporaryTopicPubSubWithDD_Tcp") ) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testTemporaryTopicPubSubWithDD_Tcp failed");
        }
    }

    public void testCommitLocalTransactionWithDD(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContext = null;
        JMSConsumer jmsConsumer = null;

        try {
            jmsContext = jmsQCFBindings.createContext(Session.SESSION_TRANSACTED);

            Message message = jmsContext.createTextMessage("testCommitLocalTransactionWithDD");
            emptyQueue(jmsQCFBindings, jmsQueue);

            JMSProducer jmsProducer = jmsContext.createProducer();
            jmsProducer.setDeliveryDelay(1000).send(jmsQueue,message);

            QueueBrowser qb = jmsContext.createBrowser(jmsQueue);
            Enumeration e = qb.getEnumeration();
            int numMsgs = 0;
            while ( e.hasMoreElements() ) {
                TextMessage message1 = (TextMessage) e.nextElement();
                numMsgs++;
            }

            jmsContext.commit();

            QueueBrowser qb1 = jmsContext.createBrowser(jmsQueue);
            Enumeration e1 = qb1.getEnumeration();
            int numMsgs1 = 0;
            while ( e1.hasMoreElements() ) {
                TextMessage message1 = (TextMessage) e1.nextElement();
                numMsgs1++;
            }

            jmsConsumer = jmsContext.createConsumer(jmsQueue);
            TextMessage rmsg = (TextMessage) jmsConsumer.receive(30000);

            jmsContext.commit();

        } catch ( Exception e ) {
            e.printStackTrace();
            testFailed = true;
        }

        if ( jmsConsumer != null ) {
            jmsConsumer.close();
        }
        if ( jmsContext != null ) {
            jmsContext.close();
        }

        if ( testFailed ) {
            throw new Exception("testCommitLocalTransactionWithDD failed");
        }
    }

    public void testCommitLocalTransactionWithDD_Tcp(
            HttpServletRequest request, HttpServletResponse response)
            throws Exception {

        boolean testFailed = false;

        JMSContext jmsContext = null;
        JMSConsumer jmsConsumer = null;

        try {
            jmsContext = jmsQCFTCP.createContext(Session.SESSION_TRANSACTED);
            emptyQueue(jmsQCFTCP, jmsQueue);

            Message message = jmsContext.createTextMessage("testCommitLocalTransactionWithDD_Tcp");
            JMSProducer jmsProducer = jmsContext.createProducer();
            jmsProducer.setDeliveryDelay(1000).send(jmsQueue, message);

            QueueBrowser qb = jmsContext.createBrowser(jmsQueue);
            Enumeration e = qb.getEnumeration();
            int numMsgs = 0;
            while ( e.hasMoreElements() ) {
                TextMessage message1 = (TextMessage) e.nextElement();
                numMsgs++;
            }

            jmsContext.commit();

            QueueBrowser qb1 = jmsContext.createBrowser(jmsQueue);
            Enumeration e1 = qb1.getEnumeration();
            int numMsgs1 = 0;
            while ( e1.hasMoreElements() ) {
                TextMessage message1 = (TextMessage) e1.nextElement();
                numMsgs1++;
            }

            jmsConsumer = jmsContext.createConsumer(jmsQueue);
            jmsConsumer.receive(30000);

            jmsContext.commit();

        } catch ( Exception e ) {
            e.printStackTrace();
            testFailed = true;
        }

        if ( jmsConsumer != null ) {
            jmsConsumer.close();
        }
        if ( jmsContext != null ) {
            jmsContext.close();
        }

        if ( testFailed ) {
            throw new Exception("testCommitLocalTransactionWithDD_Tcp failed");
        }

    }

    public void testCreateSharedDurableConsumer_create_B_SecOff(
            HttpServletRequest request, HttpServletResponse response)
            throws Exception {

        JMSContext jmsContextSender = jmsTCFBindings.createContext();
        JMSContext jmsContextReceiver = jmsTCFBindings.createContext();

        JMSConsumer jmsConsumer = jmsContextReceiver.createSharedDurableConsumer(jmsTopic, "SUBID");

        JMSProducer jmsProducer = jmsContextSender.createProducer();

        TextMessage tmsg = jmsContextSender.createTextMessage("This is a test message");

        jmsProducer.setDeliveryDelay(1000).send(jmsTopic, tmsg);

        jmsContextSender.close();
    }

    public void testCreateSharedDurableConsumer_consume_B_SecOff(
            HttpServletRequest request, HttpServletResponse response)
            throws Exception {

        boolean testFailed = false;

        JMSContext jmsContextReceiver = jmsTCFBindings.createContext();

        JMSConsumer jmsConsumer = jmsContextReceiver.createSharedDurableConsumer(jmsTopic, "SUBID");

        TextMessage tmsg = (TextMessage) jmsConsumer.receiveNoWait();

        if ( tmsg == null ) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContextReceiver.unsubscribe("SUBID");
        jmsContextReceiver.close();

        if ( testFailed ) {
            throw new Exception("testCreateSharedDurableExpiry_B_SecOff failed");
        }
    }

    public void testCreateSharedNonDurableConsumer_create_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContextSender = jmsTCFBindings.createContext();

        JMSContext jmsContextReceiver = jmsTCFBindings.createContext();

        JMSConsumer jmsConsumer = jmsContextReceiver.createSharedConsumer(jmsTopic, "SUBID");

        JMSProducer jmsProducer = jmsContextSender.createProducer();

        TextMessage tmsg = jmsContextSender.createTextMessage("This is a test message");

        jmsProducer.setDeliveryDelay(1000).send(jmsTopic, tmsg);

        jmsContextSender.close();
    }

    public void testCreateSharedNonDurableConsumer_consume_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContextReceiver = jmsTCFBindings.createContext();

        JMSConsumer jmsConsumer = jmsContextReceiver.createSharedConsumer(jmsTopic, "SUBID");

        TextMessage tmsg = (TextMessage) jmsConsumer.receiveNoWait();
        if ( tmsg != null ) {
            testFailed = true;
        }

        jmsConsumer.close();
        // jmsContextReceiver.unsubscribe("SUBID");
        jmsContextReceiver.close();
    }

    public void testCreateUnSharedDurableConsumer_create(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContextSender = jmsTCFBindings.createContext();
        JMSContext jmsContextReceiver = jmsTCFBindings.createContext();

        JMSConsumer jmsConsumer = jmsContextReceiver.createDurableConsumer(jmsTopic, "SUBID");

        JMSProducer jmsProducer = jmsContextSender.createProducer();

        TextMessage tmsg = jmsContextSender.createTextMessage("This is a test message");

        jmsProducer.setDeliveryDelay(1000).send(jmsTopic, tmsg);

        jmsConsumer.close();
        jmsContextReceiver.close();
        jmsContextSender.close();
    }

    public void testCreateUnSharedDurableConsumer_consume(
        HttpServletRequest request, HttpServletResponse response)
        throws Exception {

        boolean testFailed = false;

        JMSContext jmsContextReceiver = jmsTCFBindings.createContext();

        JMSConsumer jmsConsumer = jmsContextReceiver.createDurableConsumer(jmsTopic, "SUBID");

        TextMessage tmsg = (TextMessage) jmsConsumer.receiveNoWait();

        if ( tmsg == null ) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContextReceiver.unsubscribe("SUBID");
        jmsContextReceiver.close();

        if ( testFailed ) {
            throw new Exception("testCreateUnSharedDurableConsumer_consume failed");
        }
    }



    public void testDeliveryMultipleMsgsClassicApi(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        QueueConnection con = jmsQCFBindings.createQueueConnection();
        con.start();

        QueueSession sessionSender = con.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
        emptyQueue(jmsQCFBindings, jmsQueue1);

        QueueReceiver rec = sessionSender.createReceiver(jmsQueue1);

        // QueueReceiver rec1 = sessionSender.createReceiver(queue1);
        //
        // This sets the delivery delay of all messages sent using that jmsProducer.
        // In classic API we can create sender for a single queue.

        QueueSender send = sessionSender.createSender(jmsQueue1);
        send.setDeliveryDelay(defaultTestDeliveryDelay);

        TextMessage sendMsg1 =
            sessionSender.createTextMessage("testDeliveryMultipleMsgsClassicApi1");
        sendAndCheckDeliveryTime(send, jmsQueue1, sendMsg1);
        TextMessage recMsg1 = (TextMessage) rec.receiveNoWait();

        TextMessage sendMsg2 =
            sessionSender.createTextMessage("testDeliveryMultipleMsgsClassicApi2");
        sendAndCheckDeliveryTime(send, jmsQueue1, sendMsg2);
        TextMessage recMsg2 = (TextMessage) rec.receiveNoWait();

        if ( (recMsg1 != null) || (recMsg2 != null) ) {
            testFailed = true;
        }

        recMsg1 = (TextMessage) rec.receive(30000);
        recMsg2 = (TextMessage) rec.receive(30000);

        if ( ((recMsg1 == null) ||
              (recMsg1.getText() == null) ||
              !recMsg1.getText().equals("testDeliveryMultipleMsgsClassicApi1")) ||
             ((recMsg2 == null) ||
              (recMsg2.getText() == null) ||
              !recMsg2.getText().equals("testDeliveryMultipleMsgsClassicApi2")) ) {
            testFailed = true;
        }

        send.close();
        con.close();

        if ( testFailed ) {
            throw new Exception("testDeliveryMultipleMsgsClassicApi failed");
        }
    }

    public void testDeliveryMultipleMsgsClassicApi_Tcp(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        QueueConnection con = jmsQCFTCP.createQueueConnection();
        con.start();

        QueueSession sessionSender = con.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
        emptyQueue(jmsQCFTCP, jmsQueue1);

        QueueReceiver rec = sessionSender.createReceiver(jmsQueue1);

        // QueueReceiver rec1 = sessionSender.createReceiver(queue1);
        //
        // This sets the delivery delay of all messages sent using that jmsProducer.
        // In classic API we can create sender for a single queue.

        QueueSender send = sessionSender.createSender(jmsQueue1);
        send.setDeliveryDelay(defaultTestDeliveryDelay);

        TextMessage sendMsg1 =
            sessionSender.createTextMessage("testDeliveryMultipleMsgsClassicApi_Tcp1");
        sendAndCheckDeliveryTime(send, jmsQueue1, sendMsg1);
        TextMessage recMsg1 = (TextMessage) rec.receiveNoWait();

        TextMessage sendMsg2 =
            sessionSender.createTextMessage("testDeliveryMultipleMsgsClassicApi_Tcp2");
        sendAndCheckDeliveryTime(send, jmsQueue1, sendMsg2);
        TextMessage recMsg2 = (TextMessage) rec.receiveNoWait();

        if ( (recMsg1 != null) || (recMsg2 != null) ) {
            testFailed = true;
        }

        recMsg1 = (TextMessage) rec.receive(30000);
        recMsg2 = (TextMessage) rec.receive(30000);

        if ( ((recMsg1 == null) ||
              (recMsg1.getText() == null) ||
              !recMsg1.getText().equals("testDeliveryMultipleMsgsClassicApi_Tcp1")) ||
             ((recMsg2 == null) ||
              (recMsg2.getText() == null) ||
              !recMsg2.getText().equals("testDeliveryMultipleMsgsClassicApi_Tcp2")) ) {
            testFailed = true;
        }

        send.close();
        con.close();

        if ( testFailed ) {
            throw new Exception("testDeliveryMultipleMsgsClassicApi_Tcp failed");
        }
    }

    public void testDeliveryMultipleMsgsTopicClassicApi(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        TopicConnection con = jmsTCFBindings.createTopicConnection();
        con.start();

        TopicSession sessionSender = con.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);

        TopicSubscriber sub = sessionSender.createSubscriber(jmsTopic);

        TopicPublisher send = sessionSender.createPublisher(jmsTopic);
        send.setDeliveryDelay(defaultTestDeliveryDelay);

        TextMessage sendMsg1 =
            sessionSender.createTextMessage("testDeliveryMultipleMsgsTopicClassicApi1");
        sendAndCheckDeliveryTime(send, jmsTopic, sendMsg1);
        TextMessage recMsg1 = (TextMessage) sub.receiveNoWait();

        TextMessage sendMsg2 =
            sessionSender.createTextMessage("testDeliveryMultipleMsgsTopicClassicApi2");
        sendAndCheckDeliveryTime(send, jmsTopic, sendMsg2);
        TextMessage recMsg2 = (TextMessage) sub.receiveNoWait();

        if ( (recMsg1 != null) || (recMsg2 != null) ) {
            testFailed = true;
        }

        recMsg1 = (TextMessage) sub.receive(30000);
        recMsg2 = (TextMessage) sub.receive(30000);

        if ( ((recMsg1 == null) ||
              (recMsg1.getText() == null) ||
              !recMsg1.getText().equals("testDeliveryMultipleMsgsTopicClassicApi1")) ||
             ((recMsg2 == null) ||
              (recMsg2.getText() == null) ||
              !recMsg2.getText().equals("testDeliveryMultipleMsgsTopicClassicApi2")) ) {
            testFailed = true;
        }

        sub.close();
        con.close();

        if ( testFailed ) {
            throw new Exception("testDeliveryMultipleMsgsTopicClassicApi failed");
        }
    }

    public void testDeliveryMultipleMsgsTopicClassicApi_Tcp(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        TopicConnection con = jmsTCFTCP.createTopicConnection();
        con.start();

        TopicSession sessionSender =
            con.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);

        TopicSubscriber sub = sessionSender.createSubscriber(jmsTopic);

        TopicPublisher send = sessionSender.createPublisher(jmsTopic);
        send.setDeliveryDelay(defaultTestDeliveryDelay);

        TextMessage sendMsg1 =
            sessionSender.createTextMessage("testDeliveryMultipleMsgsTopicClassicApi_Tcp1");
        sendAndCheckDeliveryTime(send, jmsTopic, sendMsg1);
        TextMessage recMsg1 = (TextMessage) sub.receiveNoWait();

        TextMessage sendMsg2 =
            sessionSender.createTextMessage("testDeliveryMultipleMsgsTopicClassicApi_Tcp2");
        sendAndCheckDeliveryTime(send, jmsTopic, sendMsg2);
        TextMessage recMsg2 = (TextMessage) sub.receiveNoWait();

        if ( (recMsg1 != null) || (recMsg2 != null) ) {
            testFailed = true;
        }

        recMsg1 = (TextMessage) sub.receive(30000);
        recMsg2 = (TextMessage) sub.receive(30000);

        if ( ((recMsg1 == null) ||
              (recMsg1.getText() == null) ||
              !recMsg1.getText().equals("testDeliveryMultipleMsgsTopicClassicApi_Tcp1")) ||
             ((recMsg2 == null) ||
              (recMsg2.getText() == null) ||
              !recMsg2.getText().equals("testDeliveryMultipleMsgsTopicClassicApi_Tcp2")) ) {
            testFailed = true;
        }

        sub.close();
        con.close();

        if ( testFailed ) {
            throw new Exception("testDeliveryMultipleMsgsTopicClassicApi_Tcp failed");
        }
    }

    public void testDeliveryDelayZeroAndNegativeValuesClassicApi(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        QueueConnection con = jmsQCFBindings.createQueueConnection();
        con.start();

        QueueSession sessionSender = con.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
        emptyQueue(jmsQCFBindings, jmsQueue1);

        QueueReceiver rec = sessionSender.createReceiver(jmsQueue1);
        QueueSender send = sessionSender.createSender(jmsQueue1);

        send.send(sessionSender.createTextMessage("Zero Delivery Delay"));

        TextMessage recMsg = (TextMessage) rec.receiveNoWait();
        if ( recMsg == null ) {
            testFailed = true;
        } else {
            recMsg.getText();
        }

        try {
            send.setDeliveryDelay(-10);
            testFailed = true;
        } catch ( JMSException e ) {
            // expected
        }

        send.close();
        con.close();

        if ( testFailed ) {
            throw new Exception("testDeliveryDelayZeroAndNegativeValuesClassicApi failed");
        }
    }

    public void testDeliveryDelayZeroAndNegativeValuesClassicApi_Tcp(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        QueueConnection con = jmsQCFTCP.createQueueConnection();
        con.start();

        QueueSession sessionSender = con.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
        emptyQueue(jmsQCFTCP, jmsQueue1);

        QueueReceiver rec = sessionSender.createReceiver(jmsQueue1);

        QueueSender send = sessionSender.createSender(jmsQueue1);
        send.send(sessionSender.createTextMessage("Zero Delivery Delay"));

        TextMessage recMsg = (TextMessage) rec.receiveNoWait();
        if ( recMsg == null ) {
            testFailed = true;
        } else {
            recMsg.getText();
        }

        try {
            send.setDeliveryDelay(-10);
            testFailed = true;
        } catch ( JMSException e ) {
            // expected
        }

        send.close();
        con.close();

        if ( testFailed ) {
            throw new Exception("testDeliveryDelayZeroAndNegativeValuesClassicApi failed");
        }
    }

    public void testDeliveryDelayZeroAndNegativeValuesTopicClassicApi(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        TopicConnection con = jmsTCFBindings.createTopicConnection();
        con.start();

        TopicSession sessionSender = con.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);

        TopicSubscriber rec = sessionSender.createSubscriber(jmsTopic);

        TopicPublisher send = sessionSender.createPublisher(jmsTopic);
        send.publish(sessionSender.createTextMessage("Zero Delivery Delay"));

        TextMessage recMsg = (TextMessage) rec.receiveNoWait();
        if ( recMsg == null ) {
            testFailed = true;
        } else {
            recMsg.getText();
        }

        try {
            send.setDeliveryDelay(-10);
            testFailed = true;
        } catch ( JMSException e ) {
            // expected
        }

        if ( rec != null ) {
            rec.close();
        }
        con.close();

        if ( testFailed ) {
            throw new Exception("testDeliveryDelayZeroAndNegativeValuesClassicApi failed");
        }
    }

    public void testDeliveryDelayZeroAndNegativeValuesTopicClassicApi_Tcp(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        TopicConnection con = jmsTCFTCP.createTopicConnection();
        con.start();

        TopicSession sessionSender = con.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);

        TopicSubscriber rec = sessionSender.createSubscriber(jmsTopic);

        TopicPublisher send = sessionSender.createPublisher(jmsTopic);
        send.publish(sessionSender.createTextMessage("Zero Delivery Delay"));

        TextMessage recMsg = (TextMessage) rec.receiveNoWait();
        if ( recMsg == null ) {
            testFailed = true;
        } else {
            recMsg.getText();
        }

        try {
            send.setDeliveryDelay(-10);
            testFailed = true;
        } catch ( JMSException e ) {
            // expected
        }

        if ( rec != null ) {
            rec.close();
        }
        con.close();

        if ( testFailed ) {
            throw new Exception("testDeliveryDelayZeroAndNegativeValuesClassicApi failed");
        }
    }

    public void testSettingMultiplePropertiesClassicApi(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        QueueConnection con = jmsQCFBindings.createQueueConnection();
        con.start();

        QueueSession sessionSender = con.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
        emptyQueue(jmsQCFBindings, jmsQueue1);

        QueueReceiver rec = sessionSender.createReceiver(jmsQueue1);

        QueueSender send = sessionSender.createSender(jmsQueue1);
        send.setDeliveryDelay(1000);
        send.setDisableMessageID(true);
        send.send( sessionSender.createTextMessage("testSettingMultiplePropertiesClassicApi") );

        TextMessage recMsg = (TextMessage) rec.receive(30000);

        if ( ((recMsg == null) ||
              (recMsg.getText() == null) ||
              !recMsg.getText().equals("testSettingMultiplePropertiesClassicApi")) ||
             (recMsg.getJMSMessageID() != null) ) {
            testFailed = true;
        }

        send.close();
        con.close();

        if ( testFailed ) {
            throw new Exception("testSettingMultiplePropertiesClassicApi failed");
        }
    }

    public void testSettingMultiplePropertiesClassicApi_Tcp(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        QueueConnection con = jmsQCFTCP.createQueueConnection();
        con.start();

        QueueSession sessionSender = con.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
        emptyQueue(jmsQCFTCP, jmsQueue1);

        QueueReceiver rec = sessionSender.createReceiver(jmsQueue1);
        QueueSender send = sessionSender.createSender(jmsQueue1);
        send.setDeliveryDelay(1000);
        send.setDisableMessageID(true);
        send.send( sessionSender.createTextMessage("testSettingMultiplePropertiesClassicApi_Tcp") );

        TextMessage recMsg = (TextMessage) rec.receive(30000);

        if ( ((recMsg == null) ||
              (recMsg.getText() == null) ||
              !recMsg.getText().equals("testSettingMultiplePropertiesClassicApi_Tcp")) ||
             (recMsg.getJMSMessageID() != null) ) {
            testFailed = true;
        }

        send.close();
        con.close();

        if ( testFailed ) {
            throw new Exception("testSettingMultiplePropertiesClassicApi failed");
        }
    }

    public void testSettingMultiplePropertiesTopicClassicApi(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        TopicConnection con = jmsTCFBindings.createTopicConnection();
        con.start();

        TopicSession sessionSender = con.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);

        TopicSubscriber sub = sessionSender.createSubscriber(jmsTopic);

        TopicPublisher send = sessionSender.createPublisher(jmsTopic);
        send.setDeliveryDelay(1000);
        send.setDisableMessageID(true);

        send.publish( sessionSender.createTextMessage("testSettingMultiplePropertiesTopicClassicApi") );

        TextMessage recMsg = (TextMessage) sub.receive(30000);

        if ( ((recMsg == null) ||
              (recMsg.getText() == null) ||
              !recMsg.getText().equals("testSettingMultiplePropertiesTopicClassicApi")) ||
             (recMsg.getJMSMessageID() != null) ) {
            testFailed = true;
        }

        if ( sub != null ) {
            sub.close();
        }
        con.close();

        if ( testFailed ) {
            throw new Exception("testSettingMultiplePropertiesTopicClassicApi failed");
        }
    }

    public void testSettingMultiplePropertiesTopicClassicApi_Tcp(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        TopicConnection con = jmsTCFTCP.createTopicConnection();
        con.start();

        TopicSession sessionSender = con.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);

        TopicSubscriber sub = sessionSender.createSubscriber(jmsTopic);

        TopicPublisher send = sessionSender.createPublisher(jmsTopic);
        send.setDeliveryDelay(1000);
        send.setDisableMessageID(true);

        send.publish( sessionSender.createTextMessage("testSettingMultiplePropertiesTopicClassicApi_Tcp") );

        TextMessage recMsg = (TextMessage) sub.receive(30000);

        if ( ((recMsg == null) ||
              (recMsg.getText() == null) ||
              !recMsg.getText().equals("testSettingMultiplePropertiesTopicClassicApi_Tcp")) ||
             (recMsg.getJMSMessageID() != null) ) {
            testFailed = true;
        }

        if ( sub != null ) {
            sub.close();
        }
        con.close();

        if ( testFailed ) {
            throw new Exception("testSettingMultiplePropertiesTopicClassicApi failed");
        }
    }

    public void testTransactedSendClassicApi_B(HttpServletRequest request, HttpServletResponse response) throws JMSException, TestException, InterruptedException {
        testTransactedSendClassicApi(jmsQCFBindings);
    }

    public void testTransactedSendClassicApi_Tcp(HttpServletRequest request, HttpServletResponse response) throws JMSException, TestException, InterruptedException {
        testTransactedSendClassicApi(jmsQCFTCP);
    }
    
    /**
     * For transacted sends, the delay time starts when the client sends the message, not when the transaction is committed.
     * 
     * @see https://docs.oracle.com/javaee/7/api/index.html?javax/jms/MessageProducer.html
     */
    public void testTransactedSendClassicApi(QueueConnectionFactory queueConnectionFactory) throws JMSException, TestException, InterruptedException {
        
        emptyQueue(queueConnectionFactory, jmsQueue1);
        
        try ( QueueConnection queueConnection = queueConnectionFactory.createQueueConnection() ){
            queueConnection.start();

            QueueSession queueSession = queueConnection.createQueueSession(true, Session.AUTO_ACKNOWLEDGE);
            
            QueueReceiver queueReceiver = queueSession.createReceiver(jmsQueue1);
            QueueSender queueSender = queueSession.createSender(jmsQueue1);
            queueSender.setDeliveryDelay(defaultTestDeliveryDelay);
            
            TextMessage sentMessage = queueSession.createTextMessage(methodName() + " at " + timeStamp());
            long beforeSend = System.currentTimeMillis();
            queueSender.send(sentMessage);
            long afterSend = System.currentTimeMillis();
            if (afterSend - beforeSend > defaultTestDeliveryDelay)
                throw new TestException("Test Infrastructure running too slowly to meangfully test delivery delay beforeSend:" + beforeSend + " afterSend:" + afterSend
                        + " deliveryDelay:" + defaultTestDeliveryDelay);
            
            final long commitDelay = 1000;
            Thread.sleep(commitDelay);
            queueSession.commit();

            TextMessage receivedMessage = (TextMessage) queueReceiver.receive(30000);
            long afterReceive = System.currentTimeMillis();
            queueSession.commit();
            if (receivedMessage == null)
                throw new TestException("No message received, sentMessage:" + sentMessage);
            
            long receiveLatency = afterReceive - receivedMessage.getJMSDeliveryTime();
            if (receiveLatency < 0 )
                throw new TestException("JMSDeliveryTime too soon, afterReceive:" + afterReceive + " JMSDeliveryTime:" + receivedMessage.getJMSDeliveryTime()
                        + "\nreceivedMessage:" + receivedMessage);
            
            if (!receivedMessage.getText().equals(sentMessage.getBody(String.class)))
                throw new TestException("Wrong message received:" + receivedMessage + " sent:" + sentMessage);
            if (afterReceive - beforeSend < defaultTestDeliveryDelay)
                throw new TestException("Message received to soon, afterReceive:" + afterReceive + " beforeSend:" + beforeSend + " deliveryDelay:" + defaultTestDeliveryDelay
                        + "\nreceivedMessage:" + receivedMessage);
            
        }
    }

    public void testTransactedSendTopicClassicApi_B(HttpServletRequest request, HttpServletResponse response) throws JMSException, TestException, InterruptedException {
        testTransactedSendTopicClassicApi(jmsTCFBindings);
    }
    
    public void testTransactedSendTopicClassicApi_Tcp(HttpServletRequest request, HttpServletResponse response)  throws JMSException, TestException, InterruptedException {
        testTransactedSendTopicClassicApi(jmsTCFTCP);
    }
    
    /**
     * For transacted sends, the delay time starts when the client sends the message, not when the transaction is committed.
     * 
     * @see https://docs.oracle.com/javaee/7/api/index.html?javax/jms/MessageProducer.html
     */
    private void testTransactedSendTopicClassicApi(TopicConnectionFactory topicConnectionFactory) throws JMSException, TestException, InterruptedException {

        try (TopicConnection topicConnection = topicConnectionFactory.createTopicConnection()) {
            topicConnection.start();

            TopicSession topicSession = topicConnection.createTopicSession(true, Session.AUTO_ACKNOWLEDGE);

            TopicSubscriber topicSubscriber = topicSession.createSubscriber(jmsTopic);
            TopicPublisher topicPublisher = topicSession.createPublisher(jmsTopic);
            topicPublisher.setDeliveryDelay(defaultTestDeliveryDelay);
            
            TextMessage sentMessage = topicSession.createTextMessage(methodName() + " at " + timeStamp());
            long beforePublish = System.currentTimeMillis();
            topicPublisher.publish(sentMessage);
            long afterPublish = System.currentTimeMillis();
            if (afterPublish - beforePublish > defaultTestDeliveryDelay)
                throw new TestException("Test Infrastructure running too slowly to meangfully test delivery delay beforePublish:" + beforePublish + " afterPublish:" + afterPublish
                        + " deliveryDelay:" + defaultTestDeliveryDelay);
            
            final long commitDelay = 1000;
            Thread.sleep(commitDelay);
            topicSession.commit();

            TextMessage receivedMessage = (TextMessage) topicSubscriber.receive(30000);
            long afterReceive = System.currentTimeMillis();
            topicSession.commit();
            if (receivedMessage == null)
                throw new TestException("No message received, sentMessage:" + sentMessage);
            
            // For PubSub messages in Sib, the delivery delay time starts when the message is committed, later than what the JMS spec says. 
            // For point to point messages, the delivery delay starts when the message is sent.
            // JMSDeliveryTime is computed from the time the message is sent, even though a published message is actually received
            // after deliverydelay+commitDelay. The JMS specification says the earliest time a message can be received is after deliveryDelay
            // so this is within the specification. 
            
            long receiveLatency = afterReceive - receivedMessage.getJMSDeliveryTime();
            if (receiveLatency < 0 )
                throw new TestException("JMSDeliveryTime too soon, afterReceive:" + afterReceive + " JMSDeliveryTime:" + receivedMessage.getJMSDeliveryTime()
                        + "\nreceivedMessage:" + receivedMessage);
            
            if (!receivedMessage.getText().equals(sentMessage.getBody(String.class)))
                throw new TestException("Wrong message received:" + receivedMessage + " sent:" + sentMessage);
            if (afterReceive - beforePublish < defaultTestDeliveryDelay)
                throw new TestException("Message received to soon, afterReceive:" + afterReceive + " beforePublish:" + beforePublish + " deliveryDelay:" + defaultTestDeliveryDelay
                        + "\nreceivedMessage:" + receivedMessage);
        }
    }

    private String describeTimes(long afterSend, long afterCommit, long received) {
        return
            "After send [ " + afterSend + " ]" +
            "; after commit [ " + afterCommit + " ]" +
            "; received [ " + received + " ]";
    }
   
    public void testTimingClassicApi_B(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        QueueConnection con = jmsQCFBindings.createQueueConnection();
        con.start();

        emptyQueue(jmsQCFBindings, jmsQueue1);

        QueueSession sessionSender = con.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);

        QueueReceiver rec = sessionSender.createReceiver(jmsQueue1);

        QueueSender send = sessionSender.createSender(jmsQueue1);
        send.setDeliveryDelay(1000);

        TextMessage sendMsg = sessionSender.createTextMessage("testTimingClassicApi_B");
        send.send(sendMsg);

        long jmsDeliveryTime = sendMsg.getLongProperty("JMSDeliveryTime");
        long jmsTimestamp = sendMsg.getLongProperty("JMSTimestamp");

        if ( jmsDeliveryTime != (jmsTimestamp + 1000) ) {
            testFailed = true;
        }

        TextMessage recMsg2 = (TextMessage) rec.receive(31000);
        long receive_time = System.currentTimeMillis();

        if ( (recMsg2.getText() != null) && recMsg2.getText().equals("testTimingClassicApi_B") ) {
            if ( jmsDeliveryTime > receive_time ) {
                testFailed = true;
            }
        } else {
            testFailed = true;
        }

        send.close();
        con.close();

        if ( testFailed ) {
            throw new Exception("testTimingClassicApi_B failed");
        }
    }

    public void testTimingClassicApi_Tcp(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        QueueConnection con = jmsQCFTCP.createQueueConnection();
        con.start();

        emptyQueue(jmsQCFTCP, jmsQueue1);

        QueueSession sessionSender = con.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);

        QueueReceiver rec = sessionSender.createReceiver(jmsQueue1);

        QueueSender send = sessionSender.createSender(jmsQueue1);
        send.setDeliveryDelay(1000);
        TextMessage sendMsg = sessionSender.createTextMessage("testTimingClassicApi_Tcp");
        send.send(sendMsg);

        long jmsDeliveryTime = sendMsg.getLongProperty("JMSDeliveryTime");
        long jmsTimestamp = sendMsg.getLongProperty("JMSTimestamp");

        if ( jmsDeliveryTime != (jmsTimestamp + 1000) ) {
            testFailed = true;
        }

        TextMessage recMsg2 = (TextMessage) rec.receive(31000);
        long receive_time = System.currentTimeMillis();

        if ( (recMsg2.getText() != null) && recMsg2.getText().equals("testTimingClassicApi_Tcp") ) {
            if ( jmsDeliveryTime > receive_time ) {
                testFailed = true;
            }
        } else {
            testFailed = true;
        }

        send.close();
        con.close();

        if ( testFailed ) {
            throw new Exception("testTimingClassicApi_Tcp failed");
        }
    }

    public void testTimingTopicClassicApi_B(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        TopicConnection con = jmsTCFBindings.createTopicConnection();
        con.start();

        TopicSession sessionSender = con.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);

        TopicSubscriber rec = sessionSender.createSubscriber(jmsTopic);

        TopicPublisher send = sessionSender.createPublisher(jmsTopic);
        send.setDeliveryDelay(1000);
        TextMessage sendMsg = sessionSender.createTextMessage("testTimingTopicClassicApi_B");
        send.send(sendMsg);

        long jmsDeliveryTime = sendMsg.getLongProperty("JMSDeliveryTime");
        long jmsTimestamp = sendMsg.getLongProperty("JMSTimestamp");

        if ( jmsDeliveryTime != (jmsTimestamp + 1000) ) {
            testFailed = true;
        }

        TextMessage recMsg2 = (TextMessage) rec.receive(31000);
        long receive_time = System.currentTimeMillis();

        if ( (recMsg2.getText() != null) && recMsg2.getText().equals("testTimingTopicClassicApi_B") ) {
            if ( jmsDeliveryTime > receive_time ) {
                testFailed = true;
            }
        } else {
            testFailed = true;
        }

        if ( rec != null ) {
            rec.close();
        }
        con.close();

        if ( testFailed ) {
            throw new Exception("testTimingTopicClassicApi_B failed");
        }
    }

    public void testTimingTopicClassicApi_Tcp(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        TopicConnection con = jmsTCFTCP.createTopicConnection();
        con.start();

        TopicSession sessionSender = con.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);

        TopicSubscriber rec = sessionSender.createSubscriber(jmsTopic);

        TopicPublisher send = sessionSender.createPublisher(jmsTopic);
        send.setDeliveryDelay(1000);

        TextMessage sendMsg = sessionSender.createTextMessage("testTimingTopicClassicApi_Tcp");
        send.send(sendMsg);

        long jmsDeliveryTime = sendMsg.getLongProperty("JMSDeliveryTime");
        long jmsTimestamp = sendMsg.getLongProperty("JMSTimestamp");

        if ( jmsDeliveryTime != (jmsTimestamp + 1000) ) {
            testFailed = true;
        }

        TextMessage recMsg2 = (TextMessage) rec.receive(31000);
        long receive_time = System.currentTimeMillis();

        if ( (recMsg2.getText() != null) && recMsg2.getText().equals("testTimingTopicClassicApi_Tcp") ) {
            if ( jmsDeliveryTime > receive_time ) {
                testFailed = true;
            }
        } else {
            testFailed = true;
        }

        if ( rec != null ) {
            rec.close();
        }
        con.close();

        if ( testFailed ) {
            throw new Exception("testTimingTopicClassicApi_Tcp failed");
        }
    }

    public void testGetDeliveryDelayClassicApi(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        String failureReason = null;

        QueueConnection con = jmsQCFBindings.createQueueConnection();
        con.start();

        QueueSession sessionSender = con.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
        emptyQueue(jmsQCFBindings, jmsQueue1);

        QueueSender send = sessionSender.createSender(jmsQueue1);

        long val = send.getDeliveryDelay();
        if ( val != 0 ) {
            failureReason = "Incorrect default delivery dalay [ " + val + " ] expecting [ 0 ]";
        }

        send.setDeliveryDelay(1000);
        val = send.getDeliveryDelay();
        if ( val != 1000 ) {
            failureReason = "Incorrect delivery dalay [ " + val + " ] expecting [ 1000 ]";
        }

        sessionSender.close();
        con.close();

        if ( failureReason != null ) {
            throw new Exception("testGetDeliveryDelayClassicApi failed: " + failureReason);
        }
    }

    public void testGetDeliveryDelayClassicApi_Tcp(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        QueueConnection con = jmsQCFTCP.createQueueConnection();
        con.start();

        QueueSession sessionSender = con.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
        emptyQueue(jmsQCFTCP, jmsQueue1);

        QueueSender send = sessionSender.createSender(jmsQueue1);

        long val = send.getDeliveryDelay();
        if ( val != 0 ) {
            testFailed = true;
        }

        send.setDeliveryDelay(1000);
        val = send.getDeliveryDelay();
        if ( val != 1000 ) {
            testFailed = true;
        }

        sessionSender.close();
        con.close();

        if ( testFailed ) {
            throw new Exception("testGetDeliveryDelayClassicApi_Tcp failed");
        }
    }

    public void testGetDeliveryDelayClassicApiTopic(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        TopicConnection con = jmsTCFBindings.createTopicConnection();
        con.start();

        TopicSession sessionSender = con.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);

        TopicSubscriber rec = sessionSender.createSubscriber(jmsTopic);
        TopicPublisher send = sessionSender.createPublisher(jmsTopic);

        long val = send.getDeliveryDelay();
        if ( val != 0 ) {
            testFailed = true;
        }

        send.setDeliveryDelay(1000);
        val = send.getDeliveryDelay();
        if ( val != 1000 ) {
            testFailed = true;
        }

        if ( rec != null ) {
            rec.close();
        }
        con.close();

        if ( testFailed ) {
            throw new Exception("testGetDeliveryDelayClassicApiTopic failed");
        }
    }

    public void testGetDeliveryDelayClassicApiTopic_Tcp(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        TopicConnection con = jmsTCFTCP.createTopicConnection();
        con.start();

        TopicSession sessionSender = con.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);

        TopicSubscriber rec = sessionSender.createSubscriber(jmsTopic);

        TopicPublisher send = sessionSender.createPublisher(jmsTopic);

        long val = send.getDeliveryDelay();
        if ( val != 0 ) {
            testFailed = true;
        }

        send.setDeliveryDelay(1000);
        val = send.getDeliveryDelay();
        if ( val != 1000 ) {
            testFailed = true;
        }

        if ( rec != null ) {
            rec.close();
        }
        con.close();

        if ( testFailed ) {
            throw new Exception("testGetDeliveryDelayClassicApiTopic_Tcp failed");
        }
    }


    public void testJSAD_Send_Message_P2PTest(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        QueueConnectionFactory cf1 = (QueueConnectionFactory)
            new InitialContext().lookup("java:comp/env/jndi_JMS_BASE_QCF");

        Queue queue = (Queue) new InitialContext().lookup("java:comp/env/jndi_INPUT_Q");
        emptyQueue(cf1, queue);

        QueueConnection con = cf1.createQueueConnection();
        con.start();

        QueueSession sessionSender = con.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);

        QueueSender send = sessionSender.createSender(queue);
        String outbound = "Hello World from testJSAD_Send_Message_P2PTest";
        send.setDeliveryDelay(1000);
        send.send(sessionSender.createTextMessage(outbound));

        Queue queue1 = sessionSender.createQueue("QUEUE1");
        QueueReceiver rec = sessionSender.createReceiver(queue1);
        TextMessage receiveMsg = (TextMessage) rec.receive(30000);

        String inbound = receiveMsg.getText();

        if ( !outbound.equals(inbound) ) {
            testFailed = true;
        }

        sessionSender.close();
        con.close();

        if ( testFailed ) {
            throw new Exception("testJSAD_Send_Message_P2PTest failed");
        }
    }

    /**
     * Basic point-to-point test with a single send to a queue and a receive
     * from the alias,
     */

    public void testJSAD_Receive_Message_P2PTest(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        QueueConnectionFactory cf1 = (QueueConnectionFactory)
            new InitialContext().lookup("java:comp/env/jndi_JMS_BASE_QCF");

        QueueConnection con = cf1.createQueueConnection();
        con.start();

        QueueSession sessionSender = con.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
        Queue queue = sessionSender.createQueue("QUEUE1");
        emptyQueue(cf1, queue);
        QueueSender send = sessionSender.createSender(queue);

        String outbound = "Hello World from testJSAD_Receive_Message_P2PTest";
        send.setDeliveryDelay(1000);
        send.send(sessionSender.createTextMessage(outbound));

        Queue queue1 = sessionSender.createQueue("alias2Q1");

        QueueReceiver rec = sessionSender.createReceiver(queue1);

        TextMessage receiveMsg = (TextMessage) rec.receive(30000);

        String inbound = receiveMsg.getText();

        if ( !outbound.equals(inbound) ) {
            testFailed = true;
        }

        sessionSender.close();
        con.close();

        if ( testFailed ) {
            throw new Exception("testJSAD_Receive_Message_P2PTest failed");
        }
    }

    public void testBasicTemporaryQueue(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        QueueConnectionFactory cf1 = (QueueConnectionFactory)
            new InitialContext().lookup("java:comp/env/jndi_JMS_BASE_QCF");

        QueueConnection con = cf1.createQueueConnection();
        con.start();

        QueueSession jmsSession = con.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);

        TemporaryQueue tempQ = jmsSession.createTemporaryQueue();

        QueueSender queueSender = jmsSession.createSender(tempQ);
        QueueReceiver queueReceiver = jmsSession.createReceiver(tempQ);

        TextMessage msg1 = jmsSession.createTextMessage("testBasicTemporaryQueue");
        queueSender.setDeliveryDelay(1000);
        queueSender.send(msg1);

        TextMessage recMessage = (TextMessage) queueReceiver.receive(30000);

        if ( (recMessage == null) ||
             (recMessage.getText() == null) ||
             !recMessage.getText().equals("testBasicTemporaryQueue") ) {
            testFailed = true;
        }

        queueSender.close();
        queueReceiver.close();
        tempQ.delete();

        if ( testFailed ) {
            throw new Exception("testBasicTemporaryQueue failed");
        }
    }

    public void testSendMessageToQueue(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        QueueConnectionFactory cf1 = (QueueConnectionFactory)
            new InitialContext().lookup("java:comp/env/jndi_JMS_BASE_QCF");
        Queue queue = (Queue)
            new InitialContext().lookup("java:comp/env/jndi_INPUT_Q");

        QueueConnection con = cf1.createQueueConnection();
        con.start();

        emptyQueue(cf1, queue);

        QueueSession sessionSender = con.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);

        QueueSender send = sessionSender.createSender(queue);

        TextMessage msg = sessionSender.createTextMessage("ExceptionDestinationMessage");
        send.setDeliveryDelay(1000);
        send.send(msg);

        Thread.sleep(3000);

        con.close();
    }

    public void testReadMsgFromExceptionQueue(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        QueueConnectionFactory cf1 = (QueueConnectionFactory)
            new InitialContext().lookup("java:comp/env/jndi_JMS_BASE_QCF");
        Queue queue = (Queue)
            new InitialContext().lookup("java:comp/env/jndi_EXCEPTION_Q");

        QueueConnection con = cf1.createQueueConnection();
        con.start();

        QueueSession session = con.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);

        QueueReceiver rec = session.createReceiver(queue);
        TextMessage msg = (TextMessage) rec.receive(30000);

        if ( (msg == null) ||
             (msg.getText() == null) ||
             !msg.getText().equals("ExceptionDestinationMessage") ) {
            testFailed = true;
        }

        con.close();

        if ( testFailed ) {
            throw new Exception("testReadMsgFromExceptionQueue failed");
        }
    }

    public void testBytesMessage(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

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

        QueueConnectionFactory cf1 = (QueueConnectionFactory)
            new InitialContext().lookup("java:comp/env/jndi_JMS_BASE_QCF");
        Queue queue = (Queue)
            new InitialContext().lookup("java:comp/env/jndi_INPUT_Q");

        QueueConnection con = cf1.createQueueConnection();
        con.start();

        emptyQueue(cf1, jmsQueue);

        QueueSession jmsSession = con.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);

        BytesMessage sendBytesMessage = jmsSession.createBytesMessage();

        out.println("Setting bytes");

        try {
            sendBytesMessage.writeBoolean(boolean1);
            sendBytesMessage.writeByte(byte1);
            sendBytesMessage.writeChar(char1);
            sendBytesMessage.writeDouble(double1);
            sendBytesMessage.writeFloat(float1);
            sendBytesMessage.writeInt(int1);
            sendBytesMessage.writeLong(long1);
            sendBytesMessage.writeShort(short1);
            sendBytesMessage.writeUTF(string1);
            sendBytesMessage.writeBytes(bytes1);
            sendBytesMessage.writeObject(integer1);

        } catch ( JMSException e ) {
            testFailed = true;
        }

        QueueSender send = jmsSession.createSender(jmsQueue);
        QueueReceiver queueReceiver = jmsSession.createReceiver(jmsQueue);
        send.setDeliveryDelay(1000);

        send.send(jmsQueue, sendBytesMessage);
        out.println("Sent bytes message [ " + sendBytesMessage + " ]");

        BytesMessage recBytesMessage = (BytesMessage) queueReceiver.receive(30000);
        out.println("Received bytes message [ " + recBytesMessage + " ]");

        if ( recBytesMessage.readBoolean() != boolean1) {
            testFailed = true;
        }
        if ( recBytesMessage.readByte() != byte1 ) {
            testFailed = true;
        }
        if ( recBytesMessage.readChar() != char1 ) {
            testFailed = true;
        }
        if ( recBytesMessage.readDouble() != double1 ) {
            testFailed = true;
        }
        if ( recBytesMessage.readFloat() != float1 ) {
            testFailed = true;
        }
        if ( recBytesMessage.readInt() != int1 ) {
            testFailed = true;
        }
        if ( recBytesMessage.readLong() != long1 ) {
            testFailed = true;
        }
        if ( recBytesMessage.readShort() != short1 ) {
            testFailed = true;
        }

        String s1 = recBytesMessage.readUTF();
        if ( (s1 == null) || !s1.equals(string1) ) {
            testFailed = true;
        }
        if ( recBytesMessage.readBytes(bytes1) != 9 ) {
            testFailed = true;
        }

        con.close();

        if ( testFailed ) {
            throw new Exception("testBytesMessage failed");
        }
    }

    public void testComms_Send_Message_P2PTest_Default(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        QueueConnectionFactory cf1 = (QueueConnectionFactory)
            new InitialContext().lookup("java:comp/env/jndi_JMS_BASE_QCF");

        Queue queue = (Queue) new InitialContext().lookup("java:comp/env/jndi_INPUT_Q");

        QueueConnection con = cf1.createQueueConnection();
        con.start();

        emptyQueue(cf1, jmsQueue);

        QueueSession sessionSender = con.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);

        QueueSender send = sessionSender.createSender(jmsQueue);

        QueueReceiver rec = sessionSender.createReceiver(jmsQueue);
        String outbound = "testComms_Send_Message_P2PTest_Default";
        send.setDeliveryDelay(1000);
        send.send(jmsQueue, sessionSender.createTextMessage(outbound));

        TextMessage receiveMsg = (TextMessage) rec.receive(30000);
        String inbound = receiveMsg.getText();

        if ( !outbound.equals(inbound) ) {
            testFailed = true;
        }

        con.close();

        if ( testFailed ) {
            throw new Exception("testComms_Send_Message_P2PTest_Default failed");
        }
    }

    public void testSendMessage(HttpServletRequest request, HttpServletResponse response) 
        throws Exception {
        sendMessage(jmsQCFBindings);
    }
    
    public void testSendMessage_TCP(HttpServletRequest request, HttpServletResponse response) 
        throws Exception {
        sendMessage(jmsQCFTCP);        
    }

    private void sendMessage(QueueConnectionFactory queueConnectionFactory)
        throws Exception {

        try (JMSContext jmsContext = queueConnectionFactory.createContext()) {
            emptyQueue(queueConnectionFactory, jmsQueue);
            JMSProducer jmsProducer = jmsContext.createProducer();

            long delayMilliseconds = defaultTestDeliveryDelay * 12;
            jmsProducer.setDeliveryDelay(delayMilliseconds);
            TextMessage sendMsg = jmsContext.createTextMessage(this.getClass().getName()+".testSendMessage() deliveryDelay="+delayMilliseconds+" milliseconds, sentAt:"+timeStamp());
            sendMsg.setLongProperty("MustArriveAfter",System.currentTimeMillis()+delayMilliseconds);
            sendAndCheckDeliveryTime(jmsProducer, jmsQueue, sendMsg);
        }
    }

    public void testReceiveMessage(HttpServletRequest request, HttpServletResponse response)
        throws Exception {
        receiveMessage(jmsQCFBindings);
    }

    public void testReceiveMessage_TCP(HttpServletRequest request, HttpServletResponse response)
        throws Exception {
        receiveMessage(jmsQCFTCP);
    }
    
    private void receiveMessage(QueueConnectionFactory queueConnectionFactory)
        throws Exception {

        try (JMSContext jmsContext = queueConnectionFactory.createContext()) {
            JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);

            TextMessage receivedMessage = (TextMessage) jmsConsumer.receive(defaultTestDeliveryDelay*12);
            if (receivedMessage == null)
                throw new Exception("No message received");
            if ( !receivedMessage.getText().startsWith(this.getClass().getName()+".testSendMessage() "))
                throw new Exception("Incorrect Message received:"+receivedMessage.getText());
            if (receivedMessage.getLongProperty("MustArriveAfter") > System.currentTimeMillis())
                throw new Exception("Message arrived too soon\n"
                                   +"MustArriveAfter:"+receivedMessage.getLongProperty("MustArriveAfter")+" time now:"+System.currentTimeMillis()+"\n"
                                   +"Message:"+receivedMessage.getText());        
        }      
    }

}
