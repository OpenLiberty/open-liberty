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
 * Flows specify the weighting associated with a particular classification.
 * <p>
 * The classification string should exactly match the classification identifier 
 * string returned by the analyseMessage() methods supported by the MessageController
 * interface.
 * <p>
 * The weighting value is a non-negative value which is used to weight messages of the 
 * specified classification with respect to other message classifications listed in the 
 * Weighting array (it is not necessary for all weighting values in the array to add up 
 * to 100). The set of weightings will result in assigning each message classification a 
 * probability of dispatch.
 * <p>
 * Flow objects are implemented by SIB. Instances are created by XD calling the 
 * createFlow method on a MessagingEngineControl object.
 */
public interface Flow
{
  /**
   * Retrieve the classification specified for this flow.
   * 
   * @return the classification
   */
  public String getClassification();

  /**
   * Retrieve the weighting associated with this flow.
   * 
   * @return the weighting
   */
  public int getWeighting();
}
