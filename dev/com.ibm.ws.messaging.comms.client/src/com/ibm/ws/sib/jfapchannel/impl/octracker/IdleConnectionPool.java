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

package com.ibm.ws.sib.jfapchannel.impl.octracker;

import java.util.HashMap;
import java.util.LinkedList;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.jfapchannel.JFapChannelConstants;
import com.ibm.ws.sib.jfapchannel.LinkLevelState;
import com.ibm.ejs.util.am.AlarmListener;
import com.ibm.ejs.util.am.AlarmManager;
import com.ibm.ws.sib.jfapchannel.framework.Framework;
import com.ibm.ws.sib.jfapchannel.impl.OutboundConnection;
import com.ibm.ws.sib.utils.RuntimeInfo;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * A pool of idle connections.  Connections are placed into this pool when the
 * outbound connection tracker no longer requires them.  When the tracker wants a new
 * connction, it checks this pool first before creating one.  After a certain amount of
 * time, connections that remain idle are removed from the pool and closed.
 */
public class IdleConnectionPool implements AlarmListener
{
   private static final TraceComponent tc = SibTr.register(IdleConnectionPool.class, JFapChannelConstants.MSG_GROUP, JFapChannelConstants.MSG_BUNDLE);

   // Reference to the sole instance of this singleton class.
   private static IdleConnectionPool instance = null;

   static
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "@(#) SIB/ws/code/sib.jfapchannel.client.common.impl/src/com/ibm/ws/sib/jfapchannel/impl/octracker/IdleConnectionPool.java, SIB.comms, WASX.SIB, uu1215.01 1.16");
      instance = new IdleConnectionPool();
   }

   // Default amount of time to keep a connection in the pool before closing it.
   private static final int DEFAULT_CONNECTION_CLOSE_TIMEOUT = 10000;

   // Used to store the actual amount of time that we will keep a connection in
   // the pool for.  By default, this gets set to DEFAULT_CONNECTION_CLOSE_TIMEOUT,
   // but can be adjusted via a runtime property.
   private int connectionCloseTimeout;

   // Maps endpoint descriptor -> connection list.  This is the primary structure
   // backing the pool.  It maps an EndPointDescriptor to a List of Connection objects.
   private final HashMap<EndPointDescriptor, LinkedList<Object[]>> descriptorToConnectionListMap =
      new HashMap<EndPointDescriptor, LinkedList<Object[]>>();

   /** Constructor.  Marked private as part of singleton pattern. */
   private IdleConnectionPool()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "<init>");

      // Set the connection close timeout based on a runtime info property.
      connectionCloseTimeout = DEFAULT_CONNECTION_CLOSE_TIMEOUT;
      try
      {
         connectionCloseTimeout = Integer.parseInt(RuntimeInfo.getProperty("com.ibm.ws.sib.jfapchannel.CONNECTION_CLOSE_TIMEOUT", ""+DEFAULT_CONNECTION_CLOSE_TIMEOUT));
      }
      catch(NumberFormatException nfe)
      {
         // No FFDC code needed
      }
      if (connectionCloseTimeout < 0) connectionCloseTimeout = 0;

      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "connection close timeout = "+connectionCloseTimeout);
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "<init>");
   }

   /**
    * Small switch class used to determin if an alarm is valid or not.  This is required
    * because there exists no way to cancel an alarm.  Instead we need to pass in this
    * object as part of the alarm context and invalidate it from another thread if we do
    * not want the alarm to be acted upon.
    */
   private static class AlarmValid
   {
      private boolean valid = true;
      protected synchronized boolean isValid()  { return valid;  }
      protected synchronized void invalidate()  { valid = false; }
   }

   /**
    * Adds an outbound connection into the idle connection pool.  This connection will
    * remain in the pool until, either, it is removed via the "remove" method or remains
    * idle long enough to be closed.
    * @see IdleConnectionPool#remove(EndPointDescriptor)
    * @param outboundConnection The connection to add to the pool.
    * @param descriptor The descriptor which describes the remote machine that the connection
    * is connected to.
    */
   public void add(OutboundConnection outboundConnection,
                   EndPointDescriptor descriptor)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "add", new Object[] {outboundConnection, descriptor});

      if (connectionCloseTimeout == 0)
      {
         // Optimisation - if the close timeout has been set to zero, then close
         // the connection immediately, rather than even trying to add it to the pool
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "close timeout == 0, closing immediately");
         outboundConnection.physicalClose(true);
      }
      else
      {
         // Add the connection into the pool and register and alarm with the alarm
         // manager so we know to close the connection if it remains idle.
         LinkedList<Object[]> connectionList = null;
         Object[] listEntry = null;

         // Take our own monitor to prevent anyone else updating the pool
         // while we carry out this operation.
         synchronized(this)
         {
            // See if other connections to the same endpoint have already
            // been added to the pool.
            connectionList =
               descriptorToConnectionListMap.get(descriptor);
            if (connectionList == null)
            {
               // No connections to the same end point, this is the first.
               connectionList = new LinkedList<Object[]>();
               if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "pool does not contain other connections for this endpoint, adding new list: "+connectionList);
               descriptorToConnectionListMap.put(descriptor, connectionList);
            }
            else if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            {
               // If debug trace is on, the dump out what connections relating to this
               // endpoint are already present in the pool.
               SibTr.debug(this, tc, "connections already present in list: "+connectionList+" are", connectionList.toArray());
            }
            AlarmValid alarmValid = new AlarmValid();

            // The entry placed into the list is a tuple (for which we use
            // a object array of size 2) consisting of outbound connection
            // and an object which is used to determine if the alarm we
            // are about to set is still valid.
            listEntry = new Object[]{outboundConnection, alarmValid};
            connectionList.addLast(listEntry);
         }

         // Set an alarm so that we can close the connection if it remains
         // idle for too long.
         AlarmManager alarmManager = Framework.getInstance().getAlarmManager();
         alarmManager.createDeferrable(connectionCloseTimeout,
                                       this,
                                       new Object[] {connectionList, listEntry});   // D197042
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "add");
   }

   /**
    * Attempts to remove a connection from the pool.  If a connection is available
    * for the specified endpoint descriptor, it is removed from the pool and returned.
    * If no suitable connection is present, a value of null is returened.
    * @param descriptor An endpoint descriptor which specifies the remote host to which
    * the connection returned must be connected.
    * @return A connection, if present in the pool, otherwise null.
    */
   public synchronized OutboundConnection remove(EndPointDescriptor descriptor)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "remove", descriptor);

      final LinkedList connectionList =  descriptorToConnectionListMap.get(descriptor);
      OutboundConnection connection = null;
      if ((connectionList != null) && (connectionList.size() > 0))
      {
         // Remove the first entry from the list.
         Object[] connEntry = (Object[])connectionList.removeFirst();
         connection = (OutboundConnection)connEntry[0];

         // Invalidate it's alarm, so that we don't try and close
         // the connection because it has been idle for too long.
         ((AlarmValid)connEntry[1]).invalidate();

         // Reset the link level state so that the state does not
         // survive the pooling process. This may be null if on z/OS and the
         //WMQRA or MEP function are the only users of the connection.
         final LinkLevelState lls = (LinkLevelState)connection.getAttachment();
         if(lls != null) lls.reset();
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "remove", connection);
      return connection;
   }

   /**
    * Returns the single instance of this class in existence.
    * @return IdleConnectionPool
    */
   public static IdleConnectionPool getInstance()
   {
      return instance;
   }

   /**
    * AlarmListener fired when a connection may have been idle for too long.
    * This listener is registered with the alarm manager every time a connection
    * is added to the pool.  When fired, it determines if the connection has
    * remained idle and if so closes it.
    * @see com.ibm.ejs.util.am.AlarmListener#alarm(java.lang.Object)
    */
   public void alarm(Object alarmContext)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "alarm", alarmContext);

      boolean alarmValid = false;
      boolean presentInConnectionList = false;
      Object[] listEntry = null;

      // Take the pools monitor so that no-one else can try and remove the
      // connection from the pool while we decide if we should close it or not.
      synchronized(this)
      {
         // The alarm context is a tuple (implemented as an object array of
         // size 2) containing the connection list to which the connection
         // belongs and another tuple.  This second tuple contains the connection
         // itself and an object which represents whether it is still valid to
         // close the connection.
         Object[] contextArray = (Object[])alarmContext;
         LinkedList connectionList = (LinkedList)contextArray[0];
         listEntry = (Object[])contextArray[1];
         alarmValid = ((AlarmValid)listEntry[1]).isValid();
         if (alarmValid)
            presentInConnectionList = connectionList.remove(listEntry);
      }

      // If we found (and removed) a connection suitable to be closed, then
      // close it.  As we did all the updates to shared structures in the
      // above synchronized block, we are safe to do the close without
      // holding any monitors.
      if (presentInConnectionList)
      {
         OutboundConnection connection = (OutboundConnection)listEntry[0];
         connection.physicalClose(true);
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "alarm");
   }
}
