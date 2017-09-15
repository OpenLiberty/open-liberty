/*******************************************************************************
 * Copyright (c) 2004, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.comms.client.proxyqueue.impl;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.Reliability;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIIncorrectCallException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.comms.CommsConstants;
import com.ibm.ws.sib.comms.client.Transaction;
import com.ibm.ws.sib.comms.common.CommsUtils;
import com.ibm.ws.sib.jfapchannel.Conversation;
import com.ibm.ws.sib.jfapchannel.JFapChannelConstants;
import com.ibm.ws.sib.mfp.JsMessage;
import com.ibm.ws.sib.mfp.MessageDecodeFailedException;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.OrderingContext;
import com.ibm.wsspi.sib.core.SIMessageHandle;
import com.ibm.wsspi.sib.core.SITransaction;
import com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException;
import com.ibm.wsspi.sib.core.exception.SIConnectionLostException;
import com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException;
import com.ibm.wsspi.sib.core.exception.SILimitExceededException;
import com.ibm.wsspi.sib.core.exception.SIMessageNotLockedException;
import com.ibm.wsspi.sib.core.exception.SINotAuthorizedException;
import com.ibm.wsspi.sib.core.exception.SISessionDroppedException;
import com.ibm.wsspi.sib.core.exception.SISessionUnavailableException;

/*
 * This class extends the base read ahead proxy queue implementation by providing support
 * for the deleting of messages that were recoverable when reading ahead messages.
 */
public final class ReadAheadSessionProxyQueueImpl extends ReadAheadProxyQueueImpl {
  private static final TraceComponent tc = SibTr.register(ReadAheadSessionProxyQueueImpl.class, CommsConstants.MSG_GROUP, CommsConstants.MSG_BUNDLE);
  private static final String CLASS_NAME = ConversationHelperImpl.class.getName();

  //@start_class_string_prolog@
  public static final String $sccsid = "@(#) 1.28 SIB/ws/code/sib.comms.client.impl/src/com/ibm/ws/sib/comms/client/proxyqueue/impl/ReadAheadSessionProxyQueueImpl.java, SIB.comms, WASX.SIB, uu1215.01 09/08/24 08:32:30 [4/12/12 22:14:07]";
  //@end_class_string_prolog@

  static {
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Source Info: " + $sccsid);
  }

  // The unrecoverable reliability in use for this session
  private Reliability unrecoverableReliability = null;

  /*
   * Constructor.  Creates a new read ahead proxy queue.
   * @param group The group the proxy queue belongs to.
   * @param id The unique ID for the proxy queue.
   * @param conversation The conversation the proxy queue should use
   * when communicating with the ME sending it messages.
   * @param unrecoverableReliability
   */
  public ReadAheadSessionProxyQueueImpl (final ProxyQueueConversationGroupImpl group, final short id, final Conversation conversation, final Reliability unrecoverableReliability) {
    super(group, id, conversation);
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "<init>", "group="+group+", id="+id+", conversation="+conversation+", unrecoverableReliability="+unrecoverableReliability);

    setType(READAHEAD);
    this.unrecoverableReliability = unrecoverableReliability;
    setQueue(obtainQueue(READAHEAD, null, unrecoverableReliability));
    setReadAhead(true);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "<init>");
  }

  /*
   * Constructor for unit test use only.
   */
  public ReadAheadSessionProxyQueueImpl (final ProxyQueueConversationGroupImpl group, final short id, final ConversationHelper convHelper, final int type,  final OrderingContext oc) {
    super(group, id, null);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "<init>", "group="+group+", id="+id+", convHelper="+convHelper+", type="+type+", oc="+oc);
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "test form of constructor invoked");

    setOwningGroup(group);
    setConversationHelper(convHelper);
    setType(type);
    setQueue(obtainQueue(type, oc, unrecoverableReliability));

    if (type == READAHEAD) {
      setReadAhead(true);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "<init>");
  }

  /*
   * Constructor for unit test use only.
   */
  public ReadAheadSessionProxyQueueImpl (final ProxyQueueConversationGroupImpl pqimpl, final short s, final ConversationHelper convHelper) {
    super(pqimpl, s, null);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "<init>", "pqimpl="+pqimpl+", s="+s+", convHelper="+convHelper);
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "test form of constructor invoked");

    setOwningGroup(pqimpl);
    setId(s);
    setConversationHelper(convHelper);
    setType(READAHEAD);
    setQueue(obtainQueue(getType(), null, null));

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "<init>");
  }

  /*
   * Receive no wait
   */
  public JsMessage receiveNoWait (final SITransaction transaction) throws MessageDecodeFailedException,
                                                                         SISessionUnavailableException,
                                                                         SISessionDroppedException,
                                                                         SIConnectionUnavailableException,
                                                                         SIConnectionDroppedException,
                                                                         SIResourceException,
                                                                         SIConnectionLostException,
                                                                         SILimitExceededException,
                                                                         SIErrorException,
                                                                         SINotAuthorizedException,
                                                                         SIIncorrectCallException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "receiveNoWait", "transaction="+transaction);

    final JsMessage message = super.receiveNoWait(transaction);

    if (message != null) {
      // Only send a delete up if the message was recoverable. If it was not, then
      // we will have already deleted the message as an optimisation
      if (CommsUtils.isRecoverable(message, getConsumerSessionProxy().getUnrecoverableReliability())) {
        int priority = JFapChannelConstants.PRIORITY_MEDIUM;

        if (transaction != null) {
          Transaction commsTransaction = (Transaction)transaction;
          priority = commsTransaction.getLowestMessagePriority();  // d178368   // D249006
          
          // Inform the transaction that our consumer session has deleted
          // a recoverable message under this transaction. This means that if
          // a rollback is performed (and strict rollback ordering is enabled)
          // we can ensure that this message will be redelivered in order.
          commsTransaction.associateConsumer(consumerSession);
        }

        try {
          getConversationHelper().deleteMessages(new SIMessageHandle[] {message.getMessageHandle()}, transaction,  priority);
        } catch (SIMessageNotLockedException e) {
          // This is an internal error. We should FFDC and rethrow as an SIErrorException to indicate some inconsistency in state has occurred
          FFDCFilter.processException(e, CommsConstants.RHSESSPQIMPL_RECEIVENOWAIT_01, CLASS_NAME + ".receiveNoWait", this);
          if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "The message was not locked", e);
          throw new SIErrorException(e);
        }
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "receiveNoWait","rc="+message);
    return message;
  }

  /*
   * Receive with wait.
   */
  public JsMessage receiveWithWait (final long timeout, final SITransaction transaction) throws MessageDecodeFailedException,
                                                                                                SISessionUnavailableException,
                                                                                                SISessionDroppedException,
                                                                                                SIConnectionUnavailableException,
                                                                                                SIConnectionDroppedException,
                                                                                                SIResourceException,
                                                                                                SIConnectionLostException,
                                                                                                SILimitExceededException,
                                                                                                SIErrorException,
                                                                                                SINotAuthorizedException,
                                                                                                SIIncorrectCallException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "receiveWithWait","timeout="+timeout+", tracsaction="+transaction);

    final JsMessage message = super.receiveWithWait(timeout, transaction);

    if (message != null)
    {
      // Only send a delete up if the message was recoverable. If it was not, then we will have already deleted the
      // message as an optimisation
      if (CommsUtils.isRecoverable(message, getConsumerSessionProxy().getUnrecoverableReliability())) {
        int priority = JFapChannelConstants.PRIORITY_MEDIUM;

        if (transaction != null) {
          Transaction commsTransaction = (Transaction) transaction;
          priority = commsTransaction.getLowestMessagePriority();  // d178368   // D249006
         
          // Inform the transaction that our consumer session has deleted
          // a recoverable message under this transaction. This means that if
          // a rollback is performed (and strict rollback ordering is enabled)
          // we can ensure that this message will be redelivered in order.
          commsTransaction.associateConsumer(consumerSession);
        }

        try {
          getConversationHelper().deleteMessages(new SIMessageHandle[] {message.getMessageHandle()}, transaction, priority);
        } catch (SIMessageNotLockedException e) {
          // This is an internal error. We should FFDC and rethrow as an SIErrorException to indicate some inconsistency in state has occurred
          FFDCFilter.processException(e, CommsConstants.RHSESSPQIMPL_RECEIVEWITHWAIT_01, CLASS_NAME + ".receiveNoWait", this);
          if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "The message was not locked", e);
          throw new SIErrorException(e);
        }
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "receiveWithWait", "rc="+message);
    return message;
  }
}
