/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.websphere.simplicity;

import java.io.File;

/**
 * Represents a file or directory on the local {@link Machine}. This file or directory may or may not
 * actually exist on the local {@link Machine}
 */
public class LocalFile extends RemoteFile {

	/**
	 * Construct an instance based on the fully qualified path of the remote file.
	 * 
	 * @param filePath The fully qualified (absolute) path of the file
	 * @throws Exception
	 */
	public LocalFile(String filePath) throws Exception {
		super(Machine.getLocalMachine(), new File(filePath).getAbsolutePath());
	}

	/**
	 * Construct an instance based on the parent of the local file.
	 * Behavior of LocalFile is undefined if the parent does not
	 * reside on the local machine. 
	 * 
	 * @param parent The remote file's parent directory
	 * @param name The name of the file
	 */
	public LocalFile(RemoteFile parent, String name) throws Exception {
		super(Machine.getLocalMachine(), parent, name);
	}

	/**
	 * Construct an instance based on the parent of the local file.
	 * 
	 * @param parent The local file's parent directory
	 * @param name The name of the file
	 */
	public LocalFile(LocalFile parent, String name) throws Exception {
		super(Machine.getLocalMachine(), parent, name);
	}

}
