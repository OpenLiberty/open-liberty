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

package com.ibm.ws.sib.trm.links.ibl;

import com.ibm.ws.sib.trm.links.LinkException;
import com.ibm.ws.sib.utils.SIBUuid12;

/**
 * The inter-bus link manager is used specifically for managing Jetstream
 * inter-bus links. Once defined inter links can be operated on using the
 * generic com.ibm.ws.sib.trm.links.LinkManager class.
 */

public interface InterBusLinkManager {

  /**
   * Define a new inter-bus link
   *
   * @param interBusLinkConfig the configuration information for the inter-bus link
   *
   * @throws LinkException if the link is already defined
   */
  void define (InterBusLinkConfig interBusLinkConfig) throws LinkException;
  
  /**
   * Un-define an inter-bus link.
   * 
   * This is used when the ME is stopped through the MBean.
   *
   * @throws LinkException if something goes wrong.
   */
  void undefine (SIBUuid12 linkUuid) throws LinkException;

  /**
   * Start a specific inter-bus link
   *
   * @param linkUuid The uuid of the link to be started
   *
   * @throws LinkException if the linkUuid is not known
   */

  void start (SIBUuid12 linkUuid) throws LinkException;

  /**
   * Stop a specific inter-bus link
   *
   * @param linkUuid The uuid of the link to be stopped
   *
   * @throws LinkException if the linkUuid is not known
   */

  void stop (SIBUuid12 linkUuid) throws LinkException;

  /**
   * Is the inter-bus link defined
   *
   * @param linkUuid The uuid of the link
   *
   * @return boolean true if the link is already defined
   */

  boolean isDefined (SIBUuid12 linkUuid);

  /**
   * Is the link started
   *
   * @return boolean true if the lisk is started
   */

  boolean isStarted (SIBUuid12 linkUuid);

  /**
   * Is the link active
   *
   * @return boolean true if the lisk is active
   */

  boolean isActive (SIBUuid12 linkUuid);
  
  /**
   * Gets the currently active target transport chain for the given sib link
   * @param linkUuid UUID of the SIB link being queried
   * @return Currently active transport chain for given SIB link
   */
  String getActiveTargetInboundTransportChain(SIBUuid12 linkUuid);

  /**
   * Gets the currently active bootstrap endpoints for the given sib link
   * @param linkUuid UUID of the SIB link being queried
   * @return Currently active bootstrap endpoints for given SIB link
   */
  String getActiveBootstrapEndpoints(SIBUuid12 linkUuid);
  
  /**
   * Gets the currently active authentication alias for the given sib link
   * @param linkUuid UUID of the SIB link being queried
   * @return Currently active authentication alias for given SIB link
   */
  String getActiveAuthenticationAlias(SIBUuid12 linkUuid);
}
