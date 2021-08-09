/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.springboot.support.fat;

import static org.junit.Assert.assertEquals;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.utils.HttpUtils;
import componenttest.topology.utils.HttpUtils.HTTPRequestMethod;

@RunWith(FATRunner.class)
public abstract class MimeMappingBaseTest extends CommonWebServerTests {
    @Test
    public void testMimeMapping() throws Exception {
        URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/file.weby");
        Map<String, String> mimemap = new HashMap<>();
        mimemap.put("Content-Type", "");
        HttpURLConnection con = HttpUtils.getHttpConnection(url, HttpURLConnection.HTTP_OK, new int[0], 5, HTTPRequestMethod.GET,
                                                            mimemap, null);
        assertEquals("Got unexpected mime mapping", "application/json", con.getHeaderField("Content-Type"));
        HttpUtils.findStringInHttpConnection(con, "\"city\": \"Austin\"");
    }
}