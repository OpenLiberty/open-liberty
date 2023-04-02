/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
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

package com.ibm.ws.sib.jfapchannel.impl.eventrecorder;

/**
 * Common base interface for event recorders.
 */
public interface EventRecorder 
{
   /**
    * Log a debug event to the circular buffer
    * @param description description of the event
    */
   void logDebug(String description);
   
   /**
    * Log an error event to the circular buffer
    * @param description description of the event
    */   
   void logError(String description);
   
   /**
    * Log a method entry event to the circular buffer
    * @param description description of the event
    */
   void logEntry(String description);
   
   /**
    * Log a method exit event to the circular buffer
    * @param description description of the event
    */   
   void logExit(String description);
}
