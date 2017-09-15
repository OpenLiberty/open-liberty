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

import com.ibm.websphere.sib.Reliability;
import com.ibm.websphere.sib.exception.SINotPossibleInCurrentConfigurationException;
import com.ibm.ws.sib.msgstore.LockingCursor;
import com.ibm.ws.sib.utils.SIBUuid12;

public interface JSConsumerKey extends ConsumerKey 
{
  public boolean isKeyReady();
  public void markNotReady();
  void ready(Reliability unrecoverable) throws SINotPossibleInCurrentConfigurationException ;
  void notReady();
  public boolean getForwardScanning();
  public boolean isSpecific();
  public SIBUuid12 getConnectionUuid();
  public JSConsumerManager getConsumerManager();
  LockingCursor getGetCursor(SIMPMessage msg);
}
