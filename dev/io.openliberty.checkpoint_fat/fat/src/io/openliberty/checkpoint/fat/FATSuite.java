/*******************************************************************************
 * Copyright (c) 2021, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.checkpoint.fat;

import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.custom.junit.runner.AlwaysPassesTest;
import componenttest.topology.impl.LibertyFileManager;
import componenttest.topology.impl.LibertyServer;

@RunWith(Suite.class)
@SuiteClasses({
                AlwaysPassesTest.class,
                TestWithFATServlet.class,
                TestWithFATServlet2.class,
                LogsVerificationTest.class,
                OSGiConsoleTest.class,
                RemoteEJBTest.class,
                TestSPIConfig.class,
                TestMPConfigServlet.class,
                SSLTest.class
})
public class FATSuite {
    public static void copyAppsAppToDropins(LibertyServer server, String appName) throws Exception {
        RemoteFile appFile = server.getFileFromLibertyServerRoot("apps/" + appName + ".war");
        LibertyFileManager.createRemoteFile(server.getMachine(), server.getServerRoot() + "/dropins").mkdir();
        appFile.copyToDest(server.getFileFromLibertyServerRoot("dropins"));
    }

    static public <T extends Enum<T>> T getTestMethod(Class<T> type, TestName testName) {
        String testMethodSimpleName = testName.getMethodName();
        int dot = testMethodSimpleName.indexOf('.');
        if (dot != -1) {
            testMethodSimpleName = testMethodSimpleName.substring(dot + 1);
        }
        try {
            return Enum.valueOf(type, testMethodSimpleName);
        } catch (IllegalArgumentException e) {
            Log.info(type, testName.getMethodName(), "No configuration enum: " + testName.getMethodName());
            return Enum.valueOf(type, "unknown");
        }
    }
}
