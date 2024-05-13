/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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
package com.ibm.oti.shared;

import java.net.URL;

public interface SharedClassURLHelper {
	public byte[] findSharedClass(URL path, String className);
	
	public byte[] findSharedClass(String partition, URL sourceFileURL, String name);
	
	public boolean storeSharedClass(URL path, Class<?> clazz);

	public boolean storeSharedClass(String partition, URL sourceFileURL, Class clazz);

	public boolean setMinimizeUpdateChecks();
}
