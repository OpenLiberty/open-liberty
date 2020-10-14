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
package jmsdcfvar.web;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.Enumeration;

import javax.jms.ConnectionFactory;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.jms.QueueConnectionFactory;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

@SuppressWarnings("serial")
public class JMSDCFVarServlet extends HttpServlet {

    public void emptyQueue(QueueConnectionFactory qcf, Queue q) throws Exception {
        JMSContext context = qcf.createContext();
        QueueBrowser qb = context.createBrowser(q);
        JMSConsumer consumer = context.createConsumer(q);

        int numMsgs = getMessageCount(qb);

        for ( int msgNo = 0; msgNo < numMsgs; msgNo++ ) {
            Message message = consumer.receive();
        }

        context.close();
    }

    public int getMessageCount(QueueBrowser qb) throws JMSException {
        Enumeration e = qb.getEnumeration();

        int numMsgs = 0;
        while ( e.hasMoreElements() ) {
            e.nextElement();
            numMsgs++;
        }

        return numMsgs;
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
        TraceComponent tc = Tr.register(JMSDCFVarServlet.class);

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

    public void testP2PWithoutLookupName(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean exceptionFlag = false;
        try {
            ConnectionFactory newCF = (ConnectionFactory)
                new InitialContext().lookup("java:comp/CFWithoutLookup");
            exceptionFlag = true;
        } catch ( NameNotFoundException e ) {
            e.printStackTrace();
        }

        if ( exceptionFlag ) {
            throw new Exception("testP2PWithoutLookupName failed: Expected exception was not seen");
        }
    }

    public void testP2PWithLookupName(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean exceptionFlag = false;
        try {
            ConnectionFactory newCF = (ConnectionFactory)
                new InitialContext().lookup("java:comp/CFWithLookup");
            exceptionFlag = true;
        } catch ( NameNotFoundException e ) {
            e.printStackTrace();
        }

        if ( exceptionFlag ) {
            throw new Exception("testP2PWithLookupName failed: Expected message was not received");
        }
    }
}
