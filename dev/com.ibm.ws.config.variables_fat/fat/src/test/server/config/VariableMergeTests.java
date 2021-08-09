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
public class VariableMergeTests extends ServletRunner {

    private static final String CONTEXT_ROOT = "varmergedconfig";
    private static final String ALL_IN_ONE_SERVER = "varmerge/allInOne.xml";
    private static final String BREAK_SERVER = "varmerge/break.xml";
    private static final String BREAK_2_SERVER = "varmerge/break2.xml";
    private static final String IGNORE_SERVER = "varmerge/ignore.xml";
    private static final String REPLACE_SERVER = "varmerge/replace.xml";
    private static final String MERGE_SERVER = "varmerge/merge.xml";

    @Override
    protected String getContextRoot() {
        return CONTEXT_ROOT;
    }

    @Override
    protected String getServletMapping() {
        return "varMergeTest";
    }

    private static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.config.varmerging");

    @Test
    public void testMergedVariables() throws Exception {
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

    @BeforeClass
    public static void setUpForMergedConfigTests() throws Exception {
        //copy the config feature into the server features location
        server.copyFileToLibertyInstallRoot("lib/features", "internalFeatureForFat/configfatlibertyinternals-1.0.mf");

        WebArchive varmergeApp = ShrinkHelper.buildDefaultApp("varmerge", "test.config.merged");
        ShrinkHelper.exportAppToServer(server, varmergeApp, DeployOptions.DISABLE_VALIDATION);

        server.startServer("varmerge.log");
        //make sure the URL is available
        assertNotNull(server.waitForStringInLog("CWWKT0016I.*" + CONTEXT_ROOT));
        assertNotNull(server.waitForStringInLog("CWWKF0011I"));
    }

    @AfterClass
    public static void shutdown() throws Exception {
        server.stopServer();
        server.deleteFileFromLibertyInstallRoot("lib/features/configfatlibertyinternals-1.0.mf");
    }

}
