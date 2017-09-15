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

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.comms.CommsConstants;
import com.ibm.ws.sib.comms.client.BrowserSessionProxy;
import com.ibm.ws.sib.comms.client.DestinationSessionProxy;
import com.ibm.ws.sib.comms.client.proxyqueue.BrowserProxyQueue;
import com.ibm.ws.sib.comms.client.proxyqueue.queue.QueueData;
import com.ibm.ws.sib.comms.client.proxyqueue.queue.ReadAheadQueue;
import com.ibm.ws.sib.comms.common.CommsByteBuffer;
import com.ibm.ws.sib.jfapchannel.Conversation;
import com.ibm.ws.sib.mfp.JsMessage;
import com.ibm.ws.sib.mfp.MessageDecodeFailedException;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException;
import com.ibm.wsspi.sib.core.exception.SIConnectionLostException;
import com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException;
import com.ibm.wsspi.sib.core.exception.SINotAuthorizedException;
import com.ibm.wsspi.sib.core.exception.SISessionDroppedException;
import com.ibm.wsspi.sib.core.exception.SISessionUnavailableException;

/**
 * An implementation of BrowserProxyQueue, suitable for use with
 * browser sessions.
 */
public class BrowserProxyQueueImpl implements BrowserProxyQueue
{
   /** Class name for FFDC's */
   private static String CLASS_NAME = BrowserProxyQueueImpl.class.getName();
   
   /** Trace */
   private static final TraceComponent tc = SibTr.register(BrowserProxyQueueImpl.class, 
                                                           CommsConstants.MSG_GROUP, 
                                                           CommsConstants.MSG_BUNDLE);
   
   /** NLS handle */
   private static final TraceNLS nls = TraceNLS.getTraceNLS(CommsConstants.MSG_BUNDLE);

   static
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Source info: @(#) SIB/ws/code/sib.comms.client.impl/src/com/ibm/ws/sib/comms/client/proxyqueue/impl/BrowserProxyQueueImpl.java, SIB.comms, WASX.SIB, uu1215.01 1.29");
   }
   
   // The unique ID for this proxy queue.
   private short proxyQueueId;
   
   // A conversation helper which is used to communicate with the ME.
   private ConversationHelper convHelper;
   
   // The queue which implements the underlying queuing behaviour
   // required by the browser proxy queue.
   private ReadAheadQueue queue;
   
   // The browser session proxy object this queue is proxying
   // messages for.
   private BrowserSessionProxy browserSession = null;
   
   // The group which owns this proxy queue (notified when closing).
   private ProxyQueueConversationGroupImpl owningGroup;
   
   // Flag to indicate whether we have been closed
   private volatile boolean closed = false;              // d176016
   
   /**
    * Creates a browser proxy queue.
    * 
    * @param group The conversation group which created this group
    * and should be notified when it is closed.
    * @param id The unique ID to assign to this proxy queue.
    * @param conversation The conversation this proxy queue will use
    * when it needs to communicate with the ME.
    */
   public BrowserProxyQueueImpl(ProxyQueueConversationGroupImpl group,
                                short id, Conversation conversation)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "<init>", new Object[] {group, id, conversation});
      owningGroup = group;
      convHelper = new ConversationHelperImpl(conversation, (short)0);
      proxyQueueId = id;
      // Use a read ahead proxy queue as it's pacing also matches
      // the requirements for a browser proxy queue.
      queue = new ReadAheadQueue(id, convHelper);    
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "<init>");
   }

   /**
    * Creates a browser proxy queue (only) suitable for testing.
    * This constructor allows an alternative conversation helper to
    * be specified.
    * 
    * @param group
    * @param id
    * @param convHelper
    */   
   protected BrowserProxyQueueImpl(ProxyQueueConversationGroupImpl group,
                                   short id, ConversationHelper convHelper)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "<init>", new Object[] {group, id, convHelper});
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "test form of constructor invoked");
      owningGroup = group;      
      proxyQueueId = id;
      this.convHelper = convHelper;
      queue = new ReadAheadQueue(id, this.convHelper);
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "<init>");
   }

   /**
    * Returns the next message for a browse.
    * @see com.ibm.ws.sib.comms.client.proxyqueue.BrowserProxyQueue#next()
    */
   public JsMessage next()
      throws MessageDecodeFailedException,
             SISessionUnavailableException, SISessionDroppedException,
             SIConnectionUnavailableException, SIConnectionDroppedException,
             SIResourceException, SIConnectionLostException, 
             SIErrorException,
             SINotAuthorizedException
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "next");
      JsMessage retMsg = null;
	   
      // begin D249096 
      retMsg = queue.get(proxyQueueId);
      
      if (retMsg == null)
      {
         convHelper.flushConsumer();
         
         retMsg = queue.get(proxyQueueId);
      }
      // end D249096
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "next", retMsg);
      return retMsg;
	}

   /**
    * Closes the proxy queue.
    * 
    * @see com.ibm.ws.sib.comms.client.proxyqueue.BrowserProxyQueue#close()
    */
   public void close()
      throws SIResourceException, SIConnectionLostException,
             SIErrorException, SIConnectionDroppedException
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "close");

      if (!closed)
      {
         // begin D249096
         convHelper.closeSession();
         queue.purge(proxyQueueId);
         owningGroup.notifyClose(this);
         closed = true;
         // end D249096
      }
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "close");
   }

   /**
    * Returns the unique ID associated with this proxy queue.
    * @see com.ibm.ws.sib.comms.client.proxyqueue.ProxyQueue#getId()
    */
   public short getId()
   {
      return proxyQueueId;
   }

   /**
    * Places a browse message on the queue.
    * @see com.ibm.ws.sib.comms.client.proxyqueue.ProxyQueue#put(com.ibm.ws.sib.comms.common.CommsByteBuffer, short, boolean, boolean)
    */
   public void put(CommsByteBuffer msgBuffer, short msgBatch, boolean lastInBatch, boolean chunk)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "put", new Object[]{msgBuffer, msgBatch, lastInBatch, chunk});
      
      QueueData queueData = null;
      // If this data represents a chunk we need to read the flags from the buffer. This 
      // will indicate to us whether we need to create some new queue data to stash on the 
      // queue or 
      if (chunk)
      {
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Dealing with a chunked message");
         
         // Get the flags from the buffer
         byte flags = msgBuffer.get();
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Flags:", ""+flags);
         
         // Is this the first chunk?
         if ((flags & CommsConstants.CHUNKED_MESSAGE_FIRST) == CommsConstants.CHUNKED_MESSAGE_FIRST)
         {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "First chunk received");
            
            // If it is, create some new queue data to place on the queue with the initial
            // chunk
            queueData = new QueueData(this, lastInBatch, chunk, msgBuffer);
            
            // Put the data to the queue
            queue.put(queueData, msgBatch);
         }
         else
         {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Middle / Last chunk received");
            
            // Otherwise, we need to append to the chunks already collected. We do this by
            // finding the chunk to append to. This will be the last message on the queue
            // (i.e. at the back). This works for all cases as an async consumer cannot be
            // driven concurrently (so the last incomplete message on the queue will be the
            // one we want).
            boolean lastChunk = ((flags & CommsConstants.CHUNKED_MESSAGE_LAST) == CommsConstants.CHUNKED_MESSAGE_LAST);
            queue.appendToLastMessage(msgBuffer, lastChunk);
         }
      }
      else
      {
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Dealing with the entire message");
         
         queueData = new QueueData(this, lastInBatch, chunk, msgBuffer);
         
         // Put the data to the queue
         queue.put(queueData, msgBatch);
      }
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "put");
   }

   /**
    * Sets the browser session this proxy queue is being used in
    * conjunction with.
    * @see com.ibm.ws.sib.comms.client.proxyqueue.BrowserProxyQueue#setBrowserSession(BrowserSessionProxy)
    */
   public void setBrowserSession(BrowserSessionProxy browserSession)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "setBrowserSession", browserSession);
      
      if (this.browserSession != null)
      {
         // We are flagging this here as we should never call setBrowserSession() twice. 
         // A proxy queue is associated with a session for the lifetime of the session and calling
         // it twice is badness.
         SIErrorException e = new SIErrorException(
            nls.getFormattedMessage("RESET_OF_BROWSER_SESSION_SICO1035", null, null)
         );
         
         FFDCFilter.processException(e, CLASS_NAME + ".setBrowserSession",
                                     CommsConstants.BROWSERPQ_SETBROWSERSESS_01, this);
         
         throw e;
      }
      
      if (browserSession == null)
      {
         // You can't pass in a null you complete badger
         SIErrorException e = new SIErrorException(
            nls.getFormattedMessage("NULL_BROWSER_SESSION_SICO1036", null, null)
         );
         
         FFDCFilter.processException(e, CLASS_NAME + ".setBrowserSession",
                                     CommsConstants.BROWSERPQ_SETBROWSERSESS_02, this);
         
         throw e;         
      }
      
      this.browserSession = browserSession;
      convHelper.setSessionId(browserSession.getProxyID());
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "setBrowserSession");
   }
   
   /**
    * Implementation of the browser session reset action.  This is supposed to reset the
    * browse cursor - however, the implication to the proxy queue is that it should
    * be purged of messages and the batch count incremented.
    * 
    * @throws SISessionUnavailableException 
    * @throws SISessionDroppedException 
    * @throws SIConnectionUnavailableException 
    * @throws SIConnectionDroppedException 
    * @throws SIResourceException 
    * @throws SIConnectionLostException 
    * @throws SIErrorException 
    */
   // begin F171893
   public void reset()
      throws SISessionUnavailableException, SISessionDroppedException,
             SIConnectionUnavailableException, SIConnectionDroppedException,
             SIResourceException, SIConnectionLostException, 
             SIErrorException
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "reset");
      convHelper.exchangeResetBrowse();
      queue.purge(proxyQueueId);                                     // D249096
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "reset");
   }
   // end F171893
   
   // Start D209401
   /**
    * @return Returns some information about the proxy queue and its state.
    */
   public String toString()
   {
      return "BrowserPQ@" + Integer.toHexString(hashCode()) + ": " + queue.toString();
   }
   // End D209401

   /** @see com.ibm.ws.sib.comms.client.proxyqueue.ProxyQueue#getConversationHelper() */
   public ConversationHelper getConversationHelper()
   {
      return convHelper;
   }

   /** @see com.ibm.ws.sib.comms.client.proxyqueue.ProxyQueue#getDestinationSessionProxy() */
   public DestinationSessionProxy getDestinationSessionProxy()
   {
      return browserSession;
   }

   /** @see com.ibm.ws.sib.comms.client.proxyqueue.ProxyQueue#conversationDroppedNotification() */
   public void conversationDroppedNotification()
   {
      // We don't care about this event.      
   }

}
