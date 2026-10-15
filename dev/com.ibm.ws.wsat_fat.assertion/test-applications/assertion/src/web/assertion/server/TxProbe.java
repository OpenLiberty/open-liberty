/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package web.assertion.server;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import javax.naming.InitialContext;
import javax.sql.DataSource;
import javax.transaction.Status;
import javax.transaction.UserTransaction;

/**
 * Makes WS-AT propagation observable from the server side of a web service call.
 *
 * <ul>
 * <li>{@code tx=ACTIVE} / {@code tx=NONE} reports whether a global transaction was
 * active on the endpoint's thread, i.e. whether a WS-AT coordination context was
 * imported from the request.</li>
 * <li>A row is inserted into an XA Derby table. If the endpoint joined the caller's
 * transaction the row's fate follows the caller's commit/rollback. If it did not,
 * the row is committed locally and always persists.</li>
 * </ul>
 */
public class TxProbe {

    private static final String DATA_SOURCE = "jdbc/wsatDataSource0";
    private static final String TABLE = "condTable";

    /** Endpoint entry point: insert a row for {@code key} and report transaction state. */
    public static String probe(String key) throws Exception {
        final UserTransaction ut = (UserTransaction) new InitialContext().lookup("java:comp/UserTransaction");
        final String tx = ut.getStatus() == Status.STATUS_NO_TRANSACTION ? "NONE" : "ACTIVE";
        System.out.println("TxProbe: key=" + key + " tx=" + tx);

        if (key != null && !key.isEmpty()) {
            try (Connection con = dataSource().getConnection();
                    PreparedStatement ps = con.prepareStatement("insert into " + TABLE + " values (?, ?)")) {
                ps.setString(1, key);
                ps.setString(2, tx);
                ps.executeUpdate();
            }
        }
        return "Hello " + key + " tx=" + tx;
    }

    /** Create the table if it does not exist. Call outside any global transaction. */
    public static void init() throws Exception {
        try (Connection con = dataSource().getConnection(); Statement s = con.createStatement()) {
            s.executeUpdate("create table " + TABLE + " (id varchar(60) primary key not null, tx varchar(10))");
        } catch (Exception e) {
            System.out.println("TxProbe.init: table probably exists: " + e.getMessage());
        }
    }

    /** Number of rows persisted for {@code key}. Call outside any global transaction. */
    public static int count(String key) throws Exception {
        try (Connection con = dataSource().getConnection();
                PreparedStatement ps = con.prepareStatement("select count(*) from " + TABLE + " where id = ?")) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private static DataSource dataSource() throws Exception {
        return (DataSource) new InitialContext().lookup(DATA_SOURCE);
    }
}
