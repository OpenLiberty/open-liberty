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
package com.ibm.ws.sib.admin.impl;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import com.ibm.ws.sib.admin.JsAdminService;
import com.ibm.ws.sib.admin.JsBus;
import com.ibm.ws.sib.admin.JsEngineComponent;
import com.ibm.ws.sib.admin.JsMain;
import com.ibm.ws.sib.admin.JsMessagingEngine;
import com.ibm.ws.sib.admin.JsProcessComponent;
import com.ibm.ws.sib.utils.SIBUuid8;

/**

 */
public class JsAdminServiceImpl extends JsAdminService  
{
  public Vector messagingEngines; 
  private HashMap busses;
  
  public JsAdminServiceImpl()
  {
    busses = new HashMap();
  }
  
  public void reset()
  {
    busses = new HashMap();
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.admin.JsAdminService#setAdminMain(com.ibm.ws.sib.admin.JsMain)
   */
  public void setAdminMain(JsMain arg0)
  {
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.admin.JsAdminService#getAdminMain()
   */
  public JsMain getAdminMain() throws Exception
  {
    return null;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.admin.JsAdminService#isInitialized()
   */
  public boolean isInitialized()
  {
    // True so that statistic tests use our mimic StatsFactory rather than
    // not registering with PMI.
    return true;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.admin.JsAdminService#getProcessComponent(java.lang.String)
   */
  public JsProcessComponent getProcessComponent(String arg0)
  {
    return null;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.admin.JsAdminService#listMessagingEngines()
   */
  public Enumeration listMessagingEngines()
  {
    return null;
  }
  
  /**
   * Sets a neighbour for restart.
   * @param uuid
   * @param busName
   */
  public void setNeighbour(SIBUuid8 uuid, String busName)
  {
    HashSet set = (HashSet)busses.get(busName);
    
    if (set == null)
      set = new HashSet();
    
    busses.put(busName, set);
    
    set.add(uuid.toString());
    
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.admin.JsAdminService#listMessagingEngines(java.lang.String)
   */
  public Enumeration listMessagingEngines(String arg0)
  {
    return null;
  }
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.admin.JsAdminService#getMessagingEngineSet(java.lang.String)
   */
  public Set getMessagingEngineSet(String busName) 
  {
    return (Set)busses.get(busName);
  }  

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.admin.JsAdminService#getMessagingEngine(java.lang.String, java.lang.String)
   */
  public JsMessagingEngine getMessagingEngine(String arg0, String arg1)
  {
    return null;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.admin.JsAdminService#getMessageProcessor(java.lang.String)
   */
  public JsEngineComponent getMessageProcessor(String arg0)
  {
    return null;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.admin.JsAdminService#activateJMSResource()
   */
  public void activateJMSResource()
  {
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.admin.JsAdminService#deactivateJMSResource()
   */
  public void deactivateJMSResource()
  {
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.admin.JsAdminService#getService(java.lang.Class)
   */
  public Object getService(Class arg0)
  {
    return null;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.admin.JsAdminService#getBus(java.lang.String)
   */
  public JsBus getBus(String name)
  {
    return null;
  }
  
  /**
   * @see JsAdminService#getDefinedBus(String)
   */
  public JsBus getDefinedBus(String name)
  {
    return null;
  }

  public List<String> listDefinedBuses()
  {
    return null;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.admin.JsAdminService#isStandaloneServer()
   */
  public boolean isStandaloneServer()
  {
    return false;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.admin.JsAdminService#quoteJmxPropertyValue(java.lang.String)
   */
  public String quoteJmxPropertyValue(String arg0)
  {
    return null;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.admin.JsAdminService#unquoteJmxPropertyValue(java.lang.String)
   */
  public String unquoteJmxPropertyValue(String arg0)
  {
    return null;
  }
}
