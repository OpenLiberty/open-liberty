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
package com.ibm.ws.cdi.impl.managedobject;

import javax.enterprise.inject.spi.InjectionTarget;

import org.jboss.weld.construction.api.WeldCreationalContext;

import com.ibm.ejs.util.Util;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.managedobject.ManagedObject;
import com.ibm.ws.managedobject.ManagedObjectContext;

public class CDIManagedObject<T> implements ManagedObject<T> {
    private static final TraceComponent tc = Tr.register(CDIManagedObject.class);

    private T managedObject;
    private WeldCreationalContext<T> creationalContext = null;
    private InjectionTarget<T> injectionTarget = null;
    private boolean lifecycleManaged = false;
    private String beanScope = null;
    private final String identity;

    public CDIManagedObject(T managedObject, WeldCreationalContext<T> creationalContext, String beanScope) {
        this.creationalContext = creationalContext;
        this.managedObject = managedObject;
        this.beanScope = beanScope;
        this.identity = Util.identity(this.managedObject);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, this.identity + " creating --> " + Util.identity(this.creationalContext) + " beanscope: " + beanScope);
        }
    }

    public CDIManagedObject(T managedObject, WeldCreationalContext<T> creationalContext, InjectionTarget<T> injectionTarget, String beanScope) {
        this(managedObject, creationalContext, beanScope);
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
}
