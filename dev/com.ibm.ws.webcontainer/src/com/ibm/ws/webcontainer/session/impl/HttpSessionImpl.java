/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.session.impl;

import java.util.logging.Level;

import javax.servlet.ServletContext;

import com.ibm.ws.session.HttpSessionFacade;
import com.ibm.ws.session.SessionContext;
import com.ibm.ws.session.SessionData;
import com.ibm.ws.session.utils.LoggingUtil;
import com.ibm.ws.webcontainer.facade.IFacade;
import com.ibm.wsspi.session.ISession;

public class HttpSessionImpl extends SessionData implements IFacade
{

  protected HttpSessionFacade _httpSessionFacade;

  public HttpSessionImpl(ISession session, SessionContext sessCtx, ServletContext servCtx)
  {
    super(session, sessCtx, servCtx);
    _httpSessionFacade = returnFacade();
  }

  protected HttpSessionFacade returnFacade() {
      if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINER)) {
          LoggingUtil.SESSION_LOGGER_CORE.log(Level.FINE, "HttpSessionImpl returnFacade HttpSessionFacade");
      }
      return new HttpSessionFacade(this);
  }

  /*
   * To get the facade given out to the application
   * 
   * @see com.ibm.ws.webcontainer.facade.IFacade#getFacade()
   */
  public Object getFacade()
  {
    return (Object) _httpSessionFacade;
  }

}
