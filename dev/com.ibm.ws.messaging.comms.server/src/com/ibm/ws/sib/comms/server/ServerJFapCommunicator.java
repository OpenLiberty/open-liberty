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
package com.ibm.ws.sib.comms.server;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.comms.CommsConnection;
import com.ibm.ws.sib.comms.CommsConstants;
import com.ibm.ws.sib.comms.common.CommsByteBuffer;
import com.ibm.ws.sib.comms.common.JFAPCommunicator;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException;
import com.ibm.wsspi.sib.core.exception.SIConnectionLostException;

/**
 * The server JFap communicator.
 * 
 * @author Gareth Matthews
 */
public class ServerJFapCommunicator extends JFAPCommunicator {
    /** Class name for FFDC's */
    private static String CLASS_NAME = ServerJFapCommunicator.class.getName();

    /** Register Class with Trace Component */
    private static final TraceComponent tc = SibTr.register(ServerJFapCommunicator.class,
                                                            CommsConstants.MSG_GROUP,
                                                            CommsConstants.MSG_BUNDLE);

    /** Holds state information about me-me conversations */
    private ConversationState sConState = null;

    /** The byte buffer pool */
    private final CommsServerByteBufferPool commsByteBufferPool = CommsServerByteBufferPool.getInstance();

    /**
     * @return Returns the Connection ID referring to the SICoreConnection
     *         Object on the Server
     */
    @Override
    protected int getConnectionObjectID() {
        if (tc.isEntryEnabled())
            SibTr.entry(this, tc, "getConnectionObjectID");

        int objectID = 0;

        // Retrieve Conversation State if necessary
        validateConversationState();

        objectID = sConState.getConnectionObjectId();

        if (tc.isEntryEnabled())
            SibTr.exit(tc, "getConnectionObjectID", "" + objectID);
        return objectID;
    }

    /**
     * Sets the Connection ID referring to the SICoreConnection Object
     * on the server
     * 
     * @param i
     */
    @Override
    protected void setConnectionObjectID(int i) {
        if (tc.isEntryEnabled())
            SibTr.entry(this, tc, "setConnectionObjectID", "" + i);

        // Retrieve Client Conversation State if necessary
        validateConversationState();

        sConState.setConnectionObjectId((short) i);

        if (tc.isEntryEnabled())
            SibTr.exit(this, tc, "setConnectionObjectID");
    }

    /**
     * @return Returns the CommsConnection associated with this conversation
     */
    @Override
    protected CommsConnection getCommsConnection() {
        if (tc.isEntryEnabled())
            SibTr.entry(this, tc, "getCommsConnection");

        CommsConnection cc = null;

        //Retrieve Conversation State if necessary
        validateConversationState();

        cc = sConState.getCommsConnection();

        if (tc.isEntryEnabled())
            SibTr.exit(this, tc, "getCommsConnection", cc);
        return cc;
    }

    /**
     * Sets the CommsConnection associated with this Conversation
     * 
     * @param cc
     */
    @Override
    protected void setCommsConnection(CommsConnection cc) {
        if (tc.isEntryEnabled())
            SibTr.entry(this, tc, "setCommsConnection");

        // Retrieve Client Conversation State if necessary
        validateConversationState();

        sConState.setCommsConnection(cc);

        if (tc.isEntryEnabled())
            SibTr.exit(this, tc, "setCommsConnection");
    }

    /**
     * This method will create the conversation state overwritting anything previously
     * stored.
     */
    @Override
    protected void createConversationState() {
        if (tc.isEntryEnabled())
            SibTr.entry(this, tc, "createConversationState");

        // Now create the Conversation wide ConversationState
        ConversationState cs = new ConversationState();
        getConversation().setAttachment(cs);

        // Only create one if one does not already exist
        ServerLinkLevelState lls = (ServerLinkLevelState) getConversation().getLinkLevelAttachment();
        if (lls == null) {
            getConversation().setLinkLevelAttachment(new ServerLinkLevelState());
        }

        if (tc.isEntryEnabled())
            SibTr.exit(this, tc, "createConversationState");
    }

    /**
     * @return Returns a Conversation wide unique request number.
     */
    @Override
    protected int getRequestNumber() {
        if (tc.isEntryEnabled())
            SibTr.entry(this, tc, "getRequestNumber");

        int reqnum = 0;

        // Retrieve Client Conversation State if necessary
        validateConversationState();

        // Now get the unique request number
        reqnum = sConState.getUniqueRequestNumber();

        if (tc.isEntryEnabled())
            SibTr.exit(this, tc, "getRequestNumber", "" + reqnum);
        return reqnum;
    }

    /**
     * @return Returns a comms byte buffer for use.
     */
    @Override
    protected CommsByteBuffer getCommsByteBuffer() {
        if (tc.isEntryEnabled())
            SibTr.entry(tc, "getCommsByteBuffer");
        CommsByteBuffer buff = commsByteBufferPool.allocate();
        if (tc.isEntryEnabled())
            SibTr.entry(tc, "getCommsByteBuffer", buff);
        return buff;
    }

    /**
     * This is a helper method that returns the byte buffer as the correct server type object.
     * Calling this method is exactly the same as calling <code>getCommsByteBuffer()</code> and
     * casting it to a <code>CommsServerByteBuffer</code> manually.
     * 
     * @return Returns a comms server byte buffer for use.
     */
    protected CommsServerByteBuffer getCommsServerByteBuffer() {
        if (tc.isEntryEnabled())
            SibTr.entry(tc, "getCommsServerByteBuffer");
        CommsServerByteBuffer buff = commsByteBufferPool.allocate();
        if (tc.isEntryEnabled())
            SibTr.entry(tc, "getCommsServerByteBuffer", buff);
        return buff;
    }

    /**
     * Validates the conversation state by ensuring we have a local cache of it here in this class.
     */
    private void validateConversationState() {
        if (tc.isEntryEnabled())
            SibTr.entry(this, tc, "validateConversationState");

        if (sConState == null) {
            sConState = (ConversationState) getConversation().getAttachment();
            if (tc.isDebugEnabled())
                SibTr.debug(this, tc, "Using Client Conversation State:", sConState);
        }

        if (tc.isEntryEnabled())
            SibTr.exit(this, tc, "validateConversationState");
    }

    /**
     * Calls through to the JFAPCommunicator class to do the real handshaking.
     * 
     * @see com.ibm.ws.sib.comms.common.JFAPCommunicator#initiateCommsHandshaking()
     */
    @Override
    protected void initiateCommsHandshaking()
                    throws SIConnectionLostException, SIConnectionDroppedException {
        if (tc.isEntryEnabled())
            SibTr.entry(this, tc, "initiateCommsHandshaking");

        initiateCommsHandshakingImpl(true);

        if (tc.isEntryEnabled())
            SibTr.exit(this, tc, "initiateCommsHandshaking");
    }
}
