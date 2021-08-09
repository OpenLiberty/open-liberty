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
package com.ibm.ws.sib.processor.runtime;

import com.ibm.ws.sib.processor.SIMPConstants;

/**
 * The inbound receivers' view of received messages
 * @author tpm
 */
public interface SIMPReceivedMessageControllable extends SIMPRemoteMessageControllable
{
  
  public static class State
  {
    /** 
     * The message has been received but has not yet been delivered
     */
    public static final String AWAITING_DELIVERY = SIMPConstants.AWAITINGDEL_STRING;
  }
  
  
  public long getSequenceID();
  
  public long getPreviousSequenceID();
 
}
