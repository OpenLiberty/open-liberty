/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.request.timing;

/**
 * Request Timing Stats MXBean
 * 
 */
public interface RequestTimingStatsMXBean {

	/** Returns the total number of requests since the server started */
	public long getRequestCount();

	/** Returns the number of currently active requests */
	public long getActiveRequestCount();

	/** Returns the number of requests that are currently slow */
	public long getSlowRequestCount();

	/** Returns the number of requests that are currently hung */
	public long getHungRequestCount();

}
