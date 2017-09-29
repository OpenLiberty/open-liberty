 /*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
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
import com.ibm.ws.microprofile.config.fat.suite.SharedShrinkWrapApps;

/**
 *
 */
public class TypesTest extends AbstractConfigApiTest {

    private final static String testClassName = "TypesTest";

    @ClassRule
    public static SharedServer SHARED_SERVER = new ShrinkWrapSharedServer("TypesServer");

    @BuildShrinkWrap
    public static Archive buildApp() {
        String APP_NAME = "types";

        WebArchive types_war = ShrinkWrap.create(WebArchive.class, APP_NAME  + ".war")
                        .addPackages(true, "com.ibm.ws.microprofile.appConfig.types.test")
                        .addAsLibrary(SharedShrinkWrapApps.getTestAppUtilsJar())
                        .addAsManifestResource(new File("test-applications/" + APP_NAME + ".war/resources/META-INF/permissions.xml"), "permissions.xml");

        return types_war;

    }

    public TypesTest() {
        super("/types/");
    }

    @Override
    protected SharedServer getSharedServer() {
        return SHARED_SERVER;
    }

    @Rule
    public TestName testName = new TestName();

    /** Tests that a user can retrieve properties of type boolean */
    @Test
    public void testBooleanTypes() throws Exception {
        test(testName.getMethodName());
    }

    /** Tests that a user can retrieve properties of type Integer */
    @Test
    public void testIntegerTypes() throws Exception {
        test(testName.getMethodName());
    }
}
