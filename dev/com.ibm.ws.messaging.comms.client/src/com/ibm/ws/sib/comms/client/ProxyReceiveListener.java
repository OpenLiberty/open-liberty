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
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.sib.SIDestinationAddress;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.comms.CommsConnection;
import com.ibm.ws.sib.comms.CommsConstants;
import com.ibm.ws.sib.comms.CompHandshake;
import com.ibm.ws.sib.comms.client.proxyqueue.AsynchConsumerProxyQueue;
import com.ibm.ws.sib.comms.client.proxyqueue.ProxyQueue;
import com.ibm.ws.sib.comms.client.proxyqueue.ProxyQueueConversationGroup;
import com.ibm.ws.sib.comms.common.CommsByteBuffer;
import com.ibm.ws.sib.comms.common.CommsByteBufferPool;
import com.ibm.ws.sib.jfapchannel.Conversation;
import com.ibm.ws.sib.jfapchannel.ConversationReceiveListener;
import com.ibm.ws.sib.jfapchannel.Dispatchable;
import com.ibm.ws.sib.jfapchannel.HandshakeProperties;
import com.ibm.ws.sib.jfapchannel.JFapChannelConstants;
import com.ibm.ws.sib.jfapchannel.buffer.WsByteBuffer;
import com.ibm.ws.sib.mfp.impl.CompHandshakeFactory;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.ConsumerSetChangeCallback;
import com.ibm.wsspi.sib.core.DestinationAvailability;
import com.ibm.wsspi.sib.core.DestinationListener;
import com.ibm.wsspi.sib.core.SICoreConnection;
import com.ibm.wsspi.sib.core.exception.SIConnectionLostException;

/**
 * This class will receive any data sent to us from our peer that is
 * not associated with a JFAP exchange. This kind of data is generally
 * asynchronous data such as asynchronous messages or connection information.
 *
 * @author Niall
 */
public class ProxyReceiveListener implements ConversationReceiveListener
{
   /** Class name for FFDC's */
   private static String CLASS_NAME = ProxyReceiveListener.class.getName();

   /** The trace reference */
   private static final TraceComponent tc = SibTr.register(ProxyReceiveListener.class,
                                                           CommsConstants.MSG_GROUP,
                                                           CommsConstants.MSG_BUNDLE);

   /** The NLS reference */
   private static final TraceNLS nls = TraceNLS.getTraceNLS(CommsConstants.MSG_BUNDLE);

   /** The buffer pool */
   private static CommsByteBufferPool bufferPool = CommsByteBufferPool.getInstance();

   /** Trace class info on load */
   static
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Source info: @(#)SIB/ws/code/sib.comms.client.impl/src/com/ibm/ws/sib/comms/client/ProxyReceiveListener.java, SIB.comms, WASX.SIB, uu1215.01 1.87");
   }

   /**
    * Notification that data was received.
    * <p>
    * In this method we are looking for five segment types:
    * <p>
    * ProxyMessage, AsyncMessage, BrowseMessage, ConnectionEvent and Connection Info.
    * <p>
    * Other segment messages will be thrown out - we will FFDC and
    * close the client connection as this is the safest thing
    * to do at this point as we do not know where the data has come from
    * or where it should go to.
    *
    * @param data The data.
    * @param segmentType   The segment type associated.
    * @param requestNumber The request number associated with this
    *                       transmission at send time.
    * @param priority The priority the data was sent with.
    * @param allocatedFromBufferPool The dat received was placed into a buffer
    *                                 allocated from the WS buffer pool.
    * @param partOfExchange This data is expecting a reply as part of a JFAP Exchange.
    * @param conversation The conversation associated with the data received.
    *
    * @return ConversationReceiveListener A more appropriate receiveListener to receive
    * future data on this conversation
    */
   public ConversationReceiveListener dataReceived(WsByteBuffer data, int segmentType,
                                                   int requestNumber, int priority,
                                                   boolean allocatedFromBufferPool,
                                                   boolean partOfExchange,
                                                   Conversation conversation)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "dataReceived",
                                           new Object[]{data, segmentType, requestNumber, priority, allocatedFromBufferPool, partOfExchange, });

      // Wrap the byte buffer in a comms byte buffer for easier manipulation
      CommsByteBuffer buffer = bufferPool.allocate();
      buffer.reset(data);

      // This flag will determine whether we can release the byte buffer at the end
      // of this method. Note we cannot release it if the message is destined for a
      // proxy queue - if we do, then when a client attempts to receive the message
      // it all falls over
      boolean releaseBuffer = true;

      // Now do a switch on the segment type that has been received
      switch (segmentType)
      {
         case (JFapChannelConstants.SEG_ASYNC_MESSAGE) :
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Received an ASYNC message");
            releaseBuffer = false;
            processMessage(buffer, true, false, conversation, false);
            break;

         case (JFapChannelConstants.SEG_CHUNKED_ASYNC_MESSAGE) :
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Received a chunked ASYNC message");
            releaseBuffer = false;
            processMessage(buffer, true, false, conversation, true);
            break;

         case (JFapChannelConstants.SEG_BROWSE_MESSAGE) :
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Received a BROWSE message");
            releaseBuffer = false;
            processMessage(buffer, false, true, conversation, false);
            break;

         case (JFapChannelConstants.SEG_CHUNKED_BROWSE_MESSAGE) :
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Received a chunked BROWSE message");
            releaseBuffer = false;
            processMessage(buffer, false, true, conversation, true);
            break;

         case (JFapChannelConstants.SEG_PROXY_MESSAGE) :
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Received a PROXY message");
            releaseBuffer = false;
            processMessage(buffer, false, true, conversation, false);
            break;

         case (JFapChannelConstants.SEG_CHUNKED_PROXY_MESSAGE) :
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Received a chunked PROXY message");
            releaseBuffer = false;
            processMessage(buffer, false, true, conversation, true);
            break;

         case (JFapChannelConstants.SEG_EVENT_OCCURRED) :
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Received a Server Event");
            processEvent(buffer, conversation);
            break;

         case (JFapChannelConstants.SEG_CONNECTION_INFO) :
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Received a connection info event");
            processConnectionInfo(buffer, conversation);
            break;

         case (JFapChannelConstants.SEG_SEND_SCHEMA_NOREPLY) :
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Received a message schema");
            processSchema(buffer, conversation);
            break;

         case (JFapChannelConstants.SEG_CHUNKED_SYNC_SESS_MESSAGE) :
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Received a chunk of a SYNC message");
            processSyncMessageChunk(buffer, conversation, false);
            break;

         case (JFapChannelConstants.SEG_CHUNKED_SYNC_CONN_MESSAGE) :
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Received a chunk of a SYNC connection message");
            processSyncMessageChunk(buffer, conversation, true);
            break;

         case (JFapChannelConstants.SEG_DESTINATION_LISTENER_CALLBACK_NOREPLY) :                       //SIB0137.comms.3
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Received a DestinationListener callback");
            processDestinationListenerCallback(buffer, conversation);                                  //SIB0137.comms.3
            break;

         case (JFapChannelConstants.SEG_ASYNC_SESSION_STOPPED_NOREPLY) :                               //SIB0115d.comms
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Received async session stopped callback");
            processAsyncSessionStoppedCallback(buffer, conversation);                                  //SIB0115d.comms
            break;
            
         case (JFapChannelConstants.SEG_REGISTER_CONSUMER_SET_MONITOR_CALLBACK_NOREPLY) :               //F011127
        	if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Received a ConsumerSetChangeCallback callback");
         	processConsumerSetChangeCallback(buffer, conversation);                                  //F011127
         	break;
         default :
            // Here we have received something unexpected. Generate an
            // FFDC and close the conversation down. We do this as
            // something has obviously gone a bit wrong and this is
            // the safest thing to do at this point.

            // Get the connection info
            String connectionInfo =
               ((ClientConversationState) conversation.getAttachment()).
                                                getCommsConnection().getConnectionInfo();


            String seg = segmentType + " (0x" + Integer.toHexString(segmentType).toUpperCase() + ")";
            Exception e = new SIErrorException(
               nls.getFormattedMessage("UNEXPECTED_MESS_RECVD_SICO1010", new Object[] { seg, connectionInfo }, null)
            );

            FFDCFilter.processException(e,
                                        CLASS_NAME + ".dataReceived",
                                        CommsConstants.PROXYRECEIVELISTENER_DATARCVD_01,
                                        new Object[] {SibTr.formatBytes(data.array(), 0, data.array().length), this});

            SibTr.error(tc, "UNEXPECTED_MESS_RECVD_SICO1010", new Object[] { seg, connectionInfo });

            // Try and close her down
            try
            {
               conversation.close();
            }
            catch (SIConnectionLostException ce)
            {
               FFDCFilter.processException(e, CLASS_NAME + ".dataReceived",
                                           CommsConstants.PROXYRECEIVELISTENER_DATARCVD_02, this);

               if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, ce.getMessage(), ce);
            }
      }

      // If we can release and repool the buffer do so. Otherwise, if it was used for a message
      // and cannot be released, simply repool any other resources except the actual buffer.
      if (allocatedFromBufferPool && releaseBuffer)
      {
         buffer.release();
      }
//      else
//      {
//         buffer.release(false);
//      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "dataReceived");

      // There is currently no requirement to change receive listeners in the client
      // so always return null
      return null;
   }
   /**
    * Notification that an error occurred when we were expecting to receive
    * a response.  This method is used to "wake up" any conversations using
    * a connection for which an error occurres.  At the point this method is
    * invoked, the connection will already have been marked "invalid".
    * <p>
    * Where this method is implemented in the ConversationReceiveListener
    * interface (which extends this interface) it is used to notify
    * the per conversation receive listener of (almost) all error conditions
    * encountered on the associated connection.
    *
    * @see ConversationReceiveListener
    *
    * @param exception The exception which occurred.
    * @param segmentType The segment type of the data (-1 if not known)
    * @param requestNumber The request number associated with the failing
    *                       request (-1 if not known)
    * @param priority The priority associated with the failing request
    *                  (-1 if not known).
    * @param conversation The conversation (null if not known)
    */
   public void errorOccurred(SIConnectionLostException exception,
                             int segmentType,
                             int requestNumber,
                             int priority,
                             Conversation conversation)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "errorOccurred");

      // First job is to FFDC
      FFDCFilter.processException(exception, CLASS_NAME + ".errorOccurred",
                                  CommsConstants.SERVERTRANSPORTRECEIVELISTENER_ERROR_01, this);

      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
      {
         Object[] debug = { "Segment type  : " + segmentType + " (0x" + Integer.toHexString(segmentType) + ")", "Request number: " + requestNumber, "Priority      : " + priority};
         SibTr.debug(tc, "Received an error in the ProxyReceiveListener", debug);
         SibTr.debug(tc, "Primary exception:");
         SibTr.exception(tc, exception);
      }

      // At this point we should notify any connection listeners that the connection
      // has failed. If the conversation was null, then we are unable to find out where
      // to deliver exceptions, so don't try.
      if (conversation !=null)
      {
         ClientConversationState convState =
            (ClientConversationState) conversation.getAttachment();

         // If in the unlikely event that we get a JFAP exception before we have
         // even initialised properly we are a bit stuffed - so don't even try
         if (convState != null)
         {
            // Otherwise, invoke any callbacks
            SICoreConnection conn = convState.getSICoreConnection();
            final ProxyQueueConversationGroup proxyQueueGroup = convState.getProxyQueueConversationGroup();
            ClientAsynchEventThreadPool.getInstance().dispatchCommsException(conn, proxyQueueGroup, exception);
         }
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())  SibTr.exit(tc, "errorOccurred");
   }

   /**
    * Gives us the oppurtunity to pick a thread for use in the JFap receive listener dispatcher. As
    * the dispatcher is not used on the client, this is not required (or even called for that
    * matter).
    *
    * @param conversation
    * @param buff
    * @param segType
    *
    * @return Returns null
    */
   public Dispatchable getThreadContext(Conversation conversation, WsByteBuffer buff, int segType)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getThreadContext", new Object[]{conversation, buff, segType});
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getThreadContext", null);
      return null;
   }

   /**
    * This method will process a message that has been received by the listener. Essentially, we
    * must get the session ID - this tells us which proxy queue to put the message on and then put
    * the message there. For async messages we must also get the flags and pass them to the proxy
    * queue as well.
    * <p>
    * If the chunk parameter is set to true this data is not an entire message, it is a message
    * chunk. This is passed onto the Proxy Queue to indicate that it should place the data with
    * other chunks until the message is complete.
    *
    * @param buffer
    * @param asyncMessage
    * @param enableReadAhead
    * @param conversation
    * @param chunk
    */
   private void processMessage(CommsByteBuffer buffer, boolean asyncMessage,
                               boolean enableReadAhead, Conversation conversation, boolean chunk)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "processMessage",
                                           new Object[]{buffer, asyncMessage, enableReadAhead, conversation, chunk});

      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) buffer.dump(this, tc, CommsByteBuffer.ENTIRE_BUFFER);

      // Connection object ID - not needed by us
      short connectionObjectID = buffer.getShort();
      // Client session ID (proxy ID)
      short clientSessionID = buffer.getShort();

      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
      {
         SibTr.debug(this, tc, "connectionObjectId: ", ""+connectionObjectID);
         SibTr.debug(this, tc, "clientSessionID: ", ""+clientSessionID);
      }
      boolean lastInBatch = false;

      if (asyncMessage)
      {
         // Flags
         short flags = buffer.getShort();
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "lastInBatchFlag: ", ""+flags);
         if (flags == 0x0001) lastInBatch = true;
      }

      // Message Batch
      short messageBatch = buffer.getShort();
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "messageBatch: ", ""+messageBatch);

      // Data length for debug purposes only
      if (chunk)
      {
         // Get the next 8 bytes which will contain 1 byte of flags, 4 bytes of length and then some
         // message data
         long next8bytes = buffer.peekLong();
         // Chop the message data
         long messageLength = next8bytes >> 24;
         // Chop off the 5th byte
         messageLength &= ~0xFF00000000L;
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Received a message chunk of length: " + messageLength);
      }
      else
      {
         int messageLength = (int) buffer.peekLong();
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Received a message of length: " + messageLength);
      }

      // Now pass the info off to the proxy queue
      // Get the conversation state and the get the proxy group from it
      ProxyQueueConversationGroup pqcg = ((ClientConversationState) conversation.getAttachment()).getProxyQueueConversationGroup();
      // If this is null, something has gone wrong here
      if (pqcg == null)
      {
         SIErrorException e = new SIErrorException(nls.getFormattedMessage("NO_PROXY_CONV_GROUP_SICO1011", null, null));
         FFDCFilter.processException(e, CLASS_NAME + ".processAsyncMessage",
                                     CommsConstants.PROXYRECEIVELISTENER_PROCESSMSG_01, this);
         SibTr.error(tc, "NO_PROXY_CONV_GROUP_SICO1011", e);
         throw e;
      }
      // Otherwise get the proxy queue
      ProxyQueue proxyQueue = pqcg.find(clientSessionID);
      if (proxyQueue == null)
      {
         SIErrorException e = new SIErrorException(nls.getFormattedMessage("UNABLE_TO_FIND_PROXY_QUEUE_SICO1012", null, null));
         FFDCFilter.processException(e, CLASS_NAME + ".processAsyncMessage",
                                     CommsConstants.PROXYRECEIVELISTENER_PROCESSMSG_01, this);
         SibTr.error(tc, "UNABLE_TO_FIND_PROXY_QUEUE_SICO1012", e);
         throw e;
      }

      // and put the message
      proxyQueue.put(buffer, messageBatch, lastInBatch, chunk);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "processMessage");
   }

   /**
    * This method will process a connection event message received from the server.
    * <p>
    * The format of this message is as follows:
    * <p>
    * BIT16 ConnectionObjectID
    * BIT16 EventID
    * <p>
    * The event ID that we are currently trapping for is:
    * <ul>
    *   <li>0x0000  -  (Used internally to invoke comms exception on the listener)
    *   <li>0x0002  -  ME Quiescing
    *   <li>0x0004  -  Async Exception
    * <ul>
    * <p>
    * For the async exception, the following additional data is flowed from the server:
    * <p>
    * BIT16 ClientSessionId (proxy queue Id)
    * BIT16 ExceptionId
    * BIT16 DataId
    * BIT16 Data length
    * BYTE[] Data
    *
    * @param buffer
    * @param conversation
    */
   private void processEvent(CommsByteBuffer buffer, Conversation conversation)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "processEvent",
                                           new Object[]
                                           {
                                              buffer,
                                              conversation
                                           });

      buffer.getShort();                                      // BIT16 ConnectionObjectID
      short eventId = buffer.getShort();                      // BIT16 Event ID

      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Event Id", ""+eventId);

      // Now switch on the event Id
      if (eventId == CommsConstants.EVENTID_ME_QUIESCING ||
          eventId == CommsConstants.EVENTID_ME_TERMINATED)
      {
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Received an ME event");
         ClientAsynchEventThreadPool.getInstance().dispatchAsynchEvent(eventId, conversation);
      }
      // Async exception
      else if(eventId == CommsConstants.EVENTID_ASYNC_EXCEPTION)
      {
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Received an Async exception event");

         short clientSessionId = buffer.getShort();
         // Get the exception type and rebuild the correct one
         Exception exception = buffer.getException(conversation);

         // Now pass the info off to the proxy queue

         // Get the conversation state and the get the proxy group from it
         ProxyQueueConversationGroup pqcg = ((ClientConversationState) conversation.getAttachment()).getProxyQueueConversationGroup();
         // If this is null, something has gone wrong here
         if (pqcg == null)
         {
            SIErrorException e = new SIErrorException(
               nls.getFormattedMessage("NO_PROXY_CONV_GROUP_SICO1011", null, null)
            );

            FFDCFilter.processException(e, CLASS_NAME + ".processEvent",
                                        CommsConstants.PROXYRECEIVELISTENER_PROCESSEVENT_01, this);

            SibTr.error(tc, "NO_PROXY_CONV_GROUP_SICO1011", e);

            throw e;
         }
         // Otherwise get the proxy queue
         ProxyQueue proxyQueue = pqcg.find(clientSessionId);
         if (proxyQueue == null)
         {
            SIErrorException e = new SIErrorException(
               nls.getFormattedMessage("UNABLE_TO_FIND_PROXY_QUEUE_SICO1012", null, null)
            );

            FFDCFilter.processException(e, CLASS_NAME + ".processEvent",
                                        CommsConstants.PROXYRECEIVELISTENER_PROCESSEVENT_02, this);

            SibTr.error(tc, "UNABLE_TO_FIND_PROXY_QUEUE_SICO1012", e);

            throw e;
         }

         ClientAsynchEventThreadPool.getInstance().dispatchAsynchException(proxyQueue, exception);
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "processEvent");
   }

   /**
    * This method will process the connection information received from our peer.
    * At the moment this consists of the connection object ID required at the server,
    * the name of the messaging engine and the ME Uuid.
    *
    * @param buffer
    * @param conversation
    */
   private void processConnectionInfo(CommsByteBuffer buffer, Conversation conversation)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "processConnectionInfo",
                                           new Object[]{buffer, conversation});

      ClientConversationState convState = (ClientConversationState) conversation.getAttachment();

      short connectionObjectId = buffer.getShort();           // BIT16 ConnectionObjectID
      String meName = buffer.getString();                     // STR   MEName

      // Set the connection object ID in the conversation state
      convState.setConnectionObjectID(connectionObjectId);

      // Create the connection proxy and save it away in the conversation state
      final ConnectionProxy connectionProxy;

      // The type of connection proxy we create is dependent on FAP version.  Fap version 5 supports
      // a connection which MSSIXAResourceProvider.  This is done so that PEV (introduced in line with
      // FAP 5) can perform remote recovery of resources.
      if (conversation.getHandshakeProperties().getFapLevel() >= JFapChannelConstants.FAP_VERSION_5)
      {
         connectionProxy = new MSSIXAResourceProvidingConnectionProxy(conversation);
      }
      else
      {
         connectionProxy = new ConnectionProxy(conversation);
      }

      convState.setSICoreConnection(connectionProxy);

      // Set in the ME name
      connectionProxy.setMeName(meName);

      // Now get the unique Id if there was one, and set it in the connection proxy
      short idLength = buffer.getShort();
      if (idLength != 0)
      {
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Got unique id of length:", idLength);

         byte[] uniqueId = buffer.get(idLength);

         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.bytes(tc, uniqueId);

         connectionProxy.setInitialUniqueId(uniqueId);
      }
      else
      {
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "No unique Id was returned");
      }

      // Get the ME Uuid
      String meUuid = buffer.getString();
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "ME Uuid: ", meUuid);
      connectionProxy.setMeUuid(meUuid);

      // Get the resolved User Id
      String resolvedUserId = buffer.getString();
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Resolved UserId: ", resolvedUserId);
      connectionProxy.setResolvedUserId(resolvedUserId);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "processConnectionInfo");
   }

   /**
    * This method will process a schema received from our peer's MFP compoennt.
    * At the moment this consists of contacting MFP here on the client and
    * giving it the schema. Schemas are received when the ME is about to send
    * us a message and realises that we don't have the necessary schema to decode
    * it. A High priority message is then sent ahead of the data ensuring that
    * by the time the message is received the schema will be understood.
    *
    * @param buffer
    * @param conversation
    */
   private void processSchema(CommsByteBuffer buffer, Conversation conversation)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "processSchema",
                                           new Object[]
                                           {
                                              buffer,
                                              conversation
                                           });

      ClientConversationState convState = (ClientConversationState) conversation.getAttachment();

      byte[] mfpDataAsBytes = buffer.getRemaining();

      // Get hold of the CommsConnection associated with this conversation
      CommsConnection cc = convState.getCommsConnection();

      // Get hold of MFP Singleton and pass it the schema
      try
      {
         // Get hold of product version
         final HandshakeProperties handshakeGroup = conversation.getHandshakeProperties();
         final int productVersion = handshakeGroup.getMajorVersion();

         // Get hold of MFP and inform it of the schema
         CompHandshake ch = (CompHandshake) CompHandshakeFactory.getInstance();
         ch.compData(cc,productVersion,mfpDataAsBytes);
      }
      catch (Exception e1)
      {
         FFDCFilter.processException(e1, CLASS_NAME + ".processSchema",
                            CommsConstants.PROXYRECEIVELISTENER_PROCESSSCHEMA_01, this);

         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "MFP unable to create CompHandshake Singleton", e1);
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "processSchema");
   }

   /**
    * Processes a chunk received from a synchronous message.
    *
    * @param buffer
    * @param conversation
    * @param connectionMessage
    */
   private void processSyncMessageChunk(CommsByteBuffer buffer, Conversation conversation,
                                        boolean connectionMessage)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "processSyncMessageChunk",
                                           new Object[]{buffer, conversation, connectionMessage});

      // First get the ConnectionProxy for this conversation
      ClientConversationState convState = (ClientConversationState) conversation.getAttachment();
      ConnectionProxy connProxy = (ConnectionProxy) convState.getSICoreConnection();
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Found connection: ", connProxy);

      // Now read the connection object Id
      buffer.getShort();                                            // BIT16 ConnectionObjId
      if (connectionMessage)
      {
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Adding message part directly to connection");
         connProxy.addMessagePart(buffer);
      }
      else
      {
         short consumerSessionId = buffer.getShort();               // BIT16 ConsumerSessionId
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Consumer Session Id:", ""+consumerSessionId);

         // Now simply pass the message buffer off to the consumer
         ConsumerSessionProxy consumer = connProxy.getConsumerSessionProxy(consumerSessionId);
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Found consumer:", consumer);
         consumer.addMessagePart(buffer);
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "processSyncMessageChunk");
   }

   // SIB0137.comms.3 start

   private void processDestinationListenerCallback (CommsByteBuffer buffer, Conversation conversation) {
     if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "processDestinationListenerCallback", new Object[]{buffer, conversation});

     final ClientConversationState convState = (ClientConversationState) conversation.getAttachment();
     final SICoreConnection connection = convState.getSICoreConnection();

     final short connectionObjectId                = buffer.getShort();
     final short destinationListenerId             = buffer.getShort();
     final SIDestinationAddress destinationAddress = buffer.getSIDestinationAddress(conversation.getHandshakeProperties().getFapLevel());
     final short destinationAvailabilityShort      = buffer.getShort();

     if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "connectionObjectId="+connectionObjectId);
     if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "destinationListenerId="+destinationListenerId);
     if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "destinationAddress="+destinationAddress);

     DestinationAvailability destinationAvailability = null;
     if (destinationAvailabilityShort != CommsConstants.NO_DEST_AVAIL) {
       destinationAvailability = DestinationAvailability.getDestinationAvailability(destinationAvailabilityShort);
     }
     if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "destinationAvailability="+destinationAvailability);

     // Look up the real DestinationListener in the local DestinationListenerCache

     final DestinationListenerCache destinationListenerCache = convState.getDestinationListenerCache();
     final DestinationListener destinationListener = destinationListenerCache.get(destinationListenerId);

     if (destinationListener != null) {
        //Call the listener on a seperate thread.
        ClientAsynchEventThreadPool.getInstance().dispatchDestinationListenerEvent(connection, destinationAddress, destinationAvailability, destinationListener);
     } else {
       if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "DestinationListener id="+destinationListenerId+" not found in DestinationListenerCache");
       if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, destinationListenerCache.toString());

       SIErrorException e = new SIErrorException(nls.getFormattedMessage("UNABLE_TO_FIND_DESTINATION_LISTENER_SICO8019", new Object[] {Short.valueOf(destinationListenerId)}, null));
       FFDCFilter.processException(e, CLASS_NAME + ".processDestinationListenerCallback", CommsConstants.PROXYRECEIVELISTENER_DESTLIST_CALLBACK_02, this);

       SibTr.error(tc, "UNABLE_TO_FIND_DESTINATION_LISTENER_SICO8019", e);
       throw e;
     }

     if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "processDestinationListenerCallback");
   }

   // SIB0137.comms.3 end

   // SIB0115d.comms start

   private void processAsyncSessionStoppedCallback (CommsByteBuffer buffer, Conversation conversation) {
     if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "processAsyncSessionStoppedCallback", new Object[]{buffer, conversation});

     final short connectionObjectId = buffer.getShort();
     final short clientSessionId    = buffer.getShort();

     if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "connectionObjectId="+connectionObjectId);
     if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "clientSessionId="+clientSessionId);

     // Obtain the proxy queue group
     final ClientConversationState convState = (ClientConversationState) conversation.getAttachment();
     final ProxyQueueConversationGroup pqcg = convState.getProxyQueueConversationGroup();
     if (pqcg == null) {
       if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "ProxyQueueConversationGroup=null");
       SIErrorException e = new SIErrorException(nls.getFormattedMessage("NULL_PROXY_QUEUE_CONV_GROUP_CWSICO8020", new Object[] {}, null));
       FFDCFilter.processException(e, CLASS_NAME + ".processAsyncSessionStoppedCallback", CommsConstants.PROXYRECEIVELISTENER_SESSION_STOPPED_01, this);
       SibTr.error(tc, "NULL_PROXY_QUEUE_CONV_GROUP_CWSICO8020", e);
       throw e;
     }

     // Obtain the required proxy queue from the proxy queue group and ensure its of the right class (ie not read ahead)
     final ProxyQueue proxyQueue = pqcg.find(clientSessionId);
     if (proxyQueue instanceof AsynchConsumerProxyQueue) {
       final ConsumerSessionProxy consumerSessionProxy = ((AsynchConsumerProxyQueue)proxyQueue).getConsumerSessionProxy();

       //Drive the ConsumerSessionProxy.stoppableConsumerSessionStopped method on a different thread.
       ClientAsynchEventThreadPool.getInstance().dispatchStoppableConsumerSessionStopped(consumerSessionProxy);
     } else {
       if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "proxyQueue not an instance of AsynchConsumerProxyQueue is an instance of "+ proxyQueue.getClass().getName());
       SIErrorException e = new SIErrorException(nls.getFormattedMessage("WRONG_CLASS_CWSICO8021", new Object[] {proxyQueue.getClass().getName()}, null));
       FFDCFilter.processException(e, CLASS_NAME + ".processAsyncSessionStoppedCallback", CommsConstants.PROXYRECEIVELISTENER_SESSION_STOPPED_02, this);
       SibTr.error(tc, "WRONG_CLASS_CWSICO8021", e);
       throw e;
     }

     if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "processAsyncSessionStoppedCallback");
   }

   // SIB0115d.comms end
   
   // F011127 start

   private void processConsumerSetChangeCallback (CommsByteBuffer buffer, Conversation conversation) {
     if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "processConsumerSetChangeCallback", new Object[]{buffer, conversation});

     final ClientConversationState convState = (ClientConversationState) conversation.getAttachment();
     //final SICoreConnection connection = convState.getSICoreConnection();

     final short connectionObjectId                = buffer.getShort();
     final short consumerMonitorListenerid             = buffer.getShort();
     final boolean isEmpty = buffer.getBoolean();
     
     if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "connectionObjectId="+connectionObjectId);
     if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "consumerMonitorListenerid="+consumerMonitorListenerid);
     if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "isEmpty="+isEmpty);
    
     // Look up the real ConsumerSetChangeCallback in the local ConsumerMonitorListenerCache

     final ConsumerMonitorListenerCache consumerMonitorListenerCache = convState.getConsumerMonitorListenerCache();
     final ConsumerSetChangeCallback consumerSetChangeCallback = consumerMonitorListenerCache.get(consumerMonitorListenerid);

     if (consumerSetChangeCallback != null) {
        //Call the listener on a seperate thread.
        ClientAsynchEventThreadPool.getInstance().dispatchConsumerSetChangeCallbackEvent(consumerSetChangeCallback,isEmpty);
     } else {
       if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "consumerMonitorListenerid="+consumerMonitorListenerid+" not found in consumerMonitorListenerCache");
       if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, consumerMonitorListenerCache.toString());

       SIErrorException e = new SIErrorException(nls.getFormattedMessage("UNABLE_TO_FIND_CONSUMER_MONITOR_LISTENER_SICO8024", new Object[] {consumerMonitorListenerid}, null));
       FFDCFilter.processException(e, CLASS_NAME + ".processConsumerSetChangeCallback", CommsConstants.PROXYRECEIVELISTENER_CONSUMERMON_CALLBACK_01, this);

       SibTr.error(tc, "An internal error occurred. The consumerMonitorListenerid "+consumerMonitorListenerid+" received by the client can not be located.");
       if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "processConsumerSetChangeCallback");
       throw e;
     }

     if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "processConsumerSetChangeCallback");
   }

   // F011127 end

}
