/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.transaction.web;

import java.io.PrintWriter;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import javax.annotation.Resource;
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

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/XAFlowServlet")
public class XAFlowServlet extends FATServlet {

    @Resource
    UserTransaction ut;

    @Resource(name = "jdbc/ds1")
    DataSource ds1;

    @Resource(name = "jdbc/ds2")
    DataSource ds2;

    /* Tran timeout for test setup */
    private static final int SETUP_TIMEOUT = 300; // 5 mins

    /**  */
    private static final String filter = "(testfilter=jon)";

    public void setupXAFlow001(HttpServletRequest request,
                               HttpServletResponse response) throws Exception {
        final ExtendedTransactionManager tm = TransactionManagerFactory.getTransactionManager();
        XAResourceImpl.clear();
        final Serializable xaResInfo1 = XAResourceInfoFactory.getXAResourceInfo(0);
        final Serializable xaResInfo2 = XAResourceInfoFactory.getXAResourceInfo(1);

        tm.setTransactionTimeout(SETUP_TIMEOUT);
        try {
            tm.begin();
            final XAResource xaRes1 = XAResourceFactoryImpl.instance().getXAResourceImpl(xaResInfo1);
            final int recoveryId1 = tm.registerResourceInfo(filter, xaResInfo1);
            tm.enlist(xaRes1, recoveryId1);

            final XAResource xaRes2 = XAResourceFactoryImpl.instance().getXAResource(xaResInfo2);
            final int recoveryId2 = tm.registerResourceInfo(filter, xaResInfo2);
            tm.enlist(xaRes2, recoveryId2);

            XAResourceImpl.dumpState();

            tm.commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void checkXAFlow001(HttpServletRequest request,
                               HttpServletResponse response) throws Exception {
        try {
            if (XAResourceImpl.resourceCount() != 2) {
                throw new Exception("Rec007 failed: "
                                    + XAResourceImpl.resourceCount() + " resources");
            }

            if (!XAResourceImpl.allInState(XAResourceImpl.COMMITTED)) {
                throw new Exception("Rec007 failed: not all resources committed");
            }
        } catch (Exception e) {
            throw e;
        } finally {
            XAResourceImpl.clear();
        }
    }

    public void setupXAFlow002(HttpServletRequest request, HttpServletResponse response) throws Exception {
        try {
            Connection con1 = ds1.getConnection();
//            DatabaseMetaData mdata = con1.getMetaData();
//            System.out.println("XAFLOWSERVLET: Got metadata: " + mdata);
//            String dbName = mdata.getDatabaseProductName();
//            String dbVersion = mdata.getDatabaseProductVersion();
//            System.out.println("XAFLOWSERVLET: You are now connected to " + dbName + ", version " + dbVersion);
//
//            // Get connection to database via second datasource
            Connection con2 = ds2.getConnection();
//            System.out.println("XAFLOWSERVLET: Got connection: " + con2);
//            DatabaseMetaData mdata2 = con2.getMetaData();
//            System.out.println("XAFLOWSERVLET: Got metadata: " + mdata2);
//            String dbName2 = mdata2.getDatabaseProductName();
//            String dbVersion2 = mdata2.getDatabaseProductVersion();
//            System.out.println("XAFLOWSERVLET: You are now connected to " + dbName2 + ", version " + dbVersion2);
//
//            // Start with a clean sheet, drop table if its already there
//            System.out.println("create a statement");
            Statement stmtBasic = con1.createStatement();

            try {
                System.out.println("Drop existing table");
                stmtBasic.executeUpdate("DROP TABLE NEIL_DERBY");
            } catch (Exception e) {
                //Swallow this exception, assuming the table simply wasn't there
                System.out.println("Caught exception when dropping table NEIL_DERBY - " + e);
            }

            // Create the table
            Statement stmt2 = con1.createStatement();

            stmt2.executeUpdate("CREATE TABLE NEIL_DERBY( " +
                                "SERVER_NAME VARCHAR(128), " +
                                "SERVICE_ID SMALLINT, " +
                                "LOG_ID SMALLINT , " +
                                "RU_ID BIGINT, " +
                                "RUSECTION_ID BIGINT, " +
                                "RUSECTION_DATA_INDEX SMALLINT, " +
                                "DATA LONG VARCHAR FOR BIT DATA) ");

            System.out.println("Have created the table - insert special row");
            PreparedStatement specStatement = con1.prepareStatement("INSERT INTO NEIL_DERBY " +
                                                                    "(SERVER_NAME, SERVICE_ID, LOG_ID, RU_ID, RUSECTION_ID, RUSECTION_DATA_INDEX, DATA)" +
                                                                    " VALUES (?,?,?,?,?,?,?)");
            specStatement.setString(1, "server1");
            specStatement.setShort(2, (short) 1);
            specStatement.setShort(3, (short) 1);
            specStatement.setLong(4, -1);
            specStatement.setLong(5, 1);
            specStatement.setShort(6, (short) 1);
            byte buf[] = new byte[2];
            specStatement.setBytes(7, buf);
            int ret = specStatement.executeUpdate();
            System.out.println("Have inserted SPECIAL ROW with return: " + ret);

            stmt2.close();
            con1.commit();

            // Drop second table, if its already there
            System.out.println("create a statement");
            Statement stmtBasic2 = con2.createStatement();

            try {
                System.out.println("Drop existing table");
                stmtBasic2.executeUpdate("DROP TABLE NEIL_DERBY2");
            } catch (Exception e) {
                //Swallow this exception, assuming the table simply wasn't there
                System.out.println("Caught exception when dropping table NEIL_DERBY2 - " + e);
            }

            // Create Table
            stmt2 = con2.createStatement();

            stmt2.executeUpdate("CREATE TABLE NEIL_DERBY2( " +
                                "SERVER_NAME VARCHAR(128), " +
                                "SERVICE_ID SMALLINT, " +
                                "LOG_ID SMALLINT , " +
                                "RU_ID BIGINT, " +
                                "RUSECTION_ID BIGINT, " +
                                "RUSECTION_DATA_INDEX SMALLINT, " +
                                "DATA LONG VARCHAR FOR BIT DATA) ");

            System.out.println("Have created the table - insert special row");
            specStatement = con2.prepareStatement("INSERT INTO NEIL_DERBY2 " +
                                                  "(SERVER_NAME, SERVICE_ID, LOG_ID, RU_ID, RUSECTION_ID, RUSECTION_DATA_INDEX, DATA)" +
                                                  " VALUES (?,?,?,?,?,?,?)");
            specStatement.setString(1, "server1");
            specStatement.setShort(2, (short) 1);
            specStatement.setShort(3, (short) 1);
            specStatement.setLong(4, -1);
            specStatement.setLong(5, 1);
            specStatement.setShort(6, (short) 1);

            buf = new byte[2];
            specStatement.setBytes(7, buf);

            ret = specStatement.executeUpdate();
            System.out.println("Have inserted SPECIAL ROW with return: " + ret);

            stmt2.close();
            con2.commit();

// Now we have the tables, we can do some work against them
//++++++++
            // Start a new transaction
            ut.begin();
            System.out.println("XAFLOWSERVLET: Start Transaction");

            // Execute an update
            System.out.println("XAFLOWSERVLET: Execute UPDATE against FIRST DERBY");

            PreparedStatement ps = con1.prepareStatement("UPDATE NEIL_DERBY SET SERVICE_ID=20 WHERE SERVER_NAME=?");
            ps.setString(1, "server1");
            System.out.println("XAFLOWSERVLET: make executeUpdate call");
            ps.executeUpdate();
            System.out.println("XAFLOWSERVLET: close prepared statement");
            ps.close();

            System.out.println("XAFLOWSERVLET: FIRST DERBY UPDATE has finished");

            // Execute an update
            System.out.println("XAFLOWSERVLET: Execute SECOND DERBY UPDATE");

            PreparedStatement ps2 = con2.prepareStatement("UPDATE NEIL_DERBY2 SET SERVICE_ID=23 WHERE SERVER_NAME=?");
            ps2.setString(1, "server1");
            System.out.println("XAFLOWSERVLET: make second executeUpdate call");
            ps2.executeUpdate();
            System.out.println("XAFLOWSERVLET: close second prepared statement");
            ps2.close();

            System.out.println("XAFLOWSERVLET: SECOND DERBY UPDATE has finished");

        } catch (Exception e) {
            System.out.println("XAFLOWSERVLET: Exception thrown when initializing: " + e);
            e.printStackTrace();
            throw e;
        } finally {
            try {
                ut.commit();
            } catch (Exception e) {
                throw e;
            }
        }

        final PrintWriter out = response.getWriter();
        out.println("XAFLOWSERVLET: SETUP COMPLETED SUCCESSFULLY");
        out.close();
    }

    public void checkXAFlow002(HttpServletRequest request,
                               HttpServletResponse response) throws Exception {
        int serviceID = 0;
        int serviceID2 = 0;
        try {
            // Get connection to database via first datasource
            Connection con1 = ds1.getConnection();

            // Get connection to database via second datasource
            Connection con2 = ds2.getConnection();

            // Execute a Query against first connection, to check the value of the service_id
            System.out.println("create a statement");
            Statement stmtBasic = con1.createStatement();
            ResultSet rsBasic = null;

            System.out.println("Execute a query to check the value of the service_id in the first database");

            rsBasic = stmtBasic.executeQuery("SELECT SERVICE_ID" +
                                             " FROM NEIL_DERBY" +
                                             " WHERE SERVER_NAME='" + "server1" +
                                             "' AND LOG_ID=" + "1");
            while (rsBasic.next()) {
                serviceID = rsBasic.getInt("SERVICE_ID");
            }
            System.out.println("XAFLOWSERVLET: NEIL_DERBY SERVICE_ID is " + serviceID);

            // Execute a Query against second connection, to check the value of the service_id
            System.out.println("create a statement");
            Statement stmtBasic2 = con2.createStatement();
            ResultSet rsBasic2 = null;

            System.out.println("Execute a query to check the value of the service_id in the second database");
            rsBasic2 = stmtBasic2.executeQuery("SELECT SERVICE_ID" +
                                               " FROM NEIL_DERBY2" +
                                               " WHERE SERVER_NAME='" + "server1" +
                                               "' AND LOG_ID=" + "1");

            while (rsBasic2.next()) {
                serviceID2 = rsBasic2.getInt("SERVICE_ID");
            }
            System.out.println("XAFLOWSERVLET: NEIL_DERBY2 SERVICE_ID is " + serviceID2);

        } catch (Exception e) {
            System.out.println("XAFLOWSERVLET: Exception thrown when initializing: " + e);
            e.printStackTrace();

            throw e;
        }

        final PrintWriter out = response.getWriter();
        out.println("checkRecXAFlow001 COMPLETED SUCCESSFULLY expecting service id values 20 and 23, values were " + serviceID + " " + serviceID2);
        out.close();
    }
}