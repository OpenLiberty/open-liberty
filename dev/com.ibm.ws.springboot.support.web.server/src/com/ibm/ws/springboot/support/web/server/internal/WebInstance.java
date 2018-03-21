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
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import com.ibm.ws.app.manager.module.DeployedModuleInfo;
import com.ibm.ws.app.manager.module.internal.DeployedAppInfoFactoryBase;
import com.ibm.ws.app.manager.module.internal.ModuleInfoUtils;
import com.ibm.ws.app.manager.module.internal.SimpleDeployedAppInfoBase;
import com.ibm.ws.app.manager.springboot.support.ContainerInstanceFactory.Instance;
import com.ibm.ws.app.manager.springboot.support.SpringBootApplication;
import com.ibm.ws.container.service.app.deploy.ContainerInfo;
import com.ibm.ws.container.service.app.deploy.WebModuleClassesInfo;
import com.ibm.ws.container.service.app.deploy.extended.ExtendedApplicationInfo;
import com.ibm.ws.container.service.app.deploy.extended.TagLibContainerInfo;
import com.ibm.ws.container.service.metadata.MetaDataException;
import com.ibm.ws.runtime.metadata.ModuleMetaData;
import com.ibm.ws.springboot.support.web.server.initializer.WebInitializer;
import com.ibm.ws.threading.listeners.CompletionListener;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.NonPersistentCache;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;

/**
 *
 */
public class WebInstance implements Instance {
    private static final String APP_NAME_KEY = "com.ibm.websphere.servlet.enterprise.application.name";

    final class InstanceDeployedAppInfo extends SimpleDeployedAppInfoBase implements ServletContainerInitializer {
        private final WebInitializer initializer;
        private final AtomicReference<ServiceRegistration<ServletContainerInitializer>> registration = new AtomicReference<>();
        private final AtomicReference<String> appName = new AtomicReference<>();

        InstanceDeployedAppInfo(WebInitializer initializer, DeployedAppInfoFactoryBase factory,
                                ExtendedApplicationInfo appInfo) throws UnableToAdaptException {
            super(factory);
            super.appInfo = appInfo;
            this.initializer = initializer;
        }

        DeployedModuleInfo createDeployedModule(Container appContainer, String id,
                                                SpringBootApplication app) throws UnableToAdaptException, MetaDataException {
            String moduleURI = ModuleInfoUtils.getModuleURIFromLocation(id);
            WebModuleContainerInfo mci = new WebModuleContainerInfo(//
                            instanceFactory.getModuleHandler(), //
                            instanceFactory.getDeployedAppFactory().getModuleMetaDataExtenders().get("web"), //
                            instanceFactory.getDeployedAppFactory().getNestedModuleMetaDataFactories().get("web"), //
                            appContainer, null, moduleURI, moduleClassesInfo, initializer.getContextPath());
            moduleContainerInfos.add(mci);

            ModuleMetaData mmd = mci.createModuleMetaData(appInfo, this, (m, c) -> app.getClassLoader());
            appName.set(mmd.getJ2EEName().getApplication());
            return getDeployedModule(mci.moduleInfo);
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
            registration.set(context.registerService(ServletContainerInitializer.class, this, null));
        }

        public void unregisterServletContainerListener() {
            registration.getAndUpdate((r) -> {
                if (r != null) {
                    r.unregister();
                }
                return null;
            });
        }
    }

    private final String m_id;
    private final WebInstanceFactory instanceFactory;
    private final SpringBootApplication app;
    private final AtomicReference<DeployedModuleInfo> deployed = new AtomicReference<>();

    public WebInstance(WebInstanceFactory instanceFactory, SpringBootApplication app, String id,
                       WebInitializer initializer) throws IOException, UnableToAdaptException, MetaDataException {
        this.m_id = id;
        this.instanceFactory = instanceFactory;
        this.app = app;
        installIntoWebContainer(id, initializer);
    }

    private void installIntoWebContainer(String id, WebInitializer initializer) throws IOException, UnableToAdaptException, MetaDataException {
        String contextPath = initializer.getContextPath();
        if (contextPath == null) {
            contextPath = "/";
        } else if (!contextPath.startsWith("/")) {
            contextPath = "/" + contextPath;
        }

        Container appContainer = app.createContainerFor(id);
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
        DeployedModuleInfo deployedModule = deployedApp.createDeployedModule(appContainer, id, app);
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
        // do nothing
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

    @Override
    public String getId() {
        return m_id;
    }

}
