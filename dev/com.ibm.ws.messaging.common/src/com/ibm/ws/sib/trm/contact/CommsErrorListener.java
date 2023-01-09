/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.sib.trm.contact;

import com.ibm.ws.sib.comms.MEConnection;

/**
 * An implementation of this interface is used to pass on information about
 * errors with communication connections.
 */

public interface CommsErrorListener {

  /**
   * Method called to inform of a communications error
   *
   * @param m The MEConnection on which the error occurred
   *
   * @param t The Throwable object associated with the error
   */

  public void error (MEConnection m, Throwable t);

}
