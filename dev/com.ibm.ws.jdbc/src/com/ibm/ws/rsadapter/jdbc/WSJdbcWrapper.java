/*******************************************************************************
 * Copyright (c) 2006, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.rsadapter.jdbc;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLRecoverableException;
import java.sql.Wrapper;
import java.util.HashMap;
import java.util.Set; 
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference; 

import javax.sql.CommonDataSource;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.kernel.service.util.SecureAction;
import com.ibm.ws.rsadapter.AdapterUtil;
import com.ibm.ws.rsadapter.DSConfig;
import com.ibm.ws.rsadapter.impl.WSManagedConnectionFactoryImpl; 

/**
 * <p>This class is the superclass of all WebSphere JDBC wrappers:</p>
 * 
 * <ul>
 * <li>DataSource
 * <li>Connection
 * <li>Statement
 * <li>PreparedStatement
 * <li>CallableStatement
 * <li>ResultSet
 * <li>DatabaseMetaData
 * </ul>
 * 
 * <p>It is not used directly, but only through the above subclasses. This class implements
 * function specific to the java.sql.Wrapper interface. Other common wrapper operations are
 * contained in the subclass, WSJdbcObject.</p>
 */
public abstract class WSJdbcWrapper implements InvocationHandler, Wrapper 
{
    final static SecureAction priv = AccessController.doPrivileged(SecureAction.get());
    
    /**
     * Represents the state of a JDBC wrapper.
     */
    public static enum State {
        ACTIVE,
        INACTIVE,
        CLOSED;
    }

    /**
     * Reference to the data source configuration.
     * In order to allow for dynamic updates, this should always be consulted
     * instead of caching information about the configuration.
     * 
     */
    public transient AtomicReference<DSConfig> dsConfig;

    /**
     * A mapping of interface classes to dynamic wrappers.
     */
    final Map<Class<?>, Object> ifcToDynamicWrapper = new HashMap<Class<?>, Object>();

    /**
     * A mapping of dynamic wrappers to underlying implementations.
     * This map is not used for data sources because data sources need to account for dynamic
     * configuration changes. 
     */
    final Map<Object, Object> dynamicWrapperToImpl = new HashMap<Object, Object>();

    /**
     * Managed connection factory.
     */
    protected WSManagedConnectionFactoryImpl mcf; 

    /**
     * Activates a wrapper. This method is a no-op if the wrapper is already active.
     * 
     * @throws SQLException if an error occurs activating the wrapper.
     */
    void activate() throws SQLException {}

    /**
     * Create an ObjectClosedException if exception replacement is enabled, or
     * SQLRecoverableException if exception replacement is disabled.
     * 
     * @param ifc the simple interface name (such as Connection) of the closed wrapper.
     * 
     * @return an exception indicating the object is closed.
     */
    protected final SQLException createClosedException(String ifc) {
        String message = AdapterUtil.getNLSMessage("OBJECT_CLOSED", ifc);
        if (dsConfig.get().heritageReplaceExceptions)
            try {
                return AccessController.doPrivileged((PrivilegedExceptionAction<SQLException>) () -> {
                    @SuppressWarnings("unchecked")
                    Class<? extends SQLException> ObjectClosedException = (Class<? extends SQLException>)
                        mcf.getHelper().dataStoreHelper.getClass().getClassLoader().loadClass("com.ibm.websphere.ce.cm.ObjectClosedException");
                    return ObjectClosedException.getConstructor(String.class).newInstance(message);
                });
            } catch (PrivilegedActionException x) {
                FFDCFilter.processException(x.getCause(), WSJdbcWrapper.class.getName(), "122", this);
                // use the standard exception instead if unable to load
            }
        return new SQLRecoverableException(message, "08003", 0);
    }


    //  This method must return object rather than Wrapper because in the case
    // of data sources, XADataSource and ConnectionPoolDataSource do not implement Wrapper.
    /**
     * @return the underlying JDBC driver's primary implementation object that we wrap.
     */
    protected abstract Object getJDBCImplObject() throws SQLException; 

    /**
     * Locate the underlying JDBC driver's implementation of the specified interface.
     * 
     * @param interfaceClass the interface.
     * 
     * @return the underlying JDBC driver's implementation of the specified interface,
     *         or NULL if none is found.
     * @throws SQLException if an error occurs locating or unwrapping the implementation.
     */
    @SuppressWarnings("unchecked")
    protected <T> T getJDBCImplObject(Class<T> interfaceClass) throws SQLException 
    {
        Object impl = WSJdbcTracer.getImpl(getJDBCImplObject());

        return interfaceClass.isInstance(impl) ?
                        (T) impl : 
                        mcf.jdbcDriverSpecVersion >= 40 && ((Wrapper) impl).isWrapperFor(interfaceClass) ?
                                        ((Wrapper) impl).unwrap(interfaceClass) :
                                        null;
    }


    /**
     * Invokes a method on behalf of a dynamic proxy.
     * 
     * @param proxy the dynamic proxy. Be very careful when performing operations on the
     *            dynamic proxy reference. All operations result in additional invocations of
     *            this method, and could potentially lead to recursive stack overflow errors
     *            if not accounted for.
     * @param method the method being invoked.
     * @param args the parameters to the method.
     * 
     * @return the result of invoking the operation on the underlying object.
     * @throws Throwable if something goes wrong.
     */
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // Special case methods are looked up from a map and invoked.
        // Do not trace because this includes some basic things like hashcode and equals.
        // Important special case methods will take care of tracing themselves.

        WSJdbcProxyMethod methImpl = WSJdbcProxyMethod.getSpecialCase(method);

        if (methImpl != null)
            return methImpl.invoke(this, proxy, method, args);

        // end of special cases

        TraceComponent tc = getTracer();
        if (tc.isEntryEnabled())
            Tr.entry(this, tc, toString(proxy, method), args);

        // Activation should be handled by the wrapper to which we are delegating.
        Object result = null;
        boolean isOperationComplete = false;

        // Invoke on the main wrapper if it has the method.
        Set<Method> vendorMethods = mcf.vendorMethods; 

        if (!vendorMethods.contains(method))
            try 
            {
                // Locate the equivalent method on the main wrapper and invoke it.
                Method wrappedMethod = getClass().getMethod(method.getName(), method.getParameterTypes());
                result = wrappedMethod.invoke(this, args);
                isOperationComplete = true;
            } catch (NoSuchMethodException methX) {
                // No FFDC needed. Method doesn't exist on the main wrapper.
                vendorMethods.add(method); 
            } catch (SecurityException secureX) {
                // No FFDC needed. Method isn't accessible on the main wrapper.
                vendorMethods.add(method); 
            } catch (IllegalAccessException accessX) {
                // No FFDC needed. Method isn't accessible on the main wrapper.
                vendorMethods.add(method); 
            } catch (InvocationTargetException invokeX) {
                // Method exists on the main wrapper, and it failed.
                Throwable x = invokeX.getTargetException();
                FFDCFilter.processException(x, getClass().getName() + ".invoke", "134", this);
                x = x instanceof SQLException ? WSJdbcUtil.mapException(this, (SQLException) x) : x;
                if (tc.isEntryEnabled())
                    Tr.exit(this, tc, toString(proxy, method), x);
                throw x;
            }

        // If the main wrapper does not have the method, invoke it directly on the
        // underlying object.

        if (!isOperationComplete) 
        {
            if (tc.isDebugEnabled())
                Tr.debug(this, tc,
                         "Operation not found on the main wrapper.");

            // Filter out unsafe operations.

            if (!WSJdbcProxyMethod.isSafe(method) // method name
                || !WSJdbcProxyMethod.isSafeReturnType(method.getReturnType()) // return type
                && !WSJdbcProxyMethod.overrideUnsafeReturnType(method)) 
            {
                // Unsafe method. Not permitted. Raise a SQL exception if possible.
                // Otherwise, raise a runtime exception.
                Throwable unsafeX = new SQLFeatureNotSupportedException(
                                AdapterUtil.getNLSMessage("OPERATION_NOT_PERMITTED", method.getName()));

                Throwable x = null;
                for (Class<?> xType : method.getExceptionTypes())
                    if (xType.equals(SQLException.class) || xType.equals(SQLFeatureNotSupportedException.class)) {
                        x = unsafeX;
                        break;
                    }

                if (x == null)
                    x = new RuntimeException(unsafeX);

                if (tc.isEntryEnabled())
                    Tr.exit(this, tc, toString(proxy, method), unsafeX);
                throw x;
            }

            // Invoke the operation directly on the underlying implementation.

            activate(); 

            Object implObject = dynamicWrapperToImpl.get(proxy);

            // A missing entry in the dynamic-wrapper-to-impl map indicates the wrapper is
            // either,
            // 1) Closed because the parent wrapper is closed.
            // 2) No longer valid due to handle association with a managed connection that
            //    doesn't implement the same vendor interface.

            if (implObject == null) {
                String message = AdapterUtil.getNLSMessage("OBJECT_CLOSED", "Wrapper");
                Throwable closedX = new SQLRecoverableException(message, "08003", 0);

                // Raise the SQLException if we can.

                boolean raisesSQLX = false;

                for (Class<?> xClass : method.getExceptionTypes())
                    raisesSQLX |= xClass.equals(SQLException.class);

                // Otherwise use RuntimeException.

                if (!raisesSQLX)
                    closedX = new RuntimeException(closedX);

                if (tc.isEntryEnabled())
                    Tr.exit(this, tc, toString(proxy, method), closedX);
                throw closedX;
            }

            WSJdbcConnection connWrapper = null; 
            // If configured to do so, attempt to enlist in a transaction or start a new one.
            if (this instanceof WSJdbcObject) {
                connWrapper = (WSJdbcConnection) ((WSJdbcObject) this).getConnectionWrapper(); 

                if (connWrapper != null
                    && (dsConfig.get().beginTranForVendorAPIs || WSJdbcProxyMethod.alwaysBeginTranMethods.contains(method.getName())))
                    connWrapper.beginTransactionIfNecessary();
            }

            try {
                // Allow the data source to override in order to account for
                // dynamic configuration changes. 

                result = invokeOperation(implObject, method, args); 

                // If a client information setting was changed, update the managed connection
                // so that we know to reset the client information before pooling the connection.
                if (connWrapper != null && WSJdbcProxyMethod.isClientInfoSetter(method.getName()))
                    connWrapper.managedConn.clientInfoExplicitlySet = true;
            } catch (InvocationTargetException invokeX) {
                Throwable x = invokeX.getTargetException();
                FFDCFilter.processException(x, getClass().getName() + ".invoke", "171", this);
                x = x instanceof SQLException ? WSJdbcUtil.mapException(this, (SQLException) x) : x;
                if (tc.isEntryEnabled())
                    Tr.exit(this, tc, toString(proxy, method), x);
                throw x;
            }
        } // reflection error from invocation attempt on main wrapper

        if (tc.isEntryEnabled())
            Tr.exit(this, tc, toString(proxy, method), result); 
        return result;
    }

    /**
     * Invokes a method on the specified object.
     * The data source must override this method to account for dynamic configuration changes.
     * 
     * @param implObject the instance on which the operation is invoked.
     * @param method the method that is invoked.
     * @param args the parameters to the method.
     * 
     * @throws IllegalAccessException if the method is inaccessible.
     * @throws IllegalArgumentException if the instance does not have the method or
     *             if the method arguments are not appropriate.
     * @throws InvocationTargetException if the method raises a checked exception.
     * @throws SQLException if unable to invoke the method for other reasons.
     * 
     * @return the result of invoking the method.
     */
    Object invokeOperation(Object implObject, Method method, Object[] args)
                    throws IllegalAccessException, IllegalArgumentException, InvocationTargetException,
                    SQLException {
        TraceComponent tc = getTracer();
        if (tc.isDebugEnabled())
            Tr.debug(this, tc,
                     "invoking " + AdapterUtil.toString(implObject) + "." + method.getName());

        return method.invoke(implObject, args);
    }

    /**
     * @return the trace component for the JDBC wrapper subclass.
     */
    protected abstract TraceComponent getTracer(); 

    /**
     * @param runtimeX a RuntimeException which occurred, indicating the wrapper might be closed.
     * 
     * @throws SQLRecoverableException if the wrapper is closed and exception mapping is disabled.
     * 
     * @return the RuntimeException to throw if it isn't.
     */
    protected abstract RuntimeException runtimeXIfNotClosed(RuntimeException runtimeX) throws SQLException;

    /**
     * Finds out if unwrap can be successfully invoked for the specified interface.
     * 
     * @param interfaceClass the requested interface.
     * 
     * @return true if unwrap can be invoked, otherwise false.
     * @throws SQLException if an error occurs.
     */
    public boolean isWrapperFor(Class<?> interfaceClass) throws SQLException {
        TraceComponent tc = getTracer();
        if (tc.isEntryEnabled())
            Tr.entry(this, tc, "isWrapperFor", interfaceClass);

        boolean isWrapper;
        try {
            activate();
            isWrapper = interfaceClass.isInterface()
                        && (interfaceClass.isInstance(this)
                        || ((mcf.jdbcDriverSpecVersion >= 40 && !(this instanceof CommonDataSource)) ?
                                        ((Wrapper) WSJdbcTracer.getImpl(getJDBCImplObject())).isWrapperFor(interfaceClass) :
                                        getJDBCImplObject(interfaceClass) != null));
        } catch (SQLException sqlX) {
            FFDCFilter.processException(
                                        sqlX, getClass().getName() + ".isWrapperFor", "296", this);
            if (tc.isEntryEnabled())
                Tr.exit(this, tc, "isWrapperFor", sqlX);
            throw WSJdbcUtil.mapException(this, sqlX);
        } catch (NullPointerException nullX) {
            // No FFDC code needed; we might be closed.
            throw runtimeXIfNotClosed(nullX);
        } catch (RuntimeException runX) {
            FFDCFilter.processException(
                                        runX, getClass().getName() + ".isWrapperFor", "307", this);
            if (tc.isEntryEnabled())
                Tr.exit(this, tc, "isWrapperFor", runX);
            throw runX;
        } catch (Error err) {
            FFDCFilter.processException(
                                        err, getClass().getName() + ".isWrapperFor", "314", this);
            if (tc.isEntryEnabled())
                Tr.exit(this, tc, "isWrapperFor", err);
            throw err;
        }

        if (tc.isEntryEnabled())
            Tr.exit(this, tc, "isWrapperFor", isWrapper ? Boolean.TRUE : Boolean.FALSE);
        return isWrapper;
    }


    /**
     * Creates a traceable unique identifier for the dynamic proxy without producing a
     * recursive stack overflow error.
     * 
     * @param proxy the dynamic proxy.
     * @param meth the method being invoked on the proxy.
     * 
     * @return the identifier for the trace.
     */
    static String toString(Object proxy, Method meth) {
        return new StringBuilder() 
        .append("Proxy@")
                        .append(Integer.toHexString(System.identityHashCode(proxy)))
                        .append('.')
                        .append(meth.getName())
                        .toString();
    }

    /**
     * Return a class that implements the requested interface, if possible.
     * 
     * @param interfaceClass the interface to unwrap.
     * 
     * @throws SQLException if this wrapper does not wrap any instances of the requested
     *             interface.
     */
    @SuppressWarnings("unchecked")
    public <T> T unwrap(final Class<T> interfaceClass) throws SQLException 
    {
        TraceComponent tc = getTracer();
        if (tc.isEntryEnabled())
            Tr.entry(this, tc, "unwrap", interfaceClass);

        activate(); 

        Object result;

        // Return the current wrapper if it already implements the interface.

        if (interfaceClass.isInstance(this))
            result = this;

        // Return an existing dynamic wrapper if one already exists for the interface.

        else if (ifcToDynamicWrapper.containsKey(interfaceClass))
            result = ifcToDynamicWrapper.get(interfaceClass);

        // Otherwise, see if any of the wrapped objects implement (or themselves wrap)
        // the interface.

        else {
            try {
                if (tc.isDebugEnabled())
                    Tr.debug(this, tc,
                             "No existing wrappers found. Need to create a new wrapper.");

                if (!interfaceClass.isInterface()) {
                    throw new SQLException(AdapterUtil.getNLSMessage("NOT_AN_INTERFACE", interfaceClass.getName()));
                }

                Object implObject = getJDBCImplObject(interfaceClass);

                if (implObject == null) { // Not found
                    throw new SQLException( 
                    AdapterUtil.getNLSMessage("NO_WRAPPED_OBJECT", this, interfaceClass.getName())); 
                }

                if (tc.isDebugEnabled())
                    Tr.debug(this, tc,
                             "Creating a wrapper for:", AdapterUtil.toString(implObject));

                // An implementation object is found. Create a new wrapper for it.
                // And then add it to the map of dynamic wrappers.
                result = AccessController.doPrivileged(new PrivilegedAction<Object>() {
                    public Object run() {
                        return Proxy.newProxyInstance(interfaceClass.getClassLoader(),
                                                      new Class[] { interfaceClass },
                                                      WSJdbcWrapper.this);
                    }
                });

                ifcToDynamicWrapper.put(interfaceClass, result);
                dynamicWrapperToImpl.put(result, implObject);
            }
            catch (SQLException sqlX) {
                FFDCFilter.processException(
                                            sqlX, getClass().getName() + ".unwrap", "441", this);
                if (tc.isEntryEnabled())
                    Tr.exit(this, tc, "unwrap", sqlX);
                throw WSJdbcUtil.mapException(this, sqlX);
            } catch (NullPointerException nullX) {
                // No FFDC code needed; we might be closed.
                throw runtimeXIfNotClosed(nullX);
            } catch (RuntimeException runX) {
                FFDCFilter.processException(
                                            runX, getClass().getName() + ".unwrap", "451", this);
                if (tc.isEntryEnabled())
                    Tr.exit(this, tc, "unwrap", runX);
                throw runX;
            } catch (Error err) {
                FFDCFilter.processException(
                                            err, getClass().getName() + ".unwrap", "458", this);
                if (tc.isEntryEnabled())
                    Tr.exit(this, tc, "unwrap", err);
                throw err;
            }
        }

        if (tc.isEntryEnabled())
            Tr.exit(this, tc, "unwrap", result);
        return (T) result;
    }
}