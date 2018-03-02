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
import com.ibm.ws.microprofile.openapi.impl.parser.OpenAPIParser;
import com.ibm.ws.microprofile.openapi.impl.parser.core.models.SwaggerParseResult;

/**
 *
 */
public class SchemaParserTest {

    SwaggerParseResult result = new OpenAPIParser().readLocation("airlines_openapi.yaml", null, null);

    @Before
    public void testForNullResult() {
        assertNotNull(result);
        assertNotNull(result.getOpenAPI());
        assertNotNull(result.getOpenAPI().getPaths());
    }

    @Test
    public void testResponseContentSchema() {
        PathItemImpl path = new PathItemImpl();
        path = (PathItemImpl) result.getOpenAPI().getPaths().get("/");
        assertNotNull(path);
        assertNotNull(path.getGET());
        assertNotNull(path.getGET().getResponses());
        assertNotNull(path.getGET().getResponses().get("202"));
        assertNotNull(path.getGET().getResponses().get("202").getContent());
        assertNotNull(path.getGET().getResponses().get("202").getContent().get("applictaion/json"));
        assertNotNull(path.getGET().getResponses().get("202").getContent().get("applictaion/json").getSchema());
    }

    @Test
    public void testParameterSchema() {
        PathItemImpl path = new PathItemImpl();
        path = (PathItemImpl) result.getOpenAPI().getPaths().get("/availability");
        assertNotNull(path);
        assertNotNull(path.getGET());
        assertNotNull(path.getGET().getParameters());
        assertNotNull(path.getGET().getParameters().get(0).getSchema());
        assertEquals(path.getGET().getParameters().get(0).getSchema().getType().toString(), "string");
    }

    @Test
    public void testRequestBodySchema() {
        PathItemImpl path = new PathItemImpl();
        path = (PathItemImpl) result.getOpenAPI().getPaths().get("/bookings");
        assertNotNull(path);
        assertNotNull(path.getPOST());
        assertNotNull(path.getPOST().getRequestBody());
        assertNotNull(path.getPOST().getRequestBody().getContent().get("application/json").getSchema());
        assertEquals(path.getPOST().getRequestBody().getContent().get("application/json").getSchema().getRef(), "#/components/schemas/Booking");
    }
}
