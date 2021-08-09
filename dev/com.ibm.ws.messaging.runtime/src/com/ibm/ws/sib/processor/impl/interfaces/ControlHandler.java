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

import com.ibm.websphere.sib.exception.SIIncorrectCallException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.sib.mfp.control.ControlMessage;
import com.ibm.ws.sib.utils.SIBUuid8;
import com.ibm.wsspi.sib.core.exception.SIConnectionLostException;
import com.ibm.wsspi.sib.core.exception.SIRollbackException;

/**
 * An interface class for the different types of message handlers.
 */
public interface ControlHandler
{
  public void handleControlMessage(SIBUuid8 sourceMEUuid, ControlMessage cMsg)
    throws SIIncorrectCallException,
           SIResourceException,
           SIConnectionLostException,
           SIRollbackException;

  public long handleControlMessageWithReturnValue(SIBUuid8 sourceMEUuid, ControlMessage cMsg)
    throws SIIncorrectCallException,
           SIResourceException,
           SIConnectionLostException,
           SIRollbackException;

}
