/*******************************************************************************
 * Copyright (c) 2017,2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package web;

import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.ejb.EJBContext;
import javax.management.MBeanServer;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.naming.InitialContext;
import javax.resource.spi.BootstrapContext;
import javax.resource.spi.XATerminator;
import javax.resource.spi.work.ExecutionContext;
import javax.resource.spi.work.TransactionContext;
import javax.resource.spi.work.WorkManager;
import javax.sql.DataSource;
import javax.transaction.UserTransaction;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import componenttest.app.FATServlet;

public class DerbyRAServlet extends FATServlet {
    private static final long serialVersionUID = 7709282314904580334L;

    public static MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();

    /**
     * Maximum number of milliseconds a test should wait for something to happen
     */
    private static final long TIMEOUT = 5000;

    /**
     * Verify that an admin object can be looked up directly as java.util.Map.
     */
    public void testAdminObjectDirectLookup() throws Throwable {
        @SuppressWarnings("unchecked")
        Map<String, String> map1 = (Map<String, String>) new InitialContext().lookup("eis/map1");
        try {
            map1.put("test", "testAdminObjectDirectLookup");
            String value = map1.get("test");
            if (!"testAdminObjectDirectLookup".equals(value))
                throw new Exception("Unexpected value: " + value);
        } finally {
            map1.clear();
        }
    }

    /**
     * Verify that an admin object, injected into DerbyRRAnnoServlet under java:app, can be looked up as java.util.Map.
     */
    public void testAdminObjectLookUpJavaApp() throws Throwable {
        @SuppressWarnings("unchecked")
        Map<String, String> map1 = (Map<String, String>) new InitialContext().lookup("java:app/env/eis/map1ref"); // from DerbyRAAnnoServlet
        try {
            map1.put("testName", "testAdminObjectLookUpJavaApp");
            String value = map1.remove("testName");
            if (!"testAdminObjectLookUpJavaApp".equals(value))
                throw new Exception("Unexpected value: " + value);
        } finally {
            map1.clear();
        }
    }

    /**
     * Verify that an admin object can be looked up via resource-env-ref as java.util.Map.
     */
    public void testAdminObjectResourceEnvRef() throws Throwable {

        @SuppressWarnings("unchecked")
        final Map<String, String> map1 = (Map<String, String>) new InitialContext().lookup("java:comp/env/eis/map1ref");
        try {
            map1.clear();
            if (!map1.isEmpty())
                throw new Exception("Not empty after clear");

            map1.put("k1", "v1");
            String previous = map1.put("k2", "v2");
            if (previous != null)
                throw new Exception("Should not have previous value: " + previous);

            int size = map1.size();
            if (size != 2)
                throw new Exception("Expect 2 entries in map, not " + size);

            String value = map1.get("k1");
            if (!"v1".equals(value))
                throw new Exception("Got wrong value: " + value);

            previous = map1.put("k1", "value1");
            if (!"v1".equals(previous))
                throw new Exception("Wrong previous value: " + previous);

            if (!map1.containsKey("k2"))
                throw new Exception("Doesn't contain key k2");

            if (map1.containsKey("k3"))
                throw new Exception("Erroneously says it contains key k3");

            if (map1.containsValue("v1"))
                throw new Exception("Erroneously says it contains value v1");

            if (!map1.containsValue("value1"))
                throw new Exception("Doesn't contain value value1");

            Set<Entry<String, String>> entries = map1.entrySet();
            if (entries.size() != 2)
                throw new Exception("wrong number of entries " + entries);

            String entriesToString = entries.toString();
            if (!entriesToString.contains("k1")
                || !entriesToString.contains("k2")
                || !entriesToString.contains("value1")
                || !entriesToString.contains("v2"))
                throw new Exception("Wrong entries: " + entries);

            value = map1.remove("k1");
            if (!"value1".equals(value))
                throw new Exception("Remove saw wrong value: " + value);

            Set<String> keys = map1.keySet();
            if (keys.size() != 1 || !keys.contains("k2"))
                throw new Exception("Wrong set of keys: " + keys);

            // Run without thread context to test that the resource adapter can lookup a java:global name
            // via a contextual proxy that it captured during start.
            ExecutorService unmanagedExecutor = Executors.newSingleThreadExecutor();
            Collection<String> values;
            try {
                values = unmanagedExecutor.submit(new Callable<Collection<String>>() {
                    @Override
                    public Collection<String> call() throws Exception {
                        return map1.values();
                    }
                }).get(TIMEOUT, TimeUnit.MILLISECONDS);
            } finally {
                unmanagedExecutor.shutdown();
            }
            if (values.size() != 1 || !values.contains("v2"))
                throw new Exception("Wrong set of values: " + values);
        } finally {
            map1.clear();
        }
    }

    /**
     * Test for execution context where we roll back two units of work that share the same ExecutionContext (and thus the same xid).
     */
    public void testExecutionContext() throws Throwable {
        InitialContext initialContext = new InitialContext();
        BootstrapContext bootstrapContext = (BootstrapContext) initialContext.lookup("java:global/env/eis/bootstrapContext");
        DataSource ds1 = (DataSource) initialContext.lookup("java:comp/env/eis/ds1ref");
        Connection con = ds1.getConnection();
        try {
            // create the table
            Statement stmt = con.createStatement();
            try {
                stmt.executeUpdate("create table TestExecutionContextTBL (col1 int not null primary key, col2 varchar(50))");
                stmt.executeUpdate("insert into TestExecutionContextTBL values (1, 'one')");
                stmt.executeUpdate("insert into TestExecutionContextTBL values (2, 'two')");
            } finally {
                stmt.close();
            }

            // roll back inflown transaction
            Xid xid = new FATXID();
            ExecutionContext executionContext = new ExecutionContext();
            executionContext.setXid(xid);
            FATWork work = new FATWork("java:comp/env/eis/ds1ref", "update TestExecutionContextTBL set col2='uno' where col1=1");
            bootstrapContext.getWorkManager().doWork(work, WorkManager.INDEFINITE, executionContext, null);
            try {
                // reuse the execution context
                work = new FATWork("java:comp/env/eis/ds1ref", "update TestExecutionContextTBL set col2='dos' where col1=2");
                bootstrapContext.getWorkManager().doWork(work, WorkManager.INDEFINITE, executionContext, null);
            } finally {
                XATerminator xaTerminator = bootstrapContext.getXATerminator();
                int vote = xaTerminator.prepare(xid);
                if (vote == XAResource.XA_OK)
                    xaTerminator.rollback(xid);
                else
                    throw new Exception("Unexpected vote: " + vote);
            }

            // validate results
            stmt = con.createStatement();
            try {
                ResultSet results = stmt.executeQuery("select col1, col2 from TestExecutionContextTBL");
                Map<Integer, String> resultMap = new TreeMap<Integer, String>();
                while (results.next())
                    resultMap.put(results.getInt(1), results.getString(2));
                results.close();

                if (resultMap.size() != 2
                    || !"one".equals(resultMap.get(1))
                    || !"two".equals(resultMap.get(2)))
                    throw new Exception((new StringBuilder()).append("Unexpected results: ").append(resultMap).toString());
            } finally {
                stmt.close();
            }
        } finally {
            con.close();
        }
    }

    /**
     * Verify that a JCA data source can be looked up directly.
     */
    public void testJCADataSourceDirectLookup() throws Throwable {

        DataSource ds1 = (DataSource) new InitialContext().lookup("eis/ds1");
        int loginTimeout = ds1.getLoginTimeout();
        if (loginTimeout != 120)
            throw new Exception("Override of default loginTimeout in wlp-ra.xml not honored. Instead: " + loginTimeout);

        Connection con = ds1.getConnection("dbuser1", "dbpwd1");
        try {
            String userName = con.getMetaData().getUserName();
            if (!"dbuser1".equals(userName))
                throw new Exception("User name doesn't match. Instead: " + userName);

            Statement stmt = con.createStatement();
            try {
                ResultSet result = con.createStatement().executeQuery("values length('abcdefghijklmnopqrstuvwxyz')");

                if (!result.next())
                    throw new Exception("Missing result of query");

                int value = result.getInt(1);
                if (value != 26)
                    throw new Exception("Unexpected value: " + value);
            } finally {
                stmt.close();
            }
        } finally {
            con.close();
        }
    }

    /**
     * Verify that a JCA data source, injected into DerbyRRAnnoServlet under java:module, can be looked up as java.util.Map.
     */
    public void testJCADataSourceLookUpJavaModule() throws Throwable {

        DataSource ds1 = (DataSource) new InitialContext().lookup("java:module/env/eis/ds1ref");
        Connection con = ds1.getConnection();
        try {
            String userName = con.getMetaData().getUserName();
            if (!"DS1USER".equals(userName))
                throw new Exception("User name doesn't match configured value on containerAuthData. Instead: " + userName);

            Statement stmt = con.createStatement();
            try {
                ResultSet result = con.createStatement().executeQuery("values mod(87, 16)");

                if (!result.next())
                    throw new Exception("Missing result of query");

                int value = result.getInt(1);
                if (value != 7)
                    throw new Exception("Unexpected value: " + value);
            } finally {
                stmt.close();
            }
        } finally {
            con.close();
        }
    }

    /**
     * Verify that a JCA data source can be looked up via resource ref.
     */
    public void testJCADataSourceResourceRef() throws Throwable {

        DataSource ds1 = (DataSource) new InitialContext().lookup("java:comp/env/eis/ds1ref");
        Connection con = ds1.getConnection();
        try {
            String userName = con.getMetaData().getUserName();
            if (!"DS1USER".equals(userName))
                throw new Exception("User name doesn't match configured value on containerAuthData. Instead: " + userName);

            Statement stmt = con.createStatement();
            try {
                ResultSet result = con.createStatement().executeQuery("values absval(-40)");

                if (!result.next())
                    throw new Exception("Missing result of query");

                int value = result.getInt(1);
                if (value != 40)
                    throw new Exception("Unexpected value: " + value);
            } finally {
                stmt.close();
            }
        } finally {
            con.close();
        }
    }

    /**
     * Verify that a principal is added to the RA subject.
     */
    public void testJCADataSourceResourceRefSecurity() throws Throwable {

        DataSource ds1 = (DataSource) new InitialContext().lookup("java:comp/env/eis/ds1ref");
        Connection con = ds1.getConnection();

        try {
            String userName = con.getMetaData().getUserName();
            if (!"DS1USER".equals(userName))
                throw new Exception("User name doesn't match configured value on containerAuthData. Instead: " + userName);

            if (con.toString().contains("WSPrincipal:") == false) {
                throw new Exception("The subject does not contain a principal. " + con.toString());
            }
        } finally {
            con.close();
        }
    }

    /**
     * Test for transaction context where we do two-phase commit of two resources with the same xid but different timeout.
     */
    public void testTransactionContext() throws Throwable {
        InitialContext initialContext = new InitialContext();
        BootstrapContext bootstrapContext = (BootstrapContext) initialContext.lookup("java:global/env/eis/bootstrapContext");
        DataSource ds1 = (DataSource) initialContext.lookup("java:comp/env/eis/ds1ref");
        Connection con = ds1.getConnection();
        try {
            // create the table
            Statement stmt = con.createStatement();
            try {
                stmt.executeUpdate("create table TestTransactionContextTBL (col1 int not null primary key, col2 varchar(50))");
                stmt.executeUpdate("insert into TestTransactionContextTBL values (3, 'three')");
                stmt.executeUpdate("insert into TestTransactionContextTBL values (4, 'four')");
            } finally {
                stmt.close();
            }

            // commit inflown transaction
            Xid xid = new FATXID();
            TransactionContext transactionContext = new TransactionContext();
            transactionContext.setXid(xid);
            FATWork work = new FATWorkAndContext("java:comp/env/eis/ds1ref", "update TestTransactionContextTBL set col2='III' where col1=3", transactionContext);
            bootstrapContext.getWorkManager().doWork(work, WorkManager.INDEFINITE, null, null);
            try {
                // create new transaction context for same xid, but different transaction timeout
                transactionContext = new TransactionContext();
                transactionContext.setXid(xid);
                transactionContext.setTransactionTimeout(5000);
                work = new FATWorkAndContext("java:comp/env/eis/ds1ref", "update TestTransactionContextTBL set col2='IV' where col1=4", transactionContext);
                bootstrapContext.getWorkManager().doWork(work, WorkManager.INDEFINITE, null, null);
            } finally {
                XATerminator xaTerminator = bootstrapContext.getXATerminator();
                int vote = xaTerminator.prepare(xid);
                if (vote == XAResource.XA_OK)
                    xaTerminator.commit(xid, false);
                else
                    throw new Exception("Unexpected vote: " + vote);
            }

            // validate results
            stmt = con.createStatement();
            try {
                ResultSet results = stmt.executeQuery("select col1, col2 from TestTransactionContextTBL");
                Map<Integer, String> resultMap = new TreeMap<Integer, String>();
                while (results.next())
                    resultMap.put(results.getInt(1), results.getString(2));
                results.close();

                if (resultMap.size() != 2
                    || !"III".equals(resultMap.get(3))
                    || !"IV".equals(resultMap.get(4)))
                    throw new Exception((new StringBuilder()).append("Unexpected results: ").append(resultMap).toString());
            } finally {
                stmt.close();
            }
        } finally {
            con.close();
        }
    }

    /**
     * Use an unshared connection within two EJB methods that run under different transactions, one of which rolls back
     * and the other of which commits.
     */
    public void testUnsharableConnectionAcrossEJBGlobalTran() throws Exception {
        DerbyRABean bean = InitialContext.doLookup("java:global/derbyRAApp/fvtweb/DerbyRABean!web.DerbyRABean");
        final DataSource ds = (DataSource) InitialContext.doLookup("java:module/env/eis/ds5ref-unshareable");
        final Connection[] c = new Connection[1];
        c[0] = ds.getConnection();
        try {
            c[0].createStatement().execute("create table testUnsharableConEJBTable (name varchar(40) not null primary key, val int not null)");
            c[0].close();

            bean.runInNewGlobalTran(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    c[0] = ds.getConnection();
                    c[0].createStatement().executeUpdate("insert into testUnsharableConEJBTable values ('first', 1)");

                    DerbyRABean bean = InitialContext.doLookup("java:global/derbyRAApp/fvtweb/DerbyRABean!web.DerbyRABean");
                    bean.runInNewGlobalTran(new Callable<Integer>() {
                        @Override
                        public Integer call() throws Exception {
                            return c[0].createStatement().executeUpdate("insert into testUnsharableConEJBTable values ('second', 2)");
                        }
                    });

                    EJBContext ejbContext = InitialContext.doLookup("java:comp/EJBContext");
                    ejbContext.setRollbackOnly();
                    return null;
                }
            });

            int updateCount;
            updateCount = c[0].createStatement().executeUpdate("delete from testUnsharableConEJBTable where name='second'");
            // TODO connection is not enlisting in the transaction of the second EJB method.
            //if (updateCount != 1)
            //    throw new Exception("Did not find the entry that should have been committed under the EJB transaction.");

            updateCount = c[0].createStatement().executeUpdate("delete from testUnsharableConEJBTable where name='first'");
            if (updateCount != 0)
                throw new Exception("Found an entry that should have been rolled back under the Servlet's global transaction.");

        } finally {
            c[0].close();
        }
    }

    public void testConnPoolStatsExceptionInDestroy() throws Exception {
        UserTransaction tran = (UserTransaction) new InitialContext().lookup("java:comp/UserTransaction");
        tran.begin();
        DataSource ds2 = (DataSource) new InitialContext().lookup("eis/ds2");
        Connection conn = null;
        try {
            conn = ds2.getConnection();
            ObjectName objName = getConnPoolStatsMBeanObjName("ds2");
            int initialDestroyCount = getMonitorData(objName, "DestroyCount");
            mbeanServer.invoke(getMBeanObjectInstance("eis/ds2").getObjectName(), "purgePoolContents", new Object[] { "Normal" }, null);
            tran.commit();

            int finalDestroyCount = getMonitorData(objName, "DestroyCount");
            int numManagedConns = getMonitorData(objName, "ManagedConnectionCount");
            int numFreeConns = getMonitorData(objName, "FreeConnectionCount");
            if (finalDestroyCount != initialDestroyCount + 1) { //Should increase by 1 even though there is an exception destroying one connection
                throw new Exception("Expected destroyCount to increase by one. Initial: " + initialDestroyCount + " Final: " + finalDestroyCount);
            }
            if (numManagedConns != 0) { //Should be 0
                throw new Exception("Expected 0 managed connections, but found: " + numManagedConns);
            }
            if (numFreeConns != 0) { //Should be 0
                throw new Exception("Expected 0 free connections, but found: " + numFreeConns);
            }

        } finally {
            if (conn != null)
                conn.close();
        }
    }

    public void testErrorInFreeConn() throws Exception {
        DataSource ds = (DataSource) new InitialContext().lookup("eis/ds4");
        Object managedConn = null;
        Connection con = null;
        Class<?> derbyConnClass = null;
        try {
            con = ds.getConnection();
            derbyConnClass = con.getClass();
            Field f = derbyConnClass.getDeclaredField("mc");
            managedConn = f.get(con);
            Statement stmt = con.createStatement();
            stmt.close();
        } finally {
            con.close();
        }

        SQLException sqe = new SQLException("APP_SPECIFIED_CONN_ERROR");

        Class<?> c = managedConn.getClass();
        Method m = c.getMethod("notify", int.class, derbyConnClass, Exception.class);
        m.invoke(managedConn, 5, con, sqe); //5 indicates connection error

        String contents = (String) mbeanServer.invoke(getMBeanObjectInstance("eis/ds4").getObjectName(), "showPoolContents", null, null);
        int begin = contents.indexOf("size=");
        int end = contents.indexOf(System.lineSeparator(), begin);
        int poolSizeAfterError = Integer.parseInt(contents.substring(begin + 5, end).trim());

        //After the error, there should be 0 connections in the pool.
        if (poolSizeAfterError != 0)
            throw new Exception("Unexpected number of connections found.  Expected 0 but found " + poolSizeAfterError);
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

    private ObjectInstance getMBeanObjectInstance(String jndiName) throws Exception {
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        ObjectName obn = new ObjectName("WebSphere:type=com.ibm.ws.jca.cm.mbean.ConnectionManagerMBean,jndiName=" + jndiName + ",*");
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
