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
package com.ibm.ws.sib.processor;

import java.util.Map;

import com.ibm.wsspi.sib.core.SelectionCriteria;

public interface MPSelectionCriteria extends SelectionCriteria
{
  /**
   * Returns a map of properties that are associated with the selector. 
   *   
   * @return the properties map
   */
  public Map<String, Object> getSelectorProperties();
  
  /**
   * Sets a map of properties that are associated with the selector.
   *   
   * @param selectorProperties
   */
  public void setSelectorProperties(Map<String, Object> selectorProperties);  
}
