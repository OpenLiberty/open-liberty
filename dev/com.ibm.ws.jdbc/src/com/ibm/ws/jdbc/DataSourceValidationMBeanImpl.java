/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
package com.ibm.ws.jdbc;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.DynamicMBean;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.ReflectionException;

import org.osgi.framework.ServiceReference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.rsadapter.AdapterUtil;
import com.ibm.wsspi.resource.ResourceFactory;

/**
 * JMX MBean that validates a JDBC datasource connection by JNDI name.
 *
 * Registered as an immediate DS component ({@code immediate="true"}) so it activates
 * as soon as the jdbc feature bundle resolves — before any datasource is used.
 *
 * Tracks all {@link ResourceFactory} services (DataSourceService instances) via a
 * {@code 0..n dynamic} DS reference declared in
 * {@code resources/OSGI-INF/com.ibm.ws.jdbc.DataSourceValidationMBean.xml}.
 * DS binds each DataSourceService once its required references are satisfied (which
 * requires {@code servlet-3.x} or another feature that provides
 * {@code ApplicationRecycleCoordinator}).
 *
 * <p>ObjectName: {@code WebSphere:type=com.ibm.ws.jdbc.DataSourceValidationMBean}
 *
 * <p>Operation:
 * <pre>  String validateConnections(String jndiName, String user, String password)</pre>
 *
 * At call time the {@link ResourceFactory} for {@code jndiName} is looked up from the
 * tracked map and {@code createResource(null)} is called to obtain a DataSource.
 * All DataSource/Connection/DatabaseMetaData calls use reflection to safely cross the
 * OSGi classloader boundary.
 *
 * <h3>Prerequisites</h3>
 * <ul>
 *   <li>The Liberty server must include {@code servlet-3.x} (or any feature that loads
 *       {@code com.ibm.ws.app.manager}) so that {@code ApplicationRecycleCoordinator}
 *       is available and {@code DataSourceService} can become satisfied.</li>
 *   <li>The datasource must be declared in {@code server.xml} with a {@code jndiName}.</li>
 * </ul>
 */
public class DataSourceValidationMBeanImpl implements DynamicMBean {

    private static final TraceComponent tc = Tr.register(DataSourceValidationMBeanImpl.class,
                                                          AdapterUtil.TRACE_GROUP,
                                                          AdapterUtil.NLS_FILE);

    /**
     * Map of jndiName → ResourceFactory, kept current by DS bind/unbind callbacks.
     * DS binds each DataSourceService (which implements ResourceFactory) when all of
     * its required references become satisfied at server startup.
     */
    private final ConcurrentHashMap<String, ResourceFactory> resourceFactories =
        new ConcurrentHashMap<String, ResourceFactory>();

    // -------------------------------------------------------------------------
    // DS bind / unbind for the 0..n dynamic ResourceFactory reference
    // (see resources/OSGI-INF/com.ibm.ws.jdbc.DataSourceValidationMBean.xml)
    // -------------------------------------------------------------------------

    protected void addResourceFactory(ServiceReference<ResourceFactory> ref, ResourceFactory factory) {
        Object jndiName = ref.getProperty("jndiName");
        if (jndiName instanceof String && !((String) jndiName).isEmpty()) {
            resourceFactories.put((String) jndiName, factory);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "addResourceFactory jndiName=" + jndiName);
        }
    }

    protected void removeResourceFactory(ServiceReference<ResourceFactory> ref) {
        Object jndiName = ref.getProperty("jndiName");
        if (jndiName instanceof String) {
            resourceFactories.remove(jndiName);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "removeResourceFactory jndiName=" + jndiName);
        }
    }

    // -------------------------------------------------------------------------
    // Validation
    // -------------------------------------------------------------------------

    /**
     * Validate a connection for the datasource identified by {@code jndiName}.
     *
     * @param jndiName JNDI name of the datasource as configured in {@code server.xml}
     * @param user     optional user; {@code null} for container-managed credentials
     * @param password optional password; {@code null} for container-managed credentials
     * @return human-readable result string beginning with {@code successful=true} or
     *         {@code successful=false}
     */
    public String validateConnections(String jndiName, String user, String password)
            throws MBeanException {
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "validateConnections", jndiName, user,
                     password == null ? null : "******");

        LinkedHashMap<String, Object> result = new LinkedHashMap<String, Object>();
        try {
            if (jndiName == null || jndiName.isEmpty()) {
                result.put("error", "jndiName must be provided");
            } else {
                ResourceFactory factory = resourceFactories.get(jndiName);
                if (factory == null) {
                    result.put("error", "No DataSource found with jndiName=" + jndiName
                               + ". Known datasources: " + resourceFactories.keySet());
                } else {
                    Object dsObj = factory.createResource(null);
                    if (dsObj == null) {
                        result.put("error", "createResource returned null for jndiName=" + jndiName);
                    } else {
                        validateDataSource(dsObj, user, password, result);
                    }
                }
            }
        } catch (Throwable x) {
            FFDCFilter.processException(x, getClass().getName(), "validateConnections", this);
            result.put("error", x.toString());
        }

        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, Object> entry : result.entrySet()) {
            if (!first) sb.append(", ");
            sb.append(entry.getKey()).append('=').append(entry.getValue());
            first = false;
        }
        sb.insert(0, result.containsKey("error") ? "successful=false, " : "successful=true, ");

        String resultStr = sb.toString();
        if (trace && tc.isEntryEnabled())
            Tr.exit(this, tc, "validateConnections", resultStr);
        return resultStr;
    }

    private void validateDataSource(Object dsObj, String user, String password,
                                    LinkedHashMap<String, Object> result) throws Exception {
        Class<?> dsClass = dsObj.getClass();

        java.lang.reflect.Method getConnNoArg = dsClass.getMethod("getConnection");
        java.lang.reflect.Method getConnWithAuth = null;
        try {
            getConnWithAuth = dsClass.getMethod("getConnection", String.class, String.class);
        } catch (NoSuchMethodException ignore) {
        }

        Object conObj;
        try {
            if (user != null && getConnWithAuth != null)
                conObj = getConnWithAuth.invoke(dsObj, user, password);
            else
                conObj = getConnNoArg.invoke(dsObj);
        } catch (java.lang.reflect.InvocationTargetException ite) {
            Throwable cause = ite.getCause();
            String sqlState = null;
            int errorCode = 0;
            try {
                sqlState  = (String) cause.getClass().getMethod("getSQLState").invoke(cause);
                errorCode = (Integer) cause.getClass().getMethod("getErrorCode").invoke(cause);
            } catch (Exception ignore) {
            }
            result.put("error", cause.getMessage()
                       + (sqlState  != null ? " [SQLState="  + sqlState  + "]" : "")
                       + (errorCode != 0    ? " [errorCode=" + errorCode + "]" : ""));
            return;
        }

        try {
            Class<?> conClass  = conObj.getClass();
            Object   metaObj   = conClass.getMethod("getMetaData").invoke(conObj);
            Class<?> metaClass = metaObj.getClass();

            result.put("databaseProductName",
                       metaClass.getMethod("getDatabaseProductName").invoke(metaObj));
            result.put("databaseProductVersion",
                       metaClass.getMethod("getDatabaseProductVersion").invoke(metaObj));
            result.put("jdbcDriverName",
                       metaClass.getMethod("getDriverName").invoke(metaObj));
            result.put("jdbcDriverVersion",
                       metaClass.getMethod("getDriverVersion").invoke(metaObj));
            try {
                Object catalog = conClass.getMethod("getCatalog").invoke(conObj);
                if (catalog != null && !catalog.toString().isEmpty())
                    result.put("catalog", catalog);
            } catch (Exception ignore) {
            }
            try {
                Object schema = conClass.getMethod("getSchema").invoke(conObj);
                if (schema != null && !schema.toString().isEmpty())
                    result.put("schema", schema);
            } catch (Exception ignore) {
            }
            Object dbUser = metaClass.getMethod("getUserName").invoke(metaObj);
            if (dbUser != null && !dbUser.toString().isEmpty())
                result.put("user", dbUser);
            try {
                Object valid = conClass.getMethod("isValid", int.class).invoke(conObj, 120);
                if (Boolean.FALSE.equals(valid))
                    result.put("warning", "Connection.isValid returned false");
            } catch (Exception ignore) {
            }
        } finally {
            try {
                conObj.getClass().getMethod("close").invoke(conObj);
            } catch (Exception ignore) {
            }
        }
    }

    // -------------------------------------------------------------------------
    // DynamicMBean dispatch
    // -------------------------------------------------------------------------

    @Override
    public Object invoke(String actionName, Object[] params, String[] signature)
            throws MBeanException, ReflectionException {
        if ("validateConnections".equalsIgnoreCase(actionName)) {
            String jndiName = params != null && params.length >= 1 ? (String) params[0] : null;
            String user     = params != null && params.length >= 2 ? (String) params[1] : null;
            String password = params != null && params.length >= 3 ? (String) params[2] : null;
            return validateConnections(jndiName, user, password);
        }
        throw new MBeanException(new IllegalArgumentException("Unknown operation: " + actionName));
    }

    @Override
    public MBeanInfo getMBeanInfo() {
        MBeanParameterInfo p0 = new MBeanParameterInfo("jndiName",
            String.class.getCanonicalName(),
            "JNDI name of the datasource as configured in Liberty server.xml");
        MBeanParameterInfo p1 = new MBeanParameterInfo("user",
            String.class.getCanonicalName(),
            "Optional user name; pass null for container-managed credentials.");
        MBeanParameterInfo p2 = new MBeanParameterInfo("password",
            String.class.getCanonicalName(),
            "Optional password; pass null for container-managed credentials.");
        MBeanOperationInfo op = new MBeanOperationInfo(
            "validateConnections",
            "(Beta) Validates a JDBC datasource connection by JNDI name. Available as soon as " +
            "the jdbc feature activates — before any pool initialisation.",
            new MBeanParameterInfo[] { p0, p1, p2 },
            String.class.getCanonicalName(),
            MBeanOperationInfo.ACTION_INFO);
        return new MBeanInfo(
            DataSourceValidationMBeanImpl.class.getName(),
            "(Beta) Validates JDBC datasource connections by JNDI name.",
            null, null, new MBeanOperationInfo[] { op }, null, null);
    }

    @Override
    public Object getAttribute(String attribute)
            throws AttributeNotFoundException, MBeanException, ReflectionException {
        throw new AttributeNotFoundException(attribute);
    }

    @Override
    public void setAttribute(Attribute attribute)
            throws AttributeNotFoundException, InvalidAttributeValueException,
                   MBeanException, ReflectionException {
        throw new AttributeNotFoundException(attribute.getName());
    }

    @Override
    public AttributeList getAttributes(String[] attributes) {
        return new AttributeList();
    }

    @Override
    public AttributeList setAttributes(AttributeList attributes) {
        return new AttributeList();
    }
}
