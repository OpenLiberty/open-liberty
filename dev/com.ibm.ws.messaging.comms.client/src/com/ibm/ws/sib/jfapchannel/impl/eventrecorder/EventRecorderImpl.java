/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.sib.jfapchannel.impl.eventrecorder;

/**
 * Base implementation of the event recorder interface.
 */
public abstract class EventRecorderImpl implements EventRecorder 
{
   private static final byte DEBUG_EVENT = (byte)'D';
   private static final byte ERROR_EVENT = (byte)'E';
   private static final byte ENTRY_EVENT = (byte)'>';
   private static final byte EXIT_EVENT  = (byte)'<';
   
   public void logDebug(String description) 
   {
      fillInNextEvent(DEBUG_EVENT, description);
   }

   public void logError(String description) 
   {
      fillInNextEvent(ERROR_EVENT, description);
   }

   public void logEntry(String description) 
   {
      fillInNextEvent(ENTRY_EVENT, description);   
   }

   public void logExit(String description) 
   {
      fillInNextEvent(EXIT_EVENT, description);      
   }
   
   protected abstract void fillInNextEvent(byte type, String description);
}
