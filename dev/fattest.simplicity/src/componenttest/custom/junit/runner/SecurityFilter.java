/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package componenttest.custom.junit.runner;

import java.io.IOException;
import java.util.stream.Stream;

import org.junit.runner.Description;
import org.junit.runner.manipulation.Filter;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.annotation.SkipForSecurity;
import componenttest.topology.impl.JavaInfo;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.FATServletClient;
import componenttest.topology.utils.PrivHelper;

/**
 * Filter tests based on the attributes configured on the
 * {@link SkipForSecurity} annotation
 */
public class SecurityFilter extends Filter {

    private static final Class<?> c = SecurityFilter.class;

    /** {@inheritDoc} */
    @Override
    public String describe() {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public boolean shouldRun(Description desc) {
        boolean result = true; // default
        String reason = "";

        do {
            // Test: does method or class have the @SkipForSecurity annotation?
            SkipForSecurity anno = desc.getAnnotation(SkipForSecurity.class);
            if (anno == null) {
                anno = FilterUtils.getTestClass(desc, c).getAnnotation(SkipForSecurity.class);
            }

            if (anno == null) {
                reason += "Annotation @SkipForSecurity not present on method nor class";
                break; // default true
            }

            // Test: does configured property match system state?
            String[] properties = anno.property();
            boolean arePropertiesEnabled = true;

            for (String property : properties) {
                if (property.contains("=")) {
                    String[] key_value = property.split("=", 2);
                    String actual = PrivHelper.getProperty(key_value[0]);
                    arePropertiesEnabled &= key_value[1].equals(actual);
                    reason += "The property " + key_value[0]
                              + " had a value of " + actual
                              + " expected " + key_value[1] + ". ";
                } else {
                    String actual = PrivHelper.getProperty(property);
                    arePropertiesEnabled &= Boolean.parseBoolean(actual);
                    reason += "The property " + property
                              + " had a value of " + actual
                              + " expected true. ";
                }
            }

            if (arePropertiesEnabled) {
                reason = "All properties matched: " + reason;
            } else {
                reason = "At least one property did not match: " + reason;
                break; // default true
            }

            // Test: does runtime name match JVM's current state?
            String runtimeName = anno.runtimeName();

            if (runtimeName == null || runtimeName.isEmpty()) {
                result = false;
                reason += "\nNo runtime name was configured.";
                break;
            }

            String actual = getRuntimeForServer(desc);
            String postfix = "The current JVM's runtime name was " + actual
                             + " expected to contain " + runtimeName;

            if (actual.contains(runtimeName)) {
                result = false;
                reason += "\nRuntime name matched: " + postfix;
                break;
            }

            // Property is enabled, and the runtimeName does not match, therefore test should run
            reason += "\nRuntime name mismatched: " + postfix;
            break; // default true
        } while (false);

        // Log reason for the skip
        if (result) {
            Log.debug(c, "The test " + desc.getMethodName() + " will run because:\n" + reason);
        } else {
            Log.info(c, "shouldRun", "The test " + desc.getMethodName() + " will be skipped because:\n" + reason);
        }

        return result;
    }

    /**
     * Search the annotated class for the first field with a @Server annotation.
     * Extract the server name for the annotated field.
     * Then return the runtime name for that sever.
     *
     * @param  desc                  the test description
     * @return                       the runtime name for the server's JVM
     * @throws IllegalStateException if the state of the test class does not match expectations,
     *                                   or we fail to determine the runtime name of the server.
     */
    private String getRuntimeForServer(Description desc) {
        Class<?> testClass = FilterUtils.getTestClass(desc, c);

        // Someone might have put the SkipForSecurity on a test class that we don't expect
        // to have a @Server field
        if (!FATServletClient.class.isAssignableFrom(testClass)) {
            throw new IllegalStateException("The @SkipForSecurity annotation is only valid on a "
                                            + "FATServletClient test class and it's methods. The testClass "
                                            + testClass + " is not an instance of FATServletClient");
        }

        String serverName = Stream.of(testClass.getDeclaredFields())
                        .filter(field -> field.isAnnotationPresent(Server.class))
                        .map(field -> field.getAnnotation(Server.class))
                        .map(anno -> anno.value())
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("The @SkipForSecurity annotation is only valid on a "
                                                                     + "FATServletClient test class that has a public static field annotated with @Server."));

        LibertyServer server = LibertyServerFactory.getLibertyServer(serverName);

        try {
            return JavaInfo.forServer(server).runtimeName();
        } catch (IOException e) {
            throw new IllegalStateException("Could not obtain JVM runtime name for server " + serverName, e);
        }
    }
}
