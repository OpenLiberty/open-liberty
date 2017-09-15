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

import com.ibm.wsspi.sib.core.exception.SIConnectionLostException;

/**
 A connection listener is implemented by the API layer, in order to receive
 notification of events in which it may be interested. A particularly 
 important class of events are exceptions that occur during asynchronous 
 delivery, as there is no other mechanism by which the client to the core API
 can receive such exceptions.
 <p>
 This class has no security implications.
*/
public interface SICoreConnectionListener {
	
  /**
   This method is called to deliver exceptions to an application that is using
   a ConsumerSession in asynchronous delivery mode, since the Core API 
   implementation is unable to deliver exceptions to the application by 
   throwing them in the normal way.
   
   @param consumer the ConsumerSession 
   @param exception the exception that occurred
   
  */
  public void asynchronousException(
    ConsumerSession consumer,
    Throwable exception);

  /**
   This method is called when the Messaging Engine to which the connection is
   attached begins quiescing, and wishes to notify applications that they 
   should disconnect in preparation for graceful shutdown of the engine.
   
   @param conn the connection to the messaging engine that is quiescing
   
  */
  public void meQuiescing(
    SICoreConnection conn);
  
  /**
   This method is called when a connection is closed due to a communications
   failure. Examine the SICommsException to determine the nature of the failure.
   
   @param conn the connection to the messaging engine that is quiescing
   @param exception an exception providing information concerning the failure
   
  */
  public void commsFailure(
    SICoreConnection conn,
    SIConnectionLostException exception);

  /**
   This method is called when a connection is closed because the Messaging 
   Engine has terminated.
   
   @param conn the connection that has been closed
   
  */
  public void meTerminated(
    SICoreConnection conn);
}

