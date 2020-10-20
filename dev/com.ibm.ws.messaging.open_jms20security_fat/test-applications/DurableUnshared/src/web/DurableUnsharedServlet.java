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

import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.JMSProducer;
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
public class DurableUnsharedServlet extends HttpServlet {

    public static TopicConnectionFactory tcfBindings;
    public static TopicConnectionFactory tcfTCP;

    public static Topic topic;
    public static boolean exceptionFlag;

    @Override
    public void init() throws ServletException { // TODO
        // Auto-generated method stub

        super.init();
        try {

            tcfBindings = getTCFBindings();
            tcfTCP = getTCFTCP();
            topic = getTopic();

        } catch (NamingException e) { // TODO Auto-generated catch block
            e.printStackTrace();
        }

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
        out.println("Starting " + test + "<br>");
        final TraceComponent tc = Tr.register(DurableUnsharedServlet.class); // injection
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

    public void testCreateUnSharedDurableConsumer_create(
                                                         HttpServletRequest request, HttpServletResponse response) throws Throwable {

        try {
            String userName = "user1";
            String password = "user1pwd";

            JMSContext jmsContextSender = tcfBindings.createContext(userName,
                                                                    password);

            JMSContext jmsContextReceiver = tcfBindings.createContext(userName,
                                                                      password);

            JMSConsumer jmsConsumer = jmsContextReceiver.createDurableConsumer(
                                                                               topic, "SUBID");

            JMSProducer jmsProducer = jmsContextSender.createProducer();

            TextMessage tmsg = jmsContextSender.createTextMessage("This is a test message");

            jmsProducer.send(topic, tmsg);

            jmsContextSender.close();

        } catch (Exception ex) {
            ex.printStackTrace();

        }

    }

    public void testCreateUnSharedDurableConsumer_consume(
                                                          HttpServletRequest request, HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        String userName = "user1";
        String password = "user1pwd";

        JMSContext jmsContextReceiver = tcfBindings.createContext(userName,
                                                                  password);
        JMSConsumer jmsConsumer = jmsContextReceiver.createDurableConsumer(
                                                                           topic, "SUBID");

        TextMessage tmsg = (TextMessage) jmsConsumer.receiveNoWait();
        if (!(tmsg != null))
            exceptionFlag = true;

        jmsConsumer.close();

        jmsContextReceiver.unsubscribe("SUBID");

        jmsContextReceiver.close();

        if (exceptionFlag)
            throw new WrongException("testCreateUnSharedDurableConsumer_consume failed");

    }

    public void testCreateUnSharedDurableConsumer_create_TCP(
                                                             HttpServletRequest request, HttpServletResponse response) throws Throwable {

        try {

            String userName = "user1";
            String password = "user1pwd";

            JMSContext jmsContextSender = tcfTCP.createContext(userName,
                                                               password);

            JMSContext jmsContextReceiver = tcfTCP.createContext(userName,
                                                                 password);

            JMSConsumer jmsConsumer = jmsContextReceiver.createDurableConsumer(
                                                                               topic, "SUBID");

            JMSProducer jmsProducer = jmsContextSender.createProducer();

            TextMessage tmsg = jmsContextSender.createTextMessage("This is a test message");

            jmsProducer.send(topic, tmsg);

            jmsContextSender.close();
        } catch (Exception ex) {
            ex.printStackTrace();

        }

    }

    public void testCreateUnSharedDurableConsumer_consume_TCP(
                                                              HttpServletRequest request, HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        String userName = "user1";
        String password = "user1pwd";

        JMSContext jmsContextReceiver = tcfTCP.createContext(userName, password);
        JMSConsumer jmsConsumer = jmsContextReceiver.createDurableConsumer(
                                                                           topic, "SUBID");
        TextMessage tmsg = (TextMessage) jmsConsumer.receiveNoWait();

        if (!(tmsg != null))
            exceptionFlag = true;

        jmsConsumer.close();
        jmsContextReceiver.unsubscribe("SUBID");

        jmsContextReceiver.close();

        if (exceptionFlag)
            throw new WrongException("testCreateUnSharedDurableConsumer_consume_TCP failed");

    }

    public class WrongException extends Exception {
        String str;

        public WrongException(String str) {
            this.str = str;
            System.out.println(" <ERROR> " + str + " </ERROR>");
        }
    }

}
