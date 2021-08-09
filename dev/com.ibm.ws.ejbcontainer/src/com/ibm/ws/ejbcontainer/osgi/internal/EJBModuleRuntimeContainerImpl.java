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

import java.util.concurrent.Future;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.ibm.ejs.container.BeanMetaData;
import com.ibm.ejs.container.ContainerException;
import com.ibm.ejs.container.EJBConfigurationException;
import com.ibm.ejs.csi.EJBModuleMetaDataImpl;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.container.service.app.deploy.extended.ExtendedModuleInfo;
import com.ibm.ws.container.service.app.deploy.extended.ModuleRuntimeContainer;
import com.ibm.ws.container.service.metadata.MetaDataException;
import com.ibm.ws.container.service.state.StateChangeException;
import com.ibm.ws.ejbcontainer.runtime.ComponentNameSpaceConfigurationProviderImpl;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.metadata.ejb.BeanInitData;
import com.ibm.ws.metadata.ejb.ModuleInitData;
import com.ibm.ws.runtime.metadata.ModuleMetaData;
import com.ibm.ws.threading.FutureMonitor;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.injectionengine.InjectionEngine;

@Component(service = ModuleRuntimeContainer.class,
           property = "type=ejb")
public class EJBModuleRuntimeContainerImpl implements ModuleRuntimeContainer {

    private static final TraceComponent tc = Tr.register(EJBModuleRuntimeContainerImpl.class);

    private EJBRuntimeImpl runtime;
    private InjectionEngine injectionEngine;
    private FutureMonitor futureMonitor;

    @Reference
    protected void setRuntime(EJBRuntimeImpl runtime) {
        this.runtime = runtime;
    }

    protected void unsetRuntime(EJBRuntimeImpl runtime) {
        this.runtime = null;
    }

    @Reference
    protected void setInjectionEngine(InjectionEngine injectionEngine) {
        this.injectionEngine = injectionEngine;
    }

    protected void unsetInjectionEngine(InjectionEngine injectionEngine) {
        this.injectionEngine = null;
    }

    @Reference
    protected void setFutureMonitor(FutureMonitor futureMonitor) {
        this.futureMonitor = futureMonitor;
    }

    protected void unsetFutureMonitor(FutureMonitor futureMonitor) {
        this.futureMonitor = null;
    }

    @Override
    public ModuleMetaData createModuleMetaData(ExtendedModuleInfo moduleInfo) throws MetaDataException {

        if (runtime == null || injectionEngine == null) {
            // Configuration update of transaction might be unsetting us
            // while another startup processes thread is using us
            // making it possible our resources are null while calling this
            // method - 186703

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "EJBRuntime null: " + (runtime == null) + " InjectionEngine null: " + (injectionEngine == null));
            }

            return null;
        }

        // If the server is stopping; don't bother starting any EJB modules
        if (runtime.isStopping()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Server is stopping; EJB module will not be started : " + moduleInfo.getName());
            }
            return null;
        }

        Container container = moduleInfo.getContainer();

        ModuleInitData mid;
        try {
            mid = container.adapt(ModuleInitDataImpl.class);
        } catch (UnableToAdaptException e) {
            throw new MetaDataException(e);
        }

        EJBModuleMetaDataImpl ejbMMD = runtime.createModuleMetaData(moduleInfo, mid);

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

            // Create a reference context bound to MMD prior to returning from
            // createModuleMetaData so that it is properly registered for
            // deferred non-java:comp processing.
            bmd.ivReferenceContext = injectionEngine.createReferenceContext(ejbMMD);
            bmd.ivReferenceContext.add(new ComponentNameSpaceConfigurationProviderImpl(bmd, runtime));
        }

        return ejbMMD;
    }

    @FFDCIgnore(EJBRuntimeException.class)
    @Override
    public Future<Boolean> startModule(ExtendedModuleInfo moduleInfo) throws StateChangeException {

        if (runtime == null || futureMonitor == null) {
            // Configuration update of transaction might be unsetting us
            // while another startup processes thread is using us
            // making it possible our resources are null while calling this
            // method - 186703
            throw new StateChangeException("EJBRuntime available: " + (runtime != null) + " FutureMonitor available: " + (futureMonitor != null));
        }

        // Pre-create result in case server begins shutting down and futureMonitor is unset while starting module
        Future<Boolean> result = futureMonitor.createFutureWithResult(true);

        // If the server is stopping; don't bother starting any EJB modules
        if (runtime.isStopping()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Server is stopping; EJB module will not be started : " + moduleInfo.getName());
            }
            return result;
        }

        try {
            ModuleInitDataAdapter.removeFromCache(moduleInfo.getContainer());
        } catch (UnableToAdaptException e) {
            throw new StateChangeException(e);
        }

        EJBModuleMetaDataImpl mmd = (EJBModuleMetaDataImpl) moduleInfo.getMetaData();
        try {
            runtime.start(mmd);
        } catch (EJBRuntimeException e) {
            throw new StateChangeException(e.getCause());
        }

        return result;
    }

    @Override
    public void stopModule(ExtendedModuleInfo moduleInfo) {
        EJBModuleMetaDataImpl mmd = (EJBModuleMetaDataImpl) moduleInfo.getMetaData();
        if (runtime != null) {
            runtime.stop(mmd);
        }
    }
}
