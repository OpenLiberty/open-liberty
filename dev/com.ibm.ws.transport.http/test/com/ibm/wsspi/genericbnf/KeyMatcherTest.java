/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.wsspi.genericbnf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import com.ibm.wsspi.bytebuffer.WsByteBuffer;
import com.ibm.wsspi.genericbnf.exception.MalformedMessageException;

/**
 * Test KeyMatcher functionality
 */
public class KeyMatcherTest {

    /**
     * Test implementation of GenericKeys for testing purposes
     */
    private static class TestGenericKey extends GenericKeys {
        public TestGenericKey(String name, int ordinal) {
            super(name, ordinal);
        }
    }

    /**
     * Helper method to match a byte array against the KeyMatcher
     */
    private GenericKeys matchBytes(KeyMatcher matcher, byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return matcher.match(bytes, 0, 0);
        }
        return matcher.match(bytes, 0, bytes.length);
    }

    @Test
    public void testStandardHeaderMatching() throws Exception {
        KeyMatcher matcher = new KeyMatcher(false); // case-insensitive
        
        // Add standard HTTP headers using GenericKeys
        matcher.add(new TestGenericKey("Content-Length", 1));
        matcher.add(new TestGenericKey("Content-Type", 2));
        matcher.add(new TestGenericKey("Host", 3));
        
        // Test exact matches
        assertEquals("Content-Length", matchBytes(matcher, "Content-Length".getBytes()).getName());
        assertEquals("Content-Type", matchBytes(matcher, "Content-Type".getBytes()).getName());
        assertEquals("Host", matchBytes(matcher, "Host".getBytes()).getName());
        
        // Test case variations (case-insensitive matcher)
        assertEquals("Content-Length", matchBytes(matcher, "content-length".getBytes()).getName());
        assertEquals("Content-Length", matchBytes(matcher, "CONTENT-LENGTH".getBytes()).getName());
        assertEquals("Content-Type", matchBytes(matcher, "CONTENT-TYPE".getBytes()).getName());
        assertEquals("Host", matchBytes(matcher, "HOST".getBytes()).getName());
    }

    @Test
    public void testPunctuation() throws Exception {
        KeyMatcher matcher = new KeyMatcher(false); // case-insensitive
        
        matcher.add(new TestGenericKey("Content-Length", 1));
        

        assertNull("ContentMLength should NOT match Content-Length",
                   matchBytes(matcher, "ContentMLength".getBytes()));
        
        assertEquals("Content-Length", matchBytes(matcher, "Content-Length".getBytes()).getName());
        assertEquals("Content-Length", matchBytes(matcher, "content-length".getBytes()).getName());
    }

    @Test
    public void testASCIIMatches() throws Exception {
        KeyMatcher matcher = new KeyMatcher(false); // case-insensitive
        
        // Add a header with uppercase letters
        matcher.add(new TestGenericKey("ABCDEFGHIJKLMNOPQRSTUVWXYZ", 1));
        
        assertNull(matchBytes(matcher, "!\"#$%&'()*+,-./0123456789:".getBytes()));
        
        // Verify correct case variations still work
        assertEquals("ABCDEFGHIJKLMNOPQRSTUVWXYZ", matchBytes(matcher, "ABCDEFGHIJKLMNOPQRSTUVWXYZ".getBytes()).getName());
        assertEquals("ABCDEFGHIJKLMNOPQRSTUVWXYZ", matchBytes(matcher, "abcdefghijklmnopqrstuvwxyz".getBytes()).getName());
        assertEquals("ABCDEFGHIJKLMNOPQRSTUVWXYZ", matchBytes(matcher, "AbCdEfGhIjKlMnOpQrStUvWxYz".getBytes()).getName());
    }

    @Test
    public void testVariations() throws Exception {
        KeyMatcher matcher = new KeyMatcher(false); // case-insensitive
        
        matcher.add(new TestGenericKey("Transfer-Encoding", 1));
        matcher.add(new TestGenericKey("Accept-Encoding", 2));
        matcher.add(new TestGenericKey("User-Agent", 3));
        
        // Test various case combinations
        assertEquals("Transfer-Encoding", matchBytes(matcher, "Transfer-Encoding".getBytes()).getName());
        assertEquals("Transfer-Encoding", matchBytes(matcher, "transfer-encoding".getBytes()).getName());
        assertEquals("Transfer-Encoding", matchBytes(matcher, "TRANSFER-ENCODING".getBytes()).getName());
        assertEquals("Transfer-Encoding", matchBytes(matcher, "TrAnSfEr-EnCoDiNg".getBytes()).getName());
        
        assertEquals("Accept-Encoding", matchBytes(matcher, "Accept-Encoding".getBytes()).getName());
        assertEquals("Accept-Encoding", matchBytes(matcher, "ACCEPT-ENCODING".getBytes()).getName());
        
        assertEquals("User-Agent", matchBytes(matcher, "User-Agent".getBytes()).getName());
        assertEquals("User-Agent", matchBytes(matcher, "USER-AGENT".getBytes()).getName());
    }

    @Test
    public void testCaseSensitiveMatching() throws Exception {
        KeyMatcher matcher = new KeyMatcher(true); // case-sensitive
        
        matcher.add(new TestGenericKey("Content-Length", 1));
        matcher.add(new TestGenericKey("content-length", 2));
        
        assertEquals("Content-Length", matchBytes(matcher, "Content-Length".getBytes()).getName());
        assertEquals("content-length", matchBytes(matcher, "content-length".getBytes()).getName());
        
        assertNull(matchBytes(matcher, "CONTENT-LENGTH".getBytes()));
        assertNull(matchBytes(matcher, "content-LENGTH".getBytes()));
    }

    @Test
    public void testNullInputs() throws Exception {
        KeyMatcher matcher = new KeyMatcher(false);
        
        matcher.add(new TestGenericKey("Test", 1));
        
        assertNull(matchBytes(matcher, new byte[0]));
        
        // Null should not cause exception
        assertNull(matchBytes(matcher, null));
    }
}
