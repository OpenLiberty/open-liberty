/*******************************************************************************
 * Copyright (c) 2017,2021 IBM Corporation and others.
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
package concurrent.cdi.web;

import static jakarta.enterprise.concurrent.ContextServiceDefinition.ALL_REMAINING;
import static jakarta.enterprise.concurrent.ContextServiceDefinition.TRANSACTION;

import java.util.HashMap;
import java.util.Map;

import jakarta.enterprise.concurrent.ContextServiceDefinition;
import jakarta.inject.Singleton;

@ContextServiceDefinition(name = "java:module/concurrent/txcontextcleared",
                          cleared = TRANSACTION,
                          propagated = ALL_REMAINING)
@Singleton
public class SingletonScopedBean {
    private final Map<Object, Object> map = new HashMap<Object, Object>();

    public Object get(Object key) {
        return map.get(key);
    }

    public Object put(Object key, Object value) {
        return map.put(key, value);
    }
}
