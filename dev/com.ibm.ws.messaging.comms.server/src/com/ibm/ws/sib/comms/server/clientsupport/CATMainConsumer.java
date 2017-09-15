/*******************************************************************************
 * Copyright (c) 2004, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.comms.server.clientsupport;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.sib.Reliability;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.comms.CommsConstants;
import com.ibm.ws.sib.jfapchannel.Conversation;
import com.ibm.ws.sib.jfapchannel.JFapChannelConstants;
import com.ibm.ws.sib.jfapchannel.SendListener;
import com.ibm.ws.sib.utils.Semaphore;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.BifurcatedConsumerSession;
import com.ibm.wsspi.sib.core.BrowserSession;
import com.ibm.wsspi.sib.core.ConsumerSession;
import com.ibm.wsspi.sib.core.OrderingContext;
import com.ibm.wsspi.sib.core.SIMessageHandle;
import com.ibm.wsspi.sib.core.exception.SIConnectionLostException;

/**
 * This class encapsulates a consumer and all its properties. Because
 * differing behaviour is required for different consumer modes (such
 * as async, readahead etc) this class also has a subconsumer which
 * deals specifically with those calls which require specific behaviour.
 * These are calls like <code>setAsynchConsumerCallback()</code>. Standard
 * calls (such as <code>start()</code> or <code>stop</code> are passed
 * up to the superclass <code>CATConsumer</code>.
 */
public class CATMainConsumer extends CATConsumer
{
   /** Class name for FFDC's */
   private static String CLASS_NAME = CATMainConsumer.class.getName();

   /** Trace */
   private static final TraceComponent tc = SibTr.register(CATMainConsumer.class,
                                                           CommsConstants.MSG_GROUP,
                                                           CommsConstants.MSG_BUNDLE);

   /** NLS handle */
   private static final TraceNLS nls = TraceNLS.getTraceNLS(CommsConstants.MSG_BUNDLE);

   /** Log class info on load */
   static
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Source info: @(#)SIB/ws/code/sib.comms.server.impl/src/com/ibm/ws/sib/comms/server/clientsupport/CATMainConsumer.java, SIB.comms, WASX.SIB, aa1225.01 1.62.1.1");
   }

   /** The conversation this consumer is associated with */
   private Conversation conversation;

   /** The real Core SPI consumer session */
   private ConsumerSession consumerSession;

   /** The client session Id (or client proxy queue id) */
   private short clientSessionId;

   /** The sub consumer */
   private CATConsumer subConsumer;

   /** The current lowest priority flowed for this session */
   private int lowestPriority = JFapChannelConstants.PRIORITY_HIGH; // f169884

   /** The number of bytes requested for read-ahead / browse */
   private int requestedBytes; // f171177

   /** The consumer session id */
   private short consumerSessionId;

   /** Whether we are read-ahead or not */
   private boolean readAheadPermitted;

   /** Whether we are noLocal or not */
   private boolean noLocal;

   /** Whether we are doing a SICoreConnection.receive or not */
   private boolean connectionReceive = false;

   /** The current message batch number */
   private short messageBatchNumber = 0;           // f172297

   /** Whether we are started or not */
   private volatile boolean started = false;                // d174443

   /** The session unrecoverable reliability */
   private Reliability unrecoverableReliability = null;     // f187521.2.1

   /** Reference to browser session (if we are the main consumer for a browser session) */
   private BrowserSession browserSession = null;                            // F171893

   /** A pre-posted semaphore to be used when starting the consumer */
   private Semaphore startSemaphore = new Semaphore(1);

   /** Indicates is a stoppable async consumer session has been stopped by message processor or not */
   private volatile boolean stoppableSessionStopped = false;                                                    //471642

   /**
    * Constructor
    *
    * @param conversation
    * @param clientSessionId
    * @param consumerSession
    * @param readAheadPermitted
    * @param noLocal
    * @param unrecoverableReliability The unrecoverable reliability for this session
    */
   public CATMainConsumer(Conversation conversation,
                          short clientSessionId,
                          ConsumerSession consumerSession,
                          boolean readAheadPermitted,
                          boolean noLocal,
                          Reliability unrecoverableReliability)
   {
      super();

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "<init>",
                                           new Object[]
                                           {
                                              conversation,
                                              clientSessionId,
                                              consumerSession,
                                              readAheadPermitted,
                                              noLocal,
                                              unrecoverableReliability
                                           });

      this.conversation = conversation;
      this.clientSessionId = clientSessionId;
      this.consumerSession = consumerSession;
      this.readAheadPermitted = readAheadPermitted;
      this.noLocal = noLocal;
      this.unrecoverableReliability = unrecoverableReliability;

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "<init>");
   }

   /**
    * Form of constructor used to create a main consumer which "wraps" a
    * browser session.
    *
    * @param conversation
    * @param clientSessionId
    * @param browserSession
    */
   // begin F171893
   public CATMainConsumer(Conversation conversation, short clientSessionId,
                          BrowserSession browserSession)
   {
      super();

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "<init>",
                                           new Object[]
                                           {
                                              conversation,
                                              clientSessionId,
                                              browserSession
                                           });

      this.conversation = conversation;
      this.clientSessionId = clientSessionId;
      this.browserSession = browserSession;
      subConsumer = new CATBrowseConsumer(this);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "<init>");
   }
   // end F171893

   // d174443 start
   /**
    * @return Returns whether the client has started the session
    */
   public boolean isStarted()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "isStarted");

      // Wait on the semaphore before looking at this value to ensure it has been updated
      startSemaphore.waitOnIgnoringInterruptions();
      startSemaphore.post();

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "isStarted", started);
      return started;
   }
   // d174443 end

   /**
    * @return Returns whether this consumer is doing a <code>SICoreConnection.receive()</code>
    * or not. If it is not, then this is a normal consumer that has been created by
    * the client.
    */
   public boolean getUsingConnectionReceive()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getUsingConnectionReceive");
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getUsingConnectionReceive", connectionReceive);
      return connectionReceive;
   }

   /**
    * Sets whether we are doing a <code>SICoreConnection.receive()</code> or not.
    *
    * @param connectionReceive
    */
   public void setUsingConnectionReceive(boolean connectionReceive)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "setUsingConnectionReceive", connectionReceive);
      this.connectionReceive = connectionReceive;
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "setUsingConnectionReceive");
   }

   /**
    * @return Returns the conversation
    */
   protected Conversation getConversation()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getConversation");
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getConversation", conversation);
      return conversation;
   }

   /**
    * @return Returns the no local setting that this consumer session was
    * created with.
    */
   protected boolean getNoLocal()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getNoLocal");
      checkNotBrowserSession();                                   // F171893
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getNoLocal", ""+noLocal);
      return noLocal;
   }

   /**
    * @return Returns the read ahead permitted setting that this consumer session
    * was created with.
    */
   protected boolean getReadAheadPermitted()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getReadAheadPermitted");
      checkNotBrowserSession();                                   // F171893
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getReadAheadPermitted", ""+readAheadPermitted);
      return readAheadPermitted;
   }

   /**
    * @return Returns the client session Id. This is used to identify the
    * proxy queue that messages should be sent back to.
    */
   protected  short getClientSessionId()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getClientSessionId");
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getClientSessionId", ""+clientSessionId);
      return clientSessionId;
   }

   //Add for f168604.1
   /**
    * Sets the client session Id.
    *
    * @param clientSessionId
    */
   protected void setClientSessionId(short clientSessionId)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "setClientSessionId", clientSessionId);
      this.clientSessionId = clientSessionId;
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "setClientSessionId");
   }

   /**
    * @return Returns the actual consumer session that this class encapsulates
    */
   public ConsumerSession getConsumerSession()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getConsumerSession");
      checkNotBrowserSession();                                   // F171893
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getConsumerSession", consumerSession);
      return consumerSession;
   }

   // Start of change for f169884
   /**
    * @return Returns the lowest JFAP priority that has been used on this session
    */
   protected int getLowestPriority()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getLowestPriority");
      checkNotBrowserSession();                                   // F171893
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getLowestPriority", ""+lowestPriority);
      return lowestPriority;
   }

   /**
    * Sets the lowest priority that has been used on this session
    *
    * @param lowestPriority
    */
   protected void setLowestPriority(int lowestPriority)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "setLowestPriority", lowestPriority);

      checkNotBrowserSession();                                   // F171893

      // TODO We could update the lowest priority a bit more intelligently
      if (lowestPriority < this.lowestPriority)
      {
         this.lowestPriority = lowestPriority;
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "setLowestPriority");
   }
   // End of change for f169884

   /**
    * @return Returns the consumer session Id that is being used to reference this
    * consumer session in the server side object store.
    */
   protected short getConsumerSessionId()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getConsumerSessionId");
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getConsumerSessionId", consumerSessionId);
      return consumerSessionId;
   }

   /**
    * Sets the consumer session id.
    *
    * @param consumerSessionId
    */
   protected void setConsumerSessionId(short consumerSessionId)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "setConsumerSessionId", consumerSessionId);
      this.consumerSessionId = consumerSessionId;
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "setConsumerSessionId");
   }

   // f171177 start
   /**
    * @return Returns the amount of bytes that a read ahead consumer has
    * requested.
    */
   protected int getRequestedBytes()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getRequestedBytes");
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getRequestedBytes", requestedBytes);
      return requestedBytes;
   }

   /**
    * Sets the amount of bytes that a read ahead consumer has
    * requested.
    *
    * @param requestedBytes
    */
   protected void setRequestedBytes(int requestedBytes)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "setRequestedBytes", requestedBytes);
      this.requestedBytes = requestedBytes;
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "setRequestedBytes");
   }
   // f171177 end

   // f172297 start
   /**
    * @return This method will return the message batch number that async messages
    * should be sent with.
    */
   protected short getMessageBatchNumber()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getMessageBatchNumber");
      checkNotBrowserSession();                                   // F171893
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getMessageBatchNumber", messageBatchNumber);
      return messageBatchNumber;
   }

   /**
    * This method will increment the message batch number and is usually
    * done when the client requests an <code>unlockAll()</code>.
    */
   protected void incremenetMessageBatchNumber()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "incremenetMessageBatchNumber");
      checkNotBrowserSession();                                   // F171893
      this.messageBatchNumber++;
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Message batch number is now: " + messageBatchNumber);
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "incremenetMessageBatchNumber");
   }
   // f172297 end

   // Start f187521.2.1
   /**
    * @return Returns the sessions unrecoverable reliability
    */
   protected Reliability getUnrecoverableReliability()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getUnrecoverableReliability");
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getUnrecoverableReliability", unrecoverableReliability);
      return unrecoverableReliability;
   }

   // End f187521.2.1

   /**
    * Performs a receive on this consumer. This is only a valid operation when the
    * consumer is in synchronous mode. If the sub consumer has not been set up
    * then this has to be created here.
    *
    * @param requestNumber The JFAP request.
    * @param tran The transaction to use.
    * @param timeout The timeout for the receive.
    */
   public void receive(int requestNumber,
                       int tran,
                       long timeout)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "receive",
                                           new Object[]
                                           {
                                              requestNumber,
                                              tran,
                                              timeout
                                           });

      if (subConsumer == null)
      {
         subConsumer = new CATSessSynchConsumer(this);
      }

      subConsumer.receive(requestNumber, tran, timeout);    // f177889  // f187521.2.1

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "receive");
   }

   /**
    * Closes the consumer. This call is delegated to the sub consumer if
    * one exists or the <code>CATConsumer</code> version is used.
    *
    * @param requestNumber The request number which replies should be sent to.
    */
   public void close(int requestNumber)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "close", requestNumber);

      if (subConsumer != null)
      {
         subConsumer.close(requestNumber);
      }
      else
      {
         super.close(requestNumber);
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "close");
   }

   //  SIB0115d.comms start

   /**
    * Sets the async callback to be used for this consumer session.
    * This method will set up the subconsumer that will be used for
    * this consumer session. If read ahead is permitted then the
    * <code>CATProxyConsumer</code> is used as the subconsumer,
    * otherwise the <code>CATAsynchConsumer</code> is used. The
    * <code>setAsynchConsumerCallback</code> call is then delegated
    * to the subconsumer.
    *
    * @param requestNumber The request number which replies should be sent to.
    * @param maxActiveMessages
    * @param messageLockExpiry
    * @param batchsize The batch size.
    * @param orderContext The ordering context to use
    */
   public void setAsynchConsumerCallback(int requestNumber,
                                         int maxActiveMessages,
                                         long messageLockExpiry,
                                         int batchsize,
                                         OrderingContext orderContext)
   {
     if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "setAsynchConsumerCallback",
                                           new Object[]
                                           {
                                              requestNumber,
                                              maxActiveMessages,
                                              messageLockExpiry,
                                              batchsize,
                                              orderContext
                                           });

     setAsynchConsumerCallback(requestNumber,maxActiveMessages,messageLockExpiry,batchsize,orderContext, false, 0, 0);

     if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "setAsynchConsumerCallback");
   }

   /**
    * Sets the async callback to be used for this consumer session.
    * This method will set up the subconsumer that will be used for
    * this consumer session. If read ahead is permitted then the
    * <code>CATProxyConsumer</code> is used as the subconsumer,
    * otherwise the <code>CATAsynchConsumer</code> is used. The
    * <code>setAsynchConsumerCallback</code> call is then delegated
    * to the subconsumer.
    *
    * @param requestNumber The request number which replies should be sent to.
    * @param maxActiveMessages
    * @param messageLockExpiry
    * @param batchsize The batch size.
    * @param orderContext The ordering context to use
    * @param stoppable
    * @param maxSequentialFailures
    * @param hiddenMessageDelay
    */
   public void setAsynchConsumerCallback(int requestNumber,
                                         int maxActiveMessages,
                                         long messageLockExpiry,
                                         int batchsize,
                                         OrderingContext orderContext,              // f200337, F219476.2
                                         boolean stoppable,                                             //SIB0.comms
                                         int maxSequentialFailures,
                                         long hiddenMessageDelay)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "setAsynchConsumerCallback",
                                           new Object[]
                                           {
                                              requestNumber,
                                              maxActiveMessages,
                                              messageLockExpiry,
                                              batchsize,
                                              orderContext,
                                              stoppable,
                                              maxSequentialFailures,
                                              hiddenMessageDelay
                                           });

      checkNotBrowserSession();                                   // F171893

      if (getReadAheadPermitted())
      {
         if ((maxActiveMessages == 0) && (messageLockExpiry == 0) && !stoppable)
         {
            subConsumer = new CATProxyConsumer(this);
         }
         else
         {
            try
            {
               getConsumerSession().stop();
            }
            catch (SIException e)
            {
               //No FFDC Code Needed
               if(TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                  SibTr.debug(this, tc, "Caught SIException on calling stop on ConsumerSession.", e);

               //Throw a RuntimeException which can be caught by our caller as needed
               throw new RuntimeException(e.getMessage(), e);
            }
            subConsumer = new CATAsynchConsumer(this);
         }
      }
      else
      {
         subConsumer = new CATAsynchConsumer(this);
      }

        subConsumer.setAsynchConsumerCallback(requestNumber,
                                              maxActiveMessages,
                                              messageLockExpiry,
                                              batchsize,
                                              orderContext,                           // f200337, F219476.2
                                              stoppable,
                                              maxSequentialFailures,
                                              hiddenMessageDelay);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "setAsynchConsumerCallback");
   }

   //  SIB0115d.comms end

   /**
    * This method will unset the asynch consumer callback. This means that the
    * client has requested that the session should be converted from asynchronous to
    * synchronous and so the sub consumer must be changed
    *
    * @param requestNumber
    */
   public void unsetAsynchConsumerCallback(int requestNumber, boolean stoppable)                        //SIB0115d.comms
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "unsetAsynchConsumerCallback", "requestNumber="+requestNumber+",stoppable="+stoppable);

      checkNotBrowserSession();                                   // F171893

      if (subConsumer != null)
      {
         subConsumer.unsetAsynchConsumerCallback(requestNumber, stoppable);                             //SIB0115d.comms
      }

      subConsumer = null;

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "unsetAsynchConsumerCallback");
   }

   /**
    * Start the consumer
    *
    * @param requestNumber The request number which replies should be sent to.
    * @param deliverImmediately Whether we should deliver on this thread if we can.
    * @param sendReply Whether a reply is needed for this flow
    * @param sendListener A send listener that will be notified when the start reply is sent
    */
   public void start(int requestNumber, boolean deliverImmediately, boolean sendReply, SendListener sendListener) { //471642
     if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "start",
                                          new Object[]{requestNumber, deliverImmediately,sendReply,sendListener});

     start(requestNumber, deliverImmediately, sendReply, sendListener, false);

     if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "start");
   }                                                                                                            //471642

   /**
    * Start the consumer provided restart wasn't specified and message processor hasn't stopped the stoppable session. This
    * call is delegated to the sub consumer if one exists or the <code>CATConsumer</code> version is used.
    *
    * @param requestNumber The request number which replies should be sent to.
    * @param deliverImmediately Whether we should deliver on this thread if we can.
    * @param sendReply Whether a reply is needed for this flow
    * @param sendListener A send listener that will be notified when the start reply is sent
    * @param restart a stoppable session only if not stopped by message processor
    */
   public void start(int requestNumber, boolean deliverImmediately, boolean sendReply, SendListener sendListener, boolean restart) //471642
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "start",
                                           new Object[]{requestNumber,
                                                        deliverImmediately,
                                                        sendReply,
                                                        sendListener,
                                                        restart                                                 //471642
                                                        });

      if (restart && stoppableSessionStopped) {                                                                 //471642
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Stoppable session now stopped so ignoring the restart request");
      } else {                                                                                                  //471642
        stoppableSessionStopped = false;                                                                        //471642

        // We need to lock this down with a mutex lock here as it is possible that we could call
        // start on the sub consumer (or super class) causing the client to get notified that we
        // have started. If the client then decides to perform another operation which requires
        // checking if the session is started (say by calling isStarted()) we still may not have set
        // the started flag to true by the time they call this - causing really weird results (see
        // defect 347591).
        // As such, we lock around the start call by waiting on the semaphore before entering the
        // critical region. We then send the start and the SessionStartSendListener() will post the
        // semaphore when the data is actually sent. The started flag is updated just before the
        // semaphore is posted.

        checkNotBrowserSession();
        
        SessionStartSendListener listener = new SessionStartSendListener();
        startSemaphore.waitOnIgnoringInterruptions();

        try {
            if (subConsumer != null) 
            {
               subConsumer.start(requestNumber, deliverImmediately, sendReply, listener);
            } 
            else 
            {
               super.start(requestNumber, deliverImmediately, sendReply, listener);
            }
         } 
        catch (RuntimeException e) {
            FFDCFilter.processException(e, CLASS_NAME + ".start",
                                        CommsConstants.CATMAINCONSUMER_START_01, this,
                                        new Object[]{subConsumer});
            
           // the listener may never post because of this, so we post now
           // to ensure the semaphore is not left dangling
           listener.errorOccurred(e);
           if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "start", e); 
           throw e;
         }
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "start");
   }                                                                                                            //471642

   /**
    * Stops the consumer. This call is delegated to the sub consumer if
    * one exists or the <code>CATConsumer</code> version is used.
    *
    * @param requestNumber The request number which replies should be sent to.
    */
   public void stop(int requestNumber)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "stop", requestNumber);

      // We need to lock this down with a semaphore here to ensure that the 'started' flag is
      // correct by callers of isStarted() - see defect 347591

      checkNotBrowserSession();
      
      SessionStopSendListener listener = new SessionStopSendListener();
      startSemaphore.waitOnIgnoringInterruptions();

      try {
          if (subConsumer != null)
          {
              subConsumer.stop(requestNumber, listener);
          }
          else
          {
              super.stop(requestNumber, listener);
          }
      }
      catch (RuntimeException e) {
          FFDCFilter.processException(e, CLASS_NAME + ".stop",
                                      CommsConstants.CATMAINCONSUMER_STOP_01, this,
                                      new Object[]{subConsumer});

         // the listener may never post because of this, so we post now
         // to ensure the semaphore is not left dangling
         listener.errorOccurred(e);
         if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "stop", e); 
         throw e;
      }
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "stop");
   }

   /**
    * A send listener that is notified when the reply to a start has been sent. At this point
    * we can mark the session as officially started and post the semaphore to wake up any waiters.
    *
    * @author Gareth Matthews
    */
   private class SessionStartSendListener implements SendListener
   {
      private boolean hasPosted = false;
      
      public synchronized void dataSent(Conversation conversation)
      {
         started = true;
         if (! hasPosted)
         {
            startSemaphore.post();
            hasPosted = true;
         }
      }

      public synchronized void errorOccurred(SIConnectionLostException exception, Conversation conversation)
      {
         if (! hasPosted)
         {
            startSemaphore.post();
            hasPosted = true;
         }
      }
      
      public synchronized void errorOccurred(RuntimeException e)
      {
         if (! hasPosted)
         {
            startSemaphore.post();
            hasPosted = true;
         }
      }
   }

   /**
    * A send listener that is notified when the reply to a start has been sent. At this point
    * we can mark the session as officially started and post the semaphore to wake up any waiters.
    *
    * @author Gareth Matthews
    */
   private class SessionStopSendListener implements SendListener
   {
      private boolean hasPosted = false;
      
      public synchronized void dataSent(Conversation conversation)
      {
         started = false;
         if (!hasPosted)
         {
            startSemaphore.post();
            hasPosted = true;
         }
      }

      public synchronized void errorOccurred(SIConnectionLostException exception, Conversation conversation)
      {
         if (! hasPosted)
         {
            startSemaphore.post();
            hasPosted = true;
         }
      }
      
      public synchronized void errorOccurred(RuntimeException e)
      {
         if (! hasPosted)
         {
            startSemaphore.post();
            hasPosted = true;
         }
      }
   }

   /**
    * Unlocks a set of messages locked by this consumer. This call
    * is delegated to the sub consumer if one exists or the
    * <code>CATConsumer</code> version is used.
    *
    * @param requestNumber The request number which replies should be sent to.
    * @param msgHandles
    * @param reply
    */
   public void unlockSet(int requestNumber, SIMessageHandle[] msgHandles, boolean reply)                  // f199593, F219476.2
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "unlockSet",
                                           new Object[]
                                           {
                                              requestNumber,
                                              msgHandles,
                                              reply
                                           });

      checkNotBrowserSession();                                   // F171893

      if (subConsumer != null)
      {
         subConsumer.unlockSet(requestNumber, msgHandles, reply);                             // f199593, F219476.2
      }
      else
      {
         super.unlockSet(requestNumber, msgHandles, reply);                                   // f199593, F219476.2
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "unlockSet");
   }
   
   /**
    * Unlocks a set of messages locked by this consumer. This call 
    * is delegated to the sub consumer if one exists or the 
    * <code>CATConsumer</code> version is used.
    * 
    * @param requestNumber The request number which replies should be sent to.
    * @param msgIds
    * @param reply 
    * @param incrementLockCount Indicates whether the lock count should be incremented for this unlock
    */
   public void unlockSet(int requestNumber, SIMessageHandle[] msgHandles, boolean reply, boolean incrementLockcount)                  // f199593, F219476.2
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "unlockSet",
                                           new Object[]
                                           {
                                              requestNumber,
                                              msgHandles,
                                              reply,
                                              incrementLockcount
                                           });

      checkNotBrowserSession();                                   // F171893

      if (subConsumer != null)
      {
         subConsumer.unlockSet(requestNumber, msgHandles, reply, incrementLockcount);                             // f199593, F219476.2
      }
      else
      {
         super.unlockSet(requestNumber, msgHandles, reply, incrementLockcount);                                   // f199593, F219476.2
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "unlockSet");
   }

   // Start f199593
   /**
    * Reads a set of messages that are currently locked by this consumer
    *
    * @param requestNumber The request number which replies should be sent to.
    * @param msgHandles The message ids to read.
    */
   public void readSet(int requestNumber, SIMessageHandle[] msgHandles)          // F219476.2
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "readSet",
                                           new Object[]{requestNumber, msgHandles});

      checkNotBrowserSession();

      if (subConsumer != null)
      {
         subConsumer.readSet(requestNumber, msgHandles);                         // F219476.2
      }
      else
      {
         super.readSet(requestNumber, msgHandles);                               // F219476.2
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "readSet");
   }

   /**
    * Reads a set of messages that are currently locked
    *
    * @param requestNumber The request number which replies should be sent to.
    * @param msgHandles The msgids to read and delete
    * @param tran The transaction to use for the delete.
    */
   public void readAndDeleteSet(int requestNumber, SIMessageHandle[] msgHandles, int tran) // F219476.2
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "readAndDeleteSet",
                                           new Object[]{requestNumber, msgHandles, tran});

      checkNotBrowserSession();

      if (subConsumer != null)
      {
         subConsumer.readAndDeleteSet(requestNumber, msgHandles, tran); // F219476.2
      }
      else
      {
         super.readAndDeleteSet(requestNumber, msgHandles, tran);       // F219476.2
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "readAndDeleteSet");
   }
   // End f199593

   /**
    * Deletes a set of messages locked by this consumer. This call
    * is delegated to the sub consumer if one exists or the
    * <code>CATConsumer</code> version is used.
    *
    * @param requestNumber The request number which replies should be sent to.
    * @param msgHandles The msg ID's
    * @param tran The transaction to use.
    * @param reply Whether the client is expecting a reply.
    */
   public void deleteSet(int requestNumber,
                         SIMessageHandle[] msgHandles,
                         int tran,
                         boolean reply) // f174317 // f187521.2.1, F219476.2
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "deleteSet",
                                           new Object[]
                                           {
                                              requestNumber,
                                              msgHandles,
                                              tran,
                                              reply
                                           });

      checkNotBrowserSession();                                   // F171893

      if (subConsumer != null)
      {
         subConsumer.deleteSet(requestNumber, msgHandles, tran, reply);  // f187521.2.1, F219476.2
      }
      else
      {
         super.deleteSet(requestNumber, msgHandles, tran, reply);        // f187521.2.1, F219476.2
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "deleteSet");
   }

   /**
    * Unlocks all messages locked by this consumer. This call
    * is delegated to the sub consumer if one exists or the
    * <code>CATConsumer</code> version is used.
    *
    * @param requestNumber The request number which replies should be sent to.
    */
   public void unlockAll(int requestNumber)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "unlockAll", requestNumber);

      checkNotBrowserSession();                                   // F171893

      if (subConsumer != null)
      {
         subConsumer.unlockAll(requestNumber);
      }
      else
      {
         super.unlockAll(requestNumber);
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "unlockAll");
   }

   //f171177 begin
   /**
    * Flushes this consumer. This call
    * is delegated to the sub consumer if one exists or the
    * <code>CATConsumer</code> version is used.
    *
    * @param requestNumber The request number which replies should be sent to.
    */
   public void flush(int requestNumber)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "flush", requestNumber);

      if (subConsumer != null)
      {
         subConsumer.flush(requestNumber);
      }
      else
      {
         super.flush(requestNumber);
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "flush");
   }

   /**
    * Requests more messages for this consumer. This call
    * is delegated to the sub consumer if one exists or the
    * <code>CATConsumer</code> version is used.
    *
    * @param requestNumber The request number which replies should be sent to.
    * @param receiveBytes
    * @param requestedBytes
    */
   public void requestMsgs(int requestNumber, int receiveBytes, int requestedBytes)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "requestMsgs",
                                           new Object[]
                                           {
                                              requestNumber,
                                              receiveBytes,
                                              requestedBytes
                                           });

      if (subConsumer != null)
      {
         subConsumer.requestMsgs(requestNumber, receiveBytes, requestedBytes);
      }
      else
      {
         super.requestMsgs(requestNumber, receiveBytes, requestedBytes);
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "requestMsgs");
   }
   //f171177 end

   /**
    * Helper function that checks we are not "wrapping" a browser session.
    * Invoked at the start of methods which are no valid for browser
    * sessions.
    */
   // begin F171893
   private void checkNotBrowserSession()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "checkNotBrowserSession");

      if (browserSession != null)
      {
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Browser session != null", browserSession);

         // The caller of this method was expecting that this consumer wrapper was not being
         // used for browser sessions but it is.
         SIErrorException e = new SIErrorException(
            nls.getFormattedMessage("BROWSER_SESSION_UNEXPECTED_SICO2041", null, null)
         );

         FFDCFilter.processException(e, CLASS_NAME + ".checkNotBrowserSession",
                                     CommsConstants.CATMAINCONSUMER_CHECKNOTBROWSER_01, this);

         if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "checkNotBrowserSession");
         throw e;
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "checkNotBrowserSession");
   }
   // end F171893

   /**
    * Returns the browser session that this main consumer is wrapping.  It is
    * not valid to invoke this method on a non-browser session (in fact it
    * throws an exception if you try that).
    */
   // begin F171893
   protected BrowserSession getBrowserSession()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getBrowserSession");

      if (browserSession == null)
      {
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Browser session == null");

         // The caller of this method was expecting that this consumer wrapper was being
         // used for browser sessions but it is not.
         SIErrorException e = new SIErrorException(
            nls.getFormattedMessage("BROWSER_SESSION_EXPECTED_SICO2042", null, null)
         );

         FFDCFilter.processException(e, CLASS_NAME + ".getBrowserSession",
                                     CommsConstants.CATMAINCONSUMER_CHECKNOTBROWSER_02, this);

         if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getBrowserSession");
         throw e;
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getBrowserSession", browserSession);
      return browserSession;
   }
   // end F171893

   // Start f199593
   /**
    * This method will put the main consumer into bifurcated mode.
    *
    * @param sess The bifurcated session.
    */
   public void setBifurcatedSession(BifurcatedConsumerSession sess)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "setBifurcatedSession", sess);

      subConsumer = new CATBifurcatedConsumer(this, sess);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "setBifurcatedSession");
   }
   // End f199593

   public void stopStoppableSession () {                                                                        //471642
     if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "stopStoppableSession");
     stoppableSessionStopped = true;
     if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "stopStoppableSession");
   }                                                                                                            //471642

   // Start D209401
   /**
    * @return Returns the state of the consumer.
    */
   public String toString()
   {
      return getClass().getName() + "@" + Integer.toHexString(hashCode()) +
             ": started: " + started +
             ", readAhead: " + readAheadPermitted +
             ", unrecovReliability: " + unrecoverableReliability +
             ", batchNumber: " + messageBatchNumber +
             ", noLocal: " + noLocal +
             ", requestedBytes: " + requestedBytes +
             ", -> SubConsumer: " + subConsumer;
   }
   // End D209401
   
   /**
    * Unlocks all messages locked by this consumer and has an Option to 
    * increment the unlock count or  not on unlock of messages. This call
    * is delegated to the sub consumer if one exists or the
    * <code>CATConsumer</code> version is used.
    *
    * @param requestNumber The request number which replies should be sent to.
    * @param incrementUnlockCount Option to increment the unlock count or  not on unlock of messages
    */
   public void unlockAll(int requestNumber, boolean incrementUnlockCount)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "unlockAll", new Object[]{requestNumber,incrementUnlockCount});

      checkNotBrowserSession();                                   // F171893

      if (subConsumer != null)
      {
         subConsumer.unlockAll(requestNumber,incrementUnlockCount);
      }
      else
      {
         super.unlockAll(requestNumber,incrementUnlockCount);
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "unlockAll");
   }
}
