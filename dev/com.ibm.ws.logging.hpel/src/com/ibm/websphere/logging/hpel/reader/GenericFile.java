/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.logging.hpel.reader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Interface representing extension of {@link java.io.File} requiring special methods for
 * finding child instances and to return {@link java.io.InputStream} providing file's content.
 * 
 * @ibm-api
 */
public interface GenericFile  {

	/**
	 * Returns child instance with the provided name.
	 * 
	 * @param name name of the child instance
	 * @return instance of the same class representing the child instance.
	 */
	File getChild(String name);
	
	/**
	 * Returns input stream of this file content.
	 * 
	 * @return input stream instance providing access to this file content
	 * @throws IOException if problem happens to open input stream
	 */
	InputStream getInputStream() throws IOException;

}
