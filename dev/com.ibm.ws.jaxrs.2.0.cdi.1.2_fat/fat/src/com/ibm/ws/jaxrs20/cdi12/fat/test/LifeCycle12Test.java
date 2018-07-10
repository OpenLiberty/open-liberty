/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs20.cdi12.fat.test;

import static org.junit.Assert.assertTrue;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyFileManager;
import componenttest.topology.impl.LibertyServer;

@RunWith(FATRunner.class)
public class LifeCycle12Test extends AbstractTest {

    private static final String LIFECYCLEWAR = "lifecyclemethod.war";

    @Server("com.ibm.ws.jaxrs20.cdi12.fat.lifecyclemethod")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        appname = "lifecyclemethod";
        WebArchive app = ShrinkHelper.defaultDropinApp(server, appname, "com.ibm.ws.jaxrs20.cdi12.fat.lifecyclemethod");
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

    @Before
    public void preTest() {
        serverRef = server;
    }

    @After
    public void afterTest() {
        serverRef = null;
    }

    @Test
    public void testLifeCycleMethod() throws Exception {
        runGetMethod("/rest/lifecycle1", 200, "Resource: LifeCycleResource1", true);
        runGetMethod("/rest/lifecycle1", 200, "Resource: LifeCycleResource1", true);
        runGetMethod("/rest/lifecycle1", 200, "Resource: LifeCycleResource1", true);
        Thread.currentThread();
        Thread.sleep(1500);

        assertLibertyMessage("postConstruct method is called on com.ibm.ws.jaxrs20.cdi12.fat.lifecyclemethod.LifeCycleResource1", 3, "equal");
        assertLibertyMessage("postConstruct method is called on com.ibm.ws.jaxrs20.cdi12.fat.lifecyclemethod.LifeCycleResource2", 1, "equal");
        assertLibertyMessage("postConstruct method is called on com.ibm.ws.jaxrs20.cdi12.fat.lifecyclemethod.LifeCycleApplication", 1, "equal");
        assertLibertyMessage("preDestory method is called on com.ibm.ws.jaxrs20.cdi12.fat.lifecyclemethod.LifeCycleResource1", 3, "equal");
        assertLibertyMessage("preDestory method is called on com.ibm.ws.jaxrs20.cdi12.fat.lifecyclemethod.LifeCycleApplication", 0, "equal");
        assertLibertyMessage("preDestory method is called on com.ibm.ws.jaxrs20.cdi12.fat.lifecyclemethod.LifeCycleResource2", 0, "equal");

        Thread.sleep(1500);
        uninstallApplication();
        Thread.currentThread();
        Thread.sleep(1500);

        assertLibertyMessage("preDestory method is called on com.ibm.ws.jaxrs20.cdi12.fat.lifecyclemethod.LifeCycleApplication", 1, "equal");
        assertLibertyMessage("preDestory method is called on com.ibm.ws.jaxrs20.cdi12.fat.lifecyclemethod.LifeCycleResource2", 1, "equal");

    }

    /*
     * "remove the dropins app the way LibertyServer does (or just use the LibertyServer method)...
     * Instead of deleting the file itself, move it to a path outside of dropins.
     * That is more of an atomic operation and guarantees that for expanded applications
     * we don't get one event with some file deletes, and then another with the rest."
     */
    protected void uninstallApplication() throws Exception {
        boolean success = false;
        try {
            String dropinsFilePath = server.getServerRoot() + "/dropins/" + LIFECYCLEWAR;
            String nonDropinsFilePath = server.getServerRoot() + "/" + LIFECYCLEWAR;
            success = LibertyFileManager.renameLibertyFile(server.getMachine(), dropinsFilePath, nonDropinsFilePath);
        } catch (Exception e) {
            Log.warning(this.getClass(), e.getMessage());
        }
        assertTrue("Application lifecyclemethod does not appear to have removed.", success);
    }
}