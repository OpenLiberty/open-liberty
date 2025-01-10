/*******************************************************************************
 * Copyright (c) 2012, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.webcontainer.servlet31.fat;

import java.util.Locale;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.websphere.simplicity.config.Logging;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.ws.fat.util.FatLogHandler;
import com.ibm.ws.webcontainer.servlet31.fat.tests.AsyncReadListenerHttpUnit;
import com.ibm.ws.webcontainer.servlet31.fat.tests.AsyncWriteListenerHttpUnit;
import com.ibm.ws.webcontainer.servlet31.fat.tests.FormLoginReadListenerTest;
import com.ibm.ws.webcontainer.servlet31.fat.tests.NBMultiReadTest;
import com.ibm.ws.webcontainer.servlet31.fat.tests.UpgradeReadListenerHttpUnit;
import com.ibm.ws.webcontainer.servlet31.fat.tests.UpgradeReadListenerSendImmediateData;
import com.ibm.ws.webcontainer.servlet31.fat.tests.UpgradeReadWriteTimeoutHttpUnit;
import com.ibm.ws.webcontainer.servlet31.fat.tests.UpgradeWriteListenerHttpUnit;

import componenttest.rules.repeater.EmptyAction;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;

/**
 * Servlet 3.1 Tests
 */
@RunWith(Suite.class)
@SuiteClasses({
                AsyncReadListenerHttpUnit.class,
                AsyncWriteListenerHttpUnit.class,
                UpgradeWriteListenerHttpUnit.class,
                UpgradeReadListenerHttpUnit.class,
                UpgradeReadListenerSendImmediateData.class,
                UpgradeReadWriteTimeoutHttpUnit.class,
                FormLoginReadListenerTest.class,
                NBMultiReadTest.class
})
public class FATSuite {

    @ClassRule
    public static RepeatTests repeat;

    public static final boolean isWindows = System.getProperty("os.name").toLowerCase(Locale.ENGLISH).contains("win");

    static {
        // EE10 requires Java 11.
        // EE11 requires Java 17
        // If we only specify EE10/EE11 for lite mode it will cause no tests to run with lower Java versions which causes an error.
        // If we are running with a Java version less than 11, have EE9 be the lite mode test to run.
        repeat = RepeatTests.with(new EmptyAction().fullFATOnly())
                        .andWith(FeatureReplacementAction.EE8_FEATURES().fullFATOnly())
                        .andWith(FeatureReplacementAction.EE9_FEATURES().conditionalFullFATOnly(FeatureReplacementAction.GREATER_THAN_OR_EQUAL_JAVA_11))
                        .andWith(FeatureReplacementAction.EE10_FEATURES().conditionalFullFATOnly(FeatureReplacementAction.GREATER_THAN_OR_EQUAL_JAVA_17))
                        .andWith(FeatureReplacementAction.EE11_FEATURES());

    }

    //Due to Fyre performance on Windows, use this method to set the server trace to the minimum
    public static void setDynamicTrace(LibertyServer server, String trace) throws Exception {
        Logging loggingObj;
        ServerConfiguration serverConfig = server.getServerConfiguration();
        loggingObj = serverConfig.getLogging();
        loggingObj.setTraceSpecification(trace);

        server.setMarkToEndOfLog();
        server.updateServerConfiguration(serverConfig);
        server.waitForConfigUpdateInLogUsingMark(null);

        /*
         * Reset the log marks so waitForStringInLog continues to work.
         * If we don't reset the marks then anything that was logged previous
         * to this method call would be lost. For example: logs during application
         * initialization and server startup.
         */
        server.resetLogMarks();
    }

    /**
     * @see {@link FatLogHandler#generateHelpFile()}
     */
    @BeforeClass
    public static void generateHelpFile() {
        FatLogHandler.generateHelpFile();
    }

}
