/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
 * &#064;FFDCIgnore({ InterruptedException.class, NameAlreadyBoundException.class })
 * public void doSomeWork()
 * {
 * try
 * {
 * Thread.sleep(900);
 * context.bind(&quot;context/binding&quot;, &quot;hello&quot;);
 * }
 * catch (InterruptedException ie)
 * {
 * // Nothing to do. No need for FFDC.
 * }
 * catch (NameAlreadyBoundException nabe)
 * {
 * // I guess it's already in naming. No need for FFDC.
 * }
 * catch (Throwable t)
 * {
 * // Don't know what's going on here. I'd better get FFDC.
 * }
 * }
 * </pre>
 * 
 * will omit FFDC for the first two catch blocks but will add it to the third.
 */
@Retention(CLASS)
@Target({ METHOD, CONSTRUCTOR })
public @interface FFDCIgnore {
    Class<?>[] value();
}
