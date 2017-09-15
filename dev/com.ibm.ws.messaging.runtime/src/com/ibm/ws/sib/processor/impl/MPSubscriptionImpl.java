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

import java.util.Map;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.msgstore.MessageStoreException;
import com.ibm.ws.sib.msgstore.transactions.Transaction;
import com.ibm.ws.sib.processor.MPSubscription;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.SelectionCriteria;
import com.ibm.wsspi.sib.core.exception.SIDiscriminatorSyntaxException;
import com.ibm.wsspi.sib.core.exception.SISelectorSyntaxException;


 /**
  * @author Rachel Norris 
  *  
  */
  public class MPSubscriptionImpl implements MPSubscription
  {
    /** Trace for the component */
    private static final TraceComponent tc =
      SibTr.register(
        MPSubscriptionImpl.class,
        SIMPConstants.MP_TRACE_GROUP,
        SIMPConstants.RESOURCE_BUNDLE);
    
    private ConsumerDispatcher _consumerDispatcher=null;
    private MessageProcessor _messageProcessor = null;
    
    /**
     * <p>Create a new instance of an MPSubscription</p>
     * 
     * @param consumerDispatcher
     * @param consumerDispatcherState
     * @param durSubStream  
     * @param messageProcessor
     * @param destinationHandler
     */
    public MPSubscriptionImpl( ConsumerDispatcher consumerDispatcher,
                               MessageProcessor   messageProcessor )
    {
      if (tc.isEntryEnabled()) 
        SibTr.entry(tc, "MPSubscriptionImpl", 
          new Object[]{consumerDispatcher, 
                       messageProcessor});
                       
      _consumerDispatcher = consumerDispatcher;
      _messageProcessor = messageProcessor;
           
      if (tc.isEntryEnabled())
        SibTr.exit(tc, "MPSubscriptionImpl", this); 
    }
    
    /**
     * Add an additional selection criteria to the to the subscription
     * Duplicate selection criterias are ignored
     **/
    public void addSelectionCriteria(SelectionCriteria selCriteria) 
     throws SIDiscriminatorSyntaxException, SISelectorSyntaxException, SIResourceException
    {
      
      if (tc.isEntryEnabled())
        SibTr.entry(tc, "addSelectionCriteria", 
          new Object[] { selCriteria });
       
      // We should really check discriminator access at this stage
      // However since these checks require access to the connection which 
      // does not belong on this object , I have decided to avoid the checks 
      // at this stage as they will be done at message delivery time
      // Also, it is the case that in all current usage the discriminators will
      // be the same for all selectionCriteria 
           
      boolean duplicateCriteria = _consumerDispatcher.getConsumerDispatcherState().addSelectionCriteria(selCriteria);
      if( !duplicateCriteria )
      {
        // Add the new criteria to the matchspace
        try
        {
          _messageProcessor
           .getMessageProcessorMatching()
           .addConsumerDispatcherMatchTarget(
            _consumerDispatcher,
            _consumerDispatcher.getDestination().getUuid(),
             selCriteria );
        }
        catch (SIDiscriminatorSyntaxException e)
        {
          // No FFDC code needed

          // Remove the selection criteria as it was added to the list
          _consumerDispatcher.getConsumerDispatcherState().removeSelectionCriteria(selCriteria);
          
          if (tc.isEntryEnabled())
            SibTr.exit(tc, "addSelectionCriteria", e);
          throw e;
        }
        catch (SISelectorSyntaxException e)
        {
          // No FFDC code needed

          // Remove the selection criteria as it was added to the list
          _consumerDispatcher.getConsumerDispatcherState().removeSelectionCriteria(selCriteria);

          if (tc.isEntryEnabled())
            SibTr.exit(tc, "addSelectionCriteria", e);
          throw e;
        }

        // Persist change made to consumerDispatcherState
        Transaction tran = _messageProcessor.getTXManager().createAutoCommitTransaction();
        if( !_consumerDispatcher.getReferenceStream().isUpdating() )
        {
          try 
          {
            _consumerDispatcher.getReferenceStream().requestUpdate(tran);
          } 
          catch (MessageStoreException e) 
          {
            // MessageStoreException shouldn't occur so FFDC.
            FFDCFilter.processException(
            e,
            "com.ibm.ws.sib.processor.impl.MPSubscriptionImpl.addSelectionCriteria",
            "1:153:1.6",
            this);
  
            // Remove the selection criteria as it was added to the list
            _consumerDispatcher.getConsumerDispatcherState().removeSelectionCriteria(selCriteria);
            
            SibTr.exception(tc, e);
            if (tc.isEntryEnabled()) SibTr.exit(tc, "addSelectionCriteria", "SIResourceException");
            throw new SIResourceException(e);
            
          }
        }
      }
      
      if (tc.isEntryEnabled())
        SibTr.exit(tc, "addSelectionCriteria"); 

    }

    /**
     *  Remove a selection criteria from the subscription
     **/
    public void removeSelectionCriteria(SelectionCriteria selCriteria) 
    throws SIResourceException
    {

      if (tc.isEntryEnabled())
        SibTr.entry(tc, "removeSelectionCriteria", 
          new Object[] { selCriteria });
      
      // If the selection criteria was removed then this is an indication that it was
      // in the matchspace and it can be removed.
      boolean wasRemoved = 
        _consumerDispatcher.getConsumerDispatcherState().removeSelectionCriteria(selCriteria);      
      
      if (wasRemoved)
      {
        Transaction tran = _messageProcessor.getTXManager().createAutoCommitTransaction();
        if( !_consumerDispatcher.getReferenceStream().isUpdating() )
        {
          try 
          {
            _consumerDispatcher.getReferenceStream().requestUpdate(tran);
          } 
          catch (MessageStoreException e) 
          {
            // MessageStoreException shouldn't occur so FFDC.
            FFDCFilter.processException(
            e,
            "com.ibm.ws.sib.processor.impl.MPSubscriptionImpl.removeSelectionCriteria",
            "1:203:1.6",
            this);
    
            // Add the criteria back into the list as the remove failed.
            _consumerDispatcher.getConsumerDispatcherState().addSelectionCriteria(selCriteria);
            
            SibTr.exception(tc, e);
            if (tc.isEntryEnabled()) SibTr.exit(tc, "removeSelectionCriteria", "SIResourceException");
            throw new SIResourceException(e);
              
          }
        }
  
        // Remove the criteria from the matchspace
        _messageProcessor
         .getMessageProcessorMatching()
         .removeConsumerDispatcherMatchTarget(
          _consumerDispatcher,
          selCriteria );
      }
        
      if (tc.isEntryEnabled())
        SibTr.exit(tc, "removeSelectionCriteria"); 
   
    }

    /**
     * List existing selection criterias registered with the subscription
     **/
    public SelectionCriteria[] getSelectionCriteria()
    {
      if (tc.isEntryEnabled())
        SibTr.entry(tc, "getSelectionCriteria");
      
      SelectionCriteria[] list = _consumerDispatcher.getConsumerDispatcherState().getSelectionCriteriaList();
      
      if (tc.isEntryEnabled())
        SibTr.exit(tc, "getSelectionCriteria", list);
      return list;
    }

    /**
     * Store a map of user properties with a subscription
     * The map provided on this call will replace any existing map stored with the subscription
    **/
    public void setUserProperties(Map userData) throws SIResourceException
    {
      if (tc.isEntryEnabled())
        SibTr.entry(tc, "setUserProperties", 
          new Object[] { userData });
 
      _consumerDispatcher.getConsumerDispatcherState().setUserData(userData);
      
      Transaction tran = _messageProcessor.getTXManager().createAutoCommitTransaction();
      if( !_consumerDispatcher.getReferenceStream().isUpdating() )
      {
        try 
        {
          _consumerDispatcher.getReferenceStream().requestUpdate(tran);
        } 
        catch (MessageStoreException e) 
        {
          // MessageStoreException shouldn't occur so FFDC.
          FFDCFilter.processException(
          e,
          "com.ibm.ws.sib.processor.impl.MPSubscriptionImpl.setUserProperties",
          "1:269:1.6",
          this);
  
          SibTr.exception(tc, e);
          if (tc.isEntryEnabled()) SibTr.exit(tc, "setUserProperties", "SIResourceException");
          throw new SIResourceException(e);
            
        }
      }
      
      if (tc.isEntryEnabled())
        SibTr.exit(tc, "setUserProperties"); 
      
    }

    /**
      Get the map currently stored with the subscription
    **/
    public Map getUserProperties()
    {
      if (tc.isEntryEnabled())
        SibTr.entry(tc, "getUserProperties");
      
      Map map = _consumerDispatcher.getConsumerDispatcherState().getUserData();
      
      if (tc.isEntryEnabled())
        SibTr.exit(tc, "getUserProperties", map);
      return map;
    }

    /**
      Get subscriberID for this subscription
    **/
    public String getSubscriberId()
    {
      if (tc.isEntryEnabled())
        SibTr.entry(tc, "getSubscriberId");

      String subscriberId = _consumerDispatcher.getConsumerDispatcherState().getSubscriberID();
      
      if (tc.isEntryEnabled())
        SibTr.exit(tc, "getSubscriberId", subscriberId);
      return subscriberId;
    }
    
    /**
      Get WPMTopicSpaceName for this subscription
    **/
    public String getWPMTopicSpaceName()
    {
      if (tc.isEntryEnabled())      
        SibTr.entry(tc, "getWPMTopicSpaceName");
      
      String tsName = _consumerDispatcher.getConsumerDispatcherState().getTopicSpaceName();
      
      if (tc.isEntryEnabled())
        SibTr.exit(tc, "getWPMTopicSpaceName", tsName);
      return tsName;
   }

   /**
     Get MEName for this subscription
   **/
   public String getMEName()
   {
     if (tc.isEntryEnabled())
       SibTr.entry(tc, "getMEName");
     
     String meName = _messageProcessor.getMessagingEngineName();
     
     if (tc.isEntryEnabled())
       SibTr.exit(tc, "getMEName", meName);
     return meName;
   }
 }
