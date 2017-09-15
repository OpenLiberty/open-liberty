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

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.admin.BaseDestinationDefinition;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.impl.exceptions.InvalidOperationException;
import com.ibm.ws.sib.processor.impl.indexes.SubscriptionTypeFilter;
import com.ibm.ws.sib.processor.impl.interfaces.ControllableSubscription;
import com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler;
import com.ibm.ws.sib.processor.impl.interfaces.InputHandler;
import com.ibm.ws.sib.processor.impl.interfaces.LocalizationPoint;
import com.ibm.ws.sib.processor.impl.interfaces.ProducerInputHandler;
import com.ibm.ws.sib.processor.impl.store.SIMPTransactionManager;
import com.ibm.ws.sib.processor.impl.store.itemstreams.SIMPItemStream;
import com.ibm.ws.sib.processor.runtime.SIMPIterator;
import com.ibm.ws.sib.processor.runtime.impl.ControlAdapter;
import com.ibm.ws.sib.utils.SIBUuid12;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.DestinationType;

/**
 * @author millwood
 */ 
public abstract class AbstractDestinationHandler extends SIMPItemStream 
  implements DestinationHandler
{
  /** 
   * Trace for the component
   */
  private static final TraceComponent tc =
    SibTr.register(
      AbstractDestinationHandler.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);
      
  
  
  /**
   * NLS for component
   */
  static final TraceNLS nls =
    TraceNLS.getTraceNLS(SIMPConstants.RESOURCE_BUNDLE);
  
  SIMPTransactionManager txManager;
  ControlAdapter controlAdapter;

  MessageProcessor messageProcessor;
  DestinationManager destinationManager;

  // The name of the bus on which the destination resides
  private String busName = null;
  
  /** Support for destination access control */
  protected AccessChecker accessChecker;
  protected boolean isBusSecure = false;
  private List<DestinationHandler> aliasesThatTargetThisDest = null;
  
  /**
   * Has the destination already been deleted?
   */
  private boolean isDeleted = false;
  
  /**
   * A destination maintains all the ItemStreams which it hands out to 
   * InputHandlers and OutputHandlers.
   * 
   * For point to point destinations.
   *   Non-cloned destination.
   *     The PtoPInputHandler gets messageItemStream.
   *     The ConsumerDispatcher gets the same messageItemStream.
   *   Cloned destination
   *     The PtoPInputHandler gets a non-persistent messageItemStream.
   *     The ConsumerDispatchers get transmissions ItemStreams or a local
   *       persistent ItemStream.
   *     NOTE: This is not yet implemented.
   * 
   * For pub/sub destinations.
   *   The PubSubInputHandler is passed the messageItemStream.
   *   Each ConsumerDispatcher (one per subscriber) is passed a ReferenceStream
   *     that refers to Items in the messageItemStream.
   * 
   * In all cases there is only one InputHandler per destination.
   */
  ProducerInputHandler inputHandler = null;
  
  /** 
   * Is sendAllowed specified on a targeted foreign bus.
   * A 3 way boolean spec. null if it doesn't target a foreign bus. If non-null, then
   * may be TRUE or FALSE depending on the setting on the Foreign Bus Definition.
   */ 
  protected Boolean _sendAllowedOnTargetForeignBus = null;
  
  /**
   * Warm start constructor invoked by the Message Store.
   */
  public AbstractDestinationHandler()
  {
    super();
      
    // This space intentionally left blank.   
      
  }

  public AbstractDestinationHandler(
    MessageProcessor messageProcessor,
    String busName)
  {
    super();
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "AbstractDestinationHandler", 
        new Object[] { messageProcessor, busName }); 
      
    initializeNonPersistent(messageProcessor);

    // Override the default local value set by initializeNonPersistent with
    // the real one
    this.busName = busName;
                
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "AbstractDestinationHandler", this);
  }
  
  protected void reconstitute(MessageProcessor processor)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "reconstitute", processor);
    initializeNonPersistent(processor);
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "reconstitute");
  }
  
  protected void initializeNonPersistent(MessageProcessor processor)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "initializeNonPersistent", processor);
    this.messageProcessor = processor;
    txManager = messageProcessor.getTXManager();
    destinationManager = messageProcessor.getDestinationManager();
    
    // Any destination that is reconstituted from the MS will be on the local bus
    busName = messageProcessor.getMessagingEngineBus();
   
    // Set the security enablement flag
    isBusSecure = messageProcessor.isBusSecure();
    if(isBusSecure)
      accessChecker = messageProcessor.getAccessChecker();
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "initializeNonPersistent");
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#getDestinationManager()
   */
  public DestinationManager getDestinationManager()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getDestinationManager");
      SibTr.exit(tc, "getDestinationManager", destinationManager);
    }

    return destinationManager;
  }  

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#getMessageProcessor()
   */
  public MessageProcessor getMessageProcessor()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getMessageProcessor");
      SibTr.exit(tc, "getMessageProcessor", messageProcessor);
    }
    return messageProcessor;
  }

  public SIMPTransactionManager getTransactionManager() 
  {
    return this.txManager;
  }
  


  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#getTxManager()
   */
  public SIMPTransactionManager getTxManager()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getTxManager");
      SibTr.exit(tc, "getTxManager", txManager);
    }  
    return txManager;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#closeProducers()
   */
  public void closeProducers()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "closeProducers");

    //Tell any destinations that target this destination to close their producers
    if (aliasesThatTargetThisDest != null)
    {
      synchronized(aliasesThatTargetThisDest)
      {
        Iterator i = aliasesThatTargetThisDest.iterator();
        while (i.hasNext())
        {
          AbstractAliasDestinationHandler abstractAliasDestinationHandler = 
            (AbstractAliasDestinationHandler) i.next();
          abstractAliasDestinationHandler.closeProducers();
        }
      }
    }
    
    // Close all producers
    ProducerInputHandler inputHandler = (ProducerInputHandler) getInputHandler();
    
    if (inputHandler != null)
      inputHandler.closeProducersDestinationDeleted();

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "closeProducers");

    return;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#closeConsumers()
   */
  public void closeConsumers() throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "closeConsumers");

    //Tell any destinations that target this destination to close their consumers
    if (aliasesThatTargetThisDest != null)
    {
      synchronized(aliasesThatTargetThisDest)
      {
        Iterator i = aliasesThatTargetThisDest.iterator();
        while (i.hasNext())
        {
          AbstractAliasDestinationHandler abstractAliasDestinationHandler = 
            (AbstractAliasDestinationHandler) i.next();
          abstractAliasDestinationHandler.closeConsumers();
        }
      }
    }
    
    // Close all consumers
    if (isPubSub())
    {
      SubscriptionTypeFilter filter = new SubscriptionTypeFilter();  
      filter.LOCAL = Boolean.TRUE;  
      SIMPIterator itr = getSubscriptionIndex().iterator(filter);  
      while(itr.hasNext())  
      {  
        ControllableSubscription subscription = (ControllableSubscription)itr.next(); 
        ConsumerDispatcher cd = (ConsumerDispatcher)subscription.getOutputHandler();

        //Look for subscriptions made through this destination (as opposed to
        //through an alias)
        if (cd.getConsumerDispatcherState().getTopicSpaceUuid().equals(getUuid()))
        {
          //Close the consumers of the subscription  
          cd.closeAllConsumersForDelete(this);
        }
      }

      // Remove the ProxyHandlers attached to this destination
      // sanjay liberty change
      //messageProcessor.getProxyHandler().topicSpaceDeletedEvent(this);
      
      //Delete any durable subscriptions if they are made through an alias
      //in-case the underlying topicspace is not being deleted
      if (isAlias())
      {
        itr = getSubscriptionIndex().iterator(filter);  
        while(itr.hasNext())  
        {  
          ControllableSubscription subscription = (ControllableSubscription)itr.next(); 
          ConsumerDispatcher cd = (ConsumerDispatcher)subscription.getOutputHandler();
          ConsumerDispatcherState subState = cd.getConsumerDispatcherState();

          //Look for subscriptions made through this alias
          if ((subState.getTopicSpaceUuid().equals(getUuid())) &&
              (cd.isDurable()))
          {
            try {
              deleteDurableSubscription(subState.getSubscriberID(), subState.getDurableHome());
            } catch (SIException e) 
            {
              FFDCFilter.processException(
                  e,
                  "com.ibm.ws.sib.processor.impl.AbstractDestinationHandler.closeConsumers",
                  "1:347:1.57",
                  this);

              SibTr.exception(tc, e);
            }
          }
        } 
        itr.finished();
        
      }

    }
    else
    {
      LocalizationPoint ptoPMessageItemStream = getQueuePoint(messageProcessor.getMessagingEngineUuid());
      if (ptoPMessageItemStream != null)
      {
        ConsumerDispatcher consumerManager =
          (ConsumerDispatcher)ptoPMessageItemStream.getConsumerManager();
        consumerManager.closeAllConsumersForDelete(this);
      }
    }
    

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "closeConsumers");

    return;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#isPubSub()
   */
  public boolean isPubSub()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "isPubSub");
      
    boolean isPubSub = (getDestinationType() == DestinationType.TOPICSPACE);
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "isPubSub", new Boolean(isPubSub));

    return isPubSub;
  }  

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#isLink()
   */
  public boolean isLink()
  {
    return false;
  }
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#isForeignBus()
   */
  public boolean isForeignBus()
  {
    return false;
  }
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#isLink()
   */
  public boolean isMQLink()
  {
    return false;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#getInputHandler()
   */
  public InputHandler getInputHandler()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getInputHandler");
      SibTr.exit(tc, "getInputHandler", inputHandler);
    }
    
    return inputHandler;
  }
  
  /**
   * Sets the inputHandler for this destination.
   * @param inputHandler
   */
  public void setInputHandler(ProducerInputHandler inputHandler)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "setInputHandler", inputHandler);
    this.inputHandler = inputHandler;
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "setInputHandler");
  }
    
  /**
   * Sets the bus name for this destination.
   * @param busName
   */
  void setBus(String busName)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "setBus", busName);
    this.busName = busName;
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "setBus");
  }
  
  public String getBus()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getBus");
      SibTr.exit(tc, "getBus", busName);
    }
          
    return busName;
  }   
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#getContextProperty(java.lang.String)
   */
  public Object getContextValue(String keyName)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getContextValue", keyName);
      
    Map context = getDefinition().getDestinationContext();
    
    Object property = context.get(keyName);
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getContextValue", property);
      
    return property; 
  }  

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#getDescription()
   */
  public String getDescription()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getDescription");
      
    String desc = getDefinition().getDescription();
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getDescription", desc);   
      
    return desc;                           
  } 
  
  public ControlAdapter getControlAdapter()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getControlAdapter");
      SibTr.exit(tc, "getControlAdapter", controlAdapter);
    }        
    return controlAdapter;  
  }
  
  public void dereferenceControlAdapter()
  {
    controlAdapter.dereferenceControllable();
    controlAdapter = null;
  }
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#getName()
   */
  public String getName()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getName");
    
    String name = null;
    BaseDestinationDefinition def = getDefinition();
    if (def != null)
      name = getDefinition().getName();
  
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getName", name);
    
    return name; 
  } 
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#getUuid()
   */
  public SIBUuid12 getUuid()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getUuid");
    
    SIBUuid12 uuid = getDefinition().getUUID();
  
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getUuid", uuid);
    
    return uuid;
  }
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#getBaseUuid()
   */
  public SIBUuid12 getBaseUuid()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getBaseUuid");
    
    SIBUuid12 uuid = getDefinition().getUUID();
  
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getBaseUuid", uuid);
    
    return uuid;
  }
  
  /** To string representation for the destination */
  public String toString()
  {
    BaseDestinationDefinition def = getDefinition();
    
    if (def != null)    
      return "Destination (" + this.hashCode() + ") " + getName() + " : "+ getUuid();
     
    return "Destination setup incomplete (" + this.hashCode() + ") Link:" + isLink() + " Foreign Bus: " + isForeignBus();
  }
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#addTargettingAlias(com.ibm.ws.sib.processor.impl.AliasDestinationHandler)
   */
  public void addTargettingAlias(DestinationHandler aliasDestinationHandler)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "addTargettingAlias", aliasDestinationHandler);
    
    if (aliasesThatTargetThisDest == null)
    {
      aliasesThatTargetThisDest = new java.util.ArrayList<DestinationHandler>();
    }
   
    synchronized(aliasesThatTargetThisDest)
    { 
      aliasesThatTargetThisDest.add(aliasDestinationHandler);
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "addTargettingAlias");
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#removeTargettingAlias(com.ibm.ws.sib.processor.impl.AliasDestinationHandler)
   */
  public void removeTargettingAlias(DestinationHandler aliasDestinationHandler)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "removeTargettingAlias", aliasDestinationHandler);
    
    synchronized(aliasesThatTargetThisDest)
    { 
      aliasesThatTargetThisDest.remove(aliasDestinationHandler);
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "removeTargettingAlias");
  }
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#deleteTargettingAliases
   */
  public void deleteTargettingAliases()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "deleteTargettingAliases");

    //Delete any aliases that target this destination
    if (aliasesThatTargetThisDest != null)
    {
      DynamicConfigManager dcm = messageProcessor.getDynamicConfigManager();
      
      synchronized(aliasesThatTargetThisDest)
      {
        Iterator i = aliasesThatTargetThisDest.iterator();
        while (i.hasNext())
        {
          AbstractAliasDestinationHandler abstractAliasDestinationHandler = 
            (AbstractAliasDestinationHandler) i.next();
          abstractAliasDestinationHandler.deleteTargettingAliases();
          
          //Delete the alias/foreign destination
          dcm.deleteAbstractAliasDestinationHandler(abstractAliasDestinationHandler);
          
          i.remove();
        }
      }
    }
    
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "deleteTargettingAliases");

    return;
  }
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#isDeleted()
   */
  public boolean isDeleted()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "isDeleted");
      SibTr.exit(tc, "isDeleted", new Boolean(isDeleted));
    }
    return isDeleted;
  }
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#setDeleted()
   */
  public void setDeleted()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "setDeleted");
      SibTr.exit(tc, "setDeleted");
    }
    
    isDeleted = true;
  }

  /**
   * Set the Foreign Bus Level sendAllowed flag
   *  
   * @param sendAllowed
   */
  public void setForeignBusSendAllowed(boolean sendAllowed)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "setForeignBusSendAllowed", Boolean.valueOf(sendAllowed));
    }
    
    // Set the flag on this handler
    _sendAllowedOnTargetForeignBus = Boolean.valueOf(sendAllowed);
    
    // Tell any destinations that target this destination to refresh their
    // sendAllowed setting
    if (aliasesThatTargetThisDest != null)
    {
      synchronized(aliasesThatTargetThisDest)
      {
        Iterator i = aliasesThatTargetThisDest.iterator();
        while (i.hasNext())
        {
          AbstractAliasDestinationHandler abstractAliasDestinationHandler = 
            (AbstractAliasDestinationHandler) i.next();
          abstractAliasDestinationHandler.setForeignBusSendAllowed(sendAllowed);
        }
      }
    }    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.exit(tc, "setForeignBusSendAllowed");
    }
  }  
  
  /**
   * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#reset()
   * 
   * If a child destination wants to implement reset, it should override this
   * method.
   */
  public void reset() throws InvalidOperationException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "reset");

    InvalidOperationException e = new InvalidOperationException(
      nls.getFormattedMessage(
        "OPERATION_IS_INVALID_ERROR_CWSIP0121",
        new Object[] { getName(), getBus() },
      null));

    SibTr.exception(tc, e);
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())    
      SibTr.exit(tc, "reset", e);
      
    throw e;    
  } 


  /**
   * <p>Any aliases that inherit the receive allowed attribute from this
   * destination must also be notified that it has changed.</p>
   * @param receiveAllowedValue
   */
  public void notifyTargettingAliasesReceiveAllowed()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "notifyTargettingAliasesReceiveAllowed");

    //Tell any destinations that target this destination to close their producers
    if (aliasesThatTargetThisDest != null)
    {
      synchronized(aliasesThatTargetThisDest)
      {
        Iterator i = aliasesThatTargetThisDest.iterator();

        while (i.hasNext())
        {
          AbstractAliasDestinationHandler abstractAliasDestinationHandler = 
            (AbstractAliasDestinationHandler) i.next();

          abstractAliasDestinationHandler.notifyReceiveAllowed(abstractAliasDestinationHandler);
        }
      }
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "notifyTargettingAliasesReceiveAllowed");

    return;
  }
  
  
  /**
   * @return
   */
  public Boolean getSendAllowedOnTargetForeignBus()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getSendAllowedOnTargetForeignBus");
      SibTr.exit(tc, "getSendAllowedOnTargetForeignBus", _sendAllowedOnTargetForeignBus);
    }
    
    return _sendAllowedOnTargetForeignBus;
  }
}
