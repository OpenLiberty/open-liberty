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
import com.ibm.websphere.sib.exception.SINotPossibleInCurrentConfigurationException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException;
import com.ibm.wsspi.sib.core.exception.SIConnectionLostException;
import com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException;
import com.ibm.wsspi.sib.core.exception.SILimitExceededException;
import com.ibm.wsspi.sib.core.exception.SINotAuthorizedException;
import com.ibm.wsspi.sib.core.exception.SISessionDroppedException;
import com.ibm.wsspi.sib.core.exception.SISessionUnavailableException;

/**
 A ProducerSession is used to send messages to a destination. It can be 
 regarded as representing a destination "opened for put". 
 <p>
 Security checks are applied at the time that a ProducerSession is created,
 see the SICoreConnection class for details. Further security checks may be
 applied at send time, see the send method below for details.
 <p> 
 In addition to the method arguments themselves, the send operation is also 
 parameterized by several properties of the SIBusMessage:
 <ul>
   <li> ForwardRoutingPath - 
        After being delivered to the destination to which the producer is 
        attached the message is then forwarded in turn to each destination in 
        the ForwardRoutingPath. (Note that mediations at the destinations 
        specified in the ForwardRoutingPath may modify the 
        ForwardRoutingPath). Only when the message arrives at the final 
        destination in the ForwardingRoutingPath is it made available to 
        consumers. The default ForwardRoutingPath is empty. </li>
   <li> ReverseRoutingPath - 
        The ReverseRoutingPath is delivered with the message to the ultimate 
        consumer. It may be modified by any mediations along the forward route. 
        Server applications can use the ReverseRoutingPath of request messages 
        to determine the ForwardRoutingPath of reply messages. The default 
        ReverseRoutingPath is empty. </li>
   <li> Priority - 
        A value between 0 and 9, 0 indicating that the message is at lowest 
        priority, and 9 indicating highest. Efforts are made to deliver higher 
        priority messages ahead of lower priority messages. The default 
        priority is 4. </li>
   <li> Reliability - 
        Each message is sent with a particular reliability, which is one of 
        Express, Reliable, or Assured. The Reliability of the message can be 
        set to one of these values or to Unknown, which indicates that the 
        message is to be sent using the Destination's configured maximum 
        priority (this is the default). If Unknown is used, then before the send 
        call returns, Unknown will be replaced with the actual reliability used 
        in the Reliability field. If an attempt is made to send a message 
        with a stronger reliability than the Destination's maximum, then 
        SIIncompatibleQosException is thrown. </li>
   <li> TimeToLive - 
        The number of milliseconds from the time of the send call, (some time) 
        after which the message may be expired. The default value is 0, 
        indicating that the message does not expire. </li>
   <li> Disciminator  - 
        The name of the discriminator to which the message is sent. This is 
        described in more detail in the Core API package.html.</li>
   </ul>
*/
public interface ProducerSession extends DestinationSession {	

  /**
   Sends a message to the destination to which the producer is attached. Note 
   that in addition to the method arguments themselves, the send operation is 
   also parameterized by several properties of the SIBusMessage, which are 
   described above.
   <p>
   If the quality of service set in the messsage exceeds the maximum supported
   by the destination, then SINotPossibleInCurrentConfigurationException is 
   thrown.
   <p>
   If no Discriminator was supplied at the time the ProducerSession was created
   and Bus security is enabled then the authorization of the user (see
   SICoreConnection.createProducerSession for details of the user that this
   applies to) to be able to send a message for the Discriminator set in
   the message is checked before the message is accepted. If the user has not
   been assigned the Sender role for this Discriminator on this Destination
   the message will not be processed by the destination.
   <p>
   The above authorization failure can be indicated to the user in a number of ways:
   <ul>
    <li> An SINotAuthorizedException will be thrown from the send when the send is
         not transacted and the message's Reliability exceeds ExpressNonPersistent
         (it is possible for the exception to be thrown from the send even when these
         conditions are not matched).
    <li> If an SINotAuthorizedException was not thrown by the send and the send is
         transacted the transaction will be rolled back.
   </ul>
   
   @param msg the message to be sent
   @param tran the transaction under which the send is to occur (may be null)

   @throws com.ibm.wsspi.sib.core.exception.SISessionUnavailableException
   @throws com.ibm.wsspi.sib.core.exception.SISessionDroppedException
   @throws com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException
   @throws com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException
   @throws com.ibm.websphere.sib.exception.SIResourceException
   @throws com.ibm.wsspi.sib.core.exception.SIConnectionLostException
   @throws com.ibm.wsspi.sib.core.exception.SILimitExceededException
   @throws com.ibm.wsspi.sib.core.exception.SINotAuthorizedException
   @throws com.ibm.websphere.sib.exception.SIIncorrectCallException
   @throws com.ibm.websphere.sib.exception.SINotPossibleInCurrentConfigurationException
  */
  public void send(
      SIBusMessage msg,
      SITransaction tran)
    throws SISessionUnavailableException, SISessionDroppedException,
           SIConnectionUnavailableException, SIConnectionDroppedException,
           SIResourceException, SIConnectionLostException, SILimitExceededException, 
           SINotAuthorizedException,
           SIIncorrectCallException,
           SINotPossibleInCurrentConfigurationException;

}

