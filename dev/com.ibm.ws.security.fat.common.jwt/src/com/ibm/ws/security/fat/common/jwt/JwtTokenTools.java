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

import java.util.ArrayList;
import java.util.List;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.openidconnect.token.JWSHeader;
import com.ibm.ws.security.openidconnect.token.JWTPayload;
import com.ibm.ws.security.openidconnect.token.JsonTokenUtil;
import com.ibm.ws.security.openidconnect.token.WSJsonToken;

public class JwtTokenTools {

    String jwtString = null;
    WSJsonToken jwtToken = null;
    JWSHeader jwtHeader = null;
    JWTPayload jwtPayload = null;

//    public JwtTokenTools() {}

    protected static Class<?> thisClass = JwtTokenTools.class;

//    public void printJwtContent(String jwtString) throws Exception {
//
//        String header = getJwtHeader(jwtString);
//        Log.info(thisClass, "printJwtContent", "Header: " + header);
//    }

    public void printJwtContent() throws Exception {

        Log.info(thisClass, "printJwtContent", "Header: " + this.jwtToken.getHeader());
        Log.info(thisClass, "printJwtContent", "Payload: " + this.jwtToken.getPayload());

    }

    public JwtTokenTools(String jwtTokenString) throws Exception {
        jwtString = jwtTokenString;
        String[] jwtParts = JsonTokenUtil.splitTokenString(jwtTokenString);
        if (jwtParts.length < 2) {
            throw new Exception("Improperly formatted JWT Token - wrong number of parts: " + jwtTokenString);
        }
        if (jwtParts[0] == null || jwtParts[1] == null) {
            throw new Exception("Improperly formatted JWT Token - null token data: " + jwtTokenString);
        }
        jwtToken = JsonTokenUtil.deserialize(jwtParts, jwtTokenString);

        jwtHeader = new JWSHeader();
        // load header keys to header object
        JsonTokenUtil.fromJsonToken(jwtToken, jwtHeader);
        jwtPayload = new JWTPayload();
        // load payload keys to payload object
        JsonTokenUtil.fromJsonToken(jwtToken, jwtPayload);
        Log.info(thisClass, "JwtTokenTools", "Issuer: " + jwtPayload.getIssuer());
        Log.info(thisClass, "JwtTokenTools", "Issuer: " + JsonTokenUtil.getIss(jwtPayload));
        Log.info(thisClass, "JwtTokenTools", "Issuer: " + jwtPayload.get("iss"));

    }

    public String getJwtTokenString() {
        return this.jwtString;
    }

    public JWTPayload getPayload() {
        return this.jwtPayload;
    }

    public JWSHeader getHeader() {
        return this.jwtHeader;
    }

    public String getElement(String element) {
        String value = null;
        if (jwtPayload != null) {
            Object outObj = jwtPayload.get(element);
            Log.info(thisClass, "getElement", "Element: " + element + " with value: " + outObj);
            Log.info(thisClass, "getElement", "data type is: " + outObj.getClass());
            if (outObj instanceof String) {
                value = (String) outObj;
            } else if (outObj instanceof List) {
//                if (((List<String>) outObj).size() == 1) {
//                    value = ((List<String>) outObj).get(0);
                for (String val : (List<String>) outObj)
                    if (value == null) {
                        value = val;
                    } else {
                        value = value + ", " + val;
                    }
//                }
                // TODO - create list val1, val2, ...
                // if there are multiple values, return null.
            } else if (outObj instanceof Long) {
                value = Long.toString((Long) outObj);
            } else if (outObj instanceof java.util.ArrayList) {
                for (String val : (List<String>) outObj)
                    if (value == null) {
                        value = val;
                    } else {
                        value = value + ", " + val;
                    }
                value = Long.toString((Long) outObj);
            } else {
                value = outObj.toString();
            }
        }
        return value;
    }

    public List<String> getListElement(String element) {
        List<String> value = null;
        if (jwtPayload != null) {
            Object outObj = jwtPayload.get(element);
            Log.info(thisClass, "getElement", "Element: " + element + " with value: " + outObj);
//            Log.info(thisClass, "getElement", "data type is: " + outObj.getClass());
            if (outObj instanceof java.util.ArrayList) {
                value = (List<String>) outObj;
            }
        }
        return value;
    }

    public String getOneFromListElement(String element) {
        List<String> listOfElements = getListElement(element);
        if (listOfElements != null && listOfElements.size() > 0) {
            return listOfElements.get(0);
        } else {
            return "\\[\\]";
        }
    }

    public List<String> getElementValueAsList(String element) {
        List<String> theList = new ArrayList<String>();
        Object obj = jwtPayload.get(element);
        if (obj instanceof String) {
            theList.add((String) obj);
        } else if (obj instanceof List) {
            for (String val : (List<String>) obj) {
                theList.add(val);
            }
        } else if (obj instanceof Long) {
            theList.add(Long.toString((Long) obj));
        }
        // add other types as needed
        return theList;
    }

    public List<String> getClaims() {
        if (jwtPayload == null) {
            return null;
        }
        List<String> claims = new ArrayList<String>();
        for (String key : jwtPayload.keySet()) {
            claims.add(key);
        }
        return claims;
    }

//    public Object getPayloadElement(JWTPayload payload) throws Exception {
//
//    }
//        Log.info(thisClass, "printJwtContent", "JWT Token: " + getJsonJwtToken(jwtString).toString());
//
//    }
//
//    public Object extractToken(String jwtString) throws Exception {
//
//        JsonArray tokenArray = getJsonJwtToken(jwtString).asJsonArray();
//        tokenArray.
//
//    }
//
//    public JsonObject getJsonJwtToken(String jwtString) throws Exception {
//        String method = "getJsonJwtToken";
//        try {
//            JsonReader reader = Json.createReader(new StringReader(jwtString));
//            return reader.readObject();
//        } catch (Exception e) {
//            Log.error(thisClass, method, e);
//            throw new Exception("Failed to extract the JWT principal from the provided string: " + e);
//        }
//    }

//  public String getJwtHeader(String jwtString) throws Exception {
//  if (jwtString == null) {
//      throw new Exception("Improperly formatted JWT Token - null token: " + jwtString);
//  }
//  if (jwtString.split("\\.").length != 3) {
//      throw new Exception("Improperly formatted JWT Token - wrong number of parts: " + jwtString);
//  }
//
//  String header = jwtString.split("\\.")[0];
//  if (header == null) {
//      throw new Exception("Improperly formatted JWT Token - header is null: " + header);
//  }
//  return JsonTokenUtil.fromBase64ToJsonString(header);
////  return new String(base64Url.decode(header));
//}

//public void parseJwtToken (String jwtTokenString) throws Exception {

//public JwtTokenTools(String jwtTokenString) throws Exception {
//  String[] jwtParts = JsonTokenUtil.splitTokenString(jwtTokenString);
//  if (jwtParts.length < 2) {
//      throw new Exception("Improperly formatted JWT Token - wrong number of parts: " + jwtTokenString);
//  }
//  if (jwtParts[0] == null || jwtParts[1] == null) {
//      throw new Exception("Improperly formatted JWT Token - null token data: " + jwtTokenString);
//  }
//  JsonParser parser = new JsonParser();
//  JsonObject headerPart = parser.parse(JsonTokenUtil.fromBase64ToJsonString(jwtParts[0])).getAsJsonObject();
//  storeHeaderData(headerPart);
//  JsonObject payloadPart = parser.parse(JsonTokenUtil.fromBase64ToJsonString(jwtParts[1])).getAsJsonObject();
//  storePayloadData(payloadPart);
////  tokenData = new WSJsonToken(headerPart, payloadPart) ;
//}
//
//public void storeHeaderData(JsonObject header) throws Exception {
//  
//  jwtHeader = 
//}
//
//public void storePayloadData(JsonObject payload) throws Exception {
//
//}

}
