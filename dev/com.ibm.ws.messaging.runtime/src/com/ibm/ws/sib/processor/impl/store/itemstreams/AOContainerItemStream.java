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
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.utils.ras.FormattedWriter;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.ws.sib.utils.SIBUuid12;

/**
 * The persistent ItemStream for each protocol stream, at the Anycast Output Handler.
 */
public final class AOContainerItemStream extends SIMPItemStream
{
  private static final TraceComponent tc =
  SibTr.register(
    AOContainerItemStream.class,
    SIMPConstants.MP_TRACE_GROUP,
    SIMPConstants.RESOURCE_BUNDLE);

  /**
   * If this field is non-null, then this stream was created for
   * remote access to a durable subscription "homed" on this ME.
   * This is really only needed during reconstitution.
   */
  private SIBUuid12 durablePseudoDestID;
  
  /**
   * This next field needs to be persisted along with the stream
   * so that at recovery time we can determine whether or not the
   * durable subscription exists anymore (and hence whether or
   * not we should bother restoring the stream).
   */
  private String durableSubName;

  public AOContainerItemStream()
  {
    super();
  }
  
  public AOContainerItemStream(SIBUuid12 durableID, String subName)
  {
    super();
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "AOContainerItemStream", new Object[] {durableID, subName});

    durablePseudoDestID = durableID;
    durableSubName      = subName;

    if (tc.isEntryEnabled())
      SibTr.exit(tc, "AOContainerItemStream", this);

  }
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.store.itemstreams.SIMPItemStream#getPersistentData(java.io.ObjectOutputStream)
   */
  public void getPersistentData(ObjectOutputStream oos)
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "getPersistentData", oos);

    try
    {
      HashMap hm = new HashMap();

      if (durablePseudoDestID != null)
        hm.put("durablePseudoDestID", durablePseudoDestID.toByteArray());
      if (durableSubName != null)
        hm.put("durableSubName", durableSubName);
      
      oos.writeObject(hm);
    }
    catch (IOException e)
    {
      // FFDC
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.impl.store.itemstreams.AOContainerItemStream.getPersistentData",
        "1:113:1.13",
        this);
        
      SIErrorException e2 = new SIErrorException(e);
      SibTr.exception(tc, e2);
      if (tc.isEntryEnabled()) SibTr.exit(tc, "getPersistentData", e2);
      throw e2;
    }

    if (tc.isEntryEnabled())
      SibTr.exit(tc, "getPersistentData");
  }

  public void restore(ObjectInputStream ois, int dataVersion)
  {
    if (tc.isEntryEnabled())
      SibTr.entry(
        tc, "restore", new Object[] { ois, new Integer(dataVersion) });
    
    checkPersistentVersionId(dataVersion);

    try
    {
      HashMap hm = (HashMap)ois.readObject();
      
      if (hm.containsKey("durablePseudoDestID"))
        durablePseudoDestID = new SIBUuid12((byte[])hm.get("durablePseudoDestID"));
      if (hm.containsKey("durableSubName"))
        durableSubName = (String) hm.get("durableSubName");
    }
    catch (Exception e)
    {
      // FFDC
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.impl.store.itemstreams.AOContainerItemStream.restore",
        "1:149:1.13",
        this);
        
      SIErrorException e2 = new SIErrorException(e);
      SibTr.exception(tc, e2);
      
      if (tc.isEntryEnabled()) SibTr.exit(tc, "restore", e2);
      
      throw e2;
    }

    if (tc.isEntryEnabled())
      SibTr.exit(tc, "restore");
  }

  /**
   * Prints the Message Details to the xml output.
   */
  public void xmlWriteOn(FormattedWriter writer) throws IOException
  {
    if(durablePseudoDestID != null)
    {
      writer.newLine();
      writer.taggedValue("durablePseudoDestID", durablePseudoDestID);
    }
    if(durableSubName != null)
    {
      writer.newLine();
      writer.taggedValue("durableSubName", durableSubName);
    }
  }

  public SIBUuid12 getDurablePseudoDestID()
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "getDurablePseudoDestID");
    if (tc.isEntryEnabled())
      SibTr.exit(tc, "getDurablePseudoDestID", durablePseudoDestID);
    return durablePseudoDestID;
  }
  
  public String getDurableSubName()
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "getDurableSubName");
    if (tc.isEntryEnabled())
      SibTr.exit(tc, "getDurableSubName", durableSubName);
    return durableSubName;
  }
}
