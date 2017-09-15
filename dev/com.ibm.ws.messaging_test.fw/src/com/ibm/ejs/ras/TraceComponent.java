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
package com.ibm.ejs.ras;

import java.util.Enumeration;
import java.util.Properties;

import com.ibm.websphere.ws.sib.unittest.ras.Trace;
import com.ibm.ws.sib.unittest.ras.Logger;

public class TraceComponent extends TraceElement
{
  private Dumpable _dumpable;
  private String _bundle;

  TraceComponent(Logger logger, String bundle)
  {
    super(logger);
    _bundle = bundle;
  }

  public final boolean isDumpEnabled()
  {
    return false;
  }

  public final String getResourceBundleName()
  {
    return _bundle;
  }

  void registerDumpable(Dumpable d)
  {
    _dumpable = d;
  }

  Dumpable getRegisteredDumpable()
  {
    return _dumpable;
  }

  public static boolean isAnyTracingEnabled()
  {
    return TraceElement.isAnyTracingEnabled();
  }

}
