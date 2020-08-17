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
package sharedsubscription.web;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.lang.reflect.InvocationTargetException;
import java.util.Enumeration;

import javax.jms.InvalidDestinationException;
import javax.jms.InvalidDestinationRuntimeException;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.JMSProducer;
import javax.jms.JMSRuntimeException;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSession;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicSession;
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
public class SharedSubscriptionServlet extends HttpServlet {
    public static final String JMXMessage = "This is MessagingMBeanServlet.";

    public final static String MBEAN_TYPE_ME = "WEMMessagingEngine";

    public static boolean sessionValue = false;
    public static boolean connectionStart = false;
    public static boolean flag = false;
    public static boolean compFlag = false;
    public static boolean exp = false;
    public static QueueConnectionFactory QCFBindings;
    public static QueueConnectionFactory QCFTCP;

    public static TopicConnectionFactory TCFBindings;
    public static TopicConnectionFactory TCFTCP;

    public static boolean exceptionFlag;
    public static Queue queue;
    public static Queue queue1;
    public static Queue queue2;
    public static Queue queue3;
    public static Topic topic;
    public static Topic topic1;
    public static Topic topic2;
    public static Topic expiryTopic;

    @Override
    public void init() throws ServletException {
        // TODO Auto-generated method stub

        super.init();
        try {

            QCFBindings = getQCFBindings();
            TCFBindings = getTCFBindings();
            QCFTCP = getQCFTCP();
            TCFTCP = getTCFTCP();
            queue = (Queue) new InitialContext()
                            .lookup("java:comp/env/jndi_INPUT_Q");

            queue1 = (Queue) new InitialContext()
                            .lookup("java:comp/env/jndi_INPUT_Q1");

            queue2 = (Queue) new InitialContext()
                            .lookup("java:comp/env/jndi_INPUT_Q2");

            queue3 = (Queue) new InitialContext()
                            .lookup("java:comp/env/jndi_INPUT_Q3");

            topic = (Topic) new InitialContext()
                            .lookup("java:comp/env/eis/topic1");

            expiryTopic = (Topic) new InitialContext()
                            .lookup("java:comp/env/eis/topic2");

            topic1 = (Topic) new InitialContext()
                            .lookup("java:comp/env/eis/topic3");

            topic2 = (Topic) new InitialContext()
                            .lookup("java:comp/env/eis/topic4");

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
        final TraceComponent tc = Tr.register(SharedSubscriptionServlet.class); // injection
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
            getClass().getMethod(test, HttpServletRequest.class,
                                 HttpServletResponse.class).invoke(this, request, response);
            System.out.println(" Starting : " + test);
            out.println(test + " COMPLETED SUCCESSFULLY");
            System.out.println(" Ending : " + test);
            Tr.exit(this, tc, test);
        } catch (Throwable x) {
            if (x instanceof InvocationTargetException)
                x = x.getCause();
            Tr.exit(this, tc, test, x);
            out.println("<pre>ERROR in " + test + ":");
            System.out.println(" Ending : " + test);
            x.printStackTrace(out);
            out.println("</pre>");
        }
    }

    // 129623_1 JMSConsumer createSharedDurableConsumer(Topic topic,String name)
    // 129623_1_1 Creates a shared durable subscription on the specified topic
    // (if one does not already exist) and creates a consumer on that durable
    // subscription. This method creates the durable subscription without a
    // message selector.

    public void testCreateSharedDurableConsumer_create_B_SecOff(
                                                                HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        JMSContext jmsContextSender = TCFBindings.createContext();

        JMSContext jmsContextReceiver = TCFBindings.createContext();

        JMSConsumer jmsConsumer = jmsContextReceiver
                        .createSharedDurableConsumer(topic, "SUBID");

        JMSProducer jmsProducer = jmsContextSender.createProducer();

        TextMessage tmsg = jmsContextSender
                        .createTextMessage("This is a test message");

        jmsProducer.send(topic, tmsg);

        jmsContextSender.close();

    }

    public void testCreateSharedDurableConsumer_consume_B_SecOff(
                                                                 HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        exceptionFlag = false;

        JMSContext jmsContextReceiver = TCFBindings.createContext();
        JMSConsumer jmsConsumer = jmsContextReceiver
                        .createSharedDurableConsumer(topic, "SUBID");

        TextMessage tmsg = (TextMessage) jmsConsumer.receive(30000);

        if (!(tmsg != null))
            exceptionFlag = true;

        if (exceptionFlag)
            throw new WrongException("testCreateSharedDurableExpiry_B_SecOff failed");

        jmsConsumer.close();

        jmsContextReceiver.unsubscribe("SUBID");

        jmsContextReceiver.close();

    }

    public void testCreateSharedDurableConsumer_create_TCP_SecOff(
                                                                  HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        JMSContext jmsContextSender = TCFTCP.createContext();

        JMSContext jmsContextReceiver = TCFTCP.createContext();

        JMSConsumer jmsConsumer = jmsContextReceiver
                        .createSharedDurableConsumer(topic, "SUBID");

        JMSProducer jmsProducer = jmsContextSender.createProducer();

        TextMessage tmsg = jmsContextSender
                        .createTextMessage("This is a test message");

        jmsProducer.send(topic, tmsg);

        jmsContextSender.close();

    }

    public void testCreateSharedDurableConsumer_consume_TCP_SecOff(
                                                                   HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        exceptionFlag = false;

        JMSContext jmsContextReceiver = TCFTCP.createContext();
        JMSConsumer jmsConsumer = jmsContextReceiver
                        .createSharedDurableConsumer(topic, "SUBID");

        TextMessage tmsg = (TextMessage) jmsConsumer.receive(30000);

        if (!(tmsg != null))
            exceptionFlag = true;

        if (exceptionFlag)
            throw new WrongException("testCreateSharedDurableExpiry_TCP_SecOff failed");

        jmsConsumer.close();

        jmsContextReceiver.unsubscribe("SUBID");

        jmsContextReceiver.close();
    }

    // 129623_1_3 The JMS provider retains a record of this durable subscription
    // and ensures that all messages from the topic's publishers are retained
    // until they are delivered to, and acknowledged by, a consumer on this
    // durable subscription or until they have expired.

    public void testCreateSharedDurableConsumer_create_Expiry_B_SecOff(
                                                                       HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        JMSContext jmsContextSender = TCFBindings.createContext();
        JMSContext jmsContextReceiver = TCFBindings.createContext();

        JMSConsumer jmsConsumer = jmsContextReceiver
                        .createSharedDurableConsumer(expiryTopic, "EXPID");

        JMSProducer jmsProducer = jmsContextSender.createProducer();

        TextMessage tmsg = jmsContextSender
                        .createTextMessage("This is a test message");

        jmsProducer.send(expiryTopic, tmsg);

        jmsContextSender.close();

    }

    public void testCreateSharedDurableConsumer_consume_Expiry_B_SecOff(
                                                                        HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {
        exceptionFlag = false;
        JMSContext jmsContextReceiver = TCFBindings.createContext();
        JMSConsumer jmsConsumer = jmsContextReceiver
                        .createSharedDurableConsumer(topic, "EXPID");

        TextMessage tmsg = (TextMessage) jmsConsumer.receive(30000);
        System.out.println("Message Received is :" + tmsg);
        if (!(tmsg == null))
            exceptionFlag = true;
        if (exceptionFlag)
            throw new WrongException("testCreateSharedDurableExpiry_B_SecOff failed");
        jmsConsumer.close();
        jmsContextReceiver.unsubscribe("EXPID");

        jmsContextReceiver.close();

    }

    public void testCreateSharedDurableConsumer_create_Expiry_TCP_SecOff(
                                                                         HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        JMSContext jmsContextSender = TCFTCP.createContext();
        JMSContext jmsContextReceiver = TCFTCP.createContext();

        JMSConsumer jmsConsumer = jmsContextReceiver
                        .createSharedDurableConsumer(expiryTopic, "EXPID");

        JMSProducer jmsProducer = jmsContextSender.createProducer();

        TextMessage tmsg = jmsContextSender
                        .createTextMessage("This is a test message");

        jmsProducer.send(expiryTopic, tmsg);

        jmsContextSender.close();

    }

    public void testCreateSharedDurableConsumer_consume_Expiry_TCP_SecOff(
                                                                          HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContextReceiver = TCFTCP.createContext();
        JMSConsumer jmsConsumer = jmsContextReceiver
                        .createSharedDurableConsumer(expiryTopic, "EXPID");

        TextMessage tmsg = (TextMessage) jmsConsumer.receive(30000);

        if (!(tmsg == null))
            exceptionFlag = true;
        if (exceptionFlag)
            throw new WrongException("testCreateSharedDurableExpiry_TCP_SecOff failed");
        jmsConsumer.close();
        jmsContextReceiver.unsubscribe("EXPID");

        jmsContextReceiver.close();

    }

    // 129623_1_4 A durable subscription will continue to accumulate messages
    // until it is deleted using the unsubscribe method.

    public void testCreateSharedDurableConsumer_unsubscribe_B_SecOff(
                                                                     HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {
        exceptionFlag = false;
        JMSContext jmsContextTCFBindings = TCFBindings.createContext();

        JMSConsumer jmsConsumer = jmsContextTCFBindings.createSharedDurableConsumer(topic, "DURATEST");

        JMSProducer jmsProducer = jmsContextTCFBindings.createProducer();

        TextMessage tmsg = jmsContextTCFBindings
                        .createTextMessage("This is a test message");

        jmsProducer.send(topic, tmsg);

        tmsg = (TextMessage) jmsConsumer.receive(30000);

        jmsConsumer.close();

        jmsContextTCFBindings.unsubscribe("DURATEST");

        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();

        final ObjectName name = new ObjectName(
                        "WebSphere:feature=wasJmsServer,type=Subscriber,name=clientID##DURATEST");
        System.out.println("initialized MBeanServer and Object");
        System.out.println("object created");

        try {
            String obn = (String) mbs.getAttribute(name, "Id");

        } catch (InstanceNotFoundException ex) {
            ex.printStackTrace();
            exceptionFlag = true;
        }
        if (!(exceptionFlag))
            throw new WrongException("testCreateSharedDurableConsumer_unsubscribe_B_SecOff failed");

        jmsContextTCFBindings.close();

    }

    public void testCreateSharedDurableConsumer_unsubscribe_TCP_SecOff(
                                                                       HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {
        exceptionFlag = false;
        JMSContext jmsContextTCFTCP = TCFTCP.createContext();

        JMSConsumer jmsConsumer = jmsContextTCFTCP.createSharedDurableConsumer(topic, "DURATEST");

        JMSProducer jmsProducer = jmsContextTCFTCP.createProducer();

        TextMessage tmsg = jmsContextTCFTCP
                        .createTextMessage("This is a test message");

        jmsProducer.send(topic, tmsg);

        tmsg = (TextMessage) jmsConsumer.receive(30000);

        jmsConsumer.close();

        jmsContextTCFTCP.unsubscribe("DURATEST");

        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();

        final ObjectName name = new ObjectName(
                        "WebSphere:feature=wasJmsServer,type=Subscriber,name=clientID##DURATEST");
        System.out.println("initialized MBeanServer and Object");
        System.out.println("object created");

        try {
            String obn = (String) mbs.getAttribute(name, "Id");

        } catch (InstanceNotFoundException ex) {
            ex.printStackTrace();
            exceptionFlag = true;
        }

        if (!(exceptionFlag))
            throw new WrongException("testCreateSharedDurableConsumer_unsubscribe_TCP_SecOff failed");

        jmsContextTCFTCP.close();
    }

    /*
     * // 129623_1_5 Any durable subscription created using this method will be
     * // shared. This means that multiple active (i.e. not closed) consumers on
     * // the subscription may exist at the same time. The term "consumer" here
     * // means a JMSConsumer object in any client.
     * // 129623_1_6 A shared durable subscription is identified by a name
     * // specified by the client and by the client identifier (which may be
     * // unset). An application which subsequently wishes to create a consumer on
     * // that shared durable subscription must use the same client identifier.
     * 
     * public void testBasicMDBTopic(HttpServletRequest request,
     * HttpServletResponse response) throws Throwable {
     * 
     * TopicConnectionFactory cf1 = (TopicConnectionFactory) new InitialContext()
     * .lookup("java:comp/env/jms/FAT_TCF");
     * 
     * int msgs = 3;
     * 
     * Topic topic = (Topic) new InitialContext()
     * .lookup("java:comp/env/jms/FAT_TOPIC");
     * 
     * JMSContext jmsCont = cf1.createContext();
     * JMSProducer publisher = jmsCont.createProducer();
     * 
     * for (int i = 0; i < msgs; i++) {
     * publisher.send(topic, "testBasicMDBTopic:" + i);
     * }
     * 
     * System.out.println("Published  messages ");
     * 
     * jmsCont.close();
     * 
     * }
     * 
     * public void testBasicMDBTopic_TCP(HttpServletRequest request,
     * HttpServletResponse response) throws Throwable {
     * 
     * TopicConnectionFactory cf1 = (TopicConnectionFactory) new InitialContext()
     * .lookup("java:comp/env/jms/FAT_COMMS_TCF");
     * 
     * int msgs = 3;
     * 
     * Topic topic = (Topic) new InitialContext()
     * .lookup("java:comp/env/jms/FAT_TOPIC");
     * 
     * JMSContext jmsCont = cf1.createContext();
     * JMSProducer publisher = jmsCont.createProducer();
     * 
     * for (int i = 0; i < msgs; i++) {
     * publisher.send(topic, "testBasicMDBTopic:" + i);
     * }
     * 
     * System.out.println("Published  messages ");
     * 
     * jmsCont.close();
     * 
     * }
     */
    // 129623_1_7 If a shared durable subscription already exists with the same
    // name and client identifier (if set), and the same topic and message
    // selector has been specified, then this method creates a JMSConsumer on
    // the existing shared durable subscription.

    public void testCreateSharedDurableConsumer_2Subscribers_B_SecOff(
                                                                      HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        exceptionFlag = false;

        JMSContext jmsContextTCFBindings = TCFBindings.createContext();

        JMSConsumer jmsConsumer = jmsContextTCFBindings.createSharedDurableConsumer(topic, "PUBSUBTEST");

        JMSProducer jmsProducer = jmsContextTCFBindings.createProducer();

        TextMessage tmsg = jmsContextTCFBindings.createTextMessage("This is a test message");

        jmsProducer.send(topic, tmsg);

        tmsg = (TextMessage) jmsConsumer.receive(30000);

        JMSConsumer jmsConsumerCopy = jmsContextTCFBindings.createSharedDurableConsumer(topic, "PUBSUBTEST");

        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();

        final ObjectName name = new ObjectName(
                        "WebSphere:feature=wasJmsServer,type=Topic,name=NewTopic1");

        javax.management.openmbean.CompositeData[] obn = (CompositeData[]) mbs
                        .invoke(name, "listSubscriptions", null, null);

        if (!(obn.length == 1))
            exceptionFlag = true;

        if (exceptionFlag)
            throw new WrongException("testCreateSharedDurableConsumer_2Subscribers_B_SecOff failed");

        jmsConsumer.close();
        jmsConsumerCopy.close();
        jmsContextTCFBindings.unsubscribe("PUBSUBTEST");
        jmsContextTCFBindings.close();

    }

    public void testCreateSharedDurableConsumer_2Subscribers_TCP_SecOff(
                                                                        HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        exceptionFlag = false;

        JMSContext jmsContextTCFTCP = TCFTCP.createContext();

        JMSConsumer jmsConsumer = jmsContextTCFTCP.createSharedDurableConsumer(topic, "PUBSUBTEST");

        JMSProducer jmsProducer = jmsContextTCFTCP.createProducer();

        TextMessage tmsg = jmsContextTCFTCP.createTextMessage("This is a test message");

        jmsProducer.send(topic, tmsg);

        tmsg = (TextMessage) jmsConsumer.receive(30000);

        JMSConsumer jmsConsumerCopy = jmsContextTCFTCP.createSharedDurableConsumer(topic, "PUBSUBTEST");
        jmsProducer.send(topic, tmsg);
        tmsg = (TextMessage) jmsConsumerCopy.receive(30000);

        if (!(tmsg != null))
            exceptionFlag = true;

        if (exceptionFlag)
            throw new WrongException("testCreateSharedDurableConsumer_2Subscribers_TCP_SecOff failed");

        jmsConsumer.close();
        jmsConsumerCopy.close();
        jmsContextTCFTCP.unsubscribe("PUBSUBTEST");
        jmsContextTCFTCP.close();
    }

    // 129623_1_8 If a shared durable subscription already exists with the same
    // name and client identifier (if set) but a different topic has been
    // specified, and there is no consumer already active (i.e. not closed) on
    // the durable subscription then this is equivalent to unsubscribing
    // (deleting) the old one and creating a new one.

    public void testCreateSharedDurableConsumer_2SubscribersDiffTopic_B_SecOff(
                                                                               HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        exceptionFlag = false;

        JMSContext jmsContextTCFBindings = TCFBindings.createContext();

        JMSConsumer jmsConsumer = jmsContextTCFBindings.createSharedDurableConsumer(topic1, "DURATEST123");

        JMSProducer jmsProducer = jmsContextTCFBindings.createProducer();

        TextMessage tmsg = jmsContextTCFBindings
                        .createTextMessage("This is a test message");

        jmsProducer.send(topic1, tmsg);

        tmsg = (TextMessage) jmsConsumer.receive(30000);
        System.out.println("Message Received is :" + tmsg);

        jmsConsumer.close();

        JMSConsumer jmsConsumer2 = jmsContextTCFBindings.createSharedDurableConsumer(
                                                                                     topic2, "DURATEST123");

        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();

        final ObjectName name = new ObjectName(
                        "WebSphere:feature=wasJmsServer,type=Topic,name=NewTopic4");

        javax.management.openmbean.CompositeData[] obn = (CompositeData[]) mbs
                        .invoke(name, "listSubscriptions", null, null);

        if (!(obn.length == 1))
            exceptionFlag = true;

        if (exceptionFlag)
            throw new WrongException("testCreateSharedDurableConsumer_2SubscribersDiffTopic_B_SecOff failed");

        jmsConsumer2.close();
        jmsContextTCFBindings.unsubscribe("DURATEST123");

    }

    public void testCreateSharedDurableConsumer_2SubscribersDiffTopic_TCP_SecOff(
                                                                                 HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {
        try
        {
            exceptionFlag = false;

            JMSContext jmsContextTCFTCP = TCFTCP.createContext();

            JMSConsumer jmsConsumer = jmsContextTCFTCP.createSharedDurableConsumer(topic1, "DURATEST123");

            JMSProducer jmsProducer = jmsContextTCFTCP.createProducer();

            TextMessage tmsg = jmsContextTCFTCP
                            .createTextMessage("This is a test message");

            jmsProducer.send(topic1, tmsg);

            tmsg = (TextMessage) jmsConsumer.receive(30000);
            System.out.println("Message Received is :" + tmsg);

            jmsConsumer.close();

            Thread.sleep(30000);

            JMSConsumer jmsConsumer2 = jmsContextTCFTCP.createSharedDurableConsumer(
                                                                                    topic2, "DURATEST123");
            jmsProducer.send(topic2, tmsg);
            tmsg = (TextMessage) jmsConsumer2.receive(30000);
            System.out.println("Message Received is :" + tmsg);

            if (!(tmsg != null))
                exceptionFlag = true;

            if (exceptionFlag)
                throw new WrongException("testCreateSharedDurableConsumer_2SubscribersDiffTopic_B_SecOff failed");

            jmsConsumer2.close();
            System.out.println("before unscribe");
            jmsContextTCFTCP.unsubscribe("DURATEST123");
        } catch (Exception e)
        {
            e.printStackTrace();
            throw e;
        }
    }

    // 129623_1_9 If a shared durable subscription already exists with the same
    // name and client identifier (if set) but a different topic has been
    // specified, and there is a consumer already active (i.e. not closed) on
    // the durable subscription, then a JMSRuntimeException will be thrown.

    public void testCreateSharedDurableConsumer_JRException_B_SecOff(
                                                                     HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        exceptionFlag = false;

        JMSContext jmsContextTCFBindings = TCFBindings.createContext();

        JMSConsumer jmsConsumer = jmsContextTCFBindings.createSharedDurableConsumer(
                                                                                    topic, "DURATEST456");

        JMSProducer jmsProducer = jmsContextTCFBindings.createProducer();

        TextMessage tmsg = jmsContextTCFBindings
                        .createTextMessage("This is a test message");

        jmsProducer.send(topic, tmsg);

        tmsg = (TextMessage) jmsConsumer.receive(30000);

        try {

            JMSConsumer jmsConsumer1 = jmsContextTCFBindings
                            .createSharedDurableConsumer(topic1, "DURATEST456");
        } catch (JMSRuntimeException ex) {
            ex.printStackTrace();
            exceptionFlag = true;

        }

        if (!(exceptionFlag))
            throw new WrongException("testCreateSharedDurableConsumer_JRException_B_SecOff failed");

        jmsConsumer.close();
        jmsContextTCFBindings.unsubscribe("DURATEST456");
        jmsContextTCFBindings.close();

    }

    public void testCreateSharedDurableConsumer_JRException_TCP_SecOff(
                                                                       HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        exceptionFlag = false;

        JMSContext jmsContextTCFTCP = TCFTCP.createContext();

        JMSConsumer jmsConsumer = jmsContextTCFTCP.createSharedDurableConsumer(topic, "DURATEST456");

        JMSProducer jmsProducer = jmsContextTCFTCP.createProducer();

        TextMessage tmsg = jmsContextTCFTCP
                        .createTextMessage("This is a test message");

        jmsProducer.send(topic, tmsg);

        tmsg = (TextMessage) jmsConsumer.receive(30000);

        try {

            JMSConsumer jmsConsumer1 = jmsContextTCFTCP
                            .createSharedDurableConsumer(topic1, "DURATEST456");
        } catch (JMSRuntimeException ex) {
            ex.printStackTrace();
            exceptionFlag = true;

        }

        if (!(exceptionFlag))
            throw new WrongException("testCreateSharedDurableConsumer_JRException_TCP_SecOff failed");

        jmsConsumer.close();
        jmsContextTCFTCP.unsubscribe("DURATEST456");
        jmsContextTCFTCP.close();

    }

    // 129623_1_10 A shared durable subscription and an unshared durable
    // subscription may not have the same name and client identifier (if set).
    // If an unshared durable subscription already exists with the same name and
    // client identifier (if set) then a JMSRuntimeException is thrown.

    public void testCreateSharedDurableUndurableConsumer_JRException_B_SecOff(
                                                                              HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContextTCFBindings = TCFBindings.createContext();

        JMSConsumer jmsConsumer = jmsContextTCFBindings.createDurableConsumer(topic1,
                                                                              "DURATESTPS");

        JMSProducer jmsProducer = jmsContextTCFBindings.createProducer();

        TextMessage tmsg = jmsContextTCFBindings
                        .createTextMessage("This is a test message");

        jmsProducer.send(topic1, tmsg);

        tmsg = (TextMessage) jmsConsumer.receive(30000);

        try {

            JMSConsumer jmsConsumer1 = jmsContextTCFBindings.createSharedDurableConsumer(
                                                                                         topic1, "DURATESTPS");
        } catch (JMSRuntimeException ex) {
            ex.printStackTrace();
            exceptionFlag = true;

        }

        if (!(exceptionFlag))
            throw new WrongException("testCreateSharedDurableUndurableConsumer_JRException_B_SecOff failed");

        jmsConsumer.close();
        jmsContextTCFBindings.unsubscribe("DURATESTPS");
        jmsContextTCFBindings.close();

    }

    public void testCreateSharedDurableUndurableConsumer_JRException_TCP_SecOff(
                                                                                HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContextTCFTCP = TCFTCP.createContext();

        JMSConsumer jmsConsumer = jmsContextTCFTCP.createDurableConsumer(topic1,
                                                                         "DURATESTPS");

        JMSProducer jmsProducer = jmsContextTCFTCP.createProducer();

        TextMessage tmsg = jmsContextTCFTCP
                        .createTextMessage("This is a test message");

        jmsProducer.send(topic1, tmsg);

        tmsg = (TextMessage) jmsConsumer.receive(30000);

        try {

            JMSConsumer jmsConsumer1 = jmsContextTCFTCP.createSharedDurableConsumer(
                                                                                    topic1, "DURATESTPS");
        } catch (JMSRuntimeException ex) {
            ex.printStackTrace();
            exceptionFlag = true;

        }

        if (!(exceptionFlag))
            throw new WrongException("testCreateSharedDurableUndurableConsumer_JRException_TCP_SecOff failed");

        jmsConsumer.close();
        jmsContextTCFTCP.unsubscribe("DURATESTPS");
        jmsContextTCFTCP.close();

    }

    // 129623_1_12 InvalidDestinationRuntimeException - if an invalid topic is
    // specified.

    public void testCreateSharedDurableConsumer_InvalidDestination_B_SecOff(
                                                                            HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        exceptionFlag = false;

        JMSContext jmsContextTCFBindings = TCFBindings.createContext();

        try {

            JMSConsumer jmsConsumer = jmsContextTCFBindings
                            .createSharedDurableConsumer(null, "DURATEST10");

        } catch (InvalidDestinationRuntimeException ex) {
            ex.printStackTrace();
            exceptionFlag = true;

        }

        if (!(exceptionFlag))
            throw new WrongException("testCreateSharedDurableConsumer_InvalidDestination_B_SecOff failed");

        jmsContextTCFBindings.close();

    }

    public void testCreateSharedDurableConsumer_InvalidDestination_TCP_SecOff(
                                                                              HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        exceptionFlag = false;

        JMSContext jmsContextTCFTCP = TCFTCP.createContext();

        try {

            JMSConsumer jmsConsumer = jmsContextTCFTCP
                            .createSharedDurableConsumer(null, "DURATEST10");

        } catch (InvalidDestinationRuntimeException ex) {
            ex.printStackTrace();
            exceptionFlag = true;

        }

        if (!(exceptionFlag))
            throw new WrongException("testCreateSharedDurableConsumer_InvalidDestination_TCP_SecOff failed");

        jmsContextTCFTCP.close();

    }

    // 129623_1_13 Case where name is null and empty string

    public void testCreateSharedDurableConsumer_Null_B_SecOff(
                                                              HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        exceptionFlag = false;
        boolean val1 = false;
        boolean val2 = false;
        JMSContext jmsContextTCFBindings = TCFBindings.createContext();

        try {

            JMSConsumer jmsConsumer = jmsContextTCFBindings
                            .createSharedDurableConsumer(topic, null);

        } catch (JMSRuntimeException ex) {
            ex.printStackTrace();
            val1 = true;
        }

        try {
            JMSConsumer jmsConsumer = jmsContextTCFBindings
                            .createSharedDurableConsumer(topic, "");

        } catch (JMSRuntimeException ex) {
            ex.printStackTrace();
            val2 = true;

        }

        if (!(val1 == true && val2 == true))
            exceptionFlag = true;

        if (exceptionFlag)
            throw new WrongException("testCreateSharedDurableConsumer_Null_B_SecOff failed");

        jmsContextTCFBindings.close();

    }

    public void testCreateSharedDurableConsumer_Null_TCP_SecOff(
                                                                HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        exceptionFlag = false;
        boolean val1 = false;
        boolean val2 = false;
        JMSContext jmsContextTCFTCP = TCFTCP.createContext();

        try {

            JMSConsumer jmsConsumer = jmsContextTCFTCP
                            .createSharedDurableConsumer(topic, null);

        } catch (JMSRuntimeException ex) {
            ex.printStackTrace();
            val1 = true;
        }

        try {
            JMSConsumer jmsConsumer = jmsContextTCFTCP
                            .createSharedDurableConsumer(topic, "");

        } catch (JMSRuntimeException ex) {
            ex.printStackTrace();
            val2 = true;

        }

        if (!(val1 == true && val2 == true))
            exceptionFlag = true;

        if (exceptionFlag)
            throw new WrongException("testCreateSharedDurableConsumer_Null_TCP_SecOff failed");

        jmsContextTCFTCP.close();

    }

    // 129626_1 JMSConsumer createSharedConsumer(Topic topic,String sharedSubscriptionName)
    // 129626_1_1 Creates a shared non-durable subscription with the specified name on the specified topic (if one does not already exist) and creates a consumer on that
    //subscription. This method creates the non-durable subscription without a message selector.
    //129626_1_4 Non-durable subscription is not persisted and will be deleted (together with any undelivered messages associated with it) when there are no consumers on it. The
    //term "consumer" here means a MessageConsumer or JMSConsumer object in any client.

    public void testCreateSharedNonDurableConsumer_create_B_SecOff(
                                                                   HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        JMSContext jmsContextSender = TCFBindings.createContext();

        JMSContext jmsContextReceiver = TCFBindings.createContext();

        JMSConsumer jmsConsumer = jmsContextReceiver
                        .createSharedConsumer(topic, "SUBID");

        JMSProducer jmsProducer = jmsContextSender.createProducer();

        TextMessage tmsg = jmsContextSender
                        .createTextMessage("This is a test message");

        jmsProducer.send(topic, tmsg);

        jmsContextSender.close();

    }

    public void testCreateSharedNonDurableConsumer_create_TCP_SecOff(
                                                                     HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        JMSContext jmsContextSender = TCFTCP.createContext();

        JMSContext jmsContextReceiver = TCFTCP.createContext();

        JMSConsumer jmsConsumer = jmsContextReceiver
                        .createSharedConsumer(topic, "SUBID");

        JMSProducer jmsProducer = jmsContextSender.createProducer();

        TextMessage tmsg = jmsContextSender
                        .createTextMessage("This is a test message");

        jmsProducer.send(topic, tmsg);

        jmsContextSender.close();

    }

    public void testCreateSharedNonDurableConsumer_consume_B_SecOff(
                                                                    HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContextReceiver = TCFBindings.createContext();
        JMSConsumer jmsConsumer = jmsContextReceiver
                        .createSharedConsumer(topic, "SUBID");

        TextMessage tmsg = (TextMessage) jmsConsumer.receive(30000);

        if (!(tmsg == null))
            exceptionFlag = true;

        jmsConsumer.close();

        // jmsContextReceiver.unsubscribe("SUBID");

        jmsContextReceiver.close();

    }

    public void testCreateSharedNonDurableConsumer_consume_TCP_SecOff(
                                                                      HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContextReceiver = TCFTCP.createContext();
        JMSConsumer jmsConsumer = jmsContextReceiver
                        .createSharedConsumer(topic, "SUBID");

        TextMessage tmsg = (TextMessage) jmsConsumer.receive(30000);

        if (!(tmsg == null))
            exceptionFlag = true;

        jmsConsumer.close();

        // jmsContextReceiver.unsubscribe("SUBID");

        jmsContextReceiver.close();

    }

    // 129626_1_2 If a shared non-durable subscription already exists with the same name and client identifier (if set), and the same topic and message selector has been
    //specified, then this method creates a JMSConsumer on the existing subscription.

    public void testCreateSharedNonDurableConsumer_2Subscribers_B_SecOff(
                                                                         HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContextTCFBindings = TCFBindings.createContext();

        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();

        final ObjectName name = new ObjectName(
                        "WebSphere:feature=wasJmsServer,type=Topic,name=NewTopic1");

        javax.management.openmbean.CompositeData[] obn = (CompositeData[]) mbs
                        .invoke(name, "listSubscriptions", null, null);

        int beforeLength = obn.length;

        JMSConsumer jmsConsumer = jmsContextTCFBindings.createSharedConsumer(topic,
                                                                             "TEST1");

        JMSProducer jmsProducer = jmsContextTCFBindings.createProducer();

        TextMessage tmsg = jmsContextTCFBindings
                        .createTextMessage("This is a test message");

        jmsProducer.send(topic, tmsg);

        tmsg = (TextMessage) jmsConsumer.receive(30000);

        JMSConsumer jmsConsumer2 = jmsContextTCFBindings.createSharedConsumer(
                                                                              topic, "TEST1");

        MBeanServer mbs1 = ManagementFactory.getPlatformMBeanServer();

        final ObjectName name1 = new ObjectName(
                        "WebSphere:feature=wasJmsServer,type=Topic,name=NewTopic1");

        javax.management.openmbean.CompositeData[] obn1 = (CompositeData[]) mbs
                        .invoke(name, "listSubscriptions", null, null);
        int afterLength = 0;
        if (beforeLength > obn1.length)

            afterLength = beforeLength - obn1.length;

        else

            afterLength = obn1.length - beforeLength;

        if (!(afterLength == 1))
            exceptionFlag = true;

        System.out.println("obn.length:" + obn.length);

        if (exceptionFlag)
            throw new WrongException("testCreateSharedNonDurableConsumer_2Subscribers_B_SecOff failed");
        jmsConsumer.close();
        jmsConsumer2.close();
        // jmsContextTCFBindings.unsubscribe("DURATEST");
        jmsContextTCFBindings.close();

    }

    public void testCreateSharedNonDurableConsumer_2Subscribers_TCP_SecOff(
                                                                           HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContextTCFTCP = TCFTCP.createContext();

        JMSConsumer jmsConsumer = jmsContextTCFTCP.createSharedConsumer(topic,
                                                                        "DURATEST");

        JMSProducer jmsProducer = jmsContextTCFTCP.createProducer();

        TextMessage tmsg = jmsContextTCFTCP
                        .createTextMessage("This is a test message");

        jmsProducer.send(topic, tmsg);

        tmsg = (TextMessage) jmsConsumer.receive(30000);

        JMSConsumer jmsConsumer2 = jmsContextTCFTCP.createSharedConsumer(
                                                                         topic, "DURATEST");
        jmsProducer.send(topic, tmsg);

        tmsg = (TextMessage) jmsConsumer2.receive(30000);

        if (!(tmsg != null))
            exceptionFlag = true;

        if (exceptionFlag)
            throw new WrongException("testCreateSharedNonDurableConsumer_2Subscribers_TCP_SecOff failed");
        jmsConsumer.close();
        jmsConsumer2.close();
        // jmsContextTCFTCP.unsubscribe("DURATEST");
        jmsContextTCFTCP.close();
    }

    /*
     * // 129626_1_3 A non-durable shared subscription is used by a client which needs to be able to share the work of receiving messages from a topic subscription amongst multiple
     * consumers. A non-durable shared subscription may therefore have more than one consumer. Each message from the subscription will be delivered to only one of the consumers on
     * that subscription
     * 
     * public void testBasicMDBTopicNonDurable(HttpServletRequest request,
     * HttpServletResponse response) throws Throwable {
     * 
     * TopicConnectionFactory cf1 = (TopicConnectionFactory) new InitialContext()
     * .lookup("java:comp/env/jms/FAT_TCF");
     * 
     * int msgs = 3;
     * 
     * Topic topic = (Topic) new InitialContext()
     * .lookup("java:comp/env/jms/FAT_TOPIC");
     * 
     * JMSContext jmsCont = cf1.createContext();
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
     * public void testBasicMDBTopicNonDurable_TCP(HttpServletRequest request,
     * HttpServletResponse response) throws Throwable {
     * 
     * TopicConnectionFactory cf1 = (TopicConnectionFactory) new InitialContext()
     * .lookup("java:comp/env/jms/FAT_COMMS_TCF");
     * 
     * int msgs = 3;
     * 
     * Topic topic = (Topic) new InitialContext()
     * .lookup("java:comp/env/jms/FAT_TOPIC");
     * 
     * JMSContext jmsCont = cf1.createContext();
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
    // 129626_1_6 If a shared non-durable subscription already exists with the same name and client identifier (if set) but a different topic or message selector value has been
    //specified, and there is a consumer already active (i.e. not closed) on the subscription, then a JMSRuntimeException will be thrown.
    public void testCreateSharedNonDurableConsumer_JRException_B_SecOff(
                                                                        HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        exceptionFlag = false;

        JMSContext jmsContextTCFBindings = TCFBindings.createContext();

        JMSConsumer jmsConsumer = jmsContextTCFBindings.createSharedConsumer(
                                                                             topic, "DURATEST1");

        JMSProducer jmsProducer = jmsContextTCFBindings.createProducer();

        TextMessage tmsg = jmsContextTCFBindings
                        .createTextMessage("This is a test message");

        jmsProducer.send(topic, tmsg);

        tmsg = (TextMessage) jmsConsumer.receive(30000);

        try {

            JMSConsumer jmsConsumer1 = jmsContextTCFBindings
                            .createSharedConsumer(topic1, "DURATEST1");
        } catch (JMSRuntimeException ex) {
            ex.printStackTrace();
            exceptionFlag = true;
        }

        if (!(exceptionFlag))
            throw new WrongException("testCreateSharedNonDurableConsumer_JRException_B_SecOff failed");

        jmsConsumer.close();

    }

    public void testCreateSharedNonDurableConsumer_JRException_TCP_SecOff(
                                                                          HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        exceptionFlag = false;

        JMSContext jmsContextTCFTCP = TCFTCP.createContext();

        JMSConsumer jmsConsumer = jmsContextTCFTCP.createSharedConsumer(
                                                                        topic, "DURATEST1");

        JMSProducer jmsProducer = jmsContextTCFTCP.createProducer();

        TextMessage tmsg = jmsContextTCFTCP
                        .createTextMessage("This is a test message");

        jmsProducer.send(topic, tmsg);

        tmsg = (TextMessage) jmsConsumer.receive(30000);

        try {

            JMSConsumer jmsConsumer1 = jmsContextTCFTCP
                            .createSharedConsumer(topic1, "DURATEST1");
        } catch (JMSRuntimeException ex) {
            ex.printStackTrace();
            exceptionFlag = true;
        }

        if (!(exceptionFlag))
            throw new WrongException("testCreateSharedNonDurableConsumer_JRException_B_SecOff failed");

        jmsConsumer.close();
        jmsContextTCFTCP.close();
    }

    //129626_1_7 There is no restriction on durable subscriptions and shared non-durable subscriptions having the same name and clientId (which may be unset). Such subscriptions
    //would be completely separate.

    public void testCreateSharedNonDurableConsumer_coexist_B_SecOff(
                                                                    HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        exceptionFlag = false;

        JMSContext jmsContextTCFBindings = TCFBindings.createContext();

        JMSConsumer jmsConsumer = jmsContextTCFBindings.createSharedConsumer(
                                                                             topic, "DURATEST1");
        JMSProducer jmsProducer = jmsContextTCFBindings.createProducer();

        TextMessage tmsg = jmsContextTCFBindings
                        .createTextMessage("This is a test message");

        jmsProducer.send(topic, tmsg);

        tmsg = (TextMessage) jmsConsumer.receive(30000);

        JMSConsumer jmsConsumer1 = jmsContextTCFBindings.createDurableConsumer(
                                                                               topic, "DURATEST1");
        jmsProducer = jmsContextTCFBindings.createProducer();

        TextMessage tmsg1 = jmsContextTCFBindings
                        .createTextMessage("This is a test message");

        jmsProducer.send(topic, tmsg1);

        tmsg1 = (TextMessage) jmsConsumer.receive(30000);

        if (!(tmsg != null && tmsg1 != null))
            exceptionFlag = true;

        if (exceptionFlag)
            throw new WrongException("testCreateSharedNonDurableConsumer_coexist_B_SecOff failed");

        jmsConsumer.close();
        jmsConsumer1.close();
        jmsContextTCFBindings.unsubscribe("DURATEST1");
        jmsContextTCFBindings.close();

    }

    public void testCreateSharedNonDurableConsumer_coexist_TCP_SecOff(
                                                                      HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {
        exceptionFlag = false;

        JMSContext jmsContextTCFBindings = TCFBindings.createContext();

        JMSConsumer jmsConsumer = jmsContextTCFBindings.createSharedConsumer(
                                                                             topic, "DURATEST1");
        JMSProducer jmsProducer = jmsContextTCFBindings.createProducer();

        TextMessage tmsg = jmsContextTCFBindings
                        .createTextMessage("This is a test message");

        jmsProducer.send(topic, tmsg);

        tmsg = (TextMessage) jmsConsumer.receive(30000);

        JMSConsumer jmsConsumer1 = jmsContextTCFBindings.createDurableConsumer(
                                                                               topic, "DURATEST1");
        jmsProducer = jmsContextTCFBindings.createProducer();

        TextMessage tmsg1 = jmsContextTCFBindings
                        .createTextMessage("This is a test message");

        jmsProducer.send(topic, tmsg1);

        tmsg1 = (TextMessage) jmsConsumer.receive(30000);

        if (!(tmsg != null && tmsg1 != null))
            exceptionFlag = true;

        if (exceptionFlag)
            throw new WrongException("testCreateSharedNonDurableConsumer_coexist_TCP_SecOff failed");

        jmsConsumer.close();
        jmsConsumer1.close();

        jmsContextTCFBindings.unsubscribe("DURATEST1");
        jmsContextTCFBindings.close();
    }

    // 129626_1_9 InvalidDestinationRuntimeException - if an invalid topic is specified.
    public void testCreateSharedNonDurableConsumer_InvalidDestination_B_SecOff(
                                                                               HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContextTCFBindings = TCFBindings.createContext();

        try {

            JMSConsumer jmsConsumer = jmsContextTCFBindings
                            .createSharedConsumer(null, "DURATEST1");

        } catch (InvalidDestinationRuntimeException ex) {
            ex.printStackTrace();
            exceptionFlag = true;
        }

        if (!(exceptionFlag))
            throw new WrongException("testCreateSharedNonDurableConsumer_InvalidDestination_B_SecOff failed");

        jmsContextTCFBindings.close();
    }

    public void testCreateSharedNonDurableConsumer_InvalidDestination_TCP_SecOff(
                                                                                 HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContextTCFTCP = TCFTCP.createContext();

        try {

            JMSConsumer jmsConsumer = jmsContextTCFTCP
                            .createSharedConsumer(null, "DURATEST1");

        } catch (InvalidDestinationRuntimeException ex) {
            ex.printStackTrace();
            exceptionFlag = true;
        }

        if (!(exceptionFlag))
            throw new WrongException("testCreateSharedNonDurableConsumer_InvalidDestination_TCP_SecOff failed");

        jmsContextTCFTCP.close();

    }

    //Defect 174691

    public void testCreateSharedConsumer_Qsession_B_SecOff(
                                                           HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {
        exceptionFlag = false;

        boolean flag1 = false;
        boolean flag2 = false;
        boolean flag3 = false;
        boolean flag4 = false;
        boolean flag5 = false;
        boolean flag6 = false;
        boolean flag7 = false;

        QueueConnection qconn = QCFBindings.createQueueConnection();
        qconn.start();
        QueueSession qsession = qconn.createQueueSession(false, QueueSession.AUTO_ACKNOWLEDGE);

        try {
            qsession.createSharedConsumer(topic, "SUBID");
        } catch (javax.jms.IllegalStateException ex) {
            ex.printStackTrace();
            flag1 = true;
        }
        try {
            qsession.createDurableSubscriber(topic, "SUBID");
        } catch (javax.jms.IllegalStateException ex) {
            ex.printStackTrace();
            flag2 = true;
        }

        try {
            qsession.createDurableConsumer(topic, "SUBID");
        } catch (javax.jms.IllegalStateException ex) {
            ex.printStackTrace();
            flag3 = true;
        }

        try {
            qsession.createSharedDurableConsumer(topic, "SUBID");
        } catch (javax.jms.IllegalStateException ex) {
            ex.printStackTrace();
            flag4 = true;
        }

        try {
            qsession.createTemporaryTopic();
        } catch (javax.jms.IllegalStateException ex) {
            ex.printStackTrace();
            flag5 = true;
        }

        try {
            qsession.createTopic("JustCreated");
        } catch (javax.jms.IllegalStateException ex) {
            ex.printStackTrace();
            flag6 = true;
        }

        try {
            qsession.unsubscribe("SUBID");
        } catch (javax.jms.IllegalStateException ex) {
            ex.printStackTrace();
            flag7 = true;
        }

        if (flag1 == true && flag2 == true && flag3 == true && flag4 == true && flag5 == true && flag6 == true && flag7 == true)
            exceptionFlag = true;

        qconn.close();
        if (!(exceptionFlag))
            throw new WrongException("testCreateSharedConsumer_InvalidDestination_TCP_SecOff failed");

    }

    //Defect 174713
    public void testUnsubscribeInvalidSID_Tsession_B_SecOff(
                                                            HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {
        exceptionFlag = false;
        TopicConnectionFactory tcf = (TopicConnectionFactory) new InitialContext()
                        .lookup("java:comp/env/eis/tcf2");
        TopicConnection tconn = tcf.createTopicConnection();
        tconn.start();
        TopicSession tsession = tconn.createTopicSession(false, TopicSession.AUTO_ACKNOWLEDGE);

        try {
            tsession.unsubscribe("DummySID");
        } catch (InvalidDestinationException ex) {
            ex.printStackTrace();
            exceptionFlag = true;
        }

        tconn.close();
        if (!(exceptionFlag))
            throw new WrongException("testUnsubscribeInvalidSID_Tsession_B_SecOff failed");

    }

    public int getMessageCount(QueueBrowser qb) throws JMSException {

        Enumeration e = qb.getEnumeration();

        int numMsgs = 0;
        // count number of messages
        while (e.hasMoreElements()) {
            e.nextElement();
            numMsgs++;
        }

        return numMsgs;
    }

    public static QueueConnectionFactory getQCFBindings()
                    throws NamingException {

        QueueConnectionFactory cf1 = (QueueConnectionFactory) new InitialContext()
                        .lookup("java:comp/env/jndi_JMS_BASE_QCF");

        return cf1;

    }

    public static QueueConnectionFactory getQCFTCP() throws NamingException {

        QueueConnectionFactory cf1 = (QueueConnectionFactory) new InitialContext()
                        .lookup("java:comp/env/jndi_JMS_BASE_QCF1");

        return cf1;

    }

    public static TopicConnectionFactory getTCFBindings()
                    throws NamingException {

        TopicConnectionFactory tcf1 = (TopicConnectionFactory) new InitialContext()
                        .lookup("java:comp/env/eis/tcf");

        return tcf1;

    }

    public static TopicConnectionFactory getTCFTCP() throws NamingException {

        TopicConnectionFactory tcf1 = (TopicConnectionFactory) new InitialContext()
                        .lookup("java:comp/env/eis/tcf1");

        return tcf1;

    }

    public class WrongException extends Exception {
        String str;

        public WrongException(String str) {
            this.str = str;
            System.out.println(" <ERROR> " + str + " </ERROR>");
        }
    }

}
