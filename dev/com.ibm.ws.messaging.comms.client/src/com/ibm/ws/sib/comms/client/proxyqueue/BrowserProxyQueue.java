/*******************************************************************************
 * Copyright (c) 2004, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.comms.client.proxyqueue;

import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.sib.comms.client.BrowserSessionProxy;
import com.ibm.ws.sib.mfp.JsMessage;
import com.ibm.ws.sib.mfp.MessageDecodeFailedException;
import com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException;
import com.ibm.wsspi.sib.core.exception.SIConnectionLostException;
import com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException;
import com.ibm.wsspi.sib.core.exception.SINotAuthorizedException;
import com.ibm.wsspi.sib.core.exception.SISessionDroppedException;
import com.ibm.wsspi.sib.core.exception.SISessionUnavailableException;

/**
 * A proxy queue for browser sessions.
 */
public interface BrowserProxyQueue extends ProxyQueue
{ 
   /**
    * Returns the next un-browsed message from this
    * proxy queue.  A value of null is returned if there
    * is no next message.
    * 
    * @return JsMessage
    */
   JsMessage next() 
      throws MessageDecodeFailedException, SISessionUnavailableException, SISessionDroppedException,
             SIConnectionUnavailableException, SIConnectionDroppedException,
             SIResourceException, SIConnectionLostException, 
             SIErrorException,
             SINotAuthorizedException;
   
   /**
    * Associates a browser session with this proxy queue.  The
    * session must be the session the proxy queue is being used
    * by. 
    */
   void setBrowserSession(BrowserSessionProxy browserSession);
   
   /**
    * Resets the browser session.  In proxy queue terms, this sends 
    * a reset request to the ME and purges the contents of the queue.
    */
   void reset()
      throws SISessionUnavailableException, SISessionDroppedException,
             SIConnectionUnavailableException, SIConnectionDroppedException,
             SIResourceException, SIConnectionLostException, 
             SIErrorException;

   /**
    * Closes the browser session proxy queue.
    * @throws SIResourceException
    * @throws SIConnectionLostException
    * @throws SIErrorException
    * @throws SIConnectionDroppedException
    */
   void close()                                             // D249096
   throws SIResourceException, 
          SIConnectionLostException, 
          SIErrorException, 
          SIConnectionDroppedException;
}
