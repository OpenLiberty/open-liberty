/*******************************************************************************
 * Copyright (c) 2013, 2023 IBM Corporation and others.
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
package com.ibm.ws.kernel.launch.internal;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

import com.ibm.websphere.ras.annotation.Trivial;

/**
 * Wrapper around a ClassFileTransformer for tracing purposes.
 */
@Trivial
public class TraceClassFileTransformer implements ClassFileTransformer {

    // Force tc to be initialized to avoid problems with the RAS
    // ClassFileTransformer.
    public static void initialize() {
    }

    /**
     * The delegate ClassFileTransformer.
     */
    private final ClassFileTransformer transformer;

    TraceClassFileTransformer(ClassFileTransformer transformer) {
        this.transformer = transformer;
    }

    @Override
    public String toString() {
        return super.toString() + '[' + transformer + ']';
    }

    @Override
    public byte[] transform(ClassLoader loader,
                            String className,
                            Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain,
                            byte[] classfileBuffer) throws IllegalClassFormatException {

        byte[] bytes = transformer.transform(loader, className, classBeingRedefined, protectionDomain, classfileBuffer);

        return bytes;
    }
}
