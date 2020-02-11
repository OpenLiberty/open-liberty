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
package com.ibm.ws.app.manager.ear.internal;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.osgi.framework.ServiceRegistration;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.app.manager.module.DeployedAppInfo;
import com.ibm.ws.app.manager.module.DeployedAppMBeanRuntime;
import com.ibm.ws.app.manager.module.DeployedAppServices;
import com.ibm.ws.app.manager.module.internal.ContextRootUtil;
import com.ibm.ws.app.manager.module.internal.DeployedAppInfoBase;
import com.ibm.ws.app.manager.module.internal.DeployedModuleInfoImpl;
import com.ibm.ws.app.manager.module.internal.ModuleHandler;
import com.ibm.ws.app.manager.module.internal.ModuleInfoUtils;
import com.ibm.ws.container.service.annocache.AnnotationsBetaHelper;
// import com.ibm.ws.container.service.annotations.ContainerAnnotations; // PreBeta
// import com.ibm.ws.container.service.annocache.ContainerAnnotations; // PostBeta
import com.ibm.ws.container.service.app.deploy.ClientModuleInfo;
import com.ibm.ws.container.service.app.deploy.ConnectorModuleInfo;
import com.ibm.ws.container.service.app.deploy.ContainerInfo;
import com.ibm.ws.container.service.app.deploy.EJBModuleInfo;
import com.ibm.ws.container.service.app.deploy.ManifestClassPathUtils;
import com.ibm.ws.container.service.app.deploy.ModuleClassesContainerInfo;
import com.ibm.ws.container.service.app.deploy.ModuleInfo;
import com.ibm.ws.container.service.app.deploy.WebModuleInfo;
import com.ibm.ws.container.service.app.deploy.extended.ExtendedApplicationInfo;
import com.ibm.ws.javaee.dd.app.Application;
import com.ibm.ws.javaee.dd.app.Module;
import com.ibm.ws.javaee.ddmodel.DDParser;
import com.ibm.ws.javaee.version.JavaEEVersion;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.DefaultNotification;
import com.ibm.wsspi.adaptable.module.Entry;
import com.ibm.wsspi.adaptable.module.Notifier.Notification;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.application.handler.ApplicationInformation;
import com.ibm.wsspi.application.handler.ApplicationMonitoringInformation;
import com.ibm.wsspi.application.handler.DefaultApplicationMonitoringInformation;
import com.ibm.wsspi.classloading.ClassLoaderConfiguration;
import com.ibm.wsspi.classloading.ClassLoaderIdentity;
import com.ibm.wsspi.classloading.GatewayConfiguration;

public class EARDeployedAppInfo extends DeployedAppInfoBase {
    private static final TraceComponent _tc = Tr.register(EARDeployedAppInfo.class);

    // Factory derived values:

    private final ModuleHandler webModuleHandler;
    private final ModuleHandler ejbModuleHandler;
    private final ModuleHandler clientModuleHandler;
    private final ModuleHandler connectorModuleHandler;

    private final DeployedAppMBeanRuntime appMBeanRuntime;
    private ServiceRegistration<?> mbeanServiceReg;

    private final Container preExpansionAppContainer;

    private final Application appDD;
    private boolean ddInitializeInOrder;

    private final String appPreferredName;
    private final boolean altDDEnabled;
    private final AppLibsInfo appLibsInfo;

    //

    private static final String DEFAULT_CLIENT_MODULE = "defaultClientModule";
    private final String appConfigDefaultClientModule;

    private static final String CONTEXT_ROOT = "context-root";

    //

    private final ClassLoaderIdentity appClassLoaderId;
    private ClassLoader appClassLoader;
    private ProtectionDomain protectionDomain; // Used for both the app class loader and for module class loaders.

    @Override
    public ClassLoader createAppClassLoader() {
        return getAppClassLoader();
    }

    private ClassLoaderIdentity getAppClassLoaderId() {
        // Make sure the application class loader has been created, which
        // ensures that the application class load ID is valid as a reference.
        //
        // The application class loader ID is used when creating module
        // class loaders.

        getAppClassLoader();

        return appClassLoaderId;
    }

    private ClassLoader getAppClassLoader() {
        if (appClassLoader == null) {
            appClassLoader = basicCreateAppClassLoader();
        }
        return appClassLoader;
    }

    // The imports of the application class loader and for module
    // class loaders.

    private static final List<String> DYNAMIC_IMPORTS = Collections.unmodifiableList(Arrays.asList("*"));

    private ClassLoader basicCreateAppClassLoader() {
        String appPrefix;
        if (_tc.isDebugEnabled()) {
            appPrefix = "Application [ " + getName() + " ]: ";
        } else {
            appPrefix = null;
        }

        if (appPrefix != null) {
            Tr.debug(_tc, appPrefix + "Class loader ID [ " + appClassLoaderId + " ]");
        }

        List<Container> classLoaderContainers = new ArrayList<Container>();
        List<Container> nativeLibraryContainers = new ArrayList<Container>();

        for (ContainerInfo classesContainerInfo : getClasspathContainerInfos()) {
            Container classesContainer = classesContainerInfo.getContainer();

            classLoaderContainers.add(classesContainer);

            if (classesContainerInfo.getType() == ContainerInfo.Type.RAR_MODULE) {
                nativeLibraryContainers.add(classesContainer);
            }
        }

        GatewayConfiguration gatewayConfig = classLoadingService.createGatewayConfiguration();
        gatewayConfig.setApplicationName(getName());
        gatewayConfig.setDynamicImportPackage(DYNAMIC_IMPORTS);

        // Keep a reference to the protection domain: It is also used when creating
        // module class loaders.
        protectionDomain = getProtectionDomain();
        if (appPrefix != null) {
            Tr.debug(_tc, appPrefix + "Protection domain [ " + protectionDomain + " ]");
        }

        ClassLoaderConfiguration appClassLoaderConfig = classLoadingService.createClassLoaderConfiguration();
        appClassLoaderConfig.setId(appClassLoaderId);
        appClassLoaderConfig.setProtectionDomain(protectionDomain);
        appClassLoaderConfig.setNativeLibraryContainers(nativeLibraryContainers);

        ClassLoader useAppClassLoader = createTopLevelClassLoader(
                                                                  classLoaderContainers, gatewayConfig, appClassLoaderConfig);

        if (appPrefix != null) {
            Tr.debug(_tc, appPrefix + "Class loader [ " + useAppClassLoader + " ]");
        }
        return useAppClassLoader;
    }

    /**
     * Create deployment information for an application.
     *
     * @param appInfo                  The application information.
     * @param appDD                    The descriptor of the application. May be null.
     * @param factory                  Factory of various widgets used to process the application information.
     * @param preExpansionAppContainer The container of the application prior to the
     *                                     application expansion.
     *
     * @throws UnableToAdaptException Thrown in case of an error creating
     *                                    deployment information for the application.
     */
    EARDeployedAppInfo(
                       ApplicationInformation<DeployedAppInfo> appInfo,
                       Application appDD,
                       EARDeployedAppInfoFactoryImpl factory,
                       DeployedAppServices deployedAppServices,
                       Container preExpansionAppContainer) throws UnableToAdaptException {

        super(appInfo, deployedAppServices);

        this.webModuleHandler = factory.webModuleHandler;
        this.ejbModuleHandler = factory.ejbModuleHandler;
        this.clientModuleHandler = factory.clientModuleHandler;
        this.connectorModuleHandler = factory.connectorModuleHandler;
        this.appMBeanRuntime = factory.appMBeanRuntime;

        this.appDD = appDD;
        this.altDDEnabled = (factory.platformVersion.compareTo(JavaEEVersion.VERSION_7_0) >= 0); // JavaEE7 or higher

        this.preExpansionAppContainer = preExpansionAppContainer;

        // Set the name of the application either from the descriptor, or taking
        // the name of the application file, subtracting ".ear" if that is present.

        String appName = ((appDD == null) ? null : appDD.getApplicationName());
        if (appName == null) {
            appName = ModuleInfoUtils.getModuleURIFromLocation(appInfo.getLocation());
            if (appName.endsWith(".ear")) {
                appName = appName.substring(0, appName.length() - ".ear".length());
            }
        }
        this.appPreferredName = appName;

        this.appLibsInfo = this.createAppLibsInfo();

        // The application class loader uses the application name from the application information,
        // not the name obtained from the application descriptor or from the application location.

        this.appClassLoaderId = this.classLoadingService.createIdentity("EARApplication", getName());

        // When there are more than one client modules in the application, the client module to process
        // can be selected by using <enterpriseApplication ... defaultClientModule="module_name" .../>

        this.appConfigDefaultClientModule = (String) appInfo.getConfigProperty(DEFAULT_CLIENT_MODULE);

        this.createModules();

        Object appConfigContextRoot = appInfo.getConfigProperty(CONTEXT_ROOT);
        if (appConfigContextRoot != null) {
            Tr.warning(_tc, "warning.context.root.not.used", appConfigContextRoot, appPreferredName);
        }
    }

    //

    /**
     * Application life-cycle: Create application information for this deployed application.
     *
     * @return Information for the application.
     */
    @Override
    protected ExtendedApplicationInfo createApplicationInfo() {
        Container appLibContainer = ((appLibsInfo != null) ? appLibsInfo.getLibsContainer() : null);

        return appInfoFactory.createEARApplicationInfo(
                                                       getName(), appPreferredName,
                                                       getContainer(),
                                                       this,
                                                       getConfigHelper(),
                                                       appLibContainer,
                                                       this);
    }

    // Application libraries helpers ...
    //
    // An application (optionally) has shared libraries and application libraries.

    private AppLibsInfo createAppLibsInfo() {
        String libDir;
        if ((appDD != null) && (appDD.getLibraryDirectory() != null)) {
            libDir = appDD.getLibraryDirectory();
            if (libDir.length() == 0) {
                // Having an empty library directory element means that the application
                // has no library directory.
                return null;
            }
        } else {
            // Having no library directory element, including when there is no application
            // descriptor at all, means the application library is the default value, "lib".
            libDir = "lib";
        }

        Entry libDirEntry = getContainer().getEntry(libDir);
        if (libDirEntry == null) {
            // The specified library location need not exists.  When it doesn't,
            // the application is treated as having no library directory.
            return null;
        }

        Container libDirContainer;
        try {
            libDirContainer = libDirEntry.adapt(Container.class);
        } catch (UnableToAdaptException e) {
            // FFDC
            // Treat this the same as other adapt failures: Report
            // the error, then move on with as much information as
            // was successfully gathered.
            return null;
        }

        return new AppLibsInfo(libDirContainer);
    }

    /**
     * Container information for a single application library.
     *
     * These answer {@link Type#EAR_LIB} for {@link ContainerInfo#getType()}.
     *
     * These are stored in {@link #getLibsInfos()}, along with
     * other container information for entities reached from manifest class
     * paths of the EAR library entries.
     */
    private static class EARLibContainerInfo implements ContainerInfo {
        public EARLibContainerInfo(String name, Container container) {
            this.name = name;
            this.container = container;
        }

        @Override
        public Type getType() {
            return Type.EAR_LIB;
        }

        private final String name;

        /**
         * Answer the name of the EAR lib jar.
         *
         * The usual name for EAR lib jars is:
         * <code>"EAR" + earLibs.getPath() earLibContainer.getName()</code>
         * Most commonly, the EAR lib path is just "lib", giving a name, for
         * example, "EARlibmyJar.jar".
         *
         * @return The name of the EAR lib jar.
         */
        @Override
        public String getName() {
            return name;
        }

        private final Container container;

        @Override
        public Container getContainer() {
            return container;
        }
    };

    /**
     * Container information for application libraries.
     *
     * This consists of a root library container and the collection
     * of application libraries. Included in the application libraries
     * are any jars obtained by expanding manifest class paths of the
     * actual application libraries.
     */
    private static class AppLibsInfo {
        public AppLibsInfo(Container libsContainer) {
            this.libsContainer = libsContainer;

            String libsPath = libsContainer.getPath(); // Usually "lib"

            List<ContainerInfo> useLibsInfos = new ArrayList<ContainerInfo>();
            List<String> resolvedManifestIdentities = new ArrayList<String>();

            // (1) No order of processing library entries is not specified.
            // (2) Only entries in the immediate library folder are processed.
            // (3) Recursively process library jar manifest class paths.

            for (Entry libEntry : libsContainer) {
                String libEntryName = libEntry.getName();
                if (!libEntryName.toLowerCase().endsWith(".jar")) {
                    continue;
                }

                Container libContainer;
                try {
                    libContainer = libEntry.adapt(Container.class); // throws UnableToAdaptException
                } catch (UnableToAdaptException e) {
                    libContainer = null; // FFDC
                    // Entity beneath EAR/lib folder which has a ".jar" extension but which could
                    // not be opened as a container.  Usually, this means an invalid JAR.
                }
                if (libContainer == null) {
                    // Since the file extension is ".jar", a null container should only
                    // occur because of an adapt exception.  The adapt call should never
                    // return null.
                    continue;
                }

                String libName = "EAR" + libsPath + libEntryName;
                ContainerInfo libInfo = new EARLibContainerInfo(libName, libContainer);

                // TODO:
                // The manifest identities of the EAR lib is *not* added initially to the
                // resolved list (but will be when the EAR lib is reached through a manifest
                // reference). That means single duplicates of EAR libs are possible.

                useLibsInfos.add(libInfo);

                try {
                    ManifestClassPathUtils.addCompleteJarEntryUrls(useLibsInfos, libEntry, resolvedManifestIdentities);
                    // throws UnableToAdaptException
                } catch (UnableToAdaptException e) {
                    // FFDC
                    // Ignore this the same as when a lib jar fails to adapt.
                }
            }

            this.libsInfos = useLibsInfos;
        }

        //

        private final Container libsContainer;

        public Container getLibsContainer() {
            return libsContainer;
        }

        //

        private final List<ContainerInfo> libsInfos;

        public List<ContainerInfo> getLibsInfos() {
            return libsInfos;
        }
    }

    /**
     * Answer information for all application libraries.
     *
     * This consists of two parts: Any shared libraries of the application,
     * and the actual application libraries. Included with the actual
     * application libraries are any libraries found by expanding the
     * manifest class paths of the actual application libraries.
     *
     * @return Information for all application libraries.
     */
    @Override
    public List<ContainerInfo> getLibraryClassesContainerInfo() {
        List<ContainerInfo> sharedLibsInfos = super.getLibraryClassesContainerInfo();

        if (appLibsInfo == null) {
            return sharedLibsInfos; // Already unmodifiable
        }
        List<ContainerInfo> appLibsInfos = appLibsInfo.getLibsInfos();
        if (appLibsInfos.isEmpty()) {
            return sharedLibsInfos; // Already unmodifiable
        }

        if (sharedLibsInfos.isEmpty()) {
            return Collections.unmodifiableList(appLibsInfos);
        }

        List<ContainerInfo> infos = new ArrayList<ContainerInfo>(appLibsInfos.size() + sharedLibsInfos.size());
        infos.addAll(appLibsInfos);
        infos.addAll(sharedLibsInfos);

        return Collections.unmodifiableList(infos);
    }

    //

    private boolean createModules() {
        String appPrefix;
        if (_tc.isDebugEnabled()) {
            appPrefix = "Application [ " + getName() + " ]: ";
        } else {
            appPrefix = null;
        }

        if (appDD != null) {
            if (appPrefix != null) {
                Tr.debug(_tc, appPrefix + "Modules specified by descriptor");
            }

            ddInitializeInOrder = (appDD.isSetInitializeInOrder() && appDD.isInitializeInOrder());

            Set<String> moduleURIs = new HashSet<String>();
            for (Module ddModule : appDD.getModules()) {
                createModule(ddModule, moduleURIs);
            }

        } else {
            if (appPrefix != null) {
                Tr.debug(_tc, appPrefix + "Modules specified by extension and by annotations");
            }
            searchForModules();
        }

        ensureUniqueModuleNames();

        if (appPrefix != null) {
            Tr.debug(_tc, appPrefix + "Modules [ " + Integer.valueOf(moduleContainerInfos.size()) + " ]");
        }

        return true;
    }

    // TODO: This is terribly ugly, but don't have clear rules
    //       on container paths: Handle paths with and without a
    //       trailing slash.

    private void searchForModules() {
        String libDir = null;
        String libDirSlash = null;
        if (appLibsInfo != null) {
            String libsPath = appLibsInfo.getLibsContainer().getPath();
            if (libsPath.length() > 0) {
                libDir = libsPath;
                if (libDir.endsWith("/")) {
                    libDir = libDir.substring(0, libDir.length() - 1);
                }
                if (!libDir.startsWith("/")) {
                    libDir = "/" + libDir;
                }
                libDirSlash = libDir + "/";
            }
        }

        searchForModules(libDir, libDirSlash, getContainer());
    }

    /**
     * Recursively search application containers for modules.
     *
     * @param libDir      The application library directory. Null if the application has no library.
     * @param libDirSlash The application library directory, with trailing slash. Null if the
     *                        application has no library.
     * @param container   The next container to scan.
     */
    private void searchForModules(String libDir, String libDirSlash, Container container) {
        for (Entry childEntry : container) {
            String childPath = childEntry.getPath();

            if ((libDir != null) && (childPath.equals(libDir) || childPath.startsWith(libDirSlash))) {
                if (_tc.isDebugEnabled()) {
                    Tr.debug(_tc, "Skipping library container [ " + childPath + " ]");
                }
                continue;
            }

            // To make any further progress, either to type the entry
            // immediately, or to process it as a sub-directory, a container
            // must be obtained.

            Container childContainer;
            try {
                childContainer = childEntry.adapt(Container.class);
            } catch (UnableToAdaptException e) {
                // error.application.library.container=
                // CWWKZ0111E: Application {0} encountered a error when accessing application library {1}: {2}
                Tr.error(_tc, "error.application.library.container", getName(), childPath, e);
                continue;
            }

            if (childContainer == null) {
                if (_tc.isDebugEnabled()) {
                    Tr.debug(_tc, "Skipping non-container [ " + childPath + " ]");
                }
            } else {
                if (childContainer.isRoot()) {
                    createModuleContainerInfo(childEntry, childPath, childContainer);
                } else {
                    searchForModules(libDir, libDirSlash, childContainer);
                }
            }
        }
    }

    private void ensureUniqueModuleNames() {
        Set<String> usedNames = new HashSet<String>();
        List<ModuleContainerInfoBase> unresolvedModuleInfos = new ArrayList<ModuleContainerInfoBase>();

        // Since EJB lookup names in java:global are sensitive to the module
        // name, we prefer to leave EJB module names alone if possible (as in
        // tWAS, botp 698359).

        // First allow EJB modules to keep their module names.  Collect non-EJB
        // modules and EJB modules with conflicting names.
        for (ModuleContainerInfoBase modInfo : moduleContainerInfos) {
            if ((modInfo instanceof EJBModuleContainerInfo) && usedNames.add(modInfo.moduleName)) {
                if (TraceComponent.isAnyTracingEnabled() && _tc.isDebugEnabled()) {
                    Tr.debug(_tc, "Keep " + modInfo.getModuleURI() + "(" + modInfo.getType() + ") as " + modInfo.moduleName);
                }
            } else {
                unresolvedModuleInfos.add(modInfo);
            }
        }

        // Assign unique module names to the remaining modules as needed.
        for (ModuleContainerInfoBase modInfo : unresolvedModuleInfos) {
            if (usedNames.add(modInfo.moduleName)) {
                if (TraceComponent.isAnyTracingEnabled() && _tc.isDebugEnabled()) {
                    Tr.debug(_tc, "Keep " + modInfo.getModuleURI() + "(" + modInfo.getType() + ") as " + modInfo.moduleName);
                }
            } else {
                // As in tWAS, prefer to use the module URI if possible.
                String newModuleName;
                if (usedNames.add(modInfo.getModuleURI())) {
                    newModuleName = modInfo.getModuleURI();
                } else {
                    int number = 2;
                    do {
                        newModuleName = modInfo.moduleName + '_' + number;
                        number++;
                    } while (!usedNames.add(newModuleName));
                }
                if (TraceComponent.isAnyTracingEnabled() && _tc.isDebugEnabled()) {
                    Tr.debug(_tc, "Update " + modInfo.getModuleURI() + " (" + modInfo.getType() + ") from " + modInfo.moduleName + " to " + newModuleName);
                }
                modInfo.setModuleName(newModuleName);
            }
        }
    }

    @Override
    public List<ModuleClassesContainerInfo> getModuleClassesContainerInfo() {
        return Collections.unmodifiableList(new ArrayList<ModuleClassesContainerInfo>(moduleContainerInfos));
    }

    private int connectorModuleCount;
    private int ejbModuleCount;

    public void createModuleContainerInfo(
                                          ModuleHandler moduleHandler,
                                          Container moduleContainer,
                                          Entry altDDEntry,
                                          String moduleURI,
                                          ModuleClassesInfoProvider moduleClassesInfo,
                                          String contextRoot, String mainClass,
                                          boolean checkForDDOrAnnotations) throws UnableToAdaptException {

        if (moduleHandler == connectorModuleHandler) {
            ConnectorModuleContainerInfo mci = new ConnectorModuleContainerInfo(moduleHandler, deployedAppServices.getModuleMetaDataExtenders("connector"), deployedAppServices.getNestedModuleMetaDataFactories("connector"), moduleContainer, altDDEntry, moduleURI, this, moduleClassesInfo);
            if (ddInitializeInOrder) {
                moduleContainerInfos.add(mci);
            } else {
                moduleContainerInfos.add(connectorModuleCount, mci);
                connectorModuleCount++;
            }
            if (_tc.isDebugEnabled()) {
                Tr.debug(_tc, "Added connector module [ " + mci.moduleName + " ]" +
                              " with module uri [ " + mci.getModuleURI() + " ]" +
                              " at [ " + moduleContainer.getPath() + " ]");
            }
        }
        if (moduleHandler == ejbModuleHandler) {
            EJBModuleContainerInfo mci = new EJBModuleContainerInfo(moduleHandler, deployedAppServices.getModuleMetaDataExtenders("ejb"), deployedAppServices.getNestedModuleMetaDataFactories("ejb"), moduleContainer, altDDEntry, moduleURI, this, moduleClassesInfo);

            if (!checkForDDOrAnnotations || mci.moduleDD != null || hasAnnotations(mci.getContainer(), EJB_ANNOTATIONS)) {
                if (ddInitializeInOrder) {
                    moduleContainerInfos.add(mci);
                } else {
                    moduleContainerInfos.add(connectorModuleCount + ejbModuleCount, mci);
                    ejbModuleCount++;
                }
                if (_tc.isDebugEnabled()) {
                    Tr.debug(_tc, "Added ejb module [ " + mci.moduleName + " ]" +
                                  " with module uri [ " + mci.getModuleURI() + " ]" +
                                  " at [ " + moduleContainer.getPath() + " ]");
                }
            }
        }
        if (moduleHandler == clientModuleHandler) {
            // If this is called from processModuleContainerInfo(...), the mainClass argument is null.
            // Also, if checkForDDOrAnnotations is true, mainClass should not be null.
            String mfMainClass = mainClass;
            if (mfMainClass == null) {
                if (checkForDDOrAnnotations) { // called from processModuleContainerInfo(...)
                    throw new IllegalArgumentException(); // internal error
                }
                mfMainClass = getMFMainClass(moduleContainer, "/META-INF/MANIFEST.MF", true);
            }
            if (mfMainClass != null) {
                ClientModuleContainerInfo mci = new ClientModuleContainerInfo(moduleHandler, deployedAppServices.getModuleMetaDataExtenders("client"), deployedAppServices.getNestedModuleMetaDataFactories("client"), moduleContainer, altDDEntry, moduleURI, this, moduleClassesInfo, mfMainClass);
                moduleContainerInfos.add(mci);
                if (_tc.isDebugEnabled()) {
                    Tr.debug(_tc, "Added client module [ " + mci.moduleName + " ]" +
                                  " with module uri [ " + mci.getModuleURI() + " ]" +
                                  " at [ " + moduleContainer.getPath() + " ]");
                }
            }
        }
        if (moduleHandler == webModuleHandler) {

            WebModuleContainerInfo mci = new WebModuleContainerInfo(moduleHandler, deployedAppServices.getModuleMetaDataExtenders("web"), deployedAppServices.getNestedModuleMetaDataFactories("web"), moduleContainer, altDDEntry, moduleURI, this, moduleClassesInfo, contextRoot);
            moduleContainerInfos.add(mci);
            if (_tc.isDebugEnabled()) {
                Tr.debug(_tc, "Added web module [ " + mci.moduleName + " ]" +
                              " with web-uri [ " + mci.getModuleURI() + " ] and context-root [ " + mci.contextRoot + " ]" +
                              " at [ " + moduleContainer.getPath() + " ]");
            }
        }
    }

    private void createModule(Module ddModule, Set<String> uniqueModuleURIs) {
        String modulePath = ddModule.getModulePath();

        final ModuleHandler moduleHandler;
        final String moduleTypeTag;
        final String contextRoot;
        if (ddModule.getModuleType() == Module.TYPE_WEB) {
            contextRoot = ContextRootUtil.getContextRoot(ddModule.getContextRoot());
            moduleHandler = webModuleHandler;
            moduleTypeTag = "web";
            if (_tc.isDebugEnabled()) {
                Tr.debug(_tc, "Application descriptor provides web module with web-uri [ " + modulePath +
                              " ] and context-root [ " + contextRoot + " ]");
            }
        } else if (ddModule.getModuleType() == Module.TYPE_EJB) {
            moduleHandler = ejbModuleHandler;
            moduleTypeTag = "ejb";
            contextRoot = null;
            if (_tc.isDebugEnabled()) {
                Tr.debug(_tc, "Application descriptor provides ejb module with module uri [ " + modulePath + " ]");
            }
        } else if ((ddModule.getModuleType() == Module.TYPE_JAVA) &&
        // Historically, we did not check for client modules, so we
        // cannot start rejecting EARs with invalid <module><java>,
        // so unlike other module types, we ignore the module
        // altogether unless a module handler is registered.
                   clientModuleHandler != null) {
            moduleHandler = clientModuleHandler;
            moduleTypeTag = "client";
            contextRoot = null;
            if (_tc.isDebugEnabled()) {
                Tr.debug(_tc, "Application descriptor provides client module with module uri [ " + modulePath + " ]");
            }
            if (appConfigDefaultClientModule != null && !appConfigDefaultClientModule.equals(modulePath)) {
                return;
            }
        } else if (ddModule.getModuleType() == Module.TYPE_CONNECTOR) {
            moduleHandler = connectorModuleHandler;
            moduleTypeTag = "connector";
            contextRoot = null;
            if (_tc.isDebugEnabled()) {
                Tr.debug(_tc, "Application descriptor provides connector module with module uri [ " + modulePath + " ]");
            }
        } else {
            // not a module type we support
            if (_tc.isDebugEnabled()) {
                Tr.debug(_tc, "Application " + getName() + ": unsupported module " + modulePath + " of type " + ddModule.getModuleType());
            }
            return;
        }

        Entry entry = getContainer().getEntry(modulePath);
        if (entry == null) {
            // error.module.locate.failed=
            // CWWKZ0117E: Application {0}: Failed to locate module {1} of type {2}
            Tr.error(_tc, "error.module.locate.failed", getName(), modulePath, moduleTypeTag);
            return;
        }

        String entryPath = entry.getPath();
        if (!uniqueModuleURIs.add(entryPath)) {
            // error.module.uri.duplicate=
            // CWWKZ0122W: Application {0} declares multiple modules with URI {1} in META-INF/application.xml
            Tr.warning(_tc, "error.module.uri.duplicate", getName(), modulePath);
            return;
        }

        Container moduleContainer;
        try {
            moduleContainer = entry.adapt(Container.class);
        } catch (UnableToAdaptException e) {
            // error.module.container=
            // CWWKZ0114E: Application {0}: Failed to obtain container for module {1} of type {2}: {3}
            Tr.error(_tc, "error.module.container", getName(), modulePath, moduleTypeTag, e);
            return;
        }

        if (moduleContainer == null) {
            // error.module.container.null=
            // CWWKZ0115E: Application {0}: Null container for candidate module {1} of type {2}
            Tr.error(_tc, "error.module.container.null", getName(), modulePath, moduleTypeTag);
            return;
        }

        if (!moduleContainer.isRoot()) {
            throw new IllegalStateException("module container should be a root");
        }

        if (moduleHandler == null) {
            if (_tc.isDebugEnabled()) {
                Tr.debug(_tc, "A module handler for " + moduleTypeTag + " modules was not configured.");
            }
            return;
        }

        String moduleURI = entryPath.substring(1);

        Entry altDDEntry = null;
        if (ddModule.getAltDD() != null && altDDEnabled) {
            altDDEntry = getContainer().getEntry(ddModule.getAltDD());
            if (altDDEntry == null) {
                // error.module.locate.failed=
                // CWWKZ0117E: Application {0}: Failed to locate module {1} of type {2}
                Tr.error(_tc, "error.module.locate.failed", getName(), ddModule.getAltDD(), moduleTypeTag);
                return;
            }
        }

        try {
            createModuleContainerInfo(moduleHandler, moduleContainer, altDDEntry, moduleURI, moduleClassesInfo, contextRoot, null, false);
        } catch (UnableToAdaptException e) {
            handleUnableToAdaptException(e, modulePath, moduleTypeTag);
        }
    }

    private void handleUnableToAdaptException(UnableToAdaptException e, String entryPath, String moduleTypeTag) {
        Throwable cause = e.getCause();
        Object causeMessage = (cause instanceof DDParser.ParseException) ? cause.getMessage() : e;
        Tr.error(_tc, "error.module.container", getName(), entryPath, moduleTypeTag, causeMessage);
    }

    private void createModuleContainerInfo(Entry entry, String entryPath, Container moduleContainer) {
        String lowerEntryPath = entryPath.toLowerCase();
        final ModuleHandler moduleHandler;
        final String moduleTypeTag;
        String mainClass = null;

        if (lowerEntryPath.endsWith(".war")) {
            moduleHandler = webModuleHandler;
            moduleTypeTag = "web";
        } else if (lowerEntryPath.endsWith(".jar")) {
            // Check for a client module first!
            boolean hasAppClientXML = moduleContainer.getEntry("/META-INF/application-client.xml") != null;
            mainClass = getMFMainClass(moduleContainer, entryPath, hasAppClientXML);
            if (mainClass != null) {
                if (appConfigDefaultClientModule == null || appConfigDefaultClientModule.equals(entryPath.substring(1))) {
                    moduleHandler = clientModuleHandler;
                    moduleTypeTag = "client";
                } else {
                    moduleHandler = null;
                    moduleTypeTag = "unknown";
                }
            } else if (hasAppClientXML) {
                moduleHandler = null;
                moduleTypeTag = "unknown";
            } else {
                moduleHandler = ejbModuleHandler;
                moduleTypeTag = "ejb";
            }

        } else if (lowerEntryPath.endsWith(".rar")) {
            moduleHandler = connectorModuleHandler;
            moduleTypeTag = "connector";
        } else {
            // not a file extension we support, either just a regular file entry or
            // a subfolder
            moduleHandler = null;
            moduleTypeTag = "unknown";
        }

        if (moduleHandler == null) {
            if (_tc.isDebugEnabled()) {
                Tr.debug(_tc, "Skipping non-module container [ " + entryPath + " ]");
            }
            return;
        }

        String moduleURI = entryPath.substring(1);

        try {
            createModuleContainerInfo(moduleHandler, moduleContainer, null, moduleURI, moduleClassesInfo, null, mainClass, true);
        } catch (UnableToAdaptException e) {
            handleUnableToAdaptException(e, entryPath, moduleTypeTag);
        }
    }

    /**
     * Return the Main-Class from the MANIFEST.MF.
     *
     * @param appEntryContainer The root container.
     * @param entryPath         The path of the mainfest file.
     * @param required          true if current module is known to be a client module, and false otherwise
     *
     * @return The main class or null depending on the boolean value 'required'.
     */
    protected String getMFMainClass(Container appEntryContainer, String entryPath, boolean required) {
        String mfMainClass = null;
        try {
            String entry = "/META-INF/MANIFEST.MF";
            Entry manifestEntry = appEntryContainer.getEntry(entry);
            if (manifestEntry != null) {
                InputStream is = null;
                try {
                    is = manifestEntry.adapt(InputStream.class);
                    // "is" is null when MANIFEST.MF is a directory
                    if (is == null) {
                        throw new FileNotFoundException(entry);
                    }
                    Manifest manifest = new Manifest(is);
                    mfMainClass = manifest.getMainAttributes().getValue(Attributes.Name.MAIN_CLASS);
                    if ("".equals(mfMainClass)) {
                        mfMainClass = null;
                    }
                } finally {
                    if (is != null) {
                        try {
                            is.close();
                        } catch (IOException io) {
                            /* do nothing */
                        }
                    }
                }
            }
            if (mfMainClass == null && required) {
                Tr.error(_tc, "error.module.manifest.read.no.mainclass", getName(), entryPath);
            }
        } catch (IOException ioe) {
            Tr.error(_tc, "error.module.manifest.read.failed", getName(), entryPath, ioe);
        } catch (UnableToAdaptException uae) {
            Tr.error(_tc, "error.module.manifest.read.failed", getName(), entryPath, uae);
        }
        return mfMainClass;
    }

    public boolean hasModules() {
        return !moduleContainerInfos.isEmpty();
    }

    @Override
    protected void registerApplicationMBean() {
        if (appMBeanRuntime != null) {
            List<ModuleInfo> deployedModuleInfos = new ArrayList<ModuleInfo>();
            for (DeployedModuleInfoImpl moduleInfoImpl : modulesDeployed) {
                deployedModuleInfos.add(moduleInfoImpl.getModuleInfo());
            }
            mbeanServiceReg = appMBeanRuntime.registerApplicationMBean(getName(), getContainer(), appDD != null ? "/META-INF/application.xml" : null, deployedModuleInfos);
        }
    }

    @Override
    protected void deregisterApplicationMBean() {
        if (mbeanServiceReg != null) {
            ServiceRegistration<?> mbeanReg = mbeanServiceReg;
            mbeanServiceReg = null;

            mbeanReg.unregister();
        }
    }

    /*
     * We need to listen to the /WEB-INF directory on all of the WAR modules and /META-INF/application.xml in the EAR container.
     *
     * We only discover the WARs when we construct the earAppInfo so build this now, we'll pass it back to the app manager so that it can be pass through to the install method
     * to save us some time later on.
     *
     * First though we go to interpreted as this knows when a container is a WAR
     */
    public ApplicationMonitoringInformation createApplicationMonitoringInformation() {
        // TODO: Move this into the info objects; each info type should
        //       handle the cases that they care about.
        //
        // TODO: An application.xml will not always be present.

        Collection<Notification> notificationsToMonitor = new HashSet<Notification>();
        if (appDD != null) {
            notificationsToMonitor.add(new DefaultNotification(getContainer(), "/META-INF/application.xml"));
        }

        if (appLibsInfo != null) {
            String libDirectory = appLibsInfo.getLibsContainer().getPath();
            if (!libDirectory.startsWith("/")) {
                libDirectory = "/" + libDirectory;
            }
            notificationsToMonitor.add(new DefaultNotification(getContainer(), libDirectory));
        }

        // TODO: Need to consider cases of a web module with no descriptor,
        //       and of fragment jars and fragment descriptors.

        for (ModuleContainerInfoBase modInfo : moduleContainerInfos) {
            if (modInfo instanceof WebModuleContainerInfo) {
                notificationsToMonitor.add(new DefaultNotification(modInfo.getContainer(), "/WEB-INF"));
            } else if (modInfo instanceof ConnectorModuleContainerInfo) {
                notificationsToMonitor.add(new DefaultNotification(modInfo.getContainer(), "/META-INF"));
            }

        }

        for (ContainerInfo classpathContainerInfo : getClasspathContainerInfos()) {
            notificationsToMonitor.add(new DefaultNotification(classpathContainerInfo.getContainer(), "/"));
        }

        // Monitor the root of the ear file, if we expanded the ear.  Ideally we'd monitor the same
        // locations in the ear file that we are monitoring above in the expanded directory.
        if (preExpansionAppContainer != null) {
            notificationsToMonitor.add(new DefaultNotification(preExpansionAppContainer, "/"));
        }

        return new DefaultApplicationMonitoringInformation(notificationsToMonitor, true);
    }

    //

    private List<ContainerInfo> classpathContainerInfos;

    private List<ContainerInfo> getClasspathContainerInfos() {
        if (classpathContainerInfos == null) {
            List<ContainerInfo> containerInfos = new ArrayList<ContainerInfo>();

            addEJBJarContainerInfos(containerInfos);
            addEARLibContainerInfos(containerInfos);
            addConnectorContainerInfos(containerInfos);
            checkClientJarContainerInfos(containerInfos);

            classpathContainerInfos = containerInfos;
        }
        return classpathContainerInfos;
    }

    private void addEJBJarContainerInfos(List<ContainerInfo> classpathContainerInfos) {
        try {
            for (ModuleContainerInfoBase modInfo : moduleContainerInfos) {
                if (modInfo instanceof EJBModuleContainerInfo) {
                    classpathContainerInfos.addAll(modInfo.getClassesContainerInfo());
                }
            }
        } catch (Throwable th) {
            Tr.error(_tc, "error.application.libraries", getName(), th);
        }
    }

    private void addEARLibContainerInfos(List<ContainerInfo> classpathContainerInfos) {
        if (this.appLibsInfo != null) {
            classpathContainerInfos.addAll(this.appLibsInfo.getLibsInfos());
        }
    }

    private void checkClientJarContainerInfos(List<ContainerInfo> classpathContainerInfos) {
        boolean found = false;
        try {
            for (ModuleContainerInfoBase modInfo : moduleContainerInfos) {
                if (modInfo instanceof ClientModuleContainerInfo) {
                    if (appConfigDefaultClientModule == null) {
                        if (found) {
                            Tr.error(_tc, "error.module.no.defaultclient");
                        }
                        found = true;
                    } else if (appConfigDefaultClientModule.equals(modInfo.getModuleURI())) {
                        if (found) {
                            Tr.error(_tc, "error.module.dup.client", appConfigDefaultClientModule);
                        }
                        found = true;
                    }
                }
            }
            if (!found && appConfigDefaultClientModule != null) {
                Tr.error(_tc, "error.module.client.notfound", appConfigDefaultClientModule);
            }
        } catch (Throwable th) {
            Tr.error(_tc, "error.application.libraries", getName(), th);
        }
    }

    private void addConnectorContainerInfos(List<ContainerInfo> classpathContainerInfos) {
        try {
            for (ModuleContainerInfoBase modInfo : moduleContainerInfos) {
                if (modInfo instanceof ConnectorModuleContainerInfo) {
                    classpathContainerInfos.addAll(modInfo.getClassesContainerInfo());
                }
            }
        } catch (Throwable th) {
            Tr.error(_tc, "error.application.libraries", getName(), th);
        }
    }

    /** Annotations which identify an application jar as an EJB jar. */
    private static final List<String> EJB_ANNOTATIONS;

    static {
        EJB_ANNOTATIONS = new ArrayList<String>();
        EJB_ANNOTATIONS.add("javax.ejb.MessageDriven");
        EJB_ANNOTATIONS.add("javax.ejb.Stateless");
        EJB_ANNOTATIONS.add("javax.ejb.Stateful");
        EJB_ANNOTATIONS.add("javax.ejb.Singleton");
    }

    private String getFullPath(Container useContainer) {
        StringBuilder pathBuilder = new StringBuilder();

        try {
            Entry entry;
            while ((entry = useContainer.adapt(Entry.class)) != null) {
                // 'adapt' throws UnableToAdaptException
                pathBuilder.insert(0, entry.getPath());
                useContainer = entry.getRoot();
            }
        } catch (UnableToAdaptException e) {
            // FFDC
            return null;
        }

        return pathBuilder.toString();
    }

    /**
     * Tell if a module has annotations. This is currently used to type JAR files
     * of applications which do not have a descriptor. A JAR file is typed as an
     * EJB JAR file if the JAR contains EJB annotations.
     *
     * The detection is done on the immediate classes of the JAR. Detection does
     * not examine any inherited information.
     *
     * Since inherited information is not used, the module annotations data structure
     * is only given path information to the JAR itself. The module class path is
     * never given links to external information.
     *
     * @param moduleContainer        The module container to test for annotations.
     * @param useAnnotationTypeNames The names of annotations to detect in the
     *                                   module container.
     *
     * @return True or false telling if any of the annotations is present as an
     *         immediate annotation of a class of the module.
     */
    private boolean hasAnnotations(Container moduleContainer, List<String> useAnnotationTypeNames) {
        if (AnnotationsBetaHelper.getLibertyBeta()) {
            return hasAnnotationsPostBeta(moduleContainer, useAnnotationTypeNames);
        } else {
            return hasAnnotationsPreBeta(moduleContainer, useAnnotationTypeNames);
        }
    }

    private boolean hasAnnotationsPreBeta(Container moduleContainer, List<String> useAnnotationTypeNames) {
        com.ibm.ws.container.service.annotations.ContainerAnnotations ca = null;
        try {
            ca = moduleContainer.adapt(com.ibm.ws.container.service.annotations.ContainerAnnotations.class);
        } catch (UnableToAdaptException e) {
            // error.module.annotation.targets=
            // CWWKZ0121E: Application {0}: Failed to access annotations for module {1} of type {2}: {3}
            Tr.error(_tc, "error.module.class.source", getName(), moduleContainer.getPath(), "EJB", e);

            if (_tc.isDebugEnabled()) {
                Tr.debug(_tc, "Selected [ 0 ] EJB annotation cases: Error obtaining annotation targets");
            }
        }

        if (ca != null && ca.hasSpecifiedAnnotations(useAnnotationTypeNames, applicationInformation.getUseJandex())) {
            if (_tc.isDebugEnabled()) {
                Tr.debug(_tc, "Select EJB module [ " + moduleContainer.getPath() + " ]: EJB annotations were detected:");
            }
            return true;
        } else {
            if (_tc.isDebugEnabled()) {
                Tr.debug(_tc, "Reject EJB module [ " + moduleContainer.getPath() + " ]: No descriptor and no EJB annotations");
            }
            return false;
        }
    }

    private boolean hasAnnotationsPostBeta(Container moduleContainer, List<String> annotationClassNames) {
        String methodName = "hasAnnotations";

        // Use the application name from the application configuration, which
        // is implemented to obtain the name from the application information.
        // That is either the explicit name from the server configuration, or
        // the name value obtained from the application location.
        //
        // The application name as supplied by the application descriptor is
        // not used.

        String appName = getName();

        String modImmediatePath = moduleContainer.getPath();
        String modFullPath = getFullPath(moduleContainer);

        if (_tc.isDebugEnabled()) {
            Tr.debug(_tc, methodName + ": AppName [ " + appName + " ]");
            Tr.debug(_tc, methodName + ": ModImmediatePath [ " + modImmediatePath + " ]");
            Tr.debug(_tc, methodName + ": ModFullPath [ " + modFullPath + " ]");
        }

        if (modFullPath == null) {
            if (_tc.isDebugEnabled()) {
                Tr.debug(_tc, methodName + ": Failed to obtain module path");
            }
        } else if (modFullPath.isEmpty()) {
            modFullPath = null;
        }

        com.ibm.ws.container.service.annocache.ContainerAnnotations containerAnnotations;

        try {
            containerAnnotations = moduleContainer.adapt(com.ibm.ws.container.service.annocache.ContainerAnnotations.class);
        } catch (UnableToAdaptException e) {
            // CWWKZ0121E: Application {0}: Failed to access annotations for module {1} of type {2}: {3}
            Tr.error(_tc, "error.module.class.source", appName, modImmediatePath, "EJB", e);

            if (_tc.isDebugEnabled()) {
                Tr.debug(_tc, methodName + ": [ false ]: Error obtaining annotations");
            }
            return false;
        }

        if (containerAnnotations == null) {
            if (_tc.isDebugEnabled()) {
                Tr.debug(_tc, methodName + ": [ false ]: Unexpected null annotations");
            }
            return false;
        }

        containerAnnotations.setAppName(appName);
        containerAnnotations.setModName(modFullPath);

        containerAnnotations.setUseJandex(applicationInformation.getUseJandex());

        // A class loader is *not* set to the annotations.
        //
        // Normally, a class loader is required to be set on the annotations.
        // In this case, since the query does not use inheritance information,
        // the class loader is not necessary.

        boolean selected = containerAnnotations.hasSpecifiedAnnotations(annotationClassNames);

        if (_tc.isDebugEnabled()) {
            Tr.debug(_tc, methodName + ": [ " + appName + " ]" +
                          " [ " + modFullPath + " ]" +
                          " [ " + Boolean.valueOf(selected) + " ]");
        }
        return selected;
    }

    //

    /**
     * Obtain a class loader for a module. Class loader details depend on the module type:
     * EJB and connector modules use the application class loader. Web and client modules
     * have more details class loaders.
     *
     * @param moduleInfo        Information for the module which is being created.
     * @param moduleClassesInfo ...
     *
     * @return A class loader for the module.
     */
    @Override
    public ClassLoader createModuleClassLoader(ModuleInfo moduleInfo, List<ContainerInfo> moduleClassesInfos) {
        if (moduleInfo instanceof WebModuleInfo) {
            return createModuleChildClassLoader(WEB_MODULE_DOMAIN, moduleInfo, moduleClassesInfos);
        } else if (moduleInfo instanceof EJBModuleInfo) {
            return createEJBModuleClassLoader((EJBModuleInfo) moduleInfo);
        } else if (moduleInfo instanceof ClientModuleInfo) {
            return createModuleChildClassLoader(CLIENT_MODULE_DOMAIN, moduleInfo, moduleClassesInfos);
        } else if (moduleInfo instanceof ConnectorModuleInfo) {
            return createConnectorModuleClassLoader((ConnectorModuleInfo) moduleInfo);
        } else {
            return null;
        }
    }

    /**
     * Obtain a class loader for an EJB module. Answer the application
     * class loader.
     *
     * @param ejbModuleInfo EJB module information.
     *
     * @return A class loader for an EJB module.
     */
    private ClassLoader createEJBModuleClassLoader(EJBModuleInfo ejbModuleInfo) {
        return getAppClassLoader();
    }

    /**
     * Obtain a class loader for a connector (RAR) module. Answer the application
     * class loader.
     *
     * @param connectorModuleInfo Connector module information.
     *
     * @return A class loader for a connector module.
     */
    private ClassLoader createConnectorModuleClassLoader(ConnectorModuleInfo connectorModuleInfo) {
        return getAppClassLoader();
    }

    /** Control parameter: Assign the module class loader an ID in the domain of web module class loaders. */
    private static final String WEB_MODULE_DOMAIN = "WebModule";

    /** Control parameter: Assign the module class loader an ID in the domain of client module class loaders. */
    private static final String CLIENT_MODULE_DOMAIN = "ClientModule";

    /**
     * Create a class loader for a web or client module.
     *
     * The new module class loader delegates to the application class loader.
     *
     * Creation of the module class loader forces the assignment of the ID of the
     * class loader of the enclosing application.
     *
     * @param type                 The domain of the module. Either "WebModule" or "ClientModule".
     * @param moduleInfo           The module info.
     * @param classesContainerInfo classes container information for the module.
     *
     * @return The class loader for the child module.
     */
    private ClassLoader createModuleChildClassLoader(
                                                     String moduleDomain, ModuleInfo moduleInfo,
                                                     List<ContainerInfo> classesContainerInfo) {

        String moduleName = moduleInfo.getName();
        String moduleUri = moduleInfo.getURI();

        String modulePrefix;
        if (_tc.isDebugEnabled()) {
            modulePrefix = moduleDomain + " [ " + moduleName + " ]: ";
        } else {
            modulePrefix = null;
        }

        List<Container> classesContainers = new ArrayList<Container>(classesContainerInfo.size());
        for (ContainerInfo containerInfo : classesContainerInfo) {
            classesContainers.add(containerInfo.getContainer());
        }

        // Force the app class loader ID to be assigned before assigning module
        // class loader IDs.
        //
        // Assuming a sequential ID assignment algorithm, the application class
        // loader ID will be less than all module class loader IDs.

        ClassLoaderIdentity appClassLoaderId = getAppClassLoaderId();
        if (modulePrefix != null) {
            Tr.debug(_tc, modulePrefix + "Application class loader ID [ " + appClassLoaderId + " ]");
        }

        ClassLoaderIdentity moduleClassLoaderId = classLoadingService.createIdentity(moduleDomain, getName() + "#" + moduleUri);
        if (modulePrefix != null) {
            Tr.debug(_tc, modulePrefix + "Module class loader ID [ " + moduleClassLoaderId + " ]");
        }

        ClassLoaderConfiguration moduleClassLoaderConfig = classLoadingService.createClassLoaderConfiguration();
        moduleClassLoaderConfig.setId(moduleClassLoaderId);
        moduleClassLoaderConfig.setParentId(appClassLoaderId);

        moduleClassLoaderConfig.setDelegateToParentAfterCheckingLocalClasspath(isDelegateLast);
        moduleClassLoaderConfig.setIncludeAppExtensions(true);
        moduleClassLoaderConfig.setProtectionDomain(protectionDomain);

        if (modulePrefix != null) {
            Tr.debug(_tc, modulePrefix + "isDelegateLast [ " + Boolean.valueOf(isDelegateLast) + " ]");
            Tr.debug(_tc, modulePrefix + "includeAppExtensions [ true ]");
            Tr.debug(_tc, modulePrefix + "protectionDomain [ " + protectionDomain + " ]");
        }

        ClassLoader moduleClassLoader = classLoadingService.createChildClassLoader(classesContainers, moduleClassLoaderConfig);
        associateClassLoaderWithApp(moduleClassLoader);

        if (modulePrefix != null) {
            Tr.debug(_tc, modulePrefix + " Class loader [ " + moduleClassLoader + " ]");
        }
        return moduleClassLoader;
    }
}
