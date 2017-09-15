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
package com.ibm.ws.sib.processor.impl.indexes;

import java.util.HashMap;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler;
import com.ibm.ws.sib.processor.utils.index.IndexFilter;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * Class to represent the various lookups for different destination types
 */
public class ForeignBusIndex extends AbstractDestinationIndex
{
  /**
   * Trace for the component
   */
  private static final TraceComponent tc =
    SibTr.register(
      ForeignBusIndex.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);

  
  public static class Type extends AbstractDestinationType
  {
  }

  //a full map of name to foreign busses
  private HashMap nameIndex;

  public ForeignBusIndex()
  {
    super();
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "ForeignBusIndex");

    nameIndex = new HashMap(10);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "ForeignBusIndex", this);
  }

  public synchronized Entry put(DestinationHandler destinationHandler, AbstractDestinationType type)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "put", new Object[] { destinationHandler, type });

    Entry entry = super.put(destinationHandler, type);
    nameIndex.put(destinationHandler.getName(), entry);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "put", entry);

    return entry;
  }

  public synchronized void remove(DestinationHandler destinationHandler)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "remove", new Object[] { destinationHandler });

    super.remove(destinationHandler);
    DestinationEntry destEntry = (DestinationEntry) nameIndex.get(destinationHandler.getName());
    if(destEntry != null && destEntry.data == destinationHandler)
    {
      nameIndex.remove(destinationHandler.getName());
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "remove");
  }

  public synchronized DestinationHandler findByName(String name,
                                                    IndexFilter filter)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "findByName", new Object[] { name, filter });

    DestinationHandler destinationHandler = null;

    Entry entry = (Entry) nameIndex.get(name);
    if(entry != null && (filter == null || filter.matches(entry.type)))
    {
      destinationHandler = (DestinationHandler) entry.data;
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "findByName", destinationHandler);

    return destinationHandler;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.indexes.Index#remove(com.ibm.ws.sib.processor.impl.indexes.IndexEntry)
   */
  public synchronized void remove(Entry entry)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "remove", new Object[] { entry });

    super.remove(entry);
    DestinationHandler dh = (DestinationHandler) entry.data;
    DestinationEntry destEntry = (DestinationEntry) nameIndex.get(dh.getName());
    if(destEntry == entry)
    {
      nameIndex.remove(dh.getName());
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "remove");
  }
}
