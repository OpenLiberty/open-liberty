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
package com.ibm.ws.security.oauth20.util;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.ibm.oauth.core.internal.oauth20.OAuth20Util;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.security.oauth20.TraceConstants;

/**
 *
 */
public class OidcOAuth20Util extends OAuth20Util {
    final static String CLASS = OidcOAuth20Util.class.getName();
    static Logger _log = Logger.getLogger(CLASS);
    private static final TraceComponent tc = Tr.register(OidcOAuth20Util.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);

    @Trivial
    public static boolean isNullEmpty(Enumeration<String> values) {
        if (values == null) {
            return true;
        }

        return !values.hasMoreElements();
    }

    @Trivial
    public static boolean isNullEmpty(String value) {
        return value == null || value.isEmpty();
    }

    @Trivial
    public static boolean isNullEmpty(String[] values) {
        return values == null || values.length == 0;
    }

    @Trivial
    public static boolean isNullEmpty(JsonArray valueArr) {
        return valueArr == null || valueArr.size() == 0;
    }

    public static JsonArray initJsonArray(String value) {
        JsonArray jsonArr = new JsonArray();
        jsonArr.add(new JsonPrimitive(value));

        return jsonArr;
    }

    public static JsonArray initJsonArray(String[] values) {
        JsonArray jsonArr = new JsonArray();

        if (!isNullEmpty(values)) {
            for (String value : values) {
                jsonArr.add(new JsonPrimitive(value));
            }
        }

        return jsonArr;
    }

    @Trivial
    public static String[] getStringArray(JsonArray jsonArray) {
        if (isNullEmpty(jsonArray)) {
            return new String[0];
        }

        String[] strArr = new String[jsonArray.size()];
        for (int i = 0; i < jsonArray.size(); i++) {
            strArr[i] = jsonArray.get(i).getAsString();
        }

        return strArr;
    }

    @Trivial
    public static String getSpaceDelimitedString(JsonArray jsonArray) {
        return getSpaceDelimitedString(getStringArray(jsonArray));
    }

    @Trivial
    public static String getSpaceDelimitedString(String[] strArr) {
        if (strArr == null || strArr.length == 0) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < strArr.length; i++) {
            sb.append(strArr[i]);

            if (i != strArr.length - 1) {
                sb.append(" ");
            }
        }

        return sb.toString();
    }

    public static boolean jsonArrayContainsString(JsonArray jsonArray, String string) {
        return jsonArrayContainsString(jsonArray, string, false);
    }

    public static boolean jsonArrayContainsString(JsonArray jsonArray, String string, boolean matchRegexp) {

        if (isNullEmpty(jsonArray) && !isNullEmpty(string)) {
            return false;
        }

        for (int i = 0; i < jsonArray.size(); i++) {
            if (jsonArray.get(i).getAsString().equals(string)) {
                return true;
            }
        }

        if (!matchRegexp) {
            return false;
        }

        for (int i = 0; i < jsonArray.size(); i++) {
            String redirect = jsonArray.get(i).getAsString();
            // regexp redirects must start with regexp:, trim that
            if (redirect.startsWith(OIDCConstants.REGEXP_PREFIX)) {
                redirect = redirect.substring(7, redirect.length());
                redirect = redirect.replace('!', '\\'); // since gson can't deserialize a \ correctly.
            } else {
                continue;
            }
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "regex check pattern: >" + redirect + "< against string: > " + string + "< match: " + Pattern.matches(redirect, string));
            }
            try {
                if (Pattern.matches(redirect, string)) {
                    return true;
                }
            } catch (PatternSyntaxException pse) {
                // CWWKS1462E
                Tr.error(tc, "regexp.evaluation.error", new Object[] { redirect });
                continue;

            } catch (IllegalArgumentException iex) {
                Tr.error(tc, "regexp.evaluation.error", new Object[] { redirect });
                continue;
            }
        }

        return false;

    }

    public static boolean validateRedirectUris(JsonArray redirectUris, boolean allowRegexpRedirects) {
        if (isNullEmpty(redirectUris)) {
            return true;
        }

        for (int i = 0; i < redirectUris.size(); i++) {
            String buf = redirectUris.get(i).getAsString();
            if (buf.startsWith(OIDCConstants.REGEXP_PREFIX) && allowRegexpRedirects == true) {
                continue;
            }
            boolean result = validateRedirectUri(buf);
            if (result == false) {
                return false;
            }
        }

        return true;
    }

    public static final Gson GSON_RAW = new GsonBuilder()
            .excludeFieldsWithoutExposeAnnotation()
            .create();

    public static final Gson GSON_RAWEST = new GsonBuilder().create();

    @Sensitive
    public static JsonObject getJsonObj(@Sensitive Object object) {
        return (new JsonParser()).parse(GSON_RAW.toJson(object)).getAsJsonObject();
    }

    public static List<JsonObject> getListOfJsonObjects(JsonArray values) {
        List<JsonObject> list = new ArrayList<JsonObject>();
        if (!OidcOAuth20Util.isNullEmpty(values)) {
            for (JsonElement jsonEle : values) {
                list.add(jsonEle.getAsJsonObject());
            }
        }

        return list;
    }

    public static boolean isJwtToken(String accessTokenString) {
        String methodName = "isJwtToken";
        boolean result = false;
        if (!isNullEmpty(accessTokenString)) {
            if (accessTokenString.indexOf(".") >= 0) {
                result = true;
            }
        }

        _log.exiting(CLASS, methodName, "" + result);
        return result;
    }
}
