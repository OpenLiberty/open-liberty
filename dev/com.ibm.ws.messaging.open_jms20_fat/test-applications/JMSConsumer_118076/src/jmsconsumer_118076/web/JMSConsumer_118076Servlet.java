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
package jmsconsumer_118076.web;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.JMSRuntimeException;
import javax.jms.Queue;
import javax.jms.QueueConnectionFactory;
import javax.jms.Session;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicSession;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

@SuppressWarnings("serial")
public class JMSConsumer_118076Servlet extends HttpServlet {

    // JMSConnectionFactory "java:comp/env/jndi_JMS_BASE_CF"
    // JMSQueueConnectionFactory "java:comp/env/jndi_JMS_BASE_QCF"
    // JMSQueueConnectionFactory"java:comp/env/jndi_JMS_BASE_QCF1"
    // JMSTopicConnectionFactory "java:comp/env/eis/tcf"
    //
    // Queue "java:comp/env/jndi_INPUT_Q1"
    // Topic "java:comp/env/eis/topic2"

    private static QueueConnectionFactory jmsQCFBindings;
    private static QueueConnectionFactory jmsQCFTCP;
    private static Queue jmsQueue;
    private static Topic jmsTopic;

    @Override
    public void init() throws ServletException {
        System.out.println("JMSConsumer_118076Servlet.init ENTRY");

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
            jmsQueue = (Queue) new InitialContext().lookup("java:comp/env/jndi_INPUT_Q1");
        } catch ( NamingException e ) {
            e.printStackTrace();
        }
        System.out.println("Queue 'java:comp/env/jndi_INPUT_Q1':\n" + jmsQueue);

        try {
            jmsTopic = (Topic) new InitialContext().lookup("java:comp/env/eis/topic2");
        } catch ( NamingException e ) {
            e.printStackTrace();
        }
        System.out.println("Topic 'java:comp/env/eis/topic2':\n" + jmsTopic);

        System.out.println("JMSConsumer_118076Servlet.init RETURN");

        if ( (jmsQCFBindings == null) || (jmsQCFTCP == null) ||
             (jmsQueue == null) || (jmsTopic == null) ) {
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
        TraceComponent tc = Tr.register(JMSConsumer_118076Servlet.class);

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

    public void testCloseConsumer_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        boolean testFailed = false;

        try {
            JMSContext jmsContext = jmsQCFBindings.createContext();
            JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);
            jmsConsumer.close();
            jmsConsumer.receive();
            testFailed = true;
        } catch ( JMSRuntimeException ex ) {
            ex.printStackTrace();
        }

        if ( testFailed ) {
            throw new Exception("testCloseConsumer_TcpIp_SecOff failed: Expected exception did not occur");
        }
    }

    public void testCloseConsumer_TcpIp_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        boolean testFailed = false;
        try {
            JMSContext jmsContext = jmsQCFTCP.createContext();
            JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);
            jmsConsumer.close();
            jmsConsumer.receive();
            testFailed = true;
        } catch ( JMSRuntimeException ex ) {
            ex.printStackTrace();
        }

        if ( testFailed ) {
            throw new Exception("testCloseConsumer_TcpIp_SecOff failed: Expected exception did not occur");
        }
    }

    public void testCloseClosedConsumer_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContext = jmsQCFBindings.createContext();

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);
        jmsConsumer.close();

        boolean testFailed = false;
        try {
            jmsConsumer.close();
        } catch ( Exception ex ) {
            ex.printStackTrace();
            testFailed = true;
        }

        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testCloseClosedConsumer_B_SecOff failed: Unexpected exception has occured");
        }
    }

    public void testCloseClosedConsumer_TcpIp_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContext = jmsQCFTCP.createContext();

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);
        jmsConsumer.close();

        boolean testFailed = false;

        try {
            jmsConsumer.close();
        } catch ( Exception ex ) {
            ex.printStackTrace();
            testFailed = true;
        }

        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testCloseClosedConsumer_B_SecOff failed: Unexpected exception has occured");
        }
    }

    public void testGetMessageSelector_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContext = jmsQCFBindings.createContext();

        JMSConsumer testConsumer = jmsContext.createConsumer(jmsQueue, "Test");
        String testSelector = testConsumer.getMessageSelector();

        JMSConsumer nullConsumer = jmsContext.createConsumer(jmsQueue, null);
        String nullSelector = nullConsumer.getMessageSelector();

        JMSConsumer emptyConsumer = jmsContext.createConsumer(jmsQueue, "");
        String emptySelector = emptyConsumer.getMessageSelector();

        jmsContext.close();

        if ( (testSelector != "Test") || (nullSelector != null) || (emptySelector != "") ) {
            throw new Exception("testGetMessageSelector_B_SecOff failed : Selector value is incorrect");
        }
    }

    public void testGetMessageSelector_TcpIp_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContext = jmsQCFTCP.createContext();

        JMSConsumer testConsumer = jmsContext.createConsumer(jmsQueue, "Test");
        String testSelector = testConsumer.getMessageSelector();

        JMSConsumer nullConsumer = jmsContext.createConsumer(jmsQueue, null);
        String nullSelector = nullConsumer.getMessageSelector();

        JMSConsumer emptyConsumer = jmsContext.createConsumer(jmsQueue, "");
        String emptySelector = emptyConsumer.getMessageSelector();

        jmsContext.close();

        if ( (testSelector != "Test") || (nullSelector != null) || (emptySelector != "") ) {
            throw new Exception("testGetMessageSelector_TcpIp_SecOff failed : Selector value is incorrect");
        }
    }

    public void testSetMessageListener_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContext = jmsQCFBindings.createContext();
        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue, "Test");

        boolean testFailed = false;
        try {
            jmsConsumer.setMessageListener(null);
            testFailed = true;
        } catch ( JMSRuntimeException ex ) {
            ex.printStackTrace();
        }

        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testSetMessageListener_B_SecOff failed: Expected exception did not occur");
        }
    }

    public void testSetMessageListener_TcpIp_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContext = jmsQCFTCP.createContext();
        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue, "Test");

        boolean testFailed = false;
        try {
            jmsConsumer.setMessageListener(null);
            testFailed = true;
        } catch ( JMSRuntimeException ex ) {
            ex.printStackTrace();
        }

        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testSetMessageListener_TcpIp_SecOff failed: Expected exception did not occur");
        }
    }

    public void testGetMessageListener_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContext = jmsQCFBindings.createContext();
        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue, "Test");

        boolean testFailed = false;
        try {
            jmsConsumer.getMessageListener();
            testFailed = true;
        } catch ( JMSRuntimeException ex ) {
            ex.printStackTrace();
        }

        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testGetMessageListener_B_SecOff failed: Expected exception did not occur");
        }
    }

    public void testGetMessageListener_TcpIp_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContext = jmsQCFTCP.createContext();
        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue, "Test");

        boolean testFailed = false;
        try {
            jmsConsumer.getMessageListener();
            testFailed = true;
        } catch ( JMSRuntimeException ex ) {
            ex.printStackTrace();
        }

        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testGetMessageListener_TcpIp_SecOff failed: Expected exception did not occur");
        }
    }

    public void testSessionClose_IllegalStateException(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        ConnectionFactory basecf = (ConnectionFactory)
            new InitialContext().lookup("java:comp/env/jndi_JMS_BASE_CF");

        Connection tcon = basecf.createConnection();
        tcon.start();

        Session tsession = tcon.createSession(false, javax.jms.Session.AUTO_ACKNOWLEDGE);
        tsession.close();

        boolean testFailed = false;
        try {
            tsession.createDurableConsumer(jmsTopic, "SUBID1");
            testFailed = true;
        } catch ( javax.jms.IllegalStateException ex ) {
            ex.printStackTrace();
        }

        try {
            tsession.createDurableConsumer(jmsTopic, "SUBID2", "", true);
            testFailed = true;
        } catch ( javax.jms.IllegalStateException ex ) {
            ex.printStackTrace();
        }

        try {
            tsession.createDurableSubscriber(jmsTopic, "SUBID3", "", true);
            testFailed = true;
        } catch ( javax.jms.IllegalStateException ex ) {
            ex.printStackTrace();
        }

        try {
            tsession.createDurableSubscriber(jmsTopic, "SUBID4");
            testFailed = true;
        } catch ( javax.jms.IllegalStateException ex ) {
            ex.printStackTrace();
        }

        tcon.close();

        if ( testFailed ) {
            throw new Exception("testSessionClose_IllegalStateException failed: Unexpected exception has occured");
        }
    }

    public void testTopicSession_Qrelated_IllegalStateException(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        TopicConnectionFactory tcf = (TopicConnectionFactory)
            new InitialContext().lookup("java:comp/env/eis/tcf");

        TopicConnection tcon = tcf.createTopicConnection();
        tcon.start();

        TopicSession tsession = tcon.createTopicSession(false, javax.jms.Session.AUTO_ACKNOWLEDGE);

        boolean testFailed = false;
        try {
            tsession.createBrowser(jmsQueue);
            testFailed = true;
        } catch ( javax.jms.IllegalStateException ex ) {
            ex.printStackTrace();
        }

        try {
            tsession.createQueue("TestQ");
            testFailed = true;
        } catch ( javax.jms.IllegalStateException ex ) {
            ex.printStackTrace();
        }

        try {
            tsession.createTemporaryQueue();
            testFailed = true;
        } catch ( javax.jms.IllegalStateException ex ) {
            ex.printStackTrace();
        }

        tcon.close();

        if ( testFailed ) {
            throw new Exception("testTopicSession_Qrelated_IllegalStateException failed: Unexpected exception has occured");
        }
    }
}
