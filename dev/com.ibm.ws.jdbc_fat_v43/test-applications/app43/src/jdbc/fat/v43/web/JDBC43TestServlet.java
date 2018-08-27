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
import javax.transaction.UserTransaction;

import org.junit.Test;

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

    @Resource
    UserTransaction tx;

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
}
