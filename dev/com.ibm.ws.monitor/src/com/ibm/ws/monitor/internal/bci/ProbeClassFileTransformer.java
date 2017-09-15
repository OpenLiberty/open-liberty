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

import static org.objectweb.asm.Opcodes.V1_6;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.Collection;
import java.util.Set;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.util.ASMifier;
import org.objectweb.asm.util.TraceClassVisitor;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.monitor.internal.ProbeImpl;
import com.ibm.ws.monitor.internal.ProbeListener;
import com.ibm.ws.monitor.internal.ProbeManagerImpl;

/**
 * An implementation of a {@link ClassFileTransformer} that manages
 * byte code injection in support of {@link ProbeImpl}s.
 */
public class ProbeClassFileTransformer implements ClassFileTransformer {

    /**
     * Trace component for this class.
     */
    private final static TraceComponent tc = Tr.register(ProbeClassFileTransformer.class);

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
    public ProbeClassFileTransformer(ProbeManagerImpl probeManagerImpl, Instrumentation instrumentation, boolean includeBootstrap) {
        this.probeManagerImpl = probeManagerImpl;
        this.instrumentation = instrumentation;
        this.includeBootstrap = includeBootstrap;
    }

    /**
     * Instrument the provided classes with the appropriate probes.
     * 
     * @param classes target classes to process
     */
    public void instrumentWithProbes(Collection<Class<?>> classes) {
        for (Class<?> clazz : classes) {
            try {
                instrumentation.retransformClasses(clazz);
            } catch (Throwable t) {
            }
        }
    }

    /**
     * Perform necessary transformation and retransformation.
     */
    @Override
    @Sensitive
    public byte[] transform(ClassLoader loader,
                            String className,
                            Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain,
                            @Sensitive byte[] classfileBuffer) throws IllegalClassFormatException {
        // Skip over anything on the bootstrap loader and some VM
        // internal classes (like those in support of reflection)
        if (loader == null && !includeBootstrap) {
            return null;
        } else if (classBeingRedefined == null) {
            return null;
        } else if (!probeManagerImpl.isMonitorable(classBeingRedefined)) {
            return null;
        }

        try {
            return transformWithProbes(classBeingRedefined, classfileBuffer);
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * Inject the byte code required to fire probes.
     * 
     * @param classfileBuffer the source class file
     * @param probes the probe sites to activate
     * 
     * @return the modified class file
     */
    @Sensitive
    byte[] transformWithProbes(Class<?> clazz, @Sensitive byte[] classfileBuffer) {
        Set<ProbeListener> listeners = probeManagerImpl.getInterestedByClass(clazz);
        if (listeners.isEmpty()) {
            return null;
        }

        // reader --> serial version uid adder --> process candidate hook adapter --> tracing --> writer
        ClassReader reader = new ClassReader(classfileBuffer);

        int majorVersion = reader.readShort(6);
        int writerFlags = majorVersion >= V1_6 ? ClassWriter.COMPUTE_FRAMES : ClassWriter.COMPUTE_MAXS;
        ClassWriter writer = new RedefineClassWriter(clazz, reader, writerFlags);

        ClassVisitor visitor = writer;
        //Below Commented IF statement is causing change in StackSize of class resulting in exception when trace is enabled.Temporarily commenting out
        //will investigate further what is causing the below statement to change the stack size.
//        if (tc.isDumpEnabled()) {
//            visitor = new CheckClassAdapter(visitor);
//        }
        ProbeInjectionClassAdapter probeAdapter = new ProbeInjectionClassAdapter(visitor, probeManagerImpl, clazz);
        visitor = probeAdapter;

        // Process the class
        int readerFlags = majorVersion >= V1_6 ? ClassReader.EXPAND_FRAMES : ClassReader.SKIP_FRAMES;
        reader.accept(visitor, readerFlags);

        if (probeAdapter.isModifiedClass()) {
            if (tc.isDumpEnabled()) {
                StringWriter stringWriter = new StringWriter();
                stringWriter.write("=== Original Code ===\n");
                dumpClass(stringWriter, classfileBuffer);
                stringWriter.write("\n");
                stringWriter.write("=== Modified Code ===\n");
                dumpClass(stringWriter, writer.toByteArray());
                Tr.dump(tc, "Bytecode", stringWriter);
            }
            return writer.toByteArray();
        }
        return null;
    }

    @Trivial
    public static void dumpClass(StringWriter stringWriter, byte[] classfileBuffer) {
        try {
            InputStream inputStream = new ByteArrayInputStream(classfileBuffer);
            ClassReader reader = new ClassReader(inputStream);
            reader.accept(new TraceClassVisitor(null,
                            new ASMifier(),
                            new PrintWriter(stringWriter)), 0);
            inputStream.close();
        } catch (Throwable t) {
            if (tc.isDebugEnabled()) {
                Tr.error(tc, "PMI9999E", t.getMessage());
            }
        }
        // System.out.println(stringWriter.toString());
    }
}
