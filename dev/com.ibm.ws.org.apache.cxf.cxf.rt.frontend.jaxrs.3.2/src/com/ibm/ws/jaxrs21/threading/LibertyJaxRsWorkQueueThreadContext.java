/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs21.threading;

import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class LibertyJaxRsWorkQueueThreadContext {

    private final Map<String, Object> map = new HashMap<String, Object>();

    public void put(Class<?> cls, Object o) {
        if (cls == null || o == null)
            return;

        map.put(cls.getName(), o);
    }

    public Object get(Class<?> cls) {

        if (cls == null)
            return null;

        return map.get(cls.getName());
    }

    public void remove(Class<?> cls) {
        if (cls == null)
            return;

        map.remove(cls.getName());
    }

}
