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
package com.ibm.ws.rsadapter.jdbc.v41;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;

import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.rsadapter.jdbc.WSJdbcObject;
import com.ibm.ws.rsadapter.jdbc.WSJdbcResultSet;
import com.ibm.ws.rsadapter.jdbc.WSJdbcUtil;

public class WSJdbc41ResultSet extends WSJdbcResultSet implements ResultSet {

    public WSJdbc41ResultSet(ResultSet rsImpl, WSJdbcObject parent) {
        super(rsImpl, parent);
    }

    @Override
    public <T> T getObject(int columnIndex, Class<T> type) throws SQLException {
        try {
            T result = rsetImpl.getObject(columnIndex, type);
            addFreedResources(result);
            return result;
        } catch (IncompatibleClassChangeError e) {
            // If the JDBC driver was compiled with java 6
            throw new SQLFeatureNotSupportedException();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbc41ResultSet.getObject", "39", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    @Override
    public <T> T getObject(String columnLabel, Class<T> type) throws SQLException {
        try {
            T result = rsetImpl.getObject(columnLabel, type);
            addFreedResources(result);
            return result;
        } catch (IncompatibleClassChangeError e) {
            // If the JDBC driver was compiled with java 6
            throw new SQLFeatureNotSupportedException();
        } catch (SQLException ex) {
            FFDCFilter.processException(ex, "com.ibm.ws.rsadapter.jdbc.WSJdbc41ResultSet.getObject", "57", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }
}