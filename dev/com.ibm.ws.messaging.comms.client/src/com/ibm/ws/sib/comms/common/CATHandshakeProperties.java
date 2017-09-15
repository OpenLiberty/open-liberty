/*******************************************************************************
 * Copyright (c) 2004, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.comms.common;

import java.util.Hashtable;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.comms.CommsConstants;
import com.ibm.ws.sib.jfapchannel.HandshakeProperties;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * This class encapsulates all the properties that are negotiated during and initial handshake.
 * <p>
 * Note that some setter methods are not traced - this is because they are set during initial 
 * handshake and so it makes the trace neater if they are just debugged by the calling code as
 * well as this class.
 * 
 * @author Mike Schmitt
 */
public class CATHandshakeProperties implements HandshakeProperties
{
   private static final long serialVersionUID = -8426255634661083716L;

   private static final String MAJOR_VERSION      = "MAJOR_VERSION";
   private static final String MINOR_VERSION      = "MINOR_VERSION";   
   private static final String FAP_LEVEL          = "FAP_LEVEL";
   private static final String MAX_MESSAGE_SIZE   = "MAX_MESSAGE_SIZE";
   private static final String HEARTBEAT_INTERVAL = "HEARTBEAT_INTERVAL";
   private static final String CAPABILTIES        = "CAPABILITIES";
   private static final String MAX_TRANS_SIZE     = "MAX_TRANS_SIZE";
   private static final String PEER_PRODUCT_ID    = "PEER_PRODUCT_ID";
   private static final String HEARTBEAT_TIMEOUT  = "HEARTBEAT_TIMEOUT";
   private static final String REMOTE_CELL_NAME   = "REMOTE_CELL_NAME";
   private static final String REMOTE_NODE_NAME   = "REMOTE_NODE_NAME";
   private static final String REMOTE_SERVER_NAME = "REMOTE_SERVER_NAME";
   private static final String REMOTE_CLUSTER_NAME  = "REMOTE_CLUSTER_NAME";
   
   /**
    * Register Class with Trace Component
    */
   private static final TraceComponent tc = SibTr.register(CATHandshakeProperties.class,
                                                           CommsConstants.MSG_GROUP,
                                                           CommsConstants.MSG_BUNDLE);

   /**
    * Log Source code level on static load of class
    */
   static 
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Source info: @(#)SIB/ws/code/sib.comms.client.impl/src/com/ibm/ws/sib/comms/common/CATHandshakeProperties.java, SIB.comms, WASX.SIB, uu1215.01 1.21");
   }
   
   /** Our hashtable of properties */
   private Hashtable<String, Object>properties = null;
   
   /**
    * Constructor.
    */
   public CATHandshakeProperties()
   {
      properties = new Hashtable<String, Object>(9);   // Default initial size of the params
                                                       // for CatHandshakeProperties
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "<init>");
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "<init>");
   }
   
   /**
    * Sets the product minor version.
    * 
    * @param productVersion
    */
   public void setMinorVersion(short productVersion)
   {
      properties.put(MINOR_VERSION, productVersion);
   }
   
   /**
    * @return Returns the minor product version.
    */
   public short getMinorVersion()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getMinorVersion");
      short minor = ((Short)properties.get(MINOR_VERSION)).shortValue();
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getMinorVersion", ""+minor);
      return minor;                  
   }
   
   /**
    * Sets the major version.
    * 
    * @param productVersion
    */
   public void setMajorVersion(short productVersion)
   {
      properties.put(MAJOR_VERSION,productVersion);
   }
   
   /**
    * @return Returns the major product version.
    */
   public short getMajorVersion()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getMajorVersion");
      short major = ((Short)properties.get(MAJOR_VERSION)).shortValue();
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getMajorVersion", ""+major);
      return major;                  
   }   
   
   /**
    * Sets the current FAP level.
    * 
    * @param fapLevel
    */
   public void setFapLevel(short fapLevel)
   {
      properties.put(FAP_LEVEL, fapLevel);
   }
   
   /**
    * @return Returns the current FAP level.
    */
   public short getFapLevel()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getFapLevel");
      short fapLevel = ((Short)properties.get(FAP_LEVEL)).shortValue();
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getFapLevel", ""+fapLevel);
      return fapLevel;
   } 
   
   /**
    * @return true, if the FAP_LEVEL has been set in the properties.
    */
   protected boolean isFapLevelKnown()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "isFapLevelKnown");
      final boolean result = properties.contains(FAP_LEVEL);
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "isFapLevelKnown", result);
      return result;
   }
   
   /**
    * Sets the max message size.
    * 
    * @param maxMessageSize
    */
   public void setMaxMessageSize(long maxMessageSize)
   {
      properties.put(MAX_MESSAGE_SIZE, maxMessageSize);
   }
   
   /**
    * @return Returns the max message size.
    */
   public long getMaxMessageSize() 
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getMaxMessageSize");
      long maxMessageSize = ((Long)properties.get(MAX_MESSAGE_SIZE)).longValue();
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getMaxMessageSize", ""+maxMessageSize);
      return maxMessageSize;
   }   

   /**
    * Sets the heartbeat interval.
    * 
    * @param heartbeatInterval
    */
   public void setHeartbeatInterval(short heartbeatInterval)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "setHeartbeatInterval");
      properties.put(HEARTBEAT_INTERVAL, heartbeatInterval);
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "setHeartbeatInterval", ""+heartbeatInterval);
   }
   
   /**
    * @return Returns the heartbeat interval.
    */
   public short getHeartbeatInterval()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getHeartbeatInterval");
      short interval = ((Short)properties.get(HEARTBEAT_INTERVAL)).shortValue();
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getHeartbeatInterval", ""+interval);
      return interval;            
   }
   
   /**
    * Sets the capabilities
    * 
    * @param capabilities
    */
   public void setCapabilites(short capabilities)
   {
      properties.put(CAPABILTIES, capabilities);
   }
   
   /**
    * @return Returns the capabilities.
    */
   public short getCapabilites()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getCapabilites");
      short capabilities = ((Short)properties.get(CAPABILTIES)).shortValue();
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getCapabilites", ""+capabilities);
      return capabilities;            
   }

   /**
    * Sets the max transmission size.
    * 
    * @param maxTransmissionSize
    */
   public void setMaxTransmissionSize(int maxTransmissionSize)
   {
      properties.put(MAX_TRANS_SIZE, maxTransmissionSize);
   }
   
   /**
    * @return Returns the max transmission size.
    */
   public int getMaxTransmissionSize()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getMaxTransmissionSize");
      int maxTxSize = ((Integer)properties.get(MAX_TRANS_SIZE)).intValue();
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getMaxTransmissionSize", ""+maxTxSize);
      return maxTxSize;            
   }      
   
   /**
    * Sets the peer product Id.
    * 
    * @param productId
    */
   public void setPeerProductId(short productId)
   {
      properties.put(PEER_PRODUCT_ID, productId);
   }
   
   /**
    * @return Returns the peer product id.
    */
   public short getPeerProductId()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getPeerProductId");
      short productId = ((Short)properties.get(PEER_PRODUCT_ID)).shortValue();
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getPeerProductId", ""+productId);
      return productId;
   }
   
   /**
    * Sets the heartbeat timeout.
    * 
    * @param heartbeatTimeout
    */
   public void setHeartbeatTimeout(short heartbeatTimeout)
   {
      properties.put(HEARTBEAT_TIMEOUT, heartbeatTimeout);
   }
   
   /**
    * @return Returns the heartbeat timeout.
    */
   public short getHeartbeatTimeout()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getHeartbeatTimeout");
      short timeout = ((Short)properties.get(HEARTBEAT_TIMEOUT)).shortValue();
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getHeartbeatTimeout", ""+timeout);
      return timeout;            
   }
   
   /**
    * @return Returns info about this object.
    * 
    * @see java.lang.Object#toString()
    */
   public String toString()
   {
      return "CATHandshakeProperties@" + Integer.toHexString(System.identityHashCode(this)) +
             ": " + properties;
   }

   /**
    * @see HandshakeProperties#getRemoteCellName()
    */
   public String getRemoteCellName() 
   {
      if(TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getRemoteCellName");
      final String remoteCellName = (String)properties.get(REMOTE_CELL_NAME);
      if(TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getRemoteCellName", remoteCellName);
      return remoteCellName;
   }
   
   /**
    * Sets the remote cell name.
    * 
    * @param remoteCellName
    */
   public void setRemoteCellName(String remoteCellName)
   {
      properties.put(REMOTE_CELL_NAME, remoteCellName);
   }

   /**
    * @see HandshakeProperties#getRemoteNodeName()
    */
   public String getRemoteNodeName() 
   {
      if(TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getRemoteNodeName");
      final String remoteNodeName = (String)properties.get(REMOTE_NODE_NAME);
      if(TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getRemoteNodeName", remoteNodeName);
      return remoteNodeName;
   }
   
   /**
    * Sets the remote node name.
    * 
    * @param remoteNodeName
    */
   public void setRemoteNodeName(String remoteNodeName)
   {
      properties.put(REMOTE_NODE_NAME, remoteNodeName);
   }

   /**
    * @see HandshakeProperties#getRemoteServerName()
    */
   public String getRemoteServerName() 
   {
      if(TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getRemoteServerName");
      final String remoteServerName = (String)properties.get(REMOTE_SERVER_NAME);
      if(TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getRemoteServerName", remoteServerName);
      return remoteServerName;
   }

   /**
    * Sets the remote server name.
    * 
    * @param remoteServerName
    */
   public void setRemoteServerName(String remoteServerName)
   {
      properties.put(REMOTE_SERVER_NAME, remoteServerName);
   }
   
   /**
    * @see HandshakeProperties#getRemoteClusterName()
    */
   public String getRemoteClusterName() 
   {
      if(TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getRemoteClusterName");
      final String remoteClusterName = (String)properties.get(REMOTE_CLUSTER_NAME);
      if(TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getRemoteClusterName", remoteClusterName);
      return remoteClusterName;
   }
   
   /**
    * Sets the remote cluster name.
    * 
    * @param remoteClusterName
    */
   public void setRemoteClusterName(String remoteClusterName)
   {
      properties.put(REMOTE_CLUSTER_NAME, remoteClusterName);
   }
}
