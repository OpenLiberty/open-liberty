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

import com.ibm.websphere.sib.exception.SINotPossibleInCurrentStateException;
import com.ibm.wsspi.sib.core.SIMessageHandle;

/**
 This exception is used when a consumer attempts to read or operate on messages
 locked messages, using SIMessageHandle, but the message handle does not 
 identify a message that has been locked to the consumer. This may be because
 the message was never locked to the consumer, or it may be because the message
 lock has expired.   
 <p>
 This class has no security implications.
 */
public abstract class SIMessageNotLockedException 
    extends SINotPossibleInCurrentStateException 
{

  public SIMessageNotLockedException(String msg) {
    super(msg);
  }
  
  /**
   This method may be used to identify which messages were not processed by
   the method that threw the exception because they were not locked. 

   @return the handles of those messages that were not locked     
  */
  public abstract SIMessageHandle[] getUnlockedMessages();

}
