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
package com.ibm.ws.jaxws.support;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.clientcontainer.metadata.ClientModuleMetaData;
import com.ibm.ws.container.service.metadata.MetaDataEvent;
import com.ibm.ws.container.service.metadata.MetaDataSlotService;
import com.ibm.ws.container.service.metadata.ModuleMetaDataListener;
import com.ibm.ws.jaxws.metadata.JaxWsModuleInfo;
import com.ibm.ws.jaxws.metadata.JaxWsModuleMetaData;
import com.ibm.ws.jaxws.metadata.JaxWsModuleType;
import com.ibm.ws.jaxws.metadata.builder.JaxWsModuleInfoBuilder;
import com.ibm.ws.jaxws.utils.JaxWsUtils;
import com.ibm.ws.runtime.metadata.ApplicationMetaData;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.runtime.metadata.ModuleMetaData;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.NonPersistentCache;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.classloading.ClassLoadingService;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

/**
 * Listening a Web/EJB module metadata events, and build JaxWsModuleInfos.
 */
public class JaxWsModuleMetaDataListener implements ModuleMetaDataListener {

    private static final TraceComponent tc = Tr.register(JaxWsModuleMetaDataListener.class);

    private final Map<JaxWsModuleType, JaxWsModuleInfoBuilder> jaxWsModuleInfoBuilderMap = new ConcurrentHashMap<JaxWsModuleType, JaxWsModuleInfoBuilder>();

    private final AtomicServiceReference<ClassLoadingService> classLoadingServiceSR = new AtomicServiceReference<ClassLoadingService>("classLoadingService");

    @Override
    public void moduleMetaDataCreated(MetaDataEvent<ModuleMetaData> event) {

        ModuleMetaData mmd = event.getMetaData();

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "moduleMetaDataCreated(" + mmd.getName() + ") : " + mmd);
        }

        //Iris Change start
//        ModuleInfo moduleInfo = JaxWsUtils.getModuleInfo(event.getContainer());
//        if (moduleInfo == null) {
//            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
//                Tr.debug(tc, "Unsupported Module, no JaxWsModuleMetaData will be created for " + mmd.getName());
//            }
//            return;
//        }
        if (!(mmd instanceof ClientModuleMetaData))
            return;
        //Iris Chane end

        Container moduleContainer = event.getContainer();
        try {
            NonPersistentCache overlayCache = moduleContainer.adapt(NonPersistentCache.class);
            JaxWsModuleMetaData jaxWsModuleMetaData = (JaxWsModuleMetaData) overlayCache.getFromCache(JaxWsModuleMetaData.class);
            if (jaxWsModuleMetaData == null) {
                //Iris Change start
//                ClassLoader appContextClassLoader = classLoadingServiceSR.getServiceWithException().createThreadContextClassLoader(moduleInfo.getClassLoader());
                ClassLoader appContextClassLoader = classLoadingServiceSR.getServiceWithException().createThreadContextClassLoader(((ClientModuleMetaData) mmd).getModuleInfo().getClassLoader());
                //Iris Change End
                jaxWsModuleMetaData = new JaxWsModuleMetaData((ClientModuleMetaData) mmd, event.getContainer(), appContextClassLoader);
                overlayCache.addToCache(JaxWsModuleMetaData.class, jaxWsModuleMetaData);
            } else {
                jaxWsModuleMetaData.getEnclosingModuleMetaDatas().add(mmd);
            }
            JaxWsMetaDataManager.setJaxWsModuleMetaData(mmd, jaxWsModuleMetaData);

            // create the JaxWsModuleInfo
            JaxWsModuleInfo jaxWsModuleInfo = (JaxWsModuleInfo) overlayCache.getFromCache(JaxWsModuleInfo.class);
            if (jaxWsModuleInfo == null) { //when the ejb's router web module create, the jaxWsModuleInfo is not null here.
                if (JaxWsUtils.isWebModule(moduleContainer)) {
                    jaxWsModuleInfo = new JaxWsModuleInfo(JaxWsModuleType.WEB);
                } else if (JaxWsUtils.isEJBModule(moduleContainer)) {
                    jaxWsModuleInfo = new JaxWsModuleInfo(JaxWsModuleType.EJB);
                }
            }
            overlayCache.addToCache(JaxWsModuleInfo.class, jaxWsModuleInfo);

            //Iris Change: no need this part for client container
            // find the builder
//            JaxWsModuleInfoBuilder jaxWsModuleInfoBuilder = null;
//            if (JaxWsUtils.isWebModule(moduleContainer)) {
//                jaxWsModuleInfoBuilder = jaxWsModuleInfoBuilderMap.get(JaxWsModuleType.WEB);
//            } else if (JaxWsUtils.isEJBModule(moduleContainer)) {
//                jaxWsModuleInfoBuilder = jaxWsModuleInfoBuilderMap.get(JaxWsModuleType.EJB);
//            }
//
//            // build the JaxWsModuleInfo
//            if (jaxWsModuleInfoBuilder != null) {
//                jaxWsModuleInfoBuilder.build(mmd, moduleContainer, jaxWsModuleInfo);
//            } else {
//                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
//                    Tr.debug(tc, "No JaxWsModuleInfoBuilder added to JaxWsModuleMetaDataListener.");
//                }
//            }
            //Iris Change End

        } catch (UnableToAdaptException e) {
            throw new IllegalStateException(e);
        }
    }

    protected void activate(ComponentContext cc) {
        classLoadingServiceSR.activate(cc);
    }

    protected void deactivate(ComponentContext cc) {
        classLoadingServiceSR.deactivate(cc);
    }

    @Override
    public void moduleMetaDataDestroyed(MetaDataEvent<ModuleMetaData> event) {

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "moduleMetaDataDestroyed(" + event.getMetaData().getName() + ") : " + event.getMetaData());
        }

        JaxWsModuleMetaData moduleMetaData = JaxWsMetaDataManager.getJaxWsModuleMetaData(event.getMetaData());
        if (moduleMetaData != null) {
            JaxWsMetaDataManager.setJaxWsModuleMetaData(event.getMetaData(), null);
            //Only destroy while receiving the host module metadata event, e.g. for EJB-in-WAR, JaxWsModuleMetaData will be destroyed
            //if the current event is for the web module.
            if (moduleMetaData.getJ2EEName().equals(event.getMetaData().getJ2EEName())) {
                moduleMetaData.destroy();
                //distroy context classlodaer
                ClassLoadingService classLoadingService = classLoadingServiceSR.getService();
                if (classLoadingService != null) {
                    classLoadingService.destroyThreadContextClassLoader(moduleMetaData.getAppContextClassLoader());
                }
            }
        }

    }

    // declarative service
    public void setMetaDataSlotService(MetaDataSlotService slotService) {
        JaxWsMetaDataManager.jaxwsApplicationSlot = slotService.reserveMetaDataSlot(ApplicationMetaData.class);
        JaxWsMetaDataManager.jaxwsModuleSlot = slotService.reserveMetaDataSlot(ModuleMetaData.class);
        JaxWsMetaDataManager.jaxwsComponentSlot = slotService.reserveMetaDataSlot(ComponentMetaData.class);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "setMetaDataSlotService : applicationSlot=" + JaxWsMetaDataManager.jaxwsApplicationSlot);
            Tr.debug(tc, "setMetaDataSlotService : moduleSlot=" + JaxWsMetaDataManager.jaxwsModuleSlot);
            Tr.debug(tc, "setMetaDataSlotService : componentSlot=" + JaxWsMetaDataManager.jaxwsComponentSlot);
        }
    }

    // declarative service
    public void unsetMetaDataSlotService(MetaDataSlotService slotService) {
        JaxWsMetaDataManager.jaxwsApplicationSlot = null;
        JaxWsMetaDataManager.jaxwsModuleSlot = null;
        JaxWsMetaDataManager.jaxwsComponentSlot = null;
    }

    public void registerJaxWsModuleInfoBuilder(JaxWsModuleInfoBuilder jaxWsModuleInfoBuilder) {
        jaxWsModuleInfoBuilderMap.put(jaxWsModuleInfoBuilder.getSupportType(), jaxWsModuleInfoBuilder);
    }

    public void unregisterJaxWsModuleInfoBuilder(JaxWsModuleInfoBuilder jaxWsModuleInfoBuilder) {
        jaxWsModuleInfoBuilderMap.remove(jaxWsModuleInfoBuilder.getSupportType());
    }

    protected void setClassLoadingService(ServiceReference<ClassLoadingService> ref) {
        classLoadingServiceSR.setReference(ref);
    }

    protected void unsetClassLoadingService(ServiceReference<ClassLoadingService> ref) {
        classLoadingServiceSR.unsetReference(ref);
    }
}
