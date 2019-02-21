/*******************************************************************************
 * Copyright (c) 2017,2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.rest.handler.validator.jdbc;

import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.wsspi.resource.ResourceConfig;
import com.ibm.wsspi.resource.ResourceConfigFactory;
import com.ibm.wsspi.resource.ResourceFactory;
import com.ibm.wsspi.validator.Validator;

@Component(configurationPolicy = ConfigurationPolicy.IGNORE,
           service = { Validator.class },
           property = { "service.vendor=IBM", "com.ibm.wsspi.rest.handler.root=/validator", "com.ibm.wsspi.rest.handler.config.pid=com.ibm.ws.jdbc.dataSource" })
public class DataSourceValidator implements Validator {
    private final static TraceComponent tc = Tr.register(DataSourceValidator.class);

    @Reference
    private ResourceConfigFactory resourceConfigFactory;

    /**
     * @see com.ibm.wsspi.validator.Validator#validate(java.lang.Object, java.util.Map, java.util.Locale)
     */
    @Override
    public LinkedHashMap<String, ?> validate(Object instance, Map<String, Object> props, Locale locale) {
        String user = (String) props.get("user");
        String pass = (String) props.get("password");
        String auth = (String) props.get("auth");
        String authAlias = (String) props.get("authAlias");

        boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "test", user, pass == null ? null : "***", auth, authAlias);

        LinkedHashMap<String, Object> result = new LinkedHashMap<String, Object>();
        try {
            ResourceConfig config = null;
            int authType = "container".equals(auth) ? 0 : "application".equals(auth) ? 1 : -1;
            if (authType >= 0) {
                config = resourceConfigFactory.createResourceConfig(DataSource.class.getName());
                config.setResAuthType(authType);
                if (authAlias != null)
                    config.addLoginProperty("DefaultPrincipalMapping", authAlias); // set provided auth alias
            }

            DataSource ds = (DataSource) ((ResourceFactory) instance).createResource(config);
            Connection con = user == null ? ds.getConnection() : ds.getConnection(user, pass);

            try {
                DatabaseMetaData metadata = con.getMetaData();
                result.put("databaseProductName", metadata.getDatabaseProductName());
                result.put("databaseProductVersion", metadata.getDatabaseProductVersion());
                result.put("jdbcDriverName", metadata.getDriverName());
                result.put("jdbcDriverVersion", metadata.getDriverVersion());

                try {
                    String catalog = con.getCatalog();
                    if (catalog != null && catalog.length() > 0)
                        result.put("catalog", catalog);
                } catch (SQLFeatureNotSupportedException x) {
                }

                try {
                    // Don't need to use reflection here when we don't support java 6 anymore
                    String schema = (String) con.getClass().getMethod("getSchema").invoke(con);
                    if (schema != null && schema.length() > 0)
                        result.put("schema", schema);
                } catch (NoSuchMethodException ignore) {
                } catch (InvocationTargetException x) {
                    Throwable cause = x.getCause();
                    if (cause instanceof SQLFeatureNotSupportedException) {
                        // ignore
                    } else if (cause instanceof IncompatibleClassChangeError) {
                        // ignore
                    } else {
                        throw cause;
                    }
                }

                String userName = metadata.getUserName();
                if (userName != null && userName.length() > 0)
                    result.put("user", userName);

                try {
                    boolean isValid = con.isValid(120); // TODO better ideas for timeout value?
                    if (!isValid)
                        result.put("failure", "FALSE returned by JDBC driver's Connection.isValid operation");
                } catch (SQLFeatureNotSupportedException x) {
                }
            } finally {
                con.close();
            }
        } catch (Throwable x) {
            ArrayList<String> sqlStates = new ArrayList<String>();
            ArrayList<Integer> errorCodes = new ArrayList<Integer>();
            Set<Throwable> causes = new HashSet<Throwable>(); // avoid cycles in exception chain
            for (Throwable cause = x; cause != null && causes.add(cause); cause = cause.getCause()) {
                String sqlState = cause instanceof SQLException ? ((SQLException) cause).getSQLState() : null;
                Integer errorCode = cause instanceof SQLException ? ((SQLException) cause).getErrorCode() : null;
                if (sqlState == null && Integer.valueOf(0).equals(errorCode))
                    errorCode = null; // Omit, because it is unlikely that the database actually returned an error code of 0
                sqlStates.add(sqlState);
                errorCodes.add(errorCode);
            }
            result.put("sqlState", sqlStates);
            result.put("errorCode", errorCodes);
            result.put("failure", x);
        }

        if (trace && tc.isEntryEnabled())
            Tr.exit(this, tc, "test", result);
        return result;
    }
}