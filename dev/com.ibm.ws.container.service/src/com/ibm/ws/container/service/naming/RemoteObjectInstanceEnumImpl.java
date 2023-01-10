/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.container.service.naming;

/**
 *
 */
public class RemoteObjectInstanceEnumImpl implements RemoteObjectInstance {
    private static final long serialVersionUID = -6704870700191348746L;

    @SuppressWarnings("rawtypes")
    final Class<Enum> clazz;
    final String name;

    public RemoteObjectInstanceEnumImpl(@SuppressWarnings("rawtypes") Class<Enum> clazz, String name) {
        this.clazz = clazz;
        this.name = name;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.clientcontainer.remote.common.object.RemoteObjectInstance#getObject(com.ibm.ws.serialization.SerializationService)
     */
    @SuppressWarnings("unchecked")
    @Override
    public Object getObject() throws RemoteObjectInstanceException {
        return Enum.valueOf(clazz, name);
    }

}
