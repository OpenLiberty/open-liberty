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

import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.sql.DataSource;

import org.junit.Test;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/JDBC43TestServlet")
public class JDBC43TestServlet extends FATServlet {

    @Resource
    DataSource defaultDataSource;

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
}
