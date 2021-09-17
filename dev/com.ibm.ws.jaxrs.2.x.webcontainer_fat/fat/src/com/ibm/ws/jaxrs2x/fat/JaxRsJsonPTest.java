/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jaxrs2x.fat;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.topology.impl.LibertyServer;

@RunWith(FATRunner.class)
public class JaxRsJsonPTest {

    @Server("jaxrs2x.service.JaxRsJsonPTest")
    public static LibertyServer server;

    private static final String jsonpwar = "jsonp";

    @BeforeClass
    public static void setup() throws Exception {

        ShrinkHelper.defaultDropinApp(server, jsonpwar, "com.ibm.ws.jaxrs2x.fat.jsonp.service");

        if (JakartaEE9Action.isActive()) {
            Path someArchive = Paths.get("publish/servers/" + server.getServerName() + "/dropins/jsonp.war");
            JakartaEE9Action.transformApp(someArchive);
        }

        // Make sure we don't fail because we try to start an
        // already started server
        try {
            server.startServer(true);
        } catch (Exception e) {
            System.out.println(e.toString());
        }
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

    @Test
    public void testGetJsonObject() {
        String url = TestUtils.getBaseTestUri("jsonp", "Test", "jsonp", "getJsonObject");
        try {
            String resp = TestUtils.accessWithURLConn(url, "GET", null, 200, null);
            assertTrue("the return jsonobject is not right, the response is:" + resp, (resp.indexOf("firstName") > -1 && resp.indexOf("lastName") > -1));
        } catch (IOException e) {
            fail("test fails: " + e);
        }
    }

    @Test
    public void testGetJsonArray() {
        String url = TestUtils.getBaseTestUri("jsonp", "Test", "jsonp", "getJsonArray");
        try {
            String resp = TestUtils.accessWithURLConn(url, "GET", null, 200, null);
            assertTrue("the return jsonarray is not right, the response is:" + resp, (resp.indexOf("alex") > -1 && resp.indexOf("iris") > -1));
        } catch (IOException e) {
            fail("test fails: " + e);
        }
    }

    @Test
    public void testPutJsonObject() {
        String url = TestUtils.getBaseTestUri("jsonp", "Test", "jsonp", "putJsonObject");
        try {
            String t = "{\"firstName\":\"alex\",\"lastName\":\"zan\"}";
            String resp = TestUtils.accessWithURLConn(url, "POST", "application/json", 200, t.getBytes());
            Log.info(this.getClass(), "testPutJsonObject", "response=" + resp);
            assertTrue("the return is not right, the response is:" + resp, (resp.indexOf("1") > -1));
        } catch (IOException e) {
            fail("test fails: " + e);
        }
    }

    @Test
    public void testPutJsonArray() {
        String url = TestUtils.getBaseTestUri("jsonp", "Test", "jsonp", "putJsonArray");
        try {
            String t = "[\"alex\",\"iris\",\"grant\",\"zhubin\",\"wei\"]";
            String resp = TestUtils.accessWithURLConn(url, "POST", "application/json", 200, t.getBytes());
            Log.info(this.getClass(), "testPutJsonArray", "response=" + resp);
            assertTrue("the return is not right, the response is:" + resp, (resp.indexOf("1") > -1));
        } catch (IOException e) {
            fail("test fails: " + e);
        }
    }

    @Test
    public void testReadMans() {
        String url = TestUtils.getBaseTestUri("jsonp", "Test", "jsonp", "readMans");
        try {
            String t = "[\"alex\",\"iris\",\"grant\",\"zhubin\",\"wei\"]";
            String resp = TestUtils.accessWithURLConn(url, "POST", "application/json", 200, t.getBytes());
            Log.info(this.getClass(), "testReadMans", "response=" + resp);

            String expectResp = "[{\"name\":\"alex\",\"age\":20,\"gender\":\"M\",\"job\":{\"title\":\"softengineer\",\"woritems\":10},\"fav\":[\"sport\",\"travel\"]},{\"name\":\"alex\",\"age\":21,\"gender\":\"F\",\"job\":{\"title\":\"softengineer\",\"woritems\":10},\"fav\":[\"sport\",\"travel\"]},{\"name\":\"alex\",\"age\":22,\"gender\":\"M\",\"job\":{\"title\":\"softengineer\",\"woritems\":10},\"fav\":[\"sport\",\"travel\"]},{\"name\":\"alex\",\"age\":23,\"gender\":\"F\",\"job\":{\"title\":\"softengineer\",\"woritems\":10},\"fav\":[\"sport\",\"travel\"]},{\"name\":\"alex\",\"age\":24,\"gender\":\"M\",\"job\":{\"title\":\"softengineer\",\"woritems\":10},\"fav\":[\"sport\",\"travel\"]}]";

            assertTrue("the return is not right, the response is:" + resp, (expectResp.equals(resp.trim())));
        } catch (IOException e) {
            fail("test fails: " + e);
        }
    }

    @Test
    @AllowedFFDC("javax.ws.rs.BadRequestException")
    public void testPostBadJsonObject() {
        String url = TestUtils.getBaseTestUri("jsonp", "Test", "jsonp", "putJsonObject");
        int badRequestRC = javax.ws.rs.core.Response.Status.BAD_REQUEST.getStatusCode();
        try {
            String t = "{\"firstName\":\"alex\",\"lastName\":\"zan\",}";
            String resp = TestUtils.accessWithURLConn(url, "POST", "application/json", badRequestRC, t.getBytes());
            Log.info(this.getClass(), "testPostBadJsonObject", "response=" + resp);
        } catch (IOException e) {
            boolean check = (e.toString().indexOf("Server returned HTTP response code: 400") > -1);
            assertTrue("the return code should be 400, but the exception is " + e, check);
        }
    }
}