/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package web;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.naming.InitialContext;
import javax.sql.DataSource;

/**
 * Task that update an entry in the database when it runs.
 */
public class DBUpdateTask implements Runnable {
    private static final String resourceRef = "java:app/env/jdbc/persistMultiDB_RC";
	
    // Get the current count from the database
    static int get() throws Exception {
        DataSource dataSource = (DataSource) new InitialContext().lookup(resourceRef);
        Connection con = dataSource.getConnection();
        try {
            ResultSet result = con.createStatement().executeQuery("SELECT VAL FROM MYTABLE");
            return result.next() ? result.getInt(1) : 0;
        } finally {
            con.close();
        }

    }

    static void init() throws Exception {
        DataSource dataSource = (DataSource) new InitialContext().lookup(resourceRef);
        Connection con = dataSource.getConnection();
        try {
            boolean created = false;
            try {
                con.createStatement().executeUpdate("CREATE TABLE MYTABLE (VAL INTEGER)");
                created = true;
            } catch (SQLException x) {
            }
            if (created)
                con.createStatement().executeUpdate("INSERT INTO MYTABLE VALUES (0)");
        } finally {
            con.close();
        }
    }

    @Override
    public void run() {
        try {
            DataSource dataSource = (DataSource) new InitialContext().lookup(resourceRef);
            Connection con = dataSource.getConnection();
            try {
                con.createStatement().executeUpdate("UPDATE MYTABLE SET VAL=VAL+1");
            } finally {
                con.close();
            }
        } catch (RuntimeException x) {
            throw x;
        } catch (Exception x) {
            throw new RuntimeException(x);
        }
    }
}
