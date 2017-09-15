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
import com.ibm.ws.sib.processor.runtime.SIMPTransmitMessageControllable;
import com.ibm.ws.sib.utils.ras.SibTr;


/**
 * Iterates along the messages on a source stream
 * @author tpm100
 */
public class TransmitMessageControllableIterator extends BasicSIMPIterator
{
  
  private static final TraceComponent tc =
    SibTr.register(
      TransmitMessageControllableIterator.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);

  
  public TransmitMessageControllableIterator(Iterator parent)
  {
    super(parent);
  } 
  
  public Object next()
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "next");
    
    SIMPTransmitMessageControllable tran = 
       (SIMPTransmitMessageControllable)super.next();

    if (tc.isEntryEnabled())
      SibTr.exit(tc, "next", tran);      
    return tran;
  }
  

}
