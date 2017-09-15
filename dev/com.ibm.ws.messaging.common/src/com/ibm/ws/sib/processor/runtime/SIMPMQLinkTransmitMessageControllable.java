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

public interface SIMPMQLinkTransmitMessageControllable extends
  SIMPQueuedMessageControllable {
  
  /**
   * The queue manager this message is currently targetted at
   * 
   * @return Queue manager
   */
  String getTargetQMgr();
  
  /**
   * The MQ queue name to which this message is being transmitted to 
   * @return
   */
  String getTargetQueue();
  
  /**
   * Set the message state.
   * @param state 
   */
  void setState(String state);
  
  /**
   * Set the target queue manager of this message.
   * @param qMgr
   */
  void setTargetQMgr(String qMgr);
  
  /**
   * Set the target queue of this message.
   * @param queue
   */
  void setTargetQueue(String queue);

}
