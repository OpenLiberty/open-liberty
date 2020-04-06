/*******************************************************************************
 * Copyright (c) 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package javax.servlet.sip;

import javax.servlet.http.HttpSession;

/**
 * The ConvergedHttpSession class provides access to HttpSession related
 * functionality which is only present in a converged HTTP/SIP container.
 * In a converged container, an instance of HttpSession can be cast to
 * ConvergedHttpSession in order to access methods available available
 * only to converged applications.
 *
 * @since 1.1
 */
public interface ConvergedHttpSession extends HttpSession {
  /**
   * This method encodes the HTTP URL with the jsessionid.
   * ";jsessionid=http-session-id". The URL parameter should
   * be an absolute URL. For example, http://server:7001/mywebapp/foo.jsp. Where
   * "/mywebapp" is the context path of the the current ServletContext, because
   * that is where the httpSession belongs to.
   *
   * @param  url the HTTP URL String to be encoded
   * @return encoded URL with jsessionid
   */
  public String encodeURL(String url); 
 
  /**
   * Converts the given relative path to an absolute URL by
   * prepending the contextPath for the current ServletContext, the given
   * scheme ("http" or "https"), and the host:port, and then encoding the
   * resulting URL with the jsessionid.
   * <p>
   * For example, this method converts:
   * <pre>
   *   from: <code>"/foo.jsp"</code>
   *   to: <code>"http://server:8888/mywebapp/foo.jsp;jsessionid=http-session-id"</code>
   * </pre>Where,
   * 
   *   <b>"/mywebapp"</b> is the contextPath for the current ServletContext
   *   <b>server</b> is the front end host defined for the web server.
   *
   * @param relativePath relative to the current webapp
   * @param scheme the scheme ("http" or "https")
   * @return encoded URL with jsessionid
   */
  public String encodeURL(String relativePath, String scheme);
  
  /**
   * Returns the parent SipApplicationSession if it exists, if none exists
   * then a new one is created and returned after associating it with the 
   * converged http session.
   *
   * @return the parent SipApplicationSession 
   */
  public SipApplicationSession getApplicationSession();
}
