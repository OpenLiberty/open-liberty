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
package jmscontextinject.web;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.Enumeration;

import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.jms.IllegalStateRuntimeException;
import javax.jms.JMSConnectionFactory;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.JMSProducer;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.jms.QueueConnectionFactory;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import jmscontextinject.ejb.SampleSecureStatelessBean;

@SuppressWarnings("serial")
public class JMSContextInjectServlet extends HttpServlet {

    public static QueueConnectionFactory jmsQCFBindings;
    public static QueueConnectionFactory jmsQCFTCP;
    public static Queue jmsQueue;

    public void emptyQueue(QueueConnectionFactory qcf, Queue q) throws Exception {
        JMSContext context = qcf.createContext();
        JMSConsumer consumer = context.createConsumer(q);

        int numMsgs = 0;

        QueueBrowser qb = context.createBrowser(q);
        Enumeration e = qb.getEnumeration();
        while ( e.hasMoreElements() ) {
            Message message = (Message) e.nextElement();
            numMsgs++;
        }

        for ( int msgNo = 0; msgNo < numMsgs; msgNo++ ) {
            Message message = consumer.receive();
        }

        context.close();
    }

    @Override
    public void init() throws ServletException {
        super.init();

        try {
            jmsQCFBindings = (QueueConnectionFactory)
                new InitialContext().lookup("java:comp/env/jndi_JMS_BASE_QCF");
            jmsQCFTCP = (QueueConnectionFactory)
                new InitialContext().lookup("java:comp/env/jndi_JMS_BASE_QCF1");
            jmsQueue = (Queue)
                new InitialContext().lookup("java:comp/env/jndi_INPUT_Q");

        } catch ( NamingException ex ) {
            ex.printStackTrace();
        }

        if ( jmsQCFBindings == null ) {
            System.out.println("Null queue connection factory 'java:comp/env/jndi_JMS_BASE_QCF'");
        }
        if ( jmsQCFTCP == null ) {
            System.out.println("Null queue connection factory 'java:comp/env/jndi_JMS_BASE_QCF1'");
        }
        if ( jmsQueue == null ) {
            System.out.println("Null queue 'java:comp/env/jndi_INPUT_Q'");
        }
    }

    //

    @Inject
    @JMSConnectionFactory("java:comp/env/jndi_JMS_BASE_QCF")
    private JMSContext jmsContextQueue;

    @Inject
    @JMSConnectionFactory("java:comp/env/jndi_JMS_BASE_QCF1")
    private JMSContext jmsContextQueueTCP;

    @Inject
    @JMSConnectionFactory("java:comp/env/eis/qcf")
    private JMSContext jmsContextQueueNew;

    @Inject
    @JMSConnectionFactory("java:comp/env/eis/qcf1")
    private JMSContext jmsContextQueueNewTCP;

    @Inject
    @JMSConnectionFactory("java:comp/env/eis/tcf")
    private JMSContext jmsContextTopic;

    @Inject
    @JMSConnectionFactory("java:comp/env/eis/tcf1")
    private JMSContext jmsContextTopicTCP;

    @EJB
    SampleSecureStatelessBean statelessBean;

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
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {

        String test = request.getParameter("test");

        PrintWriter out = response.getWriter();
        out.println("Starting " + test + "<br>");

        // The injection engine doesn't like this at the class level.
        TraceComponent tc = Tr.register(JMSContextInjectServlet.class);

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

    public void testP2P_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        String methodName = "testP2P_B_SecOff";

        TextMessage messageOut = jmsContextQueue.createTextMessage(methodName);
        jmsContextQueue.createProducer().send(jmsQueue, messageOut);

        JMSConsumer jmsConsumer = jmsContextQueue.createConsumer(jmsQueue);
        TextMessage messageIn = (TextMessage) jmsConsumer.receive(500);

        String testFailure = null;
        if ( messageIn == null ) {
            testFailure = "Null received message";
        } else if ( !messageIn.getText().equals(methodName) ) {
            testFailure =
                "Incorrect received message:" +
                " Actual [ " + messageIn.getText() + " ]" +
                " Expected [ " + methodName + " ]";
        }

        if ( testFailure != null ) {
            throw new Exception("testP2P_B_SecOff failed: " + testFailure);
        }
    }

    public void testP2P_TCP_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        TextMessage messageOut = jmsContextQueueTCP.createTextMessage("testP2P_TCP_SecOff");
        jmsContextQueueTCP.createProducer().send(jmsQueue, messageOut);

        JMSConsumer jmsConsumer = jmsContextQueueTCP.createConsumer(jmsQueue);
        TextMessage messageIn = (TextMessage) jmsConsumer.receive(500);

        boolean testFailed = false;
        if ( (messageIn == null) || !messageIn.getText().equals("testP2P_TCP_SecOff") ) {
            testFailed = true;
        }

        if ( testFailed ) {
            throw new Exception("testP2P_TCP_SecOff failed: Expected message was not received");
        }
    }

    public void testPubSub_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        String methodName = "testPubSub_B_SecOff";

        System.out.println("ENTER: " + methodName);

        TextMessage messageOut = jmsContextTopic.createTextMessage(methodName);
        Topic jmsTopic = jmsContextTopic.createTopic("Topic1");
        JMSConsumer jmsConsumer = jmsContextTopic.createConsumer(jmsTopic);

        jmsContextTopic.createProducer().send(jmsTopic, messageOut);
        TextMessage messageIn = (TextMessage) jmsConsumer.receive(500);

        String testFailure = null;
        if ( messageIn == null ) {
            testFailure = "Null received message";
        } else if ( !messageIn.getText().equals(methodName) ) {
            testFailure =
                "Incorrect received message:" +
                " Actual [ " + messageIn.getText() + " ]" +
                " Expected [ " + methodName + " ]";
        }

        System.out.println("RETURN: " + methodName + ": " + ((testFailure == null) ? "SUCCESS" : "FAILED"));

        if ( testFailure != null ) {
            throw new Exception("testPubSub_B_SecOff failed: " + testFailure);
        }
    }

    public void testPubSub_TCP_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        TextMessage message = jmsContextTopicTCP.createTextMessage("testPubSub_TCP_SecOff");
        Topic jmsTopic = jmsContextTopicTCP.createTopic("Topic1");
        JMSConsumer jmsConsumer = jmsContextTopicTCP.createConsumer(jmsTopic);

        jmsContextTopicTCP.createProducer().send(jmsTopic, message);
        TextMessage messageIn = (TextMessage) jmsConsumer.receive(500);

        boolean testFailed = false;
        if ( (messageIn == null) || !messageIn.getText().equals("testPubSub_TCP_SecOff") ) {
            testFailed = true;
        }

        if ( testFailed ) {
            throw new Exception("testPubSub_TCP_SecOff failed: Expected message was not received");
        }
    }

    public void testPubSubDurable_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        TextMessage message = jmsContextTopic.createTextMessage("testPubSubDurable_B_SecOff");
        Topic jmsTopic = jmsContextTopic.createTopic("Topic1");
        jmsContextTopic.createProducer().send(jmsTopic, message);

        JMSConsumer jmsConsumer = jmsContextTopic.createDurableConsumer(jmsTopic, "sub1");
        TextMessage messageIn = (TextMessage) jmsConsumer.receive(500);

        boolean testFailed = false;
        if ( (messageIn == null) || !messageIn.getText().equals("testPubSubDurable_B_SecOff") ) {
            testFailed = true;
        }

        if ( testFailed ) {
            throw new Exception("testPubSubDurable_B_SecOff failed: Expected message was not received");
        }
    }

    public void testPubSubDurable_TCP_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        TextMessage message = jmsContextTopicTCP.createTextMessage("testPubSubDurable_TCP_SecOff");
        Topic jmsTopic = jmsContextTopicTCP.createTopic("Topic1");
        jmsContextTopicTCP.createProducer().send(jmsTopic, message);

        JMSConsumer jmsConsumer = jmsContextTopicTCP.createDurableConsumer(jmsTopic, "sub2");
        TextMessage messageIn = (TextMessage) jmsConsumer.receive(500);

        boolean testFailed = false;
        if ( (messageIn == null) || !messageIn.getText().equals("testPubSubDurable_TCP_SecOff") ) {
            testFailed = true;
        }

        if ( testFailed ) {
            throw new Exception("testPubSubDurable_TCP_SecOff failed: Expected message was not received");
        }
    }

    public void testNegativeSetters_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        Topic jmsTopic = jmsContextTopic.createTopic("Topic1");
        JMSConsumer jmsConsumer = jmsContextTopic.createConsumer(jmsTopic, "sub1");

        boolean testFailed = false;

        try {
            jmsContextTopic.setClientID("cid");
            testFailed = true;
        } catch ( IllegalStateRuntimeException ex ) {
            ex.printStackTrace();
        }

        try {
            jmsContextTopic.setExceptionListener(null);
            testFailed = true;
        } catch ( IllegalStateRuntimeException ex ) {
            ex.printStackTrace();
        }

        try {
            jmsContextTopic.stop();
            testFailed = true;
        } catch ( IllegalStateRuntimeException ex ) {
            ex.printStackTrace();
        }

        try {
            jmsContextTopic.acknowledge();
            testFailed = true;
        } catch ( IllegalStateRuntimeException ex ) {
            ex.printStackTrace();
        }

        try {
            jmsContextTopic.commit();
            testFailed = true;
        } catch ( IllegalStateRuntimeException ex ) {
            ex.printStackTrace();
        }

        try {
            jmsContextTopic.rollback();
            testFailed = true;
        } catch ( IllegalStateRuntimeException ex ) {
            ex.printStackTrace();
        }

        try {
            jmsContextTopic.recover();
            testFailed = true;
        } catch ( IllegalStateRuntimeException ex ) {
            ex.printStackTrace();
        }

        try {
            jmsContextTopic.setAutoStart(true);
            testFailed = true;
        } catch ( IllegalStateRuntimeException ex ) {
            ex.printStackTrace();
        }

        try {
            jmsContextTopic.start();
            testFailed = true;
        } catch ( IllegalStateRuntimeException ex ) {
            ex.printStackTrace();
        }

        try {
            jmsContextTopic.close();
            testFailed = true;
        } catch ( IllegalStateRuntimeException ex ) {
            ex.printStackTrace();
        }

        if ( testFailed ) {
            throw new Exception("testNegativeSetters_B_SecOff failed: Expected exception was not seen");
        }
    }

    public void testNegativeSetters_TCP_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        boolean testFailed = false;

        try {
            jmsContextTopicTCP.setClientID("cid");
            testFailed = true;
        } catch ( IllegalStateRuntimeException ex ) {
            ex.printStackTrace();
        }

        try {
            jmsContextTopicTCP.setExceptionListener(null);
            testFailed = true;
        } catch ( IllegalStateRuntimeException ex ) {
            ex.printStackTrace();
        }

        try {
            jmsContextTopicTCP.stop();
            testFailed = true;
        } catch ( IllegalStateRuntimeException ex ) {
            ex.printStackTrace();
        }

        try {
            jmsContextTopicTCP.acknowledge();
            testFailed = true;
        } catch ( IllegalStateRuntimeException ex ) {
            ex.printStackTrace();
        }

        try {
            jmsContextTopicTCP.commit();
            testFailed = true;
        } catch ( IllegalStateRuntimeException ex ) {
            ex.printStackTrace();
        }

        try {
            jmsContextTopicTCP.rollback();
            testFailed = true;
        } catch ( IllegalStateRuntimeException ex ) {
            ex.printStackTrace();
        }

        try {
            jmsContextTopicTCP.recover();
            testFailed = true;
        } catch ( IllegalStateRuntimeException ex ) {
            ex.printStackTrace();
        }

        try {
            jmsContextTopicTCP.setAutoStart(true);
            testFailed = true;
        } catch ( IllegalStateRuntimeException ex ) {
            ex.printStackTrace();
        }

        try {
            jmsContextTopicTCP.start();
            testFailed = true;
        } catch ( IllegalStateRuntimeException ex ) {
            ex.printStackTrace();
        }

        try {
            jmsContextTopicTCP.close();
            testFailed = true;
        } catch ( IllegalStateRuntimeException ex ) {
            ex.printStackTrace();
        }

        if ( testFailed ) {
            throw new Exception("testNegativeSetters_TCP_SecOff failed: Expected exception was not seen");
        }
    }

    public void testMessageOrder_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSProducer producer1 = jmsContextQueue.createProducer();
        JMSProducer producer2 = jmsContextQueueNew.createProducer();
        JMSConsumer jmsConsumer = jmsContextQueue.createConsumer(jmsQueue);

        int msgOrder[] = new int[10];

        for ( int msgNo = 0; msgNo < 10; msgNo++ ) {
            if ( (msgNo % 2) == 0 ) {
                Message msg = jmsContextQueue.createMessage();
                msg.setIntProperty("Message_Order", msgNo);
                producer1.send(jmsQueue, msg);
            } else {
                Message msg = jmsContextQueueNew.createMessage();
                msg.setIntProperty("Message_Order", msgNo);
                producer2.send(jmsQueue, msg);
            }

            int msgRcvd = jmsConsumer.receive(1000).getIntProperty("Message_Order");
            System.out.println("Received message [ " + msgRcvd + " ]");
            msgOrder[msgNo] = msgRcvd;
        }

        int outOfOrderCount = 0;
        for ( int msgNo = 0; msgNo < 10; msgNo++ ) {
            if ( msgOrder[msgNo] != msgNo ) {
                outOfOrderCount++;
            }
        }

        boolean testFailed = false;
        if ( outOfOrderCount != 0 ) {
            testFailed = true;
        }

        if ( testFailed ) {
            throw new Exception("testMessageOrder_B_SecOff failed: Messages were not received in the expected order");
        }
    }

    public void testMessageOrder_TCP_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSProducer producer1 = jmsContextQueueTCP.createProducer();
        JMSProducer producer2 = jmsContextQueueNewTCP.createProducer();
        JMSConsumer jmsConsumer = jmsContextQueueTCP.createConsumer(jmsQueue);

        int msgOrder[] = new int[10];

        for ( int msgNo = 0; msgNo < 10; msgNo++ ) {
            if ( (msgNo % 2) == 0 ) {
                Message msg = jmsContextQueueTCP.createMessage();
                msg.setIntProperty("Message_Order", msgNo);
                producer1.send(jmsQueue, msg);
            } else {
                Message msg = jmsContextQueueNewTCP.createMessage();
                msg.setIntProperty("Message_Order", msgNo);
                producer2.send(jmsQueue, msg);
            }

            int msgRcvd = jmsConsumer.receive(1000).getIntProperty("Message_Order");
            System.out.println("Received message number [ " + msgRcvd + " ]");
            msgOrder[msgNo] = msgRcvd;
        }

        int outOfOrderCount = 0;
        for ( int msgNo = 0; msgNo < 10; msgNo++ ) {
            if ( msgOrder[msgNo] != msgNo ) {
                outOfOrderCount++;
            }
        }

        boolean testFailed = false;
        if ( outOfOrderCount != 0 ) {
            testFailed = true;
        }

        if ( testFailed ) {
            throw new Exception("testMessageOrder_TCP_SecOff failed: Messages were not received in the expected order");
        }
    }

    public void testGetAutoStart_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        boolean testFailed = false;
        if ( !jmsContextQueue.getAutoStart() ) {
            testFailed = true;
        }

        if ( testFailed ) {
            throw new Exception("testGetAutoStart_B_SecOff failed: getAutoStart did not return true");
        }
    }

    public void testGetAutoStart_TCP_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        boolean testFailed = false;
        if ( !jmsContextQueueTCP.getAutoStart() ) {
            testFailed = true;
        }

        if ( testFailed ) {
            throw new Exception("testGetAutoStart_TCP_SecOff failed: getAutoStart did not return true");
        }
    }

    public void testBrowser_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        TextMessage messageOut = jmsContextQueue.createTextMessage("testBrowser_B_SecOff");
        jmsContextQueue.createProducer().send(jmsQueue, messageOut);

        int numMsgs = 0;
        QueueBrowser qb = jmsContextQueue.createBrowser(jmsQueue);
        Enumeration e = qb.getEnumeration();
        while ( e.hasMoreElements() ) {
            e.nextElement();
            numMsgs++;
        }

        JMSConsumer jmsConsumer = jmsContextQueue.createConsumer(jmsQueue);
        TextMessage messageIn = (TextMessage) jmsConsumer.receive(1000);

        boolean testFailed = false;
        if ( (numMsgs != 1) ||
             (messageIn == null) ||
             !messageIn.getText().equals("testBrowser_B_SecOff") ) {
            testFailed = true;
        }

        if ( testFailed ) {
            throw new Exception("testBrowser_B_SecOff failed: Expected message was not received");
        }
    }

    public void testBrowser_TCP_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        TextMessage messageOut = jmsContextQueueTCP.createTextMessage("testBrowser_TCP_SecOff");
        jmsContextQueueTCP.createProducer().send(jmsQueue, messageOut);

        int numMsgs = 0;
        QueueBrowser qb = jmsContextQueueTCP.createBrowser(jmsQueue);
        Enumeration e = qb.getEnumeration();
        while ( e.hasMoreElements() ) {
            e.nextElement();
            numMsgs++;
        }

        JMSConsumer jmsConsumer = jmsContextQueueTCP.createConsumer(jmsQueue);
        TextMessage messageIn = (TextMessage) jmsConsumer.receive(1000);

        boolean testFailed = false;
        if ( (numMsgs != 1) ||
             (messageIn == null) ||
             !messageIn.getText().equals("testBrowser_TCP_SecOff") ) {
            testFailed = true;
        }

        if ( testFailed ) {
            throw new Exception("testBrowser_TCP_SecOff failed: Expected message was not received");
        }
    }

    @TransactionAttribute(value = TransactionAttributeType.REQUIRED)
    public void testEJBCallSecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        String message = statelessBean.hello();
        System.out.println("Sent message [ " + message + " ]");
        statelessBean.sendMessage(message);

        TextMessage rec_msg = (TextMessage) jmsContextQueue.createConsumer(jmsQueue).receive(30000);
        System.out.println("Received message [ " + rec_msg.getText() + " ]");
    }
}
