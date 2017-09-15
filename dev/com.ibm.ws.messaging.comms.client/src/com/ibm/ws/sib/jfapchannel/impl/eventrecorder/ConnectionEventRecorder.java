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
 * Event recorder for connection level events. 
 */
public interface ConnectionEventRecorder extends EventRecorder 
{
   /**
    * @return a new event recorder for conversation level events on every invocation
    */
   ConversationEventRecorder getConversationEventRecorder();
}
