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
package com.ibm.ws.ui.internal.v1.utils;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.ui.internal.rest.HTTPConstants;
import com.ibm.ws.ui.internal.rest.exceptions.RESTException;
import com.ibm.ws.ui.internal.v1.pojo.Bookmark;

/**
 *
 */
public class URLUtilityImpl implements URLUtility {
    private static final TraceComponent tc = Tr.register(URLUtilityImpl.class);

    // This Pattern is used to find the title field in a URL.
    private final static Pattern titlePattern = Pattern.compile("<title>(.*?)</title>");
    private final static Pattern descPattern = Pattern.compile("<meta name=\"[Dd]escription\".*?content=\"(.*?)\"");

    /**
     * Returns the HTTP status code for a GET on the provided URL.
     * <p>
     * If the URL could not be reached, return a 404.
     * 
     * @param url The URL to GET
     * @return The HTTP status code for a GET
     * @throws RESTException if there was a problem accessing the URL
     */
    @FFDCIgnore(IOException.class)
    private int httpGETStatus(final URL url) throws RESTException {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) url.openConnection();
            // disable https check for now
            if (conn instanceof HttpsURLConnection)
            {
                return HttpURLConnection.HTTP_OK;
            }
            return conn.getResponseCode();
        } catch (IOException e) {
            if (tc.isEventEnabled()) {
                Tr.event(tc,
                         "IOException caught while trying to resolve URL "
                                         + url.toString()
                                         + " -- some possible expected cases are: UnknownHostException (the URL host is not resolvable), ConnectException (the connection timed out or was otherwise rejected), SocketException or SSLHandshakeException (the request was over the HTTPS protocol which we don't support)",
                         e);
            }
            // If we hit an IO error, all we know is we couldn't reach it so 404
            return HTTPConstants.HTTP_NOT_FOUND;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public Map<String, Object> getStatus(final URL url) throws RESTException {
        Map<String, Object> payload = new HashMap<String, Object>();
        payload.put("url", url.toString());
        payload.put("status", httpGETStatus(url));
        return payload;
    }

    /**
     * Attempts to close the Closeable object. If an error occurs, ignore it.
     * 
     * @param c The Closeable object
     */
    private void closeCloseable(Closeable c) {
        if (c != null) {
            try {
                c.close();
            } catch (IOException e) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Error closing Closeable", e);
                }
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    @FFDCIgnore(IOException.class)
    public Map<String, Object> analyzeURL(final URL url) {
        boolean couldReachURL = true;

        URLConnection connection = null;
        InputStream cis = null;
        ByteArrayOutputStream bos = null;
        String name = "";
        String description = "";
        String icon = "images/tools/defaultTool_142x142.png";

        try {
            connection = url.openConnection();
            try {
                byte[] bytes = new byte[4096];
                cis = connection.getInputStream();
                bos = new ByteArrayOutputStream();

                while (cis.read(bytes) >= 0) {
                    bos.write(bytes);
                }

                // Get the content of the page. It seems that most web pages should be using ISO-8859-1 for encoding.
                String urlContent = bos.toString("UTF-8");
                // Check to see if we have a title in the page. This will be used for the tool name
                Matcher titleMatcher = titlePattern.matcher(urlContent);
                if (titleMatcher.find()) {
                    name = titleMatcher.group(1);
                }

                // Check to see if we have a description meta in the page. This will be used for the tool name
                Matcher descMatcher = descPattern.matcher(urlContent);
                if (descMatcher.find()) {
                    description = descMatcher.group(1);
                }
            } finally {
                closeCloseable(bos);
                closeCloseable(cis);
            }
        } catch (IOException e) {
            couldReachURL = false;
        }

        Map<String, Object> payload = new HashMap<String, Object>();
        payload.put("urlReachable", couldReachURL);
        payload.put("tool", new Bookmark(name, url.toString(), icon, description));
        return payload;
    }

}
