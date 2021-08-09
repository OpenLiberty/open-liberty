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

import com.ibm.ws.sib.msgstore.MessageStoreException;
import com.ibm.ws.sib.processor.impl.BaseDestinationHandler;
import com.ibm.ws.sib.utils.SIBUuid8;


public interface LocalizationPoint extends ControllableResource
{
  public ConsumerManager createConsumerManager();
  public ConsumerManager getConsumerManager();
  public void dereferenceConsumerManager();
  
  /**
   * @return
   */
  public BaseDestinationHandler getDestinationHandler();
  /**
   * @return
   */
  public boolean reallocateMsgs();
  /**
   * @return
   */
  public SIBUuid8 getLocalizingMEUuid();
  /**
   * @return
   */
  public OutputHandler getOutputHandler();
  /**
   * @return
   */
  public boolean isSendAllowed();
  /**
   * @return
   */
  public boolean isQHighLimit();
  /**
   * @return
   */
  public long getID() throws MessageStoreException;

  /**
   * @param outputHandler
   */
  public void setOutputHandler(OutputHandler outputHandler);
  
  public void initializeNonPersistent(BaseDestinationHandler destinationHandler);
  /**
   * @return
   */
  public boolean isQLowLimit();
  
  /**
   * @return a long for the age of the oldest message on the queue 
   */
  public long getOldestMessageAge();
  
  /**
   * @return a long for the number of unlocked messages on the queue
   */
  public long getAvailableMessageCount();
  
  /**
   * @return a long for the number of locked messages on the queue
   */
  public long getUnAvailableMessageCount();
}
