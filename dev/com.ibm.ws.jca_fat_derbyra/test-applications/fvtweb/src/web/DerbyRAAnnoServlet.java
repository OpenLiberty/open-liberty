/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLSyntaxErrorException;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.Resource;
import javax.naming.InitialContext;
import javax.resource.spi.BootstrapContext;
import javax.resource.spi.XATerminator;
import javax.resource.spi.work.ExecutionContext;
import javax.resource.spi.work.WorkManager;
import javax.servlet.ServletException;
import javax.sql.CommonDataSource;
import javax.sql.DataSource;
import javax.transaction.HeuristicMixedException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.TransactionSynchronizationRegistry;
import javax.transaction.UserTransaction;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import componenttest.app.FATServlet;

public class DerbyRAAnnoServlet extends FATServlet {

    private static final long serialVersionUID = 7440572626782340872L;

    @Resource(name = "java:global/env/eis/bootstrapContext", lookup = "eis/bootstrapContext")
    private BootstrapContext bootstrapContext;

    @Resource(lookup = "eis/ds1")
    private CommonDataSource cds1;

    @Resource(name = "java:module/env/eis/ds1ref", lookup = "eis/ds1")
    private DataSource ds1;

    @Resource(name = "java:app/env/eis/map1ref", lookup = "eis/map1")
    private Map<String, String> map1;

    @Resource
    private UserTransaction tran;

    // lookup is defined via binding-name in ibm-web-bnd.xml
    @Resource(name = "eis/loginModuleCFRef")
    DataSource loginModuleCF;

    /**
     * Maximum number of nanoseconds a test should wait for something to happen
     */
    private static final long TIMEOUT_NS = TimeUnit.SECONDS.toNanos(20);

    /**
     * One-time initialization for servlet
     */
    @Override
    public void init() throws ServletException {
        try {
            Connection con = ds1.getConnection();
            try {
                // create table to be used by Message Driven Bean
                Statement stmt = con.createStatement();
                try {
                    stmt.executeUpdate("create table TestActivationSpecTBL (id varchar(50) not null primary key, oldValue varchar(50))");
                } finally {
                    stmt.close();
                }
            } finally {
                con.close();
            }
        } catch (SQLException x) {
            throw new ServletException(x);
        }
    }

    /**
     * Verify that a message driven bean is invoked.
     */
    public void testActivationSpec() throws Throwable {
        try {
            assertNull(map1.put("mdbtestActvSpec", "value1"));
            assertEquals("value1", map1.put("mdbtestActvSpec", "value2"));

            // Database entry inserted by message driven bean should show up in a reasonable amount of time
            String oldValueFromDB = null;
            Connection con = ds1.getConnection();
            try {
                PreparedStatement pstmt = con.prepareStatement("select oldValue from TestActivationSpecTBL where id=?");
                pstmt.setString(1, "mdbtestActvSpec");
                for (long start = System.nanoTime(); oldValueFromDB == null && System.nanoTime() - start < TIMEOUT_NS; TimeUnit.MILLISECONDS.sleep(200)) {
                    ResultSet result = pstmt.executeQuery();
                    if (result.next())
                        oldValueFromDB = result.getString(1);
                    result.close();
                }
                pstmt.close();
            } finally {
                con.close();
            }
            assertEquals("value1", oldValueFromDB);
        } finally {
            map1.clear();
        }
    }

    /**
     * Verify XA recovery for activation specs
     */
    public void testActivationSpecXARecovery() throws Throwable {
        try {
            assertNull(map1.put("mdbtestRecovery", "valueA"));
            assertEquals("valueA", map1.put("mdbtestRecovery", "valueB"));

            // Database entry inserted by message driven bean should show up in a reasonable amount of time
            String oldValueFromDB = null;
            DataSource ds = InitialContext.doLookup("eis/ds1");
            Connection con = ds.getConnection("ActvSpecUser", "ActvSpecPwd");
            try {
                PreparedStatement pstmt = null;
                for (long start = System.nanoTime(); oldValueFromDB == null && System.nanoTime() - start < TIMEOUT_NS; TimeUnit.MILLISECONDS.sleep(200))
                    try {
                        if (pstmt == null) {
                            pstmt = con.prepareStatement("select description from TestActivationSpecRecoveryTBL where id=?");
                            pstmt.setString(1, "mdbtestRecovery");
                        }
                        ResultSet result = pstmt.executeQuery();
                        if (result.next())
                            oldValueFromDB = result.getString(1);
                        result.close();
                    } catch (SQLSyntaxErrorException x) {
                        if ("42X05".equals(x.getSQLState()) || // table does not exist
                            "42Y07".equals(x.getSQLState())) // schema does not exist
                            ; // keep trying until test case creates it
                        else
                            throw x;
                    }
                if (pstmt != null)
                    pstmt.close();
            } finally {
                con.close();
            }
            assertEquals("mdbtestRecovery: valueA --> valueB", oldValueFromDB);
        } finally {
            map1.clear();
        }
    }

    public void testCustomLoginModuleCF() throws Exception {
        Connection con = loginModuleCF.getConnection();
        try {
            String user = con.getMetaData().getUserName();
            assertEquals("Incorrect user for loginModuleCF", "loginModuleUser", user);
        } finally {
            con.close();
        }
    }

    /**
     * Verify that an admin object can be injected as java.util.Map.
     */
    public void testAdminObjectInjected() throws Throwable {
        try {
            map1.put("testCase", "testAdminObjectInjected");
            Entry<String, String> entry = map1.entrySet().iterator().next();

            if (!"testCase".equals(entry.getKey()))
                throw new Exception("Unexpected key: " + entry.getKey());

            if (!"testAdminObjectInjected".equals(entry.getValue()))
                throw new Exception("Unexpected value: " + entry.getValue());
        } finally {
            map1.clear();
        }
    }

    /**
     * Verify that a JCA data source can be injected.
     */
    public void testJCADataSourceInjected() throws Throwable {

        Connection con = ds1.getConnection();
        try {
            String userName = con.getMetaData().getUserName();
            if (!"DS1USER".equals(userName))
                throw new Exception("User name doesn't match. Instead: " + userName);

            Statement stmt = con.createStatement();
            try {
                ResultSet result = con.createStatement().executeQuery("values sign(-3.14159)");

                if (!result.next())
                    throw new Exception("Missing result of query");

                int value = result.getInt(1);
                if (value != -1)
                    throw new Exception("Unexpected value: " + value);
            } finally {
                stmt.close();
            }
        } finally {
            con.close();
        }
    }

    /**
     * Verify that a JCA data source can be injected as super interface java.sql.CommonDataSource.
     */
    public void testJCADataSourceInjectedAsCommonDataSource() throws Throwable {

        int loginTimeout = cds1.getLoginTimeout();
        if (loginTimeout != 120)
            throw new Exception("Unexpected loginTimeout: " + loginTimeout);
    }

    /**
     * Schedule timer that schedules another timer that schedules another timer ... and so forth.
     * With each invocation, verify that thread context classloader can load classes and that a new transaction can be started.
     */
    public void testRecursiveTimer() throws Exception {
        Timer timer = bootstrapContext.createTimer();
        try {
            LinkedBlockingQueue<Object> resultQueue = new LinkedBlockingQueue<Object>();
            RecursiveTimerTask task = new RecursiveTimerTask(new AtomicInteger(4), new AtomicLong(), resultQueue, timer, tran);
            timer.scheduleAtFixedRate(task, 0, 1);

            Object result = resultQueue.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS);
            if (result instanceof Throwable)
                throw new Exception("Timer task failed. See cause.", (Throwable) result);
            if (!Long.valueOf(10).equals(result))
                throw new Exception("Timer tasks likely did not run the correct number of times because sum is: " + result);
        } finally {
            timer.cancel();
        }
    }

    /**
     * Use TransactionSynchronizationRegistry to check transaction status and mark transactions to be rolled back.
     */
    public void testTransactionSynchronizationRegistry() throws Exception {
        TransactionSynchronizationRegistry tranSyncRegistry = bootstrapContext.getTransactionSynchronizationRegistry();

        int status = tranSyncRegistry.getTransactionStatus();
        if (status != Status.STATUS_NO_TRANSACTION)
            throw new Exception("Unexpected status when not in a transaction: " + status);

        Connection con = ds1.getConnection();
        try {
            Statement stmt = con.createStatement();
            try {
                stmt.executeUpdate("create table TestTranSyncRegistryTBL (id int not null primary key, val varchar(60))");
            } finally {
                stmt.close();
            }
        } finally {
            con.close();
        }

        tran.begin();
        try {
            status = tranSyncRegistry.getTransactionStatus();
            if (status != Status.STATUS_ACTIVE)
                throw new Exception("Unexpected status when inside a transaction: " + status);

            tranSyncRegistry.putResource("TestResourceKey1", "TestResourceValue1");

            Object resource = tranSyncRegistry.getResource("TestResourceKey1");
            if (!"TestResourceValue1".equals(resource))
                throw new Exception("Unexpected resource within transaction: " + resource);

            Object key = tranSyncRegistry.getTransactionKey();
            if (key == null)
                throw new Exception("Should not have null key within transaction");

            Object key2 = tranSyncRegistry.getTransactionKey();
            if (!key.equals(key2))
                throw new Exception("Key should not change within transaction: " + key + " --> " + key2);

            if (tranSyncRegistry.getRollbackOnly())
                throw new Exception("Not expecting rollback only");

            con = ds1.getConnection();
            try {
                Statement stmt = con.createStatement();
                try {
                    stmt.executeUpdate("insert into TestTranSyncRegistryTBL values (548, 'five hundred and forty-eight')");
                } finally {
                    stmt.close();
                }
            } finally {
                con.close();
            }

            tranSyncRegistry.setRollbackOnly();

            status = tranSyncRegistry.getTransactionStatus();
            if (status != Status.STATUS_MARKED_ROLLBACK)
                throw new Exception("Unexpected status after marked rollback only: " + status);

            if (!tranSyncRegistry.getRollbackOnly())
                throw new Exception("Expecting rollback only");
        } finally {
            tran.rollback();
        }

        status = tranSyncRegistry.getTransactionStatus();
        if (status != Status.STATUS_NO_TRANSACTION)
            throw new Exception("Unexpected status after transaction completes: " + status);

        final List<String> list = new LinkedList<String>();

        tran.begin();
        try {
            tranSyncRegistry.registerInterposedSynchronization(new Synchronization() {
                @Override
                public void afterCompletion(int status) {
                    list.add("after: " + status);
                }

                @Override
                public void beforeCompletion() {
                    list.add("before");
                }
            });

            con = ds1.getConnection();
            try {
                Statement stmt = con.createStatement();
                try {
                    stmt.executeUpdate("insert into TestTranSyncRegistryTBL values (548, 'FIVE HUNDRED AND FORTY-EIGHT')");
                } finally {
                    stmt.close();
                }
            } finally {
                con.close();
            }

            if (tranSyncRegistry.getRollbackOnly())
                throw new Exception("Second transaction should not be rollback only");

            Object resource = tranSyncRegistry.getResource("TestResourceKey1");
            if (resource != null)
                throw new Exception("Resource shouldn't be present in new transaction: " + resource);
        } finally {
            tran.commit();
        }

        if (list.size() != 2)
            throw new Exception("Missing beforeCompletion, afterCompletion, or both from: " + list);
    }

    /**
     * Intentionally cause an in-doubt transaction and verify that the transaction manager successfully recovers it
     * by committing the updates to the database.
     */
    public void testXARecovery() throws Exception {
        Connection con = ds1.getConnection();
        try {
            // create table
            Statement stmt = con.createStatement();
            stmt.executeUpdate("create table TestXARecoveryTBL (id int not null primary key, value varchar(30))");
            stmt.close();
        } finally {
            con.close();
        }

        DataSource ds3 = InitialContext.doLookup("eis/ds3");

        boolean commitAttempted = false;
        tran.begin();
        try {
            Connection con1 = ds1.getConnection();
            try {
                Statement s1 = con1.createStatement();
                s1.executeUpdate("insert into TestXARecoveryTBL values (1, 'VALUE FROM DS1')");
                s1.close();
            } finally {
                con1.close();
            }

            Connection con2 = ds3.getConnection();
            try {
                Statement s2 = con2.createStatement();
                s2.executeUpdate("insert into TestXARecoveryTBL values (2, 'VALUE FROM DS3')");
                s2.close();
            } finally {
                con2.close();
            }

            try {
                commitAttempted = true;
                tran.commit();
                fail("Commit should not have succeeded because the test infrastructure is supposed to cause an in-doubt transaction.");
            } catch (HeuristicMixedException x) {
                // Adjust the XA success limit, so as to allow XA recovery to commit the in-doubt transaction
                ds3.unwrap(AtomicInteger.class).set(1); // TODO this should be removed once the JCA integration layer is updated to set the QMID
                System.out.println("Caught expected exception: " + x);
            }
        } finally {
            if (!commitAttempted)
                tran.rollback();
        }

        System.out.println("--- attempting to access data (only possible after recovery) ---");

        con = ds1.getConnection();
        try {
            PreparedStatement ps = con.prepareStatement("select value from TestXARecoveryTBL where id=?");

            ps.setInt(1, 2);
            ResultSet result = ps.executeQuery();
            assertTrue(result.next());
            assertEquals("VALUE FROM DS3", result.getString(1));
            result.close();

            ps.setInt(1, 1);
            result = ps.executeQuery();
            assertTrue(result.next());
            assertEquals("VALUE FROM DS1", result.getString(1));
            result.close();

            ps.close();
        } finally {
            con.close();
        }
    }

    /**
     * Use XATerminator to commit or roll back a transaction.
     */
    public void testXATerminator() throws Exception {
        XATerminator xaTerminator = bootstrapContext.getXATerminator();

        Connection con = ds1.getConnection();
        try {
            // create table
            Statement stmt = con.createStatement();
            try {
                stmt.executeUpdate("create table TestXATerminatorTBL (id int not null primary key, value varchar(40))");
            } finally {
                stmt.close();
            }

            tran.begin();
            try {
                stmt = con.createStatement();
                try {
                    stmt.executeUpdate("insert into TestXATerminatorTBL values (1, 'A')");

                    // commit some work in an inflown transaction
                    Xid xid = new FATXID();
                    ExecutionContext executionContext = new ExecutionContext();
                    executionContext.setXid(xid);
                    FATWork work = new FATWork("java:module/env/eis/ds1ref", "insert into TestXATerminatorTBL values (2, 'B')");
                    FATWorkListener listener = new FATWorkListener();
                    bootstrapContext.getWorkManager().startWork(work, WorkManager.INDEFINITE, executionContext, listener);
                    try {
                        // Make another update in the main transaction
                        stmt.executeUpdate("insert into TestXATerminatorTBL values (3, 'C')");
                    } finally {
                        listener.latch.await(TIMEOUT_NS, TimeUnit.NANOSECONDS);
                        xaTerminator.commit(xid, true);
                    }
                    if (listener.failure != null)
                        throw new Exception("Failure occured. See cause.", listener.failure);
                } catch (XAException xae) {
                    throw new Exception("error code is " + xae.errorCode, xae);
                } finally {
                    stmt.close();
                }
            } finally {
                tran.rollback();
            }

            // verify entries 1 and 3 are rolled back, but 2 is committed.
            stmt = con.createStatement();
            try {
                int updateCount = stmt.executeUpdate("delete from TestXATerminatorTBL where id=2");
                if (updateCount != 1)
                    throw new Exception("Work done under inflown transaction should have been committed. Update count: " + updateCount);

                updateCount = stmt.executeUpdate("delete from TestXATerminatorTBL");
                if (updateCount > 0)
                    throw new Exception("Main transaction should have been rolled back. Update count: " + updateCount);
            } finally {
                stmt.close();
            }

            tran.begin();
            try {
                stmt = con.createStatement();
                try {
                    stmt.executeUpdate("insert into TestXATerminatorTBL values (4, 'D')");

                    // roll back some work in an inflown transaction
                    Xid xid = new FATXID();
                    FATTransactionContext transactionContext = new FATTransactionContext();
                    transactionContext.setXid(xid);
                    FATWork work = new FATWorkAndContext("java:module/env/eis/ds1ref", "insert into TestXATerminatorTBL values (5, 'E')", transactionContext);
                    FATWorkListener listener = new FATWorkListener();
                    bootstrapContext.getWorkManager().startWork(work, WorkManager.INDEFINITE, null, listener);
                    try {
                        // Make another update in the main transaction
                        stmt.executeUpdate("insert into TestXATerminatorTBL values (6, 'F')");
                    } finally {
                        listener.latch.await(TIMEOUT_NS, TimeUnit.NANOSECONDS);
                        int vote = xaTerminator.prepare(xid);
                        if (vote == XAResource.XA_OK)
                            xaTerminator.rollback(xid);
                        else
                            throw new Exception("Unexpected vote: " + vote);

                        if (!transactionContext.contextSetupFailureCodes.isEmpty())
                            throw new Exception("Unexpected contextSetupFailure(s) " + transactionContext.contextSetupFailureCodes);

                        int contextSetupCompletedCount = transactionContext.contextSetupCompletedCount.get();
                        if (contextSetupCompletedCount != 1)
                            throw new Exception("contextSetupCompleted should be invoked exactly once. Instead: " + contextSetupCompletedCount);
                    }
                } catch (XAException xae) {
                    throw new Exception("error code is " + xae.errorCode, xae);
                } finally {
                    stmt.close();
                }
            } finally {
                tran.commit();
            }

            // verify entry 5 is rolled back, but 4 and 6 are committed.
            stmt = con.createStatement();
            try {
                int updateCount = stmt.executeUpdate("delete from TestXATerminatorTBL where id=5");
                if (updateCount != 0)
                    throw new Exception("Work done under inflown transaction should have been rolled back. Update count: " + updateCount);

                updateCount = stmt.executeUpdate("delete from TestXATerminatorTBL where id=4 or id=6");
                if (updateCount != 2)
                    throw new Exception("Main transaction should have been committed. Update count: " + updateCount);
            } finally {
                stmt.close();
            }
        } finally {
            con.close();
        }
    }
}