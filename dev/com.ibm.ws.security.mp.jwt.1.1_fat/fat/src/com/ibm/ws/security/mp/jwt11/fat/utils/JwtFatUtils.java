/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.mp.jwt11.fat.utils;

import java.io.StringReader;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.utils.FatStringUtils;
import com.ibm.ws.security.fat.common.web.WebResponseUtils;

public class JwtFatUtils {

    protected static Class<?> thisClass = JwtFatUtils.class;

    public JsonObject extractJwtPrincipalFromResponse(Object response) throws Exception {
        String method = "extractJwtPrincipalFromResponse";
        try {
            String fullResponse = WebResponseUtils.getResponseText(response);
            return extractJwtPrincipalFromString(fullResponse);
        } catch (Exception e) {
            Log.error(thisClass, method, e);
            throw new Exception("Failed to extract the JWT principal from the provided response: " + e);
        }
    }

    public JsonObject extractJwtPrincipalFromString(String response) throws Exception {
        String method = "extractJwtPrincipalFromString";
        try {
            String jwtPrincipalRegex = "getUserPrincipal: (\\{.+\\})";
            String jwtString = FatStringUtils.extractRegexGroup(response, jwtPrincipalRegex);
            Log.info(thisClass, method, "Got JWT string from response: " + jwtString);

            JsonReader reader = Json.createReader(new StringReader(jwtString));
            return reader.readObject();
        } catch (Exception e) {
            Log.error(thisClass, method, e);
            throw new Exception("Failed to extract the JWT principal from the provided string: " + e);
        }
    }

}
