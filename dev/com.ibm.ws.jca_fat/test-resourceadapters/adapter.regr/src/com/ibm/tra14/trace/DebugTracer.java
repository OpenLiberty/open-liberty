/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

/*
 * Debug Tracer static class for debugging the test resource adapter.
 * Provides functions for turning certain debug messages on/off, and also
 * to set the default output stream to where the debug messages will be printed.
 */

package com.ibm.tra14.trace;

import java.io.PrintStream;

public class DebugTracer {

    private static boolean _classLoader = false;
    private static boolean _stackDump = false;
    private static boolean _debugMessages = false;
    private static boolean _debugActivationSpec = false;

    private static PrintStream _outStream = System.out;

    public DebugTracer() {
        System.out.println("Error: Instantiating a completely static class");
    }

    public static void setPrintClassLoader(boolean val) {
        _classLoader = val;
    }

    public static void setDumpStack(boolean val) {
        _stackDump = val;
    }

    public static void setDebugMessages(boolean val) {
        _debugMessages = val;
    }

    public static void setDebugActivationSpec(boolean val) {
        _debugActivationSpec = val;
    }

    public static void setPrintStream(PrintStream out) {
        _outStream = out;
    }

    public static boolean isPrintClassLoader() {
        return _classLoader;
    }

    public static boolean isDumpStack() {
        return _stackDump;
    }

    public static boolean isDebugMessages() {
        return _debugMessages;
    }

    public static boolean isDebugActivationSpec() {
        return _debugActivationSpec;
    }

    public static PrintStream getPrintStream() {
        return _outStream;
    }

    public static void printClassLoaderInfo(PrintStream dest, String className, Object cl) {
        if (_classLoader) {
            dest.println("*** Printing Classloader info for class: " + className);
            dest.println("*** Current Classloader: " + cl.getClass().getClassLoader().toString());
            dest.println("*** Context Classloader: " + Thread.currentThread().getContextClassLoader().toString());
        }
    }

    // This overloaded function will simply call the previous version with System.out as the printstream
    public static void printClassLoaderInfo(String className, Object cl) {
        printClassLoaderInfo(_outStream, className, cl);
    }

    /*
     * This function will print the stack dump (with a preceding class name identifier) to the given printstream
     * but only if the stack dump static member variable is set to true
     */
    public static void printStackDump(PrintStream dest, String className, Exception e) {
        if (_stackDump) {
            dest.println("*** Printing Stack Dump for Class: " + className);
            e.printStackTrace(dest);
        }
    }

    public static void printStackDump(String className, Exception e) {
        printStackDump(_outStream, className, e);
    }

    public static void printStackDump(String className) {
        printStackDump(_outStream, className, new Exception());
    }
}