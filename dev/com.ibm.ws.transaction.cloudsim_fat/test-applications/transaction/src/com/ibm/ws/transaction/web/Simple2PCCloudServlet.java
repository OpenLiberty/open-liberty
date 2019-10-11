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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.annotation.Resource;
import javax.annotation.Resource.AuthenticationType;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

@WebServlet("/Simple2PCCloudServlet")
public class Simple2PCCloudServlet extends Base2PCCloudServlet {

    @Resource(name = "jdbc/derby", shareable = true, authenticationType = AuthenticationType.APPLICATION)
    DataSource ds;
    @Resource(name = "jdbc/tranlogDataSource", shareable = true, authenticationType = AuthenticationType.APPLICATION)
    DataSource dsTranLog;

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

    public void modifyLeaseOwner(HttpServletRequest request,
                                 HttpServletResponse response) throws Exception {

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

    public void setLatch(HttpServletRequest request,
                         HttpServletResponse response) throws Exception {

        Connection con = dsTranLog.getConnection();
        try {
            // Statement used to drop table
            Statement stmt = con.createStatement();

            try {

                long latch = 255L;
                String updateString = "UPDATE " + "WAS_PARTNER_LOGcloud001" +
                                      " SET RUSECTION_ID = " + latch +
                                      " WHERE RU_ID = -1";
                stmt.executeUpdate(updateString);
            } catch (SQLException x) {
                System.out.println("setLatch: caught exception - " + x);
            }

            System.out.println("setLatch: commit changes to database");
            con.commit();
        } catch (Exception ex) {
            System.out.println("setLatch: caught exception in testSetup: " + ex);
        }
    }

    public void setPeerOwnership(HttpServletRequest request,
                                 HttpServletResponse response) throws Exception {

        Connection con = dsTranLog.getConnection();
        try {
            // Statement used to drop table
            Statement stmt = con.createStatement();

            try {
                String updateString = "UPDATE " + "WAS_PARTNER_LOGcloud001" +
                                      " SET SERVER_NAME = 'cloud002'" +
                                      " WHERE RU_ID = -1";
                stmt.executeUpdate(updateString);
            } catch (SQLException x) {
                System.out.println("setPeerOwnership: caught exception - " + x);
            }

            System.out.println("setPeerOwnership: commit changes to database");
            con.commit();
        } catch (Exception ex) {
            System.out.println("setPeerOwnership: caught exception in testSetup: " + ex);
        }
    }
}