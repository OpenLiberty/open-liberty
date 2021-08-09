/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxws.cdi.component;

import java.util.Map;

import javax.enterprise.inject.spi.BeanManager;
import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;

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
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.jaxrws.cdi.JAXWSCDIConstants;
import com.ibm.ws.jaxws.ImplBeanCustomizer;
import com.ibm.ws.jaxws.support.JaxWsMetaDataManager;
import com.ibm.ws.managedobject.ManagedObject;
import com.ibm.ws.managedobject.ManagedObjectException;
import com.ibm.ws.managedobject.ManagedObjectFactory;
import com.ibm.ws.managedobject.ManagedObjectService;
import com.ibm.ws.runtime.metadata.ModuleMetaData;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

/**
 * CDI customizer : responsible for CDI life cycle management if the Restful Application/Resource/Provider is a CDI managed bean
 * Priority is higher than EJB by default
 */
@Component(name = "ImplBeanCustomizer", immediate = true, property = { "service.vendor=IBM" })
public class JaxWsImplBeanCDICustomizer implements ImplBeanCustomizer {

    private static final TraceComponent tc = Tr.register(JaxWsImplBeanCDICustomizer.class);
//    private static List<String> validRequestScopeList = new ArrayList<String>();
//    private static List<String> validSingletonScopeList = new ArrayList<String>();
//    static {
//        validRequestScopeList.add(JAXWSCDIConstants.REQUEST_SCOPE);
//        validRequestScopeList.add(JAXWSCDIConstants.DEPENDENT_SCOPE);
//        validRequestScopeList.add(JAXWSCDIConstants.SESSION_SCOPE);
//        validSingletonScopeList.add(JAXWSCDIConstants.DEPENDENT_SCOPE);
//        validSingletonScopeList.add(JAXWSCDIConstants.APPLICATION_SCOPE);
//    }

    private final AtomicServiceReference<ManagedObjectService> managedObjectServiceRef = new AtomicServiceReference<ManagedObjectService>("managedObjectService");

    private <T> T getBeanFromCDI(Class<T> clazz, Container container) {
        if (!isCDIEnabled())
        {
            return null;
        }

        ManagedObject<?> newServiceObject = null;

        newServiceObject = getClassFromManagedObject(clazz, container);
        if (newServiceObject != null) {

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Get instance from CDI " + clazz.getName());
            }

            return (T) newServiceObject.getObject();
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Get instance from CDI is null , use from ws for " + clazz.getName());
        }
        return null;
    }

    /**
     * @param clazz
     * @return
     */
    @FFDCIgnore(value = { Exception.class })
    private ManagedObject<?> getClassFromManagedObject(Class<?> clazz, Container container) {

        ManagedObjectFactory<?> managedObjectFactory = getManagedObjectFactory(clazz, container);

        ManagedObject<?> bean = null;
        try {
            bean = managedObjectFactory.createManagedObject();
        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Couldn't create object instance from ManagedObjectFactory for : " + clazz.getName() + ", but ignore the FFDC: " + e.toString());
            }
        }

        if (bean == null) {
            return null;
        }

        return bean;
    }

//    private Object getClassFromCDI(Class<?> clazz) {
//        BeanManager manager = getBeanManager();
//        Bean<?> bean = getBeanFromCDI(clazz);
//        Object obj = null;
//        if (bean != null) {
//            obj = manager.getReference(bean, clazz,
//                                       manager.createCreationalContext(bean));
//        }
//        return obj;
//    }

    @FFDCIgnore(NameNotFoundException.class)
    private BeanManager getBeanManager() {

        BeanManager manager = null;

//        if (manager != null)
//        {
//            return manager;
//        }
//        else {
        try {
            InitialContext initialContext = new InitialContext();
            manager = (BeanManager) initialContext.lookup("java:comp/BeanManager");
//            manager = CDI.current().getBeanManager();
            JAXWSCDIServiceImplByJndi.setBeanManager(manager);
        } catch (NameNotFoundException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Couldn't get BeanManager through JNDI: " + JAXWSCDIConstants.JDNI_STRING + ", but ignore the FFDC: " + e.toString());
            }
        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Couldn't get BeanManager through JNDI: " + JAXWSCDIConstants.JDNI_STRING + ". " + e.toString());
            }
        }
        return manager;
//        }
    }

    private boolean isCDIEnabled() {

        BeanManager beanManager = getBeanManager();
        return beanManager == null ? false : true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.jaxws.ImplBeanCustomizer#onPrepareImplBean(java.lang.Object)
     */
    @Override
    public <T> T onPrepareImplBean(Class<T> cls, Container container) {
        T instance = null;

        instance = getBeanFromCDI(cls, container);
//        if (instance == null) {
//            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
//                Tr.debug(tc, "t is null from CDI , get app from rs for " + cls.getName());
//            }
//
//        }
        return instance;

    }

    @Activate
    protected void activate(ComponentContext cContext, Map<String, Object> properties) throws Exception {
        this.managedObjectServiceRef.activate(cContext);
    }

    /**
     * DS-driven de-activation
     */
    @Deactivate
    protected void deactivate(ComponentContext cc) {
        this.managedObjectServiceRef.deactivate(cc);

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

    private ManagedObjectFactory<?> getManagedObjectFactory(Class<?> clazz, Container container) {

        if (container == null) {
            return null;
        }

//        if (managedObjectFactoryCache.containsKey(clazz)) {
//            return managedObjectFactoryCache.get(clazz);
//        };

        ManagedObjectFactory<?> mof = null;
        try {
            ManagedObjectService mos = managedObjectServiceRef.getServiceWithException();
            if (mos == null) {
                return null;
            }

            ModuleMetaData mmd = JaxWsMetaDataManager.getModuleMetaData();
            mof = mos.createManagedObjectFactory(mmd, clazz, true);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Successfully to create ManagedObjectFactory for class: " + clazz.getName());
            }
        } catch (ManagedObjectException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Fail to create ManagedObjectFactory for class: " + clazz.getName() + " Exception is: " + e.toString());
            }
        }

        return mof;
    }
}
