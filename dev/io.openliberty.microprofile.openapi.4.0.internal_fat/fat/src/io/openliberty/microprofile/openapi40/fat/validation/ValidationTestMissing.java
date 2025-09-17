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
 * Validate main required elements being missing
 * <p>
 * Includes the tests from OpenAPIValidationTestThree, updated for OpenAPI v3.1
 */
@RunWith(FATRunner.class)
public class ValidationTestMissing {
    private static final String SERVER_NAME = "OpenAPIValidationServer";

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = FATSuite.defaultRepeat(SERVER_NAME);

    @BeforeClass
    public static void setup() throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "validation-missing.war")
                                   .addAsManifestResource(ValidationTestMissing.class.getPackage(), "validation-missing.yml", "openapi.yml");
        ShrinkHelper.exportDropinAppToServer(server, war, SERVER_ONLY);

        server.startServer();
    }

    @AfterClass
    public static void shutdown() throws Exception {
        server.stopServer();
    }

    @Test
    public void testEmpty() throws Exception {
        // Smallrye OpenAPI always generates an empty paths object and a minimal info object if there isn't one present
        // This is explicitly valid: https://spec.openapis.org/oas/v3.1.0.html#paths-object
        // This also means we can't actually hit the case where none of paths, components or webhooks are present
        assertNoMessage(server, "CWWKO1650E"); // Assert no validation errors
        assertNoMessage(server, "CWWKO1651W"); // Assert no validation warnings
    }

}
