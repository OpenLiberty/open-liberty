/*******************************************************************************
 * Copyright (c) 2003, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.comms;

import java.io.UnsupportedEncodingException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.sib.admin.JsMessagingEngine;
import com.ibm.ws.sib.mfp.AbstractMessage;
import com.ibm.ws.sib.mfp.IncorrectMessageTypeException;
import com.ibm.ws.sib.mfp.MessageCopyFailedException;
import com.ibm.ws.sib.mfp.MessageEncodeFailedException;
import com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException;
import com.ibm.wsspi.sib.core.exception.SIConnectionLostException;
import com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException;

/**
 * Represents a logical connection between two MEs.
 * <p>
 * <em>A note on priority levels:</em>
 * Priority levels run 0 through 15 (inclusive).  With 15 being the highest
 * priority level.  Level 15 is reserved for internal use (heartbeats and the
 * likes) thus any attempt to use this value when transmitting data will result
 * in an exception being thrown.  The priority levels described do not (directly)
 * map onto the prioriy levels used by the core API.  Instead they run in the
 * priority range 2 to 11 with the highest priority message mapping to priority
 * level 11.  A special priority level (defined by the
 * com.ibm.ws.sib.jfapchannel.Conversation.PRIORITY_LOWEST constant) exists.
 * This attempts to queue data for transmission with the lowest priority level
 * of any data currently pending transmission.
 */
public interface MEConnection extends CommsConnection
{
   /**
    * Attempt to establish a connection from this ME to a remote ME.
    * The act of creating a new connection may not cause a new socket
    * level connection to be established.  Many connections between the
    * same two ME's may be multiplexed over the same physical socket.
    *
    * @param connectionProperties The information contained within this
    * object is used to dermine the remote ME to connect to.
    * @param meComponentHandshake
    * @throws SIResourceException
    */
   void connect(ConnectionProperties connectionProperties,
                MEComponentHandshake meComponentHandshake)
   throws SIResourceException;

   /**
    * Associates a messaging engine object with this class.
    * @see MEConnection#getMessagingEngine()
    * @param engine The messaging engine object to associate.
    */
   void setMessagingEngine(JsMessagingEngine engine);

   /**
    * Retrieves a previously associated messaging engine object.
    * @see MEConnection#setMessagingEngine(JsMessagingEngine)
    * @return JsMessagingEngine The previously associated messaging
    * engine reference.
    */
   JsMessagingEngine getMessagingEngine();

   /**
    * Closes the MEConnection.  An MEConnection cannot be used after it has been
    * closed.
    * @throws SIConnectionLostException Thrown if a communications error occurres closing the
    * connection.  Should this happen, the connection is still considered closed.
    */
   void close() throws SIConnectionLostException;
      
   /**
    * Attempts to send a message to the remote ME that this MEConnection
    * represents a connection to.
    * @param msg The message to send.
    * @param priority The priority level at which to send the data (see note in class
    * description)
    * @throws SIConnectionLostException Thrown if a communications failure occurres.
    * @throws SIConnectionDroppedException Thrown if the underlying connection is closed.
    * @throws SIConnectionUnavailableException Thrown if it is invalid to invoke
    * this method at this point in time.  For example if the MEConnection has been
    * closed.
    */
   void send(AbstractMessage msg, int priority)
      throws
         SIConnectionLostException,
         SIConnectionDroppedException,
         SIConnectionUnavailableException,
         MessageEncodeFailedException,
         MessageCopyFailedException,
         IncorrectMessageTypeException,
         UnsupportedEncodingException;

   /**
    * Returns whether the MEConnection is currently useable or not
    * @return boolean True if the Connection is ready, false if not.
    */
   boolean isReady();
   
   /**
    * Sets optional information about the ME that this MEConnection instance is connected/connecting to.
    * 
    * @param targetInformation
    */
   void setTargetInformation(final String targetInformation);
   
   /**
    * Gets information about the ME that this MEConnection instance is connected/connecting to. 
    * 
    * @param targetInformation may be null if setTargetInformation has yet to be called.
    */ 
   String getTargetInformation();

   /**
    * Indicate that an additional TRM bootstrap is required on this connection
    */
   public void setAdditionalTRMHandshakeRequired();
}
