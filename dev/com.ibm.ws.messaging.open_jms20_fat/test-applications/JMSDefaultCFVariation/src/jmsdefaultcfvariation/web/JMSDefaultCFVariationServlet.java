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
package jmsdefaultcfvariation.web;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.Enumeration;

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
import javax.naming.NameNotFoundException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

@SuppressWarnings("serial")
public class JMSDefaultCFVariationServlet extends HttpServlet {

    public static boolean exceptionFlag;

    /*
     * @Override
     * public void init() throws ServletException {
     * // TODO Auto-generated method stub
     * 
     * super.init();
     * try {
     * 
     * jmsQueue = getQueue();
     * 
     * } catch (NamingException e) {
     * // TODO Auto-generated catch block
     * e.printStackTrace();
     * }
     * 
     * }
     */

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response) throws ServletException, IOException {
        String test = request.getParameter("test");
        PrintWriter out = response.getWriter();
        out.println("Starting " + test + "<br>");
        final TraceComponent tc = Tr.register(JMSDefaultCFVariationServlet.class); // injection
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

    public void testWithJms20(HttpServletRequest request,
                              HttpServletResponse response) throws Throwable {

        exceptionFlag = false;

        try {
            ConnectionFactory cf = (ConnectionFactory) new InitialContext()
                            .lookup("java:comp/DefaultJMSConnectionFactory");

        } catch (NameNotFoundException e) {
            e.printStackTrace();
            exceptionFlag = true;
        }
        if (!exceptionFlag)
            throw new WrongException("testWithJms20 failed: Expected exception was not received");

    }

    public void testPubSubWithJms20(HttpServletRequest request,
                                    HttpServletResponse response) throws Throwable {

        exceptionFlag = false;

        ConnectionFactory cf = (ConnectionFactory) new InitialContext()
                        .lookup("java:comp/DefaultJMSConnectionFactory");
        Topic jmsTopic = (Topic) new InitialContext()
                        .lookup("java:comp/env/eis/topic1");
        JMSContext jmsContextTopic = cf.createContext();

        TextMessage message = jmsContextTopic
                        .createTextMessage("testPubSubWithJms20");
        JMSConsumer jmsConsumer = jmsContextTopic.createConsumer(jmsTopic);
        jmsContextTopic.createProducer().send(jmsTopic, message);
        TextMessage message1 = (TextMessage) jmsConsumer.receive(500);
        System.out.println("Received message: " + message1.getText());

        if (!(message1 != null && message1.getText().equals("testPubSubWithJms20")))
            exceptionFlag = true;

        jmsContextTopic.close();
        if (exceptionFlag)
            throw new WrongException("testPubSubWithJms20 failed: Expected message was not received");

    }

    public void testP2PWithoutLookupName(HttpServletRequest request,
                                         HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        try {
            ConnectionFactory newCF = (ConnectionFactory) new InitialContext()
                            .lookup("java:comp/CFWithoutLookup");

        } catch (NameNotFoundException e) {
            e.printStackTrace();
            exceptionFlag = true;
        }
        if (!exceptionFlag)
            throw new WrongException("testP2PWithoutLookupName failed: Expected exception was not seen");

    }

    public void testP2PWithLookupName(HttpServletRequest request,
                                      HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        try {
            ConnectionFactory newCF = (ConnectionFactory) new InitialContext()
                            .lookup("java:comp/CFWithLookup");
        } catch (NameNotFoundException e) {
            e.printStackTrace();
            exceptionFlag = true;
        }

        if (!exceptionFlag)
            throw new WrongException("testP2PWithLookupName failed: Expected message was not received");

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
