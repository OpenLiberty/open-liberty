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
package org.test.d43.jdbc;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.Properties;

public class D43Driver extends org.apache.derby.jdbc.AutoloadedDriver implements Driver {
    @Override
    public boolean acceptsURL(String url) throws SQLException {
        String url2 = url.replace("jdbc:d43:", "jdbc:derby:");
        return url2.equals(url) ? false : super.acceptsURL(url2);
    }

    @SuppressWarnings("resource")
    @Override
    public Connection connect(String url, Properties props) throws SQLException {
        String url2 = url.replace("jdbc:d43:", "jdbc:derby:");
        Connection con = url2.equals(url) ? null : super.connect(url2, props);
        if (con != null)
            con = (Connection) Proxy.newProxyInstance(D43Handler.class.getClassLoader(), new Class[] { Connection.class }, new D43Handler(con, null, null));
        return con;
    }

    @Override
    public int getMajorVersion() {
        return 4;
    }

    @Override
    public int getMinorVersion() {
        return 3;
    }
}