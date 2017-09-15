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
package com.ibm.ws.sib.comms.client;

import java.util.HashMap;

import javax.transaction.xa.Xid;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.comms.CommsConstants;
import com.ibm.ws.sib.jfapchannel.LinkLevelState;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * This class holds state at the physical socket level. Each Conversation
 * that uses the same physical socket will have reference to this state.
 * 
 * @author Gareth Matthews
 */
public class ClientLinkLevelState implements LinkLevelState
{
   /**
    * Register Class with Trace Component
    */
   private static final TraceComponent tc = SibTr.register(ClientLinkLevelState.class,
                                                           CommsConstants.MSG_GROUP,
                                                           CommsConstants.MSG_BUNDLE);
   
   /** The last transaction id that was allocated */
   private int lastTransactionId = 0;
   
   /** A map of XAResource's keyed by their corresponding XId's for inflight transactions */
   private HashMap<Xid, SIXAResourceProxy> xidToXAResourceMap = new HashMap<Xid, SIXAResourceProxy>();
   
   /**
    * Constructor.
    */
   public ClientLinkLevelState()
   {
      if (tc.isEntryEnabled()) SibTr.entry(this, tc, "<init>");
      if (tc.isEntryEnabled()) SibTr.exit(this, tc, "<init>");
   }
   
   // start f181927
   /**
    * @return Returns the next available transaction ID.
    */
   public synchronized int getNextTransactionId()
   {
      if (tc.isEntryEnabled()) SibTr.entry(this, tc, "getNextTransactionId");
      int nextTransactionId = ++lastTransactionId;
      if (tc.isEntryEnabled()) SibTr.exit(this, tc, "getNextTransactionId", ""+nextTransactionId);
      return nextTransactionId;
   }
   // end f181927
   
   /**
    * @return Returns a Map of XAResources keyed by XId for current in-flight, unoptimized 
    *         transactions.
    */
   public HashMap<Xid, SIXAResourceProxy> getXidToXAResourceMap()
   {
      if (tc.isEntryEnabled()) SibTr.entry(this, tc, "getXidToXAResourceMap");
      if (tc.isEntryEnabled()) SibTr.exit(this, tc, "getXidToXAResourceMap", xidToXAResourceMap);
      return xidToXAResourceMap;
   }
   
   public void reset()
   {
       xidToXAResourceMap = new HashMap<Xid, SIXAResourceProxy>();
   }
}
