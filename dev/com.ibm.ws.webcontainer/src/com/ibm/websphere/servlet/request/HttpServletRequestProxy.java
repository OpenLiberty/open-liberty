/*******************************************************************************
 * Copyright (c) 1997, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.servlet.request;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;

/**
 * 
 * 
 * @deprecated since WAS V6.0
 * Use the HttpServletRequestWrapper class instead.
 * 
* Proxies function invocations to an underlying HttpServletRequest.
* Subclasses of this class can be created that overload or enhance the
* functionality of a server-provided HttpServletRequest.
*
* <H3>Using the proxied request:</H3>
* <OL>
* <LI>Subclass this class and overload any desired functions.
* <LI>During the servlet's service method, create an instance of
*  the enhanced request using the original request from the server
*  as the proxied request.
* <LI>Forward the enhanced request to another servlet for processing instead
* of the original request that was provided by the server.
* </OL>
*
* <h3>Sample subclass (<I>overloads the request's InputStream</I>)</H3>
* <pre>
* 
* // This enhanced request will force the request to be a POST request.
* // This request POST data input will be read from a specified file.
* public class PostedFileRequest extends HttpServletRequestProxy{
*    private HttpServletRequest _request;
*    private File _file;
*    public PostedFileRequest(File f, HttpServletRequest req){
*      _file =f;
*       _request = req;
*    }
*    protected HttpServletRequest getProxiedHttpServletRequest(){
*       return _request;
*    }
*    //overload request functionality
*    public ServletInputStream getInputStream() throws IOException{
*       return new ServletInputStreamAdapter(new FileInputStream(_file));
*    }
*    public BufferedReader getReader() throws IOException{
*       return new BufferedReader(getInputStream());
*    }
*    public String getMethod(){
*       //force the HTTP method to be POST.
*       return "POST";
*    }
* }
* </pre>
*
* <h3>Using the enhanced request subclass transparently in a servlet </h3>
* <pre>
* //This servlet posts a data file as a request to another servlet.
* public class PostGeneratorServlet extends HttpServlet{
*    public void service HttpServletRequest req, HttpServletResponse resp){
*       req = new PostedFileRequest(req, new File(request.getPathTranslated()));
*       //forward the enhanced request to be used transparently by another servlet.
*       getServletContext().getRequestDispatcher("/postHandlerServlet").forward(req, resp);
*    }
* }
* </pre>
* 
* @ibm-api
*/
public abstract class HttpServletRequestProxy implements HttpServletRequest
{
 /**
  * Get the request that this object is supposed to proxy.
  */
 abstract protected HttpServletRequest getProxiedHttpServletRequest();

 //----------- HttpServletRequest interface ----------------------------//
 public String getAuthType()
 {
     return getProxiedHttpServletRequest().getAuthType();
 }
 public Cookie[] getCookies()
 {
     return getProxiedHttpServletRequest().getCookies();
 }
 public int getIntHeader(String name)
 {
     return getProxiedHttpServletRequest().getIntHeader(name);
 }
 public long getDateHeader(String name)
 {
     return getProxiedHttpServletRequest().getDateHeader(name);
 }
 public String getHeader(String name)
 {
     return getProxiedHttpServletRequest().getHeader(name);
 }
 @SuppressWarnings("unchecked")
 public Enumeration getHeaderNames()
 {
     return getProxiedHttpServletRequest().getHeaderNames();
 }
 public String getQueryString()
 {
     return getProxiedHttpServletRequest().getQueryString();
 }
 public String getMethod()
 {
     return getProxiedHttpServletRequest().getMethod();
 }
 public String getPathInfo()
 {
     return getProxiedHttpServletRequest().getPathInfo();
 }
 public String getPathTranslated()
 {
     return getProxiedHttpServletRequest().getPathTranslated();
 }
 public String getServletPath()
 {
     return getProxiedHttpServletRequest().getServletPath();
 }
 public String getRemoteUser()
 {
     return getProxiedHttpServletRequest().getRemoteUser();
 }
 public String getRequestedSessionId()
 {
     return getProxiedHttpServletRequest().getRequestedSessionId();
 }
 public String getRequestURI()
 {
     return getProxiedHttpServletRequest().getRequestURI();
 }
 public boolean isRequestedSessionIdFromCookie()
 {
     return getProxiedHttpServletRequest().isRequestedSessionIdFromCookie();
 }
 public HttpSession getSession(boolean create)
 {
     return getProxiedHttpServletRequest().getSession(create);
 }
 public HttpSession getSession()
 {
     return getProxiedHttpServletRequest().getSession();
 }
 public boolean isRequestedSessionIdValid()
 {
     return getProxiedHttpServletRequest().isRequestedSessionIdValid();
 }
 public boolean isRequestedSessionIdFromURL()
 {
     return getProxiedHttpServletRequest().isRequestedSessionIdFromURL();
 }
 public boolean isRequestedSessionIdFromUrl()
 {
     return getProxiedHttpServletRequest().isRequestedSessionIdFromUrl();
 }
 @SuppressWarnings("unchecked")
 public Enumeration getHeaders(String name)
 {
     return getProxiedHttpServletRequest().getHeaders(name);
 }
 public String getContextPath()
 {
     return getProxiedHttpServletRequest().getContextPath();
 }
 public boolean isUserInRole(String role)
 {
     return getProxiedHttpServletRequest().isUserInRole(role);
 }
 public java.security.Principal getUserPrincipal()
 {
     return getProxiedHttpServletRequest().getUserPrincipal();
 }

 // LIDB1234.4 - method below added for Servlet 2.3
 public StringBuffer getRequestURL()
 {
     return getProxiedHttpServletRequest().getRequestURL();
 }

 //----------- ServletRequest interface ----------------------------//
 public Object getAttribute(String name)
 {
     return getProxiedHttpServletRequest().getAttribute(name);
 }
 @SuppressWarnings("unchecked")
 public Enumeration getAttributeNames()
 {
     return getProxiedHttpServletRequest().getAttributeNames();
 }
 public ServletInputStream getInputStream() throws IOException{
     return getProxiedHttpServletRequest().getInputStream();
 }
 public String getCharacterEncoding()
 {
     return getProxiedHttpServletRequest().getCharacterEncoding();
 }
 public int getContentLength()
 {
     return getProxiedHttpServletRequest().getContentLength();
 }
 public String getContentType()
 {
     return getProxiedHttpServletRequest().getContentType();
 }
 public String getProtocol()
 {
     return getProxiedHttpServletRequest().getProtocol();
 }
 public String getParameter(String name)
 {
     return getProxiedHttpServletRequest().getParameter(name);
 }
 @SuppressWarnings("unchecked")
 public Enumeration getParameterNames()
 {
     return getProxiedHttpServletRequest().getParameterNames();
 }
 public String[] getParameterValues(String name)
 {
     return getProxiedHttpServletRequest().getParameterValues(name);
 }
 public String getScheme()
 {
     return getProxiedHttpServletRequest().getScheme();
 }
 public String getServerName()
 {
     return getProxiedHttpServletRequest().getServerName();
 }
 public int getServerPort()
 {
     return getProxiedHttpServletRequest().getServerPort();
 }
 public String getRealPath(String path)
 {
     return getProxiedHttpServletRequest().getRealPath(path);
 }
 public BufferedReader getReader() throws IOException {
     return getProxiedHttpServletRequest().getReader();
 }
 public String getRemoteAddr()
 {
     return getProxiedHttpServletRequest().getRemoteAddr();
 }
 public String getRemoteHost()
 {
     return getProxiedHttpServletRequest().getRemoteHost();
 }
 public void setAttribute(String key, Object o)
 {
     getProxiedHttpServletRequest().setAttribute(key, o);
 }
 public void removeAttribute(String name)
 {
     getProxiedHttpServletRequest().removeAttribute(name);
 }
 public Locale getLocale()
 {
     return getProxiedHttpServletRequest().getLocale();
 }
 @SuppressWarnings("unchecked")
 public Enumeration getLocales()
 {
     return getProxiedHttpServletRequest().getLocales();
 }
 public boolean isSecure()
 {
     return getProxiedHttpServletRequest().isSecure();
 }
 public RequestDispatcher getRequestDispatcher(String path)
 {
     return getProxiedHttpServletRequest().getRequestDispatcher(path);
 }
 
 public ServletRequest getRequest()
 {
     return getProxiedHttpServletRequest();
 }

 // LIDB1234.4 - two methods below added for Servlet 2.3
 @SuppressWarnings("unchecked")
 public Map getParameterMap()
 {
     return getProxiedHttpServletRequest().getParameterMap();
 }

 public void setCharacterEncoding(String encoding) throws UnsupportedEncodingException
 {
     getProxiedHttpServletRequest().setCharacterEncoding(encoding);
 }
}

