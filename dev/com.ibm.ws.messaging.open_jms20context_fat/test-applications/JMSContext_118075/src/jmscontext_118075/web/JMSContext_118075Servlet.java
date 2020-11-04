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

    public static Queue jmsQueue;
    public static Queue jmsQueue1;

    public static TopicConnectionFactory jmsTCFBindings;
    public static TopicConnectionFactory jmsTCFTCP;

    public static Topic jmsTopic;

    private static int DEFAULT_TIMEOUT = 30000;

    public QueueConnectionFactory getQCF(String name) {
        QueueConnectionFactory qcf;
        try {
            qcf = (QueueConnectionFactory) new InitialContext().lookup(name);
        } catch ( NamingException e ) {
            e.printStackTrace();
            qcf = null;
        }
        System.out.println("Queue connection factory '" + name + "' [ " + qcf + " ]");
        return qcf;
    }

    public Queue getQueue(String name) {
        Queue queue;
        try {
            queue = (Queue) new InitialContext().lookup(name);
        } catch ( NamingException e ) {
            e.printStackTrace();
            queue = null;
        }
        System.out.println("Queue '" + name + "' [ " + queue + " ]");
        return queue;
    }

    public static TopicConnectionFactory getTCF(String name) {
        TopicConnectionFactory tcf;
        try {
            tcf = (TopicConnectionFactory) new InitialContext().lookup(name);
        } catch ( NamingException e ) {
            e.printStackTrace();
            tcf = null;
        }
        System.out.println("Topic connection factory '" + name + "' [ " + tcf + " ]");
        return tcf;
    }

    public Topic getTopic(String name) {
        Topic topic;
        try {
            topic = (Topic) new InitialContext().lookup(name);
        } catch ( NamingException e ) {
            e.printStackTrace();
            topic = null;
        }
        System.out.println("Topic '" + name + "' [ " + topic + " ]");
        return topic;
    }

    public void emptyQueue(QueueConnectionFactory qcf, Queue q) throws Exception {
        JMSContext jmsContext = qcf.createContext();

        try {
            QueueBrowser qb = jmsContext.createBrowser(q);
            Enumeration e = qb.getEnumeration();

            JMSConsumer jmsConsumer = jmsContext.createConsumer(q);

            try {
                int numMsgs = 0;
                while ( e.hasMoreElements() ) {
                    e.nextElement();
                    numMsgs++;
                }

                for ( int msgNo = 0; msgNo < numMsgs; msgNo++ ) {
                    jmsConsumer.receive();
                }

            } finally {
                jmsConsumer.close();
            }

        } finally {
            jmsContext.close();
        }
    }

    @Override
    public void init() throws ServletException {
        super.init();

        jmsQCFBindings = getQCF("java:comp/env/jndi_JMS_BASE_QCF");
        jmsQCFTCP = getQCF("java:comp/env/jndi_JMS_BASE_QCF1");
        jmsQueue = getQueue("java:comp/env/jndi_INPUT_Q");
        jmsQueue1 = getQueue("java:comp/env/eis/queue2");

        jmsTCFBindings = getTCF("java:comp/env/eis/tcf");
        jmsTCFTCP = getTCF("java:comp/env/eis/tcf1");
        jmsTopic = getTopic("java:comp/env/eis/topic2");

        jmsQCFBindings = getQCF("java:comp/env/jndi_JMS_BASE_QCF");

        if ( jmsQCFBindings == null ) {
            throw new ServletException("Null 'jmsQCFBindings'");
        }
        if ( jmsQCFTCP == null ) {
            throw new ServletException("Null 'jmsQCFTCP'");
        }
        if ( jmsQueue == null ) {
            throw new ServletException("Null 'jmsQueue'");
        }
        if ( jmsQueue1 == null ) {
            throw new ServletException("Null 'jmsQueue1'");
        }

        if ( jmsTCFBindings == null ) {
            throw new ServletException("Null 'jmsTCFBindings'");
        }
        if ( jmsTCFTCP == null ) {
            throw new ServletException("Null 'jmsTCFTCP'");
        }
        if ( jmsTopic == null ) {
            throw new ServletException("Null 'jmsTopic'");
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
        TraceComponent tc = Tr.register(JMSContext_118075Servlet.class);

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

    public void testQueueConsumer_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsQCFBindings.createContext();
        emptyQueue(jmsQCFBindings, jmsQueue1);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue1);

        jmsContext.createProducer().send(jmsQueue1, "test message");

        TextMessage msg = (TextMessage) jmsConsumer.receive(DEFAULT_TIMEOUT);

        boolean testFailed = false;
        if ( (msg == null) ||
             (msg.getText() == null) ||
             !msg.getText().equals("test message") ) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testQueueConsumer_B_SecOff failed: Expected message was not received");
        }
    }

    public void testQueueConsumer_TcpIp_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsQCFTCP.createContext();
        emptyQueue(jmsQCFTCP, jmsQueue);

        JMSProducer jmsProducer = jmsContext.createProducer().send(jmsQueue, "testQueueConsumer_TcpIp_SecOff");

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);
        TextMessage msg = (TextMessage) jmsConsumer.receive(DEFAULT_TIMEOUT);

        boolean testFailed = false;
        if ( (msg == null) ||
             (msg.getText() == null) ||
             !msg.getText().equals("testQueueConsumer_TcpIp_SecOff") ) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testQueueConsumer_TcpIp_SecOff failed: Expected message was not received");
        }
    }

    public void testTopicConsumer_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsTCFBindings.createContext();

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsTopic);

        jmsContext.createProducer().send(jmsTopic, "testTopicConsumer_B_SecOff");

        TextMessage msg = (TextMessage) jmsConsumer.receive(DEFAULT_TIMEOUT);

        boolean testFailed = false;
        if ( (msg == null) ||
             (msg.getText() == null) ||
             !msg.getText().equals("testTopicConsumer_B_SecOff") ) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testTopicConsumer_B_SecOff failed: Expected message was not received");
        }
    }

    public void testTopicConsumer_TcpIp_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsTCFTCP.createContext();

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsTopic);

        jmsContext.createProducer().send(jmsTopic, "testTopicConsumer_TcpIp_SecOff");

        TextMessage msg = (TextMessage) jmsConsumer.receive(DEFAULT_TIMEOUT);

        boolean testFailed = false;
        if ( (msg == null) ||
             (msg.getText() == null) ||
             !msg.getText().equals("testTopicConsumer_TcpIp_SecOff") ) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testTopicConsumer_TcpIp_SecOff failed: Expected message was not received");
        }
    }

    public void testCreateConsumerInvalidDest_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsQCFBindings.createContext();

        Queue localJMSQueue = (Queue)
            new InitialContext().lookup("java:comp/env/eis/queue11");

        boolean testFailed = false;
        try {
            jmsContext.createConsumer(localJMSQueue);
        } catch ( InvalidDestinationRuntimeException e ) {
            testFailed = true;
            e.printStackTrace();
        }

        jmsContext.close();

        if ( !testFailed ) {
            throw new Exception("testCreateConsumerInvalidDest_B_SecOff failed: Expected exception was not seen");
        }
    }

    public void testCreateConsumerInvalidDest_TcpIp_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsQCFTCP.createContext();

        Queue localJMSQueue = (Queue)
            new InitialContext().lookup("java:comp/env/eis/queue11");

        boolean testFailed = false;
        try {
            jmsContext.createConsumer(localJMSQueue);
        } catch ( InvalidDestinationRuntimeException e ) {
            testFailed = true;
            e.printStackTrace();
        }

        jmsContext.close();

        if ( !testFailed ) {
            throw new Exception("testCreateConsumerInvalidDest_TcpIp_SecOff failed: Expected exception was not seen");
        }
    }

    public void testCreateConsumerInvalidDest_Topic_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsTCFBindings.createContext();

        Topic localJMSTopic = (Topic)
            new InitialContext().lookup("java:comp/env/eis/topic11");

        boolean testFailed = false;
        try {
            jmsContext.createConsumer(localJMSTopic);
        } catch ( InvalidDestinationRuntimeException e ) {
            testFailed = true;
            e.printStackTrace();
        }

        jmsContext.close();

        if ( !testFailed ) {
            throw new Exception("testCreateConsumerInvalidDest_Topic_B_SecOff failed: Expected exception was not seen");
        }
    }

    public void testCreateConsumerInvalidDest_Topic_TcpIp_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsTCFTCP.createContext();

        Topic localJMSTopic = (Topic)
            new InitialContext().lookup("java:comp/env/eis/topic11");

        boolean testFailed = false;
        try {
            jmsContext.createConsumer(localJMSTopic);
        } catch ( InvalidDestinationRuntimeException e ) {
            testFailed = true;
            e.printStackTrace();
        }

        jmsContext.close();

        if ( !testFailed ) {
            throw new Exception("testCreateConsumerInvalidDest_Topic_TcpIp_SecOff failed: Expected exception was not seen");
        }
    }

    public void testCreateConsumerWithMsgSelector_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsQCFBindings.createContext();
        emptyQueue(jmsQCFBindings, jmsQueue);

        JMSProducer jmsProducer = jmsContext.createProducer();
        TextMessage msg = jmsContext.createTextMessage("testCreateConsumerWithMsgSelector_B_SecOff");
        msg.setStringProperty("Team", "SIB");
        jmsProducer.send(jmsQueue, msg);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue, "Team = 'SIB'");
        TextMessage recmsg = (TextMessage) jmsConsumer.receive(DEFAULT_TIMEOUT);

        boolean testFailed = false;
        if ( (recmsg == null) ||
             (recmsg.getText() == null) ||
             !recmsg.getText().equals("testCreateConsumerWithMsgSelector_B_SecOff") ||
             !recmsg.getStringProperty("Team").equals("SIB") ) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testCreateConsumerWithMsgSelector_B_SecOff failed: Expected message or property value was not received");
        }
    }

    public void testCreateConsumerWithMsgSelector_TcpIp_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsQCFTCP.createContext();
        emptyQueue(jmsQCFTCP, jmsQueue);

        JMSProducer jmsProducer = jmsContext.createProducer();
        TextMessage sendmsg = jmsContext.createTextMessage("testCreateConsumerWithMsgSelector_TcpIp_SecOff");
        sendmsg.setStringProperty("Team", "SIB");
        jmsProducer.send(jmsQueue, sendmsg);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue, "Team = 'SIB'");
        TextMessage recmsg = (TextMessage) jmsConsumer.receive(DEFAULT_TIMEOUT);

        boolean testFailed = false;
        if ( (recmsg == null) ||
             (recmsg.getText() == null) ||
             !recmsg.getText().equals("testCreateConsumerWithMsgSelector_TcpIp_SecOff") ||
             !recmsg.getStringProperty("Team").equals("SIB") ) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testCreateConsumerWithMsgSelector_TcpIp_SecOff failed: Expected message or property value was not received");
        }
    }

    public void testCreateConsumerWithMsgSelectorTopic_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsTCFBindings.createContext();

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsTopic, "Team = 'SIB'");

        JMSProducer jmsProducer = jmsContext.createProducer();
        TextMessage sendmsg = jmsContext.createTextMessage("testCreateConsumerWithMsgSelectorTopic_B_SecOff");
        sendmsg.setStringProperty("Team", "SIB");
        jmsProducer.send(jmsTopic, sendmsg);

        TextMessage recmsg = (TextMessage) jmsConsumer.receive(DEFAULT_TIMEOUT);

        boolean testFailed = false;
        if ( (recmsg == null) ||
             (recmsg.getText() == null) ||
             !recmsg.getText().equals("testCreateConsumerWithMsgSelectorTopic_B_SecOff") ||
             !recmsg.getStringProperty("Team").equals("SIB") ) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testCreateConsumerWithMsgSelectorTopic_B_SecOff failed: Expected message or property value was not received");
        }
    }

    public void testCreateConsumerWithMsgSelectorTopic_TcpIp_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsTCFTCP.createContext();

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsTopic, "Team = 'SIB'");

        JMSProducer jmsProducer = jmsContext.createProducer();
        TextMessage sendmsg = jmsContext.createTextMessage("testCreateConsumerWithMsgSelectorTopic_TcpIp_SecOff");
        sendmsg.setStringProperty("Team", "SIB");
        jmsProducer.send(jmsTopic, sendmsg);

        TextMessage recmsg = (TextMessage) jmsConsumer.receive(DEFAULT_TIMEOUT);

        boolean testFailed = false;
        if ( (recmsg == null) ||
             (recmsg.getText() == null) ||
             !recmsg.getText().equals("testCreateConsumerWithMsgSelectorTopic_TcpIp_SecOff") ||
             !recmsg.getStringProperty("Team").equals("SIB") ) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testCreateConsumerWithMsgSelectorTopic_TcpIp_SecOff failed: Expected message or property value was not received");
        }
    }

    public void testCreateConsumerWithMsgSelectorInvalidDest_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsQCFBindings.createContext();

        Queue localJMSQueue = (Queue)
            new InitialContext().lookup("java:comp/env/eis/queue11");

        boolean testFailed = false;
        try {
            jmsContext.createConsumer(localJMSQueue, "Team = 'SIB'");
            testFailed = true;
        } catch ( InvalidDestinationRuntimeException e ) {
            // Expected
        }

        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testCreateConsumerWithMsgSelectorInvalidDest_B_SecOff failed: Expected exception was not seen");
        }
    }

    public void testCreateConsumerWithMsgSelectorInvalidDest_TcpIp_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsQCFTCP.createContext();

        Queue localJMSQueue = (Queue)
            new InitialContext().lookup("java:comp/env/eis/queue11");

        boolean testFailed = false;
        try {
            jmsContext.createConsumer(localJMSQueue, "Team = 'SIB'");
            testFailed = true;
        } catch ( InvalidDestinationRuntimeException e ) {
            // expected
        }

        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testCreateConsumerWithMsgSelectorInvalidDest_TcpIp_SecOff failed: Expected exception was not seen");
        }
    }

    public void testCreateConsumerWithMsgSelectorInvalidDest_Topic_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsTCFBindings.createContext();
        Topic localJMSTopic = (Topic)
            new InitialContext().lookup("java:comp/env/eis/topic11");

        boolean testFailed = false;
        try {
            jmsContext.createConsumer(localJMSTopic, "Team = 'SIB'");
            testFailed = true;
        } catch ( InvalidDestinationRuntimeException e ) {
            // expected
        }

        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testCreateConsumerWithMsgSelectorInvalidDest_Topic_B_SecOff failed: Expected exception was not seen");
        }
    }

    public void testCreateConsumerWithMsgSelectorInvalidDest_Topic_TcpIp_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsTCFTCP.createContext();

        Topic localJMSTopic = (Topic)
            new InitialContext().lookup("java:comp/env/eis/topic11");

        boolean testFailed = false;
        try {
            jmsContext.createConsumer(localJMSTopic, "Team = 'SIB'");
            testFailed = true;
        } catch ( InvalidDestinationRuntimeException e ) {
            // Expected
        }

        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testCreateConsumerWithMsgSelectorInvalidDest_Topic_TcpIp_SecOff failed: Expected exception was not seen");
        }
    }

    public void testCreateConsumerWithMsgSelectorNullDest_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsQCFBindings.createContext();

        boolean testFailed = false;
        try {
            jmsContext.createConsumer(null, "Team = 'SIB'");
            testFailed = true;
        } catch ( InvalidDestinationRuntimeException e ) {
            // Expected
        }

        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testCreateConsumerWithMsgSelectorNullDest_B_SecOff failed: Expected exception was not seen");
        }
    }

    public void testCreateConsumerWithMsgSelectorNullDest_TcpIp_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsQCFTCP.createContext();

        boolean testFailed = false;
        try {
            jmsContext.createConsumer(null, "Team = 'SIB'");
            testFailed = true;
        } catch ( InvalidDestinationRuntimeException e ) {
            // expected
        }

        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testCreateConsumerWithMsgSelectorNullDest_TcpIp_SecOff failed: Expected exception was not seen");
        }
    }

    public void testCreateConsumerWithMsgSelectorNullDest_Topic_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsTCFBindings.createContext();

        boolean testFailed = false;
        try {
            jmsContext.createConsumer(null, "Team = 'SIB'");
            testFailed = true;
        } catch ( InvalidDestinationRuntimeException e ) {
            // expected
        }

        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testCreateConsumerWithMsgSelectorNullDest_Topic_B_SecOff failed: Expected exception was not seen");
        }
    }

    public void testCreateConsumerWithMsgSelectorNullDest_Topic_TcpIp_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsTCFTCP.createContext();

        boolean testFailed = false;
        try {
            jmsContext.createConsumer(null, "Team = 'SIB'");
            testFailed = true;
        } catch ( InvalidDestinationRuntimeException e ) {
            // expected
        }

        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testCreateConsumerWithMsgSelectorNullDest_Topic_TcpIp_SecOff failed: Expected exception was not seen");
        }
    }

    public void testCreateConsumerWithNullMsgSelectorNullDest_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsQCFBindings.createContext();

        boolean testFailed = false;
        try {
            jmsContext.createConsumer(null, null);
            testFailed = true;
        } catch ( InvalidDestinationRuntimeException e ) {
            // expected
        }

        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testCreateConsumerWithNullMsgSelectorNullDest_B_SecOff failed: Expected exception was not seen");
        }
    }

    public void testCreateConsumerWithNullMsgSelectorNullDest_TcpIp_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsQCFTCP.createContext();

        boolean testFailed = false;
        try {
            jmsContext.createConsumer(null, null);
            testFailed = true;
        } catch ( InvalidDestinationRuntimeException e ) {
            // expected
        }

        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testCreateConsumerWithNullMsgSelectorNullDest_TcpIp_SecOff failed: Expected exception was not seen");
        }
    }

    public void testCreateConsumerWithNullMsgSelectorNullDest_Topic_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsTCFBindings.createContext();

        boolean testFailed = false;
        try {
            jmsContext.createConsumer(null, null);
            testFailed = true;
        } catch ( InvalidDestinationRuntimeException e ) {
            // expected
        }

        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testCreateConsumerWithNullMsgSelectorNullDest_Topic_B_SecOff failed: Expected exception was not seen");
        }
    }

    public void testCreateConsumerWithNullMsgSelectorNullDest_Topic_TcpIp_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsTCFTCP.createContext();

        boolean testFailed = false;
        try {
            jmsContext.createConsumer(null, null);
        } catch ( InvalidDestinationRuntimeException e ) {
            // expected
        }

        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testCreateConsumerWithNullMsgSelectorNullDest_Topic_TcpIp_SecOff failed: Expected exception was not seen");
        }
    }

    public void testCreateConsumerNullDest_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsQCFBindings.createContext();

        boolean testFailed = false;
        try {
            jmsContext.createConsumer(null);
            testFailed = true;
        } catch ( InvalidDestinationRuntimeException e ) {
            // expected
        }

        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testCreateConsumerNullDest_B_SecOff failed: Expected exception was not seen");
        }

    }

    public void testCreateConsumerNullDest_TcpIp_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsQCFTCP.createContext();

        boolean testFailed = false;
        try {
            jmsContext.createConsumer(null);
            testFailed = true;
        } catch ( InvalidDestinationRuntimeException e ) {
            // expected
        }

        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testCreateConsumerNullDest_TcpIp_SecOff failed: Expected exception was not seen");
        }

    }

    public void testCreateConsumerNullDest_Topic_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsTCFBindings.createContext();

        boolean testFailed = false;
        try {            
            jmsContext.createConsumer(null);
            testFailed = true;
        } catch ( InvalidDestinationRuntimeException e ) {
            // expected
        }

        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testCreateConsumerNullDest_Topic_B_SecOff failed: Expected exception was not seen");
        }

    }

    public void testCreateConsumerNullDest_Topic_TcpIp_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsTCFTCP.createContext();

        boolean testFailed = false;
        try {
            jmsContext.createConsumer(null);
            testFailed = true;
        } catch ( InvalidDestinationRuntimeException e ) {
            // expected
        }

        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testCreateConsumerNullDest_Topic_TcpIp_SecOff failed: Expected exception was not seen");
        }
    }

    public void testCreateConsumerWithInvalidMsgSelector_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsQCFBindings.createContext();

        boolean testFailed = false;
        try {
            jmsContext.createConsumer(jmsQueue, "bad selector");
            testFailed = true;
        } catch (InvalidSelectorRuntimeException e) {
            // expected
        }

        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testCreateConsumerWithInvalidMsgSelector_B_SecOff failed: Expected exception was not seen");
        }

    }

    public void testCreateConsumerWithInvalidMsgSelector_TcpIp_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsQCFTCP.createContext();

        boolean testFailed = false;
        try {
            jmsContext.createConsumer(jmsQueue, "bad selector");
            testFailed = true;
        } catch (InvalidSelectorRuntimeException e) {
            // expected
        }

        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testCreateConsumerWithInvalidMsgSelector_TcpIp_SecOff failed: Expected exception was not seen");
        }

    }

    public void testCreateConsumerWithInvalidMsgSelector_Topic_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsTCFBindings.createContext();

        boolean testFailed = false;
        try {
            jmsContext.createConsumer(jmsTopic, "bad selector");
            testFailed = true;
        } catch (InvalidSelectorRuntimeException e) {
            // expected
        }

        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testCreateConsumerWithInvalidMsgSelector_Topic_B_SecOff failed: Expected exception was not seen");
        }

    }

    public void testCreateConsumerWithInvalidMsgSelector_Topic_TcpIp_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsTCFTCP.createContext();

        boolean testFailed = false;
        try {
            jmsContext.createConsumer(jmsTopic, "bad selector");
            testFailed = true;
        } catch (InvalidSelectorRuntimeException e) {
            // expected
        }

        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testCreateConsumerWithInvalidMsgSelector_Topic_TcpIp_SecOff failed: Expected exception was not seen");
        }

    }

    public void testCreateConsumerWithMsgSelectorNoLocal_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsQCFBindings.createContext();
        emptyQueue(jmsQCFBindings, jmsQueue);

        JMSProducer jmsProducer = jmsContext.createProducer();
        TextMessage sendmsg = jmsContext.createTextMessage("testCreateConsumerWithMsgSelectorNoLocal_B_SecOff");
        sendmsg.setStringProperty("Team", "SIB");
        jmsProducer.send(jmsQueue, sendmsg);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue, "Team = 'SIB'", false);
        TextMessage recmsg = (TextMessage) jmsConsumer.receive(DEFAULT_TIMEOUT);

        boolean testFailed = false;
        if ( (recmsg == null) ||
             (recmsg.getText() == null) ||
             !recmsg.getText().equals("testCreateConsumerWithMsgSelectorNoLocal_B_SecOff") ||
             !recmsg.getStringProperty("Team").equals("SIB")) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testCreateConsumerWithMsgSelectorNoLocal_B_SecOff failed: Expected message or property value was not received");
        }
    }

    public void testCreateConsumerWithMsgSelectorNoLocal_TcpIp_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsQCFTCP.createContext();
        emptyQueue(jmsQCFTCP, jmsQueue);

        JMSProducer jmsProducer = jmsContext.createProducer();
        TextMessage sendmsg = jmsContext.createTextMessage("testCreateConsumerWithMsgSelectorNoLocal_TcpIp_SecOff");
        sendmsg.setStringProperty("Team", "SIB");
        jmsProducer.send(jmsQueue, sendmsg);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue, "Team = 'SIB'", false);
        TextMessage recmsg = (TextMessage) jmsConsumer.receive(DEFAULT_TIMEOUT);

        boolean testFailed = false;
        if ( (recmsg == null) ||
             (recmsg.getText() == null) ||
             !recmsg.getText().equals("testCreateConsumerWithMsgSelectorNoLocal_TcpIp_SecOff") ||
             !recmsg.getStringProperty("Team").equals("SIB")) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testCreateConsumerWithMsgSelectorNoLocal_TcpIp_SecOff failed: Expected message or property value was not received");
        }
    }

    public void testCreateConsumerWithMsgSelectorNoLocalTopic_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsTCFBindings.createContext();

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsTopic, "Team = 'SIB'", false);

        JMSProducer jmsProducer = jmsContext.createProducer();
        TextMessage sendmsg = jmsContext.createTextMessage("testCreateConsumerWithMsgSelectorNoLocalTopic_B_SecOff");
        sendmsg.setStringProperty("Team", "SIB");
        jmsProducer.send(jmsTopic, sendmsg);

        TextMessage recmsg = (TextMessage) jmsConsumer.receive(DEFAULT_TIMEOUT);

        boolean testFailed = false;
        if ( (recmsg == null) ||
             (recmsg.getText() == null) ||
             !recmsg.getText().equals("testCreateConsumerWithMsgSelectorNoLocalTopic_B_SecOff") ||
             !recmsg.getStringProperty("Team").equals("SIB")) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testCreateConsumerWithMsgSelectorNoLocalTopic_B_SecOff failed: Expected message or property value was not received");
        }
    }

    public void testCreateConsumerWithMsgSelectorNoLocalTopic_TcpIp_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsTCFTCP.createContext();

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsTopic, "Team = 'SIB'", false);

        JMSProducer jmsProducer = jmsContext.createProducer();
        TextMessage sendmsg = jmsContext.createTextMessage("testCreateConsumerWithMsgSelectorNoLocalTopic_TcpIp_SecOff");
        sendmsg.setStringProperty("Team", "SIB");
        jmsProducer.send(jmsTopic, sendmsg);

        TextMessage msg = (TextMessage) jmsConsumer.receive(DEFAULT_TIMEOUT);

        boolean testFailed = false;
        if ( (msg == null) ||
             (msg.getText() == null) ||
             !msg.getText().equals("testCreateConsumerWithMsgSelectorNoLocalTopic_TcpIp_SecOff") ||
             !msg.getStringProperty("Team").equals("SIB") ) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testCreateConsumerWithMsgSelectorNoLocalTopic_TcpIp_SecOff failed: Expected message or property value was not received");
        }
    }

    public void testCreateConsumerWithInvalidMsgSelectorNoLocal_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsQCFBindings.createContext();

        boolean testFailed = false;
        try {
            jmsContext.createConsumer(jmsQueue, "bad selector", false);
            testFailed = true;
        } catch (InvalidSelectorRuntimeException e) {
            // expected
        }
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testCreateConsumerWithInvalidMsgSelectorNoLocal_B_SecOff failed: Expected exception was not seen");
        }
    }

    public void testCreateConsumerWithInvalidMsgSelectorNoLocal_TcpIp_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsQCFTCP.createContext();

        boolean testFailed = false;
        try {
            jmsContext.createConsumer(jmsQueue, "bad selector", false);
            testFailed = true;
        } catch (InvalidSelectorRuntimeException e) {
            // expected
        }
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testCreateConsumerWithInvalidMsgSelectorNoLocal_TcpIp_SecOff failed: Expected exception was not seen");
        }
    }

    public void testCreateConsumerWithInvalidMsgSelectorNoLocal_Topic_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsTCFBindings.createContext();

        boolean testFailed = false;
        try {
            jmsContext.createConsumer(jmsTopic, "bad selector", false);
            testFailed = true;
        } catch (InvalidSelectorRuntimeException e) {
            // expected
        }

        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testCreateConsumerWithInvalidMsgSelectorNoLocal_Topic_B_SecOff failed: Expected exception was not seen");
        }
    }

    public void testCreateConsumerWithInvalidMsgSelectorNoLocal_Topic_TcpIp_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsTCFTCP.createContext();

        boolean testFailed = false;
        try {
            jmsContext.createConsumer(jmsTopic, "bad selector", false);
            testFailed = true;
        } catch (InvalidSelectorRuntimeException e) {
            // expected
        }
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testCreateConsumerWithInvalidMsgSelectorNoLocal_Topic_TcpIp_SecOff failed: Expected exception was not seen");
        }
    }

    public void testCreateConsumerWithMsgSelectorNoLocalNullDest_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsQCFBindings.createContext();

        boolean testFailed = false;
        try {
            jmsContext.createConsumer(null, "Team = 'SIB'", false);
            testFailed = true;
        } catch ( InvalidDestinationRuntimeException e ) {
            // expected
        }

        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testCreateConsumerWithMsgSelectorNoLocalNullDest_B_SecOff failed: Expected exception was not seen");
        }
    }

    public void testCreateConsumerWithMsgSelectorNoLocalNullDest_TcpIp_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsQCFTCP.createContext();

        boolean testFailed = false;
        try {
            jmsContext.createConsumer(null, "Team = 'SIB'", false);
            testFailed = true;
        } catch ( InvalidDestinationRuntimeException e ) {
            // expected
        }

        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testCreateConsumerWithMsgSelectorNoLocalNullDest_TcpIp_SecOff failed: Expected exception was not seen");
        }
    }

    public void testCreateConsumerWithMsgSelectorNoLocalNullDest_Topic_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsTCFBindings.createContext();

        boolean testFailed = false;
        try {
            jmsContext.createConsumer(null, "Team = 'SIB'", false);
            testFailed = true;
        } catch ( InvalidDestinationRuntimeException e ) {
            // expected
        }

        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testCreateConsumerWithMsgSelectorNoLocalNullDest_Topic_B_SecOff failed: Expected exception was not seen");
        }
    }

    public void testCreateConsumerWithMsgSelectorNoLocalNullDest_Topic_TcpIp_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsTCFTCP.createContext();

        boolean testFailed = false;
        try {
            jmsContext.createConsumer(null, "Team = 'SIB'", false);
            testFailed = true;
        } catch ( InvalidDestinationRuntimeException e ) {
            // expected
        }

        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testCreateConsumerWithMsgSelectorNoLocalNullDest_Topic_TcpIp_SecOff failed: Expected exception was not seen");
        }
    }

    public void testCreateConsumerWithNullMsgSelectorNullDestNoLocal_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsQCFBindings.createContext();

        boolean testFailed = false;
        try {
            jmsContext.createConsumer(null, null, false);
            testFailed = true;
        } catch ( InvalidDestinationRuntimeException e ) {
            // expected
        }

        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testCreateConsumerWithNullMsgSelectorNullDestNoLocal_B_SecOff failed: Expected exception was not seen");
        }
    }

    public void testCreateConsumerWithNullMsgSelectorNullDestNoLocal_TcpIp_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsQCFTCP.createContext();

        boolean testFailed = false;
        try {
            jmsContext.createConsumer(null, null, false);
            testFailed = true;
        } catch ( InvalidDestinationRuntimeException e ) {
            // expected
        }

        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testCreateConsumerWithNullMsgSelectorNullDestNoLocal_TcpIp_SecOff failed: Expected exception was not seen");
        }
    }

    public void testCreateConsumerWithNullMsgSelectorNullDestNoLocal_Topic_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsTCFBindings.createContext();

        boolean testFailed = false;
        try {
            jmsContext.createConsumer(null, null, false);
            testFailed = true;
        } catch ( InvalidDestinationRuntimeException e ) {
            // expected
        }

        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testCreateConsumerWithNullMsgSelectorNullDestNoLocal_Topic_B_SecOff failed: Expected exception was not seen");
        }
    }

    public void testCreateConsumerWithNullMsgSelectorNullDestNoLocal_Topic_TcpIp_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsTCFTCP.createContext();

        boolean testFailed = false;
        try {
            jmsContext.createConsumer(null, null, false);
            testFailed = true;
        } catch ( InvalidDestinationRuntimeException e ) {
            // expected
        }

        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testCreateConsumerWithNullMsgSelectorNullDestNoLocal_Topic_B_SecOff failed: Expected exception was not seen");
        }
    }

    public void testCreateConsumerWithNullMsgSelector_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsQCFBindings.createContext();
        emptyQueue(jmsQCFBindings, jmsQueue1);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue1, null);

        JMSProducer jmsProducer = jmsContext.createProducer();
        TextMessage msg = jmsContext.createTextMessage("testCreateConsumerWithNullMsgSelector_B_SecOff");
        msg.setStringProperty("MessageType", "text");
        jmsProducer.send(jmsQueue1, msg);

        TextMessage recmsg = (TextMessage) jmsConsumer.receive(DEFAULT_TIMEOUT);

        boolean testFailed = false;
        if ( (recmsg == null) ||
             (recmsg.getText() == null) ||
             !recmsg.getText().equals("testCreateConsumerWithNullMsgSelector_B_SecOff") ) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testCreateConsumerWithNullMsgSelector_B_SecOff failed: Expected message was not received");
        }
    }

    public void testCreateConsumerWithNullMsgSelector_TcpIp_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsQCFTCP.createContext();
        emptyQueue(jmsQCFTCP, jmsQueue1);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue1, null);

        JMSProducer jmsProducer = jmsContext.createProducer();
        TextMessage msg = jmsContext.createTextMessage("testCreateConsumerWithNullMsgSelector_B_SecOff");
        msg.setStringProperty("MessageType", "text");
        jmsProducer.send(jmsQueue1, msg);

        TextMessage recmsg = (TextMessage) jmsConsumer.receive(DEFAULT_TIMEOUT);

        boolean testFailed = false;
        if ( (recmsg == null) ||
             (recmsg.getText() == null) ||
             !recmsg.getText().equals("testCreateConsumerWithNullMsgSelector_B_SecOff") ) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testCreateConsumerWithNullMsgSelector_B_SecOff failed: Expected message was not received");
        }
    }

    public void testCreateConsumerWithNullMsgSelector_Topic_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsTCFBindings.createContext();

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsTopic, null);

        JMSProducer jmsProducer = jmsContext.createProducer();
        TextMessage msg = jmsContext.createTextMessage("testCreateConsumerWithNullMsgSelector_Topic_B_SecOff");
        msg.setStringProperty("MessageType", "text");
        jmsProducer.send(jmsTopic, msg);

        TextMessage recmsg = (TextMessage) jmsConsumer.receive();

        boolean testFailed = false;
        if ( (recmsg == null) ||
             (recmsg.getText() == null) ||
             !recmsg.getText().equals("testCreateConsumerWithNullMsgSelector_Topic_B_SecOff") ) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testCreateConsumerWithNullMsgSelector_Topic_B_SecOff failed: Expected message was not received");
        }
    }

    public void testCreateConsumerWithNullMsgSelector_Topic_TcpIp_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsTCFTCP.createContext();

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsTopic, null);

        JMSProducer jmsProducer = jmsContext.createProducer();
        TextMessage msg = jmsContext.createTextMessage("testCreateConsumerWithNullMsgSelector_Topic_TcpIp_SecOff");
        msg.setStringProperty("MessageType", "text");
        jmsProducer.send(jmsTopic, msg);

        TextMessage recmsg = (TextMessage) jmsConsumer.receive();

        boolean testFailed = false;
        if ( (recmsg == null) ||
             (recmsg.getText() == null) ||
             !recmsg.getText().equals("testCreateConsumerWithNullMsgSelector_Topic_TcpIp_SecOff") ) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testCreateConsumerWithNullMsgSelector_Topic_TcpIp_SecOff failed: Expected message was not received");
        }
    }

    public void testCreateConsumerWithNullMsgSelectorNoLocal_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsQCFBindings.createContext();
        emptyQueue(jmsQCFBindings, jmsQueue1);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue1, null, false);

        JMSProducer jmsProducer = jmsContext.createProducer();
        TextMessage msg = jmsContext.createTextMessage("testCreateConsumerWithNullMsgSelectorNoLocal_B_SecOff");
        msg.setStringProperty("MessageType", "text");
        jmsProducer.send(jmsQueue1, msg);

        TextMessage recmsg = (TextMessage) jmsConsumer.receive(DEFAULT_TIMEOUT);

        boolean testFailed = false;
        if ( (recmsg == null) ||
             (recmsg.getText() == null) ||
             !recmsg.getText().equals("testCreateConsumerWithNullMsgSelectorNoLocal_B_SecOff") ) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testCreateConsumerWithNullMsgSelectorNoLocal_B_SecOff failed: Expected message was not received");
        }
    }

    public void testCreateConsumerWithNullMsgSelectorNoLocal_TcpIp_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsQCFTCP.createContext();
        emptyQueue(jmsQCFTCP, jmsQueue1);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue1, null, false);

        JMSProducer jmsProducer = jmsContext.createProducer();
        TextMessage msg = jmsContext.createTextMessage("testCreateConsumerWithNullMsgSelectorNoLocal_TcpIp_SecOff");
        msg.setStringProperty("MessageType", "text");
        jmsProducer.send(jmsQueue1, msg);

        TextMessage recmsg = (TextMessage) jmsConsumer.receive(DEFAULT_TIMEOUT);

        boolean testFailed = false;
        if ( (recmsg == null) ||
             (recmsg.getText() == null) ||
             !recmsg.getText().equals("testCreateConsumerWithNullMsgSelectorNoLocal_TcpIp_SecOff") ) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testCreateConsumerWithNullMsgSelectorNoLocal_TcpIp_SecOff failed: Expected message was not received");
        }
    }

    public void testCreateConsumerWithNullMsgSelectorNoLocal_Topic_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsTCFBindings.createContext();

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsTopic, null, false);

        JMSProducer jmsProducer = jmsContext.createProducer();
        TextMessage sendmsg = jmsContext.createTextMessage("testCreateConsumerWithNullMsgSelectorNoLocal_Topic_B_SecOff");
        sendmsg.setStringProperty("MessageType", "text");
        jmsProducer.send(jmsTopic, sendmsg);

        TextMessage recmsg = (TextMessage) jmsConsumer.receive(DEFAULT_TIMEOUT);

        boolean testFailed = false;
        if ( (recmsg == null) ||
             (recmsg.getText() == null) ||
             !recmsg.getText().equals("testCreateConsumerWithNullMsgSelectorNoLocal_Topic_B_SecOff") ) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testCreateConsumerWithNullMsgSelectorNoLocal_Topic_B_SecOff failed: Expected message was not received");
        }
    }

    public void testCreateConsumerWithNullMsgSelectorNoLocal_Topic_TcpIp_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsTCFTCP.createContext();

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsTopic, null, false);

        JMSProducer jmsProducer = jmsContext.createProducer();
        TextMessage msg = jmsContext.createTextMessage("testCreateConsumerWithNullMsgSelectorNoLocal_Topic_TcpIp_SecOff");
        msg.setStringProperty("MessageType", "text");
        jmsProducer.send(jmsTopic, msg);

        TextMessage recmsg = (TextMessage) jmsConsumer.receive(DEFAULT_TIMEOUT);

        boolean testFailed = false;
        if ( (recmsg == null) ||
             (recmsg.getText() == null) ||
             !recmsg.getText().equals("testCreateConsumerWithNullMsgSelectorNoLocal_Topic_TcpIp_SecOff") ) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testCreateConsumerWithNullMsgSelectorNoLocal_Topic_TcpIp_SecOff failed: Expected message was not received");
        }
    }

    public void testCreateConsumerWithEmptyMsgSelector_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsQCFBindings.createContext();
        emptyQueue(jmsQCFBindings, jmsQueue1);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue1, "");

        JMSProducer jmsProducer = jmsContext.createProducer();
        TextMessage msg = jmsContext.createTextMessage("testCreateConsumerWithEmptyMsgSelector_B_SecOff");
        msg.setStringProperty("MessageType", "text");
        jmsProducer.send(jmsQueue1, msg);

        TextMessage recmsg = (TextMessage) jmsConsumer.receive(DEFAULT_TIMEOUT);

        boolean testFailed = false;
        if ( (recmsg == null) ||
             (recmsg.getText() == null) ||
             !recmsg.getText().equals("testCreateConsumerWithEmptyMsgSelector_B_SecOff") ) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testCreateConsumerWithEmptyMsgSelector_B_SecOff failed: Expected message was not received");
        }
    }

    public void testCreateConsumerWithEmptyMsgSelector_TcpIp_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsQCFTCP.createContext();
        emptyQueue(jmsQCFTCP, jmsQueue1);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue1, "");

        JMSProducer jmsProducer = jmsContext.createProducer();
        TextMessage msg = jmsContext.createTextMessage("testCreateConsumerWithEmptyMsgSelector_TcpIp_SecOff");
        msg.setStringProperty("MessageType", "text");
        jmsProducer.send(jmsQueue1, msg);

        TextMessage recmsg = (TextMessage) jmsConsumer.receive(DEFAULT_TIMEOUT);

        boolean testFailed = false;
        if ( (recmsg == null) ||
             (recmsg.getText() == null) ||
             !recmsg.getText().equals("testCreateConsumerWithEmptyMsgSelector_TcpIp_SecOff") ) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testCreateConsumerWithEmptyMsgSelector_TcpIp_SecOff failed: Expected message was not received");
        }
    }

    public void testCreateConsumerWithEmptyMsgSelector_Topic_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsTCFBindings.createContext();

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsTopic, "");

        JMSProducer jmsProducer = jmsContext.createProducer();
        TextMessage msg = jmsContext.createTextMessage("testCreateConsumerWithEmptyMsgSelector_Topic_B_SecOff");
        msg.setStringProperty("MessageType", "text");
        jmsProducer.send(jmsTopic, msg);

        TextMessage recmsg = (TextMessage) jmsConsumer.receive(DEFAULT_TIMEOUT);

        boolean testFailed = false;
        if ( (recmsg == null) ||
             (recmsg.getText() == null) ||
             !recmsg.getText().equals("testCreateConsumerWithEmptyMsgSelector_Topic_B_SecOff") ) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testCreateConsumerWithEmptyMsgSelector_Topic_B_SecOff failed: Expected message was not received");
        }
    }

    public void testCreateConsumerWithEmptyMsgSelector_Topic_TcpIp_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsTCFTCP.createContext();

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsTopic, "");

        JMSProducer jmsProducer = jmsContext.createProducer();
        TextMessage msg = jmsContext.createTextMessage("testCreateConsumerWithEmptyMsgSelector_Topic_TcpIp_SecOff");
        msg.setStringProperty("MessageType", "text");
        jmsProducer.send(jmsTopic, msg);

        TextMessage recmsg = (TextMessage) jmsConsumer.receive(DEFAULT_TIMEOUT);

        boolean testFailed = false;
        if ( (recmsg == null) ||
             (recmsg.getText() == null) ||
             !recmsg.getText().equals("testCreateConsumerWithEmptyMsgSelector_Topic_TcpIp_SecOff") ) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testCreateConsumerWithEmptyMsgSelector_Topic_TcpIp_SecOff failed: Expected message was not received");
        }
    }

    public void testCreateConsumerWithEmptyMsgSelectorNoLocal_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsQCFBindings.createContext();
        emptyQueue(jmsQCFBindings, jmsQueue1);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue1, "", false);

        JMSProducer jmsProducer = jmsContext.createProducer();
        TextMessage msg = jmsContext.createTextMessage("testCreateConsumerWithEmptyMsgSelectorNoLocal_B_SecOff");
        msg.setStringProperty("MessageType", "text");
        jmsProducer.send(jmsQueue1, msg);

        TextMessage recmsg = (TextMessage) jmsConsumer.receive(DEFAULT_TIMEOUT);

        boolean testFailed = false;
        if ( (recmsg == null) ||
             (recmsg.getText() == null) ||
             !recmsg.getText().equals("testCreateConsumerWithEmptyMsgSelectorNoLocal_B_SecOff") ) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testCreateConsumerWithEmptyMsgSelectorNoLocal_B_SecOff failed: Expected message was not received");
        }
    }

    public void testCreateConsumerWithEmptyMsgSelectorNoLocal_TcpIp_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsQCFTCP.createContext();
        emptyQueue(jmsQCFTCP, jmsQueue1);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue1, "", false);

        JMSProducer jmsProducer = jmsContext.createProducer();
        TextMessage msg = jmsContext.createTextMessage("testCreateConsumerWithEmptyMsgSelectorNoLocal_TcpIp_SecOff");
        msg.setStringProperty("MessageType", "text");
        jmsProducer.send(jmsQueue1, msg);

        TextMessage recmsg = (TextMessage) jmsConsumer.receive(DEFAULT_TIMEOUT);

        boolean testFailed = false;
        if ( (recmsg == null) ||
             (recmsg.getText() == null) ||
             !recmsg.getText().equals("testCreateConsumerWithEmptyMsgSelectorNoLocal_TcpIp_SecOff") ) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testCreateConsumerWithEmptyMsgSelectorNoLocal_TcpIp_SecOff failed: Expected message was not received");
        }
    }

    public void testCreateConsumerWithEmptyMsgSelectorNoLocal_Topic_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        Topic localJMSTopic = (Topic) new InitialContext().lookup("java:comp/env/eis/topic12");

        JMSContext jmsContext = jmsTCFBindings.createContext();

        JMSConsumer jmsConsumer = jmsContext.createConsumer(localJMSTopic, "", false);

        JMSProducer jmsProducer = jmsContext.createProducer();

        TextMessage msg = jmsContext.createTextMessage("testCreateConsumerWithEmptyMsgSelectorNoLocal_Topic_B_SecOff");
        msg.setStringProperty("MessageType", "text");
        jmsProducer.send(localJMSTopic, msg);

        TextMessage recmsg = (TextMessage) jmsConsumer.receive(5000);

        boolean testFailed = false;
        if ( (recmsg == null) ||
             (recmsg.getText() == null) ||
             !recmsg.getText().equals("testCreateConsumerWithEmptyMsgSelectorNoLocal_Topic_B_SecOff") ) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testCreateConsumerWithEmptyMsgSelectorNoLocal_Topic_B_SecOff failed: Expected message was not received");
        }
    }

    public void testCreateConsumerWithEmptyMsgSelectorNoLocal_Topic_TcpIp_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsTCFTCP.createContext();

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsTopic, "", false);

        JMSProducer jmsProducer = jmsContext.createProducer();
        TextMessage msg = jmsContext.createTextMessage("testCreateConsumerWithEmptyMsgSelectorNoLocal_Topic_TcpIp_SecOff");
        msg.setStringProperty("MessageType", "text");
        jmsProducer.send(jmsTopic, msg);

        TextMessage recmsg = (TextMessage) jmsConsumer.receive(DEFAULT_TIMEOUT);

        boolean testFailed = false;
        if ( (recmsg == null) ||
             (recmsg.getText() == null) ||
             !recmsg.getText().equals("testCreateConsumerWithEmptyMsgSelectorNoLocal_Topic_TcpIp_SecOff") ) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testCreateConsumerWithEmptyMsgSelectorNoLocal_Topic_TcpIp_SecOff failed: Expected message was not received");
        }
    }

    public void testCreateConsumerWithMsgSelectorNoLocalInvalidDest_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsQCFBindings.createContext();

        Queue localJMSQueue = (Queue)
            new InitialContext().lookup("java:comp/env/eis/queue11");

        boolean testFailed = false;
        try {
            jmsContext.createConsumer(localJMSQueue, "Destination = 'Queue'", false);
            testFailed = true;
        } catch ( InvalidDestinationRuntimeException e ) {
            // expected
        }

        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testCreateConsumerWithMsgSelectorNoLocalInvalidDest_B_SecOff failed: Expected exception was not seen");
        }
    }

    public void testCreateConsumerWithMsgSelectorNoLocalInvalidDest_TcpIp_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsQCFBindings.createContext();

        Queue localJMSQueue = (Queue)
            new InitialContext().lookup("java:comp/env/eis/queue11");

        boolean testFailed = false;
        try {
            jmsContext.createConsumer(localJMSQueue, "Destination = 'Queue'", false);
            testFailed = true;
        } catch ( InvalidDestinationRuntimeException e ) {
            // expected
        }

        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testCreateConsumerWithMsgSelectorNoLocalInvalidDest_TcpIp_SecOff failed: Expected exception was not seen");
        }
    }

    public void testCreateConsumerWithMsgSelectorNoLocalInvalidDest_Topic_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsTCFBindings.createContext();

        Topic localJMSTopic = (Topic)
            new InitialContext().lookup("java:comp/env/eis/topic11");

        boolean testFailed = false;
        try {
            jmsContext.createConsumer(localJMSTopic, "Destination = 'Topic'", false);
            testFailed = true;
        } catch ( InvalidDestinationRuntimeException e ) {
            // expected
        }

        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testCreateConsumerWithMsgSelectorNoLocalInvalidDest_Topic_B_SecOff failed: Expected exception was not seen");
        }
    }

    public void testCreateConsumerWithMsgSelectorNoLocalInvalidDest_Topic_TcpIp_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsTCFBindings.createContext();

        Topic localJMSTopic = (Topic)
            new InitialContext().lookup("java:comp/env/eis/topic11");

        boolean testFailed = false;
        try {
            jmsContext.createConsumer(localJMSTopic, "Destination = 'Topic'", false);
            testFailed = true;
        } catch ( InvalidDestinationRuntimeException e ) {
            // expected
        }

        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testCreateConsumerWithMsgSelectorNoLocalInvalidDest_Topic_TcpIp_SecOff failed: Expected exception was not seen");
        }
    }

    public void testCloseConsumerDepth_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsTCFBindings.createContext();

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsTopic);

        JMSProducer jmsProducer = jmsContext.createProducer();
        TextMessage msg = jmsContext.createTextMessage("testCloseConsumerDepth_B_SecOff");
        msg.setStringProperty("MessageType", "text");
        jmsProducer.send(jmsTopic, msg);
        jmsProducer.send(jmsTopic, msg);

        MBeanServer mbs0 = ManagementFactory.getPlatformMBeanServer();
        ObjectName name0 = new ObjectName("WebSphere:feature=wasJmsServer,type=Topic,name=NewTopic1");
        String id0 = (String) mbs0.getAttribute(name0, "Id");
        long depth0 = (Long) mbs0.getAttribute(name0, "Depth");

        jmsConsumer.close();

        Thread.sleep(2000);

        jmsProducer.send(jmsTopic, msg);

        MBeanServer mbs1 = ManagementFactory.getPlatformMBeanServer();

        ObjectName name1 = new ObjectName("WebSphere:feature=wasJmsServer,type=Topic,name=NewTopic1");
        String id1 = (String) mbs1.getAttribute(name1, "Id");
        long depth1 = (Long) mbs1.getAttribute(name1, "Depth");

        jmsContext.close();

        if ( depth1 != 0 ) {
            throw new Exception("testListSubscriber_TcpIp failed: Number of subscriptions is not 0");
        }
    }

    public void testCloseConsumerDepth_TcpIp_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsTCFTCP.createContext();

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsTopic);

        JMSProducer jmsProducer = jmsContext.createProducer();

        TextMessage msg = jmsContext.createTextMessage("testCloseConsumerDepth_TcpIp_SecOff");
        msg.setStringProperty("MessageType", "text");
        jmsProducer.send(jmsTopic, msg);
        jmsProducer.send(jmsTopic, msg);

        MBeanServer mbs0 = ManagementFactory.getPlatformMBeanServer();
        ObjectName name0 = new ObjectName("WebSphere:feature=wasJmsServer,type=Topic,name=NewTopic1");
        String id0 = (String) mbs0.getAttribute(name0, "Id");
        long depth0 = (Long) mbs0.getAttribute(name0, "Depth");

        jmsConsumer.close();

        Thread.sleep(2000);

        jmsProducer.send(jmsTopic, msg);

        MBeanServer mbs1 = ManagementFactory.getPlatformMBeanServer();

        ObjectName name1 = new ObjectName("WebSphere:feature=wasJmsServer,type=Topic,name=NewTopic1");
        String id1 = (String) mbs1.getAttribute(name1, "Id");
        long depth1 = (Long) mbs1.getAttribute(name1, "Depth");

        jmsConsumer.close();
        jmsContext.close();

        if ( depth1 != 0 ) {
            throw new Exception("testListSubscriber_TcpIp failed: Number of subscriptions is not 0");
        }
    }

    public void testListSubscriber_B(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsTCFBindings.createContext();

        JMSProducer jmsProducer = jmsContext.createProducer();

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsTopic);

        MBeanServer mbs0 = ManagementFactory.getPlatformMBeanServer();
        ObjectName name0 = new ObjectName("WebSphere:feature=wasJmsServer,type=Topic,name=NewTopic1");
        CompositeData[] obn0 = (CompositeData[]) mbs0.invoke(name0, "listSubscriptions", null, null);

        jmsConsumer.close();

        MBeanServer mbs1 = ManagementFactory.getPlatformMBeanServer();
        ObjectName name1 = new ObjectName("WebSphere:feature=wasJmsServer,type=Topic,name=NewTopic1");
        CompositeData[] obn1 = (CompositeData[]) mbs1.invoke(name1, "listSubscriptions", null, null);

        jmsContext.close();

        if ( obn1.length != 0 ){
            throw new Exception("testListSubscriber_TcpIp failed: Number of subscriptions is not 0");
        }
    }

    public void testListSubscriber_TcpIp(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsTCFTCP.createContext();

        JMSProducer jmsProducer = jmsContext.createProducer();
        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsTopic);

        MBeanServer mbs0 = ManagementFactory.getPlatformMBeanServer();
        ObjectName name0 = new ObjectName("WebSphere:feature=wasJmsServer,type=Topic,name=NewTopic1");
        CompositeData[] obn0 = (CompositeData[]) mbs0.invoke(name0, "listSubscriptions", null, null);

        jmsConsumer.close();

        MBeanServer mbs1 = ManagementFactory.getPlatformMBeanServer();
        ObjectName name1 = new ObjectName("WebSphere:feature=wasJmsServer,type=Topic,name=NewTopic1");
        CompositeData[] obn1 = (CompositeData[]) mbs1.invoke(name1, "listSubscriptions", null, null);

        jmsContext.close();

        if ( obn1.length != 0 ) {
            throw new Exception("testListSubscriber_TcpIp failed: Number of subscriptions is not 0");
        }
    }

    public void testNoLocalTrue_B(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsTCFBindings.createContext();

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsTopic, "", true);

        JMSProducer jmsProducer = jmsContext.createProducer();
        TextMessage msg = jmsContext.createTextMessage("testNoLocalTrue_B");
        msg.setStringProperty("MessageType", "text");
        jmsProducer.send(jmsTopic, msg);

        TextMessage recmsg = (TextMessage) jmsConsumer.receive(DEFAULT_TIMEOUT);

        boolean testFailed = false;
        if ( recmsg != null ) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testNoLocalTrue_B failed: Received the message");
        }
    }

    public void testNoLocalTrue_TcpIp(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        TopicConnectionFactory localJMSTCF = (TopicConnectionFactory)
            new InitialContext().lookup("java:comp/env/eis/tcf1");
        Topic localJMSTopic = (Topic)
            new InitialContext().lookup("java:comp/env/eis/topic1");

        JMSContext jmsContext = localJMSTCF.createContext();

        JMSProducer jmsProducer = jmsContext.createProducer();

        JMSConsumer jmsConsumer = jmsContext.createConsumer(localJMSTopic, "", true);

        TextMessage msg = jmsContext.createTextMessage("testNoLocalTrue_TcpIp");
        msg.setStringProperty("MessageType", "text");
        jmsProducer.send(localJMSTopic, msg);

        TextMessage recmsg = (TextMessage) jmsConsumer.receive(DEFAULT_TIMEOUT);

        boolean testFailed = false;
        if ( recmsg != null) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testNoLocalTrue_TcpIp failed");
        }
    }

    public void testNoLocalTrueQueue_B(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsQCFBindings.createContext();
        emptyQueue(jmsQCFBindings, jmsQueue);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue, "", true);

        JMSProducer jmsProducer = jmsContext.createProducer();
        TextMessage msg = jmsContext.createTextMessage("testNoLocalTrueQueue_B");
        msg.setStringProperty("MessageType", "text");
        jmsProducer.send(jmsQueue, msg);

        TextMessage recmsg = (TextMessage) jmsConsumer.receive(DEFAULT_TIMEOUT);

        boolean testFailed = false;
        if ( recmsg == null ) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testNoLocalTrueQueue_B failed: message was not received");
        }
    }

    public void testNoLocalTrueQueue_TcpIp(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsQCFTCP.createContext();
        emptyQueue(jmsQCFTCP, jmsQueue);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue, "", true);

        JMSProducer jmsProducer = jmsContext.createProducer();
        TextMessage msg = jmsContext.createTextMessage("testNoLocalTrueQueue_TcpIp");
        msg.setStringProperty("MessageType", "text");
        jmsProducer.send(jmsQueue, msg);

        TextMessage recmsg = (TextMessage) jmsConsumer.receive(DEFAULT_TIMEOUT);

        boolean testFailed = false;
        if ( recmsg == null ) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testNoLocalTrueQueue_TcpIp failed: message was not received");
        }
    }
}
