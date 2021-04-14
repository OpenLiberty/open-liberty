/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.jdbc.heritage.driver;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;
import javax.sql.XAConnection;
import javax.sql.XADataSource;

public class HDDataSource extends org.apache.derby.jdbc.EmbeddedDataSource implements XADataSource, DataSource {
    private static final long serialVersionUID = 1L;

    String driverType;
    int longDataCacheSize = 10;
    String responseBuffering;
    boolean supportsCatalog = true;
    boolean supportsNetworkTimeout = true;
    boolean supportsReadOnly = true;
    boolean supportsSchema = true;
    boolean supportsTypeMap = true;

    @Override
    public Connection getConnection() throws SQLException {
        return new HDConnection(this, super.getConnection());
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return new HDConnection(this, super.getConnection(username, password));
    }

    @Override
    public XAConnection getXAConnection() throws SQLException {
        return new HDConnection(this, super.getConnection());
    }

    @Override
    public XAConnection getXAConnection(String username, String password) throws SQLException {
        return new HDConnection(this, super.getConnection(username, password));
    }

    public String getDriverType() {
        return driverType;
    }

    public int getLongDataCacheSize() {
        return longDataCacheSize;
    }

    public String getResponseBuffering() {
        return responseBuffering;
    }

    public boolean getSupportsCatalog() {
        return supportsCatalog;
    }

    public boolean getSupportsNetworkTimeout() {
        return supportsNetworkTimeout;
    }

    public boolean getSupportsReadOnly() {
        return supportsReadOnly;
    }

    public boolean getSupportsSchema() {
        return supportsSchema;
    }

    public boolean getSupportsTypeMap() {
        return supportsTypeMap;
    }

    public void setDriverType(String value) {
        driverType = value;
    }

    public void setLongDataCacheSize(int value) {
        longDataCacheSize = value;
    }

    public void setResponseBuffering(String value) {
        responseBuffering = value;
    }

    public void setSupportsCatalog(boolean supports) {
        supportsCatalog = supports;
    }

    public void setSupportsNetworkTimeout(boolean supports) {
        supportsNetworkTimeout = supports;
    }

    public void setSupportsReadOnly(boolean supports) {
        supportsReadOnly = supports;
    }

    public void setSupportsSchema(boolean supports) {
        supportsSchema = supports;
    }

    public void setSupportsTypeMap(boolean supports) {
        supportsTypeMap = supports;
    }
}