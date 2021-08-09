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
package com.ibm.ws.sib.comms.client;

import java.util.ArrayList;

import javax.transaction.xa.XAException;
import javax.transaction.xa.Xid;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.comms.CommsConstants;
import com.ibm.ws.sib.comms.common.CommsByteBuffer;
import com.ibm.ws.sib.jfapchannel.Conversation;
import com.ibm.ws.sib.jfapchannel.JFapChannelConstants;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.SIXAResource;

/**
 * Abstract base class that other SIXAResource implementations extend.
 * Collects together the functionality common to the SIXAResourceProxy 
 * and OptimizedSIXAResourceProxy classes.
 */
public abstract class BaseSIXAResourceProxy extends Transaction implements SIXAResource
{
   /** Class name for FFDC's */
   private static String CLASS_NAME = BaseSIXAResourceProxy.class.getName();
   
   /** Register Class with Trace Component */
   private static final TraceComponent tc = SibTr.register(BaseSIXAResourceProxy.class,
                                                           CommsConstants.MSG_GROUP, 
                                                           CommsConstants.MSG_BUNDLE);
   
   // Level of FAP being used for the communications link that this
   // transaction is associated with.
   protected final int fapLevel;
    
   // Does this proxy _definately_ represent a message store XA
   // resource?
   protected final boolean isMSResource;
   
   // Flag indicating whether this resource is currently suspended or not
   protected boolean isSuspended = false;
   
   // A list of XAResource objects that have currently joined to this resource.
   // An xa_end() will blow up if any resources are currently joined to it as they
   // should be ended first.
   private ArrayList<SIXAResource> resourcesJoinedToThisResource = new ArrayList<SIXAResource>();
   
   /** Log source info on class load */
   static
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "@(#) 1.22 SIB/ws/code/sib.comms.client.impl/src/com/ibm/ws/sib/comms/client/BaseSIXAResourceProxy.java, SIB.comms, WASX.SIB, uu1215.01");
   }

   /**
    * Constructor.
    * 
    * @param conv
    * @param cp
    * @param isMSResource
    */
   public BaseSIXAResourceProxy(Conversation conv, 
         					    ConnectionProxy cp, 
         						boolean isMSResource)
   {
      super(conv, cp);      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "<init>");
      
      fapLevel = conv.getHandshakeProperties().getFapLevel();
      this.isMSResource = isMSResource;

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "<init>");
   }
   
   /**
    * Called when another XAResource is joining to us.
    * @param resource
    */
   protected synchronized void join(SIXAResource resource)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "join", resource);
      resourcesJoinedToThisResource.add(resource);
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "join");
   }
   
   /**
    * Called when another instance is un-joining from us.
    * @param resource
    */
   protected synchronized void unjoin(SIXAResource resource)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "unjoin", resource);
      resourcesJoinedToThisResource.remove(resource);
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "unjoin");
   }
   
   /**
    * @return Returns true if there are resources already joined to this XAResource.
    */
   protected synchronized boolean hasJoinedResources()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "hasJoinedResources");
      boolean result = (resourcesJoinedToThisResource.size() != 0);
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "hasJoinedResources", Boolean.valueOf(result));
      return result;
   }
   
   /**
    * Called when the transaction manager wishes to commit the 
    * transaction represented by this Xid.
    * <p>
    * Across the wire we will flow:
    * <p>
    * BIT32    TransactionId
    * The XID Structure
    * BYTE     OnePhase (0x00 = false, 0x01 = true)
    * 
    * @param xid
    * @param onePhase
    * 
    * @throws XAException if an exception is thrown at the ME. In the
    *         event of a comms failure, an XAException with XAER_RMFAIL will
    *         be thrown. 
    */
   protected void internalCommit(Xid xid, boolean onePhase) throws XAException
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "internalCommit",
                                           new Object[] {xid, ""+onePhase});
      
      try
      {
         
         CommsByteBuffer request = getCommsByteBuffer();
         request.putInt(getTransactionId());
         request.putXid(xid);
         request.put((byte)(onePhase ? 1 : 0));
         if (fapLevel >= JFapChannelConstants.FAP_VERSION_5) { 
            request.put((byte)(isMSResource ? 0x01 : 0x00));
         }
         
         CommsByteBuffer reply = jfapExchange(request, 
                                              JFapChannelConstants.SEG_XACOMMIT, 
                                              getLowestMessagePriority(), 
                                              true);
         try
         {
            reply.checkXACommandCompletionStatus(JFapChannelConstants.SEG_XACOMMIT_R, getConversation());
         }
         finally
         {
            reply.release();
         }
      }
      catch (XAException xa)
      {
         // No FFDC Code needed
         // Simply re-throw...
         throw xa;
      }
      catch (Exception e)
      {
         FFDCFilter.processException(e, CLASS_NAME + ".internalCommit",
                                     CommsConstants.SIXARESOURCEPROXY_COMMIT_01, this);
         
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Caught a comms problem:", e);
         throw new XAException(XAException.XAER_RMFAIL);
      }
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "internalCommit");
   }

   /**
    * Called when the application wishes to mark the end of the 
    * unit of work in the transaction represented by this Xid.
    * <p>
    * Across the wire we will flow:
    * <p>
    * BIT32    TransactionId
    * The XID Structure
    * BIT32    Flags
    * 
    * @param xid
    * @param flags
    * 
    * @throws XAException if an exception is thrown at the ME. In the
    *         event of a comms failure, an XAException with XAER_RMFAIL will
    *         be thrown. 
    */
   protected void internalEnd(Xid xid, int flags) throws XAException
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "internalEnd",
                                           new Object[] {xid, ""+flags});
      try
      {
         CommsByteBuffer request = getCommsByteBuffer();
         request.putInt(getTransactionId());
         request.putXid(xid);
         request.putInt(flags);
         if (fapLevel >= JFapChannelConstants.FAP_VERSION_5) {
            request.put((byte)(isMSResource ? 0x01 : 0x00));
         }         
         
         CommsByteBuffer reply = jfapExchange(request, 
                                              JFapChannelConstants.SEG_XAEND, 
                                              getLowestMessagePriority(),
                                              true);
         try
         {
            reply.checkXACommandCompletionStatus(JFapChannelConstants.SEG_XAEND_R, getConversation());            
         }
         finally
         {
            reply.release();
         }
      }
      catch (XAException xa)
      {
         // No FFDC Code needed
         // Simply re-throw...
         throw xa;
      }
      catch (Exception e)
      {
         FFDCFilter.processException(e, CLASS_NAME + ".internalEnd",
                                     CommsConstants.SIXARESOURCEPROXY_END_01, this);
         
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Caught a comms problem:", e);

         /** PM11871 - we need to tear down the Connection to ensure the ME cleans up the tran
          *  if it is still associated (ie not ended).  This should also cause the Listener
          *  to restart and so recreate the SIXAResourceProxy; thus we don't have to fake the
          *  state transition to UNENLISTED to avoid problems when it is reused. 
          */
         
         invalidateConnection(true, e, "Exception in SIXAResourceProxy.end");

         if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "internalEnd");
         throw new XAException(XAException.XAER_RMFAIL);
      }
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "internalEnd");
   }

   /**
    * Called when the transaction manager wants the ME to forget
    * about an in-doubt transaction.
    * <p>
    * Across the wire we will flow:
    * <p>
    * BIT32    Transaction ID
    * The XID Structure
    * 
    * @param xid
    * 
    * @throws XAException if an exception is thrown at the ME. In the
    *         event of a comms failure, an XAException with XAER_RMFAIL will
    *         be thrown. 
    */
   protected void internalForget(Xid xid) throws XAException
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "internalForget", xid);
      
      try
      {
         CommsByteBuffer request = getCommsByteBuffer();
         request.putInt(getTransactionId());
         request.putXid(xid);
         if (fapLevel >= JFapChannelConstants.FAP_VERSION_5) {
            request.put((byte)(isMSResource ? 0x01 : 0x00));
         }         
         
         CommsByteBuffer reply = jfapExchange(request, 
                                              JFapChannelConstants.SEG_XAFORGET, 
                                              JFapChannelConstants.PRIORITY_MEDIUM, 
                                              true);
         try
         {
            reply.checkXACommandCompletionStatus(JFapChannelConstants.SEG_XAFORGET_R, getConversation());
         }
         finally
         {
            reply.release();
         }
      }
      catch (XAException xa)
      {
         // No FFDC Code needed
         // Simply re-throw...
         throw xa;
      }
      catch (Exception e)
      {
         FFDCFilter.processException(e, CLASS_NAME + ".internalForget",
                                     CommsConstants.SIXARESOURCEPROXY_FORGET_01, this);
         
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Caught a comms problem:", e);
         throw new XAException(XAException.XAER_RMFAIL);
      }
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "internalForget");
   }

   /**
    * Called when the transaction manager would like us to 
    * prepare to complete the transaction.
    * <p>
    * Across the wire we will flow:
    * <p>
    * BIT32    Transaction Id
    * The XID Structure
    * 
    * @param xid
    * 
    * @return Returns the result of the resource manager's vote on whether
    *         we can commit. This will be XA_RDONLY if we have done no work
    *         as part of this transaction, or XA_OK.
    * 
    * @throws XAException if an exception is thrown at the ME. In the
    *         event of a comms failure, an XAException with XAER_RMFAIL will
    *         be thrown. 
    */
   protected int internalPrepare(Xid xid) throws XAException
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "internalPrepare", xid);
      
      int rc = 0;
      
      try
      {
         CommsByteBuffer request = getCommsByteBuffer();
         request.putInt(getTransactionId());
         request.putXid(xid);
         if (fapLevel >= JFapChannelConstants.FAP_VERSION_5) {
            request.put((byte)(isMSResource ? 0x01 : 0x00));
         }         
         
         CommsByteBuffer reply = jfapExchange(request, 
                                              JFapChannelConstants.SEG_XAPREPARE, 
                                              getLowestMessagePriority(), 
                                              true);
         try
         {
            reply.checkXACommandCompletionStatus(JFapChannelConstants.SEG_XAPREPARE_R, getConversation());
            
            // Get the return code and return that
            rc = reply.getInt();
         }
         finally
         {
            reply.release();
         }
      }
      catch (XAException xa)
      {
         // No FFDC Code needed
         // Simply re-throw...
         throw xa;
      }
      catch (Exception e)
      {
         FFDCFilter.processException(e, CLASS_NAME + ".internalPrepare",
                                     CommsConstants.SIXARESOURCEPROXY_PREPARE_01, this);

         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Caught a comms problem:", e);
         throw new XAException(XAException.XAER_RMFAIL);
      }
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "internalPrepare", ""+rc);
      return rc;
   }

   /**
    * Called by the transaction manager during recovery to get a list of
    * transactions that are prepared or in-doubt.
    * <p>
    * Across the wire we will flow:
    * <p>
    * BIT32    Transaction Id
    * BIT32    Flags
    * 
    * @param flags
    * 
    * @return Returns an array of Xid's
    * 
    * @throws XAException if an exception is thrown at the ME. In the
    *         event of a comms failure, an XAException with XAER_RMFAIL will
    *         be thrown.
    */
   protected Xid[] internalRecover(int flags) throws XAException
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "internalRecover", ""+flags);
      
      Xid[] xids;
      
      try
      {
         CommsByteBuffer request = getCommsByteBuffer();
         request.putInt(getTransactionId());
         request.putInt(flags);
         if (fapLevel >= JFapChannelConstants.FAP_VERSION_5) {
            request.put((byte)(isMSResource ? 0x01 : 0x00));
         }
         
         CommsByteBuffer reply = jfapExchange(request, 
                                              JFapChannelConstants.SEG_XARECOVER, 
                                              JFapChannelConstants.PRIORITY_MEDIUM, 
                                              true);

         try
         {
            reply.checkXACommandCompletionStatus(JFapChannelConstants.SEG_XARECOVER_R, getConversation());
            
            short numberOfXids = reply.getShort();
            xids = new Xid[numberOfXids];
            
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Received " + numberOfXids + " Xid(s)");
            
            for (int x = 0; x < numberOfXids; x++)
            {
               xids[x] = reply.getXid();
               if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Xid[" + x + "]: " + xids[x]);
            }
         }
         finally
         {
            reply.release();
         }
      }
      catch (XAException xa)
      {
         // No FFDC Code needed
         // Simply re-throw...
         throw xa;
      }
      catch (Exception e)
      {
         FFDCFilter.processException(e, CLASS_NAME + ".recover",
                                     CommsConstants.SIXARESOURCEPROXY_RECOVER_01, this);
         
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Caught a comms problem:", e);
         throw new XAException(XAException.XAER_RMFAIL);
      }
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "internalRecover");
      return xids;
   }

   /**
    * Called by the transaction manager to inform us to rollback the work
    * done as part of this transaction.
    * <p>
    * Across the wire we will flow:
    * <p>
    * BIT32    TransactionId
    * The XID Structure
    * 
    * @param xid
    * 
    * @throws XAException if an exception is thrown at the ME. In the
    *         event of a comms failure, an XAException with XAER_RMFAIL will
    *         be thrown.
    */
   protected void internalRollback(Xid xid) throws XAException
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "internalRollback", xid);
      
      try
      {
         CommsByteBuffer request = getCommsByteBuffer();
         request.putInt(getTransactionId());
         request.putXid(xid);
         if (fapLevel >= JFapChannelConstants.FAP_VERSION_5) {
            request.put((byte)(isMSResource ? 0x01 : 0x00));
         }           
         
         CommsByteBuffer reply = jfapExchange(request, 
                                              JFapChannelConstants.SEG_XAROLLBACK, 
                                              getLowestMessagePriority(), 
                                              true);
         try
         {
            reply.checkXACommandCompletionStatus(JFapChannelConstants.SEG_XAROLLBACK_R, getConversation());
         }
         finally
         {
            reply.release();
         }
      }
      catch (XAException xa)
      {
         // No FFDC Code needed
         // Simply re-throw...
         throw xa;
      }
      catch (Exception e)
      {
         FFDCFilter.processException(e, CLASS_NAME + ".internalRollback",
                                     CommsConstants.SIXARESOURCEPROXY_ROLLBACK_01, this);
         
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Caught a comms problem:", e);
         throw new XAException(XAException.XAER_RMFAIL);
      }
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "internalRollback");
   }

   /**
    * Called when the application wishes to mark the start of the 
    * unit of work in the transaction represented by this Xid.
    * <p>
    * Across the wire we will flow:
    * <p>
    * BIT32    Transaction Id
    * The XID Structure
    * BIT32    Flags
    * 
    * @param xid
    * 
    * @throws XAException if an exception is thrown at the ME. In the
    *         event of a comms failure, an XAException with XAER_RMFAIL will
    *         be thrown.
    */
   protected void internalStart(Xid xid, int flags) throws XAException
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "internalStart", new Object[] {xid, ""+flags});
            
      try
      {
         CommsByteBuffer request = getCommsByteBuffer();
         request.putInt(getTransactionId());
         request.putXid(xid);
         request.putInt(flags);
         if (fapLevel >= JFapChannelConstants.FAP_VERSION_5) {
            request.put((byte)(isMSResource ? 0x01 : 0x00));
         }  
         
         CommsByteBuffer reply = jfapExchange(request, 
                                              JFapChannelConstants.SEG_XASTART, 
                                              JFapChannelConstants.PRIORITY_MEDIUM, 
                                              true);
         try
         {
            reply.checkXACommandCompletionStatus(JFapChannelConstants.SEG_XASTART_R, getConversation());
         }
         finally
         {
            reply.release();
         }
      }
      catch (XAException xa)
      {
         // No FFDC Code needed
         // Simply re-throw...
         throw xa;
      }
      catch (Exception e)
      {
         FFDCFilter.processException(e, CLASS_NAME + ".internalStart",
                                     CommsConstants.SIXARESOURCEPROXY_START_01, this);

         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Caught a comms problem:", e);
         throw new XAException(XAException.XAER_RMFAIL);
      }
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "internalStart");
   }
   
   /**
    * @return Returns true if the XAResource is currently enlisted.
    * 
    * @see com.ibm.ws.sib.comms.client.Transaction#isValid()
    */
   public boolean isValid()
   {
      return isEnlisted();
   }
   
   /**
    * This method should be implemented by the real XA resource instances. Its purpose is so that 
    * resource can appear to be delisted when in fact they are still in the middle of a unit of
    * work. This is needed to support the suspend / resume functionality of an XAResource.
    * 
    * @param enlistedXid The XId that is enlisted. If the XAResource is to be marked as not 
    *                    enlisted, null is passed in.
    */
   abstract void setEnlisted(Xid enlistedXid);
}

