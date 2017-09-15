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

import com.ibm.websphere.sib.SIDestinationAddress;
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.ws.sib.comms.MEConnectionListener;
import com.ibm.ws.sib.trm.topology.TopologyListener;
import com.ibm.ws.sib.utils.SIBUuid8;



/**
 * @author jroots
 */
public interface SIMPFactory
{

  /**
   * return a new reference to an implementation of ExceptionDestinationHandler.
   * Accepts a name of a destination that could not be delivered to OR null if there
   * is no destination.
   *
   * @param destName - The name of the destination that could not be delivered to.
   */
  public ExceptionDestinationHandler createExceptionDestinationHandler(SIDestinationAddress dest)
    throws SIException;

  public ExceptionDestinationHandler createLinkExceptionDestinationHandler(SIBUuid8 mqLinkUuid)
  throws SIException;

  public MEConnectionListener getMEConnectionListener();
  public TopologyListener getTopologyListener();
  public boolean isStarted();
}
