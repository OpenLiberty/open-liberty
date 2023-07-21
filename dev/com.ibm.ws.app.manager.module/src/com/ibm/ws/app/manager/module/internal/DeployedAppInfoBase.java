/*******************************************************************************
 * Copyright (c) 2012, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
import java.security.Policy;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Future;

import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.app.manager.module.ApplicationNestedConfigHelper;
import com.ibm.ws.app.manager.module.DeployedAppServices;
import com.ibm.ws.app.manager.module.internal.DeferredCache.FailableProducer;
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

    protected static final DeferredCache<Container> containerCache = new DeferredCache<>();

    protected static final class SharedLibDeploymentInfo {
        private final WsLocationAdmin locAdmin;
        private final ArtifactContainerFactory artifactFactory;
        private final AdaptableModuleFactory moduleFactory;

        private final List<ContainerInfo> classesContainerInfo = new ArrayList<ContainerInfo>();

        public SharedLibDeploymentInfo(DeployedAppServices deployedAppServices, String parentPid) {

            this.locAdmin = deployedAppServices.getLocationAdmin();
            this.artifactFactory = deployedAppServices.getArtifactFactory();
            this.moduleFactory = deployedAppServices.getModuleFactory();

            try {
                // Find the classloaders for the application
                StringBuilder classloaderFilter = new StringBuilder(200);
                classloaderFilter.append("(&");
                classloaderFilter.append(FilterUtils.createPropertyFilter("service.factoryPid", "com.ibm.ws.classloading.classloader"));
                classloaderFilter.append(FilterUtils.createPropertyFilter("config.parentPID", parentPid));
                classloaderFilter.append(')');

                Configuration[] classloaderConfigs = deployedAppServices.getConfigurationAdmin().listConfigurations(classloaderFilter.toString());
                if (classloaderConfigs != null && classloaderConfigs.length == 1) {
                    Configuration cfg = classloaderConfigs[0];
                    Dictionary<String, Object> props = cfg.getProperties();
                    if (props != null) {
                        String[] libraryPIDs = (String[]) props.get("privateLibraryRef");
                        processLibraryPIDs(deployedAppServices, classesContainerInfo, libraryPIDs, LibraryType.PRIVATE_LIB);

                        libraryPIDs = (String[]) props.get("commonLibraryRef");
                        processLibraryPIDs(deployedAppServices, classesContainerInfo, libraryPIDs, LibraryType.COMMON_LIB);
                    } else {
                        cfg.delete();
                        return;
                    }
                }

                if (classesContainerInfo.isEmpty()) {
                    addLibraryContainers(classesContainerInfo, deployedAppServices.getGlobalSharedLibraryPid(), LibraryType.GLOBAL_LIB,
                                         deployedAppServices.getGlobalSharedLibrary());
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

        private void processLibraryPIDs(DeployedAppServices deployedAppServices, List<ContainerInfo> sharedLibContainers, String[] libraryPIDs,
                                        LibraryContainerInfo.LibraryType libType) throws InvalidSyntaxException {
            if (libraryPIDs != null) {
                for (String libraryPid : libraryPIDs) {
                    Collection<Library> libraries = deployedAppServices.getLibrariesFromPid(libraryPid);
                    for (Library library : libraries) {
                        addLibraryContainers(sharedLibContainers, libraryPid, libType, library);
                    }
                }
            }
        }

        private void addLibraryContainers(List<ContainerInfo> sharedLibContainers,
                                          String libraryPid, LibraryContainerInfo.LibraryType libType, Library library) {
            if (library == null) {
                return;
            }

            String libName = library.id();

            SharedLibClassesContainerInfo libClassesInfo = new SharedLibClassesContainerInfo(libType, library, "/" + libName);
            List<ContainerInfo> libContainerInfo = libClassesInfo.getClassesContainerInfo();

            addContainers(libContainerInfo, libraryPid, libName, library.getFiles());
            addContainers(libContainerInfo, libraryPid, libName, library.getFolders());
            for (Fileset libFileset : library.getFilesets()) {
                addContainers(libContainerInfo, libraryPid, libName, libFileset.getFileset());
            }

            if (!libContainerInfo.isEmpty()) {
                sharedLibContainers.add(libClassesInfo);
            }
        }

        private void addContainers(List<ContainerInfo> libContainerInfo,
                                   String libraryPid, String libName, Collection<File> libFiles) {

            if (libFiles == null) {
                return;
            }

            for (File libFile : libFiles) {
                Container container = setupContainer(libraryPid, libFile);
                if (container == null) {
                    continue;
                }

                // TODO: 'name' is not guaranteed to be unique.
                String name = "/" + libName + "/" + libFile.getName();
                libContainerInfo.add(new ContainerInfo() {
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

        // TODO: The first library which uses a specified library file has it's PID used by the shared
        //       library container.
        //
        //       This only matters if the same library file is used by more than one library.
        //
        //       This might not be significant: The additional associated folders are possibly never used.
        //
        //       Even if used, would the cached data be different for different libraries?  For example,
        //       extracted nested archives would be the same.  On the other hand, application specific class
        //       loading historical information or cached classes (which capture a history) would be application
        //       specific.
        //
        //       As an alternative, container sharing could be limited by library PID.

        private Container setupContainer(String libraryPid, File libFile) {
            String libPath = libFile.getAbsolutePath();
            FailableProducer<Container> containerSource = containerCache.defer(libPath, () -> baseSetupContainer(libraryPid, libFile));
            try {
                return containerSource.produce();
            } catch (Exception e) {
                return null; // Can't happen
            }
        }

        private Container baseSetupContainer(String libraryPid, File libraryFile) {
            if (_tc.isDebugEnabled()) {
                Tr.debug(_tc, "Creating container for library [ " + libraryPid + " ] on [ " + libraryFile.getAbsolutePath() + " ]");
            }

            if (!FileUtils.fileExists(libraryFile)) {
                if (_tc.isDebugEnabled()) {
                    Tr.debug(_tc, "Null library container: the library file does not exist.");
                }
                return null;
            }

            File cacheDir = new File(getCacheDir(), libraryPid);
            if (!FileUtils.ensureDirExists(cacheDir)) {
                if (_tc.isDebugEnabled()) {
                    Tr.debug(_tc, "Null library container: Cache [ " + cacheDir.getAbsolutePath() + " ] was not created");
                }
                return null;
            }
            if (_tc.isDebugEnabled()) {
                Tr.debug(_tc, "Cache [ " + cacheDir.getAbsolutePath() + " ]");
            }

            ArtifactContainer artifactContainer = artifactFactory.getContainer(cacheDir, libraryFile);
            if (artifactContainer == null) {
                if (_tc.isDebugEnabled()) {
                    Tr.debug(_tc, "Null library container: Artifact container creation failed.");
                }
                return null;
            }
            if (_tc.isDebugEnabled()) {
                Tr.debug(_tc, "Artifact container [ " + artifactContainer + " ]");
            }

            File adaptDir = new File(getCacheAdaptDir(), libraryPid);
            if (!FileUtils.ensureDirExists(adaptDir)) {
                if (_tc.isDebugEnabled()) {
                    Tr.debug(_tc, "Null library container: Adapt directory [ " + adaptDir.getAbsolutePath() + " ] was not created");
                }
                return null;
            }
            if (_tc.isDebugEnabled()) {
                Tr.debug(_tc, "Adapt directory [ " + adaptDir.getAbsolutePath() + " ]");
            }

            File overlayDir = new File(getCacheOverlayDir(), libraryPid);
            if (!FileUtils.ensureDirExists(overlayDir)) {
                if (_tc.isDebugEnabled()) {
                    Tr.debug(_tc, "Null library container: Overlay directory [ " + overlayDir.getAbsolutePath() + " ] was not created");
                }
                return null;
            }
            if (_tc.isDebugEnabled()) {
                Tr.debug(_tc, "Overlay directory [ " + overlayDir.getAbsolutePath() + " ]");
            }

            Container adaptableContainer = moduleFactory.getContainer(adaptDir, overlayDir, artifactContainer);
            if (_tc.isDebugEnabled()) {
                if (adaptableContainer == null) {
                    Tr.debug(_tc, "Null library container: Adaptable container creation failed");
                } else {
                    Tr.debug(_tc, "Created library container: Adaptable container [ " + adaptableContainer + " ]");
                }
            }
            return adaptableContainer;
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
    private final ConfigurationAdmin configAdmin;
    private final ClassLoaderConfigHelper libraryConfigHelper;
    protected final boolean isDelegateLast;
    private final PermissionManager permissionManager;
    private final PermissionsConfig permissionsConfig;

    protected DeployedAppInfoBase(ApplicationInformation<?> applicationInformation,
                                  DeployedAppServices deployedAppServices) throws UnableToAdaptException {
        super(deployedAppServices);
        this.applicationInformation = applicationInformation;
        this.location = applicationInformation.getLocation();
        this.classLoadingService = deployedAppServices.getClassLoadingService();
        this.globalSharedLibrary = deployedAppServices.getGlobalSharedLibrary();
        this.configAdmin = deployedAppServices.getConfigurationAdmin();
        this.libraryConfigHelper = new ClassLoaderConfigHelper(getConfigHelper(), configAdmin, classLoadingService);
        this.isDelegateLast = libraryConfigHelper.isDelegateLast();
        this.permissionManager = deployedAppServices.getPermissionManager();
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
        this.sharedLibDeploymentInfo = new SharedLibDeploymentInfo(deployedAppServices, parentPid);
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

    public boolean deployApp(Future<Boolean> result) {
        if (moduleContainerInfos.isEmpty()) {
            // Subclasses should either always add a module or give an error.
            throw new IllegalStateException();
        }
        appInfo = createApplicationInfo();

        return installApp(result);
    }

    protected ModuleClassLoaderFactory getModuleClassLoaderFactory() {
        return this;
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

        CodeSource codeSource;
        try {
            codeSource = getCodeSource();
        } catch (MalformedURLException e) {
            // Raise exception
            codeSource = null;
            if (_tc.isDebugEnabled()) {
                Tr.debug(_tc, "Code Source could not be set. Setting it to null.", e);
            }
        }

        if (!java2SecurityEnabled()) {
            perms.add(new AllPermission());
        } else {
            if (permissionsConfig != null && codeSource != null) {
                List<com.ibm.ws.javaee.dd.permissions.Permission> configuredPermissions = permissionsConfig.getPermissions();
                addPermissions(codeSource, configuredPermissions);
            }

            PermissionCollection mergedPermissions = new Permissions();
            if (codeSource != null) {
                mergedPermissions = Policy.getPolicy().getPermissions(codeSource);
            }

            Enumeration<Permission> enumeration = mergedPermissions.elements();
            while (enumeration.hasMoreElements()) {
                Permission p = enumeration.nextElement();
                perms.add(p);
            }
        }

        return new ProtectionDomain(codeSource, perms);
    }

    private CodeSource getCodeSource() throws MalformedURLException {
        CodeSource codeSource = getContainerCodeSource();

        if (codeSource == null) {
            codeSource = getLocationCodeSource();
        }
        return codeSource;
    }

    private CodeSource getContainerCodeSource() {
        CodeSource containerCodeSource = null;
        Container container = applicationInformation.getContainer();

        if (container != null) {
            Iterator<URL> urlsIterator = container.getURLs().iterator();
            if (urlsIterator.hasNext()) {
                containerCodeSource = new CodeSource(urlsIterator.next(), (java.security.cert.Certificate[]) null);
            }
        }

        return containerCodeSource;
    }

    private CodeSource getLocationCodeSource() throws MalformedURLException {
        String loc = applicationInformation.getLocation();
        return new CodeSource(new URL("file://" + (loc.startsWith("/") ? "" : "/") + loc), (java.security.cert.Certificate[]) null);
    }

    /**
     * @param codeSource
     * @param configuredPermissions
     */
    private void addPermissions(CodeSource codeSource, List<com.ibm.ws.javaee.dd.permissions.Permission> configuredPermissions) {
        Permission[] permissions = new Permission[configuredPermissions.size()];

        int count = 0;
        for (com.ibm.ws.javaee.dd.permissions.Permission permission : configuredPermissions) {
            Permission perm = permissionManager.createPermissionObject(permission.getClassName(),
                                                                       permission.getName(),
                                                                       permission.getActions(), null, null, null, PERMISSION_XML);

            if (perm != null) {
                permissions[count++] = perm;
                permissionManager.addPermissionsXMLPermission(codeSource, perm);
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
