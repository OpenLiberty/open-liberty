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

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.List;

import javax.annotation.Resource;
import javax.naming.Reference;
import javax.xml.namespace.QName;
import javax.xml.ws.RespectBinding;
import javax.xml.ws.Service;
import javax.xml.ws.WebServiceRef;
import javax.xml.ws.WebServiceRefs;
import javax.xml.ws.soap.Addressing;
import javax.xml.ws.soap.MTOM;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.javaee.dd.common.wsclient.ServiceRef;
import com.ibm.ws.javaee.ddmodel.wsbnd.Port;
import com.ibm.ws.javaee.ddmodel.wsbnd.WebservicesBnd;
import com.ibm.ws.jaxws.metadata.AddressingFeatureInfo;
import com.ibm.ws.jaxws.metadata.JaxWsClientMetaData;
import com.ibm.ws.jaxws.metadata.MTOMFeatureInfo;
import com.ibm.ws.jaxws.metadata.PortComponentRefInfo;
import com.ibm.ws.jaxws.metadata.RespectBindingFeatureInfo;
import com.ibm.ws.jaxws.metadata.WebServiceRefInfo;
import com.ibm.ws.jaxws.metadata.builder.WebServiceRefInfoBuilder;
import com.ibm.ws.jaxws.support.JaxWsMetaDataManager;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.injectionengine.InjectionBinding;
import com.ibm.wsspi.injectionengine.InjectionException;
import com.ibm.wsspi.injectionengine.InjectionProcessor;
import com.ibm.wsspi.injectionengine.OverrideInjectionProcessor;
import com.ibm.wsspi.injectionengine.factory.IndirectJndiLookupReferenceFactory;

/**
 * This class will be the InjectionProcessor implementation responsible for handling @WebServiceRef and @WebServiceRefs annotations.
 * It will also be responsible for handling an equivalent <service-ref> in the XML deployment descriptor.
 * 
 * The class will be responsible for creating InjectionBinding objects that will later be bound into the JNDI namespace and used to achieve appropriate resource injection.
 * 
 * Note this class also registers itself as an override processor for the @Resource annotation. It does this so it can support web service reference injections
 * being indicated by the @Resource annotation.
 * 
 */
public class WebServiceRefProcessor extends InjectionProcessor<WebServiceRef, WebServiceRefs> implements OverrideInjectionProcessor<WebServiceRef, Resource> {

    private static final TraceComponent tc = Tr.register(WebServiceRefProcessor.class);

    public WebServiceRefProcessor() {
        super(WebServiceRef.class, WebServiceRefs.class);
    }

    @Override
    public String getJndiName(WebServiceRef webServiceRef) {
        return webServiceRef.name();
    }

    @Override
    public WebServiceRef[] getAnnotations(WebServiceRefs webServiceRefs) {
        return webServiceRefs.value();
    }

    /**
     * This method will process any service-ref elements in the client's deployment descriptor.
     */
    @Override
    public void processXML() throws InjectionException {

        @SuppressWarnings("unchecked")
        List<ServiceRef> serviceRefs = (List<ServiceRef>) ivNameSpaceConfig.getWebServiceRefs();

        // no need to do any work if there are no service refs in the XML
        if (serviceRefs == null || serviceRefs.isEmpty()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "No service-refs in XML for module: " + ivNameSpaceConfig.getModuleName());
            }
            return;
        }

        ClassLoader moduleClassLoader = ivNameSpaceConfig.getClassLoader();

        if (moduleClassLoader == null) {
            throw new InjectionException("Internal Error: The classloader of module " + ivNameSpaceConfig.getModuleName() + " is null.");
        }

        // get all JAX-WS service refs from deployment descriptor
        List<ServiceRef> jaxwsServiceRefs = InjectionHelper.normalizeJaxWsServiceRefs(serviceRefs, moduleClassLoader);

        if (jaxwsServiceRefs.isEmpty()) {
            return;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Found JAX-WS service refs in XML for module: " + ivNameSpaceConfig.getModuleName());
        }

        // build up the metadata and create WebServiceRefBinding instances that will be used by the injection engine, 
        // then we will be saving off this metadata in the module or component metadata slot for later use by our ServiceRefObjectFactory
        List<InjectionBinding<WebServiceRef>> bindingList = WebServiceRefBindingBuilder.buildJaxWsWebServiceRefBindings(jaxwsServiceRefs, ivNameSpaceConfig);

        // now add all the bindings that were created
        if (bindingList != null && !bindingList.isEmpty()) {
            for (InjectionBinding<WebServiceRef> binding : bindingList) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Adding binding for JAX-WS service-ref: " + binding.getJndiName());
                }
                addInjectionBinding(binding);
            }
        }

    }

    /**
     * This method will create the injection binding from WebServiceRef annotation, if there is no DD so that processXML did not execute and create the injection binding.
     */
    @Override
    public InjectionBinding<WebServiceRef> createInjectionBinding(WebServiceRef webServiceRef, Class<?> instanceClass, Member member, String jndiName) throws InjectionException {
        InjectionBinding<WebServiceRef> binding;

        WebServiceRefInfo wsrInfo = WebServiceRefInfoBuilder.buildWebServiceRefInfo(webServiceRef, instanceClass.getClassLoader());
        wsrInfo.setClientMetaData(JaxWsMetaDataManager.getJaxWsClientMetaData(ivNameSpaceConfig.getModuleMetaData()));
        if (member == null) { //Annotated on class
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Creating new injection binding for @WebServiceRef annotation found on the " + instanceClass.getName() + " class.");
            }

            this.validateAndSetClassLevelWebServiceRef(webServiceRef, instanceClass, wsrInfo);

            // Now add the @HandlerChain instance
            javax.jws.HandlerChain hcAnnot = instanceClass.getAnnotation(javax.jws.HandlerChain.class);
            if (hcAnnot != null) {
                wsrInfo.setHandlerChainAnnotation(hcAnnot);
                wsrInfo.setHandlerChainDeclaringClassName(instanceClass.getName());
            }

            binding = new WebServiceRefBinding(wsrInfo, ivNameSpaceConfig);

        } else if (member instanceof Method) { //Annotated on method
            Method method = (Method) member;
            wsrInfo.setJndiName(jndiName);

            // Send the wsrInfo to this call so that both the 'type' and 'value' properties can be set correctly
            this.validateAndSetMemberLevelWebServiceRef(webServiceRef, method, wsrInfo);

            // Now add the @HandlerChain instance
            javax.jws.HandlerChain hcAnnot = method.getAnnotation(javax.jws.HandlerChain.class);
            if (hcAnnot != null) {
                wsrInfo.setHandlerChainAnnotation(hcAnnot);
                wsrInfo.setHandlerChainDeclaringClassName(instanceClass.getName());
            }

            //MTOM, RespectBinding, Addressing  
            //Merge any features that might be set on the field (@MTOM, @RespectBinding, @Addressing)
            //Note that we only support features for port-component-ref type injections.
            //For a port-component-ref type injection, the "Service SEI class" will be set on
            //'wsrMetadata'.
            if (wsrInfo.getServiceRefTypeClassName() != null) {
                handleMTOM(webServiceRef, wsrInfo, method.getAnnotation(MTOM.class));
                handleRespectBinding(webServiceRef, wsrInfo, method.getAnnotation(RespectBinding.class));
                handleAddressing(webServiceRef, wsrInfo, method.getAnnotation(Addressing.class));
            }

            // Create binding for @WebServiceRef annotation
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Creating new injection binding for the @WebServiceRef annotation on the " + method.getName() + " method in the "
                             + method.getDeclaringClass().getName() + " class.");
            }
            binding = new WebServiceRefBinding(wsrInfo, ivNameSpaceConfig);
            addInjectionBinding(binding);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Successfully created an injection binding for the @WebServiceRef annotation on the " + method.getName() + " method in the "
                             + method.getDeclaringClass().getName() + " class.");
            }

        } else { //Annotated on field
            Field field = (Field) member;
            wsrInfo.setJndiName(jndiName);

            // Send the wsrInfo to this call so that both the 'type' and 'value' properties can be set correctly
            this.validateAndSetMemberLevelWebServiceRef(webServiceRef, field, wsrInfo);

            // Now add the @HandlerChain instance
            javax.jws.HandlerChain hcAnnot = field.getAnnotation(javax.jws.HandlerChain.class);
            if (hcAnnot != null) {
                wsrInfo.setHandlerChainAnnotation(hcAnnot);
                wsrInfo.setHandlerChainDeclaringClassName(instanceClass.getName());
            }

            //MTOM, RespectBinding, Addressing  
            // Merge any features that might be set on the field (@MTOM, @RespectBinding, @Addressing)
            // Note that we only support features for port-component-ref type injections.
            // For a port-component-ref type injection, the "Service SEI class" will be set on
            // 'wsrMetadata'.
            if (wsrInfo.getServiceRefTypeClassName() != null) {
                handleMTOM(webServiceRef, wsrInfo, field.getAnnotation(MTOM.class));
                handleRespectBinding(webServiceRef, wsrInfo, field.getAnnotation(RespectBinding.class));
                handleAddressing(webServiceRef, wsrInfo, field.getAnnotation(Addressing.class));
            }

            // Create binding for @WebServiceRef annotation
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Creating new injection binding for the @WebServiceRef annotation on the " + field.getName() + " field in the "
                             + field.getDeclaringClass().getName() + " class.");
            }
            binding = new WebServiceRefBinding(wsrInfo, ivNameSpaceConfig);
            addInjectionBinding(binding);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Successfully created an injection binding for the @WebServiceRef annotation on the " + field.getName() + " field in the "
                             + field.getDeclaringClass().getName() + " class. HashCode=" + Integer.toHexString(System.identityHashCode(this)));
            }
        }

        return binding;
    }

    /**
     * This method will resolve the InjectionBinding it is given. This involves storing the correct information within
     * the binding instance so that later on the injection can occur. It also enables JNDI lookups to occur on the
     * resource that is indicated in the binding.
     */
    @Override
    public void resolve(InjectionBinding<WebServiceRef> binding) throws InjectionException {

        // This was a JAX-WS service reference, we need to do some setup for
        // our object factory, and we also need to make sure we store the
        // metadata away in the appropriate location.

        WebServiceRefInfo wsrInfo = ((WebServiceRefBinding) binding).getWebServiceRefInfo();

        Reference ref = null;

        // If the "lookup-name" attribute was specified for this service-ref, then we'll bind into the namespace
        // an instance of the IndirectJndiLookup reference that will be resolved by the IndirectJndiLookupObjectFactory.
        // If a JNDI lookup is performed on this "service-ref", then the IndirectJndiLookupObjectFactory will simply
        // turn around and handle the extra level of indirection by looking up the referenced service-ref, which
        // will in turn cause our ServiceRefObjectFactory to be invoked.
        if (wsrInfo.getLookupName() != null && !wsrInfo.getLookupName().isEmpty()) {

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Build IndirectJndiLookup(" + wsrInfo.getLookupName() + ") for service-ref '" + wsrInfo.getJndiName());
            }

            IndirectJndiLookupReferenceFactory factory = ivNameSpaceConfig.getIndirectJndiLookupReferenceFactory();
            ref = factory.createIndirectJndiLookup(binding.getJndiName(), wsrInfo.getLookupName(), Object.class.getName());

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Obtained Reference from IndirectJndiLookup object: " + ref.toString());
            }

        } else { // Otherwise, we'll build a reference that will be resolved by our own object factory.

            ref = new Reference(WebServiceRefProcessor.class.getName(), ServiceRefObjectFactory.class.getName(), null);

            // Add our serializable service-ref metadata to the Reference.
            WebServiceRefInfoRefAddr wsrInfoRefAddr = new WebServiceRefInfoRefAddr(wsrInfo);
            ref.add(wsrInfoRefAddr);

            // If our classloader is not null, then that means we're being called under the context of an application module.
            // In this case, we want to store away the module-specific metadata in the module's metadata slot.

            // This has been moved to LibertyProviderImpl since in that time slot, clientMetadata will always be available
            // If CDI is enabled, the injections happens before JaxwsModuleMetaDataLister.metaDataCreated()
            /*
             * if (ivNameSpaceConfig.getClassLoader() != null) {
             * // get the client metadata. in client side, here should be the first time get the client metadata, so will create one.
             * JaxWsClientMetaData clientMetaData = JaxWsMetaDataManager.getJaxWsClientMetaData(ivNameSpaceConfig.getModuleMetaData());
             * 
             * // parsing and merge the client configuration from the ibm-ws-bnd.xml
             * if (clientMetaData != null)
             * {
             * mergeWebServicesBndInfo(wsrInfo, clientMetaData);
             * }
             * 
             * }
             */
            J2EEName j2eeName = ivNameSpaceConfig.getJ2EEName();
            String componenetName = (null != j2eeName) ? j2eeName.getComponent() : null;
            wsrInfo.setComponenetName(componenetName);

        }

        WebServiceRefInfoBuilder.configureWebServiceRefPartialInfo(wsrInfo, ivNameSpaceConfig.getClassLoader());
        binding.setObjects(null, ref);

    }

    /**
     * merge the configurations from the ibm-ws-bnd.xml
     * 
     * @param wsrInfo
     */
    private void mergeWebServicesBndInfo(WebServiceRefInfo wsrInfo, JaxWsClientMetaData jaxwsClientMetaData) {

        WebservicesBnd webServicesBnd = null;
        try {
            webServicesBnd = jaxwsClientMetaData.getModuleMetaData().getModuleContainer().adapt(WebservicesBnd.class);
        } catch (UnableToAdaptException e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Can not get the custom binding file due to {0}", e);
            }
            return;
        }

        if (webServicesBnd != null) {
            J2EEName j2eeName = ivNameSpaceConfig.getJ2EEName();
            String componenetName = (null != j2eeName) ? j2eeName.getComponent() : null;
            com.ibm.ws.javaee.ddmodel.wsbnd.ServiceRef serviceRef = webServicesBnd.getServiceRef(wsrInfo.getJndiName(), componenetName);

            if (serviceRef != null) {
                List<Port> portList = serviceRef.getPorts();
                //store the port list in the map in WebServiceRefInfo object.
                if (portList != null && portList.size() > 0) {
                    for (Port port : portList) {
                        QName portQName = port.getPortQName();
                        PortComponentRefInfo portInfo = new PortComponentRefInfo(portQName);
                        portInfo.setAddress(port.getAddress());

                        portInfo.setUserName(port.getUserName());
                        portInfo.setPassword(port.getPassword());
                        portInfo.setSSLRef(port.getSSLRef());
                        portInfo.setKeyAlias(port.getKeyAlias());

                        //store the binding properties in the PortComponentRefInfo object.
                        portInfo.setProperties(port.getProperties());
                        wsrInfo.addPortComponentRefInfo(portInfo);
                    }
                }
                wsrInfo.setDefaultPortAddress(serviceRef.getPortAddress());

                //store the binding properties in the WebServiceRefInfo object.
                wsrInfo.setProperties(serviceRef.getProperties());

                String wsdlOverride = serviceRef.getWsdlLocation();
                if (wsdlOverride != null && !wsdlOverride.isEmpty()) {
                    wsrInfo.setWsdlLocation(wsdlOverride);
                }
            }
        }
    }

    @Override
    protected void validateMissingJndiName(Class<?> instanceClass, WebServiceRef webServiceRef) throws InjectionException {
        Tr.error(tc, "error.service.ref.class.level.annotation.name.or.type.absent",
                 instanceClass.getName());
        throw new InjectionException(Tr.formatMessage(tc, "error.service.ref.class.level.annotation.name.or.type.absent",
                                                      instanceClass.getName()));
    }

    static void handleMTOM(WebServiceRef webServiceRef, WebServiceRefInfo wsrInfo, MTOM mtomAnnotation) {
        if (mtomAnnotation == null) {
            return;
        }
        if (wsrInfo.isWebServiceFeaturePresent(wsrInfo.getServiceRefTypeClassName(), MTOMFeatureInfo.class)) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "MTOM feature already present on WSR metadata, ignoring @MTOM annotation");
            }
        } else {
            MTOMFeatureInfo feature = new MTOMFeatureInfo(mtomAnnotation);
            wsrInfo.addWebServiceFeatureInfo(wsrInfo.getServiceRefTypeClassName(), feature);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Recording MTOMFeatureInfo for SEI " + wsrInfo.getServiceRefTypeClassName() + ": " + feature.toString());
            }
        }
    }

    static void handleRespectBinding(WebServiceRef webServiceRef, WebServiceRefInfo wsrInfo, RespectBinding rbAnnotation) {
        if (rbAnnotation == null) {
            return;
        }
        if (wsrInfo.isWebServiceFeaturePresent(wsrInfo.getServiceRefTypeClassName(), RespectBindingFeatureInfo.class)) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "RespectBinding feature already present on WSR metadata, ignoring @RespectBinding annotation");
            }
        } else {
            RespectBindingFeatureInfo feature = new RespectBindingFeatureInfo(rbAnnotation);
            wsrInfo.addWebServiceFeatureInfo(wsrInfo.getServiceRefTypeClassName(), feature);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Recording RespectBindingFeatureInfo for SEI " + wsrInfo.getServiceRefTypeClassName() + ": " + feature.toString());
            }
        }
    }

    static void handleAddressing(WebServiceRef webServiceRef, WebServiceRefInfo wsrInfo, Addressing addrAnnotation) {
        if (addrAnnotation == null) {
            return;
        }
        if (wsrInfo.isWebServiceFeaturePresent(wsrInfo.getServiceRefTypeClassName(), AddressingFeatureInfo.class)) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Addressing feature already present on WSR metadata, ignoring @Addressing annotation");
            }
        } else {
            AddressingFeatureInfo feature = new AddressingFeatureInfo(addrAnnotation);
            wsrInfo.addWebServiceFeatureInfo(wsrInfo.getServiceRefTypeClassName(), feature);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Recording AddressingFeatureInfo for SEI " + wsrInfo.getServiceRefTypeClassName() + ": " + feature.toString());
            }
        }
    }

    /**
     * This method will be responsible for validating all of the rules that govern injection through class level WebServiceRef annotation.
     * It will also be responsible for setting the appropriate values on the WebServiceRefInfo instance.
     */
    private void validateAndSetClassLevelWebServiceRef(WebServiceRef webServiceRef, Class<?> instanceClass, WebServiceRefInfo wsrInfo) throws InjectionException {
        // Save off these annotation attributes for use later.
        Class<?> typeClass = webServiceRef.type();
        Class<?> valueClass = webServiceRef.value();

        // If the @WebServiceRef's lookup is specified, then no other attributes should be present. Only name can be specified with lookup.
        if (webServiceRef.lookup() != null && !webServiceRef.lookup().isEmpty()) {
            if (!typeClass.getName().equals(Object.class.getName())
                || !valueClass.getName().equals(Service.class.getName())
                || !webServiceRef.wsdlLocation().isEmpty()
                || !webServiceRef.mappedName().isEmpty()) {

                Tr.error(tc, "error.service.ref.annotation.lookup.redundant.attributes");
                throw new InjectionException(Tr.formatMessage(tc, "error.service.ref.annotation.lookup.redundant.attributes"));
            }

        } else { // Otherwise, no 'lookup' attribute is present, then do our normal validations.

            if (webServiceRef.name().isEmpty()) {
                //For class level WebServiceRef, the 'name' and 'type' must be specified.
                Tr.error(tc, "error.service.ref.class.level.annotation.name.or.type.absent",
                         instanceClass.getName());
                throw new InjectionException(Tr.formatMessage(tc, "error.service.ref.class.level.annotation.name.or.type.absent",
                                                              instanceClass.getName()));

            }

            if (typeClass.getName().equals(Object.class.getName())) { // type is not specified, so try service type injection
                // in this case the 'value' attribute MUST specify a subclass of javax.xml.ws.Service
                if (!Service.class.isAssignableFrom(valueClass) || valueClass.getName().equals(Service.class.getName())) {
                    Tr.error(tc, "error.service.ref.class.level.annotation.name.or.type.absent",
                             instanceClass.getName());
                    throw new InjectionException(Tr.formatMessage(tc, "error.service.ref.class.level.annotation.name.or.type.absent",
                                                                  instanceClass.getName()));
                }

                wsrInfo.setServiceInterfaceClassName(valueClass.getName());
                wsrInfo.setServiceRefTypeClassName(valueClass.getName());

            } else if (Service.class.isAssignableFrom(typeClass)) { // service type injection case
                if (typeClass.getName().equals(Service.class.getName())) { // typeClass is javax.xml.ws.Service
                    // in this case the 'value' attribute MUST specify a subclass of javax.xml.ws.Service
                    if (!Service.class.isAssignableFrom(valueClass) || valueClass.getName().equals(Service.class.getName())) {
                        Tr.error(tc, "error.service.ref.class.level.annotation.wrong.value",
                                 instanceClass.getName(), valueClass.getName());
                        throw new InjectionException(Tr.formatMessage(tc, "error.service.ref.class.level.annotation.wrong.value",
                                                                      instanceClass.getName(), valueClass.getName()));
                    }

                    wsrInfo.setServiceInterfaceClassName(valueClass.getName());
                    wsrInfo.setServiceRefTypeClassName(valueClass.getName());

                } else { //typeClass is a subclass of javax.xml.ws.Service
                    // in this case the 'value' attribute MUST be the same as 'type' or use default javax.xml.ws.Service
                    if (!valueClass.getName().equals(typeClass.getName()) && !valueClass.getName().equals(Service.class.getName())) {
                        Tr.error(tc, "error.service.ref.class.level.annotation.value.and.type.not.same",
                                 instanceClass.getName());
                        throw new InjectionException(Tr.formatMessage(tc, "error.service.ref.class.level.annotation.value.and.type.not.same",
                                                                      instanceClass.getName()));
                    }

                    wsrInfo.setServiceInterfaceClassName(typeClass.getName());
                    wsrInfo.setServiceRefTypeClassName(typeClass.getName());
                }

            } else { // port type injection case
                // in this case the 'value' attribute MUST specify a subclass of javax.xml.ws.Service
                if (!Service.class.isAssignableFrom(valueClass) || valueClass.getName().equals(Service.class.getName())) {
                    Tr.error(tc, "error.service.ref.class.level.annotation.wrong.value",
                             instanceClass.getName(), valueClass.getName());
                    throw new InjectionException(Tr.formatMessage(tc, "error.service.ref.class.level.annotation.wrong.value",
                                                                  instanceClass.getName(), valueClass.getName()));
                }

                wsrInfo.setServiceInterfaceClassName(valueClass.getName());
                wsrInfo.setServiceRefTypeClassName(typeClass.getName());
            }
        }
    }

    /**
     * This method will be responsible for validating all of the rules that govern injection through member level WebServiceRef annotation.
     * It will also be responsible for setting the appropriate values on the WebServiceRefInfo instance.
     */
    private void validateAndSetMemberLevelWebServiceRef(WebServiceRef webServiceRef, Member member, WebServiceRefInfo wsrInfo) throws InjectionException {

        // Save off these annotation attributes for use later.
        Class<?> typeClass = webServiceRef.type();
        Class<?> valueClass = webServiceRef.value();

        // If the @WebServiceRef's lookup is specified, then no other attributes should be present. Only name can be specified with lookup.
        if (webServiceRef.lookup() != null && !webServiceRef.lookup().isEmpty()) {
            if (!typeClass.getName().equals(Object.class.getName())
                || !valueClass.getName().equals(Service.class.getName())
                || !webServiceRef.wsdlLocation().isEmpty()
                || !webServiceRef.mappedName().isEmpty()) {
                Tr.error(tc, "error.service.ref.annotation.lookup.redundant.attributes");
                throw new InjectionException(Tr.formatMessage(tc, "error.service.ref.annotation.lookup.redundant.attributes"));
            }

        } else { // Otherwise, no 'lookup' attribute is present, then do our normal validations.

            // get the effective type from the member type and WebServiceRef.type
            Class<?> memberType = InjectionHelper.getTypeFromMember(member);
            Class<?> effectiveType;
            if (memberType.getName().equals(Object.class.getName())) { //memberType is Object.class
                effectiveType = typeClass;
                // effectiveType could be Object here
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() && typeClass.getName().equals(Object.class.getName())) {
                    Tr.debug(tc, "The member type and the @WebServiceRef.type on the " + member.getName() + " in " + member.getDeclaringClass().getName()
                                 + "are all Object.class, so can not infer one. Will try service type injection if the @WebServiceRef.value is a subclass of Service.class.");
                }

            } else { //memberType is not Object.class
                //we prefer subclass
                if (memberType.isAssignableFrom(typeClass)) {
                    effectiveType = typeClass;
                } else if (typeClass.isAssignableFrom(memberType)) {
                    effectiveType = memberType;
                } else {
                    Tr.error(tc, "error.service.ref.member.level.annotation.type.not.compatible",
                             member.getName(), member.getDeclaringClass().getName(), typeClass.getName(), memberType.getName());
                    throw new InjectionException(Tr.formatMessage(tc, "error.service.ref.member.level.annotation.type.not.compatible",
                                                                  member.getName(), member.getDeclaringClass().getName(), typeClass.getName(), memberType.getName()));
                }
            }

            // validate and set value and type in WebServiceRefInfo
            if (effectiveType.getName().equals(Object.class.getName())) { // effectiveType can not be determined above, so try service type injection
                // in this case the 'value' attribute MUST specify a subclass of javax.xml.ws.Service
                if (!Service.class.isAssignableFrom(valueClass) || valueClass.getName().equals(Service.class.getName())) {
                    Tr.error(tc, "error.service.ref.member.level.annotation.type.not.inferred",
                             member.getName(), member.getDeclaringClass().getName());
                    throw new InjectionException(Tr.formatMessage(tc, "error.service.ref.member.level.annotation.type.not.inferred",
                                                                  member.getName(), member.getDeclaringClass().getName()));
                }

                wsrInfo.setServiceRefTypeClassName(valueClass.getName());
                wsrInfo.setServiceInterfaceClassName(valueClass.getName());

            } else if (Service.class.isAssignableFrom(effectiveType)) { // service type injection case
                if (effectiveType.getName().equals(Service.class.getName())) { // effectiveType is javax.xml.ws.Service
                    // in this case the 'value' attribute MUST specify a subclass of javax.xml.ws.Service
                    if (!Service.class.isAssignableFrom(valueClass) || valueClass.getName().equals(Service.class.getName())) {
                        Tr.error(tc, "error.service.ref.member.level.annotation.wrong.value",
                                 member.getName(), member.getDeclaringClass(), valueClass.getName());
                        throw new InjectionException(Tr.formatMessage(tc, "error.service.ref.member.level.annotation.wrong.value",
                                                                      member.getName(), member.getDeclaringClass(), valueClass.getName()));
                    }
                    wsrInfo.setServiceInterfaceClassName(valueClass.getName());
                    wsrInfo.setServiceRefTypeClassName(valueClass.getName());

                } else { //effectiveType is a subclass of javax.xml.ws.Service
                    // in this case the 'value' attribute MUST be the same as 'type' or use default javax.xml.ws.Service
                    if (!valueClass.getName().equals(effectiveType.getName()) && !valueClass.getName().equals(Service.class.getName())) {
                        Tr.error(tc, "error.service.ref.member.level.annotation.value.and.type.not.same",
                                 member.getName(), member.getDeclaringClass().getName());
                        throw new InjectionException(Tr.formatMessage(tc, "error.service.ref.member.level.annotation.value.and.type.not.same",
                                                                      member.getName(), member.getDeclaringClass().getName()));
                    }

                    wsrInfo.setServiceInterfaceClassName(effectiveType.getName());
                    wsrInfo.setServiceRefTypeClassName(effectiveType.getName());
                }

            } else { // port type injection case
                // in this case the 'value' attribute MUST specify a subclass of javax.xml.ws.Service
                if (!Service.class.isAssignableFrom(valueClass) || valueClass.getName().equals(Service.class.getName())) {
                    Tr.error(tc, "error.service.ref.member.level.annotation.wrong.value",
                             member.getName(), member.getDeclaringClass(), valueClass.getName());
                    throw new InjectionException(Tr.formatMessage(tc, "error.service.ref.member.level.annotation.wrong.value",
                                                                  member.getName(), member.getDeclaringClass(), valueClass.getName()));
                }

                wsrInfo.setServiceRefTypeClassName(effectiveType.getName());
                wsrInfo.setServiceInterfaceClassName(valueClass.getName());
            }

        }

    }

    /**
     * This method will allow us to create an InjectionBinding instance for @Resource annotations found on a method or field
     * that are being used to indicate web service reference types.
     */
    @Override
    public InjectionBinding<WebServiceRef> createOverrideInjectionBinding(Class<?> instanceClass, Member member,
                                                                          Resource resource, String jndiName) throws InjectionException {

        Class<?> typeClass = resource.type();

        if (member != null) {
            Class<?> inferredType = InjectionHelper.getTypeFromMember(member);
            if (typeClass.getName().equals(Object.class.getName())) { //default
                typeClass = inferredType;
            } else {
                if (!inferredType.isAssignableFrom(typeClass)) {
                    Tr.error(tc, "error.service.ref.member.level.annotation.type.not.compatible",
                             member.getName(), member.getDeclaringClass().getName(), typeClass.getName(), inferredType.getName());
                    throw new InjectionException(Tr.formatMessage(tc, "error.service.ref.member.level.annotation.type.not.compatible",
                                                                  member.getName(), member.getDeclaringClass().getName(), typeClass.getName(), inferredType.getName()));
                }
            }
        }

        InjectionBinding<WebServiceRef> binding;

        //only service type injections are possible with the @Resource annotation
        if (Service.class.isAssignableFrom(typeClass)) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "The @Resource annotation found has type=" + typeClass.getName() + " and is refers to a JAX-WS service reference");
            }
            binding = WebServiceRefBindingBuilder.createWebServiceRefBindingFromResource(resource, ivNameSpaceConfig, typeClass, jndiName);
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "The @Resource annotation found did not refer to a web service reference type.");
            }
            binding = null;
        }

        return binding;
    }

    @Override
    public void mergeOverrideInjectionBinding(Class<?> instanceClass, Member member,
                                              Resource resource, InjectionBinding<WebServiceRef> binding) throws InjectionException {
        WebServiceRef wsRef = WebServiceRefBindingBuilder.createWebServiceRefFromResource(resource, resource.type(), resource.name());
        binding.merge(wsRef, instanceClass, member);

    }
}
