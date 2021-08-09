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

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.sib.SIRCConstants;
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.websphere.sib.exception.SINotPossibleInCurrentConfigurationException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.admin.RuntimeEvent;
import com.ibm.ws.sib.mfp.JsMessage;
import com.ibm.ws.sib.msgstore.AbstractItem;
import com.ibm.ws.sib.msgstore.ItemStream;
import com.ibm.ws.sib.msgstore.MessageStoreException;
import com.ibm.ws.sib.msgstore.ReferenceStream;
import com.ibm.ws.sib.msgstore.transactions.Transaction;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.exceptions.SIMPControllableNotFoundException;
import com.ibm.ws.sib.processor.exceptions.SIMPException;
import com.ibm.ws.sib.processor.exceptions.SIMPRuntimeOperationFailedException;
import com.ibm.ws.sib.processor.impl.ExceptionDestinationHandlerImpl;
import com.ibm.ws.sib.processor.impl.MessageProcessor;
import com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler;
import com.ibm.ws.sib.processor.impl.interfaces.SIMPMessage;
import com.ibm.ws.sib.processor.impl.store.SIMPTransactionManager;
import com.ibm.ws.sib.processor.impl.store.items.MessageItem;
import com.ibm.ws.sib.processor.impl.store.items.MessageItemReference;
import com.ibm.ws.sib.processor.impl.store.itemstreams.PtoPMessageItemStream;
import com.ibm.ws.sib.processor.impl.store.itemstreams.SubscriptionItemStream;
import com.ibm.ws.sib.processor.runtime.SIMPQueuedMessageControllable;
import com.ibm.ws.sib.transactions.LocalTransaction;
import com.ibm.ws.sib.transactions.PersistentTranId;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * The adapter presented by a queued message to perform dynamic
 * control operations.
 */
public class QueuedMessage extends AbstractControlAdapter implements SIMPQueuedMessageControllable
{  
  private static final TraceNLS nls = TraceNLS.getTraceNLS(SIMPConstants.RESOURCE_BUNDLE);  

  private static final TraceComponent tc =
    SibTr.register(
      QueuedMessage.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);
  
  private long _messageID;
  private ItemStream _itemStream = null;
  private ReferenceStream _refStream = null;
  private DestinationHandler _destinationHandler;

  private SIMPTransactionManager _txManager;

  private MessageProcessor _messageProcessor;

  public QueuedMessage(SIMPMessage message,
                       DestinationHandler destination,
                       ItemStream is)
     throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "QueuedMessage",
        new Object[] { message, destination, is });

    try
    {
      _messageID = message.getID();
      _itemStream = is;
      _destinationHandler = destination;
      _messageProcessor = _destinationHandler.getMessageProcessor();
      _txManager = _messageProcessor.getTXManager();
    }
    catch(MessageStoreException e)
    {
      // FFDC
      FFDCFilter.processException(
          e,
          "com.ibm.ws.sib.processor.runtime.QueuedMessage.QueuedMessage",
          "1:129:1.66",
          this);  
                  
      SibTr.exception(tc, e); 
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "QueuedMessage", e);
      
      throw new SIResourceException(e);
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "QueuedMessage", this);
  }
  
  public QueuedMessage(SIMPMessage message,
                       DestinationHandler destination,
                       ReferenceStream rs)
  throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "QueuedMessage",
        new Object[] { message, destination, rs });

    try
    {
      _messageID = message.getID();
      _refStream = rs;
      _destinationHandler = destination;
      _messageProcessor = _destinationHandler.getMessageProcessor();
      _txManager = _messageProcessor.getTXManager();
    }
    catch(MessageStoreException e)
    {
      // FFDC
      FFDCFilter.processException(
          e,
          "com.ibm.ws.sib.processor.runtime.QueuedMessage.QueuedMessage",
          "1:168:1.66",
          this);  
                  
      SibTr.exception(tc, e); 
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "QueuedMessage", e);
      
      throw new SIResourceException(e);
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "QueuedMessage", this);
  }

  public JsMessage getJsMessage() throws SIMPControllableNotFoundException, SIMPException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getJsMessage");

    assertValidControllable();

    SIMPMessage message = null;
    try
    {
      message = getSIMPMessage();
    }
    catch (SIResourceException e)
    {
      // No FFDC code needed  
      SibTr.exception(tc, e);
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
        SibTr.exit(tc, "getJsMessage", e);
      throw new SIMPRuntimeOperationFailedException(e);
    } 
    
    if(message == null)
    {
      SIMPControllableNotFoundException finalE =
        new SIMPControllableNotFoundException(
          nls.getFormattedMessage(
            "INTERNAL_MESSAGING_ERROR_CWSIP0005",
            new Object[] {"QueuedMessage.getJsMessage",
                          "1:211:1.66",
                          Long.toString(_messageID)},
            null));
            
      SibTr.exception(tc, finalE);
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "getJsMessage", finalE);
      throw finalE; 
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getJsMessage");
    return message.getMessage();
  }
  
  private SIMPMessage getSIMPMessage() throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getSIMPMessage");

    SIMPMessage message = null;
    try
    {
      if (_itemStream != null)
        message = (SIMPMessage)_itemStream.findById(_messageID);
      else if (_refStream != null)
        message = (SIMPMessage)_refStream.findById(_messageID);
    }
    catch(MessageStoreException e)
    {
      // FFDC
      FFDCFilter.processException(
          e,
          "com.ibm.ws.sib.processor.runtime.QueuedMessage.getSIMPMessage",
          "1:245:1.66",
          this);  
                  
      SibTr.exception(tc, e); 
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "getSIMPMessage", e);
      
      throw new SIResourceException(e);
    }
  
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getSIMPMessage", message);
    return message;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPQueuedMessageControllable#getState()
   * 
   * Gets the state from the message on the item stream.
   * If the item is not returned or the item isn't locked then we return State.UNLOCKED.
   */
  public String getState() throws SIMPException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getState");

    String state = State.UNLOCKED;
    
    assertValidControllable();
    SIMPMessage msg = null;
    try
    {
      msg = getSIMPMessage();
    }
    catch (SIResourceException e)
    {
      // No FFDC code needed    
      SIMPRuntimeOperationFailedException finalE =
        new SIMPRuntimeOperationFailedException(
          nls.getFormattedMessage(
            "INTERNAL_MESSAGING_ERROR_CWSIP0003",
            new Object[] {"QueuedMessage.getState",
                          "1:288:1.66",
                          e,
                          Long.toString(_messageID)},
            null), e,
            "INTERNAL_MESSAGING_ERROR_CWSIP0003", new Object[] {Long.toString(_messageID)});
  
      SibTr.exception(tc, finalE);
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
        SibTr.exit(tc, "getState", finalE);
      throw finalE;
    }
    
    if(msg!=null)  
    {
      //defect 262036: locked messages are displayed as locked, but so 
      //are messages that are no longer available 
      //e.g. messages being removed
      AbstractItem msgItem = (AbstractItem)msg;
      if(!msgItem.isAvailable())
      {
        // This message is locked because a consumer (MDB probably) has
        // hidden it due to repeated failures to process it.
        if(msg.isHidden())
          state = State.PENDING_RETRY;         
        // It's being added under a tran (probably already in prepare)
        else if(msgItem.isAdding())
          state = State.COMMITTING;
        // It's being deleted under a tran
        else if(msgItem.isRemoving())
          state = State.REMOVING;
        // It's currently locked for a remote getter
        else if(msgItem.isPersistentlyLocked())
          state = State.REMOTE_LOCKED;
        else
          state = State.LOCKED;
      }
      else
      {
        // Even if the mesage looks available, the whole stream may be blocked
        // by a ConsumerDispatcher due to a failing message not being exceptioned
        // so this mesasge is actually unavailable for the time being
        if(isStreamBlocked())
          state = State.BLOCKED;
      }
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getState", state);
    return state;
  }
  
  private boolean isStreamBlocked()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "isStreamBlocked");
    
    boolean blocked = false;
    
    // Only an item/refStream with a consumerDispatcher has the ability
    // to block it.
    if (_itemStream != null)
    {
      if(_itemStream instanceof PtoPMessageItemStream)
        blocked = ((PtoPMessageItemStream)_itemStream).isBlocked();
    }
    else if (_refStream != null)
    {
      if(_refStream instanceof SubscriptionItemStream)
        blocked = ((SubscriptionItemStream)_refStream).isBlocked();
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "isStreamBlocked", Boolean.valueOf(blocked));
    
    return blocked;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPQueuedMessageControllable#getTransactionID()
   */
  public String getTransactionId() throws SIMPException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getTransactionId");
    
    assertValidControllable();    
    
    String id = null;
    SIMPMessage msg = null;
    try
    {
      msg = getSIMPMessage();
    }
    catch (SIResourceException e)
    {
      // No FFDC code needed  
      SIMPRuntimeOperationFailedException finalE =
        new SIMPRuntimeOperationFailedException(
          nls.getFormattedMessage(
            "INTERNAL_MESSAGING_ERROR_CWSIP0003",
            new Object[] {"QueuedMessage.getTransactionId",
                          "1:389:1.66",
                          e,
                          Long.toString(_messageID)},
            null), e,
            "INTERNAL_MESSAGING_ERROR_CWSIP0003", new Object[] {Long.toString(_messageID)});
  
      SibTr.exception(tc, finalE);
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
        SibTr.exit(tc, "getTransactionId", finalE);
      throw finalE;
    } 
    
    if (msg != null)
    {
      PersistentTranId pTranId = msg.getTransactionId();
      if (pTranId != null)
      {
        id = pTranId.toTMString(); 
      }
    }
      
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getTransactionId", id);
    return id;
  }
  
  /**
   * Copy the message to the exception destination
   * @param tran can be null
   * @throws SIResourceException 
   * @throws SINotPossibleInCurrentConfigurationException 
   */
  public void copyMessageToExceptionDestination(LocalTransaction tran) 
  throws SINotPossibleInCurrentConfigurationException, SIResourceException
  
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "copyMessageToExceptionDestination", tran);
    
    SIMPMessage msg = getSIMPMessage();
    ExceptionDestinationHandlerImpl edh = null;
    
    if (_destinationHandler.isLink())
      edh = new ExceptionDestinationHandlerImpl(_destinationHandler);
    else {
      edh = new ExceptionDestinationHandlerImpl(null, _messageProcessor);
      edh.setDestination(_destinationHandler);
    }
    edh.sendToExceptionDestination(
        msg.getMessage(),
        null,
        tran,
        SIRCConstants.SIRC0036_MESSAGE_ADMINISTRATIVELY_REROUTED_TO_EXCEPTION_DESTINATION,
        null,
        new String[]{""+_messageID,
                     _destinationHandler.getName(),
                     _messageProcessor.getMessagingEngineName()});
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "copyMessageToExceptionDestination");    
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPQueuedMessageControllable#removeMessage(boolean)
   */
  public void moveMessage(boolean discard)
    throws SIMPControllableNotFoundException,
           SIMPRuntimeOperationFailedException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "moveMessage", new Object[] { Boolean.valueOf(discard) });

    assertValidControllable();

    LocalTransaction tran = _txManager.createLocalTransaction(false);
    SIMPMessage msg = null;
    try
    {
      msg = getSIMPMessage();
    }
    catch (SIResourceException e)
    {
      // No FFDC code needed    
      SIMPRuntimeOperationFailedException finalE =
        new SIMPRuntimeOperationFailedException(
          nls.getFormattedMessage(
            "INTERNAL_MESSAGING_ERROR_CWSIP0003",
            new Object[] {"QueuedMessage.moveMessage",
                          "1:473:1.66",
                          e,
                          Long.toString(_messageID)},
            null), e,
            "INTERNAL_MESSAGING_ERROR_CWSIP0003", new Object[] {Long.toString(_messageID)});
  
      SibTr.exception(tc, finalE);
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
        SibTr.exit(tc, "moveMessage", finalE);
      throw finalE;
    } 
    if(!discard)
    { 
      try
      {
        copyMessageToExceptionDestination(tran);  
      }
      catch (Exception e)
      {
        // No FFDC code needed 
        SIMPRuntimeOperationFailedException finalE =
          new SIMPRuntimeOperationFailedException(e);
        
        SibTr.exception(tc, finalE);
        
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
          SibTr.exit(tc, "moveMessage", finalE);
        throw finalE; 
      }

      discard = true;
    }
    
    if (discard)
    {      
      try
      {
        if(msg.isInStore())
        {
          if(msg.getLockID()==AbstractItem.NO_LOCK_ID)
          {
            //lock the message
            // Cache a lock ID to lock the items with          
            long lockID = _messageProcessor.getMessageStore().getUniqueLockID(AbstractItem.STORE_NEVER);
            msg.lockItemIfAvailable(lockID);
          }
          Transaction msTran = _messageProcessor.resolveAndEnlistMsgStoreTransaction(tran);
          msg.remove(msTran, msg.getLockID());  
        }
      }
      catch (Exception e)
      {
        // No FFDC code needed
        
        SIMPRuntimeOperationFailedException finalE = null;
        
        boolean adding = false;
        boolean removing = false;      
        
        if (msg instanceof MessageItem)
        {
          adding = ((MessageItem)msg).isAdding();
          removing = ((MessageItem)msg).isRemoving();
        }
        else
        {
          adding = ((MessageItemReference)msg).isAdding();
          removing = ((MessageItemReference)msg).isRemoving();          
        }
        
        if (adding)
        {
          // If the message is adding it probably means that 
          // we have an indoubt transaction which hasn't been committed
          finalE =
            new SIMPRuntimeOperationFailedException(
              nls.getFormattedMessage(
                "MESSAGE_INDOUBT_WARNING_CWSIP0361",
                new Object[] {Long.toString(_messageID),
                              _destinationHandler.getName()},
                null), 
              e,
              "MESSAGE_INDOUBT_WARNING_CWSIP0361", new Object[] {Long.toString(_messageID),
                _destinationHandler.getName() });          
        }         
        else if (removing)
        {
          // If the message is deleting it probably means that 
          // we have delivered the message to a consumer
          finalE =
            new SIMPRuntimeOperationFailedException(
              nls.getFormattedMessage(
                "MESSAGE_INDOUBT_WARNING_CWSIP0362",
                new Object[] {Long.toString(_messageID),
                              _destinationHandler.getName()},
                null), 
              e,
              "MESSAGE_INDOUBT_WARNING_CWSIP0362", new Object[] { Long.toString(_messageID),
                  _destinationHandler.getName() });          
        }         
        else
        {
          // Unexpected exception
          FFDCFilter.processException(
            e,
            "com.ibm.ws.sib.processor.runtime.QueuedMessage.moveMessage",
            "1:579:1.66",
            this);
    
          finalE =
            new SIMPRuntimeOperationFailedException(
              nls.getFormattedMessage(
                "INTERNAL_MESSAGING_ERROR_CWSIP0003",
                new Object[] {"QueuedMessage.removeMessage",
                              "1:587:1.66",
                              e,
                              Long.toString(_messageID)},
                null), e,
                "INTERNAL_MESSAGING_ERROR_CWSIP0003", new Object[] { Long.toString(_messageID)});
        }
        
        try
        {
          tran.rollback();
        }
        catch (SIException ee)
        {
          FFDCFilter.processException(
            ee,
            "com.ibm.ws.sib.processor.runtime.QueuedMessage.moveMessage",
            "1:603:1.66",
            this);
          
          SibTr.exception(tc, ee);
        }
        

        SibTr.exception(tc, finalE);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
          SibTr.exit(tc, "moveMessage", finalE);
        throw finalE;             
      }
    }
    try
    {
      tran.commit();
    }
    catch (SIException e)
    {
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.runtime.QueuedMessage.moveMessage",
        "1:625:1.66",
        this);

      SIMPRuntimeOperationFailedException finalE =
        new SIMPRuntimeOperationFailedException(
          nls.getFormattedMessage(
            "INTERNAL_MESSAGING_ERROR_CWSIP0003",
            new Object[] {"QueuedMessage.removeMessage",
                          "1:633:1.66",
                          e,
                          Long.toString(_messageID)},
            null), e,
            "INTERNAL_MESSAGING_ERROR_CWSIP0003", new Object[] {Long.toString(_messageID)});

      SibTr.exception(tc, finalE);
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "moveMessage", finalE);
      throw finalE;
    }   
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "moveMessage"); 
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPControllable#getId()
   */
  public String getId()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getId");
      SibTr.exit(tc, "getId", Long.toString(_messageID));
    }
    return ""+_messageID;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPControllable#getName()
   */
  public String getName()
  {
    return null;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.AbstractControllable#checkValidControllable()
   */
  public void assertValidControllable() throws SIMPControllableNotFoundException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "assertValidControllable");
    
    if(_messageID == AbstractItem.NO_ID)
    {
      SIMPControllableNotFoundException finalE =
        new SIMPControllableNotFoundException(
          nls.getFormattedMessage(
            "INTERNAL_MESSAGING_ERROR_CWSIP0005",
            new Object[] {"QueuedMessage.assertValidControllable",
                          "1:683:1.66",
                          Long.toString(_messageID)},
            null));
  
      SibTr.exception(tc, finalE);
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "assertValidControllable", finalE);
      throw finalE;
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "assertValidControllable");
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.AbstractControllable#dereferenceControllable()
   */
  public void dereferenceControllable()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "dereferenceControllable");
    
    _messageID = AbstractItem.NO_ID;
    _itemStream = null;
    _refStream = null;   
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "dereferenceControllable");
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.ControlAdapter#registerControlAdapterAsMBean()
   */
  public void registerControlAdapterAsMBean()
  {
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.ControlAdapter#deregisterControlAdapterMBean()
   */
  public void deregisterControlAdapterMBean()
  {
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.ControlAdapter#runtimeEventOccurred(com.ibm.ws.sib.admin.RuntimeEvent)
   */
  public void runtimeEventOccurred(RuntimeEvent event)
  {
  }
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPQueuedMessageControllable#getSequenceID()
   */
  public long getSequenceID()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getSequenceID");
    long sequenceID=0;
    try
    {
      SIMPMessage msg = getSIMPMessage();
      if(msg!=null)
      {
        sequenceID = msg.getMessage().getGuaranteedValueValueTick();
      }
    }
    catch (SIResourceException e)
    {
      // No FFDC code needed
      SibTr.exception(tc, e);
    } 
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getSequenceID", Long.valueOf(sequenceID));
    return sequenceID;
  }
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPQueuedMessageControllable#getPreviousSequenceId()
   */
  public long getPreviousSequenceId()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getPreviousSequenceId");
    long previousSequenceID=0;
    try
    {
      SIMPMessage msg = getSIMPMessage();
      if(msg!=null)
      {
        previousSequenceID = msg.getMessage().getGuaranteedValueStartTick()-1;
      }
    }
    catch (SIResourceException e)
    {
      // No FFDC code needed
      SibTr.exception(tc, e);
    } 
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getPreviousSequenceId", Long.valueOf(previousSequenceID));
    return previousSequenceID;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPQueuedMessageControllable#getApproximateLength()
   */
  public long getApproximateLength() {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getApproximateLength");
    long length=0;
    try
    {
      SIMPMessage msg = getSIMPMessage();
      if(msg!=null)
      {
        length = msg.getMessage().getApproximateLength();
      }
    }
    catch (SIResourceException e)
    {
      // No FFDC code needed
      SibTr.exception(tc, e);
    } 
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getApproximateLength", Long.valueOf(length));
    return length;
  }
}
