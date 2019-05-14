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
package com.ibm.ws.rsadapter.impl;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import javax.resource.ResourceException;

import com.ibm.ejs.cm.logger.TraceWriter;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.rsadapter.AdapterUtil;

/**
 * Helper for the PostgreSQL JDBC driver
 */
public class PostgreSQLHelper extends DatabaseHelper {
    private static final TraceComponent tc = Tr.register(PostgreSQLHelper.class, AdapterUtil.TRACE_GROUP, AdapterUtil.NLS_FILE);
    
    private static final String PROP_DEFAULT_FETCH_SIZE = "DefaultFetchSize";
    private static final String PROP_PREPARE_THRESHOLD = "PrepareThreshold";

    @SuppressWarnings("deprecation")
    private transient com.ibm.ejs.ras.TraceComponent jdbcTC = com.ibm.ejs.ras.Tr.register("com.ibm.ws.postgresql.logwriter", "WAS.database", null);

    private transient PrintWriter jdbcTraceWriter;

    PostgreSQLHelper(WSManagedConnectionFactoryImpl mcf) {
        super(mcf);
    }

    @Override
    public Map<String, Object> cacheVendorConnectionProps(Connection connImpl) throws SQLException {
        try {
            Class<?> PGConnection = connImpl.getClass();
            Map<String, Object> defaultProps = new HashMap<String, Object>(2);
            defaultProps.put(PROP_DEFAULT_FETCH_SIZE, PGConnection.getMethod("getDefaultFetchSize").invoke(connImpl));
            defaultProps.put(PROP_PREPARE_THRESHOLD, PGConnection.getMethod("getPrepareThreshold").invoke(connImpl));
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Got initial PGConnection vendor-specific property defaults: " + defaultProps);
            return defaultProps;
        } catch (RuntimeException x) {
            throw x;
        } catch (Exception x) {
            throw AdapterUtil.toSQLException(x);
        }
    }
    
    @Override
    public boolean doConnectionVendorPropertyReset(Connection connImpl, Map<String, Object> props) throws SQLException {
        try {
            Class<?> PGConnection = connImpl.getClass();
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Resetting PGConnection vendor-specific properties to defaults: " + props);
            PGConnection.getMethod("setDefaultFetchSize", int.class).invoke(connImpl, props.get(PROP_DEFAULT_FETCH_SIZE));
            PGConnection.getMethod("setPrepareThreshold", int.class).invoke(connImpl, props.get(PROP_PREPARE_THRESHOLD));
        } catch (RuntimeException x) {
            throw x;
        } catch (Exception x) {
            throw AdapterUtil.toSQLException(x);
        }
        return true;
    }

    @Override
    public PrintWriter getPrintWriter() throws ResourceException {
        if (jdbcTraceWriter == null)
            jdbcTraceWriter = new PrintWriter(new TraceWriter(jdbcTC), true);
        return jdbcTraceWriter;
    }

    @Override
    public com.ibm.ejs.ras.TraceComponent getTracer() {
        return jdbcTC;
    }

    @Override
    public boolean shouldTraceBeEnabled(WSManagedConnectionFactoryImpl mcf) {
        return TraceComponent.isAnyTracingEnabled() && jdbcTC.isDebugEnabled() && !mcf.loggingEnabled;
    }

    @Override
    public boolean shouldTraceBeDisabled(WSRdbManagedConnectionImpl mc) {
        return TraceComponent.isAnyTracingEnabled() && !jdbcTC.isDebugEnabled() && mc.mcf.loggingEnabled;
    }

    // may want to consider this if we ever support Kerberos with PostgreSQL
//    /**
//     * Indicates if the JDBC driver supports propagating the GSS credential for kerberos
//     * to the JDBC driver by obtaining the connection within Subject.doAs.
//     * 
//     * @return true if version 4.0 or higher, or if we don't know the version (because a connection hasn't been established yet).
//     */
//    @Override
//    public boolean supportsSubjectDoAsForKerberos() {
//        return driverMajorVersion >= 4 // JavaKerberos feature added in version 4.0 of JDBC driver.
//               || driverMajorVersion == 0; // Unknown version, so allow it to be attempted.
//    }
}