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
package com.ibm.ws.sib.processor.impl.store.itemstreams;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;

import com.ibm.websphere.ras.TraceComponent;

import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.impl.store.items.SIMPItem;
import com.ibm.ws.sib.transactions.PersistentTranId;
import com.ibm.ws.sib.utils.SIBUuid12;
import com.ibm.ws.sib.utils.ras.FormattedWriter;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 *
 */
public class AIProtocolItemStream extends SIMPItemStream
{
  // Standard debug/trace
  private static final TraceComponent tc =
  SibTr.register(
    AIProtocolItemStream.class,
    SIMPConstants.MP_TRACE_GROUP,
    SIMPConstants.RESOURCE_BUNDLE);
 
  /**
   * Persistent data version number.
   */
  private static final int PERSISTENT_VERSION = 1;  

  private SIBUuid12 streamId;
  private boolean flushStarted;
  
  /** the current tran for any indoubt msgs */
  private PersistentTranId currentTranId;
  /** set if more than one tran is set for indoubt msgs */
  private boolean unableToOrder;

  public AIProtocolItemStream()
  {
    super();

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "AIProtocolItemStream");

    this.streamId = null;
    this.flushStarted = false;

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "AIProtocolItemStream", this);
  }

  public AIProtocolItemStream(SIBUuid12 streamId)
  {
    super();

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "AIProtocolItemStream", streamId);

    this.streamId = streamId;
    this.flushStarted = false;

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "AIProtocolItemStream", this);
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
   * @see com.ibm.ws.sib.processor.impl.store.itemstreams.SIMPItemStream#getPersistentData(java.io.ObjectOutputStream)
   */
  public void getPersistentData(ObjectOutputStream oos)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getPersistentData", oos);

    try
    {
      HashMap<String, Object> hm = new HashMap<String, Object>();

      hm.put("streamId", streamId.toByteArray());
      hm.put("flushStarted", new Boolean(flushStarted));
      
      oos.writeObject(hm);
    }
    catch (IOException e)
    {
      // FFDC
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.impl.store.itemstreams.AIProtocolItemStream.getPersistentData",
        "1:146:1.26",
        this);
        
      SIErrorException e2 = new SIErrorException(e);
      SibTr.exception(tc, e2);
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "getPersistentData", e2);
      throw e2;
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getPersistentData");
  }

  public void restore(ObjectInputStream ois, int dataVersion)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc, "restore", new Object[] { ois, new Integer(dataVersion) });
    
    checkPersistentVersionId(dataVersion);

    try
    {
      HashMap hm = (HashMap)ois.readObject();
      
      streamId = new SIBUuid12((byte[])hm.get("streamId"));
      flushStarted = ((Boolean)hm.get("flushStarted")).booleanValue();
    }
    catch (Exception e)
    {
      // FFDC
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.impl.store.itemstreams.AIProtocolItemStream.restore",
        "1:180:1.26",
        this);
        
      SIErrorException e2 = new SIErrorException(e);
      SibTr.exception(tc, e2);
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "restore", e2);
      
      throw e2;
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "restore");
  }

  /**
   * Prints the Message Details to the xml output.
   */
  public void xmlWriteOn(FormattedWriter writer) throws IOException
  {
    writer.newLine();
    writer.taggedValue("streamId", streamId);
    writer.newLine();
    writer.taggedValue("flushStarted", flushStarted);
  }

  public SIBUuid12 getStreamId()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getStreamId");

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getStreamId", streamId);

    return streamId;
  }
  
  public boolean isFlushStarted()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "isFlushStarted");

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "isFlushStarted", Boolean.valueOf(flushStarted));

    return flushStarted;
  }
  
  public void setFlushStarted(boolean value)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "setFlushStarted",Boolean.valueOf(value));

    flushStarted = value;

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "setFlushStarted");
  }
  
  /**
   * @param id
   */
  public PersistentTranId getOrderedActiveTran() 
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getOrderedActiveTran");
      SibTr.exit(tc, "getOrderedActiveTran", currentTranId);
    }
    return currentTranId;
  }
  
  /**
   * @param id
   */
  public boolean isUnableToOrder() 
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "isUnableToOrder");
      SibTr.exit(tc, "isUnableToOrder", Boolean.valueOf(unableToOrder));
    }
    return unableToOrder;
  }
  
  /**
   * @param id
   */
  public void setCurrentTransaction(SIMPItem msg, boolean isInDoubtOnRemoteConsumer) 
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "setCurrentTransaction", new Object[] {msg, Boolean.valueOf(isInDoubtOnRemoteConsumer)});
    
    if (currentTranId != null && !currentTranId.equals(msg.getTransactionId()))
    {
      unableToOrder = true;
      
      // PK69943 Do not output a CWSIP0671 message here, as we do not know if
      // the destination is ordered or not. Leave that to the code that later checks unableToOrder
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
        SibTr.debug(tc, "Unable to order. Transaction: " + msg.getTransactionId() + " Current:" + currentTranId);
      }

    }
    else
      currentTranId = msg.getTransactionId();
        
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "setCurrentTransaction");
  }

  /**
   * 
   */
  public void clearOrderedTran() { 
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "clearOrderedTran");
      SibTr.exit(tc, "clearOrderedTran");
    }   
    currentTranId = null;    
  }
}
