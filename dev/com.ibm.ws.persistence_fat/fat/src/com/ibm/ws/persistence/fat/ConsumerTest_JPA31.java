/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
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

package com.ibm.ws.persistence.fat;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.MinimumJavaLevel;
//import componenttest.custom.junit.runner.OnlyRunInJava7Rule;
import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.JakartaEEAction;
import componenttest.rules.repeater.RepeatActions.EEVersion;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import persistence_fat.consumer.web.ConsumerServlet;

@RunWith(FATRunner.class)
@MinimumJavaLevel(javaLevel = 11)
public class ConsumerTest_JPA31 extends FATServletClient {
    private static final String APP_NAME = "consumer";

    private static final String FEATURE_NAME = "com.ibm.ws.persistence.consumer.jakarta-1.0";
    private static final String BUNDLE_NAME = "com.ibm.ws.persistence.consumer.jakarta_1.0.0";

    @Server("com.ibm.ws.persistence.consumer.jpa31")
    @TestServlet(servlet = ConsumerServlet.class, path = APP_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void beforeClass() throws Exception {
        server.installSystemFeature(FEATURE_NAME);
        server.copyFileToLibertyInstallRoot("lib/", "bundles/com.ibm.ws.persistence.consumer.jar");
        Assert.assertTrue(server.fileExistsInLibertyInstallRoot("lib/com.ibm.ws.persistence.consumer.jar"));

        Path someArchive = Paths.get(server.getInstallRoot() + File.separatorChar + "lib" + File.separatorChar + "com.ibm.ws.persistence.consumer.jar");
        JakartaEEAction.transformApp(someArchive, EEVersion.EE10);

        ShrinkHelper.defaultDropinApp(server, APP_NAME, "persistence_fat.consumer.ejb", "persistence_fat.consumer.model",
                                      "persistence_fat.consumer.web");

        Path warArchive = Paths.get(server.getServerRoot() + File.separatorChar + "dropins" + File.separatorChar + APP_NAME + ".war");
        JakartaEEAction.transformApp(warArchive, EEVersion.EE10);

        server.startServer();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        server.stopServer("WTRN0074E");
        server.deleteFileFromLibertyInstallRoot("lib/com.ibm.ws.persistence.consumer.jar");
        server.uninstallSystemFeature(FEATURE_NAME);
    }
}
