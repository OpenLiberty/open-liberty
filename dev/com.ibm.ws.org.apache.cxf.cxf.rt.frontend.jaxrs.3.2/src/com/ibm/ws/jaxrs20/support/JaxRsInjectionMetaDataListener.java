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
package com.ibm.ws.jaxrs20.support;

import java.util.List;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
//import com.ibm.ws.jaxrs.support.JaxRsInstanceManager.InstanceInterceptor;
import com.ibm.ws.jaxrs20.metadata.JaxRsModuleMetaData;
import com.ibm.ws.jaxrs20.utils.JaxRsUtils;
import com.ibm.ws.runtime.metadata.ModuleMetaData;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.injectionengine.InjectionException;
import com.ibm.wsspi.injectionengine.InjectionMetaData;
import com.ibm.wsspi.injectionengine.InjectionMetaDataListener;

public class JaxRsInjectionMetaDataListener implements InjectionMetaDataListener {

    private static final TraceComponent tc = Tr.register(JaxRsInjectionMetaDataListener.class);

    /**
     * This method will store away the map of classes and injection metadata for later use. It will be stored in the
     * module metadata slot, and it will be used when creating instances of WAR based endpoints.
     */
    @Override
    public void injectionMetaDataCreated(InjectionMetaData injectionMetaData) throws InjectionException {

        ModuleMetaData mmd = injectionMetaData.getModuleMetaData();
        JaxRsModuleMetaData jaxWsmoduleMetaData = JaxRsMetaDataManager.getJaxRsModuleMetaData(mmd);

        if (jaxWsmoduleMetaData == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Unsupported Module, no JaxWsModuleMetaData is created for " + mmd.getName() + ", Injection Processing for web service is ignored");
            }
            return;
        }

        if (!jaxWsmoduleMetaData.getJ2EEName().equals(mmd.getJ2EEName())) {
            //Only process the injection event for the main module, e.g. EJB WS Router module and EJB module share the same JaxWsModuleMetaData
            //While, we only process the injection event if it is from EJB Module itself
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Not main module for the JaxWsModuleMetaData {0} or the jaxWsModuleMetaData has been initialized, Injection Processing for web service is ignored",
                         mmd.getName());
            }
            return;
        }

        List<Class<?>> injectionClasses = injectionMetaData.getComponentNameSpaceConfiguration().getInjectionClasses();
        if (injectionClasses != null) {
            for (Class<?> clazz : injectionClasses) {
                jaxWsmoduleMetaData.setReferenceContext(clazz, injectionMetaData.getReferenceContext());
            }
        }

        try {
            if (JaxRsUtils.isEJBModule(jaxWsmoduleMetaData.getModuleContainer())) {
                //only one ReferenceContextInjectionInstanceInterceptor is needed in InstanceManager
              //  InstanceInterceptor interceptor = jaxWsmoduleMetaData.getJaxWsInstanceManager().getInterceptor(ReferenceContextInjectionInstanceInterceptor.class.getName());
//                if (interceptor == null) {
//                    jaxWsmoduleMetaData.getJaxWsInstanceManager().addInterceptor(new ReferenceContextInjectionInstanceInterceptor(jaxWsmoduleMetaData.getReferenceContextMap()));
//                }
            }
        } catch (UnableToAdaptException e) {
            throw new InjectionException(e);
        }

    }
}
