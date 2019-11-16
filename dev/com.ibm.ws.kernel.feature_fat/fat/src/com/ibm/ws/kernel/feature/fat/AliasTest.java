package com.ibm.ws.kernel.feature.fat;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

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
    }

    @AfterClass
    public static void uninstallFeatures() throws Exception {
        server.uninstallSystemFeature("test.alias.public.system-1.0");
        server.uninstallSystemBundle("test.alias.public.system");
        server.uninstallUserFeature("test.alias.public.user-1.0");
        server.uninstallUserBundle("test.alias.public.user");
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
        server.startServer(true); // init feature cache
        msg = server.waitForStringInLogUsingMark("test.alias.public.system", shortTimeOut);
        assertNotNull("The server provisioned feature bundle test.alias.public.system", msg);
        msg = server.waitForStringInLogUsingMark("CWWKF0012I:.*system-1.0", shortTimeOut); // short name!
        assertNotNull("The feature manager installed feature system-1.0", msg);
        server.stopServer();

        // Install short alias on static update
        server.changeFeatures(Arrays.asList("myorgSystem-1.0")); // short alias!
        server.startServer(); // use feature cache
        msg = server.waitForStringInLogUsingMark("test.alias.public.system", shortTimeOut);
        assertNotNull("The server provisioned feature bundle test.alias.public.system", msg);
        msg = server.waitForStringInLogUsingMark("CWWKF0012I:.*system-1.0", shortTimeOut);
        assertNotNull("The feature manager installed system-1.0 using its short alias", msg);

        // Symbolic alias must not trigger reinstall on dynamic update
        server.setMarkToEndOfLog();
        server.changeFeatures(Arrays.asList("com.myorg.test.alias.public.system-1.0")); // symbolic alias!
        server.waitForConfigUpdateInLogUsingMark(Collections.<String> emptySet());
        msg = server.waitForStringInLogUsingMark("CWWKF0012I:", shortTimeOut);
        assertNull("The feature manager did not install any features on dynamic update of its symbolic alias", msg);

        // Short name must not trigger reinstall on dynamic update
        server.setMarkToEndOfLog();
        server.changeFeatures(Arrays.asList("system-1.0")); // short name
        server.waitForConfigUpdateInLogUsingMark(Collections.<String> emptySet());
        msg = server.waitForStringInLogUsingMark("CWWKF0012I:", shortTimeOut);
        assertNull("The feature manager did not install any features on dynamic update of its short name", msg);

        // Remove feature on dynamic update
        server.setMarkToEndOfLog();
        server.changeFeatures(Arrays.asList("jsf-2.3")); // Nod to the future
        server.waitForConfigUpdateInLogUsingMark(Collections.<String> emptySet());
        msg = server.waitForStringInLogUsingMark("CWWKF0013I:.*system-1.0");
        assertNotNull("The feature manager uninstalled system-1.0, previously installed using its short alias", msg);

        // Install symbolic alias on dynamic update
        server.setMarkToEndOfLog();
        server.changeFeatures(Arrays.asList("com.myorg.test.alias.public.system-1.0"));
        server.waitForConfigUpdateInLogUsingMark(Collections.<String> emptySet());
        assertNotNull("The server provisioned feature bundle test.alias.public.system", msg);
        msg = server.waitForStringInLogUsingMark("CWWKF0012I:.*system-1.0");
        assertNotNull("The feature manager (re)installed system-1.0 on dynamic update using its symbolic alias", msg);
    }

    @Test
    public void testUserAliasesNotSupported() throws Exception {
        // Install symbolic name on clean start
        server.changeFeatures(Arrays.asList("test.alias.public.user-1.0")); // symbolic name!
        server.startServer(true); //
        msg = server.waitForStringInLogUsingMark("test.alias.public.user", shortTimeOut);
        assertNotNull("The server provisioned user feature bundle test.alias.public.user", msg);
        msg = server.waitForStringInLogUsingMark("CWWKF0012I:.*usr:user-1.0", shortTimeOut); // short name!
        assertNotNull("The feature manager installed user feature user-1.0", msg);
        server.stopServer();

        // Aliases are not supported for user features (static update)
        server.changeFeatures(Arrays.asList("usr:myorgUser-1.0")); // short alias!
        server.startServer(true); // use feature cache
        msg = server.waitForStringInLogUsingMark("test.alias.public.user", shortTimeOut);
        assertNull("The server did not provision feature bundle test.alias.public.user", msg);
        msg = server.waitForStringInLogUsingMark("CWWKF0001E:.*usr:myorguser-1.0", shortTimeOut);
        assertNotNull("The feature manager did not find a definition for user feature user-1.0 with short alias myorgUser-1.0", msg);
        server.stopServer("CWWKF0001E");
    }

    @Test
    public void testEquivalentFeatureNameAndAlias() throws Exception {
        // Ignore the equivalent names w/o incident and install the feature
        server.changeFeatures(Arrays.asList("test.alias.public.system-1.0", "myorgSystem-1.0")); // same feature
        server.startServer();
        msg = server.waitForStringInLogUsingMark("test.alias.public.system", shortTimeOut);
        assertNotNull("The server provisioned feature bundle test.alias.public.system", msg);
        msg = server.waitForStringInLogUsingMark("CWWKF0012I:.*system-1.0", shortTimeOut); // short name!
        assertNotNull("The feature manager installed feature system-1.0", msg);
    }

}