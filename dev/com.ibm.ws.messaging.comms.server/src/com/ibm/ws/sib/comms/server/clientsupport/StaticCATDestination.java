/*******************************************************************************
 * Copyright (c) 2012, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.comms.server.clientsupport;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.sib.Reliability;
import com.ibm.websphere.sib.SIDestinationAddress;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.comms.CommsConstants;
import com.ibm.ws.sib.comms.common.CommsByteBuffer;
import com.ibm.ws.sib.comms.common.CommsByteBufferPool;
import com.ibm.ws.sib.comms.common.CommsUtils;
import com.ibm.ws.sib.comms.server.CommsServerByteBuffer;
import com.ibm.ws.sib.comms.server.ConversationState;
import com.ibm.ws.sib.comms.server.IdToTransactionTable;
import com.ibm.ws.sib.comms.server.ServerLinkLevelState;
import com.ibm.ws.sib.jfapchannel.Conversation;
import com.ibm.ws.sib.jfapchannel.JFapChannelConstants;
import com.ibm.ws.sib.jfapchannel.Conversation.ThrottlingPolicy;
import com.ibm.ws.sib.mfp.JsDestinationAddress;
import com.ibm.ws.sib.mfp.impl.JsMessageFactory;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.ConsumerSession;
import com.ibm.wsspi.sib.core.DestinationConfiguration;
import com.ibm.wsspi.sib.core.DestinationType;
import com.ibm.wsspi.sib.core.Distribution;
import com.ibm.wsspi.sib.core.SIBusMessage;
import com.ibm.wsspi.sib.core.SICoreConnection;
import com.ibm.wsspi.sib.core.SITransaction;
import com.ibm.wsspi.sib.core.exception.SINotAuthorizedException;


/**
 * This class contains code handling requests from clients that pertain to destinations.
 */
public class StaticCATDestination
{
   /** Class name for FFDC's */
   private static String CLASS_NAME = StaticCATDestination.class.getName();

   /** Reference to the pool manager */
   private static CommsByteBufferPool poolManager = CommsByteBufferPool.getInstance();

   /** Registers our trace component */
   private static final TraceComponent tc = SibTr.register(StaticCATDestination.class,
                                                           CommsConstants.MSG_GROUP,
                                                           CommsConstants.MSG_BUNDLE);

   /** Log class info on load */
   static
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Source info: @(#)SIB/ws/code/sib.comms.server.impl/src/com/ibm/ws/sib/comms/server/clientsupport/StaticCATDestination.java, SIB.comms, WASX.SIB, aa1225.01 1.71");
   }

   /**
    * Creates a temporary destination for use for this connection only.
    *
    * Fields:
    *
    * BIT16    ConnectionObjectID
    * BIT16    Distribution type  (0x0000 - ONE, 0x0001 - ALL)
    * BIT16    DestinationNamePrefixLength
    * BYTE[]   DestinationNamePrefix
    *
    * @param request
    * @param conversation
    * @param requestNumber
    * @param allocatedFromBufferPool
    * @param partOfExchange
    */
   static void rcvCreateTempDestination(CommsByteBuffer request, Conversation conversation,
                                        int requestNumber, boolean allocatedFromBufferPool,
                                        boolean partOfExchange)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "rcvCreateTempDestination",
                                           new Object[]
                                           {
                                              request,
                                              conversation,
                                              ""+requestNumber,
                                              ""+allocatedFromBufferPool,
                                              ""+partOfExchange
                                            });

      ConversationState convState = (ConversationState) conversation.getAttachment();

      short connectionObjectID = request.getShort(); // BIT16 ConnectionObjectId
      SICoreConnection connection =
         ((CATConnection) convState.getObject(connectionObjectID)).getSICoreConnection();
      short dist = request.getShort();

      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
      {
         SibTr.debug(tc, "SICoreConnection Id:", connectionObjectID);
         SibTr.debug(tc, "Distribution", dist);
      }

      /**************************************************************/
      /* Distribution                                               */
      /**************************************************************/
      Distribution distribution = Distribution.getDistribution(dist);

      /**************************************************************/
      /* Destination name prefix                                    */
      /**************************************************************/
      String destinationNamePrefix = request.getString();
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Destination prefix;", destinationNamePrefix);

      try
      {
         JsDestinationAddress destAddress =
            (JsDestinationAddress) connection.createTemporaryDestination(distribution,
                                                                         destinationNamePrefix);

         CommsByteBuffer reply = poolManager.allocate();
         reply.putSIDestinationAddress(destAddress, conversation.getHandshakeProperties().getFapLevel());

         try
         {
            // Inform the client
            conversation.send(reply,
                              JFapChannelConstants.SEG_CREATE_TEMP_DESTINATION_R,
                              requestNumber,
                              JFapChannelConstants.PRIORITY_MEDIUM,
                              true,
                              ThrottlingPolicy.BLOCK_THREAD,
                              null);
         }
         catch (SIException e)
         {
            FFDCFilter.processException(e,
                                        CLASS_NAME + ".rcvCreateTempDestination",
                                        CommsConstants.STATICCATDESTINATION_DESTCREATE_01);

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, e.getMessage(), e);

            SibTr.error(tc, "COMMUNICATION_ERROR_SICO2022", e);
         }
      }
      catch (SINotAuthorizedException e)
      {
         // No FFDC Code Needed
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, e.getMessage(), e);

         StaticCATHelper.sendExceptionToClient(e,
                                               null,
                                               conversation, requestNumber);
      }
      catch (SIException e)
      {
         //No FFDC code needed
         //Only FFDC if we haven't received a meTerminated event.
         if(!convState.hasMETerminated())
         {
            FFDCFilter.processException(e,
                                        CLASS_NAME + ".rcvCreateTempDestination",
                                        CommsConstants.STATICCATDESTINATION_DESTCREATE_02);
         }

         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, e.getMessage(), e);

         StaticCATHelper.sendExceptionToClient(e,
                                               CommsConstants.STATICCATDESTINATION_DESTCREATE_02,
                                               conversation, requestNumber);
      }

      request.release(allocatedFromBufferPool);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "rcvCreateTempDestination");
   }

   /**
    * @param request
    * @param conversation
    * @param requestNumber
    * @param allocatedFromBufferPool
    * @param partOfExchange
    */
   static void rcvDeleteTempDestination(CommsByteBuffer request, Conversation conversation,
                                        int requestNumber, boolean allocatedFromBufferPool,
                                        boolean partOfExchange)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "rcvDeleteTempDestination",
                                           new Object[]
                                           {
                                              request,
                                              conversation,
                                              ""+requestNumber,
                                              ""+allocatedFromBufferPool,
                                              ""+partOfExchange
                                            });

      ConversationState convState = (ConversationState) conversation.getAttachment();

      short connectionObjectID = request.getShort(); // BIT16 ConnectionObjectId
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "SICoreConnection Id:", ""+connectionObjectID);

      SICoreConnection connection =
         ((CATConnection) convState.getObject(connectionObjectID)).getSICoreConnection();

      try
      {
         /**************************************************************/
         /* Destination address                                        */
         /**************************************************************/
         SIDestinationAddress destAddress = request.getSIDestinationAddress(conversation.getHandshakeProperties().getFapLevel());

         // Have a quick check of the cached destination that is being held open for connection
         // receives. If the destinations match, we must close that consumer before deleting
         // the temporary destination
         CATMainConsumer cacheConsumer = convState.getCachedConsumer();
         if (cacheConsumer != null)
         {
            ConsumerSession cacheSession = cacheConsumer.getConsumerSession();

            if (cacheSession != null)
            {
               if (cacheSession.getDestinationAddress().equals(destAddress))
               {
                  if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "The destination is currently held open by us");

                  try
                  {
                     cacheSession.close();

                     // Ensure we try and recreate the consumer next time
                     convState.setCachedConsumer(null);
                  }
                  catch (SIException e)
                  {
                     // No FFDC code needed
                     if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Unable to close cached consumer", e);
                  }
               }
            }
         }

         // Remove the destination
         connection.deleteTemporaryDestination(destAddress);

         try
         {
            // Inform the client
            conversation.send(poolManager.allocate(),
                              JFapChannelConstants.SEG_DELETE_TEMP_DESTINATION_R,
                              requestNumber,
                              JFapChannelConstants.PRIORITY_MEDIUM,
                              true,
                              ThrottlingPolicy.BLOCK_THREAD,
                              null);
         }
         catch (SIException e)
         {
            FFDCFilter.processException(e,
                                        CLASS_NAME + ".rcvDeleteTempDestination",
                                        CommsConstants.STATICCATDESTINATION_DESTDELETE_01);

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, e.getMessage(), e);

            SibTr.error(tc, "COMMUNICATION_ERROR_SICO2022", e);
         }
      }
      catch (SINotAuthorizedException e)
      {
         // No FFDC Code Needed
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, e.getMessage(), e);

         StaticCATHelper.sendExceptionToClient(e,
                                               null,
                                               conversation, requestNumber);
      }
      catch (SIException e)
      {
         //No FFDC code needed
         //Only FFDC if we haven't received a meTerminated event.
         if(!convState.hasMETerminated())
         {
            FFDCFilter.processException(e,
                                        CLASS_NAME + ".rcvDeleteTempDestination",
                                        CommsConstants.STATICCATDESTINATION_DESTDELETE_02);
         }

         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, e.getMessage(), e);

         StaticCATHelper.sendExceptionToClient(e,
                                               CommsConstants.STATICCATDESTINATION_DESTDELETE_02,
                                               conversation, requestNumber);
      }

      request.release(allocatedFromBufferPool);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "rcvDeleteTempDestination");
   }

   /**
    * @param request
    * @param conversation
    * @param requestNumber
    * @param allocatedFromBufferPool
    * @param partOfExchange
    */
   static void rcvGetDestinationConfiguration(CommsByteBuffer request, Conversation conversation,
                                              int requestNumber, boolean allocatedFromBufferPool,
                                              boolean partOfExchange)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "rcvGetDestinationConfiguration",
                                           new Object[]
                                           {
                                              request,
                                              conversation,
                                              ""+requestNumber,
                                              ""+allocatedFromBufferPool,
                                              ""+partOfExchange
                                            });

      ConversationState convState = (ConversationState) conversation.getAttachment();

      short connectionObjectID = request.getShort(); // BIT16 ConnectionObjectId
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "SICoreConnection Id:", ""+connectionObjectID);

      SICoreConnection connection =
         ((CATConnection) convState.getObject(connectionObjectID)).getSICoreConnection();

      try
      {
         /**************************************************************/
         /* Destination address                                        */
         /**************************************************************/
         SIDestinationAddress destAddress = request.getSIDestinationAddress(conversation.getHandshakeProperties().getFapLevel());

         // Call getDestinationConfiguration() on the SICoreConnection then simply serialize the
         // returned object and send it back to the client who called it.
         DestinationConfiguration dc = connection.getDestinationConfiguration(destAddress);

         int defaultPriority = dc.getDefaultPriority();
         int maxFailedDeliveries = dc.getMaxFailedDeliveries();

         // Take the various boolean settings from the destination configuration and add them to
         // the buffer as flags
         short flags = 0;
         if (dc.isProducerQOSOverrideEnabled()) flags |= 0x0001;
         if (dc.isReceiveAllowed())             flags |= 0x0002;
         if (dc.isReceiveExclusive())           flags |= 0x0004;
         if (dc.isSendAllowed())                flags |= 0x0008;
         // Fap version 5 and up uses the 5th bit of the flags for whether strict ordering is
         // required or not.
         if (conversation.getHandshakeProperties().getFapLevel() >= JFapChannelConstants.FAP_VERSION_5)
         {
            if (dc.isStrictOrderingRequired())  flags |= 0x0010;
         }

         Reliability reliability = dc.getDefaultReliability();
         short reliabilityShort = (short)reliability.toInt();

         Reliability maxReliability = dc.getMaxReliability();
         short maxReliabilityShort = (short)maxReliability.toInt();

         DestinationType destType = dc.getDestinationType();
         short destinationTypeShort = (short)destType.toInt();

         CommsByteBuffer reply = poolManager.allocate();
         reply.putInt(defaultPriority);
         reply.putInt(maxFailedDeliveries);
         reply.putShort(reliabilityShort);
         reply.putShort(maxReliabilityShort);
         reply.putShort(destinationTypeShort);
         reply.putShort(flags);
         reply.putString(dc.getUUID());
         reply.putString(dc.getDescription());
         reply.putString(dc.getExceptionDestination());
         reply.putString(dc.getName());
         reply.putSIDestinationAddress(dc.getReplyDestination(), conversation.getHandshakeProperties().getFapLevel());

         // Now send back the name value pairs in the destination context. Currently we will only send back
         //non-string values. Any other values are ignored.
         try
         {
            //This map contains all the destination context properties. The values aren't guaranteed to be strings
            //so we need to generate a new map which only contains string values.
            final Map context = dc.getDestinationContext();
            final HashMap<String, String> stringOnlyContext = new HashMap<String, String>();
                       
            if(context != null)
            {
               final Set entrySet = context.entrySet();
               final Iterator entryIterator = entrySet.iterator();
               
               while(entryIterator.hasNext())
               {
                  final Entry entry = (Entry) entryIterator.next();
                  final String key = (String) entry.getKey();
                  final Object value = entry.getValue();
                  
                  //Add string property to our string only map.
                  if(value instanceof String)
                  {
                     stringOnlyContext.put(key, (String) value);
                  }
                  else
                  {
                     if(TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) 
                        SibTr.debug(tc, "Absorbing non-string property.", new Object[] {key, value});
                  }
               }
            }
            
            //Work out the length of the string only properties.
            final int numberOfItems = stringOnlyContext.size();

            if(TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Adding " + numberOfItems + " name / value item(s)");

            reply.putShort(numberOfItems);

            //Now add the items.
            if(numberOfItems > 0)
            {
               final Set<Entry<String, String>> stringOnlyEntrySet = stringOnlyContext.entrySet();
               
               for(final Entry<String, String> entry : stringOnlyEntrySet)
               {
                  reply.putString(entry.getKey());
                  reply.putString(entry.getValue());
               }
            }

            // Work out the number of addresses to send
            SIDestinationAddress[] frp = dc.getDefaultForwardRoutingPath();
            int numberOfFRPAddresses = 0;

            if (frp != null) numberOfFRPAddresses = frp.length;

            reply.putShort(numberOfFRPAddresses);

            if (numberOfFRPAddresses > 0)
            {
               for (int x = 0; x < frp.length; x++)
               {
                  reply.putSIDestinationAddress(frp[x], conversation.getHandshakeProperties().getFapLevel());
               }
            }

            try
            {
               // Inform the client
               conversation.send(reply,
                                 JFapChannelConstants.SEG_GET_DESTINATION_CONFIGURATION_R,
                                 requestNumber,
                                 JFapChannelConstants.PRIORITY_MEDIUM,
                                 true,
                                 ThrottlingPolicy.BLOCK_THREAD,
                                 null);
            }
            catch (SIException e)
            {
               FFDCFilter.processException(e,
                                           CLASS_NAME + ".rcvGetDestinationConfiguration",
                                           CommsConstants.STATICCATDESTINATION_GETDESTCONFIG_01);

               if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, e.getMessage(), e);

               SibTr.error(tc, "COMMUNICATION_ERROR_SICO2022", e);
            }
         }
         // This can be thrown if someone has put something that wasn't a String inside the Map
         // This is an internal error
         catch (ClassCastException e)
         {
            FFDCFilter.processException(e,
                                        CLASS_NAME + ".rcvGetDestinationConfiguration",
                                        CommsConstants.STATICCATDESTINATION_GETDESTCONFIG_02);

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, e.getMessage(), e);

            StaticCATHelper.sendExceptionToClient(e,
                                                  CommsConstants.STATICCATDESTINATION_GETDESTCONFIG_02,
                                                  conversation, requestNumber);
         }
      }
      catch (SINotAuthorizedException e)
      {
         // No FFDC Code Needed
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, e.getMessage(), e);

         StaticCATHelper.sendExceptionToClient(e,
                                               null,
                                               conversation, requestNumber);
      }
      catch (SIException e)
      {
         //No FFDC code needed
         //Only FFDC if we haven't received a meTerminated event.
         if(!convState.hasMETerminated())
         {
            FFDCFilter.processException(e,
                                        CLASS_NAME + ".rcvGetDestinationConfiguration",
                                        CommsConstants.STATICCATDESTINATION_GETDESTCONFIG_03);
         }

         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, e.getMessage(), e);

         StaticCATHelper.sendExceptionToClient(e,
                                               CommsConstants.STATICCATDESTINATION_GETDESTCONFIG_03,  // d186970
                                               conversation, requestNumber);                          // f172297
      }

      request.release(allocatedFromBufferPool);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "rcvGetDestinationConfiguration");
   }

   /**
    * Sends a message to the exception destination.
    *
    * Fields:
    *
    * BIT16    ConnectionObjectID
    * BIT16    ProducerFlags
    * BIT32    TransactionId
    * BIT16    UUid Length
    * BYTE[]   UUid
    * BIT16    DestinationNameLength
    * BYTE[]   DestinationName
    * BIT32    Reason
    * BIT64    JMO Length
    * BYTE[]   JMO
    * BIT16    NumberOfInserts
    *
    * BIT16    InsertLength
    * BYTE[]   Insert
    *
    * @param request
    * @param conversation
    * @param requestNumber
    * @param allocatedFromBufferPool
    * @param partOfExchange
    */
   static void rcvSendToExceptionDest(CommsServerByteBuffer request,
                                      Conversation conversation,
                                      int requestNumber,
                                      boolean allocatedFromBufferPool,
                                      boolean partOfExchange)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "rcvSendToExceptionDest",
                                           new Object[]
                                           {
                                              request,
                                              conversation,
                                              ""+requestNumber,
                                              ""+allocatedFromBufferPool,
                                              ""+partOfExchange
                                           });

      ServerLinkLevelState linkState = (ServerLinkLevelState) conversation.getLinkLevelAttachment();
      ConversationState convState = (ConversationState) conversation.getAttachment();

      final boolean optimizedTx = CommsUtils.requiresOptimizedTransaction(conversation);

      try
      {
         /**************************************************************/
         /* Connection object id                                       */
         /**************************************************************/
         short connectionObjId = request.getShort();

         /**************************************************************/
         /* Transaction id                                             */
         /**************************************************************/
         int txId = request.getSITransactionId(connectionObjId, linkState, optimizedTx);

         /**************************************************************/
         /* Destination address                                        */
         /**************************************************************/
         SIDestinationAddress destAddr = request.getSIDestinationAddress(conversation.getHandshakeProperties().getFapLevel());

         /**************************************************************/
         /* Reason code                                                */
         /**************************************************************/
         int reason = request.getInt();

         /**************************************************************/
         /* Alternate user                                             */
         /**************************************************************/
         String alternateUser = request.getString();

         /**************************************************************/
         /* JMO Message length                                         */
         /**************************************************************/
         int messageLength = (int) request.peekLong();

         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
         {
            SibTr.debug(tc, "ConnectionObjectId", ""+connectionObjId);
            SibTr.debug(tc, "DestinationAddress", destAddr);
            SibTr.debug(tc, "Reason", ""+reason);
            SibTr.debug(tc, "TransactionId", ""+txId);
            SibTr.debug(tc, "MessageLength", ""+messageLength);
            SibTr.debug(tc, "AlternateUser", alternateUser);
         }

         // Now decode the JMO
         SIBusMessage jsMessage = request.getMessage(null);

         // Now grab all the inserts
         String[] inserts = null;
         int numberOfInserts = request.getShort();
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Number of inserts", ""+numberOfInserts);

         if (numberOfInserts != 0)
         {
            inserts = new String[numberOfInserts];

            for (int x = 0; x < numberOfInserts; x++)
            {
               inserts[x] = request.getString();
               if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Insert[" + x + "] = " + inserts[x]);
            }
         }

         // Get the transaction
         SITransaction tran = linkState.getTransactionTable().get(txId);

         // Get the SICoreConnection
         SICoreConnection connection =
            ((CATConnection) convState.getObject(connectionObjId)).getSICoreConnection();

         // Now call the core SPI method
         if (tran != IdToTransactionTable.INVALID_TRANSACTION)
         {
            connection.sendToExceptionDestination(destAddr,
                                                  jsMessage,
                                                  reason,
                                                  inserts,
                                                  tran,
                                                  alternateUser);
         }

         try
         {
            conversation.send(poolManager.allocate(),
                              JFapChannelConstants.SEG_SEND_TO_EXCEPTION_DESTINATION_R,
                              requestNumber,
                              JFapChannelConstants.PRIORITY_MEDIUM,
                              true,
                              ThrottlingPolicy.BLOCK_THREAD,
                              null);
         }
         catch (SIException e)
         {
            FFDCFilter.processException(e,
                                        CLASS_NAME + ".rcvSendToExceptionDest",
                                        CommsConstants.STATICCATDESTINATION_SENDTOEXCEP_01);

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, e.getMessage(), e);

            SibTr.error(tc, "COMMUNICATION_ERROR_SICO2022", e);
         }
      }
      catch (SINotAuthorizedException e)
      {
         // No FFDC Code Needed
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, e.getMessage(), e);

         StaticCATHelper.sendExceptionToClient(e,
                                               null,
                                               conversation, requestNumber);
      }
      catch (SIException e)
      {
         //No FFDC code needed
         //Only FFDC if we haven't received a meTerminated event.
         if(!convState.hasMETerminated())
         {
            FFDCFilter.processException(e,
                                        CLASS_NAME + ".rcvSendToExceptionDest",
                                        CommsConstants.STATICCATDESTINATION_SENDTOEXCEP_02);
         }

         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, e.getMessage(), e);

         StaticCATHelper.sendExceptionToClient(e,
                                               CommsConstants.STATICCATDESTINATION_SENDTOEXCEP_02,
                                               conversation, requestNumber);
      }
      catch (Exception e)
      {
         FFDCFilter.processException(e,
                                     CLASS_NAME + ".rcvSendToExceptionDest",
                                     CommsConstants.STATICCATDESTINATION_SENDTOEXCEP_03);

         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, e.getMessage(), e);

         StaticCATHelper.sendExceptionToClient(e,
                                               CommsConstants.STATICCATDESTINATION_SENDTOEXCEP_03,
                                               conversation, requestNumber);
      }

      if (allocatedFromBufferPool) request.release();

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "rcvSendToExceptionDest");
   }

   /**
    * Sends a message to the exception destination.
    *
    * Fields:
    *
    * BIT16    ConnectionObjectID
    * BIT16    ProducerFlags
    * BIT32    TransactionId
    * BIT16    UUid Length
    * BYTE[]   UUid
    * BIT16    DestinationNameLength
    * BYTE[]   DestinationName
    * BIT32    Reason
    * BIT64    JMO Length
    * BYTE[]   JMO
    * BIT16    NumberOfInserts
    *
    * BIT16    InsertLength
    * BYTE[]   Insert
    *
    * @param request
    * @param conversation
    * @param requestNumber
    * @param allocatedFromBufferPool
    * @param partOfExchange
    */
   static void rcvSendChunkedToExceptionDest(CommsServerByteBuffer request,
                                             Conversation conversation,
                                             int requestNumber,
                                             boolean allocatedFromBufferPool,
                                             boolean partOfExchange)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "rcvSendChunkedToExceptionDest",
                                           new Object[]
                                           {
                                              request,
                                              conversation,
                                              ""+requestNumber,
                                              ""+allocatedFromBufferPool,
                                              ""+partOfExchange
                                           });

      ServerLinkLevelState linkState = (ServerLinkLevelState) conversation.getLinkLevelAttachment();
      ConversationState convState = (ConversationState) conversation.getAttachment();

      final boolean txOptimized = CommsUtils.requiresOptimizedTransaction(conversation);

      ChunkedMessageWrapper wrapper = null;

      /**************************************************************/
      /* Connection object id                                       */
      /**************************************************************/
      short connectionObjectId = request.getShort();  // BIT16 ConnectionObjectId

      /**************************************************************/
      /* Transaction id                                             */
      /**************************************************************/
      int transactionId = request.getSITransactionId(connectionObjectId, linkState, txOptimized);

      /**************************************************************/
      /* Chunk flags                                                */
      /**************************************************************/
      byte flags = request.get();
      boolean first = ((flags & CommsConstants.CHUNKED_MESSAGE_FIRST) == CommsConstants.CHUNKED_MESSAGE_FIRST);
      boolean last = ((flags & CommsConstants.CHUNKED_MESSAGE_LAST) == CommsConstants.CHUNKED_MESSAGE_LAST);

      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
      {
         SibTr.debug(tc, "connectionObjectId", connectionObjectId);
         SibTr.debug(tc, "transactionId", transactionId);
         SibTr.debug(tc, "flags", flags);
      }

      long wrapperId = StaticCATProducer.getWrapperId(connectionObjectId, (short) 0, transactionId);

      // If this is the first chunk of data, create a wrapper to save it in. Otherwise retrieve the
      // wrapper to append to.
      if (first)
      {
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "This is the first chunk of data");

         /**************************************************************/
         /* Destination address                                        */
         /**************************************************************/
         SIDestinationAddress destAddr = request.getSIDestinationAddress(conversation.getHandshakeProperties().getFapLevel());

         /**************************************************************/
         /* Reason code                                                */
         /**************************************************************/
         int reason = request.getInt();

         /**************************************************************/
         /* Alternate user                                             */
         /**************************************************************/
         String alternateUser = request.getString();

         /**************************************************************/
         /* Message Inserts                                            */
         /**************************************************************/
         String[] inserts = null;
         int numberOfInserts = request.getShort();
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Number of inserts", ""+numberOfInserts);

         if (numberOfInserts != 0)
         {
            inserts = new String[numberOfInserts];

            for (int x = 0; x < numberOfInserts; x++)
            {
               inserts[x] = request.getString();
               if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Insert[" + x + "] = " + inserts[x]);
            }
         }

         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
         {
            SibTr.debug(tc, "DestinationAddress", destAddr);
            SibTr.debug(tc, "Reason", ""+reason);
            SibTr.debug(tc, "AlternateUser", alternateUser);
            SibTr.debug(tc, "Message Inserts", Arrays.toString(inserts));
         }

         SICoreConnection connection =
            ((CATConnection) convState.getObject(connectionObjectId)).getSICoreConnection();

         // Get the transaction
         SITransaction siTran = linkState.getTransactionTable().get(transactionId);

         // Create a wrapper for this data to stash in the conversation state
         wrapper = new ChunkedMessageWrapper(siTran, connection, destAddr, reason, alternateUser, inserts);
         convState.putChunkedMessageWrapper(wrapperId, wrapper);
      }
      else
      {
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Appending to chunks already collected");
         wrapper = convState.getChunkedMessageWrapper(wrapperId);
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Appending to wrapper: ", wrapper);
      }

      // If the wrapper is null at this point, this is messed up
      if (wrapper == null)
      {
         SIErrorException e = new SIErrorException(
            TraceNLS.getFormattedMessage(CommsConstants.MSG_BUNDLE, "CHUNK_WRAPPER_NULL_SICO2165", null, null)
         );
         FFDCFilter.processException(e, CLASS_NAME + ".rcvSendChunkedToExceptionDest",
                                     CommsConstants.STATICCATDESTINATION_SENDCHUNKEDTOEXCEP_01,
                                     ""+wrapperId);
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Chunked message wrapper is null!");
         throw e;
      }

      // Now get the chunk from the message and add it to the wrapper
      wrapper.addDataSlice(request.getDataSlice());


      // If this was the last slice we have received all the data we need and we must now send the
      // message into the bus and (possibly) send back a reply to the client.
      if (last)
      {
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "This is the last chunk - sending message");

         // Remove the chunks from the conversation state
         convState.removeChunkedMessageWrapper(wrapperId);

         try
         {
            // Recreate the message
            SIBusMessage sibMessage = JsMessageFactory.getInstance().createInboundJsMessage(wrapper.getMessageData());

       
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            {
               SibTr.debug(tc, "Sending exception message - " + sibMessage);
               SibTr.debug(tc, "Destination: " + wrapper.getDestinationAddress());
               SibTr.debug(tc, "Discriminator: " + sibMessage.getDiscriminator());
               SibTr.debug(tc, "Reliability: " + sibMessage.getReliability());
            }

            if (wrapper.getTransaction() != IdToTransactionTable.INVALID_TRANSACTION)
            {
               wrapper.getConnection().sendToExceptionDestination(wrapper.getDestinationAddress(),
                                                                  sibMessage,
                                                                  wrapper.getReason(),
                                                                  wrapper.getMessageInserts(),
                                                                  wrapper.getTransaction(),
                                                                  wrapper.getAlternateUser());
            }

            try
            {
               conversation.send(poolManager.allocate(),
                                 JFapChannelConstants.SEG_SEND_CHUNKED_TO_EXCEPTION_DESTINATION_R,
                                 requestNumber,
                                 JFapChannelConstants.PRIORITY_MEDIUM,
                                 true,
                                 ThrottlingPolicy.BLOCK_THREAD,
                                 null);
            }
            catch (SIException e)
            {
               FFDCFilter.processException(e,
                                           CLASS_NAME + ".rcvSendChunkedToExceptionDest",
                                           CommsConstants.STATICCATDESTINATION_SENDCHUNKEDTOEXCEP_02);

               if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, e.getMessage(), e);

               SibTr.error(tc, "COMMUNICATION_ERROR_SICO2022", e);
            }
         }
         catch (SINotAuthorizedException e)
         {
            // No FFDC Code Needed
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, e.getMessage(), e);

            StaticCATHelper.sendExceptionToClient(e,
                                                  null,
                                                  conversation, requestNumber);
         }
         catch (SIException e)
         {
            //No FFDC code needed
            //Only FFDC if we haven't received a meTerminated event.
            if(!convState.hasMETerminated())
            {
               FFDCFilter.processException(e,
                                           CLASS_NAME + ".rcvSendChunkedToExceptionDest",
                                           CommsConstants.STATICCATDESTINATION_SENDCHUNKEDTOEXCEP_03);
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, e.getMessage(), e);

            StaticCATHelper.sendExceptionToClient(e,
                                                  CommsConstants.STATICCATDESTINATION_SENDCHUNKEDTOEXCEP_03,
                                                  conversation, requestNumber);
         }
         catch (Exception e)
         {
            FFDCFilter.processException(e,
                                        CLASS_NAME + ".rcvSendChunkedToExceptionDest",
                                        CommsConstants.STATICCATDESTINATION_SENDCHUNKEDTOEXCEP_04);

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, e.getMessage(), e);

            StaticCATHelper.sendExceptionToClient(e,
                                                  CommsConstants.STATICCATDESTINATION_SENDCHUNKEDTOEXCEP_04,
                                                  conversation, requestNumber);
         }
      }

      if (allocatedFromBufferPool) request.release();

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "rcvSendChunkedToExceptionDest");
   }
}
