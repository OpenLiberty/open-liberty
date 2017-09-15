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
package com.ibm.ws.sib.processor.impl;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.processor.SIMPConstants;

import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.OrderingContext;

/**
 * This class conatains the implementation of the OrderingContext from the CoreSPI.
 */
public class OrderingContextImpl implements OrderingContext
{
  //trace
  private static final TraceComponent tc =
    SibTr.register(
      OrderingContextImpl.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);
    
    
  /**
   * Create a new ordering context group object
   */
  public OrderingContextImpl()
  {
  }
}
