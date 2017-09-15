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

package com.ibm.wsspi.sib.core;

import com.ibm.websphere.sib.exception.SIIncorrectCallException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException;
import com.ibm.wsspi.sib.core.exception.SIConnectionLostException;
import com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException;
import com.ibm.wsspi.sib.core.exception.SILimitExceededException;
import com.ibm.wsspi.sib.core.exception.SIMessageNotLockedException;
import com.ibm.wsspi.sib.core.exception.SISessionDroppedException;
import com.ibm.wsspi.sib.core.exception.SISessionUnavailableException;

/**
 A LockedMessageEnumeration is used by an AsynchConsumerCallback to view, delete 
 and/or unlock messages that the corresponding ConsumerSession has been assigned. 
 The LockedMessageEnumeration implementation maintains a cursor into a list of 
 the messages assigned to the consumer; the methods of LockedMessageEnumeration 
 are interpreted in the context of the cursor's position.
 <p>
 The methods of a LockedMessageEnumeration can only be used while the 
 AsynchConsumerCallback is inside the consumeMessages method on which it was 
 passed to the AsynchConsumerCallback. Otherwise, they throw 
 SIIncorrectCallException. 
 <p>
 This class has no security implications.
 <p>
 An API layer delivering messages asynchronously to an application has at least 
 three options: 
 <ul>
 <li> It can call nextLocked(), followed by deleteCurrent(), and then give the 
      message to the application. This provides at-most-once semantics. </li>
 <li> It can call nextLocked(), then give the message to the  application, and 
      then call deleteCurrent(). This implements at-least-once semantics. </li>
 <li> It can call nextLocked(), then delete(tran), then give the message to the 
      application, then call tran.commit(). This implements exactly-once 
      semantics. </li>
 </ul>
*/
public interface LockedMessageEnumeration {
	
  /**
   Returns the next message locked to the consumer, or null if there are no more 
   messages.
   <p>
   If SIObjectClosedException is thrown, this indicates that the ConsumerSession
   associated with the LockedMessageEnumeration has been closed, and that for 
   this reason the LockedMessageEnumeration is no longer usable. However, it 
   should be noted that there is no assurance that nextLocked calls will fail 
   immediately after the ConsumerSession has been closed. For example, in the 
   case of a remote client, the first time that the user will discover the 
   ConsumerSession is closed is when a method is invoked that (unlike 
   nextLocked) cannot be serviced within the client, but causes a call back up
   to the server.
   
   @return the next locked message
   
   @throws com.ibm.wsspi.sib.core.exception.SISessionUnavailableException
   @throws com.ibm.wsspi.sib.core.exception.SISessionDroppedException
   @throws com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException
   @throws com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException
   @throws com.ibm.websphere.sib.exception.SIResourceException
   @throws com.ibm.wsspi.sib.core.exception.SIConnectionLostException
   @throws com.ibm.websphere.sib.exception.SIIncorrectCallException
  */
  public SIBusMessage nextLocked()
    throws SISessionUnavailableException, SISessionDroppedException,
           SIConnectionUnavailableException, SIConnectionDroppedException,
           SIResourceException, SIConnectionLostException,
           SIIncorrectCallException;
    				
  /** 
   Unlocks the message currently pointed to by the LockedMessageEnumeration's 
   cursor, making it available for redelivery to the same or other consumers.
   <p>
   It should be noted that any invocation of unlockCurrent can cause messages to 
   be delivered out of sequence (just as with a transaction rollback). When a 
   message is unlocked, its redeliveryCount is incremented.
   <p>
   SIMessageNotLockedException is thrown if the current item is not locked, for 
   example before the first call, to {@link #nextLocked}, or if it has been 
   removed already with {@link #deleteCurrent}.
   
   @throws com.ibm.wsspi.sib.core.exception.SISessionUnavailableException
   @throws com.ibm.wsspi.sib.core.exception.SISessionDroppedException
   @throws com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException
   @throws com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException
   @throws com.ibm.websphere.sib.exception.SIResourceException
   @throws com.ibm.wsspi.sib.core.exception.SIConnectionLostException
   @throws com.ibm.websphere.sib.exception.SIIncorrectCallException
   @throws com.ibm.websphere.sib.exception.SIMessageNotLockedException   
  */
  public void unlockCurrent()
    throws SISessionUnavailableException, SISessionDroppedException,
           SIConnectionUnavailableException, SIConnectionDroppedException,
           SIResourceException, SIConnectionLostException, 
           SIIncorrectCallException,
           SIMessageNotLockedException;
  					
  /**
   Acknowledges receipt of the message currently pointed to by the
   LockedMessageEnumeration's cursor, causing it to be deleted from the 
   destination. If a transaction is passed, then the delete occurs as part of 
   the unit of work represented by the transaction. The cursor advances to the 
   next message.
   <p>
   SIMessageNotLockedException is thrown if the current item is not locked, for 
   example before the first call, to {@link #nextLocked}, or if it has been 
   removed already with {@link #deleteCurrent}.
   
   @param tran the transaction under which the delete is to occur (may be 
   null)
   
   @throws com.ibm.wsspi.sib.core.exception.SISessionUnavailableException
   @throws com.ibm.wsspi.sib.core.exception.SISessionDroppedException
   @throws com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException
   @throws com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException
   @throws com.ibm.websphere.sib.exception.SIResourceException
   @throws com.ibm.wsspi.sib.core.exception.SIConnectionLostException
   @throws com.ibm.wsspi.sib.core.exception.SILimitExceededException
   @throws com.ibm.websphere.sib.exception.SIMessageNotLockedException   
  */
  public void deleteCurrent(SITransaction tran)
    throws SISessionUnavailableException, SISessionDroppedException,
           SIConnectionUnavailableException, SIConnectionDroppedException,
           SIResourceException, SIConnectionLostException, SILimitExceededException,
           SIIncorrectCallException,
           SIMessageNotLockedException;

  /**
   Acknowledges receipt of all messages locked to the consumer owning this
   LockedMessageEnumeration up to and including that currently pointed to by the 
   LockedMessageEnumeration's cursor, causing them to be deleted from the 
   destination. If a transaction is passed, then the delete occurs as part of 
   the unit of work represented by the transaction. Note that this call will
   acknowledgement not only messages in this LockedMessageEnumeration, but also
   any messages in LockedMessageEnumerations passed to previous invocations
   of consumeMessages that have not already been deleted or unlocked.
   <p>
   If a message cannot be deleted because it's lock has expired, then 
   SIMessageNotLockedException is thrown. Messages that were explicitly 
   unlocked by the consumer do not cause this exception, and are simply
   excluded from processing by deleteSeen.
   
   @param tran the transaction under which the delete is to occur (may be 
   null)
   
   @throws com.ibm.wsspi.sib.core.exception.SISessionUnavailableException
   @throws com.ibm.wsspi.sib.core.exception.SISessionDroppedException
   @throws com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException
   @throws com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException
   @throws com.ibm.websphere.sib.exception.SIResourceException
   @throws com.ibm.wsspi.sib.core.exception.SIConnectionLostException
   @throws com.ibm.wsspi.sib.core.exception.SILimitExceededException
   @throws com.ibm.websphere.sib.exception.SIIncorrectCallException
   @throws com.ibm.websphere.sib.exception.SIMessageNotLockedException
  */
  public void deleteSeen(SITransaction tran)
    throws SISessionUnavailableException, SISessionDroppedException,
           SIConnectionUnavailableException, SIConnectionDroppedException,
           SIResourceException, SIConnectionLostException, SILimitExceededException,
           SIIncorrectCallException,
           SIMessageNotLockedException;
						
  /**
   Resets the LockedMessageEnumeration's cursor to point to the first message in 
   this enumeration still available. Note that this may not be the message that 
   was first returned from nextLocked, which may have been deleted on the first 
   traversal of the list of locked messages.
   
   @throws com.ibm.wsspi.sib.core.exception.SISessionUnavailableException
   @throws com.ibm.wsspi.sib.core.exception.SISessionDroppedException
   @throws com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException
   @throws com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException
   @throws com.ibm.websphere.sib.exception.SIResourceException
   @throws com.ibm.wsspi.sib.core.exception.SIConnectionLostException
   @throws com.ibm.websphere.sib.exception.SIIncorrectCallException
  */
  public void resetCursor()
    throws SISessionUnavailableException, SISessionDroppedException,
           SIConnectionUnavailableException, SIConnectionDroppedException,
           SIResourceException, SIConnectionLostException,
           SIIncorrectCallException;

  /**
   Returns the ConsumerSession with which the LockedMessageEnumeration, and 
   AsynchConsumerCallback, are associated.
   
   @return the ConsumerSession with which the LockedMessageEnumeration is 
   associated

   @throws com.ibm.wsspi.sib.core.exception.SISessionUnavailableException
   @throws com.ibm.wsspi.sib.core.exception.SISessionDroppedException
   @throws com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException
   @throws com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException
   @throws com.ibm.websphere.sib.exception.SIIncorrectCallException
  */
  public ConsumerSession getConsumerSession()
    throws SISessionUnavailableException, SISessionDroppedException,
           SIConnectionUnavailableException, SIConnectionDroppedException,
           SIIncorrectCallException;
  	
  /**
   Returns the number of messages yet to be returned to this consumer; that is,
   the number of times getNext will return a non-null value. This method should
   only be used if the number or remaining messages is significant; otherwise,
   hasNext should be used.
   
   @return the number of remaining locked messages
   
   @throws com.ibm.wsspi.sib.core.exception.SISessionUnavailableException
   @throws com.ibm.wsspi.sib.core.exception.SISessionDroppedException
   @throws com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException
   @throws com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException
   @throws com.ibm.websphere.sib.exception.SIIncorrectCallException
  */
  public int getRemainingMessageCount()
    throws SISessionUnavailableException, SISessionDroppedException,
           SIConnectionUnavailableException, SIConnectionDroppedException,
           SIIncorrectCallException;
  /**
   Returns true if one or more messages remain to be returned to this consumer;
   that is, whether or not a aubsequent getNext will (assuming no message locks
   expire in the interim) return a message.
    
   @return whether any messages remain

   @throws com.ibm.wsspi.sib.core.exception.SISessionUnavailableException
   @throws com.ibm.wsspi.sib.core.exception.SISessionDroppedException
   @throws com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException
   @throws com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException
   @throws com.ibm.websphere.sib.exception.SIIncorrectCallException
  */
  public boolean hasNext()
    throws SISessionUnavailableException, SISessionDroppedException,
           SIConnectionUnavailableException, SIConnectionDroppedException,
           SIIncorrectCallException;
    
}
