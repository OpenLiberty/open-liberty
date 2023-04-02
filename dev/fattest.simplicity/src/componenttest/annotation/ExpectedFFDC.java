/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
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

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * An FFDC produced for the specified exception will be expected; The test will fail if it is not present.
 */
@Repeatable(ExpectedFFDCs.class)
@Target({ ElementType.METHOD })
@Retention(RUNTIME)
public @interface ExpectedFFDC {

    public static final String ALL_REPEAT_ACTIONS = "ALL_REPEAT_ACTIONS";

    /**
     * A string array fully-qualified FFDC exception class names.
     */
    String[] value();

    /**
     * A string array of RepeatTestAction IDs to check if the expected list of FFDCs apply to
     * that specific repeated test run
     */
    String[] repeatAction() default ALL_REPEAT_ACTIONS;
}
