/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ffdc;

import java.util.HashMap;
import java.util.Map;

/* ************************************************************************** */
/**
 * The FFDC class provides methods to control the FFDC support. Unlike the
 * server version of FFDC, most of the methods provided have been stubbed out
 * (i.e. do nothing), except for:
 * <ul>
 * <li>registerDiagnosticModule
 * <li>deregisterDiagnosticModule
 * </ul>
 */
/* ************************************************************************** */
public final class FFDC {

    /**
     * STARTING is the state used in the server to indicate that the server is in STARTING state - provided in
     * any using code requires it
     */
    public static final int STARTING = 1;
    /**
     * RUNNING the state used in the server to indicate that the server is in RUNNING state - provided in
     * any using code requires it
     */
    public static final int RUNNING = 2;
    /**
     * STOPPING is the state used in the server to indicate that the server is in STOPPING state - provided in
     * any using code requires it
     */
    public static final int STOPPING = 3;

    /** A mapping of package names to diagnostic modules */
    private static Map<String, DiagnosticModule> modules = new HashMap<String, DiagnosticModule>();

    /* -------------------------------------------------------------------------- */
    /*
     * setState method
     * /* --------------------------------------------------------------------------
     */
    /**
     * Set the state of the FFDC code to the given state
     * 
     * <p><b>Note:</b> This method is just a stub - calling this version has no effect
     * 
     * @param newState The new state of the server
     */
    public static void setState(int newState) {
    // No code - this method is just a stub
    }

    /* -------------------------------------------------------------------------- */
    /*
     * setServer method
     * /* --------------------------------------------------------------------------
     */
    /**
     * Used to indicate to the FFDC code that it is running in the server
     * 
     * <p><b>Note:</b> This method is just a stub - calling this version has no effect
     * (not even to throw an exception!)
     */
    public static void setServer() {
    // No code - this method is just a stub
    }

    /* -------------------------------------------------------------------------- */
    /*
     * setZos method
     * /* --------------------------------------------------------------------------
     */
    /**
     * Used to indicate to the FFDC code that it is running on the zOS platform
     * 
     * <p><b>Note:</b> This method is just a stub - calling this version has no effect
     * (not even to throw an exception!)
     * 
     * @param isZOSFlag true if the caller wishes to indicate to the FFDC code that it is running on zOS
     */
    public static void setZos(boolean isZOSFlag) {
    // No code - this method is just a stub
    }

    /* -------------------------------------------------------------------------- */
    /*
     * setServerName method
     * /* --------------------------------------------------------------------------
     */
    /**
     * Set the name of the server when running within the server process.
     * 
     * <p><b>Note:</b> This method is just a stub - calling this version has no effect
     * (not even to throw an exception!)
     * 
     * @param inServerName The server name
     * @param inFullServerName The complete server name (including the node name?)
     */
    public static void setServerName(String inServerName, String inFullServerName) {
    // No code - this method is just a stub
    }

    /* -------------------------------------------------------------------------- */
    /*
     * setzOSjobAttributes method
     * /* --------------------------------------------------------------------------
     */
    /**
     * Set the Job attributes of the server process
     * 
     * <p><b>Note:</b> This method is just a stub - calling this version has no effect
     * (not even to throw an exception!)
     * 
     * @param inJobNo The MVS Job Number
     * @param inJobName The MVS Job Name
     */
    public static void setzOSjobAttributes(String inJobNo, String inJobName) {
    // No code - this method is just a stub
    }

    /* -------------------------------------------------------------------------- */
    /*
     * setLogRoot method
     * /* --------------------------------------------------------------------------
     */
    /**
     * Set the logging root directory
     * 
     * <p><b>Note:</b> This method is just a stub - calling this version has no effect
     * (not even to throw an exception!)
     * 
     * @param inLogRoot The name of the logging root directory
     */
    public static void setLogRoot(String inLogRoot) {
    // No code - this method is just a stub
    }

    /* -------------------------------------------------------------------------- */
    /*
     * isZos method
     * /* --------------------------------------------------------------------------
     */
    /**
     * Return if the FFDC code believes it's running in zOS
     * 
     * <p><b>Note:</b> This method is just a stub - it always returns false
     * 
     * @return false
     */
    public static boolean isZos() {
        // No code - this method is just a stub
        return false;
    }

    /* -------------------------------------------------------------------------- */
    /*
     * getzOSjobNumber method
     * /* --------------------------------------------------------------------------
     */
    /**
     * Return the zOS job number that the FFDC code believes it's running under
     * 
     * <p><b>Note:</b> This method is just a stub - it always returns the empty
     * string
     * 
     * @return ""
     */
    public static String getzOSjobNumber() {
        // No code - this method is just a stub
        return "";
    }

    /* -------------------------------------------------------------------------- */
    /*
     * getzOSjobName method
     * /* --------------------------------------------------------------------------
     */
    /**
     * Return the zOS job name that the FFDC code believes it's running under
     * 
     * <p><b>Note:</b> This method is just a stub - it always returns the empty
     * string
     * 
     * @return ""
     */
    public static String getzOSjobName() {
        // No code - this method is just a stub
        return "";
    }

    /* -------------------------------------------------------------------------- */
    /*
     * getFullServerName method
     * /* --------------------------------------------------------------------------
     */
    /**
     * Return the full server name that the FFDC code believes it's running under
     * 
     * <p><b>Note:</b> This method is just a stub - it always returns the empty
     * string
     * 
     * @return ""
     */
    public static String getFullServerName() {
        // No code - this method is just a stub
        return "";
    }

    /* -------------------------------------------------------------------------- */
    /*
     * getLogRoot method
     * /* --------------------------------------------------------------------------
     */
    /**
     * Return the root directory that the FFDC code believes it's writing the logs
     * into
     * 
     * <p><b>Note:</b> This method is just a stub - it always returns the empty
     * string
     * 
     * @return ""
     */
    public static String getLogRoot() {
        // No code - this method is just a stub
        return "";
    }

    /* -------------------------------------------------------------------------- */
    /*
     * resetThread method
     * /* --------------------------------------------------------------------------
     */
    /**
     * Indicate to the FFDC code that the work on the thread has completed.
     * 
     * <p><b>Note:</b> This method is just a stub - calling this version has no effect
     * (not even to throw an exception!)
     * 
     */
    public static void resetThread() {
    // No code - this method is just a stub
    }

    /* -------------------------------------------------------------------------- */
    /*
     * configureComponents method
     * /* --------------------------------------------------------------------------
     */
    /**
     * Ask the FFDC code to initialize itself.
     * 
     */
    public static void configureComponents() {
    // No code needed in this implementation
    }

    /* -------------------------------------------------------------------------- */
    /*
     * registerDiagnosticModule method
     * /* --------------------------------------------------------------------------
     */
    /**
     * Register a diagnostic module
     * 
     * @param diagnosticModule The diagnostic module to be registered
     * @param packageName The package name with which to register
     * @return 0 if the diagnostic module is registered
     *         1 if the diagnostic module was already registered
     *         3 if the diagnostic module could not be initialized
     */
    public static int registerDiagnosticModule(DiagnosticModule diagnosticModule, String packageName) {
        if (modules.containsKey(packageName)) {
            return 1;
        }

        try {
            diagnosticModule.init();
        } catch (Throwable th) {
            // No FFDC code needed - we're reporting the problem to our caller
            // (Note: the server version doesn't FFDC either...)
            return 3;
        }

        modules.put(packageName, diagnosticModule);
        return 0;
    }

    /* -------------------------------------------------------------------------- */
    /*
     * deregisterDiagnosticModule method
     * /* --------------------------------------------------------------------------
     */
    /**
     * Deregister a diagnostic module for a package
     * 
     * @param packageName The package name for which to deregister a diagnostic module
     * @return true if a diagnostic module was registered from the package (and is now
     *         not registered), false otherwise
     */
    public static boolean deregisterDiagnosticModule(String packageName) {
        if (modules.containsKey(packageName)) {
            modules.remove(packageName);
            return true;
        } else
            return false;
    }

    /* -------------------------------------------------------------------------- */
    /*
     * getExceptionFileExtension method
     * /* --------------------------------------------------------------------------
     */
    /**
     * Return the exception file extension
     * 
     * <p><b>Note:</b> This method is just a stub - it always returns the empty
     * string
     * 
     * @return ""
     */
    public static String getExceptionFileExtension() {
        // No code - this method is just a stub
        return "";
    }

    /* -------------------------------------------------------------------------- */
    /*
     * setExceptionFileExtension method
     * /* --------------------------------------------------------------------------
     */
    /**
     * Set the exception file extension
     * 
     * <p><b>Note:</b> This method is just a stub - calling this version has no effect
     * (not even to throw an exception!)
     * 
     * @param newExceptionFileExtension The new file extension
     */
    public static void setExceptionFileExtension(String newExceptionFileExtension) {
    // No code - this method is just a stub
    }

    /* -------------------------------------------------------------------------- */
    /*
     * getExceptionFileNameExtension method
     * /* --------------------------------------------------------------------------
     */
    /**
     * Return the exception index file name extension
     * 
     * <p><b>Note:</b> This method is just a stub - it always returns the empty
     * string
     * 
     * @return ""
     */
    public static String getExceptionIndexFileNameExtension() {
        // No code - this method is just a stub
        return "";
    }

    /* -------------------------------------------------------------------------- */
    /*
     * setExceptionFileNameExtension method
     * /* --------------------------------------------------------------------------
     */
    /**
     * Set the exception index file name extension
     * 
     * <p><b>Note:</b> This method is just a stub - calling this version has no effect
     * (not even to throw an exception!)
     * 
     * @param newExceptionIndexFileNameExtension The new file extension
     */
    public static void setExceptionIndexFileNameExtension(String newExceptionIndexFileNameExtension) {
    // No code - this method is just a stub
    }

    /* -------------------------------------------------------------------------- */
    /*
     * getDiagnosticModuleMap method
     * /* --------------------------------------------------------------------------
     */
    /**
     * Return the map of package names to diagnostic modules
     * 
     * @return Map<String,DiagnosticModule>
     */
    public static Map<String, DiagnosticModule> getDiagnosticModuleMap() {
        return modules;
    }
}

// End of file