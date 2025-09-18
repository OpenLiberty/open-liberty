/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.Map;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import test.common.SharedOutputManager;

import com.ibm.ws.ui.internal.v1.pojo.Bookmark;

public class URLUtilityImplTest {
    static SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    @Rule
    public TestRule managerRule = outputMgr;

    private final Mockery mock = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };
    private final HttpURLConnection mockHTTPConnection = mock.mock(HttpURLConnection.class);

    private static final String DEFAULT_ICON_URL = "images/tools/defaultTool_142x142.png";
    private static final String EXPECTED_URL = "http://www.ibm.com";

    private URLUtility utils;
    private URL mockURL;

    @Before
    public void setUp() throws Exception {
        utils = new URLUtilityImpl();

        mock.checking(new Expectations() {
            {
                allowing(mockHTTPConnection).disconnect();
            }
        });

        mockURL = new URL("http", "www.ibm.com", -1, "", new URLStreamHandler() {
            @Override
            protected URLConnection openConnection(URL u) throws IOException {
                return mockHTTPConnection;
            }
        });
    }

    @After
    public void tearDown() {
        utils = null;
        mock.assertIsSatisfied();
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.utils.URLUtilityImpl#analyzeURL(java.net.URL)}.
     */
    @Test
    public void analyzeURL() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(mockHTTPConnection).getInputStream();
                will(returnValue(new ByteArrayInputStream("<title>aaa WebPage</title>/n<meta name=\"description\" content=\"This is the aaaa Website\"".getBytes())));
            }
        });

        Map<String, Object> ret = utils.analyzeURL(mockURL);

        assertTrue("FAIL: URL should be reachable",
                   (Boolean) ret.get("urlReachable"));
        Bookmark bookmark = (Bookmark) ret.get("tool");
        assertEquals("FAIL: tool didn't have a name of \"aaa WebPage\"",
                     "aaa WebPage", bookmark.getName());
        assertEquals("FAIL: tool didn't have a url entry of " + EXPECTED_URL,
                     EXPECTED_URL, bookmark.getURL());
        assertEquals("FAIL: tool didn't have an icon entry of " + DEFAULT_ICON_URL,
                     DEFAULT_ICON_URL, bookmark.getIcon());
        assertEquals("FAIL: tool didn't have a description of \"This is the aaaa Website\"",
                     "This is the aaaa Website", bookmark.getDescription());
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.utils.URLUtilityImpl#analyzeURL(java.net.URL)}.
     */
    @Test
    public void analyzeURL_noMetadata() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(mockHTTPConnection).getInputStream();
                will(returnValue(new ByteArrayInputStream("<notitle/><nodesc/>/n".getBytes())));
            }
        });

        Map<String, Object> ret = utils.analyzeURL(mockURL);

        assertTrue("FAIL: URL should be reachable",
                   (Boolean) ret.get("urlReachable"));

        Bookmark bookmark = (Bookmark) ret.get("tool");
        assertTrue("FAIL: tool didn't have an empty name",
                   bookmark.getName().isEmpty());
        assertEquals("FAIL: tool didn't have a url entry of " + EXPECTED_URL,
                     EXPECTED_URL, bookmark.getURL());
        assertEquals("FAIL: tool didn't have an icon entry of " + DEFAULT_ICON_URL,
                     DEFAULT_ICON_URL, bookmark.getIcon());
        assertTrue("FAIL: tool didn't have an empty description",
                   bookmark.getDescription().isEmpty());
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.utils.URLUtilityImpl#analyzeURL(java.net.URL)}.
     */
    @Test
    public void analyzeURL_invalidURL() throws Exception {
        mock.checking(new Expectations() {
            {

                allowing(mockHTTPConnection).getInputStream();
                will(throwException(new IOException("Unable to find Invalid Website")));
            }
        });

        Map<String, Object> ret = utils.analyzeURL(mockURL);

        assertFalse("FAIL: URL should be not reachable",
                    (Boolean) ret.get("urlReachable"));

        Bookmark bookmark = (Bookmark) ret.get("tool");
        assertTrue("FAIL: tool didn't have an empty name",
                   bookmark.getName().isEmpty());
        assertEquals("FAIL: tool didn't have a url entry of " + EXPECTED_URL,
                     EXPECTED_URL, bookmark.getURL());
        assertEquals("FAIL: tool didn't have an icon entry of " + DEFAULT_ICON_URL,
                     DEFAULT_ICON_URL, bookmark.getIcon());
        assertTrue("FAIL: tool didn't have an empty description",
                   bookmark.getDescription().isEmpty());
    }
}
