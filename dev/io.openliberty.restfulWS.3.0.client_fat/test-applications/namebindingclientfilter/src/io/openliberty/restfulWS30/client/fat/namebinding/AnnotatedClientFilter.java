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

import java.io.IOException;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;

/**
 * Client filter with @NameBinding annotation.
 *
 * This filter reproduces the customer issue in TS020587370:
 * - It will NOT work in RESTful WS 3.0 (RESTEasy) because @NameBinding
 *   is only supported for server-side components according to the specification.
 *
 * - However, it worked in JAX-RS 2.1 (Apache CXF) because CXF was more lenient
 *   and allowed @NameBinding to work with client filters as well.
 *
 * The customer's code was likely using client filters with @NameBinding annotations
 * that worked in JAX-RS 2.1 but stopped working after migrating to RESTful WS 3.0.
 */
@CustomBinding
public class AnnotatedClientFilter implements ClientRequestFilter {
    
    @Override
    public void filter(ClientRequestContext requestContext) throws IOException {
        System.out.println("AnnotatedClientFilter - request filter executed");
        requestContext.getHeaders().add("X-Annotated-Filter", "true");
    }
}