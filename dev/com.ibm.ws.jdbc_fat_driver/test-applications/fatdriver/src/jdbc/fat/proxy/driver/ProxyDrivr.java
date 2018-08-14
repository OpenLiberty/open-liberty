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
package jdbc.fat.proxy.driver;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.logging.Logger;

// 'e' is intentionally omitted from this class name to prevent Liberty from discovering the
// corresponding data source classes by swapping out "Driver" in the class name
public class ProxyDrivr implements Driver {
    @Override
    public Connection connect(String url, Properties info) throws SQLException {
        if (!acceptsURL(url))
            return null;

        info.setProperty("DatabaseProductName", "Proxy Database");
        info.setProperty("DatabaseProductVersion", "1.0.0");
        info.setProperty("DriverName", "Proxy Driver");
        info.setProperty("DriverVersion", "1.0");
        info.setProperty("DatabaseMajorVersion", "1");
        info.setProperty("DatabaseMinorVersion", "0");
        info.setProperty("JDBCMajorVersion", "4");
        info.setProperty("JDBCMinorVersion", "2");
        info.setProperty("URL", url);
        String user = info.getProperty("user");
        if (user != null)
            info.setProperty("UserName", user);

        return (Connection) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] { Connection.class }, new Handler(Connection.class, info));
    }

    @Override
    public boolean acceptsURL(String url) throws SQLException {
        return url.startsWith("jdbc:proxydriver:");
    }

    @Override
    public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
        throw new SQLFeatureNotSupportedException();
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
    public boolean jdbcCompliant() {
        return false;
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        throw new SQLFeatureNotSupportedException();
    }
}
