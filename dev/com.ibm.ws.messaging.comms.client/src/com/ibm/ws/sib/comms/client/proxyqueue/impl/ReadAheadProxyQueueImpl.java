/*******************************************************************************
 * Copyright (c) 2004, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.comms.client.proxyqueue.impl;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIIncorrectCallException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.sib.comms.CommsConstants;
import com.ibm.ws.sib.jfapchannel.Conversation;
import com.ibm.ws.sib.mfp.JsMessage;
import com.ibm.ws.sib.mfp.MessageDecodeFailedException;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.SITransaction;
import com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException;
import com.ibm.wsspi.sib.core.exception.SIConnectionLostException;
import com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException;
import com.ibm.wsspi.sib.core.exception.SILimitExceededException;
import com.ibm.wsspi.sib.core.exception.SINotAuthorizedException;
import com.ibm.wsspi.sib.core.exception.SISessionDroppedException;
import com.ibm.wsspi.sib.core.exception.SISessionUnavailableException;

/*
 * This is the base implementation for a read ahead proxy queue. This queue has the ability
 * to allow clients to receive messages synchronously and asynchronously and is designed to be
 * implemented by subclasses for their specific need.
 */
public abstract class ReadAheadProxyQueueImpl extends AsynchConsumerProxyQueueImpl {
  private static final TraceComponent tc = SibTr.register(ReadAheadProxyQueueImpl.class, CommsConstants.MSG_GROUP, CommsConstants.MSG_BUNDLE);
  private static final TraceNLS nls = TraceNLS.getTraceNLS(CommsConstants.MSG_BUNDLE);

  //@start_class_string_prolog@
  public static final String $sccsid = "@(#) 1.51 SIB/ws/code/sib.comms.client.impl/src/com/ibm/ws/sib/comms/client/proxyqueue/impl/ReadAheadProxyQueueImpl.java, SIB.comms, WASX.SIB, uu1215.01 08/04/16 21:38:29 [4/12/12 22:14:07]";
  //@end_class_string_prolog@

  static {
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Source Info: " + $sccsid);
  }

  /*
   * Constructor
   */
  ReadAheadProxyQueueImpl (final ProxyQueueConversationGroupImpl group, final short id, final Conversation conversation) {
    super(group, id, conversation);
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "<init>","group="+group+", id="+id+", conversation="+conversation);
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "<init>");
  }

  /*
   * Receive a message with wait
   */
  public synchronized JsMessage receiveWithWait (final long to, final SITransaction transaction) throws MessageDecodeFailedException,
                                                                                                        SISessionUnavailableException, SISessionDroppedException,
                                                                                                        SIConnectionUnavailableException, SIConnectionDroppedException,
                                                                                                        SIResourceException, SIConnectionLostException, SILimitExceededException,
                                                                                                        SIErrorException,
                                                                                                        SINotAuthorizedException,
                                                                                                        SIIncorrectCallException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "receiveWithWait", "to="+to+", transaction="+transaction);

    long timeout = to;

    checkConversationLive();

    // Not allowed to call ReceiveWithWait on an asynchronous consumer
    if (getAsynchConsumerCallback() != null) {
      throw new SIIncorrectCallException(nls.getFormattedMessage("NOT_ALLOWED_WHILE_ASYNCH_SICO1020", null, null));
    }

    JsMessage message = null;

    // Optimisation: if we are started - try to get a message before even thinking about waiting
    if (getStarted()) {
      message = getQueue().get(getId());
    }

    // If we didn't get a message we need to wait for one to arrive
    if (message == null) {
      long startTime;
      boolean timedOut = false;
      // While the session is not closed, we haven't timed out and we are either not started or empty - wait
      while (!getClosed() && !timedOut && (!getStarted() || getQueue().isEmpty(getId()))) {
        startTime = System.currentTimeMillis();
        try {
          wait(timeout);
        } catch (InterruptedException e) {
          // No FFDC Code Needed
          if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Interupted!", e);
        }

        checkConversationLive();

        if (timeout != 0) {
          timeout -= (System.currentTimeMillis() - startTime);
          timedOut = timeout <= 0;
        }
      }

      // We could have exited the above loop for several reasons. If the conditions are right - see if we have a message
      if (!getClosed() && !getQueue().isEmpty(getId()) && getStarted()) {
        message = getQueue().get(getId());
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "receiveWithWait", "rc="+message);
    return message;
  }

  /*
   * Receive a message no wait
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

    checkConversationLive();

    // Why is ReceiveNoWait allowed with async but not ReceiveWithWait

    JsMessage message = null;

    if (getStarted() && !getClosed()) {
      message = getQueue().get(getId());
    }

    // If we didn't get a message, then we poke the remote machine to see if it has any messages
    if (message == null) {
      getConversationHelper().flushConsumer();

      if (getStarted() && !getClosed()) {
        message = getQueue().get(getId());
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "receiveNoWait");
    return message;
  }
}
