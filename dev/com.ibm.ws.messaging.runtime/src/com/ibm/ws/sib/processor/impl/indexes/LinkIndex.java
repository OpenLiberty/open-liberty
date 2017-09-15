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
import com.ibm.ws.sib.processor.impl.MQLinkHandler;
import com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler;
import com.ibm.ws.sib.processor.utils.index.IndexFilter;
import com.ibm.ws.sib.utils.SIBUuid8;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * Class to represent the various lookups for different destination types
 */
public class LinkIndex extends AbstractDestinationIndex
{

  public static class Type extends AbstractDestinationType
  {
    public Boolean local = null;
    public Boolean mqLink = null;
    public Boolean remote = null;
  }

  /**
   * Trace for the component
   */
  private static final TraceComponent tc =
    SibTr.register(
      LinkIndex.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);

 
  //a full map of name to links
  private HashMap<String, Entry> nameIndex;
  //a map from MQ Link Uuids to links
  private HashMap<SIBUuid8, Entry> mqLinkIndex;

  public LinkIndex()
  {
    super();
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "LinkIndex");

    nameIndex = new HashMap<String, Entry>(10);
    mqLinkIndex = new HashMap<SIBUuid8, Entry>(10);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "LinkIndex", this);
  }
  public synchronized Entry put(DestinationHandler destinationHandler, AbstractDestinationType type)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "put", new Object[] { destinationHandler, type });

    Entry entry = super.put(destinationHandler, type);
    nameIndex.put(destinationHandler.getName(), entry);
    Type linkType = (Type) type;
    if(linkType.mqLink.booleanValue())
    {
      MQLinkHandler mqLink = (MQLinkHandler) destinationHandler;
      mqLinkIndex.put(mqLink.getMqLinkUuid(), entry);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "put", entry);

    return entry;
  }

  public synchronized DestinationHandler findByName(String name,
                                                    IndexFilter filter)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "findByName", new Object[] { name, filter });

    DestinationHandler destinationHandler = null;

    Entry entry =
      nameIndex.get(name);
    if(entry != null && (filter == null || filter.matches(entry.type)))
    {
      destinationHandler = (DestinationHandler) entry.data;
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "findByName", destinationHandler);

    return destinationHandler;
  }

  public synchronized DestinationHandler findByMQLinkUuid(SIBUuid8 uuid,
                                                      IndexFilter filter)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "findByMQLinkUuid", new Object[] { uuid, filter });

    DestinationHandler destinationHandler = null;

    Entry entry =
      mqLinkIndex.get(uuid);
    if(entry != null && (filter == null || filter.matches(entry.type)))
    {
      destinationHandler = (DestinationHandler) entry.data;
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "findByMQLinkUuid", destinationHandler);

    return destinationHandler;
  }

  public synchronized void remove(DestinationHandler destinationHandler)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "remove", new Object[] { destinationHandler });

    super.remove(destinationHandler);
    if(destinationHandler.isMQLink()) removeFromMQLinkIndex(destinationHandler);
    DestinationEntry destEntry = (DestinationEntry) nameIndex.get(destinationHandler.getName());
    if(destEntry != null && destEntry.data == destinationHandler)
    {
      nameIndex.remove(destinationHandler.getName());
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "remove");
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
    if(dh.isMQLink()) removeFromMQLinkIndex(dh);
    DestinationEntry destEntry = (DestinationEntry) nameIndex.get(dh.getName());
    if(destEntry == entry)
    {
      nameIndex.remove(dh.getName());
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "remove");
  }

  private void removeFromMQLinkIndex(DestinationHandler dh)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "removeFromMQLinkIndex", dh);
    MQLinkHandler mqLink = (MQLinkHandler) dh;
    mqLinkIndex.remove(mqLink.getMqLinkUuid());
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "removeFromMQLinkIndex");
  }

  public synchronized void setLocalizationFlags(DestinationHandler destinationHandler,
                                                boolean local,
                                                boolean remote)
  {
    Type type = (Type) getType(destinationHandler);
    type.local = local;
    type.remote = remote;
    setType(destinationHandler, type);
  }
}
