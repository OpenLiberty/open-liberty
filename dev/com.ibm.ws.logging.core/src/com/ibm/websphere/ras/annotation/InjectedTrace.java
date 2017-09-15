/*******************************************************************************
 * Copyright (c) 2010, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.websphere.ras.annotation;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * This annotation indicates that trace has been injected into a method by a
 * processor in order to avoid double processing. This annotation is not
 * intended to be added manually.
 */
@Retention(RUNTIME)
@Target({ TYPE, METHOD })
public @interface InjectedTrace {
    /**
     * The processing that has been performed. Each element should be a
     * fully-qualified class name.
     */
    String[] value() default {};
}
