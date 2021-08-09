/*******************************************************************************
 * Copyright (c) 2003, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.comms;

import com.ibm.ws.sib.mfp.AbstractMessage;

/**
 * A listener which may be registered for asynchronous notification of 
 * "interesting" MEConnection related events.
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
 * @see com.ibm.ws.sib.comms.MEConnection
 */
public interface MEConnectionListener
{
   /**
    * A message was received from the remote ME.  This message will be
    * identical to that sent via the MEConnection send method.
    * @see MEConnection#send(AbstractMessage, int)
    * @param message The message received.
    */
   void receiveMessage(MEConnection conn, AbstractMessage message);
      
   /**
    * Notification that an error has occurred.
    * @param meConnection The MEConnection object for which the error has occurred.
    * @param throwable The exceptional condition that has occurred.
    */
   void error(MEConnection meConnection, Throwable throwable);
}
