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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.Reliability;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.sib.mfp.JsMessage;
import com.ibm.ws.sib.mfp.control.ControlAck;
import com.ibm.ws.sib.mfp.control.ControlAckExpected;
import com.ibm.ws.sib.mfp.control.ControlNack;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.impl.MessageProcessor;
import com.ibm.ws.sib.processor.impl.interfaces.FlushComplete;
import com.ibm.ws.sib.processor.impl.interfaces.UpstreamControl;
import com.ibm.ws.sib.utils.SIBUuid12;
import com.ibm.ws.sib.utils.ras.SibTr;

public class InternalInputStreamManager
{  
    
  private static final TraceComponent tc =
    SibTr.register(
      InternalInputStreamManager.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);
  
  

  private UpstreamControl upControl;    
  private MessageProcessor messageProcessor;
  
  // Maps stream IDs to instances of StreamSet.  For v1, we should
  // only be maintaining information about the stream in the local
  // PubSubInputHandler (for a given destination).  Longer term,
  // we may have many streams stored here.
  private Map streamSets;
  
  /**
   * A reference to the flush callback we need to make when the current
   * stream is finally flushed.
   */
  protected FlushComplete flushInProgress = null;
       
  public InternalInputStreamManager(MessageProcessor messageProcessor, UpstreamControl upControl )
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "InternalInputStreamManager", new Object[]{messageProcessor, upControl});

    this.upControl = upControl;
    this.messageProcessor = messageProcessor;
    
    streamSets = Collections.synchronizedMap(new HashMap());
    
    if (tc.isEntryEnabled())
      SibTr.exit(tc, "InternalInputStreamManager", this);
  } 
   
  /**
   * returns true if a given set of streams exists
   * 
   * @param streamID
   * @return
   * @throws SIResourceException
   */
  public boolean hasStream(SIBUuid12 streamID, int priority, Reliability reliability ) throws SIResourceException 
  {
    if (tc.isEntryEnabled()) 
      SibTr.entry(tc, "hasStream", 
        new Object[]{streamID, new Integer(priority), reliability});
        
    StreamSet streamSet = (StreamSet) streamSets.get(streamID);
    if(streamSet != null) 
    {
      if ( streamSet.getStream(priority, reliability) != null )        
      { 
        if (tc.isEntryEnabled()) SibTr.exit(tc, "hasStream", Boolean.TRUE);
        return true;
      }  
    }
    if (tc.isEntryEnabled()) SibTr.exit(tc, "hasStream", Boolean.FALSE);
    return false;  
  }
  
  /**
    * This method creates a Stream if this is the first
    * inbound message which has been sent to remote Mes
     * 
    * @param  jsMsg
   * @throws SIResourceException
    * @throws SICoreException
    */
   public void processMessage(JsMessage jsMsg) throws SIResourceException
   {
     if (tc.isEntryEnabled())
       SibTr.entry(tc, "processMessage", new Object[] { jsMsg });
    
     int priority = jsMsg.getPriority().intValue();
     Reliability reliability = jsMsg.getReliability();
       
     SIBUuid12 streamID = jsMsg.getGuaranteedStreamUUID();
     StreamSet streamSet = getStreamSet(streamID, true);
     
     InternalInputStream internalInputStream = null;
     synchronized(streamSet)
     {
       internalInputStream = (InternalInputStream) streamSet.getStream(priority, reliability);        

       // This may be the first message which has required this stream    
       if(internalInputStream == null && 
        (reliability.compareTo(Reliability.BEST_EFFORT_NONPERSISTENT) > 0))
       {
         internalInputStream = createStream(streamSet, priority, reliability);      
       }
     }
        
     if (tc.isEntryEnabled())
       SibTr.exit(tc, "processMessage");
   }
 
  
  /**
   * Handle an ControlAckExpected message. This will result in either a ControlAreYouFlushed
   * or a ControlNack being sent back to the source.
   * 
   * @param cMsg
   * @throws SIResourceException
   */
  public void processAckExpected(ControlAckExpected ackExpMsg) throws SIResourceException 
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "processAckExpectedMessage", new Object[] { ackExpMsg });
    
    int priority = ackExpMsg.getPriority().intValue();
    Reliability reliability = ackExpMsg.getReliability();
    
    SIBUuid12 streamID = ackExpMsg.getGuaranteedStreamUUID();
    StreamSet streamSet = getStreamSet(streamID, true);
  
    InternalInputStream internalInputStream = null;
    synchronized(streamSet)
    {
      internalInputStream = (InternalInputStream) streamSet.getStream(priority, reliability);        

      // This may be the first message which has required this stream    
      if(internalInputStream == null && 
      (reliability.compareTo(Reliability.BEST_EFFORT_NONPERSISTENT) > 0))
      {
        internalInputStream = createStream(streamSet, priority, reliability);      
      }
    }
        
    internalInputStream.processAckExpected(ackExpMsg.getTick());
        
    if (tc.isEntryEnabled())
      SibTr.exit(tc, "processAckExpectedMessage");
  }
  
  public List processAck(ControlAck ackMsg) throws SIResourceException
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "processAck",
        new Object[] { ackMsg });

    // Get the ackPrefix from the message
    long ackPrefix = ackMsg.getAckPrefix();

    List indexList = processAck(ackMsg, ackPrefix);
    
    if (tc.isEntryEnabled())
      SibTr.exit(tc, "processAck", indexList);
      
    return indexList;

  }

  public List processAck(ControlAck ackMsg, long ackPrefix) throws SIResourceException
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "processAck",
        new Object[] { ackMsg, new Long(ackPrefix) });           

    int priority = ackMsg.getPriority().intValue();
    Reliability reliability = ackMsg.getReliability();

    List indexList = null;
    
    SIBUuid12 streamID = ackMsg.getGuaranteedStreamUUID();
    StreamSet streamSet = getStreamSet(streamID, true);
  
    InternalInputStream internalInputStream = null;
    synchronized(streamSet)
    {
      internalInputStream = (InternalInputStream) streamSet.getStream(priority, reliability);        

      // This may be the first message which has required this stream    
      if(internalInputStream == null && 
      (reliability.compareTo(Reliability.BEST_EFFORT_NONPERSISTENT) > 0))
      {
        internalInputStream = createStream(streamSet, priority, reliability);      
      }
    }
     
    // If this increases the finality prefix then
    // update it and delete the acked messages from the ItemStream
    long completedPrefix = internalInputStream.getAckPrefix();
    if (ackPrefix > completedPrefix)
    {

      // Update the completedPrefix and the oack value for the stream
      // returns a lit of the itemStream ids of the newly Acked messages
      // which we can then remove from the itemStream
      indexList = internalInputStream.writeAckPrefix(ackPrefix);
    }
    
//    if (flushInProgress != null)
//      attemptFlush();
    
    if (tc.isEntryEnabled())
      SibTr.exit(tc, "processAck", indexList);
      
    return indexList;

  }

  public void processNack(ControlNack nackMsg) throws SIResourceException
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "processNack", nackMsg);

    int priority = nackMsg.getPriority().intValue();
    Reliability reliability = nackMsg.getReliability();
    
    SIBUuid12 streamID = nackMsg.getGuaranteedStreamUUID();
    StreamSet streamSet = getStreamSet(streamID, true);
    
    InternalInputStream internalInputStream = null;
    synchronized(streamSet)
    {
      internalInputStream = (InternalInputStream) streamSet.getStream(priority, reliability);        
      // This may be the first message which has required this stream    
      if(internalInputStream == null && 
      (reliability.compareTo(Reliability.BEST_EFFORT_NONPERSISTENT) > 0))
      {
        internalInputStream = createStream(streamSet, priority, reliability);      
      }  
    }
    
    internalInputStream.processNack(nackMsg);

    if (tc.isEntryEnabled())
      SibTr.exit(tc, "processNack");

  }
  
  /**
    * Get a StreamSet for a given streamID.  Optionally create the StreamSet
    * if it doesn't already exit.  
    * 
    * @param streamID The streamID to map to a StreamSet.
    * @param create If TRUE then create the StreamSet if it doesn't already exit.
    * @return An instance of StreamSet
    */
   private StreamSet getStreamSet(SIBUuid12 streamID, boolean create)
   {
     if (tc.isEntryEnabled())
       SibTr.entry(tc, "getStreamSet", new Object[]{streamID, new Boolean(create)});

     StreamSet streamSet = null;
     synchronized (streamSets)
     {
       streamSet = (StreamSet) streamSets.get(streamID);
       if ((streamSet == null) && create)
       {
         streamSet = new StreamSet(streamID, null, 0, StreamSet.Type.INTERNAL_INPUT);
         streamSets.put(streamID, streamSet);
       }
     }

     if (tc.isEntryEnabled())
       SibTr.exit(tc, "getStreamSet", streamSet);
     return streamSet;
   }


  private InternalInputStream createStream(StreamSet streamSet, int priority, Reliability reliability) throws SIResourceException 
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "createStream", new Object[] {new Integer(priority), reliability});

    InternalInputStream stream = null;
    
    //there is no source stream for express messages
    if(reliability.compareTo(Reliability.BEST_EFFORT_NONPERSISTENT) >0)
    {          
      stream = new InternalInputStream( priority,
                                        reliability,
                                        upControl,
                                        streamSet.getStreamID(),
                                        null,
                                        messageProcessor.getAlarmManager());
    
    }
    
    streamSet.setStream(priority, reliability, stream);
    
    if (tc.isEntryEnabled())
      SibTr.exit(tc, "createStream", stream);
      
    return stream;
  }
    

}
