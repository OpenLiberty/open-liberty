/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package componenttest.annotation;

import static java.lang.annotation.ElementType.METHOD;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Test methods with this annotation must specify the list of passwords
 * that will be searched by the LeakedPasswordChecker in the server's output files.
 * To verify a single password,
 * <pre>
 * &#064;CheckForLeakedPasswords("passwordToVerify")
 * &#064;Test public void myTest(....)
 * </pre>
 * To verify an encoded passwords,
 * <pre>
 * &#064;CheckForLeakedPasswords("\\{xor\\ ")
 * &#064;Test public void myTest(....)
 * </pre>
 * To verify more than one password,
 * <pre>
 * &#064;CheckForLeakedPasswords( { "passwordToVerify", "\\{xor\\}" })
 * &#064;Test public void myTest(....)
 * </pre>
 * 
 */
@Target(METHOD)
@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
public @interface CheckForLeakedPasswords {
    String[] value();
}
