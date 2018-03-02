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

import org.junit.Before;
import org.junit.Test;

import com.ibm.ws.microprofile.openapi.impl.model.PathItemImpl;
import com.ibm.ws.microprofile.openapi.impl.model.responses.APIResponseImpl;
import com.ibm.ws.microprofile.openapi.impl.parser.OpenAPIParser;
import com.ibm.ws.microprofile.openapi.impl.parser.core.models.SwaggerParseResult;

/**
 *
 */
public class APIResponsesParserTest {

    SwaggerParseResult result = new OpenAPIParser().readLocation("airlines_openapi.yaml", null, null);

    @Before
    public void testForNullResult() {
        assertNotNull(result);
        assertNotNull(result.getOpenAPI());
        assertNotNull(result.getOpenAPI().getPaths());
    }

    @Test
    public void testAPIResponses() {
        PathItemImpl path = new PathItemImpl();
        APIResponseImpl response = new APIResponseImpl();

        path = (PathItemImpl) result.getOpenAPI().getPaths().get("/");
        assertNotNull(path);
        assertNotNull(path.getGET());
        assertNotNull(path.getGET().getResponses());
        assertTrue(path.getGET().getResponses().size() == 1);
        assertNotNull(path.getGET().getResponses().get("202"));
        response = (APIResponseImpl) path.getGET().getResponses().get("202");
        assertEquals(response.getDescription(), "failed operation");
        assertNotNull(response.getContent());

        path = (PathItemImpl) result.getOpenAPI().getPaths().get("/availability");
        assertNotNull(path);
        assertNotNull(path.getGET());
        assertNotNull(path.getGET().getResponses());
        assertTrue(path.getGET().getResponses().size() == 2);
        assertNotNull(path.getGET().getResponses().get("202"));
        response = (APIResponseImpl) path.getGET().getResponses().get("202");
        assertEquals(response.getDescription(), "failed operation");
        assertNotNull(response.getContent());
        assertNotNull(path.getGET().getResponses().get("404"));
        response = (APIResponseImpl) path.getGET().getResponses().get("404");
        assertEquals(response.getDescription(), "No available flights found");
        assertNotNull(response.getContent());

        path = (PathItemImpl) result.getOpenAPI().getPaths().get("/bookings");
        assertNotNull(path);
        assertNotNull(path.getGET());
        assertNotNull(path.getGET().getResponses());
        assertTrue(path.getGET().getResponses().size() == 2);
        assertNotNull(path.getGET().getResponses().get("200"));
        response = (APIResponseImpl) path.getGET().getResponses().get("200");
        assertEquals(response.getDescription(), "Bookings retrieved");
        assertNotNull(response.getContent());
        assertNotNull(path.getGET().getResponses().get("404"));
        response = (APIResponseImpl) path.getGET().getResponses().get("404");
        assertEquals(response.getDescription(), "No bookings found for the user.");

        assertNotNull(path);
        assertNotNull(path.getPOST());
        assertNotNull(path.getPOST().getResponses());
        assertTrue(path.getPOST().getResponses().size() == 1);
        assertNotNull(path.getPOST().getResponses().get("201"));
        response = (APIResponseImpl) path.getPOST().getResponses().get("201");
        assertEquals(response.getDescription(), "Booking created");
        assertNotNull(response.getContent());

        path = (PathItemImpl) result.getOpenAPI().getPaths().get("/bookings/{id}");
        assertNotNull(path);
        assertNotNull(path.getGET());
        assertNotNull(path.getGET().getResponses());
        assertTrue(path.getGET().getResponses().size() == 2);
        assertNotNull(path.getGET().getResponses().get("200"));
        response = (APIResponseImpl) path.getGET().getResponses().get("200");
        assertEquals(response.getDescription(), "booking retrieved");
        assertNotNull(response.getContent());
        assertNotNull(path.getGET().getResponses().get("404"));
        response = (APIResponseImpl) path.getGET().getResponses().get("404");
        assertEquals(response.getDescription(), "No bookings found for the user.");

        assertNotNull(path.getPUT());
        assertNotNull(path.getPUT().getResponses());
        assertTrue(path.getPUT().getResponses().size() == 2);
        assertNotNull(path.getPUT().getResponses().get("200"));
        response = (APIResponseImpl) path.getPUT().getResponses().get("200");
        assertEquals(response.getDescription(), "Booking updated");
        assertNotNull(path.getPUT().getResponses().get("404"));
        response = (APIResponseImpl) path.getPUT().getResponses().get("404");
        assertEquals(response.getDescription(), "Booking not found");

        assertNotNull(path.getDELETE());
        assertNotNull(path.getDELETE().getResponses());
        assertTrue(path.getDELETE().getResponses().size() == 2);
        assertNotNull(path.getDELETE().getResponses().get("200"));
        response = (APIResponseImpl) path.getDELETE().getResponses().get("200");
        assertEquals(response.getDescription(), "Booking deleted successfully.");
        assertNotNull(path.getDELETE().getResponses().get("404"));
        response = (APIResponseImpl) path.getDELETE().getResponses().get("404");
        assertEquals(response.getDescription(), "Booking not found.");

        path = (PathItemImpl) result.getOpenAPI().getPaths().get("/reviews");
        assertNotNull(path);
        assertNotNull(path.getGET());
        assertNotNull(path.getGET().getResponses());
        assertTrue(path.getGET().getResponses().size() == 1);
        assertNotNull(path.getGET().getResponses().get("200"));
        response = (APIResponseImpl) path.getGET().getResponses().get("200");
        assertEquals(response.getDescription(), "successful operation");
        assertNotNull(response.getContent());

        assertNotNull(path.getPOST());
        assertNotNull(path.getPOST().getResponses());
        assertTrue(path.getPOST().getResponses().size() == 1);
        assertNotNull(path.getPOST().getResponses().get("201"));
        response = (APIResponseImpl) path.getPOST().getResponses().get("201");
        assertEquals(response.getDescription(), "review created");
        assertNotNull(response.getContent());

        path = (PathItemImpl) result.getOpenAPI().getPaths().get("/reviews/{id}");
        assertNotNull(path);
        assertNotNull(path.getGET());
        assertNotNull(path.getGET().getResponses());
        assertTrue(path.getGET().getResponses().size() == 2);
        assertNotNull(path.getGET().getResponses().get("200"));
        response = (APIResponseImpl) path.getGET().getResponses().get("200");
        assertEquals(response.getDescription(), "Review retrieved");
        assertNotNull(response.getContent());
        assertNotNull(path.getGET().getResponses().get("404"));
        response = (APIResponseImpl) path.getGET().getResponses().get("404");
        assertEquals(response.getDescription(), "Review not found");

        assertNotNull(path.getDELETE());
        assertNotNull(path.getDELETE().getResponses());
        assertTrue(path.getDELETE().getResponses().size() == 2);
        assertNotNull(path.getDELETE().getResponses().get("200"));
        response = (APIResponseImpl) path.getDELETE().getResponses().get("200");
        assertEquals(response.getDescription(), "Review deleted");
        assertNotNull(path.getDELETE().getResponses().get("404"));
        response = (APIResponseImpl) path.getDELETE().getResponses().get("404");
        assertEquals(response.getDescription(), "Review not found");

        path = (PathItemImpl) result.getOpenAPI().getPaths().get("/reviews/{user}");
        assertNotNull(path);
        assertNotNull(path.getGET());
        assertNotNull(path.getGET().getResponses());
        assertTrue(path.getGET().getResponses().size() == 2);
        assertNotNull(path.getGET().getResponses().get("200"));
        response = (APIResponseImpl) path.getGET().getResponses().get("200");
        assertEquals(response.getDescription(), "Review(s) retrieved");
        assertNotNull(response.getContent());
        assertNotNull(path.getGET().getResponses().get("404"));
        response = (APIResponseImpl) path.getGET().getResponses().get("404");
        assertEquals(response.getDescription(), "Review(s) not found");

        path = (PathItemImpl) result.getOpenAPI().getPaths().get("/reviews/{airline}");
        assertNotNull(path);
        assertNotNull(path.getGET());
        assertNotNull(path.getGET().getResponses());
        assertTrue(path.getGET().getResponses().size() == 2);
        assertNotNull(path.getGET().getResponses().get("200"));
        response = (APIResponseImpl) path.getGET().getResponses().get("200");
        assertEquals(response.getDescription(), "Review(s) retrieved");
        assertNotNull(response.getContent());
        assertNotNull(path.getGET().getResponses().get("404"));
        response = (APIResponseImpl) path.getGET().getResponses().get("404");
        assertEquals(response.getDescription(), "Review(s) not found");

        path = (PathItemImpl) result.getOpenAPI().getPaths().get("/reviews/{user}/{airlines}");
        assertNotNull(path);
        assertNotNull(path.getGET());
        assertNotNull(path.getGET().getResponses());
        assertTrue(path.getGET().getResponses().size() == 2);
        assertNotNull(path.getGET().getResponses().get("200"));
        response = (APIResponseImpl) path.getGET().getResponses().get("200");
        assertEquals(response.getDescription(), "Review(s) retrieved");
        assertNotNull(response.getContent());
        assertNotNull(path.getGET().getResponses().get("404"));
        response = (APIResponseImpl) path.getGET().getResponses().get("404");
        assertEquals(response.getDescription(), "Review(s) not found");
    }
}
