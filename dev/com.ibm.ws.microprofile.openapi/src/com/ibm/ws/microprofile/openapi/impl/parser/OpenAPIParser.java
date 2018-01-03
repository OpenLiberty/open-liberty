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

package com.ibm.ws.microprofile.openapi.impl.parser;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;

import org.eclipse.microprofile.openapi.models.OpenAPI;

import com.ibm.ws.microprofile.openapi.impl.parser.core.extensions.SwaggerParserExtension;
import com.ibm.ws.microprofile.openapi.impl.parser.core.models.AuthorizationValue;
import com.ibm.ws.microprofile.openapi.impl.parser.core.models.ParseOptions;
import com.ibm.ws.microprofile.openapi.impl.parser.core.models.SwaggerParseResult;

public class OpenAPIParser {
    public SwaggerParseResult readLocation(String url, List<AuthorizationValue> auth, ParseOptions options) {
        SwaggerParseResult output = null;

        for (SwaggerParserExtension extension : getExtensions()) {
            output = extension.readLocation(url, auth, options);
            if (output != null && output.getOpenAPI() != null) {
                return output;
            }
        }

        return output;
    }

    public SwaggerParseResult readContents(String swaggerAsString, OpenAPI startingModel, List<AuthorizationValue> auth, ParseOptions options) {
        SwaggerParseResult output = null;

        for (SwaggerParserExtension extension : getExtensions()) {
            output = extension.readContents(swaggerAsString, startingModel, auth, options);
            if (output != null && output.getOpenAPI() != null) {
                return output;
            }
        }

        return output;
    }

    protected List<SwaggerParserExtension> getExtensions() {
        List<SwaggerParserExtension> extensions = new ArrayList<>();

        ServiceLoader<SwaggerParserExtension> loader = ServiceLoader.load(SwaggerParserExtension.class);
        Iterator<SwaggerParserExtension> itr = loader.iterator();
        while (itr.hasNext()) {
            extensions.add(itr.next());
        }
        extensions.add(0, new OpenAPIV3Parser());
        return extensions;
    }
}
