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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.msgstore.Item;
import com.ibm.ws.sib.msgstore.ItemStream;
import com.ibm.ws.sib.msgstore.MessageStoreException;
import com.ibm.ws.sib.msgstore.transactions.Transaction;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.impl.interfaces.SIMPMessage;
import com.ibm.ws.sib.utils.DataSlice;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * @author caseyj
 *
 * All ItemStreams in the Message Processor should be children of this class.
 * Except possibly for temporary streams, you should not instantiate this
 * object directly.
 */
public class SIMPItemStream extends ItemStream
{
  /**
   * Trace for the component
   */
  private static final TraceComponent tc =
    SibTr.register(
      SIMPItemStream.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);

  /**
   * NLS for component
   */
  private static final TraceNLS nls =
    TraceNLS.getTraceNLS(SIMPConstants.RESOURCE_BUNDLE);


 
  /**
   * The default object version number used if the class has not supplied its
   * own by overriding {@link #getPersistentVersion()}
   */
  private static final int DEFAULT_PERSISTENT_VERSION = 1;

  /**
   * storageStrategy is the reply given to message store when it queries
   * getStorageStrategy, as it does on spill (though this is irrelevant as long
   * as objects extending this class are not dereferenced) or persist.
   * <p>
   * By setting it in object initialization, we are both setting it when
   * children are initially created and when children are
   * recreated by the message store during warm restart.
   * <p>
   * Children will need to reset storageStrategy if they want something
   * different.
   */
  private int storageStrategy = STORE_ALWAYS;

  /**
   * Warm start constructor for the Message Store.  You probably should not be
   * instantiating this class - use a child class.
   */
  public SIMPItemStream()
  {
    super();

    // This space intentionally blank

  }

  /**
   * Removes all items from this Stream.
   * <p>
   * This function recursively removes Items, ItemStreams and ReferenceStreams.
   * <p>
   * WARNING: Be careful using this function, since trying to remove many
   * items (more than 100) in more than one transaction may cause a transaction
   * error, since a single transaction can only be so big.  Auto-commit
   * transactions should be okay.  For a better database friendly deletion
   * approach see @see com.ibm.ws.sib.msgstore.example.EmptyableItemStream
   * <p>
   * Feature 174199.2.13
   *
   * @param tran is the transaction to use for removals.
   *
   * @throws MessageStoreException if a problem occurs when actually removing
   * from the Message Store.
   */
  public void removeAll(Transaction tran) throws MessageStoreException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "removeAll", tran);

    // 181632.1
    // We need to remove Item, ItemStreams and ReferenceStreams in three
    // different passes.  Phill ensures me this is not too expensive.
    Item item = null;
    SIMPItemStream itemStream = null;
    SIMPReferenceStream referenceStream = null;

    while (null != (item = findFirstMatchingItem(null)))
    {
      item.remove(tran, NO_LOCK_ID);
    }

    while (null != (itemStream
      = (SIMPItemStream)findFirstMatchingItemStream(null)))
    {
      itemStream.removeAll(tran);
    }

    while (null != (referenceStream
      = (SIMPReferenceStream)findFirstMatchingReferenceStream(null)))
    {
      referenceStream.removeAll(tran);
    }

    removeItemStream(tran, NO_LOCK_ID);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "removeAllItems");
  }

  public void removeItemStream(Transaction transaction, long lockID) throws MessageStoreException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "removeItemStream", new Object[] { transaction, new Long(lockID) });

    remove(transaction, lockID);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "removeItemStream");
  }

  /**
   * @see com.ibm.ws.sib.msgstore.AbstractItem#getStorageStrategy()
   */
  public final int getStorageStrategy()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getStorageStrategy");

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getStorageStrategy", new Integer(storageStrategy));

    return storageStrategy;
  }

  /**
   * Set the storage strategy of this stream.  Needs to be called before
   * the stream is actually stored.
   *
   * @param setStrategy
   */
  protected final void setStorageStrategy(int setStrategy)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "setStorageStrategy", new Integer(setStrategy));

    storageStrategy = setStrategy;

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "setStorageStrategy");
  }

  /**
   * Return the class version number when the MessageStore wants to persist
   * the object.  Each class will have its own version number.  Any
   * time the data persisted is changed, this version will have to be
   * incremented so that the restore routine can distinguish between old and
   * new objects and act accordingly.
   */
  public int getPersistentVersion()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getPersistentVersion");
      SibTr.exit(tc, "getPersistentVersion", new Integer(DEFAULT_PERSISTENT_VERSION));
    }

    return DEFAULT_PERSISTENT_VERSION;
  }

  /**
   * Child classes should override this function to write their persistent
   * data (excluding object version number).
   *
   * @param oos  ObjectOutputStream to write data to.
   */
  protected void getPersistentData(ObjectOutputStream oos)
  {

    // Intentionally blank.

  }

  /**
   * Child classes should override this method to restore their persistent
   * data.
   *
   * @param ois ObjectInputStream to restore data from.
   * @param dataVersion  Version number of object read from store.
   */
  protected void restore(ObjectInputStream ois, int dataVersion)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "restore", new Integer(dataVersion));

    checkPersistentVersionId(dataVersion);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "restore");
  }

  /**
   * @see com.ibm.ws.sib.msgstore.AbstractItem#getPersistentData().
   * <p>
   * Child classes should always use
   * {@link #getPersistentData(ObjectOutputStream)}
   * to provide the data they want to persist (excluding class version number
   * which is obtained seperately via {@link #getPersistentVersion()}).
   */
  // Feature SIB0112b.mp.1
  public final List<DataSlice> getPersistentData()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getPersistentData");

    ByteArrayOutputStream bos = new ByteArrayOutputStream();

    try
    {
      ObjectOutputStream oos = new ObjectOutputStream(bos);

      // First object stored should always be the version integer.
      oos.writeInt(getPersistentVersion());

      getPersistentData(oos);

      oos.close();
    }
    catch (Exception e)
    {
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.impl.store.itemstreams.SIMPItemStream.getPersistentData",
        "1:297:1.54",
        this);

      SibTr.exception(tc, e);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "getPersistentData", e);

      throw new SIErrorException(e);
    }

    byte[] data = bos.toByteArray();

    List<DataSlice> dataSlices = new ArrayList<DataSlice>(1);
    dataSlices.add(new DataSlice(data));

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getPersistentData", dataSlices);

    return dataSlices;
  }

  protected void checkPersistentVersionId(int dataVersion)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "checkPersistentVersionId", new Integer(dataVersion));

    if (dataVersion != getPersistentVersion())
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "checkPersistentVersionId", "SIErrorException");

      SibTr.error(tc, "ITEM_RESTORE_ERROR_CWSIP0261",
        new Object[] {new Integer(getPersistentVersion()),
                      new Integer(dataVersion)});

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "checkPersistentVersionId", "SIErrorException");

      throw new SIErrorException(
        nls.getFormattedMessage(
          "ITEM_RESTORE_ERROR_CWSIP0261",
          new Object[] {new Integer(getPersistentVersion()),
                        new Integer(dataVersion) },
          null));
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "checkPersistentVersionId");
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.store.AbstractItem#restore(List<DataSlice>)
   */
  // Feature SIB0112b.mp.1
  public final void restore(List<DataSlice> dataSlices)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "restore", dataSlices);

    try
    {
      DataSlice slice = dataSlices.get(0);

      ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(slice.getBytes()));

      // First object stored should always be the version integer.
      restore(ois, ois.readInt());

      ois.close();
    }
    catch (IOException e)
    {
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.impl.store.itemstreams.SIMPItemStream.restore",
        "1:372:1.54",
        this);

      SibTr.exception(tc, e);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "restore", e);

      throw new SIErrorException(e);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "restore");
  }

  /**
   * @param id
   */
  public void setCurrentTransaction(SIMPMessage msg, boolean isRemote)
  {
    // Overridden by PtoPMessageItemStream
  }
}
