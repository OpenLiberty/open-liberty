/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.meta;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import jakarta.json.bind.adapter.JsonbAdapter;

/**
 * To be used on Map<MetaKey, Object> fields in classes/records
 * Results in Map<String, Object>
 */
public class MetaResponseAdapter implements JsonbAdapter<Map<MetaKey, Object>, Map<String, Object>> {

    private MetaKeyAdapter adapter = new MetaKeyAdapter();

    /** {@inheritDoc} */
    @Override
    public Map<MetaKey, Object> adaptFromJson(Map<String, Object> arg0) throws Exception {
        HashMap<MetaKey, Object> hm = new HashMap<>();
        for (Entry<String, Object> entry : arg0.entrySet()) {
            hm.put(adapter.adaptFromJson(entry.getKey()), entry.getValue());
        }
        return hm;
    }

    /** {@inheritDoc} */
    @Override
    public Map<String, Object> adaptToJson(Map<MetaKey, Object> arg0) throws Exception {
        HashMap<String, Object> hm = new HashMap<>();
        for (Entry<MetaKey, Object> entry : arg0.entrySet()) {
            hm.put(adapter.adaptToJson(entry.getKey()), entry.getValue());
        }
        return hm;
    }

}
