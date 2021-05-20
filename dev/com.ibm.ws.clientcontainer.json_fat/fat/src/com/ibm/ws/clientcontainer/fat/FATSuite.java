/*******************************************************************************
 * Copyright (c) 2015,2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.clientcontainer.fat;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.rules.repeater.RepeatTests;

import com.ibm.websphere.simplicity.ShrinkHelper;

@RunWith(Suite.class)
@SuiteClasses({
    JsonbAppClientTest.class,
	JsonpAppClientTest.class
})
/**
 * Purpose: This suite collects and runs all known good test suites.
 */
public class FATSuite {
    public static EnterpriseArchive jsonbAppClientApp;
    public static EnterpriseArchive jsonpAppClientApp;

    @ClassRule
    public static RepeatTests r = RepeatTests.withoutModification()
                    .andWith(FeatureReplacementAction.EE8_FEATURES())
                    .andWith(new JakartaEE9Action());
    
    @BeforeClass
    public static void setupApps() throws Exception {
        // JsonbAppClient app
        JavaArchive jsonbAppClientJar = ShrinkHelper.buildJavaArchive("JsonbAppClient.jar", "com.ibm.ws.clientcontainer.jsonb.fat.*");
        jsonbAppClientApp = ShrinkWrap.create(EnterpriseArchive.class, "JsonbAppClient.ear");
        jsonbAppClientApp.addAsModule(jsonbAppClientJar);
        ShrinkHelper.addDirectory(jsonbAppClientApp, "test-applications/JsonbAppClient.ear/resources");

        // JsonpAppClient app
        JavaArchive jsonpAppClientJar = ShrinkHelper.buildJavaArchive("JsonpAppClient.jar", "com.ibm.ws.clientcontainer.jsonp.fat.*");
        jsonpAppClientApp = ShrinkWrap.create(EnterpriseArchive.class, "JsonpAppClient.ear");
        jsonpAppClientApp.addAsModule(jsonpAppClientJar);
        ShrinkHelper.addDirectory(jsonpAppClientApp, "test-applications/JsonpAppClient.ear/resources");
    }
}

