/*******************************************************************************
 * Copyright (c) 2012, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.osgi.internal;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import com.ibm.ejs.container.EJBConfigurationException;
import com.ibm.ws.container.service.annotations.ModuleAnnotations;
import com.ibm.ws.container.service.annotations.WebAnnotations;
import com.ibm.ws.container.service.annocache.AnnotationsBetaHelper;
import com.ibm.ws.container.service.app.deploy.ClientModuleInfo;
import com.ibm.ws.container.service.app.deploy.EJBModuleInfo;
import com.ibm.ws.container.service.app.deploy.ModuleInfo;
import com.ibm.ws.container.service.app.deploy.WebModuleInfo;
import com.ibm.ws.javaee.dd.client.ApplicationClient;
import com.ibm.ws.javaee.dd.web.WebApp;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.NonPersistentCache;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.adaptable.module.adapters.ContainerAdapter;
import com.ibm.wsspi.anno.info.InfoStore;
import com.ibm.wsspi.anno.targets.AnnotationTargets_Targets;
import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.overlay.OverlayContainer;
import com.ibm.wsspi.classloading.ClassLoadingService;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

@Component(service = ContainerAdapter.class,
           property = { "toType=com.ibm.ws.ejbcontainer.osgi.internal.ModuleInitDataImpl",
                       "toType=com.ibm.ws.ejbcontainer.EJBEndpoints",
                       "toType=com.ibm.ws.ejbcontainer.ManagedBeanEndpoints" })
public class ModuleInitDataAdapter implements ContainerAdapter<ModuleInitDataImpl> {
    private static final Class<?> NON_PERSISTENT_CACHE_KEY = ModuleInitDataImpl.class;

    public static void removeFromCache(Container container) throws UnableToAdaptException {
        container.adapt(NonPersistentCache.class).removeFromCache(NON_PERSISTENT_CACHE_KEY);
    }

    private static final String REFERENCE_CLASS_LOADING_SERVICE = "classLoadingService";
    private static final String REFERENCE_MODULE_INIT_DATA_FACTORY = "moduleInitDataFactory";

    private final AtomicServiceReference<ClassLoadingService> classLoadingServiceSR =
        new AtomicServiceReference<ClassLoadingService>(REFERENCE_CLASS_LOADING_SERVICE);
    private final AtomicServiceReference<ModuleInitDataFactory> moduleInitDataFactorySR =
        new AtomicServiceReference<ModuleInitDataFactory>(REFERENCE_MODULE_INIT_DATA_FACTORY);

    @Reference(name = REFERENCE_CLASS_LOADING_SERVICE, service = ClassLoadingService.class)
    protected void setClassLoadingService(ServiceReference<ClassLoadingService> reference) {
        classLoadingServiceSR.setReference(reference);
    }

    protected void unsetClassLoadingService(ServiceReference<ClassLoadingService> reference) {
        classLoadingServiceSR.unsetReference(reference);
    }

    @Reference(name = REFERENCE_MODULE_INIT_DATA_FACTORY, service = ModuleInitDataFactory.class)
    protected void setModuleInitDataFactory(ServiceReference<ModuleInitDataFactory> reference) {
        moduleInitDataFactorySR.setReference(reference);
    }

    protected void unsetModuleInitDataFactory(ServiceReference<ModuleInitDataFactory> reference) {
        moduleInitDataFactorySR.unsetReference(reference);
    }

    @Activate
    protected void activate(ComponentContext cc) {
        classLoadingServiceSR.activate(cc);
        moduleInitDataFactorySR.activate(cc);
    }

    @Deactivate
    protected void deactivate(ComponentContext cc) {
        classLoadingServiceSR.deactivate(cc);
        moduleInitDataFactorySR.deactivate(cc);
    }

    /**
     * Obtain module initialization data for a container.
     *
     * Model data is obtained depending on the module type: EJB modules, Web modules,
     * and Application Client module types are handled.
     * 
     * Store new module initialization data to the target contain's non-persistent cache.
     * Attempt to obtain the initialization data from the non-persistent cache, and create
     * new data only if none was available in the cache.
     */
    @Override
    public ModuleInitDataImpl adapt(
        Container root,
        OverlayContainer rootOverlay,
        ArtifactContainer artifactContainer,
        Container containerToAdapt) throws UnableToAdaptException {

        NonPersistentCache cache = root.adapt(NonPersistentCache.class);

        ModuleInitDataImpl mid = (ModuleInitDataImpl) cache.getFromCache(NON_PERSISTENT_CACHE_KEY);
        if ( mid != null ) {
            return mid;
        }

        // Module initialization depends on module info (of the appropriate type),
        // on annotations data, and on descriptor data.  Obtain these based on the
        // module type.

        try {
            EJBModuleInfo ejbModuleInfo = (EJBModuleInfo) cache.getFromCache(EJBModuleInfo.class);
            if ( ejbModuleInfo != null ) {
                // EJB doesn't need annotation scan data, and doesn't need the metadata-complete setting.
                mid = createModuleInitData(ejbModuleInfo, null, null, false);

            } else {
                WebModuleInfo webModuleInfo = (WebModuleInfo) cache.getFromCache(WebModuleInfo.class);
                if ( webModuleInfo != null ) {
                    // Retrieval of the web annotations, web annotation targets, and web info store
                    // seems to be premature.  Will these be used if the web module is metadata-complete?

                    WebAnnotations webAnno = AnnotationsBetaHelper.getWebAnnotations( webModuleInfo.getContainer() );
                    AnnotationTargets_Targets webAnnoTargets = webAnno.getAnnotationTargets();
                    InfoStore webInfoStore = webAnno.getInfoStore();

                    WebApp webapp = containerToAdapt.adapt(WebApp.class);
                    boolean webIsMetadataComplete = isMetadataComplete(webapp);

                    mid = createModuleInitData(webModuleInfo, webAnnoTargets, webInfoStore, webIsMetadataComplete);

                } else {
                    ClientModuleInfo clientModuleInfo = (ClientModuleInfo) cache.getFromCache(ClientModuleInfo.class);
                    if ( clientModuleInfo != null ) {
                        // Retrieval of the client annotations, client annotation targets, and client info store
                        // seems to be premature.  Will these be used if the client module is metadata-complete?

                        ModuleAnnotations clientAnno = AnnotationsBetaHelper.getModuleAnnotations( clientModuleInfo.getContainer() );
                        AnnotationTargets_Targets clientAnnoTargets = clientAnno.getAnnotationTargets();
                        InfoStore clientInfoStore = clientAnno.getInfoStore();

                        ApplicationClient appClient = containerToAdapt.adapt(ApplicationClient.class);
                        boolean clientIsMetadataComplete = isMetadataComplete(appClient);
                        mid = createModuleInitData(clientModuleInfo, clientAnnoTargets, clientInfoStore, clientIsMetadataComplete);

                    } else {
                        // Unsupported module type!
                    }
                }
            }

        } catch ( EJBConfigurationException e ) {
            throw new UnableToAdaptException(e);
        }

        // Null if the module type is not EJB, WEB, or CLIENT.
        if ( mid != null ) {
            cache.addToCache(NON_PERSISTENT_CACHE_KEY, mid);
        }

        return mid;
    }

    private ModuleInitDataImpl createModuleInitData(ModuleInfo moduleInfo,
                                                    AnnotationTargets_Targets annoTargets,
                                                    InfoStore infoStore,
                                                    boolean defaultMetadataComplete) throws EJBConfigurationException {
        Container container = moduleInfo.getContainer();
        ClassLoader classLoader = moduleInfo.getClassLoader();
        String modName = moduleInfo.getURI();
        String modLogicalName = moduleInfo.getName();
        String appName = moduleInfo.getApplicationInfo().getDeploymentName();
        ModuleInitDataFactory moduleInitDataFactory = moduleInitDataFactorySR.getServiceWithException();
        return moduleInitDataFactory.createModuleInitData(container,
                                                          classLoader,
                                                          modName, modLogicalName, appName,
                                                          annoTargets, infoStore, defaultMetadataComplete);
    }

    private boolean isMetadataComplete(WebApp webapp) {
        if (webapp == null)
            return false;

        // Versions before 2.5 are implicitly metadata complete.
        String version = webapp.getVersion();
        if (version.startsWith("1."))
            return true;
        if (version.startsWith("2.") && !version.equals("2.5"))
            return true;

        return webapp.isMetadataComplete();
    }

    private boolean isMetadataComplete(ApplicationClient clientapp) {
        if (clientapp == null)
            return false;

        // Versions before 5 are implicitly metadata complete. They include 1.1, 1.2, 1.3, & 1.4
        String version = clientapp.getVersion();
        if (version.startsWith("1."))
            return true;

        return clientapp.isMetadataComplete();
    }
}
