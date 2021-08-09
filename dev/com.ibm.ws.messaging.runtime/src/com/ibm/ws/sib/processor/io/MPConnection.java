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

import java.io.UnsupportedEncodingException;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.comms.ConnectionMetaData;
import com.ibm.ws.sib.comms.MEConnection;
import com.ibm.ws.sib.comms.ProtocolVersion;
import com.ibm.ws.sib.mfp.AbstractMessage;
import com.ibm.ws.sib.mfp.IncorrectMessageTypeException;
import com.ibm.ws.sib.mfp.MessageCopyFailedException;
import com.ibm.ws.sib.mfp.MessageEncodeFailedException;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.utils.SIBUuid8;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException;
import com.ibm.wsspi.sib.core.exception.SIConnectionLostException;
import com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException;

/**
 * An MPConnection wraps an MEConnection, caching information such as
 * the cellule and the lowestPriorityWithCapacity
 */
public class MPConnection
{
  // Standard debug/trace gunk
  private static final TraceComponent tc =
    SibTr.register(
      MPConnection.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);
   
  private MEConnection connection;
  private SIBUuid8 remoteUuid;
  private MPIO mpio;

  /**
   * Create a new MPConnection
   * 
   * @param mpio The parent MPIO
   * @param connection The underlying MEConnection
   * @param cellule The cellule which this is a connection to
   */
  public MPConnection(MPIO mpio,
                      MEConnection connection,
                      SIBUuid8 remoteUuid)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "MPConnection", new Object[] { mpio, connection, remoteUuid });
      
    this.mpio = mpio;
    this.connection = connection;
    this.remoteUuid = remoteUuid;
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "MPConnection", this);
  }
    
  /**
   * If comms has capacity, then send the encoded message data. Otherwise, throw the message
   * away.
   * 
   * @param messageData The encoded message data
   * @param priority the priority at which to send the message
   * @param isControl true if the message is a control message
   */
  void send(AbstractMessage aMessage, int priority)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "send", new Object[] {this,
                                            aMessage,
                                            new Integer(priority),
                                            "verboseMsg OUT : " + aMessage.toVerboseString()});
    if(TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
      MPIOMsgDebug.debug(tc, aMessage, priority);
    
    // comms accepting messages, write the message
    // immediately.
    try
    {
      //send the encoded message
      connection.send(aMessage, priority);          

      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        SibTr.debug(tc, "send", "Message accepted by Comms");
    } 
    catch (SIConnectionDroppedException e)
    {
      // No FFDC code needed
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        SibTr.debug(tc, "send", "Message refused by Comms: Connection Dropped");
      
      // Pass this exception up to our parent.  Note that we'll assume
      // we're still alive, and let our parent (in coordination with TRM
      // decide whether or not to nuke us).
      mpio.error(connection, e);
    }       
    catch (SIConnectionUnavailableException e)
    {
      // No FFDC code needed
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        SibTr.debug(tc, "send", "Message refused by Comms: Connection Unavailable");
      
      // Pass this exception up to our parent.  Note that we'll assume
      // we're still alive, and let our parent (in coordination with TRM
      // decide whether or not to nuke us).
      mpio.error(connection, e);
    } 
    catch (SIConnectionLostException e)
    {
      // No FFDC code needed
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        SibTr.debug(tc, "send", "Message refused by Comms: Connection Lost");
      
      // Pass this exception up to our parent.  Note that we'll assume
      // we're still alive, and let our parent (in coordination with TRM
      // decide whether or not to nuke us).
      mpio.error(connection, e);
    }
    catch(MessageEncodeFailedException e)
    {
      // No FFDC code needed
      //this should never happen, but comms declare the exception, so
      //we have to deal with it
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        SibTr.debug(tc, "send", "Message refused by Comms: Msg Encode Failed");
      
      // Pass this exception up to our parent.  Note that we'll assume
      // we're still alive, and let our parent (in coordination with TRM
      // decide whether or not to nuke us).
      mpio.error(connection, e);
    }
    catch(MessageCopyFailedException e)
    {
      // No FFDC code needed
      //this should never happen, but comms declare the exception, so
      //we have to deal with it
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        SibTr.debug(tc, "send", "Message refused by Comms: Msg Copy Failed");
      
      // Pass this exception up to our parent.  Note that we'll assume
      // we're still alive, and let our parent (in coordination with TRM
      // decide whether or not to nuke us).
      mpio.error(connection, e);
    }
    catch(UnsupportedEncodingException e)
    {
      // No FFDC code needed
      //this should never happen, but comms declare the exception, so
      //we have to deal with it
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        SibTr.debug(tc, "send", "Message refused by Comms: Msg Encoding Failed");
      
      // Pass this exception up to our parent.  Note that we'll assume
      // we're still alive, and let our parent (in coordination with TRM
      // decide whether or not to nuke us).
      mpio.error(connection, e);
    }
    catch(IncorrectMessageTypeException e)
    {
      // No FFDC code needed
      //this should never happen, but comms declare the exception, so
      //we have to deal with it
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        SibTr.debug(tc, "send", "Message refused by Comms: Incorrect Message Type");
      
      // Pass this exception up to our parent.  Note that we'll assume
      // we're still alive, and let our parent (in coordination with TRM
      // decide whether or not to nuke us).
      mpio.error(connection, e);
    }
      
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "send");
  }
  
  public boolean equals(Object conn)
  {
    if(conn != null && conn instanceof MPConnection)
    {
      MPConnection mpConn = (MPConnection) conn;
      return mpConn.connection.equals(connection);
    }
        
    return false;    
  }
  
  public int hashCode()
  {
    return connection.hashCode();    
  }
  
  public SIBUuid8 getRemoteMEUuid()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getRemoteMEUuid");
      SibTr.exit(tc, "getRemoteMEUuid", remoteUuid);
    }
    return remoteUuid;
  }

  /**
   * Retrieve the ProtocolVersion associated with this connection.
   * 
   * @return the version of the connection protocol.
   */
  public ProtocolVersion getVersion() 
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getVersion");
    
    // The ProtocolVersion to be returned
    ProtocolVersion version = ProtocolVersion.UNKNOWN;
    // Get the MetaData out of the connection
    ConnectionMetaData connMetaData = connection.getMetaData();
    
    // If the MetaData is non-null we can retrieve a version.
    if(connMetaData != null)
      version = connMetaData.getProtocolVersion();
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getVersion", version);
  
    return version;
  }
}
