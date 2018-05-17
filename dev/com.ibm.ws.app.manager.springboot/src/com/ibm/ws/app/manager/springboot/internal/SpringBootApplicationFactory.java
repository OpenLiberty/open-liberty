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
package com.ibm.ws.app.manager.springboot.internal;

import static com.ibm.ws.app.manager.springboot.internal.SpringConstants.SPRING_APP_TYPE;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.ibm.ws.app.manager.module.DeployedAppInfo;
import com.ibm.ws.app.manager.module.DeployedAppInfoFactory;
import com.ibm.ws.app.manager.module.internal.DeployedAppInfoFactoryBase;
import com.ibm.ws.app.manager.module.internal.ModuleHandler;
import com.ibm.ws.app.manager.springboot.support.ContainerInstanceFactory;
import com.ibm.ws.app.manager.springboot.support.SpringBootSupport;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.application.handler.ApplicationInformation;
import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.kernel.service.utils.FileUtils;

@Component(service = DeployedAppInfoFactory.class,
           property = { "service.vendor=IBM", "type=" + SPRING_APP_TYPE })
public class SpringBootApplicationFactory extends DeployedAppInfoFactoryBase {

    private final AtomicInteger nextAppID = new AtomicInteger(0);
    @Reference
    private LibIndexCache libIndexCache;
    @Reference(target = "(type=" + SPRING_APP_TYPE + ")")
    private ModuleHandler springModuleHandler;
    private final List<Container> springBootSupportContainers = new CopyOnWriteArrayList<Container>();
    private final Map<ServiceReference<SpringBootSupport>, List<Container>> springBootSupports = new ConcurrentHashMap<>();
    private final Map<Class<?>, ContainerInstanceFactory<?>> containerInstanceFactories = new ConcurrentHashMap<>();

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC, unbind = "removeSpringBootSupport")
    protected void addSpringBootSupport(SpringBootSupport support, ServiceReference<SpringBootSupport> ref) {
        List<Container> containers = getSpringBootSupportContainers(support, ref);
        this.springBootSupports.put(ref, containers);
        this.springBootSupportContainers.addAll(containers);
    }

    protected void removeSpringBootSupport(SpringBootSupport support, ServiceReference<SpringBootSupport> ref) {
        List<Container> containers = springBootSupports.remove(ref);
        if (containers != null) {
            springBootSupportContainers.removeAll(containers);
        }
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC, unbind = "removeContainerInstanceFactory")
    protected void addContainerInstanceFactory(@SuppressWarnings("rawtypes") ContainerInstanceFactory factory) {
        containerInstanceFactories.put(factory.getType(), factory);
    }

    protected void removeContainerInstanceFactory(@SuppressWarnings("rawtypes") ContainerInstanceFactory factory) {
        containerInstanceFactories.remove(factory.getType());
    }

    @Override
    public DeployedAppInfo createDeployedAppInfo(ApplicationInformation<DeployedAppInfo> applicationInformation) throws UnableToAdaptException {
        SpringBootApplicationImpl app = new SpringBootApplicationImpl(applicationInformation, this, nextAppID.getAndIncrement());
        applicationInformation.setHandlerInfo(app);
        return app;
    }

    private List<Container> getSpringBootSupportContainers(SpringBootSupport support, ServiceReference<SpringBootSupport> ref) {
        Bundle supportBundle = ref.getBundle();
        Container bundleContainer = getContainerForBundle(supportBundle);
        List<Container> supportContainers = new ArrayList<>();
        for (String path : support.getJarPaths()) {
            Entry entry = bundleContainer.getEntry(path);
            try {
                Container pathContainer = entry.adapt(Container.class);
                supportContainers.add(pathContainer);
            } catch (UnableToAdaptException e) {
                // auto generate FFDC
            }
        }
        return Collections.unmodifiableList(supportContainers);
    }

    private Container getContainerForBundle(Bundle bundle) {
        //for a bundle, we can use the bundles own private data storage as the cache..
        File cacheDir = ensureDataFileExists(bundle, "cache");
        File cacheDirAdapt = ensureDataFileExists(bundle, "cacheAdapt");
        File cacheDirOverlay = ensureDataFileExists(bundle, "cacheOverlay");
        // Create an artifact API and adaptable Container implementation for the bundle
        ArtifactContainer artifactContainer = getArtifactFactory().getContainer(cacheDir, bundle);
        Container wabContainer = getModuleFactory().getContainer(cacheDirAdapt, cacheDirOverlay, artifactContainer);
        return wabContainer;
    }

    List<Container> getSpringBootSupport() {
        return springBootSupportContainers;
    }

    File getDataDir(String path) throws IOException {
        return ensureDataFileExists(getBundleContext().getBundle(), path);
    }

    static File ensureDataFileExists(Bundle bundle, String path) {
        File dataFile = bundle.getDataFile(path);
        if (!FileUtils.ensureDirExists(dataFile)) {
            throw new RuntimeException("Failed to create data directory: " + dataFile.getAbsolutePath());
        }
        return dataFile;
    }

    LibIndexCache getLibIndexCache() {
        return libIndexCache;
    }

    ModuleHandler getModuleHandler() {
        return springModuleHandler;
    }

    @SuppressWarnings("unchecked")
    public <T> ContainerInstanceFactory<T> getContainerInstanceFactory(Class<T> type) {
        return (ContainerInstanceFactory<T>) containerInstanceFactories.get(type);
    }
}
