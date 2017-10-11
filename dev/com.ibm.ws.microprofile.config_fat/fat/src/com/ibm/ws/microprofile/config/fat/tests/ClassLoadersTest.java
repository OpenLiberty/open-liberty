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
import org.jboss.shrinkwrap.api.asset.FileAsset;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.ibm.websphere.simplicity.ShrinkHelper;

import com.ibm.ws.fat.util.BuildShrinkWrap;
import com.ibm.ws.fat.util.SharedServer;
import com.ibm.ws.fat.util.ShrinkWrapSharedServer;
import com.ibm.ws.microprofile.config.fat.suite.SharedShrinkWrapApps;

import componenttest.annotation.ExpectedFFDC;

/**
 *
 */
public class ClassLoadersTest extends AbstractConfigApiTest {

    private final static String testClassName = "ClassLoadersTest";

    @ClassRule
    public static SharedServer SHARED_SERVER = new ShrinkWrapSharedServer("ClassLoadersServer");

    @BuildShrinkWrap
    public static Archive buildApp() {
        String APP_NAME = "classLoaders";

        WebArchive classLoaders_war = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                        .addPackages(true, "com.ibm.ws.microprofile.appConfig.classLoaders.test")
                        .addAsLibrary(SharedShrinkWrapApps.getTestAppUtilsJar())
                        .addAsManifestResource(new File("test-applications/" + APP_NAME + ".war/resources/META-INF/permissions.xml"), "permissions.xml")
                        .addAsManifestResource(new File("test-applications/" + APP_NAME + ".war/resources/META-INF/microprofile-config.properties"),
                                               "microprofile-config.properties")
                        .add(new FileAsset(new File("test-applications/" + APP_NAME + ".war/resources/CUSTOM-DIR/META-INF/microprofile-config.properties")),
                                       "/CUSTOM-DIR/META-INF/microprofile-config.properties")
                        .addAsWebInfResource(new File("test-applications/" + APP_NAME + ".war/resources/WEB-INF/classes/META-INF/microprofile-config.properties"),
                                             "classes/META-INF/microprofile-config.properties");

        return classLoaders_war;
    }

    public ClassLoadersTest() {
        super("/classLoaders/");
    }

    @Override
    protected SharedServer getSharedServer() {
        return SHARED_SERVER;
    }

    @Rule
    public TestName testName = new TestName();

    /**
     *
     * @throws Exception
     */
    @Test
    public void testUserClassLoaders() throws Exception {
        test(testName.getMethodName());
    }

    /**
     *
     * @throws Exception
     */
    @Test
    @ExpectedFFDC({ "java.util.ServiceConfigurationError" })
    public void testUserLoaderErrors() throws Exception {
        test(testName.getMethodName());
    }

    /**
     *
     * @throws Exception
     */
    @Test
    public void testMultiUrlResources() throws Exception {
        test(testName.getMethodName());
    }

}
