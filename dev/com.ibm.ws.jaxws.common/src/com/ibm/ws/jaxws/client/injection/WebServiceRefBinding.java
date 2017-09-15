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

import javax.jws.HandlerChain;
import javax.xml.ws.RespectBinding;
import javax.xml.ws.Service;
import javax.xml.ws.WebServiceRef;
import javax.xml.ws.soap.Addressing;
import javax.xml.ws.soap.MTOM;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.jaxws.metadata.WebServiceRefInfo;
import com.ibm.wsspi.injectionengine.ComponentNameSpaceConfiguration;
import com.ibm.wsspi.injectionengine.InjectionBinding;
import com.ibm.wsspi.injectionengine.InjectionException;

/**
 * This class will be the InjectionBinding object type responsible for handling @WebServiceRef annotations. This will be
 * created when the WebServiceRefProcesor encounters metadata describing a service ref that was defined either by an @WebServiceRef
 * annotation or in XML. This class will be called by the injection engine to handle metadata that was found for a
 * service ref that already has a WebServiceRefBinding instance.
 * 
 */
public class WebServiceRefBinding extends InjectionBinding<WebServiceRef> {

    private static final TraceComponent tc = Tr.register(WebServiceRefBinding.class);

    protected WebServiceRefInfo wsrInfo;

    // this will indicate whether or not this binding represents a binding
    // for an @Resource annotation being used for web service reference
    // type injection
    private boolean resourceType = false;

    public WebServiceRefBinding(WebServiceRef annotation, ComponentNameSpaceConfiguration nameSpaceConfig) {
        super(annotation, nameSpaceConfig);
        setJndiName(annotation.name());
    }

    public WebServiceRefBinding(WebServiceRefInfo wsrInfo, ComponentNameSpaceConfiguration nameSpaceConfig) {
        super(wsrInfo.getAnnotationValue(), nameSpaceConfig);
        setJndiName(wsrInfo.getJndiName());
        this.wsrInfo = wsrInfo;

    }

    protected void mergeWSRInfo(WebServiceRef webServiceRef, Class<?> instanceClass, Member member) {
        if (member == null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Merging @WebServiceRef annotation found on the class: " + instanceClass.getName());
            }

        } else if (member instanceof Method) {
            Method method = (Method) member;
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Merging @WebServiceRef or @Resource annotation on the " + method.getName() + " method, in the "
                             + method.getDeclaringClass().getName() + " class.");
            }

        } else { // Field
            Field field = (Field) member;
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Merging @WebServiceRef or @Resource annotation on the " + field.getName() + " field, in the "
                             + field.getDeclaringClass().getName() + " class.");
            }

            if (isResourceType()) {
                // Merge any features that might be set on the field (@MTOM, @RespectBinding, @Addressing)
                // Note that we only support features for port-component-ref type injections.
                // For a port-component-ref type injection, the "Service SEI class" will be set on
                // 'wsrMetadata'.
                if (wsrInfo.getServiceRefTypeClassName() != null) {
                    WebServiceRefProcessor.handleMTOM(webServiceRef, wsrInfo, field.getAnnotation(MTOM.class));
                    WebServiceRefProcessor.handleRespectBinding(webServiceRef, wsrInfo, field.getAnnotation(RespectBinding.class));
                    WebServiceRefProcessor.handleAddressing(webServiceRef, wsrInfo, field.getAnnotation(Addressing.class));
                }
            }
        }
    }

    /**
     * This method is called when an @WebServiceRef annotation with a name that has already been processed is found on
     * additional fields or methods. This allows us to validate and augment any existing metadata for this service
     * reference.
     */
    @Override
    public void merge(WebServiceRef webServiceRef, Class<?> instanceClass, Member member) throws InjectionException {

        mergeWSRInfo(webServiceRef, instanceClass, member);

        // The former value should override the later one, i.e. the settings in DD should override the ones in annotation,
        // but we prefer the subclass.
        Class<?> annoType = webServiceRef.type();
        if (!annoType.getName().equals(Object.class.getName())) {
            Class<?> existingType = loadClass(wsrInfo.getServiceRefTypeClassName());

            if (!existingType.getName().equals(Object.class.getName())) {
                //prefer the subclass
                if (existingType.isAssignableFrom(annoType)) {
                    wsrInfo.setServiceRefTypeClassName(annoType.getName());
                } else if (annoType.isAssignableFrom(existingType)) {
                    wsrInfo.setServiceRefTypeClassName(existingType.getName());
                } else { // conflict
                    String msgKey;
                    Object[] inserts;
                    if (member != null) {
                        msgKey = "error.service.ref.type.mismatch.for.member";
                        inserts = new Object[] { wsrInfo.getJndiName(), member.getName(), member.getDeclaringClass().getName(),
                                                annoType.getName(), existingType.getName() };
                    } else {
                        msgKey = "error.service.ref.type.mismatch.for.class";
                        inserts = new Object[] { wsrInfo.getJndiName(), annoType.getName(), existingType.getName() };
                    }
                    Tr.error(tc, msgKey, inserts);
                    throw new InjectionException(Tr.formatMessage(tc, msgKey, inserts));
                }
            } else {
                wsrInfo.setServiceRefTypeClassName(annoType.getName());
            }
        }

        Class<?> annoValue = webServiceRef.value();
        if (!annoValue.getName().equals(Service.class.getName())) {
            Class<?> existingValue = loadClass(wsrInfo.getServiceInterfaceClassName());

            if (!existingValue.getName().equals(Service.class.getName())) {
                //prefer the subclass
                if (existingValue.isAssignableFrom(annoValue)) {
                    wsrInfo.setServiceInterfaceClassName(annoValue.getName());
                } else if (annoValue.isAssignableFrom(existingValue)) {
                    wsrInfo.setServiceInterfaceClassName(existingValue.getName());
                } else { // conflict
                    String msgKey;
                    Object[] inserts;
                    if (member != null) {
                        msgKey = "error.service.interface.mismatch.for.member";
                        inserts = new Object[] { wsrInfo.getJndiName(), member.getName(), member.getDeclaringClass().getName(), annoValue.getName(),
                                                existingValue.getName() };
                    } else {
                        msgKey = "error.service.interface.mismatch.for.class";
                        inserts = new Object[] { wsrInfo.getJndiName(), annoValue.getName(), existingValue.getName() };
                    }
                    Tr.error(tc, msgKey, inserts);
                    throw new InjectionException(Tr.formatMessage(tc, msgKey, inserts));
                }
            } else {
                wsrInfo.setServiceInterfaceClassName(annoValue.getName());
            }
        }

        // Validate and set value and type
        if (member == null) {
            validateAndSetClassLevelWebServiceRef(instanceClass);
        } else {
            // If the wsrInfo service ref type hasn't been set, the infer it from the member.
            validateAndSetMemberLevelWebServiceRef(member);
        }

        // now check the WSDL locations
        if (webServiceRef.wsdlLocation() != null && !webServiceRef.wsdlLocation().isEmpty()) {
            processExistingWSDL(webServiceRef.wsdlLocation(), member);
        }

        // now check to see if there was an @HandlerChain annotation on this member
        HandlerChain handlerChainInstance = null;
        if (member == null) {
            handlerChainInstance = instanceClass.getAnnotation(HandlerChain.class);
        } else if (member instanceof Field) {
            Field field = (Field) member;
            handlerChainInstance = field.getAnnotation(HandlerChain.class);
        } else if (member instanceof Method) {
            Method method = (Method) member;
            handlerChainInstance = method.getAnnotation(HandlerChain.class);
        }
        if (handlerChainInstance != null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "@HandlerChain annotation found with @WebServiceRef annotation, jndiName: " + webServiceRef.name());
            }
            wsrInfo.setHandlerChainDeclaringClassName(instanceClass.getName());
            wsrInfo.setHandlerChainAnnotation(handlerChainInstance);
        }

    }

    private void validateAndSetClassLevelWebServiceRef(Class<?> instanceClass) throws InjectionException {
        Class<?> typeClass = loadClass(wsrInfo.getServiceRefTypeClassName());
        Class<?> valueClass = loadClass(wsrInfo.getServiceInterfaceClassName());

        if (typeClass.getName().equals(Object.class.getName())) { // type is not specified, so try service type injection
            // in this case the 'value' attribute MUST specify a subclass of javax.xml.ws.Service
            if (!Service.class.isAssignableFrom(valueClass) || valueClass.getName().equals(Service.class.getName())) {
                Tr.error(tc, "error.service.ref.class.level.merge.service.ref.type.absent", instanceClass.getName());
                throw new InjectionException(Tr.formatMessage(tc, "error.service.ref.class.level.merge.service.ref.type.absent",
                                                              instanceClass.getName()));
            }

            wsrInfo.setServiceInterfaceClassName(valueClass.getName());
            wsrInfo.setServiceRefTypeClassName(valueClass.getName());

        } else if (Service.class.isAssignableFrom(typeClass)) { // service type injection case
            if (typeClass.getName().equals(Service.class.getName())) { // typeClass is javax.xml.ws.Service
                // in this case the 'value' attribute MUST specify a subclass of javax.xml.ws.Service
                if (!Service.class.isAssignableFrom(valueClass) || valueClass.getName().equals(Service.class.getName())) {
                    Tr.error(tc, "error.service.ref.class.level.merge.service.interface.wrong.value",
                             instanceClass.getName(), valueClass.getName());
                    throw new InjectionException(Tr.formatMessage(tc, "error.service.ref.class.level.merge.service.interface.wrong.value",
                                                                  instanceClass.getName(), valueClass.getName()));
                }

                wsrInfo.setServiceInterfaceClassName(valueClass.getName());
                wsrInfo.setServiceRefTypeClassName(valueClass.getName());

            } else { //typeClass is a subclass of javax.xml.ws.Service
                // in this case the 'value' attribute MUST be the same as 'type' or use default javax.xml.ws.Service
                if (!valueClass.getName().equals(typeClass.getName()) && !valueClass.getName().equals(Service.class.getName())) {
                    Tr.error(tc, "error.service.ref.class.level.merge.service.interface.and.service.ref.type.not.same",
                             instanceClass.getName());
                    throw new InjectionException(Tr.formatMessage(tc, "error.service.ref.class.level.merge.service.interface.and.service.ref.type.not.same",
                                                                  instanceClass.getName()));
                }

                wsrInfo.setServiceInterfaceClassName(typeClass.getName());
                wsrInfo.setServiceRefTypeClassName(typeClass.getName());
            }

        } else { // port type injection case
            // in this case the 'value' attribute MUST specify a subclass of javax.xml.ws.Service
            if (!Service.class.isAssignableFrom(valueClass) || valueClass.getName().equals(Service.class.getName())) {
                Tr.error(tc, "error.service.ref.class.level.merge.service.interface.wrong.value",
                         instanceClass.getName(), valueClass.getName());
                throw new InjectionException(Tr.formatMessage(tc, "error.service.ref.class.level.merge.service.interface.wrong.value",
                                                              instanceClass.getName(), valueClass.getName()));
            }

            wsrInfo.setServiceInterfaceClassName(valueClass.getName());
            wsrInfo.setServiceRefTypeClassName(typeClass.getName());
        }
    }

    private void validateAndSetMemberLevelWebServiceRef(Member member) throws InjectionException {

        Class<?> typeClass = loadClass(wsrInfo.getServiceRefTypeClassName());
        Class<?> valueClass = loadClass(wsrInfo.getServiceInterfaceClassName());

        // get the effective type from the member type and wsrInfo.getServiceRefTypeClassName()
        Class<?> memberType = InjectionHelper.getTypeFromMember(member);
        Class<?> effectiveType;
        if (memberType.getName().equals(Object.class.getName())) { //memberType is Object.class
            effectiveType = typeClass;
            // effectiveType could be Object here
            if (tc.isDebugEnabled() && typeClass.getName().equals(Object.class.getName())) {
                Tr.debug(tc, "The member type and the @WebServiceRef.type on the " + member.getName() + " in " + member.getDeclaringClass().getName()
                             + "are all Object.class, so can not infer one. Will try service type injection if the @WebServiceRef.value is a subclass of Service.class.");
            }

        } else { //memberType is not Object.class
            // we prefer subclass
            if (memberType.isAssignableFrom(typeClass)) {
                effectiveType = typeClass;
            } else if (typeClass.isAssignableFrom(memberType)) {
                effectiveType = memberType;
            } else {
                Tr.error(tc, "error.service.ref.member.level.merge.service.ref.type.not.compatible",
                         member.getName(), member.getDeclaringClass().getName(), typeClass.getName(), memberType.getName());
                throw new InjectionException(Tr.formatMessage(tc, "error.service.ref.member.level.merge.service.ref.type.not.compatible",
                                                              member.getName(), member.getDeclaringClass().getName(), typeClass.getName(), memberType.getName()));
            }
        }

        // validate and set value and type in WebServiceRefInfo
        if (effectiveType.getName().equals(Object.class.getName())) { // effectiveType can not be determined above, so try service type injection
            // in this case the 'value' attribute MUST specify a subclass of javax.xml.ws.Service
            if (!Service.class.isAssignableFrom(valueClass) || valueClass.getName().equals(Service.class.getName())) {
                Tr.error(tc, "error.service.ref.member.level.merge.service.ref.type.not.inferred",
                         member.getName(), member.getDeclaringClass().getName());
                throw new InjectionException(Tr.formatMessage(tc, "error.service.ref.member.level.merge.service.ref.type.not.inferred",
                                                              member.getName(), member.getDeclaringClass().getName()));
            }

            wsrInfo.setServiceRefTypeClassName(valueClass.getName());
            wsrInfo.setServiceInterfaceClassName(valueClass.getName());

        } else if (Service.class.isAssignableFrom(effectiveType)) { // service type injection case
            if (effectiveType.getName().equals(Service.class.getName())) { // effectiveType is javax.xml.ws.Service
                // in this case the 'value' attribute MUST specify a subclass of javax.xml.ws.Service
                if (!Service.class.isAssignableFrom(valueClass) || valueClass.getName().equals(Service.class.getName())) {
                    Tr.error(tc, "error.service.ref.member.level.merge.service.interface.wrong.value",
                             member.getName(), member.getDeclaringClass(), valueClass.getName());
                    throw new InjectionException(Tr.formatMessage(tc, "error.service.ref.member.level.merge.service.interface.wrong.value",
                                                                  member.getName(), member.getDeclaringClass(), valueClass.getName()));
                }
                wsrInfo.setServiceInterfaceClassName(valueClass.getName());
                wsrInfo.setServiceRefTypeClassName(valueClass.getName());

            } else { //effectiveType is a subclass of javax.xml.ws.Service
                // in this case the 'value' attribute MUST be the same as 'type' or use default javax.xml.ws.Service
                if (!valueClass.getName().equals(effectiveType.getName()) && !valueClass.getName().equals(Service.class.getName())) {
                    Tr.error(tc, "error.service.ref.member.level.merge.service.interface.and.service.ref.type.not.same",
                             member.getName(), member.getDeclaringClass().getName());
                    throw new InjectionException(Tr.formatMessage(tc, "error.service.ref.member.level.merge.service.interface.and.service.ref.type.not.same",
                                                                  member.getName(), member.getDeclaringClass().getName()));
                }

                wsrInfo.setServiceInterfaceClassName(effectiveType.getName());
                wsrInfo.setServiceRefTypeClassName(effectiveType.getName());
            }

        } else { // port type injection case
            // in this case the 'value' attribute MUST specify a subclass of javax.xml.ws.Service
            if (!Service.class.isAssignableFrom(valueClass) || valueClass.getName().equals(Service.class.getName())) {
                Tr.error(tc, "error.service.ref.member.level.merge.service.interface.wrong.value",
                         member.getName(), member.getDeclaringClass(), valueClass.getName());
                throw new InjectionException(Tr.formatMessage(tc, "error.service.ref.member.level.merge.service.interface.wrong.value",
                                                              member.getName(), member.getDeclaringClass(), valueClass.getName()));
            }

            wsrInfo.setServiceRefTypeClassName(effectiveType.getName());
            wsrInfo.setServiceInterfaceClassName(valueClass.getName());
        }
    }

    /**
     * This will compare the wsdlLocation attribute of the various annotations that have refer to the same service
     * reference. If they differ we will throw an exception as the runtime will not be able to determine which WSDL to
     * use. We will only do this checking if there was not a WSDL location specified in the deployment descriptor. If
     * the DD specifies a deployment descriptor that value will be used, and other values will be ignored.
     */
    private void processExistingWSDL(String wsdlLocation, Member newMember) throws InjectionException {
        // if the wsdlLocation for this service reference is specified in the DD, log a debug statement and continue
        if (wsrInfo.getWsdlLocation() != null && !"".equals(wsrInfo.getWsdlLocation())) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "For the " + wsrInfo.getJndiName() + " service reference, " + "the " + wsrInfo.getWsdlLocation()
                             + " WSDL file is specified in the " + "deployment descriptor. Annotation metadata referencing a WSDL file for "
                             + "this service reference will be ignored.");
            }
        } else { // else we want to set the value to whatever is in the annotation
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "For the " + wsrInfo.getJndiName() + " service reference, " + "setting the wsdlLocation: " + wsdlLocation);
            }
            wsrInfo.setWsdlLocation(wsdlLocation);
        }
    }

    public WebServiceRefInfo getWebServiceRefInfo() {
        return wsrInfo;
    }

    public void setWebServiceRefInfo(WebServiceRefInfo info) {
        this.wsrInfo = info;
    }

    public boolean isResourceType() {
        return resourceType;
    }

    public void setResourceType(boolean resourceType) {
        this.resourceType = resourceType;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(this.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(this)) + "\n");
        sb.append("resourceType: " + String.valueOf(resourceType));
        sb.append("\nsuperclass: {\n" + super.toString() + "\n}");
        sb.append("\nwsrInfo: " + (wsrInfo != null ? "{\n" + wsrInfo.toString() + "\n}" : "<null>"));

        return sb.toString();
    }
}
