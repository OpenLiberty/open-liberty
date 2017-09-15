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

import com.ibm.ws.sib.comms.*;

/**
 * The TopologyListener interface is implemeneted by objects that require
 * to be informed of changes in the bus topology.
 */

public interface TopologyListener {

  /**
   * Method called when new Cellules and/or new Messaging Engines become
   * reachable in the bus.
   *
   * @param lc New Link Cellules now reachable from this messaging engine
   *
   * @param me New Messaging Engines now reachable from this messaging engine
   */

  void increaseInReachability (LinkCellule[] lc, MessagingEngine[] me);

  /**
   * Method called when Cellules and/or Messaging Engines become
   * unreachable in the bus.
   *
   * @param mec The stopped MEConnection that has caused the unreachability
   *
   * @param lc Link Cellules no longer reachable form this messaging engine
   *
   * @param me Messaging Engines no longer reachable from this messaging engine
   */

  void decreaseInReachability (MEConnection mec, LinkCellule[] lc, MessagingEngine[] me);

  /**
   * Method called whenever a connection is started or stoppped. If an existing
   * connection is replaced by a new connection then the existing connection is
   * specified as the 'down' connection and the new connection replacing it is
   * specified as the 'up' connection.
   *
   * @param down The stopped connection
   *
   * @param up The started connection
   */

  void changeConnection (MEConnection down, MEConnection up);

}
