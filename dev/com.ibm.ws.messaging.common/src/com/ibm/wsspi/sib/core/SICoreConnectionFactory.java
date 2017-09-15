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

import java.util.Map;
import javax.security.auth.Subject;

import com.ibm.websphere.sib.exception.SIIncorrectCallException;
import com.ibm.websphere.sib.exception.SINotPossibleInCurrentConfigurationException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.sib.comms.ClientConnection;
import com.ibm.wsspi.sib.core.SICoreConnection;
import com.ibm.wsspi.sib.core.exception.SIAuthenticationException;
import com.ibm.wsspi.sib.core.exception.SIConnectionLostException;
import com.ibm.wsspi.sib.core.exception.SILimitExceededException;
import com.ibm.wsspi.sib.core.exception.SINotAuthorizedException;

/**
 SICoreConnectionFactory is used to create SICoreConnections. It is implemented 
 by SIB.processor and SIB.trm. A SIB.processor implementation can be 
 obtained by calling JsMessagingEngine.getMessageProcessor, and may be used to 
 obtain connections to the corresponding messaging engine. The SIB.trm 
 implementation may be obtained by calling 
 JsAdminService.getProcessComponent(trm), and may be used to obtain connections 
 to arbitrary messaging engines. The SIB.trm implementation may also be 
 instantiated directly, for example in client environments; for more 
 information, see the TRM design doc.
*/
public interface SICoreConnectionFactory {
	
  /**
   Creates a (logical) connection to a Messaging Engine (ME).
   <p>
   The SIB.processor implementation of SICoreConnection can only 
   create connections to a single local messaging engine. The SIB.trm 
   implementation uses the connectionProperties to determine the messaging 
   engine to which the connection should be made. For details of the properties 
   that should be supplied, see the TRM design doc. As a side effect of creating 
   a new logical connection, a new physical connection may be created (for 
   example when creating the first connection to a particular ME).
   <p>
   The following only applies when running in a secure bus:
   <p>
   The password is used to authenticate the supplied userName. If the password
   is incorrect or the userName is not recognised the connection fails and an
   SIAuthenticationException exception is thrown. 
   <p>
   If the authenticated userName has not been granted access to the bus (they have
   not been assigned the Connecter role) the connection will fail with an
   SINotAuthorizedException.
   <p>
   If authorized to connect, the authenticated user is used for all subsequent
   authorisation checks performed under the returned SICoreConnection unless otherwise
   specified (see SICoreConnection for further details).
   <p>
   If the supplied userName and password are both null or empty strings the user
   will be treated as an unauthenticated user.
   
   @param userName the name of a user in the WAS user registry
   @param password the password for the user in the WAS user registry
   @param connectionProperties used to determine the messaging engine to which 
   the connection should be made

   @return a new logical connection to a messaging engine

   @throws com.ibm.websphere.sib.exception.SIResourceException
   @throws com.ibm.wsspi.sib.core.exception.SIConnectionLostException
   @throws com.ibm.wsspi.sib.core.exception.SILimitExceededException
   @throws com.ibm.wsspi.sib.core.exception.SINotAuthorizedException
   @throws com.ibm.websphere.sib.exception.SINotPossibleInCurrentConfigurationException
   @throws com.ibm.websphere.sib.exception.SIIncorrectCallException
   @throws com.ibm.wsspi.sib.core.exception.SIAuthenticationException
  */
  public SICoreConnection createConnection(
      String userName, 
      String password, 
      Map connectionProperties)
    throws SIResourceException, SIConnectionLostException, SILimitExceededException, 
           SINotAuthorizedException,
           SINotPossibleInCurrentConfigurationException,
           SIIncorrectCallException,
           SIAuthenticationException;
		   
  /**
   Creates a (logical) connection to a MessagingEngine.
   <p>
   An authenticated JAAS Subject with permission to perform the operation must 
   be supplied. If this createConnection method is invoked in a remote client, 
   then SIMPNotAuthenticatedException will always be thrown - only userName and 
   password are supported in remote clients for Jetstream 1.
   <p>
   The following only applies when running in a secure bus:
   <p>
   If the authenticated subject has not been granted access to the bus, they have
   not been assigned the Connecter role, the connection will fail with an
   SINotAuthorizedException.
   <p>
   If authorized to connect, the subject is used for all subsequent authorisation
   checks performed under the returned SICoreConnection unless otherwise specified
   (see SICoreConnection for further details). 
 
   @param subject an authenticated JAAS subject must be supplied
   @param connectionProperties used to determine the messaging engine to which 
   the connection should be made

   @return a new logical connection to a messaging engine

   @throws com.ibm.websphere.sib.exception.SIResourceException
   @throws com.ibm.wsspi.sib.core.exception.SIConnectionLostException
   @throws com.ibm.wsspi.sib.core.exception.SILimitExceededException
   @throws com.ibm.wsspi.sib.core.exception.SINotAuthorizedException
   @throws com.ibm.websphere.sib.exception.SINotPossibleInCurrentConfigurationException
   @throws com.ibm.websphere.sib.exception.SIIncorrectCallException
   @throws com.ibm.wsspi.sib.core.exception.SIAuthenticationException
  */
  public SICoreConnection createConnection(
      Subject subject,
      Map connectionProperties)
    throws SIResourceException, SIConnectionLostException, SILimitExceededException, 
           SINotAuthorizedException,
           SINotPossibleInCurrentConfigurationException,
           SIIncorrectCallException,
           SIAuthenticationException;
  
  // Security code changes for Liberty Comms: Sharath Start
  /**
   * Create Connection call from TrmSingleton, this is to bypass the security calls from Comms Server
   * @param cc
   * @param credentialType
   * @param userid
   * @param password
   * @return
   * @throws SIResourceException
   * @throws SINotAuthorizedException
   * @throws SIAuthenticationException
   */
  public SICoreConnection createConnection(ClientConnection cc,
			String credentialType, String userid, String password)
			throws SIResourceException, SINotAuthorizedException,
			SIAuthenticationException;
  // Security code changes for Liberty Comms: Sharath End
}
