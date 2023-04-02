/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.processor.impl.interfaces;

import com.ibm.ws.sib.processor.runtime.impl.ControlAdapter;

public interface ControllableResource
{
  /**
   * Get the control adapter
   * @return a control adapter
   */
  public ControlAdapter getControlAdapter();

  /**
   * Create the control adapter for this message item stream
   */
  public void createControlAdapter();
  
  public void dereferenceControlAdapter();

  /**
   * Register the control adapter as an MBean
   */
  public void registerControlAdapterAsMBean();

  /**
   * Deregister the control adapter
   */
  public void deregisterControlAdapterMBean();
}
