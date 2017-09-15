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
package com.ibm.ws.sib.processor.impl.store.items;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;

import com.ibm.websphere.ras.TraceComponent;

import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.msgstore.AbstractItem;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.utils.ras.FormattedWriter;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 *
 */
public class AICompletedPrefixItem extends SIMPItem
{
  private long _tick;

  // Standard debug/trace
  private static final TraceComponent tc =
        SibTr.register(
          AICompletedPrefixItem.class,
          SIMPConstants.MP_TRACE_GROUP,
          SIMPConstants.RESOURCE_BUNDLE);

  /**
   * Persistent data version number.
   */
  private static final int PERSISTENT_VERSION = 1;
  
  public AICompletedPrefixItem(long tick)
  {
    super();

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "AICompletedPrefixItem", Long.valueOf(tick));

    this._tick = tick;
      
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "AICompletedPrefixItem", this);
  }

  public AICompletedPrefixItem()
  {
    super();
  }

  public int getStorageStrategy()
  {
    return AbstractItem.STORE_ALWAYS;
  }

  public long getMaximumTimeInStore()
  {
    return AbstractItem.NEVER_EXPIRES;
  }
  
  /*
   * (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.store.itemstreams.SIMPReferenceStream#getVersion()
   */
  public int getPersistentVersion()
  {
    return PERSISTENT_VERSION;
  } 

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.store.items.SIMPItem#getPersistentData(java.io.ObjectOutputStream)
   */
  public void getPersistentData(ObjectOutputStream oos)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getPersistentData", oos);

    try
    { 
      HashMap hm = new HashMap();
      hm.put("tick", Long.valueOf(_tick));
      
      oos.writeObject(hm);
    }
    catch (IOException e)
    {
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.impl.store.items.AICompletedPrefixItem.getPersistentData",
        "1:129:1.18",
        this);

      SIErrorException e2 = new SIErrorException(e);
      SibTr.exception(tc, e2);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "getPersistentData");

      throw e2;
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getPersistentData");
  }

  public void restore(ObjectInputStream ois, int dataVersion) 
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc, "restore", new Object[] { ois, Integer.valueOf(dataVersion) });
    
    checkPersistentVersionId(dataVersion);
    
    try
    {
      HashMap hm = (HashMap)ois.readObject();
      
      _tick = ((Long)hm.get("tick")).longValue();
    }
    catch (Exception e)
    {
      // FFDC
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.impl.store.items.AICompletedPrefixItem.restore",
        "1:165:1.18",
        this);

      SIErrorException e2 = new SIErrorException(e);
      SibTr.exception(tc, e2);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "restore", e2);

      throw e2;
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "restore");
  }
  
  public long getTick()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getTick");
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getTick", Long.valueOf(_tick));

    return _tick;
  }

  public void setTick(long tick)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "setTick", Long.valueOf(tick));
    
    this._tick = tick;

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "setTick");
  }

  /**
   * Prints the Message Details to the xml output.
   */
  public void xmlWriteOn(FormattedWriter writer) throws IOException
  {
    writer.newLine();
    writer.taggedValue("completedPrefix", _tick);
  }

  public String toString()
  {
    return super.toString() + "[" + _tick + "]"; 
  }
}
