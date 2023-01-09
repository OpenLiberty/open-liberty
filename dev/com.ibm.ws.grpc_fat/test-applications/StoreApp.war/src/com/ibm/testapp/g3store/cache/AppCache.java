/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
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
package com.ibm.testapp.g3store.cache;

import java.util.Set;

import com.ibm.test.g3store.grpc.RetailApp;

/**
 * @author anupag
 *
 */
public interface AppCache {

    public RetailApp getEntryValue(String key);

    public boolean setEntryValue(String key, RetailApp value, int expiry); // -1 for not expire

    public Object removeEntryValue(String key);

    public Set<String> getAllKeys();

}
