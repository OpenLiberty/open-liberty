/*******************************************************************************
 * Copyright (c) 2015, 2025 IBM Corporation and others.
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
package com.ibm.ws.cdi.liberty;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

import javax.enterprise.inject.spi.CDIProvider;

import org.jboss.weld.bootstrap.WeldStartup;
import org.jboss.weld.ejb.spi.EjbServices;
import org.jboss.weld.security.spi.SecurityServices;
import org.jboss.weld.serialization.spi.ProxyServices;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.ejs.util.Util;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.cdi.CDIException;
import com.ibm.ws.cdi.CDIService;
import com.ibm.ws.cdi.extension.WebSphereCDIExtension;
import com.ibm.ws.cdi.impl.AbstractCDIRuntime;
import com.ibm.ws.cdi.impl.CDIContainerImpl;
import com.ibm.ws.cdi.internal.archive.liberty.CDILibertyRuntime;
import com.ibm.ws.cdi.internal.archive.liberty.RuntimeFactory;
import com.ibm.ws.cdi.internal.config.CDIConfiguration;
import com.ibm.ws.cdi.internal.interfaces.Application;
import com.ibm.ws.cdi.internal.interfaces.ArchiveType;
import com.ibm.ws.cdi.internal.interfaces.BeansXmlParser;
import com.ibm.ws.cdi.internal.interfaces.BuildCompatibleExtensionFinder;
import com.ibm.ws.cdi.internal.interfaces.CDIArchive;
import com.ibm.ws.cdi.internal.interfaces.CDIContainerEventManager;
import com.ibm.ws.cdi.internal.interfaces.CDIUtils;
import com.ibm.ws.cdi.internal.interfaces.ContextBeginnerEnder;
import com.ibm.ws.cdi.internal.interfaces.EjbEndpointService;
import com.ibm.ws.cdi.internal.interfaces.ExtensionArchive;
import com.ibm.ws.cdi.internal.interfaces.ExtensionArchiveFactory;
import com.ibm.ws.cdi.internal.interfaces.ExtensionArchiveProvider;
import com.ibm.ws.cdi.internal.interfaces.TransactionService;
import com.ibm.ws.cdi.internal.interfaces.WebSphereCDIDeployment;
import com.ibm.ws.cdi.proxy.ProxyServicesImpl;
import com.ibm.ws.container.service.app.deploy.ApplicationInfo;
import com.ibm.ws.container.service.metadata.MetaDataSlotService;
import com.ibm.ws.container.service.metadata.extended.DeferredMetaDataFactory;
import com.ibm.ws.container.service.state.ApplicationStateListener;
import com.ibm.ws.container.service.state.StateChangeException;
import com.ibm.ws.kernel.LibertyProcess;
import com.ibm.ws.resource.ResourceRefConfigFactory;
import com.ibm.ws.runtime.metadata.ApplicationMetaData;
import com.ibm.ws.runtime.metadata.MetaData;
import com.ibm.ws.runtime.metadata.MetaDataSlot;
import com.ibm.ws.runtime.metadata.ModuleMetaData;
import com.ibm.wsspi.adaptable.module.AdaptableModuleFactory;
import com.ibm.wsspi.artifact.factory.ArtifactContainerFactory;
import com.ibm.wsspi.classloading.ClassLoadingService;
import com.ibm.wsspi.injectionengine.InjectionEngine;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceSet;
import com.ibm.wsspi.kernel.service.utils.ServiceAndServiceReferencePair;

import io.openliberty.cdi.spi.CDIExtensionMetadata;

/**
 * This class is to get hold all necessary services.
 */
@Component(name = "com.ibm.ws.cdi.liberty.CDIRuntimeImpl", service = { ApplicationStateListener.class, CDIService.class,
                                                                       CDIProvider.class },
           property = { "service.vendor=IBM", "service.ranking:Integer=100" }) //CDI must shut down before EJB as EJBs can have a cdi application scope and according to the CDI spec "jakarta.enterprise.event.Shutdown is not after @BeforeDestroyed(ApplicationScoped.class)"
                                                                                                                                                                                                                             //CDI must also start up after injection engine, as CDI can trigger JNDI lookups in app code as part of starting up and that code can do JNDI lookups
public class CDIRuntimeImpl extends AbstractCDIRuntime implements ApplicationStateListener, CDIService, CDILibertyRuntime, CDIProvider {
    private static final TraceComponent tc = Tr.register(CDIRuntimeImpl.class);

    private final AtomicServiceReference<MetaDataSlotService> metaDataSlotServiceSR = new AtomicServiceReference<MetaDataSlotService>("metaDataSlotService");
    private final AtomicServiceReference<EjbEndpointService> ejbEndpointServiceSR = new AtomicServiceReference<EjbEndpointService>("ejbEndpointService");

    /** Reference for delayed activation of ClassLoadingService */
    private final AtomicServiceReference<ClassLoadingService> classLoadingSRRef = new AtomicServiceReference<ClassLoadingService>("classLoadingService");

    private final AtomicServiceReference<EjbServices> ejbServices = new AtomicServiceReference<EjbServices>("ejbServices");
    private final AtomicServiceReference<TransactionService> transactionService = new AtomicServiceReference<TransactionService>("transactionService");
    private final AtomicServiceReference<SecurityServices> securityServices = new AtomicServiceReference<SecurityServices>("securityServices");

    /** Reference for all portal extensions **/
    private final ConcurrentServiceReferenceSet<WebSphereCDIExtension> extensionsSR = new ConcurrentServiceReferenceSet<WebSphereCDIExtension>("extensionService");

    /** Reference for extensions added via SPI **/
    private final ConcurrentServiceReferenceSet<CDIExtensionMetadata> spiExtensionsSR = new ConcurrentServiceReferenceSet<CDIExtensionMetadata>("spiExtensionService");

    private final AtomicServiceReference<ArtifactContainerFactory> containerFactorySRRef = new AtomicServiceReference<ArtifactContainerFactory>("containerFactory");
    private final AtomicServiceReference<AdaptableModuleFactory> adaptableModuleFactorySRRef = new AtomicServiceReference<AdaptableModuleFactory>("adaptableModuleFactory");
    private final AtomicServiceReference<InjectionEngine> injectionEngineServiceRef = new AtomicServiceReference<InjectionEngine>("injectionEngine");
    private final AtomicServiceReference<ScheduledExecutorService> scheduledExecutorServiceRef = new AtomicServiceReference<ScheduledExecutorService>("scheduledExecutorService");
    private final AtomicServiceReference<ExecutorService> executorServiceRef = new AtomicServiceReference<ExecutorService>("executorService");
    private final AtomicServiceReference<ExecutorService> managedExecutorServiceRef = new AtomicServiceReference<ExecutorService>("managedExecutorService");

    private final AtomicServiceReference<ResourceRefConfigFactory> resourceRefConfigFactoryRef = new AtomicServiceReference<ResourceRefConfigFactory>("resourceRefConfigFactory");

    private final AtomicServiceReference<DeferredMetaDataFactory> deferredMetaDataFactoryRef = new AtomicServiceReference<DeferredMetaDataFactory>("cdiDeferredMetaDataFactoryImpl");

    @Reference
    private BeansXmlParser beansXmlParser;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
    private volatile BuildCompatibleExtensionFinder bceFinder;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
    private volatile CDIContainerEventManager cdiContainerEventManager;

    @Reference
    private CDIConfiguration cdiContainerConfig;

    @Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
    private volatile List<ExtensionArchiveProvider> extensionArchiveProviders;

    @Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
    private volatile List<ExtensionArchiveFactory> extensionArchiveFactories;

    private MetaDataSlot applicationSlot;
    private boolean isClientProcess;
    private ProxyServicesImpl proxyServices;

    public void activate(ComponentContext cc) {
        //This emmits logging in a static block.
        //OpenTelemetry can have a circular dependency if that loging goes into OTel
        //And the application calls CDI.current() during its OTel configuration extensions.
        //So force it early
        WeldStartup ws = new WeldStartup();

        metaDataSlotServiceSR.activate(cc);
        ejbEndpointServiceSR.activate(cc);
        classLoadingSRRef.activate(cc);
        extensionsSR.activate(cc);
        spiExtensionsSR.activate(cc);
        applicationSlot = metaDataSlotServiceSR.getServiceWithException().reserveMetaDataSlot(ApplicationMetaData.class);
        ejbServices.activate(cc);
        securityServices.activate(cc);
        transactionService.activate(cc);
        containerFactorySRRef.activate(cc);
        scheduledExecutorServiceRef.activate(cc);
        executorServiceRef.activate(cc);
        adaptableModuleFactorySRRef.activate(cc);
        injectionEngineServiceRef.activate(cc);
        resourceRefConfigFactoryRef.activate(cc);
        managedExecutorServiceRef.activate(cc);
        deferredMetaDataFactoryRef.activate(cc);

        this.runtimeFactory = new RuntimeFactory(this);
        this.proxyServices = new ProxyServicesImpl();
        start();
    }

    public void deactivate(ComponentContext cc) {
        stop();

        this.runtimeFactory = null;
        this.proxyServices = null;

        metaDataSlotServiceSR.deactivate(cc);
        ejbEndpointServiceSR.deactivate(cc);
        classLoadingSRRef.deactivate(cc);
        ejbServices.deactivate(cc);
        securityServices.deactivate(cc);
        transactionService.deactivate(cc);
        extensionsSR.deactivate(cc);
        spiExtensionsSR.deactivate(cc);
        containerFactorySRRef.deactivate(cc);
        scheduledExecutorServiceRef.deactivate(cc);
        executorServiceRef.deactivate(cc);
        adaptableModuleFactorySRRef.deactivate(cc);
        injectionEngineServiceRef.deactivate(cc);
        resourceRefConfigFactoryRef.deactivate(cc);
        managedExecutorServiceRef.deactivate(cc);
        deferredMetaDataFactoryRef.deactivate(cc);
    }

    @Reference(name = "cdiDeferredMetaDataFactoryImpl", service = DeferredMetaDataFactory.class, target = "(deferredMetaData=CDI)")
    protected void setCDIDeferredMetaDataFactoryImpl(ServiceReference<DeferredMetaDataFactory> ref) {
        deferredMetaDataFactoryRef.setReference(ref);
    }

    protected void unsetCDIDeferredMetaDataFactoryImpl(ServiceReference<DeferredMetaDataFactory> ref) {
        deferredMetaDataFactoryRef.unsetReference(ref);
    }

    @Reference(name = "executorService", service = ExecutorService.class)
    protected void setExecutorService(ServiceReference<ExecutorService> ref) {
        executorServiceRef.setReference(ref);
    }

    protected void unsetExecutorService(ServiceReference<ExecutorService> ref) {
        executorServiceRef.unsetReference(ref);
    }

    @Reference(name = "managedExecutorService", service = ExecutorService.class, target = "(id=DefaultManagedExecutorService)", policyOption = ReferencePolicyOption.GREEDY,
               cardinality = ReferenceCardinality.OPTIONAL)
    protected void setManagedExecutorService(ServiceReference<ExecutorService> ref) {
        managedExecutorServiceRef.setReference(ref);
    }

    protected void unsetManagedExecutorService(ServiceReference<ExecutorService> ref) {
        managedExecutorServiceRef.unsetReference(ref);
    }

    @Reference(name = "scheduledExecutorService", service = ScheduledExecutorService.class, target = "(deferrable=false)")
    protected void setScheduledExecutorService(ServiceReference<ScheduledExecutorService> ref) {
        scheduledExecutorServiceRef.setReference(ref);
    }

    protected void unsetScheduledExecutorService(ServiceReference<ScheduledExecutorService> ref) {
        scheduledExecutorServiceRef.unsetReference(ref);
    }

    @Reference(name = "containerFactory", service = ArtifactContainerFactory.class, target = "(&(category=DIR)(category=JAR)(category=BUNDLE))")
    protected void setContainerFactory(ServiceReference<ArtifactContainerFactory> ref) {
        containerFactorySRRef.setReference(ref);
    }

    protected void unsetContainerFactory(ServiceReference<ArtifactContainerFactory> ref) {
        containerFactorySRRef.unsetReference(ref);
    }

    @Reference(name = "adaptableModuleFactory", service = AdaptableModuleFactory.class)
    protected void setAdaptableModuleFactory(ServiceReference<AdaptableModuleFactory> ref) {
        adaptableModuleFactorySRRef.setReference(ref);
    }

    protected void unsetAdaptableModuleFactory(ServiceReference<AdaptableModuleFactory> ref) {
        adaptableModuleFactorySRRef.unsetReference(ref);
    }

    @Reference(name = "extensionService", service = WebSphereCDIExtension.class, policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE)
    protected void setExtensionService(ServiceReference<WebSphereCDIExtension> reference) {
        extensionsSR.addReference(reference);
    }

    protected void unsetExtensionService(ServiceReference<WebSphereCDIExtension> reference) {
        extensionsSR.removeReference(reference);
        //the cdi container has a cache of ExtensionArchives ... remove this extension from that cache
        CDIContainerImpl cdiContainer = getCDIContainer();
        if (cdiContainer != null) {
            cdiContainer.removeRuntimeExtensionArchive(reference);
        }
    }

    @Reference(name = "spiExtensionService", service = CDIExtensionMetadata.class, policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE)
    protected void setSPIExtensionService(ServiceReference<CDIExtensionMetadata> reference) {
        spiExtensionsSR.addReference(reference);
    }

    protected void unsetSPIExtensionService(ServiceReference<CDIExtensionMetadata> reference) {
        spiExtensionsSR.removeReference(reference);
        //the cdi container has a cache of ExtensionArchives ... remove this extension from that cache
        CDIContainerImpl cdiContainer = getCDIContainer();
        if (cdiContainer != null) {
            cdiContainer.removeRuntimeExtensionArchiveMetaData(reference);
        }
    }

    @Reference(name = "transactionService", service = TransactionService.class, policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.OPTIONAL)
    protected void setTransactionService(ServiceReference<TransactionService> transactionService) {
        this.transactionService.setReference(transactionService);
    }

    protected void unsetTransactionService(ServiceReference<TransactionService> transactionService) {
        this.transactionService.unsetReference(transactionService);
    }

    @Reference(name = "securityServices", service = SecurityServices.class)
    protected void setSecurityServices(ServiceReference<SecurityServices> securityServices) {
        this.securityServices.setReference(securityServices);
    }

    protected void unsetSecurityServices(ServiceReference<SecurityServices> securityServices) {
        this.securityServices.unsetReference(securityServices);
    }

    @Reference(name = "ejbServices", service = EjbServices.class, policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.OPTIONAL)
    protected void setEjbServices(ServiceReference<EjbServices> ejbServices) {
        this.ejbServices.setReference(ejbServices);
    }

    protected void unsetEjbServices(ServiceReference<EjbServices> ejbServices) {
        this.ejbServices.unsetReference(ejbServices);
    }

    @Reference(name = "metaDataSlotService", service = MetaDataSlotService.class)
    protected void setMetaDataSlotService(ServiceReference<MetaDataSlotService> reference) {
        metaDataSlotServiceSR.setReference(reference);
    }

    protected void unsetMetaDataSlotService(ServiceReference<MetaDataSlotService> reference) {
        metaDataSlotServiceSR.unsetReference(reference);
    }

    @Reference(name = "ejbEndpointService", service = EjbEndpointService.class, policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.OPTIONAL)
    protected void setEjbEndpointService(ServiceReference<EjbEndpointService> reference) {
        ejbEndpointServiceSR.setReference(reference);
    }

    protected void unsetEjbEndpointService(ServiceReference<EjbEndpointService> reference) {
        ejbEndpointServiceSR.unsetReference(reference);
    }

    @Reference(name = "resourceRefConfigFactory", service = ResourceRefConfigFactory.class)
    protected void setResourceRefConfigFactory(ServiceReference<ResourceRefConfigFactory> resourceRefConfigFactory) {
        this.resourceRefConfigFactoryRef.setReference(resourceRefConfigFactory);
    }

    protected void unsetResourceRefConfigFactory(ServiceReference<ResourceRefConfigFactory> resourceRefConfigFactory) {
        this.resourceRefConfigFactoryRef.unsetReference(resourceRefConfigFactory);
    }

    /**
     * DS method for setting the class loading service reference.
     *
     * @param service
     */
    @Reference(name = "classLoadingService", service = ClassLoadingService.class)
    protected void setClassLoadingService(ServiceReference<ClassLoadingService> ref) {
        classLoadingSRRef.setReference(ref);
    }

    /**
     * DS method for unsetting the class loading service reference.
     *
     * @param service
     */
    protected void unsetClassLoadingService(ServiceReference<ClassLoadingService> ref) {
        classLoadingSRRef.unsetReference(ref);
    }

    @Reference(name = "injectionEngine", service = InjectionEngine.class)
    protected void setInjectionEngine(ServiceReference<InjectionEngine> ref) {
        injectionEngineServiceRef.setReference(ref);
    }

    protected void unsetInjectionEngine(ServiceReference<InjectionEngine> ref) {
        injectionEngineServiceRef.unsetReference(ref);
    }

    /**
     * DS method for setting the client process - this method will only be invoked
     * in the client Container process (due to the target). There is no need to
     * unset it.
     *
     * @param sr
     */
    @Reference(name = "libertyProcess", service = LibertyProcess.class, target = "(wlp.process.type=client)", cardinality = ReferenceCardinality.OPTIONAL)
    protected void setLibertyProcess(ServiceReference<LibertyProcess> sr) {
        isClientProcess = true;
    }

    @Override
    public ResourceRefConfigFactory getResourceRefConfigFactory() {
        return resourceRefConfigFactoryRef.getService();
    }

    @Override
    public TransactionService getTransactionService() {
        return transactionService.getService();
    }

    @Override
    public SecurityServices getSecurityServices() {
        return securityServices.getService();
    }

    @Override
    public Iterator<ServiceAndServiceReferencePair<WebSphereCDIExtension>> getExtensionServices() {
        return extensionsSR.getServicesWithReferences();
    }

    @Override
    public Iterator<ServiceAndServiceReferencePair<CDIExtensionMetadata>> getSPIExtensionServices() {
        return spiExtensionsSR.getServicesWithReferences();
    }

    @Override
    public ArtifactContainerFactory getArtifactContainerFactory() {
        return containerFactorySRRef.getService();
    }

    @Override
    public AdaptableModuleFactory getAdaptableModuleFactory() {
        return adaptableModuleFactorySRRef.getService();
    }

    @Override
    public MetaDataSlot getApplicationSlot() {
        return applicationSlot;
    }

    @Override
    public EjbEndpointService getEjbEndpointService() {
        return ejbEndpointServiceSR.getService();
    }

    @Override
    public InjectionEngine getInjectionEngine() {
        return injectionEngineServiceRef.getService();
    }

    @Override
    public ScheduledExecutorService getScheduledExecutorService() {
        return scheduledExecutorServiceRef.getService();
    }

    @Override
    public ExecutorService getExecutorService() {
        ExecutorService managedExecutorService = managedExecutorServiceRef.getService();
        if (managedExecutorService != null) {
            return managedExecutorService;
        } else {
            return executorServiceRef.getService();
        }
    }

    @Override
    public boolean isClientProcess() {
        return isClientProcess;
    }

    /** {@inheritDoc} */
    @SuppressWarnings("resource")
    @Override
    public void applicationStarting(ApplicationInfo appInfo) throws StateChangeException {
        /*
         * On twas at this point the TCCL is completely unrelated to the app that's starting. On Liberty it's a ContextFinder object.
         * This context finder is able to correctly load classes if it is invoked in any WAR inside an EAR with many wars as
         * ContextFinder will delegate loading classes to the correct ClassLoader.
         *
         * However if the TCCL is used as the key to the map, for example by DeltaSpike's configuration code. Then it will not
         * work. The same ContextFinder object will be used as a key regardless of which module DeltaSpike is looking at.
         *
         * While the ContextFinder has the advantage of being able to load classes, setting the TCCL like I do below has the advantage
         * of keeping Liberty and twas in sync.
         *
         * Given that both options have an advantage and a drawback I chose to keep the two servers in sync. And am documenting why I
         * did so with this comment.
         */

        try {
            Application application = this.runtimeFactory.newApplication(appInfo);
            /* if there is no app classes info then the app manager is not in control of this app */
            if (!application.hasModules()) {
                this.runtimeFactory.removeApplication(appInfo);
                return;
            }

            ClassLoader appCL = getRealAppClassLoader(application);
            if (appCL != null) {
                ClassLoader appTCCL = classLoadingSRRef.getServiceWithException().createThreadContextClassLoader(appCL);
                application.setTCCL(appTCCL);
            }

            for (CDIArchive archive : application.getModuleArchives()) {
                registerDeferedMetaData(archive);
            }

            try (ContextBeginnerEnder cbe = createContextBeginnerEnder().extractComponentMetaData(application).extractTCCL(application).beginContext()) {
                WebSphereCDIDeployment webSphereCDIDeployment = getCDIContainer().startInitialization(application);
                if (webSphereCDIDeployment != null) {
                    getCDIContainer().endInitialization(webSphereCDIDeployment);//This split is just to keep the CDIContainerImpl code conistant across liberty & websphere.
                }
            }
        } catch (CDIException e) {
            throw new StateChangeException(e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws StateChangeException
     */
    @Override
    public void applicationStopped(ApplicationInfo appInfo) {
        Application application = this.runtimeFactory.removeApplication(appInfo);
        /* ignore apps not controlled by the app manager */
        if (application == null) {
            return;
        }
        try {
            getCDIContainer().applicationStopped(application);
        } catch (CDIException e) {
            //FFDC and carry on
        } finally {
            try {
                for (CDIArchive archive : application.getModuleArchives()) {
                    DeferredMetaDataFactory metaDataFactory = deferredMetaDataFactoryRef.getService();
                    CDIDeferredMetaDataFactoryImpl cdiMetaDataFactory = (CDIDeferredMetaDataFactoryImpl) metaDataFactory;
                    cdiMetaDataFactory.removeComponentMetaData(archive);
                }
            } catch (CDIException e) {
                //FFDC and carry on
            } finally {
                // Clean up the application TCCL created for startup
                // Must do this at shutdown since it's possible for the app to hold onto it and use it after startup
                ClassLoader appTCCL = application.getTCCL();
                if (appTCCL != null) {
                    classLoadingSRRef.getServiceWithException().destroyThreadContextClassLoader(appTCCL);
                }
                application.setTCCL(null);
            }
        }
    }

    @Override
    public void applicationStarted(ApplicationInfo appInfo) throws StateChangeException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, Util.identity(this), "applicationStarted", appInfo);
        }

        try {
            Application application = this.runtimeFactory.newApplication(appInfo);
            if (application != null) {
                getCDIContainer().applicationStarted(application);
            }
        } catch (CDIException e) {
            //FFDC and carry on
        }
    }

    @Override
    public void applicationStopping(ApplicationInfo appInfo) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, Util.identity(this), "applicationStopping", appInfo);
        }
        try {
            Application application = this.runtimeFactory.newApplication(appInfo);
            if (application != null) {
                getCDIContainer().applicationStopping(application);
            }
        } catch (CDIException e) {
            //FFDC and carry on
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean isImplicitBeanArchivesScanningDisabled(CDIArchive archive) {
        return this.cdiContainerConfig.isImplicitBeanArchivesScanningDisabled();
    }

    /** {@inheritDoc} */
    @Override
    public ExtensionArchive getExtensionArchiveForBundle(Bundle bundle,
                                                         Set<String> extraClasses,
                                                         Set<String> extraAnnotations,
                                                         boolean applicationBDAsVisible,
                                                         boolean extClassesOnly,
                                                         Set<String> spiExtensionClasses) throws CDIException {

        ExtensionArchive extensionArchive = runtimeFactory.getExtensionArchiveForBundle(bundle, extraClasses, extraAnnotations, applicationBDAsVisible, extClassesOnly,
                                                                                        spiExtensionClasses);

        return extensionArchive;
    }

    /** {@inheritDoc} */
    @Override
    public ProxyServices getProxyServices() {
        return proxyServices;
    }

    private void registerDeferedMetaData(CDIArchive archive) throws CDIException {

        JndiHelperComponentMetaData cmd = null;

        MetaData metaData = archive.getMetaData();
        if (archive.isModule()) {
            ModuleMetaData moduleMetaData = (ModuleMetaData) metaData;
            cmd = new JndiHelperComponentMetaData(moduleMetaData);
        } else {
            ApplicationMetaData applicationMetaData = (ApplicationMetaData) metaData;
            cmd = new JndiHelperComponentMetaData(applicationMetaData);

        }

        DeferredMetaDataFactory metaDataFactory = deferredMetaDataFactoryRef.getService();
        CDIDeferredMetaDataFactoryImpl cdiMetaDataFactory = (CDIDeferredMetaDataFactoryImpl) metaDataFactory;

        //To ensure even delayed async calls can find this, this gets cleaned up in ApplicationStopped, not endContext().
        //However which factory is uesd is controlled by ComponentMetaDataAccessorImpl, if a JndiHelperComponentMetaData
        //is on the thread CDIDeferredMetaDataFactoryImpl will be used, otherwise something else will.
        //So leaving the data inside CDIDeferredMetaDataFactoryImpl is just a precaution encase a very long lived thread
        //holds onto a JndiHelperComponentMetaData beyond application startup.
        cdiMetaDataFactory.registerComponentMetaData(archive, cmd);
    }

    /**
     * If we are deploying a WAR file (which gets wrapped into a synthetic EAR by WAS)
     * we need to take the webapp classloader, because the surrounding EAR class loader
     * actually doesn't even contain a single class...
     */
    public ClassLoader getRealAppClassLoader(Application application) {
        try {
            List<CDIArchive> moduleArchives = new ArrayList<CDIArchive>(application.getModuleArchives());
            if (moduleArchives.isEmpty()) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, Util.identity(this), "Application {0} has no modules so no thread context class loader will be set, this is expected for an OSGI app.",
                             application);
                }
                return null;
            }
            if (moduleArchives.size() == 1 && ArchiveType.WEB_MODULE == moduleArchives.get(0).getType()) {
                return moduleArchives.get(0).getClassLoader();
            }

            return application.getClassLoader();
        } catch (CDIException e) {
            return null;
        }
    }

    @Override
    public boolean isWeldProxy(Class clazz) {
        return CDIUtils.isWeldProxy(clazz);
    }

    @Override
    public boolean isWeldProxy(Object obj) {
        return CDIUtils.isWeldProxy(obj);
    }

    /** {@inheritDoc} */
    @Override
    public BeansXmlParser getBeansXmlParser() {
        return this.beansXmlParser;
    }

    /** {@inheritDoc} */
    @Override
    public CDIContainerEventManager getCDIContainerEventManager() {
        return this.cdiContainerEventManager;
    }

    /** {@inheritDoc} */
    @Override
    public Collection<ExtensionArchiveProvider> getExtensionArchiveProviders() {
        return extensionArchiveProviders;
    }

    /** {@inheritDoc} */
    @Override
    public Collection<ExtensionArchiveFactory> getExtensionArchiveFactories() {
        return extensionArchiveFactories;
    }

    /** {@inheritDoc} */
    @Override
    public BuildCompatibleExtensionFinder getBuildCompatibleExtensionFinder() {
        return bceFinder;
    }

    /** {@inheritDoc} */
    @Override
    public ContextBeginnerEnder createContextBeginnerEnder() {
        return new ContextBeginnerEnderImpl();
    }

    /** {@inheritDoc} */
    @Override
    public boolean isContextBeginnerEnderActive() {
        return ContextBeginnerEnderImpl.isActive();
    }

    /** {@inheritDoc} */
    @Override
    public ContextBeginnerEnder cloneActiveContextBeginnerEnder() {
        ContextBeginnerEnderImpl contextBeginnerEnder = ContextBeginnerEnderImpl.getCurrentlyActive();
        if (contextBeginnerEnder == null) {
            return null;
        }
        return contextBeginnerEnder.clone();
    }

    //The System Property which enables Weld Development Mode. They have been removed from liberty.
    //But it we want to issue a warning message if appropriate
    private final static String DEVELOPMENT_MODE = "org.jboss.weld.development";

    @SuppressWarnings("unused")
    private static final boolean developmentMode = AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
        @Override
        public Boolean run() {
            String developmentModeStr = System.getProperty(DEVELOPMENT_MODE);
            Boolean developmentMode = Boolean.valueOf(developmentModeStr);
            if (developmentMode) {
                Tr.warning(tc, "dev.mode.enabled.CWOWB1020W");
            }
            return developmentMode;
        }
    });
}
