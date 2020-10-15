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

import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.JMSProducer;
import javax.jms.JMSRuntimeException;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.jms.QueueConnectionFactory;
import javax.jms.Session;
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

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

@SuppressWarnings("serial")
public class JMSContextTestServlet extends HttpServlet {

    public static QueueConnectionFactory qcfBindings;
    public static QueueConnectionFactory qcfTCP;

    public static TopicConnectionFactory tcfBindings;
    public static TopicConnectionFactory tcfTCP;

    public static Queue queue;

    public static Topic topic;

    public static boolean exceptionFlag;

    @Override
    public void init() throws ServletException { // TODO
        // Auto-generated method stub

        super.init();
        try {
            qcfBindings = getQCFBindings();
            qcfTCP = getQCFTCP();
            queue = getQueue();
            tcfBindings = getTCFBindings();
            tcfTCP = getTCFTCP();
            topic = getTopic();

        } catch (NamingException e) { // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    public static QueueConnectionFactory getQCFBindings() throws NamingException {

        QueueConnectionFactory cf1 = (QueueConnectionFactory) new InitialContext().lookup("java:comp/env/jndi_JMS_BASE_QCF");

        return cf1;

    }

    public QueueConnectionFactory getQCFTCP() throws NamingException {

        QueueConnectionFactory cf1 = (QueueConnectionFactory) new InitialContext().lookup("java:comp/env/jndi_JMS_BASE_QCF1");

        return cf1;

    }

    public Queue getQueue() throws NamingException {

        Queue queue = (Queue) new InitialContext().lookup("java:comp/env/jndi_INPUT_Q");

        return queue;
    }

    public static TopicConnectionFactory getTCFBindings() throws NamingException {

        TopicConnectionFactory tcf1 = (TopicConnectionFactory) new InitialContext().lookup("java:comp/env/eis/tcf");

        return tcf1;

    }

    public TopicConnectionFactory getTCFTCP() throws NamingException {

        TopicConnectionFactory tcf1 = (TopicConnectionFactory) new InitialContext().lookup("java:comp/env/eis/tcf1");

        return tcf1;

    }

    public Topic getTopic() throws NamingException {

        Topic topic = (Topic) new InitialContext().lookup("eis/topic1");

        return topic;
    }

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response) throws ServletException, IOException {
        String test = request.getParameter("test");
        PrintWriter out = response.getWriter();
        out.println("Starting --" + test + "<br>");
        final TraceComponent tc = Tr.register(JMSContextTestServlet.class);
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
            System.out.println(" End: " + test);
            x.printStackTrace(out);
            out.println("</pre>");
        }
    }

    // ====================================

    // -----------------118066

    public void testStartJMSContextSecOnBinding(HttpServletRequest request,
                                                HttpServletResponse response) throws Throwable {

        exceptionFlag = false;

        String userName = "user1";
        String password = "user1pwd";

        JMSContext jmsContextQCFBindings = qcfBindings.createContext(userName,
                                                                     password);
        emptyQueue(qcfBindings, queue);

        jmsContextQCFBindings.setAutoStart(false);

        String outbound = "Hello World";
        JMSProducer jmsProducerQCFBindings = jmsContextQCFBindings.createProducer();
        jmsProducerQCFBindings.send(queue, outbound);
        jmsContextQCFBindings.start();

        JMSConsumer jmsConsumerQCFBindings = jmsContextQCFBindings.createConsumer(queue);
        TextMessage receiveMsg = (TextMessage) jmsConsumerQCFBindings.receive();

        String inbound = "";
        inbound = receiveMsg.getText();

        if (outbound.equals(inbound))
            exceptionFlag = false;
        else
            exceptionFlag = true;

        if (exceptionFlag == true)
            throw new WrongException("testStartJMSContextSecOnBinding failed");

        jmsConsumerQCFBindings.close();
        jmsContextQCFBindings.close();

    }

    public void testStartJMSContextSecOnTCP(HttpServletRequest request,
                                            HttpServletResponse response) throws Throwable {
        exceptionFlag = false;

        String userName = "user1";
        String password = "user1pwd";
        JMSContext jmsContextQCFTCP = qcfTCP.createContext(userName, password);
        emptyQueue(qcfTCP, queue);
        jmsContextQCFTCP.setAutoStart(false);

        String outbound = "Hello World";
        JMSProducer jmsProducerQCFTCP = jmsContextQCFTCP.createProducer();
        jmsProducerQCFTCP.send(queue, outbound);
        jmsContextQCFTCP.start();

        JMSConsumer jmsConsumerQCFTCP = jmsContextQCFTCP.createConsumer(queue);
        TextMessage receiveMsg = (TextMessage) jmsConsumerQCFTCP.receive();

        String inbound = "";
        inbound = receiveMsg.getText();

        if (outbound.equals(inbound))
            exceptionFlag = false;
        else
            exceptionFlag = true;

        jmsContextQCFTCP.close();

        if (exceptionFlag == true)
            throw new WrongException("testStartJMSContextSecOnTCP failed");

        jmsConsumerQCFTCP.close();
        jmsContextQCFTCP.close();

    }

    public void testStartJMSContextStartSecOnBinding(
                                                     HttpServletRequest request, HttpServletResponse response) throws Throwable {
        exceptionFlag = false;
        try {

            String userName = "user1";
            String password = "user1pwd";
            JMSContext jmsContextQCFBindings = qcfBindings.createContext(
                                                                         userName, password);

            jmsContextQCFBindings.start();
            jmsContextQCFBindings.start();

            jmsContextQCFBindings.close();

        } catch (Exception e) {
            e.printStackTrace();
            exceptionFlag = true;
        }
        if (exceptionFlag == true)
            throw new WrongException("testStartJMSContextStartSecOnBinding failed");
    }

    public void testStartJMSContextStartSecOnTCP(HttpServletRequest request,
                                                 HttpServletResponse response) throws Throwable {
        exceptionFlag = false;
        try {

            String userName = "user1";
            String password = "user1pwd";
            JMSContext jmsContextQCFTCP = qcfTCP.createContext(userName,
                                                               password);

            jmsContextQCFTCP.start();

            jmsContextQCFTCP.start();

            jmsContextQCFTCP.close();

        } catch (Exception e) {
            e.printStackTrace();
            exceptionFlag = true;

        }
        if (exceptionFlag == true)
            throw new WrongException("testStartJMSContextStartSecOnTCP failed");

    }

    public void testStopJMSContextSecOnBinding(HttpServletRequest request,
                                               HttpServletResponse response) throws Throwable {
        exceptionFlag = false;
        try {

            String userName = "user1";
            String password = "user1pwd";
            JMSContext jmsContextQCFBindings = qcfBindings.createContext(
                                                                         userName, password);

            jmsContextQCFBindings.start();

            jmsContextQCFBindings.stop();

            jmsContextQCFBindings.close();

        } catch (JMSRuntimeException ex) {

            ex.printStackTrace();

            exceptionFlag = true;
        }
        if (exceptionFlag == false)
            throw new WrongException("testStopJMSContextSecOnBinding failed");
    }

    public void testStopJMSContextSecOnTCP(HttpServletRequest request,
                                           HttpServletResponse response) throws Throwable {
        exceptionFlag = false;
        try {

            String userName = "user1";
            String password = "user1pwd";
            JMSContext jmsContextQCFTCP = qcfTCP.createContext(userName,
                                                               password);

            jmsContextQCFTCP.start();

            jmsContextQCFTCP.stop();
            jmsContextQCFTCP.close();

        } catch (JMSRuntimeException ex) {

            ex.printStackTrace();

            exceptionFlag = true;
        }
        if (exceptionFlag == false)
            throw new WrongException("testStopJMSContextSecOnBinding failed");
    }

    // ----------------------------------------------------------------------
    // --------------- 118065 ----------------------------------------------

    public void testCommitLocalTransaction_B(HttpServletRequest request,
                                             HttpServletResponse response) throws Exception {

        exceptionFlag = false;
        try {
            String userName = "user1";
            String password = "user1pwd";
            JMSContext jmsContextQCFBindings = qcfBindings.createContext(
                                                                         userName, password, Session.SESSION_TRANSACTED);
            emptyQueue(qcfBindings, queue);
            Message message = jmsContextQCFBindings.createTextMessage("Hello World");
            JMSProducer jmsProducerQCFBindings = jmsContextQCFBindings.createProducer();
            jmsProducerQCFBindings.send(queue, message);

            QueueBrowser qb = jmsContextQCFBindings.createBrowser(queue);
            Enumeration e = qb.getEnumeration();
            int numMsgs = 0;
            while (e.hasMoreElements()) {
                TextMessage message1 = (TextMessage) e.nextElement();
                numMsgs++;
            }

            jmsContextQCFBindings.commit();

            QueueBrowser qb1 = jmsContextQCFBindings.createBrowser(queue);
            Enumeration e1 = qb1.getEnumeration();
            int numMsgs1 = 0;
            // count number of messages
            while (e1.hasMoreElements()) {
                TextMessage message1 = (TextMessage) e1.nextElement();
                numMsgs1++;
            }
            JMSConsumer jmsConsumerQCFBindings = jmsContextQCFBindings.createConsumer(queue);
            TextMessage rmsg = (TextMessage) jmsConsumerQCFBindings.receive();

            jmsContextQCFBindings.commit();

            jmsConsumerQCFBindings.close();
            jmsContextQCFBindings.close();

        } catch (Exception e) {

            e.printStackTrace();
            exceptionFlag = true;
        }
        if (exceptionFlag == true)
            throw new WrongException("testCommitLocalTransaction_B failed");

    }

    public void testCommitLocalTransaction_TCP(HttpServletRequest request,
                                               HttpServletResponse response) throws Exception {
        exceptionFlag = false;
        try {
            String userName = "user1";
            String password = "user1pwd";
            JMSContext jmsContextQCFTCP = qcfTCP.createContext(userName,
                                                               password, Session.SESSION_TRANSACTED);
            emptyQueue(qcfTCP, queue);

            Message message = jmsContextQCFTCP.createTextMessage("Hello World");
            JMSProducer jmsProducerQCFTCP = jmsContextQCFTCP.createProducer();
            jmsProducerQCFTCP.send(queue, message);

            QueueBrowser qb = jmsContextQCFTCP.createBrowser(queue);
            Enumeration e = qb.getEnumeration();
            int numMsgs = 0;
            while (e.hasMoreElements()) {
                TextMessage message1 = (TextMessage) e.nextElement();

                numMsgs++;
            }

            jmsContextQCFTCP.commit();

            QueueBrowser qb1 = jmsContextQCFTCP.createBrowser(queue);
            Enumeration e1 = qb1.getEnumeration();
            int numMsgs1 = 0;
            // count * number of messages
            while (e1.hasMoreElements()) {
                TextMessage message1 = (TextMessage) e1.nextElement();

                numMsgs1++;
            }

            JMSConsumer jmsConsumerQCFTCP = jmsContextQCFTCP.createConsumer(queue);
            TextMessage rmsg = (TextMessage) jmsConsumerQCFTCP.receive();

            jmsContextQCFTCP.commit();

            jmsConsumerQCFTCP.close();
            jmsContextQCFTCP.close();

        } catch (Exception e) {

            e.printStackTrace();
            exceptionFlag = true;
        }

        if (exceptionFlag == true)
            throw new WrongException("testCommitLocalTransaction_TCP failed");

    }

    public void testCommitNonLocalTransaction_B(HttpServletRequest request,
                                                HttpServletResponse response) throws Exception {

        exceptionFlag = false;

        String userName = "user1";
        String password = "user1pwd";
        try {
            JMSContext jmsContextQCFBindings = qcfBindings.createContext(
                                                                         userName, password);
            emptyQueue(qcfBindings, queue);

            Message message = jmsContextQCFBindings.createTextMessage("Hello World");

            JMSProducer jmsProducerQCFBindings = jmsContextQCFBindings.createProducer();
            jmsProducerQCFBindings.send(queue, message);

            QueueBrowser qb = jmsContextQCFBindings.createBrowser(queue);
            Enumeration e = qb.getEnumeration();
            int numMsgs = 0;
            while (e.hasMoreElements()) {
                TextMessage message1 = (TextMessage) e.nextElement();

                numMsgs++;
            }

            jmsContextQCFBindings.commit();

            jmsContextQCFBindings.close();

        } catch (JMSRuntimeException ex) {

            ex.printStackTrace();

            exceptionFlag = true;
        }

        if (exceptionFlag == false)
            throw new WrongException("testCommitNonLocalTransaction_B failed");
    }

    public void testCommitNonLocalTransaction_TCP(HttpServletRequest request,
                                                  HttpServletResponse response) throws Exception {
        exceptionFlag = false;
        try {
            String userName = "user1";
            String password = "user1pwd";
            JMSContext jmsContextQCFTCP = qcfTCP.createContext(userName,
                                                               password);
            emptyQueue(qcfTCP, queue);
            Message message = jmsContextQCFTCP.createTextMessage("Hello World");

            JMSProducer jmsProducerQCFTCP = jmsContextQCFTCP.createProducer();
            jmsProducerQCFTCP.send(queue, message);

            QueueBrowser qb = jmsContextQCFTCP.createBrowser(queue);
            Enumeration e = qb.getEnumeration();
            int numMsgs = 0;
            while (e.hasMoreElements()) {
                TextMessage message1 = (TextMessage) e.nextElement();

                numMsgs++;
            }

            jmsContextQCFTCP.commit();

            QueueBrowser qb1 = jmsContextQCFTCP.createBrowser(queue);
            Enumeration e1 = qb1.getEnumeration();
            int numMsgs1 = 0;
            // count number of messages
            while (e1.hasMoreElements()) {
                TextMessage message1 = (TextMessage) e1.nextElement();

                numMsgs1++;
            }
            jmsContextQCFTCP.close();

        } catch (JMSRuntimeException ex) {
            Throwable causeEx = ex.getCause();
            ex.printStackTrace();

            exceptionFlag = true;
        }
        if (exceptionFlag == false)
            throw new WrongException("testCommitNonLocalTransaction_B failed");

    }

    public void testRollbackLocalTransaction_B(HttpServletRequest request,
                                               HttpServletResponse response) throws Exception {

        exceptionFlag = false;
        try {
            String userName = "user1";
            String password = "user1pwd";
            JMSContext jmsContextQCFBindings = qcfBindings.createContext(
                                                                         userName, password, Session.SESSION_TRANSACTED);
            emptyQueue(qcfBindings, queue);
            Message message = jmsContextQCFBindings.createTextMessage("Hello World");

            JMSProducer jmsProducerQCFBindings = jmsContextQCFBindings.createProducer();
            jmsProducerQCFBindings.send(queue, message);
            jmsContextQCFBindings.commit();

            QueueBrowser qb = jmsContextQCFBindings.createBrowser(queue);
            Enumeration e = qb.getEnumeration();
            int numMsgs = 0;
            // count number of messages
            while (e.hasMoreElements()) {
                TextMessage message1 = (TextMessage) e.nextElement();

                numMsgs++;
            }

            JMSConsumer jmsConsumerQCFBindings = jmsContextQCFBindings.createConsumer(queue);
            TextMessage rmsg = (TextMessage) jmsConsumerQCFBindings.receive();

            QueueBrowser qb1 = jmsContextQCFBindings.createBrowser(queue);
            Enumeration e1 = qb1.getEnumeration();
            int numMsgs1 = 0;
            // count number of messages
            while (e1.hasMoreElements()) {
                TextMessage message1 = (TextMessage) e1.nextElement();
                numMsgs1++;
            }

            jmsContextQCFBindings.rollback();

            QueueBrowser qb2 = jmsContextQCFBindings.createBrowser(queue);
            Enumeration e2 = qb2.getEnumeration();
            int numMsgs2 = 0;
            while (e2.hasMoreElements()) {
                TextMessage message1 = (TextMessage) e2.nextElement();

                numMsgs2++;
            }

            jmsConsumerQCFBindings.close();
            jmsContextQCFBindings.close();

        } catch (Exception e) {

            e.printStackTrace();
            exceptionFlag = true;
        }
        if (exceptionFlag == true)
            throw new WrongException("testRollbackLocalTransaction_B failed");

    }

    public void testRollbackLocalTransaction_TCP(HttpServletRequest request,
                                                 HttpServletResponse response) throws Exception {

        exceptionFlag = false;
        try {
            String userName = "user1";
            String password = "user1pwd";
            JMSContext jmsContextQCFTCP = qcfTCP.createContext(userName,
                                                               password, Session.SESSION_TRANSACTED);
            emptyQueue(qcfTCP, queue);
            Message message = jmsContextQCFTCP.createTextMessage("Hello World");

            JMSProducer jmsProducerQCFTCP = jmsContextQCFTCP.createProducer();
            jmsProducerQCFTCP.send(queue, message);
            jmsContextQCFTCP.commit();

            QueueBrowser qb = jmsContextQCFTCP.createBrowser(queue);
            Enumeration e = qb.getEnumeration();
            int numMsgs = 0;
            // count number of messages
            while (e.hasMoreElements()) {
                TextMessage message1 = (TextMessage) e.nextElement();
                numMsgs++;
            }

            JMSConsumer jmsConsumerQCFTCP = jmsContextQCFTCP.createConsumer(queue);
            TextMessage rmsg = (TextMessage) jmsConsumerQCFTCP.receive();

            QueueBrowser qb1 = jmsContextQCFTCP.createBrowser(queue);
            Enumeration e1 = qb1.getEnumeration();
            int numMsgs1 = 0;
            // count number of messages
            while (e1.hasMoreElements()) {
                TextMessage message1 = (TextMessage) e1.nextElement();
                numMsgs1++;
            }

            jmsContextQCFTCP.rollback();

            QueueBrowser qb2 = jmsContextQCFTCP.createBrowser(queue);
            Enumeration e2 = qb2.getEnumeration();
            int numMsgs2 = 0;
            while (e2.hasMoreElements()) {
                TextMessage message1 = (TextMessage) e2.nextElement();

                numMsgs2++;
            }

            jmsConsumerQCFTCP.close();
            jmsContextQCFTCP.close();

        } catch (Exception e) {

            e.printStackTrace();
            exceptionFlag = true;
        }

        if (exceptionFlag == true)
            throw new WrongException("testRollbackLocalTransaction_TCP failed");

    }

    public void testRollbackNonLocalTransaction_B(HttpServletRequest request,
                                                  HttpServletResponse response) throws Exception {
        exceptionFlag = false;
        try {
            String userName = "user1";
            String password = "user1pwd";
            JMSContext jmsContextQCFBindings = qcfBindings.createContext(
                                                                         userName, password);
            emptyQueue(qcfBindings, queue);
            Message message = jmsContextQCFBindings.createTextMessage("Hello World");

            JMSProducer jmsProducerQCFBindings = jmsContextQCFBindings.createProducer();

            jmsProducerQCFBindings.send(queue, message);
            jmsContextQCFBindings.rollback();

            jmsContextQCFBindings.close();

        } catch (JMSRuntimeException ex) {

            ex.printStackTrace();

            exceptionFlag = true;
        }
        if (exceptionFlag == false)
            throw new WrongException("testRollbackNonLocalTransaction_B failed");
    }

    public void testRollbackNonLocalTransaction_TCP(HttpServletRequest request,
                                                    HttpServletResponse response) throws Exception {
        exceptionFlag = false;
        try {
            String userName = "user1";
            String password = "user1pwd";
            JMSContext jmsContextQCFTCP = qcfTCP.createContext(userName,
                                                               password);
            emptyQueue(qcfTCP, queue);
            Message message = jmsContextQCFTCP.createTextMessage("Hello World");

            JMSProducer jmsProducerQCFTCP = jmsContextQCFTCP.createProducer();
            jmsProducerQCFTCP.send(queue, message);
            jmsContextQCFTCP.rollback();

            jmsContextQCFTCP.close();

        } catch (JMSRuntimeException ex) {

            ex.printStackTrace();

            exceptionFlag = true;
        }
        if (exceptionFlag == false)
            throw new WrongException("testRollbackNonLocalTransaction_TCP failed");

    }

    public void testRecoverNonLocalTransaction_B(HttpServletRequest request,
                                                 HttpServletResponse response) throws Exception {
        exceptionFlag = false;
        try {
            String userName = "user1";
            String password = "user1pwd";
            JMSContext jmsContextQCFBindings = qcfBindings.createContext(
                                                                         userName, password, Session.SESSION_TRANSACTED);
            emptyQueue(qcfBindings, queue);
            Message message = jmsContextQCFBindings.createTextMessage("Hello World");

            JMSProducer jmsProducerQCFBindings = jmsContextQCFBindings.createProducer();
            jmsProducerQCFBindings.send(queue, message);
            jmsContextQCFBindings.recover();
            jmsContextQCFBindings.close();

        } catch (JMSRuntimeException ex) {

            ex.printStackTrace();

            exceptionFlag = true;
        }
        if (exceptionFlag == false)
            throw new WrongException("testRecoverNonLocalTransaction_B failed");

    }

    public void testRecoverNonLocalTransaction_TCP(HttpServletRequest request,
                                                   HttpServletResponse response) throws Exception {

        exceptionFlag = false;
        try {
            String userName = "user1";
            String password = "user1pwd";
            JMSContext jmsContextQCFTCP = qcfTCP.createContext(userName,
                                                               password, Session.SESSION_TRANSACTED);
            emptyQueue(qcfTCP, queue);
            Message message = jmsContextQCFTCP.createTextMessage("Hello World");
            JMSProducer jmsProducerQCFTCP = jmsContextQCFTCP.createProducer();

            jmsProducerQCFTCP.send(queue, message);

            jmsContextQCFTCP.recover();

            jmsContextQCFTCP.close();

        } catch (JMSRuntimeException ex) {

            ex.printStackTrace();

            exceptionFlag = true;
        }
        if (exceptionFlag == false)
            throw new WrongException("testRecoverNonLocalTransaction_TCP failed");

    }

    // ========================= 118068 ===========================

    public void testCreateTemporaryQueueSecOnBinding(
                                                     HttpServletRequest request, HttpServletResponse response) throws Throwable {
        exceptionFlag = false;

        String userName = "user1";
        String password = "user1pwd";
        JMSContext jmsContextQCFBindings = qcfBindings.createContext(userName,
                                                                     password);

        // Create the temp queue
        TemporaryQueue tempQ = jmsContextQCFBindings.createTemporaryQueue();

        if (tempQ != null) {
            exceptionFlag = false;
        } else {
            exceptionFlag = true;
        }

        if (exceptionFlag == true)
            throw new WrongException("testCreateTemporaryQueueSecOnBinding failed");
        jmsContextQCFBindings.close();

    }

    // ==========================================
    public void testCreateTemporaryQueueSecOnTCPIP(HttpServletRequest request,
                                                   HttpServletResponse response) throws Throwable {
        exceptionFlag = false;

        String userName = "user1";
        String password = "user1pwd";
        JMSContext jmsContextQCFTCP = qcfTCP.createContext(userName, password);

        TemporaryQueue tempQ = jmsContextQCFTCP.createTemporaryQueue();
        if (tempQ != null) {
            exceptionFlag = false;

        } else {

            exceptionFlag = true;

        }

        if (exceptionFlag == true)
            throw new WrongException("testCreateTemporaryQueueSecOnTCPIP failed");
    }

    public void testTemporaryQueueLifetimeSecOn_B(HttpServletRequest request,
                                                  HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        try {
            String userName = "user1";
            String password = "user1pwd";
            JMSContext jmsContextQCFBindings = qcfBindings.createContext(
                                                                         userName, password);

            TemporaryQueue tempQ = jmsContextQCFBindings.createTemporaryQueue();

            jmsContextQCFBindings.close();
            jmsContextQCFBindings.createBrowser(tempQ);

        } catch (JMSRuntimeException ex) {

            ex.printStackTrace();

            exceptionFlag = true;
        }
        if (exceptionFlag == false)
            throw new WrongException("testTemporaryQueueLifetimeSecOn_B failed");

    }

    public void testTemporaryQueueLifetimeSecOn_TCPIP(
                                                      HttpServletRequest request, HttpServletResponse response) throws Throwable {
        exceptionFlag = false;
        try {
            String userName = "user1";
            String password = "user1pwd";
            JMSContext jmsContextQCFTCP = qcfTCP.createContext(userName,
                                                               password);
            TemporaryQueue tempQ = jmsContextQCFTCP.createTemporaryQueue();

            jmsContextQCFTCP.close();
            jmsContextQCFTCP.createBrowser(tempQ);

        } catch (JMSRuntimeException ex) {

            ex.printStackTrace();

            exceptionFlag = true;
        }
        if (exceptionFlag == false)
            throw new WrongException("testTemporaryQueueLifetimeSecOn_TCPIP failed");

    }

    public void testgetTemporaryQueueNameSecOnBinding(
                                                      HttpServletRequest request, HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        String userName = "user1";
        String password = "user1pwd";
        JMSContext jmsContextQCFBindings = qcfBindings.createContext(userName,
                                                                     password);

        TemporaryQueue tempQ = jmsContextQCFBindings.createTemporaryQueue();

        if (tempQ.getQueueName() != (null)) {
            exceptionFlag = false;

        } else {

            exceptionFlag = true;

        }

        if (exceptionFlag == true)
            throw new WrongException("testgetTemporaryQueueNameSecOnTCPIP failed");

        jmsContextQCFBindings.close();

    }

    public void testgetTemporaryQueueNameSecOnTCPIP(HttpServletRequest request,
                                                    HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        String userName = "user1";
        String password = "user1pwd";
        JMSContext jmsContextQCFTCP = qcfTCP.createContext(userName, password);

        TemporaryQueue tempQ = jmsContextQCFTCP.createTemporaryQueue();

        if (tempQ.getQueueName() != (null)) {
            exceptionFlag = false;

        } else {

            exceptionFlag = true;

        }

        if (exceptionFlag == true)
            throw new WrongException("testgetTemporaryQueueNameSecOnTCPIP failed");
        jmsContextQCFTCP.close();

    }

    public void testToStringTemporaryQueueNameSecOnBinding(
                                                           HttpServletRequest request, HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        String userName = "user1";
        String password = "user1pwd";
        JMSContext jmsContextQCFBindings = qcfBindings.createContext(userName,
                                                                     password);

        TemporaryQueue tempQ = jmsContextQCFBindings.createTemporaryQueue();

        if (tempQ.toString() != (null)) {

            exceptionFlag = false;

        } else {

            exceptionFlag = true;

        }

        if (exceptionFlag == true)
            throw new WrongException("testToStringTemporaryQueueNameSecOnBinding failed");
        jmsContextQCFBindings.close();
    }

    public void testToStringTemporaryQueueNameSecOnTCPIP(
                                                         HttpServletRequest request, HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        String userName = "user1";
        String password = "user1pwd";
        JMSContext jmsContextQCFTCP = qcfTCP.createContext(userName, password);

        TemporaryQueue tempQ = jmsContextQCFTCP.createTemporaryQueue();

        if (tempQ.toString() != (null)) {
            exceptionFlag = false;
        } else {
            exceptionFlag = true;
        }

        if (exceptionFlag == true)
            throw new WrongException("testToStringTemporaryQueueNameSecOnTCPIP failed");
        jmsContextQCFTCP.close();

    }

    public void testDeleteTemporaryQueueNameSecOnBinding(
                                                         HttpServletRequest request, HttpServletResponse response) throws Throwable {
        exceptionFlag = false;
        try {
            String userName = "user1";
            String password = "user1pwd";
            JMSContext jmsContextQCFBindings = qcfBindings.createContext(
                                                                         userName, password);

            TemporaryQueue tempQ = jmsContextQCFBindings.createTemporaryQueue();
            tempQ.delete();
            jmsContextQCFBindings.createBrowser(tempQ);

            jmsContextQCFBindings.close();

        } catch (JMSRuntimeException ex) {

            ex.printStackTrace();

            exceptionFlag = true;
        }
        if (exceptionFlag == false)
            throw new WrongException("testDeleteTemporaryQueueNameSecOnBinding failed");

    }

    public void testDeleteTemporaryQueueNameSecOnTCPIP(
                                                       HttpServletRequest request, HttpServletResponse response) throws Throwable {
        exceptionFlag = false;
        try {
            String userName = "user1";
            String password = "user1pwd";
            JMSContext jmsContextQCFTCP = qcfTCP.createContext(userName,
                                                               password);

            TemporaryQueue tempQ = jmsContextQCFTCP.createTemporaryQueue();

            tempQ.delete();
            jmsContextQCFTCP.createBrowser(tempQ);
            jmsContextQCFTCP.close();

        } catch (JMSRuntimeException ex) {

            ex.printStackTrace();

            exceptionFlag = true;
        }
        if (exceptionFlag == false)
            throw new WrongException("testDeleteTemporaryQueueNameSecOnTCPIP failed");

    }

    public void testDeleteExceptionTemporaryQueueNameSecOn_B(
                                                             HttpServletRequest request, HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        try {
            String userName = "user1";
            String password = "user1pwd";
            JMSContext jmsContextQCFBindings = qcfBindings.createContext(
                                                                         userName, password);

            TemporaryQueue tempQ = jmsContextQCFBindings.createTemporaryQueue();
            tempQ.delete();
            jmsContextQCFBindings.createBrowser(tempQ);
            jmsContextQCFBindings.close();

        } catch (JMSRuntimeException ex) {

            ex.printStackTrace();

            exceptionFlag = true;
        }
        if (exceptionFlag == false)
            throw new WrongException("testDeleteExceptionTemporaryQueueNameSecOn_B failed");

    }

    public void testDeleteExceptionTemporaryQueueNameSecOn_TCPIP(
                                                                 HttpServletRequest request, HttpServletResponse response) throws Throwable {
        exceptionFlag = false;
        try {
            String userName = "user1";
            String password = "user1pwd";
            JMSContext jmsContextQCFTCP = qcfTCP.createContext(userName,
                                                               password);

            TemporaryQueue tempQ = jmsContextQCFTCP.createTemporaryQueue();

            tempQ.delete();
            jmsContextQCFTCP.createBrowser(tempQ);

            jmsContextQCFTCP.close();

        } catch (JMSRuntimeException ex) {

            ex.printStackTrace();

            exceptionFlag = true;
        }
        if (exceptionFlag == false)
            throw new WrongException("testDeleteExceptionTemporaryQueueNameSecOn_TCPIP failed");

    }

    public void testPTPTemporaryQueue_Binding(HttpServletRequest request,
                                              HttpServletResponse response) throws Throwable {
        exceptionFlag = false;

        String userName = "user1";
        String password = "user1pwd";
        JMSContext jmsContextQCFBindings = qcfBindings.createContext(userName,
                                                                     password);

        TemporaryQueue tempQ = jmsContextQCFBindings.createTemporaryQueue();

        JMSProducer jmsProducerQCFBindings = jmsContextQCFBindings.createProducer();

        JMSConsumer jmsConsumerQCFBindings = jmsContextQCFBindings.createConsumer(tempQ);

        jmsProducerQCFBindings.send(tempQ, "hello world");

        TextMessage recMessage = (TextMessage) jmsConsumerQCFBindings.receive();

        if (recMessage.getText() == "hello world") {
            exceptionFlag = false;
        } else {

            exceptionFlag = true;
        }
        if (exceptionFlag == true)
            throw new WrongException("testPTPTemporaryQueue_Binding failed");

        jmsConsumerQCFBindings.close();
        jmsContextQCFBindings.close();

    }

    public void testPTPTemporaryQueue_TCP(HttpServletRequest request,
                                          HttpServletResponse response) throws Throwable {
        exceptionFlag = false;

        String userName = "user1";

        String password = "user1pwd";
        JMSContext jmsContextQCFTCP = qcfTCP.createContext(userName, password);

        TemporaryQueue tempQ = jmsContextQCFTCP.createTemporaryQueue();

        JMSProducer jmsProducerQCFTCP = jmsContextQCFTCP.createProducer();

        JMSConsumer jmsConsumerQCFTCP = jmsContextQCFTCP.createConsumer(tempQ);

        jmsProducerQCFTCP.send(tempQ, "hello world");

        TextMessage recMessage = (TextMessage) jmsConsumerQCFTCP.receive();

        if (recMessage.getText().equalsIgnoreCase("hello world")) {
            exceptionFlag = false;
        } else {

            exceptionFlag = true;
        }
        if (exceptionFlag == true)
            throw new WrongException("testPTPTemporaryQueue_TCP failed");

        jmsConsumerQCFTCP.close();
        jmsContextQCFTCP.close();
    }

    // -------------------- Temporary topic

    public void testCreateTemporaryTopicSecOnBinding(
                                                     HttpServletRequest request, HttpServletResponse response) throws Throwable {
        exceptionFlag = false;

        String userName = "user1";
        String password = "user1pwd";
        JMSContext jmsContextTCFBindings = tcfBindings.createContext(userName,
                                                                     password);
        // Create the temp queue
        TemporaryTopic tempT = jmsContextTCFBindings.createTemporaryTopic();

        if (tempT != null) {
            exceptionFlag = false;
        } else {
            exceptionFlag = true;
        }

        if (exceptionFlag == true)
            throw new WrongException("testCreateTemporaryTopicSecOnBinding failed");
        jmsContextTCFBindings.close();

    }

    public void testCreateTemporaryTopicSecOnTCPIP(HttpServletRequest request,
                                                   HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        String userName = "user1";
        String password = "user1pwd";
        JMSContext jmsContextTCFTCP = tcfTCP.createContext(userName, password);

        TemporaryTopic tempT = jmsContextTCFTCP.createTemporaryTopic();

        if (tempT != null) {
            exceptionFlag = false;
        } else {
            exceptionFlag = true;
        }
        if (exceptionFlag == true)
            throw new WrongException("testCreateTemporaryTopicSecOnTCPIP failed");
        jmsContextTCFTCP.close();

    }

    public void testTemporaryTopicLifetimeSecOn_B(HttpServletRequest request,
                                                  HttpServletResponse response) throws Throwable {
        exceptionFlag = false;
        try {
            String userName = "user1";
            String password = "user1pwd";
            JMSContext jmsContextTCFBindings = tcfBindings.createContext(
                                                                         userName, password);

            TemporaryTopic tempT = jmsContextTCFBindings.createTemporaryTopic();

            jmsContextTCFBindings.close();
            tempT.delete();

        } catch (JMSRuntimeException ex) {

            ex.printStackTrace();

            exceptionFlag = true;
        }
        if (exceptionFlag == true)
            throw new WrongException("testTemporaryTopicLifetimeSecOn_B failed");

    }

    public void testTemporaryTopicLifetimeSecOn_TCPIP(
                                                      HttpServletRequest request, HttpServletResponse response) throws Throwable {
        exceptionFlag = false;
        try {
            String userName = "user1";
            String password = "user1pwd";
            JMSContext jmsContextTCFTCP = tcfTCP.createContext(userName,
                                                               password);

            TemporaryTopic tempT = jmsContextTCFTCP.createTemporaryTopic();

            jmsContextTCFTCP.close();
            tempT.delete();

        } catch (JMSRuntimeException ex) {
            ex.printStackTrace();
            exceptionFlag = true;
        }
        if (exceptionFlag == true)
            throw new WrongException("testTemporaryTopicLifetimeSecOn_TCPIP failed");

    }

    public void testGetTemporaryTopicSecOnBinding(HttpServletRequest request,
                                                  HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        String userName = "user1";
        String password = "user1pwd";
        JMSContext jmsContextTCFBindings = tcfBindings.createContext(userName,
                                                                     password);

        TemporaryTopic tempT = jmsContextTCFBindings.createTemporaryTopic();

        if (tempT.getTopicName() != (null)) {
            exceptionFlag = false;
        } else {

            exceptionFlag = true;
        }
        if (exceptionFlag == true)
            throw new WrongException("testGetTemporaryTopicSecOnBinding failed");
        jmsContextTCFBindings.close();

    }

    public void testGetTemporaryTopicSecOnTCPIP(HttpServletRequest request,
                                                HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        String userName = "user1";
        String password = "user1pwd";
        JMSContext jmsContextTCFTCP = tcfTCP.createContext(userName, password);

        TemporaryTopic tempT = jmsContextTCFTCP.createTemporaryTopic();

        if (tempT.getTopicName() != (null)) {
            exceptionFlag = false;
        } else {

            exceptionFlag = true;
        }
        if (exceptionFlag == true)
            throw new WrongException("testGetTemporaryTopicSecOnTCPIP failed");
        jmsContextTCFTCP.close();

    }

    public void testToStringTemporaryTopicSecOnBinding(
                                                       HttpServletRequest request, HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        String userName = "user1";
        String password = "user1pwd";
        JMSContext jmsContextTCFBindings = tcfBindings.createContext(userName,
                                                                     password);

        TemporaryTopic tempT = jmsContextTCFBindings.createTemporaryTopic();

        if (tempT.toString() != (null)) {
            exceptionFlag = false;
        } else {

            exceptionFlag = true;
        }
        if (exceptionFlag == true)
            throw new WrongException("testToStringTemporaryTopicSecOnBinding failed");

        jmsContextTCFBindings.close();

    }

    public void testToStringeTemporaryTopicSecOnTCPIP(
                                                      HttpServletRequest request, HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        String userName = "user1";
        String password = "user1pwd";
        JMSContext jmsContextTCFTCP = tcfTCP.createContext(userName, password);

        TemporaryTopic tempT = jmsContextTCFTCP.createTemporaryTopic();

        if (tempT.toString() != (null)) {
            exceptionFlag = false;
        } else {

            exceptionFlag = true;
        }
        if (exceptionFlag == true)
            throw new WrongException("testToStringeTemporaryTopicSecOnTCPIP failed");
        jmsContextTCFTCP.close();

    }

    public void testDeleteTemporaryTopicSecOnBinding(
                                                     HttpServletRequest request, HttpServletResponse response) throws Throwable {
        exceptionFlag = false;
        try {
            String userName = "user1";
            String password = "user1pwd";
            JMSContext jmsContextTCFBindings = tcfBindings.createContext(
                                                                         userName, password);

            TemporaryTopic tempT = jmsContextTCFBindings.createTemporaryTopic();

            jmsContextTCFBindings.close();
            tempT.delete();

        } catch (Exception e) {

            e.printStackTrace();
            exceptionFlag = true;
        }
        if (exceptionFlag == true)
            throw new WrongException("testDeleteTemporaryTopicSecOnBinding failed");

    }

    public void testDeleteTemporaryTopicSecOnTCPIP(HttpServletRequest request,
                                                   HttpServletResponse response) throws Throwable {
        exceptionFlag = false;
        try {
            String userName = "user1";
            String password = "user1pwd";
            JMSContext jmsContextTCFTCP = tcfTCP.createContext(userName,
                                                               password);

            TemporaryTopic tempT = jmsContextTCFTCP.createTemporaryTopic();

            jmsContextTCFTCP.close();
            tempT.delete();

        } catch (Exception e) {

            e.printStackTrace();
            exceptionFlag = true;
        }
        if (exceptionFlag == true)
            throw new WrongException("testDeleteTemporaryTopicSecOnTCPIP failed");

    }

    public void testDeleteExceptionTemporaryTopicSecOn_B(
                                                         HttpServletRequest request, HttpServletResponse response) throws Throwable {
        exceptionFlag = false;
        try {
            String userName = "user1";
            String password = "user1pwd";
            JMSContext jmsContextTCFBindings = tcfBindings.createContext(
                                                                         userName, password);

            TemporaryTopic tempT = jmsContextTCFBindings.createTemporaryTopic();
            JMSConsumer jmsConsumer = jmsContextTCFBindings.createConsumer(tempT);

            tempT.delete();

            jmsConsumer.close();
            jmsContextTCFBindings.close();

        } catch (Exception e) {

            e.printStackTrace();
            exceptionFlag = true;
        }
        if (exceptionFlag == false)
            throw new WrongException("testDeleteExceptionTemporaryTopicSecOn_B failed");

    }

    public void testDeleteExceptionTemporaryTopicSecOn_TCPIP(
                                                             HttpServletRequest request, HttpServletResponse response) throws Throwable {
        exceptionFlag = false;
        try {
            String userName = "user1";
            String password = "user1pwd";
            JMSContext jmsContextTCFTCP = tcfTCP.createContext(userName,
                                                               password);

            TemporaryTopic tempT = jmsContextTCFTCP.createTemporaryTopic();
            JMSConsumer jmsConsumer = jmsContextTCFTCP.createConsumer(tempT);

            tempT.delete();

            jmsConsumer.close();
            jmsContextTCFTCP.close();

        } catch (Exception e) {

            e.printStackTrace();
            exceptionFlag = true;
        }
        if (exceptionFlag == false)
            throw new WrongException("testDeleteExceptionTemporaryTopicSecOn_TCPIP failed");

    }

    public void testTemporaryTopicPubSubSecOn_B(HttpServletRequest request,
                                                HttpServletResponse response) throws Throwable {
        exceptionFlag = false;

        String userName = "user1";

        String password = "user1pwd";
        JMSContext jmsContextTCFBindings = tcfBindings.createContext(userName,
                                                                     password);

        TemporaryTopic tempT = jmsContextTCFBindings.createTemporaryTopic();

        JMSProducer jmsProducerTCFBindings = jmsContextTCFBindings.createProducer();

        JMSConsumer jmsConsumerTCFBindings = jmsContextTCFBindings.createConsumer(tempT);

        jmsProducerTCFBindings.send(tempT, "hello world");

        TextMessage recMessage = (TextMessage) jmsConsumerTCFBindings.receive();

        if (recMessage.getText() == "hello world") {
            exceptionFlag = false;
        } else {

            exceptionFlag = true;
        }

        if (exceptionFlag == true)
            throw new WrongException("testTemporaryTopicPubSubSecOn_B failed");

        jmsConsumerTCFBindings.close();
        jmsContextTCFBindings.close();

    }

    public void testTemporaryTopicPubSubSecOn_TCPIP(HttpServletRequest request,
                                                    HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        String userName = "user1";
        String password = "user1pwd";
        JMSContext jmsContextTCFTCP = tcfTCP.createContext(userName, password);

        TemporaryTopic tempT = jmsContextTCFTCP.createTemporaryTopic();

        JMSProducer jmsProducerTCFTCP = jmsContextTCFTCP.createProducer();

        JMSConsumer jmsConsumerTCFTCP = jmsContextTCFTCP.createConsumer(tempT);

        jmsProducerTCFTCP.send(tempT, "hello world");

        TextMessage recMessage = (TextMessage) jmsConsumerTCFTCP.receive();

        if (recMessage.getText().equals("hello world")) {
            exceptionFlag = false;
        } else {

            exceptionFlag = true;
        }

        if (exceptionFlag == true)
            throw new WrongException("testTemporaryTopicPubSubSecOn_TCPIP failed");

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

    public class WrongException extends Exception {

        String str;

        public WrongException(String str) {
            this.str = str;
            System.out.println("==== Test failed=== in WrongException ============");
        }

        @Override
        public String toString() {
            return "This is not the expected exception" + " " + str;
        }

    }

}
