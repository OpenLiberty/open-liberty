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
 * Used to annotate a method parameter of type {@link java.lang.reflect.Method}, {@link java.lang.reflect.Constructor}, {@link java.lang.reflect.Field}, or
 * {@link java.lang.reflect.Member}.
 * When the method is invoked, the {@code Member} associated with target
 * member of a field access or method call probe will be passed to the
 * method via the annotated argument.
 * <p>
 * When the argument type is not valid for the probe, a {@code null} reference will be used. This can happen, for example, when the argument
 * is declared as a {@code java.lang.reflect.Method} and the probe source
 * was actually a {@code java.lang.reflect.Constructor}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface TargetMember {

}
