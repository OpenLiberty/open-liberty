/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.springboot.support.shutdown;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootVersion;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;

import com.ibm.ws.app.manager.springboot.container.ApplicationError;
import com.ibm.ws.app.manager.springboot.container.ApplicationTr;
import com.ibm.ws.app.manager.springboot.container.ApplicationTr.Type;

public class FeatureAuditor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment env, SpringApplication app) {

        String sbv = SpringBootVersion.getVersion();
        System.out.println("spring.boot.version = " + sbv);

        // TODO:
        /*
         * SB 3
         * 1. check to make sure they are using java 17 or higher
         * 2. determine the version of servlet to check agains javax vs jakartaee
         * 3. we also need a checkSpringBoot30() method similar to the ones for 15 and 20
         */

        checkJavaVersion(sbv);

        /*
         * Throw an Application error if the wrong version of spring boot feature is
         * enabled
         */
        try {
            Class.forName("org.springframework.boot.context.embedded.EmbeddedServletContainerFactory");
            Class.forName(
                          "com.ibm.ws.springboot.support.web.server.version20.container.LibertyConfiguration");
            checkSpringBootVersion15();

        } catch (ClassNotFoundException e) {

        }

        try {
            Class.forName("org.springframework.boot.web.servlet.server.ServletWebServerFactory");
            Class.forName(
                          "com.ibm.ws.springboot.support.web.server.version15.container.LibertyConfiguration");
            checkSpringBootVersion20();

        } catch (ClassNotFoundException e) {

        }

        // SB3
        try {
            // Not sure if this is correct....
            Class.forName("org.springframework.boot.web.servlet.server.ServletWebServerFactory");
            Class.forName("com.ibm.ws.springboot.support.web.server.version30.container.LibertyConfiguration");
            checkSpringBootVersion30();

        } catch (ClassNotFoundException e) {

        }

        /* Throw an application error if servlet feature is not enabled */
        try {
            Class.forName("org.springframework.web.WebApplicationInitializer");
            checkServletPresent(sbv);
        } catch (ClassNotFoundException e) {

        }

        /* Throw an application error if websocket feature is not enabled */
        try {
            Class.forName("org.springframework.web.socket.WebSocketHandler");
            checkWebSocketPresent();
        } catch (ClassNotFoundException e) {

        }
    }

    private void checkJavaVersion(String sbv) {
        String javaVersion = System.getProperty("java.version");
        /*
         * Properties p = System.getProperties();
         * Set<String> keys = p.stringPropertyNames();
         * for (String key : keys) {
         * System.out.println("key = " + key);
         * System.out.println("value = " + p.getProperty(key));
         * }
         */
        System.out.println("java.version = " + javaVersion);

        // java version isnt supported by sb version, upgrade to 2.x or higher
        if (!javaVersion.startsWith("1.")) {
            try {
                Class.forName("org.springframework.boot.context.embedded.EmbeddedServletContainerFactory");
                ApplicationTr.warning(Type.WARNING_UNSUPPORTED_JAVA_VERSION, javaVersion, sbv); //SpringBootVersion.getVersion());
            } catch (ClassNotFoundException e) {

            }
        }

        // SB 3 - must have java 17 or higher for 3.x - do we need this?  Or would this be caught by the features required?
        String javaSpecVersion = System.getProperty("java.vm.specification.version");
        if (sbv.startsWith("3.")) {
            int jVersion = Integer.parseInt(javaSpecVersion);
            System.out.println("javaversion int = " + jVersion);
            if (jVersion >= 17) {
                System.out.println("java okay");
            } else {
                System.out.println("java needs to upgrade to 17 or higher");
            }
        }
    }

    private void checkWebSocketPresent() {
        try {
            Class.forName("javax.websocket.WebSocketContainer");
        } catch (ClassNotFoundException e) {
            throw new ApplicationError(Type.ERROR_MISSING_WEBSOCKET_FEATURE);
        }
    }

    private void checkServletPresent(String springBootVersion) {
        try {
            if (springBootVersion.startsWith("1.") || springBootVersion.startsWith("2.")) {
                Class.forName("javax.servlet.Servlet"); // SB 1 & 2
            } else if (springBootVersion.startsWith("3.")) {
                Class.forName("jakarta.servlet.Servlet"); // SB 3
            }
        } catch (ClassNotFoundException e) {
            throw new ApplicationError(Type.ERROR_MISSING_SERVLET_FEATURE);
        }
    }

    private void checkSpringBootVersion15() {
        try {
            Class.forName(
                          "com.ibm.ws.springboot.support.web.server.version15.container.LibertyConfiguration");
        } catch (ClassNotFoundException e) {
            throw new ApplicationError(Type.ERROR_NEED_SPRING_BOOT_VERSION_15);
        }

    }

    private void checkSpringBootVersion20() {
        try {
            Class.forName(
                          "com.ibm.ws.springboot.support.web.server.version20.container.LibertyConfiguration");
        } catch (ClassNotFoundException e) {
            throw new ApplicationError(Type.ERROR_NEED_SPRING_BOOT_VERSION_20);
        }

    }

    // SB3
    private void checkSpringBootVersion30() {
        try {
            Class.forName(
                          "com.ibm.ws.springboot.support.web.server.version30.container.LibertyConfiguration");
        } catch (ClassNotFoundException e) {
            throw new ApplicationError(Type.ERROR_NEED_SPRING_BOOT_VERSION_30);
        }

    }
}
