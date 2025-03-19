/*******************************************************************************
 * Copyright (c) 2012, 2025 IBM Corporation and others.
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
package com.ibm.ws.cdi.impl;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.DeploymentException;

import org.jboss.weld.bootstrap.BeanDeploymentModule;
import org.jboss.weld.bootstrap.BeanDeploymentModules;
import org.jboss.weld.bootstrap.WeldBootstrap;
import org.jboss.weld.bootstrap.api.Environment;
import org.jboss.weld.bootstrap.api.Environments;
import org.jboss.weld.bootstrap.spi.EEModuleDescriptor;
import org.jboss.weld.config.ConfigurationKey;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.cdi.CDIException;
import com.ibm.ws.cdi.CDIService;
import com.ibm.ws.cdi.impl.weld.BDAFactory;
import com.ibm.ws.cdi.impl.weld.WebSphereCDIDeploymentImpl;
import com.ibm.ws.cdi.impl.weld.WebSphereEEModuleDescriptor;
import com.ibm.ws.cdi.internal.interfaces.Application;
import com.ibm.ws.cdi.internal.interfaces.ArchiveType;
import com.ibm.ws.cdi.internal.interfaces.CDIArchive;
import com.ibm.ws.cdi.internal.interfaces.CDIContainer;
import com.ibm.ws.cdi.internal.interfaces.CDIContainerEventManager;
import com.ibm.ws.cdi.internal.interfaces.CDIRuntime;
import com.ibm.ws.cdi.internal.interfaces.CDIUtils;
import com.ibm.ws.cdi.internal.interfaces.ContextBeginnerEnder;
import com.ibm.ws.cdi.internal.interfaces.ExtensionArchive;
import com.ibm.ws.cdi.internal.interfaces.ExtensionArchiveFactory;
import com.ibm.ws.cdi.internal.interfaces.ExtensionArchiveProvider;
import com.ibm.ws.cdi.internal.interfaces.WebSphereBeanDeploymentArchive;
import com.ibm.ws.cdi.internal.interfaces.WebSphereCDIDeployment;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.runtime.metadata.ApplicationMetaData;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.runtime.metadata.MetaDataSlot;
import com.ibm.ws.runtime.metadata.ModuleMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;
import com.ibm.wsspi.injectionengine.InjectionException;
import com.ibm.wsspi.injectionengine.InjectionMetaData;
import com.ibm.wsspi.injectionengine.InjectionMetaDataListener;
import com.ibm.wsspi.injectionengine.ReferenceContext;
import com.ibm.wsspi.kernel.service.utils.ServiceAndServiceReferencePair;
import com.ibm.wsspi.kernel.service.utils.ServiceReferenceUtils;

import io.openliberty.cdi.spi.CDIExtensionMetadata;

/**
 * The main CDI entry point. Handles starting up and shutting down CDI in response to applications starting and stopping. Implements {@link CDIService} to provide information about
 * the current CDI application.
 */

public class CDIContainerImpl implements CDIContainer, InjectionMetaDataListener {
    private static final TraceComponent tc = Tr.register(CDIContainerImpl.class);
    private static final ClassLoader CLASSLOADER = CDIContainerImpl.class.getClassLoader();

    private static final String EXTENSION_API_CLASSES = "api.classes";
    private static final String EXTENSION_BEAN_DEFINING_ANNOTATIONS = "bean.defining.annotations";
    private static final String EXTENSION_APP_BDAS_VISIBLE = "application.bdas.visible";
    private static final String EXTENSION_CLASSES_ONLY_MODE = "extension.classes.only";

    private static final String EXTENSION_API_CLASSES_SEPARATOR = ";";

    //This is a map from OSGi Service ID (of the extension) to a ExtensionArchive
    private final Map<Long, ExtensionArchive> runtimeExtensionMap = new HashMap<>();

    private final ThreadLocal<WebSphereCDIDeployment> currentDeployment = new ThreadLocal<WebSphereCDIDeployment>();
    private final CDIRuntime cdiRuntime;

    private final static String WELD_DISABLE_BEANSXML_VALIDATING = ConfigurationKey.DISABLE_XML_VALIDATION.get();

    //turn off the beans.xml schema validation if it is not set
    static {
        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            @Override
            public Void run() {
                String xmlValidationDisabled = System.getProperty(WELD_DISABLE_BEANSXML_VALIDATING);
                //if the property was not set, set 'disable validating' to true

                if (xmlValidationDisabled == null) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "CDIContainerImpl", "The system property " + WELD_DISABLE_BEANSXML_VALIDATING + " was not set explicitly. Set it to be 'true' by default.");
                    }
                    System.setProperty(WELD_DISABLE_BEANSXML_VALIDATING, "true");

                } else {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "CDIContainerImpl",
                                 "The system property " + WELD_DISABLE_BEANSXML_VALIDATING + " was explicitly set and remained as " + xmlValidationDisabled);
                    }
                }
                return null;

            }
        });
    }

    /**
     * @param CDIRuntime the current CDIRuntime
     */
    public CDIContainerImpl(CDIRuntime cdiRuntime) {
        this.cdiRuntime = cdiRuntime;
    }

    @FFDCIgnore(DeploymentException.class)
    public WebSphereCDIDeployment startInitialization(Application application) throws CDIException {
        WebSphereCDIDeployment webSphereCDIDeployment = null;
        try {
            //first create the deployment object which has the full structure of BDAs inside
            webSphereCDIDeployment = createWebSphereCDIDeployment(application);
            currentDeployment.set(webSphereCDIDeployment);

            //scan for beans
            webSphereCDIDeployment.scan();

            //save the deployment away in useful places
            setDeployment(application, webSphereCDIDeployment);

            //if the application as a whole is CDI Enabled then we create and add the runtime extension BDAs as well and then bootstrap CDI
            if (webSphereCDIDeployment.isCDIEnabled()) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "startInitialization", "CDI is enabled, starting the CDI Deployment");
                }
                webSphereCDIDeployment.initializeInjectionServices();

                // get the application id
                String contextID = webSphereCDIDeployment.getDeploymentID();
                // get the environment
                CDIContainerEventManager eventManager = cdiRuntime.getCDIContainerEventManager();
                Environment environment = Environments.EE;
                if (eventManager != null) {
                    environment = eventManager.getEnvironment();
                }
                // start the bootrapping process...
                final WeldBootstrap weldBootstrap = webSphereCDIDeployment.getBootstrap();
                weldBootstrap.startExtensions(webSphereCDIDeployment.getExtensions());
                weldBootstrap.startContainer(contextID, environment, webSphereCDIDeployment);
                AccessController.doPrivileged(new PrivilegedAction<Void>() {
                    @Override
                    public Void run() {
                        weldBootstrap.startInitialization();
                        return null;
                    }

                });

                webSphereCDIDeployment.validateJEEComponentClasses();
                weldBootstrap.deployBeans();
                weldBootstrap.validateBeans();
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "startInitialization", "CDI is not enabled, shutting down CDI");
                }
                webSphereCDIDeployment.shutdown();
                unsetDeployment(application);
                webSphereCDIDeployment = null;
            }

        } catch (DeploymentException e) {
            DeploymentException e1 = e;
            if (webSphereCDIDeployment != null) {
                CDIContainerEventManager eventManager = this.cdiRuntime.getCDIContainerEventManager();
                if (eventManager != null) {
                    e1 = eventManager.processDeploymentException(webSphereCDIDeployment, e);
                }
            }
            throw e1;
        } finally {
            currentDeployment.remove();
        }
        return webSphereCDIDeployment;
    }

    public void endInitialization(WebSphereCDIDeployment webSphereCDIDeployment) throws CDIException {
        WeldBootstrap weldBootstrap = webSphereCDIDeployment.getBootstrap();
        if (weldBootstrap != null) {
            try {
                currentDeployment.set(webSphereCDIDeployment);
                weldBootstrap.endInitialization();
            } finally {
                currentDeployment.remove();
            }
        }
    }

    public void applicationStarted(Application application) throws CDIException {
        CDIContainerEventManager eventManager = cdiRuntime.getCDIContainerEventManager();
        if (eventManager != null) {
            runForEachNonWebModuleWithContext(eventManager::fireStartupEvent, application);
        }
    }

    public void applicationStopping(Application application) throws CDIException {
        CDIContainerEventManager eventManager = cdiRuntime.getCDIContainerEventManager();
        if (eventManager != null) {
            runForEachNonWebModuleWithContext(eventManager::fireShutdownEvent, application);
        }
    }

    /**
     * This method loops through each module (that is not a WebModule) in an application. For each one it sets
     * the component metadata associated with that module as well as the TCCL associated with the application
     * then it runs an action. The action is likely an event form CDIContainerEventManager
     *
     * @param action The action to run
     * @param application the application to run actions
     */
    private void runForEachNonWebModuleWithContext(Consumer<BeanDeploymentModule> action, Application application) throws CDIException {
        WebSphereCDIDeployment deployment = getDeployment(application);
        if (deployment != null) {
            BeanDeploymentModules modules = deployment.getServices().get(BeanDeploymentModules.class);
            if (modules != null) {
                for (BeanDeploymentModule module : modules) {
                    // This method is used for CDI Startup and Shutdown events. In web modules those events are fired by the ServletContext being initialized or destroyed.
                    if (!module.isWebModule()) {
                        String id = module.getId();
                        WebSphereBeanDeploymentArchive bda = deployment.getBeanDeploymentArchive(id);
                        if (bda != null) {
                            try (ContextBeginnerEnder contextBeginnerEnder = cdiRuntime.createContextBeginnerEnder().extractComponentMetaData(bda.getArchive())
                                                                                       .extractTCCL(application).beginContext()) {
                                action.accept(module);
                            }
                        } else {
                            throw new IllegalStateException(Tr.formatMessage(tc, "no.bda.for.module.CWOWB1019E", module, module.getId()));
                        }
                    }
                }
            }
        }
    }

    public void applicationStopped(Application application) throws CDIException {
        WebSphereCDIDeployment deployment = getDeployment(application);
        if (deployment != null) {
            try (ContextBeginnerEnder contextBeginnerEnder = cdiRuntime.createContextBeginnerEnder().extractComponentMetaData(application)
                                                                       .extractTCCL(application).beginContext()) {

                currentDeployment.set(deployment);
                deployment.shutdown();
            } finally {
                currentDeployment.remove();
                unsetDeployment(application);
            }
        }
    }

    /**
     * This method creates the Deployment structure with all it's BDAs.
     *
     * @param application
     * @param extensionArchives
     * @return
     * @throws CDIException
     */
    private WebSphereCDIDeployment createWebSphereCDIDeployment(Application application) throws CDIException {
        WebSphereCDIDeployment webSphereCDIDeployment = new WebSphereCDIDeploymentImpl(application, cdiRuntime);

        DiscoveredBdas discoveredBdas = new DiscoveredBdas(webSphereCDIDeployment);

        if (application.hasModules()) {

            Collection<CDIArchive> libraryArchives = application.getLibraryArchives();
            Collection<CDIArchive> moduleArchives = application.getModuleArchives();

            ClassLoader applicationClassLoader = application.getClassLoader();
            webSphereCDIDeployment.setClassLoader(applicationClassLoader);

            processLibraries(webSphereCDIDeployment,
                             discoveredBdas,
                             libraryArchives);

            processModules(webSphereCDIDeployment,
                           discoveredBdas,
                           moduleArchives);

            //discoveredBdas has the full map, let's go through them all to make sure the wire is complete
            discoveredBdas.makeCrossBoundaryWiring();

            //and finally the runtime extensions
            addRuntimeExtensions(webSphereCDIDeployment,
                                 discoveredBdas);
        }

        return webSphereCDIDeployment;
    }

    /**
     * Create a BDA for each runtime extension and add it to the deployment.
     *
     * @param webSphereCDIDeployment
     * @param excludedBdas a set of application BDAs which should not be visible to runtime extensions
     * @throws CDIException
     */
    private void addRuntimeExtensions(WebSphereCDIDeployment webSphereCDIDeployment,
                                      DiscoveredBdas discoveredBdas) throws CDIException {
        //add the normal runtime extension using the bundle classloader to load the bda classes
        Set<WebSphereBeanDeploymentArchive> extensions = createExtensionBDAs(webSphereCDIDeployment);
        //add the runtime extensions containers
        webSphereCDIDeployment.addBeanDeploymentArchives(extensions);
        //add the runtime extensions containers
        for (WebSphereBeanDeploymentArchive bda : webSphereCDIDeployment.getApplicationBDAs()) {
            for (WebSphereBeanDeploymentArchive extBDA : extensions) {
                if (bda != extBDA) {
                    bda.addBeanDeploymentArchive(extBDA);
                    if (extBDA.extensionCanSeeApplicationBDAs() && !discoveredBdas.isExcluded(bda)) {
                        extBDA.addBeanDeploymentArchive(bda);
                    }
                }
            }
        }
        //allow extensions which can see application BDAs to also see other extensions
        for (WebSphereBeanDeploymentArchive extBDA : extensions) {
            if (extBDA.extensionCanSeeApplicationBDAs()) {
                for (WebSphereBeanDeploymentArchive otherBDA : extensions) {
                    if (extBDA != otherBDA) {
                        extBDA.addBeanDeploymentArchive(otherBDA);
                    }
                }
            }
        }
    }

    /**
     * Create BDAs for all runtime extensions that cannot see application bdas
     *
     * @param applicationContext
     * @return
     * @throws CDIException
     */
    private Set<WebSphereBeanDeploymentArchive> createExtensionBDAs(WebSphereCDIDeployment applicationContext) throws CDIException {

        Set<WebSphereBeanDeploymentArchive> extensionBdas = new HashSet<WebSphereBeanDeploymentArchive>();

        Set<ExtensionArchive> extensions = getExtensionArchives(applicationContext);

        if (extensions != null) {
            for (ExtensionArchive extArchive : extensions) {

                WebSphereBeanDeploymentArchive moduleCDIContext = BDAFactory.createBDA(applicationContext,
                                                                                       extArchive,
                                                                                       cdiRuntime);
                extensionBdas.add(moduleCDIContext);
            }
        }

        return extensionBdas;
    }

    private void processLibraries(WebSphereCDIDeployment applicationContext,
                                  DiscoveredBdas discoveredBdas,
                                  Collection<CDIArchive> archives) throws CDIException {

        for (CDIArchive archive : archives) {

            String archiveID = applicationContext.getDeploymentID() + "#" + archive.getName();

            // we need to work our whether to create a bda or not
            if (cdiRuntime.skipCreatingBda(archive)) {
                continue;
            }

            WebSphereBeanDeploymentArchive moduleCDIContext = BDAFactory.createBDA(applicationContext,
                                                                                   archiveID,
                                                                                   archive,
                                                                                   cdiRuntime);
            discoveredBdas.addDiscoveredBda(archive.getType(), moduleCDIContext);
        }
    }

    /**
     * Create BDAs for either the EJB, Web or Client modules and any libraries they reference on their classpath.
     */
    private void processModules(WebSphereCDIDeployment applicationContext,
                                DiscoveredBdas discoveredBdas,
                                Collection<CDIArchive> moduleArchives) throws CDIException {

        List<WebSphereBeanDeploymentArchive> moduleBDAs = new ArrayList<WebSphereBeanDeploymentArchive>();

        for (CDIArchive archive : moduleArchives) {

            if (cdiRuntime.isClientProcess()) {
                // when on a client process, we only process client modules and ignore the others
                if (ArchiveType.CLIENT_MODULE != archive.getType()) {
                    continue;
                }
            } else {
                // when on a server process, we do not process client modules, just the others
                if (ArchiveType.CLIENT_MODULE == archive.getType()) {
                    continue;
                }
            }

            //if the app is not an EAR then there should be only one module and we can just use it's classloader
            if (applicationContext.getClassLoader() == null) {
                applicationContext.setClassLoader(archive.getClassLoader());
            }

            String archiveID = archive.getJ2EEName().toString();

            // we need to work our whether to create a bda or not
            if (cdiRuntime.skipCreatingBda(archive)) {
                continue;
            }

            EEModuleDescriptor eeModuleDescriptor = new WebSphereEEModuleDescriptor(archiveID, archive.getJ2EEName(), archive.getType());

            WebSphereBeanDeploymentArchive moduleBda = BDAFactory.createBDA(applicationContext,
                                                                            archiveID,
                                                                            archive,
                                                                            cdiRuntime,
                                                                            eeModuleDescriptor);
            discoveredBdas.addDiscoveredBda(archive.getType(), moduleBda);
            moduleBDAs.add(moduleBda);

        }

        // Having processed all the modules, we now process their libraries
        // We do it in this order in case one of the modules references another of the same type as a library
        for (WebSphereBeanDeploymentArchive bda : moduleBDAs) {
            processModuleLibraries(bda, discoveredBdas);
        }
    }

    private Set<WebSphereBeanDeploymentArchive> processModuleLibraries(WebSphereBeanDeploymentArchive parentModule,
                                                                       DiscoveredBdas discoveredBdas) throws CDIException {
        CDIArchive archive = parentModule.getArchive();

        Set<WebSphereBeanDeploymentArchive> childBdas = new HashSet<WebSphereBeanDeploymentArchive>();

        // Occasionally, the archive can include the same jar more than once, e.g. when a jar lists itself on the manifest classpath
        // Track the libraries we've seen for this module to avoid adding the same one twice
        Set<String> moduleArchivePaths = new HashSet<String>();
        moduleArchivePaths.add(archive.getPath());

        Collection<CDIArchive> childArchives = archive.getModuleLibraryArchives();

        ArchiveType parentType = archive.getType();
        for (CDIArchive child : childArchives) {

            ArchiveType childType = child.getType();
            String archiveID = null;
            EEModuleDescriptor eeModuleDescriptor = null;
            if (childType == ArchiveType.WEB_INF_LIB ||
                childType == ArchiveType.MANIFEST_CLASSPATH ||
                childType == ArchiveType.JAR_MODULE ||
                childType == ArchiveType.SHARED_LIB) {

                archiveID = parentModule.getId() + "#" + childType + "#" + child.getName();
                WebSphereEEModuleDescriptor parentDescriptor = (WebSphereEEModuleDescriptor) parentModule.getServices().get(EEModuleDescriptor.class);
                //a module library uses the same descriptor ID as it's parent module
                eeModuleDescriptor = new WebSphereEEModuleDescriptor(parentDescriptor.getId(), parentDescriptor.getJ2eeName(), childType);

            } else {
                // This isn't the right type to be a child library, skip it
                continue;
            }

            String childPath = child.getPath();

            if (discoveredBdas.isAlreadyAccessible(parentType, child) || moduleArchivePaths.contains(childPath)) {
                // This archive is accessible anyway, even if it wasn't on the classpath, skip it
                continue;
            }

            // we need to work our whether to create a bda or not
            if (cdiRuntime.skipCreatingBda(child)) {
                continue;
            }

            WebSphereBeanDeploymentArchive newChildBda = BDAFactory.createBDA(parentModule.getCDIDeployment(),
                                                                              archiveID,
                                                                              child,
                                                                              cdiRuntime,
                                                                              eeModuleDescriptor);

            discoveredBdas.addDiscoveredBda(parentType, newChildBda);
            moduleArchivePaths.add(childPath);

            // Set up the wiring according to classloaders
            parentModule.addDescendantBda(newChildBda);
            CDIUtils.addWiring(parentModule, newChildBda);

            for (WebSphereBeanDeploymentArchive childBda : childBdas) {
                CDIUtils.addWiring(childBda, newChildBda);
            }

            // Finally add the new child to the list of children
            childBdas.add(newChildBda);
        }

        return childBdas;
    }

    public BeanManager getCurrentBeanManager() {
        WebSphereCDIDeployment cdiDeployment = getCurrentDeployment();
        return getCurrentBeanManager(cdiDeployment);
    }

    public BeanManager getCurrentBeanManager(WebSphereCDIDeployment cdiDeployment) {

        // Try to walk the stack back to find the bean class and lookup the bean manager via the class.
        BeanManager beanManager = getCurrentBeanManagerViaStackWalk(cdiDeployment);

        // If that is not available, try to get the bean manager associated with the current module
        if (beanManager == null) {
            beanManager = getCurrentModuleBeanManager();
        }

        return beanManager;

    }

    private BeanManager getCurrentBeanManagerViaStackWalk(WebSphereCDIDeployment deployment) {
        // Attempt to find the bean manager by walking down the current thread stack.
        // We should be able to find the bean manager based on the bean that is
        // calling into the getBeanManager method -- which should be the class on
        // the thread stack just below the IBM code.  This should only be necessary
        // during app start, so the currentDeployment (WebSphereCDIDeployment) should
        // be set on this thread.
        BeanManager beanManager = null;
        if (deployment != null) {

            // get classes on the current stack using protected SecurityManager method
            Class<?>[] stackClasses = AccessController.doPrivileged(new PrivilegedAction<Class<?>[]>() {
                @Override
                public Class<?>[] run() {
                    return new SecurityManager() {
                        @Override
                        protected Class<?>[] getClassContext() {
                            return super.getClassContext();
                        }
                    }.getClassContext();
                }
            });

            // now iterate over them to find the appropriate bean deployment archive
            for (int i = 0; i < stackClasses.length; i++) {
                final Class<?> cls = stackClasses[i];
                ClassLoader classLoader = AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
                    @Override
                    public ClassLoader run() {
                        return cls.getClassLoader();
                    }

                });
                // skip classes that we know are not CDI beans
                if (!CLASSLOADER.equals(classLoader)) {
                    WebSphereBeanDeploymentArchive currentBda = deployment.getBeanDeploymentArchiveFromClass(cls);
                    if (currentBda != null) {
                        beanManager = currentBda.getBeanManager();
                        break;
                    }
                }
            }
        }
        return beanManager;
    }

    public BeanManager getClassBeanManager(Class<?> clazz) {
        WebSphereBeanDeploymentArchive currentBda = getClassBeanDeploymentArchive(clazz);
        BeanManager beanManager = null;
        if (currentBda != null) {
            beanManager = currentBda.getBeanManager();
        }
        return beanManager;
    }

    public WebSphereBeanDeploymentArchive getClassBeanDeploymentArchive(Class<?> clazz) {
        WebSphereCDIDeployment deployment = getCurrentDeployment();
        WebSphereBeanDeploymentArchive currentBda = null;
        if (deployment != null) {
            currentBda = deployment.getBeanDeploymentArchiveFromClass(clazz);
        }

        return currentBda;
    }

//    private WebSphereCDIDeployment getCDIDeployment(Application application) {
//        WebSphereCDIDeployment cdiDeployment = (application == null ? null : application.getDeployment());
//
//        // If we can't get the cdiDeployment from the module metadata, we're probably still in initialization
//        // Use the thread-local current deployment that we set at the start of startup
//        if (cdiDeployment == null) {
//            cdiDeployment = currentDeployment.get();
//        }
//
//        if (cdiDeployment == null) {
//            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
//                Tr.debug(tc, "getModuleBeanDeploymentArchive returning null.  applicationContext is null");
//            }
//            return null;
//        }
//
//        return cdiDeployment;
//    }

    public String getCurrentApplicationContextID() {

        WebSphereCDIDeployment cdiDeployment = getCurrentDeployment();
        if (cdiDeployment == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "getCurrentApplicationContextID returning null.  cdiDeployment is null");
            }
            return null;
        }

        String contextID = cdiDeployment.getDeploymentID();

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "getCurrentApplicationContextID successfully found a application context ID of: " + contextID);
        }

        return contextID;
    }

    /**
     * Returns the extension container info with ContainerInfo, classloader and container name
     *
     * @return
     * @throws CDIException
     */
    @SuppressWarnings("deprecation") //Until nobody uses com.ibm.ws.cdi.extension.WebSphereCDIExtension we still need to read it
    private Set<ExtensionArchive> getExtensionArchives(WebSphereCDIDeployment applicationContext) throws CDIException {

        Set<ExtensionArchive> extensionSet = new HashSet<>();

        // get hold of the container for extension bundle
        //add create the bean deployment archive from the container
        Iterator<ServiceAndServiceReferencePair<com.ibm.ws.cdi.extension.WebSphereCDIExtension>> extensions = cdiRuntime.getExtensionServices();
        while (extensions.hasNext()) {
            ServiceAndServiceReferencePair<com.ibm.ws.cdi.extension.WebSphereCDIExtension> extension = extensions.next();
            ServiceReference<com.ibm.ws.cdi.extension.WebSphereCDIExtension> sr = extension.getServiceReference();
            if (sr != null) {
                Long serviceID = ServiceReferenceUtils.getId(sr);
                ExtensionArchive extensionArchive = null;
                synchronized (this) {
                    extensionArchive = runtimeExtensionMap.get(serviceID);

                    if (extensionArchive == null) {
                        extensionArchive = newExtensionArchive(sr);
                        runtimeExtensionMap.put(serviceID, extensionArchive);
                    }
                }
                extensionSet.add(extensionArchive);
            }
        }

        // There should only be one of these, which one depends on the EE level.
        if (cdiRuntime.getExtensionArchiveFactories().size() != 1) {
            throw new IllegalStateException("found " + cdiRuntime.getExtensionArchiveFactories().size() + " extension archive factories");
        }

        for (ExtensionArchiveFactory factory : cdiRuntime.getExtensionArchiveFactories()) {
            //First iterate through the implementations of CDIExtensionMetadata and ask the providers for an archive for every implementation
            Iterator<ServiceAndServiceReferencePair<CDIExtensionMetadata>> spiExtensions = cdiRuntime.getSPIExtensionServices();
            while (spiExtensions.hasNext()) {
                ServiceAndServiceReferencePair<CDIExtensionMetadata> extensionMetaData = spiExtensions.next();
                ServiceReference<CDIExtensionMetadata> sr = extensionMetaData.getServiceReference();
                if (sr != null) {
                    Long serviceID = ServiceReferenceUtils.getId(sr);
                    ExtensionArchive extensionArchive = null;
                    synchronized (cdiRuntime) { //cdiRuntime is sure to be a common object across all threads.
                        extensionArchive = runtimeExtensionMap.get(serviceID);

                        if (extensionArchive == null) {
                            extensionArchive = factory.newSPIExtensionArchive(cdiRuntime, sr, extensionMetaData.getService(), applicationContext);
                        }
                        runtimeExtensionMap.put(serviceID, extensionArchive);
                        extensionSet.add(extensionArchive);
                    }
                }
            }
        }

        //Now ask any providers for any ExtensionArchives that are not coming from an SPI impl. These do not go in runtimeExtensionMap but do go in the extensionSet
        for (ExtensionArchiveProvider provider : cdiRuntime.getExtensionArchiveProviders()) {
            extensionSet.addAll(provider.getArchives(cdiRuntime, applicationContext));
        }

        return extensionSet;
    }

    @SuppressWarnings("deprecation") //Until nobody uses com.ibm.ws.cdi.extension.WebSphereCDIExtension we still need to read it
    private ExtensionArchive newExtensionArchive(ServiceReference<com.ibm.ws.cdi.extension.WebSphereCDIExtension> sr) throws CDIException {
        Bundle bundle = sr.getBundle();

        String extra_classes_blob = (String) sr.getProperty(EXTENSION_API_CLASSES);
        Set<String> extra_classes = new HashSet<String>();
        //parse the list
        if (extra_classes_blob != null) {

            String[] classes = extra_classes_blob.split(EXTENSION_API_CLASSES_SEPARATOR);
            if ((classes != null) && (classes.length > 0)) {
                Collections.addAll(extra_classes, classes);
            }
        }

        String extraAnnotationsBlob = (String) sr.getProperty(EXTENSION_BEAN_DEFINING_ANNOTATIONS);
        Set<String> extraAnnotations = new HashSet<String>();
        if (extraAnnotationsBlob != null) {
            String[] annotations = extraAnnotationsBlob.split(EXTENSION_API_CLASSES_SEPARATOR);
            if ((annotations != null) && (annotations.length > 0)) {
                Collections.addAll(extraAnnotations, annotations);
            }
        }

        String applicationBDAsVisibleStr = (String) sr.getProperty(EXTENSION_APP_BDAS_VISIBLE);
        boolean applicationBDAsVisible = Boolean.parseBoolean(applicationBDAsVisibleStr);

        String extClassesOnlyStr = (String) sr.getProperty(EXTENSION_CLASSES_ONLY_MODE);
        boolean extClassesOnly = Boolean.parseBoolean(extClassesOnlyStr);

        ExtensionArchive extensionArchive = cdiRuntime.getExtensionArchiveForBundle(bundle, extra_classes, extraAnnotations,
                                                                                    applicationBDAsVisible, extClassesOnly, Collections.emptySet());

        return extensionArchive;
    }

    @SuppressWarnings("deprecation") //Until nobody uses com.ibm.ws.cdi.extension.WebSphereCDIExtension we still need to read it
    public void removeRuntimeExtensionArchive(ServiceReference<com.ibm.ws.cdi.extension.WebSphereCDIExtension> sr) {
        synchronized (this) {
            Long serviceID = ServiceReferenceUtils.getId(sr);
            this.runtimeExtensionMap.remove(serviceID);
        }
    }

    public void removeRuntimeExtensionArchiveMetaData(ServiceReference<CDIExtensionMetadata> sr) {
        synchronized (this) {
            Long serviceID = ServiceReferenceUtils.getId(sr);
            this.runtimeExtensionMap.remove(serviceID);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void injectionMetaDataCreated(InjectionMetaData injectionMetaData) throws InjectionException {
        ReferenceContext referenceContext = injectionMetaData.getReferenceContext();

        if (referenceContext != null) {
            ApplicationMetaData appMetaData = injectionMetaData.getComponentNameSpaceConfiguration().getApplicationMetaData();
            WebSphereCDIDeployment deployment = getDeployment(appMetaData);

            if (deployment != null) {
                deployment.addReferenceContext(referenceContext);
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "injectionMetaDataCreated()", "Could not find a CDI Deployment for ReferenceContext " + injectionMetaData.getJ2EEName());
                }
            }
        }
    }

    /**
     * @param appMetaData
     * @return
     */
    public WebSphereCDIDeployment getDeployment(ApplicationMetaData applicationMetaData) {
        WebSphereCDIDeployment deployment = null;
        if (applicationMetaData != null) {
            MetaDataSlot slot = cdiRuntime.getApplicationSlot();
            deployment = (WebSphereCDIDeployment) applicationMetaData.getMetaData(slot);
        }
        return deployment;
    }

    public WebSphereCDIDeployment getDeployment(Application application) {
        WebSphereCDIDeployment deployment = getDeployment(application.getApplicationMetaData());
        return deployment;
    }

    public boolean isApplicationCDIEnabled(ApplicationMetaData applicationMetaData) {
        WebSphereCDIDeployment deployment = getDeployment(applicationMetaData);
        boolean enabled = false;

        if (deployment != null) {
            enabled = deployment.isCDIEnabled();
        }

        return enabled;
    }

    public boolean isCurrentApplicationCDIEnabled() {
        WebSphereCDIDeployment deployment = getCurrentDeployment();
        boolean enabled = false;

        if (deployment != null) {
            enabled = deployment.isCDIEnabled();
        }

        return enabled;
    }

    public boolean isModuleCDIEnabled(ModuleMetaData moduleMetaData) {
        WebSphereCDIDeployment deployment = getDeployment(moduleMetaData);
        boolean enabled = false;

        if (deployment != null) {
            String bdaId = moduleMetaData.getJ2EEName().toString();
            enabled = deployment.isCDIEnabled(bdaId);
        }

        return enabled;
    }

    public boolean isCurrentModuleCDIEnabled() {
        ModuleMetaData moduleMetaData = getCurrentModuleMetaData();
        boolean enabled = isModuleCDIEnabled(moduleMetaData);
        return enabled;
    }

    public ModuleMetaData getCurrentModuleMetaData() {
        ComponentMetaDataAccessorImpl cmdai = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor();
        ComponentMetaData cmd = cmdai.getComponentMetaData();
        ModuleMetaData moduleMetaData = null;
        if (cmd != null) {
            moduleMetaData = cmd.getModuleMetaData();
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "getCurrentModuleMetaData  ComponentMetaData is null");
            }
        }

        return moduleMetaData;
    }

    public void setDeployment(Application application, WebSphereCDIDeployment webSphereCDIDeployment) throws CDIException {
        ApplicationMetaData applicationMetaData = application.getApplicationMetaData();
        MetaDataSlot slot = cdiRuntime.getApplicationSlot();
        applicationMetaData.setMetaData(slot, webSphereCDIDeployment);
    }

    public void unsetDeployment(Application application) throws CDIException {
        setDeployment(application, null);
    }

    public void setDeployment(ApplicationMetaData applicationMetaData, WebSphereCDIDeployment webSphereCDIDeployment) throws CDIException {
        applicationMetaData.setMetaData(cdiRuntime.getApplicationSlot(), webSphereCDIDeployment);
    }

    public void unsetDeployment(ApplicationMetaData applicationMetaData) throws CDIException {
        setDeployment(applicationMetaData, null);
    }

    public WebSphereCDIDeployment getCurrentDeployment() {
        WebSphereCDIDeployment cdiDeployment = this.currentDeployment.get();
        if (cdiDeployment == null) {
            ModuleMetaData moduleMetaData = getCurrentModuleMetaData();
            ApplicationMetaData amd = (moduleMetaData == null ? null : moduleMetaData.getApplicationMetaData());
            if (amd != null) {
                cdiDeployment = getDeployment(amd);
            }
        }
        return cdiDeployment;
    }

    public BeanManager getBeanManager(ModuleMetaData moduleMetaData) {
        BeanManager beanManager = null;

        if (moduleMetaData == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "getModuleBeanManager returning null.  ModuleMetaData is null");
            }
            beanManager = null;
        } else {
            WebSphereBeanDeploymentArchive bda = getBeanDeploymentArchive(moduleMetaData);
            if (bda == null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "getModuleBeanManager returning null.  bda is null");
                }
                return null;
            } else {
                beanManager = bda.getBeanManager();
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "getModuleBeanManager successfully found a bean manager of: " + beanManager);
        }

        return beanManager;
    }

    private WebSphereBeanDeploymentArchive getBeanDeploymentArchive(ModuleMetaData moduleMetaData) {
        WebSphereBeanDeploymentArchive bda = null;

        WebSphereCDIDeployment cdiDeployment = getDeployment(moduleMetaData);
        if (cdiDeployment == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "getModuleBeanDeploymentArchive returning null.  cdiDeployment is null");
            }
            return null;
        }

        String moduleName = moduleMetaData.getJ2EEName().toString();
        bda = cdiDeployment.getBeanDeploymentArchive(moduleName);

        return bda;
    }

    private WebSphereCDIDeployment getDeployment(ModuleMetaData moduleMetaData) {
        WebSphereCDIDeployment cdiDeployment = null;

        if (moduleMetaData == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "getDeployment returning null.  moduleMetaData is null");
            }
            return null;
        } else {
            ApplicationMetaData applicationMetaData = moduleMetaData.getApplicationMetaData();
            cdiDeployment = getDeployment(applicationMetaData);
        }

        return cdiDeployment;
    }

    public BeanManager getCurrentModuleBeanManager() {
        ModuleMetaData moduleMetaData = getCurrentModuleMetaData();
        BeanManager beanManager = getBeanManager(moduleMetaData);
        return beanManager;
    }

}
