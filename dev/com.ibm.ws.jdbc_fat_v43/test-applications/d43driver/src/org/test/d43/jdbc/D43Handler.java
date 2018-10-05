/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.test.d43.jdbc;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.ShardingKey;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import javax.sql.PooledConnection;
import javax.transaction.Status;
import javax.transaction.TransactionManager;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;

import com.ibm.tx.jta.TransactionManagerFactory;

public class D43Handler implements InvocationHandler, Supplier<AtomicInteger[]> {
    // for tracking Connection.beginRequest/endRequest
    private final AtomicInteger beginRequests = new AtomicInteger();
    private final AtomicInteger endRequests = new AtomicInteger();

    private final D43CommonDataSource commonDataSource;

    private final Object instance;

    private boolean isAborted;
    private boolean isClosed;

    private final D43Handler parent;

    private ShardingKey shardingKey;
    private ShardingKey superShardingKey;

    private final static TransactionManager tm = TransactionManagerFactory.getTransactionManager();

    D43Handler(Object instance, D43Handler parent, D43CommonDataSource commonDataSource) {
        this.commonDataSource = commonDataSource;
        this.instance = instance;
        this.parent = parent;
    }

    D43Handler(Object instance, D43Handler parent, D43CommonDataSource commonDataSource, ShardingKey shardingKey, ShardingKey superShardingKey) {
        this.commonDataSource = commonDataSource;
        this.instance = instance;
        this.parent = parent;
        this.shardingKey = shardingKey;
        this.superShardingKey = superShardingKey;
    }

    // Accessible via wrapper pattern for obtaining the count of Connection.beginRequest/endRequest
    // Usage: requestCounts = (AtomicInteger[]) con.unwrap(Supplier).get();
    @Override
    public AtomicInteger[] get() {
        return new AtomicInteger[] { beginRequests, endRequests };
    }

    private D43Handler getConnectionHandler() {
        D43Handler c = null;
        for (D43Handler h = this; h != null; h = h.parent)
            if (h.instance instanceof PooledConnection)
                return h;
            else if (h.instance instanceof Connection)
                c = h;
        return c;
    }

    @SuppressWarnings("restriction")
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String methodName = method.getName();
        Class<?> returnType = method.getReturnType();
        D43Handler connectionHandler = getConnectionHandler();
        if ("hashCode".equals(methodName))
            return System.identityHashCode(proxy);
        if ("toString".equals(methodName))
            return "D43Handler@" + Integer.toHexString(System.identityHashCode(proxy)) + " for "
                   + instance.getClass().getName() + '@' + Integer.toHexString(System.identityHashCode(instance));

        // Allow unwrap(Supplier) as a way to access request counts
        if (instance instanceof Connection && args != null && args.length == 1 && Supplier.class.equals(args[0])) {
            if ("isWrapperFor".equals(methodName))
                return true;
            if ("unwrap".equals(methodName))
                return this;
        }

        if ("abort".equals(methodName)) {
            isAborted = true;
            isClosed = true;
            // also mark PooledConnection/XAConnection aborted
            if (connectionHandler != null)
                connectionHandler.isAborted = true;
        } else if ("close".equals(methodName)) {
            isClosed = true;
        } else if (!"isClosed".equals(methodName) && !"isValid".equals(methodName) && !"endRequest".equals(methodName)
                   && method.getDeclaringClass().getName().startsWith("java.sql")) {
            if (isAborted)
                throw new AbortedException(method.getDeclaringClass());
            if (isClosed)
                throw new ClosedException(method.getDeclaringClass());
        }

        if ("beginRequest".equals(methodName)) {
            beginRequests.incrementAndGet();
            ((Connection) instance).beginRequest();
            return null;
        }
        // this works around Derby raising IndexOutOfBoundsException on XAResource.commit/rollback after an abort
        if (("commit".equals(methodName) || "end".equals(methodName) || "prepare".equals(methodName))
            && instance instanceof XAResource
            && connectionHandler != null && connectionHandler.isAborted) {
            XAException xax = new XAException("Connection aborted");
            xax.errorCode = XAException.XA_RBOTHER;
            throw xax;
        }
        if ("endRequest".equals(methodName)) {
            if (isClosed && !isAborted)
                throw new ClosedException(method.getDeclaringClass());
            if (tm.getStatus() == Status.STATUS_ACTIVE ||
                ((org.apache.derby.iapi.jdbc.EngineConnection) instance).isInGlobalTransaction())
                throw new SQLException("Transaction is still active");
            ((Connection) instance).endRequest();
            endRequests.incrementAndGet();

            // The expectation for endRequest on abort paths is unclear.
            // It seems wrong for endRequest to be sent prior to abort because additional operations
            // for the request might subsequently come in on the main thread of execution (the thread
            // that isn't being used to invoke abort).
            // It also seems wrong to avoid sending endRequest at all when a connection is aborted.
            // That leaves sending endRequest after the abort operation, although there is no way of
            // knowing when the JDBC driver has actually completed the asynchronous abort processing.
            // To complicate matters further, a JDBC driver might decide to raise an exception if
            // endRequest is invoked on a previously aborted connection. Simulate that here, while
            // still having incremented the endRequest counter so that test can also compare that value.
            if (isAborted)
                throw new AbortedException(method.getDeclaringClass());
            return null;
        }
        // Abuse getClientInfo to expose the sharding key value to the application so that tests can compare it
        if ("getClientInfo".equals(methodName) && args != null && args.length == 1 && "SHARDING_KEY".equals(args[0]))
            return shardingKey != null ? shardingKey.toString() //
                            : commonDataSource != null && commonDataSource.defaultShardingKey != null ? commonDataSource.defaultShardingKey.toString() //
                                            : null;
        if ("getClientInfo".equals(methodName) && args != null && args.length == 1 && "SUPER_SHARDING_KEY".equals(args[0]))
            return superShardingKey != null ? superShardingKey.toString() //
                            : commonDataSource != null && commonDataSource.defaultSuperShardingKey != null ? commonDataSource.defaultSuperShardingKey.toString() //
                                            : null;
        if ("getJDBCMajorVersion".equals(methodName))
            return 4;
        if ("getJDBCMinorVersion".equals(methodName))
            return 3;
        // this works around Derby raising IndexOutOfBoundsException on XAResource.rollback after an abort
        if (("rollback".equals(methodName))
            && instance instanceof XAResource
            && connectionHandler != null && connectionHandler.isAborted) {
            return null;
        }
        if ("setShardingKey".equals(methodName)) {
            shardingKey = (ShardingKey) args[0];
            if (args.length == 2)
                superShardingKey = (ShardingKey) args[1];
            return null;
        }
        if ("setShardingKeyIfValid".equals(methodName)) {
            boolean valid = (args[0] == null || args[0] instanceof D43ShardingKey && !args[0].toString().contains("BOOLEAN:INVALID"))
                            && (args.length == 2
                                || args[1] == null
                                || args[1] instanceof D43ShardingKey && !args[1].toString().contains("BOOLEAN:INVALID"));
            if (valid) {
                shardingKey = (ShardingKey) args[0];
                if (args.length == 3)
                    superShardingKey = (ShardingKey) args[1];
            }
            return valid;
        }
        if ("supportsSharding".equals(methodName))
            return true;
        try {
            Object result = method.invoke(instance, args);
            if (returnType.isInterface() && (returnType.getPackage().getName().startsWith("java.sql")
                                             || returnType.getPackage().getName().startsWith("javax.sql")
                                             || returnType.equals(XAResource.class))) {
                D43Handler handler = new D43Handler(result, this, commonDataSource, shardingKey, superShardingKey);
                return Proxy.newProxyInstance(D43Handler.class.getClassLoader(), new Class[] { returnType }, handler);
            }
            return result;
        } catch (InvocationTargetException x) {
            throw x.getCause();
        }
    }
}
