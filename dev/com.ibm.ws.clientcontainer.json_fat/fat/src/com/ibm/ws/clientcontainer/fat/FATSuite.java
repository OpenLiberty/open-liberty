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
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.custom.junit.runner.AlwaysPassesTest;


@RunWith(Suite.class)
@SuiteClasses({
    AlwaysPassesTest.class,
	JsonpAppClientTest.class
})
/**
 * Purpose: This suite collects and runs all known good test suites.
 */
public class FATSuite {

    public static EnterpriseArchive jsonpAppClientApp;
    
    @BeforeClass
    public static void setupApps() throws Exception {
        
        // ApacheBvalConfig app
        JavaArchive jsonpAppClientJar = ShrinkHelper.buildJavaArchive("JsonpAppClient.jar", "com.ibm.ws.clientcontainer.jsonp.fat.*");
        jsonpAppClientApp = ShrinkWrap.create(EnterpriseArchive.class, "JsonpAppClient.ear");
        jsonpAppClientApp.addAsModule(jsonpAppClientJar);
        ShrinkHelper.addDirectory(jsonpAppClientApp, "test-applications/JsonpAppClient.ear/resources");
    
    }
}

