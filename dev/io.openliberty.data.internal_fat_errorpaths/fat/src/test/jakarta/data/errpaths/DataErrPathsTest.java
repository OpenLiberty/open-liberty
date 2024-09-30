/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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
package test.jakarta.data.errpaths;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.MinimumJavaLevel;
import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import test.jakarta.data.errpaths.web.DataErrPathsTestServlet;

@RunWith(FATRunner.class)
@MinimumJavaLevel(javaLevel = 17)
public class DataErrPathsTest extends FATServletClient {

    private static final String APP_NAME = "DataErrPathsTestApp";

    /**
     * Error messages that are intentionally caused by tests to cover error paths.
     * These are ignored when checking the messages.log file for errors.
     */
    private static final String[] EXPECTED_ERROR_MESSAGES = //
                    new String[] {
                                   "CWWKD1077E.*test.jakarta.data.errpaths.web.RepoWithoutDataStore",
                                   "CWWKD1078E.*test.jakarta.data.errpaths.web.InvalidNonJNDIRepo",
                                   "CWWKD1079E.*test.jakarta.data.errpaths.web.InvalidJNDIRepo"
                    };

    @Server("io.openliberty.data.internal.fat.errpaths")
    @TestServlet(servlet = DataErrPathsTestServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        WebArchive war = ShrinkHelper.buildDefaultApp("DataErrPathsTestApp",
                                                      "test.jakarta.data.errpaths.web");
        ShrinkHelper.exportAppToServer(server, war);
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer(EXPECTED_ERROR_MESSAGES);
    }
}
