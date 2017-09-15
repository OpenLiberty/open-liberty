/*******************************************************************************
 * Copyright (c) 2015, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.internal;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;

import javax.ejb.EJBException;
import javax.ejb.EJBObject;
import javax.rmi.PortableRemoteObject;

import com.ibm.ejs.container.BeanId;
import com.ibm.ejs.container.BeanMetaData;
import com.ibm.ejs.container.EJSContainer;
import com.ibm.ejs.container.EJSHome;
import com.ibm.ejs.container.EJSWrapper;
import com.ibm.ejs.container.EJSWrapperBase;
import com.ibm.ejs.container.EJSWrapperCommon;
import com.ibm.ejs.container.util.ByteArray;
import com.ibm.ejs.util.Util;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ejbcontainer.EJBReference;

public class EJBReferenceImpl implements EJBReference {
    private static final TraceComponent tc = Tr.register(EJBReferenceImpl.class,
                                                         "EJBContainer",
                                                         "com.ibm.ejs.container.container");
    private static final long serialVersionUID = 0;

    private transient EJSWrapperCommon wc;
    private transient BeanId beanId;
    private byte[] beanIdBytes;

    EJBReferenceImpl(EJSWrapperCommon wc) {
        this.wc = wc;
        this.beanId = wc.getBeanId();
    }

    private EJSWrapperCommon getEJSWrapperCommon() {
        if (wc == null) {
            EJSContainer container = EJSContainer.getDefaultContainer();
            try {
                beanId = BeanId.getBeanId(new ByteArray(beanIdBytes), container);
                wc = container.getWrapperManager().getWrapper(beanId);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
        return wc;
    }

    @Override
    public <T extends EJBObject> T getEJBObject(Class<T> klass) {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "getEJBObject : " + this + " : " + klass);

        EJSWrapper wrapper = getEJSWrapperCommon().getRemoteWrapper();
        Object wrapperRef;
        try {
            wrapperRef = wrapper.container.getEJBRuntime().getRemoteReference(wrapper);
        } catch (NoSuchObjectException e) {
            // Liberty getRemoteReference never throws.
            throw new IllegalStateException(e);
        }

        T ejbObject = klass.cast(PortableRemoteObject.narrow(wrapperRef, klass));

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "getEJBObject : " + Util.identity(ejbObject));
        return ejbObject;
    }

    @Override
    public <T> T getBusinessObject(Class<T> klass) {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "getBusinessObject : " + this + " : " + klass);

        T ejbObject;
        try {
            ejbObject = klass.cast(getEJSWrapperCommon().getBusinessObject(klass.getName()));
        } catch (RemoteException e) {
            throw new EJBException(e);
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "getBusinessObject : " + Util.identity(ejbObject));
        return ejbObject;
    }

    @Override
    public Object getAggregateLocalObject() {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "getAggregateLocalObject : " + this);

        Object aggregate = getEJSWrapperCommon().getAggregateLocalWrapper();

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "getAggregateLocalObject : " + Util.identity(aggregate));
        return aggregate;
    }

    @Override
    public void remove() {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "remove : " + this);

        EJSWrapperCommon wc = getEJSWrapperCommon();
        EJSHome home = (EJSHome) wc.getBeanId().getHome();

        if (!home.isStatefulSessionHome()) {
            throw new UnsupportedOperationException();
        }

        BeanMetaData bmd = home.getBeanMetaData();
        // Choose an arbitrary business wrapper.
        EJSWrapperBase wrapper = bmd.ivBusinessLocalInterfaceClasses != null ? wc.getLocalBusinessWrapperBase(0) : wc.getRemoteBusinessWrapper(0);
        EJSContainer container = home.getContainer();

        try {
            container.removeBean(wrapper);
        } catch (EJBException e) {
            throw e;
        } catch (Exception e) {
            throw new EJBException(e);
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "remove");
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        if (beanIdBytes == null) {
            // We only serialize the bytes, and we obtain them lazily.
            beanIdBytes = beanId.getByteArrayBytes();
        }
        out.defaultWriteObject();
    }

    @Override
    public String toString() {
        return super.toString() + "[" + beanId + "]";
    }
}
