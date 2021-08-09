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
package jdbc.fat.v41.web;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import javax.annotation.Resource;
import javax.naming.InitialContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.sql.DataSource;

import org.junit.Test;

import componenttest.app.FATDatabaseServlet;

@WebServlet(urlPatterns = "/DefaultDataSourceTestServlet")
public class DefaultDataSourceTestServlet extends FATDatabaseServlet {
    private static final long serialVersionUID = 6698194309425789687L;
    private final String tableName = "cities";

    @Resource
    DataSource defaultDataSource;

    @Override
    public void init() throws ServletException {
        createTable(defaultDataSource, tableName, "id int not null primary key, city varchar(30)");
    }

    /**
     * Execute a simple insert and select statement using the default datasource.
     * This test uses an datasource using resource injection.
     */
    @Test
    public void testDefaultDataSourceInjected() throws Throwable {
        Connection conn = defaultDataSource.getConnection();
        try {
            Statement stmt = conn.createStatement();
            stmt.executeUpdate("insert into " + tableName + " values (1, 'Rochester')");
            ResultSet result = stmt.executeQuery("select city from " + tableName + " where id=1");
            if (!result.next())
                throw new Exception("Entry missing from database");
            String value = result.getString(1);
            if (!"Rochester".equals(value))
                throw new Exception("Expected to get city of Rochester but instead got " + value);
        } finally {
            conn.close();
        }
    }

    /**
     * Execute a simple insert and select statement using the default datasource.
     * This test uses an datasource using a jndi lookup.
     */
    @Test
    public void testDefaultDataSourceLookup() throws Throwable {
        DataSource myDS = (DataSource) new InitialContext().lookup("java:comp/DefaultDataSource");
        Connection conn = myDS.getConnection();
        try {
            Statement stmt = conn.createStatement();
            stmt.executeUpdate("insert into " + tableName + " values (2, 'Austin')");
            ResultSet result = stmt.executeQuery("select city from " + tableName + " where id=2");
            if (!result.next())
                throw new Exception("Entry missing from database");
            String value = result.getString(1);
            if (!"Austin".equals(value))
                throw new Exception("Expected to get city of Austin but instead got " + value);
        } finally {
            conn.close();
        }
    }
}
