/*******************************************************************************
 * Copyright (c) 2012, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.serialization.internal;

import java.io.IOException;
import java.io.InputStream;
import java.security.AccessController;
import java.security.PrivilegedAction;

import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.serialization.DeserializationObjectInputStream;

public class DeserializationObjectInputStreamImpl extends DeserializationObjectInputStream implements PrivilegedAction<Void> {
    private final DeserializationContextImpl context;

    public DeserializationObjectInputStreamImpl(InputStream in, ClassLoader classLoader, DeserializationContextImpl context) throws IOException {
        super(in, classLoader);
        this.context = context;

        if (context.isResolveObjectNeeded()) {
            AccessController.doPrivileged(this);
        }
    }

    @Override
    public Void run() {
        enableResolveObject(true);
        return null;
    }

    @Override
    protected Class<?> loadClass(String name) throws ClassNotFoundException {
        Class<?> c = context.loadClass(name);
        if (c == null) {
            // NOTE: If you're investigating a stack trace that shows a
            // ClassNotFoundException for an internal/WAS class via the
            // following call to super.loadClass, then you're either missing
            // DeserializationClassProvider, or you've specified the service
            // properties incorrectly (i.e., typo in the class/package name).
            c = super.loadClass(name);
        }
        return c;
    }

    @Override
    @Sensitive
    protected Object resolveObject(@Sensitive Object object) throws IOException {
        return context.resolveObject(object);
    }
}
