/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.processor.impl.corespitrace;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.utils.TraceGroups;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * @author gatfora
 * 
 * Trace class to provide filtering of all Core SPI calls 
 * made against the BrowserSession interface.
 * 
 * Filtering can be done against 
 * com.ibm.ws.sib.processor.impl.trace.CoreSPIBrowserSession
 *
 */
public class CoreSPIBrowserSession
{

  //trace for messages
  public static final TraceComponent tc =
    SibTr.register(
      CoreSPIBrowserSession.class,
      TraceGroups.TRGRP_PROCESSOR,
      SIMPConstants.TRACE_MESSAGE_RESOURCE_BUNDLE);

  // NLS for component
  public static final TraceNLS nls =
    TraceNLS.getTraceNLS(SIMPConstants.TRACE_MESSAGE_RESOURCE_BUNDLE);

}
