/*******************************************************************************
 * Copyright (c) 1997, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cache.intf;

import java.util.Enumeration;
import com.ibm.ws.cache.InvalidationEvent;

/**
 * This is the underlying ExternalCacheFragment mechanism which is used by the
 * BatchUpdateDaemon, InvalidationAuditDaemon and ExternalCacheService.
 */
public interface ExternalInvalidation extends InvalidationEvent {

	/**
	 * Returns the emumeration of invalidation IDs.
	 *
	 * @return the Emumeration of invalidation IDs.
	 */
	public Enumeration getInvalidationIds();
	
	/**
	 * Returns the enumeration of URIs.
	 *
	 * @return the Enumeration of URIs.
	 */
	public Enumeration getTemplates();
	
	/**
	 * Returns the URI.
	 *
	 * @return The URI.
	 */
	public String getUri();
	
}
