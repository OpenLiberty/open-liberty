/*******************************************************************************
 * Copyright (c) 2006, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

    protected boolean introspectAnnotations = true;
    protected boolean instrumentWithFFDC = false;
    protected boolean computeFrames = false;
    protected TraceType traceType = TraceType.TR;

    protected TraceConfigFileParser configFileParser = new TraceConfigFileParser();
    protected InstrumentationOptions instrumentationOptions = new InstrumentationOptions();

    /**
     * Public default constructor to allow for programmatic use of the class.
     */
    public StaticTraceInstrumentation() {}

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
     *             the <code>InputStream</code>
     */
    final protected byte[] transform(final InputStream classfileStream) throws IOException {
        ClassConfigData classConfigData = processClassConfiguration(classfileStream);
        ClassInfo classInfo = classConfigData.getClassInfo();

        // Only instrument the classes that we're supposed to.
        if (!getInstrumentationOptions().isPackageIncluded(classInfo.getPackageName())) {
            return null;
        }

        // Merge command line, config file, package annotations, and class annotations
        classInfo = mergeClassConfigInfo(classInfo);

        int classWriterOptions = isComputeFrames() ? ClassWriter.COMPUTE_FRAMES : ClassWriter.COMPUTE_MAXS;

        InputStream classInputStream = classConfigData.getClassInputStream();
        ClassReader reader = new ClassReader(classInputStream);
        ClassWriter writer = new ClassWriter(reader, classWriterOptions);

        ClassVisitor visitor = writer;
        if (isDebug()) {
            visitor = new CheckClassAdapter(visitor);
            visitor = new TraceClassVisitor(visitor, new PrintWriter(System.out));
        }

        // Get the correct tracing adapter
        switch (getTraceType()) {
            case JAVA_LOGGING:
                visitor = new JSR47TracingClassAdapter(visitor, classInfo);
                break;
            case TR:
                visitor = new WebSphereTrTracingClassAdapter(visitor, classInfo);
                break;
            case LIBERTY:
            	break;
            case NONE:
            	break;
        }

        if (getInstrumentWithFFDC()) {
            visitor = new FFDCClassAdapter(visitor, classInfo);
        }

        // The SerialVersionUIDAdder adder must be the first visitor in
        // the chain in order to calculate the serialVersionUID before
        // the tracing class adapter mucks around and (possibly) adds a
        // class static initializer.
        visitor = new SerialVersionUIDAdder(visitor);
        try {
            // Keep all metadata information that's present in the class file
            reader.accept(visitor, isComputeFrames() ? ClassReader.EXPAND_FRAMES : 0);
        } catch (Throwable t) {
            IOException ioe = new IOException("Unable to instrument class stream with trace");
            ioe.initCause(t);
            throw ioe;
        }

        return writer.toByteArray();
    }

    /**
     * Holder class to encapsulate the results of introspection.
     */
    private final static class ClassConfigData {
        InputStream classInputStream;
        ClassInfo classInfo;

        ClassConfigData(InputStream classInputStream) {
            this.classInputStream = classInputStream;
        }

        ClassConfigData(InputStream classInputStream, ClassInfo classInfo) {
            this.classInputStream = classInputStream;
            this.classInfo = classInfo;
        }

        ClassInfo getClassInfo() {
            return classInfo;
        }

        InputStream getClassInputStream() {
            return classInputStream;
        }
    }

    /**
     * Introspect configuration information from the class in the provided
     * InputStream.
     */
    protected ClassConfigData processClassConfiguration(final InputStream inputStream) throws IOException {
        if (introspectAnnotations == false) {
            return new ClassConfigData(inputStream);
        }

        ClassReader cr = new ClassReader(inputStream);
        ClassWriter cw = new ClassWriter(cr, 0); // Don't compute anything - read only mode
        TraceConfigClassVisitor cv = new TraceConfigClassVisitor(cw);
        cr.accept(cv, 0);
        ClassInfo classInfo = cv.getClassInfo();
        InputStream classInputStream = new ByteArrayInputStream(cw.toByteArray());

        return new ClassConfigData(classInputStream, classInfo);
    }

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

    /**
     * Main entry point for command line execution.
     */
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
