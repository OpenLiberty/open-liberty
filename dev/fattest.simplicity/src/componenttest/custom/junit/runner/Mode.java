/*******************************************************************************
 * Copyright (c) 2013, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package componenttest.custom.junit.runner;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used to specify the mode under which a test is to be run.
 * 
 * See {@link TestModeFilter}.
 *
 * The annotation may be placed as a class annotation, in which case, all
 * test methods use the specified mode.  (A method test mode annotation
 * overrides a class test mode annotation.) 
 * 
 * The default mode for tests which do not have a test mode annotation
 * is {@link TestMode#LITE}.  That is, tests with no mode annotation are
 * always run.
 * 
 * The default mode for tests which do have a test annotation is
 * {@link TestMode#FULL}.  However, specifying a test mode which relies
 * on the default value is unusual.
 */
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface Mode {
    /** The default test mode. */
    public static TestMode DEFAULT_MODE = TestMode.FULL;

    /**
     * The test modes in order from the least full set of tests to the most
     * full set of tests.
     *
     * Alternatively, test modes are in the order of tests which are run from
     * most often to least often.
     *
     * Upper case symbolic names must be used.  {@link String#toUpperCase()}
     * is invoked on mode values Values which are passed in from the test
     * framework.
     */
    public static enum TestMode {
        // RAPID, // For example; could be easily added later
        LITE,
        FULL,
        QUARANTINE,
        EXPERIMENTAL
    }

    // TFB: Not sure why a default test mode is specified, nor why the
    //      default is different than the value which is used when no
    //      annotation is present.
    TestMode value() default TestMode.FULL;
}
