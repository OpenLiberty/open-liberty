/*******************************************************************************
 * Copyright (c) 2004, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/*
 * Created on Jan 1, 2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package com.ibm.ws.webcontainer.osgi.webapp;

import java.security.Principal;
import java.util.logging.Level;

import javax.servlet.http.HttpServletRequest;

import com.ibm.wsspi.webcontainer.servlet.IExtendedRequest;

/**
 * @author asisin
 * 
 *         To change the template for this generated type comment go to
 *         Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class WebAppDispatcherContext extends com.ibm.ws.webcontainer.webapp.WebAppDispatcherContext
{
    private static final String CLASS_NAME="com.ibm.ws.webcontainer.osgi.webapp.WebAppDispatcherContext"; 
  private Principal principal;

  public WebAppDispatcherContext()
  {
      if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {
          logger.logp(Level.FINE, CLASS_NAME,"constructor", "webapp is null, this -> " + this);
      }

      super.setWebApp(null);
  }

  public WebAppDispatcherContext(WebApp webapp)
  {
      if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {
          logger.logp(Level.FINE, CLASS_NAME,"constructor", "webapp -> " +webapp+ " , this -> " + this);
      }

      super.setWebApp(webapp);
  }

  public WebAppDispatcherContext(IExtendedRequest req)
  {
      if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {
          logger.logp(Level.FINE, CLASS_NAME,"constructor", "req -> " + req + " , this -> " + this);
      }
      
    this._request = req;
    this.initForNextDispatch(req);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.ibm.ws.webcontainer.webapp.WebAppDispatcherContext#getUserPrincipal()
   */
  public Principal getUserPrincipal()
  {
      //System.err.println("WebAppDispatch getUserPrincipal");
      // TODO: do we need to add any logic for getUserPrincipal in this class?
      // TODO: confirm w/ Bobby?
      return principal;
  }

  public void setUserPrincipal(Principal principal)
  {
    this.principal = principal;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.ibm.ws.webcontainer.webapp.WebAppDispatcherContext#isUserInRole(java
   * .lang.String, javax.servlet.http.HttpServletRequest)
   */
  public boolean isUserInRole(String role, HttpServletRequest req)
  {
    // TODO Auto-generated method stub
    return false;
  }

}
