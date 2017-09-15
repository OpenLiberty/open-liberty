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

import com.ibm.ws.sib.processor.impl.ConsumerDispatcherState;
import com.ibm.ws.sib.utils.SIBUuid12;

public interface ControllableSubscription extends ControllableResource
{
  public SIBUuid12 getSubscriptionUuid();
  public ConsumerDispatcherState getConsumerDispatcherState();
  public OutputHandler getOutputHandler();
  //local or proxy
  public boolean isLocal();
  public boolean isDurable();  
}
