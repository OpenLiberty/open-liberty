/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.server.config;

import static org.junit.Assert.assertNotNull;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 *
 */

@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class WSConfigurationHelperTest extends ServletRunner {

    private static final String CONTEXT_ROOT = "confighelper";

    /*
     * (non-Javadoc)
     *
     * @see test.server.config.ServletRunner#getContextRoot()
     */
    @Override
    protected String getContextRoot() {
        return CONTEXT_ROOT;
    }

    /*
     * (non-Javadoc)
     *
     * @see test.server.config.ServletRunner#getServletMapping()
     */
    @Override
    protected String getServletMapping() {
        return "helperTest";
    }

    @BeforeClass
    public static void setUpForWSConfigurationHelperTest() throws Exception {

        // Use the feature/bundle from the merged config tests
        server.copyFileToLibertyInstallRoot("lib/features", "internalFeatureForFat/mergedConfigTest-1.0.mf");
        server.copyFileToLibertyInstallRoot("lib", "bundles/test.merged.config.jar");
        server.copyFileToLibertyInstallRoot("lib/features", "internalFeatureForFat/configfatlibertyinternals-1.0.mf");

        WebArchive configHelperApp = ShrinkHelper.buildDefaultApp("confighelper", "test.config.helper");
        ShrinkHelper.exportAppToServer(server, configHelperApp, DeployOptions.DISABLE_VALIDATION);

        server.startServer("helperTest.log");
        //make sure the URL is available
        assertNotNull(server.waitForStringInLog("CWWKT0016I.*" + CONTEXT_ROOT));
        assertNotNull(server.waitForStringInLog("CWWKF0011I"));
    }

    @AfterClass
    public static void shutdown() throws Exception {
        server.stopServer();
        server.deleteFileFromLibertyInstallRoot("lib/features/configfatlibertyinternals-1.0.mf");
    }

    private static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.config.helper");

    @Test
    public void testGetDefaultProperties() throws Exception {
        test(server);
    }

    @Test
    public void testGetDefaultPropertiesWithRequired() throws Exception {
        test(server);
    }

    @Test
    public void testAddDefaultConfiguration1() throws Exception {
        test(server);
    }

    @Test
    public void testAddDefaultConfiguration2() throws Exception {
        test(server);
    }

    @Test
    public void testAddDefaultConfiguration4() throws Exception {
        test(server);
    }

    @Test
    public void testAddDefaultConfiguration3() throws Exception {
        test(server);
    }

    @Test
    public void testAddDefaultConfiguration5() throws Exception {
        test(server);
    }

    @Test
    public void testAddDefaultConfiguration6() throws Exception {
        test(server);
    }

    @Test
    public void testAddDefaultConfiguration7() throws Exception {
        test(server);
    }

    @Test
    public void testAddDefaultConfiguration8() throws Exception {
        test(server);
    }
}
