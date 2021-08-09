/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.injectionengine.osgi.internal;

import java.io.NotSerializableException;
import java.io.ObjectOutputStream;

import javax.naming.Reference;

import com.ibm.ejs.util.Util;
import com.ibm.wsspi.resource.ResourceInfo;

@SuppressWarnings("serial")
public class IndirectReference extends Reference {
    final String name;
    final String bindingName;
    final ResourceInfo resourceInfo;
    final String bindingListenerName;
    final boolean defaultBinding;

    public IndirectReference(String name,
                             String bindingName,
                             String type,
                             ResourceInfo resourceInfo,
                             String bindingListenerName,
                             boolean defaultBinding) {
        super(type, IndirectJndiLookupObjectFactory.class.getName(), null);

        if (bindingName == null) {
            // This is an internal error (processors should not pass null).
            // Detect it early to prevent hard-to-diagnose problems.
            throw new IllegalArgumentException("bindingName");
        }

        this.name = name;
        this.bindingName = bindingName;
        this.resourceInfo = resourceInfo;
        this.bindingListenerName = bindingListenerName;
        this.defaultBinding = defaultBinding;
    }

    @Override
    public String toString() {
        return Util.identity(this) + '[' +
               "name=" + name +
               ", bindingName=" + bindingName +
               ", type=" + getClassName() +
               ", resourceInfo=" + resourceInfo +
               ", bindingListenerName=" + bindingListenerName +
               ", defaultBinding=" + defaultBinding +
               ']';
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(this);
    }

    @Override
    public boolean equals(Object o) {
        return this == o;
    }

    private void writeObject(ObjectOutputStream out) throws NotSerializableException {
        throw new NotSerializableException();
    }
}
