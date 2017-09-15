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

import java.util.Iterator;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.gd.SourceStream;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * Iterates over individual source streams in a source stream set.
 * Returns a SourceStreamControl view of the stream. 
 * 
 * @author tpm100
 */
public class SourceStreamControllableIterator extends BasicSIMPIterator
{
    
  private static TraceComponent tc =
    SibTr.register(SourceStreamControllableIterator.class,
      SIMPConstants.MP_TRACE_GROUP, SIMPConstants.RESOURCE_BUNDLE);

 
  /**
   * 
   * @param _targetStreamSet An iterator for the target streams contained
   * in a single stream set
   */
  public SourceStreamControllableIterator(Iterator sourceStreamSet)
  {
    super(sourceStreamSet);
  }
  
  public Object next()
  {
    if(TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "next");
    
    SourceStream sourceStream = (SourceStream)super.next();
    ControlAdapter sourceStreamControl = 
      sourceStream.getControlAdapter();
      
    if(TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "next", sourceStreamControl);      
    return sourceStreamControl;
  }
  

}
