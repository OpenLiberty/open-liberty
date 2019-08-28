/*******************************************************************************
 * Copyright (c) 2017,2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.rest.handler.validator.jca;

import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import javax.resource.NotSupportedException;
import javax.resource.ResourceException;
import javax.resource.cci.Connection;
import javax.resource.cci.ConnectionFactory;
import javax.resource.cci.ConnectionMetaData;
import javax.resource.cci.ConnectionSpec;
import javax.resource.cci.ResourceAdapterMetaData;
import javax.sql.DataSource;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.jca.service.ConnectionFactoryService;
import com.ibm.wsspi.resource.ResourceConfig;
import com.ibm.wsspi.resource.ResourceConfigFactory;
import com.ibm.wsspi.resource.ResourceFactory;
import com.ibm.wsspi.validator.Validator;

@Component(configurationPolicy = ConfigurationPolicy.IGNORE,
           service = { Validator.class },
           property = { "service.vendor=IBM", "com.ibm.wsspi.rest.handler.root=/validation",
                        "com.ibm.wsspi.rest.handler.config.pid=com.ibm.ws.jca.connectionFactory",
                        "com.ibm.wsspi.rest.handler.config.pid=com.ibm.ws.jca.connectionFactory.supertype", // used by app-defined connection factory
                        "com.ibm.wsspi.rest.handler.config.pid=com.ibm.ws.jca.jmsConnectionFactory",
                        "com.ibm.wsspi.rest.handler.config.pid=com.ibm.ws.jca.jmsQueueConnectionFactory",
                        "com.ibm.wsspi.rest.handler.config.pid=com.ibm.ws.jca.jmsTopicConnectionFactory"
           })
public class ConnectionFactoryValidator implements Validator {
    private final static TraceComponent tc = Tr.register(ConnectionFactoryValidator.class);

    @Reference
    private ResourceConfigFactory resourceConfigFactory;

    /**
     * Utility method that attempts to construct a ConnectionSpec impl of the specified name,
     * which might or might not exist in the resource adapter.
     *
     * @param cciConFactory the connection factory class
     * @param conSpecClassName possible connection spec impl class name to try
     * @param userName user name to set on the connection spec
     * @param password password to set on the connection spec
     * @return ConnectionSpec instance if successful. Otherwise null.
     */
    @FFDCIgnore(Throwable.class)
    private ConnectionSpec createConnectionSpec(ConnectionFactory cciConFactory, String conSpecClassName, String userName, @Sensitive String password) {
        try {
            @SuppressWarnings("unchecked")
            Class<ConnectionSpec> conSpecClass = (Class<ConnectionSpec>) cciConFactory.getClass().getClassLoader().loadClass(conSpecClassName);
            ConnectionSpec conSpec = conSpecClass.newInstance();
            conSpecClass.getMethod("setPassword", String.class).invoke(conSpec, password);
            conSpecClass.getMethod("setUserName", String.class).invoke(conSpec, userName);
            return conSpec;
        } catch (Throwable x) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(this, tc, "Unable to create or populate ConnectionSpec", x.getMessage());
            return null;
        }
    }

    /**
     * Returns an implementation that can access the javax.jms package. Null if the javax.jms package is unavailable.
     *
     * @return an implementation that can access the javax.jms package. Null if the javax.jms package is unavailable.
     */
    private JMSValidator getJMSValidator() {
        try {
            return AccessController.doPrivileged(new PrivilegedExceptionAction<JMSValidator>() {
                @Override
                public JMSValidator run() throws Exception {
                    Class<?> JMSConnectionFactoryValidator = getClass().getClassLoader().loadClass("com.ibm.ws.rest.handler.validator.jms.JMSConnectionFactoryValidator");
                    return (JMSValidator) JMSConnectionFactoryValidator.newInstance();
                }
            });
        } catch (PrivilegedActionException x) {
            return null;
        }
    }

    @Override
    public LinkedHashMap<String, ?> validate(Object instance,
                                             @Sensitive Map<String, Object> props, // @Sensitive prevents auto-FFDC from including password value
                                             Locale locale) {
        final String methodName = "validate";
        String user = (String) props.get(USER);
        String password = (String) props.get(PASSWORD);
        String auth = (String) props.get(AUTH);
        String authAlias = (String) props.get(AUTH_ALIAS);
        String loginConfig = (String) props.get(LOGIN_CONFIG);
        @SuppressWarnings("unchecked")
        Map<String, String> loginConfigProps = (Map<String, String>) props.get(LOGIN_CONFIG_PROPS);

        boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, methodName, user, password == null ? null : "******", auth, authAlias, loginConfig, loginConfigProps == null ? null : loginConfigProps.entrySet());

        JMSValidator jmsValidator = null;
        LinkedHashMap<String, Object> result = new LinkedHashMap<String, Object>();
        try {
            ResourceConfig config = null;
            int authType = AUTH_CONTAINER.equals(auth) ? 0 //
                            : AUTH_APPLICATION.equals(auth) ? 1 //
                                            : -1;

            if (authType >= 0) {
                List<String> cfInterfaceNames = ((ConnectionFactoryService) instance).getConnectionFactoryInterfaceNames();
                if (cfInterfaceNames.isEmpty()) // it is unlikely this error can ever occur because there should have been an earlier failure deploying the RAR
                    throw new RuntimeException("Connection factory cannot be accessed via resource reference because no connection factory interface is defined.");
                config = resourceConfigFactory.createResourceConfig(cfInterfaceNames.get(0));
                config.setResAuthType(authType);
                if (authAlias != null)
                    config.addLoginProperty("DefaultPrincipalMapping", authAlias); // set provided auth alias
                if (loginConfig != null)
                    config.setLoginConfigurationName(loginConfig);
                if (loginConfigProps != null)
                    for (Entry<String, String> entry : loginConfigProps.entrySet()) {
                        Object value = entry.getValue();
                        config.addLoginProperty(entry.getKey(), value == null ? null : value.toString());
                    }
            }

            Object cf = ((ResourceFactory) instance).createResource(config);
            if (cf instanceof ConnectionFactory)
                validateCCIConnectionFactory((ConnectionFactory) cf, (ConnectionFactoryService) instance, user, password, result);
            else if (cf instanceof DataSource)
                validateDataSource((DataSource) cf, user, password, result);
            else {
                // other types of connection factory, such as JMS
                TreeSet<String> interfaces = new TreeSet<String>();
                LinkedList<Class<?>> stack = new LinkedList<Class<?>>();
                for (Class<?> c = cf.getClass(); c != null; c = c.getSuperclass())
                    for (Class<?> i : c.getInterfaces())
                        stack.add(i);
                for (Class<?> i = stack.poll(); i != null; i = stack.poll()) {
                    interfaces.add(i.getName());
                    for (Class<?> j : i.getInterfaces())
                        stack.add(j);
                }
                if (interfaces.contains("javax.jms.ConnectionFactory")) { // also covers QueueConnectionFactory and TopicConnectionFactory
                    jmsValidator = getJMSValidator();
                    if (jmsValidator == null)
                        result.put(FAILURE, Tr.formatMessage(tc, locale, "CWWKO1561_JMS_NOT_ENABLED"));
                    else
                        jmsValidator.validate(cf, user, password, result);
                } else
                    result.put(FAILURE, Tr.formatMessage(tc, locale, "CWWKO1560_VALIDATION_NOT_IMPLEMENTED", cf.getClass().getName(), interfaces));
            }
        } catch (Throwable x) {
            ArrayList<String> sqlStates = new ArrayList<String>();
            ArrayList<Object> errorCodes = new ArrayList<Object>();
            Set<Throwable> causes = new HashSet<Throwable>(); // avoid cycles in exception chain
            for (Throwable cause = x; cause != null && causes.add(cause); cause = cause.getCause()) {
                String sqlState = cause instanceof SQLException ? ((SQLException) cause).getSQLState() : null;
                sqlStates.add(sqlState);

                Object errorCode = null;
                if (jmsValidator != null && jmsValidator.isJMSException(cause))
                    errorCode = jmsValidator.getErrorCode(cause);
                if (cause instanceof ResourceException)
                    errorCode = ((ResourceException) cause).getErrorCode();
                else if (cause instanceof SQLException) {
                    int ec = ((SQLException) cause).getErrorCode();
                    errorCode = sqlState == null && ec == 0 //
                                    ? null // Omit, because it is unlikely that the database actually returned an error code of 0
                                    : Integer.toString(ec);
                }
                errorCodes.add(errorCode);
            }
            result.put("sqlState", sqlStates);
            result.put(FAILURE_ERROR_CODES, errorCodes);
            result.put(FAILURE, x);
        }

        if (trace && tc.isEntryEnabled())
            Tr.exit(this, tc, methodName, result);
        return result;
    }

    /**
     * Validate a connection factory that implements javax.resource.cci.ConnectionFactory.
     *
     * @param cf connection factory instance.
     * @param cfSvc connection factory service.
     * @param user user name, if any, that is specified in the header of the validation request.
     * @param password password, if any, that is specified in the header of the validation request.
     * @param result validation result to which this method appends info.
     * @throws ResourceException if an error occurs.
     */
    private void validateCCIConnectionFactory(ConnectionFactory cf, ConnectionFactoryService cfSvc,
                                              String user, @Sensitive String password,
                                              LinkedHashMap<String, Object> result) throws ResourceException {
        try {
            ResourceAdapterMetaData adapterData = cf.getMetaData();
            result.put("resourceAdapterName", adapterData.getAdapterName());
            result.put("resourceAdapterVersion", adapterData.getAdapterVersion());

            String vendor = adapterData.getAdapterVendorName();
            if (vendor != null && vendor.length() > 0)
                result.put("resourceAdapterVendor", vendor);

            String desc = adapterData.getAdapterShortDescription();
            if (desc != null && desc.length() > 0)
                result.put("resourceAdapterDescription", desc);

            String spec = adapterData.getSpecVersion();
            if (spec != null && spec.length() > 0)
                result.put("connectorSpecVersion", spec);
        } catch (NotSupportedException ignore) {
        } catch (UnsupportedOperationException ignore) {
        }

        ConnectionSpec conSpec = null;
        if (user != null || password != null) {
            String conFactoryClassName = cf.getClass().getName();
            String conSpecClassName = conFactoryClassName.replace("ConnectionFactory", "ConnectionSpec");
            if (!conFactoryClassName.equals(conSpecClassName))
                conSpec = createConnectionSpec(cf, conSpecClassName, user, password);

            if (conSpec == null) {
                // TODO find ConnectionSpec impl another way?
                throw new RuntimeException(Tr.formatMessage(tc, "CWWKO1562_NO_CONSPEC"));
            }
        }

        Connection con;
        try {
            cfSvc.setValidating(true); // initializes a ThreadLocal that instructs the allocate operation to perform additional validation
            con = conSpec == null ? cf.getConnection() : cf.getConnection(conSpec);
        } finally {
            cfSvc.setValidating(false);
        }
        try {
            try {
                ConnectionMetaData conData = con.getMetaData();

                try {
                    String prodName = conData.getEISProductName();
                    if (prodName != null && prodName.length() > 0)
                        result.put("eisProductName", prodName);
                } catch (NotSupportedException ignore) {
                } catch (UnsupportedOperationException ignore) {
                }

                try {
                    String prodVersion = conData.getEISProductVersion();
                    if (prodVersion != null && prodVersion.length() > 0)
                        result.put("eisProductVersion", prodVersion);
                } catch (NotSupportedException ignore) {
                } catch (UnsupportedOperationException ignore) {
                }

                String userName = conData.getUserName();
                if (userName != null && userName.length() > 0)
                    result.put(USER, userName);
            } catch (NotSupportedException ignore) {
            } catch (UnsupportedOperationException ignore) {
            }

            try {
                con.createInteraction().close();
            } catch (NotSupportedException ignore) {
            } catch (UnsupportedOperationException ignore) {
            }
        } finally {
            con.close();
        }
    }

    /**
     * Validate a connection factory that implements javax.sql.DataSource.
     *
     * @param ds data source instance.
     * @param user user name, if any, that is specified in the header of the validation request.
     * @param password password, if any, that is specified in the header of the validation request.
     * @param result validation result to which this method appends info.
     * @throws SQLException if an error occurs.
     */
    private void validateDataSource(DataSource ds, String user, @Sensitive String password,
                                    LinkedHashMap<String, Object> result) throws SQLException {
        java.sql.Connection con = user == null ? ds.getConnection() : ds.getConnection(user, password);

        try {
            DatabaseMetaData metadata = con.getMetaData();
            result.put("databaseProductName", metadata.getDatabaseProductName());
            result.put("databaseProductVersion", metadata.getDatabaseProductVersion());
            result.put("driverName", metadata.getDriverName());
            result.put("driverVersion", metadata.getDriverVersion());

            try {
                String catalog = con.getCatalog();
                if (catalog != null && catalog.length() > 0)
                    result.put("catalog", catalog);
            } catch (SQLFeatureNotSupportedException ignore) {
            }

            try {
                String schema = con.getSchema();
                if (schema != null && schema.length() > 0)
                    result.put("schema", schema);
            } catch (SQLFeatureNotSupportedException ignore) {
            }

            String userName = metadata.getUserName();
            if (userName != null && userName.length() > 0)
                result.put(USER, userName);

            try {
                boolean isValid = con.isValid(120); // TODO better ideas for timeout value?
                if (!isValid)
                    result.put(FAILURE, "java.sql.Connection.isValid: false");
            } catch (SQLFeatureNotSupportedException x) {
            }
        } finally {
            con.close();
        }
    }
}