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
 SICoreConnectionAdapter is an abstract class providing empty implementations 
 of the methods of SICoreConnectionListener (following the AWT pattern). It is
 provided so that applications that wish to provide implementations of one or 
 two of the methods of SICoreConnectionListener are not required to provide
 empty implementations of the other methods. 
 <p>
 This class has no security implications.
 
*/
public abstract class SICoreConnectionAdapter implements SICoreConnectionListener {
	
  /**
   This method is called to deliver exceptions to an application that is using
   a ConsumerSesion in asynchronous delivery mode, since the Core API 
   implementation is unable to deliver exceptions to the application by 
   throwing them in the normal way.
   
   @param consumer the ConsumerSession 
   @param exception the exception that occurred
   
  */
  public void asynchronousException(
    ConsumerSession consumer,
    Throwable exception) {}

  /**
   This method is called when the Messaging Engine to which the connection is
   attached begins quiescing, and wishes to notify applications that they 
   should disconnect in preparation for graceful shutdown of the engine.
   
   @param conn the connection to the messaging engine that is quiescing
   
  */
  public void meQuiescing(
    SICoreConnection conn) {}
			
  /**
   This method is called when a connection is closed due to a communications
   failure. Examine the SICommsException to determine the nature of the failure.
   
   @param conn the connection to the messaging engine that is quiescing
   @param exception an exception providing information concerning the failure
   
  */
  public void commsFailure(
    SICoreConnection conn,
    SIConnectionLostException exception) {}
	
  /**
   This method is called when a connection is closed because the Messaging 
   Engine has terminated.
   
   @param conn the connection that has been closed
   
  */
  public void meTerminated(
    SICoreConnection conn) {}
}
