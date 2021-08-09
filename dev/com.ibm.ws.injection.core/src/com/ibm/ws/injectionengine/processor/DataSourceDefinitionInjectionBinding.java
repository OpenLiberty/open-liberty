/*******************************************************************************
 * Copyright (c) 2010, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.injectionengine.processor;

import java.lang.reflect.Member;
import java.sql.Connection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.annotation.sql.DataSourceDefinition;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.javaee.dd.common.DataSource;
import com.ibm.ws.javaee.dd.common.Property;
import com.ibm.wsspi.injectionengine.ComponentNameSpaceConfiguration;
import com.ibm.wsspi.injectionengine.InjectionBinding;
import com.ibm.wsspi.injectionengine.InjectionConfigConstants;
import com.ibm.wsspi.injectionengine.InjectionConfigurationException;
import com.ibm.wsspi.injectionengine.InjectionException;
import com.ibm.wsspi.injectionengine.JNDIEnvironmentRefType;

public class DataSourceDefinitionInjectionBinding
                extends InjectionBinding<DataSourceDefinition>
{
    private static final TraceComponent tc = Tr.register(DataSourceDefinitionInjectionBinding.class,
                                                         InjectionConfigConstants.traceString,
                                                         InjectionConfigConstants.messageFile);

    private static final String KEY_DESCRIPTION = "description";
    private static final String KEY_CLASS_NAME = "className";
    private static final String KEY_SERVER_NAME = "serverName";
    private static final String KEY_PORT_NUMBER = "portNumber";
    private static final String KEY_DATABASE_NAME = "databaseName";
    private static final String KEY_URL = "url";
    private static final String KEY_USER = "user";
    private static final String KEY_PASSWORD = "password";
    private static final String KEY_LOGIN_TIMEOUT = "loginTimeout";
    private static final String KEY_TRANSACTIONAL = "transactional";
    private static final String KEY_ISOLATION_LEVEL = "isolationLevel";
    private static final String KEY_INITIAL_POOL_SIZE = "initialPoolSize";
    private static final String KEY_MAX_POOL_SIZE = "maxPoolSize";
    private static final String KEY_MIN_POOL_SIZE = "minPoolSize";
    private static final String KEY_MAX_IDLE_TIME = "maxIdleTime";
    private static final String KEY_MAX_STATEMENTS = "maxStatements";

    private static final Map<Integer, String> ISOLATION_LEVEL_NAMES = new TreeMap<Integer, String>();

    static
    {
        ISOLATION_LEVEL_NAMES.put(Connection.TRANSACTION_NONE, "TRANSACTION_NONE");
        ISOLATION_LEVEL_NAMES.put(Connection.TRANSACTION_READ_UNCOMMITTED, "TRANSACTION_READ_UNCOMMITTED");
        ISOLATION_LEVEL_NAMES.put(Connection.TRANSACTION_READ_COMMITTED, "TRANSACTION_READ_COMMITTED");
        ISOLATION_LEVEL_NAMES.put(Connection.TRANSACTION_REPEATABLE_READ, "TRANSACTION_REPEATABLE_READ");
        ISOLATION_LEVEL_NAMES.put(Connection.TRANSACTION_SERIALIZABLE, "TRANSACTION_SERIALIZABLE");
    }

    private final String ivBinding; // F743-33811
    private Set<String> ivAttributeErrors;
    private Set<String> ivPropertyErrors;

    private String ivDescription; // d662109
    private boolean ivXMLDescription; // d662109

    private String ivClassName;
    private boolean ivXMLClassName;

    private String ivServerName;
    private boolean ivXMLServerName;

    private Integer ivPortNumber;
    private boolean ivXMLPortNumber;

    private String ivDatabaseName;
    private boolean ivXMLDatabaseName;

    private String ivURL;
    private boolean ivXMLURL;

    private String ivUser;
    private boolean ivXMLUser;

    private String ivPassword;
    private boolean ivXMLPassword;

    private Map<String, String> ivProperties;
    private final Set<String> ivXMLProperties = new HashSet<String>();

    private Integer ivLoginTimeout;
    private boolean ivXMLLoginTimeout;

    private Boolean ivTransactional;
    private boolean ivXMLTransactional;

    private Integer ivIsolationLevel;
    private boolean ivXMLIsolationLevel;

    private Integer ivInitialPoolSize;
    private boolean ivXMLInitialPoolSize;

    private Integer ivMaxPoolSize;
    private boolean ivXMLMaxPoolSize;

    private Integer ivMinPoolSize;
    private boolean ivXMLMinPoolSize;

    private Integer ivMaxIdleTime;
    private boolean ivXMLMaxIdleTime;

    private Integer ivMaxStatements;
    private boolean ivXMLMaxStatements;

    DataSourceDefinitionInjectionBinding(String jndiName,
                                         ComponentNameSpaceConfiguration compNSConfig)
    {
        super(null, compNSConfig);
        setJndiName(jndiName);

        Map<String, String> dsdBindings = compNSConfig.getDataSourceDefinitionBindings();
        ivBinding = dsdBindings == null ? null : dsdBindings.get(getJndiName()); // F743-33811, d698515
    }

    @Override
    protected JNDIEnvironmentRefType getJNDIEnvironmentRefType() {
        return JNDIEnvironmentRefType.DataSource;
    }

    @Override
    public void merge(DataSourceDefinition annotation, Class<?> instanceClass, Member member)
                    throws InjectionException
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "merge: name=" + getJndiName() + ", " + annotation);

        if (member != null)
        {
            // DataSourceDefinition is a class-level annotation only.
            throw new IllegalArgumentException(member.toString());
        }

        ivDescription = mergeAnnotationValue(ivDescription, ivXMLDescription, annotation.description(), KEY_DESCRIPTION, ""); // d662109
        ivClassName = mergeAnnotationValue(ivClassName, ivXMLClassName, annotation.className(), KEY_CLASS_NAME, "");
        ivServerName = mergeAnnotationValue(ivServerName, ivXMLServerName, annotation.serverName(), KEY_SERVER_NAME, "localhost"); // d663356
        ivPortNumber = mergeAnnotationInteger(ivPortNumber, ivXMLPortNumber, annotation.portNumber(), KEY_PORT_NUMBER, -1, null);
        ivDatabaseName = mergeAnnotationValue(ivDatabaseName, ivXMLDatabaseName, annotation.databaseName(), KEY_DATABASE_NAME, "");
        ivURL = mergeAnnotationValue(ivURL, ivXMLURL, annotation.url(), KEY_URL, "");
        ivUser = mergeAnnotationValue(ivUser, ivXMLUser, annotation.user(), KEY_USER, "");
        ivPassword = mergeAnnotationValue(ivPassword, ivXMLPassword, annotation.password(), KEY_PASSWORD, "");
        ivProperties = mergeAnnotationProperties(ivProperties, ivXMLProperties, annotation.properties());
        ivLoginTimeout = mergeAnnotationInteger(ivLoginTimeout, ivXMLLoginTimeout, annotation.loginTimeout(), KEY_LOGIN_TIMEOUT, 0, null);
        ivTransactional = mergeAnnotationBoolean(ivTransactional, ivXMLTransactional, annotation.transactional(), KEY_TRANSACTIONAL, true);
        ivIsolationLevel = mergeAnnotationIsolationLevel(ivIsolationLevel, ivXMLIsolationLevel, annotation.isolationLevel(), KEY_ISOLATION_LEVEL, -1);
        ivInitialPoolSize = mergeAnnotationInteger(ivInitialPoolSize, ivXMLInitialPoolSize, annotation.initialPoolSize(), KEY_INITIAL_POOL_SIZE, -1, null);
        ivMaxPoolSize = mergeAnnotationInteger(ivMaxPoolSize, ivXMLMaxPoolSize, annotation.maxPoolSize(), KEY_MAX_POOL_SIZE, -1, null);
        ivMinPoolSize = mergeAnnotationInteger(ivMinPoolSize, ivXMLMinPoolSize, annotation.minPoolSize(), KEY_MIN_POOL_SIZE, -1, null);
        ivMaxIdleTime = mergeAnnotationInteger(ivMaxIdleTime, ivXMLMaxIdleTime, annotation.maxIdleTime(), KEY_MAX_IDLE_TIME, -1, null);
        ivMaxStatements = mergeAnnotationInteger(ivMaxStatements, ivXMLMaxStatements, annotation.maxStatements(), KEY_MAX_STATEMENTS, -1, null);

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "merge");
    }

    @Override
    protected void mergeError(Object oldValue,
                              Object newValue,
                              boolean xml,
                              String elementName,
                              boolean property,
                              String key)
                    throws InjectionConfigurationException
    {
        boolean failable = true;
        if (ivBinding != null)
        {
            if (property)
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "mergeError: ignorable property conflict for " + key +
                                 ": old=" + oldValue + ", new=" + newValue);

                if (ivPropertyErrors == null)
                {
                    ivPropertyErrors = new HashSet<String>();
                }
                ivPropertyErrors.add(key);
            }
            else
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "mergeError: ignorable conflict for " + key +
                                 ": old=" + oldValue + ", new=" + newValue);

                if (ivAttributeErrors == null)
                {
                    ivAttributeErrors = new HashSet<String>();
                }
                ivAttributeErrors.add(key);
            }

            if (!isValidationLoggable())
            {
                return;
            }

            failable = isValidationFailable();
        }

        try {
            super.mergeError(oldValue, newValue, xml, elementName, property, key);
        } catch (InjectionConfigurationException e) {
            if (failable) {
                throw e;
            }
        }
    }

    private void annotationIsolationLevelMergeError(Object newValue, String elementName)
                    throws InjectionConfigurationException
    {
        boolean failable = true;
        if (ivBinding != null)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "annotationIsolationLevelError: ignorable: new=" + newValue);

            if (ivAttributeErrors == null)
            {
                ivAttributeErrors = new HashSet<String>();
            }
            ivAttributeErrors.add(elementName);

            if (!isValidationLoggable())
            {
                return;
            }

            failable = isValidationFailable();
        }

        String component = ivNameSpaceConfig.getDisplayName();
        String module = ivNameSpaceConfig.getModuleName();
        String application = ivNameSpaceConfig.getApplicationName();
        String jndiName = getJndiName();

        Tr.error(tc, "INVALID_DATA_SOURCE_ANNOTATION_ISOLATION_LEVEL_CWNEN0067E",
                 jndiName,
                 component,
                 module,
                 application,
                 newValue);

        if (failable)
        {
            String exMsg = "The @DataSourceDefinition source code annotation with the " + jndiName +
                           " name attribute for the " + component +
                           " component in the " + module +
                           " module of the " + application +
                           " application has configuration data for the isolationLevel attribute that is not valid: " + newValue;
            throw new InjectionConfigurationException(exMsg);
        }
    }

    private Integer mergeAnnotationIsolationLevel(Integer oldValue,
                                                  boolean oldValueXML,
                                                  int newValue,
                                                  String elementName,
                                                  int defaultValue) throws InjectionConfigurationException {
        if (newValue != defaultValue && !ISOLATION_LEVEL_NAMES.containsKey(newValue)) {
            annotationIsolationLevelMergeError(newValue, elementName);
            return oldValue;
        }

        return mergeAnnotationInteger(oldValue, oldValueXML, newValue, elementName, defaultValue, ISOLATION_LEVEL_NAMES);
    }

    @Override
    protected void mergeAnnotationPropertyError(String property) throws InjectionConfigurationException {
        boolean failable = true;
        if (ivBinding != null)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "annotationPropertyError: ignorable " + property);

            if (!isValidationLoggable())
            {
                return;
            }

            failable = isValidationFailable();
        }

        try {
            super.mergeAnnotationPropertyError(property);
        } catch (InjectionConfigurationException e) {
            if (failable) {
                throw e;
            }
        }
    }

    void mergeXML(DataSource dsd)
                    throws InjectionConfigurationException
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "mergeXML: name=" + getJndiName() + ", binding=" + ivBinding + ", " + dsd);

        String description = dsd.getDescription(); // d662109
        if (description != null)
        {
            ivDescription = mergeXMLValue(ivDescription, description, "description", KEY_DESCRIPTION, null);
            ivXMLDescription = true;
        }

        String className = dsd.getClassNameValue();
        if (className != null)
        {
            ivClassName = mergeXMLValue(ivClassName, className, "class-name", KEY_CLASS_NAME, null);
            ivXMLClassName = true;
        }

        String serverName = dsd.getServerName();
        if (serverName != null)
        {
            ivServerName = mergeXMLValue(ivServerName, serverName, "server-name", KEY_SERVER_NAME, null);
            ivXMLServerName = true;
        }

        if (dsd.isSetPortNumber())
        {
            ivPortNumber = mergeXMLValue(ivPortNumber, dsd.getPortNumber(), "port-number", KEY_PORT_NUMBER, null);
            ivXMLPortNumber = true;
        }

        String databaseName = dsd.getDatabaseName();
        if (databaseName != null)
        {
            ivDatabaseName = mergeXMLValue(ivDatabaseName, databaseName, "database-name", KEY_DATABASE_NAME, null);
            ivXMLDatabaseName = true;
        }

        String url = dsd.getUrl();
        if (url != null)
        {
            ivURL = mergeXMLValue(ivURL, url, "url", KEY_URL, null);
            ivXMLURL = true;
        }

        String user = dsd.getUser();
        if (user != null)
        {
            ivUser = mergeXMLValue(ivUser, user, "user", KEY_USER, null);
            ivXMLUser = true;
        }

        String password = dsd.getPassword();
        if (password != null)
        {
            ivPassword = mergeXMLValue(ivPassword, password, "password", KEY_PASSWORD, null);
            ivXMLPassword = true;
        }

        List<Property> dsdProps = dsd.getProperties();
        ivProperties = mergeXMLProperties(ivProperties, ivXMLProperties, dsdProps);

        if (dsd.isSetLoginTimeout())
        {
            ivLoginTimeout = mergeXMLValue(ivLoginTimeout, dsd.getLoginTimeout(), "login-timeout", KEY_LOGIN_TIMEOUT, null);
            ivXMLLoginTimeout = true;
        }

        if (dsd.isSetTransactional())
        {
            ivTransactional = mergeXMLValue(ivTransactional, dsd.isTransactional(), "transactional", KEY_TRANSACTIONAL, null);
            ivXMLTransactional = true;
        }

        int isolationLevel = dsd.getIsolationLevelValue();
        if (isolationLevel != Connection.TRANSACTION_NONE)
        {
            ivIsolationLevel = mergeXMLValue(ivIsolationLevel, isolationLevel, "isolation-level", KEY_ISOLATION_LEVEL, ISOLATION_LEVEL_NAMES);
            ivXMLIsolationLevel = true;
        }

        if (dsd.isSetInitialPoolSize())
        {
            ivInitialPoolSize = mergeXMLValue(ivInitialPoolSize, dsd.getInitialPoolSize(), "initial-pool-size", KEY_INITIAL_POOL_SIZE, null);
            ivXMLInitialPoolSize = true;
        }

        if (dsd.isSetMaxPoolSize())
        {
            ivMaxPoolSize = mergeXMLValue(ivMaxPoolSize, dsd.getMaxPoolSize(), "max-pool-size", KEY_MAX_POOL_SIZE, null);
            ivXMLMaxPoolSize = true;
        }

        if (dsd.isSetMinPoolSize())
        {
            ivMinPoolSize = mergeXMLValue(ivMinPoolSize, dsd.getMinPoolSize(), "min-pool-size", KEY_MIN_POOL_SIZE, null);
            ivXMLMinPoolSize = true;
        }

        if (dsd.isSetMaxIdleTime()) // d673852
        {
            ivMaxIdleTime = mergeXMLValue(ivMaxIdleTime, dsd.getMaxIdleTime(), "max-idle-time", KEY_MAX_IDLE_TIME, null);
            ivXMLMaxIdleTime = true;
        }

        if (dsd.isSetMaxStatements())
        {
            ivMaxStatements = mergeXMLValue(ivMaxStatements, dsd.getMaxStatements(), "max-statements", KEY_MAX_STATEMENTS, null);
            ivXMLMaxStatements = true;
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "mergeXML");
    }

    @Override
    public void mergeSaved(InjectionBinding<DataSourceDefinition> injectionBinding) // d681743
    throws InjectionException
    {
        DataSourceDefinitionInjectionBinding dsdBinding = (DataSourceDefinitionInjectionBinding) injectionBinding;

        if (ivBinding != null)
        {
            mergeSavedValue(ivBinding, dsdBinding.ivBinding, "binding-name");
        }
        else
        {
            mergeSavedValue(null, dsdBinding.ivBinding, "binding-name");
            mergeSavedValue(ivDescription, dsdBinding.ivDescription, "description");
            mergeSavedValue(ivClassName, dsdBinding.ivClassName, "class-name");
            mergeSavedValue(ivServerName, dsdBinding.ivServerName, "server-name");
            mergeSavedValue(ivPortNumber, dsdBinding.ivPortNumber, "port-number");
            mergeSavedValue(ivDatabaseName, dsdBinding.ivDatabaseName, "database-name");
            mergeSavedValue(ivURL, dsdBinding.ivURL, "url");
            mergeSavedValue(ivUser, dsdBinding.ivUser, "user");
            mergeSavedValue(ivPassword, dsdBinding.ivPassword, "password");
            mergeSavedValue(ivProperties, dsdBinding.ivProperties, "properties");
            mergeSavedValue(ivLoginTimeout, dsdBinding.ivLoginTimeout, "login-timeout");
            mergeSavedValue(ivTransactional, dsdBinding.ivTransactional, "transactional");
            mergeSavedValue(ivIsolationLevel, dsdBinding.ivIsolationLevel, "isolation-level");
            mergeSavedValue(ivInitialPoolSize, dsdBinding.ivInitialPoolSize, "initial-pool-size");
            mergeSavedValue(ivMaxPoolSize, dsdBinding.ivMaxPoolSize, "max-pool-size");
            mergeSavedValue(ivMinPoolSize, dsdBinding.ivMinPoolSize, "min-pool-size");
            mergeSavedValue(ivMaxIdleTime, dsdBinding.ivMaxIdleTime, "maxidle-time");
            mergeSavedValue(ivMaxStatements, dsdBinding.ivMaxStatements, "max-statements");
        }
    }

    void resolve()
                    throws InjectionException
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "resolve");

        Map<String, Object> props = new HashMap<String, Object>();

        if (ivProperties != null)
        {
            for (Map.Entry<String, String> entry : ivProperties.entrySet())
            {
                String key = entry.getKey();

                // Only consider non-conflicting properties.               RTC111756
                if (ivPropertyErrors == null || !ivPropertyErrors.contains(key))
                {
                    props.put(key, entry.getValue());
                }
            }
        }

        // Insert all remaining attributes.
        addValidOrRemoveProperty(props, KEY_DESCRIPTION, ivDescription); // d662109
        addValidOrRemoveProperty(props, KEY_CLASS_NAME, ivClassName);
        addValidOrRemoveProperty(props, KEY_SERVER_NAME, ivServerName);
        addValidOrRemoveProperty(props, KEY_PORT_NUMBER, ivPortNumber);
        addValidOrRemoveProperty(props, KEY_DATABASE_NAME, ivDatabaseName);
        addValidOrRemoveProperty(props, KEY_URL, ivURL);
        addValidOrRemoveProperty(props, KEY_USER, ivUser);
        addValidOrRemoveProperty(props, KEY_PASSWORD, ivPassword);
        addValidOrRemoveProperty(props, KEY_LOGIN_TIMEOUT, ivLoginTimeout);
        addValidOrRemoveProperty(props, KEY_TRANSACTIONAL, ivTransactional);
        addValidOrRemoveProperty(props, KEY_ISOLATION_LEVEL, ivIsolationLevel);
        addValidOrRemoveProperty(props, KEY_INITIAL_POOL_SIZE, ivInitialPoolSize);
        addValidOrRemoveProperty(props, KEY_MIN_POOL_SIZE, ivMinPoolSize);
        addValidOrRemoveProperty(props, KEY_MAX_POOL_SIZE, ivMaxPoolSize);
        addValidOrRemoveProperty(props, KEY_MAX_IDLE_TIME, ivMaxIdleTime);
        addValidOrRemoveProperty(props, KEY_MAX_STATEMENTS, ivMaxStatements);

        setObjects(null, createDefinitionReference(ivBinding, javax.sql.DataSource.class.getName(), props));

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "resolve");
    }

    private void addValidOrRemoveProperty(Map<String, Object> props, String key, Object value)
    {
        // Only consider non-conflicting attributes.                     RTC111756
        if (ivAttributeErrors != null && ivAttributeErrors.contains(key))
        {
            value = null;
        }

        addOrRemoveProperty(props, key, value);
    }

    @Override
    public Class<?> getAnnotationType()
    {
        return DataSourceDefinition.class;
    }
}
