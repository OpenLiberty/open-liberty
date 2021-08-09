/*******************************************************************************
 * Copyright (c) 2009, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.httpsvc.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.EventListener;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.Filter;
import javax.servlet.FilterRegistration;
import javax.servlet.FilterRegistration.Dynamic;
import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import javax.servlet.SessionCookieConfig;
import javax.servlet.SessionTrackingMode;
import javax.servlet.descriptor.JspConfigDescriptor;
import javax.servlet.http.HttpSession;

import org.junit.Test;

import com.ibm.ws.httpsvc.internal.HttpServiceContainer;
import com.ibm.ws.httpsvc.servlet.internal.RequestMessage;
import com.ibm.ws.httpsvc.session.internal.SessionManager;
import com.ibm.wsspi.http.EncodingUtils;
import com.ibm.wsspi.http.HttpCookie;
import com.ibm.wsspi.http.HttpDateFormat;
import com.ibm.wsspi.http.HttpInboundConnection;
import com.ibm.wsspi.http.HttpInputStream;
import com.ibm.wsspi.http.HttpRequest;
import com.ibm.wsspi.http.HttpResponse;
import com.ibm.wsspi.http.SSLContext;

/**
 * Test session related apis.
 */
public class SessionTest {

    @Test
    public void test() {
        SessionManager mgr = new SessionManager();
        Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put("url.rewriting.enabled", "true");
        mgr.processConfig(props);

        new MockContainer(mgr);

        MockRequest mReq = new MockRequest();
        mReq.uri = "/alias";
        HttpInboundConnection conn = new MockConnection(mReq);
        RequestMessage req = new MockSvcRequest(conn, mgr);
        ServletContext fakeContext = new MockServletContext();
        req.setServletContext(fakeContext);
        assertNull(req.getSession(false));
        HttpSession session = req.getSession(true);
        assertNotNull(session);
        String id = session.getId();
        HttpSession session2 = req.getSession();
        assertNotNull(session2);
        assertEquals(session, session2);

        // make sure a second request with the same id finds the existing session
        req.clear();
        mReq.uri = "/alias;jsessionid=0000" + id;
        req.init(conn, mgr);
        req.setServletContext(fakeContext);
        session2 = req.getSession(false);
        assertNotNull(session2);
        assertEquals(session, session2);

        // invalid the session and make sure another shows that
        session.invalidate();
        req.clear();
        mReq.uri = "/alias;jsessionid=0000" + id;
        req.init(conn, mgr);
        req.setServletContext(fakeContext);
        session2 = req.getSession(false);
        assertNull(session2);
    }

    private class MockContainer extends HttpServiceContainer {
        public MockContainer(SessionManager mgr) {
            super();
        }
    }

    private class MockSvcRequest extends RequestMessage {
        public MockSvcRequest(HttpInboundConnection conn, SessionManager mgr) {
            super(conn, mgr);
        }

        @Override
        public String getRawRequestURI() {
            return ((MockRequest) super.request).uri;
        }
    }

    private class MockRequest implements HttpRequest {
        public String uri = null;

        public MockRequest() {
            // nothing
        }

        @Override
        public HttpInputStream getBody() {
            return null;
        }

        @Override
        public long getContentLength() {
            return 0;
        }

        @Override
        public HttpCookie getCookie(String name) {
            return null;
        }

        @Override
        public List<HttpCookie> getCookies() {
            return null;
        }

        @Override
        public List<HttpCookie> getCookies(String name) {
            return null;
        }

        @Override
        public String getHeader(String name) {
            return null;
        }

        @Override
        public List<String> getHeaderNames() {
            return null;
        }

        @Override
        public List<String> getHeaders(String name) {
            return new LinkedList<String>();
        }

        @Override
        public String getMethod() {
            return null;
        }

        @Override
        public String getQuery() {
            return null;
        }

        @Override
        public String getScheme() {
            return null;
        }

        @Override
        public String getURI() {
            return uri;
        }

        @Override
        public String getURL() {
            return "http://localhost" + uri;
        }

        @Override
        public String getVersion() {
            return null;
        }

        @Override
        public String getVirtualHost() {
            return null;
        }

        @Override
        public int getVirtualPort() {
            return 0;
        }

        /*
         * (non-Javadoc)
         *
         * @see com.ibm.wsspi.http.HttpRequest#getTrailerNames()
         */
        @Override
        public List<String> getTrailerNames() {
            // TODO Auto-generated method stub
            return null;
        }

        /*
         * (non-Javadoc)
         *
         * @see com.ibm.wsspi.http.HttpRequest#getTrailer(java.lang.String)
         */
        @Override
        public String getTrailer(String name) {
            // TODO Auto-generated method stub
            return null;
        }

        /*
         * (non-Javadoc)
         *
         * @see com.ibm.wsspi.http.HttpRequest#isTrailersReady()
         */
        @Override
        public boolean isTrailersReady() {
            // TODO Auto-generated method stub
            return false;
        }
    }

    private class MockConnection implements HttpInboundConnection {
        private MockRequest req = null;

        public MockConnection(MockRequest _req) {
            req = _req;
        }

        @Override
        public void finish(Exception e) {
            // nothing
        }

        @Override
        public String getLocalHostAddress() {
            return null;
        }

        @Override
        public String getLocalHostName(boolean canonical) {
            return null;
        }

        @Override
        public int getLocalPort() {
            return 0;
        }

        @Override
        public String getRemoteHostAddress() {
            return null;
        }

        @Override
        public String getRemoteHostName(boolean canonical) {
            return null;
        }

        @Override
        public int getRemotePort() {
            return 0;
        }

        @Override
        public HttpRequest getRequest() {
            return this.req;
        }

        @Override
        public HttpResponse getResponse() {
            return null;
        }

        @Override
        public SSLContext getSSLContext() {
            return null;
        }

        @Override
        public HttpDateFormat getDateFormatter() {
            return null;
        }

        @Override
        public EncodingUtils getEncodingUtils() {
            return null;
        }

        /** {@inheritDoc} */
        @Override
        public String getLocalHostAlias() {
            return null;
        }

        @Override
        public boolean useTrustedHeaders() {
            return false;
        }

        @Override
        public String getTrustedHeader(String headerKey) {
            return null;
        }

        @Override
        public String getRequestedHost() {
            return null;
        }

        @Override
        public int getRequestedPort() {
            return 0;
        }
    }

    private class MockServletContext implements ServletContext {
        /** {@inheritDoc} */
        @Override
        public Dynamic addFilter(String arg0, String arg1) throws IllegalArgumentException, IllegalStateException {
            // TODO Auto-generated method stub
            return null;
        }

        /** {@inheritDoc} */
        @Override
        public Dynamic addFilter(String arg0, Filter arg1) throws IllegalArgumentException, IllegalStateException {
            // TODO Auto-generated method stub
            return null;
        }

        /** {@inheritDoc} */
        @Override
        public Dynamic addFilter(String arg0, Class<? extends Filter> arg1) throws IllegalArgumentException, IllegalStateException {
            // TODO Auto-generated method stub
            return null;
        }

        /** {@inheritDoc} */
        @Override
        public void addListener(Class<? extends EventListener> arg0) {
            // TODO Auto-generated method stub

        }

        /** {@inheritDoc} */
        @Override
        public void addListener(String arg0) {
            // TODO Auto-generated method stub

        }

        /** {@inheritDoc} */
        @Override
        public <T extends EventListener> void addListener(T arg0) {
            // TODO Auto-generated method stub

        }

        /** {@inheritDoc} */
        @Override
        public javax.servlet.ServletRegistration.Dynamic addServlet(String arg0, String arg1) throws IllegalArgumentException, IllegalStateException {
            // TODO Auto-generated method stub
            return null;
        }

        /** {@inheritDoc} */
        @Override
        public javax.servlet.ServletRegistration.Dynamic addServlet(String arg0, Servlet arg1) throws IllegalArgumentException, IllegalStateException {
            // TODO Auto-generated method stub
            return null;
        }

        /** {@inheritDoc} */
        @Override
        public javax.servlet.ServletRegistration.Dynamic addServlet(String arg0, Class<? extends Servlet> arg1) throws IllegalArgumentException, IllegalStateException {
            // TODO Auto-generated method stub
            return null;
        }

        /** {@inheritDoc} */
        @Override
        public <T extends Filter> T createFilter(Class<T> arg0) throws ServletException {
            // TODO Auto-generated method stub
            return null;
        }

        /** {@inheritDoc} */
        @Override
        public <T extends EventListener> T createListener(Class<T> arg0) throws ServletException {
            // TODO Auto-generated method stub
            return null;
        }

        /** {@inheritDoc} */
        @Override
        public <T extends Servlet> T createServlet(Class<T> arg0) throws ServletException {
            // TODO Auto-generated method stub
            return null;
        }

        /** {@inheritDoc} */
        @Override
        public void declareRoles(String... arg0) {
            // TODO Auto-generated method stub

        }

        /** {@inheritDoc} */
        @Override
        public Object getAttribute(String arg0) {
            // TODO Auto-generated method stub
            return null;
        }

        /** {@inheritDoc} */
        @Override
        public Enumeration<String> getAttributeNames() {
            // TODO Auto-generated method stub
            return null;
        }

        /** {@inheritDoc} */
        @Override
        public ClassLoader getClassLoader() {
            // TODO Auto-generated method stub
            return null;
        }

        /** {@inheritDoc} */
        @Override
        public ServletContext getContext(String arg0) {
            // TODO Auto-generated method stub
            return null;
        }

        /** {@inheritDoc} */
        @Override
        public String getContextPath() {
            // TODO Auto-generated method stub
            return null;
        }

        /** {@inheritDoc} */
        @Override
        public Set<SessionTrackingMode> getDefaultSessionTrackingModes() {
            // TODO Auto-generated method stub
            return null;
        }

        /** {@inheritDoc} */
        @Override
        public int getEffectiveMajorVersion() throws UnsupportedOperationException {
            // TODO Auto-generated method stub
            return 0;
        }

        /** {@inheritDoc} */
        @Override
        public int getEffectiveMinorVersion() throws UnsupportedOperationException {
            // TODO Auto-generated method stub
            return 0;
        }

        /** {@inheritDoc} */
        @Override
        public Set<SessionTrackingMode> getEffectiveSessionTrackingModes() {
            // TODO Auto-generated method stub
            return null;
        }

        /** {@inheritDoc} */
        @Override
        public FilterRegistration getFilterRegistration(String arg0) {
            // TODO Auto-generated method stub
            return null;
        }

        /** {@inheritDoc} */
        @Override
        public Map<String, ? extends FilterRegistration> getFilterRegistrations() {
            // TODO Auto-generated method stub
            return null;
        }

        /** {@inheritDoc} */
        @Override
        public String getInitParameter(String arg0) {
            // TODO Auto-generated method stub
            return null;
        }

        /** {@inheritDoc} */
        @Override
        public Enumeration<String> getInitParameterNames() {
            // TODO Auto-generated method stub
            return null;
        }

        /** {@inheritDoc} */
        @Override
        public JspConfigDescriptor getJspConfigDescriptor() {
            // TODO Auto-generated method stub
            return null;
        }

        /** {@inheritDoc} */
        @Override
        public int getMajorVersion() {
            // TODO Auto-generated method stub
            return 0;
        }

        /** {@inheritDoc} */
        @Override
        public String getMimeType(String arg0) {
            // TODO Auto-generated method stub
            return null;
        }

        /** {@inheritDoc} */
        @Override
        public int getMinorVersion() {
            // TODO Auto-generated method stub
            return 0;
        }

        /** {@inheritDoc} */
        @Override
        public RequestDispatcher getNamedDispatcher(String arg0) {
            // TODO Auto-generated method stub
            return null;
        }

        /** {@inheritDoc} */
        @Override
        public String getRealPath(String arg0) {
            // TODO Auto-generated method stub
            return null;
        }

        /** {@inheritDoc} */
        @Override
        public RequestDispatcher getRequestDispatcher(String arg0) {
            // TODO Auto-generated method stub
            return null;
        }

        /** {@inheritDoc} */
        @Override
        public URL getResource(String arg0) throws MalformedURLException {
            // TODO Auto-generated method stub
            return null;
        }

        /** {@inheritDoc} */
        @Override
        public InputStream getResourceAsStream(String arg0) {
            // TODO Auto-generated method stub
            return null;
        }

        /** {@inheritDoc} */
        @Override
        public Set<String> getResourcePaths(String arg0) {
            // TODO Auto-generated method stub
            return null;
        }

        /** {@inheritDoc} */
        @Override
        public String getServerInfo() {
            // TODO Auto-generated method stub
            return null;
        }

        /** {@inheritDoc} */
        @Override
        public Servlet getServlet(String arg0) throws ServletException {
            // TODO Auto-generated method stub
            return null;
        }

        /** {@inheritDoc} */
        @Override
        public String getServletContextName() {
            // TODO Auto-generated method stub
            return null;
        }

        /** {@inheritDoc} */
        @Override
        public Enumeration<String> getServletNames() {
            // TODO Auto-generated method stub
            return null;
        }

        /** {@inheritDoc} */
        @Override
        public ServletRegistration getServletRegistration(String arg0) {
            // TODO Auto-generated method stub
            return null;
        }

        /** {@inheritDoc} */
        @Override
        public Map<String, ? extends ServletRegistration> getServletRegistrations() {
            // TODO Auto-generated method stub
            return null;
        }

        /** {@inheritDoc} */
        @Override
        public Enumeration<Servlet> getServlets() {
            // TODO Auto-generated method stub
            return null;
        }

        /** {@inheritDoc} */
        @Override
        public SessionCookieConfig getSessionCookieConfig() {
            // TODO Auto-generated method stub
            return null;
        }

        /** {@inheritDoc} */
        @Override
        public void log(String arg0) {
            // TODO Auto-generated method stub

        }

        /** {@inheritDoc} */
        @Override
        public void log(Exception arg0, String arg1) {
            // TODO Auto-generated method stub

        }

        /** {@inheritDoc} */
        @Override
        public void log(String arg0, Throwable arg1) {
            // TODO Auto-generated method stub

        }

        /** {@inheritDoc} */
        @Override
        public void removeAttribute(String arg0) {
            // TODO Auto-generated method stub

        }

        /** {@inheritDoc} */
        @Override
        public void setAttribute(String arg0, Object arg1) {
            // TODO Auto-generated method stub

        }

        /** {@inheritDoc} */
        @Override
        public boolean setInitParameter(String arg0, String arg1) {
            // TODO Auto-generated method stub
            return false;
        }

        /** {@inheritDoc} */
        @Override
        public void setSessionTrackingModes(Set<SessionTrackingMode> arg0) {
            // TODO Auto-generated method stub

        }

        /*
         * (non-Javadoc)
         *
         * @see javax.servlet.ServletContext#getVirtualServerName()
         */
        @Override
        public String getVirtualServerName() {
            // TODO Auto-generated method stub
            return null;
        }

    }
}
