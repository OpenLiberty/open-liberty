/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsonb.fat;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@RunWith(Suite.class)
@SuiteClasses({
                JSONBTest.class,
                JSONBInAppTest.class,
                JSONBContainerTest.class,
                JSONPContainerTest.class,
                JsonUserFeatureTest.class
})
public class FATSuite {
    @ClassRule
    public static RepeatTests r = RepeatTests.withoutModification() // run all tests as-is (e.g. EE8 features)
                    .andWith(new JakartaEE9Action()); // run all tests again with EE9 features+packages

    public static final String PROVIDER_YASSON = "org.eclipse.yasson.JsonBindingProvider";
    public static final String PROVIDER_JOHNZON = "org.apache.johnzon.jsonb.JohnzonProvider";
    public static final String PROVIDER_GLASSFISH_JSONP = "org.glassfish.json.JsonProviderImpl";
    public static final String PROVIDER_JOHNZON_JSONP = "org.apache.johnzon.core.JsonProviderImpl";

    public static final String JSONB_APP = "jsonbapp";
    public static final String CDI_APP = "jsonbCDIapp";

    private static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.jsonb.fat");

    public static void jsonbApp(LibertyServer server) throws Exception {
        ShrinkHelper.defaultApp(server, JSONB_APP, "web.jsonbtest");
    }

    public static void cdiApp(LibertyServer server) throws Exception {
        ShrinkHelper.defaultApp(server, CDI_APP, "jsonb.cdi.web");
    }

    @BeforeClass
    public static void beforeSuite() throws Exception {
        // Install user features
        server.copyFileToLibertyInstallRoot("usr/extension/lib/features/", "features/testFeatureUsingJsonp-1.1.mf");
        server.copyFileToLibertyInstallRoot("usr/extension/lib/features/", "features/testFeatureUsingJsonb-1.0.mf");

        // Install bundles for user features
        server.copyFileToLibertyInstallRoot("usr/extension/lib/", "bundles/test.jsonp.bundle.jar");
        server.copyFileToLibertyInstallRoot("usr/extension/lib/", "bundles/test.jsonb.bundle.jar");
    }

    @AfterClass
    public static void afterSuite() throws Exception {
        // Remove the user extension added during setup
        server.deleteDirectoryFromLibertyInstallRoot("usr/extension/");
    }

}