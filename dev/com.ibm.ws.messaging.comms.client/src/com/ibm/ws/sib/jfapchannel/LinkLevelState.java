/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.jfapchannel;

public interface LinkLevelState {

	/**
	 * Resets the state of this LinkLevelState object so that the same instance can
	 * be used. This allows MFP to maintain the list of LinkLevelState objects.
	 */
	void reset();
}
