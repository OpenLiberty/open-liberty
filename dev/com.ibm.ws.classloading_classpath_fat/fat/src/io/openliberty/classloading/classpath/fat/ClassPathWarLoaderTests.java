/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.classloading.classpath.fat;

import static io.openliberty.classloading.classpath.fat.FATSuite.CLASSPATH_TEST_WAR_LOADER_SERVER;
import static io.openliberty.classloading.classpath.fat.FATSuite.TEST_CLASS_PATH1_APP;
import static io.openliberty.classloading.classpath.fat.FATSuite.TEST_CLASS_PATH2_APP;
import static io.openliberty.classloading.classpath.fat.FATSuite.TEST_CLASS_PATH3_APP;
import static io.openliberty.classloading.classpath.fat.FATSuite.TEST_CLASS_PATH_EAR;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.classloading.classpath.test.war1.ClassPathDefaultLoaderServletTest1;
import io.openliberty.classloading.classpath.test.war2.ClassPathDefaultLoaderServletTest2;
import io.openliberty.classloading.classpath.test.war3.ClassPathDefaultLoaderServletTest3;

/**
 *
 */
@RunWith(FATRunner.class)
public class ClassPathWarLoaderTests extends FATServletClient {

    @Server(CLASSPATH_TEST_WAR_LOADER_SERVER)
    @TestServlets({
        @TestServlet(servlet = ClassPathDefaultLoaderServletTest1.class, contextRoot = TEST_CLASS_PATH1_APP),
        @TestServlet(servlet = ClassPathDefaultLoaderServletTest2.class, contextRoot = TEST_CLASS_PATH2_APP),
        @TestServlet(servlet = ClassPathDefaultLoaderServletTest3.class, contextRoot = TEST_CLASS_PATH3_APP)
    })
    public static LibertyServer server;

    @BeforeClass
    public static void setupTestServer() throws Exception {
        ShrinkHelper.exportAppToServer(server, TEST_CLASS_PATH_EAR, DeployOptions.SERVER_ONLY);
        server.startServer();
    }

    @AfterClass
    public static void stopServer() throws Exception {
        server.stopServer("SRVE9967W");
    }
}
