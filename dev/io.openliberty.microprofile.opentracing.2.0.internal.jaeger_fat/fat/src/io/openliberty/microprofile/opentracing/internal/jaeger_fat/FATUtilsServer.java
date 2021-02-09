/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.opentracing.internal.jaeger_fat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 *<p>Jaeger FAT utilities.</p>
 */
public class FATUtilsServer {
    // Logging

    private static final Class<? extends FATUtilsServer> CLASS = FATUtilsServer.class;

    private static void info(
        String methodName,
        String prefix,
        String text, Object value) {

        FATLogging.info(CLASS, methodName, prefix, text, value);
    }

    private static void info(
        String methodName,
        String prefix,
        String text1, Object value1,
        String text2, Object value2 ) {

        FATLogging.info(CLASS, methodName, prefix, text1, value1, text2, value2);
    }

    // Request processing

    public static enum HttpRequestMethod {
        GET, HEAD, POST, PUT, DELETE, CONNECT, OPTIONS, TRACE, PATCH;
    }

    /**
     * Make an HTTP connection using a specified URL.  Collect and
     * return the response lines.
     *
     * @param requestMethod The type of HTTP request method to use for the connection.
     * @param requestUrl The text of the request URL.
     *
     * @return The collected request response lines.
     *
     * @throws Exception Thrown if the connection could not be made, or if the
     *     response lines could not be collected.
     */
    public static List<String> gatherHttpRequest(HttpRequestMethod requestMethod, String requestUrl) throws Exception {
        String methodName = "gatherHttpRequest";

        info(methodName, "ENTER", "Request Method", requestMethod, "Request URL", requestUrl);

        URL url = new URL(requestUrl); // throws MalformedURLException

        HttpURLConnection connection = (HttpURLConnection) url.openConnection(); // throws IOException
        try {
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setUseCaches(false);
            connection.setRequestMethod( requestMethod.toString() ); // 'setRequestMethod' throws ProtocolException
            InputStream connectionStream = connection.getInputStream(); // throws IOException
            InputStreamReader rawConnectionReader = new InputStreamReader(connectionStream);
            BufferedReader connectionReader = new BufferedReader(rawConnectionReader);

            List<String> responseLines = read(connectionReader); // throws IOException
            info(methodName, "RETURN", "Lines", Integer.valueOf(responseLines.size()));
            return responseLines;

        } finally {
            connection.disconnect();
        }
    }

    public static List<String> read(BufferedReader reader) throws IOException {
        List<String> lines = new ArrayList<String>();

        String line;
        while ( (line = reader.readLine()) != null ) { // throws IOException
            lines.add(line);
        }

        return lines;
    }
}
