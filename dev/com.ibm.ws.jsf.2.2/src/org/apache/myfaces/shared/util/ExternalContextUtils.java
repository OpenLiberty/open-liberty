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
package org.apache.myfaces.shared.util;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.faces.context.ExternalContext;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponseWrapper;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * This provides some functionality for determining some things about the
 * native request object that is not provided by JSF.  This class is useful
 * for use in places where Portlet API's may or may not be present and can
 * also provide access to some request-specific items which are not available on
 * the JSF ExternalContext.  If portlet API's are not present, this class simply 
 * handles the Servlet Request type.
 */
public final class ExternalContextUtils
{

    /**
     * Returns <code>true</code> if a particular class relating to the supplied
     * request type is available on the current classpath or <code>false</code> 
     * if it is not.  This class assumes that all containers have a servlet type
     * request available, but the portlet request types are all dependant on the 
     * portlet container being used.
     * 
     * @param type the RequestType to test
     * @return a boolean value of <code>true</code> if the container contains the
     *         request type in the classpath
     * @since 2.0
     */
    public static final boolean isRequestTypeAvailable(RequestType type)
    {
        switch (type)
        {
        case SERVLET:
            return true;

        case ACTION:
        case RENDER:
            return _PORTLET_CONTEXT_CLASS != null;

        case RESOURCE:
        case EVENT:
            return _PORTLET_RENDER_REQUEST_CLASS != null;

        default:
            return false;
        }
    }

    /**
     * Returns <code>true</code> if a particular request type is supported by the
     * container.  For a request type to be supported, the required objects must
     * be on the classpath AND and, in the case of Portlet RequestTypes, an 
     * appropriate bridge must be avaialble which supports those objects.  This
     * means that if the supplied RequestType is RESOURCE, the 
     * javax.portlet.ResourceRequest object must be available on the classpath AND
     * a bridge which supports the Portlet 2.0 specification would also need to be
     * available.
     * 
     * @param type the RequestType to test
     * @return a boolean value of <code>true</code> if the container supports the
     *         current request type
     * @since 2.0
     */
    public static final boolean isRequestTypeSupported(RequestType type)
    {
        switch (type)
        {
        case SERVLET:
            return true;

        case ACTION:
        case RENDER:
            return _PORTLET_10_SUPPORTED;

        case RESOURCE:
        case EVENT:
            return _PORTLET_20_SUPPORTED;

        default:
            return false;
        }
    }
    
    /**
     * Returns the requestType of this ExternalContext.
     * 
     * @param externalContext the current external context
     * @return the appropriate RequestType for this external context
     * @see RequestType
     */
    public static final RequestType getRequestType(ExternalContext externalContext)
    {
        //Stuff is laid out strangely in this class in order to optimize
        //performance.  We want to do as few instanceof's as possible so
        //things are laid out according to the expected frequency of the
        //various requests occurring.
        if(_PORTLET_10_SUPPORTED || _PORTLET_20_SUPPORTED)
        {
            if (_PORTLET_CONTEXT_CLASS.isInstance(externalContext.getContext()))
            {
                //We are inside of a portlet container
                Object request = externalContext.getRequest();
                
                if(_PORTLET_RENDER_REQUEST_CLASS.isInstance(request))
                {
                    return RequestType.RENDER;
                }
                
                if(_PORTLET_RESOURCE_REQUEST_CLASS != null)
                {
                    if(_PORTLET_ACTION_REQUEST_CLASS.isInstance(request))
                    {
                        return RequestType.ACTION;
                    }

                    //We are in a JSR-286 container
                    if(_PORTLET_RESOURCE_REQUEST_CLASS.isInstance(request))
                    {
                        return RequestType.RESOURCE;
                    }
                    
                    return RequestType.EVENT;
                }
                
                return RequestType.ACTION;
            }
        }
        
        return RequestType.SERVLET;
    }

    /**
     * This method is used when a ExternalContext object is not available,
     * like in TomahawkFacesContextFactory.
     * 
     * According to TOMAHAWK-1331, the object context could receive an
     * instance of javax.portlet.PortletContext or javax.portlet.PortletConfig,
     * so we check both cases.
     * 
     * @param context
     * @param request
     * @return
     */
    public static final RequestType getRequestType(Object context, Object request)
    {
        //Stuff is laid out strangely in this class in order to optimize
        //performance.  We want to do as few instanceof's as possible so
        //things are laid out according to the expected frequency of the
        //various requests occurring.

        if(_PORTLET_10_SUPPORTED || _PORTLET_20_SUPPORTED)
        {
            if (_PORTLET_CONFIG_CLASS.isInstance(context) ||
                _PORTLET_CONTEXT_CLASS.isInstance(context))
            {
                //We are inside of a portlet container
                
                if(_PORTLET_RENDER_REQUEST_CLASS.isInstance(request))
                {
                    return RequestType.RENDER;
                }
                
                if(_PORTLET_RESOURCE_REQUEST_CLASS != null)
                {
                    if(_PORTLET_ACTION_REQUEST_CLASS.isInstance(request))
                    {
                        return RequestType.ACTION;
                    }

                    //We are in a JSR-286 container
                    if(_PORTLET_RESOURCE_REQUEST_CLASS.isInstance(request))
                    {
                        return RequestType.RESOURCE;
                    }
                    
                    return RequestType.EVENT;
                }
                
                return RequestType.ACTION;
            }
        }
        
        return RequestType.SERVLET;
    }

    /**
     * Returns the current active session id or <code>null</code> if there is
     * none.  If a session is not already created, this method will create one
     * for you.
     * 
     * @param ec the current external context
     * @return a string containing the requestedSessionId
     */
    public static String getSessionId(ExternalContext ec)
    {
        return getSessionId(ec, true);
    }

    /**
     * Returns the current active session id or <code>null</code> if there is
     * none.
     * 
     * @param ec the current external context
     * @param create create a new session if one is not created
     * @return a string containing the requestedSessionId
     */
    public static String getSessionId(ExternalContext ec, boolean create)
    {
        Object session = ec.getSession(create);
        return (null != session) ? (String) _runMethod(session, "getId") : null;
    }

    /**
     * Returns the session ID for the client, or <code>null</code> if there is none.
     *
     * @param ec the current external context
     * @return a string containing the requestedSessionId
     */
    public static String getRequestedSessionId(ExternalContext ec)
    {
        return (String) _runMethod(ec.getRequest(), "getRequestedSessionId");
    }

    /**
     * Checks if the requested session ID is still valid.
     *
     * @param ec the current external context
     * @return a boolean containing <code>true</code> if the request session is
     *         valid or <code>false</code> if it is not
     */
    public static boolean isRequestedSessionIdValid(ExternalContext ec)
    {
        return (Boolean) _runMethod(ec.getRequest(),
                "isRequestedSessionIdValid");
    }

    /**
     * Returns the contextPath of the ServletContext or <code>null</code> for portlets
     *
     * @param ec the current external context
     * @return a String containing the servletContextPath
     */
    public static String getServletContextPath(ExternalContext ec)
    {
        if (!isPortlet(ec))
        {
            return ((ServletContext) ec.getContext()).getContextPath();
        }
        else
        {
            return null;
        }
    }

    /**
     * Returns the contextPath of the ServletRequest or <code>null</code> for portlet requests
     *
     * @param ec the current external context
     * @return a String containing the request context path
     * @see ExternalContext#getRequestContextPath()
     * 
     * @deprecated use ExternalContext.getRequestContextPath() as of JSF 1.2.  This method
     *             does not appropriately handle portlet environments, but the functionality
     *             is maintained to prevent needing to change the contract.
     */
    @Deprecated
    public static String getRequestContextPath(ExternalContext ec)
    {
        if (!isPortlet(ec))
        {
            return ec.getRequestContextPath();
        }
        else
        {
            return null;
        }
    }

    /**
     * Returns the requestURI of the HttpServletRequest or <code>null</code> for 
     * portlet requests
     *
     * @param ec the current external context
     * @return A string containing the current request uri
     */
    public static String getRequestURI(ExternalContext ec)
    {
        if (!isPortlet(ec))
        {
            return ((HttpServletRequest) ec.getRequest()).getRequestURI();
        }
        else
        {
            return null;
        }
    }

    /**
     * Returns the character encoding or <code>null</code> if there isn't any
     *
     * @param ec the current external context
     * @return a string containing the request's character encoding
     * @see ExternalContext#getRequestCharacterEncoding()
     * 
     * @deprecated replaced by an API in JSF.  Use ExternalContext.getRequestCharacterEncoding()
     */
    @Deprecated
    public static String getCharacterEncoding(ExternalContext ec)
    {
        return ec.getRequestCharacterEncoding();
    }

    /**
     * Returns the name of the underlying context or <code>null</code> if something
     * went wrong in trying to retrieve the context.
     * 
     * @param ec the current external context
     * @return a String containing the context name
     */
    public static String getContextName(ExternalContext ec)
    {
        try
        {
            if (isPortlet(ec))
            {
                return (String) _runMethod(ec.getContext(),
                        "getPortletContextName");
            }
            else
            {
                return ((ServletContext) ec.getContext())
                        .getServletContextName();
            }
        }
        catch (final ClassCastException e)
        {
            _LOG.severe(e.getMessage());
        }
        return null;
    }

    /**
     * Returns the name and version of the underlying servlet container or <code>null</code> if something
     * went wrong in trying to retrieve the context.
     *
     * @param ec the current external context
     * @return a String containing the name and version of the underlying servlet container
     */
    public static String getServerInfo(ExternalContext ec)
    {
        try
        {
            if (isPortlet(ec))
            {
                return (String) _runMethod(ec.getContext(), "getServerInfo");
            }
            else
            {
                return ((ServletContext) ec.getContext()).getServerInfo();
            }
        }
        catch (final ClassCastException e)
        {
            _LOG.severe(e.getMessage());
        }
        return null;
    }

    /**
     * Returns the content length or -1 if the unknown.
     *
     * @param ec the current external context
     * @return the length or -1 if the length is unknown
     */
    public static int getContentLength(ExternalContext ec)
    {
        if (isRequestFromClient(ec))
        {
            return (Integer) _runMethod(ec.getRequest(), "getContentLength");
        }

        return -1;
    }

    /**
     * Returns the content type from the current externalContext or
     * <code>null</code> if unknown.
     *
     * @param ec the current external context
     * @return a String contining the the content type or <code>null</code>
     * @see ExternalContext#getRequestContentType()
     *
     * @deprecated use ExternalContext.getRequestContentType()
     */
    @Deprecated
    public static String getContentType(ExternalContext ec)
    {
        return ec.getRequestContentType();
    }

    /**
     * Returns the request input stream if one is available
     *
     * @param ec the current external context
     * @return the request's input stream
     * @throws IOException if there was a problem getting the input stream
     */
    public static InputStream getRequestInputStream(ExternalContext ec)
            throws IOException
    {
        RequestType type = getRequestType(ec);
        if (type.isRequestFromClient())
        {
            Object req = ec.getRequest();
            if (type.isPortlet())
            {
                return (InputStream) _runMethod(req, "getPortletInputStream");
            }
            else
            {
                return ((ServletRequest) ec.getRequest()).getInputStream();
            }
        }

        return null;
    }

    /**
     * Returns <code>true</code> if this externalContext represents an "action". 
     * An action request is any ServletRequest or a portlet ActionRequest or 
     * ResourceRequest.
     *
     * @param ec the current external context
     * @return a boolean of <code>true</code> if this request is an action-type
     *         request.
     * @see #isRequestFromClient(ExternalContext)
     *         
     * @deprecated replaced with {@link #isRequestFromClient(ExternalContext)}
     */
    @Deprecated
    public static boolean isAction(ExternalContext ec)
    {
        return isRequestFromClient(ec);
    }

    /**
     * Returns the value of {@link RequestType#isPortlet()} for the current
     * RequestType. This is a convenience function designed to perform a quick
     * check of the current request. If more capabilities need to be tested for
     * the given request, then it is more efficient to pull this information from
     * the RequestType itself.
     * 
     * @param ec the current external context
     * @return a boolean value of <code>true</code> if the current RequestType
     *         is a portlet request.
     * 
     * @see RequestType#isPortlet()
     * @see #getRequestType(ExternalContext)
     */
    public static boolean isPortlet(ExternalContext ec)
    {
        return getRequestType(ec).isPortlet();
    }

    /**
     * Returns the value of {@link RequestType#isResponseWritable()} for the
     * current RequestType. This is a convenience function designed to perform a
     * quick check of the current request. If more capabilities need to be tested
     * for the given request, then it is more efficient to pull this information
     * from the RequestType itself.
     * 
     * @param ec the current external context
     * @return a boolean value of <code>true</code> if the current RequestType
     *         is a "render" type response.
     * 
     * @see RequestType#isResponseWritable()
     * @see #getRequestType(ExternalContext)
     * @since 2.0
     */
    public static final boolean isResponseWritable(ExternalContext ec)
    {
        return getRequestType(ec).isResponseWritable();
    }

    /**
     * Returns the value of {@link RequestType#isRequestFromClient()} for the
     * current RequestType. This is a convenience function designed to perform a
     * quick check of the current request. If more capabilities need to be tested
     * for the given request, then it is more efficient to pull this information
     * from the RequestType itself.
     * 
     * @param ec the current external context
     * @return a boolean value of <code>true</code> if the current RequestType
     *         represents a request from the client.
     * 
     * @see RequestType#isResponseWritable()
     * @see #getRequestType(ExternalContext)
     * @since 2.0
     */
    public static final boolean isRequestFromClient(ExternalContext ec)
    {
        return getRequestType(ec).isRequestFromClient();
    }

    /**
     * Returns wherther of not this external context represents a true HttpServletRequest or
     * not.  Some portal containers implement the PortletRequest/Response objects as 
     * HttpServletRequestWrappers, and those objects should not be treated as an
     * HttpServlerRequest.  As such, this method first tests to see if the request is
     * a portlet request and, if not, then tests to see if the request is an instanceof
     * HttpServletRequest.
     * 
     * @param ec the current external context
     * @return a boolean value of <code>true</code> if the current request is an
     *         HttpServletRequest
     * @since 1.1
     */
    public static boolean isHttpServletRequest(ExternalContext ec)
    {
        return (!isPortlet(ec) && (ec.getRequest() instanceof HttpServletRequest));
    }

    /**
     * Returns an HttpServletResponse if one exists on the externalContext or null
     * if it does not.  Please note that some portal environments implement the
     * PortletRequest and Response objects as HttpServletRequest/Response objects.
     * This method handles these types of requests properly and will therefore
     * return null in portal environments.
     * 
     * @param response
     * @return an HttpServletResponse if we have one or null if we do not
     * @since 4.0
     */
    public static HttpServletResponse getHttpServletResponse(ExternalContext ec)
    {
        if (isHttpServletRequest(ec))
        {
            return (HttpServletResponse) ec.getResponse();
        }

        return null;
    }

    /**
     * Runs a method on an object and returns the result
     * 
     * @param obj the object to run the method on
     * @param methodName the name of the method
     * @return the results of the method run
     */
    private static Object _runMethod(Object obj, String methodName)
    {
        try
        {
            Method sessionIdMethod = obj.getClass().getMethod(methodName);
            return sessionIdMethod.invoke(obj);
        }
        catch (Exception e)
        {
            return null;
        }

    }

    // prevent this from being instantiated
    private ExternalContextUtils()
    {
    }

    private static final Logger _LOG = Logger
            .getLogger(ExternalContextUtils.class.getName());

    // =-= Scott O'Bryan =-=
    // Performance enhancement. These will be needed anyway, let's not get them every time.
    private static final Class<?> _PORTLET_ACTION_REQUEST_CLASS;
    private static final Class<?> _PORTLET_RENDER_REQUEST_CLASS;
    private static final Class<?> _PORTLET_RESOURCE_REQUEST_CLASS;
    private static final Class<?> _PORTLET_CONTEXT_CLASS;
    private static final boolean _PORTLET_10_SUPPORTED;
    private static final boolean _PORTLET_20_SUPPORTED;
    private static final Class<?> _PORTLET_CONFIG_CLASS;

    static
    {
        Class<?> context;
        Class<?> config;
        Class<?> actionRequest;
        Class<?> renderRequest;
        Class<?> resourceRequest;
        boolean portlet20Supported = false;
        boolean portlet10Supported = false;

        try
        {
            context = ClassLoaderUtils
                    .loadClass("javax.portlet.PortletContext");
            config = ClassLoaderUtils.loadClass("javax.portlet.PortletConfig");
            actionRequest = ClassLoaderUtils
                    .loadClass("javax.portlet.ActionRequest");
            renderRequest = ClassLoaderUtils
                    .loadClass("javax.portlet.RenderRequest");

            try
            {
                resourceRequest = ClassLoaderUtils
                        .loadClass("javax.portlet.ResourceRequest");
            }
            catch (ClassNotFoundException e)
            {
                _LOG.fine("Portlet 2.0 API is not available on classpath.  Portlet 2.0 functionality is disabled");
                resourceRequest = null;
            }
        }
        catch (final ClassNotFoundException e)
        {
            _LOG.fine("Portlet API is not available on the classpath.  Portlet configurations are disabled.");
            context = null;
            config = null;
            actionRequest = null;
            renderRequest = null;
            resourceRequest = null;
        }

        //Find bridge to tell if portal is supported
        if (context != null)
        {
            // Portlet 1.0 API found. In this case we have to consider that exists alternate
            // bridge implementations like in WebSphere and others.
            portlet10Supported = true;

            try
            {
                Class<?> bridge = ClassLoaderUtils
                        .loadClass("javax.portlet.faces.Bridge");

                if (bridge != null)
                {
                    //Standard bridge defines a spec name which can be used to 
                    //determine Portlet 2.0 Support.
                    String specName = bridge.getPackage()
                            .getSpecificationTitle();
                    _LOG.fine("Found Bridge: " + specName);
                    if (specName != null && specName.startsWith("Portlet 2"))
                    {
                        portlet20Supported = true;
                    }

                    if (_LOG.isLoggable(Level.INFO))
                    {
                        String ver = (portlet20Supported) ? "2.0" : "1.0";
                        _LOG.info("Portlet Environment Detected: " + ver);
                    }
                }
            }
            catch (ClassNotFoundException e)
            {
                _LOG.fine("Portlet API is present but Standard Apache Portlet Bridge is not. "
                        + " This could happen if you are using an alternate Portlet Bridge solution.");

                if (resourceRequest != null)
                {
                    portlet20Supported = true;
                }
            }
        }

        _PORTLET_CONTEXT_CLASS = context;
        _PORTLET_CONFIG_CLASS = config;
        _PORTLET_ACTION_REQUEST_CLASS = actionRequest;
        _PORTLET_RENDER_REQUEST_CLASS = renderRequest;
        _PORTLET_RESOURCE_REQUEST_CLASS = resourceRequest;
        _PORTLET_10_SUPPORTED = portlet10Supported;
        _PORTLET_20_SUPPORTED = portlet20Supported;
    }

    /**
     * Trys to obtain a HttpServletResponse from the Response.
     * Note that this method also trys to unwrap any ServletResponseWrapper
     * in order to retrieve a valid HttpServletResponse.
     * @param response
     * @return if found, the HttpServletResponse, null otherwise
     */
    public static HttpServletResponse getHttpServletResponse(Object response)
    {
        // unwrap the response until we find a HttpServletResponse
        while (response != null)
        {
            if (response instanceof HttpServletResponse)
            {
                // found
                return (HttpServletResponse) response;
            }
            if (response instanceof ServletResponseWrapper)
            {
                // unwrap
                response = ((ServletResponseWrapper) response).getResponse();
            }
            // no more possibilities to find a HttpServletResponse
            break;
        }
        return null; // not found
    }

    /**
     * Trys to obtain a ResponseSwitch from the Response.
     * @param response
     * @return if found, the ResponseSwitch, null otherwise
     */
    /*
    public static ResponseSwitch getResponseSwitch(Object response)
    {
        // unwrap the response until we find a ResponseSwitch
        while (response != null)
        {
            if (response instanceof ResponseSwitch)
            {
                // found
                return (ResponseSwitch) response;
            }
            if (response instanceof ServletResponseWrapper)
            {
                // unwrap
                response = ((ServletResponseWrapper) response).getResponse();
            }
            // no more possibilities to find a ResponseSwitch
            break; 
        }
        return null; // not found
    }*/

    /**
     * Try to create a ResponseSwitch for this response.
     * @param response
     * @return the created ResponseSwitch, if there is a ResponseSwitch 
     *         implementation for the given response, null otherwise
     */
    /*
    public static ResponseSwitch createResponseSwitch(Object response)
    {
        if (response instanceof HttpServletResponse)
        {
            return new HttpServletResponseSwitch((HttpServletResponse) response);
        }
        else if (response instanceof ServletResponse)
        {
            return new ServletResponseSwitch((ServletResponse) response);
        }
        return null;
    }*/

}
