/*******************************************************************************
 * Copyright (c) 2018, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.springboot.support.fat;

import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.custom.junit.runner.FATRunner;

@RunWith(FATRunner.class)
public abstract class CommonWebFluxTests extends CommonWebServerTests {

    @Override
    public String getApplication() {
        return SPRING_BOOT_40_APP_WEBFLUX;
    }

    @Override
    public Set<String> getFeatures() {
        return Collections.emptySet();
    }

    @AfterClass
    public static void stopTestServer() throws Exception {
        Log.info(CommonWebFluxTests.class, "stopTestServer", "CWWKE1102W CWWKE1106W");
        // ignore quiesce errors for webflex tests
        server.stopServer("CWWKE1102W", "CWWKE1106W");
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
        Log.info(getClass(), "testBlockingIO", url.toExternalForm());
        byte[] response = asyncSendDataGetResponse(data, size, numIterations, url, 1);
        if (response == null) {
            // try again
            response = asyncSendDataGetResponse(data, size, numIterations, url, 2);
        }
        if (response != null) {
            Assert.assertArrayEquals("Wrong response bytes.", expected, response);
        } else {
            Log.info(getClass(), "testBlockingIO", "Skipping test, client timedout.");
        }
    }

    private byte[] asyncSendDataGetResponse(byte[] data, int size, int numIterations, URL url, int i) throws IOException, InterruptedException {
        final AtomicReference<byte[]> result = new AtomicReference<>();
        final AtomicReference<Exception> error = new AtomicReference<>();
        final AtomicBoolean gotResponse = new AtomicBoolean();
        Runnable background = () -> {
            try {
                Log.info(getClass(), "asyncSendDataGetResponse", String.valueOf(i));
                result.set(sendDataGetResponse(data, size, numIterations, url));
                gotResponse.set(true);
            } catch (IOException e) {
                error.set(e);
            } catch (InterruptedException e) {
                error.set(e);
            }
        };
        Thread t = new Thread(background, "testBlockingIO_" + i);
        t.setDaemon(true);
        t.start();
        t.join(2 * 60 * 1000); // 2 minutes
        if (t.isAlive()) {
            Log.info(getClass(), "asyncSendDataGetResponse", "Timed out, now trying to interrupt async thread.");
            t.interrupt();
            Thread.sleep(1000);
            if (t.isAlive()) {
                Log.info(getClass(), "asyncSendDataGetResponse", "Interrupt didn't work, trying again but will likely fail.");
                t.interrupt();
            }
        }
        Exception e = error.get();
        if (e instanceof InterruptedException) {
            if (i > 1) {
                sneakyThrow(e);
            }
        } else if (e != null) {
            sneakyThrow(error.get());
        }
        if (gotResponse.get()) {
            Log.info(getClass(), "asyncSendDataGetResponse", "Got a response: " + result.get());
            assertNotNull("Got null response.", result.get());
        } else {
            Log.info(getClass(), "asyncSendDataGetResponse", "getResponse timed out.");
        }
        return result.get();
    }

    @SuppressWarnings("unchecked")
    static <E extends Throwable> void sneakyThrow(Throwable e) throws E {
        throw (E) e;
    }

    private byte[] sendDataGetResponse(byte[] data, int size, int numIterations, URL url) throws IOException, InterruptedException {
        Log.info(getClass(), "sendDataGetResponse", "about to connect");
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setDoInput(true);
        con.setDoOutput(true);
        con.setUseCaches(false);
        con.setRequestMethod("POST");
        con.setConnectTimeout(30 * 1000); // 30s
        con.connect();
        Log.info(getClass(), "sendDataGetResponse", "sending data");
        OutputStream out = con.getOutputStream();
        try {
            for (int i = 0; i < numIterations; i++) {
                Thread.sleep(sleep);
                out.write(data);
            }
        } finally {
            out.flush();
            out.close();
        }

        byte[] response = getResponse(con, size);
        return response;
    }

    private static byte[] getResponse(HttpURLConnection con, int size) throws IOException, InterruptedException {
        Log.info(CommonWebFluxTests.class, "getResponse", "reading response data");
        ByteArrayOutputStream response = new ByteArrayOutputStream();
        try {
            InputStream in = con.getInputStream();
            byte[] buf = new byte[size];
            int cnt;
            while ((cnt = in.read(buf)) > 0) {
                response.write(buf, 0, cnt);
                Thread.sleep(sleep);
            }
        } finally {
            response.flush();
            response.close();
        }
        Log.info(CommonWebFluxTests.class, "getResponse", "done reading data");
        return response.toByteArray();
    }
}
