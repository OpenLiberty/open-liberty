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

// Import required classes.
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.sib.SIDestinationAddress;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIIncorrectCallException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.exceptions.SIMPMessageNotLockedException;
import com.ibm.ws.sib.processor.impl.corespitrace.CoreSPIBifurcatedConsumerSession;
import com.ibm.ws.sib.processor.impl.interfaces.LocalConsumerPoint;
import com.ibm.ws.sib.processor.impl.interfaces.MPDestinationSession;
import com.ibm.ws.sib.processor.utils.SIMPUtils;
import com.ibm.ws.sib.transactions.TransactionCommon;
import com.ibm.ws.sib.utils.SIBUuid12;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.BifurcatedConsumerSession;
import com.ibm.wsspi.sib.core.SIBusMessage;
import com.ibm.wsspi.sib.core.SICoreConnection;
import com.ibm.wsspi.sib.core.SIMessageHandle;
import com.ibm.wsspi.sib.core.SITransaction;
import com.ibm.wsspi.sib.core.exception.SIConnectionLostException;
import com.ibm.wsspi.sib.core.exception.SISessionDroppedException;
import com.ibm.wsspi.sib.core.exception.SISessionUnavailableException;

/**
 */
public class BifurcatedConsumerSessionImpl implements BifurcatedConsumerSession, MPDestinationSession
{
  private static final TraceComponent tc =
    SibTr.register(
      BifurcatedConsumerSessionImpl.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);
      
  // NLS for component
  private static final TraceNLS nls =
    TraceNLS.getTraceNLS(SIMPConstants.RESOURCE_BUNDLE);

  /**
   * The ConsumerSession for which this Bifurcated consumer is attached 
   */
  private ConsumerSessionImpl _consumerSession;
  /**
   * The connection that this consumer is attached to
   */
  private ConnectionImpl _connection;
  /**
   * Indicates that this consumer is closed or not.
   */
  private boolean _closed;
  /**
   * The local consumer point for the associated consumer session
   */
  private LocalConsumerPoint _localConsumerPoint;

  private SIBUuid12 uuid;
  
  /**
   * Constructor for a new BifurcatedConsumerSession
   * 
   * @param session The consumer session that this bifurcated consumer is attached to.
   */
  BifurcatedConsumerSessionImpl(ConnectionImpl connection,
                                ConsumerSessionImpl session)
  {
    if (tc.isEntryEnabled()) 
      SibTr.entry(tc, "BifurcatedConsumerSessionImpl", 
        new Object[]{connection, session});
      
    _consumerSession = session;  
    _connection = connection;
    _closed = false;
    _localConsumerPoint = _consumerSession.getLocalConsumerPoint();
    _consumerSession.attachBifurcatedConsumer(this);
    this.uuid = new SIBUuid12();
    
    if (tc.isEntryEnabled()) SibTr.exit(tc, "BifurcatedConsumerSessionImpl", this);
  }
  

  /* (non-Javadoc)
   * @see com.ibm.wsspi.sib.core.BifurcatedConsumerSession#deleteSet(long[], com.ibm.wsspi.sib.core.SITransaction)
   */
  public void deleteSet(SIMessageHandle[] msgHandles, SITransaction transaction) 
  
  throws SIMPMessageNotLockedException, SISessionUnavailableException, SIConnectionLostException, SIIncorrectCallException, SIResourceException, SIErrorException
  {
    if (CoreSPIBifurcatedConsumerSession.tc.isEntryEnabled()) 
      SibTr.entry(CoreSPIBifurcatedConsumerSession.tc, "deleteSet",
        new Object[]{this, SIMPUtils.messageHandleArrayToString(msgHandles), transaction});
    
    // Check that we aren't closed
    checkNotClosed();
    
    _localConsumerPoint.processMsgSet(msgHandles, (TransactionCommon)transaction, this, false, true, false, true);

    if (CoreSPIBifurcatedConsumerSession.tc.isEntryEnabled()) 
      SibTr.exit(CoreSPIBifurcatedConsumerSession.tc, "deleteSet");    
  }

  /* (non-Javadoc)
   * @see com.ibm.wsspi.sib.core.BifurcatedConsumerSession#readSet(long[])
   */
  public SIBusMessage[] readSet(SIMessageHandle[] msgHandles)
   
  throws SIMPMessageNotLockedException, SISessionUnavailableException, SIConnectionLostException, SIIncorrectCallException, SIResourceException, SIErrorException
  {
    if (CoreSPIBifurcatedConsumerSession.tc.isEntryEnabled()) 
      SibTr.entry(CoreSPIBifurcatedConsumerSession.tc, "readSet", 
        new Object[]{this, SIMPUtils.messageHandleArrayToString(msgHandles)});
    
    // Check that we aren't closed
    checkNotClosed();
    
    SIBusMessage[] msgs = _localConsumerPoint.processMsgSet(msgHandles, null, this, false, false, true, true);
    
    if (CoreSPIBifurcatedConsumerSession.tc.isEntryEnabled()) 
      SibTr.exit(CoreSPIBifurcatedConsumerSession.tc, "readSet", msgs);
    return msgs;
  }

  /* (non-Javadoc)
   * @see com.ibm.wsspi.sib.core.BifurcatedConsumerSession#readAndDeleteSet(long[], com.ibm.wsspi.sib.core.SITransaction)
   */
  public SIBusMessage[] readAndDeleteSet(SIMessageHandle[] msgHandles, SITransaction transaction) 
  
  throws SIMPMessageNotLockedException, SISessionUnavailableException, SIConnectionLostException, SIIncorrectCallException, SIResourceException, SIErrorException
  {
    if (CoreSPIBifurcatedConsumerSession.tc.isEntryEnabled()) 
      SibTr.entry(CoreSPIBifurcatedConsumerSession.tc, "readAndDeleteSet", 
        new Object[]{this, SIMPUtils.messageHandleArrayToString(msgHandles), transaction});
    
    // Check that we aren't closed
    checkNotClosed();
    
    SIBusMessage[] msgs = 
      _localConsumerPoint.processMsgSet(msgHandles, (TransactionCommon)transaction, this, false, true, true, true);
    
    if (CoreSPIBifurcatedConsumerSession.tc.isEntryEnabled()) 
      SibTr.exit(CoreSPIBifurcatedConsumerSession.tc, "readAndDeleteSet", msgs);
    return msgs;
  }

  /* (non-Javadoc)
   * @see com.ibm.wsspi.sib.core.BifurcatedConsumerSession#unlockSet(long[])
   */
  public void unlockSet(SIMessageHandle[] msgHandles) 
  
  throws SIMPMessageNotLockedException, SISessionUnavailableException, SIConnectionLostException, SIIncorrectCallException, SIResourceException, SIErrorException
  {
    if (CoreSPIBifurcatedConsumerSession.tc.isEntryEnabled()) 
      SibTr.entry(tc, "unlockSet", 
        new Object[] { this, SIMPUtils.messageHandleArrayToString(msgHandles) });         
    
    // Check that we aren't closed.  
    checkNotClosed();
    
    _localConsumerPoint.processMsgSet(msgHandles, null, this, true, false, false, true);
    
    if (CoreSPIBifurcatedConsumerSession.tc.isEntryEnabled()) 
      SibTr.exit(CoreSPIBifurcatedConsumerSession.tc, "unlockSet");
  }
  
  public void unlockSet(SIMessageHandle[] msgHandles, boolean incrementUnlockCount) 
    throws SIMPMessageNotLockedException, SISessionUnavailableException, SIConnectionLostException, SIIncorrectCallException, SIResourceException, SIErrorException
  {
    if (TraceComponent.isAnyTracingEnabled() && CoreSPIBifurcatedConsumerSession.tc.isEntryEnabled()) 
      SibTr.entry(tc, "unlockSet", 
        new Object[] { this, SIMPUtils.messageHandleArrayToString(msgHandles), Boolean.valueOf(incrementUnlockCount) });         
    
    // Check that we aren't closed.  
    checkNotClosed();
    
    _localConsumerPoint.processMsgSet(msgHandles, null, this, true, false, false, incrementUnlockCount);
    
    if (TraceComponent.isAnyTracingEnabled() && CoreSPIBifurcatedConsumerSession.tc.isEntryEnabled()) 
      SibTr.exit(CoreSPIBifurcatedConsumerSession.tc, "unlockSet");
  }


  /* (non-Javadoc)
   * @see com.ibm.wsspi.sib.core.DestinationSession#close()
   */
  public void close() 
  throws SIResourceException, SIConnectionLostException,
         SIErrorException
  {
    if (CoreSPIBifurcatedConsumerSession.tc.isEntryEnabled()) 
      SibTr.entry(CoreSPIBifurcatedConsumerSession.tc, "close", this);
    
    boolean closedNow =_close();
    
    // If this close operation actually closed the consumer, then
    // remove the consumer from the list stored in the connection
    // Also remove the Consumer from the list store in the Consumer
    // Session.
    if (closedNow)
    {
      _connection.removeBifurcatedConsumerSession(this);
    }
    
    if (CoreSPIBifurcatedConsumerSession.tc.isEntryEnabled()) 
      SibTr.exit(CoreSPIBifurcatedConsumerSession.tc, "close");
  }
  
  /**
   * Performs the actual close operation
   * The _close version does not call the owning connection to
   * remove this consumer. However, it does still inform
   * the parent consumer that this bifurcated consumer has been
   * closed down (and its messages can be unlocked).
   */
  boolean _close() throws SIResourceException
  {
    if (tc.isEntryEnabled()) SibTr.entry(tc, "_close");
    
    boolean closedNow = false;
    
    // Indicate that this connection is closed.
    synchronized (this)
    {
      if (!_closed)
        closedNow = true;
        
      _closed = true;    
    }

    // Ensure that we inform the parent consumer that this
    // bifurcated consumer is closed, so that any messages read
    // by this consumer can be unlocked.
    if (closedNow) {
      try
      {
        _consumerSession.removeBifurcatedConsumer(this);
      }
      catch (SISessionDroppedException e)
      {
  //      RMQSessionDroppedException shouldn't occur so FFDC.
        FFDCFilter.processException(
          e,
          "com.ibm.ws.sib.processor.impl.mqproxy.MQLocalization.getCursor",
          "1:280:1.27",
          this);
  
        SibTr.exception(tc, e);
  
        SIResourceException newE =
          new SIResourceException(
            nls.getFormattedMessage(
              "CONSUMER_CLOSED_ERROR_CWSIP0177",
              new Object[] { _localConsumerPoint.getConsumerManager().getDestination().getName(),
                             _localConsumerPoint.getConsumerManager().getMessageProcessor().getMessagingEngineName()},                
              null));
  
        if (tc.isEntryEnabled())
          SibTr.exit(tc, "getCursor", newE);
  
        throw newE;
      }
    }

    if (tc.isEntryEnabled()) SibTr.exit(tc, "_close", new Boolean(closedNow));
    
    return closedNow;   
  }


  /* (non-Javadoc)
   * @see com.ibm.wsspi.sib.core.DestinationSession#getConnection()
   */
  public SICoreConnection getConnection() 
  throws SISessionUnavailableException, SISessionDroppedException
  {
    if (CoreSPIBifurcatedConsumerSession.tc.isEntryEnabled()) 
      SibTr.entry(CoreSPIBifurcatedConsumerSession.tc, "getConnection", this);
        
    // Check that this connection connection isn't closed.
    checkNotClosed();
    
    if (CoreSPIBifurcatedConsumerSession.tc.isEntryEnabled()) 
      SibTr.exit(CoreSPIBifurcatedConsumerSession.tc, "getConnection", _connection);    
    return _connection;
  } 

  /* (non-Javadoc)
   * @see com.ibm.wsspi.sib.core.DestinationSession#getDestinationAddress()
   */
  public SIDestinationAddress getDestinationAddress()
  {
    if (CoreSPIBifurcatedConsumerSession.tc.isEntryEnabled()) 
    {
      SibTr.entry(CoreSPIBifurcatedConsumerSession.tc, "getDestinationAddress", this);    
      SibTr.exit(CoreSPIBifurcatedConsumerSession.tc, "getDestinationAddress");
    }
    return _consumerSession.getDestinationAddress();
  }
  
  /**
   * First check is to make sure that the original Consumer
   * hasn't been closed.  Then check that this bifurcated consumer 
   * session is not closed.  
   * 
   * @throws SIObjectClosedException
   */
  private void checkNotClosed() throws SISessionUnavailableException 
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "checkNotClosed");

    // Check that the consumer session isn't closed
    _consumerSession.checkNotClosed();
    
    // Now check that this consumer hasn't closed.
    synchronized (this)
    {
      if(_closed)
      {
        SISessionUnavailableException e =
          new SISessionUnavailableException(
            nls.getFormattedMessage(
              "CONSUMER_CLOSED_ERROR_CWSIP0177",
              new Object[] { _localConsumerPoint.getConsumerManager().getDestination().getName(),
                             _localConsumerPoint.getConsumerManager().getMessageProcessor().getMessagingEngineName()},                
              null));

        if (tc.isEntryEnabled())
          SibTr.exit(tc, "checkNotClosed", "consumer closed");

        throw e;
      }
    }
      
    if (tc.isEntryEnabled())
      SibTr.exit(tc, "checkNotClosed");
  }


  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.MPDestinationSession#getUuid()
   */
  public SIBUuid12 getUuid()
  {
    if (tc.isEntryEnabled()) 
    {
      SibTr.entry(tc, "getUuid");
      SibTr.exit(tc, "getUuid", uuid);	
    }
    return uuid;
  }
}
