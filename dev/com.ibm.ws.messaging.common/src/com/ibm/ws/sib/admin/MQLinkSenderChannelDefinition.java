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
 * This class is a wrapper for the MQLinkSenderChannel
 * configuration.
 *
 */
public interface MQLinkSenderChannelDefinition {

  /**
   * Get the configId WCCM refId
   * @return String configId
   */
  public String getConfigId();

  /**
   * Get the Sender channel name
   * @return string containing the name of the sender channel
   */
  public String getSenderChannelName();

  /**
   * Get the host name
   * @return string host name
   */
  public String getHostName();

   /**
   * Get the connameList
   * @return string connameList
   */
  public String getConnameList();
  
  /**
   * Get the port
   * @return int port
   */
  public int getPort();

  /**
   * Get the protocol name
   * @return string protocol name
   */
  public String getProtocolName();

  /**
   * Get the disc interval
   * @return int disc interval
   */
  public int getDiscInterval();

  /**
   * Get the short retry count
   * @return int short retry count
   */
  public int getShortRetryCount();

  /**
   * Get the short retry interval
   * @return int short retry interval
   */
  public int getShortRetryInterval();

  /**
   * Get the long retry count
   * @return long long retry count
   */
  public long getLongRetryCount();

  /**
   * Get the long retry interval
   * @return long long retry interval
   */
  public long getLongRetryInterval();

  /**
   * Get the initial state
   * @return String initial state
   *         CT_SIBMQLinkInitialState.STOPPED |
   *         CT_SIBMQLinkInitialState.STARTED
   */
  public String getInitialState();

}

