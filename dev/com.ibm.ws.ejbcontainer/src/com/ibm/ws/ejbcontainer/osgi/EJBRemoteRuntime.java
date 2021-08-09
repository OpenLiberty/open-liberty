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
package com.ibm.ws.ejbcontainer.osgi;

import org.omg.PortableServer.Servant;

import com.ibm.ejs.container.BeanMetaData;
import com.ibm.ejs.container.EJSRemoteWrapper;
import com.ibm.ejs.container.RemoteAsyncResult;

/**
 * The interface between the core container and the services provided by
 * the remote interface runtime.
 */
public interface EJBRemoteRuntime {
    /**
     * Allocates a remote binding data object for an EJB. This can be used by
     * the remote runtime to store data for subsequent calls to {@link #bind} and {@link #unbind}.
     *
     * @param bmd the bean metadata
     * @param appLogicalName the application logical name, or null if standalone
     * @param moduleLogicalName the module logical name
     * @return the binding data
     */
    Object createBindingData(BeanMetaData bmd, String appLogicalName, String moduleLogicalName);

    /**
     * Notification that an EJB should be made available remotely under the
     * specified interface name.
     *
     * @param bindingData the binding data returned by {@link #createBindingData}
     * @param interfaceIndex the interface index
     * @param interfaceName the interface name
     * @return binding data
     */
    void bind(Object bindingData, int interfaceIndex, String interfaceName);

    /**
     * Notification that a system EJB should be made available remotely under
     * the specified binding name.
     *
     * @param bmd the bean metadata
     * @param homeBindingName the home binding name (e.g., "ejb/test/MyBean")
     * @return the binding data, which can be passed to {@link #unbindAll}
     */
    Object bindSystem(BeanMetaData bmd, String homeBindingName);

    /**
     * Notification that an EJB should no longer be available remotely.
     *
     * @param bindingData the binding data returned by {@link #createBindingData}
     * @param interfaceNames the interface names
     */
    void unbindAll(Object bindingData);

    /**
     * Return a client reference (stub) to a remote wrapper. The reference is
     * not guaranteed to extend or implement any specific interface, so the
     * caller is responsible for narrowing the reference as required.
     *
     * @param remoteObject the remote wrapper
     * @return the client reference (stub) connected to the remote wrapper
     */
    Object getReference(EJSRemoteWrapper remoteObject);

    /**
     * Export the servant for an asynchronous result.
     *
     * @param servant a servant for a RemoteAsyncResultImpl
     * @return the exported object ID
     */
    byte[] activateAsyncResult(Servant servant);

    /**
     * Return a client reference (stub) to a remote asynchronous result.
     */
    RemoteAsyncResult getAsyncResultReference(byte[] oid);

    /**
     * Unexport the servant for an asynchronous result.
     *
     * @param oid the exported object ID from {@link #activateAsyncResult}
     */
    void deactivateAsyncResult(byte[] oid);
}
