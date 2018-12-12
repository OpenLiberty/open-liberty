/*******************************************************************************
 * Copyright (c) 2010, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.instrument;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * Simple java.lang.instrument agent that acts as a proxy to the real agent.
 * This agent is intended to be packaged in a jar by itself to prevent pollution
 * of the application class loader's class space with the classes packaged with
 * the target agent or its dependencies.
 * <p>
 * -javaagent:boot-agent.jar=delegate-agent.jar=parms
 *
 * <p>In general this class is not used to bootstrap another agent, it is merely used
 *  to store the Instrumentation class so it can be obtained later on by the Liberty
 *  server instance.
 */
public final class BootstrapAgent {

    static Instrumentation instrumentation;
    static String arg;

    /**
     * {@inheritDoc}
     */
    public static void premain(String arg, Instrumentation inst) throws Exception {
        BootstrapAgent.arg = arg;
        BootstrapAgent.instrumentation = inst;
        setSystemProperties();

        if (arg == null || arg.length() == 0) {
            return;
        }

        // Find the relative location of the target agent jar and save its
        // arguments
        int separator = arg.indexOf('=');
        String targetAgent = separator < 0 ? arg : arg.substring(0, separator);
        String targetAgentArgs = separator < 0 ? "" : arg.substring(separator + 1);

    	try {
            loadAgentJar(targetAgent, targetAgentArgs);
    	} catch (FileNotFoundException e) {
    	    // if the code cannot find the specified jar file, do nothing. This is the original behavior. 
    	}
    }

    /**
     * Loads an agent. Assuming that premain had called before calling this method in order to set Instrumentation object.
     * @param agentJarName an relative path name from the location of bootstrap agent of jar name which is loaded.
     * @param arg argument for the agent.
     */
    public static void loadAgent(String agentJarName, String arg) throws Exception {
    	loadAgentJar(agentJarName, arg);
    }

    private static void loadAgentJar(String agentJarName, String arg) throws Exception {
        // Get the bootstrap agent location
        CodeSource bootstrapCodeSource = BootstrapAgent.class.getProtectionDomain().getCodeSource();
        URI bootstrapLocationURI = bootstrapCodeSource.getLocation().toURI();
        assert ("file".equals(bootstrapLocationURI.getScheme()));

        // Build target agent URI relative to our own
        URI agentURI = bootstrapLocationURI.resolve(agentJarName);

        // Crack open the agent's jar and read the manifest
        File agentFile = new File(agentURI);
        if (!agentFile.isDirectory() && agentFile.exists()) {
            JarFile jarFile = new JarFile(agentFile);

            Manifest manifest = jarFile.getManifest();
            jarFile.close();
            Attributes attrs = manifest.getMainAttributes();

            // Read the agent class name
            String agentClassName = attrs.getValue("Premain-Class");
            if (agentClassName == null) {
                return;
            }

            // Get the required class path
            String agentClassPath = attrs.getValue("Class-Path");
            List<URL> classpath = new ArrayList<URL>();
            classpath.add(agentURI.toURL());
            if (agentClassPath != null) {
                for (String pathEntry : agentClassPath.split("\\s+")) {
                    URI pathURI = agentURI.resolve(pathEntry.trim());
                    classpath.add(pathURI.toURL());
                }
            }

            // Create the class loader and load the target agent with it
            ClassLoader loader = URLClassLoader.newInstance(classpath.toArray(new URL[0]));
            Class<?> clazz = Class.forName(agentClassName, true, loader);

            // Find and invoke the agent's premain method
            try {
                Method premain = clazz.getMethod("premain", String.class, Instrumentation.class);
                premain.invoke(null, arg, BootstrapAgent.instrumentation);
            } catch (NoSuchMethodException e) {
                Method premain = clazz.getMethod("premain", String.class);
                premain.invoke(null, arg);
            }
        } else {
            throw new FileNotFoundException(agentFile.getAbsolutePath());
        }
    }

    /**
     * Set system properties for JVM singletons.
     */
    private static void setSystemProperties() {
        // KernelBootstrap also sets these properties in case the bootstrap
        // agent wasn't used for some reason.

        String loggingManager = System.getProperty("java.util.logging.manager");
        if (loggingManager == null)
            System.setProperty("java.util.logging.manager", "com.ibm.ws.kernel.boot.logging.WsLogManager");

        String managementBuilderInitial = System.getProperty("javax.management.builder.initial");
        if (managementBuilderInitial == null)
            System.setProperty("javax.management.builder.initial", "com.ibm.ws.kernel.boot.jmx.internal.PlatformMBeanServerBuilder");
    }

    public static void main(String[] args) {
        System.out.println("entering main");
        System.out.println("    instrumentation = " + instrumentation);
        System.out.println("exiting main");
    }

    /**
     * Called by other code in Liberty to get hold of the instrumentation class to do stuff.
     */
    public static Instrumentation getInstrumentation() {
        return instrumentation;
    }
}
