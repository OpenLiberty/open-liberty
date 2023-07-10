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

import com.ibm.ws.sib.jfapchannel.framework.IOWriteCompletedCallback;

import com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException;

/**
 * Base class for write callbacks where work needs to be prodded in order to start and 
 * register closing once requested.
 */
public abstract class BaseConnectionWriteCallback implements IOWriteCompletedCallback
{
   /**
    * Encourages to write data. If the send callback is not currently busy transmitting data, 
    * proddling it will cause it to start. If it is already sending, proddling should be
    * harmless and doesn't irritate the callback.
    */
   protected abstract void proddle() throws SIConnectionDroppedException;
   
   /**
    * Should register that a close notification for the underlying connection closed
    */
   protected abstract void physicalCloseNotification();

}
