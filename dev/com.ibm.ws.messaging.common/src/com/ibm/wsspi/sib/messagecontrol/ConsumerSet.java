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

import com.ibm.websphere.sib.exception.SIIncorrectCallException;

/**
 * A ConsumerSet is used by XD to specify all consumers that are running on any server in a 
 * particular cluster and consuming from queue points on an ME.
 * <p> 
 * If a MessageController is registered, then each consumer attaching to any destination 
 * managed by the ME can be assigned to a ConsumerSet.
 * <p>
 * A consumer is assigned to a ConsumerSet by calling the assignConsumerSet() method
 * on the MessageController.
 * <p>
 * ConsumerSets are implemented by SIB. Instances are created by XD calling the 
 * createConsumerSet method on a MessagingEngineControl object.
 */
public interface ConsumerSet
{
  /**
   * A ConsumerSet may have a concurrency limit defined which applies in aggregate across 
   * all members of the ConsumerSet. 
   * <p>
   * This method allows the limit to be reset dynamically.
   * 
   * @param maxConcurrency the maximum number of active messages for this ConsumerSet.
   */
  public void setConcurrencyLimit(int maxConcurrency);

  /**
   * This method allows the ability to define a set of weightings for classifications.
   * These are specified through flow properties.
   * <p>
   * XD can invoke setFlowProperties()at any time; setting the flow properties on a ConsumerSet
   * will automatically supercede the previous values.
   * 
   * @param flows an array of Flows specifying weightings for classifications.
   */
  public void setFlowProperties(Flow[] flows) throws SIIncorrectCallException;

  /**
   * Retrieve the Flows specified for this ConsumerSet.
   * 
   * @return the specified Flows.
   */
  public Flow[] getFlows();

  /**
   * Retrieve the ConsumerSet's label.
   * 
   * @return the label.
   */
  public String getLabel();  

}
