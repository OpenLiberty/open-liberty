/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.test.validator.adapter;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLInvalidAuthorizationSpecException;
import java.sql.SQLNonTransientConnectionException;

import javax.resource.spi.SecurityException;

/**
 * Proxy for java.sql.Connection and java.sql.DatabaseMetaData
 */
public class JDBCConnectionImpl implements InvocationHandler {
    private boolean closed;
    private String user;

    JDBCConnectionImpl(String user) {
        this.user = user;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String name = method.getName();

        if ("hashCode".equals(name))
            return System.identityHashCode(proxy);
        if ("toString".equals(name))
            return getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(proxy));

        if ("close".equals(name)) {
            closed = true;
            return null;
        }

        if ("isValid".equals(name))
            if (user.endsWith("5")) {
                // Force an error path where we can test a combination of chained SQLException and ResourceException
                SQLException x = new SQLNonTransientConnectionException("Connection rejected for user names that end in '5'.", "08001", 127);
                x.initCause(new SecurityException("Not accepting user names that end with '5'.", "ERR_SEC_USR5"));
                x.getCause().initCause(new SQLInvalidAuthorizationSpecException("The database is unable to accept user names that include a '5'.", "28000", 0));
                throw x;
            } else
                return !closed;

        if (closed)
            throw new SQLNonTransientConnectionException("Connection is closed.", "08003", 53);

        if ("getCatalog".equals(name))
            return "TestValDB";
        if ("getConnection".equals(name) || "getMetaData".equals(name))
            return proxy;
        if ("getDatabaseProductName".equals(name))
            return "TestValidationEIS";
        if ("getDatabaseProductVersion".equals(name))
            return "33.56.65";
        if ("getDriverName".equals(name))
            return "TestValidationJDBCAdapter";
        if ("getDriverVersion".equals(name))
            return "36.77.85";
        if ("getSchema".equals(name))
            return user.toUpperCase();
        if ("getUserName".equals(name))
            return user;

        for (Class<?> c : method.getExceptionTypes())
            if (SQLException.class.isAssignableFrom(c))
                throw new SQLFeatureNotSupportedException(name);

        throw new UnsupportedOperationException(name);
    }
}
