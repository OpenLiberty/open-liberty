/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.impl.parser.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;

import com.ibm.ws.microprofile.openapi.impl.model.PathItemImpl;
import com.ibm.ws.microprofile.openapi.impl.model.headers.HeaderImpl;
import com.ibm.ws.microprofile.openapi.impl.parser.OpenAPIParser;
import com.ibm.ws.microprofile.openapi.impl.parser.core.models.SwaggerParseResult;

/**
 *
 */
public class HeaderParserTest {

    SwaggerParseResult result = new OpenAPIParser().readLocation("airlines_openapi.yaml", null, null);

    @Before
    public void testForNullResult() {
        assertNotNull(result);
        assertNotNull(result.getOpenAPI());
        assertNotNull(result.getOpenAPI().getPaths());
    }

    @Test
    public void testHeaderInRequestBody() {
        PathItemImpl path = new PathItemImpl();
        path = (PathItemImpl) result.getOpenAPI().getPaths().get("/bookings/{id}");
        assertNotNull(path);

        assertNotNull(path);
        assertNotNull(path.getPUT());
        assertNotNull(path.getPUT().getRequestBody());
        assertNotNull(path.getPUT().getRequestBody().getContent());
        assertNotNull(path.getPUT().getRequestBody().getContent().get("application/json"));
        assertNotNull(path.getPUT().getRequestBody().getContent().get("application/json").getEncoding());
        assertNotNull(path.getPUT().getRequestBody().getContent().get("application/json").getEncoding().get("profileImage"));
        assertNotNull(path.getPUT().getRequestBody().getContent().get("application/json").getEncoding().get("profileImage").getHeaders());
        assertNotNull(path.getPUT().getRequestBody().getContent().get("application/json").getEncoding().get("profileImage").getHeaders().get("X-Rate-Limit-Limit"));

        HeaderImpl header = new HeaderImpl();
        header = (HeaderImpl) path.getPUT().getRequestBody().getContent().get("application/json").getEncoding().get("profileImage").getHeaders().get("X-Rate-Limit-Limit");

        assertEquals(header.getDescription(), "The number of allowed requests in the current period");
        assertNotNull(header.getSchema());
        assertEquals(header.getSchema().getType().toString(), "integer");
        assertEquals(path.getPUT().getRequestBody().getContent().get("application/json").getEncoding().get("profileImage").getHeaders().size(), 1);
    }
}
