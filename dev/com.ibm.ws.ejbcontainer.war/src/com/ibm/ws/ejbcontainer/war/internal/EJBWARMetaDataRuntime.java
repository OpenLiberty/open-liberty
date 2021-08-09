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
package com.ibm.ws.ejbcontainer.war.internal;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.ws.container.service.app.deploy.extended.ExtendedModuleInfo;
import com.ibm.ws.container.service.metadata.MetaDataException;
import com.ibm.ws.container.service.metadata.MetaDataSlotService;
import com.ibm.ws.container.service.metadata.extended.ModuleMetaDataExtender;
import com.ibm.ws.container.service.metadata.extended.NestedModuleMetaDataFactory;
import com.ibm.ws.ejbcontainer.osgi.EJBContainer;
import com.ibm.ws.runtime.metadata.MetaDataSlot;
import com.ibm.ws.runtime.metadata.ModuleMetaData;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

public class EJBWARMetaDataRuntime implements NestedModuleMetaDataFactory, ModuleMetaDataExtender {
    private final AtomicServiceReference<MetaDataSlotService> metaDataSlotServiceSR = new AtomicServiceReference<MetaDataSlotService>("metaDataSlotService");
    private final AtomicServiceReference<EJBContainer> ejbContainerSR = new AtomicServiceReference<EJBContainer>("ejbContainer");

    private MetaDataSlot moduleSlot;

    public void setMetaDataSlotService(ServiceReference<MetaDataSlotService> reference) {
        metaDataSlotServiceSR.setReference(reference);
    }

    public void unsetMetaDataSlotService(ServiceReference<MetaDataSlotService> reference) {
        metaDataSlotServiceSR.unsetReference(reference);
    }

    public void setEJBContainer(ServiceReference<EJBContainer> reference) {
        ejbContainerSR.setReference(reference);
    }

    public void unsetEJBContainer(ServiceReference<EJBContainer> reference) {
        ejbContainerSR.unsetReference(reference);
    }

    public void activate(ComponentContext cc) {
        metaDataSlotServiceSR.activate(cc);
        ejbContainerSR.activate(cc);

        moduleSlot = metaDataSlotServiceSR.getServiceWithException().reserveMetaDataSlot(ModuleMetaData.class);
    }

    public void deactivate(ComponentContext cc) {
        metaDataSlotServiceSR.deactivate(cc);
        ejbContainerSR.deactivate(cc);
    }

    @Override
    public void createdNestedModuleMetaData(ExtendedModuleInfo moduleInfo) throws MetaDataException {
        EJBContainer runtime = ejbContainerSR.getServiceWithException();
        ModuleMetaData ejbMMD = runtime.createEJBInWARModuleMetaData(moduleInfo);

        if (ejbMMD != null) {
            moduleInfo.putNestedMetaData("ejb", ejbMMD);
        }
    }

    @Override
    public ExtendedModuleInfo extendModuleMetaData(ExtendedModuleInfo moduleInfo) throws MetaDataException {
        ModuleMetaData ejbMMD = moduleInfo.getNestedMetaData("ejb");
        if (ejbMMD != null) {
            moduleInfo.getMetaData().setMetaData(moduleSlot, ejbMMD);
            EJBContainer runtime = ejbContainerSR.getServiceWithException();
            runtime.populateEJBInWARReferenceContext(moduleInfo, ejbMMD);
        }
        return null;
    }

    public void setEJBModuleMetaData(ModuleMetaData webMMD, ModuleMetaData ejbMMD) {
        webMMD.setMetaData(moduleSlot, ejbMMD);
    }

    public ModuleMetaData getEJBModuleMetaData(ModuleMetaData mmd) {
        return (ModuleMetaData) mmd.getMetaData(moduleSlot);
    }
}
