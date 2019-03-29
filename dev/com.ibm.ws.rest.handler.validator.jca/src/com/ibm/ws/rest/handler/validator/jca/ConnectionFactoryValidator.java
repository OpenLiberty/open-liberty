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

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import javax.resource.NotSupportedException;
import javax.resource.cci.Connection;
import javax.resource.cci.ConnectionFactory;
import javax.resource.cci.ConnectionMetaData;
import javax.resource.cci.ResourceAdapterMetaData;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
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

    @Override
    public LinkedHashMap<String, ?> validate(Object instance, Map<String, Object> props, Locale locale) {
        final String methodName = "validate";
        String user = (String) props.get("user");
        String pass = (String) props.get("password");
        String auth = (String) props.get("auth");
        String authAlias = (String) props.get("authAlias");
        String loginConfig = (String) props.get("loginConfig");

        boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, methodName, user, pass == null ? null : "******", auth, authAlias, loginConfig);

        LinkedHashMap<String, Object> result = new LinkedHashMap<String, Object>();
        try {
            ResourceConfig config = null;
            int authType = "container".equals(auth) ? 0 //
                            : "application".equals(auth) ? 1 //
                                            : -1;
            if (authType >= 0) {
                throw new UnsupportedOperationException("validation of connectionFactory with resource reference not supported yet");
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

                Connection con = cciConFactory.getConnection(); // TODO use ConnectionSpec if user is non-null
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
            }
        } catch (Throwable x) {
            // TODO include error codes?
            result.put("failure", x);
        }

        if (trace && tc.isEntryEnabled())
            Tr.exit(this, tc, methodName, result);
        return result;
    }
}