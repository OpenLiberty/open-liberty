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
package sharedsubscriptionwithmsgsel.web;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.lang.reflect.InvocationTargetException;

import javax.jms.InvalidDestinationRuntimeException;
import javax.jms.InvalidSelectorRuntimeException;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.JMSProducer;
import javax.jms.JMSRuntimeException;
import javax.jms.Queue;
import javax.jms.QueueConnectionFactory;
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
public class SharedSubscriptionWithMsgSelServlet extends HttpServlet {

    public static QueueConnectionFactory jmsQCFBindings;
    public static QueueConnectionFactory jmsQCFTCP;
    public static TopicConnectionFactory jmsTCFBindings;
    public static TopicConnectionFactory jmsTCFTCP;
    public static Topic jmsTopic;
    public static Topic jmsTopic1;
    public static Topic jmsExpiryTopic;
    public static Queue jmsQueue;
    public static JMSContext jmsContext;
    public static JMSConsumer jmsConsumer;
    public static JMSProducer jmsProducer;

    public static boolean exceptionFlag;

    @Override
    public void init() throws ServletException {
        // TODO Auto-generated method stub

        super.init();
        try {
            jmsTCFBindings = getTCFBindings();
            jmsTCFTCP = getTCFTCP();
            jmsTopic = getTopic("eis/topic1");
            jmsExpiryTopic = getTopic("eis/topic");
            jmsTopic1 = getTopic("eis/topic2");

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
        final TraceComponent tc = Tr.register(SharedSubscriptionWithMsgSelServlet.class); // injection
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

    public void testCreateSharedDurableConsumerWithMsgSelector_create(HttpServletRequest request,
                                                                      HttpServletResponse response) throws Throwable {

        JMSContext jmsContextSender = jmsTCFBindings.createContext();
        JMSContext jmsContextReceiver = jmsTCFBindings.createContext();
        jmsConsumer = jmsContextReceiver.createSharedDurableConsumer(jmsTopic, "SUBID", "Company = 'IBM'");

        jmsProducer = jmsContextSender.createProducer();

        TextMessage tmsg = jmsContextSender.createTextMessage("Hello");
        tmsg.setStringProperty("Company", "IBM");
        jmsProducer.send(jmsTopic, tmsg);
        System.out.println("Message being sent is :" + tmsg);

        jmsContextSender.close();

    }

    public void testCreateSharedDurableConsumerWithMsgSelector_consume(HttpServletRequest request,
                                                                       HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContextReceiver = jmsTCFBindings.createContext();
        JMSConsumer jmsConsumer = jmsContextReceiver.createSharedDurableConsumer(jmsTopic, "SUBID", "Company = 'IBM'");

        TextMessage tmsg = (TextMessage) jmsConsumer.receive(30000);
        System.out.println("Message Received is :" + tmsg);
        if (!(tmsg != null)) {
            System.out.println("testSharedDurConsumerWithMsgSelector_B message is not received");
            exceptionFlag = true;
        }
        jmsConsumer.close();
        jmsContextReceiver.unsubscribe("SUBID");

        jmsContextReceiver.close();

        if (exceptionFlag)
            throw new WrongException("testCreateSharedDurableConsumerWithMsgSelector_consume failed: expected message was not received");

    }

    public void testCreateSharedDurableConsumerWithMsgSelector_create_TCP(HttpServletRequest request,
                                                                          HttpServletResponse response) throws Throwable {

        JMSContext jmsContextSender = jmsTCFTCP.createContext();
        JMSContext jmsContextReceiver = jmsTCFTCP.createContext();
        jmsConsumer = jmsContextReceiver.createSharedDurableConsumer(jmsTopic, "SUBID1", "Company = 'IBM'");

        jmsProducer = jmsContextSender.createProducer();

        TextMessage tmsg = jmsContextSender.createTextMessage("This is a test message");
        tmsg.setStringProperty("Company", "IBM");
        jmsProducer.send(jmsTopic, tmsg);
        System.out.println("Message being sent is :" + tmsg);

        jmsContextSender.close();
    }

    public void testCreateSharedDurableConsumerWithMsgSelector_consume_TCP(HttpServletRequest request,
                                                                           HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContextReceiver = jmsTCFTCP.createContext();
        JMSConsumer jmsConsumer = jmsContextReceiver.createSharedDurableConsumer(jmsTopic, "SUBID1", "Company = 'IBM'");

        TextMessage tmsg = (TextMessage) jmsConsumer.receive(30000);
        System.out.println("Message Received is :" + tmsg);
        if (!(tmsg != null)) {
            System.out.println("testSharedDurConsumerWithMsgSelector_TCP message not received");
            exceptionFlag = true;
        }
        jmsConsumer.close();
        jmsContextReceiver.unsubscribe("SUBID1");

        jmsContextReceiver.close();

        if (exceptionFlag)
            throw new WrongException("testCreateSharedDurableConsumerWithMsgSelector_consume_TCP failed: expected message was not received");

    }

    public void testCreateSharedDurableConsumerWithMsgSelector_create_Expiry(
                                                                             HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        JMSContext jmsContextSender = jmsTCFBindings.createContext();
        JMSContext jmsContextReceiver = jmsTCFBindings.createContext();
        JMSConsumer jmsConsumer = jmsContextReceiver.createSharedDurableConsumer(jmsExpiryTopic, "SUBID3", "Company = 'IBM'");

        JMSProducer jmsProducer = jmsContextSender.createProducer();

        TextMessage tmsg = jmsContextSender.createTextMessage("testCreateSharedDurableConsumerWithMsgSelector_create_Expiry");
        tmsg.setStringProperty("Company", "IBM");
        System.out.println("Reached here in create  2");
        jmsProducer.send(jmsExpiryTopic, tmsg);

        jmsContextSender.close();

    }

    public void testCreateSharedDurableConsumerWithMsgSelector_consumeAfterExpiry(HttpServletRequest request,
                                                                                  HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContextReceiver = jmsTCFBindings.createContext();
        JMSConsumer jmsConsumer = jmsContextReceiver.createSharedDurableConsumer(jmsExpiryTopic, "SUBID3", "Company = 'IBM'");

        TextMessage tmsg = (TextMessage) jmsConsumer.receive(30000);
        System.out.println("Message Received is :" + tmsg);
        if (!(tmsg == null))
            exceptionFlag = true;
        jmsConsumer.close();
        jmsContextReceiver.unsubscribe("SUBID3");

        jmsContextReceiver.close();

        if (exceptionFlag)
            throw new WrongException("testCreateSharedDurableConsumerWithMsgSelector_consumeAfterExpiry failed");

    }

    public void testCreateSharedDurableConsumerWithMsgSelector_create_Expiry_TCP(
                                                                                 HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        JMSContext jmsContextSender = jmsTCFTCP.createContext();
        JMSContext jmsContextReceiver = jmsTCFTCP.createContext();
        JMSConsumer jmsConsumer = jmsContextReceiver.createSharedDurableConsumer(jmsExpiryTopic, "SUBID4", "Company = 'IBM'");

        JMSProducer jmsProducer = jmsContextSender.createProducer();

        TextMessage tmsg = jmsContextSender.createTextMessage("testCreateSharedDurableConsumerWithMsgSelector_create_Expiry_TCP");
        tmsg.setStringProperty("Company", "IBM");

        jmsProducer.send(jmsExpiryTopic, tmsg);

        jmsContextSender.close();

    }

    public void testCreateSharedDurableConsumerWithMsgSelector_consumeAfterExpiry_TCP(HttpServletRequest request,
                                                                                      HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContextReceiver = jmsTCFTCP.createContext();
        JMSConsumer jmsConsumer = jmsContextReceiver.createSharedDurableConsumer(jmsExpiryTopic, "SUBID4", "Company = 'IBM'");

        TextMessage tmsg = (TextMessage) jmsConsumer.receive(30000);
        System.out.println("Message Received is :" + tmsg);
        if (!(tmsg == null))
            exceptionFlag = true;
        jmsConsumer.close();
        jmsContextReceiver.unsubscribe("SUBID4");

        jmsContextReceiver.close();

        if (exceptionFlag)
            throw new WrongException("testCreateSharedDurableConsumerWithMsgSelector_consumeAfterExpiry_TCP failed");

    }

    public void testCreateSharedDurableConsumerWithMsgSelector_unsubscribe(HttpServletRequest request,
                                                                           HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        jmsContext = jmsTCFBindings.createContext();

        JMSConsumer jmsConsumer = jmsContext.createSharedDurableConsumer(jmsTopic, "UNSUB", "Company = 'IBM'");

        JMSProducer jmsProducer = jmsContext.createProducer();

        TextMessage tmsg = jmsContext.createTextMessage("This is a test message");
        tmsg.setStringProperty("Company", "IBM");
        jmsProducer.send(jmsTopic, tmsg);

        tmsg = (TextMessage) jmsConsumer.receive(30000);
        System.out.println("testCreateSharedDurableConsumer_unsubscribe: Message Received is :" + tmsg);

        jmsConsumer.close();

        jmsContext.unsubscribe("UNSUB");

        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();

        final ObjectName name = new ObjectName("WebSphere:feature=wasJmsServer,type=Subscriber,name=clientID##UNSUB");
        System.out.println("initialized MBeanServer and Object");
        System.out.println("object created");

        try {
            String obn = (String) mbs.getAttribute(name, "Id");

        } catch (InstanceNotFoundException ex) {
            ex.printStackTrace();
            //  setException(true);
            System.out.println("InstanceNotFoundException seen in testCreateSharedDurableConsumerWithMsgSelector_unsubscribe_B as expected");
            exceptionFlag = true;
        }

        if (jmsContext != null)
            jmsContext.close();
        if (!exceptionFlag)
            throw new WrongException("testCreateSharedDurableConsumerWithMsgSelector_unsubscribe failed: Expected exception not seen");
    }

    public void testCreateSharedDurableConsumerWithMsgSelector_unsubscribe_TCP(HttpServletRequest request,
                                                                               HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        jmsContext = jmsTCFTCP.createContext();

        JMSConsumer jmsConsumer = jmsContext.createSharedDurableConsumer(jmsTopic, "DURATEST", "Company = 'IBM'");

        JMSProducer jmsProducer = jmsContext.createProducer();

        TextMessage tmsg = jmsContext.createTextMessage("This is a test message");
        tmsg.setStringProperty("Company", "IBM");
        jmsProducer.send(jmsTopic, tmsg);

        tmsg = (TextMessage) jmsConsumer.receive(30000);
        System.out.println("Message Received is :" + tmsg);

        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();

        final ObjectName name = new ObjectName("WebSphere:feature=wasJmsServer,type=Subscriber,name=clientID##DURATEST");
        System.out.println("initialized MBeanServer and Object");
        System.out.println("object created");
        System.out.println("Exception value is true");

        try {
            String obn = (String) mbs.getAttribute(name, "Id");

        } catch (InstanceNotFoundException ex) {
            ex.printStackTrace();
            System.out.println("InstanceNotFoundException seen in testCreateSharedDurableConsumerWithMsgSelector_unsubscribe_TCP as expected");
            exceptionFlag = true;
        }

        if (jmsContext != null)
            jmsContext.close();

        if (!exceptionFlag)
            throw new WrongException("testCreateSharedDurableConsumerWithMsgSelector_unsubscribe_TCP failed: Expected exception not seen");

    }

    public void testCreateSharedDurableConsumerWithMsgSelector_2Subscribers(HttpServletRequest request,
                                                                            HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        jmsContext = jmsTCFBindings.createContext();

        jmsConsumer = jmsContext.createSharedDurableConsumer(jmsTopic, "DURATEST", "Company = 'IBM'");
        jmsProducer = jmsContext.createProducer();

        TextMessage tmsg = jmsContext.createTextMessage("This is a test message");
        tmsg.setStringProperty("Company", "IBM");
        jmsProducer.send(jmsTopic, tmsg);

        tmsg = (TextMessage) jmsConsumer.receive(30000);
        System.out.println("Message Received is :" + tmsg);

        JMSConsumer jmsConsumer2 = jmsContext.createSharedDurableConsumer(jmsTopic, "DURATEST", "Company = 'IBM'");

        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();

        final ObjectName name = new ObjectName("WebSphere:feature=wasJmsServer,type=Topic,name=NewTopic1");
        System.out.println("initialized MBeanServer and Object");
        System.out.println("object created");

        javax.management.openmbean.CompositeData[] obn = (CompositeData[]) mbs
                        .invoke(name, "listSubscriptions", null, null);

        System.out.println("Length is " + obn.length);
        if (obn.length == 1) {
            System.out.println("testCreateSharedDurableConsumer_2Subscribers: Number of Subscriptions is 1 as expected");
        } else
            exceptionFlag = true;

        jmsConsumer.close();
        jmsConsumer2.close();
        jmsContext.unsubscribe("DURATEST");

        if (jmsContext != null)
            jmsContext.close();

        if (exceptionFlag)
            throw new WrongException("testCreateSharedDurableConsumerWithMsgSelector_2Subscribers failed");

    }

    public void testCreateSharedDurableConsumerWithMsgSelector_2Subscribers_TCP(HttpServletRequest request,
                                                                                HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        jmsContext = jmsTCFTCP.createContext();

        jmsConsumer = jmsContext.createSharedDurableConsumer(jmsTopic, "DURATEST", "Company = 'IBM'");
        jmsProducer = jmsContext.createProducer();

        TextMessage tmsg = jmsContext.createTextMessage("This is a test message");
        tmsg.setStringProperty("Company", "IBM");
        jmsProducer.send(jmsTopic, tmsg);

        tmsg = (TextMessage) jmsConsumer.receive(30000);
        System.out.println("Message Received is :" + tmsg);

        JMSConsumer jmsConsumer2 = jmsContext.createSharedDurableConsumer(jmsTopic, "DURATEST", "Company = 'IBM'");

        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();

        final ObjectName name = new ObjectName("WebSphere:feature=wasJmsServer,type=Topic,name=NewTopic1");
        System.out.println("initialized MBeanServer and Object");
        System.out.println("object created");

        javax.management.openmbean.CompositeData[] obn = (CompositeData[]) mbs
                        .invoke(name, "listSubscriptions", null, null);

        System.out.println("Length is " + obn.length);
        if (obn.length == 1) {
            System.out.println("testCreateSharedDurableConsumerWithMsgSelector_2Subscribers_TCP: Number of Subscriptions is 1 as expected");
        } else
            exceptionFlag = true;

        jmsConsumer.close();
        jmsConsumer2.close();
        jmsContext.unsubscribe("DURATEST");

        if (jmsContext != null)
            jmsContext.close();

        if (exceptionFlag)
            throw new WrongException("testCreateSharedDurableConsumerWithMsgSelector_2Subscribers_TCP failed");

    }

    public void testCreateSharedDurableConsumerWithMsgSelector_2SubscribersDiffTopic(HttpServletRequest request,
                                                                                     HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        jmsContext = jmsTCFBindings.createContext();

        jmsConsumer = jmsContext.createSharedDurableConsumer(jmsTopic, "2SUBS", "Company = 'IBM'");

        jmsProducer = jmsContext.createProducer();

        TextMessage tmsg = jmsContext.createTextMessage("This is a test message");
        tmsg.setStringProperty("Company", "IBM");
        jmsProducer.send(jmsTopic, tmsg);

        tmsg = (TextMessage) jmsConsumer.receive(30000);
        System.out.println("Message Received is :" + tmsg);

        MBeanServer mbs1 = ManagementFactory.getPlatformMBeanServer();
        final ObjectName name1 = new ObjectName("WebSphere:feature=wasJmsServer,type=Topic,name=NewTopic1");

        javax.management.openmbean.CompositeData[] obn1 = (CompositeData[]) mbs1
                        .invoke(name1, "listSubscriptions", null, null);

        System.out.println("Length after 1 consumer is " + obn1.length);

        jmsConsumer.close();
        JMSConsumer jmsConsumer2 = jmsContext.createSharedDurableConsumer(jmsTopic1, "2SUBS", "Company = 'IBM'");

        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();

        final ObjectName name = new ObjectName("WebSphere:feature=wasJmsServer,type=Topic,name=NewTopic2");

        javax.management.openmbean.CompositeData[] obn = (CompositeData[]) mbs
                        .invoke(name, "listSubscriptions", null, null);

        System.out.println("Length after 2 consumer is " + obn.length);
        if (obn.length == 1) {
            System.out.println("testCreateSharedDurableConsumer_2SubscribersDiffTopic: Number of Subscriptions is 1 as expected");
        } else
            exceptionFlag = true;

        jmsConsumer2.close();
        jmsContext.unsubscribe("2SUBS");

        if (jmsContext != null)
            jmsContext.close();

        if (exceptionFlag)
            throw new WrongException("testCreateSharedDurableConsumerWithMsgSelector_2SubscribersDiffTopic failed");

    }

    public void testCreateSharedDurableConsumerWithMsgSelector_2SubscribersDiffTopic_TCP(HttpServletRequest request,
                                                                                         HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        jmsContext = jmsTCFTCP.createContext();

        jmsConsumer = jmsContext.createSharedDurableConsumer(jmsTopic, "DURATEST", "Company = 'IBM'");

        jmsProducer = jmsContext.createProducer();

        TextMessage tmsg = jmsContext.createTextMessage("This is a test message");
        tmsg.setStringProperty("Company", "IBM");
        jmsProducer.send(jmsTopic, tmsg);

        tmsg = (TextMessage) jmsConsumer.receive(30000);
        System.out.println("Message Received is :" + tmsg);

        MBeanServer mbs1 = ManagementFactory.getPlatformMBeanServer();
        final ObjectName name1 = new ObjectName("WebSphere:feature=wasJmsServer,type=Topic,name=NewTopic1");
        System.out.println("initialized MBeanServer and Object");
        System.out.println("object created");

        javax.management.openmbean.CompositeData[] obn1 = (CompositeData[]) mbs1
                        .invoke(name1, "listSubscriptions", null, null);

        System.out.println("Length after 1 consumer is " + obn1.length);

        jmsConsumer.close();
        JMSConsumer jmsConsumer2 = jmsContext.createSharedDurableConsumer(jmsTopic1, "DURATEST", "Company = 'IBM'");

        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();

        final ObjectName name = new ObjectName("WebSphere:feature=wasJmsServer,type=Topic,name=NewTopic2");
        System.out.println("initialized MBeanServer and Object");
        System.out.println("object created");

        javax.management.openmbean.CompositeData[] obn = (CompositeData[]) mbs
                        .invoke(name, "listSubscriptions", null, null);

        System.out.println("Length after 2 consumer is " + obn.length);
        if (obn.length == 1) {
            System.out.println("testCreateSharedDurableConsumerWithMsgSelector_2SubscribersDiffTopic_TCP: Number of Subscriptions is 1 as expected");
        } else
            exceptionFlag = true;

        jmsConsumer2.close();
        jmsContext.unsubscribe("DURATEST");

        if (jmsContext != null)
            jmsContext.close();

        if (exceptionFlag)
            throw new WrongException("testCreateSharedDurableConsumerWithMsgSelector_2SubscribersDiffTopic_TCP failed");

    }

    public void testCreateSharedDurableConsumerWithMsgSelector_InvalidMsgSelector(HttpServletRequest request,
                                                                                  HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContextReceiver = jmsTCFBindings.createContext();
        try {
            jmsConsumer = jmsContextReceiver.createSharedDurableConsumer(jmsTopic, "SUBID", "BAD SELECTOR");
        } catch (InvalidSelectorRuntimeException e) {
            System.out.println("Expected InvalidSelectorRuntimeException seen in testCreateSharedDurableConsumerWithMsgSelector_InvalidMsgSelector");
            exceptionFlag = true;
        }

        if (!exceptionFlag)
            throw new WrongException("testCreateSharedDurableConsumerWithMsgSelector_InvalidMsgSelector failed");
        jmsContextReceiver.close();

    }

    public void testCreateSharedDurableConsumerWithMsgSelector_InvalidMsgSelector_TCP(HttpServletRequest request,
                                                                                      HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContextReceiver = jmsTCFBindings.createContext();
        try {
            jmsConsumer = jmsContextReceiver.createSharedDurableConsumer(jmsTopic, "SUBID", "BAD SELECTOR");
        } catch (InvalidSelectorRuntimeException e) {
            System.out.println("Expected InvalidSelectorRuntimeException seen in testCreateSharedDurableConsumerWithMsgSelector_InvalidMsgSelector_TCP");
            exceptionFlag = true;
        }

        if (!exceptionFlag)
            throw new WrongException("testCreateSharedDurableConsumerWithMsgSelector_InvalidMsgSelector_TCP failed");
        jmsContextReceiver.close();
    }

    public void testCreateSharedDurableConsumerWithMsgSelector_InvalidDestination(HttpServletRequest request,
                                                                                  HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        jmsContext = jmsTCFBindings.createContext();

        try {
            jmsConsumer = jmsContext.createSharedDurableConsumer(null, "DURATEST1", "Company = 'IBM'");

        } catch (InvalidDestinationRuntimeException ex) {
            ex.printStackTrace();
            System.out.println("Excpected InvalidDestinationRuntimeException seen in testCreateSharedDurableConsumer_InvalidDestination");
            exceptionFlag = true;
        }
        jmsContext.close();

        if (!exceptionFlag)
            throw new WrongException("testCreateSharedDurableConsumerWithMsgSelector_InvalidDestination failed");

    }

    public void testCreateSharedDurableConsumerWithMsgSelector_InvalidDestination_TCP(HttpServletRequest request,
                                                                                      HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        jmsContext = jmsTCFTCP.createContext();

        try {
            jmsConsumer = jmsContext.createSharedDurableConsumer(null, "DURATEST1", "Company = 'IBM'");

        } catch (InvalidDestinationRuntimeException ex) {
            ex.printStackTrace();
            System.out.println("Excpected InvalidDestinationRuntimeException seen in testCreateSharedDurableConsumerWithMsgSelector_InvalidDestination_TCP");
            exceptionFlag = true;
        }
        jmsContext.close();

        if (!exceptionFlag)
            throw new WrongException("testCreateSharedDurableConsumerWithMsgSelector_InvalidDestination failed");

    }

    public void testCreateSharedDurableConsumerWithMsgSelector_Null(HttpServletRequest request,
                                                                    HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        boolean val1 = false;
        boolean val2 = false;
        jmsContext = jmsTCFBindings.createContext();
        try {

            JMSConsumer jmsConsumer = jmsContext.createSharedDurableConsumer(jmsTopic, null, "Company = 'IBM'");

        } catch (JMSRuntimeException ex) {
            ex.printStackTrace();
            System.out.println("Excpected JMSRuntimeException seen in when subscription name is null in testCreateSharedDurableConsumer_Null");
            val1 = true;
        }

        try {
            JMSConsumer jmsConsumer = jmsContext.createSharedDurableConsumer(jmsTopic, "", "Company = 'IBM'");

        } catch (JMSRuntimeException ex) {
            ex.printStackTrace();
            System.out.println("Excpected JMSRuntimeException seen in when subscription name is empty string in testCreateSharedDurableConsumer_Null");
            val2 = true;
        }

        jmsContext.close();
        if (!(val1 && val2))
            exceptionFlag = true;

        if (exceptionFlag)
            throw new WrongException("testCreateSharedDurableConsumerWithMsgSelector_Null_B failed");
    }

    public void testCreateSharedDurableConsumerWithMsgSelector_Null_TCP(HttpServletRequest request,
                                                                        HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        boolean val1 = false;
        boolean val2 = false;
        jmsContext = jmsTCFTCP.createContext();
        try {

            JMSConsumer jmsConsumer = jmsContext.createSharedDurableConsumer(jmsTopic, null, "Company = 'IBM'");

        } catch (JMSRuntimeException ex) {
            ex.printStackTrace();
            System.out.println("Excpected JMSRuntimeException seen in when subscription name is null in testCreateSharedDurableConsumer_Null_TCP");
            val1 = true;
        }

        try {
            JMSConsumer jmsConsumer = jmsContext.createSharedDurableConsumer(jmsTopic, "", "Company = 'IBM'");

        } catch (JMSRuntimeException ex) {
            ex.printStackTrace();
            System.out.println("Excpected JMSRuntimeException seen in when subscription name is empty string in testCreateSharedDurableConsumer_Null_TCP");
            val2 = true;
        }

        jmsContext.close();
        if (!(val1 && val2))
            exceptionFlag = true;

        if (exceptionFlag)
            throw new WrongException("testCreateSharedDurableConsumerWithMsgSelector_Null_TCP failed");

    }

    public void testCreateSharedDurableConsumerWithMsgSelector_JRException(
                                                                           HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        exceptionFlag = false;
        jmsContext = jmsTCFBindings.createContext();

        jmsConsumer = jmsContext.createSharedDurableConsumer(jmsTopic, "DURATEST1", "Company = 'IBM'");

        jmsProducer = jmsContext.createProducer();

        TextMessage tmsg = jmsContext
                        .createTextMessage("This is a test message");
        tmsg.setStringProperty("Company", "IBM");
        jmsProducer.send(jmsTopic, tmsg);

        tmsg = (TextMessage) jmsConsumer.receive(30000);
        System.out.println("Message Received is :" + tmsg);

        try {
            JMSConsumer jmsConsumer1 = jmsContext
                            .createSharedDurableConsumer(jmsTopic1, "DURATEST1", "Company = 'IBM'");
        } catch (JMSRuntimeException ex) {
            ex.printStackTrace();
            System.out.println("Expected JMSRuntimeException seen in testCreateSharedDurableConsumerWithMsgSelector_JRException_B");
            exceptionFlag = true;
        }
        jmsConsumer.close();
        jmsContext.unsubscribe("DURATEST1");
        jmsContext.close();

        if (!exceptionFlag)
            throw new WrongException("testCreateSharedDurableConsumerWithMsgSelector_JRException failed: expected exception is not seen");

    }

    public void testCreateSharedDurableConsumerWithMsgSelector_JRException_TCP(
                                                                               HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        exceptionFlag = false;
        jmsContext = jmsTCFTCP.createContext();

        jmsConsumer = jmsContext.createSharedDurableConsumer(jmsTopic, "DURATEST1", "Company = 'IBM'");

        jmsProducer = jmsContext.createProducer();

        TextMessage tmsg = jmsContext
                        .createTextMessage("This is a test message");
        tmsg.setStringProperty("Company", "IBM");
        jmsProducer.send(jmsTopic, tmsg);

        tmsg = (TextMessage) jmsConsumer.receive(30000);
        System.out.println("Message Received is :" + tmsg);

        try {
            JMSConsumer jmsConsumer1 = jmsContext
                            .createSharedDurableConsumer(jmsTopic1, "DURATEST1", "Company = 'IBM'");
        } catch (JMSRuntimeException ex) {
            ex.printStackTrace();
            System.out.println("Expected JMSRuntimeException seen in testCreateSharedDurableConsumerWithMsgSelector_JRException_TCP");
            exceptionFlag = true;
        }
        jmsConsumer.close();
        jmsContext.unsubscribe("DURATEST1");
        jmsContext.close();

        if (!exceptionFlag)
            throw new WrongException("testCreateSharedDurableConsumerWithMsgSelector_JRException_TCP failed: expected exception is not seen");

    }

    // 129623_1_10 A shared durable subscription and an unshared durable
    // subscription may not have the same name and client identifier (if set).
    // If an unshared durable subscription already exists with the same name and
    // client identifier (if set) then a JMSRuntimeException is thrown.

    public void testCreateSharedDurableUndurableConsumerWithMsgSelector_JRException(
                                                                                    HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        exceptionFlag = false;
        jmsContext = jmsTCFBindings.createContext();

        JMSConsumer jmsConsumer = jmsContext.createSharedDurableConsumer(jmsTopic,
                                                                         "DURATEST1", "Company = 'IBM'");

        JMSProducer jmsProducer = jmsContext.createProducer();

        TextMessage tmsg = jmsContext
                        .createTextMessage("This is a test message");
        tmsg.setStringProperty("Company", "IBM");
        jmsProducer.send(jmsTopic, tmsg);

        tmsg = (TextMessage) jmsConsumer.receive(30000);
        System.out.println("Message Received is :" + tmsg);

        try {

            JMSConsumer jmsConsumer1 = jmsContext.createDurableConsumer(
                                                                        jmsTopic, "DURATEST1");
        } catch (JMSRuntimeException ex) {
            ex.printStackTrace();
            System.out.println("Expected JMSRuntimeException seen in testCreateSharedDurableUndurableConsumerWithMsgSelector_JRException_B");
            exceptionFlag = true;
        }
        jmsConsumer.close();
        jmsContext.unsubscribe("DURATEST1");
        jmsContext.close();

        if (!exceptionFlag)
            throw new WrongException("testCreateSharedDurableUndurableConsumerWithMsgSelector_JRException failed");

    }

    public void testCreateSharedDurableUndurableConsumerWithMsgSelector_JRException_TCP(
                                                                                        HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        exceptionFlag = false;
        jmsContext = jmsTCFTCP.createContext();

        JMSConsumer jmsConsumer = jmsContext.createSharedDurableConsumer(jmsTopic,
                                                                         "DURATEST1", "Company = 'IBM'");

        JMSProducer jmsProducer = jmsContext.createProducer();

        TextMessage tmsg = jmsContext
                        .createTextMessage("This is a test message");
        tmsg.setStringProperty("Company", "IBM");
        jmsProducer.send(jmsTopic, tmsg);

        tmsg = (TextMessage) jmsConsumer.receive(30000);
        System.out.println("Message Received is :" + tmsg);

        try {

            JMSConsumer jmsConsumer1 = jmsContext.createDurableConsumer(
                                                                        jmsTopic, "DURATEST1");
        } catch (JMSRuntimeException ex) {
            ex.printStackTrace();
            System.out.println("Expected JMSRuntimeException seen in testCreateSharedDurableUndurableConsumerWithMsgSelector_JRException_TCP");
            exceptionFlag = true;
        }
        jmsConsumer.close();
        jmsContext.unsubscribe("DURATEST1");
        jmsContext.close();

        if (!exceptionFlag)
            throw new WrongException("testCreateSharedDurableUndurableConsumerWithMsgSelector_JRException_TCP failed");

    }

    public void testCreateSharedNonDurableConsumerWithMsgSelector_create(
                                                                         HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        JMSContext jmsContextSender = jmsTCFBindings.createContext();

        JMSContext jmsContextReceiver = jmsTCFBindings.createContext();

        JMSConsumer jmsConsumer = jmsContextReceiver
                        .createSharedConsumer(jmsTopic, "SUBID", "Company = 'IBM'");

        JMSProducer jmsProducer = jmsContextSender.createProducer();

        TextMessage tmsg = jmsContextSender
                        .createTextMessage("testCreateSharedNonDurableConsumerWithMsgSelector_create");
        tmsg.setStringProperty("Company", "IBM");
        jmsProducer.send(jmsTopic, tmsg);
        jmsContextSender.close();

    }

    public void testCreateSharedNonDurableConsumerWithMsgSelector_create_TCP(
                                                                             HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        JMSContext jmsContextSender = jmsTCFTCP.createContext();

        JMSContext jmsContextReceiver = jmsTCFTCP.createContext();

        JMSConsumer jmsConsumer = jmsContextReceiver
                        .createSharedConsumer(jmsTopic, "SUBID1", "Company = 'IBM'");

        JMSProducer jmsProducer = jmsContextSender.createProducer();

        TextMessage tmsg = jmsContextSender
                        .createTextMessage("testCreateSharedNonDurableConsumerWithMsgSelector_create_TCP");
        tmsg.setStringProperty("Company", "IBM");
        jmsProducer.send(jmsTopic, tmsg);

        jmsContextSender.close();

    }

    public void testCreateSharedNonDurableConsumerWithMsgSelector_consume(
                                                                          HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {
        exceptionFlag = false;
        JMSContext jmsContextReceiver = jmsTCFBindings.createContext();
        JMSConsumer jmsConsumer = jmsContextReceiver
                        .createSharedConsumer(jmsTopic, "SUBID", "Company = 'IBM'");

        TextMessage tmsg = (TextMessage) jmsConsumer.receive(30000);
        System.out.println("Message Received is :" + tmsg);
        if (tmsg == null)
            System.out.println("testCreateSharedNonDurableConsumerWithMsgSelector_consume: Message is null as expected");
        else
            exceptionFlag = true;

        jmsConsumer.close();
        jmsContextReceiver.close();
        if (exceptionFlag)
            throw new WrongException("testCreateSharedNonDurableConsumerWithMsgSelector_consume failed: Message was not null");
    }

    public void testCreateSharedNonDurableConsumerWithMsgSelector_consume_TCP(
                                                                              HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContextReceiver = jmsTCFTCP.createContext();
        JMSConsumer jmsConsumer = jmsContextReceiver
                        .createSharedConsumer(jmsTopic, "SUBID1", "Company = 'IBM'");

        TextMessage tmsg = (TextMessage) jmsConsumer.receive(30000);
        System.out.println("Message Received is :" + tmsg);
        if (tmsg == null)
            System.out.println("testCreateSharedNonDurableConsumerWithMsgSelector_consume_TCP: Message is null as expected");
        else
            exceptionFlag = true;

        jmsConsumer.close();
        jmsContextReceiver.close();
        if (exceptionFlag)
            throw new WrongException("testCreateSharedNonDurableConsumerWithMsgSelector_consume_TCP failed: Message was not null");

    }

    //   129626_1_2  If a shared non-durable subscription already exists with the same name and client identifier (if set), and the same topic and message selector has been specified, then this method creates a JMSConsumer on the existing subscription. 

    public void testCreateSharedNonDurableConsumerWithMsgSelector_2Subscribers(
                                                                               HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContextTCFBindings = jmsTCFBindings.createContext();

        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();

        final ObjectName name = new ObjectName(
                        "WebSphere:feature=wasJmsServer,type=Topic,name=NewTopic1");

        javax.management.openmbean.CompositeData[] obn = (CompositeData[]) mbs
                        .invoke(name, "listSubscriptions", null, null);

        int beforeLength = obn.length;

        JMSConsumer jmsConsumer = jmsContextTCFBindings.createSharedConsumer(jmsTopic, "TEST1", "Team = 'WAS'");
        JMSProducer jmsProducer = jmsContextTCFBindings.createProducer();
        TextMessage tmsg = jmsContextTCFBindings
                        .createTextMessage("testCreateSharedNonDurableConsumerWithMsgSelector_2Subscribers");
        tmsg.setStringProperty("Team", "WAS");

        jmsProducer.send(jmsTopic, tmsg);

        tmsg = (TextMessage) jmsConsumer.receive(30000);

        JMSConsumer jmsConsumer2 = jmsContextTCFBindings.createSharedConsumer(
                                                                              jmsTopic, "TEST1", "Team = 'WAS'");

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
            throw new WrongException("testCreateSharedNonDurableConsumerWithMsgSelector_2Subscribers_B_SecOff failed");
        jmsConsumer.close();
        jmsConsumer2.close();
        jmsContextTCFBindings.close();
    }

    public void testCreateSharedNonDurableConsumerWithMsgSelector_2Subscribers_TCP(
                                                                                   HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContext = jmsTCFTCP.createContext();

        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();

        final ObjectName name = new ObjectName(
                        "WebSphere:feature=wasJmsServer,type=Topic,name=NewTopic1");

        javax.management.openmbean.CompositeData[] obn = (CompositeData[]) mbs
                        .invoke(name, "listSubscriptions", null, null);

        int beforeLength = obn.length;

        JMSConsumer jmsConsumer = jmsContext.createSharedConsumer(jmsTopic, "TEST1", "Team = 'WAS'");

        JMSProducer jmsProducer = jmsContext.createProducer();

        TextMessage tmsg = jmsContext.createTextMessage("testCreateSharedNonDurableConsumerWithMsgSelector_2Subscribers_TCP");
        tmsg.setStringProperty("Team", "WAS");

        jmsProducer.send(jmsTopic, tmsg);

        tmsg = (TextMessage) jmsConsumer.receive(30000);

        JMSConsumer jmsConsumer2 = jmsContext.createSharedConsumer(jmsTopic, "TEST1", "Team = 'WAS'");

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
            throw new WrongException("testCreateSharedNonDurableConsumerWithMsgSelector_2Subscribers_TCP_SecOff failed");
        jmsConsumer.close();
        jmsConsumer2.close();
        jmsContext.close();
    }

    //  129626_1_3    A non-durable shared subscription is used by a client which needs to be able to share the work of receiving messages from a topic subscription amongst multiple consumers. A non-durable shared subscription may therefore have more than one consumer. Each message from the subscription will be delivered to only one of the consumers on that subscription

    //  129626_1_6  If a shared non-durable subscription already exists with the same name and client identifier (if set) but a different topic or message selector value has been specified, and there is a consumer already active (i.e. not closed) on the subscription, then a JMSRuntimeException will be thrown. 
    public void testCreateSharedNonDurableConsumerWithMsgSelector_JRException(
                                                                              HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContext = jmsTCFBindings.createContext();

        JMSConsumer jmsConsumer = jmsContext.createSharedConsumer(jmsTopic, "SUBID2", "Company = 'IBM'");

        JMSProducer jmsProducer = jmsContext.createProducer();

        TextMessage tmsg = jmsContext
                        .createTextMessage("testCreateSharedNonDurableConsumerWithMsgSelector_JRException");
        tmsg.setStringProperty("Comapny", "IBM");
        jmsProducer.send(jmsTopic, tmsg);

        tmsg = (TextMessage) jmsConsumer.receive(30000);
        try {

            JMSConsumer jmsConsumer1 = jmsContext
                            .createSharedConsumer(jmsTopic1, "SUBID2", "Company = 'IBM'");
        } catch (JMSRuntimeException ex) {
            ex.printStackTrace();
            System.out.println("Expected JMSRuntimeException seen in testCreateSharedNonDurableConsumerWithMsgSelector_JRException");
            exceptionFlag = true;
        }

        if (jmsContext != null)
            jmsContext.close();
        if (!exceptionFlag)
            throw new WrongException("testCreateSharedNonDurableConsumerWithMsgSelector_JRException failed: Expected exception was not seen");

    }

    public void testCreateSharedNonDurableConsumerWithMsgSelector_JRException_TCP(
                                                                                  HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        exceptionFlag = false;
        JMSContext jmsContext = jmsTCFBindings.createContext();

        JMSConsumer jmsConsumer = jmsContext.createSharedConsumer(jmsTopic, "SUBID3", "Company = 'IBM'");

        JMSProducer jmsProducer = jmsContext.createProducer();

        TextMessage tmsg = jmsContext
                        .createTextMessage("testCreateSharedNonDurableConsumerWithMsgSelector_JRException_TCP");
        tmsg.setStringProperty("Comapny", "IBM");
        jmsProducer.send(jmsTopic, tmsg);

        tmsg = (TextMessage) jmsConsumer.receive(30000);
        try {

            JMSConsumer jmsConsumer1 = jmsContext
                            .createSharedConsumer(jmsTopic1, "SUBID3", "Company = 'IBM'");
        } catch (JMSRuntimeException ex) {
            ex.printStackTrace();
            System.out.println("Expected JMSRuntimeException seen in testCreateSharedNonDurableConsumerWithMsgSelector_JRException_TCP");
            exceptionFlag = true;
        }

        if (jmsContext != null)
            jmsContext.close();
        if (!exceptionFlag)
            throw new WrongException("testCreateSharedNonDurableConsumerWithMsgSelector_JRException_TCP failed: Expected exception was not seen");

    }

    //129626_1_7  There is no restriction on durable subscriptions and shared non-durable subscriptions having the same name and clientId (which may be unset). Such subscriptions would be completely separate.

    public void testCreateSharedNonDurableConsumerWithMsgSelector_coexist(
                                                                          HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        exceptionFlag = false;
        jmsContext = jmsTCFBindings.createContext();
        jmsConsumer = jmsContext.createSharedConsumer(jmsTopic, "SUBID4", "Team = 'WAS'");
        jmsProducer = jmsContext.createProducer();

        TextMessage msg = jmsContext.createTextMessage("testCreateSharedNonDurableConsumerWithMsgSelector_coexist_msg1");
        msg.setStringProperty("Team", "WAS");
        jmsProducer.send(jmsTopic, msg);

        msg = (TextMessage) jmsConsumer.receive(30000);

        JMSConsumer jmsConsumer1 = jmsContext.createDurableConsumer(jmsTopic, "SUBID4", "Team = 'WAS'", false);
        jmsProducer = jmsContext.createProducer();

        TextMessage msg1 = jmsContext.createTextMessage("testCreateSharedNonDurableConsumerWithMsgSelector_coexist_msg2");
        msg1.setStringProperty("Team", "WAS");
        jmsProducer.send(jmsTopic, msg1);

        msg1 = (TextMessage) jmsConsumer.receive(30000);

        if (!(msg != null && msg1 != null))
            exceptionFlag = true;

        if (exceptionFlag)
            throw new WrongException("testCreateSharedNonDurableConsumer_coexist_B_SecOff failed");

        jmsConsumer.close();
        jmsConsumer1.close();
        jmsContext.unsubscribe("SUBID4");
        jmsContext.close();

    }

    public void testCreateSharedNonDurableConsumerWithMsgSelector_coexist_TCP(
                                                                              HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {
        exceptionFlag = false;
        jmsContext = jmsTCFTCP.createContext();
        jmsConsumer = jmsContext.createSharedConsumer(jmsTopic, "SUBID5", "Team = 'WAS'");
        jmsProducer = jmsContext.createProducer();

        TextMessage msg = jmsContext.createTextMessage("testCreateSharedNonDurableConsumerWithMsgSelector_coexist_TCP_msg1");
        msg.setStringProperty("Team", "WAS");
        jmsProducer.send(jmsTopic, msg);

        msg = (TextMessage) jmsConsumer.receive(30000);

        JMSConsumer jmsConsumer1 = jmsContext.createDurableConsumer(jmsTopic, "SUBID5", "Team = 'WAS'", false);
        jmsProducer = jmsContext.createProducer();

        TextMessage msg1 = jmsContext.createTextMessage("testCreateSharedNonDurableConsumerWithMsgSelector_coexist_TCP_msg2");
        msg1.setStringProperty("Team", "WAS");
        jmsProducer.send(jmsTopic, msg1);

        msg1 = (TextMessage) jmsConsumer.receive(30000);

        if (!(msg != null && msg1 != null))
            exceptionFlag = true;

        if (exceptionFlag)
            throw new WrongException("testCreateSharedNonDurableConsumer_coexist_TCP_SecOff failed");

        jmsConsumer.close();
        jmsConsumer1.close();
        jmsContext.unsubscribe("SUBID5");
        jmsContext.close();
    }

    // 129626_1_9  InvalidDestinationRuntimeException - if an invalid topic is specified.
    public void testCreateSharedNonDurableConsumerWithMsgSelector_InvalidDestination(
                                                                                     HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        jmsContext = jmsTCFBindings.createContext();

        try {

            JMSConsumer jmsConsumer = jmsContext
                            .createSharedConsumer(null, "DURATEST1", "Company = 'IBM'");

        } catch (InvalidDestinationRuntimeException ex) {
            ex.printStackTrace();
            System.out.println("testCreateSharedNonDurableConsumerWithMsgSelector_InvalidDestination: InvalidDestinationRuntimeExeption seen as expected");
        }

    }

    public void testCreateSharedNonDurableConsumerWithMsgSelector_InvalidDestination_TCP(
                                                                                         HttpServletRequest request, HttpServletResponse response)
                    throws Throwable {

        jmsContext = jmsTCFTCP.createContext();

        try {

            JMSConsumer jmsConsumer = jmsContext
                            .createSharedConsumer(null, "DURATEST1", "Company = 'IBM'");

        } catch (InvalidDestinationRuntimeException ex) {
            ex.printStackTrace();
            System.out.println("testCreateSharedNonDurableConsumerWithMsgSelector_InvalidDestination: InvalidDestinationRuntimeExeption seen as expected");
        }

    }

    public void testCreateSharedNonDurableConsumerWithMsgSelector_InvalidMsgSelector(HttpServletRequest request,
                                                                                     HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        jmsContext = jmsTCFBindings.createContext();
        try {
            jmsConsumer = jmsContext.createSharedDurableConsumer(jmsTopic, "SUBID", "BAD SELECTOR");
        } catch (InvalidSelectorRuntimeException e) {
            System.out.println("Expected InvalidSelectorRuntimeException seen in testCreateSharedNonDurableConsumerWithMsgSelector_InvalidMsgSelector");
            exceptionFlag = true;
        }

        if (!exceptionFlag)
            throw new WrongException("testCreateSharedNonDurableConsumerWithMsgSelector_InvalidMsgSelector failed: Expected exception not seen");
    }

    public void testCreateSharedNonDurableConsumerWithMsgSelector_InvalidMsgSelector_TCP(HttpServletRequest request,
                                                                                         HttpServletResponse response) throws Throwable {

        exceptionFlag = false;
        jmsContext = jmsTCFTCP.createContext();
        try {
            jmsConsumer = jmsContext.createSharedDurableConsumer(jmsTopic, "SUBID", "BAD SELECTOR");
        } catch (InvalidSelectorRuntimeException e) {
            System.out.println("Expected InvalidSelectorRuntimeException seen in testCreateSharedNonDurableConsumerWithMsgSelector_InvalidMsgSelector_TCP");
            exceptionFlag = true;
        }

        if (!exceptionFlag)
            throw new WrongException("testCreateSharedNonDurableConsumerWithMsgSelector_InvalidMsgSelector_TCP failed: Expected exception not seen");

    }

    public void testBasicMDBTopic(HttpServletRequest request,
                                  HttpServletResponse response) throws Throwable {

        TopicConnectionFactory cf1 = (TopicConnectionFactory) new InitialContext()
                        .lookup("java:comp/env/jms/FAT_TCF");

        int msgs = 20;

        Topic topic = (Topic) new InitialContext()
                        .lookup("java:comp/env/jms/FAT_TOPIC");

        JMSContext context = cf1.createContext();
        JMSProducer publisher = context.createProducer();

        for (int i = 0; i < msgs; i++) {
            publisher.send(topic, "testBasicMDBTopic:" + i);
        }

        System.out.println("Published  messages ");
        Thread.sleep(1000);
        context.close();

    }

    public void testBasicMDBTopic_TCP(HttpServletRequest request,
                                      HttpServletResponse response) throws Throwable {

        TopicConnectionFactory cf1 = (TopicConnectionFactory) new InitialContext()
                        .lookup("java:comp/env/jms/FAT_COMMS_TCF");

        int msgs = 20;

        Topic topic = (Topic) new InitialContext()
                        .lookup("java:comp/env/jms/FAT_TOPIC");

        JMSContext context = cf1.createContext();
        JMSProducer publisher = context.createProducer();

        for (int i = 0; i < msgs; i++) {
            publisher.send(topic, "testBasicMDBTopic:" + i);
        }

        System.out.println("Published  messages ");
        Thread.sleep(1000);
        context.close();

    }

    public static TopicConnectionFactory getTCFBindings() throws NamingException {

        TopicConnectionFactory cf1 = (TopicConnectionFactory) new InitialContext()
                        .lookup("java:comp/env/eis/tcf");

        return cf1;

    }

    public TopicConnectionFactory getTCFTCP() throws NamingException {

        TopicConnectionFactory cf1 = (TopicConnectionFactory) new InitialContext()
                        .lookup("java:comp/env/eis/tcf1");

        return cf1;

    }

    public Topic getTopic(String name) throws NamingException {

        Topic topic = (Topic) new InitialContext()
                        .lookup("java:comp/env/" + name);

        return topic;
    }

    public class WrongException extends Exception {
        String str;

        public WrongException(String str) {
            this.str = str;
            System.out.println(" <ERROR> " + str + " </ERROR>");
        }
    }

}
