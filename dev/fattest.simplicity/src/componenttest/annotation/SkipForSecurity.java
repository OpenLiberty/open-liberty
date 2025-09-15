/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
package componenttest.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import componenttest.custom.junit.runner.SecurityFilter;
import componenttest.topology.impl.JavaInfo;

/**
 * Annotation for expressing test(s) should be skipped if a certain security based
 * system property is set, and optionally a specific JVM runtime is being used.
 *
 * <p>
 * If a property is specified with [key]=[value] format, System.getProperty(key) must return
 * a string value that equalsIgnoreCase the [value], for the test to be skipped.
 * </p>
 * <p>
 * If a property is specified with only a [key], System.getProperty(key) must return
 * a non-null boolean string 'true', for the test to be skipped.
 * </p>
 * <p>
 * If more than one property is specified then all properties must meet the above requirements
 * for the test to be skipped (logical AND).
 * </p>
 * <p>
 * If a runtimeName is specified, {@link JavaInfo#runtimeName()} will be used to determine
 * if the server runtime's runtimeName contains the configured value [CASE SENSITIVE].
 * The default value is an empty string, which is this case will match any runtime name.
 * </p>
 *
 * A test is skipped iff both the property and runtimeName checks are true.
 *
 * This annotation has commonly used property and runtimeName constants that can
 * be used for a wider implementation of test skip logic.
 *
 * @see SecurityFilter
 */
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface SkipForSecurity {

    // Common properties
    public static final String FIPS_140_3 = "global.client.fips_140-3";

    // Common runtimes
    public static final String SEMERU = "Semeru";

    /**
     * A boolean system property, or a key-value system property
     */
    String[] property();

    /**
     * A JVM runtime name that supports the system property attribute
     */
    String runtimeName() default "";

}
