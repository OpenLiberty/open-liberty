/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs20.api;

import java.lang.reflect.Method;

import javax.ws.rs.core.Application;

import org.apache.cxf.message.Message;

import com.ibm.ws.jaxrs20.metadata.CXFJaxRsProviderResourceHolder;
import com.ibm.ws.jaxrs20.metadata.EndpointInfo;
import com.ibm.ws.jaxrs20.metadata.JaxRsModuleMetaData;

/**
 * OSGi service interface. Expect to be able to customize the service beans
 * registered to the containers.
 * 
 * Useful when EJB, CDI integration to replace the service beans by decorated remote object.
 * 
 * Application
 * Provider
 * ResourceClasses
 * 
 */
public interface JaxRsFactoryBeanCustomizer {

    /**
     * we can directly assume Low < Medium < High, as enum already implement comparable.
     */
    public enum Priority {
        Lower, Low, Medium, High, Higher;
    }

    public static class BeanCustomizerContext {

        EndpointInfo endpointInfo;
        JaxRsModuleMetaData moduleMetaData;
        CXFJaxRsProviderResourceHolder cxfRPHolder;

        Object contextObject = null;

        public BeanCustomizerContext(EndpointInfo endpointInfo, JaxRsModuleMetaData moduleMetaData,
                                     CXFJaxRsProviderResourceHolder cxfRPHolder) {
            this.endpointInfo = endpointInfo;
            this.moduleMetaData = moduleMetaData;
            this.cxfRPHolder = cxfRPHolder;
        }

        /**
         * @return the endpointInfo
         */
        public EndpointInfo getEndpointInfo() {
            return endpointInfo;
        }

        public CXFJaxRsProviderResourceHolder getCxfRPHolder() {
            return cxfRPHolder;
        }

        /**
         * @param endpointInfo the endpointInfo to set
         */
        public void setEndpointInfo(EndpointInfo endpointInfo) {
            this.endpointInfo = endpointInfo;
        }

        /**
         * @return the moduleMetaData
         */
        public JaxRsModuleMetaData getModuleMetaData() {
            return moduleMetaData;
        }

        /**
         * @param moduleMetaData the moduleMetaData to set
         */
        public void setModuleMetaData(JaxRsModuleMetaData moduleMetaData) {
            this.moduleMetaData = moduleMetaData;
        }

        /**
         * @return the contextObject
         */
        public Object getContextObject() {
            return contextObject;
        }

        /**
         * @param contextObject the contextObject to set
         */
        public void setContextObject(Object contextObject) {
            this.contextObject = contextObject;
        }
    }

    Priority getPriority();

    Application onApplicationInit(Application app, JaxRsModuleMetaData metaData);

    void onPrepareProviderResource(BeanCustomizerContext context);

    boolean isCustomizableBean(Class<?> clazz, Object contextObject);

    <T> T onSingletonProviderInit(T provider, Object contextObject, Message message);

    <T> T onSingletonServiceInit(T service, Object contextObject);

    <T> T beforeServiceInvoke(T serviceObject, boolean isSingleton, Object contextObject);

//    Object beforeServiceInvoke(Class<?> clazz, boolean isSingleton, Object contextObject);

    Object serviceInvoke(Object serviceObject, Method m, Object[] params, boolean isSingleton, Object contextObject, Message inMessage) throws Exception;

    void afterServiceInvoke(Object serviceObject, boolean isSingleton, Object contextObject);

    <T> T onSetupProviderProxy(T provider, Object contextObject);

    /**
     * @param jaxRsModuleMetaData
     * 
     */
    void destroyApplicationScopeResources(JaxRsModuleMetaData jaxRsModuleMetaData);
}
