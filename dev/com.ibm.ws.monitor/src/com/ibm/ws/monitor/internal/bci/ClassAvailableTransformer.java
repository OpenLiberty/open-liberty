/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.monitor.internal.bci;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.SerialVersionUIDAdder;
import org.objectweb.asm.util.CheckClassAdapter;
import org.objectweb.asm.util.TraceClassVisitor;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.monitor.internal.ProbeManagerImpl;

/**
 * An implementation of a retransform incapable {@link ClassFileTransformer} adds code to the class static initializer to notify the monitoring code
 * that the class is available.
 */
public class ClassAvailableTransformer implements ClassFileTransformer {

    /**
     * Trace component for this class.
     */
    private final static TraceComponent tc = Tr.register(ClassAvailableTransformer.class);

    /**
     * Reference to the component that is responsible for tracking listeners
     * and managing probes.
     */
    final ProbeManagerImpl probeManagerImpl;

    /**
     * The {@link java.lang.instrument.Instrumentation} reference required to
     * monitor, instrument, and transform classes at runtime.
     */
    final Instrumentation instrumentation;

    /**
     * Flag that determines whether or not classes loaded by the bootstrap
     * class loader are eligible probe targets.
     */
    final boolean includeBootstrap;

    /**
     * Create a new transformer and associate it with the specified {@code ProbeManagerImpl}.
     * 
     * @param probeManagerImpl the probe management component
     * @param instrumentation the {@code Instrumentation} reference for this VM
     * @param includeBootstrap include classes defined to the bootstrap loader
     */
    public ClassAvailableTransformer(ProbeManagerImpl probeManagerImpl, Instrumentation instrumentation, boolean includeBootstrap) {
        this.probeManagerImpl = probeManagerImpl;
        this.instrumentation = instrumentation;
        this.includeBootstrap = includeBootstrap;
    }

    /**
     * Perform necessary transformation to hook static initializers of probe
     * candidates.
     */
    @Override
    public byte[] transform(ClassLoader loader,
                            String className,
                            Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain,
                            byte[] classfileBuffer) throws IllegalClassFormatException {
        // Skip over anything on the bootstrap loader and some VM
        // internal classes (like those in support of reflection)
        if ((loader == null && !includeBootstrap) || probeManagerImpl.isExcludedClass(className)) {
            return null;
        }

        // If this is a probe candidate, hook the static initializer
        if (probeManagerImpl.isProbeCandidate(className)) {
            return transformCandidate(classfileBuffer);
        }

        return null;
    }

    /**
     * Inject the byte code required to call the {@code processCandidate} proxy
     * after class initialization.
     * 
     * @param classfileBuffer the source class file
     * 
     * @return the modified class file
     */
    byte[] transformCandidate(byte[] classfileBuffer) {
        // reader --> serial version uid adder --> process candidate hook adapter --> tracing --> writer
        ClassReader reader = new ClassReader(classfileBuffer);
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);

        ClassVisitor visitor = writer;
        StringWriter stringWriter = null;
        if (tc.isDumpEnabled()) {
            stringWriter = new StringWriter();
            visitor = new CheckClassAdapter(visitor);
            visitor = new TraceClassVisitor(visitor, new PrintWriter(stringWriter));
        }
        visitor = new ClassAvailableHookClassAdapter(visitor);
        visitor = new SerialVersionUIDAdder(visitor);

        // Process the class
        reader.accept(visitor, 0);

        if (stringWriter != null && tc.isDumpEnabled()) {
            Tr.dump(tc, "Transformed class", stringWriter);
        }

        return writer.toByteArray();
    }

}
