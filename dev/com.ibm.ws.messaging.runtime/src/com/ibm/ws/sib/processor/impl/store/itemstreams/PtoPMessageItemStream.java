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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Set;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.msgstore.AbstractItem;
import com.ibm.ws.sib.msgstore.Filter;
import com.ibm.ws.sib.msgstore.MessageStoreException;
import com.ibm.ws.sib.msgstore.NonLockingCursor;
import com.ibm.ws.sib.msgstore.Statistics;
import com.ibm.ws.sib.msgstore.transactions.Transaction;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.impl.BaseDestinationHandler;
import com.ibm.ws.sib.processor.impl.interfaces.ConsumerManager;
import com.ibm.ws.sib.processor.impl.interfaces.ControllableResource;
import com.ibm.ws.sib.processor.impl.interfaces.LocalizationPoint;
import com.ibm.ws.sib.processor.impl.interfaces.MessageEventListener;
import com.ibm.ws.sib.processor.impl.interfaces.OutputHandler;
import com.ibm.ws.sib.processor.impl.interfaces.SIMPMessage;
import com.ibm.ws.sib.processor.impl.store.MessageEvents;
import com.ibm.ws.sib.processor.impl.store.items.MessageItem;
import com.ibm.ws.sib.transactions.PersistentTranId;
import com.ibm.ws.sib.transactions.TransactionCommon;
import com.ibm.ws.sib.utils.SIBUuid8;
import com.ibm.ws.sib.utils.ras.FormattedWriter;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * @author millwood
 * 
 * A DestinationHandler can have one or more localisations.  Each 
 * localisation holds messages for consumers, if the destination 
 * is localised on this ME, or awaiting transmission to remote
 * ME's.
 */
public abstract class PtoPMessageItemStream extends BaseMessageItemStream
                                            implements MessageEventListener, ControllableResource, LocalizationPoint
{
  private static final TraceComponent tc =
    SibTr.register(
      PtoPMessageItemStream.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);
      
 
  private static final TraceNLS nls =
    TraceNLS.getTraceNLS(SIMPConstants.RESOURCE_BUNDLE);
 
  /**
   * Persistent data version number.
   */
  private static final int PERSISTENT_VERSION = 1;  
      
  /**
   * A reference to the output handler for this localisation of
   * the destination.
   */
  private OutputHandler outputHandler = null;

  /**
   * Store the UUID of the messaging engine that this localisation
   * represents.  183715.2
   */
  protected SIBUuid8 messagingEngineUuid;
  
  /**
   * Indicates whether the itemstream is awaiting deletion once all state
   * associated with it such as indoubt messages etc has been cleared up.
   */
  private Boolean toBeDeleted = Boolean.FALSE;
  
  /**
   * Indicates that the item stream was toBeDeleted=true at reconstitute
   * time. 
   */  
  private boolean deleteRequiredAtReconstitute = false;
  
  /**
   * Message ordering fields. 
   */
  private boolean unableToOrder = false;
  private PersistentTranId currentTranId = null;
  
  /**
   * Flag to indicate that the consumers from this itemStream are currently blocked
   * by the ConsumerDispatcher. We use this flag in the controllables to show that
   * a message isn't actually available to consumers at this point 
   */
  private volatile boolean _blocked = false;
  
  /**
   * Warm start constructor invoked by the Message Store.
   */
  public PtoPMessageItemStream()
  {
    super();

    // This space intentionally blank
  }

  /**
   * <p>Cold start PtoPMessageItemStream constructor.</p>
   * 
   * @param destinationHandler
   */
  public PtoPMessageItemStream(BaseDestinationHandler destinationHandler,
                               SIBUuid8 messagingEngineUuid,
                               boolean isRemote)
  {
    super();

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "PtoPMessageItemStream", 
        new Object[] 
          { destinationHandler, messagingEngineUuid, Boolean.valueOf(isRemote) });

    /**
     * Store the UUID of the messaging engine that localises the
     * destination.
     */
    this.messagingEngineUuid = messagingEngineUuid;
       
    // The PubSubMessageItemStream has to have the same storage strategy as its
    // parent destination.  Two reasons:
    //
    // 1.  The message store will not allow the ItemStream to 
    // be stored if it has a more permanent storage strategy.
    // 2.  If the DestinationHandler is not persistently stored (e.g. if it is
    // a temporary destination) then this stream should also not be persistent.
    setStorageStrategy(destinationHandler.getStorageStrategy());

    initializeNonPersistent(destinationHandler);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "PtoPMessageItemStream", this);
  }
 
  /**
   * Complete recovery of a PtoPMessageItemStream retrieved from the MessageStore.
   * <p>
   * Feature 174199.2.9
   * 
   * @param destinationHandler to use in reconstitution
   */    
  public void reconstitute(BaseDestinationHandler destinationHandler)
  throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "reconstitute", destinationHandler); 
      
    initializeNonPersistent(destinationHandler);
    
    // If message depth intervals are configured, set the MsgStore watermarks
    // accordinly (510343)
    setDestMsgInterval();

    try
    {
      // F001338-55330
      // getStatistics() inturn will load all the metadata related to the
      // destination.With the introduction of the feature F001338-55330 this
      // will benefit the ME start up time
      Statistics statistics = getStatistics();
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "reconstitute - counts total items, available items",
            new Object[] { new Long(statistics.getTotalItemCount()), new Long(statistics.getAvailableItemCount()) });
    }
    catch (MessageStoreException e)
    {
      // No FFDC code needed
      SibTr.exit(tc, "reconstitute", e);
    }     
    
  }     
  
  /**
   * Return this localisation's assigned OutputHandler.
   * <p>
   * Feature 174199.2.9
   * 
   * @return outputHandler
   */
  public OutputHandler getOutputHandler()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getOutputHandler");
      SibTr.exit(tc, "getOutputHandler", outputHandler);
    }    
    
    return outputHandler;
  } 
  
  /**
   * Set the output handler for this PtoPMessageItemStream.
   * <p>
   * Only point to point localisations have an associated OutputHandler.  When
   * this PtoPMessageItemStream is made available, the OutputHandler is added to the set
   * of OutputHandlers that the DestinationHandler can use.
   * 
   * @param outputHandler
   */
  public void setOutputHandler(OutputHandler outputHandler)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "setOutputHandler", outputHandler);
    this.outputHandler = outputHandler;
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "setOutputHandler");
  }
  
  public void setDeleteRequiredAtReconstitute(boolean value)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "setDeleteRequiredAtReconstitute", new Boolean(value));
      SibTr.exit(tc, "setDeleteRequiredAtReconstitute");
    } 
    this.deleteRequiredAtReconstitute = value;
  }
  
  public boolean getDeleteRequiredAtReconstitute()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
       SibTr.entry(tc, "getDeleteRequiredAtReconstitute");     
       SibTr.exit(tc, "getDeleteRequiredAtReconstitute",
        new Boolean(deleteRequiredAtReconstitute)); 
    }
    return deleteRequiredAtReconstitute;
  }  
  
  /**
   * Returns the messagingEngineUuid.
   * @return SIBUuid
   */
  public SIBUuid8 getLocalizingMEUuid()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getLocalizingMEUuid");
      SibTr.exit(tc, "getLocalizingMEUuid", messagingEngineUuid);
    }
    
    return messagingEngineUuid;
  }
  
  /**
   * Return this Localisations DestinationHandler.
   * 
   * @return DestinationHandler
   */
  public BaseDestinationHandler getDestinationHandler()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getDestinationHandler");
      SibTr.exit(tc, "getDestinationHandler", destinationHandler);
    }    
    
    return destinationHandler;
  }

  /**
   * Mark this itemstream as awaiting deletion and harden the indicator
   * @throws SIStoreException
   */  
  public void unmarkAsToBeDeleted(Transaction transaction) throws SIResourceException 
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "unmarkAsToBeDeleted", transaction);
      
    toBeDeleted = Boolean.FALSE;  
    
    try
    {
      this.requestUpdate(transaction);
    }
    catch (MessageStoreException e)
    {
      // MessageStoreException shouldn't occur so FFDC.
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.impl.store.itemstreams.PtoPMessageItemStream.unmarkAsToBeDeleted",
        "1:336:1.93.1.14",
        this);
        
      SibTr.exception(tc, e);
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "unmarkAsToBeDeleted", e);
      
      throw new SIResourceException(e);
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "unmarkAsToBeDeleted");
      
    return;
  }
  
  /**
   * Method isToBeDeleted.
   * @return boolean
   */
  public boolean isToBeDeleted()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "isToBeDeleted");
      SibTr.exit(tc, "isToBeDeleted", toBeDeleted);
    }
      
    return toBeDeleted.booleanValue();
  }
  
  /*
   * (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.store.itemstreams.SIMPReferenceStream#getVersion()
   */
  public int getPersistentVersion()
  {
    return PERSISTENT_VERSION;
  }   
      
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.store.itemstreams.SIMPItemStream#getPersistentData(java.io.ObjectOutputStream)
   */
  public void getPersistentData(ObjectOutputStream oos)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getPersistentData", oos);

    try
    {
      HashMap<String, Object> hm = new HashMap<String, Object>();
      addPersistentDestinationData(hm);
      oos.writeObject(hm);
    }
    catch (Exception e)
    {
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.impl.store.itemstreams.PtoPMessageItemStream.getPersistentData",
        "1:396:1.93.1.14",
        this);
        
      SibTr.exception(tc, e);
      SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0003",
        new Object[] {
          "com.ibm.ws.sib.processor.impl.store.itemstreams.PtoPMessageItemStream",
          "1:403:1.93.1.14",
          e,
          getDestinationHandler().getName()});

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "getPersistentData", e);
    
      throw new SIErrorException(
       nls.getFormattedMessage(
        "INTERNAL_MESSAGING_ERROR_CWSIP0003",
        new Object[] {
          "com.ibm.ws.sib.processor.impl.store.itemstreams.PtoPMessageItemStream",
          "1:415:1.93.1.14",
          e,
          getDestinationHandler().getName()},
        null),
        e);
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getPersistentData");
  }
  
  /**
   * <p>Build up the persistent data for the itemstream
   * @param hm
   */
  public void addPersistentDestinationData(HashMap<String, Object> hm)
  {    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "addPersistentDestinationData", hm);
      
    hm.put("messagingEngineUuid", getLocalizingMEUuid().toByteArray());
    hm.put("toBeDeleted", toBeDeleted);
      
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "addPersistentDestinationData");
  }
  
  /**
   * Method restore called by message store.
   * @param data
   */
  public void restore(ObjectInputStream ois, int dataVersion)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc, "restore", new Object[] { ois, new Integer(dataVersion) });

    checkPersistentVersionId(dataVersion);

    try
    {
      HashMap hm = (HashMap)ois.readObject();           
      restorePersistentDestinationData(hm);
    }
    catch (Exception e)
    {
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.impl.store.itemstreams.PtoPMessageItemStream.restore",
        "1:464:1.93.1.14",
        this);
        
      SibTr.exception(tc, e);
      SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
        new Object[] {
          "com.ibm.ws.sib.processor.impl.store.itemstreams.PtoPMessageItemStream",
          "1:471:1.93.1.14",
          e });

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "restore", e);
        
      throw new SIErrorException(
        nls.getFormattedMessage(
          "INTERNAL_MESSAGING_ERROR_CWSIP0002",
          new Object[] {
            "com.ibm.ws.sib.processor.impl.store.itemstreams.PtoPMessageItemStream",
            "1:482:1.93.1.14",
            e },
          null),
        e);
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "restore");
  }

  /**
   * <p>Restore the persistent data for the itemstream.</p>
   */
  public void restorePersistentDestinationData(HashMap hm) 
    throws Exception
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc, "restorePersistentDestinationData", new Object[] { hm }); //, new Integer(dataVersion)


    messagingEngineUuid = new SIBUuid8((byte[])hm.get("messagingEngineUuid"));
    toBeDeleted = (Boolean) hm.get("toBeDeleted");
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "restorePersistentDestinationData");
  }    
    


  /**
   * @see com.ibm.ws.sib.processor.impl.interfaces.MessageEventListener#registerForEvents(SIMPMessage)
   */
  public void registerForEvents(SIMPMessage msg)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "registerForEvents");
      
    msg.registerMessageEventListener(MessageEvents.POST_COMMIT_REMOVE, this);
    msg.registerMessageEventListener(MessageEvents.POST_ROLLBACK_REMOVE, this);
    msg.registerMessageEventListener(MessageEvents.POST_COMMIT_ADD, this);
    msg.registerMessageEventListener(MessageEvents.POST_ROLLBACK_ADD, this);
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "registerForEvents");
  }

  /**
   * Method deregisterForEvents.
   * <p>Message Events are no longer required by the PtoPMessageItemStream, so
   * deregister for them.</p>
   * @param msg
   */
  public void deregisterForEvents(SIMPMessage msg)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "deregisterForEvents");
      
    msg.deregisterMessageEventListener(MessageEvents.POST_COMMIT_REMOVE, this);
    msg.deregisterMessageEventListener(MessageEvents.POST_ROLLBACK_REMOVE, this);
    msg.deregisterMessageEventListener(MessageEvents.POST_COMMIT_ADD, this);
    msg.deregisterMessageEventListener(MessageEvents.POST_ROLLBACK_ADD, this);
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "deregisterForEvents");
  }
  
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.MessageEventListener#messageEventOccurred(int, com.ibm.ws.sib.processor.impl.interfaces.SIMPMessage, com.ibm.ws.sib.msgstore.Transaction)
   */
  public void messageEventOccurred(int event,
                                   SIMPMessage msg,
                                   TransactionCommon tran)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "messageEventOccurred", new Object[]{new Integer(event),msg,tran});      
    
    switch(event)
    {
      case MessageEvents.POST_COMMIT_REMOVE:
      case MessageEvents.POST_ROLLBACK_REMOVE:
      case MessageEvents.UNLOCKED:
        // Reset current tran for ordering
        if (currentTranId != null)
        {
          currentTranId = null;
          break;
        }
      case MessageEvents.POST_COMMIT_ADD:
      case MessageEvents.POST_ROLLBACK_ADD:
     
        //Test if the destination or this localisation of the destination need to
        //be deleted.
        testIfToBeDeleted(msg);  
        
        break;
        
      default:
      {
      
        final SIErrorException e = new SIErrorException(
          nls.getFormattedMessage(
            "INTERNAL_MESSAGING_ERROR_CWSIP0001",
            new Object[] {
              "com.ibm.ws.sib.processor.impl.store.itemstreams.PtoPMessageItemStream",
              "1:588:1.93.1.14"},
            null));
                
        FFDCFilter.processException(
          e,
          "com.ibm.ws.sib.processor.impl.store.itemstreams.PtoPMessageItemStream.messageEventOccurred",
          "1:594:1.93.1.14",
          this);
      
        SibTr.exception(tc, e);
        SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0001",
          new Object[] {
            "com.ibm.ws.sib.processor.impl.store.itemstreams.PtoPMessageItemStream",
            "1:601:1.93.1.14"});
      
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
          SibTr.exit(tc, "messageEventOccurred", e);
        throw e;
      }
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "messageEventOccurred");
  }  
  
  /**
   * @see com.ibm.ws.sib.processor.impl.interfaces.MessageEventListener#registerForEvents(SIMPMessage)
   */
  /**
   * <p>Test whether the item stream is marked as awaiting deletion
   * and if so, check if there are still items on the itemstream that
   * will block its removal.</p>
   * @param SIMPMessage 
   */
  public void testIfToBeDeleted(SIMPMessage msg)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "testIfToBeDeleted", msg);

    //Only bother checking if the itemstream is in the message store, as if it
    //isnt then its already been deleted
    if (isInStore())
    {
      if (isToBeDeleted() || (getDestinationHandler().isToBeDeleted()))
      {
        //As the itemstream is to-be-deleted, deregister for message events on the
        //message.  This will stop the cleanup of the message from driving this
        //test a second time.
        deregisterForEvents(msg);
  
        //Check if there are messages on the itemstream that can be cleaned up
        //now, or whether there are no unavailable items to stop the clean up.      
        try
        {
          Statistics statistics = getStatistics();
          long countOfAvailableItems =  statistics.getAvailableItemCount();
          long countOfUnavailableItems = statistics.getUnavailableItemCount();
          
          if ((countOfAvailableItems  > 0) ||
              (countOfUnavailableItems > 0))
          {
            // The itemstream is to be deleted and there are messages that can be processed, so add
            // the destination into the list of destinations awaiting cleanup that is maintained
            // by the destinationManager
            getDestinationHandler().getDestinationManager().markDestinationAsCleanUpPending(getDestinationHandler()); 
          }
        }
        catch(MessageStoreException e)
        {
          // No FFDC code needed
          //If its not in the messagestore, then the queue has already been deleted,
          //so do nothing
        }
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "testIfToBeDeleted");
  }

  /**
   * Attempt to re-allocate messages on the itemstream to another destination
   * localisation, the exception destination, or to discard the messages as 
   * appropriate.
   * @return true if everything removed
   */
  public abstract boolean reallocateMsgs();
  
  /**
   * Attempt to remove this item stream from the message store. The contents
   * should already have been removed before this is called. The MBean control
   * adapter is deregistered just before the itemStream is removed.
   * @return true if sucessfully removed
   */
  public void removeItemStream(Transaction transaction, long lockID) throws MessageStoreException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "removeItemStream", new Object[] { transaction, new Long(lockID) });

    deregisterControlAdapterMBean();
    remove(transaction, lockID);
  
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "removeItemStream");
  }
  
  /**
   * <p>If this localisation has a temporary UUID associated with it, it
   * will be replaced with the UUID passed in on the call.</p>
   * @param newUuid
   * @param transaction
   * @throws SIStoreException
   */
  public void replaceUuid(SIBUuid8 newUuid, Transaction transaction) throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "replaceUuid", new Object[] {newUuid, transaction});

    if (messagingEngineUuid.toString().equals(SIMPConstants.UNKNOWN_UUID))
    {
      messagingEngineUuid = newUuid;
      
      try
      {
        requestUpdate(transaction);
      }
      catch(MessageStoreException e)
      {
        // MessageStoreException shouldn't occur so FFDC.
        FFDCFilter.processException(
          e,
          "com.ibm.ws.sib.processor.impl.store.itemstreams.PtoPMessageItemStream.replaceUuid",
          "1:720:1.93.1.14",
          this);
        
        SibTr.exception(tc, e);
      
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
          SibTr.exit(tc, "replaceUuid", e);
      
        throw new SIResourceException(e);
      }
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "replaceUuid");
    
    return;
  }  
    
  /**
   * Mark this itemstream as no longer awaiting deletion and harden the indicator
   * @throws SIStoreException
   */  
  public void markAsToBeDeleted(Transaction transaction) throws SIResourceException 
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "markAsToBeDeleted", transaction);
      
    toBeDeleted = Boolean.TRUE;  
    
    try
    {
      this.requestUpdate(transaction);
    }
    catch (MessageStoreException e)
    {
      // MessageStoreException shouldn't occur so FFDC.
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.impl.store.itemstreams.PtoPMessageItemStream.markAsToBeDeleted",
        "1:759:1.93.1.14",
        this);
        
      SibTr.exception(tc, e);
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "markAsToBeDeleted", e);
      
      throw new SIResourceException(e);
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "markAsToBeDeleted");
      
    return;
  }
    
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.LocalizationPoint#getConsumerManager()
   */
  public ConsumerManager getConsumerManager()
  {
    return null;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.LocalizationPoint#dereferenceConsumerManager()
   */
  public void dereferenceConsumerManager()
  {
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.LocalizationPoint#isSendAllowed()
   */
  public boolean isSendAllowed()
  {
    return false;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.LocalizationPoint#createConsumerManager()
   */
  public ConsumerManager createConsumerManager()
  {
    return null;
  }
  
  /*
   *  (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.LocalizationPoint#getOldestMessageAge()
   */
  public long getOldestMessageAge()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getOldestMessageAge");
    MessageItem oldestItem=null;
    try
    {
      oldestItem = (MessageItem)this.findOldestItem();
    }
    catch (MessageStoreException e)
    {
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.impl.store.itemstreams.PtoPMessageItemStream.getOldestMessageAge",
        "1:825:1.93.1.14",
        this);        
            
      SibTr.exception(tc, e);       
    }
        
    long age = 0;
        
    if (null != oldestItem)
    {
      age = oldestItem.calculateWaitTimeUpdate(
        java.lang.System.currentTimeMillis());
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getOldestMessageAge", new Long(age));
    return age;
  }
  
  /*
   *  (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.LocalizationPoint#getAvailableMessageCount()
   */
  public long getAvailableMessageCount()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getAvailableMessageCount");
      
    long returnValue = -1;
    try
    {
      returnValue = getStatistics().getAvailableItemCount();
    }
    catch(MessageStoreException e)
    {
      // FFDC
      FFDCFilter.processException(
          e,
          "com.ibm.ws.sib.processor.impl.store.itemstreams.PtoPMessageItemStream.getAvailableMessageCount",
          "1:863:1.93.1.14",
          this);  
      
      SibTr.exception(tc, e);
    } 
      
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getAvailableMessageCount", new Long(returnValue));  
    return returnValue;    
  }
  
  /*
   *  (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.LocalizationPoint#getUnAvailableMessageCount()
   */
  public long getUnAvailableMessageCount()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getUnAvailableMessageCount");

    long returnValue = -1;
    try
    {
      returnValue = getStatistics().getUnavailableItemCount();
    }
    catch(MessageStoreException e)
    {
      // FFDC
      FFDCFilter.processException(
          e,
          "com.ibm.ws.sib.processor.impl.store.itemstreams.PtoPMessageItemStream.getUnAvailableMessageCount",
          "1:894:1.93.1.14",
          this);  
      
      SibTr.exception(tc, e);
    } 
          
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getUnAvailableMessageCount", new Long(returnValue));
    return returnValue;
  }
  
  /**
    * @param id
    */
   public void setCurrentTransaction(SIMPMessage msg, boolean isInDoubtOnRemoteConsumer) 
   {
     if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
       SibTr.entry(tc, "setCurrentTransaction", new Object[] {msg, new Boolean(isInDoubtOnRemoteConsumer)});
    
     if (currentTranId != null && !msg.getTransactionId().equals(currentTranId))
     {
       unableToOrder = true;
       
       // PK69943 Do not output a CWSIP0671 message here, as we do not know if
       // the destination is ordered or not. Leave that to the code that later checks unableToOrder
       if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
         SibTr.debug(tc, "Unable to order. Transaction: " + msg.getTransactionId() + " Current:" + currentTranId);
       }

     }
     else
       currentTranId = msg.getTransactionId();
    
     if (isInDoubtOnRemoteConsumer)
     {
       // Register callbacks to notify us when a remote consumer transaction completes
       msg.registerMessageEventListener(MessageEvents.UNLOCKED, this);
       msg.registerMessageEventListener(MessageEvents.POST_COMMIT_REMOVE,this);
     }
     else
     {
       // Register callbacks to notify us when a local consumer transaction completes
       msg.registerMessageEventListener(MessageEvents.POST_COMMIT_REMOVE, this);
       msg.registerMessageEventListener(MessageEvents.POST_ROLLBACK_REMOVE, this);
     }
    
     if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
       SibTr.exit(tc, "setCurrentTransaction");
   }
  
   /**
    * @param id
    */
   public PersistentTranId getOrderedActiveTran() 
   {
     if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
     {
       SibTr.entry(tc, "getOrderedActiveTran");
       SibTr.exit(tc, "getOrderedActiveTran", currentTranId);
     }
     return currentTranId;
   }
  
   /**
    * @param id
    */
   public boolean isUnableToOrder() 
   {
     if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
     {
       SibTr.entry(tc, "isUnableToOrder");
       SibTr.exit(tc, "isUnableToOrder", new Boolean(unableToOrder));
     }
     return unableToOrder;
   }
   
   /**
    * getActiveTransactions - This method iterates through all msgs on the
    * itemstream in Removing state and builds a list of all transaction ids
    * involved in the remove
    */
   public void getActiveTransactions(Set<PersistentTranId> transactionList) throws MessageStoreException
   {
     if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
       SibTr.entry(tc, "getActiveTransactions", transactionList);
     
     AbstractItem item = null;
     
     NonLockingCursor cursor = newNonLockingItemCursor(
         new Filter() {
            public boolean filterMatches(AbstractItem abstractItem) throws MessageStoreException {
              if (abstractItem.isRemoving())
                return true;
              else
                return false;
            }      
       
         });
     
     cursor.allowUnavailableItems();
    
     while(null != (item = cursor.next()))
     {
       // Add matching tran id to the set
       transactionList.add(item.getTransactionId());
     }     
     
     if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
       SibTr.exit(tc, "getActiveTransactions");
   }
   
   public long getTotalMsgCount()
   {
     if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
       SibTr.entry(tc, "getTotalMsgCount");

     long count = -1;
     try
     {
       count = getStatistics().getTotalItemCount();
     }
     catch(MessageStoreException e)
     {
       // FFDC
       FFDCFilter.processException(
           e,
           "com.ibm.ws.sib.processor.impl.store.itemstreams.PtoPMessageItemStream.getTotalMsgCount",
           "1:1021:1.93.1.14",
           this);  
       
       SibTr.exception(tc, e);
     }  
     
     if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
       SibTr.exit(tc, "getTotalMsgCount", count);
     return count;
   }
   
   // Mark the whole itemStream as blocked (so that the MBeans show the 'real' state of 
   // the items
   public void setBlocked(boolean blocked)
   {
     if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
       SibTr.entry(tc, "setBlocked", new Object[] {Boolean.valueOf(blocked), Boolean.valueOf(_blocked)});

     _blocked = blocked;
     
     if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
       SibTr.exit(tc, "setBlocked");
   }

   public boolean isBlocked()
   {
     if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
     {
       SibTr.entry(tc, "isBlocked");
       SibTr.exit(tc, "isBlocked", Boolean.valueOf(_blocked));
     }
     
     return _blocked;
   }

   /**
    * Prints the Message Details to the xml output.
    */
   public void xmlWriteOn(FormattedWriter writer) throws IOException
   {
     writer.newLine();
     writer.taggedValue("MEUuid", messagingEngineUuid);
     writer.newLine();
     writer.taggedValue("toBeDeleted", toBeDeleted);
   }
}
