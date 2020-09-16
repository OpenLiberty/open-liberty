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
package durableunshared.web;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.lang.reflect.InvocationTargetException;

import javax.jms.InvalidDestinationRuntimeException;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.JMSProducer;
import javax.jms.JMSRuntimeException;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.jms.TopicConnectionFactory;
import javax.management.InstanceNotFoundException;
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
public class DurableUnsharedServlet extends HttpServlet {

    public static TopicConnectionFactory tcfBindings;
    public static TopicConnectionFactory tcfTCP;

    public static Topic topic;
    public static Topic topic1;
    public static boolean exceptionFlag;
    private static int DEFAULT_TIMEOUT = 30000;

    @Override
    public void init() throws ServletException { // TODO
        // Auto-generated method stub

        super.init();
        try {

            tcfBindings = getTCFBindings();
            tcfTCP = getTCFTCP();
            topic = getTopic();
            topic1 = getTopic1();

        } catch (NamingException e) { // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    public static TopicConnectionFactory getTCFBindings()
                    throws NamingException {

        TopicConnectionFactory tcf1 = (TopicConnectionFactory) new InitialContext()
                        .lookup("java:comp/env/eis/tcf");

        return tcf1;

    }

    public TopicConnectionFactory getTCFTCP() throws NamingException {

        TopicConnectionFactory tcf1 = (TopicConnectionFactory) new InitialContext()
                        .lookup("java:comp/env/eis/tcf1");

        return tcf1;

    }

    public Topic getTopic() throws NamingException {

        Topic topic = (Topic) new InitialContext().lookup("eis/topic1");

        return topic;
    }

    public Topic getTopic1() throws NamingException {

        Topic topic = (Topic) new InitialContext().lookup("eis/topic2");

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
            System.out.println(" Error: " + test);
            x.printStackTrace(out);
            out.println("</pre>");
        }
    }

    public void testCreateUnSharedDurableConsumer_create(
                                                         HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        JMSContext jmsContextSender = tcfBindings.createContext();

        JMSContext jmsContextReceiver = tcfBindings.createContext();

        JMSConsumer jmsConsumer = jmsContextReceiver
                        .createDurableConsumer(topic, "SUBID");

        JMSProducer jmsProducer = jmsContextSender.createProducer();

        TextMessage tmsg = jmsContextSender
                        .createTextMessage("This is a test message");

        jmsProducer.send(topic, tmsg);

        jmsConsumer.close();

        jmsContextReceiver.close();

        jmsContextSender.close();

    }

    public void testCreateUnSharedDurableConsumer_consume(
                                                          HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContextReceiver = tcfBindings.createContext();
        JMSConsumer jmsConsumer = jmsContextReceiver
                        .createDurableConsumer(topic, "SUBID");

        TextMessage tmsg = (TextMessage) jmsConsumer.receive(DEFAULT_TIMEOUT);

        if (!(tmsg != null))
            exceptionFlag = true;

        jmsConsumer.close();

        jmsContextReceiver.unsubscribe("SUBID");

        jmsContextReceiver.close();

        if (exceptionFlag == true)
            throw new WrongException(
                            "testCreateUnSharedDurableConsumer_consume failed");

    }

    public void testCreateUnSharedDurableConsumer_create_TCP(
                                                             HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        JMSContext jmsContextSender = tcfTCP.createContext();

        JMSContext jmsContextReceiver = tcfTCP.createContext();

        JMSConsumer jmsConsumer = jmsContextReceiver
                        .createDurableConsumer(topic, "SUBID");

        JMSProducer jmsProducer = jmsContextSender.createProducer();

        TextMessage tmsg = jmsContextSender
                        .createTextMessage("This is a test message");

        jmsProducer.send(topic, tmsg);

        jmsContextSender.close();

    }

    public void testCreateUnSharedDurableConsumer_consume_TCP(
                                                              HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContextReceiver = tcfTCP.createContext();
        JMSConsumer jmsConsumer = jmsContextReceiver
                        .createDurableConsumer(topic, "SUBID");

        TextMessage tmsg = (TextMessage) jmsConsumer.receive(DEFAULT_TIMEOUT);

        if (!(tmsg != null))
            exceptionFlag = true;

        jmsConsumer.close();
        jmsContextReceiver.unsubscribe("SUBID");

        jmsContextReceiver.close();

        if (exceptionFlag == true)
            throw new WrongException(
                            "testCreateUnSharedDurableConsumer_consume_TCP failed");

    }

    public void testCreateUnSharedDurableConsumer_create_Expiry(
                                                                HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        JMSContext jmsContextSender = tcfBindings.createContext();
        JMSContext jmsContextReceiver = tcfBindings.createContext();

        JMSConsumer jmsConsumer = jmsContextReceiver.createDurableConsumer(topic1, "Exp");

        JMSProducer jmsProducer = jmsContextSender.createProducer();

        TextMessage tmsg = jmsContextSender
                        .createTextMessage("This is a test message");

        jmsProducer.send(topic1, tmsg);
        jmsConsumer.close();

        jmsContextSender.close();
        jmsContextReceiver.close();

    }

    public void testCreateUnSharedDurableConsumer_consume_Expiry(
                                                                 HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        exceptionFlag = false;

        JMSContext jmsContextReceiver = tcfBindings.createContext();
        JMSConsumer jmsConsumer = jmsContextReceiver
                        .createDurableConsumer(topic1, "Exp");

        TextMessage tmsg = (TextMessage) jmsConsumer.receive(DEFAULT_TIMEOUT);

        if (tmsg != null)
            exceptionFlag = true;

        jmsConsumer.close();

        jmsContextReceiver.unsubscribe("Exp");
        jmsContextReceiver.close();

        if (exceptionFlag == true)
            throw new WrongException(
                            "testCreateUnSharedDurableConsumer_consume failed");

    }

    public void testCreateUnSharedDurableConsumer_create_Expiry_TCP(
                                                                    HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        JMSContext jmsContextSender = tcfTCP.createContext();

        JMSContext jmsContextReceiver = tcfTCP.createContext();

        JMSConsumer jmsConsumer = jmsContextReceiver
                        .createDurableConsumer(topic1, "Exp");

        JMSProducer jmsProducer = jmsContextSender.createProducer();
        TextMessage tmsg = jmsContextSender
                        .createTextMessage("This is a test message");

        jmsProducer.send(topic, tmsg);
        jmsContextReceiver.close();
        jmsContextSender.close();

    }

    public void testCreateUnSharedDurableConsumer_consume_Expiry_TCP(
                                                                     HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContextReceiver = tcfTCP.createContext();
        JMSConsumer jmsConsumer = jmsContextReceiver
                        .createDurableConsumer(topic1, "Exp");

        TextMessage tmsg = (TextMessage) jmsConsumer.receive(DEFAULT_TIMEOUT);

        if (tmsg != null)
            exceptionFlag = true;

        jmsConsumer.close();
        jmsContextReceiver.unsubscribe("Exp");

        jmsContextReceiver.close();

        if (exceptionFlag == true)
            throw new WrongException(
                            "testCreateUnSharedDurableConsumer_consume_Expiry_TCP failed");

    }

    // A durable subscription will continue to accumulate messages
    // until it is deleted using the unsubscribe method.

    public void testCreateSharedDurableConsumer_unsubscribe(
                                                            HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContext = tcfBindings.createContext();

        JMSConsumer jmsConsumer = jmsContext.createDurableConsumer(
                                                                   topic, "DURATEST");

        JMSProducer jmsProducer = jmsContext.createProducer();

        TextMessage tmsg = jmsContext
                        .createTextMessage("This is a test message");

        jmsProducer.send(topic, tmsg);

        tmsg = (TextMessage) jmsConsumer.receive(DEFAULT_TIMEOUT);

        jmsConsumer.close();

        jmsContext.unsubscribe("DURATEST");

        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();

        final ObjectName name = new ObjectName(
                        "WebSphere:feature=wasJmsServer,type=Subscriber,name=clientID##DURATEST");

        try {
            String obn = (String) mbs.getAttribute(name, "Id");

        } catch (InstanceNotFoundException ex) {
            ex.printStackTrace();
            exceptionFlag = true;

        }
        jmsContext.close();
        if (exceptionFlag)
            throw new WrongException(
                            "testCreateSharedDurableConsumer_unsubscribe failed");

    }

    public void testCreateSharedDurableConsumer_unsubscribe_TCP(
                                                                HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {
        exceptionFlag = false;

        JMSContext jmsContext = tcfTCP.createContext();

        JMSConsumer jmsConsumer = jmsContext.createDurableConsumer(topic, "DURATEST");

        JMSProducer jmsProducer = jmsContext.createProducer();

        TextMessage tmsg = jmsContext
                        .createTextMessage("This is a test message");

        jmsProducer.send(topic, tmsg);

        tmsg = (TextMessage) jmsConsumer.receive(DEFAULT_TIMEOUT);

        jmsConsumer.close();

        jmsContext.unsubscribe("DURATEST");

        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();

        final ObjectName name = new ObjectName(
                        "WebSphere:feature=wasJmsServer,type=Subscriber,name=clientID##DURATEST");

        try {
            String obn = (String) mbs.getAttribute(name, "Id");
        } catch (InstanceNotFoundException ex) {
            ex.printStackTrace();
            exceptionFlag = true;
        }
        jmsContext.close();
        if (exceptionFlag)
            throw new WrongException(
                            "testCreateSharedDurableConsumer_unsubscribe_TCP failed");

    }

    //  Any durable subscription created using this method will be
    // shared. This means that multiple active (i.e. not closed) consumers on
    // the subscription may exist at the same time. The term "consumer" here
    // means a JMSConsumer object in any client.
    // 129623_1_6 A shared durable subscription is identified by a name
    // specified by the client and by the client identifier (which may be
    // unset). An application which subsequently wishes to create a consumer on
    // that shared durable subscription must use the same client identifier.

/*
 * public void testBasicMDBTopic(HttpServletRequest request,
 * HttpServletResponse response) throws Throwable {
 * 
 * int msgs = 10;
 * 
 * JMSContext jmsCont = tcfBindings.createContext();
 * JMSProducer publisher = jmsCont.createProducer();
 * 
 * for (int i = 0; i < msgs; i++) {
 * publisher.send(topic, "testBasicMDBTopic:" + i);
 * }
 * 
 * System.out.println("Published  messages ");
 * 
 * }
 * 
 * public void testBasicMDBTopic_TCP(HttpServletRequest request,
 * HttpServletResponse response) throws Throwable {
 * 
 * int msgs = 10;
 * 
 * JMSContext jmsCont = tcfTCP.createContext();
 * JMSProducer publisher = jmsCont.createProducer();
 * 
 * for (int i = 0; i < msgs; i++) {
 * publisher.send(topic, "testBasicMDBTopic:" + i);
 * }
 * 
 * System.out.println("Published  messages ");
 * 
 * }
 */

    // If a shared durable subscription already exists with the same
    // name and client identifier (if set), and the same topic and message
    // selector has been specified, then this method creates a JMSConsumer on
    // the existing shared durable subscription.

    public void testCreateSharedDurableConsumer_2Subscribers(
                                                             HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContext = tcfBindings.createContext();

        JMSConsumer jmsConsumer = jmsContext.createDurableConsumer(topic,
                                                                   "DURATEST");

        JMSProducer jmsProducer = jmsContext.createProducer();

        TextMessage tmsg = jmsContext
                        .createTextMessage("This is a test message");

        jmsProducer.send(topic, tmsg);

        tmsg = (TextMessage) jmsConsumer.receive(DEFAULT_TIMEOUT);

        jmsConsumer.close();

        JMSConsumer jmsConsumer2 = jmsContext.createDurableConsumer(
                                                                    topic, "DURATEST");

        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();

        final ObjectName name = new ObjectName(
                        "WebSphere:feature=wasJmsServer,type=Topic,name=NewTopic1");

        javax.management.openmbean.CompositeData[] obn = (CompositeData[]) mbs
                        .invoke(name, "listSubscriptions", null, null);

        if (obn.length == 1)
            exceptionFlag = true;

        jmsConsumer2.close();
        jmsContext.unsubscribe("DURATEST");
        jmsContext.close();

        if (exceptionFlag)
            throw new WrongException("testCreateSharedDurableConsumer_2Subscribers failed");

    }

    public void testCreateSharedDurableConsumer_2Subscribers_TCP(
                                                                 HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        exceptionFlag = false;

        JMSContext jmsContext = tcfTCP.createContext();

        JMSConsumer jmsConsumer = jmsContext.createDurableConsumer(topic,
                                                                   "DURATEST");

        JMSProducer jmsProducer = jmsContext.createProducer();

        TextMessage tmsg = jmsContext
                        .createTextMessage("This is a test message");

        jmsProducer.send(topic, tmsg);

        tmsg = (TextMessage) jmsConsumer.receive(DEFAULT_TIMEOUT);

        jmsConsumer.close();

        JMSConsumer jmsConsumer2 = jmsContext.createDurableConsumer(topic, "DURATEST");
        jmsProducer.send(topic, tmsg);
        tmsg = (TextMessage) jmsConsumer2.receive(DEFAULT_TIMEOUT);

        if (!(tmsg != null))
            exceptionFlag = true;

        jmsConsumer2.close();
        jmsContext.unsubscribe("DURATEST");

        jmsContext.close();

        if (exceptionFlag)
            throw new WrongException("testCreateSharedDurableConsumer_2Subscribers_TCP failed");

    }

    // If a shared durable subscription already exists with the same
    // name and client identifier (if set) but a different topic has been
    // specified, and there is no consumer already active (i.e. not closed) on
    // the durable subscription then this is equivalent to unsubscribing
    // (deleting) the old one and creating a new one.

    public void testCreateSharedDurableConsumer_2SubscribersDiffTopic(
                                                                      HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContext = tcfBindings.createContext();

        JMSConsumer jmsConsumer = jmsContext.createDurableConsumer(topic, "DURATEST123");

        JMSProducer jmsProducer = jmsContext.createProducer();

        TextMessage tmsg = jmsContext
                        .createTextMessage("This is a test message");

        jmsProducer.send(topic, tmsg);

        tmsg = (TextMessage) jmsConsumer.receive(DEFAULT_TIMEOUT);

        jmsConsumer.close();
        jmsContext.close();
        JMSConsumer jmsConsumer2 = jmsContext.createDurableConsumer(topic1, "DURATEST123");

        jmsProducer.send(topic1, tmsg);
        tmsg = (TextMessage) jmsConsumer2.receive(DEFAULT_TIMEOUT);

        System.out.println("------------------" + tmsg);

        if (tmsg != null)
            exceptionFlag = true;

        jmsConsumer2.close();
        jmsContext.unsubscribe("DURATEST123");

        jmsContext.close();

        if (exceptionFlag)
            throw new WrongException("testCreateSharedDurableConsumer_2SubscribersDiffTopic failed");

    }

    public void testCreateSharedDurableConsumer_2SubscribersDiffTopic_TCP(
                                                                          HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        exceptionFlag = false;

        JMSContext jmsContext = tcfTCP.createContext();

        JMSConsumer jmsConsumer = jmsContext.createDurableConsumer(topic, "DURATEST");

        JMSProducer jmsProducer = jmsContext.createProducer();

        TextMessage tmsg = jmsContext
                        .createTextMessage("This is a test message");
        jmsProducer.send(topic, tmsg);

        tmsg = (TextMessage) jmsConsumer.receive(DEFAULT_TIMEOUT);

        jmsConsumer.close();
        jmsContext.close();
        JMSConsumer jmsConsumer2 = jmsContext.createDurableConsumer(topic1, "DURATEST");

        jmsProducer.send(topic1, tmsg);
        tmsg = (TextMessage) jmsConsumer2.receive(DEFAULT_TIMEOUT);
        System.out.println("------------------" + tmsg);

        if (!(tmsg != null))
            exceptionFlag = true;

        jmsConsumer2.close();
        jmsContext.unsubscribe("DURATEST");

        jmsContext.close();

        if (exceptionFlag)
            throw new WrongException("testCreateSharedDurableConsumer_2SubscribersDiffTopic_TCP failed");

    }

    // If a shared durable subscription already exists with the same
    // name and client identifier (if set) but a different topic has been
    // specified, and there is a consumer already active (i.e. not closed) on
    // the durable subscription, then a JMSRuntimeException will be thrown.

    public void testCreateSharedDurableConsumer_JRException(
                                                            HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        exceptionFlag = false;

        JMSContext jmsContext = tcfBindings.createContext();

        JMSConsumer jmsConsumer = jmsContext.createDurableConsumer(topic, "DURATEST1");

        JMSProducer jmsProducer = jmsContext.createProducer();

        TextMessage tmsg = jmsContext
                        .createTextMessage("This is a test message");

        jmsProducer.send(topic, tmsg);

        tmsg = (TextMessage) jmsConsumer.receive(DEFAULT_TIMEOUT);

        try {
            JMSConsumer jmsConsumer1 = jmsContext
                            .createDurableConsumer(topic1, "DURATEST1");
        } catch (JMSRuntimeException ex) {
            ex.printStackTrace();
            exceptionFlag = true;

        }

        jmsConsumer.close();
        jmsContext.unsubscribe("DURATEST1");

        jmsContext.close();

        if (!(exceptionFlag))
            throw new WrongException("testCreateSharedDurableConsumer_JRException failed");

    }

    public void testCreateSharedDurableConsumer_JRException_TCP(
                                                                HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        exceptionFlag = false;

        JMSContext jmsContext = tcfTCP.createContext();

        JMSConsumer jmsConsumer = jmsContext.createDurableConsumer(
                                                                   topic, "DURATEST1");

        JMSProducer jmsProducer = jmsContext.createProducer();

        TextMessage tmsg = jmsContext
                        .createTextMessage("This is a test message");

        jmsProducer.send(topic, tmsg);

        tmsg = (TextMessage) jmsConsumer.receive(DEFAULT_TIMEOUT);

        try {
            JMSConsumer jmsConsumer1 = jmsContext
                            .createDurableConsumer(topic1, "DURATEST1");
        } catch (JMSRuntimeException ex) {
            ex.printStackTrace();
            exceptionFlag = true;

        }

        jmsConsumer.close();
        jmsContext.unsubscribe("DURATEST1");

        jmsContext.close();

        if (!(exceptionFlag))
            throw new WrongException("testCreateSharedDurableConsumer_JRException_TCP failed");

    }

    // A shared durable subscription and an unshared durable
    // subscription may not have the same name and client identifier (if set).
    // If an unshared durable subscription already exists with the same name and
    // client identifier (if set) then a JMSRuntimeException is thrown.

    public void testCreateSharedDurableUndurableConsumer_JRException(
                                                                     HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContext = tcfBindings.createContext();

        JMSConsumer jmsConsumer = jmsContext.createDurableConsumer(topic,
                                                                   "DURATEST456");

        JMSProducer jmsProducer = jmsContext.createProducer();

        TextMessage tmsg = jmsContext
                        .createTextMessage("This is a test message");

        jmsProducer.send(topic, tmsg);

        tmsg = (TextMessage) jmsConsumer.receive(DEFAULT_TIMEOUT);

        JMSConsumer jmsConsumer1 = null;

        try {

            jmsConsumer1 = jmsContext
                            .createSharedDurableConsumer(topic1, "DURATEST456");
        } catch (JMSRuntimeException ex) {
            ex.printStackTrace();
            exceptionFlag = true;

        }

        jmsConsumer.close();
        jmsContext.unsubscribe("DURATEST456");

        jmsContext.close();

        if (!(exceptionFlag))
            throw new WrongException("testCreateSharedDurableUndurableConsumer_JRException failed");
    }

    public void testCreateSharedDurableUndurableConsumer_JRException_TCP(
                                                                         HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContext = tcfTCP.createContext();

        JMSConsumer jmsConsumer = jmsContext.createDurableConsumer(topic,
                                                                   "DURATEST456");

        JMSProducer jmsProducer = jmsContext.createProducer();

        TextMessage tmsg = jmsContext
                        .createTextMessage("This is a test message");

        jmsProducer.send(topic, tmsg);

        tmsg = (TextMessage) jmsConsumer.receive(DEFAULT_TIMEOUT);

        JMSConsumer jmsConsumer1 = null;
        try {

            jmsConsumer1 = jmsContext
                            .createSharedDurableConsumer(topic1, "DURATEST456");
        } catch (JMSRuntimeException ex) {
            ex.printStackTrace();
            exceptionFlag = true;

        }

        jmsConsumer.close();
        jmsContext.unsubscribe("DURATEST456");

        jmsContext.close();
        if (!(exceptionFlag))
            throw new WrongException("testCreateSharedDurableUndurableConsumer_JRException failed");

    }

    // InvalidDestinationRuntimeException - if an invalid topic is
    // specified.

    public void testCreateSharedDurableConsumer_InvalidDestination(
                                                                   HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        exceptionFlag = false;

        JMSContext jmsContext = tcfBindings.createContext();
        try {

            JMSConsumer jmsConsumer = jmsContext
                            .createDurableConsumer(null, "DURATEST1");

        } catch (InvalidDestinationRuntimeException ex) {
            ex.printStackTrace();
            exceptionFlag = true;

        }
        jmsContext.close();
        if (!(exceptionFlag))
            throw new WrongException("testCreateSharedDurableConsumer_InvalidDestination failed");

    }

    public void testCreateSharedDurableConsumer_InvalidDestination_TCP(
                                                                       HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContext = tcfTCP.createContext();

        try {

            JMSConsumer jmsConsumer = jmsContext
                            .createDurableConsumer(null, "DURATEST1");

        } catch (InvalidDestinationRuntimeException ex) {
            ex.printStackTrace();
            exceptionFlag = true;
        }
        jmsContext.close();
        if (!(exceptionFlag))
            throw new WrongException("testCreateSharedDurableConsumer_InvalidDestination_TCP failed");

    }

    //  Case where name is null and empty string

    public void testCreateSharedDurableConsumer_Null(
                                                     HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        exceptionFlag = false;
        boolean val1 = false;
        boolean val2 = false;
        JMSContext jmsContext = tcfBindings.createContext();

        try {

            JMSConsumer jmsConsumer = jmsContext
                            .createDurableConsumer(topic, null);

        } catch (JMSRuntimeException ex) {
            ex.printStackTrace();
            val1 = true;
        }

        try {
            JMSConsumer jmsConsumer = jmsContext
                            .createDurableConsumer(topic, "");

        } catch (JMSRuntimeException ex) {
            ex.printStackTrace();
            val2 = true;
        }

        if (!(val1 == true && val2 == true))
            exceptionFlag = true;

        jmsContext.close();
        if (exceptionFlag)
            throw new WrongException("testCreateSharedDurableConsumer_Null failed");

    }

    public void testCreateSharedDurableConsumer_Null_TCP(
                                                         HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        exceptionFlag = false;
        boolean val1 = false;
        boolean val2 = false;

        JMSContext jmsContext = tcfTCP.createContext();

        try {

            JMSConsumer jmsConsumer = jmsContext
                            .createDurableConsumer(topic, null);

        } catch (JMSRuntimeException ex) {
            ex.printStackTrace();
            val1 = true;
        }

        try {
            JMSConsumer jmsConsumer = jmsContext
                            .createDurableConsumer(topic, "");

        } catch (JMSRuntimeException ex) {
            ex.printStackTrace();
            val2 = true;
        }

        if (!(val1 == true && val2 == true))
            exceptionFlag = true;

        jmsContext.close();

        if (exceptionFlag)
            throw new WrongException("testCreateSharedDurableConsumer_Null_TCP failed");

    }

// =========================================================== test cases with selectors  start ================================================================================

    public void testCreateUnSharedDurableConsumer_Sel_create(
                                                             HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        JMSContext jmsContextSender = tcfBindings.createContext();

        JMSContext jmsContextReceiver = tcfBindings.createContext();

        JMSConsumer jmsConsumer = jmsContextReceiver
                        .createDurableConsumer(topic, "SUBID", "Company = 'IBM'", true);

        JMSProducer jmsProducer = jmsContextSender.createProducer();

        TextMessage tmsg = jmsContextSender
                        .createTextMessage("This is a test message");
        tmsg.setStringProperty("Company", "IBM");
        jmsProducer.send(topic, tmsg);

        jmsContextSender.close();

    }

    public void testCreateUnSharedDurableConsumer_Sel_consume(
                                                              HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContextReceiver = tcfBindings.createContext();
        JMSConsumer jmsConsumer = jmsContextReceiver
                        .createDurableConsumer(topic, "SUBID", "Company = 'IBM'", true);

        TextMessage tmsg = (TextMessage) jmsConsumer.receive(DEFAULT_TIMEOUT);

        if (tmsg == null)
            exceptionFlag = true;

        jmsConsumer.close();

        jmsContextReceiver.unsubscribe("SUBID");

        jmsContextReceiver.close();

        if (exceptionFlag == true)
            throw new WrongException(
                            "testCreateUnSharedDurableConsumer_Sel_consume failed");

    }

    public void testCreateUnSharedDurableConsumer_Sel_create_TCP(
                                                                 HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        JMSContext jmsContextSender = tcfTCP.createContext();

        JMSContext jmsContextReceiver = tcfTCP.createContext();

        JMSConsumer jmsConsumer = jmsContextReceiver
                        .createDurableConsumer(topic, "SUBID", "Company = 'IBM'", true);

        JMSProducer jmsProducer = jmsContextSender.createProducer();

        TextMessage tmsg = jmsContextSender
                        .createTextMessage("This is a test message");
        tmsg.setStringProperty("Company", "IBM");

        jmsProducer.send(topic, tmsg);

        jmsContextSender.close();

    }

    public void testCreateUnSharedDurableConsumer_Sel_consume_TCP(
                                                                  HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContextReceiver = tcfTCP.createContext();
        JMSConsumer jmsConsumer = jmsContextReceiver
                        .createDurableConsumer(topic, "SUBID", "Company = 'IBM'", true);

        TextMessage tmsg = (TextMessage) jmsConsumer.receive(DEFAULT_TIMEOUT);

        if (tmsg == null)
            exceptionFlag = true;

        jmsConsumer.close();
        jmsContextReceiver.unsubscribe("SUBID");

        jmsContextReceiver.close();

        if (exceptionFlag == true)
            throw new WrongException(
                            "testCreateUnSharedDurableConsumer_Sel_consume_TCP failed");

    }

    public void testCreateUnSharedDurableConsumer_Sel_create_Expiry(
                                                                    HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        JMSContext jmsContextSender = tcfBindings.createContext();
        JMSContext jmsContextReceiver = tcfBindings.createContext();

        JMSConsumer jmsConsumer = jmsContextReceiver
                        .createDurableConsumer(topic1, "EXPID", "Company = 'IBM'", true);

        JMSProducer jmsProducer = jmsContextSender.createProducer();

        TextMessage tmsg = jmsContextSender
                        .createTextMessage("This is a test message");
        tmsg.setStringProperty("Company", "IBM");

        jmsProducer.send(topic1, tmsg);

        jmsContextSender.close();

    }

    public void testCreateUnSharedDurableConsumer_Sel_consume_Expiry(
                                                                     HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        JMSContext jmsContextReceiver = tcfBindings.createContext();
        JMSConsumer jmsConsumer = jmsContextReceiver
                        .createDurableConsumer(topic1, "EXPID", "Company = 'IBM'", true);

        TextMessage tmsg = (TextMessage) jmsConsumer.receive(DEFAULT_TIMEOUT);

        if (tmsg != null)
            exceptionFlag = true;

        jmsConsumer.close();
        jmsContextReceiver.unsubscribe("EXPID");

        jmsContextReceiver.close();

        if (exceptionFlag == true)
            throw new WrongException(
                            "testCreateUnSharedDurableConsumer_Sel_consume_Expiry failed");

    }

    public void testCreateUnSharedDurableConsumer_Sel_create_Expiry_TCP(
                                                                        HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        JMSContext jmsContextSender = tcfTCP.createContext();

        JMSContext jmsContextReceiver = tcfTCP.createContext();

        JMSConsumer jmsConsumer = jmsContextReceiver
                        .createDurableConsumer(topic1, "EXPID", "Company = 'IBM'", true);

        JMSProducer jmsProducer = jmsContextSender.createProducer();
        TextMessage tmsg = jmsContextSender
                        .createTextMessage("This is a test message");
        tmsg.setStringProperty("Company", "IBM");

        jmsProducer.send(topic1, tmsg);

        jmsContextReceiver.close();

        jmsContextSender.close();

    }

    public void testCreateUnSharedDurableConsumer_Sel_consume_Expiry_TCP(
                                                                         HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        JMSContext jmsContextReceiver = tcfTCP.createContext();
        JMSConsumer jmsConsumer = jmsContextReceiver
                        .createDurableConsumer(topic1, "EXPID", "Company = 'IBM'", true);

        TextMessage tmsg = (TextMessage) jmsConsumer.receive(DEFAULT_TIMEOUT);

        if (tmsg != null)
            exceptionFlag = true;

        jmsConsumer.close();
        jmsContextReceiver.unsubscribe("EXPID");

        jmsContextReceiver.close();

        if (exceptionFlag)
            throw new WrongException(
                            "testCreateUnSharedDurableConsumer_Sel_consume_Expiry_TCP failed");

    }

    //  If a shared durable subscription already exists with the same
    // name and client identifier (if set), and the same topic and message
    // selector has been specified, then this method creates a JMSConsumer on
    // the existing shared durable subscription.

    public void testCreateSharedDurableConsumer_Sel_2Subscribers(
                                                                 HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContext = tcfBindings.createContext();

        JMSConsumer jmsConsumer = jmsContext.createDurableConsumer(topic,
                                                                   "DURATEST2", "Company = 'IBM'", true);

        JMSProducer jmsProducer = jmsContext.createProducer();

        TextMessage tmsg = jmsContext
                        .createTextMessage("This is a test message");
        tmsg.setStringProperty("Company", "IBM");
        jmsProducer.send(topic, tmsg);

        tmsg = (TextMessage) jmsConsumer.receive(DEFAULT_TIMEOUT);

        jmsConsumer.close();

        try {
            JMSConsumer jmsConsumer2 = jmsContext.createDurableConsumer(
                                                                        topic, "DURATEST2", "Company = 'IBM'", true);

        } catch (Exception mfe) {
            mfe.printStackTrace();
            exceptionFlag = true;
        }

        jmsContext.unsubscribe("DURATEST2");
        jmsContext.close();

        if (exceptionFlag == false)
            throw new WrongException("testCreateSharedDurableConsumer_Sel_2Subscribers failed");

    }

    public void testCreateSharedDurableConsumer_Sel_2Subscribers_TCP(
                                                                     HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        exceptionFlag = false;

        JMSContext jmsContext = tcfTCP.createContext();
        JMSConsumer jmsConsumer = jmsContext.createDurableConsumer(topic,
                                                                   "DURATEST1", "Company = 'IBM'", true);

        JMSProducer jmsProducer = jmsContext.createProducer();

        TextMessage tmsg = jmsContext
                        .createTextMessage("This is a test message");
        tmsg.setStringProperty("Company", "IBM");

        jmsProducer.send(topic, tmsg);

        tmsg = (TextMessage) jmsConsumer.receive(DEFAULT_TIMEOUT);

        jmsConsumer.close();

        try {
            JMSConsumer jmsConsumer2 = jmsContext.createDurableConsumer(topic, "DURATEST1", "Company = 'IBM'", true);

        } catch (Exception mfe) {
            mfe.printStackTrace();
            exceptionFlag = true;
        }

        jmsContext.unsubscribe("DURATEST1");

        jmsContext.close();

        if (exceptionFlag == false)
            throw new WrongException("testCreateSharedDurableConsumer_Sel_2Subscribers_TCP failed");

    }

    //  If a shared durable subscription already exists with the same
    // name and client identifier (if set) but a different topic has been
    // specified, and there is no consumer already active (i.e. not closed) on
    // the durable subscription then this is equivalent to unsubscribing
    // (deleting) the old one and creating a new one.

    public void testCreateSharedDurableConsumer_Sel_2SubscribersDiffTopic(
                                                                          HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContext = tcfBindings.createContext();

        JMSConsumer jmsConsumer = jmsContext.createDurableConsumer(topic, "DURATEST3", "Company = 'IBM'", true);

        JMSProducer jmsProducer = jmsContext.createProducer();

        TextMessage tmsg = jmsContext
                        .createTextMessage("This is a test message");

        tmsg.setStringProperty("Company", "IBM");

        jmsProducer.send(topic1, tmsg);

        tmsg = (TextMessage) jmsConsumer.receive(DEFAULT_TIMEOUT);

        jmsConsumer.close();
        //jmsContext.close();
        try {
            JMSConsumer jmsConsumer2 = jmsContext.createDurableConsumer(topic1, "DURATEST3", "Company = 'IBM'", true);
        } catch (Exception mfe) {

            mfe.printStackTrace();
            exceptionFlag = true;
        }

        jmsContext.unsubscribe("DURATEST3");

        jmsContext.close();

        if (exceptionFlag == false)
            throw new WrongException("testCreateSharedDurableConsumer_Sel_2SubscribersDiffTopic failed");

    }

    public void testCreateSharedDurableConsumer_Sel_2SubscribersDiffTopic_TCP(
                                                                              HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {
        exceptionFlag = true;

        JMSContext jmsContext = tcfTCP.createContext();

        JMSConsumer jmsConsumer = jmsContext.createDurableConsumer(topic, "DURATEST4", "Company = 'IBM'", true);

        JMSProducer jmsProducer = jmsContext.createProducer();

        TextMessage tmsg = jmsContext
                        .createTextMessage("This is a test message");

        tmsg.setStringProperty("Company", "IBM");
        jmsProducer.send(topic1, tmsg);

        tmsg = (TextMessage) jmsConsumer.receive(DEFAULT_TIMEOUT);

        jmsConsumer.close();
        //jmsContext.close();
        try {
            JMSConsumer jmsConsumer2 = jmsContext.createDurableConsumer(topic1, "DURATEST4", "Company = 'IBM'", true);
        } catch (Exception mfe) {

            mfe.printStackTrace();
            exceptionFlag = true;
        }

        jmsContext.unsubscribe("DURATEST4");

        jmsContext.close();

        if (exceptionFlag == false)
            throw new WrongException("testCreateSharedDurableConsumer_Sel_2SubscribersDiffTopic_TCP failed");

    }

    //  If a shared durable subscription already exists with the same
    // name and client identifier (if set) but a different topic has been
    // specified, and there is a consumer already active (i.e. not closed) on
    // the durable subscription, then a JMSRuntimeException will be thrown.

    public void testCreateSharedDurableConsumer_Sel_JRException(
                                                                HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContext = tcfBindings.createContext();

        JMSConsumer jmsConsumer = jmsContext.createDurableConsumer(topic, "DURATEST1", "Company = 'IBM'", true);

        JMSProducer jmsProducer = jmsContext.createProducer();

        TextMessage tmsg = jmsContext
                        .createTextMessage("This is a test message");

        tmsg.setStringProperty("Company", "IBM");

        jmsProducer.send(topic, tmsg);

        tmsg = (TextMessage) jmsConsumer.receive(DEFAULT_TIMEOUT);

        try {
            JMSConsumer jmsConsumer1 = jmsContext
                            .createDurableConsumer(topic1, "DURATEST1", "Company = 'IBM'", true);
        } catch (JMSRuntimeException ex) {
            ex.printStackTrace();
            exceptionFlag = true;

        }

        jmsConsumer.close();
        jmsContext.unsubscribe("DURATEST1");

        jmsContext.close();

        if (!(exceptionFlag))
            throw new WrongException("testCreateSharedDurableConsumer_Sel_JRException failed");

    }

    public void testCreateSharedDurableConsumer_Sel_JRException_TCP(
                                                                    HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        JMSContext jmsContext = tcfTCP.createContext();

        JMSConsumer jmsConsumer = jmsContext.createDurableConsumer(topic, "DURATEST1", "Company = 'IBM'", true);

        JMSProducer jmsProducer = jmsContext.createProducer();

        TextMessage tmsg = jmsContext
                        .createTextMessage("This is a test message");

        tmsg.setStringProperty("Company", "IBM");

        jmsProducer.send(topic, tmsg);

        tmsg = (TextMessage) jmsConsumer.receive(DEFAULT_TIMEOUT);

        try {
            JMSConsumer jmsConsumer1 = jmsContext
                            .createDurableConsumer(topic1, "DURATEST1", "Company = 'IBM'", true);
        } catch (JMSRuntimeException ex) {
            ex.printStackTrace();
            exceptionFlag = true;

        }

        jmsConsumer.close();
        jmsContext.unsubscribe("DURATEST1");

        jmsContext.close();

        if (!(exceptionFlag))
            throw new WrongException("testCreateSharedDurableConsumer_JRException_TCP failed");

    }

    //  A shared durable subscription and an unshared durable
    // subscription may not have the same name and client identifier (if set).
    // If an unshared durable subscription already exists with the same name and
    // client identifier (if set) then a JMSRuntimeException is thrown.

    public void testCreateSharedDurableUndurableConsumer_Sel_JRException(
                                                                         HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContext = tcfBindings.createContext();

        JMSConsumer jmsConsumer = jmsContext.createDurableConsumer(topic, "DURATEST1", "Company = 'IBM'", true);

        JMSProducer jmsProducer = jmsContext.createProducer();

        TextMessage tmsg = jmsContext
                        .createTextMessage("This is a test message");

        tmsg.setStringProperty("Company", "IBM");

        jmsProducer.send(topic, tmsg);

        tmsg = (TextMessage) jmsConsumer.receive(DEFAULT_TIMEOUT);

        try {

            JMSConsumer jmsConsumer1 = jmsContext.createSharedDurableConsumer(topic, "DURATEST1");
        } catch (JMSRuntimeException ex) {
            ex.printStackTrace();
            exceptionFlag = true;

        }

        jmsConsumer.close();
        jmsContext.unsubscribe("DURATEST1");

        jmsContext.close();

        if (exceptionFlag == false)
            throw new WrongException("testCreateSharedDurableUndurableConsumer_Sel_JRException failed");

    }

    public void testCreateSharedDurableUndurableConsumer_Sel_JRException_TCP(
                                                                             HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        exceptionFlag = false;

        JMSContext jmsContext = tcfTCP.createContext();

        JMSConsumer jmsConsumer = jmsContext.createDurableConsumer(topic, "DURATEST5", "Company = 'IBM'", true);

        JMSProducer jmsProducer = jmsContext.createProducer();

        TextMessage tmsg = jmsContext
                        .createTextMessage("This is a test message");
        tmsg.setStringProperty("Company", "IBM");

        jmsProducer.send(topic, tmsg);

        tmsg = (TextMessage) jmsConsumer.receive(DEFAULT_TIMEOUT);

        try {

            JMSConsumer jmsConsumer1 = jmsContext.createSharedDurableConsumer(topic, "DURATEST5");
        } catch (JMSRuntimeException ex) {
            ex.printStackTrace();
            exceptionFlag = true;

        }

        jmsConsumer.close();
        jmsContext.unsubscribe("DURATEST5");

        jmsContext.close();

        if (exceptionFlag == false)
            throw new WrongException("testCreateSharedDurableUndurableConsumer_JRException failed");

    }

    //  InvalidDestinationRuntimeException - if an invalid topic is
    // specified.

    public void testCreateSharedDurableConsumer_Sel_InvalidDestination(
                                                                       HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContext = tcfBindings.createContext();

        try {

            JMSConsumer jmsConsumer = jmsContext
                            .createDurableConsumer(null, "DURATEST1", "Company = 'IBM'", true);

        } catch (InvalidDestinationRuntimeException ex) {
            ex.printStackTrace();
            exceptionFlag = true;

        }

        jmsContext.close();
        if (!(exceptionFlag))
            throw new WrongException("testCreateSharedDurableConsumer_Sel_InvalidDestination failed");

    }

    public void testCreateSharedDurableConsumer_Sel_InvalidDestination_TCP(
                                                                           HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContext = tcfTCP.createContext();

        try {

            JMSConsumer jmsConsumer = jmsContext
                            .createDurableConsumer(null, "DURATEST1", "Company = 'IBM'", true);

        } catch (InvalidDestinationRuntimeException ex) {
            ex.printStackTrace();
            exceptionFlag = true;
        }

        jmsContext.close();
        if (!(exceptionFlag))
            throw new WrongException("testCreateSharedDurableConsumer_Sel_InvalidDestination_TCP failed");

    }

    // Case where name is null and empty string

    public void testCreateSharedDurableConsumer_Sel_Null(
                                                         HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        exceptionFlag = false;
        boolean val1 = false;
        boolean val2 = false;
        JMSContext jmsContext = tcfBindings.createContext();

        try {

            JMSConsumer jmsConsumer = jmsContext
                            .createDurableConsumer(topic, null, "Company = 'IBM'", true);

        } catch (JMSRuntimeException ex) {
            ex.printStackTrace();
            val1 = true;
        }

        try {
            JMSConsumer jmsConsumer = jmsContext
                            .createDurableConsumer(topic, "", "Company = 'IBM'", true);

        } catch (JMSRuntimeException ex) {
            ex.printStackTrace();
            val2 = true;
        }
        if (!(val1 == true && val2 == true))
            exceptionFlag = true;
        jmsContext.close();
        if (exceptionFlag)
            throw new WrongException("testCreateSharedDurableConsumer_Sel_Null failed");

    }

    public void testCreateSharedDurableConsumer_Sel_Null_TCP(
                                                             HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        exceptionFlag = false;
        boolean val1 = false;
        boolean val2 = false;

        JMSContext jmsContext = tcfTCP.createContext();
        try {

            JMSConsumer jmsConsumer = jmsContext
                            .createDurableConsumer(topic, null, "Company = 'IBM'", true);

        } catch (JMSRuntimeException ex) {
            ex.printStackTrace();
            val1 = true;
        }

        try {
            JMSConsumer jmsConsumer = jmsContext
                            .createDurableConsumer(topic, "", "Company = 'IBM'", true);

        } catch (JMSRuntimeException ex) {
            ex.printStackTrace();
            val2 = true;
        }
        if (!(val1 == true && val2 == true))
            exceptionFlag = true;

        jmsContext.close();

        if (exceptionFlag)
            throw new WrongException("testCreateSharedDurableConsumer_Sel_Null_TCP failed");

    }

    // =========================================================== test cases with selectors  end ================================================================================  

    public class WrongException extends Exception {
        String str;

        public WrongException(String str) {
            this.str = str;
            System.out.println(" <ERROR> " + str + " </ERROR>");
        }
    }

}
