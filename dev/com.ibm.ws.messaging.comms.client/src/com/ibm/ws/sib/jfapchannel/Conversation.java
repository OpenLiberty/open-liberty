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
package com.ibm.ws.sib.jfapchannel;

import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException;
import com.ibm.wsspi.sib.core.exception.SIConnectionLostException;
import com.ibm.ws.sib.mfp.ConnectionSchemaSet;

/**
 * This interface represents a conversation with an ME.  It attempts to hide
 * many of the complexities of how connections are handled (both by the
 * JS comms and the Channel Framework).
 * <p>
 * An instance of an implementing class is returned from the
 * CFConnectionManager upon a successful connect call, or to the
 * AcceptListener when a new connection is established.  In both cases, the
 * implementation will be in a "open" state and will have successfully
 * negotiated the initial FAP handshake flow.
 */
public interface Conversation
{
        /**
         * Special "send with lowest" priority value for send and exchange calls.
         * This magically calculates and sends a data with the lowest priority
         * of any of the currently outstanding sends.  Useful for ensuring that,
         * say, a commit doesn't overtake something inside a transaction but without
         * necessarily incuring the performance penalty of sending it with the
         * absolute lowest priority.
         */
        final int PRIORITY_LOWEST = -1;

   /**
    * JFAP Message priorities.  The range from 0x00 to 0x0F. The range 0x0D
    * to 0x0F is reserved for use by the JFAP channel itself and so therefore
    * the highest priority that anyone using the JFAP channel can
    * send a message is the value of this constant. Sending a message
    * therefore with this priority should mean that it will
    * go before any other message (except special JFAP ones).
    */
        final int PRIORITY_HIGHEST = 0x0C;                            // f174317

   /**
    * This is the lowest priority that normal messages should be sent across
    * the network with. Note, the JFAP channel itself may use messages of
    * a lower priority than this.
    */
   final int PRIORITY_LOW = 0x02;                                // f174317

   /**
    * This is the highest priority that normal messages should be sent across
    * the network with. Note, the JFAP channel itself may use messages of
    * a higher priority than this.
    */
   final int PRIORITY_HIGH = 0x0B;                               // f174317

   /**
    * This is a value that is halfway inbetween the normal low and the normal
    * high values.
    */
   final int PRIORITY_MEDIUM = 0x07;                             // f174317

   /** Priority level that heartbeats are sent using. */
   final int PRIORITY_HEARTBEAT = 0x0F;                           // F175658

   /**
    * Defines the throttling policy used to slow down sending threads when
    * the network cannot keep up.
    */
   public enum ThrottlingPolicy
   {
      BLOCK_THREAD,          // Block the calling thread
      DISCARD_TRANSMISSION,  // Discard the transmission
      DO_NOT_THROTTLE        // Do not attempt to perform throttling
   }

   /**
    * Fast close this conversation. This method should only be called when both ends
    * know that they will both call fast close at the same time. The point of fast close
    * is to close the conversation without incurring any additional communication exchanges
    * which are costly in some environments.
    */
   void fastClose();

   /**
    * Close this conversation.  If this is the only conversation using the
    * underlying socket then it is closed / re-pooled. Attempting to close an
    * already closed conversation is ignored.
    * <p>
    * Invoking this method will result in a logical close request being
    * sent to our peer.  It should respond confirming that it has closed
    * its end of the conversation.  At some later point in time the
    * physical connection is closed (once no more conversations use it).
    * <p>
    * Any threads blocked in exchange calls at either end of the
    * conversation will be woken with an exception when this method
    * is called.  If one or more exchanges is woken, then an exception
    * will be delivered to the conversation receive listener for this
    * conversation.
    *
    * @throws SIConnectionLostException
    */
   void close() throws SIConnectionLostException;                                         // F174602

   /**
    * Participate in an "exchange" with the party at the other end of the
    * conversatopm.  An exchange is defined as sending some data and then
    * blocking until we receive a full reply.
    *
    * @param data A JFapByteBuffer object that has data to be sent.
    * @param segmentType The segment type of the data to send.
    * @param requestNumber The request number to associate with the request.
    * @param priority The priority to send with the request.
    * @param canPoolOnReceive A hint which is sent to our peer to indicate
    *                          that the data being sent can be allocated from
    *                          pooled storage.
    * @throws SIConnectionLostException Something has gone wrong comms-wise.  More than
    *                        likely, someone has yanked the network cable.
    * @throws SIConnectionDroppedException The connection was closed previously due to a comms
    *                                      failure.
    * @return ReceivedData An object representing the reply from our peer.
    */
   ReceivedData exchange(JFapByteBuffer data,
                         int segmentType,
                         int requestNumber,
                         int priority,
                         boolean canPoolOnReceive)
   throws SIConnectionLostException,
          SIConnectionDroppedException;

   /**
    * Send data to our peer.  The exact behaviour of this method depends on
    * which arguments are specified.  If a value of 'null' is specified for
    * an argument, it is considered to have not been "supplied".
    * <p>
    * The byte buffer list containing a scatter gather set of WsByteBuffers
    * to send and must always be specified.  Once the data has been sent,
    * these buffers will automatically be released back into the buffer
    * pool (if any) they were allocated from.
    * <p>
    * If the receive listener is specified, then this listener is notified
    * when a reply is received (otherwise it is assumed that no reply was
    * intended).
    * <p>
    * If a send listener is specified then it is notified when the data
    * is actually transmitted (otherwise it is assumed that no notification
    * of this event is required).
    *
    * @param data A JFapByteBuffer object that has data to be sent.
    * @param segmentType The segment type of the data to send.
    * @param requestNumber The request number to associate with the request.
    *                       This must be in the range [blah - blah].
    * @param priority The priority value to send the message using.  This
    *                  must be in the range [0-15] (or be PRIORITY_LOWEST).
    * @param canPoolOnReceive A hint to our peer that the thing heading down
    *                          the wire towards it can reasonable be placed
    *                          into storage from a buffer pool (ie. isn't
    *                          a message).
    * @param throttlingPolicy Defines how to behave when the network cannot keep up
    *                         with the rate at which data is being exchanged.  The
    *                         DO_NOT_THROTTLE policy is not supported and will result
    *                         in a SIErrorException being thrown.
    * @param sendListener  The (optional) send listener to notify when the data has
    *                      actually been sent.
    *
    * @throws SIConnectionLostException Something has gone wrong comms-wise.  More than
    *                        likely, someone has yanked the network cable.
    * @throws SIConnectionDroppedException The connection was closed previously due to a comms
    *                                      failure.
    * @return The number of bytes of data that will/have been sent.  This
    *         includes the bytes of headers appended by the JFAP channel
    *         itself.
    */
   long send(JFapByteBuffer data,
             int segmentType,
             int requestNumber,
             int priority,
             boolean canPoolOnReceive,
             ThrottlingPolicy throttlingPolicy,
             SendListener sendListener)
   throws SIConnectionLostException,
          SIConnectionDroppedException;

   /**
    * Returns true if this conversation required a new socket to be created
    * (and hence is the first conversation using the socket).  This information
    * should be used to sent the once-per socket connection flows.  Since
    * multiple CFConnections may share the same underlying socket, this
    * may return false for subsequent connection attempts to the same
    * peer.
    * <p>
    * To prevent a race condition where starting conversations - if this
    * method returns true, the user of the conversation must invoke its
    * "connectionComplete" method before subsequent conversations can be
    * started.
    * @return boolean True iff this conversation required a new TCP level
    * socket to be created.
    */
   boolean isFirst();

   /**
    * Invoked by the user of the conversation after it has performed hand-shaking.
    * This method is present to avoid a potential race condition whereby two
    * successive connection attempts sharing the same socket could result in
    * one connection using the socket before the other finishes the inital
    * data flows.
    * <p>
    * When the first conversation to use a socket is established, not only will
    * the calls to "isFirst" return true, but successive conversations which
    * would be multiplexed over the same socket are blocked until the first
    * conversion invokes this method.  This allows the first conversaion using
    * a socket to do any special once only "setup" flows before any other
    * conversaion gets a chance to use the socket.
    * <p>
    * Calling this method on a close conversation will still ensure the monitors
    * are notified meaning that any subsequent connection attempts could be
    * successful.
    */
   void handshakeComplete();

   /**
    * Invoked by the user of the conversation after a handshake has failed.
    * Like the handshakeComplete method, this method is also present to stop
    * the same race condition.  It should be invoked in a similar mannor to
    * handshakeComplete but upon failure of a handshake operation.
    * <p>
    * Calling this method on a close conversation will still ensure the monitors
    * are notified meaning that any subsequent connection attempts could be
    * successful.
    */
   void handshakeFailed();

   /**
    * Attaches an arbitary object (presumably something of interest to the
    * person using this connection) to this conversation.  Any previous
    * attachment is discarded.
    * @param attachment The object to attach.
    */
   void setAttachment(Object attachment);

   /**
    * Returns any previous attachment associated with this connection.  If no-
    * one has attached anything then the value 'null' is returned.
    * @return Object The previously attached object (if any).
    */
   Object getAttachment();

        /**
         * Attaches an arbitary object to the underlying link (socket).  Any
         * previous attachment is discarded.  The link level attachment is
    * discarded when the last conversation associated with the underlying
    * link is closed.
         * @param attachment The object to attach.
         */
        void setLinkLevelAttachment(Object attachment);

        /**
         * Retrieves any attachment previously set using the setLinkLevelAttachment
         * method.  If no object has been previously attached a value of null
         * is returned.
         * @see Conversation#setLinkLevelAttachment(Object)
         * @return Object
         */
        Object getLinkLevelAttachment();

        /**
         * Returns true iff this conversation is over the same physical link
         * (socket) as the conversation passed in as an argument.
         * @param conversation The conversation to test and see if it shares
         * the same physical link as this conversation.
         * @return boolean True iff the conversation shares the same physical
         * link as this one.
         */
        boolean sharesSameLinkAs(Conversation conversation);

   /**
    * "Clones" this conversation, creating a new conversation object which
    *  shares the same underlying physical link.
    * @param receiveListener The receive listener to associated with the cloned
    *                  conversation.
    * @return Conversation The "cloned" conversation.
    *
    * @throws SIResourceException
    */
   Conversation cloneConversation(ConversationReceiveListener receiveListener) // F173772
      throws SIResourceException;

   /**
    * Returns the conversations which share the same underlying physical
    * connection as this conversation.  The list is a point in time snapshot.
    * The conversation object this method is invoked upon will be included
    * in the list.
    * @return Conversation[]
    */
   Conversation[] getConversationsSharingSameLink();              // F172837

   /**
    * Returns an ID which is unique to this conversation and scoped at the
    * underlying socket level.  This should only be used when identifying
    * one conversation to another (for example the Core API Clone Connection
    * call).
    * @return short
    */
   int getId();                                                   // F173772

   /**
    * Sets the heartbeat interval for the underlying physical connection.
    * Some caution should be taken as this may affect other conversations.
    * The change will only be picked up when the next piece of data
    * is received from our peer, so it is possible that a different
    * heartbeat interval will be used than anticipated if the physical
    * connection dies around the point this method is called.
    * @param seconds
    */
   void setHeartbeatInterval(int seconds);

   /**
    * Returns the last set value for the heartbeat interval for this
    * physical connection.
    * @return int
    */
   int getHeartbeatInterval();

   /**
    * Sets the heartbeat timeout for the underlying physical connection.
    * Some caution should be taken as this may affect other conversations.
    * The change will only be picked up the next time a heartbeat is
    * missed.
    * @param seconds
    */
   void setHeartbeatTimeout(int seconds);

   /**
    * Returns the last set value for the heartbeat timeout for this
    * physical connection.
    * @return int
    */
   int getHeartbeatTimeout();

   /**
    * Sets the maximum size for a single transmission being sent or
    * received over the associated connection.  Care should be exercised when
    * invoking this method as it cannot know about inflight transmissions
    * and transmissions that exceed the size set here will trigger an
    * error condition and the closure of the connection.
    * @param maxTransmissionSizeInBytes
    */
   void setMaxTransmissionSize(int maxTransmissionSizeInBytes);            // F181603.2

   /**
    * Returns the current maximum transmission size for the underlying
    * connection.
    * @return Returns the current maximum transmission size for the underlying
    * connection.
    */
   int getMaxTransmissionSize();                                           // F181603.2

   /**
    * Returns meta information about this conversation, or null if no data
    * is available.
    * @return A meta data object containing information about the conversation.
    */
   ConversationMetaData getMetaData();                                     // D19667.10.1

   // begin F193735.3
   /**
    * Type safe enumeration for the type of peer that this conversation is being
    * held with.  This information is used to categorise conversations into client
    * or ME use, for the purposes of PMI statistic gathering.
    */
   static final class ConversationType
   {
      private String type;
      private ConversationType(String type) {this.type = type; }
      public String toString() {return type; }
   }
   // end F193735.3

   /** Enumerated value.  Conversation used for communication with client. */
   static final ConversationType CLIENT = new ConversationType("CLIENT");              // F193735.3
   /** Enumerated value.  Conversation used for communication with Messaging Engine */
   static final ConversationType ME = new ConversationType("ME");                      // F193735.3
   /** Enumerated value.  Conversation used when we do not know who we are communicating with */
   static final ConversationType UNKNOWN = new ConversationType("UNKNOWN");            // F193735.3


   /**
    * Sets the type of peer that this conversation is being held with.  This information
    * is used for PMI statistic gathering purposes.
    * <p>
    * When creating an outboud connection, this type can automatically be set.  Based on
    * the following assumptions:
    * <ul>
    * <li> Clients always connect outbound to MEs (and not anything else)</li>
    * <li> MEs always connect outbound to other MEs (and not anything else)</li>
    * Inbound conversations cannot be categorised in this way.  Instead, this method
    * should probably be invoked inside the accept listener when the conversation type
    * has been determined.
    * <p>
    * Although an "unknown" enumerated value exists, it is not valid to set the conversation
    * type as being unknown.  All inbound conversations start out as being unknown with the
    * expectation that this will be updated once the peer has been identified.
    * @param type
    */
   void setConversationType(ConversationType type);                                    // F193735.3

   /**
    * This method can be called on a Conversation to determine if it is currently closed. Note that
    * this method should be used with care as it is of course possible that immediately after this
    * call has returned the conversation could be closed.
    * <p>
    * This method returns true when the conversation is closed or awaiting to be closed.
    *
    * @return Returns an indication as to whether the conversation is closed at this point in time.
    */
   boolean isClosed();                                                                    // D209401

   /**
    * Adds a ConnectionClosedListener for the specified conversation usage type to the set of ConnectionClosedListeners associated with the
    * Connection backing this Conversation. If there is already a ConnectionClosedListener for the specified usage type then it is overwritten.
    * <p>
    * Each listener in the list will be driven when the physical connection closes due to it timing out, due to
    * explicit user intervention or if the connection fails. Once an individual listener has been driven,
    * it will never be driven again.
    *
    * @param listener
    * @param usageType
    */
   void addConnectionClosedListener(ConnectionClosedListener listener, ConversationUsageType usageType);

   /**
    * Get the ConnectionClosedListener for the specified conversation usage type that is associated with the
    * underlying Connection or null if there is none.
    *
    * @param type
    * @return
    */
   ConnectionClosedListener getConnectionClosedListener(ConversationUsageType usageType);

   /**
    * Associates a set of handshake properties with the link underpinning this conversation.
    * These properties are preserved until being overwritten by a subsequent use of this
    * method, or until the link itself is physically closed.
    * @param handshakeProperties
    */
   void setHandshakeProperties(HandshakeProperties handshakeProperties);

   /**
    * @return the last set of properties associated using the setHandshakeProperties method.
    * A value of null is returned if the setHandshakeProperties method has never been invoked
    * on any conversation belonging to a link.
    */
   HandshakeProperties getHandshakeProperties();

   /**
    * @return Returns a reference to the underlying connection that this Conversation is using. As
    *         the interface is not public, this can only be used to reference the connection.
    */
   ConnectionInterface getConnectionReference();

   /**
    * @return Returns a detailed summary about this particular Conversation including the toString()
    *         information and any data saved in the Conversations event recorder.
    */
   String getFullSummary();

   /**
    * Sets the default ConversationReceiveListener on a Conversation.
    * This will be used when we don't know which ConversationReceiveListener to route dispatches to.
    *
    * @param drl
    */
   public void setDefaultReceiveListener(ConversationReceiveListener drl);

   /**
    * setSchemaSet
    * Sets the schemaSet in the underlying Connection.
    *
    * @param schemaSet   The SchemaSet which pertains to the Connection.
    */
   public void setSchemaSet(ConnectionSchemaSet schemaSet);

   /**
    * getSchemaSet
    * Returns the MFP SchemaSet which pertains to the underlying Connection.
    *
    * @throws SIConnectionDroppedException The connection was closed previously due to a comms failure.
    *
    * @return ConnectionSchemaSet The SchemaSet belonging to the underlying Connection.
    */
   public ConnectionSchemaSet getSchemaSet() throws SIConnectionDroppedException;
   
   /**
    * Check to see if the given request number is free
    * @param requestNumber is the request number to check
    * @return true if the request number is free
    */
   public boolean checkRequestNumberIsFree(int requestNumber);
}
