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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.microprofile.openapi.models.security.SecurityScheme;
import org.junit.Before;
import org.junit.Test;

import com.ibm.ws.microprofile.openapi.impl.parser.OpenAPIParser;
import com.ibm.ws.microprofile.openapi.impl.parser.core.models.SwaggerParseResult;

/**
 *
 */
public class SecuritySchemeParserTest {

    SwaggerParseResult result = new OpenAPIParser().readLocation("airlines_openapi.yaml", null, null);
    Map<String, SecurityScheme> schemes = new HashMap<String, SecurityScheme>();

    @Before
    public void testForNullResult() {
        assertNotNull(result);
        assertNotNull(result.getOpenAPI());
    }

    @Test
    public void testReviewoauth2() {
        schemes = result.getOpenAPI().getComponents().getSecuritySchemes();
        assertNotNull(schemes);
        assertNotNull(schemes.get("reviewoauth2"));
        assertNotNull(schemes.get("reviewoauth2").getFlows());
        assertEquals(schemes.get("reviewoauth2").getType().toString(), "oauth2");
        assertEquals(schemes.get("reviewoauth2").getDescription(), "authentication needed to create and delete reviews");
    }

    @Test
    public void testBookingoauth2() {
        schemes = result.getOpenAPI().getComponents().getSecuritySchemes();
        assertNotNull(schemes);
        assertNotNull(schemes.get("bookingoauth2"));
        assertNotNull(schemes.get("bookingoauth2").getFlows());
        assertEquals(schemes.get("bookingoauth2").getType().toString(), "oauth2");
        assertEquals(schemes.get("bookingoauth2").getDescription(), "authentication needed to edit bookings");
    }
}
