/*******************************************************************************
 * Copyright (c) 2007, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jpa.management;

import static com.ibm.ws.jpa.management.JPAConstants.JPA_RESOURCE_BUNDLE_NAME;
import static com.ibm.ws.jpa.management.JPAConstants.JPA_TRACE_GROUP;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Logger;

import javax.sql.DataSource;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.jpa.JPAPuId;

/**
 * Provides a 'generic' DataSource implementation for datasources configured
 * in the component context (java:comp/env). <p>
 * 
 * This 'generic' datasource implementation is returned when a JPA Persistence
 * Unit has been configured to use a resource reference that is bound into
 * the component environment context namespace (java:comp/env) and the JPA
 * Provider attempts to access the DataSource outside the scope of a
 * Java EE componnet. Since every component may provide a different binding
 * for the java:comp name, it is not possible to determine the real datasource
 * when outside the scope of a component. <p>
 * 
 * This implementation is intended to satisfy the JPA Provider, so it will
 * successfully add a class transformer when creating the EntityManagerFactory
 * for a PersistenceUnit. Later, when the PersistenceUnit is actually used
 * within the scope of a component, a new EMF will be created (with the real
 * DataSource) and the initial one closed and discarded. <p>
 * 
 * The intent is that the JPA Provider will see some level of database support,
 * and likely result in the provider defaulting to its 'generic' support,
 * rather than specific support for DB2, Derby, etc. To provide meaningful
 * results, this implementation has associated GenericConnection and
 * GenericDatabaseMetaData implementations. <p>
 **/
public final class GenericDataSource implements DataSource
{
    private static final TraceComponent tc = Tr.register(GenericDataSource.class,
                                                         JPA_TRACE_GROUP,
                                                         JPA_RESOURCE_BUNDLE_NAME);

    /** Identity of associated persistence unit; used for trace. **/
    private JPAPuId ivPuId;

    /** Name of datasource represented; used for trace. **/
    private String ivDataSourceName = null;

    /** Used to support get/setLoginTimeout methods. **/
    private int ivLoginTimeout = 0;

    /** Used to support get/setLogWriter methods. **/
    private PrintWriter ivLogWriter = null;

    /**
     * Constructor.
     * 
     * @param puId identity of associated persistence unit
     * @param dataSourceName Name of datasource represented
     */
    GenericDataSource(JPAPuId puId, String dataSourceName)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "<init> : " + puId + ", dataSourceName = " + dataSourceName);

        this.ivDataSourceName = dataSourceName;
        this.ivPuId = puId;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "<init>");
    }

    /**
     * Overridden to provide meaningful trace output.
     */
    public String toString()
    {
        String identity = Integer.toHexString(System.identityHashCode(this));
        return "GenericDataSource@" + identity + "[" + ivPuId + ", " + ivDataSourceName + "]";
    }

    // --------------------------------------------------------------------------
    //
    // javax.sql.DataSource  -  interface methods
    //
    // --------------------------------------------------------------------------

    /**
     * Will always return a GenericConnection. <p>
     * 
     * @see javax.sql.DataSource#getConnection()
     */
    public Connection getConnection() throws SQLException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "getConnection : " + ivPuId);

        Connection result = new GenericConnection(ivPuId, ivDataSourceName);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "getConnection : " + result);

        return result;

    }

    /**
     * Will always return a GenericConnection. <p>
     * 
     * @see javax.sql.DataSource#getConnection(String username, String password)
     */
    public Connection getConnection(String username, String password)
                    throws SQLException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "getConnection : " + ivPuId + ", username = " + username);

        Connection result = new GenericConnection(ivPuId, ivDataSourceName);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "getConnection : " + result);

        return result;

    }

    // --------------------------------------------------------------------------
    //
    // java.sql.CommonDataSource  -  interface methods
    //
    // --------------------------------------------------------------------------

    public PrintWriter getLogWriter() throws SQLException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "getLogWriter : " + ivPuId + ", " + ivLogWriter);

        return ivLogWriter;
    }

    public void setLogWriter(PrintWriter out) throws SQLException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "setLogWriter : " + ivPuId + ", " + out);

        ivLogWriter = out;
    }

    public void setLoginTimeout(int seconds) throws SQLException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "setLoginTimeout : " + ivPuId + ", " + seconds);

        ivLoginTimeout = seconds;
    }

    public int getLoginTimeout() throws SQLException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "getLoginTimeout : " + ivPuId + ", " + ivLoginTimeout);

        return ivLoginTimeout;
    }

    public Logger getParentLogger()
    {
        return Logger.getLogger("");
    }

    // --------------------------------------------------------------------------
    //
    // java.sql.Wrapper  -  interface methods
    //
    // --------------------------------------------------------------------------

    public boolean isWrapperFor(Class<?> iface)
                    throws SQLException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "isWrapperFor : " + ivPuId + ", " + iface);

        boolean result = false;

        if (iface != null &&
            iface.isInstance(this))
        {
            result = true;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "isWrapperFor : " + result);

        return result;
    }

    public <T> T unwrap(Class<T> iface)
                    throws SQLException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "unwrap : " + ivPuId + ", " + iface);

        T result = null;

        try
        {
            result = iface.cast(this);
        } catch (Throwable ex)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.exit(tc, "unwrap : SQLException: " + getClass().getName() +
                            " does not implement " + iface);
            throw new SQLException(getClass().getName() + " does not implement " +
                                   iface);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "unwrap : " + result);

        return result;
    }

}
