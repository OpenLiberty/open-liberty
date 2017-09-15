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
package com.ibm.ws.sib.processor.impl.store;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.transaction.XAResourceInfo;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * Information needed to recover a MessageStore XA resource
 */
public class MsgStoreXAResourceInfo implements XAResourceInfo
{

  /**
   * Comment for <code>serialVersionUID</code>
   */
  private static final long serialVersionUID = -3135180725448875737L;

  /**
   * Trace for the component
   */
  private static final TraceComponent tc =
    SibTr.register(
      MsgStoreXAResourceInfo.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);

  private final String meUuid;
  private final String rmName;
  private final String meBus;
  private final String meName;

  public MsgStoreXAResourceInfo(String meUuid, String meName, String meBus)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "MsgStoreXAResourceInfo", new String[]{meUuid, meName, meBus});

    this.meUuid = meUuid;
    this.meBus = meBus;
    this.meName = meName;
    rmName = SIMPConstants.PRODUCT_NAME+meName;

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "MsgStoreXAResourceInfo", this);
  }

  public String getMEUuid()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getMEUuid");
      SibTr.exit(tc, "getMEUuid", meUuid);
    }
    return meUuid;
  }

  public String getMEBus()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getMEBus");
      SibTr.exit(tc, "getMEBus", meBus);
    }
    return meBus;
  }

  public String getMEName()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getMEName");
      SibTr.exit(tc, "getMEName", meName);
    }
    return meName;
  }

  /*
   *  (non-Javadoc)
   * @see com.ibm.ws.Transaction.XAResourceInfo#getRMName()
   */
  public String getRMName()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getRMName");
      SibTr.exit(tc, "getRMName", rmName);
    }
    return rmName;
  }

  /*
   *  (non-Javadoc)
   * @see java.lang.Object#equals(java.lang.Object)
   */
  public boolean equals(Object o)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "equals", o);
    boolean returnValue = false;
    if(o != null && o instanceof MsgStoreXAResourceInfo)
    {
      MsgStoreXAResourceInfo info = (MsgStoreXAResourceInfo)o;
      if(info.getMEUuid().equals(getMEUuid()) && info.getRMName().equals(getRMName()))
      {
        returnValue = true;
      }
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "equals", new Boolean(returnValue));
    return returnValue;
  }

  /*
   *  (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  public int hashCode()
  {
    return meUuid.hashCode();
  }

  /*
   *  (non-Javadoc)
   * @see com.ibm.ws.Transaction.XAResourceInfo#commitInLastPhase()
   */
  public boolean commitInLastPhase()
  {
    return false;
  }



}
