package com.ibm.ws.transaction.test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.transactionalEJB.web.TransactionalEJBTestServlet;

import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

// TODO - needs to be enabled when ejb-4.0 is ready
@RunWith(FATRunner.class)
@SkipForRepeat({ SkipForRepeat.EE9_FEATURES })
public class TransactionalEJBTest extends FATServletClient {

    public static final String APP_NAME = "transactionalEJB";
    public static final String SERVLET_NAME = APP_NAME + "/transactionalEJB";

    private final long TIMEOUT = 10000; // should have failed very fast

    @Server("com.ibm.ws.transactional")
    @TestServlet(servlet = TransactionalEJBTestServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    @Test
    public void testNoTransactionalEJB() throws Exception {
        // Check transactionalEJB app didn't start
        String noTransactionsAllowedMessage = server.waitForStringInLog("CWOWB2000E", TIMEOUT);
        assertNotNull("TestEJB did not fail to load", noTransactionsAllowedMessage);
        assertTrue("The message should contain the annnotation name ", noTransactionsAllowedMessage.contains("@javax.transaction.Transactional"));
        assertTrue("The message should contain the EJB name ", noTransactionsAllowedMessage.contains("TestEJB"));
    }

    @AfterClass
    public static void teardown() throws Exception {
        server.stopServer("CWWKZ0002E");
        ShrinkHelper.cleanAllExportedArchives();
    }

    @BeforeClass
    public static void setup() throws Exception {
        ShrinkHelper.defaultDropinApp(server, APP_NAME, "com.ibm.ws.transactionalEJB.*");
        server.setServerStartTimeout(600000);
        LibertyServer.setValidateApps(false);
        server.startServer(true);
    }
}