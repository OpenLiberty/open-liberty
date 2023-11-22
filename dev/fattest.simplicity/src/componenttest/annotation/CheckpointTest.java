/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
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

/**
 * Marks a test that is a checkpoint (InstantOn) test case. A test case
 * marked as a checkpoint test will get skipped when running on platforms
 * that do not support InstantOn. For example, on Windows or Mac or
 * on platforms that are not using the Semeru InstantOn JVM to run Liberty.
 * The {@code alwaysRun} value can be set to {@code true} to have the test
 * run even on platforms that do not support InstantOn.
 * <p>
 * If the property "fat.test.run.checkpoint.only" is set to {@code true} then
 * only tests annotated with {@code CheckpointTest} will run.
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE })
public @interface CheckpointTest {
    /**
     * Will cause the checkpoint test to run even when on a platform that does
     * not support InstantOn.
     *
     * @return
     */
    boolean alwaysRun() default false;
}
