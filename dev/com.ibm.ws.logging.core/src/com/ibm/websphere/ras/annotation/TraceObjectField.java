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

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * This annotation enables dynamic trace processing and specifies the static
 * trace object field (Logger or TraceComponent) that should be used. This
 * annotation is not intended to be added manually.
 */
@Retention(RUNTIME)
@Target({ TYPE })
public @interface TraceObjectField {
    /**
     * The name of the Logger or TraceComponent static field.
     */
    String fieldName() default "";

    /**
     * The descriptor of the field named by {@link #fieldName} in JVM format
     * (for example, "Lcom/ibm/websphere/ras/TraceComponent;").
     */
    String fieldDesc() default "";
}
