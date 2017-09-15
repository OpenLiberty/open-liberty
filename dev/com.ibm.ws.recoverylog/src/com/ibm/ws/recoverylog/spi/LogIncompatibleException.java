/*******************************************************************************
 * Copyright (c) 2002, 2010 IBM Corporation and others.
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
// Class: LogIncompatibleException
//------------------------------------------------------------------------------
/**
* This exception is generated if an attempt is made by a client service to
* open an "incompatible" recovery log. By this we mean that one or more of the
* following attirubtes does not match the file being opened
*
* 1. Client Service Name
* 2. Client Service Version
* 3. Log Name
*
*/
public class LogIncompatibleException extends Exception
{
  public LogIncompatibleException()
  {
  }
}
