/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.fat;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

/**
 *
 */
@RunWith(FATRunner.class)
public class RealFlushTest {

    private static Class<?> c = RealFlushTest.class;

    private static String str8192;
    private static String str8193;

    @Server("RealFlushServer")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        generateStr8192();
        generateStr8193();
        addApp();
    }

    private static void generateStr8192() {
        String starter = "";
        String string = "R";
        for (int i = 0; i < 8192; i++) {
            starter = starter + string;
        }
        str8192 = starter;
    }

    private static void generateStr8193() {
        String starter = "";
        String string = "R";
        for (int i = 0; i < 8193; i++) {
            starter = starter + string;
        }
        str8193 = starter;
    }

    public static void addApp() throws Exception {

        ShrinkHelper.defaultDropinApp(server, "RealFlushTestApp", "com.ibm.ws.logging.flush.fat.printTestApp",
                                      "com.ibm.ws.logging.flush.fat.printBoolTests",
                                      "com.ibm.ws.logging.flush.fat.printCharArrayTests",
                                      "com.ibm.ws.logging.flush.fat.printCharTests",
                                      "com.ibm.ws.logging.flush.fat.printDoubleTests",
                                      "com.ibm.ws.logging.flush.fat.printFloatTests",
                                      "com.ibm.ws.logging.flush.fat.printIntTests",
                                      "com.ibm.ws.logging.flush.fat.printLongTests",
                                      "com.ibm.ws.logging.flush.fat.printObjectTests",
                                      "com.ibm.ws.logging.flush.fat.printStringTests");
        server.startServer();
    }

    private String getHttpServlet(String servletPath) throws Exception {
        HttpURLConnection con = null;
        try {
            String sURL = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + servletPath;
            URL checkerServletURL = new URL(sURL);
            con = (HttpURLConnection) checkerServletURL.openConnection();
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setUseCaches(false);
            con.setRequestMethod("GET");

            String sep = System.getProperty("line.separator");
            String line = null;
            StringBuilder lines = new StringBuilder();
            BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));

            while ((line = br.readLine()) != null && line.length() > 0) {
                lines.append(line).append(sep);
            }
            Log.info(c, "getHttpServlet", sURL);
            return lines.toString();
        } finally {
            if (con != null)
                con.disconnect();
        }
    }

    @Test
    public void printStringsTest() throws Exception {

        String testName = "printStringsTest";
        Log.info(c, testName, "------- This Section Will Print Varying Length Strings ------");
        Log.info(c, testName, "------- PrintlnString8192 ------");
        server.setMarkToEndOfLog();
        getHttpServlet("/RealFlushTestApp/printlnString8192/printlnString8192");
        Assert.assertNotNull("str8192 NOT FOUND", server.waitForStringInLogUsingMark(str8192));

        Log.info(c, testName, "------- PrintlnString8193 ------");
        server.setMarkToEndOfLog();
        getHttpServlet("/RealFlushTestApp/printlnString8193/printlnString8193");
        Assert.assertNotNull("str8193 NOT FOUND", server.waitForStringInLogUsingMark(str8193));

        Log.info(c, testName, "------- PrintlnStringSmall ------");
        server.setMarkToEndOfLog();
        getHttpServlet("/RealFlushTestApp/printlnStringSmall/printlnStringSmall");
        Assert.assertNotNull("smallStr NOT FOUND", server.waitForStringInLogUsingMark("smallStr"));

        Log.info(c, testName, "------- PrintString8192 ------");
        server.setMarkToEndOfLog();
        getHttpServlet("/RealFlushTestApp/printString8192/printString8192");
        Assert.assertNotNull("str8192 NOT FOUND", server.waitForStringInLogUsingMark(str8192));

        Log.info(c, testName, "------- PrintString8193 ------");
        server.setMarkToEndOfLog();
        getHttpServlet("/RealFlushTestApp/printString8193/printString8193");
        Assert.assertNotNull("str8193 NOT FOUND", server.waitForStringInLogUsingMark(str8193));

        Log.info(c, testName, "------- PrintStringSmall ------");
        server.setMarkToEndOfLog();
        getHttpServlet("/RealFlushTestApp/printStringSmall/printStringSmall");
        Assert.assertNotNull("smallStr NOT FOUND", server.waitForStringInLogUsingMark("smallStr"));

    }

    @Test
    public void printCharArrayTest() throws Exception {

        String testName = "printStringsTest";
        Log.info(c, testName, "------- This Section Will Print Varying Length char[] ------");
        Log.info(c, testName, "------- PrintlnCharArray8192 ------");
        server.setMarkToEndOfLog();
        getHttpServlet("/RealFlushTestApp/printlnCharArray8192/printlnCharArray8192");
        Assert.assertNotNull("str8192 NOT FOUND", server.waitForStringInLogUsingMark(str8192));

        Log.info(c, testName, "------- PrintlnCharArray8193 ------");
        server.setMarkToEndOfLog();
        getHttpServlet("/RealFlushTestApp/printlnCharArray8193/printlnCharArray8193");
        Assert.assertNotNull("str8193 NOT FOUND", server.waitForStringInLogUsingMark(str8193));

        Log.info(c, testName, "------- PrintlnCharArraySmall ------");
        server.setMarkToEndOfLog();
        getHttpServlet("/RealFlushTestApp/printlnCharArraySmall/printlnCharArraySmall");
        Assert.assertNotNull("smallStr NOT FOUND", server.waitForStringInLogUsingMark("smallStr"));

        Log.info(c, testName, "------- PrintCharArray8192 ------");
        server.setMarkToEndOfLog();
        getHttpServlet("/RealFlushTestApp/printCharArray8192/printCharArray8192");
        Assert.assertNotNull("str8192 NOT FOUND", server.waitForStringInLogUsingMark(str8192));

        Log.info(c, testName, "------- PrintCharArray8193 ------");
        server.setMarkToEndOfLog();
        getHttpServlet("/RealFlushTestApp/printCharArray8193/printCharArray8193");
        Assert.assertNotNull("str8193 NOT FOUND", server.waitForStringInLogUsingMark(str8193));

        Log.info(c, testName, "------- PrintCharArraySmall ------");
        server.setMarkToEndOfLog();
        getHttpServlet("/RealFlushTestApp/printCharArraySmall/printCharArraySmall");
        Assert.assertNotNull("smallStr NOT FOUND", server.waitForStringInLogUsingMark("smallStr"));

    }

    @Test
    public void printLongTest() throws Exception {

        String testName = "printLongTest";
        Log.info(c, testName, "------- This Section Will Print long Types ------");
        Log.info(c, testName, "------- PrintlnLongSmall ------");
        server.setMarkToEndOfLog();
        getHttpServlet("/RealFlushTestApp/printlnLongSmall/printlnLongSmall");
        Assert.assertNotNull("222222222 NOT FOUND", server.waitForStringInLogUsingMark("222222222"));

        Log.info(c, testName, "------- PrintLongSmall ------");
        server.setMarkToEndOfLog();
        getHttpServlet("/RealFlushTestApp/printLongSmall/printLongSmall");
        Assert.assertNotNull("222222222 NOT FOUND", server.waitForStringInLogUsingMark("222222222"));

    }

    @Test
    public void printIntTest() throws Exception {

        String testName = "printIntTest";
        Log.info(c, testName, "------- This Section Will Print int Types ------");
        Log.info(c, testName, "------- PrintlnIntSmall ------");
        server.setMarkToEndOfLog();
        getHttpServlet("/RealFlushTestApp/printlnIntSmall/printlnIntSmall");
        Assert.assertNotNull("222222222 NOT FOUND", server.waitForStringInLogUsingMark("222222222"));

        Log.info(c, testName, "------- PrintIntSmall ------");
        server.setMarkToEndOfLog();
        getHttpServlet("/RealFlushTestApp/printIntSmall/printIntSmall");
        Assert.assertNotNull("222222222 NOT FOUND", server.waitForStringInLogUsingMark("222222222"));

    }

    @Test
    public void printFloatTest() throws Exception {

        String testName = "printFloatTest";
        Log.info(c, testName, "------- This Section Will Print float Types ------");
        Log.info(c, testName, "------- PrintlnFloatSmall ------");
        server.setMarkToEndOfLog();
        getHttpServlet("/RealFlushTestApp/printlnFloatSmall/printlnFloatSmall");
        Assert.assertNotNull("222222222 NOT FOUND", server.waitForStringInLogUsingMark("2.22222224E8"));

        Log.info(c, testName, "------- PrintFloatSmall ------");
        server.setMarkToEndOfLog();
        getHttpServlet("/RealFlushTestApp/printFloatSmall/printFloatSmall");
        Assert.assertNotNull("222222222 NOT FOUND", server.waitForStringInLogUsingMark("2.22222224E8"));

    }

    @Test
    public void printDoubleTest() throws Exception {

        String testName = "printDoubleTest";
        Log.info(c, testName, "------- This Section Will Print double Types ------");
        Log.info(c, testName, "------- PrintlnDoubleSmall ------");
        server.setMarkToEndOfLog();
        getHttpServlet("/RealFlushTestApp/printlnDoubleSmall/printlnDoubleSmall");
        Assert.assertNotNull("222222222 NOT FOUND", server.waitForStringInLogUsingMark("2.22222222E8"));

        Log.info(c, testName, "------- PrintDoubleSmall ------");
        server.setMarkToEndOfLog();
        getHttpServlet("/RealFlushTestApp/printDoubleSmall/printDoubleSmall");
        Assert.assertNotNull("222222222 NOT FOUND", server.waitForStringInLogUsingMark("2.22222222E8"));

    }

    @Test
    public void printCharTest() throws Exception {

        String testName = "printCharTest";
        Log.info(c, testName, "------- This Section Will Print char Types ------");
        Log.info(c, testName, "------- PrintlnCharSmall ------");
        server.setMarkToEndOfLog();
        getHttpServlet("/RealFlushTestApp/printlnChar/printlnChar");
        Assert.assertNotNull("z NOT FOUND", server.waitForStringInLogUsingMark("z"));

        Log.info(c, testName, "------- PrintCharSmall ------");
        server.setMarkToEndOfLog();
        getHttpServlet("/RealFlushTestApp/printChar/printChar");
        Assert.assertNotNull("z NOT FOUND", server.waitForStringInLogUsingMark("z"));

    }

    @Test
    public void printBoolTest() throws Exception {

        String testName = "printBoolTest";
        Log.info(c, testName, "------- This Section Will Print bool Types ------");
        Log.info(c, testName, "------- PrintFalse ------");
        server.setMarkToEndOfLog();
        getHttpServlet("/RealFlushTestApp/printFalse/printFalse");
        Assert.assertNotNull("false NOT FOUND", server.waitForStringInLogUsingMark("false"));

        Log.info(c, testName, "------- PrintlnFalse ------");
        server.setMarkToEndOfLog();
        getHttpServlet("/RealFlushTestApp/printlnFalse/printlnFalse");
        Assert.assertNotNull("false NOT FOUND", server.waitForStringInLogUsingMark("false"));

        Log.info(c, testName, "------- PrintTrue ------");
        server.setMarkToEndOfLog();
        getHttpServlet("/RealFlushTestApp/printTrue/printTrue");
        Assert.assertNotNull("true NOT FOUND", server.waitForStringInLogUsingMark("true"));

        Log.info(c, testName, "------- PrintlnTrue ------");
        server.setMarkToEndOfLog();
        getHttpServlet("/RealFlushTestApp/printlnTrue/printlnTrue");
        Assert.assertNotNull("true NOT FOUND", server.waitForStringInLogUsingMark("true"));

    }

    @Test
    public void printObjectTest() throws Exception {

        String testName = "printObjectTest";
        Log.info(c, testName, "------- This Section Will Print Varying Length Object.toString() Strings ------");
        Log.info(c, testName, "------- PrintlnObject8192 ------");
        server.setMarkToEndOfLog();
        getHttpServlet("/RealFlushTestApp/printlnObject8192/printlnObject8192");
        Assert.assertNotNull("str8192 NOT FOUND", server.waitForStringInLogUsingMark(str8192));

        Log.info(c, testName, "------- PrintlnObject8193 ------");
        server.setMarkToEndOfLog();
        getHttpServlet("/RealFlushTestApp/printlnObject8193/printlnObject8193");
        Assert.assertNotNull("str8193 NOT FOUND", server.waitForStringInLogUsingMark(str8193));

        Log.info(c, testName, "------- PrintObject8192 ------");
        server.setMarkToEndOfLog();
        getHttpServlet("/RealFlushTestApp/printObject8192/printObject8192");
        Assert.assertNotNull("str8192 NOT FOUND", server.waitForStringInLogUsingMark(str8192));

        Log.info(c, testName, "------- PrintObject8193 ------");
        server.setMarkToEndOfLog();
        getHttpServlet("/RealFlushTestApp/printObject8193/printObject8193");
        Assert.assertNotNull("str8193 NOT FOUND", server.waitForStringInLogUsingMark(str8193));

        Log.info(c, testName, "------- PrintObjectSmall ------");
        server.setMarkToEndOfLog();
        getHttpServlet("/RealFlushTestApp/printObjectSmall/printObjectSmall");
        Assert.assertNotNull("smallStr NOT FOUND", server.waitForStringInLogUsingMark("smallStr"));

        Log.info(c, testName, "------- PrintlnObjectSmall ------");
        server.setMarkToEndOfLog();
        getHttpServlet("/RealFlushTestApp/printlnObjectSmall/printlnObjectSmall");
        Assert.assertNotNull("smallStr NOT FOUND", server.waitForStringInLogUsingMark("smallStr"));

    }

    @After
    public void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.removeAllInstalledAppsForValidation();
        }
    }
}
