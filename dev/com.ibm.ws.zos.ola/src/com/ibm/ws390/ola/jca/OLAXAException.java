/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws390.ola.jca;

import javax.transaction.xa.XAException;

public class OLAXAException extends XAException
{
  /**
   * Constructor
   *
   * The XAException class does not allow both an error code and a causing
   * exception to be specified.  This class tries to mitigate that by
   * setting the exception cause manually, if it can.
   */
  OLAXAException(int errorCode, Throwable cause)
  {
    super(errorCode);
    
    try
    {
      initCause(cause);
    }
    catch (IllegalStateException ise)
    {
      /* Nothing */
    }
  }
}