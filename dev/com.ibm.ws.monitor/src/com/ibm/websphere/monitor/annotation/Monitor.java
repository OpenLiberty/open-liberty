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

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Monitor {

    /**
     * The name of the monitor. If not specified, the name of the class
     * implementing the monitor will be used as the name.
     */
    String name() default "";

    /**
     * The names of the groups that this monitor is a member of. Groups may
     * be used to enable or disable a set of related monitors.
     */
    String[] group() default {};

    /**
     * Indicates whether or not the monitor is enabled when the host bundle
     * starts. A monitor is enabled by default.
     */
    boolean enabled() default true;

}
