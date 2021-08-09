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
 * Defines a message point that is hosted on platform messaging
 */
public interface LocalizationDefinition extends BaseLocalizationDefinition {


  /**
   * @return
   */
  public long getDestinationHighMsgs();

  /**
   * @param value
   */
  public void setDestinationHighMsgs(long value);

  /**
   * @return
   */
  public long getDestinationLowMsgs();

  /**
   * @param value
   */
  public void setDestinationLowMsgs(long value);

  /**
   * @return
   */
  public boolean isSendAllowed();

  /**
   * @param arg
   */
  public void setSendAllowed(boolean arg);



}
