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

package com.ibm.websphere.monitor.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The {@code ProbeAfterCall} annotation is used to mark a method as the
 * target of an <em>after method call</em> probe event. This annotation
 * must be used in conjunction with the {@link ProbeSite} to indicate the
 * set of methods that must be probed.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ProbeAfterCall {

    /**
     * A filter specification for the class or interface defining the target
     * method.
     */
    String clazz();

    /**
     * A filter specification for the name of the method being called.
     */
    String method() default "*";

    /**
     * A filter specification for the arguments of the method being called.
     */
    String args() default "*";

}
