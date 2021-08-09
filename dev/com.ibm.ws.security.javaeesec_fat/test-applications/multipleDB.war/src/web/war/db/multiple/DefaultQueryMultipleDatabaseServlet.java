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
package web.war.db.multiple;

import java.sql.Connection;
import java.sql.Statement;

import javax.naming.InitialContext;
import javax.servlet.ServletException;
import javax.sql.DataSource;

import com.ibm.ws.security.javaeesec.fat_helper.Constants;

import componenttest.app.FATDatabaseServlet;

public class DefaultQueryMultipleDatabaseServlet extends FATDatabaseServlet {
    private static final long serialVersionUID = 6698194309425789688L;

    private final String callerTable = "callers";
    private final String groupTable = "caller_groups";

    @Override
    public void init() throws ServletException {
        System.out.println("Creating database for DatabaseIdentityStore");
        try {
            DataSource ds = (DataSource) new InitialContext().lookup("jdbc/db1");

            // create user and group table
            FATDatabaseServlet.createTable(ds, callerTable, "name varchar(30), password varchar(300)");
            FATDatabaseServlet.createTable(ds, groupTable, "group_name VARCHAR(36), caller_name VARCHAR(36)");

            Connection conn = ds.getConnection();
            Statement stmt1 = conn.createStatement();

            // good users
            stmt1.executeUpdate("insert into " + callerTable + " (password, name) values ('" + Constants.DB_USER1_PWD_HASH + "' , '" + Constants.DB_USER1 + "')");
            stmt1.close();

            stmt1 = conn.createStatement();
            stmt1.executeUpdate("insert into " + groupTable + " (group_name, caller_name) values ('" + Constants.DB_GROUP2 + "' , '" + Constants.DB_USER1 + "')");
            stmt1.close();

            ds = (DataSource) new InitialContext().lookup("jdbc/db2");

            // create user and group table
            FATDatabaseServlet.createTable(ds, callerTable, "name varchar(30), password varchar(300)");
            FATDatabaseServlet.createTable(ds, groupTable, "group_name VARCHAR(36), caller_name VARCHAR(36)");

            conn = ds.getConnection();
            stmt1 = conn.createStatement();

            // good users
            stmt1 = conn.createStatement();
            stmt1.executeUpdate("insert into " + callerTable + " (password, name) values ('" + Constants.DB_USER2_PWD_HASH + "' , '" + Constants.DB_USER2 + "')");
            stmt1.close();

            stmt1 = conn.createStatement();
            stmt1.executeUpdate("insert into " + groupTable + " (group_name, caller_name) values ('" + Constants.DB_GROUP3 + "' , '" + Constants.DB_USER2 + "')");
            stmt1.close();

        } catch (Exception e) {
            System.out.println("Failed to create database for DatabaseIdentityStore: " + e.getMessage());
            throw new ServletException("Failed to create database for DatabaseIdentityStore: " + e.getMessage(), e);
        }
        System.out.println("Created database for DatabaseIdentityStore");
    }

}
