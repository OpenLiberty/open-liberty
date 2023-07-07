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

/**
 * Liberty environment verifier. Verify that the liberty environment
 * is provisioned to run the current spring version.
 */
//@formatter:off
public class FeatureAuditor implements EnvironmentPostProcessor {

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
        ClassLoader classLoader = FeatureAuditor.class.getClassLoader();
        String resourceName = asResourceName(className);
        boolean foundClass = ( classLoader.getResource(resourceName) != null );

        //        if ( !foundClass ) {
        //            try {
        //                Class.forName(className);
        //                System.out.println("Loaded [ true ] class [ " + className + " ]");
        //            } catch ( ClassNotFoundException e ) {
        //                System.out.println("Loaded [ false ] class [ " + className + " ]");
        //            }
        //        }
        //
        //        System.out.println("Found [ " + foundClass + " ] class [ " + className + " ] as [ " + resourceName + " ]");
        //        System.out.println("Using [ " + classLoader + " ]");

        return foundClass;
    }

    protected static void warning(Type msgId, Object...parms) {
        ApplicationTr.warning(msgId, parms);
    }

    public static final String SPRING15_FACTORY = "org.springframework.boot.context.embedded.EmbeddedServletContainerFactory";
    public static final String SPRING20_FACTORY = "org.springframework.boot.web.servlet.server.ServletWebServerFactory";
    // The Spring 3.0 factory is the same as the Spring 2.0 factory.

    public static final String LIBERTY15_CONFIG = "com.ibm.ws.springboot.support.web.server.version15.container.LibertyConfiguration";
    public static final String LIBERTY20_CONFIG = "com.ibm.ws.springboot.support.web.server.version20.container.LibertyConfiguration";
    public static final String LIBERTY30_CONFIG = "com.ibm.ws.springboot.support.web.server.version30.container.LibertyConfiguration";

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
        String springBootVersion = SpringBootVersion.getVersion();
        //        System.out.println("spring.boot.version = " + springBootVersion);
        boolean springIsAtLeast20 = ( springBootVersion.compareTo("2.0.0") >= 0 );
        boolean springIsAtLeast30 = ( springBootVersion.compareTo("3.0.0") >= 0 );

        String javaVersion = System.getProperty("java.version");
        //        System.out.println("java.version = " + javaVersion);

        String javaSpecVersion = System.getProperty("java.vm.specification.version");
        //        System.out.println("java.vm.specification.version = " + javaSpecVersion);

        int javaSpecVersionNo = Integer.parseInt(javaSpecVersion);

        int requiredJavaVersion;
        String requiredVersionText;
        boolean isSatisfied;

        if ( !springIsAtLeast20 ) {
            requiredJavaVersion = 7;
            requiredVersionText = "Java 7";
        } else if ( !springIsAtLeast30 ) {
            requiredJavaVersion = 8;
            requiredVersionText = "Java 8";
        } else {
            requiredJavaVersion = 17;
            requiredVersionText = "Java 17";
        }

        if ( javaSpecVersionNo < requiredJavaVersion ) {
            warning(Type.WARNING_UNSUPPORTED_JAVA_VERSION, javaVersion, springBootVersion, requiredVersionText);
        } else {
            //            System.out.println("Validated required java version [ " + requiredVersionText + " ]" +
            //                               " for Spring Boot version [ " + springBootVersion + " ]");
        }

        if ( springIsAtLeast30 ) {
            if ( isClassAvailable(SPRING20_FACTORY) &&
                 (isClassAvailable(LIBERTY15_CONFIG) || isClassAvailable(LIBERTY20_CONFIG)) ) {
                if ( !isClassAvailable(LIBERTY30_CONFIG) ) {
                    //                    System.out.println("Failed to liberty spring configuration class [ " + LIBERTY30_CONFIG + " ]");
                    throw new ApplicationError(Type.ERROR_NEED_SPRING_BOOT_VERSION_30);
                }
            }
        } else if ( springIsAtLeast20 ) {
            if ( isClassAvailable(SPRING20_FACTORY) &&
                 (isClassAvailable(LIBERTY15_CONFIG) || isClassAvailable(LIBERTY30_CONFIG)) ) {
                if ( !isClassAvailable(LIBERTY20_CONFIG) ) {
                    //                    System.out.println("Failed to liberty spring configuration class [ " + LIBERTY20_CONFIG + " ]");
                    throw new ApplicationError(Type.ERROR_NEED_SPRING_BOOT_VERSION_20);
                }
            }
        } else {
            if ( isClassAvailable(SPRING15_FACTORY) &&
                 (isClassAvailable(LIBERTY20_CONFIG) || isClassAvailable(LIBERTY30_CONFIG)) ) {
                if ( !isClassAvailable(LIBERTY15_CONFIG) ) {
                    //                    System.out.println("Failed to liberty spring configuration class [ " + LIBERTY15_CONFIG + " ]");
                    throw new ApplicationError(Type.ERROR_NEED_SPRING_BOOT_VERSION_15);
                }
            }
        }

        // Servlet and WebSocket might not be used by the application.
        // This is detected by the inclusion of a spring class.
        //
        // Note: This is an approximate test, depending on whether the
        // spring packaging is minimal for the function in use.  That is
        // to say, the spring class might be included in the packaging but
        // is unused.
        //
        // Note: This component is not transformed for jakarta.  That means that
        // support for both javax and jakarta versions of the required API classes
        // is provided, and means that the class availability test must not be coded
        // with reference the target class.

        if ( isClassAvailable("org.springframework.web.WebApplicationInitializer") ) {
            String servletClassName =
                ( springIsAtLeast30 ? "jakarta.servlet.Servlet" : "javax.servlet.Servlet");
            if ( !isClassAvailable(servletClassName) ) {
                //                System.out.println("Failed to locate servlet class [ " + servletClassName + " ]");
                throw new ApplicationError(Type.ERROR_MISSING_SERVLET_FEATURE);
            } else {
                //                System.out.println("The Spring Servlet function was located;" +
                //                                   " the base Servlet API is provisioned.");
            }
        } else {
            //            System.out.println("The Spring Servlet function was not located.");
        }

        String webSocketHandlerClassName = "org.springframework.web.socket.WebSocketHandler";

        if ( isClassAvailable(webSocketHandlerClassName) ) {
            //            System.out.println("Noted WebSocket spring class [ " + webSocketHandlerClassName + " ]");

            String webSocketClassName =
                ( springIsAtLeast30 ? "jakarta.websocket.WebSocketContainer" : " javax.websocket.WebSocketContainer" );
            if ( !isClassAvailable(webSocketClassName) ) {
                System.out.println("Failed to locate websocket class [ " + webSocketClassName + " ]");
                // throw new ApplicationError(Type.ERROR_MISSING_WEBSOCKET_FEATURE);
            } else {
                //                System.out.println("The Spring WebSocket function was located;" +
                //                                   " the base WebSocket API is provisioned.");
            }
        } else {
            //            System.out.println("The Spring WebSocket function was not located.");
        }
    }
}
//@formatter:on
