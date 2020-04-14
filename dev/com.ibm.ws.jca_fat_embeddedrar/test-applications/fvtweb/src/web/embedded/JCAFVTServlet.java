/*******************************************************************************
 * Copyright (c) 2012, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package web.embedded;

import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.jms.Destination;
import javax.jms.Queue;
import javax.jms.Topic;
import javax.management.MBeanServer;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.naming.InitialContext;
import javax.resource.cci.Connection;
import javax.resource.cci.ConnectionFactory;
import javax.resource.cci.IndexedRecord;
import javax.resource.cci.Interaction;
import javax.resource.cci.RecordFactory;
import javax.resource.spi.work.Work;
import javax.resource.spi.work.WorkManager;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.UserTransaction;

import componenttest.app.FATServlet;
import fat.jca.testra.DummyMessage;
import fat.jca.testra.DummyQueue;
import fat.jca.testra.DummyResourceAdapter;

public class JCAFVTServlet extends FATServlet {
    private static final long serialVersionUID = 7709282314904580334L;

    public static MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();

    /**
     * Maximum number of milliseconds the tests should wait for something to happen.
     */
    private static long TIMEOUT = TimeUnit.MINUTES.toMillis(2);

    /**
     * Use an embedded resource adapter that we configured with
     * autoStart=true
     * and with a
     * contextService that includes jeeMetadataContext
     *
     * @param request
     *                     HTTP request
     * @param response
     *                     HTTP response
     * @throws Exception
     *                       if an error occurs.
     */
    public void testConfigurationOfEmbeddedResourceAdapter(HttpServletRequest request, HttpServletResponse response) throws Exception {
        // because autoStart = true, we shouldn't need to first use a resource from the resource adapter in order to get it to start
        WorkManager[] workManagers = DummyResourceAdapter.getWorkManagersForAllStartedInstances();
        if (workManagers.length != 1)
            throw new Exception("Should be exactly one started instance of our embedded resource adapter. List of workManagers is " + Arrays.asList(workManagers.length));

        WorkManager workManager = workManagers[0];

        final LinkedBlockingQueue<Object> results = new LinkedBlockingQueue<Object>();
        workManager.startWork(new Work() {
            @Override
            public void release() {
            }

            @Override
            public void run() {
                System.out.println("Running work");
                try {
                    // jeeMetadataContext is required to look up via a resource reference from the app that scheduled this work
                    Queue queue1 = (Queue) new InitialContext().lookup("java:comp/env/jms/queue1");
                    results.add(queue1.getQueueName());
                } catch (Throwable x) {
                    x.printStackTrace(System.out);
                    results.add(x);
                }
            }
        });

        Object result = results.poll(TIMEOUT, TimeUnit.MILLISECONDS);

        if (result == null)
            throw new Exception("Work did not complete within allotted interval");

        if (result instanceof Throwable)
            throw new Exception("Work failed", (Throwable) result);

        if (!"queue1".equals(result))
            throw new Exception("Administered object (JMS Queue) reports unexpected queue name: " + result);
    }

    /**
     * Look up an embedded connection factory and send & receive a message.
     *
     * @param request
     *                     HTTP request
     * @param response
     *                     HTTP response
     * @throws Exception
     *                       if an error occurs.
     */
    public void testEmbeddedConnectionFactory(HttpServletRequest request,
                                              HttpServletResponse response) throws Throwable {

        javax.resource.cci.ConnectionFactory cf = (javax.resource.cci.ConnectionFactory) new InitialContext()
                        .lookup("java:comp/env/ims/cf1");
        javax.resource.cci.Connection con = cf.getConnection();
        if (con == null)
            throw new Exception("Null connection obtained");
        // See if you can access an RA class
        try {
            Class.forName("com.ibm.connector2.ims.ico.IMSManagedConnectionFactory");
        } catch (Exception ex) {
            throw new Exception("RA Class is not visible to application");
        }
    }

    /**
     * Look up an embedded connection factory and and get a connection
     * for a 1.0 rar
     *
     * @param request
     *                     HTTP request
     * @param response
     *                     HTTP response
     * @throws Exception
     *                       if an error occurs.
     */
    public void testEmbeddedConnectionFactory10(HttpServletRequest request,
                                                HttpServletResponse response) throws Throwable {

        javax.resource.cci.ConnectionFactory cf = (javax.resource.cci.ConnectionFactory) new InitialContext()
                        .lookup("cci/cf1");
        javax.resource.cci.Connection con = cf.getConnection();
        if (con == null)
            throw new Exception("Null connection obtained");
        // See if you can access an RA class
        try {
            Class.forName("fat.jca.test10ra.FVT10ManagedConnectionFactory");
        } catch (Exception ex) {
            throw new Exception("RA Class is not visible to application");
        }
        Interaction interaction = con.createInteraction();
        RecordFactory recordFactory = cf.getRecordFactory();
        IndexedRecord input = recordFactory.createIndexedRecord("input");
        IndexedRecord output = recordFactory.createIndexedRecord("output");
        interaction.execute(null, input, output);
        Object result = output.get(0);
        if (!(result instanceof Date)) {
            throw new Exception("Unexpected result");
        }
    }

    /**
     * Test for activationSpec.
     *
     * @param request  HTTP request
     * @param response HTTP response
     * @throws Exception if an error occurs.
     */
    public void testEmbeddedActivationSpec(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        DummyQueue dq = (DummyQueue) new InitialContext().lookup("java:comp/env/jms/destination1");
        DummyMessage dm = new DummyMessage();
        dm.setText("message:testActivationSpec");
        dq.addMessage(dm);
    }

    /**
     * Verify that Queue and Topic can be looked up as Destination.
     *
     * @param request  HTTP request
     * @param response HTTP response
     * @throws Exception if an error occurs.
     */
    public void testEmbeddedDestinations(HttpServletRequest request, HttpServletResponse response) throws Throwable {

        Destination destination1 = (Destination) new InitialContext().lookup("java:comp/env/jms/destination1");
        if (!(destination1 instanceof Queue))
            throw new Exception("Unexpected type for jms/destination1: " + destination1.getClass().getName());

        Destination destination2 = (Destination) new InitialContext().lookup("java:comp/env/jms/destination2");
        if (!(destination2 instanceof Topic))
            throw new Exception("Unexpected type for jms/destination2: " + destination2.getClass().getName());
    }

    /**
     * Verify that a connection can be reauthenticated.
     *
     * @param request  HTTP request
     * @param response HTTP response
     * @throws Exception if an error occurs.
     */
    public void testReauthentication(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        InitialContext initialContext = new InitialContext();
        ConnectionFactory cf1ForUser1 = (ConnectionFactory) initialContext.lookup("java:comp/env/ims/cf1user1ref");
        ConnectionFactory cf1ForUser2 = (ConnectionFactory) initialContext.lookup("java:comp/env/ims/cf1user2ref");

        Object managedCon1 = null, managedCon2 = null;

        Connection con = cf1ForUser1.getConnection();
        try {
            String userName = con.getMetaData().getUserName();
            if (!"imsuser1".equals(userName))
                throw new Exception("Unexpected user name for ims/cf1user1ref: " + userName);

            Method getMyManagedConnection = con.getClass().getDeclaredMethod("getMyManagedConnection");
            getMyManagedConnection.setAccessible(true);
            managedCon1 = getMyManagedConnection.invoke(con);
        } finally {
            con.close();
        }

        // end the LTC and start a new transaction
        UserTransaction tran = (UserTransaction) initialContext.lookup("java:comp/UserTransaction");
        tran.begin();
        try {
            con = cf1ForUser2.getConnection();
            try {
                String userName = con.getMetaData().getUserName();
                if (!"imsuser2".equals(userName))
                    throw new Exception("Unexpected user name for ims/cf1user2ref: " + userName);

                Method getMyManagedConnection = con.getClass().getDeclaredMethod("getMyManagedConnection");
                getMyManagedConnection.setAccessible(true);
                managedCon2 = getMyManagedConnection.invoke(con);
            } finally {
                con.close();
            }
        } finally {
            tran.commit();
        }

        if (managedCon1 != managedCon2)
            throw new Exception("Same managed connection was not reused. Instead: " + managedCon1 + " and " + managedCon2);
    }

    /**
     * Verify that the connection pool stats are correctly incremented for a connection factory
     *
     * @param request  HTTP request
     * @param response HTTP response
     * @throws Exception if an error occurs.
     */
    public void testConnectionPoolStats(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        javax.resource.cci.ConnectionFactory cf = (javax.resource.cci.ConnectionFactory) new InitialContext()
                        .lookup("java:comp/env/ims/cf1");
        javax.resource.cci.Connection con = null;
        try {
            con = cf.getConnection();
            con.getMetaData();
            ObjectName objName = getConnPoolStatsMBeanObjName("cf1");
            int createCount = getMonitorData(objName, "CreateCount");
            int managedConnectionCount = getMonitorData(objName, "ManagedConnectionCount");

            if (createCount <= 0)
                throw new Exception("Expected the CreateCount to be above 0, but it is " + createCount);
            if (managedConnectionCount <= 0)
                throw new Exception("Expected the ManagedConnectionCount to be above 0, but it is " + managedConnectionCount);
        } finally {
            con.close();
        }

    }

    private int getMonitorData(ObjectName name, String attribute) throws Exception {
        return Integer.parseInt((mbeanServer.getAttribute(name, attribute)).toString());
    }

    private ObjectName getConnPoolStatsMBeanObjName(String dsName) throws Exception {
        Set<ObjectInstance> mxBeanSet;
        ObjectInstance oi;
        mxBeanSet = mbeanServer.queryMBeans(new ObjectName("WebSphere:type=ConnectionPoolStats,name=*" + dsName + "*"), null);
        if (mxBeanSet != null && mxBeanSet.size() > 0) {
            Iterator<ObjectInstance> it = mxBeanSet.iterator();
            oi = it.next();
            return oi.getObjectName();
        } else
            throw new Exception("ConnectionPoolStatsMBean:NotFound");
    }
}
