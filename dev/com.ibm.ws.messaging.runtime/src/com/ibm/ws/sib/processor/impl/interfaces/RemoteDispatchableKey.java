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

import com.ibm.wsspi.sib.core.SelectionCriteria;
import com.ibm.websphere.sib.exception.SIException;

/**
 * RemoteDispatchableKey should be implemented by any class wishing to register
 * with a RemoteConsumerDispatcher for dispatching
 */
public interface RemoteDispatchableKey extends DispatchableKey {
  
  public SelectionCriteria[] getSelectionCriteria();
  
  public void notifyException(SIException e);

  public boolean hasNonSpecificConsumers();
}
