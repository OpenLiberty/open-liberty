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

import java.util.HashMap;

import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.comms.CommsConstants;
import com.ibm.ws.sib.comms.common.CommsByteBuffer;
import com.ibm.ws.sib.jfapchannel.Conversation;
import com.ibm.ws.sib.jfapchannel.JFapChannelConstants;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.SICoreConnection;

/**
 * This class acts as a proxy between the remote client and a remote messaging engine's XAResource. 
 * All calls made on this object are proxied up to the actual remote XAResource.
 * <p>
 * This class also supports joining with units of work that were started using a different
 * XAResource instance. The current list of inflight work is held in the client link level state
 * and when TMJOIN is specified, this instance simply acts as a proxy to the real instance which
 * actually started the transaction. This resource is unjoined when end() is called.
 * 
 * @author Gareth Matthews
 */
public class SIXAResourceProxy extends BaseSIXAResourceProxy
{
   /** Class name for FFDC's */
   private static String CLASS_NAME = SIXAResourceProxy.class.getName();
   
   /** Register Class with Trace Component */
   private static final TraceComponent tc = SibTr.register(SIXAResourceProxy.class,
                                                           CommsConstants.MSG_GROUP, 
                                                           CommsConstants.MSG_BUNDLE);

   /** Log source info on class load */
   static
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Source info: @(#)SIB/ws/code/sib.comms.client.impl/src/com/ibm/ws/sib/comms/client/SIXAResourceProxy.java, SIB.comms, WASX.SIB, uu1215.01 1.41");
   }
   
   /** The resource we are currently joined to or null if we are not joined */
   private SIXAResourceProxy joinedResource = null;
   
   /** The possible states of this resource */
   private enum ResourceState { ENLISTED, UNENLISTED, JOINED };
   
   /** The current state */
   private ResourceState state = ResourceState.UNENLISTED;
   
   /** 
    * This flag is used to keep track of when end has been called. For this purpose we could 
    * just use the resource state - but the resource state goes into the unenlisted state even when
    * it is suspended. As such this flag is set to false on start(), and true when end() is called 
    * and is used for state checking to decide when an end() should be done when resources have been
    * joined together.
    */
   private boolean endHasBeenCalled = false;
   
   /**
    * Constructor.
    * 
    * @param conv
    * @param cp
    * @param isMSResource
    */
   public SIXAResourceProxy(Conversation conv, ConnectionProxy cp, boolean isMSResource)
   {
      super(conv, cp, isMSResource);
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "<init>");
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "<init>");
   }
   
   /**
    * @return Returns true if we are in between an XASTART and an XAEND 
    */
   public boolean isEnlisted()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "isEnlisted");
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Current state = " + state);
      final boolean result = (state == ResourceState.ENLISTED) || (state == ResourceState.JOINED); 
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "isEnlisted", ""+result);
      return result;
   }
   
   /**
    * This method is used internally to set whether we are currently enlisted. This is used in the
    * case where a transaction is suspended as when this occurs the resource is effectively 
    * delisted (although this is never passed onto the server).
    * 
    * @see com.ibm.ws.sib.comms.client.BaseSIXAResourceProxy#setEnlisted(javax.transaction.xa.Xid)
    */
   void setEnlisted(Xid enlistedXid)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "setEnlisted", enlistedXid);
      if (enlistedXid == null)         state = ResourceState.UNENLISTED;
      else if (joinedResource != null) state = ResourceState.JOINED;
      else                             state = ResourceState.ENLISTED;
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "State is now = " + state);
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "setEnlisted");
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
   public void commit(Xid xid, boolean onePhase) throws XAException
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "commit",
                                           new Object[] {xid, ""+onePhase});
      
      internalCommit(xid, onePhase);
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "commit");
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
   public void end(Xid xid, int flags) throws XAException
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "end",
                                           new Object[] {xid, ""+flags});
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Current state = " + state);
      
      if (state == ResourceState.UNENLISTED)
      {
         // We are not already enlisted here - something is a bit wrong
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "We are not enlisted - rejecting this call");
         throw new XAException(XAException.XAER_PROTO); 
      }
      else if (state == ResourceState.JOINED)
      {
         // Unjoin the resource...
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "state == JOINED. Unjoining the resource.");
         joinedResource.unjoin(this);
      }
      
      // Now decide whether to do the end() or not. If this resource was joined to another one then
      // we perform the end now only if there are no more joined resources and if end() has been
      // called on the primary XAResource.
      boolean performEndNow = false;
      if (joinedResource != null)
      {
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Joined resource state: " + joinedResource.state);
         
         performEndNow = (!joinedResource.hasJoinedResources()) && 
                         (joinedResource.endHasBeenCalled); 
      }
      // Otherwise, we can perform an end() if there are no other resources joined to us
      else
      {
         performEndNow = (!hasJoinedResources());
      }
      
      // Now perform the end if we need to now
      if (performEndNow)
      {
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Ending the resource.");
         
         internalEnd(xid, flags);
         
         HashMap<Xid, SIXAResourceProxy> map = getLinkLevelXAResourceMap();
         synchronized(map)
         {
            map.remove(xid);
         }
      }

      // Now we are not enlisted
      state = ResourceState.UNENLISTED;
      joinedResource = null;
      endHasBeenCalled = true;
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "end");
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
   public void forget(Xid xid) throws XAException
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "forget", xid);

      internalForget(xid);
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "forget");
   }

   /**
    * Returns the transaction timeout for this XAResource instance.
    * 
    * @return int
    * 
    * @throws XAException if an exception is thrown at the ME. In the
    *         event of a comms failure, an XAException with XAER_RMFAIL will
    *         be thrown. 
    */
   public int getTransactionTimeout() throws XAException
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getTransactionTimeout");
      
      int timeout = 0;
            
      try
      {
         CommsByteBuffer request = getCommsByteBuffer();
         request.putInt(getTransactionId());
         
         CommsByteBuffer reply = jfapExchange(request, 
                                              JFapChannelConstants.SEG_XA_GETTXTIMEOUT, 
                                              JFapChannelConstants.PRIORITY_MEDIUM, 
                                              true);
      
         try
         {
            reply.checkXACommandCompletionStatus(JFapChannelConstants.SEG_XA_GETTXTIMEOUT_R, getConversation());
                              
            timeout = reply.getInt();
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
         throw xa;
      }
      catch (Exception e)
      {
         FFDCFilter.processException(e, CLASS_NAME + ".getTransactionTimeout",
                                     CommsConstants.SIXARESOURCEPROXY_GETTXTIMEOUT_01, this);
         
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Caught a comms problem:", e);
         throw new XAException(XAException.XAER_RMFAIL);
      }
         
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getTransactionTimeout", ""+timeout);
      return timeout;
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
   public int prepare(Xid xid) throws XAException
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "prepare", xid);

      final int result = internalPrepare(xid);
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "prepare", ""+result);
      return result;
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
   public Xid[] recover(int flags) throws XAException
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "recover", ""+flags);

      final Xid[] result = internalRecover(flags);
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "recover", result);
      return result;
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
   public void rollback(Xid xid) throws XAException
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "rollback", xid);

      internalRollback(xid);
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "rollback");
   }

   /**
    * Sets the transaction timeout for this instance.
    * <p>
    * Across the wire we will flow:
    * <p>
    * BIT32    Transaction Id
    * BIT32    Timeout
    * 
    * @param timeout The timeout value
    * 
    * @return Returns true if the timeout was set. If the RM does not
    *         support setting this, it returns false.
    * 
    * @throws XAException if an exception is thrown at the ME. In the
    *         event of a comms failure, an XAException with XAER_RMFAIL will
    *         be thrown.
    */
   public boolean setTransactionTimeout(int timeout) throws XAException
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "setTransactionTimeout", ""+timeout);
      
      boolean success = false;
            
      try
      {
         CommsByteBuffer request = getCommsByteBuffer();
         request.putInt(getTransactionId());
         request.putInt(timeout);
              
         CommsByteBuffer reply = jfapExchange(request, 
                                              JFapChannelConstants.SEG_XA_SETTXTIMEOUT, 
                                              JFapChannelConstants.PRIORITY_MEDIUM, 
                                              true);

         try
         {
            reply.checkXACommandCompletionStatus(JFapChannelConstants.SEG_XA_SETTXTIMEOUT_R, getConversation());
            
            // Get the return code and return that
            if (reply.get() == 1) success = true;
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
         throw xa;
      }
      catch (Exception e)
      {
         FFDCFilter.processException(e, CLASS_NAME + ".setTransactionTimeout",
                                     CommsConstants.SIXARESOURCEPROXY_SETTXTIMEOUT_01, this);
         
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Caught a comms problem:", e);
         throw new XAException(XAException.XAER_RMFAIL);
      }
         
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "setTransactionTimeout", ""+success);
      return success;
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
    * @param flags
    * 
    * @throws XAException if an exception is thrown at the ME. In the
    *         event of a comms failure, an XAException with XAER_RMFAIL will
    *         be thrown.
    */
   public void start(Xid xid, int flags) throws XAException
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "start", new Object[] {xid, ""+flags});
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Current state = " + state);
      
      if ((state == ResourceState.ENLISTED) || (state == ResourceState.JOINED))
      {
         // We are already enlisted here - something is a bit wrong
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "We are already enlisted - rejecting this call");
         throw new XAException(XAException.XAER_PROTO);
      }
      else if ((flags & XAResource.TMJOIN) == XAResource.TMJOIN)
      {
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "TMJOIN was specified");
         
         // Now get the resource from the map that we would like to join up to
         HashMap<Xid, SIXAResourceProxy> map = getLinkLevelXAResourceMap();
         synchronized(map)
         {
            joinedResource = map.get(xid);
         }
         
         // If the resource did not exist in the map, then we cannot possible join up to it
         if (joinedResource == null)
         {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "The resource cannot be joined as it doesn't exist");
            throw new XAException(XAException.XAER_INVAL);
         }
         
         state = ResourceState.JOINED;
         joinedResource.join(this);
      }
      else
      {
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Performing a normal start");
         
         internalStart(xid, flags);
         
         // Now we are enlisted
         state = ResourceState.ENLISTED;
         
         // Now add an entry into the link level state map
         HashMap<Xid, SIXAResourceProxy> map = getLinkLevelXAResourceMap();
         synchronized(map)
         {
            map.put(xid, this);
         }
      }
      
      endHasBeenCalled = false;
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "start");
   }

   /**
    * This method will determine whether the XAResource passed in
    * is using the same resource manager as this XAResource.
    * <p>
    * At the comms level, we are checking to see if the SICoreConnection
    * that the XAResource was created from is connected to the same ME. 
    * That is, the connections must share the same link (physical socket)
    * and be connected to the same ME.
    * <p>
    * It could be possible that this returns false, when in fact they are
    * both connected to the same ME. For example, if one XAResource connects
    * to the ME in-process and one connects over TCP/IP. Here there is no
    * way for us to safely return true, so we will return false.
    * 
    * @param xaRes The XAResource to compare
    * 
    * @return Returns true if the XA resources are connected to the same RM.
    * 
    * @throws XAException
    */
   public boolean isSameRM(XAResource xaRes) throws XAException
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "isSameRM", xaRes);
      
      boolean result = false;
      
      // If the passed in XAResource is not a client proxy version
      // don't even try - there is no way for us to know here
      if (xaRes instanceof SIXAResourceProxy)
      {
         // We have no idea what the state of the other connection is in here,
         // so ensure we do not Null pointer or something
         try
         {
            SICoreConnection thisConnection = ((ClientConversationState) getConversation().getAttachment()).getSICoreConnection();
            SICoreConnection otherConnection = ((ClientConversationState) ((SIXAResourceProxy) xaRes).getConversation().getAttachment()).getSICoreConnection();
            
            result = thisConnection.isEquivalentTo(otherConnection);
         }
         catch (Throwable t)
         {
            // No FFDC code needed
            // They must have passed in an invalid type of connection, or one that
            // was null, or closed - so we would assume they are not equal. For helpfulness
            // though, print out the exception
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Caught an exception comparing the connections", t); 
         }
      }
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "isSameRM", ""+result);
      return result;
   }
   
   /**
    * Helper method to retrieve the map in the link level state that contains the XAResources and
    * the XId they are currently enlisted with.
    * 
    * @return Returns the map.
    */
   private HashMap<Xid, SIXAResourceProxy> getLinkLevelXAResourceMap()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getLinkLevelXAResourceMap");
      HashMap<Xid, SIXAResourceProxy> map = ((ClientLinkLevelState)getConversation().getLinkLevelAttachment()).getXidToXAResourceMap();
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getLinkLevelXAResourceMap", map);
      return map;
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
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Current state = " + state);
      
      final short result;
      if (state == ResourceState.JOINED)
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
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Current state = " + state);
      
      final int result;
      if (state == ResourceState.JOINED)
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
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Current state = " + state);
      
      if (state == ResourceState.JOINED)
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
    * Ensure toString includes comms tranId
    */
   public String toString() {
     return super.toString() + "[commsTx=" + getTransactionId() + "]";
   }
}
