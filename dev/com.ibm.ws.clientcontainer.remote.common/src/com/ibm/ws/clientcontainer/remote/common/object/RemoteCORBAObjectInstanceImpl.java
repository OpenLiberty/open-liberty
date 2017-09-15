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
package com.ibm.ws.clientcontainer.remote.common.object;

import java.rmi.Remote;
import java.security.AccessController;
import java.security.PrivilegedAction;

import javax.rmi.PortableRemoteObject;

import com.ibm.ws.container.service.naming.RemoteObjectInstance;
import com.ibm.ws.container.service.naming.RemoteObjectInstanceException;

/**
 *
 */
public class RemoteCORBAObjectInstanceImpl implements RemoteObjectInstance {
    private static final long serialVersionUID = -5424320741986130299L;

    final Object remoteObject;
    final String interfaceNameToNarrowTo;

    public RemoteCORBAObjectInstanceImpl(Remote remoteObject, String interfaceNameToNarrowTo) {
        this.remoteObject = remoteObject;
        this.interfaceNameToNarrowTo = interfaceNameToNarrowTo;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.clientcontainer.remote.common.object.RemoteObjectInstance#getObject(com.ibm.ws.serialization.SerializationService)
     */
    @Override
    public Object getObject() throws RemoteObjectInstanceException {
        Object object;

        ClassLoader tccl = AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {

            @Override
            public ClassLoader run() {
                return Thread.currentThread().getContextClassLoader();
            }

        });

        try {
            Class<?> interfaceToNarrowTo = Class.forName(interfaceNameToNarrowTo, false, tccl);
            object = PortableRemoteObject.narrow(remoteObject, interfaceToNarrowTo);
        } catch (ClassNotFoundException ex) {
            throw new RemoteObjectInstanceException("Failed to find class " + interfaceNameToNarrowTo + " from classloader: " + tccl, ex);
        } catch (ClassCastException ex) {
            throw new RemoteObjectInstanceException("Unable to narrow remote object to " + interfaceNameToNarrowTo, ex);
        }

        return object;
    }

}
