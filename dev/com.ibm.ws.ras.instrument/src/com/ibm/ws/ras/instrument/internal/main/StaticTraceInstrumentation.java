/*******************************************************************************
 * Copyright (c) 2006, 2023 IBM Corporation and others.
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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.SerialVersionUIDAdder;
import org.objectweb.asm.util.CheckClassAdapter;
import org.objectweb.asm.util.TraceClassVisitor;

import com.ibm.ws.ras.instrument.internal.bci.FFDCClassAdapter;
import com.ibm.ws.ras.instrument.internal.bci.JSR47TracingClassAdapter;
import com.ibm.ws.ras.instrument.internal.bci.WebSphereTrTracingClassAdapter;
import com.ibm.ws.ras.instrument.internal.introspect.TraceConfigClassVisitor;
import com.ibm.ws.ras.instrument.internal.model.ClassInfo;
import com.ibm.ws.ras.instrument.internal.model.InstrumentationOptions;
import com.ibm.ws.ras.instrument.internal.model.PackageInfo;
import com.ibm.ws.ras.instrument.internal.model.TraceType;
import com.ibm.ws.ras.instrument.internal.xml.TraceConfigFileParser;

/**
 * Executable &quot;main&quot; class that can be used to instrument
 * class files and class files in jars with trace.
 */
public class StaticTraceInstrumentation extends AbstractInstrumentation {
    public static final String CLASS_NAME = "StaticInstrumentation";

    public static boolean isLoggable(String className) {
        return FileLogger.isLoggable(className);
    }

    public PrintWriter fileWriter() {
        return FileLogger.fileWriter();
    }

    public byte[] read(InputStream inputStream) throws IOException {
        return FileLogger.read(inputStream);
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

    //

    protected boolean introspectAnnotations = true;
    protected boolean instrumentWithFFDC = false;
    protected boolean computeFrames = false;
    protected TraceType traceType = TraceType.TR;

    protected TraceConfigFileParser configFileParser = new TraceConfigFileParser();
    protected InstrumentationOptions instrumentationOptions = new InstrumentationOptions();

    /**
     * Public default constructor to allow for programmatic use of the class.
     */
    public StaticTraceInstrumentation() {
        // EMPTY
    }

    /**
     * Enable or disable the generation of calls to FFDC.
     */
    public void setInstrumentWithFFDC(boolean instrumentWithFFDC) {
        this.instrumentWithFFDC = instrumentWithFFDC;
    }

    /**
     * Determine if FFDC should be added to the instrumented classes.
     */
    public boolean getInstrumentWithFFDC() {
        return this.instrumentWithFFDC;
    }

    /**
     * Set the type of tracing that classes should be instrumented with.
     */
    public void setTraceType(TraceType traceType) {
        this.traceType = traceType;
    }

    /**
     * Get the type of tracing that classes should be instrumented with.
     */
    public TraceType getTraceType() {
        return this.traceType;
    }

    /**
     * Indicate whether or not ASM should recompute stack map frames.
     */
    public void setComputeFrames(boolean computeFrames) {
        this.computeFrames = computeFrames;
    }

    /**
     * Determine if whether or not ASM should recompute stack map frames.
     */
    public boolean isComputeFrames() {
        return this.computeFrames;
    }

    /**
     * Get the instrumentation options.
     */
    public InstrumentationOptions getInstrumentationOptions() {
        return instrumentationOptions;
    }

    /**
     * Set the instrumentation options.
     */
    public void setInstrumentationOptions(InstrumentationOptions instrumentationOptions) {
        this.instrumentationOptions = instrumentationOptions;
    }

    /**
     * Instrument the class at the current position in the specified input stream.
     * 
     * @return instrumented class file or null if the class has already
     *         been instrumented.
     * 
     * @throws IOException if an error is encountered while reading from
     *         the <code>InputStream</code>
     */
    @Override
    final protected byte[] transform(String className, InputStream classStream) throws IOException {
        String methodName = "transform";
        boolean isLoggable = isLoggable(className);
        
        if ( isLoggable ) {                         
            fileLog(methodName, "Class", className);
        }

        // To keep the processing simple, read in the entire class stream
        // initially, then re-use it for the read of the class data and
        // for the actual transformation.
        //
        // The class bytes should be 'reasonably' small.  Also, doing this
        // read early puts the read overhead all in one spot.

        byte[] initialBytes;
        try {
            initialBytes = read(classStream);
        } catch ( IOException e ) {
            fileStack(methodName, "Read failure [ " + className + " ]", e);
            return null;
        }

        ClassInfo classInfo = readConfig(className, initialBytes);

        // Merging the class information does not change whether the class is enabled
        // for instrumentation.

        if ( !getInstrumentationOptions().isPackageIncluded( classInfo.getPackageName() ) ) {
            if ( isLoggable ) {
                fileLog(methodName, "Ignore: Package not included", classInfo.getClassName());
            }
            return null;
        }

        classInfo = mergeClassConfigInfo(classInfo);

        if ( isLoggable ) {
            fileDump(methodName, "Initial bytes", initialBytes);
        }

        // The reader is created early and is provided to the class writer
        // as a write optimization which helps when the writer is optimized
        // for 'mostly add' transformations.  See the ASM ClassWriter JavaDoc.

        ClassReader classReader = new ClassReader(initialBytes);
        ClassWriter classWriter = createWriter(classReader);
        ClassVisitor classVisitor = createVisitor( classInfo, classWriter, isDebug() || isLoggable );

        int readOptions = ( isComputeFrames() ? ClassReader.EXPAND_FRAMES : 0 );

        try {
            classReader.accept(classVisitor, readOptions);

        } catch ( Throwable t ) {
            fileStack(methodName, "Instrumentation failure [ " + className + " ]", t);

            if ( !isDebug() && !isLoggable ) {
                classReader = new ClassReader(initialBytes);
                classWriter = createWriter(classReader);
                classVisitor = createVisitor(classInfo, classWriter, true);

                try {
                    classReader.accept(classVisitor, readOptions);
                } catch ( Throwable innerThrowable ) {
                    // Ignore
                }
            }

            throw new IOException("Unable to instrument class stream with trace", t);
        }

        byte[] finalBytes = classWriter.toByteArray();
        if ( isLoggable ) {
            fileDump(methodName, "Final bytes", initialBytes);
        }
        return finalBytes;
    }

    private ClassInfo readConfig(String className, byte[] classBytes) throws IOException {
        String methodName = "readConfig";

        ClassReader classReader = new ClassReader(classBytes);
        ClassWriter classWriter = new ClassWriter(classReader, 0);

        // Sets the ASM version opcode.  See the initializer.
        TraceConfigClassVisitor classVisitor = new TraceConfigClassVisitor(classWriter);

        classReader.accept(classVisitor, 0);

        byte[] updatedClassBytes = classWriter.toByteArray();
        if ( !Arrays.equals(classBytes, updatedClassBytes) ) {
            fileLog(methodName, "Class [ " + className + " ] Change detected:");
            fileDump(methodName, "Old bytes:", classBytes);
            fileDump(methodName, "New bytes:", updatedClassBytes);
        } else {
            fileLog(methodName, "Class [ " + className + " ] Unchanged");
        }

        return classVisitor.getClassInfo();
    }

    // Updated step of reading class information.
    // Note the step of rewriting the class bytes has been
    // removed.  The trace configuration visitor is a read-only
    // visitor.  There is no need to rewrite the class bytes.

    private ClassInfo readConfig_new(byte[] classBytes) throws IOException {
        // TFB: Returning null would cause an NPE when the
        //      caller attempted to obtain the package name.
        //
        // if ( !introspectAnnotations ) {
        //     return null;
        // }

        // Sets the ASM version opcode.  See the initializer.
        TraceConfigClassVisitor classVisitor = new TraceConfigClassVisitor();

        ClassReader classReader = new ClassReader(classBytes);
        classReader.accept(classVisitor, 0);

        return classVisitor.getClassInfo();
    }

    // Old step of reading class information.  Note the replacement of the
    // class bytes with the class writer bytes.
    //
    // Per ClassWriter JavaDoc, the replaced bytes should be exactly the same,
    // since no changes were made.
    //
    // See: https://asm.ow2.io/javadoc/org/objectweb/asm/ClassWriter.html#%3Cinit%3E(org.objectweb.asm.ClassReader,int), 
    //
    // Constructs a new ClassWriter object and enables optimizations
    // for "mostly add" bytecode transformations. These optimizations
    // are the following:
    //
    // The constant pool and bootstrap methods from the original class
    // are copied as is in the new class, which saves time. New
    // constant pool entries and new bootstrap methods will be added
    // at the end if necessary, but unused constant pool entries or
    // bootstrap methods won't be removed.
    //
    // Methods that are not transformed are copied as is in the new
    // class, directly from the original class bytecode (i.e. without
    // emitting visit events for all the method instructions), which
    // saves a lot of time. Untransformed methods are detected by the
    // fact that the ClassReader receives MethodVisitor objects that
    // come from a ClassWriter (and not from any other ClassVisitor
    // instance).
    //
    // TFB: I can't tell if the use of the trace config class visitor
    //      breaks the "untransformed methods" detection strategy.
    //
    //    private ClassInfo readConfig_old(byte[] classBytes) throws IOException {
    //        ClassReader classReader = new ClassReader(inputStream);
    //        ClassWriter classWriter = new ClassWriter(classReader, 0);
    //
    //        // Sets the ASM version opcode.  See the initializer.
    //        TraceConfigClassVisitor classVisitor = new TraceConfigClassVisitor(classWriter);
    //
    //        classReader.accept(classVisitor, 0);
    //
    //        ClassInfo classInfo = classVisitor.getClassInfo();
    //
    //        byte[] updatedClassBytes = classWriter.toByteArray();
    //        InputStream classInputStream = new ByteArrayInputStream(updatedClassBytes);
    //
    //        return new ClassConfigData(classInputStream, classInfo);
    //    }

    /**
     * Find process the package annotations present in the jars
     * jar and class file lists.
     */
    public void processPackageInfo() throws IOException {
        if (introspectAnnotations == false) {
            return;
        }

        super.processPackageInfo();
    }

    /**
     * Attempt to normalize the various levels of configuration information
     * prior to instrumenting a class.
     */
    protected ClassInfo mergeClassConfigInfo(ClassInfo classInfo) {
        // Update introspected class information from introspected package
        PackageInfo packageInfo = configFileParser.getPackageInfo(classInfo.getInternalPackageName());
        if (packageInfo == null) {
            packageInfo = getPackageInfo(classInfo.getInternalPackageName());
        }
        classInfo.updateDefaultValuesFromPackageInfo(packageInfo);

        // Override introspected class information from configuration document
        ClassInfo ci = configFileParser.getClassInfo(classInfo.getInternalClassName());
        if (ci != null) {
            classInfo.overrideValuesFromExplicitClassInfo(ci);
        }

        return classInfo;
    }

    private ClassWriter createWriter(ClassReader classReader) {
        int writeOptions = isComputeFrames() ? ClassWriter.COMPUTE_FRAMES : ClassWriter.COMPUTE_MAXS;
        return new ClassWriter(classReader, writeOptions);
    }

    private ClassVisitor createVisitor(ClassInfo classInfo, ClassWriter classWriter, boolean isLoggable) {
        // A class writer is at the bottom of the stack, as it receives all
        // visit perturbations from the other visitors.  The writer must be
        // remembered, as it provides the final, transformed class bytes.

        ClassVisitor visitor = classWriter;

        // The check and trace visitors are next to last: They validate and display
        // the perturbed class just before it reaches the writer.

        if ( isLoggable ) {
            visitor = new CheckClassAdapter(visitor);

            PrintWriter traceWriter = ( isLoggable ? fileWriter() : null );
            if ( traceWriter == null ) {
                traceWriter = new PrintWriter(System.out);
            }
            visitor = new TraceClassVisitor(visitor, traceWriter);
        }

        // Two trace injection adapters may be used: One for usual enter/exit
        // trace injection and one to inject FFDC handling.
        //
        // There is (mostly) no point to running trace injection if neither
        // adapter is in use.  That is probably never the case.

        TraceType useTraceType = getTraceType();
        if ( useTraceType == TraceType.JAVA_LOGGING ) {
            visitor = new JSR47TracingClassAdapter(visitor, classInfo);
        } else if ( useTraceType == TraceType.TR ) {
            visitor = new WebSphereTrTracingClassAdapter(visitor, classInfo);
        } else {
            // Don't inject entry/exit trace
        }
        if ( getInstrumentWithFFDC() ) {
            visitor = new FFDCClassAdapter(visitor, classInfo);
        }

        // The SerialVersionUIDAdder adder must be the first visitor in
        // the chain in order to calculate the serialVersionUID before
        // the tracing class adapter mucks around and (possibly) adds a
        // class static initializer.

        visitor = new SerialVersionUIDAdder(visitor);

        return visitor;
    }

    /**
     * Setup the lists of classes and jars to process based on the command
     * line arguments provided.
     */
    public void processArguments(String[] args) throws IOException {
        List<File> classFiles = new ArrayList<File>();
        List<File> jarFiles = new ArrayList<File>();
        String[] fileArgs = null;

        for (int i = 0; i < args.length; i++) {
            if (args[i].equalsIgnoreCase("--config")) {
                configFileParser = new TraceConfigFileParser(new File(args[++i]));
                configFileParser.parse();
                InstrumentationOptions options = configFileParser.getInstrumentationOptions();
                if (options.getAddFFDC()) {
                    setInstrumentWithFFDC(true);
                }
                setTraceType(options.getTraceType());
                setInstrumentationOptions(options);
            } else if (args[i].equalsIgnoreCase("--debug") || args[i].equals("-d")) {
                setDebug(true);
            } else if (args[i].equalsIgnoreCase("--tr")) {
                setTraceType(TraceType.TR);
            } else if (args[i].equalsIgnoreCase("--websphere")) {
                setTraceType(TraceType.TR);
            } else if (args[i].equalsIgnoreCase("--java-logging")) {
                setTraceType(TraceType.JAVA_LOGGING);
            } else if (args[i].equalsIgnoreCase("--jsr47")) {
                setTraceType(TraceType.JAVA_LOGGING);
            } else if (args[i].equalsIgnoreCase("--none")) {
                setTraceType(TraceType.NONE);
            } else if (args[i].equalsIgnoreCase("--no-trace")) {
                setTraceType(TraceType.NONE);
            } else if (args[i].equalsIgnoreCase("--ffdc")) {
                setInstrumentWithFFDC(true);
            } else if (args[i].equalsIgnoreCase("--compute-frames")) {
                setComputeFrames(true);
            } else {
                fileArgs = new String[args.length - i];
                System.arraycopy(args, i, fileArgs, 0, fileArgs.length);
                break;
            }
        }

        if (fileArgs == null || fileArgs.length == 0) {
            throw new IllegalArgumentException("Empty file lists are illegal");
        }

        for (int i = 0; i < fileArgs.length; i++) {
            File f = new File(fileArgs[i]);
            if (!f.exists()) {
                throw new IllegalArgumentException(f + " does not exist");
            } else if (f.isDirectory()) {
                classFiles.addAll(getClassFiles(f, null));
            } else if (f.getName().endsWith(".class")) {
                classFiles.add(f);
            } else if (f.getName().endsWith(".jar")) {
                jarFiles.add(f);
            } else if (f.getName().endsWith(".zip")) {
                jarFiles.add(f);
            } else {
                System.err.println(f + " is an unexpected file type; ignoring");
            }
        }

        setClassFiles(classFiles);
        setJarFiles(jarFiles);
    }

    /**
     * Gentle usage message that needs to be written.
     */
    private static void printUsageMessage() {
        System.out.println("Description:");
        System.out.println("  StaticTraceInstrumentation can modify classes");
        System.out.println("  in place to add calls to a trace framework that will");
        System.out.println("  delegate to JSR47 logging or WebSphere Tr.");
        System.out.println("");
        System.out.println("Required arguments:");
        System.out.println("  The paths to one or more binary classes, jars, or");
        System.out.println("  directories to scan for classes and jars are required");
        System.out.println("  parameters.");
        System.out.println("");
        System.out.println("  Class files must have a .class extension.");
        System.out.println("  Jar files must have a .jar or a .zip extension.");
        System.out.println("  Directories are recursively scanned for .jar, .zip, and");
        System.out.println("  .class files to instrument.");
    }

    public final static void main(String[] args) throws Exception {
        if (args == null || args.length <= 0) {
            printUsageMessage();
            return;
        }

        StaticTraceInstrumentation sti = new StaticTraceInstrumentation();

        sti.processArguments(args);
        sti.processPackageInfo();
        sti.executeInstrumentation();
    }
}
