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
package jmscontext_118067.web;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.Enumeration;

import javax.jms.InvalidDestinationRuntimeException;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.JMSProducer;
import javax.jms.JMSRuntimeException;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.jms.QueueConnectionFactory;
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
public class JMSContext_118067Servlet extends HttpServlet {

    public static QueueConnectionFactory jmsQCFBindings;
    public static QueueConnectionFactory jmsQCFTCP;

    public static TopicConnectionFactory jmsTCFBindings;
    public static TopicConnectionFactory jmsTCFTCP;

    public static Queue jmsQueue;
    public static Topic jmsTopic;
    public static Topic jmsTopic2;

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

        jmsTCFBindings = getTCF("java:comp/env/eis/tcf");
        jmsTCFTCP = getTCF("java:comp/env/eis/tcf1");

        jmsQueue = getQueue("java:comp/env/jndi_INPUT_Q");
        jmsTopic = getTopic("java:comp/env/eis/topic1");
        jmsTopic2 = getTopic("java:comp/env/eis/topic2");

        if ( jmsQCFBindings == null ) {
            throw new ServletException("Null 'jmsQCFBindings'");
        }
        if ( jmsQCFTCP == null ) {
            throw new ServletException("Null 'jmsQCFTCP'");
        }

        if ( jmsTCFBindings == null ) {
            throw new ServletException("Null 'jmsTCFBindings'");
        }
        if ( jmsTCFTCP == null ) {
            throw new ServletException("Null 'jmsTCFTCP'");
        }

        if ( jmsQueue == null ) {
            throw new ServletException("Null 'jmsQueue'");
        }
        if ( jmsTopic == null ) {
            throw new ServletException("Null 'jmsTopic'");
        }
        if ( jmsTopic2 == null ) {
            throw new ServletException("Null 'jmsTopic2'");
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
        TraceComponent tc = Tr.register(JMSContext_118067Servlet.class);

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

    public void testCreateJmsProducerAndSend_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContext = jmsQCFBindings.createContext();
        emptyQueue(jmsQCFBindings, jmsQueue);

        JMSProducer jmsProducer = jmsContext.createProducer();
        jmsProducer.send(jmsQueue, "testCreateJmsProducerAndSend_B_SecOff");

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);
        TextMessage msg = (TextMessage) jmsConsumer.receive(30000);

        boolean testFailed = false;
        if ( (msg == null) ||
             (msg.getText() == null) ||
             !msg.getText().equals("testCreateJmsProducerAndSend_B_SecOff") ) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testCreateJmsProducerAndSend_B_SecOff failed: Expected message was not received");
        }
    }

    public void testCreateJmsProducerAndSend_TCP_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContext = jmsQCFTCP.createContext();
        emptyQueue(jmsQCFTCP, jmsQueue);

        JMSProducer jmsProducer = jmsContext.createProducer();
        jmsProducer.send(jmsQueue, "testCreateJmsProducerAndSend_TCP_SecOff");

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);
        TextMessage msg = (TextMessage) jmsConsumer.receive(30000);

        boolean testFailed = false;
        if ( (msg == null) ||
             (msg.getText() == null) ||
             !msg.getText().equals("testCreateJmsProducerAndSend_TCP_SecOff") ) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testCreateJmsProducerAndSend_TCP_SecOff failed: Expected message was not received");
        }
    }

    public void testSetMessagePropertyBindings_Send(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContext = jmsQCFBindings.createContext();
        emptyQueue(jmsQCFBindings, jmsQueue);

        JMSProducer jmsProducer = jmsContext.createProducer();
        jmsProducer.setDisableMessageID(true);
        jmsProducer.send(jmsQueue, "testSetMessagePropertyBindings_Send");

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);
        TextMessage msg = (TextMessage) jmsConsumer.receive(30000);

        boolean testFailed = false;
        if ( msg.getJMSMessageID() != null ) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testSetMessagePropertyBindings_Send failed: JMSMessageID was not null");
        }
    }

    public void testSetMessagePropertyTcpIp_Send(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContext = jmsQCFTCP.createContext();
        emptyQueue(jmsQCFTCP, jmsQueue);

        JMSProducer jmsProducer = jmsContext.createProducer();
        jmsProducer.setDisableMessageID(true);
        jmsProducer.send(jmsQueue, "testSetMessagePropertyTcpIp_Send");

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);
        TextMessage msg = (TextMessage) jmsConsumer.receive(30000);

        boolean testFailed = false;
        if ( msg.getJMSMessageID() != null ) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testSetMessagePropertyTcpIp_Send failed: JMSMessageID was not null");
        }
    }

    public void testQueueNameNull_B(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContext = jmsQCFBindings.createContext();

        JMSConsumer jmsConsumer = null;

        boolean testFailed = false;

        try {
            Queue queue = jmsContext.createQueue(null);

            JMSProducer jmsProducer = jmsContext.createProducer();
            jmsProducer.send(queue, "testQueueNameNull_B");

            jmsConsumer = jmsContext.createConsumer(queue);
            TextMessage m = (TextMessage) jmsConsumer.receive(30000);

            testFailed = true;

        } catch ( JMSRuntimeException e ) {
            // Expected
        }

        if ( jmsConsumer != null ) {
            jmsConsumer.close();
        }
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testQueueNameNull_B failed: Expected JMSRuntimeException was not seen");
        }
    }

    public void testQueueNameNull_TcpIp(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContext = jmsQCFTCP.createContext();

        JMSConsumer jmsConsumer = null;

        boolean testFailed = false;

        try {
            Queue queue = jmsContext.createQueue(null);

            JMSProducer jmsProducer = jmsContext.createProducer();
            jmsProducer.send(queue, "testQueueNameNull_TCP");

            jmsConsumer = jmsContext.createConsumer(queue);
            TextMessage m = (TextMessage) jmsConsumer.receive(30000);

            testFailed = true;

        } catch ( JMSRuntimeException e ) {
            // Expected
        }

        if ( jmsConsumer != null ) {
            jmsConsumer.close();
        }
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testQueueNameNull_TcpIp failed: Expected JMSRuntimeException was not seen");
        }
    }

    public void testQueueNameEmptyString_B(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContext = jmsQCFBindings.createContext();

        boolean testFailed = false;
        try {
            Queue queue = jmsContext.createQueue("");
            // jmsContext.createProducer().send(queue, "testQueueNameEmpty_B");
            testFailed = true;
        } catch ( JMSRuntimeException e ) {
            // Expected
        }

        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testQueueNameEmptyString_B failed: Expected JMSRuntimeException was not seen");
        }
    }

    public void testQueueNameEmptyString_TcpIp(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContext = jmsQCFTCP.createContext();

        boolean testFailed = false;
        try {
            Queue queue = jmsContext.createQueue("");
            // jmsContext.createProducer().send(queue, "testQueueNameEmptyString_TcpIp");
            testFailed = true;
        } catch ( JMSRuntimeException e ) {
            // Expected
        }

        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testQueueNameEmptyString_TcpIp failed: Expected JMSRuntimeException was not seen");
        }
    }

    public void testQueueNameWildChars_B(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContext = jmsQCFBindings.createContext();

        JMSConsumer jmsConsumer = null;

        boolean testFailed = false;
        try {
            Queue queue = jmsContext.createQueue("ppp*");

            JMSProducer jmsProducer = jmsContext.createProducer();
            jmsProducer.send(queue, "testQueueNameWildChars_B");

            jmsConsumer = jmsContext.createConsumer(queue);
            TextMessage m = (TextMessage) jmsConsumer.receive(30000);

            testFailed = true;

        } catch ( InvalidDestinationRuntimeException ex ) {
            // Expected
        }

        if ( jmsConsumer != null ) {
            jmsConsumer.close();
        }
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testQueueNameWildChars_B failed: Expected InvalidDestinationRuntimeException was not seen");
        }
    }

    public void testQueueNameWildChars_TcpIp(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContext = jmsQCFTCP.createContext();

        JMSConsumer jmsConsumer = null;

        boolean testFailed = false;

        try {
            Queue queue = jmsContext.createQueue("ppp*");

            JMSProducer jmsProducer = jmsContext.createProducer();
            jmsProducer.send(queue, "testQueueNameWildChars_TcpIp");

            jmsConsumer = jmsContext.createConsumer(queue);
            TextMessage m = (TextMessage) jmsConsumer.receive(30000);

            testFailed = true;

        } catch ( InvalidDestinationRuntimeException ex ) {
            // Expected
        }

        if ( jmsConsumer != null ) {
            jmsConsumer.close();
        }
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testQueueNameWildChars_TcpIp failed: Expected InvalidDestinationRuntimeException was not seen");
        }
    }

    public void testQueueNameWithSpaces_B(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContext = jmsQCFBindings.createContext();
        Queue queue = jmsContext.createQueue("abc xyz");

        JMSProducer jmsProducer = jmsContext.createProducer();
        String text = "Sending testQueueNameWithSpaces_B";
        jmsProducer.send(queue, text);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(queue);
        TextMessage m = (TextMessage) jmsConsumer.receive(30000);

        String failureReason = null;
        if ( m == null ) {
            failureReason = "Null message; expected [ " + text + " ]";
        } else if ( m.getText() == null ) {
            failureReason = "Null message text; exected [ " + text + " ]";
        } else if ( !m.getText().equals(text) ) {
            failureReason = "Received [ " + m.getText() + " ]; expected [ " + text + " ]";
        }

        jmsConsumer.close();
        jmsContext.close();

        if ( failureReason != null ) {
            throw new Exception("testQueueNameWithSpaces_B failed: " + failureReason);
        }
    }

    public void testQueueNameWithSpaces_TcpIp(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContext = jmsQCFTCP.createContext();

        Queue queue = jmsContext.createQueue("abc xyz");

        JMSProducer jmsProducer = jmsContext.createProducer();
        String text = "Sending testQueueNameWithSpaces_TcpIp";
        jmsProducer.send(queue, text);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(queue);
        TextMessage m = (TextMessage) jmsConsumer.receive(30000);

        String failureReason = null;
        if ( m == null ) {
            failureReason = "Null message; expected [ " + text + " ]";
        } else if ( m.getText() == null ) {
            failureReason = "Null message text; exected [ " + text + " ]";
        } else if ( !m.getText().equals(text) ) {
            failureReason = "Received [ " + m.getText() + " ]; expected [ " + text + " ]";
        }

        jmsConsumer.close();
        jmsContext.close();

        if ( failureReason != null ) {
            throw new Exception("testQueueNameWithSpaces_TcpIp failed: " + failureReason);
        }
    }

    public void testQueueName_temp_B(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContext = jmsQCFBindings.createContext();

        Queue queue = jmsContext.createQueue("_tempXYZ");

        JMSProducer jmsProducer = jmsContext.createProducer();
        String text = "testQueueName_temp_B";
        jmsProducer.send(queue, text);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(queue);
        TextMessage m = (TextMessage) jmsConsumer.receive(30000);

        String failureReason = null;
        if ( m == null ) {
            failureReason = "Null message; expected [ " + text + " ]";
        } else if ( m.getText() == null ) {
            failureReason = "Null message text; exected [ " + text + " ]";
        } else if ( !m.getText().equals(text) ) {
            failureReason = "Received [ " + m.getText() + " ]; expected [ " + text + " ]";
        }

        jmsConsumer.close();
        jmsContext.close();

        if ( failureReason != null ) {
            throw new Exception("testQueueName_temp_B failed: " + failureReason);
        }
    }

    public void testQueueName_temp_TcpIp(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContext = jmsQCFTCP.createContext();

        Queue queue = jmsContext.createQueue("_tempXYZ");

        JMSProducer jmsProducer = jmsContext.createProducer();
        String text = "testQueueName_temp_TcpIp";
        jmsProducer.send(queue, text);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(queue);
        TextMessage m = (TextMessage) jmsConsumer.receive(30000);

        String failureReason = null;
        if ( m == null ) {
            failureReason = "Null message; expected [ " + text + " ]";
        } else if ( m.getText() == null ) {
            failureReason = "Null message text; exected [ " + text + " ]";
        } else if ( !m.getText().equals(text) ) {
            failureReason = "Received [ " + m.getText() + " ]; expected [ " + text + " ]";
        }

        jmsConsumer.close();
        jmsContext.close();

        if ( failureReason != null ) {
            throw new Exception("testQueueNameWithSpaces_TcpIp failed: " + failureReason);
        }
    }

    public void testQueueNameLong_B(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContext = jmsQCFBindings.createContext();

        Queue queue = jmsContext.createQueue("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");

        JMSProducer jmsProducer = jmsContext.createProducer();
        String text = "testQueueNameLong_B";
        jmsProducer.send(queue, text);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(queue);
        TextMessage m = (TextMessage) jmsConsumer.receive(30000);

        String failureReason = null;
        if ( m == null ) {
            failureReason = "Null message; expected [ " + text + " ]";
        } else if ( m.getText() == null ) {
            failureReason = "Null message text; exected [ " + text + " ]";
        } else if ( !m.getText().equals(text) ) {
            failureReason = "Received [ " + m.getText() + " ]; expected [ " + text + " ]";
        }

        jmsConsumer.close();
        jmsContext.close();

        if ( failureReason != null ) {
            throw new Exception("testQueueNameLong_B failed: " + failureReason);
        }
    }

    public void testQueueNameLong_TcpIp(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContext = jmsQCFTCP.createContext();

        Queue queue = jmsContext.createQueue("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");

        JMSProducer jmsProducer = jmsContext.createProducer();
        String text = "testQueueNameLong_TcpIp";
        jmsProducer.send(queue, text);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(queue);
        TextMessage m = (TextMessage) jmsConsumer.receive(30000);

        String failureReason = null;
        if ( m == null ) {
            failureReason = "Null message; expected [ " + text + " ]";
        } else if ( m.getText() == null ) {
            failureReason = "Null message text; exected [ " + text + " ]";
        } else if ( !m.getText().equals(text) ) {
            failureReason = "Received [ " + m.getText() + " ]; expected [ " + text + " ]";
        }

        jmsConsumer.close();
        jmsContext.close();

        if ( failureReason != null ) {
            throw new Exception("testQueueNameLong_TcpIp failed: " + failureReason);
        }
    }

    public void testQueueNameCaseSensitive_B(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContext = jmsQCFBindings.createContext();

        boolean testFailed = false;
        try {
            Queue queue = jmsContext.createQueue("MYQUEUE");

            JMSProducer jmsProducer = jmsContext.createProducer();
            jmsProducer.send(queue, "testQueueNameCaseSensitive_B");

            testFailed = true;

        } catch ( JMSRuntimeException ex1 ) {
            // expected
        }

        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testQueueNameCaseSensitive_B failed: Expected JMSRuntimeException was not seen");
        }
    }

    public void testQueueNameCaseSensitive_TcpIp(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContext = jmsQCFTCP.createContext();

        boolean testFailed = false;
        try {
            Queue queue = jmsContext.createQueue("MYQUEUE");

            JMSProducer jmsProducer = jmsContext.createProducer();
            jmsProducer.send(queue, "testQueueNameCaseSensitive_TcpIp");

            testFailed = true;

        } catch ( JMSRuntimeException ex1 ) {
            // expected
        }

        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testQueueNameCaseSensitive_TcpIp failed: Expected JMSRuntimeException was not seen");
        }
    }

    public void testQueueNameQUEUE_B(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContext = jmsQCFBindings.createContext();

        Queue queue = jmsContext.createQueue("QUEUE/queue");

        JMSProducer jmsProducer = jmsContext.createProducer();
        String text = "testQueueNameQUEUE_B";
        jmsProducer.send(queue, text);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(queue);
        TextMessage m = (TextMessage) jmsConsumer.receive(30000);

        String failureReason = null;
        if ( m == null ) {
            failureReason = "Null message; expected [ " + text + " ]";
        } else if ( m.getText() == null ) {
            failureReason = "Null message text; exected [ " + text + " ]";
        } else if ( !m.getText().equals(text) ) {
            failureReason = "Received [ " + m.getText() + " ]; expected [ " + text + " ]";
        }

        jmsConsumer.close();
        jmsContext.close();

        if ( failureReason != null ) {
            throw new Exception("testQueueNameQUEUE_B failed: " + failureReason);
        }
    }

    public void testQueueNameQUEUE_TcpIp(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContext = jmsQCFTCP.createContext();

        Queue queue = jmsContext.createQueue("QUEUE/queue");

        JMSProducer jmsProducer = jmsContext.createProducer();
        String text = "testQueueNameQUEUE_TcpIp";
        jmsProducer.send(queue, text);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(queue);
        TextMessage m = (TextMessage) jmsConsumer.receive(30000);

        String failureReason = null;
        if ( m == null ) {
            failureReason = "Null message; expected [ " + text + " ]";
        } else if ( m.getText() == null ) {
            failureReason = "Null message text; exected [ " + text + " ]";
        } else if ( !m.getText().equals(text) ) {
            failureReason = "Received [ " + m.getText() + " ]; expected [ " + text + " ]";
        }

        jmsConsumer.close();
        jmsContext.close();

        if ( failureReason != null ) {
            throw new Exception("testQueueNameQUEUE_TcpIp failed: " + failureReason);
        }
    }

    public void testTopicNameNull_B(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContext = jmsTCFBindings.createContext();

        JMSConsumer jmsConsumer = null;

        boolean testFailed = false;

        try {
            Topic topic = jmsContext.createTopic(null);

            jmsContext.createProducer().send(topic, "testTopicNameNull_B");

            jmsConsumer = jmsContext.createConsumer(topic);
            TextMessage m1 = (TextMessage) jmsConsumer.receive(30000);

            testFailed = true;

        } catch ( JMSRuntimeException ex ) {
            // Expected
        }

        if ( jmsConsumer != null ) {
            jmsConsumer.close();
        }
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testTopicNameNull_B failed: Expected JMSRuntimeException was not seen");
        }
    }

    public void testTopicNameNull_TcpIp(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContext = jmsTCFTCP.createContext();

        JMSConsumer jmsConsumer = null;

        boolean testFailed = false;

        try {
            Topic topic = jmsContext.createTopic(null);

            jmsContext.createProducer().send(topic, "testTopicNameNull_TcpIP");

            jmsConsumer = jmsContext.createConsumer(topic);
            TextMessage m1 = (TextMessage) jmsConsumer.receive(30000);

            testFailed = true;

        } catch ( JMSRuntimeException ex ) {
            // Expected
        }

        if ( jmsConsumer != null ) {
            jmsConsumer.close();
        }
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testTopicNameNull_TcpIp failed: Expected JMSRuntimeException was not seen");
        }
    }

    public void testTopicNameEmptyString_B(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContext = jmsTCFBindings.createContext();

        JMSConsumer jmsConsumer = null;

        boolean testFailed = false;

        try {
            Topic topic = jmsContext.createTopic("");

            jmsContext.createProducer().send(topic, "testTopicNameEmptyString_B");

            jmsConsumer = jmsContext.createConsumer(topic);
            TextMessage m1 = (TextMessage) jmsConsumer.receive(30000);

        } catch ( JMSRuntimeException ex ) {
            testFailed = true;
            ex.printStackTrace();
        }

        if ( jmsConsumer != null ) {
            jmsConsumer.close();
        }
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testTopicNameEmptyString_B failed: Unexpected JMSRuntimeException was seen");
        }
    }

    public void testTopicNameEmptyString_TcpIp(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContext = jmsTCFTCP.createContext();

        JMSConsumer jmsConsumer = null;

        boolean testFailed = false;

        try {
            Topic topic = jmsContext.createTopic("");

            jmsContext.createProducer().send(topic, "testTopicNameEmptyString_TcpIp");

            jmsConsumer = jmsContext.createConsumer(topic);
            TextMessage m1 = (TextMessage) jmsConsumer.receive(30000);

        } catch ( JMSRuntimeException ex ) {
            testFailed = true;
            ex.printStackTrace();
        }

        if ( jmsConsumer != null ) {
            jmsConsumer.close();
        }
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testTopicNameEmptyString_TcpIp failed: Unexpected JMSRuntimeException was seen");
        }
    }

    public void testTopicNameWildChars_B(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContext = jmsTCFBindings.createContext();

        JMSConsumer jmsConsumer = null;

        boolean testFailed = false;

        try {
            Topic topic = jmsContext.createTopic("ppp*");

            jmsContext.createProducer().send(topic, "testTopicNameWildChars_B");

            jmsConsumer = jmsContext.createConsumer(topic);
            TextMessage m1 = (TextMessage) jmsConsumer.receive(30000);

            testFailed = true;

        } catch ( JMSRuntimeException ex ) {
            // expected
        }

        if ( jmsConsumer != null ) {
            jmsConsumer.close();
        }
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testTopicNameWildChars_B failed: Expected JMSRuntimeException was not seen");
        }
    }

    public void testTopicNameWildChars_TcpIp(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContext = jmsTCFTCP.createContext();

        JMSConsumer jmsConsumer = null;

        boolean testFailed = false;

        try {
            Topic topic = jmsContext.createTopic("ppp*");

            jmsContext.createProducer().send(topic, "testTopicNameWildChars_TcpIp");

            jmsConsumer = jmsContext.createConsumer(topic);
            TextMessage m1 = (TextMessage) jmsConsumer.receive(30000);

            testFailed = true;

        } catch (JMSRuntimeException ex) {
            // Expected
        }

        if ( jmsConsumer != null ) {
            jmsConsumer.close();
        }
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testTopicNameWildChars_TcpIp failed: Expected JMSRuntimeException was not seen");
        }
    }

    public void testTopicNameWithSpaces_B(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContext = jmsTCFBindings.createContext();

        Topic topic = jmsContext.createTopic("New Topic");

        JMSConsumer jmsConsumer = jmsContext.createConsumer(topic);

        String text = "testTopicNameWithSpaces_B";
        jmsContext.createProducer().send(topic, text);

        TextMessage m = (TextMessage) jmsConsumer.receive(30000);

        String failureReason = null;
        if ( m == null ) {
            failureReason = "Null message; expected [ " + text + " ]";
        } else if ( m.getText() == null ) {
            failureReason = "Null message text; exected [ " + text + " ]";
        } else if ( !m.getText().equals(text) ) {
            failureReason = "Received [ " + m.getText() + " ]; expected [ " + text + " ]";
        }

        jmsConsumer.close();
        jmsContext.close();

        if ( failureReason != null ) {
            throw new Exception("testTopicNameWithSpaces_B failed: " + failureReason);
        }
    }

    public void testTopicNameWithSpaces_TcpIp(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContext = jmsTCFTCP.createContext();

        Topic topic = jmsContext.createTopic("New Topic");

        JMSConsumer jmsConsumer = jmsContext.createConsumer(topic);

        String text = "testTopicNameWithSpaces_TcpIp";
        jmsContext.createProducer().send(topic, text);

        TextMessage m = (TextMessage) jmsConsumer.receive(30000);

        String failureReason = null;
        if ( m == null ) {
            failureReason = "Null message; expected [ " + text + " ]";
        } else if ( m.getText() == null ) {
            failureReason = "Null message text; exected [ " + text + " ]";
        } else if ( !m.getText().equals(text) ) {
            failureReason = "Received [ " + m.getText() + " ]; expected [ " + text + " ]";
        }

        jmsConsumer.close();
        jmsContext.close();

        if ( failureReason != null ) {
            throw new Exception("testTopicNameWithSpaces_TcpIp failed: " + failureReason);
        }
    }

    public void testTopicName_temp_B(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContext = jmsTCFBindings.createContext();

        Topic topic = jmsContext.createTopic("_tempTopic");

        JMSConsumer jmsConsumer = jmsContext.createConsumer(topic);

        String text = "testTopicName_temp_B";
        jmsContext.createProducer().send(topic, text);

        TextMessage m = (TextMessage) jmsConsumer.receive(30000);

        String failureReason = null;
        if ( m == null ) {
            failureReason = "Null message; expected [ " + text + " ]";
        } else if ( m.getText() == null ) {
            failureReason = "Null message text; exected [ " + text + " ]";
        } else if ( !m.getText().equals(text) ) {
            failureReason = "Received [ " + m.getText() + " ]; expected [ " + text + " ]";
        }

        jmsConsumer.close();
        jmsContext.close();

        if ( failureReason != null ) {
            throw new Exception("testTopicName_temp_B failed: " + failureReason);
        }
    }

    public void testTopicName_temp_TcpIp(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContext = jmsTCFTCP.createContext();

        Topic topic = jmsContext.createTopic("_tempTopic");

        JMSConsumer jmsConsumer = jmsContext.createConsumer(topic);

        String text = "testTopicName_temp_TcpIp";
        jmsContext.createProducer().send(topic, text);

        TextMessage m = (TextMessage) jmsConsumer.receive(30000);

        String failureReason = null;
        if ( m == null ) {
            failureReason = "Null message; expected [ " + text + " ]";
        } else if ( m.getText() == null ) {
            failureReason = "Null message text; exected [ " + text + " ]";
        } else if ( !m.getText().equals(text) ) {
            failureReason = "Received [ " + m.getText() + " ]; expected [ " + text + " ]";
        }

        jmsConsumer.close();
        jmsContext.close();

        if ( failureReason != null ) {
            throw new Exception("testTopicName_temp_TcpIp failed: " + failureReason);
        }
    }

    public void testTopicNameLong_B(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContext = jmsTCFBindings.createContext();

        Topic topic = jmsContext.createTopic("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb");

        JMSConsumer jmsConsumer = jmsContext.createConsumer(topic);

        String text = "testTopicNameLong_B";
        jmsContext.createProducer().send(topic, text);

        TextMessage m = (TextMessage) jmsConsumer.receive(30000);

        String failureReason = null;
        if ( m == null ) {
            failureReason = "Null message; expected [ " + text + " ]";
        } else if ( m.getText() == null ) {
            failureReason = "Null message text; exected [ " + text + " ]";
        } else if ( !m.getText().equals(text) ) {
            failureReason = "Received [ " + m.getText() + " ]; expected [ " + text + " ]";
        }

        jmsConsumer.close();
        jmsContext.close();

        if ( failureReason != null ) {
            throw new Exception("testTopicNameLong_B failed: " + failureReason);
        }
    }

    public void testTopicNameLong_TcpIp(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContext = jmsTCFTCP.createContext();

        Topic topic = jmsContext.createTopic("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb");

        JMSConsumer jmsConsumer = jmsContext.createConsumer(topic);

        String text = "testTopicNameLong_TcpIp";
        jmsContext.createProducer().send(topic, text);

        TextMessage m = (TextMessage) jmsConsumer.receive(30000);

        String failureReason = null;
        if ( m == null ) {
            failureReason = "Null message; expected [ " + text + " ]";
        } else if ( m.getText() == null ) {
            failureReason = "Null message text; exected [ " + text + " ]";
        } else if ( !m.getText().equals(text) ) {
            failureReason = "Received [ " + m.getText() + " ]; expected [ " + text + " ]";
        }

        jmsConsumer.close();
        jmsContext.close();

        if ( failureReason != null ) {
            throw new Exception("testTopicNameLong_TcpIp failed: " + failureReason);
        }
    }

    public void testTopicNameCaseSensitive_B(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContext = jmsTCFBindings.createContext();

        JMSConsumer jmsConsumer = null;

        boolean testFailed = false;

        try {
            Topic topic = jmsContext.createTopic("NEWTOPIC");

            jmsContext.createProducer().send(topic, "testTopicNameCaseSensitive_B");

            jmsConsumer = jmsContext.createConsumer(topic);
            TextMessage m1 = (TextMessage) jmsConsumer.receive(30000);

        } catch ( JMSRuntimeException ex1 ) {
            testFailed = true;
            ex1.printStackTrace();
        }

        if ( jmsConsumer != null ) {
            jmsConsumer.close();
        }
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testTopicNameCaseSensitive_B failed: Unexpected JMSRuntime Exception was seen");
        }
    }

    public void testTopicNameCaseSensitive_TcpIp(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContext = jmsTCFTCP.createContext();

        JMSConsumer jmsConsumer = null;

        boolean testFailed = false;

        try {
            Topic topic = jmsContext.createTopic("NEWTOPIC");

            jmsContext.createProducer().send(topic, "testTopicNameCaseSensitive_TcpIp");

            jmsConsumer = jmsContext.createConsumer(topic);
            TextMessage m1 = (TextMessage) jmsConsumer.receive(30000);

        } catch ( JMSRuntimeException ex1 ) {
            testFailed = true;
            ex1.printStackTrace();
        }

        if ( jmsConsumer != null ) {
            jmsConsumer.close();
        }
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testTopicNameCaseSensitive_TcpIp failed: Unexpected JMSRuntime Exception was seen");
        }
    }

    public void testTopicNameTOPIC_B(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContext = jmsTCFBindings.createContext();

        Topic topic = jmsContext.createTopic("TOPIC/topic");

        String text = "testTopicNameTOPIC_B";
        jmsContext.createProducer().send(topic, text);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(topic);
        TextMessage m = (TextMessage) jmsConsumer.receive(30000);

        String failureReason = null;
        if ( m == null ) {
            failureReason = "Null message; expected [ " + text + " ]";
        } else if ( m.getText() == null ) {
            failureReason = "Null message text; exected [ " + text + " ]";
        } else if ( !m.getText().equals(text) ) {
            failureReason = "Received [ " + m.getText() + " ]; expected [ " + text + " ]";
        }

        jmsConsumer.close();
        jmsContext.close();

        if ( failureReason != null ) {
            throw new Exception("testTopicNameTOPIC_B failed: " + failureReason);
        }
    }

    public void testTopicNameTOPIC_TcpIp(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContext = jmsTCFTCP.createContext();

        Topic topic = jmsContext.createTopic("TOPIC/topic");

        String text = "testTopicNameTOPIC_TcpIp";
        jmsContext.createProducer().send(topic, text);

        JMSConsumer jmsConsumer = jmsContext.createConsumer(topic);
        TextMessage m = (TextMessage) jmsConsumer.receive(30000);

        String failureReason = null;
        if ( m == null ) {
            failureReason = "Null message; expected [ " + text + " ]";
        } else if ( m.getText() == null ) {
            failureReason = "Null message text; exected [ " + text + " ]";
        } else if ( !m.getText().equals(text) ) {
            failureReason = "Received [ " + m.getText() + " ]; expected [ " + text + " ]";
        }

        jmsConsumer.close();
        jmsContext.close();

        if ( failureReason != null ) {
            throw new Exception("testTopicNameTOPIC_TcpIp failed: " + failureReason);
        }
    }
}
