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
package com.ibm.ws.sib.comms;


import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * Factory class for ClientConnection objects.  Intended for use in the
 * client code by TRM.
 */
public abstract class ClientConnectionFactory
{
   /** Class name for FFDC's */
   private static String CLASS_NAME = ClientConnectionFactory.class.getName();

   private static final TraceComponent tc =
      SibTr.register(
         ClientConnectionFactory.class,
         CommsConstants.MSG_GROUP,
         CommsConstants.MSG_BUNDLE);


   /**
    * TODO: comment
    * 
    * Creates an instance of a class which implements the ClientConnection
    * interface.
    * @return ClientConnection
    */
   public abstract ClientConnection createClientConnection();

}
