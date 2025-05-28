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
 * Validation tests for References, Callbacks and PathItems
 * <p>
 * Ported from OpenAPIValidationTestFour and converted to run on OpenAPI v3.1
 */
@RunWith(FATRunner.class)
public class ValidationTestFour {
    private static final String SERVER_NAME = "OpenAPIValidationServer";

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = FATSuite.defaultRepeat(SERVER_NAME);

    @BeforeClass
    public static void setup() throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "validation4.war")
                                   .addAsManifestResource(ValidationTestFour.class.getPackage(), "validation4.yml", "openapi.yml");
        ShrinkHelper.exportDropinAppToServer(server, war, SERVER_ONLY);

        server.startServer();
    }

    @AfterClass
    public static void shutdown() throws Exception {
        server.stopServer("CWWKO1650E", // Validation errors found
                          "CWWKO1651W");// Validation warnings found
    }

    @Test
    public void testRef() throws Exception {

        // 3.1 Validation cases
        // Reference is null/empty (reference is null)
        // Reference is not a valid URI (URI.create does not parse it) (reference is not a valid URI)
        // Ref is within components and components is missing (reference not defined within Components)
        // Ref is to a known name within components and the object is missing (reference not defined within Components)
        // Ref is to an object but the object is of the wrong type (is an invalid reference) - only reported if no other errors

        // Main differences from 3.0:
        // We directly test whether the reference parses as a URI
        // We don't report the "is an invalid reference" error if we report another problem with the same reference
        // We don't validate references which aren't of the form #/components/<type>/<name> - 3.0 reports some of these as "not in a valid format"
        // - the spec doesn't restrict where a json pointer to point to
        // - we could do better validation here, but we can't currently navigate the model reflectively by name, so following a json pointer is not easy

        // Currently outstanding issues:
        // Unqualified schema references do not get automatically prefixed with '#/components/schemas/'
        // - https://github.com/smallrye/smallrye-open-api/issues/1987

//        assertMessage(server, " - Message: The \"#/components/schemas/\" reference value is not in a valid format, Location: #/paths/~1availability/get/parameters/schema");
//        assertMessage(server, " - Message: The \"#/components/schemas/ \" reference value is not defined within the Components Object, "
//                              + "Location: #/paths/~1availability/get/parameters/schema");
//        assertMessage(server, " - Message: The \"#/components/schemas/#\" reference value is not defined within the Components Object, "
//                              + "Location: #/paths/~1availability/get/parameters/schema");

        // 3-part reference with an invalid type
        assertMessage(server, " - Message: The \"#/components/Flight\" reference value is not defined within the Components Object, "
                              + "Location: #/paths/~1availability/get/responses/200/content/applictaion~1json/schema/items");
        // 4-part reference with an invalid type
        assertMessage(server, " - Message: The \"#/components//Booking\" reference value is not defined within the Components Object, "
                              + "Location: #/paths/~1bookings/get/responses/200/content/application~1json/schema/items");
        // 3-part reference with a valid type
        // When it's a schema, this is technically a valid reference since almost any map is a valid schema with extra fields
        // When it's not a schema, we should get an error
        assertMessage(server, " - Message: The \"#/components/schemas\" value is an invalid reference, "
                              + "Location: #/paths/~1availability/get/parameters");

        assertMessage(server, " - Message: The \"#/components/schemas/Airport/Cat\" reference value is not defined within the Components Object, "
                              + "Location: #/paths/~1availability/get/parameters/schema");
        assertMessage(server, " - Message: The \"#/components/requestBodies/Pet\" reference value is not defined within the Components Object, "
                              + "Location: #/paths/~1bookings/post/requestBody");
        assertMessage(server, " - Message: The \"#/components/responses/Pet\" reference value is not defined within the Components Object,");
        assertMessage(server, " - Message: The \"#/components/schemas/schemas\" reference value is not defined within the Components Object,");
        assertMessage(server, " - Message: The \"#/components/schemas/Pet\" reference value is not defined within the Components Object,");
        assertMessage(server, " - Message: The \"#/components/examples/Pet\" reference value is not defined within the Components Object, "
                              + "Location: #/paths/~1reviews/post/requestBody/content/application~1json/examples/review");

        // Valid references to something of the wrong type
        assertMessage(server, " - Message: The \"#/components/schemas/Flight\" value is an invalid reference, "
                              + "Location: #/paths/~1availability/get/parameters");
        assertMessage(server, "The \"http://\\{\\}/#/test\" value is not a valid URI, Location: #/paths/~1availability/get/parameters");

        // Missing pathItem reference
        assertMessage(server, " - Message: The \"#/components/pathItems/latestBookings\" reference value is not defined within the Components Object, "
                              + "Location: #/paths/~1bookings~1latest");
    }

    @Test
    public void testCallbacks() throws Exception {
        assertMessage(server, " - Message: The URL template of Callback Object is empty and is not a valid URL, Location: #/paths/~1bookings/post/callbacks/getBookings");
        assertMessage(server, " - Message: The Callback Object contains invalid substitution variables:*");
        assertMessage(server, " - Message: The Callback Object must contain a valid runtime expression as defined in the OpenAPI Specification.*");
    }

    @Test
    public void testPathItems() throws Exception {
        assertMessage(server, " - Message: The Path Item Object must contain a valid path\\. "
                              + "The \"DELETE\" operation of the \"/bookings/\\{id\\}\" path does not define a path parameter that is declared");
        assertMessage(server, " - Message: The Path Item Object must contain a valid path\\. "
                              + "The format of the \"http://localhost:9080/o\\{as3-ai\\{rl\\}ines/booking\" path is invalid");
        assertMessage(server, " - Message: The Path Item Object must contain a valid path\\. "
                              + "The \"GET\" operation from the \"/reviews/\\{airline\\}\" path defines a duplicated \"path\" parameter: \"airline\"");
        assertMessage(server, " - Message: The Path Item Object must contain a valid path\\. "
                              + "The \"PUT\" operation from the \"/reviews\" path defines one path parameter that is not declared: \"\\[id\\]\"");
        assertMessage(server, 4, " - Message: The Path Item Object must contain a valid path.");
    }

}
