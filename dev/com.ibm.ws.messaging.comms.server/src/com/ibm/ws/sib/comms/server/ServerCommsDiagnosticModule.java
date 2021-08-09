/*******************************************************************************
 * Copyright (c) 2003, 2021 IBM Corporation and others.
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
import com.ibm.wsspi.sib.core.SICoreConnection;

/**
 * This class is an implementation of a FFDC diagnostic module that provides information about
 * inbound conversations and ME to ME conversations. This extends the client version which only provides information about
 * outbound client conversations.
 * 
 * @author Gareth Matthews
 */
public class ServerCommsDiagnosticModule extends ClientCommsDiagnosticModule
{
   /** Register Class with Trace Component */
   private static final TraceComponent tc = SibTr.register(ServerCommsDiagnosticModule.class,
                                                           CommsConstants.MSG_GROUP,
                                                           CommsConstants.MSG_BUNDLE);
   
   /** The singleton instance */
   private static ServerCommsDiagnosticModule _serverSingleton = null;

   /**
    * @return Returns the singleton instance of this class.
    */
   public static ServerCommsDiagnosticModule getInstance()
   {
      if (_serverSingleton == null)
      {
         _serverSingleton = new ServerCommsDiagnosticModule();
      }
      return _serverSingleton;
   }
   
   /**
    * Private constructor.
    */
   private ServerCommsDiagnosticModule()
   {      
   }

   /**
    * Create an instance and create the dump.
    */
   public static void dump(FormattedWriter writer, String arg) {

       ServerCommsDiagnosticModule.getInstance().dumpCommunications(writer, arg);;

   }




   /**
    * Dump all information relating to server side comms. 
    * 
    * @param is the incident stream to log information to.
    */
   @Override
   protected void dumpJFapServerStatus(final IncidentStream is)
   {
      if(TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "dumpJFapServerStatus", is);
      
      dumpMEtoMEConversations(is);
      dumpInboundConversations(is);
      
      if(TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "dumpJFapServerStatus");
   }

   /**
    * Dump out all outbound ME to ME conversations.
    * 
    * @param is the incident stream to log information to.
    */
   private void dumpMEtoMEConversations(final IncidentStream is) 
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "dumpMEtoMEConversations", is);
      
      final ServerConnectionManager scm = ServerConnectionManager.getRef();
      final List obc = scm.getActiveOutboundMEtoMEConversations();
      
      is.writeLine("", "");
      is.writeLine("\n------ ME to ME Conversation Dump ------ ", ">");
      
      if (obc != null)
      {
         //Build a map of connection -> conversation so that we can output the
         //connection information once per set of conversations.
         final Map<Object, LinkedList<Conversation>> connectionToConversationMap = convertToMap(is, obc);

         //Go through the map and dump out a connection - followed by its conversations
         for (final Iterator<Entry<Object,LinkedList<Conversation>>>i = connectionToConversationMap.entrySet().iterator(); i.hasNext();)
         {
            final Entry<Object, LinkedList<Conversation>>entry = i.next();
            is.writeLine("\nOutbound connection:", entry.getKey());

            final LinkedList conversationList = entry.getValue();
            while(!conversationList.isEmpty())
            {
               final Conversation c = (Conversation)conversationList.removeFirst();
               is.writeLine("\nOutbound Conversation[" + c.getId() + "]: ", c.getFullSummary());
               
               try
               {
                  dumpMEtoMEConversation(is, c);
               }
               catch(Throwable t)
               {
                  // No FFDC Code Needed
                  is.writeLine("\nUnable to dump conversation", t);
               }        
            }
         }
      }
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "dumpMEtoMEConversations");
   }
   
   /**
    * This method dumps the status of any inbound conversations that are currently active. It does
    * this by asking the accept listeners for their list of active conversations and dumping out the
    * details in their conversation states.
    * 
    * @param is the incident stream to log information to.
    */
   private void dumpInboundConversations(final IncidentStream is) 
   {
      if(TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "dumpInboundConversations", is);
      
      final List serverConvs = ServerTransportAcceptListener.getInstance().getActiveConversations();
      
      is.writeLine("", "");
      is.writeLine("\n------ Inbound Conversation Dump ------ ", ">");
      
      if (serverConvs != null)
      {
         // Build a map of connection -> conversation so that we can output the
         // connection information once per set of conversations.
         final Map<Object, LinkedList<Conversation>> connectionToConversationMap = new HashMap<Object, LinkedList<Conversation>>();
         
         for (final Iterator i = serverConvs.iterator(); i.hasNext();)
         {
            try
            {
               final Conversation c = (Conversation) i.next();
               
               final Object connectionObject = c.getConnectionReference();
               final LinkedList<Conversation> conversationList;
               if (!connectionToConversationMap.containsKey(connectionObject))
               {
                  conversationList = new LinkedList<Conversation>();
                  connectionToConversationMap.put(connectionObject, conversationList);
               }   
               else
               {
                  conversationList = connectionToConversationMap.get(connectionObject);
               }
               conversationList.add(c);
               
            }
            catch (Throwable t)
            {
               // No FFDC Code Needed
               is.writeLine("\nUnable to dump conversation", t);
            }
         }

         // Go through the map and dump out a connection - followed by its conversations
         final Set<Map.Entry<Object, LinkedList<Conversation>>> entries = connectionToConversationMap.entrySet();
         for(final Map.Entry<Object, LinkedList<Conversation>> entry: entries)
         {
            final Object connectionObject = entry.getKey();
            is.writeLine("\nInbound connection:", connectionObject);

            final LinkedList<Conversation> conversationList = entry.getValue();
            while(!conversationList.isEmpty())
            {
               final Conversation c = conversationList.removeFirst();
               is.writeLine("\nInbound Conversation[" + c.getId() + "]: ", c.getFullSummary());
               try
               {
                  dumpServerConversation(is, c);
               }
               catch(Throwable t)
               {
                  // No FFDC Code Needed
                  is.writeLine("\nUnable to dump conversation", t);
               }
            }
         }
      }
      
      if(TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "dumpInboundConversations");
   }
   
   /**
    * Dumps the details of a particular server conversation.
    * 
    * @param is the incident stream to log information to.
    * @param conv the conversation we want to dump.
    */
   private void dumpServerConversation(IncidentStream is, Conversation conv)
   {
      if(TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "dumpServerConversation", new Object[]{is, conv});
      
      final ConversationState convState = (ConversationState) conv.getAttachment();
      
      final List allObjs = convState.getAllObjects();
      is.writeLine("Number of associated resources", allObjs.size());
      
      for (final Iterator i2 = allObjs.iterator(); i2.hasNext();)
      {
         final Object obj = i2.next();
         if (obj instanceof SICoreConnection)
         {
            final SICoreConnection conn = (SICoreConnection) obj;
            is.writeLine("  ", 
                         "SICoreConnection@" + Integer.toHexString(obj.hashCode()) + ": " +
                         "ME Name: " + conn.getMeName() + " [" + conn.getMeUuid() + "] " +
                         "Version: " + conn.getApiLevelDescription());
         }
         else
         {
            is.writeLine("  ", obj);
         }         
      }
      
      if(TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "dumpServerConversation");
   }
   
   /**
    * Dumps the particulars of a ME to ME client side conversation.
    * 
    * @param is the incident stream to log information to.
    * @param conv the conversation we want to dump.
    */
   private void dumpMEtoMEConversation(IncidentStream is, Conversation conv)
   {
      if(TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "dumpMEtoMEConversation", new Object[]{is, conv});
      
      //Get the conversation state and use it to find out what we can.
      final ConversationState convState = (ConversationState) conv.getAttachment();
      final MEConnection commsConnection = (MEConnection) convState.getCommsConnection();
            
      is.writeLine("  Connected using: ", commsConnection);
      
      final JsMessagingEngine me = commsConnection.getMessagingEngine();
      final String meInfo = me == null ? "<null>" : me.getName()+ " [" + me.getUuid() + "]";
      
      is.writeLine("  Local ME: ", meInfo);
      is.writeLine("  Target ME: ", commsConnection.getTargetInformation());
                
      //Introspect details of conversation state.
      is.introspectAndWriteLine("Introspection of the conversation state:", convState);
      
      if(TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "dumpMEtoMEConversation");
   }




   // Adding a set of new methods so that a dump can be taken independently of the FFDC creation.
   // There's a lot of duplication here, which isn't great, but for the dump we want to be able to use a FormattedWriter,
   // which uses a different API to format and write the output.




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

       try {
           List<Conversation> conversations = ServerConnectionManager.getRef().getActiveOutboundMEtoMEConversations();
           Map<Object, LinkedList<Conversation>> connectionToConversationMap = buildConnectionMap(conversations);

           // Dump each comms connection.
           for (final Map.Entry<Object, LinkedList<Conversation>> entry : connectionToConversationMap.entrySet()) {
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
       }
       catch (Throwable t) {
	 if (writer != null) writer.write("\nUnable to dump MEtoMEConversations " + this + " " + t);
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

       try {
           List<Conversation> conversations = ServerTransportAcceptListener.getInstance().getActiveConversations();
           Map<Object, LinkedList<Conversation>> connectionToConversationMap = buildConnectionMap(conversations);

           // Dump each comms connection.
           for (final Map.Entry<Object, LinkedList<Conversation>> entry : connectionToConversationMap.entrySet()) {
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
       }
       catch (Throwable t) {
	 if (writer != null) writer.write("\nUnable to dump InboundConversations " + this + " " + t);
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
 /*
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
*/

   private Map<Object, LinkedList<Conversation>> buildConnectionMap(final List<Conversation> serverConvs) {

       // Build a map of connection -> conversation so that we can output the
       // connection information once per set of conversations.
       final Map<Object, LinkedList<Conversation>> connectionToConversationMap = new HashMap<Object, LinkedList<Conversation>>();

	      if (serverConvs != null)
	      {

	         for (Conversation c : serverConvs) {

			 try {
		               final Object connectionObject = c.getConnectionReference();
		               final LinkedList<Conversation> conversationList;
		               if (!connectionToConversationMap.containsKey(connectionObject))
		               {
		                  conversationList = new LinkedList<Conversation>();
		                  connectionToConversationMap.put(connectionObject, conversationList);
		               }
		               else
		               {
		                  conversationList = connectionToConversationMap.get(connectionObject);
		               }
		               conversationList.add(c);
			 }
			 catch(Throwable t) {
				// No FFDC Code Needed
			 }
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
