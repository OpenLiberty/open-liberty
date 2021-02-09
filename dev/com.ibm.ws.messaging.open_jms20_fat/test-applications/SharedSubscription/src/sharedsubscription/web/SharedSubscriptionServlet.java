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
import java.net.MalformedURLException;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.Set;

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
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import javax.management.remote.JMXServiceURL;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.MBeanServerConnection;
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

    public static QueueConnectionFactory qcfBindings;
    public static TopicConnectionFactory tcfBindings;
    public static TopicConnectionFactory tcfTCP;

    public static Queue queue;

    public static Topic topic1;
    public static Topic topic2_Expiry;
    public static Topic topic3;

    @Override
    public void init() throws ServletException {
        super.init();

        try {
            qcfBindings = (QueueConnectionFactory)
                new InitialContext().lookup("java:comp/env/jndi_JMS_BASE_QCF");
        } catch ( NamingException e ) {
            e.printStackTrace();
        }
        System.out.println("Queue connection factory 'java:comp/env/jndi_JMS_BASE_QCF':\n" + qcfBindings);

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
            queue = (Queue) new InitialContext().lookup("java:comp/env/jndi_INPUT_Q");
        } catch ( NamingException e ) {
            e.printStackTrace();
        }
        System.out.println("Queue 'java:comp/env/jndi_INPUT_Q':\n" + queue);

        try {
            topic1 = (Topic) new InitialContext().lookup("java:comp/env/eis/topic1");
        } catch ( NamingException e ) {
            e.printStackTrace();
        }
        System.out.println("Topic 'java:comp/env/eis/topic1':\n" + topic1);

        try {
            topic2_Expiry = (Topic) new InitialContext().lookup("java:comp/env/eis/topic2");
            // has property 'timeToLive="100"'
        } catch ( NamingException e ) {
            e.printStackTrace();
        }
        System.out.println("Topic 'java:comp/env/eis/topic3':\n" + topic2_Expiry);

        try {
            topic3 = (Topic) new InitialContext().lookup("java:comp/env/eis/topic3");
        } catch ( NamingException e ) {
            e.printStackTrace();
        }
        System.out.println("Topic 'java:comp/env/eis/topic3':\n" + topic3);

        if ( (qcfBindings == null) || (tcfBindings == null) || (tcfTCP == null) ||
             (queue == null) ||
             (topic1 == null) || (topic2_Expiry == null) || (topic3 == null) ) {
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
        TraceComponent tc = Tr.register( getClass() );

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

            out.println("<pre>ERROR in " + test + ":<br>");
            e.printStackTrace(out);
            out.println("</pre>");

            System.out.println(" Ending : " + test);
            System.out.println("ERROR in " + test + ":");
            e.printStackTrace(System.out);
            Tr.exit(this, tc, test, e);
        }
    }

    //

    // 129623_1 JMSConsumer createSharedDurableConsumer(Topic topic,String name)
    // 129623_1_1 Creates a shared durable subscription on the specified topic
    // (if one does not already exist) and creates a consumer on that durable
    // subscription. This method creates the durable subscription without a
    // message selector.

    // testCreateSharedDurableExpiry_B_SecOff
    //   testCreateSharedDurableConsumer_create_B_SecOff
    //   testCreateSharedDurableConsumer_create_Expiry_B_SecOff
    //   testCreateSharedDurableConsumer_consume_B_SecOff
    //   testCreateSharedDurableConsumer_consume_Expiry_B_SecOff

    public void testCreateSharedDurableConsumer_create_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextSender = tcfBindings.createContext();

        JMSContext jmsContextReceiver = tcfBindings.createContext();
        JMSConsumer jmsConsumer = jmsContextReceiver.createSharedDurableConsumer(topic1, "SUBID_B_D_NE");
        JMSProducer jmsProducer = jmsContextSender.createProducer();

        TextMessage tmsg = jmsContextSender.createTextMessage("This is a test message");
        jmsProducer.send(topic1, tmsg);

        System.out.println("testCreateSharedDurableExpiry_B_SecOff (send, non-durable, non-expiring):");
        System.out.println("Topic [ " + topic1 + " ] Topic ID [ " + "SUBID_B_D_NE" + " ]");
        System.out.println("Send [ " + tmsg + " ]");

        jmsConsumer.close();
        jmsContextReceiver.close();

        jmsContextSender.close();
    }

    public void testCreateSharedDurableConsumer_consume_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextReceiver = tcfBindings.createContext();
        JMSConsumer jmsConsumer = jmsContextReceiver.createSharedDurableConsumer(topic1, "SUBID_B_D_NE");
        TextMessage tmsg = (TextMessage) jmsConsumer.receive(30000);

        System.out.println("testCreateSharedDurableExpiry_B_SecOff (receive, durable, non-expiring):");
        System.out.println("Topic [ " + topic1 + " ] Topic ID [ " + "SUBID_B_D_NE" + " ]");
        System.out.println("Receive [ " + tmsg + " ] (should not be null)");

        jmsConsumer.close();
        jmsContextReceiver.unsubscribe("SUBID_B_D_NE");
        jmsContextReceiver.close();

        if ( tmsg == null ) {
            throw new Exception("testCreateSharedDurableConsumer_consume_B_SecOff failed (null message)");
        }
    }

    // testCreateSharedDurableExpiry_TCP_SecOff

    public void testCreateSharedDurableConsumer_create_TCP_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextSender = tcfTCP.createContext();

        JMSContext jmsContextReceiver = tcfTCP.createContext();
        JMSConsumer jmsConsumer = jmsContextReceiver.createSharedDurableConsumer(topic1, "SUBID_TCP_D_NE");

        JMSProducer jmsProducer = jmsContextSender.createProducer();
        TextMessage tmsg = jmsContextSender.createTextMessage("This is a test message");
        jmsProducer.send(topic1, tmsg);

        System.out.println("testCreateSharedDurableExpiry_TCP_SecOff (send, durable, non-expiring):");
        System.out.println("Topic [ " + topic1 + " ] Topic ID [ " + "SUBID_TCP_D_NE" + " ]");
        System.out.println("Send [ " + tmsg + " ]");

        jmsConsumer.close();
        jmsContextReceiver.close();

        jmsContextSender.close();
    }

    // testCreateSharedDurableExpiry_TCP_SecOff

    public void testCreateSharedDurableConsumer_consume_TCP_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextReceiver = tcfTCP.createContext();
        JMSConsumer jmsConsumer = jmsContextReceiver.createSharedDurableConsumer(topic1, "SUBID_TCP_D_NE");
        TextMessage tmsg = (TextMessage) jmsConsumer.receive(30000);

        System.out.println("testCreateSharedDurableExpiry_TCP_SecOff (receive, durable, non-expiring):");
        System.out.println("Topic [ " + topic1 + " ] Topic ID [ " + "SUBID_TCP_D_NE" + " ]");
        System.out.println("Receive [ " + tmsg + " ] (should not be null)");

        jmsConsumer.close();
        jmsContextReceiver.unsubscribe("SUBID_TCP_D_NE");
        jmsContextReceiver.close();

        if ( tmsg == null ) {
            throw new Exception("testCreateSharedDurableExpiry_TCP_SecOff failed");
        }
    }

    // 129623_1_3 The JMS provider retains a record of this durable subscription
    // and ensures that all messages from the topic's publishers are retained
    // until they are delivered to, and acknowledged by, a consumer on this
    // durable subscription or until they have expired.

    // testCreateSharedDurableExpiry_B_SecOff

    public void testCreateSharedDurableConsumer_create_Expiry_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextSender = tcfBindings.createContext();

        JMSContext jmsContextReceiver = tcfBindings.createContext();
        JMSConsumer jmsConsumer = jmsContextReceiver.createSharedDurableConsumer(topic2_Expiry, "EXPID_B_D_E");

        JMSProducer jmsProducer = jmsContextSender.createProducer();
        TextMessage tmsg = jmsContextSender.createTextMessage("This is a test message");
        jmsProducer.send(topic2_Expiry, tmsg);

        System.out.println("testCreateSharedDurableExpiry_B_SecOff (send, durable, expiring):");
        System.out.println("Topic [ " + topic2_Expiry + " ] Topic ID [ " + "EXPID_B_D_E" + " ]");
        System.out.println("Send [ " + tmsg + " ]");

        jmsConsumer.close();
        jmsContextReceiver.close();

        jmsContextSender.close();
    }

    // testCreateSharedDurableExpiry_B_SecOff

    public void testCreateSharedDurableConsumer_consume_Expiry_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        // The topic timeToLive is set to 200ms;
        // wait twice that long to force the message to expire.
        Thread.currentThread().sleep(2 * 200);

        JMSContext jmsContextReceiver = tcfBindings.createContext();
        JMSConsumer jmsConsumer = jmsContextReceiver.createSharedDurableConsumer(topic2_Expiry, "EXPID_B_D_E");

        TextMessage tmsg = (TextMessage) jmsConsumer.receive(30000);

        System.out.println("testCreateSharedDurableExpiry_B_SecOff (receive, durable, expiring):");
        System.out.println("Topic [ " + topic2_Expiry + " ] Topic ID [ " + "EXPID_B_D_E" + " ]");
        System.out.println("Receive [ " + tmsg + " ] (should be null / expired)");

        jmsConsumer.close();
        jmsContextReceiver.unsubscribe("EXPID_B_D_E");
        jmsContextReceiver.close();

        if ( tmsg != null ) {
            throw new Exception("testCreateSharedDurableConsumer_consume_Expiry_B_SecOff failed");
        }
    }

    // testCreateSharedDurableExpiry_TCP_SecOff 

    public void testCreateSharedDurableConsumer_create_Expiry_TCP_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextSender = tcfTCP.createContext();

        JMSContext jmsContextReceiver = tcfTCP.createContext();
        JMSConsumer jmsConsumer = jmsContextReceiver.createSharedDurableConsumer(topic2_Expiry, "EXPID_TCP_D_E");

        JMSProducer jmsProducer = jmsContextSender.createProducer();
        TextMessage tmsg = jmsContextSender.createTextMessage("This is a test message");
        jmsProducer.send(topic2_Expiry, tmsg);

        System.out.println("testCreateSharedDurableExpiry_B_SecOff (send, durable, expiring):");
        System.out.println("Topic [ " + topic2_Expiry + " ] Topic ID [ " + "EXPID_TCP_D_E" + " ]");
        System.out.println("Send [ " + tmsg + " ]");

        jmsConsumer.close();
        jmsContextReceiver.close();

        jmsContextSender.close();
    }

    // testCreateSharedDurableExpiry_TCP_SecOff

    public void testCreateSharedDurableConsumer_consume_Expiry_TCP_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        // The topic timeToLive is set to 200ms;
        // wait twice that long to force the message to expire.
        Thread.currentThread().sleep(2 * 200);

        JMSContext jmsContextReceiver = tcfTCP.createContext();
        JMSConsumer jmsConsumer = jmsContextReceiver.createSharedDurableConsumer(topic2_Expiry, "EXPID_TCP_D_E");

        TextMessage tmsg = (TextMessage) jmsConsumer.receive(30000);

        System.out.println("testCreateSharedDurableExpiry_B_SecOff (receive, durable, expiring):");
        System.out.println("Topic [ " + topic2_Expiry + " ] Topic ID [ " + "EXPID_TCP_D_E" + " ]");
        System.out.println("Receive [ " + tmsg + " ] (should be null / expired)");

        jmsConsumer.close();
        jmsContextReceiver.unsubscribe("EXPID_TCP_D_E");
        jmsContextReceiver.close();

        if ( tmsg != null ) {
            throw new Exception("testCreateSharedDurableConsumer_consume_Expiry_TCP_SecOff failed");
        }
    }

    // 129623_1_4 A durable subscription will continue to accumulate messages
    // until it is deleted using the unsubscribe method.

    public void testCreateSharedDurableConsumer_unsubscribe_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextTCFBindings = tcfBindings.createContext();

        JMSConsumer jmsConsumer = jmsContextTCFBindings.createSharedDurableConsumer(topic1, "DURATEST1");
        JMSProducer jmsProducer = jmsContextTCFBindings.createProducer();

        try {
            TextMessage msgOut = jmsContextTCFBindings.createTextMessage("This is a test message");
            jmsProducer.send(topic1, msgOut);
            TextMessage msgIn = (TextMessage) jmsConsumer.receive(30000);

        } finally {
            jmsConsumer.close();
            jmsContextTCFBindings.unsubscribe("DURATEST1");
        }

        boolean testFailed = false;

        JMXConnector localConnector =
            openLocalConnector(); // throws MalformedURLException, IOException

        try {
            MBeanServerConnection localEngine =
                localConnector.getMBeanServerConnection(); // throws IOException

            ObjectName name = new ObjectName(
                "WebSphere" +
                ":feature=wasJmsServer" +
                ",type=Subscriber" +
                ",name=clientID##DURATEST1"); // throws MalformedObjectNameException
            String obn = (String) localEngine.getAttribute(name, "Id");
            testFailed = true;

        } catch ( InstanceNotFoundException ex ) {
            ex.printStackTrace();

        } finally {
            closeLocalConnector(localConnector); // throws IOException
        }

        jmsContextTCFBindings.close();

        if ( testFailed ) {
            throw new Exception("testCreateSharedDurableConsumer_unsubscribe_B_SecOff failed");
        }
    }

    public void testCreateSharedDurableConsumer_unsubscribe_TCP_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextTCFTCP = tcfTCP.createContext();

        JMSConsumer jmsConsumer = jmsContextTCFTCP.createSharedDurableConsumer(topic1, "DURATEST2");
        JMSProducer jmsProducer = jmsContextTCFTCP.createProducer();

        try {
            TextMessage msgOut = jmsContextTCFTCP.createTextMessage("This is a test message");
            jmsProducer.send(topic1, msgOut);
            TextMessage msgIn = (TextMessage) jmsConsumer.receive(30000);
        } finally {
            jmsConsumer.close();
            jmsContextTCFTCP.unsubscribe("DURATEST2");
        }

        boolean testFailed = false;

        JMXConnector localConnector =
            openLocalConnector(); // throws MalformedURLException, IOException

        try {
            MBeanServerConnection localEngine =
                localConnector.getMBeanServerConnection(); // throws IOException

            ObjectName name = new ObjectName(
                "WebSphere" +
                ":feature=wasJmsServer" +
                ",type=Subscriber" +
                ",name=clientID##DURATEST2"); // throws MalformedObjectNameException

            String obn = (String) localEngine.getAttribute(name, "Id");
            testFailed = true;

        } catch ( InstanceNotFoundException ex ) {
            ex.printStackTrace();

        } finally {
            closeLocalConnector(localConnector); // throws IOException
        }

        jmsContextTCFTCP.close();

        if ( testFailed ) {
            throw new Exception("testCreateSharedDurableConsumer_unsubscribe_TCP_SecOff failed");
        }
    }

    // 129623_1_5 Any durable subscription created using this method will be
    // shared. This means that multiple active (i.e. not closed) consumers on
    // the subscription may exist at the same time. The term "consumer" here
    // means a JMSConsumer object in any client.
    // 129623_1_6 A shared durable subscription is identified by a name
    // specified by the client and by the client identifier (which may be
    // unset). An application which subsequently wishes to create a consumer on
    // that shared durable subscription must use the same client identifier.

    public void testBasicMDBTopic(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        TopicConnectionFactory fatTCF = (TopicConnectionFactory)
            new InitialContext().lookup("java:comp/env/jms/FAT_TCF");
        Topic fatTopic = (Topic)
            new InitialContext().lookup("java:comp/env/jms/FAT_TOPIC");

        JMSContext jmsContext = fatTCF.createContext();
        JMSProducer jmsPublisher = jmsContext.createProducer();

        int numMsgs = 10;
        for ( int msgNo = 0; msgNo < numMsgs; msgNo++ ) {
            jmsPublisher.send(fatTopic, "testBasicMDBTopic:" + msgNo);
        }

        jmsContext.close();
    }

    public void testBasicMDBTopic_TCP(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        TopicConnectionFactory fatTCF = (TopicConnectionFactory)
            new InitialContext().lookup("java:comp/env/jms/FAT_COMMS_TCF");
        Topic fatTopic = (Topic)
            new InitialContext().lookup("java:comp/env/jms/FAT_TOPIC");

        JMSContext jmsContext = fatTCF.createContext();
        JMSProducer jmsPublisher = jmsContext.createProducer();

        int numMsgs = 10;
        for ( int msgNo = 0; msgNo < numMsgs; msgNo++ ) {
            jmsPublisher.send(fatTopic, "testBasicMDBTopic:" + msgNo);
        }

        jmsContext.close();
     }

    // 129623_1_7 If a shared durable subscription already exists with the same
    // name and client identifier (if set), and the same topic and message
    // selector has been specified, then this method creates a JMSConsumer on
    // the existing shared durable subscription.

    public void testCreateSharedDurableConsumer_2Subscribers_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextTCFBindings = tcfBindings.createContext();

        JMSConsumer jmsConsumer = jmsContextTCFBindings.createSharedDurableConsumer(topic1, "PUBSUBTEST");
        JMSConsumer jmsConsumerCopy = null;

        CompositeData[] obn = null;
        try {
            JMSProducer jmsProducer = jmsContextTCFBindings.createProducer();

            TextMessage msgOut = jmsContextTCFBindings.createTextMessage("This is a test message");
            jmsProducer.send(topic1, msgOut);
            TextMessage msgIn = (TextMessage) jmsConsumer.receive(30000);

            jmsConsumerCopy = jmsContextTCFBindings.createSharedDurableConsumer(topic1, "PUBSUBTEST");

            obn = listSubscriptions("WebSphere:feature=wasJmsServer,type=Topic,name=NewTopic1");
            // throws MalformedURLException, IOException

        } finally {
            if ( jmsConsumerCopy != null ) {
                jmsConsumerCopy.close();
            }
            jmsConsumer.close();
            jmsContextTCFBindings.unsubscribe("PUBSUBTEST");
            jmsContextTCFBindings.close();
        }

        if ( obn.length != 1 ) {
            throw new Exception("testCreateSharedDurableConsumer_2Subscribers_B_SecOff failed");
        }
    }

    public void testCreateSharedDurableConsumer_2Subscribers_TCP_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextTCFTCP = tcfTCP.createContext();

        JMSConsumer jmsConsumer = jmsContextTCFTCP.createSharedDurableConsumer(topic1, "PUBSUBTEST");
        JMSProducer jmsProducer = jmsContextTCFTCP.createProducer();
        JMSConsumer jmsConsumerCopy = null;
        TextMessage msgIn2 = null;

        try {
            TextMessage msgOut = jmsContextTCFTCP.createTextMessage("This is a test message");
            jmsProducer.send(topic1, msgOut);

            TextMessage msgIn1 = (TextMessage) jmsConsumer.receive(30000);

            jmsConsumerCopy = jmsContextTCFTCP.createSharedDurableConsumer(topic1, "PUBSUBTEST");
            jmsProducer.send(topic1, msgIn1);
            msgIn2 = (TextMessage) jmsConsumerCopy.receive(30000);

        } finally {
            if ( jmsConsumerCopy != null ) {
                jmsConsumerCopy.close();
            }
            jmsConsumer.close();
            jmsContextTCFTCP.unsubscribe("PUBSUBTEST");
            jmsContextTCFTCP.close();
        }

        if ( msgIn2 == null ) {
            throw new Exception("testCreateSharedDurableConsumer_2Subscribers_TCP_SecOff failed");
        }
    }

    // 129623_1_8 If a shared durable subscription already exists with the same
    // name and client identifier (if set) but a different topic has been
    // specified, and there is no consumer already active (i.e. not closed) on
    // the durable subscription then this is equivalent to unsubscribing
    // (deleting) the old one and creating a new one.

    public void testCreateSharedDurableConsumer_2SubscribersDiffTopic_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextTCFBindings = tcfBindings.createContext();

        JMSConsumer jmsConsumer = jmsContextTCFBindings.createSharedDurableConsumer(topic1, "DURATEST123_B");
        JMSProducer jmsProducer = jmsContextTCFBindings.createProducer();

        try {
            TextMessage msgOut = jmsContextTCFBindings.createTextMessage("This is a test message");
            jmsProducer.send(topic1, msgOut);
            TextMessage msgIn = (TextMessage) jmsConsumer.receive(30000);
        } finally {
            jmsConsumer.close();
        }

        JMSConsumer jmsConsumer2 =
            jmsContextTCFBindings.createSharedDurableConsumer(topic1, "DURATEST123_B");

        CompositeData[] obn = null;
        try {
            obn = listSubscriptions("WebSphere:feature=wasJmsServer,type=Topic,name=NewTopic1");

        } finally {
            jmsConsumer2.close();
            jmsContextTCFBindings.unsubscribe("DURATEST123_B");
            jmsContextTCFBindings.close();
        }

        if ( obn.length != 1 ) {
            throw new Exception("testCreateSharedDurableConsumer_2SubscribersDiffTopic_B_SecOff failed");
        }
    }

    public void testCreateSharedDurableConsumer_2SubscribersDiffTopic_TCP_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        JMSContext jmsContextTCFTCP = tcfTCP.createContext();

        JMSConsumer jmsConsumer1 = jmsContextTCFTCP.createSharedDurableConsumer(topic1, "DURATEST123_TCP");
        JMSProducer jmsProducer = jmsContextTCFTCP.createProducer();
        TextMessage msgIn1;
        try {
            TextMessage msgOut = jmsContextTCFTCP.createTextMessage("This is a test message");
            jmsProducer.send(topic1, msgOut);
            msgIn1 = (TextMessage) jmsConsumer1.receive(30000);
        } finally {
            jmsConsumer1.close();
        }

        Thread.sleep(30000);

        JMSConsumer jmsConsumer2 = jmsContextTCFTCP.createSharedDurableConsumer(topic1, "DURATEST123_TCP");
        TextMessage msgIn2;
        try {
            jmsProducer.send(topic1, msgIn1);
            msgIn2 = (TextMessage) jmsConsumer2.receive(30000);
        } finally {
            jmsConsumer2.close();
            jmsContextTCFTCP.unsubscribe("DURATEST123_TCP");
            jmsContextTCFTCP.close();
        }

        if ( msgIn2 == null ) {
            throw new Exception("testCreateSharedDurableConsumer_2SubscribersDiffTopic_TCB_SecOff failed");
        }
    }

    // 129623_1_9 If a shared durable subscription already exists with the same
    // name and client identifier (if set) but a different topic has been
    // specified, and there is a consumer already active (i.e. not closed) on
    // the durable subscription, then a JMSRuntimeException will be thrown.

    public void testCreateSharedDurableConsumer_JRException_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextTCFBindings = tcfBindings.createContext();

        JMSConsumer jmsConsumer1 = jmsContextTCFBindings.createSharedDurableConsumer(topic1, "DURATEST456_B");

        boolean testFailed = false;

        try {
            JMSProducer jmsProducer = jmsContextTCFBindings.createProducer();
            TextMessage msgOut = jmsContextTCFBindings.createTextMessage("This is a test message");
            jmsProducer.send(topic1, msgOut);
            TextMessage msgIn = (TextMessage) jmsConsumer1.receive(30000);

            try {
                JMSConsumer jmsConsumer2 = jmsContextTCFBindings.createSharedDurableConsumer(topic3, "DURATEST456_B");
                testFailed = true;
            } catch ( JMSRuntimeException ex ) {
                ex.printStackTrace();
            }

        } finally {
            jmsConsumer1.close();
            jmsContextTCFBindings.unsubscribe("DURATEST456_B");
            jmsContextTCFBindings.close();
        }

        if ( testFailed ) {
            throw new Exception("testCreateSharedDurableConsumer_JRException_B_SecOff failed");
        }
    }

    public void testCreateSharedDurableConsumer_JRException_TCP_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextTCFTCP = tcfTCP.createContext();

        JMSConsumer jmsConsumer = jmsContextTCFTCP.createSharedDurableConsumer(topic1, "DURATEST456_TCP");

        boolean testFailed = false;

        try {
            JMSProducer jmsProducer = jmsContextTCFTCP.createProducer();
            TextMessage msgOut = jmsContextTCFTCP.createTextMessage("This is a test message");
            jmsProducer.send(topic1, msgOut);

            TextMessage msgIn = (TextMessage) jmsConsumer.receive(30000);

            try {
                JMSConsumer jmsConsumer1 =
                    jmsContextTCFTCP.createSharedDurableConsumer(topic3, "DURATEST456_TCP");
                testFailed = true;
            } catch ( JMSRuntimeException ex ) {
                ex.printStackTrace();
            }

        } finally {
            jmsConsumer.close();
            jmsContextTCFTCP.unsubscribe("DURATEST456_TCP");
            jmsContextTCFTCP.close();
        }

        if ( testFailed ) {
            throw new Exception("testCreateSharedDurableConsumer_JRException_TCP_SecOff failed");
        }
    }

    // 129623_1_10 A shared durable subscription and an unshared durable
    // subscription may not have the same name and client identifier (if set).
    // If an unshared durable subscription already exists with the same name and
    // client identifier (if set) then a JMSRuntimeException is thrown.

    public void testCreateSharedDurableUndurableConsumer_JRException_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextTCFBindings = tcfBindings.createContext();

        JMSConsumer jmsConsumer = jmsContextTCFBindings.createDurableConsumer(topic1, "DURATESTPS_B");

        boolean testFailed = false;

        try {
            JMSProducer jmsProducer = jmsContextTCFBindings.createProducer();
            TextMessage msgOut = jmsContextTCFBindings.createTextMessage("This is a test message");
            jmsProducer.send(topic1, msgOut);
            TextMessage msgIn = (TextMessage) jmsConsumer.receive(30000);

            try {
                JMSConsumer jmsConsumer1 =
                    jmsContextTCFBindings.createSharedDurableConsumer(topic1, "DURATESTPS_B");
                testFailed = true;
            } catch ( JMSRuntimeException ex ) {
                ex.printStackTrace();
            }

        } finally {
            jmsConsumer.close();
            jmsContextTCFBindings.unsubscribe("DURATESTPS_B");
            jmsContextTCFBindings.close();
        }

        if ( testFailed ) {
            throw new Exception("testCreateSharedDurableUndurableConsumer_JRException_B_SecOff failed");
        }
    }

    public void testCreateSharedDurableUndurableConsumer_JRException_TCP_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextTCFTCP = tcfTCP.createContext();

        JMSConsumer jmsConsumer = jmsContextTCFTCP.createDurableConsumer(topic1, "DURATESTPS_TCP");

        JMSProducer jmsProducer = jmsContextTCFTCP.createProducer();
        TextMessage msgOut = jmsContextTCFTCP.createTextMessage("This is a test message");
        jmsProducer.send(topic1, msgOut);

        TextMessage msgIn = (TextMessage) jmsConsumer.receive(30000);

        boolean testFailed = false;
        try {
            JMSConsumer jmsConsumer1 =
                jmsContextTCFTCP.createSharedDurableConsumer(topic1, "DURATESTPS_TCP");
            testFailed = true;
        } catch ( JMSRuntimeException ex ) {
            ex.printStackTrace();

        }

        jmsConsumer.close();
        jmsContextTCFTCP.unsubscribe("DURATESTPS_TCP");
        jmsContextTCFTCP.close();

        if ( testFailed ) {
            throw new Exception("testCreateSharedDurableUndurableConsumer_JRException_TCP_SecOff failed");
        }
    }

    // 129623_1_12 InvalidDestinationRuntimeException - if an invalid topic is specified.

    public void testCreateSharedDurableConsumer_InvalidDestination_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextTCFBindings = tcfBindings.createContext();

        boolean testFailed = false;
        try {
            JMSConsumer jmsConsumer =
                jmsContextTCFBindings.createSharedDurableConsumer(null, "DURATEST3");
            testFailed = true;
        } catch ( InvalidDestinationRuntimeException ex ) {
            ex.printStackTrace();
        }

        jmsContextTCFBindings.close();

        if ( testFailed ) {
            throw new Exception("testCreateSharedDurableConsumer_InvalidDestination_B_SecOff failed");
        }
    }

    public void testCreateSharedDurableConsumer_InvalidDestination_TCP_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextTCFTCP = tcfTCP.createContext();

        boolean testFailed = false;
        try {
            JMSConsumer jmsConsumer =
                jmsContextTCFTCP.createSharedDurableConsumer(null, "DURATEST4");
            testFailed = true;
        } catch ( InvalidDestinationRuntimeException ex ) {
            ex.printStackTrace();
        }

        jmsContextTCFTCP.close();

        if ( testFailed ) {
            throw new Exception("testCreateSharedDurableConsumer_InvalidDestination_TCP_SecOff failed");
        }
    }

    // 129623_1_13 Case where name is null and empty string

    public void testCreateSharedDurableConsumer_Null_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextTCFBindings = tcfBindings.createContext();

        boolean testFailed = false;

        try {
            JMSConsumer jmsConsumer =
                jmsContextTCFBindings.createSharedDurableConsumer(topic1, null);
            testFailed = true;
        } catch ( JMSRuntimeException ex ) {
            ex.printStackTrace();
        }

        try {
            JMSConsumer jmsConsumer =
                jmsContextTCFBindings.createSharedDurableConsumer(topic1, "");
            testFailed = true;
        } catch ( JMSRuntimeException ex ) {
            ex.printStackTrace();
        }

        jmsContextTCFBindings.close();

        if ( testFailed ) {
            throw new Exception("testCreateSharedDurableConsumer_Null_B_SecOff failed");
        }
    }

    public void testCreateSharedDurableConsumer_Null_TCP_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextTCFTCP = tcfTCP.createContext();

        boolean testFailed = false;

        try {
            JMSConsumer jmsConsumer =
                jmsContextTCFTCP.createSharedDurableConsumer(topic1, null);
            testFailed = true;
        } catch ( JMSRuntimeException ex ) {
            ex.printStackTrace();
        }

        try {
            JMSConsumer jmsConsumer =
                jmsContextTCFTCP.createSharedDurableConsumer(topic1, "");
            testFailed = true;
        } catch ( JMSRuntimeException ex ) {
            ex.printStackTrace();
        }

        jmsContextTCFTCP.close();

        if ( testFailed ) {
            throw new Exception("testCreateSharedDurableConsumer_Null_TCP_SecOff failed");
        }
    }

    // 129626_1 JMSConsumer createSharedConsumer(Topic topic,String sharedSubscriptionName)
    // 129626_1_1 Creates a shared non-durable subscription with the specified name on the
    // specified topic (if one does not already exist) and creates a consumer on that
    // subscription. This method creates the non-durable subscription without a message selector.
    // 129626_1_4 Non-durable subscription is not persisted and will be deleted (together with
    // any undelivered messages associated with it) when there are no consumers on it. The
    // term "consumer" here means a MessageConsumer or JMSConsumer object in any client.

    public void testCreateSharedNonDurableConsumer_create_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextSender = tcfBindings.createContext();
        JMSContext jmsContextReceiver = tcfBindings.createContext();
        JMSConsumer jmsConsumer = jmsContextReceiver.createSharedConsumer(topic1, "SUBID_B_ND_NE");

        JMSProducer jmsProducer = jmsContextSender.createProducer();
        TextMessage msgOut = jmsContextSender.createTextMessage("This is a test message");
        jmsProducer.send(topic1, msgOut);

        jmsConsumer.close();
        jmsContextReceiver.close();
        jmsContextSender.close();
    }

    public void testCreateSharedNonDurableConsumer_create_TCP_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextSender = tcfTCP.createContext();
        JMSContext jmsContextReceiver = tcfTCP.createContext();
        JMSConsumer jmsConsumer = jmsContextReceiver.createSharedConsumer(topic1, "SUBID_TCP_ND_NE");

        JMSProducer jmsProducer = jmsContextSender.createProducer();
        TextMessage msgOut = jmsContextSender.createTextMessage("This is a test message");
        jmsProducer.send(topic1, msgOut);

        jmsConsumer.close();
        jmsContextReceiver.close();
        jmsContextSender.close();
    }

    public void testCreateSharedNonDurableConsumer_consume_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextReceiver = tcfBindings.createContext();
        JMSConsumer jmsConsumer = jmsContextReceiver.createSharedConsumer(topic1, "SUBID_B_ND_NE");
        TextMessage msgOut = (TextMessage) jmsConsumer.receive(30000);

        boolean testFailed = false;
        if ( msgOut != null ) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContextReceiver.close();

        if ( testFailed ) {
            throw new Exception("testCreateSharedNonDurableConsumer_consume_B_SecOff failed");
        }
    }

    public void testCreateSharedNonDurableConsumer_consume_TCP_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextReceiver = tcfTCP.createContext();

        JMSConsumer jmsConsumer = jmsContextReceiver.createSharedConsumer(topic1, "SUBID_TCP_ND_NE");
        TextMessage msgIn = (TextMessage) jmsConsumer.receive(30000);

        boolean testFailed = false;
        if ( msgIn != null ) {
            testFailed = true;
        }

        jmsConsumer.close();
        jmsContextReceiver.close();

        if ( testFailed ) {
            throw new Exception("testCreateSharedNonDurableConsumer_consume_TCP_SecOff failed");
        }
    }

    // 129626_1_2 If a shared non-durable subscription already exists with the same name
    // and client identifier (if set), and the same topic and message selector has been
    // specified, then this method creates a JMSConsumer on the existing subscription.

    public void testCreateSharedNonDurableConsumer_2Subscribers_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextTCFBindings = tcfBindings.createContext();

        CompositeData[] obn1 = listSubscriptions("WebSphere:feature=wasJmsServer,type=Topic,name=NewTopic1");

        JMSConsumer jmsConsumer1 = jmsContextTCFBindings.createSharedConsumer(topic1, "TEST1");
        JMSProducer jmsProducer = jmsContextTCFBindings.createProducer();
        TextMessage msgOut = jmsContextTCFBindings.createTextMessage("This is a test message");
        jmsProducer.send(topic1, msgOut);
        TextMessage msgIn = (TextMessage) jmsConsumer1.receive(30000);

        JMSConsumer jmsConsumer2 = jmsContextTCFBindings.createSharedConsumer(topic1, "TEST1");

        CompositeData[] obn2 = listSubscriptions("WebSphere:feature=wasJmsServer,type=Topic,name=NewTopic1");

        boolean testFailed = false;
        int added = Math.abs(obn2.length - obn1.length);
        if ( added != 1 ) {
            testFailed = true;
        }

        jmsConsumer1.close();
        jmsConsumer2.close();
        jmsContextTCFBindings.close();

        if ( testFailed ) {
            throw new Exception("testCreateSharedNonDurableConsumer_2Subscribers_B_SecOff failed");
        }
    }

    public void testCreateSharedNonDurableConsumer_2Subscribers_TCP_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextTCFTCP = tcfTCP.createContext();

        JMSConsumer jmsConsumer1 = jmsContextTCFTCP.createSharedConsumer(topic1, "DURATEST5");
        JMSProducer jmsProducer = jmsContextTCFTCP.createProducer();
        TextMessage msgOut = jmsContextTCFTCP.createTextMessage("This is a test message");
        jmsProducer.send(topic1, msgOut);
        TextMessage msgIn1 = (TextMessage) jmsConsumer1.receive(30000);

        JMSConsumer jmsConsumer2 = jmsContextTCFTCP.createSharedConsumer(topic1, "DURATEST5");
        jmsProducer.send(topic1, msgIn1);
        TextMessage msgIn2 = (TextMessage) jmsConsumer2.receive(30000);

        boolean testFailed = false;
        if ( msgIn2 == null ) {
            testFailed = true;
        }

        jmsConsumer1.close();
        jmsConsumer2.close();
        jmsContextTCFTCP.close();

        if ( testFailed )  {
            throw new Exception("testCreateSharedNonDurableConsumer_2Subscribers_TCP_SecOff failed");
        }
    }

    // 129626_1_3 A non-durable shared subscription is used by a client which needs
    // to be able to share the work of receiving messages from a topic subscription
    // amongst multiple consumers. A non-durable shared subscription may therefore
    // have more than one consumer. Each message from the subscription will be
    // delivered to only one of the consumers on that subscription.

    public void testBasicMDBTopicNonDurable(
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        TopicConnectionFactory fatTCF = (TopicConnectionFactory)
            new InitialContext().lookup("java:comp/env/jms/FAT_TCF");
        Topic fatTopic = (Topic)
            new InitialContext().lookup("java:comp/env/jms/FAT_TOPIC");
        JMSContext jmsContext = fatTCF.createContext();
        JMSProducer jmsPublisher = jmsContext.createProducer();

        int numMsgs = 10;
        for ( int msgNo = 0; msgNo < numMsgs; msgNo++ ) {
            jmsPublisher.send(fatTopic, "testBasicMDBTopic:" + msgNo);
        }

        Thread.sleep(1000);
        jmsContext.close();
    }

    public void testBasicMDBTopicNonDurable_TCP(
         HttpServletRequest request, HttpServletResponse response) throws Exception {
    
        TopicConnectionFactory fatTCF = (TopicConnectionFactory)
            new InitialContext().lookup("java:comp/env/jms/FAT_TCF");
        Topic fatTopic = (Topic)
            new InitialContext().lookup("java:comp/env/jms/FAT_TOPIC");
        JMSContext jmsContext = fatTCF.createContext();
        JMSProducer jmsPublisher = jmsContext.createProducer();
    
        int numMsgs = 10;
        for ( int msgNo = 0; msgNo < numMsgs; msgNo++ ) {
            jmsPublisher.send(fatTopic, "testBasicMDBTopic:" + msgNo);
        }

        Thread.sleep(1000);
        jmsContext.close();
    }

    // 129626_1_6 If a shared non-durable subscription already exists with the
    // same name and client identifier (if set) but a different topic or message
    // selector value has been specified, and there is a consumer already active
    // (i.e. not closed) on the subscription, then a JMSRuntimeException will be thrown.

    public void testCreateSharedNonDurableConsumer_JRException_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextTCFBindings = tcfBindings.createContext();

        JMSConsumer jmsConsumer1 = jmsContextTCFBindings.createSharedConsumer(topic1, "DURATEST1_B");
        JMSProducer jmsProducer = jmsContextTCFBindings.createProducer();
        TextMessage msgOut = jmsContextTCFBindings.createTextMessage("This is a test message");
        jmsProducer.send(topic1, msgOut);
        TextMessage msgIn = (TextMessage) jmsConsumer1.receive(30000);

        boolean testFailed = false;
        try {
            JMSConsumer jmsConsumer2 = jmsContextTCFBindings.createSharedConsumer(topic3, "DURATEST1_B");
            testFailed = true;
        } catch ( JMSRuntimeException ex ) {
            ex.printStackTrace();
        }

        jmsConsumer1.close();
        jmsContextTCFBindings.close();

        if ( testFailed ) {
            throw new Exception("testCreateSharedNonDurableConsumer_JRException_B_SecOff failed");
        }
    }

    public void testCreateSharedNonDurableConsumer_JRException_TCP_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextTCFTCP = tcfTCP.createContext();

        JMSConsumer jmsConsumer1 = jmsContextTCFTCP.createSharedConsumer(topic1, "DURATEST1_TCP");
        JMSProducer jmsProducer = jmsContextTCFTCP.createProducer();
        TextMessage msgOut = jmsContextTCFTCP.createTextMessage("This is a test message");
        jmsProducer.send(topic1, msgOut);
        TextMessage msgIn = (TextMessage) jmsConsumer1.receive(30000);

        boolean testFailed = false;
        try {
            JMSConsumer jmsConsumer2 = jmsContextTCFTCP.createSharedConsumer(topic3, "DURATEST1_TCP");
            testFailed = true;
        } catch ( JMSRuntimeException ex ) {
            ex.printStackTrace();
        }

        jmsConsumer1.close();
        jmsContextTCFTCP.close();

        if ( testFailed ) {
            throw new Exception("testCreateSharedNonDurableConsumer_JRException_B_SecOff failed");
        }
    }

    // 129626_1_7 There is no restriction on durable subscriptions and shared non-durable
    // subscriptions having the same name and clientId (which may be unset). Such subscriptions
    // would be completely separate.

    public void testCreateSharedNonDurableConsumer_coexist_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextTCFBindings = tcfBindings.createContext();

        JMSConsumer jmsConsumer1 = jmsContextTCFBindings.createSharedConsumer(topic1, "DURATEST6");
        JMSConsumer jmsConsumer2 = null;

        TextMessage msgIn1;
        TextMessage msgIn2;

        try {
            JMSProducer jmsProducer1 = jmsContextTCFBindings.createProducer();
            TextMessage msgOut1 = jmsContextTCFBindings.createTextMessage("This is a test message");
            jmsProducer1.send(topic1, msgOut1);
            msgIn1 = (TextMessage) jmsConsumer1.receive(30000);

            jmsConsumer2 = jmsContextTCFBindings.createDurableConsumer(topic1, "DURATEST6");
            JMSProducer jmsProducer2 = jmsContextTCFBindings.createProducer();
            TextMessage msgOut2 = jmsContextTCFBindings.createTextMessage("This is a test message");
            jmsProducer2.send(topic1, msgOut2);
            msgIn2 = (TextMessage) jmsConsumer2.receive(30000);

        } finally {
            if ( jmsConsumer2 != null ) {
                jmsConsumer2.close();
            }
            jmsConsumer1.close();
            jmsContextTCFBindings.unsubscribe("DURATEST6");
            jmsContextTCFBindings.close();
        }

        if ( (msgIn1 == null) || (msgIn2 == null) ) {
            throw new Exception("testCreateSharedNonDurableConsumer_coexist_B_SecOff failed");
        }
    }

    public void testCreateSharedNonDurableConsumer_coexist_TCP_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextTCFBindings = tcfBindings.createContext();

        JMSConsumer jmsConsumer1 = jmsContextTCFBindings.createSharedConsumer(topic1, "DURATEST7");
        JMSConsumer jmsConsumer2 = null;

        TextMessage msgIn1;
        TextMessage msgIn2;

        try {
            JMSProducer jmsProducer1 = jmsContextTCFBindings.createProducer();
            TextMessage msgOut1 = jmsContextTCFBindings.createTextMessage("This is a test message");
            jmsProducer1.send(topic1, msgOut1);
            msgIn1 = (TextMessage) jmsConsumer1.receive(30000);

            jmsConsumer2 = jmsContextTCFBindings.createDurableConsumer(topic1, "DURATEST7");
            JMSProducer jmsProducer2 = jmsContextTCFBindings.createProducer();
            TextMessage msgOut2 = jmsContextTCFBindings.createTextMessage("This is a test message");
            jmsProducer2.send(topic1, msgOut2);
            msgIn2 = (TextMessage) jmsConsumer2.receive(30000);

        } finally {
            if ( jmsConsumer2 != null ) {
                jmsConsumer2.close();
            }
            jmsConsumer1.close();
            jmsContextTCFBindings.unsubscribe("DURATEST7");
            jmsContextTCFBindings.close();
        }

        if ( (msgIn1 == null) || (msgIn2 == null) ) {
            throw new Exception("testCreateSharedNonDurableConsumer_coexist_TCP_SecOff failed");
        }
    }

    // 129626_1_9 InvalidDestinationRuntimeException - if an invalid topic is specified.
    public void testCreateSharedNonDurableConsumer_InvalidDestination_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextTCFBindings = tcfBindings.createContext();

        boolean testFailed = false;
        try {
            JMSConsumer jmsConsumer =
                jmsContextTCFBindings.createSharedConsumer(null, "DURATEST8");
            testFailed = true;
        } catch ( InvalidDestinationRuntimeException ex ) {
            ex.printStackTrace();
        }

        jmsContextTCFBindings.close();

        if ( testFailed ) {
            throw new Exception("testCreateSharedNonDurableConsumer_InvalidDestination_B_SecOff failed");
        }
    }

    public void testCreateSharedNonDurableConsumer_InvalidDestination_TCP_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        JMSContext jmsContextTCFTCP = tcfTCP.createContext();

        boolean testFailed = false;
        try {
            JMSConsumer jmsConsumer =
                jmsContextTCFTCP.createSharedConsumer(null, "DURATEST9");
            testFailed = true;
        } catch ( InvalidDestinationRuntimeException ex ) {
            ex.printStackTrace();
        }

        jmsContextTCFTCP.close();

        if ( testFailed ) {
            throw new Exception("testCreateSharedNonDurableConsumer_InvalidDestination_TCP_SecOff failed");
        }
    }

    // Defect 174691

    public void testCreateSharedConsumer_Qsession_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        QueueConnection qconn = qcfBindings.createQueueConnection();
        qconn.start();
        QueueSession qsession = qconn.createQueueSession(false, QueueSession.AUTO_ACKNOWLEDGE);

        boolean testFailed = false;

        try {
            qsession.createSharedConsumer(topic1, "SUBID_B_QS");
            testFailed = true;
        } catch ( javax.jms.IllegalStateException ex ) {
            ex.printStackTrace();
        }
        try {
            qsession.createDurableSubscriber(topic1, "SUBID_B_QS");
            testFailed = true;
        } catch ( javax.jms.IllegalStateException ex ) {
            ex.printStackTrace();
        }

        try {
            qsession.createDurableConsumer(topic1, "SUBID_B_QS");
            testFailed = true;
        } catch ( javax.jms.IllegalStateException ex ) {
            ex.printStackTrace();
        }

        try {
            qsession.createSharedDurableConsumer(topic1, "SUBID_B_QS");
            testFailed = true;
        } catch ( javax.jms.IllegalStateException ex ) {
            ex.printStackTrace();
        }

        try {
            qsession.createTemporaryTopic();
            testFailed = true;
        } catch ( javax.jms.IllegalStateException ex ) {
            ex.printStackTrace();
        }

        try {
            qsession.createTopic("JustCreated");
            testFailed = true;
        } catch ( javax.jms.IllegalStateException ex ) {
            ex.printStackTrace();
        }

        try {
            qsession.unsubscribe("SUBID_B_QS");
            testFailed = true;
        } catch ( javax.jms.IllegalStateException ex ) {
            ex.printStackTrace();
        }

        qconn.close();

        if ( testFailed ) {
            throw new Exception("testCreateSharedConsumer_InvalidDestination_TCP_SecOff failed");
        }
    }

    // Defect 174713
    public void testUnsubscribeInvalidSID_Tsession_B_SecOff(
        HttpServletRequest request, HttpServletResponse response) throws Throwable {

        TopicConnectionFactory tcf = (TopicConnectionFactory)
            new InitialContext().lookup("java:comp/env/eis/tcf2");
        TopicConnection tconn = tcf.createTopicConnection();
        tconn.start();
        TopicSession tsession = tconn.createTopicSession(false, TopicSession.AUTO_ACKNOWLEDGE);

        boolean testFailed = false;
        try {
            tsession.unsubscribe("DummySID");
            testFailed = true;
        } catch ( InvalidDestinationException ex ) {
            ex.printStackTrace();
        }

        tconn.close();

        if ( testFailed ) {
            throw new Exception("testUnsubscribeInvalidSID_Tsession_B_SecOff failed");
        }
    }
}
