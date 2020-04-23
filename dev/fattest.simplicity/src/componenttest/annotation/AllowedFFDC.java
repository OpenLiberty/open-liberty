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
package componenttest.annotation;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;

/**
 * An FFDC produced for the specified exception will be ignored; it will not cause the test to fail.
 */
@Retention(RUNTIME)
public @interface AllowedFFDC {

    public static final String ALL_FFDC = "ALL_FFDC";
    public static final String ALL_REPEAT_ACTIONS = "ALL_REPEAT_ACTIONS";

    /**
     * A string array fully-qualified FFDC exception class names.
     * If no value is specified, any FFDCs are allowed.
     */
    String[] value() default { ALL_FFDC };

    /**
     * A string array of RepeatTestAction IDs to check if the allowed list of FFDCs apply to
     * that specific repeated test run
     */
    String[] repeatAction() default ALL_REPEAT_ACTIONS;
}
