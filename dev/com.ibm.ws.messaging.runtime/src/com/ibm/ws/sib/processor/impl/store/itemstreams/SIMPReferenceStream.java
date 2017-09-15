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
import java.util.Collection;
import java.util.List;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.msgstore.MessageStoreException;
import com.ibm.ws.sib.msgstore.ReferenceStream;
import com.ibm.ws.sib.msgstore.SevereMessageStoreException;
import com.ibm.ws.sib.msgstore.transactions.Transaction;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.impl.interfaces.SIMPMessage;
import com.ibm.ws.sib.utils.DataSlice;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * @author tevans
 *
 * All ReferenceStreams in the Message Processor should be children of this
 * class.
 * Except possibly for temporary streams, you should not instantiate this
 * object directly.
 */
public class SIMPReferenceStream extends ReferenceStream
{
  /**
   * Trace for the component
   */
  private static TraceComponent tc =
    SibTr.register(
      SIMPReferenceStream.class,
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
   * List of msgRefIds to be initialised at reconcile time. For the case when
   * a message ref is restored before reconciliation.
   */
  private List unrestoredMsgIds = new ArrayList();

  /**
   * Warm start constructor for the Message Store.  You probably should not be
   * instantiating this class - use a child class.
   */
  public SIMPReferenceStream()
  {
    super();

    // This space intentionally blank

  }

  /**
   * Removes all items from this reference stream
   *
   * @param tran - the transaction to perform the removals under
   * @throws MessageStoreException
   */

  public void removeAll(Transaction tran) throws MessageStoreException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "removeAll", tran);

    while(this.removeFirstMatching(null, tran) != null);

    remove(tran, NO_LOCK_ID);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "removeAll");
  }

  /**
   * @see com.ibm.ws.sib.msgstore.AbstractItem#getStorageStrategy()
   */
  public final int getStorageStrategy()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getStorageStrategy");
      SibTr.exit(tc, "getStorageStrategy", new Integer(storageStrategy));
    }
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
  throws SevereMessageStoreException
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
        "com.ibm.ws.sib.processor.impl.store.itemstreams.SIMPReferenceStream.getPersistentData",
        "1:255:1.44",
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

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.store.AbstractItem#restore(byte[])
   */
  // Feature SIB0112b.mp.1
  public final void restore(List<DataSlice> dataSlices)
  throws SevereMessageStoreException
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
        "com.ibm.ws.sib.processor.impl.store.itemstreams.SIMPReferenceStream.restore",
        "1:303:1.44",
        this);

      SibTr.exception(tc, e);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "restore", e);

      throw new SIErrorException(e);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "restore");
  }

  protected void checkPersistentVersionId(int dataVersion)
  throws SevereMessageStoreException
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

      throw new SevereMessageStoreException(
        nls.getFormattedMessage(
          "ITEM_RESTORE_ERROR_CWSIP0261",
          new Object[] {new Integer(getPersistentVersion()),
                        new Integer(dataVersion) },
          null));
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "checkPersistentVersionId");
  }

  /**
   * A ref stream is in doubt if it has indoubt references
   * attached i.e. if the tran putting the items has been put in doubt.
   * Because the message store starts before message processor, and such in doubt
   * items are rebuilt at message store startup, they must be cached and
   * initialized properly at message processor startup.
   * See defect 257231.
   * This method is overriden by DurableSubscriptionItemStream objects.
   * @return
   * @author tpm
   */
  public boolean hasInDoubtItems()
  {
    //default a stream is not in doubt
    return false;
  }


  /**
   * Adds a msgId to the list of uninitialised msg references
   */
  public void addUnrestoredMsgId(long msgId)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "addUnrestoredMsgId", new Long(msgId));

    unrestoredMsgIds.add(new Long(msgId));

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "addUnrestoredMsgId");
  }

  /**
   * Returns a reference to the list of unrestored messages and nullifies
   * the internal reference for subsequent garbage collection.
   * @return
   * @author tpm
   */
  public Collection clearUnrestoredMessages()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "clearUnrestoredMessages");

    Collection returnCollection = unrestoredMsgIds;
    // bug fix
    unrestoredMsgIds = new ArrayList();

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "clearUnrestoredMessages", returnCollection);
    return  returnCollection;
  }

  /**
   * @param id
   */
  public void setCurrentTransaction(SIMPMessage msg, boolean isRemote)
  {
    // Overridden by DurableSubscriptionItemStream
  }
}
