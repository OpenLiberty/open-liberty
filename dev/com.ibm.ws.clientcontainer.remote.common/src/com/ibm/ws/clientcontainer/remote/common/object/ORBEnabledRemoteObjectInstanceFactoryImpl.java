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

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.ws.container.service.naming.RemoteObjectInstance;
import com.ibm.ws.container.service.naming.RemoteObjectInstanceFactory;
import com.ibm.ws.container.service.naming.RemoteObjectInstanceFactoryImpl;

/**
 *
 */
@Component(service = RemoteObjectInstanceFactory.class, configurationPolicy = ConfigurationPolicy.IGNORE, immediate = true,
           property = { "service.vendor=IBM", "service.ranking:Integer=10" })
public class ORBEnabledRemoteObjectInstanceFactoryImpl extends RemoteObjectInstanceFactoryImpl {

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.clientcontainer.remote.common.object.RemoteObjectInstanceFactory#create(java.rmi.Remote, java.lang.String)
     */
    @Override
    public RemoteObjectInstance create(Remote remoteObject, String interfaceNameToNarrowTo) {
        return new RemoteCORBAObjectInstanceImpl(remoteObject, interfaceNameToNarrowTo);
    }
}
