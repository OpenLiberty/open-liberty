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
package com.ibm.ws.managedobject.internal;

import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.managedobject.ManagedObject;
import com.ibm.ws.managedobject.ManagedObjectContext;

public class ManagedObjectImpl<T> implements ManagedObject<T> {
    @Sensitive
    private final T object;

    public ManagedObjectImpl(@Sensitive T object) {
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

    @Override
    public String getBeanScope() {
        return null;
    }
}
