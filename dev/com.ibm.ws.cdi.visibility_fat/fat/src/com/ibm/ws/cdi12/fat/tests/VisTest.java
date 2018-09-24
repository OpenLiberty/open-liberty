/*******************************************************************************
 * Copyright (c) 2015, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi12.fat.tests;

import static org.junit.Assert.fail;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.FileAsset;
import org.jboss.shrinkwrap.api.importer.ZipImporter;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.ResourceAdapterArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.ws.fat.util.LoggingTest;

import componenttest.annotation.Server;
import componenttest.topology.impl.LibertyClient;
import componenttest.topology.impl.LibertyClientFactory;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.HttpUtils;

/**
 * Tests the visibility of beans between different BDAs
 * <p>
 * We've identified the following interesting places that a bean can exist and we test the visibility between each of them
 * <p>
 * <ul>
 * <li>Ejb - an EJB jar</li>
 * <li>War - a WAR</li>
 * <li>AppClient - an application client jar</li>
 * <li>EjbLib - a jar referenced on the classpath of Ejb</li>
 * <li>WarLib - a jar referenced on the classpath of War</li>
 * <li>WarWebinfLib - a jar included in the WEB-INF/lib directory of War</li>
 * <li>AppClientLib - a jar referenced on the classpath of AppClient</li>
 * <li>EjbWarLib - a jar referenced on the classpath of Ejb and War</li>
 * <li>EjbAppClientLib - a jar referenced on the classpath of Ejb and AppClient</li>
 * <li>WarAppClientLib - a jar referenced on the classpath of War and AppClient</li>
 * <li>EarLib - a jar in the /lib directory of the ear</li>
 * <li>NonLib - a jar in the root of the ear, not referenced from anywhere</li>
 * <li>EjbAsEjbLib - an EJB jar also referenced on the classpath of Ejb</li>
 * <li>EjbAsWarLib - an EJB jar also referenced on the classpath of War</li>
 * <li>EjbAsAppClientLib - an EJB jar also referenced on the classpath of AppClient</li>
 * <li>AppClientAsEjbLib - an application client jar also referenced on the classpath of Ejb</li>
 * <li>AppClientAsWarLib - an application client jar also referenced on the classpath of War</li>
 * <li>AppClientAsAppClientLib - an application client jar also referenced on the classpath of AppClient</li>
 * <li>War2 - another WAR, which does not reference anything else</li>
 * </ul>
 * <p>
 * The test is conducted by going through a servlet or application client main class, providing the location from which to test visibility. This class will load a
 * TestingBean from the requested location and call its doTest() method which will report which of the TargetBeans are accessible.
 * <p>
 * Each row of the visibility report has a bean location and the number of beans in that location that are visible, separated by a tab character.
 */

public class VisTest {

    public static LibertyServer server = LibertyServerFactory.getLibertyServer("visTestServer");

    //This is driven by getAppClientResults
    public static void buildShrinkWrap(LibertyClient client) throws Exception {

        JavaArchive visTestWarWebinfLib1 = ShrinkWrap.create(JavaArchive.class,"visTestWarWebinfLib.jar")
                        .addClass("vistest.warWebinfLib.WarWebinfLibTargetBean")
                        .addClass("vistest.warWebinfLib.WarWebinfLibTestingBean");

        WebArchive visTestWar2 = ShrinkWrap.create(WebArchive.class, "visTestWar.war")
                        .addClass("vistest.war.WarTargetBean")
                        .addClass("vistest.war.servlet.VisibilityTestServlet")
                        .addClass("vistest.war.WarTestingBean")
                        .addAsManifestResource(new File("test-applications/visTestWar.war/resources/META-INF/MANIFEST.MF"))
                        .addAsLibrary(visTestWarWebinfLib1);

        JavaArchive visTestEjb3 = ShrinkWrap.create(JavaArchive.class,"visTestEjb.jar")
                        .addClass("vistest.ejb.dummy.DummySessionBean")
                        .addClass("vistest.ejb.EjbTargetBean")
                        .addClass("vistest.ejb.EjbTestingBean")
                        .addAsManifestResource(new File("test-applications/visTestEjb.jar/resources/META-INF/MANIFEST.MF"));

        JavaArchive visTestAppClient4 = ShrinkWrap.create(JavaArchive.class,"visTestAppClient.jar")
                        .addAsManifestResource(new File("test-applications/visTestAppClient.jar/resources/META-INF/MANIFEST.MF"))
                        .addClass("vistest.appClient.main.Main")
                        .addClass("vistest.appClient.AppClientTargetBean")
                        .addClass("vistest.appClient.AppClientTestingBean");

        JavaArchive visTestEjbAsEjbLib5 = ShrinkWrap.create(JavaArchive.class,"visTestEjbAsEjbLib.jar")
                        .addClass("vistest.ejbAsEjbLib.dummy.DummySessionBean")
                        .addClass("vistest.ejbAsEjbLib.EjbAsEjbLibTestingBean")
                        .addClass("vistest.ejbAsEjbLib.EjbAsEjbLibTargetBean");

        JavaArchive visTestEjbAsWarLib6 = ShrinkWrap.create(JavaArchive.class,"visTestEjbAsWarLib.jar")
                        .addClass("vistest.ejbAsWarLib.dummy.DummySessionBean")
                        .addClass("vistest.ejbAsWarLib.EjbAsWarLibTestingBean")
                        .addClass("vistest.ejbAsWarLib.EjbAsWarLibTargetBean");

        JavaArchive visTestEjbAsAppClientLib7 = ShrinkWrap.create(JavaArchive.class,"visTestEjbAsAppClientLib.jar")
                        .addClass("vistest.ejbAsAppClientLib.dummy.DummySessionBean")
                        .addClass("vistest.ejbAsAppClientLib.EjbAsAppClientLibTestingBean")
                        .addClass("vistest.ejbAsAppClientLib.EjbAsAppClientLibTargetBean");

        JavaArchive visTestAppClientAsEjbLib8 = ShrinkWrap.create(JavaArchive.class,"visTestAppClientAsEjbLib.jar")
                        .addClass("vistest.appClientAsEjbLib.dummy.DummyMain")
                        .addClass("vistest.appClientAsEjbLib.AppClientAsEjbLibTestingBean")
                        .addClass("vistest.appClientAsEjbLib.AppClientAsEjbLibTargetBean");

        JavaArchive visTestAppClientAsWarLib9 = ShrinkWrap.create(JavaArchive.class,"visTestAppClientAsWarLib.jar")
                        .addClass("vistest.appClientAsWarLib.dummy.DummyMain")
                        .addClass("vistest.appClientAsWarLib.AppClientAsWarLibTargetBean")
                        .addClass("vistest.appClientAsWarLib.AppClientAsWarLibTestingBean");

        JavaArchive visTestAppClientAsAppClientLib10 = ShrinkWrap.create(JavaArchive.class,"visTestAppClientAsAppClientLib.jar")
                        .addClass("vistest.appClientAsAppClientLib.dummy.DummyMain")
                        .addClass("vistest.appClientAsAppClientLib.AppClientAsAppClientLibTargetBean")
                        .addClass("vistest.appClientAsAppClientLib.AppClientAsAppClientLibTestingBean");

        WebArchive visTestWar11 = ShrinkWrap.create(WebArchive.class, "visTestWar2.war")
                        .addClass("vistest.war2.War2TargetBean")
                        .addClass("vistest.war2.War2TestingBean")
                        .addClass("vistest.war2.servlet.VisibilityTestServlet");

        JavaArchive visTestWarLib12 = ShrinkWrap.create(JavaArchive.class,"visTestWarLib.jar")
                        .addClass("vistest.warLib.WarLibTestingBean")
                        .addClass("vistest.warLib.WarLibTargetBean");

        JavaArchive visTestEjbLib13 = ShrinkWrap.create(JavaArchive.class,"visTestEjbLib.jar")
                        .addClass("vistest.ejbLib.EjbLibTargetBean")
                        .addClass("vistest.ejbLib.EjbLibTestingBean");

        JavaArchive visTestAppClientLib14 = ShrinkWrap.create(JavaArchive.class,"visTestAppClientLib.jar")
                        .addClass("vistest.appClientLib.AppClientLibTargetBean")
                        .addClass("vistest.appClientLib.AppClientLibTestingBean");

        JavaArchive visTestEjbWarLib15 = ShrinkWrap.create(JavaArchive.class,"visTestEjbWarLib.jar")
                        .addClass("vistest.ejbWarLib.EjbWarLibTestingBean")
                        .addClass("vistest.ejbWarLib.EjbWarLibTargetBean");

        JavaArchive visTestEjbAppClientLib16 = ShrinkWrap.create(JavaArchive.class,"visTestEjbAppClientLib.jar")
                        .addClass("vistest.ejbAppClientLib.EjbAppClientLibTestingBean")
                        .addClass("vistest.ejbAppClientLib.EjbAppClientLibTargetBean");

        JavaArchive visTestWarAppClientLib17 = ShrinkWrap.create(JavaArchive.class,"visTestWarAppClientLib.jar")
                        .addClass("vistest.warAppClientLib.WarAppClientLibTestingBean")
                        .addClass("vistest.warAppClientLib.WarAppClientLibTargetBean");

        JavaArchive visTestNonLib18 = ShrinkWrap.create(JavaArchive.class,"visTestNonLib.jar")
                        .addClass("vistest.nonLib.NonLibTestingBean")
                        .addClass("vistest.nonLib.NonLibTargetBean");

        JavaArchive visTestFramework19 = ShrinkWrap.create(JavaArchive.class,"visTestFramework.jar")
                        .addClass("vistest.qualifiers.InWarWebinfLib")
                        .addClass("vistest.qualifiers.InEjb")
                        .addClass("vistest.qualifiers.InEarLib")
                        .addClass("vistest.qualifiers.InEjbAsEjbLib")
                        .addClass("vistest.qualifiers.InWarAppClientLib")
                        .addClass("vistest.qualifiers.InAppClient")
                        .addClass("vistest.qualifiers.InAppClientLib")
                        .addClass("vistest.qualifiers.InNonLib")
                        .addClass("vistest.qualifiers.InAppClientAsAppClientLib")
                        .addClass("vistest.qualifiers.InEjbAppClientLib")
                        .addClass("vistest.qualifiers.InEjbAsAppClientLib")
                        .addClass("vistest.qualifiers.InAppClientAsWarLib")
                        .addClass("vistest.qualifiers.InEjbAsWarLib")
                        .addClass("vistest.qualifiers.InEjbLib")
                        .addClass("vistest.qualifiers.InWar")
                        .addClass("vistest.qualifiers.InWar2")
                        .addClass("vistest.qualifiers.InAppClientAsEjbLib")
                        .addClass("vistest.qualifiers.InWarLib")
                        .addClass("vistest.qualifiers.InEjbWarLib")
                        .addClass("vistest.framework.TargetBean")
                        .addClass("vistest.framework.VisTester")
                        .addClass("vistest.framework.TestingBean");

        JavaArchive visTestEarLib20 = ShrinkWrap.create(JavaArchive.class,"visTestEarLib.jar")
                        .addClass("vistest.earLib.EarLibTargetBean")
                        .addClass("vistest.earLib.EarLibTestingBean");

        EnterpriseArchive visTest = ShrinkWrap.create(EnterpriseArchive.class,"visTest.ear")
                        .addAsModule(visTestWar2)
                        .addAsModule(visTestEjb3)
                        .addAsModule(visTestAppClient4)
                        .addAsModule(visTestEjbAsEjbLib5)
                        .addAsModule(visTestEjbAsWarLib6)
                        .addAsModule(visTestEjbAsAppClientLib7)
                        .addAsModule(visTestAppClientAsEjbLib8)
                        .addAsModule(visTestAppClientAsWarLib9)
                        .addAsModule(visTestAppClientAsAppClientLib10)
                        .addAsModule(visTestWar11)
                        .addAsModule(visTestWarLib12)
                        .addAsModule(visTestEjbLib13)
                        .addAsModule(visTestAppClientLib14)
                        .addAsModule(visTestEjbWarLib15)
                        .addAsModule(visTestEjbAppClientLib16)
                        .addAsModule(visTestWarAppClientLib17)
                        .addAsModule(visTestNonLib18)
                        .addAsLibrary(visTestFramework19)
                        .addAsLibrary(visTestEarLib20)
                        .addAsResource("com/ibm/ws/cdi12/fat/tests/permissions.xml", "permissions.xml");

       ShrinkHelper.exportAppToServer(server, visTest);
       ShrinkHelper.exportAppToClient(client, visTest);

       server.startServer();
    }

    public static Logger LOG = Logger.getLogger(VisTest.class.getName());

    /**
     * Enumeration of locations of target beans
     * <p>
     * See class documentation for details of each location
     */
    private enum Location {
        InEjb,
        InWar,
        InAppClient,
        InEjbLib,
        InWarLib,
        InWarWebinfLib,
        InAppClientLib,
        InEjbWarLib,
        InEjbAppClientLib,
        InWarAppClientLib,
        InEarLib,
        InNonLib,
        InEjbAsEjbLib,
        InEjbAsWarLib,
        InEjbAsAppClientLib,
        InAppClientAsEjbLib,
        InAppClientAsWarLib,
        InAppClientAsAppClientLib,
        InWar2
    }

    /**
     * Set of locations that should be visible from EJBs and their libraries
     */
    Set<Location> EJB_VISIBLE_LOCATIONS = new HashSet<Location>(Arrays.asList(Location.InEjb,
                                                                              Location.InEjbLib,
                                                                              Location.InEjbWarLib,
                                                                              Location.InEjbAppClientLib,
                                                                              Location.InEarLib,
                                                                              Location.InEjbAsEjbLib,
                                                                              Location.InEjbAsWarLib,
                                                                              Location.InEjbAsAppClientLib,
                                                                              Location.InAppClientAsEjbLib));

    /**
     * Set of locations that should be visible from WARs and their libraries
     */
    Set<Location> WAR_VISIBLE_LOCATIONS = new HashSet<Location>(Arrays.asList(Location.InEjb,
                                                                              Location.InWar,
                                                                              Location.InEjbLib,
                                                                              Location.InWarLib,
                                                                              Location.InWarWebinfLib,
                                                                              Location.InEjbWarLib,
                                                                              Location.InEjbAppClientLib,
                                                                              Location.InWarAppClientLib,
                                                                              Location.InEarLib,
                                                                              Location.InEjbAsEjbLib,
                                                                              Location.InEjbAsWarLib,
                                                                              Location.InEjbAsAppClientLib,
                                                                              Location.InAppClientAsEjbLib,
                                                                              Location.InAppClientAsWarLib));

    /**
     * Set of locations that should be visible from app clients and their libraries
     */
    Set<Location> APP_CLIENT_VISIBLE_LOCATIONS = new HashSet<Location>(Arrays.asList(Location.InAppClient,
                                                                                     Location.InAppClientLib,
                                                                                     Location.InEjbAppClientLib,
                                                                                     Location.InWarAppClientLib,
                                                                                     Location.InEarLib,
                                                                                     Location.InEjbAsAppClientLib,
                                                                                     Location.InAppClientAsAppClientLib));

    Set<Location> EAR_LIB_VISIBLE_LOCATIONS = new HashSet<Location>(Arrays.asList(Location.InEarLib));

    private static Map<Location, String> appClientResults = null;

    /**
     * Run the app client and store the results in the appClientResults map
     * <p>
     * One invocation of the app client returns all the relevant results because there's a significant overhead to starting up the client.
     *
     * @throws Exception
     */
    @BeforeClass
    public static void getAppClientResults() throws Exception {

        appClientResults = new HashMap<Location, String>();

        LibertyClient client = LibertyClientFactory.getLibertyClient("visTestClient");
        buildShrinkWrap(client);
        ProgramOutput output = client.startClient();

        LOG.info("GOT THE CLIENT OUTPUT");

        if (output.getReturnCode() != 0) {
            LOG.severe("BAD RETURN CODE");
            throw new Exception("Client returned error: " + output.getReturnCode() + "\nStdout:\n" + output.getStdout() + "\nStderr:\n" + output.getStderr());
        }

        String[] resultSets = output.getStdout().split("----[\r\n]+");

        // Read the result set, skipping the initial section which is the startup messages and the final section which is the shutdown messages
        for (int i = 1; i < resultSets.length - 1; i++) {
            String resultSet = resultSets[i];
            try {
                String[] results = resultSet.split("[\r\n]+", 2);
                Location location = Location.valueOf(results[0]);
                appClientResults.put(location, results[1]);
            } catch (Throwable ex) {
                LOG.warning("FAILED TO PARSE A CLIENT LINE: " + resultSet);
            }
        }
    }

    @Test
    public void testVisibilityFromEjb() throws Exception {
        doTestWithServlet(Location.InEjb, EJB_VISIBLE_LOCATIONS);
    }

    @Test
    public void testVisibilityFromWar() throws Exception {
        doTestWithServlet(Location.InWar, WAR_VISIBLE_LOCATIONS);
    }

    @Test
    public void testVisibilityFromAppClient() throws Exception {
        doTestWithAppClient(Location.InAppClient, APP_CLIENT_VISIBLE_LOCATIONS);
    }

    @Test
    public void testVisibilityFromEjbLib() throws Exception {
        doTestWithServlet(Location.InEjbLib, EJB_VISIBLE_LOCATIONS);
    }

    @Test
    public void testVisibilityFromWarLib() throws Exception {
        doTestWithServlet(Location.InWarLib, WAR_VISIBLE_LOCATIONS);
    }

    @Test
    public void testVisibilityFromWarWebinfLib() throws Exception {
        doTestWithServlet(Location.InWarWebinfLib, WAR_VISIBLE_LOCATIONS);
    }

    @Test
    public void testVisibilityFromAppClientLib() throws Exception {
        doTestWithAppClient(Location.InAppClientLib, APP_CLIENT_VISIBLE_LOCATIONS);
    }

    @Test
    public void testVisibilityFromEjbWarLib() throws Exception {
        doTestWithServlet(Location.InEjbWarLib, EJB_VISIBLE_LOCATIONS);
    }

    @Test
    public void testVisibilityFromEjbAppClientLib() throws Exception {
        doTestWithServlet(Location.InEjbAppClientLib, EJB_VISIBLE_LOCATIONS);
    }

    @Test
    public void testVisibilityFromWarAppClientLib() throws Exception {
        doTestWithServlet(Location.InWarAppClientLib, WAR_VISIBLE_LOCATIONS);
    }

    @Test
    public void testVisibilityFromEarLib() throws Exception {
        doTestWithServlet(Location.InEarLib, EJB_VISIBLE_LOCATIONS);
    }

    @Test
    public void testVisibilityFromEjbAsEjbLib() throws Exception {
        doTestWithServlet(Location.InEjbAsEjbLib, EJB_VISIBLE_LOCATIONS);
    }

    @Test
    public void testVisibilityFromEjbAsWarLib() throws Exception {
        doTestWithServlet(Location.InEjbAsWarLib, EJB_VISIBLE_LOCATIONS);
    }

    @Test
    public void testVisibilityFromEjbAsAppClientLib() throws Exception {
        doTestWithServlet(Location.InEjbAsAppClientLib, EJB_VISIBLE_LOCATIONS);
    }

    @Test
    public void testVisibilityFromAppClientAsEjbLib() throws Exception {
        doTestWithServlet(Location.InAppClientAsEjbLib, EJB_VISIBLE_LOCATIONS);
    }

    @Test
    public void testVisibilityFromAppClientAsWarLib() throws Exception {
        doTestWithServlet(Location.InAppClientAsWarLib, WAR_VISIBLE_LOCATIONS);
    }

    @Test
    public void testVisibilityFromAppClientAsAppClientLib() throws Exception {
        doTestWithAppClient(Location.InAppClientAsAppClientLib, APP_CLIENT_VISIBLE_LOCATIONS);
    }

    /**
     * Retrieves the visibility of beans from a given location by requesting the information from a servlet. Then checks the result and fails the test if it does not match the
     * expected set of visible locations
     *
     * @param location the location to test visibility from
     * @param visibleLocations the locations which should be visible
     * @throws Exception if there is an error requesting the visibility information or parsing the result.
     */
    private void doTestWithServlet(Location location, Set<Location> visibleLocations) throws Exception {
        String response = HttpUtils.getHttpResponseAsString(server, "/visTestWar/?location=" + location);

        checkResult(response, visibleLocations);
    }

    /**
     * Retrieves the visibility of beans from a given location by looking at the output of the app client. Checks the result and fails the test if it does not match the expected
     * set of visible locations
     *
     * @param location the location to test visibility from
     * @param visibleLocations the locations which should be visible
     * @throws Exception if there is an error requesting the visibility information or parsing the result.
     */
    private void doTestWithAppClient(Location location, Set<Location> visibleLocations) throws Exception {
        String resultString = appClientResults.get(location);
        if (resultString == null) {
            throw new Exception("Client output did not include results for " + location);
        }

        checkResult(resultString, visibleLocations);
    }

    /**
     * Checks that the given result string indicates that only a given list of locations are visible.
     * <p>
     * Fails the test if the parsed result does not match the set of visible locations.
     *
     * @param resultString the result string
     * @param visibleLocations the locations that should be reported as visible in the result string
     * @throws Exception if there is an error parsing the resultString
     */
    private void checkResult(String resultString, Set<Location> visibleLocations) throws Exception {

        Map<Location, Integer> results = parseResult(resultString);

        List<String> errors = new ArrayList<String>();
        for (Location location : Location.values()) {
            Integer count = results.get(location);

            if (count == null) {
                errors.add("No result returned for " + location);
                continue;
            }

            if (count < 0) {
                errors.add("Invalid result for " + location + ": " + count);
                continue;
            }

            if (count > 1) {
                errors.add(count + " instances of bean found for " + location);
                continue;
            }

            if (visibleLocations.contains(location) && count != 1) {
                errors.add("Bean " + location + " is not accessible but should be");
                continue;
            }

            if (!visibleLocations.contains(location) && count != 0) {
                errors.add("Bean " + location + " is accessible but should not be");
                continue;
            }

            // If we get here, result is ok, don't add any errors
        }

        // If we've found any problems, return them
        if (!errors.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append("Errors found in result: \n");
            for (String error : errors) {
                sb.append(error);
                sb.append("\n");
            }
            fail(sb.toString());
        }
    }

    /**
     * Parse a result string into a map from location to number of beans visible from that location.
     *
     * @param resultString the string to parse
     * @return map from location to number of visible beans
     * @throws Exception if there is an error parsing the result
     */
    private Map<Location, Integer> parseResult(String resultString) throws Exception {
        Map<Location, Integer> results = new HashMap<Location, Integer>();

        if (resultString.startsWith("ERROR")) {
            fail("Error response received:\n" + resultString);
        }

        for (String line : resultString.split("[\r\n]+")) {

            String[] parts = line.split("\t");
            if (parts.length != 2) {
                throw parsingException(line, resultString, null);
            }

            String locationString = parts[0];
            Integer resultCount;
            try {
                resultCount = new Integer(parts[1]);
            } catch (NumberFormatException ex) {
                throw parsingException(line, resultString, ex);
            }

            Location resultLocation;
            try {
                resultLocation = Location.valueOf(locationString);
            } catch (IllegalArgumentException ex) {
                // Additional result we don't care about
                continue;
            }

            results.put(resultLocation, resultCount);
        }

        return results;
    }

    private Exception parsingException(String line, String response, Throwable cause) {
        return new Exception("Badly formed line: " + line + "\n\nWhole response:\n" + response, cause);
    }

    @AfterClass
    public static void afterClass() throws Exception {
        if (server != null) {
            server.stopServer();
        }
    }

}
