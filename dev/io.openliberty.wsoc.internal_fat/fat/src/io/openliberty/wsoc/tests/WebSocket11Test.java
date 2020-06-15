/*******************************************************************************
 * NOTICES AND INFORMATION FOR Open Liberty
 * Copyright 2015, 2020 IBM Corporation and others
 * This product includes software developed at
 * The Open Liberty Project (https://openliberty.io/).
 *******************************************************************************/
package io.openliberty.wsoc.tests;

import java.util.logging.Logger;
import java.util.Set;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.rules.TestRule;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.fat.util.LoggingTest;
import com.ibm.ws.fat.util.SharedServer;

import io.openliberty.wsoc.util.OnlyRunNotOnZRule;
import io.openliberty.wsoc.util.WebServerSetup;
import io.openliberty.wsoc.util.wsoc.WsocTest;
import io.openliberty.wsoc.tests.all.WebSocketVersion11Test;
import componenttest.annotation.MinimumJavaLevel;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

/**
 * Tests WebSocket Stuff
 * 
 * @author unknown
 */
@RunWith(FATRunner.class)
@MinimumJavaLevel(javaLevel = 7)
public class WebSocket11Test extends LoggingTest {

    @ClassRule
    public static SharedServer SS = new SharedServer("webSocket11Server", false);

    private static WebServerSetup bwst = new WebServerSetup(SS);

    @Rule
    public final TestRule notOnZRule = new OnlyRunNotOnZRule();

    private final WsocTest wt = new WsocTest(SS, false);

    private final WebSocketVersion11Test pt = new WebSocketVersion11Test(wt);

    private static final Logger LOG = Logger.getLogger(WebSocket11Test.class.getName());

    @BeforeClass
    public static void setUp() throws Exception {
        bwst.setUp();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        bwst.tearDown();
    }

    //
    //
    // WebSocket 1.1 TESTS
    //
    //

    @Mode(TestMode.LITE)
    @Test
    public void testProgrammaticTextSuccess() throws Exception {
        pt.testProgrammaticTextSuccess();
    }

    @Mode(TestMode.LITE)
    @Test
    public void testProgrammaticReaderSuccess() throws Exception {
        pt.testProgrammaticReaderSuccess();
    }

    @Mode(TestMode.LITE)
    @Test
    public void testProgrammaticPartialTextSuccess() throws Exception {
        pt.testProgrammaticPartialTextSuccess();
    }

    @Mode(TestMode.LITE)
    @Test
    public void testClientAnnoWholeServerProgPartial() throws Exception {
        pt.testClientAnnoWholeServerProgPartial();
    }

    @Mode(TestMode.LITE)
    @Test
    public void testProgrammaticInputStreamSuccess() throws Exception {
        pt.testProgrammaticInputStreamSuccess();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.fat.util.LoggingTest#getSharedServer()
     */
    @Override
    protected SharedServer getSharedServer() {
        return SS;
    }

}