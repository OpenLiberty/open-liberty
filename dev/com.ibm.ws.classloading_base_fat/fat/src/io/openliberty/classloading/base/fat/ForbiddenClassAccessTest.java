/*******************************************************************************
 * Copyright (c) 2024, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.classloading.base.fat;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.URL;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.HttpUtils;

/**
 * Tests for loading forbidden classes.
 * All tests follow the same format. A unique URL for each test returns either
 * success or a message containing the test failure
 */
public class ForbiddenClassAccessTest {

    private static final String SUCCESS_MESSAGE = "Success";

    private static final LibertyServer _server = LibertyServerFactory.getLibertyServer("forbidden");

    private static final Class<?> c = ForbiddenClassAccessTest.class;

    public String _testName = "";

    @BeforeClass
    public static void deployApp() throws Exception {
        WebArchive war = ShrinkHelper.buildDefaultApp("forbidden.war",
            "web",
            "org.apache.logging.log4j.core.lookup");
        ShrinkHelper.addDirectory(war, "test-applications/forbidden.war/resources");
        ShrinkHelper.exportAppToServer(_server, war, DeployOptions.SERVER_ONLY);
    }

    @Rule
    public TestName name = new TestName();

    @Before
    public void configTest() throws Exception {
        _testName = name.getMethodName();
        Log.info(c, _testName, "===== Starting test " + _testName + " =====");
        switch (test.valueOf(_testName)) {
            case testForbidden:
                // Remove the property just in case tests run out of order
                removeBootstrapProperties(_server, Collections.singletonMap("io.openliberty.classloading.nothing.forbidden", "true"));
                _server.startServer();
                break;
            case testNothingForbidden:
                addBootstrapProperties(_server, Collections.singletonMap("io.openliberty.classloading.nothing.forbidden", "true"));
                _server.startServer();
                break;
            default:
                Log.info(c, _testName, "No test to configure");
                break;
        }
    }

    @After
    public void tearDownTest() throws Exception {
        Log.info(c, _testName, "===== Ending test " + _testName + " =====");
        if (_server.isStarted()) {
            _server.stopServer();
        }
    }

    /**
     * Test to verify that forbidden classes cannot load
     */
    @Test
    public void testForbidden() throws Exception {
        test();
    }

    /**
     * Test to verify that forbidden classes can load when overridden by user.
     */
    @Test
    public void testNothingForbidden() throws Exception {
        test();
    }

    private String test() throws Exception {
        return test(_server, "", _testName);
    }

    private static String test(LibertyServer server, String appname, String testUri) throws Exception {
        URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + appname + "/forbidden/test?testName=" + testUri);
        String output = HttpUtils.getHttpResponseAsString(url);
        assertNotNull(output);
        assertNotNull(output.trim());
        assertTrue("url:'" + url.toString() + "' appname:'" + appname + "' output:'" + output + "' testUri:'" + testUri + "'", output.trim().contains(SUCCESS_MESSAGE));
        return output;
    }

    private enum test {
        testForbidden,
        testNothingForbidden,
    }

    void addBootstrapProperties(LibertyServer server, Map<String, String> properties) throws Exception {
        RemoteFile bootstrapPropFile = server.getServerBootstrapPropertiesFile();
        Properties mergedProps = new Properties();
        try (FileInputStream in = (FileInputStream) bootstrapPropFile.openForReading()) {
            mergedProps.load(in);
        }
        for (String key : properties.keySet()) {
            mergedProps.setProperty(key, properties.get(key));
        }
        try (FileOutputStream out = (FileOutputStream) bootstrapPropFile.openForWriting(false)) {
            mergedProps.store(out, null);
        }
    }

    void removeBootstrapProperties(LibertyServer server, Map<String, String> properties) throws Exception {
        RemoteFile bootstrapPropFile = server.getServerBootstrapPropertiesFile();
        Properties differenceProps = new Properties();
        try (FileInputStream in = (FileInputStream) bootstrapPropFile.openForReading()) {
            differenceProps.load(in);
        }
        for (String key : properties.keySet()) {
            differenceProps.remove(key);
        }
        try (FileOutputStream out = (FileOutputStream) bootstrapPropFile.openForWriting(false)) {
            differenceProps.store(out, null);
        }
    }
}
