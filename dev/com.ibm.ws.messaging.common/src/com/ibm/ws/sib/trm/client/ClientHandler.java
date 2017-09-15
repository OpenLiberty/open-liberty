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

/*
 * Provides an abstract implementation of the Comms ClientComponentHandshake
 * interface
 */

package com.ibm.ws.sib.trm.client;

import com.ibm.ws.sib.comms.ClientComponentHandshake;
import com.ibm.ws.sib.comms.ClientConnection;
import com.ibm.ws.sib.mfp.trm.TrmFirstContactMessage;
import com.ibm.ws.sib.trm.TrmConstants;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.utils.ras.SibTr;


abstract class ClientHandler implements ClientComponentHandshake {

  private static final TraceComponent tc = SibTr.register(ClientHandler.class, TrmConstants.MSG_GROUP, TrmConstants.MSG_BUNDLE);
  
  ClientAttachProperties cap;
  TrmFirstContactMessage fcm;
  Exception exception;
  CredentialType credentialType;

  /*
   * Constructor
   */

  public ClientHandler (ClientAttachProperties cap, CredentialType credentialType) {
    if (tc.isEntryEnabled()) SibTr.entry(tc, "ClientHandler", new Object[] { cap, credentialType });
    
    this.cap = cap;
    this.credentialType = credentialType;
    
    if (tc.isEntryEnabled()) SibTr.exit(tc, "ClientHandler", this);
  }

  /*
   * Method called when a client attachment fails
   */

  public void fail (ClientConnection cc, Throwable t) {
    if (tc.isEntryEnabled()) {
      SibTr.entry(tc, "fail", new Object[]{ cc, t });
      SibTr.exit(tc, "fail");
    }
  }

  /*
   * Get the generic form of reply message
   */

  TrmFirstContactMessage getReply () {
    return fcm;
  }

  /*
   * Accessor methods for 'exception' variable
   */
  
  Exception getException() {
    return exception;
  }

  void setException(Exception exception) {
    this.exception = exception;
  }
  
}
