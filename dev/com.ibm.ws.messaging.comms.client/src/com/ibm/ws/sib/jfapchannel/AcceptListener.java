/*******************************************************************************
 * Copyright (c) 2003, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.jfapchannel;

/**
 * A listener which is notified when new incoming conversations are
 * accpeted by a listening socket.  The implementor of this interface should
 * provide logic to deal with the new conversations.
 * <p>
 * By the time this listener is notified, the new conversation will have been
 * established but no initial data flows will have been sent.
 * 
 * @author prestona
 */
public interface AcceptListener
{
   /**
    * Notified when a new conversation is accepted by a listening socket.
    * <strong>Note:</strong> since this code is executed on the thread we
    * use for "receiving" data please don't go and invoked methods on the
    * connection which wait for a reply (e.g. exchange or some forms of
    * send) - you will block the receive thread waiting for itself to do
    * something.  Not a good idea.   
    * @param conversation The new conversation.
    * @return The conversation receive listener to use for asynchronous receive
    *          notification for this whole conversation.
    */
   ConversationReceiveListener acceptConnection(Conversation conversation);
}
