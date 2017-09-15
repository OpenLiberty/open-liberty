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
package com.ibm.ws.config.bvt;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import test.common.SharedOutputManager;

public class SchemaTest {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    @Rule
    public TestRule outputRule = outputMgr;

    private String getPort() {
        return System.getProperty("HTTP_default", "8000");
    }

    private String read(InputStream in) throws IOException {
        InputStreamReader isr = new InputStreamReader(in);
        BufferedReader br = new BufferedReader(isr);
        StringBuilder builder = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            builder.append(line);
            builder.append(System.getProperty("line.separator"));
        }
        return builder.toString();
    }

    @Test
    public void testSchemaGeneratorAll() throws Exception {
        test("/schema?output=file", "testSchemaGeneratorAll");
    }

    private void test(String testUri, String testName) throws Exception {
        HttpURLConnection con = null;
        try {
            URL url = new URL("http://localhost:" + getPort() + testUri);
            con = (HttpURLConnection) url.openConnection();
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setUseCaches(false);
            con.setRequestMethod("GET");
            InputStream is = con.getInputStream();
            assertNotNull(is);

            String output = read(is);
            System.out.println(output);
            assertTrue("Response should start with OK", output.trim().startsWith("OK"));
        } finally {
            if (con != null) {
                con.disconnect();
            }
        }
    }
}
