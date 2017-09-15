/*******************************************************************************
 * Copyright (c) 2003, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.jfapchannel.richclient.framework.impl;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.jfapchannel.JFapChannelConstants;
import com.ibm.ws.sib.jfapchannel.framework.NetworkConnection;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.channelfw.VirtualConnection;

/**
 * This class is extended by both the CFWIOReadRequestContext and CFWIOWriteRequestContext classes
 * to provide any common function.
 * 
 * @author Gareth Matthews
 */
public abstract class CFWIOBaseContext
{
   /** Trace */
   private static final TraceComponent tc = SibTr.register(CFWIOBaseContext.class, 
                                                           JFapChannelConstants.MSG_GROUP, 
                                                           JFapChannelConstants.MSG_BUNDLE);
   
   /** Log class info on load */
   static
   {
      if (tc.isDebugEnabled()) SibTr.debug(tc, "@(#) SIB/ws/code/sib.jfapchannel.client.rich.impl/src/com/ibm/ws/sib/jfapchannel/framework/impl/CFWIOBaseContext.java, SIB.comms, WASX.SIB, uu1215.01 1.1");
   }

   /** The connection reference */
   private NetworkConnection conn = null;
   
   /**
    * Constructor.
    * 
    * @param conn
    */
   public CFWIOBaseContext(NetworkConnection conn)
   {
      if (tc.isEntryEnabled()) SibTr.entry(this, tc, "<init>", conn);
      this.conn = conn;
      if (tc.isEntryEnabled()) SibTr.exit(tc, "<init>");
   }
   
   /**
    * This method tries to avoid creating a new instance of a CFWNetworkConnection object by seeing
    * if the specified virtual connection is the one that we are wrapping in the 
    * CFWNetworkConnection instance that created this context. If it is, we simply return that.
    * Otherwise we must create a new instance.
    * 
    * @param vc The virtual connection.
    * 
    * @return Returns a NetworkConnection instance that wraps the virtual connection.
    */
   protected NetworkConnection getNetworkConnectionInstance(VirtualConnection vc)
   {
      if (tc.isEntryEnabled()) SibTr.entry(this, tc, "getNetworkConnectionInstance", vc);
      
      NetworkConnection retConn = null;
      if (vc != null)
      {
         // Default to the connection that we were created from
         retConn = conn;
         
         if (vc != ((CFWNetworkConnection) conn).getVirtualConnection())
         {
            // The connection is different - nothing else to do but create a new instance
            retConn = new CFWNetworkConnection(vc);
         }
      }
      
      if (tc.isEntryEnabled()) SibTr.exit(this, tc, "getNetworkConnectionInstance", retConn);
      return retConn;
   }
}
