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

package com.ibm.ws.sib.processor;

import com.ibm.websphere.sib.exception.SIIncorrectCallException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.wsspi.sib.core.LockedMessageEnumeration;
import com.ibm.wsspi.sib.core.SIBusMessage;
import com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException;
import com.ibm.wsspi.sib.core.exception.SIConnectionLostException;
import com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException;
import com.ibm.wsspi.sib.core.exception.SIMessageNotLockedException;
import com.ibm.wsspi.sib.core.exception.SISessionDroppedException;
import com.ibm.wsspi.sib.core.exception.SISessionUnavailableException;

/**
 * @author gatfora
 *
 * Extensions to the LockedMessageEnumeration class
 */
public interface MPLockedMessageEnumeration extends LockedMessageEnumeration {

    /** 
    Unlocks the message currently pointed to by the LockedMessageEnumeration's 
    cursor, making it available for redelivery to the same or other consumers.
    <p>
    It should be noted that any invocation of unlockCurrent can cause messages to 
    be delivered out of sequence (just as with a transaction rollback). When a 
    message is unlocked, its redeliveryCount is incremented only if the 
    redeliveryCountUnchanged flag is set to false.
    <p>
    SIMessageNotLockedException is thrown if the current item is not locked, for 
    example before the first call, to {@link #nextLocked}, or if it has been 
    removed already with {@link #deleteCurrent}.
    
    @param redeliveryCountUnchanged A value of false will maintain the original
    unlockCurrent behaviour.  A value of true will not increment the redelivery count.
    
    @throws com.ibm.wsspi.sib.core.exception.SISessionUnavailableException
    @throws com.ibm.wsspi.sib.core.exception.SISessionDroppedException
    @throws com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException
    @throws com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException
    @throws com.ibm.websphere.sib.exception.SIResourceException
    @throws com.ibm.wsspi.sib.core.exception.SIConnectionLostException
    @throws com.ibm.websphere.sib.exception.SIIncorrectCallException
    @throws com.ibm.websphere.sib.exception.SIMessageNotLockedException   
   */
   public void unlockCurrent(boolean redeliveryCountUnchanged)
     throws SISessionUnavailableException, SISessionDroppedException,
            SIConnectionUnavailableException, SIConnectionDroppedException,
            SIResourceException, SIConnectionLostException, 
            SIIncorrectCallException,
            SIMessageNotLockedException;

   /**
    * Enables the user of the call to see the next message in the enumeration without 
    * moving the cursor to the messages position.  This means that calls to deleteCurrent
    * deleteSeen will not remove the *peeked* at message.
    * 
    * Null will be returned if there is no next message.
    * 
    * @return
    * @throws SISessionUnavailableException
    * @throws SIResourceException
    * @throws SIIncorrectCallException
    */
   public SIBusMessage peek() 
     throws SISessionUnavailableException, SIResourceException, SIIncorrectCallException;
   
}


