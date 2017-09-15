/*******************************************************************************
 * Copyright (c) 2012, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.clientcontainer.internal;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.security.auth.callback.CallbackHandler;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.csi.J2EENameFactory;
import com.ibm.ws.clientcontainer.metadata.CallbackHandlerProvider;
import com.ibm.ws.clientcontainer.metadata.ClientModuleMetaData;
import com.ibm.ws.container.service.app.deploy.extended.ExtendedModuleInfo;
import com.ibm.ws.container.service.app.deploy.extended.ModuleRuntimeContainer;
import com.ibm.ws.container.service.metadata.MetaDataException;
import com.ibm.ws.container.service.metadata.extended.DeferredMetaDataFactory;
import com.ibm.ws.container.service.state.StateChangeException;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.javaee.dd.client.ApplicationClient;
import com.ibm.ws.javaee.dd.clientbnd.ApplicationClientBnd;
import com.ibm.ws.kernel.LibertyProcess;
import com.ibm.ws.kernel.launch.service.ClientRunner;
import com.ibm.ws.managedobject.ManagedObjectService;
import com.ibm.ws.resource.ResourceRefConfigFactory;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.runtime.metadata.ModuleMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;
import com.ibm.ws.threading.FutureMonitor;
import com.ibm.ws.transport.iiop.spi.ClientORBRef;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.classloading.ClassLoadingService;
import com.ibm.wsspi.injectionengine.InjectionEngine;
import com.ibm.wsspi.injectionengine.InjectionException;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

@Component(service = { ModuleRuntimeContainer.class, DeferredMetaDataFactory.class }, property = { "type=client", "deferredMetaData=CLIENT" })
public class ClientModuleRuntimeContainer implements ModuleRuntimeContainer, DeferredMetaDataFactory {
    private FutureMonitor futureMonitor;
    private BundleContext bundleContext;
    private ServiceRegistration<CallbackHandlerProvider> serviceReg;
    private InjectionEngine injectionEngine;
    // The current design is that only a single client module can be
    // active in an EAR.  When multiple client modules exist in the EAR
    // only the specified "default" client module is processed for metadata,
    // injected with resources, and executed.  The other modules are effectively
    // non-existent.  Meaning that it is impossible for one client module (or
    // any other module for that matter) to depend on resources defined in
    // another client module.  In order to be consistent with other containers
    // and to be able to support multiple client modules in the same EAR at
    // some point in the future, we are using Maps with the client module
    // metadata as the key.  But until that design change occurs, this map
    // will have at most one entry.
    private final Map<ClientModuleMetaData, ApplicationClientBnd> appClientBnds = new HashMap<ClientModuleMetaData, ApplicationClientBnd>();
    private final Map<ClientModuleMetaData, CallbackHandler> callbackHandlers = new HashMap<ClientModuleMetaData, CallbackHandler>();
    private ResourceRefConfigFactory resourceRefConfigFactory;
    private LibertyProcess libertyProcess;
    private J2EENameFactory j2eeNameFactory;
    private final Map<J2EEName, ComponentMetaData> activeCMDs = new HashMap<J2EEName, ComponentMetaData>();
    private boolean runningInClient;
    private ClassLoadingService classLoadingService;
    private final AtomicServiceReference<ManagedObjectService> managedObjectServiceRef = new AtomicServiceReference<ManagedObjectService>("managedObjectService");
    private ClassLoader appContextClassLoader;
    private final CountDownLatch latch = new CountDownLatch(1);
    private ClientRunnerImpl clientRunner;

    @Reference
    protected void setClassLoadingService(ClassLoadingService classLoadingService) {
        this.classLoadingService = classLoadingService;
    }

    protected void unsetClassLoadingService(ClassLoadingService classLoadingService) {
        this.classLoadingService = null;
    }

    @Reference(name = "managedObjectService",
                    service = ManagedObjectService.class,
                    policy = ReferencePolicy.DYNAMIC,
                    policyOption = ReferencePolicyOption.GREEDY)
    protected void setManagedObjectService(ServiceReference<ManagedObjectService> ref) {
        this.managedObjectServiceRef.setReference(ref);
    }

    protected void unsetManagedObjectService(ServiceReference<ManagedObjectService> ref) {
        this.managedObjectServiceRef.unsetReference(ref);
    }

    @Reference
    protected void setFutureMonitor(FutureMonitor futureMonitor) {
        this.futureMonitor = futureMonitor;
    }

    protected void unsetFutureMonitor(FutureMonitor futureMonitor) {
        this.futureMonitor = null;
    }

    @Reference
    protected void setInjectionEngine(InjectionEngine injectionEngine) {
        this.injectionEngine = injectionEngine;
    }

    protected void unsetInjectionEngine(InjectionEngine injectionEngine) {
        // Nothing to do.
    }

    @Reference
    protected void setResourceRefConfigFactory(ResourceRefConfigFactory resourceRefConfigFactory) {
        this.resourceRefConfigFactory = resourceRefConfigFactory;
    }

    protected void unsetResourceRefConfigFactory(ResourceRefConfigFactory resourceRefConfigFactory) {
        // Nothing to do.
    }

    @Reference
    protected void setJ2EENameFactory(J2EENameFactory j2eeNameFactory) {
        this.j2eeNameFactory = j2eeNameFactory;
    }

    protected void unsetJ2EENameFactory(J2EENameFactory j2eeNameFactory) {
        if (this.j2eeNameFactory == j2eeNameFactory) {
            this.j2eeNameFactory = null;
        }
    }

    @Reference(service = LibertyProcess.class)
    protected void setLibertyProcess(ServiceReference<LibertyProcess> libertyProcessSR) {
        this.runningInClient = "client".equals(libertyProcessSR.getProperty("wlp.process.type"));
        this.libertyProcess = libertyProcessSR.getBundle().getBundleContext().getService(libertyProcessSR);
    }

    protected void unsetLibertyProcess(ServiceReference<LibertyProcess> libertyProcessSR) {
        // Nothing to do
    }

    @Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.OPTIONAL)
    protected void setORBRef(ClientORBRef orbRef) {
        latch.countDown();
    }

    protected void unsetORBRef(ClientORBRef orbRef) {

    }

    @Override
    public ModuleMetaData createModuleMetaData(ExtendedModuleInfo moduleInfo) throws MetaDataException {
        Container container = moduleInfo.getContainer();
        ClientModuleMetaDataImpl cmmd;
        Dictionary<String, String> props = new Hashtable<String, String>();
        try {
            J2EEName j2eeName = j2eeNameFactory.create(moduleInfo.getApplicationInfo().getName(), moduleInfo.getURI(), null);
            cmmd = new ClientModuleMetaDataImpl(container.adapt(ApplicationClient.class), moduleInfo, j2eeName);
            appClientBnds.put(cmmd, container.adapt(ApplicationClientBnd.class));
            CallbackHandlerProvider callbackHandlerProvider = new CallbackHandlerProviderImpl(moduleInfo, cmmd.getAppClient().getCallbackHandler());
            callbackHandlers.put(cmmd, callbackHandlerProvider.getCallbackHandler()); // callbackHandler can be null.

            // Register the service here so that Security can use it.
            props.put("service.vendor", "IBM");
            serviceReg = bundleContext.registerService(CallbackHandlerProvider.class, callbackHandlerProvider, props);
        } catch (UnableToAdaptException e) {
            throw new MetaDataException(e);
        }
        return cmmd;
    }

    @Override
    @FFDCIgnore(InterruptedException.class)
    public Future<Boolean> startModule(ExtendedModuleInfo moduleInfo) throws StateChangeException {
        try {
            latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e1) {
            AccessController.doPrivileged(new PrivilegedAction<Void>() {

                @Override
                public Void run() {
                    Thread.currentThread().interrupt();
                    return null;
                }
            });

            //well, we waited for a while, go ahead and try to start the client.
        }

        ClientModuleMetaData cmmd = (ClientModuleMetaData) moduleInfo.getMetaData();
        ComponentMetaData cmd = new ClientComponentMetaDataImpl(cmmd);
        ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().setDefaultCMD(cmd);
        String[] args = libertyProcess.getArgs();

        appContextClassLoader = classLoadingService.createThreadContextClassLoader(moduleInfo.getClassLoader());

        ClassLoader origLoader = AccessController.doPrivileged(new GetTCCL());
        ClassLoader newLoader = AccessController.doPrivileged(new SetTCCL(appContextClassLoader));
        try {
            ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().beginContext(cmd);
            ApplicationClientBnd appClientBnd = appClientBnds.get(cmmd);
            CallbackHandler callbackHandler = callbackHandlers.get(cmmd);
            ClientModuleInjection cmi = new ClientModuleInjection(cmmd, appClientBnd, resourceRefConfigFactory, injectionEngine,
                            managedObjectServiceRef, callbackHandler, runningInClient);
            cmi.processReferences();

            activeCMDs.put(cmmd.getJ2EEName(), cmd);
            clientRunner.readyToRun(cmi, args, cmd, newLoader);
        } catch (ClassNotFoundException e) {
            clientRunner.setupFailure();
            throw new StateChangeException(e);
        } catch (InjectionException e) {
            clientRunner.setupFailure();
            throw new StateChangeException(e);
        } finally {
            ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().endContext();
            if (origLoader != newLoader) {
                AccessController.doPrivileged(new SetTCCL(origLoader));
            }
        }
        return futureMonitor.createFutureWithResult(true);
    }

    @Override
    public void stopModule(ExtendedModuleInfo moduleInfo) {
        try {
            activeCMDs.remove(moduleInfo.getMetaData().getJ2EEName());
            serviceReg.unregister();
            int state = bundleContext.getBundle(0).getState();
            if (state == Bundle.STARTING || state == Bundle.ACTIVE) {
                bundleContext.getBundle(0).stop();
            }
            classLoadingService.destroyThreadContextClassLoader(appContextClassLoader);
        } catch (BundleException e) {
            // Ignore.
        }
    }

    public void activate(ComponentContext cc) {
        this.managedObjectServiceRef.activate(cc);
        this.bundleContext = cc.getBundleContext();

        clientRunner = new ClientRunnerImpl();
        Dictionary<String, String> props = new Hashtable<String, String>();
        props.put("service.vendor", "IBM");
        bundleContext.registerService(ClientRunner.class, clientRunner, props);
    }

    public void deactivate(ComponentContext cc) {
        this.managedObjectServiceRef.deactivate(cc);
        this.bundleContext = null;
        this.clientRunner = null;
    }

    /** {@inheritDoc} */
    @Override
    public ComponentMetaData createComponentMetaData(String identifier) {
        String[] parts = identifier.split("#");
        J2EEName j2eeName = j2eeNameFactory.create(parts[1], parts[2], null); // ignore parts[0] which is the prefix: CLIENT
        ComponentMetaData cmd = activeCMDs.get(j2eeName);
        return cmd;
    }

    /** {@inheritDoc} */
    @Override
    public void initialize(ComponentMetaData metadata) throws IllegalStateException {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    @Override
    public String getMetaDataIdentifier(String appName, String moduleName, String componentName) {
        return "CLIENT#" + appName + "#" + moduleName;// + "#" + componentName;
    }

    /** {@inheritDoc} */
    @Override
    public ClassLoader getClassLoader(ComponentMetaData metadata) {
        throw new UnsupportedOperationException();
    }

    static class GetTCCL implements PrivilegedAction<ClassLoader> {

        /** {@inheritDoc} */
        @Override
        public ClassLoader run() {
            return Thread.currentThread().getContextClassLoader();
        }

    }

    static class SetTCCL implements PrivilegedAction<ClassLoader> {
        private final ClassLoader newClassLoader;

        SetTCCL(ClassLoader newClassLoader) {
            this.newClassLoader = newClassLoader;
        }

        /** {@inheritDoc} */
        @Override
        public ClassLoader run() {
            Thread.currentThread().setContextClassLoader(newClassLoader);
            return newClassLoader;
        }

    }
}
