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
import com.ibm.ws.sib.processor.exceptions.SIMPControllableNotFoundException;
import com.ibm.ws.sib.processor.runtime.SIMPControllable;

public interface ControlAdapter extends SIMPControllable
{
  public void assertValidControllable() throws SIMPControllableNotFoundException;
  
  public void dereferenceControllable();

  /**
   * Register the control adapter as an MBean
   */
  public void registerControlAdapterAsMBean();

  /**
   * Deregister the control adapter
   */
  public void deregisterControlAdapterMBean();
  
  public void runtimeEventOccurred( RuntimeEvent event );
}
