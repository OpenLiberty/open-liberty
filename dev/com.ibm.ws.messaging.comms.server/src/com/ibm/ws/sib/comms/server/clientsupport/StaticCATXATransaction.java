/*******************************************************************************
 * Copyright (c) 2004, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.comms.server.clientsupport;

import javax.transaction.xa.XAException;
import javax.transaction.xa.Xid;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.comms.CommsConstants;
import com.ibm.ws.sib.comms.common.CommsByteBuffer;
import com.ibm.ws.sib.comms.common.CommsByteBufferPool;
import com.ibm.ws.sib.comms.common.CommsUtils;
import com.ibm.ws.sib.comms.common.XidProxy;
import com.ibm.ws.sib.comms.server.ConversationState;
import com.ibm.ws.sib.comms.server.IdToTransactionTable;
import com.ibm.ws.sib.comms.server.ServerLinkLevelState;
import com.ibm.ws.sib.jfapchannel.Conversation;
import com.ibm.ws.sib.jfapchannel.JFapChannelConstants;
import com.ibm.ws.sib.jfapchannel.Conversation.ThrottlingPolicy;
import com.ibm.ws.sib.transactions.mpspecific.MSSIXAResourceProvider;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.SICoreConnection;
import com.ibm.wsspi.sib.core.SITransaction;
import com.ibm.wsspi.sib.core.SIXAResource;
import com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException;

/**
 * This class take care of acting on proxied requests to the XAResource
 * by calling them on the actual XAResource.
 *
 * @author Gareth Matthews
 */
public class StaticCATXATransaction
{
   /** Class name for FFDC's */
   private static String CLASS_NAME = StaticCATXATransaction.class.getName();

   /** Buffer pool manager handle */
   private static CommsByteBufferPool poolManager = CommsByteBufferPool.getInstance();

   /** Registers our trace component */
   private static final TraceComponent tc = SibTr.register(StaticCATXATransaction.class,
                                                           CommsConstants.MSG_GROUP,
                                                           CommsConstants.MSG_BUNDLE);

   /** Our NLS reference object */
   private static final TraceNLS nls = TraceNLS.getTraceNLS(CommsConstants.MSG_BUNDLE);

   /** Log source info on class load */
   static
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Source info: @(#)SIB/ws/code/sib.comms.server.impl/src/com/ibm/ws/sib/comms/server/clientsupport/StaticCATXATransaction.java, SIB.comms, WASX.SIB, aa1225.01 1.55");
   }

   /**
    * Gets the XAResource from the SICoreConnection and stores it in the
    * object store.
    *
    * Fields:
    *
    * BIT16    ConnectionObjectId
    *
    * @param request
    * @param conversation
    * @param requestNumber
    * @param allocatedFromBufferPool
    * @param partOfExchange
    */
   public static void rcvXAOpen(CommsByteBuffer request, Conversation conversation,
                                int requestNumber, boolean allocatedFromBufferPool,
                                boolean partOfExchange)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "rcvXAOpen",
                                           new Object[]
                                           {
                                              request,
                                              conversation,
                                              ""+requestNumber,
                                              ""+allocatedFromBufferPool,
                                              ""+partOfExchange
                                            });

      short connectionObjectId = request.getShort();
      int clientTransactionId = request.getInt();

      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
      {
         SibTr.debug(tc, "Connection Object ID", connectionObjectId);
         SibTr.debug(tc, "Transaction ID", clientTransactionId);
      }

      try
      {
         conversation.send(poolManager.allocate(),
                           JFapChannelConstants.SEG_XAOPEN_R,
                           requestNumber,
                           JFapChannelConstants.PRIORITY_MEDIUM,
                           true,
                           ThrottlingPolicy.BLOCK_THREAD,
                           null);
      }
      catch (SIException e)
      {
         FFDCFilter.processException(e,
                                     CLASS_NAME + ".rcvXAOpen",
                                     CommsConstants.STATICCATXATRANSACTION_XAOPEN_01);

         SibTr.error(tc, "COMMUNICATION_ERROR_SICO2027", e);
      }

      request.release(allocatedFromBufferPool);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "rcvXAOpen");
   }

   /**
    * Calls start() on the SIXAResource.
    *
    * Fields:
    *
    * BIT16    XAResourceId
    * The XID Structure
    * BIT32    Flags
    *
    * @param request
    * @param conversation
    * @param requestNumber
    * @param allocatedFromBufferPool
    * @param partOfExchange
    */
   public static void rcvXAStart(CommsByteBuffer request, Conversation conversation,
                                 int requestNumber, boolean allocatedFromBufferPool,
                                 boolean partOfExchange)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "rcvXAStart",
                                           new Object[]
                                           {
                                              request,
                                              conversation,
                                              ""+requestNumber,
                                              ""+allocatedFromBufferPool,
                                              ""+partOfExchange
                                            });

      ConversationState convState = (ConversationState) conversation.getAttachment();

      try
      {
         int clientTransactionId = request.getInt();

         CATConnection catConn = (CATConnection) convState.getObject(convState.getConnectionObjectId());
         SICoreConnection connection = catConn.getSICoreConnection();

         ServerLinkLevelState linkState = (ServerLinkLevelState) conversation.getLinkLevelAttachment();
         SIXAResource xaResource = (SIXAResource)linkState.getTransactionTable().get(clientTransactionId, true);
         if (xaResource == null)
         {
            try {
                  xaResource = connection.getSIXAResource();
            }
            catch (SIConnectionUnavailableException e)
            {
               //No FFDC code needed
               //Only FFDC if we haven't received a meTerminated event.
               if(!convState.hasMETerminated())
               {
                  FFDCFilter.processException(e, CLASS_NAME + ".rcvXAStart",
                                              CommsConstants.STATICCATXATRANSACTION_XASTART_04);
               }

               // This should never happen.  We are running inside the
               // application server - so we should always be able to
               // contact a messaging engine.  Get very upset about this

               if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.exception(tc, e);
               throw new SIErrorException(e);
            }
            catch (SIResourceException e)
            {
               FFDCFilter.processException(e,
                     CLASS_NAME + ".rcvXAStart",
                     CommsConstants.STATICCATXATRANSACTION_XASTART_05);
               if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.exception(tc, e);
               throw new XAException(XAException.XAER_RMERR);
            }
         }

         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
         {
            SibTr.debug(tc, "XAResource Object ID", clientTransactionId);
         }

         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Getting Xid");
         Xid xid = request.getXid();
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Completed:", xid);

         int flags = request.getInt();
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Flags: ", flags);

         // Now call the method on the XA resource
         xaResource.start(xid, flags);

         linkState.getTransactionTable().addGlobalTransactionBranch(clientTransactionId,
                                                                    conversation.getId(),
                                                                    xaResource,
                                                                    (XidProxy) xid,
                                                                    false);

         try
         {
            conversation.send(poolManager.allocate(),
                              JFapChannelConstants.SEG_XASTART_R,
                              requestNumber,
                              JFapChannelConstants.PRIORITY_MEDIUM,
                              true,
                              ThrottlingPolicy.BLOCK_THREAD,
                              null);
         }
         catch (SIException e)
         {
            FFDCFilter.processException(e,
                                        CLASS_NAME + ".rcvXAStart",
                                        CommsConstants.STATICCATXATRANSACTION_XASTART_01);

            SibTr.error(tc, "COMMUNICATION_ERROR_SICO2027", e);
         }
      }
      catch (XAException e)
      {
         FFDCFilter.processException(e,
                                     CLASS_NAME + ".rcvXAStart",
                                     CommsConstants.STATICCATXATRANSACTION_XASTART_02);

         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "XAException - RC: " + e.errorCode, e);

         StaticCATHelper.sendExceptionToClient(e,
                                               CommsConstants.STATICCATXATRANSACTION_XASTART_02,
                                               conversation, requestNumber);
      }

      request.release(allocatedFromBufferPool);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "rcvXAStart");
   }

   /**
    * Calls end() on the SIXAResource.
    *
    * Fields:
    *
    * BIT16    XAResourceId
    * The XID Structure
    * BIT32    Flags
    *
    * @param request
    * @param conversation
    * @param requestNumber
    * @param allocatedFromBufferPool
    * @param partOfExchange
    */
   public static void rcvXAEnd(CommsByteBuffer request, Conversation conversation,
                               int requestNumber, boolean allocatedFromBufferPool,
                               boolean partOfExchange)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "rcvXAEnd",
                                           new Object[]
                                           {
                                              request,
                                              conversation,
                                              ""+requestNumber,
                                              ""+allocatedFromBufferPool,
                                              ""+partOfExchange
                                            });

      ConversationState convState = (ConversationState) conversation.getAttachment();
      ServerLinkLevelState linkState = (ServerLinkLevelState) conversation.getLinkLevelAttachment();

      try
      {
         int clientTransactionId = request.getInt();
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
         {
            SibTr.debug(tc, "XAResource Object ID", clientTransactionId);
         }

         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Getting Xid");
         Xid xid = request.getXid();
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Completed:", xid);

         int flags = request.getInt();
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Flags: ", flags);

         boolean requiresMSResource = false;
         if (conversation.getHandshakeProperties().getFapLevel() >= JFapChannelConstants.FAP_VERSION_5)
         {
            requiresMSResource = request.get() == 0x01;
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "requires MS resource: ", ""+requiresMSResource);
         }

         // Get the transaction out of the table
         SITransaction tran = linkState.getTransactionTable().get(clientTransactionId);
         linkState.getTransactionTable().endGlobalTransactionBranch(clientTransactionId,
                                                                    (XidProxy) xid);

         if (tran != IdToTransactionTable.INVALID_TRANSACTION)
         {
            // Get the actual transaction ...
            SIXAResource xaResource = getResourceFromTran(tran, convState, requiresMSResource);

            // Now call the method on the XA resource
            xaResource.end(xid, flags);
         }

         try
         {
            conversation.send(poolManager.allocate(),
                              JFapChannelConstants.SEG_XAEND_R,
                              requestNumber,
                              JFapChannelConstants.PRIORITY_MEDIUM,
                              true,
                              ThrottlingPolicy.BLOCK_THREAD,
                              null);
         }
         catch (SIException e)
         {
            FFDCFilter.processException(e,
                                        CLASS_NAME + ".rcvXAClose",
                                        CommsConstants.STATICCATXATRANSACTION_XAEND_01);

            SibTr.error(tc, "COMMUNICATION_ERROR_SICO2027", e);
         }
      }
      catch (XAException e)
      {
         FFDCFilter.processException(e,
                                     CLASS_NAME + ".rcvXAClose",
                                     CommsConstants.STATICCATXATRANSACTION_XAEND_02);

         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "XAException - RC: " + e.errorCode, e);

         StaticCATHelper.sendExceptionToClient(e,
                                               CommsConstants.STATICCATXATRANSACTION_XAEND_02,        // d186970
                                               conversation, requestNumber);
      }

      request.release(allocatedFromBufferPool);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "rcvXAEnd");
   }

   /**
    * Calls prepare() on the SIXAResource.
    *
    * Fields:
    *
    * BIT16    XAResourceId
    * The XID Structure
    *
    * @param request
    * @param conversation
    * @param requestNumber
    * @param allocatedFromBufferPool
    * @param partOfExchange
    */
   public static void rcvXAPrepare(CommsByteBuffer request,
                                   Conversation conversation,
                                   int requestNumber,
                                   boolean allocatedFromBufferPool,
                                   boolean partOfExchange)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "rcvXAPrepare",
                                           new Object[]
                                           {
                                              request,
                                              conversation,
                                              ""+requestNumber,
                                              ""+allocatedFromBufferPool,
                                              ""+partOfExchange
                                            });

      // PK61176 We need to clean up tran state in catch block if required. Scope vars appropriately.
      ConversationState convState = (ConversationState) conversation.getAttachment();
      ServerLinkLevelState linkState = (ServerLinkLevelState) conversation.getLinkLevelAttachment();
      SITransaction tran = null;
      int clientTransactionId = 0;
      XidProxy xid = null;

      final boolean optimizedTx = CommsUtils.requiresOptimizedTransaction(conversation);

      try
      {
         clientTransactionId = request.getInt();
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
         {
            SibTr.debug(tc, "XAResource Object ID", clientTransactionId);
         }

         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Getting Xid");
         xid = (XidProxy) request.getXid();
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Completed:", xid);

         boolean endRequired = false;
         int endFlags = 0;
         if (optimizedTx)
         {
            endRequired = request.get() == 0x01;
            endFlags = request.getInt();

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            {
               SibTr.debug(tc, "End Required:", ""+endRequired);
               SibTr.debug(tc, "End Flags:", ""+endFlags);
            }
         }

         boolean requiresMSResource = false;
         if (conversation.getHandshakeProperties().getFapLevel() >= JFapChannelConstants.FAP_VERSION_5)
         {
            requiresMSResource = request.get() == 0x01;
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Requires MS Resource:", ""+requiresMSResource);
         }

         // Get the transaction out of the table
         if (endRequired) linkState.getTransactionTable().endOptimizedGlobalTransactionBranch(clientTransactionId, endFlags);
         tran = linkState.getTransactionTable().getResourceForGlobalTransactionBranch(clientTransactionId, xid);

         // If the UOW was never created (because we were running with the
         // optimization that gets an XAResource and enlists it as part of the
         // first piece of transacted work and this failed) then throw
         // an exception notifying the caller that the UOW has been rolled back.
         if (tran == IdToTransactionTable.INVALID_TRANSACTION)
         {
            Throwable throwable =
               linkState.getTransactionTable().getExceptionForRollbackOnlyGlobalTransactionBranch(clientTransactionId, xid);
            String errorMsg =
               nls.getFormattedMessage("TRANSACTION_MARKED_AS_ERROR_SICO2029",
                     new Object[]{ throwable },
                     null);

            XAException xa = new XAException(errorMsg);
            xa.initCause(throwable);
            xa.errorCode = XAException.XA_RBOTHER;
            throw xa;
         }

         // Get the actual transaction ...
         SIXAResource xaResource = getResourceFromTran(tran, convState, requiresMSResource);

         boolean isUnitOfWorkInError = false;
         if (tran != null)
         {
            isUnitOfWorkInError =
               linkState.getTransactionTable().isGlobalTransactionBranchRollbackOnly(clientTransactionId, xid);
         }

         if (isUnitOfWorkInError)
         {
            // PK59276 Rollback the transaction, as after we throw a XA_RB* error the
            // transaction manager will not call us with xa_rollback.
            xaResource.rollback(xid);

            Throwable throwable = linkState.getTransactionTable().getExceptionForRollbackOnlyGlobalTransactionBranch(clientTransactionId, xid);

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "The transaction was marked as error due to",
                                                 throwable);

            // And respond with an error
            XAException xa = new XAException(
               nls.getFormattedMessage("TRANSACTION_MARKED_AS_ERROR_SICO2029",
                                       new Object[]{ throwable },
                                       null)
            );
            xa.initCause(throwable);
            xa.errorCode = XAException.XA_RBOTHER;

            throw xa;
         }

         int rc = 0;
         try
         {
            // Now call the method on the XA resource
            rc = xaResource.prepare(xid);
         }
         finally
         {
            // Now mark the transaction as no longer in doubt. Ensure we always do this
            if (tran != null)
            {
               // Remove transaction from table - as it is no longer in flight
               linkState.getTransactionTable().removeGlobalTransactionBranch(clientTransactionId, xid);
            }
         }

         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Prepare returned: " + rc);

         // PM23626 If we have enlisted in a read only transaction, for example when the only action within
         // the transaction is a synchronous receive that returned null, we also need to remove
         // the transaction from the dispatchable map - as we will not be called with commit/rollback.
         if (rc == XAException.XA_RDONLY)
         {
            linkState.getDispatchableMap().removeDispatchableForGlobalTransaction(clientTransactionId, xid);
         }

         CommsByteBuffer reply = poolManager.allocate();
         reply.putInt(rc);

         try
         {
            conversation.send(reply,
                              JFapChannelConstants.SEG_XAPREPARE_R,
                              requestNumber,
                              JFapChannelConstants.PRIORITY_MEDIUM,
                              true,
                              ThrottlingPolicy.BLOCK_THREAD,
                              null);
         }
         catch (SIException e)
         {
            FFDCFilter.processException(e,
                                        CLASS_NAME + ".rcvXAPrepare",
                                        CommsConstants.STATICCATXATRANSACTION_XAPREPARE_01);

            SibTr.error(tc, "COMMUNICATION_ERROR_SICO2027", e);
         }
      }
      catch (XAException e)
      {
         FFDCFilter.processException(e, CLASS_NAME + ".rcvXAPrepare", CommsConstants.STATICCATXATRANSACTION_XAPREPARE_02);
         
         // PK61176 If the XAException is a XA_RBXXXX then this will be the last time we will get called.
         // Remove transaction from table - as it is no longer in flight
         if (tran != null &&
             e.errorCode >= XAException.XA_RBBASE && e.errorCode <= XAException.XA_RBEND)
         {
           try
           {
             linkState.getTransactionTable().removeGlobalTransactionBranch(clientTransactionId, xid);
           }
           catch (SIErrorException sie)
           {
             // No FFDC Code Needed
             // We could potentially throw this exception because we have already called removeGlobalTransactionBranch in
             //  the finally block of the xaResource.prepare call. As other calls outside of that try-finally block can
             //  throw XAExceptions then we have no guarantee that we even called prepare so we still need this call here in
             //  the catch. We can't add a finally block to this call as we only want to remove the global tran in the case were
             //  we called prepare. The safest fix is just to catch this exception and move on.
             if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Caught SIErrorException when calling remoteGlobalTransactionBranch: " + sie);
           }
           
           linkState.getDispatchableMap().removeDispatchableForGlobalTransaction(clientTransactionId, xid);
         }

         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "XAException - RC: " + e.errorCode, e);

         StaticCATHelper.sendExceptionToClient(e,
                                               CommsConstants.STATICCATXATRANSACTION_XAPREPARE_02,
                                               conversation, requestNumber);
      }

      request.release(allocatedFromBufferPool);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "rcvXAPrepare");
   }

   /**
    * Calls commit() on the SIXAResource.
    *
    * Fields:
    *
    * BIT16    XAResourceId
    * The XID Structure
    * BYTE     OnePhase (0x00 = false, 0x01 = true)
    *
    * @param request
    * @param conversation
    * @param requestNumber
    * @param allocatedFromBufferPool
    * @param partOfExchange
    */
   public static void rcvXACommit(CommsByteBuffer request,
                                  Conversation conversation,
                                  int requestNumber,
                                  boolean allocatedFromBufferPool,
                                  boolean partOfExchange)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "rcvXACommit",
                                           new Object[]
                                           {
                                              request,
                                              conversation,
                                              ""+requestNumber,
                                              ""+allocatedFromBufferPool,
                                              ""+partOfExchange
                                            });

      ConversationState convState = (ConversationState) conversation.getAttachment();
      ServerLinkLevelState linkState = (ServerLinkLevelState) conversation.getLinkLevelAttachment();

      boolean onePhase = false;

      final boolean optimizedTx = CommsUtils.requiresOptimizedTransaction(conversation);

      try
      {
         int clientTransactionId = request.getInt();
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
         {
            SibTr.debug(tc, "XAResource Object ID", clientTransactionId);
         }

         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Getting Xid");
         XidProxy xid = (XidProxy) request.getXid();
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Completed:", xid);

         byte onePhaseByte = request.get();
         if (onePhaseByte == 1) onePhase = true;
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "One phase:", onePhase);

         boolean endRequired = false;
         int endFlags = 0;
         if (optimizedTx)
         {
            endRequired = request.get() == 0x01;
            endFlags = request.getInt();

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            {
               SibTr.debug(tc, "End Required:", ""+endRequired);
               SibTr.debug(tc, "End Flags:", ""+endFlags);
            }
         }

         boolean requiresMSResource = false;
         if (conversation.getHandshakeProperties().getFapLevel() >= JFapChannelConstants.FAP_VERSION_5)
         {
            requiresMSResource = request.get() == 0x01;
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Requires MS Resource:", ""+requiresMSResource);
         }

         // Get the transaction out of the table
         IdToTransactionTable transactionTable = linkState.getTransactionTable();
         if (endRequired) transactionTable.endOptimizedGlobalTransactionBranch(clientTransactionId, endFlags);
         SITransaction tran = null;

         // As commits (for the same XAResource) can be scheduled concurrently by the receive listener
         // dispatcher - perform all updates to the IdToTransaction table inside a synchronized block.
         boolean isInvalidTransaction = false;
         boolean isUnitOfWorkInError = false;
         Throwable rollbackException = null;
         synchronized(transactionTable)
         {
            tran = transactionTable.getResourceForGlobalTransactionBranch(clientTransactionId, xid);
            if (tran != null)
            {
               isInvalidTransaction = tran == IdToTransactionTable.INVALID_TRANSACTION;
               if (!isInvalidTransaction)
               {
                  isUnitOfWorkInError =
                     transactionTable.isGlobalTransactionBranchRollbackOnly(clientTransactionId, xid);
               }

               if (isInvalidTransaction || isUnitOfWorkInError)
               {
                  rollbackException =
                     transactionTable.getExceptionForRollbackOnlyGlobalTransactionBranch(clientTransactionId, xid);
               }

               // Leave the invalid transaction in the table.  The only way to remove it is to
               // roll it back.
               if (!isInvalidTransaction)
               {
                  transactionTable.removeGlobalTransactionBranch(clientTransactionId, xid);
               }
            }
         }

         SIXAResource xaResource = getResourceFromTran(tran, convState, requiresMSResource);

         // If the UOW was never created (because we were running with the
         // optimization that gets an XAResource and enlists it as part of the
         // first piece of transacted work and this failed) then throw
         // an exception notifying the caller that the UOW has been rolled back.
         if (isInvalidTransaction)
         {
            String errorMsg =
               nls.getFormattedMessage("TRANSACTION_MARKED_AS_ERROR_SICO2029",
                     new Object[]{ rollbackException },
                     null);

            XAException xa = new XAException(errorMsg);
            xa.initCause(rollbackException);
            xa.errorCode = XAException.XA_RBOTHER;

            throw xa;
         }

         if (isUnitOfWorkInError)
         {
            // PK59276 Rollback the transaction, as after we throw a XA_RB* error the
            // transaction manager will not call us with xa_rollback.
            xaResource.rollback(xid);

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "The transaction was marked as error due to",
                                                 rollbackException);

            // And respond with an error
            XAException xa = new XAException(
               nls.getFormattedMessage("TRANSACTION_MARKED_AS_ERROR_SICO2029",
                                       new Object[]{ rollbackException },
                                       null)
            );
            xa.initCause(rollbackException);
            xa.errorCode = XAException.XA_RBOTHER;

            throw xa;
         }

         // Now call the method on the XA resource
         xaResource.commit(xid, onePhase);

         try
         {
            conversation.send(poolManager.allocate(),
                              JFapChannelConstants.SEG_XACOMMIT_R,
                              requestNumber,
                              JFapChannelConstants.PRIORITY_MEDIUM,
                              true,
                              ThrottlingPolicy.BLOCK_THREAD,
                              null);
         }
         catch (SIException e)
         {
            FFDCFilter.processException(e,
                                        CLASS_NAME + ".rcvXACommit",
                                        CommsConstants.STATICCATXATRANSACTION_XACOMMIT_01);

            SibTr.error(tc, "COMMUNICATION_ERROR_SICO2027", e);
         }
      }
      catch (XAException e)
      {
         FFDCFilter.processException(e,
                                     CLASS_NAME + ".rcvXACommit",
                                     CommsConstants.STATICCATXATRANSACTION_XACOMMIT_02);

         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "XAException - RC: " + e.errorCode, e);

         StaticCATHelper.sendExceptionToClient(e,
                                               CommsConstants.STATICCATXATRANSACTION_XACOMMIT_02,     // d186970
                                               conversation, requestNumber);
      }

      request.release(allocatedFromBufferPool);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "rcvXACommit");
   }

        /**
    * Calls rollback() on the SIXAResource.
    *
    * Fields:
    *
    * BIT16    XAResourceId
    * The XID Structure
    *
    * @param request
    * @param conversation
    * @param requestNumber
    * @param allocatedFromBufferPool
    * @param partOfExchange
    */
   public static void rcvXARollback(CommsByteBuffer request,
                                    Conversation conversation,
                                    int requestNumber,
                                    boolean allocatedFromBufferPool,
                                    boolean partOfExchange)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "rcvXARollback",
                                           new Object[]
                                           {
                                              request,
                                              conversation,
                                              ""+requestNumber,
                                              ""+allocatedFromBufferPool,
                                              ""+partOfExchange
                                            });

      ConversationState convState = (ConversationState) conversation.getAttachment();
      ServerLinkLevelState linkState = (ServerLinkLevelState) conversation.getLinkLevelAttachment();

      final boolean optimizedTx = CommsUtils.requiresOptimizedTransaction(conversation);

      try
      {
         int clientTransactionId = request.getInt();
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
         {
            SibTr.debug(tc, "XAResource Object ID", clientTransactionId);
         }

         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Getting Xid");
         XidProxy xid = (XidProxy) request.getXid();
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Completed:", xid);

         boolean endRequired = false;
         int endFlags = 0;
         if (optimizedTx)
         {
            endRequired = request.get() == 0x01;
            endFlags = request.getInt();

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            {
               SibTr.debug(tc, "End Required:", ""+endRequired);
               SibTr.debug(tc, "End Flags:", ""+endFlags);
            }
         }

         boolean requiresMSResource = false;
         if (conversation.getHandshakeProperties().getFapLevel() >= JFapChannelConstants.FAP_VERSION_5)
         {
            requiresMSResource = request.get() == 0x01;
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Requires MS Resource:", ""+requiresMSResource);
         }

         // Get the transaction out of the table
         IdToTransactionTable transactionTable = linkState.getTransactionTable();
         if (endRequired) transactionTable.endOptimizedGlobalTransactionBranch(clientTransactionId, endFlags);

         // Update transaction table atomically as receive listener dispatcher may dispatch
         // rollback (or commit + rollback) requests concurrently.
         SITransaction tran = null;
         synchronized(transactionTable)
         {
            tran = transactionTable.getResourceForGlobalTransactionBranch(clientTransactionId, xid);
            if (tran != null)
            {
               transactionTable.removeGlobalTransactionBranch(clientTransactionId, xid);
            }
         }

         if (tran != IdToTransactionTable.INVALID_TRANSACTION)
         {
            // Get the actual transaction ...
            SIXAResource xaResource = getResourceFromTran(tran, convState, requiresMSResource);

            // Now call the method on the XA resource
            xaResource.rollback(xid);
         }

         try
         {
            conversation.send(poolManager.allocate(),
                              JFapChannelConstants.SEG_XAROLLBACK_R,
                              requestNumber,
                              JFapChannelConstants.PRIORITY_MEDIUM,
                              true,
                              ThrottlingPolicy.BLOCK_THREAD,
                              null);
         }
         catch (SIException e)
         {
            FFDCFilter.processException(e,
                                        CLASS_NAME + ".rcvXARollback",
                                        CommsConstants.STATICCATXATRANSACTION_XAROLLBACK_01);

            SibTr.error(tc, "COMMUNICATION_ERROR_SICO2027", e);
         }
      }
      catch (XAException e)
      {
         FFDCFilter.processException(e,
                                     CLASS_NAME + ".rcvXARollback",
                                     CommsConstants.STATICCATXATRANSACTION_XAROLLBACK_02);

         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "XAException - RC: " + e.errorCode, e);

         StaticCATHelper.sendExceptionToClient(e,
                                               CommsConstants.STATICCATXATRANSACTION_XAROLLBACK_02,
                                               conversation, requestNumber);
      }

      request.release(allocatedFromBufferPool);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "rcvXARollback");
   }

   /**
    * Calls recover() on the SIXAResource.
    *
    * Fields:
    *
    * BIT16    XAResourceId
    * BIT32    Flags
    *
    * @param request
    * @param conversation
    * @param requestNumber
    * @param allocatedFromBufferPool
    * @param partOfExchange
    */
   public static void rcvXARecover(CommsByteBuffer request, Conversation conversation,
                                   int requestNumber, boolean allocatedFromBufferPool,
                                   boolean partOfExchange)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "rcvXARecover",
                                           new Object[]
                                           {
                                              request,
                                              conversation,
                                              ""+requestNumber,
                                              ""+allocatedFromBufferPool,
                                              ""+partOfExchange
                                            });

      ConversationState convState = (ConversationState) conversation.getAttachment();
      ServerLinkLevelState linkState = (ServerLinkLevelState) conversation.getLinkLevelAttachment();

      try
      {
         int clientTransactionId = request.getInt();
         int flags = request.getInt();

         boolean requiresMSResource = false;
         if (conversation.getHandshakeProperties().getFapLevel() >= JFapChannelConstants.FAP_VERSION_5)
         {
            requiresMSResource = request.get() == 0x01;
         }

         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
         {
            SibTr.debug(tc, "XAResource Object ID", clientTransactionId);
            SibTr.debug(tc, "Flags: ", flags);
         }

         // Get the transaction out of the table
         SITransaction tran = linkState.getTransactionTable().get(clientTransactionId, true);           // D297060
         if (tran == IdToTransactionTable.INVALID_TRANSACTION)
         {
            // This is a odd (but not completely impossible situation to
            // be in).  The client has created an optimized XA transaction
            // (i.e. one in which we get the XA resource and enlist it
            // during the first transacted operation) yet the code that
            // is responsible for doing this has failed to get an XA
            // resource.  The client has then called recover on the
            // resource.  This is a very strange useage pattern for the
            // resource.  Never the less - the only reasonable course
            // of action is to signal a RM error.
            throw new XAException(XAException.XAER_RMERR);
         }

         // Get the actual transaction ...
         SIXAResource xaResource = getResourceFromTran(tran, convState, requiresMSResource);

         // Now call the method on the XA resource
         Xid[] xids = xaResource.recover(flags);
         CommsByteBuffer reply = poolManager.allocate();

         if (xids == null)
         {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "recover() returned null");
            reply.putShort(0);
         }
         else
         {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Serializing " + xids.length + " Xids");

            reply.putShort(xids.length);

            // Now for each one, serialize it and add it to the buffer list to send back
            for (int x = 0; x < xids.length; x++)
            {
               reply.putXid(xids[x]);
            }
         }

         try
         {
            conversation.send(reply,
                              JFapChannelConstants.SEG_XARECOVER_R,
                              requestNumber,
                              JFapChannelConstants.PRIORITY_MEDIUM,
                              true,
                              ThrottlingPolicy.BLOCK_THREAD,
                              null);
         }
         catch (SIException e)
         {
            FFDCFilter.processException(e,
                                        CLASS_NAME + ".rcvXARecover",
                                        CommsConstants.STATICCATXATRANSACTION_XARECOVER_01);

            SibTr.error(tc, "COMMUNICATION_ERROR_SICO2027", e);
         }
      }
      catch (XAException e)
      {
         FFDCFilter.processException(e,
                                     CLASS_NAME + ".rcvXARecover",
                                     CommsConstants.STATICCATXATRANSACTION_XARECOVER_02);

         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "XAException - RC: " + e.errorCode, e);

         StaticCATHelper.sendExceptionToClient(e,
                                               CommsConstants.STATICCATXATRANSACTION_XARECOVER_02,
                                               conversation, requestNumber);
      }

      request.release(allocatedFromBufferPool);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "rcvXARecover");
   }

   /**
    * Calls forget() on the SIXAResource.
    *
    * Fields:
    *
    * BIT16    XAResourceId
    * The XID Structure
    *
    * @param request
    * @param conversation
    * @param requestNumber
    * @param allocatedFromBufferPool
    * @param partOfExchange
    */
   public static void rcvXAForget(CommsByteBuffer request, Conversation conversation,
                                  int requestNumber, boolean allocatedFromBufferPool,
                                  boolean partOfExchange)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "rcvXAForget",
                                           new Object[]
                                           {
                                              request,
                                              conversation,
                                              ""+requestNumber,
                                              ""+allocatedFromBufferPool,
                                              ""+partOfExchange
                                            });

      ConversationState convState = (ConversationState) conversation.getAttachment();
      ServerLinkLevelState linkState = (ServerLinkLevelState) conversation.getLinkLevelAttachment();

      try
      {
         int clientTransactionId = request.getInt();
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
         {
            SibTr.debug(tc, "XAResource Object ID", clientTransactionId);
         }

         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Getting Xid");
         XidProxy xid = (XidProxy) request.getXid();
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Completed:", xid);

         boolean requiresMSResource = false;
         if (conversation.getHandshakeProperties().getFapLevel() >= JFapChannelConstants.FAP_VERSION_5)
         {
            requiresMSResource = request.get() == 0x01;
         }

         // Get the transaction out of the table
         SITransaction tran = linkState.getTransactionTable().getResourceForGlobalTransactionBranch(clientTransactionId, xid);

         if (tran == IdToTransactionTable.INVALID_TRANSACTION)
         {
            // If the UOW was never created (because we were running with the
            // optimization that gets an XAResource and enlists it as part of the
            // first piece of transacted work and this failed) then simply
            // remove any reference of the UOW from out table.
            linkState.getTransactionTable().removeGlobalTransactionBranch(clientTransactionId, xid);
         }
         else
         {
            // Get the actual transaction ...
            SIXAResource xaResource = getResourceFromTran(tran, convState, requiresMSResource);

            // Now call the method on the XA resource
            xaResource.forget(xid);

            // If the transaction was in the in-flight table - remove it.
            if (tran != null)
            {
               linkState.getTransactionTable().removeGlobalTransactionBranch(clientTransactionId, xid);
            }
         }

         try
         {
            conversation.send(poolManager.allocate(),
                              JFapChannelConstants.SEG_XAFORGET_R,
                              requestNumber,
                              JFapChannelConstants.PRIORITY_MEDIUM,
                              true,
                              ThrottlingPolicy.BLOCK_THREAD,
                              null);
         }
         catch (SIException e)
         {
            FFDCFilter.processException(e,
                                        CLASS_NAME + ".rcvXAForget",
                                        CommsConstants.STATICCATXATRANSACTION_XAFORGET_01);

            SibTr.error(tc, "COMMUNICATION_ERROR_SICO2027", e);
         }
      }
      catch (XAException e)
      {
         FFDCFilter.processException(e,
                                     CLASS_NAME + ".rcvXAForget",
                                     CommsConstants.STATICCATXATRANSACTION_XAFORGET_02);

         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "XAException - RC: " + e.errorCode, e);

         StaticCATHelper.sendExceptionToClient(e,
                                               CommsConstants.STATICCATXATRANSACTION_XAFORGET_02,
                                               conversation, requestNumber);
      }

      request.release(allocatedFromBufferPool);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "rcvXAForget");
   }

   /**
    * Calls getTransactionTimeout() on the SIXAResource.
    *
    * Fields:
    *
    * BIT16    XAResourceId
    *
    * @param request
    * @param conversation
    * @param requestNumber
    * @param allocatedFromBufferPool
    * @param partOfExchange
    */
   public static void rcvXA_getTxTimeout(CommsByteBuffer request, Conversation conversation,
                                         int requestNumber, boolean allocatedFromBufferPool,
                                         boolean partOfExchange)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "rcvXA_getTxTimeout",
                                           new Object[]
                                           {
                                              request,
                                              conversation,
                                              ""+requestNumber,
                                              ""+allocatedFromBufferPool,
                                              ""+partOfExchange
                                            });

      try
      {
         int clientTransactionId = request.getInt();
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
         {
            SibTr.debug(tc, "XAResource Object ID", clientTransactionId);
         }

         // Get the transaction out of the table
         ServerLinkLevelState linkState = (ServerLinkLevelState) conversation.getLinkLevelAttachment();
         SITransaction tran = linkState.getTransactionTable().get(clientTransactionId, true);

         int timeout = 0;
         if ((tran != null) && (tran != IdToTransactionTable.INVALID_TRANSACTION))
         {
                 // tran may be null if the client has got an XAResource
                 // but not called start on it.

                 // Get the actual transaction ...
                 SIXAResource xaResource = (SIXAResource) tran;

            // Now call the method on the XA resource
                timeout = xaResource.getTransactionTimeout();
         }

         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Timeout: " + timeout);

         CommsByteBuffer reply = poolManager.allocate();
         reply.putInt(timeout);

         try
         {
            conversation.send(reply,
                              JFapChannelConstants.SEG_XA_GETTXTIMEOUT_R,
                              requestNumber,
                              JFapChannelConstants.PRIORITY_MEDIUM,
                              true,
                              ThrottlingPolicy.BLOCK_THREAD,
                              null);
         }
         catch (SIException e)
         {
            FFDCFilter.processException(e,
                                        CLASS_NAME + ".rcvXA_getTxTimeout",
                                        CommsConstants.STATICCATXATRANSACTION_GETTXTIMEOUT_01);

            SibTr.error(tc, "COMMUNICATION_ERROR_SICO2027", e);
         }
      }
      catch (XAException e)
      {
         FFDCFilter.processException(e,
                                     CLASS_NAME + ".rcvXA_getTxTimeout",
                                     CommsConstants.STATICCATXATRANSACTION_GETTXTIMEOUT_02);

         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "XAException - RC: " + e.errorCode, e);

         StaticCATHelper.sendExceptionToClient(e,
                                               CommsConstants.STATICCATXATRANSACTION_GETTXTIMEOUT_02,
                                               conversation, requestNumber);
      }

      request.release(allocatedFromBufferPool);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "rcvXA_getTxTimeout");
   }

   /**
    * Calls setTransactionTimeout() on the SIXAResource.
    *
    * Fields:
    *
    * BIT16    XAResourceId
    * BIT32    Timeout
    *
    * @param request
    * @param conversation
    * @param requestNumber
    * @param allocatedFromBufferPool
    * @param partOfExchange
    */
   public static void rcvXA_setTxTimeout(CommsByteBuffer request, Conversation conversation,
                                         int requestNumber, boolean allocatedFromBufferPool,
                                         boolean partOfExchange)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "rcvXA_setTxTimeout",
                                           new Object[]
                                           {
                                              request,
                                              conversation,
                                              ""+requestNumber,
                                              ""+allocatedFromBufferPool,
                                              ""+partOfExchange
                                            });

      try
      {
         int clientTransactionId = request.getInt();
         int timeout = request.getInt();

         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
         {
            SibTr.debug(tc, "XAResource Object ID", clientTransactionId);
            SibTr.debug(tc, "Timeout", timeout);
         }

         // Get the transaction out of the table
         ServerLinkLevelState linkState = (ServerLinkLevelState) conversation.getLinkLevelAttachment();
         SITransaction tran = linkState.getTransactionTable().get(clientTransactionId, true);

         boolean success = false;
         if ((tran != null) && (tran != IdToTransactionTable.INVALID_TRANSACTION))
         {
                 // tran may be null if the client is calling this method on
                 // an unenlisted XA resource.

                 // Get the actual transaction ...
                 SIXAResource xaResource = (SIXAResource) tran;

                 // Now call the method on the XA resource
                 success = xaResource.setTransactionTimeout(timeout);
         }
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Result: " + success);

         CommsByteBuffer reply = poolManager.allocate();
         if (success)
         {
            reply.put((byte) 1);
         }
         else
         {
            reply.put((byte) 0);
         }

         try
         {
            conversation.send(reply,
                              JFapChannelConstants.SEG_XA_SETTXTIMEOUT_R,
                              requestNumber,
                              JFapChannelConstants.PRIORITY_MEDIUM,
                              true,
                              ThrottlingPolicy.BLOCK_THREAD,
                              null);
         }
         catch (SIException e)
         {
            FFDCFilter.processException(e,
                                        CLASS_NAME + ".rcvXA_setTxTimeout",
                                        CommsConstants.STATICCATXATRANSACTION_SETTXTIMEOUT_01);

            SibTr.error(tc, "COMMUNICATION_ERROR_SICO2027", e);
         }
      }
      catch (XAException e)
      {
         FFDCFilter.processException(e,
                                     CLASS_NAME + ".rcvXA_setTxTimeout",
                                     CommsConstants.STATICCATXATRANSACTION_SETTXTIMEOUT_02);

         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "XAException - RC: " + e.errorCode, e);

         StaticCATHelper.sendExceptionToClient(e,
                                               CommsConstants.STATICCATXATRANSACTION_SETTXTIMEOUT_02,
                                               conversation, requestNumber);
      }

      request.release(allocatedFromBufferPool);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "rcvXA_setTxTimeout");
   }

   /**
    *
    * @param tran
    * @param convState
    * @param msgStoreResource
    * @return Returns an XAResource to use
    * @throws XAException
    */
   private static SIXAResource getResourceFromTran(SITransaction tran, ConversationState convState, boolean msgStoreResource)
      throws XAException
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
         SibTr.entry(tc, "getResourceFromTran", new Object[]{tran, convState, Boolean.valueOf(msgStoreResource)});
      SIXAResource result = null;
      if (tran != null)
      {
          result = (SIXAResource) tran;
      }
      else
      {
         try
         {
            SICoreConnection cc = ((CATConnection) convState.getObject(convState.getConnectionObjectId())).getSICoreConnection();

            if (msgStoreResource)
            {
               if (cc instanceof MSSIXAResourceProvider)
               {
                  result = ((MSSIXAResourceProvider)cc).getMSSIXAResource();
               }
               else
               {
                  SIErrorException e = new SIErrorException();
                  FFDCFilter.processException(e, CLASS_NAME + ".getResourceFromTran",
                                              CommsConstants.STATICCATXATRANSACTION_GETRESFROMTX_03);
                  if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.exception(tc, e);
                  throw e;
               }
            }
            else
            {
               result = cc.getSIXAResource();
            }
         }
         catch(SIConnectionUnavailableException e)
         {
            //No FFDC code needed
            //Only FFDC if we haven't received a meTerminated event.
            if(!convState.hasMETerminated())
            {
               FFDCFilter.processException(e, CLASS_NAME + ".getResourceFromTran",
                                           CommsConstants.STATICCATXATRANSACTION_GETRESFROMTX_01);
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.exception(tc, e);
            throw new SIErrorException(e);
         }
         catch(SIResourceException e)
         {
            FFDCFilter.processException(e, CLASS_NAME + ".getResourceFromTran",
                                        CommsConstants.STATICCATXATRANSACTION_GETRESFROMTX_02);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.exception(tc, e);
            throw new XAException(XAException.XAER_RMERR);
         }
      }
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "getResourceFromTran", result);
      return result;
   }
}
