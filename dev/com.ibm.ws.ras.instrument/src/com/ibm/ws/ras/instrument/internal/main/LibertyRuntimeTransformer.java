/*******************************************************************************
 * Copyright (c) 2010, 2023 IBM Corporation and others.
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

import io.openliberty.asm.ASMHelper;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.logging.internal.NLSConstants;
import com.ibm.ws.ras.instrument.internal.bci.AbstractRasClassAdapter;
import com.ibm.ws.ras.instrument.internal.bci.LibertyTracingClassAdapter;

/**
 * This class is responsible for instrumenting classes that have been
 * pre-processed during our build with entry / exit tracing. The hope
 * is that we can avoid some of the code bloat and overhead of trace
 * guards by shipping classes without explicit trace and only enabling
 * the code in classes that are included in the enabled trace spec.
 */
public class LibertyRuntimeTransformer implements ClassFileTransformer {

    public static final String CLASS_NAME = "RuntimeTransformer";

    public static boolean isLoggableClassName(String className) {
        return FileLogger.isLoggableClassName(className);
    }
    
    public static boolean isLoggablePath(String path) {
        return FileLogger.isLoggablePath(path);
    }

    public static PrintWriter fileWriter() {
        return FileLogger.fileWriter();
    }

    public static void fileLog(String methodName, String text) {
        FileLogger.fileLog(CLASS_NAME, methodName, text);
    }
    
    public static void fileLog(String methodName, String text, Object value) {
        FileLogger.fileLog(CLASS_NAME, methodName, text, value);
    }
    
    public static void fileDump(String methodName, String text, byte[] bytes) {
        FileLogger.fileDump(CLASS_NAME, methodName, text, bytes);
    }   
    
    public static void fileStack(String methodName, String text, Throwable th) {
        FileLogger.fileStack(CLASS_NAME, methodName, text, th);
    }   

    static {
        fileLog("<init>", "Initializing");
    }

    /** TraceComponent for this class. This is required for debug. */
    private final static TraceComponent tc =
        Tr.register(LibertyRuntimeTransformer.class,
                    NLSConstants.GROUP,
                    NLSConstants.LOGGING_NLS);

    /** Indication that the host is an IBM VM. */
    @SuppressWarnings("unused")
    private final static boolean isIBMVirtualMachine =
        System.getProperty("java.vm.name", "unknown").contains("IBM J9") ||
        System.getProperty("java.vm.name", "unknown").contains("OpenJ9");

    /** Indication that the host is a Sun VM. */
    @SuppressWarnings("unused")
    private final static boolean isSunVirtualMachine =
        System.getProperty("java.vm.name", "unknown").contains("HotSpot");

    /** Issue detailed entry/exit trace for class transforms if this is true. */
    private static final boolean detailedTransformTrace =
        Boolean.getBoolean("com.ibm.ws.logging.instrumentation.detail.enabled");

    /**
     * The {@link java.lang.instrument.Instrumentation} reference obtained from
     * the OSGi service registry by the RAS component.
     */
    private static Instrumentation instrumentation;

    /**
     * The singleton instance of this class that has been registered with
     * the transformer.
     */
    private static LibertyRuntimeTransformer registeredTransformer;

    /**
     * A map of classes to their associated {@link TraceComponent}s. This is
     * needed to determine the trace state during redefines, particularly those
     * that are not initiated by this class.
     */
    // TODO: Multiple trace components for a single class
    private static Map<Class<?>, WeakReference<TraceComponent>> traceComponentByClass =
        Collections.synchronizedMap(new WeakHashMap<Class<?>, WeakReference<TraceComponent>>());

    /**
     * Indication that hot-code-replace is not available or should not be used.
     * Class transforms will be done aggressively as classes are defined to the
     * VM.
     */
    private static boolean injectAtTransform;

    /**
     * Flag that indicates we should attempt to work around an emma
     * instrumentation issue by removing the bad local variable table
     * that was left behind.
     */
    private static boolean skipDebugData;
    
    private static final Boolean isJDK8WithHotReplaceBug =
        LibertyJava8WorkaroundRuntimeTransformer.checkJDK8WithHotReplaceBug() ? Boolean.TRUE :  Boolean.FALSE;

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

            fileLog("addTransformer", "Transformer", registeredTransformer);
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
    private static String isTransformPossible(byte[] bytes) {
        if (bytes.length < 8) {
            return "Incomplete class bytes [ " + bytes.length + " ]";
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
        if (isJDK8WithHotReplaceBug) {
            if ( classFileVersion > Opcodes.V1_7 ) {
                return "HotReplaceBug: Class version [ " + classFileVersion + " ] Maximum [ " + Opcodes.V1_7 + " ]";
            } else {
                return null;
            }
        } else {
            if ( classFileVersion > Opcodes.V22 ) {
                return "Class version [ " + classFileVersion + " ] Maximum [ " + Opcodes.V22 + " ]";
            } else {
                return null;
            }
        }
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
            Class<?> traceClass = traceComponent.getTraceClass();
            if ((traceClass != null) && (traceClass != LibertyRuntimeTransformer.class)) {
                LibertyRuntimeTransformer.addTransformer();
                traceComponentByClass.put(traceClass, new WeakReference<TraceComponent>(traceComponent));
                retransformClass(traceClass);
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

    /**
     * {@inheritDoc}
     *
     * This method will perform a transformation only if:
     * 
     * <ul>
     * <li>The trace state has changed after the class was defined.  Trace
     * injection will be triggered by {@link #traceStateChanged(TraceComponent)}.</li>
     * <li>Class redefinitions are not allowed by the JVM.</li>
     * </ul>
     */
    @Override
    public byte[] transform(ClassLoader loader,
                            String className,
                            Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain,
                            byte[] initialBytes) throws IllegalClassFormatException {

        String methodName = "transform";

        boolean isLoggable = isLoggableClassName(className);

        String failureReason = isTransformPossible(initialBytes);

        if ( failureReason != null ) {
            if ( isLoggable ) {
                fileLog(methodName, "Ignore [ " + className + " ]: " + failureReason);
            }
            return null;
        }

        boolean traceEnabledForClass = injectAtTransform;
        if (!injectAtTransform && classBeingRedefined != null) {
            WeakReference<TraceComponent> tcReference = traceComponentByClass.get(classBeingRedefined);
            TraceComponent traceComponent = tcReference == null ? null : tcReference.get();
            traceEnabledForClass |= (traceComponent != null && traceComponent.isEntryEnabled());
        }

        if ( !traceEnabledForClass ) {
            if ( isLoggable ) {         
                fileLog(methodName, "Ignore: Trace not enabled for class", className);
            }
            return null;
        }

        if ( isLoggable ) {                 
            fileLog(methodName, "Class", className);            
            fileDump(methodName, "Initial bytes", initialBytes);
        }

        byte[] finalBytes;
        try {
            finalBytes = transform(className, initialBytes);
        } catch (Throwable t) {
            fileStack(methodName, "Transform failure [ " + className + " ]", t); 
            Tr.error(tc, "INSTRUMENTATION_TRANSFORM_FAILED_FOR_CLASS_2", className, t);
            return null;
        }

        if ( isLoggable && (finalBytes != null) ) {
            fileDump(methodName, "Final bytes", finalBytes);
        }
        return finalBytes;        
    }

    //

    public static byte[] transform(String className, byte[] bytes) throws IOException {
        return transform(className, bytes, true);
    }    
    
    public static byte[] transform(byte[] bytes, boolean skipIfNotPreprocessed) throws IOException {
        return transform(null, bytes, skipIfNotPreprocessed);
    }

    /*
     * Perform transformation of class bytes.
     *
     * Occasionally, frames must be computed.  This is not known until
     * the class is visited, as the circumstances depend on encountering
     * variable manipulating byte-codes before invoking the superclass
     * initializer.
     *
     * The (hopefully rare) occasions are handled by attempting the transformation
     * frame computation disabled.  A {@link ComputeRequiredException} is thrown
     * if frame computation is required, which is caught and the transformation is
     * attempted a second time with frame computation enabled.
     * 
     * @param className Optional: The name of the target class.
     * @param classBytes The class bytes which are to be transformed.
     * @param skipIfNotPreprocessed Control parameter.  When true, the
     *     transformation will be skipped if the target class was not
     *     preprocessed.
     *     
     * @return The transformed class bytes.  Null if no changes were made.
     */
    public static byte[] transform(String className, byte[] classBytes, boolean skipIfNotPreprocessed) throws IOException {
        String methodName = "transform";

        String debugClassName = ((className != null) ? className : "**UNKNOWN**" );

        boolean isLoggable = (className != null) && isLoggableClassName(className);
        boolean isDumpable = tc.isDumpEnabled();

        // The reader is created early and is provided to the class writer
        // as a write optimization which helps when the writer is optimized
        // for 'mostly add' transformations.  See the ASM ClassWriter JavaDoc.

        // A class writer is at the bottom of the stack, as it receives all
        // visit perturbations from the other visitors.  The writer must be
        // remembered, as it provides the final, transformed class bytes.

        // The check and trace visitors are next to last: They validate and display
        // the perturbed class just before it reaches the writer.

        // Just one trace injection class adapter is used.
        // Contrast this with static trace instrumentation, which
        // may have a distinct adapter for FFDC injection.

        // The class writer must be retained, since it has the updated class bytes.

        ClassReader classReader = new ClassReader(classBytes);
        ClassWriter classWriter = new ClassWriter(classReader, ClassWriter.COMPUTE_MAXS);
        
        PrintWriter traceWriter = ( isLoggable ? fileWriter() : null );
        StringWriter baseDumpWriter = ( isDumpable ? new StringWriter() : null );
        PrintWriter dumpWriter = ( isDumpable ? new PrintWriter(baseDumpWriter) : null );
        ClassVisitor classVisitor = addLogging(classWriter, traceWriter, dumpWriter);

        boolean isModified;

        try {
            try {
                // First try: Use COMPUTE_MAXS and THROW_COMPUTE_FRAMES.

                isModified = visit(classReader, classVisitor,
                                   AbstractRasClassAdapter.THROW_COMPUTE_FRAMES,
                                   skipIfNotPreprocessed);
                
            } catch ( ComputeRequiredException e ) {
                // Second try: Use COMPUTE_FRAMES and !THROW_COMPUTE_FRAMES.

                classReader = new ClassReader(classBytes);
                classWriter = new ClassWriter(classReader, ClassWriter.COMPUTE_FRAMES);

                traceWriter = ( isLoggable ? fileWriter() : null );
                baseDumpWriter = ( isDumpable ? new StringWriter() : null );
                dumpWriter = ( isDumpable ? new PrintWriter(baseDumpWriter) : null );
                classVisitor = addLogging(classWriter, traceWriter, dumpWriter);
                
                isModified = visit(classReader, classVisitor,
                                   !AbstractRasClassAdapter.THROW_COMPUTE_FRAMES,
                                   skipIfNotPreprocessed);             
            }

        } catch ( Throwable t ) {
            fileStack(methodName, "Trace instrumentation failure [ " + debugClassName + " ]", t);
            throw new IOException("Trace instrumentation failure [ " + debugClassName + " ]: " + t.getMessage(), t);
        }

        if ( isDumpable && isModified ) {
            Tr.dump(tc, "Transformed class [ " + debugClassName + " ]", baseDumpWriter);
        }
        if ( isLoggable ) {
            fileLog(methodName, "IsModified [ " + debugClassName + " ]", Boolean.valueOf(isModified));
        }
        return ( !isModified ? null : classWriter.toByteArray() );
    }

    /**
     * Conditionally wrap a class writer with logging visitors.
     * 
     * Answer the class writer if neither logging nor dumping is
     * enabled.
     * 
     * If either logging or dumping is enabled, add a "check" class
     * visitor -- which verifies the class bytes.
     * 
     * Independently, add either a trace visitor bound to a file writer,
     * a trace visitor bound to the string writer, or add both, depending
     * on whether logging is enabled and on whether dumping is enabled.
     * 
     * @param classWriter The base class writer.
     * @param traceWriter Control parameter: Whether to add a trace visitor
     *     mapped to the writer.
     * @param dumpWriter Control parameter: When non-null, add a trace
     *     visitor mapped to the writer.
     * 
     * @return The updated class visitor.
     */
    private static ClassVisitor addLogging(ClassWriter classWriter,
                                           PrintWriter traceWriter,
                                           PrintWriter dumpWriter) {

        ClassVisitor classVisitor = classWriter;
    
        if ( (traceWriter != null) || (dumpWriter != null) ) {
            classVisitor = new CheckClassAdapter(classVisitor, false);
        }
        if ( traceWriter != null ) {
            classVisitor = new TraceClassVisitor(classVisitor, traceWriter);
        }
        if ( dumpWriter != null ) {
            classVisitor = new TraceClassVisitor(classVisitor, dumpWriter);
        }
        
        return classVisitor;
    }
    
    /**
     * Final step of performing trace injection.
     * 
     * Wrap the reader and visitor with a trace injection visitor. 
     *
     * Contrast this with static trace injection, which might add
     * a second visitor which does FFDC injection.
     *
     * @param classReader The reader supplying class bytes.
     * @param classVisitor Additional class visitors, for example, for verifying the
     *     updated class bytes, or for displaying the updating class bytes.
     * @param throwComputeFrames Control parameter: Determines whether the
     *     tracing class visitor should throw {@link ComputeRequiredException}
     *     if the target class requires frame computation. 
     * @param skipIfNotPreprocessed Control parameter.  When true, the
     *     transformation will be skipped if the target class was not
     *     preprocessed.
     *     
     * @return True or false telling if the transformation modified the class bytes.
     * 
     * @throws ComputeRequiredException Thrown if the class visitor is not enabled
     *     for frame computation, if the target class requires frame computation,
     *     and throwing of the exception is enabled by the control parameter.
     *     The exception is a runtime exception and does not strictly need to be
     *     declared to be thrown.
     */
    protected static boolean visit(
            ClassReader classReader,
            ClassVisitor classVisitor,
            boolean throwComputeFrames,
            boolean skipIfNotPreprocessed) throws ComputeRequiredException {

        LibertyTracingClassAdapter traceVisitor =
            new LibertyTracingClassAdapter(classVisitor, throwComputeFrames, skipIfNotPreprocessed);

        classReader.accept(traceVisitor, skipDebugData ? ClassReader.SKIP_DEBUG : 0);

        return traceVisitor.isClassModified();
    }       
}
