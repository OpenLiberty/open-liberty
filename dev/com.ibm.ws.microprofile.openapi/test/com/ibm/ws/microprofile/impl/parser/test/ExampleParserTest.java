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
public class ExampleParserTest {

    SwaggerParseResult result = new OpenAPIParser().readLocation("airlines_openapi.yaml", null, null);

    @Before
    public void testForNullResult() {
        assertNotNull(result);
        assertNotNull(result.getOpenAPI());
        assertNotNull(result.getOpenAPI().getPaths());
    }

    @Test
    public void testParameterExample() {
        PathItemImpl path = new PathItemImpl();
        path = (PathItemImpl) result.getOpenAPI().getPaths().get("/bookings/{id}");

        assertNotNull(path);
        assertNotNull(path.getPUT().getParameters().get(0).getExample());
        assertEquals(path.getPUT().getParameters().get(0).getExample(), "1");
    }

    @Test
    public void testSchemaPropertiesExample() {
        PathItemImpl path = new PathItemImpl();
        path = (PathItemImpl) result.getOpenAPI().getPaths().get("/bookings/{id}");
        assertNotNull(path);

        Object example = new Object();
        example = path.getPUT().getRequestBody().getContent().get("application/json").getSchema().getProperties().get("airMiles").getExample();
        assertNotNull(example);
        assertEquals(example, 32126319);

        example = path.getPUT().getRequestBody().getContent().get("application/json").getSchema().getProperties().get("seatPreference").getExample();
        assertNotNull(example);
        assertEquals(example, "window");
    }

    @Test
    public void testComponentsSchemaPropertiesExample() {
        assertNotNull(result.getOpenAPI().getComponents());
        assertNotNull(result.getOpenAPI().getComponents().getSchemas());
        assertNotNull(result.getOpenAPI().getComponents().getSchemas().get("User"));
        assertNotNull(result.getOpenAPI().getComponents().getSchemas().get("User").getProperties());
        assertNotNull(result.getOpenAPI().getComponents().getSchemas().get("User").getProperties().get("password"));

        Object example = new Object();
        example = result.getOpenAPI().getComponents().getSchemas().get("User").getProperties().get("password").getExample();
        assertNotNull(example);
        assertEquals(example, "bobSm37");
    }

}
