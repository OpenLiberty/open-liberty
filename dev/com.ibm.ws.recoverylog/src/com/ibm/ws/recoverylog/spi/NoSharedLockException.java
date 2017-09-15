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
// Class: NoSharedLockException
//------------------------------------------------------------------------------
/**
* This exception indicates that an operation has failed as the caller currently
* does not hold a shared lock (see Lock.java)
*/
public class NoSharedLockException extends Exception
{
  /**
  * WebSphere RAS TraceComponent registration
  */
  private static final TraceComponent tc = Tr.register(NoSharedLockException.class,
                                           TraceConstants.TRACE_GROUP, null);

  //------------------------------------------------------------------------------
  // Method: NoSharedLockException.NoSharedLockException
  //------------------------------------------------------------------------------
  /**
  * Exception constructor.
  */
  public NoSharedLockException()
  {
    if (tc.isEntryEnabled()) Tr.entry(tc, "NoSharedLockException");
    if (tc.isEntryEnabled()) Tr.exit(tc, "NoSharedLockException", this);
  }
}

