/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
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
 * Annotation for expressing that a test method should only run in a given framework mode.
 * Unannotated tests default to lite, so the the default for an annotated
 * test that hasn't specified mode=something will be full
 */
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface Mode {

    /**
     * The test modes in order from least complete, to fullest set of tests
     * Use uppercase, since we toUpperCase on the value passed in for the framework
     */
    public static enum TestMode {
        //RAPID, - could be easily added later for example
        LITE,
        FULL,
        QUARANTINE,
        EXPERIMENTAL
    }

    TestMode value() default TestMode.FULL;
}
