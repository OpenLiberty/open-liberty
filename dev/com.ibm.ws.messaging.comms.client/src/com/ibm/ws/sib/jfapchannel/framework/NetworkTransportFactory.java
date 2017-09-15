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
 * A factory which returns instances of NetworkConnectionFactory.  The instance of
 * NetworkConnectionFactory returned is based on the transport requested (which is
 * another way of saying - which protocol, TCP, SSL etc. you want).
 */
public interface NetworkTransportFactory
{
   /**
    * @param chainName a chain name used to select the transport.
    * @return a network connection factory for the specified transport, or null
    * if the transport it not supported.
    * 
    * @throws FrameworkException if an error occurs getting the connection factory
    */
   NetworkConnectionFactory getOutboundNetworkConnectionFactoryByName(String chainName) throws FrameworkException;

   /**
    * @param endPoint an end point used to select the transport (this must be
    * an instance of XMLEndPoint).
    * @return a network connection factory for the specified transport, or null
    * if the transport it not supported.
    */
   NetworkConnectionFactory getOutboundNetworkConnectionFactoryFromEndPoint(Object endPoint);
}
