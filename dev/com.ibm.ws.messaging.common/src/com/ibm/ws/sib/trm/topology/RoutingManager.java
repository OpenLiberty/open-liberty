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

package com.ibm.ws.sib.trm.topology;

import com.ibm.ws.sib.comms.MEConnection;
import com.ibm.ws.sib.utils.SIBUuid8;

/**
 * This interface defines the main routing interface into topology routing &
 * management.
 */

public interface RoutingManager {

  /**
   * Return a list all available connections that may be used to reach a
   * specified Cellule from the current message engine.
   *
   * @param c The specified Cellule
   *
   * @return A list of connections which may be used to route a message to the
   * specified Cellule
   */

  public MEConnection[] listConnections (Cellule c);

  public MEConnection connectToME(SIBUuid8 meUuid);

}
