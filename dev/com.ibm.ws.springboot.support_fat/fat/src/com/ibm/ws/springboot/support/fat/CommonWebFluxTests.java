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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.Set;

import org.junit.Assert;
import org.junit.runner.RunWith;

import componenttest.custom.junit.runner.FATRunner;

@RunWith(FATRunner.class)
public class CommonWebFluxTests extends CommonWebServerTests {

    @Override
    public String getApplication() {
        return SPRING_BOOT_20_APP_WEBFLUX;
    }

    @Override
    public Set<String> getFeatures() {
        return Collections.emptySet();
    }

    static int sleep = 100;

    public void testBlockingIO() throws IOException, InterruptedException {
        int size = 10000;
        byte numIterations = 10;
        byte[] data = new byte[size];
        byte dataByte = 0;
        for (int i = 0; i < size; i++, dataByte++) {
            data[i] = dataByte;
            if (dataByte == Byte.MAX_VALUE) {
                dataByte = 0;
            }
        }

        byte[] expected = new byte[size * numIterations];
        int expectedIdx = 0;
        for (int i = 0; i < numIterations; i++) {
            for (byte b : data) {
                expected[expectedIdx++] = b;
            }
        }

        URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/echo");

        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setDoInput(true);
        con.setDoOutput(true);
        con.setUseCaches(false);
        con.setRequestMethod("POST");
        con.setConnectTimeout(30 * 1000); // 30s
        con.connect();

        OutputStream out = con.getOutputStream();
        for (int i = 0; i < numIterations; i++) {
            Thread.sleep(sleep);
            out.write(data);
        }

        byte[] response = getResponse(con, size);

        Assert.assertArrayEquals("Wrong response bytes.", expected, response);
    }

    private static byte[] getResponse(HttpURLConnection con, int size) throws IOException, InterruptedException {
        ByteArrayOutputStream response = new ByteArrayOutputStream();
        InputStream in = con.getInputStream();
        byte[] buf = new byte[size];
        int cnt;
        while ((cnt = in.read(buf)) > 0) {
            response.write(buf, 0, cnt);
            Thread.sleep(sleep);
        }
        return response.toByteArray();
    }
}
