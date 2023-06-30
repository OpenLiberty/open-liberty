/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.kernel.boot.jmx.service;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * Allows access to the Virtual Machine object that was obtained before setting the security manager
 * so it does not need to be obtained again. HotSpot Java 17 levels are missing doPrivileged calls
 * which prevent getting the VirtualMachine object after the security manager is set.
 */
public final class VirtualMachineHelper {

    private static final class PIDHolder {
        static final String PID;
        static {
            String pid = null;
            String name = ManagementFactory.getRuntimeMXBean().getName();
            int index = name.indexOf('@');
            if (index >= 0) {
                pid = name.substring(0, index);
                if (!pid.matches("[0-9]+")) {
                    pid = null;
                }
            }
            PID = pid;
        }
    }

    private static final class VirtualMachineHolder {
        static final Object virtualMachine;
        static final Method remoteDataDumpMethod;
        static final RuntimeException error;
        static {

            ClassLoader toolsClassLoader;

            File toolsJar = getToolsJar();
            if (toolsJar == null) {
                // The attach classes are on the boot classpath on Mac.
                toolsClassLoader = VirtualMachineHelper.class.getClassLoader();
            } else {
                try {
                    toolsClassLoader = new URLClassLoader(new URL[] { toolsJar.getAbsoluteFile().toURI().toURL() });
                } catch (MalformedURLException e) {
                    throw new RuntimeException(e);
                }
            }

            Object vm = null;
            Method dumpMethod = null;
            Exception ex = null;
            try {
                Class<?> vmClass = toolsClassLoader.loadClass("com.sun.tools.attach.VirtualMachine");
                Method attachMethod = vmClass.getMethod("attach", new Class<?>[] { String.class });
                vm = attachMethod.invoke(null, new Object[] { getPID() });
                dumpMethod = vm.getClass().getMethod("remoteDataDump", new Class<?>[] { Object[].class });
            } catch (ClassNotFoundException e) {
                // The class isn't found, so we won't be able to create dumps.
            } catch (Exception e) {
                ex = new RuntimeException(e);
            }
            virtualMachine = vm;
            remoteDataDumpMethod = dumpMethod;
            error = ex == null ? null : new RuntimeException(ex);
        }

        /**
         * Gets tools.jar from the JDK, or null if not found (for example, when
         * running with a JRE rather than a JDK, or when running on Mac).
         */
        private static File getToolsJar() {
            String javaHome = System.getProperty("java.home");
            File file = new File(javaHome, "../lib/tools.jar");
            if (!file.exists()) {
                file = new File(javaHome, "lib/tools.jar");
                if (!file.exists()) {
                    return null;
                }
            }

            return file;
        }

    }

    public static String getPID() {
        return PIDHolder.PID;
    }

    public static Object getVirtualMachine() {
        if (getPID() == null) {
            // Java Attach requires a PID.
            return null;
        }

        Object vm = VirtualMachineHolder.virtualMachine;

        if (vm != null) {
            return vm;
        }

        RuntimeException exception = VirtualMachineHolder.error;
        if (exception != null) {
            throw exception;
        }

        return null;
    }

    public static Method getRemoteDumpMethod() {
        if (getPID() == null) {
            // Java Attach requires a PID.
            return null;
        }

        Method dumpMethod = VirtualMachineHolder.remoteDataDumpMethod;

        if (dumpMethod != null) {
            return dumpMethod;
        }

        RuntimeException exception = VirtualMachineHolder.error;
        if (exception != null) {
            throw exception;
        }

        return null;
    }
}
