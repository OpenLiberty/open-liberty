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

package com.ibm.ws.sib.processor.impl.store.itemstreams;

import java.util.Properties;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.sib.management.SibNotificationConstants;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.impl.BaseDestinationHandler;
import com.ibm.ws.sib.processor.impl.LinkHandler;
import com.ibm.ws.sib.processor.runtime.impl.ControlAdapter;
import com.ibm.ws.sib.processor.runtime.impl.MPRuntimeEvent;
import com.ibm.ws.sib.processor.runtime.impl.MQLinkQueuePoint;
import com.ibm.ws.sib.utils.SIBUuid8;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * @author millwood
 * 
 * This object is used to store the messages for a destination    
 * localisation.
 */
public class MQLinkMessageItemStream extends PtoPLocalMsgsItemStream
{  
  
  private static TraceComponent tc =
    SibTr.register(
      MQLinkMessageItemStream.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);
      
    
  /** NLS for component */
  static final TraceNLS nls =
    TraceNLS.getTraceNLS(SIMPConstants.RESOURCE_BUNDLE);
    
  /**
   * Warm start constructor invoked by the Message Store.
   */
  public MQLinkMessageItemStream()
  {
    super();
    // This space intentionally blank
  }

  /**
   * <p>Cold start MQLinkMessageItemStream constructor.</p>
   * 
   * @param destinationHandler
   */
  public MQLinkMessageItemStream(BaseDestinationHandler destinationHandler,
                                 SIBUuid8 messagingEngineUuid)
  {
    super(destinationHandler, messagingEngineUuid);
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.ControllableResource#createControlAdapter()
   */
  public void createControlAdapter()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "createControlAdapter");
    
    controlAdapter = new MQLinkQueuePoint(destinationHandler.getMessageProcessor(), this);
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "createControlAdapter");
  }

  /**
   * Fire an event notification of type TYPE_SIB_MQLINK_DEPTH_THRESHOLD_REACHED
   * 
   * @param newState
   */
  public void fireDepthThresholdReachedEvent(ControlAdapter cAdapter,
                                             boolean reachedHigh, 
                                             long numMsgs)
  {
     if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
       SibTr.entry(tc, "fireDepthThresholdReachedEvent",
           new Object[] { cAdapter, new Boolean(reachedHigh), new Long(numMsgs)});     

     // Retrieve appropriate information
     String destinationName = destinationHandler.getName(); 
     
     String meName = destinationHandler.
                       getMessageProcessor().
                       getMessagingEngineName();
     
     // If we've been told to output the event message to the log, do it...
     // (510343)
     if((mp.getCustomProperties().getOutputLinkThresholdEventsToLog() && destinationHandler.isLink()) ||
        (mp.getCustomProperties().getOutputDestinationThresholdEventsToLog() && !destinationHandler.isLink()))
     {
       if(reachedHigh)
         SibTr.info(tc, "NOTIFY_MQLINK_DEPTH_THRESHOLD_REACHED_CWSIP0557",
             new Object[] { destinationName, meName});
       else
         SibTr.info(tc, "NOTIFY_MQLINK_DEPTH_THRESHOLD_REACHED_CWSIP0558",
             new Object[] { destinationName, meName});
     }
     
     // If we're actually issuing events, do that too...
     if(_isEventNotificationEnabled)
     {
       if(cAdapter != null)
       {
         // Build the message for the Notification
         String message = null;
         if(reachedHigh)
           message = nls.getFormattedMessage("NOTIFY_MQLINK_DEPTH_THRESHOLD_REACHED_CWSIP0557",
                                             new Object[] { destinationName, 
                                             meName},
                                             null);
         else
           message = nls.getFormattedMessage("NOTIFY_MQLINK_DEPTH_THRESHOLD_REACHED_CWSIP0558",
                                             new Object[] { destinationName, 
                                             meName},
                                             null);         
       
         // Build the properties for the Notification
         Properties props = new Properties();

         props.put(SibNotificationConstants.KEY_MQLINK_NAME, destinationName); 
         props.put(SibNotificationConstants.KEY_MQLINK_UUID, destinationHandler.getUuid().toString());

         if(reachedHigh)
           props.put(SibNotificationConstants.KEY_DEPTH_THRESHOLD_REACHED,
               SibNotificationConstants.DEPTH_THRESHOLD_REACHED_HIGH);
         else
           props.put(SibNotificationConstants.KEY_DEPTH_THRESHOLD_REACHED,
               SibNotificationConstants.DEPTH_THRESHOLD_REACHED_LOW);

         // Number of Messages
         props.put(SibNotificationConstants.KEY_MESSAGES, String.valueOf(numMsgs));
         // Now create the Event object to pass to the control adapter
         MPRuntimeEvent MPevent = 
           new MPRuntimeEvent(SibNotificationConstants.TYPE_SIB_MQLINK_DEPTH_THRESHOLD_REACHED,
                              message,
                              props);
         // Fire the event
         if (tc.isDebugEnabled()) 
           SibTr.debug(tc, "fireDepthThresholdReachedEvent","Drive runtimeEventOccurred against Control adapter: " + cAdapter);             
         
         cAdapter.runtimeEventOccurred(MPevent);
       }
       else
       {
         if (tc.isDebugEnabled()) 
           SibTr.debug(tc, "fireDepthThresholdReachedEvent", "Control adapter is null, cannot fire event" );      
       } 
     }
     
     if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
       SibTr.exit(tc, "fireDepthThresholdReachedEvent");     
  }           

  /*
   * Link specific warning message
   * (510343)
   */
  protected void issueDepthIntervalMessage(long depth)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "issueDepthIntervalMessage", new Long(depth));
    
    // {0} messages queued for transmission from messaging engine {1} to foreign bus {2} on link {3}.
    SibTr.info(tc, "REMOTE_LINK_DESTINATION_DEPTH_INTERVAL_REACHED_CWSIP0789",
        new Object[] {new Long(depth),
                      destinationHandler.getMessageProcessor().getMessagingEngineName(),
                      ((LinkHandler)destinationHandler).getBusName(),
                      destinationHandler.getName()});
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "issueDepthIntervalMessage");
  }
}
