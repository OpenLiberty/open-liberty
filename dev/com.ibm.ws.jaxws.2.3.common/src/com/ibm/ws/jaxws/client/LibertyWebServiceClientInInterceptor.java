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

import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.jaxb.JAXBDataBinding;
import org.apache.cxf.message.Message;
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
        
        Object enableDefaultValidation = null; // Liberty change

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

            // Liberty change begin
            // if messageServiceName != null, try to get the values from configuration using it
            if (messageServiceName != null) {
                // if messageServiceName != null, try to get getEnableDefaultValidation value from configuration, if it's == null try it to get the default configuration value
                if (WebServicesClientConfigHolder.getEnableDefaultValidation(messageServiceName) != null) {
                    
                    enableDefaultValidation = WebServicesClientConfigHolder.getEnableDefaultValidation(messageServiceName);
                    
                } else if (WebServicesClientConfigHolder.getEnableDefaultValidation(WebServiceConfigConstants.DEFAULT_PROP) != null) {
                    
                    enableDefaultValidation = WebServicesClientConfigHolder.getEnableDefaultValidation(WebServiceConfigConstants.DEFAULT_PROP);
                }
            }
            // Liberty change end
        } else {
            // if messageSevice == null then try to get the global configuration values, if its not set keep values null
            enableSchemaValidation = (WebServicesClientConfigHolder.getEnableSchemaValidation(WebServiceConfigConstants.DEFAULT_PROP) != null) ? WebServicesClientConfigHolder.getEnableSchemaValidation(WebServiceConfigConstants.DEFAULT_PROP) : null;

            ignoreUnexpectedElements = (WebServicesClientConfigHolder.getIgnoreUnexpectedElements(WebServiceConfigConstants.DEFAULT_PROP) != null) ? WebServicesClientConfigHolder.getIgnoreUnexpectedElements(WebServiceConfigConstants.DEFAULT_PROP) : null;
            
            enableDefaultValidation = (WebServicesClientConfigHolder.getEnableDefaultValidation(WebServiceConfigConstants.DEFAULT_PROP) != null) ? WebServicesClientConfigHolder.getEnableDefaultValidation(WebServiceConfigConstants.DEFAULT_PROP) : null; // Liberty change

        }

        
        if ((enableSchemaValidation == null && ignoreUnexpectedElements == null && enableDefaultValidation == null)) { // Liberty change
            if (debug) {
                Tr.debug(tc, "No webServiceClient configuration found. returning.");
            }
            return;
        }
        

        // As long as property is non-null:
        // Enable enhanced schema validation if true, or disable it along with default validation if false 
        if ( enableSchemaValidation != null) {
            if ((boolean) enableSchemaValidation == true) {
                // enable Schema Validation 
                message.put("schema-validation-enabled", true);
                
                if (debug) {
                    Tr.debug(tc, "Set schema-validation-enabled to " + true);

                }
            } else if ((boolean) enableSchemaValidation == false) {
                // Make sure schema validation is disabled
                message.put("schema-validation-enabled", false);

                
                if (debug) {
                    Tr.debug(tc, "Set schema-validation-enabled to " + false + " and " + JAXBDataBinding.SET_VALIDATION_EVENT_HANDLER + " to " + false);

                }
            }
        } else {

            if (debug) {
                Tr.debug(tc, "enableSchemaValdiation was null, not configuring schema-validation-enabled on client the client");

            }
        }
       

        // Set ignoreUnexpectedElements if true
        if (ignoreUnexpectedElements != null && (boolean) ignoreUnexpectedElements == true) {
            
                // Enable validation handling in CXF
                message.put(JAXBDataBinding.SET_VALIDATION_EVENT_HANDLER, true);

                // Set our custom validation event handler
                IgnoreUnexpectedElementValidationEventHandler unexpectedElementValidationEventHandler = new IgnoreUnexpectedElementValidationEventHandler();
                message.put(JAXBDataBinding.READER_VALIDATION_EVENT_HANDLER, unexpectedElementValidationEventHandler);
                
                if (enableDefaultValidation != null && (boolean)enableDefaultValidation == false) {
                  // If ignoreUnexpectedElements is true, do not let  enableDefaultValidation false value 
                  // to not set JAXBDataBinding.SET_VALIDATION_EVENT_HANDLER to false
                  return;   
                } 
            
            if (debug) {
                Tr.debug(tc, "Set JAXBDataBinding.SET_VALIDATION_EVENT_HANDLER to  " + (boolean) ignoreUnexpectedElements + " for ignoreUnexpectedElements");
                
            }
            
        } else {
            if (debug) {
                Tr.debug(tc, "ignoreUnexpectedElements was " + ignoreUnexpectedElements + " not configuring ignoreUnexpectedElements on the client");
                
            }
        }

        // As long as property is non-null:
        // Enable default validation if true, or disable it along with default validation if false 
        if (enableDefaultValidation != null) {
            // JAXB's DefaultValidationEventHandler 
            message.put(JAXBDataBinding.SET_VALIDATION_EVENT_HANDLER, enableDefaultValidation);
            
            if (debug) {
                Tr.debug(tc, "Set JAXBDataBinding.SET_VALIDATION_EVENT_HANDLER to " + enableDefaultValidation + " for enableDefaultValidation");
            }
            
        } else {
            if (debug) {
                Tr.debug(tc, "enableDefaultValidation was " + enableDefaultValidation + " not configuring enableDefaultValidation on the client");
                
            }
        }
    }

}
