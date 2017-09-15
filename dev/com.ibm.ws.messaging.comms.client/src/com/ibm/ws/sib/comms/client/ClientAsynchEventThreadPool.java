/*******************************************************************************
 * Copyright (c) 2004, 2011 IBM Corporation and others.
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
import com.ibm.websphere.sib.SIDestinationAddress;
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.comms.CommsConstants;
import com.ibm.ws.sib.comms.client.proxyqueue.AsynchConsumerProxyQueue;
import com.ibm.ws.sib.comms.client.proxyqueue.ProxyQueue;
import com.ibm.ws.sib.comms.client.proxyqueue.ProxyQueueConversationGroup;
import com.ibm.ws.sib.jfapchannel.Conversation;
import com.ibm.ws.sib.jfapchannel.framework.Framework;
import com.ibm.ws.sib.jfapchannel.threadpool.ThreadPool;
import com.ibm.ws.sib.utils.RuntimeInfo;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.ConsumerSession;
import com.ibm.wsspi.sib.core.ConsumerSetChangeCallback;
import com.ibm.wsspi.sib.core.DestinationAvailability;
import com.ibm.wsspi.sib.core.DestinationListener;
import com.ibm.wsspi.sib.core.SICoreConnection;
import com.ibm.wsspi.sib.core.SICoreConnectionListener;
import com.ibm.wsspi.sib.core.exception.SIConnectionLostException;

/**
 * Asynchronous events may come to the client from the server, such as indications that the ME
 * is terminating etc. In this case, we should call any user callbacks registered with the
 * connection. However, it would be unwise to do this on the JFap thread. As such, we need a small
 * thread pool which can be used to dispatch these events so that JFap can regain control and
 * continue to process data.
 * <p>
 * The thread pool is created with one thread but can grow as needed. If a thread cannot be
 * dispatched the event is junked - they are not that important. The runnables that process the
 * work are not pooled as it is envisaged that we will not get a large amount of these event
 * messages.
 * <p>
 * NOTE: Each message received is dispatched on it's own thread. We could therefore get messages
 * appearing out of order if one thread stalls at some in-oppurtune moment. A future improvement
 * will be to queue these events by connection and a thread will be associated not with a message
 * but with a connection.
 *
 * @author Gareth Matthews
 */
public class ClientAsynchEventThreadPool
{
   /** Class name for FFDC's */
   private static String CLASS_NAME = ClientAsynchEventThreadPool.class.getName();

   /** Trace */
   private static final TraceComponent tc = SibTr.register(ClientAsynchEventThreadPool.class,
                                                           CommsConstants.MSG_GROUP,
                                                           CommsConstants.MSG_BUNDLE);

   /** The singleton instance of the thread pool */
   private static ClientAsynchEventThreadPool instance = null;

   /**
    * Returns the singleton instance. If one does not exist, it will be created and returned.
    *
    * @return Returns the thread pool.
    */
   public static ClientAsynchEventThreadPool getInstance()
   {
      if (instance == null)
      {
         instance = new ClientAsynchEventThreadPool();
      }

      return instance;
   }

   /** The underlying thread pool */
   private ThreadPool threadPool = null;

   /** The default max size of the asynch event threadpool */
   private static final int DEFAULT_CLIENT_ASYNCH_EVENT_THREADPOOL_MAX_SIZE = 10;

   /** asynch event max thread pool size */
   private static int CLIENT_ASYNCH_EVENT_THREADPOOL_MAX_SIZE;

   /**
    * The custom property to set in the sib.properties file to increase the
    * asynch event thread pool max size
    */
   public final static String CLIENT_ASYNCH_EVENT_THREADPOOL_MAX_SIZE_PROPERTY =
     "com.ibm.ws.sib.comms.client.ClientAsynchEventThreadPoolMaxSize";

   static
   {
     if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "@(#) SIB/ws/code/sib.comms.client.impl/src/com/ibm/ws/sib/comms/client/ClientAsynchEventThreadPool.java, SIB.comms, WASX.SIB, uu1215.01 1.23");
     try
     {
       CLIENT_ASYNCH_EVENT_THREADPOOL_MAX_SIZE = Integer.parseInt(RuntimeInfo.getProperty(CLIENT_ASYNCH_EVENT_THREADPOOL_MAX_SIZE_PROPERTY,
                         ""+DEFAULT_CLIENT_ASYNCH_EVENT_THREADPOOL_MAX_SIZE));
     }
     catch(NumberFormatException nfe)
     {
       // No FFDC code needed
       if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "NumberFormatException was thrown for custom property "
                                                  + CLIENT_ASYNCH_EVENT_THREADPOOL_MAX_SIZE_PROPERTY);
     }
   }

   /**
    * Constructor. Creates the thread pool.
    */
   private ClientAsynchEventThreadPool()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "<init>");

      // Create the thread pool with one thread in
      threadPool = Framework.getInstance().getThreadPool("ClientAsynchEventThread", 1, CLIENT_ASYNCH_EVENT_THREADPOOL_MAX_SIZE);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "<init>");
   }

   /**
    * Dispatches the exception to the relevant proxy queue.
    *
    * @param proxyQueue
    * @param exception
    */
   public void dispatchAsynchException(ProxyQueue proxyQueue, Exception exception)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "dispatchAsynchException",
                                           new Object[] { proxyQueue, exception });

      // Create a runnable with the data
      AsynchExceptionThread thread = new AsynchExceptionThread(proxyQueue, exception);
      dispatchThread(thread);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "dispatchAsynchException");
   }

   /**
    * Dispatches the data to be sent to the connection event listeners on a thread.
    *
    * @param eventId
    * @param conversation
    */
   public void dispatchAsynchEvent(short eventId, Conversation conversation)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "dispatchAsynchEvent",
                                           new Object[] { ""+eventId, conversation });

      // Create a runnable with the data
      AsynchEventThread thread = new AsynchEventThread(eventId, conversation);
      dispatchThread(thread);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "dispatchAsynchEvent");
   }

   /**
    * Dispatches the exception to be sent to the connection event listeners on a thread.
    *
    * @param conn
    * @param exception
    */
   public void dispatchCommsException(SICoreConnection conn,
                                      ProxyQueueConversationGroup proxyQueueConversationGroup,
                                      SIConnectionLostException exception)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "dispatchCommsException");

      // Create a runnable with the data
      CommsExceptionThread thread = new CommsExceptionThread(conn, proxyQueueConversationGroup, exception);
      dispatchThread(thread);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "dispatchCommsException");
   }

   /**
    * Dispatches a thread which will call the destinationAvailable method on the destinationListener passing in the supplied parameters.
    *
    * @param conn
    * @param destinationAddress
    * @param destinationAvailability
    * @param destinationListener
    */
   public void dispatchDestinationListenerEvent(SICoreConnection conn, SIDestinationAddress destinationAddress,
         DestinationAvailability destinationAvailability, DestinationListener destinationListener)
   {
      if(TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "dispatchDestinationListenerEvent",
            new Object[]{conn, destinationAddress, destinationAvailability, destinationListener});

      //Create a new DestinationListenerThread and dispatch it.
      final DestinationListenerThread thread = new DestinationListenerThread(conn, destinationAddress, destinationAvailability, destinationListener);
      dispatchThread(thread);

      if(TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "dispatchDestinationListenerEvent");
   }
   
   //F011127 starts
   /**
    * Dispatches a thread which will call the consumerSetChange method on the ConsumerSetChangeCallback passing in the supplied parameters.
    *
    * @param consumerSetChangeCallback
    * @param isEmpty
    */
   public void dispatchConsumerSetChangeCallbackEvent(ConsumerSetChangeCallback consumerSetChangeCallback,boolean isEmpty)
   {
      if(TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "dispatchConsumerSetChangeCallbackEvent",
            new Object[]{consumerSetChangeCallback, isEmpty});

      //Create a new ConsumerSetChangeCallbackThread and dispatch it.
      final ConsumerSetChangeCallbackThread thread = new ConsumerSetChangeCallbackThread(consumerSetChangeCallback,isEmpty);
      dispatchThread(thread);

      if(TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "dispatchConsumerSetChangeCallbackEvent");
   }
  //F011127 ends
   
   /**
    * Dispatches a thread which will call the stoppableConsumerSessionStopped method on consumerSessionProxy.
    *
    * @param consumerSessionProxy
    */
   public void dispatchStoppableConsumerSessionStopped(ConsumerSessionProxy consumerSessionProxy)
   {
      if(TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "dispatchStoppableConsumerSessionStopped", consumerSessionProxy);

      //Create a new StoppableAsynchConsumerCallbackThread and dispatch it.
      final StoppableAsynchConsumerCallbackThread thread = new StoppableAsynchConsumerCallbackThread(consumerSessionProxy);
      dispatchThread(thread);

      if(TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "dispatchStoppableConsumerSessionStopped");
   }

   /**
    * Actually dispatches the thread.
    *
    * @param runnable
    */
   private void dispatchThread(Runnable runnable)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "dispatchThread");

      try
      {
         // Get a thread from the pool to excute it
         // By only passing the thread we default to wait if the threadpool queue is full
         // We should wait as some callbacks are more important then others but we have no
         // way of distinguishing this.
         threadPool.execute(runnable);
      }
      catch (InterruptedException e)
      {
         // No FFDC code needed
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Thread was interrupted", e);
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "dispatchThread");
   }

   /**
    * This method will send a message to the connection listeners associated
    * with this connection.
    *
    * @param conn May be null if invoking an async callback
    * @param session May be null if invoking a connection callback
    * @param exception May be null if invoking a connection callback
    * @param eventId The event Id
    */
   private static void invokeCallback(SICoreConnection conn, ConsumerSession session,   // d172528
                               Exception exception, int eventId)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "invokeCallback",
                                           new Object[] { conn, session, exception, eventId });

      if (conn != null)                                                          // f174318
      {                                                                          // f174318
         try
         {
            final AsyncCallbackSynchronizer asyncCallbackSynchronizer = ((ConnectionProxy)conn).getAsyncCallbackSynchronizer();
            SICoreConnectionListener[] myListeners = conn.getConnectionListeners();
            for (int x = 0; x < myListeners.length; x++)
            {
               if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Invoking callback on: " + myListeners[x]);

               // Obtain permission from the callback synchronizer to call the application
               asyncCallbackSynchronizer.enterAsyncExceptionCallback();

               // start f174318
               try
               {
                  switch (eventId)
                  {
                     // This special event ID will not be received across the wire, but will
                     // be used internally when we get notified of a JFAP error.
                     case (0x0000):
                        myListeners[x].commsFailure(conn, (SIConnectionLostException) exception);
                        break;

                     case (CommsConstants.EVENTID_ME_QUIESCING):                            // f179464
                        myListeners[x].meQuiescing(conn);
                        break;

                     case (CommsConstants.EVENTID_ME_TERMINATED):                           // f179464
                        myListeners[x].meTerminated(conn);                                      // f179464
                        break;                                                                  // f179464

                     case (CommsConstants.EVENTID_ASYNC_EXCEPTION):             // d172528  // f179464
                        myListeners[x].asynchronousException(session, exception);   // d172528
                        break;                                                      // d172528

                     default:
                        // Should never happen
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Invalid event ID: " + eventId);
                        break;
                  }
               }
               catch (Exception e)
               {
                  FFDCFilter.processException(e, CLASS_NAME + ".invokeCallback",
                                              CommsConstants.CLIENTASYNCHEVENTTHREADPOOL_INVOKE_01,
                                              new Object[] { myListeners[x], conn, session, exception, ""+eventId});

                  if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Caught an exception from the callback", e);
               } finally {
                 // Tell the callback synchronizer that we have completed the exception callback
                 asyncCallbackSynchronizer.exitAsyncExceptionCallback();
               }
               // end f174318
            }                                                                    // f174318
         }
         catch (SIException e)
         {
            // No FFDC Code needed
            // We couldn't get hold of the connection listeners for some reason. Not a lot we can
            // do here except debug the failure
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Unable to get connection listeners", e);
         }
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "invokeCallback");
   }

   /**
    * This private class is the thread which actually processes the async event.
    *
    * @author Gareth Matthews
    */
   private static class AsynchEventThread extends Thread
   {
      /** The event id of the event */
      private short eventId = 0;

      /** The conversation the event was received on */
      private Conversation conversation = null;

      /**
       * Constructor.
       *
       * @param eventId
       * @param conversation
       */
      public AsynchEventThread(short eventId, Conversation conversation)
      {
         if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "<init>",
                                              new Object[] { ""+eventId, conversation });

         this.eventId = eventId;
         this.conversation = conversation;

         if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "<init>");
      }

      /**
       * What to do when the thread is run.
       *
       * @see java.lang.Runnable#run()
       */
      public void run () {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "run");

        // First inform all the connection listeners on this connection
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "First, invoking on our conversation ", conversation);
        ClientConversationState convState = (ClientConversationState) conversation.getAttachment();
        SICoreConnection conn = convState.getSICoreConnection();
        final String thisMeUuid = conn.getMeUuid();

        invokeCallback(conn, null, null, eventId);

        // Now check all the connections using the same physical socket to see if they are connected to the same ME - if they are then notify them
        Conversation[] convsOnThisLink = conversation.getConversationsSharingSameLink();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Processing " + convsOnThisLink.length + " conversation(s) on the same socket link");

        for (int x = 0; x < convsOnThisLink.length; x++) {
          if (convsOnThisLink[x] != conversation) { // Make sure we do not notify ourselves again
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Processing conversation " + convsOnThisLink[x]);
            convState = (ClientConversationState) convsOnThisLink[x].getAttachment();

            if (convState != null) {
              conn = convState.getSICoreConnection();

              if (conn != null) { // Only invoke this callback if the ME uuid is the same as for the original conversation
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Processing core connection " + conn);

                if (conn.getMeUuid().equals(thisMeUuid)) {
                  if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Invoking callback on this core connection");
                  invokeCallback(conn, null, null, eventId);
                } else {
                  if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "ME uuid does not match our ME uuid - skipping");
                }
              } else {
                 if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "SICoreConnection was null - skipping");
              }
            } else {
              if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Conversation state was null - skipping");
            }
          } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Found our own conversation - skipping");
          }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "run");
      }
   }

   /**
    * This private class is the thread which actually processes the async exception.
    *
    * @author Gareth Matthews
    */
   private static class AsynchExceptionThread  implements Runnable
   {
      /** The proxy queue associated with the consumer session */
      private ProxyQueue proxyQueue = null;

      /** The exception */
      private Exception exception = null;

      /**
       * Constructor
       *
       * @param proxyQueue
       * @param exception
       */
      public AsynchExceptionThread(ProxyQueue proxyQueue, Exception exception)
      {
         if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "<init>",
                                              new Object[] { proxyQueue, exception });

         this.proxyQueue = proxyQueue;
         this.exception = exception;

         if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "<init>");
      }

      /**
       * What to do when the thread is run.
       *
       * @see java.lang.Runnable#run()
       */
      public void run()
      {
         if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "run");

         if (proxyQueue instanceof AsynchConsumerProxyQueue)
         {
            ((AsynchConsumerProxyQueue) proxyQueue).deliverException(exception);
         }
         else
         {
            // Nothing we can do here really. A proxy queue that is not an
            // async queue or a proxy queue?? Someone messed up.
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Unable to deliver exception to proxy queue of type: ", proxyQueue);
         }

         if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "run");
      }
   }

   /**
    * This private class is the thread which actually processes the comms exception.
    *
    * @author Gareth Matthews
    */
   private static class CommsExceptionThread  implements Runnable
   {
      /** The SICoreConnection the error occurred on */
      private SICoreConnection conn = null;

      /** The exception */
      private SIConnectionLostException exception = null;

      private final ProxyQueueConversationGroup proxyQueueConversationGroup;

      /**
       * Constructor
       *
       * @param conn
       * @param proxyQueueConversationGroup
       * @param exception
       */
      public CommsExceptionThread(SICoreConnection conn,
                                  ProxyQueueConversationGroup proxyQueueConversationGroup,
                                  SIConnectionLostException exception)
      {
         if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "<init>",
                                              new Object[] { conn, proxyQueueConversationGroup, exception });

         this.conn = conn;
         this.proxyQueueConversationGroup = proxyQueueConversationGroup;
         this.exception = exception;

         if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "<init>");
      }

      /**
       * What to do when the thread is run.
       *
       * @see java.lang.Runnable#run()
       */
      public void run()
      {
         if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "run");

         invokeCallback(conn, null, exception, 0);

         // Notify the proxy queue conversation group (if present) that the conversation
         // has gone away.
         if (proxyQueueConversationGroup != null)
         {
            proxyQueueConversationGroup.conversationDroppedNotification();
         }

         if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "run");
      }
   }

   /**
    * Private Runnable instance which will actually call the destinationListener.destinationAvailable() method when run.
    */
   private static class DestinationListenerThread  implements Runnable
   {
      private final SICoreConnection conn;
      private final SIDestinationAddress destinationAddress;
      private final DestinationAvailability destinationAvailability;
      private final DestinationListener destinationListener;

      /**
       * Create a new DestinationListenerThread.
       *
       * @param conn
       * @param destinationAddress
       * @param destinationAvailability
       * @param destinationListener
       */
      public DestinationListenerThread(SICoreConnection conn, SIDestinationAddress destinationAddress,
            DestinationAvailability destinationAvailability, DestinationListener destinationListener)
      {
         if(TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "DestinationListenerThread.<init>",
               new Object[]{conn, destinationAddress, destinationAvailability, destinationListener});

         this.conn = conn;
         this.destinationAddress = destinationAddress;
         this.destinationAvailability = destinationAvailability;
         this.destinationListener = destinationListener;

         if(TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "DestinationListenerThread.<init>");
      }

      /**
       * Call destinationListener.destinationAvailable().
       *
       * @see Runnable#run()
       */
      public void run()
      {
         if(TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "run");

         //Protect ourselves from the application as far as possible
         try
         {
            destinationListener.destinationAvailable(conn, destinationAddress, destinationAvailability);
         }
         catch(Throwable e)
         {
            FFDCFilter.processException(e, "DestinationListenerThread.run", CommsConstants.DESTINATIONLISTENERTHREAD_RUN_01, this);
            if(TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Caught exception when calling destinationAvailable.", e);
         }

         if(TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "run");
      }
   }

   //F011127 starts
   /**
    * Private Runnable instance which will actually call the ConsumerSetChangeCallbackThread.consumerSetChange() method when run.
    */
   private static class ConsumerSetChangeCallbackThread  implements Runnable
   {
      private final ConsumerSetChangeCallback consumerSetChangeCallback;
      private final boolean isEmpty;
      
      /**
       * Create a new ConsumerSetChangeCallbackThread.
       *
       * @param consumerSetChangeCallback
       * @param isEmpty
       * 
       */
      public ConsumerSetChangeCallbackThread(ConsumerSetChangeCallback consumerSetChangeCallback,boolean isEmpty)
      {
         if(TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "ConsumerSetChangeCallbackThread.<init>",
               new Object[]{consumerSetChangeCallback, isEmpty});

         this.consumerSetChangeCallback = consumerSetChangeCallback;
         this.isEmpty = isEmpty;

         if(TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "ConsumerSetChangeCallbackThread.<init>");
      }

      /**
       * Call ConsumerSetChangeCallbackThread.consumerSetChange().
       *
       * @see Runnable#run()
       */
      public void run()
      {
         if(TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "run");

         //Protect ourselves from the application as far as possible
         try
         {
        	 consumerSetChangeCallback.consumerSetChange(isEmpty);
         }
         catch(Throwable e)
         {
            FFDCFilter.processException(e, "ConsumerSetChangeCallbackThread.run", CommsConstants.CONSUMERSETCHANGECALLBACKTHREAD_RUN_01, this);
            if(TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Caught exception when calling consumerSetChange.", e);
         }

         if(TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "run");
      }
   }
   
   //F011127 ends
   
   /**
    * Private Runnable instance which will actually call the consumerSessionProxy.stoppableConsumerSessionStopped() method when run.
    */
   private static class StoppableAsynchConsumerCallbackThread implements Runnable
   {
      private final ConsumerSessionProxy consumerSessionProxy;

      /**
       * Create a new StoppableAsynchConsumerCallbackThread.
       *
       * @param consumerSessionProxy
       */
      public StoppableAsynchConsumerCallbackThread(ConsumerSessionProxy consumerSessionProxy)
      {
         if(TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "StoppableAsynchConsumerCallbackThread.<init>", consumerSessionProxy);

         this.consumerSessionProxy = consumerSessionProxy;

         if(TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "StoppableAsynchConsumerCallbackThread.<init>");
      }

      /**
       * Call consumerSessionProxy.stoppableConsumerSessionStopped().
       *
       * @see Runnable#run()
       */
      public void run()
      {
         if(TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "run");

         consumerSessionProxy.stoppableConsumerSessionStopped();

         if(TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "run");
      }
   }
}
