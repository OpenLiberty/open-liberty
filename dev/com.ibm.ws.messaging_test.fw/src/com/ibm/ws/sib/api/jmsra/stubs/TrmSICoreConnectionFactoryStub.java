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
 
package com.ibm.ws.sib.api.jmsra.stubs;

import java.util.Map;

import javax.security.auth.Subject;

import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.wsspi.sib.core.SICoreConnection;
import com.ibm.wsspi.sib.core.exception.SIAuthenticationException;
import com.ibm.wsspi.sib.core.exception.SINotAuthorizedException;
import com.ibm.ws.sib.comms.ClientConnection;
import com.ibm.ws.sib.trm.TrmSICoreConnectionFactory;

/**
 * Stub class for TrmSICoreConnectionFactory.
 */
public class TrmSICoreConnectionFactoryStub extends TrmSICoreConnectionFactory
{

  @Override
public SICoreConnection createConnection(String userName, String password, Map properties)
  {
    return new SICoreConnectionStub(userName, password, properties);
  }

  @Override
public SICoreConnection createConnection(Subject subject, Map properties)
  {
    return new SICoreConnectionStub(subject, properties);
  }

/** {@inheritDoc} */
@Override
public SICoreConnection createConnection(ClientConnection cc, String credentialType, String userid, String password) throws SIResourceException, SINotAuthorizedException, SIAuthenticationException {
    // TODO Auto-generated method stub
    return null;
}
  
}
