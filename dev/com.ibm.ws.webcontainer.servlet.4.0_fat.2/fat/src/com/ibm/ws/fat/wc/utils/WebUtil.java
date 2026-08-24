/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.fat.wc.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;
import java.util.StringTokenizer;

/**
 * Contains convenience methods for interacting with a web server.
 *
 * @author Mark Roth
 */
public class WebUtil {

    /**
     * Reponse object containing information returned from the web server
     */
    public static class Response {
        /** Version (usually HTTP/1.0) */
        public String versionToken;

        /** Status (e.g. 401) */
        public String statusToken;

        /** Location (for redirections) */
        public String location;

        /** Actual page content */
        public String content = "";

        /** Storage for cookies */
        public Hashtable cookies = new Hashtable();

        /** Flag; true if authentication requested */
        public boolean authenticationRequested = false;

        /**
         * Parses a header line for an old-style cookie (not Set-Cookie2), and stores the cookie in the cookies table. Only the
         * key and value are stored. Expected syntax: "Set-Cookie: NAME=VALUE[;...]" The key is stored in upper-case.
         *
         * @param cookieLine The string with the cookie in it.
         */
        public void parseCookie(String cookieLine) {
            // Strip Set-Cookie: from line:
            cookieLine = cookieLine.substring("Set-Cookie:".length()).trim();

            // Strip any additional parameters from the end of the line.
            int semicolon = cookieLine.indexOf(";");
            if (semicolon != -1) {
                cookieLine = cookieLine.substring(0, semicolon).trim();
            }

            // Now we have "NAME=VALUE"
            int equals = cookieLine.indexOf("=");
            String name = cookieLine.substring(0, equals).toUpperCase();
            String value = cookieLine.substring(equals + 1);
            cookies.put(name.trim(), value.trim());
        }

        /**
         * Returns true if the status token for this response represents an error, or false if the status token indicates the
         * page was retrieved okay. Note that a redirection is not an error.
         */
        public boolean isError() {
            // According to RFC2616, all status tokens staring with 4xx or 5xx
            // are errors.
            return statusToken.startsWith("4") || statusToken.startsWith("5");
        }
    }

    /**
     * Converts a standard URL to a request. For example, the string "http://goodtimes:8000/testing" would be converted to
     * "/testing".
     *
     * @param urlString The URL to convert
     * @return The resulting GET request
     * @exception MalformedURLException Thrown if the urlString does not contain a valid URL.
     */
    public static String getRequestFromURL(String urlString) throws MalformedURLException {
        URL url = new URL(urlString);
        return url.getFile();
    }

    /**
     * Sends a request to the web server. A WebUtil.Response object is returned with the response information.
     *
     * @param method     Can be either "GET" or "POST"
     * @param addr       Address of web server
     * @param port       Port of web server
     * @param req        The file to request (e.g. /jsp_dep_secContextRoot/jspSec.jsp)
     * @param postData   If this is a POST request, the data to be posted, encoded in a Properties class. null if no post data
     *                       to be sent.
     * @param cookieList A list of cookies to send when requesting the page. null if no cookie list is to be sent.
     * @return WebUtil.Response object containing response information
     * @exception IOException Thrown if request could not be made
     */
    public static Response sendRequest(String method, InetAddress addr, int port, String req, Properties postData, Hashtable cookieList) throws IOException {
        return sendAuthenticatedRequest(method, addr, port, req, postData, cookieList, null, null);
    }

    /**
     * Sends an authenticated request to the web server. A WebUtil.Response object is returned with the response
     * information.
     *
     * @param method     Can be either "GET" or "POST"
     * @param addr       Address of web server
     * @param port       Port of web server
     * @param req        The file to request (e.g. /jsp_dep_secContextRoot/jspSec.jsp)
     * @param postData   If this is a POST request, the data to be posted, encoded in a Properties class. null if no post data
     *                       to be sent.
     * @param cookieList A list of cookies to send when requesting the page. null if no cookie list is to be sent.
     * @param username   The username for authentication, null if no authentication required.
     * @param password   The password for authentication, null if no authentication required.
     * @return WebUtil.Response object containing response information
     * @exception IOException Thrown if request could not be made
     */
    public static Response sendAuthenticatedRequest(String method, InetAddress addr, int port, String req, Properties postData,
                                                    Hashtable cookieList, String username, String password) throws IOException {
        String protocol = "HTTP/1.0";
        URL requestURL;
        Socket socket = null;
        PrintWriter out = null;
        BufferedReader in = null;
        String line = null;
        Response response = new Response();
        String hostname = null;

        try {
            hostname = addr.getHostName();
            requestURL = new URL("http", hostname, port, req);
            req = method + " " + req + " " + protocol;

            socket = new Socket(addr, port);

            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
            send(out, req);

            // send Host header
            if (port == 80) {
                send(out, "Host: " + hostname);
            } else {
                send(out, "Host: " + hostname + ':' + port);
            }

            if (cookieList != null) {
                // Send cookies:
                Enumeration keys = cookieList.keys();

                // Does at least one cookie exist?
                if (keys.hasMoreElements()) {
                    String cookieString = "Cookie: ";

                    // Add each cookie to the string
                    boolean first = true;
                    while (keys.hasMoreElements()) {
                        String key = (String) keys.nextElement();
                        String value = (String) cookieList.get(key);
                        cookieString += (first ? "" : "; ") + key + "=" + value; // + ";
                                                                                 // $Path=/";
                        first = false;
                    }

                    // Write cookies:
                    System.out.println("PAN: WebUtil: cookieString: " + cookieString);
                    send(out, cookieString);
                }
            }

            // Send authentication information if necessary:
            if (username != null) {
                String code = encodeBase64(username + ":" + password);
                send(out, "Authorization: Basic " + code.trim());
            }

            // Send extra header information if we are posting.
            if (postData != null) {
                send(out, "Content-type: application/x-www-form-urlencoded");
            }

            // If this is a post request, send post data:
            if ((postData != null) && method.toUpperCase().equals("POST")) {
                String postString = TestUtil.toEncodedString(postData);

                // Skip a line:
                send(out, "Content-length: " + postString.length());
                send(out, "");
                System.out.println("PAN: WebUtil: postString: " + postString);
                send(out, postString);
            } else {
                // Skip a line:
                send(out, "");
            }

            out.flush();

            // Read first line and check for HTTP version and OK.
            line = in.readLine();
            if (line != null) {
                //TestUtil.logTrace("HEADER: " + line);
                System.out.println("PAN: WebUtil sendAuthenticatedRequest: HEADER: " + line);

                StringTokenizer st = new StringTokenizer(line.trim());
                response.versionToken = st.nextToken();
                response.statusToken = st.nextToken();
            }

            // Read each line of the header until we hit a blank line
            while ((line = in.readLine()) != null) {
                //TestUtil.logTrace("HEADER: " + line);
                System.out.println("PAN: WebUtil sendAuthenticatedRequest: HEADER: " + line);

                // Blank line means we are done with the header:
                if (line.trim().equals(""))
                    break;

                // Analyze special tags location and set cookie
                if (line.toLowerCase().startsWith("location:")) {
                    // This is a redirect. Extract valuable infomration:
                    response.location = line.substring(10);
                } else if (line.toLowerCase().startsWith("set-cookie:")) {
                    // This is a cookie. Add the cookie to the response
                    // object.
                    response.parseCookie(line);
                } else if (line.toLowerCase().startsWith("www-authenticate:")) {
                    // Request to authenticate this page.
                    response.authenticationRequested = true;
                }
            }

            // The rest is content:
            while ((line = in.readLine()) != null) {
                System.out.println("PAN: WebUtil sendAuthRequest: " + line);
                System.out.print("PAN: WebUtil sendqutoRequest: codepoints: ");
                line.codePoints().forEach(x -> System.out.print(x + " "));
                System.out.println();
                response.content += line + "\n";
            }

            in.close();
            out.close();
        } catch (MalformedURLException e) {
            throw new IOException("MalformedURLException: " + e.getMessage());
        } catch (UnknownHostException e) {
            throw new IOException("UnknownHostException: " + e.getMessage());
        } catch (ConnectException e) {
            throw new IOException("ConnectException: " + e.getMessage());
        }

        return response;
    }

    /**
     * Outputs a single line of text to the given output stream. Appends a \r\n automatically. By adding a
     * System.out.println here, you can easily echo what is being sent to the web server.
     */
    private static void send(PrintWriter out, String s) {
        out.print(s + "\r\n");
        ///TestUtil.logTrace("REQUEST: " + s);
        System.out.println("PAN: WebUtil send: REQUEST: " + s);
    }

    /**
     * Encodes the given string in base64 format (useful for BASIC authentication). Base64 encoding is defined by RFC2047.
     *
     * @param s The string to encode
     * @return The encoded string
     */
    public static String encodeBase64(String s) {
        BASE64Encoder encoder = new BASE64Encoder();
        return encoder.encodeBuffer(s.getBytes());
    }
}
