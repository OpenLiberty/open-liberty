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

import com.ibm.ws.security.javaeesec.cdi.beans.hash.Pbkdf2PasswordHashImpl;
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
            Pbkdf2PasswordHashImpl pphi = new Pbkdf2PasswordHashImpl();
            String testpwd = pphi.generate((new String(Constants.DB_USER1_PWD)).toCharArray());
            String testpwd2 = pphi.generate((new String(Constants.DB_USER2_PWD)).toCharArray());
            String testpwd3 = pphi.generate((new String(Constants.DB_USER3_PWD)).toCharArray());

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
