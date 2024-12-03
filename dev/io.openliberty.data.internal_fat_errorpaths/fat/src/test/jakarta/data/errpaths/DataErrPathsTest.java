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
                                   "CWWJP9991W.*4002", // 2 persistence units attempt to autocreate same table
                                   "CWWKD1019E.*livingAt", // mix of named/positional parameters
                                   "CWWKD1019E.*residingAt", // unused parameters
                                   "CWWKD1077E.*test.jakarta.data.errpaths.web.RepoWithoutDataStore",
                                   "CWWKD1078E.*test.jakarta.data.errpaths.web.InvalidNonJNDIRepo",
                                   "CWWKD1079E.*test.jakarta.data.errpaths.web.InvalidJNDIRepo",
                                   "CWWKD1080E.*test.jakarta.data.errpaths.web.InvalidDatabaseRepo",
                                   "CWWKD1080E.*test.jakarta.data.errpaths.web.Inventions", // has invalid entity class
                                   "CWWKD1082E.*test.jakarta.data.errpaths.web.WrongPersistenceUnitRefRepo",
                                   "CWWKD1083E.*bornOn", // duplicate Param annotations
                                   "CWWKD1084E.*bornIn", // Param annotation omitted
                                   "CWWKD1084E.*livingIn", // named parameter mismatch
                                   "CWWKD1085E.*livingOn", // extra Param annotations
                                   "CWWKD1086E.*withAddressShorterThan", // Param used for positional parameter
                                   "CWWKD1090E.*findByAddressOrderBy", // OrderBy anno/keyword conflict
                                   "CWWKD1094E.*register" // incompatible return type
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

        // Cause errors that will log FFDC prior to running tests
        // so that FFDC doesn't intermittently fail tests
        FATServletClient.runTest(server, APP_NAME, "forceFFDC");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer(EXPECTED_ERROR_MESSAGES);
    }
}
