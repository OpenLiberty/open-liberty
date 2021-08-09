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
package jmscontext_118070.web;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;

import javax.jms.IllegalStateRuntimeException;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.JMSProducer;
import javax.jms.JMSRuntimeException;
import javax.jms.Queue;
import javax.jms.QueueConnectionFactory;
import javax.jms.TemporaryQueue;
import javax.jms.TextMessage;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

@SuppressWarnings("serial")
public class JMSContext_118070Servlet extends HttpServlet {

    public static QueueConnectionFactory jmsQCFBindings;
    public static QueueConnectionFactory jmsQCFTCP;

    public static Queue jmsQueue;

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

    @Override
    public void init() throws ServletException {
        super.init(); // throws ServletException

        jmsQCFBindings = getQCF("java:comp/env/jndi_JMS_BASE_QCF");
        jmsQCFTCP = getQCF("java:comp/env/jndi_JMS_BASE_QCF1");
        jmsQueue = getQueue("java:comp/env/jndi_INPUT_Q");

        if ( jmsQCFBindings == null ) {
            throw new ServletException("Null 'jmsQCFBindings'");
        }
        if ( jmsQCFTCP == null ) {
            throw new ServletException("Null 'jmsQCFTCP'");
        }
        if ( jmsQueue == null ) {
            throw new ServletException("Null 'queue'");
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
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {

        String test = request.getParameter("test");

        PrintWriter out = response.getWriter();
        out.println("Starting " + test + "<br>");

        // The injection engine doesn't like this at the class level.
        TraceComponent tc = Tr.register(JMSContext_118070Servlet.class); // injection

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

    public void testCloseAll_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContext = jmsQCFBindings.createContext();

        boolean testFailed = false;
        try {
            jmsContext.close();
            jmsContext.start();
            testFailed = true;
        } catch ( JMSRuntimeException ex ) {
            // Expected
        }

        if ( testFailed ) {
            throw new Exception("testCloseAll_B_SecOff failed: Expected exception was not seen");
        }
    }

    public void testCloseAll_TcpIp_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContext = jmsQCFTCP.createContext();

        boolean testFailed = false;
        try {
            jmsContext.close();
            jmsContext.start();
            testFailed = true;
        } catch ( JMSRuntimeException ex ) {
            // Expected
        }

        if ( testFailed ) {
            throw new Exception("testCloseAll_TcpIp_SecOff failed: Expected exception was not seen");
        }
    }

    public void testCloseTempDest_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContext = jmsQCFBindings.createContext();
        TemporaryQueue tempQ = jmsContext.createTemporaryQueue();

        boolean testFailed = false;
        try {
            jmsContext.close();
            jmsContext.createBrowser(tempQ);
            testFailed = true;
        } catch ( JMSRuntimeException ex ) {
            // Expected
        }

        if ( testFailed ) {
            throw new Exception("testCloseTempDest_B_SecOff failed: Expected exception was not seen");
        }
    }

    public void testCloseTempDest_TcpIp_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContext = jmsQCFTCP.createContext();
        TemporaryQueue tempQ = jmsContext.createTemporaryQueue();

        boolean testFailed = false;
        try {
            jmsContext.close();
            jmsContext.createBrowser(tempQ);
            testFailed = true;
        } catch ( JMSRuntimeException ex ) {
            // Expected
        }

        if ( testFailed ) {
            throw new Exception("testCloseTempDest_TcpIp_SecOff failed: Expected exception was not seen");
        }
    }

    public void testCloseClosedContext_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContext = jmsQCFBindings.createContext();
        jmsContext.close();

        boolean testFailed = false;
        try {
            jmsContext.close();
        } catch ( Exception ex ) {
            ex.printStackTrace();
            testFailed = true;
        }

        if ( testFailed ) {
            throw new Exception("testCloseClosedContext_B_SecOff failed: Unexpected exception was seen");
        }
    }

    public void testCloseClosedContext_TcpIp_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContext = jmsQCFTCP.createContext();
        jmsContext.close();

        boolean testFailed = false;
        try {
            jmsContext.close();
        } catch ( Exception ex ) {
            ex.printStackTrace();
            testFailed = true;
        }

        if ( testFailed ) {
            throw new Exception("testCloseClosedContext_TcpIp_SecOff failed: Unexpected exception was seen");
        }
    }

    public void testAckOnClosedContext_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContext = jmsQCFBindings.createContext();

        JMSProducer jmsProducer = jmsContext.createProducer();
        jmsProducer.send(jmsQueue, "testAckOnClosedContext_B_SecOff");

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);
        TextMessage msg = (TextMessage) jmsConsumer.receive(DEFAULT_TIMEOUT);

        jmsContext.close();

        boolean testFailed = false;
        try {
            jmsContext.acknowledge();
            testFailed = true;
        } catch ( IllegalStateRuntimeException ex ) {
            // Expected
        }

        if ( testFailed ) {
            throw new Exception("testAckOnClosedContext_B_SecOff failed: Expected exception was not seen");
        }
    }

    public void testAckOnClosedContext_TcpIp_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContext = jmsQCFTCP.createContext();

        JMSProducer jmsProducer = jmsContext.createProducer();
        jmsProducer.send(jmsQueue, "testAckOnClosedContext_TcpIp_SecOff");

        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);
        TextMessage msg = (TextMessage) jmsConsumer.receive(DEFAULT_TIMEOUT);

        jmsContext.close();

        boolean testFailed = false;
        try {
            jmsContext.acknowledge();
            testFailed = true;
        } catch ( IllegalStateRuntimeException ex ) {
            // Expected
        }

        if ( testFailed ) {
            throw new Exception("testAckOnClosedContext_TcpIp_SecOff failed: Expected exception was not seen");
        }
    }
}
