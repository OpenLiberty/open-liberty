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
package com.ibm.ws.jaxrs20.component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.container.service.app.deploy.extended.ExtendedModuleInfo;
import com.ibm.ws.container.service.metadata.MetaDataEvent;
import com.ibm.ws.container.service.metadata.MetaDataSlotService;
import com.ibm.ws.container.service.metadata.ModuleMetaDataListener;
import com.ibm.ws.container.service.metadata.extended.ModuleMetaDataExtender;
import com.ibm.ws.jaxrs20.JaxRsConstants;
import com.ibm.ws.jaxrs20.api.JaxRsModuleInfoBuilder;
import com.ibm.ws.jaxrs20.metadata.JaxRsModuleInfo;
import com.ibm.ws.jaxrs20.metadata.JaxRsModuleMetaData;
import com.ibm.ws.jaxrs20.metadata.JaxRsModuleType;
import com.ibm.ws.jaxrs20.support.JaxRsMetaDataManager;
import com.ibm.ws.jaxrs20.utils.JaxRsUtils;
import com.ibm.ws.runtime.metadata.ApplicationMetaData;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.runtime.metadata.ModuleMetaData;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.NonPersistentCache;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.classloading.ClassLoadingService;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

/**
 * Listening a Web/EJB module metadata events, and build JaxRsModuleInfos.
 */
@Component(immediate = true, property = { "service.vendor=IBM" }, configurationPolicy = ConfigurationPolicy.OPTIONAL)
public class JaxRsModuleMetaDataListener implements ModuleMetaDataListener, ModuleMetaDataExtender {

    private static final TraceComponent tc = Tr.register(JaxRsModuleMetaDataListener.class);

    private final Map<JaxRsModuleType, JaxRsModuleInfoBuilder> jaxRsModuleInfoBuilderMap = new ConcurrentHashMap<JaxRsModuleType, JaxRsModuleInfoBuilder>();

    private final AtomicServiceReference<ClassLoadingService> classLoadingServiceSR = new AtomicServiceReference<ClassLoadingService>(JaxRsConstants.CLASSlOADINGSERVICE_REFERENCE_NAME);

    @Override
    public ExtendedModuleInfo extendModuleMetaData(ExtendedModuleInfo moduleInfo) {

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "extendModuleMetaData(" + moduleInfo.getName() + ") : " + moduleInfo);
        }

        ModuleMetaData mmd = moduleInfo.getMetaData();

        Container moduleContainer = moduleInfo.getContainer();
        try {
            //228047:  Only EJB and Web modules are appropriate for JAXRS
            if (!(JaxRsUtils.isEJBModule(moduleContainer)) &&
                !(JaxRsUtils.isWebModule(moduleContainer))) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Unsupported Module, no JaxRsModuleMetaData will be created for " + moduleInfo.getName());
                }
                return null;
            }
        } catch (UnableToAdaptException e) {
            // If the moduleContainer is not adaptable the JaxRsUtils methods called above will throw
            // this exception.  Since this indicates that this module is not an EJB or Web module we
            // will simply return.
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Unsupported Module, no JaxRsModuleMetaData will be created for " + moduleInfo.getName());
            }
            return null;
        }
        try {
            JaxRsModuleMetaData jaxRsModuleMetaData = createJaxRsModuleMetaData(mmd, moduleContainer, moduleInfo.getClassLoader());
            JaxRsModuleInfo jaxRsModuleInfo = createJaxRsModuleInfo(moduleContainer);

            // process any nested ModuleMetaData instances
            for (ModuleMetaData nestedMMD : moduleInfo.getNestedMetaData()) {
                jaxRsModuleMetaData.getEnclosingModuleMetaDatas().add(nestedMMD);
                JaxRsMetaDataManager.setJaxRsModuleMetaData(nestedMMD, jaxRsModuleMetaData);
            }

            // find the builder
            JaxRsModuleInfoBuilder jaxRsModuleInfoBuilder = null;
            if (JaxRsUtils.isWebModule(moduleContainer)) {
                jaxRsModuleInfoBuilder = jaxRsModuleInfoBuilderMap.get(JaxRsModuleType.WEB);
            } else if (JaxRsUtils.isEJBModule(moduleContainer)) {
                jaxRsModuleInfoBuilder = jaxRsModuleInfoBuilderMap.get(JaxRsModuleType.EJB);
            }

            // build the JaxRsModuleInfo
            if (jaxRsModuleInfoBuilder != null) {
                return jaxRsModuleInfoBuilder.build(mmd, moduleContainer, jaxRsModuleInfo);
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "No JaxRsModuleInfoBuilder added to JaxRsModuleMetaDataListener.");
                }
                return null;
            }
        } catch (UnableToAdaptException e) {
            throw new IllegalStateException(e);
        }
    }

    private JaxRsModuleMetaData createJaxRsModuleMetaData(ModuleMetaData mmd, Container moduleContainer, ClassLoader moduleClassLoader) throws UnableToAdaptException {
        NonPersistentCache overlayCache = moduleContainer.adapt(NonPersistentCache.class);
        JaxRsModuleMetaData jaxRsModuleMetaData = (JaxRsModuleMetaData) overlayCache.getFromCache(JaxRsModuleMetaData.class);
        if (jaxRsModuleMetaData == null) {
            ClassLoader appContextClassLoader = classLoadingServiceSR.getServiceWithException().createThreadContextClassLoader(moduleClassLoader);
            jaxRsModuleMetaData = new JaxRsModuleMetaData(mmd, moduleContainer, appContextClassLoader);
            overlayCache.addToCache(JaxRsModuleMetaData.class, jaxRsModuleMetaData);
        } else {
            jaxRsModuleMetaData.getEnclosingModuleMetaDatas().add(mmd);
        }
        JaxRsMetaDataManager.setJaxRsModuleMetaData(mmd, jaxRsModuleMetaData);
        return jaxRsModuleMetaData;
    }

    private JaxRsModuleInfo createJaxRsModuleInfo(Container moduleContainer) throws UnableToAdaptException {
        // create the JaxRsModuleInfo
        NonPersistentCache overlayCache = moduleContainer.adapt(NonPersistentCache.class);
        JaxRsModuleInfo jaxRsModuleInfo = (JaxRsModuleInfo) overlayCache.getFromCache(JaxRsModuleInfo.class);
        if (jaxRsModuleInfo == null) { //when the ejb's router web module create, the jaxRsModuleInfo is not null here.
            if (JaxRsUtils.isWebModule(moduleContainer)) {
                jaxRsModuleInfo = new JaxRsModuleInfo(JaxRsModuleType.WEB);
            } else if (JaxRsUtils.isEJBModule(moduleContainer)) {
                jaxRsModuleInfo = new JaxRsModuleInfo(JaxRsModuleType.EJB);
            }
        }
        overlayCache.addToCache(JaxRsModuleInfo.class, jaxRsModuleInfo);
        return jaxRsModuleInfo;
    }

    @Override
    public void moduleMetaDataCreated(MetaDataEvent<ModuleMetaData> event) {
        //NO-OP
    }

    @Activate
    protected void activate(ComponentContext cc) {
        classLoadingServiceSR.activate(cc);
    }

    @Deactivate
    protected void deactivate(ComponentContext cc) {
        classLoadingServiceSR.deactivate(cc);
    }

    @Override
    public void moduleMetaDataDestroyed(MetaDataEvent<ModuleMetaData> event) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "moduleMetaDataDestroyed(" + event.getMetaData().getName() + ") : " + event.getMetaData());
        }

        try {
            Container moduleContainer = event.getContainer();
            if (moduleContainer != null) {
                NonPersistentCache overlayCache = moduleContainer.adapt(NonPersistentCache.class);
                overlayCache.removeFromCache(JaxRsModuleMetaData.class);
            }
        } catch (UnableToAdaptException ex) {
            // FFDC
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "moduleMetaDataDestroyed(" + event.getMetaData().getName() +
                             ") : Failed to remove metadata from overlay cache",
                         ex);
            }
        }

        JaxRsModuleMetaData moduleMetaData = JaxRsMetaDataManager.getJaxRsModuleMetaData(event.getMetaData());
        if (moduleMetaData != null) {
            JaxRsMetaDataManager.setJaxRsModuleMetaData(event.getMetaData(), null);
            //Only destroy while receiving the host module metadata event, e.g. for EJB-in-WAR, JaxRsModuleMetaData will be destroyed
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

    @Reference(name = "metaDataSlotService", service = MetaDataSlotService.class, cardinality = ReferenceCardinality.MANDATORY)
    protected void setMetaDataSlotService(MetaDataSlotService slotService) {
        JaxRsMetaDataManager.jaxrsApplicationSlot = slotService.reserveMetaDataSlot(ApplicationMetaData.class);
        JaxRsMetaDataManager.jaxrsModuleSlot = slotService.reserveMetaDataSlot(ModuleMetaData.class);
        JaxRsMetaDataManager.jaxrsComponentSlot = slotService.reserveMetaDataSlot(ComponentMetaData.class);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "setMetaDataSlotService : applicationSlot=" + JaxRsMetaDataManager.jaxrsApplicationSlot);
            Tr.debug(tc, "setMetaDataSlotService : moduleSlot=" + JaxRsMetaDataManager.jaxrsModuleSlot);
            Tr.debug(tc, "setMetaDataSlotService : componentSlot=" + JaxRsMetaDataManager.jaxrsComponentSlot);
        }
    }

    // declarative service
    protected void unsetMetaDataSlotService(MetaDataSlotService slotService) {
        JaxRsMetaDataManager.jaxrsApplicationSlot = null;
        JaxRsMetaDataManager.jaxrsModuleSlot = null;
        JaxRsMetaDataManager.jaxrsComponentSlot = null;
    }

    @Reference(name = "jaxRsModuleInfoBuilders", service = JaxRsModuleInfoBuilder.class, policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE)
    protected void registerJaxRsModuleInfoBuilder(JaxRsModuleInfoBuilder jaxRsModuleInfoBuilder) {
        jaxRsModuleInfoBuilderMap.put(jaxRsModuleInfoBuilder.getSupportType(), jaxRsModuleInfoBuilder);
    }

    protected void unregisterJaxRsModuleInfoBuilder(JaxRsModuleInfoBuilder jaxRsModuleInfoBuilder) {
        jaxRsModuleInfoBuilderMap.remove(jaxRsModuleInfoBuilder.getSupportType());
    }

    @Reference(name = JaxRsConstants.CLASSlOADINGSERVICE_REFERENCE_NAME, service = ClassLoadingService.class, cardinality = ReferenceCardinality.MANDATORY)
    protected void setClassLoadingService(ServiceReference<ClassLoadingService> ref) {
        classLoadingServiceSR.setReference(ref);
    }

    protected void unsetClassLoadingService(ServiceReference<ClassLoadingService> ref) {
        classLoadingServiceSR.unsetReference(ref);
    }
}
