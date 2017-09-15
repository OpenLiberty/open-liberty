/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.security.metadata;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.ibm.ws.webcontainer.security.metadata.URLMatchingUtils;

/**
 *
 */
public class URLMatchingUtilsTest {

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.metadata.URLMatchingUtils#getLongestUrlPattern(java.lang.String, java.lang.String)}.
     */
    @Test
    public void getLongestUrlPattern_nulls() {
        assertNull("Should be null", URLMatchingUtils.getLongestUrlPattern(null, null));
    }

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.metadata.URLMatchingUtils#getLongestUrlPattern(java.lang.String, java.lang.String)}.
     */
    @Test
    public void getLongestUrlPattern_firstNull() {
        String url1 = null;
        String url2 = "url";
        assertEquals("Should be second url", url2, URLMatchingUtils.getLongestUrlPattern(url1, url2));
    }

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.metadata.URLMatchingUtils#getLongestUrlPattern(java.lang.String, java.lang.String)}.
     */
    @Test
    public void getLongestUrlPattern_secondNull() {
        String url1 = "url";
        String url2 = null;
        assertEquals("Should be first url", url1, URLMatchingUtils.getLongestUrlPattern(url1, url2));
    }

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.metadata.URLMatchingUtils#getLongestUrlPattern(java.lang.String, java.lang.String)}.
     */
    @Test
    public void getLongestUrlPattern_firstUrlLonger() {
        String url1 = "1234";
        String url2 = "123";
        assertEquals("Should be first url", url1, URLMatchingUtils.getLongestUrlPattern(url1, url2));
    }

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.metadata.URLMatchingUtils#getLongestUrlPattern(java.lang.String, java.lang.String)}.
     */
    @Test
    public void getLongestUrlPattern_secondUrlLonger() {
        String url1 = "123";
        String url2 = "1234";
        assertEquals("Should be second url", url2, URLMatchingUtils.getLongestUrlPattern(url1, url2));
    }

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.metadata.URLMatchingUtils#getLongestUrlPattern(java.lang.String, java.lang.String)}.
     */
    @Test
    public void getLongestUrlPattern_equal() {
        String url1 = "123";
        String url2 = "123";
        assertEquals("Should be first url", url1, URLMatchingUtils.getLongestUrlPattern(url1, url2));
    }

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.metadata.URLMatchingUtils#isExactMatch(java.lang.String, java.lang.String)}.
     */
    @Test
    public void isExactMatch_match() {
        assertTrue("Two equal strings should match",
                   URLMatchingUtils.isExactMatch("/path/file", "/path/file"));
    }

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.metadata.URLMatchingUtils#isExactMatch(java.lang.String, java.lang.String)}.
     */
    @Test
    public void isExactMatch_noMatch() {
        assertFalse("Two un-equal strings should not match",
                    URLMatchingUtils.isExactMatch("/path/file1", "/path/file"));
    }

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.metadata.URLMatchingUtils#isPathNameMatch(java.lang.String, java.lang.String)}.
     */
    @Test
    public void isPathNameMatch_nonWildcardPattern() {
        assertFalse("Even an exact match should fail this check",
                    URLMatchingUtils.isPathNameMatch("/path/file", "/path/abc"));
    }

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.metadata.URLMatchingUtils#isPathNameMatch(java.lang.String, java.lang.String)}.
     */
    @Test
    public void isPathNameMatch_exactMatch() {
        assertFalse("Even an exact match should fail this check",
                    URLMatchingUtils.isPathNameMatch("/path/file", "/path/file"));
    }

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.metadata.URLMatchingUtils#isPathNameMatch(java.lang.String, java.lang.String)}.
     */
    @Test
    public void isPathNameMatch_subPatternNonWildcard() {
        assertFalse("A sub-pattern without a wildcard does not match",
                    URLMatchingUtils.isPathNameMatch("/path/file", "/path"));
    }

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.metadata.URLMatchingUtils#isPathNameMatch(java.lang.String, java.lang.String)}.
     */
    @Test
    public void isPathNameMatch_tooLongMatchWithWildcard() {
        assertTrue("A wildcard comes after a slash and the URL must match to last slash",
                    URLMatchingUtils.isPathNameMatch("/path/file", "/path/file/*"));
    }

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.metadata.URLMatchingUtils#isPathNameMatch(java.lang.String, java.lang.String)}.
     */
    @Test
    public void isPathNameMatch_fullMatchWithWildcard() {
        assertTrue("A wildcard comes after a slash and the URL must match to last slash",
                    URLMatchingUtils.isPathNameMatch("/path/file/", "/path/file/*"));
    }

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.metadata.URLMatchingUtils#isPathNameMatch(java.lang.String, java.lang.String)}.
     */
    @Test
    public void isPathNameMatch_submatcher() {
        assertTrue("A wildcard comes after a slash and the URL must match to last slash",
                   URLMatchingUtils.isPathNameMatch("/path/file/", "/path/*"));
    }

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.metadata.URLMatchingUtils#isExtensionMatch(java.lang.String, java.lang.String)}.
     */
    @Test
    public void isExtensionMatch_nonPattern() {
        assertFalse("A non-pattern should be false",
                    URLMatchingUtils.isExtensionMatch("/path/img.jpg", "/path"));
    }

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.metadata.URLMatchingUtils#isExtensionMatch(java.lang.String, java.lang.String)}.
     */
    @Test
    public void isExtensionMatch_nonMatchingPattern() {
        assertFalse("A non-matching pattern should be false",
                    URLMatchingUtils.isExtensionMatch("/path/img.jpg", "*.gif"));
    }

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.metadata.URLMatchingUtils#isExtensionMatch(java.lang.String, java.lang.String)}.
     */
    @Test
    public void isExtensionMatch_matchingPattern() {
        assertTrue("A matching pattern should be true",
                    URLMatchingUtils.isExtensionMatch("/path/img.jpg", "*.jpg"));
    }

}
