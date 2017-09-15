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
package com.ibm.ws.kernel.launch.internal;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * Wrapper around a ClassFileTransformer for tracing purposes.
 */
public class TraceClassFileTransformer implements ClassFileTransformer {
    static final TraceComponent tc = Tr.register(TraceClassFileTransformer.class, "instrumentation");

    // Force tc to be initialized to avoid problems with the RAS
    // ClassFileTransformer.
    public static void initialize() {}

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
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "transform", transformer, loader, className, classBeingRedefined, protectionDomain, classfileBuffer.length);

        byte[] bytes = transformer.transform(loader, className, classBeingRedefined, protectionDomain, classfileBuffer);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "transform", bytes == null ? null : bytes.length);
        return bytes;
    }
}
