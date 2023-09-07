/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.sib.jfapchannel.impl;

import com.ibm.ws.sib.jfapchannel.framework.IOReadCompletedCallback;

/**
 * Base class for read callbacks to allow for heartbeat capabilities.
 */
public abstract class BaseConnectionReadCallback implements IOReadCompletedCallback
{
   /**
    * Notification callback to trigger that a heartbeat was received on the underlying connection.
    */
   protected abstract void heartbeatReceived();
   
   /**
    * Should register that a close notification for the underlying connection closed
    */
   protected abstract void stopReceiving();

   /**
    * Notified by the connection just before a physical close occurs.  This notification is
    * used to ensure that further I/O requests are not scheduled.
    * Care should be taken to synchronize correctly as the classes close method may be 
    * executed on differentthreads from the calling physical close.
    */
   protected abstract void physicalCloseNotification();

}
