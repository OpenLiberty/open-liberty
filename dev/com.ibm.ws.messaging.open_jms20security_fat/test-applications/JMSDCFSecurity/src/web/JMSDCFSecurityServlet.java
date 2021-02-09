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

import javax.annotation.Resource;
import javax.jms.ConnectionFactory;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
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
public class JMSDCFSecurityServlet extends HttpServlet {

    public static QueueConnectionFactory jmsQCFBindings;
    public static QueueConnectionFactory jmsQCFTCP;
    public static Queue jmsQueue;
    public static Topic jmsTopic;
    public static boolean exceptionFlag;

    @Override
    public void init() throws ServletException {
        // TODO Auto-generated method stub

        super.init();
        try {
            jmsQueue = getQueue();
            jmsTopic = getTopic();

        } catch (NamingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    @Resource(lookup = "java:comp/DefaultJMSConnectionFactory")
    ConnectionFactory cf;

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response) throws ServletException, IOException {
        String test = request.getParameter("test");
        PrintWriter out = response.getWriter();
        out.println("Starting " + test + "<br>");
        final TraceComponent tc = Tr.register(JMSDCFSecurityServlet.class); // injection
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

    public void testP2P_TCP_SecOn(HttpServletRequest request,
                                  HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContextQueueTCP = cf.createContext();
        emptyQueue(jmsContextQueueTCP, jmsQueue);
        TextMessage message = jmsContextQueueTCP.createTextMessage("testP2P_TCP_SecOn");
        jmsContextQueueTCP.createProducer().send(jmsQueue, message);
        JMSConsumer jmsConsumer = jmsContextQueueTCP.createConsumer(jmsQueue);
        TextMessage message1 = (TextMessage) jmsConsumer.receive(500);
        System.out.println("Received message: " + message1.getText());

        if (!(message1 != null && message1.getText().equals("testP2P_TCP_SecOn")))
            exceptionFlag = true;
        jmsContextQueueTCP.close();
        if (exceptionFlag)
            throw new WrongException("testP2P_TCP_SecOn failed: Expected message was not received");

    }

    public void testPubSub_TCP_SecOn(HttpServletRequest request,
                                     HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContextTopicTCP = cf.createContext();
        TextMessage message = jmsContextTopicTCP.createTextMessage("testPubSub_TCP_SecOn");
        JMSConsumer jmsConsumer = jmsContextTopicTCP.createConsumer(jmsTopic);
        jmsContextTopicTCP.createProducer().send(jmsTopic, message);

        TextMessage message1 = (TextMessage) jmsConsumer.receive(500);
        System.out.println("Received message: " + message1.getText());

        if (!(message1 != null && message1.getText().equals(
                                                            "testPubSub_TCP_SecOn")))
            exceptionFlag = true;
        jmsContextTopicTCP.close();
        if (exceptionFlag)
            throw new WrongException("testPubSub_TCP_SecOn failed: Expected message was not received");

    }

    public void testPubSubDurable_TCP_SecOn(HttpServletRequest request,
                                            HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContextTopicTCP = cf.createContext();
        TextMessage message = jmsContextTopicTCP.createTextMessage("testPubSubDurable_TCP_SecOn");
        JMSConsumer jmsConsumer = jmsContextTopicTCP.createDurableConsumer(
                                                                           jmsTopic, "sub2");
        jmsContextTopicTCP.createProducer().send(jmsTopic, message);

        TextMessage message1 = (TextMessage) jmsConsumer.receive(500);
        System.out.println("Received message: " + message1.getText());

        if (!(message1 != null && message1.getText().equals(
                                                            "testPubSubDurable_TCP_SecOn")))
            exceptionFlag = true;
        jmsContextTopicTCP.close();
        if (exceptionFlag)
            throw new WrongException("testPubSubDurable_TCP_SecOn failed: Expected message was not received");

    }

    public void testP2PMQ_TCP_SecOn(HttpServletRequest request,
                                    HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        try {
            cf.createContext();

        } catch (java.lang.RuntimeException e) {
            Throwable causeEx = e.getCause();
            String actualException = causeEx.getClass().getName();

            System.out.println("Exception cause is " + actualException);
            if (!(actualException.equals("com.ibm.wsspi.sib.core.exception.SIAuthenticationException")))
                exceptionFlag = true;
            e.printStackTrace();
        }

        if (exceptionFlag)
            throw new WrongException("testP2PMQ_TCP_SecOn failed: Expected message was not received");

    }

    public Queue getQueue() throws NamingException {

        Queue queue = (Queue) new InitialContext().lookup("java:comp/env/jndi_INPUT_Q");

        return queue;
    }

    public Topic getTopic() throws NamingException {

        Topic topic = (Topic) new InitialContext().lookup("java:comp/env/eis/topic1");

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