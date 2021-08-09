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

/**
 AsynchConsumerCallback is an interface that can be implemented by the client 
 application (or API layer), in order to receive messages asynchronously. The 
 consumeMessages method is called when messages are available for receipt by the 
 consumer respresented by the ConsumerSession with which the 
 AsynchConsumerCallback has been registered using 
 ConsumerSession.registerAsynchConsumerCallback.
 <p>
 This class has no security implications.
 
 
 @see com.ibm.wsspi.sib.core.ConsumerSession#registerAsynchConsumerCallback
 @see com.ibm.wsspi.sib.core.ConsumerSession#deregisterAsynchConsumerCallback
*/
public interface AsynchConsumerCallback {
	
  /**
   The consumeMessages method is called by the Core API implementation to 
   deliver messages asynchronously to an application (or API layer). Messages 
   are locked to the consumer, and cannot be delivered to other consumers; they 
   must be either deleted or unlocked by the consumer to which they have been 
   delivered, unless unlocked implicitly as a result of closing the consumer, 
   shutdown of the Messaging Engine, or returning from consumeMessages without 
   having viewed the messages using LockedMessageEnumeration.nextLocked().
	 <p>
   Note that if the consumer views some but not all of the locked messages, 
   then only those that have not been seen are implicitly unlocked on return 
   from consumeMessages.
   <p>
   The signature of the consumeMessages method does not declare any exceptions. 
   However, if the implementation throws a Throwable, the Core API 
   implementation will catch it and increment the redeliveryCount for the 
   message(s) that were not consumed.
   <p>
   It should be noted that any messages not unlocked or deleted by the 
   AsynchConsumerCallback when control is returned from consumeMessages remain 
   locked to the ConsumerSession. The unlockSet, unlockAll, and deleteSet 
   methods on  ConsumerSession can be used to process the messages at some
   later time.
   <p>
   Any exception thrown by the implementation of this method will be caught and 
   delivered to any registered SICoreConnectionListeners.
   
   @param lockedMessages
   
   @throws Throwable
   
  */
  public void consumeMessages(
    LockedMessageEnumeration lockedMessages) throws Throwable;
}

