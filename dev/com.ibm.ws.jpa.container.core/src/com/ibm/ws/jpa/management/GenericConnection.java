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

import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.jpa.JPAPuId;

/**
 * Provides a 'generic' Connection implementation for datasources configured
 * in the component context (java:comp/env). <p>
 * 
 * This 'generic' connection implementation is returned when a JPA Persistence
 * Unit has been configured to use a resource reference that is bound into
 * the component environment context namespace (java:comp/env) and the JPA
 * Provider attempts to access the Connection outside the scope of a
 * Java EE componnet. Since every component may provide a different binding
 * for the java:comp name, it is not possible to determine the real connection
 * when outside the scope of a component. <p>
 * 
 * This implementation is intended to satisfy the JPA Provider, so it will
 * successfully add a class transformer when creating the EntityManagerFactory
 * for a PersistenceUnit. Later, when the PersistenceUnit is actually used
 * within the scope of a component, a new EMF will be created (with the real
 * DataSource) and the initial one closed and discarded. <p>
 * 
 * The intent is that the JPA Provider see some level of database support,
 * and likely result in the provider defaulting to its 'generic' support,
 * rather than specific support for DB2, Derby, etc. To provide meaningful
 * results, this implementation has associated GenericDataSource and
 * GenericDatabaseMetaData implementations. <p>
 **/

public class GenericConnection implements Connection
{
    private static final TraceComponent tc = Tr.register(GenericConnection.class,
                                                         JPA_TRACE_GROUP,
                                                         JPA_RESOURCE_BUNDLE_NAME);

    /** Identity of associated persistence unit; used for trace. **/
    private JPAPuId ivPuId;

    /** Name of datasource represented; used for trace. **/
    private String ivDataSourceName = null;

    /** Used to support get/setAutoCommit methods. **/
    private boolean ivAutoCommit = false;

    /** Used to support close/isClosed methods. **/
    private boolean ivIsClosed = false;

    /** Used to support get/setReadOnly methods. **/
    private boolean ivReadOnly = false;

    /** Used to support get/setTransactionIsolation methods. **/
    private int ivTransactionIsolation = Connection.TRANSACTION_NONE;

    /*
     * Constructor.
     * 
     * @param puId identity of associated persistence unit
     * 
     * @param dataSourceName Name of datasource represented
     */
    GenericConnection(JPAPuId puId, String dataSourceName) // d508455
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "<init> : " + puId + ", dataSourceName = " + dataSourceName);

        ivPuId = puId;
        ivDataSourceName = dataSourceName; // d508455

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "<init>");
    }

    /**
     * Overridden to provide meaningful trace output.
     */
    public String toString()
    {
        String identity = Integer.toHexString(System.identityHashCode(this));
        return "GenericConnection@" + identity + "[" + ivPuId + ", " + ivDataSourceName + "]";
    }

    // --------------------------------------------------------------------------
    //
    // java.sql.Connection  -  interface methods
    //
    // --------------------------------------------------------------------------

    public Statement createStatement() throws SQLException
    {
        throw unsupportedUseSQLException();
    }

    public PreparedStatement prepareStatement(String sql) throws SQLException
    {
        throw unsupportedUseSQLException();
    }

    public CallableStatement prepareCall(String sql) throws SQLException
    {
        throw unsupportedUseSQLException();
    }

    public String nativeSQL(String sql) throws SQLException
    {
        throw unsupportedUseSQLException();
    }

    public void setAutoCommit(boolean autoCommit) throws SQLException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "setAutoCommit : " + this + ", " + autoCommit);

        ivAutoCommit = autoCommit;
    }

    public boolean getAutoCommit() throws SQLException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "getAutoCommit : " + this + ", " + ivAutoCommit);

        return ivAutoCommit;
    }

    public void commit() throws SQLException
    {
        throw unsupportedUseSQLException();
    }

    public void rollback() throws SQLException
    {
        throw unsupportedUseSQLException();
    }

    public void close() throws SQLException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "close : " + this);

        ivIsClosed = true;
    }

    public boolean isClosed() throws SQLException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "isClosed : " + this + ", " + ivIsClosed);

        return ivIsClosed;
    }

    public DatabaseMetaData getMetaData() throws SQLException
    {
        DatabaseMetaData result = new GenericDatabaseMetaData(this, ivDataSourceName); // d508455

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "getMetaData : " + this + ", " + result);

        return result;
    }

    public void setReadOnly(boolean readOnly) throws SQLException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "setReadOnly : " + this + ", " + readOnly);

        ivReadOnly = readOnly;
    }

    public boolean isReadOnly() throws SQLException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "isReadOnly : " + this + ", " + ivReadOnly);

        return ivReadOnly;
    }

    public void setCatalog(String catalog) throws SQLException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "setCatalog : " + this + ", " + catalog);
    }

    public String getCatalog() throws SQLException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "getCatalog : " + this + ", null");

        return null;
    }

    public void setTransactionIsolation(int level) throws SQLException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "setTransactionIsolation : " + this + ", " + level);

        ivTransactionIsolation = level;
    }

    public int getTransactionIsolation() throws SQLException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "getTransactionIsolation : " + this + ", " + ivTransactionIsolation);

        return ivTransactionIsolation;
    }

    public SQLWarning getWarnings() throws SQLException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "getWarnings : " + this + ", null");

        return null;
    }

    public void clearWarnings() throws SQLException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "clearWarnings : " + this);
    }

    // --------------------------JDBC 2.0----------------------------------------

    public Statement createStatement(int resultSetType, int resultSetConcurrency)
                    throws SQLException
    {
        throw unsupportedUseSQLException();
    }

    public PreparedStatement prepareStatement(String sql, int resultSetType,
                                              int resultSetConcurrency)
                    throws SQLException
    {
        throw unsupportedUseSQLException();
    }

    public CallableStatement prepareCall(String sql, int resultSetType,
                                         int resultSetConcurrency)
                    throws SQLException
    {
        throw unsupportedUseSQLException();
    }

    public Map<String, Class<?>> getTypeMap() throws SQLException
    {
        throw unsupportedUseSQLException();
    }

    public void setTypeMap(java.util.Map<String, Class<?>> map) throws SQLException
    {
        throw unsupportedUseSQLException();
    }

    // --------------------------JDBC 3.0----------------------------------------

    public void setHoldability(int holdability) throws SQLException
    {
        throw unsupportedUseSQLException();
    }

    public int getHoldability() throws SQLException
    {
        throw unsupportedUseSQLException();
    }

    public Savepoint setSavepoint() throws SQLException
    {
        throw unsupportedUseSQLException();
    }

    public Savepoint setSavepoint(String name) throws SQLException
    {
        throw unsupportedUseSQLException();
    }

    public void rollback(Savepoint savepoint) throws SQLException
    {
        throw unsupportedUseSQLException();
    }

    public void releaseSavepoint(Savepoint savepoint) throws SQLException
    {
        throw unsupportedUseSQLException();
    }

    public Statement createStatement(int resultSetType, int resultSetConcurrency,
                                     int resultSetHoldability) throws SQLException
    {
        throw unsupportedUseSQLException();
    }

    public PreparedStatement prepareStatement(String sql, int resultSetType,
                                              int resultSetConcurrency, int resultSetHoldability)
                    throws SQLException
    {
        throw unsupportedUseSQLException();
    }

    public CallableStatement prepareCall(String sql, int resultSetType,
                                         int resultSetConcurrency,
                                         int resultSetHoldability) throws SQLException
    {
        throw unsupportedUseSQLException();
    }

    public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys)
                    throws SQLException
    {
        throw unsupportedUseSQLException();
    }

    public PreparedStatement prepareStatement(String sql, int columnIndexes[])
                    throws SQLException
    {
        throw unsupportedUseSQLException();
    }

    public PreparedStatement prepareStatement(String sql, String columnNames[])
                    throws SQLException
    {
        throw unsupportedUseSQLException();
    }

    // --------------------------JDBC 4.0----------------------------------------

    /**
     * Constructs an object that implements the Clob interface.
     * 
     * @see java.sql.Connection#createClob
     **/
    // LI3294-25
    public Clob createClob()
                    throws SQLException
    {
        throw unsupportedUseSQLException();
    }

    /**
     * Constructs an object that implements the Blob interface.
     * 
     * @see java.sql.Connection#createBlob
     **/
    // LI3294-25
    public Blob createBlob()
                    throws SQLException
    {
        throw unsupportedUseSQLException();
    }

    /**
     * Constructs an object that implements the NClob interface.
     * 
     * @see java.sql.Connection#createNClob
     **/
    // LI3294-25
    public NClob createNClob()
                    throws SQLException
    {
        throw unsupportedUseSQLException();
    }

    /**
     * Constructs an object that implements the SQLXML interface.
     * 
     * @see java.sql.Connection#createSQLXML
     **/
    // LI3294-25
    public SQLXML createSQLXML()
                    throws SQLException
    {
        throw unsupportedUseSQLException();
    }

    /**
     * Returns true if the connection has not been closed and is still valid.
     * 
     * @see java.sql.Connection#isValid
     **/
    // LI3294-25
    public boolean isValid(int timeout)
                    throws SQLException
    {
        throw unsupportedUseSQLException();
    }

    /**
     * Sets the value of the client info property specified by name to the value
     * specified by value.
     * 
     * @see java.sql.Connection#setClientInfo
     **/
    // LI3294-25
    public void setClientInfo(String name, String value)
                    throws SQLClientInfoException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "setClientInfo : " + this + ", " + name + ", " + value);
    }

    /**
     * Sets the value of the connection's client info properties.
     * 
     * @see java.sql.Connection#setClientInfo
     **/
    // LI3294-25
    public void setClientInfo(Properties properties)
                    throws SQLClientInfoException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "setClientInfo : " + this + ", " + properties);
    }

    /**
     * Returns the value of the client info property specified by name.
     * 
     * @see java.sql.Connection#getClientInfo
     **/
    // LI3294-25
    public String getClientInfo(String name)
                    throws SQLException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "getClientInfo : " + this + ", null");

        return null;
    }

    /**
     * Returns a list containing the name and current value of each client info
     * property supported by the driver.
     * 
     * @see java.sql.Connection#getClientInfo
     **/
    // LI3294-25
    public Properties getClientInfo()
                    throws SQLException
    {
        throw unsupportedUseSQLException();
    }

    /**
     * Factory method for creating Array objects.
     * 
     * @see java.sql.Connection#createArrayOf
     **/
    // LI3294-25
    public Array createArrayOf(String typeName, Object[] elements)
                    throws SQLException
    {
        throw unsupportedUseSQLException();
    }

    /**
     * Factory method for creating Struct objects.
     * 
     * @see java.sql.Connection#createStruct
     **/
    // LI3294-25
    public Struct createStruct(String typeName, Object[] attributes)
                    throws SQLException
    {
        throw unsupportedUseSQLException();
    }

    // --------------------------JDBC 4.1----------------------------------------

    /**
     * Sets the given schema name to access.
     * 
     * @see java.sql.Connection#setSchema
     */
    public void setSchema(String schema)
                    throws SQLException
    {
        throw unsupportedUseSQLException();
    }

    /**
     * Retrieves this Connection object's current schema name.
     * 
     * @see java.sql.Connection#getSchema
     */
    public String getSchema()
                    throws SQLException
    {
        throw unsupportedUseSQLException();
    }

    /**
     * Terminates an open connection.
     * 
     * @see java.sql.Connection#abort
     */
    public void abort(Executor executor)
                    throws SQLException
    {
        throw unsupportedUseSQLException();
    }

    /**
     * Sets the maximum period a Connection or objects created from the
     * Connection will wait for the database to reply to any one request.
     * 
     * @see java.sql.Connection#setNetworkTimeout
     */
    public void setNetworkTimeout(Executor executor, int milliseconds)
                    throws SQLException
    {
        throw unsupportedUseSQLException();
    }

    /**
     * Retrieves the number of milliseconds the driver will wait for a database
     * request to complete. If the limit is exceeded, a SQLException is thrown.
     * 
     * @see java.sql.Connection#getNetworkTimeout
     */
    public int getNetworkTimeout()
                    throws SQLException
    {
        throw unsupportedUseSQLException();
    }

    // --------------------------------------------------------------------------
    //
    // java.sql.Wrapper  -  interface methods
    //
    // --------------------------------------------------------------------------

    /**
     * Returns true if this either implements the interface argument or is
     * directly or indirectly a wrapper for an object that does. <p>
     * 
     * @see javax.sql.Wrapper#isWrapperFor
     **/
    // LI3294-25
    public boolean isWrapperFor(Class<?> iface)
                    throws SQLException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "isWrapperFor : " + ivPuId + ", iface = " + iface);

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

    /**
     * Returns an object that implements the given interface to allow access to
     * non-standard methods, or standard methods not exposed by the proxy. <p>
     * 
     * @see javax.sql.Wrapper#unwrap
     **/
    // LI3294-25
    public <T> T unwrap(Class<T> iface)
                    throws SQLException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "unwrap : PUID = " + ivPuId + " iface = " + iface);

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

    /**
     * Internal method to provide the common SQLException with text explaining
     * why the invoked method is unsupported. <p>
     **/
    private SQLException unsupportedUseSQLException()
    {
        return new SQLException("Unsupported use of GenericConnection.  A GenericConnection is " +
                                "provided during application start when creating an " +
                                "EntityManagerFactory for a persistence unit which has configured " +
                                "one of its datasource to be in the component naming context; " +
                                "java:comp/env. During application start, the component naming " +
                                "context will not exist, and the correct datasource cannot be " +
                                "determined. When the persistence unit is used, the proper " +
                                "datasource and connection will be obtained and used.");
    }
}
