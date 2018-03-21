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
package javax.faces.context;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.faces.lifecycle.ClientWindow;

/**
 * see Javadoc of <a href="http://java.sun.com/javaee/javaserverfaces/1.2/docs/api/index.html">JSF Specification</a>
 */
public abstract class ExternalContext
{
    public static final String BASIC_AUTH = "BASIC";
    public static final String CLIENT_CERT_AUTH = "CLIENT_CERT";
    public static final String DIGEST_AUTH = "DIGEST";
    public static final String FORM_AUTH = "FORM";
    
    /**
     *
     * @param name
     * @param value
     * @param properties
     *
     * @since 2.0
     */
    public void addResponseCookie(String name, String value, Map<String, Object> properties)
    {
        ExternalContext ctx = _MyFacesExternalContextHelper.firstInstance.get();
        
        if (ctx == null)
        {
            throw new UnsupportedOperationException();
        }
        
        ctx.addResponseCookie(name, value, properties);
    }

    /**
     *
     * @param name
     * @param value
     *
     * @since 2.0
     */
    public void addResponseHeader(String name, String value)
    {
        ExternalContext ctx = _MyFacesExternalContextHelper.firstInstance.get();
        
        if (ctx == null)
        {
            throw new UnsupportedOperationException();
        }
        
        ctx.addResponseHeader(name, value);
    }

    public abstract void dispatch(String path) throws IOException;

    public abstract String encodeActionURL(String url);

    /**
     *
     * @param baseUrl
     * @param parameters
     *
     * @since 2.0
     */
    public String encodeBookmarkableURL(String baseUrl, Map<String,List<String>> parameters)
    {
        ExternalContext ctx = _MyFacesExternalContextHelper.firstInstance.get();
        
        if (ctx == null)
        {
            throw new UnsupportedOperationException();
        }
        
        return ctx.encodeBookmarkableURL(baseUrl, parameters);
    }

    public abstract String encodeNamespace(String name);


    /**
     * @since 2.0
     */
    public String encodePartialActionURL(String url)
    {
        // TODO: IMPLEMENT IMPL
        ExternalContext ctx = _MyFacesExternalContextHelper.firstInstance.get();
        
        if (ctx == null)
        {
            throw new UnsupportedOperationException();
        }
        
        return ctx.encodePartialActionURL(url);
    }

    /**
     *
     * @param baseUrl
     * @param parameters
     *
     * @since 2.0
     */
    public String encodeRedirectURL(String baseUrl, Map<String,List<String>> parameters)
    {
        ExternalContext ctx = _MyFacesExternalContextHelper.firstInstance.get();
        
        if (ctx == null)
        {
            throw new UnsupportedOperationException();
        }
        
        return ctx.encodeRedirectURL(baseUrl, parameters);
    }

    public abstract String encodeResourceURL(String url);

    public abstract Map<String, Object> getApplicationMap();

    public abstract String getAuthType();

    public abstract Object getContext();

    /**
     * Returns the name of the underlying context
     *
     * @return the name or null
     *
     * @since 2.0
     */
    public String getContextName()
    {
        ExternalContext ctx = _MyFacesExternalContextHelper.firstInstance.get();
        
        if (ctx == null)
        {
            throw new UnsupportedOperationException();
        }
        
        return ctx.getContextName();
    }

    /**
     * @since 2.0
     */
    public Flash getFlash()
    {
        ExternalContext ctx = _MyFacesExternalContextHelper.firstInstance.get();
        
        if (ctx == null)
        {
            throw new UnsupportedOperationException();
        }
        
        return ctx.getFlash();
    }

    public abstract String getInitParameter(String name);

    public abstract Map<String,String> getInitParameterMap();

    /**
     * @since JSF 2.0
     */
    public String getMimeType(String file)
    {
        ExternalContext ctx = _MyFacesExternalContextHelper.firstInstance.get();
        
        if (ctx == null)
        {
            throw new UnsupportedOperationException();
        }
        
        return ctx.getMimeType(file);
    }

    /**
     * @since JSF 2.0
     */
    public String getRealPath(String path)
    {
        ExternalContext ctx = _MyFacesExternalContextHelper.firstInstance.get();
        
        if (ctx == null)
        {
            throw new UnsupportedOperationException();
        }
        
        return ctx.getRealPath(path);
    }

    public abstract String getRemoteUser();

    public abstract Object getRequest();

    public String getRequestCharacterEncoding()
    {
        ExternalContext ctx = _MyFacesExternalContextHelper.firstInstance.get();
        
        if (ctx == null)
        {
            throw new UnsupportedOperationException();
        }
        
        return ctx.getRequestCharacterEncoding();
    }

    /**
     *
     * @return
     *
     * @since 2.0
     */
    public int getRequestContentLength()
    {
        ExternalContext ctx = _MyFacesExternalContextHelper.firstInstance.get();
        
        if (ctx == null)
        {
            throw new UnsupportedOperationException();
        }
        
        return ctx.getRequestContentLength();
    }

    public String getRequestContentType()
    {
        ExternalContext ctx = _MyFacesExternalContextHelper.firstInstance.get();
        
        if (ctx == null)
        {
            throw new UnsupportedOperationException();
        }
        
        return ctx.getRequestContentType();
    }

    public abstract String getRequestContextPath();

    public abstract Map<String, Object> getRequestCookieMap();

    public abstract Map<String, String> getRequestHeaderMap();

    public abstract Map<String, String[]> getRequestHeaderValuesMap();

    public abstract Locale getRequestLocale();

    public abstract Iterator<Locale> getRequestLocales();

    public abstract Map<String, Object> getRequestMap();

    public abstract Map<String, String> getRequestParameterMap();

    public abstract Iterator<String> getRequestParameterNames();

    public abstract Map<String, String[]> getRequestParameterValuesMap();

    public abstract String getRequestPathInfo();

    /**
     * @since JSF 2.0
     */
    public String getRequestScheme()
    {
        ExternalContext ctx = _MyFacesExternalContextHelper.firstInstance.get();
        
        if (ctx == null)
        {
            throw new UnsupportedOperationException();
        }
        
        return ctx.getRequestScheme();
    }

    /**
     * @since JSF 2.0
     */
    public String getRequestServerName()
    {
        ExternalContext ctx = _MyFacesExternalContextHelper.firstInstance.get();
        
        if (ctx == null)
        {
            throw new UnsupportedOperationException();
        }
        
        return ctx.getRequestServerName();
    }

    /**
     * @since JSF 2.0
     */
    public int getRequestServerPort()
    {
        ExternalContext ctx = _MyFacesExternalContextHelper.firstInstance.get();
        
        if (ctx == null)
        {
            throw new UnsupportedOperationException();
        }
        
        return ctx.getRequestServerPort();
    }

    public abstract String getRequestServletPath();

    public abstract java.net.URL getResource(String path) throws java.net.MalformedURLException;

    public abstract java.io.InputStream getResourceAsStream(String path);

    public abstract Set<String> getResourcePaths(String path);

    public abstract Object getResponse();

    /**
     *
     * @return
     *
     * @since 2.0
     */
    public int getResponseBufferSize()
    {
        ExternalContext ctx = _MyFacesExternalContextHelper.firstInstance.get();
        
        if (ctx == null)
        {
            throw new UnsupportedOperationException();
        }
        
        return ctx.getResponseBufferSize();
    }

    public String getResponseCharacterEncoding()
    {
        ExternalContext ctx = _MyFacesExternalContextHelper.firstInstance.get();
        
        if (ctx == null)
        {
            throw new UnsupportedOperationException("JSF 1.2 : figure out how to tell if this is a Portlet request");
        }
        
        return ctx.getResponseCharacterEncoding();
    }

    /**
     * throws <code>UnsupportedOperationException</code> by default.
     *
     * @since JSF 1.2
     */
    public String getResponseContentType()
    {
        ExternalContext ctx = _MyFacesExternalContextHelper.firstInstance.get();
        
        if (ctx == null)
        {
            throw new UnsupportedOperationException();
        }
        
        return ctx.getResponseContentType();
    }

    /**
     * @since JSF 2.0
     */
    public OutputStream getResponseOutputStream() throws IOException
    {
        ExternalContext ctx = _MyFacesExternalContextHelper.firstInstance.get();
        
        if (ctx == null)
        {
            throw new UnsupportedOperationException();
        }
        
        return ctx.getResponseOutputStream();
    }

    /**
     * @since JSF 2.0
     */
    public Writer getResponseOutputWriter() throws IOException
    {
        ExternalContext ctx = _MyFacesExternalContextHelper.firstInstance.get();
        
        if (ctx == null)
        {
            throw new UnsupportedOperationException();
        }
        
        return ctx.getResponseOutputWriter();
    }

    public abstract Object getSession(boolean create);

    public abstract Map<String, Object> getSessionMap();

    public abstract java.security.Principal getUserPrincipal();

    /**
     * @since 2.0
     */
    public void invalidateSession()
    {
        ExternalContext ctx = _MyFacesExternalContextHelper.firstInstance.get();
        
        if (ctx == null)
        {
            throw new UnsupportedOperationException();
        }
        
        ctx.invalidateSession();
    }

    /**
     * @since 2.0
     */
    public boolean isResponseCommitted()
    {
        ExternalContext ctx = _MyFacesExternalContextHelper.firstInstance.get();
        
        if (ctx == null)
        {
            throw new UnsupportedOperationException();
        }
        
        return ctx.isResponseCommitted();
    }

    public abstract boolean isUserInRole(String role);

    /**
     * @since 2.0
     */
    public abstract void log(String message);

    /**
     * @since 2.0
     */
    public abstract void log(String message, Throwable exception);

    public abstract void redirect(String url) throws java.io.IOException;

    /**
     *
     * @throws IOException
     *
     * @since 2.0
     */
    public void responseFlushBuffer() throws IOException
    {
        ExternalContext ctx = _MyFacesExternalContextHelper.firstInstance.get();
        
        if (ctx == null)
        {
            throw new UnsupportedOperationException();
        }
        
        ctx.responseFlushBuffer();
    }

    /**
     *
     * @since 2.0
     */
    public void responseReset()
    {
        ExternalContext ctx = _MyFacesExternalContextHelper.firstInstance.get();
        
        if (ctx == null)
        {
            throw new UnsupportedOperationException();
        }
        
        ctx.responseReset();
    }

    /**
     *
     * @param statusCode
     * @param message
     * @throws IOException
     *
     * @since 2.0
     */
    public void responseSendError(int statusCode, String message) throws IOException
    {
        ExternalContext ctx = _MyFacesExternalContextHelper.firstInstance.get();
        
        if (ctx == null)
        {
            throw new UnsupportedOperationException();
        }
        
        ctx.responseSendError(statusCode, message);
    }

    /**
     * throws <code>UnsupportedOperationException</code> by default.
     *
     * @since JSF 1.2
     * @param request
     */
    public void setRequest(Object request)
    {
        ExternalContext ctx = _MyFacesExternalContextHelper.firstInstance.get();
        
        if (ctx == null)
        {
            throw new UnsupportedOperationException();
        }
        
        ctx.setRequest(request);
    }

    /**
     * throws <code>UnsupportedOperationException</code> by default.
     *
     * @since JSF 1.2
     * @param encoding
     * @throws java.io.UnsupportedEncodingException
     */
    public void setRequestCharacterEncoding(java.lang.String encoding)
            throws java.io.UnsupportedEncodingException
    {
        ExternalContext ctx = _MyFacesExternalContextHelper.firstInstance.get();
        
        if (ctx == null)
        {
            throw new UnsupportedOperationException();
        }
        
        ctx.setRequestCharacterEncoding(encoding);
    }

    /**
     * throws <code>UnsupportedOperationException</code> by default.
     *
     * @since JSF 1.2
     * @param response
     */
    public void setResponse(Object response)
    {
        ExternalContext ctx = _MyFacesExternalContextHelper.firstInstance.get();
        
        if (ctx == null)
        {
            throw new UnsupportedOperationException();
        }
        
        ctx.setResponse(response);
    }

    /**
     *
     * @param size
     *
     * @since 2.0
     */
    public void setResponseBufferSize(int size)
    {
        ExternalContext ctx = _MyFacesExternalContextHelper.firstInstance.get();
        
        if (ctx == null)
        {
            throw new UnsupportedOperationException();
        }
        
        ctx.setResponseBufferSize(size);
    }

    /**
     * throws <code>UnsupportedOperationException</code> by default.
     *
     * @since JSF 1.2
     * @param encoding
     */
    public void setResponseCharacterEncoding(String encoding)
    {
        ExternalContext ctx = _MyFacesExternalContextHelper.firstInstance.get();
        
        if (ctx == null)
        {
            throw new UnsupportedOperationException();
        }
        
        ctx.setResponseCharacterEncoding(encoding);
    }

    /**
     *
     * @param length
     *
     * @since 2.0
     */
    public void setResponseContentLength(int length)
    {
        ExternalContext ctx = _MyFacesExternalContextHelper.firstInstance.get();
        
        if (ctx == null)
        {
            throw new UnsupportedOperationException();
        }
        
        ctx.setResponseContentLength(length);
    }

    /**
     *
     * @param contentType
     *
     * @since 2.0
     */
    public void setResponseContentType(String contentType)
    {
        ExternalContext ctx = _MyFacesExternalContextHelper.firstInstance.get();
        
        if (ctx == null)
        {
            throw new UnsupportedOperationException();
        }
        
        ctx.setResponseContentType(contentType);
    }

    /**
     *
     * @param name
     * @param value
     *
     * @since 2.0
     */
    public void setResponseHeader(String name, String value)
    {
        ExternalContext ctx = _MyFacesExternalContextHelper.firstInstance.get();
        
        if (ctx == null)
        {
            throw new UnsupportedOperationException();
        }
        
        ctx.setResponseHeader(name, value);
    }

    /**
     *
     * @param statusCode
     *
     * @since 2.0
     */
    public void setResponseStatus(int statusCode)
    {
        ExternalContext ctx = _MyFacesExternalContextHelper.firstInstance.get();
        
        if (ctx == null)
        {
            throw new UnsupportedOperationException();
        }
        
        ctx.setResponseStatus(statusCode);
    }
    
    /**
     * 
     * @since 2.1
     * @return
     */
    public boolean isSecure()
    {
        ExternalContext ctx = _MyFacesExternalContextHelper.firstInstance.get();
        
        if (ctx == null)
        {
            throw new UnsupportedOperationException();
        }

        return ctx.isSecure();
    }
    
    /**
     * 
     * @since 2.1
     * @return
     */
    public int getSessionMaxInactiveInterval()
    {
        ExternalContext ctx = _MyFacesExternalContextHelper.firstInstance.get();
        
        if (ctx == null)
        {
            throw new UnsupportedOperationException();
        }

        return ctx.getSessionMaxInactiveInterval();
    }
    
    /**
     * 
     * @since 2.1
     * @param interval
     */
    public void setSessionMaxInactiveInterval(int interval)
    {
        ExternalContext ctx = _MyFacesExternalContextHelper.firstInstance.get();
        
        if (ctx == null)
        {
            throw new UnsupportedOperationException();
        }
        
        ctx.setSessionMaxInactiveInterval(interval);
    }

    /**
     * @since 2.2
     * @return 
     */
    public ClientWindow getClientWindow()
    {
        ExternalContext ctx = _MyFacesExternalContextHelper.firstInstance.get();
        
        if (ctx == null)
        {
            /*throw new UnsupportedOperationException();*/
            // TODO: Return null for now, but it should throw exception
            // in JSF 2.2
            return null;
        }
        
        return ctx.getClientWindow();
    }
    
    /**
     * @since 2.2
     * @param window 
     */
    public void setClientWindow(ClientWindow window)
    {
        // No op for now.
        /*
        ExternalContext ctx = _MyFacesExternalContextHelper.firstInstance.get();
        
        if (ctx == null)
        {
            throw new UnsupportedOperationException();
        }
        
        ctx.setClientWindow(window);
        */
    }
    
    /**
     * @since 2.2
     * @param create
     * @return 
     */
    public String getSessionId(boolean create)
    {
        ExternalContext ctx = _MyFacesExternalContextHelper.firstInstance.get();
        
        if (ctx == null)
        {
            throw new UnsupportedOperationException();
        }
        
        return ctx.getSessionId(create);
    }
    
    /**
     * @since 2.2
     * @return
     */
    public String getApplicationContextPath()
    {
        ExternalContext ctx = _MyFacesExternalContextHelper.firstInstance.get();
        
        if (ctx == null)
        {
            throw new UnsupportedOperationException();
        }
        
        return ctx.getApplicationContextPath();
    }
    
    /**
     * 
     * @param url
     * @return 
     */
    public String encodeWebsocketURL(String url)
    {
        ExternalContext ctx = _MyFacesExternalContextHelper.firstInstance.get();
        
        if (ctx == null)
        {
            throw new UnsupportedOperationException();
        }
        
        return ctx.encodeWebsocketURL(url);
    }
}
