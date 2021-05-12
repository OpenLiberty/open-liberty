/*******************************************************************************
 * Copyright (c) 2013,2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package jmsproducer.web;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.Map;
import java.util.Set;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.DeliveryMode;
import javax.jms.InvalidDestinationException;
import javax.jms.InvalidDestinationRuntimeException;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.JMSProducer;
import javax.jms.JMSRuntimeException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageFormatRuntimeException;
import javax.jms.MessageNotWriteableException;
import javax.jms.MessageNotWriteableRuntimeException;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.StreamMessage;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.jms.TopicConnectionFactory;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

@SuppressWarnings("serial")
public class JMSProducerServlet extends HttpServlet {

    // JMS objects and helpers ...

    public static QueueConnectionFactory qcfBindings;
    public static QueueConnectionFactory qcfTCP;

    public static TopicConnectionFactory tcfBindings;
    public static TopicConnectionFactory tcfTCP;

    public static Queue queue1;
    public static Queue queue2;
    
    public static Topic topic1;
    
    /** @return the methodName of the caller. */
    private static final String methodName() { return new Exception().getStackTrace()[1].getMethodName(); }
    
    private final class TestException extends Exception {
        TestException(String message) {
            super(new Date() +" "+message);
        }
    }
    
    @Override
    public void init() throws ServletException {
        System.out.println("JMSProducerServlet.init ENTRY");

        super.init();

        try {
            qcfBindings = (QueueConnectionFactory)
                new InitialContext().lookup("java:comp/env/jndi_JMS_BASE_QCF");
        } catch ( NamingException e ) {
            e.printStackTrace();
        }
        System.out.println("Queue connection factory 'java:comp/env/jndi_JMS_BASE_QCF':\n" + qcfBindings);

        try {
            tcfBindings = (TopicConnectionFactory)
                new InitialContext().lookup("java:comp/env/eis/tcf");
        } catch ( NamingException e ) {
            e.printStackTrace();
        }
        System.out.println("Topic connection factory 'java:comp/env/eis/tcf':\n" + tcfBindings);

        try {
            qcfTCP = (QueueConnectionFactory)
                new InitialContext().lookup("java:comp/env/jndi_JMS_BASE_QCF1");
        } catch ( NamingException e ) {
            e.printStackTrace();
        }
        System.out.println("Queue connection factory 'java:comp/env/jndi_JMS_BASE_QCF1':\n" + qcfTCP);

        try {
            tcfTCP = (TopicConnectionFactory)
                new InitialContext().lookup("java:comp/env/eis/tcf1");
        } catch ( NamingException e ) {
            e.printStackTrace();
        }
        System.out.println("Topic connection factory 'java:comp/env/eis/tcf1':\n" + tcfTCP);

        try {
            queue1 = (Queue) new InitialContext().lookup("java:comp/env/jndi_INPUT_Q1");
        } catch ( NamingException e ) {
            e.printStackTrace();
        }
        System.out.println("Queue 'java:comp/env/jndi_INPUT_Q1':\n" + queue1);

        try {
            queue2 = (Queue) new InitialContext().lookup("java:comp/env/jndi_INPUT_Q2");
        } catch ( NamingException e ) {
            e.printStackTrace();
        }
        System.out.println("Queue 'java:comp/env/jndi_INPUT_Q2':\n" + queue2);

        try {
            topic1 = (Topic) new InitialContext().lookup("java:comp/env/eis/topic1");
        } catch ( NamingException e ) {
            e.printStackTrace();
        }
        System.out.println("Topic 'java:comp/env/eis/topic1':\n" + topic1);

        System.out.println("JMSProducerServlet.init RETURN");

        if ( (qcfBindings == null) || (tcfBindings == null) ||
             (qcfTCP == null) || (tcfTCP == null) ||
             (queue1 == null) || (queue2 == null) ||
             (topic1 == null)) {
            throw new ServletException("Failed JMS initialization");
        }
    }

    public void emptyQueue(QueueConnectionFactory qcf, Queue q) throws Exception {
        JMSContext context = qcf.createContext();
        QueueBrowser qb = context.createBrowser(q);
        JMSConsumer consumer = context.createConsumer(q);

        int numMsgs = getMessageCount(qb);

        for ( int msgNo = 0; msgNo < numMsgs; msgNo++ ) {
            Message message = consumer.receive();
        }

        context.close();
    }

    public int getMessageCount(QueueBrowser qb) throws JMSException {
        Enumeration e = qb.getEnumeration();

        int numMsgs = 0;
        while ( e.hasMoreElements() ) {
            e.nextElement();
            numMsgs++;
        }

        return numMsgs;
    }

    //

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
        TraceComponent tc = Tr.register(JMSProducerServlet.class);

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

    // 118071_1 JMSProducer send(Destination destination,Message message)
    // 118071_1_1_Q : Sends the message to the specified queue using any send
    // options, message properties and message headers that have been defined on
    // this JMSProducer.
    // Applications using the simplified API may also set these message headers
    // on the JMSProducer. Any message headers set using these methods will
    // override any values that have been set directly on the message.

    public void testJMSProducerSendMessage_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFBindings = qcfBindings.createContext();
        emptyQueue(qcfBindings, queue1);

        Message msg = jmsContextQCFBindings.createMessage();
        msg.setJMSType("TestType");
        msg.setJMSCorrelationID("MyCorrelID");

        JMSProducer producer = jmsContextQCFBindings.createProducer();

        producer
            .setJMSCorrelationID("TestCorrelID")
            .setJMSType("NewTestType")
            .setJMSReplyTo(queue2)
            .send(queue1, msg);

        QueueBrowser queueBrowserQCFBindings = jmsContextQCFBindings.createBrowser(queue1);
        int numMsgs = getMessageCount(queueBrowserQCFBindings);

        jmsContextQCFBindings.createConsumer(queue1).receive(30000);

        boolean testFailed = false;
        if ( (numMsgs != 1) ||
             !producer.getJMSCorrelationID().equals("TestCorrelID") ||
             !producer.getJMSType().equals("NewTestType") ||
             (producer.getJMSReplyTo() != queue2) ) {
            testFailed = true;
        }

        jmsContextQCFBindings.close();

        if ( testFailed ) {
            throw new Exception("testJMSProducerSendMessage_B_SecOff failed");
        }
    }

    public void testJMSProducerSendMessage_TCP_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFTCP = qcfTCP.createContext();
        emptyQueue(qcfTCP, queue1);

        Message msg = jmsContextQCFTCP.createMessage();
        msg.setJMSType("TestType");
        msg.setJMSCorrelationID("MyCorrelID");

        JMSProducer producer = jmsContextQCFTCP.createProducer();

        producer
            .setJMSCorrelationID("TestCorrelID")
            .setJMSType("NewTestType")
            .setJMSReplyTo(queue2)
            .send(queue1, msg);

        QueueBrowser queueBrowserQCFTCP = jmsContextQCFTCP.createBrowser(queue1);
        int numMsgs = getMessageCount(queueBrowserQCFTCP);

        jmsContextQCFTCP.createConsumer(queue1).receive(30000);

        boolean testFailed = false;
        if ( (numMsgs != 1) ||
             !producer.getJMSCorrelationID().equals("TestCorrelID") ||
             !producer.getJMSType().equals("NewTestType") ||
             (producer.getJMSReplyTo() != queue2) ) {
            testFailed = true;
        }

        jmsContextQCFTCP.close();

        if ( testFailed ) {
            throw new Exception("testJMSProducerSendMessage_TCP_SecOff failed");
        }
    }

    // 118071_1_3_Q InvalidDestinationRuntimeException - if a client uses this
    // method with an invalid queue

    public void testJMSProducerSendMessage_InvalidDestination_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFBindings = qcfBindings.createContext();
        JMSProducer producer = jmsContextQCFBindings.createProducer();

        Message msg = jmsContextQCFBindings.createMessage();

        boolean testFailed = false;
        try {
            Queue queue = null;
            producer.send(queue, msg);
            testFailed = true;
        } catch ( InvalidDestinationRuntimeException ex ) {
            ex.printStackTrace();
        }

        jmsContextQCFBindings.close();

        if ( testFailed ) {
            throw new Exception("testJMSProducerSendMessage_InvalidDestination_B_SecOff failed");
        }
    }

    public void testJMSProducerSendMessage_InvalidDestination_TCP_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFTCP = qcfTCP.createContext();
        JMSProducer producer = jmsContextQCFTCP.createProducer();

        Message msg = jmsContextQCFTCP.createMessage();

        boolean testFailed = false;
        try {
            Queue queue = null;
            producer.send(queue, msg);
            testFailed = true;
        } catch ( InvalidDestinationRuntimeException ex ) {
            ex.printStackTrace();
        }

        jmsContextQCFTCP.close();

        if ( testFailed ) {
            throw new Exception("testJMSProducerSendMessage_InvalidDestination_TCP_SecOff failed");
        }
    }

    // 118071_1_2_Q MessageFormatRuntimeException - if an invalid message is specified.
    // 118071_1_4_Q Test with message as null

    public void testJMSProducerSendMessage_NullMessage_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFBindings = qcfBindings.createContext();
        JMSProducer producer = jmsContextQCFBindings.createProducer();

        boolean testFailed = false;
        try {
            Message msg = null;
            producer.send(queue1, msg);
            testFailed = true;
        } catch ( MessageFormatRuntimeException ex ) {
            ex.printStackTrace();
        }

        jmsContextQCFBindings.close();

        if ( testFailed ) {
            throw new Exception("testJMSProducerSendMessage_NullMessage_B_SecOff failed");
        }
    }

    public void testJMSProducerSendMessage_NullMessage_TCP_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFTCP = qcfTCP.createContext();
        JMSProducer producer = jmsContextQCFTCP.createProducer();

        boolean testFailed = false;
        try {
            Message msg = null;
            producer.send(queue1, msg);
            testFailed = true;
        } catch ( MessageFormatRuntimeException ex ) {
            ex.printStackTrace();
        }

        jmsContextQCFTCP.close();

        if ( testFailed ) {
            throw new Exception("testJMSProducerSendMessage_NullMessage_TCP_SecOff failed");
        }
    }

    // 118071_1_5_Q Test with message as empty string

    public void testJMSProducerSendMessage_EmptyMessage_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFBindings = qcfBindings.createContext();
        emptyQueue(qcfBindings, queue1);
        JMSProducer producer = jmsContextQCFBindings.createProducer();

        TextMessage tmsg = jmsContextQCFBindings.createTextMessage("");
        try {
            producer.send(queue1, tmsg);
        } catch ( Exception ex ) {
            ex.printStackTrace();
        }

        QueueBrowser qb = jmsContextQCFBindings.createBrowser(queue1);
        int numMsgs = getMessageCount(qb);

        jmsContextQCFBindings.createConsumer(queue1).receive(30000);

        boolean testFailed = false;
        if ( numMsgs != 1 ) {
            testFailed = true;
        }

        jmsContextQCFBindings.close();

        if ( testFailed ) {
            throw new Exception("testJMSProducerSendMessage_EmptyMessage_B_SecOff failed");
        }
    }

    public void testJMSProducerSendMessage_EmptyMessage_TCP_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFTCP = qcfTCP.createContext();
        emptyQueue(qcfTCP, queue1);
        TextMessage tmsg = jmsContextQCFTCP.createTextMessage("");

        JMSProducer producer = jmsContextQCFTCP.createProducer();
        try {
            producer.send(queue1, tmsg);
        } catch ( Exception ex ) {
            ex.printStackTrace();
        }

        QueueBrowser qb = jmsContextQCFTCP.createBrowser(queue1);
        int numMsgs = getMessageCount(qb);

        jmsContextQCFTCP.createConsumer(queue1).receive(30000);

        boolean testFailed = false;
        if ( numMsgs != 1 ) {
            testFailed = true;
        }

        jmsContextQCFTCP.close();

        if ( testFailed ) {
            throw new Exception("testJMSProducerSendMessage_EmptyMessage_TCP_SecOff failed");
        }
    }

    // 118071_1_6_Q MessageNotWriteableRuntimeException - if this JMSProducer
    // has been configured to set a message property, but the message's
    // properties are read-only

    public void testJMSProducerSendMessage_NotWriteable_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFBindings = qcfBindings.createContext();
        emptyQueue(qcfBindings, queue1);

        StreamMessage msgOut = jmsContextQCFBindings.createStreamMessage();
        msgOut.reset();

        JMSProducer producer = jmsContextQCFBindings.createProducer();
        JMSConsumer jmsConsumer = jmsContextQCFBindings.createConsumer(queue1);

        producer.send(queue1, msgOut);

        StreamMessage msgIn = (StreamMessage) jmsConsumer.receive(30000);

        producer.setProperty("Role", "Tester");

        boolean testFailed = false;
        try {
            producer.send(queue1, msgIn);
            testFailed = true;
        } catch (MessageNotWriteableRuntimeException ex) {
            ex.printStackTrace();
        }

        jmsConsumer.close();
        jmsContextQCFBindings.close();

        if ( testFailed ) {
            throw new Exception("testJMSProducerSendMessage_NotWriteable_B_SecOff failed");
        }
    }

    public void testJMSProducerSendMessage_NotWriteable_TCP_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFTCP = qcfTCP.createContext();
        emptyQueue(qcfTCP, queue1);

        StreamMessage msgOut = jmsContextQCFTCP.createStreamMessage();
        msgOut.reset();

        JMSProducer producer = jmsContextQCFTCP.createProducer();
        JMSConsumer jmsConsumer = jmsContextQCFTCP.createConsumer(queue1);

        producer.send(queue1, msgOut);

        StreamMessage msgIn = (StreamMessage) jmsConsumer.receive(30000);

        producer.setProperty("Role", "Tester");

        boolean testFailed = false;
        try {
            producer.send(queue1, msgIn);
            testFailed = true;
        } catch ( MessageNotWriteableRuntimeException ex ) {
            ex.printStackTrace();
        }

        jmsConsumer.close();
        jmsContextQCFTCP.close();

        if ( testFailed ) {
            throw new Exception("testJMSProducerSendMessage_NotWriteable_TCP_SecOff failed");
        }
    }

    // 118071_1_7_T Sends a message to the specified topic using any send
    // options, message properties and message headers that have been defined on
    // this JMSProducer.

    public void testJMSProducerSendMessage_Topic_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {
        testJMSProducerSendMessage_Topic_SecOff(tcfBindings);
    }

    public void testJMSProducerSendMessage_Topic_TCP_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {
        testJMSProducerSendMessage_Topic_SecOff(tcfTCP);
    }
    
    private void testJMSProducerSendMessage_Topic_SecOff(TopicConnectionFactory topicConnectionFactory) 
            throws JMSException, TestException {
     
        // Use the default context so that the message is produced and consumed using the 
        // servlet transaction which is committed after the servlet returns.
        try (JMSContext jmsContext = topicConnectionFactory.createContext()) {  
            // Create the consumer first, JMS does not guarantee when the first message will be received
            // but in practice the subscription will have been made before the message has been sent. 
            JMSConsumer jmsConsumer = jmsContext.createConsumer(topic1);
            JMSProducer jmsProducer = jmsContext.createProducer();

            String sentMessageBody = methodName()+" at "+new Date();
            TextMessage sentMessage = jmsContext.createTextMessage(sentMessageBody);
            sentMessage.setJMSType("TestType");
            sentMessage.setJMSCorrelationID("MyCorrelID");

            // The jmsProducer set methods override the values set on the message itself.
            
            jmsProducer.setJMSCorrelationID("TestCorrelID")
                       .setJMSType("NewTestType")
                       .send(topic1, sentMessage);

            TextMessage receivedMessage = (TextMessage) jmsConsumer.receive(30000);
            if (receivedMessage == null)                    
                  throw new TestException("No message received, sent:"+sentMessage);
            if (    !receivedMessage.getText().equals(sentMessageBody) 
                 || !receivedMessage.getJMSCorrelationID().equals("TestCorrelID") 
                 || !receivedMessage.getJMSType().equals("NewTestType"))
                  throw new TestException("Wrong message received:"+receivedMessage+" sent:"+sentMessage); 
        }
    }

    public void testJMSProducerSendMessage_NullMessage_Topic_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {
        testJMSProducerSendMessage_NullMessage_Topic_SecOff(tcfBindings);
    }

    public void testJMSProducerSendMessage_NullMessage_Topic_TCP_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {
        testJMSProducerSendMessage_NullMessage_Topic_SecOff(tcfTCP);
    }
    
    private void testJMSProducerSendMessage_NullMessage_Topic_SecOff(TopicConnectionFactory topicConnectionFactory)
            throws TestException {

        try (JMSContext jmsContext = topicConnectionFactory.createContext()) {
            JMSProducer jmsProducer = jmsContext.createProducer();

            Message sentMessage = null;
            try {
                jmsProducer.send(topic1, sentMessage);
                throw new TestException("Sent null message without throwing MessageFormatRuntimeException sent:"+sentMessage);
            } catch (MessageFormatRuntimeException ex) {
                // Expected.
            }
        }
    }

    // 118071_1_9_T InvalidDestinationRuntimeException - if a client uses this
    // method with an invalid topic

    public void testJMSProducerSendMessage_InvalidDestinationTopic_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {
        testJMSProducerSendMessage_InvalidDestinationTopic_SecOff(tcfBindings);
    }

    public void testJMSProducerSendMessage_InvalidDestinationTopic_TCP_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {
        testJMSProducerSendMessage_InvalidDestinationTopic_SecOff(tcfTCP);
    }

    private void testJMSProducerSendMessage_InvalidDestinationTopic_SecOff(TopicConnectionFactory topicConnectionFactory) 
        throws TestException {
        
        try (JMSContext jmsContext = topicConnectionFactory.createContext()) {
            JMSProducer jmsProducer = jmsContext.createProducer();

            TextMessage sentMessage = jmsContext.createTextMessage(methodName() + " at " + new Date());
            try {
                Topic topic1 = null;
                jmsProducer.send(topic1, sentMessage);
                throw new TestException("Sent message to null topic without throwing InvalidDestinationRuntimeException, sent:" + sentMessage);
            } catch (InvalidDestinationRuntimeException ex) {
                // Expected.
            }
        }
    }
    
    // 118071_1_10_T MessageNotWriteableRuntimeException - if this JMSProducer
    // has been configured to set a message property, but the message's
    // properties are read-only
    //TODO Badly named, its the message thats not writable, the topic is writable.

    public void testJMSProducerSendMessage_NotWriteableTopic_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {
        testJMSProducerSendMessage_NotWriteableTopic_SecOff(tcfBindings);
    }

    public void testJMSProducerSendMessage_NotWriteableTopic_TCP_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {
        testJMSProducerSendMessage_NotWriteableTopic_SecOff(tcfTCP);
    }

    private void testJMSProducerSendMessage_NotWriteableTopic_SecOff(TopicConnectionFactory topicConnectionFactory)
            throws JMSException, TestException {

        try (JMSContext jmsContext = topicConnectionFactory.createContext()) {
            JMSProducer jmsProducer = jmsContext.createProducer();
            JMSConsumer jmsConsumer = jmsContext.createConsumer(topic1);

            StreamMessage sentMessage = jmsContext.createStreamMessage();
            sentMessage.writeString(methodName() + " 1 at " + new Date());
            sentMessage.reset();
            try {
                sentMessage.writeString(methodName() + " 2 at " + new Date());
                throw new TestException("Write to a read only message after reset() without throwing MessageNotWriteableException"
                        + " sent(1):" + sentMessage);
            } catch (MessageNotWriteableException ex) {
                // Expected
            }

            jmsProducer.send(topic1, sentMessage);

            StreamMessage receivedMessage = (StreamMessage) jmsConsumer.receive(30000);
            jmsProducer.setProperty("Role", "Tester");

            try {
                jmsProducer.send(topic1, receivedMessage);
                throw new TestException("Sent a read only message after modifying a property without throwing MessageNotWriteableRuntimeException"
                                       + " sent(2):" + receivedMessage);
            } catch (MessageNotWriteableRuntimeException ex) {
                // Expected
            }

        }

    }
    
    // 118071_1_12_T Test with message as empty string

    public void testJMSProducerSendMessage_EmptyMessage_Topic_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {
        testJMSProducerSendMessage_EmptyMessage_Topic_SecOff(tcfBindings);
    }

    public void testJMSProducerSendMessage_EmptyMessage_Topic_TCP_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {
        testJMSProducerSendMessage_EmptyMessage_Topic_SecOff(tcfTCP);
    }

    private void testJMSProducerSendMessage_EmptyMessage_Topic_SecOff(TopicConnectionFactory topicConnectionFactory)
        throws JMSException, TestException {
            
        try (JMSContext jmsContext = topicConnectionFactory.createContext()) {
            JMSConsumer jmsConsumer = jmsContext.createConsumer(topic1);
            JMSProducer jmsProducer = jmsContext.createProducer();

            TextMessage sentMessage = jmsContext.createTextMessage("");
            jmsProducer.send(topic1, sentMessage);

            TextMessage receivedMessage = (TextMessage) jmsConsumer.receive(30000);
            String receivedMessageText = receivedMessage.getText();

            if ( !receivedMessageText.equals("") ) {
                throw new TestException("Wrong message received:"+receivedMessage+" sent:"+sentMessage);
            }
        }
    }
 
    // 118071_2 JMSProducer send(Destination destination, String body)
    // 118071_2_1_Q Send a TextMessage with the specified body to the specified
    // queue, using any send options, message properties and message headers
    // that have been defined on this JMSProducer.

    public void testJMSProducerSendTextMessage_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFBindings = qcfBindings.createContext();
        emptyQueue(qcfBindings, queue1);
        JMSConsumer jmsConsumer = jmsContextQCFBindings.createConsumer(queue1);
        JMSProducer producer = jmsContextQCFBindings.createProducer();

        producer
            .setJMSCorrelationID("TestCorrelID")
            .setJMSType("NewTestType")
            .setJMSReplyTo(queue2)
            .send(queue1, "This is the messageBody");

        QueueBrowser qb = jmsContextQCFBindings.createBrowser(queue1);
        int numMsgs = getMessageCount(qb);

        String msgBody = jmsConsumer.receive(30000).getBody(String.class);

        boolean testFailed = false;
        if ( (numMsgs != 1) ||
             !msgBody.equals("This is the messageBody") ||
             !producer.getJMSCorrelationID().equals("TestCorrelID") ||
             !producer.getJMSType().equals("NewTestType") ||
             (producer.getJMSReplyTo() != queue2) ) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContextQCFBindings.close();

        if ( testFailed ) {
            throw new Exception("testJMSProducerSendTextMessage_B_SecOff failed");
        }
    }

    public void testJMSProducerSendTextMessage_TCP_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFTCP = qcfTCP.createContext();
        emptyQueue(qcfTCP, queue1);
        JMSConsumer jmsConsumer = jmsContextQCFTCP.createConsumer(queue1);
        JMSProducer producer = jmsContextQCFTCP.createProducer();

        producer
            .setJMSCorrelationID("TestCorrelID")
            .setJMSType("NewTestType")
            .setJMSReplyTo(queue2)
            .send(queue1, "This is the messageBody");

        QueueBrowser qb = jmsContextQCFTCP.createBrowser(queue1);
        int numMsgs = getMessageCount(qb);

        String msgBody = jmsConsumer.receive(30000).getBody(String.class);

        boolean testFailed = false;
        if ( (numMsgs != 1) ||
             !msgBody.equals("This is the messageBody") ||
             !producer.getJMSCorrelationID().equals("TestCorrelID") ||
             !producer.getJMSType().equals("NewTestType") ||
             (producer.getJMSReplyTo() != queue2) ) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContextQCFTCP.close();

        if ( testFailed ) {
            throw new Exception("testJMSProducerSendTextMessage_TCP_SecOff failed");
        }
    }

    // 118071_2_3_Q InvalidDestinationRuntimeException - if a client uses this
    // method with an invalid queue

    public void testJMSProducerSendTextMessage_InvalidDestination_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFBindings = qcfBindings.createContext();
        JMSProducer producer = jmsContextQCFBindings.createProducer();

        boolean testFailed = false;
        try {
            Queue queue = null;
            producer.send(queue, "This is the messageBody");
            testFailed = true;
        } catch ( InvalidDestinationRuntimeException ex ) {
            ex.printStackTrace();
        }

        jmsContextQCFBindings.close();

        if ( testFailed ) {
            throw new Exception("testJMSProducerSendTextMessage_InvalidDestination_B_SecOff failed");
        }
    }

    public void testJMSProducerSendTextMessage_InvalidDestination_TCP_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFTCP = qcfTCP.createContext();
        JMSProducer producer = jmsContextQCFTCP.createProducer();

        boolean testFailed = false;
        try {
            Queue queue = null;
            producer.send(queue, "This is the messageBody");
            testFailed = true;
        } catch ( InvalidDestinationRuntimeException ex ) {
            ex.printStackTrace();
        }

        jmsContextQCFTCP.close();

        if ( testFailed ) {
            throw new Exception("testJMSProducerSendTextMessage_InvalidDestination_TCP_SecOff failed");
        }
    }

    // 118071_2_4_Queue If a null value is specified for body then a
    // TopicextMessage with no body will be sent.

    public void testJMSProducerSendTextMessage_NullMessageBody_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFBindings = qcfBindings.createContext();
        emptyQueue(qcfBindings, queue1);
        JMSConsumer jmsConsumer = jmsContextQCFBindings.createConsumer(queue1);
        JMSProducer producer = jmsContextQCFBindings.createProducer();

        String msg = null;
        producer
            .setJMSCorrelationID("TestCorrelID")
            .setJMSType("NewTestType")
            .setJMSReplyTo(queue2)
            .send(queue1, msg);

        QueueBrowser qb = jmsContextQCFBindings.createBrowser(queue1);
        int numMsgs = getMessageCount(qb);

        jmsConsumer.receive(30000);

        boolean testFailed = false;
        if ( (numMsgs != 1) ||
             !producer.getJMSCorrelationID().equals("TestCorrelID") ||
             !producer.getJMSType().equals("NewTestType") ||
             (producer.getJMSReplyTo() != queue2) ) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContextQCFBindings.close();

        if ( testFailed ) {
            throw new Exception("testJMSProducerSendTextMessage_NullMessageBody_B_SecOff failed");
        }
    }

    public void testJMSProducerSendTextMessage_NullMessageBody_TCP_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFTCP = qcfTCP.createContext();
        emptyQueue(qcfTCP, queue1);
        JMSConsumer jmsConsumer = jmsContextQCFTCP.createConsumer(queue1);
        JMSProducer producer = jmsContextQCFTCP.createProducer();

        String msg = null;
        producer
            .setJMSCorrelationID("TestCorrelID")
            .setJMSType("NewTestType")
            .setJMSReplyTo(queue2)
            .send(queue1, msg);

        QueueBrowser qb = jmsContextQCFTCP.createBrowser(queue1);
        int numMsgs = getMessageCount(qb);

        jmsConsumer.receive(30000);

        boolean testFailed = false;
        if ( (numMsgs != 1) ||
             !producer.getJMSCorrelationID().equals("TestCorrelID") ||
             !producer.getJMSType().equals("NewTestType") ||
             (producer.getJMSReplyTo() != queue2) ) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContextQCFTCP.close();

        if ( testFailed ) {
            throw new Exception("testJMSProducerSendTextMessage_NullMessageBody_TCP_SecOff failed");
        }
    }

    // 118071_2_5_Queue Test with empty string for the body

    public void testJMSProducerSendTextMessage_EmptyMessage_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFBindings = qcfBindings.createContext();
        emptyQueue(qcfBindings, queue1);
        JMSConsumer jmsConsumer = jmsContextQCFBindings.createConsumer(queue1);
        JMSProducer producer = jmsContextQCFBindings.createProducer();

        try {
            producer.send(queue1, "");
        } catch ( Exception ex ) {
            ex.printStackTrace();
        }

        QueueBrowser qb = jmsContextQCFBindings.createBrowser(queue1);
        int numMsgs = getMessageCount(qb);

        String recvdMessage = jmsConsumer.receive(30000).getBody(String.class);

        boolean testFailed = false;
        if ( !recvdMessage.equals("") || (numMsgs != 1) ) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContextQCFBindings.close();

        if ( testFailed ) {
            throw new Exception("testJMSProducerSendTextMessage_EmptyMessage_B_SecOff failed");
        }
    }

    public void testJMSProducerSendTextMessage_EmptyMessage_TCP_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFTCP = qcfTCP.createContext();
        emptyQueue(qcfTCP, queue1);
        JMSConsumer jmsConsumer = jmsContextQCFTCP.createConsumer(queue1);
        JMSProducer producer = jmsContextQCFTCP.createProducer();

        try {
            producer.send(queue1, "");
        } catch ( Exception ex ) {
            ex.printStackTrace();
        }

        QueueBrowser qb = jmsContextQCFTCP.createBrowser(queue1);
        int numMsgs = getMessageCount(qb);

        String recvdMessage = jmsConsumer.receive(30000).getBody(String.class);

        boolean testFailed = false;
        if ( !recvdMessage.equals("") || (numMsgs != 1) ) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContextQCFTCP.close();

        if ( testFailed ) {
            throw new Exception("testJMSProducerSendTextMessage_EmptyMessage_TCP_SecOff failed");
        }
    }

    // 118071_2_6_Topic :Send a TextMessage with the specified body to the
    // specified topic, using any send options, message properties and message
    // headers that have been defined on this JMSProducer.

    public void testJMSProducerSendTextMessage_Topic_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {
        testJMSProducerSendTextMessage_Topic_SecOff(tcfBindings);
    }

    public void testJMSProducerSendTextMessage_Topic_TCP_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {
        testJMSProducerSendTextMessage_Topic_SecOff(tcfTCP);
    }

    private void testJMSProducerSendTextMessage_Topic_SecOff(TopicConnectionFactory topicConnectionFactory)
            throws JMSException, TestException {
        
        try (JMSContext jmsContext = topicConnectionFactory.createContext()) {
            JMSConsumer jmsConsumer = jmsContext.createConsumer(topic1);
            JMSProducer jmsProducer = jmsContext.createProducer();

            String sentMessageBody1 = methodName()+" 1 at "+new Date();
            jmsProducer.setJMSCorrelationID("TestCorrelID")
                       .setJMSType("NewTestType")
                       .send(topic1, sentMessageBody1);

            Message receivedMessage1 = jmsConsumer.receive(30000);
            String receivedMessageBody1 = receivedMessage1.getBody(String.class);

            if ( !receivedMessageBody1.equals(sentMessageBody1) ||
                 !receivedMessage1.getJMSCorrelationID().equals("TestCorrelID") ||
                 !receivedMessage1.getJMSType().equals("NewTestType") ) {
                throw new TestException("Wrong message received 1:"+receivedMessage1);
            }
            
            // Repeat, to establish that the producer correlationId and message type have not changed.
            
            String sentMessageBody2 = methodName()+" 2 at "+new Date();
            jmsProducer.send(topic1, sentMessageBody2);

            Message receivedMessage2 = jmsConsumer.receive(30000);
            String receivedMessageBody2 = receivedMessage2.getBody(String.class);

            if ( !receivedMessageBody2.equals(sentMessageBody2) ||
                 !receivedMessage2.getJMSCorrelationID().equals("TestCorrelID") ||
                 !receivedMessage2.getJMSType().equals("NewTestType") ) {
                throw new TestException("Wrong message received 2:"+receivedMessage2);
            }
        }
    }
    
    // 118071_2_8_Topic InvalidDestinationRuntimeException - if a client uses
    // this method with an invalid topic

    public void testJMSProducerSendTextMessage_InvalidDestinationTopic_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {
        testJMSProducerSendTextMessage_InvalidDestinationTopic_SecOff(tcfTCP);
    }

    public void testJMSProducerSendTextMessage_InvalidDestinationTopic_TCP_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {
        testJMSProducerSendTextMessage_InvalidDestinationTopic_SecOff(tcfTCP);
    }

    private void testJMSProducerSendTextMessage_InvalidDestinationTopic_SecOff(TopicConnectionFactory topicConnectionFactory)
        throws TestException {
        try (JMSContext jmsContextTCFBindings = tcfBindings.createContext()) {
            JMSProducer producer = jmsContextTCFBindings.createProducer();

            try {
                Topic nullTopic = null;
                String sentMessageBody = methodName()+" at "+new Date();
                producer.send(nullTopic, sentMessageBody);
                throw new TestException("Sent message to null topic without throwing InvalidDestinationRuntimeException, sent:" + sentMessageBody);
                
            } catch ( InvalidDestinationRuntimeException ex ) {
                // Expected
            }           
        }
    }
    
    // 118071_2_9_Topic If a null value is specified for body then a TextMessage
    // with no body will be sent.

    public void testJMSProducerSendTextMessage_NullMessage_Topic_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {
        testJMSProducerSendTextMessage_NullMessage_Topic_SecOff(tcfBindings);
    }

    public void testJMSProducerSendTextMessage_NullMessage_Topic_TCP_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {
        testJMSProducerSendTextMessage_NullMessage_Topic_SecOff(tcfTCP);
    }

    private void testJMSProducerSendTextMessage_NullMessage_Topic_SecOff(TopicConnectionFactory topicConnectionFactory)
            throws JMSException, TestException {
            
        try (JMSContext jmsContextTCFBindings = topicConnectionFactory.createContext()) {
            JMSConsumer jmsConsumer = jmsContextTCFBindings.createConsumer(topic1);
            JMSProducer jmsProducer = jmsContextTCFBindings.createProducer();

            String sentMessageBody = null;
            jmsProducer.setJMSCorrelationID("TestCorrelID")
                       .setJMSType("NewTestType")
                       .send(topic1, sentMessageBody);

            Message receivedMessage = jmsConsumer.receive(30000);

            if (    !(receivedMessage.getBody(String.class) == null) 
                 || !receivedMessage.getJMSCorrelationID().equals("TestCorrelID") 
                 || !receivedMessage.getJMSType().equals("NewTestType")) {
                 throw new TestException("Wrong message received:" + receivedMessage);
            }
        }
    }
    
    // 118071_2_10_Topic Test with empty string for the body

    public void testJMSProducerSendTextMessage_EmptyMessage_Topic_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {
        testJMSProducerSendTextMessage_EmptyMessage_Topic_SecOff(tcfBindings);
    }

    public void testJMSProducerSendTextMessage_EmptyMessage_Topic_TCP_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {
        testJMSProducerSendTextMessage_EmptyMessage_Topic_SecOff(tcfTCP);
    }

    private void testJMSProducerSendTextMessage_EmptyMessage_Topic_SecOff(TopicConnectionFactory topicConnectionFactory)
            throws JMSException, TestException {
        
        try (JMSContext jmsContext = topicConnectionFactory.createContext()) {
            JMSConsumer jmsConsumer = jmsContext.createConsumer(topic1);
            JMSProducer jmsProducer = jmsContext.createProducer();

            jmsProducer.send(topic1, "");
            Message receivedMessage = jmsConsumer.receive(30000);
            String receivedMessageBody = receivedMessage.getBody(String.class);

            if ( !receivedMessageBody.equals("") ) {
                 throw new TestException("Wrong message received:"+receivedMessage);
            }
        }
    }
    
    // 118071_3 JMSProducer send(Destination destination,Map<String,Object> body)
    // 118071_3_1_Queue Send a MapMessage with the specified body to the
    // specified queue, using any send options, message properties and message
    // headers that have been defined on this JMSProducer.

    public void testJMSProducerSendMapMessage_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable { 

        JMSContext jmsContextQCFBindings = qcfBindings.createContext();
        emptyQueue(qcfBindings, queue1);
        JMSConsumer jmsConsumer = jmsContextQCFBindings.createConsumer(queue1);

        MapMessage mapMessage = jmsContextQCFBindings.createMapMessage();
        String propName = "myPropName";
        Object val = new Integer(10);
        mapMessage.setObject(propName, val);

        JMSProducer producer = jmsContextQCFBindings.createProducer();

        producer
            .setJMSCorrelationID("TestCorrelID")
            .setJMSType("NewTestType")
            .setJMSReplyTo(queue2)
            .send(queue1, mapMessage);

        QueueBrowser qb = jmsContextQCFBindings.createBrowser(queue1);
        int numMsgs = getMessageCount(qb);

        boolean correctMapBody = jmsConsumer
            .receive(30000)
            .getBody(java.util.Map.class)
            .containsValue(val);

        boolean testFailed = false;

        if ( (numMsgs != 1) ||
             !correctMapBody ||
             !producer.getJMSCorrelationID().equals("TestCorrelID") ||
             !producer.getJMSType().equals("NewTestType") ||
             (producer.getJMSReplyTo() != queue2) ) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContextQCFBindings.close();

        if ( testFailed ) {
            throw new Exception("testJMSProducerSendMapMessage_B_SecOff failed");
        }
    }

    public void testJMSProducerSendMapMessage_TCP_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFTCP = qcfTCP.createContext();
        emptyQueue(qcfTCP, queue1);
        JMSConsumer jmsConsumer = jmsContextQCFTCP.createConsumer(queue1);

        MapMessage mapMessage = jmsContextQCFTCP.createMapMessage();
        String propName = "myPropName";
        Object val = new Integer(10);
        mapMessage.setObject(propName, val);

        JMSProducer producer = jmsContextQCFTCP.createProducer();

        producer
            .setJMSCorrelationID("TestCorrelID")
            .setJMSType("NewTestType")
            .setJMSReplyTo(queue2)
            .send(queue1, mapMessage);

        QueueBrowser qb = jmsContextQCFTCP.createBrowser(queue1);
        int numMsgs = getMessageCount(qb);

        boolean correctMapBody = jmsConsumer
            .receive(30000)
            .getBody(java.util.Map.class)
            .containsValue(val);

        boolean testFailed = false;
        if ( (numMsgs != 1) ||
             !correctMapBody  ||
             !producer.getJMSCorrelationID().equals("TestCorrelID") ||
             !producer.getJMSType().equals("NewTestType") ||
             (producer.getJMSReplyTo() != queue2) ) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContextQCFTCP.close();

        if ( testFailed ) {
            throw new Exception("testJMSProducerSendMapMessage_B_SecOff failed");
        }
    }

    // 118071_3_3_Queue InvalidDestinationRuntimeException - if a client uses
    // this method with an invalid queue

    public void testJMSProducerSendMapMessage_InvalidDestination_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFBindings = qcfBindings.createContext();
        JMSConsumer jmsConsumer = jmsContextQCFBindings.createConsumer(queue1);

        MapMessage mapMessage = jmsContextQCFBindings.createMapMessage();
        String propName = "myPropName";
        Object val = new Integer(10);
        mapMessage.setObject(propName, val);

        JMSProducer producer = jmsContextQCFBindings.createProducer();

        boolean testFailed = false;
        try {
            producer
                .setJMSCorrelationID("TestCorrelID")
                .setJMSType("NewTestType")
                .setJMSReplyTo(queue2)
                .send(null, mapMessage);
            testFailed = true;
        } catch ( InvalidDestinationRuntimeException ex ) {
            ex.printStackTrace();
        }

        jmsConsumer.close();
        jmsContextQCFBindings.close();

        if ( testFailed ) {
            throw new Exception("testJMSProducerSendMapMessage_InvalidDestination_B_SecOff failed");
        }
    }

    public void testJMSProducerSendMapMessage_InvalidDestination_TCP_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFTCP = qcfTCP.createContext();
        JMSConsumer jmsConsumer = jmsContextQCFTCP.createConsumer(queue1);

        MapMessage mapMessage = jmsContextQCFTCP.createMapMessage();
        String propName = "myPropName";
        Object val = new Integer(10);
        mapMessage.setObject(propName, val);

        JMSProducer producer = jmsContextQCFTCP.createProducer();

        boolean testFailed = false;
        try {
            producer
                .setJMSCorrelationID("TestCorrelID")
                .setJMSType("NewTestType")
                .setJMSReplyTo(queue2)
                .send(null, mapMessage);
            testFailed = true;
        } catch ( InvalidDestinationRuntimeException ex ) {
            ex.printStackTrace();
        }

        jmsConsumer.close();
        jmsContextQCFTCP.close();

        if ( testFailed ) {
            throw new Exception("testJMSProducerSendMapMessage_InvalidDestination_TCP_SecOff failed");
        }
    }

    // 118071_3_4_Queue If a null value is specified then a MapMessage with no
    // map entries will be sent.

    public void testJMSProducerSendMapMessage_Null_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFBindings = qcfBindings.createContext();
        emptyQueue(qcfBindings, queue1);
        JMSConsumer jmsConsumer = jmsContextQCFBindings.createConsumer(queue1);
        JMSProducer producer = jmsContextQCFBindings.createProducer();

        boolean testFailed = false;

        try {
            MapMessage mapMessage = null;
            producer
                .setJMSCorrelationID("TestCorrelID")
                .setJMSType("NewTestType")
                .setJMSReplyTo(queue2)
                .send(queue1, mapMessage);
            testFailed = true;
        } catch ( MessageFormatRuntimeException ex ) {
            ex.printStackTrace();
        }

        jmsConsumer.receive(30000);

        MapMessage mapMessage1 = jmsContextQCFBindings.createMapMessage();
        String propName = "myPropName";
        Object val = null;
        mapMessage1.setObject(propName, val);

        producer
            .setJMSCorrelationID("TestCorrelID")
            .setJMSType("NewTestType")
            .setJMSReplyTo(queue2)
            .send(queue1, mapMessage1);

        QueueBrowser qb = jmsContextQCFBindings.createBrowser(queue1);
        int numMsgs = getMessageCount(qb);

        if ( (numMsgs != 1) ||
             !producer.getJMSCorrelationID().equals("TestCorrelID") ||
             !producer.getJMSType().equals("NewTestType") ||
             (producer.getJMSReplyTo() != queue2) ) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContextQCFBindings.close();

        if ( testFailed ) {
            throw new Exception("testJMSProducerSendMapMessage_Null_B_SecOff failed");
        }
    }

    public void testJMSProducerSendMapMessage_Null_TCP_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFTCP = qcfTCP.createContext();
        emptyQueue(qcfTCP, queue1);
        JMSConsumer jmsConsumer = jmsContextQCFTCP.createConsumer(queue1);
        JMSProducer producer = jmsContextQCFTCP.createProducer();

        boolean testFailed = false;
        try {
            MapMessage mapMessage = null;
            producer
                .setJMSCorrelationID("TestCorrelID")
                .setJMSType("NewTestType")
                .setJMSReplyTo(queue2)
                .send(queue1, mapMessage);
            testFailed = true;
        } catch ( MessageFormatRuntimeException ex ) {
            ex.printStackTrace();
        }

        MapMessage mapMessage1 = jmsContextQCFTCP.createMapMessage();

        String propName = "myPropName";
        Object val = null;
        mapMessage1.setObject(propName, val);

        producer
            .setJMSCorrelationID("TestCorrelID")
            .setJMSType("NewTestType")
            .setJMSReplyTo(queue2)
            .send(queue1, mapMessage1);

        QueueBrowser qb = jmsContextQCFTCP.createBrowser(queue1);
        int numMsgs = getMessageCount(qb);

        if ( (numMsgs != 1) ||
             !producer.getJMSCorrelationID().equals("TestCorrelID") ||
             !producer.getJMSType().equals("NewTestType") ||
             (producer.getJMSReplyTo() != queue2) ) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContextQCFTCP.close();

        if ( testFailed ) {
            throw new Exception("testJMSProducerSendMapMessage_Null_B_SecOff failed");
        }
    }

    // 118071_3_5_Topic Send a MapMessage with the specified body to the
    // specified topic, using any send options, message properties and message
    // headers that have been defined on this JMSProducer.

    public void testJMSProducerSendMapMessage_Topic_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {
        testJMSProducerSendMapMessage_Topic_SecOff(tcfBindings);
    }

    public void testJMSProducerSendMapMessage_Topic_TCP_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {
        testJMSProducerSendMapMessage_Topic_SecOff(tcfTCP);
    }

    private void testJMSProducerSendMapMessage_Topic_SecOff(TopicConnectionFactory topicConnectionFactory)
            throws JMSException, TestException {
            
        try (JMSContext jmsContext = topicConnectionFactory.createContext()) {
            JMSConsumer jmsConsumer = jmsContext.createConsumer(topic1);
            JMSProducer jmsProducer = jmsContext.createProducer();

            MapMessage sentMessage = jmsContext.createMapMessage();
            String name = "myName";
            Object value = new Long(System.currentTimeMillis());
            sentMessage.setObject(name, value);

            jmsProducer.setJMSCorrelationID("TestCorrelID")
                       .setJMSType("NewTestType")
                       .send(topic1, sentMessage);

            MapMessage receivedMessage = (MapMessage) jmsConsumer.receive(30000);
            if (receivedMessage == null)
                throw new TestException("No message received, sent:"+sentMessage);  
            if (   !receivedMessage.getObject(name).equals(value)
                || !receivedMessage.getJMSCorrelationID().equals("TestCorrelID") 
                || !receivedMessage.getJMSType().equals("NewTestType") ) {
                throw new TestException("Wrong message received:"+ receivedMessage);
            }
        }
    }
    
    // 118071_3_7_Topic InvalidDestinationRuntimeException - if a client uses
    // this method with an invalid queue

    public void testJMSProducerSendMapMessageTopic_InvalidDestination_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {
        testJMSProducerSendMapMessageTopic_InvalidDestination_SecOff(tcfBindings);
    }

    public void testJMSProducerSendMapMessageTopic_InvalidDestination_TCP_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {
        testJMSProducerSendMapMessageTopic_InvalidDestination_SecOff(tcfTCP);
    }

    private void testJMSProducerSendMapMessageTopic_InvalidDestination_SecOff(TopicConnectionFactory topicConnectionFactory)
            throws JMSException, TestException {
            
        try (JMSContext jmsContext = topicConnectionFactory.createContext()) {
            JMSProducer jmsProducer = jmsContext.createProducer();

            MapMessage sentMessage = jmsContext.createMapMessage();
            String name = "myName";
            Object value = new Long(System.currentTimeMillis());
            sentMessage.setObject(name, value);
 
            try {
                jmsProducer.setJMSCorrelationID("TestCorrelID")
                           .setJMSType("NewTestType")
                           .send(null, sentMessage);
                // Should not reach here.
                throw new TestException("Sent message to null topic without throwing InvalidDestinationRuntimeException, sent:" + sentMessage);
            
            } catch ( InvalidDestinationRuntimeException ex ) {
                // Expected
            }
        }
    }
    
    /**
      118071_3_8_Topic If a null value is specified then a MapMessage with a null value is sent. 
      Note: The JMS specification for JMSMapMesage.setObject() states:
      <q>This method works only for the objectified primitive object types (Integer, Double, Long ...), 
      String objects, and byte arrays.</q>
      null is not a primitive, so this should not work.
    */
    public void testJMSProducerSendMapMessageTopic_Null_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {
        testJMSProducerSendMapMessageTopic_Null_SecOff(tcfBindings);
    }

    public void testJMSProducerSendMapMessageTopic_Null_TCP_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {
        testJMSProducerSendMapMessageTopic_Null_SecOff(tcfTCP);
    }

    private void testJMSProducerSendMapMessageTopic_Null_SecOff(TopicConnectionFactory topicConnectionFactory)
            throws JMSException, TestException {
            
        try (JMSContext jmsContext = topicConnectionFactory.createContext()) {
            JMSConsumer jmsConsumer = jmsContext.createConsumer(topic1);
            JMSProducer jmsProducer = jmsContext.createProducer();

            MapMessage sentMessage = jmsContext.createMapMessage();
            String name = "myName";
            Object value = null;
            sentMessage.setObject(name, value);

            jmsProducer.setJMSCorrelationID("TestCorrelID")
                       .setJMSType("NewTestType")
                       .setJMSReplyTo(topic1)
                       .send(topic1, sentMessage);
            
            MapMessage receivedMessage = (MapMessage) jmsConsumer.receive(30000);
            if (receivedMessage == null)
                throw new TestException("No message received, sent:"+sentMessage);  
            
            if (   receivedMessage.getBody(Map.class).isEmpty()
                || !(receivedMessage.getObject(name) == null)           
                || !receivedMessage.getJMSCorrelationID().equals("TestCorrelID") 
                || !receivedMessage.getJMSType().equals("NewTestType")
                || !receivedMessage.getJMSReplyTo().equals(topic1)) {
                throw new TestException("Wrong message received:" + receivedMessage + " sent:" + sentMessage);
            }
        }
    }
    
    // 118071_4 JMSProducer send(Destination destination,byte[] body)
    // 118071_4_1_Queue Send a BytesMessage with the specified body to the
    // specified queue, using any send options, message properties and message
    // headers that have been defined on this JMSProducer.

    public void testJMSProducerSendByteMessage_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFBindings = qcfBindings.createContext();
        emptyQueue(qcfBindings, queue1);
        JMSConsumer jmsConsumer = jmsContextQCFBindings.createConsumer(queue1);
        JMSProducer producer = jmsContextQCFBindings.createProducer();

        byte[] content = new byte[] { 127, 0 };

        producer
            .setJMSCorrelationID("TestCorrelID")
            .setJMSType("NewTestType")
            .setJMSReplyTo(queue2)
            .send(queue1, content);

        QueueBrowser qb = jmsContextQCFBindings.createBrowser(queue1);
        int numMsgs = getMessageCount(qb);

        String recvdByteBody =
            Arrays.toString( jmsConsumer.receiveBodyNoWait( byte[].class ) );

        boolean testFailed = false;

        if ( (numMsgs != 1) ||
             !recvdByteBody.equals("[127, 0]") ||
             !producer.getJMSCorrelationID().equals("TestCorrelID") ||
             !producer.getJMSType().equals("NewTestType") ||
             (producer.getJMSReplyTo() != queue2) ) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContextQCFBindings.close();

        if ( testFailed ) {
            throw new Exception("testJMSProducerSendByteMessage_B_SecOff failed");
        }
    }

    public void testJMSProducerSendByteMessage_TCP_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFTCP = qcfTCP.createContext();
        emptyQueue(qcfTCP, queue1);
        JMSConsumer jmsConsumer = jmsContextQCFTCP.createConsumer(queue1);
        JMSProducer producer = jmsContextQCFTCP.createProducer();

        byte[] content = new byte[] { 127, 0 };

        producer
            .setJMSCorrelationID("TestCorrelID")
            .setJMSType("NewTestType")
            .setJMSReplyTo(queue2)
            .send(queue1, content);

        QueueBrowser qb = jmsContextQCFTCP.createBrowser(queue1);
        int numMsgs = getMessageCount(qb);

        String recvdByteBody =
            Arrays.toString( jmsConsumer.receiveBodyNoWait( byte[].class ) );

        boolean testFailed = false;

        if ( (numMsgs != 1) ||
             !recvdByteBody.equals("[127, 0]") ||
             !producer.getJMSCorrelationID().equals("TestCorrelID") ||
             !producer.getJMSType().equals("NewTestType") ||
             (producer.getJMSReplyTo() != queue2) ) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContextQCFTCP.close();

        if ( testFailed ) {
            throw new Exception("testJMSProducerSendByteMessage_TCP_SecOff failed");
        }
    }

    // 118071_4_3_Queue InvalidDestinationRuntimeException - if a client uses
    // this method with an invalid destination.

    public void testJMSProducerSendByteMessage_InvalidDestination_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFBindings = qcfBindings.createContext();
        emptyQueue(qcfBindings, queue1);
        JMSConsumer jmsConsumer = jmsContextQCFBindings.createConsumer(queue1);
        JMSProducer producer = jmsContextQCFBindings.createProducer();

        boolean testFailed = false;
        try {
            byte[] content = new byte[] { 127, 0 };
            producer
                .setJMSCorrelationID("TestCorrelID")
                .setJMSType("NewTestType")
                .setJMSReplyTo(queue2)
                .send(null, content);
            testFailed = true;
        } catch ( InvalidDestinationRuntimeException ex ) {
            ex.printStackTrace();
        }

        jmsConsumer.close();
        jmsContextQCFBindings.close();

        if ( testFailed ) {
            throw new Exception("testJMSProducerSendByteMessage_InvalidDestination_B_SecOff failed");
        }
    }

    public void testJMSProducerSendByteMessage_InvalidDestination_TCP_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFTCP = qcfTCP.createContext();
        emptyQueue(qcfTCP, queue1);
        JMSConsumer jmsConsumer = jmsContextQCFTCP.createConsumer(queue1);
        JMSProducer producer = jmsContextQCFTCP.createProducer();

        boolean testFailed = false;
        try {
            byte[] content = new byte[] { 127, 0 };
            producer
                .setJMSCorrelationID("TestCorrelID")
                .setJMSType("NewTestType")
                .setJMSReplyTo(queue2)
                .send(null, content);
            testFailed = true;
        } catch ( InvalidDestinationRuntimeException ex ) {
            ex.printStackTrace();
        }

        jmsConsumer.close();
        jmsContextQCFTCP.close();

        if ( testFailed ) {
            throw new Exception("testJMSProducerSendByteMessage_InvalidDestination_TCP_SecOff failed");
        }
    }

    // 118071_4_4_Queue If a null value is specified then a BytesMessage with no
    // body will be sent.

    public void testJMSProducerSendByteMessage_Null_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFBindings = qcfBindings.createContext();
        emptyQueue(qcfBindings, queue1);
        JMSConsumer jmsConsumer = jmsContextQCFBindings.createConsumer(queue1);

        JMSProducer producer = jmsContextQCFBindings.createProducer();

        byte[] content = null;
        producer
            .setJMSCorrelationID("TestCorrelID")
            .setJMSType("NewTestType")
            .setJMSReplyTo(queue2)
            .send(queue1, content);

        Message msg = jmsConsumer.receive(30000);

        boolean testFailed = false;
        if ( (msg.getBody(byte[].class) != null) ||
             !producer.getJMSCorrelationID().equals("TestCorrelID") ||
             !producer.getJMSType().equals("NewTestType") ||
             (producer.getJMSReplyTo() != queue2) ) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContextQCFBindings.close();

        if ( testFailed ) {
            throw new Exception("testJMSProducerSendByteMessage_Null_B_SecOff failed");
        }
    }

    public void testJMSProducerSendByteMessage_Null_TCP_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFTCP = qcfTCP.createContext();
        emptyQueue(qcfTCP, queue1);
        JMSConsumer jmsConsumer = jmsContextQCFTCP.createConsumer(queue1);
        JMSProducer producer = jmsContextQCFTCP.createProducer();

        byte[] content = null;

        producer
            .setJMSCorrelationID("TestCorrelID")
            .setJMSType("NewTestType")
            .setJMSReplyTo(queue2)
            .send(queue1, content);

        Message msg = jmsConsumer.receive(30000);

        boolean testFailed = false;

        if ( (msg.getBody(byte[].class) != null) ||
             !producer.getJMSCorrelationID().equals("TestCorrelID") ||
             !producer.getJMSType().equals("NewTestType") ||
             (producer.getJMSReplyTo() != queue2) ) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContextQCFTCP.close();

        if ( testFailed ) {
            throw new Exception("testJMSProducerSendByteMessage_Null_TCP_SecOff failed");
        }
    }

    // 118071_4_5_Topic Send a BytesMessage with the specified body to the
    // specified topic, using any send options, message properties and message
    // headers that have been defined on this JMSProducer.

    public void testJMSProducerSendByteMessage_Topic_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {
        testJMSProducerSendByteMessage_Topic_SecOff(tcfBindings);
    }

    public void testJMSProducerSendByteMessage_Topic_TCP_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {
        testJMSProducerSendByteMessage_Topic_SecOff(tcfTCP);
     }

    private void testJMSProducerSendByteMessage_Topic_SecOff(TopicConnectionFactory topicConnectionFactory)
            throws JMSException, TestException, InterruptedException {
           
        try (JMSContext jmsContext = topicConnectionFactory.createContext()) {
            JMSConsumer jmsConsumer = jmsContext.createConsumer(topic1);
            JMSProducer jmsProducer = jmsContext.createProducer();

            byte[] sentMessageBody = new String(methodName() + " at " + new Date()).getBytes();
            
            jmsProducer.setJMSCorrelationID("TestCorrelID")
                       .setJMSType("NewTestType")
                       .send(topic1, sentMessageBody);

            byte[] receivedMessageBody = jmsConsumer.receiveBodyNoWait(byte[].class);
            for (int i = 0; i<10 && receivedMessageBody == null; i++) {
                receivedMessageBody = jmsConsumer.receiveBodyNoWait(byte[].class);
                Thread.sleep(100);
            }
            if (receivedMessageBody == null)
                throw new TestException("No message received, sent:"+sentMessageBody);             
            if (!Arrays.equals(receivedMessageBody, sentMessageBody) )   
                throw new TestException("Wrong message received:" + receivedMessageBody + " sent:" + sentMessageBody);     
            if (    !jmsProducer.getJMSCorrelationID().equals("TestCorrelID")
                 || !jmsProducer.getJMSType().equals("NewTestType") ) {
                throw new TestException("Invalid jmsProducer:"+jmsProducer);
            }
        }
    }
    
    // 118071_4_7_Topic InvalidDestinationRuntimeException - if a client uses
    // this method with an invalid destination.

    public void testJMSProducerSendByteMessage_InvalidDestination_Topic_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {
        testJMSProducerSendByteMessage_InvalidDestination_Topic_SecOff(tcfBindings);
    }

    public void testJMSProducerSendByteMessage_InvalidDestination_Topic_TCP_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {
        testJMSProducerSendByteMessage_InvalidDestination_Topic_SecOff(tcfTCP);
    }

    private void testJMSProducerSendByteMessage_InvalidDestination_Topic_SecOff(TopicConnectionFactory topicConnectionFactory)
        throws TestException {
      
        try (JMSContext jmsContext = topicConnectionFactory.createContext()) {
            JMSProducer jmsProducer = jmsContext.createProducer();

            byte[] sentMessageBody = new String(methodName() + " at " + new Date()).getBytes();                
            try {
                jmsProducer.setJMSCorrelationID("TestCorrelID")
                        .setJMSType("NewTestType")
                        .send(null, sentMessageBody);
                // Should not reach here.
                throw new TestException("Sent a message to a null destination without throwing InvalidDestinationRuntimeException");
            
            } catch ( InvalidDestinationRuntimeException ex ) {
                // Expected
            }
        }
    }
    
    // 118071_4_8_Topic If a null value is specified then a BytesMessage with no
    // body will be sent.

    public void testJMSProducerSendByteMessage_Null_Topic_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {
        testJMSProducerSendByteMessage_Null_Topic_SecOff(tcfBindings);
    }

    public void testJMSProducerSendByteMessage_Null_Topic_TCP_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {
        testJMSProducerSendByteMessage_Null_Topic_SecOff(tcfTCP);
    }

    private void testJMSProducerSendByteMessage_Null_Topic_SecOff(TopicConnectionFactory topicConnectionFactory)
        throws JMSException, TestException {
          
        try (JMSContext jmsContext = topicConnectionFactory.createContext()) {
            JMSConsumer jmsConsumer = jmsContext.createConsumer(topic1);
            JMSProducer jmsProducer = jmsContext.createProducer();

            byte[] sentMessageBody = null;
            jmsProducer.setJMSCorrelationID("TestCorrelID")
                       .setJMSType("NewTestType")
                       .send(topic1, sentMessageBody);

            Message receivedMessage = jmsConsumer.receive(30000);

            if (receivedMessage == null)                    
                throw new TestException("No message received, sent:"+sentMessageBody);
            if (    receivedMessage.getBody(byte[].class) != null 
                || !receivedMessage.getJMSCorrelationID().equals("TestCorrelID") 
                || !receivedMessage.getJMSType().equals("NewTestType"))
                throw new TestException("Wrong message received:"+receivedMessage+" sent:"+sentMessageBody); 
        }
    }
        
    // 118071_5 JMSProducer send(Destination destination,Serializable body)
    // 118071_5_1_Queue Send an ObjectMessage with the specified body to the
    // specified queue using any send options, message properties and message
    // headers that have been defined on this JMSProducer.

    public void testJMSProducerSendObjectMessage_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFBindings = qcfBindings.createContext();
        emptyQueue(qcfBindings, queue1);
        JMSConsumer jmsConsumer = jmsContextQCFBindings.createConsumer(queue1);
        JMSProducer producer = jmsContextQCFBindings.createProducer();

        Object objBody = "This is the Message body.";
        producer.setJMSCorrelationID("TestCorrelID")
            .setJMSType("NewTestType").setJMSReplyTo(queue2)
            .send(queue1, (Serializable) objBody);

        QueueBrowser qb = jmsContextQCFBindings.createBrowser(queue1);
        int numMsgs = getMessageCount(qb);

        Object msgRecvd = jmsConsumer.receiveBodyNoWait(Serializable.class);

        boolean testFailed = false;
        if ( (numMsgs != 1) ||
             !msgRecvd.equals(objBody) ||
             !producer.getJMSCorrelationID().equals("TestCorrelID") ||
             !producer.getJMSType().equals("NewTestType") ||
             (producer.getJMSReplyTo() != queue2) ) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContextQCFBindings.close();

        if ( testFailed ) {
            throw new Exception("testJMSProducerSendObjectMessage_B_SecOff failed");
        }
    }

    public void testJMSProducerSendObjectMessage_TCP_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFTCP = qcfTCP.createContext();
        emptyQueue(qcfTCP, queue1);
        JMSConsumer jmsConsumer = jmsContextQCFTCP.createConsumer(queue1);
        JMSProducer producer = jmsContextQCFTCP.createProducer();

        Object objBody = "This is the Message body.";
        producer.setJMSCorrelationID("TestCorrelID")
            .setJMSType("NewTestType").setJMSReplyTo(queue2)
            .send(queue1, (Serializable) objBody);

        QueueBrowser qb = jmsContextQCFTCP.createBrowser(queue1);
        int numMsgs = getMessageCount(qb);

        Object msgRecvd = jmsConsumer.receiveBodyNoWait(Serializable.class);

        boolean testFailed = false;
        if ( (numMsgs != 1) ||
             !msgRecvd.equals(objBody) ||
             !producer.getJMSCorrelationID().equals("TestCorrelID") ||
             !producer.getJMSType().equals("NewTestType") ||
             (producer.getJMSReplyTo() != queue2) ) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContextQCFTCP.close();

        if ( testFailed ) {
            throw new Exception("testJMSProducerSendObjectMessage_TCP_B_SecOff failed");
        }
    }

    // 118071_5_3_Queue InvalidDestinationRuntimeException - if a client uses
    // this method with an invalid queue.

    public void testJMSProducerSendObjectMessage_InvalidDestination_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFBindings = qcfBindings.createContext();
        emptyQueue(qcfBindings, queue1);
        JMSConsumer jmsConsumer = jmsContextQCFBindings.createConsumer(queue1);
        JMSProducer producer = jmsContextQCFBindings.createProducer();

        boolean testFailed = false;
        try {
            Object objBody = "This is the Message body.";
            producer.setJMSCorrelationID("TestCorrelID")
                            .setJMSType("NewTestType").setJMSReplyTo(queue2)
                            .send(null, (Serializable) objBody);
            testFailed = true;
        } catch ( InvalidDestinationRuntimeException ex ) {
            ex.printStackTrace();
        }

        jmsConsumer.close();
        jmsContextQCFBindings.close();

        if ( testFailed ) {
            throw new Exception("testJMSProducerSendObjectMessage_InvalidDestination_B_SecOff failed");
        }
    }

    public void testJMSProducerSendObjectMessage_InvalidDestination_TCP_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFTCP = qcfTCP.createContext();
        emptyQueue(qcfTCP, queue1);
        JMSConsumer jmsConsumer = jmsContextQCFTCP.createConsumer(queue1);
        JMSProducer producer = jmsContextQCFTCP.createProducer();

        boolean testFailed = false;
        try {
            Object objBody = "This is the Message body.";
            producer.setJMSCorrelationID("TestCorrelID")
                .setJMSType("NewTestType").setJMSReplyTo(queue2)
                .send(null, (Serializable) objBody);
            testFailed = true;
        } catch ( InvalidDestinationRuntimeException ex ) {
            ex.printStackTrace();
        }

        jmsConsumer.close();
        jmsContextQCFTCP.close();

        if ( testFailed ) {
            throw new Exception("testJMSProducerSendObjectMessage_InvalidDestination_TCP_SecOff failed");
        }
    }

    // 118071_4_4_Queue If a null value is specified then a BytesMessage with no
    // body will be sent.

    public void testJMSProducerSendObjectMessage_Null_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFBindings = qcfBindings.createContext();
        emptyQueue(qcfBindings, queue1);
        JMSConsumer jmsConsumer = jmsContextQCFBindings.createConsumer(queue1);
        JMSProducer producer = jmsContextQCFBindings.createProducer();

        Object objBody = null;
        producer.setJMSCorrelationID("TestCorrelID")
            .setJMSType("NewTestType").setJMSReplyTo(queue2)
            .send(queue1, (Serializable) objBody);

        QueueBrowser qb = jmsContextQCFBindings.createBrowser(queue1);
        int numMsgs = getMessageCount(qb);

        boolean testFailed = false;
        try {
            Object msgRecvd = jmsConsumer.receiveBodyNoWait(Serializable.class);
            testFailed = true;
        } catch ( MessageFormatRuntimeException ex ) {
            ex.printStackTrace();
        }

        jmsConsumer.close();
        jmsContextQCFBindings.close();

        if ( testFailed ) {
            throw new Exception("testJMSProducerSendObjectMessage_Null_B_SecOff failed");
        }
    }

    public void testJMSProducerSendObjectMessage_Null_TCP_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFTCP = qcfTCP.createContext();
        emptyQueue(qcfTCP, queue1);
        JMSConsumer jmsConsumer = jmsContextQCFTCP.createConsumer(queue1);
        JMSProducer producer = jmsContextQCFTCP.createProducer();

        Object objBody = null;
        producer.setJMSCorrelationID("TestCorrelID")
            .setJMSType("NewTestType").setJMSReplyTo(queue2)
            .send(queue1, (Serializable) objBody);

        QueueBrowser qb = jmsContextQCFTCP.createBrowser(queue1);
        int numMsgs = getMessageCount(qb);

        boolean testFailed = false;
        try {
            Object msgRecvd = jmsConsumer.receiveBodyNoWait(Serializable.class);
            testFailed = true;
        } catch ( MessageFormatRuntimeException ex ) {
            ex.printStackTrace();
        }

        jmsConsumer.close();
        jmsContextQCFTCP.close();

        if ( testFailed ) {
            throw new Exception("testJMSProducerSendObjectMessage_Null_TCP_SecOff failed");
        }
    }

    // 118071_5_5_Topic Send an ObjectMessage with the specified body to the
    // specified topic using any send options, message properties and message
    // headers that have been defined on this JMSProducer.

    public void testJMSProducerSendObjectMessage_Topic_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {
        testJMSProducerSendObjectMessage_Topic_SecOff(tcfBindings);   
    }

    public void testJMSProducerSendObjectMessage_Topic_TCP_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {
        testJMSProducerSendObjectMessage_Topic_SecOff(tcfTCP);
    }

    private void testJMSProducerSendObjectMessage_Topic_SecOff(TopicConnectionFactory topicConnectionFactory)
        throws TestException, InterruptedException {
            
        try (JMSContext jmsContext = topicConnectionFactory.createContext()) {
            JMSConsumer jmsConsumer = jmsContext.createConsumer(topic1);
            JMSProducer jmsProducer = jmsContext.createProducer();
            
            Object sentMessageBody = new String(methodName()+" at "+new Date());
            jmsProducer.setJMSCorrelationID("TestCorrelID")
                       .setJMSType("NewTestType")
                       .send(topic1, (Serializable) sentMessageBody);
            
            Object receivedMessageBody = null;
            for (int i = 0; i<10 && receivedMessageBody == null; i++) {
              receivedMessageBody = jmsConsumer.receiveBodyNoWait(Serializable.class);
              Thread.sleep(100);
            }

            if (receivedMessageBody == null)
                throw new TestException("No message received, sent:"+sentMessageBody); 
            if (!receivedMessageBody.equals(sentMessageBody))
                throw new TestException("Wrong message received:"+receivedMessageBody+" sent:"+sentMessageBody);
            if (   !jmsProducer.getJMSCorrelationID().equals("TestCorrelID")
                || !jmsProducer.getJMSType().equals("NewTestType") )
                 throw new TestException("Invalid jmsProducer:"+jmsProducer);
        }
    }
    
    // 118071_5_7_Topic InvalidDestinationRuntimeException - if a client uses
    // this method with an invalid Topic.

    public void testJMSProducerSendObjectMessage_InvalidDestination_Topic_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {
        testJMSProducerSendObjectMessage_InvalidDestination_Topic_SecOff(tcfBindings);
    }

    public void testJMSProducerSendObjectMessage_InvalidDestination_Topic_TCP_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {
        testJMSProducerSendObjectMessage_InvalidDestination_Topic_SecOff(tcfTCP); 
    }

    private void testJMSProducerSendObjectMessage_InvalidDestination_Topic_SecOff(TopicConnectionFactory topicConnectionFactory)
            throws TestException {
            
        try (JMSContext jmsContext = topicConnectionFactory.createContext()) {
            JMSProducer jmsProducer = jmsContext.createProducer();

            try {
                Object sentMessageBody = methodName() + " at " + new Date();                
                jmsProducer.setJMSCorrelationID("TestCorrelID")
                           .setJMSType("NewTestType")
                           .send(null, (Serializable) sentMessageBody);
                // Should not reach here.
                throw new TestException("Sent message to null topic without throwing InvalidDestinationRuntimeException, sent:" + sentMessageBody);
            
            } catch ( InvalidDestinationRuntimeException ex ) {
                // Expected
            }           
        }
    }
        
    // 118071_5_8_Topic If a null value is specified then an ObjectMessage with
    // no body will be sent.

    public void testJMSProducerSendObjectMessage_Null_Topic_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {
        testJMSProducerSendObjectMessage_Null_Topic_SecOff(tcfBindings);
    }

    public void testJMSProducerSendObjectMessage_Null_Topic_TCP_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {
        testJMSProducerSendObjectMessage_Null_Topic_SecOff(tcfTCP);
    }

    private void testJMSProducerSendObjectMessage_Null_Topic_SecOff(TopicConnectionFactory topicConnectionFactory)
        throws TestException, InterruptedException {
            
        try (JMSContext jmsContext = topicConnectionFactory.createContext()) {
            JMSConsumer jmsConsumer = jmsContext.createConsumer(topic1);        
            JMSProducer jmsProducer = jmsContext.createProducer();

            Serializable sentMessageBody = null;
            jmsProducer.setJMSCorrelationID("TestCorrelID")
                       .setJMSType("NewTestType")
                       .send(topic1, (Serializable) sentMessageBody);

            try {
                // Repeat attempts to receive the message.
                Serializable receivedMessageBody = null;
                for (int i = 0; i<10 && receivedMessageBody == null; i++) {
                    receivedMessageBody = jmsConsumer.receiveBodyNoWait(Serializable.class);
                    Thread.sleep(100);
                }
                // Should not reach here.
                // We reach here if we either didn't receive a message after 10 attempts or we received 
                // the TextMessage with no body, instead of throwing MessageFormatRuntimeException.  
                throw new TestException("Wrong message received:" + receivedMessageBody+ "sent:"+sentMessageBody);
            
            } catch (MessageFormatRuntimeException e) {
                // Expected
            }
        }
    }
    
    // 118073_1_1 Clears any message properties set on this JMSProducer

    /*
     * public void testClearProperties(
     *     HttpServletRequest request, * HttpServletResponse response) throws Throwable {
     *
     * boolean testFailed=false;
     * 
     * JMSContext jmsContextQCFBindings = qcfBindings .createContext();
     * JMSProducer jmsProducer = jmsContextQCFBindings.createProducer();
     * 
     * jmsProducer.setProperty("BooleanValue", true);
     * 
     * jmsProducer.setProperty("StringValue", "Tester");
     * 
     * byte propValue = 100;
     * 
     * jmsProducer.setProperty("ByteValue", propValue);
     * 
     * jmsProducer.setProperty("DoubleValue", 123.4);
     * 
     * jmsProducer.setProperty("FloatValue", 123.4f);
     * 
     * jmsProducer.setProperty("IntValue", 11111);
     * 
     * jmsProducer.setProperty("LongValue", 1234567890123456L);
     * 
     * short propShort = 32760;
     * 
     * jmsProducer.setProperty("ShortValue", propShort);
     * 
     * jmsProducer.setProperty("ObjectValue", new Integer(1414));
     * 
     * boolean bval = jmsProducer.getBooleanProperty("BooleanValue");
     * System.out.print("Before clearProperties, boolean value set is :"
     * + bval);
     * 
     * String propValue = jmsProducer.getStringProperty("StringValue");
     * System.out.println("Before clearProperties , String value set is :"
     * + propValue);
     * 
     * Byte byval = jmsProducer.getByteProperty("ByteValue");
     * System.out.print("Before clearProperties , Byte value set is :"
     * + byval);
     * 
     * double dval = jmsProducer.getDoubleProperty("DoubleValue");
     * System.out.print("Before clearProperties , Double value set is :"
     * + dval);
     * 
     * float fval = jmsProducer.getFloatProperty("FloatValue");
     * System.out.print("Before clearProperties , Float value set is :"
     * + fval);
     * 
     * int ival = jmsProducer.getIntProperty("IntValue");
     * System.out.print("Before clearProperties , Integer value set is :"
     * + ival);
     * 
     * long lval = jmsProducer.getLongProperty("LongValue");
     * System.out.print("Before clearProperties , Long value set is :"
     * + lval);
     * 
     * short shval = jmsProducer.getShortProperty("ShortValue");
     * System.out.print("Before clearProperties , Short value set is :"
     * + shval);
     * 
     * Object oval = jmsProducer.getObjectProperty("ObjectValue");
     * System.out.print("Before clearProperties , Object value set is :"
     * + oval);
     * 
     * jmsProducer.clearProperties();
     * 
     * boolean bval1 = jmsProducer.getBooleanProperty("BooleanValue");
     * System.out.print("After clearProperties, boolean value set is :"
     * + bval1);
     * 
     * String propValue1 = jmsProducer.getStringProperty("StringValue");
     * System.out.println("After clearProperties , String value set is :"
     * + propValue1);
     * 
     * Byte byval1 = jmsProducer.getByteProperty("ByteValue");
     * System.out.print("After clearProperties , Byte value set is :"
     * + byval1);
     * 
     * double dval1 = jmsProducer.getDoubleProperty("DoubleValue");
     * System.out.print("After clearProperties , Double value set is :"
     * + dval1);
     * 
     * float fval1 = jmsProducer.getFloatProperty("FloatValue");
     * System.out.print("After clearProperties , Float value set is :"
     * + fval1);
     * 
     * int ival1 = jmsProducer.getIntProperty("IntValue");
     * System.out.print("After clearProperties , Integer value set is :"
     * + ival1);
     * 
     * long lval1 = jmsProducer.getLongProperty("LongValue");
     * System.out.print("After clearProperties , Long value set is :"
     * + lval1);
     * 
     * short shval1 = jmsProducer.getShortProperty("ShortValue");
     * System.out.print("After clearProperties , Short value set is :"
     * + shval1);
     * 
     * Object oval1 = jmsProducer.getObjectProperty("ObjectValue");
     * System.out.print("After clearProperties , Object value set is :"
     * + oval1);
     * 
     * // if(bval1==false && propValue1==null)
     * // setflag("testClearProperties", true);
     * 
     * if (jmsContext != null)
     * jmsContext.close();
     * } catch ( Exception ex ) {
     * ex.printStackTrace();
     * }
     * 
     * }
     * 
     * public void testClearProperties_TCP(
     *     HttpServletRequest request, * HttpServletResponse response) throws Throwable {
     *
     * try {
     * QueueConnectionFactory cf1 = (QueueConnectionFactory) new InitialContext()
     * .lookup("java:comp/env/jndi_JMS_BASE_QCF1");
     * 
     * Queue queue = (queue1) new InitialContext()
     * .lookup("java:comp/env/jndi_INPUT_Q");
     * 
     * JMSContext jmsContext = cf1.createContext();
     * JMSProducer jmsProducer = jmsContext.createProducer();
     * 
     * jmsProducer.setProperty("BooleanValue", true);
     * 
     * jmsProducer.setProperty("StringValue", "Tester");
     * 
     * jmsProducer.setProperty("ByteValue", 100);
     * 
     * jmsProducer.setProperty("DoubleValue", 123.4);
     * 
     * jmsProducer.setProperty("FloatValue", 123.4f);
     * 
     * jmsProducer.setProperty("IntValue", 11111);
     * 
     * jmsProducer.setProperty("LongValue", 1234567890123456L);
     * 
     * jmsProducer.setProperty("ShortValue", 32760);
     * 
     * jmsProducer.setProperty("ObjectValue", new Integer(1414));
     * 
     * boolean bval = jmsProducer.getBooleanProperty("BooleanValue");
     * System.out.print("Before clearProperties, boolean value set is :"
     * + bval);
     * 
     * String propValue = jmsProducer.getStringProperty("StringValue");
     * System.out.println("Before clearProperties , String value set is :"
     * + propValue);
     * 
     * Byte byval = jmsProducer.getByteProperty("ByteValue");
     * System.out.print("Before clearProperties , Byte value set is :"
     * + byval);
     * 
     * double dval = jmsProducer.getDoubleProperty("DoubleValue");
     * System.out.print("Before clearProperties , Double value set is :"
     * + dval);
     * 
     * float fval = jmsProducer.getFloatProperty("FloatValue");
     * System.out.print("Before clearProperties , Float value set is :"
     * + fval);
     * 
     * int ival = jmsProducer.getIntProperty("IntValue");
     * System.out.print("Before clearProperties , Integer value set is :"
     * + ival);
     * 
     * long lval = jmsProducer.getLongProperty("LongValue");
     * System.out.print("Before clearProperties , Long value set is :"
     * + lval);
     * 
     * short shval = jmsProducer.getShortProperty("ShortValue");
     * System.out.print("Before clearProperties , Short value set is :"
     * + shval);
     * 
     * Object oval = jmsProducer.getObjectProperty("ObjectValue");
     * System.out.print("Before clearProperties , Object value set is :"
     * + oval);
     * 
     * jmsProducer.clearProperties();
     * 
     * boolean bval1 = jmsProducer.getBooleanProperty("BooleanValue");
     * System.out.print("After clearProperties, boolean value set is :"
     * + bval1);
     * 
     * String propValue1 = jmsProducer.getStringProperty("StringValue");
     * System.out.println("After clearProperties , String value set is :"
     * + propValue1);
     * 
     * Byte byval1 = jmsProducer.getByteProperty("ByteValue");
     * System.out.print("After clearProperties , Byte value set is :"
     * + byval1);
     * 
     * double dval1 = jmsProducer.getDoubleProperty("DoubleValue");
     * System.out.print("After clearProperties , Double value set is :"
     * + dval1);
     * 
     * float fval1 = jmsProducer.getFloatProperty("FloatValue");
     * System.out.print("After clearProperties , Float value set is :"
     * + fval1);
     * 
     * int ival1 = jmsProducer.getIntProperty("IntValue");
     * System.out.print("After clearProperties , Integer value set is :"
     * + ival1);
     * 
     * long lval1 = jmsProducer.getLongProperty("LongValue");
     * System.out.print("After clearProperties , Long value set is :"
     * + lval1);
     * 
     * short shval1 = jmsProducer.getShortProperty("ShortValue");
     * System.out.print("After clearProperties , Short value set is :"
     * + shval1);
     * 
     * Object oval1 = jmsProducer.getObjectProperty("ObjectValue");
     * System.out.print("After clearProperties , Object value set is :"
     * + oval1);
     * 
     * // if(bval1==false && propValue1==null)
     * setflag("testClearProperties_TCP", true);
     * 
     * if (jmsContext != null)
     * jmsContext.close();
     * } catch ( Exception ex ) {
     * ex.printStackTrace();
     * }
     * 
     * }
     */

    // 118073_1_2 Test invoking clearProperties() when there are no properties set
    // 118073_1_3 Test invoking clearProperties() soon after clearProperties() have been invoked

    public void testClearProperties_Notset_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFBindings = qcfBindings.createContext();
        JMSProducer jmsProducer = jmsContextQCFBindings.createProducer();

        boolean testFailed = false;

        try {
            jmsProducer.clearProperties();
        } catch ( Exception ex ) {
            ex.printStackTrace();
            testFailed = true;
        }

        jmsProducer.setProperty("Name", "Tester");
        jmsProducer.setProperty("ObjectType", new Integer(1414));
        jmsProducer.clearProperties();
        try {
            jmsProducer.clearProperties();
        } catch ( Exception ex ) {
            ex.printStackTrace();
            testFailed = true;
        }

        jmsContextQCFBindings.close();

        if ( testFailed ) {
            throw new Exception("testClearProperties_Notset_B_SecOff failed");
        }
    }

    public void testClearProperties_Notset_TCP_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFTCP = qcfTCP.createContext();
        JMSProducer jmsProducer = jmsContextQCFTCP.createProducer();

        boolean testFailed = false;

        try {
            jmsProducer.clearProperties();
        } catch ( Exception ex ) {
            ex.printStackTrace();
            testFailed = true;
        }

        jmsProducer.setProperty("Name", "Tester");
        jmsProducer.setProperty("ObjectType", new Integer(1414));
        jmsProducer.clearProperties();
        try {
            jmsProducer.clearProperties();
        } catch ( Exception ex ) {
            ex.printStackTrace();
            testFailed = true;
        }

        jmsContextQCFTCP.close();

        if ( testFailed ) {
            throw new Exception("testClearProperties_Notset_TCP_SecOff failed");
        }
    }

    // 118073_2 boolean propertyExists(String name)
    // 118073_2_1 Returns true if a message property with the specified name has
    // been set on this JMSProducer

    public void testPropertyExists_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFBindings = qcfBindings.createContext();
        JMSProducer jmsProducer = jmsContextQCFBindings.createProducer();

        boolean foundRandom = jmsProducer.propertyExists("RandomProperty");

        jmsProducer.setProperty("SetString", "Tester");
        boolean foundDefinite = jmsProducer.propertyExists("SetString");

        boolean testFailed = false;
        if ( foundRandom || !foundDefinite ) {
            testFailed = true;
        }

        jmsContextQCFBindings.close();

        if ( testFailed ) {
            throw new Exception("testPropertyExists_B_SecOff failed");
        }
    }

    public void testPropertyExists_TCP_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFTCP = qcfTCP.createContext();
        JMSProducer jmsProducer = jmsContextQCFTCP.createProducer();

        boolean foundRandom = jmsProducer.propertyExists("RandomProperty");

        jmsProducer.setProperty("SetString", "Tester");
        boolean foundDefinite = jmsProducer.propertyExists("SetString");

        boolean testFailed = false;
        if ( foundRandom || !foundDefinite ) {
            testFailed = true;
        }

        jmsContextQCFTCP.close();

        if ( testFailed ) {
            throw new Exception("testPropertyExists_TCP_SecOff failed");
        }
    }

    // 118073_2_2 Test by passing name as empty string

    public void testPropertyExists_emptyString_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFBindings = qcfBindings.createContext();
        JMSProducer jmsProducer = jmsContextQCFBindings.createProducer();

        boolean found = jmsProducer.propertyExists("");

        boolean testFailed = false;
        if ( found ) {
            testFailed = true;
        }

        jmsContextQCFBindings.close();

        if ( testFailed ) {
            throw new Exception("testPropertyExists_emptyString_B_SecOff failed");
        }
    }

    public void testPropertyExists_emptyString_TCP_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFTCP = qcfTCP.createContext();
        JMSProducer jmsProducer = jmsContextQCFTCP.createProducer();

        boolean found = jmsProducer.propertyExists("");

        boolean testFailed = false;
        if ( found ) {
            testFailed = true;
        }

        jmsContextQCFTCP.close();

        if ( testFailed ) {
            throw new Exception("testPropertyExists_emptyString_TCP_SecOff failed");
        }
    }

    // 118073_2_3 Test by passing name as null

    public void testPropertyExists_null_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFBindings = qcfBindings.createContext();
        JMSProducer jmsProducer = jmsContextQCFBindings.createProducer();

        boolean found = jmsProducer.propertyExists(null);

        boolean testFailed = false;
        if ( found ) {
            testFailed = true;
        }

        jmsContextQCFBindings.close();

        if ( testFailed ) {
            throw new Exception("testPropertyExists_null_B_SecOff failed");
        }
    }

    public void testPropertyExists_null_TCP_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFTCP = qcfTCP.createContext();
        JMSProducer jmsProducer = jmsContextQCFTCP.createProducer();

        boolean found = jmsProducer.propertyExists(null);

        boolean testFailed = false;
        if ( found ) {
            testFailed = true;
        }

        jmsContextQCFTCP.close();

        if ( testFailed ) {
            throw new Exception("testPropertyExists_null_TCP_SecOff failed");
        }
    }

    // 118073_3 JMSProducer setDisableMessageID(boolean value)
    // 118073_3_2 Message IDs are enabled by default.
    // 118073_4_1 Gets an indication of whether message IDs are disabled.

    public void testSetDisableMessageID_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFBindings = qcfBindings.createContext();
        emptyQueue(qcfBindings, queue1);
        JMSConsumer jmsConsumer = jmsContextQCFBindings.createConsumer(queue1);
        JMSProducer jmsProducer = jmsContextQCFBindings.createProducer();

        boolean testFailed = false;

        boolean defaultSetMessageID = jmsProducer.getDisableMessageID();
        if ( defaultSetMessageID ) {
            testFailed = true;
        }

        jmsProducer.setDisableMessageID(true);

        TextMessage tmsg = jmsContextQCFBindings.createTextMessage();
        jmsProducer.send(queue1, tmsg);

        String msgID = jmsConsumer.receive(30000).getJMSMessageID();
        if ( msgID != null ) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContextQCFBindings.close();

        if ( testFailed ) {
            throw new Exception("testSetDisableMessageID_B_SecOff failed");
        }
    }

    public void testSetDisableMessageID_TCP_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFTCP = qcfTCP.createContext();
        emptyQueue(qcfTCP, queue1);
        JMSConsumer jmsConsumer = jmsContextQCFTCP.createConsumer(queue1);
        JMSProducer jmsProducer = jmsContextQCFTCP.createProducer();

        boolean testFailed = false;

        boolean defaultSetMessageID = jmsProducer.getDisableMessageID();
        if ( defaultSetMessageID ) {
            testFailed = true;
        }

        jmsProducer.setDisableMessageID(true);

        TextMessage tmsg = jmsContextQCFTCP.createTextMessage();
        jmsProducer.send(queue1, tmsg);

        String msgID = jmsConsumer.receive(30000).getJMSMessageID();
        if ( msgID != null ) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContextQCFTCP.close();

        if ( testFailed ) {
            throw new Exception("testSetDisableMessageID_TCP_SecOff failed");
        }
    }

    // 118073_5 JMSProducer setDisableMessageTimestamp(boolean value)
    // 118073_6 boolean getDisableMessageTimestamp()

    public void testSetDisableMessageTimestamp_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFBindings = qcfBindings.createContext();
        emptyQueue(qcfBindings, queue1);
        JMSConsumer jmsConsumer = jmsContextQCFBindings.createConsumer(queue1);
        JMSProducer jmsProducer = jmsContextQCFBindings.createProducer();

        TextMessage tmsg = jmsContextQCFBindings.createTextMessage();

        boolean testFailed = false;

        boolean defaultSetMessageTimestamp = jmsProducer.getDisableMessageTimestamp();
        if ( defaultSetMessageTimestamp ) {
            testFailed = true;
        }

        jmsProducer.setDisableMessageTimestamp(true);
        jmsProducer.send(queue1, tmsg);
        long msgTS = jmsConsumer.receive(30000).getJMSTimestamp();

        if ( msgTS != 0 ) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsConsumer.close();
        jmsContextQCFBindings.close();

        if ( testFailed ) {
            throw new Exception("testSetDisableMessageTimestamp_B_SecOff failed");
        }
    }

    public void testSetDisableMessageTimestamp_TCP_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFTCP = qcfTCP.createContext();
        emptyQueue(qcfTCP, queue1);
        JMSConsumer jmsConsumer = jmsContextQCFTCP.createConsumer(queue1);
        JMSProducer jmsProducer = jmsContextQCFTCP.createProducer();

        TextMessage tmsg = jmsContextQCFTCP.createTextMessage();

        boolean testFailed = false;

        boolean defaultSetMessageTimestamp = jmsProducer.getDisableMessageTimestamp();
        if ( defaultSetMessageTimestamp ) {
            testFailed = true;
        }

        jmsProducer.setDisableMessageTimestamp(true);
        jmsProducer.send(queue1, tmsg);

        long msgTS = jmsConsumer.receive(30000).getJMSTimestamp();
        if ( msgTS != 0 ) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContextQCFTCP.close();

        if ( testFailed ) {
            throw new Exception("testSetDisableMessageTimestamp_TCP_SecOff failed");
        }
    }

    // 118073_7 JMSProducer setDeliveryMode(int deliveryMode)
    // 118073_7_1 Specifies the delivery mode of messages that are sent using
    // this JMSProducer

    public void testSetDeliveryMode_persistent_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFBindings = qcfBindings.createContext();
        JMSProducer jmsProducer = jmsContextQCFBindings.createProducer();

        boolean defaultValue = false;
        if ( jmsProducer.getDeliveryMode() == DeliveryMode.PERSISTENT ) {
            defaultValue = true;
        }

        TextMessage tmsg = jmsContextQCFBindings.createTextMessage();
        jmsProducer.send(queue1, tmsg);

        jmsContextQCFBindings.close();

        // No test of the delivery mode default value?
    }

    public void testSetDeliveryMode_nonpersistent_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFBindings = qcfBindings.createContext();
        JMSProducer jmsProducer = jmsContextQCFBindings.createProducer();

        boolean setValue = false;
        jmsProducer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
        if ( jmsProducer.getDeliveryMode() == DeliveryMode.NON_PERSISTENT ) {
            setValue = true;
        }

        TextMessage tmsg = jmsContextQCFBindings.createTextMessage();
        jmsProducer.send(queue1, tmsg);

        jmsContextQCFBindings.close();

        // No test of the delivery mode assignment?
    }

    public void testBrowseDeliveryMode_persistent_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFBindings = qcfBindings.createContext();
        QueueBrowser qb = jmsContextQCFBindings.createBrowser(queue1);
        int numMsgs = getMessageCount(qb);

        boolean testFailed = false;
        if ( numMsgs != 1 ) {
            testFailed = true;
        }

        JMSConsumer jmsConsumer = jmsContextQCFBindings.createConsumer(queue1);
        jmsConsumer.receive(30000);

        qb.close();
        jmsConsumer.close();
        jmsContextQCFBindings.close();

        if ( testFailed ) {
            throw new Exception("testBrowseDeliveryMode_persistent_B_SecOff failed");
        }
    }

    public void testBrowseDeliveryMode_nonpersistent_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFBindings = qcfBindings.createContext();

        QueueBrowser qb = jmsContextQCFBindings.createBrowser(queue1);

        int numMsgs = 0; // Why not: getMessageCount(qb);

        boolean testFailed = false;
        if ( numMsgs != 0 ) {
            testFailed = true;
        }

        qb.close();
        jmsContextQCFBindings.close();

        if ( testFailed ) {
            throw new Exception("testBrowseDeliveryMode_nonpersistent_B_SecOff failed");
        }
    }

    public void testSetDeliveryMode_nonpersistent_TCP_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFTCP = qcfTCP.createContext();
        JMSProducer jmsProducer = jmsContextQCFTCP.createProducer();

        jmsProducer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);

        boolean setValue = false;
        if ( jmsProducer.getDeliveryMode() == DeliveryMode.NON_PERSISTENT ) {
            setValue = true;
        }

        TextMessage tmsg = jmsContextQCFTCP.createTextMessage();
        jmsProducer.send(queue1, tmsg);

        jmsContextQCFTCP.close();

        // No test of the delivery mode assignment?
    }

    public void testSetDeliveryMode_persistent_TCP_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFTCP = qcfTCP.createContext();
        JMSProducer jmsProducer = jmsContextQCFTCP.createProducer();

        boolean defaultValue = false;
        if ( jmsProducer.getDeliveryMode() == DeliveryMode.PERSISTENT ) {
            defaultValue = true;
        }

        TextMessage tmsg = jmsContextQCFTCP.createTextMessage();
        jmsProducer.send(queue1, tmsg);

        jmsContextQCFTCP.close();

        // No test of the delivery mode default value?
    }

    public void testBrowseDeliveryMode_persistent_TCP_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFTCP = qcfTCP.createContext();

        QueueBrowser qb = jmsContextQCFTCP.createBrowser(queue1);

        int numMsgs = getMessageCount(qb);

        boolean testFailed = false;
        if ( numMsgs != 1 ) {
            testFailed = true;
        }

        JMSConsumer jmsConsumer = jmsContextQCFTCP.createConsumer(queue1);
        jmsConsumer.receive(30000);

        qb.close();
        jmsConsumer.close();
        jmsContextQCFTCP.close();

        if ( testFailed ) {
            throw new Exception("testBrowseDeliveryMode_persistent_TCP_SecOff failed");
        }
    }

    public void testBrowseDeliveryMode_nonpersistent_TCP_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFBindings = qcfBindings.createContext();
        QueueBrowser qb = jmsContextQCFBindings.createBrowser(queue1);

        int numMsgs = 0; // Why not: getMessageCount(qb);

        boolean testFailed = false;
        if ( numMsgs != 0 ) {
            testFailed = true;
        }

        qb.close();
        jmsContextQCFBindings.close();

        if ( testFailed ) {
            throw new Exception("testBrowseDeliveryMode_nonpersistent_TCP_SecOff failed");
        }
    }

    // 118073_7_3 Test with deliveryMode as -1
    // 118073_7_4 Test with deliveryMode with the largest number possible for int range
    // 118073_7_5 Test with deliveryMode as 0

    public void testDeliveryMode_Invalid_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFBindings = qcfBindings.createContext();
        JMSProducer jmsProducer = jmsContextQCFBindings.createProducer();

        boolean testFailed = false;

        try {
            jmsProducer.setDeliveryMode(-1);
            testFailed = true;
        } catch ( JMSRuntimeException ex ) {
            ex.printStackTrace();
            int delMode = jmsProducer.getDeliveryMode();
            if ( delMode != DeliveryMode.PERSISTENT ) {
                testFailed = true;
            }
        }

        try {
            jmsProducer.setDeliveryMode(2147483647);
            testFailed = true;
        } catch ( JMSRuntimeException ex ) {
            ex.printStackTrace();
            int delMode = jmsProducer.getDeliveryMode();
            if ( delMode != DeliveryMode.PERSISTENT ) {
                testFailed = true;
            }
        }

        try {
            jmsProducer.setDeliveryMode(0);
            testFailed = true;
        } catch ( JMSRuntimeException ex ) {
            ex.printStackTrace();
            int delMode = jmsProducer.getDeliveryMode();
            if ( delMode != DeliveryMode.PERSISTENT ) {
                testFailed = true;
            }
        }

        jmsContextQCFBindings.close();

        if ( testFailed ) {
            throw new Exception("testDeliveryMode_Invalid_B_SecOff failed");
        }
    }

    public void testDeliveryMode_Invalid_TCP_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFTCP = qcfTCP.createContext();
        JMSProducer jmsProducer = jmsContextQCFTCP.createProducer();

        boolean testFailed = false;

        try {
            jmsProducer.setDeliveryMode(-1);
            testFailed = true;
        } catch ( JMSRuntimeException ex ) {
            ex.printStackTrace();
            int delMode = jmsProducer.getDeliveryMode();
            if ( delMode != DeliveryMode.PERSISTENT ) {
                testFailed = true;
            }
        }

        try {
            jmsProducer.setDeliveryMode(2147483647);
            testFailed = true;
        } catch ( JMSRuntimeException ex ) {
            ex.printStackTrace();
            int delMode = jmsProducer.getDeliveryMode();
            if ( delMode != DeliveryMode.PERSISTENT ) {
                testFailed = true;
            }
        }

        try {
            jmsProducer.setDeliveryMode(0);
            testFailed = true;
        } catch ( JMSRuntimeException ex ) {
            ex.printStackTrace();
            int delMode = jmsProducer.getDeliveryMode();
            if ( delMode != DeliveryMode.PERSISTENT ) {
                testFailed = true;
            }
        }

        jmsContextQCFTCP.close();

        if ( testFailed ) {
            throw new Exception("testDeliveryMode_Invalid_TCP_SecOff failed");
        }
    }

    // 118073_9 JMSProducer setPriority(int priority)
    // 118073_9_1 Specifies the priority of messages that are sent using this JMSProducer
    // 118073_9_2 Priority is set to 4 by default.
    // 118073_10_1 Return the priority of messages that are sent using this JMSProducer

    public void testSetPriority_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFBindings = qcfBindings.createContext();
        emptyQueue(qcfBindings, queue1);
        JMSConsumer jmsConsumer = jmsContextQCFBindings.createConsumer(queue1);
        JMSProducer jmsProducer = jmsContextQCFBindings.createProducer();

        for ( int msgNo = 0; msgNo < 10; msgNo++ ) {
            TextMessage tmsg = jmsContextQCFBindings.createTextMessage();
            jmsProducer.setPriority(msgNo);
            jmsProducer.send(queue1, tmsg);
            System.out.println("Sent message [ " + msgNo + " ]");
        }

        boolean testFailed = false;

        QueueBrowser qb = jmsContextQCFBindings.createBrowser(queue1);
        Enumeration e = qb.getEnumeration();
        int numMsgs = 0;
        while ( e.hasMoreElements() && (numMsgs < 10) ) {
            TextMessage msgR = (TextMessage) jmsConsumer.receive(30000);
            if ( (msgR.getJMSPriority() + numMsgs) == 9 ) {
                System.out.print("Message received in correct order. Priority [ " + msgR.getJMSPriority() + " ]");
            } else {
                System.out.print("Message received in wrong order. Priority [ " + msgR.getJMSPriority() + " ]");
                testFailed = true;
            }
            numMsgs++;
        }

        jmsConsumer.close();
        jmsContextQCFBindings.close();

        if ( testFailed ) {
            throw new Exception("testSetPriority_B_SecOff failed");
        }
    }

    public void testSetPriority_TCP_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFTCP = qcfTCP.createContext();
        emptyQueue(qcfTCP, queue1);
        JMSConsumer jmsConsumer = jmsContextQCFTCP.createConsumer(queue1);
        JMSProducer jmsProducer = jmsContextQCFTCP.createProducer();

        for ( int msgNo = 0; msgNo < 10; msgNo++ ) {
            TextMessage tmsg = jmsContextQCFTCP.createTextMessage();
            jmsProducer.setPriority(msgNo);
            jmsProducer.send(queue1, tmsg);
            System.out.println("Sent message [ " + msgNo + " ]");
        }

        boolean testFailed = false;

        QueueBrowser qb = jmsContextQCFTCP.createBrowser(queue1);
        Enumeration e = qb.getEnumeration();
        int numMsgs = 0;
        while ( e.hasMoreElements() && (numMsgs < 10) ) {
            TextMessage msgR = (TextMessage) jmsConsumer.receive(30000);
            if ( (msgR.getJMSPriority() + numMsgs) == 9 ) {
                System.out.print("Message received in correct order. Priority [ " + msgR.getJMSPriority() + " ]");
            } else {
                System.out.print("Message received in wrong order. Priority [ " + msgR.getJMSPriority() + " ]");
                testFailed = true;
            }
            numMsgs++;
        }

        jmsConsumer.close();
        jmsContextQCFTCP.close();

        if ( testFailed ) {
            throw new Exception("testSetPriority_TCP_SecOff failed");
        }
    }

    // 118073_9_2 Priority is set to 4 by default.
    // 118073_9_3 Test setPriority with -1
    // 118073_9_4 test setPriority with boundary values set for int

    public void testSetPriority_default_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFBindings = qcfBindings.createContext();
        emptyQueue(qcfBindings, queue1);
        JMSConsumer jmsConsumer = jmsContextQCFBindings.createConsumer(queue1);
        JMSProducer jmsProducer = jmsContextQCFBindings.createProducer();

        TextMessage tmsg = jmsContextQCFBindings.createTextMessage();
        jmsProducer.send(queue1, tmsg);

        boolean testFailed = false;
        if ( jmsProducer.getPriority() != 4 ) {
            testFailed = true;
        }
        if ( jmsConsumer.receive(30000).getJMSPriority() != 4 ) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContextQCFBindings.close();

        if ( testFailed ) {
            throw new Exception("testSetPriority_default_B_SecOff failed");
        }
    }

    public void testSetPriority_default_TCP_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFTCP = qcfTCP.createContext();
        emptyQueue(qcfTCP, queue1);
        JMSConsumer jmsConsumer = jmsContextQCFTCP.createConsumer(queue1);
        JMSProducer jmsProducer = jmsContextQCFTCP.createProducer();

        TextMessage tmsg = jmsContextQCFTCP.createTextMessage();
        jmsProducer.send(queue1, tmsg);

        boolean testFailed = false;

        if ( jmsProducer.getPriority() != 4 ) {
            testFailed = true;
        }
        if ( jmsConsumer.receive(30000).getJMSPriority() != 4 ) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContextQCFTCP.close();

        if ( testFailed ) {
            throw new Exception("testSetPriority_default_TCP_SecOff failed");
        }
    }

    // 118073_9_3 Test setPriority with -1
    // 118073_9_4 test setPriority with boundary values set for int

    public void testSetPriority_variation_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFBindings = qcfBindings.createContext();
        emptyQueue(qcfBindings, queue1);
        JMSConsumer jmsConsumer = jmsContextQCFBindings.createConsumer(queue1);
        JMSProducer jmsProducer = jmsContextQCFBindings.createProducer();

        TextMessage tmsg = jmsContextQCFBindings.createTextMessage();

        boolean testFailed = false;

        try {
            jmsProducer.setPriority(-1);
            testFailed = true;
        } catch ( JMSRuntimeException ex ) {
            ex.printStackTrace();
        }

        try {
            jmsProducer.setPriority(2147483647);
            testFailed = true;
        } catch ( JMSRuntimeException ex ) {
            ex.printStackTrace();
        }

        jmsConsumer.close();
        jmsContextQCFBindings.close();

        if ( testFailed ) {
            throw new Exception("testSetPriority_variation_B_SecOff failed");
        }
    }

    public void testSetPriority_variation_TCP_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFTCP = qcfTCP.createContext();
        emptyQueue(qcfTCP, queue1);
        JMSConsumer jmsConsumer = jmsContextQCFTCP.createConsumer(queue1);
        JMSProducer jmsProducer = jmsContextQCFTCP.createProducer();

        boolean testFailed = false;

        try {
            jmsProducer.setPriority(-1);
            testFailed = true;
        } catch ( JMSRuntimeException ex ) {
            ex.printStackTrace();
        }

        try {
            jmsProducer.setPriority(2147483647);
            testFailed = true;
        } catch ( JMSRuntimeException ex ) {
            ex.printStackTrace();
        }

        jmsConsumer.close();
        jmsContextQCFTCP.close();

        if ( testFailed ) {
            throw new Exception("testSetPriority_variation_TCP_SecOff failed");
        }
    }

    // 118073_11 JMSProducer setTimeToLive(long timeToLive)
    // 118073_11_1 Specifies the time to live of messages that are sent using
    // this JMSProducer. This is used to determine the expiration time of a
    // message.
    // 118073_11_2 Clients should not receive messages that have expired;
    // however, JMS does not guarantee that this will not happen.
    // 118073_11_3 Time to live is set to zero by default, which means a message
    // never expires.
    // 118073_12_1 the message time to live in milliseconds; a value of zero
    // means that a message never expires.

    public void testSetTimeToLive_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFBindings = qcfBindings.createContext();
        emptyQueue(qcfBindings, queue1);
        JMSConsumer jmsConsumer = jmsContextQCFBindings.createConsumer(queue1);
        JMSProducer jmsProducer = jmsContextQCFBindings.createProducer();

        TextMessage msgOut = jmsContextQCFBindings.createTextMessage();

        long defaultTimeToLive = jmsProducer.getTimeToLive();
        System.out.println("Default time to live [ " + defaultTimeToLive + " ]");

        boolean testFailed = false;

        int shortTTL = 500;
        jmsProducer.setTimeToLive(shortTTL);
        jmsProducer.send(queue1, msgOut);
        try {
            Thread.sleep(shortTTL + 10000);
        } catch ( InterruptedException e ) {
            // Ignore
        }

        Message msgIn1 = jmsConsumer.receive(30000);
        if ( msgIn1 != null ) {
            System.out.println("Message did not expire within [ " + shortTTL + " ]");
            testFailed = true;
        } else {
            System.out.println("Message expired within [ " + shortTTL + " ]");
        }

        jmsProducer.setTimeToLive(0);
        jmsProducer.send(queue1, msgOut);
        try {
            Thread.sleep(10000);
        } catch ( InterruptedException e ) {
            // Ignore
        }

        Message msgIn2 = jmsConsumer.receive(30000);
        if ( msgIn2 != null ) {
            System.out.println("Message did not expire within [ " + 0 + " ]");
            testFailed = true;
        } else {
            System.out.println("Message expired within [ " + 0 + " ]");
        }

        jmsConsumer.close();
        jmsContextQCFBindings.close();

        if ( testFailed ) {
            throw new Exception("testSetTimeToLive_B_SecOff failed");
        }
    }

    private void displayProducer(JMSProducer jmsProducer, String tag) {
        System.out.println(tag + ": JMSProducer [ " + jmsProducer + " ]");
    }

    private void displayMessage(Message message, String tag) throws JMSException {
        System.out.println(tag + ": Message [ " + message + " ]");
    }

    public void testSetTimeToLive_TCP_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        String methodName = "testSetTimeToLive_TCP_SecOff";

        JMSContext jmsContextQCFTCP = qcfTCP.createContext();
        emptyQueue(qcfTCP, queue1);
        JMSConsumer jmsConsumer = jmsContextQCFTCP.createConsumer(queue1);
        JMSProducer jmsProducer = jmsContextQCFTCP.createProducer();

        // jmsProducer.setDeliveryMode(DeliveryMode.NON_PERSISTENT); // Doesn't change the test result.

        TextMessage msgOut = jmsContextQCFTCP.createTextMessage();
        displayMessage(msgOut, methodName + " Ougoing Message");

        boolean testFailed = false;

        long defaultTimeToLive = jmsProducer.getTimeToLive();
        System.out.println(methodName + ": Default time to live [ " + defaultTimeToLive + " ]");
        if ( defaultTimeToLive != 0 ) {
            testFailed = true;
        }

        long shortTTL = 500;
        jmsProducer.setTimeToLive(shortTTL);
        long setShortTTL = jmsProducer.getTimeToLive();
        if ( setShortTTL != shortTTL ) {
            System.out.println(methodName + ": Short TTL [ " + shortTTL + " ] set as [ " + setShortTTL + " ]");
            testFailed = true;
        }

        jmsProducer.send(queue1, msgOut);

        try {
            Thread.sleep(shortTTL + 10000);
        } catch ( InterruptedException e ) {
            System.out.println(methodName + ": Interrupted wait [ " + (shortTTL + 10000) + " ] for time-to-live set to [ " + shortTTL + " ]");
            testFailed = true;
        }

        Message msgIn1 = jmsConsumer.receive(30000); // This message is being received; it should time out.
        if ( msgIn1 != null ) {
            displayMessage(msgIn1, methodName + " Incoming Message [ TTL " + shortTTL + " ]");
            System.out.println(methodName + ": Message unexpectedly did not expire within [ " + shortTTL + " ]");
            testFailed = true;
        } else {
            System.out.println(methodName + ": Message expectedly expired within [ " + shortTTL + " ]");
        }

        jmsProducer.setTimeToLive(0L);
        long setZeroTTL = jmsProducer.getTimeToLive();
        if ( setZeroTTL != 0 ) {
            System.out.println(methodName + ": TTL [ 0 ] set as [ " + setZeroTTL + " ]");
            testFailed = true;
        }

        jmsProducer.send(queue1, msgOut);
        try {
            Thread.sleep(10000);
        } catch ( InterruptedException e ) {
            System.out.println(methodName + ": Interrupted wait [ " + 10000 + " ] for time-to-live set to [ 0 ]");
            testFailed = true;
        }

        Message msgIn2 = jmsConsumer.receive(30000);
        if ( msgIn2 != null ) {
            displayMessage(msgIn1, methodName + " Incoming Message [ TTL 0 ]");
            System.out.println(methodName + ": Message expectedly did not expire with time-to-live set to [ 0 ]");
        } else {
            System.out.println(methodName + ": Message unexpectedly expired with time-to-live set to [ 0 ]");
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContextQCFTCP.close();

        if ( testFailed ) {
            throw new Exception("testSetTimeToLive_TCP_SecOff failed");
        }
    }

    // 118073_11_4 Test with timeToLive as -1
    // 118073_11_6 Test with timeToLive set to boundary values for long

    public void testSetTimeToLive_Variation_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFBindings = qcfBindings.createContext();
        emptyQueue(qcfBindings, queue1);
        JMSProducer jmsProducer = jmsContextQCFBindings.createProducer();

        boolean testFailed = false;

        try {
            jmsProducer.setTimeToLive(-1);
            testFailed = true;
        } catch ( JMSRuntimeException ex ) {
            ex.printStackTrace();
        }

        try {
            jmsProducer.setTimeToLive(9223372036854775807L);
            testFailed = true;
        } catch ( JMSRuntimeException ex ) {
            ex.printStackTrace();
        }

        jmsContextQCFBindings.close();

        if ( testFailed ) {
            throw new Exception("testSetTimeToLive_Variation_B_SecOff failed");
        }
    }

    public void testSetTimeToLive_Variation_TCP_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFTCP = qcfTCP.createContext();
        emptyQueue(qcfTCP, queue1);
        JMSProducer jmsProducer = jmsContextQCFTCP.createProducer();

        boolean testFailed = false;

        try {
            jmsProducer.setTimeToLive(-1);
            testFailed = true;
        } catch ( JMSRuntimeException ex ) {
            ex.printStackTrace();
        }

        try {
            jmsProducer.setTimeToLive(9223372036854775807L);
            testFailed = true;
        } catch ( JMSRuntimeException ex ) {
            ex.printStackTrace();
        }

        jmsContextQCFTCP.close();

        if ( testFailed ) {
            throw new Exception("testSetTimeToLive_Variation_TCP_SecOff failed");
        }
    }

    // 118073_13_1 Specifies that messages sent using this JMSProducer will have
    // the specified property set to the specified boolean value.
    // 118073_13_2 Verify when this method is invoked when , it will replace any
    // property of the same name that is already set on the message being sent.
    // 118073_13_3 IllegalArgumentException - if the name is null or if the name
    // is an empty string.

    public void testSetBooleanProperty_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFBindings = qcfBindings.createContext();
        emptyQueue(qcfBindings, queue1);
        JMSConsumer jmsConsumer = jmsContextQCFBindings.createConsumer(queue1);
        JMSProducer jmsProducer = jmsContextQCFBindings.createProducer();

        String propName = "MyProp";
        boolean propValue = false;
        boolean propValueAfter = true;

        TextMessage msgOut = jmsContextQCFBindings.createTextMessage();
        msgOut.setBooleanProperty(propName, false);

        jmsProducer.setProperty(propName, true);

        jmsProducer.send(queue1, msgOut);

        TextMessage msgIn = (TextMessage) jmsConsumer.receive(30000);

        boolean testFailed = false;
        if ( !msgIn.getBooleanProperty(propName) ) {
            testFailed = true;
        }

        try {
            jmsProducer.setProperty("", false);
            testFailed = true;
        } catch ( IllegalArgumentException ex ) {
            ex.printStackTrace();
        }

        try {
            jmsProducer.setProperty(null, true);
            testFailed = true;
        } catch ( IllegalArgumentException ex ) {
            ex.printStackTrace();
        }

        jmsConsumer.close();
        jmsContextQCFBindings.close();

        if ( testFailed ) {
            throw new Exception("testSetBooleanProperty_B_SecOff failed");
        }
    }

    public void testSetBooleanProperty_TCP_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFTCP = qcfTCP.createContext();
        emptyQueue(qcfTCP, queue1);
        JMSConsumer jmsConsumer = jmsContextQCFTCP.createConsumer(queue1);
        JMSProducer jmsProducer = jmsContextQCFTCP.createProducer();

        String propName = "MyProp";

        TextMessage msgOut = jmsContextQCFTCP.createTextMessage();
        msgOut.setBooleanProperty(propName, false);

        jmsProducer.setProperty(propName, true);

        jmsProducer.send(queue1, msgOut);

        TextMessage msgIn = (TextMessage) jmsConsumer.receive(30000);

        boolean testFailed = false;

        if ( !msgIn.getBooleanProperty(propName) ) {
            testFailed = true;
        }

        try {
            jmsProducer.setProperty("", false);
            testFailed = true;
        } catch ( IllegalArgumentException ex ) {
            ex.printStackTrace();
        }

        try {
            jmsProducer.setProperty(null, true);
            testFailed = true;
        } catch ( IllegalArgumentException ex ) {
            ex.printStackTrace();
        }

        jmsConsumer.close();
        jmsContextQCFTCP.close();

        if ( testFailed ) {
            throw new Exception("testSetBooleanProperty_TCP_SecOff failed");
        }
    }

    // 118073_14_2 MessageFormatRuntimeException - if this type conversion is invalid.

    public void testGetBooleanProperty_MFRE_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFBindings = qcfBindings.createContext();
        emptyQueue(qcfBindings, queue1);
        JMSConsumer jmsConsumer = jmsContextQCFBindings.createConsumer(queue1);
        JMSProducer jmsProducer = jmsContextQCFBindings.createProducer();

        String propName = "MyProp";

        byte propValue = 21;
        jmsProducer.setProperty(propName, propValue);

        boolean testFailed = false;

        try {
            jmsProducer.getBooleanProperty(propName);
            testFailed = true;
        } catch ( MessageFormatRuntimeException ex ) {
            ex.printStackTrace();
        }

        try {
            jmsProducer.getBooleanProperty("");
            testFailed = true;
        } catch ( JMSRuntimeException ex ) {
            ex.printStackTrace();
        }

        try {
            jmsProducer.getBooleanProperty(null);
            testFailed = true;
        } catch ( JMSRuntimeException ex ) {
            ex.printStackTrace();
        }

        jmsConsumer.close();
        jmsContextQCFBindings.close();

        if ( testFailed ) {
            throw new Exception("testGetBooleanProperty_MFRE_B_SecOff failed");
        }
    }

    public void testGetBooleanProperty_MFRE_TCP_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFTCP = qcfTCP.createContext();
        emptyQueue(qcfTCP, queue1);
        JMSConsumer jmsConsumer = jmsContextQCFTCP.createConsumer(queue1);
        JMSProducer jmsProducer = jmsContextQCFTCP.createProducer();

        String propName = "MyProp";

        byte propValue = 21;
        jmsProducer.setProperty(propName, propValue);

        boolean testFailed = false;

        try {
            jmsProducer.getBooleanProperty(propName);
            testFailed = true;
        } catch ( MessageFormatRuntimeException ex ) {
            ex.printStackTrace();
        }

        try {
            jmsProducer.getBooleanProperty("");
            testFailed = true;
        } catch ( JMSRuntimeException ex ) {
            ex.printStackTrace();
        }

        try {
            jmsProducer.getBooleanProperty(null);
            testFailed = true;
        } catch ( JMSRuntimeException ex ) {
            ex.printStackTrace();
        }

        jmsConsumer.close();
        jmsContextQCFTCP.close();

        if ( testFailed ) {
            throw new Exception("testGetBooleanProperty_MFRE_TCP_SecOff failed");
        }
    }

    // 118073_15 JMSProducer setProperty(String name, byte value)
    // 118073_15_1 Specifies that messages sent using this JMSProducer will have
    // the specified property set to the specified byte value.
    // 118073_15_2 Test when this invoked it will replace any property of the
    // same name that is already set on the message being sent.

    public void testSetByteProperty_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFBindings = qcfBindings.createContext();
        emptyQueue(qcfBindings, queue1);
        JMSConsumer jmsConsumer = jmsContextQCFBindings.createConsumer(queue1);
        JMSProducer jmsProducer = jmsContextQCFBindings.createProducer();

        Message msg = jmsContextQCFBindings.createMessage();

        String propName = "MyProp";

        byte propValue1 = 21;
        msg.setByteProperty(propName, propValue1);

        byte propValue2 = 100;
        jmsProducer.setProperty(propName, propValue2);

        jmsProducer.send(queue1, msg);

        byte actualPropValue = jmsConsumer.receive(30000).getByteProperty(propName);

        boolean testFailed = false;
        if ( actualPropValue != propValue2 ) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContextQCFBindings.close();

        if ( testFailed ) {
            throw new Exception("testSetByteProperty_B_SecOff failed");
        }
    }

    public void testSetByteProperty_TCP_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFTCP = qcfTCP.createContext();
        emptyQueue(qcfTCP, queue1);
        JMSConsumer jmsConsumer = jmsContextQCFTCP.createConsumer(queue1);
        JMSProducer jmsProducer = jmsContextQCFTCP.createProducer();

        Message msg = jmsContextQCFTCP.createMessage();

        String propName = "MyProp";

        byte propValue1 = 21;
        msg.setByteProperty(propName, propValue1);

        byte propValue2 = 100;
        jmsProducer.setProperty(propName, propValue2);

        jmsProducer.send(queue1, msg);

        byte actualPropValue = jmsConsumer.receive(30000).getByteProperty(propName);

        boolean testFailed = false;
        if ( actualPropValue != propValue2 ) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContextQCFTCP.close();

        if ( testFailed ) {
            throw new Exception("testSetByteProperty_TCP_SecOff failed");
        }
    }

    // 118073_15_3 IllegalArgumentException - if the name is null or if the name
    // is an empty string.

    public void testSetByteProperty_variation_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFBindings = qcfBindings.createContext();
        JMSProducer jmsProducer = jmsContextQCFBindings.createProducer();

        byte propValue = 10;

        boolean testFailed = false;

        try {
            jmsProducer.setProperty("", propValue);
            testFailed = true;
        } catch ( IllegalArgumentException ex ) {
            ex.printStackTrace();
        }

        try {
            jmsProducer.setProperty(null, propValue);
            testFailed = true;
        } catch ( IllegalArgumentException ex ) {
            ex.printStackTrace();
        }

        jmsContextQCFBindings.close();

        if ( testFailed ) {
            throw new Exception("testSetByteProperty_variation_B_SecOff failed");
        }
    }

    public void testSetByteProperty_variation_TCP_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFTCP = qcfTCP.createContext();
        JMSProducer jmsProducer = jmsContextQCFTCP.createProducer();

        byte propValue = 10;

        boolean testFailed = false;

        try {
            jmsProducer.setProperty("", propValue);
            testFailed = true;
        } catch ( IllegalArgumentException ex ) {
            ex.printStackTrace();
        }

        try {
            jmsProducer.setProperty(null, propValue);
            testFailed = true;
        } catch ( IllegalArgumentException ex ) {
            ex.printStackTrace();
        }

        jmsContextQCFTCP.close();

        if ( testFailed ) {
            throw new Exception("testSetByteProperty_variation_TCP_SecOff failed");
        }
    }

    // 118073_16_2 MessageFormatRuntimeException - if this type conversion is invalid.
    // byte getByteProperty(String name)

    public void testGetByteProperty_MFRE_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFBindings = qcfBindings.createContext();
        JMSProducer jmsProducer = jmsContextQCFBindings.createProducer();

        String propName = "PropName";

        boolean testFailed = false;
        jmsProducer.setProperty(propName, 376790);
        try {
            jmsProducer.getByteProperty(propName);
            testFailed = true;
        } catch ( MessageFormatRuntimeException ex ) {
            ex.printStackTrace();
        }

        jmsContextQCFBindings.close();

        if ( testFailed ) {
            throw new Exception("testGetByteProperty_MFRE_B_SecOff failed");
        }
    }

    public void testGetByteProperty_MFRE_TCP_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFTCP = qcfTCP.createContext();
        JMSProducer jmsProducer = jmsContextQCFTCP.createProducer();

        String propName = "PropName";

        boolean testFailed = false;
        jmsProducer.setProperty(propName, 376790);
        try {
            jmsProducer.getByteProperty(propName);
            testFailed = true;
        } catch ( MessageFormatRuntimeException ex ) {
            ex.printStackTrace();
        }

        jmsContextQCFTCP.close();

        if ( testFailed ) {
            throw new Exception("testGetByteProperty_MFRE_TCP_SecOff failed");
        }
    }

    // 118073_17 JMSProducer setProperty(String name, short value)
    // 118073_17_1 Specifies that messages sent using this JMSProducer will have
    // the specified property set to the specified short value.
    // 118073_17_2 Invoking this method will replace any property of the same
    // name that is already set on the message being sent.

    public void testSetShortProperty_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFBindings = qcfBindings.createContext();
        emptyQueue(qcfBindings, queue1);
        JMSConsumer jmsConsumer = jmsContextQCFBindings.createConsumer(queue1);
        JMSProducer jmsProducer = jmsContextQCFBindings.createProducer();

        TextMessage tmsg = jmsContextQCFBindings.createTextMessage();

        String propName = "MyProp";

        short propValue1 = 21;
        tmsg.setShortProperty(propName, propValue1);

        short propValue2 = 21;
        jmsProducer.setProperty(propName, propValue2);

        jmsProducer.send(queue1, tmsg);

        Short actualPropValue = jmsConsumer.receive(30000).getShortProperty(propName);

        boolean testFailed = false;
        if ( actualPropValue != propValue2 ) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContextQCFBindings.close();

        if ( testFailed ) {
            throw new Exception("testSetShortProperty_B_SecOff failed");
        }
    }

    public void testSetShortProperty_TCP_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFTCP = qcfTCP.createContext();
        emptyQueue(qcfTCP, queue1);
        JMSConsumer jmsConsumer = jmsContextQCFTCP.createConsumer(queue1);
        JMSProducer jmsProducer = jmsContextQCFTCP.createProducer();

        TextMessage tmsg = jmsContextQCFTCP.createTextMessage();

        String propName = "MyProp";

        short propValue1 = 21;
        tmsg.setShortProperty(propName, propValue1);

        short propValue2 = 21;
        jmsProducer.setProperty(propName, propValue2);

        jmsProducer.send(queue1, tmsg);

        Short actualPropValue = jmsConsumer.receive(30000).getShortProperty(propName);

        boolean testFailed = false;
        if ( actualPropValue != propValue2 ) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContextQCFTCP.close();

        if ( testFailed ) {
            throw new Exception("testSetShortProperty_TCP_SecOff failed");
        }
    }

    // 118073_17_3 IllegalArgumentException - if the name is null or if the name
    // is an empty string.

    public void testSetShortProperty_Null_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFBindings = qcfBindings.createContext();
        JMSProducer jmsProducer = jmsContextQCFBindings.createProducer();

        short propValue = 20;

        boolean testFailed = false;

        try {
            jmsProducer.setProperty("", propValue);
            testFailed = true;
        } catch ( IllegalArgumentException ex ) {
            ex.printStackTrace();
        }

        try {
            jmsProducer.setProperty(null, propValue);
            testFailed = true;
        } catch ( IllegalArgumentException ex ) {
            ex.printStackTrace();
        }

        jmsContextQCFBindings.close();

        if ( testFailed ) {
            throw new Exception("testSetShortProperty_Null_B_SecOff failed");
        }
    }

    public void testSetShortProperty_Null_TCP_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFTCP = qcfTCP.createContext();
        JMSProducer jmsProducer = jmsContextQCFTCP.createProducer();

        short propValue = 20;

        boolean testFailed = false;

        try {
            jmsProducer.setProperty("", propValue);
            testFailed = true;
        } catch ( IllegalArgumentException ex ) {
            ex.printStackTrace();
        }

        try {
            jmsProducer.setProperty(null, propValue);
            testFailed = true;
        } catch ( IllegalArgumentException ex ) {
            ex.printStackTrace();
        }

        jmsContextQCFTCP.close();

        if ( testFailed ) {
            throw new Exception("testSetShortProperty_Null_B_SecOff failed");
        }
    }

    // 118073_17_4 Test with "value" set as 0
    // 118073_17_5 Test with "value" set as -1
    // 118073_17_6 Test with "value" set to boundary values for short

    public void testSetShortProperty_Variation_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        boolean testFailed = false;

        JMSContext jmsContextQCFBindings = qcfBindings.createContext();
        JMSProducer jmsProducer = jmsContextQCFBindings.createProducer();

        String propName = "ShortValue";

        short expectedPropValue1 = -1;
        jmsProducer.setProperty(propName, expectedPropValue1);
        short actualPropValue1 = jmsProducer.getShortProperty(propName);

        short expectedPropValue2 = 0;
        jmsProducer.setProperty(propName, expectedPropValue2);
        short actualPropValue2 = jmsProducer.getShortProperty(propName);

        short expectedPropValue3 = 127;
        jmsProducer.setProperty(propName, expectedPropValue3);
        short actualPropValue3 = jmsProducer.getShortProperty(propName);

        if ( (actualPropValue1 != expectedPropValue1) ||
             (actualPropValue2 != expectedPropValue2) ||
             (actualPropValue3 != expectedPropValue3) ) {
            testFailed = true;
        }

        jmsContextQCFBindings.close();

        if ( testFailed ) {
            throw new Exception("testSetShortProperty_Variation_B_SecOff failed");
        }
    }

    public void testSetShortProperty_Variation_TCP_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFTCP = qcfTCP.createContext();
        JMSProducer jmsProducer = jmsContextQCFTCP.createProducer();

        String propName = "ShortValue";

        short expectedPropValue1 = -1;
        jmsProducer.setProperty(propName, expectedPropValue1);
        short actualPropValue1 = jmsProducer.getShortProperty(propName);

        short expectedPropValue2 = 0;
        jmsProducer.setProperty(propName, expectedPropValue2);
        short actualPropValue2 = jmsProducer.getShortProperty(propName);

        short expectedPropValue3 = 127;
        jmsProducer.setProperty(propName, expectedPropValue3);
        short actualPropValue3 = jmsProducer.getShortProperty(propName);

        boolean testFailed = false;
        if ( (actualPropValue1 != expectedPropValue1) ||
             (actualPropValue2 != expectedPropValue2) ||
             (actualPropValue3 != expectedPropValue3) ) {
            testFailed = true;
        }

        jmsContextQCFTCP.close();

        if ( testFailed ) {
            throw new Exception("testSetShortProperty_Variation_TCP_SecOff failed");
        }
    }

    // 118073_18_2 MessageFormatRuntimeException - if this type conversion is invalid.

    public void testGetShortProperty_MFRE_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFBindings = qcfBindings.createContext();
        JMSProducer jmsProducer = jmsContextQCFBindings.createProducer();

        String propName = "propertyBoolean";

        jmsProducer.setProperty(propName, true);

        boolean testFailed = false;
        try {
            jmsProducer.getShortProperty(propName);
            testFailed = true;
        } catch ( MessageFormatRuntimeException ex ) {
            ex.printStackTrace();
        }

        jmsContextQCFBindings.close();

        if ( testFailed ) {
            throw new Exception("testGetShortProperty_MFRE_B_SecOff failed");
        }
    }

    public void testGetShortProperty_MFRE_TCP_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFTCP = qcfTCP.createContext();
        JMSProducer jmsProducer = jmsContextQCFTCP.createProducer();

        String propName = "propertyBoolean";

        jmsProducer.setProperty(propName, true);

        boolean testFailed = false;
        try {
            jmsProducer.getShortProperty(propName);
            testFailed = true;
        } catch ( MessageFormatRuntimeException ex ) {
            ex.printStackTrace();
        }

        jmsContextQCFTCP.close();

        if ( testFailed ) {
            throw new Exception("testGetShortProperty_MFRE_TCP_SecOff failed");
        }
    }

    // 118073_21 JMSProducer setProperty(String name, int value)
    // 118073_21_1 Specifies that messages sent using this JMSProducer will have
    // the specified property set to the specified int value.
    // 118073_21_2 Invoking this method will replace any property of the same
    // name that is already set on the message being sent.

    public void testSetIntProperty_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFBindings = qcfBindings.createContext();
        emptyQueue(qcfBindings, queue1);
        JMSConsumer jmsConsumer = jmsContextQCFBindings.createConsumer(queue1);
        JMSProducer jmsProducer = jmsContextQCFBindings.createProducer();

        TextMessage tmsg = jmsContextQCFBindings.createTextMessage();

        String propName = "MyProp";

        int propVal1 = 21;
        tmsg.setIntProperty(propName, propVal1);

        int propValue2 = 21;
        jmsProducer.setProperty(propName, propValue2);

        jmsProducer.send(queue1, tmsg);

        int actualPropValue = jmsConsumer.receive(30000).getIntProperty(propName);

        boolean testFailed = false;
        if ( actualPropValue != propValue2 ) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContextQCFBindings.close();

        if ( testFailed ) {
            throw new Exception("testSetIntProperty_B_SecOff failed");
        }
    }

    public void testSetIntProperty_TCP_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFTCP = qcfTCP.createContext();
        emptyQueue(qcfTCP, queue1);
        JMSConsumer jmsConsumer = jmsContextQCFTCP.createConsumer(queue1);
        JMSProducer jmsProducer = jmsContextQCFTCP.createProducer();

        TextMessage tmsg = jmsContextQCFTCP.createTextMessage();

        String propName = "MyProp";

        int propValue1 = 21;
        tmsg.setIntProperty(propName, propValue1);

        int propValue2 = 21;
        jmsProducer.setProperty(propName, propValue2);

        jmsProducer.send(queue1, tmsg);

        int actualPropValue = jmsConsumer.receive(30000).getIntProperty(propName);

        boolean testFailed = false;
        if ( actualPropValue != propValue2 ) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContextQCFTCP.close();

        if ( testFailed ) {
            throw new Exception("testSetIntProperty_TCP_SecOff failed");
        }
    }

    // 118073_19_3 IllegalArgumentException - if the name is null or if the name
    // is an empty string.

    public void testSetIntProperty_Null_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFBindings = qcfBindings.createContext();
        JMSProducer jmsProducer = jmsContextQCFBindings.createProducer();

        int propValue = 20;

        boolean testFailed = false;

        try {
            jmsProducer.setProperty("", propValue);
            testFailed = true;
        } catch ( IllegalArgumentException ex ) {
            ex.printStackTrace();
        }

        try {
            jmsProducer.setProperty(null, propValue);
            testFailed = true;
        } catch ( IllegalArgumentException ex ) {
            ex.printStackTrace();
        }

        jmsContextQCFBindings.close();

        if ( testFailed ) {
            throw new Exception("testSetIntProperty_Null_B_SecOff failed");
        }
    }

    public void testSetIntProperty_Null_TCP_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFTCP = qcfTCP.createContext();
        JMSProducer jmsProducer = jmsContextQCFTCP.createProducer();

        int propValue = 20;

        boolean testFailed = false;

        try {
            jmsProducer.setProperty("", propValue);
            testFailed = true;
        } catch ( IllegalArgumentException ex ) {
            ex.printStackTrace();
        }

        try {
            jmsProducer.setProperty(null, propValue);
            testFailed = true;
        } catch ( IllegalArgumentException ex ) {
            ex.printStackTrace();
        }

        jmsContextQCFTCP.close();

        if ( testFailed ) {
            throw new Exception("testSetIntProperty_Null_TCP_SecOff failed");
        }
    }

    // 118073_19_4 Test with "value" set as 0
    // 118073_19_5 Test with "value" set as -1
    // 118073_19_6 Test with "value" set to boundary values for short

    public void testSetIntProperty_Variation_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        boolean testFailed = false;
        JMSContext jmsContextQCFBindings = qcfBindings.createContext();
        JMSProducer jmsProducer = jmsContextQCFBindings.createProducer();

        String propName = "intValue";

        int expectedPropValue1 = -1;
        jmsProducer.setProperty(propName, expectedPropValue1);
        int actualPropValue1 = jmsProducer.getIntProperty(propName);

        int expectedPropValue2 = 0;
        jmsProducer.setProperty(propName, expectedPropValue2);
        int actualPropValue2 = jmsProducer.getIntProperty(propName);

        int expectedPropValue3 = 2147483647;
        jmsProducer.setProperty(propName, expectedPropValue3);
        int actualPropValue3 = jmsProducer.getIntProperty(propName);

        if ( (actualPropValue1 != expectedPropValue1) ||
             (actualPropValue2 != expectedPropValue2) ||
             (actualPropValue3 != expectedPropValue3) ) {
            testFailed = true;
        }

        jmsContextQCFBindings.close();

        if ( testFailed ) {
            throw new Exception("testSetIntProperty_Variation_B_SecOff failed");
        }
    }

    public void testSetIntProperty_Variation_TCP_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFTCP = qcfTCP.createContext();
        JMSProducer jmsProducer = jmsContextQCFTCP.createProducer();

        String propName = "intValue";

        int expectedPropValue1 = -1;
        jmsProducer.setProperty(propName, expectedPropValue1);
        int actualPropValue1 = jmsProducer.getIntProperty(propName);

        int expectedPropValue2 = 0;
        jmsProducer.setProperty(propName, expectedPropValue2);
        int actualPropValue2 = jmsProducer.getIntProperty(propName);

        int expectedPropValue3 = 2147483647;
        jmsProducer.setProperty(propName, expectedPropValue3);
        int actualPropValue3 = jmsProducer.getIntProperty(propName);

        boolean testFailed = false;
        if ( (actualPropValue1 != expectedPropValue1) ||
             (actualPropValue2 != expectedPropValue2) ||
             (actualPropValue3 != expectedPropValue3) ) {
            testFailed = true;
        }

        jmsContextQCFTCP.close();

        if ( testFailed ) {
            throw new Exception("testSetIntProperty_Variation_TCP_SecOff failed");
        }
    }

    // 118073_22_2 MessageFormatRuntimeException - if this type conversion is
    // invalid.

    public void testSetIntProperty_MFRE_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFBindings = qcfBindings.createContext();
        JMSProducer jmsProducer = jmsContextQCFBindings.createProducer();

        jmsProducer.setProperty("propName", 1234567890123456L);

        boolean testFailed = false;
        try {
            int propValue = jmsProducer.getIntProperty("propName");
            testFailed = true;
        } catch ( MessageFormatRuntimeException ex ) {
            ex.printStackTrace();
        }
        jmsContextQCFBindings.close();

        if ( testFailed ) {
            throw new Exception("testSetIntProperty_MFRE_B_SecOff failed");
        }
    }

    public void testSetIntProperty_MFRE_TCP_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFTCP = qcfTCP.createContext();
        JMSProducer jmsProducer = jmsContextQCFTCP.createProducer();
        TextMessage tmsg = jmsContextQCFTCP.createTextMessage();

        String propName = "propName";

        jmsProducer.setProperty(propName, 1234567890123456L);

        boolean testFailed = false;
        try {
            int propValue = jmsProducer.getIntProperty(propName);
            testFailed = true;
        } catch ( MessageFormatRuntimeException ex ) {
            ex.printStackTrace();
        }

        jmsContextQCFTCP.close();

        if ( testFailed ) {
            throw new Exception("testSetIntProperty_MFRE_TCP_SecOff failed");
        }
    }

    // 118073_23 JMSProducer setProperty(String name, long value)
    // 118073_23_1 Specifies that messages sent using this JMSProducer will have
    // the specified property set to the specified long value.
    // 118073_23_2 Invoking this method will replace any property of the same
    // name that is already set on the message being sent.

    public void testSetLongProperty_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFBindings = qcfBindings.createContext();
        emptyQueue(qcfBindings, queue1);
        JMSConsumer jmsConsumer = jmsContextQCFBindings.createConsumer(queue1);
        JMSProducer jmsProducer = jmsContextQCFBindings.createProducer();

        TextMessage tmsg = jmsContextQCFBindings.createTextMessage();

        String propName = "MyProp";

        long propValue1 = 1234567890123456L;
        tmsg.setLongProperty(propName, propValue1);

        long propValue2 = 1234567890654321L;
        jmsProducer.setProperty(propName, propValue2);

        jmsProducer.send(queue1, tmsg);

        long actualPropValue = jmsConsumer.receive(30000).getLongProperty(propName);

        boolean testFailed = false;
        if ( actualPropValue != propValue2 ) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContextQCFBindings.close();

        if ( testFailed ) {
            throw new Exception("testSetLongProperty_B_SecOff failed");
        }
    }

    public void testSetLongProperty_TCP_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFTCP = qcfTCP.createContext();
        emptyQueue(qcfTCP, queue1);
        JMSConsumer jmsConsumer = jmsContextQCFTCP.createConsumer(queue1);
        JMSProducer jmsProducer = jmsContextQCFTCP.createProducer();

        TextMessage tmsg = jmsContextQCFTCP.createTextMessage();

        String propName = "MyProp";

        long propValue1 = 1234567890123456L;
        tmsg.setLongProperty(propName, propValue1);

        long propValue2 = 1234567890654321L;
        jmsProducer.setProperty(propName, propValue2);

        jmsProducer.send(queue1, tmsg);

        long actualPropValue = jmsConsumer.receive(30000).getLongProperty(propName);

        boolean testFailed = false;
        if ( actualPropValue != propValue2 ) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContextQCFTCP.close();

        if ( testFailed ) {
            throw new Exception("testSetLongProperty_TCP_SecOff failed");
        }
    }

    // 118073_23_3 IllegalArgumentException - if the name is null or if the name
    // is an empty string.

    public void testSetLongProperty_Null_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFBindings = qcfBindings.createContext();
        JMSProducer jmsProducer = jmsContextQCFBindings.createProducer();

        long val = 1234567890654321L;

        boolean testFailed = false;

        try {
            jmsProducer.setProperty("", val);
            testFailed = true;
        } catch ( IllegalArgumentException ex ) {
            ex.printStackTrace();
        }

        try {
            jmsProducer.setProperty(null, val);
            testFailed = true;
        } catch ( IllegalArgumentException ex ) {
            ex.printStackTrace();
        }

        jmsContextQCFBindings.close();

        if ( testFailed ) {
            throw new Exception("testSetLongProperty_Null_B_SecOff failed");
        }
    }

    public void testSetLongProperty_Null_TCP_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFTCP = qcfTCP.createContext();
        JMSProducer jmsProducer = jmsContextQCFTCP.createProducer();

        long val = 1234567890654321L;

        boolean testFailed = false;

        try {
            jmsProducer.setProperty("", val);
            testFailed = true;
        } catch ( IllegalArgumentException ex ) {
            ex.printStackTrace();
        }

        try {
            jmsProducer.setProperty(null, val);
            testFailed = true;
        } catch ( IllegalArgumentException ex ) {
            ex.printStackTrace();
        }

        jmsContextQCFTCP.close();

        if ( testFailed ) {
            throw new Exception("testSetLongProperty_Null_TCP_SecOff failed");
        }
    }

    // 118073_23_4 Test when "value" is set as 0
    // 118073_23_5 Test when 'value" is set as -1
    // 118073_23_6 Test when "value" is set to boundary values allowed for long

    public void testSetLongProperty_Variation_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFBindings = qcfBindings.createContext();
        JMSProducer jmsProducer = jmsContextQCFBindings.createProducer();

        String propName = "LongValue";

        long expectedPropValue1 = -1;
        jmsProducer.setProperty(propName, expectedPropValue1);
        long actualPropValue1 = jmsProducer.getLongProperty(propName);

        long expectedPropValue2 = 0;
        jmsProducer.setProperty(propName, expectedPropValue2);
        long actualPropValue2 = jmsProducer.getLongProperty(propName);

        long expectedPropValue3 = 9223372036854775807L;
        jmsProducer.setProperty(propName, expectedPropValue3);
        long actualPropValue3 = jmsProducer.getLongProperty(propName);

        boolean testFailed = false;
        if ( (actualPropValue1 != expectedPropValue1) ||
             (actualPropValue2 != expectedPropValue2) ||
             (actualPropValue3 != expectedPropValue3) ) {
            testFailed = true;
        }

        jmsContextQCFBindings.close();

        if ( testFailed ) {
            throw new Exception("testSetLongProperty_Variation_B_SecOff failed");
        }
    }

    // 118073_19_4 Test with "value" set as 0
    // 118073_19_5 Test with "value" set as -1
    // 118073_19_6 Test with "value" set to boundary values for short

    public void testSetLongProperty_Variation_TCP_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFTCP = qcfTCP.createContext();
        JMSProducer jmsProducer = jmsContextQCFTCP.createProducer();

        String propName = "LongValue";

        long expectedPropValue1 = -1;
        jmsProducer.setProperty(propName, expectedPropValue1);
        long actualPropValue1 = jmsProducer.getLongProperty(propName);

        long expectedPropValue2 = 0;
        jmsProducer.setProperty(propName, expectedPropValue2);
        long actualPropValue2 = jmsProducer.getLongProperty(propName);

        long expectedPropValue3 = 9223372036854775807L;
        jmsProducer.setProperty(propName, expectedPropValue3);
        long actualPropValue3 = jmsProducer.getLongProperty(propName);

        boolean testFailed = false;
        if ( (actualPropValue1 != expectedPropValue1) ||
             (actualPropValue2 != expectedPropValue2) ||
             (actualPropValue3 != expectedPropValue3) ) {
            testFailed = true;
        }

        jmsContextQCFTCP.close();

        if ( testFailed ) {
            throw new Exception("testSetLongProperty_Variation_TCP_SecOff failed");
        }
    }

    // 118073_24_2 MessageFormatRuntimeException - if this type conversion is invalid.

    public void testSetLongProperty_MFRE_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFBindings = qcfBindings.createContext();
        JMSProducer jmsProducer = jmsContextQCFBindings.createProducer();

        String propName = "propName";

        jmsProducer.setProperty(propName, true);

        boolean testFailed = false;
        try {
            long propValue = jmsProducer.getLongProperty(propName);
            testFailed = true;
        } catch ( MessageFormatRuntimeException ex ) {
            ex.printStackTrace();
        }

        jmsContextQCFBindings.close();

        if ( testFailed ) {
            throw new Exception("testSetLongProperty_MFRE_B_SecOff failed");
        }
    }

    public void testSetLongProperty_MFRE_TCP_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFTCP = qcfTCP.createContext();
        JMSProducer jmsProducer = jmsContextQCFTCP.createProducer();

        String propName = "propName";

        jmsProducer.setProperty(propName, true);

        boolean testFailed = false;
        try {
            long propValue = jmsProducer.getLongProperty(propName);
            testFailed = true;
        } catch ( MessageFormatRuntimeException ex ) {
            ex.printStackTrace();
        }

        jmsContextQCFTCP.close();

        if (  testFailed ) {
            throw new Exception("testSetLongProperty_MFRE_TCP_SecOff failed");
        }
    }

    // 118073_25 JMSProducer setProperty(String name, float value)
    // 118073_25_1 Specifies that messages sent using this JMSProducer will have
    // the specified property set to the specified float value.
    // 118073_25_2 Invoking this method will replace any property of the same
    // name that is already set on the message being sent.

    public void testSetFloatProperty_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFBindings = qcfBindings.createContext();
        emptyQueue(qcfBindings, queue1);
        JMSConsumer jmsConsumer = jmsContextQCFBindings.createConsumer(queue1);
        JMSProducer jmsProducer = jmsContextQCFBindings.createProducer();

        TextMessage tmsg = jmsContextQCFBindings.createTextMessage();

        String propName = "MyProp";

        float propValue1 = 123.4f;
        tmsg.setFloatProperty(propName, propValue1);

        float propValue2 = 213.4f;
        jmsProducer.setProperty(propName, propValue2);

        jmsProducer.send(queue1, tmsg);

        float actualPropValue = jmsConsumer.receive(30000).getFloatProperty(propName);

        boolean testFailed = false;
        if ( actualPropValue != propValue2 ) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContextQCFBindings.close();

        if ( testFailed ) {
            throw new Exception("testSetFloatProperty_B_SecOff failed");
        }
    }

    public void testSetFloatProperty_TCP_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        boolean testFailed = false;

        JMSContext jmsContextQCFTCP = qcfTCP.createContext();
        emptyQueue(qcfTCP, queue1);
        JMSConsumer jmsConsumer = jmsContextQCFTCP.createConsumer(queue1);
        JMSProducer jmsProducer = jmsContextQCFTCP.createProducer();

        TextMessage tmsg = jmsContextQCFTCP.createTextMessage();

        String propName = "MyProp";

        float propValue1 = 123.4f;
        tmsg.setFloatProperty(propName, propValue1);

        float propValue2 = 213.4f;
        jmsProducer.setProperty(propName, propValue2);

        jmsProducer.send(queue1, tmsg);

        float actualPropValue = jmsConsumer.receive(30000).getFloatProperty(propName);

        if ( actualPropValue != propValue2 ) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContextQCFTCP.close();

        if ( testFailed ) {
            throw new Exception("testSetFloatProperty_TCP_SecOff failed");
        }
    }

    // 118073_25_3 IllegalArgumentException - if the name is null or if the name
    // is an empty string.

    public void testSetFloatProperty_Null_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFBindings = qcfBindings.createContext();
        JMSProducer jmsProducer = jmsContextQCFBindings.createProducer();

        float propValue = 123.4f;

        boolean testFailed = false;

        try {
            jmsProducer.setProperty("", propValue);
            testFailed = true;
        } catch ( IllegalArgumentException ex ) {
            ex.printStackTrace();
        }

        try {
            jmsProducer.setProperty(null, propValue);
            testFailed = true;
        } catch ( IllegalArgumentException ex ) {
            ex.printStackTrace();
        }

        jmsContextQCFBindings.close();

        if ( testFailed ) {
            throw new Exception("testSetFloatProperty_Null_B_SecOff failed");
        }
    }

    public void testSetFloatProperty_Null_TCP_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFTCP = qcfTCP.createContext();
        JMSProducer jmsProducer = jmsContextQCFTCP.createProducer();

        float propValue = 123.4f;

        boolean testFailed = false;

        try {
            jmsProducer.setProperty("", propValue);
            testFailed = true;
        } catch ( IllegalArgumentException ex ) {
            ex.printStackTrace();
        }

        try {
            jmsProducer.setProperty(null, propValue);
            testFailed = true;
        } catch ( IllegalArgumentException ex ) {
            ex.printStackTrace();
        }

        jmsContextQCFTCP.close();

        if ( testFailed ) {
            throw new Exception("testSetFloatProperty_Null_TCP_SecOff failed");
        }
    }

    // 118073_25_4 Test with "value" set to 0
    // 118073_25_5 Test with "value" set to -1
    // 118073_25_6 Test with "value" set to boundary values for float.

    public void testSetFloatProperty_Variation_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFBindings = qcfBindings.createContext();
        JMSProducer jmsProducer = jmsContextQCFBindings.createProducer();

        String propName = "floatValue";

        float expectedPropValue1 = -1;
        jmsProducer.setProperty(propName, expectedPropValue1);
        float actualPropValue1 = jmsProducer.getFloatProperty(propName);

        float expectedPropValue2 = 0;
        jmsProducer.setProperty(propName, expectedPropValue2);
        float actualPropValue2 = jmsProducer.getFloatProperty(propName);

        float expectedPropValue3 = 214748364788888888888889999999999000000.1234567889999999999999999999F;
        jmsProducer.setProperty(propName, expectedPropValue3);
        float actualPropValue3 = jmsProducer.getFloatProperty(propName);

        boolean testFailed = false;
        if ( (actualPropValue1 != expectedPropValue1) ||
             (actualPropValue2 != expectedPropValue2) ||
             (actualPropValue3 != expectedPropValue3) ) {
            testFailed = true;
        }

        jmsContextQCFBindings.close();

        if ( testFailed ) {
            throw new Exception("testSetFloatProperty_Variation_B_SecOff failed");
        }
    }

    public void testSetFloatProperty_Variation_TCP_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFTCP = qcfTCP.createContext();
        JMSProducer jmsProducer = jmsContextQCFTCP.createProducer();

        String propName = "floatValue";

        float expectedPropValue1 = -1;
        jmsProducer.setProperty(propName, expectedPropValue1);
        float actualPropValue1 = jmsProducer.getFloatProperty(propName);

        float expectedPropValue2 = 0;
        jmsProducer.setProperty(propName, expectedPropValue2);
        float actualPropValue2 = jmsProducer.getFloatProperty(propName);

        float expectedPropValue3 = 214748364788888888888889999999999000000.1234567889999999999999999999F;
        jmsProducer.setProperty(propName, expectedPropValue3);
        float actualPropValue3 = jmsProducer.getFloatProperty(propName);

        boolean testFailed = false;
        if ( (actualPropValue1 != expectedPropValue1) ||
             (actualPropValue2 != expectedPropValue2) ||
             (actualPropValue3 != expectedPropValue3) ) {
            testFailed = true;
        }

        jmsContextQCFTCP.close();

        if ( testFailed ) {
            throw new Exception("testSetFloatProperty_Variation_TCP_SecOff failed");
        }
    }

    // 118073_26_2 MessageFormatRuntimeException - if this type conversion is
    // invalid.

    public void testSetFloatProperty_MFRE_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFBindings = qcfBindings.createContext();
        JMSProducer jmsProducer = jmsContextQCFBindings.createProducer();

        String propName = "propName";

        jmsProducer.setProperty(propName, true);

        boolean testFailed = false;
        try {
            float propValue = jmsProducer.getFloatProperty(propName);
            testFailed = true;
        } catch ( MessageFormatRuntimeException ex ) {
            ex.printStackTrace();
        }

        jmsContextQCFBindings.close();

        if ( testFailed ) {
            throw new Exception("testSetFloatProperty_MFRE_B_SecOff failed");
        }
    }

    public void testSetFloatProperty_MFRE_TCP_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFTCP = qcfTCP.createContext();
        JMSProducer jmsProducer = jmsContextQCFTCP.createProducer();

        TextMessage tmsg = jmsContextQCFTCP.createTextMessage();

        String propName = "propName";

        jmsProducer.setProperty(propName, true);

        boolean testFailed = false;
        try {
            float propValue = jmsProducer.getFloatProperty(propName);
            testFailed = true;
        } catch ( MessageFormatRuntimeException ex ) {
            ex.printStackTrace();
        }

        jmsContextQCFTCP.close();

        if ( testFailed ) {
            throw new Exception("testSetFloatProperty_MFRE_TCP_SecOff failed");
        }
    }

    // 118073_27 JMSProducer setProperty(String name, double value)
    // 118073_27_1 Specifies that messages sent using this JMSProducer will have
    // the specified property set to the specified double value.
    // 118073_27_2 Test when this method is invoked will replace any property of
    // the same name that is already set on the message being sent.

    public void testSetDoubleProperty_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFBindings = qcfBindings.createContext();
        emptyQueue(qcfBindings, queue1);
        JMSConsumer jmsConsumer = jmsContextQCFBindings.createConsumer(queue1);
        JMSProducer jmsProducer = jmsContextQCFBindings.createProducer();

        TextMessage tmsg = jmsContextQCFBindings.createTextMessage();

        String propName = "MyProp";

        double propValue1 = 1.111e2;
        tmsg.setDoubleProperty(propName, propValue1);

        double propValue2 = 1.234e2;
        jmsProducer.setProperty(propName, propValue2);

        jmsProducer.send(queue1, tmsg);

        double actualPropValue = jmsConsumer.receive(30000).getDoubleProperty(propName);

        boolean testFailed = false;
        if ( actualPropValue != propValue2 ) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContextQCFBindings.close();

        if ( testFailed ) {
            throw new Exception("testSetDoubleProperty_B_SecOff failed");
        }
    }

    public void testSetDoubleProperty_TCP_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFTCP = qcfTCP.createContext();
        emptyQueue(qcfTCP, queue1);
        JMSConsumer jmsConsumer = jmsContextQCFTCP.createConsumer(queue1);
        JMSProducer jmsProducer = jmsContextQCFTCP.createProducer();
        TextMessage tmsg = jmsContextQCFTCP.createTextMessage();

        String propName = "MyProp";

        double propValue1 = 1.111e2;
        tmsg.setDoubleProperty(propName, propValue1);

        double propValue2 = 1.234e2;
        jmsProducer.setProperty(propName, propValue2);

        jmsProducer.send(queue1, tmsg);

        double actualPropValue = jmsConsumer.receive(30000).getDoubleProperty(propName);

        boolean testFailed = false;
        if ( actualPropValue != propValue2 ) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContextQCFTCP.close();

        if ( testFailed ) {
            throw new Exception("testSetDoubleProperty_TCP_SecOff failed");
        }
    }

    // 118073_27_3 IllegalArgumentException - if the name is null or if the name
    // is an empty string.

    public void testSetDoubleProperty_Null_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFBindings = qcfBindings.createContext();
        JMSProducer jmsProducer = jmsContextQCFBindings.createProducer();

        boolean testFailed = false;

        double propValue = 1.234e2;

        try {
            jmsProducer.setProperty("", propValue);
            testFailed = true;
        } catch ( IllegalArgumentException ex ) {
            ex.printStackTrace();
        }

        try {
            jmsProducer.setProperty(null, propValue);
            testFailed = true;
        } catch ( IllegalArgumentException ex ) {
            ex.printStackTrace();
        }

        jmsContextQCFBindings.close();

        if ( testFailed ) {
            throw new Exception("testSetDoubleProperty_Null_B_SecOff failed");
        }
    }

    public void testSetDoubleProperty_Null_TCP_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFTCP = qcfTCP.createContext();
        JMSProducer jmsProducer = jmsContextQCFTCP.createProducer();

        boolean testFailed = false;

        double propValue = 1.234e2;

        try {
            jmsProducer.setProperty("", propValue);
            testFailed = true;
        } catch ( IllegalArgumentException ex ) {
            ex.printStackTrace();
        }

        try {
            jmsProducer.setProperty(null, propValue);
            testFailed = true;
        } catch ( IllegalArgumentException ex ) {
            ex.printStackTrace();
        }

        jmsContextQCFTCP.close();

        if ( testFailed ) {
            throw new Exception("testSetDoubleProperty_Null_TCP_SecOff failed");
        }
    }

    // 118073_27_4 Test with value set to 0
    // 118073_27_5 Test with value set to -1
    // 118073_27_6 Test with value set to boundary values allowed for double

    public void testSetDoubleProperty_Variation_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFBindings = qcfBindings.createContext();
        JMSConsumer jmsConsumer = jmsContextQCFBindings.createConsumer(queue1);
        JMSProducer jmsProducer = jmsContextQCFBindings.createProducer();

        String propName = "doubleValue";

        double expectedPropValue1 = -1;
        jmsProducer.setProperty(propName, expectedPropValue1);
        double actualPropValue1 = jmsProducer.getDoubleProperty(propName);

        double expectedPropValue2 = 0;
        jmsProducer.setProperty(propName, expectedPropValue2);
        double actualPropValue2 = jmsProducer.getDoubleProperty(propName);

        double expectedPropValue3 = 2147483647888888888888899999999990000008888888888888888.1234567889999999e100;
        jmsProducer.setProperty(propName, expectedPropValue3);
        double actualPropValue3 = jmsProducer.getDoubleProperty(propName);

        boolean testFailed = false;
        if ( (actualPropValue1 != expectedPropValue1) ||
             (actualPropValue2 != expectedPropValue2) ||
             (actualPropValue3 != expectedPropValue3) ) {
            testFailed = true;
        }

        jmsContextQCFBindings.close();

        if ( testFailed ) {
            throw new Exception("testSetDoubleProperty_Variation_B_SecOff failed");
        }
    }

    public void testSetDoubleProperty_Variation_TCP_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFTCP = qcfTCP.createContext();
        JMSProducer jmsProducer = jmsContextQCFTCP.createProducer();

        String propName = "doubleValue";

        double expectedPropValue1 = -1;
        jmsProducer.setProperty(propName, expectedPropValue1);
        double actualPropValue1 = jmsProducer.getDoubleProperty(propName);

        double expectedPropValue2 = 0;
        jmsProducer.setProperty(propName, expectedPropValue2);
        double actualPropValue2 = jmsProducer.getDoubleProperty(propName);

        double expectedPropValue3 = 2147483647888888888888899999999990000008888888888888888.1234567889999999e100;
        jmsProducer.setProperty(propName, expectedPropValue3);
        double actualPropValue3 = jmsProducer.getDoubleProperty(propName);

        boolean testFailed = false;
        if ( (actualPropValue1 != expectedPropValue1) ||
             (actualPropValue2 != expectedPropValue2) ||
             (actualPropValue3 != expectedPropValue3) ) {
            testFailed = true;
        }

        jmsContextQCFTCP.close();

        if ( testFailed ) {
            throw new Exception("testSetDoubleProperty_Variation_TCP_SecOff failed");
        }
    }

    // 118073_28_2 MessageFormatRuntimeException - if this type conversion is
    // invalid.

    public void testSetDoubleProperty_MFRE_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFBindings = qcfBindings.createContext();
        JMSProducer jmsProducer = jmsContextQCFBindings.createProducer();

        String propName = "propName";

        jmsProducer.setProperty(propName, true);

        boolean testFailed = false;
        try {
            double propValue = jmsProducer.getDoubleProperty(propName);
            testFailed = true;
        } catch ( MessageFormatRuntimeException ex ) {
            ex.printStackTrace();
            testFailed = false;
        }

        jmsContextQCFBindings.close();

        if ( testFailed ) {
            throw new Exception("testSetDoubleProperty_MFRE_B_SecOff failed");
        }
    }

    public void testSetDoubleProperty_MFRE_TCP_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFTCP = qcfTCP.createContext();
        JMSProducer jmsProducer = jmsContextQCFTCP.createProducer();

        String propName = "propName";

        jmsProducer.setProperty(propName, true);

        boolean testFailed;
        try {
            double propValue = jmsProducer.getDoubleProperty(propName);
            testFailed = true;
        } catch ( MessageFormatRuntimeException ex ) {
            ex.printStackTrace();
            testFailed = false;
        }

        jmsContextQCFTCP.close();

        if ( testFailed ) {
            throw new Exception("testSetDoubleProperty_MFRE_TCP_SecOff failed");
        }
    }

    // 118073_29 JMSProducer setProperty(String name, String value)
    // 118073_29_1 Specifies that messages sent using this JMSProducer will have
    // the specified property set to the specified String value.
    // 118073_29_2 Invoking this method will replace any property of the same
    // name that is already set on the message being sent.

    public void testSetStringProperty_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFBindings = qcfBindings.createContext();
        emptyQueue(qcfBindings, queue1);
        JMSConsumer jmsConsumer = jmsContextQCFBindings.createConsumer(queue1);
        JMSProducer jmsProducer = jmsContextQCFBindings.createProducer();

        String propName = "MyProp";

        TextMessage tmsg = jmsContextQCFBindings.createTextMessage();

        String propValue1 = "EmployeeID";
        tmsg.setStringProperty(propName, propValue1);

        String propValue2 = "EmployeeName";
        jmsProducer.setProperty(propName, propValue2);

        jmsProducer.send(queue1, tmsg);

        String actualPropValue = jmsConsumer.receive(30000).getStringProperty(propName);

        boolean testFailed = false;
        if ( !actualPropValue.equals(propValue2) ) {
            testFailed = true;
        }

        jmsContextQCFBindings.close();

        if ( testFailed ) {
            throw new Exception("testSetStringProperty_B_SecOff failed");
        }
    }

    public void testSetStringProperty_TCP_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFTCP = qcfTCP.createContext();
        emptyQueue(qcfTCP, queue1);
        JMSConsumer jmsConsumer = jmsContextQCFTCP.createConsumer(queue1);
        JMSProducer jmsProducer = jmsContextQCFTCP.createProducer();

        String propName = "MyProp";
        String propValue1 = "EmployeeID";

        TextMessage tmsg = jmsContextQCFTCP.createTextMessage();

        tmsg.setStringProperty(propName, propValue1);

        String propValue2 = "EmployeeName";

        jmsProducer.setProperty(propName, propValue2);
        jmsProducer.send(queue1, tmsg);

        String actualPropValue = jmsConsumer.receive(30000).getStringProperty(propName);

        boolean testFailed = false;
        if ( !actualPropValue.equals(propValue2) ) {
            testFailed = true;
        }

        jmsContextQCFTCP.close();

        if ( testFailed ) {
            throw new Exception("testSetStringProperty_TCP_SecOff failed");
        }
    }

    // 118073_29_3 IllegalArgumentException - if the name is null or if the name
    // is an empty string.

    public void testSetStringProperty_Null_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFBindings = qcfBindings.createContext();
        JMSProducer jmsProducer = jmsContextQCFBindings.createProducer();

        String propValue = "EmployeeName";

        boolean testFailed = false;

        try {
            jmsProducer.setProperty("", propValue);
            testFailed = true;
        } catch ( IllegalArgumentException ex ) {
            ex.printStackTrace();
        }

        try {
            jmsProducer.setProperty(null, propValue);
            testFailed = true;
        } catch ( IllegalArgumentException ex ) {
            ex.printStackTrace();
        }

        jmsContextQCFBindings.close();
        if ( testFailed ) {
            throw new Exception("testSetStringProperty_Null_B_SecOff failed");
        }
    }

    public void testSetStringProperty_Null_TCP_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFTCP = qcfTCP.createContext();
        JMSProducer jmsProducer = jmsContextQCFTCP.createProducer();

        boolean testFailed = false;

        String propValue = "EmployeeName";

        try {
            jmsProducer.setProperty("", propValue);
            testFailed = true;
        } catch ( IllegalArgumentException ex ) {
            ex.printStackTrace();
        }

        try {
            jmsProducer.setProperty(null, propValue);
            testFailed = true;
        } catch ( IllegalArgumentException ex ) {
            ex.printStackTrace();
        }

        jmsContextQCFTCP.close();

        if ( testFailed ) {
            throw new Exception("testSetStringProperty_Null_TCP_SecOff failed");
        }
    }

    // No MessageFormatException should be thrown when property set as Boolean
    // is read as a String.

    public void testGetStringProperty_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFBindings = qcfBindings.createContext();
        JMSProducer jmsProducer = jmsContextQCFBindings.createProducer();

        jmsProducer.setProperty("propName", true);
        String propValue = jmsProducer.getStringProperty("propName");

        boolean testFailed = false;
        if ( !propValue.equals("true") ) {
            testFailed = true;
        }

        jmsContextQCFBindings.close();

        if ( testFailed ) {
            throw new Exception("testGetStringProperty_B_SecOff failed");
        }
    }

    public void testGetStringProperty_TCP_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFTCP = qcfTCP.createContext();
        JMSProducer jmsProducer = jmsContextQCFTCP.createProducer();

        jmsProducer.setProperty("propName", true);
        String propValue = jmsProducer.getStringProperty("propName");

        boolean testFailed = false;
        if ( !propValue.equals("true") ) {
            testFailed = true;
        }

        jmsContextQCFTCP.close();

        if ( testFailed ) {
            throw new Exception("testGetStringProperty_TCP_SecOff failed");
        }
    }

    // 118073_31 JMSProducer setProperty(String name,Object value)
    // 118073_32_1 Specifies that messages sent using this JMSProducer will have
    // the specified property set to the specified Java object value.
    // 118073_32_2 Verify that this method works only for the objectified
    // primitive object types (Integer, Double, Long ...) and String objects.
    // 118073_32_3 Test this will replace any property of the same name that is
    // already set on the message being sent.

    public void testSetObjectProperty_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFBindings = qcfBindings.createContext();
        emptyQueue(qcfBindings, queue1);
        JMSConsumer jmsConsumer = jmsContextQCFBindings.createConsumer(queue1);

        JMSProducer jmsProducer = jmsContextQCFBindings.createProducer();

        String propName = "MyProp";

        TextMessage tmsg = jmsContextQCFBindings.createTextMessage();
        tmsg.setObjectProperty( propName, Integer.valueOf(1444) );

        jmsProducer.setProperty( propName, Integer.valueOf(2000) );

        jmsProducer.send(queue1, tmsg);

        Object propValue = jmsConsumer.receive(30000).getObjectProperty(propName);

        boolean testFailed = false;
        if ( !propValue.equals(2000) ) {
            testFailed = true;
        }
        jmsConsumer.close();
        jmsContextQCFBindings.close();

        if ( testFailed ) {
            throw new Exception("testSetObjectProperty_B_SecOff failed");
        }
    }

    public void testSetObjectProperty_TCP_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFTCP = qcfTCP.createContext();
        emptyQueue(qcfTCP, queue1);
        JMSConsumer jmsConsumer = jmsContextQCFTCP.createConsumer(queue1);
        JMSProducer jmsProducer = jmsContextQCFTCP.createProducer();

        String propName = "MyProp";

        TextMessage tmsg = jmsContextQCFTCP.createTextMessage();
        tmsg.setObjectProperty(propName, new Integer(1444));

        jmsProducer.setProperty(propName, new Integer(2000));

        jmsProducer.send(queue1, tmsg);

        Object propValue = jmsConsumer.receive(30000).getObjectProperty(propName);

        boolean testFailed = false;
        if ( !propValue.equals(2000) ) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContextQCFTCP.close();

        if ( testFailed ) {
            throw new Exception("testSetObjectProperty_TCP_SecOff failed");
        }
    }

    // 118073_32_4 IllegalArgumentException - if the name is null or if the name
    // is an empty string.

    public void testSetObjectProperty_Null_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFBindings = qcfBindings.createContext();
        JMSProducer jmsProducer = jmsContextQCFBindings.createProducer();

        boolean testFailed = false;

        try {
            jmsProducer.setProperty("", new Integer(1000));
            testFailed = true;
        } catch ( IllegalArgumentException ex ) {
            ex.printStackTrace();
        }

        try {
            jmsProducer.setProperty(null, new Integer(1000));
            testFailed = true;
        } catch ( IllegalArgumentException ex ) {
            ex.printStackTrace();
        }

        jmsContextQCFBindings.close();

        if ( testFailed ) {
            throw new Exception("testSetObjectProperty_Null_B_SecOff failed");
        }
    }

    public void testSetObjectProperty_Null_TCP_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFTCP = qcfTCP.createContext();
        JMSProducer jmsProducer = jmsContextQCFTCP.createProducer();

        boolean testFailed = false;

        try {
            jmsProducer.setProperty( "", Integer.valueOf(1000) );
            testFailed = true;
        } catch ( IllegalArgumentException ex ) {
            ex.printStackTrace();
        }

        try {
            jmsProducer.setProperty( null, Integer.valueOf(1000) );
            testFailed = true;
        } catch ( IllegalArgumentException ex ) {
            ex.printStackTrace();
        }

        jmsContextQCFTCP.close();

        if ( testFailed ) {
            throw new Exception("testSetObjectProperty_Null_TCP_SecOff failed");
        }
    }

    // 118073_32_5 

    public void testSetObjectProperty_NullObject_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFBindings = qcfBindings.createContext();
        JMSProducer jmsProducer = jmsContextQCFBindings.createProducer();

        boolean testFailed;
        try {
            jmsProducer.setProperty("Tester", null);
            testFailed = false;
        } catch ( NullPointerException ex ) {
            ex.printStackTrace();
            testFailed = true;
        }

        jmsContextQCFBindings.close();

        if ( testFailed ) {
            throw new Exception("testSetObjectProperty_NullObject_B_SecOff failed");
        }
    }

    public void testSetObjectProperty_NullObject_TCP_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFTCP = qcfTCP.createContext();
        JMSProducer jmsProducer = jmsContextQCFTCP.createProducer();

        boolean testFailed;
        try {
            jmsProducer.setProperty("Tester", null);
            testFailed = false;
        } catch ( NullPointerException ex ) {
            ex.printStackTrace();
            testFailed = true;
        }

        jmsContextQCFTCP.close();

        if (testFailed)
            throw new Exception("testSetObjectProperty_NullObject_TCP_SecOff failed");
    }

    // 118073_33_2 if there is no property by this name, a null value is returned

    public void testSetObjectProperty_NullValue_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFBindings = qcfBindings.createContext();
        JMSProducer jmsProducer = jmsContextQCFBindings.createProducer();

        Object nameProperty = jmsProducer.getObjectProperty("Name");

        boolean testFailed = false;
        if ( nameProperty != null ) {
            testFailed = true;
        }

        jmsContextQCFBindings.close();

        if ( testFailed ) {
            throw new Exception("testSetObjectProperty_NullValue_B_SecOff failed");
        }
    }

    public void testSetObjectProperty_NullValue_TCP_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFTCP = qcfTCP.createContext();
        JMSProducer jmsProducer = jmsContextQCFTCP.createProducer();

        Object nameProperty = jmsProducer.getObjectProperty("Name");

        boolean testFailed = false;
        if ( nameProperty != null ) {
            testFailed = true;
        }

        jmsContextQCFTCP.close();

        if ( testFailed ) {
            throw new Exception("testSetObjectProperty_NullValue_TCP_SecOff failed");
        }
    }

    // 118073_31 Set<String> getPropertyNames()
    // 118073_31_1 Returns an unmodifiable Set view of the names of all the
    // message properties that have been set on this JMSProducer.
    // 118073_31_2 JMS standard header fields are not considered properties and
    // are not returned in this Set.

    public void testGetPropertyNames_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFBindings = qcfBindings.createContext();
        JMSProducer jmsProducer = jmsContextQCFBindings.createProducer();

        jmsProducer.setProperty("Role", "Tester");
        jmsProducer.setProperty("Bill", 1000);

        Set<String> propertyNames = jmsProducer.getPropertyNames();

        boolean testFailed = false;
        if ( !propertyNames.contains("Role") || !propertyNames.contains("Bill") ) {
            testFailed = true;
        }

        jmsContextQCFBindings.close();

        if ( testFailed ) {
            throw new Exception("testGetPropertyNames_B_SecOff failed");
        }
    }

    public void testGetPropertyNames_TCP_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFTCP = qcfTCP.createContext();
        JMSProducer jmsProducer = jmsContextQCFTCP.createProducer();

        jmsProducer.setProperty("Role", "Tester");
        jmsProducer.setProperty("Bill", 1000);

        jmsProducer.setJMSCorrelationID("correlID");

        Set<String> propertyNames = jmsProducer.getPropertyNames();

        boolean testFailed = false;
        if ( !propertyNames.contains("Role") || !propertyNames.contains("Bill") ) {
            testFailed = true;
        }

        jmsContextQCFTCP.close();

        if ( testFailed ) {
            throw new Exception("testGetPropertyNames_TCP_SecOff failed");
        }
    }

    // 118073_31_3 java.lang.UnsupportedOperationException results when attempts
    // are made to modify the returned collection

    public void testGetPropertyNames_Exception_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFBindings = qcfBindings.createContext();
        JMSProducer jmsProducer = jmsContextQCFBindings.createProducer();

        jmsProducer.setProperty("Role", "Tester");
        jmsProducer.setProperty("Bill", 1000);

        Set<String> propertyNames = jmsProducer.getPropertyNames();

        boolean testFailed = false;
        try {
            propertyNames.remove("Bill");
            testFailed = true;
        } catch ( UnsupportedOperationException ex ) {
            ex.printStackTrace();
        }

        jmsContextQCFBindings.close();

        if ( testFailed ) {
            throw new Exception("testGetPropertyNames_Exception_B_SecOff failed");
        }
    }

    public void testGetPropertyNames_Exception_TCP_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFTCP = qcfTCP.createContext();
        JMSProducer jmsProducer = jmsContextQCFTCP.createProducer();

        jmsProducer.setProperty("Role", "Tester");
        jmsProducer.setProperty("Bill", 1000);

        Set<String> propertyNames = jmsProducer.getPropertyNames();

        boolean testFailed = false;
        try {
            propertyNames.remove("Bill");
            testFailed = true;
        } catch ( UnsupportedOperationException ex ) {
            ex.printStackTrace();
        }

        jmsContextQCFTCP.close();

        if ( testFailed ) {
            throw new Exception("testGetPropertyNames_Exception_TCP_SecOff failed");
        }
    }

    // 118073_32 JMSProducer setJMSCorrelationIDAsBytes(byte[] correlationID)
    // 118073_32_1 Specifies that messages sent using this JMSProducer will have
    // their JMSCorrelationID header value set to the specified correlation ID,
    // where correlation ID is specified as an array of bytes.
    // 118073_33_1 Returns the JMSCorrelationID header value that has been set
    // on this JMSProducer, as an array of bytes.

    public void testSetJMSCorrelationIDAsBytes_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFBindings = qcfBindings.createContext();
        JMSProducer jmsProducer = jmsContextQCFBindings.createProducer();

        byte[] expectedBytes = { 1, 2, 3, 4 };
        jmsProducer.setJMSCorrelationIDAsBytes(expectedBytes);
        byte[] actualBytes = jmsProducer.getJMSCorrelationIDAsBytes();

        boolean testFailed = false;

        if ( actualBytes == null ) {
            testFailed = true;
        } else if ( expectedBytes.length != actualBytes.length ) {
            testFailed = true;
        } else {
            for ( int byteNo = 0; !testFailed && (byteNo < expectedBytes.length); byteNo++ ) {
                if ( expectedBytes[byteNo] != actualBytes[byteNo] ) {
                    testFailed = true;
                }
            }
        }

        jmsContextQCFBindings.close();

        if ( testFailed ) {
            throw new Exception("testSetJMSCorrelationIDAsBytes_B_SecOff failed");
        }
    }

    public void testSetJMSCorrelationIDAsBytes_TCP_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFTCP = qcfTCP.createContext();
        JMSProducer jmsProducer = jmsContextQCFTCP.createProducer();

        byte[] expectedBytes = { 1, 2, 3, 4 };
        jmsProducer.setJMSCorrelationIDAsBytes(expectedBytes);
        byte[] actualBytes = jmsProducer.getJMSCorrelationIDAsBytes();

        boolean testFailed = false;

        if ( actualBytes == null ) {
            testFailed = true;
        } else if ( expectedBytes.length != actualBytes.length ) {
            testFailed = true;
        } else {
            for ( int byteNo = 0; !testFailed && (byteNo < expectedBytes.length); byteNo++ ) {
                if ( expectedBytes[byteNo] != actualBytes[byteNo] ) {
                    testFailed = true;
                }
            }
        }

        jmsContextQCFTCP.close();

        if ( testFailed ) {
            throw new Exception("testSetJMSCorrelationIDAsBytes_B_SecOff failed");
        }
    }

    // 118073_34 JMSProducer setJMSCorrelationID(String correlationID)
    // 118073_34_1 Specifies that messages sent using this JMSProducer will have
    // their JMSCorrelationID header value set to the specified correlation ID,
    // where correlation ID is specified as a String.

    // 118073_35 String getJMSCorrelationID()
    // 118073_35_1 Returns the JMSCorrelationID header value that has been set
    // on this JMSProducer, as a String.

    public void testSetJMSCorrelationID_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFBindings = qcfBindings.createContext();
        JMSProducer jmsProducer = jmsContextQCFBindings.createProducer();

        String expectedId = "MyCorrelID";
        jmsProducer.setJMSCorrelationID(expectedId);
        String actualId = jmsProducer.getJMSCorrelationID();

        boolean testFailed = false;
        if ( !expectedId.equals(actualId) ) {
            testFailed = true;
        }

        jmsContextQCFBindings.close();

        if ( testFailed ) {
            throw new Exception("testSetJMSCorrelationID_B_SecOff failed");
        }
    }

    public void testSetJMSCorrelationID_TCP_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        String expectedId = "MyCorrelID";
        boolean testFailed = false;

        JMSContext jmsContextQCFTCP = qcfTCP.createContext();

        JMSProducer jmsProducer = jmsContextQCFTCP.createProducer();

        jmsProducer.setJMSCorrelationID(expectedId);

        String actualId = jmsProducer.getJMSCorrelationID();

        if (!(expectedId.equals(actualId)))
            testFailed = true;
        jmsContextQCFTCP.close();

        if (testFailed)
            throw new Exception("testSetJMSCorrelationID_TCP_SecOff failed");

    }

    // 118073_34_2 Test what JMSCorrelationID can hold as its value

    public void testSetJMSCorrelationID_Value_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFBindings = qcfBindings.createContext();
        JMSProducer jmsProducer = jmsContextQCFBindings.createProducer();

        Message msg = jmsContextQCFBindings.createMessage();
        msg.setJMSCorrelationID("ID:aaaa");

        String expectedId = "ID:ffff";
        jmsProducer.setJMSCorrelationID(expectedId);
        String actualId = jmsProducer.getJMSCorrelationID();

        boolean testFailed = false;
        if ( !expectedId.equals(actualId) ) {
            testFailed = true;
        }

        jmsContextQCFBindings.close();

        if ( testFailed ) {
            throw new Exception("testSetJMSCorrelationID_Value_B_SecOff failed");
        }
    }

    public void testSetJMSCorrelationID_Value_TCP_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFTCP = qcfTCP.createContext();
        JMSProducer jmsProducer = jmsContextQCFTCP.createProducer();

        Message msg = jmsContextQCFTCP.createMessage();
        msg.setJMSCorrelationID("ID:aaaa");

        String expectedId = "ID:ffff";
        jmsProducer.setJMSCorrelationID(expectedId);
        String actualId = jmsProducer.getJMSCorrelationID();

        boolean testFailed = false;
        if ( !expectedId.equals(actualId) ) {
            testFailed = true;
        }

        jmsContextQCFTCP.close();

        if ( testFailed ) {
            throw new Exception("testSetJMSCorrelationID_Value_TCP_SecOff failed");
        }
    }

    // 118073_36 JMSProducer setJMSType(String type)
    // 118073_36_1 Returns the JMSType header value that has been set on this JMSProducer.
    // 118073_37 String getJMSType()
    // 118073_37_1 Returns the JMSType header value that has been set on this JMSProducer.

    public void testSetJMSType_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFBindings = qcfBindings.createContext();
        JMSProducer jmsProducer = jmsContextQCFBindings.createProducer();

        String type1 = "type 1";
        jmsProducer.setJMSType(type1);
        String t1 = jmsProducer.getJMSType();

        String type2 = "type 2";
        jmsProducer.setJMSType(type2);
        String t2 = jmsProducer.getJMSType();

        boolean testFailed = false;
        if ( !(t1.equals(type1) && t2.equals(type2)) ) {
            testFailed = true;
        }

        jmsContextQCFBindings.close();

        if ( testFailed ) {
            throw new Exception("testSetJMSType_B_SecOff failed");
        }
    }

    public void testSetJMSType_TCP_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFTCP = qcfTCP.createContext();
        JMSProducer jmsProducer = jmsContextQCFTCP.createProducer();

        String type1 = "type 1";
        jmsProducer.setJMSType(type1);
        String t1 = jmsProducer.getJMSType();

        String type2 = "type 2";
        jmsProducer.setJMSType(type2);
        String t2 = jmsProducer.getJMSType();

        boolean testFailed = false;
        if ( !(t1.equals(type1) && t2.equals(type2)) ) {
            testFailed = true;
        }

        jmsContextQCFTCP.close();

        if ( testFailed ) {
            throw new Exception("testSetJMSType_TCP_SecOff failed");
        }
    }

    // Defect 175517

    public void testQueueSender_InvalidDestinationNE_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        QueueConnectionFactory cf1 = (QueueConnectionFactory) new InitialContext().lookup("java:comp/env/jndi_JMS_BASE_CF");
        QueueConnection con = cf1.createQueueConnection();
        con.start();
        QueueSession qsession = con.createQueueSession(false, javax.jms.Session.AUTO_ACKNOWLEDGE);
        QueueSender qsender = qsession.createSender(null);

        TextMessage tmsg = qsession.createTextMessage("Hello");

        boolean testFailed = false;

        try {
            qsender.send(tmsg);
            testFailed = true;
        } catch ( UnsupportedOperationException ex ) {
            ex.printStackTrace();
        } catch ( InvalidDestinationException ex ) {
            ex.printStackTrace();
            testFailed = true;
        }

        try {
            qsender.send(null, tmsg);
            testFailed = true;
        } catch ( InvalidDestinationException ex ) {
            ex.printStackTrace();
        }

        try {
            qsender.send(tmsg, tmsg.DEFAULT_DELIVERY_MODE, 0, 50000);
            testFailed = true;
        } catch ( UnsupportedOperationException ex ) {
            ex.printStackTrace();
        } catch ( InvalidDestinationException ex ) {
            ex.printStackTrace();
            testFailed = true;
        }

        try {
            qsender.send(null, tmsg, tmsg.DEFAULT_DELIVERY_MODE, 1, 50000);
            testFailed = true;
        } catch ( InvalidDestinationException ex ) {
            ex.printStackTrace();
        }

        con.close();

        if ( testFailed ) {
            throw new Exception("testJMSProducerSendTextMessage_InvalidDestinationNE_B_SecOff failed");
        }
    }

    // MessageProducer.send combinations test - createProducer(null) 

    public void testMessageProducerWithNullDestination(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        ConnectionFactory cf1 = (ConnectionFactory) new InitialContext().lookup("java:comp/env/jndi_JMS_BASE_CF");
        Connection con = cf1.createConnection();
        con.start();
        Session session = con.createSession(false, javax.jms.Session.AUTO_ACKNOWLEDGE);
        MessageProducer producer = session.createProducer(null);
        TextMessage tmsg = session.createTextMessage("Hello");

        boolean testFailed = false;

        try {
            producer.send(null, tmsg);
            testFailed = true;
        } catch ( InvalidDestinationException ex ) {
            ex.printStackTrace();
        } catch ( JMSException ex ) {
            ex.printStackTrace();
            testFailed = true;
        }

        try {
            producer.send(session.createQueue("INVALID_QUEUE"), tmsg);
            testFailed = true;
        } catch ( InvalidDestinationException ex ) {
            ex.printStackTrace();
        } catch ( JMSException ex ) {
            ex.printStackTrace();
            testFailed = true;
        }

        try {
            producer.send(queue1, tmsg);
        } catch ( InvalidDestinationException ex ) {
            ex.printStackTrace();
            testFailed = true;
        } catch ( JMSException ex ) {
            ex.printStackTrace();
            testFailed = true;
        }

        try {
            producer.send(null, tmsg, tmsg.DEFAULT_DELIVERY_MODE, 0, 50000);
            testFailed = true;
        } catch ( InvalidDestinationException ex ) {
            ex.printStackTrace();
        } catch ( JMSException ex ) {
            ex.printStackTrace();
            testFailed = true;
        }

        try {
            producer.send(session.createQueue("INVALID_QUEUE"), tmsg, tmsg.DEFAULT_DELIVERY_MODE, 0, 50000);
            testFailed = true;
        } catch ( InvalidDestinationException ex ) {
            ex.printStackTrace();
        } catch ( JMSException ex ) {
            ex.printStackTrace();
            testFailed = true;
        }

        try {
            producer.send(queue1, tmsg, tmsg.DEFAULT_DELIVERY_MODE, 0, 50000);
        } catch ( InvalidDestinationException ex ) {
            ex.printStackTrace();
            testFailed = true;
        } catch ( JMSException ex ) {
            ex.printStackTrace();
            testFailed = true;
        }


        try {
            producer.send(tmsg);
            testFailed = true;
        } catch ( UnsupportedOperationException ex ) {
            ex.printStackTrace();
        } catch ( JMSException ex ) {
            ex.printStackTrace();
            testFailed = true;
        }

        try {
            producer.send(tmsg, tmsg.DEFAULT_DELIVERY_MODE, 0, 50000);
            testFailed = true;
        } catch ( UnsupportedOperationException ex ) {
            ex.printStackTrace();
        } catch ( JMSException ex ) {
            ex.printStackTrace();
            testFailed = true;
        }

        con.close();

        if ( testFailed ) {
            throw new Exception("testMessageProducerWithNullDestination failed");
        }
    }

    // MessageProducer.send combinations test - createProducer(valid_queue) 

    public void testMessageProducerWithValidDestination(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        String methodName = "testMessageProducerWithValidDestination";

        ConnectionFactory cf1 = (ConnectionFactory) new InitialContext().lookup("java:comp/env/jndi_JMS_BASE_CF");
        Connection con = cf1.createConnection();
        con.start();
        Session session = con.createSession(false, javax.jms.Session.AUTO_ACKNOWLEDGE);
        MessageProducer producer = session.createProducer(queue2);
        TextMessage tmsg = session.createTextMessage("Hello");

        boolean testFailed = false;
        try {
            producer.send(null, tmsg);
            System.out.println(methodName + ": Unexpected send to null destination.");
            testFailed = true;
        } catch ( InvalidDestinationException ex ) {
            ex.printStackTrace();
        } catch ( JMSException ex ) {
            ex.printStackTrace();
            System.out.println(methodName + ": Unexpected exception on send to null destination.");
            testFailed = true;
        }

        try {
            producer.send(session.createQueue("INVALID_QUEUE"), tmsg);
            System.out.println(methodName + ": Unexpected send to invalid queue.");
            testFailed = true;
        } catch ( UnsupportedOperationException ex ) {
            ex.printStackTrace();
        } catch ( JMSException ex ) {
            System.out.println(methodName + ": Unexpected exception on send to invalid queue.");
            ex.printStackTrace();
            testFailed = true;
        }

        try {
            producer.send(queue1, tmsg);
            System.out.println(methodName + ": Unexpected send to disallowed desination");
            testFailed = true;
        } catch ( UnsupportedOperationException ex ) {
            ex.printStackTrace();
        } catch ( JMSException ex ) {
            System.out.println(methodName + ": Unexpected exception on send to disallowed desination");
            ex.printStackTrace();
            testFailed = true;
        }

        try {
            producer.send(null, tmsg, tmsg.DEFAULT_DELIVERY_MODE, 0, 50000);
            System.out.println(methodName + ": Unexpected send to null destination (2).");
            testFailed = true;
        } catch ( InvalidDestinationException ex ) {
            ex.printStackTrace();
        } catch ( JMSException ex ) {
            System.out.println(methodName + ": Unexpected exception on send to null destination (2).");
            ex.printStackTrace();
            testFailed = true;
        }

        try {
            producer.send(session.createQueue("INVALID_QUEUE"), tmsg, tmsg.DEFAULT_DELIVERY_MODE, 0, 50000);
            System.out.println(methodName + ": Unexpected send to invalid queue (2).");
            testFailed = true;
        } catch ( UnsupportedOperationException ex ) {
            ex.printStackTrace();
        } catch ( JMSException ex ) {
            System.out.println(methodName + ": Unexpected exception on send to invalid queue (2).");
            ex.printStackTrace();
            testFailed = true;
        }

        try {
            producer.send(queue1, tmsg, tmsg.DEFAULT_DELIVERY_MODE, 0, 50000);
            System.out.println(methodName + ": Unexpected send to valid queue (2).");            
            testFailed = true;
        } catch ( UnsupportedOperationException ex ) {
            ex.printStackTrace();
        } catch ( JMSException ex ) {
            System.out.println(methodName + ": Unexpected exception on send to valid queue (2).");
            ex.printStackTrace();
            testFailed = true;
        }

        try {
            producer.send(tmsg);
        } catch ( JMSException ex ) {
            System.out.println(methodName + ": Unexpected exception on valid send (2).");
            ex.printStackTrace();
            testFailed = true;
        }

        try {
            producer.send(tmsg, tmsg.DEFAULT_DELIVERY_MODE, 0, 50000);
        } catch ( JMSException ex ) {
            System.out.println(methodName + ": Unexpected exception on valid send (3).");
            ex.printStackTrace();
            testFailed = true;
        }

        con.close();

        if ( testFailed ) {
            throw new Exception("testMessageProducerWithValidDestination failed");
        }
    }

    public void testSendWithNullBody(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextQCFBindings = qcfBindings.createContext();
        JMSConsumer jmsConsumer = jmsContextQCFBindings.createConsumer(queue1);
        JMSProducer jmsProducer = jmsContextQCFBindings.createProducer();

        byte[] byteBody = null;
        try {
            jmsProducer.send(queue1, byteBody);
            System.out.println("Sent empty byte body");
        } catch ( MessageFormatRuntimeException e ) {
            e.printStackTrace();
        }

        Map<String, Object> Mapbody = null;
        try {
            jmsProducer.send(queue1, Mapbody);
            System.out.println("Sent empty Map body");
        } catch ( MessageFormatRuntimeException e ) {
            e.printStackTrace();
        }

        Message message = null;
        try {
            jmsProducer.send(queue1, message);
            System.out.println("Sent empty message body");
        } catch ( MessageFormatRuntimeException e ) {
            e.printStackTrace();
        }

        String body = null;
        try {
            jmsProducer.send(queue1, body);
            System.out.println("Sent empty string body");
        } catch ( MessageFormatRuntimeException e ) {
            e.printStackTrace();
        }
    }
}
