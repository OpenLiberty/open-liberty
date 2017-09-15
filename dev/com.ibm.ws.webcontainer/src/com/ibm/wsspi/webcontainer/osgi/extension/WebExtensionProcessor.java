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
package com.ibm.wsspi.webcontainer.osgi.extension;

import com.ibm.ws.webcontainer.servlet.ServletWrapper;
import com.ibm.wsspi.webcontainer.servlet.IServletConfig;
import com.ibm.wsspi.webcontainer.servlet.IServletContext;
import com.ibm.wsspi.webcontainer.servlet.IServletWrapper;

/**
 * LIBERTY: This class is needed to create com.ibm.ws.webcontainer.osgi.servlet.ServletWrapper
 * instead of com.ibm.ws.webcontainer.servlet.ServletWrapperImpl
 */
public abstract class WebExtensionProcessor extends com.ibm.ws.webcontainer.extension.WebExtensionProcessor
{
  public WebExtensionProcessor(IServletContext webApp)
  {
    super(webApp);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.ibm.ws.webcontainer.extension.WebExtensionProcessor#createServletWrapper
   * (com.ibm.ws.webcontainer.servlet.ServletConfig)
   */
  public IServletWrapper createServletWrapper(IServletConfig config) throws Exception
  {
    ServletWrapper wrapper = new com.ibm.ws.webcontainer.osgi.servlet.ServletWrapper(extensionContext);

    if (config == null)
      return wrapper;
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
