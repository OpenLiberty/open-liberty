/*******************************************************************************
 * Copyright (c) 1997, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.oauth.core.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

public class JSONUtil {
    private static final TraceComponent tc = Tr.register(JSONUtil.class);

    public static StringBuilder getJSON(Object obj) {
        StringBuilder sb = new StringBuilder();
        if (obj == null) {
            sb.append("null");
        } else if (obj instanceof String || obj instanceof StringBuilder || obj instanceof StringBuffer) {
            String str = obj.toString();
            sb.append("\"").append(formatString(str)).append("\"");
        } else {
            sb.append(obj);
        }
        return sb;
    }

    @SuppressWarnings("unchecked")
    public static StringBuilder getJSON(Object[] array) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (Object str : array) {
            if (str instanceof Map) {
                Map<String, Object> map = (Map<String, Object>) str;
                sb.append(getJSON(map)).append(",");
            } else {
                sb.append(getJSON(str)).append(",");
            }
        }
        if (array.length > 0) {
            sb.deleteCharAt(sb.length() - 1);
        }
        sb.append("]");
        return sb;
    }

    public static StringBuilder getJSON(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            sb.append(getJSON(entry.getKey())).append(":");
            Object value = entry.getValue();
            Object[] array = null;
            if (value instanceof Map<?, ?>) {
                @SuppressWarnings("unchecked")
                Map<String, Object> m = (Map<String, Object>) value;
                sb.append(JSONUtil.getJSON(m));
            } else if (value instanceof Object[]) {
                array = (Object[]) value;
                sb.append(JSONUtil.getJSON(array));
            } else {
                sb.append(JSONUtil.getJSON(value));
            }
            sb.append(",");
        }
        if (map.keySet().size() > 0) {
            sb.deleteCharAt(sb.length() - 1);
        }
        sb.append("}");
        return sb;
    }

    private static StringBuilder formatString(String str) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            char ch = str.charAt(i);
            switch (ch) {
            case '\"':
                sb.append("\\\"");
                break;
            case '\\':
                sb.append("\\\\");
                break;
            case '/':
                sb.append("\\/");
                break;
            case '\b':
                sb.append("\\b");
                break;
            case '\f':
                sb.append("\\f");
                break;
            case '\n':
                sb.append("\\n");
                break;
            case '\r':
                sb.append("\\r");
                break;
            case '\t':
                sb.append("\\t");
                break;
            default:
                if ((ch & 0xFF00) == 0) {
                    sb.append(ch);
                } else {
                    sb.append("\\u");
                    String hex = Integer.toHexString(Character.codePointAt(str, i));
                    int zerosNeeded = 4 - hex.length();
                    for (int j = 0; j < zerosNeeded; j++) {
                        sb.append('0');
                    }
                    sb.append(hex);
                }
            }
        }
        return sb;
    }

    public static StringBuilder getJSONStrings(Map<String, String[]> map) {
        if (map == null)
            return null;
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        for (Map.Entry<String, String[]> entry : map.entrySet()) {
            sb.append(getJSON(entry.getKey())).append(":");
            String[] values = entry.getValue();
            sb.append("[");
            for (String value : values) {
                sb.append(getJSON(value)).append(",");
            }
            if (values.length > 0) {
                sb.deleteCharAt(sb.length() - 1);
            }
            sb.append("]");
            sb.append(",");
        }
        if (map.keySet().size() > 0) {
            sb.deleteCharAt(sb.length() - 1);
        }
        sb.append("}");
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "JsonString:" + sb);
        }
        return sb;
    }

    public static Map<String, String[]> parseJSONToMap(String jsonString) throws JsonSyntaxException {
        if (jsonString == null || jsonString.length() == 0)
            return new HashMap<String, String[]>();// return empty Map
        JsonParser parser = new JsonParser();
        JsonObject jsonObject = parser.parse(jsonString).getAsJsonObject();
        return jsonObjectToStringsMap(jsonObject);
    }

    /**
     * @param jsonObject
     * @return
     */
    public static Map<String, String[]> jsonObjectToStringsMap(JsonObject jsonObject) {
        if (jsonObject == null || jsonObject.isJsonNull())
            return null;
        Map<String, String[]> mapJson = new HashMap<String, String[]>();
        Set<Map.Entry<String, JsonElement>> entries = jsonObject.entrySet();
        for (Map.Entry<String, JsonElement> entry : entries) {
            String key = entry.getKey();
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "mapKey:" + key);
            }
            JsonArray jsonArray = entry.getValue().getAsJsonArray();
            int iSize = jsonArray.size();
            String[] values = new String[iSize];
            for (int iI = 0; iI < iSize; iI++) {
                values[iI] = jsonArray.get(iI).getAsString();
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "mapKey:" + key + " value(" + iI + "):" + values[iI]);
                }
            }
            mapJson.put(key, values);
        }
        return mapJson;
    }

    /**
     * @param extensionProperties
     * @return
     */
    public static JsonObject getJsonObject(Map<String, String[]> extensionProperties) {
        if (extensionProperties == null || extensionProperties.size() == 0)
            return null;
        StringBuilder jsonBuilder = getJSONStrings(extensionProperties);
        if (jsonBuilder == null)
            return null;

        JsonParser parser = new JsonParser();
        JsonObject jsonObject = parser.parse(jsonBuilder.toString()).getAsJsonObject();
        return jsonObject;
    }

}
