/*******************************************************************************
 * Copyright (c) 2003, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/*
 * Created on Dec 19, 2003
 *
 * To change this generated comment go to 
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package com.ibm.ws.webcontainer.osgi.extension;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import com.ibm.ws.container.service.metadata.MetaDataException;
import com.ibm.ws.webcontainer.osgi.webapp.WebApp;
import com.ibm.wsspi.webcontainer.metadata.WebComponentMetaData;
import com.ibm.wsspi.webcontainer.servlet.IServletConfig;
import com.ibm.wsspi.webcontainer.servlet.IServletWrapper;

/**
 * @author Arvind Srinivasan
 * 
 *         To change this generated comment go to
 *         Window>Preferences>Java>Code Generation>Code and Comments
 */
public class WebExtensionProcessor extends com.ibm.wsspi.webcontainer.osgi.extension.WebExtensionProcessor
{
  private final WebApp webApp;

  public WebExtensionProcessor(WebApp webapp)
  {
    super(webapp);
    this.webApp = webapp;
  }

  public IServletConfig createConfig(String servletName) throws ServletException
  {
    com.ibm.ws.webcontainer.servlet.ServletConfig sconfig = new com.ibm.ws.webcontainer.servlet.ServletConfig(servletName, extensionContext.getWebAppConfig());
    WebComponentMetaData wccmd; 
    try
    {
      wccmd = webApp.createComponentMetaData(servletName);
    }
    catch (MetaDataException ex)
    {
      throw new ServletException(ex);
    }
    sconfig.setMetaData(wccmd);
    return sconfig;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.ibm.wsspi.webcontainer.RequestProcessor#handleRequest(javax.servlet
   * .ServletRequest, javax.servlet.ServletResponse)
   */
  public void handleRequest(ServletRequest req, ServletResponse res) throws Exception
  {
    return;
  }

  public IServletWrapper getServletWrapper(ServletRequest req, ServletResponse resp)
  {
    // TODO Auto-generated method stub
    return null;
  }

  public WebComponentMetaData getMetaData()
  {
    return ((WebApp) extensionContext).getWebAppCmd();
  }

}
