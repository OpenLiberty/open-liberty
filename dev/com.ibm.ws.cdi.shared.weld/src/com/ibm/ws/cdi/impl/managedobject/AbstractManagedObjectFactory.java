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
import com.ibm.ws.cdi.impl.weld.WebSphereBeanDeploymentArchive;
import com.ibm.ws.cdi.interfaces.CDIRuntime;
import com.ibm.ws.managedobject.ManagedObject;
import com.ibm.ws.managedobject.ManagedObjectContext;
import com.ibm.ws.managedobject.ManagedObjectFactory;
import com.ibm.ws.managedobject.ManagedObjectInvocationContext;
import com.ibm.wsspi.injectionengine.InjectionException;
import com.ibm.wsspi.injectionengine.InjectionTargetContext;
import com.ibm.wsspi.injectionengine.ReferenceContext;

public abstract class AbstractManagedObjectFactory<T> implements ManagedObjectFactory<T> {
    private static final TraceComponent tc = Tr.register(AbstractManagedObjectFactory.class);

    protected final CDIRuntime cdiRuntime;
    protected final Class<T> _managedClass;

    protected Bean<T> _bean;
    protected boolean _beanLookupComplete = false;
    protected WeldManager _beanManager;
    protected WebSphereBeanDeploymentArchive _bda;
    private final boolean _requestManagingInjectionAndInterceptors;

    private ReferenceContext referenceContext = null;

    public AbstractManagedObjectFactory(Class<T> classToManage, CDIRuntime cdiRuntime, boolean requestManagingInjectionAndInterceptors) {
        this._managedClass = classToManage;
        this.cdiRuntime = cdiRuntime;
        this._requestManagingInjectionAndInterceptors = requestManagingInjectionAndInterceptors;
    }

    public AbstractManagedObjectFactory(Class<T> classToManage, CDIRuntime cdiRuntime, boolean requestManagingInjectionAndInterceptors, ReferenceContext referenceContext) {
        this._managedClass = classToManage;
        this.cdiRuntime = cdiRuntime;
        this._requestManagingInjectionAndInterceptors = requestManagingInjectionAndInterceptors;
        this.referenceContext = referenceContext;
    }

    @Override
    public boolean managesInjectionAndInterceptors() {
        return _requestManagingInjectionAndInterceptors;
    }

    @Override
    public Class<T> getManagedObjectClass() {
        return _managedClass;
    }

    @Override
    public boolean isManaged() {
        return true;
    }

    protected synchronized WeldManager getBeanManager() {
        if (this._beanManager == null) {
            this._beanManager = (WeldManager) cdiRuntime.getClassBeanManager(getManagedObjectClass());

            if (this._beanManager == null) {
                this._beanManager = (WeldManager) cdiRuntime.getCurrentModuleBeanManager();
            }
        }

        return this._beanManager;
    }

    protected synchronized WebSphereBeanDeploymentArchive getCurrentBeanDeploymentArchive() {
        if (this._bda == null) {
            this._bda = cdiRuntime.getClassBeanDeploymentArchive(getManagedObjectClass());
        }
        return this._bda;
    }

    @SuppressWarnings("unchecked")
    protected synchronized Bean<T> getBean() {
        if (!_beanLookupComplete) {

            WeldManager beanManager = getBeanManager();

            Set<Bean<?>> beans = beanManager.getBeans(getManagedObjectClass());
            if (beans.size() == 0) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "No Beans found for managed class: " + getManagedObjectClass());
                }
            } else {
                _bean = (Bean<T>) beanManager.resolve(beans);
            }

            _beanLookupComplete = true;

            if (_bean != null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Found a bean of class : " + _bean.getBeanClass());
                }
                if (!_bean.getBeanClass().equals(getManagedObjectClass())) {
                    //this exception should never happen
                    throw new IllegalStateException("Managed Class {" + getManagedObjectClass().getName() + "} does not match Bean Class {"
                                                    + _bean.getBeanClass().getName() + "}");
                }
            }

        }
        return _bean;
    }

    public String getBeanScope() {
        Bean<T> bean = getBean();
        String beanScope = null;
        if (bean != null) {
            beanScope = bean.getScope().getCanonicalName();
        }
        return beanScope;
    }

    public ManagedObject<T> existingInstance(T instance) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ManagedObjectContext createContext() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Constructor<T> getConstructor() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ManagedObject<T> createManagedObject() throws Exception {
        throw new UnsupportedOperationException();
    }

    protected InjectionTarget<T> getInjectionTarget(boolean nonContextual) {
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

            Bean<T> bean = getBean();
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

    protected abstract WeldCreationalContext<T> getCreationalContext(ManagedObjectInvocationContext<T> invocationContext);

    @Override
    public ManagedObject<T> createManagedObject(ManagedObjectInvocationContext<T> invocationContext) throws Exception {

        // 1.Obtain a BeanManager instance.
        WeldManager beanManager = getBeanManager();
        if (beanManager != null) {

            // Create an InjectionTarget instance for the ejbDescriptor.
            boolean nonContextual = invocationContext == null;
            final InjectionTarget<T> injectionTarget = getInjectionTarget(nonContextual);

            // Get the CreationalContext
            final WeldCreationalContext<T> creationalContext = getCreationalContext(invocationContext);

            // Instantiate the component by calling the InjectionTarget produce method.
            T instance = AccessController.doPrivileged(new PrivilegedAction<T>() {
                @Override
                public T run() {
                    return injectionTarget.produce(creationalContext);
                }
            });

            // pass the injectionTarget into the MO so that preDestroy can be called during "release"
            ManagedObject<T> mo = new CDIManagedObject<T>(instance, creationalContext, injectionTarget, getBeanScope());

            if (managesInjectionAndInterceptors()) {
                injectAndPostConstruct(injectionTarget, creationalContext, mo);
            }
            return mo;
        }
        throw new IllegalStateException("Unable to obtain BeanManager");
    }

    /**
     * Perform the field and method injection and then call post construct
     *
     * @param injectionTarget
     * @param creationalContext
     * @param instance
     */
    private void injectAndPostConstruct(InjectionTarget<T> injectionTarget, WeldCreationalContext<T> creationalContext, ManagedObject<T> mo) throws InjectionException {
        //private void injectAndPostConstruct(InjectionTarget<T> injectionTarget, WeldCreationalContext<T> creationalContext, T instance) throws InjectionException {
        // Inject the component instance by calling the InjectionTarget inject method on the instance.
        performInjection(injectionTarget, mo, creationalContext);
        // Invoke the PostConstruct callback, if any, by calling the InjectionTarget postConstruct method on the instance.
        injectionTarget.postConstruct(mo.getObject());
    }

    private void performInjection(final InjectionTarget<T> cdiInjectionTarget, final ManagedObject<T> mo,
                                  final WeldCreationalContext<T> creationalContext) throws InjectionException {

        if (referenceContext != null) {

            // use WAS injection engine to perform injection
            com.ibm.wsspi.injectionengine.InjectionTarget[] targets = referenceContext.getInjectionTargets(mo.getObject().getClass());
            if (null != targets && targets.length > 0) {
                final InjectionTargetContext itc = new InjectionTargetContext() {
                    @Override
                    public <R> R getInjectionTargetContextData(Class<R> data) {
                        return mo.getContextData(data);
                    }
                };
                for (com.ibm.wsspi.injectionengine.InjectionTarget injectionTarget : targets) {
                    injectionTarget.inject(mo.getObject(), itc);
                }
            }

        } else {
            //use Weld to perform injection
            cdiInjectionTarget.inject(mo.getObject(), creationalContext);
        }
    }

}
