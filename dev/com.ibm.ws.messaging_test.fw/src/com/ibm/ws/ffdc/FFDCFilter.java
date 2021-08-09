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
package com.ibm.ws.ffdc;

import com.ibm.ws.sib.ffdc.FFDCEngine;

/* ************************************************************************** */
/**
 * This is a version of the real FFDC code that allows a test case to examine the
 * FFDCs etc.
 *
 */
/* ************************************************************************** */
public class FFDCFilter
{
  /* -------------------------------------------------------------------------- */
  /* processException method
  /* -------------------------------------------------------------------------- */
  /**
   * @param th        The Throwable being passed to FFDC
   * @param sourceId  An identifier of the source generating the FFDC
   * @param probeId   An identifier within the source that locates the FFDC 
   */
  public static void processException(Throwable th, String sourceId, String probeId)
  {
    FFDCEngine.processException(th,sourceId,probeId,null,null);
  }
  
  /* -------------------------------------------------------------------------- */
  /* processException method
  /* -------------------------------------------------------------------------- */
  /**
   * @param th         The Throwable being passed to FFDC
   * @param sourceId   An identifier of the source generating the FFDC
   * @param probeId    An identifier within the source that locates the FFDC
   * @param callerThis The object generating the FFDC
   */
  public static void processException(Throwable th, String sourceId, String probeId, Object callerThis)
  {
    FFDCEngine.processException(th,sourceId,probeId,callerThis,null);
  }
  
  /* -------------------------------------------------------------------------- */
  /* processException method
  /* -------------------------------------------------------------------------- */
  /**
   * @param th          The Throwable being passed to FFDC
   * @param sourceId    An identifier of the source generating the FFDC
   * @param probeId     An identifier within the source that locates the FFDC
   * @param objectArray An array of objects to be passed to the FFDC Engine 
   */
  public static void processException(Throwable th, String sourceId, String probeId, Object [] objectArray)
  {
    FFDCEngine.processException(th,sourceId,probeId,null,objectArray);
  }
  
  /* -------------------------------------------------------------------------- */
  /* processException method
  /* -------------------------------------------------------------------------- */
  /**
   * @param th          The Throwable being passed to FFDC
   * @param sourceId    An identifier of the source generating the FFDC
   * @param probeId     An identifier within the source that locates the FFDC 
   * @param callerThis  The object generating the FFDC
   * @param objectArray An array of objects to be passed to the FFDC Engine 
   */
  public static void processException(Throwable th, String sourceId, String probeId, Object callerThis, Object [] objectArray)
  {
    FFDCEngine.processException(th,sourceId,probeId,callerThis,objectArray);
  }
}
