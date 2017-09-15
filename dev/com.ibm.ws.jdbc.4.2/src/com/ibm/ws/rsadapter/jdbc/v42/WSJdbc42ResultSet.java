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
package com.ibm.ws.rsadapter.jdbc.v42;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLType;

import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.rsadapter.jdbc.WSJdbcObject;
import com.ibm.ws.rsadapter.jdbc.WSJdbcUtil;
import com.ibm.ws.rsadapter.jdbc.v41.WSJdbc41ResultSet;

public class WSJdbc42ResultSet extends WSJdbc41ResultSet {

    public WSJdbc42ResultSet(ResultSet rsImpl, WSJdbcObject parent) {
        super(rsImpl, parent);
    }

    @Override
    public void updateObject(int columnIndex, Object x, SQLType sqlType) throws SQLException {
        try {
            rsetImpl.updateObject(columnIndex, x, sqlType);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex,
                                        "com.ibm.ws.rsadapter.jdbc.WSJdbc42ResultSet.updateObject(int, Object, SQLType)", "4817", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    @Override
    public void updateObject(int columnIndex, Object x, SQLType sqlType, int scaleOrLength) throws SQLException {
        try {
            rsetImpl.updateObject(columnIndex, x, sqlType, scaleOrLength);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex,
                                        "com.ibm.ws.rsadapter.jdbc.WSJdbc42ResultSet.updateObject(int, Object, SQLType, int)", "4830", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    @Override
    public void updateObject(String columnLabel, Object x, SQLType sqlType) throws SQLException {
        try {
            rsetImpl.updateObject(columnLabel, x, sqlType);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex,
                                        "com.ibm.ws.rsadapter.jdbc.WSJdbc42ResultSet.updateObject(String, Object, SQLType)", "4843", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }

    @Override
    public void updateObject(String columnLabel, Object x, SQLType sqlType, int scaleOrLength) throws SQLException {
        try {
            rsetImpl.updateObject(columnLabel, x, sqlType, scaleOrLength);
        } catch (SQLException ex) {
            FFDCFilter.processException(ex,
                                        "com.ibm.ws.rsadapter.jdbc.WSJdbc42ResultSet.updateObject(String, Object, SQLType, int)", "4856", this);
            throw WSJdbcUtil.mapException(this, ex);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        }
    }
}