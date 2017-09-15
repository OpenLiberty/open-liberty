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

import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.wsspi.sib.core.exception.SIConnectionLostException;

public interface ConsumerPoint
{
  
  /**
   * Returns the consumer manager for this consumer point
   * @return the consumer manager for this consumer point
   */
  public ConsumerManager getConsumerManager();
   
  /**
   * Closes the consumer session and notifies the exception listeners 
   * that the session has closed
   * @param e - may be null
   * @throws SIConnectionLostException
   * @throws SIResourceException
   * @throws SIErrorException
   */
  public void closeSession(Throwable e) throws SIConnectionLostException, SIResourceException, SIErrorException;
  
  /** Determines whether this consumer point is being used for gathering */
  public boolean isGatheringConsumer();
}
