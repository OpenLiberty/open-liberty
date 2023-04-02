/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
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
package com.ibm.ejs.container;

import javax.rmi.CORBA.Stub;

/**
 * An extended stub that is capable of reconnecting itself when marshalled via
 * write_value/read_value.
 */
public abstract class SerializableStub
                extends Stub
{
    private final Class ivClass;

    public SerializableStub(Class klass)
    {
        ivClass = klass;
    }

    protected Object writeReplace()
    {
        return new SerializedStub(this, ivClass);
    }
}
