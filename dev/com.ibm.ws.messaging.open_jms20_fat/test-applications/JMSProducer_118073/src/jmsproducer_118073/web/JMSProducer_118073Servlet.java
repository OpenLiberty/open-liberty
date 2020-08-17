/*******************************************************************************
 * Copyright (c) 2013,2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package jmsproducer_118073.web;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;

import javax.jms.CompletionListener;
import javax.jms.Destination;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.JMSProducer;
import javax.jms.JMSRuntimeException;
import javax.jms.Queue;
import javax.jms.QueueConnectionFactory;
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
public class JMSProducer_118073Servlet extends HttpServlet {
    public static boolean exceptionFlag;

    //

    public static QueueConnectionFactory qcfBindings;
    public static QueueConnectionFactory tcfBindings;

    public static TopicConnectionFactory qcfTCP;
    public static TopicConnectionFactory tcfTCP;

    public static Queue queue;
    public static Queue queue1;

    public static Topic topic;
    public static Topic topic1;

    @Override
    public void init() throws ServletException {
        super.init();

        try {
            qcfBindings = (QueueConnectionFactory)
                new InitialContext().lookup("java:comp/env/jndi_JMS_BASE_QCF");
            tcfBindings = (QueueConnectionFactory)
                new InitialContext().lookup("java:comp/env/jndi_JMS_BASE_QCF1");
            qcfTCP = (TopicConnectionFactory)
                new InitialContext().lookup("java:comp/env/eis/tcf");
            tcfTCP = (TopicConnectionFactory)
                new InitialContext().lookup("java:comp/env/eis/tcf1");

            queue = (Queue) new InitialContext().lookup("java:comp/env/jndi_INPUT_Q");
            queue1 = (Queue) new InitialContext().lookup("java:comp/env/eis/queue1");

            topic = (Topic) new InitialContext().lookup("java:comp/env/eis/topic1");
            topic1 = (Topic) new InitialContext().lookup("java:comp/env/eis/topic2");

        } catch ( NamingException ex ) {
            ex.printStackTrace();
        }

        if ( qcfBindings == null ) {
            System.out.println("Null queue connection factory 'java:comp/env/jndi_JMS_BASE_QCF'");
        }
        if ( tcfBindings == null ) {
            System.out.println("Null queue connection factory 'java:comp/env/jndi_JMS_BASE_QCF1'");
        }
        if ( qcfTCP == null ) {
            System.out.println("Null topic connection factory 'java:comp/env/eis/tcf'");
        }
        if ( tcfTCP == null ) {
            System.out.println("Null topic connection factory 'java:comp/env/eis/tcf1'");
        }

        if ( queue == null ) {
            System.out.println("Null queue 'java:comp/env/jndi_INPUT_Q'");
        }
        if ( queue1 == null ) {
            System.out.println("Null queue 'java:comp/env/eis/queue1'");
        }

        if ( topic == null ) {
            System.out.println("Null topic 'java:comp/env/eis/topic1'");
        }
        if ( topic1 == null ) {
            System.out.println("Null topic 'java:comp/env/eis/topic2'");
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
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {

        String test = request.getParameter("test");

        PrintWriter out = response.getWriter();
        out.println("Starting " + test + "<br>");

        // The injection engine doesn't like this at the class level.
        TraceComponent tc = Tr.register(JMSProducer_118073Servlet.class);

        Tr.entry(this, tc, test);
        try {
            System.out.println(" Starting : " + test);
            getClass()
                .getMethod(test, HttpServletRequest.class, HttpServletResponse.class)
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

    //

    public void testSetGetJMSReplyTo_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext sendContext = qcfBindings.createContext();
        JMSProducer sendProducer = sendContext.createProducer();
        sendProducer.setJMSReplyTo(queue1);

        TextMessage sendMsg = sendContext.createTextMessage("testSetGetJMSReplyTo_B");
        sendProducer.send(queue, sendMsg);

        JMSContext receiveContext = qcfBindings.createContext();
        JMSConsumer receiveQueue = receiveContext.createConsumer(queue);
        TextMessage receiveMsg = (TextMessage) receiveQueue.receiveNoWait();

        Destination replyDestination = sendProducer.getJMSReplyTo();
        JMSProducer replyProducer = receiveContext.createProducer();
        TextMessage replyMsg = receiveContext.createTextMessage("testSetGetJMSReplyTo_B: Reply Msg");
        replyMsg.setJMSCorrelationID( receiveMsg.getJMSMessageID() );
        replyProducer.send(replyDestination, replyMsg);

        JMSConsumer receiveReplyQueue = sendContext.createConsumer(queue1);
        TextMessage receiveReplyMsg = (TextMessage) receiveReplyQueue.receiveNoWait();
        receiveReplyQueue.receiveNoWait();
        String receiveReplyID = new String( receiveReplyMsg.getJMSCorrelationID() );

        boolean testFailed = false;
        if ( !receiveReplyID.equals( sendMsg.getJMSMessageID() ) ) {
            testFailed = true;
        }

        sendContext.close();
        receiveContext.close();

        if ( testFailed ) {
            throw new Exception("testSetGetJMSReplyTo_B_SecOff failed");
        }
    }

    public void testSetGetJMSReplyTo_TCP_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext sendContext = qcfTCP.createContext();
        JMSProducer producer = sendContext.createProducer();
        producer.setJMSReplyTo(queue1);
        TextMessage sendMsg = sendContext.createTextMessage("testSetGetJMSReplyTo_TCP");
        producer.send(queue, sendMsg);

        JMSContext receiveContext = qcfTCP.createContext();
        JMSConsumer receiveQueue = receiveContext.createConsumer(queue);
        TextMessage receiveMsg = (TextMessage) receiveQueue.receiveNoWait();

        Destination replyDestination = producer.getJMSReplyTo();
        JMSProducer replyProducer = receiveContext.createProducer();
        TextMessage sendReplyMsg = receiveContext.createTextMessage("testSetGetJMSReplyTo_TCP: Reply Msg");
        sendReplyMsg.setJMSCorrelationID( receiveMsg.getJMSMessageID() );
        replyProducer.send(replyDestination, sendReplyMsg);

        JMSConsumer receiveReplyQueue = sendContext.createConsumer(queue1);
        TextMessage receiveReplyMsg = (TextMessage) receiveReplyQueue.receiveNoWait();
        receiveReplyQueue.receiveNoWait();
        String receiveReplyID = new String( receiveReplyMsg.getJMSCorrelationID() );

        boolean testFailed = false;
        if ( !receiveReplyID.equals( sendMsg.getJMSMessageID() ) ) {
            testFailed = true;
        }

        sendContext.close();
        receiveContext.close();

        if ( testFailed ) {
            throw new Exception("testSetGetJMSReplyTo_TCP_SecOff failed");
        }
    }

    public void testSetGetJMSReplyTo_Topic_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = tcfBindings.createContext();

        JMSProducer producer = jmsContext.createProducer();
        producer.setJMSReplyTo(topic1);

        String expectedReplyTo = "topic://Default.Topic?topicSpace=NewTopic2";
        String actualReplyTo = producer.getJMSReplyTo().toString();

        boolean testFailed = false;
        if ( !expectedReplyTo.equals(actualReplyTo) ) {
            testFailed = true;
        }

        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testSetGetJMSReplyTo_Topic_B_SecOff failed");
        }
    }

    public void testSetGetJMSReplyTo_Topic_TCP_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = tcfTCP.createContext();

        JMSProducer producer = jmsContext.createProducer();
        producer.setJMSReplyTo(topic1);

        String expectedReplyTo = "topic://Default.Topic?topicSpace=NewTopic2";
        String actualReplyTo = producer.getJMSReplyTo().toString();

        boolean testFailed = false;
        if ( !expectedReplyTo.equals(actualReplyTo) ) {
            testFailed = true;
        }

        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testSetGetJMSReplyTo_Topic_TCP_SecOff failed");
        }
    }

    public void testNullJMSReplyTo_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = qcfBindings.createContext();

        JMSProducer jmsProducer = jmsContext.createProducer();
        jmsProducer.setJMSReplyTo(null);
        jmsProducer.send(queue, "testNullJMSReplyTo_B");

        JMSConsumer jmsConsumer = jmsContext.createConsumer(queue);
        TextMessage msg = (TextMessage) jmsConsumer.receiveNoWait();
        Object replyTo = msg.getJMSReplyTo();

        boolean testFailed = false;
        if ( replyTo != null ) {
            testFailed = true;
        }

        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testNullJMSReplyTo_B_SecOff failed");
        }
    }

    public void testNullJMSReplyTo_TCP_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = qcfTCP.createContext();

        JMSProducer jmsProducer = jmsContext.createProducer();
        jmsProducer.setJMSReplyTo(null);
        jmsProducer.send(queue, "testNullJMSReplyTo_TCP");

        JMSConsumer jmsConsumer = jmsContext.createConsumer(queue);
        TextMessage msg = (TextMessage) jmsConsumer.receiveNoWait();
        Object replyTo = msg.getJMSReplyTo();

        boolean testFailed = false;
        if ( replyTo != null ) {
            testFailed = true;
        }

        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testNullJMSReplyTo_TCP_SecOff failed");
        }
    }

    public void testSetAsync_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = qcfBindings.createContext();
        JMSProducer producer = jmsContext.createProducer();

        boolean testFailed = false;
        try {
            producer.setAsync(null);
            testFailed = true;
        } catch ( JMSRuntimeException ex ) {
            ex.printStackTrace();
        }

        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testSetAsync_B_SecOff failed");
        }
    }

    public void testSetAsync_TCP_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = qcfTCP.createContext();
        JMSProducer producer = jmsContext.createProducer();

        boolean testFailed = false;
        try {
            producer.setAsync(null);
            testFailed = true;
        } catch ( JMSRuntimeException ex ) {
            ex.printStackTrace();
        }

        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testSetAsync_TCP_SecOff failed");
        }
    }

    public void testGetAsync_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = qcfBindings.createContext();
        JMSProducer producer = jmsContext.createProducer();

        CompletionListener listener = producer.getAsync();

        boolean testFailed = false;
        if ( listener != null ) {
            testFailed = true;
        }

        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testGetAsync_B_SecOff failed");
        }
    }

    public void testGetAsync_TCP_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = qcfTCP.createContext();
        JMSProducer producer = jmsContext.createProducer();

        CompletionListener listener = producer.getAsync();

        boolean testFailed = false;
        if ( listener != null ) {
            testFailed = true;
        }

        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testGetAsync_TCP_SecOff failed");
        }
    }
}
