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
package com.ibm.ws.sib.comms.client.proxyqueue;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.comms.CommsConstants;
import com.ibm.ws.sib.comms.client.proxyqueue.impl.ProxyQueueConversationGroupFactoryImpl;
import com.ibm.ws.sib.jfapchannel.Conversation;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * A factory for proxy queue conversation groups.
 * @see com.ibm.ws.sib.comms.client.proxyqueue.ProxyQueueConversationGroup
 */
public abstract class ProxyQueueConversationGroupFactory
{
   private static final TraceComponent tc = SibTr.register(ProxyQueueConversationGroupFactory.class, CommsConstants.MSG_GROUP, CommsConstants.MSG_BUNDLE);

   private static ProxyQueueConversationGroupFactory instance = null;
   
   static
   {
      if (tc.isDebugEnabled()) SibTr.debug(tc, "Source info: @(#) SIB/ws/code/sib.comms.client.impl/src/com/ibm/ws/sib/comms/client/proxyqueue/ProxyQueueConversationGroupFactory.java, SIB.comms, WASX.SIB, uu1215.01 1.8");
      instance = new ProxyQueueConversationGroupFactoryImpl();
   }
   
   public static ProxyQueueConversationGroupFactory getRef()
   {
      return instance;
   }
   
   /**
    * Creates a new conversation group for the specified conversation.
    * @param conversation The conversation to create the group for.
    * @return ProxyQueueConversationGroup The group created.
    * @throws IllegalArgumentException Thrown if an group already
    * exists for the specified conversation.
    */
   public abstract ProxyQueueConversationGroup create(Conversation conversation)
   throws IllegalArgumentException;

}
