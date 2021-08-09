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

import com.ibm.wsspi.sib.core.exception.SIConnectionLostException;

/**
 * Listener for asynchronous notification a send has completed.  This
 * listener can be supplied when invoking the send method on a
 * conversation.
 * @see com.ibm.ws.sib.jfapchannel.Conversation
 */
public interface SendListener
{
   /**
    * Invoked to notify the implementor that the data was sent successfully.
    * @param conversation The conversation the data was sent successfully over.
    */
	void dataSent(Conversation conversation);                  // F181603.2
	
   /**
    * Invoked if an error occurred when attempting to sent the data.
    * @param exception The exception that occurred.
    * @param conversation The conversation associated with the error that
    * occurred, or null if this not known.
    */
	void errorOccurred(SIConnectionLostException exception,    // F174602, F181603.2 
                       Conversation conversation);
	
}
