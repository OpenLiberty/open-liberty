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
 package com.ibm.ws.sib.processor.test;

import java.util.Map;
import java.util.Set;

import com.ibm.websphere.sib.Reliability;
import com.ibm.ws.sib.admin.ForeignBusDefinition;
import com.ibm.ws.sib.admin.VirtualLinkDefinition;
import com.ibm.ws.sib.utils.SIBUuid12;

/**
 * @author gatfora
 */
public class VirtualLinkDefinitionImpl implements VirtualLinkDefinition
{
  String name;
  SIBUuid12 uuid;
  Map topicSpaceMap;
  Set localizingMEuuidSet = null; // of stringified SIBUuid8
  String type = "SIBVirtualGatewayLink";
  ForeignBusDefinition foreignBusDefinition = null;
  String inboundUserid = null;
  String outboundUserid = null;
  String exceptionDestination = null;
  boolean preferLocal = true;
  Reliability exceptionDiscardReliability = Reliability.BEST_EFFORT_NONPERSISTENT;
  
  public VirtualLinkDefinitionImpl(String name,
                            SIBUuid12 uuid,
                            Map topicSpaceMap)
  {
    this.name = name;
    this.uuid = uuid;
    this.topicSpaceMap = topicSpaceMap;
  }
  
  public void setForeignBusDefinition(ForeignBusDefinition foreignBusDefinition)
  {
    this.foreignBusDefinition = foreignBusDefinition;
  }
  
  public void setExceptionDestination(String exceptionDestination)
  {
    this.exceptionDestination = exceptionDestination;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.admin.VirtualLinkDefinition#getUuid()
   */
  public SIBUuid12 getUuid()
  {
    return uuid;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.admin.VirtualLinkDefinition#getName()
   */
  public String getName()
  {
    return name;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.admin.VirtualLinkDefinition#getForeignBus()
   */
  public ForeignBusDefinition getForeignBus()
  {
    return foreignBusDefinition;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.admin.VirtualLinkDefinition#getLinkLocalitySet()
   */
  public Set getLinkLocalitySet()
  {
    return localizingMEuuidSet;
  }
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.admin.VirtualLinkDefinition#setLinkLocalitySet()
   */
  public void setLinkLocalitySet(Set localizingMEs)
  {
    localizingMEuuidSet = localizingMEs;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.admin.VirtualLinkDefinition#getTopicSpaceMappings()
   */
  public Map getTopicSpaceMappings()
  {
    return topicSpaceMap;  
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.admin.VirtualLinkDefinition#getType()
   */
  
  public void setType(String  type)
  {
    this.type = type;
  }
  
  public String getType()
  {
    return type;
  }

  public void setInboundUserid(String inboundUserid)
  {
    this.inboundUserid = inboundUserid;
  }
      
  public String getInboundUserid()
  {
    return inboundUserid;   
  }

  public void setOutboundUserid(String outboundUserid)
  {
    this.outboundUserid = outboundUserid;
  }
  
  public String getOutboundUserid()
  {
    return outboundUserid;
  }

  public String getExceptionDestination()
  {
    return exceptionDestination;
  }

  public void setPreferLocal(boolean preferLocal)
  {
	  this.preferLocal = preferLocal;
  }
  
  public boolean getPreferLocal()
  {
    return preferLocal;  
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.admin.VirtualLinkDefinition#getExceptionDiscardReliability()
   */
  public Reliability getExceptionDiscardReliability()
  {
    return exceptionDiscardReliability;
  }
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.admin.VirtualLinkDefinition#setExceptionDiscardReliability()
   */
  public void setExceptionDiscardReliability(Reliability exceptionDiscardReliability)
  {
    this.exceptionDiscardReliability = exceptionDiscardReliability;
  }
  
}
