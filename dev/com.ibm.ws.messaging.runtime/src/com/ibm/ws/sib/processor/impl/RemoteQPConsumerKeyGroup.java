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
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.impl.interfaces.JSConsumerKey;
import com.ibm.ws.sib.processor.impl.interfaces.RefillKey;
import com.ibm.ws.sib.processor.impl.interfaces.RemoteDispatchableKey;
import com.ibm.wsspi.sib.core.SelectionCriteria;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * The subclass of ConsumerKeyGroup that is used at the RME (for remote get)
 */
public final class RemoteQPConsumerKeyGroup extends LocalQPConsumerKeyGroup implements RemoteDispatchableKey
{

  private static TraceComponent tc =
    SibTr.register(
      RemoteQPConsumerKeyGroup.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);

 
  private final RemoteConsumerDispatcher rcd;
  private SelectionCriteria[] allCriterias; // SelectionCriterias of each member
  private final Object criteriaLock; // synchronization for reading/writing allCriterias

  public RemoteQPConsumerKeyGroup(RemoteConsumerDispatcher rcd)
  {
    super(rcd, null);
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "RemoteQPConsumerKeyGroup", rcd);

    this.rcd = rcd;
    allCriterias = null;
    criteriaLock = new Object();

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "RemoteQPConsumerKeyGroup", this);
  }

  public final SelectionCriteria[] getSelectionCriteria()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "getSelectionCriteria");
    SelectionCriteria[] criteria;
    synchronized (criteriaLock)
    {
      criteria = allCriterias;
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "getSelectionCriteria", criteria);

    return criteria;
  }

  public void notifyException(SIException e)
  {
    // ignore since only called when RCD cannot reissue get, which means either the stream is being flushed
    // so consumer has already been notified, or it is a serious error which we have already FFDC'd
  }

  /** overriding superclass method */
  public void addMember(JSConsumerKey key) throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "addMember", key);

    super.addMember(key); // superclass method does most of the work

    synchronized (criteriaLock)
    {
      if (allCriterias != null)
      {
        SelectionCriteria[] newCriterias = new SelectionCriteria[allCriterias.length + 1];
        System.arraycopy(allCriterias, 0, newCriterias, 0, allCriterias.length);
        allCriterias = newCriterias;
      }
      else
      {
        allCriterias = new SelectionCriteria[1];
      }

      allCriterias[allCriterias.length - 1] = ((RemoteQPConsumerKey) key).getSelectionCriteria()[0];
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "addMember");
  }

  public void removeMember(JSConsumerKey key)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "removeMember", key);

    SelectionCriteria toRemove = ((RemoteQPConsumerKey) key).getSelectionCriteria()[0];

    synchronized (criteriaLock)
    {
      if (allCriterias != null)
      {
        for (int i=0; i < allCriterias.length; i++)
        {
          if (toRemove == allCriterias[i])
          {
            // found
            if (allCriterias.length == 1)
            {
              allCriterias = null;
            }
            else
            {
              SelectionCriteria[] newCriterias = new SelectionCriteria[allCriterias.length - 1];
              if (i > 0)
              {
                System.arraycopy(allCriterias, 0, newCriterias, 0, i);
              }
              if (i < allCriterias.length - 1)
              {
                // more to copy
                System.arraycopy(allCriterias, i+1, newCriterias, i, allCriterias.length - (i+1));
              }
              allCriterias = newCriterias;
            } // end else
            break; // break out of for loop

          } // end if (toRemove == allCriterias[i])
        } // end for
      } // end if (allCriterias != null)
    } // end synchronized ...
    // note we did not throw an exception/error if the SelectionCriteria is not found since we rely on the
    // superclass method to check for such mistakes.

    super.removeMember(key);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "removeMember");

  }

  public AIStreamKey issueGet(long timeout, RefillKey refillCallback) throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "issueGet", Long.valueOf(timeout));

    SelectionCriteria[] latestCriterias;
    synchronized (criteriaLock)
    { // synchronize on read
      latestCriterias = allCriterias;
    }

    // pass null since don't associate get with any ConsumerKey
    AIStreamKey key = rcd.issueGet(latestCriterias, timeout, this, refillCallback);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "issueGet", key);

    return key;
  }

  public void setLatestTick(long tick) 
  {
    //no-op
  }
}
