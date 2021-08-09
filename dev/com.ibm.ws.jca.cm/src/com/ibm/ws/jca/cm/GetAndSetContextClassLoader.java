/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jca.cm;

import java.security.PrivilegedAction;

import com.ibm.websphere.ras.annotation.Trivial;

/**
 * Gets the current value of the thread context classloader and then sets it to the specified value.
 */
@Trivial
public class GetAndSetContextClassLoader implements PrivilegedAction<ClassLoader> {
    private final ClassLoader classloader;

    public GetAndSetContextClassLoader(ClassLoader classloader) {
        this.classloader = classloader;
    }

    @Override
    public ClassLoader run() {
        ClassLoader previous = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(classloader);
        return previous;
    }
}
