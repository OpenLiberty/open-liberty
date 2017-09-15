/*******************************************************************************
 * Copyright (c) 2009, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.logging.hpel.reader;

import java.io.Serializable;

/**
 * Repository result cache to improve performance of multiple calls
 * 
 * @ibm-api
 */
public interface RemoteListCache extends Serializable {

	/**
	 * gets indicator that this instance contain statistics for all files in the result
	 * @return <code>true</code> if all cache info is complete.
	 */
	public boolean isComplete();
	
	/**
	 * gets number of records in the result based on the information available in this cache.
	 * @return size which could potentially smaller than the full result if {@link #isComplete()} returns <code>false</code>
	 */
	public int getSize();
	
}
