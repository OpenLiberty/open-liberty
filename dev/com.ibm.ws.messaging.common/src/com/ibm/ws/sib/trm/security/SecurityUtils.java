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
package com.ibm.ws.sib.trm.security;

import javax.security.auth.Subject;

/**
 * This interface contains helper methods for the Trm CredentialType class.
 */
public interface SecurityUtils 
{
  /* ------------------------------------------------------------------------ */
  /* isSIBServerSubject method                                    
  /* ------------------------------------------------------------------------ */
  /**
   * @param id the subject to query
   * @return true if the subject is the server subject
   */
  public boolean isSIBServerSubject(Subject id);
  /* ------------------------------------------------------------------------ */
  /* getOpaqueAuthorizationToken method                                    
  /* ------------------------------------------------------------------------ */
  /**
   * @param busName the name of the bus
   * @param meName  the name of the messaging engine
   * @param clientIdentity the subject to conver to an OAT
   * @return the OAT.
   */
  public byte[] getOpaqueAuthorizationToken(String busName, String meName, Subject clientIdentity);
}
