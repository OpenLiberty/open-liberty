/*******************************************************************************
 * Copyright (c) 2005, 2008 IBM Corporation and others.
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
import com.ibm.ws.sib.comms.CommsConstants;
import com.ibm.ws.sib.jfapchannel.Conversation;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.OrderingContext;

/*
 * A proxy queue implementation that can be used to deliver messages to a non-read ahead, ordered consumer session
 */
public final class OrderedSessionProxyQueueImpl extends AsynchConsumerProxyQueueImpl {
  private static final TraceComponent tc = SibTr.register(OrderedSessionProxyQueueImpl.class, CommsConstants.MSG_GROUP, CommsConstants.MSG_BUNDLE);

  //@start_class_string_prolog@
  public static final String $sccsid = "@(#) 1.7 SIB/ws/code/sib.comms.client.impl/src/com/ibm/ws/sib/comms/client/proxyqueue/impl/OrderedSessionProxyQueueImpl.java, SIB.comms, WASX.SIB, uu1215.01 08/04/16 21:36:59 [4/12/12 22:14:07]";
  //@end_class_string_prolog@

  static {
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Source Info: " + $sccsid);
  }

  /*
   * Create a new ordered session proxy queue.
   * @param group The group to which the new proxy group will belong
   * @param id The id for the new proxy queue.
   * @param conversation The JFAP conversation over which the new proxy queue will converse
   * @param orderingContext The ordering context to use for the new proxy queue
   */
  public OrderedSessionProxyQueueImpl (final ProxyQueueConversationGroupImpl group, final short id, final Conversation conversation, final OrderingContext orderingContext) {
    super(group, id, conversation);
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "<init>", "group="+group+", id="+id+", conversation="+conversation+",orderingContext="+orderingContext);
    // We must be ordered. In this case we need to look up in the hashtable that maps ordering
    // contexts to queues and see if there is already a queue for this ordering context.
    setQueue(obtainQueue(ORDERED, orderingContext, null));
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "<init>");
  }

  /*
   * Constructor for unit testing only
   */
  public OrderedSessionProxyQueueImpl (final ProxyQueueConversationGroupImpl group, final short id, final ConversationHelper convHelper, final OrderingContext orderingContext) {
    super(group, id, null);
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "<init>", "group="+group+", id="+id+", convHelper="+convHelper+"orderingContext="+orderingContext);
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "test form of constructor invoked");
    setConversationHelper(convHelper);
    setQueue(obtainQueue(ORDERED, orderingContext, null));
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "<init>");
  }
}
