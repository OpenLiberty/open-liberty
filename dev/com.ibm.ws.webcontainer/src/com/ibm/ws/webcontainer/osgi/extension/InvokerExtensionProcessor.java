/*******************************************************************************
 * Copyright (c) 1997, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.osgi.extension;

import java.util.HashMap;

import com.ibm.ws.webcontainer.osgi.servlet.ServletWrapper;
import com.ibm.ws.webcontainer.webapp.WebApp;
import com.ibm.wsspi.webcontainer.servlet.IServletConfig;
import com.ibm.wsspi.webcontainer.servlet.IServletWrapper;

/**
 * @author asisin
 * 
 */
public class InvokerExtensionProcessor extends com.ibm.ws.webcontainer.extension.InvokerExtensionProcessor
{

  private static String showCfg = "com.ibm.websphere.examples.ServletEngineConfigDumper";

  /**
   * @param webApp
   */
  @SuppressWarnings("unchecked")
  public InvokerExtensionProcessor(WebApp webApp, HashMap params)
  {
    super(webApp, params);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.ibm.ws.webcontainer.extension.WebExtensionProcessor#createServletWrapper
   * (com.ibm.wsspi.webcontainer.servlet.IServletConfig)
   */
  public IServletWrapper createServletWrapper(IServletConfig config) throws Exception
  {
    ServletWrapper wrapper = new com.ibm.ws.webcontainer.osgi.servlet.ServletWrapper(extensionContext);
    try
    {
      wrapper.initialize(config);
    }
    catch (Throwable e)
    {
      // Might be more serious....so first log
      e.printStackTrace(System.out);
    }

    return wrapper;
  }

}
