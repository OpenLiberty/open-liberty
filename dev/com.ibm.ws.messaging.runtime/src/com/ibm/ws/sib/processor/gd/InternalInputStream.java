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
import java.util.List;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.ejs.util.am.AlarmListener;
import com.ibm.websphere.sib.Reliability;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.mfp.control.ControlNack;
import com.ibm.ws.sib.processor.SIMPConstants;

import com.ibm.ws.sib.processor.impl.interfaces.UpstreamControl;
import com.ibm.ws.sib.processor.utils.am.MPAlarmManager;
import com.ibm.ws.sib.utils.SIBUuid12;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * All the information maintained for a certain input stream
 * when the pubend is hosted remotely. Hence downstream messages are being received from another broker.
 */
public class InternalInputStream extends ControllableStream
{
  private static final TraceComponent tc =
    SibTr.register(
      InternalInputStream.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);

  //NLS for component
  private static final TraceNLS nls =
    TraceNLS.getTraceNLS(SIMPConstants.RESOURCE_BUNDLE);

  // Tick value of highest ack on input stream
  // arrived at by aggregating output stream acks
  volatile long iack;

  private StateStream iststream;

  private int priority;
  private Reliability reliability;  

  // The current timer which on expiry will result in an ack being transmitted to the parent
  // The Ack Expiry handler
  AckExpiryHandle ackExpiry;
 
  UpstreamControl upControl;  
  
  // The stream ID this data structure is associated with
  SIBUuid12 streamID;
 
  private MPAlarmManager am;
  
  public InternalInputStream(
    int priority,
    Reliability reliability,
    UpstreamControl upControl,
    SIBUuid12 sourceStream,
    List scratch,
    MPAlarmManager am)
  {
    if (tc.isEntryEnabled()) 
      SibTr.entry(tc, "InternalInputStream", 
        new Object[]{new Integer(priority), reliability, upControl, sourceStream, scratch, am});  

    // synchronization ensures that other synchronized methods will
    // see values initialized in constructor
    synchronized (this)
    {
      this.priority = priority;
      this.reliability = reliability;
      this.upControl = upControl;
      this.am = am;
      streamID = sourceStream;
      iststream = new StateStream();
      iststream.init();
      iack = 0;
   
    }

    if (tc.isEntryEnabled())
      SibTr.exit(tc, "InternalInputStream", this);
  }
  
   /* 
    * Utility method to send Ack up to ackPrefix
    */
   private void sendAck()
   {
     if (tc.isEntryEnabled())
       SibTr.entry(tc, "sendAck");
     
     try
     {
       upControl.sendAckMessage(null, null, null, iack, priority, reliability, streamID, false);
     }
     catch (SIException e)
     {
       // FFDC
       FFDCFilter.processException(
         e,
         "com.ibm.ws.sib.processor.gd.InternalInputStream.sendAck",
         "1:161:1.42",
         this);
         
       SibTr.exception(tc, e);

       //TODO don't know what to do here - rethrow?!
     } 
     
     if (tc.isEntryEnabled())
       SibTr.exit(tc, "sendAck");
   }

  protected void scheduleAck()
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "scheduleAck");

    if (GDConfig.GD_ACK_PROPAGATION_THRESHOLD == 0)
    {
      // send the ack immediately. 
      sendAck(); 
    }
    else if (ackExpiry == null)
    {
      ackExpiry = new AckExpiryHandle();
      // start a timer for the ack
      // There is a small race condition here, which is quite harmless unless the
      // GD_ACK_PROPAGATION_THRESHOLD is really small. If the timer expires and the
      // funcion is called before the assignment to ackHandle is done, the ack
      // will not be sent.
      am.create(GDConfig.GD_ACK_PROPAGATION_THRESHOLD, ackExpiry, ackExpiry);

    }
    // else do nothing since a timer is already active and
    // will take care of propagating this ack value when it
    // expires

    if (tc.isEntryEnabled())
      SibTr.exit(tc, "scheduleAck");
  }

  class AckExpiryHandle implements AlarmListener
  {
    public void alarm(Object thandle)
    {
      if (tc.isEntryEnabled())
        SibTr.entry(tc, "alarm", thandle);

      if (thandle == ackExpiry)
      {
        // the ack timer expired. set the handle to null and ask the inputHandler
        // to send an ack on my behalf.

        synchronized (this)
        {
          ackExpiry = null;
        }
        try
        {
          upControl.sendAckMessage(null, null, null, iack, priority, reliability, streamID,false);
        }
        catch (SIException e)
        {
          // FFDC
          FFDCFilter.processException(
           e,
           "com.ibm.ws.sib.processor.gd.InternalInputStream.AckExpiryHandle.alarm",
           "1:228:1.42",
           this);
           
           // TODO Rethrow ?
         
        } 
      }

      if (tc.isEntryEnabled())
        SibTr.exit(tc, "alarm");
    }
  }

  /**
   * Class that implements Reqested forgetting in iststream
   */
  class ReqExpiryHandle implements AlarmListener
  {
    TickRange tr;

    ReqExpiryHandle(TickRange tr)
    {
      if (tc.isEntryEnabled())
        SibTr.entry(tc, "ReqExpiryHandle", tr);

      this.tr = tr;
      am.create(GDConfig.GD_REQUESTED_FORGETTING_THRESHOLD, this);
      
      if (tc.isEntryEnabled())
        SibTr.exit(tc, "ReqExpiryHandle", this);
    }

    public void alarm(Object thandle)
    {
      if (tc.isEntryEnabled())
        SibTr.entry(tc, "alarm", thandle);

      synchronized (InternalInputStream.this)
      {
        if (tr.type == TickRange.Requested)
        {
          tr.type = TickRange.Unknown;
          iststream.writeRange(tr);
        }
      }

      if (tc.isEntryEnabled())
        SibTr.exit(tc, "alarm");
    }
  }



  /**
   * This method sends Nacks for any ticks within the range 
   * startstamp to endstamp which are in Unknown state
   * Those in Requested should already be being satisfied
   */
  public void processNack(ControlNack nm)
    throws SIResourceException
  {
    if (tc.isEntryEnabled())
          SibTr.entry(tc, "processNack", nm);
 
    long startstamp = nm.getStartTick();
    long endstamp = nm.getEndTick();

    synchronized (this)
    {
      // the following steps are performed:
      // (1) check ticks in Unknown
      // (2) create nacks when appropriate
      // (3) change Unknown ticks to Requested
      // (4) send nacks.

      iststream.setCursor(startstamp);
      boolean changed = false;
      TickRange tr = iststream.getNext();
      TickRange tr2;

      do
      {
        // look at each TickRange individually
        // and send nacks for ranges that are not already in Requested state
        if (tr.type == TickRange.Unknown)
        {
          changed = true;
          long maxStartstamp = max(tr.startstamp, startstamp);
          long minEndstamp = min(tr.endstamp, endstamp);
        
          upControl.sendNackMessage(null, null, null, maxStartstamp, minEndstamp, priority, reliability, streamID);
        }

        tr2 = tr;
        tr = iststream.getNext();
      }
      while ((tr.startstamp <= endstamp) && (tr2 != tr));

      if (changed)
      {
        // update the stream to Requested
        tr = new TickRange(TickRange.Requested, startstamp, endstamp);
        iststream.writeRange(tr);

        // start a timer to forget this Request 
        new ReqExpiryHandle(tr);
      }

    } // end synchronized (this)
   
    if (tc.isEntryEnabled())
      SibTr.exit(tc, "processNack");
  }

  
  /**
   * This method will send an Ack up to the current aggregated Ack prefix
   * and send a Nack for any ticks after this
   *
   */
  public void processAckExpected(long stamp)
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "processAckExpected", new Object[] { new Long(stamp)});

    synchronized (this)
    {
      // Send Ack message for ticks up to iack
      sendAck();

      // Send Nack message for ticks between iack and stamp
      if (stamp > iack)
      {
        try
        {
          upControl.sendNackMessage(
            null,
            null,
            null,
            iack+1,
            stamp,
            priority,
            reliability,
            streamID);
        }
        catch (SIResourceException e)
        {
          // FFDC
          FFDCFilter.processException(
            e,
            "com.ibm.ws.sib.processor.gd.InternalInputStream.processAckExpected",
            "1:379:1.42",
            this);

          SibTr.exception(tc, e);
          SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
            new Object[] {
              "com.ibm.ws.sib.processor.gd.InternalInputStream",
              "1:386:1.42",
              e });

          if (tc.isEntryEnabled())
            SibTr.exit(tc, "processAckExpected", e);

          throw new SIErrorException(
            nls.getFormattedMessage(
             "INTERNAL_MESSAGING_ERROR_CWSIP0002",
             new Object[] {
               "com.ibm.ws.sib.processor.gd.InternalInputStream",
               "1:397:1.42",
               e },
             null),
            e);
        }   
      }
    }

    if (tc.isEntryEnabled())
      SibTr.exit(tc, "processAckExpected");
  }
  
  public synchronized void releaseMemory()
  {
    // clear iststream. Note that we don't cancel any timer for curiosity forgetting, since
    // the timer expiry is harmless.
    iststream = new StateStream();
    iststream.init();
  }

  private static long max(long a, long b)
  {
    return a > b ? a : b;
  }
  private static long min(long a, long b)
  {
    return a < b ? a : b;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.gd.InputStream#getAckPrefix()
   */
  public long getAckPrefix()
  {
    return iack;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.gd.InputStream#writeAckPrefix(long)
   */
  public synchronized List writeAckPrefix(long stamp)
  {
    iack = stamp;
    scheduleAck();
    return null;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.gd.Stream#getCompletedPrefix()
   */
  public long getCompletedPrefix()
  {
    return iststream.getCompletedPrefix();
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.gd.Stream#getStateStream()
   */
  public StateStream getStateStream()
  {
    return iststream;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.gd.Stream#writeSilenceForced(long)
   */
  public void writeSilenceForced(long tick)
  {
    // No implementation
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.gd.ControllableStream#getPriority()
   */
  protected int getPriority()
  {
    if (tc.isEntryEnabled()) 
      SibTr.entry(tc, "getPriority");
    if (tc.isEntryEnabled()) 
      SibTr.exit(tc, "getPriority", new Integer(priority));
    return priority;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.gd.ControllableStream#getReliability()
   */
  protected Reliability getReliability()
  {
    if (tc.isEntryEnabled()) 
      SibTr.entry(tc, "getReliability");
    if (tc.isEntryEnabled()) 
      SibTr.exit(tc, "getReliability", reliability);  
    
    return reliability;
  }

}
