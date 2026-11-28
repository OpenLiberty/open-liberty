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
package io.openliberty.restfulWS30.client.fat.namebinding;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.ws.rs.NameBinding;

/**
 * Custom name binding annotation for testing purposes.
 *
 * This annotation is used to demonstrate the customer issue in TS020587370:
 *
 * According to the Jakarta RESTful Web Services specification,
 * name binding via annotations is only supported for server-side components.
 *
 * In JAX-RS 2.1 (Apache CXF), @NameBinding annotations worked with client filters
 * even though this wasn't strictly compliant with the specification.
 *
 * In RESTful WS 3.0 (RESTEasy), @NameBinding annotations are ignored on client filters,
 * which is compliant with the specification but breaks existing code that relied on
 * the more lenient behavior of JAX-RS 2.1.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@NameBinding
public @interface CustomBinding {
}