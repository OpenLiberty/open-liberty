/*******************************************************************************
 * Copyright (c) 2013, 2026 IBM Corporation and others.
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
package com.ibm.ws.ui.internal.v1.utils;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
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
import com.ibm.ws.ui.internal.v1.pojo.Bookmark;

/**
 *
 */
public class URLUtilityImpl implements URLUtility {
    private static final TraceComponent tc = Tr.register(URLUtilityImpl.class);

    // Security limits to prevent DoS attacks
    private static final int CONNECTION_TIMEOUT_MS = 10000; // 10 seconds
    private static final int READ_TIMEOUT_MS = 10000; // 10 seconds
    private static final int MAX_RESPONSE_SIZE_BYTES = 1048576; // 1 MB
    private static final int BUFFER_SIZE = 4096;

    // This Pattern is used to find the title field in a URL.
    private final static Pattern titlePattern = Pattern.compile("<title>(.*?)</title>");
    private final static Pattern descPattern = Pattern.compile("<meta name=\"[Dd]escription\".*?content=\"(.*?)\"");

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
        // URL has already been validated by URLUtils.getURLParameter() before reaching here.
        // Redirects are disabled to prevent redirect-based SSRF.
        boolean couldReachURL = true;
        URLConnection connection = null;
        InputStream cis = null;
        ByteArrayOutputStream bos = null;
        String name = "";
        String description = "";
        String icon = "images/tools/defaultTool_142x142.png";

        try {
            connection = url.openConnection();
            
            // SECURITY: Set connection and read timeouts to prevent hanging
            connection.setConnectTimeout(CONNECTION_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);
            
            // SECURITY: Disable redirects to prevent redirect-based SSRF
            if (connection instanceof HttpURLConnection) {
                ((HttpURLConnection) connection).setInstanceFollowRedirects(false);
            }
            
            try {
                cis = connection.getInputStream();
                bos = new ByteArrayOutputStream();
                
                byte[] buffer = new byte[BUFFER_SIZE];
                int bytesRead;
                int totalBytesRead = 0;
                
                // SECURITY: Read with size limit to prevent memory exhaustion
                while ((bytesRead = cis.read(buffer)) != -1) {
                    totalBytesRead += bytesRead;
                    
                    // SECURITY: Enforce maximum response size
                    if (totalBytesRead > MAX_RESPONSE_SIZE_BYTES) {
                        Tr.warning(tc, "CWWKX1057W: Response size exceeds maximum allowed size of " +
                                   MAX_RESPONSE_SIZE_BYTES + " bytes for URL: " + url);
                        couldReachURL = false;
                        break;
                    }
                    
                    bos.write(buffer, 0, bytesRead);
                }

                // Only process content if we successfully read within limits
                if (couldReachURL && totalBytesRead > 0) {
                    // Get the content of the page
                    String urlContent = bos.toString("UTF-8");
                    
                    // Check to see if we have a title in the page
                    Matcher titleMatcher = titlePattern.matcher(urlContent);
                    if (titleMatcher.find()) {
                        name = sanitizeExtractedText(titleMatcher.group(1));
                    }

                    // Check to see if we have a description meta in the page
                    Matcher descMatcher = descPattern.matcher(urlContent);
                    if (descMatcher.find()) {
                        description = sanitizeExtractedText(descMatcher.group(1));
                    }
                }
            } finally {
                closeCloseable(bos);
                closeCloseable(cis);
                
                // SECURITY: Ensure connection is properly closed
                if (connection instanceof HttpURLConnection) {
                    ((HttpURLConnection) connection).disconnect();
                }
            }
        } catch (SocketTimeoutException e) {
            Tr.warning(tc, "CWWKX1058W: Connection timeout while accessing URL: " + url);
            couldReachURL = false;
        } catch (IOException e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "IOException while accessing URL: " + url, e);
            }
            couldReachURL = false;
        }

        Map<String, Object> payload = new HashMap<String, Object>();
        payload.put("urlReachable", couldReachURL);
        payload.put("tool", new Bookmark(name, url.toString(), icon, description));
        return payload;
    }
    
    /**
     * Sanitizes extracted text to prevent injection attacks.
     * Limits length and removes potentially dangerous characters.
     *
     * @param text The text to sanitize
     * @return Sanitized text
     */
    private String sanitizeExtractedText(String text) {
        if (text == null) {
            return "";
        }
        
        // Limit length
        final int MAX_TEXT_LENGTH = 500;
        if (text.length() > MAX_TEXT_LENGTH) {
            text = text.substring(0, MAX_TEXT_LENGTH);
        }
        
        // Remove control characters and trim
        text = text.replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "").trim();
        
        return text;
    }

}
