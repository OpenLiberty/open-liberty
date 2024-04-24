/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package io.openliberty.reporting.internal.fat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.Properties;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

@RunWith(FATRunner.class)
public class CVEDataTest extends FATServletClient {

    public static final String SERVER_NAME = "io.openliberty.reporting.server";

    protected static final Class<?> c = CVEDataTest.class;

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {

        server.saveServerConfiguration();

    }

    @After
    public void tearDown() throws Exception {

        if (server.isStarted()) {
            server.stopServer();
        }

        server.restoreServerConfiguration();
    }

    @Test
    public void testCheckProperties() throws Exception {

        server.startServer();
        server.addIgnoredErrors(Collections.singletonList("CWWKF1704W"));

        Properties props = createJSON("\\{\"productEdition.*\\}");

        assertTrue("The property 'id' is not null or empty",
                   props.getProperty("id") != null && !props.getProperty("id").isEmpty());

        String[] productEdition = new String[] { "Core", "CORE", "BASE", "DEVELOPERS", "EXPRESS", "BLUEMIX",
                                                 "EARLY_ACCESS", "zOS", "ND", "BASE_ILAN", "Open" };

        assertThat("The property 'productEdition' did not match", props.getProperty("productEdition"),
                   Matchers.isIn(productEdition));

        assertTrue("The property 'productVersion' did not match at the start",
                   props.getProperty("productVersion").matches("^\\d\\d\\..*"));

        assertTrue("The property 'productVersion' did not match at the end",
                   props.getProperty("productVersion").matches("^\\d\\d.0.0.([1-9]|1[0123])$"));

        String javaVendor = System.getProperty("java.vendor").toLowerCase();

        if (javaVendor == null) {
            javaVendor = System.getProperty("java.vm.name", "unknown").toLowerCase();
        }

        assertEquals("The property 'javaVendor' did not match", javaVendor, props.getProperty("javaVendor"));

        assertEquals("The property 'javaVersion' did not match", System.getProperty("java.runtime.version"),
                     props.getProperty("javaVersion"));

        assertEquals("The property 'os' did not match", System.getProperty("os.name"), props.getProperty("os"));

        assertEquals("The property 'osArch' did not match", System.getProperty("os.arch"), props.getProperty("osArch"));

        String features = String.valueOf(props.get("features"));
        String iFixes = String.valueOf(props.get("iFixes"));

        assertTrue("The property 'features' did not match", features != null);

        assertTrue("The property 'iFixes' did not match", iFixes != null);

    }

    @Test
    public void testInstalledFeaturesListAndIFixList() throws Exception {
        server.copyFileToLibertyInstallRoot("lib/features/", "test.InterimFixes-1.0.mf");
        server.copyFileToLibertyInstallRoot("lib", "ProvisioningInterimFixesTestBundle1_1.0.0.jar");
        server.copyFileToLibertyInstallRoot("lib", "ProvisioningInterimFixesTestBundle1_1.0.0.20130101.jar");
        server.setServerConfigurationFile("testFeatureListServer.xml");

        server.startServer();
        server.addIgnoredErrors(Collections.singletonList("CWWKF1704W"));

        Properties props = createJSON("\\{\"productEdition.*\\}");

        String features = String.valueOf(props.get("features"));

        assertTrue("The property 'features' are incorrect",
                   features.contains("servlet-") && features.contains("test.InterimFixes-1.0"));

        String iFixes = String.valueOf(props.get("iFixes"));

        assertTrue("The property 'iFixes' did not match",
                   iFixes.contains("APAR0007") && iFixes.contains("APAR0006") && iFixes.contains("APAR0008"));

    }

    /**
     * This method takes the line found within the logs which contains the
     * JSONString and then parses it as a json object using jsonb library.
     *
     * @param regex
     * @return
     */
    public Properties createJSON(String regex) {
        Jsonb JSONB = JsonbBuilder.create();

        String response = server.waitForStringInTrace(regex);
        response = response.substring(response.indexOf("{"), response.length());

        Log.info(c, "createJSON", response);

        Properties props = JSONB.fromJson(response, Properties.class);

        Log.info(c, "createJSON", props.toString());

        return props;
    }

}
