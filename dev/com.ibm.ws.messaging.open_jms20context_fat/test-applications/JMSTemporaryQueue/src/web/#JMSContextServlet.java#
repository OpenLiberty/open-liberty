/*******************************************************************************
 * Copyright (c) 2013, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package jmstemporaryqueue.web;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.Enumeration;
import java.util.Map;

import javax.jms.BytesMessage;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.JMSProducer;
import javax.jms.JMSRuntimeException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageFormatRuntimeException;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.jms.QueueConnectionFactory;
import javax.jms.Session;
import javax.jms.StreamMessage;
import javax.jms.TemporaryQueue;
import javax.jms.TemporaryTopic;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.jms.TopicConnectionFactory;
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
public class JMSContextServlet extends HttpServlet {

    public static QueueConnectionFactory qcfBindings;
    public static QueueConnectionFactory qcfTCP;

    public static TopicConnectionFactory tcfBindings;
    public static TopicConnectionFactory tcfTCP;

    public static Queue queue;
    public static Topic topic;
    public static Topic topic2;

    public void emptyQueue(QueueConnectionFactory qcf, Queue q) throws Exception {
        JMSContext context = qcf.createContext();

        QueueBrowser qb = context.createBrowser(q);
        Enumeration e = qb.getEnumeration();

        JMSConsumer consumer = context.createConsumer(q);

        int numMsgs = 0;
        while ( e.hasMoreElements() ) {
            Message message = (Message) e.nextElement();
            numMsgs++;
        }

        for ( int i = 0; i < numMsgs; i++ ) {
            Message message = consumer.receive();
        }

        context.close();
    }

    @Override
    public void init() throws ServletException {
        super.init();

        try {
            qcfBindings = (QueueConnectionFactory)
                new InitialContext().lookup("java:comp/env/jndi_JMS_BASE_QCF");
        } catch ( NamingException e ) {
            e.printStackTrace();
        }
        System.out.println("Queue connection factory 'java:comp/env/jndi_JMS_BASE_QCF':\n" + qcfBindings);

        try {
            qcfTCP = (QueueConnectionFactory)
                new InitialContext().lookup("java:comp/env/jndi_JMS_BASE_QCF1");
        } catch ( NamingException e ) {
            e.printStackTrace();
        }
        System.out.println("Queue connection factory 'java:comp/env/jndi_JMS_BASE_QCF1':\n" + qcfTCP);

        try {
            queue = (Queue) new InitialContext().lookup("java:comp/env/jndi_INPUT_Q");
        } catch ( NamingException e ) {
            e.printStackTrace();
        }
        System.out.println("Queue 'java:comp/env/jndi_INPUT_!':\n" + queue);

        try {
            tcfBindings = (TopicConnectionFactory) new InitialContext().lookup("java:comp/env/eis/tcf");
        } catch ( NamingException e ) {
            e.printStackTrace();
        }
        System.out.println("Topic connection factory 'java:comp/env/eis/tcf':\n" + tcfBindings);

        try {
            tcfTCP = (TopicConnectionFactory) new InitialContext().lookup("java:comp/env/eis/tcf1");
        } catch ( NamingException e ) {
            e.printStackTrace();
        }
        System.out.println("Topic connection factory 'java:comp/env/eis/tcf1':\n" + tcfTCP);

        try {
            topic = (Topic) new InitialContext().lookup("eis/topic1");
        } catch ( NamingException e ) {
            e.printStackTrace();
        }
        System.out.println("Topic 'eis/topic1':\n" + topic);

        try {
            topic2 = (Topic) new InitialContext().lookup("eis/topic2");
        } catch ( NamingException e ) {
            e.printStackTrace();
        }
        System.out.println("Topic 'eis/topic2':\n" + topic2);

        if ( qcfBindings == null ) {
            throw new ServletException("Null 'qcfBindings'");
        }
        if ( qcfTCP == null ) {
            throw new ServletException("Null 'qcfTCP'");
        }

        if ( tcfBindings == null ) {
            throw new ServletException("Null 'tcfBindings'");
        }
        if ( tcfTCP == null ) {
            throw new ServletException("Null 'tcfTCP'");
        }

        if ( queue == null ) {
            throw new ServletException("Null 'queue'");
        }
        if ( topic == null ) {
            throw new ServletException("Null 'topic'");
        }
        if ( topic2 == null ) {
            throw new ServletException("Null 'topic2'");
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
        TraceComponent tc = Tr.register(JMSContextServlet.class);

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

    public void testReceiveMessageTopicSecOff_B(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        try {
            JMSContext jmsContextTCFBindings = tcfBindings.createContext();
            JMSConsumer jmsConsumerTCFBindings = jmsContextTCFBindings.createConsumer(topic);
            JMSProducer jmsProducerTCFBindings = jmsContextTCFBindings.createProducer();

            Message msg = jmsContextTCFBindings.createTextMessage("test");
            msg.clearBody();

            jmsProducerTCFBindings.send(topic, "");
            jmsConsumerTCFBindings.receive();

            jmsConsumerTCFBindings.close();
            jmsContextTCFBindings.close();

        } catch ( Exception mfe ) {
            mfe.printStackTrace();
            testFailed = true;
        }

        if ( testFailed ) {
            throw new Exception("testReceiveMessageTopicSecOff_B failed");
        }
    }

    public void testReceiveMessageTopicSecOff_TCP(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        try {
            JMSContext jmsContextTCFTCP = tcfTCP.createContext();
            JMSConsumer jmsConsumerTCFTCP = jmsContextTCFTCP.createConsumer(topic);

            JMSProducer jmsProducerTCFTCP = jmsContextTCFTCP.createProducer();

            Message msg = jmsContextTCFTCP.createTextMessage("test");
            msg.clearBody();

            jmsProducerTCFTCP.send(topic, "");
            jmsConsumerTCFTCP.receive();

            jmsConsumerTCFTCP.close();
            jmsContextTCFTCP.close();

        } catch ( Exception mfe ) {
            mfe.printStackTrace();
            testFailed = true;
        }

        if ( testFailed ) {
            throw new Exception("testReceiveMessageTopicSecOff_TCP failed");
        }
    }

    public void testReceiveMessageTopicTranxSecOff_B(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        try {
            UserTransaction ut = (UserTransaction) new InitialContext().lookup("java:comp/UserTransaction");

            ut.begin();

            JMSContext jmsContextTCFBindings = tcfBindings.createContext();

            JMSConsumer jmsConsumerTCFBindings = jmsContextTCFBindings.createConsumer(topic);

            JMSProducer jmsProducerTCFBindings = jmsContextTCFBindings.createProducer();
            Message msg = jmsContextTCFBindings.createTextMessage("testReceiveMessageTopicTranxSecOff_B");
            jmsProducerTCFBindings.send(topic, msg);

            ut.commit();

            ut.begin();
            jmsConsumerTCFBindings.receive();
            ut.commit();

            jmsConsumerTCFBindings.close();
            jmsContextTCFBindings.close();

        } catch ( Exception mfe ) {
            mfe.printStackTrace();
            testFailed = true;
        }

        if ( testFailed ) {
            throw new Exception("testReceiveMessageTopicTranxSecOff_B failed");
        }
    }

    public void testReceiveMessageTopicTranxSecOff_TCP(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        try {
            UserTransaction ut = (UserTransaction) new InitialContext().lookup("java:comp/UserTransaction");

            ut.begin();

            JMSContext jmsContextTCFTCP = tcfTCP.createContext();
            JMSConsumer jmsConsumerTCFTCP = jmsContextTCFTCP.createConsumer(topic);
            JMSProducer jmsProducerTCFTCP = jmsContextTCFTCP.createProducer();

            Message msg = jmsContextTCFTCP.createTextMessage("testReceiveMessageTopicTranxSecOff_B");
            jmsProducerTCFTCP.send(topic, msg);

            ut.commit();

            ut.begin();
            jmsConsumerTCFTCP.receive();
            ut.commit();

            jmsConsumerTCFTCP.close();
            jmsContextTCFTCP.close();

        } catch ( Exception mfe ) {
            mfe.printStackTrace();
            testFailed = true;
        }

        if ( testFailed ) {
            throw new Exception("testReceiveMessageTopicTranxSecOff_TCP failed");
        }
    }

    public void testReceiveTimeoutMessageTopicSecOff_B(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        try {
            JMSContext jmsContextTCFBindings = tcfBindings.createContext();
            JMSConsumer jmsConsumerTCFBindings = jmsContextTCFBindings.createConsumer(topic);
            JMSProducer jmsProducerTCFBindings = jmsContextTCFBindings.createProducer();

            Message msg = jmsContextTCFBindings.createTextMessage("test");
            msg.clearBody();

            jmsProducerTCFBindings.send(topic, "");
            jmsConsumerTCFBindings.receive(30000);

            jmsConsumerTCFBindings.close();
            jmsContextTCFBindings.close();

        } catch ( Exception mfe ) {
            mfe.printStackTrace();
            testFailed = true;
        }

        if ( testFailed ) {
            throw new Exception("testReceiveTimeoutMessageTopicSecOff_B failed");
        }
    }

    public void testReceiveTimeoutMessageTopicSecOff_TCP(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        try {
            JMSContext jmsContextTCFTCP = tcfTCP.createContext();
            JMSConsumer jmsConsumerTCFTCP = jmsContextTCFTCP.createConsumer(topic);

            JMSProducer jmsProducerTCFTCP = jmsContextTCFTCP.createProducer();

            Message msg = jmsContextTCFTCP.createTextMessage("test");
            msg.clearBody();

            jmsProducerTCFTCP.send(topic, "");
            jmsConsumerTCFTCP.receive(30000);

            jmsConsumerTCFTCP.close();
            jmsContextTCFTCP.close();

        } catch ( Exception mfe ) {
            mfe.printStackTrace();
            testFailed = true;
        }

        if ( testFailed ) {
            throw new Exception("testReceiveTimeoutMessageTopicSecOff_TCP failed");
        }
    }

    public void testReceiveNoWaitMessageTopicSecOff_B(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        try {
            JMSContext jmsContextTCFBindings = tcfBindings.createContext();
            JMSConsumer jmsConsumer = jmsContextTCFBindings.createConsumer(topic);

            JMSProducer p1 = jmsContextTCFBindings.createProducer();

            Message msg = jmsContextTCFBindings.createTextMessage("test");
            p1.send(topic, msg);
            jmsConsumer.receiveNoWait();

            jmsConsumer.close();
            jmsContextTCFBindings.close();

        } catch ( Exception mfe ) {
            mfe.printStackTrace();
            testFailed = true;
        }

        if ( testFailed ) {
            throw new Exception("testReceiveNoWaitMessageTopicSecOff_B failed");
        }
    }

    public void testReceiveNoWaitMessageTopicSecOff_TCP(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;
        try {
            JMSContext jmsContextTCFTCP = tcfTCP.createContext();
            JMSConsumer jmsConsumer = jmsContextTCFTCP.createConsumer(topic);

            JMSProducer p1 = jmsContextTCFTCP.createProducer();

            Message msg = jmsContextTCFTCP.createTextMessage("test");
            p1.send(topic, msg);
            jmsConsumer.receiveNoWait();

            jmsConsumer.close();
            jmsContextTCFTCP.close();

        } catch ( Exception mfe ) {
            mfe.printStackTrace();
            testFailed = true;
        }

        if ( testFailed ) {
            throw new Exception("testReceiveNoWaitMessageTopicSecOff_TCP failed");
        }
    }

    public void testReceiveNoWaitNullMessageTopicSecOff_B(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;
        try {
            JMSContext jmsContextTCFBindings = tcfBindings.createContext();

            JMSConsumer jmsConsumer = jmsContextTCFBindings.createConsumer(topic);
            jmsConsumer.receiveNoWait();

            jmsConsumer.close();
            jmsContextTCFBindings.close();

        } catch ( Exception mfe ) {
            mfe.printStackTrace();
            testFailed = true;
        }

        if ( testFailed ) {
            throw new Exception("testReceiveNoWaitNullMessageTopicSecOff_B failed");
        }
    }

    public void testReceiveNoWaitNullMessageTopicSecOff_TCP(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        try {
            JMSContext jmsContextTCFTCP = tcfTCP.createContext();
            JMSConsumer jmsConsumer = jmsContextTCFTCP.createConsumer(topic);

            jmsConsumer.receiveNoWait();

            jmsConsumer.close();
            jmsContextTCFTCP.close();

        } catch ( Exception mfe ) {
            mfe.printStackTrace();
            testFailed = true;
        }

        if ( testFailed ) {
            throw new Exception("testReceiveNoWaitNullMessageTopicSecOff_TCP failed");
        }
    }

    public void testReceiveBodyTopicSecOff_B(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        try {
            JMSContext jmsContextTCFBindings = tcfBindings.createContext();
            JMSConsumer jmsConsumer = jmsContextTCFBindings.createConsumer(topic);

            JMSProducer p1 = jmsContextTCFBindings.createProducer();
            p1.send(topic, "testing");
            jmsConsumer.receiveBody(String.class);

            jmsConsumer.close();
            jmsContextTCFBindings.close();

        } catch ( Exception mfe ) {
            mfe.printStackTrace();
            testFailed = true;
        }

        if ( testFailed ) {
            throw new Exception("testReceiveBodyTopicSecOff_B failed");
        }
    }

    public void testReceiveBodyTopicSecOff_TCPIP(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;
        try {
            JMSContext jmsContextTCFTCP = tcfTCP.createContext();
            JMSConsumer jmsConsumer = jmsContextTCFTCP.createConsumer(topic);

            JMSProducer p1 = jmsContextTCFTCP.createProducer();

            p1.send(topic, "testing");
            jmsConsumer.receiveBody(String.class);

            jmsConsumer.close();
            jmsContextTCFTCP.close();

        } catch ( Exception mfe ) {
            mfe.printStackTrace();
            testFailed = true;
        }

        if ( testFailed ) {
            throw new Exception("testReceiveBodyTopicSecOff_TCPIP failed");
        }
    }

    public void testReceiveBodyTransactionTopicSecOff_B(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        try {
            UserTransaction ut = (UserTransaction)
                new InitialContext().lookup("java:comp/UserTransaction");

            ut.begin();

            JMSContext jmsContextTCFBindings = tcfBindings.createContext();

            JMSConsumer jmsConsumer = jmsContextTCFBindings.createConsumer(topic);
            JMSProducer p1 = jmsContextTCFBindings.createProducer();

            p1.send(topic, "testReceiveBodyTransactionTopicSecOff_B");

            ut.commit();

            ut.begin();
            jmsConsumer.receiveBody(String.class);
            ut.commit();

            jmsConsumer.close();
            jmsContextTCFBindings.close();

        } catch ( Exception mfe ) {
            mfe.printStackTrace();
            testFailed = true;
        }

        if ( testFailed ) {
            throw new Exception("testReceiveBodyTransactionTopicSecOff_B failed");
        }
    }

    public void testReceiveBodyTransactionTopicSecOff_TCPIP(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        try {
            UserTransaction ut = (UserTransaction)
                new InitialContext().lookup("java:comp/UserTransaction");

            ut.begin();

            JMSContext jmsContextTCFTCP = tcfTCP.createContext();

            JMSConsumer jmsConsumer = jmsContextTCFTCP.createConsumer(topic);
            JMSProducer p1 = jmsContextTCFTCP.createProducer();

            p1.send(topic, "testReceiveBodyTransactionTopicSecOff_B");

            ut.commit();

            ut.begin();
            jmsConsumer.receiveBody(String.class);
            ut.commit();

            jmsConsumer.close();
            jmsContextTCFTCP.close();

        } catch ( Exception mfe ) {
            mfe.printStackTrace();
            testFailed = true;
        }

        if ( testFailed ) {
            throw new Exception("testReceiveBodyTransactionTopicSecOff_TCPIP failed");
        }
    }

    public void testReceiveBodyTextMessageTopicSecOff_B(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        try {
            JMSContext jmsContextTCFBindings = tcfBindings.createContext();
            JMSConsumer jmsConsumer = jmsContextTCFBindings.createConsumer(topic);

            JMSProducer p1 = jmsContextTCFBindings.createProducer();

            TextMessage m1 = jmsContextTCFBindings.createTextMessage("testReceiveBodyTextMessageTopicSecOff_B");
            p1.send(topic, m1);
            jmsConsumer.receiveBody(String.class);

            jmsConsumer.close();
            jmsContextTCFBindings.close();

        } catch ( Exception mfe ) {
            mfe.printStackTrace();
            testFailed = true;
        }

        if ( testFailed ) {
            throw new Exception("testReceiveBodyTextMessageTopicSecOff_B failed");
        }
    }

    public void testReceiveBodyTextMessageTopicSecOff_TCPIP(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        try {
            JMSContext jmsContextTCFTCP = tcfTCP.createContext();

            JMSConsumer jmsConsumer = jmsContextTCFTCP.createConsumer(topic);
            JMSProducer p1 = jmsContextTCFTCP.createProducer();

            TextMessage m1 = jmsContextTCFTCP.createTextMessage("testReceiveBodyTextMessageTopicSecOff_TCPIP");
            p1.send(topic, m1);
            jmsConsumer.receiveBody(String.class);

            jmsConsumer.close();
            jmsContextTCFTCP.close();

        } catch ( Exception mfe ) {
            mfe.printStackTrace();
            testFailed = true;
        }

        if ( testFailed ) {
            throw new Exception("testReceiveBodyTextMessageTopicSecOff_TCPIP failed");
        }
    }

    public void testReceiveBodyObjectMessageTopicSecOff_B(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        try {
            JMSContext jmsContextTCFBindings = tcfBindings.createContext();
            JMSConsumer jmsConsumer = jmsContextTCFBindings.createConsumer(topic);

            JMSProducer p1 = jmsContextTCFBindings.createProducer();

            Object abc = new String("testReceiveBodyObjectMessageTopicSecOff_B");
            ObjectMessage m1 = jmsContextTCFBindings.createObjectMessage();
            m1.setObject((Serializable) abc);
            p1.send(topic, m1);

            Object msg = jmsConsumer.receiveBody(Serializable.class);
            if ( msg == null ) {
                testFailed = true;
            } else if ( !msg.equals(abc) ) {
                testFailed = true;
            }

            jmsConsumer.close();
            jmsContextTCFBindings.close();

        } catch ( Exception mfe ) {
            mfe.printStackTrace();
            testFailed = true;
        }

        if ( testFailed ) {
            throw new Exception("testReceiveBodyObjectMessageTopicSecOff_B failed");
        }
    }

    public void testReceiveBodyObjectMessageTopicSecOff_TCPIP(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        try {
            JMSContext jmsContextTCFTCP = tcfTCP.createContext();

            JMSConsumer jmsConsumer = jmsContextTCFTCP.createConsumer(topic);
            JMSProducer p1 = jmsContextTCFTCP.createProducer();

            Object abc = new String("testReceiveBodyObjectMessageTopicSecOff_TCPIP");
            ObjectMessage m1 = jmsContextTCFTCP.createObjectMessage();
            m1.setObject((Serializable) abc);
            p1.send(topic, m1);

            Object msg = jmsConsumer.receiveBody(Serializable.class);

            if ( msg == null ) {
                testFailed = true;
            } else if ( !msg.equals(abc) ) {
                testFailed = true;
            }

            jmsConsumer.close();
            jmsContextTCFTCP.close();

        } catch ( Exception mfe ) {
            mfe.printStackTrace();
            testFailed = true;
        }

        if ( testFailed ) {
            throw new Exception("testReceiveBodyObjectMessageTopicSecOff_TCPIP failed");
        }
    }

    public void testReceiveBodyMapMessageTopicSecOff_B(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;
        try {
            JMSContext jmsContextTCFBindings = tcfBindings.createContext();

            JMSConsumer jmsConsumer = jmsContextTCFBindings.createConsumer(topic);
            JMSProducer p1 = jmsContextTCFBindings.createProducer();

            MapMessage message = jmsContextTCFBindings.createMapMessage();
            message.setString("Name", "IBM");
            message.setString("Team", "WAS");

            p1.send(topic, message);
            jmsConsumer.receiveBody(Map.class);

            jmsConsumer.close();
            jmsContextTCFBindings.close();

        } catch ( Exception mfe ) {
            mfe.printStackTrace();
            testFailed = true;
        }

        if ( testFailed ) {
            throw new Exception("testReceiveBodyMapMessageTopicSecOff_B failed");
        }
    }

    public void testReceiveBodyMapMessageTopicSecOff_TCPIP(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        try {
            JMSContext jmsContextTCFTCP = tcfTCP.createContext();

            JMSConsumer jmsConsumer = jmsContextTCFTCP.createConsumer(topic);
            JMSProducer p1 = jmsContextTCFTCP.createProducer();

            MapMessage message = jmsContextTCFTCP.createMapMessage();
            message.setString("Name", "IBM");
            message.setString("Team", "WAS");

            p1.send(topic, message);
            jmsConsumer.receiveBody(java.util.Map.class);

            jmsConsumer.close();
            jmsContextTCFTCP.close();

        } catch ( Exception mfe ) {
            mfe.printStackTrace();
            testFailed = true;
        }

        if ( testFailed ) {
            throw new Exception("testReceiveBodyMapMessageTopicSecOff_TCPIP failed");
        }
    }

    public void testReceiveBodyByteMessageTopicSecOff_B(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        try {
            JMSContext jmsContextTCFBindings = tcfBindings.createContext();

            JMSConsumer jmsConsumer = jmsContextTCFBindings.createConsumer(topic);
            JMSProducer p1 = jmsContextTCFBindings.createProducer();

            byte[] data = new byte[] { 127, 0 };
            BytesMessage message = jmsContextTCFBindings.createBytesMessage();
            message.writeBytes(data);

            p1.send(topic, message);
            jmsConsumer.receiveBody(byte[].class);

            jmsConsumer.close();
            jmsContextTCFBindings.close();

        } catch ( Exception mfe ) {
            mfe.printStackTrace();
            testFailed = true;
        }

        if ( testFailed ) {
            throw new Exception("testReceiveBodyByteMessageTopicSecOff_B failed");
        }
    }

    public void testReceiveBodyByteMessageTopicSecOff_TCPIP(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        try {
            JMSContext jmsContextTCFTCP = tcfTCP.createContext();

            JMSConsumer jmsConsumer = jmsContextTCFTCP.createConsumer(topic);
            JMSProducer p1 = jmsContextTCFTCP.createProducer();

            byte[] data = new byte[] { 127, 0 };
            BytesMessage message = jmsContextTCFTCP.createBytesMessage();
            message.writeBytes(data);

            p1.send(topic, message);
            jmsConsumer.receiveBody(byte[].class);

            jmsConsumer.close();
            jmsContextTCFTCP.close();

        } catch ( Exception mfe ) {
            mfe.printStackTrace();
            testFailed = true;
        }

        if ( testFailed ) {
            throw new Exception("testReceiveBodyByteMessageTopicSecOff_TCPIP failed");
        }
    }

    public void testReceiveBodyTimeOutTopicSecOff_B(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        try {
            JMSContext jmsContextTCFBindings = tcfBindings.createContext();

            JMSConsumer jmsConsumer = jmsContextTCFBindings.createConsumer(topic);

            JMSProducer p1 = jmsContextTCFBindings.createProducer();
            p1.send(topic, "testing");

            jmsConsumer.receiveBody(String.class, 30000);

            // TODO: Why are these closes be inside of the try/catch block?
            jmsConsumer.close();
            jmsContextTCFBindings.close();

        } catch ( Exception mfe ) {
            mfe.printStackTrace();
            testFailed = true;
        }

        if ( testFailed ) {
            throw new Exception("testReceiveBodyTimeOutTopicSecOff_B failed");
        }
    }

    public void testReceiveBodyTimeOutTopicSecOff_TCPIP(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        try {
            JMSContext jmsContextTCFTCP = tcfTCP.createContext();

            JMSConsumer jmsConsumer = jmsContextTCFTCP.createConsumer(topic);

            JMSProducer p1 = jmsContextTCFTCP.createProducer();
            p1.send(topic, "testing");

            jmsConsumer.receiveBody(String.class, 30000);

            // TODO: Why are these closes be inside of the try/catch block?
            jmsConsumer.close();
            jmsContextTCFTCP.close();

        } catch ( Exception mfe ) {
            mfe.printStackTrace();
            testFailed = true;
        }

        if ( testFailed ) {
            throw new Exception("testReceiveBodyTimeOutTopicSecOff_TCPIP failed");
        }
    }

    public void testReceiveBodyTimeOutTransactionTopicSecOff_B(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        try {
            UserTransaction ut = (UserTransaction)
                new InitialContext().lookup("java:comp/UserTransaction");

            ut.begin();

            JMSContext jmsContextTCFBindings = tcfBindings.createContext();

            JMSConsumer jmsConsumer = jmsContextTCFBindings.createConsumer(topic);

            JMSProducer p1 = jmsContextTCFBindings.createProducer();
            p1.send(topic, "testReceiveBodyTransactionTopicSecOff_B");

            ut.commit();

            ut.begin();
            jmsConsumer.receiveBody(String.class, 30000);
            ut.commit();

            // TODO: Why are these closes be inside of the try/catch block?
            jmsConsumer.close();
            jmsContextTCFBindings.close();

        } catch ( Exception mfe ) {
            mfe.printStackTrace();
            testFailed = true;
        }

        if ( testFailed ) {
            throw new Exception("testReceiveBodyTimeOutTransactionTopicSecOff_B failed");
        }
    }

    public void testReceiveBodyTimeOutTransactionTopicSecOff_TCPIP(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        try {
            UserTransaction ut = (UserTransaction)
                new InitialContext().lookup("java:comp/UserTransaction");

            ut.begin();

            JMSContext jmsContextTCFTCP = tcfTCP.createContext();

            JMSConsumer jmsConsumer = jmsContextTCFTCP.createConsumer(topic);

            JMSProducer p1 = jmsContextTCFTCP.createProducer();
            p1.send(topic, "testReceiveBodyTransactionTopicSecOff_B");

            ut.commit();

            ut.begin();
            jmsConsumer.receiveBody(String.class, 30000);
            ut.commit();

            // TODO: Why are these closes be inside of the try/catch block?
            jmsConsumer.close();
            jmsContextTCFTCP.close();

        } catch ( Exception mfe ) {
            mfe.printStackTrace();
            testFailed = true;
        }

        if ( testFailed ) {
            throw new Exception("testReceiveBodyTimeOutTransactionTopicSecOff_TCPIP failed");
        }
    }

    public void testReceiveBodyTimeOutTextMessageTopicSecOff_B(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        try {
            JMSContext jmsContextTCFBindings = tcfBindings.createContext();

            JMSConsumer jmsConsumer = jmsContextTCFBindings.createConsumer(topic);

            JMSProducer p1 = jmsContextTCFBindings.createProducer();
            TextMessage m1 = jmsContextTCFBindings.createTextMessage("testReceiveBodyTimeOutTextMessageTopicSecOff_B");
            p1.send(topic, m1);

            jmsConsumer.receiveBody(String.class, 30000);

            // TODO: Why are these closes be inside of the try/catch block?
            jmsConsumer.close();
            jmsContextTCFBindings.close();

        } catch ( Exception mfe ) {
            mfe.printStackTrace();
            testFailed = true;
        }

        if ( testFailed ) {
            throw new Exception("testReceiveBodyTimeOutTextMessageTopicSecOff_B failed");
        }
    }

    public void testReceiveBodyTimeOutTextMessageTopicSecOff_TCPIP(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        try {
            JMSContext jmsContextTCFTCP = tcfTCP.createContext();

            JMSConsumer jmsConsumer = jmsContextTCFTCP.createConsumer(topic);

            JMSProducer p1 = jmsContextTCFTCP.createProducer();
            TextMessage m1 = jmsContextTCFTCP.createTextMessage("testReceiveBodyTimeOutTextMessageTopicSecOff_TCPIP");
            p1.send(topic, m1);

            jmsConsumer.receiveBody(String.class, 30000);

            // TODO: Why are these closes be inside of the try/catch block?
            jmsConsumer.close();
            jmsContextTCFTCP.close();

        } catch ( Exception mfe ) {
            mfe.printStackTrace();
            testFailed = true;
        }

        if ( testFailed ) {
            throw new Exception("testReceiveBodyTimeOutTextMessageTopicSecOff_TCPIP failed");
        }
    }

    public void testReceiveBodyTimeOutObjectMessageTopicSecOff_B(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        try {
            JMSContext jmsContextTCFBindings = tcfBindings.createContext();

            JMSConsumer jmsConsumer = jmsContextTCFBindings.createConsumer(topic);

            JMSProducer p1 = jmsContextTCFBindings.createProducer();
            Object abc = new String("testReceiveBodyTimeOutObjectMessageTopicSecOff_B");
            ObjectMessage m1 = jmsContextTCFBindings.createObjectMessage();
            m1.setObject((Serializable) abc);
            p1.send(topic, m1);
            Object msg = jmsConsumer.receiveBody(Serializable.class, 30000);

            if ( (msg == null) || !msg.equals(abc) ) {
                testFailed = true;
            }

            // TODO: Why are these closes be inside of the try/catch block?
            jmsConsumer.close();
            jmsContextTCFBindings.close();

        } catch ( Exception mfe ) {
            mfe.printStackTrace();
            testFailed = true;
        }

        if ( testFailed ) {
            throw new Exception("testReceiveBodyTimeOutObjectMessageTopicSecOff_B failed");
        }
    }

    public void testReceiveBodyTimeOutObjectMessageTopicSecOff_TCPIP(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        try {
            JMSContext jmsContextTCFTCP = tcfTCP.createContext();

            JMSConsumer jmsConsumer = jmsContextTCFTCP.createConsumer(topic);

            JMSProducer p1 = jmsContextTCFTCP.createProducer();
            Object abc = new String("testReceiveBodyTimeOutObjectMessageTopicSecOff_TCPIP");
            ObjectMessage m1 = jmsContextTCFTCP.createObjectMessage();
            m1.setObject((Serializable) abc);
            p1.send(topic, m1);

            Object msg = jmsConsumer.receiveBody(Serializable.class, 30000);
            if ( (msg == null) || !msg.equals(abc) ) {
                testFailed = true;
            }

            // TODO: Why are these closes be inside of the try/catch block?
            jmsConsumer.close();
            jmsContextTCFTCP.close();

        } catch ( Exception mfe ) {
            mfe.printStackTrace();
            testFailed = true;
        }

        if ( testFailed ) {
            throw new Exception("testReceiveBodyTimeOutObjectMessageTopicSecOff_TCPIP failed");
        }
    }

    public void testReceiveBodyTimeOutMapMessageTopicSecOff_B(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        try {
            JMSContext jmsContextTCFBindings = tcfBindings.createContext();

            JMSConsumer jmsConsumer = jmsContextTCFBindings.createConsumer(topic);

            JMSProducer p1 = jmsContextTCFBindings.createProducer();
            MapMessage message = jmsContextTCFBindings.createMapMessage();
            message.setString("Name", "IBM");
            message.setString("Team", "WAS");
            p1.send(topic, message);

            jmsConsumer.receiveBody(java.util.Map.class, 30000);

            // TODO: Why are these closes be inside of the try/catch block?
            jmsConsumer.close();
            jmsContextTCFBindings.close();

        } catch ( Exception mfe ) {
            mfe.printStackTrace();
            testFailed = true;
        }

        if ( testFailed ) {
            throw new Exception("testReceiveBodyTimeOutMapMessageTopicSecOff_B failed");
        }
    }

    public void testReceiveBodyTimeOutMapMessageTopicSecOff_TCPIP(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        try {
            JMSContext jmsContextTCFTCP = tcfTCP.createContext();

            JMSConsumer jmsConsumer = jmsContextTCFTCP.createConsumer(topic);

            JMSProducer p1 = jmsContextTCFTCP.createProducer();
            MapMessage message = jmsContextTCFTCP.createMapMessage();
            message.setString("Name", "IBM");
            message.setString("Team", "WAS");
            p1.send(topic, message);

            jmsConsumer.receiveBody(java.util.Map.class, 30000);

            // TODO: Why are these closes be inside of the try/catch block?
            jmsConsumer.close();
            jmsContextTCFTCP.close();

        } catch ( Exception mfe ) {
            mfe.printStackTrace();
            testFailed = true;
        }

        if ( testFailed ) {
            throw new Exception("testReceiveBodyTimeOutMapMessageTopicSecOff_TCPIP failed");
        }
    }

    public void testReceiveBodyTimeOutByteMessageTopicSecOff_B(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        try {
            JMSContext jmsContextTCFBindings = tcfBindings.createContext();

            JMSConsumer jmsConsumer = jmsContextTCFBindings.createConsumer(topic);

            JMSProducer p1 = jmsContextTCFBindings.createProducer();
            byte[] data = new byte[] { 127, 0 };
            BytesMessage message = jmsContextTCFBindings.createBytesMessage();
            message.writeBytes(data);
            p1.send(topic, message);

            jmsConsumer.receiveBody(byte[].class, 30000);

            // TODO: Why are these closes be inside of the try/catch block?
            jmsConsumer.close();
            jmsContextTCFBindings.close();

        } catch ( Exception mfe ) {
            mfe.printStackTrace();
            testFailed = true;
        }

        if ( testFailed ) {
            throw new Exception("testReceiveBodyTimeOutByteMessageTopicSecOff_B failed");
        }
    }

    public void testReceiveBodyTimeOutByteMessageTopicSecOff_TCPIP(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        try {
            JMSContext jmsContextTCFTCP = tcfTCP.createContext();

            JMSConsumer jmsConsumer = jmsContextTCFTCP.createConsumer(topic);

            JMSProducer p1 = jmsContextTCFTCP.createProducer();
            byte[] data = new byte[] { 127, 0 };
            BytesMessage message = jmsContextTCFTCP.createBytesMessage();
            message.writeBytes(data);
            p1.send(topic, message);

            jmsConsumer.receiveBody(byte[].class, 30000);

            // TODO: Why are these closes be inside of the try/catch block?
            jmsConsumer.close();
            jmsContextTCFTCP.close();

        } catch ( Exception mfe ) {
            mfe.printStackTrace();
            testFailed = true;
        }

        if ( testFailed ) {
            throw new Exception("testReceiveBodyTimeOutByteMessageTopicSecOff_TCPIP failed");
        }
    }

    public void testReceiveBodyNoWaitTopicSecOff_B(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;
        try {
            JMSContext jmsContextTCFBindings = tcfBindings.createContext();

            JMSConsumer jmsConsumer = jmsContextTCFBindings.createConsumer(topic);

            JMSProducer p1 = jmsContextTCFBindings.createProducer();
            p1.send(topic, "testing");

            jmsConsumer.receiveBodyNoWait(String.class);

            // TODO: Why are these closes be inside of the try/catch block?
            jmsConsumer.close();
            jmsContextTCFBindings.close();

        } catch ( Exception mfe ) {
            mfe.printStackTrace();
            testFailed = true;
        }

        if ( testFailed ) {
            throw new Exception("testReceiveBodyNoWaitTopicSecOff_B failed");
        }

    }

    public void testReceiveBodyNoWaitTopicSecOff_TCPIP(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        try {
            JMSContext jmsContextTCFTCP = tcfTCP.createContext();

            JMSConsumer jmsConsumer = jmsContextTCFTCP.createConsumer(topic);

            JMSProducer p1 = jmsContextTCFTCP.createProducer();
            p1.send(topic, "testing");

            jmsConsumer.receiveBodyNoWait(String.class);

            // TODO: Why are these closes be inside of the try/catch block?
            jmsConsumer.close();
            jmsContextTCFTCP.close();

        } catch ( Exception mfe ) {
            mfe.printStackTrace();
            testFailed = true;
        }

        if ( testFailed ) {
            throw new Exception("testReceiveBodyNoWaitTopicSecOff_TCPIP failed");
        }
    }

    public void testReceiveBodyNoWaitTransactionTopicSecOff_B(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        try {
            UserTransaction ut = (UserTransaction)
                new InitialContext().lookup("java:comp/UserTransaction");

            ut.begin();

            JMSContext jmsContextTCFBindings = tcfBindings.createContext();

            JMSConsumer jmsConsumer = jmsContextTCFBindings.createConsumer(topic);

            JMSProducer p1 = jmsContextTCFBindings.createProducer();
            p1.send(topic, "testReceiveBodyNoWaitTransactionTopicSecOff_B");

            ut.commit();

            ut.begin();
            jmsConsumer.receiveBodyNoWait(String.class);
            ut.commit();

            // TODO: Why are these closes be inside of the try/catch block?
            jmsConsumer.close();
            jmsContextTCFBindings.close();

        } catch ( Exception mfe ) {
            mfe.printStackTrace();
            testFailed = true;
        }

        if ( testFailed ) {
            throw new Exception("testReceiveBodyNoWaitTransactionTopicSecOff_B failed");
        }

    }

    public void testReceiveBodyNoWaitTransactionTopicSecOff_TCPIP(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        try {
            UserTransaction ut = (UserTransaction)
                new InitialContext().lookup("java:comp/UserTransaction");

            ut.begin();

            JMSContext jmsContextTCFTCP = tcfTCP.createContext();

            JMSConsumer jmsConsumer = jmsContextTCFTCP.createConsumer(topic);

            JMSProducer p1 = jmsContextTCFTCP.createProducer();
            p1.send(topic, "testReceiveBodyTransactionTopicSecOff_B");

            ut.commit();

            ut.begin();
            jmsConsumer.receiveBodyNoWait(String.class);
            ut.commit();

            // TODO: Why are these closes be inside of the try/catch block?
            jmsConsumer.close();
            jmsContextTCFTCP.close();

        } catch ( Exception mfe ) {
            mfe.printStackTrace();
            testFailed = true;
        }

        if ( testFailed ) {
            throw new Exception("testReceiveBodyNoWaitTransactionTopicSecOff_TCPIP failed");
        }
    }

    public void testReceiveBodyNoWaitTextMessageTopicSecOff_B(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        try {
            JMSContext jmsContextTCFBindings = tcfBindings.createContext();

            JMSConsumer jmsConsumer = jmsContextTCFBindings.createConsumer(topic);

            JMSProducer p1 = jmsContextTCFBindings.createProducer();
            TextMessage m1 = jmsContextTCFBindings.createTextMessage("testReceiveBodyTextMessageTopicSecOff_B");
            p1.send(topic, m1);

            jmsConsumer.receiveBodyNoWait(String.class);

            // TODO: Why are these closes be inside of the try/catch block?
            jmsConsumer.close();
            jmsContextTCFBindings.close();

        } catch ( Exception mfe ) {
            mfe.printStackTrace();
            testFailed = true;
        }

        if ( testFailed ) {
            throw new Exception("testReceiveBodyNoWaitTextMessageTopicSecOff_B failed");
        }
    }

    public void testReceiveBodyNoWaitTextMessageTopicSecOff_TCPIP(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        try {
            JMSContext jmsContextTCFTCP = tcfTCP.createContext();

            JMSConsumer jmsConsumer = jmsContextTCFTCP.createConsumer(topic);

            JMSProducer p1 = jmsContextTCFTCP.createProducer();
            TextMessage m1 = jmsContextTCFTCP.createTextMessage("testReceiveBodyTextMessageTopicSecOff_TCPIP");
            p1.send(topic, m1);

            jmsConsumer.receiveBodyNoWait(String.class);

            // TODO: Why are these closes be inside of the try/catch block?
            jmsConsumer.close();
            jmsContextTCFTCP.close();

        } catch ( Exception mfe ) {
            mfe.printStackTrace();
            testFailed = true;
        }

        if ( testFailed ) {
            throw new Exception("testReceiveBodyNoWaitTextMessageTopicSecOff_TCPIP failed");
        }
    }

    public void testReceiveBodyNoWaitObjectMessageTopicSecOff_B(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        try {
            JMSContext jmsContextTCFBindings = tcfBindings.createContext();

            JMSConsumer jmsConsumer = jmsContextTCFBindings.createConsumer(topic);

            JMSProducer p1 = jmsContextTCFBindings.createProducer();

            Object abc = new String("testReceiveBodyObjectMessageTopicSecOff_B");
            ObjectMessage m1 = jmsContextTCFBindings.createObjectMessage();
            m1.setObject((Serializable) abc);
            p1.send(topic, m1);

            Object msg = jmsConsumer.receiveBodyNoWait(Serializable.class);
            if ( (msg == null)  || !msg.equals(abc) ) {
                testFailed = true;
            }

            // TODO: Why are these closes be inside of the try/catch block?
            jmsConsumer.close();
            jmsContextTCFBindings.close();

        } catch ( Exception mfe ) {
            mfe.printStackTrace();
            testFailed = true;
        }

        if ( testFailed ) {
            throw new Exception("testReceiveBodyNoWaitObjectMessageTopicSecOff_B failed");
        }
    }

    public void testReceiveBodyNoWaitObjectMessageTopicSecOff_TCPIP(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        try {
            JMSContext jmsContextTCFTCP = tcfTCP.createContext();

            JMSConsumer jmsConsumer = jmsContextTCFTCP.createConsumer(topic);

            JMSProducer p1 = jmsContextTCFTCP.createProducer();

            Object abc = new String("testReceiveBodyObjectMessageTopicSecOff_TCPIP");
            ObjectMessage m1 = jmsContextTCFTCP.createObjectMessage();
            m1.setObject((Serializable) abc);
            p1.send(topic, m1);

            Object msg = jmsConsumer.receiveBodyNoWait(Serializable.class);
            if ( (msg == null)  || !msg.equals(abc) ) {
                testFailed = true;
            }

            // TODO: Why are these closes be inside of the try/catch block?
            jmsConsumer.close();
            jmsContextTCFTCP.close();

        } catch ( Exception mfe ) {
            mfe.printStackTrace();
            testFailed = true;
        }

        if ( testFailed ) {
            throw new Exception("testReceiveBodyNoWaitObjectMessageTopicSecOff_TCPIP failed");
        }
    }

    public void testReceiveBodyNoWaitMapMessageTopicSecOff_B(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;
        try {
            JMSContext jmsContextTCFBindings = tcfBindings.createContext();

            JMSConsumer jmsConsumer = jmsContextTCFBindings.createConsumer(topic);

            JMSProducer p1 = jmsContextTCFBindings.createProducer();
            MapMessage message = jmsContextTCFBindings.createMapMessage();
            message.setString("Name", "IBM");
            message.setString("Team", "WAS");
            p1.send(topic, message);

            jmsConsumer.receiveBodyNoWait(java.util.Map.class);

            // TODO: Why are these closes be inside of the try/catch block?
            jmsConsumer.close();
            jmsContextTCFBindings.close();

        } catch ( Exception mfe ) {
            mfe.printStackTrace();
            testFailed = true;
        }

        if ( testFailed ) {
            throw new Exception("testReceiveBodyNoWaitMapMessageTopicSecOff_B failed");
        }
    }

    public void testReceiveBodyNoWaitMapMessageTopicSecOff_TCPIP(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        try {
            JMSContext jmsContextTCFTCP = tcfTCP.createContext();

            JMSConsumer jmsConsumer = jmsContextTCFTCP.createConsumer(topic);

            JMSProducer p1 = jmsContextTCFTCP.createProducer();
            MapMessage message = jmsContextTCFTCP.createMapMessage();
            message.setString("Name", "IBM");
            message.setString("Team", "WAS");
            p1.send(topic, message);

            jmsConsumer.receiveBodyNoWait(java.util.Map.class);

            // TODO: Why are these closes be inside of the try/catch block?
            jmsConsumer.close();
            jmsContextTCFTCP.close();

        } catch ( Exception mfe ) {
            mfe.printStackTrace();
            testFailed = true;
        }

        if ( testFailed ) {
            throw new Exception("testReceiveBodyNoWaitMapMessageTopicSecOff_TCPIP failed");
        }
    }

    public void testReceiveBodyNoWaitByteMessageTopicSecOff_B(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        try {
            JMSContext jmsContextTCFBindings = tcfBindings.createContext();

            JMSConsumer jmsConsumer = jmsContextTCFBindings.createConsumer(topic);

            JMSProducer p1 = jmsContextTCFBindings.createProducer();

            byte[] data = new byte[] { 127, 0 };
            BytesMessage message = jmsContextTCFBindings.createBytesMessage();
            message.writeBytes(data);
            p1.send(topic, message);

            jmsConsumer.receiveBodyNoWait(byte[].class);

            // TODO: Why are these closes be inside of the try/catch block?
            jmsConsumer.close();
            jmsContextTCFBindings.close();

        } catch ( Exception mfe ) {
            mfe.printStackTrace();
            testFailed = true;
        }

        if ( testFailed ) {
            throw new Exception("testReceiveBodyNoWaitByteMessageTopicSecOff_B failed");
        }
    }

    public void testReceiveBodyNoWaitByteMessageTopicSecOff_TCPIP(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        try {
            JMSContext jmsContextTCFTCP = tcfTCP.createContext();

            JMSConsumer jmsConsumer = jmsContextTCFTCP.createConsumer(topic);

            JMSProducer p1 = jmsContextTCFTCP.createProducer();
            byte[] data = new byte[] { 127, 0 };
            BytesMessage message = jmsContextTCFTCP.createBytesMessage();
            message.writeBytes(data);
            p1.send(topic, message);

            jmsConsumer.receiveBodyNoWait(byte[].class);

            // TODO: Why are these closes be inside of the try/catch block?
            jmsConsumer.close();
            jmsContextTCFTCP.close();

        } catch ( Exception mfe ) {
            mfe.printStackTrace();
            testFailed = true;
        }

        if ( testFailed ) {
            throw new Exception("testReceiveBodyNoWaitByteMessageTopicSecOff_TCPIP failed");
        }
    }

    public void testReceiveBodyMFENoBodyTopicSecOff_B(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContextTCFBindings = tcfBindings.createContext();

        JMSConsumer jmsConsumer = jmsContextTCFBindings.createConsumer(topic);

        JMSProducer p1 = jmsContextTCFBindings.createProducer();
        TextMessage m = jmsContextTCFBindings.createTextMessage();
        p1.send(topic, m);

        try {
            jmsConsumer.receiveBody(String.class);
            testFailed = true;
        } catch ( MessageFormatRuntimeException e ) {
            // Expected
        }

        jmsConsumer.close();
        jmsContextTCFBindings.close();

        if ( testFailed ) {
            throw new Exception("testReceiveBodyMFENoBodyTopicSecOff_B failed");
        }
    }

    public void testReceiveBodyMFENoBodyTopicSecOff_TCP(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContextTCFTCP = tcfTCP.createContext();

        JMSConsumer jmsConsumer = jmsContextTCFTCP.createConsumer(topic);

        JMSProducer p1 = jmsContextTCFTCP.createProducer();
        TextMessage m = jmsContextTCFTCP.createTextMessage();
        p1.send(topic, m);

        try {
            jmsConsumer.receiveBody(String.class);
            testFailed = true;
        } catch ( MessageFormatRuntimeException e ) {
            // Expected
        }

        jmsConsumer.close();
        jmsContextTCFTCP.close();

        if ( testFailed ) {
            throw new Exception("testReceiveBodyMFENoBodyTopicSecOff_TCP failed");
        }
    }

    public void testReceiveBodyMFEUnspecifiedTypeTopicSecOff_B(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContextTCFBindings = tcfBindings.createContext();

        JMSConsumer jmsConsumer = jmsContextTCFBindings.createConsumer(topic);

        JMSProducer p1 = jmsContextTCFBindings.createProducer();
        TextMessage m = jmsContextTCFBindings.createTextMessage();
        p1.send(topic, m);

        try {
            jmsConsumer.receiveBody(byte[].class);
            testFailed = true;
        } catch ( MessageFormatRuntimeException e ) {
            // Expected
        }

        jmsConsumer.close();
        jmsContextTCFBindings.close();

        if ( testFailed ) {
            throw new Exception("testReceiveBodyMFEUnspecifiedTypeTopicSecOff_B failed");
        }
    }

    public void testReceiveBodyMFEUnspecifiedTypeTopicSecOff_TCP(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContextTCFTCP = tcfTCP.createContext();

        JMSConsumer jmsConsumer = jmsContextTCFTCP.createConsumer(topic);

        JMSProducer p1 = jmsContextTCFTCP.createProducer();
        TextMessage m = jmsContextTCFTCP.createTextMessage();
        p1.send(topic, m);

        try {
            jmsConsumer.receiveBody(byte[].class);
            testFailed = true;
        } catch ( MessageFormatRuntimeException e ) {
            // Expected
        }

        jmsConsumer.close();
        jmsContextTCFTCP.close();

        if ( testFailed ) {
            throw new Exception("testReceiveBodyMFEUnspecifiedTypeTopicSecOff_TCP failed");
        }
    }

    public void testReceiveBodyMFEUnsupportedTypeTopicSecOff_B(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContextTCFBindings = tcfBindings.createContext();

        JMSConsumer jmsConsumer = jmsContextTCFBindings.createConsumer(topic);

        JMSProducer p1 = jmsContextTCFBindings.createProducer();
        StreamMessage m = jmsContextTCFBindings.createStreamMessage();
        p1.send(topic, m);

        try {
            jmsConsumer.receiveBody(Object.class);
            testFailed = true;
        } catch ( MessageFormatRuntimeException e ) {
            // Expected
        }

        jmsConsumer.close();
        jmsContextTCFBindings.close();

        if ( testFailed ) {
            throw new Exception("testReceiveBodyMFEUnsupportedTypeTopicSecOff_B failed");
        }
    }

    public void testReceiveBodyMFEUnsupportedTypeTopicSecOff_TCP(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContextTCFTCP = tcfTCP.createContext();

        JMSConsumer jmsConsumer = jmsContextTCFTCP.createConsumer(topic);

        JMSProducer p1 = jmsContextTCFTCP.createProducer();
        StreamMessage m = jmsContextTCFTCP.createStreamMessage();
        p1.send(topic, m);

        try {
            jmsConsumer.receiveBody(Object.class);
            testFailed = true;
        } catch ( MessageFormatRuntimeException e ) {
            // Expected
        }

        jmsConsumer.close();
        jmsContextTCFTCP.close();

        if ( testFailed ) {
            throw new Exception("testReceiveBodyMFEUnsupportedTypeTopicSecOff_TCP failed");
        }
    }

    public void testReceiveBodyTimeOutMFENoBodyTopicSecOff_B(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContextTCFBindings = tcfBindings.createContext();

        JMSConsumer jmsConsumer = jmsContextTCFBindings.createConsumer(topic);

        JMSProducer p1 = jmsContextTCFBindings.createProducer();
        TextMessage m = jmsContextTCFBindings.createTextMessage();
        p1.send(topic, m);

        try {
            jmsConsumer.receiveBody(String.class, 30000);
            testFailed = true;
        } catch ( MessageFormatRuntimeException e ) {
            // Expected
        }

        jmsConsumer.close();
        jmsContextTCFBindings.close();

        if ( testFailed ) {
            throw new Exception("testReceiveBodyTimeOutMFENoBodyTopicSecOff_B failed");
        }
    }

    public void testReceiveBodyTimeOutMFENoBodyTopicSecOff_TCP(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContextTCFTCP = tcfTCP.createContext();

        JMSConsumer jmsConsumer = jmsContextTCFTCP.createConsumer(topic);

        JMSProducer p1 = jmsContextTCFTCP.createProducer();
        TextMessage m = jmsContextTCFTCP.createTextMessage();
        p1.send(topic, m);

        try {
            jmsConsumer.receiveBody(String.class, 30000);
            testFailed = true;
        } catch ( MessageFormatRuntimeException e ) {
            // Expected
        }

        jmsConsumer.close();
        jmsContextTCFTCP.close();

        if ( testFailed ) {
            throw new Exception("testReceiveBodyTimeOutMFENoBodyTopicSecOff_TCP failed");
        }
    }

    public void testReceiveBodyTimeOutMFEUnspecifiedTypeTopicSecOff_B(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContextTCFBindings = tcfBindings.createContext();

        JMSConsumer jmsConsumer = jmsContextTCFBindings.createConsumer(topic);

        JMSProducer p1 = jmsContextTCFBindings.createProducer();
        TextMessage m = jmsContextTCFBindings.createTextMessage();
        p1.send(topic, m);

        try {
            jmsConsumer.receiveBody(byte[].class, 30000);
            testFailed = true;
        } catch ( MessageFormatRuntimeException e ) {
            // Expected
        }

        jmsConsumer.close();
        jmsContextTCFBindings.close();

        if ( testFailed ) {
            throw new Exception("testReceiveBodyTimeOutMFEUnspecifiedTypeTopicSecOff_B failed");
        }
    }

    public void testReceiveBodyTimeOutMFEUnspecifiedTypeTopicSecOff_TCP(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContextTCFTCP = tcfTCP.createContext();

        JMSConsumer jmsConsumer = jmsContextTCFTCP.createConsumer(topic);

        JMSProducer p1 = jmsContextTCFTCP.createProducer();
        TextMessage m = jmsContextTCFTCP.createTextMessage();
        p1.send(topic, m);

        try {
            jmsConsumer.receiveBody(byte[].class, 30000);
            testFailed = true;
        } catch ( MessageFormatRuntimeException e ) {
            // Expected
        }

        jmsConsumer.close();
        jmsContextTCFTCP.close();

        if ( testFailed ) {
            throw new Exception("testReceiveBodyTimeOutMFEUnspecifiedTypeTopicSecOff_TCP failed");
        }
    }

    public void testReceiveBodyTimeOutMFEUnsupportedTypeTopicSecOff_B(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContextTCFBindings = tcfBindings.createContext();

        JMSConsumer jmsConsumer = jmsContextTCFBindings.createConsumer(topic);

        JMSProducer p1 = jmsContextTCFBindings.createProducer();
        StreamMessage m = jmsContextTCFBindings.createStreamMessage();
        p1.send(topic, m);

        try {
            jmsConsumer.receiveBody(Object.class, 30000);
            testFailed = true;
        } catch ( MessageFormatRuntimeException e ) {
            // Expected
        }

        jmsConsumer.close();
        jmsContextTCFBindings.close();

        if ( testFailed ) {
            throw new Exception("testReceiveBodyTimeOutMFEUnsupportedTypeTopicSecOff_B failed");
        }
    }

    public void testReceiveBodyTimeOutMFEUnsupportedTypeTopicSecOff_TCP(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContextTCFTCP = tcfTCP.createContext();

        JMSConsumer jmsConsumer = jmsContextTCFTCP.createConsumer(topic);

        JMSProducer p1 = jmsContextTCFTCP.createProducer();
        StreamMessage m = jmsContextTCFTCP.createStreamMessage();
        p1.send(topic, m);

        try {
            jmsConsumer.receiveBody(Object.class, 30000);
            testFailed = true;
        } catch ( MessageFormatRuntimeException e ) {
            // Expected
        }

        jmsConsumer.close();
        jmsContextTCFTCP.close();

        if ( testFailed ) {
            throw new Exception("testReceiveBodyTimeOutMFEUnsupportedTypeTopicSecOff_TCP failed");
        }
    }

    public void testReceiveBodyNoWaitMFENoBodyTopicSecOff_B(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContextTCFBindings = tcfBindings.createContext();

        JMSConsumer jmsConsumer = jmsContextTCFBindings.createConsumer(topic);

        JMSProducer p1 = jmsContextTCFBindings.createProducer();
        TextMessage m = jmsContextTCFBindings.createTextMessage();
        p1.send(topic, m);

        try {
            jmsConsumer.receiveBodyNoWait(String.class);
            testFailed = true;
        } catch ( MessageFormatRuntimeException e ) {
            // Expected
        }

        jmsConsumer.close();
        jmsContextTCFBindings.close();

        if ( testFailed ) {
            throw new Exception("testReceiveBodyNoWaitMFENoBodyTopicSecOff_B failed");
        }
    }

    public void testReceiveBodyNoWaitMFENoBodyTopicSecOff_TCP(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        Topic topic = (Topic) new InitialContext().lookup("java:comp/env/eis/topic12");

        JMSContext jmsContextTCFTCP = tcfTCP.createContext();

        JMSConsumer jmsConsumer = jmsContextTCFTCP.createConsumer(topic);

        JMSProducer p1 = jmsContextTCFTCP.createProducer();
        TextMessage m = jmsContextTCFTCP.createTextMessage();
        p1.send(topic, m);

        String body = "none";
        try {
            body = jmsConsumer.receiveBodyNoWait(String.class);
            testFailed = true;
        } catch ( MessageFormatRuntimeException e ) {
            // Expected
        }

        jmsConsumer.close();
        jmsContextTCFTCP.close();

        if ( testFailed ) {
            throw new Exception("testReceiveBodyNoWaitMFENoBodyTopicSecOff_TCP failed body [ " + body + " ]");
        }
    }

    public void testReceiveBodyNoWaitMFEUnspecifiedTypeTopicSecOff_B(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContextTCFBindings = tcfBindings.createContext();

        JMSConsumer jmsConsumer = jmsContextTCFBindings.createConsumer(topic);
        JMSProducer p1 = jmsContextTCFBindings.createProducer();

        TextMessage m = jmsContextTCFBindings.createTextMessage();
        p1.send(topic, m);

        try {
            jmsConsumer.receiveBodyNoWait(byte[].class);
            testFailed = true;
        } catch ( MessageFormatRuntimeException e ) {
            // Expected
        }

        jmsConsumer.close();
        jmsContextTCFBindings.close();

        if ( testFailed ) {
            throw new Exception("testReceiveBodyNoWaitMFEUnspecifiedTypeTopicSecOff_B failed");
        }
    }

    public void testReceiveBodyNoWaitMFEUnspecifiedTypeTopicSecOff_TCP(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        String failureReason = null;

        Topic topic = (Topic) new InitialContext().lookup("java:comp/env/eis/topic12");

        JMSContext jmsContextTCFTCP = tcfTCP.createContext();

        JMSConsumer jmsConsumer = jmsContextTCFTCP.createConsumer(topic);
        JMSProducer producer = jmsContextTCFTCP.createProducer();

        TextMessage message = jmsContextTCFTCP.createTextMessage();

        Message m = jmsConsumer.receiveNoWait();
        if ( m != null ) {
            failureReason = "Topic was not clear before test.";

        } else {
            producer.send(topic, message);

            try {
                jmsConsumer.receiveBodyNoWait(String.class);
                String error = jmsConsumer.receiveBodyNoWait(String.class);
                failureReason = "Expected exception was not received";
            } catch ( MessageFormatRuntimeException e ) {
                // Expected
            }
        }

        jmsConsumer.close();
        jmsContextTCFTCP.close();

        if ( failureReason != null ) {
            throw new Exception("testReceiveBodyNoWaitMFEUnspecifiedTypeTopicSecOff_TCP failed: " + failureReason);
        }
    }

    public void testReceiveBodyNoWaitMFEUnsupportedTypeTopicSecOff_B(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContextTCFBindings = tcfBindings.createContext();

        JMSConsumer jmsConsumer = jmsContextTCFBindings.createConsumer(topic);
        JMSProducer p1 = jmsContextTCFBindings.createProducer();

        StreamMessage m = jmsContextTCFBindings.createStreamMessage();
        p1.send(topic, m);

        try {
            jmsConsumer.receiveBodyNoWait(Object.class);
            testFailed = true;
        } catch ( MessageFormatRuntimeException e ) {
            // Expected
        }

        jmsConsumer.close();
        jmsContextTCFBindings.close();

        if ( testFailed ) {
            throw new Exception("testReceiveBodyNoWaitMFEUnsupportedTypeTopicSecOff_B failed");
        }
    }

    public void testReceiveBodyNoWaitMFEUnsupportedTypeTopicSecOff_TCP(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContextTCFTCP = tcfTCP.createContext();

        JMSConsumer jmsConsumer = jmsContextTCFTCP.createConsumer(topic);
        JMSProducer p1 = jmsContextTCFTCP.createProducer();

        StreamMessage m = jmsContextTCFTCP.createStreamMessage();
        p1.send(topic, m);

        try {
            jmsConsumer.receiveBodyNoWait(Object.class);
            testFailed = true;
        } catch ( MessageFormatRuntimeException e ) {
            // Expected
        }

        jmsConsumer.close();
        jmsContextTCFTCP.close();

        if ( testFailed ) {
            throw new Exception("testReceiveBodyNoWaitMFEUnsupportedTypeTopicSecOff_TCP failed");
        }
    }

    public void testStartJMSContextSecOffBinding(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContextQCFBindings = qcfBindings.createContext();
        emptyQueue(qcfBindings, queue);

        jmsContextQCFBindings.setAutoStart(false);

        String outbound = "Hello World";
        JMSProducer jmsProducerQCFBindings = jmsContextQCFBindings.createProducer();
        jmsProducerQCFBindings.send(queue, outbound);

        jmsContextQCFBindings.start();

        JMSConsumer jmsConsumerQCFBindings = jmsContextQCFBindings.createConsumer(queue);
        TextMessage receiveMsg = (TextMessage) jmsConsumerQCFBindings.receive(30000);
        String inbound = receiveMsg.getText();

        if ( !outbound.equals(inbound) ) {
            testFailed = true;
        }

        jmsConsumerQCFBindings.close();
        jmsContextQCFBindings.close();

        if ( testFailed ) {
            throw new Exception("testStartJMSContextSecOnBinding failed");
        }
    }

    public void testStartJMSContextSecOffTCP(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContextQCFTCP = qcfTCP.createContext();
        emptyQueue(qcfTCP, queue);

        jmsContextQCFTCP.setAutoStart(false);

        String outbound = "Hello World";
        JMSProducer jmsProducerQCFTCP = jmsContextQCFTCP.createProducer();
        jmsProducerQCFTCP.send(queue, outbound);

        jmsContextQCFTCP.start();

        JMSConsumer jmsConsumerQCFTCP = jmsContextQCFTCP.createConsumer(queue);
        TextMessage receiveMsg = (TextMessage) jmsConsumerQCFTCP.receive(30000);
        String inbound = receiveMsg.getText();

        if ( !outbound.equals(inbound) ) {
            testFailed = true;
        }

        jmsConsumerQCFTCP.close();
        jmsContextQCFTCP.close();

        if ( testFailed ) {
            throw new Exception("testStartJMSContextSecOffTCP failed");
        }
    }

    public void testStartJMSContextStartSecOffBinding(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        try {
            JMSContext jmsContextQCFBindings = qcfBindings.createContext();

            jmsContextQCFBindings.start();
            jmsContextQCFBindings.start();

            jmsContextQCFBindings.close();

        } catch ( Exception e ) {
            e.printStackTrace();
            testFailed = true;
        }

        if ( testFailed ) {
            throw new Exception("testStartJMSContextStartSecOffBinding failed");
        }
    }

    public void testStartJMSContextStartSecOffTCP(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        try {
            JMSContext jmsContextQCFTCP = qcfTCP.createContext();

            jmsContextQCFTCP.start();
            jmsContextQCFTCP.start();

            jmsContextQCFTCP.close();

        } catch (Exception e) {
            e.printStackTrace();
            testFailed = true;
        }

        if ( testFailed ) {
            throw new Exception("testStartJMSContextStartSecOffTCP failed");
        }
    }

    public void testStopJMSContextSecOffBinding(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        try {
            JMSContext jmsContextQCFBindings = qcfBindings.createContext();

            jmsContextQCFBindings.start();

            jmsContextQCFBindings.stop();
            jmsContextQCFBindings.close();

            testFailed = true;

        } catch ( JMSRuntimeException ex ) {
            // Expected
        }

        if ( testFailed ) {
            throw new Exception("testStopJMSContextSecOffBinding failed");
        }
    }

    public void testStopJMSContextSecOffTCP(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        try {
            JMSContext jmsContextQCFTCP = qcfTCP.createContext();

            jmsContextQCFTCP.start();

            jmsContextQCFTCP.stop();
            jmsContextQCFTCP.close();

            testFailed = true;

        } catch ( JMSRuntimeException ex ) {
            // Expected
        }

        if ( testFailed ) {
            throw new Exception("testStopJMSContextSecOffTCP failed");
        }
    }

    public void testCommitLocalTransaction_B(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        try {
            JMSContext jmsContextQCFBindings = qcfBindings.createContext(Session.SESSION_TRANSACTED);
            emptyQueue(qcfBindings, queue);

            Message message = jmsContextQCFBindings.createTextMessage("Hello World");

            JMSProducer jmsProducerQCFBindings = jmsContextQCFBindings.createProducer();
            jmsProducerQCFBindings.send(queue, message);

            QueueBrowser qb = jmsContextQCFBindings.createBrowser(queue);
            Enumeration e = qb.getEnumeration();
            int numMsgs = 0;
            while ( e.hasMoreElements() ) {
                TextMessage message1 = (TextMessage) e.nextElement();
                numMsgs++;
            }

            jmsContextQCFBindings.commit();
            QueueBrowser qb1 = jmsContextQCFBindings.createBrowser(queue);
            Enumeration e1 = qb1.getEnumeration();
            int numMsgs1 = 0;
            while ( e1.hasMoreElements() ) {
                TextMessage message1 = (TextMessage) e1.nextElement();
                numMsgs1++;
            }

            JMSConsumer jmsConsumerQCFBindings = jmsContextQCFBindings.createConsumer(queue);
            TextMessage rmsg = (TextMessage) jmsConsumerQCFBindings.receive(30000);

            jmsContextQCFBindings.commit();

            jmsConsumerQCFBindings.close();
            jmsContextQCFBindings.close();

        } catch ( Exception e ) {
            e.printStackTrace();
            testFailed = true;
        }

        if ( testFailed ) {
            throw new Exception("testCommitLocalTransaction_B failed");
        }
    }

    public void testCommitLocalTransaction_TCP(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        try {
            JMSContext jmsContextQCFTCP = qcfTCP.createContext(Session.SESSION_TRANSACTED);
            emptyQueue(qcfTCP, queue);

            Message message = jmsContextQCFTCP.createTextMessage("Hello World");
            JMSProducer jmsProducerQCFTCP = jmsContextQCFTCP.createProducer();
            jmsProducerQCFTCP.send(queue, message);

            QueueBrowser qb = jmsContextQCFTCP.createBrowser(queue);
            Enumeration e = qb.getEnumeration();
            int numMsgs = 0;
            while ( e.hasMoreElements() ) {
                TextMessage message1 = (TextMessage) e.nextElement();
                numMsgs++;
            }

            jmsContextQCFTCP.commit();

            QueueBrowser qb1 = jmsContextQCFTCP.createBrowser(queue);
            Enumeration e1 = qb1.getEnumeration();
            int numMsgs1 = 0;
            while ( e1.hasMoreElements() ) {
                TextMessage message1 = (TextMessage) e1.nextElement();
                numMsgs1++;
            }

            JMSConsumer jmsConsumerQCFTCP = jmsContextQCFTCP.createConsumer(queue);
            jmsConsumerQCFTCP.receive(30000);

            jmsContextQCFTCP.commit();

            jmsConsumerQCFTCP.close();
            jmsContextQCFTCP.close();

        } catch (Exception e) {
            e.printStackTrace();
            testFailed = true;
        }

        if ( testFailed ) {
            throw new Exception("testCommitLocalTransaction_TCP failed");
        }
    }

    public void testCommitNonLocalTransaction_B(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        try {
            JMSContext jmsContextQCFBindings = qcfBindings.createContext();
            emptyQueue(qcfBindings, queue);

            Message message = jmsContextQCFBindings.createTextMessage("Hello World");

            JMSProducer jmsProducerQCFBindings = jmsContextQCFBindings.createProducer();
            jmsProducerQCFBindings.send(queue, message);

            QueueBrowser qb = jmsContextQCFBindings.createBrowser(queue);
            Enumeration e = qb.getEnumeration();
            int numMsgs = 0;
            while ( e.hasMoreElements() ) {
                TextMessage message1 = (TextMessage) e.nextElement();
                numMsgs++;
            }

            jmsContextQCFBindings.commit();

            testFailed = true;

        } catch ( JMSRuntimeException ex ) {
            // Expected
        }

        if ( testFailed ) {
            throw new Exception("testCommitNonLocalTransaction_B failed");
        }
    }

    public void testCommitNonLocalTransaction_TCP(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        try {
            JMSContext jmsContextQCFTCP = qcfTCP.createContext();
            emptyQueue(qcfTCP, queue);

            Message message = jmsContextQCFTCP.createTextMessage("Hello World");

            JMSProducer jmsProducerQCFTCP = jmsContextQCFTCP.createProducer();
            jmsProducerQCFTCP.send(queue, message);

            QueueBrowser qb = jmsContextQCFTCP.createBrowser(queue);
            Enumeration e = qb.getEnumeration();
            int numMsgs = 0;
            while ( e.hasMoreElements() ) {
                TextMessage message1 = (TextMessage) e.nextElement();
                numMsgs++;
            }

            jmsContextQCFTCP.commit();

            QueueBrowser qb1 = jmsContextQCFTCP.createBrowser(queue);
            Enumeration e1 = qb1.getEnumeration();
            int numMsgs1 = 0;
            while ( e1.hasMoreElements() ) {
                TextMessage message1 = (TextMessage) e1.nextElement();
                numMsgs1++;
            }

            jmsContextQCFTCP.close();

            testFailed = true;

        } catch ( JMSRuntimeException ex ) {
            // Expected
        }

        if ( testFailed ) {
            throw new Exception("testCommitNonLocalTransaction_B failed");
        }
    }

    public void testRollbackLocalTransaction_B(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        try {
            JMSContext jmsContextQCFBindings = qcfBindings.createContext(Session.SESSION_TRANSACTED);
            emptyQueue(qcfBindings, queue);

            Message message = jmsContextQCFBindings.createTextMessage("Hello World");

            JMSProducer jmsProducerQCFBindings = jmsContextQCFBindings.createProducer();
            jmsProducerQCFBindings.send(queue, message);

            jmsContextQCFBindings.commit();

            QueueBrowser qb = jmsContextQCFBindings.createBrowser(queue);
            Enumeration e = qb.getEnumeration();
            int numMsgs = 0;
            while ( e.hasMoreElements() ) {
                TextMessage message1 = (TextMessage) e.nextElement();
                numMsgs++;
            }

            JMSConsumer jmsConsumerQCFBindings = jmsContextQCFBindings.createConsumer(queue);
            TextMessage rmsg = (TextMessage) jmsConsumerQCFBindings.receive(30000);

            QueueBrowser qb1 = jmsContextQCFBindings.createBrowser(queue);
            Enumeration e1 = qb1.getEnumeration();
            int numMsgs1 = 0;
            while ( e1.hasMoreElements() ) {
                TextMessage message1 = (TextMessage) e1.nextElement();
                numMsgs1++;
            }

            jmsContextQCFBindings.rollback();

            QueueBrowser qb2 = jmsContextQCFBindings.createBrowser(queue);
            Enumeration e2 = qb2.getEnumeration();
            int numMsgs2 = 0;
            while ( e2.hasMoreElements() ) {
                TextMessage message1 = (TextMessage) e2.nextElement();
                numMsgs2++;
            }

            jmsConsumerQCFBindings.close();
            jmsContextQCFBindings.close();

        } catch ( Exception e ) {
            e.printStackTrace();
            testFailed = true;
        }

        if ( testFailed ) {
            throw new Exception("testRollbackLocalTransaction_B failed");
        }
    }

    public void testRollbackLocalTransaction_TCP(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        try {
            JMSContext jmsContextQCFTCP = qcfTCP.createContext(Session.SESSION_TRANSACTED);
            emptyQueue(qcfTCP, queue);

            Message message = jmsContextQCFTCP.createTextMessage("Hello World");

            JMSProducer jmsProducerQCFTCP = jmsContextQCFTCP.createProducer();
            jmsProducerQCFTCP.send(queue, message);
            jmsContextQCFTCP.commit();

            QueueBrowser qb = jmsContextQCFTCP.createBrowser(queue);
            Enumeration e = qb.getEnumeration();
            int numMsgs = 0;
            while ( e.hasMoreElements() ) {
                TextMessage message1 = (TextMessage) e.nextElement();
                numMsgs++;
            }

            JMSConsumer jmsConsumerQCFTCP = jmsContextQCFTCP.createConsumer(queue);
            TextMessage rmsg = (TextMessage) jmsConsumerQCFTCP.receive(30000);

            QueueBrowser qb1 = jmsContextQCFTCP.createBrowser(queue);
            Enumeration e1 = qb1.getEnumeration();
            int numMsgs1 = 0;
            while ( e1.hasMoreElements() ) {
                TextMessage message1 = (TextMessage) e1.nextElement();
                numMsgs1++;
            }

            jmsContextQCFTCP.rollback();

            QueueBrowser qb2 = jmsContextQCFTCP.createBrowser(queue);
            Enumeration e2 = qb2.getEnumeration();
            int numMsgs2 = 0;
            while ( e2.hasMoreElements() ) {
                TextMessage message1 = (TextMessage) e2.nextElement();
                numMsgs2++;
            }

            jmsConsumerQCFTCP.close();
            jmsContextQCFTCP.close();

        } catch ( Exception e ) {
            e.printStackTrace();
            testFailed = true;
        }

        if ( testFailed ) {
            throw new Exception("testRollbackLocalTransaction_TCP failed");
        }
    }

    public void testRollbackNonLocalTransaction_B(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        try {
            JMSContext jmsContextQCFBindings = qcfBindings.createContext();
            emptyQueue(qcfBindings, queue);

            Message message = jmsContextQCFBindings.createTextMessage("Hello World");

            JMSProducer jmsProducerQCFBindings = jmsContextQCFBindings.createProducer();

            jmsProducerQCFBindings.send(queue, message);
            jmsContextQCFBindings.rollback();

            jmsContextQCFBindings.close();

            testFailed = true;

        } catch ( JMSRuntimeException ex ) {
            // Expected
        }

        if ( testFailed ) {
            throw new Exception("testRollbackNonLocalTransaction_B failed");
        }
    }

    public void testRollbackNonLocalTransaction_TCP(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        try {
            JMSContext jmsContextQCFTCP = qcfTCP.createContext();
            emptyQueue(qcfTCP, queue);

            Message message = jmsContextQCFTCP.createTextMessage("Hello World");

            JMSProducer jmsProducerQCFTCP = jmsContextQCFTCP.createProducer();
            jmsProducerQCFTCP.send(queue, message);

            jmsContextQCFTCP.rollback();
            jmsContextQCFTCP.close();

            testFailed = true;

        } catch ( JMSRuntimeException ex ) {
            // Expected
        }

        if ( testFailed ) {
            throw new Exception("testRollbackNonLocalTransaction_TCP failed");
        }
    }

    public void testRecoverNonLocalTransaction_B(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        try {
            JMSContext jmsContextQCFBindings = qcfBindings.createContext(Session.SESSION_TRANSACTED);
            emptyQueue(qcfBindings, queue);

            Message message = jmsContextQCFBindings.createTextMessage("Hello World");

            JMSProducer jmsProducerQCFBindings = jmsContextQCFBindings.createProducer();
            jmsProducerQCFBindings.send(queue, message);
            jmsContextQCFBindings.recover();

            jmsContextQCFBindings.close();

            testFailed = true;

        } catch ( JMSRuntimeException ex ) {
            // Expected
        }

        if ( testFailed ) {
            throw new Exception("testRecoverNonLocalTransaction_B failed");
        }
    }

    public void testRecoverNonLocalTransaction_TCP(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        try {
            JMSContext jmsContextQCFTCP = qcfTCP.createContext(Session.SESSION_TRANSACTED);
            emptyQueue(qcfTCP, queue);

            Message message = jmsContextQCFTCP.createTextMessage("Hello World");

            JMSProducer jmsProducerQCFTCP = jmsContextQCFTCP.createProducer();

            jmsProducerQCFTCP.send(queue, message);

            jmsContextQCFTCP.recover();

            jmsContextQCFTCP.close();

            testFailed = true;

        } catch ( JMSRuntimeException ex ) {
            // Expected
        }

        if ( testFailed ) {
            throw new Exception("testRecoverNonLocalTransaction_TCP failed");
        }
    }

    public void testCreateTemporaryQueueSecOffBinding(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContextQCFBindings = qcfBindings.createContext();

        TemporaryQueue tempQ = jmsContextQCFBindings.createTemporaryQueue();

        if ( tempQ == null ) {
            testFailed = true;
        }

        if ( testFailed ) {
            throw new Exception("testCreateTemporaryQueueSecOffBinding failed");
        }

        jmsContextQCFBindings.close();
    }

    public void testCreateTemporaryQueueSecOffTCPIP(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContextQCFTCP = qcfTCP.createContext();
        TemporaryQueue tempQ = jmsContextQCFTCP.createTemporaryQueue();

        if ( tempQ == null ) {
            testFailed = true;
        }

        jmsContextQCFTCP.close();

        if ( testFailed ) {
            throw new Exception("testCreateTemporaryQueueSecOffTCPIP failed");
        }
    }

    public void testTemporaryQueueLifetimeSecOff_B(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        try {
            JMSContext jmsContextQCFBindings = qcfBindings.createContext();
            TemporaryQueue tempQ = jmsContextQCFBindings.createTemporaryQueue();

            jmsContextQCFBindings.close();
            jmsContextQCFBindings.createBrowser(tempQ);

            testFailed = true;

        } catch ( JMSRuntimeException ex ) {
            // Expected
        }

        if ( testFailed ) {
            throw new Exception("testTemporaryQueueLifetimeSecOff_B failed");
        }
    }

    public void testTemporaryQueueLifetimeSecOff_TCPIP(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        try {
            JMSContext jmsContextQCFTCP = qcfTCP.createContext();
            TemporaryQueue tempQ = jmsContextQCFTCP.createTemporaryQueue();

            jmsContextQCFTCP.close();
            jmsContextQCFTCP.createBrowser(tempQ);

            testFailed = true;

        } catch ( JMSRuntimeException ex ) {
            // Expected
        }

        if ( testFailed ) {
            throw new Exception("testTemporaryQueueLifetimeSecOff_TCPIP failed");
        }
    }

    public void testgetTemporaryQueueNameSecOffBinding(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContextQCFBindings = qcfBindings.createContext();

        TemporaryQueue tempQ = jmsContextQCFBindings.createTemporaryQueue();

        if ( (tempQ == null) || (tempQ.getQueueName() == null) ) {
            testFailed = true;
        }

        jmsContextQCFBindings.close();

        if ( testFailed ) {
            throw new Exception("testgetTemporaryQueueNameSecOffBinding failed");
        }
    }

    public void testgetTemporaryQueueNameSecOffTCPIP(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContextQCFTCP = qcfTCP.createContext();
        TemporaryQueue tempQ = jmsContextQCFTCP.createTemporaryQueue();

        if ( (tempQ == null) || (tempQ.getQueueName() == null) ) {
            testFailed = true;
        }

        jmsContextQCFTCP.close();

        if ( testFailed ) {
            throw new Exception("testgetTemporaryQueueNameSecOffTCPIP failed");
        }
    }

    public void testToStringTemporaryQueueNameSecOffBinding(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContextQCFBindings = qcfBindings.createContext();
        TemporaryQueue tempQ = jmsContextQCFBindings.createTemporaryQueue();

        if ( (tempQ == null) || (tempQ.toString() == null ) ) {
            testFailed = true;
        }

        jmsContextQCFBindings.close();

        if ( testFailed ) {
            throw new Exception("testToStringTemporaryQueueNameSecOffBinding failed");
        }
    }

    public void testToStringTemporaryQueueNameSecOffTCPIP(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContextQCFTCP = qcfTCP.createContext();

        TemporaryQueue tempQ = jmsContextQCFTCP.createTemporaryQueue();

        if ( (tempQ == null) || (tempQ.toString() == null) ) {
            testFailed = true;
        }

        jmsContextQCFTCP.close();

        if ( testFailed ) {
            throw new Exception("testToStringTemporaryQueueNameSecOffTCPIP failed");
        }
    }

    public void testDeleteTemporaryQueueNameSecOffBinding(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        try {
            JMSContext jmsContextQCFBindings = qcfBindings.createContext();

            TemporaryQueue tempQ = jmsContextQCFBindings.createTemporaryQueue();
            tempQ.delete();

            jmsContextQCFBindings.createBrowser(tempQ);
            jmsContextQCFBindings.close();

            testFailed = true;

        } catch ( JMSRuntimeException ex ) {
            // Expected
        }

        if ( testFailed ) {
            throw new Exception("testDeleteTemporaryQueueNameSecOffBinding failed");
        }
    }

    public void testDeleteTemporaryQueueNameSecOffTCPIP(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        try {
            JMSContext jmsContextQCFTCP = qcfTCP.createContext();

            TemporaryQueue tempQ = jmsContextQCFTCP.createTemporaryQueue();
            tempQ.delete();

            jmsContextQCFTCP.createBrowser(tempQ);
            jmsContextQCFTCP.close();

            testFailed = true;

        } catch ( JMSRuntimeException ex ) {
            // Expected
        }

        if ( testFailed ) {
            throw new Exception("testDeleteTemporaryQueueNameSecOffTCPIP failed");
        }
    }

    public void testDeleteExceptionTemporaryQueueNameSecOFF_B(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        try {
            JMSContext jmsContextQCFBindings = qcfBindings.createContext();

            TemporaryQueue tempQ = jmsContextQCFBindings.createTemporaryQueue();
            tempQ.delete();

            jmsContextQCFBindings.createBrowser(tempQ);
            jmsContextQCFBindings.close();

            testFailed = true;

        } catch ( JMSRuntimeException ex ) {
            // Expected
        }

        if ( testFailed ) {
            throw new Exception("testDeleteExceptionTemporaryQueueNameSecOFF_B failed");
        }
    }

    public void testDeleteExceptionTemporaryQueueNameSecOFF_TCPIP(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        try {
            JMSContext jmsContextQCFTCP = qcfTCP.createContext();

            TemporaryQueue tempQ = jmsContextQCFTCP.createTemporaryQueue();
            tempQ.delete();

            jmsContextQCFTCP.createBrowser(tempQ);
            jmsContextQCFTCP.close();

            testFailed = true;

        } catch ( JMSRuntimeException ex ) {
            // Expected
        }

        if ( testFailed ) {
            throw new Exception("testDeleteExceptionTemporaryQueueNameSecOFF_TCPIP failed");
        }
    }

    public void testPTPTemporaryQueue_Binding(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContextQCFBindings = qcfBindings.createContext();

        TemporaryQueue tempQ = jmsContextQCFBindings.createTemporaryQueue();

        JMSProducer jmsProducerQCFBindings = jmsContextQCFBindings.createProducer();
        JMSConsumer jmsConsumerQCFBindings = jmsContextQCFBindings.createConsumer(tempQ);

        jmsProducerQCFBindings.send(tempQ, "hello world");
        TextMessage recMessage = (TextMessage) jmsConsumerQCFBindings.receive(30000);

        if ( (recMessage == null) ||
             (recMessage.getText() == null) ||
             !recMessage.getText().equals("hello world") ) {
            testFailed = true;
        }

        jmsConsumerQCFBindings.close();
        jmsContextQCFBindings.close();

        if ( testFailed ) {
            throw new Exception("testPTPTemporaryQueue_Binding failed");
        }
    }

    public void testPTPTemporaryQueue_TCP(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContextQCFTCP = qcfTCP.createContext();

        TemporaryQueue tempQ = jmsContextQCFTCP.createTemporaryQueue();

        JMSProducer jmsProducerQCFTCP = jmsContextQCFTCP.createProducer();
        JMSConsumer jmsConsumerQCFTCP = jmsContextQCFTCP.createConsumer(tempQ);

        jmsProducerQCFTCP.send(tempQ, "hello world");
        TextMessage recMessage = (TextMessage) jmsConsumerQCFTCP.receive(30000);

        if ( (recMessage == null) ||
             (recMessage.getText() == null) ||
             !recMessage.getText().equalsIgnoreCase("hello world") ) {
            testFailed = true;
        }

        jmsConsumerQCFTCP.close();
        jmsContextQCFTCP.close();

        if ( testFailed ) {
            throw new Exception("testPTPTemporaryQueue_TCP failed");
        }
    }

    public void testCreateTemporaryTopicSecOffBinding(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContextTCFBindings = tcfBindings.createContext();
        TemporaryTopic tempT = jmsContextTCFBindings.createTemporaryTopic();

        if ( tempT == null ) {
            testFailed = true;
        }

        if ( testFailed ) {
            throw new Exception("testCreateTemporaryTopicSecOffBinding failed");
        }

        jmsContextTCFBindings.close();
    }

    public void testCreateTemporaryTopicSecOffTCPIP(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContextTCFTCP = tcfTCP.createContext();
        TemporaryTopic tempT = jmsContextTCFTCP.createTemporaryTopic();

        if ( tempT == null ) {
            testFailed = true;
        }

        jmsContextTCFTCP.close();

        if ( testFailed ) {
            throw new Exception("testCreateTemporaryTopicSecOffTCPIP failed");
        }
    }

    public void testTemporaryTopicLifetimeSecOff_B(
        HttpServletRequest request, HttpServletResponse response) throws Exception {
        boolean testFailed = false;

        try {
            JMSContext jmsContextTCFBindings = tcfBindings.createContext();
            TemporaryTopic tempT = jmsContextTCFBindings.createTemporaryTopic();
            jmsContextTCFBindings.close();
            tempT.delete();

        } catch ( JMSRuntimeException ex ) {
            ex.printStackTrace();
            testFailed = true;
        }

        if ( testFailed ) {
            throw new Exception("testTemporaryTopicLifetimeSecOff_B failed");
        }
    }

    public void testTemporaryTopicLifetimeSecOff_TCPIP(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        try {
            JMSContext jmsContextTCFTCP = tcfTCP.createContext();
            TemporaryTopic tempT = jmsContextTCFTCP.createTemporaryTopic();
            jmsContextTCFTCP.close();
            tempT.delete();

        } catch ( JMSRuntimeException ex ) {
            ex.printStackTrace();
            testFailed = true;
        }

        if ( testFailed ) {
            throw new Exception("testTemporaryTopicLifetimeSecOff_TCPIP failed");
        }
    }

    public void testGetTemporaryTopicSecOffBinding(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContextTCFBindings = tcfBindings.createContext();
        TemporaryTopic tempT = jmsContextTCFBindings.createTemporaryTopic();

        if ( (tempT == null) || (tempT.getTopicName() == null) ) {
            testFailed = true;
        }

        jmsContextTCFBindings.close();

        if ( testFailed ) {
            throw new Exception("testGetTemporaryTopicSecOffBinding failed");
        }
    }

    public void testGetTemporaryTopicSecOffTCPIP(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContextTCFTCP = tcfTCP.createContext();
        TemporaryTopic tempT = jmsContextTCFTCP.createTemporaryTopic();

        if ( (tempT == null) || (tempT.getTopicName() == null) ) {
            testFailed = true;
        }

        jmsContextTCFTCP.close();

        if ( testFailed ) {
            throw new Exception("testGetTemporaryTopicSecOffTCPIP failed");
        }
    }

    public void testToStringTemporaryTopicSecOffBinding(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContextTCFBindings = tcfBindings.createContext();
        TemporaryTopic tempT = jmsContextTCFBindings.createTemporaryTopic();

        if ( (tempT == null) || (tempT.toString() == null) ) {
            testFailed = true;
        }

        if ( testFailed ) {
            throw new Exception("testToStringTemporaryTopicSecOffBinding failed");
        }

        jmsContextTCFBindings.close();
    }

    public void testToStringeTemporaryTopicSecOffTCPIP(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContextTCFTCP = tcfTCP.createContext();
        TemporaryTopic tempT = jmsContextTCFTCP.createTemporaryTopic();

        if ((tempT == null) || (tempT.toString() == null) ) {
            testFailed = true;
        }

        if ( testFailed ) {
            throw new Exception("testToStringeTemporaryTopicSecOffTCPIP failed");
        }

        jmsContextTCFTCP.close();
    }

    public void testDeleteTemporaryTopicSecOffBinding(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        try {
            JMSContext jmsContextTCFBindings = tcfBindings.createContext();
            TemporaryTopic tempT = jmsContextTCFBindings.createTemporaryTopic();
            jmsContextTCFBindings.close();
            tempT.delete();

        } catch ( Exception e ) {
            e.printStackTrace();
            testFailed = true;
        }

        if ( testFailed ) {
            throw new Exception("testDeleteTemporaryTopicSecOffBinding failed");
        }
    }

    public void testDeleteTemporaryTopicSecOffTCPIP(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        try {
            JMSContext jmsContextTCFTCP = tcfTCP.createContext();
            TemporaryTopic tempT = jmsContextTCFTCP.createTemporaryTopic();
            jmsContextTCFTCP.close();
            tempT.delete();

        } catch ( Exception e ) {
            e.printStackTrace();
            testFailed = true;
        }

        if ( testFailed ) {
            throw new Exception("testDeleteTemporaryTopicSecOffTCPIP failed");
        }
    }

    public void testDeleteExceptionTemporaryTopicSecOff_B(
         HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        try {
            JMSContext jmsContextTCFBindings = tcfBindings.createContext();
            TemporaryTopic tempT = jmsContextTCFBindings.createTemporaryTopic();
            jmsContextTCFBindings.createConsumer(tempT);
            tempT.delete();
            jmsContextTCFBindings.close();

            testFailed = true;

        } catch ( Exception e ) {
            // Expected
        }

        if ( testFailed ) {
            throw new Exception("testDeleteExceptionTemporaryTopicSecOff_B failed");
        }
    }

    public void testDeleteExceptionTemporaryTopicSecOff_TCPIP(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        try {
            JMSContext jmsContextTCFTCP = tcfTCP.createContext();
            TemporaryTopic tempT = jmsContextTCFTCP.createTemporaryTopic();
            jmsContextTCFTCP.createConsumer(tempT);
            tempT.delete();
            jmsContextTCFTCP.close();

            testFailed = true;

        } catch ( Exception e ) {
            // Expected
        }

        if ( testFailed ) {
            throw new Exception("testDeleteExceptionTemporaryTopicSecOff_TCPIP failed");
        }
    }

    public void testTemporaryTopicPubSubSecOff_B(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContextTCFBindings = tcfBindings.createContext();

        TemporaryTopic tempT = jmsContextTCFBindings.createTemporaryTopic();
        JMSProducer jmsProducerTCFBindings = jmsContextTCFBindings.createProducer();
        JMSConsumer jmsConsumerTCFBindings = jmsContextTCFBindings.createConsumer(tempT);

        jmsProducerTCFBindings.send(tempT, "hello world");
        TextMessage recMessage = (TextMessage) jmsConsumerTCFBindings.receive(30000);

        if ( (recMessage == null) ||
             (recMessage.getText() == null) ||
             !recMessage.getText().equals("hello world") ){
            testFailed = true;
        }

        jmsConsumerTCFBindings.close();
        jmsContextTCFBindings.close();

        if ( testFailed ) {
            throw new Exception("testTemporaryTopicPubSubSecOff_B failed");
        }
    }

    public void testTemporaryTopicPubSubSecOff_TCPIP(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean testFailed = false;

        JMSContext jmsContextTCFTCP = tcfTCP.createContext();

        TemporaryTopic tempT = jmsContextTCFTCP.createTemporaryTopic();
        JMSProducer jmsProducerTCFTCP = jmsContextTCFTCP.createProducer();
        JMSConsumer jmsConsumerTCFTCP = jmsContextTCFTCP.createConsumer(tempT);

        jmsProducerTCFTCP.send(tempT, "hello world");

        TextMessage recMessage = (TextMessage) jmsConsumerTCFTCP.receive(30000);

        if ( (recMessage == null) ||
             (recMessage.getText() == null) ||
             !recMessage.getText().equals("hello world") ) {
            testFailed = true;
        }

        jmsConsumerTCFTCP.close();
        jmsContextTCFTCP.close();

        if ( testFailed ) {
            throw new Exception("testTemporaryTopicPubSubSecOff_TCPIP failed");
        }
    }
}
