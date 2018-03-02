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

import com.ibm.ws.microprofile.openapi.impl.model.security.OAuthFlowsImpl;
import com.ibm.ws.microprofile.openapi.impl.parser.OpenAPIParser;
import com.ibm.ws.microprofile.openapi.impl.parser.core.models.SwaggerParseResult;

/**
 *
 */
public class OAuthFlowParserTest {

    SwaggerParseResult result = new OpenAPIParser().readLocation("airlines_openapi.yaml", null, null);
    OAuthFlowsImpl flows = new OAuthFlowsImpl();

    @Before
    public void testForNullResult() {
        assertNotNull(result);
        assertNotNull(result.getOpenAPI());
    }

    @Test
    public void testReviewoauth2ImplicitFlow() {
        flows = (OAuthFlowsImpl) result.getOpenAPI().getComponents().getSecuritySchemes().get("reviewoauth2").getFlows();
        assertNotNull(flows);
        assertNotNull(flows.getImplicit());
        assertEquals(flows.getImplicit().getAuthorizationUrl(), "https://example.com/api/oauth/dialog");
    }

    @Test
    public void testBookingoauth2ImplicitFlow() {
        flows = (OAuthFlowsImpl) result.getOpenAPI().getComponents().getSecuritySchemes().get("bookingoauth2").getFlows();
        assertNotNull(flows);
        assertNotNull(flows.getImplicit());
        assertEquals(flows.getImplicit().getAuthorizationUrl(), "https://example.com/api/oauth/dialog");
    }
}
