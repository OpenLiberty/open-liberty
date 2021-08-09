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
// Class: LogCorruptedException
//------------------------------------------------------------------------------
/**
* This exception is generated if the recovery log service detects that a corruption
* in a recovery log file as it reads it back from disk. This is often not as serious
* as it sounds since as long as the basic header information at the front of the 
* target log file is formatted correctly, the recovery log will tollerate a single
* corruption at any point through the reload process. If a corruption is detected
* then the read will stop and recovery will take place with the information read
* upto that point. The rational behind this is that the recovery log is designed
* to cope with corruption that occurs due to a system failure (eg power failure) 
* where the corrption will actually occur at the end of the file. In this case the
* code which attempted to force the data to disk will not have regained control,
* so it is safe to ignore the corrupted data. The recovery log is NOT designed to 
* be able to recovery if arbitary corrption (eg user has damaged the file manually) 
* has occured. In such cases the recovery log will thow the LogCorruptedException 
* exception.
*/
public class LogCorruptedException extends Exception
{
  protected LogCorruptedException(Throwable cause)
  {
    super(cause);
  }
}

