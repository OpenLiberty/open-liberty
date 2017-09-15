/*******************************************************************************
 * Copyright (c) 1997, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cache;

/**
 * This interface provides the standard way that the InvalidationAuditDaemon
 * gets information from invalidation events and set events. 
 * It is implemented by the InvalidationByIdEvent, InvalidationByTemplateEvent,
 * CacheEntry and ExternalCacheFragment classes.
 */
public interface InvalidationEvent {
   /**
    * This gets the creation timestamp of the event.
    *
    * @return The creation timestamp.
    */
   public long getTimeStamp();
}
