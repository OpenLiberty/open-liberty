/*******************************************************************************
 * Copyright (c) 2011, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package componenttest.topology.utils;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.ProtocolException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.topology.impl.JavaInfo;
import componenttest.topology.impl.LibertyServer;

/**
 * Utilities for testing HTTP connections.
 */
public class HttpUtils {
    private final static Class<?> c = HttpUtils.class;
    private final static String LS = System.getProperty("line.separator");

    public final static int DEFAULT_TIMEOUT = 5000;

    private final static int LOWEST_ERROR_CODE = 400;

    public enum HTTPRequestMethod {
        GET, POST, HEAD, OPTIONS, PUT, DELETE, TRACE
    }

    /**
     * Get the response body of this connection, regardless of whether it returned an error code.
     */
    public static BufferedReader getResponseBody(HttpURLConnection con) throws IOException {
        if (con.getResponseCode() < LOWEST_ERROR_CODE) {
            return getConnectionStream(con);
        } else {
            return getErrorStream(con);
        }
    }

    /**
     * Get the response body of this connection, regardless of whether it returned an error code.
     */
    public static BufferedReader getResponseBody(HttpURLConnection con, String charsetName) throws IOException {
        if (con.getResponseCode() < LOWEST_ERROR_CODE) {
            return getConnectionStream(con, charsetName);
        } else {
            return getErrorStream(con, charsetName);
        }
    }

    /**
     * This method is used to get a connection stream from an HTTP connection. It
     * gives the output from the webpage that it gets from the connection
     *
     * @param con The connection to the HTTP address
     * @return The Output from the webpage
     */
    public static BufferedReader getConnectionStream(HttpURLConnection con) throws IOException {
        InputStream is = con.getInputStream();
        return getBufferedReader(is);
    }

    /**
     * This method is used to get a connection stream from an HTTP connection. It
     * gives the output from the webpage that it gets from the connection
     *
     * @param con The connection to the HTTP address
     * @param charsetName The character set name
     * @return The Output from the webpage
     */
    public static BufferedReader getConnectionStream(HttpURLConnection con, String charsetName) throws IOException {
        InputStream is = con.getInputStream();
        return getBufferedReader(is, charsetName);
    }

    /**
     * This method is used to get an error connection stream from a HTTP connection.
     *
     * @param con
     * @return
     * @throws IOException
     */
    public static BufferedReader getErrorStream(HttpURLConnection con) throws IOException {
        InputStream is = con.getErrorStream();
        return getBufferedReader(is);
    }

    /**
     * This method is used to get an error connection stream from a HTTP connection.
     *
     * @param con
     * @param charsetName
     * @return
     * @throws IOException
     */
    public static BufferedReader getErrorStream(HttpURLConnection con, String charsetName) throws IOException {
        InputStream is = con.getErrorStream();
        return getBufferedReader(is, charsetName);
    }

    private static BufferedReader getBufferedReader(InputStream is) {
        InputStreamReader isr = new InputStreamReader(is);
        return new BufferedReader(isr);
    }

    private static BufferedReader getBufferedReader(InputStream is, String charsetName) throws UnsupportedEncodingException {
        InputStreamReader isr = new InputStreamReader(is, charsetName);
        return new BufferedReader(isr);
    }

    /**
     * This method creates a connection to a webpage and then returns the connection, it doesn't care what the response code is.
     *
     * @param server The liberty server that is hosting the URL
     * @param path The path to the URL with the output to test (excluding port and server information). For instance "/someContextRoot/servlet1"
     * @return The connection to the http address
     */
    public static HttpURLConnection getHttpConnectionWithAnyResponseCode(LibertyServer server, String path) throws IOException {
        int timeout = DEFAULT_TIMEOUT;
        URL url = createURL(server, path);
        HttpURLConnection con = getHttpConnection(url, timeout, HTTPRequestMethod.GET);
        Log.finer(HttpUtils.class, "getHttpConnection", "Connecting to " + url.toExternalForm() + " expecting http response in " + timeout + " seconds.");
        con.connect();
        return con;
    }

    /**
     * This method creates an UNOPENED HTTP connection. Note that the caller must call HttpUrlConnection.connect()
     * if they wish to use the connection. The unopened connection may be further customized before calling connect()
     *
     * @param server The liberty server that is hosting the URL
     * @param path The path to the URL with the output to test (excluding port and server information). For instance "/someContextRoot/servlet1"
     * @return An unopened connection to the http address
     */
    public static HttpURLConnection getHttpConnection(LibertyServer server, String path) throws IOException {
        return getHttpConnection(createURL(server, path), DEFAULT_TIMEOUT, HTTPRequestMethod.GET);
    }

    /**
     * This method creates a connection to a webpage and then returns the connection
     *
     * @param url The Http Address to connect to
     * @param expectedResponseCode The expected response code to wait for
     * @param connectionTimeout The timeout in seconds
     * @return The connection to the http address
     */
    public static HttpURLConnection getHttpConnection(URL url, int expectedResponseCode, int connectionTimeout) throws IOException, ProtocolException {
        return getHttpConnection(url, expectedResponseCode, connectionTimeout, HTTPRequestMethod.GET);
    }

    /**
     * This method creates a connection to a webpage and then returns the connection
     *
     * @param url The Http Address to connect to
     * @param expectedResponseCode The expected response code to wait for
     * @param connectionTimeout The timeout in seconds
     * @param requestMethod The HTTP request method (GET, POST, HEAD, OPTIONS, PUT, DELETE, TRACE)
     * @return The connection to the http address
     */
    public static HttpURLConnection getHttpConnection(URL url, int expectedResponseCode, int connectionTimeout,
                                                      HTTPRequestMethod requestMethod) throws IOException, ProtocolException {
        return getHttpConnection(url, expectedResponseCode, null, connectionTimeout, requestMethod);
    }

    /**
     * This method creates a connection to a webpage and then returns the connection
     *
     * @param url The Http Address to connect to
     * @param expectedResponseCode The expected response code to wait for
     * @param allowedUnexpectedResponseCodes A list of unexpected response codes that should not cause a failure, or null to allow all unexpected response codes
     * @param connectionTimeout The timeout in seconds
     * @param requestMethod The HTTP request method (GET, POST, HEAD, OPTIONS, PUT, DELETE, TRACE)
     * @return The connection to the http address
     */
    public static HttpURLConnection getHttpConnection(URL url, int expectedResponseCode, int[] allowedUnexpectedResponseCodes, int connectionTimeout,
                                                      HTTPRequestMethod requestMethod) throws IOException, ProtocolException {

        if (System.getSecurityManager() == null) {
            return getHttpConnection(url, expectedResponseCode, allowedUnexpectedResponseCodes, connectionTimeout, requestMethod, null, null);

        } else {
            final URL f_url = url;
            final int f_expectedResponseCode = expectedResponseCode;
            final int[] f_allowedUnexpectedResponseCodes = allowedUnexpectedResponseCodes;
            final int f_connectionTimeout = connectionTimeout;
            final HTTPRequestMethod f_requestMethod = requestMethod;
            try {
                return AccessController.doPrivileged(new PrivilegedExceptionAction<HttpURLConnection>() {
                    @Override
                    public HttpURLConnection run() throws IOException, ProtocolException {
                        return getHttpConnection(f_url, f_expectedResponseCode, f_allowedUnexpectedResponseCodes, f_connectionTimeout, f_requestMethod, null, null);
                    }
                });
            } catch (PrivilegedActionException e) {
                Throwable cause = e.getCause();
                if (cause instanceof IOException) {
                    throw (IOException) cause;
                } else if (cause instanceof ProtocolException) {
                    throw (ProtocolException) cause;
                }
                return null;
            }

        }
    }

    /**
     * This method creates a connection to a webpage and then returns the connection
     *
     * @param url The Http Address to connect to
     * @param expectedResponseCode The expected response code to wait for
     * @param allowedUnexpectedResponseCodes A list of unexpected response codes that should not cause a failure, or null to allow all unexpected response codes
     * @param connectionTimeout The timeout in seconds
     * @param requestMethod The HTTP request method (GET, POST, HEAD, OPTIONS, PUT, DELETE, TRACE)
     * @param headers A map of HTTP headers keys and values.
     * @return The connection to the http address
     */
    public static HttpURLConnection getHttpConnection(URL url, int expectedResponseCode, int[] allowedUnexpectedResponseCodes, int connectionTimeout,
                                                      HTTPRequestMethod requestMethod, Map<String, String> headers,
                                                      InputStream streamToWrite) throws IOException, ProtocolException {
        Log.finer(HttpUtils.class, "getHttpConnection",
                  "Connecting to " + url.toExternalForm() + " expecting http response of " + expectedResponseCode + " in " + connectionTimeout + " seconds.");

        long startTime = System.currentTimeMillis();
        int timeout = connectionTimeout * 1000; // this is bad practice because it could overflow but the connection timeout on a urlconnection has to fit in an int.
        boolean streamToWriteReset = true; // true for first pass, set after first use.

        int count = 0;
        HttpURLConnection con = null;
        try {
            do {
                // If we've tried and failed to connect
                if (con != null) {
                    // fail if surprised or if streamToWrite unable to retry
                    if ((allowedUnexpectedResponseCodes != null
                         && !contains(allowedUnexpectedResponseCodes, con.getResponseCode()))
                        || !streamToWriteReset) {
                        String msg = "Expected response " + expectedResponseCode + ", received " + con.getResponseCode() +
                                     " (" + con.getResponseMessage() + ") while connecting to " + url;
                        if (!streamToWriteReset)
                            msg += ". Unable to reset streamToWrite";

                        String errorStream = read(con.getErrorStream());

                        // The Liberty 404 page is big and meaningless for our purposes.
                        if (con.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND &&
                            errorStream.contains("<title>Context Root Not Found</title>")) {
                            msg += ". Error title: Context Root Not Found";
                        } else {
                            msg += ". Error stream: " + errorStream;
                        }

                        AssertionError e = new AssertionError(msg);
                        Log.error(HttpUtils.class, "getHttpConnection", e);
                        throw e;
                    }

                    // fail when time's up
                    if (timeout <= (System.currentTimeMillis() - startTime)) {
                        String msg = "Expected response " + expectedResponseCode + " within " + connectionTimeout +
                                     " seconds, last received " + con.getResponseCode() + " (" + con.getResponseMessage() + ") while connecting to " + url;
                        AssertionError e = new AssertionError(msg);
                        Log.error(HttpUtils.class, "getHttpConnection", e, msg);
                        throw e;
                    }

                    // wait a second and try again
                    try {
                        Log.info(HttpUtils.class, "getHttpConnection",
                                 "Waiting 1s before retry " + count +
                                                                       " to connect to " + url +
                                                                       " due to response " + con.getResponseCode() + ": " + con.getResponseMessage());
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        //swallow the InterruptedException if there is one
                    } finally {
                    }
                }
                con = getHttpConnection(url, timeout, requestMethod);

                //Add additional headers, if any
                if (headers != null) {
                    Iterator<Entry<String, String>> entries = headers.entrySet().iterator();
                    while (entries.hasNext()) {
                        Entry<String, String> entry = entries.next();
                        con.setRequestProperty(entry.getKey(), entry.getValue());
                    }
                }

                //Write bytes if applicable
                if (streamToWrite != null) {
                    OutputStream out = con.getOutputStream();

                    byte[] buffer = new byte[1024];
                    int byteCount = 0;
                    while ((byteCount = streamToWrite.read(buffer)) != -1) {
                        out.write(buffer, 0, byteCount);
                    }
                    // if possible reset stream for retry
                    if (streamToWrite.markSupported()) {
                        streamToWrite.reset();
                    } else {
                        streamToWriteReset = false;
                    }
                }

                con.connect();
                count++;
            } while (con.getResponseCode() != expectedResponseCode);
            Log.finer(HttpUtils.class, "getHttpConnection", "RC=" + con.getResponseCode() + ", Connection established");
            return con;
        } finally {
            if (count > 1)
                Log.info(HttpUtils.class, "getHttpConnection", "Returning after " + count + " attempts to establish a connection.");
        }
    }

    /**
     * Return true if haystack contains needle.
     *
     * @param haystack the array to search
     * @param needle the value to find
     * @return true if the value was found
     */
    private static boolean contains(int[] haystack, int needle) {
        for (int value : haystack) {
            if (needle == value) {
                return true;
            }
        }
        return false;
    }

    /**
     * Method to find some text from the output of a URL. If the text isn't found an assertion error is thrown.
     *
     * @param server The liberty server for that is hosting the URL
     * @param path The path to the URL with the output to test (excluding port and server information). For instance "/someContextRoot/servlet1"
     * @param stringsToFind The strings to search for, there must be at least one string to find. Duplicates will only be searched for once
     * @throws IOException
     * @throws {@link AssertionError} If the text isn't found
     * @throws IllegalArgumentException If <code>stringsToFind</code> is <code>null</code> or empty
     */
    public static void findStringInUrl(LibertyServer server, String path, String... stringsToFind) throws IOException {

        final LibertyServer f_server = server;
        final String f_path = path;
        final String[] f_stringsToFind = stringsToFind;
        try {
            AccessController.doPrivileged(new PrivilegedExceptionAction<Void>() {
                @Override
                public Void run() throws IOException {
                    findStringInUrl(f_server, f_path, HTTPRequestMethod.GET, f_stringsToFind);
                    return null;

                }
            });
        } catch (PrivilegedActionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof IOException) {
                throw (IOException) cause;
            }
        }
    }

    /**
     * Method to find some text from the output of a URL. An assertion error is thrown if the connection returns an unexpected response code or if the text isn't found in the
     * response. This method should only be used if the caller knows URL is ready because the application is known to be started.
     *
     * @param server The liberty server for that is hosting the URL
     * @param path The path to the URL with the output to test (excluding port and server information). For instance "/someContextRoot/servlet1"
     * @param stringsToFind The strings to search for, there must be at least one string to find. Duplicates will only be searched for once
     * @throws Exception
     * @throws {@link AssertionError} If the text isn't found
     * @throws IllegalArgumentException If <code>stringsToFind</code> is <code>null</code> or empty
     */
    public static void findStringInReadyUrl(LibertyServer server, String path, String... stringsToFind) throws Exception {
        findStringInReadyUrl(server, path, HTTPRequestMethod.GET, stringsToFind);
    }

    /**
     * Method to find some text from the output of a URL. If the text isn't found an assertion error is thrown.
     *
     * @param server The liberty server for that is hosting the URL
     * @param path The path to the URL with the output to test (excluding port and server information). For instance "/someContextRoot/servlet1"
     * @param requestMethod The HTTP request method (GET, POST, HEAD, OPTIONS, PUT, DELETE, TRACE)
     * @param stringsToFind The strings to search for, there must be at least one string to find. Duplicates will only be searched for once
     * @throws IOException
     * @throws {@link AssertionError} If the text isn't found
     * @throws IllegalArgumentException If <code>stringsToFind</code> is <code>null</code> or empty
     */
    public static void findStringInUrl(LibertyServer server, String path, HTTPRequestMethod requestMethod, String... stringsToFind) throws IOException {
        findStringInUrl(server, path, null, requestMethod, stringsToFind);
    }

    /**
     * Method to find some text from the output of a URL. An assertion error is thrown if the connection returns an unexpected response code or if the text isn't found in the
     * response. This method should only be used if the caller knows URL is ready because the application is known to be started.
     *
     * @param server The liberty server for that is hosting the URL
     * @param path The path to the URL with the output to test (excluding port and server information). For instance "/someContextRoot/servlet1"
     * @param stringsToFind The strings to search for, there must be at least one string to find. Duplicates will only be searched for once
     * @throws Exception
     * @throws {@link AssertionError} If the text isn't found
     * @throws IllegalArgumentException If <code>stringsToFind</code> is <code>null</code> or empty
     */
    public static void findStringInReadyUrl(LibertyServer server, String path, HTTPRequestMethod requestMethod, String... stringsToFind) throws Exception {
        findStringInUrl(server, path, new int[0], requestMethod, stringsToFind);
    }

    /**
     * Method to find some text from the output of a URL. An assertion error is thrown if the connection returns an unexpected response code or if the text isn't found in the
     * response.
     *
     * @param server The liberty server for that is hosting the URL
     * @param path The path to the URL with the output to test (excluding port and server information). For instance "/someContextRoot/servlet1"
     * @param allowedUnexpectedResponseCodes A list of unexpected response codes that should not cause a failure, or null to allow all unexpected response codes
     * @param stringsToFind The strings to search for, there must be at least one string to find. Duplicates will only be searched for once
     * @throws IOException
     * @throws {@link AssertionError} If the text isn't found
     * @throws IllegalArgumentException If <code>stringsToFind</code> is <code>null</code> or empty
     */
    public static void findStringInUrl(LibertyServer server, String path, int[] allowedUnexpectedResponseCodes, HTTPRequestMethod requestMethod,
                                       String... stringsToFind) throws IOException {
        if (stringsToFind == null || stringsToFind.length == 0) {
            throw new IllegalArgumentException("No strings were supplied to look for");
        }
        URL url = createURL(server, path);
        Log.info(HttpUtils.class, "findStringInUrl", "Calling application with URL=" + url.toString() + " and request method=" + requestMethod);

        HttpURLConnection con = null;

        try {
            //check application is installed
            con = HttpUtils.getHttpConnection(url, HttpURLConnection.HTTP_OK, allowedUnexpectedResponseCodes, 5, requestMethod);
            findStringInHttpConnection(con, stringsToFind);
        } catch (IOException e) {
            Log.info(HttpUtils.class, "findStringInUrl", "Exception " + e.getClass().getName() + " requesting URL=" + url.toString(), e);
            throw e;
        }
    }

    private static URL createURL(LibertyServer server, String path) throws MalformedURLException {
        if (!path.startsWith("/"))
            path = "/" + path;
        return new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + path);
    }

    /**
     * Method to find some text from the output of an HttpURLConnection. If the text isn't found an assertion error is thrown.
     *
     * @param con The HttpURLconnection to retrieve the text from
     * @param stringsToFind The strings to search for, there must be at least one string to find. Duplicates will only be searched for once
     * @throws IOException
     * @throws {@link AssertionError} If the text isn't found
     * @throws IllegalArgumentException If <code>stringsToFind</code> is <code>null</code> or empty
     */
    public static void findStringInHttpConnection(HttpURLConnection con, String... stringsToFind) throws IOException {
        findStringInHttpConnection(null, con, stringsToFind);
    }

    /**
     * Method to find some text from the output of an HttpURLConnection. If the text isn't found an assertion error is thrown.
     *
     * @param charsetName The character set name. If null, use the default character set.
     * @param con The HttpURLconnection to retrieve the text from
     * @param stringsToFind The strings to search for, there must be at least one string to find. Duplicates will only be searched for once
     * @throws IOException
     * @throws {@link AssertionError} If the text isn't found
     * @throws IllegalArgumentException If <code>stringsToFind</code> is <code>null</code> or empty
     */
    public static void findStringInHttpConnection(String charsetName, HttpURLConnection con, String... stringsToFind) throws IOException {
        try {
            BufferedReader br = charsetName == null ? getResponseBody(con) : getResponseBody(con, charsetName);
            String line;

            // Convert the strings to find to a set and then remove strings as they are found
            final Set<String> stringsToFindSet = toSet(stringsToFind);

            final StringBuilder outputBuilder = new StringBuilder();

            while ((line = br.readLine()) != null) {
                outputBuilder.append(line);
                outputBuilder.append("\n");
                // Continue to read until response is finished, but only bother
                // looking at lines if we're still trying to find stuff..
                if (stringsToFindSet.size() > 0) {
                    Iterator<String> stringToFindIterator = stringsToFindSet.iterator();
                    while (stringToFindIterator.hasNext()) {
                        String textToFind = stringToFindIterator.next();
                        if (line.contains(textToFind)) {
                            stringToFindIterator.remove();
                        }
                    }
                }
            }

            //AccessController.doPrivileged(new PrivilegedAction<Void>() {
            //    @Override
            //    public Void run() {
            assertEquals("The response did not contain \"" + stringsToFindSet.toString() + "\".  Full output is:\"\n" + outputBuilder.toString() + "\".",
                         0, stringsToFindSet.size());
            //        return null;

            //    }
            //});

        } catch (IOException e) {
            Log.info(HttpUtils.class, "findStringInHttpConnection", "Exception " + e.getClass().getName() + " requesting URL=" + con.getURL().toString(), e);
            throw e;
        } finally {
            if (con != null) {
                con.disconnect();
            }
        }
    }

    private static Set<String> toSet(String[] array) {
        Set<String> collection = new HashSet<String>();
        for (String string : array) {
            collection.add(string);
        }
        return collection;
    }

    /**
     * This gets an HttpURLConnection to the requested address
     *
     * @param url The URL to get a connection to
     * @param requestMethod The HTTP request method (GET, POST, HEAD, OPTIONS, PUT, DELETE, TRACE)
     * @return
     * @throws IOException
     * @throws ProtocolException
     */
    public static HttpURLConnection getHttpConnection(URL url, int timeout, HTTPRequestMethod requestMethod) throws IOException, ProtocolException {
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setDoInput(true);
        con.setDoOutput(true);
        con.setUseCaches(false);
        con.setRequestMethod(requestMethod.toString());
        con.setConnectTimeout(timeout);

        if (con instanceof HttpsURLConnection) {
            ((HttpsURLConnection) con).setHostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            });
        }

        return con;
    }

    public static void trustAllCertificates() throws Exception {
        TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            @Override
            public void checkClientTrusted(X509Certificate[] certs, String authType) {}

            @Override
            public void checkServerTrusted(X509Certificate[] certs, String authType) {}
        } };

        SSLContext sc = SSLContext.getInstance("SSL");
        sc.init(null, trustAllCerts, new SecureRandom());
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
    }

    public static void trustAllHostnames() {
        HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
            @Override
            public boolean verify(String hostname, SSLSession session) {
                return true;
            }
        });
    }

    public static void setDefaultAuth(final String user, final String password) {
        Authenticator.setDefault(new Authenticator() {
            @Override
            public PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(user, password.toCharArray());
            }
        });
    }

    public static String readConnection(HttpURLConnection con) throws IOException {
        InputStream is = con.getInputStream();
        try {
            return read(is);
        } finally {
            is.close();
        }
    }

    public static String getHttpResponseAsString(String urlStr) throws IOException {
        URL url = new URL(urlStr);
        return getHttpResponseAsString(url);
    }

    public static String getHttpResponseAsString(LibertyServer server, String urlSuffix) throws IOException {
        URL url = createURL(server, urlSuffix);
        return getHttpResponseAsString(url);
    }

    public static String getHttpResponseAsString(URL url) throws IOException {
        final String method = "getHttpResponseAsString";
        Log.info(c, method, "url = " + url);
        HttpURLConnection con = null;
        InputStream is = null;
        try {
            con = (HttpURLConnection) url.openConnection();
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setUseCaches(false);
            con.setRequestMethod("GET");
            // See if we can get the stream for the response, may be an input or error stream
            try {
                is = con.getInputStream();
            } catch (FileNotFoundException e) {
                // No input stream so try the error
                is = con.getErrorStream();
            } catch (IOException ioe) {
                String errStream = read(con.getErrorStream());
                Log.info(c, method, "rc=" + con.getResponseCode() + " response=" + LS + errStream);
                throw new IOException(ioe.getMessage() + ": " + errStream, ioe);
            }

            String output = read(is);
            Log.info(c, method, "rc=" + con.getResponseCode() + " response=" + LS + output);
            return output;
        } finally {
            if (con != null) {
                con.disconnect();
            }
            if (is != null) {
                is.close();
            }
        }
    }

    private static String read(InputStream in) throws IOException {
        if (in == null) {
            return null;
        }
        BufferedReader br = getBufferedReader(in);
        StringBuilder builder = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            builder.append(line);
            builder.append(LS);
        }
        return builder.toString();
    }
}
