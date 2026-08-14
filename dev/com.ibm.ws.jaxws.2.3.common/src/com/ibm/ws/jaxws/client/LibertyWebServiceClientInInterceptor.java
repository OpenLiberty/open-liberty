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
 * A CXF-style inbound interceptor on the client-side that applies webServiceClient configuration
 * from the Liberty server.xml at the RECEIVE phase of the interceptor chain.
 *
 * <p>On each incoming SOAP response message it resolves the active configuration values
 * (ignoreUnexpectedElements, enableSchemaValidation, enableDefaultValidation) for the
 * calling service, then applies the corresponding CXF/JAXB message properties. Named
 * configurations (matched by serviceName) take precedence over the global default.</p>
 *
 * <p>Info-level NLS messages are emitted once per JVM lifetime via static flags to avoid
 * log flooding; subsequent activations are recorded at debug level only.</p>
 */
public class LibertyWebServiceClientInInterceptor extends AbstractPhaseInterceptor<Message> {

    private static final TraceComponent tc = Tr.register(LibertyWebServiceClientInInterceptor.class);

    /** Ensures CWWKW0064I / CWWKW0066I is logged only once across all requests. */
    private static boolean issuedIgnoreUnexpectedElementsMessage = false;

    /** Ensures CWWKW0067I / CWWKW0068I is logged only once across all requests. */
    private static boolean issuedSchemaValidationMessage = false;

    /**
     * Registers this interceptor at the CXF RECEIVE phase so it runs as soon as
     * an inbound message arrives, before unmarshalling.
     */
    public LibertyWebServiceClientInInterceptor() {
        super(Phase.RECEIVE);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "The LibertyWebServiceClientInInterceptor has been registered to the Interceptor chain.");
        }
    }

    /**
     * Applies webServiceClient configuration properties to the inbound CXF message.
     *
     * <p>Resolution order for each property:
     * <ol>
     *   <li>Named config matching the service name of the message</li>
     *   <li>Global (default) config if no named match is found</li>
     *   <li>No-op if neither exists</li>
     * </ol>
     *
     * @param message the inbound CXF message
     * @throws Fault if a CXF-level fault occurs
     */
    @Override
    public void handleMessage(Message message) throws Fault {
        boolean debug = TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled();
        
        // Skip execution of the rest when no configuration found
        if(!WebServicesClientConfigHolder.isConfigExists())      {
            if (debug) {
                Tr.debug(tc, "No configuration found. Returning.");
            }
            return;
        }
        
        // Get the serviceName from the message
        String messageServiceName = message.getExchange().getService().getName().getLocalPart();

        if (debug) {
            Tr.debug(tc, "Obtained name of the Service from the message - messageServiceName = " + messageServiceName);
        }

        Object enableSchemaValidation = null;

        Object ignoreUnexpectedElements = null;
        
        Object enableDefaultValidation = null;

        boolean ignoreUnexpectedResolvedFromNamed = false;
        boolean schemaValidationResolvedFromNamed = false;

        // if messageServiceName != null, try to get the values from configuration using it
        if (messageServiceName != null) {
            // if messageServiceName != null, try to get enableSchemaValidation value from configuration, if it's == null try it to get the default configuration value
            if(WebServicesClientConfigHolder.getEnableSchemaValidation(messageServiceName) != null) {
                
                enableSchemaValidation = WebServicesClientConfigHolder.getEnableSchemaValidation(messageServiceName);
                schemaValidationResolvedFromNamed = true;
                
            } else if (WebServicesClientConfigHolder.getEnableSchemaValidation(WebServiceConfigConstants.DEFAULT_PROP) != null) {
                
                enableSchemaValidation = WebServicesClientConfigHolder.getEnableSchemaValidation(WebServiceConfigConstants.DEFAULT_PROP);
                
            }
            

            // if messageServiceName != null, try to get ignoreUnexpectedElements value from configuration, if it's == null try to get the default configuration value
            if(WebServicesClientConfigHolder.getIgnoreUnexpectedElements(messageServiceName) != null) {
                
                ignoreUnexpectedElements = WebServicesClientConfigHolder.getIgnoreUnexpectedElements(messageServiceName);
                ignoreUnexpectedResolvedFromNamed = true;
                
            } else if (WebServicesClientConfigHolder.getIgnoreUnexpectedElements(WebServiceConfigConstants.DEFAULT_PROP) != null) {
                
                ignoreUnexpectedElements = WebServicesClientConfigHolder.getIgnoreUnexpectedElements(WebServiceConfigConstants.DEFAULT_PROP);
                
            }

            // if messageServiceName != null, try to get getEnableDefaultValidation value from configuration, if it's == null try it to get the default configuration value
            if (WebServicesClientConfigHolder.getEnableDefaultValidation(messageServiceName) != null) {

                enableDefaultValidation = WebServicesClientConfigHolder.getEnableDefaultValidation(messageServiceName);

            } else if (WebServicesClientConfigHolder.getEnableDefaultValidation(WebServiceConfigConstants.DEFAULT_PROP) != null) {

                enableDefaultValidation = WebServicesClientConfigHolder.getEnableDefaultValidation(WebServiceConfigConstants.DEFAULT_PROP);
            }
            
        } else {
            // if messageSevice == null then try to get the global configuration values, if its not set keep values null
            enableSchemaValidation = (WebServicesClientConfigHolder.getEnableSchemaValidation(WebServiceConfigConstants.DEFAULT_PROP) != null) ? WebServicesClientConfigHolder.getEnableSchemaValidation(WebServiceConfigConstants.DEFAULT_PROP) : null;

            ignoreUnexpectedElements = (WebServicesClientConfigHolder.getIgnoreUnexpectedElements(WebServiceConfigConstants.DEFAULT_PROP) != null) ? WebServicesClientConfigHolder.getIgnoreUnexpectedElements(WebServiceConfigConstants.DEFAULT_PROP) : null;            

            enableDefaultValidation = (WebServicesClientConfigHolder.getEnableDefaultValidation(WebServiceConfigConstants.DEFAULT_PROP) != null) ? WebServicesClientConfigHolder.getEnableDefaultValidation(WebServiceConfigConstants.DEFAULT_PROP) : null;

        }

        
        if ((enableSchemaValidation == null && ignoreUnexpectedElements == null && enableDefaultValidation == null)) {
            if (debug) {
                Tr.debug(tc, "No webServiceClient configuration found. returning.");
            }
            return;
        }
        
        if (debug) {
            Tr.debug(tc, "enableSchemaValidation   value: " + enableSchemaValidation);
            Tr.debug(tc, "ignoreUnexpectedElements value: " + ignoreUnexpectedElements);
            Tr.debug(tc, "enableDefaultValidation  value: " + enableDefaultValidation);
        }
        
        // As long as property is non-null:
        // Enable enhanced schema validation if true, or disable it along with default validation if false 
        if ( enableSchemaValidation != null) {
            if ((boolean) enableSchemaValidation == true) {
                // enable Schema Validation
                message.put("schema-validation-enabled", true);

                if (!issuedSchemaValidationMessage) {
                    if (schemaValidationResolvedFromNamed) {
                        Tr.info(tc, "info.schema.validation.client.named", messageServiceName); // CWWKW0068I
                    } else {
                        Tr.info(tc, "info.schema.validation.global"); // CWWKW0067I
                    }
                    issuedSchemaValidationMessage = true;
                } else if (debug) {
                    Tr.debug(tc, "enableSchemaValidation is active for service " + messageServiceName + " (message already issued)");
                }

                if (debug) {
                    Tr.debug(tc, "Set schema-validation-enabled to " + true);

                }
            } else if ((boolean) enableSchemaValidation == false) {
                // Make sure schema validation is disabled
                message.put("schema-validation-enabled", false);

                
                if (debug) {
                    Tr.debug(tc, "Set schema-validation-enabled to " + false);

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

            if (!issuedIgnoreUnexpectedElementsMessage) {
                if (ignoreUnexpectedResolvedFromNamed) {
                    Tr.info(tc, "info.ignore.unexpected.elements.client.named", messageServiceName); // CWWKW0064I
                } else {
                    Tr.info(tc, "info.ignore.unexpected.elements.global"); // CWWKW0066I
                }
                issuedIgnoreUnexpectedElementsMessage = true;
            } else if (debug) {
                Tr.debug(tc, "ignoreUnexpectedElements is active for service " + messageServiceName + " (message already issued)");
            }

            if (debug) {
                Tr.debug(tc, "Set JAXBDataBinding.SET_VALIDATION_EVENT_HANDLER to  " + (boolean) ignoreUnexpectedElements + " for ignoreUnexpectedElements");
            }

            if (enableDefaultValidation != null && (boolean) enableDefaultValidation == false) {
                // If ignoreUnexpectedElements is true, do not let  enableDefaultValidation false value 
                // to not set JAXBDataBinding.SET_VALIDATION_EVENT_HANDLER to false
                return;
            }

        } else {
            if (debug) {
                Tr.debug(tc, "ignoreUnexpectedElements was " + ignoreUnexpectedElements + " not configuring ignoreUnexpectedElements on the client");

            }
        }
        
        // As long as property is non-null:
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
