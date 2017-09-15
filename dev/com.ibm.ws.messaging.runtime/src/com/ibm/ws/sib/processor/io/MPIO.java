/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.processor.io;

import java.util.HashMap;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.comms.MEConnection;
import com.ibm.ws.sib.comms.MEConnectionListener;
import com.ibm.ws.sib.comms.ProtocolVersion;
import com.ibm.ws.sib.mfp.AbstractMessage;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.exceptions.SIMPErrorException;
import com.ibm.ws.sib.processor.impl.MessageProcessor;
import com.ibm.ws.sib.processor.utils.Enumarray;
import com.ibm.ws.sib.processor.utils.LockManager;
import com.ibm.ws.sib.trm.contact.CommsErrorListener;
import com.ibm.ws.sib.trm.topology.LinkCellule;
import com.ibm.ws.sib.trm.topology.MessagingEngine;
import com.ibm.ws.sib.trm.topology.RoutingManager;
import com.ibm.ws.sib.trm.topology.TopologyListener;
import com.ibm.ws.sib.utils.SIBUuid8;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * This class manages all ME-ME I/O for the MP.
 */
public class MPIO
  implements TopologyListener,
             MEConnectionListener
{
  // Standard debug/trace gunk
  private static final TraceComponent tc =
    SibTr.register(
      MPIO.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);
  
  
  
  private static final TraceNLS nls =
    TraceNLS.getTraceNLS(SIMPConstants.RESOURCE_BUNDLE);

  // TRM implements the following interfaces:
  private CommsErrorListener _commsErrorListener;
  private RoutingManager _routingManager;
  // The MP RMR
  private RemoteMessageReceiver _remoteMessageReciever;

  // Map from connections to MPConnections
  private HashMap<MEConnection,MPConnection> _mpConnectionsByMEConnection
    = new HashMap<MEConnection,MPConnection>();
  private HashMap<SIBUuid8,MPConnection> _mpConnectionsByMEUuid
    = new HashMap<SIBUuid8,MPConnection>();

  /**
   * Used to start and stop the processing of messages from remote 
   * messaging engines.
   */
  private LockManager mpioLockManager = new LockManager();
  
  /** Start/stop flag for indicating whether we can process messages. */
  private boolean started;

  private MessageProcessor _messageProcessor;


  public MPIO(MessageProcessor messageProcessor)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "MPIO", messageProcessor);
    
    _messageProcessor = messageProcessor;    
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "MPIO", this);
  }
  
  /**
   * Initialize the MP I/O component.
   *
   * @param CEL TRM's implementation of the CommsErrorListener interface.
   * @param RM TRM's implementation of the RoutingManager interface.
   */
  public void init(CommsErrorListener CEL,
                   RoutingManager RM)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "init", new Object[] { CEL, RM});

    // Lock exclusively for start operations
    mpioLockManager.lockExclusive();
    _commsErrorListener = CEL;
    _routingManager = RM;
    
    synchronized(_mpConnectionsByMEConnection)
    {
      _mpConnectionsByMEUuid.clear();
      _mpConnectionsByMEConnection.clear();
    }
    
    // Create a new RemoteMessageReceiver if we don't already have one
    // or re-initialise the existing one to refresh any cached data
    // (used when an ME is re-started without the Server being re-started) 
    if(_remoteMessageReciever == null)
      _remoteMessageReciever = new RemoteMessageReceiver(_messageProcessor, this);
    else
      _remoteMessageReciever.init();
    
    started = true;    
    mpioLockManager.unlockExclusive();
          
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "init");
  }

  /**
   * Method to stop MPIO from processing any new messages.
   * This will take an exclusive lock on the lock manager and change
   * the started flag to false.
   *
   */
  public void stop()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "stop");
      
    // Lock exclusively for start operations
    mpioLockManager.lockExclusive();
    started = false;
    mpioLockManager.unlockExclusive();
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "stop"); 
  }

  /**
   * Receive a new Control message from an ME-ME connection.
   *
   * @param conn The physical connection on which the message was received.
   * @param msg The ControlMessage received.
   */
  public void receiveMessage(MEConnection conn, AbstractMessage aMessage)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "receiveMessage", new Object[] {conn,
                                                      aMessage,
                                                      "verboseMsg IN : " + aMessage.toVerboseString()});

    // Minimal comms trace of the message we've received
    if (TraceComponent.isAnyTracingEnabled()) {
      MECommsTrc.traceMessage(tc, 
          _messageProcessor, 
          aMessage.getGuaranteedSourceMessagingEngineUUID(), 
          MECommsTrc.OP_RECV, 
          conn, 
          aMessage);
    }

    // Take a read lock to ensure that we can't be stopped while processing a
    // message
    mpioLockManager.lock();    
    try
    {    
      if (started)
      {
        //pass the message on to the RMR
        _remoteMessageReciever.receiveMessage(aMessage);
      }
      else if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
      {
        SibTr.debug(tc, "Ignoring message as in stopped state");
      }
    }
    catch(Throwable e)
    {
      // Anything going wrong in Processor (or below, e.g. MsgStore) shouldn't
      // bring the ME-ME connection down. To get an exception through to here
      // we must have gone wrong somewhere, i.e. an APAR-kind of event. But even
      // so, that's no reason to knobble all ME-to-ME communication, so we swallow
      // any exception here, after spitting out an FFDC.
      
      FFDCFilter.processException(
          e,
          "com.ibm.ws.sib.processor.io.MPIO.receiveMessage",
          "1:228:1.32",
          new Object[] {this, aMessage, conn, _messageProcessor.getMessagingEngineName()});
      
      if (e instanceof Exception)
        SibTr.exception(tc, (Exception)e);
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        SibTr.debug(tc, "Exception occurred when processing a message " + e);
         
      // We're not allowed to swallow this exception - let it work its way back to
      // Comms
      if(e instanceof ThreadDeath)
      {     
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
          SibTr.exit(tc, "receiveMessage", e); 
        throw (ThreadDeath)e;
      }
    }
    finally
    {
      mpioLockManager.unlock();      
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "receiveMessage");
  }

  /**
   * Get a MPConnection from the cache and if there isn't one, create a new one and
   * put it in the cache.
   * 
   * @param cellule The cellule to find a MPConnection for (optional)
   * @param conn The MEConnection to find a MPConnection for
   * @return the MPConnection
   */
  public MPConnection getOrCreateNewMPConnection(SIBUuid8 remoteUuid,
                                                 MEConnection conn)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getOrCreateNewMPConnection", new Object[] {remoteUuid, conn });

    MPConnection mpConn;
    synchronized(_mpConnectionsByMEConnection)
    {
      //look up the connection in the cache
      mpConn = _mpConnectionsByMEConnection.get(conn);
      //if it is not in the cache
      if(mpConn == null)
      {
        //make sure we know the cellule
        if(remoteUuid == null)
        {
          remoteUuid = new SIBUuid8(conn.getMessagingEngine().getUuid());
        }
        
        //create a new MPConnection for this MEConnection
        mpConn = new MPConnection(this,conn,remoteUuid);
        //put it in the cache
        _mpConnectionsByMEConnection.put(conn, mpConn);
        _mpConnectionsByMEUuid.put(remoteUuid, mpConn);
      }      
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getOrCreateNewMPConnection", mpConn);
      
    //return the MPConnection
    return mpConn;
  }
  
  /**
   * Get a MPConnection from the cache
   * 
   * @param cellule The ME for which we want a connection to
   * @return the MPConnection
   */
  public MPConnection getMPConnection(SIBUuid8 remoteUuid)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getMPConnection", new Object[] { remoteUuid });

    MPConnection mpConn;
    synchronized(_mpConnectionsByMEConnection)
    {
      //look up the MPConnection based on the cellule
      mpConn = _mpConnectionsByMEUuid.get(remoteUuid);      
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getMPConnection", mpConn);
    
    //return the MPConnection
    return mpConn;
  }
  
  /**
   * Get a MPConnection from the cache
   * 
   * @param conn The MEConnection for which we want a MPConnection for
   * @return the MPConnection
   */
  public MPConnection getMPConnection(MEConnection conn)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getMPConnection", new Object[] { conn });

    MPConnection mpConn;
    synchronized(_mpConnectionsByMEConnection)
    {
      //look up the MPConnection based on the MEConnection
      mpConn = _mpConnectionsByMEConnection.get(conn);      
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getMPConnection", mpConn);
  
    return mpConn;
  }
  
  /**
   * Find a MPConnection which corresponds to a particular cellule (ME).
   * The MPConnection may have been cached from before or we may have
   * to ask TRM.
   * 
   * @param cellule The cellule to find a connection for
   * @return The corresponding MPConnection
   */
  public MPConnection findMPConnection(SIBUuid8 remoteUuid)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "findMPConnection", new Object[] { remoteUuid });

    //try to get an MPConnection from our cache
    MPConnection mpConn = getMPConnection(remoteUuid);
    //if we didn't have one in the cache then ask TRM
    if(remoteUuid!= null && mpConn == null && _routingManager != null)
    {
      //ask TRM for a MEConnection to the given cellule ... for JS1 there should only be 0 or 1
      MEConnection[] choices = _routingManager.listConnections(new MessagingEngine(remoteUuid));
      if(choices != null && choices.length > 0 && choices[0] != null)
      {
        if(choices.length > 1)
        {
          //for JS1, if we get back more than one possible connection to a cellule then
          //we are in trouble so throw an exception ... shouldn't happen!!
          SIMPErrorException e = new SIMPErrorException(
              nls.getFormattedMessage(
                "INTERNAL_MESSAGING_ERROR_CWSIP0005",
                new Object[] {
                  "com.ibm.ws.sib.processor.io.MPIO",
                  "1:378:1.32",
                  this},
                null));
          
          FFDCFilter.processException(
            e,
            "com.ibm.ws.sib.processor.io.MPIO.findMPConnection",
            "1:385:1.32",
            this);
            
          if(TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
             SibTr.exception(tc, e); 
          if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "findMPConnection");
          
          throw e;
        }
        
        //if we were given a new MEConnection to use ... find or create
        //a MPConnection for it
        mpConn = getOrCreateNewMPConnection(remoteUuid, choices[0]);
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "findMPConnection", mpConn);
    
    //return the MPConnection
    return mpConn;
  }

  /**
   * remove a MPConnection from the cache
   * 
   * @param conn The MEConnection of the MPConnection to be removed
   * @return The MPConnection which removed
   */
  public MPConnection removeConnection(MEConnection conn)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "removeConnection", new Object[] { conn });

    MPConnection mpConn;
    synchronized(_mpConnectionsByMEConnection)
    {
      //remove the MPConnection from the 'by MEConnection' cache
      mpConn = _mpConnectionsByMEConnection.remove(conn);
      if(mpConn != null)
      {
        //remove it from the 'by cellule' cache also
        _mpConnectionsByMEUuid.remove(mpConn.getRemoteMEUuid());
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "removeConnection", mpConn);
  
    return mpConn;
  }

  /**
   * Indicates an error has occurred on a connection.
   *
   * @param conn The connection on which the error occurred.
   * @param ex The code indicating the type of error (TBD).
   */
  public void error(MEConnection conn, Throwable ex)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "error", new Object[] { this, conn, ex});

    // This one goes straight to the CEL
    _commsErrorListener.error(conn, ex);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "error");
  }

  /////////////////////////////////////////////////////////////
  // TopologyListener methods
  /////////////////////////////////////////////////////////////

  /**
   * Indicates that a new set of cellules and/or RMEs are now reachable.
   *
   * @param celluleList The set of cellules which are now reachable (in
   * addition to those which were previously reachable).  May be a
   * zero-length array.
   * @param rmeList The set of RMEs which are now reachable (in addition
   * to those which were previously reachable).  May be a zero-length
   * array.
   */
  public void increaseInReachability(LinkCellule[] celluleList, MessagingEngine[] rmeList)
  {
    // Since we don't have an orphan queue any more this is a no-op
  }

  /**
   * Indicates that a set of cellules and/or RMEs are no longer reachable
   * on a particular MEConnection.
   *
   * @param conn The connection on which reachability has changed.
   * @param celluleList The set of cellules which are no longer reachable
   * (may be a zero-length array).
   * @param rmeList The set of RMEs which are no longer reachable (may be
   * a zero-length array).
   */
  public void decreaseInReachability(MEConnection conn, LinkCellule[] celluleList,
                                     MessagingEngine[] rmeList)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "decreaseInReachability", new Object[] { this, conn, new Enumarray(celluleList), new Enumarray(rmeList)});

    removeConnection(conn);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "decreaseInReachability");
  }


  /**
   * Indiciates a failed connection, a newly created connection, or a
   * connection swap.  If upConn is null, then connection "downConn"
   * has been removed from the system.  If downConn is null, then
   * connection "upConn" has been added to the system.  Otherwise,
   * "downConn" has been removed and replaced by "upConn".
   *
   * @param downConn A connection which has been removed from the system,
   * or null.
   * @param upConn A connection which has been added to the system, or
   * null.
   */
  public void changeConnection(MEConnection downConn, MEConnection upConn)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "changeConnection", new Object[] { this, downConn, upConn} );

    if(downConn != null)
    {
      //remove the connection which has gone down
      removeConnection(downConn);
    }
      
    // The new connection will be created when needed
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "changeConnection");
  }

  /**
   * Send a ControlMessage to one specific ME
   * 
   * @param aMessage The ControlMessage to be sent
   * @param priority The priority at which to send the message
   * @param targetME The ME to send the message to
   */
  public void sendToMe(SIBUuid8 targetME, int priority, AbstractMessage aMessage)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "sendToMe", new Object[] { aMessage, Integer.valueOf(priority), targetME });
  
    // find an appropriate MPConnection
    MPConnection firstChoice = findMPConnection(targetME);
  
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
      SibTr.debug(tc, "sendToMe", firstChoice);

    // Minimal comms trace of the message we're trying to send
    if (TraceComponent.isAnyTracingEnabled()) {
      MECommsTrc.traceMessage(tc, 
          _messageProcessor, 
          aMessage.getGuaranteedTargetMessagingEngineUUID(), 
          MECommsTrc.OP_SEND, 
          firstChoice, 
          aMessage);
    }
    
    if(firstChoice != null)
    {
      //send the message
      firstChoice.send(aMessage, priority);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "sendToMe");
  }

  
  /**
   * Send a control message to a list of MEs
   * 
   * @param jsMsg The message to be sent
   * @param priority The priority at which to send it
   * @param fromTo The MEs to send it to
   */
  public void sendDownTree(SIBUuid8[] targets, int priority, AbstractMessage cMsg)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "sendDownTree", new Object[] { this, cMsg, Integer.valueOf(priority), targets});
  
    // Select a set of connections, then annotate the message.
    int length  = targets.length;
    //the unique list of connections
    MPConnection[] send = new MPConnection[length];
    int[] cCount = new int[length];
    int numSendConnections = 0;
    
    next:
    for(int i=0; i<length; i++)
    {
      SIBUuid8 targetMEUuid = targets[i];
      MPConnection firstChoice = findMPConnection(targetMEUuid);
      
      // Minimal comms trace of the message we're trying to send
      if (TraceComponent.isAnyTracingEnabled()) {
        MECommsTrc.traceMessage(tc, 
            _messageProcessor, 
            cMsg.getGuaranteedTargetMessagingEngineUUID(), 
            MECommsTrc.OP_SEND, 
            firstChoice, 
            cMsg);
      }
      
      if(firstChoice != null)
      {
        // Keep track of the set of unique connections for sending below
        int j = 0;
        //loop through send until we find the next unused slot
        for(j=0; (j<i) && (send[j]!=null); j++)
        {          
          //if we have seen the selected connection before, start again
          if (send[j].equals(firstChoice))
          {                        
            cCount[j]++;
            continue next;
          }
        }
        if(j+1 > numSendConnections) numSendConnections = (j+1);
        //store the select connection in the chosen send slot
        send[j] = firstChoice;
        cCount[j]++;
      }
    }
  
    for(int i=0; i<numSendConnections; i++)
    {
      if (send[i] != null) send[i].send(cMsg,priority);
    }
  
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "sendDownTree");
  }

  /////////////////////////////////////////////////////////////
  // Utility methods
  /////////////////////////////////////////////////////////////
  
  /**
   * Test whether or not a particular ME is reachable.
   * 
   * @param me The ME to test.
   * @return True if TRM has a path to this ME, false otherwise.
   */
  public boolean isMEReachable(SIBUuid8 meUuid)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "isMEReachable", new Object[] {this, meUuid});
      
    boolean result = (findMPConnection(meUuid) != null);    
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "isMEReachable", Boolean.valueOf(result));
      
    return result;
  }

  /**
   * Test whether or not a particular ME is of a version compatible with this one.
   * 
   * @param me The ME to test.
   * @return True if ME is of a compatible version
   */
  public boolean isCompatibleME(SIBUuid8 meUuid, ProtocolVersion version) 
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "isCompatibleME", new Object[] {meUuid});
      
    boolean result = false;
    MPConnection conn = findMPConnection(meUuid);    
    if (conn!=null)
    {
      // If the other ME is an older version then we are incompatible
      ProtocolVersion otherVersion = conn.getVersion();
      if (otherVersion != null && otherVersion.compareTo(version) >= 0)
        result = true;      
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "isCompatibleME", Boolean.valueOf(result));
      
    return result;
  }

  // Can potentially block for up to 5 seconds
  public void forceConnect(SIBUuid8 meUuid) 
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "forceConnect", meUuid);
    
    if(_routingManager!=null)
      _routingManager.connectToME(meUuid);
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "forceConnect");
  }
}
