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
import java.net.MalformedURLException;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.Set;

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
import javax.management.MBeanServerConnection;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import javax.management.remote.JMXServiceURL;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
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

    public static Topic topic1;
    public static Topic topic2;

    private static final int DEFAULT_TIMEOUT = 30000;

    @Override
    public void init() throws ServletException {
        super.init();

        try {
            tcfBindings = (TopicConnectionFactory)
                new InitialContext().lookup("java:comp/env/eis/tcf");
        } catch ( NamingException e ) {
            e.printStackTrace();
        }
        System.out.println("Topic connection factory 'java:comp/env/eis/tcf':\n" + tcfBindings);

        try {
            tcfTCP = (TopicConnectionFactory)
                new InitialContext().lookup("java:comp/env/eis/tcf1");
        } catch ( NamingException e ) {
            e.printStackTrace();
        }
        System.out.println("Topic connection factory 'java:comp/env/eis/tcf1':\n" + tcfTCP);

        try {
            topic1 = (Topic) new InitialContext().lookup("eis/topic1");
        } catch ( NamingException e ) {
            e.printStackTrace();
        }
        System.out.println("Topic 'eis/topic1':\n" + topic1);

        try {
            topic2 = (Topic) new InitialContext().lookup("eis/topic2");
        } catch ( NamingException e ) {
            e.printStackTrace();
        }
        System.out.println("Topic 'eis/topic2':\n" + topic2);

        if ( (tcfBindings == null) || (tcfTCP == null) ||
             (topic1 == null) || (topic2 == null) ) {
            throw new ServletException("Failed JMS initialization");
        }
    }

    private String test;

    private String getTest() {
        return test;
    }

    private void setTest(String test) {
        this.test = test;
    }

    // Read from the engine server at 'logs/state/com.ibm.ws.jmx.local.address'.
    private String localAddress;

    private String getLocalAddress() {
        return localAddress;
    }

    private void setLocalAddress(String localAddress) {
        this.localAddress = localAddress;
    }

    private JMXConnector openLocalConnector() throws MalformedURLException, IOException {
        JMXServiceURL localConnectorURL = new JMXServiceURL( getLocalAddress() ); // throws MalformedURLException
        System.out.println("JMX Service URL [ " + localConnectorURL + " ]");
        // System.out.println("  Protocol [ " + localConnectorURL.getProtocol() + " ]");
        // System.out.println("  Port     [ " + localConnectorURL.getPort() + " ]");
        // System.out.println("  Host     [ " + localConnectorURL.getHost() + " ]");

        // String localURLPath = localConnectorURL.getURLPath();
        // System.out.println("  Path     [ " + localURLPath + " ]");
		
        JMXConnector localConnector = JMXConnectorFactory.connect(localConnectorURL); // throws IOException
        System.out.println("JMX Connector [ " + localConnector + " ]");
        // System.out.println("JMX Connector ID [ " + localConnector.getConnectionId() + " ]");

        return localConnector;
    }

    private void closeLocalConnector(JMXConnector localConnector) throws IOException {
        localConnector.close(); // throws IOException
    }

    //

    // Expected JMS MBeans:
    //
    // WebSphere:feature=wasJmsServer,type=MessagingEngine,name=*
    // WebSphere:feature=wasJmsServer,type=Queue,name=*
    // WebSphere:feature=wasJmsServer,type=Subscriber,name=*
    // WebSphere:feature=wasJmsServer,type=Topic,name=*

    private void displayJMSMBeans(MBeanServerConnection localEngine) throws IOException {
        System.out.println("JMS MBeans [ " + getTest() + " ]");

        // List all; do not filter the results
        Set<ObjectInstance> mbeans = localEngine.queryMBeans(null, null); // throws IOException

        // [ com.ibm.ws.sib.admin.internal.JsQueue ] [ 1295428529 ]
        // [ WebSphere:feature=wasJmsServer,type=Queue,name=_PSIMP.TDRECEIVER_0DF1DC7B8ADD27AF ]

        for ( ObjectInstance mbean : mbeans ) {
            String mbeanPrintString = mbean.toString();
            if ( !mbeanPrintString.contains("feature=wasJmsServer") ) {
                continue;
            }
            System.out.println("[ " + mbean.getClassName() + " ] [ " + mbean.hashCode() + " ] [ " + mbean.getObjectName() + " ]");
            System.out.println("  [ " + mbeanPrintString + " ]");
        }
    }

    private CompositeData[] listSubscriptions(String nameText) throws Exception {

        JMXConnector localConnector =
            openLocalConnector(); // throws MalformedURLException, IOException

        try {
            MBeanServerConnection localEngine = localConnector.getMBeanServerConnection(); // throws IOException

            displayJMSMBeans(localEngine); // throws IOException

            ObjectName name = new ObjectName(nameText); // throws MalformedObjectNameException

            return (CompositeData[]) localEngine.invoke(name, "listSubscriptions", null, null);
            // throws InstanceNotFoundException, MBeanException, ReflectionException, IOException

        } finally {
            closeLocalConnector(localConnector); // throws IOException
        }
    }

    // MBeanServerConnection JMXConnector.getMBeanServerConnection(); // throws IOException
    // MBeanServerConnection.queryMBeans(...) // throws IOException
    // MBeanServerConnection.queryNames(...) // throws IOException

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
        setTest(test);

        // Read from the engine server at 'logs/state/com.ibm.ws.jmx.local.address'.
        String decodedLocalAddress = request.getParameter("localAddress");
        setLocalAddress(decodedLocalAddress);

        PrintWriter out = response.getWriter();
        out.println("Starting " + test + "<br>");

        // The injection engine doesn't like this at the class level.
        TraceComponent tc = Tr.register(DurableUnsharedServlet.class);

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

    public void testCreateUnSharedDurableConsumer_create(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContextReceiver = tcfBindings.createContext();
        JMSConsumer jmsConsumer = jmsContextReceiver.createDurableConsumer(topic1, "SUBID1");

        JMSContext jmsContextSender = tcfBindings.createContext();
        JMSProducer jmsProducer = jmsContextSender.createProducer();
        TextMessage msgOut = jmsContextSender.createTextMessage("This is a test message");
        jmsProducer.send(topic1, msgOut);

        jmsContextSender.close();

        jmsConsumer.close();
        jmsContextReceiver.close();
    }

    public void testCreateUnSharedDurableConsumer_consume(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContextReceiver = tcfBindings.createContext();
        JMSConsumer jmsConsumer = jmsContextReceiver.createDurableConsumer(topic1, "SUBID1");
        TextMessage msgOut = (TextMessage) jmsConsumer.receive(DEFAULT_TIMEOUT);

        boolean testFailed = false;
        if ( msgOut == null ) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContextReceiver.unsubscribe("SUBID1");
        jmsContextReceiver.close();

        if ( testFailed ) {
            throw new Exception("testCreateUnSharedDurableConsumer_consume failed");
        }
    }

    public void testCreateUnSharedDurableConsumer_create_TCP(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContextReceiver = tcfTCP.createContext();
        JMSConsumer jmsConsumer = jmsContextReceiver.createDurableConsumer(topic1, "SUBID2");

        JMSContext jmsContextSender = tcfTCP.createContext();
        JMSProducer jmsProducer = jmsContextSender.createProducer();
        TextMessage msgOut = jmsContextSender.createTextMessage("This is a test message");
        jmsProducer.send(topic1, msgOut);

        jmsContextSender.close();
        jmsContextReceiver.close();
    }

    public void testCreateUnSharedDurableConsumer_consume_TCP(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContextReceiver = tcfTCP.createContext();
        JMSConsumer jmsConsumer = jmsContextReceiver.createDurableConsumer(topic1, "SUBID2");
        TextMessage msgOut = (TextMessage) jmsConsumer.receive(DEFAULT_TIMEOUT);

        boolean testFailed = false;
        if ( msgOut == null ) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContextReceiver.unsubscribe("SUBID2");
        jmsContextReceiver.close();

        if ( testFailed ) {
            throw new Exception("testCreateUnSharedDurableConsumer_consume_TCP failed");
        }
    }

    public void testCreateUnSharedDurableConsumer_create_Expiry(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContextReceiver = tcfBindings.createContext();
        JMSConsumer jmsConsumer = jmsContextReceiver.createDurableConsumer(topic2, "Exp");

        JMSContext jmsContextSender = tcfBindings.createContext();
        JMSProducer jmsProducer = jmsContextSender.createProducer();
        TextMessage msgOut = jmsContextSender.createTextMessage("This is a test message");
        jmsProducer.send(topic2, msgOut);

        jmsContextSender.close();

        jmsConsumer.close();
        jmsContextReceiver.close();
    }

    public void testCreateUnSharedDurableConsumer_consume_Expiry(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        // The topic timeToLive is set to 200ms;
        // wait twice that long to force the message to expire.
        Thread.currentThread().sleep(2 * 200);

        JMSContext jmsContextReceiver = tcfBindings.createContext();
        JMSConsumer jmsConsumer = jmsContextReceiver.createDurableConsumer(topic2, "Exp");
        TextMessage msgOut = (TextMessage) jmsConsumer.receive(DEFAULT_TIMEOUT);

        boolean testFailed = false;
        if ( msgOut != null ) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContextReceiver.unsubscribe("Exp");
        jmsContextReceiver.close();

        if ( testFailed ) {
            throw new Exception("testCreateUnSharedDurableConsumer_consume failed");
        }
    }

    public void testCreateUnSharedDurableConsumer_create_Expiry_TCP(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContextReceiver = tcfTCP.createContext();
        JMSConsumer jmsConsumer = jmsContextReceiver.createDurableConsumer(topic2, "Exp");

        JMSContext jmsContextSender = tcfTCP.createContext();
        JMSProducer jmsProducer = jmsContextSender.createProducer();
        TextMessage msgOut = jmsContextSender.createTextMessage("This is a test message");
        jmsProducer.send(topic1, msgOut);

        jmsContextSender.close();
        jmsContextReceiver.close();
    }

    public void testCreateUnSharedDurableConsumer_consume_Expiry_TCP(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        // The topic timeToLive is set to 200ms;
        // wait twice that long to force the message to expire.
        Thread.currentThread().sleep(2 * 200);

        JMSContext jmsContextReceiver = tcfTCP.createContext();
        JMSConsumer jmsConsumer = jmsContextReceiver.createDurableConsumer(topic2, "Exp");
        TextMessage msgOut = (TextMessage) jmsConsumer.receive(DEFAULT_TIMEOUT);

        boolean testFailed = false;
        if ( msgOut != null ) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContextReceiver.unsubscribe("Exp");
        jmsContextReceiver.close();

        if ( testFailed ) {
            throw new Exception("testCreateUnSharedDurableConsumer_consume_Expiry_TCP failed");
        }
    }

    // A durable subscription will continue to accumulate messages
    // until it is deleted using the unsubscribe method.

    public void testCreateSharedDurableConsumer_unsubscribe(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = tcfBindings.createContext();
        JMSConsumer jmsConsumer = jmsContext.createDurableConsumer(topic1, "DURATEST1");

        JMSProducer jmsProducer = jmsContext.createProducer();
        TextMessage msgOut = jmsContext.createTextMessage("This is a test message");
        jmsProducer.send(topic1, msgOut);

        TextMessage msgIn = (TextMessage) jmsConsumer.receive(DEFAULT_TIMEOUT);

        jmsConsumer.close();
        jmsContext.unsubscribe("DURATEST1");

        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        ObjectName name =
            new ObjectName("WebSphere:feature=wasJmsServer,type=Subscriber,name=clientID##DURATEST1");

        boolean testFailed = false;
        try {
            String obn = (String) mbs.getAttribute(name, "Id");
        } catch ( InstanceNotFoundException ex ) {
            ex.printStackTrace();
            testFailed = true;
        }

        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testCreateSharedDurableConsumer_unsubscribe failed");
        }
    }

    public void testCreateSharedDurableConsumer_unsubscribe_TCP(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = tcfTCP.createContext();
        JMSConsumer jmsConsumer = jmsContext.createDurableConsumer(topic1, "DURATEST2");

        JMSProducer jmsProducer = jmsContext.createProducer();
        TextMessage msgOut = jmsContext.createTextMessage("This is a test message");
        jmsProducer.send(topic1, msgOut);

        TextMessage msgIn = (TextMessage) jmsConsumer.receive(DEFAULT_TIMEOUT);

        jmsConsumer.close();
        jmsContext.unsubscribe("DURATEST2");

        boolean testFailed = false;

        JMXConnector localConnector = openLocalConnector(); // throws MalformedURLException, IOException
        try {
            MBeanServerConnection localEngine = localConnector.getMBeanServerConnection(); // throws IOException
            ObjectName name = new ObjectName("WebSphere:feature=wasJmsServer,type=Subscriber,name=clientID##DURATEST2");
            // throws MalformedObjectNameException
            try {
                String obn = (String) localEngine.getAttribute(name, "Id");
                testFailed = true;
                System.out.println("Unexpectedly retrieved attribute [ " + name + " ] [ " + "Id" + " ] as [ " + obn + " ]");
            } catch ( InstanceNotFoundException ex ) {
                ex.printStackTrace();
            }
        } finally {
            closeLocalConnector(localConnector); // throws IOException
        }

        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testCreateSharedDurableConsumer_unsubscribe_TCP failed");
        }
    }

    // Any durable subscription created using this method will be
    // shared. This means that multiple active (i.e. not closed) consumers on
    // the subscription may exist at the same time. The term "consumer" here
    // means a JMSConsumer object in any client.

    // 129623_1_6 A shared durable subscription is identified by a name
    // specified by the client and by the client identifier (which may be
    // unset). An application which subsequently wishes to create a consumer on
    // that shared durable subscription must use the same client identifier.

/*
 * public void testBasicMDBTopic(HttpServletRequest request,
 * HttpServletResponse response) throws Exception {
 * 
 * int msgs = 10;
 * 
 * JMSContext jmsCont = tcfBindings.createContext();
 * JMSProducer publisher = jmsCont.createProducer();
 * 
 * for (int i = 0; i < msgs; i++) {
 * publisher.send(topic1, "testBasicMDBTopic:" + i);
 * }
 * 
 * System.out.println("Published  messages ");
 * 
 * }
 * 
 * public void testBasicMDBTopic_TCP(HttpServletRequest request,
 * HttpServletResponse response) throws Exception {
 * 
 * int msgs = 10;
 * 
 * JMSContext jmsCont = tcfTCP.createContext();
 * JMSProducer publisher = jmsCont.createProducer();
 * 
 * for (int i = 0; i < msgs; i++) {
 * publisher.send(topic1, "testBasicMDBTopic:" + i);
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
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = tcfBindings.createContext();
        JMSConsumer jmsConsumer1 = jmsContext.createDurableConsumer(topic1, "DURATEST3");

        JMSProducer jmsProducer = jmsContext.createProducer();
        TextMessage msgOut = jmsContext.createTextMessage("This is a test message");
        jmsProducer.send(topic1, msgOut);

        TextMessage msgIn = (TextMessage) jmsConsumer1.receive(DEFAULT_TIMEOUT);

        jmsConsumer1.close();

        JMSConsumer jmsConsumer2 = jmsContext.createDurableConsumer(topic1, "DURATEST3");

        CompositeData[] obn = null;
        try {
            obn = listSubscriptions("WebSphere:feature=wasJmsServer,type=Topic1,name=NewTopic1");
            // throws MalformedURLException, IOException
        } finally {
            jmsConsumer2.close();
            jmsContext.unsubscribe("DURATEST3");
            jmsContext.close();
        }

        if ( obn.length != 1 ) {
            throw new Exception("testCreateSharedDurableConsumer_2Subscribers failed");
        }
    }

    public void testCreateSharedDurableConsumer_2Subscribers_TCP(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = tcfTCP.createContext();
        JMSConsumer jmsConsumer1 = jmsContext.createDurableConsumer(topic1, "DURATEST4");
        JMSProducer jmsProducer = jmsContext.createProducer();

        TextMessage msgOut = jmsContext.createTextMessage("This is a test message");
        jmsProducer.send(topic1, msgOut);

        TextMessage msgIn1 = (TextMessage) jmsConsumer1.receive(DEFAULT_TIMEOUT);

        jmsConsumer1.close();

        JMSConsumer jmsConsumer2 = jmsContext.createDurableConsumer(topic1, "DURATEST4");
        jmsProducer.send(topic1, msgIn1);

        TextMessage msgIn2 = (TextMessage) jmsConsumer2.receive(DEFAULT_TIMEOUT);

        boolean testFailed = false;
        if ( msgIn2 == null ) {
            testFailed = true;
        }

        jmsConsumer2.close();
        jmsContext.unsubscribe("DURATEST4");
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testCreateSharedDurableConsumer_2Subscribers_TCP failed");
        }
    }

    // If a shared durable subscription already exists with the same
    // name and client identifier (if set) but a different topic has been
    // specified, and there is no consumer already active (i.e. not closed) on
    // the durable subscription then this is equivalent to unsubscribing
    // (deleting) the old one and creating a new one.

    public void testCreateSharedDurableConsumer_2SubscribersDiffTopic(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = tcfBindings.createContext();
        JMSConsumer jmsConsumer1 = jmsContext.createDurableConsumer(topic1, "DURATEST123");

        JMSProducer jmsProducer = jmsContext.createProducer();
        TextMessage msgOut = jmsContext.createTextMessage("This is a test message");
        jmsProducer.send(topic1, msgOut);

        TextMessage msgIn1 = (TextMessage) jmsConsumer1.receive(DEFAULT_TIMEOUT);

        jmsConsumer1.close();
        jmsContext.close();

        JMSConsumer jmsConsumer2 = jmsContext.createDurableConsumer(topic2, "DURATEST123");

        jmsProducer.send(topic2, msgIn1);
        TextMessage msgIn2 = (TextMessage) jmsConsumer2.receive(DEFAULT_TIMEOUT);

        boolean testFailed = false;
        if ( msgOut != null ) {
            testFailed = true;
        }

        jmsConsumer2.close();
        jmsContext.unsubscribe("DURATEST123");
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testCreateSharedDurableConsumer_2SubscribersDiffTopic failed");
        }
    }

    public void testCreateSharedDurableConsumer_2SubscribersDiffTopic_TCP(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = tcfTCP.createContext();
        JMSConsumer jmsConsumer1 = jmsContext.createDurableConsumer(topic1, "DURATEST456");

        JMSProducer jmsProducer = jmsContext.createProducer();
        TextMessage msgOut = jmsContext.createTextMessage("This is a test message");
        jmsProducer.send(topic1, msgOut);

        TextMessage msgIn1 = (TextMessage) jmsConsumer1.receive(DEFAULT_TIMEOUT);

        jmsConsumer1.close();
        jmsContext.close();
        JMSConsumer jmsConsumer2 = jmsContext.createDurableConsumer(topic2, "DURATEST456");

        jmsProducer.send(topic2, msgIn1);
        TextMessage msgIn2 = (TextMessage) jmsConsumer2.receive(DEFAULT_TIMEOUT);

        boolean testFailed = false;
        if ( msgIn2 == null ) {
            testFailed = true;
        }

        jmsConsumer2.close();
        jmsContext.unsubscribe("DURATEST456");

        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testCreateSharedDurableConsumer_2SubscribersDiffTopic_TCP failed");
        }
    }

    // If a shared durable subscription already exists with the same
    // name and client identifier (if set) but a different topic has been
    // specified, and there is a consumer already active (i.e. not closed) on
    // the durable subscription, then a JMSRuntimeException will be thrown.

    public void testCreateSharedDurableConsumer_JRException(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = tcfBindings.createContext();
        JMSConsumer jmsConsumer = jmsContext.createDurableConsumer(topic1, "DURATEST7");

        JMSProducer jmsProducer = jmsContext.createProducer();
        TextMessage msgOut = jmsContext.createTextMessage("This is a test message");
        jmsProducer.send(topic1, msgOut);

        TextMessage msgIn = (TextMessage) jmsConsumer.receive(DEFAULT_TIMEOUT);

        boolean testFailed = false;
        try {
            JMSConsumer jmsConsumer1 = jmsContext.createDurableConsumer(topic2, "DURATEST7");
            testFailed = true;
        } catch ( JMSRuntimeException ex ) {
            ex.printStackTrace();
        }

        jmsConsumer.close();
        jmsContext.unsubscribe("DURATEST7");
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testCreateSharedDurableConsumer_JRException failed");
        }
    }

    public void testCreateSharedDurableConsumer_JRException_TCP(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = tcfTCP.createContext();
        JMSConsumer jmsConsumer = jmsContext.createDurableConsumer(topic1, "DURATEST8");

        JMSProducer jmsProducer = jmsContext.createProducer();
        TextMessage msgOut = jmsContext.createTextMessage("This is a test message");
        jmsProducer.send(topic1, msgOut);

        TextMessage msgIn = (TextMessage) jmsConsumer.receive(DEFAULT_TIMEOUT);

        boolean testFailed = false;
        try {
            JMSConsumer jmsConsumer1 = jmsContext.createDurableConsumer(topic2, "DURATEST8");
            testFailed = true;
        } catch ( JMSRuntimeException ex ) {
            ex.printStackTrace();
        }

        jmsConsumer.close();
        jmsContext.unsubscribe("DURATEST8");
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testCreateSharedDurableConsumer_JRException_TCP failed");
        }
    }

    // A shared durable subscription and an unshared durable
    // subscription may not have the same name and client identifier (if set).
    // If an unshared durable subscription already exists with the same name and
    // client identifier (if set) then a JMSRuntimeException is thrown.

    public void testCreateSharedDurableUndurableConsumer_JRException(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = tcfBindings.createContext();
        JMSConsumer jmsConsumer = jmsContext.createDurableConsumer(topic1,"DURATEST9");

        JMSProducer jmsProducer = jmsContext.createProducer();
        TextMessage msgOut = jmsContext.createTextMessage("This is a test message");
        jmsProducer.send(topic1, msgOut);

        TextMessage msgIn = (TextMessage) jmsConsumer.receive(DEFAULT_TIMEOUT);

        boolean testFailed = false;
        try {
            JMSConsumer jmsConsumer1 = jmsContext.createSharedDurableConsumer(topic2, "DURATEST9");
            testFailed = true;
        } catch ( JMSRuntimeException ex ) {
            ex.printStackTrace();
        }

        jmsConsumer.close();
        jmsContext.unsubscribe("DURATEST9");
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testCreateSharedDurableUndurableConsumer_JRException failed");
        }
    }

    public void testCreateSharedDurableUndurableConsumer_JRException_TCP(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = tcfTCP.createContext();
        JMSConsumer jmsConsumer = jmsContext.createDurableConsumer(topic1, "DURATEST10");

        JMSProducer jmsProducer = jmsContext.createProducer();
        TextMessage msgOut = jmsContext.createTextMessage("This is a test message");
        jmsProducer.send(topic1, msgOut);

        TextMessage msgIn = (TextMessage) jmsConsumer.receive(DEFAULT_TIMEOUT);

        boolean testFailed = false;
        try {
            JMSConsumer jmsConsumer1 =
                jmsContext.createSharedDurableConsumer(topic2, "DURATEST10");
            testFailed = true;
        } catch ( JMSRuntimeException ex ) {
            ex.printStackTrace();
        }

        jmsConsumer.close();
        jmsContext.unsubscribe("DURATEST10");
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testCreateSharedDurableUndurableConsumer_JRException failed");
        }
    }

    // InvalidDestinationRuntimeException if an invalid topic is specified.

    public void testCreateSharedDurableConsumer_InvalidDestination(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = tcfBindings.createContext();

        boolean testFailed = false;
        try {
            JMSConsumer jmsConsumer = jmsContext.createDurableConsumer(null, "DURATEST11");
            testFailed = true;
        } catch ( InvalidDestinationRuntimeException ex ) {
            ex.printStackTrace();
        }

        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testCreateSharedDurableConsumer_InvalidDestination failed");
        }
    }

    public void testCreateSharedDurableConsumer_InvalidDestination_TCP(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = tcfTCP.createContext();

        boolean testFailed = false;
        try {
            JMSConsumer jmsConsumer = jmsContext .createDurableConsumer(null, "DURATEST12");
            testFailed = true;
        } catch ( InvalidDestinationRuntimeException ex ) {
            ex.printStackTrace();
        }

        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testCreateSharedDurableConsumer_InvalidDestination_TCP failed");
        }
    }

    //  Case where name is null and empty string

    public void testCreateSharedDurableConsumer_Null(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = tcfBindings.createContext();

        boolean testFailed = false;
        try {
            JMSConsumer jmsConsumer = jmsContext.createDurableConsumer(topic1, null);
            testFailed = true;
        } catch ( JMSRuntimeException ex ) {
            ex.printStackTrace();
        }

        try {
            JMSConsumer jmsConsumer = jmsContext.createDurableConsumer(topic1, "");
            testFailed = true;
        } catch ( JMSRuntimeException ex ) {
            ex.printStackTrace();
        }

        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testCreateSharedDurableConsumer_Null failed");
        }
    }

    public void testCreateSharedDurableConsumer_Null_TCP(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = tcfTCP.createContext();

        boolean testFailed = false;
        try {
            JMSConsumer jmsConsumer = jmsContext.createDurableConsumer(topic1, null);
            testFailed = true;
        } catch ( JMSRuntimeException ex ) {
            ex.printStackTrace();
        }

        try {
            JMSConsumer jmsConsumer = jmsContext.createDurableConsumer(topic1, "");
            testFailed = true;
        } catch ( JMSRuntimeException ex ) {
            ex.printStackTrace();
        }

        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testCreateSharedDurableConsumer_Null_TCP failed");
        }
    }

    // selectors

    public void testCreateUnSharedDurableConsumer_Sel_create(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContextReceiver = tcfBindings.createContext();
        JMSConsumer jmsConsumer = 
            jmsContextReceiver.createDurableConsumer(topic1, "SUBID3", "Company = 'IBM'", true);

        JMSContext jmsContextSender = tcfBindings.createContext();
        JMSProducer jmsProducer = jmsContextSender.createProducer();
        TextMessage msgOut = jmsContextSender.createTextMessage("This is a test message");
        msgOut.setStringProperty("Company", "IBM");
        jmsProducer.send(topic1, msgOut);

        jmsContextSender.close();
        jmsContextReceiver.close();
    }

    public void testCreateUnSharedDurableConsumer_Sel_consume(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContextReceiver = tcfBindings.createContext();
        JMSConsumer jmsConsumer =
            jmsContextReceiver.createDurableConsumer(topic1, "SUBID3", "Company = 'IBM'", true);
        TextMessage msgIn = (TextMessage) jmsConsumer.receive(DEFAULT_TIMEOUT);

        boolean testFailed = false;
        if ( msgIn == null ) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContextReceiver.unsubscribe("SUBID3");
        jmsContextReceiver.close();

        if ( testFailed ) {
            throw new Exception("testCreateUnSharedDurableConsumer_Sel_consume failed");
        }
    }

    public void testCreateUnSharedDurableConsumer_Sel_create_TCP(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContextReceiver = tcfTCP.createContext();
        JMSConsumer jmsConsumer =
            jmsContextReceiver.createDurableConsumer(topic1, "SUBID4", "Company = 'IBM'", true);

        JMSContext jmsContextSender = tcfTCP.createContext();
        JMSProducer jmsProducer = jmsContextSender.createProducer();
        TextMessage msgOut = jmsContextSender.createTextMessage("This is a test message");
        msgOut.setStringProperty("Company", "IBM");
        jmsProducer.send(topic1, msgOut);

        jmsContextSender.close();
        jmsContextReceiver.close();
    }

    public void testCreateUnSharedDurableConsumer_Sel_consume_TCP(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContextReceiver = tcfTCP.createContext();
        JMSConsumer jmsConsumer =
            jmsContextReceiver.createDurableConsumer(topic1, "SUBID4", "Company = 'IBM'", true);
        TextMessage msgIn = (TextMessage) jmsConsumer.receive(DEFAULT_TIMEOUT);

        boolean testFailed = false;
        if ( msgIn == null ) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContextReceiver.unsubscribe("SUBID4");
        jmsContextReceiver.close();

        if ( testFailed ) {
            throw new Exception("testCreateUnSharedDurableConsumer_Sel_consume_TCP failed");
        }
    }

    public void testCreateUnSharedDurableConsumer_Sel_create_Expiry(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContextReceiver = tcfBindings.createContext();
        JMSConsumer jmsConsumer =
            jmsContextReceiver.createDurableConsumer(topic2, "EXPID1", "Company = 'IBM'", true);

        JMSContext jmsContextSender = tcfBindings.createContext();
        JMSProducer jmsProducer = jmsContextSender.createProducer();
        TextMessage msgOut = jmsContextSender.createTextMessage("This is a test message");
        msgOut.setStringProperty("Company", "IBM");
        jmsProducer.send(topic2, msgOut);

        jmsContextSender.close();
        jmsContextReceiver.close();
    }

    public void testCreateUnSharedDurableConsumer_Sel_consume_Expiry(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        // The topic timeToLive is set to 200ms;
        // wait twice that long to force the message to expire.
        Thread.currentThread().sleep(2 * 200);

        JMSContext jmsContextReceiver = tcfBindings.createContext();
        JMSConsumer jmsConsumer =
            jmsContextReceiver.createDurableConsumer(topic2, "EXPID1", "Company = 'IBM'", true);

        TextMessage msgIn = (TextMessage) jmsConsumer.receive(DEFAULT_TIMEOUT);

        boolean testFailed = false;
        if ( msgIn != null ) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContextReceiver.unsubscribe("EXPID1");
        jmsContextReceiver.close();

        if ( testFailed ) {
            throw new Exception("testCreateUnSharedDurableConsumer_Sel_consume_Expiry failed");
        }
    }

    public void testCreateUnSharedDurableConsumer_Sel_create_Expiry_TCP(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContextSender = tcfTCP.createContext();
        JMSContext jmsContextReceiver = tcfTCP.createContext();

        JMSConsumer jmsConsumer =
            jmsContextReceiver.createDurableConsumer(topic2, "EXPID2", "Company = 'IBM'", true);

        JMSProducer jmsProducer = jmsContextSender.createProducer();
        TextMessage msgOut = jmsContextSender.createTextMessage("This is a test message");
        msgOut.setStringProperty("Company", "IBM");
        jmsProducer.send(topic2, msgOut);

        jmsContextReceiver.close();
        jmsContextSender.close();
    }

    public void testCreateUnSharedDurableConsumer_Sel_consume_Expiry_TCP(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        // The topic timeToLive is set to 200ms;
        // wait twice that long to force the message to expire.
        Thread.currentThread().sleep(2 * 200);

        JMSContext jmsContextReceiver = tcfTCP.createContext();
        JMSConsumer jmsConsumer =
            jmsContextReceiver.createDurableConsumer(topic2, "EXPID2", "Company = 'IBM'", true);

        TextMessage msgIn = (TextMessage) jmsConsumer.receive(DEFAULT_TIMEOUT);

        boolean testFailed = false;
        if ( msgIn != null ) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContextReceiver.unsubscribe("EXPID2");
        jmsContextReceiver.close();

        if ( testFailed ) {
            throw new Exception("testCreateUnSharedDurableConsumer_Sel_consume_Expiry_TCP failed");
        }
    }

    // If a shared durable subscription already exists with the same
    // name and client identifier (if set), and the same topic and message
    // selector has been specified, then this method creates a JMSConsumer on
    // the existing shared durable subscription.

    public void testCreateSharedDurableConsumer_Sel_2Subscribers(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = tcfBindings.createContext();
        JMSConsumer jmsConsumer1 =
            jmsContext.createDurableConsumer(topic1, "DURATEST13", "Company = 'IBM'", true);

        JMSProducer jmsProducer = jmsContext.createProducer();

        TextMessage msgOut = jmsContext.createTextMessage("This is a test message");
        msgOut.setStringProperty("Company", "IBM");
        jmsProducer.send(topic1, msgOut);

        TextMessage msgIn = (TextMessage) jmsConsumer1.receive(DEFAULT_TIMEOUT);

        jmsConsumer1.close();

        boolean testFailed = false;
        try {
            JMSConsumer jmsConsumer2 =
                jmsContext.createDurableConsumer(topic1, "DURATEST13", "Company = 'IBM'", true);
            testFailed = true;
        } catch ( Exception mfe ) {
            mfe.printStackTrace();
        }

        jmsContext.unsubscribe("DURATEST13");
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testCreateSharedDurableConsumer_Sel_2Subscribers failed");
        }
    }

    public void testCreateSharedDurableConsumer_Sel_2Subscribers_TCP(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = tcfTCP.createContext();
        JMSConsumer jmsConsumer1 =
            jmsContext.createDurableConsumer(topic1, "DURATEST14", "Company = 'IBM'", true);

        JMSProducer jmsProducer = jmsContext.createProducer();
        TextMessage msgOut = jmsContext.createTextMessage("This is a test message");
        msgOut.setStringProperty("Company", "IBM");
        jmsProducer.send(topic1, msgOut);

        TextMessage msgIn = (TextMessage) jmsConsumer1.receive(DEFAULT_TIMEOUT);

        jmsConsumer1.close();

        boolean testFailed = false;
        try {
            JMSConsumer jmsConsumer2 =
                jmsContext.createDurableConsumer(topic1, "DURATEST14", "Company = 'IBM'", true);
            testFailed = true;
        } catch ( Exception mfe ) {
            mfe.printStackTrace();
        }

        jmsContext.unsubscribe("DURATEST14");
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testCreateSharedDurableConsumer_Sel_2Subscribers_TCP failed");
        }
    }

    // If a shared durable subscription already exists with the same
    // name and client identifier (if set) but a different topic has been
    // specified, and there is no consumer already active (i.e. not closed) on
    // the durable subscription then this is equivalent to unsubscribing
    // (deleting) the old one and creating a new one.

    public void testCreateSharedDurableConsumer_Sel_2SubscribersDiffTopic(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = tcfBindings.createContext();
        JMSConsumer jmsConsumer1 = jmsContext.createDurableConsumer(topic1, "DURATEST15", "Company = 'IBM'", true);

        JMSProducer jmsProducer = jmsContext.createProducer();
        TextMessage msgOut = jmsContext.createTextMessage("This is a test message");
        msgOut.setStringProperty("Company", "IBM");
        jmsProducer.send(topic2, msgOut);

        TextMessage msgIn = (TextMessage) jmsConsumer1.receive(DEFAULT_TIMEOUT);

        jmsConsumer1.close();

        boolean testFailed = false;
        try {
            JMSConsumer jmsConsumer2 =
                jmsContext.createDurableConsumer(topic2, "DURATEST15", "Company = 'IBM'", true);
            testFailed = true;
        } catch ( Exception mfe ) {
            mfe.printStackTrace();
        }

        jmsContext.unsubscribe("DURATEST15");
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testCreateSharedDurableConsumer_Sel_2SubscribersDiffTopic failed");
        }
    }

    public void testCreateSharedDurableConsumer_Sel_2SubscribersDiffTopic_TCP(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = tcfTCP.createContext();
        JMSConsumer jmsConsumer1 = jmsContext.createDurableConsumer(topic1, "DURATEST16", "Company = 'IBM'", true);

        JMSProducer jmsProducer = jmsContext.createProducer();
        TextMessage msgOut = jmsContext.createTextMessage("This is a test message");
        msgOut.setStringProperty("Company", "IBM");
        jmsProducer.send(topic2, msgOut);

        TextMessage msgIn = (TextMessage) jmsConsumer1.receive(DEFAULT_TIMEOUT);

        jmsConsumer1.close();

        boolean testFailed = false;
        try {
            JMSConsumer jmsConsumer2 = jmsContext.createDurableConsumer(topic2, "DURATEST16", "Company = 'IBM'", true);
            testFailed = true;
        } catch ( Exception mfe ) {
            mfe.printStackTrace();
        }

        jmsContext.unsubscribe("DURATEST16");
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testCreateSharedDurableConsumer_Sel_2SubscribersDiffTopic_TCP failed");
        }
    }

    // If a shared durable subscription already exists with the same
    // name and client identifier (if set) but a different topic has been
    // specified, and there is a consumer already active (i.e. not closed) on
    // the durable subscription, then a JMSRuntimeException will be thrown.

    public void testCreateSharedDurableConsumer_Sel_JRException(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = tcfBindings.createContext();
        JMSConsumer jmsConsumer1 = jmsContext.createDurableConsumer(topic1, "DURATEST17", "Company = 'IBM'", true);

        JMSProducer jmsProducer = jmsContext.createProducer();
        TextMessage msgOut = jmsContext.createTextMessage("This is a test message");
        msgOut.setStringProperty("Company", "IBM");
        jmsProducer.send(topic1, msgOut);

        TextMessage msgIn = (TextMessage) jmsConsumer1.receive(DEFAULT_TIMEOUT);

        boolean testFailed = false;
        try {
            JMSConsumer jmsConsumer2 =
                jmsContext.createDurableConsumer(topic2, "DURATEST17", "Company = 'IBM'", true);
            testFailed = true;
        } catch ( JMSRuntimeException ex ) {
            ex.printStackTrace();
        }

        jmsConsumer1.close();
        jmsContext.unsubscribe("DURATEST17");
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testCreateSharedDurableConsumer_Sel_JRException failed");
        }
    }

    public void testCreateSharedDurableConsumer_Sel_JRException_TCP(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = tcfTCP.createContext();

        JMSConsumer jmsConsumer1 = jmsContext.createDurableConsumer(topic1, "DURATEST18", "Company = 'IBM'", true);
        JMSProducer jmsProducer = jmsContext.createProducer();

        TextMessage msgOut = jmsContext.createTextMessage("This is a test message");
        msgOut.setStringProperty("Company", "IBM");
        jmsProducer.send(topic1, msgOut);

        TextMessage msgIn = (TextMessage) jmsConsumer1.receive(DEFAULT_TIMEOUT);

        boolean testFailed = false;
        try {
            JMSConsumer jmsConsumer2 =
                jmsContext.createDurableConsumer(topic2, "DURATEST18", "Company = 'IBM'", true);
            testFailed = true;
        } catch ( JMSRuntimeException ex ) {
            ex.printStackTrace();
        }

        jmsConsumer1.close();
        jmsContext.unsubscribe("DURATEST18");
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testCreateSharedDurableConsumer_JRException_TCP failed");
        }
    }

    // A shared durable subscription and an unshared durable
    // subscription may not have the same name and client identifier (if set).
    // If an unshared durable subscription already exists with the same name and
    // client identifier (if set) then a JMSRuntimeException is thrown.

    public void testCreateSharedDurableUndurableConsumer_Sel_JRException(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = tcfBindings.createContext();
        JMSConsumer jmsConsumer1 = jmsContext.createDurableConsumer(topic1, "DURATEST19", "Company = 'IBM'", true);

        JMSProducer jmsProducer = jmsContext.createProducer();
        TextMessage msgOut = jmsContext.createTextMessage("This is a test message");
        msgOut.setStringProperty("Company", "IBM");
        jmsProducer.send(topic1, msgOut);

        TextMessage msgIn = (TextMessage) jmsConsumer1.receive(DEFAULT_TIMEOUT);

        boolean testFailed = false;
        try {
            JMSConsumer jmsConsumer2 = jmsContext.createSharedDurableConsumer(topic1, "DURATEST19");
            testFailed = true;
        } catch ( JMSRuntimeException ex ) {
            ex.printStackTrace();
        }

        jmsConsumer1.close();
        jmsContext.unsubscribe("DURATEST19");
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testCreateSharedDurableUndurableConsumer_Sel_JRException failed");
        }
    }

    public void testCreateSharedDurableUndurableConsumer_Sel_JRException_TCP(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = tcfTCP.createContext();
        JMSConsumer jmsConsumer1 = jmsContext.createDurableConsumer(topic1, "DURATEST20", "Company = 'IBM'", true);

        JMSProducer jmsProducer = jmsContext.createProducer();
        TextMessage msgOut = jmsContext.createTextMessage("This is a test message");
        msgOut.setStringProperty("Company", "IBM");
        jmsProducer.send(topic1, msgOut);

        TextMessage msgIn = (TextMessage) jmsConsumer1.receive(DEFAULT_TIMEOUT);

        boolean testFailed = false;
        try {
            JMSConsumer jmsConsumer2 = jmsContext.createSharedDurableConsumer(topic1, "DURATEST20");
            testFailed = true;
        } catch ( JMSRuntimeException ex ) {
            ex.printStackTrace();
        }

        jmsConsumer1.close();
        jmsContext.unsubscribe("DURATEST20");
        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testCreateSharedDurableUndurableConsumer_JRException failed");
        }
    }

    // InvalidDestinationRuntimeException if an invalid topic is specified.

    public void testCreateSharedDurableConsumer_Sel_InvalidDestination(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = tcfBindings.createContext();

        boolean testFailed = false;
        try {
            JMSConsumer jmsConsumer =
                jmsContext.createDurableConsumer(null, "DURATEST21", "Company = 'IBM'", true);
            testFailed = true;
        } catch ( InvalidDestinationRuntimeException ex ) {
            ex.printStackTrace();
        }

        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testCreateSharedDurableConsumer_Sel_InvalidDestination failed");
        }
    }

    public void testCreateSharedDurableConsumer_Sel_InvalidDestination_TCP(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = tcfTCP.createContext();

        boolean testFailed = false;
        try {
            JMSConsumer jmsConsumer =
                jmsContext.createDurableConsumer(null, "DURATEST22", "Company = 'IBM'", true);
            testFailed = true;
        } catch ( InvalidDestinationRuntimeException ex ) {
            ex.printStackTrace();
        }

        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testCreateSharedDurableConsumer_Sel_InvalidDestination_TCP failed");
        }
    }

    // Case where name is null and empty string

    public void testCreateSharedDurableConsumer_Sel_Null(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = tcfBindings.createContext();

        boolean testFailed = false;
        try {
            JMSConsumer jmsConsumer =
                jmsContext.createDurableConsumer(topic1, null, "Company = 'IBM'", true);
            testFailed = true;
        } catch ( JMSRuntimeException ex ) {
            ex.printStackTrace();
        }

        try {
            JMSConsumer jmsConsumer =
                jmsContext.createDurableConsumer(topic1, "", "Company = 'IBM'", true);
            testFailed = true;
        } catch ( JMSRuntimeException ex ) {
            ex.printStackTrace();
        }

        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testCreateSharedDurableConsumer_Sel_Null failed");
        }
    }

    public void testCreateSharedDurableConsumer_Sel_Null_TCP(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = tcfTCP.createContext();

        boolean testFailed = false;
        try {
            JMSConsumer jmsConsumer =
                jmsContext.createDurableConsumer(topic1, null, "Company = 'IBM'", true);
            testFailed = true;
        } catch ( JMSRuntimeException ex ) {
            ex.printStackTrace();
        }

        try {
            JMSConsumer jmsConsumer =
                jmsContext.createDurableConsumer(topic1, "", "Company = 'IBM'", true);
            testFailed = true;
        } catch ( JMSRuntimeException ex ) {
            ex.printStackTrace();
        }

        jmsContext.close();

        if ( testFailed ) {
            throw new Exception("testCreateSharedDurableConsumer_Sel_Null_TCP failed");
        }
    }
}
