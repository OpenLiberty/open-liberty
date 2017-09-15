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

import java.util.Arrays;

/**
 *
 */
public class RemoteReferenceObjectInstanceImpl implements RemoteObjectInstance {
    private static final long serialVersionUID = -6271605053333022950L;

    private final byte[] referenceBytes;

    public RemoteReferenceObjectInstanceImpl(byte[] referenceBytes) {
        this.referenceBytes = Arrays.copyOf(referenceBytes, referenceBytes.length);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.clientcontainer.remote.common.object.RemoteObjectInstance#getObject(com.ibm.ws.serialization.SerializationService)
     */
    @Override
    public Object getObject() throws RemoteObjectInstanceException {
        return referenceBytes;
    }

}
