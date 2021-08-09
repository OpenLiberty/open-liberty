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
package org.apache.myfaces.shared.renderkit.html.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

import javax.faces.FacesWrapper;
import javax.faces.component.StateHolder;
import javax.faces.context.FacesContext;
import javax.servlet.http.Part;

public class HttpPartWrapper implements Part, FacesWrapper<Part>, StateHolder
{
    private Part delegate;

    public HttpPartWrapper()
    {}

    public HttpPartWrapper(Part delegate)
    {
        this.delegate = delegate;
    }

    @Override
    public void delete() throws IOException
    {
        getWrapped().delete();
    }

    @Override
    public String getContentType()
    {
        return getWrapped().getContentType();
    }

    @Override
    public String getHeader(String headerName)
    {
        return getWrapped().getHeader(headerName);
    }

    @Override
    public Collection<String> getHeaderNames()
    {
        return getWrapped().getHeaderNames();
    }

    @Override
    public Collection<String> getHeaders(String headerName)
    {
        return getWrapped().getHeaders(headerName);
    }

    @Override
    public InputStream getInputStream() throws IOException
    {
        return getWrapped().getInputStream();
    }

    @Override
    public String getName()
    {
        return getWrapped().getName();
    }

    @Override
    public long getSize()
    {
        return getWrapped().getSize();
    }

    @Override
    public void write(String fileName) throws IOException
    {
        getWrapped().write(fileName);
    }

    @Override
    public Object saveState(FacesContext context)
    {
        return null;
    }

    @Override
    public void restoreState(FacesContext context, Object state)
    {}

    @Override
    public boolean isTransient()
    {
        return true;
    }

    @Override
    public void setTransient(boolean newTransientValue)
    {}

    @Override
    public Part getWrapped()
    {
        return delegate;
    }

    /** {@inheritDoc} */
    public String getSubmittedFileName() {
        //As part of JSF 2.2 development we had to start replying on EE7 technologies
        //(even though JSF 2.2 does not rely on EE7).
        //As such we had to "implement" the inherited methods from EE7 Servlet 3.1
        //These methods shouldn't ever be called or accessed from a customer's stand point
        return getWrapped().getSubmittedFileName();
    }

}
