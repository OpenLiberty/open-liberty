package com.ibm.ws.kernel.feature.fat;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@RunWith(FATRunner.class)
public class AliasTest {

    private static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.kernel.feature.alias");

    private static String msg;

    private static long shortTimeOut = 3 * 1000;

    @BeforeClass
    public static void installFeatures() throws Exception {
        server.installSystemFeature("test.alias.public.system-1.0");
        server.installSystemBundle("test.alias.public.system");
        server.installUserFeature("test.alias.public.user-1.0");
        server.installUserBundle("test.alias.public.user");
        server.installSystemFeature("test.alias.auto.internal-1.0");
        server.installSystemBundle("test.alias.auto.internal");
    }

    @AfterClass
    public static void uninstallFeatures() throws Exception {
        server.uninstallSystemFeature("test.alias.public.system-1.0");
        server.uninstallSystemBundle("test.alias.public.system");

        server.uninstallUserFeature("test.alias.public.user-1.0");
        server.uninstallUserBundle("test.alias.public.user");
        server.uninstallSystemFeature("test.alias.auto.internal-1.0");
        server.uninstallSystemBundle("test.alias.auto.internal");
    }

    @After
    public void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    @Test
    public void testPublicAliases() throws Exception {

        // Install symbolic name on clean start
        server.changeFeatures(Arrays.asList("test.alias.public.system-1.0")); // symbolic name!
        server.startServer(); // clean start - reset config, bundle, feature caches
        msg = server.waitForStringInLogUsingMark("test.alias.public.system", shortTimeOut);
        assertNotNull("The server should provision feature bundle test.alias.public.system, but did not", msg);
        msg = server.waitForStringInLogUsingMark("CWWKF0012I:", shortTimeOut); // short name!
        assertTrue("The feature manager should install feature system-1.0, but did not: msg=" + msg,
                   msg != null && msg.contains("system-1.0"));
        server.stopServer();

        // Install short alias on static update
        server.changeFeatures(Arrays.asList("myorgSystem-1.0")); // short alias!
        server.startServer(false); // use feature cache
        msg = server.waitForStringInLogUsingMark("test.alias.public.system", shortTimeOut);
        assertNotNull("The server should provision feature bundle test.alias.public.system, but did not", msg);
        msg = server.waitForStringInLogUsingMark("CWWKF0012I:", shortTimeOut);
        assertTrue("The feature manager should install system-1.0 using the short alias, but did not: msg=" + msg,
                   msg != null && msg.contains("system-1.0"));

        // NOTICE THE TESTS CHECK FOR SYMBOLIC OR SHORT NAME IN CWWK0012I MESSAGE.
        // FM IS NOT MODIFIED TO EMIT ALIASES, EVEN WHEN CONFIGURED IN SERVER.XML

        // ISSUE: WE SHOULD EMIT ALIASES IN MSGS WHEN CONFIGURED BY USER IN SERVER.XML,
        // BUT ARE WE ALLOWED TO?

        // Symbolic alias must not trigger reinstall on dynamic update
        server.setMarkToEndOfLog();
        server.changeFeatures(Arrays.asList("com.myorg.test.alias.public.system-1.0")); // symbolic alias!
        server.waitForConfigUpdateInLogUsingMark(Collections.<String> emptySet());
        msg = server.waitForStringInLogUsingMark("CWWKF0012I:", shortTimeOut);
        assertNull("The feature manager should not install any features on dynamic update of this symbolic alias, but it did: msg=" + msg, msg);

        // Short name must not trigger reinstall on dynamic update
        server.setMarkToEndOfLog();
        server.changeFeatures(Arrays.asList("system-1.0")); // short name
        server.waitForConfigUpdateInLogUsingMark(Collections.<String> emptySet());
        msg = server.waitForStringInLogUsingMark("CWWKF0012I:", shortTimeOut);
        assertNull("The feature manager should not install any features on dynamic update of this short name, but it did: msg=" + msg, msg);

        // Remove aliased feature on dynamic update
        server.setMarkToEndOfLog();
        server.changeFeatures(Arrays.asList("jsf-2.3")); // Nod to the future
        server.waitForConfigUpdateInLogUsingMark(Collections.<String> emptySet());
        msg = server.waitForStringInLogUsingMark("CWWKF0013I:");
        assertTrue("The feature manager should uninstall system-1.0, previously installed using the short alias, but did not: msg=" + msg,
                   msg != null && msg.contains("system-1.0"));

        // Install symbolic alias on dynamic update
        server.setMarkToEndOfLog();
        server.changeFeatures(Arrays.asList("com.myorg.test.alias.public.system-1.0"));
        server.waitForConfigUpdateInLogUsingMark(Collections.<String> emptySet());
        assertNotNull("The server should provision feature bundle test.alias.public.system, but did not", msg);
        msg = server.waitForStringInLogUsingMark("CWWKF0012I:");
        assertTrue("The feature manager should (re)install system-1.0 on dynamic update using the symbolic alias, but did not: msg=" + msg,
                   msg != null && msg.contains("system-1.0"));
    }

    @Test
    public void testUserAliasesNotSupported() throws Exception {

        // Install symbolic name on clean start
        server.changeFeatures(Arrays.asList("test.alias.public.user-1.0")); // symbolic name, public!
        server.startServer(); // clean start
        msg = server.waitForStringInLogUsingMark("test.alias.public.user", shortTimeOut);
        assertNotNull("The server should provision user feature bundle test.alias.public.user, but did not", msg);
        msg = server.waitForStringInLogUsingMark("CWWKF0012I:", shortTimeOut); // short name!
        assertTrue("The feature manager should install user feature user-1.0, but did not: msg=" + msg,
                   msg != null && msg.contains("usr:user-1.0"));
        server.stopServer();

        // Aliases not supported for user features (static update)
        server.changeFeatures(Arrays.asList("usr:myorgUser-1.0")); // short alias!
        server.startServer(false); // use feature cache
        msg = server.waitForStringInLogUsingMark("test.alias.public.user", shortTimeOut);
        assertNull("The server should not provision feature bundle test.alias.public.user, but it did: msg=" + msg, msg);
        msg = server.waitForStringInLogUsingMark("CWWKF0001E:", shortTimeOut);
        assertTrue("The feature manager should not find a definition for user feature user-1.0 with short alias myorgUser-1.0, but it did: msg=" + msg,
                   msg != null && msg.contains("myorguser-1.0"));
        server.stopServer("CWWKF0001E");
    }

    @Test
    public void testEquivalentFeatureNameAndAlias() throws Exception {

        // Ignore the equivalent names w/o incident and install the feature
        server.changeFeatures(Arrays.asList("test.alias.public.system-1.0", "myorgSystem-1.0")); // same feature
        server.startServer();
        msg = server.waitForStringInLogUsingMark("test.alias.public.system", shortTimeOut);
        assertNotNull("The server should provision feature bundle test.alias.public.system, but did not", msg);
        msg = server.waitForStringInLogUsingMark("CWWKF0012I:", shortTimeOut); // short name!
        assertTrue("The feature manager should install feature system-1.0, but did not: msg=" + msg,
                   msg != null && msg.contains("system-1.0"));

        // ADD TEST FOR EQUIVALENT SINGLETON FEATURE, THEN EITHER 1) MODIFY TEST TO CHECK FOR
        // MESSAGE CWWKF0033E AND CHANGE FM TO EMIT FEATURE NAME ALIAS RATHER THAN NULL, OR
        // 2) CHECK THAT THE FEATURE INSTALLED AND MODIFY FM TO INSTALL THE SINGLETON FEATURE
        // RATHER THAN IGNORE IT AND EMIT CWWKF0033E.  EITHER WAY, ADD DEBUG TO FM.

        // ISSUE: HOW TO HANDLE EQUIVALENT FEATURE NAMES IN SERVER.XML. FM CURRENTLY IGNORES
        // THE ALIAS UNLESS FEATURE IS SINGLETION (GROAN)

        // ISSUE: WHETHER TO EMIT ALIAS NAMES TO MESSAGES WHEN ALIAS APPEARS IN SERVER.XML.
    }

    @Test
    public void testAliasesSatisfyAutoFeatureCapability() throws Exception {

        // Configure feature(s) w/ short alias that satisfies auto feature capability
        server.changeFeatures(Arrays.asList("myorgSystem-1.0", "jsf-2.3")); // Alias for system-1.0
        server.startServer();
        msg = server.waitForStringInLogUsingMark("test.alias.auto.internal", shortTimeOut);
        assertNotNull("The server should provision auto feature bundle test.alias.auto.internal, but did not", msg);
        msg = server.waitForStringInLogUsingMark("CWWKF0012I:", shortTimeOut);
        assertTrue("The feature manager should install autp feature test.alias.auto.internal-1.0, but did not: msg=" + msg,
                   msg != null && msg.contains("test.alias.auto.internal-1.0"));
        server.stopServer();

        // Configure feature(s) w/ symbolic alias that satisfies auto feature capability
        server.changeFeatures(Arrays.asList("com.myorg.test.alias.public.system-1.0", "jsf-2.3")); // Alias for system-1.0
        server.startServer();
        msg = server.waitForStringInLogUsingMark("test.alias.auto.internal", shortTimeOut);
        assertNotNull("The server should provision auto feature bundle test.alias.auto.internal, but did not", msg);
        msg = server.waitForStringInLogUsingMark("CWWKF0012I:", shortTimeOut);
        assertTrue("The feature manager should install auto feature test.alias.auto.internal-1.0, but did not: msg=" + msg,
                   msg != null && msg.contains("test.alias.auto.internal-1.0"));

        // NO NEED TO TEST SCENARIOS WHERE AUTO-FEATURE DECLARES AN ALIASED CAPABILITY.
        // AUTO FEATURE CAPABILITIES ARE INTERNALS THAT WILL CHANGE ALONG WITH FEATURE
        // NAMES.

        // ISSUE: DO WE SUPPORT SYMBOLIC ALIASES APPEARING IN AUTO-FEATURE CAPABILITIES?  WE
        // MUST SUPPORT ALIASES IN SUBSYSTEM-CONTENT
    }

}