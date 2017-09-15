/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.runtime.update.bvt;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import test.common.SharedOutputManager;

public class RuntimeUpdateNotificationMBeanServletTest {
    
    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    @Rule
    public TestRule managerRule = outputMgr;

    /**
     * Test if node exist
     */
    @Test
    public void existsAndSupportsNotifications() {
        HttpURLConnection conn = null;
        try {
            URL url = new URL("http://localhost:" + getPort() + "/runtimeUpdateNotificationMBeanTest");
            System.out.println("URL=" + url.toString());
            conn = (HttpURLConnection) url.openConnection();
            conn.setDoInput(true);
            conn.setRequestMethod("GET");
            String line = readFirstResponseLine(conn);
            assertEquals("Check if runtime update notifications are supported",
                    "Runtime update notifications supported", line);
        } 
        catch (Throwable t) {
            outputMgr.failWithThrowable("existsAndSupportsNotifications", t);
        } 
        finally {
            if (null != conn) {
                conn.disconnect();
            }
        }
    }
    
    private int getPort() {
        return Integer.valueOf(System.getProperty("HTTP_default", "8000"));
    }
    
    /**
     * Grab the first line from the servlet response.
     * 
     * @param conn
     * @return
     * @throws IOException
     */
    private String readFirstResponseLine(HttpURLConnection conn) throws IOException {
        InputStream is = null;
        String firstLine = null;
        try {
            System.out.println("Starting to read response from " + conn);
            is = conn.getInputStream();
            InputStreamReader isr = new InputStreamReader(is, "UTF-8");
            BufferedReader br = new BufferedReader(isr);
            firstLine = br.readLine();
            System.out.println("Response message:");
            System.out.println("\t" + firstLine);
            String line;
            while (null != (line = br.readLine())) {
                System.out.println("\t" + line);
            }
        } 
        finally {
            tryToClose(is);
        }
        return firstLine;
    }

    /**
     * Try to close the stream if it exists.
     * 
     * @param stream
     */
    private void tryToClose(Closeable stream) {
        if (stream != null) {
            try {
                stream.close();
            } 
            catch (IOException ioe) {
                // ignore.
            }
        }
    }
}
