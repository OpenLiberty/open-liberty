/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
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
package javax.servlet.sip.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The @SipServlet annotation allows for the SipServlet metadata to 
 * be declared without having to create the deployment descriptor. 
 */
@Target (value = ElementType.TYPE)
@Retention (value=RetentionPolicy.RUNTIME)
@Inherited
public @interface SipServlet {

	/**
	 * 
	 */
	String applicationName() default "";

	/**
	 * 
	 */
	String description() default "";

	/**
	 * 
	 */
	int    loadOnStartup()   default -1;

	/**
	 * 
	 */
	String name()            default "";
}