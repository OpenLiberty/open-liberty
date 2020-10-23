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
package jmsdcf.web;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.Enumeration;

import javax.annotation.Resource;
import javax.inject.Inject;
import javax.jms.ConnectionFactory;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.JMSException;
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

@SuppressWarnings("serial")
public class JMSDCFServlet extends HttpServlet {

    public static QueueConnectionFactory jmsQCF;
    public static Queue jmsQueue;

    @Override
    public void init() throws ServletException {
        super.init();

        try {
            jmsQCF = (QueueConnectionFactory)
                new InitialContext().lookup("java:comp/env/DefaultJMSConnectionFactory");
        } catch ( NamingException e ) {
            e.printStackTrace();
        }
        System.out.println("Queue connection factory 'java:comp/env/jndi_JMS_BASE_QCF':\n" + jmsQCF);

        try {
            // jmsQueue = (Queue) new InitialContext().lookup("java:comp/env/jndi_INPUT_Q");
            jmsQueue = (Queue) new InitialContext().lookup("jndi_INPUT_Q");
        } catch ( NamingException e ) {
            e.printStackTrace();
        }
        // System.out.println("Queue 'java:comp/env/jndi_INPUT_Q' [ " + jmsQueue + " ]");
        System.out.println("Queue 'jndi_INPUT_Q' [ " + jmsQueue + " ]");
        if ( jmsQueue == null ) {
            throw new ServletException("Failed JMS initialization");
        }
    }

    @Resource(lookup = "java:comp/env/DefaultJMSConnectionFactory")
    ConnectionFactory jmsCF;

    @Inject
    private JMSContext jmsContext;

    @Resource(name = "myJMSCF")
    ConnectionFactory myJMScf;

    //

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
        TraceComponent tc = Tr.register(JMSDCFServlet.class);

        Tr.entry(this, tc, test);
        try {
            getClass()
                .getMethod(test, HttpServletRequest.class, HttpServletResponse.class)
                .invoke(this, request, response);

            System.out.println(" Starting : " + test);
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
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        System.out.println("testP2P_B_SecOff");
        System.out.println("  Injected 'jmsCF' [ java:comp/env/DefaultJMSConnectionFactory ]: [ " + jmsCF + " ]");
        System.out.println("  Injected 'myJMScf' [ myJMSCF ]: [ " + myJMScf + " ]");

        JMSContext jmsContextQueue = jmsCF.createContext();

        JMSConsumer jmsConsumer = jmsContextQueue.createConsumer(jmsQueue);

        TextMessage msgOut = jmsContextQueue.createTextMessage("testP2P_B_SecOff");
        jmsContextQueue.createProducer().send(jmsQueue, msgOut);

        TextMessage msgIn = (TextMessage) jmsConsumer.receive(500);

        String failureMessage = null;
        if ( msgIn == null ) {
            failureMessage = "Message not received";
        } else if ( !msgIn.getText().equals("testP2P_B_SecOff") ) {
            failureMessage = "Expected [ testP2P_B_SecOff ] received [ " + msgIn.getText() + " ]";
        }

        jmsContextQueue.close();

        if ( failureMessage != null ) {
            throw new Exception("testP2P_B_SecOff failed: " + failureMessage);
        }
    }

    // Test @inject using the default configuration factory

    public void testP2P_B_SecOff_inject(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);

        TextMessage msgOut = jmsContext.createTextMessage("testP2P_B_SecOff_inject");
        jmsContext.createProducer().send(jmsQueue, msgOut);

        TextMessage msgIn = (TextMessage) jmsConsumer.receive(500);

        boolean exceptionFlag = false;
        if ( (msgIn == null) || !msgIn.getText().equals("testP2P_B_SecOff_inject") ) {
            exceptionFlag = true;
        }

        if ( exceptionFlag ) {
            throw new Exception("testP2P_B_SecOff_inject failed: Expected message was not received");
        }
    }

    public void testPubSub_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContextTopic = jmsCF.createContext();
        Topic jmsTopic = jmsContextTopic.createTopic("Topic1");

        JMSConsumer jmsConsumer = jmsContextTopic.createConsumer(jmsTopic);

        TextMessage msgOut = jmsContextTopic.createTextMessage("testPubSub_B_SecOff");
        jmsContextTopic.createProducer().send(jmsTopic, msgOut);

        TextMessage msgIn = (TextMessage) jmsConsumer.receive(500);

        boolean exceptionFlag = false;
        if ( (msgIn == null) || !msgIn.getText().equals("testPubSub_B_SecOff") ) {
            exceptionFlag = true;
        }

        jmsContextTopic.close();

        if ( exceptionFlag ) {
            throw new Exception("testPubSub_B_SecOff failed: Expected message was not received");
        }
    }

    public void testPubSub_B_SecOff_implicitBinding(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContextTopic = myJMScf.createContext();
        Topic jmsTopic = jmsContextTopic.createTopic("Topic1");

        JMSConsumer jmsConsumer = jmsContextTopic.createConsumer(jmsTopic);

        TextMessage msgOut = jmsContextTopic.createTextMessage("testPubSub_B_SecOff");
        jmsContextTopic.createProducer().send(jmsTopic, msgOut);

        TextMessage msgIn = (TextMessage) jmsConsumer.receive(500);

        boolean exceptionFlag = false;
        if ( (msgIn == null) || !msgIn.getText().equals("testPubSub_B_SecOff") ) {
            exceptionFlag = true;
        }

        jmsContextTopic.close();

        if ( exceptionFlag ) {
            throw new Exception("testPubSub_B_SecOff_implicitBinding failed: Expected message was not received");
        }
    }

    public void testPubSubDurable_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContextTopic = jmsCF.createContext();
        Topic jmsTopic = jmsContextTopic.createTopic("Topic1");

        JMSConsumer jmsConsumer = jmsContextTopic.createDurableConsumer(jmsTopic, "sub1");

        TextMessage msgOut = jmsContextTopic.createTextMessage("testPubSubDurable_B_SecOff");
        jmsContextTopic.createProducer().send(jmsTopic, msgOut);

        TextMessage msgIn = (TextMessage) jmsConsumer.receive(500);

        boolean exceptionFlag = false;
        if ( (msgIn == null) || !msgIn.getText().equals("testPubSubDurable_B_SecOff") ) {
            exceptionFlag = true;
        }

        jmsContextTopic.close();

        if ( exceptionFlag ) {
            throw new Exception("testPubSubDurable_B_SecOff failed: Expected message was not received");
        }
    }

    public void testMessageOrder_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContextQueue1 = jmsCF.createContext();
        JMSContext jmsContextQueue2 = jmsCF.createContext();

        JMSConsumer jmsConsumer = jmsContextQueue1.createConsumer(jmsQueue);

        JMSProducer producer1 = jmsContextQueue1.createProducer();
        JMSProducer producer2 = jmsContextQueue2.createProducer();

        int msgOrder[] = new int[10];
        for ( int msgNo = 0; msgNo < 10; msgNo++ ) {
            Message msg = null;

            if ( msgNo % 2 == 0 ) {
                msg = jmsContextQueue1.createMessage();
                msg.setIntProperty("Message_Order", msgNo);
                producer1.send(jmsQueue, msg);

            } else {
                msg = jmsContextQueue2.createMessage();
                msg.setIntProperty("Message_Order", msgNo);
                producer2.send(jmsQueue, msg);
            }

            int msgIn = jmsConsumer.receive(1000).getIntProperty("Message_Order");
            msgOrder[msgNo] = msgIn;
        }

        int redFlag = 0;
        for ( int msgNo = 0; msgNo < 10; msgNo++ ) {
            System.out.println("Message [ " + msgNo + " ] [ " + msgOrder[msgNo] + " ]");
            if ( msgOrder[msgNo] != msgNo ) {
                redFlag++;
            }
        }

        boolean exceptionFlag = false;
        if ( redFlag != 0 ) {
            exceptionFlag = true;
        }

        jmsContextQueue1.close();
        jmsContextQueue2.close();

        if ( exceptionFlag ) {
            throw new Exception("testMessageOrder_B_SecOff failed: Messages were not received in the expected order");
        }
    }

    public void testBrowser_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContextQueue = jmsCF.createContext();

        JMSConsumer jmsConsumer = jmsContextQueue.createConsumer(jmsQueue);

        TextMessage msgOut = jmsContextQueue.createTextMessage("testBrowser_B_SecOff");
        jmsContextQueue.createProducer().send(jmsQueue, msgOut);

        int numMsgs = 0;
        QueueBrowser qb = jmsContextQueue.createBrowser(jmsQueue);
        Enumeration e = qb.getEnumeration();
        while ( e.hasMoreElements() ) {
            e.nextElement();
            numMsgs++;
        }

        TextMessage msgIn = (TextMessage) jmsConsumer.receive(1000);

        boolean exceptionFlag = false;
        if ( (numMsgs != 1) ||(msgIn == null) || !msgIn.getText().equals("testBrowser_B_SecOff") ) {
            exceptionFlag = true;
        }

        jmsContextQueue.close();

        if ( exceptionFlag ) {
            throw new Exception("testBrowser_B_SecOff failed: Expected message was not received");
        }
    }

    public void testObjects(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        ConnectionFactory lookupcf = (ConnectionFactory)
            new InitialContext().lookup("java:comp/DefaultJMSConnectionFactory");

        boolean exceptionFlag = false;

        if ( !lookupcf.equals(myJMScf) ) {
            exceptionFlag = true;
        }

        if ( exceptionFlag ) {
            throw new Exception("testObjects failed: The two objects are the not the same");
        }
    }
}
