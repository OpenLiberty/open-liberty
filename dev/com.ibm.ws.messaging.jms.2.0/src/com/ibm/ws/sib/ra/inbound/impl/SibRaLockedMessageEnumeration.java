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

package com.ibm.ws.sib.ra.inbound.impl;

import java.util.ArrayList;
import java.util.List;

import com.ibm.websphere.sib.exception.SIIncorrectCallException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.wsspi.sib.core.ConsumerSession;
import com.ibm.wsspi.sib.core.LockedMessageEnumeration;
import com.ibm.wsspi.sib.core.SIBusMessage;
import com.ibm.wsspi.sib.core.SITransaction;
import com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException;
import com.ibm.wsspi.sib.core.exception.SIConnectionLostException;
import com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException;
import com.ibm.wsspi.sib.core.exception.SILimitExceededException;
import com.ibm.wsspi.sib.core.exception.SIMessageNotLockedException;
import com.ibm.wsspi.sib.core.exception.SISessionDroppedException;
import com.ibm.wsspi.sib.core.exception.SISessionUnavailableException;

public class SibRaLockedMessageEnumeration implements LockedMessageEnumeration {

  // List of messages
  final List messages = new ArrayList();
  
  // Cursor position
  private int _cursor = 0;
  
  /**
   * Add the supplied message to the list.
   */
  public void add (SIBusMessage msg) 
  {
    messages.add (msg);
  }
  
  /**
   * Determines if the passed in msg is contained with this lme
   * 
   * @param msg   the SIBusMessage to check for
   * 
   * @return boolean true if the msg is part of this lme, false if not
   */
  public boolean contains(SIBusMessage msg)
  {
    return messages.contains(msg);
  }
  
  /**
   * Clears the current messages in the lme
   *
   */
  public void clear()
  {
    messages.clear();
  }
  
  public boolean isEmpty()
  {
    return messages.isEmpty();
  }
  
  /* (non-Javadoc)
   * @see com.ibm.wsspi.sib.core.LockedMessageEnumeration#nextLocked()
   */
  public SIBusMessage nextLocked() throws SISessionUnavailableException, SISessionDroppedException, SIConnectionUnavailableException, SIConnectionDroppedException, SIResourceException, SIConnectionLostException, SIIncorrectCallException {
    
    return _cursor >= messages.size () ? null : (SIBusMessage) messages.get(_cursor++);
    
  }

  /*
   * The following methods are either stubs or have some meaning. If a method is needed that
   * isn't currently implemented, then implement it
   */
  
  /* (non-Javadoc)
   * @see com.ibm.wsspi.sib.core.LockedMessageEnumeration#unlockCurrent()
   */
  public void unlockCurrent() throws SISessionUnavailableException, SISessionDroppedException, SIConnectionUnavailableException, SIConnectionDroppedException, SIResourceException, SIConnectionLostException, SIIncorrectCallException, SIMessageNotLockedException {
  }

  /* (non-Javadoc)
   * @see com.ibm.wsspi.sib.core.LockedMessageEnumeration#deleteCurrent(com.ibm.wsspi.sib.core.SITransaction)
   */
  public void deleteCurrent(SITransaction tran) throws SISessionUnavailableException, SISessionDroppedException, SIConnectionUnavailableException, SIConnectionDroppedException, SIResourceException, SIConnectionLostException, SILimitExceededException, SIIncorrectCallException, SIMessageNotLockedException {
  }

  /* (non-Javadoc)
   * @see com.ibm.wsspi.sib.core.LockedMessageEnumeration#deleteSeen(com.ibm.wsspi.sib.core.SITransaction)
   */
  public void deleteSeen(SITransaction tran) throws SISessionUnavailableException, SISessionDroppedException, SIConnectionUnavailableException, SIConnectionDroppedException, SIResourceException, SIConnectionLostException, SILimitExceededException, SIIncorrectCallException, SIMessageNotLockedException {
  }

  /* (non-Javadoc)
   * @see com.ibm.wsspi.sib.core.LockedMessageEnumeration#resetCursor()
   */
  public void resetCursor() throws SISessionUnavailableException, SISessionDroppedException, SIConnectionUnavailableException, SIConnectionDroppedException, SIResourceException, SIConnectionLostException, SIIncorrectCallException 
  {
    _cursor = 0;
  }

  /* (non-Javadoc)
   * @see com.ibm.wsspi.sib.core.LockedMessageEnumeration#getConsumerSession()
   */
  public ConsumerSession getConsumerSession() throws SISessionUnavailableException, SISessionDroppedException, SIConnectionUnavailableException, SIConnectionDroppedException, SIIncorrectCallException {
    return null;
  }

  /* (non-Javadoc)
   * @see com.ibm.wsspi.sib.core.LockedMessageEnumeration#getRemainingMessageCount()
   */
  public int getRemainingMessageCount() throws SISessionUnavailableException, SISessionDroppedException, SIConnectionUnavailableException, SIConnectionDroppedException, SIIncorrectCallException {
    return 0;
  }

  /* (non-Javadoc)
   * @see com.ibm.wsspi.sib.core.LockedMessageEnumeration#hasNext()
   */
  public boolean hasNext() throws SISessionUnavailableException, SISessionDroppedException, SIConnectionUnavailableException, SIConnectionDroppedException, SIIncorrectCallException {
    return _cursor < messages.size();
  }

}
