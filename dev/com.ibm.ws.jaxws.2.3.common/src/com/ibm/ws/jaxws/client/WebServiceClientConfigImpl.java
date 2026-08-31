/*******************************************************************************
 * Copyright (c) 2024, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.jaxws.client;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.jaxws.internal.ConfigValidation;
import com.ibm.ws.jaxws.internal.WebServiceConfig;
import com.ibm.ws.jaxws.internal.WebServiceConfigConstants;
import com.ibm.ws.kernel.productinfo.ProductInfo;

/**
 * The declarative service responsible for processing a given <webServiceClient> element in the server.xml
 * Adapted from com.ibm.ws.jaxrs20.clientconfig.JAXRSClientConfig
 */
@Component(configurationPid = "com.ibm.ws.jaxws.clientConfig",
           configurationPolicy = ConfigurationPolicy.REQUIRE, // Must be ConfigurationPolicy.REQUIRE to prevent the DS being activated without the configuration present. 
           service = { WebServiceConfig.class },
           immediate = true,
           property = { "service.vendor=IBM" })
public class WebServiceClientConfigImpl extends WebServiceConfig {
    private static final TraceComponent tc = Tr.register(WebServiceClientConfigImpl.class);

    static {
        // remove the serviceName from the properties to be checked, since we use the serviceName as a key
        propertiesToRemove.add(WebServiceConfigConstants.SERVICE_NAME_PROP);
    }
    
    /** Ensures the beta-edition info message is logged only once per class. */
    private static boolean issuedBetaMessage = false;

    /**
     * Guards all lifecycle methods against invocation outside of a beta Liberty edition.
     * Throws {@link UnsupportedOperationException} on non-beta builds; logs a one-time
     * debug message on beta builds.
     *
     * @throws UnsupportedOperationException if not running a beta edition
     */
    private void betaFenceCheck() throws UnsupportedOperationException {
        // Not running beta edition, throw exception
        if (!ProductInfo.getBetaEdition()) { 
            throw new UnsupportedOperationException("The webServiceClient configuration is in beta and is not available.");
        } else {
        // Running beta exception, issue message if we haven't already issued one for this class
            if (!issuedBetaMessage) {
                Tr.info(tc, "BETA: A webServiceClient configuration beta method has been invoked for the class " + this.getClass().getName() + " for the first time.");
                issuedBetaMessage = !issuedBetaMessage;
           }
        }
    }

    
    
    /**
     * OSGi DS activation constructor. Called when a {@code <webServiceClient>} element
     * is added to the server.xml. Validates the properties, filters them to only the
     * recognised config attributes, and registers them with {@link WebServicesClientConfigHolder}
     * keyed by serviceName (or the global default key if no serviceName is specified).
     *
     * @param properties the component properties supplied by OSGi config admin
     */
    @Deprecated
    @Activate
    public WebServiceClientConfigImpl(Map<String, Object> properties) {
        betaFenceCheck();
        
        if (tc.isDebugEnabled() && TraceComponent.isAnyTracingEnabled()) {
            Tr.debug(tc, "WebServiceClientConfigImpl activate - " + properties);
        }
        
        if (!WebServicesClientConfigHolder.checkConfig(properties)) {
            return;
        }
        
        String serviceName = getServiceName(properties); // find serviceName
        
        // Add info message for config without any attribute set
        Map<String, Object> filteredProperties = filterProps(properties);
        if (filteredProperties.isEmpty()) {
            Tr.info(tc, "info.no.attributes.webserviceclient");
        }

         // Add config for serviceName
        WebServicesClientConfigHolder.addConfig(this.toString(), serviceName, filteredProperties);
    }
    
    /**
     * OSGi DS modification callback. Called when an existing {@code <webServiceClient>}
     * element in the server.xml is changed. Removes the previous registration and
     * re-registers with the updated properties.
     *
     * @param properties the updated component properties supplied by OSGi config admin
     */
    @Deprecated
    @Modified
    protected void modified(Map<String, Object> properties) {

        betaFenceCheck();
        
        if (tc.isDebugEnabled() && TraceComponent.isAnyTracingEnabled()) {
            Tr.debug(tc, "entering modified - " + properties);
        }
        
        if (!WebServicesClientConfigHolder.checkConfig(properties)) {
            return;
        }
        // Clear existing config
        WebServicesClientConfigHolder.removeConfig(this.toString());
        
        // Re-add modfied config
        String serviceName = getServiceName(properties);

        // Add info message for config without any attribute set
        Map<String, Object> filteredProperties = filterProps(properties);
        if (filteredProperties.isEmpty()) {
            Tr.info(tc, "info.no.attributes.webserviceclient");
        }

         // Add config for serviceName
        WebServicesClientConfigHolder.addConfig(this.toString(), serviceName, filteredProperties);
    }

    /**
     * OSGi DS deactivation callback. Called when the {@code <webServiceClient>} element
     * is removed from the server.xml. Removes the registration from
     * {@link WebServicesClientConfigHolder}.
     */
    @Deprecated
    @Deactivate
    protected void deactivate() {
        

        betaFenceCheck();
        
        if (tc.isDebugEnabled() && TraceComponent.isAnyTracingEnabled()) {
            Tr.debug(tc, "entering deactivate");
        }
        WebServicesClientConfigHolder.removeConfig(this.toString());
    }

    /**
     * given the map of properties, remove ones we don't care about, and translate
     * some others. If it's not one we're familiar with, transfer it unaltered
     *
     * @param props - input list of properties
     * @return - a new Map of the filtered properties.
     */
    protected Map<String, Object> filterProps(Map<String, Object> props) {
        HashMap<String, Object> filteredProps = new HashMap<>();
        Iterator<String> it = props.keySet().iterator();

        while (it.hasNext()) {
            String key = it.next();
            
            if(key == null) {
                continue;
            }

            if (tc.isDebugEnabled() && TraceComponent.isAnyTracingEnabled()) {
                Tr.debug(tc, "key: " + key + " value: " + props.get(key) + " of type " + props.get(key).getClass());
            }
            // skip stuff we don't care about
            if (propertiesToRemove.contains(key)) {
                continue;
            }
            if (key.compareTo(WebServiceConfigConstants.ENABLE_SCHEMA_VALIDATION_PROP) == 0) {
                if (!ConfigValidation.validateEnableSchemaValidation((boolean) props.get(key)))
                    continue;
            }
            if (key.compareTo(WebServiceConfigConstants.IGNORE_UNEXPECTED_ELEMENTS_PROP) == 0) {
                if (!ConfigValidation.validateIgnoreUnexpectedElements((boolean) props.get(key)))
                    continue;
            }
            if (key.compareTo(WebServiceConfigConstants.ENABLE_DEFAULT_VALIDATION_PROP) == 0) {
                if (!ConfigValidation.validateEnableDefaultValidation((boolean) props.get(key)))
                    continue;
            }

            filteredProps.put(key, props.get(key));

        }
        return filteredProps;
    }

    /**
     * find the serviceName parameter which we will key off of
     *
     * @param props
     * @return value of serviceName param within props, or null if no serviceName param
     */
    private String getServiceName(Map<String, Object> props) {
        if (props == null)
            return null;
        if (props.keySet().contains(WebServiceConfigConstants.SERVICE_NAME_PROP)) {
            return (props.get(WebServiceConfigConstants.SERVICE_NAME_PROP).toString());
        } else {
            return WebServiceConfigConstants.DEFAULT_PROP;
        }
    }

    /**
     * Returns whether any webServiceClient configuration has been registered.
     * Exposed for unit test use only.
     *
     * @return {@code true} if at least one configuration entry exists
     */
    protected boolean isConfigExists() {
        return WebServicesClientConfigHolder.isConfigExists();
    }
}