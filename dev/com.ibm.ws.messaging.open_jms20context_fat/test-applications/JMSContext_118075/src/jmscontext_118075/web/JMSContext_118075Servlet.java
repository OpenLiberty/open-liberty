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
package jmscontext_118075.web;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.lang.reflect.InvocationTargetException;
import java.util.Enumeration;

import javax.jms.InvalidDestinationRuntimeException;
import javax.jms.InvalidSelectorRuntimeException;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.JMSProducer;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.jms.QueueConnectionFactory;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.jms.TopicConnectionFactory;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

@SuppressWarnings("serial")
public class JMSContext_118075Servlet extends HttpServlet {
    public static QueueConnectionFactory jmsQCFBindings;
    public static QueueConnectionFactory jmsQCFTCP;
    public static TopicConnectionFactory jmsTCFBindings;
    public static TopicConnectionFactory jmsTCFTCP;
    public static Topic jmsTopic;
    public static Queue jmsQueue;
    public static Queue jmsQueue1;
    private static int DEFAULT_TIMEOUT = 30000;

    public static boolean exceptionFlag;

    @Override
    public void init() throws ServletException {
        // TODO Auto-generated method stub

        super.init();
        try {
            jmsQCFBindings = getQCFBindings();
            jmsQCFTCP = getQCFTCP();
            jmsQueue = getQueue("jndi_INPUT_Q");
            jmsQueue1 = getQueue("eis/queue2");
            jmsTCFBindings = getTCFBindings();
            jmsTCFTCP = getTCFTCP();
            jmsTopic = getTopic();

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
        final TraceComponent tc = Tr.register(JMSContext_118075Servlet.class); // injection
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

    public void testQueueConsumer_B_SecOff(HttpServletRequest request,
                                           HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContext = jmsQCFBindings.createContext();
        emptyQueue(jmsQCFBindings, jmsQueue1);
        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue1);
        jmsContext.createProducer().send(jmsQueue1, "test message");

        TextMessage msg = (TextMessage) jmsConsumer.receive(DEFAULT_TIMEOUT);
        System.out.println(msg.getText());

        jmsContext.close();

        if (!(msg != null && msg.getText().equals("test message")))
            exceptionFlag = true;

        if (exceptionFlag)
            throw new WrongException("testQueueConsumer_B_SecOff failed: Expected message was not received");

    }

    public void testQueueConsumer_TcpIp_SecOff(HttpServletRequest request,
                                               HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContext = jmsQCFTCP.createContext();
        emptyQueue(jmsQCFTCP, jmsQueue);
        JMSProducer jmsProducer = jmsContext.createProducer().send(jmsQueue, "testQueueConsumer_TcpIp_SecOff");
        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);

        TextMessage msg = (TextMessage) jmsConsumer.receive(DEFAULT_TIMEOUT);
        System.out.println(msg.getText());

        jmsContext.close();

        if (!(msg != null && msg.getText().equals("testQueueConsumer_TcpIp_SecOff")))
            exceptionFlag = true;

        if (exceptionFlag)
            throw new WrongException("testQueueConsumer_TcpIp_SecOff failed: Expected message was not received");

    }

    public void testTopicConsumer_B_SecOff(HttpServletRequest request,
                                           HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContext = jmsTCFBindings.createContext();
        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsTopic);
        jmsContext.createProducer().send(jmsTopic, "testTopicConsumer_B_SecOff");

        TextMessage msg = (TextMessage) jmsConsumer.receive(DEFAULT_TIMEOUT);
        System.out.println(msg.getText());

        jmsConsumer.close();
        jmsContext.close();

        if (!(msg != null && msg.getText().equals("testTopicConsumer_B_SecOff")))
            exceptionFlag = true;

        if (exceptionFlag)
            throw new WrongException("testTopicConsumer_B_SecOff failed: Expected message was not received");

    }

    public void testTopicConsumer_TcpIp_SecOff(HttpServletRequest request,
                                               HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContext = jmsTCFTCP.createContext();
        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsTopic);
        jmsContext.createProducer().send(jmsTopic, "testTopicConsumer_TcpIp_SecOff");

        TextMessage msg = (TextMessage) jmsConsumer.receive(DEFAULT_TIMEOUT);
        System.out.println(msg.getText());

        jmsConsumer.close();
        jmsContext.close();

        if (!(msg != null && msg.getText().equals("testTopicConsumer_TcpIp_SecOff")))
            exceptionFlag = true;

        if (exceptionFlag)
            throw new WrongException("testTopicConsumer_TcpIp_SecOff failed: Expected message was not received");
    }

    public void testCreateConsumerInvalidDest_B_SecOff(HttpServletRequest request,
                                                       HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContext = jmsQCFBindings.createContext();
        Queue queue = (Queue) new InitialContext()
                        .lookup("java:comp/env/eis/queue11");
        try {
            jmsContext.createConsumer(queue);
        } catch (InvalidDestinationRuntimeException e) {
            System.out.println("Expected InvalidDestinationRuntimeException found in testCreateConsumerInvalidDest_B_SecOff");
            exceptionFlag = true;
            e.printStackTrace();
        }
        jmsContext.close();

        if (!exceptionFlag)
            throw new WrongException("testCreateConsumerInvalidDest_B_SecOff failed: Expected exception was not seen");

    }

    public void testCreateConsumerInvalidDest_TcpIp_SecOff(HttpServletRequest request,
                                                           HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContext = jmsQCFTCP.createContext();
        Queue queue = (Queue) new InitialContext()
                        .lookup("java:comp/env/eis/queue11");
        try {
            jmsContext.createConsumer(queue);
        } catch (InvalidDestinationRuntimeException e) {
            System.out.println("Expected InvalidDestinationRuntimeException found in testCreateConsumerInvalidDest_TcpIp_SecOff");
            exceptionFlag = true;
            e.printStackTrace();
        }
        jmsContext.close();

        if (!exceptionFlag)
            throw new WrongException("testCreateConsumerInvalidDest_TcpIp_SecOff failed: Expected exception was not seen");

    }

    public void testCreateConsumerInvalidDest_Topic_B_SecOff(HttpServletRequest request,
                                                             HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContext = jmsTCFBindings.createContext();
        Topic topic = (Topic) new InitialContext()
                        .lookup("java:comp/env/eis/topic11");
        try {
            jmsContext.createConsumer(topic);
        } catch (InvalidDestinationRuntimeException e) {
            System.out.println("Expected InvalidDestinationRuntimeException found in testCreateConsumerInvalidDest_Topic_B_SecOff");
            exceptionFlag = true;
            e.printStackTrace();
        }
        jmsContext.close();

        if (!exceptionFlag)
            throw new WrongException("testCreateConsumerInvalidDest_Topic_B_SecOff failed: Expected exception was not seen");

    }

    public void testCreateConsumerInvalidDest_Topic_TcpIp_SecOff(HttpServletRequest request,
                                                                 HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContext = jmsTCFTCP.createContext();
        Topic topic = (Topic) new InitialContext()
                        .lookup("java:comp/env/eis/topic11");
        try {
            jmsContext.createConsumer(topic);
        } catch (InvalidDestinationRuntimeException e) {
            System.out.println("Expected InvalidDestinationRuntimeException found in testCreateConsumerInvalidDest_Topic_TcpIp_SecOff");
            exceptionFlag = true;
            e.printStackTrace();
        }
        jmsContext.close();

        if (!exceptionFlag)
            throw new WrongException("testCreateConsumerInvalidDest_Topic_TcpIp_SecOff failed: Expected exception was not seen");
    }

    public void testCreateConsumerWithMsgSelector_B_SecOff(HttpServletRequest request,
                                                           HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContext = jmsQCFBindings.createContext();
        emptyQueue(jmsQCFBindings, jmsQueue);
        JMSProducer jmsProducer = jmsContext.createProducer();
        TextMessage msg = jmsContext.createTextMessage("testCreateConsumerWithMsgSelector_B_SecOff");
        msg.setStringProperty("Team", "SIB");
        jmsProducer.send(jmsQueue, msg);
        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue, "Team = 'SIB'");

        TextMessage rec_msg = (TextMessage) jmsConsumer.receive(DEFAULT_TIMEOUT);
        System.out.println("Message is : " + rec_msg.getText() + " and property is - Team = " + rec_msg.getStringProperty("Team"));

        jmsContext.close();

        if (!(rec_msg != null && rec_msg.getText().equals("testCreateConsumerWithMsgSelector_B_SecOff")) && rec_msg.getStringProperty("Team").equals("SIB"))
            exceptionFlag = true;

        if (exceptionFlag)
            throw new WrongException("testCreateConsumerWithMsgSelector_B_SecOff failed: Expected message or property value was not received");

    }

    public void testCreateConsumerWithMsgSelector_TcpIp_SecOff(HttpServletRequest request,
                                                               HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContext = jmsQCFTCP.createContext();
        emptyQueue(jmsQCFTCP, jmsQueue);
        JMSProducer jmsProducer = jmsContext.createProducer();
        TextMessage send_msg = jmsContext.createTextMessage("testCreateConsumerWithMsgSelector_TcpIp_SecOff");
        send_msg.setStringProperty("Team", "SIB");
        jmsProducer.send(jmsQueue, send_msg);
        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue, "Team = 'SIB'");

        TextMessage rec_msg = (TextMessage) jmsConsumer.receive(DEFAULT_TIMEOUT);
        System.out.println("Message is : " + rec_msg.getText() + " and property is - Team = " + rec_msg.getStringProperty("Team"));

        jmsContext.close();

        if (!(rec_msg != null && rec_msg.getText().equals("testCreateConsumerWithMsgSelector_TcpIp_SecOff")) && rec_msg.getStringProperty("Team").equals("SIB"))
            exceptionFlag = true;

        if (exceptionFlag)
            throw new WrongException("testCreateConsumerWithMsgSelector_TcpIp_SecOff failed: Expected message or property value was not received");
    }

    public void testCreateConsumerWithMsgSelectorTopic_B_SecOff(HttpServletRequest request,
                                                                HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContext = jmsTCFBindings.createContext();
        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsTopic, "Team = 'SIB'");
        JMSProducer jmsProducer = jmsContext.createProducer();
        TextMessage m = jmsContext.createTextMessage("testCreateConsumerWithMsgSelectorTopic_B_SecOff");
        m.setStringProperty("Team", "SIB");
        jmsProducer.send(jmsTopic, m);

        TextMessage msg = (TextMessage) jmsConsumer.receive(DEFAULT_TIMEOUT);
        System.out.println("Message is : " + msg.getText() + " and property is - Team = " + msg.getStringProperty("Team"));

        jmsConsumer.close();
        jmsContext.close();

        if (!(msg != null && msg.getText().equals("testCreateConsumerWithMsgSelectorTopic_B_SecOff")) && msg.getStringProperty("Team").equals("SIB"))
            exceptionFlag = true;

        if (exceptionFlag)
            throw new WrongException("testCreateConsumerWithMsgSelectorTopic_B_SecOff failed: Expected message or property value was not received");

    }

    public void testCreateConsumerWithMsgSelectorTopic_TcpIp_SecOff(HttpServletRequest request,
                                                                    HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContext = jmsTCFTCP.createContext();
        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsTopic, "Team = 'SIB'");
        JMSProducer jmsProducer = jmsContext.createProducer();
        TextMessage m = jmsContext.createTextMessage("testCreateConsumerWithMsgSelectorTopic_TcpIp_SecOff");
        m.setStringProperty("Team", "SIB");
        jmsProducer.send(jmsTopic, m);

        TextMessage msg = (TextMessage) jmsConsumer.receive(DEFAULT_TIMEOUT);
        System.out.println("Message is : " + msg.getText() + " and property is - Team = " + msg.getStringProperty("Team"));

        jmsConsumer.close();
        jmsContext.close();

        if (!(msg != null && msg.getText().equals("testCreateConsumerWithMsgSelectorTopic_TcpIp_SecOff")) && msg.getStringProperty("Team").equals("SIB"))
            exceptionFlag = true;

        if (exceptionFlag)
            throw new WrongException("testCreateConsumerWithMsgSelectorTopic_TcpIp_SecOff failed: Expected message or property value was not received");

    }

    public void testCreateConsumerWithMsgSelectorInvalidDest_B_SecOff(HttpServletRequest request,
                                                                      HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContext = jmsQCFBindings.createContext();
        Queue queue = (Queue) new InitialContext()
                        .lookup("java:comp/env/eis/queue11");
        try {
            jmsContext.createConsumer(queue, "Team = 'SIB'");
        } catch (InvalidDestinationRuntimeException e) {
            System.out.println("Expected InvalidDestinationRuntimeException found in testCreateConsumerWithMsgSelectorInvalidDest_B_SecOff");
            exceptionFlag = true;
            e.printStackTrace();
        }
        jmsContext.close();

        if (!exceptionFlag)
            throw new WrongException("testCreateConsumerWithMsgSelectorInvalidDest_B_SecOff failed: Expected exception was not seen");

    }

    public void testCreateConsumerWithMsgSelectorInvalidDest_TcpIp_SecOff(HttpServletRequest request,
                                                                          HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContext = jmsQCFTCP.createContext();
        Queue queue = (Queue) new InitialContext()
                        .lookup("java:comp/env/eis/queue11");
        try {
            jmsContext.createConsumer(queue, "Team = 'SIB'");
        } catch (InvalidDestinationRuntimeException e) {
            System.out.println("Expected InvalidDestinationRuntimeException found in testCreateConsumerWithMsgSelectorInvalidDest_TcpIp_SecOff");
            exceptionFlag = true;
            e.printStackTrace();
        }
        jmsContext.close();

        if (!exceptionFlag)
            throw new WrongException("testCreateConsumerWithMsgSelectorInvalidDest_TcpIp_SecOff failed: Expected exception was not seen");
    }

    public void testCreateConsumerWithMsgSelectorInvalidDest_Topic_B_SecOff(HttpServletRequest request,
                                                                            HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContext = jmsTCFBindings.createContext();
        Topic topic = (Topic) new InitialContext()
                        .lookup("java:comp/env/eis/topic11");
        try {
            jmsContext.createConsumer(topic, "Team = 'SIB'");
        } catch (InvalidDestinationRuntimeException e) {
            System.out.println("Expected InvalidDestinationRuntimeException found in testCreateConsumerWithMsgSelectorInvalidDest_Topic_B_SecOff");
            exceptionFlag = true;
            e.printStackTrace();
        }
        jmsContext.close();

        if (!exceptionFlag)
            throw new WrongException("testCreateConsumerWithMsgSelectorInvalidDest_Topic_B_SecOff failed: Expected exception was not seen");
    }

    public void testCreateConsumerWithMsgSelectorInvalidDest_Topic_TcpIp_SecOff(HttpServletRequest request,
                                                                                HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContext = jmsTCFTCP.createContext();
        Topic topic = (Topic) new InitialContext()
                        .lookup("java:comp/env/eis/topic11");
        try {
            jmsContext.createConsumer(topic, "Team = 'SIB'");
        } catch (InvalidDestinationRuntimeException e) {
            System.out.println("Expected InvalidDestinationRuntimeException found in testCreateConsumerWithMsgSelectorInvalidDest_Topic_TcpIp_SecOff");
            exceptionFlag = true;
            e.printStackTrace();
        }
        jmsContext.close();

        if (!exceptionFlag)
            throw new WrongException("testCreateConsumerWithMsgSelectorInvalidDest_Topic_TcpIp_SecOff failed: Expected exception was not seen");
    }

    public void testCreateConsumerWithMsgSelectorNullDest_B_SecOff(HttpServletRequest request,
                                                                   HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContext = jmsQCFBindings.createContext();

        try {
            jmsContext.createConsumer(null, "Team = 'SIB'");
        } catch (InvalidDestinationRuntimeException e) {
            System.out.println("Expected InvalidDestinationRuntimeException found in testCreateConsumerWithMsgSelectorNullDest_B_SecOff");
            exceptionFlag = true;
            e.printStackTrace();
        }
        jmsContext.close();

        if (!exceptionFlag)
            throw new WrongException("testCreateConsumerWithMsgSelectorNullDest_B_SecOff failed: Expected exception was not seen");

    }

    public void testCreateConsumerWithMsgSelectorNullDest_TcpIp_SecOff(HttpServletRequest request,
                                                                       HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContext = jmsQCFTCP.createContext();

        try {
            jmsContext.createConsumer(null, "Team = 'SIB'");
        } catch (InvalidDestinationRuntimeException e) {
            System.out.println("Expected InvalidDestinationRuntimeException found in testCreateConsumerWithMsgSelectorNullDest_TcpIp_SecOff");
            exceptionFlag = true;
            e.printStackTrace();
        }
        jmsContext.close();

        if (!exceptionFlag)
            throw new WrongException("testCreateConsumerWithMsgSelectorNullDest_TcpIp_SecOff failed: Expected exception was not seen");
    }

    public void testCreateConsumerWithMsgSelectorNullDest_Topic_B_SecOff(HttpServletRequest request,
                                                                         HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContext = jmsTCFBindings.createContext();

        try {
            jmsContext.createConsumer(null, "Team = 'SIB'");
        } catch (InvalidDestinationRuntimeException e) {
            System.out.println("Expected InvalidDestinationRuntimeException found in testCreateConsumerWithMsgSelectorNullDest_Topic_B_SecOff");
            exceptionFlag = true;
            e.printStackTrace();
        }
        jmsContext.close();

        if (!exceptionFlag)
            throw new WrongException("testCreateConsumerWithMsgSelectorNullDest_Topic_B_SecOff failed: Expected exception was not seen");
    }

    public void testCreateConsumerWithMsgSelectorNullDest_Topic_TcpIp_SecOff(HttpServletRequest request,
                                                                             HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContext = jmsTCFTCP.createContext();

        try {
            jmsContext.createConsumer(null, "Team = 'SIB'");
        } catch (InvalidDestinationRuntimeException e) {
            System.out.println("Expected InvalidDestinationRuntimeException found in testCreateConsumerWithMsgSelectorNullDest_Topic_TcpIp_SecOff");
            exceptionFlag = true;
            e.printStackTrace();
        }
        jmsContext.close();

        if (!exceptionFlag)
            throw new WrongException("testCreateConsumerWithMsgSelectorNullDest_Topic_TcpIp_SecOff failed: Expected exception was not seen");
    }

    public void testCreateConsumerWithNullMsgSelectorNullDest_B_SecOff(HttpServletRequest request,
                                                                       HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContext = jmsQCFBindings.createContext();

        try {
            jmsContext.createConsumer(null, null);
        } catch (InvalidDestinationRuntimeException e) {
            System.out.println("Expected InvalidDestinationRuntimeException found in testCreateConsumerWithNullMsgSelectorNullDest_B_SecOff");
            exceptionFlag = true;
            e.printStackTrace();
        }
        jmsContext.close();

        if (!exceptionFlag)
            throw new WrongException("testCreateConsumerWithNullMsgSelectorNullDest_B_SecOff failed: Expected exception was not seen");
    }

    public void testCreateConsumerWithNullMsgSelectorNullDest_TcpIp_SecOff(HttpServletRequest request,
                                                                           HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContext = jmsQCFTCP.createContext();

        try {
            jmsContext.createConsumer(null, null);
        } catch (InvalidDestinationRuntimeException e) {
            System.out.println("Expected InvalidDestinationRuntimeException found in testCreateConsumerWithNullMsgSelectorNullDest_TcpIp_SecOff");
            exceptionFlag = true;
            e.printStackTrace();
        }
        jmsContext.close();

        if (!exceptionFlag)
            throw new WrongException("testCreateConsumerWithNullMsgSelectorNullDest_TcpIp_SecOff failed: Expected exception was not seen");
    }

    public void testCreateConsumerWithNullMsgSelectorNullDest_Topic_B_SecOff(HttpServletRequest request,
                                                                             HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContext = jmsTCFBindings.createContext();

        try {
            jmsContext.createConsumer(null, null);
        } catch (InvalidDestinationRuntimeException e) {
            System.out.println("Expected InvalidDestinationRuntimeException found in testCreateConsumerWithNullMsgSelectorNullDest_Topic_B_SecOff");
            exceptionFlag = true;
            e.printStackTrace();
        }
        jmsContext.close();

        if (!exceptionFlag)
            throw new WrongException("testCreateConsumerWithNullMsgSelectorNullDest_Topic_B_SecOff failed: Expected exception was not seen");
    }

    public void testCreateConsumerWithNullMsgSelectorNullDest_Topic_TcpIp_SecOff(HttpServletRequest request,
                                                                                 HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContext = jmsTCFTCP.createContext();

        try {
            jmsContext.createConsumer(null, null);
        } catch (InvalidDestinationRuntimeException e) {
            System.out.println("Expected InvalidDestinationRuntimeException found in testCreateConsumerWithNullMsgSelectorNullDest_Topic_TcpIp_SecOff");
            exceptionFlag = true;
            e.printStackTrace();
        }
        jmsContext.close();

        if (!exceptionFlag)
            throw new WrongException("testCreateConsumerWithNullMsgSelectorNullDest_Topic_TcpIp_SecOff failed: Expected exception was not seen");
    }

    public void testCreateConsumerNullDest_B_SecOff(HttpServletRequest request,
                                                    HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContext = jmsQCFBindings.createContext();

        try {
            jmsContext.createConsumer(null);
        } catch (InvalidDestinationRuntimeException e) {
            System.out.println("Expected InvalidDestinationRuntimeException found in testCreateConsumerNullDest_B_SecOff");
            exceptionFlag = true;
            e.printStackTrace();
        }
        jmsContext.close();

        if (!exceptionFlag)
            throw new WrongException("testCreateConsumerNullDest_B_SecOff failed: Expected exception was not seen");

    }

    public void testCreateConsumerNullDest_TcpIp_SecOff(HttpServletRequest request,
                                                        HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContext = jmsQCFTCP.createContext();

        try {
            jmsContext.createConsumer(null);
        } catch (InvalidDestinationRuntimeException e) {
            System.out.println("Expected InvalidDestinationRuntimeException found in testCreateConsumerNullDest_TcpIp_SecOff");
            exceptionFlag = true;
            e.printStackTrace();
        }
        jmsContext.close();

        if (!exceptionFlag)
            throw new WrongException("testCreateConsumerNullDest_TcpIp_SecOff failed: Expected exception was not seen");

    }

    public void testCreateConsumerNullDest_Topic_B_SecOff(HttpServletRequest request,
                                                          HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContext = jmsTCFBindings.createContext();

        try {
            jmsContext.createConsumer(null);
        } catch (InvalidDestinationRuntimeException e) {
            System.out.println("Expected InvalidDestinationRuntimeException found in testCreateConsumerNullDest_Topic_B_SecOff");
            exceptionFlag = true;
            e.printStackTrace();
        }
        jmsContext.close();

        if (!exceptionFlag)
            throw new WrongException("testCreateConsumerNullDest_Topic_B_SecOff failed: Expected exception was not seen");

    }

    public void testCreateConsumerNullDest_Topic_TcpIp_SecOff(HttpServletRequest request,
                                                              HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContext = jmsTCFTCP.createContext();

        try {
            jmsContext.createConsumer(null);
        } catch (InvalidDestinationRuntimeException e) {
            System.out.println("Expected InvalidDestinationRuntimeException found in testCreateConsumerNullDest_Topic_TcpIp_SecOff");
            exceptionFlag = true;
            e.printStackTrace();
        }
        jmsContext.close();

        if (!exceptionFlag)
            throw new WrongException("testCreateConsumerNullDest_Topic_TcpIp_SecOff failed: Expected exception was not seen");
    }

    public void testCreateConsumerWithInvalidMsgSelector_B_SecOff(HttpServletRequest request,
                                                                  HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContext = jmsQCFBindings.createContext();

        try {
            jmsContext.createConsumer(jmsQueue, "bad selector");
        } catch (InvalidSelectorRuntimeException e) {
            System.out.println("Expected InvalidSelectorRuntimeException found in testCreateConsumerWithInvalidMsgSelector_B_SecOff");
            exceptionFlag = true;
            e.printStackTrace();
        }
        jmsContext.close();

        if (!exceptionFlag)
            throw new WrongException("testCreateConsumerWithInvalidMsgSelector_B_SecOff failed: Expected exception was not seen");

    }

    public void testCreateConsumerWithInvalidMsgSelector_TcpIp_SecOff(HttpServletRequest request,
                                                                      HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContext = jmsQCFTCP.createContext();

        try {
            jmsContext.createConsumer(jmsQueue, "bad selector");
        } catch (InvalidSelectorRuntimeException e) {
            System.out.println("Expected InvalidSelectorRuntimeException found in testCreateConsumerWithInvalidMsgSelector_TcpIp_SecOff");
            exceptionFlag = true;
            e.printStackTrace();
        }
        jmsContext.close();

        if (!exceptionFlag)
            throw new WrongException("testCreateConsumerWithInvalidMsgSelector_TcpIp_SecOff failed: Expected exception was not seen");

    }

    public void testCreateConsumerWithInvalidMsgSelector_Topic_B_SecOff(HttpServletRequest request,
                                                                        HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContext = jmsTCFBindings.createContext();

        try {
            jmsContext.createConsumer(jmsTopic, "bad selector");
        } catch (InvalidSelectorRuntimeException e) {
            System.out.println("Expected InvalidSelectorRuntimeException found in testCreateConsumerWithInvalidMsgSelector_Topic_B_SecOff");
            exceptionFlag = true;
            e.printStackTrace();
        }
        jmsContext.close();

        if (!exceptionFlag)
            throw new WrongException("testCreateConsumerWithInvalidMsgSelector_Topic_B_SecOff failed: Expected exception was not seen");

    }

    public void testCreateConsumerWithInvalidMsgSelector_Topic_TcpIp_SecOff(HttpServletRequest request,
                                                                            HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContext = jmsTCFTCP.createContext();

        try {
            jmsContext.createConsumer(jmsTopic, "bad selector");
        } catch (InvalidSelectorRuntimeException e) {
            System.out.println("Expected InvalidSelectorRuntimeException found in testCreateConsumerWithInvalidMsgSelector_Topic_TcpIp_SecOff");
            exceptionFlag = true;
            e.printStackTrace();
        }
        jmsContext.close();

        if (!exceptionFlag)
            throw new WrongException("testCreateConsumerWithInvalidMsgSelector_Topic_TcpIp_SecOff failed: Expected exception was not seen");

    }

    public void testCreateConsumerWithMsgSelectorNoLocal_B_SecOff(HttpServletRequest request,
                                                                  HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContext = jmsQCFBindings.createContext();
        emptyQueue(jmsQCFBindings, jmsQueue);
        JMSProducer jmsProducer = jmsContext.createProducer();
        TextMessage send_msg = jmsContext.createTextMessage("testCreateConsumerWithMsgSelectorNoLocal_B_SecOff");
        send_msg.setStringProperty("Team", "SIB");
        jmsProducer.send(jmsQueue, send_msg);
        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue, "Team = 'SIB'", false);

        TextMessage rec_msg = (TextMessage) jmsConsumer.receive(DEFAULT_TIMEOUT);
        System.out.println("Message is : " + rec_msg.getText() + " and property is - Team = " + rec_msg.getStringProperty("Team"));

        jmsContext.close();

        if (!(rec_msg != null && rec_msg.getText().equals("testCreateConsumerWithMsgSelectorNoLocal_B_SecOff")) && rec_msg.getStringProperty("Team").equals("SIB"))
            exceptionFlag = true;

        if (exceptionFlag)
            throw new WrongException("testCreateConsumerWithMsgSelectorNoLocal_B_SecOff failed: Expected message or property value was not received");

    }

    public void testCreateConsumerWithMsgSelectorNoLocal_TcpIp_SecOff(HttpServletRequest request,
                                                                      HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContext = jmsQCFTCP.createContext();
        emptyQueue(jmsQCFTCP, jmsQueue);
        JMSProducer jmsProducer = jmsContext.createProducer();
        TextMessage send_msg = jmsContext.createTextMessage("testCreateConsumerWithMsgSelectorNoLocal_TcpIp_SecOff");
        send_msg.setStringProperty("Team", "SIB");
        jmsProducer.send(jmsQueue, send_msg);
        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue, "Team = 'SIB'", false);

        TextMessage rec_msg = (TextMessage) jmsConsumer.receive(DEFAULT_TIMEOUT);
        System.out.println("Message is : " + rec_msg.getText() + " and property is - Team = " + rec_msg.getStringProperty("Team"));

        jmsContext.close();

        if (!(rec_msg != null && rec_msg.getText().equals("testCreateConsumerWithMsgSelectorNoLocal_TcpIp_SecOff")) && rec_msg.getStringProperty("Team").equals("SIB"))
            exceptionFlag = true;

        if (exceptionFlag)
            throw new WrongException("testCreateConsumerWithMsgSelectorNoLocal_TcpIp_SecOff failed: Expected message or property value was not received");

    }

    public void testCreateConsumerWithMsgSelectorNoLocalTopic_B_SecOff(HttpServletRequest request,
                                                                       HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContext = jmsTCFBindings.createContext();
        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsTopic, "Team = 'SIB'", false);
        JMSProducer jmsProducer = jmsContext.createProducer();
        TextMessage send_msg = jmsContext.createTextMessage("testCreateConsumerWithMsgSelectorNoLocalTopic_B_SecOff");
        send_msg.setStringProperty("Team", "SIB");
        jmsProducer.send(jmsTopic, send_msg);

        TextMessage rec_msg = (TextMessage) jmsConsumer.receive(DEFAULT_TIMEOUT);
        System.out.println("Message is : " + rec_msg.getText() + " and property is - Team = " + rec_msg.getStringProperty("Team"));

        jmsConsumer.close();
        jmsContext.close();

        if (!(rec_msg != null && rec_msg.getText().equals("testCreateConsumerWithMsgSelectorNoLocalTopic_B_SecOff")) && rec_msg.getStringProperty("Team").equals("SIB"))
            exceptionFlag = true;

        if (exceptionFlag)
            throw new WrongException("testCreateConsumerWithMsgSelectorNoLocalTopic_B_SecOff failed: Expected message or property value was not received");

    }

    public void testCreateConsumerWithMsgSelectorNoLocalTopic_TcpIp_SecOff(HttpServletRequest request,
                                                                           HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContext = jmsTCFTCP.createContext();
        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsTopic, "Team = 'SIB'", false);
        JMSProducer jmsProducer = jmsContext.createProducer();
        TextMessage send_msg = jmsContext.createTextMessage("testCreateConsumerWithMsgSelectorNoLocalTopic_TcpIp_SecOff");
        send_msg.setStringProperty("Team", "SIB");
        jmsProducer.send(jmsTopic, send_msg);

        TextMessage msg = (TextMessage) jmsConsumer.receive(DEFAULT_TIMEOUT);
        System.out.println("Message is : " + msg.getText() + " and property is - Team = " + msg.getStringProperty("Team"));

        jmsConsumer.close();
        jmsContext.close();

        if (!(msg != null && msg.getText().equals("testCreateConsumerWithMsgSelectorNoLocalTopic_TcpIp_SecOff")) && msg.getStringProperty("Team").equals("SIB"))
            exceptionFlag = true;

        if (exceptionFlag)
            throw new WrongException("testCreateConsumerWithMsgSelectorNoLocalTopic_TcpIp_SecOff failed: Expected message or property value was not received");

    }

    public void testCreateConsumerWithInvalidMsgSelectorNoLocal_B_SecOff(HttpServletRequest request,
                                                                         HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContext = jmsQCFBindings.createContext();

        try {
            jmsContext.createConsumer(jmsQueue, "bad selector", false);
        } catch (InvalidSelectorRuntimeException e) {
            System.out.println("Expected InvalidSelectorRuntimeException found in testCreateConsumerWithInvalidMsgSelectorNoLocal_B_SecOff");
            exceptionFlag = true;
            e.printStackTrace();
        }
        jmsContext.close();

        if (!exceptionFlag)
            throw new WrongException("testCreateConsumerWithInvalidMsgSelectorNoLocal_B_SecOff failed: Expected exception was not seen");

    }

    public void testCreateConsumerWithInvalidMsgSelectorNoLocal_TcpIp_SecOff(HttpServletRequest request,
                                                                             HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContext = jmsQCFTCP.createContext();

        try {
            jmsContext.createConsumer(jmsQueue, "bad selector", false);
        } catch (InvalidSelectorRuntimeException e) {
            System.out.println("Expected InvalidSelectorRuntimeException found in testCreateConsumerWithInvalidMsgSelectorNoLocal_TcpIp_SecOff");
            exceptionFlag = true;
            e.printStackTrace();
        }
        jmsContext.close();

        if (!exceptionFlag)
            throw new WrongException("testCreateConsumerWithInvalidMsgSelectorNoLocal_TcpIp_SecOff failed: Expected exception was not seen");

    }

    public void testCreateConsumerWithInvalidMsgSelectorNoLocal_Topic_B_SecOff(HttpServletRequest request,
                                                                               HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContext = jmsTCFBindings.createContext();

        try {
            jmsContext.createConsumer(jmsTopic, "bad selector", false);
        } catch (InvalidSelectorRuntimeException e) {
            System.out.println("Expected InvalidSelectorRuntimeException found in testCreateConsumerWithInvalidMsgSelectorNoLocal_Topic_B_SecOff");
            exceptionFlag = true;
            e.printStackTrace();
        }
        jmsContext.close();

        if (!exceptionFlag)
            throw new WrongException("testCreateConsumerWithInvalidMsgSelectorNoLocal_Topic_B_SecOff failed: Expected exception was not seen");

    }

    public void testCreateConsumerWithInvalidMsgSelectorNoLocal_Topic_TcpIp_SecOff(HttpServletRequest request,
                                                                                   HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContext = jmsTCFTCP.createContext();

        try {
            jmsContext.createConsumer(jmsTopic, "bad selector", false);
        } catch (InvalidSelectorRuntimeException e) {
            System.out.println("Expected InvalidSelectorRuntimeException found in testCreateConsumerWithInvalidMsgSelectorNoLocal_Topic_TcpIp_SecOff");
            exceptionFlag = true;
            e.printStackTrace();
        }
        jmsContext.close();

        if (!exceptionFlag)
            throw new WrongException("testCreateConsumerWithInvalidMsgSelectorNoLocal_Topic_TcpIp_SecOff failed: Expected exception was not seen");

    }

    public void testCreateConsumerWithMsgSelectorNoLocalNullDest_B_SecOff(HttpServletRequest request,
                                                                          HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContext = jmsQCFBindings.createContext();

        try {
            jmsContext.createConsumer(null, "Team = 'SIB'", false);
        } catch (InvalidDestinationRuntimeException e) {
            System.out.println("Expected InvalidDestinationRuntimeException found in testCreateConsumerWithMsgSelectorNoLocalNullDest_B_SecOff");
            exceptionFlag = true;
            e.printStackTrace();
        }
        jmsContext.close();

        if (!exceptionFlag)
            throw new WrongException("testCreateConsumerWithMsgSelectorNoLocalNullDest_B_SecOff failed: Expected exception was not seen");

    }

    public void testCreateConsumerWithMsgSelectorNoLocalNullDest_TcpIp_SecOff(HttpServletRequest request,
                                                                              HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContext = jmsQCFTCP.createContext();

        try {
            jmsContext.createConsumer(null, "Team = 'SIB'", false);
        } catch (InvalidDestinationRuntimeException e) {
            System.out.println("Expected InvalidDestinationRuntimeException found in testCreateConsumerWithMsgSelectorNoLocalNullDest_TcpIp_SecOff");
            exceptionFlag = true;
            e.printStackTrace();
        }
        jmsContext.close();

        if (!exceptionFlag)
            throw new WrongException("testCreateConsumerWithMsgSelectorNoLocalNullDest_TcpIp_SecOff failed: Expected exception was not seen");
    }

    public void testCreateConsumerWithMsgSelectorNoLocalNullDest_Topic_B_SecOff(HttpServletRequest request,
                                                                                HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContext = jmsTCFBindings.createContext();

        try {
            jmsContext.createConsumer(null, "Team = 'SIB'", false);
        } catch (InvalidDestinationRuntimeException e) {
            System.out.println("Expected InvalidDestinationRuntimeException found in testCreateConsumerWithMsgSelectorNoLocalNullDest_Topic_B_SecOff");
            exceptionFlag = true;
            e.printStackTrace();
        }
        jmsContext.close();

        if (!exceptionFlag)
            throw new WrongException("testCreateConsumerWithMsgSelectorNoLocalNullDest_Topic_B_SecOff failed: Expected exception was not seen");
    }

    public void testCreateConsumerWithMsgSelectorNoLocalNullDest_Topic_TcpIp_SecOff(HttpServletRequest request,
                                                                                    HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContext = jmsTCFTCP.createContext();

        try {
            jmsContext.createConsumer(null, "Team = 'SIB'", false);
        } catch (InvalidDestinationRuntimeException e) {
            System.out.println("Expected InvalidDestinationRuntimeException found in testCreateConsumerWithMsgSelectorNoLocalNullDest_Topic_TcpIp_SecOff");
            exceptionFlag = true;
            e.printStackTrace();
        }
        jmsContext.close();

        if (!exceptionFlag)
            throw new WrongException("testCreateConsumerWithMsgSelectorNoLocalNullDest_Topic_TcpIp_SecOff failed: Expected exception was not seen");
    }

    public void testCreateConsumerWithNullMsgSelectorNullDestNoLocal_B_SecOff(HttpServletRequest request,
                                                                              HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContext = jmsQCFBindings.createContext();

        try {
            jmsContext.createConsumer(null, null, false);
        } catch (InvalidDestinationRuntimeException e) {
            System.out.println("Expected InvalidDestinationRuntimeException found in testCreateConsumerWithNullMsgSelectorNullDestNoLocal_B_SecOff");
            exceptionFlag = true;
            e.printStackTrace();
        }
        jmsContext.close();

        if (!exceptionFlag)
            throw new WrongException("testCreateConsumerWithNullMsgSelectorNullDestNoLocal_B_SecOff failed: Expected exception was not seen");
    }

    public void testCreateConsumerWithNullMsgSelectorNullDestNoLocal_TcpIp_SecOff(HttpServletRequest request,
                                                                                  HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContext = jmsQCFTCP.createContext();

        try {
            jmsContext.createConsumer(null, null, false);
        } catch (InvalidDestinationRuntimeException e) {
            System.out.println("Expected InvalidDestinationRuntimeException found in testCreateConsumerWithNullMsgSelectorNullDestNoLocal_TcpIp_SecOff");
            exceptionFlag = true;
            e.printStackTrace();
        }
        jmsContext.close();

        if (!exceptionFlag)
            throw new WrongException("testCreateConsumerWithNullMsgSelectorNullDestNoLocal_TcpIp_SecOff failed: Expected exception was not seen");

    }

    public void testCreateConsumerWithNullMsgSelectorNullDestNoLocal_Topic_B_SecOff(HttpServletRequest request,
                                                                                    HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContext = jmsTCFBindings.createContext();

        try {
            jmsContext.createConsumer(null, null, false);
        } catch (InvalidDestinationRuntimeException e) {
            System.out.println("Expected InvalidDestinationRuntimeException found in testCreateConsumerWithNullMsgSelectorNullDestNoLocal_Topic_B_SecOff");
            exceptionFlag = true;
            e.printStackTrace();
        }
        jmsContext.close();

        if (!exceptionFlag)
            throw new WrongException("testCreateConsumerWithNullMsgSelectorNullDestNoLocal_Topic_B_SecOff failed: Expected exception was not seen");

    }

    public void testCreateConsumerWithNullMsgSelectorNullDestNoLocal_Topic_TcpIp_SecOff(HttpServletRequest request,
                                                                                        HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContext = jmsTCFTCP.createContext();

        try {
            jmsContext.createConsumer(null, null, false);
        } catch (InvalidDestinationRuntimeException e) {
            System.out.println("Expected InvalidDestinationRuntimeException found in testCreateConsumerWithNullMsgSelectorNullDestNoLocal_Topic_B_SecOff");
            exceptionFlag = true;
            e.printStackTrace();
        }
        jmsContext.close();

        if (!exceptionFlag)
            throw new WrongException("testCreateConsumerWithNullMsgSelectorNullDestNoLocal_Topic_B_SecOff failed: Expected exception was not seen");
    }

    public void testCreateConsumerWithNullMsgSelector_B_SecOff(HttpServletRequest request,
                                                               HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContext = jmsQCFBindings.createContext();
        emptyQueue(jmsQCFBindings, jmsQueue1);
        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue1, null);
        JMSProducer jmsProducer = jmsContext.createProducer();
        TextMessage msg = jmsContext.createTextMessage("testCreateConsumerWithNullMsgSelector_B_SecOff");
        msg.setStringProperty("MessageType", "text");
        jmsProducer.send(jmsQueue1, msg);

        TextMessage rec_msg = (TextMessage) jmsConsumer.receive(DEFAULT_TIMEOUT);
        jmsContext.close();

        if (!(rec_msg != null && rec_msg.getText().equals("testCreateConsumerWithNullMsgSelector_B_SecOff")))
            exceptionFlag = true;

        if (exceptionFlag)
            throw new WrongException("testCreateConsumerWithNullMsgSelector_B_SecOff failed: Expected message was not received");

    }

    public void testCreateConsumerWithNullMsgSelector_TcpIp_SecOff(HttpServletRequest request,
                                                                   HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContext = jmsQCFTCP.createContext();
        emptyQueue(jmsQCFTCP, jmsQueue1);
        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue1, null);
        JMSProducer jmsProducer = jmsContext.createProducer();
        TextMessage msg = jmsContext.createTextMessage("testCreateConsumerWithNullMsgSelector_B_SecOff");
        msg.setStringProperty("MessageType", "text");
        jmsProducer.send(jmsQueue1, msg);

        TextMessage rec_msg = (TextMessage) jmsConsumer.receive(DEFAULT_TIMEOUT);
        jmsContext.close();

        if (!(rec_msg != null && rec_msg.getText().equals("testCreateConsumerWithNullMsgSelector_B_SecOff")))
            exceptionFlag = true;

        if (exceptionFlag)
            throw new WrongException("testCreateConsumerWithNullMsgSelector_B_SecOff failed: Expected message was not received");

    }

    public void testCreateConsumerWithNullMsgSelector_Topic_B_SecOff(HttpServletRequest request,
                                                                     HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContext = jmsTCFBindings.createContext();
        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsTopic, null);
        JMSProducer jmsProducer = jmsContext.createProducer();
        TextMessage msg = jmsContext.createTextMessage("testCreateConsumerWithNullMsgSelector_Topic_B_SecOff");
        msg.setStringProperty("MessageType", "text");
        jmsProducer.send(jmsTopic, msg);

        TextMessage rec_msg = (TextMessage) jmsConsumer.receive();
        jmsConsumer.close();
        jmsContext.close();

        if (!(rec_msg != null && rec_msg.getText().equals("testCreateConsumerWithNullMsgSelector_Topic_B_SecOff")))
            exceptionFlag = true;

        if (exceptionFlag)
            throw new WrongException("testCreateConsumerWithNullMsgSelector_Topic_B_SecOff failed: Expected message was not received");

    }

    public void testCreateConsumerWithNullMsgSelector_Topic_TcpIp_SecOff(HttpServletRequest request,
                                                                         HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContext = jmsTCFTCP.createContext();
        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsTopic, null);
        JMSProducer jmsProducer = jmsContext.createProducer();
        TextMessage msg = jmsContext.createTextMessage("testCreateConsumerWithNullMsgSelector_Topic_TcpIp_SecOff");
        msg.setStringProperty("MessageType", "text");
        jmsProducer.send(jmsTopic, msg);

        TextMessage rec_msg = (TextMessage) jmsConsumer.receive();
        jmsConsumer.close();
        jmsContext.close();

        if (!(rec_msg != null && rec_msg.getText().equals("testCreateConsumerWithNullMsgSelector_Topic_TcpIp_SecOff")))
            exceptionFlag = true;

        if (exceptionFlag)
            throw new WrongException("testCreateConsumerWithNullMsgSelector_Topic_TcpIp_SecOff failed: Expected message was not received");

    }

    public void testCreateConsumerWithNullMsgSelectorNoLocal_B_SecOff(HttpServletRequest request,
                                                                      HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContext = jmsQCFBindings.createContext();
        emptyQueue(jmsQCFBindings, jmsQueue1);
        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue1, null, false);
        JMSProducer jmsProducer = jmsContext.createProducer();
        TextMessage msg = jmsContext.createTextMessage("testCreateConsumerWithNullMsgSelectorNoLocal_B_SecOff");
        msg.setStringProperty("MessageType", "text");
        jmsProducer.send(jmsQueue1, msg);

        TextMessage rec_msg = (TextMessage) jmsConsumer.receive(DEFAULT_TIMEOUT);
        jmsContext.close();

        if (!(rec_msg != null && rec_msg.getText().equals("testCreateConsumerWithNullMsgSelectorNoLocal_B_SecOff")))
            exceptionFlag = true;

        if (exceptionFlag)
            throw new WrongException("testCreateConsumerWithNullMsgSelectorNoLocal_B_SecOff failed: Expected message was not received");

    }

    public void testCreateConsumerWithNullMsgSelectorNoLocal_TcpIp_SecOff(HttpServletRequest request,
                                                                          HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContext = jmsQCFTCP.createContext();
        emptyQueue(jmsQCFTCP, jmsQueue1);
        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue1, null, false);
        JMSProducer jmsProducer = jmsContext.createProducer();
        TextMessage msg = jmsContext.createTextMessage("testCreateConsumerWithNullMsgSelectorNoLocal_TcpIp_SecOff");
        msg.setStringProperty("MessageType", "text");
        jmsProducer.send(jmsQueue1, msg);

        TextMessage rec_msg = (TextMessage) jmsConsumer.receive(DEFAULT_TIMEOUT);
        jmsContext.close();

        if (!(rec_msg != null && rec_msg.getText().equals("testCreateConsumerWithNullMsgSelectorNoLocal_TcpIp_SecOff")))
            exceptionFlag = true;

        if (exceptionFlag)
            throw new WrongException("testCreateConsumerWithNullMsgSelectorNoLocal_TcpIp_SecOff failed: Expected message was not received");

    }

    public void testCreateConsumerWithNullMsgSelectorNoLocal_Topic_B_SecOff(HttpServletRequest request,
                                                                            HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContext = jmsTCFBindings.createContext();
        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsTopic, null, false);
        JMSProducer jmsProducer = jmsContext.createProducer();
        TextMessage msg = jmsContext.createTextMessage("testCreateConsumerWithNullMsgSelectorNoLocal_Topic_B_SecOff");
        msg.setStringProperty("MessageType", "text");
        jmsProducer.send(jmsTopic, msg);

        TextMessage m = (TextMessage) jmsConsumer.receive(DEFAULT_TIMEOUT);
        jmsConsumer.close();
        jmsContext.close();

        if (!(m != null && m.getText().equals("testCreateConsumerWithNullMsgSelectorNoLocal_Topic_B_SecOff")))
            exceptionFlag = true;

        if (exceptionFlag)
            throw new WrongException("testCreateConsumerWithNullMsgSelectorNoLocal_Topic_B_SecOff failed: Expected message was not received");

    }

    public void testCreateConsumerWithNullMsgSelectorNoLocal_Topic_TcpIp_SecOff(HttpServletRequest request,
                                                                                HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContext = jmsTCFTCP.createContext();
        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsTopic, null, false);
        JMSProducer jmsProducer = jmsContext.createProducer();
        TextMessage msg = jmsContext.createTextMessage("testCreateConsumerWithNullMsgSelectorNoLocal_Topic_TcpIp_SecOff");
        msg.setStringProperty("MessageType", "text");
        jmsProducer.send(jmsTopic, msg);

        TextMessage rec_msg = (TextMessage) jmsConsumer.receive(DEFAULT_TIMEOUT);
        jmsConsumer.close();
        jmsContext.close();

        if (!(rec_msg != null && rec_msg.getText().equals("testCreateConsumerWithNullMsgSelectorNoLocal_Topic_TcpIp_SecOff")))
            exceptionFlag = true;

        if (exceptionFlag)
            throw new WrongException("testCreateConsumerWithNullMsgSelectorNoLocal_Topic_TcpIp_SecOff failed: Expected message was not received");

    }

    public void testCreateConsumerWithEmptyMsgSelector_B_SecOff(HttpServletRequest request,
                                                                HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContext = jmsQCFBindings.createContext();
        emptyQueue(jmsQCFBindings, jmsQueue1);
        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue1, "");
        JMSProducer jmsProducer = jmsContext.createProducer();
        TextMessage msg = jmsContext.createTextMessage("testCreateConsumerWithEmptyMsgSelector_B_SecOff");
        msg.setStringProperty("MessageType", "text");
        jmsProducer.send(jmsQueue1, msg);

        TextMessage rec_msg = (TextMessage) jmsConsumer.receive(DEFAULT_TIMEOUT);
        jmsContext.close();

        if (!(rec_msg != null && rec_msg.getText().equals("testCreateConsumerWithEmptyMsgSelector_B_SecOff")))
            exceptionFlag = true;

        if (exceptionFlag)
            throw new WrongException("testCreateConsumerWithEmptyMsgSelector_B_SecOff failed: Expected message was not received");

    }

    public void testCreateConsumerWithEmptyMsgSelector_TcpIp_SecOff(HttpServletRequest request,
                                                                    HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContext = jmsQCFTCP.createContext();
        emptyQueue(jmsQCFTCP, jmsQueue1);
        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue1, "");
        JMSProducer jmsProducer = jmsContext.createProducer();
        TextMessage msg = jmsContext.createTextMessage("testCreateConsumerWithEmptyMsgSelector_TcpIp_SecOff");
        msg.setStringProperty("MessageType", "text");
        jmsProducer.send(jmsQueue1, msg);

        TextMessage rec_msg = (TextMessage) jmsConsumer.receive(DEFAULT_TIMEOUT);
        jmsContext.close();

        if (!(rec_msg != null && rec_msg.getText().equals("testCreateConsumerWithEmptyMsgSelector_TcpIp_SecOff")))
            exceptionFlag = true;

        if (exceptionFlag)
            throw new WrongException("testCreateConsumerWithEmptyMsgSelector_TcpIp_SecOff failed: Expected message was not received");
    }

    public void testCreateConsumerWithEmptyMsgSelector_Topic_B_SecOff(HttpServletRequest request,
                                                                      HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContext = jmsTCFBindings.createContext();
        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsTopic, "");
        JMSProducer jmsProducer = jmsContext.createProducer();
        TextMessage msg = jmsContext.createTextMessage("testCreateConsumerWithEmptyMsgSelector_Topic_B_SecOff");
        msg.setStringProperty("MessageType", "text");
        jmsProducer.send(jmsTopic, msg);

        TextMessage rec_msg = (TextMessage) jmsConsumer.receive(DEFAULT_TIMEOUT);
        jmsConsumer.close();
        jmsContext.close();

        if (!(rec_msg != null && rec_msg.getText().equals("testCreateConsumerWithEmptyMsgSelector_Topic_B_SecOff")))
            exceptionFlag = true;

        if (exceptionFlag)
            throw new WrongException("testCreateConsumerWithEmptyMsgSelector_Topic_B_SecOff failed: Expected message was not received");

    }

    public void testCreateConsumerWithEmptyMsgSelector_Topic_TcpIp_SecOff(HttpServletRequest request,
                                                                          HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContext = jmsTCFTCP.createContext();
        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsTopic, "");
        JMSProducer jmsProducer = jmsContext.createProducer();
        TextMessage msg = jmsContext.createTextMessage("testCreateConsumerWithEmptyMsgSelector_Topic_TcpIp_SecOff");
        msg.setStringProperty("MessageType", "text");
        jmsProducer.send(jmsTopic, msg);

        TextMessage rec_msg = (TextMessage) jmsConsumer.receive(DEFAULT_TIMEOUT);
        jmsConsumer.close();
        jmsContext.close();

        if (!(rec_msg != null && rec_msg.getText().equals("testCreateConsumerWithEmptyMsgSelector_Topic_TcpIp_SecOff")))
            exceptionFlag = true;

        if (exceptionFlag)
            throw new WrongException("testCreateConsumerWithEmptyMsgSelector_Topic_TcpIp_SecOff failed: Expected message was not received");

    }

    public void testCreateConsumerWithEmptyMsgSelectorNoLocal_B_SecOff(HttpServletRequest request,
                                                                       HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContext = jmsQCFBindings.createContext();
        emptyQueue(jmsQCFBindings, jmsQueue1);
        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue1, "", false);
        JMSProducer jmsProducer = jmsContext.createProducer();
        TextMessage msg = jmsContext.createTextMessage("testCreateConsumerWithEmptyMsgSelectorNoLocal_B_SecOff");
        msg.setStringProperty("MessageType", "text");
        jmsProducer.send(jmsQueue1, msg);

        TextMessage rec_msg = (TextMessage) jmsConsumer.receive(DEFAULT_TIMEOUT);
        jmsContext.close();

        if (!(rec_msg != null && rec_msg.getText().equals("testCreateConsumerWithEmptyMsgSelectorNoLocal_B_SecOff")))
            exceptionFlag = true;

        if (exceptionFlag)
            throw new WrongException("testCreateConsumerWithEmptyMsgSelectorNoLocal_B_SecOff failed: Expected message was not received");

    }

    public void testCreateConsumerWithEmptyMsgSelectorNoLocal_TcpIp_SecOff(HttpServletRequest request,
                                                                           HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContext = jmsQCFTCP.createContext();
        emptyQueue(jmsQCFTCP, jmsQueue1);
        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue1, "", false);
        JMSProducer jmsProducer = jmsContext.createProducer();
        TextMessage msg = jmsContext.createTextMessage("testCreateConsumerWithEmptyMsgSelectorNoLocal_TcpIp_SecOff");
        msg.setStringProperty("MessageType", "text");
        jmsProducer.send(jmsQueue1, msg);

        TextMessage rec_msg = (TextMessage) jmsConsumer.receive(DEFAULT_TIMEOUT);
        jmsContext.close();

        if (!(rec_msg != null && rec_msg.getText().equals("testCreateConsumerWithEmptyMsgSelectorNoLocal_TcpIp_SecOff")))
            exceptionFlag = true;

        if (exceptionFlag)
            throw new WrongException("testCreateConsumerWithEmptyMsgSelectorNoLocal_TcpIp_SecOff failed: Expected message was not received");

    }

    public void testCreateConsumerWithEmptyMsgSelectorNoLocal_Topic_B_SecOff(HttpServletRequest request,
                                                                             HttpServletResponse response) throws Throwable {

        exceptionFlag = false;

        Topic topic = (Topic) new InitialContext().lookup("java:comp/env/eis/topic12");

        JMSContext jmsContext = jmsTCFBindings.createContext();

        JMSConsumer jmsConsumer = jmsContext.createConsumer(topic, "", false);
        JMSProducer jmsProducer = jmsContext.createProducer();

        TextMessage msg = jmsContext.createTextMessage("testCreateConsumerWithEmptyMsgSelectorNoLocal_Topic_B_SecOff");
        msg.setStringProperty("MessageType", "text");
        jmsProducer.send(topic, msg);

        TextMessage rec_msg = (TextMessage) jmsConsumer.receive(5000);
        jmsConsumer.close();
        jmsContext.close();

        if (!(rec_msg != null && rec_msg.getText().equals("testCreateConsumerWithEmptyMsgSelectorNoLocal_Topic_B_SecOff")))
            exceptionFlag = true;

        if (exceptionFlag) {
            System.out.println("Received Message :" + rec_msg.toString());
            throw new WrongException("testCreateConsumerWithEmptyMsgSelectorNoLocal_Topic_B_SecOff failed: Expected message was not received");

        }
    }

    public void testCreateConsumerWithEmptyMsgSelectorNoLocal_Topic_TcpIp_SecOff(HttpServletRequest request,
                                                                                 HttpServletResponse response) throws Throwable {
        exceptionFlag = false;
        JMSContext jmsContext = jmsTCFTCP.createContext();
        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsTopic, "", false);
        JMSProducer jmsProducer = jmsContext.createProducer();
        TextMessage msg = jmsContext.createTextMessage("testCreateConsumerWithEmptyMsgSelectorNoLocal_Topic_TcpIp_SecOff");
        msg.setStringProperty("MessageType", "text");
        jmsProducer.send(jmsTopic, msg);

        TextMessage rec_msg = (TextMessage) jmsConsumer.receive(DEFAULT_TIMEOUT);
        jmsConsumer.close();
        jmsContext.close();

        if (!(rec_msg != null && rec_msg.getText().equals("testCreateConsumerWithEmptyMsgSelectorNoLocal_Topic_TcpIp_SecOff")))
            exceptionFlag = true;

        if (exceptionFlag)
            throw new WrongException("testCreateConsumerWithEmptyMsgSelectorNoLocal_Topic_TcpIp_SecOff failed: Expected message was not received");
    }

    public void testCreateConsumerWithMsgSelectorNoLocalInvalidDest_B_SecOff(HttpServletRequest request,
                                                                             HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContext = jmsQCFBindings.createContext();
        Queue queue = (Queue) new InitialContext()
                        .lookup("java:comp/env/eis/queue11");
        try {
            jmsContext.createConsumer(queue, "Destination = 'Queue'", false);
        } catch (InvalidDestinationRuntimeException e) {
            System.out.println("Expected InvalidDestinationRuntimeException found in testCreateConsumerWithMsgSelectorNoLocalInvalidDest_B_SecOff");
            exceptionFlag = true;
            e.printStackTrace();
        }
        jmsContext.close();

        if (!exceptionFlag)
            throw new WrongException("testCreateConsumerWithMsgSelectorNoLocalInvalidDest_B_SecOff failed: Expected exception was not seen");
    }

    public void testCreateConsumerWithMsgSelectorNoLocalInvalidDest_TcpIp_SecOff(HttpServletRequest request,
                                                                                 HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContext = jmsQCFBindings.createContext();
        Queue queue = (Queue) new InitialContext()
                        .lookup("java:comp/env/eis/queue11");
        try {
            jmsContext.createConsumer(queue, "Destination = 'Queue'", false);
        } catch (InvalidDestinationRuntimeException e) {
            System.out.println("Expected InvalidDestinationRuntimeException found in testCreateConsumerWithMsgSelectorNoLocalInvalidDest_TcpIp_SecOff");
            exceptionFlag = true;
            e.printStackTrace();
        }
        jmsContext.close();

        if (!exceptionFlag)
            throw new WrongException("testCreateConsumerWithMsgSelectorNoLocalInvalidDest_TcpIp_SecOff failed: Expected exception was not seen");

    }

    public void testCreateConsumerWithMsgSelectorNoLocalInvalidDest_Topic_B_SecOff(HttpServletRequest request,
                                                                                   HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContext = jmsTCFBindings.createContext();
        Topic topic = (Topic) new InitialContext()
                        .lookup("java:comp/env/eis/topic11");
        try {
            jmsContext.createConsumer(topic, "Destination = 'Topic'", false);
        } catch (InvalidDestinationRuntimeException e) {
            System.out.println("Expected InvalidDestinationRuntimeException found in testCreateConsumerWithMsgSelectorNoLocalInvalidDest_Topic_B_SecOff");
            exceptionFlag = true;
            e.printStackTrace();
        }
        jmsContext.close();

        if (!exceptionFlag)
            throw new WrongException("testCreateConsumerWithMsgSelectorNoLocalInvalidDest_Topic_B_SecOff failed: Expected exception was not seen");

    }

    public void testCreateConsumerWithMsgSelectorNoLocalInvalidDest_Topic_TcpIp_SecOff(HttpServletRequest request,
                                                                                       HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContext = jmsTCFBindings.createContext();
        Topic topic = (Topic) new InitialContext()
                        .lookup("java:comp/env/eis/topic11");
        try {
            jmsContext.createConsumer(topic, "Destination = 'Topic'", false);
        } catch (InvalidDestinationRuntimeException e) {
            System.out.println("Expected InvalidDestinationRuntimeException found in testCreateConsumerWithMsgSelectorNoLocalInvalidDest_Topic_TcpIp_SecOff");
            exceptionFlag = true;
            e.printStackTrace();
        }
        jmsContext.close();

        if (!exceptionFlag)
            throw new WrongException("testCreateConsumerWithMsgSelectorNoLocalInvalidDest_Topic_TcpIp_SecOff failed: Expected exception was not seen");

    }

    public void testCloseConsumerDepth_B_SecOff(HttpServletRequest request,
                                                HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsTCFBindings.createContext();
        JMSConsumer tr = jmsContext.createConsumer(jmsTopic);
        JMSProducer producer = jmsContext.createProducer();
        TextMessage msg = jmsContext.createTextMessage("testCloseConsumerDepth_B_SecOff");
        msg.setStringProperty("MessageType", "text");
        producer.send(jmsTopic, msg);

        producer.send(jmsTopic, msg);

        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();

        final ObjectName name = new ObjectName("WebSphere:feature=wasJmsServer,type=Topic,name=NewTopic1");
        String obn = (String) mbs.getAttribute(name, "Id");
        long obn4 = (Long) mbs.getAttribute(name, "Depth");

        System.out.println("Current depth of topic before closing consumer is " + obn4);
        tr.close();
        Thread.sleep(2000);
        producer.send(jmsTopic, msg);

        MBeanServer mbs1 = ManagementFactory.getPlatformMBeanServer();

        final ObjectName name1 = new ObjectName("WebSphere:feature=wasJmsServer,type=Topic,name=NewTopic1");
        System.out.println("initialized MBeanServer and Object");
        String obn1 = (String) mbs1.getAttribute(name1, "Id");
        long obn2 = (Long) mbs1.getAttribute(name1, "Depth");

        System.out.println("Consumer is closed and one more msg is sent. Now the topic depth is " + obn2);

        jmsContext.close();

        if (obn2 != 0)
            throw new WrongException("testListSubscriber_TcpIp failed: Number of subscriptions is not 0");
    }

    public void testCloseConsumerDepth_TcpIp_SecOff(HttpServletRequest request,
                                                    HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsTCFTCP.createContext();
        JMSConsumer tr = jmsContext.createConsumer(jmsTopic);
        JMSProducer producer = jmsContext.createProducer();
        TextMessage msg = jmsContext.createTextMessage("testCloseConsumerDepth_TcpIp_SecOff");
        msg.setStringProperty("MessageType", "text");
        producer.send(jmsTopic, msg);
        producer.send(jmsTopic, msg);

        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();

        final ObjectName name = new ObjectName("WebSphere:feature=wasJmsServer,type=Topic,name=NewTopic1");
        System.out.println("initialized MBeanServer and Object");
        String obn = (String) mbs.getAttribute(name, "Id");
        long obn4 = (Long) mbs.getAttribute(name, "Depth");

        System.out.println("Current depth of topic before closing consumer is " + obn4);
        tr.close();
        Thread.sleep(2000);
        producer.send(jmsTopic, msg);

        MBeanServer mbs1 = ManagementFactory.getPlatformMBeanServer();

        final ObjectName name1 = new ObjectName("WebSphere:feature=wasJmsServer,type=Topic,name=NewTopic1");
        System.out.println("initialized MBeanServer and Object");
        String obn1 = (String) mbs1.getAttribute(name1, "Id");
        long obn2 = (Long) mbs1.getAttribute(name1, "Depth");

        System.out.println("Consumer is closed and one more msg is sent. Now the topic depth is " + obn2);

        jmsContext.close();

        if (obn2 != 0)
            throw new WrongException("testListSubscriber_TcpIp failed: Number of subscriptions is not 0");
    }

    public void testListSubscriber_B(HttpServletRequest request,
                                     HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsTCFBindings.createContext();
        JMSProducer producer = jmsContext.createProducer();
        JMSConsumer tr = jmsContext.createConsumer(jmsTopic);

        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();

        final ObjectName name = new ObjectName("WebSphere:feature=wasJmsServer,type=Topic,name=NewTopic1");

        javax.management.openmbean.CompositeData[] obn = (CompositeData[]) mbs
                        .invoke(name, "listSubscriptions", null, null);

        System.out.println("Number of subs is " + obn.length);

        tr.close();

        MBeanServer mbs1 = ManagementFactory.getPlatformMBeanServer();

        final ObjectName name1 = new ObjectName("WebSphere:feature=wasJmsServer,type=Topic,name=NewTopic1");

        javax.management.openmbean.CompositeData[] obn1 = (CompositeData[]) mbs1
                        .invoke(name1, "listSubscriptions", null, null);

        System.out.println("Number of subs after closing consumer is " + obn1.length);
        jmsContext.close();

        if (obn1.length != 0)
            throw new WrongException("testListSubscriber_TcpIp failed: Number of subscriptions is not 0");
    }

    public void testListSubscriber_TcpIp(HttpServletRequest request,
                                         HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsTCFTCP.createContext();
        JMSProducer producer = jmsContext.createProducer();
        JMSConsumer tr = jmsContext.createConsumer(jmsTopic);

        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();

        final ObjectName name = new ObjectName("WebSphere:feature=wasJmsServer,type=Topic,name=NewTopic1");

        javax.management.openmbean.CompositeData[] obn = (CompositeData[]) mbs
                        .invoke(name, "listSubscriptions", null, null);

        System.out.println("Number of subs is " + obn.length);

        tr.close();

        MBeanServer mbs1 = ManagementFactory.getPlatformMBeanServer();

        final ObjectName name1 = new ObjectName("WebSphere:feature=wasJmsServer,type=Topic,name=NewTopic1");

        javax.management.openmbean.CompositeData[] obn1 = (CompositeData[]) mbs1
                        .invoke(name1, "listSubscriptions", null, null);

        System.out.println("Number of subs after closing consumer is " + obn1.length);
        jmsContext.close();

        if (obn1.length != 0)
            throw new WrongException("testListSubscriber_TcpIp failed: Number of subscriptions is not 0");
    }

    public void testNoLocalTrue_B(HttpServletRequest request,
                                  HttpServletResponse response) throws Exception {

        exceptionFlag = false;
        JMSContext jmsContext = jmsTCFBindings.createContext();
        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsTopic, "", true);
        JMSProducer jmsProducer = jmsContext.createProducer();

        TextMessage msg = jmsContext.createTextMessage("testNoLocalTrue_B");
        msg.setStringProperty("MessageType", "text");
        jmsProducer.send(jmsTopic, msg);
        System.out.println("Sent 1st msg");

        TextMessage recmsg = (TextMessage) jmsConsumer.receive(DEFAULT_TIMEOUT);
        if (recmsg == null) {
            System.out.println("msg was not received as expected");
        }

        else {
            System.out.println("message is " + recmsg.getText());
            exceptionFlag = true;
        }

        jmsConsumer.close();
        jmsContext.close();
        if (exceptionFlag)
            throw new WrongException("testNoLocalTrue_B failed: Received the message");

    }

    public void testNoLocalTrue_TcpIp(HttpServletRequest request,
                                      HttpServletResponse response) throws Exception {

        exceptionFlag = false;
        TopicConnectionFactory cf1 = (TopicConnectionFactory) new InitialContext()
                        .lookup("java:comp/env/eis/tcf1");

        Topic topic = (Topic) new InitialContext()
                        .lookup("java:comp/env/eis/topic1");

        JMSContext context = cf1.createContext();
        JMSProducer producer = context.createProducer();
        JMSConsumer tr = context.createConsumer(topic, "", true);

        TextMessage msg = context.createTextMessage("testNoLocalTrue_TcpIp");
        msg.setStringProperty("MessageType", "text");
        producer.send(topic, msg);
        System.out.println("Sent 1st msg");

        TextMessage recmsg = (TextMessage) tr.receive(DEFAULT_TIMEOUT);
        if (recmsg == null) {
            System.out.println("msg was not received as expected");
        }

        else {
            System.out.println("message is " + recmsg.getText());
            exceptionFlag = true;
        }
        tr.close();
        context.close();

        if (exceptionFlag)
            throw new WrongException("testNoLocalTrue_TcpIp failed");

    }

    public void testNoLocalTrueQueue_B(HttpServletRequest request,
                                       HttpServletResponse response) throws Exception {

        exceptionFlag = false;
        JMSContext jmsContext = jmsQCFBindings.createContext();
        emptyQueue(jmsQCFBindings, jmsQueue);
        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue, "", true);
        JMSProducer jmsProducer = jmsContext.createProducer();

        TextMessage msg = jmsContext.createTextMessage("testNoLocalTrueQueue_B");
        msg.setStringProperty("MessageType", "text");
        jmsProducer.send(jmsQueue, msg);

        TextMessage recmsg = (TextMessage) jmsConsumer.receive(DEFAULT_TIMEOUT);
        if (!(recmsg == null)) {
            System.out.println("msg was not received as expected");
        }

        else {
            System.out.println("message is " + recmsg.getText());
            exceptionFlag = true;
        }

        jmsContext.close();
        if (exceptionFlag)
            throw new WrongException("testNoLocalTrueQueue_B failed: message was not received");

    }

    public void testNoLocalTrueQueue_TcpIp(HttpServletRequest request,
                                           HttpServletResponse response) throws Exception {

        exceptionFlag = false;
        JMSContext jmsContext = jmsQCFTCP.createContext();
        emptyQueue(jmsQCFTCP, jmsQueue);
        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue, "", true);
        JMSProducer jmsProducer = jmsContext.createProducer();

        TextMessage msg = jmsContext.createTextMessage("testNoLocalTrueQueue_TcpIp");
        msg.setStringProperty("MessageType", "text");
        jmsProducer.send(jmsQueue, msg);

        TextMessage recmsg = (TextMessage) jmsConsumer.receive(DEFAULT_TIMEOUT);
        if (!(recmsg == null)) {
            System.out.println("msg was not received as expected");
        }

        else {
            System.out.println("message is " + recmsg.getText());
            exceptionFlag = true;
        }

        jmsContext.close();
        if (exceptionFlag)
            throw new WrongException("testNoLocalTrueQueue_TcpIp failed: message was not received");
    }

    public static QueueConnectionFactory getQCFBindings() throws NamingException {

        QueueConnectionFactory cf1 = (QueueConnectionFactory) new InitialContext()
                        .lookup("java:comp/env/jndi_JMS_BASE_QCF");

        return cf1;

    }

    public QueueConnectionFactory getQCFTCP() throws NamingException {

        QueueConnectionFactory cf1 = (QueueConnectionFactory) new InitialContext()
                        .lookup("java:comp/env/jndi_JMS_BASE_QCF1");

        return cf1;

    }

    public Queue getQueue(String name) throws NamingException {

        Queue queue = (Queue) new InitialContext()
                        .lookup("java:comp/env/" + name);

        return queue;
    }

    public static TopicConnectionFactory getTCFBindings() throws NamingException {

        TopicConnectionFactory cf1 = (TopicConnectionFactory) new InitialContext()
                        .lookup("java:comp/env/eis/tcf");

        return cf1;

    }

    public TopicConnectionFactory getTCFTCP() throws NamingException {

        TopicConnectionFactory cf1 = (TopicConnectionFactory) new InitialContext()
                        .lookup("java:comp/env/eis/tcf1");

        return cf1;

    }

    public Topic getTopic() throws NamingException {

        Topic topic = (Topic) new InitialContext()
                        .lookup("java:comp/env/eis/topic2");

        return topic;
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
