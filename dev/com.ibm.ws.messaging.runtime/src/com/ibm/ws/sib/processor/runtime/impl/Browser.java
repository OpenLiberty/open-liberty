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
package com.ibm.ws.sib.processor.runtime.impl;

import com.ibm.ws.sib.admin.RuntimeEvent;
import com.ibm.ws.sib.processor.runtime.SIMPBrowserControllable;
import com.ibm.ws.sib.processor.runtime.SIMPConnectionControllable;
import com.ibm.wsspi.sib.core.OrderingContext;

/**
 * 
 */
public class Browser extends AbstractControlAdapter implements SIMPBrowserControllable
{

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPBrowserControllable#getConnection()
   */
  public SIMPConnectionControllable getConnection()
  {
    return null;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPBrowserControllable#getDestinationObject()
   */
  public Object getDestinationObject()
  {
    return null;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPBrowserControllable#getOrderingContext()
   */
  public OrderingContext getOrderingContext()
  {
    return null;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.AbstractControllable#checkValidControllable()
   */
  public void assertValidControllable()
  {
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.AbstractControllable#dereferenceControllable()
   */
  public void dereferenceControllable()
  {
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.ControlAdapter#registerControlAdapterAsMBean()
   */
  public void registerControlAdapterAsMBean()
  {
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.ControlAdapter#deregisterControlAdapterMBean()
   */
  public void deregisterControlAdapterMBean()
  {
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.ControlAdapter#runtimeEventOccurred(com.ibm.ws.sib.admin.RuntimeEvent)
   */
  public void runtimeEventOccurred(RuntimeEvent event)
  {
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.admin.Controllable#getName()
   */
  public String getName()
  {
    return null;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.admin.Controllable#getId()
   */
  public String getId()
  {   
    return null;
  }
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPBrowserControllable#getRemoteMEName()
   */
  public String getRemoteMEName()
  {
    return null;
  }

}
