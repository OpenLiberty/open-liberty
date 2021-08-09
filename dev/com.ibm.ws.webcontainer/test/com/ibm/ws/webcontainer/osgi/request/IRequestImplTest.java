/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.osgi.request;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.ibm.wsspi.http.HttpInboundConnection;
import com.ibm.wsspi.http.HttpRequest;
import com.ibm.wsspi.http.SSLContext;
import com.ibm.wsspi.webcontainer.WCCustomProperties;

import test.common.SharedOutputManager;

/**
 *
 */
//@RunWith(JMockit.class)
public class IRequestImplTest {
    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("*=info:webcontainer=all");

    private final Mockery mock = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };
    private final HttpInboundConnection conn = mock.mock(HttpInboundConnection.class);
    private final HttpRequest request = mock.mock(HttpRequest.class);
    private final SSLContext sslCtx = mock.mock(SSLContext.class);

    @Rule
    public TestRule rule = outputMgr;

    /*
     * Tests for getScheme() method
     * Outline of method:
     * if headers should be used from this host (by default accepted from all hosts)
     * ....use header named in httpsIndicatorHeader property to determine whether https
     * ....use value in $WSSC header
     * ....use $WSIS header to determine whether https
     * ....use value in X-Forwarded-Proto header
     * use scheme of inbound request
     */
    @Test
    public void testGetSchemeDefaultConfig() {

        mock.checking(new Expectations() {
            {
                // called by IRequestImpl ctor
                one(conn).getRequest();
                will(returnValue(request));

                // called by getScheme()
                one(conn).useTrustedHeaders();
                will(returnValue(true));
                one(conn).getTrustedHeader("$WSSC");
                will(returnValue(null));
                one(conn).getTrustedHeader("$WSIS");
                will(returnValue(null));
                one(conn).getTrustedHeader("X-Forwarded-Proto");
                will(returnValue(null));
                one(request).getScheme();
                will(returnValue("http"));
            }
        });
        // Set relevant config defaults
        Map<String, Object> config = new HashMap<String, Object>();
        config.put("httpsIndicatorHeader", "");
        WCCustomProperties.setCustomProperties(config);

        IRequestImpl iRequestImpl = new IRequestImpl(conn);
        String scheme = iRequestImpl.getScheme();

        assertEquals("http", scheme);
    }

    @Test
    public void testGetSchemeFromConfiguredHeader() {

        mock.checking(new Expectations() {
            {
                // called by IRequestImpl ctor
                one(conn).getRequest();
                will(returnValue(request));

                // called by getScheme()
                one(conn).useTrustedHeaders();
                will(returnValue(true));
                one(request).getHeader("Host");
                will(returnValue("localhost:9080"));
                one(request).getHeader("myPrivateHeader");
                will(returnValue("true"));
            }
        });
        // Set relevant config defaults
        Map<String, Object> config = new HashMap<String, Object>();
        config.put("httpsIndicatorHeader", "myPrivateHeader");
        WCCustomProperties.setCustomProperties(config);

        IRequestImpl iRequestImpl = new IRequestImpl(conn);
        String scheme = iRequestImpl.getScheme();

        assertEquals("https", scheme);
    }

    @Test
    public void testGetSchemeWSSC() {

        mock.checking(new Expectations() {
            {
                // called by IRequestImpl ctor
                one(conn).getRequest();
                will(returnValue(request));

                // called by getScheme()
                one(conn).useTrustedHeaders();
                will(returnValue(true));
                one(conn).getTrustedHeader("$WSSC");
                will(returnValue("wssc-scheme"));
                one(conn).getTrustedHeader("$WSIS");
                will(returnValue("true"));
                one(conn).getTrustedHeader("X-Forwarded-Proto");
                will(returnValue("XFP_scheme"));
                one(request).getScheme();
                will(returnValue("http"));
            }
        });
        // Set relevant config defaults
        Map<String, Object> config = new HashMap<String, Object>();
        config.put("httpsIndicatorHeader", "");
        WCCustomProperties.setCustomProperties(config);

        IRequestImpl iRequestImpl = new IRequestImpl(conn);
        String scheme = iRequestImpl.getScheme();

        assertEquals("wssc-scheme", scheme);
    }

    @Test
    public void testGetSchemeWSIS() {

        mock.checking(new Expectations() {
            {
                // called by IRequestImpl ctor
                one(conn).getRequest();
                will(returnValue(request));

                // called by getScheme()
                one(conn).useTrustedHeaders();
                will(returnValue(true));
                one(conn).getTrustedHeader("$WSSC");
                will(returnValue(null));
                one(conn).getTrustedHeader("$WSIS");
                will(returnValue("true"));
                one(conn).getTrustedHeader("X-Forwarded-Proto");
                will(returnValue("XFP_scheme"));
                one(request).getScheme();
                will(returnValue("http"));
            }
        });
        // Set relevant config defaults
        Map<String, Object> config = new HashMap<String, Object>();
        config.put("httpsIndicatorHeader", "");
        WCCustomProperties.setCustomProperties(config);

        IRequestImpl iRequestImpl = new IRequestImpl(conn);
        String scheme = iRequestImpl.getScheme();

        assertEquals("https", scheme);
    }

    @Test
    public void testGetSchemeXFP() {

        mock.checking(new Expectations() {
            {
                // called by IRequestImpl ctor
                one(conn).getRequest();
                will(returnValue(request));

                // called by getScheme()
                one(conn).useTrustedHeaders();
                will(returnValue(true));
                one(conn).getTrustedHeader("$WSSC");
                will(returnValue(null));
                one(conn).getTrustedHeader("$WSIS");
                will(returnValue(null));
                one(conn).getTrustedHeader("X-Forwarded-Proto");
                will(returnValue("XFP_scheme"));
                one(request).getScheme();
                will(returnValue("http"));
            }
        });
        // Set relevant config defaults
        Map<String, Object> config = new HashMap<String, Object>();
        config.put("httpsIndicatorHeader", "");
        WCCustomProperties.setCustomProperties(config);

        IRequestImpl iRequestImpl = new IRequestImpl(conn);
        String scheme = iRequestImpl.getScheme();

        assertEquals("XFP_scheme", scheme);
    }

    @Test
    public void testGetSchemeNoPrivateHeaders() {

        mock.checking(new Expectations() {
            {
                // called by IRequestImpl ctor
                one(conn).getRequest();
                will(returnValue(request));

                // called by getScheme()
                one(conn).useTrustedHeaders();
                will(returnValue(false)); // <------
                one(conn).getTrustedHeader("$WSSC");
                will(returnValue("WSSC_scheme"));
                one(conn).getTrustedHeader("$WSIS");
                will(returnValue("true"));
                one(conn).getTrustedHeader("X-Forwarded-Proto");
                will(returnValue("XFP_scheme"));
                one(request).getScheme();
                will(returnValue("http"));
            }
        });
        // Set relevant config defaults
        Map<String, Object> config = new HashMap<String, Object>();
        config.put("httpsIndicatorHeader", "");
        WCCustomProperties.setCustomProperties(config);

        IRequestImpl iRequestImpl = new IRequestImpl(conn);
        String scheme = iRequestImpl.getScheme();

        assertEquals("http", scheme);
    }

    /*
     * Tests for isSSL() method
     * Outline of method:
     * if headers should be used from this host (by default accepted from all hosts)
     * ....use header named in httpsIndicatorHeader property to determine whether SSL was used to proxy
     * ....use $WSIS header to determine whether SSL was used to proxy
     * ....use value in X-Forwarded-Proto headerto determine wheter SSL was used to proxy
     * check for SSL context on inbound request
     */

    @Test
    public void testIsSSLDefaultConfig() {

        mock.checking(new Expectations() {
            {
                // called by IRequestImpl ctor
                one(conn).getRequest();
                will(returnValue(request));

                // called by getScheme()
                one(conn).useTrustedHeaders();
                will(returnValue(true));
                one(conn).getTrustedHeader("$WSIS");
                will(returnValue(null));
                one(conn).getTrustedHeader("X-Forwarded-Proto");
                will(returnValue(null));
                one(conn).getSSLContext();
                will(returnValue(null));
            }
        });
        // Set relevant config defaults
        Map<String, Object> config = new HashMap<String, Object>();
        config.put("httpsIndicatorHeader", "");
        WCCustomProperties.setCustomProperties(config);

        IRequestImpl iRequestImpl = new IRequestImpl(conn);
        Boolean ssl = iRequestImpl.isSSL();

        assertFalse(ssl);
    }

    @Test
    public void testIsSSLFromConfiguredHeader() {

        mock.checking(new Expectations() {
            {
                // called by IRequestImpl ctor
                one(conn).getRequest();
                will(returnValue(request));

                // called by getScheme()
                one(conn).useTrustedHeaders();
                will(returnValue(true));
                one(request).getHeader("Host");
                will(returnValue("localhost:9080"));
                one(request).getHeader("myPrivateHeader");
                will(returnValue("true"));
            }
        });
        // Set relevant config defaults
        Map<String, Object> config = new HashMap<String, Object>();
        config.put("httpsIndicatorHeader", "myPrivateHeader");
        WCCustomProperties.setCustomProperties(config);

        IRequestImpl iRequestImpl = new IRequestImpl(conn);
        Boolean ssl = iRequestImpl.isSSL();

        assertTrue(ssl);
    }

    @Test
    public void testIsSSLWSIS() {

        mock.checking(new Expectations() {
            {
                // called by IRequestImpl ctor
                one(conn).getRequest();
                will(returnValue(request));

                // called by getScheme()
                one(conn).useTrustedHeaders();
                will(returnValue(true));
                one(conn).getTrustedHeader("$WSSC");
                will(returnValue(null));
                one(conn).getTrustedHeader("$WSIS");
                will(returnValue("true"));
                one(conn).getTrustedHeader("X-Forwarded-Proto");
                will(returnValue("XFP_scheme"));
                one(conn).getSSLContext();
                will(returnValue(null));
            }
        });
        // Set relevant config defaults
        Map<String, Object> config = new HashMap<String, Object>();
        config.put("httpsIndicatorHeader", "");
        WCCustomProperties.setCustomProperties(config);

        IRequestImpl iRequestImpl = new IRequestImpl(conn);
        Boolean ssl = iRequestImpl.isSSL();

        assertTrue(ssl);
    }

    @Test
    public void testIsSSLXFP() {

        mock.checking(new Expectations() {
            {
                // called by IRequestImpl ctor
                one(conn).getRequest();
                will(returnValue(request));

                // called by getScheme()
                one(conn).useTrustedHeaders();
                will(returnValue(true));
                one(conn).getTrustedHeader("$WSSC");
                will(returnValue(null));
                one(conn).getTrustedHeader("$WSIS");
                will(returnValue(null));
                one(conn).getTrustedHeader("X-Forwarded-Proto");
                will(returnValue("https"));
                one(conn).getSSLContext();
                will(returnValue(null));
            }
        });
        // Set relevant config defaults
        Map<String, Object> config = new HashMap<String, Object>();
        config.put("httpsIndicatorHeader", "");
        WCCustomProperties.setCustomProperties(config);

        IRequestImpl iRequestImpl = new IRequestImpl(conn);
        Boolean ssl = iRequestImpl.isSSL();

        assertTrue(ssl);
    }

    @Test
    public void testIsSSLNoPrivateHeaders() {

        mock.checking(new Expectations() {
            {
                // called by IRequestImpl ctor
                one(conn).getRequest();
                will(returnValue(request));

                // called by getScheme()
                one(conn).useTrustedHeaders();
                will(returnValue(false)); // <------
                one(conn).getTrustedHeader("$WSSC");
                will(returnValue("https"));
                one(conn).getTrustedHeader("$WSIS");
                will(returnValue("true"));
                one(conn).getTrustedHeader("X-Forwarded-Proto");
                will(returnValue("wss"));
                one(conn).getSSLContext();
                will(returnValue(null));
            }
        });
        // Set relevant config defaults
        Map<String, Object> config = new HashMap<String, Object>();
        config.put("httpsIndicatorHeader", "");
        WCCustomProperties.setCustomProperties(config);

        IRequestImpl iRequestImpl = new IRequestImpl(conn);
        Boolean ssl = iRequestImpl.isSSL();

        assertFalse(ssl);
    }

    @Test
    public void testIsSSLNoPrivateHeadersTrue() {

        mock.checking(new Expectations() {
            {
                // called by IRequestImpl ctor
                one(conn).getRequest();
                will(returnValue(request));

                // called by getScheme()
                one(conn).useTrustedHeaders();
                will(returnValue(false)); // <------
                one(conn).getTrustedHeader("$WSSC");
                will(returnValue("https"));
                one(conn).getTrustedHeader("$WSIS");
                will(returnValue("true"));
                one(conn).getTrustedHeader("X-Forwarded-Proto");
                will(returnValue("wss"));
                one(conn).getSSLContext();
                will(returnValue(sslCtx));
            }
        });
        // Set relevant config defaults
        Map<String, Object> config = new HashMap<String, Object>();
        config.put("httpsIndicatorHeader", "");
        WCCustomProperties.setCustomProperties(config);

        IRequestImpl iRequestImpl = new IRequestImpl(conn);
        Boolean ssl = iRequestImpl.isSSL();

        assertTrue(ssl);
    }
}
