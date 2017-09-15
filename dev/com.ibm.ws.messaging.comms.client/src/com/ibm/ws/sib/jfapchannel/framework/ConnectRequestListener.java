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
package com.ibm.ws.sib.jfapchannel.framework;

/**
 * A listener for events pertaining to the establishment of a network connection.
 * Typically, a user of this package will obtain a NetworkConnection implementation
 * and invoke the connectAsynch method supplying an implementation of this class.
 * The implementation of this class will then be used to provide notification
 * as to the successful (or otherwise) establishment of the network connection.
 * @see com.ibm.ws.sib.jfapchannel.framework.NetworkConnection
 */
public interface ConnectRequestListener
{
   /**
    * Invoked to provide notification that a connect request has completed
    * succesfully.
    * @param networkConnection the network connection that has sucesfully been
    * established.
    */
   void connectRequestSucceededNotification(NetworkConnection networkConnection);
   
   /**
    * Invoked to provide notification that a connect request has failed to
    * be sucessfully completeted.
    * @param exception an exception with information about the condition which
    * caused the network connect request to fail.
    */
   void connectRequestFailedNotification(Exception exception);
}
