/*******************************************************************************
 * Copyright (c) 1997, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.cache.exception;

/**
 * @author Andy Chow
 * 
 * Signals that the disk cache is using old format in data structure..
 */
public class DiskCacheUsingOldFormatException extends DynamicCacheException {  // LI4337-17
	
	private static final long serialVersionUID = -3760842684658943228L;

	/**
     * Constructs a DiskCacheEntrySizeOverLimitException with the specified detail message.
     */
	public DiskCacheUsingOldFormatException(String message) {
		super(message);
	}
}
