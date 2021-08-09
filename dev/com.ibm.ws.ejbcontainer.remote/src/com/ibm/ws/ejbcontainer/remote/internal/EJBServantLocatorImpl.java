/*******************************************************************************
 * Copyright (c) 2014, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.remote.internal;

import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectOutputStream;
import java.rmi.RemoteException;

import javax.rmi.CORBA.Tie;

import org.omg.CORBA.LocalObject;
import org.omg.CORBA.OBJECT_NOT_EXIST;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.Servant;
import org.omg.PortableServer.ServantLocator;
import org.omg.PortableServer.ServantLocatorPackage.CookieHolder;

import com.ibm.ejs.container.BeanId;
import com.ibm.ejs.container.BeanMetaData;
import com.ibm.ejs.container.EJSContainer;
import com.ibm.ejs.container.EJSRemoteWrapper;
import com.ibm.ejs.container.WrapperId;
import com.ibm.ejs.container.WrapperInterface;
import com.ibm.ejs.container.WrapperManager;
import com.ibm.ws.ejbcontainer.InternalConstants;
import com.ibm.ws.ejbcontainer.jitdeploy.CORBA_Utils;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

@SuppressWarnings("serial")
public class EJBServantLocatorImpl extends LocalObject implements ServantLocator {
    private final WrapperManager wrapperManager;

    EJBServantLocatorImpl(WrapperManager wrapperManager) {
        this.wrapperManager = wrapperManager;
    }

    private void writeObject(ObjectOutputStream output) throws IOException {
        throw new NotSerializableException();
    }

    @Override
    // WrapperManager has already FFDC'ed.
    @FFDCIgnore(RemoteException.class)
    public Servant preinvoke(byte[] oid, POA adapter, String operation, CookieHolder the_cookie) {
        try {
            EJSRemoteWrapper w = wrapperManager.keyToObject(oid);
            if (w == null) {
                // tWAS/ibmorb uses minor code SERVANT_NOT_FOUND_1
                throw new OBJECT_NOT_EXIST();
            }

            Servant servant = getServant(w);

            EJSContainer.getThreadData().pushClassLoader(w.bmd);

            return servant;
        } catch (RemoteException e) {
            // tWAS/ibmorb uses minor code SERVANT_NOT_FOUND_4
            OBJECT_NOT_EXIST e2 = new OBJECT_NOT_EXIST();
            e2.initCause(e);
            throw e2;
        }
    }

    private Servant getServant(EJSRemoteWrapper w) {
        Tie tie;
        synchronized (w) {
            tie = w.intie;
            if (tie == null) {
                tie = createTie(w);
                w.intie = tie;
            }
        }

        // All ties we generate extend Servant.
        return (Servant) tie;
    }

    private Tie createTie(EJSRemoteWrapper w) {
        Class<Tie> tieClass = getTieClass(w);

        Tie tie;
        try {
            tie = tieClass.newInstance();
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        } catch (InstantiationException e) {
            throw new IllegalStateException(e);
        }

        tie.setTarget(w);
        return tie;
    }

    @SuppressWarnings("unchecked")
    private Class<Tie> getTieClass(EJSRemoteWrapper w) {
        if (w.ivInterface == WrapperInterface.HOME) {
            return (Class<Tie>) w.bmd.homeRemoteTieClass;
        }
        if (w.ivInterface == WrapperInterface.REMOTE) {
            return (Class<Tie>) w.bmd.remoteTieClass;
        }
        if (w.ivInterface == WrapperInterface.BUSINESS_REMOTE ||
            w.ivInterface == WrapperInterface.BUSINESS_RMI_REMOTE) {
            return (Class<Tie>) w.bmd.ivBusinessRemoteTieClasses[w.ivBusinessInterfaceIndex];
        }
        throw new IllegalStateException(String.valueOf(w.ivInterface));
    }

    public static Object getReference(EJSRemoteWrapper w, POA adapter) {
        Object stub;
        synchronized (w) {
            stub = w.instub;
            if (stub == null) {
                stub = createReference(w, adapter);
                w.instub = stub;
            }
        }

        return stub;
    }

    private static Object createReference(EJSRemoteWrapper w, POA adapter) {
        Class<?> intf;
        byte[] oid;

        if (w.ivInterface == WrapperInterface.HOME) {
            intf = w.bmd.homeInterfaceClass;
            oid = w.beanId.getByteArrayBytes();
        } else if (w.ivInterface == WrapperInterface.REMOTE) {
            intf = w.bmd.remoteInterfaceClass;
            oid = w.beanId.getByteArrayBytes();
        } else if (w.ivInterface == WrapperInterface.BUSINESS_REMOTE ||
                   w.ivInterface == WrapperInterface.BUSINESS_RMI_REMOTE) {
            int interfaceIndex = w.ivBusinessInterfaceIndex;
            intf = w.bmd.ivBusinessRemoteInterfaceClasses[interfaceIndex];

            WrapperId wrapperId = new WrapperId(w.beanId.getByteArrayBytes(), intf.getName(), interfaceIndex);
            oid = wrapperId.getBytes();
        } else {
            throw new IllegalStateException(String.valueOf(w.ivInterface));
        }

        return adapter.create_reference_with_id(oid, CORBA_Utils.getRemoteTypeId(intf));
    }

    static org.omg.CORBA.Object createBindingReference(BeanMetaData bmd, int interfaceIndex, String intf, POA adapter) {
        byte[] oid;
        if (interfaceIndex == -1) {
            BeanId beanId = new BeanId(bmd.container.getHomeOfHomes().getJ2EEName(), bmd.j2eeName, true);
            oid = beanId.getByteArrayBytes();
        } else {
            if (bmd.type == InternalConstants.TYPE_STATEFUL_SESSION) {
                return new StatefulResolverImpl(bmd, interfaceIndex);
            }

            BeanId beanId = new BeanId(bmd.j2eeName, null, false);
            WrapperId wrapperId = new WrapperId(beanId.getByteArrayBytes(), intf, interfaceIndex);
            oid = wrapperId.getBytes();
        }

        return adapter.create_reference_with_id(oid, CORBA_Utils.getRemoteTypeId(intf));
    }

    @Override
    public void postinvoke(byte[] oid, POA adapter, String operation, Object the_cookie, Servant the_servant) {
        EJSContainer.getThreadData().popClassLoader();
    }
}
