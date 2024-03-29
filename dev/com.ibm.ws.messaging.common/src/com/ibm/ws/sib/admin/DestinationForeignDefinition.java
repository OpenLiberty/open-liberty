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

package com.ibm.ws.sib.admin;

import com.ibm.websphere.sib.Reliability;

public interface DestinationForeignDefinition extends BaseDestinationDefinition {

  /**
   * @return
   */
  public String getBus();

  /**
   * @param busName
   */
  public void setBus(String busName);

  /**
   * @return
   */
  public int getDefaultPriority();

  /**
   * @param arg
   */
  public void setDefaultPriority(int arg);

  /**
   * @return
   */
  public Reliability getMaxReliability();

  /**
   * @param arg
   */
  public void setMaxReliability(Reliability arg);

  /**
   * @return
   */
  public boolean isOverrideOfQOSByProducerAllowed();
  
  /**
   * @param arg
   */
  public void setOverrideOfQOSByProducerAllowed(boolean arg);

  /**
   * @return
   */
  public Reliability getDefaultReliability();

  /**
   * @param arg
   */
  public void setDefaultReliability(Reliability arg);

  /**
   * @return
   */
  public boolean isSendAllowed();

  /**
   * @param arg
   */
  public void setSendAllowed(boolean arg);
}
