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
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.microprofile.openapi.models.media.Schema;
import org.junit.Before;
import org.junit.Test;

import com.ibm.ws.microprofile.openapi.impl.model.PathItemImpl;
import com.ibm.ws.microprofile.openapi.impl.model.responses.APIResponseImpl;
import com.ibm.ws.microprofile.openapi.impl.parser.OpenAPIParser;
import com.ibm.ws.microprofile.openapi.impl.parser.core.models.SwaggerParseResult;

/**
 *
 */
public class MediaTypeParserTest {

    SwaggerParseResult result = new OpenAPIParser().readLocation("airlines_openapi.yaml", null, null);

    @Before
    public void testForNullResult() {
        assertNotNull(result);
        assertNotNull(result.getOpenAPI());
        assertNotNull(result.getOpenAPI().getPaths());
    }

    @Test
    public void testMediaTypeInAPIResponse() {
        PathItemImpl path = new PathItemImpl();
        APIResponseImpl response = new APIResponseImpl();

        path = (PathItemImpl) result.getOpenAPI().getPaths().get("/");
        assertNotNull(path);
        assertNotNull(path.getGET());
        assertNotNull(path.getGET().getResponses());
        assertNotNull(path.getGET().getResponses().get("202"));
        response = (APIResponseImpl) path.getGET().getResponses().get("202");
        assertNotNull(response.getContent());
        assertNotNull(response.getContent().get("applictaion/json"));
        assertNotNull(response.getContent().get("applictaion/json").getSchema());
        assertEquals(response.getContent().get("applictaion/json").getSchema().getRef(), "#/components/schemas/Flight");

        path = (PathItemImpl) result.getOpenAPI().getPaths().get("/availability");
        assertNotNull(path);
        assertNotNull(path.getGET());
        assertNotNull(path.getGET().getResponses());
        assertNotNull(path.getGET().getResponses().get("202"));
        response = (APIResponseImpl) path.getGET().getResponses().get("202");
        assertNotNull(response.getContent());
        assertNotNull(response.getContent().get("applictaion/json"));
        assertNotNull(response.getContent().get("applictaion/json").getSchema());
        assertEquals(response.getContent().get("applictaion/json").getSchema().getRef(), "#/components/schemas/Flight");
        assertNotNull(path.getGET().getResponses().get("404"));
        response = (APIResponseImpl) path.getGET().getResponses().get("404");
        assertNotNull(response.getContent());
        assertNotNull(response.getContent().get("n/a"));

        path = (PathItemImpl) result.getOpenAPI().getPaths().get("/bookings");
        assertNotNull(path);
        assertNotNull(path.getGET());
        assertNotNull(path.getGET().getResponses());
        assertNotNull(path.getGET().getResponses().get("200"));
        response = (APIResponseImpl) path.getGET().getResponses().get("200");
        assertNotNull(response.getContent());
        assertNotNull(response.getContent().get("application/json"));
        assertNotNull(response.getContent().get("application/json").getSchema());
        assertEquals(response.getContent().get("application/json").getSchema().getType().toString(), "string");

        assertNotNull(path);
        assertNotNull(path.getPOST());
        assertNotNull(path.getPOST().getResponses());
        assertNotNull(path.getPOST().getResponses().get("201"));
        response = (APIResponseImpl) path.getPOST().getResponses().get("201");
        assertNotNull(response.getContent());
        assertNotNull(response.getContent().get("application/json"));
        assertNotNull(response.getContent().get("application/json").getSchema());
        assertEquals(response.getContent().get("application/json").getSchema().getType().toString(), "string");
        assertEquals(response.getContent().get("application/json").getSchema().getDescription(), "id of the new booking");

        path = (PathItemImpl) result.getOpenAPI().getPaths().get("/bookings/{id}");
        assertNotNull(path);
        assertNotNull(path.getGET());
        assertNotNull(path.getGET().getResponses());
        assertNotNull(path.getGET().getResponses().get("200"));
        response = (APIResponseImpl) path.getGET().getResponses().get("200");
        assertNotNull(response.getContent());
        assertNotNull(response.getContent().get("application/json"));
        assertNotNull(response.getContent().get("application/json").getSchema());
        assertEquals(response.getContent().get("application/json").getSchema().getType().toString(), "array");
        assertNotNull(response.getContent().get("application/json").getSchema().getItems());
        assertEquals(response.getContent().get("application/json").getSchema().getItems().getRef(), "#/components/schemas/Booking");

        path = (PathItemImpl) result.getOpenAPI().getPaths().get("/reviews");
        assertNotNull(path);
        assertNotNull(path.getGET());
        assertNotNull(path.getGET().getResponses());
        assertNotNull(path.getGET().getResponses().get("200"));
        response = (APIResponseImpl) path.getGET().getResponses().get("200");
        assertNotNull(response.getContent());
        assertNotNull(response.getContent().get("application/json"));
        assertNotNull(response.getContent().get("application/json").getSchema());
        assertEquals(response.getContent().get("application/json").getSchema().getType().toString(), "array");
        assertNotNull(response.getContent().get("application/json").getSchema().getItems());
        assertNotNull(response.getContent().get("application/json").getSchema().getItems().getOneOf());
        assertNotNull(response.getContent().get("application/json").getSchema().getItems().getOneOf().get(0));
        assertTrue(response.getContent().get("application/json").getSchema().getItems().getOneOf().size() == 1);
        assertEquals(response.getContent().get("application/json").getSchema().getItems().getOneOf().get(0).getRef(), "#/components/schemas/Review");

        assertNotNull(path.getPOST());
        assertNotNull(path.getPOST().getResponses());
        assertNotNull(path.getPOST().getResponses().get("201"));
        response = (APIResponseImpl) path.getPOST().getResponses().get("201");
        assertNotNull(response.getContent());
        assertNotNull(response.getContent().get("application/json"));
        assertNotNull(response.getContent().get("application/json").getSchema());
        assertEquals(response.getContent().get("application/json").getSchema().getType().toString(), "string");
        assertEquals(response.getContent().get("application/json").getSchema().getDescription(), "id of the new review");

        path = (PathItemImpl) result.getOpenAPI().getPaths().get("/reviews/{id}");
        assertNotNull(path);
        assertNotNull(path.getGET());
        assertNotNull(path.getGET().getResponses());
        assertNotNull(path.getGET().getResponses().get("200"));
        response = (APIResponseImpl) path.getGET().getResponses().get("200");
        assertNotNull(response.getContent());
        assertNotNull(response.getContent().get("application/json"));
        assertNotNull(response.getContent().get("application/json").getSchema());
        assertEquals(response.getContent().get("application/json").getSchema().getRef(), "#/components/schemas/Review");

        path = (PathItemImpl) result.getOpenAPI().getPaths().get("/reviews/{user}");
        assertNotNull(path);
        assertNotNull(path.getGET());
        assertNotNull(path.getGET().getResponses());
        assertNotNull(path.getGET().getResponses().get("200"));
        response = (APIResponseImpl) path.getGET().getResponses().get("200");
        assertNotNull(response.getContent());
        assertNotNull(response.getContent().get("application/json"));
        assertNotNull(response.getContent().get("application/json").getSchema());
        assertNotNull(response.getContent().get("application/json").getSchema().getOneOf());
        assertTrue(response.getContent().get("application/json").getSchema().getOneOf().size() == 2);
        assertNotNull(response.getContent().get("application/json").getSchema().getOneOf().get(0));
        assertNotNull(response.getContent().get("application/json").getSchema().getOneOf().get(1));
        assertEquals(response.getContent().get("application/json").getSchema().getOneOf().get(0).getRef(), "#/components/schemas/Review");
        assertEquals(response.getContent().get("application/json").getSchema().getOneOf().get(1).getRef(), "#/components/schemas/User");

        path = (PathItemImpl) result.getOpenAPI().getPaths().get("/reviews/{airline}");
        assertNotNull(path);
        assertNotNull(path.getGET());
        assertNotNull(path.getGET().getResponses());
        assertNotNull(path.getGET().getResponses().get("200"));
        response = (APIResponseImpl) path.getGET().getResponses().get("200");
        assertNotNull(response.getContent());
        assertNotNull(response.getContent().get("application/json"));
        assertNotNull(response.getContent().get("application/json").getSchema());
        assertEquals(response.getContent().get("application/json").getSchema().getRef(), "#/components/schemas/Review");

        path = (PathItemImpl) result.getOpenAPI().getPaths().get("/reviews/{user}/{airlines}");
        assertNotNull(path);
        assertNotNull(path.getGET());
        assertNotNull(path.getGET().getResponses());
        assertNotNull(path.getGET().getResponses().get("200"));
        response = (APIResponseImpl) path.getGET().getResponses().get("200");
        assertNotNull(response.getContent());
        assertNotNull(response.getContent().get("application/json"));
        assertNotNull(response.getContent().get("application/json").getSchema());
        assertEquals(response.getContent().get("application/json").getSchema().getRef(), "#/components/schemas/Review");
    }

    @Test
    public void testMediaTypeInRequestBody() {

        PathItemImpl path = new PathItemImpl();
        APIResponseImpl response = new APIResponseImpl();

        path = (PathItemImpl) result.getOpenAPI().getPaths().get("/bookings");
        assertNotNull(path);
        assertNotNull(path.getPOST());
        assertNotNull(path.getPOST().getRequestBody());
        assertNotNull(path.getPOST().getRequestBody().getContent());
        assertNotNull(path.getPOST().getRequestBody().getContent().get("application/json"));
        assertNotNull(path.getPOST().getRequestBody().getContent().get("application/json").getSchema());
        assertEquals(path.getPOST().getRequestBody().getContent().get("application/json").getSchema().getRef(), "#/components/schemas/Booking");
        assertNotNull(path.getPOST().getRequestBody().getContent().get("application/json").getExamples());
        assertNotNull(path.getPOST().getRequestBody().getContent().get("application/json").getExamples().get("booking"));
        assertEquals(path.getPOST().getRequestBody().getContent().get("application/json").getExamples().get("booking").getSummary(), "External booking example");
        assertEquals(path.getPOST().getRequestBody().getContent().get("application/json").getExamples().get("booking").getExternalValue(),
                     "http://foo.bar/examples/booking-example.json");

        path = (PathItemImpl) result.getOpenAPI().getPaths().get("/bookings/{id}");
        assertNotNull(path);
        assertNotNull(path.getPUT());
        assertNotNull(path.getPUT().getRequestBody());
        assertNotNull(path.getPUT().getRequestBody().getContent());
        assertNotNull(path.getPUT().getRequestBody().getContent().get("application/json"));
        assertNotNull(path.getPUT().getRequestBody().getContent().get("application/json").getSchema());
        assertEquals(path.getPUT().getRequestBody().getContent().get("application/json").getSchema().getType().toString(), "object");
        Map<String, Schema> properties = new HashMap<String, Schema>();
        properties = path.getPUT().getRequestBody().getContent().get("application/json").getSchema().getProperties();
        assertNotNull(properties);
        for (String key : properties.keySet()) {
            assertNotNull(properties.get(key));
        }
        assertEquals(properties.get("departtureFlight").getRef(), "#/components/schemas/Flight");
        assertEquals(properties.get("returningFlight").getRef(), "#/components/schemas/Flight");
        assertEquals(properties.get("creditCard").getRef(), "#/components/schemas/CreditCard");
        assertEquals(properties.get("airMiles").getType().toString(), "string");
        assertEquals(properties.get("airMiles").getExample(), 32126319);
        assertEquals(properties.get("seatPreference").getType().toString(), "string");
        assertEquals(properties.get("seatPreference").getExample(), "window");

        path = (PathItemImpl) result.getOpenAPI().getPaths().get("/reviews");
        assertNotNull(path);
        assertNotNull(path.getPOST());
        assertNotNull(path.getPOST().getRequestBody());
        assertNotNull(path.getPOST().getRequestBody().getContent());
        assertNotNull(path.getPOST().getRequestBody().getContent().get("application/json"));
        assertNotNull(path.getPOST().getRequestBody().getContent().get("application/json").getSchema());
        assertEquals(path.getPOST().getRequestBody().getContent().get("application/json").getSchema().getRef(), "#/components/schemas/Review");

    }

    @Test
    public void testMediaTypeInParameter() {
        PathItemImpl path = new PathItemImpl();

        path = (PathItemImpl) result.getOpenAPI().getPaths().get("/reviews/{id}");
        assertNotNull(path);
        assertNotNull(path.getGET());
        assertNotNull(path.getGET().getParameters());
        assertNotNull(path.getGET().getParameters().get(0));
        assertNotNull(path.getGET().getParameters().get(0).getContent());
        assertNotNull(path.getGET().getParameters().get(0).getContent().get("*/*"));
        assertNotNull(path.getGET().getParameters().get(0).getContent().get("*/*").getSchema());
        assertEquals(path.getGET().getParameters().get(0).getContent().get("*/*").getSchema().getType().toString(), "integer");

        path = (PathItemImpl) result.getOpenAPI().getPaths().get("/reviews/{user}");
        assertNotNull(path);
        assertNotNull(path.getGET());
        assertNotNull(path.getGET().getParameters());
        assertNotNull(path.getGET().getParameters().get(0));
        assertNotNull(path.getGET().getParameters().get(0).getContent());
        assertNotNull(path.getGET().getParameters().get(0).getContent().get("*/*"));
        assertNotNull(path.getGET().getParameters().get(0).getContent().get("*/*").getSchema());
        assertEquals(path.getGET().getParameters().get(0).getContent().get("*/*").getSchema().getType().toString(), "string");
        assertNotNull(path.getGET().getParameters().get(0).getContent().get("*/*").getExamples());

        path = (PathItemImpl) result.getOpenAPI().getPaths().get("/reviews/{airline}");
        assertNotNull(path);
        assertNotNull(path.getGET());
        assertNotNull(path.getGET().getParameters());
        assertNotNull(path.getGET().getParameters().get(0));
        assertNotNull(path.getGET().getParameters().get(0).getContent());
        assertNotNull(path.getGET().getParameters().get(0).getContent().get("*/*"));
        assertNotNull(path.getGET().getParameters().get(0).getContent().get("*/*").getSchema());
        assertEquals(path.getGET().getParameters().get(0).getContent().get("*/*").getSchema().getType().toString(), "string");
        assertNotNull(path.getGET().getParameters().get(0).getContent().get("*/*").getExamples());
    }
}
