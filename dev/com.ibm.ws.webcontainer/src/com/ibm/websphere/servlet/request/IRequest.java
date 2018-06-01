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

import java.io.IOException;
import java.io.InputStream;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.List;

import javax.servlet.http.Cookie;

import com.ibm.websphere.servlet.response.IResponse;

/**
 * 
 *
 * Interface that the webcontainer recognizes as the types of requests that it can handle.
 * The webcontainer will call the methods on this interface during request processing.
 *
 * @ibm-api
 * 
 */

public interface IRequest {

	/** 
	  * Returns the method of the request
	  * @return String the method of the request
	 **/
	public String getMethod();

	/** 
	 * Returns the URI of the request 
	 * @return String the URI of the request
	 **/
	public String getRequestURI();

	/**
	 * Returns the remote user for the request
	 * @return String the remote user of the request
	 **/
	public String getRemoteUser();

	/**
	 * Returns the authorization Type of the current request
	 * @return String the authorization type of the request
	 **/
	public String getAuthType();

	/**
	 * Returns the value for the specified header requested
	 * @return String the given header value
	 */
	public String getHeader(String headerName);

   /**
    * Returns all values for the specified header requested
    * @return Enumeration the given header value
    */
    @SuppressWarnings("unchecked")
   public Enumeration getHeaders(String headerName);	

   /**
    * Returns the header value in long date format
    * @param name
    * @return date header value in date format
    */
   public long getDateHeader(String name);
   
  /**
   * Returns the header value as an int
   * @param name
   * @return Header value as an int
   */
   public int getIntHeader(String name);
   
	/**
	 * Requests the implementation to clear its headers datastructure. This allows for
	 * optimization, as the webcontainer will hold on to the reference to this request
	 * object upon finishing of the request processing, to prevent new request object 
	 * creation for every new request.
	 *
	 */
   public void clearHeaders();
   
   /**
    * Get all client header field names.
    * @return the names of all header fields sent by the client
    */
    @SuppressWarnings("unchecked")
   public Enumeration getHeaderNames();

	/**
	 * Method for getting the Content Length of the Request
	 * @return int the length of data in the request
	 **/
	public int getContentLength();
	
	/**
	 * Method for getting the Content Type of the Request
	 * @return String 
	 **/
	public String getContentType();
	

	/**
	 * Returns the protocol that the remote agent is speaking
	 * @return String the protocol of the request
	 **/
	public String getProtocol();

	/**
	 * Returns the Server hostname
	 * @return String the name of the server machine
	 **/
	public String getServerName();

	/**
	 * Returns the port of this connection
	 * @return int the port of the server
	 **/
	public int getServerPort();

	/**
	 * Returns the host name of the remote agent, or null if not known
	 * @return String the DNS name of the client machine
	 **/
	public String getRemoteHost();

	/**
	 * Returns the IP address of the remote agent, or null if not known
	 * @return String the IP Address of the client machine
	 **/
	public String getRemoteAddr();

	/**
	 * Returns the port used by the remote agent, or null if not known
	 * @return int the port of the client machine
	 */
	public int getRemotePort();

	/**
	 * Method that returns the scheme of the request
	 * @return String the scheme of the request
	 **/
	public String getScheme();

	/**
	 * Returns the input stream for this request 
	 * @param InputStream the input stream to use
	 **/
	public InputStream getInputStream() throws IOException;

	/**
	 * Returns the local address 
	 * @return String the local address 
	 **/
	public String getLocalAddr();

	/**
	 * Returns the local name 
	 * @return String the local name
	 **/
	public String getLocalName();

	/**
	 * Returns the local port 
	 * @return int the local port 
	 **/
	public int getLocalPort();

	/**
	  * Method to determine if the request is running on an SSL Connection
	  * @return boolean true if this connection is an SSL Connection
	  **/
	public boolean isSSL();

	/**
	 * Get at SSL Session ID
	 * @return byte[] containing the SSL session ID 
	 */
	public byte[] getSSLSessionID();

	/**
	 * Get the session id for this request
	 * @return String the session id
	 **/
	public String getSessionID();

	/**
	 * Returns whether the request was sent from the plugin 
	 * @return boolean true if plugin sent the request 
	 **/
	public boolean isProxied();

	/**
	 * Returns the webcontainer channel response object for this request
	 * @return IWCCResponse the response associated with this request
	 **/
	public IResponse getWCCResponse();

	/**
	 * Returns the cipherSuite
	 * @return Returns the cipherSuite
	 */
	public String getCipherSuite();
   
   /**
    * Returns the array of client certificates
    * @return Returns the client certificates
    */
   public X509Certificate[] getPeerCertificates();
   
   /**
    * Get the query string of the request.
    * @return String the query string for the request
    **/
   public String getQueryString();
   
   /**
    * Get all the cookies for the request.
    * @return Cookie[] containing all the cookies
    */
   public Cookie[] getCookies();
   
   /**
    * Get the value for the cookie specified.
    * @param name the cookie name
    * @return byte[] the value of the cookie
    */
   byte[] getCookieValue(String cookieName);

   /**
    * Get the values for the cookie specified.
    * @param name the cookie name
    * @return List of values associated with this cookie name.
    */
    @SuppressWarnings("unchecked")
   List getAllCookieValues(String cookieName);
   
   boolean getShouldDestroy();
   
   void setShouldDestroy(boolean shouldDestroy);

   public void setShouldReuse(boolean b);

   public void setShouldClose(boolean b);
   
   public void removeHeader(String headerName);
   
   
	public void startAsync();
	
	// RTC 160610. Moving this method to IRequestExtended.
	// It should not be SPI.
	//public ThreadPool getThreadPool();
	
	/*
	 * @deprecated in V8.0
	 * 
	 * isStartAsync is no longer used
	 */
	public boolean isStartAsync();
		
	/*
	 * @deprecated in V8.0
	 * 
	 * lock is no longer used
	 */
	public void lock ();

	/*
	 * unlock in V8.0
	 * 
	 * isStartAsync is no longer used
	 */
	public void unlock();

}
