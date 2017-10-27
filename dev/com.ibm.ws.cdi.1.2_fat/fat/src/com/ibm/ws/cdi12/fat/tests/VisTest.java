/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.ws.cdi12.suite.ShutDownSharedServer;
import com.ibm.ws.fat.util.LoggingTest;
import com.ibm.ws.fat.util.browser.WebBrowser;

import componenttest.topology.impl.LibertyClient;
import componenttest.topology.impl.LibertyClientFactory;

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

public class VisTest extends LoggingTest {

    @ClassRule
    public static ShutDownSharedServer SHARED_SERVER = new ShutDownSharedServer("visTestServer");

    public static Logger LOG = Logger.getLogger(VisTest.class.getName());

    @Override
    protected ShutDownSharedServer getSharedServer() {
        return SHARED_SERVER;
    }

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
        WebBrowser wb = createWebBrowserForTestCase();
        String response = SHARED_SERVER.getResponse(wb, "/visTestWar/?location=" + location).getResponseBody();

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

}
