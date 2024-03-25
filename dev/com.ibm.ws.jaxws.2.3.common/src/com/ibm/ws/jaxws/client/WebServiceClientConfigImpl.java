/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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
    
    // Flag tells us if the message for a call to a beta method has been issued
    private static boolean issuedBetaMessage = false;
    
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

    
    
    @Deprecated
    @Activate
    public WebServiceClientConfigImpl(Map<String, Object> properties) {
        betaFenceCheck();
        
        if (tc.isDebugEnabled() && TraceComponent.isAnyTracingEnabled()) {
            Tr.debug(tc, "WebServiceClientConfigImpl activate - " + properties);
        }
        
        if (properties == null) {
            
            if (tc.isDebugEnabled() && TraceComponent.isAnyTracingEnabled()) {
                Tr.debug(tc, "properites are null returning");
            }
            
            return;
        }
        
        String serviceName = getServiceName(properties); // find serviceName
        
        // Add config for serviceName
        WebServicesClientConfigHolder.addConfig(this.toString(), serviceName,
                                                filterProps(properties));
    }
    
    @Deprecated
    @Modified
    protected void modified(Map<String, Object> properties) {        

        betaFenceCheck();
        
        if (tc.isDebugEnabled() && TraceComponent.isAnyTracingEnabled()) {
            Tr.debug(tc, "entering modified - " + properties);
        }
        
        if (properties == null) {
            
            if (tc.isDebugEnabled() && TraceComponent.isAnyTracingEnabled()) {
                Tr.debug(tc, "properites are null returning");
            }
            
            return;
        }
        

        
        // Clear existing config
        WebServicesClientConfigHolder.removeConfig(this.toString());
        
        // Re-add modfied config
        String serviceName = getServiceName(properties);
        WebServicesClientConfigHolder.addConfig(this.toString(), serviceName,
                                                filterProps(properties));
    }

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
}