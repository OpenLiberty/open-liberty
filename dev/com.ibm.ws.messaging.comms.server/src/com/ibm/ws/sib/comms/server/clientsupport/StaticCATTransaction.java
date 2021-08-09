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
package com.ibm.ws.sib.comms.server.clientsupport;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.comms.CommsConstants;
import com.ibm.ws.sib.comms.common.CommsByteBuffer;
import com.ibm.ws.sib.comms.common.CommsByteBufferPool;
import com.ibm.ws.sib.comms.server.ConversationState;
import com.ibm.ws.sib.comms.server.IdToTransactionTable;
import com.ibm.ws.sib.comms.server.ServerLinkLevelState;
import com.ibm.ws.sib.jfapchannel.Conversation;
import com.ibm.ws.sib.jfapchannel.JFapChannelConstants;
import com.ibm.ws.sib.jfapchannel.Conversation.ThrottlingPolicy;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.SICoreConnection;
import com.ibm.wsspi.sib.core.SITransaction;
import com.ibm.wsspi.sib.core.SIUncoordinatedTransaction;
import com.ibm.wsspi.sib.core.exception.SIRollbackException;

/**
 * This class takes care of all operations relating to transactions.
 *
 * @author Gareth Matthews
 */
public class StaticCATTransaction
{
   /** Class name for FFDC's */
   private static String CLASS_NAME = StaticCATTransaction.class.getName();

   /** The buffer pool manager */
   private static CommsByteBufferPool poolManager = CommsByteBufferPool.getInstance();

   /** The trace */
   private static final TraceComponent tc = SibTr.register(StaticCATTransaction.class,
                                                           CommsConstants.MSG_GROUP,
                                                           CommsConstants.MSG_BUNDLE);

   /** The NLS stuff */
   private static final TraceNLS nls = TraceNLS.getTraceNLS(CommsConstants.MSG_BUNDLE);

   /** Log class info on static load */
   static
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Source info: @(#)SIB/ws/code/sib.comms.server.impl/src/com/ibm/ws/sib/comms/server/clientsupport/StaticCATTransaction.java, SIB.comms, WASX.SIB, aa1225.01 1.50");
   }

   /**
    * Create an SIUncoordinatedTransaction.
    *
    * Mandatory Fields:
    * BIT16    SICoreConnectionObjectId
    * BIT32    Client transaction Id
    *
    * @param request
    * @param conversation
    * @param requestNumber
    * @param allocatedFromBufferPool
    * @param partOfExchange
    */
   static void rcvCreateUCTransaction(CommsByteBuffer request, Conversation conversation,
                                      int requestNumber, boolean allocatedFromBufferPool,
                                      boolean partOfExchange)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "rcvCreateUCTransaction",
                                           new Object[]
                                           {
                                              request,
                                              conversation,
                                              ""+requestNumber,
                                              ""+allocatedFromBufferPool
                                            });

      ConversationState convState = (ConversationState) conversation.getAttachment();

      short connectionObjectId = request.getShort();  // BIT16 ConnectionObjectId
      int clientTransactionId = request.getInt();     // BIT32 Client transaction Id

      // By default subordinates are allowed - however, a FAP version 5 client may specifically
      // request that a local transaction does not support them.
      // see the ConnectionProxy.createUncoordinateTransaction(boolean) method javadoc comment
      // for more details.
      boolean allowSubordinates = true;
      if (conversation.getHandshakeProperties().getFapLevel() >= JFapChannelConstants.FAP_VERSION_5)
      {
         allowSubordinates = (request.get() & 0x01) == 0x01;
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
      {
         SibTr.debug(tc, "SICoreConnection Id:", connectionObjectId);
         SibTr.debug(tc, "Client transaction Id:", clientTransactionId);
      }

      CATConnection catConn = (CATConnection) convState.getObject(connectionObjectId);
      SICoreConnection connection = catConn.getSICoreConnection();

      ServerLinkLevelState linkState = (ServerLinkLevelState) conversation.getLinkLevelAttachment();
      SIUncoordinatedTransaction ucTran = null;
      try
      {
         ucTran = connection.createUncoordinatedTransaction(allowSubordinates);

      }
      // Don't bother with all the different types seeing as we don't throw them back
      catch (SIException e)
      {
         //No FFDC code needed
         //Only FFDC if we haven't received a meTerminated event.
         if(!convState.hasMETerminated())
         {
            FFDCFilter.processException(e,
                                        CLASS_NAME + ".rcvCreateUCTransaction",
                                        CommsConstants.STATICCATTRANSACTION_CREATE_01);
         }

         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Failed to create the transaction", e);

         // We have to mark this transaction as error now - so as to indicate to everyone that
         // it should not be used
         linkState.getTransactionTable().addLocalTran(clientTransactionId, conversation.getId(), IdToTransactionTable.INVALID_TRANSACTION);
         linkState.getTransactionTable().markAsRollbackOnly(clientTransactionId, e);
      }

      // If transaction creation succeeded then add it to the link level state table of
      // transactions.
      if (ucTran != null)
      {
         linkState.getTransactionTable().addLocalTran(clientTransactionId,
                                                      conversation.getId(),
                                                      ucTran);
      }

      request.release(allocatedFromBufferPool);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "rcvCreateUCTransaction");
   }

   /**
    * Commit the transaction provided by the client.
    *
    * This method uses only required fields.  All mandatory fields have a fixed order and size.
    *
    * Mandatory Fields:
    * BIT16    SIMPConnectionObjectId
    * BIT16    SIMPTransactionObjectId
    *
    * @param request
    * @param conversation
    * @param requestNumber
    * @param allocatedFromBufferPool
    * @param partOfExchange
    */
   static void rcvCommitTransaction(CommsByteBuffer request, Conversation conversation,
                                    int requestNumber, boolean allocatedFromBufferPool,
                                    boolean partOfExchange)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "rcvCommitTransaction",
                                           new Object[]
                                           {
                                              request,
                                              conversation,
                                              ""+requestNumber,
                                              ""+allocatedFromBufferPool
                                            });

      short connectionObjectId = request.getShort();  // BIT16 ConnectionObjectId
      int clientTransactionId = request.getInt();     // BIT32 Client transaction Id

      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
      {
         SibTr.debug(tc, "SICoreConnection Id:", connectionObjectId);
         SibTr.debug(tc, "Client transaction Id:", clientTransactionId);
      }

      try
      {
         // Get the transaction out of the table
         ServerLinkLevelState linkState = (ServerLinkLevelState) conversation.getLinkLevelAttachment();
         SITransaction tran = linkState.getTransactionTable().get(clientTransactionId);

         // An earlier failure may mean that this transaction is rollback only
         boolean tranIsRollbackOnly =
            (tran == IdToTransactionTable.INVALID_TRANSACTION) ||
            linkState.getTransactionTable().isLocalTransactionRollbackOnly(clientTransactionId);

         if (tranIsRollbackOnly)
         {
            Throwable t = linkState.getTransactionTable().getExceptionForRollbackOnlyLocalTransaction(clientTransactionId);

            // At this point here the transaction will either be committed or rolled back.
            // Therefore, any subsequent operations will be invalid and will be thrown out
            // by the client. As such, remove it from the table.
            linkState.getTransactionTable().removeLocalTransaction(clientTransactionId);

            SIUncoordinatedTransaction siTran = (SIUncoordinatedTransaction)tran;

            //We don't want to rollback if this is IdToTransactionTable.INVALID_TRANSACTION
            if (siTran != null && siTran != IdToTransactionTable.INVALID_TRANSACTION) siTran.rollback();

            // And respond with an error
            SIRollbackException r = new SIRollbackException(
               nls.getFormattedMessage("TRANSACTION_MARKED_AS_ERROR_SICO2008",
                                       new Object[]{ t },
                                       null),
               t
            );

            StaticCATHelper.sendExceptionToClient(r,
                                                  null,
                                                  conversation, requestNumber);
         }
         else
         {
            // At this point here the transaction will either be committed or rolled back.
            // Therefore, any subsequent operations will be invalid and will be thrown out
            // by the client. As such, remove it from the table.
            linkState.getTransactionTable().removeLocalTransaction(clientTransactionId);

            // Otherwise commit - note this cannot be null here, otherwise we would be marked as error
            SIUncoordinatedTransaction siTran = (SIUncoordinatedTransaction)tran;
            siTran.commit();

            // Respond to the client
            try
            {
               conversation.send(poolManager.allocate(),
                                 JFapChannelConstants.SEG_COMMIT_TRANSACTION_R,
                                 requestNumber,
                                 JFapChannelConstants.PRIORITY_MEDIUM,
                                 true,
                                 ThrottlingPolicy.BLOCK_THREAD,
                                 null);
            }
            catch (SIException e)
            {
               FFDCFilter.processException(e,
                                           CLASS_NAME + ".rcvCommitTransaction",
                                           CommsConstants.STATICCATTRANSACTION_COMMIT_01);

               if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, e.getMessage(), e);

               SibTr.error(tc, "COMMUNICATION_ERROR_SICO2026", e);
            }
         }
      }
      catch (SIException e)
      {
         //No FFDC code needed
         //Only FFDC if we haven't received a meTerminated event.
         if(!((ConversationState)conversation.getAttachment()).hasMETerminated())
         {
            FFDCFilter.processException(e,
                                        CLASS_NAME + ".rcvCommitTransaction",
                                        CommsConstants.STATICCATTRANSACTION_COMMIT_02);
         }

         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, e.getMessage(), e);

         StaticCATHelper.sendExceptionToClient(e,
                                               CommsConstants.STATICCATTRANSACTION_COMMIT_02,
                                               conversation, requestNumber);
      }

      request.release(allocatedFromBufferPool);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "rcvCommitTransaction");
   }

   /**
    * Rollback the transaction provided by the client.
    *
    * Mandatory Fields:
    * BIT16    SICoreConnectionObjectId
    * BIT32    Client transaction Id
    *
    * @param request
    * @param conversation
    * @param requestNumber
    * @param allocatedFromBufferPool
    * @param partOfExchange
    */
   static void rcvRollbackTransaction(CommsByteBuffer request, Conversation conversation,
                                      int requestNumber, boolean allocatedFromBufferPool,
                                      boolean partOfExchange)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "rcvRollbackTransaction",
                                           new Object[]
                                           {
                                              request,
                                              conversation,
                                              ""+requestNumber,
                                              ""+allocatedFromBufferPool
                                            });

      short connectionObjectId = request.getShort();  // BIT16 ConnectionObjectId
      int clientTransactionId = request.getInt();     // BIT32 Client transaction Id

      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
      {
         SibTr.debug(tc, "SICoreConnection Id:", connectionObjectId);
         SibTr.debug(tc, "Client transaction Id:", clientTransactionId);
      }

      try
      {
         // Get the transaction out of the table
         ServerLinkLevelState linkState = (ServerLinkLevelState) conversation.getLinkLevelAttachment();
         SITransaction tran = linkState.getTransactionTable().get(clientTransactionId);

         if (tran == IdToTransactionTable.INVALID_TRANSACTION)
         {
            Throwable e = linkState.getTransactionTable().getExceptionForRollbackOnlyLocalTransaction(clientTransactionId);

            //Remove transaction from the table
            linkState.getTransactionTable().removeLocalTransaction(clientTransactionId);

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Looks like the transaction failed to create", e);
         }
         else
         {
            // At this point here the transaction will only be rolled back.
            // Therefore, any subsequent operations will be invalid and will be thrown out
            // by the client. As such, remove it from the table.
            linkState.getTransactionTable().removeLocalTransaction(clientTransactionId);

            SIUncoordinatedTransaction siTran = (SIUncoordinatedTransaction)tran;
            siTran.rollback();
         }

         try
         {
            conversation.send(poolManager.allocate(),
                              JFapChannelConstants.SEG_ROLLBACK_TRANSACTION_R,
                              requestNumber,
                              JFapChannelConstants.PRIORITY_MEDIUM,
                              true,
                              ThrottlingPolicy.BLOCK_THREAD,
                              null);
         }
         catch (SIException e)
         {
            FFDCFilter.processException(e,
                                        CLASS_NAME + ".rcvRollbackTransaction",
                                        CommsConstants.STATICCATTRANSACTION_ROLLBACK_01);

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, e.getMessage(), e);

            SibTr.error(tc, "COMMUNICATION_ERROR_SICO2026", e);
         }
      }
      catch (SIException e)
      {
         //No FFDC code needed
         //Only FFDC if we haven't received a meTerminated event.
         if(!((ConversationState)conversation.getAttachment()).hasMETerminated())
         {
            FFDCFilter.processException(e,
                                       CLASS_NAME + ".rcvRollbackTransaction",
                                       CommsConstants.STATICCATTRANSACTION_ROLLBACK_02);
         }

         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, e.getMessage(), e);

         StaticCATHelper.sendExceptionToClient(e,
                                               CommsConstants.STATICCATTRANSACTION_ROLLBACK_02,
                                               conversation, requestNumber);
      }

      request.release(allocatedFromBufferPool);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "rcvRollbackTransaction");
   }
}
