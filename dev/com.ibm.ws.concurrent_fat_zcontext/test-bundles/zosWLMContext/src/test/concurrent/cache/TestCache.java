/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package test.concurrent.cache;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Used by tests to cache objects across application restart.
 * This has nothing to do with the simulated z/OS contexts.
 */
public class TestCache {
    public static ConcurrentHashMap<String, Object> instance = new ConcurrentHashMap<>();
}