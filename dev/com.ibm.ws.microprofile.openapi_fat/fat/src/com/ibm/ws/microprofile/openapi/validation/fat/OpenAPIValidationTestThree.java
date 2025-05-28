/*******************************************************************************
 * Copyright (c) 2018, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.microprofile.openapi.validation.fat;

import static com.ibm.ws.microprofile.openapi.validation.fat.ValidationSuite.server;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.fasterxml.jackson.databind.JsonNode;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.microprofile.openapi.fat.utils.OpenAPIConnection;
import com.ibm.ws.microprofile.openapi.fat.utils.OpenAPITestUtil;

import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.MicroProfileActions;
import componenttest.topology.utils.HttpRequest;

/**
 * A class to test the OpenAPI validator. This class covers the scenario where the info and paths fields is missing from the OpenAPI model.
 *
 */
@RunWith(FATRunner.class)
public class OpenAPIValidationTestThree {

    @BeforeClass
    public static void setUpTest() throws Exception {
        // set mark
        ValidationSuite.server.setMarkToEndOfLog();
        // deploy app and wait for start
        WebArchive war = ShrinkWrap.create(WebArchive.class, "validationThree.war")
            .addAsManifestResource(OpenAPIValidationTestThree.class.getPackage(),
                "validationTestThree.yaml",
                "openapi.yaml");
        ValidationSuite.deployApp(war);
        // Log OpenAPI doc
        String openApiDoc = new HttpRequest(server, "/openapi").run(String.class);
        Log.info(OpenAPIValidationTestThree.class, "setUpTest", "OpenAPI doc:\n" + openApiDoc);
    }

    @AfterClass
    public static void tearDownTest() throws Exception {
        ValidationSuite.removeApp("validationThree.war");
    }

    @Test
    @SkipForRepeat({
        MicroProfileActions.MP70_EE10_ID, // paths field added automatically since SmallRye 4
        MicroProfileActions.MP70_EE11_ID,
        MicroProfileActions.MP71_EE10_ID,
        MicroProfileActions.MP71_EE11_ID,
    })
    public void testPaths() throws Exception {
        assertThat("The OpenAPI Validator should have been triggered by the missing 'paths' field",
            server.findStringsInLogsUsingMark(
                "Message: Required \"paths\" field is missing or is set to an invalid value, Location: #",
                server.getDefaultLogFile()),
            not(empty()));
    }

    @Test
    @SkipForRepeat({
        MicroProfileActions.MP41_ID,
        MicroProfileActions.MP50_ID,
        MicroProfileActions.MP60_ID,
        MicroProfileActions.MP61_ID,
        MicroProfileActions.MP70_EE10_ID,
        MicroProfileActions.MP70_EE11_ID,
        MicroProfileActions.MP71_EE10_ID,
        MicroProfileActions.MP71_EE11_ID,
    })
    public void testBlankInfo() throws Exception {
        String openapiDoc = OpenAPIConnection.openAPIDocsConnection(server, false).download();
        JsonNode openapiNode = OpenAPITestUtil.readYamlTree(openapiDoc);
        OpenAPITestUtil.checkInfo(openapiNode, "Deployed APIs", "1.0.0");
    }

    @Test
    @SkipForRepeat({
        MicroProfileActions.MP22_ID, MicroProfileActions.MP33_ID
    })
    public void testBlankInfo20() throws Exception {
        String openapiDoc = OpenAPIConnection.openAPIDocsConnection(server, false).download();
        JsonNode openapiNode = OpenAPITestUtil.readYamlTree(openapiDoc);
        OpenAPITestUtil.checkInfo(openapiNode, "Generated API", "1.0");
    }
}
