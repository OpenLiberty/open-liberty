/*******************************************************************************
 * Copyright (c) 1997, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.session.store.db;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public abstract class DatabaseHandler {

    public abstract int getSmallColumnSize();

    public abstract int getMediumColumnSize();

    public abstract int getLargeColumnSize();

    public abstract void createTable(Statement s, String tableName) throws SQLException;

    public abstract void createIndex(Connection con, Statement s, String tableName) throws SQLException;

    public abstract boolean doesIndexExists(Connection con, String indexName, String tableName);

    /**
     * Checks if the given connection is still valid.
     *
     * @param conn
     * @return Returns true if the connection is valid. False, otherwise.
     */
    public boolean isConnectionValid(Connection conn) {
        try {
            return conn.isValid(5);
        } catch (Exception e) {
            return false;
        }
    }

}
