/*******************************************************************************
 * Copyright (c) 2002, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.recoverylog.spi;

//------------------------------------------------------------------------------
// Class: ServantFailureDuringRecoveryException
//------------------------------------------------------------------------------
/**
* A requested operation is not available or cannot be issued in the present state
*/
public class ServantFailureDuringRecoveryException extends RecoveryFailedException
{
  public ServantFailureDuringRecoveryException()
  {
      super();
  }
  
  public ServantFailureDuringRecoveryException(Throwable cause)
  {
      super(cause);
  }
}

