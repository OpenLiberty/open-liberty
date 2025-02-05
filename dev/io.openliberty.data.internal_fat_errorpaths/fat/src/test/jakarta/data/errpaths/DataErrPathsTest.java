/*******************************************************************************
 * Copyright (c) 2024,2025 IBM Corporation and others.
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

import static org.junit.Assert.assertEquals;

import java.io.BufferedInputStream;
import java.util.Scanner;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ProgramOutput;
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
                                   "CWWKD1003E.*existsByAddress", // exists method returning int
                                   "CWWKD1003E.*existsByName", // exists method returning CompletableFuture<Long>
                                   "CWWKD1006E.*removeBySSN", // delete method attempts to return record
                                   "CWWKD1009E.*addNothing", // Insert method without parameters
                                   "CWWKD1009E.*addSome", // Insert method with multiple parameters
                                   "CWWKD1009E.*changeNothing", // Update method without parameters
                                   "CWWKD1009E.*changeBoth", // Update method with multiple entity parameters
                                   "CWWKD1009E.*storeNothing", // Save method without parameters
                                   "CWWKD1009E.*storeInDatabase", // Save method with multiple parameters
                                   "CWWKD1010E.*nameAndZipCode", // Record return type with invalid attribute name
                                   "CWWKD1015E.*addPollingLocation", // insert null entity
                                   "CWWKD1015E.*addOrUpdatePollingLocation", // save null entity
                                   "CWWKD1017E.*livesAt", // multiple Limit parameters
                                   "CWWKD1017E.*residesAt", // multiple PageRequest parameters
                                   "CWWKD1018E.*inhabiting", // intermixed Limit and PageRequest
                                   "CWWKD1018E.*occupying", // intermixed Limit and PageRequest
                                   "CWWKD1019E.*livingAt", // mix of named/positional parameters
                                   "CWWKD1019E.*residingAt", // unused parameters
                                   "CWWKD1022E.*discardPage", // Delete operation with a PageRequest
                                   "CWWKD1033E.*selectByFirstName", // CursoredPage with ORDER BY in Query
                                   "CWWKD1037E.*findByBirthdayOrderBySSN", // CursoredPage of non-entity
                                   "CWWKD1037E.*registrations", // CursoredPage of non-entity
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
                                   "CWWKD1091E.*deleteByAddressOrderByName", // OrderBy without return type
                                   "CWWKD1094E.*register", // incompatible return type
                                   "CWWKD1096E.*discardInOrder", // OrderBy annotation without return type
                                   "CWWKD1097E.*discardLimited", // Limit parameter on Delete method
                                   "CWWKD1097E.*discardOrdered", // Order parameter on Delete method
                                   "CWWKD1097E.*discardSorted", // Sort parameter on Delete method
                                   "CWWKD1098E.*findFirst5ByAddress", // Order ahead of query params
                                   "CWWKD1098E.*occupantsOf", // PageRequest/Order ahead of query params
                                   "CWWKD1098E.*withNameLongerThan", // Limit ahead of query params
                                   "CWWKD1098E.*withNameShorterThan", // Sort ahead of query params
                                   "CWWKD1099E.*findFirst2", // Limit incompatible with First
                                   "CWWKD1099E.*findFirst3", // PageRequest incompatible with First
                                   "CWWKD1100E.*selectByLastName", // CursoredPage with ORDER BY clause
                                   "CWWKD1101E.*nameAndZipCode" // Record return type with invalid attribute name
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

        // @AfterClass is intentionally omitted so that the server continues running
        // for the next class in the suite, which is DataIntrospectorTest.
    }

    /**
     * Dump the server and collect the Jakarta Data introspector output
     * for the DataIntrospectorTest to use. Then stop the server.
     */
    @AfterClass
    public static void tearDown() throws Exception {
        ProgramOutput output = server.serverDump();
        assertEquals(0, output.getReturnCode());
        assertEquals("", output.getStderr());

        // Parse standard output. Examples:
        //
        // Server io.openliberty.data.internal.fat.errpaths dump complete in
        //   /Users/user/lgit/open-liberty/dev/build.image/wlp/usr/
        //   servers/io.openliberty.data.internal.fat.errpaths/
        //   io.openliberty.data.internal.fat.errpaths.dump-18.04.11_14.30.55.zip.
        //
        // Server io.openliberty.data.internal.fat.errpaths dump complete in
        //   C:\\jazz-build-engines\\wasrtc-proxy.hursley.ibm.com\\
        //   EBC.PROD.WASRTC\\build\\dev\\image\\output\\wlp\\usr\\
        //   servers\\io.openliberty.data.internal.fat.errpaths\\
        //   io.openliberty.data.internal.fat.errpaths.dump-18.06.10_00.16.59.zip.

        String out = output.getStdout();
        int end = out.lastIndexOf('.');
        int begin = out.lastIndexOf(' ', end) + 1;

        String dumpFileName = out.substring(begin, end);

        System.out.println("Dump file name: " + dumpFileName);

        // Example of file within the zip:
        // dump_25.01.24_14.50.04/introspections/JakartaDataIntrospector.txt

        end = dumpFileName.indexOf(".zip");
        String prefix = "io.openliberty.data.internal.fat.errpaths.dump-";
        begin = dumpFileName.lastIndexOf(prefix, end) + prefix.length();

        String introspectorFileName = "dump_" + dumpFileName.substring(begin, end) +
                                      "/introspections/JakartaDataIntrospector.txt";

        System.out.println("Looking for introspector entry: " + introspectorFileName);

        try (ZipFile dumpFile = new ZipFile(dumpFileName)) {
            ZipEntry entry = dumpFile.getEntry(introspectorFileName);
            System.out.println("Found: " + entry);
            try (BufferedInputStream in = new BufferedInputStream(dumpFile.getInputStream(entry));
                            Scanner scanner = new Scanner(in)) {
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    System.out.println(line);
                    DataIntrospectorTest.introspectorOutput.add(line);
                }
            }
        }

        server.stopServer(EXPECTED_ERROR_MESSAGES);
    }
}
