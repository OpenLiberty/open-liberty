/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.myfaces.application;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.security.Principal;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpSession;

/**
 * @author Werner Punz (latest modification by $Author: struberg $)
 * @version $Revision: 1188643 $ $Date: 2011-10-25 13:13:09 +0000 (Tue, 25 Oct 2011) $
 *
 * Dummy request for various system event listeners
 *
 * the problem with the system event listeners is that they
 * are triggered often outside of an existing request
 * hence we have to provide dummy objects
 */


public class _SystemEventServletRequest extends HttpServletRequestWrapper
{

    Map<String, Object> _attributesMap = new HashMap<String, Object>();

    public _SystemEventServletRequest()
    {
        super((HttpServletRequest) Proxy.newProxyInstance(
                HttpServletRequest.class.getClassLoader(),
                new Class[]{HttpServletRequest.class},
                new InvocationHandler()
                {
                    public Object invoke(Object proxy, Method m, Object[] args)
                    {
                        throw new UnsupportedOperationException("This request class is an empty placeholder");
                    }
                }));
    }

    public Object getAttribute(String s)
    {
        return _attributesMap.get(s);
    }

    public void setAttribute(String s, Object o)
    {
        _attributesMap.put(s, o);
    }

    public void removeAttribute(String s)
    {
        _attributesMap.remove(s);
    }

    public String getServletPath()
    {
        return null;
    }

    public String getPathInfo()
    {
        return null;
    }

    @Override
    public HttpSession getSession()
    {
        return null;
    }

    @Override
    public HttpSession getSession(boolean create)
    {
        return null;
    }

    @Override
    public int getContentLength()
    {
        return -1;
    }

    @Override
    public String getContentType()
    {
        return null;
    }

    @Override
    public String getCharacterEncoding()
    {
        return null;
    }

    @Override
    public String getHeader(String name)
    {
        return null;
    }

    @Override
    public Enumeration getHeaderNames()
    {
        return Collections.enumeration(Collections.emptyList());
    }

    @Override
    public Enumeration getHeaders(String name)
    {
        return Collections.enumeration(Collections.EMPTY_LIST);
    }

    @Override
    public Cookie[] getCookies()
    {
        return new Cookie[0];
    }

    @Override
    public String getAuthType()
    {
        return null;
    }

    @Override
    public String getContextPath()
    {
        return null;
    }

    @Override
    public long getDateHeader(String name)
    {
        return -1;
    }

    @Override
    public int getIntHeader(String name)
    {
        return -1;
    }

    @Override
    public String getMethod()
    {
        return null;
    }

    @Override
    public String getPathTranslated()
    {
        return null;
    }

    @Override
    public String getQueryString()
    {
        return null;
    }

    @Override
    public String getRemoteUser()
    {
        return null;
    }

    @Override
    public String getRequestedSessionId()
    {
        return null;
    }

    @Override
    public String getRequestURI()
    {
        return null;
    }

    @Override
    public StringBuffer getRequestURL()
    {
        return null;
    }

    @Override
    public Principal getUserPrincipal()
    {
        return null;
    }

    @Override
    public boolean isRequestedSessionIdFromCookie()
    {
        return false;
    }

    @Override
    public boolean isRequestedSessionIdFromUrl()
    {
        return false;
    }

    @Override
    public boolean isRequestedSessionIdFromURL()
    {
        return false;
    }

    @Override
    public boolean isRequestedSessionIdValid()
    {
        return false;
    }

    @Override
    public boolean isUserInRole(String role)
    {
        return false;
    }

    @Override
    public Enumeration getAttributeNames()
    {
        return Collections.enumeration(Collections.EMPTY_LIST);
    }

    @Override
    public ServletInputStream getInputStream() throws IOException
    {
        return null;
    }

    @Override
    public String getLocalAddr()
    {
        return null;
    }

    @Override
    public Locale getLocale()
    {
        return null;
    }

    @Override
    public Enumeration getLocales()
    {
        return Collections.enumeration(Collections.emptyList());
    }

    @Override
    public String getLocalName()
    {
        return null;
    }

    @Override
    public int getLocalPort()
    {
        return -1;
    }

    @Override
    public String getParameter(String name)
    {
        return null;
    }

    @Override
    public Map getParameterMap()
    {
        return Collections.emptyMap();
    }

    @Override
    public Enumeration getParameterNames()
    {
        return Collections.enumeration(Collections.emptyList());
    }

    @Override
    public String[] getParameterValues(String name)
    {
        return new String[0];
    }

    @Override
    public String getProtocol()
    {
        return null;
    }

    @Override
    public BufferedReader getReader() throws IOException
    {
        return null;
    }

    @Override
    public String getRealPath(String path)
    {
        return null;
    }

    @Override
    public String getRemoteAddr()
    {
        return null;
    }

    @Override
    public String getRemoteHost()
    {
        return null;
    }

    @Override
    public int getRemotePort()
    {
        return -1;
    }

    @Override
    public ServletRequest getRequest()
    {
        return null;
    }

    @Override
    public RequestDispatcher getRequestDispatcher(String path)
    {
        return null;
    }

    @Override
    public String getScheme()
    {
        return null;
    }

    @Override
    public String getServerName()
    {
        return null;
    }

    @Override
    public int getServerPort()
    {
        return -1;
    }

    @Override
    public boolean isSecure()
    {
        return false;
    }

    @Override
    public void setCharacterEncoding(String enc)
            throws UnsupportedEncodingException
    {
    }

    @Override
    public void setRequest(ServletRequest request)
    {
    }
}
