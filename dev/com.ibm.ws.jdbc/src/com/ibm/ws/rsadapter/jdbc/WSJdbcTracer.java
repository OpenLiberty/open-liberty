/*******************************************************************************
 * Copyright (c) 2001, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.rsadapter.jdbc;

import java.beans.IntrospectionException; 
import java.beans.Introspector; 
import java.beans.PropertyDescriptor; 
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Set;

import javax.sql.ConnectionPoolDataSource;
import javax.sql.DataSource;
import javax.sql.PooledConnection;
import javax.sql.XAConnection;
import javax.sql.XADataSource;
import javax.transaction.xa.XAResource;

import com.ibm.ejs.ras.TraceComponent;
import com.ibm.ws.jdbc.internal.PropertyService;

/**
 * This class provides generic JDBC method level tracing for JDBC drivers (like the
 * Microsoft SQL Server 2005 JDBC Driver) that don't themselves provide sufficient trace.
 */
public class WSJdbcTracer implements InvocationHandler
{
    static final Set<String> METHODS_TO_SKIP = new HashSet<String>(); 
    static {
        METHODS_TO_SKIP.add("equals");
        METHODS_TO_SKIP.add("hashCode");
        METHODS_TO_SKIP.add("toString");
    }

    // In order allow for the possibility of proprietary APIs introduced by the JDBC driver,
    // only the following interfaces (and interfaces from the wrapper pattern) are traced.
    public static final Set<Class<?>> TRACEABLE_TYPES = new HashSet<Class<?>>();
    static {
        TRACEABLE_TYPES.add(CallableStatement.class);
        TRACEABLE_TYPES.add(Connection.class);
        TRACEABLE_TYPES.add(ConnectionPoolDataSource.class);
        TRACEABLE_TYPES.add(DatabaseMetaData.class);
        TRACEABLE_TYPES.add(DataSource.class);
        TRACEABLE_TYPES.add(PooledConnection.class);
        TRACEABLE_TYPES.add(PreparedStatement.class);
        TRACEABLE_TYPES.add(ResultSet.class);
        TRACEABLE_TYPES.add(Statement.class);
        TRACEABLE_TYPES.add(XAConnection.class);
        TRACEABLE_TYPES.add(XADataSource.class);
        TRACEABLE_TYPES.add(XAResource.class);
    }

    TraceComponent tracer;
    PrintWriter writer;
    Object impl;
    String traceID;
    String sql; //  - SQL for PreparedStatement and CallableStatement

    /**
     * Construct a new proxy for tracing the specified JDBC driver class.
     * 
     * @param tracer the trace component, used for detecting enablement.
     * @param writer a location to write the trace to.
     * @param impl the JDBC driver implementation we are tracing on behalf of.
     * @param ifc the JDBC API interface we are tracing.
     * @param sql the SQL for Prepared/CallableStatements, or NULL otherwise.
     * @param introspect indicates whether to introspect and trace the configuration.
     */
    public WSJdbcTracer(TraceComponent tracer, PrintWriter writer, Object impl, Class<?> ifc, String sql, boolean introspect) {
        this.tracer = tracer;
        this.writer = writer;
        this.impl = impl;
        this.sql = sql; 
        traceID = ifc.getSimpleName() + Integer.toHexString(System.identityHashCode(impl));
        if (introspect)
            traceConfig();
    }

    /**
     * Returns the underlying object.
     * 
     * @param obj an object that might be a proxy.
     * 
     * @return the underlying object, or, if not a proxy, then the original object.
     */
    public static final Object getImpl(Object obj) {
        InvocationHandler handler;

        return Proxy.isProxyClass(obj.getClass()) && (handler = Proxy.getInvocationHandler(obj)) instanceof WSJdbcTracer ? ((WSJdbcTracer) handler).impl : obj;
    }

    /**
     * Invokes a method on behalf of a dynamic proxy.
     * 
     * @param proxy the dynamic proxy. Be very careful when performing operations on the
     *            dynamic proxy reference. Such operations result in additional invocations of
     *            this method, and could potentially lead to recursive stack overflow errors
     *            if not accounted for.
     * @param method the method being invoked.
     * @param args the parameters to the method.
     * 
     * @return the result of invoking the operation on the underlying object.
     * @throws Throwable if something goes wrong.
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // Skip common methods like toString and hashCode

        String methName = method.getName();
        if (METHODS_TO_SKIP.contains(methName)) {
            try {
                return method.invoke(impl, args);
            } catch (InvocationTargetException x) {// No FFDC coded needed.
                throw x.getCause();
            }
        }

        Object result;
        Object traceableResult = null;

        // Trace entry

        if (TraceComponent.isAnyTracingEnabled() && tracer.isDebugEnabled()) {
            boolean hasPassword = methName.equals("getPooledConnection") || methName.equals("getXAConnection") || methName.equals("connect");

            StringBuilder buffer = new StringBuilder(120);

            buffer.append("==> ").append(traceID).append('.').append(methName).append('(');

            if (tracer.getLevel() <= 1 /* LevelConstants.LEVEL_ALL */&& args != null)
                for (int i = 0; i < args.length; i++) {
                    if(methName.equals("connect") && i == 0) {
                        buffer.append(PropertyService.filterURL(toString(args[i])));
                        continue;
                    }
                    
                    if (i != 0)
                        buffer.append(", ");
                    buffer.append(hasPassword && i == 1 ? "***" : toString(args[i]));
                }

            buffer.append(");");

            // SQL for PreparedStatement & CallableStatement
            if (sql != null && methName.startsWith("execute"))
                buffer.append(" // ").append(sql);

            writer.println(new String(buffer));
        }

        // Perform the operation

        try {
            result = method.invoke(impl, args);
        } catch (InvocationTargetException x) {
            // No FFDC code needed. Trace exit for failed invocation.

            if (TraceComponent.isAnyTracingEnabled() && tracer.isDebugEnabled()) 
            {
                StringBuilder buffer = new StringBuilder(800);
                buffer.append("<== ").append(toString(x.getCause()));
                writer.println(new String(buffer));
            }

            throw x.getCause();
        }

        // If the result is a JDBC API or a result of unwrap, it should be made traceable.

        boolean isUnwrap = methName.equals("unwrap");

        if (result != null && (isUnwrap || TRACEABLE_TYPES.contains(method.getReturnType()))) { 
            final Class<?> returnType = isUnwrap ? (Class<?>) args[0] : method.getReturnType();

            // - include SQL for PreparedStatement & CallableStatement
            final String query = methName.startsWith("prepare") && PreparedStatement.class.isAssignableFrom(returnType)
                           && args.length >= 1 && args[0] instanceof String ? (String) args[0] : null;
            final WSJdbcTracer newTracer = new WSJdbcTracer(tracer, writer, result, returnType, query, false);
            traceableResult = AccessController.doPrivileged(new PrivilegedAction<Object>() {
                public Object run() {
                    return Proxy.newProxyInstance(returnType.getClassLoader(), new Class[] { returnType }, newTracer);
                }
            });
        }

        // Trace exit after successful invocation.

        if (TraceComponent.isAnyTracingEnabled() && tracer.isDebugEnabled()) {
            StringBuilder buffer = new StringBuilder(40);
            buffer.append("<== ");

            if (void.class.equals(method.getReturnType())) {
                buffer.append(methName);
            }
            else {
                if (traceableResult != null)
                    buffer.append(method.getReturnType().getSimpleName())
                                    .append(Integer.toHexString(System.identityHashCode(result)));

                else if (tracer.getLevel() <= 1 /* LevelConstants.LEVEL_ALL */)
                    buffer.append(toString(result));
            }

            writer.println(new String(buffer));
        }

        return traceableResult == null ? result : traceableResult;
    }

    /**
     * A utility method for converting generic objects to traceable strings.
     * 
     * @param obj the item to trace.
     * 
     * @return a string representing the object.
     */
    private String toString(Object obj) {
        // - unicode characters in trace
        if (obj instanceof String) {
            String str = (String) obj;
            StringBuilder sb = new StringBuilder(str.length()).append('\"');
            byte[] bytes;

            try {
                bytes = str.getBytes("UTF-16BE");
            } catch (UnsupportedEncodingException x) {// No FFDC code needed
                return new String(sb.append(str).append('\"'));
            }

            for (int i = 0; i < bytes.length; i += 2) {
                if (bytes[i] == 0 && (bytes[i + 1] & 0x80) == 0)
                    sb.append((char) bytes[i + 1]);
                else {
                    // unicode
                    sb.append("\\u").append(Integer.toHexString(bytes[i] >> 4 & 0xf))
                                    .append(Integer.toHexString(bytes[i] & 0xf))
                                    .append(Integer.toHexString(bytes[i + 1] >> 4 & 0xf))
                                    .append(Integer.toHexString(bytes[i + 1] & 0xf));
                }
            }
            return new String(sb.append('\"'));
        }

        // - some additional data types. These will make it easier to convert
        // supplemental trace into a standalone Java program.
        if (obj instanceof Long)
            return obj.toString() + 'l';
        if (obj instanceof Float)
            return obj.toString() + 'f';
        if (obj instanceof Byte)
            return "(byte)" + obj.toString();
        if (obj instanceof Short)
            return "(short)" + obj.toString();
        if (obj instanceof java.sql.Date)
            return "new java.sql.Date(" + ((Date) obj).getTime() + "l)";
        if (obj instanceof Time)
            return "new java.sql.Time(" + ((Time) obj).getTime() + "l)";
        if (obj instanceof Timestamp)
            return "new java.sql.Timestamp(" + ((Timestamp) obj).getTime() + "l)";
        if (obj instanceof URL)
            return "new java.net.URL(\"" + obj.toString() + "\")";
        if (obj instanceof BigDecimal)
            return "new java.math.BigDecimal(\"" + obj.toString() + "\")";

        if (obj instanceof Throwable) {
            StringWriter out = new StringWriter();
            ((Throwable) obj).printStackTrace(new PrintWriter(out));
            return out.toString();
        }

        if (obj != null && obj.getClass().isArray()) {
            int length = java.lang.reflect.Array.getLength(obj); 

            StringBuilder buffer = new StringBuilder(120);
            buffer.append(obj.getClass().getComponentType().getSimpleName());
            buffer.append('[').append(length).append("] {");

            if (length < 20)
                for (int i = 0; i < length; i++)
                    buffer.append(i == 0 ? " " : ", ").append(java.lang.reflect.Array.get(obj, i)); 
            else
                buffer.append(" ...");

            buffer.append(" }");
            return new String(buffer);
        }

        return obj == null ? null : obj.toString();
    }

    /**
     * Trace the configuration (found by introspection) of the underlying object.
     * This is useful for newly created data sources.
     */
    private void traceConfig() {
        writer.println(new StringBuilder(180).append(impl.getClass().getName()).append(' ').append(traceID).append(" = new ").
                        append(impl.getClass().getName()).append("();").toString());

        try {
            PropertyDescriptor[] props = Introspector.getBeanInfo(impl.getClass()).getPropertyDescriptors();
            Method readMeth, writeMeth;

            for (int i = 0; i < props.length; i++) {
                if ((readMeth = props[i].getReadMethod()) != null && (writeMeth = props[i].getWriteMethod()) != null) {
                    StringBuilder buffer = new StringBuilder(60);
                    buffer.append(traceID).append('.').append(writeMeth.getName()).append('(');

                    if (!"password".equals(props[i].getName())) {
                        try {
                            buffer.append(toString(readMeth.invoke(impl, (Object[]) null))); 
                        } catch (Throwable th) {
                        } // No FFDC code needed. Cannot read property.
                    }

                    buffer.append(");");
                    writer.println(new String(buffer));
                }
            }
        } catch (IntrospectionException introspectX) {
            // No FFDC code needed, just include the error in the trace.
            writer.println("Unable to read the configuration for " + traceID);
            introspectX.printStackTrace(writer);
        }
    }
}
