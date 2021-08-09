/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URI;
import java.util.Map;
import java.util.HashMap;

import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import test.utils.Utils;

import test.common.SharedLocationManager;
import test.common.SharedOutputManager;

import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.kernel.service.location.WsResource;

/**
 *
 */
@RunWith(JMock.class)
public class SharedLocationTest {
    /**
     * Test data directory: note the space! always test paths with spaces. Dratted
     * windows.
     */
    private static final String testClassesDir = System.getProperty("test.classesDir", "bin_test");
    public static final String TEST_DATA_DIR = testClassesDir + "/test data";

    static SharedOutputManager outputMgr;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        // make stdout/stderr "quiet"-- no output will show up for test
        // unless one of the copy methods or documentThrowable is called
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.logTo(Utils.TEST_DATA);
        outputMgr.captureStreams();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        // Make stdout and stderr "normal"
        outputMgr.restoreStreams();
    }

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {}

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
        SharedLocationManager.resetWsLocationAdmin();
    }
	
    public static final String TEST_SERVER = "com.ibm.ws.kernel.service_test";

    /**
     * Test method for {@link test.common.SharedLocationManager#createImageLocations(java.lang.String)} .
     */
    @Test
    public void testCreateImageLocationsString() {
        final String m = "testCreateImageLocationsString";
		final String ROOT_DIR =  testClassesDir + "/test data/wlp/usr";
        try {
			SharedLocationManager.createLocations(ROOT_DIR, TEST_SERVER);
			WsLocationAdmin locSvc = (WsLocationAdmin) SharedLocationManager.getLocationInstance();

            WsResource cfgRoot = locSvc.resolveResource("server.xml");
            assertTrue(cfgRoot.exists());

            URI uri = cfgRoot.toExternalURI();
            WsResource serverCfgRes = locSvc.getServerResource("server.xml");
            File serverCfg = new File(serverCfgRes.toExternalURI());

            assertEquals("server.xml parent should be the test profile", uri, serverCfg.toURI().normalize());

            WsResource bootRes = locSvc.getServerResource("bootstrap.properties");
            File bootProps = new File(bootRes.toExternalURI());
            assertTrue("bootstrap.properties should be found in server directory", bootProps.exists());

            WsResource server = locSvc.getServerResource(null);
            File serverDir = new File(server.toExternalURI());

            File serversDir = serverDir.getParentFile();
            File usrDir = serversDir.getParentFile();
            File libertyDir = usrDir.getParentFile();

            assertEquals("bootstrap.properties parent should be the test profile", "com.ibm.ws.kernel.service_test", bootProps.getParentFile().getName());
            assertEquals("parent of bootstrap.properties should be server directory", serverDir, bootProps.getParentFile());
            assertEquals("server parent should be servers", "servers", serversDir.getName());
            assertEquals("parent of servers dir should be usr", "usr", usrDir.getName());
            //assertEquals("parent of usr dir should be '" + installDir + "'", installDir, libertyDir.getName());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    final Mockery context = new org.jmock.integration.junit4.JUnit4Mockery();

    @Test
    public void testCreateDefaultLocations() {
        final String m = "testCreateDefaultLocations";

        File tmpRoot = new File(TEST_DATA_DIR, "fullDir");

        try {
            tmpRoot.mkdirs();
            SharedLocationManager.createDefaultLocations(tmpRoot.getAbsolutePath());
            System.out.println(SharedLocationManager.debugConfiguredLocations());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        } finally {
            Utils.recursiveClean(tmpRoot);
        }
    }
}
