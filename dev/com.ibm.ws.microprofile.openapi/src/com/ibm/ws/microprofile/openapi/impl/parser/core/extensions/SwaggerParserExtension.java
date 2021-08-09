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

package com.ibm.ws.microprofile.openapi.impl.parser.core.extensions;

import java.util.List;

import org.eclipse.microprofile.openapi.models.OpenAPI;

import com.ibm.ws.microprofile.openapi.impl.parser.core.models.AuthorizationValue;
import com.ibm.ws.microprofile.openapi.impl.parser.core.models.ParseOptions;
import com.ibm.ws.microprofile.openapi.impl.parser.core.models.SwaggerParseResult;

public interface SwaggerParserExtension {
    SwaggerParseResult readLocation(String url, List<AuthorizationValue> auth, ParseOptions options);

    SwaggerParseResult readContents(String swaggerAsString, OpenAPI startingModel, List<AuthorizationValue> auth, ParseOptions options);
}
