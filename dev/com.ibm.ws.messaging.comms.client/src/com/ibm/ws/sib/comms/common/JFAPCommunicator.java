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
package com.ibm.ws.sib.comms.common;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.exception.SIDataGraphException;
import com.ibm.websphere.sib.exception.SIDataGraphFormatMismatchException;
import com.ibm.websphere.sib.exception.SIDataGraphSchemaNotFoundException;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.websphere.sib.exception.SIIncorrectCallException;
import com.ibm.websphere.sib.exception.SIMessageDomainNotSupportedException;
import com.ibm.websphere.sib.exception.SIMessageException;
import com.ibm.websphere.sib.exception.SINotPossibleInCurrentConfigurationException;
import com.ibm.websphere.sib.exception.SINotSupportedException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.comms.CommsConnection;
import com.ibm.ws.sib.comms.CommsConstants;
import com.ibm.ws.sib.jfapchannel.ConnectionInterface;
import com.ibm.ws.sib.jfapchannel.Conversation;
import com.ibm.ws.sib.jfapchannel.ConversationUsageType;
import com.ibm.ws.sib.jfapchannel.JFapChannelConstants;
import com.ibm.ws.sib.jfapchannel.ReceivedData;
import com.ibm.ws.sib.jfapchannel.Conversation.ThrottlingPolicy;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.exception.SIAuthenticationException;
import com.ibm.wsspi.sib.core.exception.SICommandInvocationFailedException;
import com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException;
import com.ibm.wsspi.sib.core.exception.SIConnectionLostException;
import com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException;
import com.ibm.wsspi.sib.core.exception.SIDestinationLockedException;
import com.ibm.wsspi.sib.core.exception.SIDiscriminatorSyntaxException;
import com.ibm.wsspi.sib.core.exception.SIDurableSubscriptionAlreadyExistsException;
import com.ibm.wsspi.sib.core.exception.SIDurableSubscriptionMismatchException;
import com.ibm.wsspi.sib.core.exception.SIDurableSubscriptionNotFoundException;
import com.ibm.wsspi.sib.core.exception.SIInsufficientDataForFactoryTypeException;
import com.ibm.wsspi.sib.core.exception.SIInvalidDestinationPrefixException;
import com.ibm.wsspi.sib.core.exception.SILimitExceededException;
import com.ibm.wsspi.sib.core.exception.SIMessageNotLockedException;
import com.ibm.wsspi.sib.core.exception.SINotAuthorizedException;
import com.ibm.wsspi.sib.core.exception.SIRollbackException;
import com.ibm.wsspi.sib.core.exception.SISelectorSyntaxException;
import com.ibm.wsspi.sib.core.exception.SISessionDroppedException;
import com.ibm.wsspi.sib.core.exception.SISessionUnavailableException;
import com.ibm.wsspi.sib.core.exception.SITemporaryDestinationNotFoundException;

/**
 * The JFAPCommunicator is a thin wrapper around the basic JFap channel functions offered to us at
 * a Conversation level. This is an abstract class which is implemented differently on both the
 * client and the server.
 *
 * @author Niall
 */
public abstract class JFAPCommunicator
{
   /** Class name for FFDC's */
   private static String CLASS_NAME = JFAPCommunicator.class.getName();

   /** Register Class with Trace Component */
   private static final TraceComponent tc = SibTr.register(JFAPCommunicator.class, 
                                                           CommsConstants.MSG_GROUP, 
                                                           CommsConstants.MSG_BUNDLE);

   /** The NLS reference */
   private static final TraceNLS nls = TraceNLS.getTraceNLS(CommsConstants.MSG_BUNDLE);
   
   /** Name of the WAS cell associated with this process if it is running in a server environment.*/
   private static final String cellName;
   
   /** Length of cellName when added to a CommsByteBuffer. */
   private static final int cellNameLengthInBuffer;
   
   /** Name of the WAS node associated with this process if it is running in a server environment.*/
   private static final String nodeName;
   
   /** Length of nodeName when added to a CommsByteBuffer. */
   private static final int nodeNameLengthInBuffer;
   
   /** Name of the WAS server associated with this process if it is running in a server environment.*/
   private static final String serverName;
   
   /** Length of serverName when added to a CommsByteBuffer. */
   private static final int serverNameLengthInBuffer;
   
   /** Name of the WAS cluster associated with this process if it is running in a clustered server environment.*/
   private static final String clusterName;
   
   /** Length of clusterName when added to a CommsByteBuffer. */
   private static final int clusterNameLengthInBuffer;
   
   /** Log Source code level on static load of class */
   static
   {
	  
	  //Liberty COMMS change: No more cell/node/cluster/server names. Hence putting them as null
	  // it is harmless.
      cellName = null;
      nodeName = null;
      serverName = null;
      clusterName = null;
      
      
      //Calculate these up front as they will never change.
      cellNameLengthInBuffer = CommsByteBuffer.calculateEncodedStringLength(cellName);
      nodeNameLengthInBuffer = CommsByteBuffer.calculateEncodedStringLength(nodeName);
      serverNameLengthInBuffer = CommsByteBuffer.calculateEncodedStringLength(serverName);
      clusterNameLengthInBuffer = CommsByteBuffer.calculateEncodedStringLength(clusterName);
      
      //Dump out location information.
      if(TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
      {
         SibTr.debug(tc, "cellName: " + cellName);
         SibTr.debug(tc, "cellNameLengthInBuffer: " + cellNameLengthInBuffer);
         SibTr.debug(tc, "nodeName: " + nodeName);
         SibTr.debug(tc, "nodeNameLengthInBuffer: " + nodeNameLengthInBuffer);
         SibTr.debug(tc, "serverName: " + serverName);
         SibTr.debug(tc, "serverNameLengthInBuffer: " + serverNameLengthInBuffer);
         SibTr.debug(tc, "clusterName: " + clusterName);
         SibTr.debug(tc, "clusterNameLengthInBuffer: " + clusterNameLengthInBuffer);
      }
   }

   /** JFAP Conversation */
   private Conversation con = null;
   
   /**
    * @return Returns the reference to the Conversation with the ME
    */
   protected Conversation getConversation()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getConversation");
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getConversation", con);
      return con;
   }

   /**
    * Sets the Conversation object
    *
    * @param conversation
    */
   protected void setConversation(Conversation conversation)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "setConversation", conversation);
      con = conversation;
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "setConversation");
   }

   /**
    * @return Returns the Connection ID referring to the SICoreConnection
    *         Object on the Server
    */
   protected abstract int getConnectionObjectID();

   /**
    * Sets the Connection ID referring to the SICoreConnection Object
    * on the server
    *
    * @param i
    */
   protected abstract void setConnectionObjectID(int i);

   /**
    * @return Returns the CommsConnection associated with this conversation
    */
   protected abstract CommsConnection getCommsConnection();
   
   /**
    * Sets the CommsConnection associated with this Conversation
    *
    * @param cc
    */
   protected abstract void setCommsConnection(CommsConnection cc);
   
   /**
    * @return Returns a comms byte buffer for use.
    */
   protected abstract CommsByteBuffer getCommsByteBuffer();
   
   /**
    * @return Returns a Conversation wide unique request number.
    */
   protected abstract int getRequestNumber();
   
   /**
    * @param rcvData
    * @return Returns a comms byte buffer that wraps the received data.
    */
   protected CommsByteBuffer getCommsByteBuffer(ReceivedData rcvData)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "getCommsByteBuffer", rcvData);
      CommsByteBuffer buff = getCommsByteBuffer();
      buff.reset(rcvData);
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "getCommsByteBuffer", buff);
      return buff;
   }
   
   /**
    * Wraps the JFAP Channel exchange method to allow tracing, retrieval
    * of Unique request numbers and setting of message priority.
     *
    * @param buffer
    * @param sendSegmentType
    * @param priority
    * @param canPoolOnReceive
    *
    * @return CommsByteBuffer
    *
    * @throws SIConnectionDroppedException
    * @throws SIConnectionLostException
    */
   protected CommsByteBuffer jfapExchange(CommsByteBuffer buffer,
                                          int sendSegmentType, 
                                          int priority,
                                          boolean canPoolOnReceive)
   throws SIConnectionDroppedException, SIConnectionLostException
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "jfapExchange", 
                                           new Object[]{buffer, sendSegmentType, priority, canPoolOnReceive});

      boolean success = false;
      CommsByteBuffer rcvBuffer = null;
      try
      {
          if (buffer == null)
          {
             // The data list cannot be null
             SIErrorException e = new SIErrorException(
                nls.getFormattedMessage("NULL_DATA_LIST_PASSED_IN_SICO1046", null, null)
             );
    
             FFDCFilter.processException(e, CLASS_NAME + ".JFAPExchange",
                                         CommsConstants.JFAPCOMMUNICATOR_EXCHANGE_01, this);
    
             if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, e.getMessage(), e);
             throw e;
          }
    
          int reqNum = getRequestNumber();
    
          if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
          {
             SibTr.debug(this, tc, "About to Exchange segment " 
                             + "conversation: "+ con + " "
                             + JFapChannelConstants.getSegmentName(sendSegmentType)
                             + " - " + sendSegmentType
                             + " (0x" + Integer.toHexString(sendSegmentType) + ") "
                             + "using request number "
                             + reqNum);
             
             SibTr.debug(this, tc, con.getFullSummary());
          }
    
          ReceivedData rd = con.exchange(buffer, sendSegmentType, reqNum, priority, canPoolOnReceive);
          rcvBuffer = getCommsByteBuffer(rd);
    
          int rcvSegmentType = rcvBuffer.getReceivedDataSegmentType();
          if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
             SibTr.debug(this, tc, "Exchange completed successfully. " 
                             + "Segment returned "
                             + JFapChannelConstants.getSegmentName(rcvSegmentType)
                             + " - " + rcvSegmentType
                             + " (0x" + Integer.toHexString(rcvSegmentType) + ")");
          success = true;
      }

      finally 
      {
        if (!success && TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Exchange failed.");
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "jfapExchange", rcvBuffer);
      }
      
      return rcvBuffer;
   }

   /**
    * Wraps the JFAP Channel send method to allow tracing, retrieval
    * of Unique request numbers and setting of message priority.
    *
    * @param sendSegType
    * @param priority
    * @param canPoolOnReceive
    *
    * @throws SIConnectionDroppedException
    * @throws SIConnectionLostException
    */
   protected void jfapSend(CommsByteBuffer buffer,
                           int sendSegType,
                           int priority,
                           boolean canPoolOnReceive,
                           ThrottlingPolicy throttlingPolicy)
      throws SIConnectionDroppedException,
             SIConnectionLostException
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "jfapSend",
                                           new Object[]{buffer, sendSegType, priority, canPoolOnReceive});

      if (buffer == null)
      {
         // The data list cannot be null
         SIErrorException e = new SIErrorException(
            nls.getFormattedMessage("NULL_DATA_LIST_PASSED_IN_SICO1046", null, null)
         );

         FFDCFilter.processException(e, CLASS_NAME + ".JFAPSend",
                                     CommsConstants.JFAPCOMMUNICATOR_SEND_01, this);

         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, e.getMessage(), e);

         throw e;
      }

      int reqNum = 0;

      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
      {
         SibTr.debug(this, tc, "About to Send segment " 
                         + "conversation: "+ con + " " 
                         + JFapChannelConstants.getSegmentName(sendSegType)
                         + " - " + sendSegType 
                         + " (0x" + Integer.toHexString(sendSegType) + ") "
                         + "using request number "
                         + reqNum);
         
         SibTr.debug(this, tc, con.getFullSummary());
      }

      con.send(buffer, sendSegType, reqNum, priority, canPoolOnReceive, throttlingPolicy, null);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "jfapSend");
   }
   
   /**
    * This method will create the conversation state overwritting anything previously
    * stored.
    */
   protected abstract void createConversationState();
   
   /**
    * Exchanges handshake data with the server.
    * <p>
    * This is the initial load of data that is sent to the server. In this
    * handshake we send up versions and some basic settings.
    * <p>
    * The server may or may not reply with different values. If the server replies
    * with a different value this value is taken.
    *
    * @throws SIConnectionLostException if an error occurs.
    * @throws SIConnectionDroppedException if the connection is knackered.
    */   
   protected abstract void initiateCommsHandshaking()
      throws SIConnectionLostException, SIConnectionDroppedException;
   
   /**
    * Actually performs the handshake.
    * 
    * @param serverMode Set to true if this is a handshake from ME-ME, otherwise false.
    *
    * @throws SIConnectionLostException if an error occurs.
    * @throws SIConnectionDroppedException if the connection is knackered.
    */
   protected void initiateCommsHandshakingImpl(boolean serverMode)
      throws SIConnectionLostException, SIConnectionDroppedException
   {
      initiateCommsHandshakingImpl(serverMode, ConversationUsageType.JFAP);
   }

   /**
    * Actually performs the handshake.
    * 
    * @param serverMode Set to true if this is a handshake from ME-ME, otherwise false.
    * @param usageType indicates how the conversation over which the handshake will be performed is to be used.
    *
    * @throws SIConnectionLostException if an error occurs.
    * @throws SIConnectionDroppedException if the connection is knackered.
    */
   protected void initiateCommsHandshakingImpl(final boolean serverMode, final ConversationUsageType usageType)
      throws SIConnectionLostException, SIConnectionDroppedException
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "initiateCommsHandshakingImpl",
                                           new Object[] {Boolean.valueOf(serverMode), usageType});

      // Get the conversation state and the Handshake group object from it
      // This group will hold all the information that is negotiated.
      final CATHandshakeProperties handshakeProperties = new CATHandshakeProperties();

      // Get the connection info so we can return that in any NLS messages
      String connectionInfo = "Unknown";
      CommsConnection cConn = getCommsConnection();
      if (cConn != null) connectionInfo = cConn.getConnectionInfo();
      
      // At this point we can work out the client capabilities
      short capabilities = getClientCapabilities();

      // Check the heartbeats are not being overriden by the SIB properties file
      short heartBeatInterval = (short)
         CommsUtils.getRuntimeIntProperty(CommsConstants.HEARTBEAT_INTERVAL_KEY,
                                          ""+getConversation().getHeartbeatInterval());

      short heartBeatTimeout = (short)
         CommsUtils.getRuntimeIntProperty(CommsConstants.HEARTBEAT_TIMEOUT_KEY,
                                          ""+getConversation().getHeartbeatTimeout());
      
      // Get the current FAP level, overriding from SIB properties if required
      short currentFapLevel = (short) 
                        CommsUtils.getRuntimeIntProperty(CommsConstants.CLIENT_FAP_LEVEL_KEY,
                                                         ""+CommsConstants.CURRENT_FAP_VERSION);
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Client is FAP Level: " + currentFapLevel);

      // Build Handshaking Message consisting of:
      //
      //  Handshake Type                 0x01    Client
      //                                 0x02    ME - ME
      //
      // Then follows named fields which have a BIT16 ID, BIT16 Length and then data.
      // Each field requires 4 + length of storage. Not all fields are sent up as part
      // of the client / server handshake.
      //
      // Storage  | Details
      // ----------------------------
      //  6       |  Product Version      id=0x0001
      //  6       |  FAP Level            id=0x0002
      //  12      |  MaximumMessageSize   id=0x0003
      //  8       |  MaxumumTXSize        id=0x0004
      //  6       |  HeartbeatInterval    id=0x0005
      //  6       |  HeartbeatTimeout     id=0x000D (FAP 3 and higher only)
      //  6       |  Capabilities         id=0x0007 0x0001    Transactions are supported
      //          |                                 0x0002    Reliable messages supported
      //          |                                 0x0004    Assured messages supported
      //          |                                 0x00
      //          |                                 0xFFF8    Reserved
      //
      //  6       |  Product Id           id=0x000B 0x0001   Jetstream
      //          |                                 0x0002   WebSphere MQ
      //          |                                 0x0002   .NET
      //  36      |  Supported FAPS       id=0x000C 32 byte bit map with a 1 bit dentoting a
      //          |                                 supported level of the FAP.  Big-endian with
      //          |                                 LSB = FAP level 0.  This is only present on FAP
      //          |                                 level 3 and higher.
      //  4       |  Usage Type           id=0x000E Optional field which indicates how the conversation which initiated this handshake is to be used.
      //  4 + len |  Cell Name            id=0x000F The name of the WAS cell associated with this process if it is running in a server environment. Not sent if null.
      //  4 + len |  Node Name            id=0x0010 The name of the WAS node associated with this process if it is running in a server environment. Not sent if null.
      //  4 + len |  Server Name          id=0x0011 The name of the WAS server associated with this process if it is running in a server environment. Not sent if null.
      //  4 + len |  Cluster Name         id=0x0012 The name of the WAS cluster associated with this process if it is running in a server environment. Not sent if null.
      //
      // ----------------------------

      CommsByteBuffer hBuf = getCommsByteBuffer();
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "----- Sending the following handshake data ------");

      // Handshake type
      if (serverMode)
      {
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, " Handshake type    : ME");
         hBuf.put(CommsConstants.HANDSHAKE_ME);
      }
      else
      {
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, " Handshake type    : CLIENT");
         hBuf.put(CommsConstants.HANDSHAKE_CLIENT);
      }

      // Product Version
      byte productMajor = (byte) SIMPConstants.API_MAJOR_VERSION;
      byte productMinor = (byte) SIMPConstants.API_MINOR_VERSION;

      hBuf.putShort(CommsConstants.FIELDID_PRODUCT_VERSION);
      hBuf.putShort(2);
      hBuf.put(productMajor);
      hBuf.put(productMinor);
      handshakeProperties.setMajorVersion(productMajor);
      handshakeProperties.setMinorVersion(productMinor);
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, " Product Version   : " + productMajor + "." + productMinor);

      // FAP version
      hBuf.putShort(CommsConstants.FIELDID_FAP_LEVEL);
      hBuf.putShort(2);
      hBuf.putShort(currentFapLevel);
      handshakeProperties.setFapLevel(currentFapLevel);
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, " FAP Version       : " + currentFapLevel);

      // Max Message Size
      hBuf.putShort(CommsConstants.FIELDID_MAX_MESSAGE_SIZE);
      hBuf.putShort(8);
      hBuf.putLong(CommsConstants.MAX_MESSAGE_SIZE);
      handshakeProperties.setMaxMessageSize(CommsConstants.MAX_MESSAGE_SIZE);
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, " Max Msg Size      : " + CommsConstants.MAX_MESSAGE_SIZE);

      // Max transmission size
      hBuf.putShort(CommsConstants.FIELDID_MAX_TRANSMISSION_SIZE);
      hBuf.putShort(4);
      hBuf.putInt(CommsConstants.MAX_TRANSMISSION_SIZE);
      handshakeProperties.setMaxTransmissionSize(CommsConstants.MAX_TRANSMISSION_SIZE);
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, " Max Tx Size       : " + CommsConstants.MAX_TRANSMISSION_SIZE);

      // Heartbeat Interval
      hBuf.putShort(CommsConstants.FIELDID_HEARTBEAT_INTERVAL);
      hBuf.putShort(2);
      hBuf.putShort(heartBeatInterval);
      handshakeProperties.setHeartbeatInterval(heartBeatInterval);
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, " Heartbeat Interval: " + heartBeatInterval);
      
      // Heartbeat timeout - only FAP 4 and above
      if (currentFapLevel >= JFapChannelConstants.FAP_VERSION_5)
      {
         hBuf.putShort(CommsConstants.FIELDID_HEARTBEAT_TIMEOUT);
         hBuf.putShort(2);
         hBuf.putShort(heartBeatTimeout);
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, " Heartbeat Timeout : " + heartBeatTimeout);
      }
      // Always set it in the handshake properties even if we didn't negotiate it
      handshakeProperties.setHeartbeatTimeout(heartBeatTimeout);

      // Put the capabilities
      hBuf.putShort(CommsConstants.FIELDID_CAPABILITIES);
      hBuf.putShort(2);
      hBuf.putShort(capabilities);
      handshakeProperties.setCapabilites(capabilities);
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, " Capabilities      : 0x" + Integer.toHexString(capabilities));

      // Product Id
      hBuf.putShort(CommsConstants.FIELDID_PRODUCT_ID);
      hBuf.putShort(2);
      hBuf.putShort(CommsConstants.PRODUCT_ID_JETSTREAM);
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, " Product Id        : 0x" + Integer.toHexString(CommsConstants.PRODUCT_ID_JETSTREAM));

      // Supported FAP versions.  This is a bitmap of the supported FAP levels
      // with a 1 bit indicating support.  Big-endian (Java) byte ordering is
      // used and the LSB corresponds to FAP version 1.
      if (currentFapLevel >= JFapChannelConstants.FAP_VERSION_3)
      {
         byte[] fapBitmap = new byte[32];
         StringBuffer supportedFapLevels = null;
         
         for (int i=255; i >=0; --i)
         {
            // Determine if the bit is on or off
            boolean bitIsOn = CommsConstants.isFapLevelSupported(i+1);

            if (bitIsOn)
            {
               // Find the index into the byte array bitmap where the bit
               // is located.
               int byteOffset = (255-i) / 8;

               // Find the bit within this byte.
               int bitOffset = i % 8;

               // Work out the bit we want to set
               byte bitSet = (byte)(0x01 << bitOffset);

               // Add this bit into the array
               fapBitmap[byteOffset] |= bitSet;
               
               // Update the text string buffer
               if (supportedFapLevels == null) 
               {
                  supportedFapLevels = new StringBuffer();
                  supportedFapLevels.append(i + 1);
               }
               else
               {
                  supportedFapLevels.append(", ");
                  supportedFapLevels.append(i + 1);
               }
            }
         }

         hBuf.putShort(CommsConstants.FIELDID_SUPORTED_FAPS);
         hBuf.putShort(fapBitmap.length);
         hBuf.put(fapBitmap);
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, " Supported FAP's   : " + supportedFapLevels);
      }
      
      //This is isn't a 'basic' JFAP handshake then indicate that this is the case.
      if(usageType != ConversationUsageType.JFAP)
      {
         hBuf.putShort(CommsConstants.FIELDID_CONVERSATION_USAGE_TYPE);
         hBuf.putShort(4);
         usageType.serialize(hBuf);
      }
      
      //If the cell name is not null or too big, send it.      
      if(cellName != null && cellNameLengthInBuffer <= Short.MAX_VALUE)
      {
         hBuf.putShort(CommsConstants.FIELDID_CELL_NAME);
         hBuf.putShort(cellNameLengthInBuffer);
         hBuf.putString(cellName);
      }
      
      //If the node name is not null or too big, send it.      
      if(nodeName != null && nodeNameLengthInBuffer <= Short.MAX_VALUE)
      {
         hBuf.putShort(CommsConstants.FIELDID_NODE_NAME);
         hBuf.putShort(nodeNameLengthInBuffer);
         hBuf.putString(nodeName);
      }
      
      //If the server name is not null or too big, send it.      
      if(serverName != null && serverNameLengthInBuffer <= Short.MAX_VALUE)
      {
         hBuf.putShort(CommsConstants.FIELDID_SERVER_NAME);
         hBuf.putShort(serverNameLengthInBuffer);
         hBuf.putString(serverName);
      }
      
      //If the cluster name is not null or too big, send it.      
      if(clusterName != null && clusterNameLengthInBuffer <= Short.MAX_VALUE)
      {
         hBuf.putShort(CommsConstants.FIELDID_CLUSTER_NAME);
         hBuf.putShort(clusterNameLengthInBuffer);
         hBuf.putString(clusterName);
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "----- End of handshake data ---------------------");
      
      // Pass on call to server
      CommsByteBuffer buf = jfapExchange(hBuf, JFapChannelConstants.SEG_HANDSHAKE, 
                                         JFapChannelConstants.PRIORITY_MEDIUM, true);

      try
      {
         short err = buf.getCommandCompletionCode(JFapChannelConstants.SEG_HANDSHAKE);
         if (err != CommsConstants.SI_NO_EXCEPTION)
         {
            checkFor_SIConnectionDroppedException(buf, err);
            checkFor_SIConnectionLostException(buf, err);
            defaultChecker(buf, err);
         }

         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "----- Received the following handshake data -----");
         
         // First get the connection type
         byte connectionType = buf.get();
         if ((serverMode && connectionType != CommsConstants.HANDSHAKE_ME) ||
             (!serverMode && connectionType != CommsConstants.HANDSHAKE_CLIENT))
         {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Unexpected connection type returned!",
                                                 Byte.toString(connectionType));

            throw new SIConnectionLostException(
               nls.getFormattedMessage("UNABLE_TO_NEGOTIATE_CONNECTION_SICO1023", new Object[] {connectionInfo}, null)  // d192293
            );
         }
         
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
         {
            if (serverMode) SibTr.debug(this, tc, " Handshake type    : ME");
            else            SibTr.debug(this, tc, " Handshake type    : CLIENT");
         }

         // Now loop round through the data and get any overriden values.
         // Note that we could get nothing back here which would indicate that
         // the server is happy with our proposed values.
         while (buf.hasRemaining())
         {
            short fieldId = buf.getShort();

            switch (fieldId)
            {
            case CommsConstants.FIELDID_PRODUCT_VERSION:

               short productVersionFieldLength = buf.getShort();
               if (productVersionFieldLength != 2)
               {
                  if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Invalid product version length: " + productVersionFieldLength);
                  throw new SIConnectionLostException(
                     nls.getFormattedMessage("UNABLE_TO_NEGOTIATE_CONNECTION_SICO1023", new Object[] {connectionInfo}, null)
                  );
               }
               byte upperProductVersion = buf.get();
               byte lowerProductVersion = buf.get();

               if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, " Product Version   : " + upperProductVersion + "." + lowerProductVersion);
               
               // If the server replies saying that it has a higher major version,
               // we need to save it away as in future version we may need to
               // know that we are talking to a different server version
               handshakeProperties.setMajorVersion(upperProductVersion);
               handshakeProperties.setMinorVersion(lowerProductVersion);

               break;

            case CommsConstants.FIELDID_FAP_LEVEL:

               short fapLevelFieldLength = buf.getShort();
               if (fapLevelFieldLength != 2)
               {
                  if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Invalid FAP Field length: " + fapLevelFieldLength);
                  throw new SIConnectionLostException(
                     nls.getFormattedMessage("UNABLE_TO_NEGOTIATE_CONNECTION_SICO1023", new Object[] {connectionInfo}, null)
                  );
               }
               short fapLevel = buf.getShort();

               if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, " FAP Version       : " + fapLevel);

               // If the server replies with a higher FAP level, we cannot continue. Note this 
               // should not happen unless the server malfunctions.
               if( fapLevel > currentFapLevel)
               {
                  if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "FAP level returned ("+fapLevel+") is greater than the requested FAP level ("+currentFapLevel+")");
                  throw new SIConnectionLostException(
                     nls.getFormattedMessage("UNABLE_TO_NEGOTIATE_CONNECTION_SICO1023", new Object[] {connectionInfo}, null)  // d192293
                  );
               }

               // Is the FAP level in the supported table of FAP levels?
               if (!CommsConstants.isFapLevelSupported(fapLevel))
               {
                  if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "FAP level returned ("+fapLevel+") is not supported");
                  throw new SIConnectionLostException(
                        nls.getFormattedMessage("UNABLE_TO_NEGOTIATE_CONNECTION_SICO1023", new Object[] {connectionInfo}, null)  // d192293
                     );
               }

               handshakeProperties.setFapLevel(fapLevel);
               break;

            case CommsConstants.FIELDID_MAX_MESSAGE_SIZE:

               short maxMessageFieldLength = buf.getShort();
               if (maxMessageFieldLength != 8)
               {
                  if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Invalid Max Message Field length: " + maxMessageFieldLength);
                  throw new SIConnectionLostException(
                     nls.getFormattedMessage("UNABLE_TO_NEGOTIATE_CONNECTION_SICO1023", new Object[] {connectionInfo}, null)
                  );
               }
               long maxMessageSize = buf.getLong();
               if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, " Max Msg Size      : " + maxMessageSize);
               
               handshakeProperties.setMaxMessageSize(Math.min(maxMessageSize, CommsConstants.MAX_MESSAGE_SIZE));
               break;

            case CommsConstants.FIELDID_MAX_TRANSMISSION_SIZE:

               short maxTransmissionLength = buf.getShort();
               if (maxTransmissionLength != 4)
               {
                  if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Invalid Max Transmission Field length: " + maxTransmissionLength);
                  throw new SIConnectionLostException(
                     nls.getFormattedMessage("UNABLE_TO_NEGOTIATE_CONNECTION_SICO1023", new Object[] {connectionInfo}, null)
                  );
               }
               int maxTransmissionSize = buf.getInt();
               if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, " Max Tx Size       : " + maxTransmissionSize);
               
               handshakeProperties.setMaxTransmissionSize(Math.min(maxTransmissionSize, CommsConstants.MAX_TRANSMISSION_SIZE));
               break;

            case CommsConstants.FIELDID_HEARTBEAT_INTERVAL:

               short heartbeatFieldLength = buf.getShort();
               if (heartbeatFieldLength != 2)
               {
                  if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Invalid Heartbeat Interval Field length: " + heartbeatFieldLength);
                  throw new SIConnectionLostException(
                     nls.getFormattedMessage("UNABLE_TO_NEGOTIATE_CONNECTION_SICO1023", new Object[] {connectionInfo}, null)
                  );
               }
               short heartbeatInterval = buf.getShort();
               if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, " Heartbeat Interval: " + heartbeatInterval);
               
               handshakeProperties.setHeartbeatInterval(heartbeatInterval);
               
               break;
               
            case CommsConstants.FIELDID_HEARTBEAT_TIMEOUT:

               short heartbeatTimeoutFieldLength = buf.getShort();
               if (heartbeatTimeoutFieldLength != 2)
               {
                  if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Invalid Heartbeat Timeout Field length: " + heartbeatTimeoutFieldLength);
                  throw new SIConnectionLostException(
                     nls.getFormattedMessage("UNABLE_TO_NEGOTIATE_CONNECTION_SICO1023", new Object[] {connectionInfo}, null)
                  );
               }
               short heartbeatTimeout = buf.getShort();
               if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, " Heartbeat Timeout : " + heartbeatTimeout);
               
               handshakeProperties.setHeartbeatTimeout(heartbeatTimeout);
               
               break;

            case CommsConstants.FIELDID_CAPABILITIES:

              short capabilityInterval = buf.getShort();
              if (capabilityInterval != 2)
              {
                 if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Invalid Capability Field length: " + capabilityInterval);
                 throw new SIConnectionLostException(
                    nls.getFormattedMessage("UNABLE_TO_NEGOTIATE_CONNECTION_SICO1023", new Object[] {connectionInfo}, null)
                 );
              }
              short capability = buf.getShort();
              if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, " Capabilities      : " + capability);
              
              handshakeProperties.setCapabilites(capability);

              break;

            case CommsConstants.FIELDID_PRODUCT_ID:

               short productIdLength = buf.getShort();
               if (productIdLength != 2)
               {
                  if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Invalid Product Id Field length: " + productIdLength);
                  throw new SIConnectionLostException(
                     nls.getFormattedMessage("UNABLE_TO_NEGOTIATE_CONNECTION_SICO1023", new Object[] {connectionInfo}, null)
                  );
               }
               short productId = buf.getShort();

               if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, " Server Product Id : " + productId);
               
               handshakeProperties.setPeerProductId(productId);
               break;

            default:
               
               if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, " ** Unknown Parameter received: **");
               if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Field Id: " + fieldId);

               // We can throw this here as we have told the server what FAP version we
               // are, and therefore it should not negotiate with us any values that we
               // do not know about.

               SIConnectionLostException e = new
                  SIConnectionLostException(nls.getFormattedMessage("INVALID_PROP_SICO8009", 
                                                                       new Object[]
                                                                       {"" + fieldId}, 
                                                                       null)

               );

               FFDCFilter.processException(e, CLASS_NAME + ".initiateHandshaking",
                                           CommsConstants.JFAPCOMMUNICATOR_INITIATEHANDSHAKING_03, this);

               throw e;
            }
         }
         
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "----- End of handshake data ---------------------");
         
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Handshake properties:", handshakeProperties);
         
         // Now we have completed handshaking, set the heartbeating values to whatever were 
         // agreed upon
         getConversation().setHeartbeatInterval(handshakeProperties.getHeartbeatInterval());
         getConversation().setHeartbeatTimeout(handshakeProperties.getHeartbeatTimeout());
      }
      catch (SIException e)
      {
         FFDCFilter.processException(e, CLASS_NAME + ".initiateHandshaking",
                                     CommsConstants.JFAPCOMMUNICATOR_INITIATEHANDSHAKING_02, this);

         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Caught an SIException", e);
         
         throw new SIConnectionLostException(
            nls.getFormattedMessage("UNKNOWN_CORE_EXCP_SICO8002", new Object[] { e }, null),
            e
         );
      }
      finally
      {
         // Release pooled objects
         if (buf != null) buf.release();
      }
      
      //Only set handshake properties if the usageType requires it.
      if(usageType.requiresNormalHandshakeProcessing())
      {
         getConversation().setHandshakeProperties(handshakeProperties);
      }
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "initiateCommsHandshakingImpl");
   }
   
   
   // *******************************************************************************************
   // *                                 Private Helper Methods                                  *
   // *******************************************************************************************
   
   
   /**
    * Works out the capabilities that will be sent to the peer as part of the initial handshake.
    * This also takes into account any overrides from the SIB properties file.
    * 
    * @return Returns the capabilities.
    */
   private short getClientCapabilities()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getClientCapabilities");
      
      short capabilities = CommsConstants.CAPABILITIES_DEFAULT;
      
      // Allow the use of a runtime property to alter the capability that we require a non-java
      // bootstrap to locate an ME
      boolean nonJavaBootstrap =
         CommsUtils.getRuntimeBooleanProperty(CommsConstants.CAPABILITIY_REQUIRES_NONJAVA_BOOTSTRAP_KEY,
                                              CommsConstants.CAPABILITIY_REQUIRES_NONJAVA_BOOTSTRAP_DEF);

      if (nonJavaBootstrap)
      {
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Requesting non-java bootstrap");
         // This bit is off by default, so turn it on
         capabilities |= CommsConstants.CAPABILITIY_REQUIRES_NONJAVA_BOOTSTRAP;
      }
      
      // Allow the use of a runtime property to alter the capability that we require JMF messages
      boolean jmfMessagesOnly = 
         CommsUtils.getRuntimeBooleanProperty(CommsConstants.CAPABILITIY_REQUIRES_JMF_ENCODING_KEY,
                                              CommsConstants.CAPABILITIY_REQUIRES_JMF_ENCODING_DEF);
      
      if (jmfMessagesOnly)
      {
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Requesting JMF Only");
         // This bit is off by default, so turn it on
         capabilities |= CommsConstants.CAPABILITIY_REQUIRES_JMF_ENCODING;
      }

      // Allow the use of a runtime property to alter the capability that we require JMS messages
      boolean jmsMessagesOnly = 
         CommsUtils.getRuntimeBooleanProperty(CommsConstants.CAPABILITIY_REQUIRES_JMS_MESSAGES_KEY,
                                              CommsConstants.CAPABILITIY_REQUIRES_JMS_MESSAGES_DEF);
      
      if (jmsMessagesOnly)
      {
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Requesting JMS Only");
         // This bit is off by default, so turn it on
         capabilities |= CommsConstants.CAPABILITIY_REQUIRES_JMS_MESSAGES;
      }
      
      // Allow the use of a runtime property to turn off optimized transactions.
      boolean disableOptimizedTx =
         CommsUtils.getRuntimeBooleanProperty(CommsConstants.DISABLE_OPTIMIZED_TX_KEY,
                                              CommsConstants.DISABLE_OPTIMIZED_TX);
      
      if (disableOptimizedTx)
      {
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Disabling use of optimized transactions");
         // This bit is set on by default, so we must turn it off
         capabilities &= (0xFFFF ^ CommsConstants.CAPABILITIY_REQUIRES_OPTIMIZED_TX);
      }
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getClientCapabilities", capabilities);
      return capabilities;
   }

   
   // *******************************************************************************************
   // *                                 Exception handling code                                 *
   // *******************************************************************************************

   
   /* Notes on exception handling:
    *
    *  The core SPI is very specific about what exceptions each method can throw. As such,
    *  a seperate checker method has to be called for each exception type - each specific
    *  method throwing one type of exception. Therefore, the calling Core SPI implementations
    *  should call each checker method for each type of exception they expect. They should
    *  also call the default checker to handle exceptions that no one was expecting.
    *
    *  As such, use the following procedure to add a new exception:
    *
    *  1) Add a new constant for the exception in CommsConstants.
    *  2) Create a new checker method that checks the exception code with the appropriate
    *     code for that exception from CommsConstants. If the two match, then use getException()
    *     on the buffer to throw the exception.
    *  3) Modify getException() in CommsByteBuffer to ensure that it knows how to create the new 
    *     exception.
    */

   /**
    * @param buffer 
    * @param exceptionCode 
    * @throws SIIncorrectCallException if one was indicated in the received data.
    */
   public void checkFor_SIIncorrectCallException(CommsByteBuffer buffer, short exceptionCode)
      throws SIIncorrectCallException
   {
      if (exceptionCode == CommsConstants.SI_INCORRECT_CALL_EXCEPTION)
      {
         throw (SIIncorrectCallException) buffer.getException(con);
      }
   }

   /**
    * @param buffer 
    * @param exceptionCode 
    * @throws SIInvalidDestinationPrefixException if one was indicated in the received data.
    */
   public void checkFor_SIInvalidDestinationPrefixException(CommsByteBuffer buffer, short exceptionCode)
      throws SIInvalidDestinationPrefixException
   {
      if (exceptionCode == CommsConstants.SI_INVALID_DESTINATION_PREFIX_EXCEPTION)        // D245450
      {
         throw (SIInvalidDestinationPrefixException) buffer.getException(con);
      }
   }

   /**
    * @param buffer 
    * @param exceptionCode 
    * @throws SIDiscriminatorSyntaxException if one was indicated in the received data.
    */
   public void checkFor_SIDiscriminatorSyntaxException(CommsByteBuffer buffer, short exceptionCode)
      throws SIDiscriminatorSyntaxException                                               // D245450
   {
      if (exceptionCode == CommsConstants.SI_DISCRIMINATOR_SYNTAX_EXCEPTION)
      {
         throw (SIDiscriminatorSyntaxException) buffer.getException(con);  // D245450
      }
   }

   /**
    * @param buffer 
    * @param exceptionCode 
    * @throws SISelectorSyntaxException if one was indicated in the received data.
    */
   public void checkFor_SISelectorSyntaxException(CommsByteBuffer buffer, short exceptionCode)
      throws SISelectorSyntaxException
   {
      if (exceptionCode == CommsConstants.SI_SELECTOR_SYNTAX_EXCEPTION)
      {
         throw (SISelectorSyntaxException) buffer.getException(con);
      }
   }

   /**
    * @param buffer 
    * @param exceptionCode 
    * @throws SIInsufficientDataForFactoryTypeException if one was indicated in the received data.
    */
   public void checkFor_SIInsufficientDataForFactoryTypeException(CommsByteBuffer buffer, short exceptionCode)
      throws SIInsufficientDataForFactoryTypeException
   {
      if (exceptionCode == CommsConstants.SI_INSUFFICIENT_DATA_FOR_FACT_EXCEPTION)
      {
         throw (SIInsufficientDataForFactoryTypeException) buffer.getException(con);
      }
   }

   /**
    * @param buffer 
    * @param exceptionCode 
    * @throws SIAuthenticationException if one was indicated in the received data.
    */
   public void checkFor_SIAuthenticationException(CommsByteBuffer buffer, short exceptionCode)
      throws SIAuthenticationException
   {
      if (exceptionCode == CommsConstants.SI_AUTHENTICATION_EXCEPTION)
      {
         throw (SIAuthenticationException) buffer.getException(con);
      }
   }

   /**
    * @param buffer 
    * @param exceptionCode 
    * @throws SINotPossibleInCurrentConfigurationException if one was indicated in the received data.
    */
   public void checkFor_SINotPossibleInCurrentConfigurationException(CommsByteBuffer buffer, short exceptionCode)
      throws SINotPossibleInCurrentConfigurationException
   {
      if (exceptionCode == CommsConstants.SI_NOT_POSSIBLE_IN_CUR_CONFIG_EXCEPTION)
      {
         throw (SINotPossibleInCurrentConfigurationException) buffer.getException(con);
      }
   }

   /**
    * @param buffer 
    * @param exceptionCode 
    * @throws SINotAuthorizedException if one was indicated in the received data.
    */
   public void checkFor_SINotAuthorizedException(CommsByteBuffer buffer, short exceptionCode)
      throws SINotAuthorizedException
   {
      if (exceptionCode == CommsConstants.SI_NOT_AUTHORISED_EXCEPTION)
      {
         throw (SINotAuthorizedException) buffer.getException(con);
      }
   }

   /**
    * @param buffer 
    * @param exceptionCode 
    * @throws SISessionUnavailableException if one was indicated in the received data.
    */
   public void checkFor_SISessionUnavailableException(CommsByteBuffer buffer, short exceptionCode)
      throws SISessionUnavailableException
   {
      if (exceptionCode == CommsConstants.SI_SESSION_UNAVAILABLE_EXCEPTION)
      {
         throw (SISessionUnavailableException) buffer.getException(con);
      }
   }

   /**
    * @param buffer 
    * @param exceptionCode 
    * @throws SISessionDroppedException if one was indicated in the received data.
    */
   public void checkFor_SISessionDroppedException(CommsByteBuffer buffer, short exceptionCode)
      throws SISessionDroppedException
   {
      if (exceptionCode == CommsConstants.SI_SESSION_DROPPED_EXCEPTION)
      {
         throw (SISessionDroppedException) buffer.getException(con);
      }
   }

   /**
    * @param buffer 
    * @param exceptionCode 
    * @throws SIDurableSubscriptionAlreadyExistsException if one was indicated in the received data.
    */
   public void checkFor_SIDurableSubscriptionAlreadyExistsException(CommsByteBuffer buffer, short exceptionCode)
      throws SIDurableSubscriptionAlreadyExistsException
   {
      if (exceptionCode == CommsConstants.SI_DURSUB_ALREADY_EXISTS_EXCEPTION)
      {
         throw (SIDurableSubscriptionAlreadyExistsException) buffer.getException(con);
      }
   }

   /**
    * @param buffer 
    * @param exceptionCode 
    * @throws SIDurableSubscriptionMismatchException if one was indicated in the received data.
    */
   public void checkFor_SIDurableSubscriptionMismatchException(CommsByteBuffer buffer, short exceptionCode)
      throws SIDurableSubscriptionMismatchException
   {
      if (exceptionCode == CommsConstants.SI_DURSUB_MISMATCH_EXCEPTION)
      {
         throw (SIDurableSubscriptionMismatchException) buffer.getException(con);
      }
   }

   /**
    * @param buffer 
    * @param exceptionCode 
    * @throws SIDurableSubscriptionNotFoundException if one was indicated in the received data.
    */
   public void checkFor_SIDurableSubscriptionNotFoundException(CommsByteBuffer buffer, short exceptionCode)
      throws SIDurableSubscriptionNotFoundException
   {
      if (exceptionCode == CommsConstants.SI_DURSUB_NOT_FOUND_EXCEPTION)
      {
         throw (SIDurableSubscriptionNotFoundException) buffer.getException(con);
      }
   }

   /**
    * @param buffer 
    * @param exceptionCode 
    * @throws SIConnectionUnavailableException if one was indicated in the received data.
    */
   public void checkFor_SIConnectionUnavailableException(CommsByteBuffer buffer, short exceptionCode)
      throws SIConnectionUnavailableException
   {
      if (exceptionCode == CommsConstants.SI_CONNECTION_UNAVAILABLE_EXCEPTION)
      {
         throw (SIConnectionUnavailableException) buffer.getException(con);
      }
   }

   /**
    * @param buffer 
    * @param exceptionCode 
    * @throws SIConnectionDroppedException if one was indicated in the received data.
    */
   public void checkFor_SIConnectionDroppedException(CommsByteBuffer buffer, short exceptionCode)
      throws SIConnectionDroppedException
   {
      if (exceptionCode == CommsConstants.SI_CONNECTION_DROPPED_EXCEPTION)
      {
         throw (SIConnectionDroppedException) buffer.getException(con);
      }
   }

   /**
    * @param buffer 
    * @param exceptionCode 
    * @throws SIDataGraphFormatMismatchException if one was indicated in the received data.
    */
   public void checkFor_SIDataGraphFormatMismatchException(CommsByteBuffer buffer, short exceptionCode)
      throws SIDataGraphFormatMismatchException
   {
      if (exceptionCode == CommsConstants.SI_DATAGRAPH_FORMAT_MISMATCH_EXCEPTION)
      {
         throw (SIDataGraphFormatMismatchException) buffer.getException(con);
      }
   }

   /**
    * @param buffer 
    * @param exceptionCode 
    * @throws SIDataGraphSchemaNotFoundException if one was indicated in the received data.
    */
   public void checkFor_SIDataGraphSchemaNotFoundException(CommsByteBuffer buffer, short exceptionCode)
      throws SIDataGraphSchemaNotFoundException
   {
      if (exceptionCode == CommsConstants.SI_DATAGRAPH_SCHEMA_NOT_FOUND_EXCEPTION)
      {
         throw (SIDataGraphSchemaNotFoundException) buffer.getException(con);
      }
   }

   /**
    * @param buffer 
    * @param exceptionCode 
    * @throws SIDestinationLockedException if one was indicated in the received data.
    */
   public void checkFor_SIDestinationLockedException(CommsByteBuffer buffer, short exceptionCode)
      throws SIDestinationLockedException
   {
      if (exceptionCode == CommsConstants.SI_DESTINATION_LOCKED_EXCEPTION)
      {
         throw (SIDestinationLockedException) buffer.getException(con);
      }
   }

   /**
    * @param buffer 
    * @param exceptionCode 
    * @throws SITemporaryDestinationNotFoundException if one was indicated in the received data.
    */
   public void checkFor_SITemporaryDestinationNotFoundException(CommsByteBuffer buffer, short exceptionCode)
      throws SITemporaryDestinationNotFoundException
   {
      if (exceptionCode == CommsConstants.SI_TEMPORARY_DEST_NOT_FOUND_EXCEPTION)
      {
         throw (SITemporaryDestinationNotFoundException) buffer.getException(con);
      }
   }

   /**
    * @param buffer 
    * @param exceptionCode 
    * @throws SIMessageException if one was indicated in the received data.
    */
   public void checkFor_SIMessageException(CommsByteBuffer buffer, short exceptionCode)
      throws SIMessageException
   {
      if (exceptionCode == CommsConstants.SI_MESSAGE_EXCEPTION)
      {
         throw (SIMessageException) buffer.getException(con);
      }
   }

   /**
    * @param buffer 
    * @param exceptionCode 
    * @throws SIResourceException if one was indicated in the received data.
    */
   public void checkFor_SIResourceException(CommsByteBuffer buffer, short exceptionCode)
      throws SIResourceException
   {
      if (exceptionCode == CommsConstants.SI_RESOURCE_EXCEPTION)
      {
         throw (SIResourceException) buffer.getException(con);
      }
   }

   /**
    * @param buffer 
    * @param exceptionCode 
    * @throws SILimitExceededException if one was indicated in the received data.
    */
   public void checkFor_SILimitExceededException(CommsByteBuffer buffer, short exceptionCode)
      throws SILimitExceededException
   {
      if (exceptionCode == CommsConstants.SI_LIMIT_EXCEEDED_EXCEPTION)
      {
         throw (SILimitExceededException) buffer.getException(con);
      }
   }

   /**
    * @param buffer 
    * @param exceptionCode 
    * @throws SIConnectionLostException if one was indicated in the received data.
    */
   public void checkFor_SIConnectionLostException(CommsByteBuffer buffer, short exceptionCode)
      throws SIConnectionLostException
   {
      if (exceptionCode == CommsConstants.SI_CONNECTION_LOST_EXCEPTION)
      {
         throw (SIConnectionLostException) buffer.getException(con);
      }
   }

   /**
    * @param buffer 
    * @param exceptionCode 
    * @throws SIRollbackException if one was indicated in the received data.
    */
   public void checkFor_SIRollbackException(CommsByteBuffer buffer, short exceptionCode)
      throws SIRollbackException
   {
      if (exceptionCode == CommsConstants.SI_ROLLBACK_EXCEPTION)
      {
         throw (SIRollbackException) buffer.getException(con);
      }
   }

   /**
    * @param buffer 
    * @param exceptionCode 
    * @throws SINotSupportedException if one was indicated in the received data.
    */
   public void checkFor_SINotSupportedException(CommsByteBuffer buffer, short exceptionCode)
      throws SINotSupportedException
   {
      if (exceptionCode == CommsConstants.SI_NOT_SUPPORTED_EXCEPTION)
      {
         throw (SINotSupportedException) buffer.getException(con);
      }
   }

   /**
    * @param buffer 
    * @param exceptionCode 
    * @throws SIMessageDomainNotSupportedException if one was indicated in the received data.
    */
   public void checkFor_SIMessageDomainNotSupportedException(CommsByteBuffer buffer, short exceptionCode)
      throws SIMessageDomainNotSupportedException
   {
      if (exceptionCode == CommsConstants.SI_MSG_DOMAIN_NOT_SUPPORTED_EXCEPTION)
      {
         throw (SIMessageDomainNotSupportedException) buffer.getException(con);
      }
   }

   /**
    * @param buffer 
    * @param exceptionCode 
    * @throws SIDataGraphException if one was indicated in the received data.
    */
   public void checkFor_SIDataGraphCreationException(CommsByteBuffer buffer, short exceptionCode)
      throws SIDataGraphException
   {
      if (exceptionCode == CommsConstants.SI_DATAGRAPH_EXCEPTION)
      {
         throw (SIDataGraphException) buffer.getException(con);
      }
   }

   /**
    * @param buffer 
    * @param exceptionCode 
    * @throws SIErrorException if one was indicated in the received data.
    */
   public void checkFor_SIErrorException(CommsByteBuffer buffer, short exceptionCode)
      throws SIErrorException
   {
      if (exceptionCode == CommsConstants.SI_ERROR_EXCEPTION)
      {
         throw (SIErrorException) buffer.getException(con);
      }
   }

   // Start SIB0009.comms.1
   /**
    * @param buffer 
    * @param exceptionCode 
    * @throws SICommandInvocationFailedException if one was indicated in the received data.
    */
   public void checkFor_SICommandInvocationFailedException(CommsByteBuffer buffer, short exceptionCode)
      throws SICommandInvocationFailedException
   {
      if (exceptionCode == CommsConstants.SI_COMMAND_INVOCATION_FAILED_EXCEPTION)
      {
         throw (SICommandInvocationFailedException) buffer.getException(con);
      }
   }
   // End SIB0009.comms.1
   
   /**
    * @param buffer 
    * @param exceptionCode 
    * @throws SIMessageNotLockedException if one was indicated in the received data.
    */
   public void checkFor_SIMessageNotLockedException(CommsByteBuffer buffer, short exceptionCode)
      throws SIMessageNotLockedException
   {
      if (exceptionCode == CommsConstants.SI_MESSAGE_NOT_LOCKED_EXCEPTION)
      {
         throw (SIMessageNotLockedException) buffer.getException(con);
      }
   }
   
   /**
    * The default checker. Should always be called last after all the checkers.
    * @param buffer 
    * @param exceptionCode 
    * @throws SIErrorException if the exception code is <strong>not</strong>
    * the enumerated value for "throw no exception".
    */
   public void defaultChecker(CommsByteBuffer buffer, short exceptionCode)
      throws SIErrorException
   {
      if (exceptionCode != CommsConstants.SI_NO_EXCEPTION)
      {
         throw new SIErrorException(buffer.getException(con));
      }
   }
   
   /**
    * Utility method to invalidate Connection.  Parameters passed to ConnectionInterface.invalidate
    * @param notifyPeer
    * @param throwable
    * @param debugReason
    */
   protected void invalidateConnection(boolean notifyPeer, Throwable throwable, String debugReason)
   {
       if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "invalidateConnection", new Object[]{new Boolean(notifyPeer),
                                                                                     throwable,
                                                                                     debugReason});
       if (con != null)
       {
           ConnectionInterface connection = con.getConnectionReference();
           if (connection != null)
           {
               connection.invalidate(notifyPeer, throwable, debugReason);
           }         
       }
       if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "invalidateConnection");
   }

}
