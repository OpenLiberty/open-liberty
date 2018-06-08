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

import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.InjectionTarget;

import org.jboss.weld.construction.api.WeldCreationalContext;
import org.jboss.weld.manager.api.WeldInjectionTargetFactory;

import com.ibm.ws.cdi.internal.interfaces.CDIRuntime;
import com.ibm.ws.cdi.internal.interfaces.WebSphereBeanDeploymentArchive;
import com.ibm.ws.cdi.utils.CreationalContextResolver;
import com.ibm.ws.managedobject.ManagedObjectContext;
import com.ibm.ws.managedobject.ManagedObjectException;
import com.ibm.ws.managedobject.ManagedObjectFactory;
import com.ibm.ws.managedobject.ManagedObjectInvocationContext;

public class CDIInterceptorManagedObjectFactoryImpl<T> extends AbstractManagedObjectFactory<T> implements ManagedObjectFactory<T> {

    public CDIInterceptorManagedObjectFactoryImpl(Class<T> classToManage, CDIRuntime cdiRuntime) {
        super(classToManage, cdiRuntime, false);
    }

    /**
     * Get the CreationalContext from an existing ManagedObjectInvocationContext
     *
     * @throws ManagedObjectException
     */
    @Override
    protected WeldCreationalContext<T> getCreationalContext(ManagedObjectInvocationContext<T> invocationContext) throws ManagedObjectException {

        ManagedObjectContext managedObjectContext = invocationContext.getManagedObjectContext();

        @SuppressWarnings("unchecked")
        WeldCreationalContext<T> creationalContext = managedObjectContext.getContextData(WeldCreationalContext.class);
        creationalContext = CreationalContextResolver.resolve(creationalContext, getBean());

        return creationalContext;
    }

    /**
     * {@inheritDoc} We need to override this so that a special interceptor instance was created instead of the common proxied one
     */
    @Override
    protected InjectionTarget<T> getInjectionTarget(boolean nonContextual) {

        InjectionTarget<T> injectionTarget = null;

        Class<T> clazz = getManagedObjectClass();

        WebSphereBeanDeploymentArchive bda = getCurrentBeanDeploymentArchive();
        if (bda != null) {
            injectionTarget = bda.getJEEComponentInjectionTarget(clazz);
        }

        if (injectionTarget == null) {
            AnnotatedType<T> annotatedType = getAnnotatedType(clazz, nonContextual);
            WeldInjectionTargetFactory<T> weldInjectionTargetFactory = getBeanManager().getInjectionTargetFactory(annotatedType);
            injectionTarget = weldInjectionTargetFactory.createInterceptorInjectionTarget();

            if (bda != null) {
                bda.addJEEComponentInjectionTarget(clazz, injectionTarget);
            }
        }

        return injectionTarget;
    }

    @Override
    public String toString() {
        return "CDI Interceptor Managed Object Factory for class: " + getManagedObjectClass().getName();
    }

}
