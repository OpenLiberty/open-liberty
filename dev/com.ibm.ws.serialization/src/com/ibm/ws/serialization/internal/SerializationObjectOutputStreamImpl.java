/*******************************************************************************
 * Copyright (c) 2012, 2013 IBM Corporation and others.
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
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.security.AccessController;
import java.security.PrivilegedAction;

import com.ibm.websphere.ras.annotation.Sensitive;

public class SerializationObjectOutputStreamImpl extends ObjectOutputStream implements PrivilegedAction<Void> {
    private final SerializationContextImpl context;

    public SerializationObjectOutputStreamImpl(OutputStream output, SerializationContextImpl context) throws IOException {
        super(output);
        this.context = context;

        if (context.isReplaceObjectNeeded()) {
            AccessController.doPrivileged(this);
        }
    }

    @Override
    public Void run() {
        enableReplaceObject(true);
        return null;
    }

    @Override
    @Sensitive
    protected Object replaceObject(@Sensitive Object object) throws IOException {
        return context.replaceObject(object);
    }
}
