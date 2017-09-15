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
package com.ibm.ws.sib.processor.impl.indexes;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.utils.index.Index;
import com.ibm.ws.sib.utils.ras.SibTr;

public class DestinationTypeFilter extends AbstractDestinationTypeFilter
{
  public Boolean ALIAS = null;
  public Boolean FOREIGN_DESTINATION = null;
  public Boolean LOCAL = null;
  public Boolean REMOTE = null;
  public Boolean QUEUE = null;
  
  /** 
   * Trace for the component
   */
  private static final TraceComponent tc =
    SibTr.register(
      DestinationTypeFilter.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);
            
  public boolean matches(Index.Type type)
  {
    if(type == null) return false;
    if(type instanceof DestinationIndex.Type)
    {
      DestinationIndex.Type destType = (DestinationIndex.Type) type;      
      if(super.matches(destType) &&
        (ALIAS == null || ALIAS.equals(destType.alias)) &&
        (FOREIGN_DESTINATION == null || FOREIGN_DESTINATION.equals(destType.foreignDestination)) &&
        (LOCAL == null || LOCAL.equals(destType.local)) &&
        (REMOTE == null || REMOTE.equals(destType.remote)) &&
        (QUEUE == null || QUEUE.equals(destType.queue)))
      {
        return true;
      }
    }      
    return false;  
  }     
}
