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
package jdbc.fat.driver.derby;

import java.io.PrintWriter;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Wrapper;
import java.util.logging.Logger;

import javax.sql.CommonDataSource;

/**
 * This class intentionally does not implement ConnectionPoolDataSource.
 * It is here to test that the code that infers data sources classes skips over it.
 */
public class FATConnectionPoolDataSource implements CommonDataSource, Wrapper {
    private boolean autoCreate;
    private String databaseName;
    private int loginTimeout;
    private PrintWriter logWriter;

    public FATConnectionPoolDataSource() throws Exception {}

    public boolean getAutoCreate() throws Exception {
        return autoCreate;
    }

    public String getDatabaseName() throws Exception {
        return databaseName;
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return loginTimeout;
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return logWriter;
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        throw new SQLFeatureNotSupportedException();
    }

    public void setAutoCreate(boolean value) throws Exception {
        autoCreate = value;
    }

    public void setDatabaseName(String value) throws Exception {
        databaseName = value;
    }

    @Override
    public void setLoginTimeout(int value) throws SQLException {
        loginTimeout = value;
    }

    @Override
    public void setLogWriter(PrintWriter value) throws SQLException {
        logWriter = value;
    }

    public void setPassword(String value) throws Exception {}

    public void setUser(String value) throws Exception {}

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        if (isWrapperFor(iface))
            return iface.cast(this);
        else
            throw new SQLException(this + " does not wrap " + iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return CommonDataSource.class.equals(iface) || Wrapper.class.equals(iface);
    }
}