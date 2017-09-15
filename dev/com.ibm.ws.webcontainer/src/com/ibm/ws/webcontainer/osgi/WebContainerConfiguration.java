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
package com.ibm.ws.webcontainer.osgi;

import java.util.Map;

import com.ibm.wsspi.webcontainer.WCCustomProperties;

/**
 * @author asisin
 * 
 *         To change the template for this generated type comment go to
 *         Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class WebContainerConfiguration extends com.ibm.ws.webcontainer.WebContainerConfiguration
{
  //private String port = null;
  private Map<String, Object> properties;

  // Configuration property key strings
  private static final String CFG_KEY_ENFORCE_SEC = "enforce.security";
  
  // unused
  // public final static String CFG_KEY_LOGIN_CONFIG = "login.config";
  // public final static String CFG_KEY_TOKEN_TYPE = "token.type";
  // public final static String CFG_KEY_COOKIE_NAME = "cookie.name";

  public WebContainerConfiguration(String port)
  {
    super();
    //this.port = port;
  }

  @SuppressWarnings("unchecked")
  public void setConfiguration(Map<String, Object> properties)
  {
    this.properties = properties;
//    if (this.properties != null && "true".equalsIgnoreCase((String) this.properties.get(CFG_KEY_ENFORCE_SEC)))
//    {
//      SecurityContext.setSecurityEnabled(true);
//    }
    WCCustomProperties.setCustomProperties(properties);
    
  }

  @SuppressWarnings("unchecked")
  public Map<String, Object> getConfiguration()
  {
    return properties;
  }
  
}
