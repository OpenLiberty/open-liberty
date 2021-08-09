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

import java.util.Map;

import javax.enterprise.inject.spi.AnnotatedConstructor;

import org.jboss.weld.construction.api.AroundConstructCallback;
import org.jboss.weld.construction.api.ConstructionHandle;

import com.ibm.ws.managedobject.ManagedObjectInvocationContext;

/**
 * Implementation of AroundConstructCallback that interposes the
 * AroundCallback interceptors on the call to the constructor.
 */
public class EJBInterceptorAroundConstructCallback<T> implements AroundConstructCallback<T> {

    private final ManagedObjectInvocationContext<T> aciCtx;

    public EJBInterceptorAroundConstructCallback(ManagedObjectInvocationContext<T> aciCtx) {

        this.aciCtx = aciCtx;
    }

    /** {@inheritDoc} */
    @Override
    public T aroundConstruct(ConstructionHandle<T> handle, AnnotatedConstructor<T> constructor, Object[] parameters, Map<String, Object> data) throws Exception {
        return aciCtx.aroundConstruct(new ConstructionCallbackImpl<T>(handle, constructor), parameters, data);
    }
}
