/*******************************************************************************
 * Copyright (c) 2016, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.config.fat.tests;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.List;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import com.ibm.ws.fat.util.BuildShrinkWrap;
import com.ibm.ws.fat.util.LoggingTest;
import com.ibm.ws.fat.util.SharedServer;
import com.ibm.ws.fat.util.ShrinkWrapSharedServer;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;


//@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class IllegalOptionalTest extends LoggingTest {

    public static final String APP_NAME = "illegalOptional";

    @ClassRule
    public static SharedServer SHARED_SERVER = new ShrinkWrapSharedServer("IllegalOptionalServer");

    @Override
    protected SharedServer getSharedServer() {
        return SHARED_SERVER;
    }

    @BuildShrinkWrap
    public static WebArchive setUp() throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                        .addPackages(true, "com.ibm.microprofile.illegal.optional.test")
                        .addAsManifestResource(new File("test-applications/" + APP_NAME + ".war/resources/META-INF/permissions.xml"), "permissions.xml")
                        .addAsManifestResource(new File("test-applications/" + APP_NAME + ".war/resources/META-INF/microprofile-config.properties"), "microprofile-config.properties");

        return war;
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (SHARED_SERVER.getLibertyServer().isStarted()) {
            /*
             * Ignore following exception as those are expected:
             *
             * CWWKZ0002E: //TODO update this once we've chosen the final error message. 
             */
            SHARED_SERVER.getLibertyServer().stopServer("CWMCG5003E");
        }
    }

    @Test
    public void testUntypedOptionalThrowsException() throws Exception {
        List<String> errors = SHARED_SERVER.getLibertyServer()
                        .findStringsInLogs("CWMCG5003E.*BeanHolder.optionalProperty.*java.lang.IllegalArgumentException");
        assertTrue(errors.size() > 0);
    }

}
