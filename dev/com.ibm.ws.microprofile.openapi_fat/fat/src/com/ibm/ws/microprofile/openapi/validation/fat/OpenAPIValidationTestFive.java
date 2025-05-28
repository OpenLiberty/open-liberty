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
 * A class to test the Tag, Discriminator, Schema and Extension validators.
 * Scenarios include
 * Tag: the name field is missing
 * Discriminator: propertyName field is missing
 * Schema: inappropriate fields for certain Schema types (min/max items or uniqueOnly fields on String type, min/max Length on array types)
 * invalid values for certain fields such as negative values for length
 * conflicting fields such as the readOnly and writeOnly fields
 *
 */
@RunWith(FATRunner.class)
public class OpenAPIValidationTestFive {

    @BeforeClass
    public static void setUpTest() throws Exception {
        // set mark
        ValidationSuite.server.setMarkToEndOfLog();
        // deploy app and wait for start
        WebArchive war = ShrinkWrap.create(WebArchive.class, "validationFive.war")
            .addAsManifestResource(OpenAPIValidationTestFive.class.getPackage(),
                "validationTestFive.yaml",
                "openapi.yaml");
        ValidationSuite.deployApp(war);
        // Log OpenAPI doc
        String openApiDoc = new HttpRequest(server, "/openapi").run(String.class);
        Log.info(OpenAPIValidationTestFive.class, "setUpTest", "OpenAPI doc:\n" + openApiDoc);

        assertThat("Validation errors were reported",
            server.findStringsInLogsUsingMark("CWWKO1650E", server.getDefaultLogFile()), not(empty()));
        assertThat("Validation warnings were reported",
            server.findStringsInLogsUsingMark("CWWKO1651W", server.getDefaultLogFile()), not(empty()));
    }

    @AfterClass
    public static void tearDownTest() throws Exception {
        ValidationSuite.removeApp("validationFive.war");
    }

    @Test
    public void testTags() throws Exception {
        assertNotEmpty("The Tag Validator should have been triggered by the missing 'name' field",
            server.findStringsInLogsUsingMark(
                " - Message: Required \"name\" field is missing or is set to an invalid value, Location: #/tags",
                server.getDefaultLogFile()));
    }

    @Test
    public void testDiscriminator() throws Exception {
        assertNotEmpty("The Discriminator validator should have been triggered by the missing 'propertyName' field",
            server.findStringsInLogsUsingMark(
                "- Message: Required \"propertyName\" field is missing or is set to an invalid value,*",
                server.getDefaultLogFile()));
    }

    @Test
    public void testSchema() throws Exception {
        assertNotEmpty("The Schema validator should have been triggered by the missing 'items' field",
            server.findStringsInLogsUsingMark(
                " - Message: The Schema Object of \"array\" type must have \"items\" property defined, Location: #/paths/~1availability/get/parameters/schema",
                server.getDefaultLogFile()));
        assertNotEmpty("The Schema validator should have been triggered by the invalid 'multipleOf' field",
            server.findStringsInLogsUsingMark(
                " - Message: The Schema Object must have the \"multipleOf\" property set to a number strictly greater than zero, Location: #/paths/~1availability/get/parameters/schema",
                server.getDefaultLogFile()));
        assertNotEmpty("The Schema validator should have been triggered by the invalid 'minItems' field",
            server.findStringsInLogsUsingMark(
                "- Message: The \"minItems\" property of the Schema Object must be greater than or equal to zero, Location: #/paths/~1availability/get/parameters/schema",
                server.getDefaultLogFile()));
        assertNotEmpty("The Schema validator should have been triggered by the invalid 'maxItems' field",
            server.findStringsInLogsUsingMark(
                " - Message: The \"maxItems\" property of the Schema Object must be greater than or equal to zero, Location: #/paths/~1availability/get/parameters/schema",
                server.getDefaultLogFile()));
        assertNotEmpty("The Schema validator should have been triggered by the invalid 'minProperties' field",
            server.findStringsInLogsUsingMark(
                " - Message: The \"minProperties\" property of the Schema Object must be greater than or equal to zero, Location: #/paths/~1availability/get/parameters/schema",
                server.getDefaultLogFile()));
        assertNotEmpty("The Schema validator should have been triggered by the invalid 'maxProperties' field",
            server.findStringsInLogsUsingMark(
                " - Message: The \"maxProperties\" property of the Schema Object must be greater than or equal to zero, Location: #/paths/~1availability/get/parameters/schema",
                server.getDefaultLogFile()));
        assertNotEmpty("The Schema validator should have been triggered by the invalid 'minItems' field",
            server.findStringsInLogsUsingMark(
                " - Message: The \"minItems\" property is not appropriate for the Schema Object of \"object\" type, Location: #/paths/~1availability/get/parameters/schema",
                server.getDefaultLogFile()));
        assertNotEmpty("The Schema validator should have been triggered by the invalid 'maxItems' field",
            server.findStringsInLogsUsingMark(
                " - Message: The \"maxItems\" property is not appropriate for the Schema Object of \"object\" type, Location: #/paths/~1availability/get/parameters/schema",
                server.getDefaultLogFile()));
    }

    private void assertNotEmpty(String message,
                                List<String> stringsInLogs) {
        assertThat(message, stringsInLogs, not(empty()));
    }
}
