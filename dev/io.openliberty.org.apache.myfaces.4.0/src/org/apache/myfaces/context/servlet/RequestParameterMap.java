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

import jakarta.servlet.ServletRequest;

import org.apache.myfaces.util.lang.AbstractAttributeMap;

/**
 * ServletRequest parameters as Map.
 * 
 * @author Anton Koinov (latest modification by $Author$)
 * @version $Revision$ $Date$
 */
public final class RequestParameterMap extends AbstractAttributeMap<String>
{
    private final ServletRequest servletRequest;

    RequestParameterMap(final ServletRequest servletRequest)
    {
        this.servletRequest = servletRequest;
    }

    @Override
    protected String getAttribute(final String key)
    {
        return servletRequest.getParameter(key);
    }

    @Override
    protected void setAttribute(final String key, final String value)
    {
        throw new UnsupportedOperationException("Cannot set ServletRequest Parameter");
    }

    @Override
    protected void removeAttribute(final String key)
    {
        throw new UnsupportedOperationException("Cannot remove ServletRequest Parameter");
    }

    @Override
    protected Enumeration<String> getAttributeNames()
    {
        return servletRequest.getParameterNames();
    }
}