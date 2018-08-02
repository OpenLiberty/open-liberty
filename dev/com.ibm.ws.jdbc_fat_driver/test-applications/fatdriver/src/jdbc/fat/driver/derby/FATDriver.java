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

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import javax.sql.DataSource;

public class FATDriver implements Driver {
    private static Driver fatDriver = new FATDriver();

    static {
        try {
            DriverManager.registerDriver(fatDriver);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Connection connect(String url, Properties info) throws SQLException {
        if (!acceptsURL(url))
            return null;

        String loginTimeout = (String) info.remove("loginTimeout");

        int dbNameStart = "jdbc:fatdriver:".length();
        int firstSemicolon = url.indexOf(';', dbNameStart);

        String databaseName = url.substring(dbNameStart, firstSemicolon > 0 ? firstSemicolon : url.length());

        StringBuilder attributes = new StringBuilder();
        if (firstSemicolon > 0)
            attributes.append(url.substring(firstSemicolon + 1));

        for (Map.Entry<?, ?> prop : info.entrySet()) {
            if (attributes.length() > 0)
                attributes.append(';');
            attributes.append(prop.getKey()).append('=').append(prop.getValue());
        }

        System.out.println("[FATDriver]   ->  connect to " + databaseName + " using " + attributes);

        try {
            @SuppressWarnings("unchecked")
            Class<? extends DataSource> EmbeddedDataSource = (Class<? extends DataSource>) Class.forName("org.apache.derby.jdbc.EmbeddedDataSource");
            DataSource ds = EmbeddedDataSource.newInstance();

            EmbeddedDataSource.getMethod("setDatabaseName", String.class).invoke(ds, databaseName);

            if (attributes.length() > 0)
                EmbeddedDataSource.getMethod("setConnectionAttributes", String.class).invoke(ds, attributes.toString());

            if (loginTimeout != null)
                ds.setLoginTimeout(Integer.parseInt(loginTimeout));

            return ds.getConnection();
        } catch (RuntimeException x) {
            throw x;
        } catch (SQLException x) {
            throw x;
        } catch (Exception x) {
            throw new SQLException(x);
        }
    }

    @Override
    public boolean acceptsURL(String url) throws SQLException {
        return url.startsWith("jdbc:fatdriver:");
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
