/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package javax.servlet.sip.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The @SipApplication annotation is used to create an application level 
 * annotation for a collection of SipServlets
 *  
 */
@Target (value = ElementType.PACKAGE)
@Retention (value=RetentionPolicy.RUNTIME)
public @interface SipApplication {

	
	/**
	 * 
	 */
	String name() default "";
	
	/**
	 * 
	 */
	String description() default "";

	/**
	 * 
	 */
	String displayName() default "";

	/**
	 * 
	 */
	boolean distributable() default false;

	/**
	 * 
	 */
	String largeIcon() default "";

	/**
	 * 
	 */
	String mainServlet() default "";

	/**
	 * 
	 */
	int proxyTimeout() default 180;

	/**
	 * 
	 */
	int sessionTimeout() default 3;

	/**
	 * 
	 */
	String smallIcon() default "";
}