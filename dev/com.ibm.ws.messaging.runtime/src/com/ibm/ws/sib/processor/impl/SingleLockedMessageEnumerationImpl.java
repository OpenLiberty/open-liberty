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

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIIncorrectCallException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.mfp.JsMessage;
import com.ibm.ws.sib.mfp.MessageCopyFailedException;
import com.ibm.ws.sib.processor.MPLockedMessageEnumeration;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.impl.corespitrace.CoreSPILockedMessageEnumeration;
import com.ibm.ws.sib.processor.impl.interfaces.SIMPMessage;
import com.ibm.ws.sib.processor.utils.UserTrace;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.ConsumerSession;
import com.ibm.wsspi.sib.core.SIBusMessage;
import com.ibm.wsspi.sib.core.SITransaction;
import com.ibm.wsspi.sib.core.exception.SIConnectionLostException;
import com.ibm.wsspi.sib.core.exception.SISessionDroppedException;
import com.ibm.wsspi.sib.core.exception.SISessionUnavailableException;

/**
 * @author tevans
 *
 */
final class SingleLockedMessageEnumerationImpl implements MPLockedMessageEnumeration
{
  // NLS for component
  private static final TraceNLS nls =
    TraceNLS.getTraceNLS(SIMPConstants.RESOURCE_BUNDLE);
  
  // NLS for CWSIR component
  private static final TraceNLS nls_cwsir =
    TraceNLS.getTraceNLS(SIMPConstants.CWSIR_RESOURCE_BUNDLE);
   
 private static TraceComponent tc =
    SibTr.register(
      SingleLockedMessageEnumerationImpl.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);

   
  private JSLocalConsumerPoint _localConsumerPoint;
  private boolean _isPubsub;
  private boolean _seenSingleMessage = false;
  private boolean _messageAvailable = false;
  private SIMPMessage _singleMessage = null;
  // true if currently in a consumeMessages callback
  private boolean _validState = true;

  SingleLockedMessageEnumerationImpl(
    JSLocalConsumerPoint localConsumerPoint,
    SIMPMessage message)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "SingleLockedMessageEnumerationImpl", new Object[] { localConsumerPoint,
                                                                           message });
    _localConsumerPoint = localConsumerPoint;    
    _isPubsub = localConsumerPoint.getConsumerManager().getDestination().isPubSub();
    
    _singleMessage = message;

    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() &&
        (_singleMessage != null))
      SibTr.debug(_localConsumerPoint, tc, "verboseMsg OUT : " + _singleMessage.getMessage().toVerboseString());
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "SingleLockedMessageEnumerationImpl", this);
  }
  
  void clearMessage()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "clearMessage");
      SibTr.exit(tc, "clearMessage");
    }
  
    _validState = false;
    _singleMessage = null;
  }
  
  void newMessage(SIMPMessage message)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "newMessage", message);
      SibTr.exit(tc, "newMessage");
    }
    
    _validState = true;
    _singleMessage = message;
    _seenSingleMessage = false;
    _messageAvailable = false;
  }

  /* (non-Javadoc)
   * @see com.ibm.wsspi.sib.core.LockedMessageEnumeration#nextLocked()
   */
  public SIBusMessage nextLocked()
  throws SISessionUnavailableException, SISessionDroppedException,
         SIResourceException, SIConnectionLostException, 
         SIErrorException, SIIncorrectCallException
  {
    if (TraceComponent.isAnyTracingEnabled() && CoreSPILockedMessageEnumeration.tc.isEntryEnabled())
      SibTr.entry(CoreSPILockedMessageEnumeration.tc, "nextLocked", this);

    checkValidState("nextLocked");
          
    _localConsumerPoint.checkNotClosed();  

    if (_seenSingleMessage || _singleMessage == null)
    {
      _messageAvailable = false;
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "nextLocked", null);
      return null;
    }
    _seenSingleMessage = true;
    
    JsMessage msg = null;
    
    // Because this message is not in the message store there is no need to copy it
    // before giving it to a caller unless this is pubsub, in which case there may be
    // other subscriptions referencing the same message, in which case we must copy the
    // message (unless the caller indicates that they won't be altering it)
    if(_isPubsub &&
       ((ConnectionImpl)(_localConsumerPoint.getConsumerSession().getConnection())).getMessageCopiedWhenReceived())
    {
      try
      {
        msg = (_singleMessage.getMessage()).getReceived();
      }
      catch (MessageCopyFailedException e)
      {
        FFDCFilter.processException(
          e,
          "com.ibm.ws.sib.processor.impl.SingleLockedMessageEnumerationImpl.nextLocked",
          "1:172:1.44",
          this);
            
        SibTr.exception(tc, e);
        SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
          new Object[] {
            "com.ibm.ws.sib.processor.impl.SingleLockedMessageEnumerationImpl",
            "1:179:1.44",
            e });
          
        _seenSingleMessage = false;
        _messageAvailable = false;
  
        if (TraceComponent.isAnyTracingEnabled() && CoreSPILockedMessageEnumeration.tc.isEntryEnabled())
          SibTr.exit(CoreSPILockedMessageEnumeration.tc, "nextLocked", e);
  
        throw new SIResourceException(
          nls.getFormattedMessage(
            "INTERNAL_MESSAGING_ERROR_CWSIP0002",
            new Object[] {
              "com.ibm.ws.sib.processor.impl.SingleLockedMessageEnumerationImpl",
              "1:193:1.44",
              e },
            null),
          e);
      }
    }
    else
      msg = _singleMessage.getMessage();
          
    //indicate that there is a messageAvailable
    _messageAvailable = true;

    if (TraceComponent.isAnyTracingEnabled() && UserTrace.tc_mt.isDebugEnabled())                                   
      UserTrace.trace_Receive(null, 
                              msg, 
                              _localConsumerPoint.getConsumerSession().getDestinationAddress(), 
                              _localConsumerPoint.getConsumerSession().getIdInternal());
    
    if (TraceComponent.isAnyTracingEnabled() && CoreSPILockedMessageEnumeration.tc.isEntryEnabled())
          SibTr.exit(CoreSPILockedMessageEnumeration.tc, "nextLocked", msg);
    
    return msg;
  }

  /* (non-Javadoc)
   * @see com.ibm.wsspi.sib.core.LockedMessageEnumeration#unlockCurrent()
   */
  public void unlockCurrent()
  throws SISessionUnavailableException, SISessionDroppedException,
         SIIncorrectCallException,
         SIErrorException
  {
    if (TraceComponent.isAnyTracingEnabled() && CoreSPILockedMessageEnumeration.tc.isEntryEnabled())
      SibTr.entry(CoreSPILockedMessageEnumeration.tc, "unlockCurrent", this);
      
    deleteCurrent(null);
    
    if (TraceComponent.isAnyTracingEnabled() && CoreSPILockedMessageEnumeration.tc.isEntryEnabled())
      SibTr.exit(CoreSPILockedMessageEnumeration.tc, "unlockCurrent");
  }
   
  /* (non-Javadoc)
   * @see com.ibm.wsspi.sib.core.LockedMessageEnumeration#unlockCurrent(boolean)
   */
  public void unlockCurrent(boolean incrementUnlockCount)
  throws SISessionUnavailableException, SISessionDroppedException,
         SIIncorrectCallException,
         SIErrorException
  {
    if (TraceComponent.isAnyTracingEnabled() && CoreSPILockedMessageEnumeration.tc.isEntryEnabled())
      SibTr.entry(CoreSPILockedMessageEnumeration.tc, "unlockCurrent", new Object[] {this, new Boolean(incrementUnlockCount)});
      
    deleteCurrent(null);
    
    if (TraceComponent.isAnyTracingEnabled() && CoreSPILockedMessageEnumeration.tc.isEntryEnabled())
      SibTr.exit(CoreSPILockedMessageEnumeration.tc, "unlockCurrent");
  }

  /* (non-Javadoc)
   * @see com.ibm.wsspi.sib.core.LockedMessageEnumeration#deleteCurrent(com.ibm.wsspi.sib.core.SITransaction)
   */
  public void deleteCurrent(SITransaction simpTran)
  throws SISessionUnavailableException, SISessionDroppedException,
         SIIncorrectCallException,
         SIErrorException
  {
    if (TraceComponent.isAnyTracingEnabled() && CoreSPILockedMessageEnumeration.tc.isEntryEnabled())
      SibTr.entry(CoreSPILockedMessageEnumeration.tc, "deleteCurrent", this);
      
    checkValidState("deleteCurrent");
    
    _localConsumerPoint.checkNotClosed();
    
    if (!_messageAvailable)
    {
      if (TraceComponent.isAnyTracingEnabled() && CoreSPILockedMessageEnumeration.tc.isEntryEnabled())
        SibTr.exit(CoreSPILockedMessageEnumeration.tc, "deleteCurrent", "Invalid current Message");
        
      throw new SIIncorrectCallException(
        nls.getFormattedMessage("INVALID_MESSAGE_ERROR_CWSIP0191", 
          new Object[] { _localConsumerPoint.getConsumerManager().getDestination().getName(),
            _localConsumerPoint.getConsumerManager().getMessageProcessor().getMessagingEngineName() }, 
          null));
    }
    
    _singleMessage = null;
    _messageAvailable = false;
    
    if (TraceComponent.isAnyTracingEnabled() && CoreSPILockedMessageEnumeration.tc.isEntryEnabled())
      SibTr.exit(CoreSPILockedMessageEnumeration.tc, "deleteCurrent");
  }

  /* (non-Javadoc)
   * @see com.ibm.wsspi.sib.core.LockedMessageEnumeration#deleteSeen(com.ibm.wsspi.sib.core.SITransaction)
   */
  public void deleteSeen(SITransaction simpTran)
  throws SISessionUnavailableException, SISessionDroppedException,
         SIIncorrectCallException,
         SIErrorException
  {
    if (TraceComponent.isAnyTracingEnabled() && CoreSPILockedMessageEnumeration.tc.isEntryEnabled())
      SibTr.entry(CoreSPILockedMessageEnumeration.tc, "deleteSeen", 
        new Object[] { this, simpTran });        
      
    deleteCurrent(simpTran);
    
    if (TraceComponent.isAnyTracingEnabled() && CoreSPILockedMessageEnumeration.tc.isEntryEnabled())
      SibTr.exit(CoreSPILockedMessageEnumeration.tc, "deleteSeen");
  }

  /* (non-Javadoc)
   * @see com.ibm.wsspi.sib.core.LockedMessageEnumeration#getConsumerSession()
   */
  public ConsumerSession getConsumerSession() 
  throws SISessionUnavailableException, SISessionDroppedException,
         SIIncorrectCallException

  {
    if (TraceComponent.isAnyTracingEnabled() && CoreSPILockedMessageEnumeration.tc.isEntryEnabled())
      SibTr.entry(CoreSPILockedMessageEnumeration.tc, "getConsumerSession", this);
      
    checkValidState("getConsumerSession");
          
    _localConsumerPoint.checkNotClosed();
    
    if (TraceComponent.isAnyTracingEnabled() && CoreSPILockedMessageEnumeration.tc.isEntryEnabled())
      SibTr.exit(CoreSPILockedMessageEnumeration.tc, "getConsumerSession", 
        _localConsumerPoint.getConsumerSession());
    
    return _localConsumerPoint.getConsumerSession();
  }

  /* (non-Javadoc)
   * @see com.ibm.wsspi.sib.core.LockedMessageEnumeration#resetCursor()
   */
  public void resetCursor() 
  throws SISessionUnavailableException, SISessionDroppedException,
         SIErrorException, SIIncorrectCallException
  {
    if (TraceComponent.isAnyTracingEnabled() && CoreSPILockedMessageEnumeration.tc.isEntryEnabled())
      SibTr.entry(CoreSPILockedMessageEnumeration.tc, "resetCursor", this);
          
    checkValidState("resetCursor");
          
    _localConsumerPoint.checkNotClosed();
    
    _seenSingleMessage = false;
    
    if (TraceComponent.isAnyTracingEnabled() && CoreSPILockedMessageEnumeration.tc.isEntryEnabled())
      SibTr.exit(CoreSPILockedMessageEnumeration.tc, "resetCursor");
  }

  /* (non-Javadoc)
   * @see com.ibm.wsspi.sib.core.LockedMessageEnumeration#getRemainingMessageCount()
   */
  public int getRemainingMessageCount() 
  throws SISessionUnavailableException, SISessionDroppedException,
         SIIncorrectCallException
  {
    if (TraceComponent.isAnyTracingEnabled() && CoreSPILockedMessageEnumeration.tc.isEntryEnabled())
      SibTr.entry(CoreSPILockedMessageEnumeration.tc, "getRemainingMessageCount", this);
          
    checkValidState("getRemainingMessageCount");
          
    _localConsumerPoint.checkNotClosed();
    
    int remaining = 0;
    
    if(!_seenSingleMessage)
    {
      remaining = 1;
    }
    
    if (TraceComponent.isAnyTracingEnabled() && CoreSPILockedMessageEnumeration.tc.isEntryEnabled())
      SibTr.exit(CoreSPILockedMessageEnumeration.tc, "getRemainingMessageCount", 
        new Integer(remaining));    
      
    return remaining;
  }
  
  private void checkValidState(String method)
  throws SIIncorrectCallException
  {
    if(!_validState)
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(tc, "checkValidState"); 
        
      SIIncorrectCallException e = new SIIncorrectCallException(
        nls_cwsir.getFormattedMessage("LME_ERROR_CWSIR0131",  
          new Object[] { method }, 
          null));
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "checkValidState", e);
      
      throw e;
    }
  }

  /* (non-Javadoc)
   * @see com.ibm.wsspi.sib.core.LockedMessageEnumeration#hasNext()
   */
  public boolean hasNext() throws SISessionUnavailableException, SISessionDroppedException, SIIncorrectCallException
  {
    if (TraceComponent.isAnyTracingEnabled() && CoreSPILockedMessageEnumeration.tc.isEntryEnabled())
      SibTr.entry(CoreSPILockedMessageEnumeration.tc, "hasNext", this);
          
    checkValidState("hasNext");
          
    _localConsumerPoint.checkNotClosed();
    
    boolean hasNext = false;
    
    if(!_seenSingleMessage)
    {
      hasNext = true;
    }
    
    if (TraceComponent.isAnyTracingEnabled() && CoreSPILockedMessageEnumeration.tc.isEntryEnabled())
      SibTr.exit(CoreSPILockedMessageEnumeration.tc, "hasNext", new Boolean(hasNext));    
      
    return hasNext;
  }

  /**
   * Peek at the next message on the enumeration.
   * 
   * In the SingleLockedMessageEnumeration there is only one message.
   * If the nextLocked hasn't been called then the message will be returned,
   * otherwise a null will be returned.
   */
  public SIBusMessage peek() 
  
  throws SISessionUnavailableException, SIResourceException, SIIncorrectCallException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "peek");
    
    checkValidState("peek");
    
    _localConsumerPoint.checkNotClosed();

    SIBusMessage nextMessage = null;
    
    if(!_seenSingleMessage)
    {
      // Because this message is not in the message store there is no need to copy it
      // before giving it to a caller unless this is pubsub, in which case there may be
      // other subscriptions referencing the same message, in which case we must copy the
      // message (unless the caller indicates that they won't be altering it)
      if(_isPubsub &&
         ((ConnectionImpl)(_localConsumerPoint.getConsumerSession().getConnection())).getMessageCopiedWhenReceived())
      {
        try
        {
          nextMessage = (_singleMessage.getMessage()).getReceived();
        }
        catch (MessageCopyFailedException e)
        {
          FFDCFilter.processException(
            e,
            "com.ibm.ws.sib.processor.impl.SingleLockedMessageEnumerationImpl.peek",
            "1:456:1.44",
            this);
              
          SibTr.exception(tc, e);
          SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
            new Object[] {
              "com.ibm.ws.sib.processor.impl.SingleLockedMessageEnumerationImpl",
              "1:463:1.44",
              e });
            
          _seenSingleMessage = false;
          _messageAvailable = false;
    
          if (TraceComponent.isAnyTracingEnabled() && CoreSPILockedMessageEnumeration.tc.isEntryEnabled())
            SibTr.exit(CoreSPILockedMessageEnumeration.tc, "peek", e);
    
          throw new SIResourceException(
            nls.getFormattedMessage(
              "INTERNAL_MESSAGING_ERROR_CWSIP0002",
              new Object[] {
                "com.ibm.ws.sib.processor.impl.SingleLockedMessageEnumerationImpl",
                "1:477:1.44",
                e },
              null),
            e);
        }
      }
      else
        nextMessage = _singleMessage.getMessage();
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "peek", nextMessage);
    return nextMessage;
  }
}
