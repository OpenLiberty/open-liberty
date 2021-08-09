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
package com.ibm.ws.sib.processor.impl;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.sib.exception.SINotPossibleInCurrentConfigurationException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.sib.mfp.JsMessage;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.impl.interfaces.ConsumableKey;
import com.ibm.ws.sib.processor.impl.interfaces.ConsumerKey;
import com.ibm.ws.sib.processor.impl.interfaces.ConsumerKeyGroup;
import com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler;
import com.ibm.ws.sib.processor.impl.interfaces.JSConsumerKey;
import com.ibm.ws.sib.processor.impl.interfaces.JSKeyGroup;
import com.ibm.ws.sib.processor.impl.interfaces.SIMPMessage;
import com.ibm.ws.sib.transactions.TransactionCommon;
import com.ibm.ws.sib.utils.SIBUuid8;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.exception.SISessionDroppedException;

/**
 * This class implements those ConsumerManager functions that are
 * equivalent for all types of ConsumerManagers
 * 
 * TODO see what other methods can percolate up
 */
public abstract class AbstractConsumerManager
{ 
  //Trace
  private static final TraceComponent tc =
    SibTr.register(
      AbstractConsumerManager.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);

  // NLS for component
  private static final TraceNLS nls_cwsik =
    TraceNLS.getTraceNLS(SIMPConstants.CWSIK_RESOURCE_BUNDLE);

  
  /** The set of browsers attached to the consumer manager's destination */
  protected LinkedList<BrowserSessionImpl> _browsers = new LinkedList<BrowserSessionImpl>();
  
  protected BaseDestinationHandler _baseDestHandler;
  protected MessageProcessor _messageProcessor;
  
  // Set to true when we are closing due to a delete
  boolean closing = false;
  

  /** Unique identifier for the consumer manager */
  private SIBUuid8 _cmUuid = null;
  
  /** A map of all currently attached ordering consumerKey groups */
  private HashMap<OrderingContextImpl, JSKeyGroup> keyGroups = new HashMap<OrderingContextImpl, JSKeyGroup>();

  
  public AbstractConsumerManager(BaseDestinationHandler bdh)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "AbstractConsumerManager", bdh);
      
    _baseDestHandler = bdh;
    _messageProcessor = bdh.getMessageProcessor();
    
    // Instantiate a new uuid for this manager
    _cmUuid = new SIBUuid8();
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "AbstractConsumerManager", this);
  }
  
  /**
   * @param browserSession  The browser session to attach
   * @throws SIResourceException 
   */
  public void attachBrowser(BrowserSessionImpl browserSession)
    throws SINotPossibleInCurrentConfigurationException, SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(this, tc, "attachBrowser", browserSession);

    synchronized (_browsers)
    {
      if ((browserSession.getNamedDestination().isToBeDeleted()) ||
          (browserSession.getNamedDestination().isDeleted()))
      {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
          SibTr.exit(this, tc, "attachBrowser", "SINotPossibleInCurrentConfigurationException");
        throw new SINotPossibleInCurrentConfigurationException(
          nls_cwsik.getFormattedMessage(
            "DELIVERY_ERROR_SIRC_32",  // DESTINATION_DELETED_ERROR_CWSIP0421
            new Object[] { _baseDestHandler.getName(),
                           _messageProcessor.getMessagingEngineName()},
            null));
      }

      _browsers.add(browserSession);

    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(this, tc, "attachBrowser");
  }
  

  /**
   * @param browserSession  The browser session to detach
   */
  public void detachBrowser(BrowserSessionImpl browserSession)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(this, tc, "detachBrowser", browserSession);

    synchronized (_browsers)
    {
      if(!closing) // To avoid concurrent modification 
        _browsers.remove(browserSession);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(this, tc, "detachBrowser");
  }
  
  /**
   * Method closeBrowsersDestinationDeleted.
   * <p>Close and detach all browser sessions.</p>
   * @param destinationHandler = The destination being deleted.
   */
  protected void closeBrowsersDestinationDeleted(DestinationHandler destinationHandler)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(this, tc, "closeBrowsersDestinationDeleted", destinationHandler);

    synchronized (_browsers)
    {
      Iterator<BrowserSessionImpl> i = _browsers.iterator();
      closing = true;
      
      while (i.hasNext())
      {
        // Close the producer session, indicating that it has been closed due to delete
        boolean closedOk = i.next()._closeBrowserDestinationDeleted(destinationHandler);

        // Remove the producer session from the Producers List
        if (closedOk) 
          i.remove();
      }
      
      closing = false;
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(this, tc, "closeBrowsersDestinationDeleted");
  }
       
  /**
   * Get the MessageProcessor object to which this CD belongs
   *
   * @return the MessageProcessor instance to which this CD belongs
   */
  public MessageProcessor getMessageProcessor()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getMessageProcessor");
      SibTr.exit(tc, "getMessageProcessor", _messageProcessor);
    }

    return _messageProcessor;
  }
  
  /**
   * Get the BaseDestinationHandler object to which this CD belongs
   *
   * @return the BaseDestinationHandler object to which this CD belongs
   */
  public BaseDestinationHandler getDestination()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getDestination");
      SibTr.exit(tc, "getDestination", _baseDestHandler);
    }

    return _baseDestHandler;
  }
  
  /**
   * Get the Uuid for this consumer manager.
   * 
   * @return
   */
  public SIBUuid8 getUuid()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getUuid");
      SibTr.exit(tc, "getUuid", _cmUuid);
    }

    return _cmUuid;
  }  
  
  public void setCurrentTransaction(SIMPMessage msg, boolean isInDoubtOnRemoteConsumer) 
  {
    // no-op
  }
  
  public void setCurrentTransaction(TransactionCommon transaction, JSLockedMessageEnumeration lme) 
  {
    // no-op 
  }
  
  public long newReadyConsumer(JSConsumerKey key, boolean specific) {
    // TODO Auto-generated method stub
    return 0;
  }

  /**
   * Add the supplied consumerKey to a keyGroup, create the keyGroup if required
   */
  public ConsumerKeyGroup joinKeyGroup(ConsumerKey consumerKey, OrderingContextImpl orderingGroup) 
  throws SIResourceException, SISessionDroppedException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "joinKeyGroup", new Object[] {consumerKey, orderingGroup});

    JSKeyGroup keyGroup = null;

    // We need to hold the keyGroup lock while we add a member (to prevent the
    // member count (and therefore the ready state) changing under an existing
    // member's feet). We also need the readyConsumerPointLock while we're adding
    // a new keyGroup to the hashmap (as users of different groups may attempt to
    // join at the same time). But we cannot hold the readyConsumerPointLock while we
    // do this as it is below the keyGroup lock in the hierarchy. Instead we lock
    // the orderingGroup as a whole, this prevents anyone else trying to come along
    // and join a new keyGroup (and possibly leave it - causing it to be removed)
    // before we manage to add ourselves to it.
    synchronized(orderingGroup)
    {
      synchronized(_baseDestHandler.getReadyConsumerPointLock())
      {
        // See if a keyGroup (specific to a CD) for the supplied orderingGroup is
        // currently attached to this CD
        if((keyGroup = keyGroups.get(orderingGroup)) == null)
        {
          // If not, create one
          // We pass the consumerSet as parameter. This will only be non-null
          // where XD has registered for message classification
          JSConsumerSet consumerSet = null;
          if(consumerKey instanceof ConsumableKey)
          {
            consumerSet = ((ConsumableKey)consumerKey).getConsumerSet();
            keyGroup = createConsumerKeyGroup(consumerSet);
            if (keyGroup!=null)
              keyGroups.put(orderingGroup, keyGroup);
          }
        }
      }

      //Add this key to the keyGroup
      if (keyGroup!=null)
        ((ConsumableKey) consumerKey).joinKeyGroup(keyGroup);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "joinKeyGroup", keyGroup);

    return keyGroup;
  }

  protected abstract JSKeyGroup createConsumerKeyGroup(JSConsumerSet consumerSet);

  /**
   * Remove the keyGroup from the CD, this occurs when the last member is
   * removed from the group
   */
  public void removeKeyGroup(LocalQPConsumerKeyGroup keyGroup)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "removeKeyGroup", keyGroup);

    // Take the lock to prevent another thread from trying to add a member to
    // the group while we're removing it
    synchronized(_baseDestHandler.getReadyConsumerPointLock())
    {
      // Need to find the correct object to remove.
      Iterator keys = keyGroups.keySet().iterator();
      while (keys.hasNext())
      {
        Object key = keys.next();
        if (keyGroups.get(key).equals(keyGroup))
          keys.remove();
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "removeKeyGroup");
  }


  public void removeReadyConsumer(JSConsumerKey key, boolean specific) {
    // TODO Auto-generated method stub

  }
  
  public void setReadyForUse() {
    // TODO Auto-generated method stub

  }
}
