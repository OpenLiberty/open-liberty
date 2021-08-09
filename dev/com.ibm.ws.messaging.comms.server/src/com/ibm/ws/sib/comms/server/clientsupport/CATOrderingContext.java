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
package com.ibm.ws.sib.comms.server.clientsupport;

import com.ibm.wsspi.sib.core.OrderingContext;

/**
 * This class encapsulates an ordering context. At first glance it may seem a pointless 
 * encapsulation - but we need to dispatch in the JFap channel by message order context and so this
 * class needs to extend the CATCommonDispatchable to allow the JFap to do this.
 * 
 * @author Gareth Matthews
 */
public class CATOrderingContext extends CATCommonDispatchable
{
   /** The ordering context */
   private OrderingContext orderContet = null;
   
   /**
    * Constructor.
    * 
    * @param orderContext
    */
   public CATOrderingContext(OrderingContext orderContext)
   {
      this.orderContet = orderContext;
   }
   
   /**
    * @return Returns the ordering context.
    */
   public OrderingContext getOrderingContext()
   {
      return orderContet;
   }
}
