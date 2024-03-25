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

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.jaxb.JAXBDataBinding;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.jaxws.internal.WebServiceConfigConstants;

/**
 *
 */
public class LibertyWebServiceClientInInterceptor  extends AbstractPhaseInterceptor<Message>  {
    

    private static final TraceComponent tc = Tr.register(LibertyWebServiceClientInInterceptor.class);
    
    /**
     * @param serviceName
     */
    public LibertyWebServiceClientInInterceptor() {
        super(Phase.RECEIVE);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "The LibertyWebServiceClientInInterceptor has been registered to the Interceptor chain.");
        }
    }

    @Override
    public void handleMessage(Message message) throws Fault {
        boolean debug = TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled();
        // Get the serviceName from the message
        String messageServiceName = message.getExchange().getService().getName().getLocalPart();

        if (debug) {
            Tr.debug(tc, "Obtained name of the Service from the message - messageServiceName = " + messageServiceName);
        }
        
        Object enableSchemaValidation;
        
        Object ignoreUnexpectedElements;
        
        // if messageServiceName != null, try to get the values from configuration using it
        if(messageServiceName != null) {
            enableSchemaValidation = WebServicesClientConfigHolder.getEnableSchemaValidation(messageServiceName);
            // if enableSchemaValidation is still null, then we need to try it to get the default setting if its set
            if(enableSchemaValidation == null) {
                WebServicesClientConfigHolder.getEnableSchemaValidation(WebServiceConfigConstants.DEFAULT_PROP);
            }
            
            // if messageServiceName != null, try to get ignoreUnexpectedElements value from configuration, if messageSevice == null then try to get the default configuration value
            ignoreUnexpectedElements = WebServicesClientConfigHolder.getIgnoreUnexpectedElements(messageServiceName);
            
            
            // if ignoreUnexpectedElements is still null then we need to try it to get the default setting if its set
            if(ignoreUnexpectedElements == null) {
                WebServicesClientConfigHolder.getIgnoreUnexpectedElements(WebServiceConfigConstants.DEFAULT_PROP);
            }
        } else {
            // if messageSevice == null then try to get the default configuration values
            enableSchemaValidation = WebServicesClientConfigHolder.getEnableSchemaValidation(WebServiceConfigConstants.DEFAULT_PROP);
            
            ignoreUnexpectedElements = WebServicesClientConfigHolder.getIgnoreUnexpectedElements(WebServiceConfigConstants.DEFAULT_PROP);
        }
        
        // If both values are null, then we can just skip the rest of this interceptor and return
        if(enableSchemaValidation == null && ignoreUnexpectedElements == null) {
            if (debug) {
                Tr.debug(tc, "No webServiceClient configuration found. returning.");
            }
            return;
        }
        

        
        if (debug) {
            Tr.debug(tc, "Found webServiceClient configuration - enableSchemaValidation = " + enableSchemaValidation + ", ignoreUnexpectedElements = " + ignoreUnexpectedElements);
            
        }
        
        // now that we've done pulled the config, we can cast the Objects to proper booleans
        boolean enableSchemaValidationValue = (boolean) enableSchemaValidation;
        boolean ignoreUnexpectedElementsValue = (boolean) ignoreUnexpectedElements;
        
        // If both values are set to true, these are the default behaviors and we can just return. 
        if(enableSchemaValidationValue == true && ignoreUnexpectedElementsValue == false) {
            if (debug) {
                Tr.debug(tc, "No webServiceClient configuration found. returning.");
            }
            return;
        }
        
        if(enableSchemaValidationValue == false) { // since the existing behavior already has a true value, no need to do anything unless its non-default
            // Set the CXF property for enabling schema validation based on the value from our config
            MessageUtils.getContextualBoolean(message, "schema-validation-enabled", (boolean) enableSchemaValidation);
            
            if (debug) {
                Tr.debug(tc, "Set schema-validation-enabled to " + enableSchemaValidation);
                
            }
        }
            
        if(ignoreUnexpectedElementsValue == true) { // since the existing behavior already has a true value, no need to do anything unless its non-default
            // Set the CXF property for enabling schema validation based on the value from our config
            // TODO implement custom Validation Event Handler to ignore only UnexpectedElementExceptions
            MessageUtils.getContextualBoolean(message, JAXBDataBinding.SET_VALIDATION_EVENT_HANDLER, !ignoreUnexpectedElementsValue); // Since we are using our internal property to set a CXF property, we must invert the value
            
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Set JAXBDataBinding.SET_VALIDATION_EVENT_HANDLER to  " + !ignoreUnexpectedElementsValue);
            } 
        }
    }

}
