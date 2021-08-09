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
package com.ibm.ws.webcontainer.osgi.srt;

import com.ibm.ws.webcontainer.osgi.webapp.WebAppDispatcherContext;

/**
 * @author asisin
 * 
 *         To change the template for this generated type comment go to
 *         Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class SRTConnectionContext extends com.ibm.ws.webcontainer.srt.SRTConnectionContext
{

  /**
   * Used for pooling the SRTConnectionContext objects.
   */
  public SRTConnectionContext nextContext;

  /**
	 * 
	 */
  public SRTConnectionContext()
  {
    super();
    init();
  }
  
  protected void init() {
      this._dispatchContext = new WebAppDispatcherContext(_request);
      _request.setWebAppDispatcherContext(_dispatchContext);
  }

}
