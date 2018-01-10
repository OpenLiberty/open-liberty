/*******************************************************************************
 * Copyright (c) 2012, 2015 IBM Corporation and others.
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
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.osgi.framework.ServiceRegistration;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.app.manager.module.DeployedAppInfo;
import com.ibm.ws.app.manager.module.DeployedAppMBeanRuntime;
import com.ibm.ws.app.manager.module.internal.ContextRootUtil;
import com.ibm.ws.app.manager.module.internal.DeployedAppInfoBase;
import com.ibm.ws.app.manager.module.internal.DeployedModuleInfoImpl;
import com.ibm.ws.app.manager.module.internal.ModuleHandler;
import com.ibm.ws.app.manager.module.internal.ModuleInfoUtils;
import com.ibm.ws.container.service.annotations.ContainerAnnotations;
import com.ibm.ws.container.service.app.deploy.ClientModuleInfo;
import com.ibm.ws.container.service.app.deploy.ConnectorModuleInfo;
import com.ibm.ws.container.service.app.deploy.ContainerInfo;
import com.ibm.ws.container.service.app.deploy.EJBModuleInfo;
import com.ibm.ws.container.service.app.deploy.ManifestClassPathUtils;
import com.ibm.ws.container.service.app.deploy.ModuleClassesContainerInfo;
import com.ibm.ws.container.service.app.deploy.ModuleInfo;
import com.ibm.ws.container.service.app.deploy.WebModuleClassesInfo;
import com.ibm.ws.container.service.app.deploy.WebModuleInfo;
import com.ibm.ws.container.service.app.deploy.extended.ExtendedApplicationInfo;
import com.ibm.ws.container.service.metadata.extended.ModuleMetaDataExtender;
import com.ibm.ws.container.service.metadata.extended.NestedModuleMetaDataFactory;
import com.ibm.ws.javaee.dd.app.Application;
import com.ibm.ws.javaee.dd.app.Module;
import com.ibm.ws.javaee.ddmodel.DDParser.ParseException;
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

    private static final class EARLibDeploymentInfo {
        protected final Container container;
        protected final List<ContainerInfo> classesContainerInfo = new ArrayList<ContainerInfo>();

        public EARLibDeploymentInfo(Container libDirContainer) throws UnableToAdaptException {
            this.container = libDirContainer;
            final String libDirName = libDirContainer.getPath();
            ArrayList<String> resolved = new ArrayList<String>();
            for (Entry entry : libDirContainer) {
                final String jarEntryName = entry.getName();
                if (jarEntryName.toLowerCase().endsWith(".jar")) {
                    final Container jarContainer = entry.adapt(Container.class);
                    if (jarContainer != null) {
                        ContainerInfo containerInfo = new ContainerInfo() {
                            @Override
                            public Type getType() {
                                return Type.EAR_LIB;
                            }

                            @Override
                            public String getName() {
                                return "EAR" + libDirName + jarEntryName;
                            }

                            @Override
                            public Container getContainer() {
                                return jarContainer;
                            }
                        };
                        classesContainerInfo.add(containerInfo);
                        ManifestClassPathUtils.addCompleteJarEntryUrls(classesContainerInfo, entry, resolved);
                    }
                }
            }
        }

        public Container getContainer() {
            return this.container;
        }

        public List<ContainerInfo> getClassesContainerInfo() {
            return this.classesContainerInfo;
        }
    }

    private final ModuleHandler webModuleHandler;
    private final ModuleHandler ejbModuleHandler;
    private final ModuleHandler clientModuleHandler;
    private final ModuleHandler connectorModuleHandler;
    private final Map<String, List<ModuleMetaDataExtender>> moduleMetaDataExtenders;
    private final Map<String, List<NestedModuleMetaDataFactory>> nestedModuleMetaDataFactories;
    private final DeployedAppMBeanRuntime appMBeanRuntime;
    private final Application applicationDD;
    private final String preferredName;
    private final EARLibDeploymentInfo earLibDeploymentInfo;
    private final ClassLoaderIdentity appClassLoaderId;
    private final boolean altDDSupportEnabled;
    private final Container preExpansionEarContainer;

    private List<ContainerInfo> classpathContainerInfos;
    private ClassLoader appClassLoader;
    private ProtectionDomain protectionDomain;
    private ServiceRegistration<?> mbeanServiceReg;

    private static final String CONTEXT_ROOT = "context-root";
    private static final String DEFAULT_CLIENT_MODULE = "defaultClientModule";

    boolean initializeInOrder;
    private int connectorModuleCount;
    private int ejbModuleCount;

    private final String defaultClientModule;

    EARDeployedAppInfo(ApplicationInformation<DeployedAppInfo> applicationInformation,
                       Application applicationDD,
                       EARDeployedAppInfoFactoryImpl factory,
                       Container preExpansionEarContainer) throws UnableToAdaptException {
        super(applicationInformation, factory);
        this.webModuleHandler = factory.webModuleHandler;
        this.ejbModuleHandler = factory.ejbModuleHandler;
        this.clientModuleHandler = factory.clientModuleHandler;
        this.connectorModuleHandler = factory.connectorModuleHandler;
        this.moduleMetaDataExtenders = factory.getModuleMetaDataExtenders();
        this.nestedModuleMetaDataFactories = factory.getNestedModuleMetaDataFactories();
        this.appMBeanRuntime = factory.appMBeanRuntime;
        this.applicationDD = applicationDD;
        this.altDDSupportEnabled = factory.platformVersion.compareTo(JavaEEVersion.VERSION_7_0) >= 0;
        this.preExpansionEarContainer = preExpansionEarContainer;

        String appName = applicationDD == null ? null : applicationDD.getApplicationName();
        if (appName == null) {
            appName = ModuleInfoUtils.getModuleURIFromLocation(applicationInformation.getLocation());
            if (appName.endsWith(".ear")) {
                appName = appName.substring(0, appName.length() - ".ear".length());
            }
        }
        this.preferredName = appName;

        this.earLibDeploymentInfo = createEARLibDeploymentInfo();

        appClassLoaderId = classLoadingService.createIdentity("EARApplication", getName());

        // When there are more than one client modules in the application (.ear), the client module to process
        // can be selected by using <enterpriseApplication ... defaultClientModule="module_name" .../>
        defaultClientModule = (String) applicationInformation.getConfigProperty(DEFAULT_CLIENT_MODULE);

        findModules();

        if (applicationInformation.getConfigProperty(CONTEXT_ROOT) != null) {
            Tr.warning(_tc, "warning.context.root.not.used", applicationInformation.getConfigProperty(CONTEXT_ROOT), preferredName);
        }
    }

    private EARLibDeploymentInfo createEARLibDeploymentInfo() throws UnableToAdaptException {
        final String libDir;
        if (applicationDD != null && applicationDD.getLibraryDirectory() != null) {
            libDir = applicationDD.getLibraryDirectory();
            if (libDir.length() == 0) {
                return null;
            }
        } else {
            libDir = "lib";
        }
        Entry libDirEntry = getContainer().getEntry(libDir);
        if (libDirEntry == null) {
            return null;
        }
        Container libDirContainer = libDirEntry.adapt(Container.class);
        return new EARLibDeploymentInfo(libDirContainer);
    }

    @Override
    public List<ContainerInfo> getLibraryClassesContainerInfo() {
        List<ContainerInfo> containerInfoList = super.getLibraryClassesContainerInfo();
        if (earLibDeploymentInfo != null) {
            List<ContainerInfo> earLibContainerInfoList = earLibDeploymentInfo.getClassesContainerInfo();
            if (!earLibContainerInfoList.isEmpty()) {
                if (containerInfoList.isEmpty()) {
                    containerInfoList = Collections.unmodifiableList(earLibContainerInfoList);
                } else {
                    List<ContainerInfo> mergedList = new ArrayList<ContainerInfo>();
                    mergedList.addAll(earLibContainerInfoList);
                    mergedList.addAll(containerInfoList);
                    containerInfoList = Collections.unmodifiableList(mergedList);
                }
            }
        }
        return containerInfoList;
    }

    @Override
    public List<ModuleClassesContainerInfo> getModuleClassesContainerInfo() {
        List<ModuleClassesContainerInfo> moduleClassesContainerInfos = new ArrayList<ModuleClassesContainerInfo>();
        for (ModuleContainerInfoBase mci : moduleContainerInfos) {
            moduleClassesContainerInfos.add(mci);
        }
        return Collections.unmodifiableList(moduleClassesContainerInfos);
    }

    @Override
    public ClassLoader createAppClassLoader() {
        return getApplicationClassLoader();
    }

    @Override
    public ClassLoader createModuleClassLoader(ModuleInfo moduleInfo, List<ContainerInfo> moduleClassesContainers) {
        if (moduleInfo instanceof WebModuleInfo) {
            return createModuleChildClassLoader("WebModule", moduleInfo, moduleClassesContainers);
        } else if (moduleInfo instanceof EJBModuleInfo) {
            return createEJBModuleClassLoader(moduleInfo);
        } else if (moduleInfo instanceof ClientModuleInfo) {
            return createModuleChildClassLoader("ClientModule", moduleInfo, moduleClassesContainers);
        } else if (moduleInfo instanceof ConnectorModuleInfo) {
            return createConnectorModuleClassLoader(moduleInfo);
        } else {
            return null;
        }
    }

    private ClassLoader createEJBModuleClassLoader(ModuleInfo moduleInfo) {
        return getApplicationClassLoader();
    }

    private ClassLoader createConnectorModuleClassLoader(ModuleInfo moduleInfo) {
        return getApplicationClassLoader();
    }

    private ClassLoader getApplicationClassLoader() {
        if (appClassLoader == null) {
            createApplicationClassLoader();
        }
        return appClassLoader;
    }

    private ClassLoaderIdentity getApplicationClassLoaderId() {
        // if someone is asking for the app class loader id using this method they
        // are about to use it as the parent id for a child class loader, so we want
        // to ensure that if we have not created the app class loader yet that we do
        // so now
        getApplicationClassLoader();
        return appClassLoaderId;
    }

    @Override
    protected ExtendedApplicationInfo createApplicationInfo() {
        Container libDirContainer = earLibDeploymentInfo != null ? earLibDeploymentInfo.getContainer() : null;
        return appInfoFactory.createEARApplicationInfo(getName(),
                                                       preferredName,
                                                       getContainer(),
                                                       this,
                                                       getConfigHelper(),
                                                       libDirContainer,
                                                       this);
    }

    private boolean findModules() {
        if (_tc.isDebugEnabled()) {
            Tr.debug(_tc, "Application [ " + getName() + " ]");
        }

        if (applicationDD != null) {
            if (_tc.isDebugEnabled()) {
                Tr.debug(_tc, "Modules selected according to the application descriptor");
            }

            initializeInOrder = (applicationDD.isSetInitializeInOrder() && applicationDD.isInitializeInOrder());

            final Set<String> uniqueModuleURIs = new HashSet<String>();
            for (Module ddModule : applicationDD.getModules()) {
                processModuleContainerInfo(ddModule, uniqueModuleURIs);
            }

        } else {
            if (_tc.isDebugEnabled()) {
                Tr.debug(_tc, "Collect modules using deployment descriptors and/or annotations");
            }

            searchForModules();
        }

        // Now that all modules have been found, ensure they have non-conflicting modules names.
        ensureUniqueModuleNames();

        if (_tc.isDebugEnabled()) {
            Tr.debug(_tc, "Application [ " + getName() + " ] selects [ " + Integer.valueOf(moduleContainerInfos.size()) + " ] modules");
        }

        return true;
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
            if (modInfo instanceof EJBModuleContainerInfo && usedNames.add(modInfo.moduleName)) {
                if (TraceComponent.isAnyTracingEnabled() && _tc.isDebugEnabled()) {
                    Tr.debug(_tc, "keeping " + modInfo.getModuleURI() + " (" + modInfo.getType() + ") as " + modInfo.moduleName);
                }
            } else {
                unresolvedModuleInfos.add(modInfo);
            }
        }

        // Assign unique module names to the remaining modules as needed.
        for (ModuleContainerInfoBase modInfo : unresolvedModuleInfos) {
            if (usedNames.add(modInfo.moduleName)) {
                if (TraceComponent.isAnyTracingEnabled() && _tc.isDebugEnabled()) {
                    Tr.debug(_tc, "keeping " + modInfo.getModuleURI() + " (" + modInfo.getType() + ") as " + modInfo.moduleName);
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
                    Tr.debug(_tc, "updating " + modInfo.getModuleURI() + " (" + modInfo.getType() + ") from " + modInfo.moduleName + " to " + newModuleName);
                }
                modInfo.setModuleName(newModuleName);
            }
        }
    }

    /**
     * @param altDDEntry
     * @param mainClass the MANIFEST.MF Main-Class for client modules if checkForDDOrAnnotations, or null otherwise
     */
    public void createModuleContainerInfo(ModuleHandler moduleHandler,
                                          Container moduleContainer, Entry altDDEntry, String moduleURI,
                                          ModuleClassesInfoProvider moduleClassesInfo,
                                          String contextRoot, String mainClass,
                                          boolean checkForDDOrAnnotations) throws UnableToAdaptException {
        if (moduleHandler == connectorModuleHandler) {
            ConnectorModuleContainerInfo mci = new ConnectorModuleContainerInfo(moduleHandler, moduleMetaDataExtenders.get("connector"), nestedModuleMetaDataFactories.get("connector"), moduleContainer, altDDEntry, moduleURI, moduleClassesInfo);
            if (initializeInOrder) {
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
            EJBModuleContainerInfo mci = new EJBModuleContainerInfo(moduleHandler, moduleMetaDataExtenders.get("ejb"), nestedModuleMetaDataFactories.get("ejb"), moduleContainer, altDDEntry, moduleURI, moduleClassesInfo);
            if (!checkForDDOrAnnotations || mci.moduleDD != null || hasSpecifiedAnnotations(mci.getContainer(), EJB_ANNOTATIONS)) {
                if (initializeInOrder) {
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
                ClientModuleContainerInfo mci = new ClientModuleContainerInfo(moduleHandler, moduleMetaDataExtenders.get("client"), nestedModuleMetaDataFactories.get("client"), moduleContainer, altDDEntry, moduleURI, moduleClassesInfo, mfMainClass);
                moduleContainerInfos.add(mci);
                if (_tc.isDebugEnabled()) {
                    Tr.debug(_tc, "Added client module [ " + mci.moduleName + " ]" +
                                  " with module uri [ " + mci.getModuleURI() + " ]" +
                                  " at [ " + moduleContainer.getPath() + " ]");
                }
            }
        }
        if (moduleHandler == webModuleHandler) {

            WebModuleContainerInfo mci = new WebModuleContainerInfo(moduleHandler, moduleMetaDataExtenders.get("web"), nestedModuleMetaDataFactories.get("web"), moduleContainer, altDDEntry, moduleURI, moduleClassesInfo, contextRoot);
            moduleContainerInfos.add(mci);
            if (_tc.isDebugEnabled()) {
                Tr.debug(_tc, "Added web module [ " + mci.moduleName + " ]" +
                              " with web-uri [ " + mci.getModuleURI() + " ] and context-root [ " + mci.contextRoot + " ]" +
                              " at [ " + moduleContainer.getPath() + " ]");
            }
        }
    }

    private void processModuleContainerInfo(Module ddModule, Set<String> uniqueModuleURIs) {
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
            if (defaultClientModule != null && !defaultClientModule.equals(modulePath)) {
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
        if (ddModule.getAltDD() != null && altDDSupportEnabled) {
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

    private void searchForModules() {

        // TODO: This is terribly ugly, but don't have clear rules
        //       on what values are allowed, so support all cases.

        String libDir = null;
        String libDirSlash = null;
        if (earLibDeploymentInfo != null) {
            String path = earLibDeploymentInfo.getContainer().getPath();
            if (path.length() > 0) {
                libDir = path;
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

    private void searchForModules(String libDir, String libDirSlash, Container container) {
        for (Entry entry : container) {
            String entryPath = entry.getPath();

            // File contains and Archive containers seem to expose two entirely
            // different iteration styles.  Until the differences are worked out,
            // handle both cases of iteration across all entries (the Archive
            // strategy) and iteration one directory at a time (the File strategy).

            if (libDir != null && (entryPath.equals(libDir) || entryPath.startsWith(libDirSlash))) {
                if (_tc.isDebugEnabled()) {
                    Tr.debug(_tc, "Skipping library container [ " + entryPath + " ]");
                }
                continue;
            }

            // To make any further progress, either to type the entry
            // immediately, or to process it as a sub-directory, a container
            // must be obtained.

            Container entryContainer;
            try {
                entryContainer = entry.adapt(Container.class);
            } catch (UnableToAdaptException e) {
                // error.application.library.container=
                // CWWKZ0111E: Application {0} encountered a error when accessing application library {1}: {2}
                Tr.error(_tc, "error.application.library.container", getName(), entryPath, e);
                continue;
            }

            if (entryContainer == null) {
                if (_tc.isDebugEnabled()) {
                    Tr.debug(_tc, "Skipping non-container [ " + entryPath + " ]");
                }
            } else {
                if (entryContainer.isRoot()) {
                    createModuleContainerInfo(entry, entryPath, entryContainer);
                } else {
                    // Stepping into a subdirectory.
                    searchForModules(libDir, libDirSlash, entryContainer);
                }
            }
        }
    }

    private void handleUnableToAdaptException(UnableToAdaptException e, String entryPath, String moduleTypeTag) {
        Throwable cause = e.getCause();
        Object causeMessage = cause instanceof ParseException ? cause.getMessage() : e;
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
                if (defaultClientModule == null || defaultClientModule.equals(entryPath.substring(1))) {
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
     * @param entryPath The path of the mainfest file.
     * @param required true if current module is known to be a client module, and false otherwise
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
            mbeanServiceReg = appMBeanRuntime.registerApplicationMBean(getName(), getContainer(), applicationDD != null ? "/META-INF/application.xml" : null, deployedModuleInfos);
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

    public ApplicationMonitoringInformation createApplicationMonitoringInformation() {
        // Add monitors ...
        /*
         * We need to listen to the /WEB-INF directory on all of the WAR modules and /META-INF/application.xml in the EAR container.
         *
         * We only discover the WARs when we construct the earAppInfo so build this now, we'll pass it back to the app manager so that it can be pass through to the install method
         * to save us some time later on.
         *
         * First though we go to interpreted as this knows when a container is a WAR
         */

        // TODO: Move this into the info objects; each info type should
        //       handle the cases that they care about.

        // TODO: An application.xml will not always be present.

        Collection<Notification> notificationsToMonitor = new HashSet<Notification>();
        if (applicationDD != null) {
            notificationsToMonitor.add(new DefaultNotification(getContainer(), "/META-INF/application.xml"));
        }

        if (earLibDeploymentInfo != null) {
            String libDirectory = earLibDeploymentInfo.getContainer().getPath();
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
        if (preExpansionEarContainer != null) {
            notificationsToMonitor.add(new DefaultNotification(preExpansionEarContainer, "/"));
        }

        return new DefaultApplicationMonitoringInformation(notificationsToMonitor, true);
    }

    private List<ContainerInfo> getClasspathContainerInfos() {
        if (this.classpathContainerInfos == null) {
            List<ContainerInfo> classpathContainerInfos = new ArrayList<ContainerInfo>();
            addEJBJarContainers(classpathContainerInfos);
            addEARLibContainers(classpathContainerInfos);
            addConnectorContainers(classpathContainerInfos);
            checkClientJarContainers(classpathContainerInfos);
            this.classpathContainerInfos = classpathContainerInfos;
        }
        return this.classpathContainerInfos;
    }

    private void addEJBJarContainers(List<ContainerInfo> classpathContainerInfos) {
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

    private void checkClientJarContainers(List<ContainerInfo> classpathContainerInfos) {
        boolean found = false;
        try {
            for (ModuleContainerInfoBase modInfo : moduleContainerInfos) {
                if (modInfo instanceof ClientModuleContainerInfo) {
                    if (defaultClientModule == null) {
                        if (found) {
                            Tr.error(_tc, "error.module.no.defaultclient");
                        }
                        found = true;
                    } else if (defaultClientModule.equals(modInfo.getModuleURI())) {
                        if (found) {
                            Tr.error(_tc, "error.module.dup.client", defaultClientModule);
                        }
                        found = true;
                    }
                }
            }
            if (!found && defaultClientModule != null) {
                Tr.error(_tc, "error.module.client.notfound", defaultClientModule);
            }
        } catch (Throwable th) {
            Tr.error(_tc, "error.application.libraries", getName(), th);
        }
    }

    private void addConnectorContainers(List<ContainerInfo> classpathContainerInfos) {
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

    private void addEARLibContainers(List<ContainerInfo> classpathContainerInfos) {
        if (this.earLibDeploymentInfo != null) {
            classpathContainerInfos.addAll(this.earLibDeploymentInfo.getClassesContainerInfo());
        }
    }

    /*
     * The list of annotations that identify an EJB Jar
     */
    private static final List<String> EJB_ANNOTATIONS = new ArrayList<String>();

    static {
        EJB_ANNOTATIONS.add("javax.ejb.MessageDriven");
        EJB_ANNOTATIONS.add("javax.ejb.Stateless");
        EJB_ANNOTATIONS.add("javax.ejb.Stateful");
        EJB_ANNOTATIONS.add("javax.ejb.Singleton");
    }

    private boolean hasSpecifiedAnnotations(Container moduleContainer, List<String> useAnnotationTypeNames) {
        ContainerAnnotations ca = null;
        try {
            ca = moduleContainer.adapt(ContainerAnnotations.class);
        } catch (UnableToAdaptException e) {
            // error.module.annotation.targets=
            // CWWKZ0121E: Application {0}: Failed to access annotations for module {1} of type {2}: {3}
            Tr.error(_tc, "error.module.class.source", getName(), moduleContainer.getPath(), "EJB", e);

            if (_tc.isDebugEnabled()) {
                Tr.debug(_tc, "Selected [ 0 ] EJB annotation cases: Error obtaining annotation targets");
            }
        }

        if (ca != null && ca.hasSpecifiedAnnotations(useAnnotationTypeNames)) {
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

    /**
     * Specify the packages to be imported dynamically into all web apps
     */
    private static final List<String> DYNAMIC_IMPORT_PACKAGE_LIST = Collections.unmodifiableList(Arrays.asList("*"));

    private void createApplicationClassLoader() {
        if (_tc.isDebugEnabled()) {
            Tr.debug(_tc, "Application [ " + getName() + " ]: Class loader ID [ " + appClassLoaderId + " ]");
        }

        List<Container> classLoaderContainers = new ArrayList<Container>();
        List<Container> nativeLibraryContainers = new ArrayList<Container>();

        for (ContainerInfo containerInfo : getClasspathContainerInfos()) {
            Container container = containerInfo.getContainer();
            classLoaderContainers.add(container);
            if (containerInfo.getType() == ContainerInfo.Type.RAR_MODULE) {
                nativeLibraryContainers.add(container);
            }
        }

        GatewayConfiguration gwCfg = classLoadingService.createGatewayConfiguration().setApplicationName(getName()).setDynamicImportPackage(DYNAMIC_IMPORT_PACKAGE_LIST);

        protectionDomain = getProtectionDomain();

        ClassLoaderConfiguration clCfg = classLoadingService.createClassLoaderConfiguration().setId(appClassLoaderId).setProtectionDomain(protectionDomain).setNativeLibraryContainers(nativeLibraryContainers);

        ClassLoader appClassLoader = createTopLevelClassLoader(classLoaderContainers, gwCfg, clCfg);
        if (_tc.isDebugEnabled()) {
            Tr.debug(_tc, "Application [ " + getName() + " ]: Class loader [ " + appClassLoader + " ]");
        }
        this.appClassLoader = appClassLoader;
    }

    /**
     * <p>Create a class loader for a web or client module - which is a child to the top-level app loader.</p>
     *
     * <p>Notable: This constructor is only usable for web and client modules. Other code of the module
     * container must make available the collection of locations for the class path.</p>
     *
     * @see WebModuleClassesInfo
     *
     * @param type - The module identity domain ("WebModule" or "ClientModule") - also used for trace
     * @param moduleInfo The module info.
     * @param moduleClassesContainers The list of containers for module code.
     *
     * @return The class loader for the child module.
     */
    private ClassLoader createModuleChildClassLoader(String moduleDomain, ModuleInfo moduleInfo, List<ContainerInfo> moduleClassesContainers) {
        String moduleName = moduleInfo.getName();
        String j2eeModuleName = moduleInfo.getURI();

        List<Container> containers = new ArrayList<Container>();
        for (ContainerInfo containerInfo : moduleClassesContainers) {
            containers.add(containerInfo.getContainer());
        }

        // Force the app class loader ID to be assigned before assigning any module ID.
        // That makes for a clearer sequence of IDs, if they are sequential.

        ClassLoaderIdentity parentId = getApplicationClassLoaderId();
        if (_tc.isDebugEnabled()) {
            Tr.debug(_tc, moduleDomain + " [ " + moduleName + " ] uses application class loader ID [ " + parentId + " ]");
        }

        ClassLoaderIdentity moduleClassLoaderId = classLoadingService.createIdentity(moduleDomain, getName() + "#" + j2eeModuleName);
        if (_tc.isDebugEnabled()) {
            Tr.debug(_tc, moduleDomain + " [ " + moduleName + " ] sets module class loader ID [ " + moduleClassLoaderId + " ]");
        }

        ClassLoaderConfiguration clCfg = classLoadingService.createClassLoaderConfiguration().setId(moduleClassLoaderId).setParentId(parentId).setDelegateToParentAfterCheckingLocalClasspath(isDelegateLast);

        clCfg.setProtectionDomain(protectionDomain);

        if (_tc.isDebugEnabled()) {
            Tr.debug(_tc, moduleDomain + " [ " + moduleName + " ] sets isDelegateLast [ " + Boolean.valueOf(isDelegateLast) + " ]");
        }

        ClassLoader moduleClassLoader = classLoadingService.createChildClassLoader(containers, clCfg);
        associateClassLoaderWithApp(moduleClassLoader);
        if (_tc.isDebugEnabled()) {
            Tr.debug(_tc, moduleDomain + " [ " + moduleName + " ] sets class loader [ " + moduleClassLoader + " ]");
        }
        return moduleClassLoader;
    }
}
