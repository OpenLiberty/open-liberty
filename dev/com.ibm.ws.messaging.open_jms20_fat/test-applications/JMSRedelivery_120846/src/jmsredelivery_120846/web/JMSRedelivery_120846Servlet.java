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
package jmsredelivery_120846.web;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.Enumeration;

import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.JMSProducer;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
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
public class JMSRedelivery_120846Servlet extends HttpServlet {

    public void emptyQueue(QueueConnectionFactory qcf, Queue q) throws Exception {
        JMSContext context = qcf.createContext();

        int numMsgs = 0;

        QueueBrowser qb = context.createBrowser(q);
        Enumeration e = qb.getEnumeration();
        JMSConsumer consumer = context.createConsumer(q);
        while ( e.hasMoreElements() ) {
            Message message = (Message) e.nextElement();
            numMsgs++;
        }

        for ( int msgNo = 0; msgNo < numMsgs; msgNo++ ) {
            Message msg = consumer.receive();
        }

        context.close();
    }

    //

    // jmsQCFBindings points to jndi_JMS_BASE_QCF, which is the
    // connection factory used for remote connections.
    //
    // MDBs use the queue factory with this name.

    public static QueueConnectionFactory jmsQCFBindings;
    public static QueueConnectionFactory jmsQCFTCP;
    public static Queue jmsQueue;
    public static Queue jmsQueue1;
    public static Queue jmsQueue2;
    public static Queue jmsQueue10;
    public static Queue jmsQueue11;

    public static TopicConnectionFactory jmsTopicCF; // defect 175486
    public static Topic jmsTopic1;
    public static Topic jmsTopic2;

    @Override
    public void init() throws ServletException {
        super.init();

        try {
            jmsQCFBindings = (QueueConnectionFactory)
                new InitialContext().lookup("java:comp/env/jndi_JMS_BASE_QCF");
        } catch ( NamingException e ) {
            e.printStackTrace();
        }
        System.out.println("Queue connection factory 'java:comp/env/jndi_JMS_BASE_QCF':\n" + jmsQCFBindings);

        try {
            jmsQCFTCP = (QueueConnectionFactory)
                new InitialContext().lookup("java:comp/env/jndi_JMS_BASE_QCF1");
        } catch ( NamingException e ) {
            e.printStackTrace();
        }
        System.out.println("Queue connection factory 'java:comp/env/jndi_JMS_BASE_QCF1':\n" + jmsQCFTCP);

        try {
            jmsQueue = (Queue) new InitialContext().lookup("java:comp/env/jndi_INPUT_Q");
        } catch ( NamingException e ) {
            e.printStackTrace();
        }
        System.out.println("Queue 'jndi_INPUT_Q':\n" + jmsQueue);

        try {
            jmsQueue1 = (Queue) new InitialContext().lookup("java:comp/env/eis/queue1");
        } catch ( NamingException e ) {
            e.printStackTrace();
        }
        System.out.println("Queue 'eis/queue1':\n" + jmsQueue1);

        try {
            jmsQueue2 = (Queue) new InitialContext().lookup("java:comp/env/eis/queue2");
        } catch ( NamingException e ) {
            e.printStackTrace();
        }
        System.out.println("Queue 'eis/queue2':\n" + jmsQueue2);

        try {
            jmsQueue10 = (Queue) new InitialContext().lookup("java:comp/env/eis/queue/test");
        } catch ( NamingException e ) {
            e.printStackTrace();
        }
        System.out.println("Queue 'eis/queue/test':\n" + jmsQueue10);

        try {
            jmsQueue11 = (Queue) new InitialContext().lookup("java:comp/env/eis/Queue11/test");
        } catch ( NamingException e ) {
            e.printStackTrace();
        }
        System.out.println("Queue 'eis/Queue11/test':\n" + jmsQueue11);

        // defect 175486
        try {
            jmsTopicCF = (TopicConnectionFactory)
                new InitialContext().lookup("java:comp/env/eis/tcf");
        } catch ( NamingException e ) {
            e.printStackTrace();
        }
        System.out.println("Topic connection factory 'java:comp/env/eis/tcf':\n" + jmsTopicCF);

        try {
            jmsTopic1 = (Topic) new InitialContext().lookup("java:comp/env/eis/topic1");
        } catch ( NamingException e ) {
            e.printStackTrace();
        }
        System.out.println("Topic 'java:comp/env/eis/topic1':\n" + jmsTopic1);

        try {
            jmsTopic2 = (Topic) new InitialContext().lookup("java:comp/env/eis/topic2");
        } catch ( NamingException e ) {
            e.printStackTrace();
        }
        System.out.println("Topic 'java:comp/env/eis/topic2':\n" + jmsTopic2);

        if ( (jmsQCFBindings == null) || (jmsQCFTCP == null) ||
             (jmsQueue == null) || (jmsQueue1 == null) || (jmsQueue2 == null) ||
             (jmsQueue10 == null) || (jmsQueue11 == null) ||
             (jmsTopicCF == null) || (jmsTopic1 == null) || (jmsTopic2 == null) ) {
            throw new ServletException("Failed JMS initialization");
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
        TraceComponent tc = Tr.register(JMSRedelivery_120846Servlet.class);

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

    public void testInitialJMSXDeliveryCount_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContext = jmsQCFBindings.createContext();
        emptyQueue(jmsQCFBindings, jmsQueue);

        JMSProducer producer = jmsContext.createProducer();
        producer.send(jmsQueue, "testInitialJMSXDeliveryCount_B_SecOff");

        JMSConsumer consumer = jmsContext.createConsumer(jmsQueue);
        TextMessage msgIn = (TextMessage) consumer.receive(5000);

        boolean testFailed = false;
        if ( msgIn.getIntProperty("JMSXDeliveryCount") != 1 ) {
            testFailed = true;
        }

        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testInitialJMSXDeliveryCount_B_SecOff failed");
        }
    }

    public void testInitialJMSXDeliveryCount_TcpIp_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContext = jmsQCFTCP.createContext();
        emptyQueue(jmsQCFTCP, jmsQueue);

        JMSProducer producer = jmsContext.createProducer();
        producer.send(jmsQueue, "testInitialJMSXDeliveryCount_TcpIp_SecOff");

        JMSConsumer consumer = jmsContext.createConsumer(jmsQueue);
        TextMessage msgIn = (TextMessage) consumer.receive(5000);

        boolean testFailed = false;
        if ( msgIn.getIntProperty("JMSXDeliveryCount") != 1) {
            testFailed = true;
        }

        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testInitialJMSXDeliveryCount_TcpIp_SecOff failed");
        }
    }

    public void testRDC_B(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        Queue sendQueue = jmsQueue1;
        Queue replyQueue = jmsQueue;

        JMSContext context = jmsQCFBindings.createContext();
        JMSConsumer receiver = context.createConsumer(replyQueue);

        emptyQueue(jmsQCFBindings, replyQueue);
        emptyQueue(jmsQCFBindings, sendQueue);

        String msgOutText = "testRDC_B";
        TextMessage msgOut = context.createTextMessage(msgOutText);
        context.createProducer().send(sendQueue, msgOut);

        MapMessage msgIn = (MapMessage) receiver.receive(30000);

        boolean testFailed = false;
        if ( (msgIn == null) || !msgOutText.equals( msgIn.getStringProperty("msgText") ) ||
             !msgIn.getBooleanProperty("redelivered") ||
             (msgIn.getIntProperty("deliveryCount") != 2) ) {
            testFailed = true;
        }

        context.close();

        if ( testFailed ) {
            throw new Exception("testRDC_B failed: msgIn [ " + msgIn + " ]");
        }
    }

    public void testRDC_TCP(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        Queue sendQueue = jmsQueue1;
        Queue receiveQueue = jmsQueue;

        JMSContext context = jmsQCFBindings.createContext();
        emptyQueue(jmsQCFBindings, sendQueue);
        emptyQueue(jmsQCFBindings, receiveQueue);

        String msgOutText = "testRDC_TcpIp";
        TextMessage msgOut = context.createTextMessage(msgOutText);
        context.createProducer().send(sendQueue, msgOut);

        JMSConsumer receiver = context.createConsumer(receiveQueue);
        MapMessage msgIn = (MapMessage) receiver.receive(30000);

        boolean testFailed = false;
        if ( (msgIn == null) || !msgOutText.equals( msgIn.getStringProperty("msgText") ) ||
             !msgIn.getBooleanProperty("redelivered") ||
             (msgIn.getIntProperty("deliveryCount") != 2) ) {
            testFailed = true;
        }

        context.close();

        if ( testFailed ) {
            throw new Exception("testRDC_TcpIp failed");
        }
    }

    public void testMaxRDC_B(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        Queue sendQueue = jmsQueue2;
        Queue receiveQueue = jmsQueue;

        JMSContext context = jmsQCFBindings.createContext();

        emptyQueue(jmsQCFBindings, sendQueue);
        emptyQueue(jmsQCFBindings, receiveQueue);

        TextMessage msgOut = context.createTextMessage("testMaxRDC_B");
        context.createProducer().send(sendQueue, msgOut);

        JMSConsumer receiver = context.createConsumer(receiveQueue);
        TextMessage msgIn = (TextMessage) receiver.receive(30000);

        boolean testFailed = false;
        if ( msgIn != null ) {
            testFailed = true;
        }

        context.close();

        if ( testFailed ) {
            throw new Exception("testMaxRDC_B failed");
        }
    }

    public void testMaxRDC_TCP(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        Queue sendQueue = jmsQueue2;
        Queue receiveQueue = jmsQueue;

        JMSContext context = jmsQCFBindings.createContext();

        emptyQueue(jmsQCFBindings, sendQueue);
        emptyQueue(jmsQCFBindings, receiveQueue);

        TextMessage msgOut = context.createTextMessage("testMaxRDC_TcpIp");
        context.createProducer().send(sendQueue, msgOut);

        JMSConsumer receiver = context.createConsumer(receiveQueue);
        TextMessage msgIn = (TextMessage) receiver.receive(30000);

        boolean testFailed = false;
        if ( msgIn != null ) {
            testFailed = true;
        }

        context.close();

        if ( testFailed ) {
            throw new Exception("testMaxRDC_TcpIp failed");
        }
    }

    public void testQueueSendMDB(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext context = jmsQCFBindings.createContext();
        emptyQueue(jmsQCFBindings, jmsQueue10);

        TextMessage msg = context.createTextMessage("testQueueSendMDB");
        context.createProducer().send(jmsQueue10, msg);

        context.close();
    }

    public void testAnnotatedMDB(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext context = jmsQCFBindings.createContext();
        emptyQueue(jmsQCFBindings, jmsQueue11);

        TextMessage msg = context.createTextMessage("testAnnotatedMDB");
        context.createProducer().send(jmsQueue11, msg);
    }

    // defect 175486
    public void testTopicSendMDB(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext context = jmsTopicCF.createContext();

        JMSConsumer jmsConsumer = context.createSharedConsumer(jmsTopic1, "DURATEST1");
        JMSProducer jmsProducer = context.createProducer();

        TextMessage msg = context.createTextMessage("testTopicSendMDB");
        jmsProducer.send(jmsTopic1, msg);

        context.close();
    }

    public void testTopicAnnotatedMDB(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext context = jmsTopicCF.createContext();

        JMSConsumer jmsConsumer = context.createSharedConsumer(jmsTopic2, "DURATEST1");
        JMSProducer jmsProducer = context.createProducer();

        TextMessage msg = context.createTextMessage("testTopicAnnotatedMDB");
        jmsProducer.send(jmsTopic2, msg);

        context.close();
    }

    public void testTargetChain_B(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext context = jmsQCFTCP.createContext();
        JMSProducer jmsProducer = context.createProducer();

        TextMessage msg = context.createTextMessage("testin_TargetTransportChain");
        jmsProducer.send(jmsQueue1, msg);

        context.close();
    }
}
