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
package org.apache.myfaces.context.servlet;

import java.util.Enumeration;
import java.util.Map;
import java.util.NoSuchElementException;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.apache.myfaces.util.AbstractAttributeMap;

/**
 * HttpServletRequest Cookies as Map.
 *
 * @author Dimitry D'hondt
 * @author Anton Koinov
 * @version $Revision: 1494189 $ $Date: 2013-06-18 16:37:46 +0000 (Tue, 18 Jun 2013) $
 */
public final class CookieMap extends AbstractAttributeMap<Object>
{
    private static final Cookie[] EMPTY_ARRAY = new Cookie[0];

    private final HttpServletRequest _httpServletRequest;

    CookieMap(final HttpServletRequest httpServletRequest)
    {
        _httpServletRequest = httpServletRequest;
    }

    @Override
    public void clear()
    {
        throw new UnsupportedOperationException(
            "Cannot clear HttpRequest Cookies");
    }

    @Override
    public boolean containsValue(final Object findValue)
    {
        if (findValue == null)
        {
            return false;
        }

        final Cookie[] cookies = _httpServletRequest.getCookies();
        if (cookies == null)
        {
            return false;
        }
        for (int i = 0, len = cookies.length; i < len; i++)
        {
            if (findValue.equals(cookies[i]))
            {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean isEmpty()
    {
        final Cookie[] cookies = _httpServletRequest.getCookies();
        return cookies == null || cookies.length == 0;
    }

    @Override
    public int size()
    {
        final Cookie[] cookies = _httpServletRequest.getCookies();
        return cookies == null ? 0 : cookies.length;
    }

    @Override
    public void putAll(final Map<? extends String, ? extends Object> t)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    protected Object getAttribute(final String key)
    {
        final Cookie[] cookies = _httpServletRequest.getCookies();
        if (cookies == null)
        {
            return null;
        }
        for (int i = 0, len = cookies.length; i < len; i++)
        {
            if (cookies[i] != null && cookies[i].getName().equals(key))
            {
                return cookies[i];
            }
        }

        return null;
    }

    @Override
    protected void setAttribute(final String key, final Object value)
    {
        throw new UnsupportedOperationException(
            "Cannot set HttpRequest Cookies");
    }

    @Override
    protected void removeAttribute(final String key)
    {
        throw new UnsupportedOperationException(
            "Cannot remove HttpRequest Cookies");
    }

    @Override
    protected Enumeration<String> getAttributeNames()
    {
        final Cookie[] cookies = _httpServletRequest.getCookies();

        return cookies == null ? new CookieNameEnumeration(EMPTY_ARRAY) : new CookieNameEnumeration(cookies);
  
    }

    private static class CookieNameEnumeration implements Enumeration<String>
    {
        private final Cookie[] _cookies;
        private final int _length;
        private int _index;

        public CookieNameEnumeration(final Cookie[] cookies)
        {
            _cookies = cookies;
            _length = cookies.length;
        }

        public boolean hasMoreElements()
        {
            return _index < _length;
        }

        public String nextElement()
        {
            if (!hasMoreElements())
            {
                throw new NoSuchElementException();
            }
            return _cookies[_index++].getName();
        }
    }
}
