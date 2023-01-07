/*******************************************************************************
 * Copyright (c) 2013, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jca.utils.metagen;

import java.util.List;

import javax.resource.spi.Activation;
import javax.resource.spi.ConnectionDefinition;
import javax.resource.spi.InvalidPropertyException;
import javax.resource.spi.UnavailableException;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.jca.utils.xml.ra.RaActivationSpec;
import com.ibm.ws.jca.utils.xml.ra.RaAdminObject;
import com.ibm.ws.jca.utils.xml.ra.RaConfigProperty;
import com.ibm.ws.jca.utils.xml.ra.RaConnectionDefinition;
import com.ibm.ws.jca.utils.xml.ra.RaConnector;
import com.ibm.ws.jca.utils.xml.ra.RaInboundResourceAdapter;
import com.ibm.ws.jca.utils.xml.ra.RaMessageAdapter;
import com.ibm.ws.jca.utils.xml.ra.RaMessageListener;
import com.ibm.ws.jca.utils.xml.ra.RaOutboundResourceAdapter;
import com.ibm.ws.jca.utils.xml.ra.RaResourceAdapter;
import com.ibm.ws.jca.utils.xml.wlp.ra.WlpRaActivationSpec;
import com.ibm.ws.jca.utils.xml.wlp.ra.WlpRaAdminObject;
import com.ibm.ws.jca.utils.xml.wlp.ra.WlpRaConfigProperty;
import com.ibm.ws.jca.utils.xml.wlp.ra.WlpRaConnectionDefinition;
import com.ibm.ws.jca.utils.xml.wlp.ra.WlpRaConnector;
import com.ibm.ws.jca.utils.xml.wlp.ra.WlpRaInboundResourceAdapter;
import com.ibm.ws.jca.utils.xml.wlp.ra.WlpRaMessageAdapter;
import com.ibm.ws.jca.utils.xml.wlp.ra.WlpRaMessageListener;
import com.ibm.ws.jca.utils.xml.wlp.ra.WlpRaOutboundResourceAdapter;
import com.ibm.ws.jca.utils.xml.wlp.ra.WlpRaResourceAdapter;

public class DeploymentDescriptorMerger {
    private static final TraceComponent tc = Tr.register(DeploymentDescriptorParser.class);

    private static String format(String msgCode, Object ...parms) {
        return Tr.formatMessage(tc, msgCode, parms);
    }
    
    private static UnavailableException missingElementException(
            String elementName, String typeName, Class<?> elementClass,
            String adapterName) {
        return unavailableException(
                "J2CA9911.missing.matching.type",
                elementName, typeName, elementClass.getSimpleName(), adapterName);
    }
        
    private static UnavailableException unavailableException(String msgId, Object... parms) {
        return new UnavailableException( format(msgId, parms) );
    }
    
    private static InvalidPropertyException propertyException(String msgId, String pName, String adapterName) {
        String  msg = format(msgId, pName, adapterName);
        return new InvalidPropertyException(msg);
    }
    
    // TODO: CJN:
    //
    // raConnector should be the ra.xml combined with annotations and javabeans,
    // if any, before being called since it can be used to override any of those?  Need to
    // look for all places where this is invoked.
    //
    // It's probably ok to merge ra.xml + wlp-ra.xml before doing annotations/javabeans?
    // That will happen in ConnectorAdapter

    /**
     * Merged data from a WLP resource adapter deployment descriptor into data from
     * a standard resource resource adapter deployment descriptor.
     * 
     * @param adapterName The name of the resource adapter archive relative to which
     *     the merge is taking place.  Used in error messages.
     * @param raConnector The data into which the wlp data is merged.
     * @param wlpRaConnector WLP data which is to be merged.
     * 
     * @throws InvalidPropertyException Thrown if a property within the WLP data does not
     *     exist in the merge data.
     * @throws UnavailableException Thrown if an element of the WLP does not find a matching
     *     element in the merge data.
     */
    public static void merge(String adapterName, RaConnector raConnector, WlpRaConnector wlpRaConnector)
        throws InvalidPropertyException, UnavailableException {

        raConnector.copyWlpSettings(wlpRaConnector);

        WlpRaResourceAdapter wlpResourceAdapter = wlpRaConnector.getResourceAdapter();
        if (wlpResourceAdapter == null) {
            return;
        }
        
        RaResourceAdapter raResourceAdapter = raConnector.getResourceAdapter();
        raResourceAdapter.copyWlpSettings(wlpResourceAdapter);
        mergeProperties(adapterName, wlpResourceAdapter, raResourceAdapter, raWidget);

        List<WlpRaAdminObject> wlpAdminObjects = wlpResourceAdapter.getAdminObjects();
        if (wlpAdminObjects != null) {
            for (WlpRaAdminObject wlpAdminObject : wlpAdminObjects) {
                String aoInterface = wlpAdminObject.getAdminObjectInterface();
                String aoClass = wlpAdminObject.getAdminObjectClass();
                
                RaAdminObject raAdminObject = raResourceAdapter.getAdminObject(aoInterface, aoClass);
                if (raAdminObject == null) {
                    throw unavailableException("J2CA9910.missing.matching.adminobject", aoInterface, aoClass, adapterName);
                } else {
                    raAdminObject.copyWlpSettings(wlpAdminObject);
                    mergeProperties(adapterName, wlpAdminObject, raAdminObject, aoWidget);
                }
            }
        }

        WlpRaOutboundResourceAdapter wlpOutboundAdapter = wlpResourceAdapter.getOutboundResourceAdapter();
        if (wlpOutboundAdapter != null) {
            RaOutboundResourceAdapter raOutboundAdapter = raResourceAdapter.getOutboundResourceAdapter();

            List<WlpRaConnectionDefinition> wlpConnectionDefinitions = wlpOutboundAdapter.getConnectionDefinitions();
            if (wlpConnectionDefinitions != null) {
                for (WlpRaConnectionDefinition wlpConnectionDefinition : wlpConnectionDefinitions) {
                    RaConnectionDefinition raConnectionDefinition = raOutboundAdapter.getConnectionDefinitionByInterface(wlpConnectionDefinition.getConnectionFactoryInterface());
                    if (raConnectionDefinition == null) {
                        throw missingElementException(
                                "connection-definition", wlpConnectionDefinition.getConnectionFactoryInterface(),
                                ConnectionDefinition.class, adapterName);
                    } else {
                        raConnectionDefinition.copyWlpSettings(wlpConnectionDefinition);
                        mergeProperties(adapterName, wlpConnectionDefinition, raConnectionDefinition, cdWidget);
                    }
                }
            }
        }

        WlpRaInboundResourceAdapter wlpInboundAdapter = wlpResourceAdapter.getInboundResourceAdapter();
        if (wlpInboundAdapter != null) {
            RaInboundResourceAdapter raInboundAdapter = raResourceAdapter.getInboundResourceAdapter();

            WlpRaMessageAdapter wlpMessageAdapter = wlpInboundAdapter.getMessageAdapter();
            if (wlpMessageAdapter != null) {
                RaMessageAdapter raMessageAdapter = raInboundAdapter.getMessageAdapter();
                
                List<WlpRaMessageListener> wlpMessageListeners = wlpMessageAdapter.getMessageListeners();
                if (wlpMessageListeners != null) {
                    for (WlpRaMessageListener wlpMessageListener : wlpMessageListeners) {
                        RaMessageListener raMessageListener =
                            ((raMessageAdapter == null) ? null : raMessageAdapter.getMessageListenerByType(wlpMessageListener.getMessageListenerType()));
                        if (raMessageListener == null) {
                            throw missingElementException(
                                    "messagelistener", wlpMessageListener.getMessageListenerType(),
                                    Activation.class, adapterName);
                        } else {
                            raMessageListener.copyWlpSettings(wlpMessageListener);

                            WlpRaActivationSpec wlpActivationSpec = wlpMessageListener.getActivationSpec();
                            if (wlpActivationSpec != null) {
                                RaActivationSpec raActivationSpec = raMessageListener.getActivationSpec();
                                if (raActivationSpec == null) {
                                    throw missingElementException(
                                            "activationspec", raMessageListener.getMessageListenerType(),
                                            Activation.class, adapterName);
                                } else {
                                    mergeProperties(adapterName, wlpActivationSpec, raActivationSpec, asWidget);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private static final RA_Widget raWidget = new RA_Widget();
    private static final AO_Widget aoWidget = new AO_Widget();
    private static final CD_Widget cdWidget = new CD_Widget();    
    private static final AS_Widget asWidget = new AS_Widget();    
        
    public interface PWidget<SourceT, SinkT> {
        List<WlpRaConfigProperty> getSourceProperties(SourceT pHolder);
        List<RaConfigProperty> getSinkProperties(SinkT pHolder);
        boolean isPresent(SinkT pHolder, String pName);
        RaConfigProperty getSinkProperty(SinkT pHolder, String pName);
    }

    public static class RA_Widget implements PWidget<WlpRaResourceAdapter, RaResourceAdapter> {
        @Override
        public List<WlpRaConfigProperty> getSourceProperties(WlpRaResourceAdapter pHolder) {
            return pHolder.getConfigProperties();
        }

        @Override
        public List<RaConfigProperty> getSinkProperties(RaResourceAdapter pHolder) {
            return pHolder.getConfigProperties();
        }

        @Override
        public boolean isPresent(RaResourceAdapter pHolder, String pName) {
            return pHolder.isConfigPropertyAlreadyDefined(pName);
        }

        @Override
        public RaConfigProperty getSinkProperty(RaResourceAdapter pHolder, String pName) {
            return pHolder.getConfigPropertyById(pName);
        }
    }

    public static class AO_Widget implements PWidget<WlpRaAdminObject, RaAdminObject> {
        @Override
        public List<WlpRaConfigProperty> getSourceProperties(WlpRaAdminObject pHolder) {
            return pHolder.getConfigProperties();
        }

        @Override
        public List<RaConfigProperty> getSinkProperties(RaAdminObject pHolder) {
            return pHolder.getConfigProperties();
        }

        @Override
        public boolean isPresent(RaAdminObject pHolder, String pName) {
            return pHolder.isConfigPropertyAlreadyDefined(pName);
        }

        @Override
        public RaConfigProperty getSinkProperty(RaAdminObject pHolder, String pName) {
            return pHolder.getConfigPropertyById(pName);
        }                
    }    

    public static class CD_Widget implements PWidget<WlpRaConnectionDefinition, RaConnectionDefinition> {
        @Override
        public List<WlpRaConfigProperty> getSourceProperties(WlpRaConnectionDefinition pHolder) {
            return pHolder.getConfigProperties();
        }

        @Override
        public List<RaConfigProperty> getSinkProperties(RaConnectionDefinition pHolder) {
            return pHolder.getConfigProperties();
        }

        @Override
        public boolean isPresent(RaConnectionDefinition pHolder, String pName) {
            return pHolder.isConfigPropertyAlreadyDefined(pName);
        }

        @Override
        public RaConfigProperty getSinkProperty(RaConnectionDefinition pHolder, String pName) {
            return pHolder.getConfigPropertyById(pName);
        }                
    }    

    public static class AS_Widget implements PWidget<WlpRaActivationSpec, RaActivationSpec> {
        @Override
        public List<WlpRaConfigProperty> getSourceProperties(WlpRaActivationSpec pHolder) {
            return pHolder.getConfigProperties();
        }

        @Override
        public List<RaConfigProperty> getSinkProperties(RaActivationSpec pHolder) {
            return pHolder.getConfigProperties();
        }

        @Override
        public boolean isPresent(RaActivationSpec pHolder, String pName) {
            return pHolder.isConfigPropertyAlreadyDefined(pName);
        }
        
        @Override
        public RaConfigProperty getSinkProperty(RaActivationSpec pHolder, String pName) {
            return pHolder.getConfigPropertyById(pName);
        }        
    }

    private static <SourceT, SinkT> void mergeProperties(
            String adapterName,
            SourceT source, SinkT sink, PWidget<SourceT, SinkT> pWidget)
            throws InvalidPropertyException, UnavailableException {

        List<WlpRaConfigProperty> sourceProperties = pWidget.getSourceProperties(source);
        if ( sourceProperties == null ) {
            return;
        }
        
        for ( WlpRaConfigProperty sourceProperty : sourceProperties ) {
            String pName = sourceProperty.getWlpPropertyName();                            
            if ( sourceProperty.addWlpPropertyToMetatype() ) {
                if ( pWidget.isPresent(sink, pName) ) {
                    throw propertyException("J2CA9908.duplicate.copy", pName, adapterName);                                    
                } else {
                    RaConfigProperty sinkProperty = new RaConfigProperty();
                    sinkProperty.copyWlpSettings(sourceProperty);
                    pWidget.getSinkProperties(sink).add(sinkProperty);
                }

            } else {
                RaConfigProperty sinkProperty = pWidget.getSinkProperty(sink, pName);
                if ( sinkProperty == null ) {
                    throw unavailableException("J2CA9909.missing.matching.config.prop", pName, adapterName);
                } else {
                    sinkProperty.copyWlpSettings(sourceProperty);
                }
            }
        }
    }        
}
