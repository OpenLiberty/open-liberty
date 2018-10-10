/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.instrument.serialfilter.util.trie;

import java.util.Map;

public interface Trie<T> extends Map<String, T> {
    interface Entry<T> extends Map.Entry<String, T>{}

    T getLongestPrefixValue(String key);

    Entry<T> getLongestPrefixEntry(String key);
}
