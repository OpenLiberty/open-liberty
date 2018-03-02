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
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;

import com.ibm.ws.microprofile.openapi.impl.parser.OpenAPIParser;
import com.ibm.ws.microprofile.openapi.impl.parser.core.models.SwaggerParseResult;

/**
 *
 */
public class OpenAPIParserTest {

    SwaggerParseResult result = new OpenAPIParser().readLocation("airlines_openapi.yaml", null, null);

    @Before
    public void testForNullResult() {
        assertNotNull(result);
        assertNotNull(result.getOpenAPI());
    }

    @Test
    public void testOpenAPI() {
        assertEquals(result.getOpenAPI().getOpenapi(), "3.0.0");
    }

    @Test
    public void testInfo() {
        //only checking that info is not null, the rest will be checked in the test for Info object
        assertNotNull(result.getOpenAPI().getInfo());
    }

    @Test
    public void testServers() {
        //only checking that servers is not null, the rest will be checked in the test for Servers object
        assertNotNull(result.getOpenAPI().getServers());
    }

    @Test
    public void testPaths() {
        //only checking that paths is not null, the rest will be checked in the test for Paths object
        assertNotNull(result.getOpenAPI().getPaths());
    }

    @Test
    public void testComponents() {
        //only checking that components is not null, the rest will be checked in the test for Components object
        assertNotNull(result.getOpenAPI().getComponents());
    }

    @Test
    public void testSecurity() {
        //only checking that security is null, the rest will be checked in the test for SecurityRequirements object
        assertNull(result.getOpenAPI().getSecurity());
    }

    @Test
    public void testTags() {
        //only checking that tags is not null, the rest will be checked in the test for Tag object
        assertNotNull(result.getOpenAPI().getTags());
    }

    @Test
    public void testExternalDocs() {
        //only checking that externalDocs is not null, the rest will be checked in the test for ExternalDocumentation object
        assertNotNull(result.getOpenAPI().getExternalDocs());
    }
}
