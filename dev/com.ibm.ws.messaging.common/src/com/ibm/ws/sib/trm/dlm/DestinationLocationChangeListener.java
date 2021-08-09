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

package com.ibm.ws.sib.trm.dlm;

import com.ibm.ws.sib.utils.SIBUuid12;
import java.util.Set;

/**
 * The Destination Location Manager change listener is an interface which is
 * called by the Destination Location Manager whenever changes occur in the
 * set of messaging engines localising a destination. A
 * DestinationLocationChangeListener is registered via the DestinationLocationManager
 * setChangeListener method.
 */

public interface DestinationLocationChangeListener {

  /**
   * The listener method called on each change.
   *
   * @param destUuid UUID of the destination
   *
   * @param available Set of newly available destination uuid's (additions)
   *
   * @param unavailable Set of newly unavailable destination uuid's (deletions)
   *
   * @param capability The capability of the destination
   *
   */

  void destinationLocationChange (SIBUuid12 destUuid, Set available, Set unavailable, Capability capability);

  
  /**
   * Because TRM doesn't get told the name of the last ME to stop hosting a destination we
   * have this method to indicate that the destination is no longer available anywhere.
   *
   * @param destUuid UUID of the destination
   *
   * @param capability The capability of the destination
   *
   */
  void destinationUnavailable(SIBUuid12 destUuid, Capability capability);
  
}
