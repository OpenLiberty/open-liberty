/*******************************************************************************
 * Copyright (c) 2004, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.comms.client;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.comms.CommsConnection;
import com.ibm.ws.sib.comms.CommsConstants;
import com.ibm.ws.sib.comms.client.proxyqueue.ProxyQueueConversationGroup;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.SICoreConnection;

/**
 * @author Niall
 *
 * Used by the Comms Client code to store state per JFAP Conversation.
 */
public class ClientConversationState
{
   /** Request Number (Unique to Connection) */
   private int reqnum = 0;

   /** Stores the ID of the SIMPConnection object on the Server */
   private int connectionObjectID = -1;

   /** 
    * Stores the proxy group queue associated with this conversation.
    * This isn't always accessed under the same lock so marked as volatile. 
    */
   private volatile ProxyQueueConversationGroup proxyGroup = null;

   /** Store the connection listener group associated with this conversation */
   private CatConnectionListenerGroup catConnectionGroup = null;

   /** Store the SICoreConnection associated with this Conversation */
   private SICoreConnection siCoreConnection = null;

   /** High Level CommsConnection associated with the Conversation */
   private CommsConnection cc = null;

   /** Register Class with Trace Component */
   private static final TraceComponent tc = SibTr.register(ClientConversationState.class,
                                                           CommsConstants.MSG_GROUP,
                                                           CommsConstants.MSG_BUNDLE);

   /** Log Source code level on static load of class */
   static
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Source info: @(#)SIB/ws/code/sib.comms.client.impl/src/com/ibm/ws/sib/comms/client/ClientConversationState.java, SIB.comms, WASX.SIB, cf011121.09 1.28");
   }

   /**
    * Constructor.
    */
   public ClientConversationState()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "<init>");

      catConnectionGroup= new CatConnectionListenerGroup();

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "<init>");
   }

   /**
    * @return Returns a request number unique to a particular conversation.
    */
   public synchronized int getUniqueRequestNumber()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getUniqueRequestNumber");

      if (++reqnum > Short.MAX_VALUE)
         reqnum = 1;

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getUniqueRequestNumber", ""+reqnum);
      return reqnum;
   }

   /**
    * @return Returns the Connection ID referring to the SIMPConnection
    * Object on the Server.
    */
   public int getConnectionObjectID()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getConnectionObjectID");
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getConnectionObjectID", ""+connectionObjectID);
      return connectionObjectID;
   }

   /**
    * Sets the Connection ID referring to the SIMPConnection Object on the server.
    *
    * @param i
    */
   public void setConnectionObjectID(int i)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "setConnectionObjectID", ""+i);

      connectionObjectID = i;

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "setConnectionObjectID");
   }

   /**
    * Gets the proxy queue group associated with this conversation.
    *
    * @return ProxyQueueConversationGroup
    */
   public ProxyQueueConversationGroup getProxyQueueConversationGroup()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getProxyQueueConversationGroup");
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getProxyQueueConversationGroup", proxyGroup);
      return proxyGroup;
   }

   /**
    * Sets the proxy queue group associated with this conversation.
    *
    * @param proxyGroup
    */
   public void setProxyQueueConversationGroup(ProxyQueueConversationGroup proxyGroup)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "setProxyQueueConversationGroup", proxyGroup);

      this.proxyGroup = proxyGroup;

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "setProxyQueueConversationGroup");
   }

   /**
    * Gets the connection listener group associated with thisconversation
    *
    * @return CatConnectionListenerGroup
    */
   public CatConnectionListenerGroup getCatConnectionListeners()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getCatConnectionListeners");
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getCatConnectionListeners", catConnectionGroup);
      return catConnectionGroup;
   }

   /**
    * Returns the SICoreConnection in use with this conversation
    *
    * @return SICoreConnection
    */
   public SICoreConnection getSICoreConnection()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getSICoreConnection");
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getSICoreConnection", siCoreConnection);
      return siCoreConnection;
   }

   /**
    * Sets the SICoreConnection in use with this conversation
    *
    * @param siCoreConnection
    */
   public void setSICoreConnection(SICoreConnection siCoreConnection)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "setSICoreConnection", siCoreConnection);

      this.siCoreConnection = siCoreConnection;

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "setSICoreConnection");
   }

   /**
    * @return Returns the CommsConnection associated with the Conversation
    */
   public CommsConnection getCommsConnection()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getCommsConnection");
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getCommsConnection", cc);
      return cc;
   }

   /**
    * Sets the CommsConnection associated with the Conversation
    *
    * @param cc
    */
   public void setCommsConnection(CommsConnection cc)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "setCommsConnection", cc);
      this.cc = cc;
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "setCommsConnection");
   }

  // SIB0137.comms.2 start

   /**
    * Get the DestinationListenerCache associated with this conversation
    */
  private volatile DestinationListenerCache destinationListenerCache = null;

  public DestinationListenerCache getDestinationListenerCache () {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getDestinationListenerCache");

    if (destinationListenerCache == null) { // double-checked lock idiom works in Java 1.5 with a volatile
      synchronized (this) {
        if (destinationListenerCache == null) destinationListenerCache = new DestinationListenerCache();
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getDestinationListenerCache", destinationListenerCache);
    return destinationListenerCache;
  }

  // SIB0137.comms.2 end
  
//F011127 START

  /**
   * Get the ConsumerMonitorListenerCache associated with this conversation
   */
 private volatile ConsumerMonitorListenerCache consumerMonitorListenerCache = null;

 public ConsumerMonitorListenerCache getConsumerMonitorListenerCache() {
   if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getConsumerMonitorListenerCache");

   if (consumerMonitorListenerCache == null) { 
     synchronized (this) {
       if (consumerMonitorListenerCache == null) consumerMonitorListenerCache = new ConsumerMonitorListenerCache();
     }
   }

   if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getConsumerMonitorListenerCache", consumerMonitorListenerCache);
   return consumerMonitorListenerCache;
 }

 //F011127 END
}
