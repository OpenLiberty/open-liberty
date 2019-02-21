/*******************************************************************************
 * Copyright (c) 2012, 2013 IBM Corporation and others.
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

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

//@Mode(TestMode.FULL)
public class MetatypeProviderTest extends ServletRunner {

    private static final String CONTEXT_ROOT = "metatypeprovider";
    private static final String NO_METATYPE_SERVER = "metatype/noMetatypeServer.xml";
    private static final String ORIGINAL_SERVER = "metatype/server.xml";
    private static final String PLANT_PID = "test.metatype.provider.plant";
    private static final String ANIMAL_PID = "test.metatype.provider.animal";

    @Override
    protected String getContextRoot() {
        return CONTEXT_ROOT;
    }

    @Override
    protected String getServletMapping() {
        return "providerTest";
    }

    private static LibertyServer testServer = LibertyServerFactory.getLibertyServer("com.ibm.ws.config.metatype.provider");

    @BeforeClass
    public static void setUpForMetatypeProviderTests() throws Exception {
        //copy the feature into the server features location
        testServer.copyFileToLibertyInstallRoot("lib/features", "internalFeatureForFat/metatypeProviderTest-1.0.mf");
        testServer.copyFileToLibertyInstallRoot("lib/features", "internalFeatureForFat/configfatlibertyinternals-1.0.mf");

        //copy the bundle into the server lib location
        testServer.copyFileToLibertyInstallRoot("lib", "bundles/test.metatype.provider.jar");

        WebArchive metatypeProviderApp = ShrinkHelper.buildDefaultApp("metatypeprovider", "test.metatype.provider");
        ShrinkHelper.exportAppToServer(testServer, metatypeProviderApp);

        testServer.startServer();

        //make sure the URL is available
        assertNotNull(testServer.waitForStringInLog("CWWKT0016I.*" + CONTEXT_ROOT));
        assertNotNull(testServer.waitForStringInLog("CWWKF0011I"));
    }

    @AfterClass
    public static void shutdown() throws Exception {
        testServer.stopServer("CWWKE0701E");
        testServer.deleteFileFromLibertyInstallRoot("lib/features/metatypeProviderTest-1.0.mf");
        testServer.deleteFileFromLibertyInstallRoot("lib/test.metatype.provider_1.0.0.jar");
        testServer.deleteFileFromLibertyInstallRoot("lib/features/configfatlibertyinternals-1.0.mf");
    }

    @Test
    public void testMetatypeProvider1() throws Exception {
        test(testServer);
    }

    @Test
    public void testMetatypeProvider2() throws Exception {
        test(testServer);
    }

    @Test
    public void testMetatypeProvider3() throws Exception {

        testServer.setMarkToEndOfLog();
        testServer.setServerConfigurationFile(NO_METATYPE_SERVER);
        testServer.waitForConfigUpdateInLogUsingMark(null);
        // NOTE: We are currently waiting for these strings in trace because metatype provider removed events are not
        // hooked into the main runtime update framework. If we ever add official support for metatype providers this
        // should go away.
        testServer.waitForStringInTrace("Removed metatype for PID " + PLANT_PID);
        testServer.waitForStringInTrace("Removed metatype for PID " + ANIMAL_PID);

        test(testServer);

        testServer.setMarkToEndOfLog();
        testServer.setServerConfigurationFile(ORIGINAL_SERVER);
        testServer.waitForConfigUpdateInLogUsingMark(null);
    }
}
