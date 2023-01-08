/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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
package test.jdbc.heritage.app;

import java.util.Map;

import com.ibm.websphere.rsadapter.JDBCConnectionSpec;

/**
 * Mock implementation of the legacy JDBCConnectionSpec.
 */
@SuppressWarnings("restriction")
public class ConnectionRequest implements JDBCConnectionSpec {
    private String catalog;
    private int holdability;
    private int isolationLevel;
    private Boolean isReadOnly;
    private int networkTimeout;
    private String password;
    private String schema;
    private Map<?, ?> typeMap;
    private String user;

    @Override
    public String getCatalog() {
        return catalog;
    }

    @Override
    public int getHoldability() {
        return holdability;
    }

    @Override
    public Boolean isReadOnly() {
        return isReadOnly;
    }

    @Override
    public int getNetworkTimeout() {
        return networkTimeout;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getSchema() {
        return schema;
    }

    @Override
    public int getTransactionIsolation() {
        return isolationLevel;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Map getTypeMap() {
        return typeMap;
    }

    @Override
    public String getUserName() {
        return user;
    }

    @Override
    public void setCatalog(String value) {
        catalog = value;
    }

    @Override
    public void setHoldability(int value) {
        holdability = value;
    }

    @Override
    public void setNetworkTimeout(int value) {
        networkTimeout = value;
    }

    @Override
    public void setPassword(String value) {
        password = value;
    }

    @Override
    public void setReadOnly(Boolean value) {
        isReadOnly = value;
    }

    @Override
    public void setSchema(String value) {
        schema = value;
    }

    @Override
    public void setTransactionIsolation(int value) {
        isolationLevel = value;
    }

    @Override
    public void setTypeMap(@SuppressWarnings("rawtypes") Map value) {
        typeMap = value;
    }

    @Override
    public void setUserName(String value) {
        user = value;
    }
}
