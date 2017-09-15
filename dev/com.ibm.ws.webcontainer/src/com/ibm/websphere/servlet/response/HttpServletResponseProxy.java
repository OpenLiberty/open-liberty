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
package com.ibm.websphere.servlet.response;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import java.util.Locale;

/**
 * @ibm-api
 * 
 * 
 * @deprecated since WAS V6.0
 * Use the HttpServletResponseWrapper class instead.
 * 
	* Proxies function invocations to an underlying HttpServletResponse.
	* Subclasses of this class can be created that overload or enhance the
	* functionality of a server-provided HttpServletResponse.
	*
	*
	* <h3>Using the proxied response:</h3>
	* <OL>
	* <LI>Subclass this class and overload any desired functions.
	* <LI>During the servlet's service method, create an instance of
	*  the enhanced response using the original response from the server
	*  as the proxied response.
	* <LI>Forward the enhanced response to another servlet for processing instead
	* of the original response that was provided by the server.
	* </OL>
	*
	* <h3>Sample subclass (<I>overloads the response's OutputStream</I>)</H3>
	* <pre>
	* 
	* //The data written to this response will be saved to the specified file.
	* public class FileOutputResponse extends HttpServletResponseProxy{
	*    private HttpServletResponse _response;
	*    private File _file;
	*    public FileOutputResponse(File f, HttpServletResponse resp){
	*      _file = f;
	*       _response = resp;
	*    }
	*    protected HttpServletResponse getProxiedHttpServletResponse(){
	*       return _response;
	*    }
	*    //overload response functionality
	*    public ServletOutputStream getOutputStream() throws IOException{
	*       return new ServletOutputStreamAdapter(new FileOutputStream(_file));
	*    }
	*    public PrintWriter getWriter() throws IOException{
	*       return new PrintWriter(getOutputStream());
	*    }
	* }
	* </pre>
	*
	* <h3>Using the enhanced response subclass transparently in a servlet</h3>
	* <pre>
	* //This servlet will store the response of another servlet to a file.
	* public class SaveResponseToFileServlet extends HttpServlet{
	*    public void service(HttpServletRequest req, HttpServletResponse resp){
	*       resp = new FileOutputResponse(req, new File("/tmp/response.txt"));
	*
	*       //store the response of SnoopServlet to the response.txt file.
	*       getServletContext().getRequestDispatcher("/servlet/SnoopServlet").forward(req, resp);
	*    }
	* }
	* </pre>
	*
*/
public abstract class HttpServletResponseProxy implements HttpServletResponse
{
 /**
  * Get the response that this object is supposed to proxy.
  */
 abstract protected HttpServletResponse getProxiedHttpServletResponse();

 //------------- HttpServletResponse ------------------------//
 public void addCookie(Cookie cookie)
 {
     getProxiedHttpServletResponse().addCookie(cookie);
 }
 public boolean containsHeader(String name)
 {
     return getProxiedHttpServletResponse().containsHeader(name);
 }
 public String encodeRedirectUrl(String url)
 {
     return getProxiedHttpServletResponse().encodeRedirectUrl(url);
 }
 public String encodeURL(String url)
 {
     return getProxiedHttpServletResponse().encodeURL(url);
 }
 public String encodeRedirectURL(String url)
 {
     return getProxiedHttpServletResponse().encodeRedirectURL(url);
 }
 public String encodeUrl(String url)
 {
     return getProxiedHttpServletResponse().encodeUrl(url);
 }
 public void setDateHeader(String name, long date)
 {
     getProxiedHttpServletResponse().setDateHeader(name, date);
 }
 public void sendError(int sc) throws IOException{
     getProxiedHttpServletResponse().sendError(sc);
 }
 public void sendError(int sc, String msg) throws IOException{
     getProxiedHttpServletResponse().sendError(sc, msg);
 }
 public void sendRedirect(String location) throws IOException{
     getProxiedHttpServletResponse().sendRedirect(location);
 }
 public void setStatus(int sc)
 {
     getProxiedHttpServletResponse().setStatus(sc);
 }
 public void setStatus(int sc, String sm)
 {
     getProxiedHttpServletResponse().setStatus(sc,sm);
 }
 public void setHeader(String name, String value)
 {
     getProxiedHttpServletResponse().setHeader(name, value);
 }
 public void setIntHeader(String name, int value)
 {
     getProxiedHttpServletResponse().setIntHeader(name, value);
 }
 public void addIntHeader(String name, int value)
 {
     getProxiedHttpServletResponse().addIntHeader(name,value);
 }
 public void addHeader(String name, String value)
 {
     getProxiedHttpServletResponse().addHeader(name,value);
 }
 public void addDateHeader(String name, long date)
 {
     getProxiedHttpServletResponse().addDateHeader(name,date);
 }
 //------------- ServletResponse ------------------------//
 public String getCharacterEncoding()
 {
     return getProxiedHttpServletResponse().getCharacterEncoding();
 }
 public ServletOutputStream getOutputStream() throws IOException{
     return getProxiedHttpServletResponse().getOutputStream();
 }
 public PrintWriter getWriter() throws IOException{
     return getProxiedHttpServletResponse().getWriter();
 }
 public void setContentLength(int len)
 {
     getProxiedHttpServletResponse().setContentLength(len);
 }
 public void setContentType(String type)
 {
     getProxiedHttpServletResponse().setContentType(type);
 }
 public void setBufferSize(int size)
 {
     getProxiedHttpServletResponse().setBufferSize(size);
 }
 public int getBufferSize()
 {
     return getProxiedHttpServletResponse().getBufferSize();
 }
 public void flushBuffer() throws IOException{
     getProxiedHttpServletResponse().flushBuffer();
 }
 public boolean isCommitted()
 {
     return getProxiedHttpServletResponse().isCommitted();
 }
 public void reset()
 {
     getProxiedHttpServletResponse().reset();
 }
 public void setLocale(Locale loc)
 {
     getProxiedHttpServletResponse().setLocale(loc);
 }
 public Locale getLocale()
 {
     return getProxiedHttpServletResponse().getLocale();
 }

 // LIDB1234.3 - added method below
 /**
  * Clears the content of the underlying buffer in the response without clearing
  * headers or status code.
  *
  * @throws IllegalStateException if the response has already been committed
  *
  * @since 2.3
  */
 public void resetBuffer()
 {
     getProxiedHttpServletResponse().resetBuffer();
 }
 
 public ServletResponse getResponse() 
 {
     return getProxiedHttpServletResponse();
 }
}
