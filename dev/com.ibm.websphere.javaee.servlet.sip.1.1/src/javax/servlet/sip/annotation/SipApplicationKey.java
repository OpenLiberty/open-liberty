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
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * The @SipApplicationKey annotation is used when the application wants to associate 
 * the incoming request (and SipSession) with a certain SipApplicationSession. 
 * 
 * The method annotated with the @SipApplicationKey annotation MUST have the 
 * following restrictions:
 * 
 * 1. It MUST be public and static
 * 2. It MUST return a String
 * 3. It MUST have a single argument of type SipServletRequest
 * 4. It MUST not modify the SipServletRequest passed in 
 * 
 * If the annotated method signature does not comply with the first three rules, 
 * deployment of such an application MUST fail. 
 *
 */
@Target (value = ElementType.METHOD)
@Retention (value = RetentionPolicy.RUNTIME)
@Inherited
public @interface SipApplicationKey {
	
	
	/**
	 * 
	 */
	String applicationName() default "";
	
}