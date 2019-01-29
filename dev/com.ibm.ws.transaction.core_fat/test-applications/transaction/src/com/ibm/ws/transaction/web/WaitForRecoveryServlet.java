/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.transaction.web;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.annotation.Resource;
import javax.annotation.Resource.AuthenticationType;
import javax.naming.InitialContext;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import javax.transaction.UserTransaction;
import javax.transaction.xa.XAResource;

import com.ibm.tx.jta.ExtendedTransactionManager;
import com.ibm.tx.jta.TransactionManagerFactory;
import com.ibm.tx.jta.ut.util.XAResourceFactoryImpl;
import com.ibm.tx.jta.ut.util.XAResourceImpl;
import com.ibm.tx.jta.ut.util.XAResourceInfoFactory;
import com.ibm.websphere.uow.UOWSynchronizationRegistry;
import com.ibm.wsspi.uow.ExtendedUOWAction;
import com.ibm.wsspi.uow.UOWManager;

import componenttest.app.FATServlet;

@WebServlet("/WaitForRecoveryServlet")
public class WaitForRecoveryServlet extends FATServlet {

    /**  */
    private static final String filter = "(testfilter=jon)";

    @Resource
    UOWManager uowm;
    @Resource(name = "jdbc/derby", shareable = true, authenticationType = AuthenticationType.APPLICATION)
    DataSource ds;

    public void commitSuicide(HttpServletRequest request,
                              HttpServletResponse response) throws Exception {
        Runtime.getRuntime().halt(0);
    }

    public void testRec001(HttpServletRequest request,
                           HttpServletResponse response) throws Exception {
        UserTransaction ut = null;
        final Connection con = ds.getConnection();
        // Do the lookups on the DS
        final InitialContext ctx = new InitialContext();
        System.out.println("testRec001: Context is: " + ctx.toString());

        try {
            // Preliminary work to create the DUMMY table, if necessary and to
            // set the row to its initial state.
            setupDatabaseTable(con);

            // Look up the User Transaction
            ut = (UserTransaction) ctx.lookup("java:comp/UserTransaction");
            System.out.println("testRec001: Have looked up UT: " + ut);

            // Start a new transaction
            ut.begin();
            System.out.println("testRec001: Start Transaction");

            Statement stmt = con.createStatement();
            String selForUpdateString = "SELECT ADDR" +
                                        " FROM DUMMY" +
                                        " WHERE NUM=1 FOR UPDATE OF ADDR";
            ResultSet rs = stmt.executeQuery(selForUpdateString);
            while (rs.next()) {
                String owner = rs.getString("ADDR");
                System.out.println("testTableAccess: addr is - " + owner);
            }
            rs.close();

            String updateString = "UPDATE DUMMY" +
                                  " SET ADDR = 'avenue'" +
                                  " WHERE NUM=1";
            stmt.executeUpdate(updateString);

            final Object res = uowm.runUnderUOW(UOWSynchronizationRegistry.UOW_TYPE_GLOBAL_TRANSACTION, true, new ExtendedUOWAction() {
                @Override
                public Object run() throws Exception {
                    final ExtendedTransactionManager tm = TransactionManagerFactory
                                    .getTransactionManager();

                    final Serializable xaResInfo1 = XAResourceInfoFactory
                                    .getXAResourceInfo(0);
                    final Serializable xaResInfo2 = XAResourceInfoFactory
                                    .getXAResourceInfo(1);
                    final Serializable xaResInfo3 = XAResourceInfoFactory
                                    .getXAResourceInfo(2);

                    final XAResource xaRes1 = XAResourceFactoryImpl.instance()
                                    .getXAResourceImpl(xaResInfo1)
                                    .setCommitAction(XAResourceImpl.DIE);
                    final int recoveryId1 = tm.registerResourceInfo(filter, xaResInfo1);
                    tm.enlist(xaRes1, recoveryId1);

                    final XAResource xaRes2 = XAResourceFactoryImpl.instance()
                                    .getXAResourceImpl(xaResInfo2);
                    final int recoveryId2 = tm.registerResourceInfo(filter, xaResInfo2);
                    tm.enlist(xaRes2, recoveryId2);

                    final XAResource xaRes3 = XAResourceFactoryImpl.instance()
                                    .getXAResourceImpl(xaResInfo3);
                    final int recoveryId3 = tm.registerResourceInfo(filter, xaResInfo3);
                    tm.enlist(xaRes3, recoveryId3);
                    return Boolean.TRUE;
                }
            }, new Class[] { InstantiationException.class }, new Class[] { IllegalStateException.class });

            if (!(Boolean) res) {
                throw new Exception("Didn't get expected value back from runUnderUOW");
            }

            if (ut != null) {

                System.out.println("NYTRACE: Drive COMMIT processing");
                ut.commit();

            }
        } catch (SQLException x) {
            System.out.println("testLeaseTableAccess: caught exception - " + x);
        } catch (Exception e) {
            System.out.println("NYTRACE: SetupRecCore caught exc: " + e);
            e.printStackTrace();
        }
    }

    private void setupDatabaseTable(Connection connection) throws SQLException {
        try {
            // Drop the table
            System.out.println("testRec001: Drop DUMMY table");
            Statement dStmt = connection.createStatement();
            dStmt.executeUpdate("DROP TABLE DUMMY");
        } catch (Exception ex) {
            // swallow this exception, supposition is that table simply does not exist
            System.out.println("testRec001: Caught exception - " + ex);
        }

        System.out.println("testRec001: create a table");
        Statement cStmt = connection.createStatement();

        cStmt.executeUpdate("CREATE TABLE DUMMY( " +
                            "NUM SMALLINT, " +
                            "ADDR VARCHAR(30)) ");
        System.out.println("Have created the table - insert row");
        Statement iStmt = connection.createStatement();
        String insertString = "INSERT INTO DUMMY " +
                              "VALUES (1, 'Lane')";

        iStmt.executeUpdate(insertString);
        iStmt.close();
        connection.commit();
    }
}