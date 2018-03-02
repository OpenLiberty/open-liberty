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

import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;

import com.ibm.ws.microprofile.openapi.impl.model.PathItemImpl;
import com.ibm.ws.microprofile.openapi.impl.parser.OpenAPIParser;
import com.ibm.ws.microprofile.openapi.impl.parser.core.models.SwaggerParseResult;

/**
 *
 */
public class SecurityRequirementParserTest {

    SwaggerParseResult result = new OpenAPIParser().readLocation("airlines_openapi.yaml", null, null);

    @Before
    public void testForNullResult() {
        assertNotNull(result);
        assertNotNull(result.getOpenAPI());
        assertNotNull(result.getOpenAPI().getPaths());
    }

    @Test
    public void testBookingoauth2Requirement() {
        PathItemImpl booking = new PathItemImpl();
        booking = (PathItemImpl) result.getOpenAPI().getPaths().get("/bookings/{id}");
        assertNotNull(booking);
        assertNotNull(booking.getPUT());
        assertNotNull(booking.getPUT().getSecurity());
        assertNotNull(booking.getPUT().getSecurity().get(0));
        assertNotNull(booking.getPUT().getSecurity().get(0).get("bookingoauth2"));
    }

    @Test
    public void testReviewoauth2Requirement() {
        PathItemImpl reviews = new PathItemImpl();
        reviews = (PathItemImpl) result.getOpenAPI().getPaths().get("/reviews");
        assertNotNull(reviews);
        assertNotNull(reviews.getPOST());
        assertNotNull(reviews.getPOST().getSecurity());
        assertNotNull(reviews.getPOST().getSecurity().get(0));
        assertNotNull(reviews.getPOST().getSecurity().get(0).get("reviewoauth2"));
    }
}
