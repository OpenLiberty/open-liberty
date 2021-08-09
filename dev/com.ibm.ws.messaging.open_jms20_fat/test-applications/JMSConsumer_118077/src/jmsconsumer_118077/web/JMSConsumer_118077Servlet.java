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
package jmsconsumer_118077.web;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.jms.BytesMessage;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.JMSProducer;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageFormatException;
import javax.jms.MessageFormatRuntimeException;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.jms.QueueConnectionFactory;
import javax.jms.StreamMessage;
import javax.jms.TextMessage;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.UserTransaction;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

@SuppressWarnings("serial")
public class JMSConsumer_118077Servlet extends HttpServlet {

    // JMSQueueConnectionFactory "java:comp/env/jndi_JMS_BASE_QCF"
    // JMSQueueConnectionFactory "java:comp/env/jndi_JMS_BASE_QCF1"
    // Queue "java:comp/env/jndi_INPUT_Q1"
    // UserTransaction "java:comp/UserTransaction"

    public void emptyQueue(QueueConnectionFactory qcf, Queue q) throws Exception {
        JMSContext context = qcf.createContext();

        int numMsgs = 0;
        QueueBrowser qb = context.createBrowser(q);
        Enumeration e = qb.getEnumeration();
        while ( e.hasMoreElements() ) {
            Message message = (Message) e.nextElement();
            numMsgs++;
        }

        JMSConsumer consumer = context.createConsumer(q);
        for ( int msgNo = 0; msgNo < numMsgs; msgNo++ ) {
            Message message = consumer.receive();
        }

        context.close();
    }

    //

    private static QueueConnectionFactory jmsQCFBindings;
    private static QueueConnectionFactory jmsQCFTCP;
    private static Queue jmsQueue;

    @Override
    public void init() throws ServletException {
        System.out.println("JMSConsumer_118077Servlet.init ENTRY");

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
            jmsQueue = (Queue)
                new InitialContext().lookup("java:comp/env/jndi_INPUT_Q1");
        } catch ( NamingException e ) {
            e.printStackTrace();
        }
        System.out.println("Queue 'java:comp/env/jndi_INPUT_Q1':\n" + jmsQueue);

        System.out.println("JMSConsumer_118077Servlet.init RETURN");

        if ( (jmsQCFBindings == null) || (jmsQCFTCP == null) || (jmsQueue == null) ) {
            throw new ServletException("Failed JMS initialization");
        }
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
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {

        String test = request.getParameter("test");

        PrintWriter out = response.getWriter();
        out.println("Starting " + test + "<br>");

        // The injection engine doesn't like this at the class level.
        TraceComponent tc = Tr.register(JMSConsumer_118077Servlet.class);

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

    public void testReceive_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContext = jmsQCFBindings.createContext();
        emptyQueue(jmsQCFBindings, jmsQueue);

        String msgOutText = "testReceive_B_SecOff";
        TextMessage msgOut = jmsContext.createTextMessage(msgOutText);
        jmsContext.createProducer().send(jmsQueue, msgOut);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);
        TextMessage msgIn = (TextMessage) jmsConsumer.receive();

        boolean failedTest = false;
        if ( (msgIn == null) ||
             (msgIn.getText() == null) || !msgIn.getText().equals(msgOutText) ) {
            failedTest = true;
        }

        jmsContext.close();

        if ( failedTest ) {
            throw new Exception("testReceive_B_SecOff failed: Expected message was not received");
        }
    }

    public void testReceive_TcpIp_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContext = jmsQCFTCP.createContext();
        emptyQueue(jmsQCFTCP, jmsQueue);

        String msgOutText = "testReceive_TcpIp_SecOff";
        TextMessage msgOut = jmsContext.createTextMessage(msgOutText);
        jmsContext.createProducer().send(jmsQueue, msgOut);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);
        TextMessage msgIn = (TextMessage) jmsConsumer.receive();

        boolean failedTest = false;
        if ( (msgIn == null) ||
             (msgIn.getText() == null) || !msgIn.getText().equals(msgOutText) ) {
            failedTest = true;
        }

        jmsContext.close();

        if ( failedTest ) {
            throw new Exception("testReceive_TcpIp_SecOff failed: Expected message was not received");
        }
    }

    public void testReceiveBody_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContext = jmsQCFBindings.createContext();
        emptyQueue(jmsQCFBindings, jmsQueue);

        String msgOutText = "testReceiveBody_B_SecOff";
        jmsContext.createProducer().send(jmsQueue, msgOutText);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);
        String msgIn = jmsConsumer.receiveBody(String.class);

        boolean failedTest = false;
        if ( (msgIn == null) || !msgIn.equals(msgOutText) ) {
            failedTest = true;
        }

        jmsContext.close();

        if ( failedTest ) {
            throw new Exception("testReceiveBody_B_SecOff failed: Expected message was not received");
        }
    }

    public void testReceiveBody_TcpIp_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContext = jmsQCFTCP.createContext();
        emptyQueue(jmsQCFTCP, jmsQueue);

        String msgOutText = "testReceiveBody_TcpIp_SecOff";
        jmsContext.createProducer().send(jmsQueue, msgOutText);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);
        String msgIn = jmsConsumer.receiveBody(String.class);

        boolean failedTest = false;
        if ( (msgIn == null) || !msgIn.equals(msgOutText) ) {
            failedTest = true;
        }

        jmsContext.close();

        if ( failedTest ) {
            throw new Exception("testReceiveBody_TcpIp_SecOff failed: Expected message was not received");
        }
    }

    public void testReceiveBodyTextMsg_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContext = jmsQCFBindings.createContext();
        emptyQueue(jmsQCFBindings, jmsQueue);

        String msgOutText = "testReceiveBodyTextMsg_B_SecOff";
        TextMessage msgOut = jmsContext.createTextMessage(msgOutText);
        jmsContext.createProducer().send(jmsQueue, msgOut);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);
        String msgIn = jmsConsumer.receiveBody(String.class);

        boolean failedTest = false;
        if ( (msgIn == null) || !msgIn.equals(msgOutText) ) {
            failedTest = true;
        }

        jmsContext.close();

        if ( failedTest ) {
            throw new Exception("testReceiveBodyTextMsg_B_SecOff failed: Expected message was not received");
        }
    }

    public void testReceiveBodyTextMsg_TcpIp_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContext = jmsQCFTCP.createContext();
        emptyQueue(jmsQCFTCP, jmsQueue);

        String msgOutText = "testReceiveBodyTextMsg_TcpIp_SecOff";
        TextMessage msgOut = jmsContext.createTextMessage(msgOutText);
        jmsContext.createProducer().send(jmsQueue, msgOut);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);
        String msgIn = jmsConsumer.receiveBody(String.class);

        boolean failedTest = false;
        if ( (msgIn == null) || !msgIn.equals(msgOutText) ) {
            failedTest = true;
        }

        jmsContext.close();

        if ( failedTest ) {
            throw new Exception("testReceiveBodyTextMsg_TcpIp_SecOff failed: Expected message was not received");
        }
    }

    public void testReceiveBodyByteMsg_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContext = jmsQCFBindings.createContext();
        emptyQueue(jmsQCFBindings, jmsQueue);

        byte[] msgOutBytes = new byte[] { 127, 0 };
        BytesMessage msgOut = jmsContext.createBytesMessage();
        msgOut.writeBytes(msgOutBytes);
        jmsContext.createProducer().send(jmsQueue, msgOut);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);
        byte[] msgIn = jmsConsumer.receiveBody(byte[].class);

        boolean failedTest = false;
        if ( (msgIn == null) || !Arrays.equals(msgIn, msgOutBytes) ) {
            failedTest = true;
        }

        jmsContext.close();

        if ( failedTest ) {
            throw new Exception("testReceiveBodyByteMsg_B_SecOff failed: Expected message was not received");
        }
    }

    public void testReceiveBodyByteMsg_TcpIp_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContext = jmsQCFTCP.createContext();
        emptyQueue(jmsQCFTCP, jmsQueue);

        byte[] msgOutBytes = new byte[] { 126, 1 };
        BytesMessage msgOut = jmsContext.createBytesMessage();
        msgOut.writeBytes(msgOutBytes);
        jmsContext.createProducer().send(jmsQueue, msgOut);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);
        byte[] msgIn = jmsConsumer.receiveBody(byte[].class);

        boolean failedTest = false;

        if ( (msgIn == null) || !Arrays.equals(msgIn, msgOutBytes) ) {
            failedTest = true;
        }

        jmsContext.close();

        if ( failedTest ) {
            throw new Exception("testReceiveBodyByteMsg_TcpIp_SecOff failed: Expected message was not received");
        }
    }

    public void testReceiveBodyMapMsg_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContext = jmsQCFBindings.createContext();
        emptyQueue(jmsQCFBindings, jmsQueue);

        MapMessage msgOut = jmsContext.createMapMessage();
        String nameOut = "testReceiveBodyMapMsg_B_SecOff";
        msgOut.setString("Name", nameOut);
        String secOut = "off";
        msgOut.setString("Security", secOut);
        jmsContext.createProducer().send(jmsQueue, msgOut);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);
        Map msgIn = jmsConsumer.receiveBody(Map.class);

        boolean failedTest = false;
        if ( msgIn == null ) {
            failedTest = true;
        } else {
            Object nameIn = msgIn.get("Name");
            if ( (nameIn == null) || !nameIn.equals(nameOut) ) {
                failedTest = true;
            }
            Object secIn = msgIn.get("Security");
            if ( (secIn == null) || !secIn.equals(secOut) ) {
                failedTest = true;
            }
        }

        jmsContext.close();

        if ( failedTest ) {
            throw new Exception("testReceiveBodyMapMsg_B_SecOff failed: Expected message was not received");
        }
    }

    public void testReceiveBodyMapMsg_TcpIp_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContext = jmsQCFTCP.createContext();
        emptyQueue(jmsQCFTCP, jmsQueue);

        MapMessage msgOut = jmsContext.createMapMessage();
        String nameOut = "testReceiveBodyMapMsg_TcpIp_SecOff";
        msgOut.setString("Name", "testReceiveBodyMapMsg_TcpIp_SecOff");
        String secOut = "off";
        msgOut.setString("Security", "off");
        jmsContext.createProducer().send(jmsQueue, msgOut);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);
        Map msgIn = jmsConsumer.receiveBody(Map.class);

        boolean failedTest = false;
        if ( msgIn == null ) {
            failedTest = true;
        } else {
            Object nameIn = msgIn.get("Name");
            if ( (nameIn == null) || !nameIn.equals(nameOut) ) {
                failedTest = true;
            }
            Object secIn = msgIn.get("Security");
            if ( (secIn == null) || !secIn.equals(secOut) ) {
                failedTest = true;
            }
        }

        jmsContext.close();

        if ( failedTest ) {
            throw new Exception("testReceiveBodyMapMsg_TcpIp_SecOff failed: Expected message was not received");
        }
    }

    public void testReceiveBodyObjectMsg_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContext = jmsQCFBindings.createContext();
        emptyQueue(jmsQCFBindings, jmsQueue);

        Object msgOutText = "testReceiveBodyObjectMsg_B_SecOff";
        ObjectMessage msgOut = jmsContext.createObjectMessage();
        msgOut.setObject((Serializable) msgOutText);
        jmsContext.createProducer().send(jmsQueue, msgOut);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);
        Object msgIn = jmsConsumer.receiveBody(Serializable.class);

        boolean failedTest = false;
        if ( (msgIn == null) || !msgIn.equals(msgOutText) ) {
            failedTest = true;
        }

        jmsContext.close();

        if ( failedTest ) {
            throw new Exception("testReceiveBodyObjectMsg_B_SecOff failed: Expected message was not received");
        }
    }

    public void testReceiveBodyObjectMsg_TcpIp_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContext = jmsQCFTCP.createContext();
        emptyQueue(jmsQCFTCP, jmsQueue);

        Object msgOutText = new String("testReceiveBodyObjectMsg_TcpIp_SecOff");
        ObjectMessage msgOut = jmsContext.createObjectMessage();
        msgOut.setObject((Serializable) msgOutText);
        jmsContext.createProducer().send(jmsQueue, msgOut);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);
        Object msgIn = jmsConsumer.receiveBody(Serializable.class);

        boolean failedTest = false;
        if ( (msgIn == null) || !msgIn.equals(msgOutText) ) {
            failedTest = true;
        }

        jmsContext.close();

        if ( failedTest ) {
            throw new Exception("testReceiveBodyObjectMsg_TcpIp_SecOff failed: Expected message was not received");
        }
    }

    public void testReceiveBodyTimeOutTextMsg_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContext = jmsQCFBindings.createContext();
        emptyQueue(jmsQCFBindings, jmsQueue);

        String msgOutText = "testReceiveBodyTimeOutTextMsg_B_SecOff";
        TextMessage msgOut = jmsContext.createTextMessage(msgOutText);
        jmsContext.createProducer().send(jmsQueue, msgOut);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);
        String msgIn = jmsConsumer.receiveBody(String.class, 30000);

        jmsContext.close();

        boolean failedTest = false;
        if ( (msgIn == null) || !msgIn.equals(msgOutText) ) {
            failedTest = true;
        }

        if ( failedTest ) {
            throw new Exception("testReceiveBodyTimeOutTextMsg_B_SecOff failed: Expected message was not received");
        }
    }

    public void testReceiveBodyTimeOutTextMsg_TcpIp_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContext = jmsQCFTCP.createContext();
        emptyQueue(jmsQCFTCP, jmsQueue);

        String msgOutText = "testReceiveBodyTimeOutTextMsg_TcpIp_SecOff";
        TextMessage msgOut = jmsContext.createTextMessage(msgOutText);
        jmsContext.createProducer().send(jmsQueue, msgOut);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);
        String msgIn = jmsConsumer.receiveBody(String.class, 30000);

        boolean failedTest = false;
        if ( (msgIn == null) || !msgIn.equals(msgOutText) ) {
            failedTest = true;
        }

        jmsContext.close();

        if ( failedTest ) {
            throw new Exception("testReceiveBodyTimeOutTextMsg_TcpIp_SecOff failed: Expected message was not received");
        }
    }

    public void testReceiveBodyTimeOutByteMsg_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContext = jmsQCFBindings.createContext();
        emptyQueue(jmsQCFBindings, jmsQueue);

        byte[] msgOutBytes = new byte[] { 125, 2 };
        BytesMessage msgOut = jmsContext.createBytesMessage();
        msgOut.writeBytes(msgOutBytes);
        jmsContext.createProducer().send(jmsQueue, msgOut);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);
        byte[] msgIn = jmsConsumer.receiveBody(byte[].class, 30000);

        boolean failedTest = false;
        if ( (msgIn == null) || !Arrays.equals(msgIn, msgOutBytes) ) {
            failedTest = true;
        }

        jmsContext.close();

        if ( failedTest ) {
            throw new Exception("testReceiveBodyTimeOutByteMsg_B_SecOff failed: Expected message was not received");
        }
    }

    public void testReceiveBodyTimeOutByteMsg_TcpIp_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContext = jmsQCFTCP.createContext();
        emptyQueue(jmsQCFTCP, jmsQueue);

        byte[] msgOutBytes = new byte[] { 124, 3 };
        BytesMessage msgOut = jmsContext.createBytesMessage();
        msgOut.writeBytes(msgOutBytes);
        jmsContext.createProducer().send(jmsQueue, msgOut);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);
        byte[] msgIn = jmsConsumer.receiveBody(byte[].class, 30000);

        boolean failedTest = false;
        if ( (msgIn == null) || !Arrays.equals(msgIn, msgOutBytes) ) {
            failedTest = true;
        }

        jmsContext.close();

        if ( failedTest ) {
            throw new Exception("testReceiveBodyTimeOutByteMsg_TcpIp_SecOff failed: Expected message was not received");
        }
    }

    public void testReceiveBodyTimeOutMapMsg_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContext = jmsQCFBindings.createContext();
        emptyQueue(jmsQCFBindings, jmsQueue);

        MapMessage msgOut = jmsContext.createMapMessage();
        String nameOut = "testReceiveBodyTimeOutMapMsg_B_SecOff";
        msgOut.setString("Name", nameOut);
        String secOut = "off";
        msgOut.setString("Security", secOut);
        jmsContext.createProducer().send(jmsQueue, msgOut);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);
        Map msgIn = jmsConsumer.receiveBody(Map.class, 30000);

        boolean failedTest = false;
        if ( msgIn == null ) {
            failedTest = true;
        } else {
            Object nameIn = msgIn.get("Name");
            if ( (nameIn == null) || !nameIn.equals(nameOut) ) {
                failedTest = true;
            }
            Object secIn = msgIn.get("Security");
            if ( (secIn == null) || !secIn.equals(secOut) ) {
                failedTest = true;
            }
        }

        jmsContext.close();

        if ( failedTest ) {
            throw new Exception("testReceiveBodyTimeOutMapMsg_B_SecOff failed: Expected message was not received");
        }
    }

    public void testReceiveBodyTimeOutMapMsg_TcpIp_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContext = jmsQCFTCP.createContext();
        emptyQueue(jmsQCFTCP, jmsQueue);

        MapMessage msgOut = jmsContext.createMapMessage();
        String nameOut = "testReceiveBodyTimeOutMapMsg_TcpIp_SecOff";
        msgOut.setString("Name", nameOut);
        String secOut = "off";
        msgOut.setString("Security", secOut);
        jmsContext.createProducer().send(jmsQueue, msgOut);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);
        Map msgIn = jmsConsumer.receiveBody(Map.class, 30000);

        boolean failedTest = false;
        if ( msgIn == null ) {
            failedTest = true;
        } else {
            Object nameIn = msgIn.get("Name");
            if ( (nameIn == null) || !nameIn.equals(nameOut) ) {
                failedTest = true;
            }
            Object secIn = msgIn.get("Security");
            if ( (secIn == null) || !secIn.equals(secOut) ) {
                failedTest = true;
            }
        }

        jmsContext.close();

        if ( failedTest ) {
            throw new Exception("testReceiveBodyTimeOutMapMsg_TcpIp_SecOff failed: Expected message was not received");
        }
    }

    public void testReceiveBodyTimeOutObjectMsg_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContext = jmsQCFBindings.createContext();
        emptyQueue(jmsQCFBindings, jmsQueue);

        Object msgOutText = new String("testReceiveBodyTimeOutObjectMsg_B_SecOff");
        ObjectMessage msgOut = jmsContext.createObjectMessage();
        msgOut.setObject((Serializable) msgOutText);
        jmsContext.createProducer().send(jmsQueue, msgOut);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);
        Object msgIn = jmsConsumer.receiveBody(Serializable.class, 30000);

        boolean failedTest = false;
        if ( (msgIn == null) || !msgIn.equals(msgOutText) ) {
            failedTest = true;
        }

        jmsContext.close();

        if ( failedTest ) {
            throw new Exception("testReceiveBodyTimeOutObjectMsg_B_SecOff failed: Expected message was not received");
        }
    }

    public void testReceiveBodyTimeOutObjectMsg_TcpIp_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContext = jmsQCFTCP.createContext();
        emptyQueue(jmsQCFBindings, jmsQueue);

        Object msgOutText = new String("testReceiveBodyTimeOutObjectMsg_TcpIp_SecOff");
        ObjectMessage msgOut = jmsContext.createObjectMessage();
        msgOut.setObject((Serializable) msgOutText);
        jmsContext.createProducer().send(jmsQueue, msgOut);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);
        Object msgIn = jmsConsumer.receiveBody(Serializable.class, 30000);

        boolean failedTest = false;
        if ( (msgIn == null) || !msgIn.equals(msgOutText) ) {
            failedTest = true;
        }

        jmsContext.close();

        if ( failedTest ) {
            throw new Exception("testReceiveBodyTimeOutObjectMsg_TcpIp_SecOff failed: Expected message was not received");
        }
    }

    public void testReceiveBodyNoWaitTextMsg_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContext = jmsQCFBindings.createContext();
        emptyQueue(jmsQCFBindings, jmsQueue);

        String msgOutText = "testReceiveBodyNoWaitTextMsg_B_SecOff";
        TextMessage msgOut = jmsContext.createTextMessage(msgOutText);
        jmsContext.createProducer().send(jmsQueue, msgOut);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);
        String msgIn = jmsConsumer.receiveBodyNoWait(String.class);

        boolean failedTest = false;
        if ( (msgIn == null) || !msgIn.equals(msgOutText) ) {
            failedTest = true;
        }

        jmsContext.close();

        if ( failedTest ) {
            throw new Exception("testReceiveBodyNoWaitTextMsg_B_SecOff failed: Expected message was not received");
        }
    }

    public void testReceiveBodyNoWaitTextMsg_TcpIp_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContext = jmsQCFTCP.createContext();
        emptyQueue(jmsQCFTCP, jmsQueue);

        String msgOutText = "testReceiveBodyNoWaitTextMsg_TcpIp_SecOff";
        TextMessage msgOut = jmsContext.createTextMessage(msgOutText);
        jmsContext.createProducer().send(jmsQueue, msgOut);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);
        String msgIn = jmsConsumer.receiveBodyNoWait(String.class);

        boolean failedTest = false;
        if ( (msgIn == null) || !msgIn.equals(msgOutText) ) {
            failedTest = true;
        }

        jmsContext.close();

        if ( failedTest ) {
            throw new Exception("testReceiveBodyNoWaitTextMsg_TcpIp_SecOff failed: Expected message was not received");
        }
    }

    public void testReceiveBodyNoWaitByteMsg_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContext = jmsQCFBindings.createContext();
        emptyQueue(jmsQCFBindings, jmsQueue);

        byte[] msgOutBytes = new byte[] { 123, 4 };
        BytesMessage msgOut = jmsContext.createBytesMessage();
        msgOut.writeBytes(msgOutBytes);
        jmsContext.createProducer().send(jmsQueue, msgOut);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);
        byte[] msgIn = jmsConsumer.receiveBodyNoWait(byte[].class);

        boolean failedTest = false;
        if ( (msgIn == null) || !Arrays.equals(msgIn, msgOutBytes) ) {
            failedTest = true;
        }

        jmsContext.close();

        if ( failedTest ) {
            throw new Exception("testReceiveBodyNoWaitByteMsg_B_SecOff failed: Expected message was not received");
        }
    }

    public void testReceiveBodyNoWaitByteMsg_TcpIp_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContext = jmsQCFTCP.createContext();
        emptyQueue(jmsQCFTCP, jmsQueue);

        byte[] msgOutBytes = new byte[] { 122, 5 };
        BytesMessage msgOut = jmsContext.createBytesMessage();
        msgOut.writeBytes(msgOutBytes);
        jmsContext.createProducer().send(jmsQueue, msgOut);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);
        byte[] msgIn = jmsConsumer.receiveBodyNoWait(byte[].class);

        boolean failedTest = false;
        if ( (msgIn == null) || !Arrays.equals(msgIn, msgOutBytes) ) {
            failedTest = true;
        }

        jmsContext.close();

        if ( failedTest ) {
            throw new Exception("testReceiveBodyNoWaitByteMsg_TcpIp_SecOff failed: Expected message was not received");
        }
    }

    public void testReceiveBodyNoWaitMapMsg_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContext = jmsQCFBindings.createContext();
        emptyQueue(jmsQCFBindings, jmsQueue);

        MapMessage msgOut = jmsContext.createMapMessage();
        String nameOut = "testReceiveBodyNoWaitMapMsg_B_SecOff";
        msgOut.setString("Name", nameOut);
        String secOut = "off";
        msgOut.setString("Security", secOut);
        jmsContext.createProducer().send(jmsQueue, msgOut);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);
        Map msgIn = jmsConsumer.receiveBody(Map.class, 30000);

        boolean failedTest = false;
        if ( msgIn == null ) {
            failedTest = true;
        } else {
            Object nameIn = msgIn.get("Name");
            if ( (nameIn == null) || !nameIn.equals(nameOut) ) {
                failedTest = true;
            }
            Object secIn = msgIn.get("Security");
            if ( (secIn == null) || !secIn.equals(secOut) ) {
                failedTest = true;
            }
        }

        jmsContext.close();

        if ( failedTest ) {
            throw new Exception("testReceiveBodyNoWaitMapMsg_B_SecOff failed: Expected message was not received");
        }
    }

    public void testReceiveBodyNoWaitMapMsg_TcpIp_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContext = jmsQCFTCP.createContext();
        emptyQueue(jmsQCFTCP, jmsQueue);

        MapMessage msgOut = jmsContext.createMapMessage();
        msgOut.setString("Name", "testReceiveBodyNoWaitMapMsg_TcpIp_SecOff");
        msgOut.setString("Security", "off");
        jmsContext.createProducer().send(jmsQueue, msgOut);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);
        Map msgIn = jmsConsumer.receiveBody(Map.class, 30000);

        boolean failedTest = false;
        if ( (msgIn == null) ||
             !msgIn.get("Name").equals("testReceiveBodyNoWaitMapMsg_TcpIp_SecOff") ||
             !msgIn.get("Security").equals("off") ) {
            failedTest = true;
        }

        jmsContext.close();

        if ( failedTest ) {
            throw new Exception("testReceiveBodyNoWaitMapMsg_TcpIp_SecOff failed: Expected message was not received");
        }
    }

    public void testReceiveBodyNoWaitObjectMsg_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContext = jmsQCFBindings.createContext();
        emptyQueue(jmsQCFBindings, jmsQueue);

        Object msgOutText = new String("testReceiveBodyNoWaitObjectMsg_B_SecOff");
        ObjectMessage msgOut = jmsContext.createObjectMessage();
        msgOut.setObject((Serializable) msgOutText);
        jmsContext.createProducer().send(jmsQueue, msgOut);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);
        Object msgIn = jmsConsumer.receiveBodyNoWait(Serializable.class);

        boolean failedTest = false;
        if ( (msgIn == null) || !msgIn.equals(msgOutText) ) {
            failedTest = true;
        }

        jmsContext.close();

        if ( failedTest ) {
            throw new Exception("testReceiveBodyNoWaitObjectMsg_B_SecOff failed: Expected message was not received");
        }
    }

    public void testReceiveBodyNoWaitObjectMsg_TcpIp_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContext = jmsQCFTCP.createContext();
        emptyQueue(jmsQCFTCP, jmsQueue);

        Object msgOutText = new String("testReceiveBodyNoWaitObjectMsg_TcpIp_SecOff");
        ObjectMessage msgOut = jmsContext.createObjectMessage();
        msgOut.setObject((Serializable) msgOutText);
        jmsContext.createProducer().send(jmsQueue, msgOut);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);
        Object msgIn = jmsConsumer.receiveBodyNoWait(Serializable.class);

        boolean failedTest = false;
        if ( (msgIn == null) || !msgIn.equals(msgOutText) ) {
            failedTest = true;
        }

        jmsContext.close();

        if ( failedTest ) {
            throw new Exception("testReceiveBodyNoWaitObjectMsg_TcpIp_SecOff failed: Expected message was not received");
        }
    }

    public void testReceiveNoWaitFromEmptyQueue_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContext = jmsQCFBindings.createContext();
        emptyQueue(jmsQCFBindings, jmsQueue);
        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);

        int numMsgs = 0;
        QueueBrowser qb = jmsContext.createBrowser(jmsQueue);
        Enumeration e = qb.getEnumeration();
        while ( e.hasMoreElements() ) {
            TextMessage message = (TextMessage) e.nextElement();
            numMsgs++;
        }

        String failure = null;
        if ( numMsgs == 0 ) {
            TextMessage message = (TextMessage) jmsConsumer.receiveNoWait();
            if ( message != null ) {
                failure = "Null message expected; received [ " + message + " ]";
            }
        } else {
            failure = "The queue was not empty";
        }

        jmsContext.close();

        if ( failure != null ) {
            throw new Exception("testReceiveNoWaitFromEmptyQueue_B_SecOff failed: " + failure);
        }
    }

    public void testReceiveNoWaitFromEmptyQueue_TcpIp_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContext = jmsQCFTCP.createContext();
        emptyQueue(jmsQCFTCP, jmsQueue);
        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);

        int numMsgs = 0;
        QueueBrowser qb = jmsContext.createBrowser(jmsQueue);
        Enumeration e = qb.getEnumeration();
        while ( e.hasMoreElements() ) {
            TextMessage message = (TextMessage) e.nextElement();
            numMsgs++;
        }

        String failure = null;
        if ( numMsgs == 0 ) {
            TextMessage message = (TextMessage) jmsConsumer.receiveNoWait();
            if ( message != null ) {
                failure = "Null message expected; received [ " + message + " ]";
            }
        } else {
            failure = "The queue was not empty";
        }

        jmsContext.close();

        if ( failure != null ) {
            throw new Exception("testReceiveNoWaitFromEmptyQueue_TcpIp_SecOff failed: " + failure);
        }
    }

    public void testReceiveBodyEmptyBody_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContext = jmsQCFBindings.createContext();
        emptyQueue(jmsQCFBindings, jmsQueue);

        TextMessage msgOut = jmsContext.createTextMessage();
        jmsContext.createProducer().send(jmsQueue, msgOut);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);

        boolean failedTest = false;
        try {
            String messageBody = jmsConsumer.receiveBody(String.class);
            failedTest = true;
        } catch ( MessageFormatRuntimeException ex ) {
            ex.printStackTrace();
        }

        jmsContext.close();

        if ( failedTest ) {
            throw new Exception("testReceiveBodyEmptyBody_B_SecOff failed: Expected exception was not received");
        }
    }

    public void testReceiveBodyEmptyBody_TcpIp_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContext = jmsQCFTCP.createContext();
        emptyQueue(jmsQCFTCP, jmsQueue);

        TextMessage m = jmsContext.createTextMessage();
        jmsContext.createProducer().send(jmsQueue, m);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);

        boolean failedTest = false;
        try {
            String messageBody = jmsConsumer.receiveBody(String.class);
            failedTest = true;
        } catch ( MessageFormatRuntimeException ex ) {
            ex.printStackTrace();
        }

        jmsContext.close();

        if ( failedTest ) {
            throw new Exception("testReceiveBodyEmptyBody_TcpIp_SecOff failed: Expected exception was not received");
        }
    }

    public void testReceiveBodyUnspecifiedType_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContext = jmsQCFBindings.createContext();
        emptyQueue(jmsQCFBindings, jmsQueue);

        JMSProducer jmsProducer = jmsContext.createProducer();
        TextMessage m = jmsContext.createTextMessage();
        jmsProducer.send(jmsQueue, m);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);

        boolean failedTest = false;
        try {
            byte[] messageBody = jmsConsumer.receiveBody(byte[].class);
            failedTest = true;
        } catch ( MessageFormatRuntimeException ex ) {
            ex.printStackTrace();
        }

        jmsContext.close();

        if ( failedTest ) {
            throw new Exception("testReceiveBodyUnspecifiedType_B_SecOff failed: Expected exception was not received");
        }
    }

    public void testReceiveBodyUnspecifiedType_TcpIp_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContext = jmsQCFTCP.createContext();
        emptyQueue(jmsQCFTCP, jmsQueue);

        JMSProducer jmsProducer = jmsContext.createProducer();
        TextMessage m = jmsContext.createTextMessage();
        jmsProducer.send(jmsQueue, m);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);

        boolean failedTest = false;
        try {
            byte[] messageBody = jmsConsumer.receiveBody(byte[].class);
            failedTest = true;
        } catch ( MessageFormatRuntimeException ex ) {
            ex.printStackTrace();
        }

        jmsContext.close();

        if ( failedTest ) {
            throw new Exception("testReceiveBodyUnspecifiedType_TcpIp_SecOff failed: Expected exception was not received");
        }
    }

    public void testReceiveBodyUnsupportedType_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContext = jmsQCFBindings.createContext();
        emptyQueue(jmsQCFBindings, jmsQueue);

        JMSProducer jmsProducer = jmsContext.createProducer();
        MapMessage m = jmsContext.createMapMessage();
        jmsProducer.send(jmsQueue, m);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);

        boolean failedTest = false;
        try {
            String messageBody = jmsConsumer.receiveBody(String.class);
            failedTest = true;
        } catch ( MessageFormatRuntimeException ex ) {
            ex.printStackTrace();
        }

        jmsContext.close();

        if ( failedTest ) {
            throw new Exception("testReceiveBodyUnsupportedType_B_SecOff failed: Expected exception was not received");
        }
    }

    public void testReceiveBodyUnsupportedType_TcpIp_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContext = jmsQCFTCP.createContext();
        emptyQueue(jmsQCFTCP, jmsQueue);

        JMSProducer jmsProducer = jmsContext.createProducer();
        MapMessage m = jmsContext.createMapMessage();
        jmsProducer.send(jmsQueue, m);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);

        boolean failedTest = false;
        try {
            String messageBody = jmsConsumer.receiveBody(String.class);
            failedTest = true;
        } catch ( MessageFormatRuntimeException ex ) {
            ex.printStackTrace();
        }

        jmsContext.close();

        if ( failedTest ) {
            throw new Exception("testReceiveBodyUnsupportedType_TcpIp_SecOff failed: Expected exception was not received");
        }
    }

    public void testReceiveBodyWithTimeOutEmptyBody_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContext = jmsQCFBindings.createContext();
        emptyQueue(jmsQCFBindings, jmsQueue);

        TextMessage m = jmsContext.createTextMessage();
        jmsContext.createProducer().send(jmsQueue, m);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);

        boolean failedTest = false;
        try {
            String messageBody = jmsConsumer.receiveBody(String.class, 100);
            failedTest = true;
        } catch ( MessageFormatRuntimeException ex ) {
            ex.printStackTrace();
        }

        jmsContext.close();

        if ( failedTest ) {
            throw new Exception("testReceiveBodyWithTimeOutEmptyBody_B_SecOff failed: Expected exception was not received");
        }
    }

    public void testReceiveBodyWithTimeOutEmptyBody_TcpIp_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContext = jmsQCFTCP.createContext();
        emptyQueue(jmsQCFTCP, jmsQueue);

        TextMessage m = jmsContext.createTextMessage();
        jmsContext.createProducer().send(jmsQueue, m);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);

        boolean failedTest = false;
        try {
            String messageBody = jmsConsumer.receiveBody(String.class, 100);
            failedTest = true;
        } catch ( MessageFormatRuntimeException ex ) {
            ex.printStackTrace();
        }

        jmsContext.close();

        if ( failedTest ) {
            throw new Exception("testReceiveBodyWithTimeOutEmptyBody_TcpIp_SecOff failed: Expected exception was not received");
        }
    }

    public void testReceiveBodyWithTimeOutUnspecifiedType_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContext = jmsQCFBindings.createContext();
        emptyQueue(jmsQCFBindings, jmsQueue);

        JMSProducer jmsProducer = jmsContext.createProducer();
        TextMessage m = jmsContext.createTextMessage();
        jmsProducer.send(jmsQueue, m);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);

        boolean failedTest = false;
        try {
            byte[] messageBody = jmsConsumer.receiveBody(byte[].class, 100);
            failedTest = true;
        } catch ( MessageFormatRuntimeException ex ) {
            ex.printStackTrace();
        }

        jmsContext.close();

        if ( failedTest ) {
            throw new Exception("testReceiveBodyWithTimeOutUnspecifiedType_B_SecOff failed: Expected exception was not received");
        }
    }

    public void testReceiveBodyWithTimeOutUnspecifiedType_TcpIp_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContext = jmsQCFTCP.createContext();
        emptyQueue(jmsQCFTCP, jmsQueue);

        JMSProducer jmsProducer = jmsContext.createProducer();
        TextMessage m = jmsContext.createTextMessage();
        jmsProducer.send(jmsQueue, m);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);

        boolean failedTest = false;
        try {
            byte[] messageBody = jmsConsumer.receiveBody(byte[].class, 100);
            failedTest = true;
        } catch ( MessageFormatRuntimeException ex ) {
            ex.printStackTrace();
        }

        jmsContext.close();

        if ( failedTest ) {
            throw new Exception("testReceiveBodyWithTimeOutUnspecifiedType_TcpIp_SecOff failed: Expected exception was not received");
        }
    }

    public void testReceiveBodyWithTimeOutUnsupportedType_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContext = jmsQCFBindings.createContext();
        emptyQueue(jmsQCFBindings, jmsQueue);

        JMSProducer jmsProducer = jmsContext.createProducer();
        MapMessage m = jmsContext.createMapMessage();
        jmsProducer.send(jmsQueue, m);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);

        boolean failedTest = false;
        try {
            String messageBody = jmsConsumer.receiveBody(String.class, 100);
            failedTest = true;
        } catch ( MessageFormatRuntimeException ex ) {
            ex.printStackTrace();
        }

        jmsContext.close();

        if ( failedTest ) {
            throw new Exception("testReceiveBodyWithTimeOutUnsupportedType_B_SecOff failed: Expected exception was not received");
        }
    }

    public void testReceiveBodyWithTimeOutUnsupportedType_TcpIp_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContext = jmsQCFTCP.createContext();
        emptyQueue(jmsQCFTCP, jmsQueue);

        JMSProducer jmsProducer = jmsContext.createProducer();
        MapMessage m = jmsContext.createMapMessage();
        jmsProducer.send(jmsQueue, m);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);

        boolean failedTest = false;
        try {
            String messageBody = jmsConsumer.receiveBody(String.class, 100);
            failedTest = true;
        } catch ( MessageFormatRuntimeException ex ) {
            ex.printStackTrace();
        }

        jmsContext.close();

        if ( failedTest ) {
            throw new Exception("testReceiveBodyWithTimeOutUnsupportedType_TcpIp_SecOff failed: Expected exception was not received");
        }
    }

    public void testReceiveBodyNoWaitEmptyBody_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContext = jmsQCFBindings.createContext();
        emptyQueue(jmsQCFBindings, jmsQueue);

        TextMessage m = jmsContext.createTextMessage();
        jmsContext.createProducer().send(jmsQueue, m);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);

        boolean failedTest = false;
        try {
            String messageBody = jmsConsumer.receiveBodyNoWait(String.class);
            failedTest = true;
        } catch ( MessageFormatRuntimeException ex ) {
            ex.printStackTrace();
        }

        jmsContext.close();

        if ( failedTest ) {
            throw new Exception("testReceiveBodyNoWaitEmptyBody_B_SecOff failed: Expected exception was not received");
        }
    }

    public void testReceiveBodyNoWaitEmptyBody_TcpIp_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContext = jmsQCFTCP.createContext();
        emptyQueue(jmsQCFTCP, jmsQueue);

        TextMessage m = jmsContext.createTextMessage();
        jmsContext.createProducer().send(jmsQueue, m);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);

        boolean failedTest = false;
        try {
            String messageBody = jmsConsumer.receiveBodyNoWait(String.class);
            failedTest = true;
        } catch ( MessageFormatRuntimeException ex ) {
            ex.printStackTrace();
        }

        jmsContext.close();

        if ( failedTest ) {
            throw new Exception("testReceiveBodyNoWaitEmptyBody_TcpIp_SecOff failed: Expected exception was not received");
        }
    }

    public void testReceiveBodyNoWaitUnspecifiedType_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContext = jmsQCFBindings.createContext();
        emptyQueue(jmsQCFBindings, jmsQueue);

        JMSProducer jmsProducer = jmsContext.createProducer();
        TextMessage m = jmsContext.createTextMessage();
        jmsProducer.send(jmsQueue, m);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);

        boolean failedTest = false;
        try {
            byte[] messageBody = jmsConsumer.receiveBodyNoWait(byte[].class);
            failedTest = true;
        } catch ( MessageFormatRuntimeException ex ) {
            ex.printStackTrace();
        }

        jmsContext.close();

        if ( failedTest ) {
            throw new Exception("testReceiveBodyNoWaitUnspecifiedType_B_SecOff failed: Expected exception was not received");
        }
    }

    public void testReceiveBodyNoWaitUnspecifiedType_TcpIp_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContext = jmsQCFTCP.createContext();
        emptyQueue(jmsQCFTCP, jmsQueue);

        JMSProducer jmsProducer = jmsContext.createProducer();
        TextMessage m = jmsContext.createTextMessage();
        jmsProducer.send(jmsQueue, m);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);

        boolean failedTest = false;
        try {
            byte[] messageBody = jmsConsumer.receiveBodyNoWait(byte[].class);
            failedTest = true;
        } catch ( MessageFormatRuntimeException ex ) {
            ex.printStackTrace();
        }

        jmsContext.close();

        if ( failedTest ) {
            throw new Exception("testReceiveBodyNoWaitUnspecifiedType_TcpIp_SecOff failed: Expected exception was not received");
        }
    }

    public void testReceiveBodyNoWaitUnsupportedType_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext context = jmsQCFBindings.createContext();
        emptyQueue(jmsQCFBindings, jmsQueue);

        JMSProducer producer = context.createProducer();
        MapMessage m = context.createMapMessage();
        producer.send(jmsQueue, m);

        JMSConsumer consumer = context.createConsumer(jmsQueue);

        boolean failedTest = false;
        try {
            String messageBody = consumer.receiveBodyNoWait(String.class);
            failedTest = true;
        } catch ( MessageFormatRuntimeException ex ) {
            ex.printStackTrace();
        }

        context.close();

        if ( failedTest ) {
            throw new Exception("testReceiveBodyNoWaitUnsupportedType_B_SecOff failed: Expected exception was not received");
        }
    }

    public void testReceiveBodyNoWaitUnsupportedType_TcpIp_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext context = jmsQCFTCP.createContext();
        emptyQueue(jmsQCFTCP, jmsQueue);

        JMSProducer producer = context.createProducer();
        MapMessage m = context.createMapMessage();
        producer.send(jmsQueue, m);

        JMSConsumer consumer = context.createConsumer(jmsQueue);

        boolean failedTest = false;
        try {
            String messageBody = consumer.receiveBodyNoWait(String.class);
            failedTest = true;
        } catch ( MessageFormatRuntimeException ex ) {
            ex.printStackTrace();
        }

        context.close();

        if ( failedTest ) {
            throw new Exception("testReceiveBodyNoWaitUnsupportedType_TcpIp_SecOff failed: Expected exception was not received");
        }
    }

    public void testReceiveBodyAfterTimeout_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContext = jmsQCFBindings.createContext();
        emptyQueue(jmsQCFBindings, jmsQueue);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);

        int numMsgs = 0;
        QueueBrowser qb = jmsContext.createBrowser(jmsQueue);
        Enumeration e = qb.getEnumeration();
        while ( e.hasMoreElements() ) {
            TextMessage message = (TextMessage) e.nextElement();
            numMsgs++;
        }

        String failure = null;
        if ( numMsgs == 0 ) {
            String message = jmsConsumer.receiveBody(String.class, 100);
            if ( message != null ) {
                failure = "Null message expected; received [ " + message + " ]";
            }
        } else {
            failure = "The queue was not empty";
        }

        jmsContext.close();

        if ( failure != null ) {
            throw new Exception("testReceiveBodyAfterTimeout_B_SecOff failed: " + failure);
        }
    }

    public void testReceiveBodyAfterTimeout_TcpIp_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContext = jmsQCFTCP.createContext();
        emptyQueue(jmsQCFTCP, jmsQueue);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);

        int numMsgs = 0;
        QueueBrowser qb = jmsContext.createBrowser(jmsQueue);
        Enumeration e = qb.getEnumeration();
        while ( e.hasMoreElements() ) {
            TextMessage message = (TextMessage) e.nextElement();
            numMsgs++;
        }

        String failure = null;
        if ( numMsgs == 0 ) {
            String message = jmsConsumer.receiveBody(String.class, 100);
            if ( message != null ) {
                failure = "Null message expected; received [ " + message + " ]";
            }
        } else {
            failure = "The queue was not empty";
        }

        jmsContext.close();

        if ( failure != null ) {
            throw new Exception("testReceiveBodyAfterTimeout_TcpIp_SecOff failed: " + failure);
        }
    }

    public void testReceiveBodyNoWaitFromEmptyQueue_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContext = jmsQCFBindings.createContext();
        emptyQueue(jmsQCFBindings, jmsQueue);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);

        int numMsgs = 0;
        QueueBrowser qb = jmsContext.createBrowser(jmsQueue);
        Enumeration e = qb.getEnumeration();
        while ( e.hasMoreElements() ) {
            TextMessage message = (TextMessage) e.nextElement();
            numMsgs++;
        }

        String failure = null;
        if ( numMsgs == 0 ) {
            String message = jmsConsumer.receiveBodyNoWait(String.class);
            if ( message != null ) {
                failure = "Null message expected; received [ " + message + " ]";
            }
        } else {
            failure = "The queue was not empty";
        }

        jmsContext.close();

        if ( failure != null ) {
            throw new Exception("testReceiveBodyNoWaitFromEmptyQueue_B_SecOff failed: " + failure);
        }
    }

    public void testReceiveBodyNoWaitFromEmptyQueue_TcpIp_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContext = jmsQCFTCP.createContext();
        emptyQueue(jmsQCFTCP, jmsQueue);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);

        int numMsgs = 0;
        QueueBrowser qb = jmsContext.createBrowser(jmsQueue);
        Enumeration e = qb.getEnumeration();
        while ( e.hasMoreElements() ) {
            TextMessage message = (TextMessage) e.nextElement();
            numMsgs++;
        }

        String failure = null;
        if ( numMsgs == 0 ) {
            String message = jmsConsumer.receiveBodyNoWait(String.class);
            if ( message != null ) {
                failure = "Null message expected; received [ " + message + " ]";
            }
        } else {
            failure = "The queue was not empty";
        }

        jmsContext.close();

        if ( failure != null ) {
            throw new Exception("testReceiveBodyNoWaitFromEmptyQueue_TcpIp_SecOff failed: " + failure);
        }
    }

    public void testReceiveWithTimeOut_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContext = jmsQCFBindings.createContext();
        emptyQueue(jmsQCFBindings, jmsQueue);
        JMSProducer jmsProducer = jmsContext.createProducer();
        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);

        String msgOutText = "testReceiveWithTimeOut_B_SecOff";
        TextMessage msgOut = jmsContext.createTextMessage(msgOutText);
        jmsProducer.send(jmsQueue, msgOut);

        TextMessage msgIn = (TextMessage) jmsConsumer.receive(100);

        boolean failedTest = false;
        if ( (msgIn == null) ||
             (msgIn.getText() == null) || !msgIn.getText().equals(msgOutText) ) {
            failedTest = true;
        }

        jmsContext.close();

        if ( failedTest ) {
            throw new Exception("testReceiveWithTimeOut_B_SecOff failed: Expected message was not received");
        }
    }

    public void testReceiveWithTimeOut_TcpIp_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContext = jmsQCFTCP.createContext();
        emptyQueue(jmsQCFTCP, jmsQueue);
        JMSProducer jmsProducer = jmsContext.createProducer();
        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);

        String msgOutText = "testReceiveWithTimeOut_B_SecOff";
        TextMessage msgOut = jmsContext.createTextMessage(msgOutText);
        jmsProducer.send(jmsQueue, msgOut);

        TextMessage msgIn = (TextMessage) jmsConsumer.receive(100);

        boolean failedTest = false;
        if ( (msgIn == null) ||
             (msgIn.getText() == null) || !msgIn.getText().equals(msgOutText) ) {
            failedTest = true;
        }

        jmsContext.close();

        if ( failedTest ) {
            throw new Exception("testReceiveWithTimeOut_B_SecOff failed: Expected message was not received");
        }
    }

    public void testReceiveNoWait_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContext = jmsQCFBindings.createContext();
        emptyQueue(jmsQCFBindings, jmsQueue);
        JMSProducer jmsProducer = jmsContext.createProducer();
        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);

        String msgOutText = "testReceiveNoWait_B_SecOff";
        TextMessage msgOut = jmsContext.createTextMessage(msgOutText);
        jmsProducer.send(jmsQueue, msgOut);

        TextMessage msgIn = (TextMessage) jmsConsumer.receiveNoWait();

        boolean failedTest = false;
        if ( (msgIn == null) ||
             (msgIn.getText() == null) || !msgIn.getText().equals(msgOutText) ) {
            failedTest = true;
        }

        jmsContext.close();

        if ( failedTest ) {
            throw new Exception("testReceiveNoWait_B_SecOff failed: Expected message was not received");
        }
    }

    public void testReceiveNoWait_TcpIp_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContext = jmsQCFTCP.createContext();
        emptyQueue(jmsQCFTCP, jmsQueue);
        JMSProducer jmsProducer = jmsContext.createProducer();
        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);

        String msgOutText = "testReceiveNoWait_TcpIp_SecOff";
        TextMessage msgOut = jmsContext.createTextMessage(msgOutText);
        jmsProducer.send(jmsQueue, msgOut);

        TextMessage msgIn = (TextMessage) jmsConsumer.receiveNoWait();

        boolean failedTest = false;
        if ( (msgIn == null) ||
             (msgIn.getText() == null) || !msgIn.getText().equals(msgOutText) ) {
            failedTest = true;
        }

        jmsContext.close();

        if ( failedTest ) {
            throw new Exception("testReceiveNoWait_TcpIp_SecOff failed: Expected message was not received");
        }
    }

    public void testReceiveBodyNoWait_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContext = jmsQCFBindings.createContext();
        emptyQueue(jmsQCFBindings, jmsQueue);
        JMSProducer jmsProducer = jmsContext.createProducer();
        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);

        String message = jmsConsumer.receiveBodyNoWait(String.class);

        boolean failedTest = false;
        if ( message != null ) {
            failedTest = true;
        }

        jmsContext.close();

        if ( failedTest ) {
            throw new Exception("testReceiveBodyNoWait_B_SecOff failed: Expected exception was not received");
        }
    }

    public void testReceiveBodyNoWait_TcpIp_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContext = jmsQCFTCP.createContext();
        emptyQueue(jmsQCFTCP, jmsQueue);
        JMSProducer jmsProducer = jmsContext.createProducer();
        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);

        String message = jmsConsumer.receiveBodyNoWait(String.class);
        boolean failedTest = false;
        if ( message != null ) {
            failedTest = true;
        }

        jmsContext.close();

        if ( failedTest ) {
            throw new Exception("testReceiveBodyNoWait_TcpIp_SecOff failed: Expected exception was not received");
        }
    }

    public void testReceiveBodyTransactionSecOff_B(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        boolean failedTest = false;

        try {
            UserTransaction ut = (UserTransaction)
                new InitialContext().lookup("java:comp/UserTransaction");
            ut.begin();

            JMSContext jmsContext = jmsQCFBindings.createContext();
            emptyQueue(jmsQCFBindings, jmsQueue);
            JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);
            JMSProducer jmsProducer = jmsContext.createProducer();

            jmsProducer.send(jmsQueue, "testReceiveBodyTransactionSecOff_B");

            ut.commit();
            ut.begin();

            jmsConsumer.receiveBody(String.class);

            jmsConsumer.close();
            jmsContext.close();

        } catch ( Exception ex ) {
            ex.printStackTrace();
            failedTest = true;
        }

        if (failedTest == true) {
            throw new Exception("testReceiveBodyTransactionSecOff_B failed");
        }
    }

    public void testReceiveBodyTransactionSecOff_TCPIP(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        boolean failedTest = false;

        try {
            UserTransaction ut = (UserTransaction)
                new InitialContext().lookup("java:comp/UserTransaction");

            ut.begin();

            JMSContext jmsContext = jmsQCFTCP.createContext();
            emptyQueue(jmsQCFTCP, jmsQueue);
            JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);
            JMSProducer jmsProducer = jmsContext.createProducer();

            jmsProducer.send(jmsQueue, "testReceiveBodyTransactionSecOff_TCP");
            ut.commit();

            ut.begin();
            jmsConsumer.receiveBody(String.class);
            ut.commit();

            jmsConsumer.close();
            jmsContext.close();

        } catch ( Exception ex ) {
            ex.printStackTrace();
            failedTest = true;
        }
        if ( failedTest ) {
            throw new Exception("testReceiveBodyTransactionSecOff_TCPIP failed");
        }
    }

    public void testReceiveBodyTimeOutTransactionSecOff_B(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        boolean failedTest = false;

        try {
            UserTransaction ut = (UserTransaction)
                new InitialContext().lookup("java:comp/UserTransaction");

            ut.begin();

            JMSContext jmsContext = jmsQCFBindings.createContext();
            emptyQueue(jmsQCFBindings, jmsQueue);
            JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);
            JMSProducer jmsProducer = jmsContext.createProducer();

            jmsProducer.send(jmsQueue, "testReceiveBodyTransactionTimeOutSecOff_B");
            ut.commit();

            ut.begin();
            jmsConsumer.receiveBody(String.class, 100);
            ut.commit();

            jmsConsumer.close();
            jmsContext.close();

        } catch ( Exception ex ) {
            ex.printStackTrace();
            failedTest = true;
        }

        if ( failedTest ) {
            throw new Exception("testReceiveBodyTimeOutTransactionSecOff_B failed");
        }
    }

    public void testReceiveBodyTimeOutTransactionSecOff_TCPIP(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        boolean failedTest = false;

        try {
            UserTransaction ut = (UserTransaction)
                new InitialContext().lookup("java:comp/UserTransaction");

            ut.begin();

            JMSContext jmsContext = jmsQCFTCP.createContext();
            emptyQueue(jmsQCFTCP, jmsQueue);
            JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);
            JMSProducer jmsProducer = jmsContext.createProducer();

            jmsProducer.send(jmsQueue, "testReceiveBodyTransactionSecOff_TCP");
            ut.commit();

            ut.begin();
            jmsConsumer.receiveBody(String.class, 100);
            ut.commit();

            jmsConsumer.close();
            jmsContext.close();

        } catch ( Exception ex ) {
            ex.printStackTrace();
            failedTest = true;
        }

        if ( failedTest ) {
            throw new Exception("testReceiveBodyTimeOutTransactionSecOff_TCPIP failed");
        }
    }

    public void testReceiveBodyNoWaitTransactionSecOff_B(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        boolean failedTest = false;

        try {
            UserTransaction ut = (UserTransaction)
                new InitialContext().lookup("java:comp/UserTransaction");

            ut.begin();

            JMSContext jmsContext = jmsQCFBindings.createContext();
            emptyQueue(jmsQCFBindings, jmsQueue);
            JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);
            JMSProducer jmsProducer = jmsContext.createProducer();
            jmsProducer.send(jmsQueue, "testReceiveBodyNoWaitTransactionSecOff_B");

            ut.commit();

            ut.begin();
            jmsConsumer.receiveBodyNoWait(String.class);
            ut.commit();

            jmsConsumer.close();
            jmsContext.close();

        } catch ( Exception ex ) {
            ex.printStackTrace();
            failedTest = true;
        }

        if ( failedTest ) {
            throw new Exception("testReceiveBodyNoWaitTransactionSecOff_B failed");
        }
    }

    public void testReceiveBodyNoWaitTransactionSecOff_TCPIP(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        boolean failedTest = false;

        try {
            UserTransaction ut = (UserTransaction)
                new InitialContext().lookup("java:comp/UserTransaction");

            ut.begin();

            JMSContext jmsContext = jmsQCFTCP.createContext();
            emptyQueue(jmsQCFTCP, jmsQueue);
            JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);
            JMSProducer jmsProducer = jmsContext.createProducer();

            jmsProducer.send(jmsQueue, "testReceiveBodyTransactionSecOff_TCP");
            ut.commit();

            ut.begin();
            jmsConsumer.receiveBodyNoWait(String.class);
            ut.commit();

            jmsConsumer.close();
            jmsContext.close();

        } catch ( Exception ex ) {
            ex.printStackTrace();
            failedTest = true;
        }

        if ( failedTest ) {
            throw new Exception("testReceiveBodyNoWaitTransactionSecOff_TCPIP failed");
        }
    }

    public void testMapMessageMap(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        boolean failedTest = false;

        try {
            JMSContext jmsContext = jmsQCFBindings.createContext();
            emptyQueue(jmsQCFBindings, jmsQueue);

            MapMessage mapMsg = jmsContext.createMapMessage();
            mapMsg.setString("First", "John");
            mapMsg.setString("Middle", "Paul");
            mapMsg.setString("Last", "Richard");
            mapMsg.getBody(Map.class);

        } catch ( MessageFormatException ex ) {
            ex.printStackTrace();
            failedTest = true;
        }

        if ( failedTest ) {
            throw new Exception("testMapMessageMap failed: Unexpected exception seen");
        }
    }

    public void testMapMessageObject(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        boolean failedTest = false;

        try {
            JMSContext jmsContext = jmsQCFBindings.createContext();
            emptyQueue(jmsQCFBindings, jmsQueue);

            MapMessage mapMsg = jmsContext.createMapMessage();
            mapMsg.setString("First", "John");
            mapMsg.setString("Middle", "Paul");
            mapMsg.setString("Last", "Richard");
            mapMsg.getBody(Object.class);

        } catch ( MessageFormatException ex ) {
            ex.printStackTrace();
            failedTest = true;
        }

        if ( failedTest ) {
            throw new Exception("testMapMessageObject failed: Unexpected exception seen");
        }
    }

    public void testMapMessageHashMap(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        boolean failedTest = false;

        try {
            JMSContext jmsContext = jmsQCFBindings.createContext();
            emptyQueue(jmsQCFBindings, jmsQueue);

            MapMessage mapMsg = jmsContext.createMapMessage();
            mapMsg.setString("First", "John");
            mapMsg.setString("Middle", "Paul");
            mapMsg.setString("Last", "Richard");
            mapMsg.getBody(HashMap.class);

            failedTest = true;

        } catch ( MessageFormatException ex ) {
            ex.printStackTrace();
        }

        if ( failedTest ) {
            throw new Exception("testMapMessageHashMap failed: Expected exception was not seen");
        }
    }

    public void testMapMessageStringBuffer(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        boolean failedTest = false;

        try {
            JMSContext jmsContext = jmsQCFBindings.createContext();
            emptyQueue(jmsQCFBindings, jmsQueue);

            MapMessage mapMsg = jmsContext.createMapMessage();
            mapMsg.setString("First", "John");
            mapMsg.setString("Middle", "Paul");
            mapMsg.setString("Last", "Richard");
            mapMsg.getBody(StringBuffer.class);

            failedTest = true;

        } catch ( MessageFormatException ex ) {
            ex.printStackTrace();
        }

        if ( failedTest ) {
            throw new Exception("testMapMessageStringBuffer failed: Expected exception was not seen");
        }
    }

    public void testMapMessageNullBody(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContext = jmsQCFBindings.createContext();
        emptyQueue(jmsQCFBindings, jmsQueue);

        MapMessage mapMsg = jmsContext.createMapMessage();
        String msgBody = mapMsg.getBody(String.class);

        boolean failedTest = false;
        if ( msgBody != null ) {
            failedTest = true;
        }

        if ( failedTest ) {
            throw new Exception("testMapMessageNullBody failed: Expected object was not null");
        }
    }

    public void testTextMessageNullBody(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContext = jmsQCFBindings.createContext();
        emptyQueue(jmsQCFBindings, jmsQueue);

        TextMessage textMsg = jmsContext.createTextMessage();
        String msgBody = (String) textMsg.getBody(Object.class);

        boolean failedTest = false;
        if ( msgBody != null ) {
            failedTest = true;
        }

        if ( failedTest ) {
            throw new Exception("testTextMessageNullBody failed: Expected object was not null");
        }
    }

    public void testBytesMessageNullBody(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContext = jmsQCFBindings.createContext();
        emptyQueue(jmsQCFBindings, jmsQueue);

        BytesMessage byteMsg = jmsContext.createBytesMessage();
        String msgBody = byteMsg.getBody(String.class);

        boolean failedTest = false;
        if ( msgBody != null ) {
            failedTest = true;
        }

        if ( failedTest ) {
            throw new Exception("testBytesMessageNullBody failed: Expected object was not null");
        }
    }

    public void testObjectMessageNullBody(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContext = jmsQCFBindings.createContext();
        emptyQueue(jmsQCFBindings, jmsQueue);

        ObjectMessage objMsg = jmsContext.createObjectMessage();
        String msgBody = objMsg.getBody(String.class);

        boolean failedTest = false;
        if ( msgBody != null ) {
            failedTest = true;
        }

        if ( failedTest ) {
            throw new Exception("testObjectMessageNullBody failed: Expected object was not null");
        }
    }

    public void testStreamMessage(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        boolean failedTest = false;

        try {
            JMSContext jmsContext = jmsQCFBindings.createContext();
            emptyQueue(jmsQCFBindings, jmsQueue);

            StreamMessage msg = jmsContext.createStreamMessage();
            msg.getBody(null);

            failedTest = true;

        } catch ( MessageFormatException ex ) {
            ex.printStackTrace();
        }

        if ( failedTest ) {
            throw new Exception("testStreamMessage failed: Expected exception was not seen");
        }
    }

    public void testMessageGetBody(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContext = jmsQCFBindings.createContext();
        emptyQueue(jmsQCFBindings, jmsQueue);

        Message msg = jmsContext.createMessage();
        String strBody = msg.getBody(String.class);

        boolean failedTest = false;
        if ( strBody != null ) {
            failedTest = true;
        }

        if ( failedTest ) {
            throw new Exception("testMapMessageNullBody failed: Expected object was not null");
        }
    }

    public void testBytesMessage(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        boolean failedTest = false;

        try {
            JMSContext jmsContext = jmsQCFBindings.createContext();
            emptyQueue(jmsQCFBindings, jmsQueue);

            byte[] data = new byte[] { 1, 2 };
            BytesMessage message = jmsContext.createBytesMessage();
            message.writeBytes(data);

            message.getBody(byte[].class);

        } catch ( MessageFormatException ex ) {
            ex.printStackTrace();
            failedTest = true;
        }

        if ( failedTest ) {
            throw new Exception("testBytesMessage failed: Unexpected exception seen");
        }
    }

    public void testBytesMessageObject(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        boolean failedTest = false;

        try {
            JMSContext jmsContext = jmsQCFBindings.createContext();
            emptyQueue(jmsQCFBindings, jmsQueue);

            byte[] data = new byte[] { 1, 2 };
            BytesMessage message = jmsContext.createBytesMessage();
            message.writeBytes(data);

            message.getBody(Object.class);

        } catch ( MessageFormatException ex ) {
            ex.printStackTrace();
            failedTest = true;
        }

        if ( failedTest ) {
            throw new Exception("testBytesMessage failed: Unexpected exception seen");
        }
    }

    public void testBytesMessageStringBuffer(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContext = jmsQCFBindings.createContext();
        emptyQueue(jmsQCFBindings, jmsQueue);

        boolean failedTest = false;

        BytesMessage msgOut = jmsContext.createBytesMessage();
        msgOut.writeByte((byte) 55);
        msgOut.writeInt(10);
        try {
            msgOut.getBody(StringBuffer.class);
            failedTest = true;
        } catch ( MessageFormatException ex ) {
            ex.printStackTrace();
        }
        jmsContext.createProducer().send(jmsQueue, msgOut);

        BytesMessage msgIn = (BytesMessage) jmsContext
            .createConsumer(jmsQueue)
            .receive(1000);
        try {
            msgIn.getBody(StringBuffer.class);
            failedTest = true;
        } catch ( MessageFormatException ex ) {
            ex.printStackTrace();
        }

        if ( failedTest ) {
            throw new Exception("testBytesMessage failed: Expected exception not seen");
        }
    }

    public void testBytesMessageStringBuffer_TCP(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContext = jmsQCFTCP.createContext();
        emptyQueue(jmsQCFTCP, jmsQueue);

        boolean failedTest = false;

        BytesMessage msgOut = jmsContext.createBytesMessage();
        msgOut.writeByte((byte) 55);
        msgOut.writeInt(10);
        try {
            msgOut.getBody(StringBuffer.class);
            failedTest = true;
        } catch ( MessageFormatException ex ) {
            ex.printStackTrace();
        }

        jmsContext.createProducer().send(jmsQueue, msgOut);
        BytesMessage msgIn = (BytesMessage) jmsContext
            .createConsumer(jmsQueue)
            .receive(1000);
        try {
            msgIn.getBody(StringBuffer.class);
            failedTest = true;
        } catch ( MessageFormatException ex ) {
            ex.printStackTrace();
        }

        if ( failedTest ) {
            throw new Exception("testBytesMessage failed: Expected exception not seen");
        }
    }

    public void testMessagePropertyNameWithJMSPositive(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        boolean failedTest = false;
        try {
            JMSContext jmsContext = jmsQCFBindings.createContext();
            emptyQueue(jmsQCFBindings, jmsQueue);
            TextMessage msg = jmsContext.createTextMessage("testMessagePropertyNameWithJMSPositive");
            msg.setStringProperty("JMSCONTEXT", "context1");
        } catch ( MessageFormatException ex ) {
            ex.printStackTrace();
            failedTest = true;
        }

        if ( failedTest ) {
            throw new Exception("testMessagePropertyNameWithJMSPositive : Unable to set a property whose name starts with JMS");
        }
    }

    public void testMessagePropertyNameWithJMSNegative(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        boolean failedTest = false;
        try {
            JMSContext jmsContext = jmsQCFBindings.createContext();
            emptyQueue(jmsQCFBindings, jmsQueue);
            TextMessage msg = jmsContext.createTextMessage("testMessagePropertyNameWithJMSNegative");
            msg.setStringProperty("JMS_CONTEXT", "context1");
            failedTest = true;
        } catch ( MessageFormatException ex ) {
            ex.printStackTrace();
        }

        if ( failedTest ) {
            throw new Exception("testMessagePropertyNameWithJMSNegative : Able to set a property whose name starts with JMS_");
        }
    }
}
