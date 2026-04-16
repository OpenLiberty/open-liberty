/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.security.jakartasec.handlers;

import java.util.concurrent.ConcurrentHashMap;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * Thread-safe registry for tracking which module (WAR) each HttpAuthenticationMechanism
 * (HAM) belongs to in ALL modes (single EAR, multi-module EAR applications, etc ...)
 *
 * This enables proper HAM isolation in Jakarta Security 4.0 (EE11) which is
 * the expectation.
 */
public class HAMModuleRegistry {

    private static final TraceComponent tc = Tr.register(HAMModuleRegistry.class);

    // Map: applicationName -> (hamSimpleClassName -> moduleName)
    // i.e. multipleModule -> (FormAuthenticationMechanism -> JavaEESecMultipleISForm.war)
    private static final ConcurrentHashMap<String, ConcurrentHashMap<String, String>> registry = new ConcurrentHashMap<>();

    /**
     * Register a HAM class with its module name for a specific application.
     *
     * @param applicationName The application name (e.g., "multipleModule")
     * @param hamClass        The HAM implementation class
     * @param moduleName      The module (WAR) name where this HAM is defined
     */
    public static void register(String applicationName, Class<?> hamClass, String moduleName) {
        if (applicationName == null || hamClass == null || moduleName == null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Skipping registration - null parameter(s) passed.");
                Tr.debug(tc, "application = " + applicationName +
                             ", hamClass = " + hamClass +
                             ", moduleName = " + moduleName);
            }
            return;
        }

        // we really need a simple class name here instead of one wrapped in proxies, etc ...
        // makes the lookup API/contract more explicit
        String hamSimpleName = hamClass.getSimpleName();
        registry.computeIfAbsent(applicationName, k -> new ConcurrentHashMap<>()).put(hamSimpleName, moduleName);

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Registered HAM [" + hamSimpleName +
                         "] linked to module [" + moduleName + "] against app [" + applicationName + "].");
        }
    }

    /**
     * Get the module name for a HAM class in a specific application.
     * Handles CDI proxy classes by extracting the simple class name.
     *
     * @param applicationName The application name
     * @param hamClassName    The HAM class name (may be a CDI proxy)
     * @return The module name, or null if not found
     */
    public static String getModuleName(String applicationName, String hamClassName) {
        if (applicationName == null || hamClassName == null) {
            return null;
        }

        // Extract simple class name from proxy class name
        // e.g., "CustomFormAuthenticationMechanism$Proxy$_$$_WeldClientProxy" -> "CustomFormAuthenticationMechanism"
        String hamSimpleName = extractSimpleClassName(hamClassName);

        ConcurrentHashMap<String, String> appRegistry = registry.get(applicationName);
        String moduleName = appRegistry != null ? appRegistry.get(hamSimpleName) : null;

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Lookup HAM [" + hamClassName + "] " +
                         " (extracted [" + hamSimpleName + "])" +
                         " in app [" + applicationName + "] linked to module [" + moduleName + "].");
        }

        return moduleName;
    }

    /**
     * Extract the simple class name from a potentially proxied class name.
     * Removes CDI proxy suffixes like "$Proxy$_$$_WeldClientProxy".
     *
     * @param className The full class name (may include proxy suffixes)
     * @return The simple class name without proxy suffixes
     */
    private static String extractSimpleClassName(String className) {
        // Handle proxy patterns: $Proxy, $$_Weld, _$$_
        int proxyIndex = className.indexOf("$Proxy");
        if (proxyIndex > 0) {
            return className.substring(0, proxyIndex);
        }

        proxyIndex = className.indexOf("$$_Weld");
        if (proxyIndex > 0) {
            return className.substring(0, proxyIndex);
        }

        proxyIndex = className.indexOf("_$$_");
        if (proxyIndex > 0) {
            return className.substring(0, proxyIndex);
        }

        // No proxy suffix found, return as-is
        return className;
    }

    /**
     * Clear all HAM registrations for a specific application.
     * Called when an application is stopped/undeployed.
     *
     * @param applicationName The application name
     */
    public static void clear(String applicationName) {
        if (applicationName != null) {
            registry.remove(applicationName);
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Cleared HAM registry for app: " + applicationName);
            }
        }
    }

    /**
     * Clear all registrations (for testing purposes).
     */
    public static void clearAll() {
        registry.clear();
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Cleared entire HAM registry");
        }
    }
}
