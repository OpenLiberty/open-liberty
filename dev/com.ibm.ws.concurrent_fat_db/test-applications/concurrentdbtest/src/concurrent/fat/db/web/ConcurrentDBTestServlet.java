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
package concurrent.fat.db.web;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;
import javax.enterprise.concurrent.ContextService;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.enterprise.concurrent.ManagedExecutors;
import javax.enterprise.concurrent.ManagedScheduledExecutorService;
import javax.enterprise.concurrent.ManagedTask;
import javax.enterprise.concurrent.ManagedTaskListener;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.sql.DataSource;
import javax.transaction.NotSupportedException;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;

import org.junit.Test;

import componenttest.app.FATDatabaseServlet;

@SuppressWarnings("serial")
@WebServlet("/*")
public class ConcurrentDBTestServlet extends FATDatabaseServlet {

    // Maximum number of nanoseconds to wait for a task to finish.
    private static final long TIMEOUT_NS = TimeUnit.SECONDS.toNanos(30);

    @Resource
    private ContextService contextService;

    @Resource
    private DataSource dataSource;

    @Resource
    private ManagedScheduledExecutorService scheduledExecutor;

    @Resource
    private UserTransaction tran;

    @Override
    public void init() throws ServletException {
        createTable(dataSource, "MYTABLE", "MYKEY VARCHAR(80) NOT NULL PRIMARY KEY, MYVALUE INT");
    }

    /**
     * Run a task on the same thread in the middle of a global transaction. The transaction should be temporarily suspended,
     * and afterwards resumed.
     */
    @Test
    public void testGlobalTranSuspendAndResume() throws Exception {
        tran.begin();
        try {
            Connection con = dataSource.getConnection();
            Statement stmt = con.createStatement();
            stmt.executeUpdate("INSERT INTO MYTABLE VALUES ('testGlobalTranSuspendAndResume', 1)");

            // global transaction should suspend for this
            contextService.createContextualProxy(new Callable<Integer>() {
                @Override
                public Integer call() throws SQLException {
                    Connection con = dataSource.getConnection();
                    try {
                        return con.createStatement().executeUpdate("INSERT INTO MYTABLE VALUES ('testGlobalTranSuspendAndResume-2', 2)");
                    } finally {
                        con.commit();
                    }
                }
            }, Callable.class).call();

            // global transaction should be resumed
            int count = stmt.executeUpdate("UPDATE MYTABLE SET MYVALUE = 3 WHERE MYKEY = 'testGlobalTranSuspendAndResume'");
            if (count != 1)
                throw new Exception("Not able to see update from earlier in the transaction. Count: " + count);
        } finally {
            tran.rollback();
        }

        Connection con = dataSource.getConnection();
        try {
            Statement stmt = con.createStatement();
            int count = stmt.executeUpdate("DELETE FROM MYTABLE WHERE MYKEY = 'testGlobalTranSuspendAndResume-2'");
            if (count != 1)
                throw new Exception("Update made during LTC not found. Count: " + count);

            ResultSet result = stmt.executeQuery("SELECT MYVALUE FROM MYTABLE WHERE MYKEY = 'testGlobalTranSuspendAndResume'");
            if (result.next())
                throw new Exception("Should have been rolled back. Instead: " + result.getInt(1));
        } finally {
            con.close();
        }
    }

    /**
     * Schedule a task that does work in an LTC that spans multiple connections.
     */
    @Test
    public void testLTC() throws Exception {
        Future<Integer> future = scheduledExecutor.submit(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                Connection con = dataSource.getConnection();
                try {
                    con.setAutoCommit(false);
                    con.createStatement().executeUpdate("INSERT INTO MYTABLE VALUES ('testLTC', 4)");
                } finally {
                    // don't commit yet
                    con.close();
                }

                con = dataSource.getConnection();
                try {
                    int count = con.createStatement().executeUpdate("UPDATE MYTABLE SET MYVALUE = 5 where MYKEY = 'testLTC'");
                    if (count != 1)
                        throw new Exception("Not able to see first update. Count is " + count);
                } finally {
                    con.commit();
                    con.close();
                }

                con = dataSource.getConnection();
                try {
                    con.setAutoCommit(false);
                    int count = con.createStatement().executeUpdate("UPDATE MYTABLE SET MYVALUE = 6 WHERE MYKEY = 'testLTC' AND MYVALUE = 5");
                    if (count != 1)
                        throw new Exception("Not able to see second update. Count is " + count);
                } finally {
                    // don't roll back yet
                    con.close();
                }

                con = dataSource.getConnection();
                try {
                    int count = con.createStatement().executeUpdate("UPDATE MYTABLE SET MYVALUE = 7 WHERE MYKEY = 'testLTC' AND MYVALUE = 6");
                    if (count != 1)
                        throw new Exception("Not able to see third update. Count is " + count);
                } finally {
                    con.rollback();
                    con.close();
                }

                con = dataSource.getConnection();
                try {
                    con.setAutoCommit(false);
                    ResultSet result = con.createStatement().executeQuery("SELECT MYVALUE FROM MYTABLE WHERE MYKEY = 'testLTC'");
                    result.next();
                    return result.getInt(1);
                } finally {
                    con.setAutoCommit(true); // commits
                    con.close();
                }
            }
        });
        Integer result = future.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);
        if (!Integer.valueOf(5).equals(result))
            throw new Exception("Unexpected result: " + result);
    }

    /**
     * Schedule a task that does work in an LTC, commits it, and then starts a global transaction.
     */
    @Test
    public void testLTCCommitAndStartGlobalTran() throws Exception {
        Future<Integer> future = scheduledExecutor.submit(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                Connection con = dataSource.getConnection();
                try {
                    con.setAutoCommit(false);
                    ResultSet result = con.createStatement().executeQuery("VALUES (8)");
                    result.next();
                    result.close();
                } finally {
                    con.commit(); // commit in LTC
                    con.close();
                }

                tran.begin();
                try {
                    con = dataSource.getConnection();
                    try {
                        ResultSet result = con.createStatement().executeQuery("VALUES (9)");
                        result.next();
                        return result.getInt(1);
                    } finally {
                        con.close();
                    }
                } finally {
                    tran.commit();
                }
            }
        });
        Integer result = future.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);
        if (!Integer.valueOf(9).equals(result))
            throw new Exception("Unexpected result " + result);
    }

    /**
     * Have a task run in the transaction that is already on the thread of execution. Roll it back.
     */
    @Test
    public void testLTCOfExecutionThread() throws Exception {
        Connection con = dataSource.getConnection();
        try {
            con.setAutoCommit(false);
            con.createStatement().executeUpdate("INSERT INTO MYTABLE VALUES ('testLTCOfExecutionThread', 10)");
        } finally {
            // don't commit or roll back yet
            con.close();
        }

        int count = (Integer) contextService.createContextualProxy(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                Connection con = dataSource.getConnection();
                try {
                    return con.createStatement().executeUpdate("UPDATE MYTABLE SET MYVALUE = 11 where MYKEY = 'testLTCOfExecutionThread'");
                } finally {
                    // don't commit or roll back yet
                    con.close();
                }
            }
        }, Collections.singletonMap(ManagedTask.TRANSACTION, ManagedTask.USE_TRANSACTION_OF_EXECUTION_THREAD), Callable.class)
                        .call();

        if (count != 1)
            throw new Exception("Update was not visible to contextual proxy. Count: " + count);

        // roll back both updates
        con = dataSource.getConnection();
        con.rollback();
        con.setAutoCommit(true);

        try {
            ResultSet result = con.createStatement().executeQuery("SELECT MYVALUE FROM MYTABLE WHERE MYKEY = 'testLTCOfExecutionThread'");
            if (result.next())
                throw new Exception("Should have been rolled back. " + result.getInt(1));
        } finally {
            con.close();
        }
    }

    /**
     * Schedule a task that does work in an LTC, rolls it back, and then starts a global transaction.
     */
    @Test
    public void testLTCRollbackAndStartGlobalTran() throws Exception {
        Future<Integer> future = scheduledExecutor.schedule(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                Connection con = dataSource.getConnection();
                try {
                    con.setAutoCommit(false);
                    ResultSet result = con.createStatement().executeQuery("VALUES (12)");
                    result.next();
                    result.close();
                } finally {
                    con.rollback(); // roll back in LTC
                    con.close();
                }

                tran.begin();
                try {
                    con = dataSource.getConnection();
                    try {
                        ResultSet result = con.createStatement().executeQuery("VALUES (13)");
                        result.next();
                        return result.getInt(1);
                    } finally {
                        con.close();
                    }
                } finally {
                    tran.commit();
                }
            }
        }, 3, TimeUnit.MICROSECONDS);
        Integer result = future.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);
        if (!Integer.valueOf(13).equals(result))
            throw new Exception("Unexpected result " + result);
    }

    /**
     * Run a task on the same thread in the middle of an LTC. The LTC should be temporarily suspended, and afterwards resumed.
     */
    @Test
    public void testLTCSuspendAndResume() throws Exception {
        Connection con = dataSource.getConnection();
        try {
            con.setAutoCommit(false);
            con.createStatement().executeUpdate("INSERT INTO MYTABLE VALUES ('testLTCSuspendAndResume', 14)");
        } finally {
            // don't commit yet
            con.close();
        }

        contextService.createContextualProxy(new Runnable() {
            @Override
            public void run() {
                try {
                    tran.begin();
                    tran.commit();
                } catch (RuntimeException x) {
                    throw x;
                } catch (Exception x) {
                    throw new RuntimeException(x);
                }
            }
        }, Runnable.class).run();

        // LTC should be resumed
        con = dataSource.getConnection();
        try {
            int count = con.createStatement().executeUpdate("UPDATE MYTABLE SET MYVALUE = 15 where MYKEY = 'testLTCSuspendAndResume'");
            if (count != 1)
                throw new Exception("Not able to see first update. Count = " + count);
        } finally {
            con.rollback();
            con.close();
        }

        con = dataSource.getConnection();
        try {
            ResultSet result = con.createStatement().executeQuery("SELECT MYVALUE FROM MYTABLE WHERE MYKEY = 'testLTCSuspendAndResume'");
            if (result.next())
                throw new Exception("Updates should have been rolled back");
        } finally {
            con.commit();
            con.close();
        }
    }

    /**
     * Within a transaction, submit a task that performs a transactional operation during taskSubmitted.
     * Roll back the transaction and verify that the transactional operation is COMMITTED because the transaction
     * must be suspended while sending the taskSubmitted notification. Do the same for a repeating task.
     * Also, cancel a task from within a transaction and verify the taskAborted/taskDone notifications do not
     * run under that transaction by having them write data, but having the transaction roll back and verifying the
     * data is not rolled back.
     */
    @Test
    public void testTransactionSuspendedForTaskSubmittedEvent() throws Exception {
        // create a database entry upon taskSubmitted
        Runnable task = new Runnable() {
            @Override
            public void run() {}
        };
        ManagedTaskListener listener = new ManagedTaskListener() {
            @Override
            public void taskAborted(Future<?> future, ManagedExecutorService executor, Object task, Throwable failure) {}

            @Override
            public void taskDone(Future<?> future, ManagedExecutorService executor, Object task, Throwable failure) {}

            @Override
            public void taskStarting(Future<?> future, ManagedExecutorService executor, Object task) {}

            @Override
            public void taskSubmitted(Future<?> future, ManagedExecutorService executor, Object task) {
                try {
                    Connection con = dataSource.getConnection();
                    try {
                        Statement stmt = con.createStatement();
                        stmt.executeUpdate("INSERT INTO MYTABLE VALUES ('testTransactionSuspendedForTaskSubmittedEvent', 0)");
                        stmt.close();
                    } finally {
                        con.close();
                    }
                } catch (RuntimeException x) {
                    throw x;
                } catch (Exception x) {
                    throw new RuntimeException(x);
                }
            }
        };
        task = ManagedExecutors.managedTask(task,
                                            Collections.singletonMap(ManagedTask.IDENTITY_NAME, "TASK-A:testTransactionSuspendedForTaskSubmittedEvent"),
                                            listener);

        tran.begin();
        try {
            scheduledExecutor.submit(task);
        } finally {
            tran.rollback();
        }

        Connection con = dataSource.getConnection();
        try {
            ResultSet result = con.createStatement().executeQuery("SELECT MYVALUE FROM MYTABLE WHERE MYKEY = 'testTransactionSuspendedForTaskSubmittedEvent'");
            if (!result.next())
                throw new Exception("Database update from submitted task not found.");
            int value = result.getInt(1);
            if (value != 0)
                throw new Exception("Unexpected value after submitted task: " + value);
        } finally {
            con.close();
        }

        // increment a database entry upon taskSubmitted for a repeating task
        final CountDownLatch doneLatch = new CountDownLatch(3);
        listener = new ManagedTaskListener() {
            int numCompleted;

            @Override
            public void taskAborted(Future<?> future, ManagedExecutorService executor, Object task, Throwable failure) {
                try {
                    Connection con = dataSource.getConnection();
                    try {
                        Statement stmt = con.createStatement();
                        stmt.executeUpdate("UPDATE MYTABLE SET MYVALUE = MYVALUE + 100 WHERE MYKEY = 'testTransactionSuspendedForTaskSubmittedEvent'");
                        stmt.close();
                    } finally {
                        con.close();
                    }
                } catch (RuntimeException x) {
                    throw x;
                } catch (Exception x) {
                    throw new RuntimeException(x);
                }
            }

            @Override
            public void taskDone(Future<?> future, ManagedExecutorService executor, Object task, Throwable failure) {
                if (++numCompleted == 3)
                    try {
                        Connection con = dataSource.getConnection();
                        try {
                            Statement stmt = con.createStatement();
                            stmt.executeUpdate("UPDATE MYTABLE SET MYVALUE = MYVALUE + 1000 WHERE MYKEY = 'testTransactionSuspendedForTaskSubmittedEvent'");
                            stmt.close();
                        } finally {
                            con.close();
                        }
                    } catch (RuntimeException x) {
                        throw x;
                    } catch (Exception x) {
                        throw new RuntimeException(x);
                    }
                doneLatch.countDown();
            }

            @Override
            public void taskStarting(Future<?> future, ManagedExecutorService executor, Object task) {
                if (numCompleted == 2)
                    try {
                        tran.begin();
                        try {
                            future.cancel(false);
                        } finally {
                            tran.rollback();
                        }
                    } catch (NotSupportedException x) {
                        throw new RuntimeException(x);
                    } catch (SystemException x) {
                        throw new RuntimeException(x);
                    }
            }

            @Override
            public void taskSubmitted(Future<?> future, ManagedExecutorService executor, Object task) {
                try {
                    Connection con = dataSource.getConnection();
                    try {
                        Statement stmt = con.createStatement();
                        stmt.executeUpdate("UPDATE MYTABLE SET MYVALUE = MYVALUE + 1 WHERE MYKEY = 'testTransactionSuspendedForTaskSubmittedEvent'");
                        stmt.close();
                    } finally {
                        con.close();
                    }
                } catch (RuntimeException x) {
                    throw x;
                } catch (Exception x) {
                    throw new RuntimeException(x);
                }
            }
        };

        task = ManagedExecutors.managedTask(task,
                                            Collections.singletonMap(ManagedTask.IDENTITY_NAME, "TASK-B:testTransactionSuspendedForTaskSubmittedEvent"),
                                            listener);

        ScheduledFuture<?> future;
        tran.begin();
        try {
            future = scheduledExecutor.scheduleWithFixedDelay(task, 18, 18, TimeUnit.NANOSECONDS);
        } finally {
            tran.rollback();
        }

        // wait for final taskDone
        if (!doneLatch.await(TIMEOUT_NS, TimeUnit.NANOSECONDS))
            throw new Exception("Completion of third taskDone did not occur within the allotted interval. " + future);

        if (!future.isCancelled())
            throw new Exception("Task was not canceled within allotted interval. " + future);

        // check database for exact number of updates
        con = dataSource.getConnection();
        try {
            ResultSet result = con.createStatement().executeQuery("SELECT MYVALUE FROM MYTABLE WHERE MYKEY = 'testTransactionSuspendedForTaskSubmittedEvent'");
            if (!result.next())
                throw new Exception("Database update from scheduled task not found.");
            int value = result.getInt(1);
            if (value != 1103) // 3 submits + 100 to indicate abort + 1000 to indicate 3rd done notification
                throw new Exception("Unexpected value after scheduled task is canceled (which ought to only happen after it runs twice): " + value);
        } finally {
            con.close();
        }
    }

    /**
     * Run a task that begins and commits two global transactions, one after the other.
     */
    @Test
    public void testTwoGlobalTransactions() throws Exception {
        int updateCount = scheduledExecutor.schedule(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                int updateCount;

                tran.begin();
                try {
                    Connection con = dataSource.getConnection();
                    Statement stmt = con.createStatement();
                    stmt.executeUpdate("INSERT INTO MYTABLE VALUES ('testTwoGlobalTransactions', 16)");
                    stmt.close();
                    con.close();
                } finally {
                    tran.commit();
                }

                tran.begin();
                try {
                    Connection con = dataSource.getConnection();
                    Statement stmt = con.createStatement();
                    updateCount = stmt.executeUpdate("UPDATE MYTABLE SET MYVALUE = 17 WHERE MYKEY = 'testTwoGlobalTransactions'");
                    stmt.close();
                    con.close();
                } finally {
                    tran.commit();
                }

                return updateCount;
            }
        }, 16, TimeUnit.MICROSECONDS).get(TIMEOUT_NS, TimeUnit.NANOSECONDS);

        if (updateCount != 1)
            throw new Exception("Unexpected update count " + updateCount);
    }
}
