/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ras.instrument.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation indicates that FFDC has been injected into a method by a
 * processor in order to avoid double processing. This annotation is not
 * intended to be added manually.
 * <p>
 * This annotation differs from @InjectedTrace because of retention policy.
 * We do not perform dynamic FFDC instrumentation, so we do not need annotations
 * to be visible at runtime, which reduces Java heap overhead.
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface InjectedFFDC {}
