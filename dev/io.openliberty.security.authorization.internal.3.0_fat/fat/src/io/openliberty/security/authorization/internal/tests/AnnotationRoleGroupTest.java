/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.security.authorization.internal.tests;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;

import componenttest.annotation.Server;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;

public class AnnotationRoleGroupTest extends AnnotationRolesTest {

    private static final String SERVER_NAME = "AnnotationGroupTest";

    @ClassRule
    public static RepeatTests repeats = RepeatTests.withoutModification() //
                    .andWith(new FeatureReplacementAction().forServers(SERVER_NAME).addFeature("appAuthorization-3.0").withID(WITH_AUTHZ_ID)) //
                    .andWith(new FeatureReplacementAction().forServers(SERVER_NAME).addFeature("usr:authzTestProvider-3.0").withID(WITH_POLICY_ID));

    @Server(SERVER_NAME)
    public static LibertyServer groupServer;

    @BeforeClass
    public static void setUp() throws Exception {
        installJaccUserFeature(groupServer);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (groupServer.isStarted()) {
            groupServer.stopServer();
        }
        uninstallJaccUserFeature(groupServer);
    }

    @Override
    protected LibertyServer getLibertyServer() {
        return groupServer;
    }
}
