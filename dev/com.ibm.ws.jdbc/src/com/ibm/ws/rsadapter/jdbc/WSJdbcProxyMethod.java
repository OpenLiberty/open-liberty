/*******************************************************************************
 * Copyright (c) 2001, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.rsadapter.jdbc;

import java.lang.reflect.Method;
import java.sql.*;
import java.util.Arrays; 
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.sql.*;
import javax.transaction.xa.XAResource;

import com.ibm.ws.ffdc.FFDCFilter;

// JavaDoc 
/**
 * <p>WSJdbcProxyMethod contains special case methods for dynamically created JDBC wrappers
 * as well as utility methods for determining whether methods on dynamically created JDBC
 * wrappers are safe to run.</p>
 */
public abstract class WSJdbcProxyMethod
{
    /**
     * A list of names of unsafe operations on dynamic wrappers
     */
    static final Set<String> unsafeMethods; 
    static final Set<String> overrideUnsafeReturnTypeMethods; 

    /**
     * Methods for which beginTransactionIfNecessary must always be invoked,
     * regardless of the beginTranForVendorAPIs setting
     */
    static final Set<String> alwaysBeginTranMethods = new HashSet<String>();

    static final Map<Method, WSJdbcProxyMethod> specialCaseMethods; 

    /**
     * A list of names of client info setter methods. If any of these methods is invoked,
     * then the client information of the connection must be reset to the default before
     * returning the connection to the connection pool.
     * 
     */
    private static final Set<String> clientInfoSetterMethods = new HashSet<String>(Arrays.asList(
                                                                                                 "setAccountingInfo", // com.ddtek.jdbc.extensions.ExtConnection
                                                                                                                      // com.ddtek.jdbcx.sqlserver.SQLServerDataSource
                                                                                                 "setApplicationName", // com.ddtek.jdbc.extensions.ExtConnection
                                                                                                                       // com.ddtek.jdbcx.sqlserver.SQLServerDataSource
                                                                                                 "setAttribute", // com.ddtek.jdbc.extensions.ExtConnection 
                                                                                                 "setClientAccountingInfo", // com.ddtek.jdbc.extensions.ExtConnection
                                                                                                 "setClientAccountingInformation", // com.ibm.db2.jcc.DB2[ConnectionPool/XA]DataSource
                                                                                                 "setClientAcctInfo", // com.ddtek.jdbcx.sqlserver.SQLServerDataSource
                                                                                                 "setClientApplicationInformation", // com.ibm.db2.jcc.DB2[ConnectionPool/XA]DataSource
                                                                                                 "setClientApplicationName", // com.ddtek.jdbc.extensions.ExtConnection
                                                                                                 "setClientAppName", // com.ddtek.jdbcx.sqlserver.SQLServerDataSource
                                                                                                 "setClientHostName", // com.ddtek.jdbc.extensions.ExtConnection
                                                                                                                      // com.ddtek.jdbcx.sqlserver.SQLServerDataSource
                                                                                                 "setClientUser", // com.ddtek.jdbc.extensions.ExtConnection
                                                                                                                  // com.ibm.db2.jcc.DB2[ConnectionPool/XA]DataSource
                                                                                                 "setClientWorkstation" // com.ibm.db2.jcc.DB2[ConnectionPool/XA]DataSource
    ));


    static
    {
        // Initialize the list of unsafe method names.
        // Many unsafe methods are already covered because they return JDBC APIs:
        //   getConnection, getPooledConnection, getXAConnection, getXAResource
        // It is ok to add JDBC API methods to this list to block the vendor 
        //   versions of the method.  The API versions will not be blocked.

        unsafeMethods = new HashSet<String>(47);

        unsafeMethods.add("_getPC");
        // oracle.jdbc.OracleConnection._getPC()
        // oracle.jdbc.internal.OracleConnection._getPC()
        unsafeMethods.add("abort");
        // oracle.jdbc.OracleConnection.abort()
        // oracle.jdbc.internal.OracleConnection.abort()
        unsafeMethods.add("abortConnection");
        unsafeMethods.add("applyConnectionAttributes");
        //  oracle.jdbc.OracleConnection.applyConnectionAttributes(Properties)
        //  oracle.jdbc.internal.OracleConnection.applyConnectionAttributes(Properties)
        unsafeMethods.add("attachServerConnection");
        //  oracle.jdbc.OracleConnection.attachServerConnection
        //  oracle.jdbc.internal.OracleConnection.attachServerConnection
        unsafeMethods.add("beginRequest");
        // oracle.jdbc.OracleConnection.beginRequest()
        // oracle.jdbc.internal.OracleConnection.beginRequest()
        unsafeMethods.add("cancel");
        // oracle.jdbc.OracleConnection.cancel()
        // oracle.jdbc.internal.OracleConnection.cancel()
        unsafeMethods.add("cleanupAndClose");
        // oracle.jdbc.internal.OracleConnection.cleanupAndClose(boolean)
        unsafeMethods.add("close");
        //  oracle.jdbc.OracleConnection.close(Properties) 
        //  oracle.jdbc.OracleConnection.close(int)
        //  oracle.jdbc.internal.OracleConnection.close(Properties) 
        //  oracle.jdbc.internal.OracleConnection.close(int)
        unsafeMethods.add("closeInternal");
        // oracle.jdbc.internal.OracleConnection.closeInternal(boolean)
        unsafeMethods.add("closeLogicalConnection");
        // oracle.jdbc.internal.OracleConnection.closeLogicalConnection()
        unsafeMethods.add("closeWithKey");
        //  oracle.jdbc.OracleStatement.closeWithKey(String)
        //  oracle.jdbc.internal.OracleStatement.closeWithKey(String)
        //  oracle.jdbc.OraclePreparedStatement.closeWithKey(String)
        //  oracle.jdbc.internal.OraclePreparedStatement.closeWithKey(String)
        //  oracle.jdbc.OracleCallableStatement.closeWithKey(String)
        //  oracle.jdbc.internal.OracleCallableStatement.closeWithKey(String)
        unsafeMethods.add("commit");
        //  oracle.jdbc.OracleConnection.commit(EnumSet)
        //  oracle.jdbc.internal.OracleConnection.commit(EnumSet)
        unsafeMethods.add("createConnectionBuilder");
        // oracle.jdbc.pool.OracleConnectionPoolDataSource.createConnectionBuilder
        // oracle.jdbc.pool.OracleDataSource.createConnectionBuilder
        // oracle.jdbc.xa.client.OracleXADataSource.createConnectionBuilder
        // oracle.jdbc.xa.OracleXADataSource.createConnectionBuilder
        unsafeMethods.add("createPooledConnectionBuilder");
        // oracle.jdbc.pool.OracleConnectionPoolDataSource.createPooledConnectionBuilder
        // oracle.jdbc.xa.client.OracleXADataSource.createPooledConnectionBuilder
        // oracle.jdbc.xa.OracleXADataSource.createPooledConnectionBuilder
        unsafeMethods.add("createXAConnectionBuilder");
        // oracle.jdbc.xa.client.OracleXADataSource.createXAConnectionBuilder
        // oracle.jdbc.xa.OracleXADataSource.createXAConnectionBuilder
        unsafeMethods.add("detachServerConnection");
        // oracle.jdbc.OracleConnection.detachServerConnection(String)
        // oracle.jdbc.internal.OracleConnection.detachServerConnection(String)
        unsafeMethods.add("doClose");
        unsafeMethods.add("endRequest");
        //  oracle.jdbc.OracleConnection.endRequest()
        //  oracle.jdbc.internal.OracleConnection.endRequest()
        //these methods are already covered because they return JDBC APIs:
        //   getConnection, getPooledConnection, getXAConnection, getXAResource
        unsafeMethods.add("getDB2Object");
        unsafeMethods.add("getLogicalConnection");
        //  oracle.jdbc.internal.OracleConnection.getLogicalConnection(OraclePooledConnection, boolean)
        unsafeMethods.add("getObjectInstance");
        unsafeMethods.add("getPassword");
        unsafeMethods.add("getPhysicalConnection");
        //  oracle.jdbc.internal.OracleConnection.getPhysicalConnection()
        unsafeMethods.add("getWrapper");
        unsafeMethods.add("getXAResource");
        //  oracle.jdbc.internal.OracleConnection.getXAResource()
        unsafeMethods.add("init");
        // unsafeMethods.add("openProxySession");   
        //  oracle.jdbc.OracleConnection.openProxySession(int, Properties)
        //  oracle.jdbc.internal.OracleConnection.openProxySession(int, Properties)
        unsafeMethods.add("oracleReleaseSavepoint");
        // oracle.jdbc.OracleConnection.oracleReleaseSavepoint(OracleSavepoint)
        // oracle.jdbc.internal.OracleConnection.oracleReleaseSavepoint(OracleSavepoint)
        unsafeMethods.add("oracleRollback");
        // oracle.jdbc.OracleConnection.oracleRollback(OracleSavepoint)
        // oracle.jdbc.internal.OracleConnection.oracleRollback(OracleSavepoint)
        unsafeMethods.add("oracleSetSavepoint");
        //  oracle.jdbc.OracleConnection.oracleSetSavepoint()
        //  oracle.jdbc.OracleConnection.oracleSetSavepoint(String)
        //  oracle.jdbc.internal.OracleConnection.oracleSetSavepoint()
        //  oracle.jdbc.internal.OracleConnection.oracleSetSavepoint(String)
        //unsafeMethods.add("physicalConnectionWithin"); 
        //  oracle.jdbc.OracleConnection.physicalConnectionWithin()
        //  oracle.jdbc.internal.OracleConnection.physicalConnectionWithin()
        unsafeMethods.add("prepareCallWithKey");
        //  oracle.jdbc.OracleConnection.prepareCallWithKey(String)
        //  oracle.jdbc.internal.OracleConnection.prepareCallWithKey(String)
        unsafeMethods.add("prepareStatementWithKey");
        //  oracle.jdbc.OracleConnection.prepareStatementWithKey(String)
        //  oracle.jdbc.internal.OracleConnection.prepareStatementWithKey(String)
        unsafeMethods.add("realObject");
        unsafeMethods.add("resetUser"); 
        unsafeMethods.add("setAutoClose");
        // oracle.jdbc.OracleConnection.setAutoClose(boolean)
        // oracle.jdbc.internal.OracleConnection.setAutoClose(boolean)
        unsafeMethods.add("setCurrentUser"); 
        unsafeMethods.add("setSafelyClosed");
        //oracle.jdbc.internal.OracleConnection.setSafelyClosed(boolean)
        unsafeMethods.add("setShardingKeyIfValid");
        //  oracle.jdbc.OracleConnection.setShardingKeyIfValid
        //  oracle.jdbc.internal.OracleConnection.setShardingKeyIfValid
        unsafeMethods.add("setShardingKey");
        //  oracle.jdbc.OracleConnection.setShardingKey
        //  oracle.jdbc.internal.OracleConnection.setShardingKey
        unsafeMethods.add("setUsingXAFlag");
        //  oracle.jdbc.OracleConnection.setUsingXAFlag(boolean)
        //  oracle.jdbc.internal.OracleConnection.setUsingXAFlag(boolean)
        unsafeMethods.add("setWrapper"); 
        //  oracle.jdbc.OracleConnection.setWrapper(OracleConnection)
        //  oracle.jdbc.internal.OracleConnection.setWrapper(OracleConnection)
        unsafeMethods.add("setXAErrorFlag");
        //  oracle.jdbc.OracleConnection.setXAErrorFlag(boolean)
        //  oracle.jdbc.internal.OracleConnection.setXAErrorFlag(boolean)    
        unsafeMethods.add("unwrap"); 
        //  oracle.jdbc.OracleConnectin.unwrap()
        //  oracle.jdbc.internal.OracleConnectin.unwrap()

        // "setQueryTimeout" and "setLongDataCacheSize" are not permitted on the data source
        // because they complicate default values for statement pooling. 
        // The full package is listed to avoid disabling ExtStatement.setLongDataCacheSize
        unsafeMethods.add("com.ddtek.jdbc.extensions.ExtDataSource.setLongDataCacheSize");
        unsafeMethods.add("com.ddtek.jdbc.extensions.ExtDataSource.setQueryTimeout"); 
        unsafeMethods.add("com.microsoft.sqlserver.jdbc.ISQLServerDataSource.setResponseBuffering"); 

        overrideUnsafeReturnTypeMethods = new HashSet<String>();
        overrideUnsafeReturnTypeMethods.add("getCursor"); // implemented on WSJdbcCallableStatement
        overrideUnsafeReturnTypeMethods.add("getReturnResultSet"); // implemented on WSJdbcPreparedStatement
        overrideUnsafeReturnTypeMethods.add("getSingletonResultSet"); // implemented on WSJdbcPreparedStatement
        overrideUnsafeReturnTypeMethods.add("physicalConnectionWithin");
        overrideUnsafeReturnTypeMethods.add("prepareSQLJCall"); // implemented on WSJdbcConnection
        overrideUnsafeReturnTypeMethods.add("prepareSQLJStatement"); // implemented on WSJdbcConnection

        alwaysBeginTranMethods.add("executeBatch");
        alwaysBeginTranMethods.add("prepareSQLJCall");
        alwaysBeginTranMethods.add("prepareSQLJStatement");

        // Initialize the list of special case method implementations.

        specialCaseMethods = new HashMap<Method, WSJdbcProxyMethod>(); 

        try {
            specialCaseMethods.put(
                                   Object.class.getMethod("hashCode", (Class[]) null), new WSJdbcProxyMethod() {
                                       @Override
                                       public Object invoke(WSJdbcWrapper wrapper, Object proxy, Method method, Object[] args) throws Throwable {
                                           return System.identityHashCode(proxy);
                                       }
                                   }
                            );

            specialCaseMethods.put(
                                   Object.class.getMethod("equals", new Class[] { Object.class }), new WSJdbcProxyMethod() {
                                       @Override
                                       public Object invoke(WSJdbcWrapper wrapper, Object proxy, Method method, Object[] args) throws Throwable {
                                           return proxy == args[0] ? Boolean.TRUE : Boolean.FALSE;
                                       }
                                   });

            specialCaseMethods.put(
                                   Object.class.getMethod("toString", (Class[]) null), new WSJdbcProxyMethod() {
                                       @Override
                                       public Object invoke(WSJdbcWrapper wrapper, Object proxy, Method method, Object[] args) throws Throwable {
                                           return new StringBuilder("Proxy@") 
                                           .append(Integer.toHexString(System.identityHashCode(proxy)))
                                                           .toString();
                                       }
                                   });

            // The method "getClass" is not invoked on invocation handlers. 
        } catch (NoSuchMethodException methX) {
            FFDCFilter.processException(methX, WSJdbcProxyMethod.class.getName() + ".<init>", "152");
            throw new ExceptionInInitializerError(methX);
        }
    }

    /**
     * Determines if the specified method requires special handling, and if so, returns a
     * WSJdbcProxyMethod instance accounting for the special case.
     * 
     * @param meth a method.
     * 
     * @return the special case method, or, if not a special case, NULL.
     */
    public static final WSJdbcProxyMethod getSpecialCase(Method meth) {
        return specialCaseMethods.get(meth); 
    }

    /**
     * Invokes or implements a method on behalf of a dynamic proxy for a JDBC wrapper.
     * Implementations of the interface should abide by the JavaDoc API for the
     * InvocationHandler.invoke method.
     * 
     * @param wrapper the wrapper.
     * @param proxy the dynamic proxy.
     * @param method the method to invoke.
     * @param args the paramters to the method.
     * 
     * @return the return value of the method.
     * @throws Throwable if an error occurs.
     */
    public abstract Object invoke(WSJdbcWrapper wrapper, Object proxy, Method method, Object[] args) throws Throwable;

    /**
     * Determines if the specified method is a setter method for client information.
     * For example, setClientApplicationName from com.ddtek.jdbc.extensions.ExtConnection.
     * 
     * @param methodName name of a method, not including the package and class name.
     * 
     * @return TRUE if the method is used to set client information, otherwise FALSE.
     * 
     */
    public static final boolean isClientInfoSetter(String methodName) {
        return clientInfoSetterMethods.contains(methodName);
    }

    /**
     * Determines whether it is safe, based on the method name, to invoke the method on
     * behalf of a dynamic proxy for a JDBC wrapper.
     * 
     * @param meth the method.
     * 
     * @return TRUE if safe. FALSE if not safe.
     */
    public static final boolean isSafe(Method meth) {
        String fullyQualifiedName = new StringBuilder().append(meth.getDeclaringClass().getName()).append('.').append(meth.getName()).toString();

        return !unsafeMethods.contains(meth.getName()) && !unsafeMethods.contains(fullyQualifiedName);
    }

    /**
     * PM07473 697453 begin
     * Determines whether unsafe return types could be overridden on the method name
     * behalf of a dynamic proxy for a JDBC wrapper.
     * 
     * @param meth the method.
     * 
     * @return TRUE if it could be overridden. FALSE if not.
     */
    static boolean overrideUnsafeReturnType(Method meth) {
        return overrideUnsafeReturnTypeMethods.contains(meth.getName());
    }

    /**
     * Determine whether it is safe to return the result of an operation performed by the
     * underlying implementation from a dynamic JDBC wrapper.
     * 
     * @param type the Java type of a value returned by a method.
     * 
     * @return TRUE if safe. FALSE if not safe.
     */
    static boolean isSafeReturnType(Class<?> type) { 
        return !ConnectionPoolDataSource.class.isAssignableFrom(type)
               && !CommonDataSource.class.isAssignableFrom(type) 
               && !XADataSource.class.isAssignableFrom(type)
               && !DataSource.class.isAssignableFrom(type)
               && !PooledConnection.class.isAssignableFrom(type) // implies XAConnection
               && !Connection.class.isAssignableFrom(type)
               && !DatabaseMetaData.class.isAssignableFrom(type)
               && !Statement.class.isAssignableFrom(type) // implies prepared & callable
               && !ResultSet.class.isAssignableFrom(type)
               && !XAResource.class.isAssignableFrom(type);
    }
}
