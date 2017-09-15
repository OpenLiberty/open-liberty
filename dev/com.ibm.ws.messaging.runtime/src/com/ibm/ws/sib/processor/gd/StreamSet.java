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
package com.ibm.ws.sib.processor.gd;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.sib.Reliability;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIIncorrectCallException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.msgstore.MessageStoreException;
import com.ibm.ws.sib.msgstore.SevereMessageStoreException;
import com.ibm.ws.sib.msgstore.transactions.Transaction;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.impl.interfaces.ControllableResource;
import com.ibm.ws.sib.processor.impl.store.SIMPTransactionManager;
import com.ibm.ws.sib.processor.impl.store.items.SIMPItem;
import com.ibm.ws.sib.processor.impl.store.itemstreams.ProtocolItemStream;
import com.ibm.ws.sib.processor.runtime.impl.ControlAdapter;
import com.ibm.ws.sib.processor.runtime.impl.InternalOutputStreamSetControl;
import com.ibm.ws.sib.processor.runtime.impl.LinkPublicationPointControl;
import com.ibm.ws.sib.processor.runtime.impl.LinkReceiverControl;
import com.ibm.ws.sib.processor.runtime.impl.LinkTransmitterControl;
import com.ibm.ws.sib.processor.runtime.impl.SourceStreamSetControl;
import com.ibm.ws.sib.processor.runtime.impl.TargetStreamSetControl;
import com.ibm.ws.sib.transactions.LocalTransaction;
import com.ibm.ws.sib.transactions.TransactionCommon;
import com.ibm.ws.sib.trm.topology.MessagingEngine;
import com.ibm.ws.sib.utils.SIBUuid12;
import com.ibm.ws.sib.utils.SIBUuid8;
import com.ibm.ws.sib.utils.ras.FormattedWriter;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.exception.SIConnectionLostException;
import com.ibm.wsspi.sib.core.exception.SIRollbackException;

/**
 * @author prmf
 * 
 * A StreamSet contains an array of ReliabilitySubsets, one per reliability.
 * Each subset contains an array of Streams, one per priority.
 * Each subset can be updated independently of the others but the StreamSet
 * parent keeps a record of their msg store IDs so that it can automatically
 * restore them when needed.
 * 
 * Note that currently access is unsynchronized
 */
public class StreamSet extends SIMPItem implements ControllableResource
{
  public static class Type
  {
    public static final Type SOURCE = new Type(0,"SOURCE");
    public static final Type TARGET = new Type(1,"TARGET");
    public static final Type INTERNAL_OUTPUT = new Type(2,"INTERNAL_OUTPUT");
    public static final Type INTERNAL_INPUT = new Type(3,"INTERNAL_INPUT");
    public static final Type LINK_SOURCE = new Type(4,"LINK_SOURCE");
    public static final Type LINK_TARGET = new Type(5,"LINK_TARGET");
    public static final Type LINK_INTERNAL_OUTPUT = new Type(6,"LINK_INTERNAL_OUTPUT");
    public static final Type LINK_REMOTE_SOURCE = new Type(7,"LINK_REMOTE_SOURCE");
    
    private final int value;
    private final String name;
    
    private static final Type[] set =
      new Type[]{ SOURCE,
                  TARGET,
                  INTERNAL_OUTPUT,
                  INTERNAL_INPUT,
                  LINK_SOURCE,
                  LINK_TARGET,
                  LINK_INTERNAL_OUTPUT,
                  LINK_REMOTE_SOURCE
                  };
    
    private Type(int value, String name)
    {
      this.value = value;
      this.name = name;
    }
    
    public int toInt()
    {
      return value;
    }
    
    public String toString()
    {
      return name;
    }
    
    public static Type getType(int value)
    {
        return set[value];
    }    
  }


  private Type type;
  
  private String linkTarget;

  private ControlAdapter controlAdapter = null;

  private long initialData = -1;

  private boolean persistent = false;

  private static final int maxReliabilityIndex = Reliability.MAX_INDEX;

  private SIMPTransactionManager txManager = null;

  private ProtocolItemStream itemStream = null;

  //NLS for component
  private static final TraceNLS nls =
    TraceNLS.getTraceNLS(SIMPConstants.RESOURCE_BUNDLE);

  private static final int PERSISTENT_VERSION = 2;

  private SIBUuid8  remoteMEUuid  = null;
  private SIBUuid12 streamID = null;
  private SIBUuid12 destID   = null;
  private SIBUuid8  busID    = null;

  private static TraceComponent tc =
    SibTr.register(
      StreamSet.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);


  private long[] subsetIDs;
  private ReliabilitySubset[] subsets;

  /**
   * Warm start constructor invoked by the Message Store.
   * Empty constructor to allow recreation by the message store after it is
   * persisted.
   */
  public StreamSet()
  {
    super();

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "StreamSet");
    
    // Don't initialise until we've restored our persistent data

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "StreamSet", this);
  }

  /**
   * Cold start constructor for a non-persistent StreamSet
   * 
   * @param streamID
   */
  public StreamSet(SIBUuid12 streamID, long initialData, Type type)
  {
    this(streamID, null, initialData, type);
  }
  
  /**
   * Cold start constructor for a non-persistent StreamSet
   * 
   * @param streamID
   * @param cellule
   */
  public StreamSet(SIBUuid12 streamID,
                   SIBUuid8 remoteMEUuid,
                   long initialData,
                   Type type)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(this, tc, "StreamSet", 
        new Object[]{streamID, remoteMEUuid, Long.valueOf(initialData), type});
    
    this.streamID = streamID;
    this.remoteMEUuid = remoteMEUuid;
    this.initialData = initialData;
    this.type = type;
    
    initialize();    
    
    createNonPersistentSubsets();
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "StreamSet", this);
  }
      
  /**
   * Cold start constructor for a persistent StreamSet
   * 
   * @param streamID
   * @param cellule
   * @throws SIResourceException
   */
  public StreamSet(SIBUuid12 streamID,
                   SIBUuid8 remoteMEUuid,
                   SIBUuid12 destID,
                   SIBUuid8  busID,
                   ProtocolItemStream itemStream,
                   SIMPTransactionManager txManager,
                   long initialData,
                   Type type,
                   TransactionCommon tran,
                   String linkTarget) throws SIResourceException
  {
    super();  
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(this, tc, "StreamSet", 
        new Object[]{streamID, remoteMEUuid, destID, busID, itemStream, txManager, Long.valueOf(initialData), type, tran, linkTarget});
    
    this.streamID = streamID;
    this.remoteMEUuid = remoteMEUuid;
    this.destID = destID;
    this.busID = busID;
    this.itemStream = itemStream;
    this.txManager = txManager;
    this.initialData = initialData;
    this.type = type;
    this.linkTarget = linkTarget;
    
    initialize();    
    
    persistent = true;
    createPersistentSubsets(tran);
   
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "StreamSet", this);
  }
  
  /**
   * Initialize some arrays
   */
  private void initialize()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(this, tc, "initialize");
        
    subsets = new ReliabilitySubset[maxReliabilityIndex + 1];
    subsetIDs = new long[maxReliabilityIndex + 1];
    for(int i=0;i<subsetIDs.length;i++)
    {
      subsetIDs[i] = NO_ID;             
    }
    
    createControlAdapter(); 
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "initialize");
  }

  public Type getType()
  {
	Type localType = type;
	
	// LINK_REMOTE_SOURCE is only for internal use, and is equivalent to a LINK_SOURCE
	// for anyone external (i.e. Admin)
	if(localType == Type.LINK_REMOTE_SOURCE)
		localType = Type.LINK_SOURCE;
	
    return localType;
  }

  /**
   * Set an element in the stream array
   * 
   * @param priority
   * @param reliability
   * @param stream
   * @throws SIResourceException
   */
  protected void setStream(
    int priority,
    Reliability reliability,
    Stream stream) throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "setStream",
        new Object[] { new Integer(priority), reliability, stream });
        
    ReliabilitySubset subset = getSubset(reliability);    
    subset.setStream(priority, stream);
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "setStream");
  }

  /**
   * This method should only be called when the streamSet was created with 
   * an unknown targetCellule and WLM has now told us correct targetCellule.
   * This can only happen when the SourceStreamManager is owned by a 
   * PtoPOuptuHandler within a LinkHandler  
   */
  public void updateCellule( SIBUuid8 newRemoteMEUuid)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(this, tc, "updateCellule", newRemoteMEUuid);
     
    this.remoteMEUuid = newRemoteMEUuid;

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "updateCellule");
  }

  /**
   * Get the array index for a given reliability
   * 
   * @param reliability
   */
  private int getIndex(Reliability reliability)
  {
    return reliability.getIndex();
  }
  
  /**
   * Get the reliability for a given array index
   * 
   * @param index
   */
  private Reliability getReliability(int index)
  {
    return Reliability.getReliabilityByIndex(index);
  }

  /**
   * Create all of the ReliabilitySubsets presistently if they are not express.
   * @throws SIResourceException
   * 
   * @throws SIStoreException
   */
  private void createPersistentSubsets(TransactionCommon tran) throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(this, tc, "createPersistentSubsets", tran);
          
    for(int i=0; i<subsets.length; i++)
    {
      createPersistentSubset(getReliability(i),tran);
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "createPersistentSubsets");
  }

  /**
   * Create a new ReliabilitySubset for the given reliability. If it is not express,
   * store it in the message store and record the message store ID.
   * 
   * @param reliability
   * @throws SIResourceException
   * @throws SIStoreException
   */
  private ReliabilitySubset createPersistentSubset(Reliability reliability,
      TransactionCommon tran) throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(this, tc, "createPersistentSubset", new Object[] { reliability });
    
    ReliabilitySubset subset = new ReliabilitySubset(reliability, initialData);
    subsets[getIndex(reliability)] = subset;
    if(reliability.compareTo(Reliability.BEST_EFFORT_NONPERSISTENT) > 0)
    {
      try
      {
        Transaction msTran = txManager.resolveAndEnlistMsgStoreTransaction(tran);
        itemStream.addItem(subset, msTran);
        subsetIDs[getIndex(reliability)] = subset.getID();
      }
      catch (MessageStoreException e)
      {        
        // FFDC
        FFDCFilter.processException(
          e,
          "com.ibm.ws.sib.processor.gd.StreamSet.createPersistentSubset",
          "1:398:1.67",
          this);
          
        //not sure if this default is needed but better safe than sorry
        subsetIDs[getIndex(reliability)] = NO_ID;
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
          SibTr.exception(tc, e);
          SibTr.exit(tc, "createPersistentSubset", e);
        }
      }
    }
    else
    {
      subsetIDs[getIndex(reliability)] = NO_ID;    
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "createPersistentSubset", subset);
    return subset;
  }

  /**
   * Create all the ReliabilitySubsets non persistently.
   */
  private void createNonPersistentSubsets()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(this, tc, "createNonPersistentSubsets");
    
    for(int i=0; i<subsets.length; i++)
    {
      createNonPersistentSubset(getReliability(i));
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "createNonPersistentSubsets");
  }

  /**
   * Create a new ReliabilitySubset for the given Reliability but do not
   * persist it.
   * 
   * @param reliability
   */
  private ReliabilitySubset createNonPersistentSubset(Reliability reliability)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "createNonPersistentSubset",
        new Object[] { reliability });
    
    ReliabilitySubset subset = new ReliabilitySubset(reliability, initialData);
    subsets[getIndex(reliability)] = subset;
    subsetIDs[getIndex(reliability)] = NO_ID;
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "createNonPersistentSubset", subset);
    
    return subset;    
  }

  /**
   * Get a specific stream based on priority and reliability
   * 
   * @param priority
   * @param reliability
   * @throws SIResourceException
   */
  public Stream getStream(int priority, Reliability reliability) throws SIResourceException
  {
    return getSubset(reliability).getStream(priority);
  }
    
  private ReliabilitySubset getSubset(Reliability reliability) throws SIResourceException
  { 
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(this, tc, "getSubset", reliability); 
      
    ReliabilitySubset subset = subsets[getIndex(reliability)];
    if(subset == null)
    {
      if(persistent)
      {
        subset = createPersistentSubset(reliability,
                                        txManager.createAutoCommitTransaction());
      }
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getSubset"); 
    return subset;
  }
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.store.items.SIMPItem#getPersistentData(java.io.ObjectOutputStream)
   */
  public void getPersistentData(ObjectOutputStream dos)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(this, tc, "getPersistentData", dos);

    try
    {      
      MessagingEngine cellule = new MessagingEngine(remoteMEUuid);
      byte[] celluleBytes = cellule.getBytes();
      int celluleBytesLen = celluleBytes.length;
      byte[] streamBytes  = streamID.toByteArray(); 
      int streamBytesLen  = streamBytes.length;   
      byte[] destBytes    = destID.toByteArray();  
      int destBytesLen    = destBytes.length;
      byte[] busBytes     = busID.toByteArray();  
      int busBytesLen     = busBytes.length;
       
      byte[] copy = new byte[celluleBytesLen
                             + streamBytesLen
                             + destBytesLen
                             + busBytesLen];
      
      System.arraycopy(celluleBytes, 0, copy, 0, celluleBytesLen);
      System.arraycopy(streamBytes,  0, copy, celluleBytesLen, streamBytesLen);
      System.arraycopy(destBytes, 0, copy, 
                       celluleBytesLen + streamBytesLen,
                       destBytesLen);
      System.arraycopy(busBytes, 0, copy, 
                       celluleBytesLen + streamBytesLen + destBytesLen, 
                       busBytesLen);
                    
      dos.write(copy);
      dos.writeLong(initialData);
      dos.writeInt(type.toInt());
      
      //just to be safe write out the number of subsets we have
      dos.writeInt(subsets.length);
      //then each of the subset ids and their reliabilities
      for(int i=0;i<subsets.length;i++)
      {
        dos.writeLong(subsetIDs[i]);
        dos.writeInt(subsets[i].getReliability().toInt()); 
      }
      
      //SIB0105.mp.6
      dos.writeObject(linkTarget);
    }
    catch (java.io.IOException e)
    {
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.gd.StreamSet.getPersistentData",
        "1:545:1.67",
        this);

      SibTr.exception(tc, e);
      SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
        new Object[] {
          "com.ibm.ws.sib.processor.gd.StreamSet",
          "1:552:1.67",
          e });
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "getPersistentData", "SIErrorException");
      
      throw new SIErrorException(
        nls.getFormattedMessage(
          "INTERNAL_MESSAGING_ERROR_CWSIP0002",
          new Object[] {
            "com.ibm.ws.sib.processor.gd.StreamSet",
            "1:562:1.67",
            e },
          null),
        e);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getPersistentData");
  }
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.store.items.SIMPItem#restore(java.io.ObjectInputStream, int)
   */
  public void restore(ObjectInputStream dis, int dataVersion)
  throws SevereMessageStoreException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc, "restore", new Object[] { dis, new Integer(dataVersion) });

    itemStream = (ProtocolItemStream) getItemStream();
    
    persistent = true;    

    try
    {      
      //read back the cellule
      byte[] celluleBytes = new byte[9];
      dis.read(celluleBytes, 0, 9);
      remoteMEUuid = new MessagingEngine(celluleBytes).getUuid();
      
      //read back the streamID
      byte[] streamIDBytes = new byte[12];
      dis.read(streamIDBytes, 0, 12);
      streamID = new SIBUuid12(streamIDBytes);
      
      //read back the destID
      byte[] destIDBytes = new byte[12];
      dis.read(destIDBytes, 0, 12);
      destID = new SIBUuid12(destIDBytes);
      
      //read back the busID
      byte[] busIDBytes = new byte[8];
      dis.read(busIDBytes, 0, 8);
      busID = new SIBUuid8(busIDBytes);
            
      initialData = dis.readLong();
      type = Type.getType(dis.readInt());
      
      //read back the number of subset IDs we should expect
      int numberOfSubsets = dis.readInt();
      
      // Now we've restored our persistent data, we can initialise the set.
      initialize();
      
      ReliabilitySubset[] newSubsets = new ReliabilitySubset[numberOfSubsets];
      long[] newSubsetIDs = new long[numberOfSubsets];
      
      //now go find all of the stored subsets via their IDs
      for(int i=0;i<numberOfSubsets;i++)
      {
        //get the next ID
        long subsetID = dis.readLong();
        //get the next reliability
        Reliability reliability = Reliability.getReliability(dis.readInt());
        //if the subset was properly stored, we will have a valid ID
        if(subsetID != NO_ID)
        {
          //find the subset via it's ID
          ReliabilitySubset subset = (ReliabilitySubset) getItemStream().findById(subsetID);
          if(subset != null)
          {
            //set the reliability
            subset.setReliability(reliability);
            //store the subset in a temporary array
            newSubsets[getIndex(reliability)] = subset;
          }
          else
          {
            //if we didn't find it, set the ID to NO_ID so that it will get recreated below.
            subsetID = NO_ID;
          }
        }        
        newSubsetIDs[i] = subsetID;
      }
      
      int j = 0;
      while(j<numberOfSubsets && newSubsetIDs[j] == NO_ID)
      {
        j++;
      }

      //loop through the current array of subsets
      for(int i=0;i<maxReliabilityIndex+1;i++)
      {
        //get the next restored subset
        ReliabilitySubset subset = null;
        Reliability reliability = null;
        if(j<numberOfSubsets)
        {
          subset = newSubsets[j];
          reliability = subset.getReliability();
        }
        //if it's reliability matches that of the current entry of the array
        if( reliability == getReliability(i))
        {
          //store the restored subset in the array
          subsets[i] = subset;
          subsetIDs[i] = newSubsetIDs[j++];
          //find the next valid restored subset
          while(j<numberOfSubsets && newSubsetIDs[j] == NO_ID)
          {
            j++;
          }
        }
      }
      
      //SIB0105.mp.6
      if (dataVersion >= PERSISTENT_VERSION)
        linkTarget = (String)dis.readObject();
    }
    catch (Exception e)
    {
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.gd.StreamSet.restore",
        "1:680:1.67",
        this);

      SibTr.exception(tc, e);
      SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
        new Object[] {
          "com.ibm.ws.sib.processor.gd.StreamSet",
          "1:687:1.67",
          e });
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "restore", "SIErrorException");
      
      throw new SIErrorException(
        nls.getFormattedMessage(
          "INTERNAL_MESSAGING_ERROR_CWSIP0002",
          new Object[] {
            "com.ibm.ws.sib.processor.gd.StreamSet",
            "1:697:1.67",
            e },
          null),
        e);
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
    writer.taggedValue("remoteMEUuid", remoteMEUuid);
    writer.newLine();
    writer.taggedValue("streamID", streamID);
    writer.newLine();
    writer.taggedValue("initialData", initialData);
    writer.newLine();
    writer.taggedValue("type", type);
  }
  
  /**
   * Get the cellule
   * 
   * @return The cellule
   */
  public SIBUuid8 getRemoteMEUuid()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(this, tc, "getRemoteMEUuid");
      SibTr.exit(tc, "getRemoteMEUuid", remoteMEUuid);
    }

    return remoteMEUuid;
  }

  /**
   * Get the destination Uuid
   * 
   * @return The destination Uuid
   */
  public SIBUuid12 getDestUuid()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(this, tc, "getDestUuid");
      SibTr.exit(tc, "getDestUuid", destID);
    }

    return destID;
  }

  /**
   * Set the destination Uuid
   * 
   * @return The destination Uuid
   */
  protected void setDestUuid(SIBUuid12 destID)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "setDestUuid", destID);

    this.destID = destID;
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "setDestUuid");
  }

  /**
   * Get the bus Uuid
   * 
   * @return The bus Uuid
   */
  public SIBUuid8 getBusUuid()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(this, tc, "getBusUuid");
      SibTr.exit(tc, "getBusUuid", busID);
    }

    return busID;
  }
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.store.AbstractItem#getStorageStrategy()
   */
  public int getStorageStrategy()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(this, tc, "getStorageStrategy");

    int storageStrategy;

    storageStrategy = STORE_ALWAYS;

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getStorageStrategy", new Integer(storageStrategy));

    return storageStrategy;
  }

  /**
   * Get the persistence of this StreamSet
   * 
   * @return true if this StreamSet is persistent
   */
  public boolean isPersistent()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
    {
      SibTr.entry(this, tc, "isPersistent");
      SibTr.exit(tc, "isPersistent", Boolean.valueOf(persistent));	
    }
    return persistent;
  }
  
  /**
   * Get the stream ID for this protocol item.
   * 
   * @return An instance of SIBUuid12.
   */
  public SIBUuid12 getStreamID()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(this, tc, "getStreamID");
      SibTr.exit(tc, "getStreamID", streamID);
    }

    return streamID;
  }
  
  /**
   * Set the stream ID for this protocol item.
   * 
   * @parm An instance of SIBUuid12.
   */
  public void setStreamID(SIBUuid12 streamID)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(this, tc, "setStreamID");
      SibTr.exit(tc, "setStreamID", streamID);
    }

    this.streamID = streamID;    
  }
  
  
  /**
   * Set the persistent data for a specific stream
   * 
   * @param priority
   * @param reliability
   * @param completedPrefix
   * @throws SIResourceException
   */
  protected void setPersistentData(int priority,
                                    Reliability reliability,
                                    long completedPrefix) throws SIResourceException
  {
    getSubset(reliability).setPersistentData(priority, completedPrefix);    
  }
      
  /**
   * Get the persistent data for a specific stream
   * 
   * @param priority
   * @param reliability
   * @throws SIResourceException
   */
  protected long getPersistentData(int priority, Reliability reliability) throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "getPersistentData",
        new Object[] { new Integer(priority), reliability });

    long prefix = getSubset(reliability).getPersistentData(priority);
        
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getPersistentData", new Long(prefix));
      
    return prefix;
  }
  
  /**
   * Get an iterator over all of the non-null streams. The order is not
   * guaranteed. 
   * @throws SIResourceException
   * 
   */
  public Iterator<Stream> iterator() throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(this, tc, "iterator");
      
    List<Stream> streams = new ArrayList<Stream>();
    for (int j = 0; j < maxReliabilityIndex + 1; j++)
    {
      ReliabilitySubset subset = getSubset(getReliability(j));
      if(subset != null)
      {
        for (int i = 0; i < SIMPConstants.MSG_HIGH_PRIORITY + 1; i++)
        {      
          Stream stream = subset.getStream(i);
          if (stream != null)
            streams.add(stream);
        }
      }
    }
    
    Iterator<Stream> itr = streams.iterator();
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "iterator", itr);
    
    return itr;
  }

  /**
   * @param reliability
   * @param tran
   */
  public void requestUpdate(Reliability reliability, TransactionCommon tran) throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(this, tc, "requestUpdate", new Object[] { reliability, tran });
    
    ReliabilitySubset subset = getSubset(reliability);
    if(!subset.isUpdating())
    {
      if(tran == null) tran = txManager.createAutoCommitTransaction();
      try
      {
        Transaction msTran = txManager.resolveAndEnlistMsgStoreTransaction(tran);
        subset.requestUpdate(msTran);      
      }
      catch (MessageStoreException e)
      {
        // MessageStoreException shouldn't occur so FFDC.
        FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.gd.StreamSet.requestUpdate",
        "1:917:1.67",
        this);
  
        SibTr.exception(tc, e);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "requestUpdate", "SIStoreException");
        throw new SIResourceException(e);
      }
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "requestUpdate");
  }
  
  /**
   * @author tevans
   *
   * The ReliabilitySubset contains all the streams for a specific reliability
   */
  public static class ReliabilitySubset extends SIMPItem
  {
    private Reliability reliability;
    private long[] persistentData;
    private Stream[] streams;

    private static final TraceNLS nls =
        TraceNLS.getTraceNLS(SIMPConstants.RESOURCE_BUNDLE);
  
    private static TraceComponent tc =
      SibTr.register(
        ReliabilitySubset.class,
        SIMPConstants.MP_TRACE_GROUP,
        SIMPConstants.RESOURCE_BUNDLE);
    
    public ReliabilitySubset()
    {
      super();
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(this, tc, "ReliabilitySubset");
        
      initialize(-1); //-1 is used as initialData since it should be reset by the restore method
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "ReliabilitySubset", this);
    }
    
    /**
     * @param reliability
     */
    public void setReliability(Reliability reliability)
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(this, tc, "setReliability", reliability);
      
      this.reliability = reliability;

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "setReliability");
    }

    public ReliabilitySubset(Reliability reliability, long initialData)
    {
      super();
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(this, tc, "ReliabilitySubset", new Object[] { reliability, Long.valueOf(initialData) });
              
      this.reliability = reliability;
      initialize(initialData);
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "ReliabilitySubset", this);
    }
  
    /**
     * Initialize some arrays and set some default values.
     */
    private void initialize(long initialData)
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(this, tc, "initialize", Long.valueOf(initialData));
  
      //array of streams by priority
      streams = new Stream[SIMPConstants.MSG_HIGH_PRIORITY + 1];
      //default completed prefixes by priority
      persistentData = new long[SIMPConstants.MSG_HIGH_PRIORITY + 1];
    
      for(int i=0;i<SIMPConstants.MSG_HIGH_PRIORITY + 1;i++)
      {
        persistentData[i] = initialData;
      }
  
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "initialize");
    }
    
    public Reliability getReliability()
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      {
        SibTr.entry(this, tc, "getReliability");
        SibTr.exit(tc, "getReliability", reliability);
      }
      
      return reliability;
    }
    
    /**
     * @param priority
     */
    public Stream getStream(int priority)
    {
      return streams[priority];
    }
  
    /**
     * @param priority
     * @param stream
     */
    public void setStream(int priority, Stream stream)
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      {
        SibTr.entry(this, tc, "setStream", new Object[] {Integer.valueOf(priority), stream});
        SibTr.exit(tc, "setStream");
      }
      streams[priority] = stream;
    }
    
    /**
     * @param priority
     */
    public long getCompletedPrefix(int priority)
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      {
        SibTr.entry(this, tc, "getCompletedPrefix", Integer.valueOf(priority));
        SibTr.exit(tc, "getCompletedPrefix", Long.valueOf(streams[priority].getCompletedPrefix()));
      }
      
      return streams[priority].getCompletedPrefix();
    }
  
    /**
     * @param completedPrefixes
     */
    public void setPersistentData(long[] completedPrefixes)
    {
      persistentData = completedPrefixes;
    }
  
    /**
     * @param priority
     * @param completedPrefix
     */
    public void setPersistentData(int priority, long completedPrefix)
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      {
        SibTr.entry(this, tc, "setPersistentData", new Object[] { Integer.valueOf(priority), Long.valueOf(completedPrefix)});
        SibTr.exit(tc, "setPersistentData");
      }
      
      persistentData[priority] = completedPrefix;
    }
    
    public long getPersistentData(int priority)
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      {
        SibTr.entry(this, tc, "getPersistentData", Integer.valueOf(priority));
        SibTr.exit(tc, "getPersistentData", Long.valueOf(persistentData[priority]));
      }
      
      return persistentData[priority];
    }
    
    /* (non-Javadoc)
     * @see com.ibm.ws.sib.processor.impl.store.items.SIMPItem#getPersistentData(java.io.ObjectOutputStream)
     */
    public void getPersistentData(ObjectOutputStream dos) 
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(this, tc, "getPersistentData", dos);

      try
      { 
        //write out each of the completed prefixes
        for(int i=0;i<streams.length;i++)
        {
          if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(this, tc, " persistentData["+i+"] : " + persistentData[i]);
          
          dos.writeLong(persistentData[i]);
        }
      }
      catch (IOException e)
      {
        FFDCFilter.processException(
          e,
          "com.ibm.ws.sib.processor.gd.StreamSet.ReliabilitySubset.getPersistentData",
          "1:1084:1.67",
          this);
  
        SibTr.exception(tc, e);
        SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
          new Object[] {
            "com.ibm.ws.sib.processor.gd.ReliabilitySubset",
            "1:1091:1.67",
            e });
          
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "getPersistentData", "SIErrorException");
        
        throw new SIErrorException(
          nls.getFormattedMessage(
            "INTERNAL_MESSAGING_ERROR_CWSIP0002",
            new Object[] {
              "com.ibm.ws.sib.processor.gd.ReliabilitySubset",
              "1:1101:1.67",
              e },
            null),
          e);
      }
  
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "getPersistentData");
    }
  
    /* (non-Javadoc)
     * @see com.ibm.ws.sib.processor.impl.store.items.SIMPItem#restore(java.io.ObjectInputStream, int)
     */
    public void restore(ObjectInputStream dis, int dataVersion) 
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(
          tc, "restore", new Object[] { dis, new Integer(dataVersion) });
  
      try
      {
        //read in all the completed prefixes
        for(int i=0;i<streams.length;i++)
        {
          long completedPrefix = dis.readLong();
          setPersistentData(i, completedPrefix);
        }
      }
      catch (Exception e)
      {
        // FFDC
        FFDCFilter.processException(
          e,
          "com.ibm.ws.sib.processor.gd.StreamSet.ReliabilitySubset.restore",
          "1:1135:1.67",
          this);
  
        SibTr.exception(tc, e);
        SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
        new Object[] {
          "com.ibm.ws.sib.processor.gd.StreamSet.ReliabilitySubset",
          "1:1142:1.67",
          e } );
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "restore", "SIErrorException");
        
        throw new SIErrorException(
          nls.getFormattedMessage(
            "INTERNAL_MESSAGING_ERROR_CWSIP0002",
            new Object[] {
              "com.ibm.ws.sib.processor.gd.StreamSet.ReliabilitySubset",
              "1:1151:1.67",
              e },
            null),
          e);
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
      writer.taggedValue("reliability", reliability);
      
      boolean equal = true;
      for(int i=1;i<streams.length;i++)
      {
        if(persistentData[i] != persistentData[i-1])
          equal = false;
      }

      if(equal)
      {
        writer.newLine();
        writer.taggedValue("allData", persistentData[0]);
      }
      else
      {
        for(int i=1;i<streams.length;i++)
        {
          if(persistentData[i] != 0)
          {
            writer.newLine();
            writer.taggedValue("subsetData_"+i, persistentData[i]);
          }
        }
      }
    }
    
    /* (non-Javadoc)
     * @see com.ibm.ws.sib.store.AbstractItem#getStorageStrategy()
     */
    public int getStorageStrategy()
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(this, tc, "getStorageStrategy");
  
      int storageStrategy;
  
      if (reliability.compareTo(Reliability.BEST_EFFORT_NONPERSISTENT) <= 0)
      {
        storageStrategy = STORE_NEVER;
      }
      else if (reliability.compareTo(Reliability.RELIABLE_NONPERSISTENT) <= 0)
      {
        storageStrategy = STORE_MAYBE;
      }
      else if (reliability.compareTo(Reliability.RELIABLE_PERSISTENT) <= 0)
      {
        storageStrategy = STORE_EVENTUALLY;
      }
      else
      {
        storageStrategy = STORE_ALWAYS;
      }      
  
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "getStorageStrategy", new Integer(storageStrategy));
    
      return storageStrategy;
    }    
  }

  public void remove() 
    throws SIRollbackException, 
           SIConnectionLostException, 
           SIResourceException, 
           SIErrorException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "remove");
    LocalTransaction tran = txManager.createLocalTransaction(true);
    
    try
    {
      for(int i=0;i<subsets.length;i++)
      {      
        if(subsets[i] != null && subsets[i].isInStore())
        {
          subsets[i].remove((Transaction) tran, subsets[i].getLockID());
        }
      }
      remove((Transaction) tran, getLockID());
      tran.commit();          
    }
    catch (SIIncorrectCallException e)
    {
      // No FFDC code needed
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "remove", "SIResourceException");
      throw new SIResourceException(e);
    }
    catch (MessageStoreException e)
    {
      // MessageStoreException shouldn't occur so FFDC.
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.gd.StreamSet.remove",
        "1:1228:1.67",
        this);

      SibTr.exception(tc, e);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "remove", e);

      throw new SIResourceException(e);
    }  
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "remove");  
  }

  /**
   * @param txManager
   */
  public void initializeNonPersistent(SIMPTransactionManager txManager)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(this, tc, "initializeNonPersistent", txManager);
    this.txManager = txManager;
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "initializeNonPersistent");
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.ControllableResource#getControlAdapter()
   */
  public ControlAdapter getControlAdapter()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(this, tc, "getControlAdapter");

    if(controlAdapter == null)
    {
      createControlAdapter();
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getControlAdapter", controlAdapter);
      
    return controlAdapter;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.ControllableResource#createControlAdapter()
   */
  public void createControlAdapter()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(this, tc, "createControlAdapter");

    if(type == Type.SOURCE)
      controlAdapter = new SourceStreamSetControl(remoteMEUuid, this);  
    else if (type == Type.LINK_SOURCE)
        controlAdapter = new LinkTransmitterControl(remoteMEUuid, this, true);
    else if (type == Type.LINK_REMOTE_SOURCE)
      controlAdapter = new LinkTransmitterControl(remoteMEUuid, this, false);
    else if(type == Type.TARGET)
      controlAdapter = new TargetStreamSetControl(this);
    else if (type == Type.LINK_TARGET)
      controlAdapter = new LinkReceiverControl(this, linkTarget);
    else if(type == Type.INTERNAL_OUTPUT)
      controlAdapter = new InternalOutputStreamSetControl(this);
    else if(type == Type.LINK_INTERNAL_OUTPUT)
      controlAdapter = new LinkPublicationPointControl(this);
    else if(type == Type.INTERNAL_INPUT)
    {
      // Nothing to do for an InternalInputStream set, we don't display those
    }
      
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "createControlAdapter");
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.ControllableResource#dereferenceControlAdapter()
   */
  public void dereferenceControlAdapter()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(this, tc, "dereferenceControlAdapter");

    if(controlAdapter != null)
    {
      controlAdapter.dereferenceControllable();
      controlAdapter = null;
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "dereferenceControlAdapter");
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.ControllableResource#registerControlAdapterAsMBean()
   */
  public void registerControlAdapterAsMBean()
  {
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.ControllableResource#deregisterControlAdapterMBean()
   */
  public void deregisterControlAdapterMBean()
  {
  }  

  @Override
  public int getPersistentVersion() 
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(this, tc, "getPersistentVersion");
      SibTr.exit(tc, "getPersistentVersion", Integer.valueOf(PERSISTENT_VERSION));
    }
    return PERSISTENT_VERSION;
  }

}
