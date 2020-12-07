/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jaxws.client.injection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;

import javax.annotation.Resource;
import javax.xml.ws.Service;
import javax.xml.ws.WebServiceRef;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.javaee.dd.common.InjectionTarget;
import com.ibm.ws.javaee.dd.common.wsclient.ServiceRef;
import com.ibm.ws.jaxws.metadata.EndpointInfo;
import com.ibm.ws.jaxws.metadata.JaxWsModuleInfo;
import com.ibm.ws.jaxws.metadata.JaxWsModuleMetaData;
import com.ibm.ws.jaxws.metadata.PortComponentRefInfo;
import com.ibm.ws.jaxws.metadata.WebServiceRefInfo;
import com.ibm.ws.jaxws.metadata.builder.WebServiceRefInfoBuilder;
import com.ibm.ws.jaxws.support.JaxWsMetaDataManager;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.NonPersistentCache;
import com.ibm.wsspi.injectionengine.ComponentNameSpaceConfiguration;
import com.ibm.wsspi.injectionengine.InjectionBinding;
import com.ibm.wsspi.injectionengine.InjectionException;

/**
 * This class will be responsible for building InjectionBinding objects for web service references that were found in
 * XML. It will build objects for JAX-WS service references.
 * 
 */
public class WebServiceRefBindingBuilder {

    private static final TraceComponent tc = Tr.register(WebServiceRefBindingBuilder.class);

    /**
     * This class will build a list of InjectionBinding objects based on JAX-WS service-ref elements that were found in a client deployment descriptor.
     */
    static List<InjectionBinding<WebServiceRef>> buildJaxWsWebServiceRefBindings(List<ServiceRef> jaxwsServiceRefs, ComponentNameSpaceConfiguration cnsConfig) throws InjectionException {

        if (jaxwsServiceRefs.size() == 0) {
            return Collections.emptyList();
        }

        ClassLoader classLoader = cnsConfig.getClassLoader();

        List<InjectionBinding<WebServiceRef>> bindingList = new ArrayList<InjectionBinding<WebServiceRef>>(jaxwsServiceRefs.size());

        try {
            // For each service ref, we will build a binding and a WebServiceRefInfo object to hold the service-ref's metadata.
            for (ServiceRef serviceRef : jaxwsServiceRefs) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Building WebServiceRefInfo for service-ref: " + serviceRef.getName());
                }

                WebServiceRefInfo wsrInfo = WebServiceRefInfoBuilder.buildWebServiceRefInfo(serviceRef, classLoader);
                wsrInfo.setClientMetaData(JaxWsMetaDataManager.getJaxWsClientMetaData(cnsConfig.getModuleMetaData()));

                Class<?> srvInterfaceClass = wsrInfo.getAnnotationValue().value();
                Class<?> srvRefTypeClass = wsrInfo.getAnnotationValue().type();

                // Use portComponentLink to find the wsdl location if not specified in <wsdl-file> explicitly
                if (wsrInfo.getWsdlLocation() == null || wsrInfo.getWsdlLocation().isEmpty()) {

                    String pcLinkValue = null;
                    String srvRefTypeClassName = wsrInfo.getServiceRefTypeClassName();

                    // loop through until we find a port-component-ref whose
                    // service-endpoint-interface value matches the service-ref-type in the service-ref
                    for (Entry<String, PortComponentRefInfo> entry : wsrInfo.getPortComponentRefInfoMap().entrySet()) {
                        String seiName = entry.getKey();
                        PortComponentRefInfo pcRefInfo = entry.getValue();

                        if (!seiName.equals(srvRefTypeClassName)) {
                            continue;
                        }

                        pcLinkValue = pcRefInfo.getPortComponentLink();
                        break;
                    }

                    // if we found a non-null port-component-link value, we need to look for the wsdl location 
                    // from the port component of the same module
                    if (pcLinkValue != null) {
                        // the service metadata has processed before the client metadata processing
                        JaxWsModuleMetaData jaxwsModuleMetaData = JaxWsMetaDataManager.getJaxWsModuleMetaData(cnsConfig.getModuleMetaData());
                        Container moduleContainer = jaxwsModuleMetaData.getModuleContainer();

                        NonPersistentCache overlayCache = moduleContainer.adapt(NonPersistentCache.class);
                        JaxWsModuleInfo jaxWsModuleInfo = (JaxWsModuleInfo) overlayCache.getFromCache(JaxWsModuleInfo.class);

                        if (jaxWsModuleInfo != null) {
                            for (EndpointInfo endpointInfo : jaxWsModuleInfo.getEndpointInfos()) {
                                if (pcLinkValue.equals(endpointInfo.getPortComponentName())) {
                                    String wsdlLocation = endpointInfo.getWsdlLocation();
                                    if (wsdlLocation != null && !wsdlLocation.isEmpty()) {
                                        wsrInfo.setWsdlLocation(wsdlLocation);
                                    }
                                    break;
                                }
                            }
                        }
                    }
                }

                // Create the injection binding for this service ref.
                WebServiceRefBinding binding = new WebServiceRefBinding(wsrInfo, cnsConfig);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Created WebServiceRefBinding: " + binding.toString());
                }

                // Add injection targets which defined in injection-targets element
                // Note that injection targets will be empty for java:global/java:app/java:module.
                List<InjectionTarget> injectionTargets = serviceRef.getInjectionTargets();
                if (injectionTargets != null && !injectionTargets.isEmpty()) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Adding injection targets specified in service-ref element");
                    }
                    // get the injection type from service-interface and service-ref-type element
                    Class<?> injectionType = srvInterfaceClass;
                    if (srvRefTypeClass != null && !srvRefTypeClass.getName().equals(Object.class.getName())) {
                        injectionType = srvRefTypeClass;
                    }

                    for (InjectionTarget target : injectionTargets) {
                        if (target.getInjectionTargetClassName() != null && target.getInjectionTargetName() != null) {
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                Tr.debug(tc, "Adding injection target from XML, type= " + injectionType.getName()
                                             + ", target class= " + target.getInjectionTargetClassName()
                                             + ", target member= " + target.getInjectionTargetName());
                            }
                            binding.addInjectionTarget(injectionType, target.getInjectionTargetName(), target.getInjectionTargetClassName());
                        }
                    }
                }

                // Add the new binding to the list to be returned to the caller.
                bindingList.add(binding);
            }
        } catch (InjectionException e) {
            throw e;
        } catch (Throwable t) {
            throw new InjectionException(t);
        }

        return bindingList;
    }

    /**
     * This method will be used to create an instance of a WebServiceRefBinding object
     * that holds metadata obtained from an @Resource annotation. The @Resource annotation
     * in this case would have been indicating a JAX-WS service type injection.
     */
    static InjectionBinding<WebServiceRef> createWebServiceRefBindingFromResource(Resource resource, ComponentNameSpaceConfiguration cnsConfig, Class<?> serviceClass,
                                                                                  String jndiName) throws InjectionException {
        InjectionBinding<WebServiceRef> binding = null;

        WebServiceRef wsRef = createWebServiceRefFromResource(resource, serviceClass, jndiName);
        WebServiceRefInfo wsrInfo = WebServiceRefInfoBuilder.buildWebServiceRefInfo(wsRef, cnsConfig.getClassLoader());
        wsrInfo.setClientMetaData(JaxWsMetaDataManager.getJaxWsClientMetaData(cnsConfig.getModuleMetaData()));
        wsrInfo.setServiceInterfaceClassName(serviceClass.getName());
        binding = new WebServiceRefBinding(wsRef, cnsConfig);

        // register the metadata, and set a flag on the binding instance that let's us
        // know this binding represents metadata from an @Resource annotation
        ((WebServiceRefBinding) binding).setWebServiceRefInfo(wsrInfo);
        ((WebServiceRefBinding) binding).setResourceType(true);

        return binding;
    }

    /**
     * This creates an @WebServiceRef instance based on data from an @Resource annotation.
     * The type class and JNDI name are separately passed in because the @Resource annotation
     * may have the default values for these attributes.
     */
    static WebServiceRef createWebServiceRefFromResource(Resource resource, Class<?> typeClass, String jndiName) throws InjectionException {
        // notice we send in 'Service.class' for the 'value' attribute, this is
        // because only service type injections are possible with the @Resource
        // annotation, so we'll use the default value for the 'value' attribute.
        return new WebServiceRefSimulator(resource.mappedName(), jndiName, typeClass, Service.class, null, "");
    }
}
