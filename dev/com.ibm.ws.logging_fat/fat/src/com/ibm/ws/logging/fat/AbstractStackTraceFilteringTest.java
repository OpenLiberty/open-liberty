/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.fat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

/**
 *
 */
public class AbstractStackTraceFilteringTest {

    protected static final String INTERNAL_CLASSES_REGEXP = "at \\[internal classes\\]";
    protected static final String CONSOLE_LOG = "logs/console.log";

    protected static final int CONN_TIMEOUT = 10;

    protected static LibertyServer server;

    protected static void hitWebPage(String contextRoot, String servletName, boolean failureAllowed) throws MalformedURLException, IOException, ProtocolException {
        try {
            URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + contextRoot + "/" + servletName);
            int expectedResponseCode = failureAllowed ? HttpURLConnection.HTTP_INTERNAL_ERROR : HttpURLConnection.HTTP_OK;
            HttpURLConnection con = HttpUtils.getHttpConnection(url, expectedResponseCode, CONN_TIMEOUT);
            BufferedReader br = HttpUtils.getConnectionStream(con);
            String line = br.readLine();
            // Make sure the server gave us something back 
            assertNotNull(line);
            con.disconnect();
        } catch (IOException e) {
            // A message about a 500 code may be fine
            if (!failureAllowed) {
                throw e;
            }

        }

    }

    protected void assertConsoleLogDoesNotContain(String message, String stringToCheckFor) throws Exception {
        List<String> lines = server.findStringsInFileInLibertyServerRoot(stringToCheckFor, CONSOLE_LOG);
        assertTrue(message + " (found \'" + Arrays.toString(lines.toArray()) + "\'",
                   lines.isEmpty());

    }

    protected void assertConsoleLogContains(String message, String stringToCheckFor) throws Exception {
        assertFalse(message + " (could not find \'" + stringToCheckFor + "\')",
                    server.findStringsInFileInLibertyServerRoot(stringToCheckFor, CONSOLE_LOG).isEmpty());

    }

    protected void assertConsoleLogCountEquals(String message, String stringToCheckFor, int count) throws Exception {
        assertEquals(message, count,
                     server.findStringsInFileInLibertyServerRoot(stringToCheckFor, CONSOLE_LOG).size());

    }

    protected void assertMessagesLogDoesNotContain(String message, String stringToCheckFor) throws Exception {
        assertTrue(message,
                   server.findStringsInLogs(stringToCheckFor).isEmpty());

    }

    protected void assertMessagesLogContains(String message, String stringToCheckFor) throws Exception {
        assertFalse(message,
                    server.findStringsInLogs(stringToCheckFor).isEmpty());

    }

    protected void assertTraceLogDoesNotContain(String message, String stringToCheckFor) throws Exception {
        assertTrue(message,
                   server.findStringsInTrace(stringToCheckFor).isEmpty());

    }

    protected void assertTraceLogContains(String message, String stringToCheckFor) throws Exception {
        assertFalse(message,
                    server.findStringsInTrace(stringToCheckFor).isEmpty());

    }
}
