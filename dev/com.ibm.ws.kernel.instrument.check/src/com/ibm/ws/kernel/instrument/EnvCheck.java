/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.instrument;


import java.io.FileNotFoundException;
import java.lang.instrument.Instrumentation;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.text.MessageFormat;
import java.util.ResourceBundle;

/**
 * Check's the version of the Java running before starting the java agent, if Java 5 (or below) is being used
 * a translated error message is thrown.
 */
public class EnvCheck {
    // See Launcher.ReturnCode.
    private static final int ERROR_BAD_JAVA_VERSION = 30;
    private static final int ERROR_LAUNCH_EXCEPTION = 24;
    
    private static final String SERIALFILTER_AGENT_JAR = "ws-serialfilteragent.jar";

    
    /**
     * @param args - will just get passed onto BootstrapAgent if version check is successful
     */
    public static void main(String[] args) {
        try {
            BootstrapAgent.main(args);
        } catch (UnsupportedClassVersionError versionError) {
            System.out.println(ResourceBundle.getBundle("com.ibm.ws.kernel.boot.resources.LauncherMessages").getString("error.badVersion"));
            System.exit(ERROR_BAD_JAVA_VERSION);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(ERROR_BAD_JAVA_VERSION);
        }

    }

    /**
     * @param args - will just get passed onto BootstrapAgent if version check is successful
     * @param inst - will just get passed onto BootstrapAgent if version check is successful
     */
    public static void premain(String arg, Instrumentation inst) {
        try {
            BootstrapAgent.premain(arg, inst);
            BootstrapAgent.loadAgent(SERIALFILTER_AGENT_JAR, null);
        } catch (FileNotFoundException fnfe) {
             System.out.println(MessageFormat.format(ResourceBundle.getBundle("com.ibm.ws.kernel.boot.resources.LauncherMessages").getString("error.no.serialfilteragent"), fnfe.getMessage()));
             System.exit(ERROR_LAUNCH_EXCEPTION);
        } catch (UnsupportedClassVersionError versionError) {
            System.out.println(ResourceBundle.getBundle("com.ibm.ws.kernel.boot.resources.LauncherMessages").getString("error.badVersion"));
            System.exit(ERROR_BAD_JAVA_VERSION);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(ERROR_BAD_JAVA_VERSION);
        }
    }

    /**
     * @param args - will just get passed onto BootstrapAgent if version check is successful
     * @param inst - will just get passed onto BootstrapAgent if version check is successful
     */
    public static void agentmain(String arg, Instrumentation inst) {
        try {
            BootstrapAgent.premain(arg, inst);
        } catch (UnsupportedClassVersionError versionError) {
            System.out.println(ResourceBundle.getBundle("com.ibm.ws.kernel.boot.resources.LauncherMessages").getString("error.badVersion"));
            System.exit(ERROR_BAD_JAVA_VERSION);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(ERROR_BAD_JAVA_VERSION);
        }
    }
}
