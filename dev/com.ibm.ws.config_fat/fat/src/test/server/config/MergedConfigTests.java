/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
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
import static org.junit.Assert.assertNull;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@RunWith(FATRunner.class)
public class MergedConfigTests extends ServletRunner {

    private static final String CONTEXT_ROOT = "mergedconfig";
    private static final String ALL_IN_ONE_SERVER = "merge/allInOne.xml";
    private static final String IGNORE_SERVER = "merge/ignore.xml";
    private static final String REPLACE_SERVER = "merge/replace.xml";
    private static final String MERGE_SERVER = "merge/merge.xml";
    private static final String IGNORE_REPLACE_SERVER = "merge/ignoreReplace.xml";
    private static final String FOUR_LEVEL_REPLACE_SERVER = "merge/fourLevelReplace.xml";
    private static final String FOUR_LEVEL_IGNORE_SERVER = "merge/fourLevelIgnore.xml";
    private static final String IGNORE_ONCONFLICT_SERVER = "merge/parent.xml";

    @Override
    protected String getContextRoot() {
        return CONTEXT_ROOT;
    }

    @Override
    protected String getServletMapping() {
        return "mergedConfigTest";
    }

    private static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.config.merging");

    @Test
    public void testMergedConfig() throws Exception {
        server.setMarkToEndOfLog();
        server.setServerConfigurationFile(ALL_IN_ONE_SERVER);
        server.waitForConfigUpdateInLogUsingMark(null);

        test(server);

    }

    @Test
    public void testMergedIncludesReplace() throws Exception {
        server.setMarkToEndOfLog();
        server.setServerConfigurationFile(REPLACE_SERVER);
        server.waitForConfigUpdateInLogUsingMark(null);

        test(server);
    }

    @Test
    public void testMergedIncludesIgnore() throws Exception {
        server.setMarkToEndOfLog();
        server.setServerConfigurationFile(IGNORE_SERVER);
        server.waitForConfigUpdateInLogUsingMark(null);

        test(server);
    }

    @Test
    public void testMergedIncludesMerge() throws Exception {
        server.setMarkToEndOfLog();
        server.setServerConfigurationFile(MERGE_SERVER);
        server.waitForConfigUpdateInLogUsingMark(null);

        test(server);
    }

    @Test
    public void testMergedIncludesIgnoreReplace() throws Exception {
        server.setMarkToEndOfLog();
        server.setServerConfigurationFile(IGNORE_REPLACE_SERVER);
        server.waitForConfigUpdateInLogUsingMark(null);

        test(server);
    }

    @Test
    public void testMergedIncludesFourLevelReplace() throws Exception {
        server.setMarkToEndOfLog();
        server.setServerConfigurationFile(FOUR_LEVEL_REPLACE_SERVER);
        server.waitForConfigUpdateInLogUsingMark(null);

        test(server);
    }

    @Test
    public void testMergedIncludesFourLevelIgnore() throws Exception {
        server.setMarkToEndOfLog();
        server.setServerConfigurationFile(FOUR_LEVEL_IGNORE_SERVER);
        server.waitForConfigUpdateInLogUsingMark(null);

        test(server);
    }

    @Test
    public void testDefaultInstances1() throws Exception {
        // Test that a normal defaultInstances file was read. This works with any of the server.xml files, so no need to
        // update.
        test(server);
    }

    @Test
    public void testDefaultInstances2() throws Exception {
        // Verify onConflict="merge_when_exists". Works with any server.xml from these tests, so no need to update it.
        test(server);
    }

    @Test
    public void testDefaultInstances3() throws Exception {
        // Verify onConflict="merge_when_does_not_exist". Works with any server.xml from these tests, so no need to update it.
        test(server);
    }

    @Test
    public void testOnConflictIGNORE() throws Exception {
        server.setMarkToEndOfLog();
        server.setServerConfigurationFile(IGNORE_ONCONFLICT_SERVER);
        server.waitForConfigUpdateInLogUsingMark(null);

        assertNull(server.verifyStringNotInLogUsingMark("CWWKL0004E.*", 20));
    }

    @BeforeClass
    public static void setUpForMergedConfigTests() throws Exception {
        //copy the feature into the server features location
        server.copyFileToLibertyInstallRoot("lib/features", "internalFeatureForFat/mergedConfigTest-1.0.mf");
        server.copyFileToLibertyInstallRoot("lib/features", "internalFeatureForFat/configfatlibertyinternals-1.0.mf");

        //copy the bundle into the server lib location
        server.copyFileToLibertyInstallRoot("lib", "bundles/test.merged.config.jar");

        WebArchive mergedconfigApp = ShrinkHelper.buildDefaultApp("mergedconfig", "test.config.merged");
        ShrinkHelper.exportAppToServer(server, mergedconfigApp, DeployOptions.DISABLE_VALIDATION);

        server.startServer("mergedConfig.log");
        //make sure the URL is available
        assertNotNull(server.waitForStringInLog("CWWKT0016I.*" + CONTEXT_ROOT));
        assertNotNull(server.waitForStringInLog("CWWKF0011I"));
    }

    @AfterClass
    public static void shutdown() throws Exception {
        server.stopServer("CWWKG0101W", "CWWKG0103W");
        server.deleteFileFromLibertyInstallRoot("lib/features/mergedConfigTest-1.0.mf");
        server.deleteFileFromLibertyInstallRoot("lib/test.merged.config.jar");
        server.deleteFileFromLibertyInstallRoot("lib/features/configfatlibertyinternals-1.0.mf");
    }

}
