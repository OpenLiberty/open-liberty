/*******************************************************************************
 * Copyright (c) 2011, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jpa.container.osgi.internal;

import static com.ibm.ws.jpa.management.JPAConstants.PERSISTENCE_XML_RESOURCE_NAME;

import java.io.File;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TrConfigurator;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.Transaction.UOWCurrent;
import com.ibm.ws.container.service.app.deploy.ApplicationClassesContainerInfo;
import com.ibm.ws.container.service.app.deploy.ApplicationInfo;
import com.ibm.ws.container.service.app.deploy.ContainerInfo;
import com.ibm.ws.container.service.app.deploy.ContainerInfo.Type;
import com.ibm.ws.container.service.app.deploy.EARApplicationInfo;
import com.ibm.ws.container.service.app.deploy.ModuleClassesContainerInfo;
import com.ibm.ws.container.service.app.deploy.ModuleInfo;
import com.ibm.ws.container.service.state.ApplicationStateListener;
import com.ibm.ws.container.service.state.ModuleStateListener;
import com.ibm.ws.container.service.state.StateChangeException;
import com.ibm.ws.exception.RuntimeWarning;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.jpa.JPAComponent;
import com.ibm.ws.jpa.JPAExPcBindingContextAccessor;
import com.ibm.ws.jpa.JPAProviderIntegration;
import com.ibm.ws.jpa.JPAPuId;
import com.ibm.ws.jpa.container.osgi.jndi.JPAJndiLookupInfo;
import com.ibm.ws.jpa.container.osgi.jndi.JPAJndiLookupInfoRefAddr;
import com.ibm.ws.jpa.container.osgi.jndi.JPAJndiLookupObjectFactory;
import com.ibm.ws.jpa.management.AbstractJPAComponent;
import com.ibm.ws.jpa.management.JPAApplInfo;
import com.ibm.ws.jpa.management.JPAEMFPropertyProvider;
import com.ibm.ws.jpa.management.JPAIntrospection;
import com.ibm.ws.jpa.management.JPAPuScope;
import com.ibm.ws.jpa.management.JPARuntime;
import com.ibm.ws.kernel.LibertyProcess;
import com.ibm.ws.kernel.service.util.SecureAction;
import com.ibm.ws.tx.embeddable.EmbeddableWebSphereTransactionManager;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;
import com.ibm.wsspi.adaptable.module.NonPersistentCache;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.application.lifecycle.ApplicationRecycleCoordinator;
import com.ibm.wsspi.classloading.ClassLoadingService;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceSet;
import com.ibm.wsspi.kernel.service.utils.FrameworkState;
import com.ibm.wsspi.kernel.service.utils.ServiceAndServiceReferencePair;
import com.ibm.wsspi.logging.Introspector;
import com.ibm.wsspi.resource.ResourceBindingListener;

@Component(configurationPid = "com.ibm.ws.jpacomponent",
           configurationPolicy = ConfigurationPolicy.REQUIRE,
           service = { JPAComponent.class, ApplicationStateListener.class, ModuleStateListener.class, Introspector.class },
           // Use a higher service.ranking to ensure app/module listeners can
           // register class transformers before other components attempt to
           // load classes.
           property = { "service.vendor=IBM", "service.ranking:Integer=1000" })
public class JPAComponentImpl extends AbstractJPAComponent implements ApplicationStateListener, ModuleStateListener, Introspector {
    private static final TraceComponent tc = Tr.register(JPAComponentImpl.class);
    final static SecureAction priv = AccessController.doPrivileged(SecureAction.get());

    private static final String REFERENCE_JPA_RUNTIME = "jpaRuntime";
    private static final String REFERENCE_TRANSACTION_MANAGER = "transactionManager";
    private static final String REFERENCE_CONTEXT_ACCESSOR = "contextAccessor";
    private static final String REFERENCE_RESOURCE_BINDING_LISTENERS = "resourceBindingListeners";
    private static final String REFERENCE_JPA_PROVIDER = "jpaProvider";
    private static final String REFERENCE_JPA_PROPS_PROVIDER = "jpaPropsProvider";
    private static final String REFERENCE_APP_COORD = "appCoord";
    private static final String REFERENCE_CLASSLOADING_SERVICE = "classLoadingService";

    private ComponentContext context;
    private Dictionary<String, Object> props;
    private boolean server = false;
    private static final Set<String> stuckApps = new ConcurrentSkipListSet<String>();

    private final AtomicServiceReference<JPARuntime> jpaRuntime = new AtomicServiceReference<JPARuntime>(REFERENCE_JPA_RUNTIME);
    private final AtomicServiceReference<EmbeddableWebSphereTransactionManager> ivTransactionManagerSR = new AtomicServiceReference<EmbeddableWebSphereTransactionManager>(REFERENCE_TRANSACTION_MANAGER);
    private final AtomicServiceReference<JPAExPcBindingContextAccessor> ivContextAccessorSR = new AtomicServiceReference<JPAExPcBindingContextAccessor>(REFERENCE_CONTEXT_ACCESSOR);
    private final ConcurrentServiceReferenceSet<ResourceBindingListener> resourceBindingListeners = new ConcurrentServiceReferenceSet<ResourceBindingListener>(REFERENCE_RESOURCE_BINDING_LISTENERS);
    private final AtomicServiceReference<JPAProviderIntegration> providerIntegrationSR = new AtomicServiceReference<JPAProviderIntegration>(REFERENCE_JPA_PROVIDER);
    private final ConcurrentServiceReferenceSet<JPAEMFPropertyProvider> propProviderSRs = new ConcurrentServiceReferenceSet<JPAEMFPropertyProvider>(REFERENCE_JPA_PROPS_PROVIDER);
    private ClassLoadingService classLoadingService;

    @Activate
    protected void activate(ComponentContext cc) {
        props = cc.getProperties();
        jpaRuntime.activate(cc);
        ivTransactionManagerSR.activate(cc);
        ivContextAccessorSR.activate(cc);
        resourceBindingListeners.activate(cc);
        providerIntegrationSR.activate(cc);
        propProviderSRs.activate(cc);
        context = cc;
        initialize();
    }

    @Deactivate
    protected void deactivate(ComponentContext cc) {
        jpaRuntime.deactivate(cc);
        ivTransactionManagerSR.deactivate(cc);
        ivContextAccessorSR.deactivate(cc);
        resourceBindingListeners.deactivate(cc);
        providerIntegrationSR.deactivate(cc);
        propProviderSRs.deactivate(cc);
    }

    @Modified
    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected void modified(Map<?, ?> newProperties) {
        final String originalProvider = (String) props.get("defaultPersistenceProvider");
        final String originalDefaultJtaDataSourceJndiName = (String) props.get("defaultJtaDataSourceJndiName");
        final String originalDefaultNonJtaDataSourceJndiName = (String) props.get("defaultNonJtaDataSourceJndiName");

        if (newProperties instanceof Dictionary) {
            props = (Dictionary<String, Object>) newProperties;
        } else {
            props = new Hashtable(newProperties);
        }
        final String curProvider = (String) newProperties.get("defaultPersistenceProvider");
        final String curDefaultJtaDataSourceJndiName = (String) newProperties.get("defaultJtaDataSourceJndiName");
        final String curDefaultNonJtaDataSourceJndiName = (String) newProperties.get("defaultNonJtaDataSourceJndiName");

        boolean recycleJPAApplications = false;

        if (!Objects.equals(originalProvider, curProvider)) {
            // If the <jpa defaultPersistenceProvider=""/> element has changed, restart all JPA apps
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Detected change in defaultPersistenceProvider of the <jpa> element.  Restarting all JPA applications.",
                         originalProvider + " -> " + curProvider);
            recycleJPAApplications = true;
        } else if (!Objects.equals(originalDefaultJtaDataSourceJndiName, curDefaultJtaDataSourceJndiName)) {
            // If the <jpa defaultJtaDataSourceJndiName=""/> element has changed, restart all JPA apps
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Detected change in defaultJtaDataSourceJndiName of the <jpa> element.  Restarting all JPA applications.",
                         originalProvider + " -> " + curProvider);
            recycleJPAApplications = true;
        } else if (!Objects.equals(originalDefaultNonJtaDataSourceJndiName, curDefaultNonJtaDataSourceJndiName)) {
            // If the <jpa defaultNonJtaDataSourceJndiName=""/> element has changed, restart all JPA apps
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Detected change in defaultNonJtaDataSourceJndiName of the <jpa> element.  Restarting all JPA applications.",
                         originalProvider + " -> " + curProvider);
            recycleJPAApplications = true;
        }

        if (recycleJPAApplications) {
            recycleJPAApplications();
        }
    }

    private javax.naming.Reference createReference(boolean ejbInWar, JPAJndiLookupInfo info) {
        boolean ejbFeatureEnabled = getExPcBindingContext() != null;
        String objectFactory = ejbFeatureEnabled && ejbInWar ? "com.ibm.ws.ejbcontainer.jpa.injection.factory.HybridJPAObjectFactory" : JPAJndiLookupObjectFactory.class.getName();
        return new javax.naming.Reference("javax.persistence.EntityManager", new JPAJndiLookupInfoRefAddr(info), objectFactory, null);
    }

    @Override
    public javax.naming.Reference createPersistenceUnitReference(boolean ejbInWar,
                                                                 JPAPuId puId,
                                                                 J2EEName j2eeName,
                                                                 String refName,
                                                                 boolean isSFSB) {
        JPAJndiLookupInfo info = new JPAJndiLookupInfo(puId, j2eeName, refName, isSFSB);
        return createReference(ejbInWar, info);
    }

    @Override
    public javax.naming.Reference createPersistenceContextReference(boolean ejbInWar,
                                                                    JPAPuId puId,
                                                                    J2EEName j2eeName,
                                                                    String refName,
                                                                    boolean isExtendedContextType,
                                                                    boolean isSFSB,
                                                                    Properties properties,
                                                                    boolean isUnsynchronized) {
        JPAJndiLookupInfo info = new JPAJndiLookupInfo(puId, j2eeName, refName, isExtendedContextType, isSFSB, properties, isUnsynchronized);
        return createReference(ejbInWar, info);
    }

    @Override
    public void applicationStarting(ApplicationInfo appInfo) {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        String applName = appInfo.getDeploymentName();

        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "applicationStarting : " + applName);

        JPAApplInfo applInfo = new OSGiJPAApplInfo(this, applName, appInfo);
        ClassLoader appClassLoader = null;
        Container appLibContainer = null;
        if (appInfo instanceof EARApplicationInfo) {
            appClassLoader = ((EARApplicationInfo) appInfo).getApplicationClassLoader();
            appLibContainer = ((EARApplicationInfo) appInfo).getLibraryDirectoryContainer();
        }

        // ------------------------------------------------------------------------
        // JPA 2.0 Specification - 8.2 Persistence Unit Packaging
        //
        // A persistence unit is defined by a persistence.xml file. The jar file or
        // directory whose META-INF directory contains the persistence.xml file is
        // termed the root of the persistence unit. In Java EE environments, the
        // root of a persistence unit may be one of the following:
        //
        // -> an EJB-JAR file (not yet supported)
        // -> the WEB-INF/classes directory of a WAR file
        // -> a jar file in the WEB-INF/lib directory of a WAR file
        // -> a jar file in the EAR library directory
        // ------------------------------------------------------------------------

        try {
            JPAIntrospection.beginJPAIntrospection();
            JPAIntrospection.beginApplicationVisit(applName, applInfo);

            // Process any persistence.xml in EAR/lib/*.jar
            // Note: if there is no application classloader (standalone module),
            //       then there is no need to look for application scoped persistence.xml.
            if (appLibContainer != null && appClassLoader != null) {
                processLibraryJarPersistenceXml(applInfo, appLibContainer, applName, "lib/", JPAPuScope.EAR_Scope, appClassLoader);
            }

            // Process all modules in the application.  This must be done as early as possible
            // to prevent other features from load application classes before a JPA transformer
            // is registered for the application classloader.
            // TODO: this code would be much simpler of EARApplicationInfo provided a getModules method
            Container container = appInfo.getContainer();
            NonPersistentCache cache;
            try {
                cache = container.adapt(NonPersistentCache.class);
            } catch (UnableToAdaptException e) {
                if (isTraceOn && tc.isDebugEnabled()) {
                    Tr.debug(tc, "applicationStarting : " + e);
                }
                throw new RuntimeException("Failed to get NonPersistentCache for application ", e);
            }
            ApplicationClassesContainerInfo applicationClassesContainerInfo = (ApplicationClassesContainerInfo) cache.getFromCache(ApplicationClassesContainerInfo.class);
            // In an eba this is null
            if (applicationClassesContainerInfo != null) {
                List<ModuleClassesContainerInfo> mcci = applicationClassesContainerInfo.getModuleClassesContainerInfo();
                for (ModuleClassesContainerInfo m : mcci) {
                    List<ContainerInfo> moduleContainerInfos = m.getClassesContainerInfo();
                    if (moduleContainerInfos != null && !moduleContainerInfos.isEmpty()) {
                        ContainerInfo moduleContainerInfo = moduleContainerInfos.get(0);
                        Type t = moduleContainerInfo.getType();

                        ClassLoader moduleLoader = null;
                        try {
                            Container cc = moduleContainerInfo.getContainer();
                            NonPersistentCache npc = cc.adapt(NonPersistentCache.class);
                            ModuleInfo wmi = (ModuleInfo) npc.getFromCache(ModuleInfo.class);
                            moduleLoader = wmi.getClassLoader();
                        } catch (Exception e) {
                            if (isTraceOn && tc.isDebugEnabled()) {
                                Tr.debug(tc, "applicationStarting : " + e);
                            }
                            throw new RuntimeException("Failed to get ModuleInfo for application ", e);
                        }

                        try {
                            boolean serverRT = isServerRuntime();
                            if (t == Type.EJB_MODULE && serverRT) {
                                processEJBModulePersistenceXml(applInfo, moduleContainerInfo, moduleLoader);
                            } else if (t == Type.WEB_MODULE && serverRT) {
                                processWebModulePersistenceXml(applInfo, moduleContainerInfo, moduleLoader);
                            } else if (t == Type.CLIENT_MODULE && !serverRT) {
                                processClientModulePersistenceXml(applInfo, moduleContainerInfo, moduleLoader);
                            }
                        } catch (RuntimeException e) {
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                                Tr.debug(tc, "App failed to start due to JPA", applInfo.getApplName());
                            stuckApps.add(applInfo.getApplName());
                            throw e;
                        }
                    }
                }
            }

            try {
                startingApplication(applInfo);
            } catch (RuntimeWarning e) {
                FFDCFilter.processException(e, this.getClass().getName(), "457");
            }
        } finally {
            JPAIntrospection.endApplicationVisit();
            JPAIntrospection.executeTraceAnalysis();
            JPAIntrospection.endJPAIntrospection();
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "applicationStarting : " + applName);
    }

    @Override
    public void applicationStarted(ApplicationInfo appInfo) {}

    @Override
    public void applicationStopping(ApplicationInfo appInfo) {}

    @Override
    public void applicationStopped(ApplicationInfo appInfo) {
        String applName = appInfo.getDeploymentName();

        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "applicationStopped : " + applName);

        JPAApplInfo applInfo = applList.get(applName);
        if (applInfo != null) {
            destroyingApplication(applInfo);
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Application " + applName + " not found during destroyingDeployedApplication call.");
        }
        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "applicationStopped : " + applName);
    }

    /**
     * Notification that a module is starting.
     *
     * @param moduleInfo The ModuleInfo of the module
     */
    @Override
    public void moduleStarting(ModuleInfo moduleInfo) throws StateChangeException {
        getJPAProviderIntegration().moduleStarting(moduleInfo);
    }

    /**
     * Notification that a module has started.
     *
     * @param moduleInfo The ModuleInfo of the module
     */
    @Override
    public void moduleStarted(ModuleInfo moduleInfo) throws StateChangeException {
        getJPAProviderIntegration().moduleStarted(moduleInfo);
    }

    /**
     * Notification that a module is stopping.
     *
     * @param moduleInfo The ModuleInfo of the module
     */
    @Override
    public void moduleStopping(ModuleInfo moduleInfo) {
        getJPAProviderIntegration().moduleStopping(moduleInfo);
    }

    /**
     * Notification that a module has stopped.
     *
     * @param moduleInfo The ModuleInfo of the module
     */
    @Override
    public void moduleStopped(ModuleInfo moduleInfo) {
        getJPAProviderIntegration().moduleStopped(moduleInfo);
    }

    @Override
    public JPAProviderIntegration getJPAProviderIntegration() {
        return providerIntegrationSR.getService();
    }

    /**
     * Determine the root of all persistence units defined in a persistence.xml. <p>
     *
     * This is the value that should be returned by the method getPersistenceUnitRootUrl
     * on javax.persistence.spi.PersistenceUnitInfo. It is defined in the JPA 2.0
     * specification as follows: <p>
     *
     * The jar file or directory whose META-INF directory contains the persistence.xml
     * file is termed the root of the persistence unit. <p>
     *
     * @param appName     name of the application that contains the persistence.xml file
     * @param archiveName name of the archive that contains the persistence.xml file
     * @param pxml        reference to the persistence.xml file
     */
    private URL getPXmlRootURL(String appName, String archiveName, Entry pxml) {
        URL pxmlUrl = pxml.getResource();
        String pxmlStr = pxmlUrl.toString();
        String pxmlRootStr = pxmlStr.substring(0, pxmlStr.length() - PERSISTENCE_XML_RESOURCE_NAME.length());
        URL pxmlRootUrl = null;
        try {
            pxmlRootUrl = new URL(pxmlRootStr);
        } catch (MalformedURLException e) {
            e.getClass(); // findbugs
            Tr.error(tc, "INCORRECT_PU_ROOT_URL_SPEC_CWWJP0025E", pxmlRootStr, appName, archiveName);
        }
        return pxmlRootUrl;
    }

    /**
     * Common routine that will locate and process persistence.xml files in the
     * library directory of either an EAR or WAR archive. <p>
     *
     * @param applInfo    the application archive information
     * @param archiveName name of the archive containing the persistence.xml
     * @param rootPrefix  the persistence unit root prefix; prepended to the library
     *                        jar name if not null; otherwise archiveName is used
     * @param libEntry    the library directory entry from the enclosing container
     * @param scope       the scope to be applied to all persistence units found
     * @param classLaoder ClassLoader of the corresponding scope
     */
    private void processLibraryJarPersistenceXml(JPAApplInfo applInfo, Container libContainer,
                                                 String archiveName, String rootPrefix,
                                                 JPAPuScope scope, ClassLoader classLaoder) {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "processLibraryJarPersistenceXml : " + applInfo.getApplName() +
                         ", " + (libContainer != null ? libContainer.getName() : "null") +
                         ", " + archiveName + ", " + rootPrefix + ", " + scope);

        if (libContainer != null) {
            String puArchiveName = archiveName;
            for (Entry entry : libContainer) {
                if (isTraceOn && tc.isDebugEnabled()) {
                    Tr.debug(tc, "processing : " + entry.getName());
                }
                if (entry.getName().endsWith(".jar")) {
                    try {
                        Container jarContainer = entry.adapt(Container.class);
                        Entry pxml = jarContainer.getEntry(PERSISTENCE_XML_RESOURCE_NAME);
                        if (pxml != null) {
                            String appName = applInfo.getApplName();
                            if (rootPrefix != null) {
                                puArchiveName = rootPrefix + entry.getName();
                            }
                            URL puRoot = getPXmlRootURL(appName, archiveName, pxml);
                            applInfo.addPersistenceUnits(new OSGiJPAPXml(applInfo, puArchiveName, scope, puRoot, classLaoder, pxml));
                        }
                    } catch (UnableToAdaptException ex) {
                        // Not really a jar archive, just a poorly named file
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            Tr.debug(tc, "processLibraryJarPersistenceXml: ignoring " + entry.getName(), ex);
                    }
                }
            }
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "processLibraryJarPersistenceXml : " + applInfo.getApplName() + ", " + rootPrefix);
    }

    /**
     * Locates and processes all persistence.xml file in a WAR module. <p>
     *
     * @param applInfo the application archive information
     * @param module   the WAR module archive information
     */
    private void processWebModulePersistenceXml(JPAApplInfo applInfo, ContainerInfo warContainerInfo, ClassLoader warClassLoader) {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled()) {
            Tr.entry(tc, "processWebModulePersistenceXml : " + applInfo.getApplName() + "#" + warContainerInfo);
        }

        String archiveName = warContainerInfo.getName();
        Container warContainer = warContainerInfo.getContainer();

        // ------------------------------------------------------------------------
        // JPA 2.0 Specification - 8.2 Persistence Unit Packaging
        //
        // A persistence unit is defined by a persistence.xml file. The jar file or
        // directory whose META-INF directory contains the persistence.xml file is
        // termed the root of the persistence unit. In Java EE environments, the
        // root of a persistence unit may be one of the following:
        //
        // -> the WEB-INF/classes directory of a WAR file
        // -> a jar file in the WEB-INF/lib directory of a WAR file
        // ------------------------------------------------------------------------

        // Obtain any persistence.xml in WEB-INF/classes/META-INF
        Entry pxml = warContainer.getEntry("WEB-INF/classes/META-INF/persistence.xml");
        if (pxml != null) {
            String appName = applInfo.getApplName();
            URL puRoot = getPXmlRootURL(appName, archiveName, pxml);
            applInfo.addPersistenceUnits(new OSGiJPAPXml(applInfo, archiveName, JPAPuScope.Web_Scope, puRoot, warClassLoader, pxml));
        }

        // Obtain any persistenc.xml in WEB-INF/lib/*.jar. This includes 'utility'
        // jars and web fragments. Any PUs found are WEB scoped and considered to
        // be in the WAR, so just use the WAR archiveName (don't use a root prefix
        // that is prepended to the jar/fragment name).
        Entry webInfLib = warContainer.getEntry("WEB-INF/lib/");
        if (webInfLib != null) {
            try {
                Container webInfLibContainer = webInfLib.adapt(Container.class);
                processLibraryJarPersistenceXml(applInfo, webInfLibContainer, archiveName, null, JPAPuScope.Web_Scope, warClassLoader);
            } catch (UnableToAdaptException ex) {
                // Should never occur... just propagate failure
                throw new RuntimeException("Failure locating persistence.xml", ex);
            }
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "processWebModulePersistenceXml : " + applInfo.getApplName() +
                        "#" + warContainer);
    }

    /**
     * Locates and processes persistence.xml file in an EJB module. <p>
     *
     * @param applInfo the application archive information
     * @param module   the EJB module archive information
     */
    private void processEJBModulePersistenceXml(JPAApplInfo applInfo, ContainerInfo module, ClassLoader appClassloader) {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "processEJBModulePersistenceXml : " + applInfo.getApplName() +
                         "#" + module.getName());

        String archiveName = module.getName();
        Container ejbContainer = module.getContainer();
        ClassLoader ejbClassLoader = appClassloader;

        // ------------------------------------------------------------------------
        // JPA 2.0 Specification - 8.2 Persistence Unit Packaging
        //
        // A persistence unit is defined by a persistence.xml file. The jar file or
        // directory whose META-INF directory contains the persistence.xml file is
        // termed the root of the persistence unit. In Java EE environments, the
        // root of a persistence unit may be one of the following:
        //
        // -> an EJB-JAR file
        // ------------------------------------------------------------------------

        // Obtain persistence.xml in META-INF
        Entry pxml = ejbContainer.getEntry("META-INF/persistence.xml");
        if (pxml != null) {
            String appName = applInfo.getApplName();
            URL puRoot = getPXmlRootURL(appName, archiveName, pxml);
            applInfo.addPersistenceUnits(new OSGiJPAPXml(applInfo, archiveName, JPAPuScope.EJB_Scope, puRoot, ejbClassLoader, pxml));
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "processEJBModulePersistenceXml : " + applInfo.getApplName() +
                        "#" + module.getName());
    }

    /**
     * Locates and processes persistence.xml file in an Application Client module. <p>
     *
     * @param applInfo the application archive information
     * @param module   the client module archive information
     */
    private void processClientModulePersistenceXml(JPAApplInfo applInfo, ContainerInfo module, ClassLoader loader) {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "processClientModulePersistenceXml : " + applInfo.getApplName() +
                         "#" + module.getName());

        String archiveName = module.getName();
        Container clientContainer = module.getContainer();
        ClassLoader clientClassLoader = loader;

        // ------------------------------------------------------------------------
        // JPA 2.0 Specification - 8.2 Persistence Unit Packaging
        //
        // A persistence unit is defined by a persistence.xml file. The jar file or
        // directory whose META-INF directory contains the persistence.xml file is
        // termed the root of the persistence unit. In Java EE environments, the
        // root of a persistence unit may be one of the following:
        //
        // -> an EJB-JAR file
        // ------------------------------------------------------------------------

        // Obtain persistence.xml in META-INF
        Entry pxml = clientContainer.getEntry("META-INF/persistence.xml");
        if (pxml != null) {
            String appName = applInfo.getApplName();
            URL puRoot = getPXmlRootURL(appName, archiveName, pxml);
            applInfo.addPersistenceUnits(new OSGiJPAPXml(applInfo, archiveName, JPAPuScope.Client_Scope, puRoot, clientClassLoader, pxml));
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "processClientModulePersistenceXml : " + applInfo.getApplName() +
                        "#" + module.getName());
    }

    /**
     * Add any additional environment specific properties to the set of
     * integration-level properties used on the call to
     * PersistenceProvider.createContainerEntityManagerFactory.
     *
     * The ValidatorFactory is supported in WAS.
     *
     * @param xmlSchemaVersion       the schema version of the persistence.xml
     * @param integrationProperties  the current set of integration-level properties
     * @param applicationClassLoader the application's classloader. Used to create dynamic proxies for hibernate integration.
     */
    // F743-12524
    @Override
    public void addIntegrationProperties(String xmlSchemaVersion,
                                         Map<String, Object> integrationProperties, ClassLoader applicationClassLoader) {

        for (JPAEMFPropertyProvider propProvider : propProviderSRs.services()) {
            propProvider.updateProperties(integrationProperties, applicationClassLoader);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "addIntegrationProperties " + propProvider + " props: {0}", integrationProperties);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public String getDefaultJPAProviderClassName() {
        String def = (String) props.get("defaultPersistenceProvider");
        if (def == null || def.isEmpty()) {
            def = getJPAProviderIntegration().getProviderClassName();
        }

        return def;
    }

    @Override
    public boolean getCaptureEnhancedEntityClassBytecode() {
        Object ceecbProp = props.get("captureEnhancedEntityClassBytecode");
        if (ceecbProp != null && ceecbProp instanceof String) {
            return Boolean.parseBoolean((String) ceecbProp);
        } else {
            return false;
        }
    }

    private String getEffectiveDefaultJTADataSourceJNDIName() {
        return convertBlanksToNull((String) props.get("defaultJtaDataSourceJndiName"));
    }

    private String getEffectiveDefaultNonJTADataSourceJNDIName() {
        return convertBlanksToNull((String) props.get("defaultNonJtaDataSourceJndiName"));
    }

    @Override
    public String getDataSourceBindingName(String bindingName, boolean transactional) {
        if (bindingName == null) {
            bindingName = transactional ? getEffectiveDefaultJTADataSourceJNDIName() : getEffectiveDefaultNonJTADataSourceJNDIName();
        }

        if (bindingName != null && !bindingName.startsWith("java:")) {
            JPAResourceBindingImpl binding = null;
            for (Iterator<ServiceAndServiceReferencePair<ResourceBindingListener>> it = resourceBindingListeners.getServicesWithReferences(); it.hasNext();) {
                if (binding == null) {
                    Map<String, Object> properties = Collections.<String, Object> singletonMap("transactional", transactional);
                    binding = new JPAResourceBindingImpl(bindingName, properties);
                }
                binding.notify(it.next());
            }

            if (binding != null && binding.bindingNameSet) {
                bindingName = binding.bindingName;
            }
        }

        return bindingName;
    }

    @Override
    public boolean isIgnoreDataSourceErrors() {
        Boolean value = (Boolean) props.get("ignoreDataSourceErrors");
        return getJPARuntime().isIgnoreDataSourceErrors(value);
    }

    /**
     * Metatype doesn't allow fields without a default but we want
     * nulls for unspecified JNDI names so convert "" (our default)
     * to null.
     *
     * @param string
     * @return
     */
    private String convertBlanksToNull(String string) {
        if (string != null && string.trim().length() > 0) {
            return string;

        } else {
            return null;
        }
    }

    @Override
    public int getEntityManagerPoolCapacity() {
        int poolCapacity = (Integer) props.get("entityManagerPoolCapacity");
        if (poolCapacity >= 0 || poolCapacity <= 500) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "EMPoolCapacity = " + poolCapacity);
        } else {
            // Override with the default
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "EMPoolCapacity out of range (0,500) : " + poolCapacity);
            poolCapacity = -1;
        }

        return poolCapacity;

    }

    public Set<String> getExcludedAppNames() {
        List<String> list = Arrays.asList((String[]) props.get("excludedApplication"));
        return new HashSet<String>(list);
    }

    @Reference(name = REFERENCE_TRANSACTION_MANAGER,
               service = EmbeddableWebSphereTransactionManager.class)
    protected void setEmbeddableWebSphereTransactionManager(ServiceReference<EmbeddableWebSphereTransactionManager> ref) {
        ivTransactionManagerSR.setReference(ref);
    }

    protected void unsetEmbeddableWebSphereTransactionManager(ServiceReference<EmbeddableWebSphereTransactionManager> ref) {
        ivTransactionManagerSR.unsetReference(ref);
    }

    @Reference(name = REFERENCE_JPA_PROVIDER, service = JPAProviderIntegration.class)
    protected void setJPAProvider(ServiceReference<JPAProviderIntegration> jpaSR) {
        providerIntegrationSR.setReference(jpaSR);
    }

    protected void unsetJPAProvider(ServiceReference<JPAProviderIntegration> jpaSR) {
        providerIntegrationSR.unsetReference(jpaSR);

    }

    @Override
    public JPARuntime getJPARuntime() {
        return jpaRuntime.getServiceWithException();
    }

    @Reference(name = REFERENCE_JPA_RUNTIME,
               service = JPARuntime.class)
    protected void setJpaRuntime(ServiceReference<JPARuntime> reference) {
        jpaRuntime.setReference(reference);
    }

    protected void unsetJpaRuntime(ServiceReference<JPARuntime> reference) {
        jpaRuntime.unsetReference(reference);
    }

    @Reference(name = REFERENCE_CLASSLOADING_SERVICE, service = ClassLoadingService.class)
    protected void setClassLoadingService(ClassLoadingService ref) {
        classLoadingService = ref;
    }

    protected void unsetClassLoadingService(ClassLoadingService ref) {
        classLoadingService = null;
    }

    @Override
    public ClassLoader createThreadContextClassLoader(final ClassLoader appClassloader) {
        final ClassLoadingService cls = classLoadingService;
        return AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {

            @Override
            public ClassLoader run() {
                return cls.createThreadContextClassLoader(appClassloader);
            }
        });
    }

    @Override
    public void destroyThreadContextClassLoader(final ClassLoader tcclassloader) {
        final ClassLoadingService cls = classLoadingService;
        AccessController.doPrivileged(new PrivilegedAction() {
            @Override
            public Object run() {
                cls.destroyThreadContextClassLoader(tcclassloader);
                return null;
            }

        });
    }

    @Override
    public boolean isServerRuntime() {
        return server;
    }

    @Reference(service = LibertyProcess.class, target = "(wlp.process.type=server)", cardinality = ReferenceCardinality.OPTIONAL)
    protected void setLibertyProcess(ServiceReference<LibertyProcess> reference) {
        server = true;
    }

    protected void unsetLibertyProcess(ServiceReference<LibertyProcess> reference) {

    }

    @Reference(name = REFERENCE_CONTEXT_ACCESSOR,
               service = JPAExPcBindingContextAccessor.class,
               cardinality = ReferenceCardinality.OPTIONAL,
               policy = ReferencePolicy.DYNAMIC)
    protected void setContextAccessor(ServiceReference<JPAExPcBindingContextAccessor> reference) {
        ivContextAccessorSR.setReference(reference);
    }

    protected void unsetContextAccessor(ServiceReference<JPAExPcBindingContextAccessor> reference) {
        ivContextAccessorSR.unsetReference(reference);
    }

    @Reference(name = REFERENCE_RESOURCE_BINDING_LISTENERS,
               service = ResourceBindingListener.class,
               cardinality = ReferenceCardinality.MULTIPLE,
               policy = ReferencePolicy.DYNAMIC)
    protected void addResourceBindingListener(ServiceReference<ResourceBindingListener> reference) {
        resourceBindingListeners.addReference(reference);
    }

    protected void removeResourceBindingListener(ServiceReference<ResourceBindingListener> reference) {
        resourceBindingListeners.removeReference(reference);
    }

    @Reference(name = REFERENCE_JPA_PROPS_PROVIDER,
               service = JPAEMFPropertyProvider.class,
               cardinality = ReferenceCardinality.MULTIPLE,
               policy = ReferencePolicy.DYNAMIC)
    protected void addJPAEMFPropertyProvider(ServiceReference<JPAEMFPropertyProvider> reference) {
        propProviderSRs.addReference(reference);
    }

    protected void removeJPAEMFPropertyProvider(ServiceReference<JPAEMFPropertyProvider> reference) {
        propProviderSRs.removeReference(reference);
    }

    @Reference(name = REFERENCE_APP_COORD)
    protected void setAppRecycleCoordinator(ServiceReference<ApplicationRecycleCoordinator> ref) {}

    protected void unsetAppRecycleCoordinator(ServiceReference<ApplicationRecycleCoordinator> ref) {}

    @Override
    public void registerJPAExPcBindingContextAccessor(JPAExPcBindingContextAccessor accessor) {
        throw new UnsupportedOperationException("Not supported in Liberty");
    }

    @Override
    public JPAExPcBindingContextAccessor getExPcBindingContext() {
        return ivContextAccessorSR.getService();
    }

    @Override
    public UOWCurrent getUOWCurrent() {
        return (UOWCurrent) ivTransactionManagerSR.getService();
    }

    @Override
    public EmbeddableWebSphereTransactionManager getEmbeddableWebSphereTransactionManager() {
        return ivTransactionManagerSR.getService();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.jpa.JPAComponent#getServerLogDirectory()
     */
    @Override
    public File getServerLogDirectory() {
        String logLocation = TrConfigurator.getLogLocation();
        if (logLocation != null) {
            return new File(logLocation);
        } else {
            return null;
        }
    }

    @Override
    public void recycleJPAApplications() {
        // No need to recycle apps during server shutdown
        if (FrameworkState.isStopping())
            return;

        final Map<String, JPAApplInfo> appsToRestartMap = new HashMap<String, JPAApplInfo>();
        final Set<String> appsToRestart = new HashSet<String>();
        synchronized (applList) {
            appsToRestartMap.putAll(applList);
        }

        for (Map.Entry<String, JPAApplInfo> entry : appsToRestartMap.entrySet()) {
            if (entry.getValue().hasPersistenceUnitsDefined()) {
                appsToRestart.add(entry.getKey());
            }
        }
        appsToRestart.addAll(stuckApps);
        stuckApps.clear();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "Recycling JPA applications", appsToRestart);
        ApplicationRecycleCoordinator appCoord = (ApplicationRecycleCoordinator) priv.locateService(context, REFERENCE_APP_COORD);
        appCoord.recycleApplications(appsToRestart);
    }

    /*
     * com.ibm.wsspi.logging.Introspector implementation
     *
     */
    @Override
    public String getIntrospectorName() {
        return "JPARuntimeInspector";
    }

    @Override
    public String getIntrospectorDescription() {
        return "JPA Runtime Internal State Information";
    }

    @Override
    public void introspect(PrintWriter out) throws Exception {
        out.println("JPA Component State:");
        out.println();

        out.println("Service Properties:");
        Enumeration<String> keysEnum = props.keys();
        while (keysEnum.hasMoreElements()) {
            String key = keysEnum.nextElement();
            Object o = props.get(key);
            if (o != null && o.getClass().isArray()) {
                out.print("  " + key + " = [ ");
                Object[] objArr = (Object[]) o;
                if (objArr.length != 0) {
                    boolean first = true;
                    for (Object obj : objArr) {
                        if (first) {
                            first = false;
                        } else {
                            out.print(", ");
                        }
                        out.print(obj);
                    }
                }
                out.println(" ]");
            } else {
                out.println("  " + key + " = " + o);
            }

        }
        out.println();

        out.println("jpaRuntime = " + jpaRuntime.getService());
        out.println("Provider Runtime Integration Service = " + providerIntegrationSR.getService());

        out.println("Registered JPAEMFPropertyProvider Services:");
        Iterator<JPAEMFPropertyProvider> servicesIter = propProviderSRs.getServices();
        while (servicesIter.hasNext()) {
            out.println("   " + servicesIter.next());
        }
        out.println();

        // Collect all JPAApplInfo instances known by the JPA Runtime
        final Map<String, JPAApplInfo> appMap = new HashMap<String, JPAApplInfo>();
        synchronized (applList) {
            appMap.putAll(applList);
        }

        // Find all JPA enabled applications, scopeinfo, pxmlinfo, and persistence unit info
        JPAIntrospection.beginJPAIntrospection();
        try {
            for (Map.Entry<String, JPAApplInfo> entry : appMap.entrySet()) {
                final String appName = entry.getKey();
                final OSGiJPAApplInfo appl = (OSGiJPAApplInfo) entry.getValue();

                JPAIntrospection.beginApplicationVisit(appName, appl);
                try {
                    appl.introspect();
                } finally {
                    JPAIntrospection.endApplicationVisit();
                }
            }

            JPAIntrospection.executeIntrospectionAnalysis(out);
        } finally {
            JPAIntrospection.endJPAIntrospection();
        }
    }
}
