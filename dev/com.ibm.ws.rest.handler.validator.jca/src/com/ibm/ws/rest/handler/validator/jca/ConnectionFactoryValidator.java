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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.resource.NotSupportedException;
import javax.resource.ResourceException;
import javax.resource.cci.Connection;
import javax.resource.cci.ConnectionFactory;
import javax.resource.cci.ConnectionMetaData;
import javax.resource.cci.ConnectionSpec;
import javax.resource.cci.ResourceAdapterMetaData;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;

import com.ibm.json.java.JSONObject;
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
           property = { "service.vendor=IBM", "com.ibm.wsspi.rest.handler.root=/validator", "com.ibm.wsspi.rest.handler.config.pid=com.ibm.ws.jca.connectionFactory.supertype" })
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

    @Override
    public LinkedHashMap<String, ?> validate(Object instance, Map<String, Object> props, Locale locale) {
        final String methodName = "validate";
        String user = (String) props.get("user");
        String password = (String) props.get("password");
        String auth = (String) props.get("auth");
        String authAlias = (String) props.get("authAlias");
        String loginConfig = (String) props.get("loginConfig");

        boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, methodName, user, password == null ? null : "******", auth, authAlias, loginConfig);

        LinkedHashMap<String, Object> result = new LinkedHashMap<String, Object>();
        try {
            ResourceConfig config = null;
            int authType = "container".equals(auth) ? 0 //
                            : "application".equals(auth) ? 1 //
                                            : -1;

            if (authType >= 0) {
                List<String> cfInterfaceNames = ((ConnectionFactoryService) instance).getConnectionFactoryInterfaceNames();
                if (cfInterfaceNames.isEmpty()) // it is unlikely this error can ever occur because there should have been an earlier failure deploying the RAR
                    throw new RuntimeException("Connection factory cannot be accessed via resource reference because no connection factory interface is defined.");
                config = resourceConfigFactory.createResourceConfig(cfInterfaceNames.get(0));
                config.setResAuthType(authType);
                if (authAlias != null)
                    config.addLoginProperty("DefaultPrincipalMapping", authAlias); // set provided auth alias
                if (loginConfig != null) {
                    // Add custom login module name and properties
                    config.setLoginConfigurationName(loginConfig);
                    String requestBodyString = (String) props.get("json");
                    JSONObject requestBodyJson = requestBodyString == null ? null : JSONObject.parse(requestBodyString);
                    if (requestBodyJson != null && requestBodyJson.containsKey("loginConfigProperties")) {
                        Object loginConfigProperties = requestBodyJson.get("loginConfigProperties");
                        if (loginConfigProperties instanceof JSONObject) {
                            JSONObject loginConfigProps = (JSONObject) loginConfigProperties;
                            for (Object entry : loginConfigProps.entrySet()) {
                                @SuppressWarnings("unchecked")
                                Entry<String, String> e = (Entry<String, String>) entry;
                                if (trace && tc.isDebugEnabled())
                                    Tr.debug(tc, "Adding custom login module property with key=" + e.getKey());
                                config.addLoginProperty(e.getKey(), e.getValue());
                            }
                        }
                    }
                }
            }

            Object cf = ((ResourceFactory) instance).createResource(config);
            if (cf instanceof ConnectionFactory) {
                ConnectionFactory cciConFactory = (ConnectionFactory) cf;
                try {
                    ResourceAdapterMetaData adapterData = cciConFactory.getMetaData();
                    result.put("resourceAdapterName", adapterData.getAdapterName());
                    result.put("resourceAdapterVersion", adapterData.getAdapterVersion());

                    String spec = adapterData.getSpecVersion();
                    if (spec != null && spec.length() > 0)
                        result.put("resourceAdapterJCASupport", spec);

                    String vendor = adapterData.getAdapterVendorName();
                    if (vendor != null && vendor.length() > 0)
                        result.put("resourceAdapterVendor", vendor);

                    String desc = adapterData.getAdapterShortDescription();
                    if (desc != null && desc.length() > 0)
                        result.put("resourceAdapterDescription", desc);
                } catch (NotSupportedException ignore) {
                } catch (UnsupportedOperationException ignore) {
                }

                ConnectionSpec conSpec = null;
                if (user != null || password != null) {
                    String conFactoryClassName = cciConFactory.getClass().getName();
                    String conSpecClassName = conFactoryClassName.replace("ConnectionFactory", "ConnectionSpec");
                    if (!conFactoryClassName.equals(conSpecClassName))
                        conSpec = createConnectionSpec(cciConFactory, conSpecClassName, user, password);

                    if (conSpec == null) {
                        // TODO find ConnectionSpec impl another way?
                        throw new RuntimeException("Unable to locate javax.resource.cci.ConnectionSpec impl from resource adapter.");
                    }
                }

                Connection con = conSpec == null ? cciConFactory.getConnection() : cciConFactory.getConnection(conSpec);
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
                            result.put("user", userName);
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
            } // TODO other types of connection factory, such as DataSource or custom or JMS (which should have used jmsConnectionFactory)
        } catch (Throwable x) {
            ArrayList<String> errorCodes = new ArrayList<String>();
            Set<Throwable> causes = new HashSet<Throwable>(); // avoid cycles in exception chain
            for (Throwable cause = x; cause != null && causes.add(cause); cause = cause.getCause()) {
                String errorCode = cause instanceof ResourceException ? ((ResourceException) cause).getErrorCode() : null;
                errorCodes.add(errorCode);
            }
            result.put("errorCode", errorCodes);
            result.put("failure", x);
        }

        if (trace && tc.isEntryEnabled())
            Tr.exit(this, tc, methodName, result);
        return result;
    }
}