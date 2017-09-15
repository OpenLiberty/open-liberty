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
import com.ibm.ejs.util.am.Alarm;
import com.ibm.ejs.util.am.AlarmListener;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.mfp.JsMessage;
import com.ibm.ws.sib.msgstore.Filter;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.impl.interfaces.BrowseCursor;
import com.ibm.ws.sib.processor.impl.store.items.MessageItem;
import com.ibm.ws.sib.processor.utils.am.MPAlarmManager;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 *
 */
public class AIBrowseCursor implements BrowseCursor, AlarmListener
{
  // NLS for component
  private static final TraceNLS nls =
    TraceNLS.getTraceNLS(SIMPConstants.RESOURCE_BUNDLE);

  // Standard debug/trace
  private static final TraceComponent tc =
    SibTr.register(
      AIBrowseCursor.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);
  

  private AnycastInputHandler parent;
  private long browseId;
  private Filter filter;

  // Contains the value of the sequence number for the current BrowseGet, to be correlated with the
  // BrowseData received in response
  private long seqNum;

  private MessageItem nextItem;
  private boolean browseClosed;
  private boolean browseFailed;
  private int failureReason;

  private Alarm keepAliveAlarmHandle;

  private MPAlarmManager am;

  /**
   * The constructor
   * @param parent The encapsulating AnycastInputHandler, which creates and calls methods on this
   */
  public AIBrowseCursor(
    AnycastInputHandler parent,
    Filter filter,
    long browseId,
    MPAlarmManager am)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "AIBrowseCursor",
        new Object[] { parent, filter, new Long(browseId), am });

    this.am = am;
    this.parent = parent;
    this.filter = filter;
    this.browseId = browseId;

    // The sequence number will be incremented before a BrowseGet is sent
    // This way the current value of seqNum will also be the value to be expected in a response
    this.seqNum = -1;

    this.nextItem = null;
    this.browseClosed = false;
    this.browseFailed = false;
    this.failureReason = SIMPConstants.BROWSE_OK;

    keepAliveAlarmHandle =
      am.create(parent.getMessageProcessor().getCustomProperties().get_browse_liveness_timeout(), this);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "AIBrowseCursor", this);
  }

  //////////////////////////////////////////////////////////////
  // NonLockingCursor methods
  //////////////////////////////////////////////////////////////

  public void finished()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "finished");

    synchronized (this)
    {
      parent.sendBrowseStatus(SIMPConstants.BROWSE_CLOSE, browseId);

      if (keepAliveAlarmHandle != null)
      {
        keepAliveAlarmHandle.cancel();
        keepAliveAlarmHandle = null;
      }

      parent.removeBrowseCursor(browseId);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "finished");
  }

  public JsMessage next() throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "next");

    MessageItem item = null;

    // Serialize invocation of next.
    synchronized (this)
    {
      if (!browseClosed && !browseFailed)
      {
        // cancel the existing liveness alarm
        keepAliveAlarmHandle.cancel();

        seqNum++;
        Filter nextFilter = ((seqNum == 0) ? this.filter : null);
        parent.sendBrowseGet(browseId, seqNum, nextFilter);
        try
        {
          wait(parent.getMessageProcessor().getCustomProperties().get_browse_get_timeout());
        }
        catch (InterruptedException e)
        {
          // May not need to FFDC
          FFDCFilter.processException(
            e,
            "com.ibm.ws.sib.processor.impl.AIBrowseCursor.next",
            "1:179:1.25",
            this);

          SibTr.exception(tc, e);

          if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "next", item);

          return null;
        }
      }

      // if this.nextItem is null, then the wait ended because of a timeout, browse close or browse failure
      if (this.nextItem == null)
      {
        keepAliveAlarmHandle = null;

        if (browseClosed)
        {
          item = null;
        }
        else if (browseFailed)
        {
          // log error
          SIResourceException e =
            new SIResourceException(
              nls.getFormattedMessage(
                "BROWSE_FAILED_CWSIP0533",
                new Object[] {
                  parent.getDestName(),
                  failureReasonToString(failureReason) },
                null));

          FFDCFilter.processException(
            e,
            "com.ibm.ws.sib.processor.impl.AIBrowseCursor.next",
            "1:215:1.25",
            this);
          SibTr.exception(tc, e);

          if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "next", e);

          throw e;
        }
        else
        {
          // log error
          SIResourceException e =
            new SIResourceException(
              nls.getFormattedMessage(
                "BROWSE_TIMEOUT_CWSIP0532",
                new Object[] {parent.getDestName()},
                null));
          FFDCFilter.processException(
            e,
            "com.ibm.ws.sib.processor.impl.AIBrowseCursor.next",
            "1:236:1.25",
            this);
          SibTr.exception(tc, e);

          if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "next", e);

          throw e;
        }
      }
      else
      {
        item = this.nextItem;
        this.nextItem = null;

        // create a new liveness alarm
        keepAliveAlarmHandle =
          am.create(parent.getMessageProcessor().getCustomProperties().get_browse_liveness_timeout(), this);
      }
    }

    JsMessage returnMessage = null;
    if(item!=null)
    {
      returnMessage = item.getMessage();
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "next", returnMessage);

    return returnMessage;
  }





  //////////////////////////////////////////////////////////////
  // AlarmListener methods
  //////////////////////////////////////////////////////////////

  public void alarm(Object handle)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "alarm", handle);

    synchronized (this)
    {
      keepAliveAlarmHandle =
        am.create(parent.getMessageProcessor().getCustomProperties().get_browse_liveness_timeout(), this);
      parent.sendBrowseStatus(SIMPConstants.BROWSE_ALIVE, browseId);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "alarm");
  }

  //////////////////////////////////////////////////////////////
  // Methods invoked by AnycastInputHandler
  //////////////////////////////////////////////////////////////

  public synchronized void put(MessageItem message)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "put", message);

    JsMessage jsMsg = message.getMessage();
    long msgSeqNum = jsMsg.getGuaranteedRemoteBrowseSequenceNumber();

    if (msgSeqNum != seqNum)
    {
      this.nextItem = null;
      this.browseFailed = true;
      this.failureReason = SIMPConstants.BROWSE_OUT_OF_ORDER;
    }
    else
    {
      this.nextItem = message;
    }

    // wake up thread waiting for response
    notify();

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "put");
  }

  public synchronized void endBrowse()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "endBrowse");

    this.browseClosed = true;

    // wake up thread waiting for response
    notify();

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "endBrowse");
  }

  public synchronized void browseFailed(int reason)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "browseFailed");

    this.browseFailed = true;
    this.failureReason = reason;

    // wake up thread waiting for response
    notify();

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "browseFailed");
  }

  //////////////////////////////////////////////////////////////
  // Private Methods
  //////////////////////////////////////////////////////////////

  private String failureReasonToString(int failureReason)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "failureReasonToString", new Integer(failureReason));
    String frString = "";

    switch (failureReason)
    {
      case SIMPConstants.BROWSE_STORE_EXCEPTION:
        frString = "browse store exception";
        break;

      case SIMPConstants.BROWSE_OUT_OF_ORDER:
        frString = "browse out of order";
        break;

      case SIMPConstants.BROWSE_BAD_FILTER:
        frString = "browse bad filter";
        break;

      default:
        frString = "";
        break;
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "failureReasonToString", frString);
    return frString;
  }
}
