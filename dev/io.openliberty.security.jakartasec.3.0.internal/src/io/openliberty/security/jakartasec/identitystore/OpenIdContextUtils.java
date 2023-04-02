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
package io.openliberty.security.jakartasec.identitystore;

import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;

import com.ibm.json.java.JSONObject;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.context.SubjectManager;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.security.enterprise.identitystore.openid.OpenIdContext;

public class OpenIdContextUtils {

    public static OpenIdContext getOpenIdContextFromSubject() {
        Subject sessionSubject = getSessionSubject();
        if (sessionSubject == null) {
            return null;
        }
        Set<OpenIdContext> creds = sessionSubject.getPrivateCredentials(OpenIdContext.class);
        for (OpenIdContext openIdContext : creds) {
            // there should only be one OpenIdContext in the clientSubject.getPrivateCredentials(OpenIdContext.class) set.
            return openIdContext;
        }
        return null;
    }

    @FFDCIgnore(PrivilegedActionException.class)
    private static Subject getSessionSubject() {
        Subject sessionSubject = null;
        try {
            sessionSubject = (Subject) java.security.AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
                @Override
                public Object run() throws Exception {
                    return new SubjectManager().getCallerSubject();
                }
            });
        } catch (PrivilegedActionException pae) {

        }
        return sessionSubject;
    }

    public static JsonObject convertJsonObject(JSONObject toCovertJson) {
        if (toCovertJson == null) {
            return null;
        }

        Map<String, Object> map = new HashMap<>();
        Set<String> keys = toCovertJson.keySet();
        for (String key : keys) {
            map.put(key, toCovertJson.get(key));
        }

        JsonObjectBuilder buildJakartaJson = Json.createObjectBuilder(map);

        return buildJakartaJson.build();
    }

}
