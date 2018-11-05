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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;

import javax.json.Json;
import javax.json.JsonObject;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.StringUtils;

import com.ibm.json.java.JSONArray;
import com.ibm.json.java.JSONObject;
import com.ibm.websphere.simplicity.log.Log;

/**
 * JWT Token tools for Security testing with JWT Tokens.
 * 
 * <P>Methods in this class
 * <UL>
 * <LI>split the original string token into the different parts.
 * <LI>stores the raw header, payload and signature
 * <LI>stores the deserialized header and payload (signature TBD)
 * <LI>stores a Map of claims for the header and payload (signature again, TBD)
 * <UL>
 * <LI>(claim values will be in string or list of string form for ease of use by test cases comparing against logged app output)
 * </UL>
 * </UL>
 */
public class JwtTokenForTest {

    public static final String DELIMITER = ".";

    String jwtString = null;
    private String jwtHeaderString = null;
    private String jwtPayloadString = null;
    private String jwtSignatureString = null;
    private JsonObject jwtHeaderJson = null;
    private JsonObject jwtPayloadJson = null;
    private JsonObject jwtSignatureJson = null;
    private Map<String, Object> jwtHeaderMap = null;
    private Map<String, Object> jwtPayloadMap = null;
    private Map<String, Object> jwtSignatureMap = null;

    protected static Class<?> thisClass = JwtTokenForTest.class;

    public void printJwtContent() throws Exception {

        Log.info(thisClass, "printJwtContent", "Header: " + getJsonHeader());
        Log.info(thisClass, "printJwtContent", "Payload: " + getJsonPayload());
//        Log.info(thisClass, "printJwtContent", "Signature: " + getJsonSignature());
        Log.info(thisClass, "printJwtContent", "Signature: " + "Not Parsed");

    }

    /**
     * Format and load the JWT Token.
     * Split the string, decode, load maps, ...
     * 
     * @param jwtTokenString - the original multi part JWT Token string
     * @throws Exception
     */
    public JwtTokenForTest(String jwtTokenString) throws Exception {
        Log.info(thisClass, "JwtTokenForTest", "Original JWT Token String: " + jwtTokenString);
        jwtString = jwtTokenString;
        String[] jwtParts = splitTokenString(jwtTokenString);

        jwtHeaderString = jwtParts[0];
        jwtPayloadString = jwtParts[1];
        if (jwtParts.length == 3) {
            jwtSignatureString = jwtParts[2];
        }

        jwtHeaderJson = deserialize(jwtHeaderString);
        jwtPayloadJson = deserialize(jwtPayloadString);
        // TODO - handle signature
//        jwtSignatureJson = deserialize(jwtSignatureString);

        jwtHeaderMap = mapClaimsFromJsonAsStrings(jwtHeaderString);
        jwtPayloadMap = mapClaimsFromJsonAsStrings(jwtPayloadString);
        // TODO - handle signature
//        jwtSignatureMap = mapClaimsFromJsonAsStrings(jwtSignatureString);

        printJwtContent();

    }

    public String getJwtTokenString() {
        return this.jwtString;
    }

    public JsonObject getJsonHeader() {
        return this.jwtHeaderJson;
    }

    public String getStringHeader() {
        return this.jwtHeaderString;
    }

    public Map<String, Object> getMapHeader() {
        return this.jwtHeaderMap;
    }

    public JsonObject getJsonPayload() {
        return this.jwtPayloadJson;
    }

    public String getStringPayload() {
        return this.jwtPayloadString;
    }

    public Map<String, Object> getMapPayload() {
        return this.jwtPayloadMap;
    }

    public JsonObject getJsonSignature() {
        return this.jwtSignatureJson;
    }

    public String getStringSignature() {
        return this.jwtSignatureString;
    }

    public Map<String, Object> getMapSignature() {
        return this.jwtSignatureMap;
    }

    /**
     * Split the origin JWT Token into its multiple parts:
     * Part 1: Header ex: {"typ":"JWT","alg":"RS256"}
     * Part 2: Payload ex:
     * {"token_type":"Bearer","aud":["client01","client02"],"sub":"testuser","upn":"testuser","groups":["group3","group2"],"realm":"BasicRealm","iss":"testIssuer","exp":1540451490,"iat":1540444290}
     * Part 3: Signature ex:
     * 
     * @param tokenString
     *            The original encoded representation of a JWT
     * @return Three components of the JWT as an array of strings
     */
    public String[] splitTokenString(String tokenString) {
        String[] pieces = tokenString.split(Pattern.quote(DELIMITER));

        if (tokenString.endsWith(".") && pieces.length == 1) {
            return pieces;
        }

        if (pieces.length < 2) {
            throw new IllegalStateException("Improperly formatted JWT Token - wrong number of parts: " + tokenString);
        }
        if (pieces.length < 3) {
            Log.info(thisClass, "splitTokenString", "Most JWT Tokens contain 3 parts - this token only contained 2 - keep that in mind if you see odd behavior later");
        }
        if (pieces[0] == null || pieces[1] == null) {
            throw new IllegalStateException("Improperly formatted JWT Token - null token data: " + tokenString);
        }

        return pieces;
    }

    public JsonObject deserialize(String jwtPart) {

        if (jwtPart == null) {
            return null;
        }
        return Json.createReader(new StringReader(fromBase64ToJsonString(jwtPart))).readObject();

    }

    public String fromBase64ToJsonString(String source) {
        return StringUtils.newStringUtf8(Base64.decodeBase64(source));
    }

    /**
     * Parse the part of the jwt passed in (only handles one part of the jwt at a time (header/payload/signature(TBD))
     * Store the claims in a Map of key, claim pairs where claim will be either a string or list of strings.
     * Saved this way to validate against test application output where all values are strings.
     * 
     * @param jsonFormattedString - the portion of the jwt token to process
     * @return - the loaded claim map
     * @throws Exception
     */
    public static Map<String, Object> mapClaimsFromJsonAsStrings(String jsonFormattedString) throws Exception {
        HashMap<String, Object> map = new HashMap<String, Object>();
        if (jsonFormattedString == null) {
            return map;
        }

        JSONObject object = JSONObject.parse(decodeFromBase64String(jsonFormattedString));
        Set<Entry<String, Object>> set = object.entrySet();
        Iterator<Entry<String, Object>> iterator = set.iterator();

        while (iterator.hasNext()) {
            Entry<String, Object> entry = iterator.next();
            String key = entry.getKey();
            Object value = entry.getValue();
//            Log.info(thisClass, "claimsFromJson", "Object type: " + value.getClass());
            if (value instanceof String) {
                map.put(key, value);
            } else if (value instanceof JSONArray) {
                List<String> arr = new ArrayList<String>();
                for (int i = 0; i < ((JSONArray) value).size(); i++) {
                    arr.add(((JSONArray) value).get(i).toString());
                }
                map.put(key, arr);

            } else if (value instanceof JsonObject) {
                map.put(key, mapClaimsFromJsonAsStrings(value.toString()));
            } else {
                map.put(key, value.toString());
            }
        }
        return map;
    }

    public static String decodeFromBase64String(String encoded) {
        return new String(Base64.decodeBase64(encoded));
    }

    public List<String> getElementValueAsListOfStrings(String key) {

        Object obj = jwtPayloadMap.get(key);
        return createListOfStrings(obj);

    }

    /**
     * Creates a list of just the claims names (the keys)
     * 
     * @return - return the list of claims
     */
    public List<String> getPayloadClaims() {
        if (jwtPayloadJson == null) {
            return null;
        }
        List<String> claims = new ArrayList<String>();
        for (String key : jwtPayloadJson.keySet()) {
            claims.add(key);
        }
        return claims;
    }

    /**
     * Return a list of Strings for the claims value.
     * Some of the tests are comparing the token values with what a
     * test app logged. That test app can log all values in 1 message,
     * or 1 value per message. This method allows the verification code
     * to loop through the values...
     * If value is a list, the members of that list will be returned
     * in a new list of strings representing the original values.
     * ie: List<int> = [1, 2, 3] returned as List<String> = [1, 2, 3]
     * If the orginal value was a single instance of some datatype,
     * this method will return that value as a List<String> containing the
     * String representation of that value.
     * 
     * @param obj - the "value" to convert return as a List<String>
     * @return
     */
    public List<String> createListOfStrings(Object obj) {
        List<String> theList = new ArrayList<String>();
        // if no value found, return an empty list
        if (obj == null) {
            return theList;
        }
//        Log.info(thisClass, "createList", "Object: " + obj);
//        Log.info(thisClass, "createList", "Object type: " + obj.getClass());
        if (obj instanceof ArrayList) {
            for (String entry : (List<String>) obj) {
                theList.add(entry);
            }
        } else {
            theList.add(obj.toString());
        }
        return theList;
    }

    public static boolean isNullEmpty(String value) {
        return value == null || value.isEmpty();
    }

}
