/*******************************************************************************
 * Copyright (c) 2015, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.impl.managedobject;

import java.util.Iterator;
import java.util.Set;

import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionTarget;

import org.jboss.weld.construction.api.AroundConstructCallback;
import org.jboss.weld.construction.api.WeldCreationalContext;
import org.jboss.weld.ejb.spi.EjbDescriptor;
import org.jboss.weld.exceptions.UnsupportedOperationException;
import org.jboss.weld.manager.api.WeldManager;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.cdi.internal.interfaces.CDIRuntime;
import com.ibm.ws.cdi.internal.interfaces.WebSphereBeanDeploymentArchive;
import com.ibm.ws.managedobject.ManagedObject;
import com.ibm.ws.managedobject.ManagedObjectContext;
import com.ibm.ws.managedobject.ManagedObjectException;
import com.ibm.ws.managedobject.ManagedObjectFactory;
import com.ibm.ws.managedobject.ManagedObjectInvocationContext;

public class CDIEJBManagedObjectFactoryImpl<T> extends AbstractManagedObjectFactory<T> implements ManagedObjectFactory<T> {

    private static final TraceComponent tc = Tr.register(CDIEJBManagedObjectFactoryImpl.class);

    private String ejbName = null;
    private ManagedObjectFactory<T> defaultEJBManagedObjectFactory = null;
    private EjbDescriptor<T> ejbDescriptor;
    private Bean<T> ejbBean;
    private WebSphereBeanDeploymentArchive ejbBDA;
    private WeldManager ejbBeanManager;
    private boolean ejbBeanLookupComplete;

    public CDIEJBManagedObjectFactoryImpl(Class<T> classToManage, String ejbName, CDIRuntime cdiRuntime, ManagedObjectFactory<T> defaultEJBManagedObjectFactory) {
        super(classToManage, cdiRuntime, false);
        this.ejbName = ejbName;
        this.defaultEJBManagedObjectFactory = defaultEJBManagedObjectFactory;
    }

    @Override
    protected synchronized WeldManager getBeanManager() {
        if (this.ejbBeanManager == null) {
            //getEjbDescriptor will initialize the bean manager with the one which really contains the ejbDescriptor
            getEjbDescriptor();
        }
        return this.ejbBeanManager;
    }

    @Override
    protected synchronized WebSphereBeanDeploymentArchive getCurrentBeanDeploymentArchive() {
        if (this.ejbBDA == null) {
            //getEjbDescriptor will initialize the bda with the one which really contains the ejbDescriptor
            getEjbDescriptor();
        }
        return this.ejbBDA;
    }

    /**
     * This version creates a ManagedObjectContext which contains a CreationalContext for an EJB.
     */
    @Override
    public ManagedObjectContext createContext() {
        Bean<T> bean = null;

        EjbDescriptor<T> ejbDescriptor = getEjbDescriptor();
        //in the case of an MDB, the bean should be null, in which case the creational context will be non-contextual
        if (!ejbDescriptor.isMessageDriven()) {
            bean = getBean();
        }

        WeldManager beanManager = getBeanManager();
        WeldCreationalContext<T> creationalContext = beanManager.createCreationalContext(bean);

        ManagedObjectContext managedObjectContext = new CDIManagedObjectState(creationalContext);

        return managedObjectContext;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected synchronized Bean<T> getBean() {
        if (!this.ejbBeanLookupComplete) {

            WeldManager beanManager = getBeanManager();

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Looking for EJB Bean: " + this.ejbName);
            }

            EjbDescriptor<?> ejbDescriptor = getEjbDescriptor();
            if (ejbDescriptor != null) {
                this.ejbBean = (Bean<T>) beanManager.getBean(ejbDescriptor);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    if (this.ejbBean != null) {
                        Tr.debug(tc, "Found EJB Bean: " + this.ejbBean);
                    }
                }
            }

            this.ejbBeanLookupComplete = true;

            if (this.ejbBean != null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Found a bean of class : " + this.ejbBean.getBeanClass());
                }
                if (!this.ejbBean.getBeanClass().equals(getManagedObjectClass())) {
                    //this exception should never happen
                    throw new IllegalStateException("Managed Class {" + getManagedObjectClass().getName() + "} does not match Bean Class {"
                                                    + this.ejbBean.getBeanClass().getName() + "}");
                }
            }

        }
        return this.ejbBean;
    }

    private synchronized EjbDescriptor<T> getEjbDescriptor() {
        if (this.ejbDescriptor == null) {

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Looking for EJB Bean: " + this.ejbName);
            }
            WebSphereBeanDeploymentArchive bda = super.getCurrentBeanDeploymentArchive();
            WeldManager beanManager = null;
            if (bda != null) {
                beanManager = (WeldManager) bda.getBeanManager();
            } else {
                beanManager = (WeldManager) getCDIRuntime().getCurrentBeanManager();
            }

            EjbDescriptor<T> ejbDescriptor = beanManager.getEjbDescriptor(this.ejbName);

            if (ejbDescriptor == null) {
                Set<WebSphereBeanDeploymentArchive> children = bda.getDescendantBdas();
                Iterator<WebSphereBeanDeploymentArchive> itr = children.iterator();
                while (ejbDescriptor == null && itr.hasNext()) {
                    bda = itr.next();
                    beanManager = (WeldManager) bda.getBeanManager();
                    ejbDescriptor = beanManager.getEjbDescriptor(this.ejbName);
                }
                // check all accessible BDAs if we haven't found the ejbDescriptor in descendant BDAs
                if (ejbDescriptor == null) {
                    children = bda.getWebSphereBeanDeploymentArchives();
                    itr = children.iterator();
                    while (ejbDescriptor == null && itr.hasNext()) {
                        bda = itr.next();
                        beanManager = (WeldManager) bda.getBeanManager();
                        ejbDescriptor = beanManager.getEjbDescriptor(this.ejbName);
                    }
                }
            }

            if (ejbDescriptor != null) {
                this.ejbDescriptor = ejbDescriptor;
                this.ejbBeanManager = beanManager;
                this.ejbBDA = bda;
            }

        }

        if (this.ejbDescriptor == null) {
            Tr.error(tc, "Could not find an EjbDescriptor for : " + this.ejbName);
        }

        return this.ejbDescriptor;
    }

    @Override
    public ManagedObject<T> createManagedObject() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ManagedObject<T> createManagedObject(ManagedObjectInvocationContext<T> invocationContext) throws ManagedObjectException {
        if (getBean() == null) {
            return defaultEJBManagedObjectFactory.createManagedObject(invocationContext);
        } else {
            return super.createManagedObject(invocationContext);
        }
    }

    /**
     * Get an EJB InjectionTarget using the EJBDescriptor
     */
    @Override
    protected InjectionTarget<T> getInjectionTarget(boolean nonContextual) {
        EjbDescriptor<T> ejbDescriptor = getEjbDescriptor();
        InjectionTarget<T> injectionTarget = getBeanManager().createInjectionTarget(ejbDescriptor);
        return injectionTarget;
    }

    /**
     * Get the CreationalContext from the ManagedObjectInvocationContext
     * Register our AroundConstruct callback and stop Weld from invoking any
     */
    @Override
    protected WeldCreationalContext<T> getCreationalContext(ManagedObjectInvocationContext<T> invocationContext) {
        ManagedObjectContext managedObjectContext = invocationContext.getManagedObjectContext();

        @SuppressWarnings("unchecked")
        WeldCreationalContext<T> creationalContext = managedObjectContext.getContextData(WeldCreationalContext.class);

        AroundConstructCallback<T> callback = new EJBInterceptorAroundConstructCallback<T>(invocationContext);

        //we don't want weld to invoke any @AroundConstruct interceptors ... we will drive them all via a callback
        //so register our callback
        creationalContext.registerAroundConstructCallback(callback);
        //and stop weld from invoking them
        creationalContext.setConstructorInterceptionSuppressed(true);

        return creationalContext;
    }

    @Override
    public String toString() {
        return "CDI EJB Managed Object Factory for class: " + getManagedObjectClass().getName();
    }
}
