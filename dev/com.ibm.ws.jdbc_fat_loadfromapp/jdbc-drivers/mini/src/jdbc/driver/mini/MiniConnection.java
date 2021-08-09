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
package jdbc.driver.mini;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLNonTransientConnectionException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

// A barely usable, fake connection (and database metadata) that we include in the application.
// It should be just enough implementation to allow a connection to be created with metadata that indicates
// our fake JDBC driver is being used.
public class MiniConnection implements InvocationHandler {
    private boolean closed;
    private Map<String, Object> map = new HashMap<String, Object>();

    public MiniConnection(String databaseName, String user, String password) {
        map.put("AutoCommit", true);
        map.put("Catalog", databaseName);
        map.put("DatabaseProductName", "MiniDatabase");
        map.put("DatabaseProductVersion", "1.0");
        map.put("DriverMajorVersion", 1);
        map.put("DriverMinorVersion", 0);
        map.put("DriverName", "MiniJDBC");
        map.put("DriverVersion", "1.0");
        map.put("Holdability", ResultSet.HOLD_CURSORS_OVER_COMMIT);
        map.put("JDBCMajorVersion", 4);
        map.put("JDBCMinorVersion", 2);
        map.put("MetaData", Proxy.newProxyInstance(DatabaseMetaData.class.getClassLoader(), new Class<?>[] { DatabaseMetaData.class }, this));
        map.put("NetworkTimeout", 0);
        map.put("ReadOnly", true);
        map.put("TransactionIsolation", Connection.TRANSACTION_READ_COMMITTED);
        map.put("TypeMap", Collections.EMPTY_MAP);
        if (user != null) {
            map.put("Schema", user);
            map.put("UserName", user);
        }
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String name = method.getName();

        if ("equals".equals(name))
            return proxy == args[0];

        if ("hashCode".equals(name))
            return System.identityHashCode(proxy);

        if ("toString".equals(name))
            return getClass().getSimpleName() + '@' + Integer.toHexString(System.identityHashCode(proxy));

        if ("isClosed".equals(name) || "isValid".equals(name))
            return closed;

        if ("close".equals(name)) {
            closed = true;
            return null;
        } else if (closed) {
            for (Class<?> c : method.getExceptionTypes())
                if (c.isAssignableFrom(SQLNonTransientConnectionException.class))
                    throw new SQLNonTransientConnectionException("Connection is closed");
            throw new IllegalStateException("Connection is closed");
        }

        if (args == null) {
            if (name.startsWith("get"))
                return map.get(name.substring(3));
            else if (name.startsWith("is"))
                return map.get(name.substring(2));
        } else if (args.length == 1) {
            if (name.startsWith("set")) {
                map.put(name.substring(3), args[0]);
                return null;
            }
        }

        Class<?> returnType = method.getReturnType();
        if (void.class.equals(returnType))
            return null;

        for (Class<?> c : method.getExceptionTypes())
            if (c.isAssignableFrom(SQLFeatureNotSupportedException.class))
                throw new SQLFeatureNotSupportedException();
        throw new UnsupportedOperationException();
    }

    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return iface.isInstance(this);
    }

    public <T> T unwrap(Class<T> iface) throws SQLException {
        if (isWrapperFor(iface))
            return iface.cast(this);
        else
            throw new SQLException("Does not wrap " + iface);
    }
}