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
package jakarta.faces.context;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.Principal;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import jakarta.faces.FacesWrapper;
import jakarta.faces.lifecycle.ClientWindow;

/**
 * @since 2.0
 */
public abstract class ExternalContextWrapper extends ExternalContext implements FacesWrapper<ExternalContext>
{

    private ExternalContext delegate;
    
    @Deprecated
    public ExternalContextWrapper()
    {
    }

    public ExternalContextWrapper(ExternalContext delegate)
    {
        this.delegate = delegate;
    }

    @Override
    public void addResponseCookie(String name, String value, Map<String, Object> properties)
    {
        getWrapped().addResponseCookie(name, value, properties);
    }

    @Override
    public void addResponseHeader(String name, String value)
    {
        getWrapped().addResponseHeader(name, value);
    }

    @Override
    public void dispatch(String path) throws IOException
    {
        getWrapped().dispatch(path);
    }

    @Override
    public String encodeActionURL(String url)
    {
        return getWrapped().encodeActionURL(url);
    }

    @Override
    public String encodeBookmarkableURL(String baseUrl, Map<String,List<String>> parameters)
    {
        return getWrapped().encodeBookmarkableURL(baseUrl,parameters);
    }

    @Override
    public String encodeNamespace(String name)
    {
        return getWrapped().encodeNamespace(name);
    }

    @Override
    public String encodePartialActionURL(String url)
    {
        return getWrapped().encodePartialActionURL(url);
    }

    @Override
    public String encodeRedirectURL(String baseUrl,Map<String,List<String>> parameters)
    {
        return getWrapped().encodeRedirectURL(baseUrl,parameters);
    }

    @Override
    public String encodeResourceURL(String url)
    {
        return getWrapped().encodeResourceURL(url);
    }

    @Override
    public Map<String, Object> getApplicationMap()
    {
        return getWrapped().getApplicationMap();
    }

    @Override
    public String getAuthType()
    {
        return getWrapped().getAuthType();
    }

    @Override
    public Object getContext()
    {
        return getWrapped().getContext();
    }

    @Override
    public String getContextName()
    {
        return getWrapped().getContextName();
    }

    @Override
    public Flash getFlash()
    {
        return getWrapped().getFlash();
    }

    @Override
    public String getInitParameter(String name)
    {
        return getWrapped().getInitParameter(name);
    }

    @Override
    public Map getInitParameterMap()
    {
        return getWrapped().getInitParameterMap();
    }

    @Override
    public String getMimeType(String file)
    {
        return getWrapped().getMimeType(file);
    }

    @Override
    public String getRealPath(String path)
    {
        return getWrapped().getRealPath(path);
    }

    @Override
    public String getRemoteUser()
    {
        return getWrapped().getRemoteUser();
    }

    @Override
    public Object getRequest()
    {
        return getWrapped().getRequest();
    }

    @Override
    public String getRequestCharacterEncoding()
    {
        return getWrapped().getRequestCharacterEncoding();
    }

    @Override
    public int getRequestContentLength()
    {
        return getWrapped().getRequestContentLength();
    }

    @Override
    public String getRequestContentType()
    {
        return getWrapped().getRequestContentType();
    }

    @Override
    public String getRequestContextPath()
    {
        return getWrapped().getRequestContextPath();
    }

    @Override
    public Map<String, Object> getRequestCookieMap()
    {
        return getWrapped().getRequestCookieMap();
    }

    @Override
    public Map<String, String> getRequestHeaderMap()
    {
        return getWrapped().getRequestHeaderMap();
    }

    @Override
    public Map<String, String[]> getRequestHeaderValuesMap()
    {
        return getWrapped().getRequestHeaderValuesMap();
    }

    @Override
    public Locale getRequestLocale()
    {
        return getWrapped().getRequestLocale();
    }

    @Override
    public Iterator<Locale> getRequestLocales()
    {
        return getWrapped().getRequestLocales();
    }

    @Override
    public Map<String, Object> getRequestMap()
    {
        return getWrapped().getRequestMap();
    }

    @Override
    public Map<String, String> getRequestParameterMap()
    {
        return getWrapped().getRequestParameterMap();
    }

    @Override
    public Iterator<String> getRequestParameterNames()
    {
        return getWrapped().getRequestParameterNames();
    }

    @Override
    public Map<String, String[]> getRequestParameterValuesMap()
    {
        return getWrapped().getRequestParameterValuesMap();
    }

    @Override
    public String getRequestPathInfo()
    {
        return getWrapped().getRequestPathInfo();
    }

    @Override
    public String getRequestScheme()
    {
        return getWrapped().getRequestScheme();
    }

    @Override
    public String getRequestServerName()
    {
        return getWrapped().getRequestServerName();
    }

    @Override
    public int getRequestServerPort()
    {
        return getWrapped().getRequestServerPort();
    }

    @Override
    public String getRequestServletPath()
    {
        return getWrapped().getRequestServletPath();
    }

    @Override
    public URL getResource(String path) throws MalformedURLException
    {
        return getWrapped().getResource(path);
    }

    @Override
    public InputStream getResourceAsStream(String path)
    {
        return getWrapped().getResourceAsStream(path);
    }

    @Override
    public Set<String> getResourcePaths(String path)
    {
        return getWrapped().getResourcePaths(path);
    }

    @Override
    public Object getResponse()
    {
        return getWrapped().getResponse();
    }

    @Override
    public int getResponseBufferSize()
    {
        return getWrapped().getResponseBufferSize();
    }

    @Override
    public String getResponseCharacterEncoding()
    {
        return getWrapped().getResponseCharacterEncoding();
    }

    @Override
    public String getResponseContentType()
    {
        return getWrapped().getResponseContentType();
    }

    @Override
    public OutputStream getResponseOutputStream() throws IOException
    {
        return getWrapped().getResponseOutputStream();
    }

    @Override
    public Writer getResponseOutputWriter() throws IOException
    {
        return getWrapped().getResponseOutputWriter();
    }

    @Override
    public Object getSession(boolean create)
    {
        return getWrapped().getSession(create);
    }

    @Override
    public Map<String, Object> getSessionMap()
    {
        return getWrapped().getSessionMap();
    }

    @Override
    public Principal getUserPrincipal()
    {
        return getWrapped().getUserPrincipal();
    }

    @Override
    public ExternalContext getWrapped()
    {
        return delegate;
    }

    @Override
    public void invalidateSession()
    {
        getWrapped().invalidateSession();
    }

    @Override
    public boolean isResponseCommitted()
    {
        return getWrapped().isResponseCommitted();
    }

    @Override
    public boolean isUserInRole(String role)
    {
        return getWrapped().isUserInRole(role);
    }

    @Override
    public void log(String message, Throwable exception)
    {
        getWrapped().log(message, exception);
    }

    @Override
    public void log(String message)
    {
        getWrapped().log(message);
    }

    @Override
    public void redirect(String url) throws IOException
    {
        getWrapped().redirect(url);
    }

    @Override
    public void responseFlushBuffer() throws IOException
    {
        getWrapped().responseFlushBuffer();
    }

    @Override
    public void responseReset()
    {
        getWrapped().responseReset();
    }

    @Override
    public void responseSendError(int statusCode, String message) throws IOException
    {
        getWrapped().responseSendError(statusCode, message);
    }

    @Override
    public void setRequest(Object request)
    {
        getWrapped().setRequest(request);
    }

    @Override
    public void setRequestCharacterEncoding(String encoding) throws UnsupportedEncodingException
    {
        getWrapped().setRequestCharacterEncoding(encoding);
    }

    @Override
    public void setResponse(Object response)
    {
        getWrapped().setResponse(response);
    }

    @Override
    public void setResponseBufferSize(int size)
    {
        getWrapped().setResponseBufferSize(size);
    }

    @Override
    public void setResponseCharacterEncoding(String encoding)
    {
        getWrapped().setResponseCharacterEncoding(encoding);
    }

    @Override
    public void setResponseContentLength(int length)
    {
        getWrapped().setResponseContentLength(length);
    }

    @Override
    public void setResponseContentType(String contentType)
    {
        getWrapped().setResponseContentType(contentType);
    }

    @Override
    public void setResponseHeader(String name, String value)
    {
        getWrapped().setResponseHeader(name, value);
    }

    @Override
    public void setResponseStatus(int statusCode)
    {
        getWrapped().setResponseStatus(statusCode);
    }

    @Override
    public boolean isSecure()
    {
        return getWrapped().isSecure();
    }

    @Override
    public int getSessionMaxInactiveInterval()
    {
        return getWrapped().getSessionMaxInactiveInterval();
    }

    @Override
    public void setSessionMaxInactiveInterval(int interval)
    {
        getWrapped().setSessionMaxInactiveInterval(interval);
    }

    @Override
    public ClientWindow getClientWindow()
    {
        return getWrapped().getClientWindow();
    }

    @Override
    public void setClientWindow(ClientWindow window)
    {
        getWrapped().setClientWindow(window);
    }
    
    @Override
    public String getSessionId(boolean create)
    {
        return getWrapped().getSessionId(create);
    }
    
    @Override
    public String getApplicationContextPath()
    {
        return getWrapped().getApplicationContextPath();
    }

    @Override
    public String encodeWebsocketURL(String url)
    {
        return getWrapped().encodeWebsocketURL(url);
    }

    /**
     * @since 4.0
     */
    @Override
    public void release()
    {
        getWrapped().release();
    }
}
