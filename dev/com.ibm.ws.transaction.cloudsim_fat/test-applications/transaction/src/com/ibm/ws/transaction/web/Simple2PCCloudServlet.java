/*******************************************************************************
 * Copyright (c) 2017, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.transaction.web;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.annotation.Resource;
import javax.annotation.Resource.AuthenticationType;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import javax.transaction.Transaction;

import com.ibm.tx.jta.ExtendedTransactionManager;
import com.ibm.tx.jta.TransactionManagerFactory;
import com.ibm.tx.jta.ut.util.XAResourceImpl;
import com.ibm.ws.cloudtx.ut.util.LastingXAResourceImpl;

import componenttest.app.FATServlet;

@WebServlet("/Simple2PCCloudServlet")
public class Simple2PCCloudServlet extends FATServlet {

    /**  */
    private static final String filter = "(testfilter=jon)";

    @Resource(name = "jdbc/derby", shareable = true, authenticationType = AuthenticationType.APPLICATION)
    DataSource ds;
    @Resource(name = "jdbc/tranlogDataSource", shareable = true, authenticationType = AuthenticationType.APPLICATION)
    DataSource dsTranLog;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        XAResourceImpl.setStateFile(System.getenv("WLP_OUTPUT_DIR") + "/../shared/" + LastingXAResourceImpl.STATE_FILE_ROOT);
        super.doGet(request, response);
    }

    public void commitSuicide(HttpServletRequest request,
                              HttpServletResponse response) throws Exception {
        Runtime.getRuntime().halt(0);
    }

    public void testLeaseTableAccess(HttpServletRequest request,
                                     HttpServletResponse response) throws Exception {
        Connection con = ds.getConnection();
        try {
            // Statement used to drop table
            Statement stmt = con.createStatement();

            try {
                System.out.println("modifyLeaseOwner: sel-for-update against Lease table");
                String selForUpdateString = "SELECT LEASE_OWNER" +
                                            " FROM WAS_LEASES_LOG" +
                                            " WHERE SERVER_IDENTITY='cloud001' FOR UPDATE OF LEASE_OWNER";
                ResultSet rs = stmt.executeQuery(selForUpdateString);
                while (rs.next()) {
                    String owner = rs.getString("LEASE_OWNER");
                    System.out.println("testLeaseTableAccess: owner is - " + owner);
                }
                rs.close();

                String updateString = "UPDATE WAS_LEASES_LOG" +
                                      " SET LEASE_OWNER = 'cloud002'" +
                                      " WHERE SERVER_IDENTITY='cloud001'";
                stmt.executeUpdate(updateString);
            } catch (SQLException x) {
                System.out.println("testLeaseTableAccess: caught exception - " + x);
            }

            System.out.println("testLeaseTableAccess: commit changes to database");
            con.commit();
        } catch (Exception ex) {
            System.out.println("testLeaseTableAccess: caught exception in testSetup: " + ex);
        }
    }

    public void setupRec001(HttpServletRequest request,
                            HttpServletResponse response) throws Exception {
        final ExtendedTransactionManager tm = TransactionManagerFactory
                        .getTransactionManager();
        System.out.println("NYTRACE: setUpRec001 started for tm: " + tm);
        try {
            tm.begin();

            final Transaction tx = tm.getTransaction();

            System.out.println("NYTRACE: setUpRec001 got transaction tx: " + tx);
            LastingXAResourceImpl xares1 = new LastingXAResourceImpl();
            System.out.println("NYTRACE: setUpRec001 working with res: " + xares1);
            xares1.setCommitAction(XAResourceImpl.DIE);
            try {
                tx.enlistResource(xares1);
            } catch (IllegalStateException ex) {
                System.out.println("NYTRACE: setUpRec001 caught: " + ex);
                // Assume transient (recovery incomplete) and retry after pause
                Thread.sleep(5000);
                tx.enlistResource(xares1);
            }

            tx.enlistResource(new LastingXAResourceImpl());
            tx.enlistResource(new LastingXAResourceImpl());

            tm.commit();
        } catch (Exception e) {
            System.out.println("NYTRACE: ImplodeServlet caught exc: " + e);
            e.printStackTrace();
        }
    }

    public void modifyLeaseOwner(HttpServletRequest request,
                                 HttpServletResponse response) throws Exception {
//        InitialContext context = new InitialContext();
//        DataSource dsTranLog = (DataSource) context.lookup("java:comp/env/jdbc/tranlogDataSource");

        Connection con = dsTranLog.getConnection();
        try {
            // Statement used to drop table
            Statement stmt = con.createStatement();

            try {
                System.out.println("modifyLeaseOwner: sel-for-update against Lease table");
                String selForUpdateString = "SELECT LEASE_OWNER" +
                                            " FROM WAS_LEASES_LOG" +
                                            " WHERE SERVER_IDENTITY='cloud001' FOR UPDATE OF LEASE_OWNER";
                ResultSet rs = stmt.executeQuery(selForUpdateString);
                while (rs.next()) {
                    String owner = rs.getString("LEASE_OWNER");
                    System.out.println("modifyLeaseOwner: owner is - " + owner);
                }
                rs.close();

                String updateString = "UPDATE WAS_LEASES_LOG" +
                                      " SET LEASE_OWNER = 'cloud002'" +
                                      " WHERE SERVER_IDENTITY='cloud001'";
                stmt.executeUpdate(updateString);
            } catch (SQLException x) {
                System.out.println("modifyLeaseOwner: caught exception - " + x);
            }

            System.out.println("modifyLeaseOwner: commit changes to database");
            con.commit();
        } catch (Exception ex) {
            System.out.println("modifyLeaseOwner: caught exception in testSetup: " + ex);
        }
    }
}