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
package com.ibm.ws.container.service.naming;

import java.rmi.Remote;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

/**
 *
 */
@Component(service = RemoteObjectInstanceFactory.class, configurationPolicy = ConfigurationPolicy.IGNORE, immediate = true,
           property = { "service.vendor=IBM", "service.ranking:Integer=1" })
public class RemoteObjectInstanceFactoryImpl implements RemoteObjectInstanceFactory {

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.clientcontainer.remote.common.object.RemoteObjectInstanceFactory#create(java.lang.Object)
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public RemoteObjectInstance create(Object envEntry) {
        Class<?> clazz = envEntry.getClass();
        if (clazz.isEnum()) {
            return new RemoteObjectInstanceEnumImpl((Class<Enum>) clazz, ((Enum) envEntry).name());
        }
        return new RemoteObjectInstanceImpl(envEntry);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.clientcontainer.remote.common.object.RemoteObjectInstanceFactory#create(byte[])
     */
    @Override
    public RemoteObjectInstance create(byte[] referenceBytes) {
        return new RemoteReferenceObjectInstanceImpl(referenceBytes);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.clientcontainer.remote.common.object.RemoteObjectInstanceFactory#create(java.rmi.Remote, java.lang.String)
     */
    @Override
    public RemoteObjectInstance create(Remote remoteObject, String interfaceNameToNarrowTo) {
        // this instance of the factory cannot create ORB-related objects as it must exist in a
        // bundle that does not have any ORB dependencies.  However, when the appClient feature
        // is enabled, then an ORB-enabled sub-class of this factory will provide the implementation
        // of this method.
        throw new UnsupportedOperationException("Cannot create a remote object instance");
    }

}
