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

package com.ibm.ws.sib.matchspace.utils;


/**
 * @author Neil Young
 *
 * Make concrete instances of Trace.
 */
public class TraceFactory
{
  // The size of the ring buffer in bytes for ringBuffer tracing, 0 means no ring buffer.

  
  /**
   * Factory method to get the trace. 
   * @param Class the sourceClass
   * @return Trace implementatiion for the source class.
   */
  public static Trace getTrace(Class sourceClass, String traceGroup) 
  {
    return new TraceImpl(sourceClass, traceGroup);
  } // getTrace().
}
  