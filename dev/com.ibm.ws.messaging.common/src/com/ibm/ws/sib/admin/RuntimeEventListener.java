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

import java.util.Properties;

/**
 * The implementors of this interface receive events from elsewhere
 * in JetStream for emission via the J2EE MBean notification mechanism.
 * 
 * @see com.ibm.ws.sib.admin.ControllableRegistrationService
 */
public interface RuntimeEventListener
{
  /**
   * Sends an event to the interface implementor.
   *  
   * @param me The MessagingEngine object associated with this Notification.
   * @param type The type of Notification to be propagated.
   * @param message The message to be propagated in the Notification.
   * @param properties The Properties associated with this Notification
   * type.
   */
  public void runtimeEventOccurred(JsMessagingEngine me,
                                   String type,
                                   String message,
                                   Properties properties);
}

