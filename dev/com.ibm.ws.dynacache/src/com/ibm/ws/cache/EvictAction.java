/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cache;


import java.util.ArrayList;

public interface EvictAction {

public ArrayList walkDiskCache(int evictPolicy, int deleteEntries, long deleteSize);

public void deleteFromDisk(ArrayList delEntries);

}

