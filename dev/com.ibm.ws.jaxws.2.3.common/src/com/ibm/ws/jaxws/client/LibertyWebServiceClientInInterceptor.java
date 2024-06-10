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

import io.openliberty.jaxws.jaxb.IgnoreUnexpectedElementValidationEventHandler;

/**
 *
 */
public class LibertyWebServiceClientInInterceptor extends AbstractPhaseInterceptor<Message> {

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

        Object enableSchemaValidation = null;

        Object ignoreUnexpectedElements = null;

        // if messageServiceName != null, try to get the values from configuration using it
        if (messageServiceName != null) {
            // if messageServiceName != null, try to get enableSchemaValidation value from configuration, if it's == null try it to get the default configuration value
            if(WebServicesClientConfigHolder.getEnableSchemaValidation(messageServiceName) != null) {
                
                enableSchemaValidation = WebServicesClientConfigHolder.getEnableSchemaValidation(messageServiceName);
                
            } else if (WebServicesClientConfigHolder.getEnableSchemaValidation(WebServiceConfigConstants.DEFAULT_PROP) != null) {
                
                enableSchemaValidation = WebServicesClientConfigHolder.getEnableSchemaValidation(WebServiceConfigConstants.DEFAULT_PROP);
                
            }
            

            // if messageServiceName != null, try to get ignoreUnexpectedElements value from configuration, if it's == null try to get the default configuration value
            if(WebServicesClientConfigHolder.getIgnoreUnexpectedElements(messageServiceName) != null) {
                
                ignoreUnexpectedElements = WebServicesClientConfigHolder.getIgnoreUnexpectedElements(messageServiceName);
                
            } else if (WebServicesClientConfigHolder.getIgnoreUnexpectedElements(WebServiceConfigConstants.DEFAULT_PROP) != null) {
                
                ignoreUnexpectedElements = WebServicesClientConfigHolder.getIgnoreUnexpectedElements(WebServiceConfigConstants.DEFAULT_PROP);
                
            }

            
        } else {
            // if messageSevice == null then try to get the global configuration values, if its not set keep values null
            enableSchemaValidation = (WebServicesClientConfigHolder.getEnableSchemaValidation(WebServiceConfigConstants.DEFAULT_PROP) != null) ? WebServicesClientConfigHolder.getEnableSchemaValidation(WebServiceConfigConstants.DEFAULT_PROP) : null;

            ignoreUnexpectedElements = (WebServicesClientConfigHolder.getIgnoreUnexpectedElements(WebServiceConfigConstants.DEFAULT_PROP) != null) ? WebServicesClientConfigHolder.getIgnoreUnexpectedElements(WebServiceConfigConstants.DEFAULT_PROP) : null;            

        }
        
        
        if ((enableSchemaValidation == null && ignoreUnexpectedElements == null)) {
            if (debug) {
                Tr.debug(tc, "No webServiceClient configuration found. returning.");
            }
            return;
        }
        

        // Enable or disable schema validation as long as property is non-null
        if ( enableSchemaValidation != null) {
            message.put("schema-validation-enabled", (boolean) enableSchemaValidation);

            if (debug) {
                Tr.debug(tc, "Set schema-validation-enabled to " + (boolean) enableSchemaValidation);

            }
        } else {

            if (debug) {
                Tr.debug(tc, "enableSchemaValdiation was null, not configuring schema-validation-enabled on client the client");

            }
        }
       

        // Set ignoreUnexpectedElements if true
        if (ignoreUnexpectedElements != null && (boolean) ignoreUnexpectedElements == true) {
            // Enable validation handling in CXF
            message.put(JAXBDataBinding.SET_VALIDATION_EVENT_HANDLER, (boolean) ignoreUnexpectedElements);
            
            // Set our custom validation event handler
            IgnoreUnexpectedElementValidationEventHandler unexpectedElementValidationEventHandler = new IgnoreUnexpectedElementValidationEventHandler();
            message.put(JAXBDataBinding.READER_VALIDATION_EVENT_HANDLER, unexpectedElementValidationEventHandler); 

            if (debug) {
                Tr.debug(tc, "Set JAXBDataBinding.SET_VALIDATION_EVENT_HANDLER to  " + (boolean) ignoreUnexpectedElements);
            }

        } else {
            if (debug) {
                Tr.debug(tc, "ignoreUnexpectedElements was " + ignoreUnexpectedElements + " not configuring ignoreUnexpectedElements on the client");

            }
        }
    }

}
