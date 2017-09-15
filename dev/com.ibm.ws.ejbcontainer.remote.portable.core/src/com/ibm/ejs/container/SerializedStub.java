/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.container;

import java.io.Serializable;

import javax.rmi.PortableRemoteObject;

/**
 * A wrapper for a stub. When a stub is read using read_value, the ORB will
 * not reconnect the stub. When rmic compatibility is enabled, stubs for
 * interfaces that are not RMI/IDL abstract interfaces will extend
 * SerializableStub, which has a writeReplace method to substitute an instance
 * of this object. The ORB will reconnect stubs stored in instance fields of
 * this serialized object.
 */
public class SerializedStub
                implements Serializable
{
    private static final long serialVersionUID = 3019532699780090519L;

    private final Object ivStub;
    private final Class ivClass;

    SerializedStub(Object stub, Class klass)
    {
        ivStub = stub;
        ivClass = klass;
    }

    private Object readResolve()
    {
        return PortableRemoteObject.narrow(ivStub, ivClass);
    }
}
