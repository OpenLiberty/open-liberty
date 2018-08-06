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
package jdbc.fat.derby.web;

import java.sql.Connection;
import java.sql.DatabaseMetaData;

import javax.annotation.Resource;
import javax.annotation.Resource.AuthenticationType;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

import org.junit.Test;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/JDBCDerbyServlet")
public class JDBCDerbyServlet extends FATServlet {

    @Resource(name = "jdbc/ds1", shareable = false, authenticationType = AuthenticationType.APPLICATION)
    DataSource ds1;

    @Test
    public void testServletWorking(HttpServletRequest request, HttpServletResponse response) throws Exception {
        Connection conn = ds1.getConnection();
        try {
            DatabaseMetaData md = conn.getMetaData();
            System.out.println("Using driver: " + md.getDatabaseProductName() + ' ' + md.getDatabaseProductVersion());
            System.out.println("Driver is JDBC version: " + md.getJDBCMajorVersion() + '.' + md.getJDBCMinorVersion());
            System.out.println("other: " + md.getDriverMajorVersion() + "." + md.getDriverMinorVersion());
        } finally {
            conn.close();
        }
    }

}
