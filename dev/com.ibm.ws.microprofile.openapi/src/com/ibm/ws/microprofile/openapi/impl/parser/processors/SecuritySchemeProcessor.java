/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.microprofile.openapi.impl.parser.processors;

import static com.ibm.ws.microprofile.openapi.impl.parser.util.RefUtils.computeRefFormat;

import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.security.SecurityScheme;

import com.ibm.ws.microprofile.openapi.impl.parser.ResolverCache;
import com.ibm.ws.microprofile.openapi.impl.parser.models.RefFormat;

/**
 * Created by gracekarina on 23/06/17.
 */
public class SecuritySchemeProcessor {
    private final ResolverCache cache;
    private final OpenAPI openAPI;
    private final ExternalRefProcessor externalRefProcessor;

    public SecuritySchemeProcessor(ResolverCache cache, OpenAPI openAPI) {
        this.cache = cache;
        this.openAPI = openAPI;
        this.externalRefProcessor = new ExternalRefProcessor(cache, openAPI);
    }

    public SecurityScheme processSecurityScheme(SecurityScheme securityScheme) {

        if (securityScheme.getRef() != null) {
            RefFormat refFormat = computeRefFormat(securityScheme.getRef());
            String $ref = securityScheme.getRef();
            SecurityScheme newSecurityScheme = cache.loadRef($ref, refFormat, SecurityScheme.class);
            if (newSecurityScheme != null) {
                return newSecurityScheme;
            }
        }
        return securityScheme;

    }
}
