/*******************************************************************************
 * Copyright (c) 2003, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.recoverylog.spi;

import com.ibm.tx.util.logging.Tr;
import com.ibm.tx.util.logging.TraceComponent;

//------------------------------------------------------------------------------
// Class: HoldingExclusiveLockException
//------------------------------------------------------------------------------
/**
* This exception indicates that an operation has failed as the caller currently
* holds an exclusive lock (see Lock.java)
*/
public class HoldingExclusiveLockException extends Exception
{
  /** 
  * WebSphere RAS TraceComponent registration
  */
  private static final TraceComponent tc = Tr.register(HoldingExclusiveLockException.class,
                                           TraceConstants.TRACE_GROUP, null);

  //------------------------------------------------------------------------------
  // Method: HoldingExclusiveLockException.HoldingExclusiveLockException
  //------------------------------------------------------------------------------
  /**
  * Exception constructor.
  */
  public HoldingExclusiveLockException()
  {
    if (tc.isEntryEnabled()) Tr.entry(tc, "HoldingExclusiveLockException");
    if (tc.isEntryEnabled()) Tr.exit(tc, "HoldingExclusiveLockException", this);
  }
}

