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
package com.ibm.wsspi.sib.messagecontrol;

/**
 * The ConsumerResults interface currently only wraps a ConsumerSet. It was introduced as a 
 * potential future container for additional items, such as application editioning support.
 * <p>
 * ConsumerResults are implemented by SIB. Instances are created by XD calling the 
 * createConsumerResults method on a MessagingEngineControl object.
 *
 */
public interface ConsumerResults
{
  /**
   * Retrieve the ConsumerSet wrapped by the ConsumerResults.
   * 
   * @return consumerSet
   */
  public ConsumerSet getConsumerSet();

}
