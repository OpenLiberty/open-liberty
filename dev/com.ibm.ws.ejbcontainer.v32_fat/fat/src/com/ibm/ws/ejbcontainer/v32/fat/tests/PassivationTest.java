/*******************************************************************************
 * Copyright (c) 2014, 2025 IBM Corporation and others.
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
package com.ibm.ws.ejbcontainer.v32.fat.tests;

import java.util.List;
import java.util.regex.Pattern;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

@RunWith(FATRunner.class)
public class PassivationTest extends FATServletClient {
    private static final String PASSIVATION_CAPABLE_REGEXP = "@PrePassivate.*PassivationCapable(|(Anno|XML)True)Bean";

    @Server("com.ibm.ws.ejbcontainer.v32.fat.passivation")
    public static LibertyServer server;

    private static boolean checkLogsAfterServerStop;

    @ClassRule
    public static RepeatTests r = RepeatTests.with(FeatureReplacementAction.EE7_FEATURES().forServers("com.ibm.ws.ejbcontainer.v32.fat.passivation")) //
                    .andWith(FeatureReplacementAction.EE8_FEATURES().fullFATOnly().forServers("com.ibm.ws.ejbcontainer.v32.fat.passivation")) //
                    .andWith(FeatureReplacementAction.EE9_FEATURES().conditionalFullFATOnly(FeatureReplacementAction.GREATER_THAN_OR_EQUAL_JAVA_11).forServers("com.ibm.ws.ejbcontainer.v32.fat.passivation")) //
                    .andWith(FeatureReplacementAction.EE10_FEATURES().conditionalFullFATOnly(FeatureReplacementAction.GREATER_THAN_OR_EQUAL_JAVA_17).forServers("com.ibm.ws.ejbcontainer.v32.fat.passivation")) //
                    .andWith(FeatureReplacementAction.EE11_FEATURES().forServers("com.ibm.ws.ejbcontainer.v32.fat.passivation"));

    @BeforeClass
    public static void beforeClass() throws Exception {
        // Use ShrinkHelper to build the Ears & Wars

        //#################### PassivationCapable.war
        WebArchive PassivationCapable = ShrinkHelper.buildDefaultApp("PassivationCapable.war", "com.ibm.ws.ejbcontainer.v32.fat.passivation.");
        ShrinkHelper.exportDropinAppToServer(server, PassivationCapable, DeployOptions.SERVER_ONLY);

        server.startServer();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        if (server != null) {
            server.stopServer(false, "CNTR0332W");
            try {
                if (checkLogsAfterServerStop) {
                    checkLogsAfterServerStop();
                }
            } finally {
                server.postStopServerArchive();
            }
        }
    }

    private static void checkLogsAfterServerStop() throws Exception {
        // Check again after the server is stopped to look for passivations
        // that erroneously occurred during application stop.
        checkUnexpectedPrePassivate();

        // The rest of the EJBs should be destroyed during application stop.
        List<String> lines = server.findStringsInLogs("@PreDestroy");
        Assert.assertEquals("Unexpected number of lines: " + lines.toString(), 5, lines.size());

        Pattern p = Pattern.compile("@PreDestroy.*PassivationCapable(AnnoFalse|XMLFalse(|LoadStrategy(Once|Transaction|ActivitySession)))Bean");
        for (String line : lines) {
            Assert.assertTrue("Unexpected line: " + line, p.matcher(line).find());
        }
    }

    private static void checkUnexpectedPrePassivate() throws Exception {
        Pattern p = Pattern.compile(PASSIVATION_CAPABLE_REGEXP);
        for (String line : server.findStringsInLogs("@PrePassivate")) {
            Assert.assertTrue("Unexpected line: " + line, p.matcher(line).find());
        }
    }

    @Test
    public void testPassivationCapable() throws Exception {
        // Create more SFSB than the cache can hold, which should trigger
        // passivation of those that are capable.
        FATServletClient.runTest(server, "PassivationCapable", getTestMethodSimpleName());

        // We're only expecting 3, so wait for all of them.
        Assert.assertEquals(3, server.waitForMultipleStringsInLog(3, PASSIVATION_CAPABLE_REGEXP));

        // Check for any unexpected PrePassivate.  If a failure occurs, the
        // three expected PrePassivate might occur first and we might miss
        // unexpected ones, but we check again after stopping the server anyway.
        checkUnexpectedPrePassivate();
        checkLogsAfterServerStop = true;
    }

    @Test
    public void testPassivationCapableLoadStrategy() throws Exception {
        // The relevant EJBs are <start-at-app-start> in ibm-ejb-jar-ext.xml,
        // so these warnings should appear even without accessing the EJBs.
        List<String> lines = server.findStringsInLogs("CNTR0332W:");
        Assert.assertEquals("Unexpected number of lines: " + lines.toString(), 2, lines.size());

        Pattern p = Pattern.compile("PassivationCapableXMLFalseLoadStrategy(Transaction|ActivitySession)Bean");
        for (String line : lines) {
            Assert.assertTrue("Unexpected line: " + line, p.matcher(line).find());
        }
    }
}
