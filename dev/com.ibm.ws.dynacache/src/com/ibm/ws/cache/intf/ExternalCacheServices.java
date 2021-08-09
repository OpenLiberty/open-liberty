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

import java.util.ArrayList;
import java.util.HashMap;

/**
 * This is the underlying ExternalCacheServices mechanism which is used by the
 * BatchUpdateDaemon.
 */
public interface ExternalCacheServices {

	/**
	 * This is called by the local BatchUpdateDaemon when it wakes up to process
	 * invalidations and sets.
	 * 
	 * @param invalidateIdEvents
	 *            A HashMap of invalidate by id events.
	 * @param invalidateTemplateEvents
	 *            A HashMap of invalidate by template events.
	 * @param pushECFEvents
	 *            A ArrayList of external cache fragment events.
	 */
	public void batchUpdate(HashMap invalidateIdEvents,	HashMap invalidateTemplateEvents, ArrayList pushECFEvents);

}
