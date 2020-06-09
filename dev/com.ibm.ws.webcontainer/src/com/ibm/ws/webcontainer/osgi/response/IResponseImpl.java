/*******************************************************************************
 * Copyright (c) 2010, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.osgi.response;

import java.io.IOException;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.servlet.request.IRequest;
import com.ibm.websphere.servlet.response.IResponse;
import com.ibm.ws.http.channel.outstream.HttpOutputStreamConnectWeb;
import com.ibm.ws.webcontainer.osgi.osgi.WebContainerConstants;
import com.ibm.wsspi.http.HttpCookie;
import com.ibm.wsspi.http.HttpInboundConnection;
import com.ibm.wsspi.http.HttpResponse;
import com.ibm.wsspi.webcontainer.WebContainerRequestState;
import com.ibm.wsspi.webcontainer.util.WrappingEnumeration;

/**
 * Implementation of a servlet response wrapping the HTTP dispatcher provided
 * response message.
 */
public class IResponseImpl implements IResponse
{
  private HttpInboundConnection conn = null;
  protected IRequest request = null;
  protected HttpResponse response = null;
  private boolean allocateDirect = false;
  protected WCOutputStream outStream = null;
  
  private static final TraceComponent tc = Tr.register(IResponseImpl.class, WebContainerConstants.TR_GROUP, WebContainerConstants.NLS_PROPS);

  /**
   * Constructor.
   * 
   * @param req
   * @param connection
   */
  public IResponseImpl(IRequest req, HttpInboundConnection connection)
  {
    this.conn = connection;
    this.request = req;
    this.response = conn.getResponse();
    this.allocateDirect = false;

  }  
  
  public void addCookie(Cookie cookie)
  {
    String methodName = "addCookie";
    String cookieName = cookie.getName();
    HttpCookie hc = new HttpCookie(cookieName, cookie.getValue());
    hc.setPath(cookie.getPath());
    hc.setVersion(cookie.getVersion());
    hc.setComment(cookie.getComment());
    hc.setDomain(cookie.getDomain());
    hc.setMaxAge(cookie.getMaxAge());
    hc.setSecure(cookie.getSecure());
    hc.setHttpOnly(cookie.isHttpOnly());

    /*
     * Check to see if the WebContainerRequestState has an attribute defined for 
     * the Cookie that is being added.
     *
     * If the attribute is not recognized by the Channel Framework then it is ignored.
     *
     * Current support by the Channel Framework is for the SameSite Cookie Attribute.
     */
    WebContainerRequestState requestState = WebContainerRequestState.getInstance(false);
    if (requestState != null) {
        String cookieAttributes = requestState.getCookieAttributes(cookieName);
        if (cookieAttributes != null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())  {
                Tr.debug(tc, methodName, "cookieName: " + cookieName + " cookieAttribute: " + cookieAttributes);
            }

            if(cookieAttributes.contains("=")) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())  {
                    Tr.debug(tc, methodName, "Setting the cookieAttribute on the HttpCookie");
                }

                String[] attribute = cookieAttributes.split("=");
                hc.setAttribute(attribute[0], attribute[1]);
            }

            // Remove the Cookie attribute that was used as it is no longer needed.
            requestState.removeCookieAttributes(cookieName);
        }
    }

    this.response.addCookie(hc);
  }

  public void addDateHeader(String name, long t)
  {
    this.response.addHeader(name, this.conn.getDateFormatter().getRFC1123Time(new Date(t)));
  }

  public void addHeader(String name, String value)
  {
    this.response.addHeader(name, value);
  }

  public void addHeader(byte[] name, byte[] value)
  {
    this.response.addHeader(new String(name), new String(value));
  }

  public void addIntHeader(String name, int i)
  {
    this.response.addHeader(name, Integer.toString(i));
  }

  public void clearHeaders()
  {
    this.response.removeAllHeaders();
  }

  public boolean containsHeader(String name)
  {
    return (null != this.response.getHeader(name));
  }

  public boolean containsHeader(byte[] name)
  {
    return containsHeader(new String(name));
  }

  public void flushBufferedContent()
  {
    // bbOS.flushWriteBuffer();
  }

  public boolean getFlushMode()
  {
    // return bbOS.getFlushMode();
    return false;
  }

  public ServletOutputStream getOutputStream() throws IOException
  {
    if (null == this.outStream)
    {
        this.outStream = new WCOutputStream((HttpOutputStreamConnectWeb) this.response.getBody());        
    }
    return this.outStream;
  }

  public boolean isAllocateDirect()
  {
    return this.allocateDirect;
  }

  public boolean isCommitted()
  {
    return this.response.isCommitted();
  }

  public void prepareHeadersForWrite()
  {
    // TODO Auto-generated method stub
  }

  public void removeHeader(String name)
  {
    this.response.removeHeader(name);
  }

  public void removeHeader(byte[] name)
  {
    this.response.removeHeader(new String(name));
  }

  public void setAllocateDirect(boolean allocateDirect)
  {
    this.allocateDirect = allocateDirect;
    // bbOS.setAllocateDirect(false);
  }

  public void setContentLanguage(String value)
  {
      //PM25421
      
      if (response.isContentLanguageSet()) {
      //if (response.getHeader("Content-Language") != null){
          if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())  {
              Tr.debug(tc, "setContentLanguage(String)", "Ignored as the Content-Language already set");
          }
          return;
      }

    this.response.setHeader("Content-Language", value);
  }
  
  public void setContentLength(int length) {
      this.response.setContentLength(length);
  }

  public void setContentLanguage(byte[] value)
  {
      //PM25421
      if (response.getHeader("Content-Language") != null){
          if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())  {
              Tr.debug(tc, "setContentLanguage(byte[])", "Ignored as the Content-Language already set");
          }
          return;
      }

      this.response.setHeader("Content-Language", new String(value));
  }

  public void setContentType(String value)
  {
    this.response.setHeader("Content-Type", value);
  }

  public void setContentType(byte[] value)
  {
    this.response.setHeader("Content-Type", new String(value));
  }

  public void setDateHeader(String name, long t)
  {
    this.response.setHeader(name, this.conn.getDateFormatter().getRFC1123Time(new Date(t)));
  }

  public void setFlushMode(boolean flushToWire)
  {
    // bbOS.setFlushMode(flushToWire);

  }
  
  public void setIsClosing(boolean isClosing) {
      outStream.setIsClosing(isClosing);
  }

  public void setHeader(String name, String s)
  {
    this.response.setHeader(name, s);
  }

  public void setHeader(byte[] name, byte[] bs)
  {
    this.response.setHeader(new String(name), new String(bs));
  }

  public void setIntHeader(String name, int i)
  {
    setHeader(name, Integer.toString(i));
  }

  public void setLastBuffer(boolean writeLastBuffer)
  {
    // TODO
  }

  public void setReason(String reason)
  {
    this.response.setReason(reason);
  }

  public void setReason(byte[] reason)
  {
    this.response.setReason(new String(reason));
  }

  public void setStatusCode(int code)
  {
    this.response.setStatus(code);
  }

  public void writeHeaders()
  {
    try
    {
      this.response.getBody().flushHeaders();
    }
    catch (IOException ioe)
    {
      // HTTP transport handles FFDCs
    }
  }
  
  public void flushBuffer() throws IOException
  {
      this.response.getBody().flush(false);
  }

  public int getBufferSize()
  {
    return this.response.getBody().getBufferSize();
  }

  public void resetBuffer()
  {
    this.response.getBody().clear();
  }

  public void setBufferSize(int bufferSize)
  {
    this.response.getBody().setBufferSize(bufferSize);
  }

  /**
   * Convert the transport cookie to a J2EE cookie.
   * 
   * @param cookie
   * @return Cookie
   */
  private Cookie convertHttpCookie(HttpCookie cookie)
  {
    Cookie rc = new Cookie(cookie.getName(), cookie.getValue());
    rc.setVersion(cookie.getVersion());
    if (null != cookie.getPath())
    {
      rc.setPath(cookie.getPath());
    }
    if (null != cookie.getDomain())
    {
      rc.setDomain(cookie.getDomain());
    }
    rc.setMaxAge(cookie.getMaxAge());
    rc.setSecure(cookie.isSecure());
    return rc;
  }

  public Cookie[] getCookies()
  {
    List<HttpCookie> cookies = this.response.getCookies();
    if (null == cookies)
    {
      return new Cookie[0];
    }
    Cookie[] rc = new Cookie[cookies.size()];
    int i = 0;
    for (HttpCookie cookie : cookies)
    {
      rc[i++] = convertHttpCookie(cookie);
    }
    return rc;
  }

  public String getHeader(String name)
  {
    return this.response.getHeader(name);
  }

  public String getHeader(byte[] name)
  {
    return this.response.getHeader(new String(name));
  }

  @SuppressWarnings("unchecked")
  public Vector[] getHeaderTable()
  {
    // Note: this API should be updated at some point to return a Map<String,
    // List<String>>
    List<String> names = this.response.getHeaderNames();
    Vector[] table = { new Vector(names.size()), new Vector(names.size()) };
    for (String name : names)
    {
      List<String> values = this.response.getHeaders(name);
      for (String value : values)
      {
        table[0].add(name);
        table[1].add(value);
      }
    }
    return table;
  }

  public IRequest getWCCRequest()
  {
    return this.request;
  }

  public void releaseChannel()
  {
      if (conn != null) {
          if (this.request.isStartAsync()) {
              if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
              {
                Tr.debug(tc, "releaseChannel for this IResponseImpl: " + this + " and this HttpInboundConnection: " + conn);
              }
              conn.finish(null);
          }    
      }
  }

  @Override
  public Enumeration getHeaderNames()
  {
      List<String> responseHeaders = response.getHeaderNames();
      return new WrappingEnumeration(responseHeaders);
  }

  @Override
  public Enumeration getHeaders(String name)
  {
      List<String> responseHeadersForName = response.getHeaders(name);
      return new WrappingEnumeration(responseHeadersForName);
  }

  @Override
  public void removeCookie(String cookieName)
  {
      List<HttpCookie> cookies = response.getCookies(cookieName);
      for (HttpCookie cookie:cookies) {
          response.removeCookie(cookie);
      }
  }
}
