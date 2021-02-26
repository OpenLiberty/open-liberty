/*******************************************************************************
 * Copyright (c) 2017,2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package concurrent.cdi.web;

import java.util.HashMap;
import java.util.Map;

import jakarta.inject.Singleton;

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
