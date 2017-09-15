/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.osgi.filter;

import com.ibm.ws.webcontainer.osgi.webapp.WebApp;
import com.ibm.ws.webcontainer.webapp.WebAppConfiguration;

public class WebAppFilterManagerImpl extends com.ibm.ws.webcontainer.filter.WebAppFilterManager
{
  public WebAppFilterManagerImpl(WebAppConfiguration config, WebApp webApp)
  {
    super(config, webApp);
  }

  
}
