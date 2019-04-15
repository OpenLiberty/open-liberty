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
        getHttpServlet("/RealFlushTestApp/printStringLongTests/printlnString8192");
        Assert.assertNotNull("str8192 NOT FOUND", server.waitForStringInLogUsingMark(str8192));

        Log.info(c, testName, "------- PrintlnString8193 ------");
        server.setMarkToEndOfLog();
        getHttpServlet("/RealFlushTestApp/printStringLongTests/printlnString8193");
        Assert.assertNotNull("str8193 NOT FOUND", server.waitForStringInLogUsingMark(str8193));

        Log.info(c, testName, "------- PrintlnStringSmall ------");
        server.setMarkToEndOfLog();
        getHttpServlet("/RealFlushTestApp/printStringSmallTests/printlnStringSmall");
        Assert.assertNotNull("smallStr NOT FOUND", server.waitForStringInLogUsingMark("smallStr"));

        Log.info(c, testName, "------- PrintString8192 ------");
        server.setMarkToEndOfLog();
        getHttpServlet("/RealFlushTestApp/printStringLongTests/printString8192");
        Assert.assertNotNull("str8192 NOT FOUND", server.waitForStringInLogUsingMark(str8192));

        Log.info(c, testName, "------- PrintString8193 ------");
        server.setMarkToEndOfLog();
        getHttpServlet("/RealFlushTestApp/printStringLongTests/printString8193");
        Assert.assertNotNull("str8193 NOT FOUND", server.waitForStringInLogUsingMark(str8193));

        Log.info(c, testName, "------- PrintStringSmall ------");
        server.setMarkToEndOfLog();
        getHttpServlet("/RealFlushTestApp/printStringSmallTests/printStringSmall");
        Assert.assertNotNull("smallStr NOT FOUND", server.waitForStringInLogUsingMark("smallStr"));

    }

    @Test
    public void printCharArrayTest() throws Exception {

        String testName = "printStringsTest";
        Log.info(c, testName, "------- This Section Will Print Varying Length char[] ------");
        Log.info(c, testName, "------- PrintlnCharArray8192 ------");
        server.setMarkToEndOfLog();
        getHttpServlet("/RealFlushTestApp/printCharArrayLargeTests/printlnCharArray8192");
        Assert.assertNotNull("str8192 NOT FOUND", server.waitForStringInLogUsingMark(str8192));

        Log.info(c, testName, "------- PrintlnCharArray8193 ------");
        server.setMarkToEndOfLog();
        getHttpServlet("/RealFlushTestApp/printCharArrayLargeTests/printlnCharArray8193");
        Assert.assertNotNull("str8193 NOT FOUND", server.waitForStringInLogUsingMark(str8193));

        Log.info(c, testName, "------- PrintlnCharArraySmall ------");
        server.setMarkToEndOfLog();
        getHttpServlet("/RealFlushTestApp/printCharArraySmallTests/printlnCharArraySmall");
        Assert.assertNotNull("smallStr NOT FOUND", server.waitForStringInLogUsingMark("smallStr"));

        Log.info(c, testName, "------- PrintCharArray8192 ------");
        server.setMarkToEndOfLog();
        getHttpServlet("/RealFlushTestApp/printCharArrayLargeTests/printCharArray8192");
        Assert.assertNotNull("str8192 NOT FOUND", server.waitForStringInLogUsingMark(str8192));

        Log.info(c, testName, "------- PrintCharArray8193 ------");
        server.setMarkToEndOfLog();
        getHttpServlet("/RealFlushTestApp/printCharArrayLargeTests/printCharArray8193");
        Assert.assertNotNull("str8193 NOT FOUND", server.waitForStringInLogUsingMark(str8193));

        Log.info(c, testName, "------- PrintCharArraySmall ------");
        server.setMarkToEndOfLog();
        getHttpServlet("/RealFlushTestApp/printCharArraySmallTests/printCharArraySmall");
        Assert.assertNotNull("smallStr NOT FOUND", server.waitForStringInLogUsingMark("smallStr"));

    }

    @Test
    public void printLongTest() throws Exception {

        String testName = "printLongTest";
        Log.info(c, testName, "------- This Section Will Print long Types ------");
        Log.info(c, testName, "------- PrintlnLong ------");
        server.setMarkToEndOfLog();
        getHttpServlet("/RealFlushTestApp/printLongTests/printlnLong");
        Assert.assertNotNull("222222222 NOT FOUND", server.waitForStringInLogUsingMark("222222222"));

        Log.info(c, testName, "------- PrintLongSmall ------");
        server.setMarkToEndOfLog();
        getHttpServlet("/RealFlushTestApp/printLongTests/printLong");
        Assert.assertNotNull("222222222 NOT FOUND", server.waitForStringInLogUsingMark("222222222"));

    }

    @Test
    public void printIntTest() throws Exception {

        String testName = "printIntTest";
        Log.info(c, testName, "------- This Section Will Print int Types ------");
        Log.info(c, testName, "------- PrintlnInt ------");
        server.setMarkToEndOfLog();
        getHttpServlet("/RealFlushTestApp/printIntTests/printlnInt");
        Assert.assertNotNull("222222222 NOT FOUND", server.waitForStringInLogUsingMark("222222222"));

        Log.info(c, testName, "------- PrintIntSmall ------");
        server.setMarkToEndOfLog();
        getHttpServlet("/RealFlushTestApp/printIntTests/printInt");
        Assert.assertNotNull("222222222 NOT FOUND", server.waitForStringInLogUsingMark("222222222"));

    }

    @Test
    public void printFloatTest() throws Exception {

        String testName = "printFloatTest";
        Log.info(c, testName, "------- This Section Will Print float Types ------");
        Log.info(c, testName, "------- PrintlnFloat ------");
        server.setMarkToEndOfLog();
        getHttpServlet("/RealFlushTestApp/printFloatTests/printlnFloat");
        Assert.assertNotNull("222222222 NOT FOUND", server.waitForStringInLogUsingMark("2.22222224E8"));

        Log.info(c, testName, "------- PrintFloat ------");
        server.setMarkToEndOfLog();
        getHttpServlet("/RealFlushTestApp/printFloatTests/printFloat");
        Assert.assertNotNull("222222222 NOT FOUND", server.waitForStringInLogUsingMark("2.22222224E8"));

    }

    @Test
    public void printDoubleTest() throws Exception {

        String testName = "printDoubleTest";
        Log.info(c, testName, "------- This Section Will Print double Types ------");
        Log.info(c, testName, "------- PrintlnDouble ------");
        server.setMarkToEndOfLog();
        getHttpServlet("/RealFlushTestApp/printDoubleTests/printlnDouble");
        Assert.assertNotNull("222222222 NOT FOUND", server.waitForStringInLogUsingMark("2.22222222E8"));

        Log.info(c, testName, "------- PrintDouble ------");
        server.setMarkToEndOfLog();
        getHttpServlet("/RealFlushTestApp/printDoubleTests/printDouble");
        Assert.assertNotNull("222222222 NOT FOUND", server.waitForStringInLogUsingMark("2.22222222E8"));

    }

    @Test
    public void printCharTest() throws Exception {

        String testName = "printCharTest";
        Log.info(c, testName, "------- This Section Will Print char Types ------");
        Log.info(c, testName, "------- PrintlnChar ------");
        server.setMarkToEndOfLog();
        getHttpServlet("/RealFlushTestApp/printCharTests/printlnChar");
        Assert.assertNotNull("z NOT FOUND", server.waitForStringInLogUsingMark("z"));

        Log.info(c, testName, "------- PrintChar ------");
        server.setMarkToEndOfLog();
        getHttpServlet("/RealFlushTestApp/printCharTests/printChar");
        Assert.assertNotNull("z NOT FOUND", server.waitForStringInLogUsingMark("z"));

    }

    @Test
    public void printBoolTest() throws Exception {

        String testName = "printBoolTest";
        Log.info(c, testName, "------- This Section Will Print bool Types ------");
        Log.info(c, testName, "------- PrintFalse ------");
        server.setMarkToEndOfLog();
        getHttpServlet("/RealFlushTestApp/printBoolTests/printFalse");
        Assert.assertNotNull("false NOT FOUND", server.waitForStringInLogUsingMark("false"));

        Log.info(c, testName, "------- PrintlnFalse ------");
        server.setMarkToEndOfLog();
        getHttpServlet("/RealFlushTestApp/printBoolTests/printlnFalse");
        Assert.assertNotNull("false NOT FOUND", server.waitForStringInLogUsingMark("false"));

        Log.info(c, testName, "------- PrintTrue ------");
        server.setMarkToEndOfLog();
        getHttpServlet("/RealFlushTestApp/printBoolTests/printTrue");
        Assert.assertNotNull("true NOT FOUND", server.waitForStringInLogUsingMark("true"));

        Log.info(c, testName, "------- PrintlnTrue ------");
        server.setMarkToEndOfLog();
        getHttpServlet("/RealFlushTestApp/printBoolTests/printlnTrue");
        Assert.assertNotNull("true NOT FOUND", server.waitForStringInLogUsingMark("true"));

    }

    @Test
    public void printObjectTest() throws Exception {

        String testName = "printObjectTest";
        Log.info(c, testName, "------- This Section Will Print Varying Length Object.toString() Strings ------");
        Log.info(c, testName, "------- PrintlnObject8192 ------");
        server.setMarkToEndOfLog();
        getHttpServlet("/RealFlushTestApp/printObjectLargeTests/printlnObject8192");
        Assert.assertNotNull("str8192 NOT FOUND", server.waitForStringInLogUsingMark(str8192));

        Log.info(c, testName, "------- PrintlnObject8193 ------");
        server.setMarkToEndOfLog();
        getHttpServlet("/RealFlushTestApp/printObjectLargeTests/printlnObject8193");
        Assert.assertNotNull("str8193 NOT FOUND", server.waitForStringInLogUsingMark(str8193));

        Log.info(c, testName, "------- PrintObject8192 ------");
        server.setMarkToEndOfLog();
        getHttpServlet("/RealFlushTestApp/printObjectLargeTests/printObject8192");
        Assert.assertNotNull("str8192 NOT FOUND", server.waitForStringInLogUsingMark(str8192));

        Log.info(c, testName, "------- PrintObject8193 ------");
        server.setMarkToEndOfLog();
        getHttpServlet("/RealFlushTestApp/printObjectLargeTests/printObject8193");
        Assert.assertNotNull("str8193 NOT FOUND", server.waitForStringInLogUsingMark(str8193));

        Log.info(c, testName, "------- PrintObjectSmall ------");
        server.setMarkToEndOfLog();
        getHttpServlet("/RealFlushTestApp/printObjectSmallTests/printObjectSmall");
        Assert.assertNotNull("smallStr NOT FOUND", server.waitForStringInLogUsingMark("smallStr"));

        Log.info(c, testName, "------- PrintlnObjectSmall ------");
        server.setMarkToEndOfLog();
        getHttpServlet("/RealFlushTestApp/printObjectSmallTests/printlnObjectSmall");
        Assert.assertNotNull("smallStr NOT FOUND", server.waitForStringInLogUsingMark("smallStr"));

    }

    @After
    public void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.removeAllInstalledAppsForValidation();
        }
    }
}
