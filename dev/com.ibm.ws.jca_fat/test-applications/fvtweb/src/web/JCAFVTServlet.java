/*******************************************************************************
 * Copyright (c) 2011,2020 IBM Corporation and others.
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
import java.lang.management.ManagementFactory;
import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.Set;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.ConnectionMetaData;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.management.MBeanServer;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.naming.InitialContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.UserTransaction;

import web.mdb.FVTMessageDrivenBean;
import web.mdb.bindings.FVTMessageDrivenBeanBinding;

public class JCAFVTServlet extends HttpServlet {
    private static final long serialVersionUID = 7709282314904580334L;

    private static String MBEAN_TYPE = "com.ibm.ws.jca.cm.mbean.ConnectionManagerMBean";

    /**
     * Message written to servlet to indicate that is has been successfully invoked.
     */
    public static final String SUCCESS_MESSAGE = "COMPLETED SUCCESSFULLY";

    /**
     * This state is used by certain tests to determine whether a Servlet instance,
     * and thus the application as a whole, survives a config update.
     */
    private String state = "NEW";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String test = request.getParameter("test");
        PrintWriter out = response.getWriter();
        out.println("Starting " + test + "<br>");
        System.out.println("-----> " + test + " starting");
        try {
            getClass().getMethod(test, HttpServletRequest.class, HttpServletResponse.class).invoke(this, request, response);
            System.out.println("<----- " + test + " successful");
            out.println(test + " COMPLETED SUCCESSFULLY");
        } catch (Throwable x) {
            if (x instanceof InvocationTargetException)
                x = x.getCause();
            System.out.println("<----- " + test + " failed:");
            x.printStackTrace(System.out);
            out.println("<pre>ERROR in " + test + ":");
            x.printStackTrace(out);
            out.println("</pre>");
        }
    }

    public void requireNewServletInstance(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        if (!"NEW".equals(state))
            throw new Exception("It appears that the existing servlet instance was used, meaning the app was not restarted. State: " + state);
    }

    public void requireServletInstanceStillActive(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        if (!"SERVLET_INSTANCE_STILL_ACTIVE".equals(state))
            throw new Exception("It appears that a different servlet instance was used, meaning the app was restarted. State: " + state);
    }

    public void resetState(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        state = "NEW";
    }

    public void setServletInstanceStillActive(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        state = "SERVLET_INSTANCE_STILL_ACTIVE";
    }

    /**
     * Test for activationSpec.
     */
    public void testActivationSpec(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        FVTMessageDrivenBean.messages.clear();

        ConnectionFactory cf = (ConnectionFactory) new InitialContext().lookup("java:comp/env/jms/cf1");
        Connection con = cf.createConnection();
        try {
            Session session = con.createSession(true, Session.AUTO_ACKNOWLEDGE);
            Queue queue = (Queue) new InitialContext().lookup("java:comp/env/jms/queue1");
            MessageProducer producer = session.createProducer(queue);
            MessageConsumer consumer = session.createConsumer(queue);

            con.start();

            TextMessage message = session.createTextMessage();
            message.setText("message:testActivationSpec");
            producer.setDeliveryMode(0xA); // Signal the fake resource adapter to use the JCA work manager to invoke onMessage asynchronously
            producer.send(message);

            consumer.receiveNoWait();

            session.close();
        } finally {
            con.close();
        }

        Iterator<String> results = FVTMessageDrivenBean.messages.iterator();
        String message = results.next();
        if (!"message:testActivationSpec".equals(message))
            throw new Exception("Did not get correct message. Instead: " + message);

        if (results.hasNext())
            throw new Exception("Got an extra message: " + results.next());
    }

    public void testActivationSpecBindings(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        FVTMessageDrivenBeanBinding.messages.clear();

        ConnectionFactory cf = (ConnectionFactory) new InitialContext().lookup("java:comp/env/jms/cf1");
        Connection con = cf.createConnection();
        try {
            Session session = con.createSession(true, Session.AUTO_ACKNOWLEDGE);
            Topic topic = (Topic) new InitialContext().lookup("java:comp/env/jms/topic2");
            MessageProducer producer = session.createProducer(topic);
            MessageConsumer consumer = session.createConsumer(topic);

            con.start();

            TextMessage message = session.createTextMessage();
            message.setText("message:testActivationSpecBindings");
            producer.setDeliveryMode(0xD); // Signal the fake resource adapter to use JCA timer to schedule onMessage
            producer.send(message);

            consumer.receiveNoWait();

            session.close();
        } finally {
            con.close();
        }

        Iterator<String> results = FVTMessageDrivenBeanBinding.messages.iterator();
        String message = results.next();
        if (!"message:testActivationSpecBindings".equals(message))
            throw new Exception("Did not get correct message. Instead: " + message);

        if (results.hasNext())
            throw new Exception("Got an extra message: " + results.next());
    }

    /**
     * Test for 2 jmsActivationSpecs both using the same topic: topic2.
     */
    public void testActivationSpecsBothUsingTopic2(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        FVTMessageDrivenBean.messages.clear();
        FVTMessageDrivenBeanBinding.messages.clear();

        ConnectionFactory cf = (ConnectionFactory) new InitialContext().lookup("java:comp/env/jms/cf1");
        Connection con = cf.createConnection();
        try {
            Session session = con.createSession(true, Session.AUTO_ACKNOWLEDGE);
            Topic topic = (Topic) new InitialContext().lookup("java:comp/env/jms/topic2");
            MessageProducer producer = session.createProducer(topic);
            MessageConsumer consumer = session.createConsumer(topic);

            con.start();

            TextMessage message = session.createTextMessage();
            message.setText("message:testActivationSpecsBothUsingTopic2");
            producer.send(message);

            consumer.receiveNoWait();

            session.close();
        } finally {
            con.close();
        }

        Iterator<String> results = FVTMessageDrivenBean.messages.iterator();
        String message = results.next();
        if (!"message:testActivationSpecsBothUsingTopic2".equals(message))
            throw new Exception("Did not get correct message for jmsActivationSpec with no bindings. Instead: " + message);

        if (results.hasNext())
            throw new Exception("Got an extra message for jmsActivationSpec with no bindings: " + results.next());

        results = FVTMessageDrivenBeanBinding.messages.iterator();
        message = results.next();
        if (!"message:testActivationSpecsBothUsingTopic2".equals(message))
            throw new Exception("Did not get correct message for jmsActivationSpec with bindings. Instead: " + message);

        if (results.hasNext())
            throw new Exception("Got an extra message for jmsActivationSpec with bindings: " + results.next());
    }

    /**
     * Look up a connection factory and send & receive a message.
     */
    public void testConnectionFactory(HttpServletRequest request, HttpServletResponse response) throws Throwable {

        ConnectionFactory cf = (ConnectionFactory) new InitialContext().lookup("java:comp/env/jms/cf1");
        Queue queue = (Queue) new InitialContext().lookup("java:comp/env/jms/queue1");

        Connection con = cf.createConnection();
        try {
            Session session = con.createSession(true, Session.AUTO_ACKNOWLEDGE);
            MessageConsumer consumer = session.createConsumer(queue);
            MessageProducer producer = session.createProducer(queue);

            TextMessage message = session.createTextMessage();
            message.setText("message1");
            producer.send(message);

            con.start();

            message = (TextMessage) consumer.receiveNoWait(); // read from Derby table

            String text = message.getText();
            if (!"message1".equals(text))
                throw new Exception("Received wrong message text: " + text);

            session.close();
        } finally {
            con.close();
        }
    }

    /**
     * Look up a connection factory and verify that is has the default clientID.
     */
    public void testConnectionFactoryClientIDDefault(HttpServletRequest request, HttpServletResponse response) throws Throwable {

        ConnectionFactory cf = (ConnectionFactory) new InitialContext().lookup("java:comp/env/jms/cf1");
        Connection con = cf.createConnection("ClientIDUser", "ClientIDPwd");
        try {
            String clientID = con.getClientID();
            if (!"defaultClientID".equals(clientID))
                throw new Exception("Expecting default clientID for connection, not " + clientID);
        } finally {
            con.close();
        }

        cf = (ConnectionFactory) new InitialContext().lookup("jms/cf1");
        con = cf.createConnection("ClientIDUser", "ClientIDPwd");
        try {
            String clientID = con.getClientID();
            if (!"defaultClientID".equals(clientID))
                throw new Exception("Expected default clientID for connection, not " + clientID);
        } finally {
            con.close();
        }
    }

    /**
     * Look up a connection factory and verify that is has an updated client ID.
     */
    public void testConnectionFactoryClientIDUpdated(HttpServletRequest request, HttpServletResponse response) throws Throwable {

        ConnectionFactory cf = (ConnectionFactory) new InitialContext().lookup("java:comp/env/jms/cf1");
        Connection con = cf.createConnection("ClientIDUser", "ClientIDPwd");
        try {
            String clientID = con.getClientID();
            if (!"updatedClientID".equals(clientID))
                throw new Exception("Expecting updatedClientID for connection, not " + clientID);
        } finally {
            con.close();
        }

        cf = (ConnectionFactory) new InitialContext().lookup("jms/cf1");
        con = cf.createConnection("ClientIDUser", "ClientIDPwd");
        try {
            String clientID = con.getClientID();
            if (!"updatedClientID".equals(clientID))
                throw new Exception("Expected updatedClientID for connection, not " + clientID);
        } finally {
            con.close();
        }
    }

    /**
     * Verify the userName used by cf1.
     */
    public void testConnectionFactoryUserDefault(HttpServletRequest request, HttpServletResponse response) throws Throwable {

        ConnectionFactory cf = (ConnectionFactory) new InitialContext().lookup("java:comp/env/jms/cf1");
        Connection con = cf.createConnection();
        try {
            Session session = con.createSession(true, Session.AUTO_ACKNOWLEDGE);
            Message message = session.createMessage();
            String userName = message.getStringProperty("userName");
            if (!"CF1USER".equals(userName))
                throw new Exception("Incorrect userName: " + userName);

            session.close();
        } finally {
            con.close();
        }
    }

    /**
     * This test is valid only after updating the configuration for cf1 to have user name = ACTV1USER
     */
    public void testConnectionFactoryUserUpdated1(HttpServletRequest request, HttpServletResponse response) throws Throwable {

        ConnectionFactory cf = (ConnectionFactory) new InitialContext().lookup("java:comp/env/jms/cf1");
        Connection con = cf.createConnection();
        try {
            Session session = con.createSession(true, Session.AUTO_ACKNOWLEDGE);
            Message message = session.createMessage();
            String userName = message.getStringProperty("userName");
            if (!"ACTV1USER".equals(userName))
                throw new Exception("Incorrect userName: " + userName);

            session.close();
        } finally {
            con.close();
        }
    }

    /**
     * This test is valid only after updating the configuration for cf1 to have user name = NEWUSER
     */
    public void testConnectionFactoryUserUpdated2(HttpServletRequest request, HttpServletResponse response) throws Throwable {

        ConnectionFactory cf = (ConnectionFactory) new InitialContext().lookup("java:comp/env/jms/cf1");
        Connection con = cf.createConnection();
        try {
            Session session = con.createSession(true, Session.AUTO_ACKNOWLEDGE);
            Message message = session.createMessage();
            String userName = message.getStringProperty("userName");
            if (!"NEWUSER".equals(userName))
                throw new Exception("Incorrect userName: " + userName);

            session.close();
        } finally {
            con.close();
        }
    }

    /**
     * Verify that Queue and Topic can be looked up as Destination.
     */
    public void testDestinations(HttpServletRequest request, HttpServletResponse response) throws Throwable {

        Destination destination1 = (Destination) new InitialContext().lookup("java:comp/env/jms/destination1");
        if (!(destination1 instanceof Queue))
            throw new Exception("Unexpected type for jms/destination1: " + destination1.getClass().getName());

        Destination destination2 = (Destination) new InitialContext().lookup("java:comp/env/jms/destination2");
        if (!(destination2 instanceof Topic))
            throw new Exception("Unexpected type for jms/destination2: " + destination1.getClass().getName());
    }

    /**
     * Verify that connections are unshared when enableSharingForDirectLookups=false
     */
    public void testEnableSharingForDirectLookupsFalse(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        ConnectionFactory cf = (ConnectionFactory) new InitialContext().lookup("jms/cf6");
        Connection con = null;
        Connection con2 = null;
        UserTransaction tran = (UserTransaction) new InitialContext().lookup("java:comp/UserTransaction");
        try {
            tran.begin();
            con = cf.createConnection();
            Session session = con.createSession(true, Session.AUTO_ACKNOWLEDGE);
            Queue queue = (Queue) new InitialContext().lookup("java:comp/env/jms/queue1");
            MessageConsumer consumer = session.createConsumer(queue);
            consumer.receiveNoWait();
            session.close();

            con2 = cf.createConnection();
            session = con2.createSession(true, Session.AUTO_ACKNOWLEDGE);
            queue = (Queue) new InitialContext().lookup("java:comp/env/jms/queue1");
            consumer = session.createConsumer(queue);
            consumer.receiveNoWait();
            session.close();
            tran.commit();
            throw new Exception("Connection is shared - enableSharingForDirectLookups property is not being honored");
        } catch (IllegalStateException x) {
            tran.rollback();
        } finally {
            con2.close();
            con.close();
        }
    }

    /**
     * testLoginModuleInJarInJarInRar - Look up a JMS connection factory using a resource reference that specifies to use a login module
     * that is packaged within a JAR within another JAR within a RAR.
     * Check the JMS provider name (to which the fake resource adapter appends the user name) to confirm that the login module is used.
     */
    public void testLoginModuleInJarInJarInRar(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        ConnectionFactory cf = (ConnectionFactory) new InitialContext().lookup("java:module/env/jms/jar1loginref");
        Connection con = cf.createConnection();
        try {
            ConnectionMetaData mdata = con.getMetaData();
            String name = mdata.getJMSProviderName();
            if (!name.endsWith("for jar1user"))
                throw new Exception("User name from login module not included in: " + name);
        } finally {
            con.close();
        }
    }

    /**
     * testLoginModuleInJarInRar - Look up a JMS connection factory using a resource reference that specifies to use a login module
     * that is packaged within a JAR within a RAR.
     * Check the JMS provider name (to which the fake resource adapter appends the user name) to confirm that the login module is used.
     */
    public void testLoginModuleInJarInRar(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        ConnectionFactory cf = (ConnectionFactory) new InitialContext().lookup("java:global/env/jms/jar2loginref");
        Connection con = cf.createConnection();
        try {
            ConnectionMetaData mdata = con.getMetaData();
            String name = mdata.getJMSProviderName();
            if (!name.endsWith("for jar2user"))
                throw new Exception("User name from login module not included in: " + name);
        } finally {
            con.close();
        }
    }

    /**
     * Verify that the connectionManager's maximum pool size is 1.
     */
    public void testMaxPoolSize1(HttpServletRequest request, HttpServletResponse response) throws Throwable {

        ConnectionFactory cf = (ConnectionFactory) new InitialContext().lookup("java:comp/env/jms/cf1");
        Connection con1 = cf.createConnection("user1", "pwd1");
        try {
            Connection con2 = cf.createConnection("user2", "pwd2");
            con2.close();
            throw new Exception("Should not be able to create a second connection given the expectation that maxPoolSize=1");
        } catch (JMSException x) {
            // Expected. Connection 2 should not be allowed when maxPoolSize=1
        } finally {
            con1.close();
        }
    }

    /**
     * Verify that the connectionManager's maximum pool size is 2.
     */
    public void testMaxPoolSize2(HttpServletRequest request, HttpServletResponse response) throws Throwable {

        ConnectionFactory cf = (ConnectionFactory) new InitialContext().lookup("java:comp/env/jms/cf1");
        Connection con1 = cf.createConnection("user1", "pwd1");
        try {
            Connection con2 = cf.createConnection("user2", "pwd2");
            try {
                Connection con3 = cf.createConnection("user3", "pwd3");
                con3.close();
                throw new Exception("Should not be able to create a third connection given the expectation that maxPoolSize=2");
            } catch (JMSException x) {
                // Expected. Connection 3 should not be allowed when maxPoolSize=2
            } finally {
                con2.close();
            }
        } finally {
            con1.close();
        }
    }

    /**
     * Verify that the connectionManager's maximum pool size is greater than 2.
     */
    public void testMaxPoolSizeGreaterThan2(HttpServletRequest request, HttpServletResponse response) throws Throwable {

        ConnectionFactory cf = (ConnectionFactory) new InitialContext().lookup("java:comp/env/jms/cf1");
        Connection con1 = cf.createConnection("user1", "pwd1");
        try {
            Connection con2 = cf.createConnection("user2", "pwd2");
            try {
                cf.createConnection("user3", "pwd3").close();
            } finally {
                con2.close();
            }
        } finally {
            con1.close();
        }
    }

    public void testPoolSizeDelegatedBEFORE(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        ConnectionFactory cf = (ConnectionFactory) new InitialContext().lookup("java:comp/env/jms/cf1");
        Connection con = cf.createConnection();
        try {
            ObjectInstance bean = getMBeanObjectInstance("jms/cf1");
            System.out.println(getPoolContents(bean));
        } finally {
            con.close();
        }

        ConnectionFactory ds6 = (ConnectionFactory) new InitialContext().lookup("java:comp/env/jms/cf6");

        Connection conn1 = ds6.createConnection();
        Connection conn2 = ds6.createConnection();

        conn1.start();
        conn2.close();

        // intentionally hang connection, will be verified in the AFTER servlet method
    }

    public void testPoolSizeDelegatedAFTER(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        ObjectInstance bean = getMBeanObjectInstance("jms/cf6");

        mbs.invoke(bean.getObjectName(), "purgePoolContents", null, null);

        if (getPoolSize(bean) != 0)
            throw new Exception("Pool size should be 0, but was not.");
    }

    /**
     * Look up a JMS queue and verify the queue name is correct.
     */
    public void testQueueNameDefault(HttpServletRequest request, HttpServletResponse response) throws Throwable {

        Queue queue = (Queue) new InitialContext().lookup("java:comp/env/jms/queue1");

        String queueName = queue.getQueueName();
        if (!"queue1".equals(queueName))
            throw new Exception("Unexpected queue name: " + queueName);
    }

    /**
     * Look up a JMS queue and verify the updated queue name is correct.
     */
    public void testQueueNameUpdated(HttpServletRequest request, HttpServletResponse response) throws Throwable {

        Queue queue = (Queue) new InitialContext().lookup("java:comp/env/jms/queue1");

        String queueName = queue.getQueueName();
        if (!"updatedQueueName".equals(queueName))
            throw new Exception("Unexpected queue name: " + queueName);
    }

    /**
     * Look up a connection factory for IMS
     */
    public void testIMSConnectionFactory(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        javax.resource.cci.ConnectionFactory cf = (javax.resource.cci.ConnectionFactory) new InitialContext().lookup("java:comp/env/ims/cf1");
        javax.resource.cci.Connection con = cf.getConnection();
        if (con == null)
            throw new Exception("Null connection obtained");
    }

    /**
     * Makes contact with a ConnectionFactory, which initializes an MBean.
     * Then, we run a query for the Bean to make sure it exists... and that
     * we find only 1 bean.
     */
    public void testMBeanCreation(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        ConnectionFactory cf = (ConnectionFactory) new InitialContext().lookup("java:comp/env/jms/cf1");
        Connection con = cf.createConnection();
        try {
            ObjectInstance bean = getMBeanObjectInstance("jms/cf1");

            System.out.println(getPoolContents(bean));
        } finally {
            con.close();
        }
    }

    /**
     * Starts a transaction, acquires a connection, closes connection and ends transaction,
     * then tries to purge the pool.
     */
    public void testMBeanPurge(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        ConnectionFactory cf = (ConnectionFactory) new InitialContext().lookup("java:comp/env/jms/cf1");
        Connection con = null;
        UserTransaction tran = (UserTransaction) new InitialContext().lookup("java:comp/UserTransaction");
        try {
            tran.begin();//Starting this removes the LTC
            con = cf.createConnection();

            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            ObjectInstance bean = getMBeanObjectInstance("jms/cf1");
            con.close();
            tran.commit(); // Transaction done, connection closed: able to be purged.

            // Purge pool of jms/cf1
            mbs.invoke(bean.getObjectName(), "purgePoolContents", null, null);

            // Pool size should be 0 after purge
            System.out.println("**  Pool contents after purge:\n" + getPoolContents(bean));
            if (getPoolSize(bean) != 0)
                throw new Exception("Not all connections were purged from the pool!");
        } finally {
            if (con != null)
                con.close();
        }
    }

    /**
     * Starts a transaction, acquires a connection, purge the pool, closes connection and ends transaction,
     * then checks to see if the pool is purged.
     */
    public void testMBeanPurgeDuringTransaction(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        ConnectionFactory cf = (ConnectionFactory) new InitialContext().lookup("java:comp/env/jms/cf1");
        Connection con = null;
        UserTransaction tran = (UserTransaction) new InitialContext().lookup("java:comp/UserTransaction");
        try {
            tran.begin(); // Starting this removes the LTC
            con = cf.createConnection();

            // Purge the pool of jms/cf1
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            ObjectInstance bean = getMBeanObjectInstance("jms/cf1");
            mbs.invoke(bean.getObjectName(), "purgePoolContents", null, null);

            // Show pool during tran for debugging purposes
            System.out.println("** Pool contents after purge but during tran:\n" + getPoolContents(bean));

            // Close con and finish tran so connections may be purged
            con.close();
            tran.commit();

            // Show pool contents after tran, verify size=0
            System.out.println("**  Pool contents after purge:\n" + getPoolContents(bean));
            if (getPoolSize(bean) != 0)
                throw new Exception("Not all connections were purged from the pool!");
        } finally {
            if (con != null)
                con.close();
        }
    }

    /**
     * Starts a transaction, acquires a connection, closes connection and ends transaction,
     * then tries to purge the pool using the "immediate" option.
     */
    public void testMBeanPurgeImmediate(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        ConnectionFactory cf = (ConnectionFactory) new InitialContext().lookup("java:comp/env/jms/cf1");
        Connection con = null;
        UserTransaction tran = (UserTransaction) new InitialContext().lookup("java:comp/UserTransaction");
        try {
            tran.begin(); // Starting this removes the LTC
            con = cf.createConnection();

            ObjectInstance bean = getMBeanObjectInstance("jms/cf1");
            con.close();
            tran.commit(); // Transaction done, connection closed: able to be purged.

            // Purge the pool
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            mbs.invoke(bean.getObjectName(), "purgePoolContents", new Object[] { "immediate" }, null);
            System.out.println("**  Pool contents after purge:\n" + getPoolContents(bean));
            if (getPoolSize(bean) != 0)
                throw new Exception("Not all connections were purged from the pool!");
        } finally {
            if (con != null)
                con.close();
        }
    }

    /**
     * Starts a transaction, acquires a connection, purge the pool, closes connection and ends transaction,
     * then checks to see if the pool is purged.
     */
    public void testMBeanPurgeImmediateDuringTransaction(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        ConnectionFactory cf = (ConnectionFactory) new InitialContext().lookup("java:comp/env/jms/cf1");
        Connection con = null;
        UserTransaction tran = (UserTransaction) new InitialContext().lookup("java:comp/UserTransaction");
        try {
            // Starting this removes the LTC
            tran.begin();
            con = cf.createConnection();

            // Purge the pool of jms/cf1
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            ObjectInstance bean = getMBeanObjectInstance("jms/cf1");
            mbs.invoke(bean.getObjectName(), "purgePoolContents", new Object[] { "immediate" }, null);

            // Note that doing an "immediate" purge during a tran will decrement the
            // total connection count, but there will still be 1 connection displayed
            // which will be waiting to be purged
            String duringTran = getPoolContents(bean);
            System.out.println("**  Pool contents during transaction:\n" + duringTran);
            if (!duringTran.contains("ActiveInTransactionToBePurged"))
                throw new Exception("Expected 1 connection waiting to be purged.");
            if (getPoolSize(bean) != 0)
                throw new Exception("Not all connections were purged from the pool!");

            // Transaction done, connection closed: able to be purged.
            con.close();
            tran.commit();

            System.out.println("**  Pool contents after purge:\n" + getPoolContents(bean));
            if (getPoolSize(bean) != 0)
                throw new Exception("Not all connections were purged from the pool!");
        } finally {
            if (con != null)
                con.close();
        }
    }

    /**
     * Using two resource-refs (one sharing, one unsharing), create a connection on both,
     * and make sure that purgePoolContents will deal with both types of connections.
     */
    public void testMBeanPurgeTwoResourceRef(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        ConnectionFactory cf1 = (ConnectionFactory) new InitialContext().lookup("java:comp/env/jms/cf1");
        ConnectionFactory cf2 = (ConnectionFactory) new InitialContext().lookup("java:comp/env/jms/cf1-unshareable");
        Connection con1 = null;
        Connection con2 = null;
        UserTransaction tran = (UserTransaction) new InitialContext().lookup("java:comp/UserTransaction");
        try {
            tran.begin();//Starting this removes the LTC
            con1 = cf1.createConnection();
            con2 = cf2.createConnection();

            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            ObjectInstance bean = getMBeanObjectInstance("jms/cf1");

            // Verify pool contents during a transaction
            String poolContentsDuring = getPoolContents(bean);
            System.out.println("**  Pool contents during transaction: \n" + poolContentsDuring);
            if (!poolContentsDuring.contains("shared=1"))
                throw new Exception("Wrong number of connections found in shared pool!");
            if (!poolContentsDuring.contains("unshared=1"))
                throw new Exception("Wrong number of connections found in unshared pool!");
            if (!poolContentsDuring.contains("size=2"))
                throw new Exception("Wrong total number of connections in the pool!");

            // Close connections, commit tran, then purge pool
            con1.close();
            con2.close();
            tran.commit();
            mbs.invoke(bean.getObjectName(), "purgePoolContents", null, null);

            // Verify pool is empty after pool is purged
            String poolContentsAfter = getPoolContents(bean);
            System.out.println("**  Pool contents after purge: \n" + poolContentsAfter);
            if (!poolContentsAfter.contains("size=0"))
                throw new Exception("Not all connections were purged from the pool!");
        } finally {
            if (con1 != null)
                con1.close();
            if (con2 != null)
                con2.close();
        }
    }

    /**
     * Makes contact with a ConnectionFactory, which initializes an MBean.
     * Then, we run a query for the Bean to make sure it exists... and that
     * we find only 1 bean.
     */
    public void testMBeanIsMissing(HttpServletRequest request, HttpServletResponse response) throws Throwable {

        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        ObjectName obn = new ObjectName("WebSphere:type=" + MBEAN_TYPE + ",jndiName=jms/cf1,*");
        Set<ObjectInstance> s = mbs.queryMBeans(obn, null);
        for (ObjectInstance bean : s)
            System.out.println("  ERROR: Found bean: " + bean.getObjectName());

        if (s.size() != 0)
            throw new Exception("Expected to find no MBeans.  Instead found: " + s.size());
    }

    public void testGetConnectionFactoryPoolContents(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        ConnectionFactory cf = (ConnectionFactory) new InitialContext().lookup("java:comp/env/jms/cf1");
        Connection con = cf.createConnection();
        try {
            // Get mbean for jms/cf1 and print out pool contents to the HttpServletResponse
            ObjectInstance bean = getMBeanObjectInstance("jms/cf1");
            String poolContents = getPoolContents(bean);
            System.out.println(poolContents);
            response.getWriter().append(poolContents);
        } finally {
            con.close();
        }
    }

    public void testJNDILookupConnectionFactory(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        @SuppressWarnings("unused")
        ConnectionFactory jms = (ConnectionFactory) new InitialContext().lookup("java:comp/env/jms/cf1");
    }

    public void testMissingCloseInServlet(HttpServletRequest request, HttpServletResponse response) throws Exception {

        ConnectionFactory connFact = (ConnectionFactory) new InitialContext().lookup("java:comp/env/jms/cf1");

        Connection conn1 = null;
        Connection conn2 = null;
        try {
            conn1 = connFact.createConnection();
            conn2 = connFact.createConnection();
        } catch (Exception e) {
            if (conn1 != null)
                conn1.close();
            if (conn2 != null) {
                conn2.close();
            }
            throw e;
        }
        conn1.close();

    }

    private ObjectInstance getMBeanObjectInstance(String jndiName) throws Exception {
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        ObjectName obn = new ObjectName("WebSphere:type=" + MBEAN_TYPE + ",jndiName=" + jndiName + ",*");
        Set<ObjectInstance> s = mbs.queryMBeans(obn, null);
        if (s.size() != 1) {
            System.out.println("ERROR: Found incorrect number of MBeans (" + s.size() + ")");
            for (ObjectInstance i : s)
                System.out.println("  Found MBean: " + i.getObjectName());
            throw new Exception("Expected to find exactly 1 MBean, instead found " + s.size());
        }
        return s.iterator().next();
    }

    private int getPoolSize(ObjectInstance bean) throws Exception {
        return Integer.parseInt((String) ManagementFactory.getPlatformMBeanServer().getAttribute(bean.getObjectName(), "size"));
    }

    private String getPoolContents(ObjectInstance bean) throws Exception {
        String contents = "   ";
        contents += (String) ManagementFactory.getPlatformMBeanServer().invoke(bean.getObjectName(), "showPoolContents", null, null);
        return contents.replace("\n", "\n   ");
    }
}
