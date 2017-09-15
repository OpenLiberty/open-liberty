/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
 * Annotation for expressing tests should only run when the java level is above the given level.
 * The argument is a double, such as 1.6 or 1.7.<br>
 * <b>Note: If you want to skip an entire FAT on a certain java level, you can specify the following
 * property in your build-test.xml instead of using this annotation on every single test class:</b><br>
 * <code>
 * &lt;property name="minimum.java.level.for.test.execution" value="1.7"/>
 * </code>
 */
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface MinimumJavaLevel {

    double javaLevel();

    /**
     * Deprecated: The synthetic test is no longer necessary.
     */
    @Deprecated
    boolean runSyntheticTest() default false;

}
