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
public class DestinationIndex extends AbstractDestinationIndex
{
  /** 
   * Trace for the component
   */
  private static final TraceComponent tc =
    SibTr.register(
      DestinationIndex.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);
      
  
  
  public static class Type extends AbstractDestinationIndex.AbstractDestinationType
  {
    //Queue or Topicspace
    public Boolean queue = null;
    //alias
    public Boolean alias = null;
    //foreign destination
    public Boolean foreignDestination = null;
    //does it have local localizations
    public Boolean local = null;
    //does it have remote localizations
    public Boolean remote = null;        
  }    
 
  //a map from bus name to a map of destinations by name
  private HashMap<String, HashMap<String, Entry>> busIndex;
 
  public DestinationIndex(String localBusName)
  {
    super();
    
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "DestinationIndex", localBusName);

    busIndex = new HashMap<String, HashMap<String, Entry>>(1);
    HashMap<String, Entry> localBus = new HashMap<String, Entry>(10);
    busIndex.put(localBusName, localBus);
      
    if (tc.isEntryEnabled())
      SibTr.exit(tc, "DestinationIndex", this);       
  }
  
  public synchronized Entry put(DestinationHandler destinationHandler, AbstractDestinationType type)
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "put", new Object[] { destinationHandler, type });

    Entry entry = super.put(destinationHandler, type);
    addToBusMap(destinationHandler, entry);      
    
    if (tc.isEntryEnabled())
      SibTr.exit(tc, "put", entry);
    
    return entry;
  }
  
  synchronized void addToBusMap(DestinationHandler destinationHandler, Entry entry)
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "addToBusMap", new Object[] { destinationHandler, entry });

    HashMap<String, Entry> busMap = getBusMap(destinationHandler.getBus());
    DestinationEntry destEntry = (DestinationEntry) busMap.get(destinationHandler.getName());
    if(destEntry != null)
    {
      if (tc.isDebugEnabled())
        SibTr.debug(tc, "*** put, duplicate name", new Object[]{destinationHandler, destEntry.data});
    }
    busMap.put(destinationHandler.getName(), entry);

    if (tc.isEntryEnabled())
      SibTr.exit(tc, "addToBusMap");
  }
  
  public synchronized void remove(DestinationHandler destinationHandler)
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "remove", new Object[] { destinationHandler });

    super.remove(destinationHandler);
    HashMap<String, Entry> busMap = getBusMap(destinationHandler.getBus());
    DestinationEntry destEntry = (DestinationEntry) busMap.get(destinationHandler.getName());
    if(destEntry != null && destEntry.data == destinationHandler)
    {
      busMap.remove(destinationHandler.getName());
    } 
    else
    {
      if(tc.isDebugEnabled())
        SibTr.debug(tc, "destination is not removed from the bus map", new Object[]{destEntry, destinationHandler});
    }
  
    if (tc.isEntryEnabled())
      SibTr.exit(tc, "remove");
  }
  
  public synchronized DestinationHandler findByName(String name,
                                                    String busName,
                                                    IndexFilter filter)
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "findByName", new Object[] { busName, name, filter });
  
    DestinationHandler destinationHandler = null;
  
    HashMap<String, Entry> busMap = getBusMap(busName);    
  
    AbstractDestinationIndex.DestinationEntry destination =
      (AbstractDestinationIndex.DestinationEntry) busMap.get(name);
    if(destination != null && (filter == null || filter.matches(destination.type)))
    {
      destinationHandler = destination.getHandler();
    }
  
    if (tc.isEntryEnabled())
      SibTr.exit(tc, "findByName", destinationHandler);

    return destinationHandler;
  }
          
  private synchronized HashMap<String, Entry> getBusMap(String busName)
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "getBusMap", new Object[] { busName });      
  
    HashMap<String, Entry> busMap = busIndex.get(busName);
    if(busMap == null)
    {
      busMap = new HashMap<String, Entry>(10);
      busIndex.put(busName, busMap);
    }    

    if (tc.isEntryEnabled())
      SibTr.exit(tc, "getBusMap", busMap);

    return busMap; 
  }
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.indexes.Index#remove(com.ibm.ws.sib.processor.impl.indexes.IndexEntry)
   */
  public synchronized void remove(Entry entry)
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "remove", new Object[] { entry });

    super.remove(entry);
    DestinationHandler dh = (DestinationHandler) entry.data;
    HashMap<String, Entry> busMap = getBusMap(dh.getBus());
    DestinationEntry destEntry = (DestinationEntry) busMap.get(dh.getName());
    if(destEntry == entry)
    {
      busMap.remove(dh.getName());
    }    
    
    if (tc.isEntryEnabled())
      SibTr.exit(tc, "remove");
  }
  
  public synchronized boolean containsKey(String bus, String name)
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "containsKey", new Object[] { bus, name });
    
    HashMap<String, Entry> busMap = getBusMap(bus);
    boolean res = busMap.containsKey(name);
  
    if (tc.isEntryEnabled())
      SibTr.exit(tc, "containsKey", new Boolean(res));
    return res;    
  }
  
  public synchronized void create(DestinationHandler destinationHandler)
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "create", new Object[] { destinationHandler });

    AbstractDestinationType type = _getType(destinationHandler);    
    type.state = type.state.create();
    //setType(destinationHandler, type);
    
    if(type.state.isVisible())
    {
      addToBusMap(destinationHandler, (Entry) index.get(destinationHandler.getUuid()));
    }
  
    if (tc.isEntryEnabled())
      SibTr.exit(tc, "create");
  }
  
  public synchronized void setLocalizationFlags(DestinationHandler destinationHandler)
  {
    Type type = (Type) getType(destinationHandler);
    type.local = destinationHandler.hasLocal();
    type.remote = destinationHandler.hasRemote();
    setType(destinationHandler, type);
  }
}
