/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package jdbc.fat.v43.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.sql.DataSource;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;

import org.junit.Test;

import com.ibm.tx.jta.TransactionManagerFactory;

import componenttest.annotation.ExpectedFFDC;
import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/JDBC43TestServlet")
public class JDBC43TestServlet extends FATServlet {
    /**
     * Array indices for beginRequest/endRequest tracking.
     */
    private static final int BEGIN = 0, END = 1;

    @Resource
    DataSource defaultDataSource;

    @Resource(lookup = "jdbc/poolOf1", shareable = false)
    DataSource unsharablePool1DataSource;

    @Resource(lookup = "jdbc/xa", shareable = false)
    DataSource unsharableXADataSource;

    @Resource
    UserTransaction tx;

    private final static TransactionManager txm = TransactionManagerFactory.getTransactionManager();

    // create a table for tests to use and pre-populate it with some data
    @Override
    public void init() throws ServletException {
        try {
            Connection con = defaultDataSource.getConnection();
            try {
                Statement stmt = con.createStatement();
                stmt.execute("CREATE TABLE STREETS (NAME VARCHAR(50), CITY VARCHAR(50), STATE CHAR(2), PRIMARY KEY (NAME, CITY, STATE))");
                PreparedStatement ps = con.prepareStatement("INSERT INTO STREETS VALUES(?, ?, ?)");
                ps.setString(2, "Rochester");
                ps.setString(3, "MN");
                ps.setString(1, "Civic Center Drive NW");
                ps.executeUpdate();
                ps.setString(1, "East Circle Drive");
                ps.executeUpdate();
                ps.setString(1, "West Circle Drive");
                ps.executeUpdate();
                ps.setString(1, "Valleyhigh Drive NW");
                ps.executeUpdate();
            } finally {
                con.close();
            }
        } catch (SQLException x) {
            throw new ServletException(x);
        }
    }

    /**
     * Verify that that DatabaseMetaData indicates spec version 4.3.
     */
    @Test
    public void testMetaData() throws Exception {
        Connection con = defaultDataSource.getConnection();
        try {
            DatabaseMetaData mdata = con.getMetaData();

            assertEquals(4, mdata.getJDBCMajorVersion());
            assertEquals(3, mdata.getJDBCMinorVersion());
        } finally {
            con.close();
        }
    }

    /**
     * Verify that within a global transaction, a single request is made that covers all shared handles.
     * In this test, the shared handles are open at the same time.
     */
    @Test
    public void testMultipleOpenSharableHandlesInTransaction() throws Exception {
        AtomicInteger[] requests;
        int begins = -1000, ends = -1000;

        tx.begin();
        try {
            Connection con1 = defaultDataSource.getConnection();
            requests = (AtomicInteger[]) con1.unwrap(Supplier.class).get();
            begins = requests[BEGIN].get();
            ends = requests[END].get();
            assertEquals(ends + 1, begins);

            PreparedStatement ps1 = con1.prepareStatement("INSERT INTO STREETS VALUES(?, ?, ?)");
            ps1.setString(1, "North Broadway Avenue");
            ps1.setString(2, "Rochester");
            ps1.setString(3, "MN");
            ps1.executeUpdate();
            ps1.close();

            Connection con2 = defaultDataSource.getConnection();
            PreparedStatement ps2 = con2.prepareStatement("INSERT INTO STREETS VALUES(?, ?, ?)");
            ps2.setString(1, "South Broadway Avenue");
            ps2.setString(2, "Rochester");
            ps2.setString(3, "MN");
            ps2.executeUpdate();
            ps2.close();

            assertEquals(begins, requests[BEGIN].get());
            assertEquals(ends, requests[END].get());

            @SuppressWarnings("unused")
            Connection con3 = defaultDataSource.getConnection();
            assertEquals(begins, requests[BEGIN].get());
            assertEquals(ends, requests[END].get());
        } finally {
            tx.commit();
        }

        assertEquals(begins, requests[BEGIN].get());
        assertEquals(ends + 1, requests[END].get());
    }

    /**
     * Verify that within a global transaction, a request is made per unshared connection,
     * and those requests remain open until the connection is closed and the transaction ends,
     * regardless of whether the close happens within the transaction or after it.
     */
    @Test
    public void testMultipleUnsharedConnectionsInTransaction() throws Exception {
        boolean successful = false;
        Connection con1 = null, con2 = null;
        AtomicInteger[] requests1 = null, requests2 = null, requests3 = null;
        int begin1 = -1000, end1 = -1000;
        int begin2 = -2000, end2 = -2000;
        int begin3 = -3000, end3 = -3000;

        tx.begin();
        try {
            con1 = unsharableXADataSource.getConnection();
            requests1 = (AtomicInteger[]) con1.unwrap(Supplier.class).get();
            begin1 = requests1[BEGIN].get();
            end1 = requests1[END].get();
            assertEquals(end1 + 1, begin1);

            PreparedStatement ps1 = con1.prepareStatement("INSERT INTO STREETS VALUES(?, ?, ?)");
            ps1.setString(1, "Salem Road SW");
            ps1.setString(2, "Rochester");
            ps1.setString(3, "MN");
            ps1.executeUpdate();
            ps1.close();

            con2 = unsharableXADataSource.getConnection();
            requests2 = (AtomicInteger[]) con2.unwrap(Supplier.class).get();
            begin2 = requests2[BEGIN].get();
            end2 = requests2[END].get();
            assertEquals(end2 + 1, begin2);

            PreparedStatement ps2 = con2.prepareStatement("INSERT INTO STREETS VALUES(?, ?, ?)");
            ps2.setString(1, "Assisi Drive NW");
            ps2.setString(2, "Rochester");
            ps2.setString(3, "MN");
            ps2.executeUpdate();
            ps2.close();

            Connection con3 = unsharableXADataSource.getConnection();
            try {
                requests3 = (AtomicInteger[]) con3.unwrap(Supplier.class).get();
                begin3 = requests3[BEGIN].get();
                end3 = requests3[END].get();
                assertEquals(end3 + 1, begin3);
            } finally {
                con3.close();
            }

            // all requests remain open for the duration of the transaction
            assertEquals(requests3[BEGIN].get(), begin3);
            end3 = requests3[END].get();
            assertEquals(end3 + 1, begin3);

            end2 = requests2[END].get();
            assertEquals(end2 + 1, begin2);

            end1 = requests1[END].get();
            assertEquals(end1 + 1, begin1);

            successful = true;
        } finally {
            tx.commit();

            try {
                if (successful) {
                    successful = false;

                    // third requests ends after commit due to closed connection
                    assertEquals(requests3[BEGIN].get(), begin3);
                    end3 = requests3[END].get();
                    assertEquals(end3, begin3);

                    // other requests remain open across transaction boundary
                    assertEquals(requests1[BEGIN].get(), begin1);
                    end1 = requests1[END].get();
                    assertEquals(end1 + 1, begin1);

                    assertEquals(requests2[BEGIN].get(), begin2);
                    end2 = requests2[END].get();
                    assertEquals(end2 + 1, begin2);

                    successful = true;
                }
            } finally {
                con1.close();

                try {
                    if (successful) {
                        successful = false;

                        // first request ends due to closed connection
                        assertEquals(requests1[BEGIN].get(), begin1);
                        end1 = requests1[END].get();
                        assertEquals(end1, begin1);

                        // second request still open due to connection remaining open
                        assertEquals(requests2[BEGIN].get(), begin2);
                        end2 = requests2[END].get();
                        assertEquals(end2 + 1, begin2);

                        successful = true;
                    }
                } finally {
                    con2.close();

                    if (successful) {
                        // second request ends due to closed connection
                        assertEquals(requests2[BEGIN].get(), begin2);
                        end2 = requests2[END].get();
                        assertEquals(end2, begin2);
                    }
                }
            }
        }
    }

    /**
     * Verify that within a global transaction, a single request is used across get/use/close of all shared handles.
     */
    @Test
    public void testSerialReuseInGlobalTransaction() throws Exception {
        AtomicInteger[] requests;
        int begins = -1000, ends = -1000;

        Connection con3;
        tx.begin();
        try {
            Connection con1 = defaultDataSource.getConnection();
            requests = (AtomicInteger[]) con1.unwrap(Supplier.class).get();
            begins = requests[BEGIN].get();
            ends = requests[END].get();
            assertEquals(ends + 1, begins);

            PreparedStatement ps1 = con1.prepareStatement("INSERT INTO STREETS VALUES(?, ?, ?)");
            ps1.setString(1, "East River Road");
            ps1.setString(2, "Rochester");
            ps1.setString(3, "MN");
            ps1.executeUpdate();
            ps1.close();
            con1.close();

            Connection con2 = defaultDataSource.getConnection();
            PreparedStatement ps2 = con2.prepareStatement("INSERT INTO STREETS VALUES(?, ?, ?)");
            ps2.setString(1, "West River Road");
            ps2.setString(2, "Rochester");
            ps2.setString(3, "MN");
            ps2.executeUpdate();
            ps2.close();
            con2.close();

            assertEquals(begins, requests[BEGIN].get());
            assertEquals(ends, requests[END].get());

            con3 = defaultDataSource.getConnection();
            assertEquals(begins, requests[BEGIN].get());
            assertEquals(ends, requests[END].get());
        } finally {
            tx.commit();
        }

        assertEquals(begins, requests[BEGIN].get());
        assertEquals(ends + 1, requests[END].get());

        tx.begin();
        try {
            // might use different managed connection instance
            PreparedStatement ps3 = con3.prepareStatement("UPDATE STREETS SET NAME=? WHERE NAME=? AND CITY=? AND STATE=?");
            requests = (AtomicInteger[]) con3.unwrap(Supplier.class).get();
            begins = requests[BEGIN].get();
            ends = requests[END].get();
            assertEquals(ends + 1, begins);

            ps3.setString(1, "West River Parkway");
            ps3.setString(2, "West River Road");
            ps3.setString(3, "Rochester");
            ps3.setString(4, "MN");
            ps3.executeUpdate();
            ps3.close();

            assertEquals(ends + 1, begins);
        } finally {
            tx.commit();
        }

        assertEquals(begins, requests[BEGIN].get());
        assertEquals(ends + 1, requests[END].get());
    }

    /**
     * Verify that within an LTC, a single request is used across get/use/close of all shared handles.
     */
    @Test
    public void testSerialReuseInLTC() throws Exception {
        AtomicInteger[] requests;
        int begins = -1000, ends = -1000;

        Connection con1 = defaultDataSource.getConnection();
        try {
            requests = (AtomicInteger[]) con1.unwrap(Supplier.class).get();
            begins = requests[BEGIN].get();
            ends = requests[END].get();
            assertEquals(ends + 1, begins);

            PreparedStatement ps1 = con1.prepareStatement("INSERT INTO STREETS VALUES(?, ?, ?)");
            ps1.setString(1, "East Center Street");
            ps1.setString(2, "Rochester");
            ps1.setString(3, "MN");
            ps1.executeUpdate();
            ps1.close();
        } finally {
            con1.close();
        }

        Connection con2 = defaultDataSource.getConnection();
        try {
            PreparedStatement ps2 = con2.prepareStatement("INSERT INTO STREETS VALUES(?, ?, ?)");
            ps2.setString(1, "W Center Street");
            ps2.setString(2, "Rochester");
            ps2.setString(3, "MN");
            ps2.executeUpdate();
            ps2.close();

            assertEquals(begins, requests[BEGIN].get());
            assertEquals(ends, requests[END].get());
        } finally {
            con2.close();
        }

        Connection con3 = defaultDataSource.getConnection();
        try {
            assertEquals(begins, requests[BEGIN].get());
            assertEquals(ends, requests[END].get());

            // end the LTC
            tx.begin();
            tx.commit();

            assertEquals(begins, requests[BEGIN].get());
            assertEquals(ends + 1, requests[END].get());

            // new LTC, might use different managed connection instance
            PreparedStatement ps3 = con3.prepareStatement("UPDATE STREETS SET NAME=? WHERE NAME=? AND CITY=? AND STATE=?");
            requests = (AtomicInteger[]) con3.unwrap(Supplier.class).get();
            begins = requests[BEGIN].get();
            ends = requests[END].get();
            assertEquals(ends + 1, begins);

            ps3.setString(1, "West Center Street");
            ps3.setString(2, "W Center Street");
            ps3.setString(3, "Rochester");
            ps3.setString(4, "MN");
            ps3.executeUpdate();
            ps3.close();
        } finally {
            con3.close();
        }

        // end the second LTC
        tx.begin();
        tx.commit();

        assertEquals(begins, requests[BEGIN].get());
        assertEquals(ends + 1, requests[END].get());
    }

    /**
     * Verify that within a global transaction, separate requests are used for each unshared connection,
     * and that these requests only end when both the connection is closed AND the global transaction has ended.
     */
    @Test
    public void testSerialUnsharedInGlobalTransaction() throws Exception {
        AtomicInteger[] requests1 = null, requests2 = null, requests3 = null;
        int begin1 = -1000, end1 = -1000;
        int begin2 = -2000, end2 = -2000;
        int begin3 = -3000, end3 = -3000;

        boolean successful = false;
        Connection con3 = null;
        tx.begin();
        try {
            Connection con1 = unsharableXADataSource.getConnection();
            try {
                requests1 = (AtomicInteger[]) con1.unwrap(Supplier.class).get();
                begin1 = requests1[BEGIN].get();
                end1 = requests1[END].get();
                assertEquals(end1 + 1, begin1);

                PreparedStatement ps1 = con1.prepareStatement("INSERT INTO STREETS VALUES(?, ?, ?)");
                ps1.setString(1, "Bamber Valley Road SW");
                ps1.setString(2, "Rochester");
                ps1.setString(3, "MN");
                ps1.executeUpdate();
                ps1.close();
            } finally {
                con1.close();
            }

            Connection con2 = unsharableXADataSource.getConnection();
            try {
                PreparedStatement ps2 = con2.prepareStatement("INSERT INTO STREETS VALUES(?, ?, ?)");
                ps2.setString(1, "Mayowood Road SW");
                ps2.setString(2, "Rochester");
                ps2.setString(3, "MN");
                ps2.executeUpdate();
                ps2.close();

                requests2 = (AtomicInteger[]) con2.unwrap(Supplier.class).get();
                begin2 = requests2[BEGIN].get();
                end2 = requests2[END].get();
                assertEquals(end2 + 1, begin2);
            } finally {
                con2.close();
            }

            con3 = unsharableXADataSource.getConnection();

            requests3 = (AtomicInteger[]) con3.unwrap(Supplier.class).get();
            begin3 = requests1[BEGIN].get();
            end3 = requests1[END].get();
            assertEquals(end3 + 1, begin3);

            successful = true;
        } finally {
            tx.commit();

            if (!successful && con3 != null)
                con3.close();
        }

        try {
            // first two requests should end after commit because their connections were closed
            assertEquals(end1 + 1, requests1[END].get());
            assertEquals(end2 + 1, requests2[END].get());

            // third request must not end because the connection remains open
            assertEquals(end3, requests3[END].get());

            tx.begin();
            try {
                // same managed connection instance is used here
                PreparedStatement ps3 = con3.prepareStatement("INSERT INTO STREETS VALUES(?, ?, ?)");
                assertEquals(begin3, requests3[BEGIN].get());
                assertEquals(end3, requests3[END].get());

                ps3.setString(1, "Collegeview Road SE");
                ps3.setString(2, "Rochester");
                ps3.setString(3, "MN");
                ps3.executeUpdate();
                ps3.close();

                assertEquals(end3, requests3[END].get());
            } finally {
                tx.commit();
            }
        } finally {
            con3.close();
        }
        assertEquals(end3 + 1, requests3[END].get());
    }

    /**
     * Verify that within an LTC, a separate request is used for get/use/close of each unshared handle.
     */
    @Test
    public void testSerialUnsharedInLTC() throws Exception {
        AtomicInteger[] requests;
        int begins = -1000, ends = -1000;

        Connection con1 = unsharablePool1DataSource.getConnection();
        try {
            requests = (AtomicInteger[]) con1.unwrap(Supplier.class).get();
            begins = requests[BEGIN].get();
            ends = requests[END].get();
            assertEquals(ends + 1, begins);

            PreparedStatement ps1 = con1.prepareStatement("INSERT INTO STREETS VALUES(?, ?, ?)");
            ps1.setString(1, "Silver Creek Road NE");
            ps1.setString(2, "Rochester");
            ps1.setString(3, "MN");
            ps1.executeUpdate();
            ps1.close();
        } finally {
            con1.close();
        }
        assertEquals(ends + 1, requests[END].get());

        // same connection must be returned from the pool because pool size is 1
        Connection con2 = unsharablePool1DataSource.getConnection();
        try {
            PreparedStatement ps2 = con2.prepareStatement("INSERT INTO STREETS VALUES(?, ?, ?)");
            ps2.setString(1, "Marion Road SE");
            ps2.setString(2, "Rochester");
            ps2.setString(3, "MN");
            ps2.executeUpdate();
            ps2.close();

            assertEquals(begins + 1, requests[BEGIN].get());
            assertEquals(ends + 1, requests[END].get());
        } finally {
            con2.close();
        }
        assertEquals(ends + 2, requests[END].get());

        // Again, same connection must be returned from the pool because pool size is 1
        Connection con3 = unsharablePool1DataSource.getConnection();
        try {
            assertEquals(begins + 2, requests[BEGIN].get());
            assertEquals(ends + 2, requests[END].get());

            // end the LTC
            tx.begin();
            tx.commit();

            assertEquals(begins + 2, requests[BEGIN].get());
            assertEquals(ends + 2, requests[END].get());

            // new LTC, but same request
            PreparedStatement ps3 = con3.prepareStatement("INSERT INTO STREETS VALUES(?, ?, ?)");
            assertEquals(begins + 2, requests[BEGIN].get());
            assertEquals(ends + 2, requests[END].get());

            ps3.setString(1, "Pinewood Road SE");
            ps3.setString(2, "Rochester");
            ps3.setString(3, "MN");
            ps3.executeUpdate();
            ps3.close();
        } finally {
            con3.close();
        }
        assertEquals(ends + 3, requests[END].get());

        // end the second LTC
        tx.begin();
        tx.commit();

        assertEquals(begins + 2, requests[BEGIN].get());
        assertEquals(ends + 3, requests[END].get());
    }

    /**
     * Begin a request (using a sharable connection) within one global transaction. Suspend that transaction
     * and use the connection handle within a different global transaction, which must be considered a different
     * request. After committing the second global transaction, resume the first global transaction and perform
     * additional operations which should be under the first request. Commit the first transaction and verify
     * that the first request has ended.
     */
    @ExpectedFFDC("java.lang.IllegalStateException") // TODO remove this once transactions bug is fixed
    @Test
    public void testSuspendRunGlobalTranAndResumeSharable() throws Exception {
        AtomicInteger[] requests1;
        int begin1, end1;

        AtomicInteger[] requests2;
        int begin2, end2;

        // Obtain and use a connection handle within one global transaction
        tx.begin();
        try {
            Connection con = defaultDataSource.getConnection();
            con.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);

            PreparedStatement ps1 = con.prepareStatement("INSERT INTO STREETS VALUES(?, ?, ?)");
            ps1.setString(1, "Viola Road NE");
            ps1.setString(2, "Rochester");
            ps1.setString(3, "MN");
            ps1.executeUpdate();
            ps1.close();

            requests1 = (AtomicInteger[]) con.unwrap(Supplier.class).get();
            begin1 = requests1[BEGIN].get();
            end1 = requests1[END].get();
            assertEquals(end1 + 1, begin1);

            Transaction suspended = txm.suspend();
            try {
                tx.begin();
                try {
                    assertEquals(end1, requests1[END].get());
                    assertEquals(Connection.TRANSACTION_SERIALIZABLE, con.getTransactionIsolation());

                    requests2 = (AtomicInteger[]) con.unwrap(Supplier.class).get();
                    begin2 = requests2[BEGIN].get();
                    end2 = requests2[END].get();
                    assertEquals(end2 + 1, begin2);

                    PreparedStatement ps2 = con.prepareStatement("INSERT INTO STREETS VALUES(?, ?, ?)");
                    ps2.setString(1, "Elton Hills Drive NW");
                    ps2.setString(2, "Rochester");
                    ps2.setString(3, "MN");
                    ps2.executeUpdate();
                    ps2.close();

                } finally {
                    tx.rollback();
                }
                // TODO bug: It appears the same managed connection remains with the sharable handle across suspend/begin/rollback
                // and therefore it is operating under the original request, rather than being a separate request that can end here!
                //TODO assertEquals(end2 + 1, requests2[END].get());
                System.out.println("At this point, the second request should have end count of " + (end2 + 1) + ". We observed: " + requests2[END].get());
            } finally {
                txm.resume(suspended);
            }

            assertEquals(end1, requests1[END].get());

            try {
                // Continue using the connection handle after the global transaction completes,
                PreparedStatement ps3 = con.prepareStatement("INSERT INTO STREETS VALUES(?, ?, ?)");
                ps3.setString(1, "Northern Heights Drive NE");
                ps3.setString(2, "Rochester");
                ps3.setString(3, "MN");
                ps3.executeUpdate();
                ps3.close();

                assertEquals(Connection.TRANSACTION_SERIALIZABLE, con.getTransactionIsolation());
            } finally {
                con.close();
            }
        } finally {
            tx.commit();
        }

        assertEquals(end1 + 1, requests1[END].get());

        // Check if the data is consistent. The first and third update must commit. The second must roll back.
        Connection c = defaultDataSource.getConnection();
        try {
            PreparedStatement ps = c.prepareStatement("SELECT NAME FROM STREETS WHERE NAME=? AND CITY=? AND STATE=?");
            ps.setString(3, "MN");
            ps.setString(2, "Rochester");
            ps.setString(1, "Viola Road NE");
            assertTrue(ps.executeQuery().next()); // must be committed

            ps.setString(1, "Elton Hills Drive NW");
            // TODO data integrity bug: either the suspend/resume or the inner begin/rollback are not being honored!
            // assertFalse(ps.executeQuery().next()); // must be rolled back

            ps.setString(1, "Northern Heights Drive NE");
            assertTrue(ps.executeQuery().next()); // must be committed

            ps.close();
        } finally {
            c.close();
        }
    }

    /**
     * Begin a request (using an unsharable connection) within one global transaction. Suspend that transaction
     * and use the connection within a different global transaction, which must be considered the same request
     * because it is the same connection, even though running under a different transaction.
     * After committing the second global transaction, resume the first global transaction and perform additional
     * operations, which should continue be under the first (and only) request. Commit the first transaction and
     * verify that the single request has ended.
     */
    @ExpectedFFDC("java.lang.IllegalStateException") // TODO remove this once transactions bug is fixed
    @Test
    public void testSuspendRunGlobalTranAndResumeUnsharable() throws Exception {
        AtomicInteger[] requests;
        int begins, ends;

        // Obtain and use a connection within one global transaction
        Connection con = null;
        tx.begin();
        try {
            con = unsharableXADataSource.getConnection();
            con.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);

            PreparedStatement ps1 = con.prepareStatement("INSERT INTO STREETS VALUES(?, ?, ?)");
            ps1.setString(1, "Bandel Road NW");
            ps1.setString(2, "Rochester");
            ps1.setString(3, "MN");
            ps1.executeUpdate();
            ps1.close();

            requests = (AtomicInteger[]) con.unwrap(Supplier.class).get();
            begins = requests[BEGIN].get();
            ends = requests[END].get();
            assertEquals(ends + 1, begins);

            Transaction suspended = txm.suspend();
            try {
                tx.begin();
                try {
                    assertEquals(ends, requests[END].get());
                    assertEquals(Connection.TRANSACTION_READ_UNCOMMITTED, con.getTransactionIsolation());

                    PreparedStatement ps2 = con.prepareStatement("INSERT INTO STREETS VALUES(?, ?, ?)");
                    ps2.setString(1, "Country Club Road SW");
                    ps2.setString(2, "Rochester");
                    ps2.setString(3, "MN");
                    ps2.executeUpdate();
                    ps2.close();

                } finally {
                    tx.rollback();
                }
                assertEquals(ends, requests[END].get());
            } finally {
                txm.resume(suspended);
            }

            assertEquals(ends, requests[END].get());

            // Continue using the connection after the global transaction completes,
            PreparedStatement ps3 = con.prepareStatement("INSERT INTO STREETS VALUES(?, ?, ?)");
            ps3.setString(1, "Wilder Road NW");
            ps3.setString(2, "Rochester");
            ps3.setString(3, "MN");
            ps3.executeUpdate();
            ps3.close();

            assertEquals(Connection.TRANSACTION_READ_UNCOMMITTED, con.getTransactionIsolation());
        } finally {
            try {
                if (con != null)
                    con.close();
            } finally {
                tx.commit();
            }
        }

        assertEquals(ends + 1, requests[END].get());

        // Check if the data is consistent. The first and third update must commit. The second must roll back.
        Connection c = unsharableXADataSource.getConnection();
        try {
            PreparedStatement ps = c.prepareStatement("SELECT NAME FROM STREETS WHERE NAME=? AND CITY=? AND STATE=?");
            ps.setString(3, "MN");
            ps.setString(2, "Rochester");
            ps.setString(1, "Bandel Road NW");
            assertTrue(ps.executeQuery().next()); // must be committed

            ps.setString(1, "Country Club Road SW");
            // TODO data integrity bug: either the suspend/resume or the inner begin/rollback are not being honored!
            // assertFalse(ps.executeQuery().next()); // must be rolled back

            ps.setString(1, "Wilder Road NW");
            assertTrue(ps.executeQuery().next()); // must be committed

            ps.close();
        } finally {
            c.close();
        }
    }

    /**
     * Begin a request (using a sharable connection) within one global transaction. Suspend that transaction
     * and use the connection handle within an LTC, which must be considered a different request.
     * After performing commits and rollbacks within the LTC, resume the first global transaction and perform
     * additional operations which should be under the first request. Commit the first transaction and verify
     * that the first request has ended.
     */
    @Test
    public void testSuspendRunLTCAndResumeSharable() throws Exception {
        AtomicInteger[] requests1;
        int begin1, end1;

        AtomicInteger[] requests2;
        int begin2, end2;

        // Obtain and use a connection handle within one global transaction
        tx.begin();
        try {
            Connection con = defaultDataSource.getConnection();
            con.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);

            PreparedStatement ps1 = con.prepareStatement("INSERT INTO STREETS VALUES(?, ?, ?)");
            ps1.setString(1, "Simpson Road SE");
            ps1.setString(2, "Rochester");
            ps1.setString(3, "MN");
            ps1.executeUpdate();
            ps1.close();

            requests1 = (AtomicInteger[]) con.unwrap(Supplier.class).get();
            begin1 = requests1[BEGIN].get();
            end1 = requests1[END].get();
            assertEquals(end1 + 1, begin1);

            Transaction suspended = txm.suspend();
            try {
                assertEquals(end1, requests1[END].get());
                assertEquals(Connection.TRANSACTION_REPEATABLE_READ, con.getTransactionIsolation());

                requests2 = (AtomicInteger[]) con.unwrap(Supplier.class).get();
                begin2 = requests2[BEGIN].get();
                end2 = requests2[END].get();
                assertEquals(end2 + 1, begin2);

                con.setAutoCommit(false);
                PreparedStatement ps2 = con.prepareStatement("INSERT INTO STREETS VALUES(?, ?, ?)");
                ps2.setString(1, "Eastwood Road SE");
                ps2.setString(2, "Rochester");
                ps2.setString(3, "MN");
                ps2.executeUpdate();
                ps2.close();
                // TODO con.commit(); enable once suspend/resume is fixed

                PreparedStatement ps3 = con.prepareStatement("INSERT INTO STREETS VALUES(?, ?, ?)");
                ps3.setString(1, "Park Lane SE");
                ps3.setString(2, "Rochester");
                ps3.setString(3, "MN");
                ps3.executeUpdate();
                ps3.close();
                // TODO con.rollback(); enable once suspend/resume is fixed

                // TODO con.setAutoCommit(true); enable once suspend/resume is fixed

                // TODO bug: It appears the same managed connection remains with the sharable handle across suspend/begin/rollback
                // and therefore it is operating under the original request, rather than being a separate request that can end here!
                // assertEquals(end2 + 1, requests2[END].get());
                System.out.println("At this point, the second request should have end count of " + (end2 + 1) + ". We observed: " + requests2[END].get());
            } finally {
                txm.resume(suspended);
            }

            assertEquals(end1, requests1[END].get());

            try {
                // Continue using the connection handle after the LTC ends,
                PreparedStatement ps4 = con.prepareStatement("INSERT INTO STREETS VALUES(?, ?, ?)");
                ps4.setString(1, "St. Bridget's Road SE");
                ps4.setString(2, "Rochester");
                ps4.setString(3, "MN");
                ps4.executeUpdate();
                ps4.close();

                assertEquals(Connection.TRANSACTION_REPEATABLE_READ, con.getTransactionIsolation());
            } finally {
                con.close();
            }
        } finally {
            tx.commit();
        }

        assertEquals(end1 + 1, requests1[END].get());

        // Check if the data is consistent. The first and third update must commit. The second must roll back.
        Connection c = defaultDataSource.getConnection();
        try {
            PreparedStatement ps = c.prepareStatement("SELECT NAME FROM STREETS WHERE NAME=? AND CITY=? AND STATE=?");
            ps.setString(3, "MN");
            ps.setString(2, "Rochester");
            ps.setString(1, "Simpson Road SE");
            assertTrue(ps.executeQuery().next()); // must be committed (from global transaction)

            ps.setString(1, "Eastwood Road SE");
            assertTrue(ps.executeQuery().next()); // must be committed (from commit in LTC)

            ps.setString(1, "Park Lane SE");
            // TODO enable once suspend/resume are fixed
            //assertFalse(ps.executeQuery().next()); // must be rolled back (from rollback in LTC)

            ps.setString(1, "St. Bridget's Road SE");
            assertTrue(ps.executeQuery().next()); // must be committed (from global transaction)

            ps.close();
        } finally {
            c.close();
        }
    }

    /**
     * Begin a request (using an unsharable connection) within one global transaction. Suspend that transaction
     * and use the connection within an LTC, which must be considered the same request because it is the same
     * connection, even though running separate transactions. After committing and rolling back transactions
     * within the LTC, resume the first global transaction and perform additional operations, which should
     * continue be under the first (and only) request. Commit the first transaction and verify that the single
     * request has ended.
     */
    @Test
    public void testSuspendRunLTCAndResumeUnsharable() throws Exception {
        AtomicInteger[] requests;
        int begins, ends;

        // Obtain and use a connection within one global transaction
        Connection con = null;
        tx.begin();
        try {
            con = unsharableXADataSource.getConnection();
            con.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);

            PreparedStatement ps1 = con.prepareStatement("INSERT INTO STREETS VALUES(?, ?, ?)");
            ps1.setString(1, "Valkyrie Drive NW");
            ps1.setString(2, "Rochester");
            ps1.setString(3, "MN");
            ps1.executeUpdate();
            ps1.close();

            requests = (AtomicInteger[]) con.unwrap(Supplier.class).get();
            begins = requests[BEGIN].get();
            ends = requests[END].get();
            assertEquals(ends + 1, begins);

            Transaction suspended = txm.suspend();
            try {
                assertEquals(ends, requests[END].get());
                assertEquals(Connection.TRANSACTION_READ_UNCOMMITTED, con.getTransactionIsolation());

                con.setAutoCommit(false);
                PreparedStatement ps2 = con.prepareStatement("INSERT INTO STREETS VALUES(?, ?, ?)");
                ps2.setString(1, "Zumbro Drive NW");
                ps2.setString(2, "Rochester");
                ps2.setString(3, "MN");
                ps2.executeUpdate();
                ps2.close();
                // TODO con.rollback(); enable once suspend/resume is fixed

                PreparedStatement ps3 = con.prepareStatement("INSERT INTO STREETS VALUES(?, ?, ?)");
                ps3.setString(1, "Viking Drive NW");
                ps3.setString(2, "Rochester");
                ps3.setString(3, "MN");
                ps3.executeUpdate();
                ps3.close();
                // TODO con.commit(); enable once suspend/resume is fixed

                // TODO con.setAutoCommit(true); enable once suspend/resume is fixed

                assertEquals(ends, requests[END].get());
            } finally {
                txm.resume(suspended);
            }

            assertEquals(ends, requests[END].get());

            // Continue using the connection after the global transaction completes,
            PreparedStatement ps4 = con.prepareStatement("INSERT INTO STREETS VALUES(?, ?, ?)");
            ps4.setString(1, "Terracewood Drive NW");
            ps4.setString(2, "Rochester");
            ps4.setString(3, "MN");
            ps4.executeUpdate();
            ps4.close();

            assertEquals(Connection.TRANSACTION_READ_UNCOMMITTED, con.getTransactionIsolation());
        } finally {
            try {
                if (con != null)
                    con.close();
            } finally {
                tx.commit();
            }
        }

        assertEquals(ends + 1, requests[END].get());

        // Check if the data is consistent. The first and third update must commit. The second must roll back.
        Connection c = unsharableXADataSource.getConnection();
        try {
            PreparedStatement ps = c.prepareStatement("SELECT NAME FROM STREETS WHERE NAME=? AND CITY=? AND STATE=?");
            ps.setString(3, "MN");
            ps.setString(2, "Rochester");
            ps.setString(1, "Valkyrie Drive NW");
            assertTrue(ps.executeQuery().next()); // must be committed (from global transaction)

            ps.setString(1, "Zumbro Drive NW");
            // TODO enable once suspend/resume are fixed
            //assertFalse(ps.executeQuery().next()); // must be rolled back (from rollback during LTC)

            ps.setString(1, "Viking Drive NW");
            assertTrue(ps.executeQuery().next()); // must be committed (from commit during LTC)

            ps.setString(1, "Terracewood Drive NW");
            assertTrue(ps.executeQuery().next()); // must be committed

            ps.close();
        } finally {
            c.close();
        }
    }
}
