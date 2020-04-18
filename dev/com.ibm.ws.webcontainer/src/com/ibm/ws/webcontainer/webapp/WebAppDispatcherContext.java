/*******************************************************************************
 * Copyright (c) 1997, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.webapp;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.DispatcherType;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpUtils;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.servlet.error.ServletErrorReport;
import com.ibm.ws.webcontainer.osgi.WebContainer;
import com.ibm.ws.webcontainer.session.IHttpSessionContext;
import com.ibm.ws.webcontainer.srt.SRTRequestContext;
import com.ibm.ws.webcontainer.srt.SRTServletRequest;
import com.ibm.ws.webcontainer.util.UnsynchronizedStack;
import com.ibm.wsspi.webcontainer.RequestProcessor;
import com.ibm.wsspi.webcontainer.WCCustomProperties;
import com.ibm.wsspi.webcontainer.logging.LoggerFactory;
import com.ibm.wsspi.webcontainer.servlet.IExtendedRequest;
import com.ibm.wsspi.webcontainer.servlet.IExtendedResponse;
import com.ibm.wsspi.webcontainer.servlet.IServletWrapper;
import com.ibm.wsspi.webcontainer.webapp.IWebAppDispatcherContext;
/**
 * Dispatch context represents data stored for the execution of
 * a request to a dispatcher.
 *
 * @author Spike Washburn, IBM
 */

@SuppressWarnings("unchecked")
public abstract class WebAppDispatcherContext implements Cloneable, IWebAppDispatcherContext
{
    private static TraceNLS nls = TraceNLS.getTraceNLS(WebAppDispatcherContext.class, "com.ibm.ws.webcontainer.resources.Messages");
    protected static Logger logger = LoggerFactory.getInstance().getLogger("com.ibm.ws.webcontainer.webapp");
    private static final String CLASS_NAME="com.ibm.ws.webcontainer.webapp.WebAppDispatcherContext";

    // objects requiring cloning
    //==========================
    protected UnsynchronizedStack<IServletWrapper> _servletReferenceStack = new UnsynchronizedStack();
    private UnsynchronizedStack _exceptionStack = new UnsynchronizedStack();
    protected IExtendedRequest _request;
    protected SRTRequestContext reqContext = null;
    protected WebAppDispatcherContext parentContext = null;

    // instance variables not needing cloning
    //=======================================
    private String relativeUri;
    protected String _servletPath;
    protected String _pathInfo;
    private String _requestUri;
    private String _contextPath=null;
    private String queryString = null;
    protected boolean _useParent = false;
    private boolean enforceSecurity = true;
    private String decodedReqUri;           //280335
    //=======================================

    private static final boolean redirectWithPathInfo = WCCustomProperties.REDIRECT_WITH_PATH_INFO;

    private static final boolean removeServletPathSlash = WCCustomProperties.REMOVE_TRAILING_SERVLET_PATH_SLASH;
    private boolean possibleSlashStarMapping = true; //PK39337

    //other object not needing Cloning
    //========================
    private WebApp _webApp;           //PH08872
    private DispatcherType dispatcherType = DispatcherType.REQUEST;
    private boolean isNamedDispatcher = false;
    
    public void setNamedDispatcher(boolean b){
        isNamedDispatcher = b;
    }
    
    public boolean isNamedDispatcher(){
        return isNamedDispatcher;
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.webcontainer.webapp.IWebAppDispatcherContext#setParentContext(com.ibm.ws.webcontainer.webapp.WebAppDispatcherContext)
     */
    public void setParentContext(WebAppDispatcherContext parent){
        parentContext = parent;
    }
    /* (non-Javadoc)
     * @see com.ibm.ws.webcontainer.webapp.IWebAppDispatcherContext#finish()
     */
    public void finish()
    {
        resetObject();
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.webcontainer.webapp.IWebAppDispatcherContext#setUseParent(boolean)
     */
    public void setUseParent(boolean useParent){
        _useParent = useParent;
    }

    //TODO: Remove
    /* (non-Javadoc)
     * @see com.ibm.ws.webcontainer.webapp.IWebAppDispatcherContext#getWebApp()
     */
    public WebApp getWebApp()
    {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME,"getWebApp", "webapp -> "+ _webApp +" ,this -> " + this);
        }
        
        return _webApp;
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.webcontainer.webapp.IWebAppDispatcherContext#initForNextDispatch(com.ibm.ws.webcontainer.core.Request)
     */
    public void initForNextDispatch(IExtendedRequest req) //, WebAppRequestDispatcherInfo di)
    {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME,"initForNextDispatch", "req -> "+ req +" ,this -> " + this);
        }
        
        _request = req;
        if (req != null)
        {
            reqContext = ((SRTServletRequest)_request).getRequestContext();
        }
    }

    /* (non-Javadoc)
     * Push this dispatch context into the domain of of a new ServletReference.
     */
    public void pushServletReference(IServletWrapper wrapper)
    {
        _servletReferenceStack.push(wrapper);
    }

    /* (non-Javadoc)
     * Pop the current ServletReference.
     */
    public void popServletReference()
    {
        if (_servletReferenceStack.size()>1)
            _servletReferenceStack.pop();
    }

    /* (non-Javadoc)
     * Get the ServletReference that this response is currently excuting in the context of.
     */
    public IServletWrapper getCurrentServletReference()
    {
        return (IServletWrapper) _servletReferenceStack.peek();
    }

    /* (non-Javadoc)
     * Get the ServletReference that this response is currently excuting in the context of.
     */
    public Throwable getCurrentException()
    {
        if (_exceptionStack.size() == 0)
            return null; 
        return (Throwable) _exceptionStack.peek();
    }


    /* (non-Javadoc)
     * Get the WebAppRequest.
     */
    public ServletRequest getRequest()
    {
        return _request;
    }

    // d151464 - appears that this is no longer used
    //public String getProxiedRequestURI()
    //{
    //    return _request.getProxiedRequestURI();
    //}

    /* (non-Javadoc)
     * Get the WebAppResponse.
     */
    public IExtendedResponse getResponse()
    {
        return _request.getResponse();
    }

    void resetObject()
    {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  
            logger.logp(Level.FINE, CLASS_NAME,"resetObject", "this ->" + this);
        }
        this._servletReferenceStack.clear();
        this._exceptionStack.clear();
        this._pathInfo = null;
        this._servletPath = null;
        this._webApp = null;
        this.relativeUri = null;
        this.queryString = null;
        this._requestUri = null;
        this.enforceSecurity = true;//PK17095
        this.dispatcherType = DispatcherType.REQUEST;
        this._contextPath = null;  //PI08569
    }

    private IHttpSessionContext getSessionContext()
    {
        return _webApp.getSessionContext();
    }

    //---session related----
    /*
     * @see HttpServletRequest#getRequestedSessionId()
     */
    /* (non-Javadoc)
     * @see com.ibm.ws.webcontainer.webapp.IWebAppDispatcherContext#getRequestedSessionId()
     */
    public String getRequestedSessionId()
    {
        return getSessionContext().getRequestedSessionId((HttpServletRequest) _request);
    }

    /*
     * @see HttpServletRequest#isRequestedSessionIdFromCookie()
     */
    /* (non-Javadoc)
     * @see com.ibm.ws.webcontainer.webapp.IWebAppDispatcherContext#isRequestedSessionIdFromCookie()
     */
    public boolean isRequestedSessionIdFromCookie()
    {
        return getSessionContext().isRequestedSessionIdFromCookie((HttpServletRequest) _request);
    }

    /*
     * @see HttpServletRequest#isRequestedSessionIdFromUrl()
     * @deprecated
     */
    /* (non-Javadoc)
     * @see com.ibm.ws.webcontainer.webapp.IWebAppDispatcherContext#isRequestedSessionIdFromUrl()
     */
    public boolean isRequestedSessionIdFromUrl()
    {
        return this.isRequestedSessionIdFromURL();
    }

    /*
     * @see HttpServletRequest#isRequestedSessionIdFromURL()
     */
    /* (non-Javadoc)
     * @see com.ibm.ws.webcontainer.webapp.IWebAppDispatcherContext#isRequestedSessionIdFromURL()
     */
    public boolean isRequestedSessionIdFromURL()
    {
        return getSessionContext().isRequestedSessionIdFromUrl((HttpServletRequest) _request);
    }

    /* (non-Javadoc)
     * called before request is served
     */
    public void sessionPreInvoke()
    {
        reqContext.sessionPreInvoke(_webApp);
    }

    /* (non-Javadoc)
     * called after request is served
     */
    public void sessionPostInvoke()
    {
        // CMD LIDB2557 always call, even if session is null
        try
        {
            reqContext.sessionPostInvoke();
        }
        catch (Throwable th)
        {
        }
    }

    /*
     * @see HttpServletResponse#encodeURL(String)
     */
    /* (non-Javadoc)
     * @see com.ibm.ws.webcontainer.webapp.IWebAppDispatcherContext#encodeURL(java.lang.String)
     */
    public String encodeURL(String arg0)
    {
        return reqContext.encodeURL(_webApp, (HttpServletRequest) _request, arg0);
    }
    /**
     * @see com.ibm.ws.webcontainer.webapp.IWebAppDispatcherContext#getRelativeUri()
     * Returns the relativeUri.
     * @return String
     */
    public String getRelativeUri()
    {
        if (parentContext!=null && _useParent)
            return parentContext.getRelativeUri();
        return relativeUri;
    }

    /**
     * @see com.ibm.ws.webcontainer.webapp.IWebAppDispatcherContext#setRelativeUri(java.lang.String)
     * Sets the relativeUri.
     * @param relativeUri The relativeUri to set
     */
    public void setRelativeUri(String relativeUri)
    {
        this.relativeUri = relativeUri;
    }

    //RequestContext methods
    /* (non-Javadoc)
     * @see com.ibm.ws.webcontainer.webapp.IWebAppDispatcherContext#getRealPath(java.lang.String)
     */
    public String getRealPath(String path)
    {
        return getWebApp().getRealPath(path);
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.webcontainer.webapp.IWebAppDispatcherContext#getRequestDispatcher(java.lang.String)
     */
    public RequestDispatcher getRequestDispatcher(String path)
    {
        if (path == null)
            return null;

        if (path.startsWith("/"))
        {
            // 113234 - indicate that security check is not needed -- not any more 126196
            return getWebApp().getFacade().getRequestDispatcher(path); // true
        }
        else
        {
            if (path.startsWith("./"))
            {
                path = path.substring(2);
            }

            // begin 108232: part 1
            // method did not handle relative includes in subdirectories of docroot.
            String newPath;
            String servletPath = (String) _request.getAttribute("javax.servlet.include.servlet_path");
            if (servletPath == null)
            {
                String pathInfo = getPathInfo();
                newPath = getServletPath();
                if (pathInfo != null)
                {
                    newPath += pathInfo;
                }
            }
            else
            {
                String pathInfo = (String) _request.getAttribute("javax.servlet.include.path_info");
                newPath = servletPath;
                if (pathInfo != null)
                {
                    newPath += pathInfo;
                }
            }
            // PK67895 Allow for null newPath
            if (newPath!=null) {
                int pathElementIndex = newPath.indexOf(';');
                if (pathElementIndex==-1)
                    newPath = newPath.substring(0, newPath.lastIndexOf("/"));
                else
                    newPath = newPath.substring(0, newPath.lastIndexOf("/",pathElementIndex)); //There has to be a '/' before the ; even in the case of a welcome page at the the default context "http://localhost:9080/;somePathElement"
                path = newPath + '/' + path;
            } else {
                path = '/' + path;
            }

            // 113234 - indicate that security check is not needed --- not any more --Defect 126196
            return getWebApp().getFacade().getRequestDispatcher(path); // true
            // end 108232: part 1

        }
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.webcontainer.webapp.IWebAppDispatcherContext#getUserPrincipal()
     */
    public abstract java.security.Principal getUserPrincipal();

    /* (non-Javadoc)
     * @see com.ibm.ws.webcontainer.webapp.IWebAppDispatcherContext#isUserInRole(java.lang.String, javax.servlet.http.HttpServletRequest)
     */
    public abstract boolean isUserInRole(String role, HttpServletRequest req);

    /* (non-Javadoc)
     * @see com.ibm.ws.webcontainer.webapp.IWebAppDispatcherContext#getPathInfo()
     */
    public String getPathInfo()
    {
        if (parentContext!=null && _useParent)
            return parentContext.getPathInfo();
        return _pathInfo;
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.webcontainer.webapp.IWebAppDispatcherContext#setPathInfo(java.lang.String)
     */
    public void setPathInfo(String pathInfo)
    {
        //System.out.println("Setting pathinfo = "+pathInfo);
        _pathInfo = pathInfo;
    }
    
    // implemented by Servlet 4
    public String getMappingValue() {
        return null;
    }
    
    /* (non-Javadoc)
     * @see com.ibm.ws.webcontainer.webapp.IWebAppDispatcherContext#getRequestURI()
     */
    public String getRequestURI()
    {
        //System.out.println("SRTRequestContext: getRrequestURI() - "+_requestURI);
        if (parentContext!=null && _useParent)
            return parentContext.getRequestURI();
        return _requestUri;
    }

    @Override
    public String getOriginalRelativeURI(){
        WebAppDispatcherContext currentContext = this;
        WebAppDispatcherContext tempParentContext = parentContext;
        while (tempParentContext!=null){
            currentContext = tempParentContext;
            tempParentContext = tempParentContext.getParentContext();
        }
        return currentContext.getRelativeUri();
    }



    protected WebAppDispatcherContext getParentContext() {
        return this.parentContext;
    }
    /* (non-Javadoc)
     * @see com.ibm.ws.webcontainer.webapp.IWebAppDispatcherContext#getContextPath()
     */
    public String getContextPath()
    {
        if (parentContext!=null && _useParent)
            return parentContext.getContextPath();
        String cp =null;
        if (_contextPath!=null)
            cp=_contextPath;
        else
            cp = getWebApp().getContextPath();
        if (cp.equals("/"))
        {
            return "";
        }
        else
        {
            return cp;
        }
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.webcontainer.webapp.IWebAppDispatcherContext#getPathTranslated()
     */
    public String getPathTranslated()
    {
        String pathInfo = getPathInfo();

        if (pathInfo == null)
        {
            return null;
        }
        else
        {
            return getWebApp().getRealPath(pathInfo);
        }
    }

    //	public void setCurrentRequest(Request req)
    //	{
    //		_request = req;
    //		_servletPath = req.getServletPath();
    //		_pathInfo = req.getPathInfo();
    //		_requestURI = req.getRequestURI();
    //	}

    /* (non-Javadoc)
     * @see com.ibm.ws.webcontainer.webapp.IWebAppDispatcherContext#getServletPath()
     */
    public String getServletPath()
    {
        if (parentContext!=null && _useParent)
            return parentContext.getServletPath();
        return _servletPath;
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.webcontainer.webapp.IWebAppDispatcherContext#setServletPath(java.lang.String)
     */
    public void setServletPath(String servletPath)
    {
        //System.out.println("Setting servlet path = "+servletPath);
        _servletPath = servletPath;
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.webcontainer.webapp.IWebAppDispatcherContext#sendRedirect303(java.lang.String)
     */
    public void sendRedirect303(String location) throws IOException {
        sendRedirectWithStatusCode(location, HttpServletResponse.SC_SEE_OTHER);
    }
    /* (non-Javadoc)
     * @see com.ibm.ws.webcontainer.webapp.IWebAppDispatcherContext#sendRedirect(java.lang.String)
     */
    public void sendRedirect(String location) throws IOException {
        sendRedirectWithStatusCode(location, HttpServletResponse.SC_MOVED_TEMPORARILY);
    }
    // public void sendRedirect(String location) throws IOException 
    private void sendRedirectWithStatusCode(String location, int statusCode) throws IOException
    //PQ97429	
    {
        HttpServletResponse response = (HttpServletResponse) getResponse();
        if (!response.isCommitted())
        {
            // begin 133968: part 1
            if (location == null)
            {
                throw new IllegalArgumentException("Location cannot be null in javax.servlet.http.HttpServletResponse.sendRedirect(location)");
            }
            // end 133968: part 1

            // d128646 - we might not have a writer to flush at this point...make sure we do
            // PQ77141 overrides the above defect
//		try
//		{
//			response.getWriter();
//		}
//		catch (Throwable th)
//		{
//		}

            response.resetBuffer();

            location = convertRelativeURIToURL(location);

            // 115010 - begin - make header name begin with upper case
            response.setHeader("Location", location);
            // 115010 - end
            // PQ97429
            // response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
            response.setStatus(statusCode);
            // PQ97429	    

            //response.flushBuffer();  PK79143

        }
        else
        {
            throw new IllegalStateException();
        }
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.webcontainer.webapp.IWebAppDispatcherContext#sendError(int)
     */
    public void sendError(int sc) throws IOException
    {
        String message;
        RequestProcessor ref = getCurrentServletReference();
        if (ref != null)
        {
            sendError(sc, MessageFormat.format(nls.getString("[{0}].reported.an.error", "[{0}] reported an error"), new Object[] { ref.getName()}));
        }
        else
        {
            sendError(
                      sc,
                      MessageFormat.format(nls.getString("[{0}].reported.an.error", "[{0}] reported an error"), new Object[] { getWebApp().getConfiguration().getDisplayName()}));
        }
    }

    public void sendError(int sc, String message, boolean ignoreCommittedException) throws IOException
    {
        // LIDB1234.3 - throw exception if response already committed.
        if (getResponse().isCommitted())
        {
            if (!ignoreCommittedException) {
                throw new IllegalStateException("Response already committed.");
            }
            else if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                logger.logp(Level.FINE, CLASS_NAME, "sendError", "response is committed, but not throwing ISE");
            }
        }
        else {
            getResponse().resetBuffer();
            try
            {
                ((HttpServletResponse) getResponse()).setStatus(sc);
            }
            catch (IllegalStateException e)
            {
                //failed to set staus code. This could be caused by the servlet being included.
                com.ibm.wsspi.webcontainer.util.FFDCWrapper.processException(e, "com.ibm.ws.webcontainer.webapp.WebAppDispatcherResponse.sendError", "112", this);
            }
        }

        WebApp webapp = getWebApp();
        //PK69491 Start
        Object fileNotFound = getRequest().getAttribute ("com.ibm.ws.webcontainer.filter.filterproxyservletfilenotfound");
        if(fileNotFound != null) {
            getRequest().removeAttribute ("com.ibm.ws.webcontainer.filter.filterproxyservletfilenotfound");
            webapp.sendError((HttpServletRequest) _request, (HttpServletResponse) getResponse(), (ServletErrorReport)fileNotFound);
        }else{ //PK69491 End
            WebAppErrorReport error = null;
            error = new WebAppErrorReport(message);
            error.setErrorCode(sc);
            RequestProcessor ref = getCurrentServletReference();
            if (ref != null)
            {
                error.setTargetServletName(ref.getName());
            }

            getWebApp().sendError((HttpServletRequest) _request, (HttpServletResponse) getResponse(), error);
        } //PK69491
        getResponse().flushBuffer();	
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.webcontainer.webapp.IWebAppDispatcherContext#sendError(int, java.lang.String)
     */
    public void sendError(int sc, String message) throws IOException
    {
        sendError(sc,message,false);
    }

    //	/**
    //	 * Set the underlying response that this object proxies.
    //	 */
    //	public void setCurrentResponse(Response resp)
    //	{
    //		_response = resp;
    //	}

    /**
     * convert a relative URI to a full URL.
     */
    private String convertRelativeURIToURL(String relativeURI)
    {
        String location = null;
        if (relativeURI == null)
        {
            throw new IllegalStateException();
        }
        else
        {
            location = relativeURI;
            relativeURI = relativeURI.trim();
        }

        // If the passed URI contains "://" before the query string then it is either an
        // absolute URL or invalid and cannot be converted anyway
        int indexOfSchemeDelimiter = relativeURI.indexOf("://");
        if (indexOfSchemeDelimiter != -1)
        {
            int indexOfQueryString = relativeURI.indexOf('?');

            // If "://" occurs after '?' then it's part of the query string.
            // Otherwise return this absolute (or invalid) URL
            if ((indexOfQueryString == -1) || (indexOfSchemeDelimiter < indexOfQueryString))
            {
                return relativeURI;
            }
        }

        String urlScheme = _request.getScheme();		
        if (com.ibm.ws.webcontainer.osgi.WebContainer.getServletContainerSpecLevel() >= 31) {
            if (relativeURI.startsWith("//")) {
                StringBuffer url = new StringBuffer(urlScheme);
                url.append(":"); // slash slash will be added from the relativeURI
                url.append(relativeURI);
                return url.toString();
            }
        }

        String webAppRootURI = null;
        boolean relativeToRootURI = false;
        try
        {
            webAppRootURI = getWebApp().getContextPath().trim();
            if (webAppRootURI.startsWith("/") == false)
            {
                webAppRootURI = ("/" + webAppRootURI);
            }
        }
        catch (Exception ex)
        {
            com.ibm.wsspi.webcontainer.util.FFDCWrapper.processException(ex, "com.ibm.ws.webcontainer.webapp.WebAppDispatcherResponse.convertRelativeURIToURL", "184", this);
            webAppRootURI = "/";
        }

        try
        {
            relativeToRootURI = relativeURI.startsWith("/");

            // If relative URI begins with "./" (current level in URL hierarchy) then remove it
            if (relativeURI.startsWith("./"))
                relativeURI = relativeURI.substring(2);

            IExtendedRequest request = _request;

            // 112000 - begin - there was no reason to have two schemes for handling relative to root vs.
            // not relative to root so redid the section below

            // add the server info to the front of the url
            int urlPort = request.getServerPort();

            StringBuffer url = new StringBuffer(urlScheme);
            url.append("://");
            url.append(request.getServerName());

            // Add the port to the URL if it is not the default one for the scheme
            if (((urlScheme.equals("http") == true) && (urlPort != 80)) || ((urlScheme.equals("https") == true) && (urlPort != 443)))
            {
                url.append(":");
                url.append(urlPort);
            }

            String redirectURL = null;
            if (relativeToRootURI)
            {
                // 115010 - begin
                // if the relative URI begins with '/' and we're in sendRedirect compatibility mode, then make uri 
                // relative to the context root...otherwise, make it relative to the server (no context root)
                if (com.ibm.ws.webcontainer.util.WebContainerSystemProps.getSendRedirectCompatibilty())
                {
                    // append webapp context and relative URI
                    url.append(webAppRootURI);
                    url.append(relativeURI);
                }
                else
                {
                    url.append(relativeURI);
                }
                // 115010 - end
                redirectURL = url.toString();
            }
            else
            {
                // not relative to webapp context root, but relative to the presently
                // invoked URL

                String requestString = HttpUtils.getRequestURL((HttpServletRequest) request).toString();
                String pathInfo = request.getPathInfo();
                // start PI22830
                if(!webAppRootURI.equals("/") && request.getRequestURI().equals(webAppRootURI)){
                    requestString = requestString + "/";
                    if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
                        logger.logp(Level.FINE, CLASS_NAME,"convertRelativeURIToURL", "appended / to requestString --> " + requestString);
                } // end PI22830

                if ((pathInfo != null) && (pathInfo.length() > 0))
                {
                    if (!pathInfo.startsWith("/"))
                    {
                        pathInfo = "/" + pathInfo;
                    }
                    //start PK23779

                    if(redirectWithPathInfo || (com.ibm.ws.webcontainer.osgi.WebContainer.getServletContainerSpecLevel() >= 31)){
                        requestString = requestString.substring(0, requestString.lastIndexOf("/"));
                    }
                    else{
                        // Remove the PathInfo from the request
                        requestString = requestString.substring(0, requestString.lastIndexOf(pathInfo));
                    }
                    //end PK23779
                    redirectURL = requestString + "/" + relativeURI;
                }
                else
                {
                    redirectURL = requestString.substring(0, requestString.lastIndexOf('/') + 1) + relativeURI;
                }
            }
            if (redirectURL.indexOf("..") > -1)
            { // only normalize where required.. this is expensive.
                int skip = new String(urlScheme + "://").length();
                int split = redirectURL.indexOf("/", skip);
                String serverString = redirectURL.substring(0, split);
                String uri = redirectURL.substring(split);
                uri = getWebApp().normalize(uri);
                if (uri != null)
                {
                    return serverString + uri;
                }
            }
            else
            { // return url since it is already normalized.
                return redirectURL;
            }
            // 112000 - end
        }
        catch (Exception ex)
        {
            com.ibm.wsspi.webcontainer.util.FFDCWrapper.processException(ex, "com.ibm.ws.webcontainer.webapp.WebAppDispatcherResponse.convertRelativeURIToURL", "256", this);
        }

        // Could not convert
        logger.logp(Level.FINE, CLASS_NAME,"convertRelativeURIToURL", "could not convert [" + location + "]");
        return location;
    }

    public void callPage(String fileName, javax.servlet.http.HttpServletRequest hreq) throws IOException, ServletException
    {
        ServletContext sc = getWebApp().getContext(fileName);
        WebApp webapp = (WebApp) sc;

        String relativePath = fileName.substring(webapp.getContextPath().length());
        RequestDispatcher rd = sc.getRequestDispatcher(relativePath);
        rd.forward(hreq, getResponse());
    }

    public void _include(String fileName, HttpServletRequest request) throws IOException, ServletException
    {
        WebApp webApp = getWebApp();

        RequestDispatcher requestDispatcher = webApp.getRequestDispatcher(fileName);

        requestDispatcher.include(request, getResponse());
    }

    public void _forward(String fileName, HttpServletRequest request) throws IOException, ServletException
    {
        WebApp webApp = getWebApp();

        RequestDispatcher requestDispatcher = webApp.getRequestDispatcher(fileName);

        requestDispatcher.forward(request, getResponse());
    }
    /*
     * @see HttpServletResponse#encodeUrl(String)
     * @deprecated
     */

    /* (non-Javadoc)
     * @see com.ibm.ws.webcontainer.webapp.IWebAppDispatcherContext#isAutoRequestEncoding()
     */
    public boolean isAutoRequestEncoding()
    {
        return getWebApp().getConfiguration().isAutoResponseEncoding();
    }

    /**
     * @param string
     */
    public void setRequestURI(String path)
    {
        setRequestURI(path, false);
    }

    public void setRequestURI(String path, boolean encodeURI)
    {
        //Begin 255635
        if (path==null){
            _requestUri = path;
            return;
        }
        //End 255635
        int qMark = path.indexOf("?");
        if (qMark != -1)
        {
            queryString = path.substring(qMark + 1);
            _requestUri = path.substring(0, qMark);
        }
        else
            _requestUri = path;
        if (encodeURI) {
            try {
                _requestUri = (new URI(null,_requestUri,null)).toASCIIString();
            }
            catch (URISyntaxException use) {
                //We should never go here...
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable (Level.FINE)) {    //PI67942
                    logger.logp(Level.FINE, CLASS_NAME,"setRequestURI", "An URISyntaxException was thrown during URI encoding");
                }
            }
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable (Level.FINE)) {    //PI67942
                logger.logp(Level.FINE, CLASS_NAME,"setRequestURI", "encoding request URI, path -> "+ path +", encoded URI -> " + _requestUri);
            }
        }

    }

    /* (non-Javadoc)
     * @see com.ibm.ws.webcontainer.webapp.IWebAppDispatcherContext#setPathElements(java.lang.String, java.lang.String)
     * @param string
     * @param string2
     */
    public void setPathElements(String servletPath, String pathInfo)
    {
        ((SRTServletRequest)_request).resetPathElements();

        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable (Level.FINE)) {    //PI67942
            logger.logp(Level.FINE, CLASS_NAME,"setPathElements", "servletPath = " + servletPath +", pathInfo = " + pathInfo +" : this = " + this);
        }
        //PK39337 - start
        if (removeServletPathSlash) {

            boolean hasSlashStar = false;
            boolean isPossible = isPossibleSlashStarMapping();
            if (isPossible) {
                hasSlashStar = hasSlashStarMapping();
            }

            if(hasSlashStar) {
                this._servletPath = "";
                if (servletPath == null) {
                    this._pathInfo = pathInfo;
                } else if (pathInfo == null) {
                    this._pathInfo = servletPath;
                } else {
                    this._pathInfo = servletPath + pathInfo;
                }
            }
            else if (isPossible) {
                //this means that it probably used the mapping '/'
                //so the servlet path can't be null or and empty string.
                if (servletPath == null || servletPath.equals("")) {
                    this._servletPath = pathInfo;
                    this._pathInfo = null;
                }
                else {
                    this._servletPath = servletPath;
                    this._pathInfo = pathInfo;
                }
            }
            else {
                this._servletPath = servletPath;
                this._pathInfo = pathInfo;
            }

            if ((_servletPath != null) && (_servletPath.length() > 1 && _servletPath.endsWith("/"))) {
                this._servletPath = _servletPath.substring(0, (_servletPath.length() - 1));
                if (_pathInfo ==null)
                    this._pathInfo = "/";
                else
                    this._pathInfo = "/"+ _pathInfo;
            }       		
        }
        //PK39337 - end
        else if (servletPath.length()==1 && servletPath.charAt(0)=='/' && !Boolean.valueOf(WCCustomProperties.SERVLET_PATH_FOR_DEFAULT_MAPPING).booleanValue()) {
            this._servletPath = "";
            if (pathInfo ==null)
                this._pathInfo = "/";
            else
                this._pathInfo = "/"+ pathInfo;
        }
        else {
            this._servletPath = servletPath;
            this._pathInfo = pathInfo;
        }

        this.relativeUri = _servletPath;

        if (_pathInfo != null)
            relativeUri += _pathInfo;
        
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable (Level.FINE)) { 
            logger.logp(Level.FINE, CLASS_NAME,"setPathElements", "returns with servletPath = " + _servletPath +", pathInfo = " + _pathInfo +" : this = " + this);
        }
        
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.webcontainer.webapp.IWebAppDispatcherContext#isForward()
     */
    public boolean isForward()
    {
        return (this.dispatcherType == DispatcherType.FORWARD);
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.webcontainer.webapp.IWebAppDispatcherContext#isInclude()
     */
    public boolean isInclude()
    {
        return (this.dispatcherType == DispatcherType.INCLUDE);
    }
    
    public boolean isAsync() 
    {
        return (this.dispatcherType == DispatcherType.ASYNC);
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.webcontainer.webapp.IWebAppDispatcherContext#getQueryString()
     */
    public String getQueryString()
    {
        if (parentContext!=null && _useParent)
            return parentContext.getQueryString();
        return queryString;
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.webcontainer.webapp.IWebAppDispatcherContext#setQueryString(java.lang.String)
     */
    public void setQueryString(String string)
    {
        queryString = string;
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.webcontainer.webapp.IWebAppDispatcherContext#pushException(java.lang.Throwable)
     */
    public void pushException(Throwable th)
    {
        _exceptionStack.push(th);
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.webcontainer.webapp.IWebAppDispatcherContext#setWebApp(com.ibm.ws.webcontainer.webapp.WebApp)
     */
    public void setWebApp(WebApp app)
    {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME,"setWebApp", "webapp -> "+ app +" ,this -> " + this);
        }
        
        this._webApp = app;
        if (reqContext != null)
        {
            reqContext.setCurrWebAppBoundary(app);
        }
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.webcontainer.webapp.IWebAppDispatcherContext#setContextPath(java.lang.String)
     */
    public void setContextPath(String _contextPath){
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME,"setContextPath", "contextPath -> "+ _contextPath +" ,this -> " + this);
        }
        this._contextPath = _contextPath;
    }

    /* (non-Javadoc)
     * @return Returns the enforceSecurity.
     */
    public boolean isEnforceSecurity() {
        return enforceSecurity;
    }
    /* (non-Javadoc)
     * @param enforceSecurity The enforceSecurity to set.
     */
    public void setEnforceSecurity(boolean enforceSecurity) {
        this.enforceSecurity = enforceSecurity;
    }

    /* (non-Javadoc)
     * @param decodedURI The decoded request uri
     */
    public void setDecodedReqUri(String decodedURI) {
        this.decodedReqUri = decodedURI;

    }
    /* (non-Javadoc)
     * @return Returns the decodedReqUri.
     */
    public String getDecodedReqUri() {
        return decodedReqUri;
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.webcontainer.webapp.IWebAppDispatcherContext#isSecurityEnabledForApplication()
     */
    public boolean isSecurityEnabledForApplication(){
        //since security enabled/disabled will restart the apps (is this only for the feature set or for app security too???)
        return _webApp.isSecurityEnabledForApplication();
    }



    // begin PK17095
    /* (non-Javadoc)
     * @see com.ibm.ws.webcontainer.webapp.IWebAppDispatcherContext#dumpDispatchContextHierarchy()
     */
    public void dumpDispatchContextHierarchy(){
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)){
            logger.logp(Level.FINE, CLASS_NAME,"dumpDispatchContextHierarchy", "current context -->" + this);
            WebAppDispatcherContext tmpParentContext = parentContext;
            while (tmpParentContext != null){
                logger.logp(Level.FINE, CLASS_NAME,"dumpDispatchContextHierarchy", "parent context -->" + tmpParentContext);
                tmpParentContext = tmpParentContext.parentContext;
            }
        }
    }
    // end PK17095

    /* (non-Javadoc)
     * @see com.ibm.ws.webcontainer.webapp.IWebAppDispatcherContext#clone(com.ibm.ws.webcontainer.srt.SRTServletRequest, com.ibm.ws.webcontainer.srt.SRTRequestContext)
     */
    public Object clone(SRTServletRequest clonedRequest, SRTRequestContext clonedReqContext) throws CloneNotSupportedException
    {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  
            logger.logp(Level.FINE, CLASS_NAME,"clone", "clone entry");
        }

        WebAppDispatcherContext _cloneDispatchContext = (WebAppDispatcherContext) super.clone();
        _cloneDispatchContext._request=clonedRequest;
        _cloneDispatchContext.reqContext=clonedReqContext;

        if(_servletReferenceStack != null){
            _cloneDispatchContext._servletReferenceStack=(UnsynchronizedStack)_servletReferenceStack.clone();
        }
        if(_exceptionStack != null){
            _cloneDispatchContext._exceptionStack=(UnsynchronizedStack)_exceptionStack.clone();
        }
        if (this.parentContext != null){
            _cloneDispatchContext.parentContext=(WebAppDispatcherContext)parentContext.clone(clonedRequest,clonedReqContext);
        }
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  
            logger.logp(Level.FINE, CLASS_NAME,"clone", "clone exit original -->" + this +" cloned -->" + _cloneDispatchContext);
        }

        return _cloneDispatchContext;
    }

//  PK39337 start
    public boolean hasSlashStarMapping() {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINEST)){
            logger.entering (CLASS_NAME, "hasSlashStarMapping");
        }
        WebAppConfiguration webAppConfig = null;		
        WebApp webApp = this._webApp;

        if (webApp != null) {
            webAppConfig = webApp.getConfiguration();			
        }		
        if (webAppConfig != null) {
            Map<String,List<String>> mappings = webAppConfig.getServletMappings();
            if (mappings != null) {
                for (List<String> list : mappings.values()) {
                    for (String urlPattern : list) {
                        if (urlPattern != null && ("/*").equals(urlPattern)) {
                            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINEST)){
                                logger.exiting (CLASS_NAME, "hasSlashStarMapping: true");
                            }
                            return true;
                        }
                    }				
                }												
            }
        }
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINEST)){
            logger.exiting (CLASS_NAME, "hasSlashStarMapping");
        }
        return false;
    }

    public void setPossibleSlashStarMapping(boolean isPossible) {
        this.possibleSlashStarMapping = isPossible;
    }

    public boolean isPossibleSlashStarMapping() {
        return possibleSlashStarMapping;
    }
    //PK39337 end


    public void clearAndPushServletReference(IServletWrapper servletWrapper) {
        this._servletReferenceStack.clear();
        pushServletReference(servletWrapper);
    }
    public void setDispatcherType(DispatcherType dispatcherType) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
            logger.logp(Level.FINE, CLASS_NAME,"setDispatcherType","dispatcherType->"+dispatcherType);
        this.dispatcherType = dispatcherType;
    }
    public DispatcherType getDispatcherType() {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
            logger.logp(Level.FINE, CLASS_NAME,"getDispatcherType","dispatcherType->"+dispatcherType);
        return dispatcherType;
    }


}

