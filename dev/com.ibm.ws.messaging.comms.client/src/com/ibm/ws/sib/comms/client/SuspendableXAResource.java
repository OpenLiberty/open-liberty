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
package com.ibm.ws.sib.comms.client;

import java.util.ArrayList;
import java.util.HashMap;

import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.comms.CommsConstants;
import com.ibm.ws.sib.jfapchannel.Conversation;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.SIXAResource;

/**
 * This class allows the client XA Resource objects to be suspendable as per the JTA specification.
 * This class is the object that is returned to clients when the <code>getSIXAResource()</code> 
 * method is called on SICoreConnection over a client connection. Embedded into this instance is
 * a 'real' transaction implementation (which is either an optimized transaction instance or an
 * old style transaction instance) and under normal operation any XA calls are simply forwarded 
 * onto the real transaction instance.
 * <p>
 * In the event that the client issues a call to start() specifying TMRESUME or end() specifying
 * TMSUSPEND then this class gets involved by masking the suspension from the server end. (You may
 * be wondering why the server does not simply support this - but we require this functionality to
 * work across all the major releases and it is less work to do it this way).
 * <p>
 * When TMSUSPEND is specified on an end() call, the server is not informed of this (so it believes
 * the transaction is still in progress). The actual transaction implementation is placed into a 
 * map of suspended transactions. It is now possible for this XAResource to be used in other units
 * of work (i.e. a start() can be called with a different XId). When this happens a new transaction
 * implementation is instatiated under the covers and this is used for the new work. When the 
 * initial transaction is needed to be resumed it is removed from the map and made 'live' again.
 * <p>
 * This class keeps track of the current 'live' transaction in progress. Any calls on this class 
 * are then forwarded onto the live transaction. Once a new transaction instance is created due to
 * it being required because of an earlier suspension these instances are pooled in case they are 
 * needed in the future. They are pooled when end() is called on them. The size of the pool will 
 * grow to the maximum number of concurrently suspended transactions using this XAResource.
 * 
 * @author Gareth Matthews
 */
public class SuspendableXAResource extends Transaction implements SIXAResource
{
   /** Class name for FFDC's */
   private static String CLASS_NAME = SuspendableXAResource.class.getName();

   /** Register Class with Trace Component */
   private static final TraceComponent tc = SibTr.register(SuspendableXAResource.class,
                                                           CommsConstants.MSG_GROUP, 
                                                           CommsConstants.MSG_BUNDLE);
   
   /** The suspended transactions map */
   private HashMap<Xid, BaseSIXAResourceProxy> suspendedTransactions = new HashMap<Xid, BaseSIXAResourceProxy>();
   
   /** The current useable (non-suspended) XA resource */
   private BaseSIXAResourceProxy currentLiveXAResource = null;
   
   /** A list of created SIXAResource's that are available for use */
   private ArrayList<BaseSIXAResourceProxy> useableXAResources = new ArrayList<BaseSIXAResourceProxy>();
   
   /** The current Xid that is in use */
   private Xid currentXid = null;
   
   /** A flag to indicate whether a MS resource should be created */
   private boolean useMSResource = false;
   
   /**
    * Constructor.
    * 
    * @param con
    * @param owningConnection
    * @param initialXAResource
    * @param useMSResource
    */
   public SuspendableXAResource(Conversation con, ConnectionProxy owningConnection, 
                                BaseSIXAResourceProxy initialXAResource, boolean useMSResource)
   {
      super(con, owningConnection);
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "<init>",
                                           new Object[]{con, owningConnection, initialXAResource, ""+useMSResource});
      
      this.currentLiveXAResource = initialXAResource;
      this.useMSResource = useMSResource;
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "<init>");
   }
   
   /**
    * @return Returns the current 'live' XAResource that is being used. This is needed when the 
    *         transaction information is needed to be serialized across the wire.
    */
   public SIXAResource getCurrentXAResource()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getCurrentXAResource");
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getCurrentXAResource", currentLiveXAResource);
      return currentLiveXAResource;
   }
   
  
   // *******************************************************************************************
   // *                     Methods inherited from Transaction                                  *
   // *******************************************************************************************
   
   
   /**
    * @return Returns whether the current live XA Resource is valid.
    * @see com.ibm.ws.sib.comms.client.Transaction#isValid()
    */
   public boolean isValid()
   {
      return currentLiveXAResource.isValid();
   }
   
   /**
    * @return Returns the transaction Id of the current live XA Resource instance. This is
    *         important as that is the key which the server uses to keep track of these things. 
    * @see com.ibm.ws.sib.comms.client.Transaction#getTransactionId()
    */
   public int getTransactionId()
   {
      return currentLiveXAResource.getTransactionId();
   }
   
   
   // *******************************************************************************************
   // *                     Methods inherited from SIXAResource                                 *
   // *******************************************************************************************

   
   /**
    * The start method is used to begin a unit of work. If TMRESUME is specified a search for the 
    * XId is done in our suspendedTransactions map. If the XId finds a match, the transaction is
    * made 'live' and play continues using the associated XAResource.
    * <p>
    * If TMRESUME was not specified, this falls through to the current live XAResource instance. If
    * however, the current XAResource is suspended a new one is retrieved directly from the 
    * ConnectionProxy and used.
    * 
    * @see javax.transaction.xa.XAResource#start(javax.transaction.xa.Xid, int)
    */
   public void start(Xid xid, int flags) throws XAException
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "start", new Object[]{xid, flags});
      
      // First have a check to see if this start is a resume.
      if ((flags & XAResource.TMRESUME) != 0)
      {
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "TMRESUME was specified");
         
         // Now look for the right XA Resource that is in our suspended XID map. If that is not
         // found then we can throw a XAER_NOTA return code
         BaseSIXAResourceProxy tx = suspendedTransactions.remove(xid);
         
         if (tx == null)
         {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "There was no XId in the map");
            
            throw new XAException(XAException.XAER_NOTA);
         }
         
         currentLiveXAResource = tx;
         currentLiveXAResource.isSuspended = false;
         currentLiveXAResource.setEnlisted(xid);
         
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Resumed: ", currentLiveXAResource);
      }
      // Otherwise we assume we are going to start a new transaction. The SIXAResource we use 
      // doesn't really matter but we make an attempt not to create any more that we need. Normally
      // we'll use 'this' unless we are suspended. In which case we'll use one from our 
      // useableSIXAResources list. If this is empty, we'll create a new one from the connection
      else
      {
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Starting a new transaction");
         
         // If the current useable XA resource is not null and is not suspended, then we do not 
         // need to do anything, we can simply use that.
         if (currentLiveXAResource != null && (!currentLiveXAResource.isSuspended))
         {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Using the current XAResource");
         }
         else
         {
            // If we are suspended, then have a look and see if there is an SIXAResource free in 
            // the useableXAResources list
            if (useableXAResources.size() != 0)
            {
               if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Re-using an old SIXAResource");
               currentLiveXAResource = useableXAResources.remove(0);
            }
            else
            {
               // Nope - we must create a new one
               if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Obtaining fresh XA Resource");
               try
               {
                  currentLiveXAResource = getConnectionProxy()._createXAResource(useMSResource);
               }
               catch (SIException e)
               {
                  FFDCFilter.processException(e, CLASS_NAME + ".start", 
                                              CommsConstants.SUSPENDABLEXARESOURCE_START_01, this);
                  
                  if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Unable to obtain the XA Resource");
                  
                  // This is bad, rethrow as the worst XA Exception
                  XAException xa = new XAException(XAException.XAER_RMERR);
                  xa.initCause(e);
                  throw xa;
               }
            }            
         }
         
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Using: ", currentLiveXAResource);
         
         // Now call the relevant method on the right XA Resource
         currentLiveXAResource.start(xid, flags);

      }

      currentXid = xid;
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "start");
   }
   
   /**
    * The end method is used to signify the end or the temporary end of a unit of work. If the 
    * TMSUSPEND flag is specified, the transaction can be resumed later but other units of work can
    * be done on the same XAResource during that time.
    * <p>
    * If TMSUSPEND is specified the current live XAResource is placed into our suspendedTransactions
    * map keyed by the XId and a flag is set on the XAResource indicating that it is currently 
    * suspended. This has the effect of informing the start() call that a new XAResource instance is
    * needed if other units of work are to be started.
    * <p>
    * If TMSUSPEND is not specified then either a unit of work is ending normally or a suspended
    * transaction is being completed. In the event that a suspended transaction is being completed
    * the associated XAResource is made live (as if they had called start(TMRESUME)) before the 
    * end call is passed onto the real XAResource.
    * 
    * @see javax.transaction.xa.XAResource#end(javax.transaction.xa.Xid, int)
    */
   public void end(Xid xid, int flags) throws XAException
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "end", new Object[]{xid, flags});
      
      // Are they wanting to suspend the current transaction?
      if ((flags & XAResource.TMSUSPEND) != 0)
      {
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "TMSUSPEND was specified");
         
         // Is the resource already in the map? If so, throw an exception
         if (suspendedTransactions.get(xid) != null)
         {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "XID is already suspended");
            
            throw new XAException(XAException.XAER_PROTO);
         }
         
         // Is this XId actually the one currently being used?
         if (currentXid != null && !currentXid.equals(xid))
         {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Attempt to suspend some other XId");
            
            throw new XAException(XAException.XAER_NOTA);
         }
         
         // Add this transaction to the suspended transactions map
         suspendedTransactions.put(xid, currentLiveXAResource);
         currentLiveXAResource.isSuspended = true;
         currentLiveXAResource.setEnlisted(null);
      }
      else
      {
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "TMSUSPEND was not specified");
         
         // Is the current XA Resource not active? If so then this is probably a call to
         // simply end a suspended transaction as you can transition to this state without first
         // restarting a suspended transaction.
         if (!currentLiveXAResource.isEnlisted())
         {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Currently not enlisted");
            
            // It is possible that this will be an end call to a suspended transaction (i.e. to skip
            // the restarting). Have a quick check for this XId in our suspended map
            BaseSIXAResourceProxy tx = suspendedTransactions.remove(xid);
            
            // If we find nothing here, then we were not started for this XId and this is a protocol
            // error
            if (tx == null)
            {
               if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "No suspended XId found and not currently started");
               throw new XAException(XAException.XAER_NOTA);
            }
            
            // Restart the suspended transaction
            currentLiveXAResource = tx;
            currentLiveXAResource.setEnlisted(xid);
         }
         
         currentLiveXAResource.isSuspended = false;
         currentLiveXAResource.end(xid, flags);
         
         // Here we can pool the SIXAResource if we have not already done so
         if (!useableXAResources.contains(currentLiveXAResource))
         {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Pooling the SIXAResource");
            useableXAResources.add(currentLiveXAResource);
         }
      }
      
      currentXid = null;
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "end");
   }

   /**
    * @see javax.transaction.xa.XAResource#forget(javax.transaction.xa.Xid)
    */
   public void forget(Xid xid) throws XAException
   {
      currentLiveXAResource.forget(xid);
   }

   /**
    * @see javax.transaction.xa.XAResource#getTransactionTimeout()
    */
   public int getTransactionTimeout() throws XAException
   {
      return currentLiveXAResource.getTransactionTimeout();
   }
   
   /**
    * In this method we must ensure that we unpack the appropriate XAResource if appropriate from
    * the SuspendableXAResource instance if one was passed in.
    * 
    * @see javax.transaction.xa.XAResource#isSameRM(javax.transaction.xa.XAResource)
    */
   public boolean isSameRM(XAResource xares) throws XAException
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "isSameRM", xares);
      XAResource actualXARes = xares;
      if (xares instanceof SuspendableXAResource)
      {
         actualXARes = ((SuspendableXAResource) xares).getCurrentXAResource();
      }
      boolean result = currentLiveXAResource.isSameRM(actualXARes);
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "isSameRM", result);
      return result; 
   }
   
   /**
    * @see javax.transaction.xa.XAResource#prepare(javax.transaction.xa.Xid)
    */
   public int prepare(Xid xid) throws XAException
   {
      return currentLiveXAResource.prepare(xid);
   }

   /**
    * @see javax.transaction.xa.XAResource#recover(int)
    */
   public Xid[] recover(int flag) throws XAException
   {
      return currentLiveXAResource.recover(flag);
   }

   /**
    * @see javax.transaction.xa.XAResource#rollback(javax.transaction.xa.Xid)
    */
   public void rollback(Xid xid) throws XAException
   {
      currentLiveXAResource.rollback(xid);
   }

   /**
    * @see javax.transaction.xa.XAResource#setTransactionTimeout(int)
    */
   public boolean setTransactionTimeout(int seconds) throws XAException
   {
      return currentLiveXAResource.setTransactionTimeout(seconds);
   }
   
   /**
    * @see com.ibm.wsspi.sib.core.SIXAResource#isEnlisted()
    */
   public boolean isEnlisted()
   {
      return currentLiveXAResource.isEnlisted();
   }

   /**
    * @see javax.transaction.xa.XAResource#commit(javax.transaction.xa.Xid, boolean)
    */
   public void commit(Xid xid, boolean onePhase) throws XAException
   {
      currentLiveXAResource.commit(xid, onePhase);
   }
}
