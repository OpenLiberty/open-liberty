/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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

package com.ibm.websphere.ras.annotation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Indicates that a non-static TraceComponent field should be ignored by the
 * Liberty bytecode instrumentation tool. This annotation is used in cases where
 * a TraceComponent must be instance-specific (non-static) for legitimate reasons,
 * such as when different instances need different trace channels or configurations.
 * <p>
 * Without this annotation, the instrumentation tool will issue a warning and fail
 * instrumentation when it encounters a non-static TraceComponent field, as the
 * standard pattern requires TraceComponent fields to be declared as
 * {@code private static final}.
 * <p>
 * Example usage:
 * <pre>
 * public class MyClass {
 *     // Dummy static TraceComponent to satisfy instrumentation
 *     private static final TraceComponent tc = Tr.register(MyClass.class);
 *     
 *     // Instance-specific TraceComponent that won't trigger instrumentation errors
 *     &#64;IgnoreNonStaticTraceComponent
 *     private final TraceComponent instanceTc;
 *     
 *     public MyClass(String channelName) {
 *         this.instanceTc = Tr.register(channelName, MyClass.class, TRACE_GROUP);
 *     }
 * }
 * </pre>
 * 
 * @see com.ibm.websphere.ras.TraceComponent
 * @see com.ibm.websphere.ras.Tr
 */
@Target({ FIELD })
@Retention(RUNTIME)
public @interface IgnoreNonStaticTraceComponent {}
