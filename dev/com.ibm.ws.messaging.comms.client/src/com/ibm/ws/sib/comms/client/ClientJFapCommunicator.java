/*******************************************************************************
 * Copyright (c) 2003, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.comms.client;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.comms.CommsConnection;
import com.ibm.ws.sib.comms.CommsConstants;
import com.ibm.ws.sib.comms.common.CommsByteBuffer;
import com.ibm.ws.sib.comms.common.CommsByteBufferPool;
import com.ibm.ws.sib.comms.common.JFAPCommunicator;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.SICoreConnection;
import com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException;
import com.ibm.wsspi.sib.core.exception.SIConnectionLostException;

/**
 * The client JFap communicator.
 * 
 * @author Gareth Matthews
 */
public class ClientJFapCommunicator extends JFAPCommunicator
{
   /** Register Class with Trace Component */
   private static final TraceComponent tc = SibTr.register(ClientJFapCommunicator.class, 
                                                           CommsConstants.MSG_GROUP, 
                                                           CommsConstants.MSG_BUNDLE);
   
   /** Holds state information about client-server conversations */
   private ClientConversationState cConState = null;
   
   /** The comms byte buffer pool */
   private CommsByteBufferPool commsByteBufferPool = CommsByteBufferPool.getInstance();

   /**
    * @return Returns the Connection ID referring to the SICoreConnection
    *         Object on the Server
    */
   protected int getConnectionObjectID()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getConnectionObjectID");

      int objectID = 0;

      // Retrieve Conversation State if necessary
      validateConversationState();

      objectID = cConState.getConnectionObjectID();

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "getConnectionObjectID", ""+objectID);
      return objectID;
   }
   
   /**
    * Sets the Connection ID referring to the SICoreConnection Object
    * on the server
    *
    * @param i
    */
   protected void setConnectionObjectID(int i)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "setConnectionObjectID", "" + i);

      // Retrieve Client Conversation State if necessary
      validateConversationState();

      cConState.setConnectionObjectID(i);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "setConnectionObjectID");
   }
   
   /**
    * @return Returns the CommsConnection associated with this conversation
    */
   protected CommsConnection getCommsConnection()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getCommsConnection");

      CommsConnection cc = null;

      //Retrieve Conversation State if necessary
      validateConversationState();

      cc = cConState.getCommsConnection();

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getCommsConnection", cc);
      return cc;
   }
   
   /**
    * Sets the CommsConnection associated with this Conversation
    *
    * @param cc
    */
   protected void setCommsConnection(CommsConnection cc)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "setCommsConnection");

      // Retrieve Client Conversation State if necessary
      validateConversationState();

      cConState.setCommsConnection(cc);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "setCommsConnection");
   }

   /**
    * Sets the SICoreConnection reference on the server
    *
    * @param connection
    */
   protected void setSICoreConnection(SICoreConnection connection)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "setSICoreConnection", connection);

      //Retrieve Client Conversation State if necessary
      validateConversationState();
      cConState.setSICoreConnection(connection);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "setSICoreConnection");
   }
   
   /**
    * This method will create the conversation state overwritting anything previously
    * stored.
    */
   protected void createConversationState()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "createConversationState");

      // Now create the Conversation wide ConversationState
      ClientConversationState ccs = new ClientConversationState();
      getConversation().setAttachment(ccs);

      // Only create one if one does not already exist
      ClientLinkLevelState clls = (ClientLinkLevelState) getConversation().getLinkLevelAttachment();
      if (clls == null)
      {
         getConversation().setLinkLevelAttachment(new ClientLinkLevelState());
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "createConversationState");
   }
   
   /**
    * @return Returns a Conversation wide unique request number.
    */
   protected int getRequestNumber()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getRequestNumber");

      int reqnum = -1;

      // Retrieve Client Conversation State if necessary
      validateConversationState();
      
      int initialRequestNumber = -1;
      
      while((reqnum < 0) || (!getConversation().checkRequestNumberIsFree(reqnum)))
      {
    	  // Now get the unique request number
    	  reqnum = cConState.getUniqueRequestNumber();
    	  
    	  if(initialRequestNumber == reqnum)
	      {
    		  // If we get here, request numbers have wrapped. Continue and use the first request number we got.
	    	  // This will cause the old behaviour to throw out the request ID as a 'duplicate request' in JFap's
	    	  // ConversationImpl. It'll manifest as an SIErrorException CWSIJ0046E.
    		  if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc,"Request numbers wrapped");
	    	  break;
	      }
          // If initialRequestNumber is less than 0, it's the first time around the loop. So cache the first
	      // request number we got so we can detect wrapping.
	      if(initialRequestNumber < 0) initialRequestNumber = reqnum;
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getRequestNumber", ""+reqnum);
      return reqnum;
   }
   
   /**
    * @return Returns a comms byte buffer for use.
    */
   protected CommsByteBuffer getCommsByteBuffer()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "getCommsByteBuffer");
      CommsByteBuffer buff = commsByteBufferPool.allocate();
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "getCommsByteBuffer", buff);
      return buff;
   }
   
   /**
    * Validates the conversation state by ensuring we have a local cache of it here in this class.
    */
   private void validateConversationState()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "validateConversationState");
      
      if (cConState == null)
      {
         cConState = (ClientConversationState) getConversation().getAttachment();
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Using Client Conversation State:", cConState);
      }
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "validateConversationState");
   }

   /**
    * Calls through to the JFAPCommunicator class to do the real handshaking.
    * 
    * @see com.ibm.ws.sib.comms.common.JFAPCommunicator#initiateCommsHandshaking()
    */
   protected void initiateCommsHandshaking() 
      throws SIConnectionLostException, SIConnectionDroppedException
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "initiateCommsHandshaking");
      
      initiateCommsHandshakingImpl(false);
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "initiateCommsHandshaking");
   }
}
