/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.request.timing.stats;

/**
 * Interface for the requestTiming feature.
 *
 */
public interface RequestTiming {

	/** Total number of requests */
	public long getRequestCount(String type);

	/** Number of currently active requests */
	public long getActiveRequestCount(String type);

	/** Number of currently active requests that are slow */
	public long getSlowRequestCount(String type);

	/** Number of currently active requests that are hung */
	public long getHungRequestCount(String type);

}
