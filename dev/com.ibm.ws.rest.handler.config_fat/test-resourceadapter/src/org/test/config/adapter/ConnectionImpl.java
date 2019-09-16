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
package org.test.config.adapter;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLNonTransientConnectionException;

import javax.resource.NotSupportedException;
import javax.resource.ResourceException;

/**
 * Proxy for java.sql.Connection, java.sql.DatabaseMetaData,
 * javax.resource.cci.Connection, javax.resource.cci.ConnectionMetaData, javax.resource.cci.Interaction.
 */
public class ConnectionImpl implements InvocationHandler {
    private boolean closed;
    private String user;

    ConnectionImpl(String user) {
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
            return !closed;

        if (closed)
            throw new SQLNonTransientConnectionException("Connection is closed.", "08003", 53);

        if ("getCatalog".equals(name))
            return "TestConfigDB";
        if ("getConnection".equals(name) || "getMetaData".equals(name) || "createInteraction".equals(name))
            return proxy;
        if ("getDatabaseProductName".equals(name) || "getEISProductName".equals(name))
            return "TestConfig Data Store, Enterprise Edition";
        if ("getDatabaseProductVersion".equals(name) || "getEISProductVersion".equals(name))
            return "48.55.72";
        if ("getDriverName".equals(name))
            return "TestConfigJDBCAdapter";
        if ("getDriverVersion".equals(name))
            return "65.72.97";
        if ("getSchema".equals(name))
            return user == null ? null : user.toUpperCase();
        if ("getUserName".equals(name))
            return user;

        for (Class<?> c : method.getExceptionTypes())
            if (SQLException.class.isAssignableFrom(c))
                throw new SQLFeatureNotSupportedException(name);
            else if (ResourceException.class.isAssignableFrom(c))
                throw new NotSupportedException(name);

        throw new UnsupportedOperationException(name);
    }
}
