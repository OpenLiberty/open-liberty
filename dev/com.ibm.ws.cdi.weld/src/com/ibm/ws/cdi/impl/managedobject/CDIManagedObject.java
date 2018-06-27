/*******************************************************************************
 * Copyright (c) 2012, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.impl.managedobject;

import java.util.HashSet;
import java.util.Set;

import javax.enterprise.inject.spi.InjectionTarget;
import javax.inject.Inject;

import org.jboss.weld.construction.api.WeldCreationalContext;

import com.ibm.ejs.util.Util;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.cdi.CDIException;
import com.ibm.ws.cdi.internal.interfaces.WebSphereInjectionServices;
import com.ibm.ws.cdi.internal.interfaces.WebSphereInjectionTargetListener;
import com.ibm.ws.managedobject.ManagedObject;
import com.ibm.ws.managedobject.ManagedObjectContext;
import com.ibm.ws.managedobject.ManagedObjectException;
import com.ibm.wsspi.injectionengine.InjectionException;
import com.ibm.wsspi.injectionengine.InjectionTargetContext;
import com.ibm.wsspi.injectionengine.ReferenceContext;

public class CDIManagedObject<T> implements ManagedObject<T>, WebSphereInjectionTargetListener<T> {
    private static final TraceComponent tc = Tr.register(CDIManagedObject.class);

    private T managedObject;
    private WeldCreationalContext<T> creationalContext = null;
    private InjectionTarget<T> injectionTarget = null;
    private boolean lifecycleManaged = false;
    private String beanScope = null;
    private final String identity;

    private WebSphereInjectionServices websphereInjectionServices;

    private Set<com.ibm.wsspi.injectionengine.InjectionTarget> currentInjectionTargets;

    private InjectionTargetContext currentInjectionContext;

    public CDIManagedObject(T managedObject, WeldCreationalContext<T> creationalContext, String beanScope, WebSphereInjectionServices websphereInjectionServices) {
        this.creationalContext = creationalContext;
        this.managedObject = managedObject;
        this.beanScope = beanScope;
        this.websphereInjectionServices = websphereInjectionServices;
        this.identity = Util.identity(this.managedObject);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, this.identity + " creating --> " + Util.identity(this.creationalContext) + " beanscope: " + beanScope);
        }
    }

    public CDIManagedObject(T managedObject, WeldCreationalContext<T> creationalContext, InjectionTarget<T> injectionTarget, String beanScope,
                            WebSphereInjectionServices websphereInjectionServices) {
        this(managedObject, creationalContext, beanScope, websphereInjectionServices);
        this.injectionTarget = injectionTarget;
        this.lifecycleManaged = (this.injectionTarget != null);
    }

    @Override
    public <K> K getContextData(Class<K> klass) {
        if (klass == WeldCreationalContext.class)
            return klass.cast(this.creationalContext);
        return null;
    }

    @Override
    public T getObject() {
        return this.managedObject;
    }

    @Override
    public ManagedObjectContext getContext() {
        return new CDIManagedObjectState(this.creationalContext);
    }

    @Override
    public void release() {
        if (null != this.creationalContext) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, this.identity + " releasing --> " + Util.identity(this.creationalContext));
            }
            if (this.injectionTarget != null) {
                try {
                    this.injectionTarget.preDestroy(this.managedObject);
                } catch (Throwable t) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, this.identity + " preDestroy exception --> " + t);
                    }
                }
                this.injectionTarget.dispose(this.managedObject);
                this.injectionTarget = null;
            }
            this.creationalContext.release();
            this.creationalContext = null;
            this.managedObject = null;
        }

    }

    @Override
    public boolean isLifecycleManaged() {
        return this.lifecycleManaged;
    }

    /** {@inheritDoc} */
    @Override
    public String getBeanScope() {
        return this.beanScope;
    }

    @Override
    public String toString() {
        String released = this.creationalContext == null ? " (RELEASED)" : "";
        return "CDIManagedObject: " + this.identity + released;
    }

    /**
     * {@inheritDoc}
     *
     * @throws CDIException
     */
    @Override
    public T inject(ReferenceContext referenceContext) throws ManagedObjectException {
        T instance = getObject();
        //if there is no referenceContext then just skip straight to CDI injection
        if (referenceContext == null) {
            instance = cdiInjection(instance);
        } else {
            //get all the WS Injection Targets from the supplied reference context
            com.ibm.wsspi.injectionengine.InjectionTarget[] injectionTargets;
            try {
                injectionTargets = referenceContext.getInjectionTargets(instance.getClass());
            } catch (InjectionException e) {
                throw new ManagedObjectException(e);
            }

            InjectionTargetContext injectionContext = new InjectionTargetContext() {
                @Override
                public <S> S getInjectionTargetContextData(Class<S> data) {
                    return getContextData(data);
                }
            };
            //call inject using the injection targets
            instance = inject(injectionTargets, injectionContext);
        }

        return instance;
    }

    /** {@inheritDoc} */
    @Override
    public T inject(com.ibm.wsspi.injectionengine.InjectionTarget[] targets, InjectionTargetContext injectionContext) throws ManagedObjectException {
        T instance = getObject();
        synchronized (this) { //we really don't want inject being called twice at the same time
            try {
                this.currentInjectionTargets = new HashSet<>();
                this.currentInjectionContext = injectionContext;
                this.websphereInjectionServices.registerInjectionTargetListener(this);

                //save away the list of current WS Injection Targets
                for (com.ibm.wsspi.injectionengine.InjectionTarget it : targets) {
                    this.currentInjectionTargets.add(it);
                }

                //use Weld to do the CDI injection
                instance = cdiInjection(instance);

                //when cdi injection occurs, we will "cross off" some of the injection targets as they are processed
                //if there are any left then we directly use the injection engine to do the injection for those targets
                for (com.ibm.wsspi.injectionengine.InjectionTarget it : this.currentInjectionTargets) {
                    if (it.getInjectionBinding().getAnnotationType() != Inject.class) {
                        try {
                            it.inject(instance, injectionContext);
                        } catch (InjectionException e) {
                            throw new ManagedObjectException(e);
                        }
                    }
                }
            } finally {
                //always tidy up before we release the lock
                this.websphereInjectionServices.deregisterInjectionTargetListener(this);
                this.currentInjectionContext = null;
                this.currentInjectionTargets = null;
            }

        }

        return instance;
    }

    private T cdiInjection(T instance) {
        //use Weld to perform injection
        this.injectionTarget.inject(instance, this.creationalContext);
        return instance;
    }

    /** {@inheritDoc} */
    @Override
    public void injectionTargetProcessed(com.ibm.wsspi.injectionengine.InjectionTarget injectionTarget) {
        this.currentInjectionTargets.remove(injectionTarget);
    }

    /** {@inheritDoc} */
    @Override
    public InjectionTargetContext getCurrentInjectionTargetContext() {
        return this.currentInjectionContext;
    }
}
