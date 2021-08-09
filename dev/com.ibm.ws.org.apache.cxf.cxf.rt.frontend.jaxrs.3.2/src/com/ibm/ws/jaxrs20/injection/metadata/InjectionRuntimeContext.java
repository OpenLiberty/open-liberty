/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs20.injection.metadata;

import java.util.HashMap;
import java.util.Map;

/**
 * InjectionRuntimeContextImpl helps to store objects for injection
 * ParamInjectionMetadata object is used for parameter injection
 */
public class InjectionRuntimeContext {

    private final Map<String, Object> map = new HashMap<String, Object>();

    public void setRuntimeCtxObject(String type, Object object) {

        if (null == type || null == object) {
            return;
        }

        map.put(type, object);
    }

    public Object getRuntimeCtxObject(String type) {

        if (map.containsKey(type)) {
            return map.get(type);
        }

        return null;
    }
}
