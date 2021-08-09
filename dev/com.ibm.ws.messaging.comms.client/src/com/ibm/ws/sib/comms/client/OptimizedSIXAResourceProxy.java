/*******************************************************************************
 * Copyright (c) 2005, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.comms.client;

import java.util.HashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.comms.CommsConstants;
import com.ibm.ws.sib.comms.common.CommsByteBuffer;
import com.ibm.ws.sib.comms.common.CommsLightTrace;
import com.ibm.ws.sib.jfapchannel.Conversation;
import com.ibm.ws.sib.jfapchannel.JFapChannelConstants;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.SICoreConnection;

/**
 * An SIXAResource proxy implementation that is optimized to reduce the amount
 * of network trafic it generates.  This is achieved by not round-tripping
 * data across the network for the following XA operations:
 * <ul>
 * <li>SICoreConnection.getSIXAResource()</li>
 * <li>SIXAResource.start()</li>
 * <li>SIXAResource.end()</li>
 * </ul>
 * <p>
 * This class also supports joining with units of work that were started using a different
 * XAResource instance. The current list of inflight work is held in the xidToResourceInfo map
 * and when TMJOIN is specified, this instance simply acts as a proxy to the real instance which
 * actually started the transaction. This resource is unjoined when end() is called.
 *
 * @author Adrian Preston
 */
public class OptimizedSIXAResourceProxy extends BaseSIXAResourceProxy implements OptimizedTransaction
{
   /** Class name for FFDC's */
   private static final String CLASS_NAME = OptimizedSIXAResourceProxy.class.getName();

   /** Register Class with Trace Component */
   private static final TraceComponent tc = SibTr.register(OptimizedSIXAResourceProxy.class,
                                                           CommsConstants.MSG_GROUP,
                                                           CommsConstants.MSG_BUNDLE);

   /** Log class info on load */
   static
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "@(#) 1.36 SIB/ws/code/sib.comms.client.impl/src/com/ibm/ws/sib/comms/client/OptimizedSIXAResourceProxy.java, SIB.comms, WASX.SIB, uu1215.01");
   }

   // Has the server side UOW been created to correspond to the client side
   // UOW?  This mismatch arises because XA_START is not flowed explicitly
   // and thus starting a UOW is rolled in with the first piece of recoverable
   // work done in the scope of the UOW.
   private boolean serverUowCreated = false;

   // The XID for the current UOW being processed by this resource.  This is
   // null if the resource is not currently enlisted for a UOW.
   private Xid enlistedXid = null;

   // Reader/writer style synchronization primative used to guard critical
   // sections against closure of the SICoreConnection that created the
   // SIXAResource.
   private final ReentrantReadWriteLock closeLock;

   // Does the server side UOW associated with this resource require ending?
   // A mismatch in state between the client and server resource is possible
   // because XA_END is not explicitly flowed to the server.  Instead the
   // end is flowed during the next globally transacted transmission.
   private boolean serverUowRequiresEnding = false;

   // Flags specified to last end call.  These are recorded at the point that
   // end is invoked on this, the client proxy XAResource.  They are then flowed
   // during the next transmission (relating to this XAResource) to the server
   // which updates the corresponding server side XAResource.
   private int endFlags = 0;

   // Further to confuse the issue, there is a case where you can call end() on a resource
   // joined to this resource either before or after end() is called on this resource. To
   // manage the state and to know whether we need to actually perform the end, we use this
   // flag to track whether end has been called on this particular unit of work. This is set
   // to false during start(), and true when end() has completed. This is then checked in
   // end() also
   private boolean endHasBeenCalled = false;

   // The id of the connection that created this instance
   private int creatingConnectionId = 0;

   // The id of the conversation that created this instance
   private int creatingConversationId = 0;

   // The resource to which this resource is TMJOIN'ed (if any).
   private OptimizedSIXAResourceProxy joinedResource = null;

   // Map of XID to information about the OptimizedSIXAResource implementation
   // that has previously started the UOW for the XID.  As this is static it
   // is used to record all unresolved UOW started within the JVM.  As updates
   // to this are synchronized - this has the potential to become a bottle
   // neck.  If this is the case a more "synchronization friendly" data
   // structure will need to be used in it's place.
   private static HashMap<Xid, ResourceInfo> xidToResourceInfoMap = new HashMap<Xid, ResourceInfo>();

   // Information about any resource used to start an UOW.  This is stored in
   // the xidToResourceInfoMap.  There are two reasons for this:
   // 1) State checking.  XAResources used by a JVM need to be tracked globally
   //    to ensure that the appropriate XA state transitions are enforced.
   // 2) Managing affinity between XAResource implementation and UOW.  Although
   //    the JTA specification does not require that commit (or prepare or rollback)
   //    occure on the same XAResource that started the UOW - it does make the
   //    ordering of these operations much easier to manage if they do.  Thus, this
   //    implementation ensurs that this ordering always occures.
   private static class ResourceInfo
   {
      // The resource that started the UOW
      public OptimizedSIXAResourceProxy resource;

      // Was a server side transaction ever created for the resource. If the user of
      // the resource has only ever invoked "start, end and commit" without performing
      // any recoverable work - we can choose to optimize this by doing absolutely nothing!
      public boolean serverTransactionCreated = false;

      // Was the resource ever ended. Required for state checking.
      public boolean resourceEnded = false;

      // Has the resource been prepared? This is used to ensure that when commit is called
      // we can correctly ensure the protocol of the onePhase flag
      public boolean resourcePrepared = false;

      // Constructor. Provides a shorthand for setting the resource field.
      public ResourceInfo(OptimizedSIXAResourceProxy resource)
      {
         this.resource = resource;
      }

      // Info about this object for debug
      public String toString()
      {
         return "ResourceInfo@" + Integer.toHexString(System.identityHashCode(this)) + ": " +
                "{ resource=" + resource +
                ", serverTransactionCreated=" + serverTransactionCreated +
                ", resourceEnded=" + resourceEnded +
                ", resourcePrepared=" + resourcePrepared + " }";
      }
   }

   /**
    * Constructor.
    *
    * @param conv
    *           conversation object that represent the SICoreConnection to which this optimized
    *           SIXAResource implementation belongs.
    * @param cp
    *           proxy implementation of the SICoreConnection to which this optimized SIXAResource
    *           implementation belongs.
    * @param isMSXAResource
    *           does this XA resource _definately_ represent the Message Store XA resource?
    */
   public OptimizedSIXAResourceProxy(Conversation conv, ConnectionProxy cp, boolean isMSXAResource)
   {
      super(conv, cp, isMSXAResource);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "<init>", new Object[] { conv, cp, "" + isMSXAResource });

      closeLock = cp.closeLock;
      creatingConnectionId = getConnectionObjectID();
      creatingConversationId = conv.getId();

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "<init>");
   }

   /** @see com.ibm.ws.sib.comms.client.OptimizedTransaction#isServerTransactionCreated() */
   public boolean isServerTransactionCreated()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "isServerTransactionCreated");
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Joined Resource:", joinedResource);
      boolean result = false;
      if (joinedResource != null) result = joinedResource.serverUowCreated;
      else                        result = serverUowCreated;
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "isServerTransactionCreated", "" + result);
      return result;
   }

   /** @see com.ibm.ws.sib.comms.client.OptimizedTransaction#setServerTransactionCreated() */
   public void setServerTransactionCreated()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "setServerTransactionCreated");
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Joined Resource:", joinedResource);
      if (joinedResource != null) joinedResource.serverUowCreated = true;
      else                        serverUowCreated = true;
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "setServerTransactionCreated");
   }

   /** @see com.ibm.ws.sib.comms.client.OptimizedTransaction#getXidForCurrentUow() */
   public Xid getXidForCurrentUow()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getXidForCurrentUow");
      final Xid result = enlistedXid;
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getXidForCurrentUow", result);
      return result;
   }

   /** @see com.ibm.wsspi.sib.core.SIXAResource#isEnlisted() */
   public boolean isEnlisted()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "isEnlisted");
      final boolean result = enlistedXid != null;
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "isEnlisted", "" + result);
      return result;
   }

   /**
    * This method is used internally to set whether we are currently enlisted. This is used in the
    * case where a transaction is suspended as when this occurs the resource is effectively
    * delisted (although this is never passed onto the server).
    * <p>
    * This call indicates which XId we are now currently enlisted against or null if we are being
    * suspended (delisted).
    *
    * @see com.ibm.ws.sib.comms.client.BaseSIXAResourceProxy#setEnlisted(javax.transaction.xa.Xid)
    */
   public void setEnlisted(Xid enlistedXid)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "setEnlisted", enlistedXid);
      this.enlistedXid = enlistedXid;
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "setEnlisted");
   }

   /** @see javax.transaction.xa.XAResource#commit(javax.transaction.xa.Xid, boolean) */
   public void commit(Xid xid, boolean onePhase) throws XAException
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "commit", new Object[]{xid, ""+onePhase});

      if (TraceComponent.isAnyTracingEnabled()) {
        CommsLightTrace.traceTransaction(tc, "CommitTxnTrace", xid, getTransactionId(), -1);
      }

      // Find the XAResource implementation that was used to start this
      // transaction.
      boolean performingRecovery = false;
      boolean thisResourceStartedUnitOfWork = false;
      boolean serverTransactionWasCreated = true;
      OptimizedSIXAResourceProxy delegateResource = null;
      ResourceInfo info = null;

      synchronized(xidToResourceInfoMap)
      {
         info = xidToResourceInfoMap.get(xid);
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Resource Info: ", info);

         // If there is no resource in the map, then start() was never invoked
         // for this XID.  We must be performing recovery.
         if (info == null)
         {
            performingRecovery = true;
         }
         else
         {
            // This XID was started by a resource in this JVM - but was it
            // this resource?
            if (info.resource == this)
            {
               // If the resource has not been ended - then the XID is
               // invalid as an argument to this method.
               if (!info.resourceEnded)
               {
                  final XAException exception = new XAException(XAException.XAER_INVAL);
                  if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.exception(this, tc, exception);
                  throw exception;
               }

               // If we have been told to commit 1PC and the resource has already
               // been prepared, this is a protocol error
               if (onePhase && info.resourcePrepared)
               {
                  final XAException exception = new XAException(XAException.XAER_PROTO);
                  if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.exception(this, tc, exception);
                  throw exception;
               }

               thisResourceStartedUnitOfWork = true;
               serverTransactionWasCreated = info.serverTransactionCreated;

               // This remainder of this method will resolve the unit of
               // work - so remove it from the map.
               xidToResourceInfoMap.remove(xid);
            }
            else
            {
               delegateResource = info.resource;
            }
         }
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
      {
         String debugText =
            "thisResourceStartedUnitOfWork="+thisResourceStartedUnitOfWork+
            "\nperformingRecoveryOr2PCCommit="+performingRecovery+
            "\nserverTransactionWasCreated="+serverTransactionWasCreated;
         SibTr.debug(this, tc, debugText);
      }

      // If this resource started the transaction (or we are performing
      // recovery) then attempt to resolve the unit of work.
      if (thisResourceStartedUnitOfWork || performingRecovery)
      {
         // Only attempt to contact the server if the transaction was
         // actually created.  If no work was done inside the scope
         // of the transaction - this is a no-op.
         if (serverTransactionWasCreated)
         {
            // Take a "reader" lock on the connections reader/writer
            // lock.  This will prevent the connection that "owns" this
            // resource from closing whilst the unit of work is resolved.
            takeCloseLock();
            try
            {
               // If the connection that "owns" this resource is closed, and we are not prepared (1PC),
               // then we can assume that the unit of work must have been resolved (rolled back) 
               // by the server-side code as part of it's clean-up.  If we are prepared, then we
               // must ask to be retried later with RMFAIL.
               // if info == null we are in recovery and so must also retry
               if (isClosed())
               {
                  final XAException exception = (info == null || info.resourcePrepared) ? new XAException(XAException.XAER_RMFAIL) : 
                                                                                          new XAException(XAException.XA_RBROLLBACK);
                  if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.exception(this, tc, exception);
                  throw exception;
               }

               CommsByteBuffer request = getCommsByteBuffer();
               request.putInt(getTransactionId());
               request.putXid(xid);
               request.put(onePhase ? (byte)1 : (byte)0);

               if (isEndRequired())
               {
                  request.put((byte)0x01);
                  request.putInt(getEndFlags());
                  setEndNotRequired();
               }
               else
               {
                  request.put((byte)0x00);
                  request.putInt(0);
               }
               if (fapLevel >= JFapChannelConstants.FAP_VERSION_5)
               {
                  request.put((byte)(isMSResource ? 0x01 : 0x00));
               }

               final CommsByteBuffer reply = jfapExchange(request,
                                                          JFapChannelConstants.SEG_XACOMMIT,
                                                          JFapChannelConstants.PRIORITY_LOWEST,
                                                          true);
               try
               {
                  reply.checkXACommandCompletionStatus(JFapChannelConstants.SEG_XACOMMIT_R, getConversation());
               }
               finally
               {
                  if (reply != null) reply.release();
               }
            }
            catch (XAException xa)
            {
               // No FFDC Code needed
               // Simply re-throw...
               if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.exception(this, tc, xa);
               throw xa;
            }
            catch (Exception e)
            {
                FFDCFilter.processException(e, CLASS_NAME + ".commit",
                                           CommsConstants.OPTRESOURCEPROXY_COMMIT_01, this);

                // Any non-XA exception is mapped to an XA RM error.
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Non-XA exception caught: "+e);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.exception(this, tc, e);
                final XAException xaException = new XAException(XAException.XAER_RMFAIL);
                xaException.initCause(e);
                throw xaException;
            }
            finally
            {
               // Stop "reading" the owning connection's read/writer close
               // lock.  This allows the connection to be closed.
               releaseCloseLock();
            }
         }
         else
         {
            // The transaction was never created on the server.  The caller must
            // not have carried out any work for this resource inside the scope
            // of the unit of work.
            if ((endFlags & TMFAIL) == TMFAIL)
            {
               // If the unit of work was ended with the failure flag set
               // then we should fail the commit.
               if (onePhase)
               {
                  // If the transaction is one phase  - then signal that the
                  // resource manager has already rolled back the unit of work.
                  final XAException exception = new XAException(XAException.XA_HEURRB);
                  if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.exception(this, tc, exception);
                  throw exception;
               }

               // It should not be possible to enter into this arm of the
               // if statement as it implies that the transaction manager
               // has attempted to perform a 2PC commit without going
               // through the prepare stage.  Throw a protocol exception
               final XAException exception = new XAException(XAException.XAER_PROTO);
               if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.exception(this, tc, exception);
               throw exception;
            }
         }
      }
      else
      {
         // The unit of work was started by another XA resource.
         // Delegate the resolution of the unit of work to the resource
         // that started it.
         delegateResource.commit(xid, onePhase);
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "commit");
   }

   /** @see javax.transaction.xa.XAResource#end(javax.transaction.xa.Xid, int) */
   public void end(Xid xid, int flags) throws XAException
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "end", new Object[] { xid, "" + flags });

      // If the XID is null then it is clearly not valid.
      if (xid == null)
      {
         final XAException exception = new XAException(XAException.XAER_NOTA);
         if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.exception(this, tc, exception);
         throw exception;
      }

      // If there is currently no enlisted XID - then the XID argument is invalid
      if (enlistedXid == null)
      {
         final XAException exception = new XAException(XAException.XAER_INVAL);
         if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.exception(this, tc, exception);
         throw exception;
      }

      // If the XID is not the same as the enlisted XID - it is not valid.
      if (!enlistedXid.equals(xid))
      {
         final XAException exception = new XAException(XAException.XAER_NOTA);
         if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.exception(this, tc, exception);
         throw exception;
      }

      // Below is some logic to unjoin from a resource. The comms implementation of TMJOIN places
      // the first XAResource that starts a transaction as the 'primary' XAResource. Therefore if
      // any XAResources join up with it then they effectively become 'co-opted' XAResources to the
      // first. As the XAResources can be ended in any order, we must ensure that we if the primary
      // is ended first then ending the last co-opted resource causes the end to actually occur.

      boolean performEndNow;
      // If we are currently joined to a resource, unjoin it here. We also need to work out wh
      if (joinedResource != null)
      {
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Unjoining from: " + joinedResource);
         joinedResource.unjoin(this);

         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "JoinedResource.endHasBeenCalled: " +
                                                        joinedResource.endHasBeenCalled);
         // The answer we care about here is whether the primary XA Resource has any further joined
         // resources. We also should only perform the end now if the primary XA Resource has also
         // been ended.
         performEndNow = (!joinedResource.hasJoinedResources()) &&
                         joinedResource.endHasBeenCalled;
      }
      else
      {
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Not joined to any other resource");
         performEndNow = (!hasJoinedResources());
      }

      // Check and see if any resources are joined to us. If there are no further resources joined
      // then we can perform the end processing. Otherwise, we must wait for the final end() call.
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "performEndNow:" + performEndNow);
      if (performEndNow)
      {
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Performing standard end");

         synchronized (xidToResourceInfoMap)
         {
            ResourceInfo info = xidToResourceInfoMap.get(xid);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Resource Info: ", info);

            if ((info == null) || ((joinedResource == null) && (info.resource != this)) || ((joinedResource != null) && (info.resource != joinedResource)))
            {
               // XID was either never started or started using a different
               // resource. This is a protocol exception.
               XAException exception = new XAException(XAException.XAER_PROTO);
               if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.exception(this, tc, exception);
               throw exception;
            }

            // Record whether a transaction was ever created on the server
            // If it was _not_ then this information makes completing the
            // transaction a no-op. Again, if this is a co-opted resource,
            // defer the real answer to the primary XAResource.
            if (joinedResource != null) info.serverTransactionCreated = joinedResource.serverUowCreated;
            else                        info.serverTransactionCreated = serverUowCreated;
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "serverTransactionCreated", ""+info.serverTransactionCreated);

            // Record that the transaction was ended. This is required for
            // state checking at the point resolution is attempted.
            info.resourceEnded = true;

            if (info.serverTransactionCreated)
            {
               // If a transaction was created on the server then end will
               // need invoking on the server before the resource can be
               // used again. Set internal state to remember this. Ensure
               // we update the flags in the correct instance if this is a
               // co-opted XAResource.
               if (joinedResource != null)
               {
                  joinedResource.serverUowRequiresEnding = true;
                  joinedResource.endFlags = flags;
               }
               else
               {
                  serverUowRequiresEnding = true;
                  endFlags = flags;
               }
            }
         }

         // Reset the flags in the joined instance if we have one
         if (joinedResource != null)
         {
            joinedResource.serverUowCreated = false;
            joinedResource.enlistedXid = null;
         }

         // And in this one
         serverUowCreated = false;
      }

      // These variables must always be reset
      joinedResource = null;
      endHasBeenCalled = true;
      enlistedXid = null;

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "end");
   }

   /** @see javax.transaction.xa.XAResource#forget(javax.transaction.xa.Xid) */
   public void forget(Xid xid) throws XAException
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "forget", xid);
      internalForget(xid);
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "forget");
   }

   /** @see javax.transaction.xa.XAResource#getTransactionTimeout() */
   public int getTransactionTimeout() throws XAException
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getTransactionTimeout");

      // Message store implementation does not support setting of
      // timeouts. Thus, neither shall we.
      final int result = 0;

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getTransactionTimeout", "" + result);
      return result;
   }

   /** @see javax.transaction.xa.XAResource#prepare(javax.transaction.xa.Xid) */
   public int prepare(Xid xid) throws XAException
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "prepare", xid);

      if (TraceComponent.isAnyTracingEnabled()) {
        CommsLightTrace.traceTransaction(tc, "PrepareTxnTrace", xid, getTransactionId(), -1);
      }

      final int result;

      // Find the XAResource implementation that was used to start this
      // transaction.
      boolean performingRecovery = false;
      boolean thisResourceStartedUnitOfWork = false;
      boolean serverTransactionWasCreated = true;
      OptimizedSIXAResourceProxy delegateResource = null;

      ResourceInfo info = null; // PK61176
      synchronized(xidToResourceInfoMap)
      {
         info = xidToResourceInfoMap.get(xid);
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Resource Info: ", info);

         // If there is no resource in the map, then start() was never invoked
         // for this XID.  We must be performing recovery.
         if (info == null)
         {
            performingRecovery = true;
         }
         else
         {
            // This XID was started by a resource in this JVM - but was it
            // this resource?
            if (info.resource == this)
            {
               // If the resource has not been ended - then the XID is
               // invalid as an argument to this method.
               if (!info.resourceEnded)
               {
                  final XAException exception = new XAException(XAException.XAER_INVAL);
                  if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.exception(this, tc, exception);
                  throw exception;
               }

               thisResourceStartedUnitOfWork = true;
               serverTransactionWasCreated = info.serverTransactionCreated;

               // Mark the resource as prepared
               info.resourcePrepared = true;
            }
            else
            {
               delegateResource = info.resource;
            }
         }
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
      {
         String debugText =
            "thisResourceStartedUnitOfWork="+thisResourceStartedUnitOfWork+
            "\nperformingRecovery="+performingRecovery+
            "\nserverTransactionWasCreated="+serverTransactionWasCreated;
         SibTr.debug(this, tc, debugText);
      }

      // If this resource started the transaction (or we are performing
      // recovery) then attempt to resolve the unit of work.
      if (thisResourceStartedUnitOfWork || performingRecovery)
      {
         // Only attempt to contact the server if the transaction was
         // actually created.  If no work was done inside the scope
         // of the transaction - this is a no-op.
         if (serverTransactionWasCreated)
         {
            // Take a "reader" lock on the connections reader/writer
            // lock.  This will prevent the connection that "owns" this
            // resource from closing whilst the unit of work is resolved.
            takeCloseLock();
            try
            {
               // If the connection that "owns" this resource is closed
               // then we can assume that the unit of work must have been
               // resolved (rolled back) by the server-side code as part
               // of it's clean-up
               if (isClosed())
               {
                  final XAException exception = new XAException(XAException.XA_RBROLLBACK);
                  if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.exception(this, tc, exception);
                  throw exception;
               }

               CommsByteBuffer request = getCommsByteBuffer();
               request.putInt(getTransactionId());
               request.putXid(xid);

               if (isEndRequired())
               {
                  request.put((byte)0x01);
                  request.putInt(getEndFlags());
                  setEndNotRequired();
               }
               else
               {
                  request.put((byte)0x00);
                  request.putInt(0);
               }
               if (fapLevel >= JFapChannelConstants.FAP_VERSION_5)
               {
                  request.put((byte)(isMSResource ? 0x01 : 0x00));
               }

               final CommsByteBuffer reply = jfapExchange(request,
                                                          JFapChannelConstants.SEG_XAPREPARE,
                                                          JFapChannelConstants.PRIORITY_LOWEST,
                                                          true);
               try
               {
                  reply.checkXACommandCompletionStatus(JFapChannelConstants.SEG_XAPREPARE_R, getConversation());

                  // Get the return code and return that
                  result = reply.getInt();
               }
               finally
               {
                  if (reply != null) reply.release();
               }
            }
            catch (XAException xa)
            {
               // No FFDC Code needed

               // PK61176 If the XAException is a XA_RBXXXX then this will be the last time we will get called.
               if (xa.errorCode >= XAException.XA_RBBASE && xa.errorCode <= XAException.XA_RBEND)
               {
                 // Remove the failed xid (which will have been rolled back by the messaging engine) from xidToResourceInfoMap
                 // PK61176 - XA spec says this is correct for any return code (XA_RB or XA_RMERR).
                 synchronized(xidToResourceInfoMap) {
                   if (xidToResourceInfoMap.containsKey(xid)) {
                     xidToResourceInfoMap.remove(xid);
                   }
                 }
               }

               if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.exception(this, tc, xa);
               throw xa;
            }
            catch (Exception e)
            {
               FFDCFilter.processException(e, CLASS_NAME + ".prepare",
                                           CommsConstants.SIXARESOURCEPROXY_COMMIT_01, this);

               // Any non-XA exception is mapped to an XA RM error.
               if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Non-XA exception caught: "+e);
               if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.exception(this, tc, e);
               final XAException xaException = new XAException(XAException.XAER_RMFAIL);
               xaException.initCause(e);
               
               /** PM11871 - we need to tear down the Connection to ensure the ME cleans up the tran
                *  if it is still associated (ie not ended).  This should also cause the Listener
                *  to restart and so recreate the SIXAResourceProxy; thus we don't have to fake the
                *  state transition to UNENLISTED to avoid problems when it is reused. 
                */
               
               invalidateConnection(true, e, "Exception in SIXAResourceProxy.end");
               
               if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "prepare");
               throw xaException;
            }
            finally
            {
               // Stop "reading" the owning connection's read/writer close
               // lock.  This allows the connection to be closed.
               releaseCloseLock();
            }
         }
         else
         {
            // The transaction was never created on the server.  The caller must
            // not have carried out any work for this resource inside the scope
            // of the unit of work.  Decide how to vote based on the flags used
            // to end the unit of work.
            if ((endFlags & TMFAIL) == TMFAIL)
            {
               final XAException exception = new XAException(XAException.XA_RBROLLBACK);
               if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.exception(this, tc, exception);
               throw exception;
            }

            result = XA_OK;
         }
      }
      else
      {
         // The unit of work was started by another XA resource.
         // Delegate the resolution of the unit of work to the resource
         // that started it.
         result = delegateResource.prepare(xid);
      }
      
      /* PM40261 - Start */
      // In case of a Readonly  is the result of prepare the transaction manager would drop the branch 
      // of the transaction as commit wouldn't make any sense for that particular branch. So we should
      // remove the XID of the transaction from the map else we would simply accumulate in map and never 
      // remove from it as this transaction would never get into commit or rollback.
      if (XA_RDONLY == result) {
    	  synchronized (xidToResourceInfoMap) {
    		  if (xidToResourceInfoMap.containsKey(xid)) {
    			  xidToResourceInfoMap.remove(xid);
    		  }
    	  }
      }    	  	
      /* PM40261 - End */
      	 
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "prepare", ""+result);
      return result;
   }

        /** @see javax.transaction.xa.XAResource#recover(int) */
   public Xid[] recover(int flags) throws XAException
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "recover", "" + flags);
      final Xid[] result = internalRecover(flags);
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "recover", result);
      return result;
   }

   /** @see javax.transaction.xa.XAResource#rollback(javax.transaction.xa.Xid)*/
   public void rollback(Xid xid) throws XAException
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "rollback", xid);

      if (TraceComponent.isAnyTracingEnabled()) {
        CommsLightTrace.traceTransaction(tc, "RollbackTxnTrace", xid, getTransactionId(), -1);
      }

      if (xid == null)
      {
         final XAException exception = new XAException(XAException.XAER_NOTA);
         if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.exception(this, tc, exception);
         throw exception;
      }

      // Find the XAResource implementation that was used to start this
      // transaction.
      boolean performingRecovery = false;
      boolean thisResourceStartedUnitOfWork = false;
      boolean serverTransactionWasCreated = true;
      OptimizedSIXAResourceProxy delegateResource = null;
      ResourceInfo info = null;

      synchronized(xidToResourceInfoMap)
      {
         info = xidToResourceInfoMap.get(xid);
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Resource Info: ", info);

         // If there is no resource in the map, then start() was never invoked
         // for this XID.  We must be performing recovery.
         if (info == null)
         {
            performingRecovery = true;
         }
         else
         {
            // This XID was started by a resource in this JVM - but was it
            // this resource?
            if (info.resource == this)
            {
               // If the resource has not been ended - then the XID is
               // invalid as an argument to this method.
               if (!info.resourceEnded)
               {
                  final XAException exception = new XAException(XAException.XAER_INVAL);
                  if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.exception(this, tc, exception);
                  throw exception;
               }

               thisResourceStartedUnitOfWork = true;
               serverTransactionWasCreated = info.serverTransactionCreated;

               // This remainder of this method will resolve the unit of
               // work - so remove it from the map.
               xidToResourceInfoMap.remove(xid);
            }
            else
            {
               delegateResource = info.resource;
            }
         }
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
      {
         String debugText =
            "thisResourceStartedUnitOfWork="+thisResourceStartedUnitOfWork+
            "\nperformingRecovery="+performingRecovery+
            "\nserverTransactionWasCreated="+serverTransactionWasCreated;
         SibTr.debug(this, tc, debugText);
      }

      // If this resource started the transaction (or we are performing
      // recovery) then attempt to resolve the unit of work.
      if (thisResourceStartedUnitOfWork || performingRecovery)
      {
         // Only attempt to contact the server if the transaction was
         // actually created.  If no work was done inside the scope
         // of the transaction - this is a no-op.
         if (serverTransactionWasCreated)
         {
            // Take a "reader" lock on the connections reader/writer
            // lock.  This will prevent the connection that "owns" this
            // resource from closing whilst the unit of work is resolved.
            takeCloseLock();
            try
            {
               // If the connection that "owns" this resource is closed, and we are not prepared (1PC),
               // then we can assume that the unit of work must have been resolved (rolled back) 
               // by the server-side code as part of it's clean-up.  If we are prepared, then we
               // must ask to be retried later with RMFAIL.
               // if info == null we are in recovery and so must also retry
               if (isClosed())
               {
                  XAException exception = (info == null || info.resourcePrepared) ? new XAException(XAException.XAER_RMFAIL) : 
                                                                                    new XAException(XAException.XA_RBROLLBACK);
                  if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.exception(this, tc, exception);
                  throw exception;
               }

               CommsByteBuffer request = getCommsByteBuffer();
               request.putInt(getTransactionId());
               request.putXid(xid);
               if (isEndRequired())
               {
                  request.put((byte)0x01);
                  request.putInt(getEndFlags());
                  setEndNotRequired();
               }
               else
               {
                  request.put((byte)0x00);
                  request.putInt(0);
               }
               if (fapLevel >= JFapChannelConstants.FAP_VERSION_5)
               {
                  request.put((byte)(isMSResource ? 0x01 : 0x00));
               }

               final CommsByteBuffer reply = jfapExchange(request,
                                                          JFapChannelConstants.SEG_XAROLLBACK,
                                                          JFapChannelConstants.PRIORITY_LOWEST,
                                                          true);
               try
               {
                  reply.checkXACommandCompletionStatus(JFapChannelConstants.SEG_XAROLLBACK_R, getConversation());
               }
               finally
               {
                  if (reply != null) reply.release();
               }
            }
            catch (XAException xa)
            {
               // No FFDC Code needed
               // PK61176 XID already removed from our table
               // Simply re-throw...
               if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.exception(this, tc, xa);
               throw xa;
            }
            catch (Exception e)
            {
                FFDCFilter.processException(e, CLASS_NAME + ".rollback",
                                           CommsConstants.SIXARESOURCEPROXY_COMMIT_01, this);

               // Any non-XA exception is mapped to an XA RM error.
               if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Non-XA exception caught: "+e);
               if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.exception(this, tc, e);
               final XAException xaException = new XAException(XAException.XAER_RMFAIL);
               xaException.initCause(e);
               throw xaException;
            }
            finally
            {
               // Stop "reading" the owning connection's read/writer close
               // lock.  This allows the connection to be closed.
               releaseCloseLock();
            }
         }
         else
         {
            // The transaction was never created on the server.  The caller must
            // not have carried out any work for this resource inside the scope
            // of the unit of work.

            // Do nothing.
         }
      }
      else
      {
         // The unit of work was started by another XA resource.
         // Delegate the resolution of the unit of work to the resource
         // that started it.
         delegateResource.rollback(xid);
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "rollback");
   }

   /** @see javax.transaction.xa.XAResource#setTransactionTimeout(int) */
   public boolean setTransactionTimeout(int timeout) throws XAException
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "setTransactionTimeout", "" + timeout);

      // Message store implementation does not support setting of
      // timeouts. Thus, neither shall we.
      final boolean result = false;

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "setTransactionTimeout", "" + result);
      return result;
   }

   /** @see javax.transaction.xa.XAResource#start(javax.transaction.xa.Xid, int) */
   public void start(Xid xid, int flags) throws XAException
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "start",new Object[]{xid,flags});

      // A null XID is not valid...
      if (xid == null)
      {
         final XAException exception = new XAException(XAException.XAER_NOTA);
         if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.exception(this, tc, exception);
         throw exception;
      }

      // Throw a "routine was invoked in an improper context" exception if
      // the resource is already enlisted into a unit of work.
      if (enlistedXid != null)
      {
         final XAException exception = new XAException(XAException.XAER_PROTO);
         if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.exception(this, tc, exception);
         throw exception;
      }

      // This resource supports neither joining an existing unit of work
      // or suspend/resume.
      if ((flags & TMJOIN) == TMJOIN)
      {
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "TMJOIN was specified");

         if (joinedResource != null)
         {
            final XAException exception = new XAException(XAException.XAER_PROTO);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.exception(this, tc, exception);
            throw exception;
         }

         synchronized(xidToResourceInfoMap)
         {
            final ResourceInfo info = xidToResourceInfoMap.get(xid);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Resource Info: ", info);

            if (info == null)
            {
               final XAException exception = new XAException(XAException.XAER_INVAL);
               if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.exception(this, tc, exception);
               throw exception;
            }

            joinedResource = info.resource;
            enlistedXid = xid;
            joinedResource.join(this);

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Joined to resource: ", joinedResource);
         }
      }
      else
      {
         ResourceInfo info = new ResourceInfo(this);
         synchronized(xidToResourceInfoMap)
         {
            // Check if the XID has already been used to start a unit of work
            // with a resource from this resource manager.  If so, this is a
            // duplicate XID exception.
            if (xidToResourceInfoMap.containsKey(xid))
            {
               final XAException exception = new XAException(XAException.XAER_DUPID);
               if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.exception(this, tc, exception);
               throw exception;
            }

            xidToResourceInfoMap.put(xid, info);
         }

         enlistedXid = xid;
      }

      endHasBeenCalled = false;

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "start");
   }

   /** @see XAResource#isSameRM(javax.transaction.xa.XAResource) */
   public boolean isSameRM(XAResource xaRes) throws XAException
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "isSameRM", xaRes);

      boolean result = false;

      // If the passed in XAResource is not a client proxy version
      // don't even try - there is no way for us to know here
      if (xaRes instanceof OptimizedSIXAResourceProxy)
      {
         // We have no idea what the state of the other connection is in here,
         // so catch anything it might throw - just to be on the safe side.
         try
         {
            SICoreConnection thisConnection = ((ClientConversationState) getConversation().getAttachment()).getSICoreConnection();
            SICoreConnection otherConnection = ((ClientConversationState) ((OptimizedSIXAResourceProxy) xaRes).getConversation().getAttachment()).getSICoreConnection();

            result = thisConnection.isEquivalentTo(otherConnection);
         }
         catch (Throwable t)
         {
            // No FFDC code needed
            // They must have passed in an invalid type of connection, or one that
            // was null, or closed - so we would assume they are not equal. For helpfulness
            // though, print out the exception
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Caught an exception comparing the connections", t);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.exception(this, tc, t);
         }
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "isSameRM", ""+result);
      return result;
   }

   /**
    * Take a "reader" lock on the close synchronization primative.
    */
   private void takeCloseLock()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "takeCloseLock");

      while (true)
      {
         try
         {
            closeLock.readLock().lockInterruptibly();
            break;
         }
         catch (InterruptedException e)
         {
            // No FFDC code needed.
         }
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "takeCloseLock");
   }

   /**
    * Release a "reader" lock on the close synchronization primative.
    */
   private void releaseCloseLock()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "releaseCloseLock");
      closeLock.readLock().unlock();
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "releaseCloseLock");
   }

   /** @see com.ibm.ws.sib.comms.client.OptimizedTransaction#isEndRequired() */
   public boolean isEndRequired()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "isEndRequired");
      final boolean result = serverUowRequiresEnding;
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "isEndRequired", "" + result);
      return result;
   }

   /** @see com.ibm.ws.sib.comms.client.OptimizedTransaction#setEndNotRequired() */
   public void setEndNotRequired()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "setEndNotRequired");
      if (!serverUowRequiresEnding)
      {
         final SIErrorException exception = new SIErrorException();
         FFDCFilter.processException(exception, CLASS_NAME + ".setEndNotRequired",
                                     CommsConstants.OPTRESOURCEPROXY_SETENDNOTREQUIRED_01, this);
         if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.exception(this, tc, exception);
         throw exception;
      }
      serverUowRequiresEnding = false;
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "setEndNotRequired");
   }

   /** @see com.ibm.ws.sib.comms.client.OptimizedTransaction#getEndFlags() */
   public int getEndFlags()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getEndFlags");
      final int result;
      if (!serverUowRequiresEnding)
      {
         final SIErrorException exception = new SIErrorException();
         FFDCFilter.processException(exception, CLASS_NAME + ".getEndFlags",
                                     CommsConstants.OPTRESOURCEPROXY_GETENDFLAGS_01, this);
         if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.exception(this, tc, exception);
         throw exception;
      }
      result = endFlags;
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getEndFlags", "" + result);
      return result;
   }

   /** @see com.ibm.ws.sib.comms.client.OptimizedTransaction#areSubordinatesAllowed() */
   public boolean areSubordinatesAllowed()
   {
      // Not supported for XAResources.
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "areSubordinatesAllowed");
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "areSubordinatesAllowed", "SIErrorException");
      throw new SIErrorException();
   }

   /** @see com.ibm.ws.sib.comms.client.OptimizedTransaction#getCreatingConnectionId() */
   public int getCreatingConnectionId()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getCreatingConnectionId");
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getCreatingConnectionId", creatingConnectionId);
      return creatingConnectionId;
   }

   /**
    * @return Returns the creating conversation id.
    */
   public int getCreatingConversationId()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getCreatingConversationId");
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getCreatingConversationId", creatingConversationId);
      return creatingConversationId;
   }


   // *******************************************************************************************
   // *                   Override of methods specified in Transaction                          *
   // *******************************************************************************************


   /**
    * @return Returns the lowest message priority of either the joined resource (if we are joined
    *         to one), or us if we are not.
    *
    * @see com.ibm.ws.sib.comms.client.Transaction#getLowestMessagePriority()
    */
   public short getLowestMessagePriority()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getLowestMessagePriority");

      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Joined resource:", joinedResource);

      final short result;
      if (joinedResource != null)
      {
         result = joinedResource.getLowestMessagePriority();
      }
      else
      {
         result = super.getLowestMessagePriority();
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getLowestMessagePriority", result);
      return result;
   }

   /**
    * @return Returns the transaction Id of the either joined resource (if we are joined to one), or
    *         us if we are not.
    *
    * @see com.ibm.ws.sib.comms.client.Transaction#getTransactionId()
    */
   public int getTransactionId()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getTransactionId");

      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Joined resource:", joinedResource);

      final int result;
      if (joinedResource != null)
      {
         result = joinedResource.getTransactionId();
      }
      else
      {
         result = super.getTransactionId();
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getTransactionId", result);
      return result;
   }

   /**
    * Updates the lowest message priority of either the joined resource (if we are joined to one),
    * or us if we are not.
    *
    * @param messagePriority
    *
    * @see com.ibm.ws.sib.comms.client.Transaction#updateLowestMessagePriority(short)
    */
   public void updateLowestMessagePriority(short messagePriority)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "updateLowestMessagePriority", messagePriority);

      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Joined resource:", joinedResource);

      if (joinedResource != null)
      {
         joinedResource.updateLowestMessagePriority(messagePriority);
      }
      else
      {
         super.updateLowestMessagePriority(messagePriority);
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "updateLowestMessagePriority");
   }

   /**
    * Ensure toString includes comms tranId + enlisted XID
    */
   public String toString() {
     return super.toString() + "[commsTx=" + getTransactionId() + ",enlistedXid=" + enlistedXid + "]";
   }
}
