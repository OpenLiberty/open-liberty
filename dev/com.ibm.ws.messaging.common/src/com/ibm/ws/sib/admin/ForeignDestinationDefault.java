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

import java.util.Map;

import com.ibm.websphere.sib.Reliability;

/**
 * @author philip
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public interface ForeignDestinationDefault {

  /**
   * Get the value of the defaultPriority attribute
   * @return the value
   */
  public int getDefaultPriority();

  /**
   * Get the value of the reliability attribute
   * @return the value
   */
  public Reliability getDefaultReliability();

  /**
   * @return
   */
  public Map getDestinationContext();

  /**
   * Get the value of the maxReliability attribute
   * @return the value
   */
  public Reliability getMaxReliability();

  /**
   * Is overrideOfQOSByProducerAllowed set?
   * @return
   */
  public boolean isOverrideOfQOSByProducerAllowed();

  /**
   * Set the value of the overrideOfQOSByProducerAllowed attribute
   * @param arg
   */
  public void setOverrideOfQOSByProducerAllowed(boolean arg);

  /**
   * @return
   */
  public boolean isReceiveAllowed();

  /**
   * @return
   */
  public boolean isSendAllowed();
}
