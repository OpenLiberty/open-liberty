/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.security.jakartasec.fat.tests;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import io.openliberty.security.jakartasec.fat.utils.AppValidator;
import multiple.ham.common.MultipleHAMProtectedResource;
import multiple.ham.custom.hams.CustomHAMOneOperator;
import multiple.ham.custom.hams.CustomHAMTwoAdmin;
import multiple.ham.inbuilt.MultipleHAMQualifiersApplication;

/**
 * Tests appSecurity-6.0
 *
 * Tests that we see CWWKS2610E: Missing a custom handler when using qualified HttpAuthenticationMechanisms when qualifiers are found without a custom handler
 */
@RunWith(FATRunner.class)
@Mode(TestMode.LITE)
@AllowedFFDC
public class MissingCustomHandlerTests extends BaseJakartaSecurity40Test {

    private static final Class<?> c = MissingCustomHandlerTests.class;

    public static final String APP_NAME = "MultipleHAMWithoutHandlerApp";

    @Server(MULTIPLE_HAM_SERVER_NAME)
    public static LibertyServer server;

    @Override
    protected Class<?> getTestClass() {
        return c;
    }

    @Override
    protected LibertyServer getServer() {
        return server;
    }

    @BeforeClass
    public static void setUp() throws Exception {
        server.startServer();
    }

    @AfterClass
    public static void shutdown() throws Exception {
        server.stopServer();
    }

    /*
     * Tests multiple inbuilt HAMs with qualifiers and a custom HAM with a qualifier without a handler
     */
    @Test
    public void testCustomHAMAndInBuiltWithQualifierWithoutHandler() {
        AppValidator.validateAppOn(server).withClass(MultipleHAMQualifiersApplication.class).withClass(CustomHAMOneOperator.class).withClass(CustomHAMTwoAdmin.class).withClass(MultipleHAMProtectedResource.class).withPackage("multiple.ham.common.qualifiers").failsWith("CWWKS2610E|CWWKZ0002E").run();
    }

    /*
     * Tests in-built HAM's with qualifiers without a handler
     */
    @Test
    public void testMultipleInBuiltHAMWithQualifierWithoutHandler() {
        AppValidator.validateAppOn(server).withClass(MultipleHAMQualifiersApplication.class).withClass(MultipleHAMProtectedResource.class).withPackage("multiple.ham.common.qualifiers").failsWith("CWWKS2610E|CWWKZ0002E").run();
    }

}
