/**
 *
 */
package com.ibm.ws.jaxrs20.fat.resourceinfo;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.jaxrs20.fat.AbstractTest;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

/**
 * Tests {@code ResourceInfo} methods {@code getResourceClass} and {@code getResourceMethod}.
 */
@RunWith(FATRunner.class)
public class ResourceInfoTest extends AbstractTest {

    @Server("com.ibm.ws.jaxrs.fat.resourceinfo")
    public static LibertyServer server;

    private static final String war = "resourceinfo";
    private final String target = war + "/TestServlet";

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultDropinApp(server, war, "com.ibm.ws.jaxrs.fat.resourceinfo");

        // Make sure we don't fail because we try to start an
        // already started server
        try {
            server.startServer(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null) {
            server.stopServer();
        }
    }

    @Before
    public void preTest() {
        serverRef = server;
    }

    @After
    public void afterTest() {
        serverRef = null;
    }

    @Test
    public void testSimpleResource() throws Exception {
        this.runTestOnServer(target, "testSimpleResource", null, "OK");
    }

    @Test
    public void testAbstractAndSubClassResource() throws Exception {
        this.runTestOnServer(target, "testAbstractAndSubClassResource", null, "OK");
    }

    @Test
    public void testInterfaceAndImplClassResource() throws Exception {
        this.runTestOnServer(target, "testInterfaceAndImplClassResource", null, "OK");
    }
}