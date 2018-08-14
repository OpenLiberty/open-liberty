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
package jdbc.driver.mini.jse;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.logging.Logger;

import jdbc.driver.mini.MiniConnection;

// A barely usable, fake driver that we include in the application
public class DriverImpl implements Driver {
    @Override
    public boolean acceptsURL(String url) throws SQLException {
        return url.startsWith("jdbc:mini:");
    }

    @Override
    public Connection connect(String url, Properties info) throws SQLException {
        if (!acceptsURL(url))
            return null;

        // parse database name from url: jdbc:mini://localhost:1234/dbname?...
        int start = url.lastIndexOf('/') + 1;
        int end = url.indexOf('?', start);
        if (end < 0)
            end = url.length();
        String databaseName = start > 0 ? url.substring(start, end) : null;

        return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(),
                                                   new Class<?>[] { Connection.class },
                                                   new MiniConnection(databaseName, info.getProperty("user"), info.getProperty("password")));
    }

    @Override
    public int getMajorVersion() {
        return 1;
    }

    @Override
    public int getMinorVersion() {
        return 0;
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public boolean jdbcCompliant() {
        return false;
    }
}