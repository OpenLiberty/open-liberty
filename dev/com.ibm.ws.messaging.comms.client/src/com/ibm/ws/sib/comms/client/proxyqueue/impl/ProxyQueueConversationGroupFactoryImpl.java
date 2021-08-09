/*******************************************************************************
 * Copyright (c) 2004, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.sib.comms.client.proxyqueue.impl;

import java.util.HashMap;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.comms.CommsConstants;
import com.ibm.ws.sib.comms.client.proxyqueue.ProxyQueueConversationGroup;
import com.ibm.ws.sib.comms.client.proxyqueue.ProxyQueueConversationGroupFactory;
import com.ibm.ws.sib.jfapchannel.Conversation;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * Implementation of the proxy queue conversation group factory.
 * A factory for proxy queue conversation groups.
 */
public class ProxyQueueConversationGroupFactoryImpl extends ProxyQueueConversationGroupFactory
{
   /** Class name for FFDC's */
   private static String CLASS_NAME = ProxyQueueConversationGroupFactoryImpl.class.getName();
   
   /** Trace */
   private static final TraceComponent tc = SibTr.register(ProxyQueueConversationGroupFactory.class, 
                                                           CommsConstants.MSG_GROUP, 
                                                           CommsConstants.MSG_BUNDLE);
                                                           
   /** NLS handle */
   private static final TraceNLS nls = TraceNLS.getTraceNLS(CommsConstants.MSG_BUNDLE);

   static
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Source info: @(#) SIB/ws/code/sib.comms.client.impl/src/com/ibm/ws/sib/comms/client/proxyqueue/impl/ProxyQueueConversationGroupFactoryImpl.java, SIB.comms, WASX.SIB, uu1215.01 1.12");
   }
   
   // Maps conversations to proxy queue groups.
   private HashMap<Conversation, ProxyQueueConversationGroup> convToGroupMap;
   
   /** Constructor */
   public ProxyQueueConversationGroupFactoryImpl()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "<init>");
      convToGroupMap = new HashMap<Conversation, ProxyQueueConversationGroup>();
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "<init>");
   }
   
   /**
    * Creates a proxy queue conversation group.
    * @see com.ibm.ws.sib.comms.client.proxyqueue.ProxyQueueConversationGroupFactory#create(com.ibm.ws.sib.jfapchannel.Conversation)
    */
   public synchronized ProxyQueueConversationGroup create(Conversation conversation)
      throws IllegalArgumentException
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "create", conversation);
      
      if (convToGroupMap.containsKey(conversation))
      {
         // A proxy queue conversation group is associated one-to-one with a conversation. In this
         // case someone has tried to create 2 for the same conversation.
         SIErrorException e = new SIErrorException(
            nls.getFormattedMessage("PQGROUP_ALREADY_CREATED_SICO1054", null, null)
         );
            
         FFDCFilter.processException(e, CLASS_NAME + ".create",
                                     CommsConstants.PQCONVGRPFACTIMPL_CREATE_01, this);
         
         throw e;
      }
      
      ProxyQueueConversationGroup group =
         new ProxyQueueConversationGroupImpl(conversation, this);
      
      convToGroupMap.put(conversation, group);
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "create", group);
      return group;
   }

   /**
    * Called by a proxy queue conversation group to notify this
    * factory of its closure.
    * @param conversation
    * @param group
    */
   protected synchronized void groupCloseNotification(Conversation conversation, 
                                                      ProxyQueueConversationGroup group)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "groupCloseNotification", new Object[] {conversation, group});
      if (convToGroupMap.remove(conversation) == null)
      {
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "group unknown!");
      }
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "groupCloseNotification");
   }
}
