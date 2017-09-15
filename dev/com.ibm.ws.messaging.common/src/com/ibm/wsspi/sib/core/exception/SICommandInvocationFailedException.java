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

package com.ibm.wsspi.sib.core.exception;

import com.ibm.websphere.sib.exception.SIException;

/**
 This exception is thrown by the invokeCommand method if the invoked command
 returns an exception. The original exception is returned as a linked exception. 
 <p>
 This class has no security implications.
*/
public class SICommandInvocationFailedException extends SIException 

{
  private static final long serialVersionUID = 2224385837581092786L;
  
  public SICommandInvocationFailedException(String msg)
  {
    super(msg);
  }
  
  public SICommandInvocationFailedException(String msg, Throwable linkedException)
  {

    super(msg, linkedException);
  }
}
