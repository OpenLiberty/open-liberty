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
package com.ibm.ws.ejbcontainer.osgi.internal;

import java.lang.reflect.Method;
import java.rmi.RemoteException;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.ejb.CreateException;
import javax.ejb.EJBException;
import javax.ejb.RemoveException;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.ibm.ejs.container.BeanMetaData;
import com.ibm.ejs.container.ContainerException;
import com.ibm.ejs.container.EJBConfigurationException;
import com.ibm.ejs.container.EJBNotFoundException;
import com.ibm.ejs.container.HomeOfHomes;
import com.ibm.ejs.csi.EJBModuleMetaDataImpl;
import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.csi.J2EENameFactory;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.container.service.app.deploy.ModuleInfo;
import com.ibm.ws.container.service.app.deploy.extended.ExtendedModuleInfo;
import com.ibm.ws.container.service.metadata.MetaDataException;
import com.ibm.ws.container.service.state.StateChangeException;
import com.ibm.ws.ejbcontainer.EJBEndpoint;
import com.ibm.ws.ejbcontainer.EJBReferenceFactory;
import com.ibm.ws.ejbcontainer.InternalConstants;
import com.ibm.ws.ejbcontainer.osgi.EJBContainer;
import com.ibm.ws.ejbcontainer.osgi.EJBRemoteRuntime;
import com.ibm.ws.ejbcontainer.osgi.EJBSystemBeanConfig;
import com.ibm.ws.ejbcontainer.osgi.EJBSystemModule;
import com.ibm.ws.ejbcontainer.osgi.SessionBeanRuntime;
import com.ibm.ws.ejbcontainer.osgi.internal.metadata.OSGiEJBModuleMetaDataImpl;
import com.ibm.ws.ejbcontainer.runtime.ComponentNameSpaceConfigurationProviderImpl;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.javaee.dd.ejb.EJBJar;
import com.ibm.ws.managedobject.ManagedObjectContext;
import com.ibm.ws.metadata.ejb.BeanInitData;
import com.ibm.ws.runtime.metadata.ModuleMetaData;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.ejbcontainer.WSEJBEndpointManager;
import com.ibm.wsspi.injectionengine.InjectionEngine;
import com.ibm.wsspi.injectionengine.ReferenceContext;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

@Component(service = EJBContainer.class)
public class EJBContainerImpl implements EJBContainer {
    private static final String REFERENCE_RUNTIME = "runtime";
    private static final String REFERENCE_SESSION_RUNTIME = "sessionRuntime";
    private static final String REFERENCE_J2EE_NAME_FACTORY = "j2eeNameFactory";
    private static final String REFERENCE_INJECTION_ENGINE = "injectionEngine";

    private final AtomicServiceReference<EJBRuntimeImpl> runtimeSR = new AtomicServiceReference<EJBRuntimeImpl>(REFERENCE_RUNTIME);
    private final AtomicServiceReference<SessionBeanRuntime> sessionRuntimeSR = new AtomicServiceReference<SessionBeanRuntime>(REFERENCE_SESSION_RUNTIME);
    private final AtomicServiceReference<J2EENameFactory> j2eeNameFactorySR = new AtomicServiceReference<J2EENameFactory>(REFERENCE_J2EE_NAME_FACTORY);
    private final AtomicServiceReference<InjectionEngine> injectionEngineSR = new AtomicServiceReference<InjectionEngine>(REFERENCE_INJECTION_ENGINE);

    @Reference(name = REFERENCE_RUNTIME, service = EJBRuntimeImpl.class)
    protected void setRuntime(ServiceReference<EJBRuntimeImpl> reference) {
        runtimeSR.setReference(reference);
    }

    protected void unsetRuntime(ServiceReference<EJBRuntimeImpl> reference) {
        runtimeSR.unsetReference(reference);
    }

    @Reference(name = REFERENCE_SESSION_RUNTIME,
               service = SessionBeanRuntime.class,
               cardinality = ReferenceCardinality.OPTIONAL,
               policy = ReferencePolicy.DYNAMIC)
    protected void setSessionRuntime(ServiceReference<SessionBeanRuntime> reference) {
        sessionRuntimeSR.setReference(reference);
    }

    protected void unsetSessionRuntime(ServiceReference<SessionBeanRuntime> reference) {
        sessionRuntimeSR.unsetReference(reference);
    }

    @Reference(name = REFERENCE_J2EE_NAME_FACTORY, service = J2EENameFactory.class)
    protected void setJ2EENameFactory(ServiceReference<J2EENameFactory> reference) {
        j2eeNameFactorySR.setReference(reference);
    }

    protected void unsetJ2EENameFactory(ServiceReference<J2EENameFactory> reference) {
        j2eeNameFactorySR.unsetReference(reference);
    }

    @Reference(name = REFERENCE_INJECTION_ENGINE, service = InjectionEngine.class)
    protected void setInjectionEngine(ServiceReference<InjectionEngine> reference) {
        injectionEngineSR.setReference(reference);
    }

    protected void unsetInjectionEngine(ServiceReference<InjectionEngine> reference) {
        injectionEngineSR.unsetReference(reference);
    }

    @Activate
    protected void activate(ComponentContext cc) {
        runtimeSR.activate(cc);
        sessionRuntimeSR.activate(cc);
        j2eeNameFactorySR.activate(cc);
        injectionEngineSR.activate(cc);
    }

    @Deactivate
    protected void deactivate(ComponentContext cc) {
        runtimeSR.deactivate(cc);
        sessionRuntimeSR.deactivate(cc);
        j2eeNameFactorySR.deactivate(cc);
        injectionEngineSR.deactivate(cc);
    }

    @Override
    public ModuleMetaData createEJBInWARModuleMetaData(ModuleInfo moduleInfo) throws MetaDataException {
        try {
            Container container = moduleInfo.getContainer();
            if (!container.isRoot()) {
                throw new IllegalStateException("WAR module container is not a root");
            }

            // This adapt should always succeed because ModuleInitDataAdapter
            // should always find WebModuleInfo.
            ModuleInitDataImpl mid = container.adapt(ModuleInitDataImpl.class);
            if (null == mid || mid.ivBeans.isEmpty()) {
                // Do nothing if there are no EJBs.
                return null;
            }

            EJBRuntimeImpl runtime = runtimeSR.getServiceWithException();
            EJBModuleMetaDataImpl ejbMMD = runtime.createModuleMetaData(moduleInfo, mid);
            ejbMMD.ivEJBInWAR = true;
            ejbMMD.ivManagedBeansOnly = mid.containsManagedBeansOnly();

            return ejbMMD;
        } catch (UnableToAdaptException e) {
            throw new MetaDataException(e);
        }
    }

    @Override
    public void populateEJBInWARReferenceContext(ModuleInfo moduleInfo, ModuleMetaData mmd) throws MetaDataException {
        EJBModuleMetaDataImpl ejbMMD = (EJBModuleMetaDataImpl) mmd;
        EJBRuntimeImpl runtime = runtimeSR.getServiceWithException();
        InjectionEngine injectionEngine = injectionEngineSR.getServiceWithException();

        // This must be done after moduleMetaDataCreated so that injection
        // engine has a chance to propagate the ReferenceContext that is
        // common to both web and EJB ModuleMetaData.
        ReferenceContext rc = injectionEngine.getCommonReferenceContext(((ExtendedModuleInfo) moduleInfo).getMetaData());

        for (BeanInitData bid : ejbMMD.ivInitData.ivBeans) {
            BeanMetaData bmd;
            try {
                bmd = runtime.createBeanMetaData(bid, ejbMMD);
            } catch (ContainerException e) {
                throw new MetaDataException(e);
            } catch (EJBConfigurationException e) {
                throw new MetaDataException(e);
            }
            BeanInitDataImpl bidImpl = (BeanInitDataImpl) bid;
            bidImpl.beanMetaData = bmd;

            bmd.ivReferenceContext = rc;
            rc.add(new ComponentNameSpaceConfigurationProviderImpl(bmd, runtime));
        }
    }

    @FFDCIgnore(EJBRuntimeException.class)
    @Override
    public void startEJBInWARModule(ModuleMetaData mmd, Container container) throws StateChangeException {
        EJBModuleMetaDataImpl ejbMMD = (EJBModuleMetaDataImpl) mmd;
        EJBRuntimeImpl runtime = runtimeSR.getServiceWithException();
        try {
            runtime.start(ejbMMD);
        } catch (EJBRuntimeException e) {
            throw new StateChangeException(e.getCause());
        }
    }

    @Override
    public void startedEJBInWARModule(ModuleMetaData mmd, Container container) throws StateChangeException {
        try {
            ModuleInitDataAdapter.removeFromCache(container);
        } catch (UnableToAdaptException e) {
            throw new StateChangeException(e);
        }
    }

    @Override
    public void stopEJBInWARModule(ModuleMetaData mmd, Container container) {
        EJBModuleMetaDataImpl ejbMMD = (EJBModuleMetaDataImpl) mmd;
        runtimeSR.getServiceWithException().stop(ejbMMD);
    }

    @Override
    public EJBSystemModule startSystemModule(String moduleName,
                                             ClassLoader classLoader,
                                             EJBSystemBeanConfig[] ejbs,
                                             EJBRemoteRuntime ejbRemoteRuntime) {
        EJBRuntimeImpl runtime = runtimeSR.getServiceWithException();

        // Don't bother starting a system module if the server is shutting down
        if (runtime.isStopping()) {
            return null;
        }

        ModuleInitDataImpl mid = createSystemModuleInitData(moduleName, classLoader, ejbs);
        OSGiEJBModuleMetaDataImpl mmd = (OSGiEJBModuleMetaDataImpl) runtime.createSystemModuleMetaData(mid);
        mmd.systemModuleNameSpaceBinder = new SystemNameSpaceBinderImpl(ejbRemoteRuntime);

        try {
            runtime.startSystemModule(mmd);
        } catch (EJBRuntimeException e) {
            throw new IllegalStateException(e);
        }

        // Clear the name space binder to remove the cached copy of the EJBRemoteRuntime
        mmd.systemModuleNameSpaceBinder = null;

        return new EJBSystemModuleImpl(runtime, mmd, getReferenceFactories(mid));
    }

    private ModuleInitDataImpl createSystemModuleInitData(String moduleName, ClassLoader classLoader, EJBSystemBeanConfig[] ejbs) {
        J2EENameFactory j2eeNameFactory = j2eeNameFactorySR.getServiceWithException();
        SessionBeanRuntime beanRuntime = sessionRuntimeSR.getServiceWithException();

        // Use the internal HomeOfHomes application name.
        ModuleInitDataImpl mid = new ModuleInitDataImpl(moduleName, HomeOfHomes.HOME_OF_HOMES, EJBJar.VERSION_3_0, beanRuntime, null, null);
        mid.systemModule = true;
        mid.ivJ2EEName = j2eeNameFactory.create(mid.ivAppName, mid.ivName, null);
        mid.ivHasTimers = false;
        mid.ivClassLoader = classLoader;

        for (EJBSystemBeanConfig ejb : ejbs) {
            BeanInitDataImpl bid = new BeanInitDataImpl(ejb.getName(), mid);

            bid.ivType = InternalConstants.TYPE_STATELESS_SESSION;
            bid.ivClassName = ejb.getEJBClass().getName();
            bid.ivRemoteHomeInterfaceName = ejb.getRemoteHomeInterface().getName();
            bid.ivRemoteInterfaceName = ejb.getRemoteInterface().getName();
            bid.systemHomeBindingName = ejb.getRemoteHomeBindingName();

            bid.beanRuntime = beanRuntime;
            bid.ivJ2EEName = j2eeNameFactory.create(mid.ivAppName, mid.ivName, bid.ivName);
            bid.ivHasScheduleTimers = false;

            mid.ivBeans.add(bid);
        }

        return mid;
    }

    private Map<String, EJBReferenceFactory> getReferenceFactories(ModuleInitDataImpl mid) {
        Map<String, EJBReferenceFactory> referenceFactories = new LinkedHashMap<String, EJBReferenceFactory>();
        for (EJBEndpoint ep : mid.getEJBEndpoints()) {
            referenceFactories.put(ep.getName(), ep.getReferenceFactory());
        }
        return referenceFactories;
    }

    @Override
    public Object getExPcBindingContext() {
        EJBRuntimeImpl runtime = runtimeSR.getServiceWithException();
        return runtime.getExPcBindingContext();
    }

    @Override
    public WSEJBEndpointManager createWebServiceEndpointManager(J2EEName name, Method[] methods) throws EJBException, EJBConfigurationException {
        EJBRuntimeImpl runtime = runtimeSR.getServiceWithException();
        return runtime.createWebServiceEndpointManager(name, null, methods);
    }

    @Override
    public WSEJBEndpointManager createWebServiceEndpointManager(J2EEName name, Class<?> provider) throws EJBException, EJBConfigurationException {
        EJBRuntimeImpl runtime = runtimeSR.getServiceWithException();
        return runtime.createWebServiceEndpointManager(name, provider, null);
    }

    @Override
    @Sensitive
    public Object createAggregateLocalReference(J2EEName beanName, ManagedObjectContext context) throws CreateException, EJBNotFoundException {
        EJBRuntimeImpl runtime = runtimeSR.getServiceWithException();
        return runtime.createAggregateLocalReference(beanName, context);
    }

    @Override
    public boolean removeStatefulBean(@Sensitive Object bean) throws RemoteException, RemoveException {
        EJBRuntimeImpl runtime = runtimeSR.getServiceWithException();
        return runtime.removeStatefulBean(bean);
    }
}
