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

import java.util.HashSet;
import java.util.Set;

import com.ibm.ws.sib.trm.links.ibl.InterBusLinkConfig;
import com.ibm.ws.sib.trm.links.ibl.InterBusLinkManager;
import com.ibm.ws.sib.utils.SIBUuid12;

/**
 * @author millwood
 *
 */
public class UTInterBusLinkManager implements InterBusLinkManager
{
  private Set links = new HashSet();

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.trm.links.ibl.InterBusLinkManager#define(com.ibm.ws.sib.trm.links.ibl.InterBusLinkConfig)
   */
  public void define(InterBusLinkConfig arg0)
  {
    links.add(arg0.getUuid());    
  }
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.trm.links.ibl.InterBusLinkManager#define(com.ibm.ws.sib.trm.links.ibl.InterBusLinkConfig)
   */
  public void undefine(SIBUuid12 linkUuid)
  {
    links.remove(linkUuid);    
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.trm.links.ibl.InterBusLinkManager#start(com.ibm.ws.sib.utils.SIBUuid12)
   */
  public void start(SIBUuid12 arg0)
  {
    return;
    
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.trm.links.ibl.InterBusLinkManager#stop(com.ibm.ws.sib.utils.SIBUuid12)
   */
  public void stop(SIBUuid12 arg0)
  {
    return;    
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.trm.links.ibl.InterBusLinkManager#isDefined(com.ibm.ws.sib.utils.SIBUuid12)
   */
  public boolean isDefined(SIBUuid12 arg0)
  {
    return links.contains(arg0);
  }

  /**
   * Our tests don't yet use this method.
   */
  public boolean isStarted(SIBUuid12 arg0)
  {
    return true;
  }

  /**
   * Our tests don't yet use this method.
   */
  public boolean isActive(SIBUuid12 arg0)
  {
    return true;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.trm.links.ibl.InterBusLinkManager#getActiveAuthenticationAlias(com.ibm.ws.sib.utils.SIBUuid12)
   */
  public String getActiveAuthenticationAlias(SIBUuid12 linkUuid)
  {
    // Not used: just return NULL
    return null;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.trm.links.ibl.InterBusLinkManager#getActiveBootstrapEndpoints(com.ibm.ws.sib.utils.SIBUuid12)
   */
  public String getActiveBootstrapEndpoints(SIBUuid12 linkUuid)
  {
    // Not used: just return NULL
    return null;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.trm.links.ibl.InterBusLinkManager#getActiveTargetInboundTransportChain(com.ibm.ws.sib.utils.SIBUuid12)
   */
  public String getActiveTargetInboundTransportChain(SIBUuid12 linkUuid)
  {
    // Not used: just return NULL
    return null;
  }

}
