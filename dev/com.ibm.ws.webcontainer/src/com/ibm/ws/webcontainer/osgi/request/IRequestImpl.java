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
package com.ibm.ws.webcontainer.osgi.request;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import javax.servlet.http.Cookie;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.servlet.request.extended.IRequestExtended;
import com.ibm.websphere.servlet.response.IResponse;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.util.ThreadPool;
import com.ibm.ws.webcontainer.osgi.osgi.WebContainerConstants;
import com.ibm.ws.webcontainer.util.IteratorEnumerator;
import com.ibm.ws.webcontainer.webapp.WebApp;
import com.ibm.wsspi.http.HttpCookie;
import com.ibm.wsspi.http.HttpInboundConnection;
import com.ibm.wsspi.http.HttpRequest;
import com.ibm.wsspi.http.SSLContext;
import com.ibm.wsspi.http.channel.values.HttpHeaderKeys;
import com.ibm.wsspi.http.ee7.HttpInboundConnectionExtended;
import com.ibm.wsspi.webcontainer.WCCustomProperties;


/**
 * Implementation of a webcontainer request wrapping an HTTP dispatcher provided
 * connection and request object.
 */
public class IRequestImpl implements IRequestExtended
{
  private static final TraceComponent tc = Tr.register(IRequestImpl.class, WebContainerConstants.TR_GROUP, WebContainerConstants.NLS_PROPS);

  // private static final String
  // CLASS_NAME="com.ibm.ws.webcontainer.osgi.request.IRequestImpl";

  private HttpInboundConnection conn = null;
  protected HttpRequest request = null;
  private boolean startAsync;
  private ReentrantLock asyncLock;
  private String serverName = null;
  private int serverPort = -1;
  private boolean isHttpsIndicatorSecure;
  private boolean isHttpsIndicatorSecureSet;
  private String normalizedURI= null; // PI05525

  private String contentType;
  private String scheme;
  private Boolean isSSL;
  private static boolean normalizeRequestURI = WCCustomProperties.NORMALIZE_REQUEST_URI; //PI05525

  /**
   * Constructor for a webcontainer request on a given inbound connection.
   *
   * @param connection
   */
  public IRequestImpl(HttpInboundConnection connection)
  {
    this.conn = connection;
    this.request = connection.getRequest();
  }

  public void clearHeaders()
  {
    // cannot clear inbound request headers
  }

  public List<String> getAllCookieValues(String cookieName)
  {
    // seems like it should be the byte[] back like the getCookieValue
    // but session code is expecting Strings for this API
    List<HttpCookie> cookies = this.request.getCookies(cookieName);
    List<String> values = new ArrayList<String>(cookies.size());
    for (HttpCookie cookie : cookies)
    {
      values.add(cookie.getValue());
    }
    return values;
  }

  public String getAuthType()
  {
      // TODO webcontainer checks dispatcher.isSecurityenabledForapplication
      // and uses some private attributes in that case
      String type = this.conn.getTrustedHeader(HttpHeaderKeys.HDR_$WSAT.getName());

      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
          Tr.debug(tc, "type=" + type);
      return type;
  }

  public String getCipherSuite()
  {
      if (this.conn.useTrustedHeaders()) {
          String csHdr = this.conn.getTrustedHeader(HttpHeaderKeys.HDR_$WSCS.getName());
          if (csHdr != null) {
              //321485
              if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                  Tr.debug(tc, "getCipherSuite isTrusted --> true suite --> " + csHdr);

              return csHdr;
          }
          //F001872 - start
          // Client connected to web server but did not provide certificate
          if ((this.conn.getTrustedHeader(HttpHeaderKeys.HDR_$WSRA.getName())) != null){
              return null;
          }
      }

      return getConnectionCipherSuite();   //PI75166
  }

  //PI75166
  /**
   * @return the ciphersuite string, or null if the SSL context or session is null
   */
  public String getConnectionCipherSuite() { //F001872 Start

      String suite = null;
      SSLContext ssl = this.conn.getSSLContext();
      if (null != ssl) {
          if (ssl.getSession() != null) {
              suite = ssl.getSession().getCipherSuite();
          }
      }
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
          Tr.debug(tc, "getConnectionCipherSuite suite --> " + suite);

      return suite;
  }

  //PI75166
  /**
   * @return
   */
  public Boolean checkForDirectConnection(){

      boolean direct = true;

      if(this.request.getHeader(HttpHeaderKeys.HDR_$WSRA.getName()) != null)
         direct = false ;

      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
          Tr.debug(tc, "checkForDirectConnection return --> " + direct);

      return Boolean.valueOf(direct);
  }     //F001872 - end


  public int getContentLength()
  {
    long rc = this.request.getContentLength();
    if (rc > Integer.MAX_VALUE)
    {
      return -1;
    }
    return (int) rc;
  }

  public String getContentType()
  {
      if (this.contentType == null) {
          this.contentType = this.request.getHeader("Content-Type");
      }

      return this.contentType;
  }

  public byte[] getCookieValue(String cookieName)
  {
    HttpCookie cookie = this.request.getCookie(cookieName);
    if (null != cookie)
    {
      return cookie.getValue().getBytes();
    }
    return null;
  }

  public Cookie[] getCookies()
  {
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
          Tr.debug(tc, "getCookies ENTRY");

    List<HttpCookie> cookies = this.request.getCookies();
    if (cookies.size()==0) {
        return null;
    }
    Cookie[] rc = new Cookie[cookies.size()];
    int i = 0;
    //Start PI15886
    int t = 0;
    for (HttpCookie cookie : cookies)
    {
        Cookie newCookie;
        try {
            newCookie = new Cookie(cookie.getName(), cookie.getValue());
        } catch (IllegalArgumentException iae) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Invalid cookie name: " + cookie.getName());
            t++;
            continue;
        }
      if(cookie.getDomain() != null && !cookie.getDomain().equals("")){
          newCookie.setDomain(cookie.getDomain());
      }
      newCookie.setComment(cookie.getComment());
      newCookie.setHttpOnly(cookie.isHttpOnly());
      newCookie.setMaxAge(cookie.getMaxAge());
      newCookie.setPath(cookie.getPath());
      newCookie.setVersion(cookie.getVersion());
      newCookie.setSecure(cookie.isSecure());
      rc[i++] = newCookie;
    }

   if(t==0){
        return rc;
    } else {
        if (t==cookies.size()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "No valid cookies");
            return null;
        }
        Cookie[] ca = new Cookie[cookies.size()-t];
        System.arraycopy(rc, 0, ca, 0, ca.length);
        return ca;
    }
    //End PI15886
  }

  // Preventing us generating an FFDC for a parse excption because that relates to bad data passed to us from the HTTP client and not a serivce issue.
  @FFDCIgnore(ParseException.class)
  public long getDateHeader(String name)
  {
    String value = this.request.getHeader(name);
    if (null != value)
    {
      try
      {
        Date rc = this.conn.getDateFormatter().parseTime(value);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
          Tr.debug(tc, name + " " + rc.getTime());
        return rc.getTime();
      }
      catch (ParseException e)
      {
        throw new IllegalArgumentException(value, e);
      }
    }
    return -1L;
  }

  public String getHeader(String headerName)
  {
    return this.request.getHeader(headerName);
  }

  @SuppressWarnings("unchecked")
  public Enumeration getHeaderNames()
  {
    List<String> names = this.request.getHeaderNames();
    return new IteratorEnumerator(names.iterator());
  }

  @SuppressWarnings("unchecked")
  public Enumeration getHeaders(String headerName)
  {
    List<String> values = this.request.getHeaders(headerName);
    return new IteratorEnumerator(values.iterator());
  }

  public InputStream getInputStream() throws IOException
  {
    return this.request.getBody();
  }

  public int getIntHeader(String name)
  {
    int rc = -1;
    String value = this.request.getHeader(name);
    if (null != value)
    {
      rc = Integer.parseInt(value.trim());
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
      Tr.debug(tc, name + " " + rc);
    return rc;
  }

  public String getLocalAddr()
  {
    return this.conn.getLocalHostAddress();
  }

  public String getLocalName()
  {
    return this.conn.getLocalHostName(true);
  }

  public int getLocalPort()
  {
    return this.conn.getLocalPort();
  }

  public String getMethod()
  {
    return this.request.getMethod();
  }

  // Begin PI29820
  /**
   * Convert a single certificate to a chain of certificate(s).
   * Method pulled from tWAS com.ibm.ws.webcontainer.channel.WCCRequestimpl
   */
  private X509Certificate[] convertCertToChain (X509Certificate rootCert)
  throws Exception {
      //321485
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
          Tr.debug(tc, "convertCertToChain", "");

      X509Certificate[] chain = null;

      if (rootCert != null)
      {
          chain = new X509Certificate[1];
          chain[0] = rootCert;
      }
      return chain;
  }

  /**
   * ASCII-armor a string.  (See RFC 2440, but there may be a better RFC)
   * Assume the string is already base-64 encoded.
   * Method pulled from tWAS com.ibm.ws.webcontainer.channel.WCCRequestimpl
   */
  private String armor (String str)
  {
      StringBuffer sb = new StringBuffer();
      sb.append("-----BEGIN CERTIFICATE-----\r\n");
      for (int begin = 0; begin < str.length();)
      {
          int end = Math.min(begin+76,str.length());
          sb.append (str.substring(begin,end) + "\r\n");
          begin += 76;
      }
      sb.append("-----END CERTIFICATE-----\r\n");
     //321485
      String buffer = sb.toString();
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
          Tr.debug(tc, "armor", " " + buffer);

      return buffer;
  }

  /**
   * Method pulled from tWAS com.ibm.ws.webcontainer.channel.WCCRequestimpl
   */
  @FFDCIgnore(javax.net.ssl.SSLPeerUnverifiedException.class)
  public X509Certificate[] getPeerCertificates()
  {
      try {
          X509Certificate[] chain = null;
          if (this.conn.useTrustedHeaders()) {
              String clientCertificate = this.request.getHeader(HttpHeaderKeys.HDR_$WSCC.getName());
              if (clientCertificate != null) {
                  CertificateFactory cf = CertificateFactory.getInstance("X.509");
                  ByteArrayInputStream inStream = new ByteArrayInputStream(armor(clientCertificate).getBytes());
                  X509Certificate cert = (X509Certificate)cf.generateCertificate(inStream);
                  chain = convertCertToChain (cert);
                  return chain;
              }
              //F001872 - start
              // Client connected to web server but did not provide certificate
              if ((request.getHeader(HttpHeaderKeys.HDR_$WSRA.getName())) != null){
                      return null;
              }
              //F001872 - end
          }
          // PK46372 - added || part to if statement.

          chain = getConnectionPeerCertificates();
          //end 254912

          return chain;
      }
    catch (Exception exc) {
        FFDCFilter.processException(exc, getClass().getName(), "peercerts", new Object[] { this });
        if (TraceComponent.isAnyTracingEnabled() && tc.isErrorEnabled())
        {
            TraceComponent tc_twas = Tr.register(IRequestImpl.class, WebContainerConstants.TR_GROUP, "com.ibm.ws.webcontainer.resources.Messages");
            Tr.error(tc_twas, "invalid.peer.certificate", exc.toString());
        }

        return null;
    }
  }
  /**
   * Method pulled from tWAS com.ibm.ws.webcontainer.channel.WCCRequestimpl
   */
  public X509Certificate[] getConnectionPeerCertificates() throws Exception {
      X509Certificate[] rc = null;
      SSLContext ssl = this.conn.getSSLContext();
      if (null != ssl && (ssl.getNeedClientAuth() || ssl.getWantClientAuth()))
      {
        try
        {
          Object[] objs = ssl.getSession().getPeerCertificates();
          if(objs != null) {
              rc = (java.security.cert.X509Certificate[]) objs;
          }
          else {
              // javadoc: getPeerCertificateChain exists for compatibility with previous releases
              // http://docs.oracle.com/javase/7/docs/api/javax/net/ssl/SSLSession.html#getPeerCertificateChain()
              objs = ssl.getSession().getPeerCertificateChain();
              if (objs != null) {
                  rc = convertCertificateChain((javax.security.cert.X509Certificate[]) objs);
              }
          }
        }
        catch (javax.net.ssl.SSLPeerUnverifiedException e) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "No certificates in the SSLSession");
        }
      }
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        Tr.debug(tc, "certs->", (Object[]) rc);
      return rc;
  }

  /**
   * Method pulled from tWAS com.ibm.ws.webcontainer.channel.WCCRequestimpl
   */
  private X509Certificate[] convertCertificateChain(javax.security.cert.X509Certificate[] inChain) throws Exception {
      //321485
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
          Tr.debug(tc, "convertCertificateChain", ""); //306998.4

      X509Certificate[] outChain = new X509Certificate[inChain.length];
      CertificateFactory cf = CertificateFactory.getInstance("X.509");
      for (int idx = 0; idx < inChain.length; idx++) {
          outChain[idx] = (X509Certificate) (cf.generateCertificate(new ByteArrayInputStream(inChain[idx].getEncoded())));
      }
      return outChain;
  }
  // End PI29820

  public String getProtocol()
  {
      String protocol = this.conn.getTrustedHeader(HttpHeaderKeys.HDR_$WSPR.getName());
      if ( protocol != null ) {
          if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
              Tr.debug(tc, "getProtocol isTrusted --> true, protocol --> " + protocol);
      } else {
          protocol = this.request.getVersion();
          if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
              Tr.debug(tc, "getProtocol protocol --> " + protocol);
      }

      return protocol;
  }

  public String getQueryString()
  {
    return this.request.getQuery();
  }

  public String getRemoteAddr()
  {
    // The connection takes $WSRA into account.
    return conn.getRemoteHostAddress();
  }

  public String getRemoteHost()
  {
    // The connection takes $WSRH into account
    return conn.getRemoteHostName(true);
  }

  public int getRemotePort()
  {
    return this.conn.getRemotePort();
  }

  public String getRemoteUser()
  {
    String user = this.conn.getTrustedHeader(HttpHeaderKeys.HDR_$WSRU.getName());
    return user;
  }

  public String getRequestURI()
  {
      // TODO webcontainer.channel used it's own encoding on the byte[]
      //Start PI05525
      String uri=null;
      if(normalizeRequestURI){
          if (this.normalizedURI != null ) {
              uri = this.normalizedURI;
              if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                  Tr.debug(tc,"getRequestURI","Use previously normalized request uri --> " + normalizedURI);

          } else {
              uri = this.request.getURI();
              if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                  Tr.debug(tc,"getRequestURI","normalize request uri --> ", uri);
              uri = WebApp.normalize(uri);
              this.normalizedURI = uri;
          }

      }  //End PI05525
      else{
          uri = this.request.getURI();

      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
          Tr.debug(tc,"getRequestURI", " uri --> " + uri);

      return uri;
  }

  //PI75166
  public byte[] getSSLSessionID()
  {
    byte[] rc = null;

    String sslHdr = this.conn.getTrustedHeader(HttpHeaderKeys.HDR_$WSSI.getName());
    if(sslHdr!=null){
        rc = sslHdr.getBytes();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "getSSLSessionID , trusted found " + sslHdr);

        return rc;
    }

    SSLContext ssl = this.conn.getSSLContext();
    if (null != ssl) {
      rc = ssl.getSession().getId();
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
      Tr.debug(tc, "getSSLSessionID , trusted not found");

    return rc;
  }

  public String getScheme()
  {
      if (this.scheme == null) {
          boolean useForwarded =false;

          if(conn instanceof HttpInboundConnectionExtended) {

              HttpInboundConnectionExtended ice = (HttpInboundConnectionExtended) conn;

              if(ice.useForwardedHeaders()) {

                  useForwarded = true;
                  String forwardedProto = ice.getRemoteProto();

                  if (forwardedProto!=null) {

                      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                          Tr.debug(tc, " isTrusted --> true --> containsHeader --> X-Forwarded-Proto or Forwarded proto parameter --> scheme --> "+forwardedProto);
                      }
                      this.scheme = forwardedProto;
                      return this.scheme;
                  }
              }
          }


          // PM70260 - Duplicate code from tWAS WCCRequestImpl.java
          //321485(tWAS)
          // First determine whether to trust headers from this connection
          // By default headers are trusted from all connections but this can be limited to a specific host/set of hosts
          // with the HTTP Dispatcher config property trustedHeaderOrigin (or set to "none" to disallow from any host).
          if (this.conn.useTrustedHeaders()) {
              if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                  Tr.debug(tc, "getScheme: useTrustedHeaders is true");
              // WC config property httpsIndicatorHeader can be set to the name of a header that states whether SSL termination
              // has been performed upstream
              if (isHttpsIndicatorSecure()) {
                  //321485(tWAS)
                  if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                      Tr.debug(tc, " isTrusted --> true, isHttpsIndicatorSecure --> true, scheme --> https");
                  this.scheme = "https";
                  return this.scheme;
              }

              // Private WAS header set by WAS Plugin (and other proxies if configured) to contain original scheme
              String WSSC_header =  this.conn.getTrustedHeader(HttpHeaderKeys.HDR_$WSSC.getName());
              if (WSSC_header != null) {
                  //321485(tWAS)
                  if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                      Tr.debug(tc, " isTrusted --> true, containsHeader --> $WSSC, scheme --> " + WSSC_header);
                  this.scheme = WSSC_header;
                  return this.scheme;
              }

              // Private WAS header set by WAS Plugin (and other proxies if configured) to state that secure protocal was used
              String WSIS_header =  this.conn.getTrustedHeader(HttpHeaderKeys.HDR_$WSIS.getName());
              if (WSIS_header != null ){
                  if (WSIS_header.equalsIgnoreCase("true")) {
                      //321485(tWAS)
                      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                          Tr.debug(tc, " isTrusted --> true --> containsHeader --> $WSIS  --> scheme --> https");
                      this.scheme = "https";
                      return this.scheme;
                  }
                  else {
                      //321485(tWAS)
                      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                          Tr.debug(tc, " isTrusted --> true --> containsHeader --> $WSIS  --> scheme --> http");
                      this.scheme = "http";
                      return this.scheme;
                  }
              }

              // De-facto standard header used to indicate original scheme
              String FORWARDED_PROTO_header =  this.conn.getTrustedHeader(HttpHeaderKeys.HDR_X_FORWARDED_PROTO.getName());
              if (FORWARDED_PROTO_header != null && !useForwarded){
                  if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                      Tr.debug(tc, " isTrusted --> true --> containsHeader --> X-Forwarded-Proto  --> scheme --> "+FORWARDED_PROTO_header);
                  this.scheme = FORWARDED_PROTO_header;
                  return this.scheme;
              }
          }
          //321485(tWAS)
          this.scheme = request.getScheme();
      }
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
          Tr.debug(tc, "scheme --> " + this.scheme);
      return this.scheme;
  }

  public String getServerName()
  {
    String name = this.serverName;
    if (null == name)
    {
    	// $WSSN or request.getVirtualHost or ...
        name = this.serverName = this.conn.getRequestedHost();
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
      Tr.debug(tc, "name=" + name);
    return name;
  }

  public int getServerPort()
  {
      int port = this.serverPort;
      if (-1 == port) {
    	  // $WSSP or request.getVirtualPort or local listening port
          port = this.serverPort = this.conn.getRequestedPort();
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
          Tr.debug(tc, "port=" + port);
      return port;
  }


    private boolean isHttpsIndicatorSecure() {
        if (!isHttpsIndicatorSecureSet) {
            if ((WCCustomProperties.HTTPS_INDICATOR_HEADER != null)
                            && (getHeader(WCCustomProperties.HTTPS_INDICATOR_HEADER) != null)) {
                isHttpsIndicatorSecure = true;

                // Check the host header so we can set the port to 443
                String host = getHeader("Host");
                if (host != null)
                {
                    int idx = host.indexOf(':');
                    if (idx != -1)
                    {
                        int len = host.length();
                        int sp = 0;
                        for (int i = idx + 1; i < len; i++)
                        {
                            sp = sp * 10 + host.charAt(i) - '0';
                        }
                        this.serverPort = sp;
                    }
                    else {
                        this.serverPort = 443;
                    }
                }
            }
            else {
                isHttpsIndicatorSecure = false;
            }
            isHttpsIndicatorSecureSet = true;
        }
        return isHttpsIndicatorSecure;
  }

  public String getSessionID()
  {
    // TODO Session
    return null;
  }

  public boolean getShouldDestroy()
  {
    // TODO Need to implement
    return false;
  }

  public IResponse getWCCResponse()
  {
    // TODO Auto-generated method stub
    return null;
  }

  public boolean isProxied()
  {
    return false;
  }

  public boolean isSSL()
  {
      if (this.isSSL == null) {
          boolean useForwarded =false;

          if (conn instanceof HttpInboundConnectionExtended){

              HttpInboundConnectionExtended ice = (HttpInboundConnectionExtended) conn;

              if (ice.useForwardedHeaders()) {
                  useForwarded = true;
                  String forwardedProto = ice.getRemoteProto();

                  // router may set this header for all protocols so check specifically for regular ssl (https) and websocket ssl (wss)
                  if (("https").equalsIgnoreCase(forwardedProto)||("wss").equalsIgnoreCase(forwardedProto)) {
                      isSSL = true;
                      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){
                          Tr.debug(tc, " isTrusted --> true --> containsHeader --> X-Forwarded-Proto or Forwarded proto parameter --> "+ forwardedProto+" ssl --> " + isSSL);
                      }
                      return isSSL;
                  }
              }
          }

          //321485(tWAS)
          if (this.conn.useTrustedHeaders()) {
              //begin PK12164
              if (isHttpsIndicatorSecure()) {
                  if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) //306998.4(tWAS)
                      Tr.debug(tc, " isTrusted --> true, isHttpsIndicatorSecure --> true ssl --> true");
                  isSSL = true;
                  return isSSL;
              }
              //end  PK12164
              String WSIS_header =  this.conn.getTrustedHeader(HttpHeaderKeys.HDR_$WSIS.getName());
              if (WSIS_header != null) {
                  isSSL = WSIS_header.equalsIgnoreCase("true");
                  if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) //306998.4(tWAS)
                      Tr.debug(tc, " isTrusted --> true ssl --> " + isSSL);
                  return isSSL;
              }
              String FORWARDED_PROTO_header =  this.conn.getTrustedHeader(HttpHeaderKeys.HDR_X_FORWARDED_PROTO.getName());
              if (FORWARDED_PROTO_header != null && !useForwarded) {
                  // router may set this header for all protocols so check specifically for regular ssl (https) and websocket ssl (wss)
                  if ((FORWARDED_PROTO_header.equalsIgnoreCase("https"))||(FORWARDED_PROTO_header.equalsIgnoreCase("wss"))) {
                      isSSL = true;
                      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                          Tr.debug(tc, " isTrusted --> true --> containsHeader --> X-Forwarded-Proto  --> "+FORWARDED_PROTO_header+" ssl --> " + isSSL);
                      return isSSL;
                  }
              }
          }
          isSSL = (null != this.conn.getSSLContext());
      }
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) //306998.4(tWAS)
          Tr.debug(tc, " ssl --> " + isSSL);
      return isSSL;
  }

  public boolean isStartAsync()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
      Tr.debug(tc, String.valueOf(startAsync));
    return startAsync;
  }

  public void lock()
  {

    synchronized (this)
    {
      if (asyncLock == null)
      {
        asyncLock = new ReentrantLock();
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
      Tr.debug(tc, "lock asyncLock: " + asyncLock);

    asyncLock.lock();
  }

  public void removeHeader(String headerName)
  {
    // cannot remove a header from a request
  }

  public void setShouldClose(boolean flag)
  {
    // TODO Auto-generated method stub

  }

  public void setShouldDestroy(boolean shouldDestroy)
  {
    // TODO Auto-generated method stub

  }

  public void setShouldReuse(boolean flag)
  {
    // TODO Auto-generated method stub

  }


  public void startAsync()
  {
    startAsync = true;
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
      Tr.debug(tc, String.valueOf(startAsync));
  }

  public void unlock()
  {
    if (asyncLock != null && asyncLock.isHeldByCurrentThread())
    {
      asyncLock.unlock();

      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        Tr.debug(tc, "unlock asyncLock: " + asyncLock);
    }

  }

  @Override
  public ThreadPool getThreadPool()
  {
    // LIBERTY: TODO Auto-generated method stub - probably needed for ARD
    // function
    return null;
  }

  /**
   * @return HttpInboundConnection for this request
   */
  @Override
  public HttpInboundConnection getHttpInboundConnection() {
      return this.conn;
  }
}
