/*******************************************************************************
 * Copyright (c) 2003, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.comms.server;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Map.Entry;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.IncidentStream;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.sib.admin.JsMessagingEngine;
import com.ibm.ws.sib.comms.CommsConstants;
import com.ibm.ws.sib.comms.MEConnection;
import com.ibm.ws.sib.comms.common.ClientCommsDiagnosticModule;
import com.ibm.ws.sib.comms.server.clientsupport.CATConnection;
import com.ibm.ws.sib.comms.server.clientsupport.CATMainConsumer;
import com.ibm.ws.sib.comms.server.clientsupport.CATOrderingContext;
import com.ibm.ws.sib.comms.server.clientsupport.ServerSideConnection;
import com.ibm.ws.sib.comms.server.clientsupport.ServerTransportAcceptListener;
import com.ibm.ws.sib.jfapchannel.Conversation;
import com.ibm.ws.sib.jfapchannel.server.ServerConnectionManager;
import com.ibm.ws.sib.utils.ras.FormattedWriter;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.ConsumerSession;
import com.ibm.wsspi.sib.core.SICoreConnection;

/**
 * Write a diagnostic dump of the Server Comms state.
 */
public class ServerCommsDiagnosticDump {
    /** Register Class with Trace Component */
    private static final TraceComponent tc = SibTr.register(ServerCommsDiagnosticDump.class,
                                                            CommsConstants.MSG_GROUP,
                                                            CommsConstants.MSG_BUNDLE);

    /**
     * Create an instance and create the cump.
     */
    public static void dump(FormattedWriter writer, String arg) {

        new ServerCommsDiagnosticDump().dumpCommunications(writer, arg);;

    }

    /**
     * Private constructor.
     */
    private ServerCommsDiagnosticDump() {
    }

    private void dumpCommunications(FormattedWriter writer, String arg) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "dumpCommunications", new Object[] { writer, arg });

        StringTokenizer st = new StringTokenizer(arg, ":");
        boolean found = false;
        while (st.hasMoreElements()) {
            if (st.nextToken().endsWith("ServerCommsDiagnosticDump")) {
                found = true;
                break;
            }
        }

        try {
            if (found) {
                writer.newLine();
                writer.startTag(this.getClass().getSimpleName());
                writer.indent();

                writer.newLine();
                writer.startTag("MEtoMECommunications");
                writer.indent();
                dumpMEtoMEConversations(writer);
                writer.outdent();
                writer.newLine();
                writer.endTag("MEtoMECommunications");

                writer.newLine();
                writer.startTag("InboundCommunications");
                writer.indent();
                dumpInboundConversations(writer);
                writer.outdent();
                writer.newLine();
                writer.endTag("InboundCommunications");

                writer.outdent();
                writer.newLine();
                writer.endTag(this.getClass().getSimpleName());
            }
        } catch (Exception exception) {
            // No FFDC Code Needed
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                SibTr.event(this, tc, "Exception caught writing ServerCommunications dump!", exception);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "dumpCommunications");
    }

    /**
     * Dump out all outbound ME to ME conversations.
     *
     * @param writer
     * @throws IOException
     */
    private void dumpMEtoMEConversations(final FormattedWriter writer) throws IOException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "dumpMEtoMEConversations", writer);

        List<Conversation> conversations = ServerConnectionManager.getRef().getActiveOutboundMEtoMEConversations();
        Map<Object, List<Conversation>> connectionToConversationMap = buildConnectionMap(conversations);

        // Dump each comms connection.
        for (final Map.Entry<Object, List<Conversation>> entry : connectionToConversationMap.entrySet()) {
            Object connectionObject = entry.getKey();
            writer.newLine();
            writer.startTag("CommsConnection");
            writer.newLine();
            writer.taggedValue("ME-ME connection", connectionObject);

            // Dump the conversations using this connection.
            for (Conversation conversation : entry.getValue()) {
                    dumpMEtoMEConversation(writer, conversation);
            }

            writer.outdent();
            writer.newLine();
            writer.endTag("CommsConnection");
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "dumpMEtoMEConversations");
    }

    /**
     * Dump the state of all inbound conversations that are currently active.
     *
     * @param writer
     * @throws IOException
     */
    private void dumpInboundConversations(final FormattedWriter writer) throws IOException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "dumpInboundConversations", writer);

        List<Conversation> conversations = ServerTransportAcceptListener.getInstance().getActiveConversations();
        Map<Object, List<Conversation>> connectionToConversationMap = buildConnectionMap(conversations);

        // Dump each comms connection.
        for (final Map.Entry<Object, List<Conversation>> entry : connectionToConversationMap.entrySet()) {
            Object connectionObject = entry.getKey();
            writer.newLine();
            writer.startTag("CommsConnection");
            writer.newLine();
            writer.taggedValue("Inbound connection", connectionObject);

            // Dump the conversations using this connection.
            for (Conversation conversation : entry.getValue()) {
                dumpServerConversation(writer, conversation);
            }

            writer.outdent();
            writer.newLine();
            writer.endTag("CommsConnection");
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "dumpInboundConversations");
    }

    /**
     * Build a map of connection -> conversation set,
     * so that we can output the connection information once per set of conversations.
     *
     * @param conversations
     * @throws IOException
     */
    private Map<Object, List<Conversation>> buildConnectionMap(List<Conversation> conversations) throws IOException {
        Map<Object, List<Conversation>> connectionToConversationMap = new HashMap<>();
        if (conversations != null) {
            for (Conversation c : conversations) {
                Object connectionObject = c.getConnectionReference();
                List<Conversation> conversationList;
                if (!connectionToConversationMap.containsKey(connectionObject)) {
                    conversationList = new LinkedList<Conversation>();
                    connectionToConversationMap.put(connectionObject, conversationList);
                } else {
                    conversationList = connectionToConversationMap.get(connectionObject);
                }
                conversationList.add(c);
            }
        }
        return connectionToConversationMap;
    }

    /**
     * Dumps the details of a server conversation.
     *
     * @param writer
     * @param conversation the conversation we want to dump.
     * @throws IOException
     */
    private void dumpServerConversation(final FormattedWriter writer, Conversation conversation) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "dumpServerConversation", new Object[] { writer, conversation });

        try {
            writer.newLine();
            writer.startTag("Conversation");
            writer.indent();

            writer.newLine();
            writer.taggedValue("Summary", conversation.getFullSummary());

            final ConversationState conversationState = (ConversationState) conversation.getAttachment();
            final List conversatiobObjects = conversationState.getAllObjects();

            for (Object object : conversatiobObjects) {
                if (object instanceof CATConnection) {
                    writer.newLine();
                    writer.taggedValue("CATConnection", (CATConnection) object);

                } else if (object instanceof ServerSideConnection) {
                    writer.newLine();
                    writer.startTag("ServerSideConnection");
                    writer.indent();
                    ServerSideConnection conn = (ServerSideConnection) object;
                    writer.newLine();
                    writer.taggedValue("toString", conn);
                    writer.newLine();
                    writer.taggedValue("CommsConnection", conn.getCommsConnection());
                    writer.newLine();
                    writer.taggedValue("Info", conn.getConnectionInfo());
                    writer.newLine();
                    writer.taggedValue("ObjectID", conn.getConnectionObjectID());
                    writer.newLine();
                    writer.taggedValue("MetaData", conn.getMetaData());
                    writer.newLine();
                    writer.taggedValue("RequestNumber", conn.getRequestNumber());
                    writer.outdent();
                    writer.newLine();
                    writer.endTag("ServerSideConnection");

                } else if (object instanceof CATOrderingContext) {
                    writer.newLine();
                    writer.taggedValue("CATOrderingContext", (CATOrderingContext) object);

                } else if (object instanceof CATMainConsumer) {
                    ((CATMainConsumer) object).dump(writer);

                } else {
                    writer.newLine();
                    writer.taggedValue("other", object);
                }
            }

            writer.outdent();
            writer.newLine();
            writer.endTag("Conversation");
        } catch (Throwable t) {
            // No FFDC Code Needed
            try {
                writer.write("\nUnable to dump conversation " + t);
            } catch ( IOException e ) {
                e.printStackTrace();
            }
        }


        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "dumpServerConversation");
    }

    /**
     * Dumps the particulars of a ME to ME client side conversation.
     *
     * @param writer
     * @param conv the conversation we want to dump.
     * @throws IOException
     */
    private void dumpMEtoMEConversation(final FormattedWriter writer, Conversation conversation) throws IOException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "dumpMEtoMEConversation", new Object[] { writer, conversation });

        try {
            writer.newLine();
            writer.startTag("Conversation");
            writer.indent();

            writer.newLine();
            writer.taggedValue("Summary", conversation.getFullSummary());

            //Get the conversation state and use it to find out what we can.
            final ConversationState convState = (ConversationState) conversation.getAttachment();
            final MEConnection commsConnection = (MEConnection) convState.getCommsConnection();

            writer.taggedValue("commsConnection", commsConnection);

            final JsMessagingEngine me = commsConnection.getMessagingEngine();
            final String meInfo = me == null ? "<null>" : me.getName() + " [" + me.getUuid() + "]";

            writer.taggedValue("Local ME: ", meInfo);
            writer.taggedValue("Target ME: ", commsConnection.getTargetInformation());

            writer.outdent();
            writer.newLine();
            writer.endTag("Conversation");
        } catch (Throwable t) {
            // No FFDC Code Needed
            try {
                writer.write("\nUnable to dump conversation " + t);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "dumpMEtoMEConversation");
    }
}
