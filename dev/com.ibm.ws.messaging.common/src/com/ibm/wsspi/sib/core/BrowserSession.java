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

import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException;
import com.ibm.wsspi.sib.core.exception.SIConnectionLostException;
import com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException;
import com.ibm.wsspi.sib.core.exception.SINotAuthorizedException;
import com.ibm.wsspi.sib.core.exception.SISessionDroppedException;
import com.ibm.wsspi.sib.core.exception.SISessionUnavailableException;

/**
 A BrowserSession is used to inspect the contents of a destination. In the 
 interests of domain-neutrality, it is possible to create a BrowserSession for a 
 TopicSpace. However, such a BrowserSession's next method will always return 
 null. It is anticipated that most calls to createBrowserSession will pass a 
 DestinationType of QUEUE, to cause the core API implementation to throw an 
 SIObjectNotFoundException if the named destination is in fact a TopicSpace.
 <p>
 It should be noted that the order in which messages are returned from 
 BrowserSession.next is undefined. It should also be noted that a browser is not 
 a consumer, and does not participate in consumer cardinality checks, for 
 example. The purpose of BrowserSession is to enable simple monitoring 
 applications to be written using the Core API.
 <p>
 This class has no direct security implications. Security checks are applied at
 the time that a BrowserSession is created, see the SICoreConnection
 class for details.
*/
public interface BrowserSession extends DestinationSession
{
	
  /**
   Returns a message that has been sent to the destination but not yet consumed, 
   or null if there are no more messages. Note that a null return value does 
   not imply that there are no messages that have been produced to the 
   destination and not yet consumed, but merely that there are no such messages
   visible in the part of the bus to which the application is connected.
   <p>
   The possibility of an SINotAuthorizedException being thrown by this method has
   been removed, there are no security implications with this method.
   
   @return a message from the destination
	 
   @throws com.ibm.wsspi.sib.core.exception.SISessionUnavailableException
   @throws com.ibm.wsspi.sib.core.exception.SISessionDroppedException
   @throws com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException
   @throws com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException
   @throws com.ibm.websphere.sib.exception.SIResourceException
   @throws com.ibm.wsspi.sib.core.exception.SIConnectionLostException
   @throws com.ibm.wsspi.sib.core.exception.SINotAuthorizedException
  */
  public SIBusMessage next()
    throws SISessionUnavailableException, SISessionDroppedException,
           SIConnectionUnavailableException, SIConnectionDroppedException,
           SIResourceException, SIConnectionLostException,
           SINotAuthorizedException;
	  				
  /**
   Resets the BrowserSession, such that the current traversal of the destination 
   is abandoned, and a fresh traversal begun. A subsequent call to next() 
   returns the first message on the destination that is available to this 
   BrowserSession. If there are active consumers attached to the destination, 
   then this will likely be a different message to that first returned in the
   original traversal.
   
   @throws com.ibm.wsspi.sib.core.exception.SISessionUnavailableException
   @throws com.ibm.wsspi.sib.core.exception.SISessionDroppedException
   @throws com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException
   @throws com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException
   @throws com.ibm.websphere.sib.exception.SIResourceException
   @throws com.ibm.wsspi.sib.core.exception.SIConnectionLostException
  */
  public void reset()
    throws SISessionUnavailableException, SISessionDroppedException,
           SIConnectionUnavailableException, SIConnectionDroppedException,
           SIResourceException, SIConnectionLostException;

}
