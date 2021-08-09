/*******************************************************************************
 * Copyright (c) 2010, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ras.instrument.internal.main;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.ref.WeakReference;
import java.security.ProtectionDomain;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.util.CheckClassAdapter;
import org.objectweb.asm.util.TraceClassVisitor;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.logging.internal.NLSConstants;
import com.ibm.ws.ras.instrument.internal.bci.LibertyTracingClassAdapter;

/**
 * This class is responsible for instrumenting classes that have been
 * pre-processed during our build with entry / exit tracing. The hope
 * is that we can avoid some of the code bloat and overhead of trace
 * guards by shipping classes without explicit trace and only enabling
 * the code in classes that are included in the enabled trace spec.
 */
public class LibertyRuntimeTransformer implements ClassFileTransformer {

    /**
     * TraceComponent for this class. This is required for debug.
     */
    private final static TraceComponent tc = Tr.register(LibertyRuntimeTransformer.class, NLSConstants.GROUP, NLSConstants.LOGGING_NLS);

    /**
     * Indication that the host is an IBM VM.
     */
    @SuppressWarnings("unused")
    private final static boolean isIBMVirtualMachine = System.getProperty("java.vm.name", "unknown").contains("IBM J9") ||
                                                       System.getProperty("java.vm.name", "unknown").contains("OpenJ9");

    /**
     * Indication that the host is a Sun VM.
     */
    @SuppressWarnings("unused")
    private final static boolean isSunVirtualMachine = System.getProperty("java.vm.name", "unknown").contains("HotSpot");

    /** Issue detailed entry/exit trace for class transforms if this is true. */
    private static final boolean detailedTransformTrace = Boolean.getBoolean("com.ibm.ws.logging.instrumentation.detail.enabled");

    /**
     * The {@link java.lang.instrument.Instrumentation} reference obtained from
     * the OSGi service registry by the RAS component.
     */
    private static Instrumentation instrumentation;

    /**
     * The singleton instance of this class that has been registered with
     * the transformer.
     */
    private static LibertyRuntimeTransformer registeredTransformer = null;

    /**
     * A map of classes to their associated {@link TraceComponent}s. This is
     * needed to determine the trace state during redefines, particularly those
     * that are not initiated by this class.
     */
    // TODO: Multiple trace components for a single class
    private static Map<Class<?>, WeakReference<TraceComponent>> traceComponentByClass = Collections.synchronizedMap(new WeakHashMap<Class<?>, WeakReference<TraceComponent>>());

    // FIXME: This is a workaround for the J9 hot-code-replace bug
    /**
     * An executor that is responsible for transforming
     */
   // private static ExecutorService retransformExecutor = isHotCodeReplaceBroken() ? Executors.newSingleThreadExecutor() : null;

    /**
     * Indication that hot-code-replace is not available or should not be used.
     * Class transforms will be done aggressively as classes are defined to the
     * VM.
     */
    private static boolean injectAtTransform = false;

    /**
     * Flag that indicates we should attempt to work around an emma
     * instrumentation issue by removing the bad local variable table
     * that was left behind.
     */
    private static boolean skipDebugData = false;
    
    private static final Boolean isJDK8WithHotReplaceBug = LibertyJava8WorkaroundRuntimeTransformer.checkJDK8WithHotReplaceBug() ? Boolean.TRUE :  Boolean.FALSE;

    /**
     * Set the {@link java.lang.instrument.Instrumenation} instance to use for
     * for trace injection.
     *
     * @param inst the {@code Instrumentation} reference obtained by RAS
     */
    public static synchronized void setInstrumentation(Instrumentation inst) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "setInstrumentation", inst);

        instrumentation = inst;

        if (instrumentation == null) {
            // TODO: Investigate using attach API to acquire instrumentation - in which case
            // change logic to warn when instrumentation isn't available
            // reference when one wasn't made available by the bootstrap agent
            //  Tr.warning(tc, "INSTRUMENTATION_SERVICE_UNAVAILABLE");
        } else {
            if (Boolean.getBoolean("com.ibm.websphere.ras.inject.at.transform")) {
                setInjectAtTransform(true);
            } else if (!instrumentation.isRetransformClassesSupported()) {
                Tr.info(tc, "INSTRUMENTATION_RETRANSFORM_NOT_SUPPORTED");
                setInjectAtTransform(true);
            }
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "setInstrumentation");
    }

    /**
     * Indicate whether or not to aggressively inject trace at class definition.
     *
     * @param injectAtTransform true if classes should be transformed at definition
     */
    protected static void setInjectAtTransform(boolean injectAtTransform) {
        LibertyRuntimeTransformer.injectAtTransform = injectAtTransform;
        if (injectAtTransform) {
            addTransformer();
        }
    }

    /**
     * Indicate whether or not class debug data should be preserved in the
     * transform.
     */
    protected static void setSkipDebugData(boolean skipDebugData) {
        LibertyRuntimeTransformer.skipDebugData = skipDebugData;
    }

    /**
     * Add the RAS class transformer to the
     * instrumentation {@link java.lang.instrument.ClassFileTransformer} list.
     */
    private static synchronized void addTransformer() {
        if (detailedTransformTrace && tc.isEntryEnabled())
            Tr.entry(tc, "addTransformer");

        if (registeredTransformer == null && instrumentation != null) {
            registeredTransformer = new LibertyRuntimeTransformer();
            instrumentation.addTransformer(registeredTransformer, true);
        }

        if (detailedTransformTrace && tc.isEntryEnabled())
            Tr.exit(tc, "addTransformer");
    }

    /**
     * Determines whether or not class bytes can be transformed.
     *
     * @param bytes the class bytes
     * @return true if the class can be transformed
     */
    private static boolean isTransformPossible(byte[] bytes) {
        if (bytes.length < 8) {
            return false;
        }

        // The transform method will be called for all classes, but ASM is only
        // capable of processing some class file format versions.  That's ok
        // because the transformer only modifies classes that have been
        // preprocessed by our build anyway.
        //
        // ASM doesn't provide a way to determine its max supported version, so
        // we hard code the supported version number.
        int classFileVersion = ((bytes[6] & 0xff) << 8) | (bytes[7] & 0xff);
        
        //Limit bytecode that we transform based on JDK retransform compatibility
        //If we have issues here, 1.8 classes will be instead handled by a separate
        //transformer that only does those classes.
        if (isJDK8WithHotReplaceBug)
            return classFileVersion <= Opcodes.V1_7;
        else
            return classFileVersion <= Opcodes.V11;
    }

    /**
     * Callback from the RAS code that indicates that the trace spec for
     * a {@code TraceComponent} has changed.
     *
     * @param traceComponent the trace component that has changed
     */
    public static void traceStateChanged(final TraceComponent traceComponent) {
        if (instrumentation == null) {
            return;
        }
        if (!injectAtTransform && traceComponent.isEntryEnabled()) {
            final Class<?> traceClass = traceComponent.getTraceClass();
            if ((traceClass != null) && (traceClass != LibertyRuntimeTransformer.class)) {
                LibertyRuntimeTransformer.addTransformer();
                traceComponentByClass.put(traceClass, new WeakReference<TraceComponent>(traceComponent));
                //if (retransformExecutor == null) {
                    retransformClass(traceClass);
//                } else {
//                    // IBM Hot Code Replace Bug:
//                    // Run async on another thread so this thread can return from <clinit>
//                    retransformExecutor.execute(new Runnable() {
//                        @Override
//                        public void run() {
//                            retransformClass(traceClass);
//                        }
//                    });
//                }
            }
        }
    }

    /**
     * Ask the JVM to retransform the class that has recently been enabled for
     * trace. This class will explicitly call the class loader to load and
     * initialize the class to work around an IBM JDK issue with hot code replace
     * during class initialization.
     *
     * @param clazz the class that needs to be instrumented with entry/exit trace
     */
    private final static void retransformClass(Class<?> clazz) {
        if (detailedTransformTrace && tc.isEntryEnabled())
            Tr.entry(tc, "retransformClass", clazz);

        try {
            instrumentation.retransformClasses(clazz);
        } catch (Throwable t) {
            Tr.error(tc, "INSTRUMENTATION_TRANSFORM_FAILED_FOR_CLASS_2", clazz.getName(), t);
        }

        if (detailedTransformTrace && tc.isEntryEnabled())
            Tr.exit(tc, "retransformClass");
    }
    
    public static byte[] transform(byte[] bytes) throws IOException {
    	return transform(bytes, true);
    }
    
    /**
     * Instrument the class at the current position in the specified input stream.
     *
     * @return instrumented class file or null if the class has already
     *         been instrumented.
     *
     * @throws IOException if an error is encountered while reading from
     *             the <code>InputStream</code>
     */
    public static byte[] transform(byte[] bytes, boolean skipIfNotPreprocessed) throws IOException {
        if (detailedTransformTrace && tc.isEntryEnabled())
            Tr.entry(tc, "transform");
        
        ClassReader reader = new ClassReader(bytes);
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);

        StringWriter sw = null;
        ClassVisitor visitor = writer;
        if (tc.isDumpEnabled()) {
            sw = new StringWriter();
            visitor = new CheckClassAdapter(visitor, false);
            visitor = new TraceClassVisitor(visitor, new PrintWriter(sw));
        }

        LibertyTracingClassAdapter tracingClassAdapter = new LibertyTracingClassAdapter(visitor, skipIfNotPreprocessed);
        try {
            // Class reader must maintain all metadata information that's present in
            // the class
            reader.accept(tracingClassAdapter, skipDebugData ? ClassReader.SKIP_DEBUG : 0);
        } catch (Throwable t) {
            IOException ioe = new IOException("Unable to instrument class stream with trace: " + t.getMessage(), t);
            throw ioe;
        }

        // Provide a whole lot of detailed information on the resulting class
        if (detailedTransformTrace && tc.isDumpEnabled() && tracingClassAdapter.isClassModified()) {
            Tr.dump(tc, "Transformed class", sw);
        }

        // Try to short circuit when the class didn't change
        byte[] result = tracingClassAdapter.isClassModified() ? writer.toByteArray() : null;

        if (detailedTransformTrace && tc.isEntryEnabled())
            Tr.exit(tc, "transform", result);
        return result;
    }

    /**
     * {@inheritDoc} <p>
     * This method will only execute a transformation if we're configured to class
     * has been explicitly targeted for injection by {@link #traceStateChanged} or
     * if we're forced to perform transformation against all classes at class
     * definition because the host JVM doesn't support class retransformation or
     * class redefinition.
     */
    @Override
    public byte[] transform(ClassLoader loader,
                            String className,
                            Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain,
                            byte[] classfileBuffer) throws IllegalClassFormatException {
        if (detailedTransformTrace && tc.isEntryEnabled())
            Tr.entry(this, tc, "transform", loader, className, classBeingRedefined, protectionDomain);

        byte[] newClassBytes = null;
        if (isTransformPossible(classfileBuffer)) {
            boolean traceEnabledForClass = injectAtTransform;
            if (!injectAtTransform && classBeingRedefined != null) {
                WeakReference<TraceComponent> tcReference = traceComponentByClass.get(classBeingRedefined);
                TraceComponent traceComponent = tcReference == null ? null : tcReference.get();
                traceEnabledForClass |= (traceComponent != null && traceComponent.isEntryEnabled());
            }
            if (traceEnabledForClass) {
                try {
                    newClassBytes = transform(classfileBuffer);
                } catch (Throwable t) {
                    Tr.error(tc, "INSTRUMENTATION_TRANSFORM_FAILED_FOR_CLASS_2", className, t);
                }
            }
        }

        if (detailedTransformTrace && tc.isEntryEnabled())
            Tr.exit(this, tc, "transform", newClassBytes);
        return newClassBytes;
    }
}
