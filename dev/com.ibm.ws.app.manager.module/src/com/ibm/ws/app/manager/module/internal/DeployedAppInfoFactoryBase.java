/*******************************************************************************
 * Copyright (c) 2012, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.app.manager.module.internal;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.ws.app.manager.module.DeployedAppInfoFactory;
import com.ibm.ws.classloading.java2sec.PermissionManager;
import com.ibm.ws.container.service.app.deploy.extended.ApplicationInfoFactory;
import com.ibm.ws.container.service.metadata.MetaDataService;
import com.ibm.ws.container.service.metadata.extended.ModuleMetaDataExtender;
import com.ibm.ws.container.service.metadata.extended.NestedModuleMetaDataFactory;
import com.ibm.ws.container.service.state.StateChangeService;
import com.ibm.ws.threading.FutureMonitor;
import com.ibm.wsspi.adaptable.module.AdaptableModuleFactory;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.factory.ArtifactContainerFactory;
import com.ibm.wsspi.classloading.ClassLoadingService;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.kernel.service.location.WsLocationConstants;
import com.ibm.wsspi.kernel.service.utils.FileUtils;
import com.ibm.wsspi.library.Library;

public abstract class DeployedAppInfoFactoryBase implements DeployedAppInfoFactory {

    static final String SERVER_APPS_DIR = WsLocationConstants.SYMBOL_SERVER_CONFIG_DIR + "apps/";
    protected static final String EXPANDED_APPS_DIR = SERVER_APPS_DIR + "expanded/";
    protected static final String XML_SUFFIX = ".xml";

    private BundleContext bundleContext;
    private ApplicationInfoFactory applicationInfoFactory;
    private FutureMonitor futureMonitor;
    private ClassLoadingService classLoadingService;
    private Library globalSharedLibrary;
    private String globalSharedLibraryPid;
    private ConfigurationAdmin configAdmin;
    private MetaDataService metaDataService;
    private StateChangeService stateChangeService;
    private final String[] allModuleTypes = { "ejb", "web", "client", "connector" };
    private final Map<String, List<ModuleMetaDataExtender>> moduleMetaDataExtenders = new ConcurrentHashMap<String, List<ModuleMetaDataExtender>>();
    private final Map<String, List<NestedModuleMetaDataFactory>> nestedModuleMetaDataFactories = new ConcurrentHashMap<String, List<NestedModuleMetaDataFactory>>();
    private PermissionManager permissionManager;
    private WsLocationAdmin locAdmin;
    private ArtifactContainerFactory artifactFactory;
    private AdaptableModuleFactory moduleFactory;

    public DeployedAppInfoFactoryBase() {
        for (String moduleType : allModuleTypes) {
            moduleMetaDataExtenders.put(moduleType, new CopyOnWriteArrayList<ModuleMetaDataExtender>());
            nestedModuleMetaDataFactories.put(moduleType, new CopyOnWriteArrayList<NestedModuleMetaDataFactory>());
        }
    }

    @Activate
    protected void activate(ComponentContext ctx) {
        this.bundleContext = ctx.getBundleContext();
    }

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public ApplicationInfoFactory getApplicationInfoFactory() {
        return applicationInfoFactory;
    }

    public FutureMonitor getFutureMonitor() {
        return futureMonitor;
    }

    public ClassLoadingService getClassLoadingService() {
        return classLoadingService;
    }

    public Library getGlobalSharedLibrary() {
        return globalSharedLibrary;
    }

    public String getGlobalSharedLibraryPid() {
        return globalSharedLibraryPid;
    }

    public ConfigurationAdmin getConfigurationAdmin() {
        return configAdmin;
    }

    public MetaDataService getMetaDataService() {
        return metaDataService;
    }

    public StateChangeService getStateChangeService() {
        return stateChangeService;
    }

    public Map<String, List<ModuleMetaDataExtender>> getModuleMetaDataExtenders() {
        return moduleMetaDataExtenders;
    }

    public Map<String, List<NestedModuleMetaDataFactory>> getNestedModuleMetaDataFactories() {
        return nestedModuleMetaDataFactories;
    }

    public PermissionManager getPermissionManager() {
        return permissionManager;
    }

    public WsLocationAdmin getLocationAdmin() {
        return locAdmin;
    }

    public ArtifactContainerFactory getArtifactFactory() {
        return artifactFactory;
    }

    public AdaptableModuleFactory getModuleFactory() {
        return moduleFactory;
    }

    protected Container setupContainer(String pid, File locationFile) {
        if (!FileUtils.fileExists(locationFile)) {
            return null;
        }

        File cacheDir = new File(getCacheDir(), pid);
        if (!FileUtils.ensureDirExists(cacheDir)) {
            return null;
        }

        ArtifactContainer artifactContainer = getArtifactFactory().getContainer(cacheDir, locationFile);
        if (artifactContainer == null) {
            return null;
        }

        File cacheDirAdapt = new File(getCacheAdaptDir(), pid);
        if (!FileUtils.ensureDirExists(cacheDirAdapt)) {
            return null;
        }

        File cacheDirOverlay = new File(getCacheOverlayDir(), pid);
        if (!FileUtils.ensureDirExists(cacheDirOverlay)) {
            return null;
        }

        return getModuleFactory().getContainer(cacheDirAdapt, cacheDirOverlay, artifactContainer);
    }

    private File getCacheAdaptDir() {
        return getLocationAdmin().getBundleFile(this, "cacheAdapt");
    }

    private File getCacheOverlayDir() {
        return getLocationAdmin().getBundleFile(this, "cacheOverlay");
    }

    private File getCacheDir() {
        return getLocationAdmin().getBundleFile(this, "cache");
    }

    @Reference
    protected void setApplicationInfoFactory(ApplicationInfoFactory applicationInfoFactory) {
        this.applicationInfoFactory = applicationInfoFactory;
    }

    protected void unsetApplicationInfoFactory(ApplicationInfoFactory applicationInfoFactory) {}

    @Reference
    protected void setFutureMonitor(FutureMonitor futureMonitor) {
        this.futureMonitor = futureMonitor;
    }

    protected void unsetFutureMonitor(FutureMonitor futureMonitor) {}

    @Reference
    protected void setClassLoadingService(ClassLoadingService service) {
        classLoadingService = service;
    }

    protected void unsetClassLoadingService(ClassLoadingService service) {
        classLoadingService = null;
    }

    @Reference(target = "(id=global)")
    protected void setGlobalSharedLibrary(Library library, Map<String, ?> serviceProps) {
        globalSharedLibrary = library;
        globalSharedLibraryPid = (String) serviceProps.get("service.pid");
    }

    protected void unsetGlobalSharedLibrary(Library library) {
        globalSharedLibrary = null;
    }

    @Reference
    protected void setConfigAdmin(ConfigurationAdmin admin) {
        configAdmin = admin;
    }

    protected void unsetConfigAdmin(ConfigurationAdmin admin) {
        configAdmin = null;
    }

    @Reference
    protected void setMetaDataService(MetaDataService metaDataService) {
        this.metaDataService = metaDataService;
    }

    protected void unsetMetaDataService(MetaDataService metaDataService) {}

    @Reference
    protected void setStateChangeService(StateChangeService stateChangeService) {
        this.stateChangeService = stateChangeService;
    }

    protected void unsetStateChangeService(StateChangeService stateChangeService) {}

    @Reference(cardinality = ReferenceCardinality.MULTIPLE,
               policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
    protected void setModuleMetaDataExtender(ModuleMetaDataExtender moduleMetaDataExtender, Map<String, ?> serviceProps) {
        // ignore services that don't have the property we require.
        Object o = serviceProps.get("type");
        if (o != null) {
            if (o instanceof String) {
                String moduleType = (String) o;
                _setModuleMetaDataExtender(moduleMetaDataExtender, moduleType);
            } else if (o instanceof String[]) {
                String[] moduleTypes = (String[]) o;
                _setModuleMetaDataExtender(moduleMetaDataExtender, moduleTypes);
            }
        } else {
            _setModuleMetaDataExtender(moduleMetaDataExtender, allModuleTypes);
        }
    }

    private void _setModuleMetaDataExtender(ModuleMetaDataExtender moduleMetaDataExtender, String... types) {
        for (String moduleType : types) {
            List<ModuleMetaDataExtender> extenders = this.moduleMetaDataExtenders.get(moduleType);
            if (extenders != null) {
                extenders.add(moduleMetaDataExtender);
            }
        }
    }

    protected void unsetModuleMetaDataExtender(ModuleMetaDataExtender moduleMetaDataExtender, Map<String, ?> serviceProps) {
        // ignore services that don't have the property we require.
        Object o = serviceProps.get("type");
        if (o != null) {
            if (o instanceof String) {
                String moduleType = (String) o;
                _unsetModuleMetaDataExtender(moduleMetaDataExtender, moduleType);
            } else if (o instanceof String[]) {
                String[] moduleTypes = (String[]) o;
                _unsetModuleMetaDataExtender(moduleMetaDataExtender, moduleTypes);
            }
        } else {
            _unsetModuleMetaDataExtender(moduleMetaDataExtender, allModuleTypes);
        }
    }

    private void _unsetModuleMetaDataExtender(ModuleMetaDataExtender moduleMetaDataExtender, String... types) {
        for (String moduleType : types) {
            List<ModuleMetaDataExtender> extenders = this.moduleMetaDataExtenders.get(moduleType);
            if (extenders != null) {
                extenders.remove(moduleMetaDataExtender);
            }
        }
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE,
               policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
    protected void setNestedModuleMetaDataFactory(NestedModuleMetaDataFactory nestedModuleMetaDataFactory, Map<String, ?> serviceProps) {
        // ignore services that don't have the property we require.
        Object o = serviceProps.get("type");
        if (o != null) {
            if (o instanceof String) {
                String moduleType = (String) o;
                _setNestedModuleMetaDataFactory(nestedModuleMetaDataFactory, moduleType);
            } else if (o instanceof String[]) {
                String[] moduleTypes = (String[]) o;
                _setNestedModuleMetaDataFactory(nestedModuleMetaDataFactory, moduleTypes);
            }
        } else {
            _setNestedModuleMetaDataFactory(nestedModuleMetaDataFactory, allModuleTypes);
        }
    }

    private void _setNestedModuleMetaDataFactory(NestedModuleMetaDataFactory nestedModuleMetaDataFactory, String... types) {
        for (String moduleType : types) {
            List<NestedModuleMetaDataFactory> factories = this.nestedModuleMetaDataFactories.get(moduleType);
            if (factories != null) {
                factories.add(nestedModuleMetaDataFactory);
            }
        }
    }

    protected void unsetNestedModuleMetaDataFactory(NestedModuleMetaDataFactory nestedModuleMetaDataFactory, Map<String, ?> serviceProps) {
        // ignore services that don't have the property we require.
        Object o = serviceProps.get("type");
        if (o != null) {
            if (o instanceof String) {
                String moduleType = (String) o;
                _unsetNestedModuleMetaDataFactory(nestedModuleMetaDataFactory, moduleType);
            } else if (o instanceof String[]) {
                String[] moduleTypes = (String[]) o;
                _unsetNestedModuleMetaDataFactory(nestedModuleMetaDataFactory, moduleTypes);
            }
        } else {
            _unsetNestedModuleMetaDataFactory(nestedModuleMetaDataFactory, allModuleTypes);
        }
    }

    private void _unsetNestedModuleMetaDataFactory(NestedModuleMetaDataFactory nestedModuleMetaDataFactory, String... types) {
        for (String moduleType : types) {
            List<NestedModuleMetaDataFactory> factories = this.nestedModuleMetaDataFactories.get(moduleType);
            if (factories != null) {
                factories.remove(nestedModuleMetaDataFactory);
            }
        }
    }

    @Reference(service = PermissionManager.class,
               cardinality = ReferenceCardinality.OPTIONAL,
               policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
    protected void setPermissionManager(PermissionManager permissionManager) {
        this.permissionManager = permissionManager;
    }

    protected void unsetPermissionManager(PermissionManager permissionManager) {
        this.permissionManager = null;
    }

    @Reference
    protected void setLocationAdmin(WsLocationAdmin locAdmin) {
        this.locAdmin = locAdmin;
    }

    protected void unsetLocationAdmin(WsLocationAdmin locAdmin) {
        this.locAdmin = null;
    }

    @Reference
    protected void setArtifactFactory(ArtifactContainerFactory factory) {
        this.artifactFactory = factory;
    }

    protected void unsetArtifactFactory(ArtifactContainerFactory factory) {
        this.artifactFactory = null;
    }

    @Reference
    protected void setModuleFactory(AdaptableModuleFactory factory) {
        this.moduleFactory = factory;
    }

    protected void unsetModuleFactory(AdaptableModuleFactory factory) {
        this.moduleFactory = null;
    }
}
