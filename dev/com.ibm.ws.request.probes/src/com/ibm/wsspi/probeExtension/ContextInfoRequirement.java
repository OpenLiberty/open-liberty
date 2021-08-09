/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.probeExtension;

public interface ContextInfoRequirement {

	/**
	 * Indicates which events, in sampled requests, this ProbeExtension requires
	 * context information to be populated for.
	 */

	int ALL_EVENTS = 0; // context info is required for all events

	/**
	 * Context info is required for events whose type matches a type returned by
	 * getEventTypes.
	 */
	int EVENTS_MATCHING_SPECIFIED_EVENT_TYPES = 1;

	int NONE = 2; // Context info is not required

}
