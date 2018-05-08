/*******************************************************************************
 * Copyright (c) 2014, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.beanvalidation.v11.cdi.internal;

import java.util.Map;
import java.util.WeakHashMap;

import javax.enterprise.inject.spi.BeanManager;
import javax.validation.ConstraintValidatorFactory;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.beanvalidation.service.ValidationReleasable;
import com.ibm.ws.beanvalidation.service.ValidationReleasableFactory;
import com.ibm.ws.cdi.CDIService;
import com.ibm.ws.container.service.metadata.ComponentMetaDataListener;
import com.ibm.ws.container.service.metadata.MetaDataEvent;
import com.ibm.ws.managedobject.ManagedObject;
import com.ibm.ws.managedobject.ManagedObjectException;
import com.ibm.ws.managedobject.ManagedObjectFactory;
import com.ibm.ws.managedobject.ManagedObjectService;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.runtime.metadata.ModuleMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

/**
 * An implementation that is CDI aware.
 */
@Component(service = { ValidationReleasableFactory.class,
                       ComponentMetaDataListener.class })
public class ValidationReleasableFactoryImpl implements ValidationReleasableFactory, ComponentMetaDataListener {

    private static final TraceComponent tc = Tr.register(ValidationReleasableFactoryImpl.class);
    private static final String REFERENCE_CDI_SERVICE = "cdiService";
    private static final String REFERENCE_MANAGED_OBJECT_SERVICE = "managedObjectService";

    private final AtomicServiceReference<CDIService> cdiService = new AtomicServiceReference<CDIService>(REFERENCE_CDI_SERVICE);
    private final AtomicServiceReference<ManagedObjectService> managedObjectServiceRef = new AtomicServiceReference<ManagedObjectService>(REFERENCE_MANAGED_OBJECT_SERVICE);
    private final Map<ComponentMetaData, BeanManager> beanManagers = new WeakHashMap<ComponentMetaData, BeanManager>();

    @Override
    public <T> ManagedObject<T> createValidationReleasable(Class<T> clazz) {
        BeanManager beanManager = getCurrentBeanManager();

        // If the bean manger isn't null, this indicates that the module that is
        // invoking this code path has CDI enabled.
        if (beanManager != null) {

            // The mof handles calling produce, inject, and postConstruct.
            ManagedObjectFactory<T> mof = getManagedBeanManagedObjectFactory(clazz);
            try {
                return mof.createManagedObject();
            } catch (Exception e) {
                // ffdc
            }
        }
        return null;
    }

    @Override
    public ValidationReleasable<ConstraintValidatorFactory> createConstraintValidatorFactory() {
        BeanManager beanManager = getCurrentBeanManager();
        if (beanManager != null) {
            return new ReleasableConstraintValidatorFactory(this);
        }
        return null;
    }

    private BeanManager getCurrentBeanManager() {
        ComponentMetaData cmd = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();
        BeanManager beanMgr = beanManagers.get(cmd);
        if (beanMgr == null) {
            beanMgr = cdiService.getServiceWithException().getCurrentBeanManager();
            beanManagers.put(cmd, beanMgr);
        }
        return beanMgr;
    }

    private <T> ManagedObjectFactory<T> getManagedBeanManagedObjectFactory(Class<T> clazz) {
        ManagedObjectFactory<T> factory = null;
        ModuleMetaData mmd = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData().getModuleMetaData();
        ManagedObjectService managedObjectService = managedObjectServiceRef.getService();
        if (managedObjectService != null) {
            try {
                factory = managedObjectService.createManagedObjectFactory(mmd, clazz, true);
            } catch (ManagedObjectException e) {
                // ffdc
            }
        }
        return factory;
    }

    @Activate
    protected void activate(ComponentContext cc) {
        cdiService.activate(cc);
        managedObjectServiceRef.activate(cc);
    }

    @Deactivate
    protected void deactivate(ComponentContext cc) {
        cdiService.deactivate(cc);
        managedObjectServiceRef.deactivate(cc);
    }

    @Reference(name = REFERENCE_CDI_SERVICE, service = CDIService.class)
    protected void setCdiService(ServiceReference<CDIService> ref) {
        cdiService.setReference(ref);
    }

    protected void unsetCdiService(ServiceReference<CDIService> ref) {
        cdiService.unsetReference(ref);
    }

    @Reference(name = REFERENCE_MANAGED_OBJECT_SERVICE,
               service = ManagedObjectService.class,
               policy = ReferencePolicy.DYNAMIC,
               policyOption = ReferencePolicyOption.GREEDY)
    protected void setManagedObjectService(ServiceReference<ManagedObjectService> ref) {
        managedObjectServiceRef.setReference(ref);
    }

    protected void unsetManagedObjectService(ServiceReference<ManagedObjectService> ref) {
        managedObjectServiceRef.unsetReference(ref);
    }

    @Override
    public void componentMetaDataCreated(MetaDataEvent<ComponentMetaData> event) {
        // no-op

    }

    @Override
    public void componentMetaDataDestroyed(MetaDataEvent<ComponentMetaData> event) {
        BeanManager beanManager = beanManagers.remove(event.getMetaData());
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "Removed bean manager from cache: ", beanManager);
    }
}