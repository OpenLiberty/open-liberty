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

package com.ibm.ws.ffdc;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.ibm.wsspi.logging.IncidentForwarder;

public final class FFDC {
    /** A mapping of package names to diagnostic modules */
    private static Map<String, DiagnosticModule> modules = new HashMap<String, DiagnosticModule>();
    private static final Set<IncidentForwarder> forwarders = new HashSet<IncidentForwarder>();

    /**
     * Register a diagnostic module
     * 
     * @param diagnosticModule
     *            The diagnostic module to be registered
     * @param packageName
     *            The package name with which to register
     * @return 0 if the diagnostic module is registered 1 if the diagnostic
     *         module was already registered 3 if the diagnostic module could
     *         not be initialized
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

    /**
     * Deregister a diagnostic module for a package
     * 
     * @param packageName
     *            The package name for which to deregister a diagnostic module
     * @return true if a diagnostic module was registered from the package (and
     *         is now not registered), false otherwise
     */
    public static boolean deregisterDiagnosticModule(String packageName) {
        if (modules.containsKey(packageName)) {
            modules.remove(packageName);
            return true;
        } else
            return false;
    }

    /*
     * --------------------------------------------------------------------------
     */
    /*
     * getDiagnosticModuleMap method /*
     * ------------------------------------------
     * --------------------------------
     */
    /**
     * Return the map of package names to diagnostic modules
     * 
     * @return Map<String,DiagnosticModule>
     */
    public static Map<String, DiagnosticModule> getDiagnosticModuleMap() {
        return modules;
    }

    public static boolean registerIncidentForwarder(IncidentForwarder forwarder) {
        return forwarders.add(forwarder);
    }

    public static boolean deregisterIncidentForwarder(IncidentForwarder forwarder) {
        return forwarders.remove(forwarder);
    }

    /**
     * @return
     */
    public static Set<IncidentForwarder> getIncidentForwarders() {
        return forwarders;
    }
}