/*******************************************************************************
 * Copyright (c) 1997, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.cache;

/**
 * This interface is used by CacheEntry to indicate that the value implements the GenerateContents() method. 
 * @ibm-spi 
 * @since WAS7.0
 */
public interface GenerateContents {
    /**
     * Returns the contents of this object in byte array.
     * 
     * @return byte[] the byte array of the object
     */
	public byte[] generateContents();
}
