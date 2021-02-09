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
package web;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Enumeration;

import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.JMSProducer;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueReceiver;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicPublisher;
import javax.jms.TopicSession;
import javax.jms.TopicSubscriber;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

@SuppressWarnings("serial")
public class AutoCloseableTestServlet extends HttpServlet {
    public static QueueConnectionFactory QCFBindings;
    public static TopicConnectionFactory TCFBindings;

    public static Queue queue;

    public static Topic topic;

    public static boolean expectedExceptionFlag;

    public static boolean unexpectedExceptionFlag;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {       
        String test = request.getParameter("test");
        PrintWriter out = response.getWriter();
        out.println("Starting " + test + "<br>");
        final TraceComponent tc = Tr.register(AutoCloseableTestServlet.class); //injection engine doesn't like this at the class level
        Tr.entry(this, tc, test);
        try {
            getClass().getMethod(test, HttpServletRequest.class,
                                 HttpServletResponse.class).invoke(this, request, response);
            out.println(test + " COMPLETED SUCCESSFULLY");
            Tr.exit(this, tc, test);
        } catch (Throwable x) {
            if (x instanceof InvocationTargetException)
                x = x.getCause();
            Tr.exit(this, tc, test, x);
            out.println("<pre>ERROR in " + test + ":");
            x.printStackTrace(out);
            out.println("</pre>");
        }
    }

    @Override
    public void init() throws ServletException {      
        super.init();
        try {
            QCFBindings = getQCFBindings();
            TCFBindings = getTCFBindings();

            queue = (Queue) new InitialContext().lookup("java:comp/env/jndi_INPUT_Q");

            topic = (Topic) new InitialContext().lookup("java:comp/env/eis/topic1");
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    // This test case will test the AutoCloseable feature for QueueConnection
    public void testQueueConnectionClose(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        QueueConnection con1 = null;

        expectedExceptionFlag = false;

        unexpectedExceptionFlag = false;

        try (QueueConnection con = QCFBindings.createQueueConnection()) {
            con1 = con;
            con.start();
            System.out.println("Connection Started : ");
        } catch (JMSException ex3) {
            ex3.printStackTrace();
            // Check if QueueConnection fails
            unexpectedExceptionFlag = true;
        }

        try {
            con1.stop();
        } catch (Exception e) {            
            System.out.println("Exception occurred while doing a stop operation on the closed connection.");
            expectedExceptionFlag = true;
        }
        if (!expectedExceptionFlag || unexpectedExceptionFlag == true)
            throw new WrongException("testQueueConnectionClose failed");
    }

    // This test case will test the AutoCloseable feature for QueueSession
    public void testQueueSessionClose(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        expectedExceptionFlag = false;
        unexpectedExceptionFlag = false;
        QueueSession session1 = null;

        try (QueueConnection con = QCFBindings.createQueueConnection();
             QueueSession session = con.createQueueSession(false, javax.jms.Session.AUTO_ACKNOWLEDGE)) {
            
            con.start();
            session1 = session;
            System.out.println("Connection Started : ");
        } catch (JMSException ex3) {
            ex3.printStackTrace();
            unexpectedExceptionFlag = true;
        }

        try {
            session1.createProducer(queue);
        } catch (Exception e) {           
            System.out.println("Exception occurred while performing a createProducer operation on the closed QueueSession.");
            expectedExceptionFlag = true;
        }

        if (!expectedExceptionFlag || unexpectedExceptionFlag == true)
            throw new WrongException("testQueueSessionClose failed");
    }

    // This test case will test the AutoCloseable feature for QueueSender
    public void testQueueSenderClose(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        expectedExceptionFlag = false;

        unexpectedExceptionFlag = false;

        QueueSender sender1 = null;

        TextMessage msg = null;

        try (QueueConnection con = QCFBindings.createQueueConnection();
             QueueSession sessionSender = con.createQueueSession(false, javax.jms.Session.AUTO_ACKNOWLEDGE);
             QueueSender send = sessionSender.createSender(queue)) {

            con.start();
            sender1 = send;
            msg = sessionSender.createTextMessage("You got a new Message ");
            System.out.println("Connection Started : ");
        } catch (JMSException ex3) {
            ex3.printStackTrace();
            unexpectedExceptionFlag = true;
        }

        try {
            sender1.send(msg);
        } catch (Exception e) {            
            System.out.println("Exception occurred while sending message when QueueSender is closed.");
            expectedExceptionFlag = true;
        }

        if (!expectedExceptionFlag || unexpectedExceptionFlag == true)
            throw new WrongException("testQueueSenderClose failed");
    }

    // This test case will test the AutoCloseable feature for QueueBrowser
    public void testQueueBrowserClose(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        expectedExceptionFlag = false;
        QueueBrowser browser1 = null;
        unexpectedExceptionFlag = false;

        try (QueueConnection con = QCFBindings.createQueueConnection();
             QueueSession sessionSender = con.createQueueSession(false, javax.jms.Session.AUTO_ACKNOWLEDGE);
             QueueSender send = sessionSender.createSender(queue);
             QueueBrowser qb = sessionSender.createBrowser(queue);) {

            con.start();
            browser1 = qb;
            System.out.println("Connection Started in QueueBrowser: ");

        } catch (JMSException ex3) {
            ex3.printStackTrace();
            unexpectedExceptionFlag = true;
        }

        try {
            System.out.println(Arrays.asList(QCFBindings));
            System.out.println(Arrays.asList(QCFBindings.getClass().getInterfaces()));

            Enumeration e = browser1.getEnumeration();

            int numMsgs = 0;
            // count number of messages
            while (e.hasMoreElements()) {
                TextMessage message = (TextMessage) e.nextElement();
                numMsgs++;
            }

            System.out.println("Number of messages in Queue = " + numMsgs);
            PrintWriter out = response.getWriter();
            out.println("Queue has " + numMsgs);
        } catch (Exception e) {          
            System.out.println("Exception occurred while getting enumeration from a QueueBrowser which is closed");
            expectedExceptionFlag = true;
        }
        if (!expectedExceptionFlag || unexpectedExceptionFlag == true)
            throw new WrongException("testQueueBrowserClose failed");
    }

    // This test case will test the AutoCloseable feature for TopicConnection
    public void testTopicConnectionClose(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        expectedExceptionFlag = false;
        TopicConnection tconn1 = null;
        unexpectedExceptionFlag = false;

        try (TopicConnection con = TCFBindings.createTopicConnection()) {
            tconn1 = con;
            con.start();
            System.out.println("Connection Started : ");
        } catch (JMSException ex3) {
            ex3.printStackTrace();
            unexpectedExceptionFlag = true;
        }

        try {
            tconn1.stop();
        } catch (Exception e) {
            System.out.println("Exception occurred while performing a stop operation on a closed connection");
            expectedExceptionFlag = true;
        }

        if (!expectedExceptionFlag || unexpectedExceptionFlag == true)
            throw new WrongException("testTopicConnectionClose failed");
    }

    // This test case will test the AutoCloseable feature for TopicSession
    public void testTopicSessionClose(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        expectedExceptionFlag = false;

        TopicSession tsession1 = null;

        unexpectedExceptionFlag = false;

        try (TopicConnection con = TCFBindings.createTopicConnection();
             TopicSession session = con.createTopicSession(false, javax.jms.Session.AUTO_ACKNOWLEDGE)) {

            tsession1 = session;
            con.start();
            System.out.println("Connection Started : ");
        } catch (JMSException ex3) {
            ex3.printStackTrace();
            unexpectedExceptionFlag = true;
        }

        try {
            tsession1.createProducer(topic);
        } catch (Exception e) {
            System.out.println("Exception occurred while performing a createProducer on a closed TopicSession");
            expectedExceptionFlag = true;
        }

        if (!expectedExceptionFlag || unexpectedExceptionFlag == true)
            throw new WrongException("testTopicSessionClose failed");
    }

    // This test case will test the AutoCloseable feature for TopicSubscriber
    public void testTopicSubscriberClose(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        expectedExceptionFlag = false;
        TopicSubscriber tsub1 = null;

        unexpectedExceptionFlag = false;

        try (TopicConnection con = TCFBindings.createTopicConnection();
             TopicSession session = con.createTopicSession(false, javax.jms.Session.AUTO_ACKNOWLEDGE);
             TopicSubscriber sub = session.createSubscriber(topic)) {

            tsub1 = sub;
            con.start();
            System.out.println("Connection Started : ");
        } catch (JMSException ex3) {
            ex3.printStackTrace();
            unexpectedExceptionFlag = true;
        }

        try {
            tsub1.receive();
        } catch (Exception e) {
            System.out.println("Exception occurred while performing receive on a closed subscriber");
            expectedExceptionFlag = true;
        }

        if (!expectedExceptionFlag || unexpectedExceptionFlag == true)
            throw new WrongException("testTopicSubscriberClose failed");
    }

    // This test case will test the AutoCloseable feature for TopicPublisher
    public void testTopicPublisherClose(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        expectedExceptionFlag = false;
        TopicPublisher tpub1 = null;
        TextMessage msg = null;
        unexpectedExceptionFlag = false;
        
        try (TopicConnection con = TCFBindings.createTopicConnection();
             TopicSession session = con.createTopicSession(false, javax.jms.Session.AUTO_ACKNOWLEDGE);
             TopicSubscriber sub = session.createSubscriber(topic);
             TopicPublisher publisher = session.createPublisher(topic)) {

            tpub1 = publisher;
            con.start();
            System.out.println("Connection Started : ");
            msg = session.createTextMessage("Pub/Sub Message");
        } catch (JMSException ex3) {
            ex3.printStackTrace();
            unexpectedExceptionFlag = true;
        }

        try {
            tpub1.publish(msg);
        } catch (Exception e) {
            System.out.println("Exception occurred while performing publish on a closed TopicPublisher");
            expectedExceptionFlag = true;
        }

        if (!expectedExceptionFlag || unexpectedExceptionFlag == true)
            throw new WrongException("testTopicPublisherClose failed");
    }

    // This test case will test the AutoCloseable feature for QueueReceiver
    public void testQueueReceiverClose(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        expectedExceptionFlag = false;
        QueueReceiver rec1 = null;

        unexpectedExceptionFlag = false;

        try (QueueConnection con = QCFBindings.createQueueConnection();
             QueueSession sessionSender = con.createQueueSession(false, javax.jms.Session.AUTO_ACKNOWLEDGE);
             QueueSender send = sessionSender.createSender(queue);
             QueueBrowser qb = sessionSender.createBrowser(queue);
             QueueReceiver rec = sessionSender.createReceiver(queue)) {

            con.start();
            rec1 = rec;
            System.out.println("Connection Started : ");
        } catch (JMSException ex3) {
            ex3.printStackTrace();
            unexpectedExceptionFlag = true;
        }

        try {
            rec1.receive();
        } catch (Exception e) {
            System.out.println("Exception occurred while performing a receive on a closed QueueReceiver.");
            expectedExceptionFlag = true;
        }

        if (!expectedExceptionFlag || unexpectedExceptionFlag == true)
            throw new WrongException("testQueueReceiverClose failed");
    }

    // This test case will test the AutoCloseable feature for JMSContext
    public void testJMSContextClose(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        JMSContext jmsContext = null;

        unexpectedExceptionFlag = false;
        int trueCounter = 0;

        try (JMSContext context = QCFBindings.createContext()) {
            jmsContext = context;
            context.createConsumer(queue);
        } catch (Exception ex) {
            ex.printStackTrace();
            unexpectedExceptionFlag = true;
        }

        try {
            System.out.println("Try creating a JMSProducer");
            JMSProducer jmsprod = jmsContext.createProducer();
            System.out.println(jmsprod);
            if (jmsprod == null)
                trueCounter++;
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }

        try {
            System.out.println("Try creating a JMSConsumer");
            JMSConsumer jmsCons = jmsContext.createConsumer(queue);
            System.out.println(jmsCons);
        } catch (Exception ex) {
            ex.printStackTrace();
            trueCounter++;
        }

        try {
            System.out.println("Try creating a browser");
            QueueBrowser qb = jmsContext.createBrowser(queue);
            System.out.println(qb);
        } catch (Exception ex) {
            ex.printStackTrace();
            trueCounter++;
        }
        try {
            System.out.println("Try creating a consumer with params");
            JMSConsumer jmsConsumer = jmsContext.createConsumer(queue, "COLOR=RED", true);
            System.out.println(jmsConsumer);
        } catch (Exception ex) {
            ex.printStackTrace();
            trueCounter++;
        }
        try {
            System.out.println("Try creating durable subscriber with params");
            JMSConsumer jmsDursub = jmsContext.createDurableConsumer(topic, "HEllo", "COLOR=RED", true);
            System.out.println(jmsDursub);
        } catch (Exception ex) {
            ex.printStackTrace();
            trueCounter++;
        }

        if (trueCounter != 5 || unexpectedExceptionFlag == true)
            throw new WrongException("testJMSConsumerClose failed");
    }

    // This test case will test the AutoCloseable feature for JMSConsumer
    public void testJMSConsumerClose(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        expectedExceptionFlag = false;

        unexpectedExceptionFlag = false;

        JMSConsumer jmsConsumer = null;

        JMSContext context = QCFBindings.createContext();

        JMSProducer jmsProducer = context.createProducer();

        jmsProducer.send(queue, "Hello1");

        try (JMSConsumer consumer = context.createConsumer(queue)) {
            consumer.receiveNoWait();
            jmsConsumer = consumer;
        } catch (Exception ex) {
            ex.printStackTrace();
            unexpectedExceptionFlag = true;
        }

        jmsProducer.send(queue, "Hello2");

        try {
            jmsConsumer.receiveNoWait();
        } catch (Exception ex) {
            System.out.println("Exception occurred while performing receiving operation on a closed JMSConsumer.");
            expectedExceptionFlag = true;
        }

        if (!expectedExceptionFlag || unexpectedExceptionFlag == true)
            throw new WrongException("testJMSConsumerClose failed");
    }

    public static QueueConnectionFactory getQCFBindings() throws NamingException {
        QueueConnectionFactory cf1 = (QueueConnectionFactory) new InitialContext().lookup("java:comp/env/jndi_JMS_BASE_QCF");

        return cf1;
    }

    public static TopicConnectionFactory getTCFBindings() throws NamingException {
        TopicConnectionFactory tcf1 = (TopicConnectionFactory) new InitialContext().lookup("java:comp/env/eis/tcf");

        return tcf1;
    }

    public class WrongException extends Exception {
        String str;

        public WrongException(String str) {
            this.str = str;
            System.out.println(" <ERROR> " + str + " </ERROR>");
        }
    }
}
