/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.springboot.support.web.server.internal;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;

import com.ibm.ws.app.manager.module.DeployedModuleInfo;
import com.ibm.ws.app.manager.module.internal.DeployedAppInfoFactoryBase;
import com.ibm.ws.app.manager.module.internal.ModuleInfoUtils;
import com.ibm.ws.app.manager.module.internal.SimpleDeployedAppInfoBase;
import com.ibm.ws.app.manager.springboot.container.config.SpringConfiguration;
import com.ibm.ws.app.manager.springboot.container.config.SpringErrorPageData;
import com.ibm.ws.app.manager.springboot.support.ContainerInstanceFactory.Instance;
import com.ibm.ws.app.manager.springboot.support.SpringBootApplication;
import com.ibm.ws.container.ErrorPage;
import com.ibm.ws.container.service.app.deploy.ContainerInfo;
import com.ibm.ws.container.service.app.deploy.WebModuleClassesInfo;
import com.ibm.ws.container.service.app.deploy.extended.ExtendedApplicationInfo;
import com.ibm.ws.container.service.app.deploy.extended.TagLibContainerInfo;
import com.ibm.ws.container.service.metadata.MetaDataException;
import com.ibm.ws.springboot.support.web.server.initializer.WebInitializer;
import com.ibm.ws.threading.listeners.CompletionListener;
import com.ibm.ws.webcontainer.osgi.webapp.WebAppConfiguration;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.NonPersistentCache;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.http.VirtualHost;
import com.ibm.wsspi.webcontainer.metadata.WebModuleMetaData;

/**
 *
 */
public class WebInstance implements Instance {
    private static final String APP_NAME_KEY = "com.ibm.websphere.servlet.enterprise.application.name";

    final class InstanceDeployedAppInfo extends SimpleDeployedAppInfoBase implements ServletContainerInitializer {
        private final WebInitializer initializer;
        private final AtomicReference<ServiceRegistration<ServletContainerInitializer>> sciRegistration = new AtomicReference<>();
        private final AtomicReference<String> appName = new AtomicReference<>();

        InstanceDeployedAppInfo(WebInitializer initializer, DeployedAppInfoFactoryBase factory,
                                ExtendedApplicationInfo appInfo) throws UnableToAdaptException {
            super(factory);
            super.appInfo = appInfo;
            this.initializer = initializer;
        }

        DeployedModuleInfo createDeployedModule(Container appContainer, String id,
                                                SpringBootApplication app, SpringConfiguration additionalConfig) throws UnableToAdaptException, MetaDataException {
            String moduleURI = ModuleInfoUtils.getModuleURIFromLocation(id);
            WebModuleContainerInfo mci = new WebModuleContainerInfo(//
                            instanceFactory.getModuleHandler(), //
                            instanceFactory.getDeployedAppFactory().getModuleMetaDataExtenders().get("web"), //
                            instanceFactory.getDeployedAppFactory().getNestedModuleMetaDataFactories().get("web"), //
                            appContainer, null, moduleURI, moduleClassesInfo, initializer.getContextPath());
            moduleContainerInfos.add(mci);
            WebModuleMetaData mmd = (WebModuleMetaData) mci.createModuleMetaData(appInfo, this, (m, c) -> app.getClassLoader());
            addSpringConfigToModuleMetadata(mmd, additionalConfig);
            appName.set(mmd.getJ2EEName().getApplication());
            return getDeployedModule(mci.moduleInfo);
        }

        private void addSpringConfigToModuleMetadata(WebModuleMetaData mmd, SpringConfiguration additionalConfig) {
            WebAppConfiguration wac = ((WebAppConfiguration) mmd.getConfiguration());
            @SuppressWarnings("unchecked")
            HashMap<Integer, com.ibm.ws.container.ErrorPage> code_errorPage_hm = wac.getCodeErrorPages();
            //configure spring mime-mappings
            wac.setMimeMappings(additionalConfig.getMimeMappings());
            @SuppressWarnings("unchecked")
            HashMap<String, com.ibm.ws.container.ErrorPage> exception_errorPage_hm = wac.getExceptionErrorPages();
            for (SpringErrorPageData spr_ep : additionalConfig.getErrorPages()) {
                if (spr_ep.isGlobal()) {
                    wac.setDefaultErrorPage(spr_ep.getLocation());
                } else {
                    ErrorPage container_ep = new ErrorPage(spr_ep.getLocation());

                    if (spr_ep.getErrorCode() != null) {
                        container_ep.setErrorParam(spr_ep.getErrorCode().toString());
                        code_errorPage_hm.put(spr_ep.getErrorCode().intValue(), container_ep);
                    } else if (spr_ep.getExceptionType() != null) {
                        container_ep.setErrorParam(spr_ep.getExceptionType());
                        exception_errorPage_hm.put(spr_ep.getExceptionType().toString(), container_ep);
                    }
                }
            }
        }

        @Override
        public void onStartup(Set<Class<?>> classes, ServletContext context) throws ServletException {
            String expectedName = appName.get();
            if (expectedName == null) {
                expectedName = appInfo.getName();
            }
            if (expectedName.equals(context.getAttribute(APP_NAME_KEY))) {
                initializer.getContextInitializer().apply(context);
                unregisterServletContainerListener();
            }
        }

        public void registerServletContainerListener(BundleContext context) {
            sciRegistration.set(context.registerService(ServletContainerInitializer.class, this, null));
        }

        public void unregisterServletContainerListener() {
            sciRegistration.getAndUpdate((r) -> {
                if (r != null) {
                    r.unregister();
                }
                return null;
            });
        }
    }

    private final WebInstanceFactory instanceFactory;
    private final SpringBootApplication app;
    private final AtomicReference<DeployedModuleInfo> deployed = new AtomicReference<>();
    private final ServiceTracker<VirtualHost, VirtualHost> tracker;

    public WebInstance(WebInstanceFactory instanceFactory, SpringBootApplication app, String id,
                       String virtualHostId, WebInitializer initializer,
                       ServiceTracker<VirtualHost, VirtualHost> tracker, SpringConfiguration additionalConfig) throws IOException, UnableToAdaptException, MetaDataException {
        this.instanceFactory = instanceFactory;
        this.app = app;
        this.tracker = tracker;
        installIntoWebContainer(id, virtualHostId, initializer, additionalConfig);
    }

    private void installIntoWebContainer(String id, String virtualHostId, WebInitializer initializer,
                                         SpringConfiguration cfg) throws IOException, UnableToAdaptException, MetaDataException {
        String contextPath = initializer.getContextPath();
        if (contextPath == null) {
            contextPath = "/";
        } else if (!contextPath.startsWith("/")) {
            contextPath = "/" + contextPath;
        }

        Container appContainer = app.createContainerFor(virtualHostId);
        // create a classes info that reflects the spring app so that we can
        // put that into the overlay cache to supersede the JEE classes info
        WebModuleClassesInfo classesInfo = new WebModuleClassesInfo() {

            @Override
            public List<ContainerInfo> getClassesContainers() {
                // TODO unclear if we should return any class containers here at all
                // The containers that get returned are scanned for annotations
                return Collections.emptyList(); // returning empty to avoid annotation scanning
                //return app.getSpringClassesContainerInfo().getClassesContainerInfo();
            }
        };
        // classesInfo is known to be non-null - getClassesInfo always constructs a new one
        NonPersistentCache npc = appContainer.adapt(NonPersistentCache.class);
        npc.addToCache(WebModuleClassesInfo.class, classesInfo);

        TagLibContainerInfo tagLibInfo = new TagLibContainerInfo() {

            @Override
            public List<ContainerInfo> getTagLibContainers() {
                return app.getSpringClassesContainerInfo().getClassesContainerInfo();
            }
        };
        npc.addToCache(TagLibContainerInfo.class, tagLibInfo);

        ExtendedApplicationInfo appInfo = app.createApplicationInfo(id, appContainer);
        InstanceDeployedAppInfo deployedApp = new InstanceDeployedAppInfo(initializer, instanceFactory.getDeployedAppFactory(), appInfo);
        DeployedModuleInfo deployedModule = deployedApp.createDeployedModule(appContainer, id, app, cfg);
        deployed.set(deployedModule);

        deployedApp.registerServletContainerListener(instanceFactory.getContext());
        Future<Boolean> appFuture = instanceFactory.getModuleHandler().deployModule(deployedModule, deployedApp);
        instanceFactory.getFutureMonitor().onCompletion(appFuture, new CompletionListener<Boolean>() {
            @Override
            public void successfulCompletion(Future<Boolean> future, Boolean result) {
                // if successful we would have unregistered the service in the InstanceDeployedAppInfo
            }

            @Override
            public void failedCompletion(Future<Boolean> future, Throwable t) {
                deployedApp.unregisterServletContainerListener();
            }

        });
    }

    @Override
    public void start() {
        tracker.open();
        try {
            VirtualHost v = tracker.waitForService(30000);
            if (v == null) {
                throw new IllegalStateException("Virtual host not configured.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } finally {
            tracker.close();
        }
    }

    @Override
    public void stop() {
        DeployedModuleInfo deployedModule = deployed.getAndSet(null);
        if (deployedModule != null) {
            instanceFactory.getModuleHandler().undeployModule(deployedModule);
            deployedModule.getModuleInfo().getApplicationInfo();
            app.destroyApplicationInfo((ExtendedApplicationInfo) deployedModule.getModuleInfo().getApplicationInfo());
        }
    }

}
