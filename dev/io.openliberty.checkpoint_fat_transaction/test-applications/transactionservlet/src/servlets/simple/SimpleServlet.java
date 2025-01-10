/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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

package servlets.simple;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import javax.annotation.Resource;
import javax.annotation.Resource.AuthenticationType;
import javax.inject.Inject;
import javax.naming.InitialContext;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;
import javax.transaction.UserTransaction;

import org.junit.Test;

import com.ibm.tx.jta.TransactionManagerFactory;
import com.ibm.tx.jta.UserTransactionFactory;
import com.ibm.websphere.uow.UOWSynchronizationRegistry;
import com.ibm.ws.tx.jta.embeddable.UserTransactionController;
import com.ibm.wsspi.uow.UOWAction;
import com.ibm.wsspi.uow.UOWManager;

import componenttest.annotation.ExpectedFFDC;
import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/SimpleServlet")
public class SimpleServlet extends FATServlet {

    @Resource(name = "jdbc/derby", shareable = true, authenticationType = AuthenticationType.APPLICATION)
    DataSource ds;

    @Inject
    AsyncBean bean;

    /**
     * Message written to servlet to indicate that is has been successfully invoked.
     */
    public static final String SUCCESS_MESSAGE = "COMPLETED SUCCESSFULLY";

    private String printStatus(int status) {
        switch (status) {
            case Status.STATUS_ACTIVE:
                return "Status.STATUS_ACTIVE";
            case Status.STATUS_COMMITTED:
                return "Status.STATUS_COMMITTED";
            case Status.STATUS_COMMITTING:
                return "Status.STATUS_COMMITTING";
            case Status.STATUS_MARKED_ROLLBACK:
                return "Status.STATUS_MARKED_ROLLBACK";
            case Status.STATUS_NO_TRANSACTION:
                return "Status.STATUS_NO_TRANSACTION";
            case Status.STATUS_PREPARED:
                return "Status.STATUS_PREPARED";
            case Status.STATUS_PREPARING:
                return "Status.STATUS_PREPARING";
            case Status.STATUS_ROLLEDBACK:
                return "Status.STATUS_ROLLEDBACK";
            case Status.STATUS_ROLLING_BACK:
                return "Status.STATUS_ROLLING_BACK";
            default:
                return "Status.STATUS_UNKNOWN";
        }
    }

    final int fallbackResult = 17;

    @Test
    public void testUserTranLookup(HttpServletRequest request, HttpServletResponse response) throws Exception {
        final Object ut = new InitialContext().lookup("java:comp/UserTransaction");

        if (ut instanceof javax.transaction.UserTransaction) {
            ((UserTransaction) ut).begin();
            ((UserTransaction) ut).commit();
        } else {
            if (ut == null) {
                throw new Exception("UserTransaction instance was null");
            } else {
                throw new Exception("UserTransaction lookup did not work: " + ut.getClass().getCanonicalName());
            }
        }
    }

    @Test
    public void testUserTranFactory(HttpServletRequest request, HttpServletResponse response) throws Exception {
        UserTransaction ut = UserTransactionFactory.getUserTransaction();
        ut.begin();
        ut.commit();
    }

    @Test
    public void testTranSyncRegistryLookup(HttpServletRequest request, HttpServletResponse response) throws Exception {
        InitialContext context = new InitialContext();
        TransactionSynchronizationRegistry tsr = (TransactionSynchronizationRegistry) context.lookup("java:comp/TransactionSynchronizationRegistry");

        int status = tsr.getTransactionStatus();
        if (status != Status.STATUS_NO_TRANSACTION) {
            throw new IllegalStateException("Expected first STATUS_NO_TRANSACTION, got " + printStatus(status));
        }

        UserTransaction ut = (UserTransaction) context.lookup("java:comp/UserTransaction");

        ut.begin();

        status = tsr.getTransactionStatus();
        if (status != Status.STATUS_ACTIVE) {
            throw new IllegalStateException("Expected STATUS_ACTIVE, got " + printStatus(status));
        }

        ut.commit();

        status = tsr.getTransactionStatus();
        if (status != Status.STATUS_NO_TRANSACTION) {
            throw new IllegalStateException("Expected second STATUS_NO_TRANSACTION, got " + printStatus(status));
        }
    }

    /**
     * Test basic database connectivity
     */
    @Test
    public void testBasicConnection(HttpServletRequest request, HttpServletResponse response) throws Exception {

        String statusString = "";

        try {

            InitialContext context = new InitialContext();
            UserTransaction tran = (UserTransaction) context.lookup("java:comp/UserTransaction");

            statusString = statusString + "UserTransaction=" + tran + "<br>";
            //DataSource ds = (DataSource) context.lookup("java:comp/env/jdbc/derby");
            statusString = statusString + "DataSource=" + ds + "<br>";
            Connection con = ds.getConnection();

            try {
                statusString = statusString + "Connection=" + con + "<br>";
                DatabaseMetaData metadata = con.getMetaData();
                String dbName = metadata.getDatabaseProductName();
                statusString = statusString + "Database Name=" + dbName + "<br>";
                String dbVersion = metadata.getDatabaseProductVersion();
                statusString = statusString + "Database Version=" + dbVersion + "<br>";

                // Set up table
                Statement stmt = con.createStatement();
                statusString = statusString + "Statement=" + stmt + "<br>";
                try {
                    stmt.executeUpdate("drop table bvtable");
                } catch (SQLException x) {
                    // didn't exist
                }
                stmt.executeUpdate("create table bvtable (col1 int not null primary key, col2 varchar(20))");

                // Insert data
                PreparedStatement ps = con.prepareStatement("insert into bvtable values (?, ?)");
                statusString = statusString + "PreparedStatement=" + ps + "<br>";
                ps.setInt(1, 45);
                ps.setString(2, "XLV");
                ps.executeUpdate();
                ps.setInt(1, 91);
                ps.setString(2, "XCI");
                ps.executeUpdate();
                ps.setInt(1, 13);
                ps.setString(2, "XIII");
                ps.executeUpdate();
                ps.close();

                tran.begin();
                try {
                    stmt = con.createStatement();
                    stmt.executeUpdate("update bvtable set col1=24, col2='XXIV' where col1=13");
                } finally {
                    tran.commit();
                }

                // Query for updates
                stmt = con.createStatement();
                ResultSet rs = stmt.executeQuery("select col1, col2 from bvtable order by col1 asc");
                statusString = statusString + "ResultSet=" + rs + "<br>";
                while (rs.next())
                    statusString = statusString + "Entry from database (" + rs.getInt(1) + ", " + rs.getString(2) + ")<br>";
                rs.close();
                stmt.close();

            } finally {
                con.close();
            }
        } catch (Exception ex) {

            if (statusString.indexOf("DataSource=") < 0)
                fail("Missing DataSource in output. " + statusString);

            if (statusString.indexOf("Connection=") < 0)
                fail("Missing Connection in output. " + statusString);

            if (statusString.indexOf("Statement=") < 0)
                fail("Missing Statement in output. " + statusString);

            if (statusString.indexOf("PreparedStatement=") < 0)
                fail("Missing PreparedStatement in output. " + statusString);

            if (statusString.indexOf("ResultSet=") < 0)
                fail("Missing ResultSet in output. " + statusString);

            if (statusString.indexOf("Entry from database (45, XLV)") < 0)
                fail("Missing entry (45, XLV). Output: " + statusString);

            if (statusString.indexOf("Entry from database (91, XCI)") < 0)
                fail("Missing entry (91, XCI). Output: " + statusString);

            if (statusString.indexOf("Entry from database (24, XXIV)") < 0)
                fail("Missing entry (24, XXIV). Output: " + statusString);
        }
    }

    /**
     * Test enlistment in transactions.
     *
     * @param request  HTTP request
     * @param response HTTP response
     * @throws Exception if an error occurs.
     */
    @Test
    public void testTransactionEnlistment(HttpServletRequest request, HttpServletResponse response) throws Exception {
        InitialContext context = new InitialContext();
        //DataSource ds = (DataSource) context.lookup("java:comp/env/jdbc/derby");
        PrintWriter out = response.getWriter();
        Connection con = ds.getConnection();
        try {
            // Set up table
            Statement stmt = con.createStatement();

            try {
                stmt.executeUpdate("drop table bvtable");
            } catch (SQLException x) {
                // didn't exist
            }
            stmt.executeUpdate("create table bvtable (col1 int not null primary key, col2 varchar(20))");
            stmt.executeUpdate("insert into bvtable values (1, 'one')");
            stmt.executeUpdate("insert into bvtable values (2, 'two')");
            stmt.executeUpdate("insert into bvtable values (3, 'three')");
            stmt.executeUpdate("insert into bvtable values (4, 'four')");

            // UserTransaction Commit
            con.setAutoCommit(false);
            UserTransaction tran = (UserTransaction) context.lookup("java:comp/UserTransaction");
            tran.setTransactionTimeout(7); // afford 6 more seconds to commit
            tran.begin();
            try {
                stmt = con.createStatement();
                stmt.executeUpdate("update bvtable set col2='uno' where col1=1");
            } finally {
                tran.commit();
            }

            con.rollback(); // shouldn't have any impact because update was made in UserTransaction

            stmt = con.createStatement();
            ResultSet result = stmt.executeQuery("select col2 from bvtable where col1=1");
            if (!result.next())
                throw new Exception("entry with col1=1 not found in table");
            String value = result.getString(1);
            if (!"uno".equals(value))
                throw new Exception("UserTransaction commit not honored. Incorrect value: " + value);

            out.println("UserTransaction commit successful<br>");

            con.commit();

            // UserTransaction Rollback
            tran.begin();
            try {
                stmt = con.createStatement();
                stmt.executeUpdate("update bvtable set col2='dos' where col1=2");
            } finally {
                tran.rollback();
            }
            con.commit(); // shouldn't have any impact because update was made in UserTransaction

            stmt = con.createStatement();
            result = stmt.executeQuery("select col2 from bvtable where col1=2");
            if (!result.next())
                throw new Exception("entry with col1=2 not found in table");
            value = result.getString(1);
            if (!"two".equals(value))
                throw new Exception("UserTransaction rollback not honored. Incorrect value: " + value);

            out.println("UserTransaction rollback successful<br>");

            // Connection commit
            stmt.executeUpdate("update bvtable set col2='tres' where col1=3");
            con.commit();
            result = stmt.executeQuery("select col2 from bvtable where col1=3");
            if (!result.next())
                throw new Exception("entry with col1=3 not found in table");
            value = result.getString(1);
            if (!"tres".equals(value))
                throw new Exception("Connection commit not honored. Incorrect value: " + value);

            out.println("Connection commit successful<br>");

            // Connection rollback
            stmt.executeUpdate("update bvtable set col2='cuatro' where col1=4");
            con.rollback();
            result = stmt.executeQuery("select col2 from bvtable where col1=4");
            if (!result.next())
                throw new Exception("entry with col1=4 not found in table");
            value = result.getString(1);
            if (!"four".equals(value))
                throw new Exception("Connection rollback not honored. Incorrect value: " + value);

            out.println("Connection rollback successful<br>");
        } finally {
            try {
                con.rollback();
            } catch (Throwable x) {
            }
            con.close();
        }
    }

    /**
     * Test that rolling back a newly started UserTransaction doesn't affect the previously implicitly committed
     * LTC transaction.
     */
    @Test
    public void testImplicitLTCCommit(HttpServletRequest request, HttpServletResponse response) throws Exception {
        InitialContext context = new InitialContext();
        //DataSource ds = (DataSource) context.lookup("java:comp/env/jdbc/derby");
        Connection con = ds.getConnection();
        try {
            // Set up table
            Statement stmt = con.createStatement();

            try {
                stmt.executeUpdate("drop table bvtable");
            } catch (SQLException x) {
                // didn't exist
            }

            con.setAutoCommit(true);
            stmt.executeUpdate("create table bvtable (col1 int not null primary key, col2 varchar(20))");
            stmt.executeUpdate("insert into bvtable values (1, 'one')");
            stmt.executeUpdate("insert into bvtable values (2, 'two')");
            stmt.executeUpdate("insert into bvtable values (3, 'three')");
            stmt.executeUpdate("insert into bvtable values (4, 'four')");

            //Begin an UserTransaction and implicitly commit the LTC
            UserTransaction tran = (UserTransaction) context.lookup("java:comp/UserTransaction");
            tran.begin();

            tran.rollback(); // we should have already committed when we began the UserTransaction

            stmt = con.createStatement();
            ResultSet result = stmt.executeQuery("select col2 from bvtable where col1=1");

            if (!result.next())
                throw new Exception("entry with col1=1 not found in table");

            String value = result.getString(1);

            if (!"one".equals(value))
                throw new Exception("Implicit LTC commit not honored. Incorrect value: " + value);

        } finally {
            con.close();
        }
    }

    @Test
    @ExpectedFFDC(value = { "javax.transaction.NotSupportedException" })
    public void testNEW(HttpServletRequest request, HttpServletResponse response) throws Exception {
        InitialContext context = new InitialContext();
        //DataSource ds = (DataSource) context.lookup("java:comp/env/jdbc/derby");
        Connection con = ds.getConnection();
        try {
            // Set up table
            Statement stmt = con.createStatement();

            try {
                stmt.executeUpdate("drop table bvtable");
            } catch (SQLException x) {
                // didn't exist
            }

            con.setAutoCommit(true);
            stmt.executeUpdate("create table bvtable (col1 int not null primary key, col2 varchar(20))");
            stmt.executeUpdate("insert into bvtable values (1, 'one')");
            stmt.executeUpdate("insert into bvtable values (2, 'two')");
            stmt.executeUpdate("insert into bvtable values (3, 'three')");
            stmt.executeUpdate("insert into bvtable values (4, 'four')");

            //Begin an UserTransaction and implicitly commit the LTC
            UserTransaction tran = (UserTransaction) context.lookup("java:comp/UserTransaction");

            tran.begin();

            //Begin an UserTransaction and implicitly commit the LTC
            UserTransaction tran2 = (UserTransaction) context.lookup("java:comp/UserTransaction");
            try {
                tran2.begin();
            } catch (NotSupportedException nex) {
                System.out.println("caught nex - thats rght!");
            }
            tran.commit(); // we should have already committed when we began the UserTransaction

            //Should now have LTC
            stmt = con.createStatement();
            ResultSet result = stmt.executeQuery("select col2 from bvtable where col1=1");

            if (!result.next()) {
                System.out.println("entry with col1=1 not found in table");
                throw new Exception("entry with col1=1 not found in table");
            }

            String value = result.getString(1);

            if (!"one".equals(value))
                throw new Exception("Implicit LTC commit not honored. Incorrect value: " + value);

        } finally {
            con.close();
        }
    }

    @Test
    @ExpectedFFDC(value = { "javax.transaction.NotSupportedException" })
    public void testNEW2(HttpServletRequest request, HttpServletResponse response) throws Exception {
        InitialContext context = new InitialContext();
        //DataSource ds = (DataSource) context.lookup("java:comp/env/jdbc/derby");
        Connection con = ds.getConnection();
        try {
            // Set up table
            Statement stmt = con.createStatement();

            try {
                stmt.executeUpdate("drop table bvtable");
            } catch (SQLException x) {
                // didn't exist
            }

            con.setAutoCommit(true);
            stmt.executeUpdate("create table bvtable (col1 int not null primary key, col2 varchar(20))");
            stmt.executeUpdate("insert into bvtable values (1, 'one')");
            stmt.executeUpdate("insert into bvtable values (2, 'two')");
            stmt.executeUpdate("insert into bvtable values (3, 'three')");
            stmt.executeUpdate("insert into bvtable values (4, 'four')");

            //Begin an UserTransaction and implicitly commit the LTC
            UserTransaction tran = (UserTransaction) context.lookup("java:comp/UserTransaction");
            try {
                tran.begin();

                //Begin an UserTransaction and implicitly commit the LTC
                UserTransaction tran2 = (UserTransaction) context.lookup("java:comp/UserTransaction");

                tran2.begin();

                tran.commit(); // we should have already committed when we began the UserTransaction
            } catch (NotSupportedException nex) {
                System.out.println("caught nex - thats rght! Lets rollback");
                tran.rollback();
            }

            //Should now have LTC
            stmt = con.createStatement();
            ResultSet result = stmt.executeQuery("select col2 from bvtable where col1=1");

            if (!result.next())
                throw new Exception("entry with col1=1 not found in table");

            String value = result.getString(1);

            if (!"one".equals(value))
                throw new Exception("Implicit LTC commit not honored. Incorrect value: " + value);

        } finally {
            con.close();
        }
    }

    /**
     * Test that rolling back a newly started UserTransaction doesn't affect the previously explicitly committed
     * LTC transaction.
     */
    @Test
    public void testExplicitLTCCommit(HttpServletRequest request, HttpServletResponse response) throws Exception {
        InitialContext context = new InitialContext();
        //DataSource ds = (DataSource) context.lookup("java:comp/env/jdbc/derby");
        Connection con = ds.getConnection();
        try {
            // Set up table
            Statement stmt = con.createStatement();

            try {
                stmt.executeUpdate("drop table bvtable");
            } catch (SQLException x) {
                // didn't exist
            }

            con.setAutoCommit(false);
            stmt.executeUpdate("create table bvtable (col1 int not null primary key, col2 varchar(20))");
            stmt.executeUpdate("insert into bvtable values (1, 'one')");
            stmt.executeUpdate("insert into bvtable values (2, 'two')");
            stmt.executeUpdate("insert into bvtable values (3, 'three')");
            stmt.executeUpdate("insert into bvtable values (4, 'four')");
            con.commit();

            //Begin an UserTransaction and implicitly commit the LTC
            UserTransaction tran = (UserTransaction) context.lookup("java:comp/UserTransaction");
            tran.begin();

            tran.rollback(); // we should have already committed when we began the UserTransaction

            stmt = con.createStatement();
            ResultSet result = stmt.executeQuery("select col2 from bvtable where col1=1");

            if (!result.next())
                throw new Exception("entry with col1=1 not found in table");

            String value = result.getString(1);

            if (!"one".equals(value))
                throw new Exception("Implicit LTC commit not honored. Incorrect value: " + value);

        } finally {
            con.commit(); //need to commit because auto-commit was turned off
            con.close();
        }
    }

    @Test
    public void testLTCAfterGlobalTran(HttpServletRequest request, HttpServletResponse response) throws Exception {
        UserTransaction tran = (UserTransaction) new InitialContext().lookup("java:comp/UserTransaction");
        Statement stmt;
        Connection con;

        con = ds.getConnection();
        try {
            stmt = con.createStatement();

            tran.begin();
            try {
                if (!stmt.isClosed())
                    throw new Exception("1) For sharable connection, statement should be closed after tran.begin");
            } finally {
                tran.commit();
            }
        } finally {
            con.close();
        }

        con = ds.getConnection();
        try {
            stmt = con.createStatement();

            tran.begin();
            try {
                if (!stmt.isClosed())
                    throw new Exception("2) For sharable connection, statement should be closed after tran.begin");
            } finally {
                tran.rollback();
            }
        } finally {
            con.close();
        }
    }

    @Test
    public void testUOWManagerLookup(HttpServletRequest request, HttpServletResponse response) throws Exception {
        final UOWManager uowm = (UOWManager) new InitialContext().lookup("java:comp/websphere/UOWManager");

        if (!(uowm instanceof UOWManager)) {
            throw new Exception("Lookup of java:comp/websphere/UOWManager failed");
        }

        final long localUOWId = uowm.getLocalUOWId();

        uowm.runUnderUOW(UOWSynchronizationRegistry.UOW_TYPE_GLOBAL_TRANSACTION, false, new UOWAction() {
            @Override
            public void run() throws Exception {
                if (localUOWId == uowm.getLocalUOWId()) {
                    throw new Exception("UOWAction not run under new UOW");
                }
            }
        });
    }

    @Test
    public void testUserTranRestriction(HttpServletRequest request, HttpServletResponse response) throws Exception {
        final UserTransaction ut = (UserTransaction) new InitialContext().lookup("java:comp/UserTransaction");

        final UserTransactionController utc = (UserTransactionController) new InitialContext().lookup("java:comp/websphere/UOWManager");

        utc.setEnabled(false);

        try {
            ut.begin();
            throw new Exception("UserTransaction.begin() was not disabled.");
        } catch (IllegalStateException e) {
            // As expected
        }

        try {
            ut.commit();
            throw new Exception("UserTransaction.commit() was not disabled.");
        } catch (IllegalStateException e) {
            // As expected
        }

        try {
            ut.getStatus();
            throw new Exception("UserTransaction.getStatus() was not disabled.");
        } catch (IllegalStateException e) {
            // As expected
        }

        try {
            ut.rollback();
            throw new Exception("UserTransaction.rollback() was not disabled.");
        } catch (IllegalStateException e) {
            // As expected
        }

        try {
            ut.setRollbackOnly();
            throw new Exception("UserTransaction.setRollbackOnly() was not disabled.");
        } catch (IllegalStateException e) {
            // As expected
        }

        try {
            ut.setTransactionTimeout(0);
            throw new Exception("UserTransaction.setTransactionTimeout() was not disabled.");
        } catch (IllegalStateException e) {
            // As expected
        }

        utc.setEnabled(true);

        ut.begin();
        ut.commit();
    }

    @Test
    public void testSetTransactionTimeout(HttpServletRequest request, HttpServletResponse response) throws Exception {
        final TransactionManager tm = TransactionManagerFactory.getTransactionManager();

        int tries = 0;

        tm.setTransactionTimeout(1);
        tm.begin();

        // wait up to 5 seconds
        while (Status.STATUS_ACTIVE == tm.getStatus() && ++tries < 5) {
            Thread.sleep(1000);
        }

        try {
            tm.commit();
            // Should have rolled back
            throw new Exception("Test failed because 1 second timeout hadn't popped after " + tries + " seconds!");
        } catch (RollbackException e) {
            // As expected
        }
    }

    @Test
    public void testSingleThreading() throws Exception {
        final TransactionManager tm = TransactionManagerFactory.getTransactionManager();

        tm.begin();

        final Transaction tran = tm.getTransaction();

        try {
            final CompletableFuture<Integer> stage = CompletableFuture.supplyAsync(() -> {
                try {
                    assertNull(tm.suspend());

                    tm.resume(tran);
                } catch (IllegalStateException e) {
                    return 1;
                } catch (Exception e) {
                    throw new CompletionException(e);
                }

                return 0;
            });

            assertEquals(Integer.valueOf(1), stage.join());
        } finally {
            tm.rollback();
        }
    }
}
