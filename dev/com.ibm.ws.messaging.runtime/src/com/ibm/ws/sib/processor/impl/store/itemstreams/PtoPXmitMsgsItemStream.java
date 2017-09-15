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
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.websphere.sib.management.SibNotificationConstants;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.msgstore.MessageStoreException;
import com.ibm.ws.sib.msgstore.transactions.ExternalAutoCommitTransaction;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.impl.BaseDestinationHandler;
import com.ibm.ws.sib.processor.impl.LinkHandler;
import com.ibm.ws.sib.processor.impl.PtoPOutputHandler;
import com.ibm.ws.sib.processor.impl.interfaces.ConsumerManager;
import com.ibm.ws.sib.processor.runtime.impl.ControlAdapter;
import com.ibm.ws.sib.processor.runtime.impl.LinkRemoteQueuePoint;
import com.ibm.ws.sib.processor.runtime.impl.MPRuntimeEvent;
import com.ibm.ws.sib.processor.runtime.impl.XmitPoint;
import com.ibm.ws.sib.processor.runtime.impl.XmitPointControl;
import com.ibm.ws.sib.processor.utils.SIMPUtils;
import com.ibm.ws.sib.utils.SIBUuid8;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * @author millwood
 * 
 * This object is used to store messages awaiting transmission to
 * a remote destination localisation.
 */
public class PtoPXmitMsgsItemStream extends PtoPMessageItemStream
{
  private static final TraceComponent tc =
    SibTr.register(
      PtoPXmitMsgsItemStream.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);
      
  
  /**
   * NLS for component
   */
  private static final TraceNLS nls =
    TraceNLS.getTraceNLS(SIMPConstants.RESOURCE_BUNDLE);  
  
  /**
   * Warm start constructor invoked by the Message Store.
   */
  public PtoPXmitMsgsItemStream()
  {
    super();
  }

  /**
   * <p>Cold start PtoPMessageItemStream constructor.</p>
   * 
   * @param destinationHandler
   * @param messagingEngineUuid
   */
  public PtoPXmitMsgsItemStream(BaseDestinationHandler destinationHandler,
                                SIBUuid8 messagingEngineUuid)
  {
    super(destinationHandler, messagingEngineUuid, true);
  }
  
  /*
   * Low level override is no longer required, see BaseMessageItemStream
   * for the implementation of this method:
   * 
   *   protected void setDestLimits(long newDestHighMsgs, 
   *                          long newDestLowMsgs) 
   *                          
   * (510343)
   */

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.store.itemstreams.PtoPMessageItemStream#reallocateMsgs()
   */
  public boolean reallocateMsgs()
  {
     if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
       SibTr.entry(tc, "reallocateMsgs");
     
     boolean success = true;

     // Reallocate the messages on the outputhandler to spread the load over
     // the localisations
     try
     {     
       if (getOutputHandler() instanceof PtoPOutputHandler) 
       {
         ((PtoPOutputHandler)getOutputHandler()).reallocateMsgs(getDestinationHandler(), false, false);
       }
       else
         success = false;
     }
     catch (SIException e)
     {
       // No FFDC code needed
       success = false;
     }     
     
     if (success)
     {     
       // If non-empty, then do not delete yet
       long itemCount = -1;
       long availableCount = -1;
       long removingCount = -1;
       
       try
       {
         itemCount = getStatistics().getTotalItemCount();
         availableCount = getStatistics().getAvailableItemCount();
         removingCount = getStatistics().getRemovingItemCount();
       }
       catch(MessageStoreException e)
       {
         // FFDC
         FFDCFilter.processException(
             e,
             "com.ibm.ws.sib.processor.impl.store.itemstreams.PtoPXmitMsgsItemStream.reallocateMsgs",
             "1:160:1.56",
             this);  
         
         SibTr.exception(tc, e); 
       }   
       
       if (itemCount > 0)
       {
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
           SibTr.debug(tc, "Messages remain on stream: " + itemCount + " : " +
               availableCount + " : " +
               removingCount);
         
         success = false;
       }
       else
       {
         if (isToBeDeleted() || destinationHandler.isToBeDeleted())
         {
           ExternalAutoCommitTransaction transaction = getDestinationHandler().
                                                        getTxManager().
                                                        createAutoCommitTransaction();
           try
           {
             removeItemStream(transaction, NO_LOCK_ID);
           }
           catch (MessageStoreException e)
           {
             
             // MessageStoreException shouldn't occur so FFDC.
             FFDCFilter.processException(
               e,
               "com.ibm.ws.sib.processor.impl.store.itemstreams.PtoPXmitMsgsItemStream.reallocateMsgs",
               "1:193:1.56",
               this);
    
             SibTr.exception(tc, e);
              
             success = false;
           }
         }
       }
     }
       
     if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
       SibTr.exit(tc, "reallocateMsgs", new Boolean(success));
    
     return success;
  }

  /**
   * Complete recovery of a ItemStream retrieved from the MessageStore.
   * <p>
   * 
   * @param destinationHandler to use in reconstitution
   */    
  public void reconstitute(BaseDestinationHandler destinationHandler) throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "reconstitute", destinationHandler); 

    super.reconstitute(destinationHandler);
      
    _destHighMsgs = destinationHandler.getMessageProcessor().getHighMessageThreshold();

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.exit(tc, "reconstitute");           
    }
  }     

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.ControllableResource#createControlAdapter()
   */
  public void createControlAdapter()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "createControlAdapter");

    // The type of control adapter created depends on the type of destination
    // that we're working with
    if(!destinationHandler.isLink())
    {
      XmitPoint rqm =
        (XmitPoint) destinationHandler.getRemoteQueuePointControl(getLocalizingMEUuid(), true);
      controlAdapter = new XmitPointControl(rqm,
                                            this,                                        
                                            destinationHandler); 
    }
    else
    { 
      // Use the StreamSet's Control Adapter for the "link remote queue points"
      controlAdapter = 
        new LinkRemoteQueuePoint(destinationHandler, this);
    }                                          
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "createControlAdapter", controlAdapter);
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.ControllableResource#deregisterControlAdapterMBean()
   */
  public void deregisterControlAdapterMBean()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "deregisterControlAdapterMBean");
    getControlAdapter().deregisterControlAdapterMBean();        
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "deregisterControlAdapterMBean");
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.ControllableResource#registerControlAdapterAsMBean()
   */
  public void registerControlAdapterAsMBean()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "registerControlAdapterAsMBean");    
    getControlAdapter().registerControlAdapterAsMBean();
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "registerControlAdapterAsMBean");
  }
  
  /**
   * Fire an event notification of type TYPE_SIB_REMOTE_MESSAGEPOINT_DEPTH_THRESHOLD_REACHED
   * 
   * @param newState
   */
  public void fireDepthThresholdReachedEvent(ControlAdapter cAdapter,
                                                   boolean reachedHigh, 
                                                   long numMsgs)
  {
     if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
       SibTr.entry(tc, "fireDepthThresholdReachedEvent",
           new Object[] { cAdapter, new Boolean(reachedHigh), new Long(numMsgs), messagingEngineUuid });     

     // Retrieve appropriate information
     
     String meName = destinationHandler.
                       getMessageProcessor().
                       getMessagingEngineName();
     
     // The message for the Notification
     String message = null;
     
     // Build the properties for the Notification
     Properties props = new Properties();

     String evType = null;
     if(destinationHandler.isLink())
     {
       String busName = ((LinkHandler)destinationHandler).getBusName();
       evType = SibNotificationConstants.TYPE_SIB_LINK_DEPTH_THRESHOLD_REACHED;
       props.put(SibNotificationConstants.KEY_FOREIGN_BUS_NAME, busName); 
       props.put(SibNotificationConstants.KEY_LINK_NAME, destinationHandler.getName()); 
       props.put(SibNotificationConstants.KEY_LINK_UUID, destinationHandler.getUuid().toString());
       
       // Build the message for the Notification
       if(reachedHigh)
       {
         message = nls.getFormattedMessage("NOTIFY_SIBLINK_DEPTH_THRESHOLD_REACHED_CWSIP0559",
                                           new Object[] {destinationHandler.getName(),
                                             busName, 
                                             meName},
                                             null);
         
         // Write the message to the log if requested (510343)
         if(mp.getCustomProperties().getOutputLinkThresholdEventsToLog())
           SibTr.info(tc, "NOTIFY_SIBLINK_DEPTH_THRESHOLD_REACHED_CWSIP0559",
                 new Object[] {destinationHandler.getName(),
                               busName, 
                               meName});
       }
       else
       {
         message = nls.getFormattedMessage("NOTIFY_SIBLINK_DEPTH_THRESHOLD_REACHED_CWSIP0560",
                                           new Object[] {destinationHandler.getName(),
                                             busName, 
                                             meName},
                                             null);                  
         
         // Write the message to the log if requested (510343)
         if(mp.getCustomProperties().getOutputLinkThresholdEventsToLog())
           SibTr.info(tc, "NOTIFY_SIBLINK_DEPTH_THRESHOLD_REACHED_CWSIP0560",
                 new Object[] {destinationHandler.getName(),
                               busName, 
                               meName});
       }
     }
     else
     {
       evType = SibNotificationConstants.TYPE_SIB_REMOTE_MESSAGEPOINT_DEPTH_THRESHOLD_REACHED;
       props.put(SibNotificationConstants.KEY_DESTINATION_NAME, destinationHandler.getName()); 
       props.put(SibNotificationConstants.KEY_DESTINATION_UUID, destinationHandler.getUuid().toString());
       
       // Build the message for the Notification
       if(reachedHigh)
       {
         message = nls.getFormattedMessage("NOTIFY_REMOTE_DEPTH_THRESHOLD_REACHED_CWSIP0555",
                                           new Object[] { meName,
                                             destinationHandler.getName(), 
                                             messagingEngineUuid.toString()},
                                             null);
         
         // Write the message to the log if requested (510343)
         if(mp.getCustomProperties().getOutputDestinationThresholdEventsToLog())
           SibTr.info(tc, "NOTIFY_REMOTE_DEPTH_THRESHOLD_REACHED_CWSIP0555",
                 new Object[] {meName,
                               destinationHandler.getName(),
                               messagingEngineUuid.toString()}); 
       }
       else
       {
         message = nls.getFormattedMessage("NOTIFY_REMOTE_DEPTH_THRESHOLD_REACHED_CWSIP0556",
                                           new Object[] { meName,
                                             destinationHandler.getName(), 
                                             messagingEngineUuid.toString()},
                                             null);
         
         // Write the message to the log if requested (510343)
         if(mp.getCustomProperties().getOutputDestinationThresholdEventsToLog())
           SibTr.info(tc, "NOTIFY_REMOTE_DEPTH_THRESHOLD_REACHED_CWSIP0556",
               new Object[] {meName,
                             destinationHandler.getName(),
                             messagingEngineUuid.toString()}); 
       }
     }
     
     if(_isEventNotificationEnabled)
     {
       props.put(SibNotificationConstants.KEY_REMOTE_MESSAGING_ENGINE_UUID,messagingEngineUuid.toString());
       
       if(reachedHigh)
         props.put(SibNotificationConstants.KEY_DEPTH_THRESHOLD_REACHED,
             SibNotificationConstants.DEPTH_THRESHOLD_REACHED_HIGH);
       else
         props.put(SibNotificationConstants.KEY_DEPTH_THRESHOLD_REACHED,
             SibNotificationConstants.DEPTH_THRESHOLD_REACHED_LOW);

       // Number of Messages
       props.put(SibNotificationConstants.KEY_MESSAGES, String.valueOf(numMsgs));
       
       if(cAdapter != null)
       {
         // Now create the Event object to pass to the control adapter
         MPRuntimeEvent MPevent = 
           new MPRuntimeEvent(evType,
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
   * Remote queue point (queue or link) specific warning message
   * (510343)
   */
  protected void issueDepthIntervalMessage(long depth)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "issueDepthIntervalMessage", new Long(depth));
    
    if(destinationHandler.isLink())
    {
      // {0} messages queued for transmission from messaging engine {1} to foreign bus {2} on link {3}.
      SibTr.info(tc, "REMOTE_LINK_DESTINATION_DEPTH_INTERVAL_REACHED_CWSIP0789",
          new Object[] {new Long(depth),
                        destinationHandler.getMessageProcessor().getMessagingEngineName(),
                        ((LinkHandler)destinationHandler).getBusName(),
                        destinationHandler.getName()});
    }
    else
    {
      // {0} messages queued for transmission on messaging engine {1} to destination {2} on messaging engine {3}.
      SibTr.info(tc, "REMOTE_DESTINATION_DEPTH_INTERVAL_REACHED_CWSIP0788",
          new Object[] {new Long(depth),
          destinationHandler.getMessageProcessor().getMessagingEngineName(),
                        destinationHandler.getName(),
                        SIMPUtils.getMENameFromUuid(((PtoPOutputHandler)getOutputHandler()).getTargetMEUuid().toString())});
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "issueDepthIntervalMessage");
  }
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.LocalizationPoint#getConsumerManager()
   */
  public ConsumerManager getConsumerManager()
  {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.LocalizationPoint#dereferenceConsumerManager()
   */
  public void dereferenceConsumerManager()
  {
    // TODO Auto-generated method stub
    
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.LocalizationPoint#isSendAllowed()
   */
  public boolean isSendAllowed()
  {
    // TODO Auto-generated method stub
    return false;
  }           
}

