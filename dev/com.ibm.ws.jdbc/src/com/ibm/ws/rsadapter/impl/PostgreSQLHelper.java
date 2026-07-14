/*******************************************************************************
 * Copyright (c) 2019, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
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
import javax.transaction.xa.XAResource;

import com.ibm.ejs.cm.logger.TraceWriter;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.resource.ResourceRefInfo;
import com.ibm.ws.rsadapter.AdapterUtil;

/**
 * Helper for the PostgreSQL JDBC driver
 */
public class PostgreSQLHelper extends DatabaseHelper {
    private static final TraceComponent tc = Tr.register(PostgreSQLHelper.class, AdapterUtil.TRACE_GROUP, AdapterUtil.NLS_FILE);
    
    private static final String PROP_DEFAULT_FETCH_SIZE = "DefaultFetchSize";
    private static final String PROP_PREPARE_THRESHOLD = "PrepareThreshold";
    private static final String PROP_AUTOSAVE = "AutoSave";

    @SuppressWarnings("deprecation")
    private transient com.ibm.ejs.ras.TraceComponent jdbcTC = com.ibm.ejs.ras.Tr.register("com.ibm.ws.postgresql.logwriter", "WAS.database", null);

    private Class<?> Autosave;
    private Class<?> LargeObjectManager;
    private Class<?> BaseConnection;

    PostgreSQLHelper(WSManagedConnectionFactoryImpl mcf) throws ClassNotFoundException {
        super(mcf);
    }
    
    /**
     *  Proxy method for org.postgresql.PGConnection.getLargeObjectAPI()
     *  During 19.0.0.7 this method was blocked because it closed the underlying connection on some code paths.
     *  However, we later realized that by overriding this method and constructing the LargeObjectManager using
     *  a proxied instance of this class, we can maintain control of the connection that the LargeObjectAPI uses.
     */
    public Object getLargeObjectAPI(Connection con) throws SQLException {
        try {
            if (LargeObjectManager == null || BaseConnection == null) {
                LargeObjectManager = WSManagedConnectionFactoryImpl.priv.loadClass(mcf.jdbcDriverLoader, "org.postgresql.largeobject.LargeObjectManager");
                BaseConnection = WSManagedConnectionFactoryImpl.priv.loadClass(mcf.jdbcDriverLoader, "org.postgresql.core.BaseConnection");
            }
            
            Object selfProxy = con.unwrap(BaseConnection);
            return LargeObjectManager.getConstructor(BaseConnection).newInstance(selfProxy);
        } catch (Exception e) {
            throw new SQLException(e);
        }
    }

    @Override
    public Map<String, Object> cacheVendorConnectionProps(Connection connImpl) throws SQLException {
        try {
            if (Autosave == null)
                Autosave = WSManagedConnectionFactoryImpl.priv.loadClass(mcf.jdbcDriverLoader, "org.postgresql.jdbc.AutoSave");
            
            Class<?> PGConnection = connImpl.getClass();
            Map<String, Object> defaultProps = new HashMap<String, Object>(2);
            defaultProps.put(PROP_DEFAULT_FETCH_SIZE, PGConnection.getMethod("getDefaultFetchSize").invoke(connImpl));
            defaultProps.put(PROP_PREPARE_THRESHOLD, PGConnection.getMethod("getPrepareThreshold").invoke(connImpl));
            defaultProps.put(PROP_AUTOSAVE, PGConnection.getMethod("getAutosave").invoke(connImpl));
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
            if (Autosave == null)
                Autosave = WSManagedConnectionFactoryImpl.priv.loadClass(mcf.jdbcDriverLoader, "org.postgresql.jdbc.AutoSave");
            
            Class<?> PGConnection = connImpl.getClass();
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Resetting PGConnection vendor-specific properties to defaults: " + props);
            PGConnection.getMethod("setDefaultFetchSize", int.class).invoke(connImpl, props.get(PROP_DEFAULT_FETCH_SIZE));
            PGConnection.getMethod("setPrepareThreshold", int.class).invoke(connImpl, props.get(PROP_PREPARE_THRESHOLD));
            PGConnection.getMethod("setAutosave", Autosave).invoke(connImpl, props.get(PROP_AUTOSAVE));
        } catch (RuntimeException x) {
            throw x;
        } catch (Exception x) {
            throw AdapterUtil.toSQLException(x);
        }
        return true;
    }

    @Override
    public PrintWriter getPrintWriter() throws ResourceException {
        if (genPw == null)
            genPw = new PrintWriter(new TraceWriter(jdbcTC), true);
        return genPw;
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
    
    @Override
    public boolean supportsSubjectDoAsForKerberos() {
        return true;
    }

    /**
     * Returns the XA start flag for the specified branch coupling type.
     *
     * PostgreSQL has no proprietary XA branch coupling flags. LOOSE coupling is the
     * natural behavior of the PostgreSQL XA implementation (each enlistment gets its
     * own independent branch), so BRANCH_COUPLING_LOOSE maps to XAResource.TMNOFLAGS.
     * TIGHT coupling is not supported.
     *
     * @param couplingType branch coupling type
     * @return XAResource.TMNOFLAGS for LOOSE; -1 (not supported) for TIGHT
     */
    @Override
    public int branchCouplingSupported(int couplingType) {
        if (couplingType == ResourceRefInfo.BRANCH_COUPLING_LOOSE)
            return XAResource.TMNOFLAGS;
        // -1 indicates the JDBC driver has no support for this branch coupling type
        return -1;
    }
}