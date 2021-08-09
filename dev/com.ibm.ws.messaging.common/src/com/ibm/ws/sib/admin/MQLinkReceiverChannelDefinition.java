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
package com.ibm.ws.sib.admin;

/**
 * @author leonarda
 *
 * This class is a wrapper for the MQLink receiver channel
 * configuration.
 *
 */
public interface MQLinkReceiverChannelDefinition {

  /**
   * Get the configId WCCM refId
   * @return String configId
   */
  public String getConfigId();

  /**
   * Get the receiver channel name
   * @return string receiver channel name
   */
  public String getReceiverChannelName();

  /**
   * Get the inbound npm reliability
   * @return string inbound npm reliability
   *         CT_SIBMQNonPersistentReliability.BEST_EFFORT |
   *         CT_SIBMQNonPersistentReliability.EXPRESS |
   *         CT_SIBMQNonPersistentReliability.RELIABLE
   */
  public String getInboundNpmReliability();

  /**
   * Get the inbound pm reliability
   * @return string inbound pm reliability
   *         CT_SIBMQPersistentReliability.RELIABLE |
   *         CT_SIBMQPersistentReliability.ASSURED
   */
  public String getInboundPmReliability();

  /**
   * Get the initial state
   * @return String initial state
   *         CT_SIBMQLinkInitialState.STOPPED |
   *         CT_SIBMQLinkInitialState.STARTED
   */
  public String getInitialState();
 /**
   * Return whether local queue points are
   * preferred (now optional in WAS7.x)
   * 
   * @return boolean
   */
  public boolean getPreferLocal();

}

