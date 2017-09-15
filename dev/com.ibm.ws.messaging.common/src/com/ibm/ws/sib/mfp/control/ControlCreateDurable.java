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
package com.ibm.ws.sib.mfp.control;

import java.util.Map;

/**
 * ControlCreateDurable extends the general ControlMessage interface and provides
 * get/set methods for the fields specific to a Control Create Durable.
 */
public interface ControlCreateDurable extends ControlMessage {

  /* **************************************************************************/
  /* Get Methods                                                              */
  /* **************************************************************************/

  /**
   * Get the request ID for this request
   *
   * @return A long containing the request ID
   */
  public long getRequestID();

  /**
   * Get the name of the subscription to create
   *
   * @return A subscription name of the form client##name
   */
  public String getDurableSubName();

  /**
   * Get the topic discriminator for the subscription to create.
   *
   * @return A topic discriminator.
   */
  public String getDurableDiscriminator();

  /**
   * Get the selector for the subscription to create.
   *
   * @return A subscription selector.
   */
  public String getDurableSelector();

  /**
   * Get the selector domain for the subscription to create.
   *
   * @return A selector domain value
   */
  public int getDurableSelectorDomain();

  /**
   *  Get the contents of the SecurityUserid field for the subscription.
   *
   *  @return A String containing the SecurityUserid name.
   */
  public String getSecurityUserid();

  /**
   *  Indicate whether the message was sent by a system user.
   *
   *  @return A boolean indicating whether the message was sent by a system user
   */
  public boolean isSecurityUseridSentBySystem();

  /**
   * Get the noLocal flag value.
   * This indicates that consumers on this durable subscription
   * should not receive messages that have been produced on the same connection
   * as the consumer.
   */
  public boolean isNoLocal();

  /**
   * Get the cloned flag.
   * This indicates that the durable susbcription has been cloned and so
   * can be shared by more than one ME on the bus.
   */
  public boolean isCloned();

  /**
   * Get the map of prefixes to namespace URLs that are associated with the selector.
   *
   * @return the map of namespace prefixes
   */
  public Map<String,String> getDurableSelectorNamespaceMap();

  /* **************************************************************************/
  /* Set Methods                                                              */
  /* **************************************************************************/

  /**
   * Set the request ID for this request
   *
   * @param value A long containing the request ID
   */
  public void setRequestID(long value);

  /**
   * Set the name of the subscription to delete.
   *
   * @param name A subscription name of the form client##name
   */
  public void setDurableSubName(String name);

  /**
   * Set the topic discriminator for the subscription to create.
   *
   * @return The topic discriminator for the new subscription.
   */
  public void setDurableDiscriminator(String discriminator);

  /**
   * Set the selector for the subscription to create.
   *
   * @param selector The selector for the new subscription.
   */
  public void setDurableSelector(String selector);

  /**
   * Set the selector domain for the subscription to create.
   *
   * @param domain The selector domain value
   */
  public void setDurableSelectorDomain(int domain);

  /**
   *  Set whether the message was sent by a system user.
   *
   *  @param value A boolean indicating whether the message was sent by a system user
   */
  public void setSecurityUseridSentBySystem(boolean value);

  /**
   *  Set the contents of the SecurityUserid field for the subscription.
   *
   *  @param value A String containing the SecurityUserid name.
   */
  public void setSecurityUserid(String value);

  /**
   * Set the noLocal flag.
   * This indicates that consumers on this durable subscription
   * should not receive messages that have been produced on the same connection
   * as the consumer.
   */
  public void setNoLocal(boolean value);

  /**
   * Set the cloned flag
   * This indicates that the durable susbcription has been cloned and so
   * can be shared by more than one ME on the bus.
   */
  public void setCloned(boolean value);

  /**
   * Sets a map of prefixes to namespace URLs that are associated with the selector.
   *
   * @param namespaceMap
   */
  public void setDurableSelectorNamespaceMap(Map<String,String> namespaceMap);

}
