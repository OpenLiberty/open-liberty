/*******************************************************************************
 * Copyright (c) 2017, 2025 IBM Corporation and others.
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

@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface TestServlet {

    /**
     * The path to the test servlet URL (i.e. <code>application/servlet</code>).
     * The hostname and port are inferred from the LibertyServer field which is being annotated.<br>
     * This value cannot be specified with <code>@TestServlet.contextRoot()</code>
     */
    String path() default "";

    /**
     * The application context root leading to the test servlet. The rest of the servlet path will be inferred
     * from the first value in <code>@WebServlet.value()</code> or <code>@WebServlet.urlPatterns()</code>. If
     * XML descriptors are being used instead of <code>@WebServlet</code>, the <code>@TestServlet.path()</code>
     * approach must be used instead.
     * This value cannot be specified with <code>@TestServlet.path()</code>
     */
    String contextRoot() default "";

    /**
     * The servlet class to scan for '@Test' annotations, which will be invoked automatically via HTTP GET request
     */
    Class<?> servlet();

    /**
     * The servlet class should only be scanned if the minimum java level is meet.
     * This will be used to avoid attempting to load annotations on the servlet class
     * that were compiled with a later version of java then the current runtime.
     *
     * Otherwise, developers should be using the {@link MinimumJavaLevel} annotation.
     *
     * Default: 0
     */
    int minJavaLevel() default 0;

}
