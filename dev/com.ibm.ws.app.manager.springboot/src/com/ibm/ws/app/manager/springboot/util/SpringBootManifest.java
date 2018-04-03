/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.app.manager.springboot.util;

import java.util.jar.Manifest;

/**
 * Relevant Spring Boot information contained in the Spring Boot JAR
 * manifest.
 */
public class SpringBootManifest {
    private static final String JAR_MAIN_CLASS = "Main-Class";
    private static final String SPRING_START_CLASS_HEADER = "Start-Class";
    private static final String SPRING_BOOT_CLASSES_HEADER = "Spring-Boot-Classes";
    private static final String SPRING_BOOT_LIB_HEADER = "Spring-Boot-Lib";

    enum SpringLauncher {
        JarLauncher("JarLauncher", "BOOT-INF/lib/", "BOOT-INF/classes/"),
        WarLauncher("WarLauncher", "WEB-INF/lib/", "WEB-INF/classes/");

        private SpringLauncher(String name, String libDefault, String classesDefault) {
            this.name = name;
            this.libDefault = libDefault;
            this.classesDefault = classesDefault;
        }

        private String name;
        private String libDefault;
        private String classesDefault;

        static SpringLauncher fromMainClass(String mainClass) {
            if (mainClass != null) {
                mainClass = mainClass.trim();
                for (SpringLauncher l : SpringLauncher.values()) {
                    if (mainClass.endsWith(l.name)) {
                        return l;
                    }
                }
            }
            return JarLauncher;
        }

        String getDefault(String springBootHeaderKey) {
            switch (springBootHeaderKey) {
                case SPRING_BOOT_CLASSES_HEADER:
                    return classesDefault;
                case SPRING_BOOT_LIB_HEADER:
                    return libDefault;
                default:
                    return null;
            }
        }
    }

    private final String springStartClass;
    private final String springBootClasses;
    private final String springBootLib;

    /**
     * Returns the start class for the Spring Boot application.
     *
     * @return the start class
     */
    public String getSpringStartClass() {
        return springStartClass;
    }

    /**
     * Returns the path to the classes directory for the Spring
     * Boot Application
     *
     * @return the path to the classes directory
     */
    public String getSpringBootClasses() {
        return springBootClasses;
    }

    /**
     * Returns the path to the lib folder for the Spring Boot application
     *
     * @return the path to the lib folder
     */
    public String getSpringBootLib() {
        return springBootLib;
    }

    public SpringBootManifest(Manifest mf) {
        String mainClass = mf.getMainAttributes().getValue(JAR_MAIN_CLASS);
        SpringLauncher launcher = SpringLauncher.fromMainClass(mainClass);
        springStartClass = mf.getMainAttributes().getValue(SPRING_START_CLASS_HEADER);
        springBootClasses = getSpringHeader(mf, SPRING_BOOT_CLASSES_HEADER, launcher);
        springBootLib = getSpringHeader(mf, SPRING_BOOT_LIB_HEADER, launcher);
    }

    private static String getSpringHeader(Manifest mf, String springBootHeaderKey, SpringLauncher launcher) {
        String value = mf.getMainAttributes().getValue(springBootHeaderKey);
        if (value == null) {
            value = launcher.getDefault(springBootHeaderKey);
        }
        return removeTrailingSlash(value);
    }

    private static String removeTrailingSlash(String path) {
        if (path != null && path.length() > 1 && path.endsWith("/")) {
            return path.substring(0, path.length() - 1);
        }
        return path;
    }
}
