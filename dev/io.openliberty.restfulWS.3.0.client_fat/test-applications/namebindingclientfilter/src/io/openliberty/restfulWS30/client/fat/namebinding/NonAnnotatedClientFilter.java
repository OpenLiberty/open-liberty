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
 * Client filter without @NameBinding annotation.
 * 
 * This filter will work in both JAX-RS 2.1 (Apache CXF) and RESTful WS 3.0 (RESTEasy)
 * because it doesn't use @NameBinding annotation.
 */
public class NonAnnotatedClientFilter implements ClientRequestFilter {
    
    @Override
    public void filter(ClientRequestContext requestContext) throws IOException {
        System.out.println("NonAnnotatedClientFilter - request filter executed");
        requestContext.getHeaders().add("X-Non-Annotated-Filter", "true");
    }
}