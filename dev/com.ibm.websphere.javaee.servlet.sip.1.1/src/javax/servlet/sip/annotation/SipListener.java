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
 * The @SipListener annotation allows the application developer to specify a 
 * listener without declaring it in the deployment descriptor of the application. 
 * The listener type is inferred from the interfaces implemented by the target class 
 *  
 */
@Target (value = ElementType.TYPE)
@Retention (value=RetentionPolicy.RUNTIME)
public @interface SipListener {

	/**
	 * 
	 */
    String applicationName() default "";

    /**
	 * 
	 */
    String description() default "";

}