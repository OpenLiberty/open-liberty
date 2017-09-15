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
package com.ibm.ws.sib.processor.test.utils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;


import com.ibm.ws.sib.admin.MQLinkDefinition;
import com.ibm.ws.sib.admin.VirtualLinkDefinition;
import com.ibm.ws.sib.utils.SIBUuid8;

/**
 * A class to support the WCCM simulation.
 * 
 * Links are tracked using this structure.
 * 
 */
public class LinkInformation
{
  private VirtualLinkDefinition lDefinition; // Returned by the Transformer
                                              // during addTarget
  private boolean mq;
  private SIBUuid8 mqLinkUuid;

  public LinkInformation(VirtualLinkDefinition lDefinition)
  {
    this.lDefinition = lDefinition;
    mq = false;
  }

  public LinkInformation(VirtualLinkDefinition lDefinition, SIBUuid8 mqLinkUuid)
  {
    this.lDefinition = lDefinition;
    this.mqLinkUuid = mqLinkUuid;
    mq = true;
  }

  /**
   * Returns the lDefinition.
   * 
   * @return DestinationDefinition
   */
  public VirtualLinkDefinition getLDefinition()
  {
    return lDefinition;
  }

  /**
   * Gets the MQLinkDefinition.
   * 
   * @return MQLinkDefinition
   */
  public MQLinkDefinition getMQLinkDefinition() 
  {
    // Use reflection to provide an MQLinkDefinition
    class MQLinkDefinitionImpl implements InvocationHandler
    {
      /*
       * (non-Javadoc)
       * 
       * @see java.lang.reflect.InvocationHandler#invoke(java.lang.Object,
       *      java.lang.reflect.Method, java.lang.Object[])
       */
      public Object invoke(Object object, Method method, Object[] args)
          throws Throwable
      {
        if (method.getName().equals("getUuid"))
        {
          return mqLinkUuid;
        }
        else
          return null;
      }
    };// invocation handler.
     
    MQLinkDefinitionImpl mqlinkIh = new MQLinkDefinitionImpl();
    // Set up our outbound client.
    MQLinkDefinition mqLinkDef = 
      (MQLinkDefinition)Proxy.newProxyInstance(MQLinkDefinition.class.getClassLoader(), 
                                               new Class[] { MQLinkDefinition.class }, 
                                               mqlinkIh);        
      return mqLinkDef;
  }      

  public boolean isMQ()
  {
    return mq;
  }

  public boolean isLocalisationDeleted()
  {
    return lDefinition.getLinkLocalitySet().isEmpty();
  }
}