/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
 * Annotation for expressing tests should be run only if a certain system property is set.
 * An array of string values may be supplied. If multiple values are supplied, tests will
 * be run only if ALL of the conditions are satisfied (logical AND).
 * <p>
 * If a string is specified with [key]=[value] format, System.getProperty(key) must return
 * a string value that equalsIgnoreCase the [value].
 * <p>
 * If a string is specified with only a [key], if System.getProperty(key) returns any non-null
 * result, the test(s) will be skipped.
 * <p>
 * Examples:
 *
 * <pre>
 * <code>
 * // Run only if the system property "is.sle" is non-null
 * {@literal @}Test
 * {@literal @}OnlyIfSysProp("is.sle")
 * public void testSomething() {}
 *
 * // Run only if the system property "is.sle" has a value of "true"
 * {@literal @}Test
 * {@literal @}OnlyIfSysProp("is.sle=true")
 * public void testSomething() {}
 *
 * // Run only if the system property "favorite.color" has a value of "blue"
 * {@literal @}Test
 * {@literal @}OnlyIfSysProp("favorite.color=blue")
 * public void testSomething() {}
 *
 * // Run if the system property "is.monday" is non-null
 * // AND the system property "is.raining" has a value of "false"
 * {@literal @}Test
 * {@literal @}OnlyIfSysProp({"is.monday", "is.raining=false"})
 * public void testSomething() {}
 * </code>
 * </pre>
 */
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface OnlyIfSysProp {

    public static final String IS_SLE = "is.sle=true";

    /**
     * Run test only if a different database is configured
     * besides the default.
     */
    public static final String DB_Not_Default = "fat.bucket.db.type";

    // DB type system properties
    public static final String DB_DB2 = "fat.bucket.db.type=DB2";
    public static final String DB_Derby = "fat.bucket.db.type=Derby";
    public static final String DB_DerbyClient = "fat.bucket.db.type=DerbyClient";
    public static final String DB_Informix = "fat.bucket.db.type=Informix";
    public static final String DB_Oracle = "fat.bucket.db.type=Oracle";
    public static final String DB_Postgres = "fat.bucket.db.type=Postgres";
    public static final String DB_SQLServer = "fat.bucket.db.type=SQLServer";
    public static final String DB_Sybase = "fat.bucket.db.type=Sybase";

    // OS system properties
    public static final String OS_ZOS = "os.name=z/OS";

    String[] value();

}
