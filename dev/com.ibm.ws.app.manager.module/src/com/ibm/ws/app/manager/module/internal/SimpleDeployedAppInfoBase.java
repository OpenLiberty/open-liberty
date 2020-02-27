/*******************************************************************************
 * Copyright (c) 2012, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.app.manager.module.internal;

import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;

import com.ibm.ws.app.manager.module.DeployedAppInfo;
import com.ibm.ws.app.manager.module.DeployedAppServices;
import com.ibm.ws.app.manager.module.DeployedModuleInfo;
import com.ibm.ws.container.service.app.deploy.ApplicationInfo;
import com.ibm.ws.container.service.app.deploy.ContainerInfo;
import com.ibm.ws.container.service.app.deploy.ManifestClassPathUtils;
import com.ibm.ws.container.service.app.deploy.ModuleClassesContainerInfo;
import com.ibm.ws.container.service.app.deploy.NestedConfigHelper;
import com.ibm.ws.container.service.app.deploy.extended.AltDDEntryGetter;
import com.ibm.ws.container.service.app.deploy.extended.ApplicationInfoFactory;
import com.ibm.ws.container.service.app.deploy.extended.ExtendedApplicationInfo;
import com.ibm.ws.container.service.app.deploy.extended.ExtendedModuleInfo;
import com.ibm.ws.container.service.app.deploy.extended.ModuleContainerInfo;
import com.ibm.ws.container.service.metadata.MetaDataException;
import com.ibm.ws.container.service.metadata.MetaDataService;
import com.ibm.ws.container.service.metadata.extended.ModuleMetaDataExtender;
import com.ibm.ws.container.service.metadata.extended.NestedModuleMetaDataFactory;
import com.ibm.ws.container.service.state.StateChangeService;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.javaee.dd.client.ApplicationClient;
import com.ibm.ws.javaee.dd.common.ModuleDeploymentDescriptor;
import com.ibm.ws.javaee.dd.ejb.EJBJar;
import com.ibm.ws.javaee.dd.web.WebApp;
import com.ibm.ws.runtime.metadata.ModuleMetaData;
import com.ibm.ws.threadContext.ModuleMetaDataAccessorImpl;
import com.ibm.ws.threading.FutureMonitor;
import com.ibm.ws.threading.listeners.CompletionListener;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;
import com.ibm.wsspi.adaptable.module.NonPersistentCache;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;

public abstract class SimpleDeployedAppInfoBase implements DeployedAppInfo {

    protected static final class ModuleClassesInfoProvider {
        private final Map<String, List<ContainerInfo>> entryContainerInfosMap = new HashMap<String, List<ContainerInfo>>();

        public List<ContainerInfo> getClassesContainers(Container moduleContainer) throws UnableToAdaptException {
            Entry moduleEntry = moduleContainer.adapt(Entry.class);
            if (moduleEntry == null) {
                return Collections.emptyList();
            }
            String entryIdentity = ManifestClassPathUtils.createEntryIdentity(moduleEntry);
            List<ContainerInfo> containerInfos = entryContainerInfosMap.get(entryIdentity);
            if (containerInfos == null) {
                Set<String> resolved = new HashSet<String>();
                containerInfos = new ArrayList<ContainerInfo>();
                ManifestClassPathUtils.processMFClasspath(moduleEntry, containerInfos, resolved);
                entryContainerInfosMap.put(entryIdentity, containerInfos);
            }
            return containerInfos;
        }
    }

    protected static class ExtendedContainerInfo implements ContainerInfo {
        protected final ContainerInfo.Type type;
        protected final String name;
        protected final Container container;
        protected final ModuleClassLoaderFactory moduleClassLoaderFactory;
        protected final Entry altDDEntry;
        protected final List<ContainerInfo> classesContainerInfo = new ArrayList<ContainerInfo>();

        protected ExtendedContainerInfo(ContainerInfo.Type type, String name, Container container,
                                        ModuleClassLoaderFactory moduleClassLoaderFactory, Entry altDDEntry) {
            this.type = type;
            this.name = name;
            this.container = container;
            this.moduleClassLoaderFactory = moduleClassLoaderFactory;
            this.altDDEntry = altDDEntry;
            if (altDDEntry != null) {
                try {
                    NonPersistentCache cache = container.adapt(NonPersistentCache.class);
                    cache.addToCache(AltDDEntryGetter.class, new AltDDEntryGetter() {
                        @Override
                        public Entry getAltDDEntry(ContainerInfo.Type type) {
                            if (ExtendedContainerInfo.this.type == type) {
                                return ExtendedContainerInfo.this.altDDEntry;
                            } else {
                                return null;
                            }
                        }
                    });
                } catch (UnableToAdaptException e) {
                    // Auto FFDC
                }
            }
        }

        @Override
        public ContainerInfo.Type getType() {
            return this.type;
        }

        @Override
        public String getName() {
            return this.name;
        }

        @Override
        public Container getContainer() {
            return this.container;
        }

        public List<ContainerInfo> getClassesContainerInfo() {
            return this.classesContainerInfo;
        }
    }

    protected static abstract class ModuleContainerInfoBase extends ExtendedContainerInfo implements ModuleClassesContainerInfo, ModuleContainerInfo {
        public final ModuleHandler moduleHandler;
        public final List<ModuleMetaDataExtender> moduleMetaDataExtenders;
        public final List<NestedModuleMetaDataFactory> nestedModuleMetaDataFactories;
        public List<DeployedModuleInfoImpl> nestedModules;

        /**
         * The explicitly specified module name from the module deployment
         * descriptor, or the implicit module name determined from the module
         * URI. This might not be the final module if it conflicts with another
         * module in the same application (or another application name for
         * standalone modules).
         */
        public String moduleName;

        public final ModuleDeploymentDescriptor moduleDD;

        public ExtendedModuleInfo moduleInfo;

        public abstract ExtendedModuleInfoImpl createModuleInfoImpl(ApplicationInfo appInfo, ModuleClassLoaderFactory classLoaderFactory) throws MetaDataException;

        public ModuleContainerInfoBase(ModuleHandler moduleHandler, List<ModuleMetaDataExtender> moduleMetaDataExtenders,
                                       List<NestedModuleMetaDataFactory> nestedModuleMetaDataFactories,
                                       Container moduleContainer, Entry altDDEntry, String moduleURI,
                                       ContainerInfo.Type moduleContainerType,
                                       ModuleClassLoaderFactory moduleClassLoaderFactory,
                                       ModuleClassesInfoProvider moduleClassesInfo,
                                       Class<? extends ModuleDeploymentDescriptor> moduleDDClass) throws UnableToAdaptException {
            super(moduleContainerType, moduleURI, moduleContainer, moduleClassLoaderFactory, altDDEntry);
            this.moduleHandler = moduleHandler;
            this.moduleMetaDataExtenders = moduleMetaDataExtenders;
            this.nestedModuleMetaDataFactories = nestedModuleMetaDataFactories;
            this.moduleDD = moduleContainer.adapt(moduleDDClass);
            this.moduleName = ModuleInfoUtils.getModuleName(moduleDD, moduleURI);
            this.classesContainerInfo.add(this);
            if (moduleClassesInfo != null) {
                this.classesContainerInfo.addAll(moduleClassesInfo.getClassesContainers(moduleContainer));
            }
        }

        public ModuleMetaData createModuleMetaData(ApplicationInfo appInfo, SimpleDeployedAppInfoBase deployedApp) throws MetaDataException {
            ModuleClassLoaderFactory classLoaderFactory = moduleClassLoaderFactory;
            if (classLoaderFactory == null) {
                classLoaderFactory = (ModuleClassLoaderFactory) deployedApp;
            }
            ExtendedModuleInfoImpl moduleInfoImpl = createModuleInfoImpl(appInfo, classLoaderFactory);
            moduleInfo = moduleInfoImpl;
            if (moduleInfo == null || moduleHandler == null) {
                return null;
            }
            ModuleMetaData mmd = moduleHandler.createModuleMetaData(moduleInfo, deployedApp);
            if (mmd != null) {
                moduleInfoImpl.setMetaData(mmd);

                for (NestedModuleMetaDataFactory nestedModuleMetaDataFactory : nestedModuleMetaDataFactories) {
                    nestedModuleMetaDataFactory.createdNestedModuleMetaData(moduleInfo);
                }
                List<ExtendedModuleInfo> nestedModules = null;
                for (ModuleMetaDataExtender moduleMetaDataExtender : moduleMetaDataExtenders) {
                    ExtendedModuleInfo nestedModule = moduleMetaDataExtender.extendModuleMetaData(moduleInfo);
                    if (nestedModule != null) {
                        if (nestedModules == null) {
                            nestedModules = new ArrayList<ExtendedModuleInfo>(2);
                        }
                        nestedModules.add(nestedModule);
                    }
                }
                DeployedModuleInfoImpl deployedMod = new DeployedModuleInfoImpl(moduleHandler, moduleInfo);
                deployedApp.addDeployedModule(deployedMod, nestedModules);
                this.nestedModules = deployedMod.getNestedModules();
            }
            return mmd;
        }

        public String getModuleURI() {
            return getName();
        }

        public void setModuleName(String newModuleName) {
            this.moduleName = newModuleName;
        }

        @Override
        public ClassLoader getClassLoader() {
            return moduleInfo.getClassLoader();
        }
    }

    protected static final class ConnectorModuleContainerInfo extends ModuleContainerInfoBase {

        public ConnectorModuleContainerInfo(ModuleHandler moduleHandler, List<ModuleMetaDataExtender> moduleMetaDataExtenders,
                                            List<NestedModuleMetaDataFactory> nestedModuleMetaDataFactories,
                                            Container moduleContainer, Entry altDDEntry,
                                            String moduleURI,
                                            ModuleClassLoaderFactory moduleClassLoaderFactory,
                                            ModuleClassesInfoProvider moduleClassesInfo) throws UnableToAdaptException {
            super(moduleHandler, moduleMetaDataExtenders, nestedModuleMetaDataFactories, moduleContainer, altDDEntry, moduleURI, ContainerInfo.Type.RAR_MODULE, moduleClassLoaderFactory, moduleClassesInfo, com.ibm.ws.javaee.dd.connector.Connector.class);
            getConnectorModuleClassesInfo(moduleContainer);
        }

        @Override
        public ExtendedModuleInfoImpl createModuleInfoImpl(ApplicationInfo appInfo, ModuleClassLoaderFactory classLoaderFactory) throws MetaDataException {
            try {
                return new ConnectorModuleInfoImpl(appInfo, moduleName, name, container, altDDEntry, classesContainerInfo, classLoaderFactory);
            } catch (UnableToAdaptException e) {
                FFDCFilter.processException(e, getClass().getName(), "createModuleInfo", this);
                return null;
            }
        }

        private void getConnectorModuleClassesInfo(Container moduleContainer) throws UnableToAdaptException {
            for (Entry entry : moduleContainer) {
                getEntryClassesInfo(entry);
            }
        }

        private void getEntryClassesInfo(Entry entry) throws UnableToAdaptException {
            if (entry.getName().toLowerCase().endsWith(".jar")) {
                final String jarEntryName = entry.getName();
                final Container jarContainer = entry.adapt(Container.class);
                ContainerInfo containerInfo = new ContainerInfo() {
                    @Override
                    public Type getType() {
                        return Type.JAR_MODULE;
                    }

                    @Override
                    public String getName() {
                        return jarEntryName;
                    }

                    @Override
                    public Container getContainer() {
                        return jarContainer;
                    }
                };
                this.classesContainerInfo.add(containerInfo);
                Set<String> resolved = new HashSet<String>();
                ManifestClassPathUtils.addCompleteJarEntryUrls(this.classesContainerInfo, entry, resolved);
                for (Entry childEntry : jarContainer) {
                    getEntryClassesInfo(childEntry);
                }
            }
        }
    }

    protected static final class EJBModuleContainerInfo extends ModuleContainerInfoBase {

        public EJBModuleContainerInfo(ModuleHandler moduleHandler, List<ModuleMetaDataExtender> moduleMetaDataExtenders,
                                      List<NestedModuleMetaDataFactory> nestedModuleMetaDataFactories,
                                      Container moduleContainer, Entry altDDEntry, String moduleURI,
                                      ModuleClassLoaderFactory moduleClassLoaderFactory,
                                      ModuleClassesInfoProvider moduleClassesInfo) throws UnableToAdaptException {
            super(moduleHandler, moduleMetaDataExtenders, nestedModuleMetaDataFactories, moduleContainer, altDDEntry, moduleURI, ContainerInfo.Type.EJB_MODULE, moduleClassLoaderFactory, moduleClassesInfo, EJBJar.class);
        }

        @Override
        public ExtendedModuleInfoImpl createModuleInfoImpl(ApplicationInfo appInfo, ModuleClassLoaderFactory classLoaderFactory) throws MetaDataException {
            try {
                return new EJBModuleInfoImpl(appInfo, moduleName, name, container, altDDEntry, classesContainerInfo, classLoaderFactory);
            } catch (UnableToAdaptException e) {
                FFDCFilter.processException(e, getClass().getName(), "createModuleInfo", this);
                return null;
            }
        }
    }

    protected static final class ClientModuleContainerInfo extends ModuleContainerInfoBase {
        final String mainClassName;

        public ClientModuleContainerInfo(ModuleHandler moduleHandler, List<ModuleMetaDataExtender> moduleMetaDataExtenders,
                                         List<NestedModuleMetaDataFactory> nestedModuleMetaDataFactories,
                                         Container moduleContainer, Entry altDDEntry, String moduleURI,
                                         ModuleClassLoaderFactory moduleClassLoaderFactory,
                                         ModuleClassesInfoProvider moduleClassesInfo,
                                         String mainClass) throws UnableToAdaptException {
            super(moduleHandler, moduleMetaDataExtenders, nestedModuleMetaDataFactories, moduleContainer, altDDEntry, moduleURI, ContainerInfo.Type.CLIENT_MODULE, moduleClassLoaderFactory, moduleClassesInfo, ApplicationClient.class);
            mainClassName = mainClass;
        }

        @Override
        public ExtendedModuleInfoImpl createModuleInfoImpl(ApplicationInfo appInfo, ModuleClassLoaderFactory classLoaderFactory) throws MetaDataException {
            try {
                return new ClientModuleInfoImpl(appInfo, moduleName, name, container, altDDEntry, classesContainerInfo, mainClassName, classLoaderFactory);
            } catch (UnableToAdaptException e) {
                FFDCFilter.processException(e, getClass().getName(), "createModuleInfo", this);
                return null;
            }
        }
    }

    protected static final class WebModuleContainerInfo extends ModuleContainerInfoBase {
        /**
         * The explicitly specified context root from application.xml, web
         * extension, or server configuration.
         */
        public String contextRoot;
        public String defaultContextRoot;

        public WebModuleContainerInfo(ModuleHandler moduleHandler, List<ModuleMetaDataExtender> moduleMetaDataExtenders,
                                      List<NestedModuleMetaDataFactory> nestedModuleMetaDataFactories,
                                      Container moduleContainer, Entry altDDEntry, String moduleURI,
                                      ModuleClassesInfoProvider moduleClassesInfo,
                                      String contextRoot) throws UnableToAdaptException {
            this(moduleHandler, moduleMetaDataExtenders, nestedModuleMetaDataFactories, moduleContainer, altDDEntry, moduleURI, null, moduleClassesInfo, contextRoot);
        }

        public WebModuleContainerInfo(ModuleHandler moduleHandler, List<ModuleMetaDataExtender> moduleMetaDataExtenders,
                                      List<NestedModuleMetaDataFactory> nestedModuleMetaDataFactories,
                                      Container moduleContainer, Entry altDDEntry, String moduleURI,
                                      ModuleClassLoaderFactory moduleClassLoaderFactory,
                                      ModuleClassesInfoProvider moduleClassesInfo,
                                      String contextRoot) throws UnableToAdaptException {
            super(moduleHandler, moduleMetaDataExtenders, nestedModuleMetaDataFactories, moduleContainer, altDDEntry, moduleURI, ContainerInfo.Type.WEB_MODULE, moduleClassLoaderFactory, moduleClassesInfo, WebApp.class);
            getWebModuleClassesInfo(moduleContainer);
            this.contextRoot = contextRoot;
            this.defaultContextRoot = moduleName;
        }

        @Override
        public void setModuleName(String newModuleName) {
            super.setModuleName(newModuleName);
            this.defaultContextRoot = newModuleName;
        }

        @Override
        public ExtendedModuleInfoImpl createModuleInfoImpl(ApplicationInfo appInfo, ModuleClassLoaderFactory classLoaderFactory) throws MetaDataException {
            try {

                WebModuleInfoImpl webModuleInfo = new WebModuleInfoImpl(appInfo, moduleName, name, contextRoot, container, altDDEntry, classesContainerInfo, classLoaderFactory);
                /** Set the Default Context Root information to the web module info */
                webModuleInfo.setDefaultContextRoot(defaultContextRoot);
                return webModuleInfo;
            } catch (UnableToAdaptException e) {
                FFDCFilter.processException(e, getClass().getName(), "createModuleInfo", this);
                return null;
            }
        }

        private void getWebModuleClassesInfo(Container moduleContainer) throws UnableToAdaptException {
            ArrayList<String> resolved = new ArrayList<String>();

            Entry classesEntry = moduleContainer.getEntry("WEB-INF/classes");
            if (classesEntry != null) {
                final Container classesContainer = classesEntry.adapt(Container.class);
                if (classesContainer != null) {
                    ContainerInfo containerInfo = new ContainerInfo() {
                        @Override
                        public Type getType() {
                            return Type.WEB_INF_CLASSES;
                        }

                        @Override
                        public String getName() {
                            return "WEB-INF/classes";
                        }

                        @Override
                        public Container getContainer() {
                            return classesContainer;
                        }
                    };
                    this.classesContainerInfo.add(containerInfo);
                }
            }

            Entry libEntry = moduleContainer.getEntry("WEB-INF/lib");
            if (libEntry != null) {
                Container libContainer = libEntry.adapt(Container.class);
                if (libContainer != null) {
                    for (Entry entry : libContainer) {
                        if (entry.getName().toLowerCase().endsWith(".jar")) {
                            final String jarEntryName = entry.getName();
                            final Container jarContainer = entry.adapt(Container.class);
                            if (jarContainer != null) {
                                ContainerInfo containerInfo = new ContainerInfo() {
                                    @Override
                                    public Type getType() {
                                        return Type.WEB_INF_LIB;
                                    }

                                    @Override
                                    public String getName() {
                                        return "WEB-INF/lib/" + jarEntryName;
                                    }

                                    @Override
                                    public Container getContainer() {
                                        return jarContainer;
                                    }
                                };
                                this.classesContainerInfo.add(containerInfo);

                                ManifestClassPathUtils.addCompleteJarEntryUrls(this.classesContainerInfo, entry, resolved);
                            }
                        }
                    }
                }
            }
        }
    }

    public ExtendedApplicationInfo appInfo;
    private ServiceRegistration<ApplicationInfo> appInfoRegistration;
    public final List<DeployedModuleInfoImpl> modulesDeployed = new ArrayList<DeployedModuleInfoImpl>();
    public boolean starting;
    public boolean started;

    protected final DeployedAppServices deployedAppServices;
    protected final FutureMonitor futureMonitor;
    protected final ApplicationInfoFactory appInfoFactory;
    protected final MetaDataService metaDataService;
    protected final StateChangeService stateChangeService;
    protected final ModuleClassesInfoProvider moduleClassesInfo;

    protected final List<ModuleContainerInfoBase> moduleContainerInfos = new ArrayList<ModuleContainerInfoBase>();
    protected final Map<ExtendedModuleInfo, ModuleHandler> activeModuleHandlers = new IdentityHashMap<ExtendedModuleInfo, ModuleHandler>(4);

    protected SimpleDeployedAppInfoBase(DeployedAppServices deployedAppServices) throws UnableToAdaptException {
        this.starting = false;
        this.started = false;
        this.deployedAppServices = deployedAppServices;
        this.futureMonitor = deployedAppServices.getFutureMonitor();
        this.appInfoFactory = deployedAppServices.getApplicationInfoFactory();
        this.metaDataService = deployedAppServices.getMetaDataService();
        this.stateChangeService = deployedAppServices.getStateChangeService();
        this.moduleClassesInfo = new ModuleClassesInfoProvider();
    }

    public void addDeployedModule(DeployedModuleInfoImpl deployedMod, List<ExtendedModuleInfo> nestedModules) {
        activeModuleHandlers.remove(deployedMod.getModuleInfo());
        modulesDeployed.add(0, deployedMod);
        if (nestedModules != null) {
            for (ExtendedModuleInfo nestedModule : nestedModules) {
                ModuleHandler moduleHandler = activeModuleHandlers.remove(nestedModule);
                if (moduleHandler != null) {
                    DeployedModuleInfoImpl nestedDeployedMod = new DeployedModuleInfoImpl(moduleHandler, nestedModule);
                    deployedMod.addNestedModule(nestedDeployedMod);
                }
            }
        }
    }

    protected void registerApplicationMBean() {
        // no-op unless overridden
    }

    protected void deregisterApplicationMBean() {
        // no-op unless overridden
    }

    @Override
    public void moduleMetaDataCreated(ExtendedModuleInfo moduleInfo, ModuleHandler moduleHandler, ModuleMetaData mmd) {
        // keep track of the module handlers for the module infos
        activeModuleHandlers.put(moduleInfo, moduleHandler);
    }

    @Override
    public DeployedModuleInfo getDeployedModule(ExtendedModuleInfo moduleInfo) {
        List<DeployedModuleInfoImpl> deployedModules = modulesDeployed;
        for (DeployedModuleInfoImpl deployedModule : deployedModules) {
            if (deployedModule.getModuleInfo() == moduleInfo) {
                return deployedModule;
            }
        }
        return null;
    }

    public boolean installApp(Future<Boolean> result) {
        if (!preDeployApp(result)) {
            return false;
        }
        if (!deployModules(result)) {
            return false;
        }
        if (!postDeployApp(result)) {
            return false;
        }
        return true;
    }

    public boolean preDeployApp(Future<Boolean> result) {
        try {
            metaDataService.fireApplicationMetaDataCreated(appInfo.getMetaData(), appInfo.getContainer());
        } catch (Throwable ex) {
            uninstallApp();
            futureMonitor.setResult(result, ex);
            return false;
        }

        Map<URL, ModuleMetaData> mmds = new HashMap<URL, ModuleMetaData>();
        for (ModuleContainerInfoBase modInfo : moduleContainerInfos) {
            try {
                ModuleMetaData mmd = modInfo.createModuleMetaData(appInfo, this);
                URL location = modInfo.getContainer().getURLs().iterator().next();
                mmds.put(location, mmd);
            } catch (Throwable ex) {
                uninstallApp();
                futureMonitor.setResult(result, ex);
                return false;
            }
        }

        registerApplicationMBean();

        starting = true;
        try {
            ModuleMetaDataAccessorImpl.getModuleMetaDataAccessor().beginContext(mmds);
            stateChangeService.fireApplicationStarting(appInfo);
        } catch (Throwable ex) {
            uninstallApp();
            futureMonitor.setResult(result, ex);
            return false;
        } finally {
            ModuleMetaDataAccessorImpl.getModuleMetaDataAccessor().endContext();
        }
        return true;
    }

    public boolean postDeployApp(Future<Boolean> result) {
        started = true;
        try {
            // Register ApplicationInfo for started applications as a service component that other services can depend on via declarative services.
            final Hashtable<String, Object> props = new Hashtable<String, Object>();
            NestedConfigHelper config = appInfo.getConfigHelper();
            if (config != null) {
                Object value;
                props.put("service.pid", config.get("service.pid"));
                if (null != (value = config.get("id")))
                    props.put("id", value);
                if (null != (value = config.get("location")))
                    props.put("location", value);
                if (null != (value = config.get("type")))
                    props.put("type", value);

                appInfoRegistration = AccessController.doPrivileged(new PrivilegedAction<ServiceRegistration<ApplicationInfo>>() {
                    @Override
                    public ServiceRegistration<ApplicationInfo> run() {
                        BundleContext bundleContext = FrameworkUtil.getBundle(SimpleDeployedAppInfoBase.class).getBundleContext();
                        return bundleContext.registerService(ApplicationInfo.class, appInfo, props);
                    }
                });
            }

            stateChangeService.fireApplicationStarted(appInfo);
        } catch (Throwable ex) {
            uninstallApp();
            futureMonitor.setResult(result, ex);
            return false;
        }
        return true;
    }

    private boolean deployModules(final Future<Boolean> result) {
        DeployModulesListener listener = new DeployModulesListener(result);

        for (ModuleContainerInfoBase modInfo : moduleContainerInfos) {
            DeployedModuleInfoImpl deployedMod = (DeployedModuleInfoImpl) getDeployedModule(modInfo.moduleInfo);
            futureMonitor.onCompletion(deployedMod.installModule(this, futureMonitor, modInfo.getType()), listener);
            if (listener.isFailed()) {
                uninstallApp();
                return false;
            }
            if (modInfo.nestedModules != null) {
                for (DeployedModuleInfoImpl nestedMod : modInfo.nestedModules) {
                    nestedMod.installModule(this, null, null);
                }
            }
        }
        return true;
    }

    private class DeployModulesListener implements CompletionListener<Boolean> {
        private final Future<Boolean> aggregateResultFuture;
        private int remaining = moduleContainerInfos.size();
        private volatile boolean aggregateResult = true;

        DeployModulesListener(Future<Boolean> aggregateResultFuture) {
            this.aggregateResultFuture = aggregateResultFuture;
        }

        @Override
        public synchronized void successfulCompletion(Future<Boolean> future, Boolean result) {
            aggregateResult &= result;
            if (--remaining == 0) {
                futureMonitor.setResult(aggregateResultFuture, aggregateResult);
            }
        }

        @Override
        public synchronized void failedCompletion(Future<Boolean> future, Throwable t) {
            futureMonitor.setResult(aggregateResultFuture, t);
            aggregateResult = false;
        }

        public boolean isFailed() {
            return !aggregateResult;
        }
    }

    @Override
    public boolean uninstallApp() {
        boolean success = true;
        if (started) {
            try {
                stateChangeService.fireApplicationStopping(appInfo);
            } catch (Throwable t) {
                FFDCFilter.processException(t, getClass().getName(), "fireApplicationStopping", this);
                success = false;
            }
        }
        if (appInfoRegistration != null) {
            try {
                AccessController.doPrivileged(new PrivilegedAction<Void>() {
                    @Override
                    public Void run() {
                        appInfoRegistration.unregister();
                        return null;
                    }
                });
            } catch (Throwable t) {
                // auto FFDC
                success = false;
            }
            appInfoRegistration = null;
        }
        List<DeployedModuleInfoImpl> deployedModules = modulesDeployed;
        for (DeployedModuleInfoImpl deployedModule : deployedModules) {
            if (deployedModule.getNestedModules() != null) {
                for (DeployedModuleInfoImpl nestedMod : deployedModule.getNestedModules()) {
                    nestedMod.uninstallModule();
                }
            }
            success = deployedModule.uninstallModule();
        }
        if (starting) {
            try {
                stateChangeService.fireApplicationStopped(appInfo);
            } catch (Throwable t) {
                FFDCFilter.processException(t, getClass().getName(), "fireApplicationStopped", this);
                success = false;
            }
        }

        deregisterApplicationMBean();

        if (appInfo != null) {
            try {
                metaDataService.fireApplicationMetaDataDestroyed(appInfo.getMetaData());
            } catch (Throwable t) {
                FFDCFilter.processException(t, getClass().getName(), "fireApplicationMetaDataDestroyed", this);
                success = false;
            }

            try {
                appInfoFactory.destroyApplicationInfo(appInfo);
            } catch (Throwable t) {
                FFDCFilter.processException(t, getClass().getName(), "destroyApplicationInfo", this);
                success = false;
            }
        }
        return success;
    }
}