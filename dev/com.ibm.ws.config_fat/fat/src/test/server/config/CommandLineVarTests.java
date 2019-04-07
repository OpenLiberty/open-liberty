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

import java.util.ArrayList;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

public class CommandLineVarTests extends ServletRunner {

    private static final String CONTEXT_ROOT = "varmergedconfig";
    private static final String UPDATED_SERVER = "clv/updated.xml";

    @Override
    protected String getContextRoot() {
        return CONTEXT_ROOT;
    }

    @Override
    protected String getServletMapping() {
        return "varMergeTest";
    }

    private static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.config.commandLineVar");

    @Test
    public void testCommandLineVariables() throws Exception {
        server.setMarkToEndOfLog();

        test(server);

        // check that variables are still overridden after updating the server
        server.setServerConfigurationFile(UPDATED_SERVER);

        test(server);
    }

    @BeforeClass
    public static void setUpForMergedConfigTests() throws Exception {
        //copy the config feature into the server features location
        server.copyFileToLibertyInstallRoot("lib/features", "internalFeatureForFat/configfatlibertyinternals-1.0.mf");

        WebArchive varmergeApp = ShrinkHelper.buildDefaultApp("varmerge", "test.config.merged");
        ShrinkHelper.exportAppToServer(server, varmergeApp);

        server.setConsoleLogName("clv.log");
        ArrayList<String> args = new ArrayList<String>();
        args.add("--");
        args.add("--clvOnly=CLV");
        args.add("--clvOverrideBootstrap=CLV");
        args.add("--clvOverrideServerXML=CLV");
        args.add("--clvOverrideBoth=CLV");
        args.add("--clvEmpty=");
        args.add("--clvInvalid");
        args.add("--=clvInvalid2");
        args.add("clvOnly=invalidCLVOnly");
        args.add("clvOverrideBootstrap=invalidOverrideBootstrap");
        args.add("--");
        args.add("===");

        server.startServerWithArgs(true, true, true, false, "start", args, true);
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
