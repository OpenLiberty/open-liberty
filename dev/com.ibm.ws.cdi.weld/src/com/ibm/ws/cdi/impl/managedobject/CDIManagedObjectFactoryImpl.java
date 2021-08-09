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

import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;

import org.jboss.weld.construction.api.WeldCreationalContext;
import org.jboss.weld.manager.api.WeldManager;

import com.ibm.ejs.util.Util;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.cdi.internal.interfaces.CDIRuntime;
import com.ibm.ws.managedobject.ManagedObject;
import com.ibm.ws.managedobject.ManagedObjectContext;
import com.ibm.ws.managedobject.ManagedObjectException;
import com.ibm.ws.managedobject.ManagedObjectFactory;
import com.ibm.ws.managedobject.ManagedObjectInvocationContext;
import com.ibm.wsspi.injectionengine.ReferenceContext;

public class CDIManagedObjectFactoryImpl<T> extends AbstractManagedObjectFactory<T> implements ManagedObjectFactory<T> {

    private static final TraceComponent tc = Tr.register(CDIManagedObjectFactoryImpl.class);

    public CDIManagedObjectFactoryImpl(Class<T> classToManage, CDIRuntime cdiRuntime, boolean requestManagingInjectionAndInterceptors) {
        super(classToManage, cdiRuntime, requestManagingInjectionAndInterceptors);
    }

    public CDIManagedObjectFactoryImpl(Class<T> classToManage, CDIRuntime cdiRuntime, boolean requestManagingInjectionAndInterceptors, ReferenceContext referenceContext) {
        super(classToManage, cdiRuntime, requestManagingInjectionAndInterceptors, referenceContext);
    }

    @Override
    public ManagedObject<T> existingInstance(T instance) throws ManagedObjectException {
        ManagedObject<T> moi = null;
        BeanManager beanManager = this.getBeanManager();
        if (beanManager != null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "existingInstance entered with: " + Util.identity(instance));
            }
            WeldCreationalContext<T> cc = getCreationalContext(null);
            moi = new CDIManagedObject<T>(instance, cc, null, this.getCDIRuntime().getCurrentDeployment().getInjectionServices());
        }

        return moi;

    }

    @Override
    public ManagedObject<T> createManagedObject() throws ManagedObjectException {
        return super.createManagedObject(null);
    }

    /**
     * Create a new non-Contextual CreationalContext
     *
     * @throws ManagedObjectException
     */
    @Override
    protected WeldCreationalContext<T> getCreationalContext(ManagedObjectInvocationContext<T> invocationContext) throws ManagedObjectException {
        return getCreationalContext(invocationContext, false);
    }

    /**
     * Create a new non-Contextual CreationalContext
     *
     * @throws ManagedObjectException
     */
    @Override
    protected WeldCreationalContext<T> getCreationalContext(ManagedObjectInvocationContext<T> invocationContext, boolean nonContextual) throws ManagedObjectException {

        ManagedObjectContext moc;
        // for managed bean case, if invocation context is not null, use that
        if (invocationContext != null) {
            moc = invocationContext.getManagedObjectContext();

        } else {
            //otherwise, use the one created when creating this object(annotated with scope)
            moc = createContext(nonContextual);
        }
        @SuppressWarnings("unchecked")
        WeldCreationalContext<T> creationalContext = moc.getContextData(WeldCreationalContext.class);
        return creationalContext;
    }

    @Override
    public ManagedObjectContext createContext() throws ManagedObjectException {
        return createContext(false);
    }

    public ManagedObjectContext createContext(boolean nonContextual) throws ManagedObjectException {

        Bean<T> bean = nonContextual ? null : getBean();
        //A ManagedBean may or may not be a CDI bean.
        //If it is a CDI bean then the creational context should be contextual
        //If not the creational context will be non-contextual

        WeldManager beanManager = getBeanManager();
        WeldCreationalContext<T> creationalContext;
        creationalContext = beanManager.createCreationalContext(bean);
        CDIManagedObjectState managedObjectState = new CDIManagedObjectState(creationalContext);
        return managedObjectState;

    }

    @Override
    public String toString() {
        return "CDI Managed Object Factory for class: " + getManagedObjectClass().getName();
    }
}
