/*******************************************************************************
 * Copyright (c) 2012, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.processor.runtime.impl;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.admin.ControllableType;
import com.ibm.ws.sib.admin.RuntimeEvent;
import com.ibm.ws.sib.msgstore.AbstractItem;
import com.ibm.ws.sib.msgstore.MessageStoreException;
import com.ibm.ws.sib.msgstore.NonLockingCursor;
import com.ibm.ws.sib.msgstore.NotInMessageStore;
import com.ibm.ws.sib.msgstore.ReferenceStream;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.exceptions.SIMPControllableNotFoundException;
import com.ibm.ws.sib.processor.exceptions.SIMPException;
import com.ibm.ws.sib.processor.exceptions.SIMPInvalidRuntimeIDException;
import com.ibm.ws.sib.processor.exceptions.SIMPRuntimeOperationFailedException;
import com.ibm.ws.sib.processor.impl.AnycastOutputHandler;
import com.ibm.ws.sib.processor.impl.BaseDestinationHandler;
import com.ibm.ws.sib.processor.impl.ConsumerDispatcher;
import com.ibm.ws.sib.processor.impl.ConsumerDispatcherState;
import com.ibm.ws.sib.processor.impl.MessageProcessor;
import com.ibm.ws.sib.processor.impl.interfaces.SIMPMessage;
import com.ibm.ws.sib.processor.impl.store.items.MessageItem;
import com.ibm.ws.sib.processor.impl.store.items.MessageItemReference;
import com.ibm.ws.sib.processor.runtime.SIMPIterator;
import com.ibm.ws.sib.processor.runtime.SIMPLocalSubscriptionControllable;
import com.ibm.ws.sib.processor.runtime.SIMPQueuedMessageControllable;
import com.ibm.ws.sib.processor.runtime.SIMPTopicSpaceControllable;
import com.ibm.ws.sib.processor.utils.SIMPUtils;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * 
 */
public class LocalSubscriptionControl 
  extends AbstractRegisteredControlAdapter 
  implements SIMPLocalSubscriptionControllable
{
  
  private ConsumerDispatcherState consumerDispatcherState;
  private ReferenceStream referenceItemStream;

  private static TraceComponent tc =
    SibTr.register(
      LocalSubscriptionControl.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);

  
  private static final TraceNLS nls = TraceNLS.getTraceNLS(SIMPConstants.RESOURCE_BUNDLE);  

  private SIMPTopicSpaceControllable topicspaceControl;
  private ConsumerDispatcher consumerDispatcher;

  public LocalSubscriptionControl(ConsumerDispatcher cd,
                                  SIMPTopicSpaceControllable topicspaceControl,
                                  MessageProcessor messageProcessor)
  {
    super( messageProcessor , ControllableType.SUBSCRIPTION_POINT );
    this.consumerDispatcher = cd;
    this.referenceItemStream = cd.getReferenceStream();
    this.topicspaceControl = topicspaceControl;
    this.consumerDispatcherState = consumerDispatcher.getConsumerDispatcherState();
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPLocalSubscriptionControllable#getTopicSpace()
   */
  public SIMPTopicSpaceControllable getTopicSpace()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getTopicSpace");
      SibTr.exit(tc, "getTopicSpace", topicspaceControl);
    }
    return topicspaceControl;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPLocalSubscriptionControllable#getSelector()
   */
  public String getSelector()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getSelector");

    String selector = consumerDispatcherState.
             getSelectionCriteria().
             getSelectorString();
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())             
      SibTr.exit(tc, "getSelector", selector);
    return selector;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPLocalSubscriptionControllable#getSubscriberID()
   */
  public String getSubscriberID()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getSubscriberID");

    String subId = consumerDispatcherState.getSubscriberID();
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())             
      SibTr.exit(tc, "getSubscriberID", subId);
    return subId;
  }
  
  protected String getDurableHome()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getDurableHome");

    String durableHome = consumerDispatcherState.getDurableHome();
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())             
      SibTr.exit(tc, "getDurableHome", durableHome);
    return durableHome;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPLocalSubscriptionControllable#getTopics()
   */
  public String[] getTopics()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getTopics");

    String[] topics = consumerDispatcherState.getTopics();
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())             
      SibTr.exit(tc, "getTopics", topics);
    return topics;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPLocalSubscriptionControllable#getConsumerIterator()
   */
  public SIMPIterator getConsumerIterator()
  {   
    return null;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPLocalSubscriptionControllable#getRemoteConsumerTransmit()
   */
  public SIMPIterator getRemoteConsumerTransmit()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getRemoteConsumerTransmitIterator");

    SIMPIterator remoteConsumerTransmitIterator = null;
    
    BaseDestinationHandler destinationHandler = consumerDispatcher.getDestination();  
    String subName = consumerDispatcherState.getSubscriberID();
    String pseudoDestName = destinationHandler.constructPseudoDurableDestName(subName);
    AnycastOutputHandler aoh = destinationHandler.getAnycastOHForPseudoDest(pseudoDestName);
    
    if(aoh!=null)
      remoteConsumerTransmitIterator = new BasicSIMPIterator(aoh.getAOControlAdapterIterator());
    else // Create an empty iterator
      remoteConsumerTransmitIterator = new BasicSIMPIterator(new LinkedList().iterator());

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getRemoteConsumerTransmitIterator", remoteConsumerTransmitIterator);

    return remoteConsumerTransmitIterator;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPLocalSubscriptionControllable#getQueuedMessageIterator()
   */
  public SIMPIterator getQueuedMessageIterator()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getQueuedMessageIterator");

    List<ControlAdapter> messages = new ArrayList<ControlAdapter>();
    
    NonLockingCursor cursor = null;

    MessageItem referredItem = null;
    try
    {
      cursor =
        consumerDispatcher.getReferenceStream().newNonLockingCursor(null);
      cursor.allowUnavailableItems();
      MessageItemReference item = (MessageItemReference) cursor.next();
      while(item != null)
      {
        try
        {
          referredItem = (MessageItem)item.getReferredItem();
          if(referredItem != null)
          {
            // force the arrival time to be written to the jsMessage  
            referredItem.forceCurrentMEArrivalTimeToJsMessage();
          }
          
          // It's possible to get a null adapter back 
          ControlAdapter cAdapter = item.getControlAdapter();
          if(cAdapter != null)
            messages.add(cAdapter);
          
          item = (MessageItemReference) cursor.next();
        }
        catch (NotInMessageStore ef)
        {
          // No FFDC code needed
          // The message has already been consumed. Trace the exception but allow processing to continue.
          SibTr.exception(tc, ef);
        }
        catch (MessageStoreException e)
        {
          // FFDC
          FFDCFilter.processException(
            e,
            "com.ibm.ws.sib.processor.runtime.LocalSubscriptionControl.getQueuedMessageIterator",
            "1:275:1.36",
            this);
            

          SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
            new Object[] { "com.ibm.ws.sib.processor.runtime.LocalSubscriptionControl.getQueuedMessageIterator", 
                           "1:281:1.36", 
                           SIMPUtils.getStackTrace(e) });
        }        
      }      
    }
    catch (MessageStoreException e)
    {
      // FFDC
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.runtime.LocalSubscriptionControl.getQueuedMessageIterator",
        "1:292:1.36",
        this);
        

      SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
        new Object[] { "com.ibm.ws.sib.processor.runtime.LocalSubscriptionControl.getQueuedMessageIterator", 
                       "1:298:1.36", 
                       SIMPUtils.getStackTrace(e) });
    }
    finally
    {
      if (cursor != null)
        cursor.finished();
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getQueuedMessageIterator");
      
    return new BasicSIMPIterator(messages.iterator());    
  }
  
  public SIMPQueuedMessageControllable getQueuedMessageByID(String ID)
      throws SIMPInvalidRuntimeIDException,
             SIMPControllableNotFoundException,
             SIMPException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getQueuedMessageByID", new Object[] { ID });

    long messageID = AbstractItem.NO_ID;
    try
    {
      messageID = Long.parseLong(ID);
    }
    catch(NumberFormatException e)
    {
//    FFDC
      FFDCFilter.processException(
         e,
         "com.ibm.ws.sib.processor.runtime.LocalQueuePoint.getQueuedMessageByID",
         "1:332:1.36",
         this);
      throw new SIMPInvalidRuntimeIDException(e);
    }
    
    SIMPMessage message = null;
    try
    {
      /* There are some ID's which are reserved to keep the reference of Standard ItemStreams (BaseDestinationHandler, PubSubItemStreamLink etc).
       * So we have to check if the AbstractItem returned is of type SIMPMessage and then cast it to that.
       * If we are not doing the check before casting, we may get ClassCastException 
       */
      AbstractItem abstractItem = consumerDispatcher.
              getReferenceStream().
              findById(messageID);
      if(abstractItem instanceof SIMPMessage)
        message = (SIMPMessage) abstractItem;
    }
    catch (MessageStoreException e)
    {
      FFDCFilter.processException(
          e,
          "com.ibm.ws.sib.processor.runtime.LocalSubscriptionControl.getQueuedMessageByID",
          "1:349:1.36", 
          this);
      SibTr.exception(tc, e);
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
        SibTr.exit(tc, "getQueuedMessageByID", e);
      throw new SIMPRuntimeOperationFailedException(e);
    }

    if(message == null)
    {
      throw new SIMPControllableNotFoundException(
        nls.getFormattedMessage(
            "MESSAGE_EXISTS_ERROR_CWSIP0572",
              new Object[] {
                ID,
                consumerDispatcher.getDestination().getName() },
                null));
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getQueuedMessageByID");
      
    return (SIMPQueuedMessageControllable) message.getControlAdapter();
  }
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.admin.Controllable#getId()
   */
  public String getId()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getId");

    String id = consumerDispatcher.getSubscriptionUuid().toString();
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())             
      SibTr.exit(tc, "getId", id);
    return id;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.admin.Controllable#getName()
   */
  public String getName()
  {    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getName");

    String name=consumerDispatcher.getConsumerDispatcherState().getSubscriberID();
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())             
      SibTr.exit(tc, "getName", name);
    return name;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPLocalSubscriptionControllable#getNumberOfQueuedMessages()
   */
  public long getNumberOfQueuedMessages()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getNumberOfQueuedMessages");

    long total = 0;
    try
    {
      total = referenceItemStream.getStatistics().getTotalItemCount();
    }
    catch (MessageStoreException e)
    {
      FFDCFilter.processException(
          e,
          "com.ibm.ws.sib.processor.runtime.LocalSubscriptionControl.getNumberOfQueuedMessages",
          "1:422:1.36", 
          this);
      
      SibTr.exception(tc, e);
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())             
      SibTr.exit(tc, "getNumberOfQueuedMessages", new Long(total));
    return total;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.AbstractControllable#checkValidControllable()
   */
  public void assertValidControllable() throws SIMPControllableNotFoundException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "checkValidControllable");
    
    if(referenceItemStream == null || !referenceItemStream.isInStore())
    {
      SIMPControllableNotFoundException finalE =
        new SIMPControllableNotFoundException(
          nls.getFormattedMessage(
            "INTERNAL_MESSAGING_ERROR_CWSIP0005",
            new Object[] {"LocalSubscriptionControl.assertValidControllable",
                          "1:448:1.36",
                          referenceItemStream},
            null));
            
      SibTr.exception(tc, finalE);
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exception(tc, finalE);
      throw finalE; 
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "checkValidControllable");
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.AbstractControllable#dereferenceControllable()
   */
  public void dereferenceControllable()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "dereferenceControllable");
    
    super.dereferenceControllable();
    referenceItemStream = null;
    consumerDispatcher = null;
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "dereferenceControllable");
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.ControlAdapter#registerControlAdapterAsMBean()
   */
  public void registerControlAdapterAsMBean()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
	  SibTr.entry(tc, "registerControlAdapterAsMBean");
    if (isRegistered() ) {
      // Don't register a 2nd time.
    } else {
      super.registerControlAdapterAsMBean();
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    SibTr.exit(tc, "registerControlAdapterAsMBean");
  }
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.ControlAdapter#runtimeEventOccurred(com.ibm.ws.sib.admin.RuntimeEvent)
   */
  public void runtimeEventOccurred(RuntimeEvent event)
  {
  }
}
