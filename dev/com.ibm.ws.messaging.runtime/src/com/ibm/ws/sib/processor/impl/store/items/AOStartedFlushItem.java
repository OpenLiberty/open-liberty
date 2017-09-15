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

// Import required classes.
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.IOException;

import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.ws.sib.msgstore.AbstractItem;
import com.ibm.ws.sib.msgstore.MessageStoreException;
import com.ibm.ws.sib.msgstore.SevereMessageStoreException;
import com.ibm.ws.sib.msgstore.transactions.Transaction;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.utils.SIBUuid12;
import com.ibm.ws.sib.utils.ras.FormattedWriter;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.ws.ffdc.FFDCFilter;

/**
 * The persistent ItemStream for each protocol stream, at the Anycast Output Handler.
 */
public final class AOStartedFlushItem extends SIMPItem
{
  private static final TraceComponent tc =
  SibTr.register(
    AOStartedFlushItem.class,
    SIMPConstants.MP_TRACE_GROUP,
    SIMPConstants.RESOURCE_BUNDLE);
  
  /**
   * Persistent data version number.
   */
  private static final int PERSISTENT_VERSION = 2;

  private String _streamKey;
  private SIBUuid12 _streamId;

  public AOStartedFlushItem()
  {
    super();
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "AOStartedFlushItem");

    _streamKey = null;
    _streamId = null;

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "AOStartedFlushItem", this);

  }

  public AOStartedFlushItem(String streamKey, SIBUuid12 streamId)
  {
    super();

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "AOStartedFlushItem", new Object[]{streamKey, streamId});

    this._streamKey = streamKey;
    this._streamId = streamId;

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "AOStartedFlushItem", this);
  }

  public final String getStreamKey()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
    {    
      SibTr.entry(tc, "getStreamTableKey");
      SibTr.exit(tc, "getStreamTableKey", _streamKey);
    }
    return _streamKey;
  }
  
  public final SIBUuid12 getStreamId()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getStreamId");
      SibTr.exit(tc, "getStreamId",_streamId);
    }
    return _streamId;
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
  public void getPersistentData(ObjectOutputStream dout)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getPersistentData", dout);

    try
    {
      dout.writeUTF(_streamKey);
      dout.writeUTF(_streamId.toString());
    }
    catch (IOException e)
    {
      // No FFDC code needed
      SIErrorException e2 = new SIErrorException(e);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "getPersistentData", e2);

      throw e2;
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getPersistentData");
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.store.items.SIMPItem#restore(java.io.ObjectInputStream, int)
   */
  public void restore(ObjectInputStream din, int dataVersion)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc, "restore", new Object[] { din, Integer.valueOf(dataVersion) });
    
    checkPersistentVersionId(dataVersion);

    try
    {
      if(dataVersion == 1)
        _streamKey = din.readUTF() + SIMPConstants.DEFAULT_CONSUMER_SET;
      else
        _streamKey = din.readUTF();
      _streamId = new SIBUuid12(din.readUTF());            
    }
    catch (Exception e)
    {
      // No FFDC code needed
      SIErrorException e2 = new SIErrorException(e);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "restore", e2);

      throw e2;
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "restore");
  }

  public long getMaximumTimeInStore()
  {
    return AbstractItem.NEVER_EXPIRES; // we don't want it to ever expire
  }
  public int getStorageStrategy()
  {
    return AbstractItem.STORE_ALWAYS;
  }

  public void eventPostRollbackRemove(final Transaction transaction) throws SevereMessageStoreException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "eventPostRollbackRemove", transaction);

    super.eventPostRollbackRemove(transaction);
    // now unlock the item.
    try
    {
      unlock(this.getLockID());
    }
    catch (MessageStoreException e)
    {
      // FFDC
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.impl.store.items.AOStartedFlushItem.eventPostRollbackRemove",
        "1:227:1.24",
        this);

      SibTr.exception(tc, e);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "eventPostRollbackRemove");
  }

  /**
   * Prints the Message Details to the xml output.
   */
  public void xmlWriteOn(FormattedWriter writer) throws IOException
  {
    writer.newLine();
    writer.taggedValue("streamId", _streamId);
    writer.taggedValue("streamKey", _streamKey);
  }

  public String toString()
  {
    return super.toString() + "[" + _streamId + ", " + _streamKey + "]"; 
  }
}
