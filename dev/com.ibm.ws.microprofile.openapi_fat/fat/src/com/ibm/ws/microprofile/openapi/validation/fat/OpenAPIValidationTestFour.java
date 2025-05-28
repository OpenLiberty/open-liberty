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
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.utils.HttpRequest;

/**
 * A class to test the Callbacks, Reference and PathItem validator.
 * Scenarios include:
 * Reference: all possible invalid references
 * Callback: invalid fields and missing required fields
 * PathItems: duplicate path items and invalid fields
 *
 */
@RunWith(FATRunner.class)
public class OpenAPIValidationTestFour {

    @BeforeClass
    public static void setUpTest() throws Exception {
        // set mark
        ValidationSuite.server.setMarkToEndOfLog();
        // deploy app and wait for start
        WebArchive war = ShrinkWrap.create(WebArchive.class, "validationFour.war")
            .addAsManifestResource(OpenAPIValidationTestFour.class.getPackage(),
                "validationTestFour.yaml",
                "openapi.yaml");
        ValidationSuite.deployApp(war);
        // Log OpenAPI doc
        String openApiDoc = new HttpRequest(server, "/openapi").run(String.class);
        Log.info(OpenAPIValidationTestFour.class, "setUpTest", "OpenAPI doc:\n" + openApiDoc);
    }

    @AfterClass
    public static void tearDownTest() throws Exception {
        ValidationSuite.removeApp("validationFour.war");
    }

    @Test
    public void testRef() throws Exception {
        assertNotEmpty("The Reference validator should have been triggered by the invalid reference",
            server.findStringsInLogsUsingMark(
                " - Message: The \"#/invalidRef/schemas/testSchema\" reference value is not in a valid format, Location: #/paths/~1/get/responses/200/content/application~1json/schema/items",
                server.getDefaultLogFile()));
        assertNotEmpty("The Reference validator should have been triggered by the invalid reference",
            server.findStringsInLogsUsingMark(
                " - Message: The \"#/invalidRef/schemas/testSchema\" value is an invalid reference, Location: #/paths/~1/get/responses/200/content/application~1json/schema/items",
                server.getDefaultLogFile()));
        assertNotEmpty("The Reference validator should have been triggered by the invalid reference",
            server.findStringsInLogsUsingMark(
                " - Message: The \"#/components/components/schemas/Date\" reference value is not in a valid format, Location: #/paths/~1availability/get/parameters/schema",
                server.getDefaultLogFile()));
        assertNotEmpty("The Reference validator should have been triggered by the invalid reference",
            server.findStringsInLogsUsingMark(
                " - Message: The \"#/components/components/schemas/Date\" value is an invalid reference, Location: #/paths/~1availability/get/parameters/schema",
                server.getDefaultLogFile()));
        assertNotEmpty("The Reference validator should have been triggered by the invalid reference",
            server.findStringsInLogsUsingMark(
                " - Message: The \"#/components/schemas/\" reference value is not in a valid format, Location: #/paths/~1availability/get/parameters/schema",
                server.getDefaultLogFile()));
        assertNotEmpty("The Reference validator should have been triggered by the invalid reference",
            server.findStringsInLogsUsingMark(
                " - Message: The \"#/components/schemas/\" value is an invalid reference, Location: #/paths/~1availability/get/parameters/schema",
                server.getDefaultLogFile()));
        assertNotEmpty("The Reference validator should have been triggered by the invalid reference",
            server.findStringsInLogsUsingMark(
                " - Message: The \"#/components/schemas/ \" reference value is not defined within the Components Object, Location: #/paths/~1availability/get/parameters/schema",
                server.getDefaultLogFile()));
        assertNotEmpty("The Reference validator should have been triggered by the invalid reference",
            server.findStringsInLogsUsingMark(
                " - Message: The \"#/components/schemas/ \" value is an invalid reference, Location: #/paths/~1availability/get/parameters/schema",
                server.getDefaultLogFile()));
        assertNotEmpty("The Reference validator should have been triggered by the invalid reference",
            server.findStringsInLogsUsingMark(
                " - Message: The \"#/components/schemas/Airport/Cat\" reference value is not in a valid format, Location: #/paths/~1availability/get/parameters/schema",
                server.getDefaultLogFile()));
        assertNotEmpty("The Reference validator should have been triggered by the invalid reference",
            server.findStringsInLogsUsingMark(
                " - Message: The \"#/components/schemas/Airport/Cat\" value is an invalid reference, Location: #/paths/~1availability/get/parameters/schema",
                server.getDefaultLogFile()));
        assertNotEmpty("The Reference validator should have been triggered by the invalid reference",
            server.findStringsInLogsUsingMark(
                " - Message: The \"#/components/schemas/#\" reference value is not defined within the Components Object, Location: #/paths/~1availability/get/parameters/schema",
                server.getDefaultLogFile()));
        assertNotEmpty("The Reference validator should have been triggered by the invalid reference",
            server.findStringsInLogsUsingMark(
                " - Message: The \"#/components/schemas/#\" value is an invalid reference, Location: #/paths/~1availability/get/parameters/schema",
                server.getDefaultLogFile()));
        assertNotEmpty("The Reference validator should have been triggered by the invalid reference",
            server.findStringsInLogsUsingMark(
                " - Message: The \"#/\" reference value is not in a valid format, Location: #/paths/~1availability/get/parameters/schema",
                server.getDefaultLogFile()));
        assertNotEmpty("The Reference validator should have been triggered by the invalid reference",
            server.findStringsInLogsUsingMark(
                " - Message: The \"#/\" value is an invalid reference, Location: #/paths/~1availability/get/parameters/schema",
                server.getDefaultLogFile()));
        assertNotEmpty("The Reference validator should have been triggered by the invalid reference",
            server.findStringsInLogsUsingMark(
                " - Message: The \"#/components/Flight\" reference value is not in a valid format, Location: #/paths/~1availability/get/responses/200/content/applictaion~1json/schema/items",
                server.getDefaultLogFile()));
        assertNotEmpty("The Reference validator should have been triggered by the invalid reference",
            server.findStringsInLogsUsingMark(
                " - Message: The \"#/components/Flight\" value is an invalid reference, Location: #/paths/~1availability/get/responses/200/content/applictaion~1json/schema/items",
                server.getDefaultLogFile()));
        assertNotEmpty("The Reference validator should have been triggered by the invalid reference",
            server.findStringsInLogsUsingMark(
                " - Message: The \"#/components//Booking\" reference value is invalid, Location: #/paths/~1bookings/get/responses/200/content/application~1json/schema/items",
                server.getDefaultLogFile()));
        assertNotEmpty("The Reference validator should have been triggered by the invalid reference",
            server.findStringsInLogsUsingMark(
                " - Message: The \"#/components//Booking\" value is an invalid reference, Location: #/paths/~1bookings/get/responses/200/content/application~1json/schema/items",
                server.getDefaultLogFile()));
        assertNotEmpty("The Reference validator should have been triggered by the invalid reference",
            server.findStringsInLogsUsingMark(
                " - Message: The \"#/components/schemas\" reference value is not in a valid format, Location: #/paths/~1bookings/post/callbacks/getBookings//get/responses/200/content/application~1json/schema/items",
                server.getDefaultLogFile()));
        assertNotEmpty("The Reference validator should have been triggered by the invalid reference",
            server.findStringsInLogsUsingMark(
                " - Message: The \"#/components/schemas\" value is an invalid reference, Location: #/paths/~1bookings/post/callbacks/getBookings//get/responses/200/content/application~1json/schema/items",
                server.getDefaultLogFile()));
        assertNotEmpty("The Reference validator should have been triggered by the invalid reference",
            server.findStringsInLogsUsingMark(
                " - Message: The \"#/components/requestBodies/Pet\" reference value is not defined within the Components Object, Location: #/paths/~1bookings/post/requestBody",
                server.getDefaultLogFile()));
        assertNotEmpty("The Reference validator should have been triggered by the invalid reference",
            server.findStringsInLogsUsingMark(
                " - Message: The \"#/components/requestBodies/Pet\" value is an invalid reference, Location: #/paths/~1bookings/post/requestBody",
                server.getDefaultLogFile()));
        assertNotEmpty("The Reference validator should have been triggered by the invalid reference",
            server.findStringsInLogsUsingMark(
                " - Message: The \"#/components/responses/Pet\" reference value is not defined within the Components Object,*",
                server.getDefaultLogFile()));
        assertNotEmpty("The Reference validator should have been triggered by the invalid reference",
            server
                .findStringsInLogsUsingMark(
                    " - Message: The \"#/components/responses/Pet\" value is an invalid reference,*",
                    server.getDefaultLogFile()));
        assertNotEmpty("The Reference validator should have been triggered by the invalid reference",
            server.findStringsInLogsUsingMark(
                " - Message: The \"#/components/schemas/schemas\" reference value is not defined within the Components Object,*",
                server.getDefaultLogFile()));
        assertNotEmpty("The Reference validator should have been triggered by the invalid reference",
            server.findStringsInLogsUsingMark(
                " - Message: The \"#/components/schemas/schemas\" value is an invalid reference,*",
                server.getDefaultLogFile()));
        assertNotEmpty("The Reference validator should have been triggered by the invalid reference",
            server.findStringsInLogsUsingMark(
                " - Message: The \"#/components/schemas/Pet\" reference value is not defined within the Components Object,*",
                server.getDefaultLogFile()));
        assertNotEmpty("The Reference validator should have been triggered by the invalid reference",
            server.findStringsInLogsUsingMark(
                " - Message: The \"#/components/schemas/Pet\" value is an invalid reference,*",
                server.getDefaultLogFile()));
        assertNotEmpty("The Reference validator should have been triggered by the invalid reference",
            server.findStringsInLogsUsingMark(
                " - Message: The \"#/components/examples/Pet\" reference value is not defined within the Components Object, Location: #/paths/~1reviews/post/requestBody/content/application~1json/examples/review",
                server.getDefaultLogFile()));
        assertNotEmpty("The Reference validator should have been triggered by the invalid reference",
            server.findStringsInLogsUsingMark(
                " - Message: The \"#/components/examples/Pet\" value is an invalid reference, Location: #/paths/~1reviews/post/requestBody/content/application~1json/examples/review",
                server.getDefaultLogFile()));
    }

    @Test
    public void testCallbacks() throws Exception {
        assertNotEmpty("The Callback validator should have been triggered by the invalid URL",
            server.findStringsInLogsUsingMark(
                " - Message: The URL template of Callback Object is empty and is not a valid URL, Location: #/paths/~1bookings/post/callbacks/getBookings",
                server.getDefaultLogFile()));
        assertNotEmpty(
            "The Callback validator should have been triggered by the invalid substitution variables in the URL",
            server.findStringsInLogsUsingMark(
                " - Message: The Callback Object contains invalid substitution variables:*",
                server.getDefaultLogFile()));
        assertNotEmpty("The Callback validator should have been triggered by the invalid runtime expression",
            server.findStringsInLogsUsingMark(
                " - Message: The Callback Object must contain a valid runtime expression as defined in the OpenAPI Specification.*",
                server.getDefaultLogFile()));
    }

    @Test
    public void testPathItems() throws Exception {
        assertNotEmpty("The PathItem validator should have been triggered by the by ",
            server.findStringsInLogsUsingMark(" - Message: The Path Item Object must contain a valid path.",
                server.getDefaultLogFile()));
        assertNotEmpty("The PathItem validator should have been triggered by the missing parameter definition",
            server
                .findStringsInLogsUsingMark(
                    " - Message: The Path Item Object must contain a valid path. The format of the",
                    server.getDefaultLogFile()));
        assertThat("The PathItem validator should have been triggered by the by the duplicate path",
            server.findStringsInLogsUsingMark(" - Message: The Path Item Object must contain a valid path.",
                server.getDefaultLogFile()),
            hasSize(4));
    }

    /**
     * @param string
     * @param stringsInLogs
     */
    private void assertNotEmpty(String message,
                                List<String> stringsInLogs) {
        assertThat(message, stringsInLogs, not(empty()));
    }
}
