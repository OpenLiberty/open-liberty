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
package com.ibm.ws.jaxws.webcontainer;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.app.manager.module.DeployedAppInfo;
import com.ibm.ws.app.manager.module.internal.ModuleClassLoaderFactory;
import com.ibm.ws.app.manager.module.internal.ModuleHandler;
import com.ibm.ws.app.manager.module.internal.WebModuleInfoImpl;
import com.ibm.ws.container.service.app.deploy.ApplicationClassesContainerInfo;
import com.ibm.ws.container.service.app.deploy.ApplicationInfo;
import com.ibm.ws.container.service.app.deploy.ContainerInfo;
import com.ibm.ws.container.service.app.deploy.ModuleInfo;
import com.ibm.ws.container.service.app.deploy.extended.ExtendedModuleInfo;
import com.ibm.ws.container.service.app.deploy.extended.ExtendedWebModuleInfo;
import com.ibm.ws.container.service.metadata.MetaDataException;
import com.ibm.ws.javaee.dd.web.WebApp;
import com.ibm.ws.jaxws.metadata.JaxWsModuleInfo;
import com.ibm.ws.jaxws.metadata.JaxWsModuleMetaData;
import com.ibm.ws.jaxws.support.JaxWsMetaDataManager;
import com.ibm.ws.jaxws.support.JaxWsWebContainerManager;
import com.ibm.ws.jaxws.webcontainer.internal.FileUtils;
import com.ibm.ws.runtime.metadata.ModuleMetaData;
import com.ibm.wsspi.adaptable.module.AdaptableModuleFactory;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.NonPersistentCache;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.factory.ArtifactContainerFactory;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

/**
 * JaxWsWebContainerManager is used for managing the lifecycle of web router modules.
 * Mostly, it is used for EJB based web services, which are required to publish as web endpoints
 */
public class JaxWsWebContainerManagerImpl implements JaxWsWebContainerManager {

    private static final TraceComponent tc = Tr.register(JaxWsWebContainerManagerImpl.class);

    private final AtomicServiceReference<ModuleHandler> webModuleHandlerRef = new AtomicServiceReference<ModuleHandler>("webModuleHandler");

    private final AtomicServiceReference<ArtifactContainerFactory> artifactContainerFactoryRef = new AtomicServiceReference<ArtifactContainerFactory>("artifactContainerFactory");;

    private final AtomicServiceReference<AdaptableModuleFactory> adaptableModuleFactoryRef = new AtomicServiceReference<AdaptableModuleFactory>("adaptableModuleFactory");;

    private final AtomicServiceReference<WsLocationAdmin> locationAdminRef = new AtomicServiceReference<WsLocationAdmin>("wsLocationAdmin");

    private final Map<J2EEName, File> moduleCacheBaseDirectoryMap = new ConcurrentHashMap<J2EEName, File>();

    private File cacheBaseDirectory;

    protected void activate(ComponentContext cc) {
        webModuleHandlerRef.activate(cc);
        artifactContainerFactoryRef.activate(cc);
        adaptableModuleFactoryRef.activate(cc);
        locationAdminRef.activate(cc);

        cacheBaseDirectory = locationAdminRef.getServiceWithException().getBundleFile(this, "routermodules");
        if (!FileUtils.ensureDirExists(cacheBaseDirectory)) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Could not create directory at {0}.", cacheBaseDirectory.getAbsolutePath());
            }
        }
    }

    protected void deactivate(ComponentContext cc) {
        //Stop all managed web container manager
        for (File moduleCacheBaseDirectory : moduleCacheBaseDirectoryMap.values()) {
            FileUtils.recursiveDelete(moduleCacheBaseDirectory);
        }
        moduleCacheBaseDirectoryMap.clear();

        webModuleHandlerRef.deactivate(cc);
        artifactContainerFactoryRef.deactivate(cc);
        adaptableModuleFactoryRef.deactivate(cc);
        locationAdminRef.deactivate(cc);
    }

    protected void setWebModuleHandler(ServiceReference<ModuleHandler> ref) {
        webModuleHandlerRef.setReference(ref);
    }

    protected void unsetWebModuleHandler(ServiceReference<ModuleHandler> ref) {
        webModuleHandlerRef.setReference(null);
    }

    protected void setArtifactContainerFactory(ServiceReference<ArtifactContainerFactory> ref) {
        artifactContainerFactoryRef.setReference(ref);
    }

    protected void unsetArtifactContainerFactory(ServiceReference<ArtifactContainerFactory> ref) {
        artifactContainerFactoryRef.unsetReference(ref);
    }

    protected void setAdaptableModuleFactory(ServiceReference<AdaptableModuleFactory> ref) {
        adaptableModuleFactoryRef.setReference(ref);
    }

    protected void unsetAdaptableModuleFactory(ServiceReference<AdaptableModuleFactory> ref) {
        adaptableModuleFactoryRef.unsetReference(ref);
    }

    protected void setWsLocationAdmin(ServiceReference<WsLocationAdmin> ref) {
        locationAdminRef.setReference(ref);
    }

    protected void unsetWsLocationAdmin(ServiceReference<WsLocationAdmin> ref) {
        locationAdminRef.unsetReference(ref);
    }

    @Override
    public ExtendedWebModuleInfo createWebModuleInfo(ExtendedModuleInfo moduleInfo, String contextRoot) throws UnableToAdaptException {
        try {
            File moduleCacheBaseDirectory = new File(cacheBaseDirectory, UUID.randomUUID().toString());
            if (!FileUtils.ensureDirExists(moduleCacheBaseDirectory)) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Could not create directory at {0}.", moduleCacheBaseDirectory.getAbsolutePath());
                }
            }

            File cacheDirectory = new File(moduleCacheBaseDirectory, "cache");
            if (!FileUtils.ensureDirExists(cacheDirectory)) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Could not create directory at {0}.", cacheDirectory.getAbsolutePath());
                }
            }

            File cacheAdaptDirectory = new File(moduleCacheBaseDirectory, "cacheAdapt");
            if (!FileUtils.ensureDirExists(cacheAdaptDirectory)) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Could not create directory at {0}.", cacheAdaptDirectory.getAbsolutePath());
                }
            }

            File cacheOverlayDirectory = new File(moduleCacheBaseDirectory, "cacheOverlay");
            if (!FileUtils.ensureDirExists(cacheOverlayDirectory)) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Could not create directory at {0}.", cacheOverlayDirectory.getAbsolutePath());
                }
            }

            File dummyWebApplicationDirectory = new File(moduleCacheBaseDirectory, "router.war");
            if (!FileUtils.ensureDirExists(dummyWebApplicationDirectory)) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Could not create directory at {0}.", dummyWebApplicationDirectory.getAbsolutePath());
                }
            }
            ArtifactContainer artifactContainer = artifactContainerFactoryRef.getServiceWithException().getContainer(cacheDirectory, dummyWebApplicationDirectory);
            if (artifactContainer == null) {
                throw new IllegalStateException("");
            }
            Container moduleContainer = adaptableModuleFactoryRef.getServiceWithException().getContainer(cacheAdaptDirectory, cacheOverlayDirectory, artifactContainer);

            JaxWsModuleMetaData jaxWsModuleMetaData = JaxWsMetaDataManager.getJaxWsModuleMetaData(moduleInfo.getMetaData());
            if (jaxWsModuleMetaData == null) {
                return null;
            }

            Container appContainer = moduleInfo.getApplicationInfo().getContainer();
            NonPersistentCache appCache = appContainer.adapt(NonPersistentCache.class);
            ApplicationClassesContainerInfo appCCI = (ApplicationClassesContainerInfo) appCache.getFromCache(ApplicationClassesContainerInfo.class);
            DeployedAppInfo deployedAppInfo = (DeployedAppInfo) appCCI;
            deployedAppInfo.getDeployedModule(moduleInfo);

            NonPersistentCache overlayCache = moduleContainer.adapt(NonPersistentCache.class);
            overlayCache.addToCache(JaxWsModuleMetaData.class, jaxWsModuleMetaData);
            overlayCache.addToCache(WebApp.class, new JaxWsRouterWebApp(jaxWsModuleMetaData.getJ2EEName().toString() + "-Router-WebModule"));
            overlayCache.addToCache(JaxWsModuleInfo.class, moduleInfo.getContainer().adapt(JaxWsModuleInfo.class));

            ApplicationInfo appInfo = moduleInfo.getApplicationInfo();
            String ejbModuleName = moduleInfo.getName();
            String webModuleName = ejbModuleName + "-Router";

            String defaultContextRoot;
            if (contextRoot == null || contextRoot.isEmpty()) {
                defaultContextRoot = "/" + ejbModuleName;
            } else {
                defaultContextRoot = contextRoot;
            }

            final ClassLoader moduleClassLoader = moduleInfo.getClassLoader();
            ModuleClassLoaderFactory classPathFactory = new ModuleClassLoaderFactory() {
                @Override
                public ClassLoader createModuleClassLoader(ModuleInfo moduleInfo, List<ContainerInfo> moduleClassesContainers) {
                    return moduleClassLoader;
                }
            };
            WebModuleInfoImpl webModuleInfo = new WebModuleInfoImpl(appInfo, webModuleName, webModuleName, defaultContextRoot, moduleContainer, null, null, classPathFactory);

            //Create ModuleMetaData for the router web application, also save the JaxWsModuleMetaData into that.
            ModuleMetaData webModuleMetaData = webModuleHandlerRef.getServiceWithException().createModuleMetaData(webModuleInfo, deployedAppInfo);
            JaxWsMetaDataManager.setJaxWsModuleMetaData(webModuleMetaData, jaxWsModuleMetaData);
            webModuleInfo.setMetaData(webModuleMetaData);

            moduleCacheBaseDirectoryMap.put(webModuleMetaData.getJ2EEName(), moduleCacheBaseDirectory);

            return webModuleInfo;
        } catch (MetaDataException e) {
            throw new IllegalStateException(e);
        }
    }
}
