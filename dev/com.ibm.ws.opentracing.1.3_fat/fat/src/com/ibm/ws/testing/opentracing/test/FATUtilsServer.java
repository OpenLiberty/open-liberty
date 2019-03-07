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
package com.ibm.ws.testing.opentracing.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *<p>Open tracing FAT utilities.</p>
 */
public class FATUtilsServer implements FATOpentracingConstants {
    // Logging ...

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

    // URL construction ...

    public static String getRequestUrl(
        String hostName, int portNumber,
        String requestPath) {

        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append("http://");
        urlBuilder.append(hostName);
        urlBuilder.append(":");
        urlBuilder.append(Integer.toString(portNumber));
        urlBuilder.append("/");
        urlBuilder.append(requestPath);
        return urlBuilder.toString();
    }

    // URL utilities ...

    public static String getRequestPath(
        String contextRoot,
        String appPath, String servicePath, String endpointPath,
        Map<String, Object> requestParameters)
        throws UnsupportedEncodingException {

        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append(contextRoot);
        urlBuilder.append("/");
        urlBuilder.append(appPath);
        urlBuilder.append("/");
        urlBuilder.append(servicePath);
        urlBuilder.append("/");
        urlBuilder.append(endpointPath);

        if ( (requestParameters != null) && !requestParameters.isEmpty() ) {
            urlBuilder.append("?");

            String prefix = null;
            for ( Map.Entry<String, Object> parmEntry : requestParameters.entrySet() ) {
                String parmName = parmEntry.getKey();
                Object parmValue = parmEntry.getValue();

                if ( prefix != null ) {
                    urlBuilder.append(prefix);
                } else {
                    prefix = "&";
                }

                urlBuilder.append( urlEncode(parmName) );
                // 'encode' throws UnsupportedEncodingException

                if ( parmValue != null ) {
                    urlBuilder.append("=");
                    urlBuilder.append( urlEncode( parmValue.toString() ) );
                    // 'encode' throws UnsupportedEncodingException
                }
            }
        }

        return urlBuilder.toString();
    }

    public static final String UTF8_ENCODING = "UTF-8";

    public static String urlEncode(String text) throws UnsupportedEncodingException {
        return URLEncoder.encode(text, UTF8_ENCODING); // throws UnsupportedEncodingException
    }

    // Request processing ...

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

    /**
     * Starting at the specified line, search the lines for a target value.
     *
     * Answer the number of the first matching line.  Answer the number of
     * lines if no match is found.
     *
     * @param lines The lines which are to be searched.
     * @param lineNo The number of the first line which is to be searched.
     * @param target The value for which to search.
     *
     * @return The number of the first matching line.  Answer the last line
     *         number if no match is found.
     */
    public static int match(List<String> lines, int lineNo, String target) {
        int numLines = lines.size();
        while ( lineNo < numLines ) {
            String line = lines.get(lineNo);
            if ( line.indexOf(target) != -1 ) {
                break;
            }
            lineNo++;
        }
        return lineNo;
    }

    public static class Match {
        public final int lineNo;
        public final int matchNo;

        public Match(int lineNo, int matchNo) {
            this.lineNo = lineNo;
            this.matchNo = matchNo;
        }
    }

    /**
     * Match lines against targets.
     *
     * Search for the targets sequentially.
     * One target match per line is handled.
     *
     * Answer match data: The number of the last line which had
     * a match and the number of the last target which matched.
     */
    public static Match match(List<String> lines, List<String> targets) {
        int numLines = lines.size();
        int numTargets = targets.size();

        int lineNo = 0;
        int targetNo = 0;

        while ( (lineNo < numLines) && (targetNo < numTargets) ) {
            String target = targets.get(targetNo);
            lineNo = match(lines, lineNo, target);
            if ( lineNo == numLines ) {
                // This target found no match.
                break;
            } else if ( targetNo == numTargets ) {
                // This target found a match, but there are no more targets.
                break;
            } else {
                // This target found a match, and there are more targets.
                lineNo++;
                targetNo++;
            }
        }

        // Will be either (
        return new Match(lineNo, targetNo);
    }

    public static List<String> read(BufferedReader reader) throws IOException {
        // String methodName = "read";

        List<String> lines = new ArrayList<String>();

        String line;
        // int lineNo = 0;
        while ( (line = reader.readLine()) != null ) { // throws IOException
            // info(methodName, Integer.toString(lineNo), line);
            lines.add(line);
            // lineNo++;
        }

        return lines;
    }
}
