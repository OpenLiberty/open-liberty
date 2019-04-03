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
import java.lang.instrument.Instrumentation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.jar.JarFile;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;

/**
 * Wrapper around Instrumentation for tracing purposes.
 */
public class TraceInstrumentation implements InvocationHandler {
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
    private final Map<ClassFileTransformer, TraceClassFileTransformer> traceTransformers = Collections.synchronizedMap(new IdentityHashMap<ClassFileTransformer, TraceClassFileTransformer>());

    public TraceInstrumentation(Instrumentation instrumentation) {
        this.instrumentation = instrumentation;
    }

    // If this method is traced it can call proxy.toString which causes another invoke call leading to an infinite loop.
    @Override
    @Trivial
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if ("addTransformer".equals(method.getName())) {
            addTransformer((ClassFileTransformer) args[0], args.length > 1 ? (Boolean) args[1] : false);
            return null;
        }
        if ("removeTransformer".equals(method.getName())) {
            return removeTransformer((ClassFileTransformer) args[0]);
        }
        if ("appendToBootstrapClassLoaderSearch".equals(method.getName())) {
            appendToBootstrapClassLoaderSearch((JarFile) args[0]);
            return null;
        }
        if ("appendToSystemClassLoaderSearch".equals(method.getName())) {
            appendToSystemClassLoaderSearch((JarFile) args[0]);
            return null;
        }
        if ("setNativeMethodPrefix".equals(method.getName())) {
            setNativeMethodPrefix((ClassFileTransformer) args[0], (String) args[1]);
            return null;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, method.getName(), args);
        }

        Object retValue = method.invoke(instrumentation, args);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            if (void.class == method.getReturnType()) {
                Tr.exit(tc, method.getName());
            } else {
                Tr.exit(tc, method.getName(), retValue);
            }
        }
        return retValue;
    }

    private void addTransformer(ClassFileTransformer transformer, boolean canRetransform) {
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

    private boolean removeTransformer(ClassFileTransformer transformer) {
        // Throw NPE per method contract.  Check explicitly to avoid wrongly
        // returning false if traceTransformers.get returns null.
        if (transformer == null) {
            throw new NullPointerException();
        }

        TraceClassFileTransformer traceTransformer = traceTransformers.remove(transformer);
        return traceTransformer != null && instrumentation.removeTransformer(traceTransformer);
    }

    private static String jarFileToString(JarFile jarfile) {
        return jarfile == null ? null : jarfile.toString() + '[' + jarfile.getName() + ']';
    }

    private void appendToBootstrapClassLoaderSearch(JarFile jarFile) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "appendToBootstrapClassLoaderSearch", jarFileToString(jarFile));

        instrumentation.appendToBootstrapClassLoaderSearch(jarFile);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "appendToBootstrapClassLoaderSearch");
    }

    private void appendToSystemClassLoaderSearch(JarFile jarFile) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "appendToSystemClassLoaderSearch", jarFileToString(jarFile));

        instrumentation.appendToSystemClassLoaderSearch(jarFile);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "appendToSystemClassLoaderSearch");
    }

    private void setNativeMethodPrefix(ClassFileTransformer transformer, String prefix) {
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
