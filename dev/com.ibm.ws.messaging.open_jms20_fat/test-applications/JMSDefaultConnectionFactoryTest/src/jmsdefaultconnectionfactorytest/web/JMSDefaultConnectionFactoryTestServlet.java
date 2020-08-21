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
package jmsdefaultconnectionfactorytest.web;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.Enumeration;

import javax.annotation.Resource;
import javax.inject.Inject;
import javax.jms.ConnectionFactory;
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

@SuppressWarnings("serial")
public class JMSDefaultConnectionFactoryTestServlet extends HttpServlet {

    public static QueueConnectionFactory jmsQCFBindings;
    public static QueueConnectionFactory jmsQCFTCP;
    public static Queue jmsQueue;

    public static boolean exceptionFlag;

    @Override
    public void init() throws ServletException {
        // TODO Auto-generated method stub

        super.init();
        try {

            jmsQueue = getQueue();

        } catch (NamingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    @Resource(lookup = "java:comp/DefaultJMSConnectionFactory")
    ConnectionFactory cf;

    @Inject
    private JMSContext jmsContextQueuetest;

    @Resource(name = "myJMSCF")
    ConnectionFactory myJMScf;

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response) throws ServletException, IOException {
        String test = request.getParameter("test");
        PrintWriter out = response.getWriter();
        out.println("Starting " + test + "<br>");
        final TraceComponent tc = Tr.register(JMSDefaultConnectionFactoryTestServlet.class); // injection
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

    public void testP2P_B_SecOff(HttpServletRequest request,
                                 HttpServletResponse response) throws Throwable {

        exceptionFlag = false;

        JMSContext jmsContextQueue = cf.createContext();

        TextMessage message = jmsContextQueue
                        .createTextMessage("testP2P_B_SecOff");
        jmsContextQueue.createProducer().send(jmsQueue, message);
        JMSConsumer jmsConsumer = jmsContextQueue.createConsumer(jmsQueue);
        TextMessage message1 = (TextMessage) jmsConsumer.receive(500);
        System.out.println("Received message: " + message1.getText());

        if (!(message1 != null && message1.getText().equals("testP2P_B_SecOff")))
            exceptionFlag = true;

        jmsContextQueue.close();
        if (exceptionFlag)
            throw new WrongException("testP2P_B_SecOff failed: Expected message was not received");

    }

    public void testP2P_B_SecOff_inject(HttpServletRequest request,
                                        HttpServletResponse response) throws Throwable {
//This is a test with @inject using defaultConnectionFactory
        exceptionFlag = false;

        TextMessage message = jmsContextQueuetest
                        .createTextMessage("testP2P_B_SecOff_inject");
        jmsContextQueuetest.createProducer().send(jmsQueue, message);
        JMSConsumer jmsConsumer = jmsContextQueuetest.createConsumer(jmsQueue);
        TextMessage message1 = (TextMessage) jmsConsumer.receive(500);
        System.out.println("Received message: " + message1.getText());

        if (!(message1 != null && message1.getText().equals("testP2P_B_SecOff_inject")))
            exceptionFlag = true;

        if (exceptionFlag)
            throw new WrongException("testP2P_B_SecOff_inject failed: Expected message was not received");

    }

    public void testPubSub_B_SecOff(HttpServletRequest request,
                                    HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContextTopic = cf.createContext();
        TextMessage message = jmsContextTopic
                        .createTextMessage("testPubSub_B_SecOff");
        Topic jmsTopic = jmsContextTopic.createTopic("Topic1");
        JMSConsumer jmsConsumer = jmsContextTopic.createConsumer(jmsTopic);
        jmsContextTopic.createProducer().send(jmsTopic, message);

        TextMessage message1 = (TextMessage) jmsConsumer.receive(500);
        System.out.println("Received message: " + message1.getText());

        if (!(message1 != null && message1.getText().equals("testPubSub_B_SecOff")))
            exceptionFlag = true;

        jmsContextTopic.close();
        if (exceptionFlag)
            throw new WrongException("testPubSub_B_SecOff failed: Expected message was not received");

    }

    public void testPubSub_B_SecOff_implicitBinding(HttpServletRequest request,
                                                    HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContextTopic = myJMScf.createContext();
        TextMessage message = jmsContextTopic
                        .createTextMessage("testPubSub_B_SecOff");
        Topic jmsTopic = jmsContextTopic.createTopic("Topic1");
        JMSConsumer jmsConsumer = jmsContextTopic.createConsumer(jmsTopic);
        jmsContextTopic.createProducer().send(jmsTopic, message);

        TextMessage message1 = (TextMessage) jmsConsumer.receive(500);
        System.out.println("Received message: " + message1.getText());

        if (!(message1 != null && message1.getText().equals("testPubSub_B_SecOff")))
            exceptionFlag = true;

        jmsContextTopic.close();
        if (exceptionFlag)
            throw new WrongException("testPubSub_B_SecOff_implicitBinding failed: Expected message was not received");

    }

    public void testPubSubDurable_B_SecOff(HttpServletRequest request,
                                           HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContextTopic = cf.createContext();
        TextMessage message = jmsContextTopic
                        .createTextMessage("testPubSubDurable_B_SecOff");
        Topic jmsTopic = jmsContextTopic.createTopic("Topic1");
        JMSConsumer jmsConsumer = jmsContextTopic.createDurableConsumer(jmsTopic, "sub1");
        jmsContextTopic.createProducer().send(jmsTopic, message);

        TextMessage message1 = (TextMessage) jmsConsumer.receive(500);
        System.out.println("Received message: " + message1.getText());

        if (!(message1 != null && message1.getText().equals("testPubSubDurable_B_SecOff")))
            exceptionFlag = true;

        jmsContextTopic.close();
        if (exceptionFlag)
            throw new WrongException("testPubSubDurable_B_SecOff failed: Expected message was not received");

    }

    public void testMessageOrder_B_SecOff(HttpServletRequest request,
                                          HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        int msgOrder[] = new int[10];
        int redFlag = 0;
        JMSContext jmsContextQueue = cf.createContext();
        JMSContext jmsContextQueueNew = cf.createContext();

        JMSProducer producer1 = jmsContextQueue.createProducer();
        JMSProducer producer2 = jmsContextQueueNew.createProducer();
        JMSConsumer jmsConsumer = jmsContextQueue.createConsumer(jmsQueue);
        Message msg = null;
        int msgRcvd = 0;
        for (int i = 0; i < 10; i++)
        {

            if (i % 2 == 0) {
                msg = jmsContextQueue.createMessage();
                msg.setIntProperty("Message_Order", i);
                producer1.send(jmsQueue, msg);
            }
            else {
                msg = jmsContextQueueNew.createMessage();
                msg.setIntProperty("Message_Order", i);
                producer2.send(jmsQueue, msg);
            }

            msgRcvd = jmsConsumer.receive(1000).getIntProperty("Message_Order");
            System.out.println("Received message number : " + msgRcvd);
            msgOrder[i] = msgRcvd;
        }

        for (int i = 0; i < 10; i++) {
            System.out.println("Retrieving Message Order:" + msgOrder[i]);
            if (msgOrder[i] == i)
                System.out.println("msgOrder:" + msgOrder[i]);
            else
                redFlag++;
        }

        if (!(redFlag == 0))
            exceptionFlag = true;
        jmsContextQueue.close();
        jmsContextQueueNew.close();

        if (exceptionFlag)
            throw new WrongException("testMessageOrder_B_SecOff failed: Messages were not received in the expected order");

    }

    public void testBrowser_B_SecOff(HttpServletRequest request,
                                     HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContextQueue = cf.createContext();

        TextMessage message = jmsContextQueue.createTextMessage("testBrowser_B_SecOff");
        jmsContextQueue.createProducer().send(jmsQueue, message);
        JMSConsumer jmsConsumer = jmsContextQueue.createConsumer(jmsQueue);
        QueueBrowser qb = jmsContextQueue.createBrowser(jmsQueue);

        Enumeration e = qb.getEnumeration();

        int numMsgs = 0;
        // count number of messages
        while (e.hasMoreElements()) {
            e.nextElement();
            numMsgs++;
        }

        TextMessage message1 = (TextMessage) jmsConsumer.receive(1000);
        System.out.println("Received message: " + message1.getText());

        if (!(numMsgs == 1 && message1 != null && message1.getText().equals("testBrowser_B_SecOff")))
            exceptionFlag = true;
        jmsContextQueue.close();

        if (exceptionFlag)
            throw new WrongException("testBrowser_B_SecOff failed: Expected message was not received");

    }

    public void testObjects(HttpServletRequest request,
                            HttpServletResponse response) throws Throwable {

        exceptionFlag = false;

        ConnectionFactory lookupcf = (ConnectionFactory) new InitialContext()
                        .lookup("java:comp/DefaultJMSConnectionFactory");

        if (!(lookupcf.equals(myJMScf))) {
            System.out.println(" cf and myJMScf are not the same objects");
            exceptionFlag = true;
        }

        if (exceptionFlag)
            throw new WrongException("testObjects failed: The two objects are the not the same");
    }

    public Queue getQueue() throws NamingException {

        Queue queue = (Queue) new InitialContext()
                        .lookup("java:comp/env/jndi_INPUT_Q");

        return queue;
    }

    public class WrongException extends Exception {
        String str;

        public WrongException(String str) {
            this.str = str;
            System.out.println(" <ERROR> " + str + " </ERROR>");
        }
    }

    public void emptyQueue(QueueConnectionFactory qcf, Queue q) throws Exception {

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
            Message message = consumer.receive();
        }

        context.close();
    }
}
