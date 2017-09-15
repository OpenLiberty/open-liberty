/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.processor.gd;

// Import required classes.
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.util.am.AlarmListener;
import com.ibm.websphere.sib.Reliability;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.admin.JsConstants;
import com.ibm.ws.sib.mfp.JsMessage;
import com.ibm.ws.sib.mfp.control.ControlNack;
import com.ibm.ws.sib.msgstore.AbstractItem;
import com.ibm.ws.sib.msgstore.MessageStoreException;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.impl.LinkHandler;
import com.ibm.ws.sib.processor.impl.MessageProcessor;
import com.ibm.ws.sib.processor.impl.exceptions.InvalidMessageException;
import com.ibm.ws.sib.processor.impl.interfaces.BatchListener;
import com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler;
import com.ibm.ws.sib.processor.impl.interfaces.DownstreamControl;
import com.ibm.ws.sib.processor.impl.interfaces.HealthStateListener;
import com.ibm.ws.sib.processor.impl.interfaces.SIMPMessage;
import com.ibm.ws.sib.processor.runtime.HealthState;
import com.ibm.ws.sib.processor.runtime.SIMPDeliveryTransmitControllable;
import com.ibm.ws.sib.processor.runtime.impl.SourceStreamControl;
import com.ibm.ws.sib.processor.utils.am.MPAlarmManager;
import com.ibm.ws.sib.transactions.TransactionCommon;
import com.ibm.ws.sib.utils.SIBUuid12;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * A source stream for point to point
 *
 */
public class SourceStream extends ControllableStream implements BatchListener
{

  private static final TraceComponent tc =
    SibTr.register(
      SourceStream.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);

  private StreamSet streamSet;
  
  private SourceStreamState sourceStreamState = SourceStreamState.ACTIVE;

  private int priority;
  private Reliability reliability;
  private DownstreamControl downControl;

  private StateStream oststream;

  private long oack = 0;

  // True if we have started the timer to send AckExpected
  private boolean ackExpAlarmStarted = false;

  // The expiry handle and timer handle for AckExpected 
  private final AckExpExpiryHandler ackExpExpiryHandler;
  
  // The alarm handle which checks the front of the stream for healthy usage
  private BlockedStreamAlarm blockedStreamAlarm;

  // A handle to the DestinationHandler object that owns this SourceStream
  private DestinationHandler dh;
  
  // A handle to the MessageProcessor, which holds the custom properties
  private MessageProcessor mp;

  // The alarm manager to use for AckExpected alarms
  private MPAlarmManager am;

  // Tick of last message written into the stream
  private long lastMsgAdded = 0;
  
  // Tick of last message sent by the stream
  private long lastMsgSent = 0;

  // This is the last Value tick before the timer started
  // We send AckExpected for everything from completedPrefix to this tick when
  // the timer expires.
  private long timerStartSendTick = 0;

  // Interval to wait before sending AckExpected message. 
  // This interval is doubled each time the AckExpected alarm expires 
  // until it reaches the GDConfig.GD_ACK_EXPECTED_THRESHOLD_MAX
  // If an alarm is started for a new message this field is reset to its initial value
  private long ackExpInterval = GDConfig.GD_ACK_EXPECTED_THRESHOLD;
   
  // The stream that this data structure is associated with
  private SIBUuid12 stream;
  
  // This indicates whether the stream contains any ticks for which 
  // we have had to guess that this is the correct stream to put
  // them in. 
  //TODO must start as true once reallocator is called at startup 
  private boolean containsGuesses = false; 
  
  // This is the number of messages which the administrator has defined
  // can be indoubt ( ie awaiting an Ack ) on this stream.
  // If there are no messages in the stream which are 'guesses' then
  // this is the value which will be used to limit the indoubt messages. 
  private long definedSendWindow = 1000; // This value is overwritten immediately
                                         // using initialiseSendWindow()
  
  // This is the current value of the sendWindow.
  private long sendWindow = definedSendWindow;
  private long firstMsgOutsideWindow = RangeList.INFINITY;
  
  // This is the total number of Uncommitted and Value messages in the stream
  private long totalMessages = 0;
  
  private long totalMessagesSent=0;
  private long timeLastMsgSent=0;
  
  private SourceStreamControl srcStreamControl;
  
  // Messages to be deleted when source batch completes
  // This list is used to update the sendWindow and send 
  // any messages which move inside it when the batch completes 
  List batchList = null; 
  List batchSendList = null; 
  
  // Indicator that we`ve heard from the target end recently
  private boolean inboundFlow = true;

  // The latest tick we sent an ackExcpected for on this stream
  private long lastAckExpTick;
  // The latest tick we received a Nack for on this stream
  private long lastNackReceivedTick;
  
  /**
   * An enumeration for the different states of a source stream
   * 
   * @author tpm100
   */
  public static class SourceStreamState 
    implements SIMPDeliveryTransmitControllable.StreamState
  {
    //TODO In future releases we might expose new stream states
    public static final SourceStreamState ACTIVE = 
      new SourceStreamState("Active", 0);
      
    private String state;
    private int value=-1;
    private SourceStreamState(String _state, int _value)
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(tc, "SourceStreamState", new Object[] {_state, Integer.valueOf(_value)});
      
      this.state = _state;
      this.value = _value;
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "SourceStreamState", this);            
    }
    public String toString()
    {
      return state;
    }
    
    public int getValue()
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(tc, "getValue"); 
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "getValue", Integer.valueOf(value)); 
      return value;
    }
  }
  
  public SourceStream(
    int priority,
    Reliability reliability,
    DownstreamControl downControl,
    List scratch,
    StreamSet streamSet,
    MPAlarmManager am,
    DestinationHandler dh)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "SourceStream", 
        new Object[]{Integer.valueOf(priority), reliability, downControl, scratch, streamSet, am, dh});

    // synchronization ensures that other synchronized methods will see values
    // initialized in constructor
    synchronized (this)
    {
      // the state stream is created here
      oststream = new StateStream();
      oststream.init();

      this.downControl = downControl;

      this.priority = priority;
      this.reliability = reliability;
      this.streamSet = streamSet;
      this.stream = streamSet.getStreamID();
      this.am = am;
      this.dh = dh;
      this.mp = dh.getMessageProcessor();

      //Initialize the expiry handler for handling timeouts
      ackExpExpiryHandler = new AckExpExpiryHandler(this);

      this.batchList = new ArrayList();
      this.batchSendList = new ArrayList();
       
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "SourceStream", this);

  }

  public int getPriority()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(this, tc, "getPriority");
      SibTr.exit(tc,  "getPriority", Integer.valueOf(priority));      
    }
    return priority;
  }

  public Reliability getReliability()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(this, tc, "getReliability");
      SibTr.exit(tc,  "getReliability", reliability);      
    }
    return reliability;
  }

  public long getCompletedPrefix()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(this, tc, "getCompletedPrefix");
         
    long returnValue = oststream.getCompletedPrefix();
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc,  "getCompletedPrefix", Long.valueOf(returnValue));
    return returnValue;
  }

  public long getAckPrefix()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(this, tc, "getAckPrefix");
      SibTr.exit(tc,  "getAckPrefix", Long.valueOf(oack));      
    }
    return oack;
  }

  public synchronized long getLastMsgAdded()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(this, tc, "getLastMsgAdded");
      SibTr.exit(tc,  "getLastMsgAdded", Long.valueOf(lastMsgAdded));      
    }
    return lastMsgAdded;
  }
  
  public synchronized long getLastMsgSent()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(this, tc, "getLastMsgSent");
      SibTr.exit(tc,  "getLastMsgSent", Long.valueOf(lastMsgSent));      
    }
    return lastMsgSent;
  }

  public synchronized long getTotalMessages()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(this, tc, "getTotalMessages");
      SibTr.exit(tc,  "getTotalMessages", Long.valueOf(totalMessages));      
    }
    return totalMessages;
  }
  
  public synchronized long getTotalMessagesSent()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(this, tc, "getTotalMessagesSent");
      SibTr.exit(tc,  "getTotalMessagesSent", Long.valueOf(totalMessagesSent));      
    }
    return totalMessagesSent;
  }
  
  public synchronized long getLastMsgSentTime()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(this, tc, "getLastMsgSentTime");
      SibTr.exit(tc,  "getLastMsgSentTime", Long.valueOf(timeLastMsgSent));      
    }
    return timeLastMsgSent;
  }

  public synchronized long getFirstMsgOutsideWindow()
  {
    if(TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(this, tc, "getFirstMsgOutsideWindow");
      SibTr.exit(tc,  "getFirstMsgOutsideWindow", Long.valueOf(firstMsgOutsideWindow));
    }
    return firstMsgOutsideWindow;
  }
  
  public synchronized long getSendWindow()
  {
    if(TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(this, tc, "getSendWindow");
      SibTr.exit(tc,  "getSendWindow", Long.valueOf(sendWindow));
    }
    return sendWindow;
  }
  
  public synchronized boolean containsGuesses()
  {
    if(TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(this, tc, "containsGuesses");
      SibTr.exit(tc,  "containsGuesses", Boolean.valueOf(containsGuesses));
    }
    return containsGuesses;
  }

  // Used for debug
  public StateStream getStateStream()
  {
    if(TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(this, tc, "getStateStream");
      SibTr.exit(tc,  "getStateStream", oststream);
    }
    return oststream;
  }
  
  public SourceStreamState getStreamState()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(this, tc, "getStreamState");
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getStreamState", sourceStreamState);      
    return sourceStreamState;
  }

  /**
   * This method uses a Value message to write an Uncommitted tick
   * into the stream.
   * It is called at preCommit time.
   *
   * @param m The value message to write to the stream
   *
   * @exception GDException  Can be thrown from the writeRange method
   *
   */
  public void writeUncommitted(SIMPMessage m) throws SIResourceException
  {

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(this, tc, "writeUncommitted", new Object[] { m });

    TickRange tr = null;

    JsMessage jsMsg = m.getMessage();
   
    long stamp = jsMsg.getGuaranteedValueValueTick();
    long starts = jsMsg.getGuaranteedValueStartTick();
    long ends = jsMsg.getGuaranteedValueEndTick();

    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
    {
      SibTr.debug(tc, "writeUncommitted at: " + stamp + " on Stream " +  stream);
    }

    synchronized (this)
    {      
      if (m.getStreamIsGuess())
      {
        newGuessInStream(stamp);
      }
      msgAdded(stamp);

      // write into stream
      tr = TickRange.newUncommittedTick(stamp);
      tr.startstamp = starts;
      tr.endstamp = ends;
    
      // Save message in the stream while it is Uncommitted
      // It will be replaced by its index in the ItemStream once it becomes a Value
      tr.value = m;

      oststream.writeCombinedRange(tr);
 
      // Now update our lastMsgAdded with the tick of the
      // message we have just added
      lastMsgAdded = stamp;
      
      // SIB0105
      // If this is the first uncommitted messages to be added to the stream, start off the 
      // blocked message health state timer. This will check back in the future to see if the
      // message successfully sent.
      if (blockedStreamAlarm==null)
      {
        blockedStreamAlarm = new BlockedStreamAlarm(getCompletedPrefix());
        am.create(mp.getCustomProperties().getBlockedStreamHealthCheckInterval(), blockedStreamAlarm);
      }

    } // end synchronized

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "writeUncommitted");
  }

  /**
    * This method uses a Value message to write a Value tick
    * into the stream.
    * It is called at postCommit time.
    *
    * @param m The value message to write to the stream
    *
    * @return boolean true if the message can be sent downstream
    *                      this is determined by the sendWindow 
    */
  public boolean writeValue(SIMPMessage m) throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(this, tc, "writeValue", m);

    TickRange tr = null;
    boolean sendMessage = true;
    
    long msgStoreId = AbstractItem.NO_ID;
    try
    {
      if (m.isInStore())
        msgStoreId = m.getID();
    }
    catch(MessageStoreException e)
    {
      // FFDC
      FFDCFilter.processException(
          e,
          "com.ibm.ws.sib.processor.gd.SourceStream.writeValue",
          "1:520:1.138",
          this);  
                  
      SibTr.exception(tc, e); 
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "writeValue", e);
      
      throw new SIResourceException(e);
    }
      
    JsMessage jsMsg = m.getMessage();

    long stamp = jsMsg.getGuaranteedValueValueTick();
    long starts = jsMsg.getGuaranteedValueStartTick();
    long ends = jsMsg.getGuaranteedValueEndTick();

    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
    {
      SibTr.debug(tc, "writeValue at: " + stamp + " with Value from: " + starts + " to " + ends + " on Stream " +  stream);
    }

    synchronized (this)
    {
      // Now update Value tick to indicate it is now committed
      tr = TickRange.newValueTick(stamp, null, msgStoreId);
      tr.startstamp = starts;
      tr.endstamp = ends;

      oststream.writeCombinedRange(tr);
      sendMessage = msgCanBeSent(stamp, false);

      if (sendMessage)
      {
        // Only move the lastMsgSent forward, not backwards. Other threads with greater
        // ticks could already have passed through here and moved the lastMsgSent on past
        // this tick
        if(stamp > lastMsgSent)
          lastMsgSent = stamp;
        
        if (!ackExpAlarmStarted)
        {
          ackExpAlarmStarted = true;
          timerStartSendTick = lastMsgSent;
          ackExpInterval = GDConfig.GD_ACK_EXPECTED_THRESHOLD;
          
          am.create(ackExpInterval, ackExpExpiryHandler);
        }
        
        // SIB0105
        // Message committed so reset the health state if we've solved a blocking problem
        if (blockedStreamAlarm != null)
          blockedStreamAlarm.checkState(false);        
      }

    } // end synchronized

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "writeValue", Boolean.valueOf(sendMessage));

    return sendMessage;
  }

  /**
   * This method uses a Value message to write Silence into the stream,
   * because a message has been rolled back
   * 
   * @param m The value message 
   * 
   * @exception GDException  Can be thrown from the writeCompletedRange method
   *
   * @return boolean true if the message can be sent downstream
   *                      this is determined by the sendWindow 
   */
  public boolean writeSilence(SIMPMessage m) throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(this, tc, "writeSilence", new Object[] { m });

    boolean sendMessage = true;
    JsMessage jsMsg = m.getMessage();

    // There may be Completed ticks after the Value, if so then
    // write these into the stream too
    long stamp = jsMsg.getGuaranteedValueValueTick();
    long start = jsMsg.getGuaranteedValueStartTick();
    long end  = jsMsg.getGuaranteedValueEndTick();

    if (end < stamp)
      end = stamp;

    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
    {
      SibTr.debug(tc, "writeSilence from: " + start + " to " + end + " on Stream " +  stream);
    }

    TickRange tr =
      new TickRange(
        TickRange.Completed,
        start,
        end);

    // used to send message if it moves inside inDoubt window
    List sendList = null;
    synchronized (this)
    {
      oststream.writeCompletedRange(tr);

      sendMessage = msgCanBeSent(stamp, false);

      if( sendMessage )
      {
        if ( stamp > lastMsgSent )
          lastMsgSent = stamp;
      }
    
      // Check whether removing this message means we have
      // a message to send
      TickRange tr1 = null;
      if ((tr1 = msgRemoved(stamp, oststream, null)) != null)
      {
        sendList = new ArrayList();
        sendList.add(tr1);
        if(tr1.valuestamp > lastMsgSent)
          lastMsgSent = tr1.valuestamp;
      }
      
      // SIB0105
      // Message silenced so reset the health state if we've solved a blocking problem
      if (blockedStreamAlarm != null)
        blockedStreamAlarm.checkState(false); 
    } // synchronized

    // Do the send outside of the synchronise
    if (sendList != null)
    {
      sendMsgs( sendList, false );
    }
 
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "writeSilence");

    return sendMessage;
  }

  /**
   * This method uses a Value message to write Silence into the stream,
   * because a message which was a Guess is being removed from the stream
   * It forces the stream to be updated to Silence without checking the 
   * existing state
   * 
   * @param m The value message 
   * 
   *
   */
  public boolean writeSilenceForced(SIMPMessage m) throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(this, tc, "writeSilenceForced", new Object[] { m });

    boolean msgRemoved = true;
    
    JsMessage jsMsg = m.getMessage();

    long start = jsMsg.getGuaranteedValueStartTick();
    long end  = jsMsg.getGuaranteedValueEndTick();

    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
    {
      SibTr.debug(tc, "writeSilenceForced from: " + start + " to " + end + " on Stream " +  stream);
    }
   
    TickRange tr =
      new TickRange(
        TickRange.Completed,
        start,
        end);

    // used to send message if it moves inside inDoubt window
    List sendList = null;
    
    synchronized (this)
    {
      // If the tick of the new silence has already been set to completed then
      // the message must already have been processed (either sent+acked or already
      // silenced) either way, we should class it as a 'removed' message.
      // This fixes a problem where a message is nacked,sent and acked all in the
      // window between setting it as a value and actually trying to lock it (in PtoPOutputHandler)
      // prior to sending it. In this situation, the message no longer exists in the
      // MsgStore, so PtoPOutputHandler believed it had expired, which then calls this
      // method to try to remove it - decrementing the totalMessage count and messing
      // up future sendWindow calculations (which can leave messages left unsent).
      if(end <= getCompletedPrefix())
      {
        msgRemoved = false;
        
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
          SibTr.debug(tc, "Message " + end + " already completed", m);
      }
      else
      {
          oststream.writeCompletedRangeForced(tr);
          
          // Check whether removing this message means we have
          // a message to send
          TickRange str = null;
          if ( (str = msgRemoved(jsMsg.getGuaranteedValueValueTick(), oststream, null)) != null)
          {
            sendList = new LinkedList();
            sendList.add(str);
            if(str.valuestamp > lastMsgSent)
              lastMsgSent = str.valuestamp;
          }  
      }
    }

    // Do the send outside of the synchronise
    if (sendList != null)
    {
      sendMsgs( sendList, false );
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "writeSilenceForced", Boolean.valueOf(msgRemoved));
    
    return msgRemoved;

  }

  /**
   * This method uses a Value TickRange to write Silence into the stream,
   * because a message has expired before it was sent and so needs to be removed
   * from the stream
   * It forces the stream to be updated to Silence without checking the 
   * existing state
   * 
   * @param m The value tickRange 
   * 
   *
   */
  public void writeSilenceForced(TickRange vtr) throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(this, tc, "writeSilenceForced", new Object[] { vtr });

    long start = vtr.startstamp;
    long end   = vtr.endstamp;

    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
    {
      SibTr.debug(tc, "writeSilenceForced from: " + start + " to " + end + " on Stream " +  stream);
    }
     
    TickRange tr =
      new TickRange(
        TickRange.Completed,
        start,
        end );
          
    // used to send message if it moves inside inDoubt window
    List sendList = null;

    synchronized (this)
    {
      oststream.writeCompletedRangeForced(tr);
    
      // Check whether removing this message means we have
      // a message to send
      TickRange str = null;
      if ( (str = msgRemoved(vtr.valuestamp, oststream, null)) != null)
      {
        sendList = new LinkedList();
        sendList.add(str);
        if(str.valuestamp > lastMsgSent)
          lastMsgSent = str.valuestamp;
      }
    }
   
    // Do the send outside of the synchronise
    if (sendList != null)
    {
      sendMsgs( sendList, false );
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "writeSilenceForced");

  }

  /**
   * This replaces the specified tick with Silence
   * without checking the existing state
   * 
   * @param m The tick to be removed from the stream 
   * 
   *
   */
  public void writeSilenceForced(long tick) throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(this, tc, "writeSilenceForced",  Long.valueOf(tick) );
    
    // Details of the new silence
    long startTick = -1;
    long endTick = -1;
    long completedPrefix = -1;
    
    // used to send message if it moves inside inDoubt window
    List sendList = null;
    
    synchronized (this)
    {
      oststream.setCursor(tick);

      // Get the TickRange containing this tick 
      TickRange tr = oststream.getNext();
        
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
      {
        SibTr.debug(tc, "writeSilenceForced from: " + tr.startstamp + " to " + tr.endstamp + " on Stream " +  stream);
      }
  
      TickRange silenceRange = oststream.writeCompletedRangeForced(tr);
      
      // While we have the stream locked cache the details of the new completed
      // range
      if(silenceRange != null)
      {
        startTick = silenceRange.startstamp;
        endTick = silenceRange.endstamp;
        completedPrefix = getCompletedPrefix();
      }
      
      // Check whether removing this message means we have
      // a message to send
      TickRange str = null;
      if ( (str = msgRemoved(tick, oststream, null)) != null)
      {
        sendList = new LinkedList();
        sendList.add(str);
        if(str.valuestamp > lastMsgSent)
          lastMsgSent = str.valuestamp;
      }
    }
    
    // Do the send outside of the synchronise
    
    // If we created a new completed (silenced) range then we actually send this to the
    // target ME. If we don't do this it's possible for the target to never realise that
    // this message has been explicitly deleted (via the MBeans), and therefore, if there's
    // a problem with the message it will never know to give up trying to process it, which
    // could leave the stream blocked until an ME re-start.
    if(startTick != -1)
    {
      downControl.sendSilenceMessage(startTick,
          endTick,
          completedPrefix,
          false,
          priority,
          reliability,
          stream);
    }
   
    if (sendList != null)
    {
      sendMsgs( sendList, false );
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "writeSilenceForced");

  }


  /**
    * This method is called when all the messages up to the ackPrefix
    * have been acknowledged. For pointTopoint this is called when an Ack
    * message is recieved from the target. For pubsub this is called when
    * all the InternalOutputStreams associated with this SourceStream have
    * received Ack messages.  
    *  
    * @param stamp The ackPrefix 
    * 
    * @exception GDException  
    * 
    * @return List of MessagesIds of messages to delete from the ItemStream
    */
  public List writeAckPrefix(long stamp)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(this, tc, "writeAckPrefix", Long.valueOf(stamp));

    // Holds TickRanges of all Value ticks between AckPrefix
    // and completedPrefix
    // This is returned to the caller
    List<TickRange> indexList = new ArrayList<TickRange>();
       
    synchronized (this)
    {
      // SIB0105
      // Update controllable health state if poss
      if (stamp >= lastAckExpTick)
      {
        getControlAdapter().getHealthState().updateHealth(HealthStateListener.ACK_EXPECTED_STATE, 
                                                          HealthState.GREEN);
        // Disable the checking until another ackExpected is sent
        lastAckExpTick = Long.MAX_VALUE;
      }
      if (stamp >= lastNackReceivedTick)
      {
        getControlAdapter().getHealthState().updateHealth(HealthStateListener.NACK_RECEIVED_STATE, 
                                        HealthState.GREEN);
        // Disable the checking until another ackExpected is sent
        lastNackReceivedTick = Long.MAX_VALUE;
      }
      
      inboundFlow = true;
      long completedPrefix = oststream.getCompletedPrefix();
      if (stamp > completedPrefix)
      {
        // Retrieve all Value ticks between completedPrefix and stamp
        oststream.setCursor(completedPrefix + 1);

        // Get the first TickRange
        TickRange tr = oststream.getNext();

        TickRange tr2 = null;
        while ((tr.startstamp <= stamp) && (tr != tr2))
        {
          if (tr.type == TickRange.Value)
          {
            indexList.add(tr);
            totalMessagesSent++;
            timeLastMsgSent = System.currentTimeMillis();
          }
          else if (tr.type != TickRange.Completed)
          {
            //See defect 278043:
            //The remote ME is trying to ACK a tick that we do not understand.
            //Perhaps we went down just after the message was sent and the
            //message was lost from our message store, so that it is now UNKNOWN.
            //We do not want to stop processing this ACK however, as that
            //would mean that there will always be a hole and we will eventually
            //hit the sendMsgWindow.
            //We should log the event in trace & FFDC and then continue
            //to process the ack.
            if(TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            {
              SibTr.debug(tc, 
                          "Invalid message found processing ack stamp " 
                          + stamp + ": completed prefix " + completedPrefix, 
                          tr);
            }
            
            //ffdc incase trace is not enabled, but we do not go on to
            //throw this exception
            InvalidMessageException invalidMessageException = 
              new InvalidMessageException(); 
            FFDCFilter.processException(
              invalidMessageException,
              "com.ibm.ws.sib.processor.gd.SourceStream.writeAckPrefix",
              "1:981:1.138",
              this,
              new Object[]{Long.valueOf(stamp), Long.valueOf(completedPrefix), tr});
              
            // We only want to force any unknown ranges to 'completed' up to
            // the ack'd tick, beyond that should stay 'unknown' so we crop the
            // current TickRange to be up to the ack'd tick.
            long endstamp = tr.endstamp;
            if(endstamp > stamp)
              endstamp = stamp;
              
            TickRange completedTr =
              new TickRange(
                TickRange.Completed,
                tr.startstamp,
                endstamp );              
              
            oststream.writeCompletedRangeForced(completedTr);
            
          }
          tr2 = tr;
          tr = oststream.getNext(); // get next TickRange
        } // end while
        
        // 513948
        // If we acked more messages than the sendWindow then we need to grow the sendwindow
        // to be bigger than the definedSendWindow. This will reduce back down to the correct value
        // when the msgs are removed. We also set the correct firstMsgOutsideWindow at this point.
        if (indexList.size() > sendWindow)
        {
          sendWindow = indexList.size(); // Dont need to persist - this will be done when msg removed
          // Find the firstMsgOutsideWindow if it exists
          while( tr.type == TickRange.Completed && tr.endstamp != RangeList.INFINITY)
            tr = oststream.getNext();
          
          firstMsgOutsideWindow = tr.valuestamp;

          if(TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(tc, "firstMsgOutsideWindow: " + firstMsgOutsideWindow + ", sendWindow: " + sendWindow);
        }

        oststream.setCompletedPrefix(stamp);

        // May have moved beyond stamp if there were adjacent
        // Completed ticks
        oack = oststream.getCompletedPrefix();
      }
    } // end sync

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "writeAckPrefix", Boolean.FALSE);

    return indexList;

  }
  
  

  /**
   * This method is called when a Nack message is received from the 
   * downstream ME corresponding to this InternalOutputStream.
   * It sends Value and Silence messages downstream for any ticks 
   * in these states in the stream, and for any ticks in Unknown or 
   * Requested state it sends a Nack upstream.
   * 
   * @exception GDException  Can be thrown from writeRange
   * 
   * @return null The corresponding method on the SourceStream returns
   *              the list of messages to be deleted from the ItemStream.
   *              The calling code relies on this method returning null to
   *              indiacte that no messages should be deleted.   
   */
  public void processNack(ControlNack nm) throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(this, tc, "processNack", nm);

    boolean sendPending = false;
    List sendList = new ArrayList();
    
    boolean sendLeadingSilence = false;
    long lsstart = 0;
    long lsend = 0;
    boolean sendTrailingSilence = false;
    long tsstart = 0;
    long tsend = 0;
      
    long startstamp = nm.getStartTick();
    long endstamp = nm.getEndTick();

    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
    {
      SibTr.debug(tc, "processNack from: " + startstamp + " to " + endstamp + " on Stream " +  stream);
    }

    long completedPrefix;

    // the TickRange to hold information discovered as we traverse stream
    TickRange r = null;

    // Go through oststream and see which ticks in the Nack range are
    // Completed or Value or Uncommitted and send appropriate messages
    // for these ticks.
    synchronized (this)
    {
      // SIB0105
      // Update the health state of this stream 
      getControlAdapter().getHealthState().updateHealth(HealthStateListener.NACK_RECEIVED_STATE, 
                                                        HealthState.AMBER);
      lastNackReceivedTick = endstamp;
      
      inboundFlow = true;
      completedPrefix = oststream.getCompletedPrefix();

      // If some of the ticks in the Nack range are before the completedPrefix
      // of our Stream we send a Silence message from the startstamp 
      // to the completedPrefix.
      if (startstamp <= completedPrefix)
      {
        sendLeadingSilence = true;
        lsstart = startstamp;
        lsend = completedPrefix;
        
        //Some of the ticks in the Nack range are before the completedPrefix
        //of our Stream so start from there
        startstamp = completedPrefix + 1;
      }
      
      // If there are any tick in the Nack range which are not yet
      // complete process these now
      if( endstamp > completedPrefix)
      {
        oststream.setCursor(startstamp);

        // Get the first TickRange
        TickRange tr = oststream.getNext();

        TickRange tr2 = null;
        while ((tr.startstamp <= endstamp) && (tr != tr2))
        {
          if ((tr.type == TickRange.Unknown)
            || (tr.type == TickRange.Requested))
          {
            break;
          }
          else if (tr.type == TickRange.Value)
          {
            // Do we have a previous Value message to add
            // to the list
            if (sendPending)
            {
              sendList.add(r);
            }

            // If message is inside sendWindow
            // copy the Value tick range into r 
            if (msgCanBeSent(tr.valuestamp, true))
            {
              r = (TickRange)tr.clone();
              sendPending = true;
            }
            else
            {
              // If this message is outside the sendWindow we should stop
              break;
            }
          }
          else if (tr.type == TickRange.Uncommitted)
          {
            // If there is a previous Value message in the list
            // we can put any Completed ticks between that and this
            // Uncommitted tick into it.
            if (sendPending)
            {
              // If there are Completed ticks between
              // the Value and Uncommitted ticks
              // Add them to the end of the Value message
              if (tr.valuestamp > (r.valuestamp + 1))
              {
                r.endstamp = tr.valuestamp - 1;
              }
              sendList.add(r);
              sendPending = false;
            }
          }
          tr2 = tr;
          tr = oststream.getNext();
        } // end while

        // If we finish on a Completed range then add this to the
        // last Value in our list
        // Check for null as we may have dropped out first time round loop
        // above without ever initialising tr2
        if ((tr2 != null) && (tr2.type == TickRange.Completed))
        {
          if (sendPending)
          {
            r.endstamp = tr2.endstamp;
          }
          else
          {
            // Need to send this Completed range in a Silence
            // message as there is no Value to add it to
            // This may be because the whole Nack range is Completed or
            // because the previous range was Uncommitted
            // or because the previous range was outside the sendWindow
            if (msgCanBeSent(tr2.startstamp, true))
            {
              sendTrailingSilence = true;
              tsstart = tr2.startstamp;
              tsend = tr2.endstamp;
            }
          }
        }
        if (sendPending)
        {
          sendList.add(r);
        }
      }
    } // end sync

    // Send any Silence at start of Nack range
    if (sendLeadingSilence)
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
      {
        SibTr.debug(tc, "processNack sending Silence from: " + lsstart + " to " + lsend + " on Stream " +  stream);
      }

      try
      {
        downControl.sendSilenceMessage(lsstart, lsend, completedPrefix, true,
                                       nm.getPriority().intValue(),
                                       nm.getReliability(), stream);
      }
      catch (SIResourceException e)
      {
        // FFDC
        FFDCFilter.processException(
          e,
          "com.ibm.ws.sib.processor.gd.SourceStream.processNack",
          "1:1222:1.138",
          this);

        SibTr.exception(tc, e);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
          SibTr.exit(tc, "processNack", e);

        throw e;
      }
    }


    // send and messages in Nack range
    if (sendList.size() != 0)
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
      {
        SibTr.debug(tc, "processNack resending Value messages on Stream " +  stream);
      }

      // second parameter indicates that this is a resend in response to a Nack 
      sendMsgs( sendList, true );
    }

    // send any Silence at end of Nack range
    if (sendTrailingSilence)
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
      {
        SibTr.debug(tc, "processNack sending Silence from: " + tsstart + " to " + tsend + " on Stream " +  stream);
      }

      try
      {
        downControl.sendSilenceMessage(tsstart, tsend, completedPrefix, true,
                                       nm.getPriority().intValue(), 
                                       nm.getReliability(), stream);
      }
      catch (SIResourceException e)
      {
        // FFDC
        FFDCFilter.processException(
          e,
          "com.ibm.ws.sib.processor.gd.SourceStream.processNack",
          "1:1267:1.138",
          this);

        SibTr.exception(tc, e);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
          SibTr.exit(tc, "processNack", e);

        throw e;
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "processNack");
  }

  public List restoreStream(int startMode) throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(this, tc, "restoreStream");

    List sentMsgs = null;
    synchronized (this)
    {
      // Run through stream looking for V's
      oststream.setCursor(0);

      // Get the first TickRange
      TickRange tr = oststream.getNext();

      long prevValue = 0;

      while (tr.endstamp != RangeList.INFINITY)
      {
        if (tr.type == TickRange.Value || tr.type == TickRange.Uncommitted)
        {
             
          // We need to ensure that there is a single range of
          // Completed ticks between the previous Value and
          // this one (if they're not already contiguous).
          if((prevValue + 1) < tr.valuestamp)
          {
            TickRange tr1 =
              new TickRange(
                TickRange.Completed,
                prevValue + 1,
                tr.valuestamp - 1);
            
            oststream.writeCompletedRange(tr1);
          }
     
          // Put this back in if we ever need Value range to
          // hold Completed range info
          // tr.startstamp = prevValue + 1;
          // tr.endstamp = tr.valuestamp -1;
       
          prevValue = tr.valuestamp;

          // Check whether adding this message takes us above 
          // the sendWindow and set the firstMsgOutsideWindow if so 
          //Defect 197614.  The msgAdded() method has already been called when the messages
          //were restored.  Calling it again here, means the totalMessages count is higher
          //than it should be, which can cause an infinite loop when adjusting the send 
          //window
   
          // Check if there is a possibility that adding this message 
          // will take us over the number of allowed sendWindow
          totalMessages++;
          if(TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(this, tc, "totalMessages: " + totalMessages + ", sendWindow: " + sendWindow);
          
          if (totalMessages > sendWindow)
          {
             // Is this the first time we have overflowed
             // the limit for inDoubt messages  
             if (firstMsgOutsideWindow == RangeList.INFINITY)
             {
               firstMsgOutsideWindow = tr.valuestamp;
               if(TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                 SibTr.debug(this, tc, "firstMsgOutsideWindow: " + firstMsgOutsideWindow); 
             }
          }
          else if (tr.type == TickRange.Value )
          {
            // Keep track of the last message we sent
            lastMsgSent = tr.valuestamp;
            
            // Add this message to list of messages which should be locked by caller
            if ( sentMsgs == null )
                 sentMsgs = new ArrayList();
        
            sentMsgs.add(Long.valueOf(tr.itemStreamIndex));   
          }

          // Need to get back to where we were before we setCompletedRange
          oststream.setCursor(tr.valuestamp);
          tr = oststream.getNext();         
        }
        tr = oststream.getNext();
      } // end while

      // Initialise the lastMsgAdded
      lastMsgAdded = prevValue;
      
      // If the restore of the stream marked us as containing guesses (only if
      // it's a link) then we crop the sendWindow down to the current number of
      // messages on the stream (if less than the sendWindow) and remove the guesses
      // flag. This will prevent any new messages being sent until noGuesesInStream()
      // is called by SourceStreamManager.updateTargetCellule(). Which is called when
      // TRM tells us where the other end of the link is (once it's started).
      if(containsGuesses)
      {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
          SibTr.debug(this, tc, "Stream is in guess state with " + totalMessages + " messages (" + sendWindow + ")");
        
        containsGuesses = false;
        if(totalMessages < sendWindow)
        {
          sendWindow = totalMessages;
          persistSendWindow(sendWindow, null);
        }
      }
      
      // Do not start the AckExpected alarm if we are in recovery
      // mode as we do not want to send any messages in this case
      if( (startMode & JsConstants.ME_START_RECOVERY) == 0 )
      {
        // Start the AckExpected timer for all messages already sent
        if (!ackExpAlarmStarted)
        {
          ackExpAlarmStarted = true;
          timerStartSendTick = lastMsgSent;
          ackExpInterval = GDConfig.GD_ACK_EXPECTED_THRESHOLD;
          am.create(ackExpInterval, ackExpExpiryHandler);
        }
      }
    } // end sync

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "restoreStream");
      
    return sentMsgs;  
  }

  /**
   * This method uses a Value message to write an Uncommitted tick
   * into the stream.
   * It is called at retore time.
   *
   * @param m The value message to write to the stream
   *
   */
  public void restoreUncommitted(SIMPMessage m)
  {

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(this, tc, "restoreUncommitted", new Object[] { m });

    TickRange tr = null;

    JsMessage jsMsg = m.getMessage();
   
    long stamp = jsMsg.getGuaranteedValueValueTick();
    long starts = jsMsg.getGuaranteedValueStartTick();
    long ends = jsMsg.getGuaranteedValueEndTick();

    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
    {
      SibTr.debug(tc, "restoreUncommitted at: " + stamp + " on Stream " +  stream);
    }

    synchronized (this)
    {
      // write into stream
      tr = TickRange.newUncommittedTick(stamp);
      tr.startstamp = starts;
      tr.endstamp = ends;
    
      // Save message in the stream while it is Uncommitted
      // It will be replaced by its index in the ItemStream once it becomes a Value
      tr.value = m;

      oststream.writeCombinedRange(tr);
 
    } // end synchronized

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "restoreUncommitted");
  }

  /**
    * This method uses a Value message to write a Value tick
    * into the stream.
    * It is called at retore time.
    *
    * @param m The value message to write to the stream
    *
    */
  public void restoreValue(SIMPMessage m) throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(this, tc, "restoreValue", m);

    TickRange tr = null;
      
    long msgStoreId = AbstractItem.NO_ID;
    try
    {
      if (m.isInStore())
        msgStoreId = m.getID();
    }
    catch(MessageStoreException e)
    {
      // FFDC
      FFDCFilter.processException(
          e,
          "com.ibm.ws.sib.processor.gd.SourceStream.restoreValue",
          "1:1484:1.138",
          this);  
                  
      SibTr.exception(tc, e); 
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "restoreValue", e);
      
      throw new SIResourceException(e);
    }
    
      
    JsMessage jsMsg = m.getMessage();

    long stamp = jsMsg.getGuaranteedValueValueTick();
    long starts = jsMsg.getGuaranteedValueStartTick();
    long ends = jsMsg.getGuaranteedValueEndTick();

    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
    {
      SibTr.debug(tc, "restoreValue at: " + stamp + " with Silence from: " + starts + " to " + ends + " on Stream " +  stream);
    }

    synchronized (this)
    {
      // Now update Value tick to indicate it is now committed
      tr = TickRange.newValueTick(stamp, null, msgStoreId);
      tr.startstamp = starts;
      tr.endstamp = ends;

      oststream.writeCombinedRange(tr);
    } 

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "restoreValue");
 
  }

  public void releaseMemory()
  {

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(this, tc, "releaseMemory");

    synchronized (this)
    {
      // Create a new stream to replace old one
      // as set latest ls prefix
      oststream = new StateStream();
      oststream.init();
      oststream.setCompletedPrefix(oack);

    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "releaseMemory");
  }

  public String toString()
  {
    String ret =
      "SourceStream (" + this.hashCode() + "): Stream ID "
        + stream
        + " Priority "
        + priority
        + " Reliability "
        + reliability;

    return ret;
  }

  /**
   * Check if this stream is flushable.  A stream is flushable if
   * there are no V/U or L/U ticks.
   *
   * @return true if there are currently no V/U or L/U ticks, and
   * false otherwise.
   */
  public synchronized boolean flushable()
  {
    TickRange range = new TickRange(TickRange.Unknown, 0, RangeList.INFINITY);

    return (
      (!oststream.containsState(range, TickRange.Value))
        && (!oststream.containsState(range, TickRange.Uncommitted))
        && (!oststream.containsState(range, TickRange.Error)));
  }
  
  /**
   * Class that implements blocked stream health checker
   */
  public class BlockedStreamAlarm implements AlarmListener
  {
    private long previousCompletedPrefix;
    private String blockingTran = null;
    private int blockedCount = 0;

    public BlockedStreamAlarm(long completedPrefix)
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(this, tc, "BlockedStreamAlarm", Long.valueOf(completedPrefix));

      previousCompletedPrefix = completedPrefix;

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "BlockedStreamAlarm", this);
    }

    public void checkState(boolean create) {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(this, tc, "checkState", new Object[] {Boolean.valueOf(create),
                                                          Long.valueOf(previousCompletedPrefix),
                                                          Integer.valueOf(blockedCount)});

      boolean allClear = false;
      
      synchronized(SourceStream.this)
      {
        if ( totalMessages > 0)
        {
          // Check the completed prefix is different from the previous check
          long completedPrefix = getCompletedPrefix();
          oststream.setCursor(completedPrefix + 1);
          
          TickRange tr = oststream.getNext(); 
          
          if (tr.type == TickRange.Uncommitted &&
              completedPrefix == previousCompletedPrefix )
          {
            // If we called this as the result of a value being committed, we're only running this
            // check to see if we've FIXED a problem, not caused one. Therefore we don't report a
            // suspected problem. Otherwise, if we send two messages, the first is prepared, the
            // other commits, as soon as that commit happens we'd mark the stream blocked - without
            // waiting for the BlockedStreamAlarm to pop - so we'd flip-flop in and out of the blocked
            // state constantly under normal running conditions.
            if(create)
            {
              // Update the health to report that there has been no change at the front of the stream
              // This means that the msg at the head of the stream has not been committed.
              // Update the health state of this stream 
              getControlAdapter().getHealthState().updateHealth(HealthStateListener.BLOCKED_STREAM_STATE, 
                                                                HealthState.AMBER);

              blockedCount++;
              
              // Calculate how often we should warn (at most, once every 30 minutes)
              int countInterval = 1;
              int blockedStreamCheckInterval = mp.getCustomProperties().getBlockedStreamHealthCheckInterval();
              if((blockedStreamCheckInterval < SIMPConstants.BLOCKED_STREAM_REPEAT_INTERVAL) &&
                 (blockedStreamCheckInterval > 0))
                countInterval = SIMPConstants.BLOCKED_STREAM_REPEAT_INTERVAL / blockedStreamCheckInterval;
              
              // If this is the n-th time round (or we haven't warned for 30 minutes) we should
              // issue a warning message. Where 'n' is the number of times the alarm must pop before
              // we've waited for the minimum report interval (the alarm pops on a potentially shorter
              // 'check' timer to support the HealthState logic).
              if((blockedCount % countInterval) == mp.getCustomProperties().getBlockedStreamRatio())
              {
                // The stream is still pointing to the same uncommitted message as before, we need
                // to issue a warning

                String name = dh.getName();
                if(dh.isLink())
                  name = ((LinkHandler)dh).getBusName();
                
                // Remember the transaction that is blocking the stream (so we can reference it when its
                // finally resolved)
                SIMPMessage msg = (SIMPMessage)tr.value;
                
                // We shouldn't ever have a value without an item, or a committing item without a
                // tran, but just in case we do we still issue the message (plus an FFDC for our
                // own benefit).
                if((msg != null) &&
                   (msg.getTransactionId() != null))
                  blockingTran = msg.getTransactionId().toString();
                else
                {
                  blockingTran = "NULL";
                  
                  if(TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                  {
                    SibTr.debug(tc, "No Item or transaction associated with this committing value ",
                                new Object[] {tr, msg, dh});
                  }

                  InvalidMessageException invalidMessageException = 
                    new InvalidMessageException(); 
                  FFDCFilter.processException(
                    invalidMessageException,
                    "com.ibm.ws.sib.processor.gd.SourceStream.BlockedStreamAlarm.checkState",
                    "1:1674:1.138",
                    this,
                    new Object[]{tr, msg, dh});
                }
                
                SibTr.info(tc, "STREAM_BLOCKED_BY_COMMITTING_MESSAGE_CWSIP0785",
                    new Object[] {name,
                                  dh.getMessageProcessor().getMessagingEngineName(),
                                  new Long((blockedCount * mp.getCustomProperties().getBlockedStreamHealthCheckInterval()) / 1000),
                                  blockingTran});
              }
            }
          }
          else
          {
            blockedCount = 0;
            previousCompletedPrefix = completedPrefix;
            // Update health to green
            getControlAdapter().getHealthState().updateHealth(HealthStateListener.BLOCKED_STREAM_STATE, 
                                                              HealthState.GREEN);  
            
            // Issue an 'all clear' info message if necessary
            if(blockingTran != null)
                allClear = true;
          }             
          
          // Reset the timer
          if (create)
            am.create(mp.getCustomProperties().getBlockedStreamHealthCheckInterval(), this);
        }
        else 
        {
          // Cancel the alarm - no msgs on stream
          if (create) blockedStreamAlarm = null;
          
          // Update health to green
          getControlAdapter()
          .getHealthState().updateHealth(HealthStateListener.BLOCKED_STREAM_STATE, 
                                         HealthState.GREEN); 
        }           

        // If we've previously reported a blockage, we now sound the all clear
        if(allClear)
        {
          String name  = dh.getName();
          if(dh.isLink())
            name = ((LinkHandler)dh).getBusName();
            
          SibTr.info(tc, "STREAM_UNBLOCKED_BY_COMMITTING_MESSAGE_CWSIP0786",
              new Object[] {name,
                            dh.getMessageProcessor().getMessagingEngineName(),
                            blockingTran});
          
          blockingTran = null;
        }
      }


      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "checkState");

    }

    public void alarm(Object thandle)
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(this, tc, "alarm", this);
      
      checkState(true);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "alarm");

    } // End of method timerExpired

  }

  /**
   * Class that implements AckExpected timeout
   */
  public class AckExpExpiryHandler implements AlarmListener
  {
    private SourceStream sourceStream;

    public AckExpExpiryHandler(SourceStream sourceStream)
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(this, tc, "AckExpExpiryHandler", sourceStream);

      this.sourceStream = sourceStream;

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "AckExpExpiryHandler", this);
    }

    public void alarm(Object thandle)
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(this, tc, "alarm", this);

      try
      {
        // We don't want to hold the sourceStream lock while we send an AckExpected message so
        // we need to get a local copy of variables under the lock. 
        long completedPrefix;
        long localTimerStartSendTick;
        synchronized (sourceStream)
        {
          // Get the completedPrefix under the lock and release if prior
          // to sending the ackExpected message
          completedPrefix = getCompletedPrefix();
          localTimerStartSendTick = timerStartSendTick;
        }
            
        // Send Ack_Expected with tick of last message sent
        // when we started this timer
        // Don't send it if we've received Acks beyond it
        if ( localTimerStartSendTick > completedPrefix )
        {
          downControl.sendAckExpectedMessage(
            localTimerStartSendTick,
            priority,
            reliability,
            stream);
        }

        synchronized (sourceStream)
        {
          // Now we've taken the lock again, re-take the completedPrefix
          completedPrefix = getCompletedPrefix();
              
          if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
              SibTr.debug(tc, "lastMsgSent:" + lastMsgSent +
                              " completedPrefix:" + completedPrefix +
                              " inboundFLow:" + inboundFlow);
              
          // Reset the AckExp timer if necessary
          if( lastMsgSent > completedPrefix )
          {
            // Increase the repetition interval each time we restart
            // this timer and we havent heard anything from the other
            // side
            if(!inboundFlow)
            {
              ackExpInterval = (ackExpInterval * 2) > GDConfig.GD_ACK_EXPECTED_THRESHOLD_MAX ? GDConfig.GD_ACK_EXPECTED_THRESHOLD_MAX: ackExpInterval * 2;  
            }
            else
            {
              // Reset it as we are waiting for a new message
              ackExpInterval = GDConfig.GD_ACK_EXPECTED_THRESHOLD;    
            }
            // note that ackExpectedAlarmStarted is already true
            timerStartSendTick = lastMsgSent;  
           
            am.create(ackExpInterval, ackExpExpiryHandler);
          }
          else
          {
            ackExpAlarmStarted = false;
            ackExpInterval = GDConfig.GD_ACK_EXPECTED_THRESHOLD;
          }  
          inboundFlow = false;
        }
      }
      catch (Exception e)
      {
        // FFDC
        FFDCFilter.processException(
          e,
          "com.ibm.ws.sib.processor.gd.SourceStream.AckExpExpiryHandler.alarm",
          "1:1844:1.138",
          this);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
          SibTr.exception(tc, e);
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "alarm");

    } // End of method timerExpired

  }

  // All functions after this point deal with managing the 
  // stream send window
  // Called by reallocate code to stop any further messages 
  // being sent from this stream
  // Also called by writeUncomitted when a guess is written into the stream
  public synchronized void newGuessInStream(long tick) throws SIResourceException
  {

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(this, tc, "newGuessInStream", new Object[] { Long.valueOf(tick),
          Boolean.valueOf(containsGuesses),
          Long.valueOf(sendWindow),
          Long.valueOf(totalMessages),
          Long.valueOf(firstMsgOutsideWindow)});
    
    // If this is the first guess in the stream 
    if( !containsGuesses)
    { 
      containsGuesses=true;  
      if( sendWindow > totalMessages )
      {
        sendWindow = totalMessages;
        persistSendWindow(sendWindow, null);  
        firstMsgOutsideWindow = tick; 
      }  
    }
         
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "newGuessInStream", new Object[] {Boolean.valueOf(containsGuesses),
          Long.valueOf(sendWindow),
          Long.valueOf(firstMsgOutsideWindow)});
  }
  
  // Called by the reallocate code to prevent the stream sending 
  // any messages while some are being reallocated
  public synchronized void guessesInStream()  
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(this, tc, "guessesInStream", Boolean.valueOf(containsGuesses));

    containsGuesses = true;
  
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "guessesInStream", Boolean.valueOf(containsGuesses));
  }
             
  // Called when the reallocate code has cleaned up this stream
  // so that it no longer contains any messages which may not be
  // ours to send
  public synchronized void noGuessesInStream() throws SIResourceException 
  {

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
       SibTr.entry(this, tc, "noGuessesInStream");

    if(TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        SibTr.debug(tc, "oldContainesGuesses:" + containsGuesses +
                        "firstMsgOutsideWindow: " + firstMsgOutsideWindow +
                        ", oldSendWindow: " + sendWindow);
    
    containsGuesses = false;
     
    // If we have reduced our sendWindow below the definedSendWindow
    // we now need to send all the messages between the two 
    if( definedSendWindow > sendWindow ) 
    {
      long oldSendWindow = sendWindow;
      sendWindow = definedSendWindow;  
      persistSendWindow(sendWindow, null); 
      // Send the messages up to the new send window
      // this will update firstMsgOutsideWindow
      sendMsgsInWindow(oldSendWindow, sendWindow);
    } 
    else
    {
      if(TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
      {
        SibTr.debug(tc, 
                    "no processing : definedSendWindow=" + definedSendWindow + 
                    ", sendWindow=" +sendWindow);
      }
    }
         
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
       SibTr.exit(tc, "noGuessesInStream");
  }
  
  // This is called when the sendWindow is being set from its persisted value
  // when the stream is being created. In this case we know we can start using 
  // this value straight away and also that we don't need to persist it
  public synchronized void initialiseSendWindow(long sendWindow, long definedSendWindow)
    throws SIResourceException 
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(this, tc, "initialiseSendWindow", new Object[] {Long.valueOf(sendWindow), Long.valueOf(definedSendWindow)} );
     
    
    // Prior to V7 the sendWindow was not persisted unless it was modified due to a change in
    // reachability, otherwise it was stored as INFINITY (Long.MAX_VALUE). Therefore, if the
    // defined sendWindow was modified we could be in the situation where before a restart we
    // had 1000 messages indoubt, then shut the ME down, changes the defined sendWindow to 50
    // and restarted. At that point we'd set the sendWindow to 50 and suddenly we'd have 950
    // available messages again (not indoubt). We'd then be allowed to reallocate them elsewhere!
    
    // Luckily, the ability to modify the sendWindow wasn't correctly exposed prior to V7 (it's
    // now a custom property), so no-one could have modified it. So, we can interpret a value
    // of INFINITY as the original sendWindow value, which is 1000. And use that when we see it.
    if( sendWindow == RangeList.INFINITY )
    {
      this.definedSendWindow = definedSendWindow; 
      this.sendWindow = 1000; // Original default sendWindow
      
      // Now persist the 1000 to make it clearer
      persistSendWindow(this.sendWindow, null);
    }
    else
    {
      this.sendWindow = sendWindow;
      this.definedSendWindow = definedSendWindow;   
    }  
      
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "initialiseSendWindow");
  }

  // This is used to set the sendWindow defined in Admin panels 
  public synchronized void setDefinedSendWindow(long newSendWindow)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(this, tc, "setDefinedSendWindow", Long.valueOf(newSendWindow) );

    definedSendWindow = newSendWindow;

    // PK41355 - Commenting out - should not be sending stuff at reconstitute
    // updateAndPersistSendWindow();
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    
    SibTr.exit(tc, "setDefinedSendWindow",Long.valueOf(newSendWindow));
  }
  
  // ONLY called from tests
  public synchronized void updateAndPersistSendWindow() throws SIResourceException
  {

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(this, tc, "updateAndPersistSendWindow" );

    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
    {
      SibTr.debug(tc, "definedSendWindow is: " + definedSendWindow 
          + " sendWindow is " +  sendWindow + " containsGuesses is " + containsGuesses
          + " totalMessages is " + totalMessages);
    }

    // If our current sendWindow is less than this new value
    // we can move to the new value straight away and send
    // any messages which were between the two
    if( definedSendWindow > sendWindow ) 
    {
      // If the stream containsGuesses we can't send
      // anything at the moment so we do the code
      // below in noGuessesInStream() instead
      if (!containsGuesses)
      {
        long oldSendWindow = sendWindow;
        sendWindow = definedSendWindow;  
        persistSendWindow(sendWindow, null); 
        // Send the messages up to the new send window
        // this will update firstMsgOutsideWindow
        sendMsgsInWindow(oldSendWindow, sendWindow);
      }
    } 
    else
    {
       // sendWindow is being reduced
       if ( definedSendWindow > totalMessages )
       {
         // sendWindow is being reduced but is bigger than 
         // totalMessages so we can just set it
         sendWindow = definedSendWindow;  
         persistSendWindow(sendWindow, null);    
       }
       else if ( totalMessages < sendWindow)
       {
         sendWindow = totalMessages;
         persistSendWindow(sendWindow, null); 
       }
    }
      
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "updateAndPersistSendWindow");
  }
  
  public synchronized void msgAdded(long stamp)
  {

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(this, tc, "msgAdded", new Object[] { Long.valueOf(stamp),
    	  Long.valueOf(totalMessages),
          Long.valueOf(sendWindow),
          Long.valueOf(firstMsgOutsideWindow)});

    // Check if there is a possibility that adding this message 
    // will take us over the number of allowed sendWindow
    totalMessages++;
    if (totalMessages > sendWindow)
    {
       // Is this the first time we have overflowed
       // the limit for inDoubt messages  
       if (firstMsgOutsideWindow == RangeList.INFINITY)
         firstMsgOutsideWindow = stamp;
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "msgAdded", Long.valueOf(firstMsgOutsideWindow));
  }

  /**
   * Method that determines if the message can be sent.
   * 
   * If the message is a NACK message then we should not check the stream for guesses,
   * but we should still follow the other checks for the stream.
   * 
   * @param stamp
   * @return
   */
  private synchronized boolean msgCanBeSent( long stamp, boolean nackMsg )
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(this, tc, "msgCanBeSent", new Object[] { Long.valueOf(stamp),
          Boolean.valueOf(nackMsg),
          Long.valueOf(firstMsgOutsideWindow),
          Boolean.valueOf(containsGuesses)});

    boolean sendMessage = true;
    
    // Check whether we can send the message
    // Don't send any messages once there is a guess in the stream (unless a NACK)
    if ((stamp >= firstMsgOutsideWindow) || (containsGuesses && !nackMsg))
    {
      sendMessage = false;
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
      {
        SibTr.debug(tc, "firstMsgOutsideWindow is: " + firstMsgOutsideWindow
        + " sendWindow is " +  sendWindow + " containsGuesses is " + containsGuesses);
      }
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "msgCanBeSent", Boolean.valueOf(sendMessage));

    return sendMessage;
  }

 
  
  /**
   * This method is called when the totalMessages on the stream
   * falls and we have the possibility of sending a message
   * because it is now inside the sendWindow
   */
  private synchronized TickRange msgRemoved(long tick, StateStream ststream, TransactionCommon tran) throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
       SibTr.entry(this, tc, "msgRemoved", 
                   new Object[]{Long.valueOf(tick), 
                               ststream, 
                               tran});
       
    
    TickRange tr1 = null;
    boolean sendMessage = false;

    long stamp = tick;
    
    // We know we have reduced the number of messages in the stream
    totalMessages--;
        
    if(TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
      SibTr.debug(this, tc, "totalMessages: " + totalMessages +
                            ", sendWindow: " + sendWindow +
                            ", definedSendWindow: " + definedSendWindow +
                            ", firstMsgOutsideSendWindow: " + firstMsgOutsideWindow);
    
    // We may not send any messages if our stream contains guesses
    // or if we are supposed to be reducing the sendWindow
    if( ( (containsGuesses) || (sendWindow > definedSendWindow ) )
      && ( stamp < firstMsgOutsideWindow ) )
    {
      // Just shrink the sendWindow
      if( sendWindow > 0)
      { 
        sendWindow--;
        persistSendWindow(sendWindow, tran); 
      }
    }
    else
    {
      // If the message we have just removed from the stream was
      // inside the sendWindow then we can move up our 
      // firstMsgOutsideWindow and possibly send the message which
      // moves inside the window
      // If it actually was the first message outside the window
      // the code below will also work
      // If it was beyond the window we can afford to ignore it 
      if (stamp <= firstMsgOutsideWindow)
      {
        // send firstMsgOutsideWindow
        // first need to get it and check that it is in Value state
        ststream.setCursor(firstMsgOutsideWindow);
 
        // Get the first TickRange outside the inDoubt window
        tr1 = ststream.getNext();
          
        // This range must be either Uncommitted or Value as that is how
        // we set the firstMsgOutsideWindow pointer    
        // Only want to send this message if it is 
        // committed. Otherwise do nothing as it will
        // get sent when it commits
        if (tr1.type == TickRange.Value)
        {
          sendMessage = true;
        }
     
        TickRange tr = null;
        if (totalMessages > sendWindow)
        {
          // Get the next Value or Uncommitted tick from the stream
          tr = ststream.getNext();
          while( tr.type == TickRange.Completed && tr.endstamp != RangeList.INFINITY)
          {
            tr = ststream.getNext();
          }
          firstMsgOutsideWindow = tr.valuestamp;
        }
        // That was the last message outside the send window, so put us back into a state
        // ignorant of guesses, otherwise we may not realise to re-calculate the firstMsgOutSideWindow
        // the next time we get a guess.
        else
        {
          firstMsgOutsideWindow = RangeList.INFINITY;
          containsGuesses = false;
        }
        
        if(TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
          SibTr.debug(this, tc, "firstMsgOutsideSendWindow: " + firstMsgOutsideWindow);
      }
    }

    // Return a null if nothing to send
    if (!sendMessage)
      tr1 = null;

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "msgRemoved", tr1);

    return tr1;
  }
  
  
  private synchronized void sendMsgsInWindow(long oldSendWindow, long newSendWindow)
    throws SIResourceException
  {
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
           SibTr.entry(this, tc, "sendMsgsInWindow",  new Object[]{Long.valueOf(oldSendWindow), Long.valueOf(newSendWindow)});
           
    // Return the first message after the newSendWindow 
    long newFirstMsgOutsideWindow = RangeList.INFINITY;
    long messagesToSend=0;
           
    // Try to deliver messages between oldSendWindow and newSendWindow
    if(TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
      SibTr.debug(this, tc, "totalMessages: " + totalMessages +
                            ", sendWindow: " + sendWindow +
                            ", definedSendWindow: " + definedSendWindow +
                            ", firstMsgOutsideSendWindow: " + firstMsgOutsideWindow);
        
    // Check that we have messages outside the oldSendWindow  
    if( totalMessages > oldSendWindow )
    {
      // We need to send all the messages between the old and newSendWindows
      if( totalMessages > newSendWindow)
        messagesToSend = newSendWindow - oldSendWindow;
      else
        messagesToSend = totalMessages - oldSendWindow;  
        
      //  Get the TickRange containing the firstMsgOutside the old sendWindow
      oststream.setCursor(firstMsgOutsideWindow);
      TickRange tr = oststream.getNext();
      
      List<TickRange> sendList = new ArrayList<TickRange>();
     
      // Retrieve the required messages from the Stream 
      // There is a window where totalMessages is larger than the
      // actual number of Value TickRanges on the state stream as
      // the totalMessages counter is updated at postCommit time
      // (when the messages are removed from the ItemStream) but
      // 'firstMsgOutsideWindow' is updated at preCommit time (when
      // an uncommitted message is added). So there can be less
      // messages found in the stream than are currently accounted
      // for on the ItemStream. For this reason we simply use the
      // messagesToSend as a maximum number of messages to send
      // rather than an expected number.
      while( sendList.size() < messagesToSend &&
             tr.endstamp != RangeList.INFINITY) // PK57432
      {
        if (tr.type == TickRange.Value)
        {
          sendList.add(tr);
          lastMsgSent = tr.valuestamp;
        }
        else if(tr.type == TickRange.Uncommitted)
        {
          //defect 267283: It is possible that there are uncommited messages
          //in the window at this point. If so, we might never reach 'messagesToSend'.
          //messagesToSend has assumed that the src stream contains contiguous
          //committed messages. 
          if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
          {
            SibTr.debug(tc, "uncommited tick range: " + tr +
                             " : reducing messagesToSend from " + messagesToSend +
                             " to " + (messagesToSend-1));
          }
          messagesToSend--;
        }   
        tr = oststream.getNext();
      }
        
      // Now find the firstMsgOutside the newSendWindow if there is one  
      if( totalMessages > newSendWindow)
      { 
        while( tr.type == TickRange.Completed && tr.endstamp != RangeList.INFINITY)
        {
          tr = oststream.getNext();
        }
        newFirstMsgOutsideWindow = tr.valuestamp;
      }
      
      // send list
      if (sendList.size() != 0)
      {
        sendMsgs(sendList, false);
      } 
    }

    firstMsgOutsideWindow = newFirstMsgOutsideWindow;  

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "sendMsgsInWindow", Long.valueOf(firstMsgOutsideWindow));
  
  }

  void persistSendWindow(long tick, TransactionCommon tran) throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(this, tc, "persistSendWindow", new Object[]{Long.valueOf(tick), tran});
    streamSet.setPersistentData(priority, reliability, tick);
    streamSet.requestUpdate(reliability, tran);
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "persistSendWindow");
  }

  public synchronized List getMessagesAfterSendWindow()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(this, tc, "getMessagesAfterSendWindow", Long.valueOf(firstMsgOutsideWindow));
      
    List msgs = new ArrayList();

    // Check whether there are any messages outside the window
    if( firstMsgOutsideWindow != RangeList.INFINITY)
    {
      oststream.setCursor(firstMsgOutsideWindow);

      // Get the first TickRange
      TickRange tr = oststream.getNext();

      while (tr.endstamp < RangeList.INFINITY)
      {
        if (tr.type == TickRange.Value)
        {
          msgs.add(Long.valueOf(tr.itemStreamIndex));
        }
        if (tr.type == TickRange.Uncommitted)
        {
          // Reallocator needs to be run again when this commits
          tr.reallocateOnCommit();
        }
        tr = oststream.getNext();
      } // end while
    
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getMessagesAfterSendWindow", msgs);

    return msgs;
  }
  
  /**
   * Get an unmodifiable list of all of the messages in the VALUE
   * state on this stream
   */
  public synchronized List getAllMessagesOnStream()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(this, tc, "getAllMessagesOnStream");
      
    List<Long> msgs = new LinkedList<Long>();
    oststream.setCursor(0);
    // Get the first TickRange
    TickRange tr = oststream.getNext();
    while (tr.endstamp < RangeList.INFINITY)
    {
      if (tr.type == TickRange.Value)
      {
        msgs.add(Long.valueOf(tr.itemStreamIndex));
      }
      if (tr.type == TickRange.Uncommitted)
      {
        // Reallocator needs to be run again when this commits
        tr.reallocateOnCommit();
      }
      tr = oststream.getNext();
    } // end while
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getAllMessagesOnStream");

    return Collections.unmodifiableList(msgs);
  }
  
  /**
   * Get an unmodifiable list of all of the message items in the VALUE
   * state on this stream and, optionally, in the Uncommitted state
   */
  public synchronized List<TickRange> getAllMessageItemsOnStream(boolean includeUncommitted)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(this, tc, "getAllMessageItemsOnStream", Boolean.valueOf(includeUncommitted));
   
    List<TickRange> msgs = new LinkedList<TickRange>();
    oststream.setCursor(0);
    // Get the first TickRange
    TickRange tr = oststream.getNext();
    while (tr.endstamp < RangeList.INFINITY)
    {
      if (tr.type == TickRange.Value)
      {
        //get this msg from the downstream control
        msgs.add((TickRange)tr.clone());
      }
      else if (tr.type == TickRange.Uncommitted && includeUncommitted)
      {
        //get this msg directly
        if(tr.value!=null)
          msgs.add((TickRange)tr.clone());
      }
      tr = oststream.getNext();
    } // end while
       
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getAllMessageItemsOnStream", msgs);
    return Collections.unmodifiableList(msgs);
  }
  
  public synchronized long countAllMessagesOnStream()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(this, tc, "countAllMessagesOnStream");
      
    long count=0;
    oststream.setCursor(0);
    // Get the first TickRange
    TickRange tr = oststream.getNext();
    while (tr.endstamp < RangeList.INFINITY)
    {
      if (tr.type == TickRange.Value)
      {
        count++;
      }
      tr = oststream.getNext();
    } // end while
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "countAllMessagesOnStream", Long.valueOf(count));
    return count;
  }

  
  /**
   * Get a tick range given a tick value
   * @param tick
   * @return TickRange
   */
  public synchronized TickRange getTickRange(long tick)
  {
    oststream.setCursor(tick);

    // Get the TickRange
    return (TickRange) oststream.getNext().clone();
  }

  private void sendMsgs( List sendList, boolean requestedOnly ) throws SIResourceException
  {    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(this, tc, "sendMsgs", sendList);
    
    // Before we come in here lasMsgSent will have been updated
    // to the tick of the highest message in the list
    if (!ackExpAlarmStarted)
    {
      ackExpAlarmStarted = true;
      timerStartSendTick = lastMsgSent;
      ackExpInterval = GDConfig.GD_ACK_EXPECTED_THRESHOLD;
      am.create(ackExpInterval, ackExpExpiryHandler);
    }
   
    // This will return a list of expired Messages 
    // If there are any we have to replace them with Silence in the stream
    List expiredMsgs = null;
    try
    {
      expiredMsgs = downControl.sendValueMessages(sendList, 
                                                  getCompletedPrefix(), 
                                                  requestedOnly,
                                                  priority,
                                                  reliability,
                                                  streamSet.getStreamID());
    }
    catch (SIResourceException e)
    {
      // FFDC
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.gd.SourceStream.sendMsgs",
        "1:2495:1.138",
        this);

      SibTr.exception(tc, e);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "sendMsgs", e);

      throw e;
    }
  
 
    // If we were told to remove some messages from the stream do it now
    if ( expiredMsgs != null )
    {
      TickRange vtr = null;
      for (int i = 0; i < expiredMsgs.size(); i++)
      {
        vtr = (TickRange)expiredMsgs.get(i);
        writeSilenceForced(vtr);
      }    
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "sendMsgs");
  }
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.BatchListener#batchPrecommit(com.ibm.ws.sib.msgstore.transactions.Transaction)
   */
  public void batchPrecommit(TransactionCommon currentTran)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(this, tc, "batchPrecommit", currentTran);
 
    // Holds TickRanges of all messages to be sent downstream
    TickRange tr1 = null;
    TickRange tickRange = null;
    
    synchronized (this)
    {
      // 316010 : It is possible for the sourcestream to have
      // been removed when the batch timer triggers (if destination
      // is deleted). So check the streamset is still in the store.
      if ( !streamSet.isPersistent() || streamSet.isInStore() )
      {
        for (int i = 0; i < batchList.size(); i++)
        {
          tickRange = (TickRange)batchList.get(i);
          try
          {
            if ((tr1 = msgRemoved(tickRange.valuestamp, oststream, currentTran)) != null)
            {
              batchSendList.add(tr1);
              lastMsgSent = tr1.valuestamp;
            }
          }
          catch (SIResourceException e)
          {
            //FFDC
            FFDCFilter.processException(
              e,
              "com.ibm.ws.sib.processor.gd.SourceStream.batchPrecommit",
              "1:2558:1.138",
              this);
  
            SibTr.exception(tc, e);
          }
        } 
      }  
    }  
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "batchPrecommit");
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.BatchListener#batchCommitted()
   */
  public void batchCommitted()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(this, tc, "batchCommitted");
      
    if (batchSendList.size() != 0)
    {
      try
      {
        sendMsgs(batchSendList, false);
      }
      catch (SIResourceException e)
      {
        //FFDC
        FFDCFilter.processException(
          e,
          "com.ibm.ws.sib.processor.gd.SourceStream.batchCommitted",
          "1:2590:1.138",
          this);

        SibTr.exception(tc, e);
      }
    }
    
    // Empty list ready for use by next batch
    batchList.clear();
    batchSendList.clear();
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "batchCommitted");
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.BatchListener#batchRolledBack()
   */
  public void batchRolledBack()
  {
  }

  public void addToBatchList( TickRange tr )
  {
    batchList.add(tr); 
  }
  
  public SourceStreamControl getControlAdapter()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(this, tc, "getControlAdapter");
    if (srcStreamControl==null)
      srcStreamControl = new SourceStreamControl(this, streamSet, downControl);   
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "getControlAdapter",srcStreamControl);
    return srcStreamControl;   
  }
  
  public synchronized boolean isOutsideSendWindow(long guaranteedValueValueTick) {

    if(TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(this, tc, "isOutsideSendWindow", new Object[] {Long.valueOf(guaranteedValueValueTick),
          Long.valueOf(firstMsgOutsideWindow)});
    
    boolean outside = true;
    if (guaranteedValueValueTick < firstMsgOutsideWindow)
      outside = false;
    
    if(TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "isOutsideSendWindow", Boolean.valueOf(outside)); 
    return outside;
  }

  public synchronized void setLatestAckExpected(long ackExpStamp) {
    if(TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(this, tc, "setLatestAckExpected", Long.valueOf(ackExpStamp));
      SibTr.exit(this, tc, "setLatestAckExpected");
    }
    this.lastAckExpTick = ackExpStamp;
  }
}

