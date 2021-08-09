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
 
package com.ibm.ws.sib.processor.impl;

import java.io.IOException;
import java.util.HashMap;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.Reliability;
import com.ibm.ws.sib.admin.BaseDestinationDefinition;
import com.ibm.ws.sib.admin.DestinationDefinition;
import com.ibm.ws.sib.admin.internal.JsAdminFactory;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.impl.indexes.SubscriptionTypeFilter;
import com.ibm.ws.sib.processor.impl.interfaces.ControllableSubscription;
import com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler;
import com.ibm.ws.sib.processor.runtime.SIMPIterator;
import com.ibm.ws.sib.utils.SIBUuid12;
import com.ibm.ws.sib.utils.ras.FormattedWriter;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.DestinationType;

/**
 * @author caseyj
 *
 * At the moment, this abstract class provides a place for getters of
 * destination definition properties, so that they don't clutter up child
 * classes.  Think carefully before adding anything other than definition
 * property getters to this class.
 */
abstract class AbstractBaseDestinationHandler 
  extends AbstractDestinationHandler
{  
  /** Trace for the component */
  private static final TraceComponent tc =
    SibTr.register(
      AbstractBaseDestinationHandler.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);
      
    
  /** The definition for this base (main) destination handler. */
  protected DestinationDefinition definition;
  
  /** The cached receive exclusive flag - defaults to true
    * (volatile to ensure an up-to-date view of the current setting when enquired;
    * a synchronize can cause a deadlock with the Localisation.Manager._xmitQueuePoints
    * object)
    */
  private volatile boolean _isReceiveAllowed = true;
  
  /** Cache for the threshold rather than getting from the definition */
  protected int _maxFailedDeliveries = -1;

  /** Cache that holds whether the redelivery count should be persisted or not */
  protected boolean _isRedeliveryCountPersisted = false;
  
  /** Cache for the retry interval rather than getting from the definition */
  protected long _blockedRetryInterval = -1;
  
  /**
   * Warm start constructor invoked by the Message Store.
   */
  public AbstractBaseDestinationHandler()
  {
    super();
    // This space intentionally left blank.   
  }
  
  /**
   * @param messageProcessor
   * @param destinationDefinition
   * @param busName
   */
  public AbstractBaseDestinationHandler(
    MessageProcessor messageProcessor,
    DestinationDefinition destinationDefinition,
    String busName)
  {      
    super(messageProcessor, busName);    
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "AbstractBaseDestinationHandler", 
        new Object[] { messageProcessor, destinationDefinition, busName });
            
    definition = destinationDefinition;     
    
    // SIB0115.mp.4 - BlockedRetryInterval can now be set on a Destination, thus overriding the ME setting.
    if(definition !=null)
    {
      _blockedRetryInterval = definition.getBlockedRetryTimeout();
      if(_blockedRetryInterval < 0)
      {
        // BlockedRetryInterval is unset on the destination, retrieve the ME-wide value
        _blockedRetryInterval = messageProcessor.getCustomProperties().get_blocked_retry_timeout();
      }
    }
    
    // If a definition is found then set the receive allowed to be the value
    // in the definition
    if (definition != null)
      _isReceiveAllowed = destinationDefinition.isReceiveAllowed();
    // otherwise if there is no definition set the receive allowed to true
    else
      _isReceiveAllowed = true;
      
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "AbstractBaseDestinationHandler", this);
  }  
  
  /**
   * Add data to given object for message store persistence.
   * 
   * @param hm
   */
  public void addPersistentData(HashMap hm)
  {  
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "addPersistentData", hm); 
           
    hm.put("name", definition.getName()); 
    hm.put("uuid",definition.getUUID().toByteArray());
    hm.put("maxReliability", 
      Integer.valueOf(definition.getMaxReliability().toInt())); 
    hm.put("defaultReliability",
      Integer.valueOf(definition.getDefaultReliability().toInt())); 
    hm.put("destinationType",
      Integer.valueOf(definition.getDestinationType().toInt()));
      
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "addPersistentData"); 
  }
   
 /* (non-Javadoc)
  * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#getAlterationTime()
  */
  public long getAlterationTime()
  {
    return definition.getAlterationTime();
  }
    
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#getDefaultPriority()
   */
  public int getDefaultPriority()
  {
    return definition.getDefaultPriority();
  }
    
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#getDefaultReliability()
   */
  public Reliability getDefaultReliability()
  {
    return definition.getDefaultReliability();
  }  
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#getDestinationType()
   */
  public DestinationType getDestinationType()
  {
    return definition.getDestinationType();
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#getExceptionDestination()
   */
  public String getExceptionDestination()
  {    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getExceptionDestination"); 
    
    String exDest = definition.getExceptionDestination();

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getExceptionDestination", exDest); 
    
    return exDest;
  }  
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#getExceptionDiscardReliability()
   */
  public Reliability getExceptionDiscardReliability()
  {    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getExceptionDiscardReliability"); 
    
    Reliability exDiscardRel = definition.getExceptionDiscardReliability();

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getExceptionDiscardReliability", exDiscardRel); 
    
    return exDiscardRel;
  }  
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#getMaxFailedDeliveries()
   */
  public int getMaxFailedDeliveries()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getMaxFailedDeliveries"); 
    
    if (_maxFailedDeliveries < 0)
      _maxFailedDeliveries = definition.getMaxFailedDeliveries();
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getMaxFailedDeliveries", Integer.valueOf(_maxFailedDeliveries));
    
    return _maxFailedDeliveries;
  }

  public boolean isRedeliveryCountPersisted()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "isRedeliveryCountPersisted"); 

    _isRedeliveryCountPersisted = definition.isRedeliveryCountPersisted();
	    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "isRedeliveryCountPersisted", Boolean.valueOf(_isRedeliveryCountPersisted));

    return _isRedeliveryCountPersisted;

  }
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#getMaxFailedDeliveries()
   */
  public long getBlockedRetryInterval()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getBlockedRetryInterval"); 
    
    if (_blockedRetryInterval < 0)
      _blockedRetryInterval = definition.getBlockedRetryTimeout();
    
    // If BlockedRetryInterval is unset on the destination, retrieve the ME-wide value
    if (_blockedRetryInterval < 0)
      _blockedRetryInterval = messageProcessor.getCustomProperties().get_blocked_retry_timeout();
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getBlockedRetryInterval", Long.valueOf(_blockedRetryInterval));
    
    return _blockedRetryInterval;
  }
  
  
    
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#getMaxReliability()
   */
  public Reliability getMaxReliability()
  {
    return definition.getMaxReliability();
  }
    
    
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#getDefinition()
   */
  public BaseDestinationDefinition getDefinition()
  {
    return definition;
  }  
    
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#isAlias()
   */
  public boolean isAlias()
  {
    return false;
  }  
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#isForeign()
   */
  public boolean isForeign()
  {
    return false;
  }
    
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#isReceiveAllowed()
   */
  public boolean isReceiveAllowed()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {    
      SibTr.entry(tc, "isReceiveAllowed");  
      SibTr.exit(tc, "isReceiveAllowed", Boolean.valueOf(_isReceiveAllowed));
    }       
    return _isReceiveAllowed;                       
  }
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#isSendAllowed()
   */
  public boolean isSendAllowed()
  {    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "isSendAllowed");
    
    boolean isSendAllowed = definition.isSendAllowed();
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "isSendAllowed", Boolean.valueOf(isSendAllowed));
     
    return isSendAllowed;                        
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#isOverrideOfQOSByProducerAllowed()
   */
  public boolean isOverrideOfQOSByProducerAllowed()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "isOverrideOfQOSByProducerAllowed");
    
    boolean isOverrideOfQOSByProducerAllowed = definition.isOverrideOfQOSByProducerAllowed();
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "isOverrideOfQOSByProducerAllowed", Boolean.valueOf(isOverrideOfQOSByProducerAllowed));
     
    return isOverrideOfQOSByProducerAllowed; 
        
  }    
    
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#isReceiveExclusive()
   */
  public boolean isReceiveExclusive()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "isReceiveExclusive");
    
    boolean isReceiveExclusive = definition.isReceiveExclusive();
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "isReceiveExclusive", Boolean.valueOf(isReceiveExclusive));
     
    return isReceiveExclusive; 
  }

  public void restorePersistentData(HashMap hm)
    throws Exception
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "restorePersistentData", hm); 
          
    // Recreate definition object
    int destType = ((Integer)hm.get("destinationType")).intValue();
    DestinationType destinationType 
      = DestinationType.getDestinationType(destType);
    String name = (String)hm.get("name");

    JsAdminFactory jsAdminFactory = JsAdminFactory.getInstance();
    definition = jsAdminFactory.createDestinationDefinition(
      destinationType, name); 
            
    // Restore uuid
    definition.setUUID(new SIBUuid12((byte[])hm.get("uuid")));    
      
    // Restore reliabilities
    int maxReliability = ((Integer)hm.get("maxReliability")).intValue();
    definition.setMaxReliability(Reliability.getReliability(maxReliability));
      
    int defaultReliability = ((Integer)hm.get("defaultReliability")).intValue();
    definition.setDefaultReliability(Reliability.getReliability(
      defaultReliability));   
      
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "restorePersistentData"); 
  }  
    
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#updateDefinition(com.ibm.ws.sib.admin.DestinationDefinition)
   */
  public void updateDefinition(BaseDestinationDefinition destinationDefinition)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "updateDefinition", new Object[] {destinationDefinition});
      
    DestinationDefinition oldDefinition = definition;
    DestinationDefinition newDefinition = (DestinationDefinition) destinationDefinition;
      
    // Reset values to force a reload on first reference
    _maxFailedDeliveries = -1;
    _blockedRetryInterval = -1;
    
    if (!isPubSub() && ((oldDefinition == null) ||
        (oldDefinition.isReceiveExclusive() != newDefinition.isReceiveExclusive())))
    {
      // notify the AnycastOutputHandler, *before* throwing off all consumers. 
      // If this is not done, AnycastOutputHandler could
      // create a new consumer before knowing the change in the receiveExclusive value which is not good!
      notifyAOHReceiveExclusiveChange(newDefinition.isReceiveExclusive());

      //throw off all consumers attached through this destination
      ConsumerDispatcher cm = (ConsumerDispatcher)getLocalPtoPConsumerManager();
      if (cm != null)
      {
        cm.closeAllConsumersForReceiveExclusive();
      }
      
      // notify the RME RemoteConsumerDispatchers that the receiveExlusive value has changed
      notifyRCDReceiveExclusiveChange(newDefinition.isReceiveExclusive());
    }
    
    // definition must be updated before notifying consumer dispatcher(s) of update 
    definition = (DestinationDefinition) destinationDefinition;           

    if ((oldDefinition == null) ||
        (oldDefinition.isReceiveAllowed() != newDefinition.isReceiveAllowed()))
    {    
      _isReceiveAllowed = newDefinition.isReceiveAllowed();
      
      notifyReceiveAllowed(this);
      
      //Tell any aliases that inherit the receive allowed value from this
      //destination that the value has changed.
      notifyTargettingAliasesReceiveAllowed();

    }    
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "updateDefinition");
  }  

  /*
   * Subclasses should implement this method to notify the change in receiveExclusive
   * to the DME side of the remote get components.
   */
  protected abstract void notifyAOHReceiveExclusiveChange(boolean newValue);
  
  /*
   * Subclasses should implement this method to notify the change in receiveExclusive
   * to the RemoteConsumerDispatchers on the RME side.
   */
  protected abstract void notifyRCDReceiveExclusiveChange(boolean newValue);
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#notifyReceiveAllowed(com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler)
   */
  public void notifyReceiveAllowed(DestinationHandler destinationHandler)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "notifyReceiveAllowed", new Object[] {destinationHandler});
      
    if(isPubSub())
    {
      // Notify consumers on this localization 
      SubscriptionTypeFilter filter = new SubscriptionTypeFilter();  
      filter.LOCAL = Boolean.TRUE;  
      SIMPIterator itr = getSubscriptionIndex().iterator(filter);  
      while(itr.hasNext())  
      {  
        ControllableSubscription subscription = (ControllableSubscription)itr.next(); 
        ConsumerDispatcher cd = (ConsumerDispatcher)subscription.getOutputHandler();
        if(cd != null) 
          cd.notifyReceiveAllowed(destinationHandler); 
      } 
      itr.finished(); 
    }
    else
    {
      //tell the local consumer dispatcher that this destinations receiveAllowed has changed
      ConsumerDispatcher cm = (ConsumerDispatcher)getLocalPtoPConsumerManager();
      if (cm != null)
      {
        cm.notifyReceiveAllowed(destinationHandler);
      }
    }

    //tell the any RME remote consumer dispatchers that this destinations receiveAllowed has changed
    notifyReceiveAllowedRCD(destinationHandler);
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "notifyReceiveAllowed");
  }    
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.msgstore.AbstractItem#xmlWriteOn(com.ibm.ws.sib.msgstore.FormattedWriter)
   */
  public void xmlWriteOn(FormattedWriter writer) throws IOException  
  {
    writer.newLine();
    writer.taggedValue("name", definition.getName());
    writer.newLine();
    writer.taggedValue("uuid", definition.getUUID());
  }  
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#isTopicAccessCheckRequired()
   */
  public boolean isTopicAccessCheckRequired()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "isTopicAccessCheckRequired");
    // Look to the underlying definition
    boolean ret = definition.isTopicAccessCheckRequired();
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "isTopicAccessCheckRequired", Boolean.valueOf(ret));
    return ret;
  }   
   
}
