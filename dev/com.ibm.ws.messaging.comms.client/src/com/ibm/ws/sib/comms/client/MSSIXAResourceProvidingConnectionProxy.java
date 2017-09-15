/*******************************************************************************
 * Copyright (c) 2005, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.sib.comms.client;

import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.sib.jfapchannel.Conversation;
import com.ibm.ws.sib.transactions.mpspecific.MSSIXAResourceProvider;
import com.ibm.wsspi.sib.core.SIXAResource;
import com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException;
import com.ibm.wsspi.sib.core.exception.SIConnectionLostException;
import com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException;

/**
 * Comms implementation of SICoreConnection which is augmented such that it may obtain the XAResource of the Message Store.  This
 * additional capability is used for some recovery scenarios.
 * <p>
 * This implementation of the SICoreConnection interface is returned in preference to ConnectionProxy (which it extends) if the FAP
 * version 4 (WAS 6.1) protocol (or later) is being used.
 *
 * @see com.ibm.ws.sib.transactions.mpspecific.MSSIXAResourceProvider
 */
public class MSSIXAResourceProvidingConnectionProxy extends ConnectionProxy implements MSSIXAResourceProvider {
  public MSSIXAResourceProvidingConnectionProxy (final Conversation c) {
    super(c);
  }

  public MSSIXAResourceProvidingConnectionProxy (final Conversation c, final ConnectionProxy parent) {
    super(c, parent);
  }

  public SIXAResource getMSSIXAResource()throws SIConnectionDroppedException, SIConnectionUnavailableException, SIConnectionLostException, SIResourceException, SIErrorException {
    return _internalGetSIXAResource(true);
  }
}
