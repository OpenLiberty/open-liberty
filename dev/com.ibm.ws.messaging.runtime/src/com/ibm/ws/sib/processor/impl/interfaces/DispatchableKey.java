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

import com.ibm.websphere.sib.exception.SIException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.sib.matchspace.Selector;

/**
 * DispatchableKey should be implemented by any class wishing to register
 * with a ConsumerDispatcher for dispatching
 */
public interface DispatchableKey extends JSConsumerKey
{
  
  public long getVersion();
  public DispatchableConsumerPoint getConsumerPoint();
  public boolean requiresRecovery(SIMPMessage message) throws SIResourceException;
  public JSConsumerKey getParent();
  public DispatchableKey resolvedKey();
  public Selector getSelector();
  public void notifyReceiveAllowed(boolean newReceiveAllowed, DestinationHandler handler);
  public void notifyConsumerPointAboutException(SIException e);

}
