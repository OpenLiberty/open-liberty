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
package io.openliberty.security.oidcclientcore.userinfo;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.ibm.json.java.JSONObject;

public class UserInfoResponse {

    private final JSONObject rawResponse;
    private Map<String, Object> responseAsMap;

    public UserInfoResponse(JSONObject responseStr) {
        rawResponse = responseStr;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> asMap() {
        if (responseAsMap != null) {
            return responseAsMap;
        }
        if (rawResponse == null) {
            return null;
        }
        Map<String, Object> map = new HashMap<>();
        Set<String> keys = rawResponse.keySet();
        for (String key : keys) {
            map.put(key, rawResponse.get(key));
        }
        responseAsMap = new HashMap<>(map);
        return map;
    }

    public JSONObject asJSON() {
        return rawResponse;
    }

    public String serialize() {
        try {
            return rawResponse.serialize();
        } catch (IOException e) {
            return null;
        }
    }

}
