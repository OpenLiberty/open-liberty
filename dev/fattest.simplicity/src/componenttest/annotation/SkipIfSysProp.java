/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
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
 * Annotation for expressing tests should be skipped if a certain system property is set.
 * An array of string values may be supplied. If multiple values are supplied, tests will
 * be skipped if ANY of the conditions are satisfied (logical OR).
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
 * // Skipped if the system property "is.sle" is non-null
 * {@literal @}Test
 * {@literal @}SkipIfSysProp("is.sle")
 * public void testSomething() {}
 *
 * // Skipped if the system property "is.sle" has a value of "true"
 * {@literal @}Test
 * {@literal @}SkipIfSysProp("is.sle=true")
 * public void testSomething() {}
 *
 * // Skipped if the system property "favorite.color" has a value of "blue"
 * {@literal @}Test
 * {@literal @}SkipIfSysProp("favorite.color=blue")
 * public void testSomething() {}
 *
 * // Skipped if the system property "is.monday" is non-null
 * // OR the system property "is.raining" has a value of "false"
 * {@literal @}Test
 * {@literal @}SkipIfSysProp({"is.monday", "is.raining=false"})
 * public void testSomething() {}
 * </code>
 * </pre>
 */
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface SkipIfSysProp {

    public static final String IS_SLE = "is.sle=true";

    // DB type system properties
    public static final String DB_DB2 = "fat.bucket.db.type=DB2";
    public static final String DB_Derby = "fat.bucket.db.type=Derby";
    public static final String DB_Informix = "fat.bucket.db.type=Informix";
    public static final String DB_Oracle = "fat.bucket.db.type=Oracle";
    public static final String DB_Postgres = "fat.bucket.db.type=Postgres";
    public static final String DB_SQLServer = "fat.bucket.db.type=SQLServer";
    public static final String DB_Sybase = "fat.bucket.db.type=Sybase";

    // OS system properties
    public static final String OS_ZOS = "os.name=z/OS";

    String[] value();

}
