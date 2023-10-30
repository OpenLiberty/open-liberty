/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
package io.openliberty.checkpoint.fat.springboot;

import static componenttest.topology.utils.FATServletClient.getTestMethodSimpleName;

import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.config.SpringBootApplication;

import componenttest.custom.junit.runner.AlwaysPassesTest;
import componenttest.topology.impl.LibertyServer;

@RunWith(Suite.class)
@SuiteClasses({
                AlwaysPassesTest.class,
                BasicSpringBootTests.class,
                BasicSpringBootWefluxTests.class
})

public class FATSuite {

    public static String getTestMethodNameOnly(TestName testName) {
        String testMethodSimpleName = getTestMethodSimpleName(testName);
        // Sometimes the method name includes the class name; remove the class name.
        int dot = testMethodSimpleName.indexOf('.');
        if (dot != -1) {
            testMethodSimpleName = testMethodSimpleName.substring(dot + 1);
        }
        return testMethodSimpleName;
    }

    public static void configureApplication(LibertyServer server, String appName) throws Exception {
        ServerConfiguration config = server.getServerConfiguration();
        RemoteFile appFile = server.getFileFromLibertyServerRoot("apps/" + appName);
        SpringBootApplication app = new SpringBootApplication();
        app.setLocation(appFile.getName());
        config.getSpringBootApplications().add(app);
        server.updateServerConfiguration(config);
    }
}
