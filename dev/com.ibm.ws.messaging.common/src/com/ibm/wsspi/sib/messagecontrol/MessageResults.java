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
 * MessageResults contain a classification identifier, that associates the message with 
 * an XD service policy. XD returns a MessageResults object in response to an
 * analyseMessage() call against a MessageController.
 * <p>
 * MessageResults are implemented by SIB. Instances are created by XD calling the 
 * createMessageResults method on a MessagingEngineControl object.
 */
public interface MessageResults
{
  /**
   * Retrieve the classification associated with these MessageResults.
   * 
   * @return the classification
   */
  public String getClassification();
}
