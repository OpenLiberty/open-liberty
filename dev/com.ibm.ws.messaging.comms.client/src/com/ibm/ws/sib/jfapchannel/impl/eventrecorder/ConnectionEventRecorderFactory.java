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
 * Factory for creating connection event recorders. 
 */
public class ConnectionEventRecorderFactory 
{
   /**
    * @return a new connection event recorder that uses the default number of
    * connection and conversation events in its circular log.
    */
   public static final ConnectionEventRecorder getConnectionEventRecorder()
   {
      return new ConnectionEventRecorderImpl();
   }

   /**
    * @param maxConnectionEvents the maximum number of connection events to
    * log in the recorders circular buffer
    * @return a new connection event recorder that uses the default number of
    * conversation events in its circular log.
    */
   public static final ConnectionEventRecorder getConnectionEventRecorder(int maxConnectionEvents)
   {
      return new ConnectionEventRecorderImpl(maxConnectionEvents);
   }

   /**
    * @param maxConnectionEvents the maximum number of connection events to
    * log in the recorders circular buffer
    * @param maxConversationEvents the maximum number of conversation events to
    * log in the recorders circular buffer
    * @return a connection event recorder that logs a number of events determined
    * by the parameters passed to this method when it is invoked.
    */
   public static final ConnectionEventRecorder getConnectionEventRecorder(int maxConnectionEvents, int maxConversationEvents)
   {
      return new ConnectionEventRecorderImpl(maxConnectionEvents, maxConversationEvents);
   }
   
}
