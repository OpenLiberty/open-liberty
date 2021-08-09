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

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

import javax.faces.FacesException;
import javax.faces.FactoryFinder;
import javax.faces.context.FacesContext;
import javax.faces.context.Flash;
import javax.faces.context.FlashFactory;
import javax.faces.context.PartialResponseWriter;
import javax.faces.context.PartialViewContext;
import javax.faces.lifecycle.ClientWindow;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.myfaces.shared.context.flash.FlashImpl;
import org.apache.myfaces.util.EnumerationIterator;

/**
 * Implements the external context for servlet request. JSF 1.2, 6.1.3
 *
 * @author Manfred Geiler (latest modification by $Author: lu4242 $)
 * @author Anton Koinov
 * @version $Revision: 1542444 $ $Date: 2013-11-16 01:41:08 +0000 (Sat, 16 Nov 2013) $
 */
public final class ServletExternalContextImpl extends ServletExternalContextImplBase
{
    //private static final Log log = LogFactory.getLog(ServletExternalContextImpl.class);
    private static final Logger log = Logger.getLogger(ServletExternalContextImpl.class.getName());

    private static final String URL_PARAM_SEPERATOR="&";
    private static final String URL_QUERY_SEPERATOR="?";
    private static final String URL_FRAGMENT_SEPERATOR="#";
    private static final String URL_NAME_VALUE_PAIR_SEPERATOR="=";

    private ServletRequest _servletRequest;
    private ServletResponse _servletResponse;
    private Map<String, Object> _sessionMap;
    private Map<String, Object> _requestMap;
    private Map<String, String> _requestParameterMap;
    private Map<String, String[]> _requestParameterValuesMap;
    private Map<String, String> _requestHeaderMap;
    private Map<String, String[]> _requestHeaderValuesMap;
    private Map<String, Object> _requestCookieMap;
    private HttpServletRequest _httpServletRequest;
    private HttpServletResponse _httpServletResponse;
    private String _requestServletPath;
    private String _requestPathInfo;
    private FlashFactory _flashFactory;
    private Flash _flash;
    private FacesContext _currentFacesContext;

    public ServletExternalContextImpl(final ServletContext servletContext, 
            final ServletRequest servletRequest,
            final ServletResponse servletResponse)
    {
        super(servletContext); // initialize ServletExternalContextImplBase
        
        _servletRequest = servletRequest;
        _servletResponse = servletResponse;
        _sessionMap = null;
        _requestMap = null;
        _requestParameterMap = null;
        _requestParameterValuesMap = null;
        _requestHeaderMap = null;
        _requestHeaderValuesMap = null;
        _requestCookieMap = null;
        _httpServletRequest = isHttpServletRequest(servletRequest) ? (HttpServletRequest) servletRequest : null;
        _httpServletResponse = isHttpServletResponse(servletResponse) ? (HttpServletResponse) servletResponse : null;

        if (_httpServletRequest != null)
        {
            // HACK: MultipartWrapper scrambles the servletPath for some reason in Tomcat 4.1.29 embedded in JBoss
            // 3.2.3!?
            // (this was reported by frederic.auge [frederic.auge@laposte.net])
            _requestServletPath = _httpServletRequest.getServletPath();
            _requestPathInfo = _httpServletRequest.getPathInfo();
        }
    }
    
    public ServletExternalContextImpl(final ServletContext servletContext, 
            final ServletRequest servletRequest,
            final ServletResponse servletResponse,
            final FlashFactory flashFactory)
    {
        this(servletContext, servletRequest, servletResponse);
        _flashFactory = flashFactory;
    }

    public void release()
    {
        super.release(); // releases fields on ServletExternalContextImplBase
        
        _currentFacesContext = null;
        _servletRequest = null;
        _servletResponse = null;
        _sessionMap = null;
        _requestMap = null;
        _requestParameterMap = null;
        _requestParameterValuesMap = null;
        _requestHeaderMap = null;
        _requestHeaderValuesMap = null;
        _requestCookieMap = null;
        _httpServletRequest = null;
        _httpServletResponse = null;
    }

    @Override
    public Object getSession(boolean create)
    {
        checkHttpServletRequest();
        return ((HttpServletRequest) _servletRequest).getSession(create);
    }
    
    @Override
    public String getSessionId(boolean create)
    {
        checkHttpServletRequest();
        HttpSession session = ((HttpServletRequest) _servletRequest).getSession(create);
        if (session != null)
        {
            return session.getId();
        }
        else
        {
            return "";
        }
    }
    

    @Override
    public Object getRequest()
    {
        return _servletRequest;
    }

    /**
     * @since 2.0
     */
    @Override
    public int getRequestContentLength()
    {
        return _servletRequest.getContentLength();
    }

    @Override
    public Object getResponse()
    {
        return _servletResponse;
    }

    /**
     * @since 2.0
     */
    @Override
    public int getResponseBufferSize()
    {
        return _servletResponse.getBufferSize();
    }

    @Override
    public String getResponseContentType()
    {
        return _servletResponse.getContentType();
    }

    @Override
    public OutputStream getResponseOutputStream() throws IOException
    {
        return _servletResponse.getOutputStream();
    }

    /**
     * @since JSF 2.0
     */
    @Override
    public Writer getResponseOutputWriter() throws IOException
    {
        return _servletResponse.getWriter();
    }

    @Override
    public Map<String, Object> getSessionMap()
    {
        if (_sessionMap == null)
        {
            checkHttpServletRequest();
            _sessionMap = new SessionMap(_httpServletRequest);
        }
        return _sessionMap;
    }

    @Override
    public Map<String, Object> getRequestMap()
    {
        if (_requestMap == null)
        {
            _requestMap = new RequestMap(_servletRequest);
        }
        return _requestMap;
    }

    @Override
    public Map<String, String> getRequestParameterMap()
    {
        if (_requestParameterMap == null)
        {
            _requestParameterMap = new RequestParameterMap(_servletRequest);
        }
        return _requestParameterMap;
    }

    @Override
    public Map<String, String[]> getRequestParameterValuesMap()
    {
        if (_requestParameterValuesMap == null)
        {
            _requestParameterValuesMap = new RequestParameterValuesMap(_servletRequest);
        }
        return _requestParameterValuesMap;
    }

    @Override
    public int getRequestServerPort()
    {
        return _servletRequest.getServerPort();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Iterator<String> getRequestParameterNames()
    {
        return new EnumerationIterator(_servletRequest.getParameterNames());
    }

    @Override
    public Map<String, String> getRequestHeaderMap()
    {
        if (_requestHeaderMap == null)
        {
            checkHttpServletRequest();
            _requestHeaderMap = new RequestHeaderMap(_httpServletRequest);
        }
        return _requestHeaderMap;
    }

    @Override
    public Map<String, String[]> getRequestHeaderValuesMap()
    {
        if (_requestHeaderValuesMap == null)
        {
            checkHttpServletRequest();
            _requestHeaderValuesMap = new RequestHeaderValuesMap(_httpServletRequest);
        }
        return _requestHeaderValuesMap;
    }

    // FIXME: See with the EG if we can get the return value changed to Map<String, Cookie> as it
    //        would be more elegant -= Simon Lessard =-
    @Override
    public Map<String, Object> getRequestCookieMap()
    {
        if (_requestCookieMap == null)
        {
            checkHttpServletRequest();
            _requestCookieMap = new CookieMap(_httpServletRequest);
        }

        return _requestCookieMap;
    }

    @Override
    public Locale getRequestLocale()
    {
        return _servletRequest.getLocale();
    }

    @Override
    public String getRequestPathInfo()
    {
        checkHttpServletRequest();
        // return (_httpServletRequest).getPathInfo();
        // HACK: see constructor
        return _requestPathInfo;
    }

    @Override
    public String getRequestContentType()
    {
        return _servletRequest.getContentType();
    }

    @Override
    public String getRequestContextPath()
    {
        checkHttpServletRequest();
        return _httpServletRequest.getContextPath();
    }

    @Override
    public String getRequestScheme()
    {
        return _servletRequest.getScheme();
    }

    @Override
    public String encodeActionURL(final String url)
    {
        checkNull(url, "url");
        checkHttpServletRequest();
        String encodedUrl = ((HttpServletResponse) _servletResponse).encodeURL(url);
        encodedUrl = encodeURL(encodedUrl, null);
        return encodedUrl;
    }

    @Override
    public String encodeBookmarkableURL(String baseUrl, Map<String,List<String>> parameters)
    {
        return encodeURL(baseUrl, parameters);
    }

    @Override
    public String encodeResourceURL(final String url)
    {
        checkNull(url, "url");
        checkHttpServletRequest();
        return ((HttpServletResponse) _servletResponse).encodeURL(url);
    }

    @Override
    public String encodeNamespace(final String s)
    {
        return s;
    }

    @Override
    public String encodePartialActionURL(String url)
    {
        checkNull(url, "url");
        checkHttpServletRequest();
        return encodeURL(((HttpServletResponse) _servletResponse).encodeURL(url), null);
    }

    @Override
    public String encodeRedirectURL(String baseUrl, Map<String,List<String>> parameters)
    {
        return _httpServletResponse.encodeRedirectURL(encodeURL(baseUrl, parameters));
    }

    @Override
    public void dispatch(final String requestURI) throws IOException, FacesException
    {
        RequestDispatcher requestDispatcher = _servletRequest.getRequestDispatcher(requestURI);

        // If there is no dispatcher, send NOT_FOUND
        if (requestDispatcher == null)
        {
            ((HttpServletResponse) _servletResponse).sendError(HttpServletResponse.SC_NOT_FOUND);

            return;
        }

        try
        {
            requestDispatcher.forward(_servletRequest, _servletResponse);
        }
        catch (ServletException e)
        {
            if (e.getMessage() != null)
            {
                throw new FacesException(e.getMessage(), e);
            }

            throw new FacesException(e);

        }
    }

    @Override
    public String getRequestServerName()
    {
        return _servletRequest.getServerName();
    }

    @Override
    public String getRequestServletPath()
    {
        checkHttpServletRequest();
        // return (_httpServletRequest).getServletPath();
        // HACK: see constructor
        return _requestServletPath;
    }

    @Override
    public String getAuthType()
    {
        checkHttpServletRequest();
        return _httpServletRequest.getAuthType();
    }

    @Override
    public String getRemoteUser()
    {
        checkHttpServletRequest();
        return _httpServletRequest.getRemoteUser();
    }

    @Override
    public boolean isUserInRole(final String role)
    {
        checkNull(role, "role");
        checkHttpServletRequest();
        return _httpServletRequest.isUserInRole(role);
    }

    @Override
    public Principal getUserPrincipal()
    {
        checkHttpServletRequest();
        return _httpServletRequest.getUserPrincipal();
    }

    @Override
    public void invalidateSession()
    {
        HttpSession session = (HttpSession) getSession(false);

        if (session != null)
        {
            session.invalidate();
        }
    }

    /**
     * @since 2.0
     */
    @Override
    public boolean isResponseCommitted()
    {
        return _httpServletResponse.isCommitted();
    }

    @Override
    public void redirect(final String url) throws IOException
    {
        FacesContext facesContext = getCurrentFacesContext();
        PartialViewContext partialViewContext = facesContext.getPartialViewContext(); 
        if (partialViewContext.isPartialRequest())
        {
            PartialResponseWriter writer = partialViewContext.getPartialResponseWriter();
            this.setResponseContentType("text/xml");
            this.setResponseCharacterEncoding("UTF-8");
            this.addResponseHeader("Cache-control", "no-cache");
            writer.startDocument();
            writer.redirect(url);
            writer.endDocument();
            facesContext.responseComplete();
        }
        else if (_servletResponse instanceof HttpServletResponse)
        {
            ((HttpServletResponse) _servletResponse).sendRedirect(url);
            facesContext.responseComplete();
        }
        else
        {
            throw new IllegalArgumentException("Only HttpServletResponse supported");
        }
    }

    /**
     * @since 2.0
     */
    @Override
    public void responseFlushBuffer() throws IOException
    {
        checkHttpServletResponse();
        _httpServletResponse.flushBuffer();
    }

    /**
     * @since 2.0
     */
    @Override
    public void responseReset()
    {
        checkHttpServletResponse();
        _httpServletResponse.reset();
    }

    /**
     * @since 2.0
     */
    @Override
    public void responseSendError(int statusCode, String message) throws IOException
    {
        checkHttpServletResponse();
        if (message == null)
        {
            _httpServletResponse.sendError(statusCode);
        }
        else
        {
            _httpServletResponse.sendError(statusCode, message);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Iterator<Locale> getRequestLocales()
    {
        checkHttpServletRequest();
        return new EnumerationIterator(_httpServletRequest.getLocales());
    }

    /**
     * @since JSF 1.2
     * @param request
     */
    @Override
    public void setRequest(final java.lang.Object request)
    {
        this._servletRequest = (ServletRequest) request;
        this._httpServletRequest = isHttpServletRequest(_servletRequest) ? (HttpServletRequest) _servletRequest : null;
        this._httpServletRequest = isHttpServletRequest(_servletRequest) ? (HttpServletRequest) _servletRequest : null;
        this._requestHeaderMap = null;
        this._requestHeaderValuesMap = null;
        this._requestMap = null;
        this._requestParameterMap = null;
        this._requestParameterValuesMap = null;
        this._requestCookieMap = null;
        this._sessionMap = null;
    }

    /**
     * @since JSF 1.2
     * @param encoding
     * @throws java.io.UnsupportedEncodingException
     */
    @Override
    public void setRequestCharacterEncoding(final java.lang.String encoding) throws java.io.UnsupportedEncodingException
    {

        this._servletRequest.setCharacterEncoding(encoding);

    }

    /**
     * @since JSF 1.2
     */
    @Override
    public String getRequestCharacterEncoding()
    {
        return _servletRequest.getCharacterEncoding();
    }

    /**
     * @since JSF 1.2
     */
    @Override
    public String getResponseCharacterEncoding()
    {
        return _servletResponse.getCharacterEncoding();
    }

    /**
     * @since JSF 1.2
     * @param response
     */
    @Override
    public void setResponse(final java.lang.Object response)
    {
        this._servletResponse = (ServletResponse) response;
    }

    /**
     * @since 2.0
     */
    @Override
    public void setResponseBufferSize(int size)
    {
        checkHttpServletResponse();
        _httpServletResponse.setBufferSize(size);
    }

    /**
     * @since JSF 1.2
     * @param encoding
     */
    @Override
    public void setResponseCharacterEncoding(final java.lang.String encoding)
    {
        this._servletResponse.setCharacterEncoding(encoding);
    }

    /**
     * @since 2.0
     */
    @Override
    public void setResponseContentLength(int length)
    {
        checkHttpServletResponse();
        _httpServletResponse.setContentLength(length);
    }

    @Override
    public void setResponseContentType(String contentType)
    {
        // If the response has not been committed yet.
        if (!_servletResponse.isCommitted())
        {
            // Sets the content type of the response being sent to the client
            _servletResponse.setContentType(contentType);
        }
        else
        {
            // I did not throw an exception just to be sure nothing breaks.
            log.severe("Cannot set content type. Response already committed");
        }
    }

    /**
     * @since 2.0
     */
    @Override
    public void setResponseHeader(String name, String value)
    {
        checkHttpServletResponse();
        _httpServletResponse.setHeader(name, value);
    }

    @Override
    public void setResponseStatus(int statusCode)
    {
        checkHttpServletResponse();
        _httpServletResponse.setStatus(statusCode);
    }

    private void checkHttpServletRequest()
    {
        if (_httpServletRequest == null)
        {
            throw new UnsupportedOperationException("Only HttpServletRequest supported");
        }
    }

    private boolean isHttpServletRequest(final ServletRequest servletRequest)
    {
        return servletRequest instanceof HttpServletRequest;
    }

    private void checkHttpServletResponse()
    {
        if (_httpServletRequest == null)
        {
            throw new UnsupportedOperationException("Only HttpServletResponse supported");
        }
    }
    private boolean isHttpServletResponse(final ServletResponse servletResponse)
    {
        return servletResponse instanceof HttpServletResponse;
    }

    /**
     * @since JSF 2.0
     */
    @Override
    public void addResponseCookie(final String name,
            final String value, final Map<String, Object> properties)
    {
        checkHttpServletResponse();
        Cookie cookie = new Cookie(name, value);
        if (properties != null)
        {
            for (Map.Entry<String, Object> entry : properties.entrySet())
            {
                String propertyKey = entry.getKey();
                Object propertyValue = entry.getValue();
                if ("comment".equals(propertyKey))
                {
                    cookie.setComment((String) propertyValue);
                    continue;
                }
                else if ("domain".equals(propertyKey))
                {
                    cookie.setDomain((String)propertyValue);
                    continue;
                }
                else if ("maxAge".equals(propertyKey))
                {
                    cookie.setMaxAge((Integer) propertyValue);
                    continue;
                }
                else if ("secure".equals(propertyKey))
                {
                    cookie.setSecure((Boolean) propertyValue);
                    continue;
                }
                else if ("path".equals(propertyKey))
                {
                    cookie.setPath((String) propertyValue);
                    continue;
                }
                else if ("httpOnly".equals(propertyKey))
                {
                    cookie.setHttpOnly((Boolean) propertyValue);
                    continue;
                }
                throw new IllegalArgumentException("Unused key when creating Cookie");
            }
        }
        _httpServletResponse.addCookie(cookie);
    }

    @Override
    public void addResponseHeader(String name, String value)
    {
        _httpServletResponse.addHeader(name, value);
    }

    private String encodeURL(String baseUrl, Map<String, List<String>> parameters)
    {
        checkNull(baseUrl, "url");
        checkHttpServletRequest();

        String fragment = null;
        String queryString = null;
        Map<String, List<String>> paramMap = null;

        //extract any URL fragment
        int index = baseUrl.indexOf(URL_FRAGMENT_SEPERATOR);
        if (index != -1)
        {
            fragment = baseUrl.substring(index+1);
            baseUrl = baseUrl.substring(0,index);
        }

        //extract the current query string and add the params to the paramMap
        index = baseUrl.indexOf(URL_QUERY_SEPERATOR);
        if (index != -1)
        {
            queryString = baseUrl.substring(index + 1);
            baseUrl = baseUrl.substring(0, index);
            String[] nameValuePairs = queryString.split(URL_PARAM_SEPERATOR);
            for (int i = 0; i < nameValuePairs.length; i++)
            {
                String[] currentPair = nameValuePairs[i].split(URL_NAME_VALUE_PAIR_SEPERATOR);

                ArrayList<String> value = new ArrayList<String>(1);
                try
                {
                    value.add(currentPair.length > 1
                                ? URLDecoder.decode(currentPair[1], getResponseCharacterEncoding())
                                : "");
                }
                catch (UnsupportedEncodingException e)
                {
                    //shouldn't ever get here
                    throw new UnsupportedOperationException("Encoding type=" + getResponseCharacterEncoding()
                                                            + " not supported", e);
                }
                if (paramMap == null)
                {
                    paramMap = new HashMap<String, List<String>>();
                }
                paramMap.put(currentPair[0], value);
            }
        }

        //add/update with new params on the paramMap
        if (parameters != null && parameters.size() > 0)
        {
            for (Map.Entry<String, List<String>> pair : parameters.entrySet())
            {
                if (pair.getKey() != null && pair.getKey().trim().length() != 0)
                {
                    if (paramMap == null)
                    {
                        paramMap = new HashMap<String, List<String>>();
                    }
                    paramMap.put(pair.getKey(), pair.getValue());
                }
            }
        }
        
        FacesContext facesContext = getCurrentFacesContext();
        ClientWindow window = facesContext.getExternalContext().getClientWindow();
        if (window != null)
        {
            //TODO: Use StringBuilder or some optimization.
            if (window.isClientWindowRenderModeEnabled(facesContext))
            {
                Map<String, String> map = window.getQueryURLParameters(facesContext);
                if (map != null)
                {
                    for (Map.Entry<String , String> entry : map.entrySet())
                    {
                        ArrayList<String> value = new ArrayList<String>(1);
                        value.add(entry.getValue());
                        if (paramMap == null)
                        {
                            paramMap = new HashMap<String, List<String>>();
                        }
                        paramMap.put(entry.getKey(), value);
                    }
                }
            }
        }        

        boolean hasParams = paramMap != null && paramMap.size()>0;

        if (!hasParams && fragment == null) {
            return baseUrl;
        }

        // start building the new URL
        StringBuilder newUrl = new StringBuilder(baseUrl);

        //now add the updated param list onto the url
        if (hasParams)
        {
            boolean isFirstPair = true;
            for (Map.Entry<String, List<String>> pair : paramMap.entrySet())
            {
                for (String value : pair.getValue())
                {
                    if (!isFirstPair)
                    {
                        newUrl.append(URL_PARAM_SEPERATOR);
                    }
                    else
                    {
                        newUrl.append(URL_QUERY_SEPERATOR);
                        isFirstPair = false;
                    }

                    newUrl.append(pair.getKey());
                    newUrl.append(URL_NAME_VALUE_PAIR_SEPERATOR);
                    try
                    {
                        newUrl.append(URLEncoder.encode(value,getResponseCharacterEncoding()));
                    }
                    catch (UnsupportedEncodingException e)
                    {
                        //shouldn't ever get here
                        throw new UnsupportedOperationException("Encoding type=" + getResponseCharacterEncoding()
                                                                + " not supported", e);
                    }
                }
            }
        }

        //add the fragment back on (if any)
        if (fragment != null)
        {
            newUrl.append(URL_FRAGMENT_SEPERATOR);
            newUrl.append(fragment);
        }

        return newUrl.toString();
    }
    
    /**
     * @since 2.0
     */
    public Flash getFlash()
    {
        if (_flash == null)
        {
            if (_flashFactory == null)
            {
                _flashFactory = (FlashFactory) FactoryFinder.getFactory(
                    FactoryFinder.FLASH_FACTORY);
                if (_flashFactory == null)
                {
                    //Fallback to servlet default flash
                    _flash = FlashImpl.getCurrentInstance(this);
                }
                else
                {
                    _flash = _flashFactory.getFlash(true);
                }
            }
            else
            {
                _flash = _flashFactory.getFlash(true);
            }
        }
        return _flash;
        //return FlashImpl.getCurrentInstance(this);
    }

    @Override
    public boolean isSecure()
    {
        return _servletRequest.isSecure();
    }

    @Override
    public int getSessionMaxInactiveInterval()
    {
        HttpSession session = _httpServletRequest.getSession();
        return session.getMaxInactiveInterval();
    }
    
    @Override
    public void setSessionMaxInactiveInterval(int interval)
    {
        HttpSession session = _httpServletRequest.getSession();
        session.setMaxInactiveInterval(interval);
    }
    
    protected FacesContext getCurrentFacesContext()
    {
        if (_currentFacesContext == null)
        {
            _currentFacesContext = FacesContext.getCurrentInstance();
        }
        return _currentFacesContext;
    }
}
