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

package com.ibm.ws.sib.processor.impl.interfaces;

import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.sib.mfp.JsMessage;
import com.ibm.ws.sib.transactions.TransactionCallback;

public interface JsMessageWrapper extends TransactionCallback
{
  /**
   * Get the underlying message object
   *
   * @return The underlying message object 
   */
  public JsMessage getMessage();

  public int guessRedeliveredCount();

  public boolean isReference();

  public Object getReportCOD() throws SIResourceException;

  public long updateStatisticsMessageWaitTime();
  
  public long getMessageWaitTime();
  
  /**
   * Whether this message was obtained via remote get
   */
  public boolean isRemoteGet();
}
