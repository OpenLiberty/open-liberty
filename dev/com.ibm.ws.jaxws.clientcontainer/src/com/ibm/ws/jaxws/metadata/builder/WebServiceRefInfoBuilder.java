/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxws.metadata.builder;

import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.WebServiceClient;
import javax.xml.ws.WebServiceRef;
import javax.xml.ws.soap.AddressingFeature;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.javaee.dd.common.wsclient.Addressing;
import com.ibm.ws.javaee.dd.common.wsclient.HandlerChain;
import com.ibm.ws.javaee.dd.common.wsclient.PortComponentRef;
import com.ibm.ws.javaee.dd.common.wsclient.ServiceRef;
import com.ibm.ws.jaxws.client.injection.WebServiceRefSimulator;
import com.ibm.ws.jaxws.metadata.AddressingFeatureInfo;
import com.ibm.ws.jaxws.metadata.MTOMFeatureInfo;
import com.ibm.ws.jaxws.metadata.PortComponentRefInfo;
import com.ibm.ws.jaxws.metadata.RespectBindingFeatureInfo;
import com.ibm.ws.jaxws.metadata.WebServiceRefInfo;
import com.ibm.ws.jaxws.metadata.WebServiceRefPartialInfo;
import com.ibm.ws.jaxws.utils.StringUtils;

/**
 * The class is responsible for building WebServiceRefInfo based on the annotation or ServiceRef from deployment descriptor.
 */
public class WebServiceRefInfoBuilder {

    private static final TraceComponent tc = Tr.register(WebServiceRefInfoBuilder.class);

    public static WebServiceRefInfo buildWebServiceRefInfo(WebServiceRef anno, ClassLoader classLoader) {
        WebServiceRefInfo webServiceRefInfo = new WebServiceRefInfo(anno);
        return webServiceRefInfo;
    }

    public static WebServiceRefInfo buildWebServiceRefInfo(ServiceRef serviceRef, ClassLoader classloader) throws ClassNotFoundException {
        if (serviceRef == null) {
            return null;
        }

        // The <service-interface> and <service-ref-type> classes.
        // We'll use their default annotation values here.
        Class<?> srvInterfaceClass = Service.class;
        Class<?> srvRefTypeClass = Object.class;

        if (serviceRef.getServiceInterfaceName() != null) {
            srvInterfaceClass = Class.forName(serviceRef.getServiceInterfaceName(), false, classloader);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Loaded service-interface class from DD: " + srvInterfaceClass.getName());
            }
        }

        if (serviceRef.getServiceRefTypeName() != null) {
            srvRefTypeClass = Class.forName(serviceRef.getServiceRefTypeName(), false, classloader);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Loaded service-ref-type class from DD: " + srvRefTypeClass.getName());
            }
        }

        String serviceRefName = serviceRef.getName();
        String wsdlLocation = serviceRef.getWsdlFile();
        String lookupName = serviceRef.getLookupName();
        if (lookupName != null) {
            lookupName = lookupName.trim();
        }

        // Create a simulated WebServiceRef annotation object so we can construct the WebServiceRefInfo object with it.
        WebServiceRef wsrSimuAnnot = new WebServiceRefSimulator("", serviceRefName, srvRefTypeClass, srvInterfaceClass, wsdlLocation, lookupName);
        WebServiceRefInfo wsrInfo = new WebServiceRefInfo(wsrSimuAnnot);

        // If the 'lookup-name' attribute was specified, then skip all the rest of the metadata.
        if (wsrInfo.getLookupName() != null && !wsrInfo.getLookupName().isEmpty()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Service-ref's lookup name was specified... bypassing the rest of metadata processing...");
            }
            return wsrInfo;
        }

        // Set the WSDL service QName value.
        if (serviceRef.getServiceQname() != null) {
            QName serviceQName = new QName(serviceRef.getServiceQname().getNamespaceURI(), serviceRef.getServiceQname().getLocalPart());
            wsrInfo.setServiceQName(serviceQName);
        }

        // set the handler-chain information if appropriate
        //Only read the handlerChain configurations, which are used by JAX-WS runtime only
        if (!serviceRef.getHandlerChainList().isEmpty()) {
            wsrInfo.setHandlersFromXML(true);
            for (HandlerChain handerChain : serviceRef.getHandlerChainList()) {
                wsrInfo.addHandlerChain(HandlerChainInfoBuilder.buildHandlerChainInfoFromXML(handerChain));
            }
        }

        // Next, we'll visit our list of PortComponentRefs
        List<PortComponentRef> pcRefs = serviceRef.getPortComponentRefs();
        if (pcRefs != null && !pcRefs.isEmpty()) {

            for (PortComponentRef pcRef : pcRefs) {

                String seiName = pcRef.getServiceEndpointInterfaceName();

                // Make sure the PortComponentRef has an SEI classname as we
                // need to base our metadata on that key.
                if (StringUtils.isEmpty(seiName)) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "service-endpoint-interface is not configured in the current port-component-ref from the service-ref {0}", serviceRef.getName());
                    }
                    continue;
                }

                PortComponentRefInfo portComponentRefInfo = new PortComponentRefInfo(seiName.trim());
                wsrInfo.addPortComponentRefInfo(portComponentRefInfo);

                // set the portComponentLink
                if (!StringUtils.isEmpty(pcRef.getPortComponentLink())) {
                    portComponentRefInfo.setPortComponentLink(pcRef.getPortComponentLink());
                }

                // set Features
                // If any of the MTOM info is set, then add a WebServiceFeature for it.
                // Note: if either of the values is not set, we'll still retrieve the attribute
                // from the PortComponentRef, then the default value will be returned for us so we can use it.
                if (pcRef.isSetEnableMtom() || pcRef.isSetMtomThreshold()) {
                    MTOMFeatureInfo feature = new MTOMFeatureInfo();

                    //we should check if the <enable-mtom> or <motm-threshold> is set
                    //if yes, let them override the attributes of MTOMFeatureInfo
                    //if no, the MTOMFeatureInfo provide us the default values
                    if (pcRef.isSetEnableMtom())
                        feature.setEnabled(pcRef.isEnableMtom());

                    if (pcRef.isSetMtomThreshold())
                        feature.setThreshold(pcRef.getMtomThreshold());

                    portComponentRefInfo.addWebServiceFeatureInfo(feature);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Recording MTOMFeatureInfo for SEI " + seiName + ": " + feature.toString());
                    }
                }

                // If we have a "respect-binding" sub-element, then add a feature for it.
                if (pcRef.getRespectBinding() != null) {
                    RespectBindingFeatureInfo feature = new RespectBindingFeatureInfo();

                    //we should check if the <enabled> is set
                    //if yes, let them override the attributes of RespectBindingFeatureInfo
                    //if no, the RespectBindingFeatureInfo provide us the default values
                    if (pcRef.getRespectBinding().isSetEnabled())
                        feature.setEnabled(pcRef.getRespectBinding().isEnabled());

                    portComponentRefInfo.addWebServiceFeatureInfo(feature);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Recording RespectBindingFeatureInfo for SEI " + seiName + ": " + feature.toString());
                    }
                }

                // If we have an "addressing" sub-element, then add a feature for it.
                Addressing addr = pcRef.getAddressing();
                if (addr != null) {
                    AddressingFeatureInfo feature = new AddressingFeatureInfo();

                    //we should check if the <enabled> or <required> is set
                    //if yes, let them override the attributes of AddressingFeatureInfo
                    //if no, the AddressingFeatureInfo provide us the default values
                    if (addr.isSetEnabled())
                        feature.setEnabled(addr.isEnabled());

                    if (addr.isSetRequired())
                        feature.setRequired(addr.isRequired());

                    // If the response field was specified, then map the WCCM value to
                    // a "web service feature" value.
                    switch (addr.getAddressingResponsesTypeValue()) {
                        case Addressing.ADDRESSING_RESPONSES_ANONYMOUS:
                            feature.setResponses(AddressingFeature.Responses.ANONYMOUS);
                            break;
                        case Addressing.ADDRESSING_RESPONSES_NON_ANONYMOUS:
                            feature.setResponses(AddressingFeature.Responses.NON_ANONYMOUS);
                            break;
                        default:
                            feature.setResponses(AddressingFeature.Responses.ALL);
                            break;
                    }

                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Recording AddressingFeatureInfo for SEI " + seiName + ": " + feature.toString());
                    }
                    portComponentRefInfo.addWebServiceFeatureInfo(feature);
                }
            }

        }
        return wsrInfo;
    }

    @FFDCIgnore(ClassNotFoundException.class)
    public static void configureWebServiceRefPartialInfo(WebServiceRefInfo webServiceRefInfo, ClassLoader classLoader) {

        String serviceInterfaceClassName = webServiceRefInfo.getServiceInterfaceClassName();
        if (StringUtils.isEmpty(serviceInterfaceClassName) || serviceInterfaceClassName.equals(Service.class.getName())) {
            return;
        }

        Class<?> serviceInterfaceClass;
        try {
            if (classLoader == null) {
                classLoader = ClassLoader.getSystemClassLoader();
            }
            serviceInterfaceClass = classLoader.loadClass(webServiceRefInfo.getServiceInterfaceClassName());
            WebServiceRefPartialInfo partialInfo = buildPartialInfoFromWebServiceClient(serviceInterfaceClass);
            webServiceRefInfo.setPartialInfo(partialInfo);
        } catch (ClassNotFoundException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Unable to locate classinfo for class " + webServiceRefInfo.getServiceInterfaceClassName() + " for " + e.getMessage());
            }
        }

    }

    /**
     * This method will build a ServiceRefPartialInfo object from a class with an
     * 
     * @WebServiceClient annotation.
     */
    private static WebServiceRefPartialInfo buildPartialInfoFromWebServiceClient(Class<?> serviceInterfaceClass) {

        WebServiceClient webServiceClient = serviceInterfaceClass.getAnnotation(WebServiceClient.class);
        if (webServiceClient == null) {
            return null;
        }
        String className = serviceInterfaceClass.getName();
        String wsdlLocation = webServiceClient.wsdlLocation();
        QName serviceQName = null;
        String localPart = webServiceClient.name();
        if (localPart != null) {
            serviceQName = new QName(webServiceClient.targetNamespace(), localPart);
        }

        String handlerChainDeclaringClassName = null;
        javax.jws.HandlerChain handlerChainAnnotation = serviceInterfaceClass.getAnnotation(javax.jws.HandlerChain.class);
        if (handlerChainAnnotation != null)
            handlerChainDeclaringClassName = serviceInterfaceClass.getName();
        WebServiceRefPartialInfo partialInfo = new WebServiceRefPartialInfo(className, wsdlLocation, serviceQName, null, handlerChainDeclaringClassName, handlerChainAnnotation);
        return partialInfo;
    }
}
