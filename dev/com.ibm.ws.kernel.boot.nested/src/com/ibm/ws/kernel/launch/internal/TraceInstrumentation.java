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

import java.lang.instrument.ClassDefinition;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.jar.JarFile;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * Wrapper around Instrumentation for tracing purposes.
 */
public class TraceInstrumentation implements Instrumentation {
    static final TraceComponent tc = Tr.register(TraceInstrumentation.class, "instrumentation");

    static {
        // Force TraceClassFileTransformer's call to Tr.register now to avoid
        // problems with the RAS ClassFileTransformer.
        TraceClassFileTransformer.initialize();
    }

    /**
     * The underlying Instrumentation from the JVM.
     */
    private final Instrumentation instrumentation;

    /**
     * Map of caller ClassFileTransformer to our wrapper.
     */
    private final Map<ClassFileTransformer, TraceClassFileTransformer> traceTransformers =
                    Collections.synchronizedMap(new IdentityHashMap<ClassFileTransformer, TraceClassFileTransformer>());

    public TraceInstrumentation(Instrumentation instrumentation) {
        this.instrumentation = instrumentation;
    }

    @Override
    public void addTransformer(ClassFileTransformer transformer, boolean canRetransform) {
        // Throw NPE per method contract rather than wrapping null.
        if (transformer == null) {
            throw new NullPointerException();
        }

        TraceClassFileTransformer traceTransformer = new TraceClassFileTransformer(transformer);
        try {
            instrumentation.addTransformer(traceTransformer, canRetransform);
        } finally {
            traceTransformers.put(transformer, traceTransformer);
        }
    }

    @Override
    public void addTransformer(ClassFileTransformer transformer) {
        // Throw NPE per method contract rather than wrapping null.
        if (transformer == null) {
            throw new NullPointerException();
        }

        TraceClassFileTransformer traceTransformer = new TraceClassFileTransformer(transformer);
        try {
            instrumentation.addTransformer(traceTransformer);
        } finally {
            traceTransformers.put(transformer, traceTransformer);
        }
    }

    @Override
    public boolean removeTransformer(ClassFileTransformer transformer) {
        // Throw NPE per method contract.  Check explicitly to avoid wrongly
        // returning false if traceTransformers.get returns null.
        if (transformer == null) {
            throw new NullPointerException();
        }

        TraceClassFileTransformer traceTransformer = traceTransformers.remove(transformer);
        return traceTransformer != null && instrumentation.removeTransformer(traceTransformer);
    }

    @Override
    public boolean isRetransformClassesSupported() {
        return instrumentation.isRetransformClassesSupported();
    }

    @Override
    public void retransformClasses(Class<?>... classes) throws UnmodifiableClassException {
        instrumentation.retransformClasses(classes);
    }

    @Override
    public boolean isRedefineClassesSupported() {
        return instrumentation.isRedefineClassesSupported();
    }

    @Override
    public void redefineClasses(ClassDefinition... definitions) throws ClassNotFoundException, UnmodifiableClassException {
        instrumentation.redefineClasses(definitions);
    }

    @Override
    public boolean isModifiableClass(Class<?> theClass) {
        return instrumentation.isModifiableClass(theClass);
    }

    @Override
    public Class<?>[] getAllLoadedClasses() {
        return instrumentation.getAllLoadedClasses();
    }

    @Override
    public Class<?>[] getInitiatedClasses(ClassLoader loader) {
        return instrumentation.getInitiatedClasses(loader);
    }

    @Override
    public long getObjectSize(Object objectToSize) {
        return instrumentation.getObjectSize(objectToSize);
    }

    private static String jarFileToString(JarFile jarfile) {
        return jarfile == null ? null : jarfile.toString() + '[' + jarfile.getName() + ']';
    }

    @Override
    public void appendToBootstrapClassLoaderSearch(JarFile jarFile) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "appendToBootstrapClassLoaderSearch", jarFileToString(jarFile));

        instrumentation.appendToBootstrapClassLoaderSearch(jarFile);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "appendToBootstrapClassLoaderSearch");
    }

    @Override
    public void appendToSystemClassLoaderSearch(JarFile jarFile) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "appendToSystemClassLoaderSearch", jarFileToString(jarFile));

        instrumentation.appendToSystemClassLoaderSearch(jarFile);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "appendToSystemClassLoaderSearch");
    }

    @Override
    public boolean isNativeMethodPrefixSupported() {
        return instrumentation.isNativeMethodPrefixSupported();
    }

    @Override
    public void setNativeMethodPrefix(ClassFileTransformer transformer, String prefix) {
        // Throw NPE per method contract.  Check explicitly to avoid throwing
        // the wrong exception if traceTransformers.get returns null.
        if (transformer == null) {
            throw new NullPointerException();
        }

        ClassFileTransformer traceTransformer = traceTransformers.get(transformer);

        // Throw IAE per method contract.
        if (traceTransformer == null) {
            throw new IllegalArgumentException();
        }

        instrumentation.setNativeMethodPrefix(traceTransformer, prefix);
    }
}
