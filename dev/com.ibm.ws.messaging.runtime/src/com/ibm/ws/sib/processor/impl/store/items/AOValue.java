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
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.msgstore.AbstractItem;
import com.ibm.ws.sib.msgstore.ItemStream;
import com.ibm.ws.sib.msgstore.MessageStoreException;
import com.ibm.ws.sib.msgstore.SevereMessageStoreException;
import com.ibm.ws.sib.msgstore.transactions.Transaction;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.impl.interfaces.ControllableResource;
import com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler;
import com.ibm.ws.sib.processor.impl.interfaces.SIMPMessage;
import com.ibm.ws.sib.processor.impl.store.itemstreams.AOProtocolItemStream;
import com.ibm.ws.sib.processor.runtime.impl.ControlAdapter;
import com.ibm.ws.sib.processor.runtime.impl.QueuedMessage;
import com.ibm.ws.sib.processor.utils.am.BatchedTimeoutEntry;
import com.ibm.ws.sib.processor.utils.am.BatchedTimeoutManager;
import com.ibm.ws.sib.utils.SIBUuid8;
import com.ibm.ws.sib.utils.ras.FormattedWriter;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * The information kept for each value tick in a stream
 */
public final class AOValue extends SIMPItem implements BatchedTimeoutEntry, ControllableResource
{
  private static final TraceComponent tc =
  SibTr.register(
    AOValue.class,
    SIMPConstants.MP_TRACE_GROUP,
    SIMPConstants.RESOURCE_BUNDLE);
    
 

  /**
   * Persistent data version number.
   */
  private static final int PERSISTENT_VERSION = 2;

  /** the tick in the stream */
  private long tick;
  /** the id of the message in the MessageStore */
  private long msgId;
  /** the storage policy of the message */
  private int storagePolicy;
  /** the id of the persistent lock on the message. Only when the storage policy is STORE_ALWAYS or STORE_EVENTUALLY */
  private long plockId;

  /** the wait time for the request */
  private long waitTime;
  /** the previous tick of the same priority and reliability */
  private long prevTick;
  
  /**
   * SIB0113a - Gathering consumers
   * When a remote gather occurs (i.e. a gathering consumer is remote to the cluster), the requested AOValue
   * could refer to a message which is local to this ME, or could be from a remote ME. We therefore need to store
   * the source meuuid of the message so we can pick the correct consumerDispatcher when the message is needed later.
   * We also need to persist the original msg's reliability and priority in order to reconstitute the in-memory state
   * for this AOValue (normally we have these in the referenced message - but this wont exist).
   */
  private SIBUuid8 sourceMEUuid;
  private int reliability = -1;
  private int priority = -1;

  /** the next field is for implementing the BatchedTimeoutEntry interface */
  private BatchedTimeoutManager.LinkedListEntry entry;

  private ControlAdapter controlAdapter;

  private long aiRequestTick;

  /**
   * SIB0113
   * This indicates if the AOValue is set to be removed due to a flush.
   * This is used if we are an IME and are restoring a msg that this AOValue targets during message gathering.
   * If the AOValue is flushed before the target msg is restored - We do not lock the msg to this AOValue.
   * The protocols should ensure that the incoming target msg will either be assigned to another consumer
   * or will expire and will be unlocked on the DME.
   * 
   */
  private boolean isFlushing;
  
  /** true if in the process of removing the persistent information for this value tick, else false
   * initial state: false
   * final state: true
   * possible state transitions: false -> true
   */
  public boolean removing;

  public boolean restored = true;
  
  // This is the number of times the message was unlocked on the RME before it expired
  // and was rejected. If it never got rejected this value will never be set.
  public long rmeUnlockCount;
  
  /**
   * Constructor
   * @param tick
   * @param msgId The id of the message in the Message Store
   * @param storagePolicy The storage policy of the message and this item
   * @param plockId The persistent lock on the message, if any
   * @param waitTime The time that the request has waited at this ME before being satisfied
   * @param prevTick The previous tick of the same priority and reliability
   */
  public AOValue(long tick, SIMPMessage msg, long msgId, int storagePolicy, long plockId, long waitTime, long prevTick)
  {
    super();

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "AOValue", new Object[]{Long.valueOf(tick), msg, Long.valueOf(msgId), Integer.valueOf(storagePolicy), Long.valueOf(plockId),
          Long.valueOf(waitTime), Long.valueOf(prevTick)});

    this.tick = tick;
    this.msgId = msgId;
    this.reliability = msg.getMessage().getReliability().getIndex();
    this.priority = msg.getMessage().getPriority().intValue();
    this.sourceMEUuid = msg.getLocalisingMEUuid();
    this.aiRequestTick = msg.getMessage().getGuaranteedRemoteGetValueTick();
    this.storagePolicy = storagePolicy;
    this.plockId = plockId;
    this.waitTime = waitTime;
    this.prevTick = prevTick;
    this.removing = false;
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "AOValue", this);
  }

  /**
   * Empty Constructor. Used only by Message Store when this Item is restored from persistent storage
   */
  public AOValue()
  {
    super();

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "AOValue");
      SibTr.exit(tc, "AOValue", this);
    }
    
    removing = false;
  }

  public final long getTick() { return tick; }
  public final long getMsgId() { return msgId; }
  public final long getPLockId() { return plockId; }
  public final long getWaitTime() { return waitTime; }
  public final long getPrevTick() { return prevTick; }

  public BatchedTimeoutManager.LinkedListEntry getEntry()
  {
    return entry;
  }

  public void setEntry(BatchedTimeoutManager.LinkedListEntry entry)
  {
    this.entry = entry;
  }
  
  public void cancel()
  {
	  // NO-OP, just for the BatchedTimeoutEntry interface
  }


  // overriding some methods in AbstractItem. These will be called by the MessageStore only for
  // AOValue objects that are added to an ItemStream. We only add persistent AOValue objects to the
  // ItemStream, which represent protocol information for ASSURED messages that have been persistently
  // locked.
  
  public long getMaximumTimeInStore()
  {
    return AbstractItem.NEVER_EXPIRES; // we don't want it to ever expire
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
      dout.writeLong(tick);
      dout.writeLong(msgId);
      dout.writeInt(storagePolicy);
      dout.writeLong(plockId);
      dout.writeLong(prevTick);
      dout.writeLong(waitTime);
      dout.writeInt(priority);
      dout.writeInt(reliability);
      dout.writeLong(aiRequestTick);
      dout.writeUTF(sourceMEUuid.toString());
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

  public int getStorageStrategy() 
  {
    return storagePolicy;
  }
  
  public void restore(ObjectInputStream din, int dataVersion)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc, "restore", new Object[] { din, Integer.valueOf(dataVersion) });
    
    checkPersistentVersionId(dataVersion);

    try
    {
      tick = din.readLong();
      msgId = din.readLong();
      storagePolicy = din.readInt();
      plockId = din.readLong();
      prevTick = din.readLong();
      waitTime = din.readLong();
      if (dataVersion >= PERSISTENT_VERSION)
      {
        priority = din.readInt();
        reliability = din.readInt();
        aiRequestTick = din.readLong();
        sourceMEUuid = new SIBUuid8(din.readUTF());
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

  public void eventPostRollbackRemove(final Transaction transaction)  throws SevereMessageStoreException
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
      // this should not occur. FFDC
      // MessageStoreException shouldn't occur so FFDC.
      FFDCFilter.processException(e,"com.ibm.ws.sib.processor.impl.store.items.AOValue.eventPostRollbackRemove",
          "1:337:1.28.1.5",this);

      SibTr.exception(tc, e);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "eventPostRollbackRemove");
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.ControllableResource#createControlAdapter()
   */
  public void createControlAdapter()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "createControlAdapter");
    
    DestinationHandler dh = null;
    try
    {
      ItemStream is = getItemStream();
      
      // TODO - This method is using the wrong itemstream
  
      dh = ((AOProtocolItemStream) is).getDestinationHandler();
      SIMPMessage msg = (SIMPMessage) is.findById(msgId);
      controlAdapter = new QueuedMessage(msg,dh,is);
    }
    catch(Exception e)
    {
      // FFDC
      FFDCFilter.processException(
          e,
          "com.ibm.ws.sib.processor.impl.store.items.AOValue.createControlAdapter",
          "1:371:1.28.1.5",
          this);  
                  
      SibTr.exception(tc, e); 
    }
         
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "createControlAdapter");
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.ControllableResource#dereferenceControlAdapter()
   */
  public void dereferenceControlAdapter()
  {

  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.ControllableResource#deregisterControlAdapterMBean()
   */
  public void deregisterControlAdapterMBean()
  {

  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.ControllableResource#getControlAdapter()
   */
  public ControlAdapter getControlAdapter()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getControlAdapter");
      
    if (controlAdapter == null)
    {
      createControlAdapter();
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getControlAdapter", controlAdapter);
    return controlAdapter;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.ControllableResource#registerControlAdapterAsMBean()
   */
  public void registerControlAdapterAsMBean()
  {

  }

  /**
   * Returns the source meUuid this message is being requested from. (Usually the local ME unless
   * gathering is taking place)
   * 
   * Returns null if pre-WAS70 data
   * @return
   */
  public SIBUuid8 getSourceMEUuid() 
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getSourceMEUuid"); 
      SibTr.exit(tc, "getSourceMEUuid", sourceMEUuid);
    }
    return sourceMEUuid;
  }

  /*
   * SIB0113 Only set for IME remote gathering (Must not be called in other circumstances since the value
   * will not be persisted)
   */
  public void setMsgId(long msgId) 
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "setMsgId", Long.valueOf(msgId)); 

    // No need to harden this. We only call this as part of restoring an AOValue->AIMessageItem link
    // for an IME (remote gathering). If the ME goes down we dont need the msgId anyway.
    this.msgId = msgId;
    restored = true;
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "setMsgId"); 
  }
  
  public void setPLockId(long lockID) 
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "setPLockId", Long.valueOf(lockID)); 

    // No need to harden this. We only call this as part of restoring an AOValue->AIMessageItem link
    // for an IME (remote gathering). If the ME goes down we dont need the lockId anyway.
    this.plockId = lockID;
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "setPLockId"); 
  }

  public long getAIRequestTick()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getAIRequestTick"); 
      SibTr.exit(tc, "getAIRequestTick", Long.valueOf(aiRequestTick));
    }
    return aiRequestTick;  
  }

  public int getMsgPriority() 
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getMsgPriority"); 
      SibTr.exit(tc, "getMsgPriority", Integer.valueOf(priority));
    }
    return priority;
  }

  public int getMsgReliability()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getMsgReliability"); 
      SibTr.exit(tc, "getMsgReliability", Integer.valueOf(reliability));
    }
    return reliability;
  }
  
  public void setToBeFlushed()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "setToBeFlushed"); 
      SibTr.exit(tc, "setToBeFlushed");
    }
    isFlushing = true;    
  }
  
  public boolean isFlushing()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "isFlushing"); 
      SibTr.exit(tc, "isFlushing", Boolean.valueOf(isFlushing));
    }
    return isFlushing;
  }
  
  /**
   * Prints the Message Details to the xml output.
   */
  public void xmlWriteOn(FormattedWriter writer) throws IOException
  {
    writer.newLine();
    writer.taggedValue("referencedMsg", msgId);
    writer.newLine();
    writer.taggedValue("AOTicks", tick+","+prevTick);
    writer.newLine();
    writer.taggedValue("gatheredMEUuid", sourceMEUuid);
  }

  public String toString()
  {
    return super.toString()+"["+msgId+", "+sourceMEUuid+", "+tick+", "+prevTick+"]";
  }

  public boolean isRestored() 
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "isRestored"); 
      SibTr.exit(tc, "isRestored", Boolean.valueOf(restored));
    }
    return restored;
  }



}
