/*******************************************************************************
 * Copyright (c) 2015, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.impl.managedobject;

import java.lang.reflect.Constructor;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Set;

import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionTarget;
import javax.enterprise.inject.spi.InjectionTargetFactory;

import org.jboss.weld.construction.api.WeldCreationalContext;
import org.jboss.weld.exceptions.UnsupportedOperationException;
import org.jboss.weld.manager.api.WeldManager;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.cdi.internal.interfaces.CDIRuntime;
import com.ibm.ws.cdi.internal.interfaces.CDIUtils;
import com.ibm.ws.cdi.internal.interfaces.WebSphereBeanDeploymentArchive;
import com.ibm.ws.cdi.internal.interfaces.WebSphereCDIDeployment;
import com.ibm.ws.cdi.internal.interfaces.WebSphereInjectionServices;
import com.ibm.ws.managedobject.ManagedObject;
import com.ibm.ws.managedobject.ManagedObjectContext;
import com.ibm.ws.managedobject.ManagedObjectException;
import com.ibm.ws.managedobject.ManagedObjectFactory;
import com.ibm.ws.managedobject.ManagedObjectInvocationContext;
import com.ibm.wsspi.injectionengine.ReferenceContext;

public abstract class AbstractManagedObjectFactory<T> implements ManagedObjectFactory<T> {
    private static final TraceComponent tc = Tr.register(AbstractManagedObjectFactory.class);

    private final CDIRuntime cdiRuntime;
    private final Class<T> managedClass;

    private Bean<T> bean;
    private boolean beanLookupComplete = false;
    private WeldManager beanManager;
    private WebSphereBeanDeploymentArchive bda;
    private final boolean requestManagingInjectionAndInterceptors;

    private ReferenceContext referenceContext = null;

    private WebSphereInjectionServices injectionServices;

    public AbstractManagedObjectFactory(Class<T> classToManage, CDIRuntime cdiRuntime, boolean requestManagingInjectionAndInterceptors) {
        this.managedClass = classToManage;
        this.cdiRuntime = cdiRuntime;
        this.requestManagingInjectionAndInterceptors = requestManagingInjectionAndInterceptors;
    }

    public AbstractManagedObjectFactory(Class<T> classToManage, CDIRuntime cdiRuntime, boolean requestManagingInjectionAndInterceptors, ReferenceContext referenceContext) {
        this.managedClass = classToManage;
        this.cdiRuntime = cdiRuntime;
        this.requestManagingInjectionAndInterceptors = requestManagingInjectionAndInterceptors;
        this.referenceContext = referenceContext;
    }

    @Override
    public boolean managesInjectionAndInterceptors() {
        return this.requestManagingInjectionAndInterceptors;
    }

    @Override
    public Class<T> getManagedObjectClass() {
        return this.managedClass;
    }

    @Override
    public boolean isManaged() {
        return true;
    }

    protected synchronized WeldManager getBeanManager() {
        if (this.beanManager == null) {
            this.beanManager = (WeldManager) cdiRuntime.getClassBeanManager(getManagedObjectClass());

            if (this.beanManager == null) {
                this.beanManager = (WeldManager) cdiRuntime.getCurrentModuleBeanManager();
            }
        }

        return this.beanManager;
    }

    protected synchronized WebSphereBeanDeploymentArchive getCurrentBeanDeploymentArchive() {
        if (this.bda == null) {
            this.bda = cdiRuntime.getClassBeanDeploymentArchive(getManagedObjectClass());
        }
        return this.bda;
    }

    protected synchronized WebSphereInjectionServices getWebSphereInjectionServices() {
        if (this.injectionServices == null) {
            this.injectionServices = cdiRuntime.getCurrentDeployment().getInjectionServices();
        }
        return this.injectionServices;
    }

    @SuppressWarnings("unchecked")
    protected synchronized Bean<T> getBean() throws ManagedObjectException {
        if (!this.beanLookupComplete) {

            WeldManager beanManager = getBeanManager();

            Set<Bean<?>> beans = beanManager.getBeans(getManagedObjectClass());
            if (beans.size() == 0) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "No Beans found for managed class: " + getManagedObjectClass());
                }
            } else {
                this.bean = (Bean<T>) beanManager.resolve(beans);
            }

            this.beanLookupComplete = true;

            if (this.bean != null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Found a bean of class : " + this.bean.getBeanClass());
                }
                if (!this.bean.getBeanClass().equals(getManagedObjectClass())) {
                    //this exception should never happen
                    //TODO NLS?
                    throw new ManagedObjectException("Managed Class {" + getManagedObjectClass().getName() + "} does not match Bean Class {"
                                                     + this.bean.getBeanClass().getName() + "}");
                }
            }

        }
        return this.bean;
    }

    public ManagedObject<T> existingInstance(T instance) throws ManagedObjectException {
        throw new UnsupportedOperationException();
    }

    @Override
    public ManagedObjectContext createContext() throws ManagedObjectException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Constructor<T> getConstructor() {
        throw new UnsupportedOperationException();
    }

    public CDIRuntime getCDIRuntime() {
        return this.cdiRuntime;
    }

    @Override
    public ManagedObject<T> createManagedObject() throws ManagedObjectException {
        throw new UnsupportedOperationException();
    }

    protected InjectionTarget<T> getInjectionTarget(boolean nonContextual) throws ManagedObjectException {
        InjectionTarget<T> injectionTarget = null;

        Class<T> clazz = getManagedObjectClass();

        //if this is a J2EE Component Class then we should have already created and validated the injection target
        WebSphereBeanDeploymentArchive bda = getCurrentBeanDeploymentArchive();
        if (bda != null) {
            injectionTarget = bda.getJEEComponentInjectionTarget(clazz);
        }

        //if it isn't (or we missed it :-o ) then create a new one
        if (injectionTarget == null) {
            AnnotatedType<T> annotatedType = getAnnotatedType(clazz, nonContextual);

            WeldManager beanManager = getBeanManager();

            InjectionTargetFactory<T> itFactory = beanManager.getInjectionTargetFactory(annotatedType);

            Bean<T> bean = nonContextual ? null : getBean();
            injectionTarget = itFactory.createInjectionTarget(bean);

            //if the bda is null then the class is not known in the CDI deployment, so we won't fire CDI events
            if (bda != null) {
                //if this class is a bean then Weld should already have processed it and fired the events
                //if not, then we need to fire the event now
                if (bean == null) {
                    injectionTarget = beanManager.fireProcessInjectionTarget(annotatedType, injectionTarget);
                }
                bda.addJEEComponentInjectionTarget(clazz, injectionTarget);
            }
        }
        return injectionTarget;
    }

    protected AnnotatedType<T> getAnnotatedType(Class<T> clazz, boolean nonContextual) {

        WeldManager beanManager = getBeanManager();
        AnnotatedType<T> annotatedType = null;
        if (nonContextual) {
            annotatedType = beanManager.createAnnotatedType(clazz, clazz.getName() + ": " + clazz.hashCode());
        } else {
            annotatedType = beanManager.createAnnotatedType(clazz);
        }

        return annotatedType;
    }

    protected abstract WeldCreationalContext<T> getCreationalContext(ManagedObjectInvocationContext<T> invocationContext) throws ManagedObjectException;

    protected WeldCreationalContext<T> getCreationalContext(ManagedObjectInvocationContext<T> invocationContext, boolean nonContextual) throws ManagedObjectException {
        //The nonContextual flag is only relevant in CDIManagedObjectFactoryImpl.
        return getCreationalContext(invocationContext);
    }

    @Override
    public final ManagedObject<T> createManagedObject(T instance, ManagedObjectInvocationContext<T> invocationContext) throws ManagedObjectException {

        //In order to avoid creating a second proxy object with a broken internal state we simply wrap any weld proxy we are given in a DummyManagedObject.
        if (CDIUtils.isWeldProxy(getManagedObjectClass())) {
            if(instance != null) {
                return new DummyManagedObject<T>(instance);
            }
            else {
                throw new IllegalArgumentException("when calling createManagedObject on a Managed Object Factory for a weld subclass; please provide an instance of the class");
            }
        }

        // 1.Obtain a BeanManager instance.
        WeldManager beanManager = getBeanManager();
        if (beanManager == null) {
            throw new IllegalStateException("Unable to obtain BeanManager");
        } else {

            // Get an InjectionTarget instance
            boolean nonContextual = invocationContext == null;
            InjectionTarget<T> injectionTarget = getInjectionTarget(nonContextual);

            // Get the CreationalContext
            WeldCreationalContext<T> creationalContext = getCreationalContext(invocationContext, nonContextual);

            // pass the injectionTarget into the MO so that preDestroy can be called during "release"
            ManagedObject<T> mo = createManagedObject(instance, creationalContext, injectionTarget, nonContextual);
            return mo;
        }
    }

    @Override
    public ManagedObject<T> createManagedObject(ManagedObjectInvocationContext<T> invocationContext) throws ManagedObjectException {
        ManagedObject<T> mo = createManagedObject(null, invocationContext);
        return mo;
    }

    private ManagedObject<T> createManagedObject(T instance, WeldCreationalContext<T> creationalContext, InjectionTarget<T> injectionTarget, boolean nonContextual) throws ManagedObjectException {
        CDIRuntime cdiRuntime = getCDIRuntime();
        WebSphereCDIDeployment deployment = cdiRuntime.getCurrentDeployment();
        WebSphereInjectionServices webSphereInjectionServices = deployment.getInjectionServices();

        //if the instance is null then use CDI to produce one
        if (instance == null) {
            instance = createInstance(injectionTarget, creationalContext);
        }

        // pass the injectionTarget into the MO so that preDestroy can be called during "release"
        ManagedObject<T> mo = new CDIManagedObject<T>(instance, creationalContext, injectionTarget, getBeanScope(nonContextual), webSphereInjectionServices);

        if (managesInjectionAndInterceptors()) {
            mo.inject(referenceContext);
            // Invoke the PostConstruct callback, if any, by calling the InjectionTarget postConstruct method on the instance.
            injectionTarget.postConstruct(mo.getObject());
        }
        return mo;
    }

    private String getBeanScope(boolean nonContextual) throws ManagedObjectException {
        String beanScope = null;
        if (! nonContextual) { //if nonContextual==true then there is no bean and no beanScope
            Bean<T> bean = getBean();
            if (bean != null) {
                beanScope = bean.getScope().getCanonicalName();
            }
        }
        return beanScope;
    }

    private T createInstance(final InjectionTarget<T> injectionTarget, final WeldCreationalContext<T> creationalContext) {
        // Instantiate the component by calling the InjectionTarget produce method.
        T instance = AccessController.doPrivileged(new PrivilegedAction<T>() {
            @Override
            public T run() {
                return injectionTarget.produce(creationalContext);
            }
        });
        return instance;
    }

}
