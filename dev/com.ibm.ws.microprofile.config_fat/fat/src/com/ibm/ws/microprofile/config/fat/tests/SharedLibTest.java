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


import java.util.HashMap;
import java.util.Map;
import java.io.File;

import org.jboss.shrinkwrap.api.Archive;	
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
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
public class SharedLibTest extends AbstractConfigApiTest {

    private final static String testClassName = "SharedLibTest";

    @ClassRule
    public static ShrinkWrapSharedServer SHARED_SERVER = new ShrinkWrapSharedServer("SharedLibUserServer");

    public static void buildApp() {
        return;
    }

    @BuildShrinkWrap
    public static Map<Archive,String> buildApps() {
        String APP_NAME = "sharedLibUser";

        //TODO differentiate the pacakage names for these two jars. 
        JavaArchive sharedLib_jar = ShrinkWrap.create(JavaArchive.class, "sharedLib.jar")
                        .addClass("com.ibm.ws.microprofile.archaius.impl.fat.tests.PingableSharedLibClass")
                        .addAsManifestResource(new File("test-applications/sharedLib.jar/resources/META-INF/microprofile-config.json"), "microprofile-config.json")
                        .addAsManifestResource(new File("test-applications/sharedLib.jar/resources/META-INF/microprofile-config.properties"), "microprofile-config.properties")
                        .addAsManifestResource(new File("test-applications/sharedLib.jar/resources/META-INF/microprofile-config.xml"), "microprofile-config.xml");

        JavaArchive sharedLibUser_jar = ShrinkWrap.create(JavaArchive.class, APP_NAME + ".jar")
                        .addClass("com.ibm.ws.microprofile.archaius.impl.fat.tests.SharedLibUserToStringWebPage");

        WebArchive sharedLibUser_war = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                        .addAsLibrary(sharedLibUser_jar)
                        .addAsLibrary(SharedShrinkWrapApps.getTestAppUtilsJar())
                        .addAsManifestResource(new File("test-applications/" + APP_NAME + ".war/resources/META-INF/permissions.xml"), "permissions.xml");
 
        Map<Archive,String> toReturn = new HashMap<Archive,String>();
        toReturn.put(sharedLib_jar, "publish/servers/SharedLibUserServer/shared");
        toReturn.put(sharedLibUser_war, "publish/servers/SharedLibUserServer/apps");
        return toReturn;
    }

    public SharedLibTest() {
        super("/sharedLibUser/");
    }

    @Override
    protected SharedServer getSharedServer() {
        return SHARED_SERVER;
    }

    @Rule
    public TestName testName = new TestName();

    /**
     * Tests that a config source can be loaded from within a
     * shared lib
     */
    @Test
    public void defaultsGetConfigPathSharedLib() throws Exception {
        test(testName.getMethodName());
    }
}
