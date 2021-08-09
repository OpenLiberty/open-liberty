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
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.mfp.JsMessage;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.ws.sib.utils.SIBUuid12;
import com.ibm.ws.sib.utils.SIBUuid8;
import com.ibm.ws.sib.processor.impl.interfaces.BrowseCursor;
import com.ibm.ws.sib.processor.utils.SIMPUtils;
import com.ibm.ws.sib.processor.utils.am.MPAlarmManager;
import com.ibm.ejs.util.am.Alarm;
import com.ibm.ejs.util.am.AlarmListener;
import com.ibm.wsspi.sib.core.exception.SISessionDroppedException;

/**
 * Encapsulates a remote browse session, at the DME
 */
public final class AOBrowserSession implements AlarmListener
{

  private MPAlarmManager am;

  // NLS for component
  private static final TraceNLS nls =
    TraceNLS.getTraceNLS(SIMPConstants.RESOURCE_BUNDLE);

  private static final TraceComponent tc =
    SibTr.register(
      AOBrowserSession.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);

  private final AnycastOutputHandler parent; // the encapsulating handler

  private BrowseCursor browseCursor; // the browse cursor

  private final AOBrowserSessionKey key; // the key of this AOBrowserSession
  private final SIBUuid8 remoteMEUuid;
  private final SIBUuid12 gatheringTargetDestUuid;

  private long expectedSequenceNumber; // the expected sequence number of the next BrowseGet message

  private Alarm expiryAlarmHandle; // the alarm that will expire this session

  private boolean closed = false; // whether this session has been closed

  /**
   * The constructor
   * @param parent The encapsulating AnycastOutputHandler, which creates and calls methods on this
   * @param browseCursor The browse cursor to be used
   * @param remoteMEUuid The UUID of the remote ME which is requesting the browse
   * @param browseId The unique Id of the browse session, wrt the remoteMEUuid
   */
  public AOBrowserSession(AnycastOutputHandler parent,
                          BrowseCursor browseCursor,
                          SIBUuid8 remoteMEUuid,
                          SIBUuid12 gatheringTargetDestUuid,
                          long browseId,
                          MPAlarmManager am)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "AOBrowserSession",
        new Object[]{parent, browseCursor, remoteMEUuid, gatheringTargetDestUuid, Long.valueOf(browseId), am});

    synchronized (this)
    {
      this.parent = parent;
      this.browseCursor = browseCursor;
      this.key = new AOBrowserSessionKey(remoteMEUuid, gatheringTargetDestUuid, browseId);
      this.remoteMEUuid = remoteMEUuid;
      this.gatheringTargetDestUuid = gatheringTargetDestUuid;
      this.am = am;

      this.expectedSequenceNumber = 0;

      expiryAlarmHandle =
        am.create(parent.getMessageProcessor().getCustomProperties().get_browse_expiry_timeout(), this);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "AOBrowserSession", this);

  }

  /**
   * Get the key of this browser session
   * @return key
   */
  public AOBrowserSessionKey getKey()
  {
    return key;
  }

  /**
   * Send the next message in this session to the remote ME.
   * @param seqNum The sequence number for the next message
   * @return true if closed, either because reached end of cursor, or due to some failure, or due to expiry; else false
   */
  public synchronized boolean next(long seqNum)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "next",
      new Long(seqNum));

    // cancel the existing alarm and create a new one
    expiryAlarmHandle.cancel();
    expiryAlarmHandle =
      am.create(parent.getMessageProcessor().getCustomProperties().get_browse_expiry_timeout(), this);

    if (closed)
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "next", new Boolean(closed));

      return true;
    }

    if (seqNum == expectedSequenceNumber)
    {
      try
      {
        JsMessage msg = browseCursor.next();
        if (msg != null)
        { // a message was found
          parent.sendBrowseData(msg, remoteMEUuid, gatheringTargetDestUuid, key.getRemoteMEUuid(), key.getBrowseId(), expectedSequenceNumber);
          expectedSequenceNumber++;
        }
        else
        { // no more messages in the cursor
          parent.sendBrowseEnd(remoteMEUuid, gatheringTargetDestUuid, key.getBrowseId(), SIMPConstants.BROWSE_OK);
          close();
        }
      }
      catch (SIException e)
      {
        // SIResourceException shouldn't occur so FFDC.
        FFDCFilter.processException(
          e,
          "com.ibm.ws.sib.processor.impl.AOBrowserSession.next",
          "1:182:1.30",
          this);

        Exception e2 =
          new SIResourceException(nls.getFormattedMessage(
            "INTERNAL_MESSAGING_ERROR_CWSIP0002",
            new Object[] {
              "com.ibm.ws.sib.processor.impl.AOBrowserSession",
              "1:190:1.30",
              e },
            null),
          e);

        SibTr.exception(tc, e2);
        SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
          new Object[] {
            "com.ibm.ws.sib.processor.impl.AOBrowserSession",
            "1:199:1.30",
            e });

        parent.sendBrowseEnd(remoteMEUuid, gatheringTargetDestUuid, key.getBrowseId(), SIMPConstants.BROWSE_STORE_EXCEPTION);
        close();
      }
    }
    else
    { // wrong sequence number
      parent.sendBrowseEnd(remoteMEUuid, gatheringTargetDestUuid, key.getBrowseId(), SIMPConstants.BROWSE_OUT_OF_ORDER);
      close();
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "next", new Boolean(closed));

    return closed;
  }

  /**
   * Close this session
   */
  public synchronized final void close()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "close");

    closed = true;
    if (browseCursor != null) {
      try
      {
        browseCursor.finished();
      }
      catch (SISessionDroppedException e)
      {
        FFDCFilter.processException(
            e,
            "com.ibm.ws.sib.processor.impl.AOBrowserSession.close",
            "1:237:1.30",
            this);

        SibTr.exception(tc, e);
        SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
          new Object[] {
            "com.ibm.ws.sib.processor.impl.AOBrowserSession.close",
            "1:244:1.30",
            SIMPUtils.getStackTrace(e) });
      } // deallocate resources
      browseCursor = null;
    }
    if (expiryAlarmHandle != null) {
      expiryAlarmHandle.cancel();
      expiryAlarmHandle = null;
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "close");

  }

  /**
   * Keep this session alive
   */
  public synchronized final void keepAlive()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "keepAlive");

    if (!closed)
    {
      if (expiryAlarmHandle != null)
      {
        // cancel the existing alarm
        expiryAlarmHandle.cancel();
      }
      // create a new alarm
      expiryAlarmHandle =
        am.create(parent.getMessageProcessor().getCustomProperties().get_browse_expiry_timeout(), this);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "keepAlive");

  }

  /**
   * The alarm has expired
   */
  public void alarm(Object thandle)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "alarm", thandle);

    synchronized (this)
    {
      if (expiryAlarmHandle != null)
      {
        expiryAlarmHandle = null;
        close();
        // call into parent asking it to remove me
        parent.removeBrowserSession(key);
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "alarm");

  }

  public long getExpectedSequenceNumber()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getExpectedSequenceNumber");

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getExpectedSequenceNumber", new Long(expectedSequenceNumber));
    return expectedSequenceNumber;
  }

}
