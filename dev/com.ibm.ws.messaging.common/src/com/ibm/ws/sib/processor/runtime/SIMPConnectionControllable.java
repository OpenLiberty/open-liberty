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
package com.ibm.ws.sib.processor.runtime;

/**
 * The connection controllable interface
 */
public interface SIMPConnectionControllable extends SIMPControllable
{
  /**
   * 
   * @return SIMPMessageProcessorControllable  to perform dynamic control operations
   */
  SIMPMessageProcessorControllable getMessageProcessor();

  /**
   * Locates the consumers created under this connection. 
   *
   * @return Iterator  An iterator over all of the Consumer objects. 
   *
   */
  SIMPIterator getConsumerIterator();

  /**
   * Locates the producers created under this connection. 
   *
   * @return Iterator  An iterator over all of the Producer objects. 
   */
  SIMPIterator getProducerIterator();

  /**
   * Locates the browsers created under this connection. 
   *
   * @return Iterator  An iterator over all of the Browser objects. 
   *
   */
  SIMPIterator getBrowserIterator();
}
