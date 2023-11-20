/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package web;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;
import javax.annotation.Resource.AuthenticationType;
import javax.management.JMX;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanServer;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import javax.transaction.UserTransaction;

import com.ibm.ws.jca.cm.mbean.ConnectionManagerMBean;

public class ConnectionPoolStatsServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    @SuppressWarnings("unused")
    private final String className = "ConnectionPoolStatsServlet";
    /**
     * Message written to servlet to indicate that is has been successfully invoked.
     */
    public static final String SUCCESS_MESSAGE = "COMPLETED SUCCESSFULLY";

    private static final String MBEAN_TYPE = "com.ibm.ws.jca.cm.mbean.ConnectionManagerMBean";

    @Resource
    private UserTransaction tran;

    @Resource(name = "jdbc/ds1", shareable = false, authenticationType = AuthenticationType.APPLICATION)
    DataSource ds1;

    @Resource(name = "jdbc/ds1_maxconlimit", shareable = false, authenticationType = AuthenticationType.APPLICATION)
    DataSource ds1_maxconlimit;

    @Resource(name = "jdbc/ds1s", lookup = "jdbc/ds1", shareable = true, authenticationType = AuthenticationType.APPLICATION)
    DataSource ds1s;


    @Resource(name = "jdbc/sharedDS", shareable = true, authenticationType = AuthenticationType.APPLICATION)
    DataSource sharedDS;

    @Resource(name = "jdbc/exceptionInCreateDS", shareable = false, authenticationType = AuthenticationType.APPLICATION)
    DataSource exceptionInCreateDS;

    @Resource(name = "jdbc/waitTimeDS", shareable = false, authenticationType = AuthenticationType.APPLICATION)
    DataSource waitTimeDS;

    @Resource(name = "jdbc/multiThreadDS", shareable = false, authenticationType = AuthenticationType.APPLICATION)
    DataSource multiThreadDS;

    public static MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();

    String tableName = "COLORS";

    private static boolean firstTime = true;

    @Override
    public void init() throws ServletException {
        // since MBeans are lazy initialized we need to get a connection to ensure we can find the MBean
        Connection conn = null;
        try {
            conn = ds1.getConnection();
            Statement st = conn.createStatement();
            st.executeUpdate("CREATE TABLE " + tableName + " (id int not null primary key, color varchar(30))");
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

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

    public void testCountsAfterAbort(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        ObjectName objName = getConnPoolStatsMBeanObjName("ds1");
        //clear any connections
        mbeanServer.invoke(getMBeanObjectInstance("jdbc/ds1").getObjectName(), "purgePoolContents", new Object[] { "abort" }, null);

        Connection conn1 = ds1.getConnection();
        Connection conn = ds1.getConnection();
        try {
            int numFreePoolConns = getMonitorData(objName, "FreeConnectionCount");
            if (numFreePoolConns != 0)
                throw new Exception("Expected 0 connections in the free pool, but found " + numFreePoolConns);

            int numConnectionHandles = getMonitorData(objName, "ConnectionHandleCount");
            if (numConnectionHandles != 2)
                throw new Exception("Expected 2 connection handles, but found " + numConnectionHandles);

            int numManagedConns = getMonitorData(objName, "ManagedConnectionCount");
            if (numManagedConns != 2)
                throw new Exception("Expected 2 managed connections, but found " + numManagedConns);

            conn.close();

            numFreePoolConns = getMonitorData(objName, "FreeConnectionCount");
            if (numFreePoolConns != 1)
                throw new Exception("Expected 1 connections in the free pool, but found " + numFreePoolConns);

            numConnectionHandles = getMonitorData(objName, "ConnectionHandleCount");
            if (numConnectionHandles != 1)
                throw new Exception("Expected 1 connection handles, but found " + numConnectionHandles);

            numManagedConns = getMonitorData(objName, "ManagedConnectionCount");
            if (numManagedConns != 2)
                throw new Exception("Expected 2 managed connection, but found " + numManagedConns);
            int destroyCountBeforeAbort = getMonitorData(objName, "DestroyCount");

            Executor testExecutor = new Executor() {
                @Override
                public void execute(Runnable arg0) {
                    arg0.run();
                }
            };
            conn1.abort(testExecutor);

            numFreePoolConns = getMonitorData(objName, "FreeConnectionCount");
            if (numFreePoolConns != 1)
                throw new Exception("Expected 1 connections in the free pool, but found " + numFreePoolConns);

            numConnectionHandles = getMonitorData(objName, "ConnectionHandleCount");
            if (numConnectionHandles != 0)
                throw new Exception("Expected 0 connection handles, but found " + numConnectionHandles);
            numManagedConns = getMonitorData(objName, "ManagedConnectionCount");
            if (numManagedConns != 1)
                throw new Exception("Expected 1 managed connection, but found " + numManagedConns);
            int destroyCountAfterAbort = getMonitorData(objName, "DestroyCount");
            if (destroyCountAfterAbort != destroyCountBeforeAbort + 1)
                throw new Exception("Expected destroy count to increase by 1 after abort.  Before: " + destroyCountBeforeAbort + " After: " + destroyCountAfterAbort);

        } finally {
            if (conn != null && !conn.isClosed())
                conn.close();
            if (conn1 != null && !conn1.isClosed())
                conn1.close();
        }
    }

    public void testCountsAfterInUseInuseAbortNoIdlesArray(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        String test = "testCountsAfterInUseInuseAbortNoIdlesArray";
        ObjectName dsStatobjName = getConnPoolStatsMBeanObjName("ds1");
        ObjectInstance dsObjectName = getMBeanObjectInstance("jdbc/ds1");
        //clear any connections
        mbeanServer.invoke(dsObjectName.getObjectName(), "purgePoolContents", new Object[] { "abort" }, null);

        // get one shared and one unshared connection.
        Connection conn1shared = ds1s.getConnection();
        Connection connunshared = ds1.getConnection();

        try {
            // expected values before close and abort
            int expectNumFreePoolConns = 0;
            int expectedNumConnectionHandles = 2;
            int expectedNumManagedConns = 2;
            checkStats(test, dsStatobjName, dsObjectName, expectNumFreePoolConns, expectedNumConnectionHandles, expectedNumManagedConns);

            Thread.sleep(2); // simulate in use time running a query.

            connunshared.close();

            // expected values after close before abort
            expectNumFreePoolConns = 1;
            expectedNumConnectionHandles = 1;
            expectedNumManagedConns = 2;
            checkStats(test, dsStatobjName, dsObjectName, expectNumFreePoolConns, expectedNumConnectionHandles, expectedNumManagedConns);

            int destroyCountBeforeAbort = getMonitorData(dsStatobjName, "DestroyCount");
            String abortInfo = (String) mbeanServer.invoke(dsObjectName.getObjectName(), "abortConnections", new Object[] { "inuse", "2000" }, null); // 2 seconds should work for slow systems.
            String unexpectedString1 = "in use connections added to the in use abort list";
            String expectedString1 = "1 free connections not added to the in use abort list";
            String expectedString2 = "1 in use connections not added to the in use abort list";
            if (!abortInfo.contains(expectedString1)) {
                throw new Exception("Did not find exected return string " + expectedString1 + " \nString returned " + abortInfo);
            }
            if (!abortInfo.contains(expectedString2)) {
                throw new Exception("Did not find exected return string " + expectedString2 + " \nString returned " + abortInfo);
            }
            if (abortInfo.contains(unexpectedString1)) {
                throw new Exception("Found unexpected return string " + unexpectedString1 + " \nString returned " + abortInfo);
            }

            // expected values after close and abort
            expectNumFreePoolConns = 1;
            expectedNumConnectionHandles = 1;
            expectedNumManagedConns = 2;
            checkStats(test, dsStatobjName, dsObjectName, expectNumFreePoolConns, expectedNumConnectionHandles, expectedNumManagedConns);

            int expectNumberOfAddedAborts = 0;
            int destroyCountAfterAbort = getMonitorData(dsStatobjName, "DestroyCount");
            if (destroyCountAfterAbort != destroyCountBeforeAbort + expectNumberOfAddedAborts)
                throw new Exception("Expected destroy count to increase by " + expectNumberOfAddedAborts + " after abort.  Before: " + destroyCountBeforeAbort + " After: "
                                    + destroyCountAfterAbort + "\n"
                                    + (String) mbeanServer.invoke(dsObjectName.getObjectName(), "showPoolContents", null, null));
        } finally {
            if (connunshared != null && !connunshared.isClosed())
                connunshared.close();
            if (conn1shared != null && !conn1shared.isClosed())
                conn1shared.close();
        }
    }

    /**
     * @param dsStatobjName
     * @param expectNumFreePoolConns
     * @param expectedNumConnectionHandles
     * @param expectedNumManagedConns
     * @param dsObjectName
     * @throws Exception
     */
    private void checkStats(String test, ObjectName dsStatobjName, ObjectInstance dsObjectName, int expectNumFreePoolConns, int expectedNumConnectionHandles,
                            int expectedNumManagedConns) throws Exception {
        int numFreePoolConns = getMonitorData(dsStatobjName, "FreeConnectionCount");
        int numConnectionHandles = getMonitorData(dsStatobjName, "ConnectionHandleCount");
        int numManagedConns = getMonitorData(dsStatobjName, "ManagedConnectionCount");
        System.out.println(test + " - FreeConnectionCount=" + numFreePoolConns + " ConnectionHandleCount=" + numConnectionHandles + " numManagedConns="
                           + numManagedConns);
        if (numFreePoolConns != expectNumFreePoolConns)
            throw new Exception("Expected " + expectNumFreePoolConns + " connections in the free pool, but found " + numFreePoolConns + "\n"
                                + (String) mbeanServer.invoke(dsObjectName.getObjectName(), "showPoolContents", null, null));
        if (numConnectionHandles != expectedNumConnectionHandles)
            throw new Exception("Expected " + expectedNumConnectionHandles + " connection handles, but found " + numConnectionHandles + "\n"
                                + (String) mbeanServer.invoke(dsObjectName.getObjectName(), "showPoolContents", null, null));
        if (numManagedConns != expectedNumManagedConns)
            throw new Exception("Expected " + expectedNumManagedConns + " managed connections, but found " + numManagedConns + "\n"
                                + (String) mbeanServer.invoke(dsObjectName.getObjectName(), "showPoolContents", null, null));
    }

    public void testCountsAfterInUseSharedInuseAbortConnectionManagerMBean(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        String test = "testCountsAfterInUseSharedInuseAbortConnectionManagerMBean";
        ObjectName dsStatobjName = getConnPoolStatsMBeanObjName("ds1");
        ObjectInstance dsObjectName = getMBeanObjectInstance("jdbc/ds1");
        ConnectionManagerMBean cmb = getConnectionManagerBean("jdbc/ds1");
        //clear any connections
        cmb.purgePoolContents("abort");

        // get one shared and one unshared connection.
        Connection conn1shared = ds1s.getConnection();
        Connection connunshared = ds1.getConnection();

        try {
            // expected values before close and abort
            int expectNumFreePoolConns = 0;
            int expectedNumConnectionHandles = 2;
            int expectedNumManagedConns = 2;
            checkStats(test, dsStatobjName, dsObjectName, expectNumFreePoolConns, expectedNumConnectionHandles, expectedNumManagedConns);

            Thread.sleep(2); // simulate in use time running a query.

            connunshared.close();

            // expected values after close before abort
            expectNumFreePoolConns = 1;
            expectedNumConnectionHandles = 1;
            expectedNumManagedConns = 2;
            checkStats(test, dsStatobjName, dsObjectName, expectNumFreePoolConns, expectedNumConnectionHandles, expectedNumManagedConns);

            int destroyCountBeforeAbort = getMonitorData(dsStatobjName, "DestroyCount");
            String abortInfo = cmb.abortConnections("inuse", (long) 1);
            String expectedString1 = "1 in use connections added to the in use abort list";
            String expectedString2 = "1 free connections not added to the in use abort list";
            String unexpectedString1 = "in use connections not added to the in use abort list";
            if (!abortInfo.contains(expectedString1)) {
                throw new Exception("Did not find exected return string " + expectedString1 + " \nString returned " + abortInfo);
            }
            if (!abortInfo.contains(expectedString2)) {
                throw new Exception("Did not find exected return string " + expectedString2 + " \nString returned " + abortInfo);
            }
            if (abortInfo.contains(unexpectedString1)) {
                throw new Exception("Found unexpected return string " + unexpectedString1 + " \nString returned " + abortInfo);
            }

            // expected values after close and abort
            expectNumFreePoolConns = 1;
            expectedNumConnectionHandles = 0;
            expectedNumManagedConns = 1;
            checkStats(test, dsStatobjName, dsObjectName, expectNumFreePoolConns, expectedNumConnectionHandles, expectedNumManagedConns);

            int expectNumberOfAddedAborts = 1;
            int destroyCountAfterAbort = getMonitorData(dsStatobjName, "DestroyCount");
            if (destroyCountAfterAbort != destroyCountBeforeAbort + expectNumberOfAddedAborts)
                throw new Exception("Expected destroy count to increase by " + expectNumberOfAddedAborts + " after abort.  Before: " + destroyCountBeforeAbort + " After: "
                                    + destroyCountAfterAbort + "\n"
                                    + cmb.showPoolContents());
        } finally {
            if (connunshared != null && !connunshared.isClosed())
                connunshared.close();
            if (conn1shared != null && !conn1shared.isClosed())
                conn1shared.close();
        }
    }

    public void testCountsAfterInUseSharedInuseAbortArray(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        String test = "testCountsAfterInUseSharedInuseAbortArray";
        ObjectName dsStatobjName = getConnPoolStatsMBeanObjName("ds1");
        ObjectInstance dsObjectName = getMBeanObjectInstance("jdbc/ds1");
        //clear any connections
        mbeanServer.invoke(dsObjectName.getObjectName(), "purgePoolContents", new Object[] { "abort" }, null);

        // get one shared and one unshared connection.
        Connection conn1shared = ds1s.getConnection();
        Connection connunshared = ds1.getConnection();

        try {
            // expected values before close and abort
            int expectNumFreePoolConns = 0;
            int expectedNumConnectionHandles = 2;
            int expectedNumManagedConns = 2;
            checkStats(test, dsStatobjName, dsObjectName, expectNumFreePoolConns, expectedNumConnectionHandles, expectedNumManagedConns);

            Thread.sleep(2); // simulate in use time running a query.

            connunshared.close();

            // expected values after close before abort
            expectNumFreePoolConns = 1;
            expectedNumConnectionHandles = 1;
            expectedNumManagedConns = 2;
            checkStats(test, dsStatobjName, dsObjectName, expectNumFreePoolConns, expectedNumConnectionHandles, expectedNumManagedConns);

            int destroyCountBeforeAbort = getMonitorData(dsStatobjName, "DestroyCount");
            String abortInfo = (String) mbeanServer.invoke(dsObjectName.getObjectName(), "abortConnections", new Object[] { "inuse", "1" }, null);
            String expectedString1 = "1 in use connections added to the in use abort list";
            String expectedString2 = "1 free connections not added to the in use abort list";
            String unExpectedString1 = "in use connections not added to the in use abort list";
            if (!abortInfo.contains(expectedString1)) {
                throw new Exception("Did not find exected return string " + expectedString1 + " \nString returned " + abortInfo);
            }
            if (!abortInfo.contains(expectedString2)) {
                throw new Exception("Did not find exected return string " + expectedString2 + " \nString returned " + abortInfo);
            }
            if (abortInfo.contains(unExpectedString1)) {
                throw new Exception("Found unexpected return string " + unExpectedString1 + " \nString returned " + abortInfo);
            }
            // expected values after close and abort
            expectNumFreePoolConns = 1;
            expectedNumConnectionHandles = 0;
            expectedNumManagedConns = 1;
            checkStats(test, dsStatobjName, dsObjectName, expectNumFreePoolConns, expectedNumConnectionHandles, expectedNumManagedConns);

            int expectNumberOfAddedAborts = 1;
            int destroyCountAfterAbort = getMonitorData(dsStatobjName, "DestroyCount");
            if (destroyCountAfterAbort != destroyCountBeforeAbort + expectNumberOfAddedAborts)
                throw new Exception("Expected destroy count to increase by " + expectNumberOfAddedAborts + " after abort.  Before: " + destroyCountBeforeAbort + " After: "
                                    + destroyCountAfterAbort + "\n"
                                    + (String) mbeanServer.invoke(dsObjectName.getObjectName(), "showPoolContents", null, null));
        } finally {
            if (connunshared != null && !connunshared.isClosed())
                connunshared.close();
            if (conn1shared != null && !conn1shared.isClosed())
                conn1shared.close();
        }
    }

    public void testCountsAfterInUseSharedInuseAbortIntegerArray(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        String test = "testCountsAfterInUseSharedInuseAbortIntegerArray";
        ObjectName dsStatobjName = getConnPoolStatsMBeanObjName("ds1");
        ObjectInstance dsObjectName = getMBeanObjectInstance("jdbc/ds1");
        //clear any connections
        mbeanServer.invoke(dsObjectName.getObjectName(), "purgePoolContents", new Object[] { "abort" }, null);

        // get one shared and one unshared connection.
        Connection conn1shared = ds1s.getConnection();
        Connection connunshared = ds1.getConnection();

        try {
            // expected values before close and abort
            int expectNumFreePoolConns = 0;
            int expectedNumConnectionHandles = 2;
            int expectedNumManagedConns = 2;
            checkStats(test, dsStatobjName, dsObjectName, expectNumFreePoolConns, expectedNumConnectionHandles, expectedNumManagedConns);

            Thread.sleep(2); // simulate in use time running a query.

            connunshared.close();

            // expected values after close before abort
            expectNumFreePoolConns = 1;
            expectedNumConnectionHandles = 1;
            expectedNumManagedConns = 2;
            checkStats(test, dsStatobjName, dsObjectName, expectNumFreePoolConns, expectedNumConnectionHandles, expectedNumManagedConns);

            int destroyCountBeforeAbort = getMonitorData(dsStatobjName, "DestroyCount");
            Integer inuseIdleMax = new Integer(1);
            String abortInfo = (String) mbeanServer.invoke(dsObjectName.getObjectName(), "abortConnections", new Object[] { "inuse", inuseIdleMax }, null);
            String expectedString1 = "1 in use connections added to the in use abort list";
            String expectedString2 = "1 free connections not added to the in use abort list";
            String unExpectedString1 = "in use connections not added to the in use abort list";
            if (!abortInfo.contains(expectedString1)) {
                throw new Exception("Did not find exected return string " + expectedString1 + " \nString returned " + abortInfo);
            }
            if (!abortInfo.contains(expectedString2)) {
                throw new Exception("Did not find exected return string " + expectedString2 + " \nString returned " + abortInfo);
            }
            if (abortInfo.contains(unExpectedString1)) {
                throw new Exception("Found unexpected return string " + unExpectedString1 + " \nString returned " + abortInfo);
            }

            // expected values after close and abort
            expectNumFreePoolConns = 1;
            expectedNumConnectionHandles = 0;
            expectedNumManagedConns = 1;
            checkStats(test, dsStatobjName, dsObjectName, expectNumFreePoolConns, expectedNumConnectionHandles, expectedNumManagedConns);

            int expectNumberOfAddedAborts = 1;
            int destroyCountAfterAbort = getMonitorData(dsStatobjName, "DestroyCount");
            if (destroyCountAfterAbort != destroyCountBeforeAbort + expectNumberOfAddedAborts)
                throw new Exception("Expected destroy count to increase by " + expectNumberOfAddedAborts + " after abort.  Before: " + destroyCountBeforeAbort + " After: "
                                    + destroyCountAfterAbort + "\n"
                                    + (String) mbeanServer.invoke(dsObjectName.getObjectName(), "showPoolContents", null, null));
        } finally {
            if (connunshared != null && !connunshared.isClosed())
                connunshared.close();
            if (conn1shared != null && !conn1shared.isClosed())
                conn1shared.close();
        }
    }

    public void testCountsAfterInUseSharedInuseAbortLongArray(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        String test = "testCountsAfterInUseSharedInuseAbortLongArray";
        ObjectName dsStatobjName = getConnPoolStatsMBeanObjName("ds1");
        ObjectInstance dsObjectName = getMBeanObjectInstance("jdbc/ds1");
        //clear any connections
        mbeanServer.invoke(dsObjectName.getObjectName(), "purgePoolContents", new Object[] { "abort" }, null);

        // get one shared and one unshared connection.
        Connection conn1shared = ds1s.getConnection();
        Connection connunshared = ds1.getConnection();

        try {
            // expected values before close and abort
            int expectNumFreePoolConns = 0;
            int expectedNumConnectionHandles = 2;
            int expectedNumManagedConns = 2;
            checkStats(test, dsStatobjName, dsObjectName, expectNumFreePoolConns, expectedNumConnectionHandles, expectedNumManagedConns);

            Thread.sleep(2); // simulate in use time running a query.

            connunshared.close();

            // expected values after close before abort
            expectNumFreePoolConns = 1;
            expectedNumConnectionHandles = 1;
            expectedNumManagedConns = 2;
            checkStats(test, dsStatobjName, dsObjectName, expectNumFreePoolConns, expectedNumConnectionHandles, expectedNumManagedConns);

            int destroyCountBeforeAbort = getMonitorData(dsStatobjName, "DestroyCount");
            Long inuseIdleMax = new Long(1);
            String abortInfo = (String) mbeanServer.invoke(dsObjectName.getObjectName(), "abortConnections", new Object[] { "inuse", inuseIdleMax }, null);
            String expectedString1 = "1 in use connections added to the in use abort list";
            String expectedString2 = "1 free connections not added to the in use abort list";
            String unExpectedString1 = "in use connections not added to the in use abort list";
            if (!abortInfo.contains(expectedString1)) {
                throw new Exception("Did not find exected return string " + expectedString1 + " \nString returned " + abortInfo);
            }
            if (!abortInfo.contains(expectedString2)) {
                throw new Exception("Did not find exected return string " + expectedString2 + " \nString returned " + abortInfo);
            }
            if (abortInfo.contains(unExpectedString1)) {
                throw new Exception("Found unexpected return string " + unExpectedString1 + " \nString returned " + abortInfo);
            }

            // expected values after close and abort
            expectNumFreePoolConns = 1;
            expectedNumConnectionHandles = 0;
            expectedNumManagedConns = 1;
            checkStats(test, dsStatobjName, dsObjectName, expectNumFreePoolConns, expectedNumConnectionHandles, expectedNumManagedConns);

            int expectNumberOfAddedAborts = 1;
            int destroyCountAfterAbort = getMonitorData(dsStatobjName, "DestroyCount");
            if (destroyCountAfterAbort != destroyCountBeforeAbort + expectNumberOfAddedAborts)
                throw new Exception("Expected destroy count to increase by " + expectNumberOfAddedAborts + " after abort.  Before: " + destroyCountBeforeAbort + " After: "
                                    + destroyCountAfterAbort + "\n"
                                    + (String) mbeanServer.invoke(dsObjectName.getObjectName(), "showPoolContents", null, null));
        } finally {
            if (connunshared != null && !connunshared.isClosed())
                connunshared.close();
            if (conn1shared != null && !conn1shared.isClosed())
                conn1shared.close();
        }
    }

    public void testCountsAfterInUseSharedInuseAbortDoubleArray(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        String test = "testCountsAfterInUseSharedInuseAbortDoubleArray";
        ObjectName dsStatobjName = getConnPoolStatsMBeanObjName("ds1");
        ObjectInstance dsObjectName = getMBeanObjectInstance("jdbc/ds1");
        //clear any connections
        mbeanServer.invoke(dsObjectName.getObjectName(), "purgePoolContents", new Object[] { "abort" }, null);

        // get one shared and one unshared connection.
        Connection conn1shared = ds1s.getConnection();
        Connection connunshared = ds1.getConnection();

        try {
            // expected values before close and abort
            int expectNumFreePoolConns = 0;
            int expectedNumConnectionHandles = 2;
            int expectedNumManagedConns = 2;
            checkStats(test, dsStatobjName, dsObjectName, expectNumFreePoolConns, expectedNumConnectionHandles, expectedNumManagedConns);

            Thread.sleep(2); // simulate in use time running a query.

            connunshared.close();

            // expected values after close before abort
            expectNumFreePoolConns = 1;
            expectedNumConnectionHandles = 1;
            expectedNumManagedConns = 2;
            checkStats(test, dsStatobjName, dsObjectName, expectNumFreePoolConns, expectedNumConnectionHandles, expectedNumManagedConns);

            int destroyCountBeforeAbort = getMonitorData(dsStatobjName, "DestroyCount");
            Double inuseIdleMax = new Double(1);
            try {
                mbeanServer.invoke(dsObjectName.getObjectName(), "abortConnections", new Object[] { "inuse", inuseIdleMax }, null);
            } catch (MBeanException e) {
                // no purge of idle connection should occur.
            }

            // expected values after close and no abort
            expectNumFreePoolConns = 1;
            expectedNumConnectionHandles = 1;
            expectedNumManagedConns = 2;
            checkStats(test, dsStatobjName, dsObjectName, expectNumFreePoolConns, expectedNumConnectionHandles, expectedNumManagedConns);

            int expectNumberOfAddedAborts = 0;
            int destroyCountAfterAbort = getMonitorData(dsStatobjName, "DestroyCount");
            if (destroyCountAfterAbort != destroyCountBeforeAbort + expectNumberOfAddedAborts)
                throw new Exception("Expected destroy count to increase by " + expectNumberOfAddedAborts + " after abort.  Before: " + destroyCountBeforeAbort + " After: "
                                    + destroyCountAfterAbort + "\n"
                                    + (String) mbeanServer.invoke(dsObjectName.getObjectName(), "showPoolContents", null, null));
        } finally {
            if (connunshared != null && !connunshared.isClosed())
                connunshared.close();
            if (conn1shared != null && !conn1shared.isClosed())
                conn1shared.close();
        }
    }

    public void testCountsAfterInUseSharedAndUnsharedInuseAbortArray(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        String test = "testCountsAfterInUseSharedAndUnsharedInuseAbortArray";
        ObjectName dsStatobjName = getConnPoolStatsMBeanObjName("ds1");
        ObjectInstance dsObjectName = getMBeanObjectInstance("jdbc/ds1");
        //clear any connections
        mbeanServer.invoke(dsObjectName.getObjectName(), "purgePoolContents", new Object[] { "abort" }, null);

        // get one shared and one unshared connection.
        Connection conn1shared = ds1s.getConnection();
        Connection connunshared = ds1.getConnection();

        try {
            // expected values before abort
            int expectNumFreePoolConns = 0;
            int expectedNumConnectionHandles = 2;
            int expectedNumManagedConns = 2;
            checkStats(test, dsStatobjName, dsObjectName, expectNumFreePoolConns, expectedNumConnectionHandles, expectedNumManagedConns);

            int destroyCountBeforeAbort = getMonitorData(dsStatobjName, "DestroyCount");

            Thread.sleep(2); // simulate in use time running a query.

            String abortInfo = (String) mbeanServer.invoke(dsObjectName.getObjectName(), "abortConnections", new Object[] { "inuse", "1" }, null);
            String expectedString1 = "2 in use connections added to the in use abort list";
            String unexpectedString2 = "free connections not added to the in use abort list";
            String unExpectedString1 = "in use connections not added to the in use abort list";
            if (!abortInfo.contains(expectedString1)) {
                throw new Exception("Did not find exected return string " + expectedString1 + " \nString returned " + abortInfo);
            }
            if (abortInfo.contains(unexpectedString2)) {
                throw new Exception("Found unexpected return string " + unexpectedString2 + " \nString returned " + abortInfo);
            }
            if (abortInfo.contains(unExpectedString1)) {
                throw new Exception("Found unexpected return string " + unExpectedString1 + " \nString returned " + abortInfo);
            }

            // expected values after abort
            expectNumFreePoolConns = 0;
            expectedNumConnectionHandles = 0;
            expectedNumManagedConns = 0;
            checkStats(test, dsStatobjName, dsObjectName, expectNumFreePoolConns, expectedNumConnectionHandles, expectedNumManagedConns);

            int expectNumberOfAddedAborts = 2;
            int destroyCountAfterAbort = getMonitorData(dsStatobjName, "DestroyCount");
            if (destroyCountAfterAbort != destroyCountBeforeAbort + expectNumberOfAddedAborts)
                throw new Exception("Expected destroy count to increase by " + expectNumberOfAddedAborts + " after abort.  Before: " + destroyCountBeforeAbort + " After: "
                                    + destroyCountAfterAbort + "\n"
                                    + (String) mbeanServer.invoke(dsObjectName.getObjectName(), "showPoolContents", null, null));

        } finally {
            if (connunshared != null && !connunshared.isClosed())
                connunshared.close();
            if (conn1shared != null && !conn1shared.isClosed())
                conn1shared.close();
        }
    }

    public void testCountsAfterInUse2SharedAnd2UnsharedInuseAbortArray(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        String test = "testCountsAfterInUse2SharedAnd2UnsharedInuseAbortArray";
        ObjectName dsStatobjName = getConnPoolStatsMBeanObjName("ds1");
        ObjectInstance dsObjectName = getMBeanObjectInstance("jdbc/ds1");
        //clear any connections
        mbeanServer.invoke(dsObjectName.getObjectName(), "purgePoolContents", new Object[] { "abort" }, null);

        // get one shared and one unshared connection.
        Connection conn1shared = ds1s.getConnection();
        Connection connunshared = ds1.getConnection();
        Connection conn2shared = ds1s.getConnection();
        Connection conn2unshared = ds1.getConnection();

        try {
            // expected values before abort
            int expectNumFreePoolConns = 0;
            int expectedNumConnectionHandles = 4;
            int expectedNumManagedConns = 4;
            checkStats(test, dsStatobjName, dsObjectName, expectNumFreePoolConns, expectedNumConnectionHandles, expectedNumManagedConns);

            int destroyCountBeforeAbort = getMonitorData(dsStatobjName, "DestroyCount");

            Thread.sleep(2); // simulate in use time running a query.

            String abortInfo = (String) mbeanServer.invoke(dsObjectName.getObjectName(), "abortConnections", new Object[] { "inuse", "1" }, null);
            String expectedString1 = "4 in use connections added to the in use abort list";
            String unexpectedString2 = "free connections not added to the in use abort list";
            String unExpectedString1 = "in use connections not added to the in use abort list";
            if (!abortInfo.contains(expectedString1)) {
                throw new Exception("Did not find exected return string " + expectedString1 + " \nString returned " + abortInfo);
            }
            if (abortInfo.contains(unexpectedString2)) {
                throw new Exception("Found unexpected return string " + unexpectedString2 + " \nString returned " + abortInfo);
            }
            if (abortInfo.contains(unExpectedString1)) {
                throw new Exception("Found unexpected return string " + unExpectedString1 + " \nString returned " + abortInfo);
            }

            // expected values after abort
            expectNumFreePoolConns = 0;
            expectedNumConnectionHandles = 0;
            expectedNumManagedConns = 0;
            checkStats(test, dsStatobjName, dsObjectName, expectNumFreePoolConns, expectedNumConnectionHandles, expectedNumManagedConns);

            int expectNumberOfAddedAborts = 4;
            int destroyCountAfterAbort = getMonitorData(dsStatobjName, "DestroyCount");
            if (destroyCountAfterAbort != destroyCountBeforeAbort + expectNumberOfAddedAborts)
                throw new Exception("Expected destroy count to increase by " + expectNumberOfAddedAborts + " after abort.  Before: " + destroyCountBeforeAbort + " After: "
                                    + destroyCountAfterAbort + "\n"
                                    + (String) mbeanServer.invoke(dsObjectName.getObjectName(), "showPoolContents", null, null));

        } finally {
            if (connunshared != null && !connunshared.isClosed())
                connunshared.close();
            if (conn1shared != null && !conn1shared.isClosed())
                conn1shared.close();
            if (conn2unshared != null && !conn2unshared.isClosed())
                conn2unshared.close();
            if (conn2shared != null && !conn2shared.isClosed())
                conn2shared.close();
        }
    }

    public void testCountsAfterInUseMultipleSharedAndUnsharedInuseAbortSSArray(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        String test = "testCountsAfterInUseMultipleSharedAndUnsharedInuseAbortSSArray";
        ObjectName dsStatobjName = getConnPoolStatsMBeanObjName("ds1");
        ObjectInstance dsObjectName = getMBeanObjectInstance("jdbc/ds1");
        //clear any connections
        mbeanServer.invoke(dsObjectName.getObjectName(), "purgePoolContents", new Object[] { "abort" }, null);

        // get one shared and one unshared connection.
        Connection[] connsharedFirstSet = new Connection[10];
        Connection[] connunsharedFirstSet = new Connection[10];

        int expectNumFreePoolConns = 0;
        int expectedNumConnectionHandles = 0;
        int expectedNumManagedConns = 0;

        long creatingConnectionStartFirstSet = System.currentTimeMillis();

        for (int i = 0; i < 10; ++i) {

            connsharedFirstSet[i] = ds1s.getConnection();
            connunsharedFirstSet[i] = ds1.getConnection();
            expectNumFreePoolConns = 0;
            expectedNumConnectionHandles = expectedNumConnectionHandles + 2;
            expectedNumManagedConns = expectedNumManagedConns + 2;
            checkStats(test, dsStatobjName, dsObjectName, expectNumFreePoolConns, expectedNumConnectionHandles, expectedNumManagedConns);
        }

        Thread.sleep(60); // simulate in use time running a query.

        long creatingConnectionStartSecondSet = System.currentTimeMillis();

        long ApproxConnectionInuseDurationTimeFirstSet = creatingConnectionStartSecondSet - creatingConnectionStartFirstSet;
        Connection[] connsharedSecondSet = new Connection[10];
        Connection[] connunsharedSecondSet = new Connection[10];
        for (int i = 0; i < 10; ++i) {
            connsharedSecondSet[i] = ds1s.getConnection();
            connunsharedSecondSet[i] = ds1.getConnection();
            expectNumFreePoolConns = 0;
            expectedNumConnectionHandles = expectedNumConnectionHandles + 2;
            expectedNumManagedConns = expectedNumManagedConns + 2;
            checkStats(test, dsStatobjName, dsObjectName, expectNumFreePoolConns, expectedNumConnectionHandles, expectedNumManagedConns);
        }

        try {
            // expected values before close and abort
            expectNumFreePoolConns = 0;
            expectedNumConnectionHandles = 40;
            expectedNumManagedConns = 40;
            checkStats(test, dsStatobjName, dsObjectName, expectNumFreePoolConns, expectedNumConnectionHandles, expectedNumManagedConns);

            for (Connection cs : connsharedSecondSet) {
                if (cs != null)
                    cs.close();;
            }
            for (Connection cs : connunsharedSecondSet) {
                if (cs != null)
                    cs.close();;
            }
            // expected values after close and before abort
            expectNumFreePoolConns = 10;
            expectedNumConnectionHandles = 20;
            expectedNumManagedConns = 40;
            checkStats(test, dsStatobjName, dsObjectName, expectNumFreePoolConns, expectedNumConnectionHandles, expectedNumManagedConns);

            Thread.sleep(20); // simulate in use time running a query.

            int destroyCountBeforeAbort = getMonitorData(dsStatobjName, "DestroyCount");

            long testvalue = System.currentTimeMillis() - creatingConnectionStartSecondSet + 40;
            String abortInfo = (String) mbeanServer.invoke(dsObjectName.getObjectName(), "abortConnections", new Object[] { "inuse", testvalue }, null);
            String expectedString1 = "20 in use connections added to the in use abort list";
            String expectedString2 = "10 free connections not added to the in use abort list";
            String expectedString3 = "10 in use connections not added to the in use abort list";
            if (!abortInfo.contains(expectedString1)) {
                throw new Exception("Did not find exected return string " + expectedString1 + " \nString returned " + abortInfo);
            }
            if (!abortInfo.contains(expectedString2)) {
                throw new Exception("Did not find exected return string " + expectedString2 + " \nString returned " + abortInfo);
            }
            if (!abortInfo.contains(expectedString3)) {
                throw new Exception("Did not find exected return string " + expectedString3 + " \nString returned " + abortInfo);
            }
            // expected values after abort
            expectNumFreePoolConns = 10;
            expectedNumConnectionHandles = 0;
            expectedNumManagedConns = 20;
            checkStats(test, dsStatobjName, dsObjectName, expectNumFreePoolConns, expectedNumConnectionHandles, expectedNumManagedConns);

            int expectNumberOfAddedAborts = 20;
            int destroyCountAfterAbort = getMonitorData(dsStatobjName, "DestroyCount");
            if (destroyCountAfterAbort != destroyCountBeforeAbort + expectNumberOfAddedAborts)
                throw new Exception("Expected destroy count to increase by " + expectNumberOfAddedAborts + " after abort.  Before: " + destroyCountBeforeAbort + " After: "
                                    + destroyCountAfterAbort + "\n"
                                    + (String) mbeanServer.invoke(dsObjectName.getObjectName(), "showPoolContents", null, null));

        } finally {
            for (Connection c : connunsharedFirstSet) {
                if (c != null && !c.isClosed())
                    c.close();
            }
            for (Connection c : connsharedFirstSet) {
                if (c != null && !c.isClosed())
                    c.close();
            }
        }
    }

    public void testCountsAfterInUseMultipleSharedAndUnsharedInuseAbortSSCloseFSSharedArray(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        String test = "testCountsAfterInUseMultipleSharedAndUnsharedInuseAbortSSCloseFSSharedArray";
        ObjectName dsStatobjName = getConnPoolStatsMBeanObjName("ds1");
        ObjectInstance dsObjectName = getMBeanObjectInstance("jdbc/ds1");
        //clear any connections
        mbeanServer.invoke(dsObjectName.getObjectName(), "purgePoolContents", new Object[] { "abort" }, null);

        // get one shared and one unshared connection.
        Connection[] connsharedFirstSet = new Connection[10];
        Connection[] connunsharedFirstSet = new Connection[10];

        int expectNumFreePoolConns = 0;
        int expectedNumConnectionHandles = 0;
        int expectedNumManagedConns = 0;

        long creatingConnectionStartFirstSet = System.currentTimeMillis();

        for (int i = 0; i < 10; ++i) {

            connsharedFirstSet[i] = ds1s.getConnection();
            connunsharedFirstSet[i] = ds1.getConnection();
            expectNumFreePoolConns = 0;
            expectedNumConnectionHandles = expectedNumConnectionHandles + 2;
            expectedNumManagedConns = expectedNumManagedConns + 2;
            checkStats(test, dsStatobjName, dsObjectName, expectNumFreePoolConns, expectedNumConnectionHandles, expectedNumManagedConns);
        }

        Thread.sleep(60); // simulate in use time running a query.

        long creatingConnectionStartSecondSet = System.currentTimeMillis();

        long ApproxConnectionInuseDurationTimeFirstSet = creatingConnectionStartSecondSet - creatingConnectionStartFirstSet;
        Connection[] connsharedSecondSet = new Connection[10];
        Connection[] connunsharedSecondSet = new Connection[10];
        for (int i = 0; i < 10; ++i) {
            connsharedSecondSet[i] = ds1s.getConnection();
            connunsharedSecondSet[i] = ds1.getConnection();
            expectNumFreePoolConns = 0;
            expectedNumConnectionHandles = expectedNumConnectionHandles + 2;
            expectedNumManagedConns = expectedNumManagedConns + 2;
            checkStats(test, dsStatobjName, dsObjectName, expectNumFreePoolConns, expectedNumConnectionHandles, expectedNumManagedConns);
        }

        try {
            // expected values before close and abort
            expectNumFreePoolConns = 0;
            expectedNumConnectionHandles = 40;
            expectedNumManagedConns = 40;
            checkStats(test, dsStatobjName, dsObjectName, expectNumFreePoolConns, expectedNumConnectionHandles, expectedNumManagedConns);

            for (Connection cs : connsharedSecondSet) {
                if (cs != null)
                    cs.close();
            }
            for (Connection cs : connunsharedSecondSet) {
                if (cs != null)
                    cs.close();
            }
            for (Connection cs : connsharedFirstSet) {
                if (cs != null) {
                    cs.close();
                }
            }
            for (int i = 0; i < 10; ++i) {
                connsharedFirstSet[i] = ds1s.getConnection();
            }
            // expected values after close and before abort
            expectNumFreePoolConns = 10;
            expectedNumConnectionHandles = 20;
            expectedNumManagedConns = 40;
            checkStats(test, dsStatobjName, dsObjectName, expectNumFreePoolConns, expectedNumConnectionHandles, expectedNumManagedConns);

            Thread.sleep(20); // simulate in use time running a query.

            int destroyCountBeforeAbort = getMonitorData(dsStatobjName, "DestroyCount");

            long testvalue = System.currentTimeMillis() - creatingConnectionStartSecondSet + 40;
            String abortInfo = (String) mbeanServer.invoke(dsObjectName.getObjectName(), "abortConnections", new Object[] { "inuse", testvalue }, null);
            String expectedString1 = "20 in use connections added to the in use abort list";
            String expectedString2 = "10 free connections not added to the in use abort list";
            String expectedString3 = "10 in use connections not added to the in use abort list";
            if (!abortInfo.contains(expectedString1)) {
                throw new Exception("Did not find exected return string " + expectedString1 + " \nString returned " + abortInfo);
            }
            if (!abortInfo.contains(expectedString2)) {
                throw new Exception("Did not find exected return string " + expectedString2 + " \nString returned " + abortInfo);
            }
            if (!abortInfo.contains(expectedString3)) {
                throw new Exception("Did not find exected return string " + expectedString3 + " \nString returned " + abortInfo);
            }
            // expected values after abort
            expectNumFreePoolConns = 10;
            expectedNumConnectionHandles = 0;
            expectedNumManagedConns = 20;
            checkStats(test, dsStatobjName, dsObjectName, expectNumFreePoolConns, expectedNumConnectionHandles, expectedNumManagedConns);

            int expectNumberOfAddedAborts = 20;
            int destroyCountAfterAbort = getMonitorData(dsStatobjName, "DestroyCount");
            if (destroyCountAfterAbort != destroyCountBeforeAbort + expectNumberOfAddedAborts)
                throw new Exception("Expected destroy count to increase by " + expectNumberOfAddedAborts + " after abort.  Before: " + destroyCountBeforeAbort + " After: "
                                    + destroyCountAfterAbort + "\n"
                                    + (String) mbeanServer.invoke(dsObjectName.getObjectName(), "showPoolContents", null, null));

        } finally {
            for (Connection c : connunsharedFirstSet) {
                if (c != null && !c.isClosed())
                    c.close();
            }
            for (Connection c : connsharedFirstSet) {
                if (c != null && !c.isClosed())
                    c.close();
            }
        }
    }

    public void testCountsAfterInUseMultipleSharedAndUnsharedInuseAbortFSArray(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        String test = "testCountsAfterInUseMultipleSharedAndUnsharedInuseAbortFSArray";
        ObjectName dsStatobjName = getConnPoolStatsMBeanObjName("ds1");
        ObjectInstance dsObjectName = getMBeanObjectInstance("jdbc/ds1");
        //clear any connections
        mbeanServer.invoke(dsObjectName.getObjectName(), "purgePoolContents", new Object[] { "abort" }, null);

        // get one shared and one unshared connection.
        Connection[] connsharedFirstSet = new Connection[10];
        Connection[] connunsharedFirstSet = new Connection[10];

        int expectNumFreePoolConns = 0;
        int expectedNumConnectionHandles = 0;
        int expectedNumManagedConns = 0;

        long creatingConnectionStartFirstSet = System.currentTimeMillis();

        for (int i = 0; i < 10; ++i) {

            connsharedFirstSet[i] = ds1s.getConnection();
            connunsharedFirstSet[i] = ds1.getConnection();
            expectNumFreePoolConns = 0;
            expectedNumConnectionHandles = expectedNumConnectionHandles + 2;
            expectedNumManagedConns = expectedNumManagedConns + 2;
            checkStats(test, dsStatobjName, dsObjectName, expectNumFreePoolConns, expectedNumConnectionHandles, expectedNumManagedConns);
        }

        Thread.sleep(60); // simulate in use time running a query.

        long creatingConnectionStartSecondSet = System.currentTimeMillis();

        long ApproxConnectionInuseDurationTimeFirstSet = creatingConnectionStartSecondSet - creatingConnectionStartFirstSet;
        Connection[] connsharedSecondSet = new Connection[10];
        Connection[] connunsharedSecondSet = new Connection[10];
        for (int i = 0; i < 10; ++i) {
            connsharedSecondSet[i] = ds1s.getConnection();
            connunsharedSecondSet[i] = ds1.getConnection();
            expectNumFreePoolConns = 0;
            expectedNumConnectionHandles = expectedNumConnectionHandles + 2;
            expectedNumManagedConns = expectedNumManagedConns + 2;
            checkStats(test, dsStatobjName, dsObjectName, expectNumFreePoolConns, expectedNumConnectionHandles, expectedNumManagedConns);
        }

        try {
            // expected values before close and abort
            expectNumFreePoolConns = 0;
            expectedNumConnectionHandles = 40;
            expectedNumManagedConns = 40;
            checkStats(test, dsStatobjName, dsObjectName, expectNumFreePoolConns, expectedNumConnectionHandles, expectedNumManagedConns);

            for (Connection cs : connsharedFirstSet) {
                if (cs != null)
                    cs.close();;
            }
            for (Connection cs : connunsharedFirstSet) {
                if (cs != null)
                    cs.close();;
            }
            // expected values after close and before abort
            expectNumFreePoolConns = 10;
            expectedNumConnectionHandles = 20;
            expectedNumManagedConns = 40;
            checkStats(test, dsStatobjName, dsObjectName, expectNumFreePoolConns, expectedNumConnectionHandles, expectedNumManagedConns);

            Thread.sleep(20); // simulate in use time running a query.

            int destroyCountBeforeAbort = getMonitorData(dsStatobjName, "DestroyCount");

            long testvalue = System.currentTimeMillis() - creatingConnectionStartSecondSet + 40;
            String abortInfo = (String) mbeanServer.invoke(dsObjectName.getObjectName(), "abortConnections", new Object[] { "inuse", testvalue }, null);
            String expectedString1 = "10 in use connections added to the in use abort list";
            String expectedString2 = "10 free connections not added to the in use abort list";
            String expectedString3 = "20 in use connections not added to the in use abort list";
            if (!abortInfo.contains(expectedString1)) {
                throw new Exception("Did not find exected return string " + expectedString1 + " \nString returned " + abortInfo);
            }
            if (!abortInfo.contains(expectedString2)) {
                throw new Exception("Did not find exected return string " + expectedString2 + " \nString returned " + abortInfo);
            }
            if (!abortInfo.contains(expectedString3)) {
                throw new Exception("Did not find exected return string " + expectedString3 + " \nString returned " + abortInfo);
            }
            // expected values after abort
            expectNumFreePoolConns = 10;
            expectedNumConnectionHandles = 20;
            expectedNumManagedConns = 30;
            checkStats(test, dsStatobjName, dsObjectName, expectNumFreePoolConns, expectedNumConnectionHandles, expectedNumManagedConns);

            int expectNumberOfAddedAborts = 10;
            int destroyCountAfterAbort = getMonitorData(dsStatobjName, "DestroyCount");
            if (destroyCountAfterAbort != destroyCountBeforeAbort + expectNumberOfAddedAborts)
                throw new Exception("Expected destroy count to increase by " + expectNumberOfAddedAborts + " after abort.  Before: " + destroyCountBeforeAbort + " After: "
                                    + destroyCountAfterAbort + "\n"
                                    + (String) mbeanServer.invoke(dsObjectName.getObjectName(), "showPoolContents", null, null));

        } finally {
            for (Connection c : connunsharedFirstSet) {
                if (c != null && !c.isClosed())
                    c.close();
            }
            for (Connection c : connsharedFirstSet) {
                if (c != null && !c.isClosed())
                    c.close();
            }
        }
    }

    public void testGetCreateCount(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        //clear any connections
        mbeanServer.invoke(getMBeanObjectInstance("jdbc/ds1").getObjectName(), "purgePoolContents", new Object[] { "abort" }, null);

        ObjectName objName = getConnPoolStatsMBeanObjName("ds1");
        int initialCreateCount = getMonitorData(objName, "CreateCount");
        Connection conn = ds1.getConnection();
        Connection conn2 = ds1.getConnection();
        Connection conn3 = ds1.getConnection();
        try {
            conn.close();
            int createCountAfter3 = getMonitorData(objName, "CreateCount"); //Free pool was empty and we got 3 connections: increase by 3
            conn2.close();
            conn3.close();
            conn2 = ds1.getConnection();
            int finalCreateCount = getMonitorData(objName, "CreateCount"); //Free pool was not empty and we got a new connection: createCount should not increase
            int difference1 = createCountAfter3 - initialCreateCount;
            if (difference1 != 3) {
                throw new Exception("Expected an increase of 3 after getting three connections with empty pool, but was: " + difference1);
            }
            int difference2 = finalCreateCount - createCountAfter3;
            if (difference2 != 0) {
                throw new Exception("Expected an increase of 0 since there were connections in pool, but was: " + difference2);
            }

        } finally {
            if (conn != null && !conn.isClosed())
                conn.close();
            if (conn2 != null && !conn2.isClosed())
                conn2.close();
            if (conn3 != null && !conn3.isClosed())
                conn3.close();
        }

    }

    public void testGetDestroyCount(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        //clear any connections
        mbeanServer.invoke(getMBeanObjectInstance("jdbc/ds1").getObjectName(), "purgePoolContents", new Object[] { "abort" }, null);

        ObjectName objName = getConnPoolStatsMBeanObjName("ds1");
        int initialDestroyCount = getMonitorData(objName, "DestroyCount");
        Connection conn = ds1.getConnection();
        Connection conn2 = ds1.getConnection();
        try {
            mbeanServer.invoke(getMBeanObjectInstance("jdbc/ds1").getObjectName(), "purgePoolContents", new Object[] { "abort" }, null);

            int destroyCountAfterPurge = getMonitorData(objName, "DestroyCount"); // Two Connections when purge issued, increase destroy count by 2
            conn = ds1.getConnection();
            conn.close();
            int finalDestroyCount = getMonitorData(objName, "DestroyCount"); // Destroy count should not change after closing connection
            int difference1 = destroyCountAfterPurge - initialDestroyCount;
            if (difference1 != 2) {
                throw new Exception("Expected an increase of 2 after purging two connections, but was: " + difference1);
            }
            int difference2 = finalDestroyCount - destroyCountAfterPurge;
            if (difference2 != 0) {
                throw new Exception("Destroy count should not have increased after closing connection - expecting 0 but was: " + difference2);
            }

        } finally {
            if (conn != null && !conn.isClosed())
                conn.close();
            if (conn2 != null && !conn2.isClosed())
                conn2.close();
        }
    }

    public void testGetConnectionHandleCount(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        //clear any connections
        mbeanServer.invoke(getMBeanObjectInstance("jdbc/ds1").getObjectName(), "purgePoolContents", new Object[] { "abort" }, null);

        ObjectName objName = getConnPoolStatsMBeanObjName("ds1");
        int connHandleCount1 = getMonitorData(objName, "ConnectionHandleCount"); //we purged pool, connection handle count should be 0
        Connection conn = ds1.getConnection();
        Connection conn2 = ds1.getConnection();
        Connection conn3 = ds1.getConnection();
        try {
            conn.close();
            int connHandleCount2 = getMonitorData(objName, "ConnectionHandleCount"); //Pool was empty, we got 3 connections, closed 1, connection handle count should be 2
            conn2.close();
            conn3.close();
            conn2 = ds1.getConnection();
            int connHandleCount3 = getMonitorData(objName, "ConnectionHandleCount"); //We had 2 connection handles, closed 2, got 1, connection handle count should be 1
            if (connHandleCount1 != 0) {
                throw new Exception("Expected connection handle count to be 0 after the pool was purged, but was: " + connHandleCount1);
            }
            if (connHandleCount2 != 2) {
                throw new Exception("Expected connection handle count to be 2, but was: " + connHandleCount2);
            }
            if (connHandleCount3 != 1) {
                throw new Exception("Expected connection handle count to be 1, but was: " + connHandleCount3);
            }

        } finally {
            if (conn != null && !conn.isClosed())
                conn.close();
            if (conn2 != null && !conn2.isClosed())
                conn2.close();
            if (conn3 != null && !conn3.isClosed())
                conn3.close();
        }
    }

    public void testGetManagedConnectionCount(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        //clear any connections
        mbeanServer.invoke(getMBeanObjectInstance("jdbc/ds1").getObjectName(), "purgePoolContents", new Object[] { "abort" }, null);

        ObjectName objName = getConnPoolStatsMBeanObjName("ds1");
        int managedConnectionCountCount1 = getMonitorData(objName, "ManagedConnectionCount"); //we purged pool, managed connection count should be 0
        Connection conn = ds1.getConnection();
        Connection conn2 = ds1.getConnection();
        Connection conn3 = ds1.getConnection();
        try {
            conn.close();
            int managedConnectionCountCount2 = getMonitorData(objName, "ManagedConnectionCount"); //Pool was empty, we got 3 connections, closed 1, managed connection count should be 3
            conn2.close();
            conn3.close();
            conn2 = ds1.getConnection();
            int managedConnectionCountCount3 = getMonitorData(objName, "ManagedConnectionCount"); //We had three managed connections, closed 2, got 1, managed connection count should be 3
            if (managedConnectionCountCount1 != 0) {
                throw new Exception("Expected managed connection count to be 0 after the pool was purged, but was: " + managedConnectionCountCount1);
            }
            if (managedConnectionCountCount2 != 3) {
                throw new Exception("Expected managed connection count to be 3, but was: " + managedConnectionCountCount2);
            }
            if (managedConnectionCountCount3 != 3) {
                throw new Exception("Expected managed connection count to be 3, but was: " + managedConnectionCountCount3);
            }

        } finally {
            if (conn != null && !conn.isClosed())
                conn.close();
            if (conn2 != null && !conn2.isClosed())
                conn2.close();
            if (conn3 != null && !conn3.isClosed())
                conn3.close();
        }
    }

    public void testGetWaitTime(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        Connection conn = waitTimeDS.getConnection();
        ObjectName objName = getConnPoolStatsMBeanObjName("waitTimeDS");
        double initialWaitTime = (Double) mbeanServer.getAttribute(objName, "WaitTime");
        Connection conn2 = waitTimeDS.getConnection();
        Connection conn3 = null;
        try {
            conn3 = waitTimeDS.getConnection();
        } catch (SQLException e) {
            //expected
        }
        try {

            double waitTime2 = (Double) mbeanServer.getAttribute(objName, "WaitTime") / 1000; // convert from milliseconds to seconds

            if (Double.compare(initialWaitTime, 0) != 0) {
                throw new Exception("Expected wait time to be 0 before any waiters, but was: " + initialWaitTime);
            }
            if (Double.compare(waitTime2, 0) <= 0) { //Wait time should be greater than 0
                throw new Exception("Expected wait time to be greater than 0, but was: " + waitTime2);
            }

        } finally {
            if (conn != null && !conn.isClosed())
                conn.close();
            if (conn2 != null && !conn2.isClosed())
                conn2.close();
            if (conn3 != null && !conn3.isClosed())
                conn3.close();
        }
    }

    public void testGetFreeConnectionCount(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        //clear any connections
        mbeanServer.invoke(getMBeanObjectInstance("jdbc/ds1").getObjectName(), "purgePoolContents", new Object[] { "abort" }, null);

        ObjectName objName = getConnPoolStatsMBeanObjName("ds1");
        int freeCount1 = getMonitorData(objName, "FreeConnectionCount"); //we purged pool, free count should be 0
        Connection conn = ds1.getConnection();
        Connection conn2 = ds1.getConnection();
        Connection conn3 = ds1.getConnection();
        try {
            conn.close();
            int freeCount2 = getMonitorData(objName, "FreeConnectionCount"); //Pool was empty, we got 3 connections, closed 1, free count should be 1
            conn2.close();
            conn3.close();
            conn2 = ds1.getConnection(); //We had 1 free connection, closed two, and then got 1, should be 2 free connections
            int freeCount3 = getMonitorData(objName, "FreeConnectionCount");
            if (freeCount1 != 0) {
                throw new Exception("Expected no free connections after the pool was purged, but found: " + freeCount1);
            }
            if (freeCount2 != 1) {
                throw new Exception("Expected one free connection, but found: " + freeCount2);
            }
            if (freeCount3 != 2) {
                throw new Exception("Expected two free connections, but found: " + freeCount3);
            }

        } finally {
            if (conn != null && !conn.isClosed())
                conn.close();
            if (conn2 != null && !conn2.isClosed())
                conn2.close();
            if (conn3 != null && !conn3.isClosed())
                conn3.close();
        }
    }

    public void testGetInUseTime(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        Connection conn = ds1.getConnection();
        try {
            ObjectName objName = getConnPoolStatsMBeanObjName("ds1");
            conn.close();
            double inUseTime = (Double) mbeanServer.getAttribute(objName, "InUseTime");
            if (Double.compare(inUseTime, 0) <= 0) {
                throw new Exception("Expected in use time to be greater than 0, but was: " + inUseTime);
            }

        } finally {
            if (conn != null && !conn.isClosed())
                conn.close();
        }
    }

    public void testGetMaxConnectionsLimit(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        Connection conn = ds1_maxconlimit.getConnection();
        try {
            ObjectName objName = getConnPoolStatsMBeanObjName("ds1_maxconlimit");
            conn.close();
            int maxConnectionsLimit = getMonitorData(objName, "MaxConnectionsLimit");
            if (firstTime) {
                firstTime = false;
                if (maxConnectionsLimit != 50) {
                    throw new Exception("Expected maximum connections to be 50, but was: " + maxConnectionsLimit);
                }
            } else if (maxConnectionsLimit != 20) {
                throw new Exception("Expected maximum connections to be 20, but was: " + maxConnectionsLimit);
            }

        } finally {
            if (conn != null && !conn.isClosed())
                conn.close();
        }
    }

    public void testMbeanAttributeList(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        List<String> attributes = new ArrayList<String>(Arrays.asList("CreateCount",
                                                                      "DestroyCount",
                                                                      "ConnectionHandleCount",
                                                                      "ManagedConnectionCount",
                                                                      "WaitTime",
                                                                      "FreeConnectionCount",
                                                                      "InUseTime",
                                                                      "WaitTimeDetails",
                                                                      "InUseTimeDetails",
                                                                      "MaxConnectionsLimit"));
        int attributeListSize = attributes.size();
        ObjectName objName = getConnPoolStatsMBeanObjName("ds1");
        MBeanInfo mi = mbeanServer.getMBeanInfo(objName);
        MBeanAttributeInfo[] mba = mi.getAttributes();
        int attListSize = mba.length;
        if (attListSize != attributeListSize)
            throw new Exception("ConnectionPoolStatsMBean:Changed size for - " + objName + " was " + attributeListSize + " new size " + attListSize);
        for (int i = 0; i < attListSize; ++i) {
            String attribute = mba[i].getName();
            if (!attributes.remove(attribute)) {
                throw new Exception("ConnectionPoolStatsMBean:Additional attribute " + attribute + " was found, its likely this test needs to be update for the new attribute");
            }
        }
        if (attributes.size() != 0) {
            throw new Exception("ConnectionPoolStatsMBean:Missing an attribute('s) " + attributes.toString());
        }
    }

    public void testExceptionInCreate(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        Connection conn = exceptionInCreateDS.getConnection(); //ensure MBean exists
        conn.close();
        mbeanServer.invoke(getMBeanObjectInstance("jdbc/exceptionInCreateDS").getObjectName(), "purgePoolContents", new Object[] { "abort" }, null); //clear pool
        ObjectName objName = getConnPoolStatsMBeanObjName("exceptionInCreateDS");
        int initialCreateCount = getMonitorData(objName, "CreateCount");
        double initialWaitTime = (Double) mbeanServer.getAttribute(objName, "WaitTime");
        conn = exceptionInCreateDS.getConnection();
        Connection conn2 = exceptionInCreateDS.getConnection();
        Connection conn3 = null;
        try {
            try {
                conn3 = exceptionInCreateDS.getConnection();
            } catch (SQLException e) {
                //expected
            }
            int finalCreateCount = getMonitorData(objName, "CreateCount");
            int numManagedConns = getMonitorData(objName, "ManagedConnectionCount");
            int numFreeConns = getMonitorData(objName, "FreeConnectionCount");
            double waitTime2 = (Double) mbeanServer.getAttribute(objName, "WaitTime") / 1000; // convert from milliseconds to seconds
            if (finalCreateCount != initialCreateCount + 2) { //Should increase by 2 since we successfully create 2 conns
                throw new Exception("Expected createCount to increase by two. Initial: " + initialCreateCount + " Final: " + finalCreateCount);
            }
            if (numManagedConns != 2) { //Should be 2
                throw new Exception("Expected 2 managed connections, but found: " + numManagedConns);
            }
            if (numFreeConns != 0) { //Should be 0
                throw new Exception("Expected 0 free connections, but found: " + numFreeConns);
            }
            if (Double.compare(initialWaitTime, 0) != 0) {
                throw new Exception("Expected wait time to be 0 before any waiters, but was: " + initialWaitTime);
            }
            if (Double.compare(waitTime2, 0) <= 0) { // The wait timeout causes an exception in queueRequest - the probe should still trigger so wait timeout should be greater than 0
                throw new Exception("Expected wait time to be greater than 0, but was: " + waitTime2);
            }

        } finally {
            if (conn != null && !conn.isClosed())
                conn.close();
            if (conn2 != null && !conn2.isClosed())
                conn2.close();
            if (conn3 != null && !conn3.isClosed())
                conn3.close();
        }
    }

    public void testSharedConnections(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        Connection conn = sharedDS.getConnection(); //ensure we can access the mbean
        conn.close();
        mbeanServer.invoke(getMBeanObjectInstance("jdbc/sharedDS").getObjectName(), "purgePoolContents", new Object[] { "abort" }, null); //clear any connections
        tran.begin();
        ObjectName objName = getConnPoolStatsMBeanObjName("sharedDS");
        int initialCreateCount = getMonitorData(objName, "CreateCount");
        int initialDestoryCount = getMonitorData(objName, "DestroyCount");
        conn = sharedDS.getConnection();
        Connection conn2 = sharedDS.getConnection();
        Connection conn3 = sharedDS.getConnection();
        try {
            int finalCreateCount = getMonitorData(objName, "CreateCount"); //should only increase by 1 since we are using shared conns
            int numManagedConns = getMonitorData(objName, "ManagedConnectionCount"); //should be 1 since we are using shared conns
            int numConnectionHandles1 = getMonitorData(objName, "ConnectionHandleCount"); // should be 3
            conn.close();
            int numConnectionHandles2 = getMonitorData(objName, "ConnectionHandleCount"); // should be 2
            conn2.close();
            conn3.close();
            int numFreeConns1 = getMonitorData(objName, "FreeConnectionCount"); // should be 0
            mbeanServer.invoke(getMBeanObjectInstance("jdbc/sharedDS").getObjectName(), "purgePoolContents", new Object[] { "abort" }, null);
            conn2 = sharedDS.getConnection();
            tran.commit();
            int numFreeConns2 = getMonitorData(objName, "FreeConnectionCount"); //should be 1
            int finalDestroyCount = getMonitorData(objName, "DestroyCount"); //should increase by 1
            if (finalCreateCount != initialCreateCount + 1) { // Since they are shared conns we should only have created one physical conn to the database
                throw new Exception("Expected CreateCount to increase by 1.  Initial " + initialCreateCount + " final " + finalCreateCount);
            }
            if (numConnectionHandles1 != 3)
                throw new Exception("Expected three connection handles, but was: " + numConnectionHandles1);
            if (numConnectionHandles2 != 2)
                throw new Exception("Expected two connection handles, but was  " + numConnectionHandles2);
            if (numManagedConns != 1)
                throw new Exception("Expected one managed connection, but was  " + numManagedConns);
            if (numFreeConns1 != 0)
                throw new Exception("Expected zero free connections, but was  " + numFreeConns1);
            if (numFreeConns2 != 1)
                throw new Exception("Expected one free connection, but was  " + numFreeConns2);
            if (finalDestroyCount != initialDestoryCount + 1)
                throw new Exception("Expected DestroyCount to increase by 1.  Initial " + initialDestoryCount + " final " + finalDestroyCount);

        } finally {
            if (conn != null && !conn.isClosed())
                conn.close();
            if (conn2 != null && !conn2.isClosed())
                conn2.close();
            if (conn3 != null && !conn3.isClosed())
                conn3.close();
        }
    }

    public void testMultiThread(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        Connection con = multiThreadDS.getConnection(); //ensure the MBean exists
        con.close();
        mbeanServer.invoke(getMBeanObjectInstance("jdbc/multiThreadDS").getObjectName(), "purgePoolContents", new Object[] { "abort" }, null); //clear any connections

        ObjectName objName = getConnPoolStatsMBeanObjName("multiThreadDS");
        int initialCreateCount = getMonitorData(objName, "CreateCount");
        final List<Long> listUseTimes = Collections.synchronizedList(new ArrayList<Long>());
        int numThreads = 75;

        ExecutorService exec = Executors.newFixedThreadPool(numThreads);
        try {
            int i = numThreads;
            while (i > 0) {
                exec.execute(new Runnable() {
                    @Override
                    public void run() {
                        final ThreadLocal<Long> tlocalStartUseConn = new ThreadLocal<Long>();
                        final ThreadLocal<Long> tlocalEndUseConn = new ThreadLocal<Long>();
                        Connection conn = null;
                        try {
                            conn = multiThreadDS.getConnection();
                            tlocalStartUseConn.set(System.currentTimeMillis());
                        } catch (SQLException e) {
                            e.printStackTrace();
                        } finally {
                            try {
                                conn.close();
                                tlocalEndUseConn.set(System.currentTimeMillis());
                                Long differenceUseTimes = tlocalEndUseConn.get() - tlocalStartUseConn.get();
                                listUseTimes.add(differenceUseTimes);
                            } catch (SQLException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });
                i--;
            }

        } finally {
            if (exec != null)
                exec.shutdown();
            try {
                exec.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            } catch (InterruptedException e) {
            }
        }
        long sumUseTime = 0L;
        for (final Long item : listUseTimes) {
            sumUseTime += item;
        }
        double averageUseTime = (double) sumUseTime / numThreads;

        int numFreeConns = getMonitorData(objName, "FreeConnectionCount");
        int finalCreateCount = getMonitorData(objName, "CreateCount");
        int numManagedConns = getMonitorData(objName, "ManagedConnectionCount");
        double inUseTime = (Double) mbeanServer.getAttribute(objName, "InUseTime");

        if (finalCreateCount != initialCreateCount + 50) //max connections = 50
            throw new Exception("Expected CreateCount to increase by 50. Initial " + initialCreateCount + " final " + finalCreateCount);
        if (numManagedConns != 50) //max connections = 50
            throw new Exception("Expected ManagedConnectionCount to be 50, but was: " + numManagedConns);
        if (numFreeConns != 50)
            throw new Exception("Expected FreeConnectionCount to be 50, but was: " + numFreeConns);
        if (Double.compare(averageUseTime - 30, inUseTime) > 0 || Double.compare(inUseTime, averageUseTime + 30) > 0) //30 ms +- average
            throw new Exception("Expected InUseTime to be " + averageUseTime + " but was: " + inUseTime);
    }

    private int getMonitorData(ObjectName name, String attribute) throws Exception {
        return Integer.parseInt((mbeanServer.getAttribute(name, attribute)).toString());
    }

    private ConnectionManagerMBean getConnectionManagerBean(String jndiName) throws Exception {
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        ObjectName obn = new ObjectName("WebSphere:type=" + MBEAN_TYPE + ",jndiName=" + jndiName + ",*");
        Set<ObjectInstance> s = mbs.queryMBeans(obn, null);
        if (s.size() != 1) {
            System.out.println("ERROR: Found incorrect number of MBeans (" + s.size() + ")");
            for (ObjectInstance i : s)
                System.out.println("  Found MBean: " + i.getObjectName());
            throw new Exception("Expected to find exactly 1 MBean, instead found " + s.size());
        }
        return JMX.newMBeanProxy(mbs, s.iterator().next().getObjectName(), ConnectionManagerMBean.class);
    }

    private ObjectName getConnPoolStatsMBeanObjName(String dsName) throws Exception {
        Set<ObjectInstance> mxBeanSet;
        ObjectInstance oi;
        mxBeanSet = mbeanServer.queryMBeans(new ObjectName("WebSphere:type=ConnectionPoolStats,name=*" + dsName + ",*"), null);
        if (mxBeanSet != null && mxBeanSet.size() > 0) {
            Iterator<ObjectInstance> it = mxBeanSet.iterator();
            oi = it.next();
            return oi.getObjectName();
        } else
            throw new Exception("ConnectionPoolStatsMBean:NotFound");
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

}
