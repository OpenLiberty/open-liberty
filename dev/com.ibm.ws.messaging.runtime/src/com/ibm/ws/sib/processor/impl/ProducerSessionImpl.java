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

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.SIDestinationAddress;
import com.ibm.websphere.sib.SIRCConstants;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIIncorrectCallException;
import com.ibm.websphere.sib.exception.SINotPossibleInCurrentConfigurationException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.mfp.JsApiMessage;
import com.ibm.ws.sib.mfp.JsDestinationAddress;
import com.ibm.ws.sib.mfp.JsMessage;
import com.ibm.ws.sib.mfp.MessageCopyFailedException;
import com.ibm.ws.sib.mfp.ProtocolType;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.exceptions.SIMPErrorException;
import com.ibm.ws.sib.processor.exceptions.SIMPIncorrectCallException;
import com.ibm.ws.sib.processor.exceptions.SIMPNoLocalisationsException;
import com.ibm.ws.sib.processor.exceptions.SIMPNotAuthorizedException;
import com.ibm.ws.sib.processor.exceptions.SIMPNotPossibleInCurrentConfigurationException;
import com.ibm.ws.sib.processor.exceptions.SIMPSessionUnavailableException;
import com.ibm.ws.sib.processor.impl.corespitrace.CoreSPIProducerSession;
import com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler;
import com.ibm.ws.sib.processor.impl.interfaces.MPDestinationSession;
import com.ibm.ws.sib.processor.impl.interfaces.MessageProducer;
import com.ibm.ws.sib.processor.impl.interfaces.ProducerInputHandler;
import com.ibm.ws.sib.processor.impl.store.items.MessageItem;
import com.ibm.ws.sib.processor.utils.DestinationSessionUtils;
import com.ibm.ws.sib.processor.utils.SIMPUtils;
import com.ibm.ws.sib.processor.utils.UserTrace;
import com.ibm.ws.sib.security.auth.OperationType;
import com.ibm.ws.sib.transactions.TransactionCommon;
import com.ibm.ws.sib.utils.SIBUuid12;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.ProducerSession;
import com.ibm.wsspi.sib.core.SIBusMessage;
import com.ibm.wsspi.sib.core.SICoreConnection;
import com.ibm.wsspi.sib.core.SITransaction;
import com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException;
import com.ibm.wsspi.sib.core.exception.SIConnectionLostException;
import com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException;
import com.ibm.wsspi.sib.core.exception.SILimitExceededException;
import com.ibm.wsspi.sib.core.exception.SINotAuthorizedException;
import com.ibm.wsspi.sib.core.exception.SISessionDroppedException;
import com.ibm.wsspi.sib.core.exception.SISessionUnavailableException;


/**
 * @author tevans
 */
public final class ProducerSessionImpl implements ProducerSession, MessageProducer, MPDestinationSession
{
  // NLS for component
  private static final TraceNLS nls_mt =
    TraceNLS.getTraceNLS(SIMPConstants.TRACE_MESSAGE_RESOURCE_BUNDLE);

  // NLS for component
  private static final TraceNLS nls =
    TraceNLS.getTraceNLS(SIMPConstants.RESOURCE_BUNDLE);
  private static final TraceNLS nls_cwsik =
    TraceNLS.getTraceNLS(SIMPConstants.CWSIK_RESOURCE_BUNDLE);

  private static final TraceComponent tc =
    SibTr.register(
      ProducerSessionImpl.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);  
 
  private int _seed = 0;
  private MessageProcessor _messageProcessor;
  private boolean _closed;
  private ProducerInputHandler _inputHandler;
  private ConnectionImpl _conn;

  /* The unresolved DestinationHandler that we're attached to (e.g. alias) */
  private DestinationHandler _destination;

  /* The SIDestinationAddress supplied in the create producer call */
  private JsDestinationAddress _address;

  /* The address sometimes set into each message produced. This is generally
   * used when the message arrives on remote MEs or foreign buses. */
  private JsDestinationAddress _routingDestinationAddr;
  private boolean _isRoutingDestinationAddrSet = false;

  private TransactionCommon _autoCommitTransaction;

  // Security related parameters

  // Should we check access permissions for the discriminator at send time
  private boolean _checkDiscriminatorAccessAtSend = true;
  // The discriminator that was specified at send time
  private String _discriminatorAtCreate = null;
  // The security context associated with the session
  private SecurityContext _securityContext = null;
  // Specifies whether the security userid should be preserved in a messaage at send time
  private boolean _keepSecurityUserid = false;

  private SIBUuid12 uuid;

  private boolean _fixedMessagePoint = false;
  private boolean _preferLocal = true;
  
  // As a message is passed arount a PubSub network a trail of fingerprints is
  // added to a message by the PubSub engines. Any application re-sending a message
  // should have any fingerprints wiped from the message (unless explicitly excluded,
  // i.e. PubSub bridge and mediations).
  private boolean _clearPubSubFingerprints = true;
  
  /**
   * Constructer for the ProducerSession class
   *
   * @param address
   * @param dest
   * @param conn
   */
  ProducerSessionImpl(SIDestinationAddress inAddress,
                      DestinationHandler dest,
                      ConnectionImpl conn,
                      SecurityContext secContext,
                      boolean keepSecurityUSerid,
                      boolean fixedMessagePoint,
                      boolean preferLocal,
                      boolean clearPubSubFingerprints) throws SINotPossibleInCurrentConfigurationException, SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "ProducerSessionImpl",
        new Object[] { inAddress,
                       dest,
                       conn,
                       secContext,
                       keepSecurityUSerid,
                       fixedMessagePoint,
                       preferLocal});

    _destination = dest;
    _conn = conn;
    _securityContext = secContext;

    _fixedMessagePoint = fixedMessagePoint;
    _preferLocal = preferLocal;

    _keepSecurityUserid = keepSecurityUSerid;
    _messageProcessor = conn.getMessageProcessor();
    _address = (JsDestinationAddress)inAddress;

    this.uuid = new SIBUuid12();

    ProtocolType inputProtocol = ProtocolType.UNICASTINPUT;
    if(dest.isPubSub()) inputProtocol = ProtocolType.PUBSUBINPUT;


      // Use the supplied value (defaults to true by caller)
      _clearPubSubFingerprints = clearPubSubFingerprints;
      
      _inputHandler = (ProducerInputHandler) _destination.getInputHandler(
                                                 inputProtocol,
                                                 _messageProcessor.getMessagingEngineUuid(),
                                                 null);

    _inputHandler.attachProducer(this);

    _closed = false;

    //Set up an autocommit transaction for putting all non-transacted point to
    //point messages (also set up for topicspaces as if they get
    //mediated we send the message point to point to the mediation
    _autoCommitTransaction = _destination.getTxManager().createAutoCommitTransaction();

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "ProducerSessionImpl", new Object[] {this});
  }

  /**
   *
   *
   */
  static final void setRFH2Allowed(JsMessage msg, DestinationHandler destination)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "setRFH2Allowed", new Object[] { msg, destination });

    Object rfh2Allowed = destination.getContextValue("_MQRFH2Allowed");
    boolean allowed = false;

    if (rfh2Allowed instanceof Boolean)
    {
      Boolean isAllowed = (Boolean) rfh2Allowed;
      allowed = isAllowed.booleanValue();
    }
    else
    {
      if (rfh2Allowed instanceof String)
      {
        String isAllowed = (String) rfh2Allowed;
        if (isAllowed.compareToIgnoreCase("YES") == 0)
        {
          allowed = true;
        }
      }
    }
    if (allowed)
    {
      msg.setMQRFH2Allowed(true);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "setRFH2Allowed", Boolean.valueOf(allowed));
  }


  synchronized void updateInputHandler(ProducerInputHandler newInputHandler)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "updateInputHandler", newInputHandler);

    _inputHandler = newInputHandler;

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "updateInputHandler");
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.ProducerSession#send(com.ibm.ws.sib.mfp.JsMessage, com.ibm.ws.sib.processor.QualityOfService, com.ibm.ws.sib.processor.SITransaction)
   */
  public void send(SIBusMessage sibMsg, SITransaction tranImpl)
  throws SISessionUnavailableException, SISessionDroppedException,
         SIConnectionUnavailableException, SIConnectionDroppedException,
         SIResourceException, SIConnectionLostException, SILimitExceededException,
         SIErrorException,
         SINotAuthorizedException,
         SIIncorrectCallException,
         SINotPossibleInCurrentConfigurationException
  {
    if (TraceComponent.isAnyTracingEnabled() && CoreSPIProducerSession.tc.isEntryEnabled())
      SibTr.entry(CoreSPIProducerSession.tc, "send", new Object[] { this, sibMsg, tranImpl });

    if (tranImpl != null && !((TransactionCommon)tranImpl).isAlive())
    {
      SIMPIncorrectCallException e = new SIMPIncorrectCallException(
        nls_cwsik.getFormattedMessage(
           "DELIVERY_ERROR_SIRC_16",  // TRANSACTION_SEND_USAGE_ERROR_CWSIP0093
           new Object[] { _destination },
           null) );

      e.setExceptionReason(SIRCConstants.SIRC0016_TRANSACTION_SEND_USAGE_ERROR);
      e.setExceptionInserts(new String[] {_destination.getName()});

      if (TraceComponent.isAnyTracingEnabled() && TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
        SibTr.exception(tc, e);

      if (TraceComponent.isAnyTracingEnabled() && CoreSPIProducerSession.tc.isEntryEnabled())
        SibTr.exit(CoreSPIProducerSession.tc, "send", e);

      throw e;
    }
    synchronized (this)
    {
      checkNotClosed();

      // Ensure that the destination is put enabled
      if (!_destination.isSendAllowed())
      {
        if (TraceComponent.isAnyTracingEnabled() && CoreSPIProducerSession.tc.isEntryEnabled())
          SibTr.exit(CoreSPIProducerSession.tc, "send", "Destination is send disabled");

        SIMPNotPossibleInCurrentConfigurationException e = new SIMPNotPossibleInCurrentConfigurationException(
          nls_cwsik.getFormattedMessage(
            "DELIVERY_ERROR_SIRC_17", // DESTINATION_LOCKED_ERROR_CWSIP0232
            new Object[] { _destination.getName(),
                           _messageProcessor.getMessagingEngineName() },
            null));

        e.setExceptionReason(SIRCConstants.SIRC0017_DESTINATION_LOCKED_ERROR);
        e.setExceptionInserts(new String[] { _destination.getName(),
                                      _messageProcessor.getMessagingEngineName() });
        throw e;
      }

      // Extract the message
      JsMessage msg = null;
      try
      {
        msg = ((JsMessage) sibMsg).getSent(_conn.getMessageCopiedWhenSent());
      }
      catch (MessageCopyFailedException e)
      {
        FFDCFilter.processException(
          e,
          "com.ibm.ws.sib.processor.impl.ProducerSessionImpl.send",
          "1:379:1.217",
          this);

        SibTr.exception(tc, e);
        SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
          new Object[] {
            "com.ibm.ws.sib.processor.impl.ProducerSessionImpl",
            "1:386:1.217",
            e });

        if (TraceComponent.isAnyTracingEnabled() && CoreSPIProducerSession.tc.isEntryEnabled())
          SibTr.exit(CoreSPIProducerSession.tc, "send", "Could not make a safe copy of message");

        SIMPErrorException ex = new SIMPErrorException(
          nls.getFormattedMessage(
            "INTERNAL_MESSAGING_ERROR_CWSIP0002",
            new Object[] {
              "com.ibm.ws.sib.processor.impl.ProducerSessionImpl",
              "1:397:1.217",
              e },
            null),
          e);

        ex.setExceptionReason(SIRCConstants.SIRC0901_INTERNAL_MESSAGING_ERROR);
        ex.setExceptionInserts(new String[] {
          "com.ibm.ws.sib.processor.impl.ProducerSessionImpl",
          "1:405:1.217",
          SIMPUtils.getStackTrace(e)});
        throw ex;
      }
      
      // Unless otherwise specified (mediations, PubSub bridge and MQLink), clear out
      // any fingerprints already in the message to prevent accidental 'loop' detection
      // occuring.
      if(_clearPubSubFingerprints)
      {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
          SibTr.debug(this, tc, "clearFingerprints");
        msg.clearFingerprints();
      }

      // On entry to Message Processor trace the message content
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        SibTr.debug(this, tc, "verboseMsg IN : " + msg.toVerboseString());

      // Set the RFH2 allowed flag (for messages destined for MQ)
      if (_destination.isAlias() ||
          _destination.isForeign() ||
          _destination.isForeignBus())
      {
        setRFH2Allowed(msg, _destination);
      }

      // Set the message put time if required
      if (msg.getTimestamp().longValue() == SIMPConstants.TIMESTAMP_REQUIRED)
      {
        msg.setTimestamp(System.currentTimeMillis());
      }

      // If security is enabled then we'll do the discriminator access check
      if(_conn.isBusSecure())
      {
        // If pubsub, then we need to check access to the discriminator
        // And if the discriminator access was checked at create time, then
        // we need to set the discriminator into the message.
        if(_checkDiscriminatorAccessAtSend)
        {
          // set the discriminator into the security context
          _securityContext.setDiscriminator(msg.getDiscriminator());
          if(!_destination.
              checkDiscriminatorAccess(_securityContext,
                                       OperationType.SEND))
          {
            if (TraceComponent.isAnyTracingEnabled() && CoreSPIProducerSession.tc.isEntryEnabled())
              SibTr.exit(CoreSPIProducerSession.tc, "send", "not authorized to produce to this destination's discriminator");

            // Write an audit record if access is denied
            SibTr.audit(tc, nls_cwsik.getFormattedMessage(
              "DELIVERY_ERROR_SIRC_20", // USER_NOT_AUTH_SEND_ERROR_CWSIP0308
              new Object[] { _destination.getName(),
                             msg.getDiscriminator(),
                             _conn.getResolvedUserid() },
              null));

            // Thrown if user denied access to destination
            SIMPNotAuthorizedException e = new SIMPNotAuthorizedException(
              nls_cwsik.getFormattedMessage(
                "DELIVERY_ERROR_SIRC_20", // USER_NOT_AUTH_SEND_ERROR_CWSIP0308
              new Object[] { _destination.getName(),
                             msg.getDiscriminator(),
                             _conn.getResolvedUserid() },
              null));

            e.setExceptionReason(SIRCConstants.SIRC0020_USER_NOT_AUTH_SEND_ERROR);
            e.setExceptionInserts(new String[] { _destination.getName(),
                                                  msg.getDiscriminator(),
                                                  _conn.getResolvedUserid() });
            throw e;
          }
        }
        else // Access was checked at session creation time
        {
          // set the stored discriminator into the message
          msg.setDiscriminator(_discriminatorAtCreate);
        }
      }

      // Set the userid associated with the connection into the message
      // unless the sender has alternate user privilege - in which case
      // the alternate userid will be set into the message
      if(_securityContext != null &&
         _securityContext.isAlternateUserBased())
      {
        // Set the alternate user into the message
        if(!_keepSecurityUserid)
        {
          _messageProcessor.
            getAccessChecker().
            setSecurityIDInMessage(_securityContext.getAlternateUser(),msg);
        }
      }
      else
      {
        // Set the userid associated with the connection into the message
        if(!_keepSecurityUserid)
        {
          _messageProcessor.
            getAccessChecker().
            setSecurityIDInMessage(_conn.getSecuritySubject(),msg);
        }
      }

  
     
    
      //Create a message item.  If the message has come from a mediation it
      //has a message wait time in it from its time before the mediation
      //and this must be maintained, otherwise 0 can be assumed and this
      //saves having to read the waittime from the JsMessage, which is
      //fairly expensive to do performance wise.
      MessageItem item = null;


      item = new MessageItem(msg, 0L);

      // Mark this message to indicate if local queue points are preferred over others
      item.setPreferLocal(_preferLocal);

      if(_destination.isPubSub())
      {
        //defect 278038:
        //Set the connectionUuid to this connection (for durable subs noLocal matching)
        //The field will be set in the jsMsg when the msgitem is persisted or
        //when the jsMsg is sent over the wire.
        item.setProducerConnectionUuid(_conn.getUuid());
      }
      item.setProducerSeed(++_seed);

      //-----199744------------------------
      //If no message priority has been set in thes message, the default is taken
      //from the destination.  This cannot be the case for JMS messages, as JMS
      //always sets a default of 4 itself, however for non JMS messages it does
      //have a use.
      if (item.getPriority() == -1)
      {
        item.setPriority(_destination.getDefaultPriority());
      }

      if (!_destination.isOverrideOfQOSByProducerAllowed())
      {
        item.setReliability(_destination.getDefaultReliability());
      }

        // Indicate that this message requires a new SystemId
        item.setRequiresNewId(true);

      TransactionCommon transaction = null;

      if (tranImpl != null)
      {
        transaction = (TransactionCommon) tranImpl;
      }
      else
      {
        transaction = _autoCommitTransaction;
      }

      //Inidcates whether the send was a sucess, used for the rm unblock call
      boolean sucessful = false;
      Object rmCallBackObject = null;

      try
      {

        if (TraceComponent.isAnyTracingEnabled() && UserTrace.tc_mt.isDebugEnabled())
          traceSend(item, tranImpl);

        // We need to know if the FRP in the message came from an app or a
        // destination's FRP. As this is the point of entry from an app it's
        // the best place to look.
        boolean msgFRP = false;
        if(!msg.isForwardRoutingPathEmpty())
          msgFRP = true;

        _inputHandler.handleProducerMessage(item,
                                    transaction,
                                    _address,
                                    this, // If mediated, don't use us as context across messages
                                    msgFRP);

        sucessful = true;
      }
      catch(SIMPNoLocalisationsException e)
      {
        // No FFDC code needed

        // We can't find a suitable localisation.
        // Although a queue must have at least one localisation this is
        // possible if the sender restricted the potential localisations
        // using a fixed ME or a scoping alias (to an out-of-date set of
        // localisation)
      
    	
        
        if (TraceComponent.isAnyTracingEnabled() && CoreSPIProducerSession.tc.isEntryEnabled())
          SibTr.exit(CoreSPIProducerSession.tc, "send", "SINotPossibleInCurrentConfigurationException");

        SIMPNotPossibleInCurrentConfigurationException ee =
          new SIMPNotPossibleInCurrentConfigurationException(e);
        ee.setExceptionReason(SIRCConstants.SIRC0026_NO_LOCALISATIONS_FOUND_ERROR);
        ee.setExceptionInserts(new String[] { _destination.getName() });
        throw ee;
      }
     
    }//end sync

    if (TraceComponent.isAnyTracingEnabled() && CoreSPIProducerSession.tc.isEntryEnabled())
      SibTr.exit(CoreSPIProducerSession.tc, "send");
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.ProducerSession#close()
   */
  public void close()
  {
    if (TraceComponent.isAnyTracingEnabled() && CoreSPIProducerSession.tc.isEntryEnabled())
      SibTr.entry(CoreSPIProducerSession.tc, "close", this);

    boolean closedNow = _close();

    if (closedNow)
    {
      _conn.removeProducerSession(this);
    }

    if (TraceComponent.isAnyTracingEnabled() && CoreSPIProducerSession.tc.isEntryEnabled())
      SibTr.exit(CoreSPIProducerSession.tc, "close", this);
  }

  /**
   * Method _close.
   * <p>Internal method to close a producer session</p>
   * @return boolean True if this close call closed the session
   *                  False if the session was already closed
   */
  boolean _close()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "_close");

    boolean closedNow = false;

    synchronized (this)
    {
      if (!_closed)
      {
        _closed = true;
        closedNow = true;
      }
    }

    if (closedNow)
    {
      /*
       * This must be done outside the synchronization as it will synchronize on
       * the set of producer sessions whilst detaching this producer and if not
       * done outside, this could cause a deadlock with methods that synchronize
       * on the set and then each producersession in the set in turn.
       */
      _inputHandler.detachProducer(this);
      _inputHandler = null;
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "_close", new Boolean(closedNow));

    return closedNow;
  }

  /**
   * Returns this sessions connection
   */

  public SICoreConnection getConnection() throws SISessionUnavailableException
  {
    if (TraceComponent.isAnyTracingEnabled() && CoreSPIProducerSession.tc.isEntryEnabled())
    {
      SibTr.entry(CoreSPIProducerSession.tc, "getConnection", this);
      SibTr.exit(CoreSPIProducerSession.tc, "getConnection", _conn);
    }
    checkNotClosed();
    return _conn;
  }

  /**
   * Method _closeProducerDestinationDeleted.
   * <p>Called internally to close a session because the destination has been
   * deleted.  </p>
   */
  protected void _closeProducerDestinationDeleted()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "_closeProducerDestinationDeleted");

    /* This routine does similar processing to _close() but it does
     * not call back to the inputHandler to remove the producerSession.  Doing
     * so would stop users of this routine in the inputHandler from using an
     * iterator to close all the producersessions.
     */
    
    // Check if this is actually closing the producer (i.e. it's not already closed)
    boolean closedNow = false;

    synchronized (this)
    {
      if (!_closed)
      {
        _closed = true;
        _inputHandler = null;
        closedNow = true;
      }
    }
    
    // If we've just cosed the session, remove it from the connection's list, although
    // not under 'this' lock as that would deadlock with the connection '_producers' lock.
    if (closedNow)
      _conn.removeProducerSession(this);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "_closeProducerDestinationDeleted");

    return;
  }

  /* (non-Javadoc)
   * @see com.ibm.wsspi.sib.core.DestinationSession#getDestinationAddress()
   */
  public SIDestinationAddress getDestinationAddress()
  {
    if (TraceComponent.isAnyTracingEnabled() && CoreSPIProducerSession.tc.isEntryEnabled())
      SibTr.entry(
        CoreSPIProducerSession.tc,
        "getDestinationAddress", this);

//    if(_ != null)
//    {
//      if (TraceComponent.isAnyTracingEnabled() && CoreSPIProducerSession.tc.isEntryEnabled())
//        SibTr.exit(CoreSPIProducerSession.tc, "getDestinationAddress", _routingDestinationAddr);
//      return _routingDestinationAddr;
//    }

    if (_address == null)
    {
      SIDestinationAddress destAddr
        = DestinationSessionUtils.createJsDestinationAddress(_destination);
      if (TraceComponent.isAnyTracingEnabled() && CoreSPIProducerSession.tc.isEntryEnabled())
        SibTr.exit(CoreSPIProducerSession.tc, "getDestinationAddress", destAddr);
      return destAddr;
    }
    else
    {
      if (TraceComponent.isAnyTracingEnabled() && CoreSPIProducerSession.tc.isEntryEnabled())
        SibTr.exit(CoreSPIProducerSession.tc, "getDestinationAddress", _address);
      return _address;
    }
  }

  
  /**
   * Disable discriminator access checks at send time
   */
  void disableDiscriminatorAccessCheckAtSend(String discriminatorAtCreate)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "disableDiscriminatorAccessCheckAtSend");

    _checkDiscriminatorAccessAtSend = false;
    this._discriminatorAtCreate = discriminatorAtCreate;

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "disableDiscriminatorAccessCheckAtSend");
  }

  private void traceSend(MessageItem item, SITransaction siTran)
  {
    if (item.getMessage().isApiMessage())
    {

      String apiMsgId = null;
      String correlationId = null;

      if (item.getMessage() instanceof JsApiMessage)
      {
        apiMsgId = ((JsApiMessage)item.getMessage()).getApiMessageId();
        correlationId = ((JsApiMessage)item.getMessage()).getCorrelationId();
      }
      else
      {
        if (item.getMessage().getApiMessageIdAsBytes() != null)
          apiMsgId = new String(item.getMessage().getApiMessageIdAsBytes());

        if (item.getMessage().getCorrelationIdAsBytes() != null)
          correlationId = new String(item.getMessage().getCorrelationIdAsBytes());
      }

      if ( siTran != null )
      {
        String msg = "PRODUCER_SEND_CWSJU0001";
        if (_destination.isForeignBus())
          msg = "PRODUCER_SEND_BUS_CWSJU0017";
        else if (_destination.isMQLink())
          msg = "PRODUCER_SEND_MQLINK_CWSJU0019";
        else if (_destination.isLink())
          msg = "PRODUCER_SEND_LINK_CWSJU0018";

        if (TraceComponent.isAnyTracingEnabled() && UserTrace.tc_mt.isDebugEnabled())
          SibTr.debug(UserTrace.tc_mt,
            nls_mt.getFormattedMessage(
              msg,
              new Object[] {
                _conn.getUuid(),
                apiMsgId,
                correlationId,
                _destination.getName(),
                ((TransactionCommon)siTran).getPersistentTranId()},
              null));
      }
      else
      {
        String msg = "PRODUCER_SEND_NO_TRAN_CWSJU0002";
        if (_destination.isForeignBus())
          msg = "PRODUCER_SEND_NO_TRAN_BUS_CWSJU0061";
        else if (_destination.isMQLink())
          msg = "PRODUCER_SEND_NO_TRAN_MQLINK_CWSJU0063";
        else if (_destination.isLink())
          msg = "PRODUCER_SEND_NO_TRAN_LINK_CWSJU0062";

        if (TraceComponent.isAnyTracingEnabled() && UserTrace.tc_mt.isDebugEnabled())
         SibTr.debug(UserTrace.tc_mt,
          nls_mt.getFormattedMessage(
            msg,
            new Object[] {
              _conn.getUuid(),
              apiMsgId,
              correlationId,
              _destination.getName()},
            null));
      }
    }
  }

  private void checkNotClosed() throws SISessionUnavailableException
  {
    // Check the session is not closed
    if (_closed)
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "checkNotClosed", "ProducerSession closed");

      SIMPSessionUnavailableException e = new SIMPSessionUnavailableException(
        nls_cwsik.getFormattedMessage(
          "DELIVERY_ERROR_SIRC_21",  // OBJECT_CLOSED_ERROR_CWSIP0233
          new Object[] { _destination.getName(),
                         _messageProcessor.getMessagingEngineName() },
          null));

      e.setExceptionReason(SIRCConstants.SIRC0021_OBJECT_CLOSED_ERROR);
      e.setExceptionInserts(new String[] { _destination.getName(),
                                  _messageProcessor.getMessagingEngineName() });
      throw e;
    }
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.MPDestinationSession#getUuid()
   */
  public SIBUuid12 getUuid()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getUuid");
      SibTr.exit(tc, "getUuid", uuid);	
    }
    return uuid;
  }

  public boolean fixedMessagePoint()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "fixedMessagePoint");
      SibTr.exit(tc, "fixedMessagePoint", Boolean.valueOf(_fixedMessagePoint));
    }

    return _fixedMessagePoint;
  }

  public JsDestinationAddress getRoutingDestination()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getRoutingDestination");
      SibTr.exit(tc, "getRoutingDestination", _routingDestinationAddr);
    }

    return _routingDestinationAddr;
  }

  public boolean isRoutingDestinationSet()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "isRoutingDestinationSet");
      SibTr.exit(tc, "isRoutingDestinationSet", Boolean.valueOf(_isRoutingDestinationAddrSet));
    }

    return _isRoutingDestinationAddrSet;
  }

  public void setRoutingAddress(JsDestinationAddress routingAddr)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "setRoutingAddress", routingAddr);
      SibTr.exit(tc, "setRoutingAddress");
    }

    _routingDestinationAddr = routingAddr;
    _isRoutingDestinationAddrSet = true;

  }

}
