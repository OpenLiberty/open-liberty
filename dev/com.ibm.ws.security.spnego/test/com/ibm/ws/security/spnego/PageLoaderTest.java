/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.spnego;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

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

public class PageLoaderTest {
    static SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    @Rule
    public TestRule managerRule = outputMgr;

    private final Mockery mock = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    private final HttpURLConnection mockHTTPConnection = mock.mock(HttpURLConnection.class);

    private URL mockURL;

    private final String DEFAULT_ERROR_PAGE_CONTENT = "this is the default error page";
    private final String CUSTOM_ERROR_PAGE_CONTENT = "this is my custom error page";
    private final String DEFAULT_CONTENT_TYPE = "text/html";
    private final String DEFAULT_ENCODING = "UTF-8";
    private final String CONTENT_TYPE_HTMLX = "text/htmlx";
    private final String UTF16 = "UTF-16";
    private final String CUSTOM_ERROR_PAGE_URL = "http://localhost:9999/myErrorPage";

    @Before
    public void setUp() throws Exception {

        mockURL = new URL("http", "www.ibm.com", -1, "", new URLStreamHandler() {
            @Override
            protected URLConnection openConnection(URL u) throws IOException {
                return mockHTTPConnection;
            }
        });
    }

    @After
    public void tearDown() {
        mock.assertIsSatisfied();
        outputMgr.resetStreams();
    }

    @Test
    public void testDefaultErrorPage_noURL() {
        final String methodName = "testDefaultErrorPage_noURL";
        final PageLoader pageLoader = new PageLoader(null, DEFAULT_ERROR_PAGE_CONTENT);
        try {
            assertEquals("Error page content should be " + DEFAULT_ERROR_PAGE_CONTENT, DEFAULT_ERROR_PAGE_CONTENT, pageLoader.getContent());
            assertEquals("Error page default content type should be " + DEFAULT_CONTENT_TYPE, DEFAULT_CONTENT_TYPE, pageLoader.getContentType());
            assertEquals("Error page default encoding should be " + DEFAULT_ENCODING, DEFAULT_ENCODING, pageLoader.getEncoding());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testDefaultErrorPage_badURL() {
        final String methodName = "testErrorPage_badURL";
        final PageLoader pageLoader = new PageLoader("badURL", DEFAULT_ERROR_PAGE_CONTENT);
        try {
            assertEquals("Error page content should be " + DEFAULT_ERROR_PAGE_CONTENT, DEFAULT_ERROR_PAGE_CONTENT, pageLoader.getContent());
            assertEquals("Error page default content type should be " + DEFAULT_CONTENT_TYPE, DEFAULT_CONTENT_TYPE, pageLoader.getContentType());
            assertEquals("Error page default encoding should be " + DEFAULT_ENCODING, DEFAULT_ENCODING, pageLoader.getEncoding());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testErrorPageWithURL() {
        final String methodName = "testErrorPageWithURL";
        final PageLoader pageLoader = new PageLoader(CUSTOM_ERROR_PAGE_URL, DEFAULT_ERROR_PAGE_CONTENT);
        try {
            mock.checking(new Expectations() {
                {
                    allowing(mockHTTPConnection).getInputStream();
                    will(returnValue(new ByteArrayInputStream("this is my custom error page".getBytes(UTF16))));
                    allowing(mockHTTPConnection).getContent();
                    will(returnValue(CUSTOM_ERROR_PAGE_CONTENT));
                    allowing(mockHTTPConnection).getContentType();
                    will(returnValue(CONTENT_TYPE_HTMLX));
                    allowing(mockHTTPConnection).getContentEncoding();
                    will(returnValue(UTF16));
                }
            });

            pageLoader.loadCustomErrorPage(mockURL, CUSTOM_ERROR_PAGE_URL);
            assertTrue("content error page should be " + CUSTOM_ERROR_PAGE_CONTENT, pageLoader.getContent().contains(CUSTOM_ERROR_PAGE_CONTENT));
            assertEquals("default content type should be " + CONTENT_TYPE_HTMLX, CONTENT_TYPE_HTMLX, pageLoader.getContentType());
            assertEquals("default encoding should be " + UTF16, UTF16, pageLoader.getEncoding());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGetDefaultContentEncoding() {
        final String methodName = "testGetDefaultContentEncoding";
        final PageLoader pageLoader = new PageLoader(CUSTOM_ERROR_PAGE_URL, DEFAULT_ERROR_PAGE_CONTENT);
        try {
            assertNotNull("default content encode should not be null", pageLoader.getDefaultContentEncoding());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGetContentEncodingFromHttpContentType() {
        final String methodName = "testGetContentEncodingFromHttpContentType";
        final PageLoader pageLoader = new PageLoader(CUSTOM_ERROR_PAGE_URL, DEFAULT_ERROR_PAGE_CONTENT);
        String contentTypeNoEncode = "text/html";
        try {
            assertNull("contentType encoding should be null", pageLoader.getContentEncodingFromHttpContentType(null));
            assertNull("contentType encoding should be null", pageLoader.getContentEncodingFromHttpContentType(contentTypeNoEncode));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGetContentTypeFromPage() {
        final String methodName = "testGetContentTypeFromPage";
        final PageLoader pageLoader = new PageLoader(CUSTOM_ERROR_PAGE_URL, DEFAULT_ERROR_PAGE_CONTENT);
        try {
            mock.checking(new Expectations() {
                {
                    allowing(mockHTTPConnection).getInputStream();
                    will(returnValue(new ByteArrayInputStream("<head>/n<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">/n</head>/n".getBytes())));
                }
            });
            pageLoader.getContentTypeFromPage(mockURL);
            assertNull("contentType should be null", pageLoader.getContentTypeFromPage(mockURL));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGetContentEncoding_null() {
        final String methodName = "testGetContentEncoding";
        final PageLoader pageLoader = new PageLoader(CUSTOM_ERROR_PAGE_URL, DEFAULT_ERROR_PAGE_CONTENT);
        try {
            mock.checking(new Expectations() {
                {
                    allowing(mockHTTPConnection).getInputStream();
                    will(returnValue(new ByteArrayInputStream("<head>/n<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">/n</head>/n".getBytes())));
                    allowing(mockHTTPConnection).getContent();
                    will(returnValue(CUSTOM_ERROR_PAGE_CONTENT));
                    allowing(mockHTTPConnection).getContentType();
                    will(returnValue(DEFAULT_CONTENT_TYPE));
                    allowing(mockHTTPConnection).getContentEncoding();
                    will(returnValue(null));
                }
            });
            assertTrue("Load the custom error page should be successful", pageLoader.loadCustomErrorPage(mockURL, CUSTOM_ERROR_PAGE_URL));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }
}
