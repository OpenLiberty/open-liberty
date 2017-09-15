/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.remote.client.internal;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import com.ibm.ws.container.service.app.deploy.extended.ExtendedModuleInfo;
import com.ibm.ws.container.service.metadata.MetaDataException;
import com.ibm.ws.container.service.metadata.MetaDataSlotService;
import com.ibm.ws.container.service.metadata.extended.ModuleMetaDataExtender;
import com.ibm.ws.container.service.metadata.extended.NestedModuleMetaDataFactory;
import com.ibm.ws.ejbcontainer.osgi.EJBContainer;
import com.ibm.ws.kernel.LibertyProcess;
import com.ibm.ws.runtime.metadata.MetaDataSlot;
import com.ibm.ws.runtime.metadata.ModuleMetaData;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

@Component(service = { NestedModuleMetaDataFactory.class, ModuleMetaDataExtender.class, EJBClientMetaDataRuntime.class },
           property = { "service.vendor=IBM", "type:String=client" })
public class EJBClientMetaDataRuntime implements NestedModuleMetaDataFactory, ModuleMetaDataExtender {
    private final AtomicServiceReference<MetaDataSlotService> metaDataSlotServiceSR = new AtomicServiceReference<MetaDataSlotService>("metaDataSlotService");
    private final AtomicServiceReference<EJBContainer> ejbContainerSR = new AtomicServiceReference<EJBContainer>("ejbContainer");

    private MetaDataSlot moduleSlot;

    @Reference(service = LibertyProcess.class, target = "(wlp.process.type=client)")
    protected void setLibertyProcess(ServiceReference<LibertyProcess> reference) {}

    protected void unsetLibertyProcess(ServiceReference<LibertyProcess> reference) {}

    @Reference(name = "metaDataSlotService", service = MetaDataSlotService.class)
    protected void setMetaDataSlotService(ServiceReference<MetaDataSlotService> reference) {
        metaDataSlotServiceSR.setReference(reference);
    }

    protected void unsetMetaDataSlotService(ServiceReference<MetaDataSlotService> reference) {
        metaDataSlotServiceSR.unsetReference(reference);
    }

    @Reference(name = "ejbContainer", service = EJBContainer.class)
    protected void setEJBContainer(ServiceReference<EJBContainer> reference) {
        ejbContainerSR.setReference(reference);
    }

    protected void unsetEJBContainer(ServiceReference<EJBContainer> reference) {
        ejbContainerSR.unsetReference(reference);
    }

    @Activate
    protected void activate(ComponentContext cc) {
        metaDataSlotServiceSR.activate(cc);
        ejbContainerSR.activate(cc);

        moduleSlot = metaDataSlotServiceSR.getServiceWithException().reserveMetaDataSlot(ModuleMetaData.class);
    }

    @Deactivate
    protected void deactivate(ComponentContext cc) {
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

    public void setEJBModuleMetaData(ModuleMetaData clientMMD, ModuleMetaData ejbMMD) {
        clientMMD.setMetaData(moduleSlot, ejbMMD);
    }

    public ModuleMetaData getEJBModuleMetaData(ModuleMetaData mmd) {
        return (ModuleMetaData) mmd.getMetaData(moduleSlot);
    }
}
