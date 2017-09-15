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
package com.ibm.ws.webcontainer.security.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import javax.servlet.http.HttpServletRequest;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Test;

/**
 *
 */
public class URLHandlerTest {
    private final Mockery mock = new JUnit4Mockery();
    private final HttpServletRequest req = mock.mock(HttpServletRequest.class);

    private static final String encodedURL = "percent%25semicolon%3Bcomma%2C";
    private static final String decodedURL = "percent%semicolon;comma,";
    private static final String url = "http://myHostName:9080/myServlet";
    private static final String urlNoHost = "http://:9080/myServlet";

    private final URLHandler handler = new URLHandler(null);

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.internal.URLHandler#encodeURL(java.lang.String)}.
     */
    @Test
    public void encodeURL() {
        assertEquals("Encoded url should have special chars replaced.", encodedURL, handler.encodeURL(decodedURL));
    }

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.internal.URLHandler#decodeURL(java.lang.String)}.
     */
    @Test
    public void decodeURL() {
        assertEquals("Encoded url should have special chars restored.", decodedURL, handler.decodeURL(encodedURL));
    }

    /**
     * encodeURL and decodeURL shall not modify the specified String
     * if there is nothing to encode or decode.
     */
    @Test
    public void encodeDecodeURL_nothingToEncode() {
        String raw = "simpleString";
        assertSame(raw, handler.encodeURL(raw));
        assertSame(raw, handler.decodeURL(raw));
    }

    /**
     * encodeURL shall modify the specified String so that all
     * instances of '%' are encoded to be "%25". decodeURL shall
     * do the reverse.
     */
    @Test
    public void encodeDecodeURL_percent() {
        String raw = "%";
        String encoded = "%25";
        assertEquals(encoded, handler.encodeURL(raw));
        assertEquals(raw, handler.decodeURL(encoded));
    }

    /**
     * encodeURL shall modify the specified String so that all
     * instances of ';' are encoded to be "%3B". decodeURL shall
     * do the reverse.
     */
    @Test
    public void encodeDecodeURL_semicolon() {
        String raw = ";";
        String encoded = "%3B";
        assertEquals(encoded, handler.encodeURL(raw));
        assertEquals(raw, handler.decodeURL(encoded));
    }

    /**
     * encodeURL shall modify the specified String so that all
     * instances of ',' are encoded to be "%2C". decodeURL shall
     * do the reserve.
     */
    @Test
    public void encodeDecodeURL_comma() {
        String raw = ",";
        String encoded = "%2C";
        assertEquals(encoded, handler.encodeURL(raw));
        assertEquals(raw, handler.decodeURL(encoded));
    }

    /**
     * encodeURL shall modify the specified String so that all
     * instances of '%', ';', and',' are properly encoded.
     * decodeURL shall do the reverse.
     */
    @Test
    public void encodeDecodeURL_complex() {
        String raw = "http://site.com/page;/subpage,/more%.html";
        String encoded = "http://site.com/page%3B/subpage%2C/more%25.html";
        assertEquals(encoded, handler.encodeURL(raw));
        assertEquals(raw, handler.decodeURL(encoded));
    }

    /**
     * removeHostNameFromURL shall return the URL without modification
     * if the URL is relative.
     */
    @Test
    public void removeHostNameFromURL_relativeURL() {
        String url = "/page";
        assertEquals(url, handler.removeHostNameFromURL(url));
    }

    /**
     * removeHostNameFromURL shall return the fully qualified
     * URL without the host.
     */
    @Test
    public void removeHostNameFromURL_fullyQualifiedURL() {
        String url = "https://site.com/page";
        String urlWithoutHost = "https:///page";
        assertEquals(urlWithoutHost, handler.removeHostNameFromURL(url));
    }

    /**
     * removeHostNameFromURL shall return the fully qualified
     * URL without the host. If specified, the port will remain.
     */
    @Test
    public void removeHostNameFromURL_withPort() {
        String url = "http://site.com:80/page";
        String urlWithoutHost = "http://:80/page";
        assertEquals(urlWithoutHost, handler.removeHostNameFromURL(url));
    }

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.internal.URLHandler#removeHostNameFromURL(java.lang.String)}.
     */
    @Test
    public void removeHostNameFromURL() {
        assertEquals("Url should have hostname removed.", urlNoHost, handler.removeHostNameFromURL(url));
    }

    /**
     * restoreHostName shall return the host specified in URLString if
     * the referrer URL is an empty String.
     */
    @Test
    public void restoreHostNameToURL_emptyReferrerURL() throws Exception {
        String host = "http://myhost.com";
        String referrerURL = "";
        String url = host + "/mypage";
        assertEquals(host, handler.restoreHostNameToURL(referrerURL, url));
    }

    /**
     * restoreHostNameToURL shall return the referrerURL if it contains
     * a host name.
     */
    @Test
    public void restoreHostNameToURL_referrerURLWithHost() throws Exception {
        String referrerURL = "http://otherhost.com/otherpage";
        String url = "http://myhost.com/mypage";
        assertEquals(referrerURL, handler.restoreHostNameToURL(referrerURL, url));
    }

    /**
     * restoreHostNameToURL shall return a String with the host of URLString prepended
     * to the storedReq if the URL specified by storedReq is relative
     * (meaning it starts with a '/' and is not an empty String).
     */
    @Test
    public void restoreHostNameToURL_relativeReferrerURL() throws Exception {
        String host = "http://myhost.com";
        String referrerURL = "/otherpage";
        String url = host + "/mypage";
        assertEquals(host + referrerURL, handler.restoreHostNameToURL(referrerURL, url));
    }

    /**
     * restoreHostNameToURL shall insert the host of the current URL into
     * the referrerURL if referrerURL does not contain a host name.
     */
    @Test
    public void restoreHostNameToURL_referrerURLWithoutHost() throws Exception {
        String referrerURL = "http:///otherpage";
        String url = "http://myhost.com/mypage";
        String expected = "http://myhost.com/otherpage";
        assertEquals(expected, handler.restoreHostNameToURL(referrerURL, url));
    }

    /**
     * restoreHostNameToURL shall insert the host of the current URL into
     * the referrerURL if referrerURL does not contain a host name. It must
     * preserve the port specified in referrerURL.
     */
    @Test
    public void restoreHostNameToURL_referrerURLWithoutHostWithPort() throws Exception {
        String referrerURL = "https://:9043/otherpage";
        String url = "http://myhost.com/mypage";
        String expected = "https://myhost.com:9043/otherpage";
        assertEquals(expected, handler.restoreHostNameToURL(referrerURL, url));
    }

    /**
     * restoreHostNameToURL shall return the specified referrer URL if
     * the referrer URL is malformed.
     */
    @Test
    public void restoreHostNameToURL_malformedReferrerURL() throws Exception {
        String referrerURL = "reallyMalformedURL";
        String url = "http://myhost.com/mypage";
        assertEquals(referrerURL, handler.restoreHostNameToURL(referrerURL, url));
    }

    /**
     * restoreHostNameToURL shall return the specified referrer URL if
     * the current URL is malformed.
     */
    @Test
    public void restoreHostNameToURL_malformedCurrentURL() throws Exception {
        String referrerURL = "http://myhost.com/mypage";
        String url = "reallyMalformedURL";
        assertEquals(referrerURL, handler.restoreHostNameToURL(referrerURL, url));
    }

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.internal.URLHandler#restoreHostNameToURL(java.lang.String, java.lang.String)}.
     */
    @Test
    public void restoreHostNameToURL() {
        assertEquals("Url should have hostname restored.", url, handler.restoreHostNameToURL(urlNoHost, url));
    }

    /**
     * getServletURI shall return the servlet path.
     */
    @Test
    public void getServletURI_simpleServlet() {
        final String uri = "/servlet"; // http://www.ibm.com/servlet
        mock.checking(new Expectations() {
            {
                allowing(req).getServletPath();
                will(returnValue(uri));
                allowing(req).getPathInfo();
                will(returnValue(null));
            }
        });
        assertEquals(uri, handler.getServletURI(req));
    }

    /**
     * getServletURI shall return the servlet path and the path info
     * when both are defined to be valid Strings.
     */
    @Test
    public void getServletURI_servletWithPathInfo() {
        final String uri = "/servlet"; // http://www.ibm.com/servlet
        final String pathInfo = "/pathInfo";
        final String expected = "/servlet/pathInfo";
        mock.checking(new Expectations() {
            {
                allowing(req).getServletPath();
                will(returnValue(uri));
                allowing(req).getPathInfo();
                will(returnValue(pathInfo));
            }
        });
        assertEquals(expected, handler.getServletURI(req));
    }

    /**
     * getServletURI shall return "/" when the servlet path is null.
     */
    @Test
    public void getServletURI_nullServlet() {
        final String uri = null;
        final String expected = "/";
        mock.checking(new Expectations() {
            {
                allowing(req).getServletPath();
                will(returnValue(uri));
                allowing(req).getPathInfo();
                will(returnValue(null));
            }
        });
        assertEquals(expected, handler.getServletURI(req));
    }

    /**
     * getServletURI shall return "/" when the servlet path is an
     * empty String.
     */
    @Test
    public void getServletURI_emptyStringServlet() {
        final String uri = "";
        final String expected = "/";
        mock.checking(new Expectations() {
            {
                allowing(req).getServletPath();
                will(returnValue(uri));
                allowing(req).getPathInfo();
                will(returnValue(null));
            }
        });
        assertEquals(expected, handler.getServletURI(req));
    }

    /**
     * getServletURI shall return "/" when the servlet path is "/".
     */
    @Test
    public void getServletURI_rootServlet() {
        final String uri = "/";
        final String expected = "/";
        mock.checking(new Expectations() {
            {
                allowing(req).getServletPath();
                will(returnValue(uri));
                allowing(req).getPathInfo();
                will(returnValue(null));
            }
        });
        assertEquals(expected, handler.getServletURI(req));
    }

    /**
     * getServletURI shall encode any colons in the servlet path.
     */
    @Test
    public void getServletURI_servletWithColon() {
        final String uri = "/servlet:one";
        final String expected = "/servlet%3Aone";
        mock.checking(new Expectations() {
            {
                allowing(req).getServletPath();
                will(returnValue(uri));
                allowing(req).getPathInfo();
                will(returnValue(null));
            }
        });
        assertEquals(expected, handler.getServletURI(req));
    }

    /**
     * getServletURI shall encode any colons in the servlet path
     * or path info.
     */
    @Test
    public void getServletURI_servletAndPathInfoWithColon() {
        final String uri = "/servlet:one";
        final String pathInfo = "/path:Info";
        final String expected = "/servlet%3Aone/path%3AInfo";
        mock.checking(new Expectations() {
            {
                allowing(req).getServletPath();
                will(returnValue(uri));
                allowing(req).getPathInfo();
                will(returnValue(pathInfo));
            }
        });
        assertEquals(expected, handler.getServletURI(req));
    }

    /**
     * getServletURI shall truncate the URI up to the first semicolon.
     */
    @Test
    public void getServletURI_servletAndPathInfoWithSemiColon() {
        final String uri = "/servlet:one";
        final String pathInfo = "/path;Info";
        final String expected = "/servlet%3Aone/path";
        mock.checking(new Expectations() {
            {
                allowing(req).getServletPath();
                will(returnValue(uri));
                allowing(req).getPathInfo();
                will(returnValue(pathInfo));
            }
        });
        assertEquals(expected, handler.getServletURI(req));
    }

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.internal.URLHandler#getServletURI(javax.servlet.http.HttpServletRequest)}.
     */
    @Test
    public void getServletURI() {
        final String servletPath = "servletPath";
        final String pathInfo = "pathInfo";
        mock.checking(new Expectations() {
            {
                allowing(req).getServletPath();
                will(returnValue(servletPath));
                allowing(req).getPathInfo();
                will(returnValue(pathInfo));
            }
        });
        assertEquals("Servlet URI should be " + servletPath + pathInfo, servletPath + pathInfo, handler.getServletURI(req));
    }

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.internal.URLHandler#getServletURI(javax.servlet.http.HttpServletRequest)}.
     */
    @Test
    public void getServletURI_nullPath() {
        mock.checking(new Expectations() {
            {
                allowing(req).getServletPath();
                will(returnValue(null));
                allowing(req).getPathInfo();
                will(returnValue(null));
            }
        });
        assertEquals("Servlet URI should be /", "/", handler.getServletURI(req));
    }

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.internal.URLHandler#getServletURI(javax.servlet.http.HttpServletRequest)}.
     */
    @Test
    public void getServletURI_semicolon() {
        final String servletPath = "servletPath";
        final String pathInfo = "pathInfo";
        mock.checking(new Expectations() {
            {
                allowing(req).getServletPath();
                will(returnValue(servletPath + ";"));
                allowing(req).getPathInfo();
                will(returnValue(pathInfo));
            }
        });
        assertEquals("Servlet URI should be " + servletPath, servletPath, handler.getServletURI(req));
    }

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.internal.URLHandler#getServletURI(javax.servlet.http.HttpServletRequest)}.
     */
    @Test
    public void getServletURI_colon() {
        final String servletPath = "servletPath";
        final String pathInfo = "pathInfo";
        mock.checking(new Expectations() {
            {
                allowing(req).getServletPath();
                will(returnValue(servletPath + ":"));
                allowing(req).getPathInfo();
                will(returnValue(pathInfo + ":"));
            }
        });
        String expectedURI = servletPath + "%3A" + pathInfo + "%3A";
        assertEquals("Servlet URI should be " + expectedURI, expectedURI, handler.getServletURI(req));
    }

}
