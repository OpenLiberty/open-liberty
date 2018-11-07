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
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.AllPermission;
import java.security.CodeSource;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.app.manager.module.ApplicationNestedConfigHelper;
import com.ibm.ws.classloading.ClassLoaderConfigHelper;
import com.ibm.ws.classloading.ClassLoadingButler;
import com.ibm.ws.classloading.java2sec.PermissionManager;
import com.ibm.ws.container.service.app.deploy.ApplicationClassesContainerInfo;
import com.ibm.ws.container.service.app.deploy.ContainerInfo;
import com.ibm.ws.container.service.app.deploy.NestedConfigHelper;
import com.ibm.ws.container.service.app.deploy.extended.AppClassLoaderFactory;
import com.ibm.ws.container.service.app.deploy.extended.ExtendedApplicationInfo;
import com.ibm.ws.container.service.app.deploy.extended.LibraryClassesContainerInfo;
import com.ibm.ws.container.service.app.deploy.extended.LibraryContainerInfo;
import com.ibm.ws.container.service.app.deploy.extended.LibraryContainerInfo.LibraryType;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.javaee.dd.permissions.PermissionsConfig;
import com.ibm.ws.runtime.metadata.ModuleMetaData;
import com.ibm.ws.threadContext.ModuleMetaDataAccessorImpl;
import com.ibm.ws.threading.FutureMonitor;
import com.ibm.ws.threading.listeners.CompletionListener;
import com.ibm.wsspi.adaptable.module.AdaptableModuleFactory;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.application.handler.ApplicationInformation;
import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.factory.ArtifactContainerFactory;
import com.ibm.wsspi.classloading.ClassLoaderConfiguration;
import com.ibm.wsspi.classloading.ClassLoadingService;
import com.ibm.wsspi.classloading.GatewayConfiguration;
import com.ibm.wsspi.config.Fileset;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.kernel.service.utils.FileUtils;
import com.ibm.wsspi.kernel.service.utils.FilterUtils;
import com.ibm.wsspi.library.Library;

public abstract class DeployedAppInfoBase extends SimpleDeployedAppInfoBase implements ApplicationClassesContainerInfo, AppClassLoaderFactory, ModuleClassLoaderFactory {
    private static final TraceComponent _tc = Tr.register(DeployedAppInfoBase.class, "app.manager", "com.ibm.ws.app.manager.module.internal.resources.Messages");
    private static final String PERMISSION_XML = "permissions.xml";

    protected static final class SharedLibClassesContainerInfo implements LibraryClassesContainerInfo {
        private final LibraryContainerInfo.LibraryType libType;
        private final Library sharedLib;
        private final String libName;

        private final List<ContainerInfo> classesContainerInfo = new ArrayList<ContainerInfo>();

        SharedLibClassesContainerInfo(LibraryContainerInfo.LibraryType libType, Library sharedLib, String libName) {
            this.sharedLib = sharedLib;
            this.libName = libName;
            this.libType = libType;
        }

        @Override
        public Type getType() {
            return Type.SHARED_LIB;
        }

        @Override
        public String getName() {
            return libName;
        }

        @Override
        public Container getContainer() {
            return null;
        }

        @Override
        public LibraryType getLibraryType() {
            return libType;
        }

        @Override
        public ClassLoader getClassLoader() {
            return sharedLib.getClassLoader();
        }

        @Override
        public List<ContainerInfo> getClassesContainerInfo() {
            return classesContainerInfo;
        }
    }

    protected static final class SharedLibDeploymentInfo {
        private final BundleContext bundleContext;
        private final WsLocationAdmin locAdmin;
        private final ArtifactContainerFactory artifactFactory;
        private final AdaptableModuleFactory moduleFactory;

        private final List<ContainerInfo> classesContainerInfo = new ArrayList<ContainerInfo>();

        public SharedLibDeploymentInfo(DeployedAppInfoFactoryBase factory, String parentPid) {

            this.bundleContext = factory.getBundleContext();
            this.locAdmin = factory.getLocationAdmin();
            this.artifactFactory = factory.getArtifactFactory();
            this.moduleFactory = factory.getModuleFactory();

            try {
                // Find the classloaders for the application
                StringBuilder classloaderFilter = new StringBuilder(200);
                classloaderFilter.append("(&");
                classloaderFilter.append(FilterUtils.createPropertyFilter("service.factoryPid", "com.ibm.ws.classloading.classloader"));
                classloaderFilter.append(FilterUtils.createPropertyFilter("config.parentPID", parentPid));
                classloaderFilter.append(')');

                Configuration[] classloaderConfigs = factory.getConfigurationAdmin().listConfigurations(classloaderFilter.toString());
                if (classloaderConfigs != null && classloaderConfigs.length == 1) {
                    Configuration cfg = classloaderConfigs[0];
                    Dictionary<String, Object> props = cfg.getProperties();
                    if (props != null) {
                        String[] libraryPIDs = (String[]) props.get("privateLibraryRef");
                        processLibraryPIDs(classesContainerInfo, libraryPIDs, LibraryType.PRIVATE_LIB);

                        libraryPIDs = (String[]) props.get("commonLibraryRef");
                        processLibraryPIDs(classesContainerInfo, libraryPIDs, LibraryType.COMMON_LIB);
                    } else {
                        cfg.delete();
                        return;
                    }
                }

                if (classesContainerInfo.isEmpty()) {
                    addLibraryContainers(classesContainerInfo, factory.getGlobalSharedLibraryPid(), LibraryType.GLOBAL_LIB, factory.getGlobalSharedLibrary());
                }
            } catch (IOException e) {
                // Auto FFDC
                return;
            } catch (InvalidSyntaxException e) {
                // Auto FFDC
                return;
            }
        }

        public List<ContainerInfo> getClassesContainerInfo() {
            return this.classesContainerInfo;
        }

        private void processLibraryPIDs(List<ContainerInfo> sharedLibContainers, String[] libraryPIDs, LibraryContainerInfo.LibraryType libType) throws InvalidSyntaxException {
            if (libraryPIDs != null) {
                for (String pid : libraryPIDs) {
                    String libraryFilter = FilterUtils.createPropertyFilter(Constants.SERVICE_PID, pid);
                    Collection<ServiceReference<Library>> libraryRefs = bundleContext.getServiceReferences(Library.class, libraryFilter);
                    for (ServiceReference<Library> libraryRef : libraryRefs) {
                        Library library = bundleContext.getService(libraryRef);
                        addLibraryContainers(sharedLibContainers, pid, libType, library);
                    }
                }
            }
        }

        private void addLibraryContainers(List<ContainerInfo> sharedLibContainers, String pid, LibraryContainerInfo.LibraryType libType, Library library) {
            if (library != null) {
                String libName = library.id();
                SharedLibClassesContainerInfo libClassesInfo = new SharedLibClassesContainerInfo(libType, library, "/" + libName);
                Collection<File> files = library.getFiles();
                addContainers(libClassesInfo.getClassesContainerInfo(), pid, libName, files);
                Collection<File> folders = library.getFolders();
                addContainers(libClassesInfo.getClassesContainerInfo(), pid, libName, folders);
                Collection<Fileset> filesets = library.getFilesets();
                for (Fileset fileset : filesets) {
                    addContainers(libClassesInfo.getClassesContainerInfo(), pid, libName, fileset.getFileset());
                }
                if (!libClassesInfo.getClassesContainerInfo().isEmpty()) {
                    sharedLibContainers.add(libClassesInfo);
                }
            }
        }

        private void addContainers(List<ContainerInfo> sharedLibContainers, String pid, String libName, Collection<File> files) {
            if (files != null) {
                for (File file : files) {
                    final Container container = setupContainer(pid, file);
                    if (container != null) {
                        final String name = "/" + libName + "/" + file.getName();
                        sharedLibContainers.add(new ContainerInfo() {
                            @Override
                            public Type getType() {
                                return Type.SHARED_LIB;
                            }

                            @Override
                            public String getName() {
                                return name;
                            }

                            @Override
                            public Container getContainer() {
                                return container;
                            }
                        });
                    }
                }
            }
        }

        private Container setupContainer(String pid, File locationFile) {
            if (!FileUtils.fileExists(locationFile)) {
                return null;
            }

            File cacheDir = new File(getCacheDir(), pid);
            if (!FileUtils.ensureDirExists(cacheDir)) {
                return null;
            }

            ArtifactContainer artifactContainer = artifactFactory.getContainer(cacheDir, locationFile);
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

            return moduleFactory.getContainer(cacheDirAdapt, cacheDirOverlay, artifactContainer);
        }

        private File getCacheAdaptDir() {
            return locAdmin.getBundleFile(this, "cacheAdapt");
        }

        private File getCacheOverlayDir() {
            return locAdmin.getBundleFile(this, "cacheOverlay");
        }

        private File getCacheDir() {
            return locAdmin.getBundleFile(this, "cache");
        }
    }

    protected final ApplicationInformation<?> applicationInformation;
    public final String location;

    protected final ClassLoadingService classLoadingService;
    protected final Library globalSharedLibrary;
    protected final SharedLibDeploymentInfo sharedLibDeploymentInfo;
    private final FutureMonitor futureMonitor;
    private final ConfigurationAdmin configAdmin;
    private final ClassLoaderConfigHelper libraryConfigHelper;
    protected final boolean isDelegateLast;
    private final PermissionManager permissionManager;
    private final PermissionsConfig permissionsConfig;

    protected DeployedAppInfoBase(ApplicationInformation<?> applicationInformation,
                                  DeployedAppInfoFactoryBase factory) throws UnableToAdaptException {
        super(factory);
        this.applicationInformation = applicationInformation;
        this.location = applicationInformation.getLocation();
        this.classLoadingService = factory.getClassLoadingService();
        this.globalSharedLibrary = factory.getGlobalSharedLibrary();
        this.futureMonitor = factory.getFutureMonitor();
        this.configAdmin = factory.getConfigurationAdmin();
        this.libraryConfigHelper = new ClassLoaderConfigHelper(getConfigHelper(), configAdmin, classLoadingService);
        this.isDelegateLast = libraryConfigHelper.isDelegateLast();
        this.permissionManager = factory.getPermissionManager();
        try {
            this.permissionsConfig = getContainer().adapt(PermissionsConfig.class); // throws UnableToAdaptException
        } catch (UnableToAdaptException e) {
            // error.application.parse.descriptor=
            // CWWKZ0113E: Application {0}: Parse error for application descriptor {1}: {2}
            Tr.error(_tc, "error.application.parse.descriptor", getName(), "META-INF/permissions.xml", e);
            throw e;
        }
        String parentPid = (String) applicationInformation.getConfigProperty(Constants.SERVICE_PID);
        String sourcePid = (String) applicationInformation.getConfigProperty("ibm.extends.source.pid");
        if (sourcePid != null) {
            parentPid = sourcePid;
        }
        this.sharedLibDeploymentInfo = new SharedLibDeploymentInfo(factory, parentPid);
    }

    @Trivial
    public String getName() {
        return applicationInformation.getName();
    }

    @Trivial
    public Container getContainer() {
        return applicationInformation.getContainer();
    }

    @Trivial
    public NestedConfigHelper getConfigHelper() {
        return new ApplicationNestedConfigHelper(applicationInformation);
    }

    protected abstract ExtendedApplicationInfo createApplicationInfo();

    @Override
    public List<ContainerInfo> getLibraryClassesContainerInfo() {
        if (sharedLibDeploymentInfo.getClassesContainerInfo() != null) {
            return Collections.unmodifiableList(sharedLibDeploymentInfo.getClassesContainerInfo());
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public ClassLoader createAppClassLoader() {
        // default implementation for simple single module apps, EARDeployedAppInfo overrides this
        return null;
    }

    protected void registerApplicationMBean() {
        // no-op unless overridden
    }

    public boolean preDeployApp(Future<Boolean> result) {
        appInfo = createApplicationInfo();

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
                ModuleMetaData mmd = modInfo.createModuleMetaData(appInfo, this, this);
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
            stateChangeService.fireApplicationStarted(appInfo);
        } catch (Throwable ex) {
            uninstallApp();
            futureMonitor.setResult(result, ex);
            return false;
        }
        return true;
    }

    public boolean deployApp(Future<Boolean> result) {
        if (moduleContainerInfos.isEmpty()) {
            // Subclasses should either always add a module or give an error.
            throw new IllegalStateException();
        }

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

    protected ClassLoader createTopLevelClassLoader(List<Container> classPath,
                                                    GatewayConfiguration gwConfig,
                                                    ClassLoaderConfiguration config) {
        ClassLoader classLoader = libraryConfigHelper.createTopLevelClassLoader(classPath, gwConfig, config, classLoadingService, globalSharedLibrary);
        associateClassLoaderWithApp(classLoader);
        return classLoader;
    }

    protected boolean associateClassLoaderWithApp(ClassLoader loader) {
        boolean success;
        try {
            ClassLoadingButler butler = appInfo.getContainer().adapt(ClassLoadingButler.class);
            butler.addClassLoader(loader);
            success = true;
        } catch (Exception ex) {
            success = false;
            //ffdc -- this will most likely be an NPE if the appInfo has not yet been created, or its
            // container was not created.
            // it could also be an UnableToAdaptException if the butler could not be created for some
            // reason.
            if (_tc.isDebugEnabled()) {
                Tr.debug(_tc, "Failed to associate this app with a its loader via the ClassLoadingButler", ex);
            }
        }
        return success;
    }

    /**
     * @return
     */
    @FFDCIgnore({ MalformedURLException.class })
    protected ProtectionDomain getProtectionDomain() {
        PermissionCollection perms = new Permissions();

        if (!java2SecurityEnabled()) {
            perms.add(new AllPermission());
        } else {
            if (permissionsConfig != null) {
                List<com.ibm.ws.javaee.dd.permissions.Permission> configuredPermissions = permissionsConfig.getPermissions();
                addPermissions(configuredPermissions);
            }

            ArrayList<Permission> mergedPermissions = permissionManager.getEffectivePermissions(applicationInformation.getLocation());
            int count = mergedPermissions.size();
            for (int i = 0; i < count; i++)
                perms.add(mergedPermissions.get(i));
        }

        CodeSource codesource;
        try {
            // codesource must start file:///
            // assume loc starts with 0 or 1 /
            String loc = applicationInformation.getLocation();
            codesource = new CodeSource(new URL("file://" + (loc.startsWith("/") ? "" : "/") + loc), (java.security.cert.Certificate[]) null);
        } catch (MalformedURLException e) {

            codesource = null;
            if (_tc.isDebugEnabled()) {

                Tr.debug(_tc, "Code Source could not be set. Setting it to null.", e);
            }
        }
        ProtectionDomain protectionDomain = new ProtectionDomain(codesource, perms);
        return protectionDomain;
    }

    /**
     * @param configuredPermissions
     */
    private void addPermissions(List<com.ibm.ws.javaee.dd.permissions.Permission> configuredPermissions) {
        Permission[] permissions = new Permission[configuredPermissions.size()];

        String codeBase = applicationInformation.getLocation();

        int count = 0;
        for (com.ibm.ws.javaee.dd.permissions.Permission permission : configuredPermissions) {
            Permission perm = permissionManager.createPermissionObject(permission.getClassName(),
                                                                       permission.getName(),
                                                                       permission.getActions(), null, null, null, PERMISSION_XML);

            if (perm != null) {
                permissions[count++] = perm;
                permissionManager.addPermissionsXMLPermission(codeBase, perm);
            }
        }
    }

    /**
     * @return
     */
    private boolean java2SecurityEnabled() {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null)
            return true;
        else
            return false;
    }
}