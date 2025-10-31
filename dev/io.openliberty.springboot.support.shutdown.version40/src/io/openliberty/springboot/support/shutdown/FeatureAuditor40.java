/*******************************************************************************
 * Copyright (c) 2023, 2025 IBM Corporation and others.
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
package io.openliberty.springboot.support.shutdown;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootVersion;
import org.springframework.core.env.ConfigurableEnvironment;

import com.ibm.ws.app.manager.springboot.container.ApplicationTr;
import com.ibm.ws.app.manager.springboot.container.ApplicationTr.Type;

/**
 * Liberty environment verifier. Verify that the liberty environment
 * is provisioned to run the current spring version.
 */
//@formatter:off
public class FeatureAuditor40 implements EnvironmentPostProcessor {

    /**
     * Generate the resource name for a specified class.
     *
     * Convert '.' into '/' and append '.class'.
     *
     * Note: This will not work for inner classes, which convert one or
     * more '.' into '$' instead of '/'.
     *
     * @param className A fully qualified non-inner class name.
     *
     * @return The name of the resource of the class.
     */
    protected static String asResourceName(String className) {
        return className.replace('.', '/') + ".class";
    }

    /**
     * Tell if a class is available in the current classloading environment.
     *
     * That is, tell if the resource of the class is available using this class's
     * classloader.
     *
     * Note: This does not work for inner classes.  See {@link #asResourceName(String)}.
     *
     * @param className The name of the class which is to be located.
     *
     * @return True or false telling if the class resource was located.
     */
    protected static boolean isClassAvailable(String className) {
        ClassLoader classLoader = FeatureAuditor40.class.getClassLoader();
        String resourceName = asResourceName(className);
        boolean foundClass = ( classLoader.getResource(resourceName) != null );

        // System.out.println("FeatureAuditor: Found [ " + foundClass + " ] class [ " + className + " ] as [ " + resourceName + " ]");
        // System.out.println("FeatureAuditor: Using [ " + classLoader + " ]");

        return foundClass;
    }

    protected static void warning(Type msgId, Object...parms) {
        ApplicationTr.warning(msgId, parms);
    }

    /**
     * Verify that the liberty server is provisioned correctly for the current spring version.
     *
     * <ul><li>Verify that the java version is sufficient to run the spring version.</li>
     *     <li>Verify that the necessary liberty spring feature is provisioned.</li>
     *     <li>Verify that the necessary web featres are provisioned.</li>
     * </ul>
     *
     * Java checks are done by cross-checking the java version with the spring version.
     *
     * Provisioning checks are done by verifying that specific classes are available in the
     * server environment.
     *
     * Provisioning failures result in a thrown {@link ApplicationExcepption}.
     *
     * Java version failures result in a warning.
     *
     * @param env The configuration environment.  Currently ignored.
     * @param app The spring application.  Currently ignored.
     */
    @Override
    public void postProcessEnvironment(ConfigurableEnvironment env, SpringApplication app) {
        String appSpringBootVersion = SpringBootVersion.getVersion();
        boolean appHasSpring40 = ( appSpringBootVersion.compareTo("4.0.0") >= 0 );

        boolean appUsesServlets = isClassAvailable("org.springframework.web.WebApplicationInitializer");
        boolean appUsesWebsockets = isClassAvailable("org.springframework.web.socket.WebSocketHandler");

        // System.out.println("App uses servlets [ " + appUsesServlets + " ]");
        // System.out.println("App uses websockets [ " + appUsesWebsockets + " ]");

        // Previously, the application was tested for either
        // "org.springframework.boot.context.embedded.EmbeddedServletContainerFactory"
        // or for
        // "org.springframework.boot.web.servlet.server.ServletWebServerFactory"
        //
        // However, these checks are consistency checks against the spring application
        // content.  The checks were removed.

        // https://www.ibm.com/docs/en/was-liberty/base?topic=liberty-deploying-spring-boot-application


        boolean libertyHasSpring15 =
            isClassAvailable("com.ibm.ws.springboot.support.web.server.version15.container.LibertyConfiguration");
        boolean libertyHasSpring20 =
            isClassAvailable("com.ibm.ws.springboot.support.web.server.version20.container.LibertyConfiguration");
        boolean libertyHasSpring30 =
            isClassAvailable("io.openliberty.springboot.support.web.server.version30.container.LibertyConfiguration");
        boolean libertyHasSpring40 =
                        isClassAvailable("io.openliberty.springboot.support.web.server.version40.container.LibertyConfiguration");

        String libertySpringFeature =
            (libertyHasSpring15 ? "springBoot-1.5" :
             (libertyHasSpring20 ? "springBoot-2.0" :
              (libertyHasSpring30 ? "springBoot-3.0" :
                (libertyHasSpring40 ? "springBoot-4.0": null))));

        // System.out.println("Liberty spring feature [ " + libertySpringFeature + " ]");

        boolean libertyHasJakartaServlets = isClassAvailable("jakarta.servlet.Servlet");

        boolean libertyHasJakartaWebsockets = isClassAvailable("jakarta.websocket.WebSocketContainer");

        // System.out.println("Liberty has javax servlets [ " + libertyHasJavaxServlets + " ]");
        // System.out.println("Liberty has javax websockets [ " + libertyHasJavaxWebsockets + " ]");
        // System.out.println("Liberty has jakarta  servlets [ " + libertyHasJakartaServlets + " ]");
        // System.out.println("Liberty has jakarta websockets [ " + libertyHasJakartaWebsockets + " ]");

        // First, test that the Spring Boot version can be run with the current java.

        // https://docs.spring.io/spring-boot/docs/current/reference/html/getting-started.html#getting-started.system-requirements
        //
        // "Spring Boot 3.1.1 requires Java 17 and is compatible up to and
        // including Java 20. Spring Framework 6.0.10 or above is also required."
        //
        // https://endoflife.date/spring-boot
        // 3.0 - 3.1 	17 - 20
        //
        // 2.7 	         8 - 20
        // 2.6 	         8 - 19
        // 2.5 	         8 - 18
        // 2.4 	         8 - 16
        // 2.2 - 2.3 	 8 - 15
        // 2.1 	         8 - 12
        // 2.0 	         8 - 9
        //
        // 1.5 	         6 - 8

        String javaVersion = System.getProperty("java.version");
        String javaSpecVersion = System.getProperty("java.vm.specification.version");
        int javaSpecVersionNo = 0;
        if (javaSpecVersion.contains(".")) {
            javaSpecVersionNo = Integer.parseInt(javaSpecVersion.substring(javaSpecVersion.indexOf(".") + 1, javaSpecVersion.length())) ;
        } else {
            javaSpecVersionNo = Integer.parseInt(javaSpecVersion);
        }

        int requiredJavaVersion;
        String requiredVersionText;

        // Note: Checking is against the application content, not against the
        // server provisioning.  Feature requirements should detect feature/java
        // dependencies well before application startup.

        if ( appHasSpring40 ) {
            requiredJavaVersion = 17;
            requiredVersionText = "JavaSE-17.0";

            if ( javaSpecVersionNo < requiredJavaVersion ) {
                ApplicationTr.warning("warning.unsupported.spring.java.version",
                                      javaVersion, appSpringBootVersion, requiredVersionText);
                // CWWKC0270W: The current Java version {0} does not support the provisioned
                // Spring Boot version {1}: Java version {2} or higher is required.
            }

        }

        // Next, look for mismatched features:

        // Leaving this off: Feature intercompatibility should checked as a
        // usual step of feature initialization, well before applications are
        // started.

        // Then, look content errors:
        //   The liberty spring version must be correct for the application content.
        //   The liberty servlet and websocket versions must be correct for the application contents.
        //
        // Match the liberty spring version to the application content!

        if ( appHasSpring40 ) {
            // Don't test '!libertyHasSpring40'; that may not be set.
            if ( libertyHasSpring15 || libertyHasSpring20  || libertyHasSpring30) {
                ApplicationTr.error("error.spring4.required", "springBoot-4.0", libertySpringFeature, "4.0");
                // "CWWKC0276E: Error: Feature springBoot-4.0 must be provisioned:
                // Feature springBoot-1.5 or springBoot-2.0 or springBoot-3.0 is provisioned
                // and the application has Spring 4.0 content."
            } else if ( appUsesServlets && !libertyHasJakartaServlets ) {
                ApplicationTr.error("error.spring4.requires.servlet61.or.later.application");
                // "CWWKC0277E: Error: Feature servlet-6.1 or later must be provisioned:
                // Feature springBoot-4.0 is provisioned and the application uses Servlets."
            } else if ( appUsesWebsockets && !libertyHasJakartaWebsockets ) {
                ApplicationTr.error("error.spring4.requires.websocket22.or.later.application");
                // "CWWKC0278E: Error: Feature websocket-2.2 or later must be provisioned:
                // Feature springBoot-4.0 is provisioned and the application uses WebSockets."
            }
        }
    }
}
//@formatter:on
