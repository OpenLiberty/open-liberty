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
import com.ibm.ws.sib.processor.gd.InternalOutputStream;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * Iterates over individual source streams in a source stream set.
 * Returns a SourceStreamControl view of the stream. 
 * 
 * @author tpm100
 */
public class InternalOutputStreamControllableIterator extends BasicSIMPIterator
{   
  private static final TraceComponent tc =
    SibTr.register(InternalOutputStreamControllableIterator.class,
      SIMPConstants.MP_TRACE_GROUP, SIMPConstants.RESOURCE_BUNDLE);

  /**
   * 
   * @param _targetStreamSet An iterator for the target streams contained
   * in a single stream set
   */
  public InternalOutputStreamControllableIterator(Iterator _sourceStreamSet)
  {
    super(_sourceStreamSet);
  }
  
  public Object next()
  {
    if(tc.isEntryEnabled())
      SibTr.entry(tc, "next");
    
    InternalOutputStream sourceStream = (InternalOutputStream)super.next();
    InternalOutputStreamControl sourceStreamControl = 
      sourceStream.getControlAdapter();
      
    if(tc.isEntryEnabled())
      SibTr.exit(tc, "next", sourceStreamControl);      
    return sourceStreamControl;
  }
  

}