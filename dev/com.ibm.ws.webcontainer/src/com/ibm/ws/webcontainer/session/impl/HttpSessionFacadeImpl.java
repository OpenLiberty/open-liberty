/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.session.impl;

import com.ibm.ws.session.HttpSessionFacade;
import com.ibm.ws.webcontainer.session.IHttpSession;

public class HttpSessionFacadeImpl extends HttpSessionFacade implements IHttpSession
{

  public HttpSessionFacadeImpl(HttpSessionImpl data)
  {
    super(data);
  }

  // Webcontainer's IHttpSession Interface methods cmd 196151
  public Object getSecurityInfo()
  {
    return ((HttpSessionImpl) _session).getSecurityInfo();
  }

  public void putSecurityInfo(Object pValue)
  {
    ((HttpSessionImpl) _session).putSecurityInfo(pValue);
  }
}
