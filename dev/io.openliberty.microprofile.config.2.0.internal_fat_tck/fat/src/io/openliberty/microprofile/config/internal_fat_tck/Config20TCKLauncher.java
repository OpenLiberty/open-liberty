/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.config.internal_fat_tck;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.List;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.MvnUtils;

/**
 * This is a test class that runs a whole Maven TCK as one test FAT test.
 * There is a detailed output on specific
 */
@RunWith(FATRunner.class)
@Mode(TestMode.LITE)
public class Config20TCKLauncher {

    @ClassRule
    public static RepeatTests r = RepeatTests.with(FeatureReplacementAction.EE8_FEATURES());

    @Server("Config20TCKServer")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        server.setDebuggingAllowed(true);
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer("CWMCG0007E", "CWMCG0014E", "CWMCG0015E", "CWMCG5003E", "CWWKZ0002E");
    }

    @Test
    @AllowedFFDC // The tested deployment exceptions cause FFDC so we have to allow for this.
    public void launchConfig20Tck() throws Exception {
        MvnUtils.runTCKMvnCmd(server, "io.openliberty.microprofile.config.2.0.internal_fat_tck", this.getClass() + ":launchConfig20Tck");
        String productVersion = "";
        try{
            String productVersion = "";
            List<String> matches = server.findStringsInLogs("product =");
            if(!matches.isEmpty()){
                productVersion = matches.get(0);
            }
        }
        finally{         
            MvnUtils.preparePublicationFile("MicroProfile", productVersion);
        }
    }
}
