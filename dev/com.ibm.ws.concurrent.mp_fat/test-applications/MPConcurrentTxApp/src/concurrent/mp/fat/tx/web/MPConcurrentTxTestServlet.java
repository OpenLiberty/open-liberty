/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package concurrent.mp.fat.tx.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.sql.DataSource;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;

import org.eclipse.microprofile.concurrent.ManagedExecutor;
import org.eclipse.microprofile.concurrent.ThreadContext;
import org.junit.Test;

import com.ibm.tx.jta.TransactionManagerFactory;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/MPConcurrentTestServlet")
public class MPConcurrentTxTestServlet extends FATServlet {
    /**
     * 2 minutes. Maximum number of nanoseconds to wait for a task to complete.
     */
    private static final long TIMEOUT_NS = TimeUnit.MINUTES.toNanos(2);

    @Resource
    private DataSource defaultDataSource;

    @Resource(shareable = false)
    private DataSource defaultDataSource_unsharable;

    @Resource(lookup = "jdbc/ds1phase")
    private DataSource onePhaseDataSource;

    @Resource(lookup = "jdbc/ds1phase", shareable = false)
    private DataSource onePhaseDataSource_unsharable;

    // Executor that can be used when tests don't want to tie up threads from the Liberty global thread pool to perform concurrent test logic
    private ExecutorService testThreads;

    private TransactionManager tm = TransactionManagerFactory.getTransactionManager();

    @Resource
    private UserTransaction tx;

    private ThreadContext txContext = ThreadContext.builder()
                    .propagated(ThreadContext.TRANSACTION)
                    .unchanged()
                    .cleared(ThreadContext.ALL_REMAINING)
                    .build();

    private ThreadContext txContextUnchanged = ThreadContext.builder()
                    .propagated()
                    .unchanged(ThreadContext.TRANSACTION)
                    .cleared(ThreadContext.ALL_REMAINING)
                    .build();

    private ManagedExecutor txExecutor = ManagedExecutor.builder()
                    .propagated(ThreadContext.TRANSACTION)
                    .cleared(ThreadContext.ALL_REMAINING)
                    .build();

    @Override
    public void destroy() {
        testThreads.shutdownNow();
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        testThreads = Executors.newFixedThreadPool(5);

        // create tables for tests to use and pre-populate each with a single entry
        try {
            try (Connection con = defaultDataSource.getConnection(); Statement st = con.createStatement()) {
                st.execute("CREATE TABLE MNCOUNTIES (NAME VARCHAR(50) NOT NULL PRIMARY KEY, POPULATION INT NOT NULL)");
                st.execute("CREATE TABLE IACOUNTIES (NAME VARCHAR(50) NOT NULL PRIMARY KEY, POPULATION INT NOT NULL)");
                st.execute("INSERT INTO MNCOUNTIES VALUES ('Olmsted', 154930)");
                st.execute("INSERT INTO IACOUNTIES VALUES ('Polk', 481830)");
            }
        } catch (SQLException x) {
            throw new ServletException(x);
        }
    }

    /**
     * This case demonstrates that the com.ibm.tx.jta.TransactionManagerFactory public API, even without MP Concurrency,
     * already enables applications to concurrently run multiple operations within a single transaction
     * by resuming the transaction onto another thread and performing transactional operations in it
     * while it simultaneously remains actively in use on the main thread.
     */
    @Test
    public void testJTATransactionPropagationToMultipleThreadsWithExistingTransactionManagerAPI() throws Exception {
        javax.transaction.TransactionManager tm = com.ibm.tx.jta.TransactionManagerFactory.getTransactionManager();

        // scenario with successful commit
        tx.begin();
        try {
            javax.transaction.Transaction tranToPropagate = tm.getTransaction();

            Connection con = defaultDataSource.getConnection();
            Statement st = con.createStatement();
            assertEquals(1, st.executeUpdate("INSERT INTO MNCOUNTIES VALUES ('Hennepin', 1224763)"));

            CompletableFuture<Integer> stage = CompletableFuture.supplyAsync(() -> {
                try {
                    javax.transaction.Transaction tranToRestore = tm.suspend();
                    tm.resume(tranToPropagate);
                    try (Connection con2 = defaultDataSource.getConnection(); Statement st2 = con2.createStatement()) {
                        return st2.executeUpdate("INSERT INTO IACOUNTIES VALUES ('Dubuque', 96571)");
                    } finally {
                        tm.suspend();
                        if (tranToRestore != null)
                            tm.resume(tranToRestore);
                    }
                } catch (Exception x) {
                    throw new CompletionException(x);
                }
            });

            assertEquals(1, st.executeUpdate("INSERT INTO MNCOUNTIES VALUES ('Dakota', 414655)"));
            con.close();

            // ensure the above runs in the same transaction while the transaction is still active on the current thread
            assertEquals(Integer.valueOf(1), stage.join());

            tx.commit();
        } finally {
            if (tx.getStatus() != Status.STATUS_NO_TRANSACTION)
                tx.rollback();
        }

        // scenario with rollback
        tx.begin();
        try {
            javax.transaction.Transaction tranToPropagate = tm.getTransaction();

            Connection con = defaultDataSource.getConnection();
            Statement st = con.createStatement();
            assertEquals(1, st.executeUpdate("INSERT INTO MNCOUNTIES VALUES ('Ramsey', 537893)"));

            CompletableFuture<Integer> stage = CompletableFuture.supplyAsync(() -> {
                try {
                    javax.transaction.Transaction tranToRestore = tm.suspend();
                    tm.resume(tranToPropagate);
                    try (Connection con2 = defaultDataSource.getConnection(); Statement st2 = con2.createStatement()) {
                        return st2.executeUpdate("INSERT INTO IACOUNTIES VALUES ('Story', 95888)");
                    } finally {
                        tm.suspend();
                        if (tranToRestore != null)
                            tm.resume(tranToRestore);
                    }
                } catch (Exception x) {
                    throw new CompletionException(x);
                }
            });

            assertEquals(1, st.executeUpdate("INSERT INTO MNCOUNTIES VALUES ('Anoka', 344861)"));
            con.close();

            // ensure the above runs in the same transaction while the transaction is still active on the current thread
            assertEquals(Integer.valueOf(1), stage.join());
        } finally {
            tx.rollback();
        }

        // verify table contents, which should show that first transaction was committed and the second rolled back.
        try (Connection con = defaultDataSource.getConnection(); Statement st = con.createStatement()) {
            ResultSet result = st.executeQuery("SELECT SUM(POPULATION) FROM MNCOUNTIES WHERE NAME='Hennepin' OR NAME='Ramsey' OR NAME='Dakota' OR NAME='Anoka'");
            assertTrue(result.next());
            assertEquals(1639418, result.getInt(1));

            result = st.executeQuery("SELECT SUM(POPULATION) FROM IACOUNTIES WHERE NAME='Dubuque' OR NAME='Story'");
            assertTrue(result.next());
            assertEquals(96571, result.getInt(1));
        }
    }

    /**
     * Propagates transaction context to stages that are later run inline on the same thread and committed.
     */
    @Test
    public void testJTATransactionPropagatedToSameThreadAndCommit() throws Exception {
        CompletableFuture<Integer> stage0 = txExecutor.newIncompleteFuture();
        CompletableFuture<Integer> stage1, stage2, stage3;
        tx.begin();
        try {
            stage1 = stage0.thenApply(numUpdates -> {
                try (Connection con = defaultDataSource.getConnection(); Statement st = con.createStatement()) {
                    return numUpdates + st.executeUpdate("INSERT INTO MNCOUNTIES VALUES ('Winona', 50992)");
                } catch (Exception x) {
                    throw new CompletionException(x);
                }
            });
            stage2 = stage1.thenApply(numUpdates -> {
                try (Connection con = defaultDataSource.getConnection(); Statement st = con.createStatement()) {
                    return numUpdates + st.executeUpdate("INSERT INTO MNCOUNTIES VALUES ('Rice', 65251)");
                } catch (Exception x) {
                    throw new CompletionException(x);
                }
            });
            stage3 = stage2.thenApply(numUpdates -> {
                try (Connection con = defaultDataSource.getConnection(); Statement st = con.createStatement()) {
                    numUpdates += st.executeUpdate("INSERT INTO MNCOUNTIES VALUES ('Fillmore', 20825)");
                    tx.commit();
                    return numUpdates;
                } catch (Exception x) {
                    throw new CompletionException(x);
                }
            }).exceptionally(x -> {
                try {
                    tx.rollback();
                } catch (Exception x2) {
                    x2.printStackTrace();
                }
                throw new CompletionException(x);
            });
        } finally {
            tm.suspend();
        }

        assertTrue(stage0.complete(0));

        assertEquals(Integer.valueOf(3), stage3.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));

        tx.begin(); // also start a new transaction to verify the prior transaction is not left around on the main thread
        try (Connection con = defaultDataSource.getConnection(); Statement st = con.createStatement()) {
            ResultSet result = st.executeQuery("SELECT SUM(POPULATION) FROM MNCOUNTIES WHERE NAME='Winona' OR NAME='Rice' OR NAME='Fillmore'");
            assertTrue(result.next());
            assertEquals(137068, result.getInt(1));
        } finally {
            tx.commit();
        }
    }

    /**
     * Propagates transaction context to stages that are later run inline on the same thread and rolled back.
     */
    @Test
    public void testJTATransactionPropagatedToSameThreadAndRollBack() throws Exception {
        CompletableFuture<Integer> stage0 = txExecutor.newIncompleteFuture();
        CompletableFuture<Integer> stage1, stage2, stage3;
        tx.begin();
        try {
            stage1 = stage0.thenApply(numUpdates -> {
                try (Connection con = defaultDataSource.getConnection(); Statement st = con.createStatement()) {
                    return numUpdates + st.executeUpdate("INSERT INTO MNCOUNTIES VALUES ('Sherburne', 92024)");
                } catch (Exception x) {
                    throw new CompletionException(x);
                }
            });
            stage2 = stage1.thenApply(numUpdates -> {
                try (Connection con = defaultDataSource.getConnection(); Statement st = con.createStatement()) {
                    return numUpdates + st.executeUpdate("INSERT INTO MNCOUNTIES VALUES ('St. Louis', 200294)");
                } catch (Exception x) {
                    throw new CompletionException(x);
                }
            });
            stage3 = stage2.thenApply(numUpdates -> {
                try (Connection con = defaultDataSource.getConnection(); Statement st = con.createStatement()) {
                    numUpdates += st.executeUpdate("INSERT INTO MNCOUNTIES VALUES ('Stearns', 155300)");
                    if (numUpdates > 0) // always true
                        throw new SQLException("Fake error is raised to force a rollback.");
                    return numUpdates;
                } catch (Exception x) {
                    throw new CompletionException(x);
                }
            }).exceptionally(x -> {
                try {
                    tx.rollback();
                } catch (Exception x2) {
                    x2.printStackTrace();
                }
                throw new CompletionException(x);
            });
        } finally {
            tm.suspend();
        }

        assertTrue(stage0.complete(0));

        try {
            fail("Should report exception completion. Instead: " + stage3.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        } catch (ExecutionException x) {
            // expected
        }

        // Verify that the transaction rolled back, meaning none of the inserts remain in the database
        tx.begin(); // also start a new transaction to verify the prior transaction is not left around on the main thread
        try (Connection con = defaultDataSource.getConnection(); Statement st = con.createStatement()) {
            ResultSet result = st.executeQuery("SELECT SUM(POPULATION) FROM MNCOUNTIES WHERE NAME='Sherburne' OR NAME='St. Louis' OR NAME='Stearns'");
            assertTrue(result.next());
            assertEquals(0, result.getInt(1));
        } finally {
            tx.commit();
        }
    }

    /**
     * Propagates transaction context to stages that are immediately run inline on the same thread and committed.
     * The purpose of this test is to determine whether the transactions implementation will be confused by a
     * restore step that involves restoring an already-committed transaction back to the main thread. This should
     * ideally be a no-op, but it necessary to validate that it behaves that way and that it does not prevent the
     * starting of subsequent transactions.
     */
    @Test
    public void testJTATransactionPropagatedToSameThreadImmediatelyAndCommit() throws Exception {
        CompletableFuture<Integer> stage0 = txExecutor.completedFuture(0);
        CompletableFuture<Integer> stage1, stage2;
        tx.begin();
        try {
            stage1 = stage0.thenApply(numUpdates -> {
                try (Connection con = defaultDataSource.getConnection(); Statement st = con.createStatement()) {
                    return numUpdates + st.executeUpdate("INSERT INTO MNCOUNTIES VALUES ('Itasca', 45237)");
                } catch (Exception x) {
                    throw new CompletionException(x);
                }
            });
            stage2 = stage1.thenApply(numUpdates -> {
                try (Connection con = defaultDataSource.getConnection(); Statement st = con.createStatement()) {
                    numUpdates += st.executeUpdate("INSERT INTO MNCOUNTIES VALUES ('Isanti', 38584)");
                    tx.commit();
                    return numUpdates;
                } catch (Exception x) {
                    throw new CompletionException(x);
                }
            }).exceptionally(x -> {
                try {
                    tx.rollback();
                } catch (Exception x2) {
                    x2.printStackTrace();
                }
                throw new CompletionException(x);
            });
        } finally {
            tm.suspend();
        }

        assertEquals(Integer.valueOf(2), stage2.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));

        tx.begin(); // also start a new transaction to verify the prior transaction is not left around on the main thread
        try (Connection con = defaultDataSource.getConnection(); Statement st = con.createStatement()) {
            ResultSet result = st.executeQuery("SELECT SUM(POPULATION) FROM MNCOUNTIES WHERE NAME='Itasca' OR NAME='Isanti'");
            assertTrue(result.next());
            assertEquals(83821, result.getInt(1));
        } finally {
            tx.commit();
        }
    }

    /**
     * Propagates transaction context to stages that are immediately run inline on the same thread and rolled back.
     * The purpose of this test is to determine whether the transactions implementation will be confused by a
     * restore step that involves restoring an already-rolled-back transaction back to the main thread. This should
     * ideally be a no-op, but it necessary to validate that it behaves that way and that it does not prevent the
     * starting of subsequent transactions.
     */
    @Test
    public void testJTATransactionPropagatedToSameThreadImmediatelyAndRollBack() throws Exception {
        CompletableFuture<Integer> stage0 = txExecutor.completedFuture(0);
        CompletableFuture<Integer> stage1, stage2;
        tx.begin();
        try {
            stage1 = stage0.thenApply(numUpdates -> {
                try (Connection con = defaultDataSource.getConnection(); Statement st = con.createStatement()) {
                    return numUpdates + st.executeUpdate("INSERT INTO MNCOUNTIES VALUES ('Hubbard', 20743)");
                } catch (Exception x) {
                    throw new CompletionException(x);
                }
            });
            stage2 = stage1.thenApply(numUpdates -> {
                try (Connection con = defaultDataSource.getConnection(); Statement st = con.createStatement()) {
                    numUpdates += st.executeUpdate("INSERT INTO MNCOUNTIES VALUES ('Jackson', 10104)");
                    if (numUpdates > 0) // always true
                        throw new SQLException("Fake error is raised to force a rollback.");
                    return numUpdates;
                } catch (Exception x) {
                    throw new CompletionException(x);
                }
            }).exceptionally(x -> {
                try {
                    tx.rollback();
                } catch (Exception x2) {
                    x2.printStackTrace();
                }
                throw new CompletionException(x);
            });
        } finally {
            tm.suspend();
        }

        try {
            fail("Should report exceptional completion. Instead: " + stage2.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        } catch (ExecutionException x) {
            // expected
        }

        // Verify that the transaction rolled back, meaning none of the inserts remain in the database
        tx.begin(); // also start a new transaction to verify the prior transaction is not left around on the main thread
        try (Connection con = defaultDataSource.getConnection(); Statement st = con.createStatement()) {
            ResultSet result = st.executeQuery("SELECT SUM(POPULATION) FROM MNCOUNTIES WHERE NAME='Hubbard' OR NAME='Jackson'");
            assertTrue(result.next());
            assertEquals(0, result.getInt(1));
        } finally {
            tx.commit();
        }
    }

    /**
     * Propagates transaction context to be used serially from another thread, which also commits the transaction.
     */
    @Test
    public void testJTATransactionUsedSeriallyAndCommitWhenComplete() throws Exception {
        CompletableFuture<Integer> stage1 = txExecutor.newIncompleteFuture();
        CompletableFuture<Integer> stage2;
        tx.begin();
        try {
            stage2 = stage1.thenApply(numUpdates -> {
                try (Connection con = defaultDataSource.getConnection(); Statement st = con.createStatement()) {
                    return numUpdates + st.executeUpdate("INSERT INTO MNCOUNTIES VALUES ('Waseca', 18898)");
                } catch (SQLException x) {
                    throw new CompletionException(x);
                }
            }).thenApply(numUpdates -> {
                try (Connection con = defaultDataSource.getConnection(); Statement st = con.createStatement()) {
                    return numUpdates + st.executeUpdate("INSERT INTO MNCOUNTIES VALUES ('Wadena', 13626)");
                } catch (SQLException x) {
                    throw new CompletionException(x);
                }
            }).whenComplete((result, failure) -> {
                try {
                    if (failure == null && tx.getStatus() == Status.STATUS_ACTIVE)
                        tx.commit();
                    else
                        tx.rollback();
                } catch (Exception x) {
                    x.printStackTrace();
                    if (failure == null)
                        throw new CompletionException(x);
                }
            });
        } finally {
            tm.suspend();
        }

        // complete the stages on another thread
        int updateCount = testThreads.submit(() -> {
            stage1.complete(0);
            return stage2.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);
        }).get(TIMEOUT_NS, TimeUnit.NANOSECONDS);

        assertEquals(2, updateCount);

        try (Connection con = defaultDataSource.getConnection(); Statement st = con.createStatement()) {
            ResultSet result = st.executeQuery("SELECT SUM(POPULATION) FROM MNCOUNTIES WHERE NAME='Waseca' OR NAME='Wadena'");
            assertTrue(result.next());
            assertEquals(32524, result.getInt(1));
        }
    }

    /**
     * Propagates transaction context to be used serially from another thread, which also rolls back the transaction.
     */
    @Test
    public void testJTATransactionUsedSeriallyAndRollBackWhenComplete() throws Exception {
        CompletableFuture<Integer> stage1 = txExecutor.newIncompleteFuture();
        CompletableFuture<Integer> stage2;
        tx.begin();
        try {
            stage2 = stage1.thenApply(numUpdates -> {
                try (Connection con = defaultDataSource.getConnection(); Statement st = con.createStatement()) {
                    return numUpdates + st.executeUpdate("INSERT INTO MNCOUNTIES VALUES ('Washington', 250979)");
                } catch (SQLException x) {
                    throw new CompletionException(x);
                }
            }).thenApply(numUpdates -> {
                try (Connection con = defaultDataSource.getConnection(); Statement st = con.createStatement()) {
                    return numUpdates + st.executeUpdate("INSERT INTO MNCOUNTIES VALUES ('Wright', 131130)");
                } catch (SQLException x) {
                    throw new CompletionException(x);
                }
            }).thenApply(numUpdates -> {
                try {
                    tx.setRollbackOnly(); // ensure the transaction always rolls back
                    return 0;
                } catch (IllegalStateException | SystemException x) {
                    throw new CompletionException(x);
                }
            }).whenComplete((result, failure) -> {
                try {
                    if (failure == null && tx.getStatus() == Status.STATUS_ACTIVE)
                        tx.commit();
                    else
                        tx.rollback();
                } catch (Exception x) {
                    x.printStackTrace();
                    if (failure == null)
                        throw new CompletionException(x);
                }
            });
        } finally {
            tm.suspend();
        }

        // complete the stages on another thread
        int updateCount = testThreads.submit(() -> {
            stage1.complete(0);
            return stage2.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);
        }).get(TIMEOUT_NS, TimeUnit.NANOSECONDS);

        assertEquals(0, updateCount);

        // Verify that the transaction rolled back, meaning none of the inserts remain in the database
        try (Connection con = defaultDataSource.getConnection(); Statement st = con.createStatement()) {
            ResultSet result = st.executeQuery("SELECT SUM(POPULATION) FROM MNCOUNTIES WHERE NAME='Washington' OR NAME='Wright'");
            assertTrue(result.next());
            assertEquals(0, result.getInt(1));
        }
    }

    /**
     * Propagates transaction context such that transactional operations are performed serially on
     * another thread (which also does the commit). These operations can overlap the transaction
     * which remains active on the main thread for a time during which no transactional work is being
     * performed by the main thread.
     */
    @Test
    public void testJTATransactionUsedSeriallyWithOverlapAndCommitWithinLastStage() throws Exception {
        CompletableFuture<Integer> stage;
        tx.begin();
        try {
            stage = txExecutor.supplyAsync(() -> {
                try (Connection con = defaultDataSource.getConnection(); Statement st = con.createStatement()) {
                    return st.executeUpdate("INSERT INTO MNCOUNTIES VALUES ('Freeborn', 30619)");
                } catch (SQLException x) {
                    throw new CompletionException(x);
                }
            }).thenApplyAsync(numUpdates -> {
                try (Connection con = defaultDataSource.getConnection(); Statement st = con.createStatement()) {
                    numUpdates += st.executeUpdate("INSERT INTO MNCOUNTIES VALUES ('Mower', 39386)");
                    tx.commit();
                    return numUpdates;
                } catch (Exception x) {
                    throw new CompletionException(x);
                }
            }).exceptionally(x -> {
                try {
                    tx.rollback();
                } catch (Exception x2) {
                    x2.printStackTrace();
                }
                throw new CompletionException(x);
            });
        } finally {
            tm.suspend();
        }

        assertEquals(Integer.valueOf(2), stage.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));

        try (Connection con = defaultDataSource.getConnection(); Statement st = con.createStatement()) {
            ResultSet result = st.executeQuery("SELECT SUM(POPULATION) FROM MNCOUNTIES WHERE NAME='Freeborn' OR NAME='Mower'");
            assertTrue(result.next());
            assertEquals(70005, result.getInt(1));
        }
    }

    /**
     * Propagates transaction context such that transactional operations are performed serially on
     * another thread (which also does the rollback). These operations can overlap the transaction
     * which remains active on the main thread for a time during which no transactional work is being
     * performed by the main thread.
     */
    @Test
    public void testJTATransactionUsedSeriallyWithOverlapAndRollBackWithinLastStage() throws Exception {
        CompletableFuture<Integer> stage;
        tx.begin();
        try {
            stage = txExecutor.supplyAsync(() -> {
                try (Connection con = defaultDataSource.getConnection(); Statement st = con.createStatement()) {
                    return st.executeUpdate("INSERT INTO MNCOUNTIES VALUES ('Houston', 18709)");
                } catch (SQLException x) {
                    throw new CompletionException(x);
                }
            }).thenApplyAsync(numUpdates -> {
                try (Connection con = defaultDataSource.getConnection(); Statement st = con.createStatement()) {
                    numUpdates += st.executeUpdate("INSERT INTO MNCOUNTIES VALUES ('Steele', 36612)");
                    if (numUpdates > 0) // should always be true - intentionally cause rollback
                        throw new SQLException("Intentionally raising error to force a rollback.");
                    else {
                        tx.commit();
                        return numUpdates;
                    }
                } catch (Exception x) {
                    throw new CompletionException(x);
                }
            }).exceptionally(x -> {
                try {
                    tx.rollback();
                } catch (Exception x2) {
                    x2.printStackTrace();
                }
                throw new CompletionException(x);
            });
        } finally {
            tm.suspend();
        }

        try {
            fail("Should raise CompletionException. Instead: " + stage.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        } catch (ExecutionException x) {
            // expected
        }

        // Verify that the transaction rolled back, meaning none of the inserts remain in the database
        try (Connection con = defaultDataSource.getConnection(); Statement st = con.createStatement()) {
            ResultSet result = st.executeQuery("SELECT SUM(POPULATION) FROM MNCOUNTIES WHERE NAME='Houston' OR NAME='Steele'");
            assertTrue(result.next());
            assertEquals(0, result.getInt(1));
        }
    }

    /**
     * Serially use an unshared connection handle across multiple threads in a resource local transaction,
     * where transaction context is left unchanged by MicroProfile Concurrency.
     * Commit the transaction and verify the updates.
     */
    @Test
    public void testResourceLocalTransactionUnchangedWithUnsharedHandleUsedSeriallyAndCommit() throws Exception {
        Connection con = defaultDataSource_unsharable.getConnection();
        con.setAutoCommit(false);

        CompletableFuture<Integer> stage = txExecutor
                        .supplyAsync(txContextUnchanged.contextualSupplier(() -> {
                            try {
                                return con.createStatement().executeUpdate("INSERT INTO MNCOUNTIES VALUES ('Clearwater', 8824)");
                            } catch (SQLException x) {
                                throw new CompletionException(x);
                            }
                        }))
                        .thenApply(txContextUnchanged.contextualFunction(numUpdates -> {
                            try {
                                return numUpdates + con.createStatement().executeUpdate("INSERT INTO MNCOUNTIES VALUES ('Cook', 5270)");
                            } catch (SQLException x) {
                                throw new CompletionException(x);
                            }
                        }))
                        .thenApplyAsync(txContextUnchanged.contextualFunction(numUpdates -> {
                            try {
                                return numUpdates + con.createStatement().executeUpdate("INSERT INTO MNCOUNTIES VALUES ('Cottonwood', 11437)");
                            } catch (SQLException x) {
                                throw new CompletionException(x);
                            }
                        }))
                        .whenComplete((result, failure) -> {
                            try {
                                try {
                                    if (failure == null)
                                        con.commit();
                                    else
                                        con.rollback();
                                } finally {
                                    con.close();
                                }
                            } catch (SQLException x) {
                                throw new CompletionException(x);
                            }
                        });

        assertEquals(Integer.valueOf(3), stage.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));

        // verify table contents, which should show that the above transaction committed
        try (Connection c = defaultDataSource_unsharable.getConnection(); Statement st = c.createStatement()) {
            ResultSet result = st.executeQuery("SELECT SUM(POPULATION) FROM MNCOUNTIES WHERE NAME='Clearwater' OR NAME='Cook' OR NAME='Cottonwood'");
            assertTrue(result.next());
            assertEquals(25531, result.getInt(1));
        }
    }

    /**
     * Serially use an unshared connection handle across multiple threads in a resource local transaction,
     * where transaction context is left unchanged by MicroProfile Concurrency.
     * Roll back the transaction and verify that no updates were made.
     */
    @Test
    public void testResourceLocalTransactionUnchangedWithUnsharedHandleUsedSeriallyAndRollBack() throws Exception {
        Connection con = defaultDataSource_unsharable.getConnection();
        con.setAutoCommit(false);

        CompletableFuture<Integer> stage = txExecutor
                        .supplyAsync(txContextUnchanged.contextualSupplier(() -> {
                            try {
                                return con.createStatement().executeUpdate("INSERT INTO MNCOUNTIES VALUES ('Crow Wing', 63505)");
                            } catch (SQLException x) {
                                throw new CompletionException(x);
                            }
                        }))
                        .thenApply(txContextUnchanged.contextualFunction(numUpdates -> {
                            try {
                                return numUpdates + con.createStatement().executeUpdate("INSERT INTO MNCOUNTIES VALUES ('Douglas', 36891)");
                            } catch (SQLException x) {
                                throw new CompletionException(x);
                            }

                        }))
                        .thenApplyAsync(txContextUnchanged.contextualFunction(numUpdates -> {
                            try {
                                return numUpdates + con.createStatement().executeUpdate("INSERT INTO MNCOUNTIES VALUES ('Aitkin', 15841)");
                            } catch (SQLException x) {
                                throw new CompletionException(x);
                            }
                        }))
                        .thenApplyAsync(txContextUnchanged.contextualFunction(numUpdates -> {
                            try {
                                // Intentionally violate the primary key constraint in order to force an exception path,
                                // causing subsequent logic to roll back the transaction
                                return numUpdates + con.createStatement().executeUpdate("INSERT INTO MNCOUNTIES VALUES ('Aitkin', 15829)");
                            } catch (SQLException x) {
                                throw new CompletionException(x);
                            }
                        }))
                        .whenComplete((result, failure) -> {
                            try {
                                try {
                                    if (failure == null)
                                        con.commit();
                                    else
                                        con.rollback();
                                } finally {
                                    con.close();
                                }
                            } catch (SQLException x) {
                                throw new CompletionException(x);
                            }
                        });

        try {
            fail("Duplicate key should have caused failure, not result of " + stage.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        } catch (ExecutionException x) {
            // expected
        }

        // verify table contents, which should show that the above transaction rolled back
        try (Connection c = defaultDataSource_unsharable.getConnection(); Statement st = c.createStatement()) {
            ResultSet result = st.executeQuery("SELECT SUM(POPULATION) FROM MNCOUNTIES WHERE NAME='Crow Wing' OR NAME='Douglas' OR NAME='Aitkin'");
            assertTrue(result.next());
            assertEquals(0, result.getInt(1));
        }
    }

    /**
     * Serially use a sharable connection handle across multiple threads in a resource local transaction.
     * Commit the transaction and verify the updates.
     * The steps performed within this test do not actually require transaction context propagation.
     * The purpose of this test is to verify that transaction context propagation/clearing at each stage
     * does not interfere with resource local transactions.
     */
    @Test
    public void testResourceLocalTransactionWithSharableHandleUsedSeriallyAndCommit() throws Exception {
        Connection con = defaultDataSource.getConnection();
        con.setAutoCommit(false);

        CompletableFuture<Integer> stage = txExecutor
                        .supplyAsync(() -> {
                            try {
                                return con.createStatement().executeUpdate("INSERT INTO MNCOUNTIES VALUES ('Chippewa', 12040)");
                            } catch (SQLException x) {
                                throw new CompletionException(x);
                            }
                        })
                        .thenApply(numUpdates -> {
                            try {
                                return numUpdates + con.createStatement().executeUpdate("INSERT INTO MNCOUNTIES VALUES ('Chisago', 54297)");
                            } catch (SQLException x) {
                                throw new CompletionException(x);
                            }
                        })
                        .thenApplyAsync(numUpdates -> {
                            try {
                                return numUpdates + con.createStatement().executeUpdate("INSERT INTO MNCOUNTIES VALUES ('Clay', 62040)");
                            } catch (SQLException x) {
                                throw new CompletionException(x);
                            }
                        })
                        .whenComplete((result, failure) -> {
                            try {
                                try {
                                    if (failure == null)
                                        con.commit();
                                    else
                                        con.rollback();
                                } finally {
                                    con.close();
                                }
                            } catch (SQLException x) {
                                throw new CompletionException(x);
                            }
                        });

        assertEquals(Integer.valueOf(3), stage.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));

        // verify table contents, which should show that the above transaction committed
        try (Connection c = defaultDataSource.getConnection(); Statement st = c.createStatement()) {
            ResultSet result = st.executeQuery("SELECT SUM(POPULATION) FROM MNCOUNTIES WHERE NAME='Chippewa' OR NAME='Chisago' OR NAME='Clay'");
            assertTrue(result.next());
            assertEquals(128377, result.getInt(1));
        }
    }

    /**
     * Serially use a sharable connection handle across multiple threads in a resource local transaction.
     * Roll back the transaction and verify that no updates were made.
     * The steps performed within this test do not actually require transaction context propagation.
     * The purpose of this test is to verify that transaction context propagation/clearing at each stage
     * does not interfere with resource local transactions.
     */
    @Test
    public void testResourceLocalTransactionWithSharableHandleUsedSeriallyAndRollBack() throws Exception {
        Connection con = defaultDataSource.getConnection();
        con.setAutoCommit(false);

        CompletableFuture<Integer> stage = txExecutor
                        .supplyAsync(() -> {
                            try {
                                return con.createStatement().executeUpdate("INSERT INTO MNCOUNTIES VALUES ('Carlton', 35408)");
                            } catch (SQLException x) {
                                throw new CompletionException(x);
                            }
                        })
                        .thenApply(numUpdates -> {
                            try {
                                return numUpdates + con.createStatement().executeUpdate("INSERT INTO MNCOUNTIES VALUES ('Carver', 98799)");
                            } catch (SQLException x) {
                                throw new CompletionException(x);
                            }

                        })
                        .thenApplyAsync(numUpdates -> {
                            try {
                                return numUpdates + con.createStatement().executeUpdate("INSERT INTO MNCOUNTIES VALUES ('Cass', 28810)");
                            } catch (SQLException x) {
                                throw new CompletionException(x);
                            }
                        })
                        .thenApplyAsync(numUpdates -> {
                            try {
                                // Intentionally violate the primary key constraint in order to force an exception path,
                                // causing subsequent logic to roll back the transaction
                                return numUpdates + con.createStatement().executeUpdate("INSERT INTO MNCOUNTIES VALUES ('Cass', 29355)");
                            } catch (SQLException x) {
                                throw new CompletionException(x);
                            }
                        })
                        .whenComplete((result, failure) -> {
                            try {
                                try {
                                    if (failure == null)
                                        con.commit();
                                    else
                                        con.rollback();
                                } finally {
                                    con.close();
                                }
                            } catch (SQLException x) {
                                throw new CompletionException(x);
                            }
                        });

        try {
            fail("Duplicate key should have caused failure, not result of " + stage.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        } catch (ExecutionException x) {
            // expected
        }

        // verify table contents, which should show that the above transaction rolled back
        try (Connection c = defaultDataSource.getConnection(); Statement st = c.createStatement()) {
            ResultSet result = st.executeQuery("SELECT SUM(POPULATION) FROM MNCOUNTIES WHERE NAME='Carlton' OR NAME='Carver' OR NAME='Cass'");
            assertTrue(result.next());
            assertEquals(0, result.getInt(1));
        }
    }

    /**
     * Serially use an unshared connection handle across multiple threads in a resource local transaction.
     * Commit the transaction and verify the updates.
     * The steps performed within this test do not actually require transaction context propagation.
     * The purpose of this test is to verify that transaction context propagation/clearing at each stage
     * does not interfere with resource local transactions.
     */
    // TODO Unshared connections ARE impacted by the usage of different LTCs for the various stages.
    // When clearing the LTC after the first stage:
    // WLTC0033W: Resource dataSource[DefaultDataSource]/connectionManager rolled back in cleanup of LocalTransactionContainment.
    // WLTC0032W: One or more local transaction resources were rolled back during the cleanup of a LocalTransactionContainment.
    // FFDC1015I: An FFDC Incident has been created: "com.ibm.ws.LocalTransaction.RolledbackException: Resources rolled back due to unresolved action of rollback. com.ibm.ws.transaction.context.internal.TransactionContextImpl 116" at ffdc_19.03.11_09.50.58.0.log
    //@Test
    public void testResourceLocalTransactionWithUnsharedHandleUsedSeriallyAndCommit() throws Exception {
        Connection con = defaultDataSource_unsharable.getConnection();
        con.setAutoCommit(false);

        CompletableFuture<Integer> stage = txExecutor
                        .supplyAsync(() -> {
                            try {
                                return con.createStatement().executeUpdate("INSERT INTO MNCOUNTIES VALUES ('Big Stone', 5039)");
                            } catch (SQLException x) {
                                throw new CompletionException(x);
                            }
                        })
                        .thenApply(numUpdates -> {
                            try {
                                return numUpdates + con.createStatement().executeUpdate("INSERT INTO MNCOUNTIES VALUES ('Blue Earth', 65767)");
                            } catch (SQLException x) {
                                throw new CompletionException(x);
                            }
                        })
                        .thenApplyAsync(numUpdates -> {
                            try {
                                return numUpdates + con.createStatement().executeUpdate("INSERT INTO MNCOUNTIES VALUES ('Brown', 25243)");
                            } catch (SQLException x) {
                                throw new CompletionException(x);
                            }
                        })
                        .whenComplete((result, failure) -> {
                            try {
                                try {
                                    if (failure == null)
                                        con.commit();
                                    else
                                        con.rollback();
                                } finally {
                                    con.close();
                                }
                            } catch (SQLException x) {
                                throw new CompletionException(x);
                            }
                        });

        assertEquals(Integer.valueOf(3), stage.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));

        // verify table contents, which should show that the above transaction committed
        try (Connection c = defaultDataSource_unsharable.getConnection(); Statement st = c.createStatement()) {
            ResultSet result = st.executeQuery("SELECT SUM(POPULATION) FROM MNCOUNTIES WHERE NAME='Big Stone' OR NAME='Blue Earth' OR NAME='Brown'");
            assertTrue(result.next());
            assertEquals(96049, result.getInt(1));
        }
    }

    /**
     * Serially use an unshared connection handle across multiple threads in a resource local transaction.
     * Roll back the transaction and verify that no updates were made.
     * The steps performed within this test do not actually require transaction context propagation.
     * The purpose of this test is to verify that transaction context propagation/clearing at each stage
     * does not interfere with resource local transactions.
     */
    // TODO Unshared connections ARE impacted by the usage of different LTCs for the various stages. See comment on previous test.
    // @Test
    public void testResourceLocalTransactionWithUnsharedHandleUsedSeriallyAndRollBack() throws Exception {
        Connection con = defaultDataSource_unsharable.getConnection();
        con.setAutoCommit(false);

        CompletableFuture<Integer> stage = txExecutor
                        .supplyAsync(() -> {
                            try {
                                return con.createStatement().executeUpdate("INSERT INTO MNCOUNTIES VALUES ('Becker', 33552)");
                            } catch (SQLException x) {
                                throw new CompletionException(x);
                            }
                        })
                        .thenApply(numUpdates -> {
                            try {
                                return numUpdates + con.createStatement().executeUpdate("INSERT INTO MNCOUNTIES VALUES ('Beltrami', 45847)");
                            } catch (SQLException x) {
                                throw new CompletionException(x);
                            }

                        })
                        .thenApplyAsync(numUpdates -> {
                            try {
                                return numUpdates + con.createStatement().executeUpdate("INSERT INTO MNCOUNTIES VALUES ('Benton', 39360)");
                            } catch (SQLException x) {
                                throw new CompletionException(x);
                            }
                        })
                        .thenApplyAsync(numUpdates -> {
                            try {
                                // Intentionally violate the primary key constraint in order to force an exception path,
                                // causing subsequent logic to roll back the transaction
                                return numUpdates + con.createStatement().executeUpdate("INSERT INTO MNCOUNTIES VALUES ('Beltrami', 46513)");
                            } catch (SQLException x) {
                                throw new CompletionException(x);
                            }
                        })
                        .whenComplete((result, failure) -> {
                            try {
                                try {
                                    if (failure == null)
                                        con.commit();
                                    else
                                        con.rollback();
                                } finally {
                                    con.close();
                                }
                            } catch (SQLException x) {
                                throw new CompletionException(x);
                            }
                        });

        try {
            fail("Duplicate key should have caused failure, not result of " + stage.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        } catch (ExecutionException x) {
            // expected
        }

        // verify table contents, which should show that the above transaction rolled back
        try (Connection c = defaultDataSource_unsharable.getConnection(); Statement st = c.createStatement()) {
            ResultSet result = st.executeQuery("SELECT SUM(POPULATION) FROM MNCOUNTIES WHERE NAME='Becker' OR NAME='Beltrami' OR NAME='Benton'");
            assertTrue(result.next());
            assertEquals(0, result.getInt(1));
        }
    }

    /**
     * Propagates transaction context to be used serially from another thread, which also commits the transaction.
     * The resource that is enlisted in the transaction is a sharable one-phase only resource.
     * A single connection handle is cached and reused across all stages where transactional work is performed.
     */
    @Test
    public void testSharableOnePhaseResourceInJTATransactionCachedHandleUsedSeriallyAndCommitWhenComplete() throws Exception {
        CompletableFuture<DataSource> getDataSource = txExecutor.newIncompleteFuture();
        CompletableFuture<Integer> stage;
        tx.begin();
        try {
            stage = getDataSource.thenApplyAsync(ds -> {
                try {
                    Connection con = ds.getConnection();
                    con.createStatement().executeUpdate("INSERT INTO MNCOUNTIES VALUES ('Mille Lacs', 25635)");
                    return con;
                } catch (SQLException x) {
                    throw new CompletionException(x);
                }
            }).thenApplyAsync(con -> {
                try {
                    con.createStatement().executeUpdate("INSERT INTO MNCOUNTIES VALUES ('Morrison', 32880)");
                    return con;
                } catch (SQLException x) {
                    throw new CompletionException(x);
                }
            }).thenApplyAsync(con -> {
                try {
                    return con.createStatement().executeUpdate("INSERT INTO MNCOUNTIES VALUES ('Murray', 8394)");
                } catch (SQLException x) {
                    throw new CompletionException(x);
                }
            }).whenCompleteAsync((result, failure) -> {
                try {
                    if (failure == null && tx.getStatus() == Status.STATUS_ACTIVE)
                        tx.commit();
                    else
                        tx.rollback();
                } catch (Exception x) {
                    x.printStackTrace();
                    if (failure == null)
                        throw new CompletionException(x);
                }
            });
        } finally {
            tm.suspend();
        }

        getDataSource.complete(onePhaseDataSource);

        assertEquals(Integer.valueOf(1), stage.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));

        try (Connection con = onePhaseDataSource.getConnection(); Statement st = con.createStatement()) {
            ResultSet result = st.executeQuery("SELECT SUM(POPULATION) FROM MNCOUNTIES WHERE NAME='Mille Lacs' OR NAME='Morrison' OR NAME='Murray'");
            assertTrue(result.next());
            assertEquals(66909, result.getInt(1));
        }
    }

    /**
     * Propagates transaction context to be used serially from another thread, which also rolls back the transaction.
     * The resource that is enlisted in the transaction is a sharable one-phase only resource.
     * A single connection handle is cached and reused across all stages where transactional work is performed.
     */
    @Test
    public void testSharableResourceInOnePhaseJTATransactionCachedHandleUsedSeriallyAndRollBackWhenComplete() throws Exception {
        CompletableFuture<DataSource> getDataSource = txExecutor.newIncompleteFuture();
        CompletableFuture<Integer> stage;
        tx.begin();
        try {
            stage = getDataSource.thenApplyAsync(ds -> {
                try {
                    Connection con = ds.getConnection();
                    con.createStatement().executeUpdate("INSERT INTO MNCOUNTIES VALUES ('Nicollet', 33477)");
                    return con;
                } catch (SQLException x) {
                    throw new CompletionException(x);
                }
            }).thenApplyAsync(con -> {
                try {
                    con.createStatement().executeUpdate("INSERT INTO MNCOUNTIES VALUES ('Nobles', 21854)");
                    return con;
                } catch (SQLException x) {
                    throw new CompletionException(x);
                }
            }).thenApplyAsync(con -> {
                try {
                    return con.createStatement().executeUpdate("INSERT INTO MNCOUNTIES VALUES ('Norman', 6589)");
                } catch (SQLException x) {
                    throw new CompletionException(x);
                }
            }).thenApplyAsync(result -> {
                try {
                    tx.setRollbackOnly(); // ensure the transaction always rolls back
                    return 0;
                } catch (IllegalStateException | SystemException x) {
                    throw new CompletionException(x);
                }
            }).whenCompleteAsync((result, failure) -> {
                try {
                    if (failure == null && tx.getStatus() == Status.STATUS_ACTIVE) {
                        tx.commit();
                    } else
                        tx.rollback();
                } catch (Exception x) {
                    x.printStackTrace();
                    if (failure == null)
                        throw new CompletionException(x);
                }
            });
        } finally {
            tm.suspend();
        }

        getDataSource.complete(onePhaseDataSource);

        assertEquals(Integer.valueOf(0), stage.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));

        // Verify that the transaction rolled back, meaning none of the inserts remain in the database
        try (Connection con = onePhaseDataSource.getConnection(); Statement st = con.createStatement()) {
            ResultSet result = st.executeQuery("SELECT SUM(POPULATION) FROM MNCOUNTIES WHERE NAME='Nicollet' OR NAME='Nobles' OR NAME='Norman'");
            assertTrue(result.next());
            assertEquals(0, result.getInt(1));
        }
    }

    /**
     * Propagates transaction context to be used serially from another thread, which also commits the transaction.
     * The resource that is enlisted in the transaction is a sharable one-phase only resource.
     */
    @Test
    public void testSharableOnePhaseResourceInJTATransactionUsedSeriallyAndCommitWhenComplete() throws Exception {
        CompletableFuture<Integer> stage1 = txExecutor.newIncompleteFuture();
        CompletableFuture<Integer> stage2;
        tx.begin();
        try {
            stage2 = stage1.thenApply(numUpdates -> {
                try (Connection con = onePhaseDataSource.getConnection(); Statement st = con.createStatement()) {
                    return numUpdates + st.executeUpdate("INSERT INTO MNCOUNTIES VALUES ('Kanabec', 15948)");
                } catch (SQLException x) {
                    throw new CompletionException(x);
                }
            }).thenApplyAsync(numUpdates -> {
                try (Connection con = onePhaseDataSource.getConnection(); Statement st = con.createStatement()) {
                    return numUpdates + st.executeUpdate("INSERT INTO MNCOUNTIES VALUES ('Kandiyohi', 42577)");
                } catch (SQLException x) {
                    throw new CompletionException(x);
                }
            }).whenComplete((result, failure) -> {
                try {
                    if (failure == null && tx.getStatus() == Status.STATUS_ACTIVE)
                        tx.commit();
                    else
                        tx.rollback();
                } catch (Exception x) {
                    x.printStackTrace();
                    if (failure == null)
                        throw new CompletionException(x);
                }
            });
        } finally {
            tm.suspend();
        }

        // complete the stages on other threads
        int updateCount = testThreads.submit(() -> {
            stage1.complete(0);
            return stage2.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);
        }).get(TIMEOUT_NS, TimeUnit.NANOSECONDS);

        assertEquals(2, updateCount);

        try (Connection con = onePhaseDataSource.getConnection(); Statement st = con.createStatement()) {
            ResultSet result = st.executeQuery("SELECT SUM(POPULATION) FROM MNCOUNTIES WHERE NAME='Kanabec' OR NAME='Kandiyohi'");
            assertTrue(result.next());
            assertEquals(58525, result.getInt(1));
        }
    }

    /**
     * Propagates transaction context to be used serially from another thread, which also rolls back the transaction.
     * The resource that is enlisted in the transaction is a sharable one-phase only resource.
     */
    @Test
    public void testSharableResourceInOnePhaseJTATransactionUsedSeriallyAndRollBackWhenComplete() throws Exception {
        CompletableFuture<Integer> stage1 = txExecutor.newIncompleteFuture();
        CompletableFuture<Integer> stage2;
        tx.begin();
        try {
            stage2 = stage1.thenApply(numUpdates -> {
                try (Connection con = onePhaseDataSource.getConnection(); Statement st = con.createStatement()) {
                    return numUpdates + st.executeUpdate("INSERT INTO MNCOUNTIES VALUES ('Kittson', 4384)");
                } catch (SQLException x) {
                    throw new CompletionException(x);
                }
            }).thenApplyAsync(numUpdates -> {
                try (Connection con = onePhaseDataSource.getConnection(); Statement st = con.createStatement()) {
                    return numUpdates + st.executeUpdate("INSERT INTO MNCOUNTIES VALUES ('Lac qui Parle', 6840)");
                } catch (SQLException x) {
                    throw new CompletionException(x);
                }
            }).thenApply(numUpdates -> {
                try {
                    tx.setRollbackOnly(); // ensure the transaction always rolls back
                    return 0;
                } catch (IllegalStateException | SystemException x) {
                    throw new CompletionException(x);
                }
            }).whenComplete((result, failure) -> {
                try {
                    if (failure == null && tx.getStatus() == Status.STATUS_ACTIVE)
                        tx.commit();
                    else
                        tx.rollback();
                } catch (Exception x) {
                    x.printStackTrace();
                    if (failure == null)
                        throw new CompletionException(x);
                }
            });
        } finally {
            tm.suspend();
        }

        // complete the stages on other threads
        int updateCount = testThreads.submit(() -> {
            stage1.complete(0);
            return stage2.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);
        }).get(TIMEOUT_NS, TimeUnit.NANOSECONDS);

        assertEquals(0, updateCount);

        // Verify that the transaction rolled back, meaning none of the inserts remain in the database
        try (Connection con = onePhaseDataSource.getConnection(); Statement st = con.createStatement()) {
            ResultSet result = st.executeQuery("SELECT SUM(POPULATION) FROM MNCOUNTIES WHERE NAME='Kittson' OR NAME='Lac qui Parle'");
            assertTrue(result.next());
            assertEquals(0, result.getInt(1));
        }
    }

    /**
     * Have two threads perform transactional operations within the same thread, which can run
     * at the same time. The main thread commits the transaction when the transactional operations finish.
     */
    @Test
    public void testTwoThreadsConcurrentlyOperateInJTATransactionAndCommit() throws Exception {
        tx.begin();
        try (Connection con = defaultDataSource.getConnection()) {
            PreparedStatement ps = con.prepareStatement("INSERT INTO MNCOUNTIES VALUES (?,?)");
            ps.setString(1, "Goodhue");
            ps.setInt(2, 46138);
            ps.executeUpdate();

            CompletableFuture<Integer> stage = txExecutor.supplyAsync(() -> {
                try (Connection con2 = defaultDataSource.getConnection()) {
                    assertEquals(Status.STATUS_ACTIVE, tx.getStatus());
                    return con2.createStatement().executeUpdate("INSERT INTO IACOUNTIES VALUES ('Linn', 220008)");
                } catch (SQLException | SystemException x) {
                    throw new CompletionException(x);
                }
            });

            ps.setString(1, "Wabasha");
            ps.setInt(2, 21490);
            ps.executeUpdate();

            assertEquals(Integer.valueOf(1), stage.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        } finally {
            tx.commit();
        }

        try (Connection con = defaultDataSource.getConnection(); Statement st = con.createStatement()) {
            ResultSet result = st.executeQuery("SELECT SUM(POPULATION) FROM MNCOUNTIES WHERE NAME='Goodhue' OR NAME='Wabasha'");
            assertTrue(result.next());
            assertEquals(67628, result.getInt(1));
            result.close();
            result = st.executeQuery("SELECT POPULATION FROM IACOUNTIES WHERE NAME='Linn'");
            assertTrue(result.next());
            assertEquals(220008, result.getInt(1));
        }
    }

    /**
     * Have two threads perform transactional operations within the same thread, which can run
     * at the same time. The main thread rolls back the transaction when the transactional operations finish.
     */
    @Test
    public void testTwoThreadsConcurrentlyOperateInJTATransactionAndRollBack() throws Exception {
        tx.begin();
        try (Connection con = defaultDataSource.getConnection()) {
            PreparedStatement ps = con.prepareStatement("INSERT INTO MNCOUNTIES VALUES (?,?)");
            ps.setString(1, "Le Sueur");
            ps.setInt(2, 27810);
            ps.executeUpdate();

            CompletableFuture<Integer> stage = txExecutor.supplyAsync(() -> {
                try (Connection con2 = defaultDataSource.getConnection()) {
                    assertEquals(Status.STATUS_ACTIVE, tx.getStatus());
                    return con2.createStatement().executeUpdate("INSERT INTO IACOUNTIES VALUES ('Allamakee', 13940)");
                } catch (SQLException | SystemException x) {
                    throw new CompletionException(x);
                }
            });

            ps.setString(1, "Faribault");
            ps.setInt(2, 13966);
            ps.executeUpdate();

            assertEquals(Integer.valueOf(1), stage.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        } finally {
            tx.rollback();
        }

        // Verify that the transaction rolled back, meaning none of the inserts remain in the database
        try (Connection con = defaultDataSource.getConnection(); Statement st = con.createStatement()) {
            ResultSet result = st.executeQuery("SELECT POPULATION FROM MNCOUNTIES WHERE NAME='Le Sueur' OR NAME='Faribault'");
            assertFalse(result.next());
            result.close();
            result = st.executeQuery("SELECT POPULATION FROM IACOUNTIES WHERE NAME='Allamakee'");
            assertFalse(result.next());
        }
    }

    /**
     * Propagates transaction context to be used serially from another thread, which also commits the transaction.
     * The resource that is enlisted in the transaction is an unsharable one-phase only resource.
     * A single connection handle is cached and reused across all stages where transactional work is performed.
     */
    @Test
    public void testUnsharableOnePhaseResourceInJTATransactionCachedHandleUsedSeriallyAndCommitWhenComplete() throws Exception {
        CompletableFuture<DataSource> getDataSource = txExecutor.newIncompleteFuture();
        CompletableFuture<Boolean> stage;
        tx.begin();
        try {
            stage = getDataSource.thenApplyAsync(ds -> {
                Connection con = null;
                try {
                    con = ds.getConnection();
                    con.createStatement().executeUpdate("INSERT INTO MNCOUNTIES VALUES ('Mahnomen', 5500)");
                    return con;
                } catch (SQLException x) {
                    if (con != null)
                        try {
                            con.close();
                        } catch (SQLException cx) {
                            cx.printStackTrace();
                        }
                    throw new CompletionException(x);
                }
            }).whenCompleteAsync((con, failure) -> {
                if (failure == null)
                    try {
                        con.createStatement().executeUpdate("INSERT INTO MNCOUNTIES VALUES ('Marshall', 9397)");
                    } catch (SQLException x) {
                        throw new CompletionException(x);
                    }
            }).whenCompleteAsync((con, failure) -> {
                if (failure == null)
                    try {
                        con.createStatement().executeUpdate("INSERT INTO MNCOUNTIES VALUES ('Martin', 20084)");
                    } catch (SQLException x) {
                        throw new CompletionException(x);
                    }
            }).handleAsync((con, failure) -> {
                if (con != null)
                    try {
                        con.close();
                    } catch (SQLException x) {
                        if (failure == null)
                            failure = x;
                    }
                try {
                    if (failure == null && tx.getStatus() == Status.STATUS_ACTIVE) {
                        tx.commit();
                        return true;
                    } else
                        tx.rollback();
                } catch (Exception x) {
                    if (failure == null)
                        failure = x;
                }
                if (failure == null)
                    return false;
                else
                    throw new CompletionException(failure);
            });
        } finally {
            tm.suspend();
        }

        getDataSource.complete(onePhaseDataSource_unsharable);

        assertEquals(Boolean.TRUE, stage.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));

        try (Connection con = onePhaseDataSource_unsharable.getConnection(); Statement st = con.createStatement()) {
            ResultSet result = st.executeQuery("SELECT SUM(POPULATION) FROM MNCOUNTIES WHERE NAME='Mahnomen' OR NAME='Marshall' OR NAME='Martin'");
            assertTrue(result.next());
            assertEquals(34981, result.getInt(1));
        }
    }

    /**
     * Propagates transaction context to be used serially from another thread, which also rolls back the transaction.
     * The resource that is enlisted in the transaction is an unsharable one-phase only resource.
     * A single connection handle is cached and reused across all stages where transactional work is performed.
     */
    @Test
    public void testUnsharableResourceInOnePhaseJTATransactionCachedHandleUsedSeriallyAndRollBackWhenComplete() throws Exception {
        CompletableFuture<DataSource> getDataSource = txExecutor.newIncompleteFuture();
        CompletableFuture<Boolean> stage;
        tx.begin();
        try {
            stage = getDataSource.thenApplyAsync(ds -> {
                Connection con = null;
                try {
                    con = ds.getConnection();
                    con.createStatement().executeUpdate("INSERT INTO MNCOUNTIES VALUES ('Meeker', 23105)");
                    return con;
                } catch (SQLException x) {
                    if (con != null)
                        try {
                            con.close();
                        } catch (SQLException cx) {
                            cx.printStackTrace();
                        }
                    throw new CompletionException(x);
                }
            }).whenCompleteAsync((con, failure) -> {
                if (failure == null)
                    try {
                        con.createStatement().executeUpdate("INSERT INTO MNCOUNTIES VALUES ('McLeod', 35816)");
                    } catch (SQLException x) {
                        throw new CompletionException(x);
                    }
            }).whenComplete((con, failure) -> {
                try {
                    tx.setRollbackOnly(); // ensure the transaction always rolls back
                } catch (IllegalStateException | SystemException x) {
                    throw new CompletionException(x);
                }
            }).handleAsync((con, failure) -> {
                if (con != null)
                    try {
                        con.close();
                    } catch (SQLException x) {
                        if (failure == null)
                            failure = x;
                    }
                try {
                    if (failure == null && tx.getStatus() == Status.STATUS_ACTIVE) {
                        tx.commit();
                        return true;
                    } else
                        tx.rollback();
                } catch (Exception x) {
                    if (failure == null)
                        failure = x;
                }
                if (failure == null)
                    return false;
                else
                    throw new CompletionException(failure);
            });
        } finally {
            tm.suspend();
        }

        getDataSource.complete(onePhaseDataSource_unsharable);

        assertEquals(Boolean.FALSE, stage.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));

        // Verify that the transaction rolled back, meaning none of the inserts remain in the database
        try (Connection con = onePhaseDataSource_unsharable.getConnection(); Statement st = con.createStatement()) {
            ResultSet result = st.executeQuery("SELECT SUM(POPULATION) FROM MNCOUNTIES WHERE NAME='Meeker' OR NAME='McLeod'");
            assertTrue(result.next());
            assertEquals(0, result.getInt(1));
        }
    }

    /**
     * Propagates transaction context to be used serially from another thread, which also commits the transaction.
     * The resource that is enlisted in the transaction is an unsharable one-phase only resource.
     */
    // @Test TODO Encounters an expected error for unsharable one-phase resources: Illegal attempt to enlist multiple 1PC XAResources
    public void testUnsharableOnePhaseResourceInJTATransactionUsedSeriallyAndCommitWhenComplete() throws Exception {
        CompletableFuture<Integer> stage1 = txExecutor.newIncompleteFuture();
        CompletableFuture<Integer> stage2;
        tx.begin();
        try {
            stage2 = stage1.thenApply(numUpdates -> {
                try (Connection con = onePhaseDataSource_unsharable.getConnection(); Statement st = con.createStatement()) {
                    return numUpdates + st.executeUpdate("INSERT INTO MNCOUNTIES VALUES ('Lake', 10578)");
                } catch (SQLException x) {
                    throw new CompletionException(x);
                }
            }).thenApplyAsync(numUpdates -> {
                try (Connection con = onePhaseDataSource_unsharable.getConnection(); Statement st = con.createStatement()) {
                    return numUpdates + st.executeUpdate("INSERT INTO MNCOUNTIES VALUES ('Lake of the Woods', 3841)");
                } catch (SQLException x) {
                    throw new CompletionException(x);
                }
            }).whenComplete((result, failure) -> {
                try {
                    if (failure == null && tx.getStatus() == Status.STATUS_ACTIVE)
                        tx.commit();
                    else
                        tx.rollback();
                } catch (Exception x) {
                    x.printStackTrace();
                    if (failure == null)
                        throw new CompletionException(x);
                }
            });
        } finally {
            tm.suspend();
        }

        // complete the stages on other threads
        int updateCount = testThreads.submit(() -> {
            stage1.complete(0);
            return stage2.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);
        }).get(TIMEOUT_NS, TimeUnit.NANOSECONDS);

        assertEquals(2, updateCount);

        try (Connection con = onePhaseDataSource_unsharable.getConnection(); Statement st = con.createStatement()) {
            ResultSet result = st.executeQuery("SELECT SUM(POPULATION) FROM MNCOUNTIES WHERE NAME='Lake' OR NAME='Lake of the Woods'");
            assertTrue(result.next());
            assertEquals(14419, result.getInt(1));
        }
    }

    /**
     * Propagates transaction context to be used serially from another thread, which also rolls back the transaction.
     * The resource that is enlisted in the transaction is an unsharable one-phase only resource.
     */
    // @Test TODO Encounters an expected error for unsharable one-phase resources: Illegal attempt to enlist multiple 1PC XAResources
    public void testUnsharableResourceInOnePhaseJTATransactionUsedSeriallyAndRollBackWhenComplete() throws Exception {
        CompletableFuture<Integer> stage1 = txExecutor.newIncompleteFuture();
        CompletableFuture<Integer> stage2;
        tx.begin();
        try {
            stage2 = stage1.thenApply(numUpdates -> {
                try (Connection con = onePhaseDataSource_unsharable.getConnection(); Statement st = con.createStatement()) {
                    return numUpdates + st.executeUpdate("INSERT INTO MNCOUNTIES VALUES ('Lincoln', 5724)");
                } catch (SQLException x) {
                    throw new CompletionException(x);
                }
            }).thenApplyAsync(numUpdates -> {
                try (Connection con = onePhaseDataSource_unsharable.getConnection(); Statement st = con.createStatement()) {
                    return numUpdates + st.executeUpdate("INSERT INTO MNCOUNTIES VALUES ('Lyon', 25789)");
                } catch (SQLException x) {
                    throw new CompletionException(x);
                }
            }).thenApply(numUpdates -> {
                try {
                    tx.setRollbackOnly(); // ensure the transaction always rolls back
                    return 0;
                } catch (IllegalStateException | SystemException x) {
                    throw new CompletionException(x);
                }
            }).whenComplete((result, failure) -> {
                try {
                    if (failure == null && tx.getStatus() == Status.STATUS_ACTIVE)
                        tx.commit();
                    else
                        tx.rollback();
                } catch (Exception x) {
                    x.printStackTrace();
                    if (failure == null)
                        throw new CompletionException(x);
                }
            });
        } finally {
            tm.suspend();
        }

        // complete the stages on other threads
        int updateCount = testThreads.submit(() -> {
            stage1.complete(0);
            return stage2.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);
        }).get(TIMEOUT_NS, TimeUnit.NANOSECONDS);

        assertEquals(0, updateCount);

        // Verify that the transaction rolled back, meaning none of the inserts remain in the database
        try (Connection con = onePhaseDataSource_unsharable.getConnection(); Statement st = con.createStatement()) {
            ResultSet result = st.executeQuery("SELECT SUM(POPULATION) FROM MNCOUNTIES WHERE NAME='Lincoln' OR NAME='Lyon'");
            assertTrue(result.next());
            assertEquals(0, result.getInt(1));
        }
    }
}
