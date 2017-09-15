/*******************************************************************************
 * Copyright (c) 2003, 2008 IBM Corporation and others.
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
import com.ibm.ws.sib.comms.CommsConstants;
import com.ibm.ws.sib.jfapchannel.AcceptListener;
import com.ibm.ws.sib.jfapchannel.Conversation;
import com.ibm.ws.sib.jfapchannel.ConversationReceiveListener;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * A listener which is notified when new incoming conversations are
 * accepted by a listening socket. It provides a generic
 * ReceiveListener when a connection is accepted which
 * will then filter data to an appropriate subordinate ReceiveListener
 * depending on whether the connection is client or ME initiated.
 * <p>
 * By the time this listener is notified, the new conversation will have been
 * established but no initial data flows will have been sent.
 * 
 * @author prestona
 */
public class GenericTransportAcceptListener implements AcceptListener {
    /** Trace */
    private static TraceComponent tc = SibTr.register(GenericTransportAcceptListener.class,
                                                      CommsConstants.MSG_GROUP,
                                                      CommsConstants.MSG_BUNDLE);

    /** Singleton instance */
    private static GenericTransportReceiveListener genericTransportRecieveListnerInstance = GenericTransportReceiveListener.getInstance();

    /** Log class info on load */
    static {
        if (tc.isDebugEnabled())
            SibTr.debug(tc,
                        "Source info: @(#)SIB/ws/code/sib.comms.server.impl/src/com/ibm/ws/sib/comms/server/GenericTransportAcceptListener.java, SIB.comms, WASX.SIB, aa1225.01 1.12");
    }

    /**
     * Constructor
     */
    public GenericTransportAcceptListener() {
        if (tc.isEntryEnabled())
            SibTr.entry(tc, "<init>");

        if (tc.isEntryEnabled())
            SibTr.exit(tc, "<init>");
    }

    /**
     * Notified when a new conversation is accepted by a listening socket.
     * 
     * @param cfConversation The new conversation.
     * 
     * @return The conversation receive listener to use for asynchronous receive
     *         notification for this whole conversation.
     */
    public ConversationReceiveListener acceptConnection(Conversation cfConversation) {
        if (tc.isEntryEnabled())
            SibTr.entry(tc, "acceptConnection");

        // Return new instance of a GenericTransportReceiveListener. This listener
        // determines whether data has been received from a client or ME and routes it
        // accordingly to the ServerTransportReceiveListener or MEConnectionListener

        if (tc.isEntryEnabled())
            SibTr.exit(tc, "acceptConnection");

        return genericTransportRecieveListnerInstance; // F193735.3
    }
}
