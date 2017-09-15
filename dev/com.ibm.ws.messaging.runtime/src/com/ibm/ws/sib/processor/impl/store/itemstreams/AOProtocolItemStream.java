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

// Import required classes.
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.IOException;

import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.ws.sib.msgstore.MessageStoreException;
import com.ibm.ws.sib.msgstore.SevereMessageStoreException;
import com.ibm.ws.sib.msgstore.transactions.Transaction;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.utils.SIBUuid12;
import com.ibm.ws.sib.utils.SIBUuid8;
import com.ibm.ws.sib.utils.ras.FormattedWriter;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler;


/**
 * The persistent ItemStream for each protocol stream, at the Anycast Output Handler.
 */
public final class AOProtocolItemStream extends SIMPItemStream
{
  private static TraceComponent tc =
  SibTr.register(
    AOProtocolItemStream.class,
    SIMPConstants.MP_TRACE_GROUP,
    SIMPConstants.RESOURCE_BUNDLE);

 
  /**
   * Persistent data version number.
   */
  private static final int PERSISTENT_VERSION = 2;  
  private static final String NULL = "null"; 
  
  private SIBUuid8 remoteMEId;
  private SIBUuid12 gatheringTargetDestUuid;
  private SIBUuid12 streamId;
  private DestinationHandler destinationHandler;

  public AOProtocolItemStream()
  {
    super();
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "AOProtocolItemStream");

    gatheringTargetDestUuid = null;
    streamId = null;
    remoteMEId = null;

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "AOProtocolItemStream", this);

  }

  public AOProtocolItemStream(SIBUuid8 remoteMEId, SIBUuid12 gatheringTargetDestUuid, SIBUuid12 streamId, DestinationHandler destinationHandler)
  {
    super();
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "AOProtocolItemStream", new Object[]{remoteMEId, gatheringTargetDestUuid, streamId, destinationHandler});

    this.remoteMEId = remoteMEId;
    this.gatheringTargetDestUuid = gatheringTargetDestUuid;
    this.streamId = streamId;
    this.destinationHandler = destinationHandler;

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "AOProtocolItemStream", this);

  }

  public final SIBUuid12 getStreamId()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
    {    
      SibTr.entry(tc, "getStreamId");
      SibTr.exit(tc, "getStreamId", streamId);
    }
    return streamId;
  }
  
  public final SIBUuid12 getGatheringTargetDestUuid()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
    {    
      SibTr.entry(tc, "getGatheringTargetDestUuid");
      SibTr.exit(tc, "getGatheringTargetDestUuid", gatheringTargetDestUuid);
    }
    return gatheringTargetDestUuid;
  }

  public final SIBUuid8 getRemoteMEId()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getRemoteMEId");
      SibTr.exit(tc, "getRemoteMEId", remoteMEId);
    }
    return remoteMEId;
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
  public void getPersistentData(ObjectOutputStream dout)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getPersistentData", dout);

    try
    {
      dout.writeUTF(remoteMEId.toString()); 
      dout.writeUTF(streamId.toString());
      String id = NULL;
      if (gatheringTargetDestUuid!=null) 
        id = gatheringTargetDestUuid.toString();
      dout.writeUTF(id); 
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
   * @see com.ibm.ws.sib.processor.impl.store.itemstreams.SIMPItemStream#restore(java.io.ObjectInputStream, int)
   */
  public void restore(ObjectInputStream din, int dataVersion)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc, "restore", new Object[] { din, Integer.valueOf(dataVersion) });
    
    checkPersistentVersionId(dataVersion);

    try
    {
      remoteMEId = new SIBUuid8(din.readUTF());
      streamId = new SIBUuid12(din.readUTF()); 
      if (dataVersion > 1)
      {
        String id = din.readUTF();
        if (!id.equals(NULL))
          gatheringTargetDestUuid = new SIBUuid12(id);
      }
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
  
  /**
   * Prints the Message Details to the xml output.
   */
  public void xmlWriteOn(FormattedWriter writer) throws IOException
  {
      writer.newLine();
      writer.taggedValue("remoteMEId", remoteMEId);
      writer.newLine();
      writer.taggedValue("streamId", streamId);
      writer.newLine();
      writer.taggedValue("gatheringTargetDestUuid", gatheringTargetDestUuid);
  }

  public void eventPostRollbackRemove(Transaction transaction) throws SevereMessageStoreException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "eventPostRollbackRemove", transaction);

    super.eventPostRollbackRemove(transaction);
    // now unlock the item.
    try
    {
      this.unlock(this.getLockID());
    }
    catch (MessageStoreException e)
    {
      // FFDC
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.impl.store.itemstreams.AOProtocolItemStream.eventPostRollbackRemove",
        "1:255:1.25",
        this);

      SibTr.exception(tc, e);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "eventPostRollbackRemove");
  }

  public DestinationHandler getDestinationHandler()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "getDestinationHandler");
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "getDestinationHandler", destinationHandler);
    return destinationHandler;
  }

  @Override
  protected void checkPersistentVersionId(int dataVersion) {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "checkPersistentVersionId", new Integer(dataVersion));
      
    // We are backwards compatible
    if (dataVersion > getPersistentVersion())
    {
      super.checkPersistentVersionId(dataVersion);
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "checkPersistentVersionId");
  }
}
