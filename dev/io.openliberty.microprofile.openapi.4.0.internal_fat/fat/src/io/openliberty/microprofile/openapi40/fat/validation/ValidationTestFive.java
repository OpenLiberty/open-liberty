/*******************************************************************************
 * Copyright (c) 2024, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.openapi40.fat.validation;

import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.SERVER_ONLY;
import static io.openliberty.microprofile.openapi40.fat.validation.ValidationTestUtils.assertMessage;
import static io.openliberty.microprofile.openapi40.fat.validation.ValidationTestUtils.assertNoMessage;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import io.openliberty.microprofile.openapi40.fat.FATSuite;

/**
 * Validation tests for Tags, Discriminator, Schema and Extension
 * <p>
 * Ported from OpenAPIValidationTestFive and converted to run on OpenAPI v3.1
 * <p>
 * The validation tests for Schema in particular are quite different for OpenAPI 3.1
 */
@RunWith(FATRunner.class)
public class ValidationTestFive {
    private static final String SERVER_NAME = "OpenAPIValidationServer";

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = FATSuite.defaultRepeat(SERVER_NAME);

    @BeforeClass
    public static void setup() throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "validation5.war")
                                   .addAsManifestResource(ValidationTestFive.class.getPackage(), "validation5.yml", "openapi.yml");
        ShrinkHelper.exportDropinAppToServer(server, war, SERVER_ONLY);

        server.startServer();
    }

    @AfterClass
    public static void shutdown() throws Exception {
        server.stopServer("CWWKO1650E", // Validation errors found
                          "CWWKO1651W");// Validation warnings found
    }

    @Test
    public void testTags() throws Exception {
        assertMessage(server, "- Message: Required \"name\" field is missing or is set to an invalid value, Location: #/tags");
    }

    @Test
    public void testDiscriminator() throws Exception {
        assertMessage(server, "- Message: Required \"propertyName\" field is missing or is set to an invalid value,*");
    }

    @Test
    public void testSchema() throws Exception {
        assertMessage(server, " - Message: The Schema Object must have the \"multipleOf\" property set to a number strictly greater than zero, "
                              + "Location: #/paths/~1availability/get/parameters/schema");
        assertMessage(server, " - Message: The \"minItems\" property of the Schema Object must be greater than or equal to zero, "
                              + "Location: #/paths/~1availability/get/parameters/schema");
        assertMessage(server, " - Message: The \"maxItems\" property of the Schema Object must be greater than or equal to zero, "
                              + "Location: #/paths/~1availability/get/parameters/schema");
        assertMessage(server, " - Message: The \"minProperties\" property of the Schema Object must be greater than or equal to zero, "
                              + "Location: #/paths/~1availability/get/parameters/schema");
        assertMessage(server, " - Message: The \"maxProperties\" property of the Schema Object must be greater than or equal to zero, "
                              + "Location: #/paths/~1availability/get/parameters/schema");

        // Warnings not currently emitted for 3.1
        assertNoMessage(server, " - Message: The \"minItems\" property is not appropriate for the Schema Object of \"object\" type");
        assertNoMessage(server, " - Message: The \"maxItems\" property is not appropriate for the Schema Object of \"object\" type");

        // Dubious error reported for 3.0, not reported for 3.1
        assertNoMessage(server, " - Message: The Schema Object of \"array\" type must have \"items\" property defined");
    }

}
