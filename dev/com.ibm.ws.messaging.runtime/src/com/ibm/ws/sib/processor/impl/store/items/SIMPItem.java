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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.msgstore.Item;
import com.ibm.ws.sib.msgstore.SevereMessageStoreException;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.utils.DataSlice;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * @author caseyj
 *
 * All non message Items in the Message Processor should be children of this
 * class.  You should not instantiate this
 * object directly.
 */
public class SIMPItem extends Item
{
  /**
   * Trace for the component
   */
  private static final TraceComponent tc =
    SibTr.register(
      SIMPItem.class,
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
   * Warm start constructor for the Message Store.  You probably should not be
   * instantiating this class - use a child class.
   */
  public SIMPItem()
  {
    super();

    // This space intentionally blank
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
      SibTr.exit(tc, "getPersistentVersion", DEFAULT_PERSISTENT_VERSION);
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
   * @throws SevereMessageStoreException 
   */
  protected void restore(ObjectInputStream ois, int dataVersion) throws SevereMessageStoreException
  
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "restore", new Object[] { dataVersion});
    checkPersistentVersionId(dataVersion);
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "restore");
  }

  protected void checkPersistentVersionId(int dataVersion)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "checkPersistentVersionId", dataVersion);

    if (dataVersion != getPersistentVersion())
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "checkPersistentVersionId", "SIErrorException");

      SibTr.error(tc, "ITEM_RESTORE_ERROR_CWSIP0261",
        new Object[] {getPersistentVersion(),dataVersion});

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "checkPersistentVersionId", "SIErrorException");

      throw new SIErrorException(
        nls.getFormattedMessage(
          "ITEM_RESTORE_ERROR_CWSIP0261",
          new Object[] {getPersistentVersion(),dataVersion},
          null));
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "checkPersistentVersionId");
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
        "com.ibm.ws.sib.processor.impl.store.items.SIMPItem.getPersistentData",
        "1:208:1.21",
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
  public final void restore(List<DataSlice> dataSlices) throws SevereMessageStoreException
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
        "com.ibm.ws.sib.processor.impl.store.items.SIMPItem.restore",
        "1:255:1.21",
        this);

      SibTr.exception(tc, e);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "restore", e);

      throw new SIErrorException(e);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "restore");
  }

}
