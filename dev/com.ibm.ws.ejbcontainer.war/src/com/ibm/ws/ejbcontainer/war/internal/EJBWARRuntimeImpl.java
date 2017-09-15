/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.war.internal;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.ws.container.service.app.deploy.ModuleInfo;
import com.ibm.ws.container.service.app.deploy.extended.ExtendedModuleInfo;
import com.ibm.ws.container.service.state.ModuleStateListener;
import com.ibm.ws.container.service.state.StateChangeException;
import com.ibm.ws.ejbcontainer.osgi.EJBContainer;
import com.ibm.ws.runtime.metadata.ModuleMetaData;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

public class EJBWARRuntimeImpl implements ModuleStateListener {
    private final AtomicServiceReference<EJBContainer> ejbContainerSR = new AtomicServiceReference<EJBContainer>("ejbContainer");
    private EJBWARMetaDataRuntime ejbWARMetaDataRuntime;

    public void setEJBContainer(ServiceReference<EJBContainer> reference) {
        ejbContainerSR.setReference(reference);
    }

    public void unsetEJBContainer(ServiceReference<EJBContainer> reference) {
        ejbContainerSR.unsetReference(reference);
    }

    public void setEJBWARMetaDataRuntime(EJBWARMetaDataRuntime runtime) {
        this.ejbWARMetaDataRuntime = runtime;
    }

    public void unsetEJBWARMetaDataRuntime(EJBWARMetaDataRuntime runtime) {
        this.ejbWARMetaDataRuntime = null;
    }

    public void activate(ComponentContext cc) {
        ejbContainerSR.activate(cc);
    }

    public void deactivate(ComponentContext cc) {
        ejbContainerSR.deactivate(cc);
    }

    private ModuleMetaData getEJBModuleMetaData(ModuleInfo moduleInfo) {
        return ejbWARMetaDataRuntime.getEJBModuleMetaData(((ExtendedModuleInfo) moduleInfo).getMetaData());
    }

    @Override
    public void moduleStarting(ModuleInfo moduleInfo) throws StateChangeException {
        ModuleMetaData ejbMMD = getEJBModuleMetaData(moduleInfo);
        if (ejbMMD != null) {
            ejbContainerSR.getServiceWithException().startEJBInWARModule(ejbMMD, moduleInfo.getContainer());
        }
    }

    @Override
    public void moduleStarted(ModuleInfo moduleInfo) throws StateChangeException {
        ModuleMetaData ejbMMD = getEJBModuleMetaData(moduleInfo);
        if (ejbMMD != null) {
            ejbContainerSR.getServiceWithException().startedEJBInWARModule(ejbMMD, moduleInfo.getContainer());
        }
    }

    @Override
    public void moduleStopping(ModuleInfo moduleInfo) {}

    @Override
    public void moduleStopped(ModuleInfo moduleInfo) {
        ModuleMetaData ejbMMD = getEJBModuleMetaData(moduleInfo);
        if (ejbMMD != null) {
            ejbContainerSR.getServiceWithException().stopEJBInWARModule(ejbMMD, moduleInfo.getContainer());
        }
    }
}
