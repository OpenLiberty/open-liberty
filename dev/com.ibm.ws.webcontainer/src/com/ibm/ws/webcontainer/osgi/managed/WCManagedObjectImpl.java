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
package com.ibm.ws.webcontainer.osgi.managed;

import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.managedobject.ManagedObject;
import com.ibm.ws.managedobject.ManagedObjectContext;
import com.ibm.ws.managedobject.ManagedObjectException;
import com.ibm.wsspi.injectionengine.InjectionException;
import com.ibm.wsspi.injectionengine.InjectionTarget;
import com.ibm.wsspi.injectionengine.InjectionTargetContext;
import com.ibm.wsspi.injectionengine.ReferenceContext;

/**
 *
 */
public class WCManagedObjectImpl<T> implements ManagedObject<T> {

    @Sensitive
    private final T object;

    public WCManagedObjectImpl(@Sensitive T object) {
        this.object = object;
    }

    @Override
    public String toString() {
        return super.toString() + '[' + object.getClass().getName() + '@' + Integer.toHexString(System.identityHashCode(object)) + ']';
    }

    @Sensitive
    @Override
    public T getObject() {
        return object;
    }

    @Override
    public ManagedObjectContext getContext() {
        return null;
    }

    @Override
    public <R> R getContextData(Class<R> klass) {
        return null;
    }

    @Override
    public void release() {}

    @Override
    public boolean isLifecycleManaged() {
        return false;
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.managedobject.ManagedObject#getBeanScope()
     */
    @Override
    public String getBeanScope() {
       
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.managedobject.ManagedObject#inject()
     */
    @Override
    public T inject(ReferenceContext referenceContext) throws ManagedObjectException {
        InjectionTarget[] targets;
        try {
            targets = referenceContext.getInjectionTargets(object.getClass());
        } catch (InjectionException e) {
            throw new ManagedObjectException(e);
        }

        InjectionTargetContext injectionContext = new InjectionTargetContext() {
            @Override
            public <S> S getInjectionTargetContextData(Class<S> data) {
                return getContextData(data);
            }
        };

        T object = inject(targets, injectionContext);
        return object;
    }

    @Override
    public T inject(InjectionTarget[] targets, InjectionTargetContext injectionContext) throws ManagedObjectException {
        for (InjectionTarget injectionTarget : targets) {
            try {
                injectionTarget.inject(object, injectionContext);
            } catch (InjectionException e) {
                throw new ManagedObjectException(e);
            }
        }
        return object;
    }
}
