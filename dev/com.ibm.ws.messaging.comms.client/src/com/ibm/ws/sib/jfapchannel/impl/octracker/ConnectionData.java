/*******************************************************************************
 * Copyright (c) 2004, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.jfapchannel.impl.octracker;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.jfapchannel.JFapChannelConstants;
import com.ibm.ws.sib.jfapchannel.impl.OutboundConnection;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * A wrapper for an outbound connection which augments it with additional tracking information.
 * This class is used in conjunction with the outbound connection tracker to store additional
 * information about an outbound connection.
 */
public class ConnectionData
{
   private static final TraceComponent tc = SibTr.register(ConnectionData.class, JFapChannelConstants.MSG_GROUP, JFapChannelConstants.MSG_BUNDLE);

   static   
   {
      if (tc.isDebugEnabled()) SibTr.debug(tc, "@(#) SIB/ws/code/sib.jfapchannel.client.common.impl/src/com/ibm/ws/sib/jfapchannel/impl/octracker/ConnectionData.java, SIB.comms, WASX.SIB, uu1215.01 1.6");
   }
     
   // The outbound connection this class holds data on.
   private OutboundConnection outboundConnection;
   
   // How many conversations are using this connection.
   private int useCount;
   
   // The group to which this connection belongs.
   private ConnectionDataGroup group;
         
   // The endpoint descriptor relating to the remote process this connection is connected to.
   private EndPointDescriptor endPointDescriptor;
   
   /**
    * Creates a new connection data object.
    * @param group The group this connection is associated with.
    * @param endPointDescriptor The end point descriptor this connection is associated with.
    */
   public ConnectionData(ConnectionDataGroup group, EndPointDescriptor endPointDescriptor)
   {
      if (tc.isEntryEnabled()) SibTr.entry(this, tc, "<init>", new Object[]{group, endPointDescriptor});
      this.group = group;
      this.endPointDescriptor = endPointDescriptor;
      if (tc.isEntryEnabled()) SibTr.exit(this, tc, "<init>");
   }
   
   /**
    * Returns the current use count for this connection.
    * @return The current use count.
    */
   protected int getUseCount()
   {
      return useCount;
   }
   
   /**
    * Associates a connection with this connection data object.
    * @param connection The connection to associate with this connection data object.
    */
   protected void setConnection(OutboundConnection connection)
   {
      outboundConnection = connection;
   }
   
   /**
    * Returns the connection associated with this connection data object.
    * @return The connection associated with this connection data object.
    */
   protected OutboundConnection getConnection()
   {
      return outboundConnection;
   }
   
   /**
    * Increments the use count for this connection.  This is done to signify an
    * additional conversation is using the connection.
    */
   protected void incrementUseCount()
   {
      ++useCount;
   }

   /**
    * Decrements the use count for this connection.  This is done to signify a
    * conversation using the connection has closed.
    */
   protected void decrementUseCount()
   {
      --useCount;
   }
   
   /**
    * Returns the end point descriptor associated with this connection.
    * @return The end point descriptor associated with this connection.
    */
   protected EndPointDescriptor getEndPointDescriptor()
   {
      return endPointDescriptor;
   }
   
   /**
    * Returns the connection data group associated with this connection.
    * @return The connection data group associated with this connection.
    */
   protected ConnectionDataGroup getConnectionDataGroup()
   {
      return group;
   }
}
