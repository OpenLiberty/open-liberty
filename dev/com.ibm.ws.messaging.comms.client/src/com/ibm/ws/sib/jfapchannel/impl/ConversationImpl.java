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
package com.ibm.ws.sib.jfapchannel.impl;

import java.net.InetAddress;
import java.util.Iterator;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.jfapchannel.ConnectionClosedListener;
import com.ibm.ws.sib.jfapchannel.ConnectionInterface;
import com.ibm.ws.sib.jfapchannel.Conversation;
import com.ibm.ws.sib.jfapchannel.ConversationMetaData;
import com.ibm.ws.sib.jfapchannel.ConversationReceiveListener;
import com.ibm.ws.sib.jfapchannel.ConversationUsageType;
import com.ibm.ws.sib.jfapchannel.DispatchQueue;
import com.ibm.ws.sib.jfapchannel.Dispatchable;
import com.ibm.ws.sib.jfapchannel.HandshakeProperties;
import com.ibm.ws.sib.jfapchannel.JFapByteBuffer;
import com.ibm.ws.sib.jfapchannel.JFapChannelConstants;
import com.ibm.ws.sib.jfapchannel.ReceiveListener;
import com.ibm.ws.sib.jfapchannel.ReceivedData;
import com.ibm.ws.sib.jfapchannel.SendListener;
import com.ibm.ws.sib.jfapchannel.buffer.WsByteBuffer;
import com.ibm.ws.sib.jfapchannel.impl.eventrecorder.ConnectionEventRecorderFactory;
import com.ibm.ws.sib.jfapchannel.impl.eventrecorder.ConversationEventRecorder;
import com.ibm.ws.sib.jfapchannel.impl.rldispatcher.AbstractInvocation;
import com.ibm.ws.sib.jfapchannel.impl.rldispatcher.ReceiveListenerDispatcher;
import com.ibm.ws.sib.mfp.ConnectionSchemaSet;
import com.ibm.ws.sib.utils.Semaphore;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException;
import com.ibm.wsspi.sib.core.exception.SIConnectionLostException;

/**
 * Implementation of a conversation.
 * <p>
 * The logic required to close a conversation is a little convoluted.  The following
 * state diagram shows the internal states that the conversation transitions through.
 * <p>
 * <img src="doc-files/conversation-states.jpg">
 * <p>
 * Key:<br>
 * <table>
 * <tr><th>Transition name</th><th>Details</th></tr>
 * <tr><td>Receive close request</td>
 *     <td>Our peer sends us a close request.  This causes the
 *         processLogicalCloseRequest method to be driven.</td></tr>
 * <tr><td>Receive close response</td>
 *     <td>Our peer sends us a close response.  This causes the
 *         processLogicalCloseResponse method to be driven.</td></tr>
 * <tr><td>Send close request</td>
 *     <td>We send a close request to our peer.  This is handled by invoking the
 *         sendLogicalClose method with a argument of <i>true</i></td></tr>
 * <tr><td>Send close response</td>
 *     <td>We send a close response to our peer.  This is handled by invoking the
 *         sendLogicalClose method with a argument of <i>false</i></td></tr>
 * <tr><td>Send close request completes</td>
 *     <td>We receive notification that a logical close request we sent has been
 *         transmitted to (but not necessarily received by) our peer.</td></tr>
 * <tr><td>Receive close() invocation</td>
 *     <td>The close() method has been invoked on this class.</td></tr>
 * </table>
 */
public class ConversationImpl implements Conversation, Dispatchable                       // F201521
{
   private static final TraceComponent tc = SibTr.register(ConversationImpl.class, JFapChannelConstants.MSG_GROUP, JFapChannelConstants.MSG_BUNDLE);

   private static final TraceNLS nls = TraceNLS.getTraceNLS(JFapChannelConstants.MSG_BUNDLE);

   //@start_class_string_prolog@
   public static final String $sccsid = "@(#) 1.105 SIB/ws/code/sib.jfapchannel.client.common.impl/src/com/ibm/ws/sib/jfapchannel/impl/ConversationImpl.java, SIB.comms, WASX.SIB, uu1215.01 10/05/28 04:39:10 [4/12/12 22:14:14]";
   //@end_class_string_prolog@

   static {
     if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Source Info: " + $sccsid);
   }

   // The ID assigned to this conversation.
   private short id = 0;

   // Is this the first conversation to take place over this connection?
   private boolean first = false;

   // The connection the conversation is taking place over.
   private Connection connection = null;

   // A table used to store outstanding requests that this conversation is expecting
   // replies for.
   private RequestIdTable reqIdTable = null;

   // The "user" attachment for this conversation (if any)
   private Object attachment = null;

   // The receive listener any transmissions not listed in the reqIdTable will be
   // routed to.
   private volatile ConversationReceiveListener defaultReceiveListener = null;

   // Mechanism to close timing window between conversation being passed to
   // accept listener and having a default receive listener associated with it.
   private Object defaultReceiveListenerLock = new Object();                  // F174772

   // Mechanism to close timing window between conversation being passed to
   // accept listener and having a default receive listener associated with it.
   private Semaphore waitForDefaultReceiveListenerSemaphore = new Semaphore();// F174772

   // The monotomically increasing counter value associated with this conversation
   // instance.
   private static int instanceCounter = 0;                                    // D185831

   // Monotomically increasing counter used to assign conversation instance counter
   // values.
   private int thisInstanceCounter = 0;                                       // D185831

   // Lock taken when modifying dispatch queue or associated reference count.
   private Object dispatchLockObject = new Object();                          // D185831

   // Dispatch queue currently associated with conversation (if any)
   private DispatchQueue dispatchQueue = null;                                // D185831  // F201521

   // Number of times this conversation has been queued into the associated
   // dispatch queue.
   private int dispatchQueueReferenceCount = 0;                               // D185831

   // Does this conversation reside on the client side?  Primarily used to
   // determine if we can skip dispatching to the receive listener dispatcher.
   private boolean onClientSide = false;                                      // F181603.2

   // A counter that indicates how many outstanding requests there are waiting
   // this Conversation. These may be dispatched to different dispatch queues
   // but we need this so that we can ensure we deliver
   private int totalOutstandingRequests = 0;                                  // F201521

   // A lock to be used to ensure integrity of the request count
   private Object totalOutstandingRequestsLock = new Object();                // F201521

   // When an error occurs on this conversation it is saved here so that it can
   // be executed when all the outstanding requests for this conversation have
   // been processed.
   private AbstractInvocation errorOccurredInvocation = null;                 // F201521

   protected String description = "";

   private Object sendMonitor = new Object();                                 // D262663
   private boolean inSend = false;                                            // D262663
   private boolean invalidateOutstanding = false;                             // D262663
   private Throwable invalidateOutstandingThrowable = null;                   // D262663

   private final Semaphore waitForCloseToCompleteSemaphore = new Semaphore();

   // begin D203646
   /** Enumeration for state. */
   private static class StateEnum
   {
      private String toStringName;
      private StateEnum(String toStringName)
      {
         this.toStringName = "STATE: "+toStringName;
      }
      public String toString()
      {
         return toStringName;
      }
   }

   // begin D273932
   // States that a conversation can be in.
   // The transitions between these states is described in the header comment.
   private static final StateEnum OPEN = new StateEnum("open");                        // D203646
   private static final StateEnum NOTIFY_PEER = new StateEnum("notify peer");
   private static final StateEnum AWAITING_PEER1 = new StateEnum("awaiting peer 1");
   private static final StateEnum PARALLEL_CLOSE1 = new StateEnum("parallel close 1");
   private static final StateEnum AWAITING_PEER2 = new StateEnum("awaiting peer 2");
   private static final StateEnum PARALLEL_CLOSE2 = new StateEnum("parallel close 2");
   private static final StateEnum AWAITING_PEER3 = new StateEnum("awaiting peer 3");
   private static final StateEnum CLOSED = new StateEnum("closed");                    // D203646
   // end D273932

   // Monitor object which must be held whilst testing, or transitioning between
   // conversation states.  Because send operations can end up driving notification
   // callbacks on the same thread (but for a different conversation).  This monitor
   // should _not_ be held across network operations.
   private final Object stateChangeMonitor = new Object();                             // D203646

   // The current state.
   private StateEnum state = OPEN;                                                     // D203646
   // end 203646

   private final ConversationEventRecorder eventRecorder;

   private ExchangeReceiveListenerPool exchangeReceiveListenerPool = new ExchangeReceiveListenerPool(8);

   /**
    * @return Returns info about this instance.
    * @see java.lang.Object#toString()
    */
   public String toString()
   {
      return getClass() + "@" + System.identityHashCode(this) +
              " id: " + id +
              " first: " + first +
              " " + state +                                                               // D209401
              " connection: " + System.identityHashCode(connection) +
              " onClientSide: " + onClientSide;
   }

   /**
    * @return Returns the toString() information plus all the information saved in the event
    *         recorder.
    */
   public String getFullSummary()
   {
      return toString() + " " + eventRecorder;
   }

   /**
    * Dummy constructor.  Used to create a ConversationImpl which is required for reasons of
    * "type convenience" rather than an instance which will actually have methods invoked upon it.
    * See ConversationTable for an example of where this comes in useful.
    */
   protected ConversationImpl()
   {
          if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "<init>");

          this.eventRecorder =
         ConnectionEventRecorderFactory.getConnectionEventRecorder().getConversationEventRecorder();

          if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "<init>");
   }

   /**
    * Creates a new conversation.
    * @param id
    * @param first
    * @param connection
    * @param drl
    */
   //Venu Liberty change: changed constructor access from protected to public
   public ConversationImpl(short id,
                              boolean first,
                              Connection connection,
                              ConversationReceiveListener drl)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "<init>", new Object[] {""+id, ""+first, connection, drl});

      this.id = id;
      this.first = first;
      this.connection = connection;
      if (connection != null)
      {
         this.eventRecorder = connection.getConnectionEventRecorder().getConversationEventRecorder();
      }
      else
      {
         // this is a unit test "special"...  we shouldn't enter this branch of code unless
         // we are running in the unit test environment.
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "entered unit test code branch!");
         this.eventRecorder =
            ConnectionEventRecorderFactory.getConnectionEventRecorder().getConversationEventRecorder();
      }
      reqIdTable = new RequestIdTable();
      defaultReceiveListener = drl;
      thisInstanceCounter = instanceCounter++;        // D185831
      if (instanceCounter < 0) instanceCounter = 0;   // D185831
      description = "ConvId:"+id;                     // D224570

      //@stoptracescan@
      if (TraceComponent.isAnyTracingEnabled()) JFapUtils.debugSummaryMessage(tc, connection, this, "New conversation started");
      //@starttracescan@

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "<init>");
   }

   /**
    * @see Conversation#setDefaultReceiveListener(ConversationReceiveListener)
    */
   public void setDefaultReceiveListener(ConversationReceiveListener drl)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "setDefaultReceiveListener", drl);

      defaultReceiveListener = drl;
      waitForDefaultReceiveListenerSemaphore.post();

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "setDefaultReceiveListener");
   }

   /**
    * Returns the request ID table being used by this conversation.
    * @return RequestIdTable
    */
   protected RequestIdTable getRequestIdTable()
        {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getRequestIdTable");
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getRequestIdTable", reqIdTable);
      return reqIdTable;
   }

  /**
   * Retrieves the default receive listener for this conversation.
   * @return ReceiveListener
   */
   public ConversationReceiveListener getDefaultReceiveListener()    // F174776           // F201521
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getDefaultReceiveListener");
      synchronized(defaultReceiveListenerLock)
      {
         if (defaultReceiveListener == null)
         waitForDefaultReceiveListenerSemaphore.waitOnIgnoringInterruptions();
      }
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getDefaultReceiveListener", defaultReceiveListener);
      return defaultReceiveListener;
   }

   // This fast close method should only be called in situations where it is understood that both ends will perform
   // a fastclose at the same time. The reason for calling fast close is to avoid an exchange of SEGMENT_LOGICAL_CLOSE
   // flows which are costly, particular on z/OS
   public void fastClose () {
     if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "fastClose");

     if (!isClosed()) { // Only close if not already closed - once closed we can't be reopened so no need to sync across test & closing
       synchronized(stateChangeMonitor) {
         state = CLOSED;
       }

       connection.removeConversationById(id);
       connection.closeNotification(this);
       wakeupAllWithException(new SIConnectionLostException("Conversation was fast closed"), false);
       eventRecorder.logDebug("state transition: open -> closed (fast)");
     } else {
       synchronized(stateChangeMonitor) {
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "fastclose invoked in state "+state+" - ignoring");
         eventRecorder.logDebug("fastclose invoked in state "+state+" - ignoring");
       }
     }

     if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "fastClose");
   }

   // begin D203646
   /**
    * Closes this conversation.
    * @throws SIConnectionLostException
    */
   // begin D273932
   public void close() throws SIConnectionLostException
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "close");

      eventRecorder.logEntry("close");
      boolean sendClose = false;                                                          // D262663
      synchronized(stateChangeMonitor)
      {
         if (state == OPEN)
         {
            sendClose = true;                                                             // D262663
            state = NOTIFY_PEER;
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "state transition: open -> notify peer");
            eventRecorder.logDebug("state transition: open -> notify peer");
         }
         else
         {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "close invoked in state "+state+" - ignoring");
            eventRecorder.logDebug("close invoked in state "+state+" - ignoring");
         }
      }
      if (sendClose)
      {
         sendLogicalClose(true);                                              // D262663
         if (onClientSide)
         {
            // Block this thread until the logical close operation completes.  This will be
            // completed by a TCP channel thread.  We only do this on the client side, where
            // we are blocking a user thread.  Were we to do this on the server side then we
            // could be blocking a TCP channel thread, and would deadlock ourself as the
            // thread could not continue and perform the close processing that would unblock.
            waitForCloseToCompleteSemaphore.waitOnIgnoringInterruptions();
         }
      }
      eventRecorder.logExit("close");

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "close");
   }
   // end D203646, DD273932

   // begin D203646
   /**
    * Invalidates this conversation.
    * @param throwable
    */
   public void invalidate(Throwable throwable)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "invalidate", throwable);
      eventRecorder.logEntry("invalidate");

      // begin D262663, DD273932
      synchronized(sendMonitor)
      {
         if (inSend)
         {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "send currently in progress - delaying");
            eventRecorder.logDebug("send in progress - delaying invalidation");
            invalidateOutstanding = true;
            invalidateOutstandingThrowable = throwable;
         }
         else
         {
            boolean wakeup = false;
            synchronized(stateChangeMonitor)
            {
               if (state != CLOSED)
               {
                  //@stoptracescan@
                  if (TraceComponent.isAnyTracingEnabled()) JFapUtils.debugSummaryMessage(tc, connection, this, "Conversation invalidated!");
                  //@starttracescan@
                  eventRecorder.logDebug("conversation invalidated!");
                  connection.removeConversationById(id);
               }
               wakeup = true;
               state = CLOSED;
               waitForCloseToCompleteSemaphore.post();
            }
            if (wakeup)
            {
               SIConnectionLostException clException = null;
               if (throwable instanceof SIConnectionLostException)
               {
                  clException = (SIConnectionLostException)throwable;
               }
               else
               {
                  // Grab some data about this message
                  ConversationMetaData m = getMetaData();
                  String remoteHostAddress = "<Unknown>";
                  String remoteHostPort = "<Unknown>";
                  String chainName = m.getChainName();

                  InetAddress addr = m.getRemoteAddress();
                  if (addr != null)
                  {
                     remoteHostAddress = addr.getHostAddress();
                  }
                  remoteHostPort = ""+m.getRemotePort();

                  if (m.isInbound())
                  {
                     clException = new SIConnectionLostException(
                        nls.getFormattedMessage("CONVERSATIONIMPL_INVALIDATE_SICJ0045",
                                                new Object[] {remoteHostAddress, chainName} , null),
                        throwable
                     );
                  }
                  else
                  {
                     clException = new SIConnectionLostException(
                        nls.getFormattedMessage("CONVERSATIONIMPL_INVALIDATE_OUTBOUND_SICJ0072",
                                                new Object[] {remoteHostAddress, remoteHostPort, chainName} , null),
                        throwable
                     );
                  }
               }

               wakeupAllWithException(clException, true);
            }
         }
      }
      // end D262663
      eventRecorder.logExit("invalidate");
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "invalidate");
   }
   // end D203646, D273932

   // begin D203646, D273932
   /**
    * Process a request from our peer to close this conversation.
    */
   private void processLogicalCloseRequest()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "processLogicalCloseRequest");
      eventRecorder.logEntry("processLogicalCloseRequest");
      boolean invalidate = false;
      boolean sendCloseResponse = false;
      boolean close = false;
      synchronized(stateChangeMonitor)
      {
         if (state == OPEN)
         {
            state = CLOSED;
            sendCloseResponse = true;
            close = true;
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "state transition: open -> closed");
            eventRecorder.logDebug("state transition: open -> closed");
         }
         else if (state == NOTIFY_PEER)
         {
            state = AWAITING_PEER2;
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "state transition: notify peer -> awaiting peer 2");
            eventRecorder.logDebug("state transition: notify peer -> awaiting peer 2");
         }
         else if (state == AWAITING_PEER1)
         {
            state = PARALLEL_CLOSE1;
            sendCloseResponse = true;
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "state transition: awaiting peer 1 -> parallel close 1");
            eventRecorder.logDebug("state transition: awaiting peer 1 -> parallel close 1");
         }
         else
         {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Invalid state transition. Received processLogicalClose whilst in state "+state);
            eventRecorder.logError("invalid state transition.  state="+state+" transition=logical close request");
            FFDCFilter.processException
            (new Exception(), "com.ibm.ws.sib.jfapchannel.impl.ConversationImpl", JFapChannelConstants.CONVIMPL_PROCESSLOGICALREQUEST_01, new Object[] {state, eventRecorder.toString()});
            invalidate = true;
         }
      }
      if (sendCloseResponse) sendLogicalClose(false);
      if (close)
      {
         wakeupAllWithException(new SIConnectionLostException("Connection lost after peer responded to a logical close request before all conversations were closed"), false);
         //@stoptracescan@
         if (TraceComponent.isAnyTracingEnabled()) JFapUtils.debugSummaryMessage(tc, connection, this, "Conversation closed (processLogicalCloseResponse)");
         //@starttracescan@
         connection.removeConversationById(id);
         connection.closeNotification(this);
         waitForCloseToCompleteSemaphore.post();
      }
      if (invalidate) connection.invalidate(false, new SIConnectionLostException("Connection closed as part of processing a logical close request"), "error during logical close"); // D224570
      eventRecorder.logExit("processLogicalCloseRequest");
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "processLogicalCloseRequest");
   }
   // end D203646, D273932

   // begin D203646, D273932
   /**
    * Invoked when our peer responds to a logical close request.
    */
   private void processLogicalCloseResponse()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "processLogicalCloseResponse");
      eventRecorder.logEntry("processLogicalCloseResponse");
      boolean wakeup = false;
      boolean invalidate = false;
      boolean sendCloseResponse = false;
      synchronized(stateChangeMonitor)
      {
         if (state == AWAITING_PEER2)
         {
            state = PARALLEL_CLOSE2;
            sendCloseResponse = true;
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "state transition: awaiting peer 2 -> parallel close 2");
            eventRecorder.logDebug("state transition: awaiting peer 2 -> parallel close 2");
         }
         else if (state == PARALLEL_CLOSE1)
         {
            wakeup = true;
            //@stoptracescan@
            if (TraceComponent.isAnyTracingEnabled()) JFapUtils.debugSummaryMessage(tc, connection, this, "Conversation closed (processLogicalCloseResponse)");
            //@starttracescan@
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "state transition: parallel close 1 -> closed");
            eventRecorder.logDebug("state transition: parallel close 1 -> closed");
            connection.removeConversationById(id);
            state = CLOSED;
            connection.closeNotification(this);
            waitForCloseToCompleteSemaphore.post();
         }
         else if (state == NOTIFY_PEER)
         {
            state = AWAITING_PEER3;
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "state transition: notify peer -> awaiting peer 3");
            eventRecorder.logDebug("state transition: notify peer -> awaiting peer 3");
         }
         else if (state == AWAITING_PEER1)
         {
            wakeup = true;
            //@stoptracescan@
            if (TraceComponent.isAnyTracingEnabled()) JFapUtils.debugSummaryMessage(tc, connection, this, "Conversation closed (processLogicalCloseResponse)");
            //@starttracescan@
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "state transition: awaiting peer 1 -> closed");
            eventRecorder.logDebug("state transition: awaiting peer 1 -> closed");
            connection.removeConversationById(id);
            state = CLOSED;
            connection.closeNotification(this);
            waitForCloseToCompleteSemaphore.post();
         }
         else
         {
            invalidate = true;
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "processLogicalCloseResponse invoked when in state: "+state);
            eventRecorder.logError("invalid state transition.  state="+state+" transition=logical close response");
            FFDCFilter.processException
            (new Exception(), "com.ibm.ws.sib.jfapchannel.impl.ConversationImpl", JFapChannelConstants.CONVIMPL_PROCESSLOGICALRESPONSE_01, new Object[] {state, eventRecorder.toString()});
         }
      }
      if (sendCloseResponse) sendLogicalClose(false);
      if (wakeup) wakeupAllWithException(new SIConnectionLostException("Connection lost after peer responded to a logical close request before all conversations were closed"), false);
      if (invalidate) connection.invalidate(false, new SIConnectionLostException("Connection invalidated after peer responded to a logical close request "+state), "received unexpected logical close response");  // D224570
      eventRecorder.logExit("processLogicalCloseResponse");
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "processLogicalCloseResponse");
   }
   // end D203646, D273932

   // begin D203646, D273932
   /**
    * Helper method that sends a logical close flow
    */
   private void sendLogicalClose(boolean request)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "sendLogicalClose", ""+request);
      eventRecorder.logEntry("sendLogicalClose");

      // Create a new implementation of the JFapByteBuffer with a single byte in it
      InternalJFapByteBuffer buffer = new InternalJFapByteBuffer();
      if (request)
         buffer.put((byte)0);
      else
         buffer.put((byte)1);

      // begin D273932
      LogicalCloseSendListener listener = null;
      if (request)
      {
         // Only use the send listener if this is a request to close.  We do not need notification
         // for sending close responses.
         listener = new LogicalCloseSendListener();
         eventRecorder.logDebug("LogicalCloseSendListener hashcode: "+Integer.toHexString(System.identityHashCode(listener)));
      }
      try
      {
         connection.send(buffer,
                         JFapChannelConstants.SEGMENT_LOGICAL_CLOSE,
                         id,
                         0,
                         Conversation.PRIORITY_LOWEST,
                         true,
                         false,
                         ThrottlingPolicy.DO_NOT_THROTTLE,
                         listener,
                         this,
                         false);


      }
      catch (SIException e)
      {
         FFDCFilter.processException
            (e, "com.ibm.ws.sib.jfapchannel.impl.ConversationImpl", JFapChannelConstants.CONVIMPL_CLOSE_01, eventRecorder.toString());

         if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.exception(this, tc, e);
         connection.invalidate(false, e, "SIException thrown from system send"); // D224570
         waitForCloseToCompleteSemaphore.post();
      }
      eventRecorder.logExit("sendLogicalClose");
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "sendLogicalClose");
   }
   // end D203646, D273932

   // D273932
   /**
    * Invoked by the send listener callback when the sending of a logical close
    * completes.  This notification is used to update the internal state of the
    * conversation as part of logically closing the conversation.
    */
   private void sendCompletes()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "sendCompletes");

      boolean sendCloseResponse = false;
      boolean wakeup = false;
      synchronized(stateChangeMonitor)
      {
         if (state == NOTIFY_PEER)
         {
            state = AWAITING_PEER1;
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "state transition: notify peer -> awaiting peer 1");
            eventRecorder.logDebug("state transition: notify peer -> awaiting peer 1");
         }
         else if (state == AWAITING_PEER2)
         {
            state = PARALLEL_CLOSE1;
            sendCloseResponse = true;
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "state transition: awaiting peer 2 -> parallel close 1");
            eventRecorder.logDebug("state transition: awaiting peer 2 -> parallel close 1");
         }
         else if (state == PARALLEL_CLOSE2)
         {
            wakeup = true;
            //@stoptracescan@
            if (TraceComponent.isAnyTracingEnabled()) JFapUtils.debugSummaryMessage(tc, connection, this, "Conversation closed (processLogicalCloseResponse)");
            //@starttracescan@
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "state transition: parallel close 2 -> closed");
            eventRecorder.logDebug("state transition: parallel close 2 -> closed");
            connection.removeConversationById(id);
            state = CLOSED;
            connection.closeNotification(this);
            waitForCloseToCompleteSemaphore.post();
         }
         else if (state == AWAITING_PEER3)
         {
            wakeup = true;
            //@stoptracescan@
            if (TraceComponent.isAnyTracingEnabled()) JFapUtils.debugSummaryMessage(tc, connection, this, "Conversation closed (processLogicalCloseResponse)");
            //@starttracescan@
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "state transition: awaiting peer 3 -> closed");
            eventRecorder.logDebug("state transition: awaiting peer 3 -> closed");
            connection.removeConversationById(id);
            state = CLOSED;
            connection.closeNotification(this);
            waitForCloseToCompleteSemaphore.post();
         }
         else
         {
            // Not a valid transition for the state machine.
            eventRecorder.logError("invalid state transition.  state="+state+" transition=sendCompletes");
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Invalid state transition.  State="+state+".  Transition=sendCompletes");
            FFDCFilter.processException
            (new Exception(), "com.ibm.ws.sib.jfapchannel.impl.ConversationImpl", "NEW_FFDC_PROBE_NEEDED_03", new Object[] {state, eventRecorder.toString()});
            waitForCloseToCompleteSemaphore.post();
         }
      }
      if (sendCloseResponse) sendLogicalClose(false);
      if (wakeup) wakeupAllWithException(new SIConnectionLostException("Peer initiated connection close while conversations still active"), false);   // D242366
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "sendCompletes");
   }
   // end D273932

   // Start D269145, D273932
   /**
    * This class is used as a send listener to notify anyone waiting to send a logical close that
    * the last one was sent. This ensure ordering when sending logical closes.
    *
    * @author Gareth Matthews
    */
   private class LogicalCloseSendListener implements SendListener
   {
      public void dataSent(Conversation conversation)
      {
         if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "dataSent", conversation        );

         eventRecorder.logEntry("LogicalCloseSendListener.dataSent");
         eventRecorder.logDebug("LogicalCloseSendListener hashcode: "+Integer.toHexString(System.identityHashCode(this)));
         sendCompletes();
         eventRecorder.logExit("LogicalCloseSendListener.dataSent");

         if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "dataSent");
      }

      public void errorOccurred(SIConnectionLostException exception, Conversation conversation)
      {
         if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "errorOccurred", new Object[]{exception, conversation});

         eventRecorder.logEntry("LogicalCloseSendListener.errorOccurred");
         eventRecorder.logDebug("LogicalCloseSendListener hashcode: "+Integer.toHexString(System.identityHashCode(this)));
         sendCompletes();
         eventRecorder.logExit("LogicalCloseSendListener.errorOccurred");

         if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "errorOccurred");
      }
   }
   // End D269145, D273932

   /**
    * Wakes up any exchanges (with the specified exception).
    * Normally, if any exchanges need waking up, the exception is
    * delievered to the conversation receive listener.
    * @param exception Exception to deliver.
    * @param alwaysDeliverToConversationReceiveListener Always
    * deliver the exception to the conversation receive listener,
    * regardless of whether any exchanges were woken or not.
    */
   protected void wakeupAllWithException(SIConnectionLostException exception,
                                         boolean alwaysDeliverToConversationReceiveListener)
   {
      //Suppressing trace of exception to prevent it causing alarm when there are no conversations in an exchange.
      //It will get traced out in queueErrorOccurredInvocation if needed, so we won't loose useful debug.
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "wakeupAllWithException", Boolean.valueOf(alwaysDeliverToConversationReceiveListener));

      boolean requestorsWoken = false;

      Iterator requestIdIterator =
         reqIdTable.idIterator();

      while(requestIdIterator.hasNext())
      {

         Integer requestId = (Integer)requestIdIterator.next();
         ReceiveListener receiveListener = reqIdTable.getListener(requestId.intValue());
         if (receiveListener != null)
         {
            requestorsWoken = true;
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "waking "+receiveListener);
            try
            {
               ReceiveListenerDispatcher.getInstance(connection.getConversationType(),onClientSide).
                  queueErrorOccurredInvocation(connection, receiveListener, exception, -1, requestId.intValue(), -1, this);
            }
            catch(Throwable t)
            {
               FFDCFilter.processException
                  (t, "com.ibm.ws.sib.jfapchannel.impl.ConversationImpl", JFapChannelConstants.CONVIMPL_WAKEUPALLWITHEXCP_01, new Object[] {state, eventRecorder.toString()});
               if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "exception thrown in receive listener error occurred method");
               if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.exception(this, tc, t);
            }
         }

         SendListener sendListener = reqIdTable.getSendListener(requestId.intValue());
         if (sendListener != null)
         {
            requestorsWoken = true;
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "waking "+sendListener);
            try
            {
               sendListener.errorOccurred(exception, this);
            }
            catch(Throwable t)
            {
               FFDCFilter.processException
                  (t, "com.ibm.ws.sib.jfapchannel.impl.ConversationImpl", JFapChannelConstants.CONVIMPL_WAKEUPALLWITHEXCP_01, eventRecorder.toString());
               if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "exception thrown in send listener error occurred method");
               if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.exception(this, tc, t);

            }
         }
      }
      reqIdTable.clear();

      if (requestorsWoken || alwaysDeliverToConversationReceiveListener)
      {
         ConversationReceiveListener crl = getDefaultReceiveListener();
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "notifying conversation receive listener "+crl);
         try
         {
            ReceiveListenerDispatcher.getInstance(connection.getConversationType(), onClientSide).
               queueErrorOccurredInvocation(connection, crl, exception, -1, -1, -1, this);
         }
         catch(Throwable t)
         {
            FFDCFilter.processException
               (t, "com.ibm.ws.sib.jfapchannel.impl.ConversationImpl", JFapChannelConstants.CONVIMPL_WAKEUPALLWITHEXCP_01, eventRecorder.toString());
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "exception thrown in connection receive listener error occurred method");
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.exception(this, tc, t);
         }
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "wakeupAllWithException");
   }

   /**
    * Exchange data with our peer on this conversation.
    * @see Conversation#exchange(JFapByteBuffer, int, int, int, boolean)
    */
        public ReceivedData exchange(JFapByteBuffer buffer,
                                 int segmentType,
                                 int requestNumber,
                                 int priority,
                                 boolean canPoolOnReceive)
   throws SIConnectionLostException,
          SIConnectionDroppedException
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "exchange", new Object[] {buffer, ""+segmentType, ""+requestNumber, ""+priority, ""+canPoolOnReceive});

      if (requestNumber == 0)
      {
         // A zero request number is reserved from asynchronous transmissions, ie. not
         // exchanges.  Calling this method with a requestNumber of zero is a programming error.
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "exchnage with requestNumber == 0");

         SIErrorException sie = new SIErrorException(nls.getFormattedMessage("CONVERSATIONIMPL_INTERNAL_SICJ0046", null, "CONVERSATIONIMPL_INTERNAL_SICJ0046"));

         if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "exchange", sie);

         throw sie;
      }


      // Mark the buffers as read-only once they are given to us so that anyone trying to modify
      // them on another thread will get a stern ticking off.
      buffer.setReadOnly();

      ExchangeReceiveListener receiveListener = exchangeReceiveListenerPool.allocate(requestNumber);

      sendInternal(buffer,
           segmentType,
           requestNumber,
           priority,
           canPoolOnReceive,
           true,
           ThrottlingPolicy.BLOCK_THREAD,
           receiveListener,
           null);

      receiveListener.waitToComplete();

      if (!receiveListener.successful())
      {
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "exchange", "operation unsuccessful");
         if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.exception(this, tc, receiveListener.getException());
         if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "exchange", receiveListener.getException());
         throw receiveListener.getException();
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "exchange", receiveListener);
      return receiveListener;
   }

   // Start f181007
   /**
    * Sends data to our peer for this conversation.
    * @see Conversation#send(JFapByteBuffer, int, int, int, boolean, ThrottlingPolicy, SendListener)
    */
        public long send(JFapByteBuffer buffer,
                         int segmentType,
                     int requestNumber,
                     int priority,
                     boolean canPoolOnReceive,
                     ThrottlingPolicy throttlingPolicy,
                     SendListener sendListener)
        throws SIConnectionLostException,
          SIConnectionDroppedException
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "send", new Object[] {buffer, Integer.valueOf(segmentType), Integer.valueOf(requestNumber), Integer.valueOf(priority), Boolean.valueOf(canPoolOnReceive), throttlingPolicy, sendListener});

      // Mark the buffers as read-only once they are given to us so that anyone trying to modify
      // them on another thread will get a stern ticking off.
      buffer.setReadOnly();

      long bytes = 0;
      bytes = sendInternal(buffer,
                   segmentType,
                   requestNumber,
                   priority,
                   canPoolOnReceive,
                   false,                 // false indicates we are not an exchange
                   throttlingPolicy,
                   null,
                   sendListener);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "send", ""+bytes);
      return bytes;
   }

   /**
    * This is a private version of the send method. It does a send, but takes
    * an extra flag to indicate whether or not it is part of an exchange. The
    * public methods exchange() and send() both call this method and will set
    * the flag appropriately - meaning that the user does not have to.
    *
    * @param buffer
    * @param segmentType
    * @param requestNumber
    * @param priority
    * @param canPoolOnReceive
    * @param isPartOfExchange
    * @param throttlingPolicy
    * @param receiveListener
    * @param sendListener
    *
    * @return Returns the amount of data sent
    */
   private long sendInternal(JFapByteBuffer buffer,
                             int segmentType,
                             int requestNumber,
                             int priority,
                             boolean canPoolOnReceive,
                             boolean isPartOfExchange,
                             ThrottlingPolicy throttlingPolicy,
                             ReceiveListener receiveListener,
                             SendListener sendListener)
      throws SIConnectionDroppedException,
             SIConnectionLostException
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "sendInternal", new Object[]{buffer, segmentType, requestNumber, priority, canPoolOnReceive, isPartOfExchange, throttlingPolicy, receiveListener, sendListener});
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) buffer.dump(this, tc, 16);

      eventRecorder.logDebug(" >> Data sent: Segment " + segmentType +
                             " (0x" + Integer.toHexString(segmentType) + "), "+
                             "Request No: " + requestNumber);

      if (getDefaultReceiveListener() == null)
      {
         // There is no default receive listener associated with this conversation.  This means that
         // any transmissions sent asynchronously from our peer cannot be delivered to anyone.
         // This is an programming error.
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Conversation has no default receive listener");
         
         SIErrorException e = new SIErrorException(nls.getFormattedMessage("CONVERSATIONIMPL_INTERNAL_SICJ0046", null, "CONVERSATIONIMPL_INTERNAL_SICJ0046"));
         FFDCFilter.processException(e, ConversationImpl.class.getName() + ".sendInternal", JFapChannelConstants.CONVIMPL_SEND_01);
         
         if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "sendInternal");
         throw e;
      }

      if (reqIdTable.containsId(requestNumber))
      {
         // Check that this isn't a duplicate request id.  This would be a programming error.
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Duplicate request ID");
         
         SIErrorException e = new SIErrorException(nls.getFormattedMessage("CONVERSATIONIMPL_INTERNAL_SICJ0046", null, "CONVERSATIONIMPL_INTERNAL_SICJ0046"));
         FFDCFilter.processException(e, ConversationImpl.class.getName() + ".sendInternal", JFapChannelConstants.CONVIMPL_SEND_01);
         
         if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "sendInternal");
         throw e;
      }

      if ((priority != PRIORITY_LOWEST) &&
          ((priority < 0) || (priority > PRIORITY_HEARTBEAT)))
      {
         // Check that the priority specified lies within the range that callers of this
         // class are permitted to specify.  If the priority lies outside this range, this is
         // considered a programming error.
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "priority ("+priority+") outside range of user priorities");

         // It turns out the GD code tries to send values outside this range.
         // As a quick work around, don't throw an exception, cap the value.
         //throw new SIErrorException(nls.getFormattedMessage("CONVERSATIONIMPL_INTERNAL_SICJ0046", null, "CONVERSATIONIMPL_INTERNAL_SICJ0046"));
         if (priority < 0) priority = 0;
         else priority = PRIORITY_HEARTBEAT;
      }

      if ((throttlingPolicy != ThrottlingPolicy.BLOCK_THREAD) &&
          (throttlingPolicy != ThrottlingPolicy.DISCARD_TRANSMISSION))
      {
         // Check that the throttling policy is one of the policies that we support calling
         // via this interface.  ThrottlingPolicy.DO_NOT_THROTTLE is reserved for internal
         // use only (i.e. cannot be specified using this method).
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Invalid throttling policy: "+throttlingPolicy);

         SIErrorException e = new SIErrorException(nls.getFormattedMessage("CONVERSATIONIMPL_INTERNAL_SICJ0046", null, "CONVERSATIONIMPL_INTERNAL_SICJ0046"));
         FFDCFilter.processException(e, ConversationImpl.class.getName() + ".sendInternal", JFapChannelConstants.CONVIMPL_SEND_01);

         if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "sendInternal");
         throw e;
      }

      synchronized(sendMonitor)
      {
         synchronized(stateChangeMonitor)                                        // D203646
         {
            if (state != OPEN)                                                   // D263646
            {
               if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "sendInternal", "connection closed");
               if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "sendInternal");
               throw new SIConnectionDroppedException(nls.getFormattedMessage("CONVERSATIONIMPL_CLOSED_SICJ0047", null, "CONVERSATIONIMPL_CLOSED_SICJ0047"));  // D226223
            }

            inSend = true;
         }
      }

      long bytesSent;
      try
      {
         // Record info about this request in the request id table.  We need
         // to do this regardless of whether there is a receive listener
         // and/or semaphore as it polices against someone specifying a duplicate
         // request identifier.  However, we allow multiple request ids of zero
         // as this means no reply.
         if ((requestNumber != 0) && (receiveListener != null))
            reqIdTable.add(requestNumber, receiveListener, sendListener);

         bytesSent =                                         // F168604.3
            connection.send(buffer,
                            segmentType,
                            id,
                            requestNumber,
                            priority,
                            canPoolOnReceive,
                            isPartOfExchange,
                            throttlingPolicy,
                            sendListener,
                            this,
                            true);

      }
      // begin D262663
      finally
      {
         synchronized(sendMonitor)
         {
            inSend = false;
            if (invalidateOutstanding)
            {
               if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "invalidate delayed - performing now");
               eventRecorder.logDebug("performing delayed invalidation");
               invalidateOutstanding = false;
               invalidate(invalidateOutstandingThrowable);
               invalidateOutstandingThrowable = null;
            }
         }
      }
      // end D262663

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "sendInternal", ""+bytesSent);
      return bytesSent;                                        // F168604.3
   }
   // end f181007

   /** @see Conversation#isFirst() */
   public boolean isFirst()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "isFirst");
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "isFirst", ""+first);
      return first;
   }


   /** @see Conversation#handshakeComplete() */
   public void handshakeComplete()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "handshakeComplete");

      // begin D196125
      synchronized(stateChangeMonitor)                               // D203646
      {
         // We do not check here whether the connection is closed as we always want to
         // ensure the monitor is notified.
         connection.handshakeComplete();
      }
      // end D196125
                                     // D181493
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "handshakeComplete");
   }

   // begin D221433
   public void handshakeFailed()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "handshakeFailed");
      synchronized(stateChangeMonitor)
      {
         // We do not check here whether the connection is closed as we always want to
         // ensure the monitor is notified.
         connection.handshakeFailed();
      }
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "handshakeFailed");
   }
   // end D221433

   /**
    * Returns this conversations ID
    * @return int This conversations ID
    */
   public int getId()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getId");
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getId", ""+id);
      return id;
   }

   /**
    * Associates a "user" attachment object with this conversation.
    * @see Conversation#setAttachment(Object)
    */
   public void setAttachment(Object attachment)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "setAttachment", attachment);
      this.attachment = attachment;
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "setAttachment");
   }

   /**
    * Retrieves any previously associated "user" attachment object.
    * @see Conversation#getAttachment()
    */
   public Object getAttachment()
   {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getAttachment");
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getAttachment", attachment);
      return attachment;
   }

        /**
         * Sets a "user" per connection level attachment object
         * @see com.ibm.ws.sib.jfapchannel.Conversation#setLinkLevelAttachment(java.lang.Object)
         */
        public void setLinkLevelAttachment(Object attachment)
        {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "setLinkLevelAttachment", attachment);
                connection.setAttachment(attachment);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "setLinkLevelAttachment");
        }

        /**
         * Retrieves a "user" per connection level attachment object.
         * @see com.ibm.ws.sib.jfapchannel.Conversation#getLinkLevelAttachment()
         */
        public Object getLinkLevelAttachment()
        {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getLinkLevelAttachment");
      Object returnAttachment = connection.getAttachment();
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getLinkLevelAttachment", returnAttachment);
                return returnAttachment;
        }

        /**
         * Determines if this conversation shares the same connection as another conversation.
         * @see com.ibm.ws.sib.jfapchannel.Conversation#sharesSameLinkAs(com.ibm.ws.sib.jfapchannel.Conversation)
         */
        public boolean sharesSameLinkAs(Conversation conversation)
        {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "sharesSameLinkAs", conversation);
      boolean returnValue = ((ConversationImpl)conversation).connection == connection;
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "sharesSameLinkAs", ""+returnValue);
                return returnValue;
        }

        /**
         * Makes a "clone" of this conversation which will share the same connection.
         * @see com.ibm.ws.sib.jfapchannel.Conversation#cloneConversation(ConversationReceiveListener)
         */
   // begin F191566
        public Conversation cloneConversation(ConversationReceiveListener receiveListener)
   throws SIResourceException
        {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "cloneConversation", receiveListener);

      Conversation returnConversation = connection.cloneConversation(receiveListener);

                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "cloneConversation", returnConversation);
      return returnConversation;
        }
   // end F191566

        /**
    * Returns an array of conversations which share the same underlying connection.
         * @see com.ibm.ws.sib.jfapchannel.Conversation#getConversationsSharingSameLink()
         */
   // begin F172937
        public Conversation[] getConversationsSharingSameLink()
        {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getConversationsSharingSameLink");
      Conversation[] retValue = connection.getConversations();
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getConversationsSharingSameLink", retValue);
      return retValue;
        }
   // end 172937

   /**
    * Returns the next request number to use for outbound transmissions on this
    * conversation.
    */
   // begin F173152
   protected byte getNextRequestNumber()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc,"getNextRequestNumber");
      byte nextRequestNumber = connection.getNextRequestNumber();
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc,"getNextRequestNumber", ""+nextRequestNumber);
      return nextRequestNumber;
   }
   // end F173152

   /**
    * Returns true if this conversation has outstanding requests which
    * have yet to be sent to its peer.
    */
   // begin F174772
   protected boolean hasOutstandingRequests()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "hasOutstandingRequests");
      boolean hasOutstandingRequests = reqIdTable.hasReceiveListeners();
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "hasOutstandingRequests", ""+hasOutstandingRequests);
      return hasOutstandingRequests;
   }
   // end F174772

   /** @see com.ibm.ws.sib.jfapchannel.Conversation#setHeartbeatInterval(int) */
   // begin F175658
   public void setHeartbeatInterval(int seconds)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "setHeartbeatInterval", ""+seconds);
      if (seconds < 1) throw new SIErrorException(nls.getFormattedMessage("CONVERSATIONIMPL_INTERNAL_SICJ0046", null, "CONVERSATIONIMPL_INTERNAL_SICJ0046"));  // D226223
      connection.setHeartbeatInterval(seconds);
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "setHeartbeatInterval");
   }
   // end F175658

   /** @see com.ibm.ws.sib.jfapchannel.Conversation#getHeartbeatInterval() */
   // begin F175658
   public int getHeartbeatInterval()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getHeartbeatInterval");
      int returnValue = connection.getHeartbeatInterval();
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getHeartbeatInterval", ""+returnValue);
      return returnValue;
   }
   // end F175658

   /** @see com.ibm.ws.sib.jfapchannel.Conversation#setHeartbeatTimeout(int) */
   // begin F175658
   public void setHeartbeatTimeout(int seconds)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "setHeartbeatTimeout", ""+seconds);
      if (seconds < 1) throw new SIErrorException(nls.getFormattedMessage("CONVERSATIONIMPL_INTERNAL_SICJ0046", null, "CONVERSATIONIMPL_INTERNAL_SICJ0046"));  // D226223
      connection.setHeartbeatTimeout(seconds);
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "setHeartbeatTimeout");
   }
   // end F175658

   /** @see com.ibm.ws.sib.jfapchannel.Conversation#getHeartbeatTimeout() */
   // begin F175658
   public int getHeartbeatTimeout()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getHeartbeatTimeout");
      int returnValue = connection.getHeartbeatTimeout();
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getHeartbeatTimeout", ""+returnValue);
      return returnValue;
   }
   // end F175658

   // begin D185831
   /**
    * @return Returns a reference to the dispatch queue lock object.  This lock
    * object must by synchronized on prior to modifying the dispatch queue
    * associated with this object or incrementing / decrementing its
    * reference count.
    */
   public Object getDispatchLockObject()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getDispatchLockObject");
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getDispatchLockObject", dispatchLockObject);
      return dispatchLockObject;
   }
   // end D185831

   // begin D185831
   /**
    * Decrement the dispatch queue reference count associated with this
    * conversation.
    */
   public void decrementDispatchQueueRefCount()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "decrementDispatchQueueRefCount");
      --dispatchQueueReferenceCount;
      if (dispatchQueueReferenceCount < 0)
      {
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "dispatchQueueReferneceCount="+dispatchQueueReferenceCount);
         throw new SIErrorException(nls.getFormattedMessage("CONVERSATIONIMPL_INTERNAL_SICJ0046", null, "CONVERSATIONIMPL_INTERNAL_SICJ0046"));    // D226223
      }
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "dispatchQueueReferenceCount = " + Integer.valueOf(dispatchQueueReferenceCount));
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "decrementDispatchQueueRefCount");
   }
   // end D185831

   // begin D185831
   /**
    * @return Returns the value of the dispatch queue reference count associated with
    * this conversation.
    */
   public int getDispatchQueueRefCount()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getDispatchQueueRefCount");
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getDispatchQueueRefCount", ""+dispatchQueueReferenceCount);
      return dispatchQueueReferenceCount;
   }
   // end D185831

   // begin D185831
   /**
    * Associates a new dispatch queue with this conversation.
    * @param o
    */
   public void setDispatchQueue(DispatchQueue o)                                          // F201521
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "setDispatchQueue", o);
      dispatchQueue = o;
      dispatchQueueReferenceCount = 0 ;
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "setDispatchQueue");
   }
   // end D185831

   // begin D185831
   /**
    * @return Retrieves the dispatch queue currently associated with this conversation.
    */
   public DispatchQueue getDispatchQueue()                                                // F201521
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getDispatchQueue");
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getDispatchQueue", dispatchQueue);
      return dispatchQueue;
   }
   // end D185831

   // begin D185831
   /**
    * Increments the dispatch queue reference count for this conversation.
    */
   public void incrementDispatchQueueRefCount()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "incrementDispatchQueueRefCount");

      ++dispatchQueueReferenceCount;

      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "dispatchQueueReferenceCount = " + Integer.valueOf(dispatchQueueReferenceCount));

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "incrementDispatchQueueRefCount");
   }
   // end D185831

   // begin D185831
   /**
    * @return Returns the monotomically increased instance count for this conversation.
    */
   public int getInstanceCounterValue()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getInstanceCounterValue");
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getInstanceCounterValue", ""+thisInstanceCounter);
      return thisInstanceCounter;
   }
   // end D185831

   // begin F181603.2
   /**
    * Process a logical close request for this conversation.
    */
   protected void processLogicalClose(WsByteBuffer data)                      // D274606
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "processLogicalClose", data);  // D274606

      data.flip();
      int flagByte = data.get();

      boolean closeImmediately =
         (flagByte & JFapChannelConstants.LOGICAL_CLOSE_IMMEDIATE_BIT) != 0;

      // For the moment, the only bit we support is the first.
      if ((flagByte &
         (0xFF ^ JFapChannelConstants.LOGICAL_CLOSE_IMMEDIATE_BIT)) != 0)
      {
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Bad flags", ""+flagByte);
      }

      // begin D196125
      if (closeImmediately)
         processLogicalCloseResponse();
      else
         processLogicalCloseRequest();
      // end D196125

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "processLogicalClose");   // D274606
   }
   // end F181603.2


   // begin F181603.2
   /**
    * Process a ping request for this conversation.
    */
   protected void processPing(int requestNumber,
                              int priority,
                              WsByteBuffer data)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "processPing", new Object[] {Integer.valueOf(requestNumber), Integer.valueOf(priority), data});

      long pingTime = data.getLong();
      int dataLength = data.getInt();

      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "ping time="+pingTime+" ping length="+dataLength);

      // Set position and limit so we will re-transmit ping data.
      int currentLimit = data.limit();
      data.limit(data.position()+dataLength);
      // Set the position at the end of the data because JFap buffers are flipped before
      // they are sent
      data.position(data.limit());
      InternalJFapByteBuffer buffer = new InternalJFapByteBuffer(data);

      // Send response to ping request.
      try
      {
         connection.send(buffer,
                         JFapChannelConstants.SEGMENT_PING_RESPONSE,
                         id,
                         requestNumber,
                         priority,
                         true,
                         false,
                         ThrottlingPolicy.DO_NOT_THROTTLE,
                         null,
                         this,
                         false);
      }
      catch (SIException e)
      {
         FFDCFilter.processException
            (e, "com.ibm.ws.sib.jfapchannel.impl.ConversationImpl", JFapChannelConstants.CONVIMPL_PROCESSPING_01, eventRecorder.toString());
         if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.exception(this, tc, e);
         connection.invalidate(true, e, "SIException thrown from system send");  // D224570
      }

      // Reset data limit to it's starting value - just incase.
      data.limit(currentLimit);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "processPing");
   }
   // end F181603.2

   // begin F181603.2
   /**
    * Process a ping response for this conversation.  This is a somewhat surprising
    * event as there is currently no provision to ping a peer...
    */
   protected void processPingResponse(WsByteBuffer data)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "processPingResponse", data);

      // We should never get a ping response - so ignore it unless debug is on.
      // If it is, then dump out some information about the ping response.
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
      {
         long currentTime = System.currentTimeMillis();
         long pingTime = data.getLong();
         int dataLength = data.getInt();

         StringBuffer sb = new StringBuffer("receieved ping response - which should not have originated from me:\n");
         sb.append("ping time: ");
         sb.append(pingTime);
         sb.append("\nping data length: ");
         sb.append(dataLength);
         if (currentTime > pingTime)
         {
            sb.append("\nround trip time: ");
            sb.append(currentTime - pingTime);
         }
         else
         {
            sb.append("\nping time incorrect, current system time is: ");
            sb.append(currentTime);
         }
         sb.append("\ndump of ping data follows:");
         SibTr.debug(this, tc, sb.toString());
         JFapUtils.debugTraceWsByteBuffer(this, tc, data, dataLength, "Ping data");
      }

      data.release();

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "processPingResponse");
   }
   // end F181603.2

   // begin F181603.2
   public void setMaxTransmissionSize(int maxTransmissionSize)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "setMaxTransmissionSize", ""+maxTransmissionSize);
      connection.setMaxTransmissionSize(maxTransmissionSize);
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "setMaxTransmissionSize");
   }
   // end F181603.2

   // begin F181603.2
   public int getMaxTransmissionSize()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getMaxTransmissionSize");
      int value = connection.getMaxTransmissionSize();
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getMaxTransmissionSize", ""+value);
      return value;
   }
   // end F181603.2

   // begin F181603.2
   public void setOnClientSide()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "setOnClientSide");
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "setOnClientSide");
      onClientSide = true;
   }
   // end F181603.2

   // begin F181603.2
   public boolean isOnClientSide()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "isOnClientSide");
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "isOnClientSide", ""+onClientSide);
      return onClientSide;
   }
   // end F181603.2

   // Start F201521
   /**
    * @return Returns a lock to use when updating / getting the request count
    */
   public Object getTotalOutstandingRequestCountLock()
   {
           if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
           {
                 SibTr.entry(this, tc, "getTotalOutstandingRequestCountLock");
                 SibTr.exit(this, tc, "getTotalOutstandingRequestCountLock", ""+ totalOutstandingRequestsLock);
           }
           return totalOutstandingRequestsLock;
   }

   // Start D262285
   /**
    * @return Returns the total number of outstanding requests for this Conversation. These
    *         requests may be scattered amongst many dispatch queues.
    */
   public int getTotalOutstandingRequestCount()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      {
         SibTr.entry(this, tc, "getTotalOutstandingRequestCount");
         SibTr.exit(this, tc, "getTotalOutstandingRequestCount", ""+ totalOutstandingRequests);
      }
      return totalOutstandingRequests;
   }

   /**
    * Increments the total number of outstanding requests.
    */
   public void incrementTotalOutstandingCount()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this,tc, "incrementTotalOutstandingCount");
      ++totalOutstandingRequests;

      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "totalOutstandingRequests = " + Integer.valueOf(totalOutstandingRequests));

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "incrementTotalOutstandingCount");
   }

   /**
    * Decrements the total number of outstanding requests.
    */
   public void decrementTotalOutstandingCount()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "decrementTotalOutstandingCount");

      --totalOutstandingRequests;

      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "totalOutstandingRequests = " + Integer.valueOf(totalOutstandingRequests));

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "decrementTotalOutstandingCount");
   }

   /**
    * This is called by the JFap channel when something has gone wrong on this Converstion
    * and the Conversation receive listeners errorOccurred() method needs to be invoked. We
    * could just queue it and let it get executed at some point, but this could end up being
    * before another dispatch queue has finished processing data for that Conversation. As
    * such, rather than queueing it, it will be saved here and so that we know that as soon
    * as the totalOutstandingCount reaches zero, the errorOccurred invocation can be invoked.
    *
    * @param error
    */
   public void setErrorOccurredInvocation(AbstractInvocation error)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "setErrorOccurredInvocation", error);

      // Set the errorOccurred invocation. If we already have one, it will not provide
      // any more useful information, so simply debug it.
      if (errorOccurredInvocation == null)
      {
         errorOccurredInvocation = error;
      }
      else
      {
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Already received an errorOccurred", error);
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "setErrorOccurredInvocation");
   }
   // End D262285

   /**
    * @return Returns the error occurred invocation (if there is one)
    */
   public AbstractInvocation getErrorOccurredInvocation()
   {
      return errorOccurredInvocation;
   }
   // End F201521

   // begin D196678.10.1
   /** @see com.ibm.ws.sib.jfapchannel.Conversation#getMetaData() */
   public ConversationMetaData getMetaData()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getMetaData");
      ConversationMetaData retValue = connection.getMetaData();
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getMetaData", retValue);
      return retValue;
   }
   // end D196678.10.1

   // begin F193735.3
   /** @see com.ibm.ws.sib.jfapchannel.Conversation#setConversationType(com.ibm.ws.sib.jfapchannel.Conversation.ConversationType) */
   public void setConversationType(ConversationType type)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "setConversationType", type);
      connection.setConversationType(type);
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "setConversationType");
   }
   // end F193735.3

   // begin F193735.3
   /**
    * @see com.ibm.ws.sib.jfapchannel.Conversation#setConversationType(com.ibm.ws.sib.jfapchannel.Conversation.ConversationType)
    * @return Returns the conversation type.
    */
   public ConversationType getConversationType()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getConversationType");
      ConversationType retType = connection.getConversationType();
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getConversationType", retType);
      return retType;
   }
   // end F193735.3

   // Start D209410
   /** @see com.ibm.ws.sib.jfapchannel.Conversation#isClosed() */
   public boolean isClosed()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "isClosed");
      boolean ret = false;
      synchronized (stateChangeMonitor)
      {
         ret = (state != OPEN);
      }
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "isClosed", ""+ret);
      return ret;
   }
   // End D209401

   /**
    * @see Conversation#addConnectionClosedListener(ConnectionClosedListener, ConversationUsageType)
    */
   public void addConnectionClosedListener(ConnectionClosedListener listener, ConversationUsageType usageType)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "addConnectionClosedListener", new Object[]{listener, usageType});
      connection.addConnectionClosedListener(listener, usageType);
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "addConnectionClosedListener");
   }

   /**
    * @see Conversation#getConnectionClosedListener(ConversationUsageType)
    */
   public ConnectionClosedListener getConnectionClosedListener(ConversationUsageType usageType)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getConnectionClosedListener", usageType);
      final ConnectionClosedListener listener = connection.getConnectionClosedListener(usageType);
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getConnectionClosedListener", listener);
      return listener;
   }

   /**
    * This method will return a reference to the underlying physical connection. Nothing can be
    * done with this object (as it's interface is not public) but it can be used as a key in
    * Hashtables for example.
    *
    * @return The underlying connection.
    */
   public ConnectionInterface getConnectionReference()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getConnectionReference");
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getConnectionReference", connection);
      return connection;
   }

   /**
    * Causes a log event to be written to our log recorder that indicates data was received for
    * this conversation.
    *
    * @param segmentType The segment that was received.
    * @param requestNumber The request number.
    */
   protected void logDataReceivedEvent(int segmentType, int requestNumber)
   {
     // For performance reasons there is no trace entry statement here
           eventRecorder.logDebug(" << Data rcvd: Segment " + segmentType +
                             " (0x" + Integer.toHexString(segmentType) + "), "+
                             "Request No: " + requestNumber);
     // For performance reasons there is no trace exit statement here
   }

   /**
    * @return Returns handshake properties associated with the connection that backs this
    *         Conversation.
    */
   public HandshakeProperties getHandshakeProperties()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getHandshakeProperties");
      final HandshakeProperties result = connection.getHandshakeProperties();
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getHandshakeProperties", result);
      return result;
   }

   /**
    * Sets handshake properties that are associated with the connection that backs this
    * Conversation.
    *
    * @param handshakeProperties
    */
   public void setHandshakeProperties(HandshakeProperties handshakeProperties)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "setHandshakeProperties", handshakeProperties);
      connection.setHandshakeProperties(handshakeProperties);
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "setHandshakeProperties");
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
     connection.setSchemaSet(schemaSet);
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
   public ConnectionSchemaSet getSchemaSet() throws SIConnectionDroppedException
   {
     if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getSchemaSet");
     ConnectionSchemaSet result = connection.getSchemaSet();
     if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getSchemaSet",  result);
     return result;
   }
   
   /**
    * Check to see if the given request number is free
    * @param requestNumber is the request number to check
    * @return true if the request number is free
    */
    public boolean checkRequestNumberIsFree(int requestNumber)
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "checkRequestNumberIsFree", new Integer(requestNumber));
      boolean retVal = !reqIdTable.containsId(requestNumber);
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "checkRequestNumberIsFree", new Boolean(retVal));
      return retVal;
    }
}
