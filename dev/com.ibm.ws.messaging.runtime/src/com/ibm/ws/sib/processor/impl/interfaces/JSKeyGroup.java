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
import com.ibm.ws.sib.msgstore.LockingCursor;

public interface JSKeyGroup extends JSConsumerKey, ConsumerKeyGroup
{

  void addMember(JSConsumerKey key) throws SIResourceException;

  void removeMember(JSConsumerKey key);

  void stopMember();

  void startMember();

  boolean isStarted();

  void groupNotReady();

  void groupReady();

  void setConsumerActive(boolean b);

  boolean isGroupReady();

  void attachMessage(ConsumableKey key);

  ConsumableKey getAttachedMember();

  ConsumableKey getMatchingMember(ConsumableKey key);

  Object getAsynchGroupLock();

  LockingCursor getDefaultGetCursor();

  LockingCursor getGetCursor(int classification);
  
  /*
   * Methods that implement the required ReentrantLock methods
   */
  void lock();
  
  void unlock();

}
