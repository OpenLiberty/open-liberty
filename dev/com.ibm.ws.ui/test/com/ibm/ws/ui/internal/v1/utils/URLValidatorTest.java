/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.net.MalformedURLException;
import java.net.URL;

import org.junit.Test;

/**
 * Unit tests for URLValidator to ensure SSRF protection
 */
public class URLValidatorTest {

    /**
     * Test that valid HTTPS URLs are accepted
     */
    @Test
    public void testValidHttpsURL() throws Exception {
        URL url = new URL("https://www.ibm.com");
        URLValidator.validateURL(url);
        // Should not throw exception
    }

    /**
     * Test that valid HTTP URLs are accepted
     */
    @Test
    public void testValidHttpURL() throws Exception {
        URL url = new URL("http://www.example.com");
        URLValidator.validateURL(url);
        // Should not throw exception
    }

    /**
     * Test that file:// scheme is blocked
     */
    @Test
    public void testFileSchemeBlocked() throws Exception {
        URL url = new URL("file:///etc/passwd");
        try {
            URLValidator.validateURL(url);
            fail("Should have thrown SecurityException for file:// scheme");
        } catch (SecurityException e) {
            assertNotNull("Exception message should not be null", e.getMessage());
            assertEquals("Should reject file scheme", true, 
                        e.getMessage().contains("not allowed"));
        }
    }

    /**
     * Test that ftp:// scheme is blocked
     */
    @Test
    public void testFtpSchemeBlocked() throws Exception {
        URL url = new URL("ftp://ftp.example.com/file.txt");
        try {
            URLValidator.validateURL(url);
            fail("Should have thrown SecurityException for ftp:// scheme");
        } catch (SecurityException e) {
            assertNotNull("Exception message should not be null", e.getMessage());
        }
    }

    /**
     * Test that localhost is blocked
     */
    @Test
    public void testLocalhostBlocked() throws Exception {
        URL url = new URL("http://localhost:8080");
        try {
            URLValidator.validateURL(url);
            fail("Should have thrown SecurityException for localhost");
        } catch (SecurityException e) {
            assertNotNull("Exception message should not be null", e.getMessage());
            assertEquals("Should reject localhost", true, 
                        e.getMessage().contains("localhost"));
        }
    }

    /**
     * Test that 127.0.0.1 is blocked
     */
    @Test
    public void testLoopbackIPBlocked() throws Exception {
        URL url = new URL("http://127.0.0.1:9080");
        try {
            URLValidator.validateURL(url);
            fail("Should have thrown SecurityException for 127.0.0.1");
        } catch (SecurityException e) {
            assertNotNull("Exception message should not be null", e.getMessage());
        }
    }

    /**
     * Test that private IP 10.x.x.x is blocked
     */
    @Test
    public void testPrivateIP10Blocked() throws Exception {
        URL url = new URL("http://10.0.0.1");
        try {
            URLValidator.validateURL(url);
            fail("Should have thrown SecurityException for 10.0.0.1");
        } catch (SecurityException e) {
            assertNotNull("Exception message should not be null", e.getMessage());
            assertEquals("Should reject private IP", true, 
                        e.getMessage().contains("private"));
        }
    }

    /**
     * Test that private IP 192.168.x.x is blocked
     */
    @Test
    public void testPrivateIP192Blocked() throws Exception {
        URL url = new URL("http://192.168.1.1");
        try {
            URLValidator.validateURL(url);
            fail("Should have thrown SecurityException for 192.168.1.1");
        } catch (SecurityException e) {
            assertNotNull("Exception message should not be null", e.getMessage());
        }
    }

    /**
     * Test that private IP 172.16-31.x.x is blocked
     */
    @Test
    public void testPrivateIP172Blocked() throws Exception {
        URL url = new URL("http://172.16.0.1");
        try {
            URLValidator.validateURL(url);
            fail("Should have thrown SecurityException for 172.16.0.1");
        } catch (SecurityException e) {
            assertNotNull("Exception message should not be null", e.getMessage());
        }
    }

    /**
     * Test that link-local IP 169.254.x.x is blocked
     */
    @Test
    public void testLinkLocalIPBlocked() throws Exception {
        URL url = new URL("http://169.254.169.254");
        try {
            URLValidator.validateURL(url);
            fail("Should have thrown SecurityException for 169.254.169.254");
        } catch (SecurityException e) {
            assertNotNull("Exception message should not be null", e.getMessage());
        }
    }

    /**
     * Test that IPv6 localhost ::1 is blocked
     */
    @Test
    public void testIPv6LocalhostBlocked() throws Exception {
        URL url = new URL("http://[::1]:8080");
        try {
            URLValidator.validateURL(url);
            fail("Should have thrown SecurityException for ::1");
        } catch (SecurityException e) {
            assertNotNull("Exception message should not be null", e.getMessage());
        }
    }

    /**
     * Test that excessively long URLs are blocked
     */
    @Test
    public void testExcessivelyLongURLBlocked() throws Exception {
        StringBuilder longUrl = new StringBuilder("http://example.com/");
        for (int i = 0; i < 3000; i++) {
            longUrl.append("a");
        }
        
        URL url = new URL(longUrl.toString());
        try {
            URLValidator.validateURL(url);
            fail("Should have thrown SecurityException for excessively long URL");
        } catch (SecurityException e) {
            assertNotNull("Exception message should not be null", e.getMessage());
            assertEquals("Should reject long URL", true, 
                        e.getMessage().contains("exceeds maximum"));
        }
    }

    /**
     * Test that null URL is rejected
     */
    @Test
    public void testNullURLRejected() {
        try {
            URLValidator.validateURL(null);
            fail("Should have thrown SecurityException for null URL");
        } catch (SecurityException e) {
            assertNotNull("Exception message should not be null", e.getMessage());
            assertEquals("Should reject null", true, 
                        e.getMessage().contains("cannot be null"));
        }
    }

    /**
     * Test validateAndCreateURL with valid URL
     */
    @Test
    public void testValidateAndCreateURL_Valid() throws Exception {
        URL url = URLValidator.validateAndCreateURL("https://www.ibm.com");
        assertNotNull("URL should not be null", url);
        assertEquals("https", url.getProtocol());
        assertEquals("www.ibm.com", url.getHost());
    }

    /**
     * Test validateAndCreateURL with invalid scheme
     */
    @Test
    public void testValidateAndCreateURL_InvalidScheme() {
        try {
            URLValidator.validateAndCreateURL("file:///etc/passwd");
            fail("Should have thrown SecurityException");
        } catch (MalformedURLException e) {
            fail("Should have thrown SecurityException, not MalformedURLException");
        } catch (SecurityException e) {
            assertNotNull("Exception message should not be null", e.getMessage());
        }
    }

    /**
     * Test validateAndCreateURL with localhost
     */
    @Test
    public void testValidateAndCreateURL_Localhost() {
        try {
            URLValidator.validateAndCreateURL("http://localhost:8080");
            fail("Should have thrown SecurityException");
        } catch (MalformedURLException e) {
            fail("Should have thrown SecurityException, not MalformedURLException");
        } catch (SecurityException e) {
            assertNotNull("Exception message should not be null", e.getMessage());
        }
    }

    /**
     * Test validateAndCreateURL with null string
     */
    @Test
    public void testValidateAndCreateURL_NullString() {
        try {
            URLValidator.validateAndCreateURL(null);
            fail("Should have thrown SecurityException");
        } catch (MalformedURLException e) {
            fail("Should have thrown SecurityException, not MalformedURLException");
        } catch (SecurityException e) {
            assertNotNull("Exception message should not be null", e.getMessage());
        }
    }

    /**
     * Test validateAndCreateURL with empty string
     */
    @Test
    public void testValidateAndCreateURL_EmptyString() {
        try {
            URLValidator.validateAndCreateURL("");
            fail("Should have thrown SecurityException");
        } catch (MalformedURLException e) {
            fail("Should have thrown SecurityException, not MalformedURLException");
        } catch (SecurityException e) {
            assertNotNull("Exception message should not be null", e.getMessage());
        }
    }

    /**
     * Test that 0.0.0.0 is blocked
     */
    @Test
    public void testZeroIPBlocked() throws Exception {
        URL url = new URL("http://0.0.0.0");
        try {
            URLValidator.validateURL(url);
            fail("Should have thrown SecurityException for 0.0.0.0");
        } catch (SecurityException e) {
            assertNotNull("Exception message should not be null", e.getMessage());
        }
    }
}

// Made with Bob
