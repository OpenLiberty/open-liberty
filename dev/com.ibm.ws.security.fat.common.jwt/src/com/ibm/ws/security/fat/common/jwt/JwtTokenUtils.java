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
package com.ibm.ws.security.fat.common.jwt;

import java.io.StringReader;
import java.util.regex.Pattern;

import javax.json.Json;
import javax.json.JsonObject;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.StringUtils;

public class JwtTokenUtils {

    protected static Class<?> thisClass = JwtTokenUtils.class;

    /**
     * @param jwtTokenString - The original encoded representation of a JWT
     * @return Three components of the JWT as an array of strings
     */
    public static String[] splitTokenString(String jwtTokenString) {
        boolean isPlainTextJWT = false;
        if (jwtTokenString.endsWith(".")) {
            isPlainTextJWT = true;
        }
        String[] parts = jwtTokenString.split(Pattern.quote(JwtConstants.JWT_DELIMITER));
        if (!isPlainTextJWT && parts.length != 3) {
            throw new IllegalStateException("Expected JWT to have 3 segments separated by '" + JwtConstants.JWT_DELIMITER + "', but it has " + parts.length + " segments");
        }
        return parts;
    }

    public static String getHeaderString(String jwtTokenString) {
//        Log.info(thisClass, "getHeaderString", "Header string: " + splitTokenString(jwtTokenString)[0]);
        return splitTokenString(jwtTokenString)[0];
    }

    public static JsonObject getHeaderJsonObject(String jwtTokenString) {
//        Log.info(thisClass, "getHeaderJsonObject", "Header object: " + Json.createReader(new StringReader(fromBase64ToJsonString(getHeaderString(jwtTokenString)))).readObject());
        return Json.createReader(new StringReader(fromBase64ToJsonString(getHeaderString(jwtTokenString)))).readObject();
    }

    public static String getPayloadString(String jwtTokenString) {
        return splitTokenString(jwtTokenString)[1];
    }

    public static JsonObject getPayloadJsonObject(String jwtTokenString) {
        return Json.createReader(new StringReader(fromBase64ToJsonString(getPayloadString(jwtTokenString)))).readObject();
    }

    public static String fromBase64ToJsonString(String jwtString) {
        return StringUtils.newStringUtf8(Base64.decodeBase64(jwtString));
    }
}
