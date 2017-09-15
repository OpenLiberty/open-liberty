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
package com.ibm.ws.sib.processor.matching;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.wsspi.sib.core.ConsumerSetChangeCallback;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * Wrapper for ConsumerSetChangeCallback to support synchronization.
 */
public class WrappedConsumerSetChangeCallback
{
  private static final TraceComponent tc =
    SibTr.register(
      WrappedConsumerSetChangeCallback.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);

  /**
   * The underlying callback object.
   */
  private ConsumerSetChangeCallback callback;

  /**
   * Variable used to track the transition requests driven for the wrapped
   * callback. It supports the handling of multiple requests for the callback
   * to be driven across multiple threads.
   */
  private int transition;

  /**
   * Constructor for wrapped object
   */
  public WrappedConsumerSetChangeCallback(ConsumerSetChangeCallback callback)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "WrappedConsumerSetChangeCallback", new Object[]{ callback});

    this.callback = callback;
    transition = 0;

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "WrappedConsumerSetChangeCallback", this);
  }

  /**
   * @return the wrapped callback object
   */
  public ConsumerSetChangeCallback getCallback()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getCallback");
      SibTr.exit(tc, "getCallback", callback);	
    }
    return callback;
  }

  /**
   * Method transitionEvent
   *
   * Called when the number of consumers for a registered callback drops to zero
   * or rises above zero.
   *
   * @param toEmpty
   */
  public synchronized void transitionEvent(boolean isEmpty)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "transitionEvent", new Object[]{ new Boolean(isEmpty)});

    if(isEmpty)
    {
      transition--;
    }
    else
    {
      transition++;
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "transitionEvent", new Integer(transition));
  }

  /**
   * Method consumerSetChange
   *
   * If the transition variable is negative at the point of invoking the callback,
   * then the callback is being called in order to signal that there are no longer
   * any consumers for the expression on which the callback was registered. Similarly,
   * if transition is positive, then the callback is being called in order to signal that
   * there are now consumers for the expression on which the callback was registered. If
   * transition is zero, then no action need be taken.
   *
   * @param isEmpty
   */
  public synchronized void consumerSetChange()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "consumerSetChange");

    if (transition < 0)
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        SibTr.debug(tc, "Negative transition, isEmpty is true");
      callback.consumerSetChange(true);
    }
    else if (transition > 0)
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        SibTr.debug(tc, "Positive transition, isEmpty is false");
      callback.consumerSetChange(false);
    }
    else
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        SibTr.debug(tc, "Zero transition, no callback");
    }

    // Set transition to zero
    transition = 0;

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "consumerSetChange");
  }

  /* (non-Javadoc)
   * @see java.lang.Object#equals(java.lang.Object)
   */
  public boolean equals(Object o)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "equals", o);
    boolean areEqual = false;
    if (o instanceof WrappedConsumerSetChangeCallback)
    {
      ConsumerSetChangeCallback otherCB =
        ((WrappedConsumerSetChangeCallback) o).getCallback();

      if(callback.equals(otherCB))
        areEqual = true;
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "equals", new Boolean(areEqual));
    return areEqual;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  public int hashCode()
  {
    return callback.hashCode();
  }
}
