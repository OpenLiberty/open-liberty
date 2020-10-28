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
    public static boolean exceptionFlag;

    @Override
    public void init() throws ServletException {
        // TODO Auto-generated method stub

        super.init();
        try {
            jmsQCFBindings = getQCF("jndi_JMS_BASE_QCF");
            jmsQCFTCP = getQCF("jndi_JMS_BASE_QCF1");
            jmsQueue = getQueue();

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
        final TraceComponent tc = Tr.register(JMSContext_118070Servlet.class); // injection
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

    public void testCloseAll_B_SecOff(HttpServletRequest request,
                                      HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContext = jmsQCFBindings.createContext();

        try {
            jmsContext.close();
            jmsContext.start();

        } catch (JMSRuntimeException ex) {

            ex.printStackTrace();
            System.out.println("******THE EXCEPTION IN testCloseAll_B_SecOff IS : "
                               + ex.getClass().getName());
            exceptionFlag = true;
        }

        if (!exceptionFlag)
            throw new WrongException("testCloseAll_B_SecOff failed: Expected exception was not seen");

    }

    public void testCloseAll_TcpIp_SecOff(HttpServletRequest request,
                                          HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContext = jmsQCFTCP.createContext();

        try {
            jmsContext.close();
            jmsContext.start();

        } catch (JMSRuntimeException ex) {

            ex.printStackTrace();
            System.out.println("******THE EXCEPTION IN testCloseAll_TcpIp_SecOff IS : "
                               + ex.getClass().getName());
            exceptionFlag = true;
        }

        if (!exceptionFlag)
            throw new WrongException("testCloseAll_TcpIp_SecOff failed: Expected exception was not seen");

    }

    public void testCloseTempDest_B_SecOff(HttpServletRequest request,
                                           HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContext = jmsQCFBindings.createContext();
        TemporaryQueue tempQ = jmsContext.createTemporaryQueue();
        try {
            jmsContext.close();
            jmsContext.createBrowser(tempQ);

        } catch (JMSRuntimeException ex) {
            ex.printStackTrace();
            System.out.println("******THE EXCEPTION IN testCloseTempDest_B_SecOff IS : "
                               + ex.getClass().getName());
            exceptionFlag = true;
        }
        if (!exceptionFlag)
            throw new WrongException("testCloseTempDest_B_SecOff failed: Expected exception was not seen");
    }

    public void testCloseTempDest_TcpIp_SecOff(HttpServletRequest request,
                                               HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContext = jmsQCFTCP.createContext();
        TemporaryQueue tempQ = jmsContext.createTemporaryQueue();
        try {
            jmsContext.close();
            jmsContext.createBrowser(tempQ);

        } catch (JMSRuntimeException ex) {
            ex.printStackTrace();
            System.out.println("******THE EXCEPTION IN testCloseTempDest_TcpIp_SecOff IS : "
                               + ex.getClass().getName());
            exceptionFlag = true;
        }
        if (!exceptionFlag)
            throw new WrongException("testCloseTempDest_TcpIp_SecOff failed: Expected exception was not seen");
    }

    public void testCloseClosedContext_B_SecOff(HttpServletRequest request,
                                                HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContext = jmsQCFBindings.createContext();
        jmsContext.close();
        try {
            jmsContext.close();
        } catch (Exception ex) {
            ex.printStackTrace();
            System.out.println("Unexpected Exception Occurred");
            exceptionFlag = true;
        }

        if (exceptionFlag)
            throw new WrongException("testCloseClosedContext_B_SecOff failed: Unexpected exception was seen");
    }

    public void testCloseClosedContext_TcpIp_SecOff(HttpServletRequest request,
                                                    HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContext = jmsQCFTCP.createContext();
        jmsContext.close();
        try {
            jmsContext.close();
        } catch (Exception ex) {
            ex.printStackTrace();
            System.out.println("Unexpected Exception Occurred");
            exceptionFlag = true;
        }

        if (exceptionFlag)
            throw new WrongException("testCloseClosedContext_TcpIp_SecOff failed: Unexpected exception was seen");
    }

    public void testAckOnClosedContext_B_SecOff(HttpServletRequest request,
                                                HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContext = jmsQCFBindings.createContext();
        JMSProducer jmsProducer = jmsContext.createProducer();

        jmsProducer.send(jmsQueue, "testAckOnClosedContext_B_SecOff");
        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);

        TextMessage msg = (TextMessage) jmsConsumer.receive(DEFAULT_TIMEOUT);
        jmsContext.close();
        try {
            jmsContext.acknowledge();

        } catch (IllegalStateRuntimeException ex) {

            ex.printStackTrace();
            System.out.println("******THE EXCEPTION IN testAckOnClosedContext_B_SecOff IS : "
                               + ex.getClass().getName());
            exceptionFlag = true;
        }

        if (!exceptionFlag)
            throw new WrongException("testAckOnClosedContext_B_SecOff failed: Expected exception was not seen");
    }

    public void testAckOnClosedContext_TcpIp_SecOff(HttpServletRequest request,
                                                    HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContext = jmsQCFTCP.createContext();
        JMSProducer jmsProducer = jmsContext.createProducer();

        jmsProducer.send(jmsQueue, "testAckOnClosedContext_TcpIp_SecOff");
        JMSConsumer jmsConsumer = jmsContext.createConsumer(jmsQueue);

        TextMessage msg = (TextMessage) jmsConsumer.receive(DEFAULT_TIMEOUT);
        jmsContext.close();
        try {
            jmsContext.acknowledge();

        } catch (IllegalStateRuntimeException ex) {

            ex.printStackTrace();
            System.out.println("******THE EXCEPTION IN testAckOnClosedContext_TcpIp_SecOff IS : "
                               + ex.getClass().getName());
            exceptionFlag = true;
        }

        if (!exceptionFlag)
            throw new WrongException("testAckOnClosedContext_TcpIp_SecOff failed: Expected exception was not seen");

    }

    public QueueConnectionFactory getQCF(String name) throws NamingException {

        QueueConnectionFactory cf1 = (QueueConnectionFactory) new InitialContext()
                        .lookup("java:comp/env/" + name);

        return cf1;

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

}
