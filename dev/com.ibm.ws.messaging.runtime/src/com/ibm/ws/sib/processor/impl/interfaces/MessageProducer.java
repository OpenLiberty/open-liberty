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

package com.ibm.ws.sib.processor.impl.interfaces;

import com.ibm.ws.sib.mfp.JsDestinationAddress;

public interface MessageProducer
{	
  public boolean isRoutingDestinationSet();

  public boolean fixedMessagePoint();

  public JsDestinationAddress getRoutingDestination();

  public void setRoutingAddress(JsDestinationAddress routingAddr);
}
