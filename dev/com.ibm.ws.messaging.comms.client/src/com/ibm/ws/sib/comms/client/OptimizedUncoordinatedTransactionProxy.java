/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.comms.client;

import javax.transaction.xa.Xid;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIIncorrectCallException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.comms.CommsConstants;
import com.ibm.ws.sib.jfapchannel.Conversation;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.exception.SIConnectionLostException;
import com.ibm.wsspi.sib.core.exception.SIRollbackException;

/**
 * Optimized implementation of an uncoordinated (local) transaction.
 * The term "optimized" refers to the implementation creating the
 * local transaction at the point it is required - rather than
 * sending an explicit "create local transaction" flow over the
 * network.
 */
public class OptimizedUncoordinatedTransactionProxy extends LocalTransactionProxy implements OptimizedTransaction
{
// @start_class_string_prolog@
   public static final String $sccsid = "@(#) 1.6 SIB/ws/code/sib.comms.client.impl/src/com/ibm/ws/sib/comms/client/OptimizedUncoordinatedTransactionProxy.java, SIB.comms, WASX.SIB, uu1215.01 06/07/28 02:37:17 [4/12/12 22:14:05]";
// @end_class_string_prolog@
   
   /** Class name for FFDC's */
   private static final String CLASS_NAME = OptimizedUncoordinatedTransactionProxy.class.getName();
   
   /** Register Class with Trace Component */
   private static final TraceComponent tc = SibTr.register(OptimizedUncoordinatedTransactionProxy.class,
                                                           CommsConstants.MSG_GROUP, 
                                                           CommsConstants.MSG_BUNDLE);
   
   private int creatingConnection;
   private int creatingConversation;
   
   static
   {
      if (tc.isDebugEnabled()) SibTr.debug(tc, $sccsid);
   }

   // Flag used to track whether a corresponding local transaction object has
   // been created on the server.  This is required for two reasons:
   // 1) So that the transaction can be created at the point of its first use
   // 2) So that a transaction which has no work done in its context can be
   //    optimized away to a no-op.
   private boolean isTxCreatedOnServer = false;

   // Should the server side representation support subordinates?
   private final boolean allowSubordinates;
   
	/**
    * @param con
    *           Conversation which "owns" the transaction.
    * @param cp
    *           Connection which "owns" the transaction.
    * @param allowSubordinates
    *           Should the optimize transaction attempt to create a server-side uncoordinated
    *           transaction which allows subordinate enlistment.
    */
   public OptimizedUncoordinatedTransactionProxy(Conversation con, ConnectionProxy cp,
            boolean allowSubordinates)
   {
      super(con, cp);
      this.allowSubordinates = allowSubordinates;
      creatingConnection = getConnectionObjectID();
      creatingConversation = con.getId();

      if (tc.isEntryEnabled())
      {
         SibTr.entry(this, tc, "<init>", new Object[]
         {
            con, cp
         });
         SibTr.exit(this, tc, "<init>");
      }
   }

   /** @see com.ibm.wsspi.sib.core.SIUncoordinatedTransaction#commit() */
   public void commit() throws SIIncorrectCallException, SIRollbackException, SIResourceException,
            SIConnectionLostException
   {
      if (tc.isEntryEnabled()) SibTr.entry(this, tc, "commit");
      if (isTxCreatedOnServer)
      {
         super.commit();
      }
      if (tc.isEntryEnabled()) SibTr.exit(this, tc, "commit");
   }

   /** @see com.ibm.wsspi.sib.core.SIUncoordinatedTransaction#rollback() */
   public void rollback() throws SIIncorrectCallException, SIResourceException,
            SIConnectionLostException
   {
      if (tc.isEntryEnabled()) SibTr.entry(this, tc, "rollback");
      if (isTxCreatedOnServer)
      {
         super.rollback();
      }
      if (tc.isEntryEnabled()) SibTr.exit(this, tc, "rollback");
   }

   /** @see com.ibm.ws.sib.comms.client.OptimizedTransaction#isServerTransactionCreated() */
   public boolean isServerTransactionCreated()
   {
      if (tc.isEntryEnabled()) SibTr.entry(this, tc, "isServerTransactionCreated");
      final boolean result = isTxCreatedOnServer;
      if (tc.isEntryEnabled()) SibTr.exit(this, tc, "isServerTransactionCreated", "" + result);
      return result;
   }

   /** @see com.ibm.ws.sib.comms.client.OptimizedTransaction#setServerTransactionCreated() */
   public void setServerTransactionCreated()
   {
      if (tc.isEntryEnabled()) SibTr.entry(this, tc, "setServerTransactionCreated");
      isTxCreatedOnServer = true;
      if (tc.isEntryEnabled()) SibTr.exit(this, tc, "setServerTransactionCreated");
   }

   /** @see com.ibm.ws.sib.comms.client.OptimizedTransaction#getXidForCurrentUow() */
   public Xid getXidForCurrentUow()
   {
      if (tc.isEntryEnabled()) SibTr.entry(this, tc, "getXidForCurrentUow");

      // This should never be invoked for an uncoordinated transaction!
      final SIErrorException exception = new SIErrorException();
      FFDCFilter.processException(exception, CLASS_NAME + ".getXidForCurrentUow",
                                  CommsConstants.OPTUNCOORDPROXY_GETXID_01, this);
      if (tc.isEventEnabled()) SibTr.exception(this, tc, exception);

      if (tc.isEntryEnabled()) SibTr.exit(this, tc, "getXidForCurrentUow");
      throw exception;
   }

   /** @see com.ibm.ws.sib.comms.client.OptimizedTransaction#isEndRequired() */
   public boolean isEndRequired()
   {
      if (tc.isEntryEnabled()) SibTr.entry(this, tc, "isEndRequired");

      // This should never be invoked for an uncoordinated transaction!
      final SIErrorException exception = new SIErrorException();
      FFDCFilter.processException(exception, CLASS_NAME + ".isEndRequired",
                                  CommsConstants.OPTUNCOORDPROXY_ISENDREQUIRED_01, this);
      if (tc.isEventEnabled()) SibTr.exception(this, tc, exception);

      if (tc.isEntryEnabled()) SibTr.exit(this, tc, "isEndRequired");
      throw exception;
   }

   /** @see com.ibm.ws.sib.comms.client.OptimizedTransaction#setEndNotRequired() */
   public void setEndNotRequired()
   {
      if (tc.isEntryEnabled()) SibTr.entry(this, tc, "setEndNotRequired");

      // This should never be invoked for an uncoordinated transaction!
      final SIErrorException exception = new SIErrorException();
      FFDCFilter.processException(exception, CLASS_NAME + ".setEndNotRequired",
                                  CommsConstants.OPTUNCOORDPROXY_SETENDNOTREQUIRED_01, this);
      if (tc.isEventEnabled()) SibTr.exception(this, tc, exception);

      if (tc.isEntryEnabled()) SibTr.exit(this, tc, "setEndNotRequired");
      throw exception;
   }

   /** @see com.ibm.ws.sib.comms.client.OptimizedTransaction#getEndFlags() */
   public int getEndFlags()
   {
      if (tc.isEntryEnabled()) SibTr.entry(this, tc, "getEndFlags");

      // This should never be invoked for an uncoordinated transaction!
      final SIErrorException exception = new SIErrorException();
      FFDCFilter.processException(exception, CLASS_NAME + ".getEndFlags",
                                  CommsConstants.OPTUNCOORDPROXY_GETENDFLAGS_01, this);
      if (tc.isEventEnabled()) SibTr.exception(this, tc, exception);

      if (tc.isEntryEnabled()) SibTr.exit(this, tc, "getEndFlags");
      throw exception;
   }

   /**
    * @return true if the optimized transaction allows subordinate enlistment.
    */
   public boolean areSubordinatesAllowed()
   {
      if (tc.isEntryEnabled()) SibTr.entry(this, tc, "areSubordinatesAllowed");
      final boolean result = allowSubordinates;
      if (tc.isEntryEnabled()) SibTr.exit(this, tc, "areSubordinatesAllowed", "" + result);
      return result;
   }

   /** @see com.ibm.ws.sib.comms.client.OptimizedTransaction#getCreatingConnectionId() */
   public int getCreatingConnectionId()
   {
      return creatingConnection;
   }

   /** @see com.ibm.ws.sib.comms.client.OptimizedTransaction#getCreatingConversationId() */
   public int getCreatingConversationId()
   {
      return creatingConversation;
   }
}
