/*******************************************************************************
 * Copyright (c) 2003, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.comms.server.clientsupport;

import java.util.LinkedList;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.comms.CommsConstants;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.SICoreConnection;

/**
 * This class wraps an SICoreConnection that is saved in our object store. It also keeps track of
 * any transactions that the connection owns so that when the connection is closed the transactions
 * can be removed from the table (that spans the socket) of transactions.
 * 
 * @author Gareth Matthews
 */
public class CATConnection
{
   /** Trace */
   private static final TraceComponent tc = SibTr.register(CATConnection.class, 
                                                           CommsConstants.MSG_GROUP,
                                                           CommsConstants.MSG_BUNDLE);

   /** Log class info on load */
   static
   {
      if (tc.isDebugEnabled()) SibTr.debug(tc, "Source info: @(#)SIB/ws/code/sib.comms.server.impl/src/com/ibm/ws/sib/comms/server/clientsupport/CATConnection.java, SIB.comms, WASX.SIB, aa1225.01 1.5");
   }

   /** The underlying SICoreConnection */
   private SICoreConnection conn = null;
   
   /** The list of transaction id's that are owned by this connection */
   private LinkedList tranIds = new LinkedList();
   
   /** 
    * Constructor
    * 
    * @param conn
    */
   public CATConnection(SICoreConnection conn)
   {
      this.conn = conn;
   }
   
   /**
    * @return Returns the underlying SICoreConnection.
    */
   public SICoreConnection getSICoreConnection()
   {
      return conn;
   }
   
   /**
    * @return Returns info about this object.
    */
   public String toString()
   {
      return "CATConnection@" + Integer.toHexString(System.identityHashCode(this)) + ": " +
             ", SICoreConnection: " + conn + 
             ", ME Name: " + conn.getMeName() + " [" + conn.getMeUuid() + "] " +
             ", Version: " + conn.getApiLevelDescription() + 
             ", Associated transactions: " + tranIds;
   }
}
