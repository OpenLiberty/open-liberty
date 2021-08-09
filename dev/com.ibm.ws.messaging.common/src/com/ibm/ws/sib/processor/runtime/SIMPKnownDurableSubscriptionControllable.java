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
package com.ibm.ws.sib.processor.runtime;

/**
 * 
 */
public interface SIMPKnownDurableSubscriptionControllable extends SIMPControllable
{
  /**
   * Locate the name of the ME which hosts the subscription
   * 
   * @return String Name of the ME.
   */
  public String getDurableHome();

  /**
   * Locates the LocalSubscription controllable of this durable subscription.
   *
   * @return SIMPLocalSubscriptionControllable  The LocalSubscriptionControl object. 
   */
  SIMPLocalSubscriptionControllable getLocalSubscriptionControl();
  
}
