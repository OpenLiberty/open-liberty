/*******************************************************************************
 * Copyright (c) 2003, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.comms.common;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.ws.ffdc.IncidentStream;
import com.ibm.ws.sib.comms.CommsConstants;
import com.ibm.ws.sib.comms.client.ClientConversationState;
import com.ibm.ws.sib.comms.client.proxyqueue.ProxyQueue;
import com.ibm.ws.sib.comms.client.proxyqueue.impl.ProxyQueueConversationGroupImpl;
import com.ibm.ws.sib.jfapchannel.ClientConnectionManager;
import com.ibm.ws.sib.jfapchannel.Conversation;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.SICoreConnection;

/**
 * This class is an implementation of an FFDC diagnostic module that provides information about
 * outbound conversations.
 * 
 * @author Gareth Matthews
 */
public class ClientCommsDiagnosticModule extends CommsDiagnosticModule
{
   /** Register Class with Trace Component */
   private static final TraceComponent tc = SibTr.register(ClientCommsDiagnosticModule.class,
                                                           CommsConstants.MSG_GROUP,
                                                           CommsConstants.MSG_BUNDLE);
   
   /** The singleton instance */
   private static ClientCommsDiagnosticModule _clientSingleton = null;

   /**
    * @return Returns the singleton instance of this class.
    */
   public static ClientCommsDiagnosticModule getInstance()
   {
      if (_clientSingleton == null)
      {
         _clientSingleton = new ClientCommsDiagnosticModule();
      }
      return _clientSingleton;
   }
   
   /**
    * Protected constructor.
    */
   protected ClientCommsDiagnosticModule()
   {}
   
   /**
    * This method dumps the status of any client conversations that are currently active. It does
    * this by asking the JFap outbound connection tracker for its list of active conversations and
    * then has a look at the conversation states. All this information is captured in the FFDC 
    * incident stream
    * 
    * @param is The incident stream to log information to.
    */
   @Override
   protected void dumpJFapClientStatus(IncidentStream is)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "dumpJFapClientStatus");
      
      ClientConnectionManager ccm = ClientConnectionManager.getRef();
      List obc = ccm.getActiveOutboundConversationsForFfdc();
      
      is.writeLine("\n------ Client Conversation Dump ------ ", ">");
      
      if (obc != null)
      {         
         // Build a map of connection -> conversation so that we can output the
         // connection information once per set of conversations.
         final Map<Object, LinkedList<Conversation>> connectionToConversationMap = convertToMap(is, obc);

         // Go through the map and dump out a connection - followed by its conversations
         for (Iterator<Entry<Object,LinkedList<Conversation>>>i = connectionToConversationMap.entrySet().iterator(); i.hasNext();)
         {
            final Entry<Object, LinkedList<Conversation>>entry = i.next();
            is.writeLine("\nOutbound connection:", entry.getKey());

            LinkedList conversationList = entry.getValue();
            while(!conversationList.isEmpty())
            {
               Conversation c = (Conversation)conversationList.removeFirst();
               is.writeLine("\nOutbound Conversation[" + c.getId() + "]: ", c.getFullSummary());
               try
               {
                  dumpClientConversation(is, c);
               }
               catch(Throwable t)
               {
                  // No FFDC Code Needed
                  is.writeLine("\nUnable to dump conversation", t);
               }
                  
            }
         }
      }
      else
      {
    	  is.writeLine("\nUnable to fetch list of conversations", "");
      }
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "dumpJFapClientStatus");
   }
   
   /**
    * This method does nothing for the client diagnostic module.
    * 
    * @see com.ibm.ws.sib.comms.common.CommsDiagnosticModule#dumpJFapServerStatus(com.ibm.ws.ffdc.IncidentStream)
    */
   @Override
   protected void dumpJFapServerStatus(IncidentStream is)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "dumpJFapServerStatus", is);
      
      is.writeLine("No Server Conversation Dump", "");
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "dumpJFapServerStatus");
   }
   
   /**
    * This method dumps the particulars of a particular client conversation.
    * 
    * @param is
    * @param conv
    * 
    * @throws SIException of the getResolvedUserId() call fails.
    */
   private void dumpClientConversation(IncidentStream is, Conversation conv) throws SIException
   {
      // Get the conversation state
      ClientConversationState convState = (ClientConversationState) conv.getAttachment();
      SICoreConnection siConn = convState.getSICoreConnection();
      
      if (siConn == null)
      {
         is.writeLine("  ** No SICoreConnection in ConversationState **", "(Conversation is initializing?)");
      }
      else
      {
         is.writeLine("  Connected using: ", convState.getCommsConnection());
         is.writeLine("  Connected to ME: ", siConn.getMeName() + " [" + siConn.getMeUuid() + "]");
         is.writeLine("  Resolved UserId: ", siConn.getResolvedUserid());
         is.writeLine("  ME is version ", siConn.getApiLevelDescription());
         
         // Get the proxy queue group
         ProxyQueueConversationGroupImpl pqgroup = 
               (ProxyQueueConversationGroupImpl) convState.getProxyQueueConversationGroup();
         
         if (pqgroup == null)
         {
            is.writeLine("  Number of proxy queues found", "0");
         }
         else
         {
            Map map = pqgroup.getProxyQueues();
   
            // Clone it
            Map idToProxyQueueMap = (Map) ((HashMap) map).clone();
            is.writeLine("  Number of proxy queues found", idToProxyQueueMap.size());
            
            for (Iterator i2 = idToProxyQueueMap.values().iterator(); i2.hasNext();)
            {
               ProxyQueue pq = (ProxyQueue) i2.next();
               is.writeLine("  ", pq);
            }
         }
         
         // Lets also introspect the details of the conversation state
         is.introspectAndWriteLine("Introspection of the conversation state:", convState);
      }
   }
   
   /**
    * Generates a set of mappings between a Connection and the Conversations which are making use of it.
    * 
    * @param is the incident stream to log information to.
    * @param obc a List of Conversation objects
    * 
    * @return A Map where each entry consists of a Connection and its associated Conversations.
    */
   protected Map<Object, LinkedList<Conversation>> convertToMap(final IncidentStream is, final List obc) 
   {
      if(TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "convertToMap", new Object[]{is, obc});
      
      final Map<Object, LinkedList<Conversation>> connectionToConversationMap = new HashMap<Object, LinkedList<Conversation>>();
      
      for(Iterator i = obc.iterator(); i.hasNext();)
      {
         try
         {
            final Conversation c = (Conversation) i.next();
            
            final Object connectionObject = c.getConnectionReference();
            final LinkedList<Conversation> conversationList;
            if (!connectionToConversationMap.containsKey(connectionObject))
            {
               conversationList = new LinkedList<Conversation>();
               connectionToConversationMap.put(connectionObject, conversationList);
            }   
            else
            {
               conversationList = connectionToConversationMap.get(connectionObject);
            }
            conversationList.add(c);
            
         }
         catch(Throwable t)
         {
            // No FFDC Code Needed
            is.writeLine("\nUnable to dump conversation", t);
         }
      }
      
      if(TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "convertToMap", connectionToConversationMap);
      return connectionToConversationMap;
   }
}
