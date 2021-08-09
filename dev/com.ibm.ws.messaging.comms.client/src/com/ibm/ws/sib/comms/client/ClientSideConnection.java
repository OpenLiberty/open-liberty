/*******************************************************************************
 * Copyright (c) 2003, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.comms.client;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.comms.ClientComponentHandshake;
import com.ibm.ws.sib.comms.ClientConnection;
import com.ibm.ws.sib.comms.CommsConstants;
import com.ibm.ws.sib.comms.CompHandshake;
import com.ibm.ws.sib.comms.ConnectionMetaData;
import com.ibm.ws.sib.comms.ConnectionProperties;
import com.ibm.ws.sib.comms.common.CommsByteBuffer;
import com.ibm.ws.sib.comms.common.CommsDiagnosticModule;
import com.ibm.ws.sib.jfapchannel.ClientConnectionManager;
import com.ibm.ws.sib.jfapchannel.ConnectionClosedListener;
import com.ibm.ws.sib.jfapchannel.Conversation;
import com.ibm.ws.sib.jfapchannel.Conversation.ThrottlingPolicy;
import com.ibm.ws.sib.jfapchannel.ConversationUsageType;
import com.ibm.ws.sib.jfapchannel.JFapChannelConstants;
import com.ibm.ws.sib.mfp.ConnectionSchemaSet;
import com.ibm.ws.sib.mfp.impl.CompHandshakeFactory;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.SICoreConnection;
import com.ibm.wsspi.sib.core.exception.SIAuthenticationException;
import com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException;
import com.ibm.wsspi.sib.core.exception.SIConnectionLostException;
import com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException;
import com.ibm.wsspi.sib.core.trm.SibTrmConstants;

import java.net.InetSocketAddress;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Map;

/**
 * An implementation of the ClientConnection to be used by TRM on the client.
 */
public class ClientSideConnection extends ClientJFapCommunicator implements ClientConnection,
                                                                            ConnectionClosedListener
{
   /** Class name for FFDC's */
   private static String CLASS_NAME = ClientSideConnection.class.getName();

   /** Register Class with Trace Component */
   private static final TraceComponent tc = SibTr.register(ClientSideConnection.class,
                                                           CommsConstants.MSG_GROUP,
                                                           CommsConstants.MSG_BUNDLE);

   /** NLS */
   private static final TraceNLS nls = TraceNLS.getTraceNLS(CommsConstants.MSG_BUNDLE);

   /** Log Source code level on static load of class */
   static
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Source info: @(#)SIB/ws/code/sib.comms.client.impl/src/com/ibm/ws/sib/comms/client/ClientSideConnection.java, SIB.comms, WASX.SIB, uu1215.01 1.104");

      // This is generally the first point of call for new client connections. So when this class
      // is loaded, initialise the diagnostic module
      CommsDiagnosticModule.initialise();
   }

   /** Helper String that contains info about what we are connecting to */
   private String connectionInfo = "Unknown";

   /**
    * Attempts to establish a client connection with the remote ME identified
    * by the connection properties argument.  Comms allocates communications
    * resources to the connection and if successful, invoke the client
    * component handshake implementation passed as an argument.
    * <p>
    * Normally, the caller will be notified of a failure to connect by
    * the fail method of the ClientComponentHandshake object being invoked.
    *
    * @param cp A connection properties object which identifies the ME to
    * attempt to connect to.
    *
    * @param cch A client component handshake object to call and notify when
    * a connection attempt succeeds or fails.
    *
    * @throws SIResourceException Indicates that the connection attempt
    * failed but also that something so catastrophic went wrong that the
    * normal failure reporting mechanism offered by the client component
    * handshake object could not be invoked.
    *
    * @throws SIAuthenticationException Indicates that on an initial handshake
    * the user ID and password combination was rejected.
    */
   public void connect(ConnectionProperties cp, ClientComponentHandshake cch)
      throws SIResourceException, SIAuthenticationException
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "connect", new Object[] {cp, cch});

      ClientConnectionManager conMan = null;

      if (cp == null)
      {
         // The caller of this method passed in a null for the connection properties. This is
         // bad as we kinda need those so we know where to connect to.
         SIErrorException e = new SIErrorException(
            nls.getFormattedMessage("NULL_CONNECTION_PROPERTIES_SICO1039", null, null)
         );

         FFDCFilter.processException(e, CLASS_NAME + ".connect",
                                     CommsConstants.CLIENTSIDECONNECTION_CONNECT_04, this);

         throw e;
      }

      if (cch == null)
      {
         // The caller of this method passed in null for the client component handshake. We need
         // this so we can call back to them to inform them to handshake.
         SIErrorException e = new SIErrorException(
            nls.getFormattedMessage("NULL_CCH_SICO1040", null, null)
         );

         FFDCFilter.processException(e, CLASS_NAME + ".connect",
                                     CommsConstants.CLIENTSIDECONNECTION_CONNECT_05, this);

         throw e;
      }

      // Use the JFAP Channel to connect to the server
      Conversation con = null;
      boolean handshakeCompletedOk = false;

      try
      {
         ClientConnectionManager.initialise();
         conMan = ClientConnectionManager.getRef();

         // Establish a connection using details passed in via TRM.
         // These details can either be a hostname / port or a WLM
         // end point.
         if (cp.getMode() == ConnectionProperties.PropertiesType.HOST_PORT)
         {
            final String host = cp.getEndPoint().getHost();
            final int port = cp.getEndPoint().getPort().intValue();
            final String chainName = cp.getChainName();

            connectionInfo = host + ":" + port + " - " + chainName;
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Connecting to: " + connectionInfo);

            final InetSocketAddress addr = AccessController.doPrivileged(new PrivilegedAction<InetSocketAddress>() {
               @Override
               public InetSocketAddress run() {
                  return new InetSocketAddress(host, port);
               }
            });
            con = conMan.connect(addr, new ProxyReceiveListener(), chainName);
         }
         else if (cp.getMode() == ConnectionProperties.PropertiesType.WLM_EP)
         {
            Object wlmEndpointData = cp.getWLMEndPointData();

            connectionInfo = wlmEndpointData.toString();
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Connecting to: " + connectionInfo);

            con = conMan.connect(wlmEndpointData,
                                 new ProxyReceiveListener());
         }
         else // cp.getMode() == ConnectionProperties.Z_TCP_PROXY
         {
            connectionInfo = "tcp bridge service";
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Connecting via: "+connectionInfo);
            con = conMan.connect(new ProxyReceiveListener(), ConversationUsageType.JFAP);
         }

         // Store the Conversation reference
         setConversation(con);
         // We need to store Conversation state for each conversation so now
         // seems a good time to create the state storage object
         createConversationState();

         // Store away the ClientSideConnection associated with this conversation
         this.setCommsConnection(this);

         // Start F247845
         // Check and see the multicast parameters passed into us by TRM
         Map trmProperties = cp.getClientConnectionPropertyMap();
         if (trmProperties != null)
         {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Client properties: ", trmProperties);

            // Work out if Multicast is infact enabled
            String subProtocol = (String) trmProperties.get(SibTrmConstants.SUBSCRIPTION_PROTOCOL);
            if (subProtocol != null)
            {
               if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Subscription protocol:", subProtocol);

               if (subProtocol.equals(SibTrmConstants.SUBSCRIPTION_PROTOCOL_MULTICAST))
               {
                  // TODO: fix this up!
                  throw new SIErrorException("This shouldn't happen!");
               }
            }
         }

         // We only need to do Comms and MFP handshaking if the connection was made
         // using a brand new socket
         if (con.isFirst())
         {
            // Set the connection closed listener
            con.addConnectionClosedListener(this, ConversationUsageType.JFAP);

            initiateCommsHandshaking();

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Comms handshake completed successfully");
            
            // Get hold of product version
            int productVersion = getConversation().getHandshakeProperties().getMajorVersion();

            try
            {
               // Get hold of MFP and drive it to handshake
               CompHandshake ch = (CompHandshake) CompHandshakeFactory.getInstance();
               ch.compStartHandshake(this,productVersion);
            }
            catch (Exception e1)
            {
               FFDCFilter.processException(e1, CLASS_NAME + ".connect",
                                          CommsConstants.CLIENTSIDECONNECTION_CONNECT_03, this);

               if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "MFP unable to create CompHandshake Singleton", e1);

               String message = nls.getFormattedMessage(
                  "MFP_HANDSHAKE_FAILED_SICO1005", new Object[] { e1 }, null
               );

               SIResourceException ce = new SIResourceException(message, e1);
               throw ce;
            }
         }

         // Now call TRM to do its handshaking
         if (!cch.connect(this))
         {
            // If TRM returned false here, we need to throw an exception
            // to cancel the proceedings. This may be a TRM failure or more
            // likely an authorisation problem. No matter what though, we
            // just indicate this to TRM and let them figure out why
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "TRM connection returned false - connection will be aborted");  //d179741

            // The caller of this method passed in null for the client component handshake. We need
            // this so we can call back to them to inform them to handshake.
            SIResourceException e = new SIResourceException(
               nls.getFormattedMessage("TRM_HANDSHAKE_FAILED_SICO1037", null, null)
            );

            FFDCFilter.processException(e, CLASS_NAME + ".connect",
                                        CommsConstants.CLIENTSIDECONNECTION_CONNECT_06, this);

            throw e;
         }
         
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "TRM handshake completed successfully"); // D223615
         handshakeCompletedOk = true;
      }
      catch (SIException e)
      {
         // No FFDC code needed

         // We don't FFDC here as being unable to connect is not necessarily a problem. In the case of bootstraping clients
         // each bootstrap triplet will be tried since being unable to connect to some targets is to be expected - particularly
         // in fail over scenarios. We have also discovered that a FFDC during a fail over can slow the fail over process. Instead
         // we just throw the exception back to our caller and let the layers above which know the bigger picture decide whether
         // to FFDC or not.

         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Unable to make initial connection", e);

         SIResourceException ce = new SIResourceException(
            nls.getFormattedMessage("CONNECT_FAILED_SICO1001", new Object[] { e }, null)
         );

         ce.initCause(e);
         if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "connect", ce);
         throw ce;
      }
      finally
      {
         // Ensure we always call this - as even if we fail we don't want to block everyone
         // else out from starting a conversation
         if (con != null)
         {
            if (handshakeCompletedOk)
            {
               con.handshakeComplete();
            }
            else
            {
            	// release any handshake waiters  
                if (con != null)
                   con.handshakeFailed();
                
                // Start D273578
                // Ensure we close the conversation down ignoring any exceptions
                try 
                {
                   if (con != null)
                      con.close();
                }
                catch (SIException e2)
                {
                   // No FFDC Code Needed
                   if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Failed to close connection: " + e2);
                }
                // End D273578
                
                con = null;
            }
         }
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "connect");
   }

   /**
    * @return Returns a helper String about where are connected to.
    */
   public String getConnectionInfo()
   {
      return connectionInfo;
   }

   /**
    * Associates a SICoreConnection with this client connection.  It is not
    * valid to invoke this method from a client - it may only be invoked on
    * the ME instance of ClientConection.
    * <p>
    * From a implementation perspective, invoking this method does little more
    * than storing a reference to the SICoreConnection inside the implementation
    * of ClientConnection.
    */
   public void setSICoreConnection(SICoreConnection conn)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "setSICoreConnection", conn);
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "setSICoreConnection");

      // This method is not valid on the client side
      SIErrorException e = new SIErrorException(
         nls.getFormattedMessage("METHOD_CALL_NOT_ALLOWED_SICO8003",
                        new Object[] { "setSICoreConnection" },
                        null)
      );

      FFDCFilter.processException(e, CLASS_NAME + ".setSICoreConnection",
                                  CommsConstants.CLIENTSIDECONNECTION_SETSICONN_01, this);

      throw e;
   }

   /**
    * Returns the SICoreConnection associated with this connection. The actual
    * proxy object will have been created and saved in the conversation state
    * when the server returned us the information about the connection. So, here
    * we just retrieve it and return it.
    *
    * @see ClientConnection#setSICoreConnection(SICoreConnection)
    *
    * @return SICoreConnection A class which implements SICoreConnection.  For the
    * ME ClientConnection, this will be whatever was set using the
    * setSICoreConnection method.  In the client ClientConnection case this will
    * be a "proxy" SICoreConnection object which represents the object set
    * using setSICoreConnection on the ClientConnection's ME based peer.
    *
    * @throws SIConnectionLostException Thrown in the client ClientConnection case where
    * a communications failure occures when trying to retrieve the required
    * data to build a SICoreConnection "proxy" object.
    */
   public SICoreConnection getSICoreConnection() throws SIConnectionLostException
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getSICoreConnection");

      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Building connection proxy object using ID: ",
                                           ""+ getConnectionObjectID());

      ClientConversationState convState = (ClientConversationState) getConversation().getAttachment();
      SICoreConnection siConn = convState.getSICoreConnection();

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getSICoreConnection", siConn);
      return siConn;
   }

   /**
    * Exchanges TRM handshake data with this Connection's peer.
    * An exchange is defined as sending some data to our
    * "peer" then waiting until our "peer" responds with a reply.
    * <p>
    * This method will block the calling thread until the "peer" being
    * communicated with sends back a response.
    * @param trmData The data to send to the TRM component on this Connection's peer.
    *
    * @return byte[] The data sent back by the peer in response.
    *
    * @throws SIConnectionLostException Thrown if a communications problem is encountered
    * whilst attempting to send the data.
    * @throws SIConnectionDroppedException Thrown if the underlying connection is closed.
    * @throws SIConnectionUnavailableException Thrown if it would be invalid to
    * attempt this operation at this time - for example if the Connection is
    * not currently in a connected state.
    */
   public byte[] trmHandshakeExchange(byte[] trmData)
      throws
         SIConnectionLostException,
         SIConnectionDroppedException,
         SIConnectionUnavailableException
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "trmHandshakeExchange");

      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.bytes(this, tc, trmData);

      CommsByteBuffer request = getCommsByteBuffer();
      request.wrap(trmData);

      // Pass on TRM data to server
      CommsByteBuffer reply = jfapExchange(request,
                                       JFapChannelConstants.SEG_TOPOLOGY,
                                       JFapChannelConstants.PRIORITY_MEDIUM,
                                       true);

      byte[] trmReplyData;

      try
      {
         short err = reply.getCommandCompletionCode(JFapChannelConstants.SEG_TOPOLOGY);
         if (err != CommsConstants.SI_NO_EXCEPTION)
         {
            checkFor_SIConnectionLostException(reply, err);
            checkFor_SIConnectionDroppedException(reply, err);
            checkFor_SIConnectionUnavailableException(reply, err);
            checkFor_SIErrorException(reply, err);
            defaultChecker(reply, err);
         }

         trmReplyData = reply.getRemaining();

         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.bytes(this, tc, trmReplyData);
      }
      finally
      {
         if (reply != null) reply.release();
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "trmHandshakeExchange");
      return trmReplyData;
   }

   /**
    * Exchanges MFP handshake data with this Connection's peer.
    * An exchange is defined as sending some data to our
    * "peer" then waiting until our "peer" responds with a reply.
    * <p>
    * This method will block the calling thread until the "peer" being
    * communicated with sends back a response.
    * @param mfpData The data to send to the MFP component on this Connection's peer.
    *
    * @return byte[] The data sent back by the peer in response.
    *
    * @throws SIConnectionLostException Thrown if a communications problem is encountered
    * whilst attempting to send the data.
    * @throws SIConnectionDroppedException Thrown if the underlying connection is closed
    * @throws SIConnectionUnavailableException Thrown if it would be invalid to
    * attempt this operation at this time - for example if the Connection is
    * not currently in a connected state.
    */
   public byte[] mfpHandshakeExchange(byte[] mfpData)
      throws
         SIConnectionLostException,
         SIConnectionDroppedException,
         SIConnectionUnavailableException
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "mfpHandshakeExchange");

      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.bytes(this, tc, mfpData);

      CommsByteBuffer request = getCommsByteBuffer();
      request.wrap(mfpData);

      // Pass on MFP data to server
      CommsByteBuffer reply = jfapExchange(request,
                                          JFapChannelConstants.SEG_MESSAGE_FORMAT_INFO,
                                          JFapChannelConstants.PRIORITY_MEDIUM,
                                          true);
      byte[] mfpReplyData;

      try
      {
         short err = reply.getCommandCompletionCode(JFapChannelConstants.SEG_MESSAGE_FORMAT_INFO);
         if (err != CommsConstants.SI_NO_EXCEPTION)
         {
            checkFor_SIConnectionLostException(reply, err);
            checkFor_SIConnectionDroppedException(reply, err);
            checkFor_SIConnectionUnavailableException(reply, err);
            checkFor_SIErrorException(reply, err);
            defaultChecker(reply, err);
         }

         mfpReplyData = reply.getRemaining();

         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.bytes(this, tc, mfpReplyData);
      }
      finally
      {
         if (reply != null) reply.release();
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "mfpHandshakeExchange");
      return mfpReplyData;
   }

   /**
    * Attempts to send an MFP message schema to the peer at the highest possible
    * priority. At Message encode time MFP can discover that the destination entity
    * cannot decode the message about to be sent. This method is then called to
    * send a top priority transmission containing the message schema ahead of the
    * message relying on it to ensure it can be decoded correctly.
    *
    * @param schemaData The data to send to the MFP component on this Connection's peer.
    *
    * @throws SIConnectionLostException Thrown if a communications failure occurres.
    * @throws SIConnectionDroppedException Thrown if the underlying connection is closed
    * @throws SIConnectionUnavailableException Thrown if it is invalid to invoke
    * this method at this point in time.  For example if the Connection has been
    * closed.
    */
   public void sendMFPSchema(byte[] schemaData)
      throws
         SIConnectionLostException,
         SIConnectionDroppedException,
         SIConnectionUnavailableException
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "sendMFPSchema");

      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.bytes(this, tc, schemaData);

      CommsByteBuffer request = getCommsByteBuffer();
      request.wrap(schemaData);

      // Pass on schema data to server
      jfapSend(request,
               JFapChannelConstants.SEG_SEND_SCHEMA_NOREPLY,
               JFapChannelConstants.PRIORITY_HIGHEST,
               true,
               ThrottlingPolicy.BLOCK_THREAD);

      // Note: There is currently no concept of capacity exceptions on the client
      //       or the client connection being in an invalid state. Might be worth
      //       considering these at some point.

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "sendMFPSchema");
   }

   /**
    * Performs a controlled close of this connection.  It is not valid to
    * invoke this method whilst within connect processing.  If possible, a
    * close request is proporgated to the remote "peer" associated with this
    * ClientConnection.
    */
   public void close()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "close");

      // We should close the underlying connection at this point
      try
      {
         Conversation conv = getConversation();
         if (conv != null) conv.close();
      }
      catch (SIConnectionLostException e)
      {
         // Not a lot we can do here. So, debug the error and FFDC
         FFDCFilter.processException(e, CLASS_NAME + ".close",
                                     CommsConstants.CLIENTSIDECONNECTION_CONNECT_02, this);

         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Unable to close connection", e);
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "close");
   }

   /**
    * @return Returns the meta data for this connection.
    */
   public ConnectionMetaData getMetaData()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getMetaData");
      ConnectionMetaData meta = new ConnectionMetaDataImpl(getConversation().getMetaData(),
                                                           getConversation().getHandshakeProperties());
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getMetaData", meta);
      return meta;
   }

   /**
    * @return Returns some information about this connection.
    */
   public String toString()
   {
      return "ClientSideConnection@" + Integer.toHexString(System.identityHashCode(this)) +
             ": " + connectionInfo;
   }

   /**
    * Called when the JFap channel has closed the underlying connection. At this point we can
    * inform MFP so that they can delete the schema information that refered to this connection.
    *
    * @param linkLevelAttachement
    */
   public void connectionClosed(Object linkLevelAttachement)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "connectionClosed", linkLevelAttachement);

      try
      {
         // Get hold of MFP and inform it of connection closure
         CompHandshake ch = (CompHandshake) CompHandshakeFactory.getInstance();
         ch.compClose(this);
      }
      catch (Exception e1)
      {
         // No FFDC code needed
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "MFP unable to create CompHandshake Singleton", e1);
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "connectionClosed");
   }

   /**
    * Used to synchronously request an MFP schema from the server. This code will block until the
    * server replies with the requested data.
    *
    * @param schemaData The data used to request the schema.
    *
    * @return Returns the data
    */
   public byte[] requestMFPSchemata(byte[] schemaData)
   throws SIConnectionLostException,
          SIConnectionDroppedException,
          SIConnectionUnavailableException,
          SIErrorException
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "requestMFPSchemata");

      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.bytes(this, tc, schemaData);

      byte[] returnSchemaData = null;

      CommsByteBuffer request = getCommsByteBuffer();
      request.wrap(schemaData);

      CommsByteBuffer reply = jfapExchange(request,
                                           JFapChannelConstants.SEG_REQUEST_SCHEMA,
                                           JFapChannelConstants.PRIORITY_HIGHEST,
                                           true);

      try
      {
         short err = reply.getCommandCompletionCode(JFapChannelConstants.SEG_REQUEST_SCHEMA_R);
         if (err != CommsConstants.SI_NO_EXCEPTION)
         {
            checkFor_SIConnectionUnavailableException(reply, err);
            checkFor_SIErrorException(reply, err);
            defaultChecker(reply, err);
         }

         // Now get the bytes
         returnSchemaData = reply.getRemaining();

         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.bytes(this, tc, returnSchemaData);
      }
      finally
      {
         if (reply != null) reply.release();
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "requestMFPSchemata", returnSchemaData);
      return returnSchemaData;
   }

   /**
    * setSchemaSet
    * Sets the schemaSet in the underlying Connection.
    *
    * @param schemaSet   The SchemaSet which pertains to the Connection.
    */
   public void setSchemaSet(ConnectionSchemaSet schemaSet)
   {
     if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "setSchemaSet", schemaSet);
     getConversation().setSchemaSet(schemaSet);
     if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "setSchemaSet");
   }

   /**
    * getSchemaSet
    * Returns the MFP SchemaSet which pertains to the underlying Connection.
    *
    * @throws SIConnectionDroppedException Thrown if the underlying connection is closed.
    *
    * @return ConnectionSchemaSet The SchemaSet belonging to the underlying Connection.
    */
   public ConnectionSchemaSet getSchemaSet()  throws SIConnectionDroppedException
   {
     if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getSchemaSet");
     ConnectionSchemaSet result = getConversation().getSchemaSet();
     if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getSchemaSet",  result);
     return result;
   }
}
