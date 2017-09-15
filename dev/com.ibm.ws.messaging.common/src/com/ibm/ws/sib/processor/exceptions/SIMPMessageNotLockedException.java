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

package com.ibm.ws.sib.processor.exceptions;

import com.ibm.wsspi.sib.core.SIMessageHandle;
import com.ibm.wsspi.sib.core.exception.SIMessageNotLockedException;

public class SIMPMessageNotLockedException extends SIMessageNotLockedException
{
  /** The serial version UID, for version to version compatability */
  private static final long serialVersionUID = -7792307965921039274L;

  /**
   * @param arg0
   */
  public SIMPMessageNotLockedException(String arg0, SIMessageHandle[] messageHandles)
  {
    super(arg0);
    this.messageHandles = messageHandles;     
  }

  private int exceptionReason = -1;
  private String[] exceptionInserts = null;
  private SIMessageHandle[] messageHandles;

  /* (non-Javadoc)
   * @see com.ibm.websphere.sib.exception.SIException#getExceptionInserts()
   */
  public String[] getExceptionInserts()
  {
    if (exceptionInserts == null)
      return super.getExceptionInserts();
    
    return exceptionInserts;
  }

  /* (non-Javadoc)
   * @see com.ibm.websphere.sib.exception.SIException#getExceptionReason()
   */
  public int getExceptionReason()
  {
    if (exceptionReason < 0)
      return super.getExceptionReason();
    
    return exceptionReason;
  }
  
  /**
   * Set the exception inserts
   * @param exceptionInserts
   */
  public void setExceptionInserts(String[] exceptionInserts)
  {
    this.exceptionInserts = exceptionInserts;
  }
  
  /**
   * Set the exception reason code
   * @param exceptionReason
   */
  public void setExceptionReason(int exceptionReason)
  {
    this.exceptionReason = exceptionReason;
  }

  /* (non-Javadoc)
   * @see com.ibm.wsspi.sib.core.exception.SIMessageNotLockedException#getUnlockedMessages()
   */
  public SIMessageHandle[] getUnlockedMessages()
  {
    return messageHandles;
  }
}
