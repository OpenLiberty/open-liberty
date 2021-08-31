/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package web;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.Enumeration;

import javax.inject.Inject;
import javax.jms.IllegalStateRuntimeException;
import javax.jms.JMSConnectionFactory;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.JMSPasswordCredential;
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
public class JMSContextInjectServlet extends HttpServlet {

    public static QueueConnectionFactory jmsQCFBindings;
    public static QueueConnectionFactory jmsQCFTCP;
    public static Queue jmsQueue;
    public static Topic jmsTopic;
    public static boolean exceptionFlag;

    public static Queue queue10;

    @Inject
    @JMSConnectionFactory("java:comp/env/jndi_JMS_BASE_QCF")
    @JMSPasswordCredential(userName = "user1", password = "user1pwd")
    private JMSContext jmsContextQueue;

    @Inject
    @JMSConnectionFactory("java:comp/env/jndi_JMS_BASE_QCF1")
    @JMSPasswordCredential(userName = "user1", password = "user1pwd")
    private JMSContext jmsContextQueueTCP;

    @Inject
    @JMSConnectionFactory("java:comp/env/eis/tcf")
    @JMSPasswordCredential(userName = "user1", password = "user1pwd")
    private JMSContext jmsContextTopic;

    @Inject
    @JMSConnectionFactory("java:comp/env/eis/tcf1")
    @JMSPasswordCredential(userName = "user1", password = "user1pwd")
    private JMSContext jmsContextTopicTCP;

    @Inject
    @JMSConnectionFactory("java:comp/env/jndi_JMS_BASE_QCF")
    @JMSPasswordCredential(userName = "user1", password = "wrongpwd")
    private JMSContext jmsContextQueueWrongPasswd;

    @Inject
    @JMSConnectionFactory("java:comp/env/jndi_JMS_BASE_QCF1")
    @JMSPasswordCredential(userName = "user1", password = "wrongpwd")
    private JMSContext jmsContextQueueTCPWrongPasswd;

    @Inject
    @JMSConnectionFactory("java:comp/env/eis/tcf")
    @JMSPasswordCredential(userName = "user1", password = "wrongpwd")
    private JMSContext jmsContextTopicWrongPasswd;

    @Inject
    @JMSConnectionFactory("java:comp/env/eis/tcf1")
    @JMSPasswordCredential(userName = "user1", password = "wrongpwd")
    private JMSContext jmsContextTopicTCPWrongPasswd;

    @Override
    public void init() throws ServletException {
        // TODO Auto-generated method stub

        super.init();
        try {
            jmsQueue = getQueue();
            jmsTopic = getTopic();
            jmsQCFBindings = getQCF();

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
        final TraceComponent tc = Tr.register(JMSContextInjectServlet.class); // injection
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
            System.out.println(" Start: " + test);
            getClass().getMethod(test, HttpServletRequest.class,
                                 HttpServletResponse.class).invoke(this, request, response);
            out.println(test + " COMPLETED SUCCESSFULLY");
            System.out.println(" End: " + test);
            Tr.exit(this, tc, test);
        } catch (Throwable x) {
            if (x instanceof InvocationTargetException)
                x = x.getCause();
            Tr.exit(this, tc, test, x);
            out.println("<pre>ERROR in " + test + ":");
            System.out.println(" Error: " + test);
            x.printStackTrace(out);
            out.println("</pre>");
        }
    }

    public void testP2P_B_SecOn(HttpServletRequest request,
                                HttpServletResponse response) throws Throwable {

        exceptionFlag = false;

        emptyQueue(jmsContextQueue, jmsQueue);
        TextMessage message = jmsContextQueue.createTextMessage("testP2P_B_SecOn");
        jmsContextQueue.createProducer().send(jmsQueue, message);
        JMSConsumer jmsConsumer = jmsContextQueue.createConsumer(jmsQueue);
        TextMessage message1 = (TextMessage) jmsConsumer.receive(500);
        System.out.println("Received message: " + message1.getText());

        if (!(message1 != null && message1.getText().equals("testP2P_B_SecOn")))
            exceptionFlag = true;

        if (exceptionFlag)
            throw new WrongException("testP2P_B_SecOn failed: Expected message was not received");

    }

    public void testP2P_TCP_SecOn(HttpServletRequest request,
                                  HttpServletResponse response) throws Throwable {

        exceptionFlag = false;

        emptyQueue(jmsContextQueueTCP, jmsQueue);
        TextMessage message = jmsContextQueueTCP.createTextMessage("testP2P_TCP_SecOn");
        jmsContextQueueTCP.createProducer().send(jmsQueue, message);
        JMSConsumer jmsConsumer = jmsContextQueueTCP.createConsumer(jmsQueue);
        TextMessage message1 = (TextMessage) jmsConsumer.receive(500);
        System.out.println("Received message: " + message1.getText());

        if (!(message1 != null && message1.getText().equals("testP2P_TCP_SecOn")))
            exceptionFlag = true;

        if (exceptionFlag)
            throw new WrongException("testP2P_TCP_SecOn failed: Expected message was not received");

    }

    public void testPubSub_B_SecOn(HttpServletRequest request,
                                   HttpServletResponse response) throws Throwable {

        exceptionFlag = false;

        TextMessage message = jmsContextTopic.createTextMessage("testPubSub_B_SecOn");
        JMSConsumer jmsConsumer = jmsContextTopic.createConsumer(jmsTopic);
        jmsContextTopic.createProducer().send(jmsTopic, message);

        TextMessage message1 = (TextMessage) jmsConsumer.receive(500);
        System.out.println("Received message: " + message1.getText());

        if (!(message1 != null && message1.getText().equals(
                                                            "testPubSub_B_SecOn")))
            exceptionFlag = true;

        if (exceptionFlag)
            throw new WrongException("testPubSub_B_SecOn failed: Expected message was not received");

    }

    public void testPubSub_TCP_SecOn(HttpServletRequest request,
                                     HttpServletResponse response) throws Throwable {

        exceptionFlag = false;

        TextMessage message = jmsContextTopicTCP.createTextMessage("testPubSub_TCP_SecOn");
        JMSConsumer jmsConsumer = jmsContextTopicTCP.createConsumer(jmsTopic);
        jmsContextTopicTCP.createProducer().send(jmsTopic, message);

        TextMessage message1 = (TextMessage) jmsConsumer.receive(500);
        System.out.println("Received message: " + message1.getText());

        if (!(message1 != null && message1.getText().equals(
                                                            "testPubSub_TCP_SecOn")))
            exceptionFlag = true;

        if (exceptionFlag)
            throw new WrongException("testPubSub_TCP_SecOn failed: Expected message was not received");

    }

    public void testPubSubDurable_B_SecOn(HttpServletRequest request,
                                          HttpServletResponse response) throws Throwable {

        exceptionFlag = false;

        TextMessage message = jmsContextTopic.createTextMessage("testPubSubDurable_B_SecOn");
        JMSConsumer jmsConsumer = jmsContextTopic.createDurableConsumer(
                                                                        jmsTopic, "sub1");
        jmsContextTopic.createProducer().send(jmsTopic, message);

        TextMessage message1 = (TextMessage) jmsConsumer.receive(500);
        System.out.println("Received message: " + message1.getText());

        if (!(message1 != null && message1.getText().equals(
                                                            "testPubSubDurable_B_SecOn")))
            exceptionFlag = true;

        if (exceptionFlag)
            throw new WrongException("testPubSubDurable_B_SecOn failed: Expected message was not received");

    }

    public void testPubSubDurable_TCP_SecOn(HttpServletRequest request,
                                            HttpServletResponse response) throws Throwable {

        exceptionFlag = false;

        TextMessage message = jmsContextTopicTCP.createTextMessage("testPubSubDurable_TCP_SecOn");
        JMSConsumer jmsConsumer = jmsContextTopicTCP.createDurableConsumer(
                                                                           jmsTopic, "sub2");
        jmsContextTopicTCP.createProducer().send(jmsTopic, message);

        TextMessage message1 = (TextMessage) jmsConsumer.receive(500);
        System.out.println("Received message: " + message1.getText());

        if (!(message1 != null && message1.getText().equals(
                                                            "testPubSubDurable_TCP_SecOn")))
            exceptionFlag = true;

        if (exceptionFlag)
            throw new WrongException("testPubSubDurable_TCP_SecOn failed: Expected message was not received");

    }

    public void testNegativeSetters_B_SecOn(HttpServletRequest request,
                                            HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        boolean ex1 = false;
        boolean ex2 = false;
        boolean ex3 = false;
        boolean ex4 = false;
        boolean ex5 = false;
        boolean ex6 = false;
        boolean ex7 = false;
        boolean ex8 = false;
        boolean ex9 = false;
        boolean ex10 = false;

        try {
            jmsContextTopic.setClientID("cid");
        } catch (IllegalStateRuntimeException e) {
            ex1 = true;
            System.out.println("Expected exception seen on calling setClientID method");
        }

        try {
            jmsContextTopic.setExceptionListener(null);
        } catch (IllegalStateRuntimeException e) {
            ex2 = true;
            System.out.println("Expected exception seen on calling setExceptionListener method");
        }

        try {
            jmsContextTopic.stop();
        } catch (IllegalStateRuntimeException e) {
            ex3 = true;
            System.out.println("Expected exception seen on calling stop method");
        }

        try {
            jmsContextTopic.acknowledge();
        } catch (IllegalStateRuntimeException e) {
            ex4 = true;
            System.out.println("Expected exception seen on calling acknowledge method");
        }

        try {
            jmsContextTopic.commit();
        } catch (IllegalStateRuntimeException e) {
            ex5 = true;
            System.out.println("Expected exception seen on calling commit method");
        }

        try {
            jmsContextTopic.rollback();
        } catch (IllegalStateRuntimeException e) {
            ex6 = true;
            System.out.println("Expected exception seen on calling rollback method");
        }

        try {
            jmsContextTopic.recover();
        } catch (IllegalStateRuntimeException e) {
            ex7 = true;
            System.out.println("Expected exception seen on calling recover method");
        }

        try {
            jmsContextTopic.setAutoStart(true);
        } catch (IllegalStateRuntimeException e) {
            ex8 = true;
            System.out.println("Expected exception seen on calling setAutoStart method");
        }

        try {
            jmsContextTopic.start();
        } catch (IllegalStateRuntimeException e) {
            ex9 = true;
            System.out.println("Expected exception seen on calling start method");
        }

        try {
            jmsContextTopic.close();
        } catch (IllegalStateRuntimeException e) {
            ex10 = true;
            System.out.println("Expected exception seen on calling close method");
        }

        if (!(ex1 && ex2 && ex3 && ex4 && ex5 && ex6 && ex7 && ex8 && ex9 && ex10))
            exceptionFlag = true;

        if (exceptionFlag)
            throw new WrongException("testNegativeSetters_B_SecOn failed: Expected exception was not seen");

    }

    public void testNegativeSetters_TCP_SecOn(HttpServletRequest request,
                                              HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        boolean ex1 = false;
        boolean ex2 = false;
        boolean ex3 = false;
        boolean ex4 = false;
        boolean ex5 = false;
        boolean ex6 = false;
        boolean ex7 = false;
        boolean ex8 = false;
        boolean ex9 = false;
        boolean ex10 = false;

        try {
            jmsContextTopicTCP.setClientID("cid");
        } catch (IllegalStateRuntimeException e) {
            ex1 = true;
            System.out.println("Expected exception seen on calling setClientID method");
        }

        try {
            jmsContextTopicTCP.setExceptionListener(null);
        } catch (IllegalStateRuntimeException e) {
            ex2 = true;
            System.out.println("Expected exception seen on calling setExceptionListener method");
        }

        try {
            jmsContextTopicTCP.stop();
        } catch (IllegalStateRuntimeException e) {
            ex3 = true;
            System.out.println("Expected exception seen on calling stop method");
        }

        try {
            jmsContextTopicTCP.acknowledge();
        } catch (IllegalStateRuntimeException e) {
            ex4 = true;
            System.out.println("Expected exception seen on calling acknowledge method");
        }

        try {
            jmsContextTopicTCP.commit();
        } catch (IllegalStateRuntimeException e) {
            ex5 = true;
            System.out.println("Expected exception seen on calling commit method");
        }

        try {
            jmsContextTopicTCP.rollback();
        } catch (IllegalStateRuntimeException e) {
            ex6 = true;
            System.out.println("Expected exception seen on calling rollback method");
        }

        try {
            jmsContextTopicTCP.recover();
        } catch (IllegalStateRuntimeException e) {
            ex7 = true;
            System.out.println("Expected exception seen on calling recover method");
        }

        try {
            jmsContextTopicTCP.setAutoStart(true);
        } catch (IllegalStateRuntimeException e) {
            ex8 = true;
            System.out.println("Expected exception seen on calling setAutoStart method");
        }

        try {
            jmsContextTopicTCP.start();
        } catch (IllegalStateRuntimeException e) {
            ex9 = true;
            System.out.println("Expected exception seen on calling start method");
        }

        try {
            jmsContextTopicTCP.close();
        } catch (IllegalStateRuntimeException e) {
            ex10 = true;
            System.out.println("Expected exception seen on calling close method");
        }

        if (!(ex1 && ex2 && ex3 && ex4 && ex5 && ex6 && ex7 && ex8 && ex9 && ex10))
            exceptionFlag = true;

        if (exceptionFlag)
            throw new WrongException("testNegativeSetters_TCP_SecOn failed: Expected exception was not seen");

    }

    public void testWrongUserCredentialsQueue_B_SecOn(
                                                      HttpServletRequest request, HttpServletResponse response) throws Throwable {

        try {
            jmsContextQueueWrongPasswd.createConsumer(jmsQueue);

        } catch (java.lang.RuntimeException e) {
            Throwable causeEx = e.getCause();
            String actualException = causeEx.getClass().getName();

            System.out.println("Exception cause is " + actualException);
            if (!(actualException.equals("javax.jms.JMSSecurityRuntimeException")))
                exceptionFlag = true;
            e.printStackTrace();
        }

        if (exceptionFlag)
            throw new WrongException("testWrongUserCredentialsQueue_B_SecOn failed: Expected exception was not seen");

    }

    public void testWrongUserCredentialsQueue_TCP_SecOn(
                                                        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        try {
            jmsContextQueueTCPWrongPasswd.createConsumer(jmsQueue);

        } catch (java.lang.RuntimeException e) {
            Throwable causeEx = e.getCause();
            String actualException = causeEx.getClass().getName();

            System.out.println("Exception cause is " + actualException);
            if (!(actualException.equals("javax.jms.JMSSecurityRuntimeException")))
                exceptionFlag = true;
            e.printStackTrace();
        }

        if (exceptionFlag)
            throw new WrongException("testWrongUserCredentialsQueue_TCP_SecOn failed: Expected exception was not seen");

    }

    public void testWrongUserCredentialsTopic_B_SecOn(
                                                      HttpServletRequest request, HttpServletResponse response) throws Throwable {

        try {
            jmsContextTopicWrongPasswd.createConsumer(jmsTopic);

        } catch (java.lang.RuntimeException e) {
            Throwable causeEx = e.getCause();
            String actualException = causeEx.getClass().getName();

            System.out.println("Exception cause is " + actualException);
            if (!(actualException.equals("javax.jms.JMSSecurityRuntimeException")))
                exceptionFlag = true;
            e.printStackTrace();
        }

        if (exceptionFlag)
            throw new WrongException("testWrongUserCredentialsTopic_B_SecOn failed: Expected exception was not seen");

    }

    public void testWrongUserCredentialsTopic_TCP_SecOn(
                                                        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        try {
            jmsContextTopicTCPWrongPasswd.createConsumer(jmsTopic);

        } catch (java.lang.RuntimeException e) {
            Throwable causeEx = e.getCause();
            String actualException = causeEx.getClass().getName();

            System.out.println("Exception cause is " + actualException);
            if (!(actualException.equals("javax.jms.JMSSecurityRuntimeException")))
                exceptionFlag = true;
            e.printStackTrace();
        }

        if (exceptionFlag)
            throw new WrongException("testWrongUserCredentialsTopic_TCP_SecOn failed: Expected exception was not seen");

    }

    public void testQueueMDB(HttpServletRequest request,
                             HttpServletResponse response) throws Throwable {

        exceptionFlag = false;

        JMSContext context = jmsQCFBindings.createContext();
        Queue queue10 = (Queue) new InitialContext().lookup("java:comp/env/queue/test");
        emptyQueue(context, queue10);
        TextMessage message = context.createTextMessage("testQueueMDB");
        context.createProducer().send(queue10, message);
        System.out.println("Sent Message: " + message.getText());
        context.close();

    }

    public QueueConnectionFactory getQCF() throws NamingException {

        QueueConnectionFactory qcf = (QueueConnectionFactory) new InitialContext().lookup("java:comp/env/jndi_JMS_BASE_QCF");

        return qcf;
    }

    public Queue getQueue() throws NamingException {

        Queue queue = (Queue) new InitialContext().lookup("java:comp/env/jndi_INPUT_Q");

        return queue;
    }

    public Topic getTopic() throws NamingException {

        Topic topic = (Topic) new InitialContext().lookup("java:comp/env/eis/topic1");
        System.out.println("Returning topic object: " + topic.toString());
        return topic;
    }

    public class WrongException extends Exception {
        String str;

        public WrongException(String str) {
            this.str = str;
            System.out.println(" <ERROR> " + str + " </ERROR>");
        }
    }

    public void emptyQueue(JMSContext context, Queue q) throws Exception {

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
            Message message = consumer.receive();
        }
    }
}