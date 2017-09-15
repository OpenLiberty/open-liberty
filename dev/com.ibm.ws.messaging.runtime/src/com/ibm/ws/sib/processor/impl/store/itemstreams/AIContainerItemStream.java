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
import java.util.ArrayList;
import java.util.HashMap;

import com.ibm.websphere.ras.TraceComponent;

import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.impl.store.items.AOValue;
import com.ibm.ws.sib.utils.SIBUuid8;
import com.ibm.ws.sib.utils.SIBUuid12;
import com.ibm.ws.sib.utils.ras.FormattedWriter;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * Trivial class to allow instances of SIMPItemStream
 */
public class AIContainerItemStream extends SIMPItemStream
{
  // Standard debug/trace
  private static final TraceComponent tc =
  SibTr.register(
    AIContainerItemStream.class,
    SIMPConstants.MP_TRACE_GROUP,
    SIMPConstants.RESOURCE_BUNDLE);
  
  private SIBUuid8 dmeId;
  private SIBUuid12 gatheringTargetDestUuid;
  
  /**
   * If this is non-null then this aistream was created
   * in support of remote durable access.
   */
  private SIBUuid12 durablePseudoDestID;
  
  /**
   * If this stream was used for remote access to a durable
   * sub, then this is the name of that sub.
   */
  private String durableSubName;
  
  /**
   * If this stream was used for remote access to a durable
   * sub, the this is the name of the durable home
   * for that sub.
   */
  private String durableSubHome;
  
  /**
   * SIB0113
   * List to store the AOValues that need to be considered when we reconstitute our
   * AIStreams (for use in IME reconstitution)
   */
  private ArrayList<AOValue> aoLinks;
    
  /**
   *
   */
  public AIContainerItemStream()
  {
    super();
  }

  public AIContainerItemStream(SIBUuid8 dmeId, SIBUuid12 gatheringTargetDestUuid, SIBUuid12 durableDest, String subName, String durableHome)
  {
    super();

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "AIContainerItemStream", new Object[] { dmeId, gatheringTargetDestUuid, durableDest, subName, durableHome });
    
    this.dmeId = dmeId;
    this.gatheringTargetDestUuid = gatheringTargetDestUuid;
    durablePseudoDestID = durableDest;
    durableSubName      = subName;
    durableSubHome      = durableHome;

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "AIContainerItemStream", this);
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
      HashMap hm = new HashMap();

      hm.put("dmeId", dmeId.toByteArray());
      if(gatheringTargetDestUuid != null)
        hm.put("gatheringTargetDestUuid", gatheringTargetDestUuid.toByteArray());
      if (durablePseudoDestID != null)
        hm.put("durablePseudoDestID", durablePseudoDestID.toByteArray());
      if (durableSubName != null)
        hm.put("durableSubName", durableSubName);
      if (durableSubHome != null)
        hm.put("durableSubHome", durableSubHome);
      
      oos.writeObject(hm);
    }
    catch (IOException e)
    {
      // FFDC
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.impl.store.itemstreams.AIContainerItemStream.getPersistentData",
        "1:150:1.23.1.2",
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
      
      dmeId = new SIBUuid8((byte[])hm.get("dmeId"));
      if (hm.containsKey("gatheringTargetDestUuid"))  // This should work ok with legacy itemstreams also
        gatheringTargetDestUuid = new SIBUuid12((byte[])hm.get("gatheringTargetDestUuid"));
      if (hm.containsKey("durablePseudoDestID"))
        durablePseudoDestID = new SIBUuid12((byte[])hm.get("durablePseudoDestID"));
      if (hm.containsKey("durableSubName"))
        durableSubName = (String) hm.get("durableSubName");
      if (hm.containsKey("durableSubHome"))
        durableSubHome = (String) hm.get("durableSubHome");
    }
    catch (Exception e)
    {
      // FFDC
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.impl.store.itemstreams.AIContainerItemStream.restore",
        "1:191:1.23.1.2",
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
    writer.taggedValue("DMEUuid", dmeId);
    if(gatheringTargetDestUuid != null)
    {
      writer.newLine();
      writer.taggedValue("gatheringTargetDestUuid", gatheringTargetDestUuid);
    }
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
    if(durableSubHome != null)
    {
      writer.newLine();
      writer.taggedValue("durableSubHome", durableSubHome);
    }
  }

  public SIBUuid8 getDmeId()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getDmeId");

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getDmeId", dmeId);

    return dmeId;
  }
  
  public SIBUuid12 getDurablePseudoDestID()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getDurablePseudoDestID");

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getDurablePseudoDestID", durablePseudoDestID);
    
    return durablePseudoDestID;
  }
  
  public String getDurableSubName()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getDurableSubName");
      
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getDurableSubName", durableSubName);
      
    return durableSubName;
  }

  public String getDurableSubHome()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getDurableSubHome");
      
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getDurableSubHome", durableSubHome);
      
    return durableSubHome;
  }

  public SIBUuid12 getGatheringTargetDestUuid() 
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getGatheringTargetDestUuid");
      SibTr.exit(tc, "getGatheringTargetDestUuid", gatheringTargetDestUuid);
    }
      
    return gatheringTargetDestUuid;
  }

  public void addAOLink(AOValue val) 
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "addAOLink", val);
    
    if (aoLinks==null)
      aoLinks = new ArrayList<AOValue>(10);
    
    aoLinks.add(val);
      
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "addAOLink");
  } 
  
  public ArrayList<AOValue> getAOLinks() 
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getAOLinks");
      SibTr.exit(tc, "getAOLinks", aoLinks);
    }
      
    return aoLinks;
  }
  
  
}
