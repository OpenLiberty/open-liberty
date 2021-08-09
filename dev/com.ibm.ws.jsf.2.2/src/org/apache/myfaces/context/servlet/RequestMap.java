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

import javax.servlet.ServletRequest;

import org.apache.myfaces.util.AbstractAttributeMap;


/**
 * ServletRequest attributes Map.
 * 
 * @author Anton Koinov (latest modification by $Author: bommel $)
 * @version $Revision: 1187701 $ $Date: 2011-10-22 12:21:54 +0000 (Sat, 22 Oct 2011) $
 */
public final class RequestMap extends AbstractAttributeMap<Object>
{
    final ServletRequest _servletRequest;

    RequestMap(final ServletRequest servletRequest)
    {
        _servletRequest = servletRequest;
    }

    @Override
    protected Object getAttribute(final String key)
    {
        return _servletRequest.getAttribute(key);
    }

    @Override
    protected void setAttribute(final String key, final Object value)
    {
        _servletRequest.setAttribute(key, value);
    }

    @Override
    protected void removeAttribute(final String key)
    {
        _servletRequest.removeAttribute(key);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Enumeration<String> getAttributeNames()
    {
        return _servletRequest.getAttributeNames();
    }

    @Override
    public void putAll(final Map<? extends String, ? extends Object> t)
    {
        throw new UnsupportedOperationException();
    }


    @Override
    public void clear()
    {
        throw new UnsupportedOperationException();
    }    
}
