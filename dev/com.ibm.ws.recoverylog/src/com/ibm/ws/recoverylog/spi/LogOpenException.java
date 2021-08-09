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
// Class: LogOpenException
//------------------------------------------------------------------------------
/**
* This exception is generated when an attempt is made to call an operation that
* requires the log to be closed but the log is actually open.
*/
public class LogOpenException extends Exception
{
  protected LogOpenException(Throwable cause)
  {
    super(cause);
  }
}

