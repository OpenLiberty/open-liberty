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
 * Test that we use the configured OpenAPI version to validate, regardless of the openapi field of a static document
 */
@RunWith(FATRunner.class)
public class ValidationTestCrossVersion {

    private static final String SERVER_NAME = "OpenAPIValidationServer";

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = FATSuite.defaultRepeat(SERVER_NAME);

    @BeforeClass
    public static void setup() throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "validationCrossVersion.war")
                                   .addAsManifestResource(ValidationTestTwo.class.getPackage(), "validation-openapi30.yml", "openapi.yml");
        ShrinkHelper.exportDropinAppToServer(server, war, SERVER_ONLY);

        server.startServer();
    }

    @AfterClass
    public static void shutdown() throws Exception {
        server.stopServer("CWWKO1650E", // Validation errors found
                          "CWWKO1651W");// Validation warnings found
    }

    @Test
    public void testValidatedAsOpenAPI31() throws Exception {
        // This validation message is only output if the 3.1 validator is used
        assertMessage(server, "Message: The type of the \"multipleOf\" property of the Schema Object must be \"number\"");
    }

}
