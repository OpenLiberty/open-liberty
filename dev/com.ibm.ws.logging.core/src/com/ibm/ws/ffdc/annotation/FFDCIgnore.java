/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
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

package com.ibm.ws.ffdc.annotation;

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.CLASS;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Indicate which catch blocks should not be instrumented for FFDC. This
 * annotation should be added to methods that contain catch blocks for
 * exceptions that are part of the normal execution.
 * <p>
 * The exception classes called out in the annotation must match the exception
 * type <em>declared</em> on the catch block. The filtering is not based on the
 * runtime type of the exception class but on the statically declared types in
 * the code.
 * <p>
 * For example:<br>
 * 
 * <pre>
 * &#064;FFDCIgnore({ RuntimeException.class, MyException2.class })
 * public void doSomeWork() {
 *     try {
 *         someMethod();
 *     } catch (MyException e) {
 *         // This block will have FFDC instrumentation added
 *     } catch (MyException2 e) {
 *         // This block will not
 *     } catch (RuntimeException e) {
 *         // This block will not either
 *     } catch (Throwable e) {
 *         // This block will have FFDC instrumentation added
 *     }
 * }
 * </pre>
 *
 * <p>
 * <strong>Multi-catch blocks</strong>
 * <p>
 * For a multi-catch block, the <em>last</em> exception in the multi-catch should be listed in {@code @FFDCIgnore}.
 * <p>
 * <pre>
 * &#064;FFDCIgnore(InvalidStateException.class)
 * public void doSomeWork() {
 *     try {
 *         someMethod();
 *     } catch (IOException | InvalidStateException e) {
 *         // This block will not have FFDC instrumentation added
 *     } catch (Exception e) {
 *         // This block will have FFDC instrumentation added
 *     }
 * }
 * </pre>
 * 
 * <p>
 * <strong>try-with-resources limitation</strong>
 * <p>
 * When using the try-with-resources construction, the compiler will generate several {@code catch (Throwable)} blocks and
 * the FFDC instrumentation cannot currently tell these apart from catch blocks which are actually in the code.
 * <p>
 * Therefore, when using try-with-resources, you currently need to add {@code @FFDCIgnore(Throwable.class)} to avoid having 
 * these extra catch blocks instrumented.
 * <p>
 * <pre>
 * &#064;FFDCIgnore(Throwable.class)
 * public void readSomeFile(File file) {
 *     try (InputStream is = new FileInputStream(file)) {
 *        // Read the file here
 *     }
 * }
 * </pre>
 * See <a href="https://github.com/OpenLiberty/open-liberty/issues/22396" >issue 22396</a> for the issue for this limitation
 * <p>
 * See <a href="https://docs.oracle.com/javase/specs/jls/se17/html/jls-14.html#jls-14.20.3.1">JLS 14.20.3.1</a> for more information
 * about the equivalent code generated for a try-with-resources statement.
 */
@Retention(CLASS)
@Target({ METHOD, CONSTRUCTOR })
public @interface FFDCIgnore {
    Class<?>[] value();
}
