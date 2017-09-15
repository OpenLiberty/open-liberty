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

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.Reliability;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.websphere.sib.exception.SIIncorrectCallException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.admin.BaseLocalizationDefinition;
import com.ibm.ws.sib.admin.DestinationDefinition;
import com.ibm.ws.sib.admin.JsMessagingEngine;
import com.ibm.ws.sib.admin.LocalizationDefinition;
import com.ibm.ws.sib.admin.MQLinkDefinition;
import com.ibm.ws.sib.admin.VirtualLinkDefinition;
import com.ibm.ws.sib.comms.mq.MQLinkObject;
import com.ibm.ws.sib.msgstore.Item;
import com.ibm.ws.sib.msgstore.ItemStream;
import com.ibm.ws.sib.msgstore.MessageStoreException;
import com.ibm.ws.sib.msgstore.NonLockingCursor;
import com.ibm.ws.sib.msgstore.OutOfCacheSpace;
import com.ibm.ws.sib.msgstore.transactions.ExternalAutoCommitTransaction;
import com.ibm.ws.sib.msgstore.transactions.Transaction;
import com.ibm.ws.sib.processor.MQLinkLocalization;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.impl.interfaces.LocalizationPoint;
import com.ibm.ws.sib.processor.impl.store.filters.ClassEqualsFilter;
import com.ibm.ws.sib.processor.impl.store.itemstreams.MQLinkMessageItemStream;
import com.ibm.ws.sib.processor.impl.store.itemstreams.MQLinkPubSubBridgeItemStream;
import com.ibm.ws.sib.processor.impl.store.itemstreams.SIMPItemStream;
import com.ibm.ws.sib.processor.runtime.SIMPMQLinkQueuePointControllable;
import com.ibm.ws.sib.processor.runtime.impl.ControlAdapter;
import com.ibm.ws.sib.processor.runtime.impl.MQLinkControl;
import com.ibm.ws.sib.transactions.LocalTransaction;
import com.ibm.ws.sib.transactions.TransactionCommon;
import com.ibm.ws.sib.trm.links.LinkException;
import com.ibm.ws.sib.trm.links.LinkManager;
import com.ibm.ws.sib.trm.links.mql.MQLinkManager;
import com.ibm.ws.sib.utils.SIBUuid8;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.DestinationType;
import com.ibm.wsspi.sib.core.exception.SIConnectionLostException;
import com.ibm.wsspi.sib.core.exception.SIRollbackException;

/**
 * @author Neil Young
 *
 */
public class MQLinkHandler extends LinkHandler implements MQLinkLocalization
{
  /** Trace for the component */
  private static final TraceComponent tc =
    SibTr.register(
      MQLinkHandler.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);

  private static final TraceNLS nls =
    TraceNLS.getTraceNLS(SIMPConstants.RESOURCE_BUNDLE);
 
  private SIBUuid8 _mqLinkUuid;
  private String _mqLinkName;
  
  // The reference to the MQLink component's object for this link
  private MQLinkObject _mqLinkObject;
  
  private ItemStream _mqLinkStateItemStream; // was type MQLinkStateItemStream
  
  private long _mqLinkStateItemStreamId;
  
  private MQLinkPubSubBridgeItemStream _mqLinkPubSubBridgeItemStream;  
       
  private MQLinkManager _mqLinkManager;
  private boolean _registeredInWLM = false;
  private LinkManager _linkManager;
  
    
  // Have we registered the PSB MBean
  private boolean _psbMBeanRegistered = false;
     
 
  /**
   * Warm start constructor invoked by the Message Store.
   */
  public MQLinkHandler()
  {
    super();
    // This space intentionally left blank.   
  }
  
  /**
   * <p>Cold start constructor.</p>
   * <p>Create a new instance of a link, passing in the name of the 
   * link and its definition.</p>
   * 
   * @param mqld
   * @param virtualLinkDefinition
   * @param messageProcessor
   * @param parentItemStream
   * @param transaction
   * @param durableSubscriptionsTable
   * @throws SIResourceException
   * @throws MessageStoreException
   */
  public MQLinkHandler(
    MQLinkDefinition mqld,
    VirtualLinkDefinition virtualLinkDefinition,
    MessageProcessor messageProcessor,
    SIMPItemStream parentItemStream,   
    TransactionCommon transaction, 
    HashMap durableSubscriptionsTable) throws SIResourceException, MessageStoreException 
  {
    super(virtualLinkDefinition
         ,messageProcessor
         ,parentItemStream 
         ,transaction
         ,durableSubscriptionsTable);
         
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "MQLinkHandler", 
        new Object[]{mqld, 
                     virtualLinkDefinition, 
                     messageProcessor, 
                     parentItemStream, 
                     transaction, 
                     durableSubscriptionsTable});
                     
    // Set the mqlink uuid and store the definition
    _mqLinkUuid = mqld.getUuid();
    _mqLinkName = mqld.getName();

    // Create a destination definition with default attributes for use by this
    // mqlink handler
    DestinationDefinition mqLinkDestDefinition = 
      messageProcessor.createDestinationDefinition(DestinationType.QUEUE, 
                                                   virtualLinkDefinition.getName());  
      //Set up a suitable qos
      mqLinkDestDefinition.setMaxReliability(Reliability.ASSURED_PERSISTENT);
      mqLinkDestDefinition.setDefaultReliability(Reliability.ASSURED_PERSISTENT);                                                     
      updateDefinition(mqLinkDestDefinition);   
                                                        
     //The ItemStream for MQLink State
    _mqLinkStateItemStream = null;

    // Add ItemStream for MQLink PubSub Bridge
    Transaction msTran = messageProcessor.resolveAndEnlistMsgStoreTransaction(transaction);
    _mqLinkPubSubBridgeItemStream = new MQLinkPubSubBridgeItemStream(this, msTran);
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "MQLinkHandler", this);        
  }

  /**
   * Complete recovery of a MQLinkLocalizationItemStream retrieved from the 
   * MessageStore.
   * 
   * @param processor
   * @param destinationHandler to use in reconstitution
   * 
   * @throws SIResourceException
   */    
  public void reconstitute(
    MessageProcessor processor, 
    HashMap durableSubscriptionsTable,
    int startMode) throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "reconstitute", 
        new Object[] { processor, durableSubscriptionsTable, Integer.valueOf(startMode) });
        
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
      SibTr.debug(tc, "Reconstituting MQLink " + getName());
   
    try 
    {
      super.reconstitute(processor, durableSubscriptionsTable, startMode);

      // Check Removed.
//      if(!isToBeDeleted())
//      {                
        // There should only be one MQLinkStateItemStream in the MQLinkLocalizationItemStream.
        if (_mqLinkStateItemStreamId != 0)
        {
          _mqLinkStateItemStream 
            = (ItemStream) findById(_mqLinkStateItemStreamId);
             
          // A MQLinkHandler must have a MQLinkStateItemStream as long as the destination
          // is not in delete state.  The ME may have restarted and the item stream already 
          // deleted
          if (_mqLinkStateItemStream == null && !isToBeDeleted())
          {
            SIResourceException e =
              new SIResourceException(
                nls.getFormattedMessage(
                  "LINK_HANDLER_RECOVERY_ERROR_CWSIP0049",
                  new Object[] { getName() },
                  null));
                
            SibTr.error(tc, "LINK_HANDLER_RECOVERY_ERROR_CWSIP0049", new Object[] { getName() });
      
            FFDCFilter.processException(
              e,
              "com.ibm.ws.sib.processor.impl.MQLinkHandler.reconstitute",
              "1:270:1.71",
              this);
      
            SibTr.exception(tc, e);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
              SibTr.exit(tc, "reconstitute", e);
            throw e;
          }
        }
  
        // There should only be one MQLinkStateItemStream in the MQLinkLocalizationItemStream.
        NonLockingCursor cursor = newNonLockingItemStreamCursor(
          new ClassEqualsFilter(MQLinkPubSubBridgeItemStream.class));
        _mqLinkPubSubBridgeItemStream = (MQLinkPubSubBridgeItemStream)cursor.next();
        
        // A MQLinkLocalizationItemStream should not be in the DestinationManager
        // without a MQLinkStateItemStream as long as the destination
        // is not in delete state.  The ME may have restarted and the item stream already 
        // deleted
        if (_mqLinkPubSubBridgeItemStream == null && !isToBeDeleted())
        {
          SIResourceException e =
             new SIResourceException(
              nls.getFormattedMessage(
                "LINK_HANDLER_RECOVERY_ERROR_CWSIP0049",
                new Object[] { getName() },
                null));

          SibTr.error(tc, "LINK_HANDLER_RECOVERY_ERROR_CWSIP0049", new Object[] { getName() });
    
          FFDCFilter.processException(
            e,
            "com.ibm.ws.sib.processor.impl.MQLinkHandler.reconstitute",
            "1:303:1.71",
            this);
    
          SibTr.exception(tc, e);
          if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
            SibTr.exit(tc, "reconstitute", e);
          throw e;
        }
      
        cursor.finished();
/*      }
      else
      {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
          SibTr.debug(tc, "MQLink marked to be deleted, bypass state stream integrity checks");
      }*/          
    }
    catch (Exception e)
    {
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.impl.MQLinkHandler.reconstitute",
        "1:325:1.71",
        this);  
        
      SibTr.exception(tc, e);     
           
      // At the moment, any exception we get while reconstituting means that we
      // want to mark the destination as corrupt.
      _isCorruptOrIndoubt = true;        
           
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "reconstitute", e);
        
      throw new SIResourceException(e);
    }
      
    /*
     * We should have completed all restart message store operations for this
     * link by this point.
     */
     
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "reconstitute");           
  }     

  /**
   * Returns the mqLinkPubSubBridgeItemStream.
   * @return MQLinkPubSubBridgeItemStream
   */
  public MQLinkPubSubBridgeItemStream getMqLinkPubSubBridgeItemStream() {
	  return _mqLinkPubSubBridgeItemStream;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.MQLinkLocalization#getMQLinkStateItemStream()
   */
  public ItemStream getMQLinkStateItemStream() 
  {    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getMQLinkStateItemStream");
      SibTr.exit(tc, "getMQLinkStateItemStream", _mqLinkStateItemStream);
    }    
    return _mqLinkStateItemStream;
  }
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.MQLinkLocalization#setMQLinkStateItemStream(com.ibm.ws.sib.msgstore.ItemStream)
   */
  public void setMQLinkStateItemStream(ItemStream mqLinkStateItemStream) 
    throws SIException                                      
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "setMQLinkStateItemStream", 
        new Object[]{mqLinkStateItemStream});
      // Test that itemstream has not already been set
      
      if(_mqLinkStateItemStream != null)
      {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
          SibTr.exit(tc, "setMQLinkStateItemStream", "SIIncorrectCallException");
          
        throw new SIIncorrectCallException(
          nls.getFormattedMessage(
            "MQLINK_STATE_ITEMSTREAM_ALREADY_EXISTS",
            null,
            null));
      }      
            
      // Now set the itemstream
      _mqLinkStateItemStream = mqLinkStateItemStream;
     
      // First, create a local transaction
      ExternalAutoCommitTransaction transaction = 
        messageProcessor.getTXManager().createAutoCommitTransaction();
          
      try
      {
        // Try to set the mqlink itemstream.             
        addItemStream(mqLinkStateItemStream, transaction);
        
        // Get the itemstreams id - we'll use this in reconstitution
        _mqLinkStateItemStreamId = mqLinkStateItemStream.getID();
        
        // Prod the MQLinkHandler to persist this id
        requestUpdate(transaction);
      }
      catch (OutOfCacheSpace e)
      {
        // No FFDC code needed
        SibTr.exception(tc, e);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
          SibTr.exit(tc, "setMQLinkStateItemStream", "SIResourceException");
        throw new SIResourceException(e);
      }
      catch (MessageStoreException e)
      {
        // FFDC
        FFDCFilter.processException(
          e,
          "com.ibm.ws.sib.processor.impl.MQLinkHandler.setMQLinkStateItemStream",
          "1:5452:1.341",
          this);
      
        // Map to a core exception  
        SIResourceException eCore =
            new SIResourceException(
              nls.getFormattedMessage(
                "MQLINK_STATE_ITEMSTREAM_MESSAGE_STORE_ERROR",
                null,
                null));   
                
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
          SibTr.exit(tc, "setMQLinkStateItemStream", eCore);
           
        throw eCore;  
      }          

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "setMQLinkStateItemStream");        
      
  }
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.store.AbstractItem#getPersistentData()
   */
  public void getPersistentData(ObjectOutputStream oos)
  {
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getPersistentData", oos);

    try
    {
      HashMap hm = new HashMap();
      
      addPersistentDestinationData(hm);
      addPersistentLinkData(hm);
      
      // The uuid of the destination
      hm.put("mqlinkuuid",_mqLinkUuid.toByteArray());   
      hm.put("mqlinkname", _mqLinkName);
      
      // The MessageStore id of the MQLinkStateItemStream
      hm.put("mqlinkstateid", Long.valueOf(_mqLinkStateItemStreamId));
      
      oos.writeObject(hm);
    }
    catch (java.io.IOException e)
    {
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.impl.MQLinkHandler.getPersistentData",
        "1:477:1.71",
        this);

      SibTr.exception(tc, e);
      
      SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
        new Object[] { "com.ibm.ws.sib.processor.impl.MQLinkHandler.getPersistentData", 
                       "1:484:1.71", 
                       e });
                       
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
        SibTr.exit(tc, "getPersistentData", "SIErrorException");
      
      throw new SIErrorException(
        nls.getFormattedMessage(
          "INTERNAL_MESSAGING_ERROR_CWSIP0002",
          new Object[] { "com.ibm.ws.sib.processor.impl.MQLinkHandler.getPersistentData", 
                         "1:494:1.71", 
                         e },
          null),
        e);
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getPersistentData");
  }
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.store.itemstreams.SIMPItemStream#restore(java.io.ObjectInputStream, int)
   */
  public void restore(ObjectInputStream ois, int dataVersion)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc, "restore", new Object[] { ois, Integer.valueOf(dataVersion) });
    
    checkPersistentVersionId(dataVersion);  

    try
    {
      HashMap hm = (HashMap)ois.readObject();
      
      restorePersistentDestinationData(hm);
      restorePersistentLinkData(hm);
      
      // Restore mqLinkUuid
      _mqLinkUuid = new SIBUuid8((byte[])hm.get("mqlinkuuid"));  
      // Restore the mqLinkName
      _mqLinkName = (String)hm.get("mqlinkname");
      
      // Restore the MessageStore id of the MQLinkStateItemStream
      _mqLinkStateItemStreamId = ((Long)hm.get("mqlinkstateid")).longValue();
     
   }
    catch (Exception e) 
    {
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.impl.MQLinkHandler.restore",
        "1:536:1.71",
        this);

      SibTr.exception(tc, e);
      SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
        new Object[] {
          "com.ibm.ws.sib.processor.impl.MQLinkHandler",
          "1:543:1.71",
          e });
          
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
        SibTr.exit(tc, "restore", "SIErrorException");
      throw new SIErrorException(
        nls.getFormattedMessage(
          "INTERNAL_MESSAGING_ERROR_CWSIP0002",
          new Object[] {
            "com.ibm.ws.sib.processor.impl.MQLinkHandler",
            "1:553:1.71",
            e },
          null),
        e);
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "restore");
  }  

  /**
   * <p>Ensure all stream state associated with the link has been removed, then
   * the linkHandler itself can be removed.</p>
   */
  public boolean cleanupDestination()
  throws 
    SIRollbackException, 
    SIConnectionLostException, 
    SIIncorrectCallException, 
    SIResourceException, 
    SIErrorException   
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "cleanupDestination");
  
    boolean cleanedUp = false;

    //Ensure any cleanup does not occur at the same time the link is being used.
    synchronized(this)
    {
      if (isToBeDeleted())
      {
        // Tidy up the state item streams first
        LocalTransaction siTran =
          txManager.createLocalTransaction(true);          
        removeStateItemStreams((Transaction) siTran); 
        siTran.commit();
      }
    }

    // Now tidy up the rest of the infrastructure    
    cleanedUp = cleanupBaseDestination();
   
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "cleanupDestination", Boolean.valueOf(cleanedUp));

    return cleanedUp;
  }  
  
  /**
   * Initialize non-persistent fields.  These fields are common to both MS
   * reconstitution of DestinationHandlers and initial creation.
   * 
   * @param messageProcessor the message processor instance
   * @param durableSubscriptionsTable the topicspace durable subscriptions
   *         HashMap from the DestinationManager.  
   * @param transaction the transaction to use for non persistent 
   *         initialization.  Can be null, in which case an auto transaction
   *         will be used.
   * 
   * @throws MessageStoreException if there was an error interacting with the
   *          Message Store.
   * @throws SIStoreException if there was a transaction error.
   */
  void initializeNonPersistent(
    MessageProcessor messageProcessor, 
    HashMap durableSubscriptionsTable,
    TransactionCommon transaction)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "initializeNonPersistent",
        new Object[] 
          { messageProcessor, durableSubscriptionsTable, transaction });

    // Required to pick where to send messages too
    _mqLinkManager = messageProcessor.getMQLinkManager();      

    // Required to pick where to send messages too
    _linkManager = messageProcessor.getLinkManager();      

    
    super.initializeNonPersistent(messageProcessor,
                                  durableSubscriptionsTable,
                                  transaction);


    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "initializeNonPersistent");     
  }
  
  /**
   * @return
   */
  public SIBUuid8 getMqLinkUuid() 
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getMqLinkUuid");
      SibTr.exit(tc, "getMqLinkUuid", _mqLinkUuid);
    }
    return _mqLinkUuid;
  }
  
  public MQLinkObject getMQLinkObject() 
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getMQLinkObject");
      SibTr.exit(tc, "getMQLinkObject", _mqLinkObject);
    }
    return _mqLinkObject;
  }

  public void setMQLinkObject(MQLinkObject mqLinkObject) 
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "setMQLinkObject", mqLinkObject);
    }
    
    _mqLinkObject = mqLinkObject;
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {    
      SibTr.exit(tc, "setMQLinkObject");
    }
  }  
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#isLink()
   */
  public boolean isMQLink()
  {
    return true;
  }
  
  /**
   * <p>Register the destination vith WLM via TRM</p>
   * 
   * System destinations and Temporary destinations are not registered
   * with WLM.  The destinations themselves have their own addressing 
   * mechanisms.
   */
  void registerDestination()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "registerDestination");

    //Only register the destination once its been defined to WLM
    if (_mqLinkManager.isDefined(getUuid()))
    {
      if ((hasLocal()) && (!_registeredInWLM))
      {
        try
        {
          if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(tc, "Register MQLink: " + getUuid() + ", with linkManager");           
          _linkManager.register(getUuid());
          _registeredInWLM = true;
        }
        catch (LinkException e)
        {
          //Error during create of the link.  Trace an FFST.  If we cant 
          //advertise the link, other ME's wont be able to send to it
          FFDCFilter.processException(
            e,
            "com.ibm.ws.sib.processor.impl.MQLinkHandler.registerDestination",
            "1:724:1.71",
            this);
    
          SibTr.exception(tc, e);
          if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
            SibTr.exit(tc, "registerDestination", e);
        }
      }
    }
        
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "registerDestination");  
      
    return;
  }
   
  /**
   * <p>Deregister the destination vith WLM via TRM</p>
   * 
   * System destinations and Temporary destinations are not registered
   * with WLM.  The destinations themselves have their own addressing 
   * mechanisms.
   */
  void deregisterDestination()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "deregisterDestination");

    try
    {
      if(_registeredInWLM) // defect 241790, guard against attempting delete when not registered
      {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
          SibTr.debug(tc, "Deregister MQLink: " + getUuid() + ", from LinkManager");       
        _linkManager.deregister(getUuid());
      }
      _registeredInWLM = false;
    }
    catch (LinkException e)
    {
      //Error during create of the link.  Trace an FFST.  If we cant 
      //advertise the link, other ME's wont be able to send to it
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.impl.MQLinkHandler.deregisterDestination",
        "1:769:1.71",
        this);
  
      SibTr.exception(tc, e);
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
        SibTr.exit(tc, "deregisterDestination", e);
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "deregisterDestination");  
      
    return;
  }
   
  /**
   * Registers the link with WLM
   * @throws SIResourceException
   */ 
  public void registerLink() throws SIResourceException
  { 
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "registerLink");
    // Tell TRM that the link exists 
    try
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        SibTr.debug(tc, "Register MQLink: " + getUuid() + ", with mqLinkManager");       
      _mqLinkManager.define(getUuid());
    }
    catch (LinkException e)
    {
      //Error during create of the link.  Trace an FFST and exit
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.impl.MQLinkHandler.registerLink",
        "1:804:1.71",
        this);
  
      SibTr.exception(tc, e);
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
        SibTr.exit(tc, "registerLink", e);
      throw new SIResourceException(e);
    }

    //Register the link with TRM
    registerDestination();
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "registerLink");
  }

  /**
   * De-registers the link from WLM
   * @throws SIResourceException
   */ 
  public void deregisterLink()
  { 
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "deregisterLink");
    //Deregister the link with TRM
    deregisterDestination();    
    // Tell TRM that the link should be undefined 
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
      SibTr.debug(tc, "Deregister MQLink: " + getUuid() + ", with mqLinkManager");       
    _mqLinkManager.undefine(getUuid());

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "deregisterLink");
  }  
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.BaseDestinationHandler#updatePostRegistration(boolean)
   */
  public void updatePostRegistration(boolean advertise)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "updatePostRegistration", Boolean.valueOf(advertise));

    // If the link is available for messages to be put to it, then
    // then advertise it in WLM, otherwise remove the advertisement from
    // WLM
    if (advertise)
    { 
      registerDestination();      
    }
    else
    {
      deregisterDestination();
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "updatePostRegistration");
  }
  /**
   * Method removeStateItemStreams.
   * <p>This removes the state itemstreams associated with the link.
   * @param transaction
   * @throws SIStoreException
   */
  private void removeStateItemStreams(Transaction transaction)
    throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "removeStateItemStreams", transaction );

    try
    {
      // Clean up mqlink state stream. This is a raw itemstream so
      // we do this "by hand". 
      if( _mqLinkStateItemStream != null && _mqLinkStateItemStream.isInStore()) 
      {
        Item item = null;
    
        while (null != (item = _mqLinkStateItemStream.findFirstMatchingItem(null)))
        {
          item.remove(transaction, NO_LOCK_ID);
        }
        
        // Now remove the itemstream itself
        _mqLinkStateItemStream.remove(transaction, NO_LOCK_ID);        
      }
      
      // Clean up pubsub bridge state itemstream.
      // Note that pubsub bridge should already have tidied up contained
      // itemstreams. This does a final tidy and removes the state itemstream
      // itself.  Must check that this is not in store as we may have the item stream
      // from a previous failed delete
      if( _mqLinkPubSubBridgeItemStream != null && _mqLinkPubSubBridgeItemStream.isInStore()) 
      {
        _mqLinkPubSubBridgeItemStream.removeAll(transaction);
      }
      
      // Set refs to null removed as we may fail the delete later.
//      _mqLinkStateItemStream = null;
//      _mqLinkPubSubBridgeItemStream = null;  
    }
    catch (MessageStoreException e)
    {
      // FFDC
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.impl.MQLinkHandler.removeStateItemStreams",
        "1:910:1.71",
        this);

      SibTr.exception(tc, e);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "removeStateItemStreams", e);

      throw new SIResourceException(e);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "removeStateItemStreams");
  }
  
  /**
   * Create a new PtoPMessageItemStream and add it to this Destination's Localisations.
   * <p>
   * In addition to creating and adding it, this function also performs all the 
   * necessary updates to make it a recognized part of the Destination.
   * 
   * @param localisationIsRemote should be true if the localisation is remote.
   * @param transaction  The Transaction to add under.  Cannot be null.
   * @param messagingEngineUuid The uuid of the messaging engine that owns the localisation
   * @return PtoPMessageItemStream the new PtoPMessageItemStream added.
   * 
   * @throws SIResourceException if the add fails due to a Message Store problem.
   */
  protected MQLinkMessageItemStream addNewMQLinkLocalisation(
    TransactionCommon transaction,
    SIBUuid8 messagingEngineUuid,
    LocalizationDefinition destinationLocalizationDefinition)
     throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "addNewMQLinkLocalisation",
        new Object[] {
          transaction,
          destinationLocalizationDefinition });

    MQLinkMessageItemStream mqlinkItemStream = null;

    // Add to the MessageStore
    try
    {
      mqlinkItemStream = 
        new MQLinkMessageItemStream(
            this,
            messagingEngineUuid);
            
      _localisationManager.
        addNewLocalPtoPLocalisation(transaction,
                                    mqlinkItemStream);      

      // Feature 176658.3.2
      Transaction msTran = txManager.resolveAndEnlistMsgStoreTransaction(transaction);
      addItemStream(mqlinkItemStream, msTran);

      // Set the default limits
      mqlinkItemStream.setDefaultDestLimits();
      // Setup any message depth interval checking (510343)
      mqlinkItemStream.setDestMsgInterval();
           
      //Update the localisation definition of the itemstream now that it has
      //been added into the message store
      mqlinkItemStream.updateLocalizationDefinition(destinationLocalizationDefinition);
      
      attachPtoPLocalisation(mqlinkItemStream, false);
    }
    catch (OutOfCacheSpace e)
    {
      // No FFDC code needed
      
      SibTr.exception(tc, e);
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
        SibTr.exit(tc, "addNewMQLinkLocalisation", "SIResourceException");
      
      throw new SIResourceException(e);
    }
    catch (MessageStoreException e)
    {
      // MessageStoreException shouldn't occur so FFDC.
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.impl.MQLinkHandler.addNewMQLinkLocalisation",
        "1:998:1.71",
        this);

      SibTr.exception(tc, e);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "addNewMQLinkLocalisation", e);

      throw new SIResourceException(e);
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "addNewMQLinkLocalisation", mqlinkItemStream);

    return mqlinkItemStream;
  }  
  
  // Start d266910
                                           
  /**
   * This stop method overrides the stop method in BaseDestinationHandler and is driven when the ME
   * is stopped. As well as performing the normal stop processing for the MQLink object, it also 
   * ensures that the uuid of the the MQLink is undefined from the set managed by the TRM. Otherwise, 
   * there is the potential to add the uuid of the same MQLink twice. 
   * 
   * Additionally, call the MQLink component via the MQLinkObjct to notify
   * it that we are stopping.
   * 
   * @param mode - stop mode 
   */                                           
  public void stop(int mode) {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "stop");
       
    super.stop(mode); 

    // Signal the MQLink component to stop
    try 
    {
      if(_mqLinkObject != null)
        _mqLinkObject.stop();
      
      
    } 
    catch (SIResourceException e) 
    {
      // No FFDC code needed
      
      SibTr.exception(tc, e);
      
      // The MQLink component will have FFDC'd we'll trace
      // the problem but allow processing to continue  
    } 
    catch (SIException e) 
    {
      // No FFDC code needed
      
      SibTr.exception(tc, e);
      
      // The MQLink component will have FFDC'd we'll trace
      // the problem but allow processing to continue  
    }
  
    _mqLinkManager.undefine(getUuid()); 
           
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "stop");    
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.MQLinkLocalization#delete()
   */
  public void delete() 
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "delete");
    
    

    // Mark the destination for deletion
    try
    {
      // Set the deletion flag in the DH persistently. A transaction per DH??
      LocalTransaction siTran = txManager.createLocalTransaction(true);

      setToBeDeleted(true);
      
      // Adjust the destination lookups in Destination Manager
      destinationManager.getLinkIndex().delete(this);
      
      requestUpdate((Transaction) siTran);
      // commit the transaction
      siTran.commit();
      
      String name = _mqLinkName;
      if (name == null)      
        name = getName();
      SibTr.info(tc, "MQLINK_DEST_DELETE_INFO_CWSIP0064", 
                   new Object[]{name, _mqLinkUuid});   
    }
    catch (MessageStoreException e)
    {
      // No FFDC code needed
      
      SibTr.exception(tc, e);
      
      //throw e;
    }
    catch (SIException e)
    {
      // No FFDC code needed
      
      SibTr.exception(tc, e);
       
      //handleRollback(siTran);
      //        throw e;
    }    

    // Now start the asynch deletion thread to tidy up
    destinationManager.startAsynchDeletion();
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "delete");        
  }

 
  
  /**
   * Alert the MQLink and PSB components that MP has now started.
   * 
   * @param startMode
   * @param me
   * @throws SIException 
   * @throws SIResourceException 
   */
  public void announceMPStarted(
      int startMode,
      JsMessagingEngine me) throws SIResourceException, SIException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "announceMPStarted",
        new Object[] {
            startMode,
            me });

    // Drive mpStarted against the associated MQLink object
    if(_mqLinkObject != null)
      _mqLinkObject.mpStarted(startMode, me);

    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "announceMPStarted");
  }  

  /**
   * Alert the MQLink and PSB components that destroy has been driven.
   * 
   * @throws SIException 
   * @throws SIResourceException 
   */
  public void destroy() throws SIResourceException, SIException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "destroy");

    // Drive destroy against the associated MQLink object
    if(_mqLinkObject != null)
      _mqLinkObject.destroy();
    
   
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "destroy");
  }    
  
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.ControllableResource#createControlAdapter()
   */
  public void createControlAdapter()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "createControlAdapter");
    
    // MQLinkHandler behaves like a Queue in this context
    controlAdapter = new MQLinkControl(messageProcessor, this);
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "createControlAdapter");
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.MQLinkLocalization#getSIMPMQLinkQueuePointControllable()
   */
  public SIMPMQLinkQueuePointControllable getSIMPMQLinkQueuePointControllable() 
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getSIMPMQLinkQueuePointControllable"); 
    
    ControlAdapter ca = null;
    SIMPMQLinkQueuePointControllable mqlinkca = null;    
    
    // We'll return the controllable associated with the mqlink itemstream
    LocalizationPoint stream = getQueuePoint(messageProcessor.getMessagingEngineUuid());

    if(stream instanceof MQLinkMessageItemStream)
    {
      ca = stream.getControlAdapter();
      if(ca instanceof SIMPMQLinkQueuePointControllable)
      {
        mqlinkca = (SIMPMQLinkQueuePointControllable)ca;  
      }
    }
    // Retrieve the control adapter associated with the MQLink itemstream
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getSIMPMQLinkQueuePointControllable");    
    return mqlinkca;
  }
  
  

  /**
   * <p>This method updates the LocalizationDefinition associated
   * with the MQLinkHandler and performs any necessary
   * modifications to the message store and other components to
   * reflect the new state of the Handler.</p>
   * @param destinationLocalizationDefinition
   * <p>Updates the DestinationLocalizationDefinition associated with the 
   * MQLinkHandler.</p>
   */
  protected void updateLocalizationDefinition(BaseLocalizationDefinition destinationLocalizationDefinition,
      TransactionCommon transaction) 
     throws 
       SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "updateLocalizationDefinition", new Object[] {destinationLocalizationDefinition, transaction});

    //this is an update of the existing localization  
    _ptoPRealization.updateLocalisationDefinition(destinationLocalizationDefinition, transaction);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "updateLocalizationDefinition");
  }

  
}
