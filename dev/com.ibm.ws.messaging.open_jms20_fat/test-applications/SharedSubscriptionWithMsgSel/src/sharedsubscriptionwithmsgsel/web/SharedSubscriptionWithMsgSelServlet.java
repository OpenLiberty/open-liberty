/*******************************************************************************
 * Copyright (c) 2013,2021 IBM Corporation and others.
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
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.util.Set;

import javax.jms.InvalidDestinationRuntimeException;
import javax.jms.InvalidSelectorRuntimeException;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.JMSProducer;
import javax.jms.JMSRuntimeException;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.jms.TopicConnectionFactory;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanServerConnection;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
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

    public static TopicConnectionFactory jmsTCFBindings;
    public static TopicConnectionFactory jmsTCFTCP;

    public static Topic jmsTopic1;
    public static Topic jmsTopic2_Expiry;
    public static Topic jmsTopic3;

    @Override
    public void init() throws ServletException {
        super.init();

        try {
            jmsTCFBindings = (TopicConnectionFactory) new InitialContext().lookup("java:comp/env/eis/tcf");
        } catch (NamingException e) {
            e.printStackTrace();
        }
        System.out.println("Topic connection factory 'java:comp/env/eis/tcf':\n" + jmsTCFBindings);

        try {
            jmsTCFTCP = (TopicConnectionFactory) new InitialContext().lookup("java:comp/env/eis/tcf1");
        } catch (NamingException e) {
            e.printStackTrace();
        }
        System.out.println("Topic connection factory 'java:comp/env/eis/tcf1':\n" + jmsTCFTCP);

        try {
            jmsTopic1 = (Topic) new InitialContext().lookup("java:comp/env/eis/topic1");
        } catch (NamingException e) {
            e.printStackTrace();
        }
        System.out.println("Topic 'java:comp/env/eis/topic1':\n" + jmsTopic1);

        try {
            jmsTopic2_Expiry = (Topic) new InitialContext().lookup("java:comp/env/eis/topic2");

        } catch (NamingException e) {
            e.printStackTrace();
        }
        System.out.println("Topic 'java:comp/env/eis/topic2' (expiry):\n" + jmsTopic2_Expiry);

        try {
            jmsTopic3 = (Topic) new InitialContext().lookup("java:comp/env/eis/topic3");
        } catch (NamingException e) {
            e.printStackTrace();
        }
        System.out.println("Topic 'java:comp/env/eis/topic3':\n" + jmsTopic3);

        if ((jmsTCFBindings == null) || (jmsTCFTCP == null) ||
            (jmsTopic1 == null) || (jmsTopic2_Expiry == null) || (jmsTopic3 == null)) {
            throw new ServletException("Failed JMS initialization");
        }
    }

    //

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
        JMXServiceURL localConnectorURL = new JMXServiceURL(getLocalAddress()); // throws MalformedURLException
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

        for (ObjectInstance mbean : mbeans) {
            String mbeanPrintString = mbean.toString();
            if (!mbeanPrintString.contains("feature=wasJmsServer")) {
                continue;
            }
            System.out.println(
                               "[ " + mbean.getClassName() + " ]" +
                               " [ " + mbean.hashCode() + " ] [ " + mbean.getObjectName() + " ]");
            System.out.println(
                               "  [ " + mbeanPrintString + " ]");
        }
    }

    private CompositeData[] listSubscriptions(String nameText) throws Exception {
        JMXConnector localConnector = openLocalConnector(); // throws MalformedURLException, IOException

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

    //

    /**
     * Handle a GET request to this servlet: Invoke the test method specified as
     * request paramater "test".
     *
     * The test method throws an exception when it fails. If no exception
     * is thrown by the test method, indicate success through the response
     * output. If an exception is thrown, omit the success indication.
     * Instead, display an error indication and display the exception stack
     * to the response output.
     *
     * @param request  The HTTP request which is being processed.
     * @param response The HTTP response which is being processed.
     *
     * @throws ServletException Thrown in case of a servlet processing error.
     * @throws IOException      Thrown in case of an input/output error.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        String test = request.getParameter("test");
        setTest(test);

        // Read from the engine server at 'logs/state/com.ibm.ws.jmx.local.address'.
        String decodedLocalAddress = request.getParameter("localAddress");
        setLocalAddress(decodedLocalAddress);

        PrintWriter out = response.getWriter();
        out.println("Starting " + test + "<br>");

        // The injection engine doesn't like this at the class level.
        TraceComponent tc = Tr.register(getClass());

        Tr.entry(this, tc, test);
        try {
            System.out.println(" Starting : " + test);
            getClass().getMethod(test, HttpServletRequest.class, HttpServletResponse.class).invoke(this, request, response);
            out.println(test + " COMPLETED SUCCESSFULLY");
            System.out.println(" Ending : " + test);

            Tr.exit(this, tc, test);

        } catch (Throwable e) {
            if (e instanceof InvocationTargetException) {
                e = e.getCause();
            }

            out.println("<pre>ERROR in " + test + ":");
            e.printStackTrace(out);
            out.println("</pre>");

            System.out.println(" Ending : " + test);
            System.out.println("ERROR in " + test + ":");
            e.printStackTrace(System.out);

            Tr.exit(this, tc, test, e);
        }
    }

    //

    public void testCreateSharedDurableConsumerWithMsgSelector_create(
                                                                      HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContextSender = jmsTCFBindings.createContext();
        JMSContext jmsContextReceiver = jmsTCFBindings.createContext();
        JMSConsumer jmsConsumer = jmsContextReceiver.createSharedDurableConsumer(jmsTopic1, "SUBID00", "Company = 'IBM'");
        JMSProducer jmsProducer = jmsContextSender.createProducer();

        TextMessage msgOut = jmsContextSender.createTextMessage("Hello");
        msgOut.setStringProperty("Company", "IBM");
        jmsProducer.send(jmsTopic1, msgOut);

        jmsConsumer.close();
        jmsContextReceiver.close();
        jmsContextSender.close();
    }

    public void testCreateSharedDurableConsumerWithMsgSelector_consume(
                                                                       HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContextReceiver = jmsTCFBindings.createContext();
        JMSConsumer jmsConsumer = jmsContextReceiver.createSharedDurableConsumer(jmsTopic1, "SUBID00", "Company = 'IBM'");
        TextMessage msgIn = (TextMessage) jmsConsumer.receive(30000);

        boolean testFailed = false;
        if (msgIn == null) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContextReceiver.unsubscribe("SUBID00");
        jmsContextReceiver.close();

        if (testFailed) {
            throw new Exception("testCreateSharedDurableConsumerWithMsgSelector_consume failed");
        }
    }

    public void testCreateSharedDurableConsumerWithMsgSelector_create_TCP(
                                                                          HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContextSender = jmsTCFTCP.createContext();
        JMSContext jmsContextReceiver = jmsTCFTCP.createContext();
        JMSConsumer jmsConsumer = jmsContextReceiver.createSharedDurableConsumer(jmsTopic1, "SUBID01", "Company = 'IBM'");
        JMSProducer jmsProducer = jmsContextSender.createProducer();

        TextMessage msgOut = jmsContextSender.createTextMessage("This is a test message");
        msgOut.setStringProperty("Company", "IBM");
        jmsProducer.send(jmsTopic1, msgOut);

        jmsConsumer.close();
        jmsContextReceiver.close();
        jmsContextSender.close();
    }

    public void testCreateSharedDurableConsumerWithMsgSelector_consume_TCP(
                                                                           HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContextReceiver = jmsTCFTCP.createContext();
        JMSConsumer jmsConsumer = jmsContextReceiver.createSharedDurableConsumer(jmsTopic1, "SUBID01", "Company = 'IBM'");
        TextMessage msgIn = (TextMessage) jmsConsumer.receive(30000);

        boolean testFailed = false;
        if (msgIn == null) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContextReceiver.unsubscribe("SUBID01");
        jmsContextReceiver.close();

        if (testFailed) {
            throw new Exception("testCreateSharedDurableConsumerWithMsgSelector_consume_TCP failed");
        }
    }

    public void testCreateSharedDurableConsumerWithMsgSelector_create_Expiry(
                                                                             HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContextSender = jmsTCFBindings.createContext();
        JMSContext jmsContextReceiver = jmsTCFBindings.createContext();
        JMSConsumer jmsConsumer = jmsContextReceiver.createSharedDurableConsumer(jmsTopic2_Expiry, "SUBID02", "Company = 'IBM'");

        JMSProducer jmsProducer = jmsContextSender.createProducer();
        TextMessage msgOut = jmsContextSender.createTextMessage("testCreateSharedDurableConsumerWithMsgSelector_create_Expiry");
        msgOut.setStringProperty("Company", "IBM");
        jmsProducer.send(jmsTopic2_Expiry, msgOut);

        jmsConsumer.close();
        jmsContextReceiver.close();
        jmsContextSender.close();
    }

    public void testCreateSharedDurableConsumerWithMsgSelector_consumeAfterExpiry(
                                                                                  HttpServletRequest request, HttpServletResponse response) throws Exception {

        // The topic timeToLive is set to 200ms;
        // wait twice that long to force the message to expire.
        Thread.currentThread().sleep(2 * 200);

        JMSContext jmsContextReceiver = jmsTCFBindings.createContext();
        JMSConsumer jmsConsumer = jmsContextReceiver.createSharedDurableConsumer(jmsTopic2_Expiry, "SUBID02", "Company = 'IBM'");
        TextMessage msgIn = (TextMessage) jmsConsumer.receive(30000);

        boolean testFailed = false;
        if (msgIn != null) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContextReceiver.unsubscribe("SUBID02");
        jmsContextReceiver.close();

        if (testFailed) {
            throw new Exception("testCreateSharedDurableConsumerWithMsgSelector_consumeAfterExpiry failed");
        }
    }

    public void testCreateSharedDurableConsumerWithMsgSelector_create_Expiry_TCP(
                                                                                 HttpServletRequest request, HttpServletResponse response) throws Exception {

        // The topic timeToLive is set to 200ms;
        // wait twice that long to force the message to expire.
        Thread.currentThread().sleep(2 * 200);

        JMSContext jmsContextSender = jmsTCFTCP.createContext();
        JMSContext jmsContextReceiver = jmsTCFTCP.createContext();
        JMSConsumer jmsConsumer = jmsContextReceiver.createSharedDurableConsumer(jmsTopic2_Expiry, "SUBID03", "Company = 'IBM'");

        JMSProducer jmsProducer = jmsContextSender.createProducer();

        TextMessage msgOut = jmsContextSender.createTextMessage("testCreateSharedDurableConsumerWithMsgSelector_create_Expiry_TCP");
        msgOut.setStringProperty("Company", "IBM");

        jmsProducer.send(jmsTopic2_Expiry, msgOut);

        jmsConsumer.close();
        jmsContextReceiver.close();
        jmsContextSender.close();
    }

    public void testCreateSharedDurableConsumerWithMsgSelector_consumeAfterExpiry_TCP(
                                                                                      HttpServletRequest request, HttpServletResponse response) throws Exception {

        // The topic timeToLive is set to 200ms;
        // wait twice that long to force the message to expire.
        Thread.currentThread().sleep(2 * 200);

        JMSContext jmsContextReceiver = jmsTCFTCP.createContext();
        JMSConsumer jmsConsumer = jmsContextReceiver.createSharedDurableConsumer(jmsTopic2_Expiry, "SUBID03", "Company = 'IBM'");
        TextMessage msgIn = (TextMessage) jmsConsumer.receive(30000);

        boolean testFailed = false;
        if (msgIn != null) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContextReceiver.unsubscribe("SUBID03");
        jmsContextReceiver.close();

        if (testFailed) {
            throw new Exception("testCreateSharedDurableConsumerWithMsgSelector_consumeAfterExpiry_TCP failed");
        }
    }

    public void testCreateSharedDurableConsumerWithMsgSelector_unsubscribe(
                                                                           HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContextReceiver = jmsTCFBindings.createContext();

        JMSConsumer jmsConsumer = jmsContextReceiver.createSharedDurableConsumer(jmsTopic1, "UNSUB", "Company = 'IBM'");
        JMSProducer jmsProducer = jmsContextReceiver.createProducer();
        TextMessage msgOut = jmsContextReceiver.createTextMessage("This is a test message");
        msgOut.setStringProperty("Company", "IBM");
        jmsProducer.send(jmsTopic1, msgOut);
        TextMessage msgIn = (TextMessage) jmsConsumer.receive(30000);

        jmsConsumer.close();
        jmsContextReceiver.unsubscribe("UNSUB");

        boolean testFailed = false;
        JMXConnector localConnector = openLocalConnector(); // throws MalformedURLException, IOException
        try {
            MBeanServerConnection localEngine = localConnector.getMBeanServerConnection(); // throws IOException
            ObjectName name = new ObjectName("WebSphere:feature=wasJmsServer,type=Subscriber,name=clientID##UNSUB");
            // throws MalformedObjectNameException
            try {
                String obn = (String) localEngine.getAttribute(name, "Id");
                testFailed = true;
                System.out.println("Unexpectedly retrieved attribute [ " + name + " ] [ " + "Id" + " ] as [ " + obn + " ]");
            } catch (InstanceNotFoundException ex) {
                ex.printStackTrace();
            }
        } finally {
            closeLocalConnector(localConnector); // throws IOException
        }

        jmsContextReceiver.close();

        if (testFailed) {
            throw new Exception("testCreateSharedDurableConsumerWithMsgSelector_unsubscribe failed");
        }
    }

    public void testCreateSharedDurableConsumerWithMsgSelector_unsubscribe_TCP(
                                                                               HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContextReceiver = jmsTCFTCP.createContext();
        JMSConsumer jmsConsumer = jmsContextReceiver.createSharedDurableConsumer(jmsTopic1, "DURATEST0", "Company = 'IBM'");
        JMSProducer jmsProducer = jmsContextReceiver.createProducer();
        TextMessage msgOut = jmsContextReceiver.createTextMessage("This is a test message");
        msgOut.setStringProperty("Company", "IBM");
        jmsProducer.send(jmsTopic1, msgOut);
        TextMessage msgIn = (TextMessage) jmsConsumer.receive(30000);

        boolean testFailed = false;
        JMXConnector localConnector = openLocalConnector(); // throws MalformedURLException, IOException
        try {
            MBeanServerConnection localEngine = localConnector.getMBeanServerConnection(); // throws IOException
            ObjectName name = new ObjectName("WebSphere:feature=wasJmsServer,type=Subscriber,name=clientID##DURATEST");
            // throws MalformedObjectNameException
            try {
                String obn = (String) localEngine.getAttribute(name, "Id");
                testFailed = true;
                System.out.println("Unexpectedly retrieved attribute [ " + name + " ] [ " + "Id" + " ] as [ " + obn + " ]");
            } catch (InstanceNotFoundException ex) {
                ex.printStackTrace();
            }
        } finally {
            closeLocalConnector(localConnector); // throws IOException
        }

        jmsConsumer.close();
        jmsContextReceiver.unsubscribe("DURATEST0");
        jmsContextReceiver.close();

        if (testFailed) {
            throw new Exception("testCreateSharedDurableConsumerWithMsgSelector_unsubscribe_TCP failed");
        }
    }

    public void testCreateSharedDurableConsumerWithMsgSelector_2Subscribers(
                                                                            HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContextReceiver = jmsTCFBindings.createContext();

        JMSConsumer jmsConsumer1 = jmsContextReceiver.createSharedDurableConsumer(jmsTopic1, "DURATEST1", "Company = 'IBM'");
        JMSProducer jmsProducer = jmsContextReceiver.createProducer();
        TextMessage msgOut = jmsContextReceiver.createTextMessage("This is a test message");
        msgOut.setStringProperty("Company", "IBM");
        jmsProducer.send(jmsTopic1, msgOut);
        TextMessage msgIn = (TextMessage) jmsConsumer1.receive(30000);

        JMSConsumer jmsConsumer2 = jmsContextReceiver.createSharedDurableConsumer(jmsTopic1, "DURATEST1", "Company = 'IBM'");

        CompositeData[] obn = listSubscriptions("WebSphere:feature=wasJmsServer,type=Topic,name=NewTopic1");

        boolean testFailed = false;
        if (obn.length != 1) {
            testFailed = true;
        }

        jmsConsumer1.close();
        jmsConsumer2.close();
        jmsContextReceiver.unsubscribe("DURATEST1");
        jmsContextReceiver.close();

        if (testFailed) {
            throw new Exception("testCreateSharedDurableConsumerWithMsgSelector_2Subscribers failed [ " + obn.length + " ] [ " + obn + " ]");
        }
    }

    public void testCreateSharedDurableConsumerWithMsgSelector_2Subscribers_TCP(
                                                                                HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsTCFTCP.createContext();

        JMSConsumer jmsConsumer1 = jmsContext.createSharedDurableConsumer(jmsTopic1, "DURATEST2", "Company = 'IBM'");
        JMSProducer jmsProducer = jmsContext.createProducer();

        TextMessage msgOut = jmsContext.createTextMessage("This is a test message");
        msgOut.setStringProperty("Company", "IBM");
        jmsProducer.send(jmsTopic1, msgOut);
        TextMessage msgIn = (TextMessage) jmsConsumer1.receive(30000);

        JMSConsumer jmsConsumer2 = jmsContext.createSharedDurableConsumer(jmsTopic1, "DURATEST2", "Company = 'IBM'");

        CompositeData[] obn = listSubscriptions("WebSphere:feature=wasJmsServer,type=Topic,name=NewTopic1");

        boolean testFailed = false;
        if (obn.length != 1) {
            testFailed = true;
        }

        jmsConsumer1.close();
        jmsConsumer2.close();
        jmsContext.unsubscribe("DURATEST2");
        jmsContext.close();

        if (testFailed) {
            throw new Exception("testCreateSharedDurableConsumerWithMsgSelector_2Subscribers_TCP failed");
        }

    }

    public void testCreateSharedDurableConsumerWithMsgSelector_2SubscribersDiffTopic(
                                                                                     HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsTCFBindings.createContext();

        JMSConsumer jmsConsumer = jmsContext.createSharedDurableConsumer(jmsTopic1, "2SUBS", "Company = 'IBM'");
        JMSProducer jmsProducer = jmsContext.createProducer();
        TextMessage msgOut = jmsContext.createTextMessage("This is a test message");
        msgOut.setStringProperty("Company", "IBM");
        jmsProducer.send(jmsTopic1, msgOut);
        TextMessage msgIn = (TextMessage) jmsConsumer.receive(30000);

        CompositeData[] obn1 = listSubscriptions("WebSphere:feature=wasJmsServer,type=Topic,name=NewTopic1");

        jmsConsumer.close();

        JMSConsumer jmsConsumer2 = jmsContext.createSharedDurableConsumer(jmsTopic1, "2SUBS", "Company = 'IBM'");

        CompositeData[] obn2 = listSubscriptions("WebSphere:feature=wasJmsServer,type=Topic,name=NewTopic1");

        boolean testFailed = false;
        if (obn2.length != 1) {
            testFailed = true;
        }

        jmsConsumer2.close();
        jmsContext.unsubscribe("2SUBS");
        jmsContext.close();

        if (testFailed) {
            throw new Exception("testCreateSharedDurableConsumerWithMsgSelector_2SubscribersDiffTopic failed [ " + obn2.length + " ] [ " + obn2 + " ]");
        }
    }

    public void testCreateSharedDurableConsumerWithMsgSelector_2SubscribersDiffTopic_TCP(
                                                                                         HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsTCFTCP.createContext();

        JMSConsumer jmsConsumer1 = jmsContext.createSharedDurableConsumer(jmsTopic1, "DURATEST3", "Company = 'IBM'");
        JMSProducer jmsProducer = jmsContext.createProducer();

        TextMessage msgOut = jmsContext.createTextMessage("This is a test message");
        msgOut.setStringProperty("Company", "IBM");
        jmsProducer.send(jmsTopic1, msgOut);
        TextMessage msgIn = (TextMessage) jmsConsumer1.receive(30000);

        CompositeData[] obn1 = listSubscriptions("WebSphere:feature=wasJmsServer,type=Topic,name=NewTopic1");

        jmsConsumer1.close();

        JMSConsumer jmsConsumer2 = jmsContext.createSharedDurableConsumer(jmsTopic1, "DURATEST3", "Company = 'IBM'");

        CompositeData[] obn2 = listSubscriptions("WebSphere:feature=wasJmsServer,type=Topic,name=NewTopic1");

        boolean testFailed = false;
        if (obn2.length != 1) {
            testFailed = true;
        }

        jmsConsumer2.close();
        jmsContext.unsubscribe("DURATEST3");
        jmsContext.close();

        if (testFailed) {
            throw new Exception("testCreateSharedDurableConsumerWithMsgSelector_2SubscribersDiffTopic_TCP failed");
        }
    }

    public void testCreateSharedDurableConsumerWithMsgSelector_InvalidMsgSelector(
                                                                                  HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContextReceiver = jmsTCFBindings.createContext();

        boolean testFailed = false;
        try {
            JMSConsumer jmsConsumer = jmsContextReceiver.createSharedDurableConsumer(jmsTopic1, "SUBID04", "BAD SELECTOR");
            testFailed = true;
        } catch (InvalidSelectorRuntimeException ex) {
            ex.printStackTrace();
        }

        jmsContextReceiver.close();

        if (testFailed) {
            throw new Exception("testCreateSharedDurableConsumerWithMsgSelector_InvalidMsgSelector failed");
        }
    }

    public void testCreateSharedDurableConsumerWithMsgSelector_InvalidMsgSelector_TCP(
                                                                                      HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContextReceiver = jmsTCFBindings.createContext();

        boolean testFailed = false;
        try {
            JMSConsumer jmsConsumer = jmsContextReceiver.createSharedDurableConsumer(jmsTopic1, "SUBID05", "BAD SELECTOR");
            testFailed = true;
        } catch (InvalidSelectorRuntimeException ex) {
            ex.printStackTrace();
        }

        jmsContextReceiver.close();

        if (testFailed) {
            throw new Exception("testCreateSharedDurableConsumerWithMsgSelector_InvalidMsgSelector_TCP failed");
        }
    }

    public void testCreateSharedDurableConsumerWithMsgSelector_InvalidDestination(
                                                                                  HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsTCFBindings.createContext();

        boolean testFailed = false;
        try {
            JMSConsumer jmsConsumer = jmsContext.createSharedDurableConsumer(null, "DURATEST4", "Company = 'IBM'");
            testFailed = true;
        } catch (InvalidDestinationRuntimeException ex) {
            ex.printStackTrace();
        }

        jmsContext.close();

        if (testFailed) {
            throw new Exception("testCreateSharedDurableConsumerWithMsgSelector_InvalidDestination failed");
        }
    }

    public void testCreateSharedDurableConsumerWithMsgSelector_InvalidDestination_TCP(
                                                                                      HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsTCFTCP.createContext();

        boolean testFailed = false;
        try {
            JMSConsumer jmsConsumer = jmsContext.createSharedDurableConsumer(null, "DURATEST5", "Company = 'IBM'");
            testFailed = true;
        } catch (InvalidDestinationRuntimeException ex) {
            ex.printStackTrace();
        }

        jmsContext.close();

        if (testFailed) {
            throw new Exception("testCreateSharedDurableConsumerWithMsgSelector_InvalidDestination failed");
        }
    }

    public void testCreateSharedDurableConsumerWithMsgSelector_Null(
                                                                    HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsTCFBindings.createContext();

        boolean testFailed = false;

        try {
            JMSConsumer jmsConsumer = jmsContext.createSharedDurableConsumer(jmsTopic1, null, "Company = 'IBM'");
            testFailed = true;
        } catch (JMSRuntimeException ex) {
            ex.printStackTrace();
        }

        try {
            JMSConsumer jmsConsumer = jmsContext.createSharedDurableConsumer(jmsTopic1, "", "Company = 'IBM'");
            testFailed = true;
        } catch (JMSRuntimeException ex) {
            ex.printStackTrace();
        }

        jmsContext.close();

        if (testFailed) {
            throw new Exception("testCreateSharedDurableConsumerWithMsgSelector_Null failed");
        }
    }

    public void testCreateSharedDurableConsumerWithMsgSelector_Null_TCP(
                                                                        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsTCFTCP.createContext();

        boolean testFailed = false;

        try {
            JMSConsumer jmsConsumer = jmsContext.createSharedDurableConsumer(jmsTopic1, null, "Company = 'IBM'");
            testFailed = true;
        } catch (JMSRuntimeException ex) {
            ex.printStackTrace();
        }

        try {
            JMSConsumer jmsConsumer = jmsContext.createSharedDurableConsumer(jmsTopic1, "", "Company = 'IBM'");
            testFailed = true;
        } catch (JMSRuntimeException ex) {
            ex.printStackTrace();
        }

        jmsContext.close();

        if (testFailed) {
            throw new Exception("testCreateSharedDurableConsumerWithMsgSelector_Null_TCP failed");
        }
    }

    // https://javaee.github.io/javaee-spec/javadocs/javax/jms/JMSContext.html#createSharedDurableConsumer-javax.jms.Topic-java.lang.String-java.lang.String-

    // If a shared durable subscription already exists with the same
    // name and client identifier (if set), and the same topic and
    // message selector have been specified, then this method creates
    // a JMSConsumer on the existing shared durable subscription.

    // If a shared durable subscription already exists with the same
    // name and client identifier (if set), but a different topic or
    // message selector has been specified, and there is no consumer
    // already active (i.e. not closed) on the durable subscription
    // then this is equivalent to unsubscribing (deleting) the old one
    // and creating a new one.

    // If a shared durable subscription already exists with the same
    // name and client identifier (if set) but a different topic or
    // message selector has been specified, and there is a consumer
    // already active (i.e. not closed) on the durable subscription,
    // then a JMSRuntimeException will be thrown.

    public void testCreateSharedDurableConsumerWithMsgSelector_JRException(
                                                                           HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsTCFBindings.createContext();

        JMSConsumer jmsConsumer1 = jmsContext.createSharedDurableConsumer(jmsTopic1, "DURATEST6", "Company = 'IBM'");
        JMSProducer jmsProducer = jmsContext.createProducer();
        TextMessage msgOut = jmsContext.createTextMessage("This is a test message");
        msgOut.setStringProperty("Company", "IBM");
        jmsProducer.send(jmsTopic1, msgOut);
        TextMessage msgIn = (TextMessage) jmsConsumer1.receive(30000);

        boolean testFailed = false;
        try {
            JMSConsumer jmsConsumer2 = jmsContext.createSharedDurableConsumer(jmsTopic3, "DURATEST6", "Company = 'IBM'");
            testFailed = true;
        } catch (JMSRuntimeException ex) {
            ex.printStackTrace();
        }

        jmsConsumer1.close();
        jmsContext.unsubscribe("DURATEST6");
        jmsContext.close();

        if (testFailed) {
            throw new Exception("testCreateSharedDurableConsumerWithMsgSelector_JRException failed");
        }
    }

    public void testCreateSharedDurableConsumerWithMsgSelector_JRException_TCP(
                                                                               HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsTCFTCP.createContext();

        JMSConsumer jmsConsumer1 = jmsContext.createSharedDurableConsumer(jmsTopic1, "DURATEST7", "Company = 'IBM'");
        JMSProducer jmsProducer = jmsContext.createProducer();
        TextMessage msgOut = jmsContext.createTextMessage("This is a test message");
        msgOut.setStringProperty("Company", "IBM");
        jmsProducer.send(jmsTopic1, msgOut);
        TextMessage msgIn = (TextMessage) jmsConsumer1.receive(30000);

        boolean testFailed = false;
        try {
            JMSConsumer jmsConsumer2 = jmsContext.createSharedDurableConsumer(jmsTopic3, "DURATEST7", "Company = 'IBM'");
            testFailed = true;
        } catch (JMSRuntimeException ex) {
            ex.printStackTrace();
        }

        jmsConsumer1.close();
        jmsContext.unsubscribe("DURATEST7");
        jmsContext.close();

        if (testFailed) {
            throw new Exception("testCreateSharedDurableConsumerWithMsgSelector_JRException_TCP failed");
        }
    }

    // 129623_1_10 A shared durable subscription and an unshared durable
    // subscription may not have the same name and client identifier (if set).
    // If an unshared durable subscription already exists with the same name and
    // client identifier (if set) then a JMSRuntimeException is thrown.

    public void testCreateSharedDurableUndurableConsumerWithMsgSelector_JRException(
                                                                                    HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsTCFBindings.createContext();
        JMSConsumer jmsConsumer1 = jmsContext.createSharedDurableConsumer(jmsTopic1, "DURATEST8", "Company = 'IBM'");

        JMSProducer jmsProducer = jmsContext.createProducer();
        TextMessage msgOut = jmsContext.createTextMessage("This is a test message");
        msgOut.setStringProperty("Company", "IBM");
        jmsProducer.send(jmsTopic1, msgOut);
        TextMessage msgIn = (TextMessage) jmsConsumer1.receive(30000);

        boolean testFailed = false;
        try {
            JMSConsumer jmsConsumer2 = jmsContext.createDurableConsumer(jmsTopic1, "DURATEST8");
            testFailed = true;
        } catch (JMSRuntimeException ex) {
            ex.printStackTrace();
        }

        jmsConsumer1.close();
        jmsContext.unsubscribe("DURATEST8");
        jmsContext.close();

        if (testFailed) {
            throw new Exception("testCreateSharedDurableUndurableConsumerWithMsgSelector_JRException failed");
        }
    }

    public void testCreateSharedDurableUndurableConsumerWithMsgSelector_JRException_TCP(
                                                                                        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsTCFTCP.createContext();

        JMSConsumer jmsConsumer1 = jmsContext.createSharedDurableConsumer(jmsTopic1, "DURATEST9", "Company = 'IBM'");
        JMSProducer jmsProducer = jmsContext.createProducer();

        TextMessage msgOut = jmsContext.createTextMessage("This is a test message");
        msgOut.setStringProperty("Company", "IBM");
        jmsProducer.send(jmsTopic1, msgOut);
        TextMessage msgIn = (TextMessage) jmsConsumer1.receive(30000);

        boolean testFailed = false;
        try {
            JMSConsumer jmsConsumer2 = jmsContext.createDurableConsumer(jmsTopic1, "DURATEST9");
            testFailed = true;
        } catch (JMSRuntimeException ex) {
            ex.printStackTrace();
        }

        jmsConsumer1.close();
        jmsContext.unsubscribe("DURATEST9");
        jmsContext.close();

        if (testFailed) {
            throw new Exception("testCreateSharedDurableUndurableConsumerWithMsgSelector_JRException_TCP failed");
        }
    }

    public void testCreateSharedNonDurableConsumerWithMsgSelector_create(
                                                                         HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContextSender = jmsTCFBindings.createContext();

        JMSContext jmsContextReceiver = jmsTCFBindings.createContext();
        JMSConsumer jmsConsumer = jmsContextReceiver.createSharedConsumer(jmsTopic1, "SUBID06", "Company = 'IBM'");

        JMSProducer jmsProducer = jmsContextSender.createProducer();
        TextMessage msgOut = jmsContextSender.createTextMessage("testCreateSharedNonDurableConsumerWithMsgSelector_create");
        msgOut.setStringProperty("Company", "IBM");
        jmsProducer.send(jmsTopic1, msgOut);

        jmsConsumer.close();
        jmsContextReceiver.close();
        jmsContextSender.close();
    }

    public void testCreateSharedNonDurableConsumerWithMsgSelector_create_TCP(
                                                                             HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContextSender = jmsTCFTCP.createContext();

        JMSContext jmsContextReceiver = jmsTCFTCP.createContext();
        JMSConsumer jmsConsumer = jmsContextReceiver.createSharedConsumer(jmsTopic1, "SUBID07", "Company = 'IBM'");

        JMSProducer jmsProducer = jmsContextSender.createProducer();
        TextMessage msgOut = jmsContextSender.createTextMessage("testCreateSharedNonDurableConsumerWithMsgSelector_create_TCP");
        msgOut.setStringProperty("Company", "IBM");
        jmsProducer.send(jmsTopic1, msgOut);

        jmsConsumer.close();
        jmsContextReceiver.close();
        jmsContextSender.close();
    }

    public void testCreateSharedNonDurableConsumerWithMsgSelector_consume(
                                                                          HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContextReceiver = jmsTCFBindings.createContext();
        JMSConsumer jmsConsumer = jmsContextReceiver.createSharedConsumer(jmsTopic1, "SUBID06", "Company = 'IBM'");
        TextMessage msgIn = (TextMessage) jmsConsumer.receive(30000);

        boolean testFailed = false;
        if (msgIn != null) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContextReceiver.close();

        if (testFailed) {
            throw new Exception("testCreateSharedNonDurableConsumerWithMsgSelector_consume failed");
        }
    }

    public void testCreateSharedNonDurableConsumerWithMsgSelector_consume_TCP(
                                                                              HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContextReceiver = jmsTCFTCP.createContext();
        JMSConsumer jmsConsumer = jmsContextReceiver.createSharedConsumer(jmsTopic1, "SUBID07", "Company = 'IBM'");
        TextMessage msgIn = (TextMessage) jmsConsumer.receive(30000);

        boolean testFailed = false;
        if (msgIn != null) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContextReceiver.close();

        if (testFailed) {
            throw new Exception("testCreateSharedNonDurableConsumerWithMsgSelector_consume_TCP failed");
        }
    }

    // 129626_1_2 If a shared non-durable subscription already exists
    // with the same name and client identifier (if set), and the same
    // topic and message selector has been specified, then this method
    // creates a JMSConsumer on the existing subscription.

    public void testCreateSharedNonDurableConsumerWithMsgSelector_2Subscribers(
                                                                               HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContextTCFBindings = jmsTCFBindings.createContext();

        CompositeData[] obn1 = listSubscriptions("WebSphere:feature=wasJmsServer,type=Topic,name=NewTopic1");

        JMSConsumer jmsConsumer1 = jmsContextTCFBindings.createSharedConsumer(jmsTopic1, "TEST1", "Team = 'WAS'");
        JMSProducer jmsProducer = jmsContextTCFBindings.createProducer();

        TextMessage msgOut = jmsContextTCFBindings.createTextMessage("testCreateSharedNonDurableConsumerWithMsgSelector_2Subscribers");
        msgOut.setStringProperty("Team", "WAS");
        jmsProducer.send(jmsTopic1, msgOut);
        TextMessage msgIn = (TextMessage) jmsConsumer1.receive(30000);

        JMSConsumer jmsConsumer2 = jmsContextTCFBindings.createSharedConsumer(jmsTopic1, "TEST1", "Team = 'WAS'");

        CompositeData[] obn2 = listSubscriptions("WebSphere:feature=wasJmsServer,type=Topic,name=NewTopic1");

        int added = Math.abs(obn2.length - obn1.length);

        boolean testFailed = false;
        if (added != 1) {
            testFailed = true;
        }

        jmsConsumer1.close();
        jmsConsumer2.close();
        jmsContextTCFBindings.close();

        if (testFailed) {
            throw new Exception("testCreateSharedNonDurableConsumerWithMsgSelector_2Subscribers failed");
        }
    }

    public void testCreateSharedNonDurableConsumerWithMsgSelector_2Subscribers_TCP(HttpServletRequest request, HttpServletResponse response) 
        throws Exception {

        try (JMSContext jmsContext = jmsTCFTCP.createContext()) {

            CompositeData[] obn1 = listSubscriptions("WebSphere:feature=wasJmsServer,type=Topic,name=NewTopic1");

            JMSConsumer jmsConsumer1 = jmsContext.createSharedConsumer(jmsTopic1, "TEST2", "Team = 'WAS'");
            JMSProducer jmsProducer = jmsContext.createProducer();

            TextMessage msgOut = jmsContext.createTextMessage("testCreateSharedNonDurableConsumerWithMsgSelector_2Subscribers_TCP");
            msgOut.setStringProperty("Team", "WAS");
            jmsProducer.send(jmsTopic1, msgOut);
            TextMessage msgIn = (TextMessage) jmsConsumer1.receive(30000);

            JMSConsumer jmsConsumer2 = jmsContext.createSharedConsumer(jmsTopic1, "TEST2", "Team = 'WAS'");

            CompositeData[] obn2 = listSubscriptions("WebSphere:feature=wasJmsServer,type=Topic,name=NewTopic1");

            int added = Math.abs(obn2.length - obn1.length);

            if (added != 1)
                throw new Exception("testCreateSharedNonDurableConsumerWithMsgSelector_2Subscribers_TCP failed obn1="+obn1+" obn2="+obn2);
        }
    }

    // 129626_1_3 A non-durable shared subscription is used by a
    // client which needs to be able to share the work of receiving
    // messages from a topic subscription amongst multiple
    // consumers. A non-durable shared subscription may therefore
    // have more than one consumer. Each message from the
    // subscription will be delivered to only one of the consumers on
    // that subscription

    // 129626_1_6 If a shared non-durable subscription already exists
    // with the same name and client identifier (if set) but a
    // different topic or message selector value has been specified,
    // and there is a consumer already active (i.e. not closed) on
    // the subscription, then a JMSRuntimeException will be thrown.

    public void testCreateSharedNonDurableConsumerWithMsgSelector_JRException(
                                                                              HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsTCFBindings.createContext();

        JMSConsumer jmsConsumer1 = jmsContext.createSharedConsumer(jmsTopic1, "SUBID08", "Company = 'IBM'");
        JMSProducer jmsProducer = jmsContext.createProducer();

        TextMessage msgOut = jmsContext.createTextMessage("testCreateSharedNonDurableConsumerWithMsgSelector_JRException");
        msgOut.setStringProperty("Comapny", "IBM");
        jmsProducer.send(jmsTopic1, msgOut);
        TextMessage msgIn = (TextMessage) jmsConsumer1.receive(30000);

        boolean testFailed = false;
        try {
            JMSConsumer jmsConsumer2 = jmsContext.createSharedConsumer(jmsTopic3, "SUBID08", "Company = 'IBM'");
            testFailed = true;
        } catch (JMSRuntimeException ex) {
            ex.printStackTrace();
        }

        jmsConsumer1.close();
        jmsContext.close();

        if (testFailed) {
            throw new Exception("testCreateSharedNonDurableConsumerWithMsgSelector_JRException failed");
        }
    }

    public void testCreateSharedNonDurableConsumerWithMsgSelector_JRException_TCP(
                                                                                  HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsTCFBindings.createContext();

        JMSConsumer jmsConsumer1 = jmsContext.createSharedConsumer(jmsTopic1, "SUBID09", "Company = 'IBM'");
        JMSProducer jmsProducer = jmsContext.createProducer();

        TextMessage msgOut = jmsContext.createTextMessage("testCreateSharedNonDurableConsumerWithMsgSelector_JRException_TCP");
        msgOut.setStringProperty("Comapny", "IBM");
        jmsProducer.send(jmsTopic1, msgOut);
        TextMessage msgIn = (TextMessage) jmsConsumer1.receive(30000);

        boolean testFailed = false;
        try {
            JMSConsumer jmsConsumer2 = jmsContext.createSharedConsumer(jmsTopic3, "SUBID09", "Company = 'IBM'");
            testFailed = true;
        } catch (JMSRuntimeException ex) {
            ex.printStackTrace();
        }

        jmsConsumer1.close();
        jmsContext.close();

        if (testFailed) {
            throw new Exception("testCreateSharedNonDurableConsumerWithMsgSelector_JRException_TCP failed");
        }
    }

    // 129626_1_7  There is no restriction on durable subscriptions and shared non-durable subscriptions having
    // the same name and clientId (which may be unset). Such subscriptions would be completely separate.

    public void testCreateSharedNonDurableConsumerWithMsgSelector_coexist(
                                                                          HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsTCFBindings.createContext();

        JMSConsumer jmsConsumer1 = jmsContext.createSharedConsumer(jmsTopic1, "SUBID10", "Team = 'WAS'");
        JMSProducer jmsProducer1 = jmsContext.createProducer();

        TextMessage msg = jmsContext.createTextMessage("testCreateSharedNonDurableConsumerWithMsgSelector_coexist_msg1");
        msg.setStringProperty("Team", "WAS");
        jmsProducer1.send(jmsTopic1, msg);
        msg = (TextMessage) jmsConsumer1.receive(30000);

        JMSConsumer jmsConsumer2 = jmsContext.createDurableConsumer(jmsTopic1, "SUBID10", "Team = 'WAS'", false);
        JMSProducer jmsProducer2 = jmsContext.createProducer();

        TextMessage msg1 = jmsContext.createTextMessage("testCreateSharedNonDurableConsumerWithMsgSelector_coexist_msg2");
        msg1.setStringProperty("Team", "WAS");
        jmsProducer1.send(jmsTopic1, msg1);
        msg1 = (TextMessage) jmsConsumer2.receive(30000);

        boolean testFailed = false;
        if ((msg == null) || (msg1 == null)) {
            testFailed = true;
        }

        jmsConsumer1.close();
        jmsConsumer2.close();
        jmsContext.close();

        if (testFailed) {
            throw new Exception("testCreateSharedNonDurableConsumer_coexist failed");
        }
    }

    public void testCreateSharedNonDurableConsumerWithMsgSelector_coexist_TCP(
                                                                              HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsTCFTCP.createContext();

        JMSConsumer jmsConsumer1 = jmsContext.createSharedConsumer(jmsTopic1, "SUBID11", "Team = 'WAS'");
        JMSProducer jmsProducer1 = jmsContext.createProducer();

        TextMessage msg = jmsContext.createTextMessage("testCreateSharedNonDurableConsumerWithMsgSelector_coexist_TCP_msg1");
        msg.setStringProperty("Team", "WAS");
        jmsProducer1.send(jmsTopic1, msg);
        msg = (TextMessage) jmsConsumer1.receive(30000);

        JMSConsumer jmsConsumer2 = jmsContext.createDurableConsumer(jmsTopic1, "SUBID11", "Team = 'WAS'", false);
        JMSProducer jmsProducer2 = jmsContext.createProducer();

        TextMessage msg1 = jmsContext.createTextMessage("testCreateSharedNonDurableConsumerWithMsgSelector_coexist_TCP_msg2");
        msg1.setStringProperty("Team", "WAS");
        jmsProducer2.send(jmsTopic1, msg1);
        msg1 = (TextMessage) jmsConsumer2.receive(30000);

        boolean testFailed = false;
        if ((msg == null) || (msg1 == null)) {
            testFailed = true;
        }

        jmsConsumer1.close();
        jmsConsumer2.close();
        jmsContext.close();

        if (testFailed) {
            throw new Exception("testCreateSharedNonDurableConsumer_coexist_TCP failed");
        }
    }

    // 129626_1_9  InvalidDestinationRuntimeException - if an invalid topic is specified.

    public void testCreateSharedNonDurableConsumerWithMsgSelector_InvalidDestination(
                                                                                     HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsTCFBindings.createContext();

        boolean testFailed = false;
        try {
            JMSConsumer jmsConsumer = jmsContext.createSharedConsumer(null, "DURATEST10", "Company = 'IBM'");
            testFailed = true;
        } catch (InvalidDestinationRuntimeException ex) {
            ex.printStackTrace();
        }

        jmsContext.close();

        if (testFailed) {
            throw new Exception("testCreateSharedNonDurableConsumerWithMsgSelector_InvalidDestination failed");
        }
    }

    public void testCreateSharedNonDurableConsumerWithMsgSelector_InvalidDestination_TCP(
                                                                                         HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsTCFTCP.createContext();

        boolean testFailed = false;
        try {
            JMSConsumer jmsConsumer = jmsContext.createSharedConsumer(null, "DURATEST10", "Company = 'IBM'");
            testFailed = true;
        } catch (InvalidDestinationRuntimeException ex) {
            ex.printStackTrace();
        }

        jmsContext.close();

        if (testFailed) {
            throw new Exception("testCreateSharedNonDurableConsumerWithMsgSelector_InvalidDestination_TCP failed");
        }
    }

    public void testCreateSharedNonDurableConsumerWithMsgSelector_InvalidMsgSelector(
                                                                                     HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsTCFBindings.createContext();

        boolean testFailed = false;
        try {
            JMSConsumer jmsConsumer = jmsContext.createSharedDurableConsumer(jmsTopic1, "SUBID12", "BAD SELECTOR");
            testFailed = true;
        } catch (InvalidSelectorRuntimeException e) {
            e.printStackTrace();
        }

        jmsContext.close();

        if (testFailed) {
            throw new Exception("testCreateSharedNonDurableConsumerWithMsgSelector_InvalidMsgSelector failed");
        }
    }

    public void testCreateSharedNonDurableConsumerWithMsgSelector_InvalidMsgSelector_TCP(
                                                                                         HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContext = jmsTCFTCP.createContext();

        boolean testFailed = false;
        try {
            JMSConsumer jmsConsumer = jmsContext.createSharedDurableConsumer(jmsTopic1, "SUBID13", "BAD SELECTOR");
            testFailed = true;
        } catch (InvalidSelectorRuntimeException e) {
            e.printStackTrace();
        }

        jmsContext.close();

        if (testFailed) {
            throw new Exception("testCreateSharedNonDurableConsumerWithMsgSelector_InvalidMsgSelector_TCP failed");
        }
    }

    public void testBasicMDBTopic(HttpServletRequest request, HttpServletResponse response) 
        throws Exception {

        TopicConnectionFactory fatTCF = (TopicConnectionFactory) new InitialContext().lookup("java:comp/env/jms/FAT_TCF");
        Topic fatTopic = (Topic) new InitialContext().lookup("java:comp/env/jms/FAT_TOPIC");
        JMSContext jmsContext = fatTCF.createContext();
        JMSProducer jmsPublisher = jmsContext.createProducer();

        int msgs = 20;
        for (int msgNo = 0; msgNo < msgs; msgNo++) {
            jmsPublisher.send(fatTopic, "testBasicMDBTopic:" + msgNo);
        }

        jmsContext.close();
    }

    public void testBasicMDBTopic_TCP(HttpServletRequest request, HttpServletResponse response) 
        throws Exception {

        TopicConnectionFactory fatTCF = (TopicConnectionFactory) new InitialContext().lookup("java:comp/env/jms/FAT_COMMS_TCF");

        Topic fatTopic = (Topic) new InitialContext().lookup("java:comp/env/jms/FAT_TOPIC");
        JMSContext jmsContext = fatTCF.createContext();
        JMSProducer jmsPublisher = jmsContext.createProducer();

        int msgs = 20;
        for (int msgNo = 0; msgNo < msgs; msgNo++) {
            jmsPublisher.send(fatTopic, "testBasicMDBTopic_TCP:" + msgNo);
        }

        jmsContext.close();
    }
}
