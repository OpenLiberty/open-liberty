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
import java.io.InputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.ibm.ws.ras.instrument.internal.model.PackageInfo;
import com.ibm.ws.ras.instrument.internal.model.TraceType;

/**
 * Exploitation of the Java instrumentation framework in JDK5 that
 * will register a <code>ClassFileTransformer</code> to inject
 * byte codes for entry/exit tracing as they're defined to the JVM.
 */
public class DynamicTraceInstrumentation {
    public static void premain(String arg, Instrumentation inst) {

        Properties props = new Properties();

        // Perform simple parsing of the argument list.
        // Arguments are key=value parameters separated by semi-colons.
        String[] args = arg != null ? arg.split(";") : new String[0];
        for (int i = 0; i < args.length; i++) {
            String[] keyValue = args[i].split("=");

            if (keyValue.length == 1) {
                props.setProperty(keyValue[0], "");
            } else if (keyValue.length == 2) {
                props.setProperty(keyValue[0], keyValue[1]);
            }
        }

        // Plug-in to the instrumentation framework.
        inst.addTransformer(new Transformer(props, false));
    }
}

/**
 * Implementation of a <code>ClassFileTransformer</code> that will
 * add entry/exit tracing to the classes in packages that are in the
 * includes list and are not in the excludes list.
 */
class Transformer extends StaticTraceInstrumentation implements ClassFileTransformer {

    List<String> includesList = new ArrayList<String>();
    List<String> excludesList = new ArrayList<String>();

    /**
     * Constructor that processes the arguments to the agent. Arguments to the
     * agent are in key=value form and are separated by semi-colons.
     * <ul>
     * <li><code>packages.include</code> is a comma separated list of packages
     * that should be instrumented. All subpackages of the provided list
     * will be instrumented unless they are in the excludes list.
     * <li><code>packages.exclude</code> is a comma separated list of packages
     * or classes that should not be instrumented.
     * <li><code>debug</code> when specified and set to true, debug will dump
     * debug information about the classes resulting from instrumentation.
     * <li><code>ffdc</code> when set to false, will omit the addition of calls
     * to FFDC in exception blocks.
     * <li><code>retransform</code> when set to true, will retransform classes
     * that have already been defined to the JVM. This should only be
     * required when instrumenting classes loaded during bootstrap.
     * <li><code>style</code> indicates what style of trace to add to the
     * instrumented classes. Valid options are:
     * <ul>
     * <li><code>tr</code> - WebSphere logging and tracing
     * <li><code>jsr47</code> - java.util.logging based tracing
     * <li><code>none</code> - No entry/exit tracing
     * </ul>
     * <li><code>computeFrames</code> when set to true, ASM will discard and
     * recalculate stack map frames.
     * </ul>
     */
    Transformer(Properties props, boolean quiet) {
        super();
        if (!props.containsKey("packages.include") && !props.containsKey("packages.exclude")) {
            if (!quiet) {
                System.err.println("Dynamic trace transformation is enabled without args");
            }
            includesList.add("/");
            excludesList.add("java/");
        } else {

            String packagesValue = props.getProperty("packages.include");
            if (packagesValue != null) {
                String[] packages = packagesValue.split(",");
                for (int i = 0; i < packages.length; i++) {
                    includesList.add(packages[i].replaceAll("\\.", "/"));
                }
            }

            packagesValue = props.getProperty("packages.exclude");
            if (packagesValue != null) {
                String[] packages = packagesValue.split(",");
                for (int i = 0; i < packages.length; i++) {
                    excludesList.add(packages[i].replaceAll("\\.", "/"));
                }
            }

            // Enable verbose debugging of the instrumentation
            String debug = props.getProperty("debug", "false");
            setDebug(Boolean.valueOf(debug));

            // Indicate wither or not to inject FFDC
            String instrumentWithFFDC = props.getProperty("ffdc", "true");
            setInstrumentWithFFDC(Boolean.valueOf(instrumentWithFFDC));

            // Determine the trace style to use
            String traceStyle = props.getProperty("style", "jsr47");
            if (traceStyle.equalsIgnoreCase("jsr47")) {
                setTraceType(TraceType.JAVA_LOGGING);
            } else if (traceStyle.equalsIgnoreCase("tr")) {
                setTraceType(TraceType.TR);
            } else if (traceStyle.equalsIgnoreCase("none")) {
                setTraceType(TraceType.NONE);
            }

            // Indicate whether or not ASM should recompute the frames
            String computeFrames = props.getProperty("computeFrames", "false");
            setComputeFrames(Boolean.valueOf(computeFrames));

            if (!quiet) {
                System.err.println("Dynamic trace transformation is enabled");
                System.err.println("  includesList = " + includesList);
                System.err.println("  excludesList = " + excludesList);
                System.err.println("  debug = " + debug);
                System.err.println("  ffdc = " + instrumentWithFFDC);
                System.err.println("  style = " + traceStyle);
                System.err.println("  computeFrames = " + computeFrames);
            }
        }
    }

    /**
     * Instrument the classes.
     */
    public byte[] transform(ClassLoader loader,
                            String className,
                            Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain,
                            byte[] classfileBuffer) throws IllegalClassFormatException {

        // Don't modify our own package
        if (className.startsWith(Transformer.class.getPackage().getName().replaceAll("\\.", "/"))) {
            return null;
        }

        // Don't modify the java.util.logging classes
        if (className.startsWith("java/util/logging/")) {
            return null;
        }

        boolean include = false;
        for (String s : includesList) {
            if (className.startsWith(s) || s.equals("/")) {
                include = true;
                break;
            }
        }

        for (String s : excludesList) {
            if (className.startsWith(s) || s.equals("/")) {
                include = false;
                break;
            }
        }

        if (include == false) {
            return null;
        }

        String internalPackageName = className.replaceAll("/[^/]+$", "");
        PackageInfo packageInfo = getPackageInfo(internalPackageName);
        if (packageInfo == null && loader != null) {
            String packageInfoResourceName = internalPackageName + "/package-info.class";
            InputStream is = loader.getResourceAsStream(packageInfoResourceName);
            packageInfo = processPackageInfo(is);
            addPackageInfo(packageInfo);
        }

        try {
            return transform(new ByteArrayInputStream(classfileBuffer));
        } catch (Throwable t) {
            t.printStackTrace();
            return null;
        }
    }
}
