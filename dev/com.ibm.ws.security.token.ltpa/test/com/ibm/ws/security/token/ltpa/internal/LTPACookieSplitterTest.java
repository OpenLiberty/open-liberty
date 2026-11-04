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
package com.ibm.ws.security.token.ltpa.internal;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.junit.Test;

import com.ibm.websphere.security.auth.InvalidTokenException;

/**
 * Unit tests for LTPACookieSplitter.
 */
public class LTPACookieSplitterTest {
    
    private static final String COOKIE_NAME = "LtpaToken3";
    
    /**
     * Test that small tokens don't require splitting.
     */
    @Test
    public void testSmallTokenNoSplitting() {
        byte[] smallToken = new byte[1000]; // 1KB token
        assertFalse("Small token should not require splitting", 
                   LTPACookieSplitter.requiresSplitting(smallToken));
    }
    
    /**
     * Test that large tokens require splitting.
     */
    @Test
    public void testLargeTokenRequiresSplitting() {
        byte[] largeToken = new byte[3500]; // 3.5KB token (will be ~4.7KB Base64)
        assertTrue("Large token should require splitting", 
                  LTPACookieSplitter.requiresSplitting(largeToken));
    }
    
    /**
     * Test splitting and reassembling a small token (single cookie).
     */
    @Test
    public void testSplitAndReassembleSmallToken() throws Exception {
        byte[] originalToken = generateRandomBytes(1000);
        
        Map<String, String> cookies = LTPACookieSplitter.splitToken(originalToken, COOKIE_NAME);
        
        // Should have only main cookie for small token
        assertEquals("Should have 1 cookie", 1, cookies.size());
        assertTrue("Should have main cookie", cookies.containsKey(COOKIE_NAME));
        
        byte[] reassembled = LTPACookieSplitter.reassembleToken(cookies, COOKIE_NAME);
        
        assertArrayEquals("Reassembled token should match original", originalToken, reassembled);
    }
    
    /**
     * Test splitting and reassembling a large token (multiple cookies).
     */
    @Test
    public void testSplitAndReassembleLargeToken() throws Exception {
        byte[] originalToken = generateRandomBytes(3530); // Typical LTPA3 size
        
        Map<String, String> cookies = LTPACookieSplitter.splitToken(originalToken, COOKIE_NAME);
        
        // Should have multiple cookies
        assertTrue("Should have multiple cookies", cookies.size() > 1);
        assertTrue("Should have main cookie", cookies.containsKey(COOKIE_NAME));
        assertTrue("Should have fragment cookie", cookies.containsKey(COOKIE_NAME + "_1"));
        
        byte[] reassembled = LTPACookieSplitter.reassembleToken(cookies, COOKIE_NAME);
        
        assertArrayEquals("Reassembled token should match original", originalToken, reassembled);
    }
    
    /**
     * Test splitting a very large token (3+ fragments).
     */
    @Test
    public void testSplitVeryLargeToken() throws Exception {
        byte[] originalToken = generateRandomBytes(7000); // 7KB token
        
        Map<String, String> cookies = LTPACookieSplitter.splitToken(originalToken, COOKIE_NAME);
        
        // Should have 3+ cookies
        assertTrue("Should have at least 3 cookies", cookies.size() >= 3);
        assertTrue("Should have main cookie", cookies.containsKey(COOKIE_NAME));
        assertTrue("Should have fragment 1", cookies.containsKey(COOKIE_NAME + "_1"));
        assertTrue("Should have fragment 2", cookies.containsKey(COOKIE_NAME + "_2"));
        
        byte[] reassembled = LTPACookieSplitter.reassembleToken(cookies, COOKIE_NAME);
        
        assertArrayEquals("Reassembled token should match original", originalToken, reassembled);
    }
    
    /**
     * Test that isFragmented correctly identifies fragmented tokens.
     */
    @Test
    public void testIsFragmented() throws Exception {
        // Small token - not fragmented
        byte[] smallToken = generateRandomBytes(1000);
        Map<String, String> smallCookies = LTPACookieSplitter.splitToken(smallToken, COOKIE_NAME);
        assertFalse("Small token should not be fragmented", 
                   LTPACookieSplitter.isFragmented(smallCookies, COOKIE_NAME));
        
        // Large token - fragmented
        byte[] largeToken = generateRandomBytes(3530);
        Map<String, String> largeCookies = LTPACookieSplitter.splitToken(largeToken, COOKIE_NAME);
        assertTrue("Large token should be fragmented", 
                  LTPACookieSplitter.isFragmented(largeCookies, COOKIE_NAME));
    }
    
    /**
     * Test reassembly with missing main cookie.
     */
    @Test(expected = InvalidTokenException.class)
    public void testReassembleWithMissingMainCookie() throws Exception {
        Map<String, String> cookies = new HashMap<>();
        cookies.put(COOKIE_NAME + "_1", "fragment1");
        
        LTPACookieSplitter.reassembleToken(cookies, COOKIE_NAME);
    }
    
    /**
     * Test reassembly with missing fragment cookie.
     */
    @Test(expected = InvalidTokenException.class)
    public void testReassembleWithMissingFragment() throws Exception {
        byte[] originalToken = generateRandomBytes(3530);
        Map<String, String> cookies = LTPACookieSplitter.splitToken(originalToken, COOKIE_NAME);
        
        // Remove a fragment
        cookies.remove(COOKIE_NAME + "_1");
        
        LTPACookieSplitter.reassembleToken(cookies, COOKIE_NAME);
    }
    
    /**
     * Test reassembly with corrupted main cookie.
     */
    @Test(expected = InvalidTokenException.class)
    public void testReassembleWithCorruptedMainCookie() throws Exception {
        Map<String, String> cookies = new HashMap<>();
        cookies.put(COOKIE_NAME, "corrupted_base64_data!!!!");
        
        LTPACookieSplitter.reassembleToken(cookies, COOKIE_NAME);
    }
    
    /**
     * Test reassembly with corrupted fragment.
     */
    @Test(expected = InvalidTokenException.class)
    public void testReassembleWithCorruptedFragment() throws Exception {
        byte[] originalToken = generateRandomBytes(3530);
        Map<String, String> cookies = LTPACookieSplitter.splitToken(originalToken, COOKIE_NAME);
        
        // Corrupt a fragment
        cookies.put(COOKIE_NAME + "_1", "corrupted_data!!!!");
        
        LTPACookieSplitter.reassembleToken(cookies, COOKIE_NAME);
    }
    
    /**
     * Test splitting with null token.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testSplitWithNullToken() {
        LTPACookieSplitter.splitToken(null, COOKIE_NAME);
    }
    
    /**
     * Test splitting with empty token.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testSplitWithEmptyToken() {
        LTPACookieSplitter.splitToken(new byte[0], COOKIE_NAME);
    }
    
    /**
     * Test reassembly with null cookies.
     */
    @Test(expected = InvalidTokenException.class)
    public void testReassembleWithNullCookies() throws Exception {
        LTPACookieSplitter.reassembleToken(null, COOKIE_NAME);
    }
    
    /**
     * Test reassembly with empty cookies.
     */
    @Test(expected = InvalidTokenException.class)
    public void testReassembleWithEmptyCookies() throws Exception {
        LTPACookieSplitter.reassembleToken(new HashMap<>(), COOKIE_NAME);
    }
    
    /**
     * Test that each fragment stays within size limits.
     */
    @Test
    public void testFragmentSizeLimits() throws Exception {
        byte[] largeToken = generateRandomBytes(10000); // 10KB token
        
        Map<String, String> cookies = LTPACookieSplitter.splitToken(largeToken, COOKIE_NAME);
        
        // Check that each cookie is within reasonable size
        for (Map.Entry<String, String> entry : cookies.entrySet()) {
            int cookieSize = entry.getValue().length();
            assertTrue("Cookie " + entry.getKey() + " too large: " + cookieSize, 
                      cookieSize <= 4000); // Base64 encoded size
        }
    }
    
    /**
     * Test splitting with custom cookie name.
     */
    @Test
    public void testCustomCookieName() throws Exception {
        String customName = "CustomToken";
        byte[] token = generateRandomBytes(3530);
        
        Map<String, String> cookies = LTPACookieSplitter.splitToken(token, customName);
        
        assertTrue("Should have custom main cookie", cookies.containsKey(customName));
        assertTrue("Should have custom fragment", cookies.containsKey(customName + "_1"));
        
        byte[] reassembled = LTPACookieSplitter.reassembleToken(cookies, customName);
        assertArrayEquals("Reassembled token should match", token, reassembled);
    }
    
    /**
     * Test that requiresSplitting handles null input.
     */
    @Test
    public void testRequiresSplittingWithNull() {
        assertFalse("Null token should not require splitting", 
                   LTPACookieSplitter.requiresSplitting(null));
    }
    
    /**
     * Test boundary case: token exactly at split threshold.
     */
    @Test
    public void testBoundaryCase() throws Exception {
        // Calculate size that's right at the boundary
        int boundarySize = 2250; // Approximately at the split threshold
        byte[] token = generateRandomBytes(boundarySize);
        
        Map<String, String> cookies = LTPACookieSplitter.splitToken(token, COOKIE_NAME);
        byte[] reassembled = LTPACookieSplitter.reassembleToken(cookies, COOKIE_NAME);
        
        assertArrayEquals("Boundary case should work correctly", token, reassembled);
    }
    
    /**
     * Test with realistic LTPA3 token sizes.
     */
    @Test
    public void testRealisticLTPA3Sizes() throws Exception {
        // Test ML-KEM-512 token size
        testTokenSize(3530);
        
        // Test ML-KEM-768 token size (larger)
        testTokenSize(4500);
        
        // Test ML-KEM-1024 token size (largest)
        testTokenSize(5500);
    }
    
    private void testTokenSize(int size) throws Exception {
        byte[] token = generateRandomBytes(size);
        Map<String, String> cookies = LTPACookieSplitter.splitToken(token, COOKIE_NAME);
        byte[] reassembled = LTPACookieSplitter.reassembleToken(cookies, COOKIE_NAME);
        assertArrayEquals("Token size " + size + " should work", token, reassembled);
    }
    
    /**
     * Generate random bytes for testing.
     */
    private byte[] generateRandomBytes(int size) {
        byte[] bytes = new byte[size];
        new Random().nextBytes(bytes);
        return bytes;
    }
}

// Made with Bob
