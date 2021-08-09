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

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.impl.indexes.statemodel.State;
import com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler;
import com.ibm.ws.sib.processor.utils.index.Index;
import com.ibm.ws.sib.processor.utils.index.IndexFilter;
import com.ibm.ws.sib.utils.SIBUuid12;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * Class to represent the various lookups for different destination types
 */ 
public abstract class AbstractDestinationIndex extends Index
{
  /** 
   * Trace for the component
   */
  private static final TraceComponent tc =
    SibTr.register(
      AbstractDestinationIndex.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);

  /**
   * NLS for component
   */
  private static final TraceNLS nls =
    TraceNLS.getTraceNLS(SIMPConstants.RESOURCE_BUNDLE);
       
 
  public abstract static class AbstractDestinationType extends Index.Type
  {
    //State
    public State state = null;        
  }
  
  protected static class DestinationEntry extends Index.Entry
  {
    DestinationEntry(DestinationHandler destinationHandler, AbstractDestinationType type)
    {
      super(destinationHandler.getUuid(), destinationHandler, type);
    }
    
    DestinationHandler getHandler()
    {
      return (DestinationHandler) data;
    }
  }
  
  public AbstractDestinationIndex()
  {
    super();
  }
  
  public synchronized Entry put(DestinationHandler destinationHandler, AbstractDestinationType type)
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "put", new Object[] { destinationHandler, type });

    Entry entry = new DestinationEntry(destinationHandler, (AbstractDestinationType) type.clone());
    add(entry);    
    
    if (tc.isEntryEnabled())
      SibTr.exit(tc, "put", entry);
      
    return entry;      
  }
  
  public synchronized void remove(DestinationHandler destinationHandler)
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "remove", new Object[] { destinationHandler });

    Object dh = get(destinationHandler.getUuid());
    
    if (dh == null)
    {
      if (tc.isEntryEnabled()) SibTr.exit(tc, "remove", "SIErrorException");
      throw new SIErrorException(
        nls.getFormattedMessage(
        "INTERNAL_MESSAGING_ERROR_CWSIP0005",
        new Object[] { "AbstractDestinationIndex",
                       "1:121:1.14.1.2",
                       destinationHandler.getName() }, 
      null));
    }
    
    remove(destinationHandler.getUuid());              
  
    if (tc.isEntryEnabled())
      SibTr.exit(tc, "remove");
  }
  
  public synchronized DestinationHandler findByUuid(SIBUuid12 uuid, IndexFilter filter)
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "findByUuid", new Object[] { uuid, filter });
    
    DestinationHandler destinationHandler = (DestinationHandler) get(uuid,filter);        
    
    if (tc.isEntryEnabled())
      SibTr.exit(tc, "findByUuid", destinationHandler);

    return destinationHandler;
  }  
    
  public synchronized AbstractDestinationType getType(DestinationHandler destinationHandler)
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "getType", new Object[] { destinationHandler });

    AbstractDestinationType type = (AbstractDestinationType)_getType(destinationHandler).clone();  
          
    if (tc.isEntryEnabled())
      SibTr.exit(tc, "getType", type);
  
    return type;
  }
  
  protected synchronized AbstractDestinationType _getType(DestinationHandler destinationHandler)
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "_getType", new Object[] { destinationHandler });

    AbstractDestinationType type =
      (AbstractDestinationType) (getType(destinationHandler.getUuid()));  
        
    if (tc.isEntryEnabled())
      SibTr.exit(tc, "_getType", type);

    return type;
  }
  
  public synchronized void setType(DestinationHandler destinationHandler, Type type)
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "setType", new Object[] { destinationHandler, type });

    setType(destinationHandler.getUuid(), type);
            
    if (tc.isEntryEnabled())
      SibTr.exit(tc, "setType");
  }
  
  public synchronized State getState(DestinationHandler destinationHandler)
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "getState", new Object[] { destinationHandler });

    AbstractDestinationType type = _getType(destinationHandler);
    State state = null;
    if(type!=null)
      state = type.state;  
        
    if (tc.isEntryEnabled())
      SibTr.exit(tc, "getState", state);

    return state;
  }
  
  public synchronized void create(DestinationHandler destinationHandler)
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "create", new Object[] { destinationHandler });

    AbstractDestinationType type = _getType(destinationHandler);    
    type.state = type.state.create();
    //setType(destinationHandler, type);
    
    if (tc.isEntryEnabled())
      SibTr.exit(tc, "create", type.state);
  }
  
  public synchronized void cleanup(DestinationHandler destinationHandler)
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "cleanup", new Object[] { destinationHandler });

    AbstractDestinationType type = _getType(destinationHandler);    
    type.state = type.state.cleanup();
    //setType(destinationHandler, type);
  
    if (tc.isEntryEnabled())
      SibTr.exit(tc, "cleanup", type.state);
  }
  
  public synchronized void delete(DestinationHandler destinationHandler)
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "delete", new Object[] { destinationHandler });

    AbstractDestinationType type = _getType(destinationHandler);    
    type.state = type.state.delete();
    //setType(destinationHandler, type);

    if (tc.isEntryEnabled())
      SibTr.exit(tc, "delete", type.state);
  }
  
  public synchronized void defer(DestinationHandler destinationHandler)
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "defer", new Object[] { destinationHandler });

    AbstractDestinationType type = _getType(destinationHandler);    
    type.state = type.state.defer();
    //setType(destinationHandler, type);

    if (tc.isEntryEnabled())
      SibTr.exit(tc, "defer", type.state);
  }
  
  public synchronized void cleanupComplete(DestinationHandler destinationHandler)
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "cleanupComplete", new Object[] { destinationHandler });

    AbstractDestinationType type = _getType(destinationHandler);    
    type.state = type.state.cleanupComplete();
    //setType(destinationHandler, type);

    if (tc.isEntryEnabled())
      SibTr.exit(tc, "cleanupComplete", type.state);
  } 

  /**
   * Before reconciliation, we need to move any inDoubt handlers
   * to the Unreconciled state.
   * If the destination gets reconciled then we have recovered.
   * If not, we might get moved back to the inDoubt state, arguing
   * that the corrupt WCCM file is stil causing problems,
   * or finally WCCM might now tell us to remove the destination
   * 
   * @author tpm
   */  
  public synchronized void putUnreconciled(DestinationHandler destinationHandler)
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "putUnreconciled", new Object[] { destinationHandler });

    AbstractDestinationType type = _getType(destinationHandler);    
    type.state = type.state.putUnreconciled();
    //setType(destinationHandler, type);
    if (tc.isEntryEnabled())
      SibTr.exit(tc, "putUnreconciled", type.state);
  }

  /**
   * Not sure whether it is safe to delete this destination or not.
	 * A destination goes in doubt when we are not told to recreate it from
	 * WCCM on reboot, but have not been told to delete it either.
   * 
   * @author tpm
   */  
  public synchronized void putInDoubt(DestinationHandler destinationHandler)
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "putInDoubt", new Object[] { destinationHandler });

    AbstractDestinationType type = _getType(destinationHandler);    
    type.state = type.state.putInDoubt();
    //setType(destinationHandler, type);
    if (tc.isEntryEnabled())
      SibTr.exit(tc, "putInDoubt", type.state);
  }
  
  public synchronized void corrupt(DestinationHandler destinationHandler)
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "corrupt", new Object[] { destinationHandler });

    AbstractDestinationType type = _getType(destinationHandler);    
    type.state = type.state.corrupt();

    if (tc.isEntryEnabled())
      SibTr.exit(tc, "corrupt", type.state);
  }
  
  public synchronized void reset(DestinationHandler destinationHandler)
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "reset", new Object[] { destinationHandler });

    AbstractDestinationType type = _getType(destinationHandler);    
    type.state = type.state.reset();

    if (tc.isEntryEnabled())
      SibTr.exit(tc, "reset", type.state);
  }
  
  public synchronized void addPseudoUuid(DestinationHandler destinationHandler, SIBUuid12 pseudoUuid)
  {
    if (tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "addPseudoUuid",
        new Object[] { destinationHandler, pseudoUuid });

    Entry entry = (Entry) index.get(destinationHandler.getUuid());      
    index.put(pseudoUuid, entry);
    
    if (tc.isEntryEnabled())
      SibTr.exit(tc, "addPseudoUuid");
  }
  
  public synchronized void removePseudoUuid(SIBUuid12 pseudoUuid)
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "removePseudoUuid", new Object[] { pseudoUuid });

    index.remove(pseudoUuid);

    if (tc.isEntryEnabled())
      SibTr.exit(tc, "removePseudoUuid");
  }

  public synchronized boolean containsDestination(DestinationHandler destinationHandler)
  {
      if (tc.isEntryEnabled())
        SibTr.entry(tc, "containsDestination", new Object[] { destinationHandler });

      boolean result = false;
      if (destinationHandler.getUuid() != null) 
          result = containsKey(destinationHandler.getUuid());
      
      if (tc.isEntryEnabled())
        SibTr.exit(tc, "containsDestination", Boolean.valueOf(result));

      return result;
  }
}
