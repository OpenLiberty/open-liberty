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

import javax.servlet.http.HttpServlet;

@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface TestServlet {

    /**
     * The path to the test servlet URL (i.e. <code>application/servlet</code>).
     * The hostname and port are inferred from the LibertyServer field which is being annotated.
     */
    String path();

    /**
     * The servlet class.<br>
     * <b>IMPORTANT:</b> To use this annotation in a FAT, the project's build-test.xml must
     * specify either of the following in order to get the servlet class on the javac classpath:
     * <ul>
     * <li> &lt;property name="project.compile.use.classpath.source" value="true"/> </li>
     * <li> &lt;import file="../ant_build/public_imports/shrinkwrap_fat_imports.xml"/> </li>
     * </ul>
     */
    Class<? extends HttpServlet> servlet();

}
