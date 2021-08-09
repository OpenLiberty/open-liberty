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

import java.lang.reflect.Method;

public class TraceUtils 
{
  /**
   * Wrapper method to get the trace via the Factory. 
   * @param Class the sourceClass
   * @return Trace implementatiion for the source class.
   */
  public static Trace getTrace(Class sourceClass, String traceGroup) 
  {
    Class traceFactoryClass;
    Object ret = null;
    try 
    {
      traceFactoryClass = Class.forName("com.ibm.ws.sib.matchspace.utils.TraceFactory");

      Class[] params = new Class[]{Class.class, String.class};
      Method getTraceMethod = traceFactoryClass.getMethod("getTrace", params);
      Object[] objParams = new Object[]{sourceClass, traceGroup};
      ret = getTraceMethod.invoke(null, objParams);
    } 
    catch (Exception e) 
    {
      // No FFDC Code Needed.
      // Trace and FFDC not available so print to stdout.      
      e.printStackTrace();
    }    
       
    return (Trace)ret;
  } // getTrace().

}
