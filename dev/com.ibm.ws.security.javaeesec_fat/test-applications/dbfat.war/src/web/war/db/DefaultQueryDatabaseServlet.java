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
package web.war.db;

import java.sql.Connection;
import java.sql.Statement;

import javax.naming.InitialContext;
import javax.servlet.ServletException;
import javax.sql.DataSource;

import com.ibm.ws.security.javaeesec.fat_helper.Constants;

import componenttest.app.FATDatabaseServlet;

public class DefaultQueryDatabaseServlet extends FATDatabaseServlet {
    private static final long serialVersionUID = 6698194309425789687L;

    private final String callerTable = "callers";
    private final String groupTable = "caller_groups";

    @SuppressWarnings("restriction")
    @Override
    public void init() throws ServletException {
        System.out.println("Creating database for DatabaseIdentityStore");
        try {
            DataSource ds = (DataSource) new InitialContext().lookup("java:comp/DefaultDataSource");

            // create user and group table
            FATDatabaseServlet.createTable(ds, callerTable, "name varchar(30), password varchar(300)");
            FATDatabaseServlet.createTable(ds, groupTable, "group_name VARCHAR(36), caller_name VARCHAR(36)");

            // create passwords
            // this is a hashed value of "pwd".
            String testpwd = "PBKDF2WithHmacSHA256:2048:cRXf00gdzCPVfeflEINqBWEjm0Vhvlq9IXRI5nYorLU=:68P/amIBNLCGdlGqViThbeL2YJXPJzLdUUohhAltydc=";
            // this is a hashed value of "pwd2".
            String testpwd2 = "PBKDF2WithHmacSHA256:2048:Su7sJuASLoYmKknP/L2nhH2t9XsnX5YEFaYkrgr0T2c=:e12Ym58HsS9PzZPnLfcTL/gFwUhikFllB/cVKa2Obes=";
            // this is a hashed value of "pwd3".
            String testpwd3 = "PBKDF2WithHmacSHA256:2048:4ax3fqYXsu3FWftO+vap99PASwpCkBdZLTsY/6oj3+k=:uhDD9IlbxRRdjQt9Iji5mQV8rMqaYoFb0Fxxb7EnVk4=";

            Connection conn = ds.getConnection();
            Statement stmt1 = conn.createStatement();

            // good users
            stmt1.executeUpdate("insert into " + callerTable + " (password, name) values ('" + testpwd + "' , '" + Constants.DB_USER1 + "')");
            stmt1.close();

            stmt1 = conn.createStatement();
            stmt1.executeUpdate("insert into " + groupTable + " (group_name, caller_name) values ('" + Constants.DB_GROUP2 + "' , '" + Constants.DB_USER1 + "')");
            stmt1.close();

            stmt1 = conn.createStatement();
            stmt1.executeUpdate("insert into " + callerTable + " (password, name) values ('" + testpwd2 + "' , '" + Constants.DB_USER2 + "')");
            stmt1.close();

            stmt1 = conn.createStatement();
            stmt1.executeUpdate("insert into " + groupTable + " (group_name, caller_name) values ('" + Constants.DB_GROUP3 + "' , '" + Constants.DB_USER2 + "')");
            stmt1.close();

            // user with no group
            stmt1 = conn.createStatement();
            stmt1.executeUpdate("insert into " + callerTable + " (password, name) values ('" + testpwd3 + "' , '" + Constants.DB_USER3 + "')");
            stmt1.close();

            // add duplicate user
            stmt1 = conn.createStatement();
            stmt1.executeUpdate("insert into " + callerTable + " (password, name) values ('" + testpwd + "' , '" + Constants.DB_USER_DUPE + "')");
            stmt1.close();

            stmt1 = conn.createStatement();
            stmt1.executeUpdate("insert into " + callerTable + " (password, name) values ('" + testpwd + "' , '" + Constants.DB_USER_DUPE + "')");
            stmt1.close();

            // add user with no password
            stmt1 = conn.createStatement();
            stmt1.executeUpdate("insert into " + callerTable + " (name) values ('" + Constants.DB_USER_NOPWD + "')");
            stmt1.close();

        } catch (Exception e) {
            System.out.println("Failed to create database for DatabaseIdentityStore: " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println("Created database for DatabaseIdentityStore");
    }

}
