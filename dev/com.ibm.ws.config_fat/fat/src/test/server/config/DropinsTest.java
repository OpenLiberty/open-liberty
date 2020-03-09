/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.server.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.ExpectedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@RunWith(FATRunner.class)
public class DropinsTest extends ServletRunner {

    private static final String CONTEXT_ROOT = "configdropins";

    @Override
    protected String getContextRoot() {
        return CONTEXT_ROOT;
    }

    @Override
    protected String getServletMapping() {
        return "dropinsTest";
    }

    private static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.config.dropins");

    @Test
    public void testNonXmlFile() throws Exception {
        try {
            server.setMarkToEndOfLog();
            server.addDropinDefaultConfiguration("dropins/simple.xml");
            assertTrue(
                "Message was not detected in the log",
                !server.waitForConfigUpdateInLogUsingMark(null).isEmpty());
            server.setMarkToEndOfLog();
            server.addDropinOverrideConfiguration("dropins/simple.notxml");
            assertTrue(
                "Message was not detected in the log",
                !server.waitForConfigUpdateInLogUsingMark(null).isEmpty());

            test(server);
        } finally {
            server.setMarkToEndOfLog();
            server.deleteDropinDefaultConfiguration("simple.xml");
            assertTrue(
                "Message was not detected in the log",
                !server.waitForConfigUpdateInLogUsingMark(null).isEmpty());
            server.setMarkToEndOfLog();
            server.deleteDropinOverrideConfiguration("simple.notxml");
            assertTrue(
                "Message was not detected in the log",
                !server.waitForConfigUpdateInLogUsingMark(null).isEmpty());
        }
    }

    @Test
    @ExpectedFFDC("com.ibm.websphere.config.ConfigParserException")
    public void testBrokenDropin() throws Exception {
        try {
            server.setMarkToEndOfLog();
            server.addDropinOverrideConfiguration("dropins/simple.xml");
            assertTrue(
                "Message was not detected in the log",
                !server.waitForConfigUpdateInLogUsingMark(null).isEmpty());
            server.addDropinDefaultConfiguration("dropins/aBrokenFile.xml");
            assertTrue(
                "Message was not detected in the log",
                !server.waitForConfigUpdateInLogUsingMark(null).isEmpty());
            server.addDropinOverrideConfiguration("dropins/aBrokenFile.xml");
            assertTrue(
                "Message was not detected in the log",
                !server.waitForConfigUpdateInLogUsingMark(null).isEmpty());
            assertEquals("There should be two CWWKG0014E errors", 2, server.waitForMultipleStringsInLog(2, "CWWKG0014E"));

            test(server);
        } finally {
            server.setMarkToEndOfLog();
            server.deleteDropinOverrideConfiguration("simple.xml");
            assertTrue(
                "Message was not detected in the log",
                !server.waitForConfigUpdateInLogUsingMark(null).isEmpty());
            server.setMarkToEndOfLog();
            server.deleteDropinDefaultConfiguration("aBrokenFile.xml");
            assertTrue(
                "Message was not detected in the log",
                !server.waitForConfigUpdateInLogUsingMark(null).isEmpty());
            server.setMarkToEndOfLog();
            server.deleteDropinOverrideConfiguration("aBrokenFile.xml");
            assertTrue(
                "Message was not detected in the log",
                !server.waitForConfigUpdateInLogUsingMark(null).isEmpty());
            server.setMarkToEndOfLog();
        }
    }

    @Test
    public void testSimpleDefaults() throws Exception {
        try {
            server.setMarkToEndOfLog();
            server.addDropinDefaultConfiguration("dropins/simple.xml");
            assertTrue(
                "Message was not detected in the log",
                !server.waitForConfigUpdateInLogUsingMark(null).isEmpty());

            test(server);
        } finally {
            server.setMarkToEndOfLog();
            server.deleteDropinDefaultConfiguration("simple.xml");
            assertTrue(
                "Message was not detected in the log",
                !server.waitForConfigUpdateInLogUsingMark(null).isEmpty());
        }
    }

    @Test
    public void testSimpleOverrides() throws Exception {
        try {
            server.setMarkToEndOfLog();
            server.addDropinOverrideConfiguration("dropins/simple.xml");
            assertTrue(
                "Message was not detected in the log",
                !server.waitForConfigUpdateInLogUsingMark(null).isEmpty());

            test(server);
        } finally {
            server.setMarkToEndOfLog();
            server.deleteDropinOverrideConfiguration("simple.xml");
            assertTrue(
                "Message was not detected in the log",
                !server.waitForConfigUpdateInLogUsingMark(null).isEmpty());
        }
    }

    @Test
    public void testSimpleOverrides2() throws Exception {
        try {
            server.setMarkToEndOfLog();
            server.addDropinOverrideConfiguration("dropins/simple.xml");
            assertTrue(
                "Message was not detected in the log",
                !server.waitForConfigUpdateInLogUsingMark(null).isEmpty());
            server.setMarkToEndOfLog();
            server.addDropinOverrideConfiguration("dropins/simple2.xml");
            assertTrue(
                "Message was not detected in the log",
                !server.waitForConfigUpdateInLogUsingMark(null).isEmpty());

            test(server);
        } finally {
            server.setMarkToEndOfLog();
            server.deleteDropinOverrideConfiguration("simple.xml");
            assertTrue(
                "Message was not detected in the log",
                !server.waitForConfigUpdateInLogUsingMark(null).isEmpty());
            server.setMarkToEndOfLog();
            server.deleteDropinOverrideConfiguration("simple2.xml");
            assertTrue(
                "Message was not detected in the log",
                !server.waitForConfigUpdateInLogUsingMark(null).isEmpty());
        }
    }

    @Test
    public void testDefaultsOrdering() throws Exception {
        try {
            server.setMarkToEndOfLog();
            server.addDropinDefaultConfiguration("dropins/simple.xml");
            assertTrue(
                "Message was not detected in the log",
                !server.waitForConfigUpdateInLogUsingMark(null).isEmpty());
            server.setMarkToEndOfLog();
            server.addDropinDefaultConfiguration("dropins/simple2.xml");
            assertTrue(
                "Message was not detected in the log",
                !server.waitForConfigUpdateInLogUsingMark(null).isEmpty());

            test(server);
        } finally {
            server.setMarkToEndOfLog();
            server.deleteDropinDefaultConfiguration("simple.xml");
            assertTrue(
                "Message was not detected in the log",
                !server.waitForConfigUpdateInLogUsingMark(null).isEmpty());
            server.setMarkToEndOfLog();
            server.deleteDropinDefaultConfiguration("simple2.xml");
            assertTrue(
                "Message was not detected in the log",
                !server.waitForConfigUpdateInLogUsingMark(null).isEmpty());
        }
    }

    @Test
    public void testOverridesOrdering() throws Exception {
        try {
            server.setMarkToEndOfLog();
            server.addDropinOverrideConfiguration("dropins/simple.xml");
            assertTrue(
                "Message was not detected in the log",
                !server.waitForConfigUpdateInLogUsingMark(null).isEmpty());
            server.setMarkToEndOfLog();
            server.addDropinOverrideConfiguration("dropins/simple2.xml");
            assertTrue(
                "Message was not detected in the log",
                !server.waitForConfigUpdateInLogUsingMark(null).isEmpty());

            test(server);
        } finally {
            server.setMarkToEndOfLog();
            server.deleteDropinOverrideConfiguration("simple.xml");
            assertTrue(
                "Message was not detected in the log",
                !server.waitForConfigUpdateInLogUsingMark(null).isEmpty());
            server.setMarkToEndOfLog();
            server.deleteDropinOverrideConfiguration("simple2.xml");
            assertTrue(
                "Message was not detected in the log",
                !server.waitForConfigUpdateInLogUsingMark(null).isEmpty());
        }
    }

    @Test
    public void testNoServerValue1() throws Exception {
        try {
            server.setMarkToEndOfLog();
            server.addDropinDefaultConfiguration("dropins/alibrary.xml");
            assertTrue(
                "Message was not detected in the log",
                !server.waitForConfigUpdateInLogUsingMark(null).isEmpty());

            test(server);
        } finally {
            server.setMarkToEndOfLog();
            server.deleteDropinDefaultConfiguration("alibrary.xml");
            assertTrue(
                "Message was not detected in the log",
                !server.waitForConfigUpdateInLogUsingMark(null).isEmpty());
        }
    }

    @Test
    public void testNoServerValue2() throws Exception {
        try {
            server.setMarkToEndOfLog();
            server.addDropinOverrideConfiguration("dropins/alibrary.xml");
            assertTrue(
                "Message was not detected in the log",
                !server.waitForConfigUpdateInLogUsingMark(null).isEmpty());

            test(server);
        } finally {
            server.setMarkToEndOfLog();
            server.deleteDropinOverrideConfiguration("alibrary.xml");
            assertTrue(
                "Message was not detected in the log",
                !server.waitForConfigUpdateInLogUsingMark(null).isEmpty());
        }
    }

    @Test
    public void testNoServerValue3() throws Exception {
        try {
            server.setMarkToEndOfLog();
            server.addDropinDefaultConfiguration("dropins/alibrary.xml");
            assertTrue(
                "Message was not detected in the log",
                !server.waitForConfigUpdateInLogUsingMark(null).isEmpty());
            server.setMarkToEndOfLog();
            server.addDropinOverrideConfiguration("dropins/blibrary.xml");
            assertTrue(
                "Message was not detected in the log",
                !server.waitForConfigUpdateInLogUsingMark(null).isEmpty());

            test(server);
        } finally {
            server.setMarkToEndOfLog();
            server.deleteDropinDefaultConfiguration("alibrary.xml");
            assertTrue(
                "Message was not detected in the log",
                !server.waitForConfigUpdateInLogUsingMark(null).isEmpty());
            server.setMarkToEndOfLog();
            server.deleteDropinOverrideConfiguration("blibrary.xml");
            assertTrue(
                "Message was not detected in the log",
                !server.waitForConfigUpdateInLogUsingMark(null).isEmpty());
        }
    }

    @BeforeClass
    public static void setUpForDropinsTests() throws Exception {
        //copy the feature into the server features location

        server.copyFileToLibertyInstallRoot("lib/features", "internalFeatureForFat/configfatlibertyinternals-1.0.mf");

        // Delete dropin configurations just in case
        server.deleteAllDropinConfigurations();

        WebArchive dropinsApp = ShrinkHelper.buildDefaultApp("configdropins", "test.config.dropins");
        ShrinkHelper.exportAppToServer(server, dropinsApp);

        server.startServer("configDropins.log");

        //make sure the URL is available
        assertNotNull(server.waitForStringInLog("CWWKT0016I.*" + CONTEXT_ROOT));
        assertNotNull(server.waitForStringInLog("CWWKF0011I"));

    }

    @AfterClass
    public static void shutdown() throws Exception {
        server.stopServer("CWWKG0014E");
        server.deleteFileFromLibertyInstallRoot("lib/features/configfatlibertyinternals-1.0.mf");

    }

}
