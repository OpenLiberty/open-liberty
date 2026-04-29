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

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * Thread-safe registry for tracking which module (WAR) each HttpAuthenticationMechanism
 * (HAM) belongs to in ALL modes (single EAR, multi-module EAR applications, etc ...)
 *
 * This enables proper HAM isolation in Jakarta Security 4.0 (EE11) which is
 * the expectation.
 *
 * Supports multiple modules registering the same HAM class (e.g., FormAuthenticationMechanism
 * in multiple WARs within the same EAR).
 */
public class HAMModuleRegistry {

    private static final TraceComponent tc = Tr.register(HAMModuleRegistry.class);

    // Map: applicationName -> (hamSimpleClassName -> Set<moduleName>)
    // i.e. multipleModule2 -> (FormAuthenticationMechanism -> {JavaEESecMultipleISForm.war, JavaEESecMultipleISForm2.war})
    private static final ConcurrentHashMap<String, ConcurrentHashMap<String, Set<String>>> registry = new ConcurrentHashMap<>();

    /**
     * Register a HAM class with its module name for a specific application.
     * Supports multiple modules registering the same HAM class.
     *
     * @param applicationName The application name (e.g., "multipleModule2")
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

        // Get or create the app-level map
        ConcurrentHashMap<String, Set<String>> appRegistry = registry.computeIfAbsent(applicationName, k -> new ConcurrentHashMap<>());

        // Get or create the set of modules for this HAM class, and add the module
        Set<String> modules = appRegistry.computeIfAbsent(hamSimpleName, k -> new CopyOnWriteArraySet<>());
        modules.add(moduleName);

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Registered HAM [" + hamSimpleName +
                         "] linked to module [" + moduleName + "] against app [" + applicationName + "].");
            Tr.debug(tc, "Registry for app [" + applicationName + "] HAM [" + hamSimpleName + "] now has " +
                         modules.size() + " module(s): " + modules);
        }
    }

    /**
     * Get the module name for a HAM class in a specific application and module context.
     * When multiple modules register the same HAM class, returns the module name that matches
     * the requested module, or null if the HAM is not registered for that module.
     *
     * @param applicationName The application name
     * @param hamClassName    The HAM class name (may be a CDI proxy)
     * @param requestedModule The module name being requested (for multi-module disambiguation)
     * @return The module name if the HAM is registered for that module, or null if not found
     */
    public static String getModuleName(String applicationName, String hamClassName, String requestedModule) {

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Fetching module name for application ["
                         + applicationName + "], ham class name ["
                         + hamClassName + "] and requested module ["
                         + requestedModule + "].");
        }
        if (applicationName == null || hamClassName == null) {
            return null;
        }

        // Extract simple class name from proxy class name
        // e.g., "CustomFormAuthenticationMechanism$Proxy$_$$_WeldClientProxy" -> "CustomFormAuthenticationMechanism"
        String hamSimpleName = extractSimpleClassName(hamClassName);

        ConcurrentHashMap<String, Set<String>> appRegistry = registry.get(applicationName);
        if (appRegistry == null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "No registry found for app [" + applicationName + "]");
            }
            return null;
        }

        Set<String> modules = appRegistry.get(hamSimpleName);
        if (modules == null || modules.isEmpty()) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "No modules registered for HAM [" + hamSimpleName + "] in app [" + applicationName + "]");
            }
            return null;
        }

        // If requested module is specified and found in the set, return it
        if (requestedModule != null && modules.contains(requestedModule)) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Lookup HAM [" + hamClassName + "] (extracted [" + hamSimpleName + "]) " +
                             "in app [" + applicationName + "] for module [" + requestedModule + "]: FOUND");
            }
            return requestedModule;
        }

        // If only one module registered this HAM, return it (backward compatibility)
        if (modules.size() == 1) {
            String singleModule = modules.iterator().next();
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Lookup HAM [" + hamClassName + "] (extracted [" + hamSimpleName + "]) " +
                             "in app [" + applicationName + "]: single module [" + singleModule + "]");
            }
            return singleModule;
        }

        // Multiple modules registered, but requested module not found
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Lookup HAM [" + hamClassName + "] (extracted [" + hamSimpleName + "]) " +
                         "in app [" + applicationName + "] for module [" + requestedModule + "]: NOT FOUND. " +
                         "Available modules: " + modules);
        }
        return null;
    }

    /**
     * Get the module name for a HAM class in a specific application.
     * Backward compatibility method - delegates to the new method with null requestedModule.
     *
     * @param applicationName The application name
     * @param hamClassName    The HAM class name (may be a CDI proxy)
     * @return The module name, or null if not found or ambiguous
     */
    public static String getModuleName(String applicationName, String hamClassName) {
        return getModuleName(applicationName, hamClassName, null);
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
