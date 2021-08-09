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
import com.ibm.websphere.sib.Reliability;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.msgstore.AbstractItem;
import com.ibm.ws.sib.msgstore.transactions.Transaction;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.impl.AIStreamKey;
import com.ibm.ws.sib.processor.impl.AnycastInputHandler;
import com.ibm.ws.sib.utils.ras.FormattedWriter;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 *
 */
public class AIProtocolItem extends SIMPItem implements Comparable
{
  /** the tick in the stream */
  private long _tick;
  /** the type of tick, currently only accepted */
  private byte _protocolState;
  /** the storage policy of the message */
  private int _storagePolicy;

  /** the reference to the AIH to issue callbacks on */
  private AnycastInputHandler _aihCallbackTarget;
  /** true if this item was unavailable after recovery */
  private boolean _unavailableAfterRecovery;

  // Standard debug/trace
  private static final TraceComponent tc =
        SibTr.register(
          AIProtocolItem.class,
          SIMPConstants.MP_TRACE_GROUP,
          SIMPConstants.RESOURCE_BUNDLE);

 
  /**
   * Persistent data version number.
   */
  private static final int PERSISTENT_VERSION = 1;

  public AIProtocolItem(long tick, byte protocolState, Reliability reliability, AnycastInputHandler aih)
  {
    super();

    if (tc.isEntryEnabled())
      SibTr.entry(tc, "AIProtocolItem",
        new Object[]{new Long(tick), new Byte(protocolState), reliability, aih});

    _tick = tick;
    _protocolState = protocolState;
    _storagePolicy = computeStoragePolicy(reliability);
    _aihCallbackTarget = aih;
    _unavailableAfterRecovery = false;

    if (tc.isEntryEnabled())
      SibTr.exit(tc, "AIProtocolItem", this);
  }

  public AIProtocolItem()
  {
    super();

    if (tc.isEntryEnabled())
      SibTr.entry(tc, "AIProtocolItem");

    _storagePolicy = AbstractItem.STORE_EVENTUALLY;  // place holder until real value is available
    _aihCallbackTarget = null;
    _unavailableAfterRecovery = false;

    if (tc.isEntryEnabled())
      SibTr.exit(tc, "AIProtocolItem", this);
  }

  public int compareTo(Object o)
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "compareTo", o);

     if (o instanceof AIProtocolItem)
    {
      AIProtocolItem other = (AIProtocolItem)o;
      int result;
      if (other._tick < _tick)
      {
        result = -1;
      }
      else if (other._tick > _tick)
      {
        result = 1;
      }
      else
      {
        result = 0;
      }

      if (tc.isEntryEnabled())
        SibTr.exit(tc, "compareTo");

       return result;
    }

    ClassCastException e = new ClassCastException();
    FFDCFilter.processException(
      e,
      "com.ibm.ws.sib.processor.impl.store.items.AIProtocolItem.compareTo",
      "1:156:1.27",
      this);

    SibTr.exception(tc, e);

    if (tc.isEntryEnabled())
      SibTr.exit(tc, "compareTo");

    throw e;
  }

  public int getStorageStrategy()
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "getStorageStrategy");

    if (tc.isEntryEnabled())
      SibTr.exit(tc, "getStorageStrategy", new Integer(_storagePolicy));

    return _storagePolicy;
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
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "getPersistentData", oos);

    try
    {
      HashMap hm = new HashMap();

      hm.put("tick", new Long(_tick));
      hm.put("protocolState", new Byte(_protocolState));
      hm.put("storagePolicy", new Integer(_storagePolicy));

      oos.writeObject(hm);
    }
    catch (IOException e)
    {
      // FFDC
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.impl.store.items.AIProtocolItem.getPersistentData",
        "1:216:1.27",
        this);

      SIErrorException e2 = new SIErrorException(e);
      SibTr.exception(tc, e2);

      if (tc.isEntryEnabled())
        SibTr.exit(tc, "getPersistentData", e2);

      throw e2;
    }

    if (tc.isEntryEnabled())
      SibTr.exit(tc, "getPersistentData");
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.store.items.SIMPItem#restore(java.io.ObjectInputStream, int)
   */
  public void restore(ObjectInputStream ois, int dataVersion)
  {
    if (tc.isEntryEnabled())
      SibTr.entry(
        tc, "restore", new Object[] { ois, new Integer(dataVersion) });

    checkPersistentVersionId(dataVersion);

    try
    {
      HashMap hm = (HashMap)ois.readObject();

      _tick = ((Long)hm.get("tick")).longValue();
      _protocolState = ((Byte)hm.get("protocolState")).byteValue();
      _storagePolicy = ((Integer)hm.get("storagePolicy")).intValue();
    }
    catch (Exception e)
    {
      // FFDC
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.impl.store.items.AIProtocolItem.restore",
        "1:257:1.27",
        this);

      SIErrorException e2 = new SIErrorException(e);
      SibTr.exception(tc, e2);

      if (tc.isEntryEnabled())
        SibTr.exit(tc, "restore", e2);

      throw e2;
    }

    if (tc.isEntryEnabled())
      SibTr.exit(tc, "restore");
  }

  public void eventPostCommitAdd(Transaction transaction)
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "eventPostCommitAdd", transaction);

    if (_aihCallbackTarget != null)
    {
      AIStreamKey key = new AIStreamKey(_tick);
      key.setAcceptedItem(this);

      _aihCallbackTarget.committed(key);

      // Reset any ordered transaction
      _aihCallbackTarget.clearOrderedTran();
    }

    if (tc.isEntryEnabled()) SibTr.exit(tc, "eventPostCommitAdd");
  }

  public void eventPostRollbackAdd(Transaction transaction)
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "eventPostRollbackAdd", transaction);

    if (_aihCallbackTarget != null && _unavailableAfterRecovery)
    {
      AIStreamKey key = new AIStreamKey(_tick);
      key.setAcceptedItem(this);

      _aihCallbackTarget.rolledback(key);
    }

    //  Reset any ordered transaction
    if (_aihCallbackTarget != null)
    {
      _aihCallbackTarget.clearOrderedTran();
    }

    if (tc.isEntryEnabled())
      SibTr.exit(tc, "eventPostRollbackAdd");
  }

  public long getTick()
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "getTick");

    if (tc.isEntryEnabled())
      SibTr.exit(tc, "getTick", new Long(_tick));

    return _tick;
  }

  public byte getProtocolState()
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "getProtocolState");

    if (tc.isEntryEnabled())
      SibTr.exit(tc, "getProtocolState", new Byte(_protocolState));

    return _protocolState;
  }

  public final long getStoragePolicy()
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "getStoragePolicy");

    if (tc.isEntryEnabled())
      SibTr.exit(tc, "getStoragePolicy", new Integer(_storagePolicy));

    return _storagePolicy;
  }

  public void setAIHCallbackTarget(AnycastInputHandler aih)
  {
    if (tc.isEntryEnabled()) SibTr.entry(tc, "setAIHCallbackTarget", aih);
    _aihCallbackTarget = aih;
    if (tc.isEntryEnabled()) SibTr.exit(tc, "setAIHCallbackTarget");
  }

  public boolean isUnavailableAfterRecovery()
  {
    if (tc.isEntryEnabled())
    {
      SibTr.entry(tc, "isUnavailableAfterRecovery");
      SibTr.exit(tc, "isUnavailableAfterRecovery", new Boolean(_unavailableAfterRecovery));
    }
    return _unavailableAfterRecovery;
  }

  public void setUnavailableAfterRecovery(boolean value)
  {
    if (tc.isEntryEnabled()) SibTr.entry(tc, "setUnavailableAfterRecovery", new Boolean(value));
    _unavailableAfterRecovery = value;
    if (tc.isEntryEnabled()) SibTr.exit(tc, "setUnavailableAfterRecovery");
  }

  private int computeStoragePolicy(Reliability reliability)
  {
    if (tc.isEntryEnabled()) SibTr.entry(tc, "computeStoragePolicy", reliability);
    int result = 0;
    if (reliability.compareTo(Reliability.ASSURED_PERSISTENT) == 0)
      result = AbstractItem.STORE_ALWAYS;
    else if (reliability.compareTo(Reliability.RELIABLE_PERSISTENT) == 0)
      result = AbstractItem.STORE_EVENTUALLY;
    else
      result = AbstractItem.STORE_NEVER;
    if (tc.isEntryEnabled()) SibTr.exit(tc, "computeStoragePolicy", new Integer(result));
    return result;
  }

  /**
   * Prints the Message Details to the xml output.
   */
  public void xmlWriteOn(FormattedWriter writer) throws IOException
  {
    writer.newLine();
    writer.taggedValue("tick", _tick);
    writer.taggedValue("protocolState", _protocolState);
  }

  public String toString()
  {
    return super.toString() + "[" + _tick + ", " + _protocolState + "]";
  }
}
