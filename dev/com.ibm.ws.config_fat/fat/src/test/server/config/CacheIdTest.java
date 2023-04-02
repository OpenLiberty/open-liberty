/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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

package test.server.config;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jboss.shrinkwrap.api.spec.WebArchive;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.topology.impl.LibertyServer;

/**
 * Base class for cache ID testing.
 */
public class CacheIdTest {
    // Server configuration values ...

    protected static final String CONFIG_NAME = "server.xml";

    // Application configuration elements ...

    protected static final String APP_NAME0 = "cacheid0";
    protected static final String APP_NAME1 = "cacheid1";

    protected static final String APP_NOID_WAR0 = "<application name=\"cacheid0\" type=\"war\" location=\"cacheid0.war\"/>";
    protected static final String APP_ID0_WAR0 = "<application id=\"cacheid0\" name=\"cacheid0\" type=\"war\" location=\"cacheid0.war\"/>";
    protected static final String APP_NOID_WAR1 = "<application name=\"cacheid1\" type=\"war\" location=\"cacheid1.war\"/>";
    protected static final String APP_ID1_WAR1 = "<application id=\"cacheid1\" name=\"cacheid1\" type=\"war\" location=\"cacheid1.war\"/>";

    // Shared application update instructions ... for the update and rearrange tests.

    protected static final String[] WAR0_DEFAULT_ID = { APP_NOID_WAR0 };
    protected static final String[] WAR0_ASSIGNED_ID = { APP_ID0_WAR0 };

    protected static final String[] WAR1_DEFAULT_ID = { APP_NOID_WAR1 };
    protected static final String[] WAR1_ASSIGNED_ID = { APP_ID1_WAR1 };

    protected static final String[] REMOVE_MATCH0 = { null };
    protected static final Set<Integer> ADD_AFTER_MATCH0 = Collections.singleton(Integer.valueOf(0));

    // The test applications ...

    protected static WebArchive cacheId0App;
    protected static WebArchive cacheId1App;

    public static synchronized void setupApps() throws Exception {
        if (cacheId0App == null) {
            cacheId0App = ShrinkHelper.buildDefaultApp("cacheid0", "test.server.config.cacheid");
        }
        if (cacheId1App == null) {
            cacheId1App = ShrinkHelper.buildDefaultApp("cacheid1", "test.server.config.cacheid");
        }
    }

    // Server utility ...

    public static String getConfigPath(LibertyServer server) {
        return server.getServerRoot() + '/' + CONFIG_NAME;
    }

    public static void waitForFeatureUpdate(LibertyServer server) {
        assertNotNull("Feature update did not complete",
                      server.waitForStringInLog("CWWKF0008I"));
    }

    public static void waitForApp0(LibertyServer server) {
        assertNotNull("The 'cacheid0' application did not start",
                      server.waitForStringInLog("CWWKZ0001I.* cacheid0"));
    }

    public static void waitForApp1(LibertyServer server) {
        assertNotNull("The 'cacheid1' application did not start",
                      server.waitForStringInLog("CWWKZ0001I.* cacheid1"));
    }

    public static void verifyApp0(LibertyServer server) throws Exception {
        doGet(server, "/cacheid0/cacheid0?testName=testCacheId", "testCacheId");
    }

    public static void verifyApp1(LibertyServer server) throws Exception {
        doGet(server, "/cacheid1/cacheid1?testName=testCacheId", "testCacheId");
    }

    protected static URL getServerURL(LibertyServer server, String requestUri) throws Exception {
        return new URL("http://" + server.getHostname() + ":" +
                       server.getHttpDefaultPort() +
                       requestUri);
    }

    // Parameters to 'LibertyServer.startServer' ...

    protected static final boolean CLEAN_START = true;
    protected static final boolean PRECLEAN_START = true;

    protected static void doGet(LibertyServer server, String testUri, String testName) throws Exception {
        URL url = getServerURL(server, testUri);
        System.out.println("Connection: [ " + url + " ]");

        HttpURLConnection con = (HttpURLConnection) url.openConnection();

        try {
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setUseCaches(false);
            con.setRequestMethod("GET");

            List<String> lines;
            try (InputStream is = con.getInputStream()) {
                lines = read(is);
            }

            boolean foundTestName = false;

            System.out.println("Response:");
            for (String line : lines) {
                System.out.println("[ " + line + " ]");
                if (!foundTestName) {
                    if (line.contains(testName)) {
                        foundTestName = true;
                    }
                }
            }
            assertTrue("Expected '" + testName + "'", foundTestName);

        } finally {
            con.disconnect();
        }
    }

    protected static List<String> read(InputStream in) throws IOException {
        InputStreamReader isr = new InputStreamReader(in);
        BufferedReader br = new BufferedReader(isr);

        List<String> lines = new ArrayList<>();
        String line;
        while ((line = br.readLine()) != null) {
            lines.add(line);
        }
        return lines;
    }

    // Server configuration utility ...

    @FunctionalInterface
    public interface ServerRunnable {
        void run(LibertyServer server) throws Exception;
    }

    protected static final ServerRunnable noidsCopyActor = (server) -> {
        server.copyFileToLibertyServerRoot("cacheid/noids/" + CONFIG_NAME);
    };

    protected static final ServerRunnable idsCopyActor = (server) -> {
        server.copyFileToLibertyServerRoot("cacheid/ids/" + CONFIG_NAME);
    };

    protected static final ServerRunnable noopActor = (server) -> {
        // EMPTY
    };

    protected static final ServerRunnable touchActor = (server) -> {
        String configPath = CacheIdTest.getConfigPath(server);
        new File(configPath).setLastModified(System.currentTimeMillis());
    };

    // ID values, for ID verification ...

    // The two web applications 'cacheid0' and 'cacheid1', are configured
    // either with no assigned IDs, or with assigned IDs.
    //
    // When configured with no IDs, the two default IDs are assigned.

    protected static final String id_default0 = "com.ibm.ws.app.manager_0";
    protected static final String id_default1 = "com.ibm.ws.app.manager_1";
    protected static final String id_assigned0 = "cacheid0";
    protected static final String id_assigned1 = "cacheid1";

    // These are the common ID assignment patterns.

    protected static final Set<String> unsetIds = null;
    protected static final Set<String> emptyIds = asSet();
    protected static final Set<String> defaultIds = asSet(id_default0, id_default1);
    protected static final Set<String> assignedIds = asSet(id_assigned0, id_assigned1);
    protected static final Set<String> allIds = union(defaultIds, assignedIds);

    // Set utility ...

    protected static Set<String> union(Set<String> set1, Set<String> set2) {
        int totalSize = set1.size() + set2.size();
        if (totalSize == 0) {
            return Collections.emptySet();
        } else {
            Set<String> unionSet = new HashSet<String>(totalSize);
            unionSet.addAll(set1);
            unionSet.addAll(set2);
            return unionSet;
        }
    }

    protected static Set<String> asSet(String... values) {
        if (values.length == 0) {
            return Collections.emptySet();
        }

        HashSet<String> set = new HashSet<>(values.length);
        for (String value : values) {
            set.add(value);
        }
        return set;
    }

    protected static List<String> add(List<String> values, String value) {
        if (values == null) {
            values = new ArrayList<>();
        }
        values.add(value);
        return values;
    }

    /**
     * Read the current cache IDs from a server. Validate these against
     * an expected set of cache IDs.
     *
     * An assertion failure will occur if the actual cache IDs are not the
     * same as the expected cache IDS.
     *
     * See {@link WorkareaReader} for details on reading the cache IDs.
     */
    protected void verifyCacheIds(LibertyServer server,
                                  String testName, String testPart,
                                  Set<String> expectedIds) {

        String installRoot = server.getInstallRoot();
        String serverName = server.getServerName();

        WorkareaReader reader = new WorkareaReader(installRoot, serverName);

        String pluginName = reader.getPluginName();
        String cachePath = reader.getCachePath();
        Set<String> cacheIds = reader.getCacheIds();

        System.out.println("[[ " + testName + " - " + testPart + " ]]");
        System.out.println("Installation root: [ " + installRoot + " ]");
        System.out.println("Server name: [ " + serverName + " ]");
        System.out.println("Plugin name: [ " + pluginName + " ]");
        System.out.println("Cache path: [ " + cachePath + " ]");

        System.out.println("Cache IDs:");
        if (cacheIds == null) {
            System.out.println("  [ ** NONE ** ]");
        } else if (cacheIds.isEmpty()) {
            System.out.println("  [ ** EMPTY ** ]");
        } else {
            for (String cacheId : cacheIds) {
                System.out.println("  [ " + cacheId + " ]");
            }
        }

        List<String> faults = verify(expectedIds, cacheIds);
        if (faults == null) {
            System.out.println("The IDs are as expected");
        } else {
            for (String fault : faults) {
                System.out.println("ID Error: [ " + fault + " ]");
            }

            System.out.println("Expected Cache IDs:");
            if (expectedIds == null) {
                System.out.println("  [ ** NONE ** ]");
            } else if (expectedIds.isEmpty()) {
                System.out.println("  [ ** EMPTY ** ]");
            } else {
                for (String cacheId : expectedIds) {
                    System.out.println("  [ " + cacheId + " ]");
                }
            }
        }

        assertTrue("Unexpected IDs", (faults == null));
    }

    /**
     * Verify that two sets have the same (equal) elements.
     *
     * The sets may be null. A null set does not match an empty set.
     * Two null sets match each other.
     *
     * A single fault is generated if one of the sets is null, and the other
     * is not null.
     *
     * One fault is generated if the sets have different sizes.
     *
     * One fault is generated for each missing element and for each extra
     * element of the "actual" set.
     *
     * If the sizes are different, at least two messages will be generated,
     * one to describe the size difference, and at least one for a missing or
     * an extra element.
     *
     * @param expected The "expected" elements.
     * @param actual   The "actual" elements.
     *
     * @return A list of faults found when comparing the two sets. Null
     *         if the sets have the same elements, or are both null.
     */
    protected static List<String> verify(Set<String> expected, Set<String> actual) {
        List<String> faults = null;

        int expectedSize;
        int actualSize;

        if (expected == null) {
            if (actual == null) {
                return null;
            } else {
                return Collections.singletonList("Expected [ ** UNSET ** ] but have [ " + actual.size() + " ]");
            }

        } else {
            expectedSize = expected.size();
            if (actual == null) {
                return Collections.singletonList("Expected [ " + expected + " ] but have [ ** UNSET ** ]");
            } else {
                actualSize = actual.size();
            }
        }

        if (expectedSize != actualSize) {
            faults = add(faults, "Expected [ " + expectedSize + " ] but have [ " + actualSize + " ]");
        }

        for (String expectedId : expected) {
            if (!actual.contains(expectedId)) {
                faults = add(faults, "Missing [ " + expectedId + " ]");
            }
        }

        for (String actualId : actual) {
            if (!expected.contains(actualId)) {
                faults = add(faults, "Extra [ " + actualId + " ]");
            }
        }

        return faults;
    }
}
