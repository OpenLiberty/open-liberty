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

package com.ibm.ws.microprofile.config.fat.tests;

import java.io.File;

import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.ibm.ws.fat.util.BuildShrinkWrap;
import com.ibm.ws.fat.util.SharedServer;
import com.ibm.ws.fat.util.ShrinkWrapSharedServer;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.fat.util.ShrinkWrapSharedServer;
import com.ibm.ws.microprofile.config.fat.suite.SharedShrinkWrapApps;

/**
 *
 */
public class OrdinalsForDefaultsTest extends AbstractConfigApiTest {

    private final static String testClassName = "OrdinalsForDefaultsTest";

    @ClassRule
    public static SharedServer SHARED_SERVER = new ShrinkWrapSharedServer("OrdForDefaultsServer");

    public OrdinalsForDefaultsTest() {
        super("/ordForDefaults/");
    }

    @BuildShrinkWrap
    public static Archive buildApp() {
        String APP_NAME = "ordForDefaults";

        WebArchive ordForDefaults_war = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                        .addPackages(true, "com.ibm.ws.microprofile.appConfig.ordForDefaults.test")
                        .addAsLibrary(SharedShrinkWrapApps.getTestAppUtilsJar())
                        .addAsManifestResource(new File("test-applications/" + APP_NAME + ".war/resources/META-INF/permissions.xml"), "permissions.xml")
                        .addAsManifestResource(new File("test-applications/" + APP_NAME + ".war/resources/META-INF/microprofile-config.properties"),
                                               "microprofile-config.properties");

        return ordForDefaults_war;
    }

    @Override
    protected SharedServer getSharedServer() {
        return SHARED_SERVER;
    }

    @Rule
    public TestName testName = new TestName();

    /**
     * Tests that default properties files can tolerate having the same
     * property defined in more that on micro-profile.xxx file and behaviour
     * is as expected.
     *
     * @throws Exception
     */
    @Test
    public void defaultsMixedOrdinals() throws Exception {
        test(testName.getMethodName());
    }

    @Test
    public void defaultsOrdinalFromSource() throws Exception {
        test(testName.getMethodName());
    }
}
