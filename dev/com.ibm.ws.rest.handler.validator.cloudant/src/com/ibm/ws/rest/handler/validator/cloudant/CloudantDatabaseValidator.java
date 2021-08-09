/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.rest.handler.validator.cloudant;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.cloudant.CloudantDatabaseService;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.wsspi.resource.ResourceConfig;
import com.ibm.wsspi.resource.ResourceConfigFactory;
import com.ibm.wsspi.resource.ResourceFactory;
import com.ibm.wsspi.validator.Validator;

@Component(configurationPolicy = ConfigurationPolicy.IGNORE,
           service = { Validator.class },
           property = { "service.vendor=IBM", "com.ibm.wsspi.rest.handler.root=/validation", "com.ibm.wsspi.rest.handler.config.pid=com.ibm.ws.cloudant.cloudantDatabase" })
public class CloudantDatabaseValidator implements Validator {
    private final static TraceComponent tc = Tr.register(CloudantDatabaseValidator.class);

    @Reference
    private ResourceConfigFactory resourceConfigFactory;

    /**
     * Identifies whether the specified query parameter is a valid parameter for the validator.
     * Header parameters such as the user name & password return a false value because they are not query parameters.
     *
     * @param name query parameter name.
     * @return true if a valid parameter for validation. Otherwise false.
     */
    public boolean isParameter(String name) {
        return Validator.AUTH.equals(name)
               || Validator.AUTH_ALIAS.equals(name);
    }

    /**
     * @see com.ibm.wsspi.validator.Validator#validate(java.lang.Object, java.util.Map, java.util.Locale)
     */
    @Override
    @FFDCIgnore(java.lang.NoSuchMethodException.class)
    public LinkedHashMap<String, ?> validate(Object instance, Map<String, Object> props, Locale locale) {
        String auth = (String) props.get(AUTH);
        String authAlias = (String) props.get(AUTH_ALIAS);

        boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "validate", auth, authAlias);

        LinkedHashMap<String, Object> result = new LinkedHashMap<String, Object>();

        try {
            ResourceConfig config = null;
            int authType = AUTH_CONTAINER.equals(auth) ? 0 : AUTH_APPLICATION.equals(auth) ? 1 : -1;
            if (authType >= 0) {
                config = resourceConfigFactory.createResourceConfig("com.cloudant.client.api.Database");
                config.setResAuthType(authType);
                if (authAlias != null && authType == 0)
                    config.addLoginProperty("DefaultPrincipalMapping", authAlias); // set provided auth alias
            }

            Object database = ((ResourceFactory) instance).createResource(config);

            //There isn't anything particularly useful in the DB Info, but invoking the method
            //ensures that the database exists (or it will be created if create="true")
            database.getClass().getMethod("info").invoke(database);

            URI dbURI = (URI) database.getClass().getMethod("getDBUri").invoke(database);
            result.put("uri", dbURI == null ? "null" : dbURI.toString());

            Object cloudantClient = ((CloudantDatabaseService) instance).getCloudantClient(config);

            if (cloudantClient != null) {
                try {
                    Object metaInfo = cloudantClient.getClass().getMethod("metaInformation").invoke(cloudantClient);
                    result.put("serverVersion", metaInfo.getClass().getMethod("getVersion").invoke(metaInfo));
                    Object metaInfoVendor = metaInfo.getClass().getMethod("getVendor").invoke(metaInfo);
                    result.put("vendorName", metaInfoVendor.getClass().getMethod("getName").invoke(metaInfoVendor));
                    String metaInfoVendorVersion = (String) metaInfoVendor.getClass().getMethod("getVersion").invoke(metaInfoVendor);
                    if (metaInfoVendorVersion != null) {
                        result.put("vendorVersion", metaInfoVendorVersion);
                    }
                    String metaInfoVendorVarient = (String) metaInfoVendor.getClass().getMethod("getVariant").invoke(metaInfoVendor);
                    if (metaInfoVendorVarient != null) {
                        result.put("vendorVariant", metaInfoVendorVarient);
                    }
                } catch (NoSuchMethodException ex) {
                    try {
                        result.put("serverVersion", cloudantClient.getClass().getMethod("serverVersion").invoke(cloudantClient));
                    } catch (NoSuchMethodException ignore) {

                    }
                }
            }

        } catch (Throwable x) {
            result.put(FAILURE, x);
        }

        if (trace && tc.isEntryEnabled())
            Tr.exit(this, tc, "validate", result);
        return result;
    }
}