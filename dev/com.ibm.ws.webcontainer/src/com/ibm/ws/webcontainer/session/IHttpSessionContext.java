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
package com.ibm.ws.webcontainer.session;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;
import javax.servlet.http.HttpSessionAttributeListener;

import com.ibm.ws.session.SessionManagerConfig;

public interface IHttpSessionContext{
  HttpSession getIHttpSession(HttpServletRequest req, HttpServletResponse res, boolean create);
  
  String getSessionUserName(HttpServletRequest req, HttpServletResponse res);
  
  String getRequestedSessionId(HttpServletRequest req);

  boolean isRequestedSessionIdValid(HttpServletRequest req, HttpSession sess);

  boolean isRequestedSessionIdFromCookie(HttpServletRequest req);

  boolean isRequestedSessionIdFromUrl(HttpServletRequest req);

  HttpSession sessionPreInvoke(HttpServletRequest req, HttpServletResponse res);

  void sessionPostInvoke(HttpSession sess);

  boolean isValid();

  void stop(String J2EEName); // cmd

  void reload(String J2EEName); // cmd
  
  boolean isValid(HttpSession sess, HttpServletRequest req, boolean create); // cmd PK01801

  String encodeURL(HttpSession sess, HttpServletRequest req, String url);

  /**
   * providing helper method to add listneners
   * for activity session impl
   */
  void addHttpSessionListener(HttpSessionListener listener, String J2EEName); // cmd

  // PK34418 begins
  void addHttpSessionAttributeListener(HttpSessionAttributeListener listener, String J2EEName);

  // PK34418 ends

  /**
   * To drive HttpSession created event
   * 
     * @param      event  object on which event is triggered.
   */
  public void sessionCreatedEvent(HttpSessionEvent event);

  /**
   * To drive HttpSession invalidated event
   * 
     * @param      event  object on which event is triggered.
   */
  public void sessionDestroyedEvent(HttpSessionEvent event);

  /**
   * To drive attribute added event
   * 
     * @param      event  object on which event is triggered.
   */
  public void sessionAttributeAddedEvent(HttpSessionBindingEvent event);

  /**
   * To drive attribute replaced event
   * 
     * @param      event  object on which event is triggered
   */
  public void sessionAttributeReplacedEvent(HttpSessionBindingEvent event);

  /**
   * To drive attribute removed event
   * 
     * @param      event  object on which event is triggered
   */
  public void sessionAttributeRemovedEvent(HttpSessionBindingEvent event);

  /**
     *   To check if timeout is set in deployment descriptor of the web module(web.xml) or not
   * 
   * @return true if session timeout is set
   *         false if session timeout is set to zero or not set.
   */
  public boolean isSessionTimeoutSet();

  /**
   * To get at session timeout used by web module
   * 
   * @return returns session timeout of the web module.
   */
  public int getSessionTimeOut();

  /**
   * To find out if session security integration is enabled
   * 
   * @return true if enabled, false otherwise
   * 
   */
  public boolean getIntegrateWASSecurity(); // PK01801

  //Liberty - Changed to return SessionManagerConfig from session_3.0 instead of the 
  //webcontainer SessionManagerConfigBase so that the webconatiner can use the componentised
  //session feature set
  public SessionManagerConfig getWASSessionConfig();
}
