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
package com.ibm.ws.sib.processor.utils.am;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.utils.am.BatchedTimeoutManager.LinkedListEntry;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * The entry provided to the BatchedTimeoutManager. The get and set methods are used
 * by the BatchedTimeoutManager to store data in this entry, for efficient removal.
 */
public abstract class AbstractBatchedTimeoutEntry implements BatchedTimeoutEntry
{
  //trace
  private static final TraceComponent tc =
    SibTr.register(
      AbstractBatchedTimeoutEntry.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);

   
  LinkedListEntry entry;

  public LinkedListEntry getEntry()
  {
    if (tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getEntry");
      SibTr.exit(tc, "getEntry", entry);
    }

    return entry;
  }

  public void setEntry(LinkedListEntry entry)
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "setEntry", entry);

    this.entry = entry;

    if (tc.isEntryEnabled())
      SibTr.exit(tc, "setEntry");
  }
  
  public void cancel()
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "cancel");

    if (tc.isEntryEnabled())
      SibTr.exit(tc, "cancel");
  }
}
