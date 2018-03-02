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

import org.eclipse.microprofile.openapi.models.links.Link;
import org.junit.Before;
import org.junit.Test;

import com.ibm.ws.microprofile.openapi.impl.model.PathItemImpl;
import com.ibm.ws.microprofile.openapi.impl.model.responses.APIResponseImpl;
import com.ibm.ws.microprofile.openapi.impl.parser.OpenAPIParser;
import com.ibm.ws.microprofile.openapi.impl.parser.core.models.SwaggerParseResult;

/**
 *
 */
public class LinkParserTest {

    SwaggerParseResult result = new OpenAPIParser().readLocation("airlines_openapi.yaml", null, null);
    Map<String, Link> links = new HashMap<String, Link>();

    @Before
    public void testForNullResult() {
        assertNotNull(result);
        assertNotNull(result.getOpenAPI());
    }

    @Test
    public void testLinkInComponents() {
        links = result.getOpenAPI().getComponents().getLinks();
        assertNotNull(links);
        assertTrue(links.size() == 2);
        assertNotNull(links.get("UserName"));
        assertEquals(links.get("UserName").getDescription(), "The username corresponding to provided user id");
        assertEquals(links.get("UserName").getOperationId(), "getUserByUserName");
        assertNotNull(links.get("ReviewId"));
        assertEquals(links.get("ReviewId").getDescription(), "The id corresponding to a particular review");
        assertEquals(links.get("ReviewId").getOperationId(), "getReviewById");
    }

    @Test
    public void testLinkInResponse() {
        PathItemImpl path = new PathItemImpl();
        APIResponseImpl response = new APIResponseImpl();

        path = (PathItemImpl) result.getOpenAPI().getPaths().get("/reviews/{id}");
        assertNotNull(path);
        assertNotNull(path.getGET());
        assertNotNull(path.getGET().getResponses());
        response = (APIResponseImpl) path.getGET().getResponses().get("200");
        assertNotNull(response);
        assertNotNull(response.getLinks());
        assertTrue(response.getLinks().size() == 1);
        assertNotNull(response.getLinks().get("ReviewId"));
        assertEquals(response.getLinks().get("ReviewId").getDescription(), "The id corresponding to a particular review");
        assertEquals(response.getLinks().get("ReviewId").getOperationId(), "getReviewById");

    }
}
