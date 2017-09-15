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
// Class: LogAllocationException
//------------------------------------------------------------------------------
/**
* This exception is generated when an attempt to create or extend a recovery
* log file failes (eg initial creation of a recovery log file during a cold
* start fails due to lack of disk space)
*/
public class LogAllocationException extends Exception
{
  public LogAllocationException(Throwable cause)
  {
    super(cause);
  }
}

