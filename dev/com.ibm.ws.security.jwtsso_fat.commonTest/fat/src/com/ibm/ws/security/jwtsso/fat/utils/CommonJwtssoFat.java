/*******************************************************************************
 * Copyright (c) 2021, 2023 IBM Corporation and others.
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
package com.ibm.ws.security.jwtsso.fat.utils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Base64;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.CommonSecurityFat;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class CommonJwtssoFat extends CommonSecurityFat {

    protected static Class<?> thisClass = CommonJwtssoFat.class;

    protected void verifyJwtHeaderContainsKey(String jwt, String key) throws UnsupportedEncodingException {
        Log.info(thisClass, "verifyJwtHeaderContainsKey", "Verifying that JWT header contains key \"" + key + "\". JWT: " + jwt);
        JsonObject header = extractHeaderAsJsonObject(jwt);
        assertTrue("JWT cookie header should have included a \"" + key + "\" entry but did not. Header was: " + header, header.containsKey(key));
    }

    protected void verifyJwtHeaderContainsKeyAndValue(String jwt, String key, String value) throws UnsupportedEncodingException {
        Log.info(thisClass, "verifyJwtHeaderContainsKey", "Verifying that JWT header contains key \"" + key + "\" and value \"" + value + "\". JWT: " + jwt);
        JsonObject header = extractHeaderAsJsonObject(jwt);
        assertTrue("JWT cookie header should have included a \"" + key + "\" entry but did not. Header was: " + header, header.containsKey(key));
        String val = header.getString(key);
        assertTrue("JWT cookie header should have included a \"" + key + "\" entry with value \"" + value + "\" but did not. Header was: " + header, value.equals(val));
    }

    protected void verifyJwtHeaderDoesNotContainKey(String jwt, String key) throws UnsupportedEncodingException {
        Log.info(thisClass, "verifyJwtHeaderDoesNotContainKey", "Verifying that JWT header does not contain key \"" + key + "\". JWT: " + jwt);
        String jwtHeader = extractAndDecodeJwtHeader(jwt);
        JsonObject header = convertStringToJsonObject(jwtHeader);
        assertFalse("JWT cookie header should NOT have included a \"" + key + "\" entry but did. Header was: " + header, header.containsKey(key));
    }

    private String extractAndDecodeJwtHeader(String jwt) throws UnsupportedEncodingException {
        String encodedJwtHeader = jwt.substring(0, jwt.indexOf("."));
        return new String(Base64.getDecoder().decode(encodedJwtHeader), "UTF-8");
    }

    private JsonObject convertStringToJsonObject(String jsonObjectString) {
        JsonReader reader = Json.createReader(new StringReader(jsonObjectString));
        return reader.readObject();
    }

    private JsonObject extractHeaderAsJsonObject(String jwt) throws UnsupportedEncodingException {
        String jwtHeader = extractAndDecodeJwtHeader(jwt);
        JsonObject header = convertStringToJsonObject(jwtHeader);
        return header;
    }

    protected static void addServerStartupAllowedErrors(LibertyServer server) throws Exception {

        server.addIgnoredErrors(Arrays.asList(MessageConstants.CWWKE0701E_BUNDLE_ACTIVATOR_FAILURE + ": bundle io.openliberty.checkpoint:"));

    }
}
