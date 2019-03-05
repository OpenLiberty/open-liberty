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
import static org.junit.Assert.assertTrue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
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
    public void testTransactionPropagationToMultipleThreadsWithExistingTransactionManagerAPI() throws Exception {
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

    @Test
    public void testTransactionPropagatedToSameThread() throws Exception {
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

        try (Connection con = defaultDataSource.getConnection(); Statement st = con.createStatement()) {
            ResultSet result = st.executeQuery("SELECT SUM(POPULATION) FROM MNCOUNTIES WHERE NAME='Winona' OR NAME='Rice' OR NAME='Fillmore'");
            assertTrue(result.next());
            assertEquals(137068, result.getInt(1));
        }
    }

    @Test
    public void testTransactionUsedSeriallyAndCommitOnDifferentThread() throws Exception {
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

    @Test
    public void testTwoThreadsConcurrentlyOperateInTransaction() throws Exception {
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
}
