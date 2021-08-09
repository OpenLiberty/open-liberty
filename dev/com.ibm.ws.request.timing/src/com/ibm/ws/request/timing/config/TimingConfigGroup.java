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
package com.ibm.ws.request.timing.config;

import java.util.List;

/**
 * Collects all the timing related configuration for a subtype.
 */
public interface TimingConfigGroup {
	/**
	 * Returns all the slow request configuration for this subtype.
	 */
	public List<Timing> getSlowRequestTimings();
	
	/**
	 * Returns all the hung request configuration for this subtype.
	 */
	public List<Timing> getHungRequestTimings();
}
